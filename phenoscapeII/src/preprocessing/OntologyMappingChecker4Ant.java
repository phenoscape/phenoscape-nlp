/**
 * 
 */
package preprocessing;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import outputter.TermSearcher;

/**
 * @author Hong Updates
 */
public class OntologyMappingChecker4Ant {
	private Connection conn;
	private String sourcetable;
	private String targettable;
	private static String username="root";
	private static String password="root";
	private static final Logger LOGGER = Logger.getLogger(OntologyMappingChecker4Ant.class);   


	/**
	 * constructor
	 */
	public OntologyMappingChecker4Ant(String sourcetable, String targettable, String database) {
		this.sourcetable = sourcetable;
		this.targettable = targettable;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}

	/*compound terms to simple terms
	 * */
	public void normalize(){
		try{
			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select structure, hao from "+this.sourcetable);
			while(rs.next()){
				String structure = rs.getString("structure");
				System.out.println(structure);
				String hao = rs.getString("hao");
				String [] structs = structure.split("\\s+and\\s+");
				String [] haos = hao.split("and");
				for(int i = 0; i<structs.length; i++){
					insert(structs[i].trim(), i<haos.length? haos[i].trim() : "xxx");
				}
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}
	


	private void insert(String structure, String hao) {
		try{
			Statement stmt = conn.createStatement();
			String q = "select structure, hao from "+this.targettable+" where structure='"+structure+"'";
			ResultSet rs = stmt.executeQuery(q);
			if(rs.next()){
				String hao1 = rs.getString("hao").trim();
				if(hao1.indexOf(hao)<0){
					stmt.execute("update "+this.targettable+" set hao='"+hao+","+hao1+"' where structure='"+structure+"'");					
				}
			}else{
				stmt.execute("insert into "+this.targettable+"(structure, hao) values ('"+structure+"','"+hao+"')");
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String source = "anstructure2hao_original";
		String target = "anstructure2hao_checked";
		String database = "ontologymapping";
		OntologyMappingChecker4Ant omca = new OntologyMappingChecker4Ant(source, target, database);
		omca.normalize();
	}

}
