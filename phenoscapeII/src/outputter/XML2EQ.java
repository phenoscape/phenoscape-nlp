/**
 * 
 */
package outputter;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 *This class output EQ statements from the XML files output by CharaParser
 *The XML files are in \target\final
 */
public class XML2EQ {
	private File source;
	private String outputtable;
	private Connection conn;
	private String username = "root";
	private String password = "root";
	private String positionprep = "in|on";
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
				stmt.execute("create table if not exists "+outputtable+" (id int(11) not null unique auto_increment primary key, source varchar(500), description text, entity varchar(200), entityid varchar(20), quality varchar(200), qualityid varchar(20), qualitymodifier varchar(200), qualitymodifierid varchar(20), entitylocator varchar(200), entitylocatorid varchar(20))");
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
				List<Element> characterstatements = XPath.selectNodes(root, ".//statement[@statement_type='character']");
				List<Element> statestatements = XPath.selectNodes(root, ".//statement[@statement_type='character_state']");				
				System.out.println();
				System.out.println(src);				
				outputEQs4CharacterUnit(characterstatements, statestatements, src, root); //the set of statements related to a character (one of the statement is the character itself)				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	
	/**
	 * For example, 1 character statement with 3 state statements     
	<statement statement_type="character" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" seg_id="0">
      <text>Shape of ancistrine opercle (ordered )</text>
      <structure id="o650" name="whole_organism">
        <character name="shape" value="shape" constraint="of ancistrine opercle" constraintid="o651" />
      </structure>
      <structure id="o651" name="opercle" constraint="ancistrine" />
    </statement>
    <statement statement_type="character_state" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" state_id="4a99e866-54d9-4875-8b5e-385427db1245" seg_id="0">
      <text>sickle-shaped (&lt;i&gt;Peckoltia&lt;/i&gt;-type )</text>
      <structure id="o652" name="whole_organism">
        <character name="shape" value="sickle-shaped" />
      </structure>
    </statement>
    <statement statement_type="character_state" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" state_id="d53ba92f-0865-4456-9111-c6ff37fc624a" seg_id="0">
      <text>barshaped (&lt;i&gt;Ancistrus&lt;/i&gt;-type )</text>
      <structure id="o653" name="whole_organism">
        <character name="shape" value="barshaped" />
      </structure>
    </statement>
    <statement statement_type="character_state" character_id="0a1e6749-13fc-47be-bc7f-8184fc9c26ad" state_id="f56a9b6a-9720-437c-a1f4-60f01cd1bb15" seg_id="0">
      <text>oval or triangular</text>
      <structure id="o654" name="whole_organism">
        <character name="shape" value="oval" />
        <character name="shape" value="triangular" />
      </structure>
    </statement>
	 * @param statements
	 */
	private void outputEQs4CharacterUnit(List<Element> chars, List<Element> states, String src, Element root) {
		//on the first try, assuming the simple model: 
		//1 char statement holding 1 organ/process/entity
		//n states provide quality
		
		//step 1: process Character Statements
		//the set of character statements is expected to generate an E that is the subject for the state statements
		//while it may also generate additional EQ statements
		Element E = processCharStatements(chars, src, root); 
		//step 2: process State Statements
		//Use E to replace the "whole_organism" placeholder in the state statements and generate EQ statements
		//the state statements may also generate additional EQ statements
		if(E != null){
			processStateStatements(E, states, src, root);
		}		
	}
	

