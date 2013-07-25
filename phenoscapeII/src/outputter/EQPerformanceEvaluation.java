/**
 * 
 */
package outputter;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.File;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;


import org.apache.log4j.Logger;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.semanticweb.owlapi.model.OWLOntologyCreationException;



/**
 * @author Hariharan
 * Used the existing EQPerformance and enhanced it to work with upgraded algorithm
 * 
 */
@SuppressWarnings("unused")
public class EQPerformanceEvaluation {
	private static final Logger LOGGER = Logger.getLogger(EQPerformanceEvaluation.class);   
	private Connection conn;
	private String username ="root";
	private String password = "forda444";
	private String testtable;
	private String answertable;
	private String prtablefields;
	private String prtableEQs;
	private String prtablestates;
	private boolean printfields = false;
	private boolean printEQs = false;
	private boolean printTranslations = false;
	private ArrayList<ArrayList<Hashtable<String,String>>> astates;
	private ArrayList<ArrayList<Hashtable<String,String>>> tstates;
	private Hashtable<String,Hashtable<String,Float>> substringcache;
	private Hashtable<String,Hashtable<String,String>> equivalencecache;

	private ArrayList<String> states = new ArrayList<String>(); 
	private ELKReasoner elkentity,elkquality,elkspatial;
	//init
	Hashtable<String, String> counts;
	ArrayList<String> fields = new ArrayList<String>();
	private boolean nowislabel=false;

