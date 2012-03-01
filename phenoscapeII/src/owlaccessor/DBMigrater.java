package owlaccessor;

//import java.io.File;
import java.io.File;
import java.sql.*;
import java.util.*;
//import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

/**
 * This class extracts all terms, their IDs and synonyms from PATO to database.
 * 
 * 
 * @author Zilong Chang
 * 
 * */
public class DBMigrater {

	private Connection con;
	private String url;
	private String dburl = "jdbc:mysql://localhost:3306/";
	private String uname = "termsuser";
	private String upw = "termspassword";

	/**
	 * This method extracts terms, their IDs and synonyms from PATO (from web)
	 * and stores them into a table. The table will have the structure as
	 * follows:
	 * 
	 * ============================ |rid |term |termid |synonym|
	 * ============================
	 * 
	 * rid is a auto-generated surrogate key (you don't have to worry about it)
	 * term is a PATO term termid is PATO ID of the term synonym is one of the
	 * term's synonyms
	 * 
	 * If a term has multiple synonyms, there will be multiple rows related to
	 * the term. If a term has no synonyms, the synonym field will be null.
	 * 
	 * This method could be extended to deal with other ontologies, but may need
	 * some changes.
	 * 
	 * @param dbName
	 *            Name of the existed database (assume database is already
	 *            created)
	 * @param tabName
	 *            Desirable name of the table to be created
	 * @param ontoURI
	 *            the ontology uri
	 */
	public void migrate(String dbName, String tabName, String ontoURI) {
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
				this.url = ontoURI;
				// create the accessor to the pato on web
				OWLAccessor oa = null;
				if (url.startsWith("http")) {
					oa = new OWLAccessorImpl(url);
				} else {
					oa = new OWLAccessorImpl(new File(url));
				}

				// for each pato term
				for (OWLClass c : oa.getAllClasses()) {
					String id = oa.getID(c);
					String label = oa.getLabel(c);

					// no synonyms
					if (oa.getSynonymLabels(c).isEmpty()) {
						stmt.executeUpdate("INSERT INTO " + tabName
								+ "(term, termid, synonym) VALUES('"
								+ label.trim().replaceAll("'", "''") + "','"
								+ id.trim().replaceAll("'", "''") + "', null)");
					} else {
						// have synonyms
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
	
	public void migrateKeywords(String dbName, String tabName, String ontoURI){
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
						+ "kid int primary key auto_increment, "
						+ "term varchar(100) not null, "
						+ "termid varchar(50) not null, "
						+ "keyword varchar(100)" + ")");

				Statement stmt = con.createStatement();
				this.url = ontoURI;
				// create the accessor to the pato on web
				OWLAccessor oa = null;
				if (url.startsWith("http")) {
					oa = new OWLAccessorImpl(url);
				} else {
					oa = new OWLAccessorImpl(new File(url));
				}

				// for each pato term
				for (OWLClass c : oa.getAllClasses()) {
					String id = oa.getID(c);
					String label = oa.getLabel(c);
					
					//keywords of the term itself
					for (String k:oa.getKeywords(c)){
						stmt.executeUpdate("INSERT INTO " + tabName
								+ "(term, termid, keyword) VALUES('"
								+ label.trim().replaceAll("'", "''")
								+ "','" + id.trim().replaceAll("'", "''")
								+ "','" + k.trim().replaceAll("'", "''")
								+ "')");
					}
					
					//add parents' keywords
					for(OWLClass p:((OWLAccessorImpl)oa).getParents(c)){
						for (String k:oa.getKeywords(p)){
							stmt.executeUpdate("INSERT INTO " + tabName
									+ "(term, termid, keyword) VALUES('"
									+ label.trim().replaceAll("'", "''")
									+ "','" + id.trim().replaceAll("'", "''")
									+ "','" + k.trim().replaceAll("'", "''")
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

	/**
	 * Gets the last word of a given string.
	 * 
	 * @param s
	 *            the string
	 * @return the last word
	 */
	public String getLastWord(String s) {
		String[] words = s.split("\\s");
		if(words.length>0){
			return words[words.length - 1];
		}else{
			return "";
		}
	}

	public void addToStructureWords(String dbName, String source, String sourceID, String destination){		
		try {
			Class.forName("com.mysql.jdbc.Driver");
			try {
				con = DriverManager.getConnection(dburl + dbName, uname, upw);
				
				Statement s = con.createStatement();
				ResultSet rs = s.executeQuery("select term from "+source+" where termid like '"+sourceID.toUpperCase()+":%'");
				
				Set<String> structures = new HashSet<String>();
				
				while(rs.next()){
					String term = rs.getString("term");
					structures.add(this.getLastWord(term));
				}
				
				Statement s1 = con.createStatement();
				for(String str:structures){
					s1.executeUpdate("INSERT INTO "+destination+" values('"+str+"','','')");
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
		// String url = "http://purl.obolibrary.org/obo/pato.owl";
		// String tname = "ontoPATO";
		// String url = "http://purl.obolibrary.org/obo/tao.owl";
		// String tname = "ontoTAO";
//		String url = "C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\vao.owl";
//		String tname = "ontoVAO";
//		String url = "C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\aa.owl";
//		String tname = "ontoAMAO";
//		String url = "C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\vao.owl";
//		String tname = "ontoVAO";
//		
		//String tname = "ontoAMAO";
//		dbm.migrate("phenoscape", tname, url);
		
//		dbm.addToStructureWords("phenoscape", "ontoamao","AMAO","learnedstructurewords_ini_onto_lastword");
//		System.out.println("DONE!");
		
		String url = "http://purl.obolibrary.org/obo/pato.owl";
		String tname = "patokeywords";
		
		dbm.migrateKeywords("phenoscape", tname, url);

	}

}
