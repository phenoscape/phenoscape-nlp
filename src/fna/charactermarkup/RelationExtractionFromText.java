 /* $Id: RelationExtractionFromText.java 790 2011-04-11 17:57:38Z hong1.cui $ */
package fna.charactermarkup;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import fna.parsing.ProcessListener;

@SuppressWarnings({ "unused" })
public class RelationExtractionFromText {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "";
	protected int count = 0;
	private ProcessListener listener;

	public RelationExtractionFromText() {
		// TODO Auto-generated constructor stub
	}

	public RelationExtractionFromText(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}
	
	protected void collect(String database){
		RelationExtractionFromText.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists marked_pos (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent TEXT, PRIMARY KEY(sentid))");
				stmt.execute("delete from marked_pos");
				extractRelations();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	protected void extractRelations(){
		try
		{
			FileInputStream istream = new FileInputStream("F:\\UA\\RA\\Code\\parsing-gui-charactermarkup\\finaloutput.txt"); 
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(istream));
			FileOutputStream ostream = new FileOutputStream("F:\\UA\\RA\\Code\\parsing-gui-charactermarkup\\relationanalysis.txt"); 
			PrintWriter out = new PrintWriter(ostream);
        	String s = "";
        	String str = "";
        	int sentidct = 0;
        	int printcount = 0;
			while ((s=stdInput.readLine())!=null){
				//System.out.println(s);
				if(!s.contains("(ROOT")){
					str = str.concat(s);
				}					
				else{
					sentidct++;
					if(sentidct!=1){
						//System.out.println(str);
						Random r = new Random();
						if(r.nextDouble()<.05){
							String result = extract(str);
							printcount++;
							out.println("Sentid: "+(sentidct-1));
							Pattern pattern2 = Pattern.compile("[\\s]+");
					        Matcher matcher2 = pattern2.matcher(str);
					        str = matcher2.replaceAll(" ");
					        matcher2.reset();
							out.println("Sentence: "+str);
					    	out.println("Relation: "+result);
						}
						str = "";
					}
					//System.out.println(s);
				}
        	}
			String result = extract(str);
			if(result!=""){
				printcount++;
				out.println(sentidct-1);
				out.println(str);
		    	out.println(result);
			}
			//System.out.println(str);
			System.out.println(printcount);
			System.out.println(count);
		}catch (Exception e){
    		//System.err.println(e);
			e.printStackTrace();
        }
	}
	
	private Element xmlRoot(String response){
		Document doc =null;
		try {
		     DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		     DocumentBuilder db = dbf.newDocumentBuilder();
		     ByteArrayInputStream bais = new ByteArrayInputStream(response.getBytes());
		     doc = db.parse(bais);
		    } catch (Exception e) {
		      System.out.print("Problem parsing the xml: \n" + e.toString());
		}
		    
		return doc.getDocumentElement();
		
	}
	
	protected String extract (String str) {
		String relation="";
		String result="";
		Pattern pattern = Pattern.compile("\\(VP[()\\w\\s]+\\(PP[\\s]\\([\\w\\s]+\\)");
        Matcher matcher = pattern.matcher(str);
        while(matcher.find()){
			count++;
        	relation = str.substring(matcher.start(), matcher.end());
        	Pattern pattern1 = Pattern.compile("(\\(|\\)|VP|ADJP|RB|NP|PP|ADVP|CC|CD|JJ|TO[\\s]|IN[\\s]|VB(.?)|NN(.?)|DT)");
            Matcher matcher1 = pattern1.matcher(relation);
            relation = matcher1.replaceAll("");
            matcher1.reset();
            Pattern pattern2 = Pattern.compile("[\\s]+");
            Matcher matcher2 = pattern2.matcher(relation);
            relation = matcher2.replaceAll(" ");
            result = result.concat(relation+"\n");
            matcher2.reset();
        }
        matcher.reset();
        return result;
	}

	
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new RelationExtractionFromText("fnav19_benchmark");
	}

}
