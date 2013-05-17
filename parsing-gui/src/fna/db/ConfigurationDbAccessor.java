package fna.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Text;

import fna.beans.ExpressionBean;
import fna.beans.NomenclatureBean;
import fna.beans.SectionBean;
import fna.beans.SpecialBean;
import fna.beans.TextBean;
import fna.beans.Type2Bean;
import fna.parsing.ApplicationUtilities;
import fna.parsing.Type2Document;


@SuppressWarnings({ "unused" })
public class ConfigurationDbAccessor {

	/**
	 * @param args
	 */
	private static final Logger LOGGER = Logger.getLogger(ConfigurationDbAccessor.class);
	private static String url = ApplicationUtilities.getProperty("database.url");
	
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
			Connection conn = DriverManager.getConnection(url);
			Statement stmt = conn.createStatement();
			stmt.execute("create table if not exists configtags (tagname varchar(200), marker varchar(100), startStyle varchar(100), primary key (tagname))");
			stmt.execute("DROP TABLE IF EXISTS `type4startparagraph`");
			stmt.execute("CREATE TABLE type4startparagraph (`tagid` int(5) NOT NULL AUTO_INCREMENT, `paragraph` mediumtext CHARACTER SET utf8, `docformat` varchar(50) DEFAULT NULL, PRIMARY KEY (`tagid`))");
			stmt.execute("DROP TABLE IF EXISTS `ocrstartparagraph`");
			stmt.execute("CREATE TABLE `ocrstartparagraph` (`tagid` int(5) NOT NULL AUTO_INCREMENT, `paragraph` mediumtext, PRIMARY KEY (`tagid`) )");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in ConfigurationDbAccessor" + e);
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	
	public void retrieveTagDetails(List <String> tags) throws Exception {
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet rset = null;
		int i = 0;
		try {
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			rset = stmt.executeQuery("SELECT tagname FROM configtags c order by tagname;");
			while(rset.next()) {
				tags.add(rset.getString("tagname"));
			}
			
			
		} catch (Exception exe) {
			
			LOGGER.error("Couldn't retrieve Tag Details in ConfigurationDbAccessor" + exe);
			exe.printStackTrace();
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (stmt != null) {
				stmt.close();
			}

			if (conn != null) {
				conn.close();
			}
		}
		
	}
	
	public void saveSemanticTagDetails(HashMap <Integer, Combo> comboFields, 
			HashMap <Integer, Button> checkFields) throws SQLException{
		Connection conn = null;
		Statement stmt = null;
		
		int count = comboFields.size();
		try {
			conn = DriverManager.getConnection(url);
			stmt = conn.createStatement();
			for (int i= 1; i<= count; i++) {
				Integer key = new Integer(i);
				Combo combo = comboFields.get(key);
				String tagName = combo.getText().trim();
				Button button = checkFields.get(key);
				boolean checked = button.getSelection();
				if(!tagName.equals("")) {
					if(checked) {
						stmt.executeUpdate("insert into configtags (tagname, marker, startStyle) values " +
								"('" + tagName + "', 'U', 'Y');");
					} else {
						stmt.executeUpdate("insert into configtags (tagname, marker) values " +
								"('" + tagName + "', 'U');");
					}

				}
			}

		} catch (Exception exe) {
			if(!exe.getMessage().contains("Duplicate entry")) {
			 LOGGER.error("Couldn't insert Tag Details in ConfigurationDbAccessor:saveSemanticTagDetails" + exe);
			 exe.printStackTrace();
			}
		} finally {

			if (stmt != null) {
				stmt.close();
			}

			if (conn != null) {
				conn.close();
			}
		}

	}
	
	
	public void saveParagraphTagDetails(String docFormat, String...paragraphs) throws SQLException{
		Connection conn = null;
		PreparedStatement stmt = null;
		String tableName = "ocrstartparagraph";
		
		if (docFormat != null) {
			tableName = "type4startparagraph";
		}

		try {
			conn = DriverManager.getConnection(url);
			stmt = conn.prepareStatement("delete from " + tableName);
			stmt.execute();
			
			if (docFormat != null) {
				stmt = conn.prepareStatement("insert into "+ tableName+"(paragraph, docformat) values (?,?)");
			} else {
				stmt = conn.prepareStatement("insert into "+ tableName+"(paragraph) values (?)");
			}
			

			for (String para : paragraphs) {
				if(!para.trim().equals("") && !para.trim().equals("\r")) {
					if (docFormat != null) {
						stmt.setString(1, para);
						stmt.setString(2, docFormat);
					} else {
						stmt.setString(1, para);
					}					
					stmt.executeUpdate();
				}
			}

		} catch (Exception exe) {
			 LOGGER.error("Couldn't insert paragraph Details in ConfigurationDbAccessor:saveParagraphTagDetails" + exe);
			 exe.printStackTrace();
		} finally {

			if (stmt != null) {
				stmt.close();
			}

			if (conn != null) {
				conn.close();
			}
		}

	}
	
