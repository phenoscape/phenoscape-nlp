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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Pattern;

import org.jdom.xpath.XPath;

/**
 * @author updates
 * This class replaces XML2EQ and TermEQ2IDEQ. 
 * Outline of the algorithm:
 * input: fine-grained markup in xml
 * output: EQ statements with IDs
 * 
 * 1. identify structure anchors from all structure phrases. E Anchors are the phrases that have exact match in an anatomy ontology
 * 2. identify character state anchors from all character state phrases. Q Anchors are the character state phrases that have exact match in a quality ontology
 * 3. process xml files one by one (1 file = 1 charater statment + n state statement), each file will generate 0 - n EQ statements.
 * 3.1 find a structure phrase, apply various search_strategies to find possible matches, rank the matches by the distances to the E anchors
 * 3.2 find the structure's quality, apply various search_strategies to find possible matches, rank the matches by the distances to the Q anchors  
 * 3.3.find the relations, use restricted relation list to filter out undesired relations
 * 
 * search_strategies:
 * 1. by parts of the phrases 
 * 2. stemmed phrases
 * 3. terms from definitions in wordnet or other dictionaries
 * 4. prefix: unossified => ossification
 * 5. remove spatial terms
 * 6. 
 * 
 */
public class EQGenerator {
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
	//Changed by Zilong
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

	public EQGenerator(String sourcedir, String database, String outputtable, String prefix, String glosstable) throws Exception {
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
	
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String srcdir = "C:\\Users\\Zilong Chang\\Desktop\\BSPOTest\\target\\final";
		String database = "biocreative2012";
		// String outputtable = "biocreative_nexml2eq";
		String outputtable = "bspotest1210_swartz_xml2eq";
		// String benchmarktable = "internalworkbench";
		String prefix = "bspotest_swatz";
		String glosstable = "fishglossaryfixed";
		try {
			EQGenerator x2e = new EQGenerator(srcdir, database, outputtable, /* benchmarktable, */prefix, glosstable);

			//x2e.outputEQs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
