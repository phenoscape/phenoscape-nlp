package fna.parsing.character;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.DeHyphenizerCorrected;
import fna.parsing.Learn2Parse;

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
 * 	   "formed by", "axillary to", "back to back"
 * 
 * In Bootstrap: allow different characters in a group. e.g lacking or yellowish to light orangish
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

public class CharacterLearner  implements Learn2Parse{
	
	private static final Logger LOGGER = Logger.getLogger(CharacterLearner.class);
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in CharacterLearner" + e);
			e.printStackTrace();
		}
	}

	static private Connection conn = null;
	static private String database = null;
	static private String word = "(?:[\\w_]+\\s)";
	static private String num = "\\d[^a-z]+"; //5 pairs. 0 . 5 - 3 mm
	static public String stop = "a|above|after|all|almost|along|amp|an|and|are|as|at|be|because|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|each|even|from|has|had|have|here|how|if|in|into|is|it|its|less|may|might|more|most|much|near|not|of|often|on|over|should|so|some|sometimes|should|than|that|the|then|there|these|this|those|toward|towards|was|well|were|what|when|why|with|without|would|few|frequently|occasionally|often|rarely|somewhat|throughout|very";
	static private String tophrases="connate to|to ca|reduced to|to form|appressed to|in contrast to|similar to|confined to|equal to|perpendicular to|dissimilar to|lobed to|divided to|invisible to|adherent to|according to|proximal to|distal to|to touch|fused to|attached to|axillary to|back to back";
	static private String orphrases="more or less";
	static private String synonyms ="(?:>|^|,|"+stop+") ("+word+"{1,3})(\\[ (.{1,30}) \\])";
	//static private String simple = "((?:(?:^|,|>|"+stop+") "+word+"))or ("+word+"{1,})";     //a or b
	static private String simple = "((?:(?:^|,|>) "+word+"))or ("+word+"{1,}?)"+"(?=$|,|;|:|\\.|<|"+stop+")";     //a or b
	//static private String simple = "((?:(?:^|,|>|"+stop+") "+word+"))or ("+word+"{1,}?)"+"(?:$|,|;|:|\\.|<|"+stop+")";     //a or b
	//static private String list = "((?:(?:^|,|>|"+stop+") "+word+")*), or ("+word+"{1,})";   //a, b, c, or e f g
	static private String list = "((?:(?:^|,|>) "+word+")*), or ("+word+"{1,}?)"+"(?=$|,|;|:|\\.|<|"+stop+")";   //a, b, c, or e f g
	//static private String list = "((?:(?:^|,|>|"+stop+") "+word+")*), or ("+word+"{1,}?)"+"(?:$|,|;|:|\\.|<|"+stop+")";   //a, b, c, or e f g
	//static private String to =  "((?:(?:^|,|>|"+stop+") (?:[_a-z]+\\s)))to ((?:[_a-z]+\\s){1,})";
	static private String to =  "((?:(?:^|,|>) (?:[_a-z]+\\s)))to ((?:[_a-z]+\\s){1,}?)"+"(?=$|,|;|:|\\.|<|"+stop+")";
	//static private String to =  "((?:(?:^|,|>|"+stop+") (?:[_a-z]+\\s)))to ((?:[_a-z]+\\s){1,}?)"+"(?:$|,|;|:|\\.|<|"+stop+")";
	//static private String tolist ="((?:(?:^|,|>|"+stop+") (?:[_a-z]+\\s))*), to ((?:[_a-z]+\\s){1,})";
	//static private String tolist ="((?:(?:^|,|>|"+stop+") (?:[_a-z]+\\s))*), to ((?:[_a-z]+\\s){1,}?)"+"(?:$|,|;|:|\\.|<|"+stop+")";
	static private String tolist ="((?:(?:^|,|>) (?:[_a-z]+\\s))*), to ((?:[_a-z]+\\s){1,}?)"+"(?=$|,|;|:|\\.|<|"+stop+")";

	
	private ArrayList<StateGroup> stategroups = null;
	private Hashtable<Integer, String> sentences = null;
	private Glossary glossary = null;
	private Hashtable<String, StateGroup> groups = null; 
	private String statespatterns = "";
	private String organnames = null;
	private String tablePrefix = "";
	

	public CharacterLearner(String database, String tablePrefix) {
		// read sentences in the database.sentencetable generated by unsupervised.pl
		// create StateGroups
		// bootstrap StateGroups
		this.tablePrefix = tablePrefix;
		CharacterLearner.database = database;
		this.groups = new Hashtable<String, StateGroup>();
		try{
			if(conn == null){
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists "+this.tablePrefix+"_learnedstates (state varchar(100) NOT NULL PRIMARY KEY, count int(4))");
				stmt.execute("delete from "+this.tablePrefix+"_learnedstates");
				//Statement stmt = conn.createStatement();
				stmt.execute("update "+this.tablePrefix+"_sentence set charsegment =''");

			}
		}catch(Exception e){
			LOGGER.error("Exception in  CharacterLearner constructor" + e);
			e.printStackTrace();
		}
		//glossary is created in VolumeDehyphenizer
	//	this.glossary = new Glossary(new File(Registry.ConfigurationDirectory + "FNAGloss.txt"), true, this.database, this.tablePrefix);
		
		this.stategroups =  new ArrayList<StateGroup>();
		this.sentences = new Hashtable<Integer, String>();
		this.organnames = collectOrganNames();

		markSentences();//tag organ names
		
		parseSentences();//create StateGroups 
		bootstrap();//infer characters
		
		DeHyphenizerCorrected dh = new DeHyphenizerCorrected(database, this.tablePrefix+"_learnedstates", "state", "count", "_", this.tablePrefix, this.glossary);
		dh.deHyphen();
		this.statespatterns = collectStateNames(); //create character patterns
	}
	/*
	 * bootstrap stategroups
	 */
	private void bootstrap(){
		/*try{
			Statement stmt = conn.createStatement();
			stmt.execute("create table if not exists "+this.tablePrefix+"_bootstrap (state1 varchar(100), character1 varchar(200), PRIMARY KEY (state1, character1))");
		}catch(Exception e){
			e.printStackTrace();
		}*/
		
		//Bootstrap b = new Bootstrap(stategroups, glossary, database, "bootstrap");
		Bootstrap b = new Bootstrap(stategroups,glossary, database);
		b.go();
	}
	
	
	
	@SuppressWarnings("unused")
	private void assembleDescription(){
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select filename, endindex from "+this.tablePrefix+"_fileclauselink");
			int start = 0;
			while(rs.next()){
				String filename = rs.getString("filename");
				int end = rs.getInt("endindex");
				System.out.println("output "+filename);
				String content = getDescription(start, end);
				System.out.println(content);
			//	SAXBuilder builder = new SAXBuilder();
				//Document doc = builder.build(new ByteArrayInputStream(content.getBytes("UTF-8")));
			    BufferedWriter out = new BufferedWriter(new FileWriter(filename));
		        out.write(content);
		        out.close();
				start = end+1;
			}
		}catch (Exception e){
			LOGGER.error("Exception in  CharacterLearner assembleDescription" + e);
			e.printStackTrace();
		}
	}
	public ArrayList<String> getMarkedDescription(String filename){
		ArrayList<String> results = new ArrayList<String>();
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select filename, endindex from "+this.tablePrefix+"_fileclauselink where filename=\""+filename+"\"");
			if(rs.next()){
				int end = rs.getInt("endindex");
				rs = stmt.executeQuery("select filename, endindex from "+this.tablePrefix+"_fileclauselink where endindex<"+end+" order by endindex desc");
				int start = 0;
				if(rs.next()){
					start = rs.getInt("endindex")+1;
				}
				results.add(getDescription(start, end));
				return results;
			}
		}catch (Exception e){
			LOGGER.error("Exception in  CharacterLearner getMarkedDescription" + e);
			e.printStackTrace();
		}
		return results;
	}
	
	private String getDescription(int start, int end) throws SQLException {
		String content = "<?xml version=\"1.0\"?><description>";
		for(int i = start; i <= end; i++){
			Statement stmt1 = conn.createStatement();
			ResultSet rs1 = stmt1.executeQuery("select clause, tag, attributes, modifier from "+this.tablePrefix+"_clause where clauseid="+i);
			rs1.next();
			String sent = rs1.getString("clause");
			sent = sent==null? "" : sent.trim();
			String atts = rs1.getString("attributes");
			atts = atts==null? "" : atts.trim();
			String modifier = rs1.getString("modifier");
			modifier = modifier==null? "" : modifier.trim();
			modifier = modifier.replaceAll("\\s+", "_");
			String tag = rs1.getString("tag");
			tag = tag==null? "" : tag.trim();

			tag = tag.replaceFirst("\\b(2n|n|x)\\b", "chromosomes");
			tag = tag.replaceAll("\\s+", "_");
			
			
			String starttag ="";
			if(modifier!=null && modifier.compareTo("")!= 0 && modifier.compareTo("null")!= 0){
				starttag +=modifier+"_"+tag;
			}else{
				starttag +=tag.trim();
			}
			starttag = starttag.replaceAll("^\\d+\\s*", "").replaceAll("\\W", "");
			String endtag = "</"+starttag+">";
			if(atts.compareTo("")!=0){
				starttag += " "+atts;
			}
			starttag = "<"+starttag+">";
			content += starttag+sent+endtag;
		}
		content+="</description>";
		return unhide(content.replaceAll("[}{]", ""));
	}
	
	private String generateAttributes(String sent, String charsegment){
		if(sent.compareTo("or perennial ;") == 0){
			System.out.println();
		}
		String attributes = "";
		//TODO sort atts, 
		//TODO no duplicated atts are allowed in an xml tag
		//TODO deal with comparisons between two organs.
		Hashtable<String, String> atts = new Hashtable<String, String>(); //collect attributes then sort them alphabetically
		//deal with numbers:size
		Pattern p = Pattern.compile("(.*?) ("+num+")(cm|mm|m|dm|meters|meter)\\b(.*)");
		Matcher m = p.matcher(sent);
		while(m.find()){
			String value = m.group(2).trim()+ " "+m.group(3);
			if(atts.get("size") == null){
				atts.put("size", value);
			}else{
				atts.put("size", atts.get("size")+";"+value);
			}
			sent = m.group(1)+m.group(4);
			m = p.matcher(sent);
		}
		//deal with numbers:count
		p = Pattern.compile("(.*?) ("+num+")(.*)");
		m = p.matcher(sent);
		while(m.find()){
			String value = m.group(2).replaceAll("\\W+$", "");
			if (value.indexOf('/')<0){
				if(atts.get("count") == null){
					atts.put("count", value);
				}else{
					atts.put("count", atts.get("count")+";"+value);
				}
			}
			sent = m.group(1)+m.group(3);
			m = p.matcher(sent);
		}
		
		if(charsegment != null && charsegment.compareTo("")!=0){
			String[] segs = charsegment.split(";");
			for(int i = 0; i < segs.length; i++){
				String[] parts = segs[i].split("#");
				String text = parts[0];
				String exp = parts[1];
				sent = sent.replace(text, " ");
				StateGroup sg = (StateGroup)groups.get(exp);
				String att = sg.mostFreqCategory().replaceFirst("#.*","").replaceAll("\\s+", "_");
				if(att.compareTo("") != 0){
					String value = text;//TODO to or patterns
					if(atts.get(att) == null){
						atts.put(att, value);
					}else{
						atts.put(att, atts.get(att)+";"+value);
					}
				}
			}
		}
		//TODO deal with negations 
		
		Pattern pattern = Pattern.compile("((?:(?:not|rarely|barely|seldom) (?:\\w+ )?)?\\b("+statespatterns+")\\b)");
		m = pattern.matcher(sent);
		while(m.find()){
			String state = m.group(2);
			String value = m.group(1);
			ArrayList<?> chars = Glossary.getCharacter(state);
			if(chars.size() >0){
				Iterator<?> it = chars.iterator();
				String att = "";
				while(it.hasNext()){
					att += ((String)it.next()).replaceAll("\\s+", "_")+"_or_";
				}
				att = att.replaceFirst("_or_$", "");
			
				if(atts.get(att) == null){
					atts.put(att, value);
				}else{
					atts.put(att, atts.get(att)+";"+value);
				}
			}
		}
		//sort atts
		Set<String> keys = atts.keySet();
		String[] keyarray = (String[])keys.toArray(new String[]{});
		Arrays.sort(keyarray);
		for(int i = 0; i<keyarray.length; i++){
			String att = keyarray[i]+"='"+(String)atts.get(keyarray[i])+"'";
			attributes += att+" ";
		}
		return attributes.trim(); 
	}
	/**
	 * parse or and to patterns, 
	 * save each pattern as a group of state (StateGroup)
	 * check for characters for a state
	 * save patterns in a sorted collection 
	 */
	private void parseSentences(){
		Enumeration<Integer> en = sentences.keys();
		while(en.hasMoreElements()){
			Integer key = (Integer)en.nextElement();
			int sentid = key.intValue();
			String taggedsent = (String)sentences.get(key);
			parseSentence(sentid, taggedsent);
		}
	}
	
	private void parseSentence(int sentid, String sent){
		boolean match = false;
		do{
			match = false;
			String copy = sent;
			if(sent.indexOf("glabrous or floccose to tomentose or lanate")>=0){
				System.out.println();
			}
			//sent = doSynonyms(sentid, sent);
			sent = doSimple(sentid, sent);
			sent = doList(sentid, sent);
			sent = doTo(sentid,sent);
			sent = doToList(sentid, sent);
			if(copy.compareTo(sent) != 0){
				match = true;
			}
		}while(match);
	}
	private String doToList(int sentid, String sent){
		Pattern tolistp = Pattern.compile(CharacterLearner.tolist);
		Matcher m = tolistp.matcher(sent);
		if(m.find()){
			String seg = sent.substring(m.start(), m.end());
			System.out.println( "\t"+seg);
		    String t1 = normalize(m.group(1));
		    String t2 = normalize(m.group(2));
		    String [] terms = t1.split("\\s*,\\s*");
		    List<String> list = Arrays.asList(terms);
		    ArrayList<String> alist = new ArrayList<String>(list);
		    alist.add(t2);
		    //if(alist.size() > 1){
		    	group(alist, sentid, seg);
		    	System.out.println("["+t1+"] and ["+t2+"] are in the same group [tolist]");
		    //}else{
		    	System.out.println("["+t1+"] and ["+t2+"] were not put in the same group [tolist]");
		    //}
		    sent = sent.replaceFirst(CharacterLearner.tolist, "");
		}
	    return sent;
	}
	private String doTo(int sentid, String sent){
		Pattern top = Pattern.compile(CharacterLearner.to);
		Matcher m = top.matcher(sent);
		if(m.find()){
			String seg = sent.substring(m.start(), m.end());
			System.out.println( "\t"+seg);
			String t1 = normalize(m.group(1));
			String t2 = normalize(m.group(2));
			//if(t1.compareTo("") != 0 && t2.compareTo("") != 0){
				ArrayList<String> list = new ArrayList<String>();
				list.add(t1);
				System.out.print("["+t1+"] ");
				String[] t2s = t2.split("\\b(to|or)\\b");
				for(int i = 0; i<t2s.length; i++){
					list.add(normalize(t2s[i]));
					System.out.print("["+t2s[i]+"] ");
				}
				group(list, sentid, seg);
				System.out.println(" are in the same group [to]\n");
			//}else{
			//	System.out.println("["+t1+"] and ["+t2+"] were not put in the same group [to]\n");
			//}
				sent = sent.replaceFirst(CharacterLearner.to, "");
		}
	    return sent;
	}
	private String doList(int sentid, String sent){

		Pattern listp = Pattern.compile(CharacterLearner.list);
		Matcher m = listp.matcher(sent);
		if(m.find()){
			String seg = sent.substring(m.start(), m.end());
			System.out.println( "\t"+seg);
			String t1 = m.group(1);
			String t2 = m.group(2);
	        String [] terms = t1.split("\\s*,\\s*");
	        List<String> list = Arrays.asList(terms);
	        ArrayList<String> alist = new ArrayList<String>(list);
			if (alist.size() >= 3){
				alist.remove(0); //be conservative to avoid sessile, rhomic, lanceolate, or oblanceolate
			}
			alist.add(t2);
			//if(alist.size() > 1){
				for(int i = 0; i < alist.size(); i++){
					alist.set(i, normalize((String)alist.get(i)));
				}
				group(alist, sentid, seg);
				System.out.println ("["+t1+"] and ["+t2+"] are in the same group [list]\n");
			//}else{
			//	System.out.println ("["+t1+"] and ["+t2+"] were not put in the same group [list]\n");
			//}
			sent = sent.replaceFirst(CharacterLearner.list, "");
		}
	    return sent;
	}
	
	private String doSimple(int sentid, String sent){
		Pattern simplep = Pattern.compile(CharacterLearner.simple);
		Matcher m = simplep.matcher(sent);
		if(m.find()){
			String seg = sent.substring(m.start(), m.end());
			System.out.println("\t"+seg);
			String t1 = normalize(m.group(1));
			String t2 = normalize(m.group(2));
			if(t2.length()<30){
			//if(t1.compareTo("") != 0 && t2.compareTo("") != 0){
				ArrayList<String> list = new ArrayList<String>();
				list.add(t1);
				System.out.print("["+t1+"] "); 
				String[] t2s = t2.split("\\b(to|or)\\b");
				for(int i = 0; i<t2s.length; i++){
					list.add(normalize(t2s[i]));
					System.out.print("["+t2s[i]+"] "); 
				}
				group(list, sentid, seg);
				System.out.println(" are in the same group [simple]\n");
			//}else{
			//	System.out.println("["+t1+"] and ["+t2+"] were not put in the same group [simple]\n");
			//}
			}
	        sent = sent.replaceFirst(CharacterLearner.simple, "");
		}
	    return sent;
	}
	
	@SuppressWarnings("unused")
	private String doSynonyms(int sentid, String sent){
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
	}
	/**
	 * check against glossary
	 */
	private void group(List<String> terms, int clauseid, String matchedseg){
		Iterator<String> it = terms.iterator();
		StateGroup g = new StateGroup();
		while(it.hasNext()){
			String term = ((String) it.next()).trim();
			String[] tmp = new String[1];
			tmp[0] = term;
			if(term.matches(".*?\\b(or|to)\\b.*")){
				tmp = term.split("\\s*(or|to)\\s*");
			}
			for(int i=0; i<tmp.length; i++){
				if(tmp[i].compareTo("") != 0 && !tmp[i].matches(".*?\\b("+this.organnames+")\\b.*")){
					String t = add2LearnedStates(tmp[i]);
					State s = new State(t, glossary);
					g.addState(s);
					
				}
			}
		}
		String exp = g.toString();
		if(exp.compareTo("") != 0){
			if(this.groups.containsKey(exp)){
				((StateGroup)this.groups.get(exp)).increment();
			}else{
				stategroups.add(g); //duplicates will not be added
				this.groups.put(exp, g);
			}
		
			matchedseg = matchedseg.replaceAll("[><;,\\.]", "").trim();
			matchedseg = matchedseg+"#"+exp;
			//sentence
			try{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select charsegment from "+this.tablePrefix+"_sentence where sentid="+clauseid);
				rs.next();
				String tmp =rs.getString("charsegment");
				if(tmp != null && tmp.compareTo("") !=0){
					matchedseg = tmp+";"+matchedseg; // seg#exp;seg#exp
				}
				stmt.execute("update "+this.tablePrefix+"_sentence set charsegment =\""+matchedseg+"\" where sentid ="+clauseid);
			}catch (Exception e){
				LOGGER.error("Exception in  CharacterLearner group" + e);
				e.printStackTrace();
			}
			/*clause
			 * try{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select charsegment from "+this.tablePrefix+"_clause where clauseid="+clauseid);
				rs.next();
				String tmp =rs.getString("charsegment");
				if(tmp != null && tmp.compareTo("") !=0){
					matchedseg = tmp+";"+matchedseg; // seg#exp;seg#exp
				}
				stmt.execute("update "+this.tablePrefix+"_clause set charsegment =\""+matchedseg+"\" where clauseid ="+clauseid);
			}catch (Exception e){
				e.printStackTrace();
			}*/
		}
	}
	
	private String add2LearnedStates(String term){
		String t = null;
		//if(term.indexOf("_")<0){ //normal term without "-"
			t = term;
			try{
				Statement stmt =conn.createStatement();
				ResultSet rs = stmt.executeQuery("select state, count from "+this.tablePrefix+"_learnedstates where state ='"+term+"'");
				if(rs.next()){
					int count = rs.getInt("count")+1;
					stmt.execute("update "+this.tablePrefix+"_learnedstates set count ="+count+" where state ='"+term+"'");
				}else{
					stmt.execute("insert into "+this.tablePrefix+"_learnedstates values('"+term+"', 1)");
				}
			}catch (Exception e){
				LOGGER.error("Exception in  CharacterLearner add2LearnedStates" + e);
				e.printStackTrace();
			}
		/*}else{
			String t1 = term.replaceAll("_", ""); 
			String t2 = term.replaceAll("_", " "); 
			try{
				Statement stmt =conn.createStatement();
				ResultSet rs = stmt.executeQuery("select state, count from "+this.tablePrefix+"_learnedstates where state ='"+t1+"'");
				if(rs.next()){//use t1
					int count = rs.getInt("count")+1;
					stmt.execute("update "+this.tablePrefix+"_learnedstates set count ="+count+" where state ='"+t1+"'");
					t = t1;
				}else{//use t2
					rs = stmt.executeQuery("select state, count from "+this.tablePrefix+"_learnedstates where state ='"+t2+"'");
					if(rs.next()){
						int count = rs.getInt("count")+1;
						stmt.execute("update "+this.tablePrefix+"_learnedstates set count ="+count+" where state ='"+t2+"'");
					}else{
						stmt.execute("insert into "+this.tablePrefix+"_learnedstates values('"+t2+"', 1)");
					}
					t = t2;
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}*/
		return t;
	}
	
	private String normalize(String sent){
		if(sent == null){return sent;}
		sent = sent.replaceAll("[<>,;.]", "")
		.replaceAll("\\b("+stop+")\\b", "").replaceAll("\\d+", "").replaceAll("\\b[_a-z]*?ly\\b","")
		.replaceFirst("^to_", "to").replaceFirst("^or_", "or")./*replaceAll("_", " ").*/replaceAll("\\s+", " ").replaceFirst("^\\s+", "").replaceFirst("\\s+$", "");
		return sent.trim().toLowerCase();
	}
	
	private String collectStateNames(){
		StringBuffer tags = new StringBuffer();
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct state from "+this.tablePrefix+"_learnedstates");
			while(rs.next()){
				String tag = rs.getString("state");
				if(tag == null){continue;}
				tags.append(tag+"|");
			}
		}catch(Exception e){
			LOGGER.error("Exception in  CharacterLearner collectStateNames" + e);
			e.printStackTrace();
		}
		return tags.toString()+Glossary.getAllCharacters();
	}
	
	private String collectOrganNames(){
		StringBuffer tags = new StringBuffer();
		try{
		Statement stmt = conn.createStatement();
		//ResultSet rs = stmt.executeQuery("select distinct term from fna.fnaglossary where category in ('STRUCTURE', 'CHARACTER', 'FEATURE', 'SUBSTANCE', 'PLANT')");
		ResultSet rs = stmt.executeQuery("select distinct term from "+this.tablePrefix+"_fnaglossary where category in ('STRUCTURE', 'SUBSTANCE', 'PLANT')");
		while(rs.next()){
			String tag = rs.getString("term");
			if(tag == null){continue;}
			tags.append(tag+"|");
		}
		rs = stmt.executeQuery("select distinct tag from "+this.tablePrefix+"_sentence");
		while(rs.next()){
			String tag = rs.getString("tag");
			if(tag == null || tags.indexOf("|"+tag+"|") >= 0){continue;}
			tags.append(tag+"|");
		}
		//find pl. form
		rs = stmt.executeQuery("select word from "+this.tablePrefix+"_wordpos where pos = \"p\"");
		while(rs.next()){
			tags.append(rs.getString("word").trim()+"|");
		}
		tags = tags.replace(tags.lastIndexOf("|"), tags.lastIndexOf("|")+1, "");
		}catch(Exception e){
			LOGGER.error("Exception in  CharacterLearner collectOrganNames" + e);
			e.printStackTrace();
		}
		return tags.toString();
		/*StringBuffer names = new StringBuffer();
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct tag from "+this.tablePrefix+"_sentence order by tag");
			while(rs.next()){
				names.append(rs.getString("tag")+"|");
			}
			rs = stmt.executeQuery("select distinct word from "+this.tablePrefix+"_wordpos where pos='p' order by word");
			while(rs.next()){
				names.append(rs.getString("word")+"|");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return names.toString().replaceFirst("\\|$", "");*/
	}
