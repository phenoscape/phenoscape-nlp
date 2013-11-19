 /* $Id: TermOutputerUtilities.java 971 2011-09-13 18:32:55Z hong1.cui $ */
/**
 * 
 */
package fna.charactermarkup;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Connection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;

import fna.parsing.ApplicationUtilities;
import fna.parsing.MainForm;
import fna.parsing.state.StateCollector;
import fna.parsing.state.WordNetWrapper;
import java.util.ArrayList;
import java.util.regex.*;
/**
 * @author hongcui
 *
 */
@SuppressWarnings({ "unused" })
public class Utilities {
	public static String or = "_or_";
	private static String selectivepreps = ChunkedSentence.prepositions.replaceFirst("\\|of\\|", "|");
	private static Pattern prepphraseptn = Pattern.compile(".*?((?:^| )in \\w+ (?:"+selectivepreps+")\\b)(.*)");
	
	public static ArrayList<String> sureVerbs = new ArrayList<String>();
	public static ArrayList<String> sureAdvs = new ArrayList<String>();
	public static ArrayList<String> partOfPrepPhrase = new ArrayList<String>();
	//public static ArrayList<String> prepPhrases = new ArrayList<String>();
	public static ArrayList<String> notSureVerbs = new ArrayList<String>();
	public static ArrayList<String> notSureAdvs = new ArrayList<String>();
	public static ArrayList<String> notPartOfPrepPhrase = new ArrayList<String>();
	public static boolean debug = false;
	public static boolean debugPOS = true;
	
	
	
