 /* $Id: Segmentation.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Segmentation {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "termsuser";
	static protected String password = "termspassword";

	public Segmentation(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}

	protected void collect(String database){
		Segmentation.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists segments (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent varchar(2000), patternid MEDIUMINT NOT NULL, PRIMARY KEY(sentid))");
				stmt.execute("delete from segments");
				constraintfilter2();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//break into smaller segments
	protected void constraintfilter2(){
		try
		{
			String source;
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			int count=0;
			ResultSet rs = stmt.executeQuery("select * from tmp_result3");
			while(rs.next())
        	{
					source=rs.getString(1);
        			String [] terms = rs.getString(2).split(",");
        			String str = ""; 
        			for(int i=0;i<terms.length;i++){
        				String str1 = "";
        				int prepflag = 0;
        				// Compile regular expression
        				
                        Pattern pattern = Pattern.compile("((?<![\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×,µ\\*\\{\\}\\[\\](<\\{)(\\}>) m]\\s)<[a-zA-Z_ ]+>(\\s<[a-zA-Z_ ]+>)*[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ\\*\\{\\}\\[\\](<\\{)(\\}>) m]*)");
                        
                        // Replace all occurrences of pattern in input
                        Matcher matcher = pattern.matcher(terms[i]);
                        if ( matcher.find()){
                        	str1=terms[i];
                        	Pattern pattern1 = Pattern.compile("(?<!\\})>\\s<(?!\\{)");
                        	Matcher matcher1 = pattern1.matcher(str1);
                        	str1 = matcher1.replaceAll("_has_");
                            matcher1.reset();

                            Pattern pattern3 = Pattern.compile("[\\s]+");
                            matcher1 = pattern3.matcher(str1);
                            str1 = matcher1.replaceAll(" ");
                            matcher1.reset();
                            count+=1;
                            if(i<terms.length-1)
                            	str1=str1.concat(",");
                            stmt1.execute("insert into segments values('"+count+"','"+source+"','"+str1+"',0)");
                        	int sind=str1.indexOf("<");
                           	int eind=str1.indexOf(">");
                       		str=str1.substring(sind,eind+1);
                        }
                        else{
                        	String hide = terms[i];
                        	Pattern pattern2 = Pattern.compile("(<[a-zA-Z_ ]+>)|([<]?\\{[a-zA-Z_\\./\\-\\d\\–{}:]+\\}[>]?)");
                        	Matcher matcher1 = pattern2.matcher(hide);
                        	hide=matcher1.replaceAll("#");
                        	matcher1.reset();
                        	//System.out.println(source+":"+terms[i]);
                        	//System.out.println(source+":"+hide);
                        	Pattern pattern4 = Pattern.compile("[\\w]+");
                        	matcher1 = pattern4.matcher(hide);
                        	while ( matcher1.find()){
                        		int m=matcher1.start();
                            	int n=matcher1.end();
                            	String word = hide.substring(m, n);
                        		ResultSet rs1=stmt2.executeQuery("Select * from preposition where prep='"+word+"'");
                        		while(rs1.next())
                       		 		prepflag = 1;
                        	}
                        	matcher1.reset();
                        	if (prepflag==1)
                   		 		str1=str.concat(terms[i]);
                        	else
                        		str1=terms[i];
                        	Pattern pattern1 = Pattern.compile("(?<!\\})>\\s<(?!\\{)");
                        	matcher1 = pattern1.matcher(str1);
                        	str1 = matcher1.replaceAll("_has_");
                            matcher1.reset();
                            
                            Pattern pattern3 = Pattern.compile("[\\s]+");
                            matcher1 = pattern3.matcher(str1);
                            str1 = matcher1.replaceAll(" ");
                            matcher1.reset();
                            count+=1;
                            if(i<terms.length-1)
                            	str1=str1.concat(",");
                            stmt1.execute("insert into segments values('"+count+"','"+source+"','"+str1+"',0)");
                        	if(i==0){
                        		int sind=str1.indexOf("<");
                            	int eind=str1.indexOf(">");
                            	if(sind<0)
                            		str=str.concat("<org>");
                            	else
                            		str=str1.substring(sind,eind+1);
                        	}
                        }
                        matcher.reset();
        			}
        	}
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//new Segmentation("benchmark_learningcurve_fnav19_test_24");
		new Segmentation("annotationevaluation_heuristics");
	}

}
