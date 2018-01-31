/**
 * 
 */
package preprocessing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import outputter.ApplicationUtilities;

/**
 * @author Hong Cui
 * 
 * Cuarator's results provided by Jim used sequential ids.
 * This class generates a mapping between these ids and the statesid/stateids used in the XML source. 
 * And replace uuid ids with numbered ids. 
 *
 */
public class ReplaceIdsInCuratorResults {
	String inputtablename;
	String outputtablename;
	File mappingfile;
	Element xmlroot;
	Connection conn;
	/**
	 * results with replaced ids saved in knowledge/naive_38484
	 * @param tablename: knowledge/naive_38484_text
	 * @param xmlfilepath
	 * 
	 */
	public ReplaceIdsInCuratorResults(String inputtablename, String xmlfilepath) {
		this.inputtablename = inputtablename;
		this.outputtablename = inputtablename.replaceFirst("_text$", "");
		File f = new File(xmlfilepath);
		this.mappingfile = new File(f.getParent(), "id_mapping.txt");
		SAXBuilder builder = new SAXBuilder();
		try {
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			}
			xmlroot = (builder.build(f)).getRootElement();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	public void replace(){
		StringBuffer mapping = new StringBuffer();
		Statement stmt = null;
		ResultSet rs = null;
		Statement stmt1 = null;
		try{
			stmt = conn.createStatement();
			stmt.execute("drop table if exists "+this.outputtablename);
			stmt.execute("create table "+this.outputtablename+ " select * from "+this.inputtablename);
			rs = stmt.executeQuery("select distinct characterid, characterlabel from "+this.outputtablename);
			while(rs.next()){
				String numberid = rs.getString("characterid");
				String character = rs.getString("characterlabel");
				XPath charpath = XPath.newInstance(".//x:char[@label=\""+character+"\"]");
				//XPath charpath = XPath.newInstance(".//x:char[@label=\"Claspers with 'toothed' plate\"]");
				charpath.addNamespace("x", xmlroot.getNamespaceURI()); //this is how to handle default namespace
				Element chara = (Element) charpath.selectSingleNode(xmlroot);
				if(chara==null){
					System.err.println("No match for "+character);
				}
				else{
					String uuid = chara.getAttributeValue("states");
					mapping.append(numberid+"\t"+uuid+"\n");
					stmt1 = conn.createStatement();
					stmt1.execute("update "+this.outputtablename+" set characterid='"+uuid+"' where characterid='"+numberid+"'");
					
					@SuppressWarnings("unchecked")
					XPath statepath = XPath.newInstance("//x:states[@id='"+uuid+"']");
					statepath.addNamespace("x", xmlroot.getNamespaceURI());
					Element stategroup = (Element)statepath.selectSingleNode(xmlroot);
					@SuppressWarnings("unchecked")
					List<Element>states = stategroup.getChildren("state", xmlroot.getNamespace());
					for(Element state: states){
						String symbol = state.getAttributeValue("symbol");
						String suuid = state.getAttributeValue("id");
						mapping.append(numberid+"_"+symbol+"\t"+suuid+"\n");
						stmt1 = conn.createStatement();
						stmt1.execute("update "+this.outputtablename+" set stateid='"+suuid+"' where characterid='"+uuid+"' and stateid='"+symbol+"'");
					}					
				}
			}
			//remove '' from labels
			stmt1.execute("update "+outputtablename+" set entitylabel = replace(entitylabel, '\\'', '')");
			stmt1.execute("update "+outputtablename+" set qualitylabel = replace(qualitylabel, '\\'', '')");
			stmt1.execute("update "+outputtablename+" set relatedentitylabel = replace(relatedentitylabel, '\\'', '')");
			
			//write out mapping file
			FileWriter fw = new FileWriter(this.mappingfile);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(mapping.toString());
			bw.close();			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(rs!=null)
				try {
					rs.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(stmt!=null)
				try {
					stmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(stmt1!=null)
				try {
					stmt1.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			if(conn!=null)
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArrayList<String> xmlfilepaths= new ArrayList<String>();
		String path = "C:/Users/updates/CharaParserTest/charaParserEval2013/matchIds/";
		xmlfilepaths.add(path+"38484.xml");
		xmlfilepaths.add(path+"40674.xml");
		xmlfilepaths.add(path+"40676.xml");
		xmlfilepaths.add(path+"40716.xml");
		xmlfilepaths.add(path+"40717.xml");
		xmlfilepaths.add(path+"40718.xml");
		ArrayList<String> inputtables = new ArrayList<String>();
		inputtables.add("naive_38484_text");
		inputtables.add("naive_40674_text");
		inputtables.add("naive_40676_text");
		inputtables.add("knowledge_40716_text");
		inputtables.add("knowledge_40717_text");
		inputtables.add("knowledge_40718_text");
		for(int i = 0; i < 6;  i++){
			ReplaceIdsInCuratorResults replacer = new ReplaceIdsInCuratorResults(inputtables.get(i), xmlfilepaths.get(i));
			replacer.replace();
		}
	}

}
