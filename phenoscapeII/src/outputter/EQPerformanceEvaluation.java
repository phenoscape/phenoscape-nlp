/**
 * 
 */
package outputter;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;

/**
 * @author Hong Updates
 * compare raw EQ table to EQ answer table
 * obtain precision/recall measurements 
 */
public class EQPerformanceEvaluation {

	private Connection conn;
	private String username ="root";
	private String password = "root";
	private String testtable;
	private String answertable;
	private String prtablefields;
	private String prtableEQs;
	private String prtableTranslations;
	
	//init
	Hashtable<String, String> counts;
	ArrayList<String> fields = new ArrayList<String>();

	/**
	 * 
	 */
	public EQPerformanceEvaluation(String database, String testtable, String answertable, String prtable) {
		this.testtable = testtable;
		this.answertable = answertable;
		this.prtableEQs = prtable+"_EQs";
		this.prtablefields = prtable+"_fields";
		this.prtableTranslations = prtable+"_translations";
		initFields();
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();

				stmt.execute("create table if not exists "+prtablefields+" (id TIMESTAMP DEFAULT CURRENT_TIMESTAMP primary key, " +
						"entityp float(4,2), entityr float(4,2), " +
						"entitylabelp float(4,2), entitylabelr float(4,2), " +
						"entityidp float(4,2), entityidr float(4,2), " +
						"qualityp float(4,2), qualityr float(4,2), " +
						"qualitylabelp float(4,2), qualitylabelr float(4,2), " +
						"qualityidp float(4,2), qualityidr float(4,2), " +
						"qualitynegatedp float(4,2), qualitynegatedr float(4,2), " +
						"qualitynegatedlabelp float(4,2), qualitynegatedlabelr float(4,2), " +
						"qnparentlabelp float(4,2), qnparentlabelr float(4,2), " +
						"qnparentidp float(4,2), qnparentidr float(4,2), " +
						"qualitymodifierp float(4,2), qualitymodifierr float(4,2), " +
						"qualitymodifierlabelp float(4,2), qualitymodifierlabelr float(4,2), " +
						"qualitymodifieridp float(4,2), qualitymodifieridr float(4,2), " +
						"entitylocatorp float(4,2), entitylocatorr float(4,2), " +
						"entitylocatorlabelp float(4,2), entitylocatorlabelr float(4,2), " +
						"entitylocatoridp float(4,2), entitylocatoridr float(4,2) " +
						"countp float(4,2), countr float(4,2) " +
						")");
				
				stmt.execute("create table if not exists "+prtableEQs+" (id TIMESTAMP DEFAULT CURRENT_TIMESTAMP primary key, " +
						"rawexactp float(4,2), rawexactr float(4,2), " +
						"rawpartialp float(4,2), rawpartialr float(4,2), " +
						"formalexactp float(4,2), formalexactr float(4,2), " +
						"formalpartialp float(4,2), formalpartialr float(4,2), "+
						")");

				stmt.execute("create table if not exists "+this.prtableTranslations+" (id TIMESTAMP DEFAULT CURRENT_TIMESTAMP primary key, " +
						"entityp float(4,2), entityr float(4,2), " +
						"qualityp float(4,2),qualityr float(4,2), " +
						")");
				}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void initFields() {
		this.fields.add("entity");
		this.fields.add("entitylabel");
		this.fields.add("entityid");
		this.fields.add("quality");
		this.fields.add("qualitylabel");
		this.fields.add("qualityid");
		this.fields.add("qualitynegated");
		this.fields.add("qualitynegatedlabel");
		this.fields.add("qnparentlabel");
		this.fields.add("qnparentid");
		this.fields.add("qualitymodifier");
		this.fields.add("qualitymodifierlabel");
		this.fields.add("qualitymodifierid");
		this.fields.add("entitylocator");
		this.fields.add("entitylocatorlabel");
		this.fields.add("entitylocatorid");
		this.fields.add("count");		
	}

