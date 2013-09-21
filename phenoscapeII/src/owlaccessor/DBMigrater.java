package owlaccessor;

//import java.io.File;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;

import outputter.search.TermSearcher;

/**
 * This class extracts all terms, their IDs and synonyms from an OWL ontology to a database table.
 * 
 * 
 * @author Zilong Chang, Hong Cui
 * 
 * */
public class DBMigrater {

	private Connection con;
	private String url;
	private String dburl = "jdbc:mysql://localhost:3306/";
	private String uname = "root";
	private String upw = "root";
	private static final Logger LOGGER = Logger.getLogger(DBMigrater.class);   

	/**
	 * This method extracts terms, their IDs and synonyms from PATO (from web)
	 * and stores them into a table. The table will have the structure as
	 * follows:
	 * 
	 * |rid |term |termid |synonym|
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
	public void migrate(String dbName, String tabName, String ontoURI) throws Exception {
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
					oa = new OWLAccessorImpl(url, new ArrayList<String>());
				} else {
					oa = new OWLAccessorImpl(new File(url), new ArrayList<String>());
				}

				// for each ontology term
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

				//con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				LOGGER.error("", e);
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("", e);
		}

		if(ontoURI.contains("/bspo.")){
			//create and populate uniquespatialterms term
			collectUniquespatialterms(dbName, tabName);
		}
		con.close();

	}

	/**
	 * create and populate uniquespatialterms term
	 * @param tableName: onto_bspo table
	 */
	private void collectUniquespatialterms(String dbName, String tableName) {	
		try {
			if(con==null){
				Class.forName("com.mysql.jdbc.Driver");
				con = DriverManager.getConnection(dburl + dbName, uname, upw);
			}
			// Drop table if exists
			Statement stmt = con.createStatement();
			stmt.executeUpdate("DROP TABLE IF EXISTS uniquespatialterms");
			// Create table
			stmt.executeUpdate("create table uniquespatialterms (term varchar(100) primary key, id varchar(50))" );

			ArrayList<String> terms = new ArrayList<String>();
			Statement insert = con.createStatement();
			ResultSet rs = stmt.executeQuery("select term, synonym, termid from "+tableName+ " where LEFT(termid, 4)='BSPO'");
			while(rs.next()){
				String t = rs.getString("term").trim();
				if(t.indexOf(" ")>0) t = t.substring(0, t.indexOf(" "));
				if(!terms.contains(t)){
					terms.add(t);
					insert.execute("insert into uniquespatialterms (term, id) values ('"+t+"','"+rs.getString("termid")+"')");
				}
				t = rs.getString("synonym");
				if(t!=null){
					t = t.trim();
					if(t.indexOf(" ")>0) t = t.substring(0, t.indexOf(" "));
					if(!terms.contains(t)){
						terms.add(t);			
						insert.execute("insert into uniquespatialterms (term, id) values ('"+t+"','"+rs.getString("termid")+"')");
					}
				}
			}				
		}catch(Exception e){
			LOGGER.error("", e);
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
					oa = new OWLAccessorImpl(url, new ArrayList<String>());
				} else {
					oa = new OWLAccessorImpl(new File(url),new ArrayList<String>());
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
				LOGGER.error("", e);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LOGGER.error("", e);
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("", e);
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
				LOGGER.error("", e);
			}
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("", e);
		}

	}

	public static void main(String[] args) {
		DBMigrater dbm = new DBMigrater();
		// String url = "http://purl.obolibrary.org/obo/pato.owl";
		// String tname = "ontoPATO";
		// String url = "http://purl.obolibrary.org/obo/tao.owl";
		// String tname = "ontoTAO";
		String url = "http://purl.obolibrary.org/obo/bspo.owl";
		String tname = "onto_BSPO";
		//		String url = "C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\vao.owl";
		//		String tname = "ontoVAO";
		//		String url = "C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\aa.owl";
		//		String tname = "ontoAMAO";
		//		String url = "C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\vao.owl";
		//		String tname = "ontoVAO";

		try{
			dbm.migrate("biocreative2012", tname, url);
		}catch(Exception e){
			LOGGER.error("", e);
		}

		//dbm.addToStructureWords("phenoscape", "ontoamao","AMAO","learnedstructurewords_ini_onto_lastword");
		//System.out.println("DONE!");

	}

}
