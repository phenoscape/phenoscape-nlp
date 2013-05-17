/**
 * 
 */
package preprocessing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;


/**
 * @author Hong Updates
 *This class reads character statements from text files, striping off the numbering, and
 *saving the sentences in a table. 
 *The characters are saved in another table  
 */
public class CharacterDescriptionReader {
	private File source;
	private File output;
	private Connection conn;
	private String tableprefix;
	private static String username="phenoscape";
	private static String password="pheno!scape";
	private static String statementtable = "sentence";
	private static String charactertable = "character";
	private static StringBuffer text = new StringBuffer();
	

	/**
	 * constructor
	 */
	public CharacterDescriptionReader(String source, String output, String database, String tableprefix) {
		this.source = new File(source);
		this.output = new File(output);
		this.tableprefix = tableprefix;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("drop table if exists "+tableprefix+"_"+statementtable);
				stmt.execute("create table if not exists "+tableprefix+"_"+statementtable+" (sentid int(11) not null unique, source varchar(500), sentence text, originalsent text, lead varchar(2000), status varchar(20), tag varchar(500),modifier varchar(150), charid int(11), primary key (sentid)) engine=innodb");
				stmt.execute("drop table if exists "+tableprefix+"_"+charactertable);
				stmt.execute("create table if not exists "+tableprefix+"_"+charactertable+" (charid int(11) not null unique, source varchar(500), characterr varchar(500),  primary key (charid)) engine=innodb");
				stmt.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * blank-line separated descriptions, each contains 1 character and N numbered-character statement
	 * Ex:

93. Development of gas bladder (ORDERED). (CI =
0.333, RI = 0.879)
0: Gas bladder not reduced, with large anterior and poste-
rior chambers.
1: Gas bladder somewhat reduced, with large anterior
chamber and small posterior chambers. (Chiloglanis sp.
‘kalungwishi’; Chiloglanis sp. ‘burundi’; Atopochilus;
Euchilichthys)
2: Gas bladder greatly reduced, with small anterior chamber
only. (Amphiliidae; all Chiloglanis except C. macropterus
[0], Chiloglanis sp. ‘kalungwishi’ [1] and Chiloglanis sp.
‘burundi’ [1]; Atopodontus)

	 */
	public void read(){
		File[] files = source.listFiles();
		for(int i = 0; i<files.length; i++){
			readfile(files[i]);
		}
		try{
			FileWriter wrt = new FileWriter(output);
		    wrt.append(CharacterDescriptionReader.text.toString());
		    wrt.flush();
		    wrt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void readfile (File f){
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
		CharacterDescriptionReader.text.append(clean.replaceFirst("\\W\\s*$", "")+"; ");
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
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String source = "Z:\\DATA\\phenoscape\\text";
		String output = "Z:\\DATA\\phenoscape\\descriptions\\vigliotta_2008.txt";
		String database = "phenoscape";
		String tableprefix = "test";
		CharacterDescriptionReader cdr = new CharacterDescriptionReader(source, output, database, tableprefix);
		cdr.read();
	}

}