	/**
	 * get precision and recall measurements
	 * precision = #matched/#generated
	 * recall = #matched/#inanswer
	 */
	public void evaluation(){	
		ArrayList<String> states = new ArrayList<String>(); 
		//tallying 
		try{
			//collect all state ids
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select characterid, stateid from "+this.answertable);
			while(rs.next()){
				String characterid = rs.getString("characterid");
				String stateid = rs.getString("stateid");
				if(characterid.length() > 0 && stateid.length() > 0){
					states.add(stateid);
				}
			}
			//pair up answer and test states
			ArrayList<ArrayList<Hashtable<String,String>>> astates = new ArrayList<ArrayList<Hashtable<String,String>>>();
			ArrayList<ArrayList<Hashtable<String,String>>> tstates = new ArrayList<ArrayList<Hashtable<String,String>>>();			
			Iterator<String> it = states.iterator();
			while(it.hasNext()){
				String stateid = it.next();
				ArrayList<Hashtable<String, String>> astate = new ArrayList<Hashtable<String, String>>();
				rs = stmt.executeQuery("select entity, entitylabel, entityid, quality, qualitylabel, qualityid, quality_negated, quality_negatedlabel, qn_parentlabel, qn_parentid, qualitymodifier, qualitymodifierlabel, qualitymodifierid, entitylocator, entitylocatorlabel, entitylocatorid, count from "+
					this.answertable+" where stateid = '"+stateid+"'");
				while(rs.next()){
					Hashtable<String, String> EQ = new Hashtable<String, String> ();
					for(String field: this.fields){
						EQ.put(field, rs.getString(field));
					}		
					astate.add(EQ);
				}
				astates.add(astate);
				ArrayList<Hashtable<String, String>> tstate = new ArrayList<Hashtable<String, String>>();
				rs = stmt.executeQuery("select entity, entitylabel, entityid, quality, qualitylabel, qualityid, quality_negated, quality_negatedlabel, qn_parentlabel, qn_parentid, qualitymodifier, qualitymodifierlabel, qualitymodifierid, entitylocator, entitylocatorlabel, entitylocatorid, count from "+
					this.testtable+" where stateid = '"+stateid+"'");
				while(rs.next()){
					Hashtable<String, String> EQ = new Hashtable<String, String> ();
					for(String field: this.fields){
						EQ.put(field, rs.getString(field));
					}
					tstate.add(EQ);
				}
				tstates.add(tstate);
			}
			compareFields(tstates, astates);//precision and recall for each of the fields
			compareEQs(tstates, astates); //for raw/labeled EQ statements
			compareTranslations(); //translation from raw to labels/Ids
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	/****************************************************  Fields  *************************************
	 * 
	 * @param tstate
	 * @param astate
	 */
	private void compareFields(ArrayList<ArrayList<Hashtable<String, String>>> tstates,
			ArrayList<ArrayList<Hashtable<String, String>>> astates) {
		if(counts == null){
			 counts = new Hashtable<String, String> ();
			 //init
			for(String field : this.fields){
					counts.put("inanswer"+field, ""+0);		
					counts.put("generated"+field, ""+0);
					counts.put("matched"+field, ""+0);
			}
			//put totals in
			for(String field : this.fields){
				getTotal(field);
			}			
		}
		
		//collecting matched
		for(String field : this.fields){	
			ArrayList<String> avalues = new ArrayList<String>();
			for(ArrayList<Hashtable<String, String>> astate: astates){
				for(Hashtable<String, String> EQ :astate){
					String v = EQ.get(field);
					if(v!=null && v.length()>0){
						String[] vs = v.split("\\s*,\\s*");
						for(String v1 : vs){
							avalues.add(v1);
						}
					}
				}
			}
			
			ArrayList<String> tvalues = new ArrayList<String>();
			for(ArrayList<Hashtable<String, String>> tstate: tstates){
				for(Hashtable<String, String> EQ :tstate){
					String v = EQ.get(field);
					if(v!=null && v.length()>0){
						String[] vs = v.split("\\s*,\\s*");
						for(String v1 : vs){
							tvalues.add(v1);
						}
					}
				}
			}
			
			
			for(String v : tvalues){
				if(avalues.contains(v)){
					counts.put("matched"+field, ""+(Integer.parseInt(counts.get("matched"+field))+1));
				}
			}
		}

		//calculate and output P/R measurements
		String prstring = ""; 
		for(String field : this.fields){
			prstring += (float)Integer.parseInt(counts.get("matched"+field))/Integer.parseInt(counts.get("generated"+field))+","+(float)Integer.parseInt(counts.get("matched"+field))/Integer.parseInt(counts.get("inanswer"+field))+",";
		}
		prstring = prstring.replaceFirst(",$", "");
		insertInto(this.prtablefields, prstring);
	}

	/**
	 * 
	 * @param prtablefields2
	 */
	private void insertInto(String tablename, String prstring) {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+tablename+" values ("+prstring+")");
		}catch(Exception e){
			e.printStackTrace();
		}		
	}

