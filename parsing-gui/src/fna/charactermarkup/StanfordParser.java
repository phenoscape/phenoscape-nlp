 /* $Id: StanfordParser.java 997 2011-10-07 01:14:22Z hong1.cui $ */
/**
 * 
 */
package fna.charactermarkup;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.*;
import java.io.File;
import java.util.regex.*;

import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.parsing.ApplicationUtilities;
import fna.parsing.Learn2Parse;
import fna.parsing.Registry;
import fna.parsing.VolumeFinalizer;
import fna.parsing.state.SentenceOrganStateMarker;


/**
 * @author hongcui
 * updates April 2011
 * 	1. annual etc. => life_style
	2. added may_be_the_same relation for structures with the same name and constraint.
	3. to 60 m => marked as a range value with to_value="60" to_unit="m"
	4. separate two types of constraints for structure element (see below)
	5. removed empty values
	6. put <text> element as the first element in output
 */
@SuppressWarnings({ "unused","static-access" })
public class StanfordParser implements Learn2Parse, SyntacticParser{
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "root";
	//protected int count = 0;
	static private int allchunks = 0;
	static private int discoveredchunks = 0;
	private File posedfile = null;
	private File parsedfile = null;
	private String POSTaggedSentence = "POSedSentence";
	private POSTagger4StanfordParser tagger = null;
	private String tableprefix = null;
	private String glosstable = null;
	//private SentenceOrganStateMarker sosm = null;
	//private Hashtable sentmapping = new Hashtable();
	private boolean finalize = true;
	//private boolean debug = true;
	private boolean printSent = true;
	private boolean printProgress = true;
	private boolean evaluation = false;
	/**
	 * 
	 */
	public StanfordParser(String posedfile, String parsedfile, String database, String tableprefix, String glosstable, boolean evaluation) {
		// TODO Auto-generated constructor stub
		this.posedfile = new File(posedfile); 
		this.parsedfile = new File(parsedfile);
		this.database = database;
		this.tableprefix = tableprefix;
		this.glosstable = glosstable;
		this.evaluation = evaluation;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = "jdbc:mysql://localhost/"+database+"?user=termsuser&password=termspassword";
				//String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
			Statement stmt = conn.createStatement();
			stmt.execute("create table if not exists "+this.tableprefix+"_"+this.POSTaggedSentence+"(source varchar(100) NOT NULL, posedsent TEXT, PRIMARY KEY(source))");
			stmt.execute("delete from "+this.tableprefix+"_"+this.POSTaggedSentence);
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		tagger = new POSTagger4StanfordParser(conn, this.tableprefix, glosstable);
	}
	
