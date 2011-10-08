/**
 * 
 */
package preprocessing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * @author Hong Updates
 *
 */
public class SourceAdder {

	private String termtable;
	private String sentencetable;
	private Connection conn;
	private String username="root";
	private String password="root";
	private String srcprefix;
	
	public SourceAdder(String termtable, String sentencetable, String termdatabase){
		this.termtable = termtable;
		this.sentencetable = sentencetable;
		this.srcprefix = sentencetable.substring(0, sentencetable.indexOf("."));
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+termdatabase+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void addSource(){
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select structure from "+this.termtable);
			while(rs.next()){
				String word = rs.getString("structure");
				Statement stmt1 = conn.createStatement();
				ResultSet rs1 = stmt1.executeQuery("select source, sentence from "+this.sentencetable+" where concat(modifier, \" \",tag) like '% "+word+" %' or  tag like '"+word+" %' or  tag like '% "+word+"' or  tag = '"+word+"'");
				if(rs1.next()){
					System.out.println(word);
					addSource4Structure(word, rs1.getString("source"), rs1.getString("sentence"));
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void addSource4Structure(String structure, String source, String sentence) {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.termtable+ " set srcfile='"+srcprefix+"."+source+"', srcsentence='"+sentence+"' where structure='"+structure+"'");
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String termtable="anstructure2hao_checked";
		String sourcetable="plaziant_benchmark.plaziant_nn_sentence";
		String database="ontologymapping";
		SourceAdder sa = new SourceAdder(termtable, sourcetable, database);
		sa.addSource();

	}

}