/**
 * collect tag names (sing. and pl. forms)
	mark sentences one by one using tags
	tag only the starting words of a sentence.
	convert numbers to NUM
 * @param sentencetable
 */
	private void markSentences(){
		try{
			Pattern tagsp = Pattern.compile("(.*?)\\b("+this.organnames+")\\b(.*)", Pattern.CASE_INSENSITIVE);
			//now mark sentence one by one, add marked sentences in this.sentences
			//break sentence into meaningful clauses (each with a marked subject)
			//create a clause table to save the clauses
			//tracking the relation between filenames and clauses, saving this info in 
			//a new table fileclauselink (filename, endindex of the last clause in the file).
			/*moved to sentences2clauses
			 stmt.execute("create table if not exists "+this.tablePrefix+"_clause (clauseid int(11) not null primary key, tag varchar(150), modifier varchar(150), clause varchar(500), charsegment varchar(250), attributes varchar(500))");
			stmt.execute("delete from "+this.tablePrefix+"_clause");
			stmt.execute("create table if not exists "+this.tablePrefix+"_fileclauselink (filename varchar(200) not null primary key, endindex int(11))");
			stmt.execute("delete from "+this.tablePrefix+"_fileclauselink");
			*/
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select count(sentid) from "+this.tablePrefix+"_sentence");
			rs.next();
			int total = rs.getInt(1);
			int sentid = 0;
			String[] tos = CharacterLearner.tophrases.split("\\|");
			String[] ors = CharacterLearner.orphrases.split("\\|");
			
			while(sentid < total){
				//rs = stmt.executeQuery("select tag, modifier, sentence from "+this.tablePrefix+"_sentence where sentid="+sentid+""); Hong: 10/17/09

				rs = stmt.executeQuery("select tag, modifier, originalsent from "+this.tablePrefix+"_sentence where sentid="+sentid+"");
				if(rs.next()){
				String sent = (String)rs.getString("originalsent"); //Partha 10/17/09
				//String sent = (String)rs.getString("sentence"); 
				sent = sent.replaceAll("\\([^)]*\\)", ""); ///Hong 10/17/09 added 3 lines
				sent = sent.replaceAll("\\{[^}]*\\}", "");
				sent = sent.replaceAll("\\[[^]]*\\]", "");
				//sent = sent.replaceAll(this.num, "NUM "); //all numbers => NUM
				if(sent.matches(".*?("+CharacterLearner.tophrases+").*")){
				sent = hide(tos, sent);
				}
				if(sent.matches(".*?("+CharacterLearner.orphrases+").*")){
				sent = hide(ors, sent);
				}

				String taggedsent = "";
				/*String[] sts = sent.split("\\s*,\\s*");
				Matcher m = null;
				for(int i = 0; i< sts.length; i++){
					if(i!=0){ sts[i] = " , "+sts[i];}
					m = p.matcher(sts[i]);
					if(m.matches()){ //tag the first mentioning of an organ in a sentence
						taggedsent += m.group(1)+"<"+m.group(2)+">"+m.group(3);
					}else{
						taggedsent +=sts[i];
					}
				}*/
				Matcher m = tagsp.matcher(sent);
				while(m.matches()){
					taggedsent += m.group(1)+"<"+m.group(2)+">";
					sent = m.group(3);
					m = tagsp.matcher(sent);
			    }
				taggedsent +=sent;
				/* seg clauses should be done later, after the learning of states.
				Pattern p = Pattern.compile(", (\\w+)? ?(<.*?>)");//the word after , should not be connectors such as "or"
				Matcher m2 = p.matcher(taggedsent);
				int start = 0;
				while(m2.find()){
					if(m2.group(1)==null || m2.group(1).compareTo("or") != 0){//the word after , should not be connectors such as "or"
						int end = m2.start(); //this ends a clause
						String taggedclause = taggedsent.substring(start, end+1);
						addClause(sentid, sentid+offset, tag, modifier, taggedclause, false);
						offset++;
						start = end+1;
						modifier = m2.group(1);
						tag = m2.group(2).replaceAll("[<>]", "");
					}
				}
				String taggedclause = taggedsent.substring(start);
				addClause(sentid, sentid+offset, tag, modifier, taggedclause, true);
				*/
				sentences.put(new Integer(sentid), taggedsent); //do this in addClause
				}
				sentid++;
				//System.out.println(sentid);
				
			}
		}catch (Exception e){
			LOGGER.error("Exception in  CharacterLearner markSentences" + e);
			e.printStackTrace();
		}
	}
	
	private String hide(String[] phrases, String str){
		for(int i = 0; i < phrases.length; i++){
			String hidden = phrases[i].replaceAll("\\s+", "*");
			str = str.replaceAll(phrases[i], hidden);
		}
		return str;
	}

	private String unhide(String str){
		str = str.replaceAll("\\*", " ").trim();
		return str;
	}
	
	private void createClauseTables(){
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("create table if not exists "+this.tablePrefix+"_clause (clauseid int(11) not null primary key, tag varchar(150), modifier varchar(150), clause varchar(500), charsegment varchar(500), attributes varchar(500))");
			stmt.execute("delete from "+this.tablePrefix+"_clause");
			stmt.execute("create table if not exists "+this.tablePrefix+"_fileclauselink (filename varchar(200) not null primary key, endindex int(11))");
			stmt.execute("delete from "+this.tablePrefix+"_fileclauselink");
		}catch(Exception e){
			LOGGER.error("Exception in  CharacterLearner createClauseTables" + e);
			e.printStackTrace();
		}
	}
	/**
	 * tag states with {}
	 * @param sentence, with organ names tagged, e.g. <leaves> basal.
	 * @return
	 */
	private String tagStates(String sent){
		String taggedsent = "";
		Pattern tagsp = Pattern.compile("(.*?)\\b("+this.statespatterns+")\\b(.*)", Pattern.CASE_INSENSITIVE);
		Matcher m = tagsp.matcher(sent);
		while(m.matches()){
			taggedsent += m.group(1)+"{"+m.group(2)+"}";
			sent = m.group(3);
			m = tagsp.matcher(sent);
	    }
		taggedsent +=sent;
		return taggedsent.replaceAll("\\} \\{", " ");
	}
	
	private String[] getInfo(int sentid){
		String[] info = new String[3];
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select tag, modifier, charsegment from "+this.tablePrefix+"_sentence where sentid="+sentid+"");
			rs.next();
			info[0] = rs.getString("tag");
			info[1]=  rs.getString("modifier");
			info[2] = rs.getString("charsegment");
		}catch (Exception e){
			LOGGER.error("Exception in  CharacterLearner getInfo" + e);
			e.printStackTrace();
		}
		return info;
	}
	/**
	 * 
	 * @param taggedclause
	 * @param charsegment
	 * @return [0] the segment of charsegment matching this taggedclause, [1] the rest
	 */
	private String[] splitCharSegment(String taggedclause, String charsegment){
		String[] splits = {"",""};
		if(charsegment == null || charsegment.trim().compareTo("") == 0){
			return splits;
		}
		String[] segs = charsegment.split(";");
		int i = 0;
		for(i = 0; i<segs.length; i++){
			String[] parts = segs[i].split("#");
			if(taggedclause.indexOf(parts[0]) >= 0){
				splits[0] += segs[i]+";";
			}else{
				break;
			}
		}
		for(int j = i; j<segs.length; j++){
			splits[1] += segs[j]+";";
		}
		
		splits[0] = splits[0].replaceFirst(";$", "");
		splits[1] = splits[1].replaceFirst(";$", "");
		
		return splits;
	}
	
	private void sentences2clauses(){
		createClauseTables();
		int total = sentences.size();
		int offset = 0;
		for(int sentid = 0; sentid < total; sentid++){
			if(sentid == 567){
				System.out.println();
			}
			String taggedsent = (String)sentences.get(new Integer(sentid));
			taggedsent = tagStates(taggedsent);
			String[] info = getInfo(sentid);
			String tag = info[0];
			String modifier = info[1];
			String charsegment = info[2]; //TODO split charsegment among clauses.
			Pattern p = Pattern.compile(", (\\{[^{]*?\\})? ?(<\\w*?>)");
			Matcher m2 = p.matcher(taggedsent);
			int start = 0;
			while(m2.find()){
				//if(m2.group(1)==null ){
					int end = m2.start(); //this ends a clause
					String taggedclause = taggedsent.substring(start, end+1);
					taggedclause = taggedclause.replaceAll("[}{]", "");
					String[] segs = splitCharSegment(taggedclause, charsegment);
					charsegment = segs[1];
					addClause(sentid, sentid+offset, tag, modifier, taggedclause, segs[0], false);
					offset++;
					start = end+1;
					modifier = m2.group(1)==null? "" : m2.group(1);
					tag = m2.group(2).replaceAll("[<>]", "");
				//}
			}
			String taggedclause = taggedsent.substring(start);
			taggedclause = taggedclause.replaceAll("[}{]", "");
			addClause(sentid, sentid+offset, tag, modifier, taggedclause, charsegment, true);
		}
	}
	/**
	 * update clause table and fileclauselink table, and clause hashtable
	 * @param clauseid
	 * @param tag
	 * @param modifier
	 * @param clause
	 */
	private void addClause(int sentid, int clauseid, String tag, String modifier, String taggedclause, String charsegment, boolean lastclause){
		//remove <> from taggedclause before put it in the clause table
		//remove 2nd and later sets of <> from taggedclause before put into sentences (renamed to clauses) <pollen><grains> a b c d e ...
		Pattern p = Pattern.compile("^([^>]*?)> <(.*)");
		Matcher m = p.matcher(taggedclause);
		if(m.matches()){
			taggedclause = m.group(1)+"@"+m.group(2);
		}
		String tmp = taggedclause.replaceFirst("<", "#").replaceFirst(">", "##");
		tmp = tmp.replaceAll("[<>]", "");
		tmp = tmp.replaceFirst("##", ">");
		tmp = tmp.replaceFirst("#", "<");
		tmp = tmp.replaceFirst("@", "> <");
		//sentences.put(new Integer(clauseid), tmp);
		tmp = tmp.replaceAll("[<>]", "");
		charsegment = charsegment==null || charsegment.trim().compareToIgnoreCase("null")==0 ? "" : charsegment;
		charsegment = charsegment.trim();
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+this.tablePrefix+"_clause (clauseid, tag, modifier, clause, charsegment) values("+clauseid+", '"+tag+"', '"+modifier+"', '"+tmp+"', '"+charsegment+"')");
			if(lastclause){
				ResultSet rs = stmt.executeQuery("select filename from "+this.tablePrefix+"_sentinfile where endindex="+sentid);
				if(rs.next()){
					String fname = rs.getString("filename");
					stmt.execute("insert into "+this.tablePrefix+"_fileclauselink values ('"+fname+"', '"+clauseid+"')");
				}
			}
		}catch (Exception e){
			LOGGER.error("Exception in  CharacterLearner addClause" + e);
			e.printStackTrace();
		}
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	/*	String gfile = "C://Documents and Settings//hongcui//Desktop//WorkFeb2008//FNA//FNAGloss.txt";
		CharacterLearner cl = new CharacterLearner("fnav5_corpus", "fna");
		cl.markupCharState();
		cl.assembleDescription();
		cl.getMarkedDescription("1.xml");*/

	}

}