	public void POSTagging(){
		try{
  			Statement stmt = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			FileOutputStream ostream = new FileOutputStream(posedfile); 
			PrintStream out = new PrintStream( ostream );
			
			//ResultSet rs = stmt.executeQuery("select * from newsegments");
			//stmt.execute("alter table markedsentence add rmarkedsent text");

			ResultSet rs = stmt.executeQuery("select source, markedsent from "+this.tableprefix+"_markedsentence order by sentid");// order by (source+0) ");////sort as numbers
			//ResultSet rs = stmt.executeQuery("select source, sentence from "+this.tableprefix+"_sentence order by sentid");// order by (source+0) ");////sort as numbers
			int count = 1;
			while(rs.next()){
				String src = rs.getString(1);
				String str = rs.getString(2);
				//TODO: may need to fix "_"
				//if(src.compareTo("56.txt-7")!=0) continue;
				str = tagger.POSTag(str, src);
	       		stmt2.execute("insert into "+this.tableprefix+"_"+this.POSTaggedSentence+" values('"+rs.getString(1)+"','"+str+"')");
	       		out.println(str);
	       		count++;
	       		
			}
			stmt2.close();
			rs.close();
			out.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * direct the parsed result to the text file parsedfile
	 */
	public void parsing(){
		PrintStream out = null;
		Pattern ptn = Pattern.compile("^Parsing \\[sent\\. (\\d+) len\\. \\d+\\]:");
	  	try{
	  		FileOutputStream ostream = new FileOutputStream(parsedfile); 
			out = new PrintStream( ostream );	
 	  		Runtime r = Runtime.getRuntime();
	  		//Process p = r.exec("cmd /c stanfordparser.bat");
 	  		//String cmdtext = "stanfordparser.bat >"+this.parsedfile+" 2<&1";
 	  		//String cmdtext = "cmd /c stanfordparser.bat";	
 	  		//String parserJarfilePath = ApplicationUtilities.getProperty("stanford.parser.jar"); 
	  		//String englishPCFGpath = ApplicationUtilities.getProperty("englishPCFG");
	  		String parserJarfilePath="lib\\stanford-parser.jar";
	  		String englishPCFGpath ="lib\\englishPCFG.ser.gz";
 	  		String cmdtext = "java -mx900m -cp "+parserJarfilePath+" edu.stanford.nlp.parser.lexparser.LexicalizedParser " +
	  				"-sentences newline -tokenized -tagSeparator / "+englishPCFGpath+" \""+
	  				this.posedfile+"\"";
 	  		System.out.println("parser path::"+cmdtext);
 	  		
	  		Process proc = r.exec(cmdtext);
          
		    ArrayList<String> headings = new ArrayList<String>();
	  	    ArrayList<String> trees = new ArrayList<String>();
	  
            // any error message?
            StreamGobbler errorGobbler = new 
                StreamGobbler(proc.getErrorStream(), "ERROR", headings, trees);            
            
            // any output?
            StreamGobbler outputGobbler = new 
                StreamGobbler(proc.getInputStream(), "OUTPUT", headings, trees);
                
            // kick them off
            errorGobbler.start();
            outputGobbler.start();
                                    
            // any error???
            int exitVal = proc.waitFor();
            System.out.println("ExitValue: " + exitVal);

			//format
            if(headings.size() != trees.size()){
            	System.err.println("Error reading parsing results");
            	System.exit(2);
            }
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i<headings.size(); i++){
            	sb.append(headings.get(i)+System.getProperty("line.separator"));
            	sb.append(trees.get(i)+System.getProperty("line.separator"));
            }
            PrintWriter pw = new PrintWriter(new FileOutputStream(this.parsedfile));
            pw.print(sb.toString());
            pw.flush();
            pw.close();			
	  	}catch(Exception e){
	  		e.printStackTrace();
	  	}
	  	//out.close();
	}
	
	
	public void extracting(){
    	try{
	       //String test="(ROOT  (S (NP      (NP (NN body) (NN ovoid))      (, ,)      (NP        (NP (CD 2-4))        (PP (IN x)          (NP            (NP (CD 1-1.5) (NN mm))            (, ,)            (ADJP (RB not) (JJ winged)))))      (, ,))    (VP (VBZ woolly))    (. .)))";
	       // test="(ROOT  (NP    (NP      (NP (NNP Ray))      (ADJP (JJ laminae)        (NP (CD 6))))    (: -)    (NP      (NP        (NP (CD 7) (NNS x))        (NP (CD 2/CD-32) (NN mm)))      (, ,)      (PP (IN with)        (NP (CD 2))))    (: -)    (NP      (NP (CD 5) (NNS hairs))      (PP (IN inside)        (NP          (NP (NN opening))          (PP (IN of)            (NP (NN tube))))))    (. .)))";
	       // test="(S (NP (NP (NN margins) (UCP (NP (JJ entire)) (, ,) (ADJP (JJ dentate)) (, ,) (ADJP (RB pinnately) (JJ lobed)) (, ,) (CC or) (NP (JJ pinnatifid) (NN pinnately))) (NN compound)) (, ,) (NP (JJ spiny)) (, ,)) (VP (JJ tipped) (PP (IN with) (NP (NNS tendrils)))) (. .))";
	        FileInputStream istream = new FileInputStream(this.parsedfile); 
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(istream));
			String line = "";
			String text = "";
			int i = 0;
			Statement stmt = conn.createStatement();

			ResultSet rs = stmt.executeQuery("select source, rmarkedsent from "+this.tableprefix+"_markedsentence order by sentid");//(source+0)"); //+0 so sort as numbers
			//ResultSet rs = stmt.executeQuery("select source, sentence from "+this.tableprefix+"_sentence order by sentid");//(source+0)"); //+0 so sort as numbers

			Pattern ptn = Pattern.compile("^Parsing \\[sent\\. (\\d+) len\\. \\d+\\]:(.*)");
			Matcher m = null;
			Tree2XML t2x = null;
			Document doc = null;
			CharacterAnnotatorChunked cac = new CharacterAnnotatorChunked(conn, this.tableprefix, glosstable, this.evaluation);
			SentenceChunker4StanfordParser ex = null;
			Element statement = null;
			ChunkedSentence cs = null;
			String pdescID ="";
			int order = 0;
			//int pfileindex = 0;
			String pfileindex = "";
			Element baseroot = null;
			Element description = new Element("description");
			while ((line = stdInput.readLine())!=null){
				if(line.startsWith("Loading") || line.startsWith("X:") || line.startsWith("Parsing file")|| line.startsWith("Parsed") ){continue;}
				if(line.trim().length()>1){
					m = ptn.matcher(line);
					if(m.matches()){
						i = Integer.parseInt((String)m.group(1));
					}else{
						text += line.replace(System.getProperty("line.separator"), ""); 
					}
				}else{
					//if(i != 359 && i !=484 && i!=517 && i!=549 && i != 1264 && i!=1515 && i!=1613 && i !=1782 && i !=2501 && i !=2793 && i!=4798 && i!=9243 && i!=10993 && i!=12449 && text.startsWith("(ROOT")){
					//if(i !=2793 && text.startsWith("(ROOT")){//FNAv19 865[162.txt-0], 310, 1638 (SentenceOrganStateMarkup); 5262[466.txt-14]
					if(/*194.txt-5*&&*/ text.startsWith("(ROOT")){//treatiseh
					//if(/*i != 2468 && i != 3237 &&i != 9555 && i != 9504 &&*/ i !=2018 && i !=2793 && text.startsWith("(ROOT")){//bhl	
					text = text.replaceAll("(?<=[A-Z])\\$ ", "S ");
					t2x = new Tree2XML(text);
					doc = t2x.xml();
					//Document doccp = (Document)doc.clone();
					if(rs.relative(i)){
						String sent = rs.getString(2);
						String src = rs.getString(1);
						String thisdescID = src.replaceFirst("-\\d+$", "");//1.txtp436_1.txt-0's descriptionID is 1.txtp436_1.txt
						//int thisfileindex = Integer.parseInt(src.replaceFirst("\\.txt.*$", ""));
						String thisfileindex = src.replaceFirst("\\.txt.*$", "");
						if(finalize){
							if(baseroot ==null){
								order++;
								baseroot = VolumeFinalizer.getBaseRoot(thisfileindex, order);
							}
						}
						//sent = this.normalizeSpacesRoundNumbers(sent);
							if(!sent.matches(".*? [;\\.]\\s*$")){//at 30x. => at 30x. .
								sent = sent+" .";
							}
							sent = sent.replaceAll("<\\{?times\\}?>", "times");
							sent = sent.replaceAll("<\\{?diam\\}?>", "diam");
							sent = sent.replaceAll("<\\{?diams\\}?>", "diams");
							ex = new SentenceChunker4StanfordParser(i, doc, sent, src, this.tableprefix, conn, glosstable);
							cs = ex.chunkIt();
							//System.out.print("["+src+"]:");
							if(this.printSent){
								System.out.println();
								System.out.println(i+"["+src+"]: "+cs.toString());

							}
							statement = cac.annotate(src, src, cs); //src: 100.txt-18
							if(finalize){
								if(thisdescID.compareTo(pdescID)!=0){
									if(description.getChildren().size()!=0){ //not empty
										//plug description in XML document
										//write the XML to final
										//call MainForm to display
										//VolumeFinalizer.replaceWithAnnotated(description, count, "/treatment/description", "FINAL", false);
										
										placeDescription(description, pdescID, baseroot);
										cac.reset();
										description = new Element("description");
										if(this.printProgress){
											System.out.println(pfileindex+".xml written");
										}
									}
								}
								
								//if(thisfileindex != pfileindex){
								if(thisfileindex.compareTo(pfileindex) !=0){
									//if(pfileindex !=0){
									if(pfileindex.length() !=0){
										order++;
										VolumeFinalizer.outputFinalXML(baseroot, pfileindex, "FINAL");
										baseroot = VolumeFinalizer.getBaseRoot(thisfileindex, order);
									}								
								}
							}
							description.addContent(statement);
							pdescID = thisdescID;
							pfileindex = thisfileindex;
						
						rs.relative(i*-1); //reset the pointer
					}
					}
					text = "";
					i = 0;
				}
			}
			if(finalize){
				placeDescription(description, pdescID, baseroot);
				VolumeFinalizer.outputFinalXML(baseroot, pfileindex, "FINAL");		
			}
			rs.close();
    	}catch (Exception e){
    		//System.err.println(e);
			e.printStackTrace();
        }
    }