	/**
	 * 
	 */
	public EQPerformanceEvaluation(String database, String testtable, String answertable, String prtable) {
		this.testtable = testtable;
		this.answertable = answertable;
		this.prtableEQs = prtable+"_EQs";
		this.prtablefields = prtable+"_fields";
		this.prtablestates = prtable+"_states";
		this.substringcache= new Hashtable<String, Hashtable<String, Float>>();
		this.equivalencecache = new Hashtable<String,Hashtable<String,String>>();
		try {
			this.elkentity = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+"ext.owl"));
			this.elkquality = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+"pato.owl"));
			this.elkspatial = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+"bspo.owl"));
		} catch (OWLOntologyCreationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		initFields();
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				//Holds the precision and recall values of each and every fields
				String sql ="create table if not exists "+prtablefields+" (id TIMESTAMP DEFAULT CURRENT_TIMESTAMP primary key, " +
						"entitylabelp float(4,2), entitylabelr float(4,2), " +
						"entityidp float(4,2), entityidr float(4,2), " +
						"qualitylabelp float(4,2), qualitylabelr float(4,2), " +
						"qualityidp float(4,2), qualityidr float(4,2), " +
						"RelatedEntityLabelp float(4,2), RelatedEntityLabelr float(4,2), " +
						"RelatedEntityidp float(4,2), RelatedEntityidr float(4,2)" +
						")";
				stmt.execute(sql);
				//Holds the table level EQ Precision and recall values
				stmt.execute("create table if not exists "+prtableEQs+" (id TIMESTAMP DEFAULT CURRENT_TIMESTAMP primary key, " +
						"exactp float(4,2), exactr float(4,2)" +
						")");
				
				stmt.execute(sql);
				//Holds the state level EQ Precision and recall values
				System.out.println("create table if not exists "+prtablestates+" (stateid varchar(100) primary key, " +
						"stateprecision float(4,2), staterecall float(4,2)" +
						")");
				stmt.execute("create table if not exists "+prtablestates+" (stateid varchar(100) primary key, " +
						"stateprecision float(4,2), staterecall float(4,2)" +
						")");
				
				

			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}

	private void initFields() {
		//this.fields.add("stateid");
		this.fields.add("entitylabel");
		this.fields.add("entityid");
		this.fields.add("qualitylabel");
		this.fields.add("qualityid");
		this.fields.add("relatedentitylabel");
		this.fields.add("relatedentityid");
	}

	/**
	 * get precision and recall measurements
	 * precision = #matched/#generated
	 * recall = #matched/#inanswer
	 */
	public void evaluate(){	

		//tallying 
		try{
			//collect all unique state ids
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct characterid, stateid from "+this.answertable);
			while(rs.next()){
				String characterid = rs.getString("characterid");
				String stateid = rs.getString("stateid");
				if(characterid.length() > 0 && stateid.length() > 0){
					states.add(stateid);
				}
			}
			stmt.close();

			readResultsfromDatabase();
			compareFields();//precision and recall for each of the fields
			readResultsfromDatabase();
			compareEQs(); //for raw/labeled EQ statements

		}catch(Exception e){
			LOGGER.error("", e);
		}
		this.elkentity.dispose();
		this.elkquality.dispose();
		this.elkspatial.dispose();
	}

	private void readResultsfromDatabase() throws SQLException {
		//System.out.println("inside read results");
		ResultSet rs;
		Statement stmt = conn.createStatement();
		//pair up answer and test states
		astates = new ArrayList<ArrayList<Hashtable<String,String>>>();
		tstates = new ArrayList<ArrayList<Hashtable<String,String>>>();			
		Iterator<String> it = states.iterator();
		while(it.hasNext()){
			String stateid = it.next();
			ArrayList<Hashtable<String, String>> astate = new ArrayList<Hashtable<String, String>>();
			rs = stmt.executeQuery("select entitylabel, entityid, qualitylabel, qualityid, relatedentityLabel, relatedentityid from "+
					this.answertable+" where stateid = '"+stateid+"'");
			//All EQ's associated with a state are populated in hash table
			while(rs.next()){
				Hashtable<String, String> EQ = new Hashtable<String, String> ();
				EQ.put("stateid", stateid);
				for(String field: this.fields){
					String v = rs.getString(field);
					if(v==null){v="";}
					EQ.put(field, v);
				}		
				astate.add(EQ);
			}
			//System.out.println("added ["+stateid+"] from answer");
			astates.add(astate);// the state eq statements are grouped here => gold standard
			ArrayList<Hashtable<String, String>> tstate = new ArrayList<Hashtable<String, String>>();
			rs = stmt.executeQuery("select entitylabel, entityid, qualitylabel, qualityid, relatedentityLabel, relatedentityid from "+
					this.testtable+" where stateid = '"+stateid+"'");
			while(rs.next()){
				Hashtable<String, String> EQ = new Hashtable<String, String> ();
				for(String field: this.fields){
					String v = rs.getString(field);
					if(v==null){v="";}
					EQ.put(field, v);
				}
				tstate.add(EQ);
			}
			tstates.add(tstate);//generated states
			//System.out.println("added ["+stateid+"] from test");
		}
	}


	/****************************************************  Fields  *************************************
	 * 
	 * @param tstate
	 * @param astate
	 */
	private void compareFields() {
		System.out.println("Inside compare fields");
		if(counts == null){
			counts = new Hashtable<String, String> ();
			//init
			for(String field : this.fields){
				counts.put("inanswer"+field, ""+0);		//Gold standard
				counts.put("generated"+field, ""+0); 	//our charparser output
				counts.put("matched"+field, ""+0);		// total number of matches between our algo and gold standard
			}
			//put totals in
			for(String field : this.fields){
				getTotal(field);//counts the number of tokens in a field
			}			
		}


		//collecting matched field by field
		for(String field : this.fields){
	
			float wcount = 0;
			float tcount = 0;
			float acount = 0;
			float tempcount =0;
			System.out.println("Field========"+field);
			for(int i = 0; i < astates.size(); i++){
				System.out.println("state==="+i);
				ArrayList<String> avalues = new ArrayList<String>();
				//There is no guarantee that each entity is mapped to same quality of the corresponding gold standard
				System.out.println("Gold standard total EQ"+astates.get(i).size());
				for(Hashtable<String, String> EQ :astates.get(i)){//gold standard
					String v = EQ.get(field).toLowerCase();
					if(v!=null && v.length()>0){
						String[] vs = v.split("\\s*,\\s*");// handling multiple values in each statement(field level) => multiple EQ's for gold standard
						for(String v1 : vs){
							if(v1.length()>0) avalues.add(v1);// collects the field values from all the EQ's
						}
					}
				}	


				Hashtable<String,Hashtable<String,Float>> topvalues = new Hashtable<String,Hashtable<String,Float>>();
				int eqcount=0;
				System.out.println("charparser total EQ"+tstates.get(i).size());
				for(Hashtable<String, String> EQ :tstates.get(i)){// reference=> CharParser generated output
					System.out.println("EQ"+eqcount);
					ArrayList<String> tvalues = new ArrayList<String>();
					String v = EQ.get(field).toLowerCase();
					if(v!=null && v.length()>0){
						String[] vs = v.split("\\s*(@,)\\s*");
						for(String v1 : vs){
							if(v1.length()>0) tvalues.add(v1);//holds all the entity proposals of this EQ statement
						}
					}
					System.out.println("total number of proposals"+tvalues.size());
					//evaluate cost associated with each proposal and GS, grouping them
					Hashtable<String,Float> groups = new Hashtable<String,Float>();
					for(int j = 0; j<avalues.size(); j++){//gold standard entities
						float maxscore = 0;
						String entityproposal ="";
						ArrayList<Float> otherscores = new ArrayList<Float>();
						//System.out.println("gold standard GS   "+j);
						for(int k = 0; k < tvalues.size(); k++){// reference entity proposals
							String v1 = tvalues.get(k).replace("\"", "");
							String a = avalues.get(j).replace("\"", "");
							
							//here, all the possible combinations should be scored and the best should be retained
							if(field.matches(".*(id)")==true)
							{
								tempcount= getIdMatchScore(v1.toLowerCase(),a.toLowerCase(),field);// uses exact match, partial match using elk to find the similarity score
								//System.out.println(tempcount);
								if(maxscore<tempcount)
								{
									maxscore=tempcount;//score alone is important
								}
								otherscores.add(tempcount);
							}
							else if(field.matches(".*(label)")==true)
							{
								tempcount= getLabelMatchScore(v1.toLowerCase(),a.toLowerCase(),field);// Uses exact match and METEOR evaluation parameter to find the similarity of sentences
								//System.out.println(tempcount);
								if(maxscore<tempcount)
								{
									maxscore=tempcount;
								}
								otherscores.add(tempcount);
							}
						}
						//apply penalty to the maximum score, since many alternatives were proposed
						maxscore = penalty(maxscore,otherscores,otherscores.size());
						System.out.println("gold standard GS   "+j+"Max score ==== "+maxscore);
						groups.put("GS"+j, maxscore);// stores the best of  eq (from GS) *eqp1
					}
					topvalues.put("EQ"+eqcount++, groups);//Stores each EQ's best entity proposals
					//group should contain the maximum combination of generated*gold standard
				}
				//the below code picks out the best EP and E(GS) mapping => basically resolving the groups
					String matched ="";			
					Set<String> EQS = topvalues.keySet();
					String tempgroup="";
				for(String EQ:EQS)//each EQ's
				{
					Hashtable<String,Float> gsgroups = topvalues.get(EQ);//gets the group of each EQ containing GS->EQ matching scores
					Set<String> groups = gsgroups.keySet();
					float max=0;
					tempgroup="";
					for(String group:groups)
					{
						if(matched.contains(group)==false)
						{
							if(max<gsgroups.get(group))
							{
								max = gsgroups.get(group);
								tempgroup=group;
							}
						}
					}
					matched+=" "+tempgroup;
					System.out.println("max====="+max);
					counts.put("matched"+field, ""+(Float.parseFloat(counts.get("matched"+field))+max));

				}
			//	if(tempgroup!="")
			//		topvalues.remove(tempgroup);// to make sure that this group is not considered again as one entity proposal has been already considered
				


			}
		}

		//calculate and output P/R measurements
		String prstring = ""; 
		String fieldstring = "";
		for(String field : this.fields){
			fieldstring += field+"p,"+field+"r,";
			System.out.println("\t\t\t\t matchedfield======"+counts.get("matched"+field));
			System.out.println("\t\t\t\tgeneratedfield======"+counts.get("generated"+field));
			System.out.println("\t\t\t inanswerfield======"+counts.get("inanswer"+field));

			float p = Float.parseFloat(counts.get("generated"+field))==0? -1 : Float.parseFloat(counts.get("matched"+field))/Float.parseFloat(counts.get("generated"+field));
			float r = Float.parseFloat(counts.get("inanswer"+field)) ==0? -1 : Float.parseFloat(counts.get("matched"+field))/Float.parseFloat(counts.get("inanswer"+field));
			prstring += p+","+r+",";
		}
		prstring = prstring.replaceFirst(",$", "");
		fieldstring = fieldstring.replaceFirst(",$", "");
		insertInto(this.prtablefields, fieldstring, prstring);
		System.out.println("End of compare fields");
	}
