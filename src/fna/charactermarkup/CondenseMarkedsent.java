 /* $Id: CondenseMarkedsent.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CondenseMarkedsent {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "termsuser";
	static protected String password = "termspassword";
	/**
	 * @param args
	 */
	
	public CondenseMarkedsent(String database){
		collect(database);
	}
	
	protected void collect(String database){
		CondenseMarkedsent.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists tmp_result1 (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from tmp_result1");
				replace();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//parse '-' separated states into a single state { }, parse numbers into states { }
	protected void replace(){
		try
		{
			String source;
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			//ResultSet rs = stmt.executeQuery("select * from markedsentence");
			ResultSet rs = stmt.executeQuery("select source, markedsent from markedsentence");
			while (rs.next())
        		{
						source=rs.getString(1);
           				CharSequence inputStr = rs.getString(2);              
           				// Compile regular expression
           				Pattern pattern = Pattern.compile("[\\d]+[\\-\\–]+[\\d]+[\\-\\–]+((<\\{)?|[<{]?)[A-Za-z ]+([}>]?|(\\}>)?)|[\\d]+[\\-\\–]+((<\\{)?|[<{]?)[A-Za-z ]+([}>]?|(\\}>)?)|((<\\{)?|[<{]?)[A-Za-z ]+([}>]?|(\\}>)?)[\\-\\–]+((<\\{)?|[<{]?)[A-Za-z ]+([}>]?|(\\}>)?)");
           				Pattern pattern1 = Pattern.compile("(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+[\\s]?[\\–\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]+[\\s]?[\\–\\-]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d][\\s]?[\\–\\-][\\s]?[\\d]/[\\d])|(?<!(ca[\\s]?|diam[\\s]?))([\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d])");
           				Pattern pattern2 = Pattern.compile("((?<!(/|(\\.[\\s]?)))[\\d]+[\\-\\–]+[\\d]+(?!([\\–\\-]+/|([\\s]?\\.))))|((?<!(\\{|/))[\\d]+(?!(\\}|/)))");
           				Pattern pattern3 = Pattern.compile("\\} or \\{");
                        Pattern pattern4 = Pattern.compile("> or <");
                        Pattern pattern5 = Pattern.compile("> of <");
                        Pattern pattern6 = Pattern.compile("[\\-\\–\\—]+");
                        
                        // Replace all occurrences of pattern in input
                        Matcher matcher = pattern.matcher(inputStr);
                        StringBuffer sb2 = new StringBuffer();
                        while ( matcher.find()){
                        	String str2="";
                    		int i=matcher.start();
                    		int j=matcher.end();
                    		str2=inputStr.subSequence(i,j).toString();
                    		Pattern pattern7 = Pattern.compile("[\\w\\-\\–\\.\\s]+");
                    		Matcher matcher1 = pattern7.matcher(str2);
                    		String str3="";
                    		while(matcher1.find()){
                    			int k=matcher1.start();
                        		int l=matcher1.end();
                        		str3=str3.concat(str2.subSequence(k,l).toString());
                    		}
                    		matcher.appendReplacement(sb2, "{"+str3+"}");
                        }
                        matcher.appendTail(sb2);
                        String output=sb2.toString();
                        matcher.reset();
                        
                        matcher = pattern1.matcher(output);
                        StringBuffer sb = new StringBuffer();
                        while ( matcher.find()){
                        	String str2=" ";
                    		int i=matcher.start();
                    		int j=matcher.end();
                    		str2=output.subSequence(i,j).toString();
                    		matcher.appendReplacement(sb, "{"+str2+"}");
                        }
                        matcher.appendTail(sb);
                        output=sb.toString();
                        matcher.reset();
                        
                        matcher = pattern2.matcher(output);
                        StringBuffer sb1 = new StringBuffer();
                        while ( matcher.find()){
                        	String str2=" ";
                    		int i=matcher.start();
                    		int j=matcher.end();
                    		str2=output.subSequence(i,j).toString();
                    		matcher.appendReplacement(sb1, "{"+str2+"}");
                        }
                        matcher.appendTail(sb1);
                        output=sb1.toString();
                        matcher.reset();
                        
                        matcher = pattern3.matcher(output);
                        output = matcher.replaceAll("_or_");
                        matcher.reset();
                        
                        matcher = pattern4.matcher(output);
                        output = matcher.replaceAll(" or ");
                        matcher.reset();
                        
                        matcher = pattern5.matcher(output);
                        output = matcher.replaceAll("_of_");
                        matcher.reset();
                        
                        matcher = pattern6.matcher(output);
                        output = matcher.replaceAll("-");
                        matcher.reset();
                        stmt1.execute("insert into tmp_result1 values('"+source+"','"+output+"')");
        		}
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//new CondenseMarkedsent("benchmark_learningcurve_fnav19_test_24");
		new CondenseMarkedsent("annotationevaluation_heuristics");
	}

}
