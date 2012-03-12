/**
 * 
 */
package oboaccessor;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author Hong Updates
 * export id, name, synonym info from a OBO ontology to a db table
 */
public class OBO2DB {
	static protected Connection conn = null;
	static protected String database = null;
	String ontoname = null;
	File onto = null;
	/**
	 * 
	 */
	public OBO2DB(String database, String ontoPath, String ontoname) {
		this.ontoname = ontoname;
		this.onto = new File(ontoPath);
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = "jdbc:mysql://localhost/"+database+"?user=root&password=root";
				//String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("show tables");
			boolean exist = false;
			while(rs.next()){
				if(rs.getString(1).compareTo(this.ontoname)==0){
					exist = true;
					break;
				}
			}
			if(!exist){
				stmt.execute("create table if not exists "+this.ontoname+" (ontoid varchar(100) NOT NULL, term varchar(100), termstatus varchar(20))");
				stmt.execute("delete from "+this.ontoname);
				stmt.close();
				export2DB();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void export2DB() throws Exception{
	  FileInputStream fstream = new FileInputStream(this.onto);
	  DataInputStream in = new DataInputStream(fstream);
	  BufferedReader br = new BufferedReader(new InputStreamReader(in));
	  String strLine;
	  String term = null;
	  String id = null;
	  String excsyn = null;
	  String relsyn = null;
	  while ((strLine = br.readLine()) != null)   {
		  strLine = strLine.trim();
		  if(strLine.compareTo("[Term]")==0){
			  //save the last set
			  insert2db(term, id, excsyn, relsyn);
			  term = "";
			  id = "";
			  excsyn = "";
			  relsyn = "";
		  }
		  if(strLine.startsWith("id:")){
			  id = strLine.replaceFirst("id:", "").trim();
		  }
		  if(strLine.startsWith("name:")){
			  term = strLine.replaceFirst("name:", "").trim();
		  }
		  if(strLine.startsWith("synonym:")){
			  if(strLine.contains("EXACT")){
				  excsyn += strLine.replaceFirst("synonym:", "").replaceFirst("EXACT.*$", "").replaceAll("\"", "").trim()+";";
			  }
			  if(strLine.contains("RELATED")){
				  relsyn += strLine.replaceFirst("synonym:", "").replaceFirst("RELATED.*$", "").replaceAll("\"", "").trim()+";";
			  }
		  }
	  }			  
	  in.close();
	}
	
	/**
	 * 
	 * @param term
	 * @return 0: ONTO ID  1:matched term
	 */
	public String[] getID(String term) {
		try{
			String[] result = new String[]{"", ""};
			Statement stmt = conn.createStatement();
			String q = "select ontoid, term from "+this.ontoname+" where term =\""+term+"\"";
			ResultSet rs = stmt.executeQuery(q);
			if(rs.next()){
				result[0] = rs.getString("ontoid");
				result[1] = rs.getString("term");
				return result;
			}
			/*q = "select ontoid, term from "+this.ontoname+" where term like \"%"+term+"%\"";
			rs = stmt.executeQuery(q);
			if(rs.next()){
				result[0] += rs.getString("ontoid")+";";
				result[1] += rs.getString("term")+";";
				return result;
			}*/		
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	private void insert2db(String term, String id, String excsyn, String relsyn) {
		if(id==null) return;
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into " +this.ontoname+" values (\""+id+"\", \""+term+"\",\"label\")");
			if(excsyn!=null && excsyn.length()>0){
				String [] words = excsyn.split("\\s*;\\s*");
				for(String w: words){
					stmt.execute("insert into "+this.ontoname+" values (\""+id+"\", \""+w+"\",\"exact syn\")");
				}
			}
			if(relsyn !=null && relsyn.length() > 0){
				String[] words = relsyn.split("\\s*;\\s*");
				for(String w: words){
					stmt.execute("insert into "+this.ontoname+" values (\""+id+"\", \""+w+"\",\"related syn\")");
				}
			}
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\archosaur\\vertebrate_anatomy.obo";
		//OBO2DB o2d = new OBO2DB("obo", file ,"vertebrate_anatomy");
		//String file = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\archosaur\\amniote_draft.obo";
		//OBO2DB o2d = new OBO2DB("obo", file ,"amniote_draft");	
	}



}
