/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author updates
 *
 */
public class XMLNormalizer {
	public Element root;
	private static XPath pathWithHaveHasRelation;
	private static XPath pathRangeValueCharacter;
	private static XPath pathCountStructure;
	public static XPath pathCharacterStatement;
	public static XPath pathStateStatement;
	public static XPath pathWholeOrganismStructure;
	public static XPath pathNonWholeOrganismStructure;
	private static XPath pathText;
	
	
	static{
		try{
			pathCharacterStatement = XPath.newInstance(".//statement[@statement_type='character']");
			pathStateStatement = XPath.newInstance(".//statement[@statement_type='character_state']");
			pathWithHaveHasRelation = XPath.newInstance("//relation[@name='with'] | //relation[@name='have'] | //relation[@name='has']");
			pathRangeValueCharacter = XPath.newInstance("//character[@char_type='range_value']");
			pathCountStructure = XPath.newInstance("//structure[character[@name='count']]");
			pathText = XPath.newInstance(".//text");
			pathWholeOrganismStructure = XPath.newInstance(".//structure[@name='whole_organism']");
			pathNonWholeOrganismStructure = XPath.newInstance(".//structure[@name!='whole_organism']");
		}catch(Exception e){
			e.printStackTrace();
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
		}catch(Exception e){
			e.printStackTrace();
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
				List<Element> wos = XPath.selectNodes(statement, ".//structure[@name='whole_organism']");
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
		// TODO Auto-generated method stub

	}

}
