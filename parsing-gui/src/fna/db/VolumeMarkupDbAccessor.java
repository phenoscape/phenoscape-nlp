/**
 * VolumeMarkupDbAccessor.java
 *
 * Description : This performs all the database access needed by the VolumeMarkup
 * Version     : 1.0
 * @author     : Partha Pratim Sanyal
 * Created on  : September 11, 2009
 *
 * Modification History :
 * Date   | Version  |  Author  | Comments
 *
 * Confidentiality Notice :
 * This software is the confidential and,
 * proprietary information of The University of Arizona.
 */

package fna.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;
import fna.parsing.ParsingException;

public class VolumeMarkupDbAccessor {
	//public static String PRONOUN ="all|each|every|some|few|individual|both|other";
	//public static String NUMBERS = "zero|one|ones|first|two|second|three|third|thirds|four|fourth|fourths|quarter|five|fifth|fifths|six|sixth|sixths|seven|seventh|sevenths|eight|eighths|eighth|nine|ninths|ninth|tenths|tenth";
	//public static String CHARACTERS ="lengths|length|lengthed|width|widths|widthed|heights|height|character|characters|distribution|distributions|outline|outlines|profile|profiles|feature|features|form|forms|mechanism|mechanisms|nature|natures|shape|shapes|shaped|size|sizes|sized";
	//public static String PREPOSITION ="above|across|after|along|around|as|at|before|beneath|between|beyond|by|for|from|in|into|near|of|off|on|onto|out|outside|over|than|throughout|toward|towards|up|upward|with|without";
	//public static String CLUSTERSTRINGS = "group|groups|clusters|cluster|arrays|array|series|fascicles|fascicle|pairs|pair|rows|number|numbers|\\d+";
	//public static String SUBSTRUCTURESTRINGS = "part|parts|area|areas|portion|portions";
	//public static String STOP ="a|about|above|across|after|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|for|from|had|has|have|hence|here|how|if|in|into|inside|inward|is|it|its|may|might|more|most|near|no|not|of|off|on|onto|or|out|outside|outward|over|should|so|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|was|were|what|when|where|which|why|with|within|without|would";

