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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import conceptmapping.TermOutputerUtilities;


/**
 * @author Hong Updates
 * This class finds IDs from ontologies for Entity and Quality terms
 * and fill in the blank ID columns in the outputtable
 * 
 * list of relational qualities that are symmetrical:
 *ARTICULATED_WITH = "PATO:0002278"
 *ASSOCIATED_WITH = "PATO:0001668"
 *ATTACHED_TO = "PATO:0001667"
 *DETACHED_FROM = "PATO:0001453"
 *DISSOCIATED_FROM = "PATO:0001738"
 *FUSED_WITH = "PATO:0000642"
 *IN_CONTACT_WITH = "PATO:0001961"
 *OVERLAP_WITH = "PATO:0001590"
 *SEPARATED_FROM = "PATO:0001505"
 *UNFUSED_FROM = "PATO:0000651"
 *STRUCTURE = "PATO:0000141"
 * 
 *unsymmetrical ones  are marked as 'relational' in PATO obo file
 * 
 */
@SuppressWarnings("unused")
public class TermEQ2IDEQ {

	//name of the ouput table
	private String outputtable;
	
	private Connection conn;
	private String username="root";
	private String password="root";
	//private TreeSet<String> entityterms = new TreeSet<String>();
	//private TreeSet<String> qualityterms = new TreeSet<String>();
	
	private Hashtable<String, String[]> entityIDCache = new Hashtable<String, String[]>(); //term=> {id, label}
	private Hashtable<String, String[]> qualityIDCache = new Hashtable<String, String[]>();
	
	private ArrayList<String> spatialterms = new ArrayList<String>();
	
	private String prefix;
	private String ontologyfolder;
	private static TermOutputerUtilities ontoutil;
	private String process="crest|ridge|process|tentacule|shelf|flange|ramus";
	private boolean debug = false;
	