	/**
	 * word must be a verb if
	 * 1. its pos is "verb" only, or
	 * 2. "does not" word
	 * 3. has "verb" pos and seen patterns (word "a/the", or word prep <organ>) and not seen pattern (word \w+ly$).
	 * 4. -ed, -ing 
	 * @param word
	 * @param conn
	 * @return
	 */
	public static boolean mustBeVerb(String word, Connection conn, String prefix){
		if(sureVerbs.contains(word)) return true;
		if(notSureVerbs.contains(word)) return false;
		WordNetWrapper wnw = new WordNetWrapper(word);
		boolean v = wnw.isV();
		//wordnet contains verb sense only
		if(!wnw.isAdj() && !wnw.isAdv() && !wnw.isN() && v && !word.endsWith("ing")){
			sureVerbs.add(word);
			if(debugPOS) System.out.println(word+" is sureVerb");
			return true;
		}
		if(!v) return false;
		try{
			Statement stmt = conn.createStatement();
			String q = "select * from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
					"where originalsent rlike '(does|do) not "+word+"'";
			ResultSet rs = stmt.executeQuery(q);
			if(rs.next()){
				sureVerbs.add(word);
				if(debugPOS) System.out.println(word+" is sureVerb");
				return true;
			}
			if(v){
				q = "select * from "+prefix+"_"+ApplicationUtilities.getProperty("HEURISTICNOUNS")+" " +
						"where word = '"+word+"'";
				rs = stmt.executeQuery(q);
				if(rs.next()){
					notSureVerbs.add(word);
					return false;
				}
				
				q = "select * from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
						"where sentence rlike '(^| )"+word+" +[-a-z_]+ly$'";
				rs = stmt.executeQuery(q);
				if(rs.next()){
					notSureVerbs.add(word);
					return false;
				}
				
				q = "select * from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
						"where sentence rlike '(^| )(a|an|the) "+word+"( |$)'";
				rs = stmt.executeQuery(q);
				if(rs.next()){
					notSureVerbs.add(word);
					return false;
				}
				
				q = "select sentence from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
						"where sentence rlike '(^| )"+word+" (a|an|the) '";
				rs = stmt.executeQuery(q);
				if(rs.next()){
					sureVerbs.add(word);
					if(debugPOS) System.out.println(word+" is sureVerb");
					return true;
				}
				
				if(word.endsWith("ed") || word.endsWith("ing")){
					q = "select sentence from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
							"where sentence rlike '(^| )"+word+" '";
					rs = stmt.executeQuery(q);
					while(rs.next()){
						String sent = rs.getString("sentence");
						
						Pattern p = Pattern.compile("\\b"+word+"\\b(?: (?:"+selectivepreps+")) +(\\S+)");
						Matcher m = p.matcher(sent);
						while(m.find()){
							String term = m.group(1);
							if(term.matches("(a|an|the|some|any|this|second|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)")){
								sureVerbs.add(word);
								if(debugPOS) System.out.println(word+" is sureVerb");
								return true;
							}else if(isOrgan(term, conn, prefix)){
								sureVerbs.add(word);
								if(debugPOS) System.out.println(word+" is sureVerb");
								return true;
							}
						}		
					}
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
		notSureVerbs.add(word);
		return false;
	}
	
	private static boolean isOrgan(String term, Connection conn, String tablePrefix) {
		try{
			Statement stmt = conn.createStatement();
			String wordrolesable = tablePrefix+ "_"+ApplicationUtilities.getProperty("WORDROLESTABLE");		
			ResultSet rs = stmt.executeQuery("select word from "+wordrolesable+" where semanticrole in ('os', 'op') and word='"+term+"'");		
			if(rs.next()){
				if(debugPOS) System.out.println(term+" is an organ");
				return true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * 	position terms from BSPO are in the glossary as of 052513		
		relational adjectives from URBERON are considered structures (and not spatial term) and are in the glossary too	
	 * @param term
	 * @param conn
	 * @param glossary
	 * @return
	 */
	public static boolean isPosition(String term, Connection conn, String glossary) {
		try{
			//position term in gloss
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select term from "+glossary+" where category in ('position') and term='"+term+"'");		
			if(rs.next()){
				if(debugPOS) System.out.println(term+" is a position");
				return true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	public static boolean mustBeAdv(String word){
		if(sureAdvs.contains(word)) return true;
		if(notSureAdvs.contains(word)) return false;
		WordNetWrapper wnw = new WordNetWrapper(word);
		if(!wnw.isAdj() && wnw.isAdv() && !wnw.isN() && !wnw.isV()){
			sureAdvs.add(word);
			if(debugPOS) System.out.println(word+" is sureAdv");
			return true;
		}
		notSureAdvs.add(word);
		return false;
	}
	
	public static boolean partOfPrepPhrase(String word, Connection conn, String prefix){
		if(partOfPrepPhrase.contains(word)) return true;
		if(notPartOfPrepPhrase.contains(word)) return true;
		if(isOrgan(word, conn, prefix)){
			notPartOfPrepPhrase.add(word);
			return false;
		}
		try{
			Statement stmt = conn.createStatement();
			String sql = "select sentence from "+prefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+" " +
					"where originalsent rlike '(^| )in "+word+" ("+selectivepreps+")( |$)'";
			ResultSet rs = stmt.executeQuery(sql);
			boolean select = true;//add other rules in the future
			boolean exist = false;
			while(rs.next()){
				exist = true;
				partOfPrepPhrase.add(word);
				if(debugPOS) System.out.println(word+" is partOfPrepPhrase");
				Matcher m = prepphraseptn.matcher(rs.getString("sentence"));
				while(m.matches()){					
					add2table(m.group(1).trim(), conn, prefix);
					m = prepphraseptn.matcher(m.group(2));
				}
				return true;
			}
			/*if(exist && select){

				return true;
			}	*/		
		}catch(Exception e){
			e.printStackTrace();
		}
		notPartOfPrepPhrase.add(word);
		return false;
	}
	
	private static void add2table(String phrase, Connection conn, String prefix) {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+prefix+"_prepphrases values ('"+phrase+"')");
		}catch(Exception e){
			e.printStackTrace();
		}		
	}
	
	///////////////////////////////////////////////////////////////////////

	public static boolean isNoun(String word, ArrayList<String> nouns, ArrayList<String> notnouns){
		word = word.trim();
		if(word.indexOf(' ')>0) return false;
		word = word.replaceAll("[<>{}\\]\\[]", "");
		if(!word.matches(".*?[a-z]+.*")){
			notnouns.add(word);
			return false;
		}
		if(word.matches("\\b("+ChunkedSentence.stop+")\\b")){
			notnouns.add(word);
			return false;
		}
		if(nouns.contains(word)){
			return true;
		}
		
		if(notnouns.contains(word)){
			return false;
		}
		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null){
			if(pos.compareTo("noun") == 0){
				nouns.add(word);
				return true;
			}
		}
		notnouns.add(word);
		return false;

	}
	
	public static boolean isVerb(String word, ArrayList<String> verbs, ArrayList<String> notverbs) {
		word = word.replaceAll("[<>{}\\]\\[]", "").trim();
		if(!word.matches(".*?[a-z]+.*")){
			return false;
		}
		if(word.matches("\\b("+ChunkedSentence.stop+")\\b")){
			return false;
		}
		if(verbs.contains(word)){
			return true;
		}
		if(notverbs.contains(word)){
			return false;
		}
		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null){
			if(pos.compareTo("verb") == 0){
				verbs.add(word);
				return true;
			}else{
				if(wnw.isV() && word.endsWith("ed")){
					verbs.add(word);
					return true;
				}
			}
		}
		notverbs.add(word);
		return false;

	}
	
	public static boolean isAdv(String word, ArrayList<String> adverbs, ArrayList<String> notadverbs) {
		word = word.replaceAll("[<>{}\\]\\[()\\d+-]", "").trim();
		if(word.matches("(not|at-?least|throughout|much)")){
			return true;
		}
		if(word.matches("in.*?(profile|view)")){//covers in-dorsal-view, in-profile
			return true;
		}
		if(word.compareTo("moreorless")==0){
			return true;
		}
		if(word.compareTo("becoming")==0){
			return true;
		}
		if(word.compareTo("±")==0){
			return true;
		}
		if(!word.matches(".*?[a-z]+.*")){
			notadverbs.add(word);
			return false;
		}
		if(word.matches("\\b("+ChunkedSentence.stop+")\\b")){
			notadverbs.add(word);
			return false;
		}
		if(adverbs.contains(word)){
			return true;
		}
		if(notadverbs.contains(word)){
			return false;
		}
		

		WordNetWrapper wnw = new WordNetWrapper(word);
		String pos = wnw.mostlikelyPOS();
		if(pos != null && pos.length()>0){
			if(pos.compareTo("adv") == 0){
				adverbs.add(word);
				return true;
			}
		}else{
			if(word.endsWith("ly")){
				adverbs.add(word);
				return true;
			}
		}
		notadverbs.add(word);
		return false;
	}
	
	/**
	 * 5-{merous}
	 * @param w
	 * @return null if not found
	 */
	public static String lookupCharacter(String w, Connection conn, Hashtable<String, String> characterhash, String glosstable, String prefix) {
		if(w.trim().length()==0) return null;
		if(w.indexOf(" ")>0) w = w.substring(w.lastIndexOf(" ")+1).trim();
		w = w.replaceAll("[{}<>()]", "").replaceAll("\\d+[–-]", "_").replaceAll("–", "-")./*replaceAll(" ", "").*/replaceAll("_+", "_");//"(3-)5-merous" =>_merous
		w = w.replaceFirst(".*?_(?=[a-z]+$)", ""); //_or_ribbed
		String wc = w;
		String ch = characterhash.get(w);
		if(ch != null){
			return ch;
		}else{
			ch = "";
			if(w.endsWith("shaped")){
				return "shape";
			}
			if(w.indexOf('-')>0){
				String[] ws = w.split("-+");
				w = ws[ws.length-1];
			}
			ch = lookup(w, conn, characterhash, glosstable, wc, prefix);
			if(ch == null && wc.indexOf('-')>0){//pani_culiform
				ch = lookup(wc.replaceAll("-", ""), conn, characterhash, glosstable, wc, prefix);
			}
		}
		return ch;
	}

	
	private static String lookup(String w, Connection conn,
			Hashtable<String, String> characterhash, String glosstable,
			String wc, String prefix) {
		String ch ="";
		HashSet<String> chs = new HashSet<String>();
		try{
			Statement stmt = conn.createStatement();
			//check glossarytable
			ResultSet rs = stmt.executeQuery("select distinct category from "+glosstable+" where term = '"+w+"' or term ='_"+w+"' order by category");
			while(rs.next()){
				String cat = rs.getString("category");
				chs.add(cat);
				//if(! ch.matches(".*?(^|_)"+cat+"(_|$).*")){
				//	ch += rs.getString("category").trim().replaceAll("\\s+", "_")+"_or_";
				//}
			}
			//check _term_category table
			String q = "select distinct category from "+prefix+"_term_category where term='"+w+"' and category !='structure' order by category";
			rs = stmt.executeQuery(q);
			while(rs.next()){
				String cat = rs.getString("category");
				chs.add(cat);
				//if(! ch.matches(".*?(^|_)"+cat+"(_|$).*")){
				//	ch += rs.getString("decision").trim().replaceAll("\\s+", "_")+"_or_";
				//}
			}			
			rs.close();
			stmt.close();
			String[] charas = chs.toArray(new String[]{});
 			Arrays.sort(charas);
 			
 			for(String character: charas){
 				ch += character.replaceAll("\\s+", "_")+"_or_";
 			}
			if(ch.length()>0){
				ch = ch.replaceFirst(Utilities.or+"$", "");
				characterhash.put(wc, ch);
				return ch;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static void insert2TermCategoryTable(String term, String cat, Connection conn, String prefix) {
		try{
			String sql = "insert into " + prefix +"_term_category values (?,?)";
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, term);
			pstmt.setString(2, cat);
			pstmt.execute();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param term
	 * @param conn
	 * @param glosstable
	 * @return
	 */
	public static boolean inGlossary(String term, Connection conn, String glosstable, String prefix) {
		term = term.replaceAll(".*[_-]", "");
		String termcopy = term;
		term = term.replaceFirst("(semi|sub|un)", "");
		boolean in = false;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select term, category from "+glosstable+" where term ='"+term+"'");
			if(rs.next()){
				String cat = rs.getString("category");
				in = true;
				Statement stmt1 = conn.createStatement();
				stmt1.execute("insert into "+prefix+"_term_category (term, category) values ('"+termcopy+"', '"+cat+"')");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return in;
	}

	/**
	 * break text into correct tokens: 
	 * @param text: that is {often} {concealed} r[p[by] o[(trichomes)]];
	 * @return
	 */
	public static ArrayList<String> breakText(String text) {
		ArrayList<String> tokens = new ArrayList<String>();
		String[] words = text.split("\\s+");
		String t = "";
		int left = 0;
		for(int i = 0; i<words.length; i++){
			String w = words[i];
			if(w.indexOf("[")<0 && w.indexOf("]")<0 && left==0){
				if(!w.matches("\\b(this|have|that|may|be|which|where|when)\\b")){tokens.add(w);};
			}else{
				left += w.replaceAll("[^\\[]", "").length();
				left -= w.replaceAll("[^\\]]", "").length();
				t += w+" ";
				if(left==0){
					tokens.add(t.trim());
					t = "";
				}
			}
		}
		return tokens;
	}
	
	public static String threeingSentence(String str) {
		//hide the numbers in count list: {count~list~9~or~less~} <fin> <rays>
		ArrayList<String> lists = new ArrayList<String>();
		str = hideLists(str, lists);		
		//threeing
		str = str.replaceAll("(?<=\\d)-(?=\\{)", " - "); //this is need to keep "-" in 5-{merous} after 3ed (3-{merous} and not 3 {merous}) 
		//Pattern pattern3 = Pattern.compile("[\\d]+[\\-\\–]+[\\d]+");
		Pattern pattern3 = Pattern.compile(NumericalHandler.numberpattern);
		//Pattern pattern4 = Pattern.compile("(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+[\\s]?[\\–\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]+[\\s]?[\\–\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d][\\s]?[\\–\\-][\\s]?[\\d]/[\\d])|(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d])");
		//Pattern pattern5 = Pattern.compile("[\\d±\\+\\–\\-\\—°²:½/¼\"“”\\_´\\×µ%\\*\\{\\}\\[\\]=]+");
		//Pattern pattern5 = Pattern.compile("[\\d\\+°²½/¼\"“”´\\×µ%\\*]+(?!~[a-z])");
		Pattern pattern5 = Pattern.compile("[\\d\\+°²½/¼\"“”´\\×µ%\\*]+(?![a-z])"); //not including individual "-", would turn 3-branched to 3 branched 
		//Pattern pattern6 = Pattern.compile("([\\s]*0[\\s]*)+(?!~[a-z])"); //condense multiple 0s.
		Pattern pattern6 = Pattern.compile("(?<=\\s)[0\\s]+(?=\\s)");
		//Pattern pattern5 = Pattern.compile("((?<!(/|(\\.[\\s]?)))[\\d]+[\\-\\–]+[\\d]+(?!([\\–\\-]+/|([\\s]?\\.))))|((?<!(\\{|/))[\\d]+(?!(\\}|/)))");
         //[\\d±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\]=(<\\{)(\\}>)]+
		Pattern pattern7 = Pattern.compile("[(\\[]\\s*\\d+\\s*[)\\]]"); // deal with ( 2 ), (23) is dealt with by NumericalHandler.numberpattern
		

		Matcher	 matcher1 = pattern3.matcher(str);
         //str = matcher1.replaceAll(" 0 ");
		str = matcher1.replaceAll("0");
		matcher1.reset();
         
         /*matcher1 = pattern4.matcher(str);
         str = matcher1.replaceAll("0");
         matcher1.reset();*/
         
         matcher1 = pattern5.matcher(str);//single numbers
         str = matcher1.replaceAll("0");
         matcher1.reset();
         
         /* should not remove space around 0, because: pollen 70-80% 3-porate should keep 2 separate numbers: 70-80% and 3-porate
		* 
         String scptemp = str;
         matcher1 = pattern6.matcher(str);//remove space around 0
         str = matcher1.replaceAll("0");
         if(!scptemp.equals(str)){
		   System.out.println();
         }
         matcher1.reset();*/
         
         matcher1 = pattern7.matcher(str);//added for (2)
         str = matcher1.replaceAll("0");
         matcher1.reset();
         //further normalization
         
         
         //3 -{many} or 3- {many}=> {3-many}
         str = str.replaceAll("0\\s*-\\s*", "0-").replaceAll("0(?!~[a-z])", "3").replaceAll("3\\s*[–-]\\{", "{3-").replaceAll("±(?!~[a-z])","{moreorless}").replaceAll("±","moreorless"); //stanford parser gives different results on 0 and other numbers.
         
         //2-or-{3-lobed} => {2-or-3-lobed}
         str = str.replaceAll("(?<=-(to|or)-)\\{", "").replaceAll("[^\\{]\\b(?=3-(to|or)-3\\S+\\})", " {");
		
         //unhide count list
         str = unCountLists(str, lists);
         return str;
	}

	/**
	 * hide lists such as
	 * {upper} {pharyngeal} <tooth> <plates_4_and_5>
	 * count~list~2~to~4
	 * so the numbers will not be turned into 3.
	 * @param str
	 * @param countlists
	 * @return
	 */
	private static String hideLists(String str,
			ArrayList<String> lists) {
		if(str.contains("count~list~") || str.matches(".*?<\\S+_\\d.*")){
			String newstr = "";
			String[] tokens = str.split("\\s+");
			int count = 0;
			for(String t: tokens){
				if(t.indexOf("count~list~")>=0 || t.matches("<\\S+_\\d.*")){
					newstr +="# ";
					lists.add(t);
					count++;
				}else{
					newstr +=t+" ";
				}
			}			
			return newstr.trim();
		}else{
			return str;
		}
	}

	private static String unCountLists(String str, ArrayList<String> lists) {
		if(str.contains("#")){
			String newstr = "";
			String[] tokens = str.split("\\s+");
			int count = 0;
			for(String t: tokens){
				if(t.contains("#")){
					newstr += lists.get(count)+" ";
					count++;
				}else{
					newstr +=t+" ";
				}
			}
			return newstr.trim();
		}else{
			return str;
		}
	}
	
	/**
	 * 
	 * @param text does [not] overlap 
	 * @return '[not] overlapping' --remove does and add -ing to signal overlap is a word
	 */
	public static String reformAuxiliaryVerbs(String text) {	
		String[] segments = text.split("\\b(does|do)\\b");
		String newtext = segments[0];
		for(int i = 1; i<segments.length; i++){
			String segment = segments[i].trim();
			boolean found = false;
			while(segment.indexOf(" ")>0){
				String word = segment.substring(0, segment.indexOf(" "));
				segment = segment.substring(segment.indexOf(" ")).trim();
				WordNetWrapper wnw = new WordNetWrapper(word);
				if(wnw.isV()){
					newtext += " "+word+"ing";
					newtext += " "+segment;
					found = true;
					break;
				}
				newtext+=" "+word;
			}
			if(!found){
				//last word in text
				WordNetWrapper wnw = new WordNetWrapper(segment);
				if(wnw.isV()){
					newtext += " "+segment+"ing";
				}
			}
		}
		return newtext.trim();
	}
	/**
	 * clean off [;\\]\\[{}(),+\\.&%@<>=`:] from string
	 * @param str
	 * @return
	 */
	public static String cleanup(String str){
		while(str.matches(".*?\\[.*?\\].*")){
			str = str.replaceAll("\\[.*?\\]", "").trim();
		}
		while(str.matches(".*?<.*?>.*")){
			str = str.replaceAll("<.*?>", "").trim();
		}
		while(str.matches(".*?\\(.*?\\).*")){
			str = str.replaceAll("\\(.*?\\)", "").trim();
		}
		while(str.matches(".*?\\{.*?\\}.*")){
			str = str.replaceAll("\\{.*?\\}", "").trim();
		}
		str = str.replaceAll("`", "'");
		if(str.contains("%")) str = ""; //%2cxyz%67
		
		str = str.replaceFirst("[@=+;,.&:<>\\]\\[\\(\\)\\{\\}].*", ""); //@fr, x=y, x+y+z
		str = str.replaceFirst("[_-]$", "");
		//if(str.matches(".*?[;\\]\\[,+\\.&%@<>=`:].*")){
		//	System.out.print(str+ " ");
	    //	}
		return str;
	}


	public static void main(String[] argv){
		
		System.out.println(reformAuxiliaryVerbs("does often overlap abc , does not usually overlap"));
		//TermOutputerUtilities.lookupCharacter(w, conn, characterhash)
		//System.out.println(TermOutputerUtilities.isNoun(",", new ArrayList<String>()));
		//System.out.println(TermOutputerUtilities.isAdv("much", new ArrayList<String>()));
	}


}
