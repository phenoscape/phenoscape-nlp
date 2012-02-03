package owlaccessor;

//import java.io.File;
import java.sql.*;
import java.util.Calendar;
//import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

/**
 * This class extract all terms, their IDs and synonyms in PATO to database. 
 * @author Zilong Chang
 * 
 * */
public class DBMigrater {

	/**
	 * @param args
	 */
	private Connection con;

	private String url = "http://purl.obolibrary.org/obo/pato.owl";
	// "C:/Users/Zilong Chang/Documents/WORK/Ontology/pato.owl";

	private String dburl = "jdbc:mysql://localhost:3306/";
	private String uname = "termsuser";
	private String upw = "termspassword";
	
	/**
	 * This method extracts terms, their IDs and synonyms from PATO (from web) and stores
	 * them into a table. The table will have the structure as follows: 
	 * 
	 * ============================
	 * |rid |term |termid |synonym|
	 * ============================
	 * 
	 * rid is a auto-generated surrogate key (you don't have to worry about it)
	 * term is a PATO term
	 * termid is PATO ID of the term
	 * synonym is one of the term's synonyms
	 * 
	 * If a term has multiple synonyms, there will be multiple rows related to the term.
	 * If a term has no synonyms, the synonym field will be null.
	 * 
	 * This method could be extended to deal with other ontologies, but may need some
	 * changes.
	 *  
	 * @param dbName Name of the existed database (assume database is already created)
	 * @param tabName Desirable name of the table to be created
	 */
	public void migrate(String dbName, String tabName) {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			try {
				con = DriverManager.getConnection(dburl + dbName, uname, upw);

				// Drop table if exists
				Statement stmt0 = con.createStatement();
				stmt0.executeUpdate("DROP TABLE IF EXISTS " + tabName);
				
				// Create table
				Statement stmtc = con.createStatement();
				stmtc.executeUpdate("create table " + tabName + " ("
						+ "rid int primary key auto_increment, "
						+ "term varchar(100) not null, "
						+ "termid varchar(50) not null, "
						+ "synonym varchar(100)" + ")");

				Statement stmt = con.createStatement();
				
				//create the accessor to the pato on web 
				OWLAccessor oa = new OWLAccessorImpl(url);
				
				//for each pato term
				for (OWLClass c : oa.getAllClasses()) {
					String id = oa.getID(c);
					String label = oa.getLabel(c);
					
					//no synonyms
					if (oa.getSynonymLabels(c).isEmpty()) {
						stmt.executeUpdate("INSERT INTO " + tabName
								+ "(term, termid, synonym) VALUES('"
								+ label.trim().replaceAll("'", "''")
								+ "','" + id.trim().replaceAll("'", "''")
								+ "', null)");
					} else {
						//have synonyms
						for (String syn : oa.getSynonymLabels(c)) {
							stmt.executeUpdate("INSERT INTO " + tabName
									+ "(term, termid, synonym) VALUES('"
									+ label.trim().replaceAll("'", "''")
									+ "','" + id.trim().replaceAll("'", "''")
									+ "','" + syn.trim().replaceAll("'", "''")
									+ "')");
						}
					}
				}

				con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		DBMigrater dbm = new DBMigrater();
		dbm.migrate("tozilong", "test1");
		System.out.println("DONE!");

	}

}
