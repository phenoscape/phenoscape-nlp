/**
 * 
 */
package outputter.prep;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import outputter.ApplicationUtilities;
import outputter.Utilities;

/**
 * @author updates
 *
 */
public class XMLNormalizer {
	private static final Logger LOGGER = Logger.getLogger(XMLNormalizer.class);   
	public Element root;
	private static XPath pathWithHaveHasRelation;
	private static XPath pathRangeValueCharacter;
	private static XPath pathCountStructure;
	public static XPath pathCharacterStatement;
	public static XPath pathStateStatement;
	public static XPath pathWholeOrganismStructure;
	public static XPath pathNonWholeOrganismStructure;
	public static XPath pathCharacter;
	public static XPath pathText;
	
	
	static{
		try{
			pathCharacterStatement = XPath.newInstance(".//statement[@statement_type='character']");
			pathStateStatement = XPath.newInstance(".//statement[@statement_type='character_state']");
			pathWithHaveHasRelation = XPath.newInstance("//relation[@name='with'] | //relation[@name='have'] | //relation[@name='has']");
			pathRangeValueCharacter = XPath.newInstance("//character[@char_type='range_value']");
			pathCharacter = XPath.newInstance(".//character");
			pathCountStructure = XPath.newInstance("//structure[character[@name='count']]");
			pathText = XPath.newInstance(".//text");
			pathWholeOrganismStructure = XPath.newInstance(".//structure[@name='"+ApplicationUtilities.getProperty("unknown.structure.name")+"']");
			pathNonWholeOrganismStructure = XPath.newInstance(".//structure[@name!='"+ApplicationUtilities.getProperty("unknown.structure.name")+"']");
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}
	
	/**
	 * preprocessing xml so the data in it are suitable for subsequent EQ generation 
	 */
	public XMLNormalizer(Element root) {
		this.root = root;
	}
	
	public void normalize(){
		try{
			//with2partof(root);
			removeCategoricalRanges(root);
			// expect 1 file to have 1 character statement and n statements, but for generality, use arrayList for characterstatements too.
			//characterstatements are character descriptions
			List<Element> characterstatements = pathCharacterStatement.selectNodes(root);
			integrateWholeOrganism4CharacterStatements(characterstatements, root);
			repairWholeOrganismOnlyCharacterStatements(characterstatements, root);
			
			//Fixing size to corresponding measure
			fixSizeForRespectiveMeasureOnlyCharacterStatements(root);
			collapsePreps(root); //A with a row of B => <structure name="B" constraint="a row of"><relation name="with" from="A" to="B">
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}

	/**
	 * A with a row of B => <structure name="B" constraint="a row of"><relation name="with" from="A" to="B">
	 * @param root
	 */
	private void collapsePreps(Element root) {
		List<Element> descriptions = root.getChildren("description"); //should have only 1 description
		for(Element description: descriptions){
			List<Element> statements = description.getChildren("statement");
			for(Element statement: statements){
				collapsePrepsInStatement(statement);
			}
		}		
	}
	
	/**
	 * <statement statement_type="character" character_id="states356" seg_id="0">
      <text>Coronoids with a row of very small teeth or denticles lateral to tooth-row</text>
      <structure id="o228" name="coronoid" />
      <structure id="o229" name="row" />
      <relation id="r37" name="with" from="o228" to="o229" negation="false" />
      <structure id="o230" name="tooth">
        <character name="size" value="small" modifier="very" />
      </structure>
      <structure id="o231" name="denticle">
        <character name="size" value="small" modifier="very" />
      </structure>
      <relation id="r38" name="consist_of" from="o229" to="o230" negation="false" />
      <relation id="r39" name="consist_of" from="o229" to="o231" negation="false" />
      <structure id="o232" name="tooth row" />
      <relation id="r40" name="lateral to" from="o230" to="o232" negation="false" />
      <relation id="r41" name="lateral to" from="o231" to="o232" negation="false" />
    </statement>
	 */
	
	/**
	 * A with a row of B => <structure name="B" constraint="a row of"><relation name="with" from="A" to="B">
	 * @param statement
	 */
	@SuppressWarnings("unchecked")
	private void collapsePrepsInStatement(Element statement) {
		//find relations that are both to and from organs in different relation
		//when it is a from organ, the relation is "consist of"
		//expanding beyond "consist_of" can be risky
		try{
			boolean fixed = false;
			ArrayList<Element> tobedetached = new ArrayList<Element>();
			List<Element> relationofs = XPath.selectNodes(statement, ".//relation[@name='consist_of']");
			for(Element relationof: relationofs){
				String rowid = relationof.getAttributeValue("from");
				String strid = relationof.getAttributeValue("to"); //tooth id
				List<Element> relationwiths = XPath.selectNodes(statement, ".//relation[@to='"+rowid+"']");
				if(relationwiths!=null && relationwiths.size()>0 
						&& !Utilities.hasCharacters(rowid, root) //'row' without characters
						&& relationofs.equals(XPath.selectNodes(statement, ".//relation[@from='"+rowid+"']")))//no other relations refers to rowid, so row structure may be safely removed
				{  //found the target, now transform
					//1. clone relationwiths, then replace rowid with strid in relationwiths clones, add clones to xml, schedule to detach originals
					for(Element relationwith: relationwiths){
						Element relationwithcp = (Element) relationwith.clone();
						relationwithcp.setAttribute("to", strid);
						statement.addContent(relationwithcp);
						tobedetached.add(relationwith);
					}
					//2. remove row <structure>
					Element row = (Element) XPath.selectSingleNode(statement, ".//structure[@id='"+rowid+"']");
					tobedetached.add(row);
					//3. add constraint "row of" to strid
					Element struct = (Element) XPath.selectSingleNode(statement, ".//structure[@id='"+strid+"']");
					String constraint = struct.getAttribute("constraint")==null? "" : struct.getAttributeValue("constraint") +";";
					struct.setAttribute("constraint", constraint+ Utilities.getStructureName(root, rowid) +" of");
					//4. remove relationofs
					tobedetached.add(relationof);
					fixed=true;
				}
			}
			
			//detach unneeded elements
			for(Element e : tobedetached){
				e.detach();
			}			
			
			if(fixed){
				XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
				System.out.println(outputter.outputString(root));
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
		
	}

	/*
	 * Currently the below text
	 * <text>posteriormost teeth at least twice height of anteriormost teeth</text>
	 * is intepreted as,
	 * 
	 * <structure constraint="posteriormost" name="tooth" id="o324">
	 *  <character constraint="height of anteriormost teeth" name="size" value="2 times" modifier="at-least" constraintid="o325"/> 
	 *  </structure> <structure constraint="anteriormost" name="tooth" id="o325"/>
	 * 
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void fixSizeForRespectiveMeasureOnlyCharacterStatements(Element root) throws Exception {
		
		List<Element> characters = pathCharacter.selectNodes(root);
		
		for(Element chara:characters)
		{
		if(chara.getAttributeValue("name").equals("size"))
		{
			if((chara.getAttributeValue("constraint")!=null)&&(chara.getAttributeValue("constraint").matches(".*(height|width|length|depth).*")))
			{
				//System.out.println("-----------inside normalizer------------");
				if(chara.getAttributeValue("constraint").contains("height"))
				{
					chara.setAttribute("name","height");
				}
				else if(chara.getAttributeValue("constraint").contains("width"))
				{
					chara.setAttribute("name","width");
				}
				else if(chara.getAttributeValue("constraint").contains("depth"))
				{
					chara.setAttribute("name","depth");
				}
				else
				{
					chara.setAttribute("name","length");
				}
			}
		}
		}
	}

	/**
	 * A with B => B part_of A
	 * only sometimes A is not a structure: for example: "body scale: rhomboid with internal ridge; rounded"
	 * @param root
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private void with2partof(Element root) throws Exception {
		List<Element> withs = pathWithHaveHasRelation.selectNodes(root);
		for (Element with : withs) {
			String to = with.getAttributeValue("to");
			String from = with.getAttributeValue("from");
			with.setAttribute("name", "part_of");
			with.setAttribute("to", from);
			with.setAttribute("from", to);
		}
	}

	/**
	 * remove categorical char_type="range_value"
	 * recompose numerical char_type="range_value"
	 * recompose count "7 or more"
	 * 
	 * @param root
	 */
	@SuppressWarnings("unchecked")
	private void removeCategoricalRanges(Element root) throws Exception {

		List<Element> charas = pathRangeValueCharacter.selectNodes(root);
		for (Element chara : charas) {
			if (!chara.getAttributeValue("from").matches("[\\d\\.]+") && !chara.getAttributeValue("to").matches("[\\d\\.]+")) {
				chara.detach(); // remove
			} else {// recompose
				String value = chara.getAttributeValue("from") + (chara.getAttribute("from_unit") == null ? "" : chara.getAttributeValue("from_unit")) + " to "
						+ chara.getAttributeValue("to") + (chara.getAttribute("to_unit") == null ? "" : chara.getAttributeValue("to_unit"));
				chara.removeAttribute("from");
				chara.removeAttribute("from_unit");
				chara.removeAttribute("to");
				chara.removeAttribute("to_unit");
				chara.removeAttribute("char_type");
				chara.setAttribute("value", value.trim());
			}
		}

		List<Element> structs = pathCountStructure.selectNodes(root);
		for (Element struct : structs) {
			charas = struct.getChildren();
			int i = 0;
			String name = "";
			while (charas.size() > i && !name.equals("count")) {
				name = charas.get(i).getAttributeValue("name");
				i++;
			}
			if (name.equals("count") && charas.size() > i) {
				// i is count, check i+1
				if (charas.get(i).getAttributeValue("name").equals("count")) {
					String value = charas.get(i - 1).getAttributeValue("value") + " or " + charas.get(i).getAttributeValue("value");
					charas.get(i - 1).setAttribute("value", value);
					charas.get(i).detach();
				}
			}
		}
	}
	
	/**
	 * in phylogenetic descriptions, whole-organisms are not semantically possible in a character statement
	 * [although whole-organism is used as ditto in state statement]
	 * if such element exists in a character statement, remove whole_organism and merge its character to the next structure
	 * Turn:
	 * <statement statement_type="character" character_id="c97d5c39-838d-4cbb-b159-bce93a7a7291" seg_id="0">
	 * <text>Hatchet-shaped opercle</text>
	 * <structure id="o1507" name="whole_organism">
	 * <character name="shape" value="hatchet-shaped" />
	 * </structure>
	 * <structure id="o1508" name="opercle" />
	 * </statement>
	 * To:
	 * <statement statement_type="character" character_id="c97d5c39-838d-4cbb-b159-bce93a7a7291" seg_id="0">
	 * <text>Hatchet-shaped opercle</text>
	 * </structure>
	 * <structure id="o1508" name="opercle" >
	 * <character name="shape" value="hatchet-shaped" />
	 * </structure>
	 * </statement>
	 * 
	 * @param characterstatements
	 * @return
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void integrateWholeOrganism4CharacterStatements(List<Element> characterstatements, Element root) throws Exception {

		for (Element statement : characterstatements) {
			List<Element> wholeOrgans = pathWholeOrganismStructure.selectNodes(statement);
			if (wholeOrgans.size() > 0 && statement.getChildren("structure").size() > wholeOrgans.size()) {
				// collect ids and chars from wholeOrgans
				ArrayList<Element> chars = new ArrayList<Element>();
				ArrayList<String> woids = new ArrayList<String>();
				for (Element wo : wholeOrgans) {
					woids.add(wo.getAttributeValue("id"));
					chars.addAll((List<Element>) wo.getChildren("character"));
					wo.detach();
				}
				// integration
				Element firststructure = (Element) pathNonWholeOrganismStructure.selectSingleNode(statement);
				String sid = firststructure.getAttributeValue("id");
				// add characters
				for (Element chara : chars) {
					chara.detach();
					firststructure.addContent(chara);
				}
				// replace ids in relations
				for (String woid : woids) {
					Utilities.changeIdsInRelations(woid, sid, root);
				}
			}
		}

	}
	
	/**
	 * character statements that contain 1 structure "whole_organism"
	 * these were caused by annotation errors
	 * for example "IO4", "A-B contact", "bony stays" etc.
	 * 
	 * @param characterstatements
	 *            : character statements that contain 1 structure "whole_organism". This should not be possible. Remark it as structure/entity
	 * @param root
	 */
	@SuppressWarnings("unchecked")
	private void repairWholeOrganismOnlyCharacterStatements(List<Element> characterstatements, Element root) throws Exception {

		for (Element statement : characterstatements) {
			List<Element> structures = pathNonWholeOrganismStructure.selectNodes(statement);
			if (structures.size() == 0) {
				// repair
				Element etext = (Element) pathText.selectSingleNode(statement);
				String text = etext.getTextTrim().replaceAll("\\[.*?\\]", "");
				String struct = text.replaceFirst(".* ", "");
				String constraint = text.replace(struct, "").trim();
				List<Element> wos = XPath.selectNodes(statement, ".//structure[@name='"+ApplicationUtilities.getProperty("unknown.structure.name")+"']");
				if (wos.size() > 0) {
					for (int i = 1; i < wos.size(); i++) {
						wos.get(i).detach();
						wos.remove(i);
					}
					Element wo = wos.get(0);
					wo.setAttribute("name", struct);
					wo.setAttribute("constraint", constraint);
					wo.removeContent();
				}
			}
		}

	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			File dir = new File(ApplicationUtilities.getProperty("source.dir")+"final");
			File[] files = dir.listFiles();
			for(File f: files){
				//File f = new File(ApplicationUtilities.getProperty("source.dir")+"test", "Swartz 2012.xml_states356.xml");
				SAXBuilder builder = new SAXBuilder();
				Document xml = builder.build(f);
				Element root = xml.getRootElement();
				new XMLNormalizer(root).normalize();
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}

	}

}
