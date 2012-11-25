/**
 * 
 */
package outputter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 *This class output EQ statements from the XML files output by CharaParser
 *The XML files are in \target\final
 */
/**
 * Entity: eye
 * EntityID: TAO:1234567
 * Quality:
 * Quality-Negated: not round
 * QN-Parent:shape
 * QN-ParentID: PATO:0000113(id for shape)
 * QualityID: PATO:0000111(id for round)
 * 
 * 
 */
public class XML2EQ {
	private File source;
	private int unknownid = 0;
	private String outputtable;
	private String tableprefix;
	private String glosstable;
	// private String benchmarktable;
	private Connection conn;
	private String username = "biocreative";
	private String password = "biocreative";
	private int count = 0;
	// private String keyentity = null;
	private List<String> keyentities;
	private String keyentitylocator = null;
	private ArrayList<Hashtable<String, String>> allEQs = null;
	private HashSet<String> stateids = new HashSet<String>();
	private static ArrayList<String> serenostyle = new ArrayList<String>();
	private String characters = null;
	private XPath pathCharacterStatement;
	private XPath pathStateStatement;
	private XPath pathNonWholeOrganismStructure;
	private XPath pathText;
	private XPath pathWholeOrganismStructure;
	private XPath pathStructure;
	private XPath pathWholeOrgStrucChar;
	private XPath pathCharacter;
	private XPath pathText2;
	private XPath pathRelation;
	private XPath pathRelationUnderCharacter;
	private XPath pathStructure2;
	private XPath pathWithHaveHasRelation;
	private XPath pathRnageValueCharacter;
	private XPath pathCountStructure;
	private Hashtable<String, String> entityhash = new Hashtable<String, String>();
	private Pattern p2 = Pattern.compile("(.*?)(\\d+) to (\\d+)");
	private Pattern p1 = Pattern.compile("(first|second|third|forth|fouth|fourth|fifth|sixth|seventh|eighth|ninth|tenth)\\b(.*)");
	private String binaryTvalues = "true|yes|usually";
	private String binaryFvalues = "false|no|rarely";
	private String positionprep = "of|part_of|in|on|between";
	private String selfReference = "counterpart";//Extendible
	private String contact="connection|contact|interconnection";//Extendible
	
	private Hashtable<String, String> ossification = new Hashtable<String,String>();
	//populate in constructor, <"Q","E,Q"> eg. <"unossified", "ossification,absent">;
	
	private List<String> spatialHeadNoun = new ArrayList<String>();
	//By Zilong:
	//sometimes, spatial terms could be used as adjectives to modify head nouns. 
	//Instead of directly using <spatial terms+head nouns> when searching the ontology,
	//the program should interpret the pattern as a part_of relation. 
	//eg. "anterior coracoid process" should be interpreted as "anterior region(part_of(coracoid))"
	//This list contains all identified head nouns
	
	private ArrayList<String> spatialterms = new ArrayList<String>();
	
	static {
		serenostyle.add("sereno");
		serenostyle.add("martinez");
		serenostyle.add("earlyevolutionofarchosaurs");
	}

	/**
	 * 
	 */
	/*
	 * public XML2EQ(String sourcedir, String database, String outputtable, String benchmarktable, String prefix, String glosstable) {
	 * this.source = new File(sourcedir);
	 * this.outputtable = outputtable;
	 * this.tableprefix = prefix;
	 * this.glosstable = glosstable;
	 * this.benchmarktable = benchmarktable;
	 * try{
	 * if(conn == null){
	 * Class.forName("com.mysql.jdbc.Driver");
	 * String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
	 * conn = DriverManager.getConnection(URL);
	 * Statement stmt = conn.createStatement();
	 * //label and id fields are ontology-related fields
	 * //other fields are raw text
	 * //entity and quality fields are atomic
	 * //qualitynegated fields are alternative to quality and is composed as "not quality" for qualitynegated, "not(quality)" for qualitynegatedlabel, the "quality" has id
	 * qualityid
	 * //qualitymodifier/label/id and entitylocator/label/id may hold multiple values separated by "," which preserves the order of multiple values
	 * stmt.execute("drop table if exists "+ outputtable);
	 * stmt.execute("create table if not exists "+outputtable+
	 * " (id int(11) not null unique auto_increment primary key, source varchar(500), characterID varchar(100), stateID varchar(100), description text, " +
	 * "entity varchar(200), entitylabel varchar(200), entityid varchar(200), " +
	 * "quality varchar(200), qualitylabel varchar(200), qualityid varchar(200), " +
	 * "qualitynegated varchar(200), qualitynegatedlabel varchar(200), " +
	 * "qnparentlabel varchar(200), qnparentid varchar(200), " +
	 * "qualitymodifier varchar(200), qualitymodifierlabel varchar(200), qualitymodifierid varchar(200), " +
	 * "entitylocator varchar(200), entitylocatorlabel varchar(200), entitylocatorid varchar(200), " +
	 * "countt varchar(200))");
	 * 
	 * 
	 * pathCharacterStatement = XPath.newInstance(".//statement[@statement_type='character']");
	 * pathStateStatement = XPath.newInstance(".//statement[@statement_type='character_state']");
	 * pathNonWholeOrganismStructure = XPath.newInstance(".//structure[@name!='whole_organism']");
	 * pathText = XPath.newInstance(".//text");
	 * pathWholeOrganismStructure = XPath.newInstance(".//structure[@name='whole_organism']");
	 * pathStructure = XPath.newInstance(".//structure");
	 * path7 = XPath.newInstance(".//structure[@name='whole_organism']/character");
	 * pathCharacter = XPath.newInstance(".//character");
	 * pathText2 = XPath.newInstance(".//text");
	 * pathRelation = XPath.newInstance(".//relation");
	 * pathRelationUnderCharacter = XPath.newInstance(".//statement[@statement_type='character']/relation");
	 * pathStructure2 = XPath.newInstance(".//structure");
	 * pathWithHaveHasRelation = XPath.newInstance("//relation[@name='with'] | //relation[@name='have'] | //relation[@name='has']");
	 * pathRnageValueCharacter = XPath.newInstance("//character[@char_type='range_value']");
	 * pathCountStructure = XPath.newInstance("//structure[character[@name='count']]");
	 * }
	 * }catch(Exception e){
	 * e.printStackTrace();
	 * }
	 * }
	 */

	public XML2EQ(String sourcedir, String database, String outputtable, String prefix, String glosstable) throws Exception {
		this.source = new File(sourcedir);
		this.outputtable = outputtable;
		this.tableprefix = prefix;
		this.glosstable = glosstable;
		this.keyentities = new ArrayList<String>();
		this.ossification.put("unossified", "ossification,absent");
		
		//populate spatialHeadNoun here:
		this.spatialHeadNoun.add("coronoid");
		this.spatialHeadNoun.add("process");
		this.spatialHeadNoun.add("coracoid");
		//TODO
		
		if (conn == null) {
			Class.forName("com.mysql.jdbc.Driver");
			String URL = "jdbc:mysql://localhost/" + database + "?user=" + username + "&password=" + password;
			conn = DriverManager.getConnection(URL);
			Statement stmt = conn.createStatement();
			
			ResultSet rs = stmt.executeQuery("select distinct term from uniquespatialterms");
			while(rs.next()){
				spatialterms.add(rs.getString("term"));
			}
			spatialterms.add("accessory");
			// label and id fields are ontology-related fields
			// other fields are raw text
			// entity and quality fields are atomic
			// qualitynegated fields are alternative to quality and is composed as "not quality" for qualitynegated, "not(quality)" for qualitynegatedlabel, the "quality" has id
			// qualityid
			// qualitymodifier/label/id and entitylocator/label/id may hold multiple values separated by "," which preserves the order of multiple values
			stmt.execute("drop table if exists " + outputtable);
			stmt.execute("create table if not exists " + outputtable
					+ " (id int(11) not null unique auto_increment primary key, source varchar(500), characterID varchar(100), stateID varchar(100), description text, "
					+ "entity varchar(200), entitylabel varchar(200), entityid varchar(200), " + "quality varchar(200), qualitylabel varchar(200), qualityid varchar(200), "
					+ "qualitynegated varchar(200), qualitynegatedlabel varchar(200), " + "qnparentlabel varchar(200), qnparentid varchar(200), "
					+ "qualitymodifier varchar(200), qualitymodifierlabel varchar(200), qualitymodifierid varchar(300), "
					+ "entitylocator varchar(200), entitylocatorlabel varchar(200), entitylocatorid varchar(200), " + "countt varchar(200))");

			pathCharacterStatement = XPath.newInstance(".//statement[@statement_type='character']");
			pathStateStatement = XPath.newInstance(".//statement[@statement_type='character_state']");
			pathNonWholeOrganismStructure = XPath.newInstance(".//structure[@name!='whole_organism']");
			pathText = XPath.newInstance(".//text");
			pathWholeOrganismStructure = XPath.newInstance(".//structure[@name='whole_organism']");
			pathStructure = XPath.newInstance(".//structure");
			pathWholeOrgStrucChar= XPath.newInstance(".//structure[@name='whole_organism']/character");
			pathCharacter = XPath.newInstance(".//character");
			pathText2 = XPath.newInstance(".//text");
			pathRelation = XPath.newInstance(".//relation");
			pathRelationUnderCharacter = XPath.newInstance(".//statement[@statement_type='character']/relation");
			pathStructure2 = XPath.newInstance(".//structure");
			pathWithHaveHasRelation = XPath.newInstance("//relation[@name='with'] | //relation[@name='have'] | //relation[@name='has']");
			pathRnageValueCharacter = XPath.newInstance("//character[@char_type='range_value']");
			pathCountStructure = XPath.newInstance("//structure[character[@name='count']]");
		}
	}

