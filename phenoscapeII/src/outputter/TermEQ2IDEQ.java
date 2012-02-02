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
import java.util.Hashtable;
import java.util.TreeSet;
import conceptmapping.Utilities;

/**
 * @author Hong Updates
 * This class finds IDs from ontologies for Entity and Quality terms
 * and fill in the blank ID columns in the outputtable
 */
public class TermEQ2IDEQ {
	private String outputtable;
	private Connection conn;
	private String username="root";
	private String password="root";
	private TreeSet<String> entityterms = new TreeSet<String>();
	private TreeSet<String> qualityterms = new TreeSet<String>();
	//private Hashtable<String, String> entityIDCache = new Hashtable<String, String>();
	//private Hashtable<String, String> qualityIDCache = new Hashtable<String, String>();
	
	/**
	 * 
	 */
	public TermEQ2IDEQ(String database, String outputtable) {
		this.outputtable = outputtable;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select entity, qualitymodifier, entitylocator from "+outputtable);
				while(rs.next()){
					String e = normalize(rs.getString("entity"));
					if(e.length()>0) entityterms.add(e);
					e = normalize(rs.getString("qualitymodifier"));
					if(e.length()>0) entityterms.add(e);
					e = normalize(rs.getString("entitylocator"));
					if(e.length()>0) entityterms.add(e);
				}
				rs = stmt.executeQuery("select quality from "+outputtable);
				while(rs.next()){
					String e = normalize(rs.getString("quality"));
					if(e.length()>0)qualityterms.add(e);
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param string
	 * @return
	 */
	private String normalize(String string) {
		string = string.replaceAll("\\[.*?\\]", "");//remove [usually]
		string = string.replaceAll("[()]", ""); //turn dorsal-(fin) to dorsal-fin
		string = string.replaceAll("-to\\b", " to"); //turn dorsal-to to dorsal to
		return string.trim();
	}

	public void fillInIDs(){
		for(String entityterm: entityterms){
			ArrayList<String[]> results = Utilities.searchOntologies(entityterm, "entity");
			if(results!=null && results.size()>=1) insertEntityResults2Table(entityterm, results);
		}
		for(String qualityterm: qualityterms){
			ArrayList<String[]>  results = Utilities.searchOntologies(qualityterm, "quality");
			if(results!=null && results.size()>=1)insertQualityResults2Table(qualityterm, results);
		}		
	}
	
	/**
	 * 
	 * @param entityterm
	 * @param results: a two dimensional array holding multiple mappings, 
	 * each mapping contains 3 elements: type, id, and label
	 */
	private void insertQualityResults2Table(String qualityterm, ArrayList<String[]>  results) {
		try{
			String id = results.get(0)[1]+":"+results.get(0)[2];
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set qualityid='"+id+"' where quality='"+qualityterm+"'");
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * 
	 * @param entityterm
	 * @param results: a two dimensional array holding multiple mappings, 
	 * each mapping contains 3 elements: type, id, and label
	 */
	private void insertEntityResults2Table(String entityterm, ArrayList<String[]> results) {
		try{
			String id = results.get(0)[1]+":"+results.get(0)[2];
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set entityid='"+id+"' where entity='"+entityterm+"'");
			stmt.execute("update "+this.outputtable+" set entitylocatorid='"+id+"' where entitylocator='"+entityterm+"'");
			stmt.execute("update "+this.outputtable+" set qualifymodifierid='"+id+"' where qualitymodifier='"+entityterm+"'");			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TermEQ2IDEQ t2id = new TermEQ2IDEQ("phenoscape", "biocreative_nexml2eq");
		t2id.fillInIDs();

	}

}