	/**
	 * 
	 * @param field
	 */
	private void getTotal(String field) {
		try{
			Statement stmt = conn.createStatement();
			//total for answers
			ResultSet rs = stmt.executeQuery("select "+field+" from "+this.answertable+" where "+field+" is not null and length("+field+")>0");
			while(rs.next()){
				String[] tokens = rs.getString(1).split(",");
				int count = Integer.parseInt(counts.get("inanswer"+field));
				counts.put("inanswer"+field, ""+(count+tokens.length));
			}
			
			//total for answers
			rs = stmt.executeQuery("select "+field+" from "+this.testtable+" where "+field+" is not null and length("+field+")>0");
			while(rs.next()){
				String[] tokens = rs.getString(1).split(",");
				int count = Integer.parseInt(counts.get("inanswer"+field));
				counts.put("generated"+field, ""+(count+tokens.length));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/****************************************************  EQs  *************************************
	 * compare EQs to answer keys to get matched counts
	 * 1. first sort out attachment relations, components with such relation must match as one unit
	 *   E-Q, E-Elabel, Q-Qlabel, E-EL, EL-ELlabel, QM-QMlabel, E-QN, QN-QNlabel, QN-QNparentlabel
	 * 
	 * @param tstate: EQs generated by the algorithm for a state
	 * @param astate: EQs in answer key for a state
	 */
	private void compareEQs(ArrayList<ArrayList<Hashtable<String, String>>> tstates,
			ArrayList<ArrayList<Hashtable<String, String>>> astates) {
		//raw
		int totalrawgenerated = 0;
		int totalrawinanswer = 0;
		int partialrawmatches = 0;
		int exactrawmatches = 0;
		String prstring = "";
		for(int i = 0; i<astates.size(); i++){
			totalrawinanswer += astates.get(i).size();
			totalrawgenerated += tstates.get(i).size();
			for(Hashtable<String, String> tEQ : tstates.get(i)){
				String entity = tEQ.get("entity");
				String entitylocator = tEQ.get("entitylocator");
				String qualitymodifier = tEQ.get("qualitymodifier");
				String quality = tEQ.get("quality");
				if(quality==null || quality.length()==0){
					quality = tEQ.get("qualitynegated");
				}
				int[] matchingresult = matchAstates(entity, entitylocator, quality, qualitymodifier, astates.get(i), "");
				exactrawmatches += matchingresult[0];
				partialrawmatches += matchingresult[0] + matchingresult[1];
			}
		}
		
		prstring += (float)exactrawmatches/totalrawgenerated +","+(float)exactrawmatches/totalrawinanswer +"," +
				(float)partialrawmatches/totalrawgenerated +","+(float)partialrawmatches/totalrawinanswer +",";
		
		//labeled
		int totallabeledgenerated = 0;
		int totallabeledinanswer = 0;
		int partiallabeledmatches = 0;
		int exactlabeledmatches = 0;

		for(int i = 0; i<astates.size(); i++){
			totallabeledinanswer += astates.get(i).size();
			totallabeledgenerated += tstates.get(i).size();
			for(Hashtable<String, String> tEQ : tstates.get(i)){
				String entity = tEQ.get("entitylabel");
				String entitylocator = tEQ.get("entitylocatorlabel");
				String qualitymodifier = tEQ.get("qualitymodifierlabel");
				String quality = tEQ.get("qualitylabel");
				if(quality==null || quality.length()==0){
					quality = tEQ.get("qualitynegatedlabel");
				}
				int[] matchingresult = matchAstates(entity, entitylocator, quality, qualitymodifier, astates.get(i), "label");
				exactlabeledmatches += matchingresult[0];
				partiallabeledmatches += matchingresult[0] + matchingresult[1];
			}
		}		
		prstring += (float)exactlabeledmatches/totallabeledgenerated +","+(float)exactlabeledmatches/totallabeledinanswer +"," +
				(float)partiallabeledmatches/totallabeledgenerated +","+(float)partiallabeledmatches/totallabeledinanswer;
		this.insertInto(this.prtableEQs, prstring);
	}
	
	
	
	/**
	 * match the set of 4 values to EQs in aState
	 * the match EQ is removed from aState
	 * @param entity
	 * @param entitylocator
	 * @param quality
	 * @param qualitymodifier
	 * @param aState
	 * @param postfix
	 * @return 2-element int array: the first element is 1 (0) if there is (not) an exact match, the second element is 1 (0) if there is (not) an partial match
	 */
	private int[] matchAstates(String entity, String entitylocator,
			String quality, String qualitymodifier,
			ArrayList<Hashtable<String, String>> aState, String suffix) {
		int[] matches = new int[]{0, 0};
		int matchsize = 0;
		int index = -1;
		for(int i = 0; i < aState.size(); i++){
			int matchedfields = matchInState(entity, entitylocator, quality, qualitymodifier, aState.get(i), suffix);
			if(matchedfields > matchsize){
				matchsize = matchedfields;
				index = i;
			}
		}
		if(matchsize == 4){
			matches[0] = 1;
			matches[1] = 0;
			aState.remove(index);
			return matches;
		}else if(matchsize >=2 && matchsize < 4){
			matches[0] = 0;
			matches[1] = 1;
			aState.remove(index);
			return matches;
		}else{
			return matches;
		}
	}
	
	
	
	/**
	 * 
	 * @param entity
	 * @param entitylocator
	 * @param quality
	 * @param qualitymodifier
	 * @param EQ
	 * @param suffix
	 * @return 0 if entity and/or quality not match, otherwise, 3 to indicate a partial match, 4 an exact match of all 4 fields 
	 */
	private int matchInState(String entity, String entitylocator,
			String quality, String qualitymodifier,
			Hashtable<String, String> EQ, String suffix) {
		String e = EQ.get("entity"+suffix);
		String q = EQ.get("quality"+suffix);
		int exact = 0;
		int partial = 0;
		if(!e.contains(entity) && !entity.contains(e) && !q.contains(quality) && !quality.contains(q)){
			return 0;
		}

		if(e.compareTo(entity) ==0){
			exact++;
		}else if(e.contains(entity) || entity.contains(e)){
			partial++;
		}
		
		if(q.compareTo(quality)==0){
			exact++;
		}else if(q.contains(quality) || quality.contains(q)){
			partial++;
		}
		
		//compare qualitymodifier
		String[] aqms = EQ.get("qualitymodifier"+suffix).split("\\s*,\\s*");
		String[] tqms = qualitymodifier.split("\\s*,\\s*");
		
		int thisexact = 0;
		int thispartial = 0;
		for(int i = 0; i < aqms.length; i++){
			for(int j = 0; j <tqms.length; j++){
				if(aqms[i].compareTo(tqms[j])==0){
					thisexact++;
				}else if(aqms[i].contains(tqms[j]) && tqms[j].contains(aqms[i])){
					thispartial++;
				}
			}
		}
		if(aqms.length != tqms.length || (thispartial+thisexact > 0 && thisexact != aqms.length)){
			partial++;
		}else if(aqms.length == tqms.length && thisexact == aqms.length){
			exact++; 
		}
		
		//compare entitylocator
		String[] tels = entitylocator.split("\\s*,\\s*");
		String[] aels = EQ.get("entitylocator"+suffix).split("\\s*,\\s*");
		thisexact = 0;
		thispartial = 0;
		for(int i = 0; i < aels.length; i++){
			for(int j = 0; j <tels.length; j++){
				if(aels[i].compareTo(tels[j])==0){
					thisexact++;
				}else if(aels[i].contains(tels[j]) && tels[j].contains(aels[i])){
					thispartial++;
				}
			}
		}
		if(aels.length != tels.length || (thispartial+thisexact > 0 && thisexact != aels.length)){
			partial++;
		}else if(aels.length == tels.length && thisexact == aels.length){
			exact++; 
		}

		return exact+partial;
	}

	/*************************************************  translations  *****************************************
	 * measures P/R for entities and qualities translations from raw to label
	 * entities: entity, entitylocator, qualitymodifier
	 * qualities: quality
	 * @param tstate
	 * @param astate
	 */
	private void compareTranslations() {
		//select unique pairs of raw and labeled values from database 
		ArrayList<String> entities = new ArrayList<String>();
		entities.add("entity, entitylabel");
		entities.add("entitylocator, entitylocatorlabel");
		entities.add("qualitymodifier, qualitymodifierlabel");
		
		ArrayList<String> qualities = new ArrayList<String>();
		qualities.add("quality, qualitylabel");
		
		Hashtable<String, String> aentitytranslations = new Hashtable<String, String>();
		Hashtable<String, String> tentitytranslations = new Hashtable<String, String>();
		Hashtable<String, String> aqualitytranslations = new Hashtable<String, String>();
		Hashtable<String, String> tqualitytranslations = new Hashtable<String, String>();
		try{
			//collecting entity pairs
			for(String entitypair : entities){
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct "+entitypair+" from "+this.answertable);
				while(rs.next()){					
					String[] raws = rs.getString(1).split("\\s*,\\s*");
					String[] labels = rs.getString(2).split("\\s*,\\s*");
					for(int i = 0; i<raws.length; i++){
						aentitytranslations.put(raws[i].trim(), labels[i].trim());
					}
				}				
				rs = stmt.executeQuery("select distinct "+entitypair+" from "+this.testtable);
				while(rs.next()){					
					String[] raws = rs.getString(1).split("\\s*,\\s*");
					String[] labels = rs.getString(2).split("\\s*,\\s*");
					for(int i = 0; i<raws.length; i++){
						tentitytranslations.put(raws[i].trim(), labels[i].trim());
					}
				}											
			}
			//collecting quality pairs
			for(String qualitypair : qualities){
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct "+qualitypair+" from "+this.answertable);
				while(rs.next()){					
					String[] raws = rs.getString(1).split("\\s*,\\s*");
					String[] labels = rs.getString(2).split("\\s*,\\s*");
					for(int i = 0; i<raws.length; i++){
						aqualitytranslations.put(raws[i].trim(), labels[i].trim());
					}
				}				
				rs = stmt.executeQuery("select distinct "+qualitypair+" from "+this.testtable);
				while(rs.next()){					
					String[] raws = rs.getString(1).split("\\s*,\\s*");
					String[] labels = rs.getString(2).split("\\s*,\\s*");
					for(int i = 0; i<raws.length; i++){
						tqualitytranslations.put(raws[i].trim(), labels[i].trim());
					}
				}											
			}
			//matching and calculating entity translations
			String prstring ="";
			int totalentitypairgenerated = tentitytranslations.size();
			int totalentitypairinanswer = aentitytranslations.size();
			int matchedentities = 0;
			Enumeration<String> trawentities = tentitytranslations.keys();
			while(trawentities.hasMoreElements()){
				String traw = trawentities.nextElement();
				String tlabel = tentitytranslations.get(traw);
				String alabel = aentitytranslations.get(traw);
				if(alabel !=null && alabel.compareToIgnoreCase(tlabel)==0){
					matchedentities++;
				}
			}
			prstring += (float)matchedentities/totalentitypairgenerated + ","+ (float)matchedentities/totalentitypairinanswer+",";			
			int totalqualitypairgenerated = tqualitytranslations.size();
			int totalqualitypairinanswer = aqualitytranslations.size();
			int matchedqualities = 0;
			Enumeration<String> trawqualities = tqualitytranslations.keys();
			while(trawqualities.hasMoreElements()){
				String traw = trawqualities.nextElement();
				String tlabel = tqualitytranslations.get(traw);
				String alabel = aqualitytranslations.get(traw);
				if(alabel !=null && alabel.compareToIgnoreCase(tlabel)==0){
					matchedqualities++;
				}
			}
			prstring += (float)matchedqualities/totalqualitypairgenerated + ","+ (float)matchedqualities/totalqualitypairinanswer;
			this.insertInto(this.prtableTranslations, prstring);			
		}catch(Exception e){
				e.printStackTrace();
		}				
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		

	}

}