/*
 * 
 * Calculates penalty for ID and label(proposals)
 * 
 */

	private float penalty(float maxscore, ArrayList<Float> otherscores, int totalsize) {
//System.out.println("inside penalty for EP");
		//removing the max score from the list
		float meansquare =0;
		float standarddeviation =0;
		float finalscore=0,penalty =0;
		for(int i=0;i<otherscores.size();i++)
		{
			if(otherscores.get(i)==maxscore)
				{
				otherscores.remove(i);
				break;
				}
		}
		
		//calculating S.D
		
		for(int i=0;i<otherscores.size();i++)
		{
			meansquare+=(maxscore -otherscores.get(i))*(maxscore -otherscores.get(i));
		}
		if(otherscores.size()>0)
		{
			standarddeviation = meansquare/otherscores.size();
			standarddeviation=(float) Math.sqrt(standarddeviation);
			penalty = (float) (standarddeviation * Math.pow((double)(((float)totalsize-1)/(float)(totalsize)), 3));
		}
		finalscore = maxscore - penalty;
		return finalscore;
	}

	private float getLabelMatchScore(String a, String v, String field) {
		float count=0;

		//TODO: remove some,partof, all
		if(a.length()>0 && v.length()>0 && (a.toLowerCase().equals(v.toLowerCase()))){
			count +=1;
		}
		else
		{
			//Call meteor to get the score(closeness value of the two strings)
			count+=this.meteor(a, v);
		}
		return count;
	}
