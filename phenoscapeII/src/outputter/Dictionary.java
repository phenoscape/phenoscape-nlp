package outputter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;

public class Dictionary {
	public Connection conn;
	public String patoupperclasses = "2-D shape|shape|size|position|closure|structure|count|optical quality|composition|texture|behavioral quality|mobility|mass|quality of a solid";
	//spatial terms form BSPO
	public ArrayList<String> spatialterms = new ArrayList<String>();
	public static String process="crest|ridge|process|tentacule|shelf|flange|ramus";
	public static String binaryTvalues = "present|true|yes|usually";//added present/absent
	public static String binaryFvalues = "absent|false|no|rarely";
	public static String positionprep = "of|part_of|in|on|between";
	//Changed by Zilong
	public static String selfReference = "counterpart";//Extendible
	public static String contact="connection|contact|interconnection";//Extendible
	public static String spatialtermptn="";
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
	public Hashtable<String, String> relationalqualities = new Hashtable<String, String>();
	public Hashtable<String, String> resrelationQ = new Hashtable<String, String>();
	public Dictionary() {

		try{
			
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			
			Statement stmt = conn.createStatement();
			
			//load spatial terms
			ResultSet rs = stmt.executeQuery("select distinct term from uniquespatialterms");
			while(rs.next()){
				String term = rs.getString("term");
				term = term.replaceAll("\\(.*?\\)", "").trim(); //remove "(obsolete)"
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
		resrelationQlist();
		relationalquality();
				
	}
	
	private void resrelationQlist() {
		
		resrelationQ.put("BFO:0000053","bearer_of");
		resrelationQ.put("RO:0002220","adjacent_to");
		resrelationQ.put("BSPO:0000096","anterior_to");
		resrelationQ.put("UBERON:anteriorly_connected_to","anteriorly_connected_to");
		resrelationQ.put("UBERON:attaches_to","attaches_to");
		resrelationQ.put("PHENOSCAPE:extends_from","extends_from");
		resrelationQ.put("RO:0002150","connected_to");
		resrelationQ.put("PATO:decreased_in_magnitude_relative_to","decreased_in_magnitude_relative_to");
		resrelationQ.put("BSPO:0000107","deep_to");
		resrelationQ.put("RO:0002202","develops_from");
		resrelationQ.put("BSPO:0000097","distal_to");
		resrelationQ.put("UBERON:distally_connected_to","distally_connected_to");
		resrelationQ.put("BSPO:0000098","dorsal_to");
		resrelationQ.put("UBERON:encloses","encloses");
		resrelationQ.put("PHENOSCAPE:extends_to","extends_to");
		resrelationQ.put("PATO:has_cross_section","has_cross_section");
		resrelationQ.put("UBERON:has_muscle_insertion","has_muscle_insertion");
		resrelationQ.put("UBERON:has_muscle_origin","has_muscle_origin");
		resrelationQ.put("BFO:0000051","has_part");
		resrelationQ.put("BSPO:0000123","in_anterior_side_of");
		resrelationQ.put("BSPO:0000125","in_distal_side_of");
		resrelationQ.put("UBERON:in_lateral_side_of","in_lateral_side_of");
		resrelationQ.put("BSPO:0000120","in_left_side_of");
		resrelationQ.put("UBERON:in_median_plane_of","in_median_plane_of");
		resrelationQ.put("BSPO:0000122","in_posterior_side_of");
		resrelationQ.put("BSPO:0000124","in_proximal_side_of");
		resrelationQ.put("BSPO:0000121","in_right_side_of");
		resrelationQ.put("PATO:increased_in_magnitude_relative_to","increased_in_magnitude_relative_to");
		resrelationQ.put("OBO_REL:located_in","located_in");
		resrelationQ.put("RO:0002131","overlaps");
		resrelationQ.put("BFO:0000050","part_of");
		resrelationQ.put("BSPO:passes_through","passes_through");
		resrelationQ.put("BSPO:0000099","posterior_to");
		resrelationQ.put("UBERON:posteriorly_connected_to","posteriorly_connected_to");
		resrelationQ.put("BSPO:0000100","proximal_to");
		resrelationQ.put("UBERON:proximally_connected_to","proximally connected to");
		resrelationQ.put("PATO:similar_in_magnitude_relative_to","similar_in_magnitude_relative_to");
		resrelationQ.put("RO:0002219","surrounded by");
		resrelationQ.put("RO:0002221","surrounds");
		resrelationQ.put("BSPO:0000102","ventral_to");
		resrelationQ.put("BSPO:0000103","vicinity_of");
		resrelationQ.put("PHENOSCAPE:serves_as_attachment_site_for","serves_as_attachment_site_for");
		resrelationQ.put("BFO:0000052","inheres_in");
		resrelationQ.put("PHENOSCAPE:complement_of","not");

		
	}

	void relationalquality()
	{
		relationalqualities.put("anterior to", "BSPO:0000096");
		relationalqualities.put("ahead of", "BSPO:0000096");
		relationalqualities.put("not extending to", "BSPO:0000096");
		
		relationalqualities.put("attached to", "UBERON:attaches_to");
		relationalqualities.put("attach to", "UBERON:attaches_to");
		relationalqualities.put("attaching to", "UBERON:attaches_to");
		relationalqualities.put("attaches to", "UBERON:attaches_to");		
		relationalqualities.put("extends between", "UBERON:attaches_to");
		relationalqualities.put("inserting on", "UBERON:attaches_to");
		relationalqualities.put("inserting into", "UBERON:attaches_to");
		relationalqualities.put("extend between", "UBERON:attaches_to");
		relationalqualities.put("extended between", "UBERON:attaches_to");
		relationalqualities.put("extending between", "UBERON:attaches_to");

		
		relationalqualities.put("located in", "OBO_REL:located_in");
		relationalqualities.put("situated on", "OBO_REL:located_in");
		relationalqualities.put("located", "OBO_REL:located_in");
		relationalqualities.put("placed on", "OBO_REL:located_in");

		relationalqualities.put("beyond", "BSPO:0000099");
		
		relationalqualities.put("as far as", "RO:0002220");
		relationalqualities.put("along", "RO:0002220");
		relationalqualities.put("near", "RO:0002220");
		relationalqualities.put("closer to", "RO:0002220");
		relationalqualities.put("extends anteriorly to", "RO:0002220");
		relationalqualities.put("project towards", "RO:0002220");
		relationalqualities.put("reaching", "RO:0002220");
		

		relationalqualities.put("bearer of","BFO:0000053");
		relationalqualities.put("adjacent to","RO:0002220");
		//relationalqualities.put("anterior to","BSPO:0000096");
		relationalqualities.put("anteriorly connected to","UBERON:anteriorly_connected_to");
		//relationalqualities.put("attaches to","UBERON:attaches_to");
		relationalqualities.put("extends from","PHENOSCAPE:extends_from");
		relationalqualities.put("connected to","RO:0002150");
		relationalqualities.put("decreased in magnitude relative to","PATO:decreased_in_magnitude_relative_to");
		relationalqualities.put("deep to","BSPO:0000107");
		relationalqualities.put("develops from","RO:0002202");
		relationalqualities.put("distal to","BSPO:0000097");
		relationalqualities.put("distally connected to","UBERON:distally_connected_to");
		relationalqualities.put("dorsal to","BSPO:0000098");
		relationalqualities.put("encloses","UBERON:encloses");
		relationalqualities.put("extends to","PHENOSCAPE:extends_to");
		relationalqualities.put("has cross section","PATO:has_cross_section");
		relationalqualities.put("has muscle insertion","UBERON:has_muscle_insertion");
		relationalqualities.put("has muscle origin","UBERON:has_muscle_origin");
		relationalqualities.put("has part","BFO:0000051");
		relationalqualities.put("in anterior side of","BSPO:0000123");
		relationalqualities.put("in distal side of","BSPO:0000125");
		relationalqualities.put("in lateral side of","UBERON:in_lateral_side_of");
		relationalqualities.put("in left side of","BSPO:0000120");
		relationalqualities.put("in median plane of","UBERON:in_median_plane_of");
		relationalqualities.put("in posterior side of","BSPO:0000122");
		relationalqualities.put("in proximal side of","BSPO:0000124");
		relationalqualities.put("in right side of","BSPO:0000121");
		relationalqualities.put("increased in magnitude relative to","PATO:increased_in_magnitude_relative_to");
		//relationalqualities.put("located in","OBO_REL:located_in");
		relationalqualities.put("overlaps","RO:0002131");
		relationalqualities.put("part of","BFO:0000050");
		relationalqualities.put("passes through","BSPO:passes_through");
		relationalqualities.put("posterior to","BSPO:0000099");
		relationalqualities.put("posteriorly connected to","UBERON:posteriorly_connected_to");
		relationalqualities.put("proximal to","BSPO:0000100");
		relationalqualities.put("proximally connected to","UBERON:proximally_connected_to");
		relationalqualities.put("similar in magnitude relative to","PATO:similar_in_magnitude_relative_to");
		relationalqualities.put("surrounded by","RO:0002219");
		relationalqualities.put("surrounds","RO:0002221");
		relationalqualities.put("ventral to","BSPO:0000102");
		relationalqualities.put("vicinity of","BSPO:0000103");
		relationalqualities.put("serves as attachment site for","PHENOSCAPE:serves_as_attachment_site_for");
		relationalqualities.put("inheres in","BFO:0000052");
		relationalqualities.put("not","PHENOSCAPE:complement_of");

	}
}
