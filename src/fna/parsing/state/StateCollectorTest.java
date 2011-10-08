package fna.parsing.state;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * DO NOT treat a list of states, such as imbricate, lanceolate or ovate because it is common for an author to enumerate different characters in a list
 * Treat only states connected by or/to, such as /{elliptic} to {oblong}/ or {ovate}, {glabrous} or /{villous} to {tomentose}/, clasping or short_decurrent,  
 * Watch out for "adv or/to adv state" pattern, such as {thinly} to {densely} {arachnoid_tomentose}
 * Watch out for preposition to: reduced to
 * 
 * {distalmost} {linear} to {narrowly} {elliptic} , {bractlike} , {spinulose} to {irregularly} {dentate} or {shallowly} {lobed} .

 * 
 * @author hongcui
 *
 */
@SuppressWarnings({  "unused","static-access" })
public class StateCollectorTest extends StateCollector {
	private boolean filtered = false;
	private Hashtable<String, Boolean> checkedtermpairs = new Hashtable<String, Boolean>();
		
	public StateCollectorTest(Connection conn, String tableprefix, boolean filtered, String glosstable) {
		super(conn, tableprefix, glosstable);
		//statematrix.save2MySQL(database, "termsuser", "termspassword");
		this.filtered = filtered;
		
	}
	
	public StateCollectorTest(Connection conn, String tableprefix, ArrayList<String> knownstates, boolean filtered, String glosstable) {
		super(conn, tableprefix, knownstates, glosstable);
		this.filtered = filtered;
		
	}

	public void saveStates(){
		statematrix.save2MySQL(this.conn, this.tableprefix, "termsuser", "termspassword");
	}
	
	public int grouping4GraphML(){
		statematrix.Grouping();
		int countXMLFiles = statematrix.output2GraphML();
		return countXMLFiles;
	}
			
	/**
	 * rely on {c} /[o] and to/or 
	 * add to the statematrix
	 */
	protected void parseSentence(String source, String sent){
		String scopy = sent;
		sent = sent.replaceAll("\\}-\\{", "-").replaceAll("\\}-c-\\{", "-c-");
		Pattern p = Pattern.compile("\\b(to|or)\\b");
		Matcher m = p.matcher(sent);
		if(m.find()){
			System.out.println("from sent ["+source+"]:"+sent);
			
			//Pattern p1 = Pattern.compile("((?:\\{\\w+}\\s)+|\\s*(,|or|to)\\s*)+\\s*(to|or|nor)\\s*(?:\\{\\w+\\}\\s)+");
			Pattern p1 = Pattern.compile("(?:\\{[\\w-]+}\\s)+\\s*(or|to)\\s*(?:\\{[\\w-]+\\}\\s*)+"); //add - for {dark-c-brown}
			//Pattern p1 = Pattern.compile("(?:(?:\\{\\w+}\\s)+\\s*(or|to)\\s*)+(?:\\{\\w+\\}\\s*)+");
			Matcher m1 = p1.matcher(sent);
			while(m1.find()){
				String matched = sent.substring(m1.start(), m1.end());
				String mstring = matched;
				boolean endofseg = false;
				int end = m1.end() + 5 > sent.length()? sent.length() : m1.end()+5;
				String follow = sent.substring(m1.end(), end);
				if(follow.matches("\\s*[,;\\.:].*") ){
					endofseg = true;
				}
				matched = matched.toLowerCase();
				//sent = sent.substring(m1.end()); take from after (or|to) instead
				sent = sent.substring(m1.end(1)+1); //3 for "or|to "
				matched = matched.replaceFirst("^[\\s,]*", "").replaceAll("[{}]", "");
				matched = split(matched, endofseg).replaceAll("-c-", "-");
				if(matched.length() > 0 && ! mstring.matches(".*?(ed|ing)}.*? to .*")){ //ignore "reduced to", but take "reduced or"
					add2matrix(matched, source);
					System.out.println("\t====::"+matched); //deal with two "to"/"or" in one match: {distalmost} {linear} to {narrowly} {elliptic} , {bractlike} , {spinulose} to {irregularly} {dentate} or {shallowly} {lobed} .
				}
				m1 = p1.matcher(sent);
			}
		}
	return;	
	}
	
