/**
 * 
 */
package outputter.evaluation;


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

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import outputter.ApplicationUtilities;
import outputter.XML2EQ;
import outputter.knowledge.Dictionary;
import outputter.knowledge.ELKReasoner;
import owlaccessor.OWLAccessorImpl;




/**
 * @author Hariharan
 * Used the existing EQPerformance and enhanced it to work with upgraded algorithm
 * 
 */
@SuppressWarnings("unused")
public class EQPerformanceEvaluation {
	private static final Logger LOGGER = Logger.getLogger(EQPerformanceEvaluation.class);   
	private Connection conn;
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
	private Hashtable<String,String> existscache = new Hashtable<String,String>();// holds whether a id exists in ontology
	private Hashtable<String,Hashtable<String,Hashtable<String,String>>> Fieldgsnotontology = new Hashtable<String,Hashtable<String,Hashtable<String,String>>>(); // Stores fieldid<stateid,<original string,modified string>>

	static String relation ="inheres_in|adjacent_to|distal_to|OBO_REL_part_of|part of|inheres in|adjacent to|distal to|PHENOSCAPE_complement_of|complement of|and|some|bearer_of|anterior_to|anteriorly_connected_to|attaches_to|extends_from|connected_to|decreased_in_magnitude_relative_to|deep_to|develops_from|distal_to|distally_connected_to|dorsal_to|encloses|extends_to|has_cross_section|has_muscle_insertion|has_muscle_origin|has_part|in_anterior_side_of|in_distal_side_of|in_lateral_side_of|in_left_side_of|in_median_plane_of|in_posterior_side_of|in_proximal_side_of|in_right_side_of|increased_in_magnitude_relative_to|located_in|overlaps|part_of|passes_through|posterior_to|posteriorly_connected_to|proximal_to|proximally connected to|similar_in_magnitude_relative_to|surrounded by|surrounds|ventral_to|vicinity_of|serves_as_attachment_site_for|inheres_in|not";
	static String relationid = relation+ "|BFO_0000050|BFO_0000052|BFO_0000053|BFO:0000053|RO:0002220|BSPO:0000096|UBERON:anteriorly_connected_to|UBERON:attaches_to|PHENOSCAPE:extends_from|RO:0002150|PATO:decreased_in_magnitude_relative_to|BSPO:0000107|RO:0002202|BSPO:0000097|UBERON:distally_connected_to|BSPO:0000098|UBERON:encloses|PHENOSCAPE:extends_to|PATO:has_cross_section|UBERON:has_muscle_insertion|UBERON:has_muscle_origin|BFO:0000051|BSPO:0000123|BSPO:0000125|UBERON:in_lateral_side_of|BSPO:0000120|UBERON:in_median_plane_of|BSPO:0000122|BSPO:0000124|BSPO:0000121|PATO:increased_in_magnitude_relative_to|OBO_REL:located_in|RO:0002131|BFO:0000050|BSPO:passes_through|BSPO:0000099|UBERON:posteriorly_connected_to|BSPO:0000100|UBERON:proximally_connected_to|PATO:similar_in_magnitude_relative_to|RO:0002219|RO:0002221|BSPO:0000102|BSPO:0000103|PHENOSCAPE:serves_as_attachment_site_for|PHENOSCAPE:complement_of";


	private ArrayList<String> states = new ArrayList<String>(); 
	private ELKReasoner elkentity,elkquality,elkspatial;
	//init
	Hashtable<String, String> counts;
	ArrayList<String> fields = new ArrayList<String>();
	private boolean nowislabel=false;
	private String runsetting;
	private int partialcounts = 0;

	/**
	 * @param runsetting 
	 * 
	 */
	public EQPerformanceEvaluation(String database, String testtable, String answertable, String prtable, String runsetting) {
		this.testtable = testtable;
		this.answertable = answertable;
		this.prtableEQs = prtable+"_EQs";
		this.prtablefields = prtable+"_fields_"+runsetting;
		this.prtablestates = prtable+"_states_"+runsetting;
		this.substringcache= new Hashtable<String, Hashtable<String, Float>>();
		this.equivalencecache = new Hashtable<String,Hashtable<String,String>>();
		this.runsetting = runsetting;
		String ontodir = ApplicationUtilities.getProperty("ontology.dir");
		String uberon = ontodir+System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.uberon")+".owl";
		String bspo = ontodir+System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.bspo")+".owl";
		String pato = ontodir+System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.pato")+".owl";
		//long startTime = System.currentTimeMillis();
		try {
			this.elkentity = new ELKReasoner(new File(XML2EQ.uberon==null? uberon : XML2EQ.uberon), false);
			this.elkquality = new ELKReasoner(new File(XML2EQ.pato==null? pato :XML2EQ.pato), false);
			this.elkspatial = new ELKReasoner(new File(XML2EQ.bspo==null? bspo: XML2EQ.bspo), false);
		} catch (OWLOntologyCreationException e1) {
			LOGGER.debug("", e1);
			System.out.print("can't load reasoner");
			System.exit(1);
		}
		//long stopTime = System.currentTimeMillis();
		//System.out.println("time spent on init elks was " + (stopTime - startTime)/60000f + " minutes.");

		initFields();
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
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
						"runsetting varchar(100),"+
						"exactp float(4,2), exactr float(4,2)" +
						")");

				stmt.execute(sql);
				//Holds the state level EQ Precision and recall values
				LOGGER.debug("create table if not exists "+prtablestates+" (stateid varchar(100) primary key, " +
						"stateprecision float(4,2), staterecall float(4,2)" +
						")");
				stmt.execute("create table if not exists "+prtablestates+" (stateid varchar(100) primary key, " +
						"stateprecision float(4,2), staterecall float(4,2)" +
						")");

