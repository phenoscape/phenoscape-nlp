/**
 * 
 */
package preprocessing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import outputter.ApplicationUtilities;

/**
 * @author Hong Cui
 * Add UUID to Prashanti's sim_j, n_ic results.
 *
 */
public class AddUUIDs {
	String inputtablename;
	String outputtablename;
	Hashtable<String, String> mapping;
	Element xmlroot;
	Connection conn;
	/**
	 * 
	 */
	public AddUUIDs(String inputtablename, String mappingtable) {
			this.inputtablename = inputtablename;
			this.outputtablename = inputtablename+"_allIDs";
			this.mapping = new Hashtable<String, String> ();
			Statement stmt = null;
			ResultSet rs = null;
			try {
				if(conn == null){
					Class.forName("com.mysql.jdbc.Driver");
					conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
					stmt = conn.createStatement();
					rs = stmt.executeQuery("select character_state_number, character_state_uuid from "+mappingtable);
					while(rs.next()){
						mapping.put(rs.getString("character_state_number"), rs.getString("character_state_uuid"));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
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
			}
		}
		
		public void replace(){
			Statement stmt = null;
			ResultSet rs = null;
			Statement stmt1 = null;
			try{
				stmt = conn.createStatement();
				stmt1 = conn.createStatement();
				stmt.execute("drop table if exists `"+this.outputtablename+"`");
				stmt.execute("create table `"+this.outputtablename+ "` select * from `"+this.inputtablename+"`");
				stmt.execute("alter table `"+this.outputtablename+ "` add column stateid varchar(200)");
				rs = stmt.executeQuery("select distinct character_number, state_number from `"+this.outputtablename+"`");
				while(rs.next()){
					String cnumber = rs.getString("character_number");
					String snumber = rs.getString("state_number");
					System.out.println("numbers:"+rs.getString("character_number")+"_"+rs.getString("state_number"));
					String uuid = mapping.get(rs.getString("character_number")+"_"+rs.getString("state_number"));
					System.out.println("uuid:"+uuid);
					stmt1.execute("update `"+this.outputtablename+"` set stateid ='"+uuid+
							"' where character_number ='"+cnumber+"' and state_number='"+snumber+"'");
				}
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
			ArrayList<String> tables = new ArrayList<String>();
			tables.add("pereq_kr--ad_40718--cp_40718");
			tables.add("pereq_kr--ad_40718--cp_best");
			tables.add("pereq_kr--ad_40718--cp2012_bioc");
			tables.add("pereq_kr--ad_40718--ni_40716");
			tables.add("pereq_kr--ni_40716--cp_40716");
			tables.add("pereq_kr--ni_40716--cp_best");
			tables.add("pereq_kr--ni_40716--cp2012_bioc");
			tables.add("pereq_kr--wd_40717--ad_40718");
			tables.add("pereq_kr--wd_40717--cp_40717");
			tables.add("pereq_kr--wd_40717--cp_best");
			tables.add("pereq_kr--wd_40717--cp2012_bioc");
			tables.add("pereq_kr--wd_40717--ni_40716");
			tables.add("pereq_nr--ad_40674--cp_40674");
			tables.add("pereq_nr--ad_40674--cp_best");
			tables.add("pereq_nr--ad_40674--cp2012_bioc");
			tables.add("pereq_nr--ad_40674--ni_40676");
			tables.add("pereq_nr--ni_40676--cp_40676");
			tables.add("pereq_nr--ni_40676--cp_best");
			tables.add("pereq_nr--ni_40676--cp2012_bioc");
			tables.add("pereq_nr--wd_38484--ad_40674");
			tables.add("pereq_nr--wd_38484--cp_38484");
			tables.add("pereq_nr--wd_38484--cp_best");
			tables.add("pereq_nr--wd_38484--cp2012_bioc");
			tables.add("pereq_nr--wd_38484--ni_40676");
			tables.add("pereq_kr--ad_40718--cp_40718_pr");
			tables.add("pereq_kr--ad_40718--cp_best_pr");
			tables.add("pereq_kr--ad_40718--cp2012_bioc_pr");
			tables.add("pereq_kr--ad_40718--ni_40716_pr");
			tables.add("pereq_kr--ni_40716--cp_40716_pr");
			tables.add("pereq_kr--ni_40716--cp_best_pr");
			tables.add("pereq_kr--ni_40716--cp2012_bioc_pr");
			tables.add("pereq_kr--wd_40717--ad_40718_pr");
			tables.add("pereq_kr--wd_40717--cp_40717_pr");
			tables.add("pereq_kr--wd_40717--cp_best_pr");
			tables.add("pereq_kr--wd_40717--cp2012_bioc_pr");
			tables.add("pereq_kr--wd_40717--ni_40716_pr");
			tables.add("pereq_nr--ad_40674--cp_40674_pr");
			tables.add("pereq_nr--ad_40674--cp_best_pr");
			tables.add("pereq_nr--ad_40674--cp2012_bioc_pr");
			tables.add("pereq_nr--ad_40674--ni_40676_pr");
			tables.add("pereq_nr--ni_40676--cp_40676_pr");
			tables.add("pereq_nr--ni_40676--cp_best_pr");
			tables.add("pereq_nr--ni_40676--cp2012_bioc_pr");
			tables.add("pereq_nr--wd_38484--ad_40674_pr");
			tables.add("pereq_nr--wd_38484--cp_38484_pr");
			tables.add("pereq_nr--wd_38484--cp_best_pr");
			tables.add("pereq_nr--wd_38484--cp2012_bioc_pr");
			tables.add("pereq_nr--wd_38484--ni_40676_pr");
			for(String table: tables){
				AddUUIDs replacer = new AddUUIDs(table, "number2id_mapping");
				replacer.replace();
			}
		}


}
