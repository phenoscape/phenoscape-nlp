 /* $Id: HideCommas.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({  "unused" })
public class HideCommas {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "termsuser";
	static protected String password = "termspassword";
	
	public HideCommas(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}

	protected void collect(String database){
		HideCommas.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists tmp_result2 (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from tmp_result2");
				stmt.execute("create table if not exists exception (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from exception");
				stmt.execute("create table if not exists tmp_result3 (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from tmp_result3");
				filter();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//Hide the commas between prep and organ as {:} and pass it on to next function
	//if no organ after prep then store in exception table and dont pass it on to next function
	protected void filter(){
		try
		{
			String source;
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			Statement stmt3 = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from tmp_result1");
			while (rs.next())
        	{
					source=rs.getString(1);
					CharSequence inputStr = rs.getString(2);              
        			Pattern pattern1 = Pattern.compile("[\\s][a-zA-Z]+");    		
        			Matcher matcher = pattern1.matcher(inputStr);
        			int flag1=0;
        			while(matcher.find()){
        				String str=" ";
        				int i=matcher.start()+1;
                    	int j=matcher.end();
                   		str=inputStr.subSequence(i,j).toString();
                   		ResultSet rs1=stmt3.executeQuery("Select * from preposition where prep='"+str+"'");
               		 	int streq=0;
               		 	while (rs1.next()){
               		 		streq=1;
                		}
                   		if(streq==1){
                   			Pattern pattern2 = Pattern.compile("(<[a-zA-Z_ ]+>)");
                   			Matcher matcher2 = pattern2.matcher(inputStr);
                   			//System.out.println(inputStr);
                   			if(inputStr.toString().indexOf(">",j)>0)
                   				matcher2=matcher2.region(j,inputStr.toString().indexOf(">",j)+1);
                   			else
                   				matcher2=matcher2.region(j,inputStr.toString().length());
                   			/*else if(inputStr.toString().indexOf(" .",j)>0)
                   				matcher2=matcher2.region(j,inputStr.toString().indexOf(" .",j));
                   			else if(inputStr.toString().indexOf(";",j)>0)
                   				matcher2=matcher2.region(j,inputStr.toString().indexOf(";",j));
                   			else
                   				matcher2=matcher2.region(j,inputStr.toString().indexOf(":",j));*/
                   			if(matcher2.find()){
                   				int flag=0;
                   				Pattern pattern3 = Pattern.compile("(?<![{]),(?![}])");
                   				Matcher matcher3 = pattern3.matcher(inputStr);
                   				matcher3=matcher3.region(j,matcher2.start());
                                StringBuffer sb = new StringBuffer();
                                while (matcher3.find()){
                                  	//System.out.println("inside2");
                                	int k=matcher3.start();
                                	int l=matcher3.end();
                                	String str2=inputStr.subSequence(k,l).toString();
                                	char comma=':';
                                	matcher3.appendReplacement(sb, "{"+comma+"}");
                                	flag=1;
                                }
                                matcher3.appendTail(sb);
                                inputStr=sb.toString();
                                if (flag==1){
                                    matcher.reset();
                                    matcher = pattern1.matcher(inputStr);
                                    matcher=matcher.region(matcher2.end(),inputStr.toString().length());
                                    /*if(inputStr.toString().indexOf(" .",matcher2.end())>0)
                                    	matcher=matcher.region(matcher2.end(),inputStr.toString().indexOf(" .",matcher2.end()));
                                    else if(inputStr.toString().indexOf(";",matcher2.end())>0)
                                    	matcher=matcher.region(matcher2.end(),inputStr.toString().indexOf(";",matcher2.end()));
                                    else
                                    	matcher=matcher.region(matcher2.end(),inputStr.toString().indexOf(":",matcher2.end()));*/
                                }
                                matcher3.reset();
                   			}
                   			else{
                   				//Store in exception table and don't pass it to next step
                   				if(flag1==0){
                   					stmt1.execute("insert into exception values('"+source+"','"+inputStr+"')");
                   					stmt1.execute("insert into tmp_result2 values('"+source+"','"+inputStr+"')");
                   				}
                   				flag1=1;
                   			}
                   			matcher2.reset();
                   		}
                    }
                    matcher.reset();
                    //Store it in table for next function
                    if(flag1==0){
                    	stmt2.execute("insert into tmp_result2 values('"+source+"','"+inputStr+"')");
                    }
        	}
        	constraintfilter();
		}
        catch (Exception e)
        {
        	e.printStackTrace();
        }
	}
	
	//merge segments with only states in them with the previous segment that has organ
	protected void constraintfilter(){
		try
		{
			String source;
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from tmp_result2");
			while(rs.next())
        	{
						source=rs.getString(1);
						String [] terms = rs.getString(2).split(",");
        				String str = ""; 
        				for(int i=0;i<terms.length;i++){
        				CharSequence inputStr = terms[i];              
           				// Compile regular expression
        				
                        Pattern pattern = Pattern.compile("((\\s\\{[a-zA-Z_\\./\\-\\d\\s\\本}]+\\}|\\s[\\w]+)\\s<[a-zA-Z_ ]+>)|(<[a-zA-Z_ ]+>\\s((\\{[a-zA-Z_\\./\\-\\d\\s\\本}]+\\})*|[\\w]*))|((\\{[a-zA-Z_\\./\\-\\d\\s\\本}]+\\}|[\\w]+)\\s<[a-zA-Z_ ]+>\\s(\\{[a-zA-Z_\\./\\-\\d\\s\\本}]+\\}|[\\w]+))");
                        	
                        // Replace all occurrences of pattern in input
                        Matcher matcher = pattern.matcher(inputStr);
                        if ( matcher.find()){
                        	if (i!=0&&i<terms.length){
                        		str=str.concat(",");
                        	}
                        	str=str.concat(terms[i]); 
                        }
                        else{
                        	if (i!=0&&i<terms.length){
                        		str=str.concat("{:}");
                        	}
                        	str=str.concat(terms[i]);
                        }
                        matcher.reset();
        				}
        				stmt1.execute("insert into tmp_result3 values('"+source+"','"+str+"')");
        				
        	}
		}
        catch (Exception e)
        {
        	e.printStackTrace();
        }
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//new HideCommas("fnav19_benchmark");
		new HideCommas("annotationevaluation_heuristics");
	}

}