/*
 * returns the matching score of entityId/quality ID
 * 
 * @parameters a gold standard string 
 * @parameeters v charparser algo generated string
 * 
 * @return count holds the closeness score
 */
	private float getIdMatchScore(String a, String v, String field) {
//System.out.println("inside getidmatch score");
		float count=0;
		ELKReasoner elk =null;
		Hashtable<String,Float> substrings = new Hashtable<String, Float>();//holds substrings in candidate string along with the score
		Hashtable<String,String> equivalence = new Hashtable<String,String>();//hold matching substrings(candidate, reference)
		if(field.matches(".*(entity).*")==true)
		{
			elk = this.elkentity;
		}
		else
		{
			elk = this.elkquality;
		}


		if(a.length()>0 && v.length()>0)
		{
			//make a call to substring function, followed by  replace substring function
			a=format(a);
			v= format(v);
			//The below cache is used to speed up the lookup process
			if(this.substringcache.get(a.trim()+","+v.trim())==null)
			{
			getMatchingSubstrings(a,v,0,a.split(" ").length-1,substrings ,field,elk,equivalence);
			this.substringcache.put(a.trim()+","+v.trim(), substrings);
			this.equivalencecache.put(a.trim()+","+v.trim(), equivalence);
			}
			else
			{
				substrings = this.substringcache.get(a.trim()+","+v.trim());
				equivalence = this.equivalencecache.get(a.trim()+","+v.trim());
				
			}
			count = replaceSubString(a,v,substrings,equivalence);
			count = count/a.split(" ").length;

		}
		return count;
	}

	/**
	 * 
	 * @param prtablefields2
	 */
	private void insertInto(String tablename, String fieldstring, String prstring) {
		try{
			Statement stmt = conn.createStatement();
			System.out.println("insert into "+tablename+"("+fieldstring+")"+" values ("+prstring+")");
			System.out.println(prstring);
			System.out.println(fieldstring);
			stmt.execute("insert into "+tablename+"("+fieldstring+")"+" values ("+prstring+")");
		}catch(Exception e){
			LOGGER.error("", e);
		}		
	}

	/**
	 * count only the fields associated with a state statement
	 * counts the number of entity/quality/relationship tokens in both gold standard and charparser output strings
	 * 
	 * @param field
	 */
	private void getTotal(String field) {
//System.out.println("inside get total");
		try{
			Statement stmt = conn.createStatement();
			//total for answers(Gold standard)
			int count = 0;
			ResultSet rs = stmt.executeQuery("select "+field+" from "+this.answertable+" where "+field+" is not null and length(trim("+field+"))>0 and length(stateid)>0");
			while(rs.next()){
				String t = rs.getString(1);
					if(t.length()>0)
					{
						t= format(t);//removes and|all|some, brackets() and replaces : with underscore "_"
						count++;
					}
			}
			counts.put("inanswer"+field, ""+(count++));
			System.out.println("inanswer"+field+ "    "+(count));

			//total for generated(our algorithm
			count = 0;
			rs = stmt.executeQuery("select "+field+" from "+this.testtable+" where "+field+" is not null and length(trim("+field+"))>0 and length(stateid)>0");
			while(rs.next()){
				String t = rs.getString(1);
				if(t.length()>0)
				{
					t= format(t);//removes and|all|some, brackets() and replaces : with underscore "_"
					count++;
				}
		}
		
			counts.put("generated"+field, ""+(count++));
			System.out.println("generated"+field+ "    "+(count));


		}catch(Exception e){
			LOGGER.error("", e);
		}


	}

	//	private int getTokens(String t) {
	//
	//		String temp="";
	//		Pattern p = Pattern.compile("((pato|bfo|uberon|bspo)_[\\d]+){1}");
	//		Matcher m = p.matcher(t);
	//		
	//		while(m.find()){
	//			temp +=" "+ m.group(1).trim();
	//			}
	//		
	//		return temp.trim().split(" ").length;
	//	}

	/****************************************************  EQs  *************************************
	 * compare EQ's as a whole
	 * 
	 * @param tstate: EQs generated by the algorithm for a state
	 * @param astate: EQs in answer key for a state
	 * @throws SQLException 
	 */
	private void compareEQs() throws SQLException {
		//raw
		System.out.println("inside compare eq's");
		nowislabel=false;

		int totalgenerated = 0;//charparser
		int totalinanswer = 0;//gold standard
		float eqmatchscore =0;
		float statescore =0;
		float totalscore =0;
		String prstring = "";
		String fieldstring = "";
		float stateprecision=0;
		float staterecall =0;
		
		for(int i = 0; i<astates.size(); i++){//Gold standard
			totalinanswer += astates.get(i).size();//Gives in number of EQ's in this state => gold standard
			totalgenerated += tstates.get(i).size();//Gives in number of EQ's in this state => our algorithm
			Hashtable<String,Hashtable<String,Float>> eqgroups = new Hashtable<String,Hashtable<String,Float>>();
			int counter=0;
			statescore=0;
			int eqcount=0;
			System.out.println("state"+i);
			for(Hashtable<String, String> tEQ : tstates.get(i)){
				System.out.println("EQ==="+eqcount++);
				String entity = tEQ.get("entityid");//contains entity proposals seprated by comma
				String relatedentitylabel = tEQ.get("relatedentityid");//ditto
				String quality = tEQ.get("qualityid");//ditto

				quality=quality.replaceAll("\\[.*\\]", "").trim();//replacing anything inside bracket[] with space
				eqgroups.put("EQ"+counter++,matchAstates(entity, relatedentitylabel, quality, astates.get(i), ""));//   EQ,(gs,scores)

			}
			//change the hash table to be GS -> EQ to maximize the score
			//pick out the matching EQ's 
			System.out.println("sorting out EQ's");
			Set<String> keys = eqgroups.keySet();//gives all the eq's
			String gsmatched="";
			for(String key:keys)
			{
				Hashtable<String,Float> eqmatch = eqgroups.get(key);//reading each of the EQ groups(gs,score)
				 
				Set<String> keys2 = eqmatch.keySet();
				float maxscore=0;
				String matched="";
				System.out.println(key);
				for(String key2:keys2)
				{
					System.out.println("gs=== "+key2+" match score"+eqmatch.get(key2));
					if((maxscore<eqmatch.get(key2))&&(gsmatched.matches(key2)==false)) //reading each of the eq's and finding the best match
					{
						maxscore=eqmatch.get(key2);
						matched=key2;
					}
				}
				gsmatched+=" "+matched;//used to track the gold standards already matched
				statescore+=maxscore;
			}
			System.out.println("State score"+i+"   "+statescore);
			
			stateprecision = tstates.get(i).size()==0? -1 :(float)statescore/tstates.get(i).size();
			staterecall = astates.get(i).size()==0? -1 :(float)statescore/astates.get(i).size();
			
			fieldstring = "stateid,stateprecision,staterecall";
			prstring =astates.get(i).get(0).get("stateid")+","+stateprecision+","+staterecall;
			
			this.insertInto(this.prtablestates, fieldstring, prstring);

			
			totalscore+=statescore;
		}
		fieldstring = "exactp, exactr, partialp, partialr";
		float precision = totalgenerated==0? -1 : (float)totalscore/totalgenerated;
		float recall = totalinanswer==0? -1 : (float)totalscore/totalinanswer;
		prstring  = precision +","+ recall +",0" +",0"+"";


		this.insertInto(this.prtableEQs, fieldstring, prstring);
	}



	/**
	 * match the set of 4 values to EQs in aState
	 * the match EQ is removed from aState
	 * @param entity
	 * @param relatedentity
	 * @param quality
	 * @param aState
	 * @param postfix: "label" or ""
	 * @return 2-element int array: the first element is 1 (0) if there is (not) an exact match, the second element is 1 (0) if there is (not) an partial match
	 */
	private Hashtable<String, Float> matchAstates(String entity, String relatedentity,
			String quality, ArrayList<Hashtable<String, String>> aState, String suffix) {
		System.out.println("inside match a state");
		//one state may have N EQs
		float matchscore = 0;
		String entityproposals[] = entity.replace("\"","").split("(@,)");
		String qualityproposals[] = quality.replace("\"","").split("(@,)");
		String relatedentityproposals[] = relatedentity.replace("\"","").split("(@,)");
		Hashtable<String,Float> group = new Hashtable<String,Float>();
//Gives score of all the proposals against all the gold standards
		for(int i = 0; i < aState.size(); i++){//Parsing through multiple EQ's of each state(gold standard)
			ArrayList<Float> otherscores = new ArrayList<Float>();
			for(int j=0;j<entityproposals.length;j++)//parsing through multiple proposals
			{
				for(int k=0;k<qualityproposals.length;k++)
				{
					for(int p=0;p<relatedentityproposals.length;p++)
					{
						float epscore = matchInState(entityproposals[j], relatedentityproposals[p], qualityproposals[k], aState.get(i), suffix);
						
						if(epscore > matchscore)
						{//max of all states as the character's matchsize 
							matchscore = epscore;
							//Not breaking out until, we find more appropriate match by iterating through the EQ's for that particular stateid
						}
						otherscores.add(epscore);
					}
				}	
				}
			int totalsize= gettotalsize(entityproposals.length,qualityproposals.length,relatedentityproposals.length);
			matchscore= penalty(matchscore,otherscores,totalsize);
			group.put("GS"+i, matchscore);//holds the maximum closeness score for all the GS statement with this EQ
			System.out.println("GS"+i+"matchscore"+matchscore);
			}
		return group;
		}



	private int gettotalsize(int entitysize, int qualitysize, int relatedentitysize) {

/*		int maxsize = entitysize;
		if(qualitysize>relatedentitysize)
		{
		if(maxsize<qualitysize)
			maxsize = qualitysize;
		}else
		{
			if(maxsize<relatedentitysize)
				maxsize = relatedentitysize;
		}*/
		return entitysize+qualitysize+relatedentitysize;

	}

	/**
	 * 
	 * @param entity
	 * @param relatedentity
	 * @param quality
	 * @param EQ
	 * @param suffix
	 * @return 0 if entity and/or quality not match, otherwise, 1 or 2 to indicate a partial match, 3 an exact match of all 3 fields 
	 */
	private float matchInState(String entity, String relatedentity,String quality, Hashtable<String, String> EQ, String suffix)
	{
		//System.out.println("inside match in state");
		float totalscore=0;
		
		totalscore+=getIdMatchScore(entity.toLowerCase(),EQ.get("entityid").toLowerCase(),"entity");
		totalscore+=getIdMatchScore(relatedentity.toLowerCase(),EQ.get("relatedentityid").toLowerCase(),"entity");
		totalscore+=getIdMatchScore(quality.toLowerCase(),EQ.get("qualityid").toLowerCase(),"quality");
		//Related entity, if present in both then only it should be taken into consideration		
		if((relatedentity.toLowerCase().equals("")==true)&&(EQ.get("relatedentityid").toLowerCase().equals("")==true))
		return totalscore/2;
		else
		return totalscore/3;//approximation to 1
		
	}
	/*
	 * 
	 * Calculates METEOR score which is used to tell the closeness of two sentences
	 * @parameter candidate Represents the algorithm output
	 * @parameter refernce Represents the goldstandard output
	 */
	public  float meteor(String candidate,String reference)
	{
		//System.out.println("inside meteor");
		candidate = candidate.replaceAll("(\\(|\\))","");
		reference = reference.replaceAll("(\\(|\\))","");
		float matchedunigrams = (float)unigramMatcher(candidate,reference);
		float precision = (float) matchedunigrams/candidate.split(" ").length;
		float recall = (float) matchedunigrams/reference.split(" ").length;
		float alpha=(float) 1.0;
		float fmean =  (10*(precision*recall))/((9*precision)+recall);
		float finalscore =(float) 0.0;
		float penalty;		
		String substring;
		float chunklength= (float)maxChunks(candidate,reference,0,candidate.split(" ").length-1);
		// calculate penalty
		if(matchedunigrams>0)
		{
		penalty = (float) ((float)(0.5)* Math.pow((chunklength/matchedunigrams), 3));//chunklength shouldnt be greater than number of unigrams
		// calculate meteor score
		finalscore = (fmean)*(1-penalty);
		}
		return finalscore;

	}