	public static String normalizeSpacesRoundNumbers(String sent) {
		sent = ratio2number(sent);//bhl
		sent = sent.replaceAll("(?<=\\d)\\s*/\\s*(?=\\d)", "/");
		sent = sent.replaceAll("(?<=\\d)\\s+(?=\\d)", "-"); //bhl: two numbers connected by a space
		sent = sent.replaceAll("at least", "at-least");
		sent = sent.replaceAll("2\\s*n\\s*=", "2n=");
		sent = sent.replaceAll("2\\s*x\\s*=", "2x=");
		sent = sent.replaceAll("n\\s*=", "n=");
		sent = sent.replaceAll("x\\s*=", "x=");

		//sent = sent.replaceAll("[–—-]", "-").replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\[", " [ ").replaceAll("\\]", " ] ").replaceAll("\\(", " ( ").replaceAll("\\)", " ) ").replaceAll("\\s+", " ").trim();
		sent = sent.replaceAll("[–—-]", "-").replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\s+", " ").trim();
		sent = sent.replaceAll("(?<=\\d) (?=\\?)", ""); //deals especially x=[9 ? , 13] 12, 19 cases
		sent = sent.replaceAll("(?<=\\?) (?=,)", "");
		if(sent.matches(".*?[nx]=.*")){
			sent = sent.replaceAll("(?<=[\\d?])\\s*,\\s*(?=\\d)", ","); //remove spaces around , for chromosome only so numericalHandler.numericalPattern can "3" them into one 3. Other "," connecting two numbers needs spaces to avoid being "3"-ed (fruits 10, 3 of them large) 
		}
		sent = sent.replaceAll("\\b(?<=\\d+) \\. (?=\\d+)\\b", ".");//2 . 5 => 2.5
		sent = sent.replaceAll("(?<=\\d)\\.(?=\\d[nx]=)", " . "); //pappi 0.2n=12
		
		
		//sent = sent.replaceAll("(?<=\\d)\\s+/\\s+(?=\\d)", "/"); // 1 / 2 => 1/2
		//sent = sent.replaceAll("(?<=[\\d()\\[\\]])\\s+[–—-]\\s+(?=[\\d()\\[\\]])", "-"); // 1 - 2 => 1-2
		//sent = sent.replaceAll("(?<=[\\d])\\s+[–—-]\\s+(?=[\\d])", "-"); // 1 - 2 => 1-2
		
		//4-25 [ -60 ] => 4-25[-60]: this works only because "(text)" have already been removed from sentence in perl program
		sent = sent.replaceAll("\\(\\s+(?=[\\d\\+\\-])", "("). //"( 4" => "(4"
		replaceAll("(?<=[\\d\\+\\-])\\s+\\(", "("). //" 4 (" => "4("
		replaceAll("\\)\\s+(?=[\\d\\+\\-])", ")"). //") 4" => ")4"
		replaceAll("(?<=[\\d\\+\\-])\\s+\\)", ")"); //"4 )" => "4)"
		
		sent = sent.replaceAll("\\[\\s+(?=[\\d\\+\\-])", "["). //"[ 4" => "[4"
		replaceAll("(?<=[\\d\\+\\-])\\s+\\[", "["). //" 4 [" => "4["
		replaceAll("\\]\\s+(?=[\\d\\+\\-])", "]"). //"] 4" => "]4"
		replaceAll("(?<=[\\d\\+\\-])\\s+\\]", "]"); //"4 ]" => "4]"
		
		/*Pattern p = Pattern.compile("(.*?)(\\d*)\\s+\\[\\s+([ –—+\\d\\.,?×/-]+)\\s+\\]\\s+(\\d*)(.*)");  //4-25 [ -60 ] => 4-25[-60]. ? is for chromosome count
		Matcher m = p.matcher(sent);
		while(m.matches()){
			sent = m.group(1)+ (m.group(2).length()>0? m.group(2):" ")+"["+m.group(3).replaceAll("\\s*[–—-]\\s*", "-")+"]"+(m.group(4).length()>0? m.group(4):" ")+m.group(5);
			m = p.matcher(sent);
		}
		////keep the space after the first (, so ( 3-15 mm) will not become 3-15mm ) in POSTagger.
		p = Pattern.compile("(.*?)(\\d*)\\s+\\(\\s+([ –—+\\d\\.,?×/-]+)\\s+\\)\\s+(\\d*)(.*)");  //4-25 ( -60 ) => 4-25(-60)
		//p = Pattern.compile("(.*?)(\\d*)\\s*\\(\\s*([ –—+\\d\\.,?×/-]+)\\s*\\)\\s*(\\d*)(.*)");  //4-25 ( -60 ) => 4-25(-60)
		m = p.matcher(sent);
		while(m.matches()){
			sent = m.group(1)+ (m.group(2).length()>0? m.group(2):" ")+"("+m.group(3).replaceAll("\\s*[–—-]\\s*", "-")+")"+(m.group(4).length()>0? m.group(4):" ")+m.group(5);
			m = p.matcher(sent);
		}*/
		
		sent = sent.replaceAll("\\s+/\\s+", "/"); //and/or 1/2
		sent = sent.replaceAll("\\s+×\\s+", "×");
		sent = sent.replaceAll("\\s*\\+\\s*", "+"); // 1 + => 1+
		sent = sent.replaceAll("(?<![\\d()\\]\\[×-])\\+", " +");
		sent = sent.replaceAll("\\+(?![\\d()\\]\\[×-])", "+ ");
		sent = sent.replaceAll("(?<=(\\d))\\s*\\?\\s*(?=[\\d)\\]])", "?"); // (0? )
		sent = sent.replaceAll("\\s*-\\s*", "-"); // 1 - 2 => 1-2, 4 - {merous} => 4-{merous}
		sent = sent.replaceAll("(?<=[\\d\\+-][\\)\\]])\\s+(?=[\\(\\[][\\d-])", "");//2(–3) [–6]  ??
		//%,°, and ×
		sent = sent.replaceAll("(?<![a-z])\\s+%", "%").replaceAll("(?<![a-z])\\s+°", "°").replaceAll("(?<![a-z ])\\s*×\\s*(?![ a-z])", "×");
		/*if(sent.indexOf(" -{")>=0){//1–2-{pinnately} or -{palmately} {lobed} => {1–2-pinnately-or-palmately} {lobed}
			sent = sent.replaceAll("\\s+or\\s+-\\{", "-or-").replaceAll("\\s+to\\s+-\\{", "-to-").replaceAll("\\s+-\\{", "-{");
		}*/
		return sent;
	}
	
