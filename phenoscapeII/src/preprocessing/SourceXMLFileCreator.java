/**
 * 
 */
package preprocessing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


import org.jdom.Element;
import org.jdom.Document;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;




/**
 * @author Hong Updates
 *This class reads character statements from database, 
 *output XML files, one for each character statement
 *<treatment><character><description> 
 *The XML files will be used as Type 3 source files for the Parser 
 */
public class SourceXMLFileCreator {
	private File output;
	private Connection conn;
	private String database;
	private String sourcetable;
	private String tableprefix;
	private static String username="phenoscape";
	private static String password="pheno!scape";

	private static StringBuffer text = new StringBuffer();
	

	/**
	 * constructor
	 */
	public SourceXMLFileCreator(String tablename, String outputdir, String database, String tableprefix) {
		this.output = new File(outputdir);		
		if(output.exists()){
			File[] all = this.output.listFiles();
			for(int i =0; i<all.length; i++){
				all[i].delete();
			}
		}
		
		this.database = database;
		this.tableprefix = tableprefix;
		this.sourcetable = tableprefix+"_"+tablename;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void outputXMLFiles(){
		try{			
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct source from "+this.sourcetable);
			while(rs.next()){
				String src = rs.getString("source");
				Statement stmt1 = conn.createStatement();
				String q = "select distinct characterr, sentence from "+this.sourcetable+" where source='"+src+"'";
				ResultSet rs1 = stmt1.executeQuery(q);
				boolean ch = false;
				Element root = new Element("treatment");
				StringBuffer sb = new StringBuffer();
				while(rs1.next()){//one character + n sentences
					if(!ch){
						Element chara = new Element("character");
						chara.setText(rs1.getString("characterr").trim());
						root.addContent(chara);
						ch = true;
					}
					String sent = rs1.getString("sentence").trim();
					sent = sent.matches("[\\.;]$")? sent : sent+";"; 
					sb.append(sent+" ");
				}
				Element descr = new Element("description");
				String text = sb.toString().trim();
				text = text.matches("[\\.;]$")? text : text+"."; 
				descr.setText(text);
				root.addContent(descr);
				root.detach();
				//output doc
				XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
				String filename = src.replaceAll(" ", "")+".xml";
				out.output(new Document(root), new BufferedOutputStream(new FileOutputStream(new File(this.output, filename))));
				System.out.println("Write "+filename);
			}
		}catch(Exception e){
			e.printStackTrace();
		}

	}
	
	/*private void readfile (File f){
		try{
			FileInputStream istream = new FileInputStream(f);
			InputStreamReader inread = new InputStreamReader(istream);
			BufferedReader buff = new BufferedReader(inread);
			String source = f.getName();
			String s="";
			String ch = "";
			String sent = "";
			int charid = 1;
			int sentid = 1;
			boolean startc = false;
			boolean starts = false;
			while((s = buff.readLine())!=null){
				if(s.trim().length()==0){
					ch = "";
					startc = true;
					starts = false;
				}else if(startc){
					//read and concat character line
					ch +=s+" "; 
				}
				if(s.trim().matches("\\d+:.*")){
					startc = false;
					starts = true;
					insertCharacter(ch.trim(), charid, source);
					if(sent.trim().length()>0){
						insertSentence(sent.trim(), sentid++, source, charid);
						sent = "";
					}
					charid++;
				}
				if(starts){
					sent +=s+" ";
				}
			}			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	private void insertSentence(String sentence, int sentid, String source, int charid) {
		String clean = sentence.replaceFirst("^\\d+:", "").replaceAll("\\([^)]*\\)", "").trim();
		this.text.append(clean.replaceFirst("\\W\\s*$", "")+"; ");
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+this.tableprefix+"_sentence (sentid, source, sentence, originalsent, charid) values ("+sentid+",'"+source+"','"+clean+"','"+sentence+"',"+charid+")");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private void insertCharacter(String character, int charid, String source) {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+this.tableprefix+"_character (charid, source, characterr) values ("+charid+",'"+source+"','"+character+"')");
		}catch(Exception e){
			e.printStackTrace();
		}
	}*/

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String source = "original";
		String output = "Z:\\DATA\\phenoscape\\subcontract\\fish\\source";
		String database = "phenoscape";
		String tableprefix = "fish";
		SourceXMLFileCreator sfc = new SourceXMLFileCreator(source, output, database, tableprefix);
		sfc.outputXMLFiles();
	}

}
