 /* $Id: MarkedSentGenerator_notinuse.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkedSentGenerator_notinuse {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "root";
	
	public MarkedSentGenerator_notinuse(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}
	
	protected void collect(String database){
		MarkedSentGenerator_notinuse.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists markedsentence (source varchar(100) NOT NULL, markedsent text, rmarkedsent text, PRIMARY KEY(source))");
				stmt.execute("delete from markedsentence");
				markedsentgen();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}

	protected void markedsentgen(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			String str = "";
        	ResultSet rs = stmt.executeQuery("select * from sentence");
        	while(rs.next()){
        		str = rs.getString(3);
        		if(!str.contains("<Z>")){
	        		Pattern pattern = Pattern.compile("<N>");
	                Matcher matcher = pattern.matcher(str);
	                str = matcher.replaceAll("<");
	                matcher.reset();
	                Pattern pattern1 = Pattern.compile("</N>");
	                matcher = pattern1.matcher(str);
	                str = matcher.replaceAll(">");
	                matcher.reset();
	                StringBuffer sb = new StringBuffer();
	                Pattern pattern2 = Pattern.compile("(<B>)([\\(\\)?\\–\\—°²:=/½\"¼x´\\×\\*µ%“”+\\d±.;,\\[\\]]+)(</B>)");
	                matcher = pattern2.matcher(str);
	                while (matcher.find()){
	           			matcher.appendReplacement(sb, matcher.group(2));
	                }
	                matcher.appendTail(sb);
	        		str=sb.toString();
	                matcher.reset();
	                StringBuffer sb1 = new StringBuffer();
	                Pattern pattern5 = Pattern.compile("(<B>)([\\w]+)(</B>)");
	                matcher = pattern5.matcher(str);
	                while (matcher.find()){
	                	ResultSet rs1=stmt2.executeQuery("Select * from preposition where prep='"+matcher.group(2)+"'");
	             		if(rs1.next())
	             			matcher.appendReplacement(sb1, matcher.group(2));
	                }
	                matcher.appendTail(sb1);
	        		str=sb1.toString();
	                matcher.reset();
	                Pattern pattern3 = Pattern.compile("<M><B>|<B>|<M>");
	                matcher = pattern3.matcher(str);
	                str = matcher.replaceAll("{");
	                matcher.reset();
	                Pattern pattern4 = Pattern.compile("</B></M>|</B>|</M>");
	                matcher = pattern4.matcher(str);
	                str = matcher.replaceAll("}");
	                matcher.reset();
	                
	                int ct = 0;
	                if(rs.getString("tag").contains("[")|rs.getString("tag").compareTo("ditto")==0|rs.getString("tag").compareTo("general")==0){
	                	if(rs.getString("modifier").compareTo("")!=0){
	                		String tag = rs.getString("tag");
	                		String modifier = rs.getString("modifier");
	                		while(tag.compareTo("ditto")==0){
	                			ct++;
	                			rs.previous();
	                			tag = rs.getString("tag");
	                		}
	                		if(tag.compareTo("ignore")!=0){
		                		if(tag.contains("["))
		                			tag = tag.substring(1, tag.length()-1);
		                		if(modifier.contains(" ")){
		            				String [] terms = modifier.split(" ");
		            				modifier = terms[terms.length-1];
		            				if(modifier.contains("["))
		            					modifier = terms[terms.length-2];
		            			}
		                		StringBuffer sb2 = new StringBuffer();
		    	                Pattern pattern6 = Pattern.compile("\\{"+modifier+"\\}");
		    	                matcher = pattern6.matcher(str);
		    	                while (matcher.find()){
		    	           			matcher.appendReplacement(sb2, matcher.group(0)+" <"+tag+">");
		    	                }
		    	                matcher.appendTail(sb2);
		    	        		str=sb2.toString();
		    	                matcher.reset();
	                		}
	                	}
	                	else{
	                		String tag = rs.getString("tag");
	                		while(tag.compareTo("ditto")==0){
	                			ct++;
	                			rs.previous();
	                			tag = rs.getString("tag");
	                		}
	                		if(tag.compareTo("ignore")!=0){
		                		if(tag.contains("["))
		                			tag = tag.substring(1, tag.length()-1);
		                		str = "<"+tag+"> "+str;
	                		}
	                	}
	                }
	                if(ct>0){
	                	for(int i = 0; i < ct; i++)
	                		rs.next();
	                }
	                
	        		stmt1.execute("insert into markedsentence values('"+rs.getString(2)+"','"+str+"')");
        		}
        	}
		}catch (Exception e)
        {
    		System.err.println(e);
        }
	}

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new MarkedSentGenerator_notinuse("benchmark_learningcurve_treatiseh_test_19");
	}

}