	/**
	 * @param args
	 */
    private static final Logger LOGGER = Logger.getLogger(VolumeMarkupDbAccessor.class);
    private static String url = ApplicationUtilities.getProperty("database.url");
	private String tablePrefix = null ;
	private String glossarytable;
	private Connection conn = null;
   
    
    public VolumeMarkupDbAccessor(String dataPrefix, String glossarytable){
    	this.tablePrefix = dataPrefix;
    	this.glossarytable = glossarytable;
    	try {
    			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
    			conn = DriverManager.getConnection(url);
    	} catch (Exception e) {
    				// TODO Auto-generated catch block
    			LOGGER.error("Couldn't find Class in MainFormDbAccessor" + e);
    			e.printStackTrace();
    	}
    		
	
    }
	
    
    /**
     * display learned new structures in structures subtab in step 4 (perl markup) for curation.
     * @param tagList
     * @throws ParsingException
     * @throws SQLException
     */
    public ArrayList<String> structureTags4Curation(List <String> tagList) throws  ParsingException, SQLException {
    	
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
		try {

			String sql = "select distinct tag as structure from "+this.tablePrefix+"_sentence where tag != 'unknown' and tag is not null and tag not like '% %' " +
			"union select plural as structure from "+this.tablePrefix+"_singularplural"+","+ this.tablePrefix+"_sentence where singular=tag "+
			"order by structure"; 		
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			while (rs.next()) {
				String tag = rs.getString("structure");
				populateCurationList(tagList, tag); //select tags for curation
			}
			sql = "select distinct word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos in ('p', 's') and saved_flag !='red' order by word";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			while (rs.next()) {
				String tag = rs.getString("word");
				populateCurationList(tagList, tag); //select tags for curation
			}
			return deduplicateSort(tagList);
		} catch (SQLException sqlexe) {
			LOGGER.error("Couldn't update sentence table in VolumeMarkupDbAccessor:updateData", sqlexe);
			sqlexe.printStackTrace();
			throw new ParsingException("Error Accessing the database" , sqlexe);
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}		
		}
    }


	private ArrayList<String> deduplicateSort(List<String> tagList) {
		HashSet<String> set = new HashSet<String>(tagList);
		String[] sorted = set.toArray(new String[]{}); 
		Arrays.sort(sorted);
		ArrayList<String> results = new ArrayList<String>();
		for(int i=0; i<sorted.length; i++){
			results.add(sorted[i]);
		}
		return results;
	}
    
    
    /**
     * display unknown terms in morestructure/moredescriptor subtabs 
     * in step 4 (perl markup) for curation.
     * @param curationList
     * @throws ParsingException
     * @throws SQLException
     */
    public ArrayList<String> contentTerms4Curation(List <String> curationList) throws  ParsingException, SQLException {
    	
    	PreparedStatement stmt = null;
    	ResultSet rs = null;
		 
	 	try {
			
			String sql = "select dhword from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("ALLWORDS")+
			" where inbrackets=0 and dhword not like '%\\_%' and " +
			" dhword not in (select word from "+ this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where saved_flag='red') and" +
			" dhword not in (select word from "+ this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+") order by dhword";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			while (rs.next()) {
				String word = rs.getString("dhword");
				populateCurationList(curationList, word);
			}
			return this.deduplicateSort(curationList);
		} catch (SQLException sqlexe) {
			LOGGER.error("Couldn't update sentence table in VolumeMarkupDbAccessor:contentTerms4Curation", sqlexe);
			sqlexe.printStackTrace();
			throw new ParsingException("Error Accessing the database" , sqlexe);
		} finally {
			if (rs != null) {
				rs.close();
			}
			if (stmt != null) {
				stmt.close();
			}		
		}
    }
    /**
     * if word in glossary, add it to wordroles
     * if not in glossary, add to curationList
     * @param curationList
     * @param word
     */
	private void populateCurationList(List<String> curationList, String word) {
		try{
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select category from "+this.glossarytable+" where term ='"+word+"'");
			if(rs.next()){
				String cat = rs.getString("category");
				if(cat.matches("("+ApplicationUtilities.getProperty("STRUCTURECATEGORYPATTERNINGLOSSARY")+")")){
					add2WordRolesTable(word, "op");
				}else{
					add2WordRolesTable(word, "c");
				}
			}else{
				curationList.add(word);
			}
		}catch(Exception sqlexe){
			LOGGER.error("Couldn't update sentence table in VolumeMarkupDbAccessor:populateCurationList", sqlexe);
			sqlexe.printStackTrace();
		}
	}

	/**
	 * load descriptor subtab in step 4 (perl markup)
	 * @return
	 * @throws SQLException
	 */
	public ArrayList<String> descriptorTerms4Curation() throws SQLException {
		
		ArrayList<String> words = new ArrayList<String>();
		
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
		
			//stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos=? and word not in (select distinct term from "+this.glossarytable+")");
			stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos=? and saved_flag !='red' order by word");
			stmt.setString(1, "b");
			rset = stmt.executeQuery();
			if (rset != null) {
				while(rset.next()){
					populateDescriptorList(words, rset.getString("word"));
				}	
			}
			words = deduplicateSort(words);
		} catch (SQLException exe) {
			LOGGER.error("Error in getting words as descriptors: " +
					"mainFormDbAccessor.descriptorTerms4Curation", exe);
			exe.printStackTrace();
		} finally {
			if(rset != null) {
				rset.close();
			}
			
			if(stmt != null) {
				stmt.close();
			}
			
		}
	return words;	
	}
	
	/**
	 * w is put into words only if 
	 * it is a word
	 * it is not a pronoun, a stopword, a preposition, an adv (-ly), or -shaped
	 * it is not in the glossary
	 * 
	 * if it is in the glossary, get its role from glossary and save it in wordroles table.
	 * @param words
	 * @param w
	 */
	private void populateDescriptorList(ArrayList<String> words, String w) {
		if(w.matches(".*?\\w.*")){
				String wc = w;
				if(w.indexOf("-")>=0 || w.indexOf("_")>=0){
					String[] ws = w.split("[_-]");
					w = ws[ws.length-1];
				}
				try {

					Statement stmt = conn.createStatement();
					ResultSet rset = stmt.executeQuery("select category from "+this.glossarytable+" where term ='"+w+"'");					 
					if(rset.next()){//in glossary
						String cat = rset.getString(1);
						if(cat.matches("\\b(STRUCTURE|FEATURE|SUBSTANCE|PLANT|nominative|structure)\\b")){
							add2WordRolesTable(wc, "os");
						}else{
							add2WordRolesTable(wc, "c");
						}
						
					}else{ //not in glossary
						words.add(wc);
					}
				} catch (SQLException exe) {
					LOGGER.error("Error in VolumeMarkupDbAccess.populateDescriptorList", exe);
					exe.printStackTrace();
				}
		}
	}

	/**
	 * 
	 * @param w
	 * @param role
	 */
	private void add2WordRolesTable(String w, String role) {
		try {

			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+" where word='"+w+"' and semanticrole='"+role+"'");
			if(!rs.next()){
				stmt.execute("insert into "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+" values ('"+w+"','"+role+"')");
			}
		} catch (SQLException exe) {
			LOGGER.error("Error in adding a word to wordroles table" +
					"mainFormDbAccessor.Add2WordRolesTable", exe);
			exe.printStackTrace();
		}				
	}