	public static String ratio2number(String sent){
		String small = "\\b(?:one|two|three|four|five|six|seven|eight|nine)\\b";
		String big = "\\b(?:half|third|fourth|fifth|sixth|seventh|eighth|ninth|tenth)s?\\b";
		//ratio
		Pattern ptn = Pattern.compile("(.*?)("+small+"\\s*-?_?\\s*"+big+")(.*)");
		Matcher m = ptn.matcher(sent);
		while(m.matches()){
			String ratio = m.group(2);
			ratio = toRatio(ratio);
			sent = m.group(1)+ratio+m.group(3);
			m = ptn.matcher(sent);
		}
		//number
		small = "\\b(?:two|three|four|five|six|seven|eight|nine)\\b";
		ptn = Pattern.compile("(.*?)("+small+")(.*)");
		m = ptn.matcher(sent);
		while(m.matches()){
			String number = m.group(2);
			number = toNumber(number);
			sent = m.group(1)+number+m.group(3);
			m = ptn.matcher(sent);
		}
		sent = sent.replaceAll("(?<=\\d)\\s*to\\s*(?=\\d)", "-");
		return sent;
	}

	public static String toNumber(String ratio){
		ratio = ratio.replaceAll("\\btwo\\b", "2");
		ratio = ratio.replaceAll("\\bthree\\b", "3");
		ratio = ratio.replaceAll("\\bfour\\b", "4");
		ratio = ratio.replaceAll("\\bfive\\b", "5");
		ratio = ratio.replaceAll("\\bsix\\b", "6");
		ratio = ratio.replaceAll("\\bseven\\b", "7");
		ratio = ratio.replaceAll("\\beight\\b", "8");
		ratio = ratio.replaceAll("\\bnine\\b", "9");
		return ratio;
	}
	
