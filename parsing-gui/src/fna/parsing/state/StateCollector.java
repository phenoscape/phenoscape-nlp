package fna.parsing.state;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;

/**
 * first run dehypenizer, then run unsupervised.pl, 
 * run on untagged sentences/originalsent
 */

/**
 * this class should only gather state info, to be used as a base for further reasoning
 * Should not involve any glossary or other ontology which should be part of further reasoning.
 * Use simple, list, to, tolist patterns to gather states, and record the cooccurrence of the states in a matrix easily be used for further reasoning.
 * The matrix holds co-occurrence scores for a pair of states
 * 
 */
/**
 * Changes:
 * Patterns: remove "stop" from start, keep "stop" in the end [when to remove after stop? consider states learned vs. displayed. learned should include only the key term, displayed needs to show constraints and modifiers.]
 *           to make sure states extracted are of the organ, not related parts.
 * Stop: add new stop words:
 * a|above|after|all|almost|along|amp|an|and|are|as|at
 * |be|because|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|
 * |each|even|from|has|had|have|here|how|if|in|into|is|it|its
 * |less|may|might|more|most|much|near|not|of|often|on|over|should|so|some|sometimes|should
 * |than|that|the|then|there|these|this|those|toward|towards
 * |was|well|were|what|when|why|with|without|would
 * 
 * |few|frequently
 * |occasionally|often|rarely|somewhat|throughout|very";
 * 
 * False to-patterns:"reduced to", "to form", "appressed to", "in contrast to", "similar to"
 *     "confined to", "equal to", "perpendicular to", "dissimilar to","lobed to", "divided to", "invisible to"
 * 	   "adherent to", "according to", "proximal to"/"distal to", "to touch", "fused to", "attached to"
 * 	   "formed by", "axillary to", "back to back", "articulated to"
 * 
 * In Bootstrap: allow different characters in a group. e.g lacking or yellowish to light orangeish
 * 
 * numbers: convert to NUM with state number.
 * 
 * comparative: > equal to or slightly shorter than <
 * special -ly words: mealy, scaly, prickly,
 * 
 * adv to/or adv adj patterns: sparsely to much branched => record only adj branched.
 * 
 * OR patterns: or not: scabrous or not. =>record only adj scabrous.
 *              more or less
 * ca.: , to ca. x m times or just ca.
 * 
 * list pattern: , "or meeting <"=>match single seg, most cases are fine, this one is bad.
 *
 *preposition phrases: on rock
 *
 *tag sentence: should organ names only be tagged when they appear at the beginning of a sentence? e.g. , rounded or with single <groove> [ or change simple pattern?]
 *markup character: may need to merge saved stategroups and checked states. eg. > entire or with 3 broad ,
 */

public class StateCollector  {
	static protected Connection conn = null;
	//static protected String database = null;
	//static protected String username = "termsuser";
	//static protected String password = "termspassword";
	static protected String word = "(?:[\\w_]+\\s)";
	static public String stop ="a|about|above|across|after|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|for|from|had|has|have|hence|here|how|if|in|into|inside|inward|is|it|its|may|might|more|most|near|no|not|of|off|on|onto|or|out|outside|outward|over|should|so|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|was|were|what|when|where|which|why|with|within|without|would";
	//static public String stop ="a|above|above|across|after|along|also|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|each|even|few|frequently|from|had|has|have|here|how|if|in|into|is|it|its|less|may|might|more|most|much|near|not|occasionally|of|off|often|on|onto|or|over|rarely|should|so|some|sometimes|somewhat|soon|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|very|was|well|were|what|when|where|which|why|with|without|would";
	static protected String tophrases="articulated to|adnate to|connate to|to ca|reduced to|to form|appressed to|in contrast to|similar to|confined to|equal to|perpendicular to|dissimilar to|lobed to|divided to|invisible to|adherent to|according to|proximal to|distal to|to touch|fused to|attached to|axillary to|back to back|restricted to|ankylosed to|anterior to|attaching to|close to|complementary to|connected to|continuing to|difficult to|extending to|extended to|extend to|joined to|leading to|limited to|posterior to|prior to|tending to|tend to|tends to|tendency to|up to|widening to";
	static protected String orphrases="more or less";
	static protected String simple = "((?:(?:^|,|>) "+word+"))or ("+word+"{1,}?)"+"(?=$|,|;|:|\\.|<|\\b(?:"+stop+")\\b)";     //a or b
	static protected String list = "((?:(?:^|,|>) "+word+")*), or ("+word+"{1,}?)"+"(?=$|,|;|:|\\.|<|\\b(?:"+stop+")\\b)";   //a, b, c, or e f g
	static protected String to =  "((?:(?:^|,|>) (?:[_a-z]+\\s)))to ((?:[_a-z]+\\s){1,}?)"+"(?=$|,|;|:|\\.|<|\\b(?:"+stop+")\\b)";
	static protected String tolist ="((?:(?:^|,|>) (?:[_a-z]+\\s))*), to ((?:[_a-z]+\\s){1,}?)"+"(?=$|,|;|:|\\.|<|\\b(?:"+stop+")\\b)";

