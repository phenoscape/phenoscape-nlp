package fna.parsing.finalizer;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.regex.*;
import java.util.Hashtable;
import java.util.Enumeration;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.character.CharacterLearner;

/*
 * after unsupervise.pl (learn organ names, marked clauses)
 * and fna.parsing.character (learn about states)
 * 
 * run this class to resolve the unknown tags applied to some of the clauses
 */
public class UnknownResolver {
	private static String sentencetable;
	//private static String structuretable;
	private static String statetable;
	//private static String glosstable;
	private static String postable;
	
	private Hashtable<String, String> unmarked = new Hashtable<String, String>();
//	private static String username = ApplicationUtilities.getProperty("database.username");
//	private static String password = ApplicationUtilities.getProperty("database.password");
	private Connection conn = null;
	private static final Logger LOGGER = Logger.getLogger(UnknownResolver.class);
	
	//public UnknownResolver(String database, String senttable, String postable, String structtable, String statetable, String glosstable) {
	public UnknownResolver(String database, String senttable, String postable, String statetable) {
		UnknownResolver.sentencetable = senttable;
		//this.structuretable = structtable;
		UnknownResolver.statetable = statetable;
		//this.glosstable = glosstable;
		UnknownResolver.postable = postable;
		try{
			if(conn == null){
				Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			LOGGER.error("Error in UnknownResolver: constructor", e);
			e.printStackTrace();
		}
	}
	
	/**
	 * <c>: states
	 * <s>: stopwords
	 * <o>: organ names
	 * <b>: boundary words (-states)
	 * <m>: modifier (-states-boundary words)
	 */
	private void markupUnknows(){
		String statepattern = collectLearnedStateNames();//not include glossary term
		String organpattern = collectOrganNames(); //not include glossary terms
		String stoppattern = CharacterLearner.stop;
		String boundarypattern = getRequiredString("word", "pos", "b", UnknownResolver.postable);
		String modifierpattern = getRequiredString("modifier","modifier", "%", UnknownResolver.sentencetable);
		int count = 1;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select sentid, sentence from "+UnknownResolver.sentencetable+" where tag ='unknown' order by sentence");
			while(rs.next()){
				String marked = simpleTag(rs.getString("sentence"), statepattern, "c");
				marked = simpleTag(marked, organpattern, "o");
				marked = simpleTag(marked, stoppattern, "s");
				marked = simpleTag(marked, boundarypattern, "b");
				marked = simpleTag(marked, modifierpattern, "m");
				System.out.println((count++)+": "+rs.getInt("sentid")+": "+marked);
				collectUnmarked(marked);
			}
		}catch(Exception e){
			LOGGER.error("Error in UnknownResolver: markupUnknows", e);
			e.printStackTrace();
		}
		Enumeration<String> en = unmarked.keys();
		while(en.hasMoreElements()){
			System.out.println(en.nextElement().toString());
		}
	}
	
	//=============================================================================
	/*
	 * collect unmarked terms from marked sentence
	 */
	private void collectUnmarked(String marked){
		String temp = marked.replaceAll("<[a-z]>[^<]*?</[a-z]>", "");
		String words[] = temp.split("[ ,;\\.]");
		for(int i = 0; i < words.length; i++){
			if(!words[i].matches("\\d+")){
				unmarked.put(words[i], "");
			}
		}
	}
	
	private String simpleTag(String sent, String pattern, String tag) {
		String taggedsent = "";
		Pattern tagsp = Pattern.compile("(.*?)\\b("+pattern+")\\b(.*)", Pattern.CASE_INSENSITIVE);
		Matcher m = tagsp.matcher(sent);
		while(m.matches()){
			String g1 = m.group(1);
			String g2 = m.group(2);
			String g3 = m.group(3);
			if(g1.endsWith(">") || g2.matches("\\d+")){ //pattern earlier tagged <s>an</s>
				taggedsent += g1+g2;
			}else{
				taggedsent += g1+"<"+tag+">"+g2+"</"+tag+">";
			}
			sent = g3;
			m = tagsp.matcher(sent);
		}
		taggedsent +=sent;
		return taggedsent;
	}
	private String getRequiredString(String selcol, String testcol, String value, String table){
		StringBuffer sb = new StringBuffer();
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct "+selcol+" from "+table+" where "+testcol+" like'"+value+"'");
			while(rs.next()){
				String t = rs.getString(1).trim();
				if(t.length() >=1){
					sb.append(t+"|");
				}
			}
			sb = sb.replace(sb.lastIndexOf("|"), sb.lastIndexOf("|")+1, "");
		}catch(Exception e){
			LOGGER.error("Error in UnknownResolver: getRequiredString", e);
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	private String collectOrganNames(){
		StringBuffer tags = new StringBuffer();
		try{
		Statement stmt = conn.createStatement();
		/*ResultSet rs = stmt.executeQuery("select distinct term from fnaglossary where category in ('STRUCTURE', 'FEATURE', 'SUBSTANCE', 'PLANT')");
		while(rs.next()){
			String tag = rs.getString("term");
			if(tag == null){continue;}
			tags.append(tag+"|");
		}*/
		ResultSet rs = stmt.executeQuery("select distinct tag from "+UnknownResolver.sentencetable);
		while(rs.next()){
			String tag = rs.getString("tag");
			if(tag == null || tags.indexOf("|"+tag+"|") >= 0){continue;}
			tags.append(tag+"|");
		}
		//find pl. form
		rs = stmt.executeQuery("select word from "+UnknownResolver.postable+" where pos = 'p'");
		while(rs.next()){
			tags.append(rs.getString("word").trim()+"|");
		}
		tags = tags.replace(tags.lastIndexOf("|"), tags.lastIndexOf("|")+1, "");
		}catch(Exception e){
			e.printStackTrace();
		}
		return tags.toString();
	}
	
	private String collectLearnedStateNames() {
		StringBuffer tags = new StringBuffer();
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct state from "+statetable);
			while(rs.next()){
				String tag = rs.getString("state");
				if(tag != null){
					tags.append(tag+"|");
				}
			}
			tags = tags.replace(tags.lastIndexOf("|"), tags.lastIndexOf("|")+1, "");
		}catch(Exception e){
			LOGGER.error("Error in UnknownResolver: collectLearnedStateNames", e);
			e.printStackTrace();
		}
		return tags.toString();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		UnknownResolver ur = new UnknownResolver("fnav5_corpus", "sentence", "wordpos", "learnedstates");
		ur.markupUnknows();
	}

}
