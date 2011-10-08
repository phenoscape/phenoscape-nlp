 /* $Id: NewSegmentation.java 790 2011-04-11 17:57:38Z hong1.cui $ */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({ "unused" })
public class NewSegmentation {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "root";

	public NewSegmentation(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}

	protected void collect(String database){
		NewSegmentation.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists newsegments (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent varchar(2000), patternid MEDIUMINT NOT NULL, PRIMARY KEY(sentid))");
				stmt.execute("delete from newsegments");
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
        			String str1 = "";
        			for(int i=0;i<terms.length;i++){
        				int prepflag = 0;
        				// Compile regular expression
        				
                        Pattern pattern = Pattern.compile("((?<![\\w±\\+\\–\\-\\—°.²\\:½/¼\"“”\\_;x´\\×,µ\\*\\{\\}\\[\\](<\\{)(\\}>) m]\\s[“\\s]?)<[a-zA-Z_ ]+>(\\s<[a-zA-Z_ ]+>)*[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ\\*\\{\\}\\[\\](<\\{)(\\}>) m]*)");
                        
                        // Replace all occurrences of pattern in input
                        Matcher matcher = pattern.matcher(terms[i]);
                        if ( matcher.find()){
                        	if(i!=0){
                        		count+=1;
                            	stmt1.execute("insert into newsegments values('"+count+"','"+source+"','"+str1+"',0)");
                            }
                        	str1=terms[i];
                        	Pattern pattern1 = Pattern.compile("(?<!\\})>\\s<(?!\\{)");
                        	Matcher matcher1 = pattern1.matcher(str1);
                        	str1 = matcher1.replaceAll("_has_");
                            matcher1.reset();

                            Pattern pattern3 = Pattern.compile("[\\s]+");
                            matcher1 = pattern3.matcher(str1);
                            str1 = matcher1.replaceAll(" ");
                            matcher1.reset();
                            if(i<terms.length-1)
                            	str1=str1.concat(",");
                        }
                        else{
                        	str1 = str1.concat(terms[i]);
                        	Pattern pattern1 = Pattern.compile("(?<!\\})>\\s<(?!\\{)");
                        	Matcher matcher1 = pattern1.matcher(str1);
                        	str1 = matcher1.replaceAll("_has_");
                            matcher1.reset();
                            
                            Pattern pattern3 = Pattern.compile("[\\s]+");
                            matcher1 = pattern3.matcher(str1);
                            str1 = matcher1.replaceAll(" ");
                            matcher1.reset();
                            if(i<terms.length-1)
                            	str1=str1.concat(",");
                        }
                        matcher.reset();
        			}
        			count+=1;
        			stmt2.execute("insert into newsegments values('"+count+"','"+source+"','"+str1+"',0)");
        	}
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new NewSegmentation("fnav19_benchmark");
	}

}
