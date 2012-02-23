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
	//private TreeSet<String> entityterms = new TreeSet<String>();
	//private TreeSet<String> qualityterms = new TreeSet<String>();
	private Hashtable<String, String> entityIDCache = new Hashtable<String, String>();
	private Hashtable<String, String> qualityIDCache = new Hashtable<String, String>();
	
	private String process="crest|ridge|process|tentacule|shelf|flange|ramus";
	
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
				ResultSet rs = stmt.executeQuery("select id, entitylabel, entitylocatorlabel, quality, qualitynegated, qualitymodifierlabel from "+outputtable+" where stateid!=''");
				while(rs.next()){
					String srcid = rs.getString("id");
					String entitylabel = rs.getString("entitylabel");
					String entitylocatorlabel = rs.getString("entitylocatorlabel");
					String quality = rs.getString("quality");
					String qualitynegated = rs.getString("qualitynegated");
					String qualitymodifierlabel = rs.getString("qualitymodifierlabel");
					fillInIDs(srcid, entitylabel, entitylocatorlabel, quality, qualitynegated, qualitymodifierlabel);
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	/**
	 * 
	 * @param srcid used to insert IDs back into the output table
	 * @param entitylabel will be updated to a label that matches an ID
	 * @param entitylocatorlabel will be updated to labels that match a set of IDs
	 * @param quality used to find an qualityID and qualitylabel
	 * @param qualitynegated used to find an qualityID, qualitynegatedlabel, qnparentlabel, and qnparentid
	 * @param qualitymodifierlabel will be updated to labels that match a set of IDs
	 */
	public void fillInIDs(String srcid, String entitylabel, String entitylocatorlabel, String quality, String qualitynegated, String qualitymodifierlabel){
		//first find update entitylabel
		fillInIDs4Entity(srcid, entitylabel, entitylocatorlabel); //0: label; 1:id
		if(quality.length()>0){//rounded dorsally
			fillInIDs4Quality(srcid, quality);
		}else if(qualitynegated.length()>0){
			fillInIDs4Qualitynegated(srcid, qualitynegated);
		}
	}
	
	
	private void fillInIDs4Qualitynegated(String srcid, String qualitynegated) {
		// TODO Auto-generated method stub
		
	}


	private void fillInIDs4Quality(String srcid, String quality) {
		// TODO Auto-generated method stub
		
	}

	/**
	 * update entitylabel, entityid, entitylocatorlabel, entitylocatorid
	 * @param srcid
	 * @param entitylabel
	 * @param entitylocatorlabel
	 */
	private void fillInIDs4Entity(String srcid, String entitylabel,
			String entitylocatorlabel) {
		entitylabel = entitylabel.replaceAll("("+this.process+")", "process");
		entitylocatorlabel = entitylocatorlabel.replaceAll("("+this.process+")", "process");
		//try a number of heuristics
		//search starts with the last token, and move progressively backwards
			//preopercular latero-sensory canal =>	preopercular sensory canal
			//ventrolateral corner => ventro-lateral region
			//pectoral-fin spine => pectoral fin spine
		//if an entity returned no match, try entitylocator one by one, update entitylabel and entitylocatorlabel
	}

	private Hashtable<String, String> searchPATO(String entityterm){
		
		
		
		ArrayList<String[]> results = Utilities.searchOntologies(entityterm, "entity");
		return null;
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
			stmt.execute("update "+this.outputtable+" set qualitymodifierid='"+id+"' where qualitymodifier='"+entityterm+"'");			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TermEQ2IDEQ t2id = new TermEQ2IDEQ("biocreative2012", "run0");
		t2id.fillInIDs();

	}

}