	/*1) thinly to/or densely arachnoid_tomentose} : leave this alone: no need to capture degrees.
	 *2) distalmost linear to/or narrowly elliptic
	 * @param if endofseg is true, take the last adj for the last segment
	 * @return
	 */
	private String split(String conjunction, boolean endofseg){
		String[] terms = conjunction.split("\\s+(to|or)\\s+");
		String csv = "";
		int count = 0;
		System.out.println("########### from :"+conjunction);
		
		int size = terms.length;
		int i = 0;
		//all but the last term: save the last non-adv word from each term
		for(i = 0; i < terms.length-1; i++){
			terms[i] = terms[i].trim();
			String[] parts = terms[i].split("\\s+");
			//if(parts.length > 1){
			//	System.out.println("########### from :"+conjunction);
			//}
			for(int j = parts.length-1; j >=0; j--){
				if(!isAdv(parts[j])){ //save the last non-adv word from each term
						csv += ","+parts[j];
						count++;
						break;
				}
			}
		}
		//the last term: save the first non-adv word
		String[] parts = terms[i].split("\\s+");
		if(!endofseg){
			for(int j = 0; j <parts.length; j++){
				if(!isAdv(parts[j])){ //save the first non-adv word for the last term
						csv += ","+parts[j];
						count++;
						break;
				}
			}
		}else{
			for(int j = parts.length-1; j >=0; j--){
				if(!isAdv(parts[j])){ //save the first non-adv word for the last term
						csv += ","+parts[j];
						count++;
						break;
				}
			}
		}
		if(count > 1){//at least two states in a conjunction
			csv = csv.replaceFirst("^[\\s,]*", "").replaceFirst("[\\s,]*$", "");
			return csv;
		}
		return "";
	}
	
	protected boolean isAdv(String word){
		//access WordNet for answer
		String wordc = word;
		
		word = word.replaceFirst("ly$", "");
		if(word.compareTo(wordc) != 0){
			WordNetWrapper wnw1 = new WordNetWrapper(word);
			WordNetWrapper wnw2 = new WordNetWrapper(word+"e");
			if(wnw1.isAdj() || wnw2.isAdv()){
				System.out.println(wordc + " is an adv");
				return true;
			}
		}
		
		WordNetWrapper wnw = new WordNetWrapper(wordc);
		//if(wnw.isAdv() && !wnw.isAdj()){
		if(wnw.mostlikelyPOS() !=null && wnw.mostlikelyPOS().compareTo("adv") == 0){
			System.out.println(word + " is an adv");
			return true;
		}
		
		
		return false;
	}
	
	/*
	 * refined is a list of format: a,b,c,d
	 */
	protected void add2matrix(String refined, String source){
		String[] alist = refined.split(",");
		for(int i = 0; i<alist.length; i++){
	    	for(int j = i+1; j<alist.length; j++){
	    		int score = 1; //absent or erect
	    		State s1 = statematrix.getStateByName(alist[i]);
	    		State s2 = statematrix.getStateByName(alist[j]);
	    		s1 = s1 == null? new State(alist[i]) : s1;
	    		s2 = s2 == null? new State(alist[j]) : s2;
	    		boolean add = true;
	    		if(this.filtered){
	    			add = notInGlossary(s1.getName(), s2.getName());
	    		}
	    		if(add && !s1.getName().matches("\\d+") && !s2.getName().matches("\\d+")){
	    			statematrix.addPair(s1, s2, score, source);
	    		}
	    	}
	    }	
	}
	/**
	 * 
	 * @param term1
	 * @param term2
	 * @return false iff term1 and term2's categories overlap in glossary.
	 */
	private boolean notInGlossary(String term1, String term2) {
		Boolean result = null;
		//check the cache
		result = this.checkedtermpairs.get(term1+"#"+term2);
		result = result == null? this.checkedtermpairs.get(term2+"#"+term1) : result;
		if(result==null){//not in cache
			try{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select * from "+super.glosstable+
				" where term ='"+term1+"' and category in (select category from "+super.glosstable+
				" where term='"+term2+"')");
				if(rs.next()){
					this.checkedtermpairs.put(term1+"#"+term2, new Boolean(false));
					return false;
				}
			}catch (Exception e){
				e.printStackTrace();
			}
			
		}else{
			return result.booleanValue();
		}		
		return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		/*StateCollectorTest sct = new StateCollectorTest("onto_foc_corpus"); //using learned semanticroles only
		sct.collect("onto_foc_corpus");
		sct.saveStates("onto_foc_corpus");
		*/
		//to use the result from unsupervisedclausemarkup, change wordpos table to wordroles (word, semanticroles) where semanticroles in (c, os, op)
		Connection conn = null;
		String database="";
		String username="";
		String password="";
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}	
		StateCollectorTest sct = new StateCollectorTest(conn, "fnav19", false, "fnaglossaryfixed"); /*using learned semanticroles only*/
		sct.collect();
		sct.saveStates();
		sct.grouping4GraphML();
	}

}