	/**
	 * step 1: process Character Statements
	 * the set of character statements is expected to generate an E that is the subject for the state statements
	 * while it may also generate additional EQ statements
	 * @param chars: a set of statements with type="character"
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private Element processCharStatements(List<Element> chars, String src, Element root) {
		Element key = null;
		ArrayList<Element> purge = new ArrayList<Element>();
		try{
			int troubles = 0;
			boolean findstructure = false;
			for(Element statement: chars){
				List<Element> structures = statement.getChildren("structure");
				for(Element e: structures){
					//expect all characters of whole_organism are CHARACTERS, which are identified by (name=value)
					//if not, increment troubles count
					if(e.getAttributeValue("name").contains("whole_organism")){
						//check e's characters
						List<Element> children = e.getChildren();
						for(Element c:  children){
							if(c.getAttributeValue("name").compareTo(c.getAttributeValue("value"))!=0){
								troubles++;					
							}
						}
						if(troubles == 0) purge.add(e); //this whole_organism is not useful
					}else{
						//return the first real structure element 
						if(!findstructure){
							findstructure = true;
							key = e; //find the key structure
							/*fill in possible locator relations + elements
							List<Element> reltemp = XPath.selectNodes(statement, ".//relation[@from='"+key.getAttributeValue("id")+"']");
							for(Element rel : reltemp){
								String relatedstructureid = rel.getAttributeValue("to");
								String relationname = rel.getAttributeValue("name");
								if(relationname.matches("(part_of|"+this.positionprep+")")){
									Element relatedstructure = (Element) XPath.selectSingleNode(statement, ".//structure[@id='"+relatedstructureid+"']");
									relatedstructure.removeChildren("character");
									keyrelations.add(rel, relatedstructure)
								}
							}*/
							break;
						}					
					}
				}
				//generate other EQ statements from this statement
				for(Element e: purge) e.detach();
				Element text = (Element)XPath.selectSingleNode(statement, ".//text");
				structures = XPath.selectNodes(statement, ".//structure");
				List<Element> relations = XPath.selectNodes(statement,".//relation");
				outputEQs4Statement(src, root, text, structures, relations);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return key;
	}

	/**
	 * step 2: process State Statements
	 * Use the name of E to replace the "whole_organism" placeholder in the state statements and generate EQ statements
	 * the state statements may also generate additional EQ statements
	 * If the name of E is also contained in the states, then use the name in the states
	 * @param e
	 * @param states
	 */
	@SuppressWarnings("unchecked")
	private void processStateStatements(Element e, List<Element> states, String src, Element root) {
		try{
			for(Element statement : states){
				//fill whole_organism place-holder with a real structure
				List<Element> whole_organism = XPath.selectNodes(statement, ".//structure[@name='whole_organism']");
				for(Element wo : whole_organism){
					wo.setAttribute("name", e.getAttributeValue("name"));
					changeIdsInRelations(wo.getAttributeValue("id"), e.getAttributeValue("id"), root);
					wo.setAttribute("id", e.getAttributeValue("id"));
					if(e.getAttribute("constraint")!=null){
						wo.setAttribute("constraint", e.getAttributeValue("constraint"));
					}
				}
				//generate other EQ statements from this statement
				Element text = (Element)XPath.selectSingleNode(statement, ".//text");
				List<Element> structures = XPath.selectNodes(statement, ".//structure");
				//relations should include those in this state statement and those in character statement
				List<Element> relations = XPath.selectNodes(statement,".//relation"); 
				relations.addAll(XPath.selectNodes(root, ".//statement[@statement_type='character']/relation"));
				outputEQs4Statement(src, root, text, structures, relations);
			}			
		}catch(Exception ex){
			ex.printStackTrace();
		}		
	}

	/**
	 * search in all relations in root and replace oldid with newid for all from and to attributes
	 * @param oldid
	 * @param newid
	 * @param root
	 */
	@SuppressWarnings("unchecked")
	private void changeIdsInRelations(String oldid,
			String newid, Element root) {
		try{
			List<Element> rels = XPath.selectNodes(root, "//relation[@to='"+oldid+"'|@from='"+oldid+"']");
			for(Element rel : rels){
				if(rel.getAttributeValue("to").compareTo(oldid)==0) rel.setAttribute("to", newid);
				if(rel.getAttributeValue("from").compareTo(oldid)==0) rel.setAttribute("from", newid);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}


	/**
	 * 
	 * @param src
	 * @param root
	 * @param textelement
	 * @param structures
	 * @param relations
	 */
	private void outputEQs4Statement(String src, Element root,  Element textelement, List<Element> structures,
			List<Element> relations) {
		String text = textelement.getText();
		System.out.println("text::"+text);
		
		//process relations first and hold the information in hashtable
		Hashtable<String, String> rels = new Hashtable<String, String> (); //fromstructureid => (relation name) tostructureid 
		Iterator<Element> it = relations.iterator();
		while(it.hasNext()){
			Element rel = it.next();
			String fromid = rel.getAttributeValue("from");
			String toid = rel.getAttributeValue("to");
			String relname = rel.getAttributeValue("name").trim();
			String neg = rel.getAttributeValue("negation");
			if(neg.compareTo("true")==0){
				relname = "not "+relname+"";
			}
			if(rels.get(fromid)==null){
				rels.put(fromid, "("+relname+")"+toid);
			}else{
				rels.put(fromid, rels.get(fromid)+"#("+relname+")"+toid);
			}
		}
		
		//process structures: output
		 it = structures.iterator();
		while(it.hasNext()){
			Element struct = it.next();
			outputEQs4Structure(src, root, text, struct, rels);
		}
	}

	private void addEQStatement(String src, String text, String entity, String entityid, String quality, String qualityid, String qualitymodifier, String qualitymodifierid, String entitylocator, String entitylocatorid) {
		String q = "insert into "+this.outputtable+" (source, description, entity, entityid, quality, qualityid, qualitymodifier, qualitymodifierid, entitylocator, entitylocatorid) values " +
				"('"+src+"','"+text+"','"+ entity+"','"+ entityid+"','"+ quality+"','"+ qualityid+"','"+qualitymodifier+"','"+ qualitymodifierid+"','"+ entitylocator+"','"+ entitylocatorid+"')";
		try{
			Statement stmt = conn.createStatement();
			stmt.execute(q);
			System.out.println("EQ::[E]"+entity+" [Q]"+quality+(qualitymodifier.length()>0? " [QM]"+qualitymodifier :"")+(entitylocator.length()>0? " [EL]"+entitylocator :""));
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

	/**
	 * 
	 * @param src
	 * @param root
	 * @param text
	 * @param struct
	 */
	@SuppressWarnings("unchecked")
	private void outputEQs4Structure(String src, Element root, String text, Element struct, Hashtable<String, String> relations) {
		String structid = struct.getAttributeValue("id");
		String[] rels = null;
		String arelation = relations.get(structid);
		if(arelation!=null) rels = arelation.split("#");
		String structname = this.getStructureName(root, structid);
		try{
			List<Element> chars = XPath.selectNodes(struct, ".//character");
			Iterator<Element> it = chars.iterator();
			boolean hascharacter = false;
			while(it.hasNext()){
				hascharacter = true;
				Element chara = it.next();
				String charatype = chara.getAttribute("char_type")!=null ? "range" : "discrete"; 
				//characters
				String quality = "";
				if(charatype.compareTo("range")==0){
					quality = chara.getAttributeValue("from")+
							  " "+
							  (chara.getAttribute("from_unit")!=null? chara.getAttributeValue("from_unit"): "")+
							  " to "+
							  chara.getAttributeValue("to")+
							  " "+
							  (chara.getAttribute("to_unit")!=null?  chara.getAttributeValue("to_unit") : "");

				}else{
					quality = (chara.getAttribute("modifier")!=null && chara.getAttributeValue("modifier").matches(".*?\\bnot\\b.*")? "not" : "")	
					          +" "+chara.getAttributeValue("value")+
					          " "+
					 		  (chara.getAttribute("unit")!=null? chara.getAttributeValue("unit"): "")+"["+
					 		  (chara.getAttribute("modifier")!=null? chara.getAttributeValue("modifier").replaceAll("\\bnot\\b;?", "") : "")+"]";
					         		  
				}
				quality = quality.replaceAll("\\[\\]", "").replaceAll("\\s+", " ").trim();
				//constraints
				String qualitymodifier = "";
				if(chara.getAttribute("constraintid")!=null){
					String[] ids = chara.getAttributeValue("constraintid").split("\\s*[;,]\\s*");
					for(String id: ids){
						qualitymodifier +=this.getStructureName(root, id)+";";
					}
					qualitymodifier = qualitymodifier.replaceFirst(";$", "");
				}
				//relations: may be entitylocators or qualitymodifiers
				String entitylocator = "";
				if(rels!=null){
					for(String r : rels){
						String toid = r.replaceFirst(".*?\\)", "").trim();
						String toname = this.getStructureName(root, toid);
						if(r.matches("\\((part_of|"+this.positionprep+")\\).*")){ //entitylocator							
							entitylocator += toname+";";
						}else if(r.matches("(with).*")){							
							//do nothing
						}else if(r.matches("(without).*")){							
							//output absent as Q for toid
							addEQStatement(src, text, toname,"", "absent", "", "", "", "", ""); //without entity locators, which are treated in relation processing 
						}else{
							qualitymodifier +=toname+";";
						}
					}
					entitylocator = entitylocator.replaceFirst(";$", "");
					qualitymodifier = qualitymodifier.replaceFirst(";$", "");
				}
				addEQStatement(src, text, structname,"", quality, "", qualitymodifier, "", entitylocator, ""); //without entity locators, which are treated in relation processing 
			}
			if(!hascharacter && rels != null){
				//this is the case where the structure's character information is expressed in the relations (it has no character elements, but is involved in some relations)
				for(String rel : rels){ //rel: (covered in)o621
					//make "covered in" a quality and "o621" quality modifier
					if(!rel.matches("\\((part_of|"+this.positionprep+")\\).*")){//exclude Locator relations
						String toid = rel.replaceFirst(".*?\\)", "").trim();
						String quality = rel.replace(toid, "").replaceAll("[()]", "").trim();
						String qualitymodifier = this.getStructureName(root, toid);		
						addEQStatement(src, text, structname,"", quality, "", qualitymodifier, "", "", ""); //without entity locators, which are treated in relation processing 
					}
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
		String srcdir = "C:/Documents and Settings/Hong Updates/Desktop/Australia/phenoscape-fish-source/target/final";
		String database = "phenoscape";
		String outputtable = "biocreative_nexml2eq";
		XML2EQ x2e = new XML2EQ(srcdir, database, outputtable);
		x2e.outputEQs();
	}

}
