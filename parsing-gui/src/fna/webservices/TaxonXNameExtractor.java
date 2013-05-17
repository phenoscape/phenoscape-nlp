/**
 * 
 */
package fna.webservices;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 * Extracts taxon names from taxonX documents
 */
public class TaxonXNameExtractor {
	private Connection conn;
	private String table;
	private File sourcedir;
	/**
	 * 
	 */
	public TaxonXNameExtractor(String sourcedir, String database, String table) {
		try{
			this.sourcedir = new File(sourcedir);
			this.table =table;
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = "jdbc:mysql://localhost/"+database+"?user=termsuser&password=termspassword";
				//String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists "+table+"(source varchar(100), name varchar(200))");
				stmt.execute("delete from "+table);
				stmt.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void extract(){
		try{
			File[] files = this.sourcedir.listFiles();
			for(int i = 0; i < files.length; i++){
				File f = files[i];
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(f);
				Element root = doc.getRootElement();
				List<Element> names = (List<Element>)XPath.selectNodes(root, ".//tax:name");
				extractFromNameElement(names, f);
				System.out.println(f.getName());
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * one file may generate n names
	 * @param names
	 * @param f
	 */
	private void extractFromNameElement(List<Element> names, File f) {
		Iterator<Element> it = names.iterator();
		while(it.hasNext()){
			Element name = it.next();
			String namestring = name.getTextNormalize();
			addName2Table(namestring, f.getName());
		}		
	}
	/**
	 * save to database
	 * @param namestring
	 * @param name
	 */
	private void addName2Table(String namestring, String source) {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+this.table+" values('"+source+"', '"+namestring+"')");
		}catch(Exception e){
			e.printStackTrace();
		}		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String sourcedir = "Z:\\DATA\\Plazi\\2ndFetchFromPlazi\\taxonX-ants";
		String sourcedir = "Z:\\DATA\\Plazi\\2ndFetchFromPlazi\\taxonX-fish";
		String database="markedupdatasets";
		//String table="antnames";
		String table="fishnames";
		TaxonXNameExtractor tne = new TaxonXNameExtractor(sourcedir, database, table);
		tne.extract();
	}

}
