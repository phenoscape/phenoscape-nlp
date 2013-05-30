package outputter;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;

import org.semanticweb.owlapi.model.OWLClass;

import owlaccessor.OWLAccessorImpl;

import edu.mit.jwi.IDictionary;


public class Dictionary {
	public static Connection conn;
	//see http://phenoscape.svn.sourceforge.net/viewvc/phenoscape/trunk/vocab/character_slims.obo from Jim
	public static String patoupperclasses = "2-D shape|cellular quality|shape|size|position|closure|structure|count in organism|optical quality|composition|texture|physical quality of a process|behavioral quality|mobility|mass|quality of a solid";
	//spatial terms form BSPO
	public static ArrayList<String> spatialterms = new ArrayList<String>();
	public static String process="crest|ridge|process|tentacule|shelf|flange|ramus";
	public static String binaryTvalues = "present|true|yes|usually";//added present/absent
	public static String binaryFvalues = "absent|false|no|rarely";
	public static String positionprep = "of|part_of|in|on|between";
	//Changed by Zilong
	public static String selfReference = "counterpart";//Extendible
	public static String contact="connection|contact|interconnection";//Extendible
	public static String spatialtermptn="";
	public static String prefixes = "post|pre|post-|pre-";
	public static String negation = "absent|lacking";

	//By Zilong:
	//sometimes, spatial terms could be used as adjectives to modify head nouns. 
	//Instead of directly using <spatial terms+head nouns> when searching the ontology,
	//the program should interpret the pattern as a part_of relation. 
	//eg. "anterior coracoid process" should be interpreted as "anterior region(part_of(coracoid))"
	//This list contains all identified head nouns
	public static ArrayList<String> spatialHeadNoun = new ArrayList<String>();
	//eg. <"unossified", "ossification,absent">; ossified => with ossification => ossification = present
	public static Hashtable<String, String> verbalizednouns = new Hashtable<String,String>();
	//this instance var is used to map spatial terms that are not in ontology to terms that are.
	/** The spatial maps. */
	public static Hashtable<String, String> spatialMaps = new Hashtable<String, String>();
	public static Hashtable<String,Hashtable<String, String>> relationalqualities = new Hashtable<String,Hashtable<String, String>>();
	public static Hashtable<String, String> resrelationQ = new Hashtable<String, String>();
	public static Hashtable<String, String> parentclass2label = new Hashtable<String, String>();
	
	/** special ontology classes **/
	public static String mcorganism="UBERON:0000468"; //multi-cellular organism
	public static String anatprojection = "anatomical projection";
	public static String cellquality = "http://purl.obolibrary.org/obo/PATO_0001396";
	public static String patoiri="http://purl.obolibrary.org/obo/";
	
	public static Hashtable<String, String> singulars = new Hashtable<String, String>();
	public static Hashtable<String, String> plurals = new Hashtable<String, String>();
	//private ArrayList<Hashtable<String, String>>  alladjectiveorgans = new ArrayList<Hashtable<String, String>> (); //one hashtable from an ontology
	public static IDictionary wordnetdict = new edu.mit.jwi.Dictionary(new File(ApplicationUtilities.getProperty("wordnet.dictionary")));
	
