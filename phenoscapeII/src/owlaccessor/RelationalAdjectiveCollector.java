/**
 * 
 */
package owlaccessor;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;



import outputter.ApplicationUtilities;

/**
 * @author Hong Cui
 * calls OWLAccessorImpl to collect relational adjectives (i.e., relational organs) from UBERON
 * this class is used to create a newtable holding relational adjectives that may be added to the the glossary table (relational adjectives are considered structures)
 */
public class RelationalAdjectiveCollector {

	/**
	 * 
	 */
	public RelationalAdjectiveCollector(String newtable, String glossarytable, String updater) {

		String uberondir = ApplicationUtilities.getProperty("ontology.dir")+
				System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.uberon")+".owl";
		OWLAccessorImpl oai = new OWLAccessorImpl(new File(uberondir), new ArrayList<String>());
		Hashtable<String,Hashtable<String, String>> adjectives = OWLAccessorImpl.adjectiveorgans;
		Set<String> adjs = adjectives.keySet();
		try{
			Class.forName("com.mysql.jdbc.Driver");
			Connection conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+newtable);
			stmt.execute("create table "+newtable+" like "+glossarytable);
			for(String adj: adjs){
				ResultSet rs = stmt.executeQuery("select * from "+glossarytable+" where term ='"+adj+"' and category='structure'");
				if(!rs.next()){
					//insert a new row
					Statement insert = conn.createStatement();
					insert.execute("insert into "+newtable+" (term, category, last_updated_by) values('"+adj+"', 'structure', '"+updater+"')");
					System.out.println(adj +" inserted as a structure");
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RelationalAdjectiveCollector rac = new RelationalAdjectiveCollector("adjectiveorgans", "fishglossaryfixed", "hong052513");

	}

}