	public static String toRatio(String ratio){
		ratio = ratio.replaceAll("\\bone\\b", "1/");
		ratio = ratio.replaceAll("\\btwo\\b", "2/");
		ratio = ratio.replaceAll("\\bthree\\b", "3/");
		ratio = ratio.replaceAll("\\bfour\\b", "4/");
		ratio = ratio.replaceAll("\\bfive\\b", "5/");
		ratio = ratio.replaceAll("\\bsix\\b", "6/");
		ratio = ratio.replaceAll("\\bseven\\b", "7/");
		ratio = ratio.replaceAll("\\beight\\b", "8/");
		ratio = ratio.replaceAll("\\bnine\\b", "9/");
		ratio = ratio.replaceAll("\\bhalf\\b", "2");
		ratio = ratio.replaceAll("\\bthirds?\\b", "3");
		ratio = ratio.replaceAll("\\bfourths?\\b", "4");
		ratio = ratio.replaceAll("\\bfifths?\\b", "5");
		ratio = ratio.replaceAll("\\bsixthths?\\b", "6");
		ratio = ratio.replaceAll("\\bsevenths?\\b", "7");
		ratio = ratio.replaceAll("\\beighths?\\b", "8");
		ratio = ratio.replaceAll("\\bninths?\\b", "9");
		ratio = ratio.replaceAll("\\btenths?\\b", "10");
		ratio = ratio.replaceAll("-", "").replaceAll("\\s", "");
		return ratio;
	}