	//special cases for singulars and plurals
	static{
		//check cache
		singulars.put("axis", "axis");
		singulars.put("axes", "axis");
		singulars.put("bases", "base");
		singulars.put("boss", "boss");
		singulars.put("buttress", "buttress");
		singulars.put("callus", "callus");
		singulars.put("frons", "frons");
		singulars.put("grooves", "groove");
		singulars.put("interstices", "interstice");
		singulars.put("lens", "len");
		singulars.put("media", "media");
		singulars.put("midnerves", "midnerve");
		singulars.put("process", "process");
		singulars.put("series", "series");
		singulars.put("species", "species");
		singulars.put("teeth", "tooth");
		singulars.put("valves", "valve");
		singulars.put("i", "i"); //could add more roman digits
		singulars.put("ii", "ii");
		singulars.put("iii", "iii");
		
		plurals.put("axis", "axes");
		plurals.put("base", "bases");		
		plurals.put("groove", "grooves");
		plurals.put("interstice", "interstices");
		plurals.put("len", "lens");
		plurals.put("media", "media");
		plurals.put("midnerve", "midnerves");
		plurals.put("tooth", "teeth");
		plurals.put("valve", "valves");
		plurals.put("boss", "bosses");
		plurals.put("buttress", "buttresses");
		plurals.put("callus", "calluses");
		plurals.put("frons", "fronses");
		plurals.put("process", "processes");
		plurals.put("series", "series");
		plurals.put("species", "species");
		plurals.put("i", "i"); //could add more roman digits
		plurals.put("ii", "ii");
		plurals.put("iii", "iii");
	}
	//upper level quality classes 
	static{
		parentclass2label.put("PATO:0000186", "behavioral quality");
		parentclass2label.put("PATO:0001396", "cellular quality");
		parentclass2label.put("PATO:0000136", "closure");
		parentclass2label.put("PATO:0000025", "composition");
		parentclass2label.put("PATO:0000070", "count in organism");
		parentclass2label.put("PATO:0000125", "mass");
		parentclass2label.put("PATO:0000004", "mobility");
		parentclass2label.put("PATO:0001300", "optical quality");
		parentclass2label.put("PATO:0002062", "physical quality of a process");
		parentclass2label.put("PATO:0000140", "position");
		parentclass2label.put("PATO:0001546", "quality of a solid");
		parentclass2label.put("PATO:0000052", "shape");
		parentclass2label.put("PATO:0000117", "size");
		parentclass2label.put("PATO:0000141", "structure");
		parentclass2label.put("PATO:0000150", "texture");
	}
	//load spatial terms and construct spatial term pattern
	static{
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
					spatialterms.add(term);
					spatialtermptn += term+"|";
				}
			}
			spatialtermptn = spatialtermptn.replaceFirst("\\|$", "");
		}catch(Exception e){
			e.printStackTrace();
		}
		spatialterms.add("accessory");
		
	}
	//other spatial related info
	static{
		//spatialHeadNoun: TODO make it more flexible
		spatialHeadNoun.add("coronoid");
		spatialHeadNoun.add("process");
		spatialHeadNoun.add("coracoid");
		
		//un-ed pattern, TODO make it more flexible
		verbalizednouns.put("unossified", "ossification,absent");
		
		spatialMaps.put("portion", "region");	
		//end is defined to be region => distal end => distal region
		spatialMaps.put("end", "region");
		//spatialMaps.put("end", "margin");
	}
	//legal relational qualities
	static{	
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
	//syn phrases mapping to relational qualities
	static{
	//THis code populates the relationalqualities from Pato - Hariharan	
		File pato_file = new File(ApplicationUtilities.getProperty("ontology.dir")+"/pato.owl");
		//String url = "http://obo.svn.sourceforge.net/viewvc/obo/uberon/trunk/merged.owl";
		OWLAccessorImpl a = new OWLAccessorImpl(pato_file, new ArrayList<String>());
		for(OWLClass b:a.getRelationalSlim())
			{
			String root_form = Utilities.removeprepositions(a.getLabel(b).trim());
			if(relationalqualities.containsKey(root_form))
			{
				Hashtable<String,String> list = relationalqualities.get(root_form);
				list.put(a.getLabel(b).trim(), a.getID(b).trim());
			}
			else
			{
				Hashtable<String,String> list = new Hashtable<String,String>();
				list.put(a.getLabel(b).trim(), a.getID(b).trim());
				relationalqualities.put(root_form,list);
			}
			}
		System.out.println("");
	}
	//wordnet 
	static{
		try {
			wordnetdict.open();					
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