	public boolean saveType2Details(Type2Bean bean) throws SQLException {
		

		PreparedStatement pstmt = null;
		Connection conn = null;
		boolean success = false;
		
		try {
			conn = DriverManager.getConnection(url);
			/* Insert the data from the first tab 
			 * use TextBean
			 * */
			pstmt = conn.prepareStatement("delete from configtype2text");
			pstmt.execute();
			String query = "insert into configtype2text (firstpara, leadingIntend, spacing, avglength, pgNoForm," +
					"capitalized, allcapital, sectionheading, hasfooter, hasHeader, footerToken, headertoken) " +
					"values (?,?,?,?,?,?,?,?,?,?,?,?)";
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, bean.getTextBean().getFirstPara().getText());
			pstmt.setString(2, bean.getTextBean().getLeadingIndentation().getText());
			pstmt.setString(3, bean.getTextBean().getSpacing().getText());
			pstmt.setString(4, bean.getTextBean().getEstimatedLength().getText());
			pstmt.setString(5, bean.getTextBean().getPageNumberFormsText().getText());
			pstmt.setString(6, bean.getTextBean().getSectionHeadingsCapButton().getSelection()?"Y":"N");
			pstmt.setString(7, bean.getTextBean().getSectionHeadingsAllCapButton().getSelection()?"Y":"N");
			pstmt.setString(8, bean.getTextBean().getSectionHeadingsText().getText());
			SpecialBean  splBean = bean.getTextBean().getFooterHeaderBean();
			pstmt.setString(9, splBean.getFirstButton().getSelection()?"Y":"N");
			pstmt.setString(10, splBean.getSecondButton().getSelection()?"Y":"N");
			pstmt.setString(11, splBean.getFirstText().getText());
			pstmt.setString(12, splBean.getSecondText().getText());			
			pstmt.execute();
			
			/* Save Nomenclature tab now - Use nomenclatures*/
			pstmt = conn.prepareStatement("delete from nomenclatures");
			pstmt.execute();
			query = "insert into nomenclatures (nameLabel, _yes, _no, description, _type) values (?,?,?,?,?)";
			Set <Integer> keys = bean.getNomenclatures().keySet();
			String type1 = ApplicationUtilities.getProperty("Type1");
			String type2 = ApplicationUtilities.getProperty("Type2");
			String type3 = ApplicationUtilities.getProperty("Type3");
			pstmt = conn.prepareStatement(query);
			for (Integer i : keys) {
				NomenclatureBean nBean = bean.getNomenclatures().get(i);
				pstmt.setString(1, nBean.getLabel().getText());
				pstmt.setString(2, nBean.getYesRadioButton().getSelection()?"Y":"N");
				pstmt.setString(3, nBean.getNoRadioButton().getSelection()?"Y":"N");
				pstmt.setString(4, nBean.getDescription().getText());
				int offset = i.intValue()%3;
				switch(offset) {
				 case 0 : pstmt.setString(5, type1);
				 		  break;
				 case 1 : pstmt.setString(5, type2);
				 	      break;
				 case 2 : pstmt.setString(5, type3);
		 	      		  break;
				}
				
				pstmt.execute();
			}
			
			/* Save the data in Expression tab - use expressions*/
			pstmt = conn.prepareStatement("delete from expressions");
			pstmt.execute();
			query = "insert into expressions (_label, description) values (?,?)";
			keys = bean.getExpressions().keySet();
			pstmt = conn.prepareStatement(query);
			for (Integer i : keys) {
				ExpressionBean expBean = bean.getExpressions().get(i);
				pstmt.setString(1, expBean.getLabel().getText());
				pstmt.setString(2, expBean.getText().getText());
				pstmt.execute();
			}
			
			/*Save morphological descriptions  - use descriptionBean */
			pstmt = conn.prepareStatement("delete from morpdesc");
			pstmt.execute();
			
			query = "insert into morpdesc values(?,?)";
			pstmt = conn.prepareStatement(query);			
			pstmt.setString(1, bean.getDescriptionBean().getYesButton().getSelection()?"Y":"N");
			pstmt.setString(2, bean.getDescriptionBean().getOtherInfo().getText());			
			pstmt.execute();
			
			pstmt = conn.prepareStatement("delete from descriptions");
			pstmt.execute();
			
			query = "insert into descriptions (_order, section, start_token, end_token, embedded_token) values(?,?,?,?,?)";
			pstmt = conn.prepareStatement(query);
			HashMap <Integer, SectionBean> descriptions = bean.getDescriptionBean().getSections();
			keys = descriptions.keySet();
			
			for(Integer i : keys) {
				SectionBean  secBean = descriptions.get(i);
				pstmt.setString(1, secBean.getOrder().getText());
				pstmt.setString(2, secBean.getSection().getText());
				pstmt.setString(3, secBean.getStartTokens().getText());
				pstmt.setString(4, secBean.getEndTokens().getText());
				pstmt.setString(5, secBean.getEmbeddedTokens().getText());
				pstmt.execute();
			}
			
			/* Save Special tab data - use SpecialBean */
			pstmt = conn.prepareStatement("delete from specialsection");
			pstmt.execute();
			
			query = "insert into specialsection(hasGlossary,glossaryHeading," +
					"hasReference,referenceHeading) values (?,?,?,?)";
			pstmt = conn.prepareStatement(query);
			pstmt.setString(1, bean.getSpecial().getFirstButton().getSelection()?"Y":"N");
			pstmt.setString(2, bean.getSpecial().getFirstText().getText());
			pstmt.setString(3, bean.getSpecial().getSecondButton().getSelection()?"Y":"N");
			pstmt.setString(4, bean.getSpecial().getSecondText().getText());
			pstmt.execute();
			
			/* Save the abbreviations tab data - use abbreviations */
			pstmt = conn.prepareStatement("delete from abbreviations");
			pstmt.execute();
			
			query = "insert into abbreviations (_label, abbreviation) values(?,?)";
			
			pstmt = conn.prepareStatement(query);
			Set <String> keySet = bean.getAbbreviations().keySet();
			for (String name: keySet) {
				pstmt.setString(1, name);
				pstmt.setString(2, bean.getAbbreviations().get(name).getText());
				pstmt.execute();
			}
			success = true;
			
		} catch (SQLException exe) {
			LOGGER.error("Couldn't insert type2 Details in ConfigurationDbAccessor:saveType2Details" + exe);
			exe.printStackTrace();
			
		} finally {
			if (pstmt != null) {
				pstmt.close();
			} 
			if (conn != null) {
				conn.close();
			}
		}
		
		
		
