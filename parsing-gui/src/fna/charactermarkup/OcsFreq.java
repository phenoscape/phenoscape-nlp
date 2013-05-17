 /* $Id: OcsFreq.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OcsFreq {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "termsuser";
	static protected String password = "termspassword";

	public OcsFreq(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}
	
	protected void collect(String database){
		OcsFreq.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists ocs_freq_simpleseg (ocid MEDIUMINT NOT NULL,source varchar(100) NOT NULL, organ varchar(30), characters varchar(50), states varchar(1000), freq INT, PRIMARY KEY(ocid))");
				stmt.execute("delete from ocs_freq_simpleseg");
				stmt.execute("create table if not exists ocs_freq_complexseg (ocid MEDIUMINT NOT NULL AUTO_INCREMENT,source varchar(100) NOT NULL, organ1 varchar(50), organ2 varchar(50), relation varchar(1000), freq INT, sourcesent varchar(7000), PRIMARY KEY(ocid))");
				stmt.execute("delete from ocs_freq_complexseg");
				ocs_freq_simpleseg();
				ocs_freq_complexseg();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	protected void ocs_freq_simpleseg(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			String str="";
			String str2="";
			String str3="";
			String str4="";
			ResultSet rs = stmt.executeQuery("select * from marked_simpleseg");
			while(rs.next()){
				int flag=0;
				int flag1=0;
				String str1="";
				str=rs.getString(3);
				Pattern pattern = Pattern.compile("<[a-zA-Z\\_]+|<[\\s]");
            	Matcher matcher = pattern.matcher(str);
            	while ( matcher.find()){
            		int i=matcher.start()+1;
            		int j=matcher.end();
            		if(flag==1){
            			str1=str1.concat("_");
            		}
            		str1=str1.concat(str.subSequence(i,j).toString());
            		flag=1;
            	}
            	matcher.reset();
            	Pattern pattern1 = Pattern.compile("[a-zA-Z\\_]+=\"[\\w±\\+\\–\\-\\.:/\\_;x´X\\×\\s,]+\"");
            	matcher = pattern1.matcher(str);
            	while ( matcher.find()){
            		flag1=1;
            		int i=matcher.start();
            		int j=matcher.end();
            		str2=str.subSequence(i,j).toString();
            		str3=str2.substring(0,str2.indexOf("="));
            		str4=str2.substring(str2.indexOf("\"")+1,str2.lastIndexOf("\""));
            		stmt1.execute("insert into ocs_freq_simpleseg(source,organ,characters,states)values('"+rs.getString(2)+"','"+str1+"','"+str3+"','"+str4+"')");
                }
            	matcher.reset();
            	if(flag1==0)
            		stmt1.execute("insert into ocs_freq_simpleseg(source,organ) values('"+rs.getString(2)+"','"+str1+"')");
            }	
			ocs_freq_simpleseg_count();
		}
		catch (Exception e)
        {
        		System.err.println(e);
        }
	}
	
	protected void ocs_freq_simpleseg_count(){
		try
		{	
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from ocs_freq_simpleseg");
	    	while(rs.next()){
	    		int freq=0;
	    		String org = rs.getString("organ");
	    		String character = rs.getString("characters");
	    		ResultSet rs1 = stmt1.executeQuery("select * from ocs_freq_simpleseg where organ='"+org+"' and characters='"+character+"'");
	    		while(rs1.next()){
	    			freq+=1;
	    		}
	    		stmt2.execute("update ocs_freq_simpleseg set freq='"+freq+"' where ocid='"+rs.getString(1)+"'");
	    	}
		}
		catch (Exception e)
        {
            		System.err.println(e);
        }
	}
	
	protected void ocs_freq_complexseg(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			String str;
			String str2;
			String org1="";
			String org2="";
			String relation;
        	ResultSet rs = stmt.executeQuery("select * from segments");
        	while(rs.next()){
        		int orcount = 0;
        		str=rs.getString(3);
        		Pattern pattern = Pattern.compile("(<[a-zA-Z_ ]+>)");
                // Replace all occurrences of pattern in input
                Matcher matcher = pattern.matcher(str);
                while ( matcher.find()){
                	orcount +=1;
                }
                matcher.reset();
                if(orcount>1){
                	StringBuffer sb2 = new StringBuffer();
                	Pattern pattern1 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern1.matcher(str);
                	while ( matcher.find()){
                		int k=matcher.start()+1;
						int l=matcher.end()-1;
						String org=str.subSequence(k,l).toString();
						if(org.contains("_has_")){
							org1=org.subSequence(0,org.indexOf("_")).toString();
							org2=org.subSequence(org.lastIndexOf("_")+1,org.length()).toString();
							matcher.appendReplacement(sb2, "<"+org1+"> <"+org2+">");
						}
                	}
                	matcher.appendTail(sb2);
					str=sb2.toString();
					matcher.reset();
                	StringBuffer sb1 = new StringBuffer();
					Pattern pattern2 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
					matcher = pattern2.matcher(str);
					while ( matcher.find()){
						int k=matcher.start()+1;
						int l=matcher.end()-1;
						String state=str.subSequence(k,l).toString();
						if(state.contains("_")){
							String firstr=state.subSequence(0,state.indexOf("_")).toString();
							String secstr=state.subSequence(state.indexOf("_")+1,state.length()).toString();
							matcher.appendReplacement(sb1, "{"+firstr+"} or {"+secstr+"}");
						}
					}
					matcher.appendTail(sb1);
					str=sb1.toString();
					matcher.reset();
					orcount = 0;
					
	                // Replace all occurrences of pattern in input
	                matcher = pattern.matcher(str);
	                while ( matcher.find()){
	                	orcount +=1;
	                }
	                matcher.reset();
                	Pattern pattern4 = Pattern.compile("(<[a-zA-Z_ ]+>)[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´×\\s,µ*\\{\\}\\[\\](<\\{)(\\}>) m]+(<[a-zA-Z_ ]+>)");
                	matcher = pattern4.matcher(str);
               		while ( matcher.find()){
               			int i=matcher.start();
                   		int j=matcher.end();
                   		str2=str.subSequence(i,j).toString();
                   		int m=str2.indexOf("<");
                   		int k=str2.indexOf(">");
                   		int l;
                   		int ct=1;
                   		while(ct<orcount){
                   			if(str2.charAt(m+1)!='{')
                   				org1=str2.substring(m+1,k);
                   			l=str2.indexOf("<",k);
                   			if(str2.charAt(l+1)!='{')
                   				org2=str2.substring(l+1,str2.indexOf(">",l));
                   			relation=str2.substring(k+1,l);
                   			m=l;                    			
                   			k=str2.indexOf(">",l);
                    		stmt1.execute("insert into ocs_freq_complexseg(source,organ1,organ2,relation) values('"+rs.getString(2)+"','"+org1+"','"+org2+"','"+relation+"')");
                    		ct++;
                    	}
                    }
                }
                else{
                		System.out.println(str);
                }
            }
        	ocs_freq_complexseg_count();
		}
        catch (Exception e)
        {
            		System.err.println(e);
        }
	}
	
	protected void ocs_freq_complexseg_count(){
		try
		{	
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			String str;
			String org1="";
			String org2="";
			String relation;
			ResultSet rs = stmt.executeQuery("select * from ocs_freq_complexseg");
	    	while(rs.next()){
	    		int freq=0;
	    		String oc = rs.getString(1);
	    		str="";
	    		org1 = rs.getString("organ1");
	    		org2 = rs.getString("organ2");
	    		relation = rs.getString("relation");
	    		ResultSet rs1 = stmt1.executeQuery("select * from ocs_freq_complexseg where organ1='"+org1+"' and organ2='"+org2+"' and relation='"+relation+"'");
	    		while(rs1.next()){
	    			freq+=1;
	    			str=str.concat(rs1.getString(2)+",");
		    	}
	    		stmt2.execute("update ocs_freq_complexseg set freq='"+freq+"' where ocid='"+oc+"'");
    			stmt2.execute("update ocs_freq_complexseg set sourcesent='"+str+"' where ocid='"+oc+"'");
	    	}			
		}
		catch (Exception e)
        {
            		System.err.println(e);
        }
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new OcsFreq("onto_fna_corpus");
	}

}