	@SuppressWarnings("unchecked")
	public void outputEQs() throws Exception {
		File[] xmlfiles = this.source.listFiles();
		// try{
		for (File f : xmlfiles) {
			String src = f.getName();
			SAXBuilder builder = new SAXBuilder();
			Document xml = builder.build(f);
			Element root = xml.getRootElement();
			with2partof(root);
			removeCategoricalRanges(root);
			// expect 1 file to have 1 character statement and n statements, but for generality, use arrayList for characterstatements too.
			//characterstatements are character descriptions
			List<Element> characterstatements = pathCharacterStatement.selectNodes(root);
			//statestatements are state descriptions
			List<Element> statestatements = pathStateStatement.selectNodes(root);
			
			integrateWholeOrganism4CharacterStatements(characterstatements, root);
			repairWholeOrganismOnlyCharacterStatements(characterstatements, root);
			// if(count!= 67){ count++; continue;}
			System.out.println();
			System.out.println("[" + count + "]" + src);
			count++;
			// reset
			keyentities = new ArrayList<String>();
			keyentitylocator = null;
			allEQs = new ArrayList<Hashtable<String, String>>();// used to hold all EQ statement generated from this character unit (1 file holds 1 character unit)
			String author = src.replaceFirst("_.*$", "").toLowerCase();
			if (XML2EQ.serenostyle.contains(author)) {
				createEQs4CharacterUnitInSerenoStyle(characterstatements, statestatements, src, root);
			} else {
				createEQs4CharacterUnit(characterstatements, statestatements, src, root); // the set of statements related to a character (one of the statement is the character
																							// itself)
			}
			outputEQs4CharacterUnit();
		}
		// discardNonTestCharacterUnits();
		// }catch(Exception e){
		// e.printStackTrace();
		// }
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

		List<Element> charas = this.pathRnageValueCharacter.selectNodes(root);
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

		List<Element> structs = this.pathCountStructure.selectNodes(root);
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
	 * A with B => B part_of A
	 * 
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
	 * use workbench to select/keep only the ones in the workbench
	 */
	/*
	 * private void discardNonTestCharacterUnits() throws Exception {
	 * Statement stmt = conn.createStatement();
	 * stmt.execute("delete from "+this.outputtable+" where source not in (select source from "+this.benchmarktable+ ")");
	 * }
	 */

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
	 * perform global sanity check and normalization
	 * global sanity check: one state text may generate n EQs but one of them must hold keyentities
	 * in the following example, EQ 2 and 3 are wrong
	 * 
	 * [11]Armbruster_2004.xml_0ada121b-dfa5-4093-8ceb-483163cae12e.xml
	 * text::Lateral wall of metapterygoid channel
	 * text::absent
	 * 1 EQ::[E]lateral wall [Q]absent [EL]metapterygoid channel
	 * text::just a slight ridge
	 * 2 EQ::[E]ridge [Q]slight [just]
	 * text::triangular
	 * 3 EQ::[E]lateral wall [Q]triangular [EL]metapterygoid channel
	 * text::broad ridge, perpendicular to metapterygoid
	 * 4 EQ::[E]ridge [Q]broad [QM]metapterygoid
	 * text::long and rounded along entire length
	 * 5 EQ::[E]lateral wall [Q]long [EL]metapterygoid channel
	 * 6 EQ::[E]lateral wall [Q]rounded [along entire length] [EL]metapterygoid channel
	 */
	private void outputEQs4CharacterUnit() throws Exception {
		// sanity check has problems

		/*
		 * for(String stateid: stateids){
		 * ArrayList<Hashtable<String, String>> problems = new ArrayList<Hashtable<String,String>>();
		 * for(Hashtable<String,String> EQ: allEQs){
		 * if(EQ.get("stateid").compareTo(stateid)==0){
		 * if(this.isRelated2KeyEntity(EQ.get("entity")) || this.isRelated2KeyEntity(EQ.get("entitylocator"))){
		 * problems = new ArrayList<Hashtable<String, String>>(); //passed, reset problems
		 * break;
		 * }else{
		 * problems.add(EQ);
		 * }
		 * }
		 * }
		 * //EQ failed sanity check if landed here,
		 * if(problems.size()>0){
		 * repairProblemEQs(problems);
		 * }
		 * }
		 */

		// filter allEQs to match human cruation practice: only the keyentities are mentioned
		// remove EQs associated with a state that are not the first EQ or that are not related to entity or entity locator
		/*
		 * for(String stateid: stateids){
		 * ArrayList<Hashtable<String, String>> problems = new ArrayList<Hashtable<String,String>>();
		 * int count = 0;
		 * for(Hashtable<String,String> EQ: allEQs){
		 * if(EQ.get("stateid").compareTo(stateid)==0){
		 * if(count>0 && (!this.isRelated2KeyEntity(EQ.get("entity")) || !this.isRelated2KeyEntity(EQ.get("entitylocator")))){
		 * allEQs.remove(EQ);
		 * }
		 * }
		 * }
		 * }
		 */

		String text = "";
		
		/** 
		 * NORMALIZATION PROCESS
		 **/
		for (Hashtable<String, String> EQ : allEQs) {
			// 1. entitylocator inherit from the character statement
			/*
			 * [8]Armbruster_2004.xml_0638f15b-0de4-45fd-a3af-b1d209cea9d3.xml
			 * text::Walls of metapterygoid channel
			 * text::lateral wall slightly smaller to just slightly larger than mesial wall, or absent
			 * EQ::[E]lateral wall [Q]smaller [slightly]
			 * EQ::[E]lateral wall [Q]larger [just slightly] [QM]mesial wall
			 * EQ::[E]lateral wall [Q]absent
			 * text::mesial wall much taller
			 * EQ::[E]mesial wall [Q]taller [much]
			 */
			String entitylocator = EQ.get("entitylocator");
			String entity=EQ.get("entity");
			String qualitymodifier=EQ.get("qualitymodifier");
			String quality=EQ.get("quality");
			
			if (EQ.get("type").compareTo("state") == 0 && this.keyentitylocator != null) {
				// EQ based on a state statement
				// this is not a binarystate statement
				if (EQ.get("entitylocator").compareTo(this.keyentitylocator) != 0 && isRelated2KeyEntities(EQ.get("entity"))) {
					// to inhere the entitylocator, this entity must be somewhat related to this.keyentity
					// "lateral wall" is related to "walls" of ...
					entitylocator += "," + this.keyentitylocator;
				}
			}
			entitylocator = entitylocator.trim().replaceAll("(^,|,$)", "");
			EQ.put("entitylocator", entitylocator);

			// 2. negation
			if (EQ.get("quality").startsWith("not ")) {
				EQ.put("qualitynegated", EQ.get("quality"));
				EQ.put("quality", "");
			}

			// finally
			if (EQ.get("description").compareTo(text) != 0) {
				System.out.println("text::" + EQ.get("description"));
				text = EQ.get("description");
			}
			
			/**
			 * Start adjusting results based on identified patterns
			 * */
			
			/*
			 * Changed by Zilong: change any self-reference word to the keyentity(es)
			 * To add any new self-reference word, please modify the instance variable
			 * "selfReference." 
			 * 
			 * */
			
			if(entity.toLowerCase().trim().matches("("+this.selfReference+")")){
				EQ.put("entity", this.keyentities.get(0));
			}
			if(entitylocator.toLowerCase().trim().matches("("+this.selfReference+")")){
				EQ.put("entitylocator", this.keyentities.get(0));
			}
			if(qualitymodifier.toLowerCase().trim().matches("("+this.selfReference+")")){
				EQ.put("qualitymodifier", this.keyentities.get(0));
			}
			/*End dealing with self reference terms*/
			
			
			/*
			 * Changed by Zilong: deal with relationship such as connect, contact, interconnect etc.
			 * Transform the result from CharaParser which is of the form:
			 * connection[E] between A[EL] and B[EL] <some text>[Q] -the quality could be misidentified
			 * to the form:
			 * A[E] is in connection with[Q] B[QM]
			 * 
			 * */
			if(entity.toLowerCase().trim().matches("("+this.contact+")")){
				EQ.put("entity", entitylocator.split(",")[0]);//the first EL as E
				EQ.put("quality", "in contact with"); //"in contact with" can be found in ontos
				EQ.put("qualitymodifier", entitylocator.replaceFirst("[^,]*,?", "").trim());//the rest of EL is QM
				EQ.put("entitylocator", "");//empty the EL
			}
			/*End handling the "contact" type relation*/
			
			/*
			 * Changed by Zilong: deal with terms like "unossified" 
			 * Transform the result from an adjective word (binary form) to "noun+present/absent" 
			 * */
			quality = quality.toLowerCase().trim();
			if(ossification.containsKey(quality)){
				EQ.put("entity", entity+" "+ossification.get(quality).split(",")[0]);
				EQ.put("quality", ossification.get(quality).split(",")[1]);
			}
			/*end handling the "unossified" like term*/
			
			/*
			 * Changed by Zilong: deal with spatial terms. 
			 */
			String[] entityTerms=entity.toLowerCase().trim().split("\\s+");
			//if contains spatial terms
			if(this.spatialterms.contains(entityTerms[0])){
				//if the entity contains the spatial head noun 
				if(this.spatialHeadNoun.contains(entityTerms[entityTerms.length-1])){
					String ne=entityTerms[0]+" region";//spatial term + region
					String nel=entityTerms[entityTerms.length-1]+","+entitylocator;
					EQ.put("entity", ne);
					EQ.put("entitylocator", nel);
				}
			}
			
			
			
			
			/*case: Mesethmoid flares anteriorly-> E: anterior region(part_of(mesethmoid bone)), Q: decreased width*/
			/*need more supporting cases. For now, comment it out to avoid interference with cases like ventrally directed*/
			//String[] qualityTerms=quality.toLowerCase().trim().replaceAll("[\\[\\]]", "").split("\\s+");
//			if(this.spatialterms.contains(qualityTerms[qualityTerms.length-1].replaceFirst("ly$", ""))){
//				//if the quality contains the spatial term's adverb form.
//				String ne = qualityTerms[qualityTerms.length-1].replaceFirst("ly$", "")+" region";
//				String nel = entity+((entitylocator==null||entitylocator.equals(""))?"":(","+entitylocator));
//				String nq = quality.toLowerCase().trim().replaceAll("\\[.*\\]", "");
//				
//				EQ.put("entity", ne);
//				EQ.put("entitylocator", nel);
//				EQ.put("quality", nq);
//				
//			}
			/*end handling spatial terms*/
			EQ.put("quality", quality.toLowerCase().trim().replaceAll("[\\[\\]]", ""));
			
			this.insertEQs2Table(EQ);
		}

	}

	/**
	 * 
	 [11]Armbruster_2004.xml_0ada121b-dfa5-4093-8ceb-483163cae12e.xml
	 * text::Lateral wall of metapterygoid channel
	 * text::absent
	 * 1 EQ::[E]lateral wall [Q]absent [EL]metapterygoid channel
	 * text::just a slight ridge
	 * 2 EQ::[E]ridge [Q]slight [just]
	 * text::triangular
	 * 3 EQ::[E]lateral wall [Q]triangular [EL]metapterygoid channel
	 * text::broad ridge, perpendicular to metapterygoid
	 * 4 EQ::[E]ridge [Q]broad [QM]metapterygoid
	 * text::long and rounded along entire length
	 * 5 EQ::[E]lateral wall [Q]long [EL]metapterygoid channel
	 * 6 EQ::[E]lateral wall [Q]rounded [along entire length] [EL]metapterygoid channel
	 * 
	 * 
	 * 
	 * 
	 * turn EQ::[E]ridge [Q]slight [just] to
	 * EQ::[E]lateral wall [Q]ridge [just slight] [EL]metapterygoid channel
	 * 
	 * @param problems
	 *            : EQs failed the sanity check
	 */
	@SuppressWarnings("unused")
	private void repairProblemEQs(ArrayList<Hashtable<String, String>> problems) {
		// to repair the first EQ
		// EQ #2 in the above example
		Hashtable<String, String> EQ = problems.get(0);
		String olde = EQ.get("entity");
		String oldq = EQ.get("quality").replaceAll("[\\[\\]]", "");
		String oldqm = EQ.get("qualitymodifier");
		String oldel = EQ.get("entitylocator");
		for (String s : keyentities) {
			EQ.put("entity", s);
		}
		EQ.put("entitylocator", keyentitylocator == null ? "" : keyentitylocator);
		EQ.put("quality", olde + " [" + oldq + "]");
		EQ.put("qualitymodifier", oldel + "," + oldqm);
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
					this.changeIdsInRelations(woid, sid, root);
				}
			}
		}

	}

	/**
	 * For example, 1 character statement with 3 state statements
	 * <statement statement_type="character" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" seg_id="0">
	 * 	<text>Shape of ancistrine opercle (ordered )</text>
	 * 	<structure id="o650" name="whole_organism">
	 * 		<character name="shape" value="shape" constraint="of ancistrine opercle" constraintid="o651" />
	 * 	</structure>
	 * 	<structure id="o651" name="opercle" constraint="ancistrine" />
	 * </statement>
	 * 
	 * <statement statement_type="character_state" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" state_id="4a99e866-54d9-4875-8b5e-385427db1245" seg_id="0">
	 * 	<text>sickle-shaped (&lt;i&gt;Peckoltia&lt;/i&gt;-type )</text>
	 * 	<structure id="o652" name="whole_organism">
	 * 		<character name="shape" value="sickle-shaped" />
	 * 	</structure>
	 * </statement>
	 * 
	 * <statement statement_type="character_state" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" state_id="d53ba92f-0865-4456-9111-c6ff37fc624a" seg_id="0">
	 * 	<text>barshaped (&lt;i&gt;Ancistrus&lt;/i&gt;-type )</text>
	 * 	<structure id="o653" name="whole_organism">
	 * 		<character name="shape" value="barshaped" />
	 * 	</structure>
	 * </statement>
	 * 
	 * <statement statement_type="character_state" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" state_id="f56a9b6a-9720-437c-a1f4-60f01cd1bb15" seg_id="0">
	 * 	<text>oval or triangular</text>
	 * 	<structure id="o654" name="whole_organism">
	 * 		<character name="shape" value="oval" />
	 * 		<character name="shape" value="triangular" />
	 * 	</structure>
	 * </statement>
	 * 
	 * @param statements
	 */
	private void createEQs4CharacterUnit(List<Element> charstatements, List<Element> states, String src, Element root) throws Exception {

		this.addEQ4CharacterStatement(src, charstatements);
		// step 0: decide if a character is a binary (yes/no, true/false, rarely/usually types of states)
		if (isBinary(states)) {
			// the yes state = character description
			// the no state = negated character description
			// a negation may be to a relation (verb [A "not contact" B] or prep [A "not with" B]) or to a character state ("not expanded")

			// compose n EQs from character statement, because a character statement can describe two equally weighted characters ( A exits B and enters C)
			ArrayList<Hashtable<String, String>> EQs = processBinaryCharacter(charstatements, src, root);
			createEQsFromBinaryStates(states, src, EQs);
		} else {
			// on the first try, assuming the simple model:
			// 1 char statement holding 1 organ/process/entity
			// n states provide quality

			// step 1: process Character Statements
			// the set of character statements is expected to generate an E that is the subject for the state statements
			// while it may also generate additional EQ statements
			// also set keyentities and keyentitylocator fields
			List<Element> keys = processCharacter(charstatements, src, root);
			// step 2: process State Statements
			// Use E to replace the "whole_organism" placeholder in the state statements and generate EQ statements
			// the state statements may also generate additional EQ statements
			if (!keys.isEmpty()) {
				createEQsFromStateStatements(keys, states, src, root);
			}
		}
	}

	/**
	 * character: L1, L2, ..., Ln, V q(may contain QM, eg. length relative to eyes)
	 * states: V1, V2
	 * 
	 * so for each state [e.g., V1]: E = Ln, Q=V1 [q], QM=parse from q, EL=L1, ..., L(n-1)
	 * 
	 * @param charstatements
	 * @param states
	 * @param src
	 * @param root
	 */
	@SuppressWarnings("unchecked")
	private void createEQs4CharacterUnitInSerenoStyle(List<Element> charstatements, List<Element> states, String src, Element root) throws Exception {

		// collect category="character" terms from the glossarytable
		if (this.characters == null) {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct term from " + this.glosstable + " where category='character' " + "union " + "select distinct term from "
					+ this.tableprefix + "_term_category where category='character' ");
			while (rs.next()) {
				this.characters += rs.getString(1) + "|";
			}
			this.characters = characters.replaceFirst("\\|$", "");
		}
		// get E and ELs from character statement
		addEQ4CharacterStatement(src, charstatements);
		String chtext = charstatements.get(0).getChild("text").getTextTrim();
		chtext = markcharacters(chtext);
		System.out.println(chtext);
		chtext = chtext.replaceAll("\\(.*?\\)", "");
		String[] chparts = chtext.toLowerCase().split("\\s*,\\s*");
		List<Element> structs = pathStructure.selectNodes(charstatements.get(0));
		ArrayList<String> snames = new ArrayList<String>();
		for (Element struct : structs) {
			snames.add(this.getStructureName(root, struct.getAttributeValue("id")).replaceFirst("(?<=\\w)_(?=\\d+$)", " "));
		}
		String E = "";
		String ELs = "";
		for (int i = 0; i < chparts.length; i++) {
			String n = firstMatchedStructureName(chparts[i], snames, i);// match in absence of/before [character]
			if (n != null) {
				snames.remove(n);
				ELs = E + "," + ELs;
				E = n;
				String rest = chparts[i].replaceFirst(n, "").trim();
				
				String moreELs = "";
				while (rest.length() > 0) {
					n = firstMatchedStructureName(rest, snames, i * -1);
					if (n != null) {
						snames.remove(n);
						moreELs = moreELs + "," + n;
						rest = rest.replaceFirst(".*?\\b" + n, "").trim();
					} else {
						break;
					}
				}
				ELs = moreELs + "," + ELs;
			}
		}
		
		// get QM
		// need to be changed
		String QMs = "";
		for (String sname : snames) {// some remaining structures after [character] are QMs
			if (chtext.indexOf("] of " + sname) > 0) {// Postorbital, [form] of dorsal surface
				ELs = E + "," + ELs;
				E = sname;
			} else {
				QMs += sname + ",";
			}
		}
		ELs = ELs.replaceAll(",+", ",").replaceFirst("^,", "").replaceFirst(",$", "");

		// process states
		Hashtable<String, String> EQ = new Hashtable<String, String>();
		this.initEQHash(EQ);
		EQ.put("source", src);
		EQ.put("entity", E);
		EQ.put("entitylocator", ELs);
		EQ.put("type", "state");
		for (Element state : states) {
			Hashtable<String, String> EQc = (Hashtable<String, String>) EQ.clone();
			String description = state.getChild("text").getTextTrim();
			EQc.put("description", description);
			EQc.put("characterid", state.getAttributeValue("character_id"));
			EQc.put("stateid", state.getAttributeValue("state_id"));

			Element firststruct = (Element) state.getChildren("structure").get(0);
			if (!firststruct.getAttributeValue("name").contains("whole_organism")) {
				// noun as state
				String fsname = this.getStructureName(root, firststruct.getAttributeValue("id"));
				String characterstr = charactersAsString(root, firststruct);
				if (description.endsWith(fsname)) {// form: low crest (noun as state)
					EQc.put("quality", characterstr + " " + fsname);
					this.allEQs.add(EQc);
				} else {
					EQc.put("entitylocator", EQc.get("entity") + "," + EQc.get("entitylocator").replaceFirst(",$", ""));
					EQc.put("entity", fsname);
					EQc.put("quality", characterstr);
					this.allEQs.add(EQc);
				}
			} else {
				// collecting all characters of whole_organism
				List<Element> chars = pathWholeOrgStrucChar.selectNodes(state);
				for (Element chara : chars) {
					Hashtable<String, String> EQi = (Hashtable<String, String>) EQc.clone();
					EQi.put("quality", charactersAsString(root, firststruct));
					if (chara.getAttribute("constraintid") != null) {
						String names = this.getStructureName(root, chara.getAttributeValue("constraintid"));
						names = names + "," + this.getStructureChain(root, "//relation[@name='part_of'][@from='" + chara.getAttributeValue("constraintid") + "']");
						names = names.replaceFirst(",$", "");
						QMs = QMs + "," + names;
					}
					QMs = QMs.replaceFirst(",$", "").replaceFirst("^,", "").replaceAll(",+", ",");
					EQi.put("qualitymodifier", QMs);
					this.allEQs.add(EQi);
				}
				// collecting relations of whole_organism
				List<Element> wos = pathWholeOrganismStructure.selectNodes(state);
				for (Element wo : wos) {
					String id = wo.getAttributeValue("id");
					List<Element> rels = XPath.selectNodes(state, ".//relation[@from='" + id + "']");
					for (Element rel : rels) {
						Hashtable<String, String> EQi = (Hashtable<String, String>) EQc.clone();
						String relname = rel.getAttributeValue("name");
						String toid = rel.getAttributeValue("to");
						String toname = this.getStructureName(root, toid);
						toname = toname + "," + this.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
						toname = toname.replaceFirst(",$", "");
						String negation = rel.getAttributeValue("negation");
						if (negation.contains("true")) {
							EQi.put("qualitynegated", "not " + relname);
						} else {
							EQi.put("quality", relname);
						}
						EQi.put("qualitymodifier", toname);
						this.allEQs.add(EQi);
					}
				}
			}
			// deal with other structures

		}
	}

	private void addEQ4CharacterStatement(String src, List<Element> charstatements) {
		Hashtable<String, String> EQ = new Hashtable<String, String>();
		this.initEQHash(EQ);
		String chtext = charstatements.get(0).getChild("text").getTextTrim();
		String characterid = charstatements.get(0).getAttributeValue("character_id");
		EQ.put("source", src);
		EQ.put("description", chtext);
		EQ.put("characterid", characterid);
		EQ.put("type", "character");
		allEQs.add(EQ);
	}

	/**
	 * 
	 * @param firststruct
	 * @return all character value as a string
	 */
	@SuppressWarnings("unchecked")
	private String charactersAsString(Element root, Element firststruct) throws Exception {
		String chstring = "";

		List<Element> chars = pathCharacter.selectNodes(firststruct);
		for (Element chara : chars) {
			String m = (chara.getAttribute("modifier") == null ? "" : chara.getAttributeValue("modifier"));
			chstring += chara.getAttributeValue("value") + " ";
			if (m.length() > 0) {
				chstring += "[" + m + "] ";
			}
		}
		chstring.trim();

		return chstring;
	}

	/**
	 * tooth, height => tooth, [height]
	 * 
	 * @param chtext
	 * @return
	 */
	private String markcharacters(String chtext) {
		String[] chars = this.characters.split("\\|");
		for (String chara : chars) {
			chtext = chtext.replaceAll(chara, "[" + chara + "]");
		}
		chtext = chtext.replaceAll("\\]+", "]").replaceAll("\\[+", "[");
		return chtext;
	}

	/**
	 * this works only when the names in text are in singular form as those in snames
	 * this may be true for Sereno style
	 * 
	 * @param text
	 *            : contains no , or ;
	 * @param snames
	 * @return a structure name from snames that appear the earliest from text before a [character]
	 */
	private String firstMatchedStructureName(String text, ArrayList<String> snames, int i) {
		if (snames.size() == 0)
			return null;
		String textc = text;
		text = text.replaceFirst("\\[.*$", "") + " ";//remove the character term
		do {
			for (String sname : snames) {
				sname = sname.toLowerCase().replaceAll("_", " ");
				//Changes by Zilong
//				Pattern structRoman = Pattern.compile("(.*) [/dixv]+");
//				Matcher m = structRoman.matcher(sname);
//				if(m.matches()){
//					
//				}
				//Changed by Zilong end
				// if(!sname.matches(".*?[ivx\\d]+") && sname.length()>=3) sname = sname.substring(0, sname.length()-2);
				if (text.startsWith(sname)) {
					if (textc.endsWith(sname) && i == 0) {
						snames.remove(sname);
						return textc.trim();
					}
					return textc.trim();
				}
			}
			text = text.replaceFirst("^.*?\\s+", "");
		} while (text.length() > 0);
		return null;
	}

	@SuppressWarnings("unchecked")
	private void createEQsFromBinaryStates(List<Element> states, String src, ArrayList<Hashtable<String, String>> EQs) {
		// copy or negate the EQ for each state
		for (Element state : states) {
			Element text = state.getChild("text");
			String stext = text.getTextTrim();
			String characterid = state.getAttributeValue("character_id");
			String stateid = state.getAttributeValue("state_id");
			stateids.add(stateid);
			if (stext.matches("(" + binaryTvalues + ")")) {
				// copy
				for (Hashtable<String, String> EQ : EQs) { // add some needed fields
					Hashtable<String, String> EQcp = (Hashtable<String, String>) EQ.clone();
					EQcp.put("source", src);
					EQcp.put("characterid", characterid);
					EQcp.put("stateid", stateid);
					EQcp.put("description", stext);
					EQcp.put("type", "binary");
					allEQs.add(EQcp);
					/*
					 * this.addEQStatement(src, characterid, stateid, stext,
					 * EQ.get("entity"), EQ.get("entityid"), EQ.get("quality"), EQ.get("qualitynegated"),
					 * EQ.get("qnparent"), EQ.get("qnparentid"),EQ.get("qualityid"), EQ.get("qualitymodifier"),
					 * EQ.get("qualitymodifierid"),EQ.get("entitylocator"), EQ.get("entitylocatorid"), true);
					 */
				}

			} else if (stext.matches("(" + binaryFvalues + ")")) {
				// negate
				for (Hashtable<String, String> EQ : EQs) {// add some needed fields
					Hashtable<String, String> EQcp = (Hashtable<String, String>) EQ.clone();
					EQcp.put("source", src);
					EQcp.put("characterid", characterid);
					EQcp.put("stateid", stateid);
					EQcp.put("description", stext);
					EQcp.put("qualitynegated", "not " + EQ.get("quality")); // compose negated quality
					EQcp.put("quality", ""); // reset quality
					EQcp.put("type", "binary");
					allEQs.add(EQcp);
					/*
					 * this.addEQStatement(src, characterid, stateid, stext,
					 * EQ.get("entity"), EQ.get("entityid"), "", "not "+EQ.get("quality"),
					 * EQ.get("qnparent"), EQ.get("qnparentid"),EQ.get("qualityid"), EQ.get("qualitymodifier"),
					 * EQ.get("qualitymodifierid"),EQ.get("entitylocator"), EQ.get("entitylocatorid"), true);
					 */
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
	@SuppressWarnings("unchecked")
	private ArrayList<Hashtable<String, String>> processBinaryCharacter(List<Element> chars, String src, Element root) throws Exception {
		// these EQs will be transformed into state EQs
		ArrayList<Hashtable<String, String>> EQs = new ArrayList<Hashtable<String, String>>();
		Hashtable<String, String> EQ = new Hashtable<String, String>();
		initEQHash(EQ);

		// //get the first structure element
		// Element firststruct = (Element)pathNonWholeOrganismStructure.selectSingleNode(chars.get(0));
		// //TODO: what if firststruct == null?
		// String sid = firststruct.getAttributeValue("id");
		// String sname =this.getStructureName(root, sid);
		// EQ.put("entity", sname);

		List<Element> firststructs = new ArrayList<Element>();
		for (Element e : chars) {
			firststructs.addAll(pathNonWholeOrganismStructure.selectNodes(e));
		}

		for (Element e : firststructs) {
			Element firststruct=e;
			String sid = firststruct.getAttributeValue("id");
			String sname=this.getStructureName(root, sid);
			EQ.put("entity", sname);
			this.keyentities.add(sname);
		
			// take the first character
			Element chara = (Element) pathCharacter.selectSingleNode(firststruct);
			if (chara != null) {
				String q = formQualityValueFromCharacter(chara).replaceAll("\\[[^\\]]*?\\]", "");
				EQ.put("quality", q);
				EQs.add((Hashtable<String, String>) EQ.clone());
			}
			// keep positional relations associated with the first element
			// List<Element> rels = XPath.selectNodes(root, "//relation[@to='"+sid+"'|@from='"+sid+"']");
			List<Element> rels = XPath.selectNodes(root, "//relation[@from='" + sid + "']");
			String relationalquality = "";
			String qualitymodifier = "";
			for (Element rel : rels) {
				String relname = rel.getAttributeValue("name");
				String toid = rel.getAttributeValue("to");
				// String fromid = rel.getAttributeValue("from");
				// String lid = toid.compareTo("sid")==0? fromid : toid;
				String toname = this.getStructureName(root, toid);
				toname = toname + "," + this.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
				toname = toname.replaceFirst(",$", "");
				if (relname.matches("\\((" + positionprep + ")\\).*")) {
					if (relname.contains("between"))
						EQ.put("entitylocator", "between " + toname);// TODO chained part_of relations??
					else
						EQ.put("entitylocator", toname);
					this.keyentitylocator = toname;
				} else if (EQ.get("quality").compareTo("") == 0) {// quality not found, turn relation to quality, toid to qualitymodifier
					relationalquality += relname + "+";
					qualitymodifier += toname + "+";
				}
			}
			if (relationalquality.length() > 0) {
				relationalquality = relationalquality.replaceFirst("\\+$", "");
				String[] qs = relationalquality.split("\\+");
				String[] qms = qualitymodifier.split("\\+");
				for (int i = 0; i < qs.length; i++) {
					EQ.put("quality", qs[i]);
					EQ.put("qualitymodifier", qms[i]);
					EQs.add((Hashtable<String, String>) EQ.clone());
				}
			}
		}
		return EQs;
	}

	private void initEQHash(Hashtable<String, String> EQ) {
		EQ.put("source", "");
		EQ.put("characterid", "");
		EQ.put("stateid", "");
		EQ.put("description", "");
		EQ.put("type", ""); // do not output type to table
		EQ.put("entity", "");
		EQ.put("entitylabel", "");
		EQ.put("entityid", "");
		EQ.put("quality", "");
		EQ.put("qualitylabel", "");
		EQ.put("qualityid", "");
		EQ.put("qualitynegated", "");
		EQ.put("qualitynegatedlabel", "");
		EQ.put("qnparentlabel", "");
		EQ.put("qnparentid", "");
		EQ.put("qualitymodifier", "");
		EQ.put("qualitymodifierlabel", "");
		EQ.put("qualitymodifierid", "");
		EQ.put("entitylocator", "");
		EQ.put("entitylocatorlabel", "");
		EQ.put("entitylocatorid", "");
		EQ.put("countt", "");
	}

	/**
	 * if all states hold a binary value, return true, otherwise return false
	 * 
	 * @param states
	 * @return
	 */
	private boolean isBinary(List<Element> states) throws Exception {
		if (states.size() == 0)
			return false;

		for (Element state : states) {
			Element text = (Element) pathText2.selectSingleNode(state);
			String value = text.getTextTrim();
			if (!value.matches("(" + binaryTvalues + "|" + binaryFvalues + ")")) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Depreciated
	 * step 1: process Character Statements
	 * the set of character statements is expected to generate an E that is the subject for the state statements
	 * while it may also generate additional EQ statements
	 * 
	 * Changed by Zilong
	 * step 1: process Chratcer Statements
	 * may generate a list of structures which are subjects for the state.
	 * 
	 * @param chars
	 *            : a set of statements with type="character"
	 * @return
	 */
	// @SuppressWarnings("unchecked")
	// private Element processCharacter(List<Element> chars, String src, Element root) throws Exception{
	@SuppressWarnings("unchecked")
	private List<Element> processCharacter(List<Element> chars, String src, Element root) throws Exception {
		// Element key = null;
		List<Element> keys = new ArrayList<Element>();
		// //ArrayList<Element> purge = new ArrayList<Element>();
		// key = (Element)pathNonWholeOrganismStructure.selectSingleNode(chars.get(0));
		
		//add all structures which are not "whole organism" to key structures
		for (Element e : chars) {
			keys.addAll(pathNonWholeOrganismStructure.selectNodes(e));
			// if(key==null){
			// key = new Element("structure");
			// key.setAttribute("name", "unknown");
			// key.setAttribute("id", "unknown"+unknownid);
			// unknownid++;
			// }
			//
		}

		//no key structures found
		if (keys.size() == 0) {
			Element key = new Element("structure");
			key.setAttribute("name", "unknown");
			key.setAttribute("id", "unknown" + unknownid);
			unknownid++;
			keys.add(key);
		}

		//populate keyentities
		for(Element key:keys){
			keyentities.add(this.getStructureName(root, key.getAttributeValue("id")));
		}
	
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
					for(Element c: children){
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
			Element text = (Element)pathText2.selectSingleNode(statement);
			structures = pathStructure.selectNodes(statement);
			List<Element> relations = pathRelation.selectNodes(statement);
			createEQsfromStatement(src, root, text, structures, relations, true);
		}
		return keys;
	}

	/**
	 * step 2: process State Statements
	 * Use the name of E to replace the "whole_organism" placeholder in the state statements and generate EQ statements
	 * the state statements may also generate additional EQ statements
	 * If the name of E is also contained in the states, then use the name in the states
	 * 
	 * @param key
	 * @param states
	 */
	@SuppressWarnings("unchecked")
	private void createEQsFromStateStatements(List<Element> keys, List<Element> states, String src, Element root) throws Exception {

		for (Element statement : states) {
			// fill whole_organism place-holder with a real structure
			List<Element> whole_organism = pathWholeOrganismStructure.selectNodes(statement);
			for (Element origwo : whole_organism) {
				for(Element key:keys){
					Element wo=(Element) origwo.clone();
					wo.setAttribute("name", key.getAttributeValue("name"));
					changeIdsInRelations(wo.getAttributeValue("id"), key.getAttributeValue("id"), root);
					wo.setAttribute("id", key.getAttributeValue("id"));
					if (key.getAttribute("constraint") != null) {
						wo.setAttribute("constraint", key.getAttributeValue("constraint"));
					}
					origwo.getParentElement().addContent(wo);
				}
				origwo.detach();
			}
			// generate other EQ statements from this statement
			Element text = (Element) pathText2.selectSingleNode(statement);
			// List<Element> structures = pathStructure.selectNodes(statement, ".//structure");
			List<Element> structures = selectEntityStructures(statement);
			// relations should include those in this state statement and those in character statement
			List<Element> relations = pathRelation.selectNodes(statement);
			relations.addAll(pathRelationUnderCharacter.selectNodes(root));
			createEQsfromStatement(src, root, text, structures, relations, false);
		}

	}

	/**
	 * select structures that have characters and/or are from structure in a relation
	 * 
	 * @param statement
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Element> selectEntityStructures(Element statement) throws Exception {
		ArrayList<Element> selected = new ArrayList<Element>();

		List<Element> allstructs = pathStructure2.selectNodes(statement);
		for (Element struct : allstructs) {
			if (struct.getChildren().size() > 0)
				selected.add(struct);
			else {
				String id = struct.getAttributeValue("id");
				List<Element> from = XPath.selectNodes(statement, ".//relation[@from='" + id + "']");
				if (from.size() > 0)
					selected.add(struct);
			}
		}

		return selected;
	}

	/**
	 * search in all relations in root and replace oldid with newid for all from and to attributes
	 * 
	 * @param oldid
	 * @param newid
	 * @param root
	 */
	@SuppressWarnings("unchecked")
	private void changeIdsInRelations(String oldid, String newid, Element root) throws Exception {

		List<Element> rels = XPath.selectNodes(root, "//relation[@to='" + oldid + "'|@from='" + oldid + "']");
		for (Element rel : rels) {
			if (rel.getAttributeValue("to").compareTo(oldid) == 0)
				rel.setAttribute("to", newid);
			if (rel.getAttributeValue("from").compareTo(oldid) == 0)
				rel.setAttribute("from", newid);
		}
	}

	/**
	 * 
	 * @param src
	 * @param root 
	 * @param textelement - text nodes
	 * @param structures - all structure nodes
	 * @param relations - all relation nodes
	 * @param keyelement
	 *            if true, save its entitylocator info in the field entitylocator
	 */
	private void createEQsfromStatement(String src, Element root, Element textelement, List<Element> structures, List<Element> relations, boolean keyelement) throws Exception {
		String text = textelement.getText();
		// System.out.println("text::"+text);

		Hashtable<String, String> rels = relationHash(relations);

		// process structures: output
		Iterator<Element> it = structures.iterator();
		while (it.hasNext()) {
			Element struct = it.next();
			createEQs4Structure(src, root, text, struct, rels, keyelement);
		}
	}

	private Hashtable<String, String> relationHash(List<Element> relations) {
		// process relations first and hold the information in hashtable
		Hashtable<String, String> rels = new Hashtable<String, String>(); // fromstructureid => (relation name) tostructureid
		Iterator<Element> it = relations.iterator();
		while (it.hasNext()) {
			Element rel = it.next();
			String fromid = rel.getAttributeValue("from");
			String toid = rel.getAttributeValue("to");
			String relname = rel.getAttributeValue("name").trim();
			String neg = rel.getAttributeValue("negation");
			if (neg.compareTo("true") == 0) {
				relname = "not " + relname + "";
			}
			if (rels.get(fromid) == null) {
				rels.put(fromid, "(" + relname + ")" + toid);
			} else {
				rels.put(fromid, rels.get(fromid) + "#(" + relname + ")" + toid);
			}
		}
		return rels;
	}

	private void insertEQs2Table(Hashtable<String, String> EQ) throws Exception {
		Enumeration<String> fields = EQ.keys();

		// print
		String entity = EQ.get("entity");
		EQ.put("entity", format(entity));
		String entitylabel = transform(entity);
		EQ.put("entitylabel", entitylabel);
		String quality = EQ.get("quality");
		EQ.put("quality", format(quality));
		String qualitynegated = EQ.get("qualitynegated");
		EQ.put("qualitynegated", format(qualitynegated));
		String qualitymodifier = EQ.get("qualitymodifier");
		EQ.put("qualitymodifier", format(qualitymodifier));
		String qualitymodifierlabel = transform(qualitymodifier);
		EQ.put("qualitymodifierlabel", qualitymodifierlabel);
		String entitylocator = EQ.get("entitylocator");
		EQ.put("entitylocator", format(entitylocator));
		String entitylocatorlabel = transform(entitylocator);
		EQ.put("entitylocatorlabel", entitylocatorlabel);

		if (quality.length() == 0 && qualitynegated.length() == 0)
			return; // do not add E/EL only statement
		// quality and qualitynegated can not both hold values!
		// if(quality.length()>0 || entitylocator.length()>0){
		if (quality.length() > 0) {
			System.out.println("EQ::[E]" + entitylabel + " [Q]" + quality + (qualitymodifierlabel.length() > 0 ? " [QM]" + qualitymodifierlabel : "")
					+ (entitylocatorlabel.length() > 0 ? " [EL]" + entitylocatorlabel : ""));
		} else if (qualitynegated.length() > 0) {
			System.out.println("EQ::[E]" + entitylabel + " [QN]" + qualitynegated + (qualitymodifierlabel.length() > 0 ? " [QM]" + qualitymodifierlabel : "")
					+ (entitylocatorlabel.length() > 0 ? " [EL]" + entitylocatorlabel : ""));
		} else if (quality.length() == 0 && qualitynegated.length() == 0 && entitylocator.length() > 0) {
			System.out.println("EQ::[E]" + entitylabel + " [Q]" + quality + (qualitymodifierlabel.length() > 0 ? " [QM]" + qualitymodifierlabel : "")
					+ (entitylocatorlabel.length() > 0 ? " [EL]" + entitylocatorlabel : ""));
		} else {
			if (EQ.get("type").compareTo("character") != 0)
				System.out.println("A EQ was not printed");
		}

		// compose sql for insertion
		String fieldstring = "";
		String valuestring = "";
		while (fields.hasMoreElements()) {
			String f = fields.nextElement();
			if (f.compareTo("type") != 0) {
				fieldstring += f + ",";
				String fv = EQ.get(f);
				if (EQ.get("type").compareTo("character") == 0) {
					valuestring += "'" + (f.matches("(source|characterid|description)") ? fv : "") + "',";
				} else {
					valuestring += "'" + fv + "',";
				}
			}
		}
		fieldstring = fieldstring.replaceFirst(",$", "");
		valuestring = valuestring.replaceFirst(",$", "");

		String q = "insert into " + this.outputtable + "(" + fieldstring + ") values " + "(" + valuestring + ")";
		Statement stmt = conn.createStatement();
		stmt.execute(q);

	}

	private String format(String word) {
		word = word.replaceAll("_", " "); // abc_1
		word = word.replaceAll("(?<=\\w)- (?=\\w)", "-"); // dorsal- fin
		// word = word.replaceAll("\\[.*?\\]", "");//remove [usually]
		word = word.replaceAll("[()]", ""); // turn dorsal-(fin) to dorsal-fin
		word = word.replaceAll("-to\\b", " to"); // turn dorsal-to to dorsal to
		return word;
	}

	/**
	 * fifth abc => abc 5
	 * abc_1 => abc 1
	 * 
	 * @param entitylist
	 *            : entity1, entity2
	 * @return
	 */
	private String transform(String entitylist) {
		entitylist = entitylist.replaceAll("(?<=\\w)- (?=\\w)", "-");
		String transformed = entityhash.get(entitylist);
		if (transformed != null)
			return transformed;

		transformed = "";
		if (entitylist.matches(".*?(_[\\divx]+|first|second|third|forth|fouth|fourth|fifth|sixth|seventh|eighth|ninth|tenth).*")) {
			String[] entities = entitylist.split("(?<!_),(?!_)");
			for (String entity : entities) {
				// case one
				entity = entity.trim();
				if (entity.matches(".*?\\b(first|second|third|forth|fouth|fourth|fifth|sixth|seventh|eighth|ninth|tenth)\\b.*")) {
					Matcher m = p1.matcher(entity);
					if (m.matches()) {
						String position = turnPosition2Number(m.group(1));
						entity = m.group(2) + " " + position;
						transformed += entity + ",";
					} else {
						transformed += entity + ",";
					}
					// transformed = transformed.replaceFirst(",$", "").trim();
					// entityhash.put(entitylist, transformed);
					// return transformed;
				} // case two
				else if (entity.matches("(.*?_[\\divx]+)|(.*?_[\\divx]+-[\\divx]+)")) {// abc_1, abc_1_and_2, abc_1_to_3, abc_1-3
					String organ = entity.substring(0, entity.indexOf("_"));

					if (entity.matches(".*?_[\\divx]+-[\\divx]+")) {// abc_1-3
						entity = entity.replaceAll("-", "_to_");// before reformatRomans,replace "-" with "_to_"
					}

					entity = reformatRomans(entity);
					entity = entity.replaceAll("_(?=\\d+)", " ").replaceAll("(?<=\\d)_", " "); // abc_1_and_3 => abc 1 and 3
					if (entity.indexOf(" and ") < 0 && entity.indexOf(" to ") < 0) { // single entity
						transformed += entity + ",";
						// entityhash.put(entitylist, transformed);
						// return transformed;
					} else {// abc 1 and 2
						if (entity.indexOf(" and ") > 0) {
							transformed += entity.replaceFirst(" and ", "," + organ + " ") + ","; // abc 1,abc 2
							// entityhash.put(entitylist, transformed);
							// return transformed;
						}

						// abc 1 , 2 to 5 ; abc 2 to 5
						Matcher m = p2.matcher(entity);
						if (m.matches()) {
							String part1 = m.group(1);
							int from = Integer.parseInt(m.group(2));
							int to = Integer.parseInt(m.group(3));
							String temp1 = "";
							for (int i = from; i <= to; i++) {
								temp1 = temp1 + organ + " " + i + ",";
							}

							String temp = "";
							part1 = part1.replaceAll("\\D", "").trim();
							if (part1.length() > 0) {
								String[] nums = part1.split("\\s+");
								for (String n : nums) {
									temp = temp + organ + " " + n + ",";
								}
							}

							transformed = transformed + temp + temp1;
							// transformed.replaceFirst(",$", "").trim();
							// entityhash.put(entitylist, transformed);
							// return transformed;
						}
					}
				} else {// neither
					transformed += entity + ",";
				}
			}
		} else {
			transformed = entitylist;
			// entityhash.put(entitylist, entitylist);
		}
		transformed = transformed.replaceFirst(",$", "").trim();
		entityhash.put(entitylist, transformed);
		return transformed;
	}

	/**
	 * abc_iv_and_v
	 * 
	 * @param entity
	 * @return
	 */
	private String reformatRomans(String entity) {
		String[] parts = entity.split("_");
		String reformatted = "";
		for (String part : parts) {
			if (part.matches("[ivx]+"))
				reformatted += this.turnRoman2Number(part) + "_";
			else
				reformatted += part + "_";
		}
		return reformatted.replaceFirst("_$", "");
	}

	/**
	 * 
	 * @param entity
	 * @return
	 */
	private String turnRoman2Number(String word) {
		int total = 0;
		if (word.endsWith("iv")) {
			total += 4;
			word = word.replaceFirst("iv$", "");
		}
		if (word.endsWith("ix")) {
			total += 9;
			word = word.replaceFirst("ix$", "");
		}
		int length = word.length();
		for (int i = 0; i < length; i++) {
			if (word.charAt(i) == 'i')
				total += 1;
			if (word.charAt(i) == 'v')
				total += 5;
			if (word.charAt(i) == 'x')
				total += 10;
		}
		return total + "";
	}

	/**
	 * fifth => 5
	 * 
	 * @param word
	 * @return
	 */
	private String turnPosition2Number(String word) {
		if (word.compareTo("first") == 0)
			return "1";
		if (word.compareTo("second") == 0)
			return "2";
		if (word.compareTo("third") == 0)
			return "3";
		if (word.compareTo("forth") == 0)
			return "4";
		if (word.compareTo("fouth") == 0)
			return "4";
		if (word.compareTo("fourth") == 0)
			return "4";
		if (word.compareTo("fifth") == 0)
			return "5";
		if (word.compareTo("sixth") == 0)
			return "6";
		if (word.compareTo("seventh") == 0)
			return "7";
		if (word.compareTo("eighth") == 0)
			return "8";
		if (word.compareTo("ninth") == 0)
			return "9";
		if (word.compareTo("tenth") == 0)
			return "10";
		return null;
	}

	/**
	 * [8]Armbruster_2004.xml_0638f15b-0de4-45fd-a3af-b1d209cea9d3.xml
	 * text::Walls of metapterygoid channel
	 * text::lateral wall slightly smaller to just slightly larger than mesial wall, or absent
	 * EQ::[E]lateral wall [Q]smaller [slightly]
	 * EQ::[E]lateral wall [Q]larger [just slightly] [QM]mesial wall
	 * EQ::[E]lateral wall [Q]absent
	 * text::mesial wall much taller
	 * EQ::[E]mesial wall [Q]taller [much]
	 * 
	 * @param entity
	 * @return
	 */
	private boolean isRelated2KeyEntities(String entity) {
		String[] tokens = entity.split("\\s*,\\s*");
		for (String token : tokens) {
			for(String keyentity:this.keyentities){
				if (token.contains(keyentity) || keyentity.contains(token))
					return true;
			}
		}
		return false;
	}

	/**
	 * trace part_of relations of structid to get all its parent structures,
	 * separated by , in order
	 * 
	 * @param root
	 * @param xpath
	 *            : "//relation[@name='part_of'][@from='"+structid+"']"
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private String getStructureChain(Element root, String xpath) throws Exception {
		String path = "";

		List<Element> relations = XPath.selectNodes(root, xpath);
		xpath = "";
		for (Element r : relations) {
			String pid = r.getAttributeValue("to");
			path += this.getStructureName(root, pid) + ",";
			String[] pids = pid.split("\\s+");
			for (String id : pids) {
				if (id.length() > 0)
					xpath += "//relation[@name='part_of'][@from='" + id + "']|";
			}
		}
		if (xpath.length() > 0) {
			xpath = xpath.replaceFirst("\\|$", "");
			path += getStructureChain(root, xpath);
		} else {
			return path.replaceFirst(",$", "");
		}
		return path.replaceFirst(",$", "");
	}

	/**
	 * Get structure name from the XML results of CharaParser.
	 * 
	 * @param root
	 * @param structids
	 *            : 1 or more structids
	 * @return
	 */
	private String getStructureName(Element root, String structids) throws Exception {
		String result = "";

		String[] ids = structids.split("\\s+");
		for (String structid : ids) {
			Element structure = (Element) XPath.selectSingleNode(root, "//structure[@id='" + structid + "']");
			String sname = "";
			if (structure == null) {
				sname = "REF"; // this should never happen
			} else {
				sname = ((structure.getAttribute("constraint") == null ? "" : structure.getAttributeValue("constraint")) + " " + structure.getAttributeValue("name"));
			}
			result += sname + ",";
		}

		result = result.replaceAll("\\s+", " ").replaceFirst(",$", "").trim();
		return result;
	}

	/**
	 * 
	 * @param src
	 * @param root
	 * @param text
	 * @param struct - structure node
	 * @param keyelement
	 *            TODO
	 */
	@SuppressWarnings({ "unchecked", "static-access" })
	private void createEQs4Structure(String src, Element root, String text, Element struct, Hashtable<String, String> relations, boolean keyelement) throws Exception {
		if (keyelement)
			System.out.println("text::" + text);
		Hashtable<String, String> srcids = getStateId(root, struct);
		String characterid = srcids.get("characterid");
		String stateid = srcids.get("stateid");
		if (stateid.length() > 0)
			stateids.add(stateid);
		String structid = struct.getAttributeValue("id");
		String[] rels = null;
		String arelation = relations.get(structid);
		if (arelation != null)
			rels = arelation.split("#");
		String structname = this.getStructureName(root, structid);

		List<Element> chars = pathCharacter.selectNodes(struct, ".//character");
		Iterator<Element> it = chars.iterator();
		// structure has both characters and relations
		boolean hascharacter = false;
		while (it.hasNext()) {
			hascharacter = true;
			Element chara = it.next();
			// characters
			String quality = formQualityValueFromCharacter(chara);
			// constraints
			String qualitymodifier = "";
			if (chara.getAttribute("constraintid") != null) {
				qualitymodifier = this.getStructureName(root, chara.getAttributeValue("constraintid"));
				qualitymodifier = qualitymodifier + "," + this.getStructureChain(root, "//relation[@from='" + chara.getAttributeValue("constraintid") + "']");
				qualitymodifier = qualitymodifier.replaceFirst(",$", "");
			}
			// relations: may be entitylocators or qualitymodifiers
			String entitylocator = "";
			if (rels != null) {
				for (String r : rels) {
					String toid = r.replaceFirst(".*?\\)", "").trim();
					String toname = this.getStructureName(root, toid);
					toname = toname + "," + this.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
					toname = toname.replaceFirst(",$", "");
					if (r.matches("\\((" + positionprep + ")\\).*")) { // entitylocator
						if (r.contains("between"))
							entitylocator += "between " + toname + ",";
						else
							entitylocator += toname + ",";
					} else if (r.matches("\\(with\\).*")) {
						continue;
					} else if (r.matches("\\(without\\).*")) {
						// output absent as Q for toid
						if (!keyelement) {
							Hashtable<String, String> EQ = new Hashtable<String, String>();
							initEQHash(EQ);
							EQ.put("source", src);
							EQ.put("characterid", characterid);
							EQ.put("stateid", stateid);
							EQ.put("description", text);
							EQ.put("entity", toname);
							EQ.put("quality", "absent");
							EQ.put("type", keyelement ? "character" : "state");
							allEQs.add(EQ);
						}
					} else {
						qualitymodifier += toname + ",";
					}
				}
				entitylocator = entitylocator.replaceFirst(",$", "");
				qualitymodifier = qualitymodifier.replaceFirst(",$", "");
			}
			if (keyelement && keyentities.contains(structname))
				this.keyentitylocator = entitylocator;
			if (!keyelement) {
				Hashtable<String, String> EQ = new Hashtable<String, String>();
				initEQHash(EQ);
				EQ.put("source", src);
				EQ.put("characterid", characterid);
				EQ.put("stateid", stateid);
				EQ.put("description", text);
				EQ.put("entity", structname);
				EQ.put("quality", quality);
				EQ.put("qualitymodifier", qualitymodifier);
				EQ.put("entitylocator", entitylocator);
				EQ.put("type", keyelement ? "character" : "state");
				allEQs.add(EQ);
			}
		}
		// structure has only relations
		if (!hascharacter && rels != null) {
			// this is the case where the structure's character information is expressed in the relations (it has no character elements, but is involved in some relations)
			String entitylocator = "";
			// first, collect entitylocators
			for (String rel : rels) { // rel: (covered in)o621
				if (rel.matches("\\((" + positionprep + ")\\).*")) {
					String toid = rel.replaceFirst(".*?\\)", "").trim();
					String toname = this.getStructureName(root, toid);
					toname = toname + "," + this.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
					toname = toname.replaceFirst(",$", "");
					if (rel.contains("between"))
						entitylocator += "between " + toname + ",";
					else
						entitylocator += toname + ",";
				}
			}
			entitylocator = entitylocator.replaceFirst(",$", "");
			if (keyelement && keyentities.contains(structname))
				this.keyentitylocator = entitylocator;
			// then, create EQs for each qualities
			boolean hasrelquality = false;
			for (String rel : rels) {// rel: (covered in)o621
				// make "covered in" a quality and "o621" quality modifier
				// quality and qualitymodifier
				if (!rel.matches("\\((" + positionprep + ")\\).*")) {// exclude Locator relations
					String toid = rel.replaceFirst(".*?\\)", "").trim();
					String toname = this.getStructureName(root, toid);
					String quality = rel.replace(toid, "").replaceAll("[()]", "").trim();
					String qualitymodifier = this.getStructureName(root, toid);
					qualitymodifier = qualitymodifier + "," + this.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
					qualitymodifier = qualitymodifier.replaceFirst(",$", "");
					if (!keyelement && !quality.matches("(with|without)")) {
						hasrelquality = true;
						Hashtable<String, String> EQ = new Hashtable<String, String>();
						initEQHash(EQ);
						EQ.put("source", src);
						EQ.put("characterid", characterid);
						EQ.put("stateid", stateid);
						EQ.put("description", text);
						EQ.put("entity", structname);
						EQ.put("quality", quality); // may be negated: not closely connected to
						EQ.put("qualitymodifier", qualitymodifier);// qm may have locator too
						EQ.put("entitylocator", entitylocator);
						EQ.put("type", keyelement ? "character" : "state");
						allEQs.add(EQ);
					} else if (quality.compareTo("without") == 0) {
						Hashtable<String, String> EQ = new Hashtable<String, String>();
						initEQHash(EQ);
						EQ.put("source", src);
						EQ.put("characterid", characterid);
						EQ.put("stateid", stateid);
						EQ.put("description", text);
						EQ.put("entity", toname);
						EQ.put("quality", "absent");
						EQ.put("qualitymodifier", qualitymodifier);
						EQ.put("type", keyelement ? "character" : "state");
						allEQs.add(EQ);
					}
				}
			}
			if (!hasrelquality) {// output E and EL only /without quality
				Hashtable<String, String> EQ = new Hashtable<String, String>();
				initEQHash(EQ);
				EQ.put("source", src);
				EQ.put("characterid", characterid);
				EQ.put("stateid", stateid);
				EQ.put("description", text);
				EQ.put("entity", structname);
				EQ.put("entitylocator", entitylocator);
				EQ.put("type", keyelement ? "character" : "state");
				allEQs.add(EQ);
			}
		}
		// structure has no character, no relations
		if (!hascharacter && rels == null) { // most likely due to annotation errors
			if (!keyelement) {
				Hashtable<String, String> EQ = new Hashtable<String, String>();
				initEQHash(EQ);
				EQ.put("source", src);
				EQ.put("characterid", characterid);
				EQ.put("stateid", stateid);
				EQ.put("description", text);
				EQ.put("entity", structname);
				for(String keyentity:keyentities){
					if (!this.isRelated2KeyEntities(keyentity) && !this.isRelated2KeyEntities(this.keyentitylocator)) {
						String el = (keyentity + ", " + this.keyentitylocator).replaceAll("null", "").trim().replaceFirst("(^,|,$)", "").trim();
						EQ.put("entitylocator", el);
					}
				}
				EQ.put("type", keyelement ? "character" : "state");
				allEQs.add(EQ);
			}
		}
	}

	private String formQualityValueFromCharacter(Element chara) {
		String charatype = chara.getAttribute("char_type") != null ? "range" : "discrete";
		String quality = "";
		if (charatype.compareTo("range") == 0) {
			quality = chara.getAttributeValue("from") + " " + (chara.getAttribute("from_unit") != null ? chara.getAttributeValue("from_unit") : "") + " to "
					+ chara.getAttributeValue("to") + " " + (chara.getAttribute("to_unit") != null ? chara.getAttributeValue("to_unit") : "");

		} else {
			quality = (chara.getAttribute("modifier") != null && chara.getAttributeValue("modifier").matches(".*?\\bnot\\b.*") ? "not" : "") + " "
					+ chara.getAttributeValue("value") + " " + (chara.getAttribute("unit") != null ? chara.getAttributeValue("unit") : "") + "["
					+ (chara.getAttribute("modifier") != null ? chara.getAttributeValue("modifier").replaceAll("\\bnot\\b;?", "") : "") + "]";

		}
		quality = quality.replaceAll("\\[\\]", "").replaceAll("\\s+", " ").trim();
		return quality;
	}

	/**
	 * find the <statement> parent of the struct from the root
	 * return character id and state id
	 * 
	 * @param root
	 * @param struct
	 * @return characterid and stateid
	 */
	private Hashtable<String, String> getStateId(Element root, Element struct) {
		Hashtable<String, String> srcids = new Hashtable<String, String>();
		Element statement = struct.getParentElement();
		srcids.put("characterid", statement.getAttributeValue("character_id"));
		String stateid = statement.getAttribute("state_id") == null ? "" : statement.getAttributeValue("state_id");
		srcids.put("stateid", stateid);
		return srcids;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String srcdir = "C:\\Users\\Zilong Chang\\Desktop\\CHPImpr\\target\\final";
		String database = "biocreative2012";
		// String outputtable = "biocreative_nexml2eq";
		String outputtable = "gstestNew_xml2eq";
		// String benchmarktable = "internalworkbench";
		String prefix = "gstestNew";
		String glosstable = "fishglossaryfixed";
		try {
			XML2EQ x2e = new XML2EQ(srcdir, database, outputtable, /* benchmarktable, */prefix, glosstable);

			x2e.outputEQs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
