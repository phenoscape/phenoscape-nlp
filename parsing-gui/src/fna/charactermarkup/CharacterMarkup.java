 /* $Id: CharacterMarkup.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({ "unused" })
public class CharacterMarkup {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "";
	
	
	public CharacterMarkup(String database){
		collect(database);
	}
	
	protected void collect(String database){
		CharacterMarkup.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
			/*	stmt.execute("create table if not exists tmp_result1 (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from tmp_result1");
				stmt.execute("create table if not exists tmp_result2 (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from tmp_result2");
				stmt.execute("create table if not exists exception (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from exception");
				stmt.execute("create table if not exists tmp_result3 (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from tmp_result3");
				stmt.execute("create table if not exists wordnet (source varchar(100) NOT NULL, markedsent varchar(2000), PRIMARY KEY(source))");
				stmt.execute("delete from wordnet");
				stmt.execute("create table if not exists segments (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent varchar(2000), patternid MEDIUMINT NOT NULL, PRIMARY KEY(sentid))");
				stmt.execute("delete from segments");
				stmt.execute("create table if not exists patterns (sentid MEDIUMINT NOT NULL, pattern varchar(2000), PRIMARY KEY(sentid))");
				stmt.execute("delete from patterns");
				stmt.execute("create table if not exists patterns_freq (patternid MEDIUMINT NOT NULL, pattern varchar(2000), freq INT, PRIMARY KEY(patternid))");
				stmt.execute("delete from patterns_freq");
				stmt.execute("create table if not exists simpleseg_modifier (modifier varchar(2000), semanticrole varchar(2), simplesegment varchar(2000))");
				stmt.execute("delete from simpleseg_modifier");
				stmt.execute("create table if not exists complexseg_modifier (modifier varchar(2000), semanticrole varchar(2), complexsegment varchar(2000))");
				stmt.execute("delete from complexseg_modifier");
				stmt.execute("create table if not exists marked_simpleseg (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent varchar(6000), PRIMARY KEY(sentid))");
				stmt.execute("delete from marked_simpleseg");
				stmt.execute("create table if not exists marked_complexseg (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent varchar(8000), PRIMARY KEY(sentid))");
				stmt.execute("delete from marked_complexseg");
				stmt.execute("create table if not exists ocs_freq_simpleseg (ocid MEDIUMINT NOT NULL,source varchar(100) NOT NULL, organ varchar(30), characters varchar(50), states varchar(1000), freq INT, PRIMARY KEY(ocid))");
				stmt.execute("delete from ocs_freq_simpleseg");
				stmt.execute("create table if not exists ocs_freq_complexseg (ocid MEDIUMINT NOT NULL AUTO_INCREMENT,source varchar(100) NOT NULL, organ1 varchar(50), organ2 varchar(50), relation varchar(1000), freq INT, sourcesent varchar(7000), PRIMARY KEY(ocid))");
				stmt.execute("delete from ocs_freq_complexseg");*/
				//replace();
				//parse_simpleseg();
				//ocs_freq_complexseg_count();
				clausecount();
			}
		}
		catch(Exception e){
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
           				Pattern pattern = Pattern.compile("[\\d]+[\\-\\–]+[\\d]+[\\-\\–]+((<\\{)?|[<{]?)[A-Za-z]+([}>]?|(\\}>)?)|[\\d]+[\\-\\–]+((<\\{)?|[<{]?)[A-Za-z]+([}>]?|(\\}>)?)|((<\\{)?|[<{]?)[A-Za-z]+([}>]?|(\\}>)?)[\\-\\–]+((<\\{)?|[<{]?)[A-Za-z]+([}>]?|(\\}>)?)");
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
                        output = matcher.replaceAll("_");
                        matcher.reset();
                        
                        matcher = pattern4.matcher(output);
                        output = matcher.replaceAll("_or_");
                        matcher.reset();
                        
                        matcher = pattern5.matcher(output);
                        output = matcher.replaceAll("_of_");
                        matcher.reset();
                        
                        matcher = pattern6.matcher(output);
                        output = matcher.replaceAll("-");
                        matcher.reset();
                        stmt1.execute("insert into tmp_result1 values('"+source+"','"+output+"')");
        		}
				filter();
		}
        catch (Exception e)
        {
        		System.err.println(e);
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
                   			Pattern pattern2 = Pattern.compile("(<[a-zA-Z_]+>)");
                   			Matcher matcher2 = pattern2.matcher(inputStr);
                   			//System.out.println(inputStr);
                   			if(inputStr.toString().indexOf(">",j)>0)
                   				matcher2=matcher2.region(j,inputStr.toString().indexOf(">",j)+1);
                   			else if(inputStr.toString().indexOf(" .",j)>0)
                   				matcher2=matcher2.region(j,inputStr.toString().indexOf(" .",j));
                   			else if(inputStr.toString().indexOf(";",j)>0)
                   				matcher2=matcher2.region(j,inputStr.toString().indexOf(";",j));
                   			else
                   				matcher2=matcher2.region(j,inputStr.toString().indexOf(":",j));
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
                                	System.out.println(inputStr);
                                    matcher.reset();
                                    matcher = pattern1.matcher(inputStr);
                                    if(inputStr.toString().indexOf(" .",matcher2.end())>0)
                                    	matcher=matcher.region(matcher2.end(),inputStr.toString().indexOf(" .",matcher2.end()));
                                    else if(inputStr.toString().indexOf(";",matcher2.end())>0)
                                    	matcher=matcher.region(matcher2.end(),inputStr.toString().indexOf(";",matcher2.end()));
                                    else
                                    	matcher=matcher.region(matcher2.end(),inputStr.toString().indexOf(":",matcher2.end()));
                                }
                                matcher3.reset();
                   			}
                   			else{
                   				//Store in exception table and don't pass it to next function
                   				if(flag1==0){
                   					stmt1.execute("insert into exception values('"+source+"','"+inputStr+"')");
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
        		System.err.println(e);
        }
	}
	
	//merge segments with only states in them with the previous organ
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
        				
                        Pattern pattern = Pattern.compile("((\\s\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\}|\\s[\\w]+)\\s<[a-zA-Z_]+>)|(<[a-zA-Z_]+>\\s((\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\})*|[\\w]*))|((\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\}|[\\w]+)\\s<[a-zA-Z_]+>\\s(\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\}|[\\w]+))");
                        	
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
        	constraintfilter2();
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}
	
	//break into smaller segments
	protected void constraintfilter2(){
		try
		{
			String source;
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			int count=0;
			ResultSet rs = stmt.executeQuery("select * from tmp_result3");
			while(rs.next())
        	{
					source=rs.getString(1);
        			String [] terms = rs.getString(2).split(",");
        			String str = ""; 
        			for(int i=0;i<terms.length;i++){
        				CharSequence inputStr = terms[i];              
           				// Compile regular expression
        				
                        Pattern pattern = Pattern.compile("(([\\w]+\\s(\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\}\\s)*)<[a-zA-Z_]+>((\\s\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\})*\\s[\\w]+))|(([\\w]+\\s(\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\}\\s)*)<[a-zA-Z_]+>)");
                        
                        // Replace all occurrences of pattern in input
                        Matcher matcher = pattern.matcher(inputStr);
                        if ( matcher.find()){
                        	String str1=str.concat(terms[i]);
                            Pattern pattern3 = Pattern.compile("[\\s]+");
                            matcher = pattern3.matcher(str1);
                            str1 = matcher.replaceAll(" ");
                            matcher.reset();
                            count+=1;
                            stmt1.execute("insert into segments values('"+count+"','"+source+"','"+str1+"',0)");
                        	if(i==0){
                        		int sind=terms[i].indexOf("<");
                            	int eind=terms[i].indexOf(">");
                            	str=terms[i].substring(sind,eind+1);
                        	}
                        }
                        else{
                        	String str1=str.concat(terms[i]);
                        	Pattern pattern1 = Pattern.compile("(?<!\\})>\\s<(?!\\{)");
                        	Matcher matcher1 = pattern1.matcher(str1);
                        	str1 = matcher1.replaceAll("_has_");
                            matcher1.reset();

                            Pattern pattern3 = Pattern.compile("[\\s]+");
                            matcher = pattern3.matcher(str1);
                            str1 = matcher.replaceAll(" ");
                            matcher.reset();
                            count+=1;
                        	//out.println(str1);
                            stmt1.execute("insert into segments values('"+count+"','"+source+"','"+str1+"',0)");
                        	if(i==0){
                        		int sind=terms[i].indexOf("<");
                            	int eind=terms[i].indexOf(">");
                            	if(sind<0)
                            		str=str.concat("< >");
                            	else
                            	str=terms[i].substring(sind,eind+1);
                        	}
                        }
                        matcher.reset();
        			}
        	}
        	parse_simpleseg();
        	//////////////////////////////nametag();
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}
	
	protected void parse_simpleseg(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			String str;
        	ResultSet rs = stmt.executeQuery("select * from segments");
        	while(rs.next()){
        		int orcount=0;
        		str=rs.getString(3);
        		Pattern pattern = Pattern.compile("(<[a-zA-Z_ ]+>)");
                
                // Replace all occurrences of pattern in input
                Matcher matcher = pattern.matcher(str);
                while ( matcher.find()){
                	orcount +=1;
                }
                matcher.reset();
                if(orcount==1|orcount==0){
                	StringBuffer sb2 = new StringBuffer();
                	Pattern pattern12 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern12.matcher(str);
                	while ( matcher.find()){
                		int k=matcher.start()+1;
						int l=matcher.end()-1;
						String org=str.subSequence(k,l).toString();
						if(org.contains("_has_")){
							String org1=org.subSequence(0,org.indexOf("_")).toString();
							String org2=org.subSequence(org.lastIndexOf("_")+1,org.length()).toString();
							matcher.appendReplacement(sb2, "<"+org1+"> <"+org2+">");
						}
                	}
                	matcher.appendTail(sb2);
					str=sb2.toString();
					matcher.reset();
                	StringBuffer sb1 = new StringBuffer();
					Pattern pattern11 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
					matcher = pattern11.matcher(str);
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
                	String str1="";
                	Pattern pattern1 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´×µ*“”]+");
                	matcher = pattern1.matcher(str);
                	while ( matcher.find()){
                		int i=matcher.start();
                		int j=matcher.end();
                		str1=str1.concat(str.subSequence(i,j).toString());
                	}
                	matcher.reset();
                	String str6=str1;
                	String str2="";
                	String str4="";
                	int flag6 = 0;
                	int ct = 0;
                	Pattern pattern2 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern2.matcher(str);
                	while ( matcher.find()){//if ( matcher.find()){
                		ct++;
                		int i=matcher.start();
                		int j=matcher.end();
                			if(!str.subSequence(i,j).toString().contains("<n>")){
                				if(ct>1){
                					str2=str2.concat(">"+str.subSequence(i,j-1).toString());
                					str4="</"+str.subSequence(i+1,j).toString()+str4;
                				}
                				else{
                					str2=str2.concat(str.subSequence(i,j-1).toString());
                					str4="</"+str.subSequence(i+1,j).toString();
                				}
                				Pattern pattern7 = Pattern.compile("[\\d][\\s]?n[\\s]?=[\\s]?[\\d]+([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\d]+)*");
                				Matcher matcher1 = pattern7.matcher(str1);
                				if ( matcher1.find()){
                					i=matcher1.start();
                					j=matcher1.end();
                					str2=str2.concat(" "+"chromosome_count=\""+str1.subSequence(i,j).toString()+"\"");
                				}
                				str1 = matcher1.replaceAll("#");
                				matcher1.reset();
                			}
                			else{
                				if(ct>1){
                					str2=str2.concat("><chromosome_count");
                					str4="</chromosome_count>"+str4;
                				}
                				else{
                				str2=str2.concat("<chromosome_count");
                				str4="</chromosome_count>";
                				}
                				str1="";
                			}
                		flag6=1;
                	}
                	if(flag6 == 0){
                		Pattern pattern8 = Pattern.compile("[\\d][\\s]?n[\\s]?=[\\s]?[\\d]+([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\d]+)*");
                    	Matcher matcher1 = pattern8.matcher(str1);
                    	if ( matcher1.find()){
                    		str2=str2.concat("<chromosome_count");
                    		str4="</chromosome_count>";
                    		str1 = matcher1.replaceAll("#");
                    	}
                    	else{
                		str2=str2.concat("< ");
                		str4="</ >";
                    	}
                	}
                	matcher.reset();
                	Pattern pattern4 = Pattern.compile("[x×±\\d\\–\\-\\.\\s]+[\\s]?[cmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
                	matcher = pattern4.matcher(str1);
                	int flag=0;
                	while ( matcher.find()){
                		int i;
                		if(str1.charAt(matcher.start())==' '){
                			i=matcher.start()+1;
                		}
                		else{
                			i=matcher.start();
                		}
                		int j=matcher.end();
                		if(flag==0)
                			str2=str2.concat(" "+"size=\""+str1.subSequence(i,j).toString());
                		else
                			str2=str2.concat(","+str1.subSequence(i,j).toString());
                		flag=1;
                	}
                	if(flag==1)
                		str2=str2.concat("\"");
                	str1 = matcher.replaceAll("#");
                	matcher.reset();
                	Pattern pattern5 = Pattern.compile("[±\\d\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
                	matcher = pattern5.matcher(str1);
                	int flag1=0;
                	while ( matcher.find()){
                		int i;
                		if(str1.charAt(matcher.start())==' '){
                			i=matcher.start()+1;
                		}
                		else{
                			i=matcher.start();
                		}
                		int j=matcher.end();
                		if(flag1==0)
                			str2=str2.concat(" "+"size=\""+str1.subSequence(i,j).toString());
                		else
                			str2=str2.concat(","+str1.subSequence(i,j).toString());
                		flag1=1;
                	}
                	if(flag1==1)
                		str2=str2.concat("\"");
                	str1 = matcher.replaceAll("#");
                	matcher.reset();
                	Pattern pattern10 = Pattern.compile("([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)[\\–\\–\\-]");
                	matcher = pattern10.matcher(str1);
                	str1 = matcher.replaceAll("#");
                	matcher.reset();     	
                	Pattern pattern6 = Pattern.compile("(?<!([\\d]?n[\\s]?=[\\s]?|[/]))([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)(?!([\\–\\–\\-]|[n/]|[\\–\\–\\-][\\d]|[\\s]?[\\.]|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[cm]?m))");
                	matcher = pattern6.matcher(str1);
                	int flag2=0;
                	while ( matcher.find()){
                		int i=matcher.start();
                		int j=matcher.end();
                		if(flag2==0)
                			str2=str2.concat(" "+"count=\""+str1.subSequence(i,j).toString());
                		else
                			str2=str2.concat(","+str1.subSequence(i,j).toString());
                		flag2=1;
                	}
                	if(flag2==1)
                		str2=str2.concat("\"");
                	matcher.reset();
                	String str3="";
                	Pattern pattern3 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
                	matcher = pattern3.matcher(str);
                	int flag3=0;
                	while ( matcher.find()){
                		int flag5=0;
                		String first = "";
                		int i=matcher.start()+1;
                		int j=matcher.end()-1;
                		str3=str.subSequence(i,j).toString();
                		if(str3.contains("-")|str3.contains("–")){
                			first = str3.substring(0, str3.indexOf("-"));
                			str3=str3.substring(str3.indexOf("-")+1|str3.indexOf("–")+1, str3.length());
                			flag5=1;
                		}
                		if(flag3==0){
                			ResultSet rs1 = stmt1.executeQuery("select * from character_type where word='"+str3+"'");
                			if(rs1.next()){
                				if(flag5==1)
                					str2=str2.concat(" "+rs1.getString(2)+"=\""+first+"-"+str3+"\"");
                				else
                					str2=str2.concat(" "+rs1.getString(2)+"=\""+str3+"\"");
                				flag3=1;
                			}
                		}
                		else{
                			ResultSet rs1 = stmt1.executeQuery("select * from character_type where word='"+str3+"'");
                			if(rs1.next()){
                				int flag4=0;
                				StringBuffer sb = new StringBuffer();
            					Pattern pattern9 = Pattern.compile(rs1.getString(2)+"=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,]+\"");
            					Matcher matcher1 = pattern9.matcher(str2);
            					while ( matcher1.find()){
            						int k=matcher1.start();
            						int l=matcher1.end();
            						if(flag5==1)
            							matcher1.appendReplacement(sb, str2.subSequence(k,str2.lastIndexOf("\""))+","+first+"-"+str3+"\"");
                    				else
                    					matcher1.appendReplacement(sb, str2.subSequence(k,str2.lastIndexOf("\""))+","+str3+"\"");
            						flag4=1;
            					}
            					if(flag4==1){
            						matcher1.appendTail(sb);
            						str2=sb.toString();
            					}
            					else{
            						if(flag5==1)
            							str2=str2.concat(" "+rs1.getString(2)+"=\""+first+"-"+str3+"\"");
            						else
            							str2=str2.concat(" "+rs1.getString(2)+"=\""+str3+"\"");
            					}
            					matcher1.reset();
                			}                			
                		}
                		
                	}
                	Pattern pattern13 = Pattern.compile("_");
                    // Replace all occurrences of pattern in input
                    Matcher matcher2 = pattern13.matcher(str6);
                    str6 = matcher2.replaceAll(" ");
                	str2=str2.concat(">"+str6+str4);
                	stmt2.execute("insert into marked_simpleseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str2+"')");
                	matcher.reset();
                }
                else{
                	//stmt1.execute("insert into marked_comlexseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str2+"')");
                }
            }
        	
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}
		
	protected void parse_complexseg(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			String str;
        	ResultSet rs = stmt.executeQuery("select * from segments");
        	while(rs.next()){
        		int orcount=0;
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
                	Pattern pattern12 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern12.matcher(str);
                	while ( matcher.find()){
                		int k=matcher.start()+1;
						int l=matcher.end()-1;
						String org=str.subSequence(k,l).toString();
						if(org.contains("_has_")){
							String org1=org.subSequence(0,org.indexOf("_")).toString();
							String org2=org.subSequence(org.lastIndexOf("_")+1,org.length()).toString();
							matcher.appendReplacement(sb2, "<"+org1+"> <"+org2+">");
						}
                	}
                	matcher.appendTail(sb2);
					str=sb2.toString();
					matcher.reset();
                	StringBuffer sb1 = new StringBuffer();
					Pattern pattern11 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
					matcher = pattern11.matcher(str);
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
                	String str1="";
                	Pattern pattern1 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´×µ*“”]+");
                	matcher = pattern1.matcher(str);
                	while ( matcher.find()){
                		int i=matcher.start();
                		int j=matcher.end();
                		str1=str1.concat(str.subSequence(i,j).toString());
                	}
                	matcher.reset();
                	String str6=str1;
                	String str2="";
                	String str4="";
                	int flag6 = 0;
                	int ct = 0;
                	Pattern pattern2 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern2.matcher(str);
                	while ( matcher.find()){//if ( matcher.find()){
                		ct++;
                		int i=matcher.start();
                		int j=matcher.end();
                			if(!str.subSequence(i,j).toString().contains("<n>")){
                				if(ct>1){
                					str2=str2.concat(">"+str.subSequence(i,j-1).toString());
                					str4="</"+str.subSequence(i+1,j).toString()+str4;
                				}
                				else{
                					str2=str2.concat(str.subSequence(i,j-1).toString());
                					str4="</"+str.subSequence(i+1,j).toString();
                				}
                				Pattern pattern7 = Pattern.compile("[\\d][\\s]?n[\\s]?=[\\s]?[\\d]+([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\d]+)*");
                				Matcher matcher1 = pattern7.matcher(str1);
                				if ( matcher1.find()){
                					i=matcher1.start();
                					j=matcher1.end();
                					str2=str2.concat(" "+"chromosome_count=\""+str1.subSequence(i,j).toString()+"\"");
                				}
                				str1 = matcher1.replaceAll("#");
                				matcher1.reset();
                			}
                			else{
                				if(ct>1){
                					str2=str2.concat("><chromosome_count");
                					str4="</chromosome_count>"+str4;
                				}
                				else{
                				str2=str2.concat("<chromosome_count");
                				str4="</chromosome_count>";
                				}
                				str1="";
                			}
                		flag6=1;
                	}
                	if(flag6 == 0){
                		Pattern pattern8 = Pattern.compile("[\\d][\\s]?n[\\s]?=[\\s]?[\\d]+([\\s]?[\\–\\-\\—]?[:]?[\\s]?[\\d]+)*");
                    	Matcher matcher1 = pattern8.matcher(str1);
                    	if ( matcher1.find()){
                    		str2=str2.concat("<chromosome_count");
                    		str4="</chromosome_count>";
                    		str1 = matcher1.replaceAll("#");
                    	}
                    	else{
                		str2=str2.concat("< ");
                		str4="</ >";
                    	}
                	}
                	matcher.reset();
                	Pattern pattern4 = Pattern.compile("[x×±\\d\\–\\-\\.\\s]+[\\s]?[cmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
                	matcher = pattern4.matcher(str1);
                	int flag=0;
                	while ( matcher.find()){
                		int i;
                		if(str1.charAt(matcher.start())==' '){
                			i=matcher.start()+1;
                		}
                		else{
                			i=matcher.start();
                		}
                		int j=matcher.end();
                		if(flag==0)
                			str2=str2.concat(" "+"size=\""+str1.subSequence(i,j).toString());
                		else
                			str2=str2.concat(","+str1.subSequence(i,j).toString());
                		flag=1;
                	}
                	if(flag==1)
                		str2=str2.concat("\"");
                	str1 = matcher.replaceAll("#");
                	matcher.reset();
                	Pattern pattern5 = Pattern.compile("[±\\d\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
                	matcher = pattern5.matcher(str1);
                	int flag1=0;
                	while ( matcher.find()){
                		int i;
                		if(str1.charAt(matcher.start())==' '){
                			i=matcher.start()+1;
                		}
                		else{
                			i=matcher.start();
                		}
                		int j=matcher.end();
                		if(flag1==0)
                			str2=str2.concat(" "+"size=\""+str1.subSequence(i,j).toString());
                		else
                			str2=str2.concat(","+str1.subSequence(i,j).toString());
                		flag1=1;
                	}
                	if(flag1==1)
                		str2=str2.concat("\"");
                	str1 = matcher.replaceAll("#");
                	matcher.reset();
                	Pattern pattern10 = Pattern.compile("([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)[\\–\\–\\-]");
                	matcher = pattern10.matcher(str1);
                	str1 = matcher.replaceAll("#");
                	matcher.reset();     	
                	Pattern pattern6 = Pattern.compile("(?<!([\\d]?n[\\s]?=[\\s]?|[/]))([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)(?!([\\–\\–\\-]|[n/]|[\\–\\–\\-][\\d]|[\\s]?[\\.]|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[cm]?m))");
                	matcher = pattern6.matcher(str1);
                	int flag2=0;
                	while ( matcher.find()){
                		int i=matcher.start();
                		int j=matcher.end();
                		if(flag2==0)
                			str2=str2.concat(" "+"count=\""+str1.subSequence(i,j).toString());
                		else
                			str2=str2.concat(","+str1.subSequence(i,j).toString());
                		flag2=1;
                	}
                	if(flag2==1)
                		str2=str2.concat("\"");
                	matcher.reset();
                	String str3="";
                	Pattern pattern3 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
                	matcher = pattern3.matcher(str);
                	int flag3=0;
                	while ( matcher.find()){
                		int flag5=0;
                		String first = "";
                		int i=matcher.start()+1;
                		int j=matcher.end()-1;
                		str3=str.subSequence(i,j).toString();
                		if(str3.contains("-")|str3.contains("–")){
                			first = str3.substring(0, str3.indexOf("-"));
                			str3=str3.substring(str3.indexOf("-")+1|str3.indexOf("–")+1, str3.length());
                			flag5=1;
                		}
                		if(flag3==0){
                			ResultSet rs1 = stmt1.executeQuery("select * from character_type where word='"+str3+"'");
                			if(rs1.next()){
                				if(flag5==1)
                					str2=str2.concat(" "+rs1.getString(2)+"=\""+first+"-"+str3+"\"");
                				else
                					str2=str2.concat(" "+rs1.getString(2)+"=\""+str3+"\"");
                				flag3=1;
                			}
                		}
                		else{
                			ResultSet rs1 = stmt1.executeQuery("select * from character_type where word='"+str3+"'");
                			if(rs1.next()){
                				int flag4=0;
                				StringBuffer sb = new StringBuffer();
            					Pattern pattern9 = Pattern.compile(rs1.getString(2)+"=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,]+\"");
            					Matcher matcher1 = pattern9.matcher(str2);
            					while ( matcher1.find()){
            						int k=matcher1.start();
            						int l=matcher1.end();
            						if(flag5==1)
            							matcher1.appendReplacement(sb, str2.subSequence(k,str2.lastIndexOf("\""))+","+first+"-"+str3+"\"");
                    				else
                    					matcher1.appendReplacement(sb, str2.subSequence(k,str2.lastIndexOf("\""))+","+str3+"\"");
            						flag4=1;
            					}
            					if(flag4==1){
            						matcher1.appendTail(sb);
            						str2=sb.toString();
            					}
            					else{
            						if(flag5==1)
            							str2=str2.concat(" "+rs1.getString(2)+"=\""+first+"-"+str3+"\"");
            						else
            							str2=str2.concat(" "+rs1.getString(2)+"=\""+str3+"\"");
            					}
            					matcher1.reset();
                			}                			
                		}
                		
                	}
                   	Pattern pattern13 = Pattern.compile("_");
                    // Replace all occurrences of pattern in input
                    Matcher matcher2 = pattern13.matcher(str6);
                    str6 = matcher2.replaceAll(" ");
                	str2=str2.concat(">"+str6+str4);
                	stmt2.execute("insert into marked_simpleseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str2+"')");
                	matcher.reset();
                }
                else{
                	//stmt1.execute("insert into marked_comlexseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+str2+"')");
                }
            }
			
		}catch (Exception e)
        {
    		System.err.println(e);
        }
	}
	
	protected void wordnet(){
		Runtime r = Runtime.getRuntime();
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			int flag=0;
			ResultSet rs = stmt.executeQuery("select * from tmp_result3");
			while(rs.next()){
				String str=rs.getString(2);
				Pattern pattern = Pattern.compile("(<[a-zA-Z_ ]+>)|((?<![<])\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\}(?![>]))");
				Matcher matcher = pattern.matcher(str);
            	String str1=matcher.replaceAll("#");
            	matcher.reset();
            	
            	Pattern pattern1 = Pattern.compile("[a-zA-Z]+");
            	matcher = pattern1.matcher(str1);
            	while ( matcher.find()){
            		int i=matcher.start();
            		int j=matcher.end();
            		ResultSet rs1=stmt2.executeQuery("Select * from wordnet_ref where ='"+str1.subSequence(i,j).toString()+"'");
    				while(rs1.next()){
    					flag=1;
    				}
    				if(flag==0){
            		Process p = r.exec("cmd /c wn "+str1.subSequence(i,j)+" -over"); 
        			InputStream in = p.getInputStream();
        			BufferedInputStream buf = new BufferedInputStream(in);
        			InputStreamReader inread = new InputStreamReader(buf);
        			BufferedReader bufferedreader = new BufferedReader(inread);
        			String line="";
        			while ((bufferedreader.readLine())!=null) {
        			      line=line.concat(bufferedreader.readLine()+" ");
        			}
        			System.out.println(line);
        			if(line.contains("Overview of noun") && !line.contains("Overview of verb") && !line.contains("Overview of adj") && !line.contains("Overview of adv")){
        					StringBuffer sb = new StringBuffer();
        					Pattern pattern2 = Pattern.compile("[\\s]"+str1.subSequence(i,j).toString()+"([\\s]|;|.|:|,)");
        					Matcher matcher1 = pattern2.matcher(str);
        					while ( matcher1.find()){
        						int k=matcher1.start()+1;
        						int l=matcher1.end()-1;
        						matcher1.appendReplacement(sb, "<"+str.subSequence(k,l)+">");
        					}
        					matcher1.appendTail(sb);
        					str=sb.toString();
        					matcher1.reset();
        				}
        			}
    				flag=0;
            	}
                stmt1.execute("insert into wordnet values('"+rs.getString(1)+"','"+str+"')");
            	matcher.reset();
			}		
		}
        catch (Exception e)
        {
        		System.err.println(e);
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
            	Pattern pattern1 = Pattern.compile("[a-zA-Z\\_]+=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,]+\"");
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
        		int orcount=0;
        		str=rs.getString(3);
        		Pattern pattern = Pattern.compile("(<[a-zA-Z_ ]+>)");
                // Replace all occurrences of pattern in input
                Matcher matcher = pattern.matcher(str);
                while ( matcher.find()){
                	orcount +=1;
                }
                matcher.reset();
                if(orcount>1){
                		Pattern pattern1 = Pattern.compile("(<[a-zA-Z_ ]+>)[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´×\\s,µ*\\{\\}\\[\\](<\\{)(\\}>) m]+(<[a-zA-Z_ ]+>)");
                		matcher = pattern1.matcher(str);
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
	
	/*protected void nametag(){
	try
	{
		Statement stmt = conn.createStatement();
		Statement stmt1 = conn.createStatement();
		int count=0;
		//FileInputStream istream = new FileInputStream("result4.txt");
		//BufferedReader in = new BufferedReader(new InputStreamReader(istream));
		//FileOutputStream ostream = new FileOutputStream("result5.txt"); 
		//PrintStream out = new PrintStream( ostream ); 
		//String strLine;
		ResultSet rs = stmt.executeQuery("select * from segments");
    	//while ((strLine = in.readLine()) != null)
		while(rs.next())
    	{
       				CharSequence inputStr = rs.getString(3);              
       				// Compile regular expression
                    Pattern pattern1 = Pattern.compile("(<[a-zA-Z_ ]+>)");
                    Pattern pattern2 = Pattern.compile("((?<![<])\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\}(?![>]))");
                    Pattern pattern3 = Pattern.compile("([\\d]?[\\s]?\\.[\\s]?[\\d]+[\\s]?[\\–\\–]+[\\s]?[\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]?[\\s]?\\.[\\s]?[\\d]+)|([\\d]/[\\d][\\s]?[\\–\\-][\\s]?[\\d]/[\\d])|([\\d]/[\\d])");
                    Pattern pattern4 = Pattern.compile("(FLOAT[\\-]+FLOAT)");
                    Pattern pattern5 = Pattern.compile("(([\\d]+[\\-\\–]+[\\d]+)|([\\d]+))");
                    Pattern pattern6 = Pattern.compile("(NUMBER[\\-\\–]+NUMBER)");
                    Pattern pattern7 = Pattern.compile("(((?<![a-zA-Z])mm(?![a-zA-z]))|((?<![a-zA-Z])cm(?![a-zA-z]))|((?<![a-zA-Z])m(?![a-zA-z])))");
                    Pattern pattern8 = Pattern.compile("[\\-]{2}");
                    Pattern pattern9 = Pattern.compile("±[\\s]?STATE");
                    Pattern pattern10 = Pattern.compile("STATE[\\s]+ORGAN");
                    Pattern pattern11 = Pattern.compile("ORGAN\\sof\\sORGAN");
                    Pattern pattern12 = Pattern.compile("STATE([\\s]+STATE)+");
                                            
                    // Replace all occurrences of pattern in input
                    Matcher matcher = pattern1.matcher(inputStr);
                    String output = matcher.replaceAll("ORGAN");
                    matcher.reset();
                    
                    matcher = pattern2.matcher(output);
                    output = matcher.replaceAll("STATE");
                    matcher.reset();
                    
                    matcher = pattern3.matcher(output);
                    output = matcher.replaceAll("FLOAT");
                    matcher.reset();
                    
                    matcher = pattern4.matcher(output);
                    output = matcher.replaceAll("FLOAT");
                    matcher.reset();
                    
                    matcher = pattern5.matcher(output);
                    output = matcher.replaceAll("NUMBER");
                    matcher.reset();
                    
                    matcher = pattern6.matcher(output);
                    output = matcher.replaceAll("NUMBER");
                    matcher.reset();
                    
                    matcher = pattern7.matcher(output);
                    output = matcher.replaceAll("UNIT");
                    matcher.reset();
                    
                    matcher = pattern8.matcher(output);
                    output = matcher.replaceAll("-");
                    matcher.reset();
                    
                    matcher = pattern9.matcher(output);
                    output = matcher.replaceAll("STATE");
                    matcher.reset();
                    
                    matcher = pattern10.matcher(output);
                    output = matcher.replaceAll("ORGAN");
                    matcher.reset();
                    
                    matcher = pattern11.matcher(output);
                    output = matcher.replaceAll("ORGAN");
                    matcher.reset();
                    
                    matcher = pattern12.matcher(output);
                    output = matcher.replaceAll("STATE");
                    matcher.reset();
                    
                    //out.println(output);
                    count+=1;
                    stmt1.execute("insert into patterns values('"+count+"','"+output+"')");
    	}
    	//in.close();
    	//out.close();
    	////////////////////////////patterns();
	}
    catch (Exception e)
    {
    		System.err.println(e);
    }
}

protected void patterns(){
	try
	{
		Statement stmt = conn.createStatement();
		Statement stmt1 = conn.createStatement();
		Statement stmt2 = conn.createStatement();
		String str;
		int freq=0;
		int	count=0;
    	ResultSet rs = stmt.executeQuery("select distinct pattern from patterns");
    	while(rs.next()){
    		str=rs.getString("pattern");
    		ResultSet rs1 = stmt1.executeQuery("select * from patterns where pattern='"+str+"'");
    		freq=0;
    		count+=1;
    		while(rs1.next()){
    		freq+=1;
    		int sentid=rs1.getInt("sentid");
    		stmt2.execute("update segments set patternid='"+count+"' where sentid='"+sentid+"'");
    		}
    		stmt1.execute("insert into patterns_freq values('"+count+"','"+str+"','"+freq+"')");
    	}
    	/////////////////////////////parse_simpleseg();
	}
    catch (Exception e)
    {
    		System.err.println(e);
    }
}*/
	
	/*protected void modifiers(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			String str;
			
        	ResultSet rs = stmt.executeQuery("select distinct markedsent from segments order by length(markedsent)");
        	while(rs.next()){
        		int orcount=0;
        		str=rs.getString(3);
        		Pattern pattern = Pattern.compile("(<[a-zA-Z_ ]+>)");
                
                // Replace all occurrences of pattern in input
                Matcher matcher = pattern.matcher(str);
                while ( matcher.find()){
                	orcount +=1;
                }
                matcher.reset();
                if(orcount==1|orcount==0){
                	String str2="";
                	Pattern pattern1 = Pattern.compile("(<[a-zA-Z_ ]+>)|((?<![<])\\{[a-zA-Z_\\./\\-\\d\\s\\–{}]+\\}(?![>]))");
                	Matcher matcher1 = pattern1.matcher(str);
                	String str1=matcher1.replaceAll("#");
                	matcher1.reset();
                	Pattern pattern2 = Pattern.compile("[\\w±\\+-\\.]+|(<\\{[a-zA-Z_]+\\}>)");
                	Matcher matcher2 = pattern2.matcher(str1);
                	while ( matcher2.find()){
                		int i=matcher2.start();
                		int j=matcher2.end();
                		str2=str2.concat(str1.subSequence(i,j).toString())+" ";
                		
                	}
                	matcher2.reset();
                	stmt1.execute("insert into simpleseg_modifier values('"+str2+"','m','"+str+"')");	
                }
                else{
                	//stmt1.execute("insert into complexseg_modifier values('"+count+"','"+str+"','"+freq+"')");
                }
            }
		}
        catch (Exception e)
        {
        		System.err.println(e);
        }
	}*/
	
	protected void clausecount(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			String str;
			int freq1 = 0;
			int freq2 = 0;
			int freq3 = 0;
			int freq4 = 0;
			ResultSet rs = stmt.executeQuery("select * from segments");
			while(rs.next()){
				int orcount=0;
				str=rs.getString(3);
				Pattern pattern = Pattern.compile("(<[a-zA-Z_ ]+>)");
		        // Replace all occurrences of pattern in input
		        Matcher matcher = pattern.matcher(str);
		        while ( matcher.find()){
		        	orcount +=1;
		        }
		        matcher.reset();
		        if(orcount==1){
		        	freq1+=1;
		        }
		        else if(orcount==2){
		        	Pattern pattern1 = Pattern.compile("<[a-zA-Z_ ]+>[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´×\\s,µ*\\{\\}\\[\\](<\\{)(\\}>) m]+<[a-zA-Z_ ]+>");    		
        			matcher = pattern1.matcher(str);
        			while(matcher.find()){
        				String str1 = "";
        				int i=matcher.start();
                    	int j=matcher.end();
                   		str1=str.subSequence(i,j).toString();
                   		str1=str1.substring(str1.indexOf(">")+1,str1.lastIndexOf("<"));
                   	  	Pattern pattern2 = Pattern.compile("[\\s][a-zA-Z]+");    		
            			Matcher matcher1 = pattern2.matcher(str1);
           		 		int streq=0;
            			while(matcher1.find()){
            				String str2 = "";
            				int k=matcher1.start()+1;
            				int l=matcher1.end();
            				str2=str1.subSequence(k,l).toString();
            				ResultSet rs1=stmt1.executeQuery("Select * from preposition where prep='"+str2+"'");
               		 		while (rs1.next()){
               		 			streq=1;
               		 		}
            			}
            			matcher1.reset();
            			if(streq==1)
            				freq2+=1;
            			Pattern pattern3 = Pattern.compile("[\\s][a-zA-Z]+(ed|ing)");    		
            			matcher1 = pattern3.matcher(str1);
            			int verb=0;
            			while(matcher1.find()){
            				verb=1;
            			}
            			matcher1.reset();
            			if(verb==1)
            				freq3+=1;
        			}
        			matcher.reset();
		        }
		        else if(orcount>2){
		        	freq4+=1;
		        }
			}	
		    System.out.println("Clause1:"+freq1);
		    System.out.println("Clause2:"+freq2);
		    System.out.println("Clause3:"+freq3);
		    System.out.println("Clause4:"+freq4);

		}catch (Exception e)
        {
    		System.err.println(e);
        }
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new CharacterMarkup("onto_fna_corpus");

	
	}

}

