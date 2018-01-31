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

import outputter.search.TermSearcher;

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
	private String termcolumn;
	private String mode;
	private static final Logger LOGGER = Logger.getLogger(SourceAdder.class);   
	
	public SourceAdder(String termtable, String termcolumn, String mode,  String sentencetable, String termdatabase){
		this.termtable = termtable;
		this.sentencetable = sentencetable;
		this.srcprefix = sentencetable.substring(0, sentencetable.indexOf("."));
		this.termcolumn = termcolumn;
		this.mode = mode;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+termdatabase+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}
	
	public void addSource(){
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select "+this.termcolumn+" from "+this.termtable);
			while(rs.next()){
				String term = rs.getString(1);
				String copyterm = term;
				term = term.replaceAll("\\[.*?\\]", "").replaceAll("-", "_").trim();
				Statement stmt1 = conn.createStatement();
				ResultSet rs1 = null;
				if(this.mode.compareTo("structure")==0)
					rs1 = stmt1.executeQuery("select source, sentence from "+this.sentencetable+" where concat(modifier, \" \",tag) like '% "+term+" %' or  tag like '"+term+" %' or  tag like '% "+term+"' or  tag = '"+term+"'");
				else if (this.mode.compareTo("character")==0){
					String q = "select source, sentence from "+this.sentencetable+" where sentence like '% "+term+" %' or  sentence like '"+term+" %' or  sentence like '% "+term+"' or  sentence = '"+term+"'";
					rs1 = stmt1.executeQuery(q);
					if(!rs1.next())
						rs1 = stmt1.executeQuery("select source, sentence from "+this.sentencetable+" where sentence like '%"+term+"%'");
				}
				if(rs1.next()){
					System.out.println(term);
					addSource4Structure(copyterm, rs1.getString("source"), rs1.getString("sentence"));
				}
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}
	
	private void addSource4Structure(String term, String source, String sentence) {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("update "+this.termtable+ " set srcfile='"+srcprefix+"."+source+"', srcsentence='"+sentence+"' where "+this.termcolumn+"='"+term+"'");
		}catch(Exception e){
			LOGGER.error("", e);
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String termtable="anstructure2hao_checked";
		//String sourcetable="plaziant_benchmark.plaziant_nn_sentence";
		String termtable="fna2pato_checked"; //termtable must have columns of srcfile and srcsentence
		String sourcetable="fnav19_benchmark.sentence";
		String termcolumn ="term";
		String mode = "character"; //or "structure"
		String database="ontologymapping";
		SourceAdder sa = new SourceAdder(termtable, termcolumn, mode, sourcetable, database);
		sa.addSource();
	}

}