	/**
	 * depending on the type of descriptionID, call different replaceWithAnnotated methods in VolumeFinalizer
	 * @param description
	 * @param dID: may be 1.txt or 1.txtp436_1.txt
	 */
	
	private void placeDescription(Element description, String dID, Element baseroot) {
		/*validate description, incomplete
		SAXBuilder builder = new SAXBuilder("org.apache.xerces.parsers.SAXParser", true);
		builder.setFeature("http://apache.org/xml/features/validation/schema", true);
		builder.setProperty(
				  "http://apache.org/xml/properties/schema/external-schemaLocation",
				  "http://biosemantics.googlecode.com/svn/trunk/characterStatements/ characterAnnotationSchema.xsd");
		builder.build(description);*/
		
		if(dID.indexOf(".txtp")>=0){
			String pid = dID.replaceFirst("\\.txt$", "");
			VolumeFinalizer.replaceWithAnnotated(description, baseroot, ".//description[@pid=\""+pid+"\"]");			
		}else{
			VolumeFinalizer.replaceWithAnnotated(description, baseroot, ".//description");
		}
		
	}
	
	
	public ArrayList<String> getMarkedDescription(String source){
		return null;
	}
	
	public static void countChunks(int all, int discovered){
		StanfordParser.allchunks += all;
		StanfordParser.discoveredchunks += discovered;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String text=", ( 2 – ) 2 . 5 – 3 . 5 ( – 4 ) × ( 1 . 5 – ) 2 – 3 ( – 4 ) {cm} ";
		//String text="blades ± obovate , [ 0 ] ( 1 – ) 2 – 3 - pinnately lobed , ultimate margins dentate , ";
		//String text="cypselae 9 – 18 mm , bodies terete or narrowly conic to obconic , 5 – 9 mm , beaks 3 – 10 mm , lengths ( 1 / 2 – ) 2 times bodies ;";
		//String text="2 n = 24 , 40 , [ 16 , 32 ] .";
		//String text = "x= 9 ( 18 ? ) .";
		//String text = "2 n = 24 , 40 , [ 16 , 32 ] .";
		//String text = "blades broadly elliptic or ovate to lanceolate , 6 – 12 ( – 18 + ) cm × 30 – 80 ( – 120 + ) mm , both faces sparsely pilose to hirsute .";
		//String text = "blades either linear to lanceolate and not lobed , 10 – 20 ( – 38 ) cm × 6 – 10 mm , or oblanceolate to oblong and pinnately lobed , 10 – 20 cm × 25 – 50 mm , or both ;";
		//String text = " often 2 - , 3 - , or 5 - ribbed ;";
		//<involucres> {shape~list~ovoid~to~broadly~cylindric~or~campanulate} , (2-)2.5-3.5(-4)×(1.5-)2-3(-4) {cm} , {thinly} {arachnoid} .
		//String text = "<branchlets> {slender} , 4-{sided} , {separated} from {one} another , 0 . 8 - 1 mm . {thick} , the ultimate ones 1 . 5 - 2 {cm} . {long} ;";
		//String text = "laminae 6 17 cm . long , 2 - 7 cm . broad , lanceolate to narrowly oblong or elliptic_oblong , abruptly and narrowly acuminate , obtuse to acute at the base , margin entire , the lamina drying stiffly chartaceous to subcoriaceous , smooth on both surfaces , essentially glabrous and the midvein prominent above , glabrous to sparsely puberulent beneath , the 8 to 18 pairs of major secondary veins prominent beneath and usually loop_connected near the margin , microscopic globose_capitate or oblongoid_capitate hairs usually present on the lower surface , clear or orange distally .";
		//String text = "<inflorescences> {terminal} and {axillary} , {terminal} usually occupying {distal} 1 / 5 – 1 / 3 of <stem> , rather {lax} , {interrupted} in {proximal} 1 / 2 , or almost to top , usually narrowly {paniculate} .";
		/*String text = "<petals> {lavender} or {white} , {often} {spatulate} , sometimes {oblanceolate} or {obovate} , 6 – 9 ( – 10 ) × ( 1 . 5 – ) 2 – 3 ( – 3 . 5 ) mm , <margins> not {crisped} , <claw> strongly {differentiated} from <blade> , ( {slender} , 2 – 3 . 5 ( – 4 ) mm , {narrowest} at <base> ) ;";
		text = StanfordParser.normalizeSpacesRoundNumbers(text);
		String str = Utilities.threeingSentence(text);
		String p1 ="\\([^()]*?[a-zA-Z][^()]*?\\)";
  		String p2 = "\\[[^\\]\\[]*?[a-zA-Z][^\\]\\[]*?\\]";
  		//String p3 = "\\{[^{}]*?[a-zA-Z][^{}]*?\\}";				
		if(str.matches(".*?"+p1+".*") || str.matches(".*?"+p2+".*")){ 
			str = Utilities.threeingSentence(str);
			str = str.replaceAll(p1, "").replaceAll(p2, "").replaceAll("\\s+", " ").trim();					
		}*/
		//String text = "ovary more than two to three-fourths to one half superior. ";
		//System.out.println(StanfordParser.ratio2number(text));
		//String text="<pollen> 70-100% 3-{porate} , {mean} 25 um .";
		//String text="x = [ 9 ? , 13 , 15 ] 17 , 18 , 19 .";
		String text="<stamens> 2 ( – 3 ) [ – 6 ] , {exserted} ";
		System.out.println(StanfordParser.normalizeSpacesRoundNumbers(text));
		
		
		//String database = "phenoscape";
		//String posedfile = "PSposedsentences.txt";
		//String parsedfile = "PSparsedsentences.txt";
		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "pltest", "wordpos4parser", "antglossaryfixed");