/**
 * called also by "load last project"	
 * @return
 * @throws SQLException
 */
public ArrayList<String> getSavedDescriptorWords() throws SQLException {
		
		ArrayList<String> words = new ArrayList<String>();
		
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			
			//Populate descriptor Hong TODO 5/23/11
			//stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_wordpos4parser where pos=? and word not in (select distinct term from "+this.glossarytable+") and saved_flag not in ('green','red')");
			stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos=? and word not in (select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+") and saved_flag not in ('red')");
			stmt.setString(1, "b");
			rset = stmt.executeQuery();
			if (rset != null) {
				while(rset.next()){
					words.add(rset.getString("word"));
				}	
			}			
		} catch (SQLException exe) {
			LOGGER.error("Error in getting words as descriptors: " +
					"mainFormDbAccessor.getDescriptorWords", exe);
		} finally {
			if(rset != null) {
				rset.close();
			}
			
			if(stmt != null) {
				stmt.close();
			}
			
		}
		
		return words;
	}
    public static void main(String[] args)throws Exception {
		// TODO Auto-generated method stub
		//System.out.println(DriverManager.getConnection(url));
	}

	public ArrayList<ArrayList> getUnSavedDescriptorWords() throws SQLException {
		
		ArrayList<String> words = new ArrayList<String>();
		ArrayList<String> flag = new ArrayList<String>();
		ArrayList<ArrayList> wordsAndFlag = new ArrayList<ArrayList>();
		
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
		
			//Populate descriptor Hong TODO 5/23/11
			//stmt = conn.prepareStatement("select word from "+this.tablePrefix+"_wordpos4parser where pos=? and word not in (select distinct term from "+this.glossarytable+") and saved_flag not in ('green','red')");
			stmt = conn.prepareStatement("select word,saved_flag from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+" where pos=? and word not in (select word from "+this.tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+")");
			stmt.setString(1, "b");
			rset = stmt.executeQuery();
			if (rset != null) {
				while(rset.next()){
					words.add(rset.getString("word"));
					flag.add(rset.getString("saved_flag"));
				}	
			}			
		} catch (SQLException exe) {
			LOGGER.error("Error in getting words as descriptors: " +
					"mainFormDbAccessor.getUnSavedDescriptorWords", exe);
			exe.printStackTrace();
		} finally {
			if(rset != null) {
				rset.close();
			}
			
			if(stmt != null) {
				stmt.close();
			}			
		}
		wordsAndFlag.add(words);
		wordsAndFlag.add(flag);
		return wordsAndFlag;
	}
}