				stmt.execute("delete from "+prtablestates);

			}
		}catch(Exception e){
			LOGGER.error("", e);
		}

		//long stopTime2 = System.currentTimeMillis();
		//System.out.println("time spent on init fields and db was " + (stopTime2 - stopTime)/60000f + " minutes.");

	}

	private void initFields() {
		//this.fields.add("stateid");
		this.fields.add("entitylabel");
		this.fields.add("entityid");
		this.fields.add("qualitylabel");
		this.fields.add("qualityid");
		this.fields.add("relatedentitylabel");
		this.fields.add("relatedentityid");
		/*this.fields.add("entity");
		this.fields.add("quality");
		this.fields.add("relatedentity");*/
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

			LOGGER.debug(System.currentTimeMillis());
			//long startTime = System.currentTimeMillis();
			readResultsfromDatabase();			
			//long stopTime = System.currentTimeMillis();
			//System.out.println("time spent on reading results was " + (stopTime - startTime)/60000f + " minutes.");

			compareFields();//precision and recall for each of the fields
			//long stopTime2 = System.currentTimeMillis();
			//System.out.println("time spent on comparing fields was " + (stopTime2 - stopTime)/60000f + " minutes.");

			readResultsfromDatabase();

			//long stopTime3 = System.currentTimeMillis();
			//System.out.println("time spent on reading results [again] was " + (stopTime3 - stopTime2)/60000f + " minutes.");

			compareEQs(); //for raw/labeled EQ statements


			//long stopTime4 = System.currentTimeMillis();
			//System.out.println("time spent on comare EQs was " + (stopTime4 - stopTime3)/60000f + " minutes.");
			LOGGER.debug(System.currentTimeMillis());

			//compareNonOntologizedGS();


		}catch(Exception e){
			LOGGER.error("", e);
		}
		//long startTime = System.currentTimeMillis();

		/*System.out.println("reasoning results: ");

		System.out.println("elkentity: subclass:");
		Set<String> e = elkentity.subclasscache.keySet();
		for(String key: e){
			System.out.println(key+"=>"+elkentity.subclasscache.get(key));
		}
		System.out.println("elkquality: subclass:");
		e = elkquality.subclasscache.keySet();
		for(String key: e){
			System.out.println(key+"=>"+elkquality.subclasscache.get(key));
		}
		System.out.println("elkspatial: subclass:");
	    e = elkspatial.subclasscache.keySet();
	    for(String key: e){
			System.out.println(key+"=>"+elkspatial.subclasscache.get(key));
		}
	    System.out.println("elkentity: partof:");
		e = elkentity.partofcache.keySet();
		 for(String key: e){
			System.out.println(key+"=>"+elkentity.partofcache.get(key));
		}
		System.out.println("elkquality: partof:");
		e = elkquality.partofcache.keySet();
		for(String key: e){
			System.out.println(key+"=>"+elkquality.partofcache.get(key));
		}

		System.out.println("elkspatial: partof:");
	    e = elkspatial.partofcache.keySet();
	    for(String key: e){
			System.out.println(key+"=>"+elkspatial.partofcache.get(key));
		}

		System.out.println("end of reasoning results ");
		 */

		this.elkentity.dispose();
		this.elkquality.dispose();
		this.elkspatial.dispose();

		//long stopTime = System.currentTimeMillis();
		//System.out.println("time spent on disposing elks was " + (stopTime - startTime)/60000f + " minutes.");
		System.out.println("total partial counts="+this.partialcounts);

	}

	private void compareNonOntologizedGS() throws SQLException {


		//get the set of stateid's
		//fetch from gold standard and store stateid and gslabel
		//capture the index differences of the original label id and nullified ones
		//get the corresponding label and extract the matching labels.store them # separated
		//fetch from charparser output and store stateid and charparser string
		//using stateid get the string and unontologized entity string 
		//if unontologized string is present then use it, else parse through the string and extract the entities separately
		//   separate proposals by @, compare the eq with each one of the GS and arrive at mazimum score

		Set<String> fields = Fieldgsnotontology.keySet();
		Hashtable<String,Hashtable<String,String>> field_states = new Hashtable<String,Hashtable<String,String>>();
		Hashtable<String,String>  gsstates;
		Hashtable<String,String>  cpstates;
		Hashtable<String,Float> count = new Hashtable<String,Float>();

		for(String field:fields)
		{
			gsstates = new Hashtable<String,String>();
			cpstates = new Hashtable<String,String>();
			getNonOntologizedvaluefromdb(field,Fieldgsnotontology.get(field),gsstates,cpstates,count);
			compareNonOntologized(field,gsstates,cpstates,count);
		}

		for(String field:fields)
		{
			System.out.println(count.get("gs"+field));
			System.out.println(count.get("cp"+field));
			System.out.println(count.get("gsmatched"+field));

		}

	}

	private void compareNonOntologized(String field,Hashtable<String, String> gsstates,
			Hashtable<String, String> cpstates, Hashtable<String,Float> count) {

		float match_score =0;//contains the total matched score
		Set<String> stateids = gsstates.keySet();//Fetches the states of all the objects
		//ArrayList<String> gsEQs = new ArrayList<String>();

		for(String stateid:stateids)// Each of he state id's
		{
			String gsstrings = gsstates.get(stateid);
			String[] gsEQs = gsstrings.split("<>");//Each individual GS array

			String cpstrings = cpstates.get(stateid);
			String[] cpEQs = cpstrings.split("<>");//Each individual EQ array
			Hashtable<String,Float> gseq_matchedscore = new Hashtable<String,Float>();//contains the matched score of all the EQ's of this state
			//prepopulate  0 in the counts
			/*			for(String gsEQ:gsEQs)
			{
				gseq_matchedscore.put(gsEQ, 0.0f);// This will fail, if the GSEQ term is same all over
			}*/
			String matched_gs_final ="";
			for( String cpEQ: cpEQs) // each of charparser EQ's
			{
				float max_score_gs_cp_eq =0;//contains maximum score of a GS that best matches this CP
				String matched_gs ="";
				for(String gsEQ:gsEQs) // Each of the GS eq's - here we are comparing all GS against a single CP EQ
				{
					if(matched_gs_final.contains(gsEQ)==false)
					{
						String[] cpEQPs = cpEQ.split("@,");//Each charparser proposals
						float max_score_gs =0;
						String GS_CP_final_match ="";
						for(String cpEQP:cpEQPs)//Each of the charparser proposals
						{
							String[] cpindividuallabels = cpEQP.split("#");//each individual objects/entities in each of the proposal
							String[] gsindividuallabels = gsEQ.split("#");// each of the GS individual objects
							String pairedCP ="";
							float final_maxscore =0;
							for(String gsindividal:gsindividuallabels)// each individual GS objects
							{
								float score =0,maxscore=0;;
								String matchcp ="";//holds the perfect match for this individual gs entity
								for(String cpeachentities:cpindividuallabels)// each individual cp objects
								{
									score=getLabelMatchScore(cpeachentities,gsindividal,field);//returns string to label matching
									if((maxscore<score)&&(pairedCP.contains(cpeachentities)==false))//need to check contains********
									{
										maxscore = score;									
										matchcp=cpeachentities;
									}
								}
								pairedCP+=matchcp+" ";
								final_maxscore+=maxscore;// contains the maximum score of this GS with this CP proposal
							}
							final_maxscore /=gsindividuallabels.length;//it will give a score in the range of 0 to 1 -( maximum score of this GS with this proposal)
							if(max_score_gs<final_maxscore)
							{								
								max_score_gs = final_maxscore;//stores the final maxscore of this GSeq with this cp
							}					
						}
						if(max_score_gs_cp_eq<max_score_gs)
						{
							max_score_gs_cp_eq=max_score_gs;
							matched_gs = gsEQ;// contains the best matching GS(of all) for this cp EQ
						}
					}
				}
				matched_gs_final+=matched_gs+" ";////contains the best EQproposal for this GS
				gseq_matchedscore.put(matched_gs, max_score_gs_cp_eq);//store the maximum score of this GS with this CP				
				match_score +=max_score_gs_cp_eq;
			}


		}

		count.put("gsmatched"+field, match_score);

	}

	private void getNonOntologizedvaluefromdb(String field, Hashtable<String, Hashtable<String, String>> gsnotontology, Hashtable<String, String> gsstates, Hashtable<String, String> cpstates, Hashtable<String, Float> count) throws SQLException {

		Statement stmt = conn.createStatement();

		String rootfield = field.replace("id", "");
		String sql;
		ResultSet rs;
		Set<String> stateids = gsnotontology.keySet();//this returns the stateid's
		float countgseq=0,countcpeq=0;//counts the total number of EQ's i charparser and Gold standard seprately

		for(String stateid:stateids)
		{
			//fetching from GS first
			Hashtable<String,String> eqs = gsnotontology.get(stateid);//this returns the original id string, ontology nullified strings
			Set<String> eqkeys = eqs.keySet();//this returns each of eq's original id's
			String multipleeqs ="";
			for(String eqkey:eqkeys)//this fetches the original label of each of the original id's
			{

				Hashtable<String,Integer> nonexist = nonexistentid(eqkey,eqs.get(eqkey));//returns the difference label between original copy and nullified copy along with the index
				sql = "select "+rootfield+"label from "+this.answertable+" where stateid = '"+stateid+"' and "+rootfield+"id = '"+eqkey+"'";
				rs = stmt.executeQuery(sql);
				String unontologized_label ="";
				if(rs.next())
				{
					String label= rs.getString(rootfield+"label");
					Set<String> ids = nonexist.keySet();

					for(String id:ids)//if multiple temp ids are prsent, their labels are retrieved and stored as "#" separated value
					{
						unontologized_label+=extractLabel(unontologized_label, nonexist.get(id))+"#";
					}

					unontologized_label= unontologized_label.replaceAll("(#)$", "");
				}
				multipleeqs +=unontologized_label+"<>";//<> is the delimiter
				countgseq++;//counts the number of EQ's in Gold standard
			}
			multipleeqs = multipleeqs.replaceAll("(<>)$", "");
			gsstates.put(stateid, multipleeqs); //Stores stateid,eq's(separated by <>)

			//fetching charparser information

			sql = "select "+rootfield+"id, "+rootfield+"label, unontologized"+ rootfield+" from "+this.testtable+" where stateid = '"+stateid+"'";
			rs = stmt.executeQuery(sql);
			String final_string="";
			while(rs.next())
			{
				String label = rs.getString(rootfield+"label");
				String unontologized = rs.getString("unontologized"+rootfield);
				String rootfieldid = rs.getString(rootfield+"id");

				if((unontologized!=null)&&(unontologized.equals("")==false))
				{
					final_string += unontologized +"<>";
				} else
				{
					String[] ids = extractids(rootfieldid);
					ids = clean(ids);
					String[] labels = label.split("@,");
					for(String ilabel:labels)
					{
						for(int i=0;i<ids.length;i++)
						{
							final_string +=extractLabel(label, i)+"#";
						}
						final_string +="@,";
					}
					final_string.replaceAll("(@,)$", "");
					final_string +="<>";
				}
				countcpeq++;//counts the number of EQ's in this state generated by charparser
			}
			final_string.replaceAll("(<>)$", "");
			cpstates.put(stateid, final_string);// contains charaparser's stateid, final_string which is made of terms inside an id separated by # and each EP separated by @,and EQ separated by <>
		}

		count.put("gs"+field,countgseq);
		count.put("cp"+field, countcpeq);

	}

	private void readResultsfromDatabase() throws SQLException {
		//LOGGER.debug("inside read results");
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
			//LOGGER.debug("added ["+stateid+"] from answer");
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
			//LOGGER.debug("added ["+stateid+"] from test");
		}
	}


	/****************************************************  Fields  *************************************
	 * 
	 * @param tstate
	 * @param astate
	 */
	private void compareFields() {
		LOGGER.debug("Inside compare fields");
		if(counts == null){
			counts = new Hashtable<String, String> ();
			//init
			for(String field : this.fields){
				counts.put("inanswer"+field, ""+0);		//Gold standard
				counts.put("generated"+field, ""+0); 	//our charparser output
				counts.put("matched"+field, ""+0);		// total number of matches (matching score) between our algo and gold standard
			}
			//put totals in
			for(String field : this.fields){
				getTotal(field);//counts the number of tokens in a field
			}			
		}


		//collecting matched field by field
		for(String field : this.fields){
			//long startTime = System.currentTimeMillis();
			if(field.matches("entity|relatedentity|quality"))
				continue;

			float wcount = 0;
			float tcount = 0;
			float acount = 0;
			float tempcount =0;
			Hashtable<String,Hashtable<String,String>> GSnotontology = new Hashtable<String,Hashtable<String,String>>(); // Stores stateid,<original string,modified string>

			LOGGER.debug("Field========"+field);
			for(int i = 0; i < astates.size(); i++){
				LOGGER.debug("state==="+i);
				ArrayList<String> avalues = new ArrayList<String>();
				//There is no guarantee that each entity is mapped to same quality of the corresponding gold standard
				LOGGER.debug("Gold standard total EQ"+astates.get(i).size());

				for(Hashtable<String, String> EQ :astates.get(i)){//gold standard
					String v = EQ.get(field).toLowerCase();
					String stateid = EQ.get("stateid");
					if(v!=null && v.length()>0){
						String[] vs = v.split("\\s*,\\s*");// handling multiple values in each statement(field level) => multiple EQ's for gold standard
						String copy ="";
						for(String v1 : vs){
							if(v1.length()>0) 
							{
								if(field.contains("id")==true)
								{
									copy=v1;
									v1 = checkInOntology(v1,field);//nullifies the term, if it is not present in ontology
									if(v1.equals(copy)==false)
									{
										Hashtable<String,String> temp = new Hashtable<String,String>();
										if(GSnotontology.get(stateid)!=null)
										{
											GSnotontology.get(stateid).put(copy,v1);//stores the copy of the nullified term along with the original string, stateid's
										} else
										{
											temp.put(copy,v1);
											GSnotontology.put(stateid,temp);
										}
									}
									avalues.add(v1);// collects the field values from all the EQ's
								}else
								{
									avalues.add(v1);
								}

							}
						}
					}
				}	


				Hashtable<String,Hashtable<String,Float>> topvalues = new Hashtable<String,Hashtable<String,Float>>();
				int eqcount=0;
				int gscount = avalues.size();
				LOGGER.debug("charparser total EQ"+tstates.get(i).size());
				for(Hashtable<String, String> EQ :tstates.get(i)){// reference=> CharParser generated output
					LOGGER.debug("EQ"+eqcount);
					ArrayList<String> tvalues = new ArrayList<String>();
					String v = EQ.get(field).toLowerCase();
					if(v!=null && v.length()>0){
						String[] vs = v.split("\\s*(@,)\\s*");
						for(String v1 : vs){
							if(v1.length()>0)
							{
								if(v1.contains("score")==true) //lower case because of the application of .toLowerCase before
								{
									tvalues.add((v1.substring(0, v1.indexOf("score")-1)).trim());//holds all the entity proposals of this EQ statement
								}else
								{
									tvalues.add(v1.trim());
								}
							}
						}
					}
					LOGGER.debug("total number of proposals"+tvalues.size());
					//evaluate cost associated with each proposal and GS, grouping them
					Hashtable<String,Float> groups = new Hashtable<String,Float>();
					
					for(int j = 0; j<avalues.size(); j++){//gold standard fields
						float maxscore = 0;
						String entityproposal ="";
						ArrayList<Float> otherscores = new ArrayList<Float>();
						//LOGGER.debug("gold standard GS   "+j);
						for(int k = 0; k < tvalues.size(); k++){// reference entity proposals
							String v1 = tvalues.get(k).replace("\"", "");
							String a = avalues.get(j).replace("\"", "");

							//here, all the possible combinations should be scored and the best should be retained
							if(field.matches(".*(id)")==true)
							{
								tempcount= getIdMatchScore(v1.toLowerCase(),a.toLowerCase(),field);// uses exact match, partial match using elk to find the similarity score
								//LOGGER.debug(tempcount);
								if(maxscore<tempcount)
								{
									maxscore=tempcount;//score alone is important
								}
								otherscores.add(tempcount);
							}
							else if(field.matches(".*(label)")==true)
							{
								tempcount= getLabelMatchScore(v1.toLowerCase(),a.toLowerCase(),field);// Uses exact match and METEOR evaluation parameter to find the similarity of sentences
								//LOGGER.debug(tempcount);
								if(maxscore<tempcount)
								{
									maxscore=tempcount;
								}
								otherscores.add(tempcount);
							}
						}
						//apply penalty to the maximum score, since many alternatives were proposed
						maxscore = penalty(maxscore,otherscores,otherscores.size());
						LOGGER.debug("gold standard GS   "+j+"Max score ==== "+maxscore);
						groups.put("GS"+j, maxscore);// stores the best of  eq (from GS) *eqp1
					}
					topvalues.put("EQ"+eqcount++, groups);//Stores each EQ's best entity proposals
					//group should contain the maximum combination of generated*gold standard
				}
				//long stopTime1 = System.currentTimeMillis();
				//System.out.println("time spent on grouping was " + (stopTime1 - startTime)/60000f + " minutes.");

				//the below code calculates the matching score of the best EP and E(GS) mapping 
				//from a matrix with rows being EPs, columns being GS, and cells holding scores
				//the code find the greatest value in a column and then sum up those values from all columns and use the sum as the matching score of the best EP to GS mapping
				//This logic does not identify which of the machine-generated EQ best match which GS EQ. 
				//This logic measures the extent of the 'semantics' in GS EQ is covered by machine-generated EQ set collectively. 
				//float sum = 0f;
				for(int g = 0; g <gscount; g++){
					Set<String> EQS = topvalues.keySet();
					float max = 0f;
					//find the greatest value for GS_g column
					for(String EQ:EQS)//each EQ's
					{
						Hashtable<String,Float> gsgroups = topvalues.get(EQ);//gets the group of each EQ containing GS->EQ matching scores
						if(max < gsgroups.get("GS"+g)){
							max = gsgroups.get("GS"+g);
						}
					}
					LOGGER.debug("max====="+max);
					counts.put("matched"+field, ""+(Float.parseFloat(counts.get("matched"+field))+max));
					//sum += max;
				}
					
				//the code below is logically incorrect. It takes any first encountered non-zero value as the max and not checking other scores. Hong 1/13/2014
				/*String matched ="";			
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
					LOGGER.debug("max====="+max);
					counts.put("matched"+field, ""+(Float.parseFloat(counts.get("matched"+field))+max));
				}*/

				//long stopTime2 = System.currentTimeMillis();
				//System.out.println("time spent on scoring was " + (stopTime2 - stopTime1)/60000f + " minutes.");

				//	if(tempgroup!="")
				//		topvalues.remove(tempgroup);// to make sure that this group is not considered again as one entity proposal has been already considered

			}
			//long stopTime = System.currentTimeMillis();
			//System.out.println("time spent on comparing "+field+" was " + (stopTime - startTime)/60000f + " minutes.");
			if(field.contains("id")==true)
			{
				Fieldgsnotontology.put(field, GSnotontology);
			}
		}

		//calculate and output P/R measurements
		String prstring = ""; 
		String fieldstring = "";
		for(String field : this.fields){
			fieldstring += field+"p,"+field+"r,";
			LOGGER.debug("\t\t\t\t matchedfield======"+counts.get("matched"+field));
			LOGGER.debug("\t\t\t\tgeneratedfield======"+counts.get("generated"+field));
			LOGGER.debug("\t\t\t inanswerfield======"+counts.get("inanswer"+field));

			float p = Float.parseFloat(counts.get("generated"+field))==0? 0 : Float.parseFloat(counts.get("matched"+field))/Float.parseFloat(counts.get("generated"+field));
			float r = Float.parseFloat(counts.get("inanswer"+field)) ==0? 0 : Float.parseFloat(counts.get("matched"+field))/Float.parseFloat(counts.get("inanswer"+field));
			prstring += p+","+r+",";
		}
		prstring = prstring.replaceFirst(",$", "");
		fieldstring = fieldstring.replaceFirst(",$", "");
		insertInto(this.prtablefields, fieldstring, prstring);
		LOGGER.debug("End of compare fields");
	}

	//If the id doesn't exists, it nullifies the id and return the string
	private String checkInOntology(String value, String field) {
		value = value.toUpperCase().trim();
		if(existscache.get(value)!=null)
			return existscache.get(value);

		String[] ids = extractids(value);
		String valuecopy = value;
		Boolean exist=false;
		ELKReasoner tempelk=null;
		for(String id:ids)
		{
			exist=false;
			id=id.trim();

			if(id.contains("BSPO"))
			{
				tempelk = this.elkspatial;
			} else if(field.contains("quality"))
			{
				tempelk = this.elkquality;
			}else
			{
				tempelk = this.elkentity;
			}
			exist=tempelk.CheckClassExistence(id);

			if(exist == false)
			{
				value=value.replaceFirst(id, "null");
			}
		}
		existscache.put(valuecopy, value);

		return value.toLowerCase();
	}

	private static String[] extractids(String value) {
		value=value.replaceAll("(\\(|\\))", "");
		String[] temp = value.split("\\s");
		String id ="";
		for(String t:temp)
		{
			if(t.matches("[A-Z]+[_:][0-9A-Z_-]+")==true)
			{
				id+=t+" ";
			}
		}
		id=id.trim();
		return id.split(" ");
	}

	/*	private boolean checkInOntology(String id) {

		String iri="";
		if(id.startsWith(OWLAccessorImpl.temp)){
			iri=Dictionary.provisionaliri+id.substring(id.indexOf(":")+1);
		}else{
			iri=Dictionary.baseiri+id.replace(':', '_');
		}

		if(allclasses.contains(iri.trim()))		
		return true;
		else
		return false;
	}
	 */
	/*
	 * 
	 * Calculates penalty for ID and label(proposals)
	 * 
	 */

	private float penalty(float maxscore, ArrayList<Float> otherscores, int totalsize) {
		//LOGGER.debug("inside penalty for EP");
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
		//LOGGER.debug("inside getidmatch score");
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
			a= format(a);
			v= format(v);
			//The below cache is used to speed up the lookup process
			if(this.substringcache.get(a.trim()+","+v.trim())==null)
			{

				//getMatchingSubstrings(a,v,0,a.split(" ").length-1,substrings ,field,elk,equivalence);
				substring(a,v,substrings ,field,elk,equivalence);
				this.substringcache.put(a.trim()+","+v.trim(), substrings);
				this.equivalencecache.put(a.trim()+","+v.trim(), equivalence);
			}
			else
			{
				substrings = this.substringcache.get(a.trim()+","+v.trim());
				equivalence = this.equivalencecache.get(a.trim()+","+v.trim());

			}
			count = replaceSubString(a,v,substrings,equivalence);
			count = count/a.split(" ").length; // to reduce it to value of 0.0 - 1.0

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
			LOGGER.debug("insert into "+tablename+"("+fieldstring+")"+" values ("+prstring+")");
			LOGGER.debug(prstring);
			LOGGER.debug(fieldstring);
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
		//LOGGER.debug("inside get total");
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
			LOGGER.debug("inanswer"+field+ "    "+(count));

			//total for generated(our algorithm)
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
			LOGGER.debug("generated"+field+ "    "+(count));


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
		LOGGER.debug("inside compare eq's");
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

		for(int i = 0; i<astates.size(); i++){//all states from Gold standard
			//long startTime = System.currentTimeMillis();
			totalinanswer += astates.get(i).size();//Gives in number of EQ's in this state => gold standard
			totalgenerated += tstates.get(i).size();//Gives in number of EQ's in this state => our algorithm
			Hashtable<String,Hashtable<String,Float>> eqgroups = new Hashtable<String,Hashtable<String,Float>>();
			int counter=0;
			statescore=0;
			int eqcount=0;

			LOGGER.debug("state"+i);

			for(Hashtable<String, String> tEQ : tstates.get(i)){
				LOGGER.debug("EQ==="+eqcount++);
				String entity = tEQ.get("entityid");//contains entity proposals separated by comma
				String relatedentitylabel = tEQ.get("relatedentityid");//ditto
				String quality = tEQ.get("qualityid");//ditto

				quality=quality.replaceAll("\\[.*\\]", "").trim();//replacing anything inside bracket[] with space
				eqgroups.put("EQ"+counter++,matchAstates(entity, relatedentitylabel, quality, astates.get(i), ""));//   EQ,(gs,scores)
			}
			//change the hash table to be GS -> EQ to maximize the score
			
			LOGGER.debug("sorting out EQ's");
			//the below code calculates the matching score of the best EP and E(GS) mapping 
			//from a matrix with rows being EPs, columns being GS, and cells holding scores
			//the code find the greatest value in a column and then sum up those values from all columns and use the sum as the matching score of the best EP to GS mapping
			//This logic does not identify which of the machine-generated EQ best match which GS EQ. 
			//This logic measures the extent of the 'semantics' in GS EQ is covered by machine-generated EQ set collectively. 
			//float sum = 0f;
			for(int g = 0; g <astates.get(i).size(); g++){
				Set<String> EQS = eqgroups.keySet();
				float max = 0f;
				//find the greatest value for GS_g column
				for(String EQ:EQS)//each EQ's
				{
					Hashtable<String,Float> eqmatch = eqgroups.get(EQ);//gets the group of each EQ containing GS->EQ matching scores
					LOGGER.debug("gs=== "+g+" match score"+eqmatch.get("GS"+g));
					if(max < eqmatch.get("GS"+g)){
						max = eqmatch.get("GS"+g);
					}
				}
				statescore += max;
			}
			//TODO: this logic is incorrect. It doesn't get the global maximum.
			/*Set<String> keys = eqgroups.keySet();//gives all the eq's
			String gsmatched="";
			for(String key:keys)
			{
				Hashtable<String,Float> eqmatch = eqgroups.get(key);//reading each of the EQ groups(gs,score)

				Set<String> keys2 = eqmatch.keySet();
				float maxscore=0;
				String matched="";
				LOGGER.debug(key);
				for(String key2:keys2)
				{
					LOGGER.debug("gs=== "+key2+" match score"+eqmatch.get(key2));
					if((maxscore<eqmatch.get(key2))&&(gsmatched.contains(key2)==false)) //reading each of the eq's and finding the best match
					{
						maxscore=eqmatch.get(key2);
						matched=key2;
					}
				}
				gsmatched+=" "+matched;//used to track the gold standards already matched
				gsmatched=gsmatched.trim();
				statescore+=maxscore;
				//long stopTime = System.currentTimeMillis();
				//System.out.println("time spent on EQ"+states.get(i)+" was " + (stopTime - startTime)/60000f + " minutes.");
			}*/
			LOGGER.debug("State score"+i+"   "+statescore);

			stateprecision = tstates.get(i).size()==0? 0 :(float)statescore/tstates.get(i).size();
			staterecall = astates.get(i).size()==0? 0 :(float)statescore/astates.get(i).size();

			fieldstring = "stateid,stateprecision,staterecall";
			prstring ="'"+astates.get(i).get(0).get("stateid")+"',"+stateprecision+","+staterecall;

			this.insertInto(this.prtablestates, fieldstring, prstring);


			totalscore+=statescore;


		}
		fieldstring = "runsetting, exactp, exactr";
		float precision = totalgenerated==0? 0 : (float)totalscore/totalgenerated;
		float recall = totalinanswer==0? 0 : (float)totalscore/totalinanswer;
		prstring  = "'"+this.runsetting+"',"+ precision +","+ recall +"";


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
		LOGGER.debug("inside match a state");
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
			LOGGER.debug("GS"+i+"matchscore"+matchscore);
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
		//LOGGER.debug("inside match in state");
		float totalscore=0;
		if(entity.equals("")==false && entity.indexOf("Score")>=0)
		{
			entity = (entity.substring(0,entity.indexOf("Score")-1)).trim();
		}
		if(relatedentity.equals("")==false && relatedentity.indexOf("Score")>=0)
		{
			relatedentity = (relatedentity.substring(0,relatedentity.indexOf("Score")-1)).trim();
		}
		if(quality.equals("")==false && quality.indexOf("Score")>0)
		{
			quality = (quality.substring(0,quality.indexOf("Score")-1)).trim();
		}
		if(quality.startsWith("Score")) quality="";

		totalscore+=getIdMatchScore(entity.toLowerCase(),EQ.get("entityid").toLowerCase(),"entity");
		totalscore+=getIdMatchScore(relatedentity.toLowerCase(),EQ.get("relatedentityid").toLowerCase(),"entity");
		totalscore+=getIdMatchScore(quality.toLowerCase(),EQ.get("qualityid").toLowerCase(),"quality");
		//Related entity, if present in both then only it should be taken into consideration		
		if((relatedentity.toLowerCase().equals("")==true)&&(EQ.get("relatedentityid").toLowerCase().equals("")==true))
			return totalscore/2;
		else
			return totalscore/3;//approximation to 1

	}

	//-------------------------------------------------------

	/*
	 * 
	 * populates the substring array
	 * 	
	 */
	public void substring(String candidate,String reference,Hashtable<String,Float> substrings,String type,ELKReasoner elk,Hashtable<String,String> equivalence)
	{
		long start = System.currentTimeMillis();
		long end;
		LOGGER.debug("Inside substring "+System.currentTimeMillis());
		int[][] match = new int[candidate.split(" ").length][reference.split(" ").length];
		float[][] scores = new float[candidate.split(" ").length][reference.split(" ").length];
		String[][] matchingstring = new String[candidate.split(" ").length][reference.split(" ").length];

		String c[] = candidate.split(" ");
		String r[] = reference.split(" ");


		for(int i=0;i<candidate.split(" ").length;i++)
		{
			//if(c[i].matches(".*(bspo|BSPO|UBERON|uberon|BFO|bfo|RO|ro).*")==true) //entities could come from CL, GO or other imported ontologies
			if(!c[i].matches(".*[pP][Aa][Tt][Oo].*"))
			{
				if(c[i].matches(".*[Bb][Ss][Pp][Oo].*")==false)
					elk = this.elkentity;
				else
					elk = this.elkspatial;
			}
			else
			{
				elk = this.elkquality;
			}

			for(int j=0;j<reference.split(" ").length;j++)
			{
				float score =0;
				//add code to ignore provisionalid's
				if(c[i].equals(r[j]))
				{
					score=1;
				} else if(elk.isSubClassOf("http://purl.obolibrary.org/obo/"+c[i].toUpperCase(),"http://purl.obolibrary.org/obo/"+r[j].toUpperCase())==true || elk.isSubClassOf("http://purl.obolibrary.org/obo/"+r[j].toUpperCase(),"http://purl.obolibrary.org/obo/"+c[i].toUpperCase())==true)
				{
					score=(float) 0.75;
					partialcounts++;
				} else if((c[i].matches(".*[pP][Aa][Tt][Oo].*")==false)&&((elk.isPartOf("http://purl.obolibrary.org/obo/"+c[i].toUpperCase(),"http://purl.obolibrary.org/obo/"+r[j].toUpperCase())==true ||  elk.isPartOf("http://purl.obolibrary.org/obo/"+r[j].toUpperCase(),"http://purl.obolibrary.org/obo/"+c[i].toUpperCase())==true)))
				{
					score=(float) 0.75;
					partialcounts++;
				} else if(elk.isEquivalent(c[i], r[j]) == true)
				{
					score=(float) 1.0;
					partialcounts++;
				}

				if(score>0)
				{
					if(i==0||j==0)
					{
						match[i][j] =1;
						scores[i][j] = score;
						matchingstring[i][j] = c[i];
					}
					else
					{
						match[i][j] = match[i-1][j-1]+1;
						scores[i][j] = scores[i-1][j-1]+score;
						matchingstring[i][j] = matchingstring[i-1][j-1]+" "+c[i];
					}
				}
			}
		}
		end = System.currentTimeMillis();

		LOGGER.debug("end of substring "+System.currentTimeMillis());
		LOGGER.debug("Difference"+ (end-start));

		getNonOverlappingSubstrings(match,matchingstring,scores,candidate.split(" "),reference.split(" "),substrings, equivalence);

	}
	/*
	 * 
	 * Gets all the non overlapping substring
	 */
	private void getNonOverlappingSubstrings(int[][] match,String[][] matchingstring, float[][] scores,String input[],String reference[],Hashtable<String, Float> substrings,Hashtable<String, String> equivalence) {

		LOGGER.debug("Inside getnonoverlapping "+System.currentTimeMillis());

		int rows= input.length;
		int columns = reference.length;
		int max = matrixIsZero(match,rows,columns);

		while(max!=0)
		{
			float finalscore=0;
			String finalstring="";
			for(int i = rows-1;i>=0;i--)
			{
				for(int j=columns-1;j>=0;j--)
				{
					if(match[i][j] == max)
					{
						String candidatetemp="";
						String referencetemp="";
						finalscore = scores[i][j];
						for(int count=0;count<max;count++)//breaking condition
						{
							if(((i-count)>=0)&&((j-count)>=0))
							{
								candidatetemp = input[i-count]+" "+candidatetemp;//Holds the matching string in candidate
								referencetemp = reference[j-count]+" "+referencetemp;//Holds the equivalent strings in reference
								//correctmatrix(match,j-count,rows,columns);
							}
							else
								break;
						}
						correctmatrix(match,i,j,rows,columns,max);
						if(candidatetemp.equals("")==false)
						{
							substrings.put(candidatetemp.trim(), finalscore);
							equivalence.put(candidatetemp.trim(), referencetemp.trim());
						}
					}
				}
			}
			max = matrixIsZero(match,rows,columns);
		}
		LOGGER.debug("end of getnonoverlapping "+System.currentTimeMillis());


	}


	/*
	 * 
	 * removes redundancy
	 * it zeroes the columns and rows involved to make sure that no overlap happens again
	 */

	private static void correctmatrix(int[][] match, int currentrow, int currentcolumn, int row, int totalcolumns, int length) {

		//making all columns except current column as 0
		length=length-1;
		for(int i=0;i<row;i++)
		{
			for(int j=(currentcolumn-length);j<currentcolumn;j++)
			{
				match[i][j] = 0;
			}
		}
		for(int i=0;i<row;i++)
		{
			if(match[i][currentcolumn]>0)
			{
				match[i][currentcolumn] =0;
				int k=i;
				for(int j=currentcolumn+1;j<totalcolumns;j++)
				{
					k++;
					if(((k<row)&&(j<totalcolumns))&&(match[k][j]>0))
					{
						match[k][j]= match[k-1][j-1]+1;
					}else
						break;

				}

			}
		}
		//cleaning all the rows involved
		for(int i=(currentrow-length);i<currentrow;i++)
		{
			for(int j=0;j<totalcolumns;j++)
			{
				match[i][j] =0;
			}
		}

		for(int i=0;i<totalcolumns;i++)
		{
			if(match[currentrow][i]>0)
			{
				match[currentrow][i] = 0;
				int k=i;
				for(int j=currentrow+1;j<row;j++)
				{
					k++;
					if((k<totalcolumns)&&(match[j][k]>0))
					{
						match[j][k]=match[j-1][k-1]+1;
					}
					else
						break;
				}
			}
		}
	}

	/*
	 * returns maximum value in the current matrix else return 0, saying there is no match
	 * 
	 */
	private static int matrixIsZero(int[][] match, int rows, int columns) {

		int max=0;
		for(int i=0;i<rows;i++)
		{
			for(int j=0;j<columns;j++)
			{
				if(max<match[i][j])
				{
					max= match[i][j];
				}
			}
		}
		return max;
	}



	/*
	 * 
	 * Calculates METEOR score which is used to tell the closeness of two sentences
	 * @parameter candidate Represents the algorithm output
	 * @parameter refernce Represents the goldstandard output
	 */
	public  float meteor(String candidate,String reference)
	{
		//LOGGER.debug("inside meteor");
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
		//LOGGER.debug("inside unigram matcher");
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
		//LOGGER.debug("inside max chunks");
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
			//LOGGER.debug(substring);
			return 0 ;
		}


	}
	/*
	 * @param type can be "entity" or "quality"
	 * @param substringmatches holds the substring and the score of that substring
	 * @param equivalence holds the matching substrings in compared strings
	 */

	/*private void getMatchingSubstrings(String candidate,String reference,int start, int end, Hashtable<String,Float> substrings,String type,ELKReasoner elk,Hashtable<String,String> equivalence)
	{
		//LOGGER.debug("inside getmatching substring");
		//LOGGER.debug(candidate+"       "+reference);
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
				//LOGGER.debug("--end of function-----");
				return;
			}
		} else 
		{
			substrings.put(substring,finalscore);
		//	LOGGER.debug("--end of function-----");
			return;
		}
	}*/
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



	/*
	 * It takes the list of substrings that are common to candidate and reference strings.
	 * Then it creates a character sequence and using LCS logic, it identifies and remove the LCS
	 * It also calculates the closeness of two strings whose substring matching is given
	 */
	private static float replaceSubString(String candidate, String reference,
			Hashtable<String, Float> substrings, Hashtable<String,String> substringmap) {
		//LOGGER.debug("inside replace substring");
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
		//LOGGER.debug(candidate);
		//LOGGER.debug(reference);
	}

	//Sorts the given set according to length of the key
	private static Set<String> sort(Set<String> keys) {
		//LOGGER.debug("Inside sort");
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
		//LOGGER.debug("Inside LCS");
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
		//LOGGER.debug(sb.reverse().toString());
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

	private static Hashtable<String, Integer> nonexistentid(String original, String modified) {

		String originals[] = extractids(original);
		String modifieds[] = extractids(modified);
		originals = clean(originals);
		modifieds = clean(modifieds);

		Hashtable<String,Integer> nonexists = new Hashtable<String,Integer>();

		for(int j=0;j<originals.length;j++)
		{
			int i=0;
			for(i=0;i<modifieds.length;i++)
			{
				if(originals[j].equals(modifieds[i])==true)
				{
					modifieds[i]="";
					break;		
				}
			}
			if(i==modifieds.length)
			{
				nonexists.put(originals[j], j);
			}
		}
		return nonexists;
	}

	private static String[] clean(String[] idarray) {

		String id ="";
		for(String t:idarray)
		{
			if(t.matches("[A-Z]+[_:][0-9]+")==true)
			{
				if(t.matches("("+relationid+")")==false)
					id+=t+" ";
			}
		}
		id=id.trim();
		return id.split(" ");

	}

	private static String extractLabel(String label,int index)
	{
		/*		label= label.replaceAll(relation, "");
		label= label.replaceAll("(\\(|\\))", "");
		label = label.replaceAll("(\\s)+", " ");
		System.out.println(label.split("\\s")[index]);
		return "";*/


		String labels[]=null,final_labels[] = null;
		int count=0;
		label= label.replaceAll("(\\(|\\))", "");
		labels = label.split(relation);
		final_labels = new String[labels.length];	

		for(String temp:labels)
		{
			if(temp.equals(" ")==false)
			{
				final_labels[count++] = temp;
			}
		}
		System.out.println(final_labels[index].trim());
		return final_labels[index].trim();



	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String database = "biocreative2012";
		//String candidate = "the cat sat on the mat";
		//	String reference = "the cat was sat on the mat";
		//LOGGER.debug(meteor(candidate,reference,1,1));
		//String resulttable = ApplicationUtilities.getProperty("table.output");
		//long startTime = System.currentTimeMillis();

		//String resulttable = "test_equivalent";
		//String goldstandard = "goldstandard";
		//EQPerformanceEvaluation pe = new EQPerformanceEvaluation(database, resulttable, goldstandard,"evaluationrecords", "test_is_equivalent");		
		//pe.evaluate();


		/*intercurator comparison
		String database = "charaparsereval2013";
		String[] resulttable = new String[]{"naive_38484", "naive_38484", "naive_40674",
				"knowledge_40716", "knowledge_40716","knowledge_40717"};
		String[] goldstandard =  new String[]{"naive_40674","naive_40676", "naive_40676",
				"knowledge_40717", "knowledge_40718","knowledge_40718"};
		String[] setting = new String[]{"38484_40674", "38484_40676","40674_40676",
				"40716_40717","40716_40718","40717_40718" };

		for(int i = 1; i<6; i++){
			EQPerformanceEvaluation pe = new EQPerformanceEvaluation(database, resulttable[i], goldstandard[i],"evaluationrecords", setting[i]);		
			pe.evaluate();
		}*/

		EQPerformanceEvaluation pe = new EQPerformanceEvaluation("charaparsereval2013", "xml2eq_best", "knowledge1","evaluationrecords", "debug");		
		pe.evaluate();



		//long stopTime = System.currentTimeMillis();
		//System.out.println("Elapsed time was " + (stopTime - startTime)/60000f + " minutes.");


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
		//    LOGGER.debug("final score====="+replaceSubString(candidate,reference,temp,equivalence));
		//    candidate= candidate.replace(" ", "");
		//    reference = reference.replace(" ", "");

	}




}