/*
 * @function returns the number of unigram matches between two strings
 * 
 * 	
 */
	private  int unigramMatcher(String candidate, String reference)
	{
		//System.out.println("inside unigram matcher");
		String cand[] = candidate.split(" ");
		String ref[] = reference.split(" ");
		int matches=0;

		for(int i=0; i<cand.length;i++)
		{
			for(int j=0;j<ref.length;j++)
			{
				if(cand[i].equals(ref[j])==true)
				{
					ref[j]="";
					matches+=1;
					break;
				}
			}
		}
		return matches;
	}

	/*
	 * Calculate the number of matching chunks between the input and reference strings
	 * @parameter candidate represents the candidate string
	 * @parameter reference represents the reference string
	 * 
	 * @Return the total number of chunks in a candidate string that matches the reference string	
	 */
	private int maxChunks(String candidate,String reference,int start,int end)
	{
		//System.out.println("inside max chunks");
		String tokens[] = candidate.split(" ");
		String bigchunk="";
		String substring;
		int chunklength=0;
		int chunkstart=-1;
		int chunkend=-1;

		// Find the maximum sized chunk
		for(int i=start;i<end+1;i++)
		{
			for(int j=end;j>=i;j--)
			{
				substring = substring(tokens,i,j);

				if((reference.matches("(^|.*?\\s+)("+substring+")(\\s+.*|$)")==true)&&(substring.split(" ").length)>chunklength)
				{
					chunklength = substring.split(" ").length;
					bigchunk=substring;
					chunkstart = i;
					chunkend = j;
				}
			}
		}
		if(chunklength!=0)
		{
			if(chunkstart>start && chunkend<end)
			{
				return 1+maxChunks(candidate,reference,start,chunkstart-1)+maxChunks(candidate,reference,chunkend+1,end);
			}
			else if((chunkstart==start)&&(chunkend!=end))
			{
				return 1+maxChunks(candidate,reference,chunkend+1,end);
			}
			else if((chunkstart!=start)&&(chunkend==end))
			{
				return 1+maxChunks(candidate,reference,start,chunkstart-1);
			}
			else
			{
				return 1;
			}
		} else 
		{
			//substring = substring(tokens,start,end);
			//System.out.println(substring);
			return 0 ;
		}


	}
	/*
	 * @param type can be "entity" or "quality"
	 * @param substringmatches holds the substring and the score of that substring
	 * @param equivalence holds the matching substrings in compared strings
	 */

	private void getMatchingSubstrings(String candidate,String reference,int start, int end, Hashtable<String,Float> substrings,String type,ELKReasoner elk,Hashtable<String,String> equivalence)
	{
		//System.out.println("inside getmatching substring");
		//System.out.println(candidate+"       "+reference);
		if (candidate == null || reference == null || candidate.length() == 0 || reference.length() == 0) {
			return;
		}
		String c[] = candidate.split(" ");
		String r[] = reference.split(" ");
		String substring="",matchingrefstring ="";
		int chunklength = 0,maxi=-1,maxj=-1;
		//int clength = c.length>end?end:c.length;
		int rlength = r.length;
		int[][] matchtable = new int[end+1][rlength];
		float[][] scoretable = new float[end+1][rlength];
		float finalscore=(float) 0.0;
		String[][] matches = new String[end+1][rlength];

		for (int i = start; i <= end; i++) {
			
			if(c[i].matches(".*(bspo|BSPO|UBERON|uberon|BFO|bfo|RO|ro).*")==true)
			{
				if(c[i].matches(".*(bspo|BSPO).*")==false)
					elk = this.elkentity;
				else
					elk = this.elkspatial;
			}
			else
			{
				elk = this.elkquality;
			}
			for (int j = 0; j < rlength; j++) {
				float score=0;
				//direct equal check or partial match a score should be assigned
				if(c[i].equals(r[j]))
				{
					score =1;
				} else if(elk.isSubClassOf("http://purl.obolibrary.org/obo/"+c[i].toUpperCase(),"http://purl.obolibrary.org/obo/"+r[j].toUpperCase())==true || elk.isSubClassOf("http://purl.obolibrary.org/obo/"+r[j].toUpperCase(),"http://purl.obolibrary.org/obo/"+c[i].toUpperCase())==true)
				{
					score=(float) 0.75;
				} else if((c[i].matches(".*(pato|PATO).*")==false)&&((elk.isPartOf("http://purl.obolibrary.org/obo/"+c[i].toUpperCase(),"http://purl.obolibrary.org/obo/"+r[j].toUpperCase())==true ||  elk.isPartOf("http://purl.obolibrary.org/obo/"+r[j].toUpperCase(),"http://purl.obolibrary.org/obo/"+c[i].toUpperCase())==true)))
				{
					score=(float) 0.75;
				}

				if (score>0) {
					if (i == 0 || j == 0) {
						matchtable[i][j] = 1;
						matches[i][j] = c[i];
						scoretable[i][j] = score;
					}
					else {
						matchtable[i][j] = matchtable[i - 1][j - 1] + 1;
						matches[i][j] = ((matches[i-1][j-1]!=null?matches[i-1][j-1]:"")+" "+c[i]).trim();
						scoretable[i][j] = scoretable[i-1][j-1]+score;

					}
					if (matchtable[i][j] > chunklength) {
						chunklength = matchtable[i][j];
						substring = matches[i][j];
						finalscore = scoretable[i][j];
						maxi=i;
						maxj=j;
					}
				}
			}
		}

		if(chunklength!=0)
		{
			substrings.put(substring, finalscore);

			for(int i=maxj;i>maxj-chunklength;i--)
				matchingrefstring=r[i]+" "+matchingrefstring;

			matchingrefstring = matchingrefstring.trim();
			equivalence.put(substring, matchingrefstring);//holds the equivalence candidate and reference substrings(to mainly handle partial matches)

			if(maxi-chunklength>=start && maxi<end)
			{
				getMatchingSubstrings(candidate,reference,start,maxi-chunklength,substrings,type,elk,equivalence);
				getMatchingSubstrings(candidate,reference,maxi+1,end,substrings,type,elk,equivalence);
			}
			else if((maxi-chunklength+1==start)&&(maxi!=end))
			{
				getMatchingSubstrings(candidate,reference,maxi+1,end,substrings,type,elk,equivalence);
			}
			else if(((maxi-chunklength)+1!=start)&&(maxi==end))
			{
				getMatchingSubstrings(candidate,reference,start,maxi-chunklength,substrings,type,elk,equivalence);
			}
			else
			{
				//System.out.println("--end of function-----");
				return;
			}
		} else 
		{
			substrings.put(substring,finalscore);
		//	System.out.println("--end of function-----");
			return;
		}
	}
