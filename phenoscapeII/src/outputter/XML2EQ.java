/**
 * 
 */
package outputter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.charactermarkup.ChunkedSentence;

/**
 * @author Hong Updates
 *This class output EQ statements from the XML files output by CharaParser
 *The XML files are in \target\final
 */
/**
 * 						Entity: eye
						EntityID: TAO:1234567
						Quality:
						Quality-Negated: not round
						QN-Parent:shape
						QN-ParentID: PATO:0000113(id for shape)
						QualityID: PATO:0000111(id for round)
 
 *
 */
public class XML2EQ {
	private File source;
	private String outputtable;
	private Connection conn;
	private String username = "root";
	private String password = "root";
	private int count = 0;
	private String keyentity = null;
	private String keyentitylocator = null;
	private ArrayList<Hashtable<String, String>> allEQs = null;
	private HashSet<String> stateids = new HashSet<String>();
	
	/**
	 * 
	 */
	public XML2EQ(String sourcedir, String database, String outputtable) {
		this.source = new File(sourcedir);
		this.outputtable = outputtable;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("drop table if exists "+ outputtable);
				stmt.execute("create table if not exists "+outputtable+" (id int(11) not null unique auto_increment primary key, source varchar(500), characterID varchar(100), stateID varchar(100), description text, entity varchar(200), entityid varchar(200), quality varchar(200), quality_negated varchar(200), qn_parent varchar(200), qn_parentid varchar(200), qualityid varchar(200), qualitymodifier varchar(200), qualitymodifierid varchar(200), entitylocator varchar(200), entitylocatorid varchar(200))");
				}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	
	public void outputEQs(){
		File[] xmlfiles = this.source.listFiles();
		try{
			for(File f: xmlfiles){
				String src = f.getName();
				SAXBuilder builder = new SAXBuilder();
				Document xml = builder.build(f);
				Element root = xml.getRootElement();
				//expect 1 file to have 1 character statement and n statements, but for generality, use arrayList for characterstatements too.
				List<Element> characterstatements = XPath.selectNodes(root, ".//statement[@statement_type='character']");
				List<Element> statestatements = XPath.selectNodes(root, ".//statement[@statement_type='character_state']");				
				integrateWholeOrganism4CharacterStatements(characterstatements, root);	
				repairWholeOrganismOnlyCharacterStatements(characterstatements, root);
				System.out.println();
				System.out.println("["+count+"]"+src);
				count++;
				//reset
				keyentity = null;
				keyentitylocator = null;
				allEQs = new ArrayList<Hashtable<String, String>>();//used to hold all EQ statement generated from this character unit (1 file holds 1 character unit)				
				createEQs4CharacterUnit(characterstatements, statestatements, src, root); //the set of statements related to a character (one of the statement is the character itself)				
				outputEQs4CharacterUnit();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * character statements that contain 1 structure "whole_organism" 
	 * these were caused by annotation errors
	 * for example "IO4", "A-B contact", "bony stays" etc.
	 * @param characterstatements: character statements that contain 1 structure "whole_organism". This should not be possible. Remark it as structure/entity
	 * @param root
	 */
	private void repairWholeOrganismOnlyCharacterStatements(
			List<Element> characterstatements, Element root) {
		try{
			for(Element statement: characterstatements){
				List<Element> structures = XPath.selectNodes(statement, ".//structure[@name!='whole_organism']");
				if(structures.size()==0){
					//repair
					Element etext = (Element) XPath.selectSingleNode(statement, ".//text");
					String text = etext.getTextTrim().replaceAll("\\[.*?\\]", "");
					String struct = text.replaceFirst(".* ", "");
					String constraint = text.replace(struct, "").trim();
					List<Element> wos = XPath.selectNodes(statement, ".//structure[@name='whole_organism']");
					if(wos.size()>0){
						for(int i = 1; i<wos.size(); i++){
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
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}


	/**
	 * perform global sanity check and normalization
	 * global sanity check: one state text may generate n EQs but one of them must hold keyentity
	 * in the following example, EQ 2 and 3 are wrong
	 
	    [11]Armbruster_2004.xml_0ada121b-dfa5-4093-8ceb-483163cae12e.xml
		text::Lateral wall of metapterygoid channel
		text::absent
	1	EQ::[E]lateral wall [Q]absent [EL]metapterygoid channel
		text::just a slight ridge
	2	EQ::[E]ridge [Q]slight [just]
		text::triangular
	3	EQ::[E]lateral wall [Q]triangular [EL]metapterygoid channel
		text::broad ridge, perpendicular to metapterygoid
	4	EQ::[E]ridge [Q]broad [QM]metapterygoid
		text::long and rounded along entire length
	5	EQ::[E]lateral wall [Q]long [EL]metapterygoid channel
	6	EQ::[E]lateral wall [Q]rounded [along entire length] [EL]metapterygoid channel
	 */
	private void outputEQs4CharacterUnit() {
		//sanity check
		
		for(String stateid: stateids){
			ArrayList<Hashtable<String, String>> problems = new ArrayList<Hashtable<String,String>>();
			for(Hashtable<String,String> EQ: allEQs){
				if(EQ.get("stateid").compareTo(stateid)==0){
					if(this.related2KeyEntity(EQ.get("entity")) || this.related2KeyEntity(EQ.get("entitylocator"))){
						problems = new ArrayList<Hashtable<String, String>>(); //passed, reset problems
						break;
					}else{
						problems.add(EQ);
					}
				}
			}
			//EQ failed sanity check if landed here,
			if(problems.size()>0){
				repairProblemEQs(problems);
			}
		}
		
		String text = "";
		//normalization
		for(Hashtable<String, String> EQ: allEQs){
			//1. entitylocator inherit from the character statement
			/*[8]Armbruster_2004.xml_0638f15b-0de4-45fd-a3af-b1d209cea9d3.xml
			text::Walls of metapterygoid channel
			text::lateral wall slightly smaller to just slightly larger than mesial wall, or absent
			EQ::[E]lateral wall [Q]smaller [slightly]
			EQ::[E]lateral wall [Q]larger [just slightly] [QM]mesial wall
			EQ::[E]lateral wall [Q]absent
			text::mesial wall much taller
			EQ::[E]mesial wall [Q]taller [much]*/
			String entitylocator = EQ.get("entitylocator");
			if(EQ.get("type").compareTo("state")==0 && this.keyentitylocator!=null){ 
				//EQ based on a state statement
				//this is not a binarystate statement
				if(EQ.get("entitylocator").compareTo(this.keyentitylocator)!=0 && related2KeyEntity(EQ.get("entity"))){ 
					//to inhere the entitylocator, this entity must be somewhat related to this.keyentity
					//"lateral wall" is related to "walls" of ...
					entitylocator += ";"+this.keyentitylocator;
					
				}
			}
			entitylocator = entitylocator.trim().replaceAll("(^;|;$)", "");
			EQ.put("entitylocator", entitylocator);

			//2. negation
			if(EQ.get("quality").startsWith("not ")){
				EQ.put("quality", "");
				EQ.put("qualitynegated", EQ.get("quality"));
			}
			
			//finally
			if(EQ.get("text").compareTo(text)!=0) {
				System.out.println("text::"+EQ.get("text"));
				text = EQ.get("text");
			}
			this.insertEQs2Table(EQ);
		}

	}

	/**
	 * 
	 	[11]Armbruster_2004.xml_0ada121b-dfa5-4093-8ceb-483163cae12e.xml
		text::Lateral wall of metapterygoid channel
		text::absent
	1	EQ::[E]lateral wall [Q]absent [EL]metapterygoid channel
		text::just a slight ridge
	2	EQ::[E]ridge [Q]slight [just]
		text::triangular
	3	EQ::[E]lateral wall [Q]triangular [EL]metapterygoid channel
		text::broad ridge, perpendicular to metapterygoid
	4	EQ::[E]ridge [Q]broad [QM]metapterygoid
		text::long and rounded along entire length
	5	EQ::[E]lateral wall [Q]long [EL]metapterygoid channel
	6	EQ::[E]lateral wall [Q]rounded [along entire length] [EL]metapterygoid channel

	 * 
	 * 
	 * 
	 * turn EQ::[E]ridge [Q]slight [just] to
	 * EQ::[E]lateral wall [Q]ridge [just slight] [EL]metapterygoid channel
	 * @param problems: EQs failed the sanity check
	 */
	private void repairProblemEQs(ArrayList<Hashtable<String,String>> problems) {
		//to repair the first EQ
		//EQ #2 in the above example
			Hashtable<String, String> EQ = problems.get(0);
			String olde = EQ.get("entity");
			String oldq = EQ.get("quality").replaceAll("[\\[\\]]", "");
			String oldqm = EQ.get("qualitymodifier");
			String oldel = EQ.get("entitylocator");
			EQ.put("entity", this.keyentity);
			EQ.put("entitylocator", keyentitylocator==null? "" : keyentitylocator);
			EQ.put("quality", olde+ " ["+oldq+"]");
			EQ.put("qualitymodifer", oldel+";"+oldqm);	
	}

	/**
	 * in phylogenetic descriptions, whole-organisms are not semantically possible in a character statement
	 * [although whole-organism is used as ditto in state statement]
	 * if such element exists in a character statement, remove whole_organism and merge its character to the next structure
	 * Turn:
	 * <statement statement_type="character" character_id="c97d5c39-838d-4cbb-b159-bce93a7a7291" seg_id="0">
  	 *	<text>Hatchet-shaped opercle</text>
  	 *	<structure id="o1507" name="whole_organism">
     *	<character name="shape" value="hatchet-shaped" />
  	 *	</structure>
   	 *  <structure id="o1508" name="opercle" />
	 * </statement>
	 * To:
	 * <statement statement_type="character" character_id="c97d5c39-838d-4cbb-b159-bce93a7a7291" seg_id="0">
  	 *	<text>Hatchet-shaped opercle</text>
  	 *	</structure>
   	 *  <structure id="o1508" name="opercle" >
   	 *  	<character name="shape" value="hatchet-shaped" />
   	 *  </structure>
	 * </statement>
	 * 
	 * @param characterstatements
	 * @return
	 * 
	 */
	private void integrateWholeOrganism4CharacterStatements(List<Element> characterstatements, Element root) {
		try{
			for(Element statement: characterstatements){
				List<Element> wholeOrgans = XPath.selectNodes(statement, ".//structure[@name='whole_organism']");
				if(wholeOrgans.size()>0 && statement.getChildren("structure").size()>wholeOrgans.size()){
					//collect ids and chars from wholeOrgans
					ArrayList<Element> chars = new ArrayList<Element>();
					ArrayList<String> woids = new ArrayList<String>();
					for(Element wo: wholeOrgans){
						woids.add(wo.getAttributeValue("id"));
						chars.addAll(wo.getChildren("character"));
						wo.detach();
					}
					//integration
					Element firststructure = (Element) XPath.selectSingleNode(statement, ".//structure[@name!='whole_organism']");
					String sid = firststructure.getAttributeValue("id");
					//add characters
					for(Element chara: chars){
						chara.detach();
						firststructure.addContent(chara);
					}
					//replace ids in relations
					for(String woid: woids){
						this.changeIdsInRelations(woid, sid, root);
					}
				}
			}	
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	/**
	 * For example, 1 character statement with 3 state statements     
	<statement statement_type="character" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" seg_id="0">
      <text>Shape of ancistrine opercle (ordered )</text>
      <structure id="o650" name="whole_organism">
        <character name="shape" value="shape" constraint="of ancistrine opercle" constraintid="o651" />
      </structure>
      <structure id="o651" name="opercle" constraint="ancistrine" />
    </statement>
    <statement statement_type="character_state" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" state_id="4a99e866-54d9-4875-8b5e-385427db1245" seg_id="0">
      <text>sickle-shaped (&lt;i&gt;Peckoltia&lt;/i&gt;-type )</text>
      <structure id="o652" name="whole_organism">
        <character name="shape" value="sickle-shaped" />
      </structure>
    </statement>
    <statement statement_type="character_state" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" state_id="d53ba92f-0865-4456-9111-c6ff37fc624a" seg_id="0">
      <text>barshaped (&lt;i&gt;Ancistrus&lt;/i&gt;-type )</text>
      <structure id="o653" name="whole_organism">
        <character name="shape" value="barshaped" />
      </structure>
    </statement>
    <statement statement_type="character_state" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" state_id="f56a9b6a-9720-437c-a1f4-60f01cd1bb15" seg_id="0">
      <text>oval or triangular</text>
      <structure id="o654" name="whole_organism">
        <character name="shape" value="oval" />
        <character name="shape" value="triangular" />
      </structure>
    </statement>
	 * @param statements
	 */
	private void createEQs4CharacterUnit(List<Element> charstatements, List<Element> states, String src, Element root) {
		//step 0: decide if a character is a binary (yes/no, true/false, rarely/usually types of states)
		if(isBinary(states)){
			//the yes state = character description
			//the no state = negated character description
			//a negation may be to a relation (verb [A "not contact" B] or prep [A "not with" B]) or to a character state ("not expanded") 
			
			//compose n EQs from character statement, because a character statement can describe two equally weighted characters ( A exits B and enters C)
			ArrayList<Hashtable> EQs = processBinaryCharacter(charstatements, src, root);
			createEQsFromBinaryStates(states, src, EQs);
		}else{
			//on the first try, assuming the simple model: 
			//1 char statement holding 1 organ/process/entity
			//n states provide quality
			
			//step 1: process Character Statements
			//the set of character statements is expected to generate an E that is the subject for the state statements
			//while it may also generate additional EQ statements
			//also set keyentity and keyentitylocator fields
			Element E = processCharacter(charstatements, src, root); 
			//step 2: process State Statements
			//Use E to replace the "whole_organism" placeholder in the state statements and generate EQ statements
			//the state statements may also generate additional EQ statements
			if(E != null){
				createEQsFromStateStatements(E, states, src, root);
			}		
		}
	}


	private void createEQsFromBinaryStates(List<Element> states, String src,
			ArrayList<Hashtable> EQs) {
		//copy or negate the EQ for each state
		for(Element state: states){
			Element text = state.getChild("text");
			String stext = text.getTextTrim();
			String characterid = state.getAttributeValue("character_id");
			String stateid = state.getAttributeValue("state_id");
			stateids.add(stateid);
			if(stext.matches("("+ChunkedSentence.binaryTvalues+")")){
				//copy
				for(Hashtable<String,String> EQ: EQs){ //add some needed fields
					EQ.put("src", src);
					EQ.put("characterid", characterid);
					EQ.put("stateid", stateid);
					EQ.put("text", stext); 
					EQ.put("type", "binary");
					allEQs.add(EQ);
					/*this.addEQStatement(src, characterid, stateid, stext, 
							EQ.get("entity"), EQ.get("entityid"), EQ.get("quality"), EQ.get("qualitynegated"),
							EQ.get("qnparent"), EQ.get("qnparentid"),EQ.get("qualityid"), EQ.get("qualitymodifier"), 
							EQ.get("qualitymodifierid"),EQ.get("entitylocator"), EQ.get("entitylocatorid"), true);*/
				}

			}else if(stext.matches("("+ChunkedSentence.binaryFvalues+")")){
				//negate
				for(Hashtable<String,String> EQ: EQs){//add some needed fields
					EQ.put("src", src);
					EQ.put("characterid", characterid);
					EQ.put("stateid", stateid);
					EQ.put("text", stext); 
					EQ.put("quality", ""); //reset quality
					EQ.put("qualitynegated", "not "+EQ.get("quality")); //compose negated quality
					EQ.put("type", "binary");
					allEQs.add(EQ);
					/*this.addEQStatement(src, characterid, stateid, stext, 
						EQ.get("entity"), EQ.get("entityid"), "", "not "+EQ.get("quality"),
						EQ.get("qnparent"), EQ.get("qnparentid"),EQ.get("qualityid"), EQ.get("qualitymodifier"), 
						EQ.get("qualitymodifierid"),EQ.get("entitylocator"), EQ.get("entitylocatorid"), true);*/					
				}
			}
		}
	}
	
	/**
	 * 
	 * @param chars
	 * @param src
	 * @param root
	 * @return a hashtable fieldname => value, if a field does not have a value, set it ""
	 */
	private ArrayList<Hashtable> processBinaryCharacter(
			List<Element> chars, String src, Element root) {
		ArrayList<Hashtable> EQs = new ArrayList<Hashtable>();
		Hashtable<String, String> EQ = new Hashtable<String, String>();
		//initialization
		initEQHash(EQ);
		try{
			//get the first structure element
			Element firststruct = (Element)XPath.selectSingleNode(chars.get(0), ".//structure[@name!='whole_organism']");
			//TODO: what if firststruct == null?
			String sid = firststruct.getAttributeValue("id");
			String sname =this.getStructureName(root, sid);
			EQ.put("entity", sname);
			this.keyentity = sname;
			//take the first character 
			Element chara = (Element)XPath.selectSingleNode(firststruct, ".//character");
			if(chara!=null){
				String q = formQualityValueFromCharacter(chara).replaceAll("\\[[^\\]]*?\\]", "");
				EQ.put("quality", q);
			}
			//keep positional relations associated with the first element			
			//List<Element> rels = XPath.selectNodes(root, "//relation[@to='"+sid+"'|@from='"+sid+"']");
			List<Element> rels = XPath.selectNodes(root, "//relation[@from='"+sid+"']");
			String relationalquality = "";
			String qualitymodifier = "";
			for(Element rel: rels){
				String relname = rel.getAttributeValue("name");
				String toid = rel.getAttributeValue("to");
				//String fromid = rel.getAttributeValue("from");
				//String lid = toid.compareTo("sid")==0? fromid : toid;
				String toname = this.getStructureName(root, toid);
				if(relname.matches("\\(("+ChunkedSentence.positionprep+")\\).*")){
					EQ.put("entitylocator", toname);//TODO chained part_of relations??
					this.keyentitylocator = toname;
				}else if(EQ.get("quality").compareTo("")==0){//quality not found, turn relation to quality, toid to qualitymodifier
					relationalquality += relname+"+";
					qualitymodifier +=toname+"+";
				}
			}
			if(relationalquality.length()>0){
				relationalquality = relationalquality.replaceFirst("\\+$", "");
				String[] qs = relationalquality.split("\\+");
				String[] qms = qualitymodifier.split("\\+");
				for(int i = 0; i<qs.length; i++){
						EQ.put("quality", qs[i]);
						EQ.put("qualitymodifier", qms[i]);
						EQs.add((Hashtable)EQ.clone());
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return EQs;
	}


	private void initEQHash(Hashtable<String, String> EQ) {
		EQ.put("entity", "");
		EQ.put("entityid", "");
		EQ.put("quality", "");
		EQ.put("qualitynegated", "");
		EQ.put("qnparent", "");
		EQ.put("qnparentid", "");
		EQ.put("qualityid", "");
		EQ.put("qualitymodifier", "");
		EQ.put("qualitymodifierid", "");
		EQ.put("entitylocator", "");
		EQ.put("entitylocatorid", "");
	}

	/**
	 * if all states hold a binary value, return true, otherwise return false
	 * @param states
	 * @return
	 */
	private boolean isBinary(List<Element> states) {
		if(states.size()==0) return false;
		try{
		for(Element state: states){
			Element text = (Element)XPath.selectSingleNode(state, ".//text");
			String value = text.getTextTrim();
			if(!value.matches("("+ChunkedSentence.binaryTvalues+"|"+ChunkedSentence.binaryFvalues+")")){
				return false;
			}
		}
		}catch(Exception e){
			e.printStackTrace();
		}
		return true;
	}


	/**
	 * step 1: process Character Statements
	 * the set of character statements is expected to generate an E that is the subject for the state statements
	 * while it may also generate additional EQ statements
	 * @param chars: a set of statements with type="character"
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Element processCharacter(List<Element> chars, String src, Element root) {
		Element key = null;
		//ArrayList<Element> purge = new ArrayList<Element>();
		try{
			key = (Element)XPath.selectSingleNode(chars.get(0), ".//structure[@name!='whole_organism']");
			//TODO: what if key is null
			keyentity = this.getStructureName(root, key.getAttributeValue("id"));
			//boolean findstructure = false;
			for(Element statement: chars){
				List<Element> structures = statement.getChildren("structure");			
				/*for(Element e: structures){
					//expect all characters of whole_organism are CHARACTERS, which are identified by (name=value)
					//if not, increment troubles count
					if(e.getAttributeValue("name").contains("whole_organism")){
						//check e's characters
						int troubles = 0;
						List<Element> children = e.getChildren();
						for(Element c:  children){
							if(c.getAttributeValue("name").compareTo(c.getAttributeValue("value"))!=0){
								troubles++;					
							}
						}
						if(troubles == 0) purge.add(e); //this whole_organism is not useful
					}else{
						//return the first real structure element 
						if(!findstructure){
							findstructure = true;
							key = e; //find the key structure
							break;
						}					
					}
				}*/
				//generate other EQ statements from this statement
				//for(Element e: purge) e.detach();
				Element text = (Element)XPath.selectSingleNode(statement, ".//text");
				structures = XPath.selectNodes(statement, ".//structure");
				List<Element> relations = XPath.selectNodes(statement,".//relation");
				createEQsfromStatement(src, root, text, structures, relations, true);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return key;
	}

	/**
	 * step 2: process State Statements
	 * Use the name of E to replace the "whole_organism" placeholder in the state statements and generate EQ statements
	 * the state statements may also generate additional EQ statements
	 * If the name of E is also contained in the states, then use the name in the states
	 * @param key
	 * @param states
	 */
	@SuppressWarnings("unchecked")
	private void createEQsFromStateStatements(Element key, List<Element> states, String src, Element root) {
		try{
			for(Element statement : states){
				//fill whole_organism place-holder with a real structure
				List<Element> whole_organism = XPath.selectNodes(statement, ".//structure[@name='whole_organism']");
				for(Element wo : whole_organism){
					wo.setAttribute("name", key.getAttributeValue("name"));
					changeIdsInRelations(wo.getAttributeValue("id"), key.getAttributeValue("id"), root);
					wo.setAttribute("id", key.getAttributeValue("id"));
					if(key.getAttribute("constraint")!=null){
						wo.setAttribute("constraint", key.getAttributeValue("constraint"));
					}
				}
				//generate other EQ statements from this statement
				Element text = (Element)XPath.selectSingleNode(statement, ".//text");
				List<Element> structures = XPath.selectNodes(statement, ".//structure");
				//relations should include those in this state statement and those in character statement
				List<Element> relations = XPath.selectNodes(statement,".//relation"); 
				relations.addAll(XPath.selectNodes(root, ".//statement[@statement_type='character']/relation"));
				createEQsfromStatement(src, root, text, structures, relations, false);
			}			
		}catch(Exception ex){
			ex.printStackTrace();
		}		
	}

	/**
	 * search in all relations in root and replace oldid with newid for all from and to attributes
	 * @param oldid
	 * @param newid
	 * @param root
	 */
	@SuppressWarnings("unchecked")
	private void changeIdsInRelations(String oldid,
			String newid, Element root) {
		try{
			List<Element> rels = XPath.selectNodes(root, "//relation[@to='"+oldid+"'|@from='"+oldid+"']");
			for(Element rel : rels){
				if(rel.getAttributeValue("to").compareTo(oldid)==0) rel.setAttribute("to", newid);
				if(rel.getAttributeValue("from").compareTo(oldid)==0) rel.setAttribute("from", newid);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}


	/**
	 * 
	 * @param src
	 * @param root
	 * @param textelement
	 * @param structures
	 * @param relations
	 * @param keyelement if true, save its entitylocator info in the field entitylocator
	 */
	private void createEQsfromStatement(String src, Element root,  Element textelement, List<Element> structures,
			List<Element> relations, boolean keyelement) {
		String text = textelement.getText();
		//System.out.println("text::"+text);
		
		Hashtable<String, String> rels = relationHash(relations);
		
		//process structures: output
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element struct = it.next();
			createEQs4Structure(src, root, text, struct, rels, keyelement);
		}
	}


	private Hashtable<String, String> relationHash(List<Element> relations) {
		//process relations first and hold the information in hashtable
		Hashtable<String, String> rels = new Hashtable<String, String> (); //fromstructureid => (relation name) tostructureid 
		Iterator<Element> it = relations.iterator();
		while(it.hasNext()){
			Element rel = it.next();
			String fromid = rel.getAttributeValue("from");
			String toid = rel.getAttributeValue("to");
			String relname = rel.getAttributeValue("name").trim();
			String neg = rel.getAttributeValue("negation");
			if(neg.compareTo("true")==0){
				relname = "not "+relname+"";
			}
			if(rels.get(fromid)==null){
				rels.put(fromid, "("+relname+")"+toid);
			}else{
				rels.put(fromid, rels.get(fromid)+"#("+relname+")"+toid);
			}
		}
		return rels;
	}

	private void insertEQs2Table(Hashtable<String, String> EQ) {
		String src = EQ.get("src") ;
		String characterid = EQ.get("characterid");
		String stateid = EQ.get("stateid");
		String text = EQ.get("text") ;
		String entity = EQ.get("entity");
		String entityid = EQ.get("entityid");
		String quality = EQ.get("quality");
		String qualitynegated = EQ.get("qualitynegated");
		String qnparent = EQ.get("qnparent"); 
		String qnparentid = EQ.get("qnparentid");
		String qualityid = EQ.get("qualityid");
		String qualitymodifier = EQ.get("qualitymodifier");
		String qualitymodifierid = EQ.get("qualitymodifierid");
		String entitylocator = EQ.get("entitylocator");
		String entitylocatorid = EQ.get("entitylocatorid");
		String q = "insert into "+this.outputtable+" (source, characterid, stateid, description, entity, entityid, quality, quality_negated, qn_parent, qn_parentid, qualityid, qualitymodifier, qualitymodifierid, entitylocator, entitylocatorid) values " +
				"('"+src+"','"+characterid+"','"+stateid+"','"+text+"','"+ entity+"','"+ entityid+"','"+ quality+"','"+ qualitynegated+"','"+ qnparent+"','"+ qnparentid+"','"+ qualityid+"','"+qualitymodifier+"','"+ qualitymodifierid+"','"+ entitylocator+"','"+ entitylocatorid+"')";
		try{
			Statement stmt = conn.createStatement();
			stmt.execute(q);

			//quality and qualitynegated can not both hold values!
			if(quality.length()>0 || entitylocator.length()>0){
				System.out.println("EQ::[E]"+entity+" [Q]"+quality+(qualitymodifier.length()>0? " [QM]"+qualitymodifier :"")+(entitylocator.length()>0? " [EL]"+entitylocator :""));
			}
			if(qualitynegated.length()>0){
				System.out.println("EQ::[E]"+entity+" [QN]"+qualitynegated+(qualitymodifier.length()>0? " [QM]"+qualitymodifier :"")+(entitylocator.length()>0? " [EL]"+entitylocator :""));
			}
			}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 *  [8]Armbruster_2004.xml_0638f15b-0de4-45fd-a3af-b1d209cea9d3.xml
		text::Walls of metapterygoid channel
		text::lateral wall slightly smaller to just slightly larger than mesial wall, or absent
		EQ::[E]lateral wall [Q]smaller [slightly]
		EQ::[E]lateral wall [Q]larger [just slightly] [QM]mesial wall
		EQ::[E]lateral wall [Q]absent
		text::mesial wall much taller
		EQ::[E]mesial wall [Q]taller [much]
	 * @param entity
	 * @return
	 */
	private boolean related2KeyEntity(String entity) {
		if(entity.contains(this.keyentity)) return true;
		return false;
	}


	/**
	 * 
	 * @param root
	 * @param structids: 1 or more structids
	 * @return
	 */
	private String getStructureName(Element root, String structids) {
		String result = "";
		try{
			String[] ids = structids.split("\\s+");
			for(String structid: ids){
				Element structure = (Element)XPath.selectSingleNode(root, "//structure[@id='"+structid+"']");
				String sname = "";
				if(structure==null){
					sname="REF"; //this should never happen
				}else{
					sname = ((structure.getAttribute("constraint")==null? "" : structure.getAttributeValue("constraint"))+" "+structure.getAttributeValue("name"));
				}
				result += sname+";";
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		result = result.replaceAll("\\s+", " ").replaceFirst(";$", "").trim();
		return result;
	}

	/**
	 * 
	 * @param src
	 * @param root
	 * @param text
	 * @param struct
	 * @param keyelement TODO
	 */
	@SuppressWarnings("unchecked")
	private void createEQs4Structure(String src, Element root, String text, Element struct, Hashtable<String, String> relations, boolean keyelement) {
		if(keyelement) 		System.out.println("text::"+text);
		Hashtable<String, String> srcids = getStateId(root, struct);
		String characterid = srcids.get("characterid");
		String stateid = srcids.get("stateid");
		if(stateid.length()>0) stateids.add(stateid);
		String structid = struct.getAttributeValue("id");
		String[] rels = null;
		String arelation = relations.get(structid);
		if(arelation!=null) rels = arelation.split("#");
		String structname = this.getStructureName(root, structid);
		try{
			List<Element> chars = XPath.selectNodes(struct, ".//character");
			Iterator<Element> it = chars.iterator();
			boolean hascharacter = false;
			while(it.hasNext()){
				hascharacter = true;
				Element chara = it.next();
				//characters
				String quality = formQualityValueFromCharacter(chara);
				//constraints
				String qualitymodifier = "";
				if(chara.getAttribute("constraintid")!=null){
					qualitymodifier =this.getStructureName(root, chara.getAttributeValue("constraintid"));
				}
				//relations: may be entitylocators or qualitymodifiers
				String entitylocator = "";
				if(rels!=null){
					for(String r : rels){
						String toid = r.replaceFirst(".*?\\)", "").trim();
						String toname = this.getStructureName(root, toid);
						if(r.matches("\\(("+ChunkedSentence.positionprep+")\\).*")){ //entitylocator							
							entitylocator += toname+";";
						}else if(r.matches("(with).*")){							
							//do nothing
						}else if(r.matches("(without).*")){							
							//output absent as Q for toid
							Hashtable<String, String> EQ = new Hashtable<String, String>();
							initEQHash(EQ);
							EQ.put("src", src);
							EQ.put("characterid", characterid);
							EQ.put("stateid", stateid);
							EQ.put("text", text); 
							EQ.put("entity", toname);
							EQ.put("quality", "absent");
							EQ.put("type", keyelement? "character" : "state");
							allEQs.add(EQ);
							//addEQStatement(src, characterid, stateid, text, toname,"", "absent", "","","","", "", "", "", "", keyelement); //without entity locators, which are treated in relation processing 
						}else{
							qualitymodifier +=toname+";";
						}
					}
					entitylocator = entitylocator.replaceFirst(";$", "");
					qualitymodifier = qualitymodifier.replaceFirst(";$", "");
				}
				if(keyelement && structname.compareTo(keyentity)==0) this.keyentitylocator = entitylocator;
				Hashtable<String, String> EQ = new Hashtable<String, String>();
				initEQHash(EQ);
				EQ.put("src", src);
				EQ.put("characterid", characterid);
				EQ.put("stateid", stateid);
				EQ.put("text", text); 
				EQ.put("entity", structname);
				EQ.put("quality", quality);
				EQ.put("qualitymodifier", qualitymodifier);
				EQ.put("entitylocator", entitylocator);
				EQ.put("type", keyelement? "character" : "state");
				allEQs.add(EQ);
				//addEQStatement(src, characterid, stateid, text, structname,"", quality,"","","", "", qualitymodifier, "", entitylocator, "", keyelement); //without entity locators, which are treated in relation processing 
			}
			if(!hascharacter && rels != null){
				//this is the case where the structure's character information is expressed in the relations (it has no character elements, but is involved in some relations)
				for(String rel : rels){ //rel: (covered in)o621
					//make "covered in" a quality and "o621" quality modifier
					if(!rel.matches("\\(("+ChunkedSentence.positionprep+")\\).*")){//exclude Locator relations
						String toid = rel.replaceFirst(".*?\\)", "").trim();
						String quality = rel.replace(toid, "").replaceAll("[()]", "").trim();
						String qualitymodifier = this.getStructureName(root, toid);	
						Hashtable<String, String> EQ = new Hashtable<String, String>();
						initEQHash(EQ);
						EQ.put("src", src);
						EQ.put("characterid", characterid);
						EQ.put("stateid", stateid);
						EQ.put("text", text); 
						EQ.put("entity", structname);
						EQ.put("quality", quality);
						EQ.put("qualitymodifier", "qualitymodifier");
						EQ.put("type", keyelement? "character" : "state");
						allEQs.add(EQ);
						//addEQStatement(src, characterid, stateid, text, structname,"", quality, "","","", "", qualitymodifier, "", "", "", keyelement); //without entity locators, which are treated in relation processing 
					}else{
						String toid = rel.replaceFirst(".*?\\)", "").trim();
						String entitylocator = this.getStructureName(root, toid);		
						if(keyelement && structname.compareTo(keyentity)==0) this.keyentitylocator = entitylocator;
						Hashtable<String, String> EQ = new Hashtable<String, String>();
						initEQHash(EQ);
						EQ.put("src", src);
						EQ.put("characterid", characterid);
						EQ.put("stateid", stateid);
						EQ.put("text", text); 
						EQ.put("entity", structname);
						EQ.put("entitylocator", entitylocator);
						EQ.put("type", keyelement? "character" : "state");
						allEQs.add(EQ);
						//addEQStatement(src, characterid, stateid, text, structname,"", "", "","","","", "", "", entitylocator, "", keyelement); //without entity locators, which are treated in relation processing 
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}		
	}


	private String formQualityValueFromCharacter(Element chara) {
		String charatype = chara.getAttribute("char_type")!=null ? "range" : "discrete"; 
		String quality = "";
		if(charatype.compareTo("range")==0){
			quality = chara.getAttributeValue("from")+
					  " "+
					  (chara.getAttribute("from_unit")!=null? chara.getAttributeValue("from_unit"): "")+
					  " to "+
					  chara.getAttributeValue("to")+
					  " "+
					  (chara.getAttribute("to_unit")!=null?  chara.getAttributeValue("to_unit") : "");

		}else{
			quality = (chara.getAttribute("modifier")!=null && chara.getAttributeValue("modifier").matches(".*?\\bnot\\b.*")? "not" : "")	
			          +" "+chara.getAttributeValue("value")+
			          " "+
			 		  (chara.getAttribute("unit")!=null? chara.getAttributeValue("unit"): "")+"["+
			 		  (chara.getAttribute("modifier")!=null? chara.getAttributeValue("modifier").replaceAll("\\bnot\\b;?", "") : "")+"]";
			         		  
		}
		quality = quality.replaceAll("\\[\\]", "").replaceAll("\\s+", " ").trim();
		return quality;
	}

	/**
	 * find the <statement> parent of the struct from the root
	 * return character id and state id
	 * @param root
	 * @param struct
	 * @return characterid and stateid
	 */
	private Hashtable<String, String> getStateId(Element root, Element struct) {
		Hashtable<String, String> srcids = new Hashtable<String, String>();
		Element statement = struct.getParentElement();
		srcids.put("characterid", statement.getAttributeValue("character_id"));
		String stateid = statement.getAttribute("state_id")==null? "" : statement.getAttributeValue("state_id");
		srcids.put("stateid", stateid);		
		return srcids;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String srcdir = "C:/Documents and Settings/Hong Updates/Desktop/Australia/phenoscape-fish-source/target/final";
		String database = "phenoscape";
		String outputtable = "biocreative_nexml2eq";
		XML2EQ x2e = new XML2EQ(srcdir, database, outputtable);
		x2e.outputEQs();
	}

}
