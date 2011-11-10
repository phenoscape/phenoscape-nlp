package outputter;

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
 *This class output EQ statements from CharaParser XML files
 */
public class XML2Trait {
	private File source;
	private String outputtable;
	private Connection conn;
	private String username = "root";
	private String password = "root";
	/**
	 * 
	 */
	public XML2Trait(String sourcedir, String database, String outputtable) {
		this.source = new File(sourcedir);
		this.outputtable = outputtable;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("drop table if exists "+ outputtable);
				stmt.execute("create table if not exists "+outputtable+" (id int(11) not null unique auto_increment primary key, source varchar(500), description text, trait varchar(500))");
				}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void outputTraits(){
		File[] xmlfiles = this.source.listFiles();
		try{
			for(File f: xmlfiles){
				String src = f.getName();
				System.out.println("src::"+src);
				SAXBuilder builder = new SAXBuilder();
				Document xml = builder.build(f);
				Element root = xml.getRootElement();
				List<Element> statements = XPath.selectNodes(root, "//description/statement");
				Iterator<Element> it = statements.iterator();
				while(it.hasNext()){
					Element statement = it.next();
					Element text = (Element)XPath.selectSingleNode(statement, ".//text");
					List<Element> structures = XPath.selectNodes(statement, ".//structure");
					outputTraitStatements(src, root, text, structures);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void outputTraitStatements(String src, Element root,  Element textelement, List<Element> structures) {
		String text = textelement.getText();
		//System.out.println("text::"+text);
		//process structures
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element struct = it.next();
			outputTrait4Structure(src, root, text, struct);
		}
	}

	private void addTrait(String src, String text, String trait) {
		String q = "insert into "+this.outputtable+" (source, description, trait) values " +
				"('"+src+"','"+text+"','"+ trait+"')";
		try{
			Statement stmt = conn.createStatement();
			stmt.execute(q);
			System.out.println("trait:"+trait);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private String getStructureName(Element root, String structid) {
		try{
			Element structure = (Element)XPath.selectSingleNode(root, "//structure[@id='"+structid+"']");
			if(structure==null)return "REF";
			return ((structure.getAttribute("constraint")==null? "" : structure.getAttributeValue("constraint"))+" "+structure.getAttributeValue("name")).trim();
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private void outputTrait4Structure(String src, Element root, String text, Element struct) {
		String structname = this.getStructureName(root, struct.getAttributeValue("id"));
		try{
			List<Element> chars = XPath.selectNodes(struct, ".//character");
			Iterator<Element> it = chars.iterator();
			while(it.hasNext()){
				Element chara = it.next();
				String charaName = chara.getAttributeValue("name"); 
				addTrait(src, text, structname+" "+charaName);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String srcdir = "C:/Documents and Settings/Hong Updates/Desktop/Australia/fnav19_final/final";
		String database = "traits";
		String outputtable = "fnav19";
		XML2Trait x2e = new XML2Trait(srcdir, database, outputtable);
		x2e.outputTraits();
	}

}

