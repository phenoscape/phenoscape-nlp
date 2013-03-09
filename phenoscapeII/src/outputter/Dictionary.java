package outputter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;

public class Dictionary {
	public Connection conn;
	//spatial terms form BSPO
	public ArrayList<String> spatialterms;
	public static String process="crest|ridge|process|tentacule|shelf|flange|ramus";
	public static String binaryTvalues = "true|yes|usually";
	public static String binaryFvalues = "false|no|rarely";
	public static String positionprep = "of|part_of|in|on|between";
	//Changed by Zilong
	public static String selfReference = "counterpart";//Extendible
	public static String contact="connection|contact|interconnection";//Extendible
	public static String spatialtermptn;
	//By Zilong:
	//sometimes, spatial terms could be used as adjectives to modify head nouns. 
	//Instead of directly using <spatial terms+head nouns> when searching the ontology,
	//the program should interpret the pattern as a part_of relation. 
	//eg. "anterior coracoid process" should be interpreted as "anterior region(part_of(coracoid))"
	//This list contains all identified head nouns
	public ArrayList<String> spatialHeadNoun = new ArrayList<String>();
	//eg. <"unossified", "ossification,absent">; ossified => with ossification => ossification = present
	public Hashtable<String, String> verbalizednouns = new Hashtable<String,String>();
	//this instance var is used to map spatial terms that are not in ontology to terms that are.
	/** The spatial maps. */
	public Hashtable<String, String> spatialMaps = new Hashtable<String, String>();


	public Dictionary(ArrayList<String> spatialterms) {
		this.spatialterms = spatialterms;
		try{
			
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			
			Statement stmt = conn.createStatement();
			
			//load spatial terms
			ResultSet rs = stmt.executeQuery("select distinct term from uniquespatialterms");
			while(rs.next()){
				String term = rs.getString("term").trim();
				if(term.length()>0){
					this.spatialterms.add(term);
					this.spatialtermptn += term+"|";
				}
			}
			this.spatialtermptn = this.spatialtermptn.replaceFirst("\\|$", "");
		}catch(Exception e){
			e.printStackTrace();
		}
		spatialterms.add("accessory");
		//spatialHeadNoun: TODO make it more flexible
		this.spatialHeadNoun.add("coronoid");
		this.spatialHeadNoun.add("process");
		this.spatialHeadNoun.add("coracoid");
		
		//un-ed pattern, TODO make it more flexible
		this.verbalizednouns.put("unossified", "ossification,absent");
		
		spatialMaps.put("portion", "region");
	}
}