	protected StateMatrix statematrix = null;
	protected Hashtable<String, String> sentences = null;
	protected String tableprefix = null;
	protected String glosstable = null;
	//protected String organnames = null;
    protected Display display;
    protected StyledText charLog;
	
	protected boolean marked = false;
	
	StateCollector(Connection conn, String tableprefix, String glosstable, Display display, StyledText charLog){
		this.statematrix = new StateMatrix(conn, tableprefix,glosstable);
		this.tableprefix = tableprefix;
		StateCollector.conn = conn;
		this.glosstable = glosstable;
		//this.database = database;
		//collect(database);
		this.display = display;
		this.charLog = charLog;
	}
	
	StateCollector(Connection conn, String tableprefix, ArrayList<String> knownstates, String glosstable, Display display, StyledText charLog){
		if(knownstates!=null){
			StateImporter si = new StateImporter(knownstates);
			this.statematrix = new StateMatrix(conn, tableprefix, si.getStates(),glosstable);
		}
		this.tableprefix = tableprefix;
		StateCollector.conn = conn;
		this.glosstable = glosstable;
		//this.database = database;
		//collect(database);
		this.display = display;
		this.charLog = charLog;
	}
	
	public void collect(){
		// read sentences in the database.sentencetable generated by unsupervised.pl
		// create statemartix	
		try{
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists "+this.tableprefix+"_learnedstates (state varchar(100) NOT NULL PRIMARY KEY, count int(4))");
				stmt.execute("delete from "+this.tableprefix+"_learnedstates");
				//stmt.execute("create table if not exists "+this.tableprefix+"_markedsentence (source varchar(100) NOT NULL PRIMARY KEY, markedsent text, rmarkedsent text)");
				//ResultSet rs = stmt.executeQuery("select * from "+this.tableprefix+"_markedsentence");
				//if(rs.next()){this.marked = true;}
				stmt.execute("update "+this.tableprefix+"_sentence set charsegment =''");
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(StateCollector.conn, this.tableprefix, this.glosstable, true, display, charLog);//tag organ names
		try {
			this.sentences = sosm.markSentences();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		parseSentences();//create StateGroups 
		//System.out.println(statematrix.toString());
	}
	
	/**
	 * parse or and to patterns, 
	 * save each pattern as a group of state (StateGroup)
	 * check for characters for a state
	 * save patterns in a sorted collection 
	 */
	protected void parseSentences(){
		Enumeration<String> en = sentences.keys();
		this.showOutputMessage("System is parsing sentences for character terms ...");
		while(en.hasMoreElements()){
			String source = en.nextElement();
			String taggedsent = (String)sentences.get(source);
			parseSentence(source, taggedsent);
		}
	}
	
	protected void parseSentence(String source, String sent){
		boolean match = false;
		do{
			match = false;
			String copy = sent;
			//sent = doSynonyms(source, sent);
			sent = doSimple(source, sent);
			sent = doList(source, sent);
			sent = doTo(source,sent);
			sent = doToList(source, sent);
			if(copy.compareTo(sent) != 0){
				match = true;
			}
		}while(match);
	}

	protected String doToList(String source, String sent){
		Pattern tolistp = Pattern.compile(StateCollector.tolist);
		Matcher m = tolistp.matcher(sent);
		if(m.find()){
			String seg = sent.substring(m.start(), m.end());
			System.out.println( "\t"+seg);
		    String t1 = normalize(m.group(1));
		    String t2 = normalize(m.group(2));
		    if(isValidStateString(t2) && isValidStateString(t1)){
			    String [] terms = t1.split("\\s*,\\s*");
			    List<String> list = Arrays.asList(terms);
			    ArrayList<String> alist = new ArrayList<String>(list);
			    alist.add(t2);
			    for(int i = 0; i<alist.size(); i++){
			    	for(int j = i+1; j<alist.size(); j++){
			    		int score = i==0? -1 : 1; //absent or erect
			    		State s1 = statematrix.getStateByName(alist.get(i));
			    		State s2 = statematrix.getStateByName(alist.get(j));
			    		s1 = s1 == null? new State(alist.get(i)) : s1;
			    		s2 = s2 == null? new State(alist.get(j)) : s2;
			    		statematrix.addPair(s1, s2, score, source);
			    	}
			    }
			    System.out.println(t1+" and "+t2+" are in the same group [tolist] in ["+sent+"]\n");
		    }
		    sent = sent.replaceFirst(StateCollector.tolist, "");
		}
	    return sent;
	}
	
	protected String doTo(String source, String sent){
		Pattern top = Pattern.compile(StateCollector.to);
		Matcher m = top.matcher(sent);
		if(m.find()){
			String seg = sent.substring(m.start(), m.end());
			System.out.println( "\t"+seg);
			String t1 = normalize(m.group(1));
			String t2 = normalize(m.group(2));
			if(isValidStateString(t2) && isValidStateString(t1)){/**TODO: move this check to function group**/
				ArrayList<String> list = new ArrayList<String>();
				list.add(t1);
				System.out.print("["+t1+"] ");
				String[] t2s = t2.split("\\b(to|or)\\b");
				for(int i = 0; i<t2s.length; i++){
					list.add(normalize(t2s[i]));
					System.out.print("["+t2s[i]+"] ");
				}
				System.out.println(" are in the same group [to] in ["+sent+"]\n");
				for(int i = 0; i<list.size(); i++){
			    	for(int j = i+1; j<list.size(); j++){
			    		int score = i==0? -1 : 1; //absent or erect
			    		State s1 = statematrix.getStateByName(list.get(i));
			    		State s2 = statematrix.getStateByName(list.get(j));
			    		s1 = s1 == null? new State(list.get(i)) : s1;
			    		s2 = s2 == null? new State(list.get(j)) : s2;
			    		statematrix.addPair(s1, s2, score, source);
			    		//statematrix.addPair(new State(list.get(i)), new State(list.get(j)), score);
			    	}
			    }
			
			}
			sent = sent.replaceFirst(StateCollector.to, "");
		}
	    return sent;
	}
	
	protected String doList(String source, String sent){

		Pattern listp = Pattern.compile(StateCollector.list);
		Matcher m = listp.matcher(sent);
		if(m.find()){
			String seg = sent.substring(m.start(), m.end());
			System.out.println( "\t"+seg);
			String t1 = m.group(1);
			String t2 = m.group(2);
			if(isValidStateString(t2) && isValidStateString(t1)){
		        String [] terms = t1.split("\\s*,\\s*");
		        List<String> list = Arrays.asList(terms);
		        ArrayList<String> alist = new ArrayList<String>(list);
				alist.add(t2);
				for(int i = 0; i < alist.size(); i++){
					alist.set(i, normalize((String)alist.get(i)));
				}
				System.out.println(t1+" and "+t2+" are in the same group [list] in ["+sent+"]\n");

				for(int i = 0; i<alist.size(); i++){
			    	for(int j = i+1; j<alist.size(); j++){
			    		int score = i==0? -1 : 1; //absent or erect
			    		State s1 = statematrix.getStateByName(alist.get(i));
			    		State s2 = statematrix.getStateByName(alist.get(j));
			    		s1 = s1 == null? new State(alist.get(i)) : s1;
			    		s2 = s2 == null? new State(alist.get(j)) : s2;
			    		statematrix.addPair(s1, s2, score, source);
			    		//statematrix.addPair(new State(alist.get(i)), new State(alist.get(j)), score);
			    	}
			    }
			}
			sent = sent.replaceFirst(StateCollector.list, "");
		}
	    return sent;
	}
	
	/**
	 * watch out for "internodes glabrous or midstem ones slightly scabrous ."
	 * @param sentid
	 * @param sent
	 * @return
	 */
	protected String doSimple(String source, String sent){
		Pattern simplep = Pattern.compile(StateCollector.simple);
		Matcher m = simplep.matcher(sent);
		if(m.find()){
			String seg = sent.substring(m.start(), m.end());
			System.out.println("\t"+seg);
			String t1 = normalize(m.group(1));
			String t2 = normalize(m.group(2));
			if(isValidStateString(t2) && isValidStateString(t1)){/*TODO move this checkpoint to function group*/
				ArrayList<String> list = new ArrayList<String>();
				list.add(t1);
				System.out.print("["+t1+"] "); 
				String[] t2s = t2.split("\\b(to|or)\\b");
				for(int i = 0; i<t2s.length; i++){
					list.add(normalize(t2s[i]));
					System.out.print("["+t2s[i]+"] "); 
				}
				for(int i = 0; i<list.size(); i++){
			    	for(int j = i+1; j<list.size(); j++){
			    		int score = i==0? -1 : 1; //absent or erect
			    		State s1 = statematrix.getStateByName(list.get(i));
			    		State s2 = statematrix.getStateByName(list.get(j));
			    		s1 = s1 == null? new State(list.get(i)) : s1;
			    		s2 = s2 == null? new State(list.get(j)) : s2;
			    		statematrix.addPair(s1, s2, score, source);
			    		//statematrix.addPair(new State(list.get(i)), new State(list.get(j)), score);
			    	}
			    }
				System.out.println(" are in the same group [simple] in ["+sent+"]\n");
			}
	        sent = sent.replaceFirst(StateCollector.simple, "");
		}
	    return sent;
	}
	
	/**
	 * a state is of typical length if each section of the state separated by or/and/to contains no more than two words
	 * @param state
	 * @return
	 */
	protected boolean isValidStateString(String statestring) {
		/*if(statestring.matches(".*?\\b("+this.organnames+")\\b.*")){
			return false;
		}*/
		if(statestring.matches("\\d")){
			return false;
		}
		String[] sections = statestring.split("\\b(,|or|and|to)\\b");
		for(int i = 0; i < sections.length; i++){
			if(sections[i].trim().split("\\s+").length > 2){
				return false;
			}
		}
		return true;
	}
	/*TODO
	 * protected String doSynonyms(int sentid, String sent){
		Pattern synonymsp = Pattern.compile(CharacterLearner.synonyms);
		Matcher m = synonymsp.matcher(sent);
		if(m.find()){
			String seg = sent.substring(m.start(), m.end());
			System.out.println("\t"+seg);
			String save = m.group(2);
			String t1 = normalize(m.group(1));
			String t2 = normalize(m.group(3));
			String[] terms = t2.split("\\s*(\\bor\\b|,)\\s*");
			List<String> list = Arrays.asList(terms);
			list.add(t1);
			//if(list.size() > 1){
				group(list, sentid, seg);
				System.out.println("[t1] and [t2] are in the same group [syn]");
			//}else{
				//System.out.println("[t1] and [t2] were not put in the same group [syn]");
			//}
			save = save.replaceAll("\\[", "\\[").replaceAll("\\]", "\\]");
			sent = sent.replaceFirst(" "+save, "");
		}
		return sent;
	}*/
	

	protected String normalize(String sent){
		if(sent == null){return sent;}
		sent = sent.replaceAll("[<>,;.]", "")/*.replaceAll("\\bwith\\b", " WITH ")*/
		.replaceAll("\\b("+stop+")\\b", "")/*.replaceAll(" WITH ", " with ")*/.replaceAll("\\d+", "").replaceAll("\\b[_a-z]*?ly\\b","")
		.replaceFirst("^to_", "to").replaceFirst("^or_", "or")./*replaceAll("_", " ").*/replaceAll("\\s+", " ").replaceFirst("^\\s+", "").replaceFirst("\\s+$", "");
		return sent.trim().toLowerCase();
	}
	
	/*
	 * NOT USED 
	 protected String collectStateNames(){
		StringBuffer tags = new StringBuffer();
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct state from learnedstates");
			while(rs.next()){
				String tag = rs.getString("state");
				if(tag == null){continue;}
				tags.append(tag+"|");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return tags.toString()+glossary.getAllCharacters();
	}*/
	

	
/**
 * collect tag names (sing. and pl. forms)
	mark sentences one by one using tags
	tag only the starting words of a sentence.
	convert numbers to NUM
 * @param sentencetable
 */
/*
	protected void mark() {
	try{
		String organnames = collectOrganNames();
		Pattern tagsp = Pattern.compile("(.*?)\\b("+organnames+")\\b(.*)", Pattern.CASE_INSENSITIVE);
		//now mark sentence one by one, add marked sentences in this.sentences
		//break sentence into meaningful clauses (each with a marked subject)
		//create a clause table to save the clauses
		//tracking the relation between filenames and clauses, saving this info in 
		//a new table fileclauselink (filename, endindex of the last clause in the file).
		
		Statement stmt = conn.createStatement();
		String[] tos = this.tophrases.split("\\|");
		String[] ors = this.orphrases.split("\\|");
		
		ResultSet	rs = stmt.executeQuery("select source, tag, modifier, originalsent from sentence");
		while(rs.next()){
			String source = (String)rs.getString("source");
			String sent = (String)rs.getString("originalsent"); 
			//sent = sent.replaceAll(this.num, "NUM "); //all numbers => NUM
			if(sent.matches(".*?("+this.tophrases+").*")){
				sent = hide(tos, sent, "*");
			}
			if(sent.matches(".*?("+this.orphrases+").*")){
				sent = hide(ors, sent, "*");
			}

			String taggedsent = "";
			Matcher m = tagsp.matcher(sent);
			while(m.matches()){
				taggedsent += m.group(1)+"<"+m.group(2)+">";
				sent = m.group(3);
				m = tagsp.matcher(sent);
		    }
			taggedsent +=sent;
			// seg clauses should be done later, after the learning of states.
			//Pattern p = Pattern.compile(", (\\w+)? ?(<.*?>)");//the word after , should not be connectors such as "or"
			//Matcher m2 = p.matcher(taggedsent);
			//int start = 0;
			//while(m2.find()){
			//	if(m2.group(1)==null || m2.group(1).compareTo("or") != 0){//the word after , should not be connectors such as "or"
			//		int end = m2.start(); //this ends a clause
			//		String taggedclause = taggedsent.substring(start, end+1);
			//		addClause(sentid, sentid+offset, tag, modifier, taggedclause, false);
			//		offset++;
			//		start = end+1;
			//		modifier = m2.group(1);
			//		tag = m2.group(2).replaceAll("[<>]", "");
			//	}
			//}
			//String taggedclause = taggedsent.substring(start);
			//addClause(sentid, sentid+offset, tag, modifier, taggedclause, true);
			
			Statement stmt1 = conn.createStatement();
			stmt1.executeQuery("insert into markedsentence values('"+source+"', '"+taggedsent+"')");
			sentences.put(source, taggedsent); //do this in addClause
		}
	}catch (Exception e){
		e.printStackTrace();
	}
}
*/

	
	static public String hide(String[] phrases, String str, String symbol){
		for(int i = 0; i < phrases.length; i++){
			String hidden = phrases[i].replaceAll("\\s+", symbol);
			str = str.replaceAll(phrases[i], hidden);
		}
		return str;
	}

	
    protected void resetOutputMessage() {
		display.syncExec(new Runnable() {
			public void run() {
				charLog.setText("");
			}
		});
	}
    
	protected void showOutputMessage(final String message) {
		display.syncExec(new Runnable() {
			public void run() {
				charLog.append(message+"\n");
			}
		});
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//StateCollector sc = new StateCollector("test_asist09ont");
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
		//StateCollector sc = new StateCollector(conn, "fnav19", "fnaglossaryfixed");
		//sc.collect();
	}

}
