/**
 * 
 */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

import java.sql.Statement;
import java.util.ArrayList;

import characterAnnotation.SplitAnnotationOutput;

import fna.parsing.state.SentenceOrganStateMarker;

/**
 * @author Hong Updates
 * use fna.v19 and treatise.h as test sets
 * each major revision of fna.charactermarkup should be evaluated using this CharaParserUnitTest class
 * the performance of a revision should not worse than reported parsing performance on fna.v19 and treatise.h
 * fna.v19: 815.txt-4 contains encoding problem, change "[]" (a box) to x.
 */

public class CharaParserUnitTest {

	
	public void testOnFna(){
		//use the annotation file path below to save the parser output
		String annotation = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\annotation.txt";
		//database setup
		Connection conn = null;
		String username="root";
		String password="root";
		String database="annotationevaluation";
		String gloss = "fnaglossaryfixed";
		//fna-benchmark
		String outdir ="C:\\DATA\\evaluation\\fnav19\\UnsupervisedStanford_Benchmark_sentence";
		String selectdir = "C:\\DATA\\evaluation\\fnav19\\UnsupervisedStanford_Benchmark_selected_sentence";
		String answerdir = "C:\\DATA\\evaluation\\fnav19\\AnsKey_Benchmark_selected_sentence";
		//fna intermediate results
		String prefix="fnav19";
		String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\v19\\posedsentences.txt";
		String parsedfile ="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\v19\\parsedsentences.txt";
		//evaluation
		String evaluationfolder="C:\\DATA\\evaluation\\fnav19";	
		String precisionrecalltable="fnav19_precisionrecall_final";

		runCharaParser(annotation, conn, username, password, database,
				gloss, outdir, selectdir, answerdir,
				prefix, posedfile, parsedfile, evaluationfolder);
		
		System.out.println("%%%%%%%%%%%%%%%%%%%RESULTS OF THE UNIT TEST ON FNA");
		compare(precisionrecalltable, "precisionrecall");
		
	}

	public void testOnTreatise(){
		//use the annotation file path below to save the parser output
		String annotation = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\annotation.txt";
		//database setup
		Connection conn = null;
		String username="root";
		String password="root";
		String database="annotationevaluation";
		String gloss = "treatisehglossaryfixed";
		//benchmark
		String outdir ="C:\\DATA\\evaluation\\treatise\\UnsupervisedStanford_Benchmark_sentence";
		String selectdir = "C:\\DATA\\evaluation\\treatise\\UnsupervisedStanford_Benchmark_selected_sentence";
		String answerdir = "C:\\DATA\\evaluation\\treatise\\AnsKey_Benchmark_selected_sentence";
		//intermediate results
		String prefix="treatiseh";
		String posedfile = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\treatiseh\\posedsentences.txt";
		String parsedfile ="C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\treatiseh\\parsedsentences.txt";
		//evaluation
		String evaluationfolder="C:\\DATA\\evaluation\\treatise";	
		String precisionrecalltable="treatise_precisionrecall_final";

		runCharaParser(annotation, conn, username, password, database,
			    gloss, outdir, selectdir, answerdir,
				prefix, posedfile, parsedfile, evaluationfolder);
		
		System.out.println("%%%%%%%%%%%%%%%%%%%RESULTS OF THE UNIT TEST ON TREATISE");
		compare(precisionrecalltable, "precisionrecall");
	}
	
