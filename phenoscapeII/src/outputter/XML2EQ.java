/**
 * 
 */
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
public class XML2EQ {
	private File source;
	private String outputtable;
	private Connection conn;
	private String username = "root";
	private String password = "root";
	/**
	 * 
	 */
	public XML2EQ(String sourcedir, String database, String outputtable) {
		this.source = new File(sourcedir);
		this.outputtable = outputtable;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("drop table if exists "+ outputtable);
				stmt.execute("create table if not exists "+outputtable+" (id int(11) not null unique auto_increment primary key, source varchar(500), description text, entity varchar(200), entityid varchar(20), quality varchar(200), qualityid varchar(20), relatedentity varchar(200), relatedentityid varchar(20))");
				}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void outputEQs(){
		File[] xmlfiles = this.source.listFiles();
		try{
			for(File f: xmlfiles){
				String src = f.getName();
				SAXBuilder builder = new SAXBuilder();
				Document xml = builder.build(f);
				Element root = xml.getRootElement();
				List<Element> statements = XPath.selectNodes(root, "//description/statement");
				Iterator<Element> it = statements.iterator();
				while(it.hasNext()){
					Element statement = it.next();
					Element text = (Element)XPath.selectSingleNode(statement, ".//text");
					List<Element> structures = XPath.selectNodes(statement, ".//structure");
					List<Element> relations = XPath.selectNodes(statement,".//relation");
					outputEQs4Statement(src, root, text, structures, relations);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void outputEQs4Statement(String src, Element root,  Element textelement, List<Element> structures,
			List<Element> relations) {
		String text = textelement.getText();
		System.out.println("text::"+text);
		//process structures
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element struct = it.next();
			outputEQa4Structure(src, root, text, struct);
		}
		//process relations
		it = relations.iterator();
		while(it.hasNext()){
			Element rel = it.next();
			String fromid = rel.getAttributeValue("from");
			String toid = rel.getAttributeValue("to");
			String relname = rel.getAttributeValue("name").trim();
			String neg = rel.getAttributeValue("negation");
			String fromstruct = getStructureName(root, fromid); 
			String tostruct = getStructureName(root, toid); 
			if(neg.compareTo("true")==0){
				relname = "((not)"+relname+")";
			}
			addEQStatement(src, text, fromstruct,"", relname, "", tostruct, "");
		}
	}

	private void addEQStatement(String src, String text, String entity, String entityid, String quality, String qualityid, String relatedentity, String relatedentityid) {
		String q = "insert into "+this.outputtable+" (source, description, entity, entityid, quality, qualityid, relatedentity, relatedentityid) values " +
				"('"+src+"','"+text+"','"+ entity+"','"+ entityid+"','"+ quality+"','"+ qualityid+"','"+ relatedentity+"','"+ relatedentityid+"')";
		try{
			Statement stmt = conn.createStatement();
			stmt.execute(q);
			System.out.println("EQ::[E]"+entity+" [Q]"+quality+(relatedentity.length()>0? " [RE]"+relatedentity :""));
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
	private void outputEQa4Structure(String src, Element root, String text, Element struct) {
		String structname = this.getStructureName(root, struct.getAttributeValue("id"));
		try{
			List<Element> chars = XPath.selectNodes(struct, ".//character");
			Iterator<Element> it = chars.iterator();
			while(it.hasNext()){
				Element chara = it.next();
				String charatype = chara.getAttribute("char_type")!=null ? "range" : "discrete"; 
				//characters
				String quality = "";
				if(charatype.compareTo("range")==0){
					quality = chara.getAttributeValue("from")+
							  " "+
							  (chara.getAttribute("from_unit")!=null? chara.getAttributeValue("from_unit"): "")+
							  " "+
							  chara.getAttributeValue("to")+
							  " "+
							  (chara.getAttribute("to_unit")!=null?  chara.getAttributeValue("to_unit") : "");

				}else{
					quality = chara.getAttributeValue("value")+
					          " "+
					 		  (chara.getAttribute("unit")!=null? chara.getAttributeValue("unit"): "")+
					          "["+
							  (chara.getAttribute("modifier")!=null? chara.getAttributeValue("modifier") : "")	
					          +"]";		  
				}
				quality = quality.replaceAll("\\[\\]", "").replaceAll("\\s+", " ").trim();
				//constraints
				String relatedentities = "";
				if(chara.getAttribute("constraintid")!=null){
					String[] ids = chara.getAttributeValue("constraintid").split("\\s*[;,]\\s*");
					for(String id: ids){
						relatedentities +=this.getStructureName(root, id)+";";
					}
					relatedentities = relatedentities.replaceFirst(";$", "");
				}
				addEQStatement(src, text, structname,"", quality, "", relatedentities, "");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String srcdir = "C:/Documents and Settings/Hong Updates/Desktop/Australia/phenoscape-fish-source/target/final";
		String database = "phenoscape";
		String outputtable = "fish_xml2eq";
		XML2EQ x2e = new XML2EQ(srcdir, database, outputtable);
		x2e.outputEQs();
	}

}
