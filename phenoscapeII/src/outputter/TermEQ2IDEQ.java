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
	private Hashtable<String, String[]> entityIDCache = new Hashtable<String, String[]>(); //term=> {id, label}
	private Hashtable<String, String[]> qualityIDCache = new Hashtable<String, String[]>();
	
	private String process="crest|ridge|process|tentacule|shelf|flange|ramus";
	private boolean debug = true;
	
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
	 * Search a phrase A B C
	 * search A B C
	 * if succeeds, search the parent entity locator + A B C [tooth => ceratobranchial 5 tooth]
	 *             if succeeds, entitylabel = p.e.l + A B C, entitylocator = entitylocator - p.e.l
	 *             if fails, entitylabel = A B C, entitylocator = entitylocator
	 * if fails, search B C
	 *             if succeeds, entitylabel = B C, entitylocator = entitylocator
	 *             if fails, search C
	 *             			 if succeeds, entitylabel = C, entitylocator = entitylocator
	 *             			 if fails, search the parent entity locator
	 *                                 if succeeds, entitylable = p.e.l*, entitylocator = entitylocator - p.e.l
	 *                                 if fails, search the next parent entity locator
	 *                                 ....
	 *                                 
	 *                                   
	 *                                   
	 * then lookup IDs for other terms in entitylocators                                  	
	 * update entitylabel, entityid, entitylocatorlabel, entitylocatorid using srcid
	 * @param srcid
	 * @param entitylabel
	 * @param entitylocatorlabel
	 */
	
	
	
	
	private void fillInIDs4Entity(String srcid, String entitylabel,
			String entitylocatorlabel) {
		if(this.debug){
			System.out.println("entity terms:["+entitylabel+"]["+entitylocatorlabel+"]");
		}
		
		entitylabel = entitylabel.replaceAll("("+this.process+")", "process");
		entitylocatorlabel = entitylocatorlabel.replaceAll("("+this.process+")", "process");
		String finalentitylocator = "";
		String finalentitylabel = "";
		String finalentityid = "";
		String[] entitylocators = null;
		if(entitylocatorlabel.length()>0) entitylocators = entitylocatorlabel.split("\\s*,\\s*");
		String[] entitylabeltokens = entitylabel.split("\\s+");
		//unhandled cases: 
		//upper pharyngeal tooth plates 4 and 5 => upper pharyngeal tooth plate
		//humeral deltopectoral crest apex => process
		int size = entitylabeltokens.length - 1;
		for(int i = 0; i <= size; i++){
			String entityterm = join(entitylabeltokens, i, size, " "); 
			String [] result = searchTerm(entityterm, "entity");
			if(result!=null){
				if(entitylocators != null && i==0){//has entitylocator
					String [] newresult = searchTerm(entitylocators[0]+" "+entityterm, "entity");
					if(newresult!=null){
						finalentityid = newresult[0];
						finalentitylabel = newresult[1];
						finalentitylocator = entitylocatorlabel.replaceFirst(entitylocators[0], "").replaceAll("^\\s*,\\s*", "");
						break;
					}
				}else{
					finalentityid = result[0];
					finalentitylabel = result[1];
					finalentitylocator = entitylocatorlabel;
					break;
				}			
			}else{
				if(i == size && entitylocators!= null){//entitylabel returned no result, try entitylocators
					int j = 0;
					while(result==null && j<entitylocators.length){
						result = searchTerm(entitylocators[j], "entity");
						j++;
					}
					if(result!=null){
						finalentityid = result[0];
						finalentitylabel = result[1];
						finalentitylocator = join(entitylocators, j, entitylocators.length-1, ",");
						break;
					}
				}
			}				
		}
		
		String finalentitylocatorids = "";
		String finalentitylocatorlabels="";
		if(finalentitylocator.length()>0){
			String[] finalentitylocators = finalentitylocator.split("\\s*,\\s*");
			for(String fel: finalentitylocators){
				String [] result = searchTerm(fel, "entity");
				if(result!=null){
					finalentitylocatorids += result[0]+",";
					finalentitylocatorlabels += result[1]+",";
				}
			}
			finalentitylocatorids = finalentitylocatorids.replaceFirst(",$", "");
			finalentitylocatorlabels = finalentitylocatorlabels.replaceFirst(",$", "");
		}
	

		if(this.debug){
			System.out.println("entity IDs:["+finalentitylabel+"/"+finalentityid+"]["+finalentitylocatorlabels+"/"+finalentitylocatorids+"]");
		}
		updateEntityIDs(srcid, finalentitylabel, finalentityid, finalentitylocatorlabels, finalentitylocatorids);
	}

	
	/**
	 * like array join function in Perl
	 * @param entitylabeltokens
	 * @param i
	 * @param size
	 * @param string
	 * @return
	 */
	private String join(String[] tokens, int start, int end,
			String delimiter) {
		String result = "";
		for(int i = start; i <=end; i++) result += tokens[i]+delimiter;
		return result.replaceFirst(delimiter+"$", "");
	}


	private void updateEntityIDs(String srcid, String entitylabel,
			String entityid, String entitylocatorlabels,
			String entitylocatorids) {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set entitylabel='"+entitylabel+"'," +
					" entityid='"+entityid+"', entitylocatorlabel='"+entitylocatorlabels+"'," +
					" entitylocatorid='"+entitylocatorids+"' where id="+srcid);
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}


	/**
	 * preopercular latero-sensory canal =>	preopercular sensory canal
	 * pectoral-fin spine => pectoral fin spine
	 * @param term
	 * @return
	 */
	private String[] searchTerm(String term, String type){	
		//search in cache
		if(type.compareTo("entity")==0){
			String[] result = this.entityIDCache.get(term);
			if(result!=null) return result;
		}
		if(type.compareTo("quality")==0){
			String[] result = this.qualityIDCache.get(term);
			if(result!=null) return result;
		}
		
		//search ontologies
		if(term.indexOf("-")>0){
		    //first search the original word
			ArrayList<String[]> results = Utilities.searchOntologies(term, type);
			String[] exactmatch = retrieveExactMatch(results, term);
			if(exactmatch!=null){
				if(type.compareTo("entity")==0) this.entityIDCache.put(term, exactmatch);
				if(type.compareTo("quality")==0) this.qualityIDCache.put(term, exactmatch);				
				return exactmatch;
			}
			/*String termcp = term;
			if(results.size()==0){
				//then search the word without -
				while(term.indexOf("-")>0 && results.size()==0){
					term = term.replaceFirst("-", " ");
					results = Utilities.searchOntologies(term, type);
				}
			}
			term = termcp;
			*/
			
			//then search the word after "ccc-" part is removed
			/*while(term.indexOf("-")>0 && results.size()==0){
				term = term.replaceFirst("(?<=(^| ))\\w+-", "");
				results = Utilities.searchOntologies(term, type);
			}*/
						 
		}else{
			ArrayList<String[]> results = Utilities.searchOntologies(term, type);
			String[] exactmatch = retrieveExactMatch(results, term);
			if(exactmatch!=null){
				if(type.compareTo("entity")==0) this.entityIDCache.put(term, exactmatch);
				if(type.compareTo("quality")==0) this.qualityIDCache.put(term, exactmatch);				
				return exactmatch;
			}
		}		
		return null;
	}
	
	/**
	 * 
	 * @param results
	 * @return
	 */
	private String[] retrieveExactMatch(ArrayList<String[]> results, String term) {
		for(String[] result: results){
			if(result[2].compareTo(term)==0){
				return new String[]{result[1], result[2]};
			}
		}
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
	}

}
