/**
 * MainFormDbAccessor.java
 *
 * Description : This performs all the database access needed by the MainForm
 * Version     : 1.0
 * @author     : Partha Pratim Sanyal
 * Created on  : Aug 29, 2009
 *
 * Modification History :
 * Date   | Version  |  Author  | Comments
 *
 * Confidentiality Notice :
 * This software is the confidential and,
 * proprietary information of The University of Arizona.
 */
package fna.db;

import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;


import fna.charactermarkup.Utilities;
import fna.parsing.ApplicationUtilities;
import fna.parsing.MainForm;
import fna.parsing.ParsingException;


@SuppressWarnings({ "unused"})
public class MainFormDbAccessor {


	private static final Logger LOGGER = Logger.getLogger(MainFormDbAccessor.class);
	private static Connection conn = null;
	 
	public static void main(String[] args) throws Exception {

		Connection conn = DriverManager.getConnection(url);
		System.out.println(conn);
		conn.close(); 
	}
	
	private static String url = ApplicationUtilities.getProperty("database.url");
	
	public MainFormDbAccessor(){
		//Statement stmt = null;
		//Connection conn = null;
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
			conn = DriverManager.getConnection(url);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in MainFormDbAccessor" + e);
			e.printStackTrace();
		} 
	}
	
	public void createNonEQTable(){
		//noneqterms table is refreshed for each data collection
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+MainForm.dataPrefixCombo.getText()+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE"));
			stmt.execute("create table if not exists "+MainForm.dataPrefixCombo.getText()+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE")+" (term varchar(100) not null, source varchar(200), savedid varchar(40))");
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void createTermCategoryTable(){
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+MainForm.dataPrefixCombo.getText()+"_term_category");
			stmt.execute("create table if not exists "+MainForm.dataPrefixCombo.getText()+"_term_category (term varchar(100), category varchar(200))");
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
}

	
	
	/**
	 * This method is used to remove the Bad structure names from Tab4, after they are marked RED,two steps are taken:
	 * First Step: Remove from the database (update the tag). Step Two: Keep the UI as it is with selected rows in Red color 
	 * @param removedTags: List of structures that should be removed
	 * @throws ParsingException
	 * @throws SQLException
	 */
	public void setUnknownTags(List <String> removedTags) throws ParsingException, SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		try {
			
				//Class.forName(driverPath);
				//conn = DriverManager.getConnection(url);
				String tablePrefix = MainForm.dataPrefixCombo.getText();
				String sql = "update "+tablePrefix+"_sentence set tag = 'unknown' where tag = ?";
				stmt = conn.prepareStatement(sql);
				
				for (String tag : removedTags) {
					stmt.setString(1, tag);
					stmt.executeUpdate();
				}
		} catch (SQLException sqlexe) {
			LOGGER.error("Couldn't update sentence table in MainFormDbAccessor:removeMarkUpData", sqlexe);
			sqlexe.printStackTrace();
			throw new ParsingException("Error Accessing the database" , sqlexe);
			
		} /*catch (ClassNotFoundException clexe) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:removeMarkUpData", clexe);
			throw new ParsingException("Couldn't load the db Driver" , clexe);
			
		} */finally {
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}			
			
		}
	}
	
	public void loadTagsData(Combo tagListCombo, Combo modifierListCombo) throws ParsingException, SQLException {
		ResultSet rs = null;
		PreparedStatement stmt = null;
		PreparedStatement stmt_select = null;
		//Connection conn = null;
		try {
			//Class.forName(driverPath);
			//conn = DriverManager.getConnection(url);
			String tablePrefix = MainForm.dataPrefixCombo.getText();
			String sql = "select distinct tag from "+tablePrefix+"_sentence where tag != 'unknown' and tag is not null order by tag asc";
			stmt = conn.prepareStatement(sql);
			
			rs = stmt.executeQuery();
			while (rs.next()) {
				String tag = rs.getString("tag");
				tagListCombo.add(tag);
			}
//changed 02/28 added modifier != ''
			sql = "select distinct modifier from "+tablePrefix+"_sentence where modifier is not null and modifier != '' order by modifier asc";
			stmt_select = conn.prepareStatement(sql);			
			rs = stmt_select.executeQuery();
			
			while (rs.next()) {
				String mod = rs.getString("modifier");
				modifierListCombo.add(mod);
			}
			

		} catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:loadTagsData", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} /*catch (ClassNotFoundException clex) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:loadTagsData", clex);
			throw new ParsingException("Couldn't load the db Driver" , clex);			
		}*/ finally {
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			if (stmt_select != null) {
				stmt_select.close();
			}
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
	}
	
	/**
	 * 
	 * @param tagTable
	 * @return # of records loaded
	 * @throws ParsingException
	 * @throws SQLException
	 */
	
	public int loadTagsTableData(Table tagTable) throws ParsingException, SQLException {
		
		PreparedStatement stmt = null;
		//Connection conn = null;
		ResultSet rs = null;
		
		try {
			//Class.forName(driverPath);
			//conn = DriverManager.getConnection(url);
			String tablePrefix = MainForm.dataPrefixCombo.getText();
			String sql = "select * from "+tablePrefix+"_sentence where tag = 'unknown' or isnull(tag) order by sentence";
			stmt = conn.prepareStatement(sql);
			
			int i = 0;
			rs = stmt.executeQuery();
			while (rs.next()) {
				String sentid = rs.getString("sentid");
				String tag = rs.getString("tag");
				//String sentence = rs.getString("sentence");
				String sentence = rs.getString("originalsent");
				TableItem item = new TableItem(tagTable, SWT.NONE);
			    item.setText(new String[]{++i+"", sentid,"", tag, sentence});
			}
			return i;

		} catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:loadTagsTableData", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} /*catch (ClassNotFoundException clex) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:loadTagsTableData", clex);
			throw new ParsingException("Couldn't load the db Driver" , clex);			
		}*/ finally {
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
	}
	
	/**
	 * show a complete character description ( 1 character + n states)
	 * @param sentid
	 * @param contextStyledText
	 * @throws SQLException
	 * @throws ParsingException
	 */
	public void updateContextData(int sentid, StyledText contextStyledText) throws SQLException, ParsingException {
		
		Statement stmt = conn.createStatement();
		ResultSet rs = null;		
		
		try {
			String tablePrefix = MainForm.dataPrefixCombo.getText();
			String sql = "select source from "+tablePrefix+"_sentence where sentid ="+sentid;
			rs = stmt.executeQuery(sql);
			String characterid="";
			String source = "";
			if(rs.next()){
				source = rs.getString("source");
				//Buckup_1998.xml_088683b8-4718-48de-ad0e-eb1de9c58eb6_ae45e0c9-0753-4dff-ab3c-de1860a0c81e.txt-0
				characterid = source.replaceFirst("^.*?xml_", "").replaceFirst("_.*$", "");				
			}
			
			sql = "select * from "+tablePrefix+"_sentence where source like '%"+characterid+"%'";
			rs = stmt.executeQuery(sql);
			while (rs.next()) {
				String sid = rs.getString("sentid");
				String tag = rs.getString("tag");
				//String sentence = rs.getString("sentence");
				String sentence = rs.getString("originalsent");
				sentence+= "["+source+"]";
				int start = contextStyledText.getText().length();
				
				contextStyledText.append(sentence + "\r\n");
				if (Integer.parseInt(sid) == sentid) {
					StyleRange styleRange = new StyleRange();
					styleRange.start = start;
					styleRange.length = sentence.length();
					styleRange.fontStyle = SWT.BOLD;
					// styleRange.foreground = display.getSystemColor(SWT.COLOR_BLUE);
					contextStyledText.setStyleRange(styleRange);
				}
			}

		} catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:updateContextData", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} /*catch (ClassNotFoundException clex) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:updateContextData", clex);
			throw new ParsingException("Couldn't load the db Driver" , clex);			
		}*/ finally {
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
	}
	
	/**
	 * This is used when Save is clicked on Step5.
	 * @param tagTable
	 * @throws ParsingException
	 * @throws SQLException
	 */
	public void saveTagData(Table tagTable) throws ParsingException, SQLException{
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		PreparedStatement stmt_update = null;
		ResultSet rs = null;
		
		try {
			//Class.forName(driverPath);
			//conn = DriverManager.getConnection(url);
			
			
			for (TableItem item : tagTable.getItems()) {
				String sentid = item.getText(1);
				String modifier = item.getText(2);
				String tag = item.getText(3);
				
				if (tag.equals("unknown"))
					continue;
				
				if(tag.equals("PART OF LAST SENTENCE")){//find tag of the last sentence
					String tablePrefix = MainForm.dataPrefixCombo.getText();
					String sql = "select tag from "+tablePrefix+"_sentence where sentid ="+(Integer.parseInt(sentid)-1);
					stmt = conn.prepareStatement(sql);
					rs = stmt.executeQuery();
					rs.next();
					tag = rs.getString("tag");
				}
				String tablePrefix = MainForm.dataPrefixCombo.getText();
				String sql = "update "+tablePrefix+"_sentence set modifier = ?, tag = ? where sentid = ?";
				stmt_update = conn.prepareStatement(sql);
				stmt_update.setString(1, modifier);
				stmt_update.setString(2, tag);
				stmt_update.setString(3, sentid);
				
				stmt_update.executeUpdate();
			}

		} catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:saveTagData", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} finally {
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			if (stmt_update != null) {
				stmt_update.close();
			}
			
			//if (conn != null) {
			//	conn.close();
		//	}
						
		}
	}
	
	//added March 1st
	public void glossaryPrefixRetriever(List <String> datasetPrefixes) 
	throws ParsingException, SQLException{
		//Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		try {
			//conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			rset = stmt.executeQuery("SELECT table_name FROM information_schema.tables where table_schema ='"+ApplicationUtilities.getProperty("database.name")+"' and table_name like '%glossaryfixed'");
			while (rset.next()) {
				datasetPrefixes.add(rset.getString("table_name"));
			}
		}catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:datasetPrefixRetriever", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
						
		}
		
		
	}
	
	//added March 1st ends
	public void datasetPrefixRetriever(List <String> datasetPrefixes) 
		throws ParsingException, SQLException{
		
		//Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		
		/*String createprefixTable = "CREATE TABLE  if not exists datasetprefix (" +
				 "prefix varchar(20) NOT NULL DEFAULT '', "+
				  "time_last_accessed timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
				  "tab_general varchar(1) DEFAULT NULL, "+
				  "tab_segm varchar(1) DEFAULT NULL, "+
				  "tab_verf varchar(1) DEFAULT NULL, "+
				  "tab_trans varchar(1) DEFAULT NULL, "+
				  "tab_struct varchar(1) DEFAULT NULL, "+
				  "tab_unknown varchar(1) DEFAULT NULL, "+
				  "tab_finalm varchar(1) DEFAULT NULL, "+
				  "tab_gloss varchar(1) DEFAULT NULL, "+
				  "glossary varchar(40) DEFAULT NULL, "+
				  "option_chosen varchar(1) DEFAULT '', "+
				  "PRIMARY KEY (prefix, time_last_accessed) ) " ; */
		
		String createprefixTable = "CREATE TABLE  if not exists datasetprefix (" +
				 "prefix varchar(20) NOT NULL DEFAULT '', "+
				  "time_last_accessed timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
				  "tab_general varchar(1) DEFAULT NULL, "+
				  "tab_trans varchar(1) DEFAULT NULL, "+
				  "tab_struct varchar(1) DEFAULT NULL, "+
				  "tab_finalm varchar(1) DEFAULT NULL, "+
				  "glossary varchar(40) DEFAULT NULL, "+
				  "option_chosen varchar(1) DEFAULT '', "+
				  "PRIMARY KEY (prefix, time_last_accessed) ) " ; 
		
		try {
			//conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			stmt.execute(createprefixTable);
			rset = stmt.executeQuery("select * from datasetprefix order by time_last_accessed desc");
			while (rset.next()) {
				datasetPrefixes.add(rset.getString("prefix"));//collects all the prefixes used in this system
			}
		}catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:datasetPrefixRetriever", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
						
		}
		
		
	}
	

	
	public void savePrefixData(String prefix, String glossaryName, int optionChosen) 
	throws ParsingException, SQLException{
	
	//Connection conn = null;
	PreparedStatement stmt = null;
	ResultSet rset = null;
	
	try {
		if (!prefix.equals("")) {

			stmt = conn.prepareStatement("select prefix from datasetprefix where prefix='"+prefix+"'");
			rset=stmt.executeQuery();
			if(rset.next()){
				//stmt = conn.prepareStatement("update datasetprefix set time_last_accessed = current_timestamp, tab_general = 1,tab_segm=1," +
				//		"tab_verf =1,tab_trans =1,tab_struct =1,tab_unknown =1,tab_finalm =1,tab_gloss =1,glossary= '" +glossaryName+"',option_chosen='"+optionChosen+"' where prefix='"+prefix+"'");

				//stmt = conn.prepareStatement("update datasetprefix set time_last_accessed = current_timestamp, tab_general = 1," +
				//		"tab_trans =0,tab_struct =0,tab_finalm =0,glossary= '" +glossaryName+"',option_chosen='"+optionChosen+"' where prefix='"+prefix+"'");
				stmt = conn.prepareStatement("update datasetprefix set time_last_accessed = current_timestamp, tab_general = 1," +
						"glossary= '" +glossaryName+"',option_chosen='"+optionChosen+"' where prefix='"+prefix+"'");
				stmt.executeUpdate();
			}
			else
			{
				stmt = conn.prepareStatement("insert into datasetprefix values ('"+ 
						prefix + "', current_timestamp, 1, 0, 0 ,0,'"+glossaryName+"','"+optionChosen+"')");

				//stmt = conn.prepareStatement("insert into datasetprefix values ('"+ 
				//		prefix + "', current_timestamp, 1, 1, 1 ,1, 1, 1, 1, 1,'"+glossaryName+"','"+optionChosen+"')");
				//changed insert from 0 to 1 by Prasad, since the remaining code is taking 1 as unprocessed 
				stmt.executeUpdate();
			}
		}
	}catch (SQLException exe) {
		
		if (exe.getMessage().contains("key 'PRIMARY'")) {
			stmt = conn.prepareStatement("update datasetprefix set time_last_accessed = current_timestamp" +
					" where prefix = '" + prefix + "'" ); 
			stmt.executeUpdate();
		}
		if (!exe.getMessage().contains("key 'PRIMARY'")) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:savePrefixdata", exe);
			throw new ParsingException("Failed to execute the statement.", exe);
		}
		
	} finally {
		if (rset != null) {
			rset.close();
		}
		
		if (stmt != null) {
			stmt.close();
		}
		
		//if (conn != null) {
		//	conn.close();
		//}
		
		
					
	}

 }
	public void loadStatusOfMarkUp(boolean [] markUpStatus, String dataPrefix) 
		throws 	ParsingException, SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		try {
			if(dataPrefix != null && !dataPrefix.equals("")) {
			//	conn = DriverManager.getConnection(url);
				stmt = conn.prepareStatement("select * from datasetprefix where prefix ='" + dataPrefix + "'");
				rset = stmt.executeQuery();
				
				if (rset != null && rset.next()) {
					
					/* Segmentation tab 
					if (rset.getInt("tab_segm") == 0) {
						markUpStatus[1] = false;
					} else {
						markUpStatus[1] = true;
					}*/
					
					/* Verification tab 
					if (rset.getInt("tab_verf") == 0) {
						markUpStatus[2] = false;
					} else {
						markUpStatus[2] = true;
					}*/
					
					/* Transformation tab 
					if (rset.getInt("tab_trans") == 0) {
						markUpStatus[3] = false;
					} else {
						markUpStatus[3] = true;
					}*/
					
					/* Transformation tab */
					if (rset.getInt("tab_trans") == 0) {
						markUpStatus[1] = false;
					} else {
						markUpStatus[1] = true;
					}
					
					/* Structure Name Correction tab 
					if (rset.getInt("tab_struct") == 0) {
						markUpStatus[4] = false;
					} else {
						markUpStatus[4] = true;
					}*/
					
					if (rset.getInt("tab_struct") == 0) {
						markUpStatus[2] = false;
					} else {
						markUpStatus[2] = true;
					}
					
					/* Unknown removal tab 
					if (rset.getInt("tab_unknown") == 0) {
						markUpStatus[5] = false;
					} else {
						markUpStatus[5] = true;
					}*/
					
					/* Finalizer tab 
					if (rset.getInt("tab_finalm") == 0) {
						markUpStatus[6] = false;
					} else {
						markUpStatus[6] = true;
					}*/
					
					if (rset.getInt("tab_finalm") == 0) {
						markUpStatus[3] = false;
					} else {
						markUpStatus[3] = true;
					}
					
					/* Glossary tab 
					if (rset.getInt("tab_gloss") == 0) {
						markUpStatus[7] = false;
					} else {
						markUpStatus[7] = true;
					}*/
					
					
				}
			}
		} catch (Exception exe) {
			LOGGER.error("Error loading saved status",exe);
			exe.printStackTrace();
		} finally {
			
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
	//		if (conn != null) {
		//		conn.close();
			//}
			
		}
		

	}
	
	public void saveStatus(String tab, String prefix, boolean status) throws SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		int tabStatus = 0;
		//Lookup
		{
			/*if (tab.equals(ApplicationUtilities.getProperty("tab.one.name"))) {
				tab = "tab_general";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.two.name"))) {
				tab = "tab_segm";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.three.name"))) {
				tab = "tab_verf";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.four.name"))) {
				tab = "tab_trans";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.five.name"))) {
				tab = "tab_struct";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.six.name"))) {
				tab = "tab_unknown";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.seven.name"))) {
				tab = "tab_finalm";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.eight.name"))) {
				tab = "tab_gloss";
			}*/
			
			if (tab.equals(ApplicationUtilities.getProperty("tab.one.name"))) {
				tab = "tab_general";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.two.name"))) {
				tab = "tab_trans";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.three.name"))) {
				tab = "tab_struct";
			}
			if (tab.equals(ApplicationUtilities.getProperty("tab.four.name"))) {
				tab = "tab_finalm";
			}

		}
		
		 if (status == true) {
			 tabStatus = 1;
			 //tabStatus = 0;//changed to 0 by Prasad. if status is 0 that means processed and can be loaded
			 //status of 1 means yet to be loaded
		 } 
		
		try {
			//conn = DriverManager.getConnection(url);
			stmt = conn.prepareStatement("update datasetprefix set " + tab + "= " + tabStatus + 
					" where prefix='" + prefix +"'");
			stmt.executeUpdate();
			
		} catch (Exception exe) {
			LOGGER.error("Unable to save status", exe);
			exe.printStackTrace();
		} finally {
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}
		
	}
	

	
	
	public ArrayList<String> getUnknownWords()throws SQLException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rset = null;
		ArrayList<String> unknownWords = new ArrayList<String>();
		String tablePrefix = MainForm.dataPrefixCombo.getText();
		try{
			//conn = DriverManager.getConnection(url);
			stmt = conn.prepareStatement("select word from "+tablePrefix+"_unknownwords " +
					"where flag = ?");
			stmt.setString(1, "unknown");
			rset = stmt.executeQuery();
			if(rset != null) {
				while(rset.next()) {
					unknownWords.add(rset.getString("word"));
				}
			}
		} catch (Exception exe) {
			LOGGER.error("Error getting unknown words",exe);
			exe.printStackTrace();
		} finally {
			
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
				//conn.close();
			//}
			
		}		
		
		return unknownWords;
	}
	
	/** 
	 * This function will save terms from the Markup - (Structure tab) to database
	 * @param terms
	 */
	public void saveTermRole
		(ArrayList<String> terms, String role, UUID last, UUID current)throws SQLException {
		//Connection conn = null;
		PreparedStatement pstmt = null;
		String tablePrefix = MainForm.dataPrefixCombo.getText();
		try {
			//conn = DriverManager.getConnection(url);
			String wordrolesable = tablePrefix+ "_"+ApplicationUtilities.getProperty("WORDROLESTABLE");
			Statement stmt = conn.createStatement();
			stmt.execute("delete from "+wordrolesable+" where savedid='"+last.toString()+"'");
			stmt.close();
			pstmt = conn.prepareStatement("Insert into "+wordrolesable+" (word,semanticrole, savedid) values (?,?, ?)");
			//stmt = conn.prepareStatement("Update "+postable+" set saved_flag ='green' where word = ?");
			for (String term : terms) {
				pstmt.setString(1, term);
				pstmt.setString(2, role);	
				pstmt.setString(3, current.toString());
				try {
					pstmt.execute();
				} catch (Exception exe) {
				 if (!exe.getMessage().contains("Duplicate entry")) {
					 throw exe;
				 }
				}			
			}
			//stmt.executeBatch();
		} catch (Exception exe) {
			LOGGER.error("Error saving structure names from markup tab",exe);
			exe.printStackTrace();
		} finally {
			
			if (pstmt != null) {
				pstmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}
			
		}		
	}

	public void recordNonEQTerms(ArrayList<String> words, UUID last, UUID current) throws SQLException {
		//Connection conn = null;
		PreparedStatement pstmt = null ;
		String tablePrefix = MainForm.dataPrefixCombo.getText();
		try {
			Statement stmt = conn.createStatement();
			if(last!=null){
				//clean up last saved info
				stmt.execute("update "+tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag = '' where savedid='"+last.toString()+"'");
			}
			if(current==null){
			//set flag in pos table
				//pstmt = conn.prepareStatement("update "+tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag ='red' where pos=? and word=?");
				pstmt = conn.prepareStatement("update "+tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag ='red' where word=?");
			}else{
				//pstmt = conn.prepareStatement("update "+tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag ='red', savedid='"+current.toString()+"' where pos=? and word=?");
				pstmt = conn.prepareStatement("update "+tablePrefix+"_"+ApplicationUtilities.getProperty("POSTABLE")+ " set saved_flag ='red', savedid='"+current.toString()+"' where word=?");
			}
			for (String word : words) {
				//pstmt.setString(1, "b");
				//pstmt.setString(2, word);
				pstmt.setString(1, word);
				pstmt.addBatch();
			}
			pstmt.executeBatch();
			//insert words in noneqterms table	
			//clean up last saved info
			if(last!=null){
				stmt.execute("delete from "+tablePrefix+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE")+ " where savedid='"+last.toString()+"'");
			}
			if(last!=null){
				pstmt = conn.prepareStatement("insert into "+tablePrefix+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE")+ "(term, source, savedid) values(?, ?, ?)");
			}else{
				pstmt = conn.prepareStatement("insert into "+tablePrefix+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE")+ "(term, source) values(?, ?)");
			}
			for (String word : words) {
					pstmt.setString(1, word);
					pstmt.setString(2, tablePrefix);
					if(current!=null) pstmt.setString(3, current.toString());
					pstmt.addBatch();
			}
			pstmt.executeBatch();
		} catch (SQLException exe){
			LOGGER.error("Exception in RemoveDescriptorData", exe);
			exe.printStackTrace();
		} finally {
			if (pstmt != null) {
				pstmt.close();
			}
			//if (conn != null) {
			//	conn.close();
			//}			
			
		}
	}
	
	
	
	public void createWordRoleTable(){
		//Connection conn = null;
		Statement stmt = null ;
		String tablePrefix = MainForm.dataPrefixCombo.getText();
		try {
			//conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			stmt.execute("drop table if exists "+tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE"));
			stmt.execute("create table if not exists "+tablePrefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+ " (word varchar(50), semanticrole varchar(2), savedid varchar(40), primary key(word, semanticrole))");			
		} catch (SQLException exe){
			LOGGER.error("Exception in MainFormDbAccessor", exe);
			exe.printStackTrace();
		} finally {
			try{
			if (stmt != null) {
				stmt.close();
			}
			
			//if (conn != null) {
			//	conn.close();
			//}		
			}catch(Exception e){
				LOGGER.error("Exception in MainFormDbAccessor", e);
				e.printStackTrace();
			}
			
		}
	}

	//added newly to load the styled context for step 4 (all 4 sub-tabs)
	public void getContextData(String word,StyledText context) throws SQLException, ParsingException {
		
		//Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		
		try {
			//Class.forName(driverPath);
			//conn = DriverManager.getConnection(url);
			String tablePrefix = MainForm.dataPrefixCombo.getText();
			String sql = "select source,originalsent from "+tablePrefix+"_sentence where sentence like '% "+word+" %' or sentence like '"+word+" %' or sentence like '% "+word+"' or sentence ='"+word+"' or tag = '"+word+"'";
			stmt = conn.prepareStatement(sql);
			rs = stmt.executeQuery();
			context.cut();
			String text = "";
			int count = 0;
			while (rs.next()) { //collect sentences
				count++;
				String src = rs.getString("source");
				String sentence = rs.getString("originalsent");
				text += count+": "+sentence+" ["+src+"] \r\n";
				//System.out.println(src+"::"+sentence+" \r\n");
				//context.append(src+"::"+sentence+" \r\n");
			}	
			//format sentences
			ArrayList<StyleRange> srs = new ArrayList<StyleRange>();
			String[] tokens = text.split("\\s");
			int currentindex = 0;
			for(String token: tokens){
				if(token.contains(word)){
					StyleRange sr = new StyleRange();
					sr.start = currentindex;
					sr.length = word.length();
					sr.fontStyle = SWT.BOLD;
					srs.add(sr);
				}else if(token.matches("^\\[.*?\\]$")){
					StyleRange sr = new StyleRange();
					sr.start = currentindex;
					sr.length = token.length();
					sr.foreground = MainForm.grey;
					srs.add(sr);
				}
				currentindex +=token.length() + 1;				
			}
			context.append(text);
			context.setStyleRanges(srs.toArray(new StyleRange[]{}));
		} catch (SQLException exe) {
			LOGGER.error("Couldn't execute db query in MainFormDbAccessor:updateContextData", exe);
			exe.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", exe);
		} /*catch (ClassNotFoundException clex) {
			LOGGER.error("Couldn't load the db Driver in MainFormDbAccessor:updateContextData", clex);
			throw new ParsingException("Couldn't load the db Driver" , clex);			
		}*/ finally {
			if (rs != null) {
				rs.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}
			
		//	if (conn != null) {
			//	conn.close();
			//}
			
		}
	}
	
	/**
	 * merge grouped_terms and group_decision table and add data into 
	 * term_category table, which may already have data
	 * also add newly learned structure term to the table for category "structure"
	 * This makes term_category contain all new terms learned from a volume of text
	 */
	public void finalizeTermCategoryTable() {
		String prefix = MainForm.dataPrefixCombo.getText();
		try{
			Statement stmt = conn.createStatement();
			//skipped term categorization step so don't need this
			/*String q = "select distinct groupId, category from "+ prefix+"_group_decisions where category !='done'"; //"done" was a fake decision for unpaired terms
			ResultSet rs = stmt.executeQuery(q);
			while(rs.next()){
				int gid = rs.getInt(1);
				String cat = rs.getString(2);
				Statement stmt2 = conn.createStatement();
				ResultSet rs2 = stmt2.executeQuery("select term, cooccurTerm from "+prefix+"_grouped_terms where groupId ="+gid);
				while(rs2.next()){
					String t1 = rs2.getString(1);
					String t2 = rs2.getString(2);
					insert2TermCategoryTable(t1, cat);
					if(t2!=null && t2.trim().length()>0) insert2TermCategoryTable(t2, cat);
				}
			}*/
			//simply add "c" terms as "feature" in term_category table
			String q = "select distinct word from "+prefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+" where semanticrole in ('c') and " +
					" word not in (select distinct term from "+MainForm.glossaryPrefixCombo.getText()+" where category not in ('STRUCTURE', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative', 'structure'))";
			ResultSet rs = stmt.executeQuery(q);
			while(rs.next()){
				String t = rs.getString(1);
				Utilities.insert2TermCategoryTable(t, "feature", conn, MainForm.dataPrefixCombo.getText().trim());
			}
			//insert structure terms
			q = "select distinct word from "+prefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+" where semanticrole in ('op', 'os') and " +
					" word not in (select distinct term from "+MainForm.glossaryPrefixCombo.getText()+" where category in ('STRUCTURE', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative', 'structure'))";
			rs = stmt.executeQuery(q);
			while(rs.next()){
				String t = rs.getString(1);
				Utilities.insert2TermCategoryTable(t, "structure", conn, MainForm.dataPrefixCombo.getText().trim());
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	


	public void createPrepphraseTable() {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+MainForm.dataPrefixCombo.getText()+"_"+ApplicationUtilities.getProperty("NONEQTERMSTABLE"));
			stmt.execute("create table if not exists "+MainForm.dataPrefixCombo.getText()+"_prepphrases (phrase varchar(100))");
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		

		
	}
	
	


}