		return success;
	}
	
	public void retrieveType2Details(Type2Bean bean, Type2Document type2) throws SQLException {
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rset = null;
		try {
			conn = DriverManager.getConnection(url);
			
			/* Retrieve Text tab*/
			
			pstmt = conn.prepareStatement("select * from configtype2text");
			rset = pstmt.executeQuery();
			if(rset.next()) {
				TextBean textBean = bean.getTextBean();
				textBean.getFirstPara().setText(rset.getString("firstpara"));
				textBean.getLeadingIndentation().setText(rset.getString("leadingIntend"));
				textBean.getSpacing().setText(rset.getString("spacing"));
				textBean.getEstimatedLength().setText(rset.getString("avglength"));
				textBean.getPageNumberFormsText().setText(rset.getString("pgNoForm"));
				textBean.getSectionHeadingsCapButton().setSelection(rset.getString("capitalized").equals("Y")?true:false);
				textBean.getSectionHeadingsAllCapButton().setSelection(rset.getString("allcapital").equals("Y")?true:false);
				textBean.getSectionHeadingsText().setText(rset.getString("sectionheading"));
				textBean.getFooterHeaderBean().getFirstButton().setSelection(
						rset.getString("hasfooter").equals("Y")?true:false);
				textBean.getFooterHeaderBean().getSecondButton().setSelection(
						rset.getString("hasHeader").equals("Y")?true:false);
				textBean.getFooterHeaderBean().getFirstText().setText(rset.getString("footerToken"));
				textBean.getFooterHeaderBean().getSecondText().setText(rset.getString("headertoken"));
			}
			
			/* Retrieve nomenclature tab */
			
			pstmt = conn.prepareStatement("select * from nomenclatures");
			rset = pstmt.executeQuery();
			int count = 0;
			int groupCount = 0;
			HashMap <Integer, NomenclatureBean> nomenclatures = bean.getNomenclatures();
			int size = nomenclatures.size();
			NomenclatureBean nbean = null;
			while(rset.next()){				
				size = nomenclatures.size();
				if(groupCount%3 == 0) {
					count++;
				}
				if(count > size/3){
					type2.addNomenclatureRow(rset.getString("nameLabel"));
				}
				nbean = nomenclatures.get(new Integer(groupCount));
				nbean.getYesRadioButton().setSelection(rset.getString("_yes").contains("Y")?true:false);
				nbean.getNoRadioButton().setSelection(rset.getString("_no").contains("Y")?true:false);
				nbean.getDescription().setText(rset.getString("description"));				
				groupCount++;				
			}
			
			/* Retrieve expressions tab */
			
			pstmt = conn.prepareStatement("select * from expressions");
			rset = pstmt.executeQuery();
			count = 0;
			HashMap <Integer, ExpressionBean> expressions = bean.getExpressions();
			size = expressions.size();
			ExpressionBean expBean = null;
			while(rset.next()){
				size = expressions.size();
				if(count >= size) {
					type2.addExpressionRow(rset.getString("_label"));
				}
				expBean = expressions.get(new Integer(count));
				expBean.getText().setText(rset.getString("description"));
				count++;
			}
			
			/* Retrieve descriptions tab */
			
			pstmt = conn.prepareStatement("select * from morpdesc");
			rset = pstmt.executeQuery();
			if(rset.next()){
				bean.getDescriptionBean().getYesButton().setSelection(rset.getString("allInOne").contains("Y")?true:false);
				bean.getDescriptionBean().getNoButton().setSelection(rset.getString("allInOne").contains("N")?true:false);
				bean.getDescriptionBean().getOtherInfo().setText(rset.getString("OtherInfo"));
			}
			
			pstmt = conn.prepareStatement("select * from descriptions");
			rset = pstmt.executeQuery();
			count = 0;
			HashMap <Integer, SectionBean> sections = bean.getDescriptionBean().getSections();
			size = sections.size();
			SectionBean secBean = null;
			while(rset.next()){
				size = sections.size();
				if(count >= size){
					type2.addDescriptionRow(rset.getString("section"));					
				}
				secBean = sections.get(new Integer(count));
				secBean.getOrder().setText(rset.getString("_order"));
				secBean.getStartTokens().setText(rset.getString("start_token"));
				secBean.getEndTokens().setText(rset.getString("end_token"));
				secBean.getEmbeddedTokens().setText(rset.getString("embedded_token"));
				count ++;
			}			
			
			/* Retrieve Special tab data */
			
			pstmt = conn.prepareStatement("select * from specialsection");
			rset = pstmt.executeQuery();
			if(rset.next()) {
				SpecialBean specialBean = bean.getSpecial();
				specialBean.getFirstButton().setSelection(rset.getString("hasGlossary").equals("Y")?true:false);
				specialBean.getSecondButton().setSelection(rset.getString("hasReference").equals("Y")?true:false);
				specialBean.getFirstText().setText(rset.getString("glossaryHeading"));
				specialBean.getSecondText().setText(rset.getString("referenceHeading"));
			}
			
			/* Retrieve Abbreviations tab data */
			
			pstmt = conn.prepareStatement("select * from abbreviations");
			rset = pstmt.executeQuery();
			HashMap <String, Text> abbreviations = bean.getAbbreviations();
			count = 0;
			while (rset.next()) {
				abbreviations.get(rset.getString("_label")).setText(rset.getString("abbreviation"));
				count++;
			}
			
		} catch (SQLException exe) {
			LOGGER.error("Couldn't retrieve type2 Details in ConfigurationDbAccessor:retrieveType2Details" + exe);
			exe.printStackTrace();
		} finally {
			if (rset != null) {
				rset.close();
			}
			
			if (pstmt != null) {
				pstmt.close();
			}

			if (conn != null) {
				conn.close();
			}
	    }
	}

}
