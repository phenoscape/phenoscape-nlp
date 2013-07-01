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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;

import owlaccessor.OWLAccessorImpl;

/* annotation guideline: http://phenoscape.org/wiki/Guide_to_Character_Annotation */
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
@SuppressWarnings("static-access")
public class XML2EQ {
	private File source;
	public static int unknownid = 0;
	private String outputtable;
	private String tableprefix;
	private String glosstable;
	private int count = 0;
	// private String keyentity = null;
	private ArrayList<EntityProposals> keyentities;
	//private String keyentitylocator = null;
	private ArrayList<EQStatementProposals> allEQs = null;
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



	public static TermOutputerUtilities ontoutil = new TermOutputerUtilities();
	public static ELKReasoner elk; 
	static{
		try{
			//TODO: figure out why the two calls give different results?
			//elk = new ELKReasoner(TermOutputerUtilities.uberon);
			elk = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+"ext.owl"));
			/*OWLOntology elkonto = elk.getOntology();
			System.out.println(elkonto.getAxiomCount());
			System.out.println(TermOutputerUtilities.uberon.getAxiomCount());
			System.out.println(elkonto.equals(TermOutputerUtilities.uberon));*/
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private Dictionary dictionary = new Dictionary();
	//private EntitySearcherOriginal es = new EntitySearcherOriginal(dictionary);
	//private TermSearcher ts = new TermSearcher(dictionary);
	//private CharacterHandler ch = new CharacterHandler(ts, es, ontoutil);
	//private RelationHandler rh = new RelationHandler(dictionary, es);
	//private KeyEntityFinder kef = new  KeyEntityFinder(es);
	
	public static final int RELATIONAL_SLIM=1;
	public static final int ATTRIBUTE_SLIM=2;

	//a convenient way to separate Sereno style from others by listing the source file names here.
	//TODO replace it with a more elegant approach
	/*static {
		serenostyle.add("sereno");
		serenostyle.add("martinez");
		serenostyle.add("earlyevolutionofarchosaurs");
		ontoutil = new TermOutputerUtilities(ApplicationUtilities.getProperty("ontology.dir"), ApplicationUtilities.getProperty("database.name"));
	}*/


	public XML2EQ(String sourcedir, String database, String outputtable, String prefix, String glosstable) throws Exception {
		this.source = new File(sourcedir);
		this.outputtable = outputtable;
		this.tableprefix = prefix;
		this.glosstable = glosstable;
		//this.keyentities = new ArrayList<Hashtable<String,String>>();

		
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
		pathWholeOrgStrucChar= XPath.newInstance(".//structure[@name='"+ApplicationUtilities.getProperty("unknown.structure.name")+"']/character");
		pathCharacter = XPath.newInstance(".//character");
		pathText2 = XPath.newInstance(".//text");
		pathRelation = XPath.newInstance(".//relation");
		pathRelationUnderCharacter = XPath.newInstance(".//statement[@statement_type='character']/relation");
		pathStructure2 = XPath.newInstance(".//structure");
		//pathCharacterText = XPath.newInstance(".//.//statement[@statement_type='character']/text");	
		pathCharacterText = XPath.newInstance(".//statement[@statement_type='character']/text");	

	}