/*
 * creates a substring with specified start and ending token values
 */
	private static String substring(String[] tokens, int i, int j) {

		String substring="";
		for( ;i<=j;i++)
		{
			substring+=tokens[i]+" ";
		}
		return substring.trim();
	}

	public static void work(String database, String testtable, String answertable, String prtable){

		EQPerformanceEvaluation pe = new EQPerformanceEvaluation(database, testtable, answertable, prtable);		
		pe.evaluate();
	}

/*
 * It takes the list of substrings that are common to candidate and reference strings.
 * Then it creates a character sequence and using LCS logic, it identifies and remove the LCS
 * It also calculates the closeness of two strings whose substring matching is given
 */
	private static float replaceSubString(String candidate, String reference,
			Hashtable<String, Float> substrings, Hashtable<String,String> substringmap) {
//System.out.println("inside replace substring");
		char alphabets = 'a';
		int i=0;
		float finalscore= 0;
		Hashtable<String,Float> chunkscore = new Hashtable<String,Float>();
		Set<String> keys = substrings.keySet();
		String candidatecopy = candidate;
		String referencecopy = reference;
		//replaces each matching chunk with an alphabet followed by @@(delimiter) in both reference and candidate strings
		keys = sort(keys);// The longest matching substring should be replaced first
		
		for(String key:keys)
		{
			if(key!="")
			{
				String replace = (String)((char)(alphabets+i)+"@@");
				candidate = candidate.replace(key.trim(), replace.trim());
				if(substringmap.get(key).equals(key))//to map the partial and exact matchings
				{
					reference = reference.replace(key.trim(), replace);
				}
				else
				{
					reference = reference.replace(substringmap.get(key).trim(), replace);
				}
				chunkscore.put(replace.replaceAll("@@", ""), substrings.get(key));//stores the scores of each of the chunks
				i++;
			}
		}

		String tokens[] = candidate.split(" ");
		//replacing unmatched tokens with alphabets @@ is used to distinguish alphabets from normal text
		for(int j=0;j<tokens.length;j++)
		{
			if((tokens[j].contains("@@")==false)&&(tokens[j].trim().equals("")==false))
			{
				candidate=candidate.replace(tokens[j], (String)((char)(alphabets+i)+"@@"));
				reference=reference.replace(tokens[j], (String)((char)(alphabets+i)+"@@"));
				i++;
			}

		}

		tokens = reference.split(" ");
		//replacing unmatched tokens with alphabets in reference string

		for(int j=0;j<tokens.length;j++)
		{
			if(tokens[j].contains("@@")==false &&(tokens[j].trim().equals("")==false))
			{
				reference=reference.replace(tokens[j], (String)((char)(alphabets+i)+"@@"));
				i++;
			}
		}
		//formatting candidate and reference strings to find LCS
		candidate = candidate.replaceAll("@@", "");
		reference = reference.replaceAll("@@", "");
		candidate = candidate.replaceAll(" ", "");
		reference = reference.replaceAll(" ", "");

		//find LCS and remove it
		ArrayList<String> position = lcs(candidate,reference,chunkscore);//holds the position of LCS in both candidate and reference strings

		tokens = position.get(0).split(" ");
		char can[] = candidate.toCharArray();
		for(String index:tokens)
		{
			if(index!="")
			{
				finalscore+=chunkscore.get(can[Integer.parseInt(index)]+"");
				can[Integer.parseInt(index)] = '\0';
			}
		}
		candidate = "";
		for(char c:can)//rebuilding candidate string
		{
			if(c!='\0')
				candidate+=c;
		}


		tokens = position.get(1).split(" ");
		char ref[] = reference.toCharArray();
		for(String index:tokens)
		{
			if(index!="")
			{
				ref[Integer.parseInt(index)] = '\0';
			}
		}
		reference = "";
		for(char r:ref)//rebuilding reference string
		{
			if(r!='\0')
				reference+=r;
		}
		//the final score of the LCS is added with other matching chunks but those not in LCS. Since order is mismatched a penalty of halving is applied here
		for(char c:candidate.toCharArray())
		{
			if(reference.contains(c+"")==true)
			{
				finalscore+=((chunkscore.get(c+""))/2);
				reference.replaceFirst(c+"", "");
			}
		}

		return finalscore;
		//System.out.println(candidate);
		//System.out.println(reference);
	}
	
	//Sorts the given set according to length of the key
	private static Set<String> sort(Set<String> keys) {
//System.out.println("Inside sort");
		LinkedHashSet<String> temp1 = new LinkedHashSet<String>();
		String temp2[] = new String[keys.size()];
		String temp;
		int i=0;
		for(String key:keys)
		{
			temp2[i++] = key;
		}
		
		for(int j=0;j<i;j++)
		{
			for(int k=j+1;k<i;k++)
			{
				if(temp2[j].split(" ").length<temp2[k].split(" ").length)
				{
					temp=temp2[j];
					temp2[j]=temp2[k];
					temp2[k]=temp;
				}
			}
			temp1.add(temp2[j]);
		}
		
		
		return temp1;
}

	/*
	 * 
	 * returns the longest common subsequence
	 * If there are more than one LCS, then the one with highest score is chosen
	 * 
	 * 
	 */
	public static ArrayList<String> lcs(String a, String b, Hashtable<String, Float> chunkscore) {
		//System.out.println("Inside LCS");
		int[][] lengths = new int[a.length()+1][b.length()+1];
		String candidateposition ="";
		String referenceposition ="";
		ArrayList<String> position = new ArrayList<String>();
		int max=0;
		// row 0 and column 0 are initialized to 0 already

		for (int i = 0; i < a.length(); i++)
		{
			for (int j = 0; j < b.length(); j++)
			{
				if (a.charAt(i) == b.charAt(j))
				{
					lengths[i+1][j+1] = lengths[i][j] + 1;
				}
				else
				{ lengths[i+1][j+1] =
				Math.max(lengths[i+1][j], lengths[i][j+1]);
				}
	        	if(lengths[i+1][j+1]>max)
	        		max = lengths[i+1][j+1];
			}
		}
	
		//Used to identify all the LCS positions
        String pos[] = new String[a.length()+1];
        int k=0;
        
	    for (int i = 0; i < a.length()+1; i++)
	    {
	    	for (int j = 0; j < b.length()+1; j++)
	        {
	        	if(lengths[i][j]==max)
	        	{
	        		pos[k++] = ""+i+","+j;
	        		break;
	        	}
	        }
	    }
	    
		// read the substrings out from the matrix
	    StringBuffer sb;
	    StringBuffer finalsb = null;
	    String finalcandidateposition="";
	    String finalreferenceposition="";
	    for(int p=0;p<k;p++)
	    {
	    	sb = new StringBuffer();
	    	candidateposition="";
	    	referenceposition ="";
	    for (int x = Integer.parseInt(pos[p].split(",")[0]), y = Integer.parseInt(pos[p].split(",")[1]);
	         x != 0 && y != 0; ) {
			if (lengths[x][y] == lengths[x-1][y])
			{
				x--;
			}
			else if (lengths[x][y] == lengths[x][y-1])
			{
				y--;
			}
			else {
				assert a.charAt(x-1) == b.charAt(y-1);
				sb.append(a.charAt(x-1));
				candidateposition=(x-1)+" "+candidateposition;
				referenceposition=(y-1)+" "+referenceposition;
				x--;
				y--;
			}	
			if(finalsb==null)
				{
				finalsb=sb;
				finalcandidateposition=candidateposition;
				finalreferenceposition = referenceposition;
				}
			else
				{
				finalsb = resolveBasedOnScore(finalsb,sb,chunkscore);
				if(finalsb.equals(sb)==true)
				{
					finalcandidateposition=candidateposition;
					finalreferenceposition = referenceposition;
				}
				}
			}
	    }
		position.add(finalcandidateposition.trim());
		position.add(finalreferenceposition.trim());
		//System.out.println(sb.reverse().toString());
		return position;
	}

	private static StringBuffer resolveBasedOnScore(StringBuffer finalsb,
			StringBuffer sb, Hashtable<String, Float> chunkscore) {
		
		char temp[]=finalsb.toString().toCharArray();
		float score1=0,score2=0;
		
		for(int i=0;i<temp.length;i++)
		{
			score1+=chunkscore.get(temp[i]+"");
		}
		
		temp=sb.toString().toCharArray();
		for(int i=0;i<temp.length;i++)
		{
			score2+=chunkscore.get(temp[i]+"");
		}
		
		if(score1>score2)
			return finalsb;
		else
			return sb;
	}

	private static String format(String token) {

		token = token.replaceAll("(and |some |all )", "");
		token = token.replaceAll("(\\(|\\))", "");
		token = token.replace(":", "_");
		return token;

	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String database = "biocreative2012";
		//String candidate = "the cat sat on the mat";
		//	String reference = "the cat was sat on the mat";
		//System.out.println(meteor(candidate,reference,1,1));
		work(database, "input", "goldstandard","NewEQEval");
		
		//maxChunks("28135","13528635",0,3);

//		String candidate ="UBERON:0002743 and (OBO_REL_part_of some (UBERON:0004741 and (OBO_REL_part_of some UBERON:0011683)))";
//		String reference = "UBERON:0007831 and (OBO_REL_part_of some (UBERON:0011648 and (OBO_REL_part_of some UBERON:0002743)))";
//		LinkedHashMap<String,Float> temp = new LinkedHashMap<String,Float>();
//		Hashtable<String,String> equivalence = new Hashtable<String,String>();
//
//		ELKReasoner elk = null;
//		try {
////			elk = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+"ext.owl"));
//		} catch (OWLOntologyCreationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		//start should be zero and end should be length of candidate string -1
		//getMatchingSubstrings("2 8 1 3 5 2 8 7","1 3 5 1 3 8 5 6 3 5 2",0,7,temp,"exact",elk);
		//	candidate = format(candidate);
		//	reference = format(reference);
		//	getMatchingSubstrings(candidate,reference,0,candidate.split(" ").length-1,temp,"entity",elk,equivalence);
		//    System.out.println("final score====="+replaceSubString(candidate,reference,temp,equivalence));
		//    candidate= candidate.replace(" ", "");
		//    reference = reference.replace(" ", "");

	}




}