	/**
	 * 
	 */
	public TermEQ2IDEQ(String database, String outputtable, String prefix, String ontologyfolder, String csv) throws Exception {
		this.prefix = prefix;
		this.ontologyfolder = ontologyfolder;
		ontoutil = new TermOutputerUtilities(ontologyfolder, database);
		this.outputtable = outputtable+"_result";
		//put process in the cache?
		this.entityIDCache.put("process", new String[]{"entity", "VAO:0000180", "process"});
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct term from uniquespatialterms");
				while(rs.next()){
					spatialterms.add(rs.getString("term"));
				}			
				spatialterms.add("accessory");
				stmt.execute("drop table if exists "+this.outputtable);
				stmt.execute("create table "+this.outputtable+" select * from "+outputtable);
				rs = stmt.executeQuery("select source,id, entitylabel, entitylocatorlabel, quality, qualitynegated, qualitymodifierlabel from "+this.outputtable+" where stateid!=''");
				while(rs.next()){
					String src = rs.getString("source");
					String srcid = rs.getString("id");
					String entitylabel = rs.getString("entitylabel");
					String entitylocatorlabel = rs.getString("entitylocatorlabel");
					String quality = rs.getString("quality");
					String qualitynegated = rs.getString("qualitynegated");
					String qualitymodifierlabel = rs.getString("qualitymodifierlabel");
					System.out.println(src);
					entitylabel = knowntransformation(entitylabel);
					entitylocatorlabel= knowntransformation(entitylocatorlabel);
					qualitymodifierlabel = knowntransformation(qualitymodifierlabel);
					fillInIDs(srcid, entitylabel, entitylocatorlabel, quality, qualitynegated, qualitymodifierlabel);
				}
				//export the table to a csv file
				File csvfile = new File(csv);
				if(csvfile.exists()) csvfile.delete();
				String fieldlist ="source, characterid, stateid, description, entity, entitylabel, entityid, " +
						"quality, qualitylabel, qualityid, qualitynegated, qualitynegatedlabel, qnparentlabel, qnparentid, " +
						"qualitymodifier, qualitymodifierlabel, qualitymodifierid, " +
						"entitylocator, entitylocatorlabel, entitylocatorid, countt";
				String header = "'"+fieldlist.replaceAll("\\s*,\\s*", "','")+"'";
				String export = "SELECT * "+
						" INTO OUTFILE '"+csv+"' FIELDS TERMINATED BY ',' "+
						" OPTIONALLY ENCLOSED BY '\\\"' "+
						" ESCAPED BY '\\\\' LINES TERMINATED BY '\\n' FROM ("+
						" SELECT "+header+ " UNION SELECT "+fieldlist+ " From "+ this.outputtable+") a";	
				stmt.execute(export);
			}			
	}
	
	private String knowntransformation(String entitylabel) {
		entitylabel = entitylabel.replaceAll("latero-sensory", "sensory");
		entitylabel = entitylabel.replaceAll("laterosensory", "sensory");
		return entitylabel;
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
	public void fillInIDs(String srcid, String entitylabel, String entitylocatorlabel, String quality, String qualitynegated, String qualitymodifierlabel) throws Exception{
		//deal with special case first: between
		boolean completed = fillInIDs4ContactBetween(srcid, entitylabel, entitylocatorlabel, quality, qualitynegated, qualitymodifierlabel);
		if(completed) return;
		
		fillInIDs4Entity(srcid, entitylabel, entitylocatorlabel); //0: label; 1:id
		fillInIDs4QualityModifier(srcid, qualitymodifierlabel);
		if(quality.length()>0){//rounded dorsally
			fillInIDs4Quality(srcid, quality);
		}else if(qualitynegated.length()>0){
			fillInIDs4Qualitynegated(srcid, qualitynegated);
		}
	}
	
	/**
	 * update qualitymodifierlabel and qualitymodifierid
	 * @param srcid
	 * @param qualitymodifierlabel
	 */
	private void fillInIDs4QualityModifier(String srcid,
			String qualitymodifierlabel) throws Exception{
		String finalqualitymodifierids = "";
		String finalqualitymodifierlabels = "";		
		if(qualitymodifierlabel.length()>0){
			String[] qualitymodifiers = qualitymodifierlabel.split("\\s*,\\s*");
			for(String qm: qualitymodifiers){
				/*String [] result = searchTerm(fel, "entity");
				if(result!=null){
					finalentitylocatorids += result[1]+",";
					finalentitylocatorlabels += result[2]+",";
				}*/
				String [] finalresult = searchEntity(qm, "", false, qm);
				if(finalresult!=null){
					finalqualitymodifierids += finalresult[0]+",";
					finalqualitymodifierlabels += finalresult[1]+",";
				}else{//no result, insert empty string as result 
					finalqualitymodifierids += ",";
					finalqualitymodifierlabels += ",";
				}
			}
			finalqualitymodifierids = finalqualitymodifierids.replaceFirst(",$", "");
			finalqualitymodifierlabels = finalqualitymodifierlabels.replaceFirst(",$", "");
		}
		updateQualityModifierIDs(srcid, finalqualitymodifierlabels, finalqualitymodifierids);
	}

	private void updateQualityModifierIDs(String srcid, String qualitymodifierlabels,
			String qualitymodifierids) throws Exception {
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set qualitymodifierlabel='"+qualitymodifierlabels+"'," +
					" qualitymodifierid='"+qualitymodifierids+"' where id="+srcid);

		
	}

	/**
	 * update qualitynegatedlabel, qualityid, qnparentlabel, qnparentid
	 * @param srcid
	 * @param qualitynegated
	 */
	private void fillInIDs4Qualitynegated(String srcid, String qualitynegated) throws Exception{
		String term = qualitynegated.replaceFirst("not ", "").trim();
		String qualitynegatedlabel="";
		String qualityid="";
		String qnparentlabels="";
		String qnparentids="";
		String[] result = this.searchTerm(term, "quality");//3-element array: 0:type; 1:id; 2:label
		if(result!=null){
			qualitynegatedlabel = "not("+result[2]+")";
			qualityid = result[1];
			String [] parentinfo = ontoutil.retreiveParentInfoFromPATO(result[2]);
			if(parentinfo != null){
				qnparentids = parentinfo[0];
				qnparentlabels = parentinfo[1];
			}			
		}
		updateQualitynegatedIDs(srcid, qualitynegatedlabel, qualityid, qnparentlabels, qnparentids);
		
	}

	private void updateQualitynegatedIDs(String srcid,
			String qualitynegatedlabel, String qualityid,
			String qnparentlabels, String qnparentids) throws Exception{

			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set qualitynegatedlabel='"+qualitynegatedlabel+"'," +
					" qualityid='"+qualityid+"', qnparentlabel='"+qnparentlabels+"'," +
					" qnparentid='"+qnparentids+"' where id="+srcid);

	}


	/**
	 * update qualitylabel, qualityid
	 * @param srcid
	 * @param quality
	 */
	private void fillInIDs4Quality(String srcid, String term) throws Exception {
		term = term.replaceAll("\\[.*?\\]", "").trim();
		//if(this.debug){
		//	System.out.println("quality term:["+term+"]");
		//}
		//heuristics: numerical
		if(term.startsWith("present ")){//present regardless of ...
			term = "present";
		}else if(term.matches(".*?\\d+.*")){
			updateCount(srcid, term);
			if(term.indexOf("%")>0 && term.matches(".*?\\d\\.\\d.*")){//80% or 1.2
				term = "size";
			}else if(term.indexOf("�")>0 ){
				term = "orientation";
			}else{
				term = "count";
			}
		}else if(term.matches("zero")){
			updateCount(srcid, term);
			term = "count";
		}else if(term.matches("^\\w+er\\b.*") || term.endsWith("^\\w+est\\b.*")){//heuristics: comparative/superlative adjective
			term = term.replaceFirst(" .*", "");
			term = checkWN4base(term);
		}else if(term.matches(".*?\\b(width|wide|widen|long|length|\\w+-sized)\\b.*")){
			term = "size";
		}else if(term.matches(".*?equal.*")){
			term = "size";
		}		
		String[] result = this.searchTerm(term, "quality");
		if(result!=null){
			//if(this.debug){
			//	System.out.println("qulity label:["+result[2]+"/"+result[1]+"]");
			//}
			updateQualityIDs(srcid, result[2], result[1]);
		}else{
			if(this.debug){
				System.out.println("quality term:["+term+"]");
				System.out.println("qulity label:[/]");
			}
		}
	}
	
	private void updateCount(String srcid, String term) throws Exception{

			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set countt='"+term+"' where id="+srcid);
			
	}
	

	private void updateQualityIDs(String srcid, String qualitylabel, String qualityid) throws Exception{

			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set qualitylabel='"+qualitylabel+"'," +
					" qualityid='"+qualityid+"' where id="+srcid);
				
	}


	/**
	 * search entities
	 * then lookup IDs for other terms in entitylocators                                  	
	 * update entitylabel, entityid, entitylocatorlabel, entitylocatorid using srcid
	 * @param srcid
	 * @param entitylabel
	 * @param entitylocatorlabel
	 */
	
	
	
	
	private void fillInIDs4Entity(String srcid, String entitylabel,
			String entitylocatorlabel) throws Exception{
		//if(this.debug){
		//	System.out.println("entity terms:["+entitylabel+"]["+entitylocatorlabel+"]");
		//}
		//find ID for entity
		String finalentitylocator = "";
		String finalentitylabel = "";
		String finalentityid = "";
		String[] finals = searchEntity(entitylabel, entitylocatorlabel, true, entitylabel);
		finalentityid = finals[0];
		finalentitylabel = finals[1];
		finalentitylocator = finals[2];
		
		//find IDs for entitylocator
		String[] finalentitylocatorresult = new String[2];
		searchEntityLocator(finalentitylocator, finalentitylocatorresult);	
		String finalentitylocatorids = finalentitylocatorresult[0]; 
		String finalentitylocatorlabels = finalentitylocatorresult[1]; 
		//if(this.debug){
		//	System.out.println("entity IDs:["+finalentitylabel+"/"+finalentityid+"]["+finalentitylocatorlabels+"/"+finalentitylocatorids+"]");
		//}
		updateEntityIDs(srcid, finalentitylabel, finalentityid, finalentitylocatorlabels, finalentitylocatorids);
	}

	
	/**
	 * this deals with a transformation of the pattern similar to this: 
	 * original: contact between A and B / present / absent
	 * results: A in contact with B/A separated from B
	 * 
	 * very prelimary/specific at this time
	 * @param srcid
	 * @param entitylabel
	 * @param entitylocatorlabel
	 * @return
	 * @throws Exception 
	 */
	private boolean fillInIDs4ContactBetween(String srcid, String entitylabel,
			String entitylocatorlabel, String quality, String qualitynegated, String qualitymodifierlabel) throws Exception {
		boolean complete = false;
		if(entitylabel.matches("(contact|connection)") && entitylocatorlabel.matches("between .*") && quality.matches("(absent|present).*")){
			//new entitylabel, qualitymodifierlabel, entitylocator, quality
			String[] els = entitylocatorlabel.split("\\s*,\\s*");
			entitylabel = els[0].replaceFirst("between ", "");
			qualitymodifierlabel = els[1] +","+qualitymodifierlabel;
			qualitymodifierlabel = qualitymodifierlabel.trim().replaceFirst(",$", "");
			if(els.length-1 > 2) entitylocatorlabel = join(els, 2, els.length-1, " ");
			else entitylocatorlabel = "";
		
			if(quality.equals("present")) quality = "in contact with";
			else if(quality.equals("absent")) quality = "separated from";
			
			//search for IDs: entity
			String[] finals = searchEntity(entitylabel, entitylocatorlabel, true, entitylabel);
			String finalentityid = finals[0];
			String finalentitylabel = finals[1];
			String finalentitylocator = finals[2];
			
			if(finalentityid.length()>0){
				//search for IDs: entitylocator
				String[] entitylocatorinfo = new String[2];
				String finalentitylocatorids = "";
				String finalentitylocatorlabels = "";
				if(finalentitylocator.length()>0){
					searchEntityLocator(finalentitylocator, entitylocatorinfo);
					finalentitylocatorids = entitylocatorinfo[0];
					finalentitylocatorlabels = entitylocatorinfo[1];
				}
				
				//search for IDs: quality
				String[] result = this.searchTerm(quality, "quality");
				String finalqualitylabel = result[2];
				String finalqualityid = result[1];
				
				//search for IDs: qualitymodifierlabel
				String[] qualitymodifierinfo = new String[2];
				String finalqualitymodifierids = "";
				String finalqualitymodifierlabels = "";
				if(qualitymodifierlabel.length()>0){
					searchEntityLocator(qualitymodifierlabel, qualitymodifierinfo);
					finalqualitymodifierids = qualitymodifierinfo[0];
					finalqualitymodifierlabels = qualitymodifierinfo[1];
				}				
				updateEQIDs(srcid, finalentitylabel, finalentityid, finalqualitylabel, finalqualityid, finalqualitymodifierlabels, finalqualitymodifierids, finalentitylocatorlabels, finalentitylocatorids);
				complete = true;
			}
		}
		return complete;
	}

	private void updateEQIDs(String srcid, String entitylabel,
			String entityid, String qualitylabel, String qualityid, String qualitymodifierlabels, String qualitymodifierids, String entitylocatorlabels,
			String entitylocatorids) throws Exception{
		
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set entitylabel='"+entitylabel+"', " +
					" entityid='"+entityid+"', qualitylabel ='"+qualitylabel+"', qualityid='"+qualityid+"', qualitymodifierlabel='"+
					qualitymodifierlabels+"', qualitymodifierid='"+qualitymodifierids+"', entitylocatorlabel='"+entitylocatorlabels+"'," +
					" entitylocatorid='"+entitylocatorids+"' where id="+srcid);
	}

	private void searchEntityLocator(String finalentitylocator,
			String[] finalentitylocatorresult) throws Exception {
		String finalentitylocatorids = "";
		String finalentitylocatorlabels="";
		if(finalentitylocator.length()>0){
			String[] finalentitylocators = finalentitylocator.split("\\s*,\\s*");
			for(String fel: finalentitylocators){
				/*String [] result = searchTerm(fel, "entity");
				if(result!=null){
					finalentitylocatorids += result[1]+",";
					finalentitylocatorlabels += result[2]+",";
				}*/
				String [] finalresult = searchEntity(fel, "", false, fel);
				if(finalresult!=null){
					finalentitylocatorids += finalresult[0]+",";
					finalentitylocatorlabels += finalresult[1]+",";
				}else{//no result, insert empty string as result 
					finalentitylocatorids += ",";
					finalentitylocatorlabels += ",";
				}
			}
			finalentitylocatorids = finalentitylocatorids.replaceFirst(",$", "");
			finalentitylocatorlabels = finalentitylocatorlabels.replaceFirst(",$", "");
		}
		finalentitylocatorresult[0] = finalentitylocatorids;
		finalentitylocatorresult[1] = finalentitylocatorlabels;
	}

	
	/**
	 * 	 * Search a phrase A B C
	 * search A B C
	 * if succeeds, search the parent entity locator + A B C [tooth => ceratobranchial 5 tooth]
	 *             if succeeds, entitylabel = p.e.l + A B C, entitylocator = entitylocator - p.e.l
	 *             if fails, entitylabel = A B C, entitylocator = entitylocator
	 * if fails, search B C
	 *             if succeeds, entitylabel = B C, entitylocator = (entitylabel - B C), entitylocator
	 *             if fails, search C
	 *             			 if succeeds, entitylabel = C, entitylocator = (entitylabel - C), entitylocator
	 *             			 if fails, search the parent entity locator
	 *                                 if succeeds, entitylable = p.e.l*, entitylocator = entitylocator - p.e.l
	 *                                 if fails, search the next parent entity locator
	 *                                 ....
	 * @param entitylabel
	 * @param entitylocatorlabel
	 * @param entityWithLocator
	 * @return
	 */
	private String[] searchEntity(String entitylabel, String entitylocatorlabel, boolean entityWithLocator, String originalentitylabel) throws Exception{
		//System.out.println("search entity: "+entitylabel);
		String [] finals = new String[3];
		finals[2] = entitylocatorlabel;
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
				if(entitylocators != null && i==0 && entityWithLocator){//has entitylocator
					String [] newresult = searchTerm(entitylocators[0]+" "+entityterm, "entity");
					if(newresult!=null){
						finalentityid = newresult[1];
						finalentitylabel = newresult[2];
						finalentitylocator = entitylocatorlabel.replaceFirst(entitylocators[0], "").replaceAll("^\\s*,\\s*", "");
						break;
					}
				}
				finalentityid = result[1];
				finalentitylabel = result[2];
				String left = entitylabel.replaceFirst(entityterm, "").trim();//e.g. ventral [process]
				if(!this.spatialterms.contains(left)){
					finalentitylocator = left+","+entitylocatorlabel.trim();
				}else{
					finalentitylocator = entitylocatorlabel;
				}
				finalentitylocator = finalentitylocator.replaceFirst(",$", "").replaceFirst("^,", "").trim();
				break;			
			}else{
				if(i == size && entitylocators!= null){//entitylabel returned no result, try entitylocators
					int j = 0;
					while(result==null && j<entitylocators.length){
						result = searchTerm(entitylocators[j], "entity");
						j++;
					}
					if(result!=null){
						finalentityid = result[1];
						finalentitylabel = result[2];
						finalentitylocator = join(entitylocators, j, entitylocators.length-1, ",");
						break;
					}
				}
			}				
		}		

		//having gone through all of the above, still hasn't find a good entityid
		//deal with spatial expressions here
		if(finalentityid.length()==0){
			String tokens[] = entitylabel.split("\\s+");
			if(tokens.length==2 && this.spatialterms.contains(tokens[0])){
				tokens[1] = "region";
				entitylabel = this.join(tokens, 0, 1, " ");
				String[] result = searchTerm(entitylabel, "entity");
				if(result!=null){
					finalentityid = result[1];
					finalentitylabel = result[2];
				}
			}
		}
		finals[0] = finalentityid;
		finals[1] = finalentitylabel;
		finals[2] = finalentitylocator;

		//bone, cartilage, and element
		if(finalentityid.length()==0 && entitylabel.equals(originalentitylabel)){
			entitylabel = entitylabel+" bone";
			String[] result = searchTerm(entitylabel, "entity");
			if(result!=null){
				finalentityid = result[1];
				finalentitylabel = result[2];
				finals[0] = finalentityid;
				finals[1] = finalentitylabel;
				finals[2] = finalentitylocator;
				return finals;
			}
		}			

		//last rule
		//still not find a match, remove the last term in the entitylabel 
		//"humeral deltopectoral crest apex" => "humeral deltopectoral crest"
		if(finalentityid.length()==0){
			String[] tokens = entitylabel.split("\\s+");
			if(tokens.length>=2){
				String last2 = "<"+join(tokens, tokens.length-2, tokens.length-1, " ").replace(" ", "> <")+">";
				if(valid(last2)){
					String shortened = entitylabel.substring(0, entitylabel.lastIndexOf(" ")).trim();
					return searchEntity(shortened, entitylocatorlabel, entityWithLocator, entitylabel);
				}
			}
		}
		return finals;
	}

	/**
	 * 
	 * @param two word structure phrase marked with <>, like <process> <apex>
	 * @return
	 */
	private boolean valid(String organphrase) throws Exception{

			String text = organphrase.replaceAll("[<>]", "");
			boolean flag1= false;
			boolean flag2 = false;
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select count(*) from "+prefix+"_markedsentence where markedsent like '%"+organphrase+"%'");
			if(rs.next()) flag1=true;
			rs = stmt.executeQuery("select count(*) from "+prefix+"_sentence where sentence like '%"+text+"%'");
			if(rs.next()) flag2=true;
			return flag1&&flag2;

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
			String entitylocatorids) throws Exception{
		
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set entitylabel='"+entitylabel+"'," +
					" entityid='"+entityid+"', entitylocatorlabel='"+entitylocatorlabels+"'," +
					" entitylocatorid='"+entitylocatorids+"' where id="+srcid);

	}


	/**
	 * preopercular latero-sensory canal =>	preopercular sensory canal
	 * pectoral-fin spine => pectoral fin spine
	 * @param term
	 * @return 3-element array: type, id, label
	 */
	private String[] searchTerm(String term, String type) throws Exception{	
		if(term.trim().length()==0) return null;
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

		//first search the original word
		ArrayList<String[]> results = ontoutil.searchOntologies(term, type);
		String[] exactmatch = null;
		if(results.size()>0) exactmatch = results.get(0);
		if(exactmatch!=null){
			if(type.compareTo("entity")==0) this.entityIDCache.put(term, exactmatch);
			if(type.compareTo("quality")==0) this.qualityIDCache.put(term, exactmatch);				
			return exactmatch;
		}

		//if landed here, the first try has failed				 
		if(term.indexOf("-")>0){ //caudal-fin
			term = term.replaceAll("-", " ");
			results = ontoutil.searchOntologies(term, type);
			exactmatch = null;
			if(results.size()>0) exactmatch = results.get(0);
			if(exactmatch!=null){
				if(type.compareTo("entity")==0) this.entityIDCache.put(term, exactmatch);
				if(type.compareTo("quality")==0) this.qualityIDCache.put(term, exactmatch);				
				return exactmatch;
			}
		}		
		
		//if landed here, the 2nd try was not successful either
		if(term.indexOf("/")>0){ //bone/tendon
			String[] tokens = term.split("/");
			for(String token: tokens){
				results = ontoutil.searchOntologies(token, type);
				exactmatch = null;
				if(results.size()>0) exactmatch = results.get(0);
				if(exactmatch!=null){
					if(type.compareTo("entity")==0) this.entityIDCache.put(term, exactmatch);
					if(type.compareTo("quality")==0) this.qualityIDCache.put(term, exactmatch);				
					return exactmatch;
				}
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
	private void insertQualityResults2Table(String qualityterm, ArrayList<String[]>  results) throws Exception {
			String id = results.get(0)[1]+":"+results.get(0)[2];
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set qualityid='"+id+"' where quality='"+qualityterm+"'");
	}

	/**
	 * 
	 * @param entityterm
	 * @param results: a two dimensional array holding multiple mappings, 
	 * each mapping contains 3 elements: type, id, and label
	 */
	private void insertEntityResults2Table(String entityterm, ArrayList<String[]> results) throws Exception{
		
			String id = results.get(0)[1]+":"+results.get(0)[2];
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.outputtable+" set entityid='"+id+"' where entity='"+entityterm+"'");
			stmt.execute("update "+this.outputtable+" set entitylocatorid='"+id+"' where entitylocator='"+entityterm+"'");
			stmt.execute("update "+this.outputtable+" set qualitymodifierid='"+id+"' where qualitymodifier='"+entityterm+"'");			
	}

	public static String checkWN4base(String word) throws Exception{
		
		String result = checkWN("wn "+word+" -over");
		if (result.length()==0){//word not in WN
			return word;
		}
		//found word in WN:
		String t = "";
		Pattern p = Pattern.compile("(.*?)Overview of adj (\\w+) (.*)");
		Matcher m = p.matcher(result);
		while(m.matches()){
			 t += m.group(2)+" ";
			 result = m.group(3);
			 m = p.matcher(result);
		}
		if (t.length() ==0){//word is not an adj
			return word;
		} 
		String[] ts = t.trim().split("\\s+"); 
		for(int i = 0; i<ts.length; i++){
			if(ts[i].compareTo(word)!=0){//find the base
				return ts[i];
			}
		}
		return word;
	}
	 
	public static String checkWN(String cmdtext) throws Exception{
		
 	  		Runtime r = Runtime.getRuntime();	
	  		Process proc = r.exec(cmdtext);
		    ArrayList<String> errors = new ArrayList<String>();
	  	    ArrayList<String> outputs = new ArrayList<String>();
	  
            // any error message?
            //StreamGobbler errorGobbler = new 
                //StreamGobblerWordNet(proc.getErrorStream(), "ERROR", errors, outputs);            
            
            // any output?
            StreamGobbler outputGobbler = new 
                StreamGobblerWordNet(proc.getInputStream(), "OUTPUT", errors, outputs);
                
            // kick them off
            //errorGobbler.start();
            outputGobbler.start();
                                    
            // any error???
            int exitVal = proc.waitFor();
            //System.out.println("ExitValue: " + exitVal);

            StringBuffer sb = new StringBuffer();
            for(int i = 0; i<outputs.size(); i++){
            	//sb.append(errors.get(i)+" ");
            	sb.append(outputs.get(i)+" ");
            }
            return sb.toString();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try{
			//String csv = "C:/Documents and Settings/Hong Updates/Desktop/Australia/phenoscape-fish-source/target/trash_EQ.csv";
			//String csv = "C:/Users/Zilong Chang/Documents/WORK/getestNew/target/aftersereno.csv";
			//TermEQ2IDEQ t2id = new TermEQ2IDEQ("biocreative2012", "xml2eq", "gstestnew", "C:\\Users\\Zilong Chang\\Documents\\WORK\\getestNew\\ontologies",csv);
			
			//TermEQ2IDEQ(String database, String outputtable, String prefix, String ontologyfolder, String csv) throws Exception {
			String csv = "C:\\Documents and Settings\\Hong Updates\\Desktop\\ATEST\\output.csv";
			String ontologies = "C:\\Documents and Settings\\Hong Updates\\Desktop\\ATEST\\ontologies";
			TermEQ2IDEQ t2id = new TermEQ2IDEQ("biocreative2012", "test4_xml2eq", "test4", ontologies, csv);
		
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