		//String database = "annotationevaluation";

		//*fna
		//String posedfile = "FNAv19posedsentences.txt";
		//String parsedfile = "FNAv19parsedsentences.txt";
		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "fnav19", "wordpos4parser", "fnaglossaryfixed");

		
		//*treatiseh
		//String posedfile = "Treatisehposedsentences.txt";
		//String parsedfile = "Treatisehparsedsentences.txt";
		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "treatiseh", "wordpos4parser", "treatisehglossaryfixed");
		
		
		//String posedfile = "Treatisehposedsentences.txt";
		//String parsedfile = "Treatisehparsedsentences.txt";
		//String database = "treatiseh_benchmark";
		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "treatiseh", "wordpos4parser", "treatisehglossaryfixed");*/
		

		//String posedfile = "BHLposedsentences.txt";
		//String parsedfile = "BHLparsedsentences.txt";
		//String database = "bhl_benchmark";
		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "bhl_clean", "wordpos4parser", "fnabhlglossaryfixed");
		
		//String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\v4\\target\\fnav4_posedsentences.txt";
		//String parsedfile ="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\v4\\target\\fnav4_parsedsentences.txt";
		//String posedfile = "C:\\temp\\DEMO\\demo-folders\\taxonX-ants_description\\target\\taxon_ants_posedsentences.txt";
		//String parsedfile="C:\\temp\\DEMO\\demo-folders\\taxonX-ants_description\\target\\taxon_ants_parsedsentences.txt";
		//String posedfile="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\plaziantfirst\\target\\plazi_ant_first_posedsentences.txt";
		//String parsedfile="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\plaziantfirst\\target\\plazi_ant_first_parsedsentences.txt";
		String posedfile="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source\\target\\pheno_fish_posedsentences.txt";
		String parsedfile="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source\\target\\pheno_fish_parsedsentences.txt";
		
		String database = "markedupdatasets";
		

		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "fnav4", "fnaglossaryfixed", false);
		//StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "plazi_ant_first", "antglossaryfixed", false);
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, "pheno_fish", "antglossaryfixed", false);

		//sp.POSTagging();
		//sp.parsing();
		sp.extracting();
		//System.out.println("total chunks: "+StanfordParser.allchunks);
		//System.out.println("discovered chunks: "+StanfordParser.discoveredchunks);
	}
}