	private void runCharaParser(String annotation, Connection conn,
			String username, String password, String database,
			String gloss, String outdir,
			String selectdir, String answerdir, String prefix,
			String posedfile, String parsedfile, String evaluationfolder) {
		//connect to the database
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		//parsing
		SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, prefix, gloss, true);
		sosm.markSentences();
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, prefix, gloss, true);
		sp.POSTagging();
		sp.parsing();
		sp.extracting();

		//split annontation result into individual xml files		
		SplitAnnotationOutput sao = new SplitAnnotationOutput(annotation, outdir, selectdir, answerdir);
		sao.split();
		sao.select();
		
		CompareXML cXML = new CompareXML(database, evaluationfolder);
	}
	
	private void compare(String benchmark, String result) {
		Connection conn = null;
		String username="root";
		String password="root";
		String database="annotationevaluation";
		ArrayList<Float> benchmarkf= new ArrayList<Float>();
		ArrayList<Float> resultf= new ArrayList<Float>();
		ArrayList<String> labels= new ArrayList<String>();
		labels.add("pperfst"); 
		labels.add("preasonst"); 
		labels.add("pperfch"); 
		labels.add("preasonch"); 
		labels.add("pperfrel"); 
		labels.add("preasonrel"); 
		labels.add("rperfst"); 
		labels.add("rreasonst"); 
		labels.add("rperfch"); 
		labels.add("rreasonch"); 
		labels.add("rperfrel"); 
		labels.add("rreasonrel"); 
		labels.add("sentprecisionperf"); 
		labels.add("sentrecallperf"); 
		labels.add("sentprecisionreas"); 
		labels.add("sentrecallreas");
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select pperfst, preasonst, pperfch, preasonch, pperfrel, preasonrel, rperfst, rreasonst, rperfch, rreasonch, rperfrel, rreasonrel,sentprecisionperf,sentrecallperf, sentprecisionreas, sentrecallreas" +
						" from "+benchmark+" where source='avg'");
				if(rs.next()){
					benchmarkf.add(new Float(rs.getFloat(1)));
					benchmarkf.add(new Float(rs.getFloat(2)));
					benchmarkf.add(new Float(rs.getFloat(3)));
					benchmarkf.add(new Float(rs.getFloat(4)));
					benchmarkf.add(new Float(rs.getFloat(5)));
					benchmarkf.add(new Float(rs.getFloat(6)));
					benchmarkf.add(new Float(rs.getFloat(7)));
					benchmarkf.add(new Float(rs.getFloat(8)));
					benchmarkf.add(new Float(rs.getFloat(9)));
					benchmarkf.add(new Float(rs.getFloat(10)));
					benchmarkf.add(new Float(rs.getFloat(11)));
					benchmarkf.add(new Float(rs.getFloat(12)));
					benchmarkf.add(new Float(rs.getFloat(13)));
					benchmarkf.add(new Float(rs.getFloat(14)));
					benchmarkf.add(new Float(rs.getFloat(15)));
					benchmarkf.add(new Float(rs.getFloat(16)));
				}
				
				rs = stmt.executeQuery("select pperfst, preasonst, pperfch, preasonch, pperfrel, preasonrel, rperfst, rreasonst, rperfch, rreasonch, rperfrel, rreasonrel,sentprecisionperf,sentrecallperf, sentprecisionreas, sentrecallreas" +
						" from "+result+" where source='avg'");
				if(rs.next()){
					resultf.add(new Float(rs.getFloat(1)));
					resultf.add(new Float(rs.getFloat(2)));
					resultf.add(new Float(rs.getFloat(3)));
					resultf.add(new Float(rs.getFloat(4)));
					resultf.add(new Float(rs.getFloat(5)));
					resultf.add(new Float(rs.getFloat(6)));
					resultf.add(new Float(rs.getFloat(7)));
					resultf.add(new Float(rs.getFloat(8)));
					resultf.add(new Float(rs.getFloat(9)));
					resultf.add(new Float(rs.getFloat(10)));
					resultf.add(new Float(rs.getFloat(11)));
					resultf.add(new Float(rs.getFloat(12)));
					resultf.add(new Float(rs.getFloat(13)));
					resultf.add(new Float(rs.getFloat(14)));
					resultf.add(new Float(rs.getFloat(15)));
					resultf.add(new Float(rs.getFloat(16)));
				}
				

				for(int i = 0; i<labels.size(); i++){
					System.out.println(labels.get(i)+":"+benchmarkf.get(i).floatValue()+" vs "+resultf.get(i).floatValue()+" (t-a)=: "+(resultf.get(i).floatValue()-benchmarkf.get(i).floatValue()));
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
		CharaParserUnitTest test = new CharaParserUnitTest();
		//test.testOnFna();
		test.testOnTreatise();
	}

}
