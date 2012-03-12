package fna.parsing.character;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.DeHyphenizerCorrected;
import fna.parsing.MainForm;
/**
 * compare learned terms with what is in glossary, produce a report
 * @author hongcui
 *
 */

public class LearnedTermsReport {
	static private String gtablename = "fnaglossary";
	static private String stablename = "learnedstates";
	static private String otablename1 = "sentence";
	static private String otablename2 = "wordpos";
	static private String otablename = "learnedstructures";
	static private String gstablename = "glossstructures";
	private String database;
	private static final Logger LOGGER = Logger.getLogger(LearnedTermsReport.class);
	static private Connection conn = null;
	//static private String username = ApplicationUtilities.getProperty("database.username");
	//static private String password = ApplicationUtilities.getProperty("database.password");
	private ArrayList<String> overlappedstructures = new ArrayList<String>();
	private ArrayList<String> newstructures = new ArrayList<String>();
	private ArrayList<String> modifiedstructures = new ArrayList<String>();
	private ArrayList<String> overlappedstates = new ArrayList<String>();
	private ArrayList<String> newstates = new ArrayList<String>();
	private ArrayList<String> modifiedstates = new ArrayList<String>();
	//private ArrayList unusedstructures = new ArrayList();
	//private ArrayList unusedstates = new ArrayList();
	private HashSet<String> learnedstructures = new HashSet<String>();
	private HashSet<String> learnedstates = new HashSet<String>();
	private Hashtable<String, String> donestates = new Hashtable<String, String>();
	
	
	
	
	public LearnedTermsReport(String database) {
		//check if fnaglossary and learnedstates tables exist
		this.database = database;
		boolean g = false;
		boolean s = false;
		boolean o1 = false;
		boolean o2 = false;
		try{
			if(conn == null){
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("show tables");
			while(rs.next()){
				String tablename = rs.getString(1);
				if(tablename.compareTo(stablename) == 0){
					s = true;
				}
				if(tablename.compareTo(gtablename) == 0){
					g = true;
				}
				if(tablename.compareTo(otablename1) == 0){
					o1 = true;
				}
				if(tablename.compareTo(otablename2) == 0){
					o2 = true;
				}
			}
			if(!s){
				System.err.println("Learned state table does not exist! Program exists.");
				System.exit(1);
			}
			if(!g){
				System.err.println("Glossary table does not exist! Program exists.");
				System.exit(1);
			}
			if(!o1){
				System.err.println("Learned organ table does not exist! Program exists.");
				System.exit(1);
			}
			if(!o2){
				System.err.println("Learned pos table does not exist! Program exists.");
				System.exit(1);
			}
			//create a table holding singular structure terms from the glossary
			createGlossStructureTable();
			//make another table with learned singular organ names.
			createLearnedStructureTable();
			DeHyphenizerCorrected dh = new DeHyphenizerCorrected(ApplicationUtilities.getProperty("database.name"), 
					otablename, "structure", null, "_", MainForm.dataPrefixCombo.getText().replaceAll("-", "_"), null);//TODO: replace last null with glossary
			dh.deHyphen();
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport constructor", e);
			e.printStackTrace();
		}
	}
	
	
	public String report(){
		StringBuffer sb = new StringBuffer();
		String ls = System.getProperty("line.separator");
		//Glossary
		sb.append("Comparison between FNA Glossary and Learned Terms"+ls);
		sb.append("Note: in the report, structure is defined to include any terms in either of the following categories: 'STRUCTURE / SUBSTANCE','STRUCTURE', 'CHARACTER', 'FEATURE', 'SUBSTANCE', 'PLANT'"+ls);
		sb.append("FNA Glossary Info:"+ls);
		sb.append("FNA Glossary All Structure Count: "+getAllStructureCount()+ls);
		/*unusedStructures();
		sb.append("\t FNA Glossary Structures NOT Seen in Text: "+unusedstructures.size());
		Iterator it = unusedstructures.iterator();
		while(it.hasNext()){
			String name = (String) it.next();
			sb.append("\t\t "+name);
		}*/
		/*sb.append("FNA Glossary All States Count: "+ (getAllStateCount()-learnedstates));
		unusedStates();
		sb.append("\t FNA Glossary States NOT Seen in Text: "+unusedstates.size());
		Iterator it = unusedstates.iterator();
		while(it.hasNext()){
			String name = (String) it.next();
			sb.append("\t\t "+name);
		}*/
		sb.append("FNA Glossary All Character Count: "+getAllCharacterCount()+ls);
		//organ/structure names
		sb.append(ls+":::::::::::::::::::::::::::::::::::::"+ls);
		sb.append("Structures Learned from "+database+":"+ls);
		sb.append("Learned Structure Count: "+getLearnedStructuresCount()+ls);
		compareStructureTerms();
		sb.append("\t Learned Structure Names Overlap with Glossary: "+this.overlappedstructures.size()+ls);
		sb.append("\t Learned Modified Structure Names: "+this.modifiedstructures.size()+ls);
		Iterator<String> it = modifiedstructures.iterator();
		while(it.hasNext()){
			String name = (String) it.next();
			sb.append("\t\t "+name+ls);
		}
		sb.append("\t Learned Structure Names NOT in Glossary: "+this.newstructures.size()+ls);
		it = newstructures.iterator();
		while(it.hasNext()){
			String name = (String) it.next();
			sb.append("\t\t "+name+ls);
		}
		//character states
		
		sb.append(ls+":::::::::::::::::::::::::::::::::::::"+ls);
		sb.append("States Learned from "+database+":"+ls);
		sb.append("Learned States Count: "+getLearnedStatesCount()+ls);
		compareStateTerms();
		sb.append("\t Learned State Names Overlap with Glossary: "+this.overlappedstates.size()+ls);
		sb.append("\t Learned Modified State Names: "+this.modifiedstates.size()+ls);
		it = modifiedstates.iterator();
		while(it.hasNext()){
			String name = (String) it.next();
			sb.append("\t\t "+name+ls);
		}
		sb.append("\t Learned State Names NOT in Glossary: "+this.newstates.size()+ls);
		it = newstates.iterator();
		while(it.hasNext()){
			String name = (String) it.next();
			sb.append("\t\t "+name+ls);
		}
		statesAssignedCharacters();
		sb.append("\t Learned State Assigned Characters: "+this.donestates.size()+ls);
		Enumeration<String> en = donestates.keys();
		while(en.hasMoreElements()){
			String name = (String) en.nextElement();
			String chara = (String) donestates.get(name);
			sb.append("\t\t "+name+ " is a type of "+chara+ls);
		}
		return sb.toString();
	}
	private void createGlossStructureTable(){
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+LearnedTermsReport.gstablename);
			stmt.execute("create table if not exists "+LearnedTermsReport.gstablename+" as select term from "+gtablename+" where category in ('STRUCTURE / SUBSTANCE','STRUCTURE', 'CHARACTER', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative') and status !='learned' and term not in (select distinct term2 from termforms where type ='pl')");
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport createGlossStructureTable", e);
			e.printStackTrace();
		}
	}
	/**
	 * 
	 */
	private void createLearnedStructureTable(){
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("create table if not exists "+LearnedTermsReport.otablename +"(structure varchar(100))");
			stmt.execute("delete from "+LearnedTermsReport.otablename);
			ResultSet rs = stmt.executeQuery("select distinct modifier, tag from "+LearnedTermsReport.otablename1);
			while(rs.next()){
				if(rs.getString("tag")!=null && !rs.getString("tag").equals("unknown")){
					String modifier = rs.getString("modifier") == null || rs.getString("modifier").equals("NULL")? "" : rs.getString("modifier");
					String tag = (modifier+" "+rs.getString("tag")).trim();
					Statement stmt1 = conn.createStatement();
					stmt1.execute("insert into "+LearnedTermsReport.otablename +" values ('"+tag+"')");
				}
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport createLearnedStructureTable", e);
			e.printStackTrace();
		}
	}

	
	@SuppressWarnings("unused")
	private ArrayList<String> unusedStructures(){
		ArrayList<String> unused = new ArrayList<String>();
		ArrayList<String> sents = new ArrayList<String>();
		try{
			Statement stmt = conn.createStatement();
			String query = "select sentence from "+otablename1;
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				sents.add(rs.getString("sentence"));
			}
			
			query = "select distinct term from "+gtablename +" where category in ('STRUCTURE / SUBSTANCE','STRUCTURE', 'CHARACTER', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative')";
			rs = stmt.executeQuery(query);
			while(rs.next()){
				String term = rs.getString("term");
				boolean used = false;
				Iterator<String> it = sents.iterator();
				while(it.hasNext()){//TODO: match singular with pl. 
					String sent = ((String) it.next()).toLowerCase();
					if(sent.indexOf(term)>=0){
						used = true;
						continue;
					}
				}
				if(!used){
					unused.add(term);
				}
			}
			int size = unused.size();
			HashSet<String> toremove = new HashSet<String>();
			for(int i = 0; i<size; i++){
				String term = (String)unused.get(i);
				rs = stmt.executeQuery("select term from "+gtablename+" where definition in (select definition from "+gtablename+" where term = '"+term+"')");
				boolean used = false;
				ArrayList<String> terms = new ArrayList<String>();
				while(rs.next()){
					String tprime = rs.getString("term").trim();
					terms.add(tprime);
					if(term.compareTo(tprime)!= 0){
						if(!unused.contains(tprime)){
							used = true;
						}
					}
				}
				terms.remove(term);
				if(used){
					toremove.add(term);
					toremove.addAll(terms);
				}else{
					toremove.addAll(terms);
				}
			}
			unused.removeAll(toremove);
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport unusedStructures", e);
			e.printStackTrace();
		}
		return unused;
	}
	
	@SuppressWarnings("unused")
	private ArrayList<String> unusedStates(){
		ArrayList<String> unused = new ArrayList<String>();
		ArrayList<String> sents = new ArrayList<String>();
		try{
			Statement stmt = conn.createStatement();
			String query = "select sentence from "+otablename1;
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				sents.add(rs.getString("sentence"));
			}
			
			query = "select distinct term from "+gtablename +" where category NOT in ('STRUCTURE / SUBSTANCE','STRUCTURE', 'CHARACTER', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative')";
			rs = stmt.executeQuery(query);
			while(rs.next()){
				String term = rs.getString("term");
				boolean used = false;
				Iterator<String> it = sents.iterator();
				while(it.hasNext()){
					String sent = ((String) it.next()).toLowerCase();
					if(sent.indexOf(term)>=0){
						used = true;
						continue;
					}
				}
				if(!used){
					unused.add(term);
				}
			}
			
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport unusedStates", e);
			e.printStackTrace();
		}
		return unused;
	}
	
	private void statesAssignedCharacters(){
		try{
			Statement stmt = conn.createStatement();
			String query = "select term, category from "+gtablename +" where status ='learned'";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				donestates.put(rs.getString("term"), rs.getString("category"));
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport statesAssignedCharacters", e);
			e.printStackTrace();
		}
	}
	
	private void compareStructureTerms(){
		Iterator<String> it = learnedstructures.iterator();
		while(it.hasNext()){
			String name = (String)it.next();
			String[] parts = name.split(" ");
			if(matchInGlossStructure(name)){
				this.overlappedstructures.add(name);
			}else if(findInGlossStructure(parts[parts.length-1])){
				this.modifiedstructures.add(name);
			}else{
				this.newstructures.add(name);
			}
		}
	}
	private void compareStateTerms(){
		Iterator<String> it = learnedstates.iterator();
		while(it.hasNext()){
			String name = (String)it.next();
			String[] parts = name.split(" ");
			if(matchInGlossStates(name)){
				this.overlappedstates.add(name);
			}else if(findInGlossStates(parts[parts.length-1])){
				this.modifiedstates.add(name);
			}else{
				this.newstates.add(name);
			}
		}
	}
	private int getLearnedStructuresCount(){
		try{
			Statement stmt = conn.createStatement();
			String query = "select structure from "+otablename;
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
			    String structure = rs.getString("structure");
				learnedstructures.add(structure.trim());
			}
			/*query = "select distinct word from "+otablename2+" where pos='p' or pos='s'";
			rs = stmt.executeQuery(query);
			while(rs.next()){
				learnedstructures.add(rs.getString("word").trim());
			}*/
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport getLearnedStructuresCount", e);
			e.printStackTrace();
		}
		return learnedstructures.size();
	}
	
	private int getLearnedStatesCount(){
		try{
			Statement stmt = conn.createStatement();
			String query = "select distinct state from "+stablename;
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				learnedstates.add(rs.getString("state"));
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport getLearnedStatesCount", e);
			e.printStackTrace();
		}
		return learnedstates.size();
	}
	
	private int getAllCharacterCount(){
		try{
			Statement stmt = conn.createStatement();
			String query = "select count(distinct category) from "+gtablename;
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				return rs.getInt(1);
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport getAllCharacterCount", e);
			e.printStackTrace();
		}
		return -1;
	}
	//state
	@SuppressWarnings("unused")
	private int getAllStateCount(){
		
		try{
			Statement stmt = conn.createStatement();
			String query = "select count(term) from "+gtablename +" where category not in ('STRUCTURE / SUBSTANCE','STRUCTURE', 'CHARACTER', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative')";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				return rs.getInt(1);
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport getAllStateCount", e);
			e.printStackTrace();
		}
		return -1;
	}
	
		
	@SuppressWarnings("unused")
	private int getAllStateCount(String status){
		try{
			Statement stmt = conn.createStatement();
			String query = "select count(term) from "+gtablename +" where category not in ('STRUCTURE / SUBSTANCE','STRUCTURE', 'CHARACTER', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative') and status ='"+status+"'";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				return rs.getInt(1);
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport getAllStateCount", e);
			e.printStackTrace();
		}
		return -1;
	}
	
	private boolean matchInGlossStates(String term){ //match whole term
		boolean match = false;
		try{
			Statement stmt = conn.createStatement();
			String query = "select term from "+gtablename +" where term ='"+term+"' and category not in ('STRUCTURE / SUBSTANCE','STRUCTURE', 'CHARACTER', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative') and status !='learned'";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				match = true;
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport matchInGlossStates", e);
			e.printStackTrace();
		}
		return match;
	}
	
	private boolean findInGlossStates(String main){//match the main word in the term
		boolean find = false;
		try{
			Statement stmt = conn.createStatement();
			String query = "select term from "+gtablename +" where (term like '% "+main+"' or term = '"+main+"') and category not in ('STRUCTURE / SUBSTANCE','STRUCTURE', 'CHARACTER', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative') and status !='learned'";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				find = true;
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport findInGlossStates", e);
			e.printStackTrace();
		}
		return find;
	}
	
	//structure
	private int getAllStructureCount(){
		
		try{
			Statement stmt = conn.createStatement();
			String query = "select count(term) from "+gstablename;
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				return rs.getInt(1);
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport getAllStructureCount", e);
			e.printStackTrace();
		}
		return -1;
	}

	private boolean matchInGlossStructure(String term){ //match whole term: single to single
		boolean match = false;
		try{
			Statement stmt = conn.createStatement();
			String query = "select term from "+gstablename +" where term ='"+term+"'";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				match = true;
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport matchInGlossStructure", e);
			e.printStackTrace();
		}
		return match;
	}
	
	private boolean findInGlossStructure(String main){//match the main word in the term
		boolean find = false;
		try{
			Statement stmt = conn.createStatement();
			String query = "select term from "+gstablename +" where term like '% "+main+"' or term = '"+main+"'";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				find = true;
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport findInGlossStructure", e);
			e.printStackTrace();
		}
		return find;
	}
	
	@SuppressWarnings("unused")
	private boolean stringMatchInGloss(String term){
		boolean find = false;
		try{
			Statement stmt = conn.createStatement();
			String query = "select term from "+gtablename +" where term like '%"+term+"%'";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				find = true;
			}
		}catch(Exception e){
			LOGGER.error("Exception in LearnedTermsReport stringMatchInGloss", e);
			e.printStackTrace();
		}
		return find;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LearnedTermsReport ltr = new LearnedTermsReport("fnav5_corpus");
		ltr.report();
	}

}
