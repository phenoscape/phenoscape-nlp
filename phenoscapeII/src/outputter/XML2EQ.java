/**
 * 
 */
package outputter;

import java.io.File;
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

import conceptmapping.TermOutputerUtilities;

/**
 * @author Hong Updates
 *This class output EQ statements from the XML files output by CharaParser
 *The XML files are in \target\final
 *
 *This is in essence a rule-based algorithm. 
 *Rules for identifying the primary entity from the character statement (could also include character state statements) are implemented in KeyEntityFinder.java
 *Rules for generating EQs from <structure> elements are in XML2EQ.createEQs4Structure method.
 *Rules for matching an entity-entity locator pair are in EntitySearcher.java
 *Interface to ontologies is  TermSearcher.java and TermOutputerUtilities.java
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
	public static int unknownid = 0;
	private String outputtable;
	private String tableprefix;
	private String glosstable;
	private int count = 0;
	// private String keyentity = null;
	private List<String> keyentities;
	private String keyentitylocator = null;
	private ArrayList<Hashtable<String, String>> allEQs = null;
	private HashSet<String> stateids = new HashSet<String>();
	private static ArrayList<String> serenostyle = new ArrayList<String>();
	private String characters = null;
	private XPath pathStructure;
	private XPath pathWholeOrgStrucChar;
	private XPath pathCharacter;
	private XPath pathText2;
	private XPath pathRelation;
	private XPath pathRelationUnderCharacter;
	private XPath pathStructure2;
	private XPath pathCharacterText;



	public static TermOutputerUtilities ontoutil;
	private Dictionary dictionary = new Dictionary(new ArrayList<String>());

	private EntitySearcher es = new EntitySearcher(dictionary);
	private TermSearcher ts = new TermSearcher(dictionary);
	private CharacterHandler ch = new CharacterHandler(ts, es);
	private RelationHandler rh = new RelationHandler(dictionary, es);
	private KeyEntityFinder kef = new  KeyEntityFinder(es);

	//a convenient way to separate Sereno style from others by listing the source file names here.
	//TODO replace it with a more elegant approach
	static {
		serenostyle.add("sereno");
		serenostyle.add("martinez");
		serenostyle.add("earlyevolutionofarchosaurs");
	}


	public XML2EQ(String sourcedir, String database, String outputtable, String prefix, String glosstable) throws Exception {
		this.source = new File(sourcedir);
		this.outputtable = outputtable;
		this.tableprefix = prefix;
		this.glosstable = glosstable;
		this.keyentities = new ArrayList<String>();
		ontoutil = new TermOutputerUtilities(ApplicationUtilities.getProperty("ontologydir"), ApplicationUtilities.getProperty("database.name"));
		
		if(dictionary.conn == null){
			Class.forName("com.mysql.jdbc.Driver");
			dictionary.conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
		}

		Statement stmt = dictionary.conn.createStatement();
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

		pathStructure = XPath.newInstance(".//structure");
		pathWholeOrgStrucChar= XPath.newInstance(".//structure[@name='whole_organism']/character");
		pathCharacter = XPath.newInstance(".//character");
		pathText2 = XPath.newInstance(".//text");
		pathRelation = XPath.newInstance(".//relation");
		pathRelationUnderCharacter = XPath.newInstance(".//statement[@statement_type='character']/relation");
		pathStructure2 = XPath.newInstance(".//structure");
		pathCharacterText = XPath.newInstance(".//.//statement[@statement_type='character']/text");	
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
			new XMLNormalizer(root).normalize();
			// if(count!= 67){ count++; continue;}
			System.out.println();
			System.out.println("[" + count + "]" + src);
			count++;
			// reset
			keyentities = new ArrayList<String>();
			keyentitylocator = null;
			allEQs = new ArrayList<Hashtable<String, String>>();// used to hold all EQ statement generated from this character unit (1 file holds 1 character unit)
			String author = src.replaceFirst("_.*$", "").toLowerCase();
			// expect 1 file to have 1 character statement and n statements, but for generality, use arrayList for characterstatements too.
			//characterstatements are character descriptions
			List<Element> characterstatements = XMLNormalizer.pathCharacterStatement.selectNodes(root);
			//statestatements are state descriptions
			List<Element> statestatements = XMLNormalizer.pathStateStatement.selectNodes(root);
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
	 * use workbench to select/keep only the ones in the workbench
	 */
	/*
	 * private void discardNonTestCharacterUnits() throws Exception {
	 * Statement stmt = conn.createStatement();
	 * stmt.execute("delete from "+this.outputtable+" where source not in (select source from "+this.benchmarktable+ ")");
	 * }
	 */



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


		//String text = "";
		
		/** 
		 * NORMALIZATION PROCESS
		 **/
		for (Hashtable<String, String> EQ : allEQs) {
			
			this.insertEQs2Table(EQ);
			/*
			// 2. negation
			if (EQ.get("quality").startsWith("not ")) {
				EQ.put("qualitynegated", EQ.get("quality"));
				EQ.put("quality", "");
			}

			// finally
			if (EQ.get("description").compareTo(text) != 0) {
				System.out.println("text::" + EQ.get("description"));
				text = EQ.get("description");
			}*/
			

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
			Element charactertext = null;
			try{
				charactertext = (Element)pathCharacterText.selectSingleNode(root);
			}catch(Exception e){
				e.printStackTrace();
			}
			processBinaryCharacter(charstatements, states, src, root,  charactertext==null? "" : charactertext.getTextNormalize());

		} else {
			// on the first try, assuming the simple model:
			// 1 char statement holding 1 organ/process/entity
			// n states provide quality

			// step 1: process Character Statements
			// the set of character statements is expected to generate an E that is the subject for the state statements
			// while it may also generate additional EQ statements
			// also set keyentities and keyentitylocator fields
			List<Element> keys = processCharacterStatements(charstatements, states, src, root);
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
			Statement stmt = dictionary.conn.createStatement();
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
			snames.add(Utilities.getStructureName(root, struct.getAttributeValue("id")).replaceFirst("(?<=\\w)_(?=\\d+$)", " "));
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
		Utilities.initEQHash(EQ);
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
				String fsname = Utilities.getStructureName(root, firststruct.getAttributeValue("id"));
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
						String names = Utilities.getStructureName(root, chara.getAttributeValue("constraintid"));
						names = names + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + chara.getAttributeValue("constraintid") + "']");
						names = names.replaceFirst(",$", "");
						QMs = QMs + "," + names;
					}
					QMs = QMs.replaceFirst(",$", "").replaceFirst("^,", "").replaceAll(",+", ",");
					EQi.put("qualitymodifier", QMs);
					this.allEQs.add(EQi);
				}
				// collecting relations of whole_organism
				List<Element> wos = XMLNormalizer.pathWholeOrganismStructure.selectNodes(state);
				for (Element wo : wos) {
					String id = wo.getAttributeValue("id");
					List<Element> rels = XPath.selectNodes(state, ".//relation[@from='" + id + "']");
					for (Element rel : rels) {
						Hashtable<String, String> EQi = (Hashtable<String, String>) EQc.clone();
						String relname = rel.getAttributeValue("name");
						String toid = rel.getAttributeValue("to");
						String toname = Utilities.getStructureName(root, toid);
						toname = toname + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
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
		Utilities.initEQHash(EQ);
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
	private Element getFalseState(List<Element> states) {
		// copy or negate the EQ for each state
		for (Element state : states) {
			Element text = state.getChild("text");
			String stext = text.getTextTrim();
			if (stext.matches("(" + Dictionary.binaryFvalues + ")")) {
				return state;				
			}
		}
		return null;
	}

	private Element getTrueState(List<Element> states) {
		// copy or negate the EQ for each state
		for (Element state : states) {
			Element text = state.getChild("text");
			String stext = text.getTextTrim();
			if (stext.matches("(" + Dictionary.binaryTvalues + ")")) {
				return state;
			} 
		}
		return null;
	}
	/**
	 * BinaryCharacter: those taking yes/no or present/absent as character states.
	 * 
	 * case 1: "expanded ribs: present/absent" =>ribs: expanded/not expanded
	 * 
	 * “Preopercular latero-sensory canal leaves preopercle at first exit and enters a plate: yes/no”
	 * =>Preopercular latero-sensory canal: position (1 EQ)
	 * 
	 * No need to analyze state statements ( since they are binary values).
	 * Analyzing character statement alone is sufficient to generate one or more EQs
	 * 
	 *
	 * @param chars
	 * @param src
	 * @param root
	 * @return an arraylist of EQs, each is an EQ-hashtable. Only  
	 */
	@SuppressWarnings("unchecked")
	private void processBinaryCharacter(List<Element> chars, List<Element> states,  String src, Element root, String charactertext) throws Exception {
		//identify primary entities, which may not correspond directly to one <structure> (e.g., junction between A and B => A-B joint)
		List<Element> entities  = this.kef.getKeyEntities(chars, states, root, keyentities);
		
		//loop through entities and their character statements to generate EQs
		for (Element e : entities) {
			String sid = e.getAttributeValue("id");
			String ontoid = e.getAttribute("ontoid") !=null? e.getAttributeValue("ontoid") : "";
			String sname=Utilities.getStructureName(root, sid);
			//this.keyentities.add(sname);
		
			//collect <character> and <relation> associated with the entity (being the subject)
			List<Element> chara = (List<Element>) pathCharacter.selectNodes(e);
			List<Element> relations = XPath.selectNodes(root, "//relation[@from='" + sid + "']");
			String rq = null;
			String rqm = null;
			String el = null;
			Hashtable<String, Object> relationresults = null;
			if(relations !=null && relations.size()>0){
				String[] relationstrings = relationHash(relations).get(sid).split("#");
				relationresults = rh.handle(root, relationstrings, sname, sid, true);
				if(relationresults!=null){
					//TODO synch output 
					rq = (String) relationresults.get("relationalqualty");
					rqm = (String)  relationresults.get("qualtymodifier");
					el = (String) relationresults.get("entitylocator");
					//TODO accept rqm and el
				}
			}
			String nq = "";
			String q = "";
			String qualitylabel = "";
			String qualityid = "";
			
			boolean presentabsent =false;
			boolean outputnegated = false; //default to return two EQs corresponding to the binary statements; but return 1 EQ for complicated cases (case 2 example)
			//case 1: there is one character or one relation
			if(rq !=null && rq.indexOf(",") < 0 && rq.indexOf(";")<0){
				outputnegated = true;
				if(rq.startsWith("not ")){
					nq = rq;
					q = rq.replaceFirst("^not ", "");
				}else{
					nq = "not "+rq;
				}					
				//TODO: synch output 
				if(relationresults!=null){
					qualitylabel = (String) relationresults.get("relationalqualtylabel");
					qualityid = (String) relationresults.get("relationalqualtyid");
				}				
			}else{
				outputnegated = true;
				//string all rqs together to form one quality, if too complicated, search for broader category in PATO
				if(relationresults!=null){
					qualityid = (String) relationresults.get("relationalqualtyid");
					if(qualityid!=null && qualityid.length() > 0){
						//TODO: string together,
					}else{
						outputnegated = false;
						q = "position"; //relations default to "position" TODO other cases?
					}
				}			

			}		
			//q = absent/present
			if (chara!=null && chara.size()==0) {
				presentabsent = true;
				nq = "absent";
				q = "present";
				Hashtable<String, String> result = ts.searchTerm(q, "quality", 0);
				if(result!=null){
					qualitylabel = result.get("label");
					qualityid = result.get("id");
				}
				
			}else if (chara!=null && chara.size()==1) {
				outputnegated = true;
				q = Utilities.formQualityValueFromCharacter(chara.get(0)).replaceAll("\\[[^\\]]*?\\]", "");
				if(q.startsWith("not ")){
					nq = q;
					q = nq.replaceFirst("^not ", "");
				}else{
					nq = "not "+q;
				}
				Hashtable<String, String> result = ts.searchTerm(q, "quality", 0);
				if(result!=null){
					qualitylabel = result.get("label");
					qualityid = result.get("id");
				}
				
			}else{
				outputnegated = true;
				//string all qs together to form one quality, if too complicated, search for broader category in PATO
				//TODO
			}
			
							
			//create e/q 
			Element state = getTrueState(states);
			String characterid = state.getAttributeValue("character_id");
			String stateid = state.getAttributeValue("state_id");
			String stext = state.getChild("text").getTextNormalize();
			Hashtable<String, String> EQ = new Hashtable<String, String>();
			Utilities.initEQHash(EQ);
			EQ.put("source", src);
			EQ.put("characterid", characterid);
			EQ.put("stateid", stateid);
			EQ.put("description", charactertext+":"+stext);
			EQ.put("type", "binary");
			EQ.put("entity", sname);
			EQ.put("entityid", ontoid);
			EQ.put("entitylabel", sname);
			EQ.put("quality", q);
			EQ.put("qualitylabel", qualitylabel);
			EQ.put("qualityid", qualityid);
			allEQs.add((Hashtable<String, String>) EQ.clone());
			
			state = getFalseState(states);
			characterid = state.getAttributeValue("character_id");
			stateid = state.getAttributeValue("state_id");
			stext = state.getChild("text").getTextNormalize();
			
			if(presentabsent){
				Utilities.initEQHash(EQ);
				EQ.put("source", src);
				EQ.put("characterid", characterid);
				EQ.put("stateid", stateid);
				EQ.put("description", charactertext+":"+stext);
				EQ.put("type", "binary");
				EQ.put("entity", sname);
				EQ.put("entityid", ontoid);
				EQ.put("entitylabel", sname);
				EQ.put("quality", nq);
				Hashtable<String, String> result = ts.searchTerm(nq, "quality", 0); //"absent"
				EQ.put("qualitylabel", result.get("label"));
				EQ.put("qualityid", result.get("id"));
				allEQs.add((Hashtable<String, String>) EQ.clone());
			}else if(outputnegated){
				//create e/negated q
				Utilities.initEQHash(EQ);
				EQ.put("source", src);
				EQ.put("characterid", characterid);
				EQ.put("stateid", stateid);
				EQ.put("description", charactertext+":"+stext);
				EQ.put("type", "binary");
				EQ.put("entity", sname);
				EQ.put("entityid", ontoid);
				EQ.put("entitylabel", sname);
				EQ.put("qualitynegated", nq);
				if(qualitylabel.length()>0){
					EQ.put("qualitynegatedlabel", "not("+qualitylabel+")");
					EQ.put("qnparentlabel", "");
					EQ.put("qnparentid", "");
					String [] parentinfo = ontoutil.retreiveParentInfoFromPATO(qualitylabel);
					if(parentinfo != null){
						EQ.put("qnparentid", parentinfo[0]);
						EQ.put("qnparentlabel", parentinfo[1]);
					}else{
						System.err.println("should not landed here");
					}
				}
				allEQs.add((Hashtable<String, String>) EQ.clone());
			}			
		}
	}
	
	
	/*
	 * Fill in the following in EQ
	 *  EQ.put("quality", "");
		EQ.put("qualitylabel", "");
		EQ.put("qualityid", "");
		EQ.put("qualitynegated", "");
		EQ.put("qualitynegatedlabel", "");
		EQ.put("qnparentlabel", "");
		EQ.put("qnparentid", "");
	 */
	private void insertQualityNegated(String qualitynegated, Hashtable<String, String> EQ){
		String term = qualitynegated.replaceFirst("not ", "").trim();

	}

 	/**
	 * BinaryCharacter: those taking yes/no or present/absent as character states.
	 * @param chars
	 * @param src
	 * @param root
	 * @return a hashtable fieldname => value, if a field does not have a value, set it ""
	 */
/*	@SuppressWarnings("unchecked")
	private ArrayList<Hashtable<String, String>> processBinaryCharacter(List<Element> chars, String src, Element root) throws Exception {
		// these EQs will be transformed into state EQs
		ArrayList<Hashtable<String, String>> EQs = new ArrayList<Hashtable<String, String>>();
		Hashtable<String, String> EQ = new Hashtable<String, String>();
		Utilities.initEQHash(EQ);

		// //get the first structure element
		// Element firststruct = (Element)pathNonWholeOrganismStructure.selectSingleNode(chars.get(0));
		// //TODO: what if firststruct == null?
		// String sid = firststruct.getAttributeValue("id");
		// String sname =Utilities.getStructureName(root, sid);
		// EQ.put("entity", sname);

		List<Element> firststructs = new ArrayList<Element>();
		for (Element e : chars) {
			firststructs.addAll(XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(e));
		}

		for (Element e : firststructs) {
			Element firststruct=e;
			String sid = firststruct.getAttributeValue("id");
			String sname=Utilities.getStructureName(root, sid);
			EQ.put("entity", sname);
			this.keyentities.add(sname);
		
			// take the first character
			Element chara = (Element) pathCharacter.selectSingleNode(firststruct);
			if (chara != null) {
				String q = Utilities.formQualityValueFromCharacter(chara).replaceAll("\\[[^\\]]*?\\]", "");
				
				//TODO modifier="not" => negatedquality
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
				String toname = Utilities.getStructureName(root, toid);
				toname = toname + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
				toname = toname.replaceFirst(",$", "");
				if (relname.matches("\\((" + Dictionary.positionprep + ")\\).*")) {
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
			// quality not found, turn relation to quality, toid to qualitymodifier
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

 */

	/**
	 * if all states hold a binary value, return true, otherwise return false
	 * example:
	 * yes but interrupted by Meckelian foramina or fenestrae
	 * yes by prearticular 
	 * @param states
	 * @return
	 */
	private boolean isBinary(List<Element> states) throws Exception {
		if (states.size() == 0)
			return false;

		for (Element state : states) {
			Element text = (Element) pathText2.selectSingleNode(state);
			String value = text.getTextTrim();
			if (!value.matches("(" + Dictionary.binaryTvalues + "|" + Dictionary.binaryFvalues + ")")) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Deprecated
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
	private List<Element> processCharacterStatements(List<Element> chars, List<Element> states, String src, Element root) throws Exception {
		List<Element> keys = kef.getKeyEntities(chars, states, root, keyentities);
		for(Element statement: chars){
			List<Element> structures = statement.getChildren("structure");
			//generate other EQ statements from this statement
			Element text = (Element)pathText2.selectSingleNode(statement);
			structures = pathStructure.selectNodes(statement); //all <structure> in the statement
			List<Element> relations = pathRelation.selectNodes(statement); //all <relation> in this statement
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
			// normalize elements of state statements
			
			// fill whole_organism place-holder with key structures
			List<Element> whole_organism = XMLNormalizer.pathWholeOrganismStructure.selectNodes(statement);
			for (Element origwo : whole_organism) {
				for(Element key:keys){
					Element wo=(Element) origwo.clone();
					wo.setAttribute("name", key.getAttributeValue("name"));
					Utilities.changeIdsInRelations(wo.getAttributeValue("id"), key.getAttributeValue("id"), root);
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
			List<Element> relations = pathRelation.selectNodes(statement); //relations in the state statement
			relations.addAll(pathRelationUnderCharacter.selectNodes(root)); //relations in the character statement
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

	/**
	 * 
	 * @param relations
	 * @return hash: fromstructureid => (relation name) tostructureid
	 */
	private Hashtable<String, String> relationHash(List<Element> relations) {
		if(relations==null || relations.size()==0) return null;
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
		String entitylabel = EQ.get("entity")+"["+EQ.get("entitylabel")+"]";
		String quality = EQ.get("quality");
		String qualitynegated = EQ.get("qualitynegated");
		String qualitymodifierlabel = EQ.get("qualitymodifier")+"["+EQ.get("qualitymodifierlabel")+"]";
		String entitylocator = EQ.get("entitylocator");
		String entitylocatorlabel = EQ.get("entitylocator")+"["+EQ.get("entitylocatorlabel")+"]";

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
		Statement stmt = dictionary.conn.createStatement();
		stmt.execute(q);

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
		
		//TODO
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
		//String entitylocator = EQ.get("entitylocator");			
		//if (EQ.get("type").compareTo("state") == 0 && this.keyentitylocator != null) {
			// EQ based on a state statement
			// this is not a binarystate statement
			//if (EQ.get("entitylocator").compareTo(this.keyentitylocator) != 0 && isRelated2KeyEntities(EQ.get("entity"))) {
				// to inhere the entitylocator, this entity must be somewhat related to this.keyentity
				// "lateral wall" is related to "walls" of ...
				//entitylocator += "," + this.keyentitylocator;
			//}
		//}
		//entitylocator = entitylocator.trim().replaceAll("(^,|,$)", "");
		//EQ.put("entitylocator", entitylocator);
		
		
		//1. deal with relations first: relation may generate entity, entitylocator, relationalquality and/or quality modifier
		String structid = struct.getAttributeValue("id");
		String structname = Utilities.getStructureName(root, structid);
		String[] rels = null;
		String arelation = relations.get(structid);
		String entityID = null;
		String entitylocatorID = null;
		
		if (arelation != null){
			rels = arelation.split("#"); //all relation names with this structure as a from organ
			Hashtable<String, Object> resultsfromrelations = rh.handle(root, rels, structname, structid, keyelement);
			entityID = (String)resultsfromrelations.get("entity");
			entitylocatorID = (String)resultsfromrelations.get("entitylocator");
			String relationalqualityIDs = (String)resultsfromrelations.get("relationalquality"); //relationalquality and qualitymodifier are pairs
			String qualitymodifierIDs = (String)resultsfromrelations.get("qualitymodifier");//relationalquality and qualitymodifier are pairs
			String[] relqualityIDs = relationalqualityIDs.split(";");
			String[] qmodifierIDs = qualitymodifierIDs.split(";");
			for(int i = 0; i < relqualityIDs.length; i++){ //one EQ for each relationalquality
				Hashtable<String, String> EQ = new Hashtable<String, String>();
				if(relqualityIDs[i].trim().length()>0){
					Utilities.initEQHash(EQ);
					EQ.put("source", src);
					EQ.put("characterid", characterid);
					EQ.put("stateid", stateid);
					EQ.put("description", text);
					EQ.put("entity", entityID);
					EQ.put("entitylocator", entitylocatorID);
					EQ.put("quality", relqualityIDs[i]);
					EQ.put("qualitymodifier", qmodifierIDs[i]);
					EQ.put("type", keyelement ? "character" : "state");
					allEQs.add(EQ);
				}
			}			
			List<Hashtable<String, String>> extraEQs = (List<Hashtable<String, String>>) resultsfromrelations.get("extraEQs");
			for(Hashtable<String, String> extraEQ : extraEQs){
				extraEQ.put("source", src);
				extraEQ.put("characterid", characterid);
				extraEQ.put("stateid", stateid);
				extraEQ.put("description", text);
				allEQs.add(extraEQ);
			}
		}
		//2. next deal with character, which may affect quality and quality modifier
		//use entityID/entitylocatorID identified if they have been
		if(entityID==null) 
			entityID = es.searchEntity(root, structid, structname, "", structname, "",  0).get("entityid");
		List<Element> chars = pathCharacter.selectNodes(struct, ".//character");
		Hashtable<String, String> resultsfromcharacters = ch.handle(root, chars);
		String qualityIDs = resultsfromcharacters.get("quality"); //quality and qualitymodifers are pairs
		String qualitymodifierIDs = resultsfromcharacters.get("qualitymodifier"); //quality and qualitymodifers are pairs
		String[] qIDs = qualityIDs.split(";");
		String[] qmIDs = qualitymodifierIDs.split(";");
		for(int i = 0; i < qIDs.length; i++){ //one EQ for each quality
			Hashtable<String, String> EQ = new Hashtable<String, String>();
			if(qIDs[i].trim().length()>0){
				Utilities.initEQHash(EQ);
				EQ.put("source", src);
				EQ.put("characterid", characterid);
				EQ.put("stateid", stateid);
				EQ.put("description", text);
				EQ.put("entity", entityID);
				EQ.put("entitylocator", entitylocatorID);
				EQ.put("quality", qIDs[i]);
				EQ.put("qualitymodifier", qmIDs[i]);
				EQ.put("type", keyelement ? "character" : "state");
				allEQs.add(EQ);
			}
		}	
		
	}
			/*
			// relations: may be entitylocators or qualitymodifiers
			String entitylocator = "";
			if (rels != null) {
				for (String r : rels) {
					String toid = r.replaceFirst(".*?\\)", "").trim();
					String toname = Utilities.getStructureName(root, toid);
					toname = toname + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
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
					String toname = Utilities.getStructureName(root, toid);
					toname = toname + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
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
					String toname = Utilities.getStructureName(root, toid);
					String quality = rel.replace(toid, "").replaceAll("[()]", "").trim();
					String qualitymodifier = Utilities.getStructureName(root, toid);
					qualitymodifier = qualitymodifier + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
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
	}*/

	

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
		String srcdir = "C:/Users/updates/CharaParserTest/swartz2012/target/final";
		String database = "biocreative2012";
		// String outputtable = "biocreative_nexml2eq";
		String outputtable = "bspotest1210_swartz_xml2eq";
		// String benchmarktable = "internalworkbench";
		String prefix = "swartz";
		String glosstable = "fishglossaryfixed";
		try {
			XML2EQ x2e = new XML2EQ(srcdir, database, outputtable, /* benchmarktable, */prefix, glosstable);
			x2e.outputEQs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