	@SuppressWarnings("unchecked")
	public void outputEQs() {
		File[] xmlfiles = this.source.listFiles();
		for (File f : xmlfiles) {
			try{
				String src = f.getName();
				SAXBuilder builder = new SAXBuilder();
				Document xml = builder.build(f);
				Element root = xml.getRootElement();
				new XMLNormalizer(root).normalize();
				// if(count!= 67){ count++; continue;}
				System.out.println("[" + count + "]" + src);
				count++;
				allEQs = new ArrayList<EQStatementProposals>();
				Element characterstatement = (Element) XMLNormalizer.pathCharacterStatement.selectSingleNode(root);
				System.out.println("text: " + characterstatement.getChildText("text"));
				List<Element> statestatements = XMLNormalizer.pathStateStatement.selectNodes(root);
				if(isBinary(statestatements)){
					BinaryCharacterStatementParser bcsp = new BinaryCharacterStatementParser(ontoutil);
					bcsp.parse(characterstatement, root);
					allEQs = bcsp.getEQStatements();
				}else{
					CharacterStatementParser csp = new CharacterStatementParser(ontoutil);
					csp.parse(characterstatement, root);
					keyentities = csp.getKeyEntities();
					ArrayList<String> qualityclue = csp.getQualityClue();
					StateStatementParser ssp = new StateStatementParser(ontoutil, keyentities, qualityclue);
					for(Element statestatement: statestatements){
						ssp.parse(statestatement, root);
						allEQs.addAll(ssp.getEQStatements());
						ssp.EQStatements.clear();
					}
					fixIncompleteStates(src, root);//try to fix states with incomplete EQs by drawing info from  EQs from other states
				}
				outputEQs4CharacterUnit();
			}catch(Exception e){

				e.printStackTrace();
			}
		}
		elk.dispose();
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

		for (EQStatementProposals EQ : allEQs) {
			
			//this.insertEQs2Table(EQ);
			System.out.println(EQ.toString());
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

	/*private void repairProblemEQs(ArrayList<Hashtable<String, String>> problems) {
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
	}*/


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
	/*@SuppressWarnings("unchecked")
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
	}*/

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
	 * TODO
	 * text::Prearticular with mesially projecting flange on dorsal edge along posterior border of adductor fossa
	 * text::no
	 * text::yes
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

	//check allEQs to identify the case like
	//[0]Swartz 2012.xml_states1034.xml
	//text::Body scale [morphology]
	//text::<rhomboid> with internal ridge
	//text::round
	//round is a shape, then the 1st state should be about shape too, 'rhomboid' is not a structure, but a shape in PATO
	//reprocess state 1, looking for a shape term in PATO
	
	//TODO: (needs info about other characters to know pit and ridge are not important, regular is important)
	//text::Nature of dermal ornament
	//text::tuberculate
	//text::fairly regular pit and ridge 
	//text::irregular [PATO:irregular spatial pattern, irregular shape, irregular sleep pattern, etc.]
	//text::absent or almost absent
	//TODO make sure each state has an EQ on the key entities.?
	private void fixIncompleteStates(String src, Element root) {

		ArrayList<String> incompletestateids = new ArrayList<String>();//not ontologized character state (0 EQ for this state)
		ArrayList<EQStatement> completestateids = new ArrayList<EQStatement>();//ontologized E and Q 
		identifyStates(incompletestateids, completestateids);
		if(incompletestateids.size()!=0){
			//find qualityids from completed states for the key entities
			ArrayList<String> qualitylabels = new ArrayList<String>();
			EQStatement keyEQ = null;
			for(EQStatement EQ: completestateids){
				String entitylabel = null;
				Entity e = EQ.getEntity();
				if(e instanceof SimpleEntity) entitylabel = ((SimpleEntity)e).getLabel();
				else entitylabel = ((CompositeEntity)e).getPrimaryEntity().getLabel();
				if(matchWithKeyEntities(entitylabel)){
					keyEQ = EQ;
					String qlabel = EQ.getQuality().getLabel();
					if(qlabel.compareTo("absent")!=0) qualitylabels.add(qlabel); //ignore absent
				}
			}
			//deal with incomplete states
			for(String stateid: incompletestateids){
				boolean solved = false;
				String text = "";
				try{
					Element texte = (Element) XPath.selectSingleNode(root, ".//statement[@state_id='"+stateid+"']/text");
					text = texte.getTextNormalize();
				}catch(Exception e){
					e.printStackTrace();
				}
				String [] tokens = text.split("\\s+");
				for(int n =1; n <= (tokens.length>=4?4:tokens.length); n++){
					for(int b = 0; b < tokens.length-n+1; b++){
						String ngram = Utilities.join(tokens, b, b+n-1, " ");
						//TODO consider negation
						Quality q = (Quality) new TermSearcher().searchTerm(ngram, "quality"); 
						if(q!=null){
							String qlabel = q.getLabel();
							String cp = commonParent(qlabel, qualitylabels);
							if(cp!=null && cp.matches(".*?\\b("+dictionary.patoupperclasses+")\\b.*")){//TODO matches parent quality or any of its offsprings is fine.
								EQStatementProposals EQp = relatedEQ(stateid, ngram);
								if(EQp==null){ //add one
									EQp = new EQStatementProposals();
									EQStatement EQ = new EQStatement();
									//add metadata
									String characterid = "";
									try{
										Element statement = (Element) XPath.selectSingleNode(root, ".//statement[@state_id='"+stateid+"']");
										characterid = statement.getAttributeValue("character_id");
									}catch(Exception e){
										e.printStackTrace();
									}
									EQ.setSource(src);
									EQ.setCharacterId(characterid);
									EQ.setStateId(stateid);
									EQ.setDescription(text);
									EQp.add(EQ);
									allEQs.add(EQp);
								}
								//accept this result for this stateid
								EQStatement EQ = EQp.getProposals().get(0); //assuming there is only one candidate???
								EQ.setEntity(keyEQ.getEntity());
								EQ.setQuality(q);
								solved = true;
								break;
							}
						}
					}
					if(solved) break;
				}
			}
		}
	}
	
	/**
	 * 
	 * @param stateid
	 * @param ngram
	 * @return the EQ from allEQs with the stateid and included ngram in an element
	 */

	private EQStatementProposals relatedEQ(String stateid, String ngram) {
		ArrayList<EQStatementProposals> EQs = this.getEQsforState(stateid);
		for(EQStatementProposals EQ: EQs){
			String value = EQ.getPhrase();
			if(value!=null && value.length()>0 && (value.contains(ngram) || ngram.contains(value))){
				return EQ;
			}			
		}
		return null;
	}

	/**
	 * return the average distance of qlabel to qualitylabels in their ontology
	 * if qlabel is from an ontology different from qualitylabels, set the distance = 1000
	 * @param qid
	 * @param qualityids
	 * @return
	 */
	private String commonParent(String qlabel, ArrayList<String> qualitylabels) {
		for(String qualitylabel: qualitylabels){
			String cp = commonParentBtw(qlabel, qualitylabel);
			if(cp!=null && cp.matches(".*?\\b("+dictionary.patoupperclasses+")\\b.*")){
				return cp;
			}			
		}
		return null;
	}

	
	/**
	 * return the average distance of qid1 to qid2 in their ontologies
	 * if qid1 is from an ontology different from qid2, set the distance = 1000
	 * @param qid
	 * @param qualityid
	 * @return
	 */
	private String commonParentBtw(String qlabel1, String qlabel2) {
		ArrayList<String> path1 = new ArrayList<String> ();
		ArrayList<String> path2 = new ArrayList<String> ();
		String parent = qlabel1;
		String temp[];
		while(parent.compareTo("quality")!=0){
			temp=ontoutil.retreiveParentInfoFromPATO(parent);
			parent = temp!=null?temp[1]:null;
			if((parent==null)||(parent.length()==0)) break;
			path1.add(parent);
		}
		parent = qlabel2;
		while(parent.compareTo("quality")!=0){
			temp=ontoutil.retreiveParentInfoFromPATO(parent);
			parent = temp!=null?temp[1]:null;
			if((parent==null)||(parent.length()==0)) break;
			path2.add(parent);
		}
		if(path1.size()==0 || path2.size()==0) return null;
		int dist1 = 0;
		int dist2 = 0;
		for(String p1 : path1){
			dist1++;
			dist2 = 0;
			for(String p2: path2){
				dist2++;
				if(p2.matches(".*(^|,)"+p1+"(,|$).*")){
					if(p2.contains(p1)) return p1;
					if(p1.contains(p2)) return p2;
				}
			}
		}
		return null;
	}

	/**
	 * 
	 * @param entitylabel
	 * @return true if the entitylabel matches one of the key entities.
	 */
	private boolean matchWithKeyEntities(String entitylabel) {
		if (this.keyentities == null) return false;
		for(EntityProposals keyentityp: this.keyentities){
			for(Entity keyentity: keyentityp.getProposals()){
				String label = null;
				if(keyentity instanceof SimpleEntity) label = ((SimpleEntity)keyentity).getLabel();
				if(keyentity instanceof CompositeEntity) label = ((CompositeEntity)keyentity).getPrimaryEntity().getLabel();
				if(label !=null && label.compareTo(entitylabel)==0) return true;
			}
		}		
		return false;
	}

	/**
	 * 
	 * @param stateid
	 * @return the EQs in allEQs that have the stateid 
	 */
	private ArrayList<EQStatementProposals> getEQsforState(String stateid) {
		ArrayList<EQStatementProposals> EQs = new ArrayList<EQStatementProposals>();
		for(EQStatementProposals EQp: allEQs){
			for(EQStatement EQ: EQp.getProposals()){
				if(EQ.getStateId().compareTo(stateid)==0){
					EQs.add(EQp);
					continue;
				}				
			}
		}
		return EQs;
	}

	/**
	 * populate two parameters with results saved in allEQs
	 * @param incompletestateids: EQs of states with keyentity as an entity but without any qualityid/label, and states without any entity or quality
	 * @param completestateids: EQs of states with keyentity as an entity and with qualityid/label
	 */
	private void identifyStates(ArrayList<String> incompletestateids,
			ArrayList<EQStatement> completestateeqs) {
		for(EQStatementProposals EQp: allEQs){
			if(EQp.getType() !=null && EQp.getType().compareTo("state")==0){
				String stateid = EQp.getStateId();
				 ArrayList<EQStatementProposals> EQs = getEQsforState(stateid);
				 boolean hasentity = false;
				 boolean hasquality = false;
				 boolean haskeyentity = false;
				 for(EQStatementProposals aEQp: EQs){
					 //need to examine the effectiveness of this method in the context of the proposals
					 //should only highconfidence score EQs be considered?
					 for(EQStatement aEQ: aEQp.getProposals()){
						 Entity E = aEQ.getEntity();
						 String e = null;
						 hasentity = false;
						 hasquality = false;
						 haskeyentity = false;
						 if(E instanceof SimpleEntity)
						 {
							 e = ((SimpleEntity)E).getLabel();
							 if(((SimpleEntity)E).isOntologized()==true)
							 {
								 if(e.length()>0) hasentity = true; 
								 if(hasentity && matchWithKeyEntities(e)) haskeyentity = true; //haskeyentity is true if any of the proposal meets the condition
							 }
						 }
						 else
						 {
							 e= ((CompositeEntity)E).getPrimaryEntity().getLabel();
							 if(((CompositeEntity)E).isOntologized()==true)
							 {
								 if(e.length()>0) hasentity = true; 
								 if(hasentity && matchWithKeyEntities(e)) haskeyentity = true; 
							 }
						 }	
						 
						 String q = aEQ.getQuality()!=null?aEQ.getQuality().getLabel():""; //ternary operator added => Hariharan
						 if(q==null) q="";
						 
						 if(q.length()>0) hasquality = true;
						 if(haskeyentity && hasquality) completestateeqs.add(aEQ);
					 }	
				 }
				 if(!hasquality) incompletestateids.add(stateid); //none of the EQs for the state has a ontologized quality
				 //if(!hasentity && !hasquality) incompletestates.addAll(EQs);
				 //if(haskeyentity && !hasquality) incompletestates.addAll(EQs); //none of the EQs for the state has a ontologized quality
			}
		}
	}

	/**
	 * select structures that have characters and/or are from structure in a relation
	 * 
	 * @param statement
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private List<Element> selectEntityStructures(Element statement) {
		ArrayList<Element> selected = new ArrayList<Element>();
		try{
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
		}catch(Exception e){
			e.printStackTrace();
		}
		return selected;
	}


	

	

	/*private void insertEQs2Table(EQStatement EQ) throws Exception {
		

		// print
		String entitylabel = EQ.get("entitylabel")+"["+EQ.get("entityid")+"]";
		String quality = EQ.get("qualitylabel")+"["+EQ.get("qualityid")+"]";
		String qualitynegated = EQ.get("qualitynegatedlabel");
		String qualitymodifierlabel = EQ.get("qualitymodifierlabel")+"["+EQ.get("qualitymodifierid")+"]";
		String entitylocator = EQ.get("entitylocator");
		String entitylocatorlabel = EQ.get("entitylocatorlabel")+"["+EQ.get("entitylocatorid")+"]";

		if (quality.length() == 0 && qualitynegated.length() == 0){
			System.out.println("EQ::[E]" + entitylabel + " [Q]" + quality + (qualitymodifierlabel.length() > 0 ? " [QM]" + qualitymodifierlabel : "")
					+ (entitylocatorlabel.length() > 0 ? " [EL]" + entitylocatorlabel : ""));
		}else
		// quality and qualitynegated can not both hold values! //changed by hong march 2013, they can hold the same value: quality=qualitynegated
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

	}*/


	
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
	/*private boolean isRelated2KeyEntities(String entity) {
		String[] tokens = entity.split("\\s*,\\s*");
		for (String token : tokens) {
			for(String keyentity:this.keyentities){
				if (token.contains(keyentity) || keyentity.contains(token))
					return true;
			}
		}
		return false;
	}*/



	//if not akeyentity and not key element, may need to constructure new entity and/or inherit entity locators from keyentities.
	/**
	 * 
	 * @param EQ: to be updated with an entity locator
	 * @param entitylabel
	 */
	private void inheritEntityLocator(EQStatement EQ, String entity){
		
		
	/*	String elid = EQ.get("entitylocatorid");
		for(Entity keyentity: this.keyentities){
			String keyentityphrase = keyentity.getPrimaryEntityString();
			if(keyentityphrase!=null && keyentityphrase.compareTo(entity)==0){ //if entityphrase and keyentityphrase are the same, inherit the entity locator
				String entitylocator = keyentity.get("entitylocator");
				String entitylocatorid = keyentity.get("entitylocatorid");
				String entitylocatorlabel = keyentity.get("entitylocatorlabel");
				if(elid==null || elid.length()==0){
					EQ.put("entitylocator", entitylocator==null? "":entitylocator);
					EQ.put("entitylocatorid", entitylocatorid==null? "":entitylocatorid);
					EQ.put("entitylocatorlabel", entitylocatorlabel==null? "":entitylocatorlabel);
				}else if(elid.compareTo(entitylocatorid)!=0){
					EQ.put("entitylocator", EQ.get("entitylocator")+","+entitylocator==null? "":entitylocator);
					EQ.put("entitylocatorid", EQ.get("entitylocatorid")+","+entitylocatorid==null? "":entitylocatorid);
					EQ.put("entitylocatorlabel", EQ.get("entitylocatorlabel")+","+entitylocatorlabel==null? "":entitylocatorlabel);
				}
			}
		}*/
	}
	

	/**
	 * if resultsfromrelations.get("entitylocator")!=null
	 * @param resultsfromrelations
	 */
/*	private void addentitylocator4keyentities(
			Hashtable<String, Object> resultsfromrelations, String entitylabel) {
		if(resultsfromrelations != null && entitylabel !=null){
			String entitylocator = (String)resultsfromrelations.get("entitylocator");
			if(entitylocator != null){
				String entitylocatorid = (String)resultsfromrelations.get("entitylocatorid");
				String entitylocatorlabel = (String)resultsfromrelations.get("entitylocatorlabel");
				for(Hashtable<String, String> keyentity: this.keyentities){
					String keyentitylabel = keyentity.get("entitylabel");
					if(keyentitylabel!=null){
						if(keyentitylabel.compareTo(entitylabel)==0){					
							keyentity.put("entitylocator", entitylocator);
							if(entitylocatorid!=null) keyentity.put("entitylocatorid", entitylocatorid);
							if(entitylocatorlabel!=null) keyentity.put("entitylocatorlabel", entitylocatorlabel);
						}
					}
				}
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
		
		String srcdir = ApplicationUtilities.getProperty("source.dir")+"test";

		String database =ApplicationUtilities.getProperty("database.name");
		String outputtable=ApplicationUtilities.getProperty("table.output");;
		String prefix =ApplicationUtilities.getProperty("table.prefix");
		String glosstable = "fishglossaryfixed";

		try {
			XML2EQ x2e = new XML2EQ(srcdir, database, outputtable, /* benchmarktable, */prefix, glosstable);
			x2e.outputEQs();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
	}

}
