 /* $Id: ParseComplexseg_notinuse.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings({  "unused" })
public class ParseComplexseg_notinuse {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "termsuser";
	static protected String password = "termspassword";
	int flag6;

	public ParseComplexseg_notinuse() {
		// TODO Auto-generated constructor stub
	}
	
	public ParseComplexseg_notinuse(String database) {
		// TODO Auto-generated constructor stub
		collect(database);
	}

	protected void collect(String database){
		ParseComplexseg_notinuse.database = database;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("create table if not exists marked_complexseg (sentid MEDIUMINT NOT NULL, source varchar(100) NOT NULL, markedsent TEXT, relation TEXT, PRIMARY KEY(sentid))");
				stmt.execute("delete from marked_complexseg");
				parse_complexseg();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	protected void parse_complexseg(){
		try
		{
			Statement stmt = conn.createStatement();
			Statement stmt1 = conn.createStatement();
			Statement stmt2 = conn.createStatement();
			Statement stmt3 = conn.createStatement();
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
                	ParseSimpleseg ps = new ParseSimpleseg();
                	str = ps.reversecondense(str);
                	/*StringBuffer sb2 = new StringBuffer();
                	Pattern pattern1 = Pattern.compile("<[a-zA-Z_ ]+>");
                	matcher = pattern1.matcher(str);
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
					matcher.reset();*/
                	
                	String str1="";
                	str1 = ps.plaintextextractornum(str);
                	/*Pattern pattern3 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×µ%“”\\_,]+");
                	matcher = pattern3.matcher(str);
                	while ( matcher.find()){
                		int i=matcher.start();
                		int j=matcher.end();
                		str1=str1.concat(str.subSequence(i,j).toString());
                		System.out.println(str1);
                	}
                	matcher.reset();*/
                	String str6=ps.plaintextextractor(str);
                	orcount = 0;
                	                    
                    // Replace all occurrences of pattern in input
                    matcher = pattern.matcher(str);
                    while ( matcher.find()){
                    	//System.out.println("Organ:"+str.substring(matcher.start(),matcher.end()));
                    	orcount+=1;
                    }
                    matcher.reset();
                	String str2 = "";
                	String org1 = "";
                	String organ1 = "";
                	String org2 = "";
                	String organ2 = "";
                	String innertagstate = "";
                	String relation = "";
                	String plainrelation = "";
                	String markedrelations = "";
                	String negation = "";
                	String outertag = "";
                	String innertags = "";
                	String innertags1 = "";
                	String markedsent = "";
                	String markedsent1 = "";
                	String markedsent2 = "";
                	String charset = "";
                	System.out.println("str:"+str);
                	Pattern pattern4 = Pattern.compile("(<[a-zA-Z_ ]+>)[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\]=(<\\{)(\\}>) m]+(<[a-zA-Z_ ]+>)");
            		matcher = pattern4.matcher(str);
            		while ( matcher.find()){
            			int i=matcher.start();
                		int j=matcher.end();
                		str2=str.subSequence(i,j).toString();
                		//System.out.println("str2:"+str2);
                		int m=str2.indexOf("<");
                		int k=str2.indexOf(">");
                		int l;
                		int ct=1;
                		while(ct<orcount){
                			if(str2.charAt(m+1)!='{'){
                				org1=str2.substring(m+1,k);
                				if(ct==1)
                					organ1 = org1;
                				ResultSet rs2 = stmt3.executeQuery("select * from singularplural where plural='"+org1+"'");
                				while(rs2.next()){
                					org1 = rs2.getString(1);
                				}
                			}
                			if(org1.compareTo("n")==0)
                				org1 = "chromosome";
                			l=str2.indexOf("<",k);
                			if(str2.charAt(l+1)!='{'){
                				org2=str2.substring(l+1,str2.indexOf(">",l));
                				organ2 = org2;
                				ResultSet rs2 = stmt3.executeQuery("select * from singularplural where plural='"+org2+"'");
                				while(rs2.next()){
                					org2 = rs2.getString(1);
                				}
                			}
                			if(org2.compareTo("n")==0)
                				org2 = "chromosome";
                			relation=str2.substring(k+2,l);
                			m=l;
                			k=str2.indexOf(">",l);
                			innertagstate = "";
                			//System.out.println("relation:"+relation);
                			if(relation.contains(":")){
                				charset = relation.substring(0, relation.lastIndexOf("{:}"));
                				relation = relation.substring(relation.lastIndexOf("{:}")+3);
                				int flag7 = 0;
                				String plaincharset = "";
                				plaincharset = ps.plaintextextractornum(charset);
                				/*Pattern pattern12 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]+");
                            	Matcher matcher2 = pattern12.matcher(charset);
                            	while ( matcher2.find()){
                            		i=matcher2.start();
                            		j=matcher2.end();
                            		plaincharset=plaincharset.concat(charset.subSequence(i,j).toString());
                            	}
                            	matcher2.reset();*/
                				
                				Pattern pattern19 = Pattern.compile("[±]?[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?[m]?[\\s]?[xX\\×]+[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?m");
                            	Matcher matcher2 = pattern19.matcher(plaincharset);
                            	int flag3=0;
                            	while ( matcher2.find()){
                            		if(plaincharset.charAt(matcher2.start())==' '){
                            			i=matcher2.start()+1;
                            		}
                            		else{
                            			i=matcher2.start();
                            		}
                            		j=matcher2.end();
                            		if(flag3==0)
                        				innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
                        			else
                        				innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
                        			flag3=1;
                            	}
                            	if(flag3==1)
                            		innertagstate=innertagstate.concat("\"");
                            	plaincharset = matcher2.replaceAll("#");
                            	matcher2.reset();
                            	//System.out.println("plaincharset1:"+plaincharset);
                            	int sizect = 0;
                				Pattern pattern13 = Pattern.compile("[xX\\×±\\d\\[\\]\\–\\-\\.\\s\\+]+[\\s]?[dcmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
                            	matcher2 = pattern13.matcher(plaincharset);
                            	int flag=0;
                            	String numrange="";
                            	while ( matcher2.find()){
                            		flag7 = 1;
                            		if(plaincharset.charAt(matcher2.start())==' '){
                            			i=matcher2.start()+1;
                            		}
                            		else{
                            			i=matcher2.start();
                            		}
                            		j=matcher2.end();
                            		String extreme = plaincharset.substring(i,j);
                        			i = 0;
                        			j = extreme.length();
                            		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
                                	Matcher matcher1 = pattern20.matcher(extreme);
                                	if ( matcher1.find()){
                                		int p = matcher1.start();
                                		int q = matcher1.end();
                                		if(extreme.charAt(q-2)=='–' | extreme.charAt(q-2)=='-')
                                			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-2)+"\"");
                                		else
                                			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
                                	}
                                	extreme = matcher1.replaceAll("#");
                            		matcher1.reset();
                            		if(extreme.contains("#"))
                            			i = extreme.indexOf("#")+1;
                            		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
                                	matcher1 = pattern21.matcher(extreme);
                                	if ( matcher1.find()){
                                		int p = matcher1.start();
                                		int q = matcher1.end();
                                		if(extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-')
                                			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+2,q-1)+"\"");
                                		else
                                			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
                                	}
                                	extreme = matcher1.replaceAll("#");
                            		matcher1.reset();
                            		j = extreme.length();
                            		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
	                            	matcher1 = pattern23.matcher(extreme);
	                            	if ( matcher1.find()){
	                            		int p = matcher1.start();
	                            		int q = matcher1.end();
	                            		numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
	                            	}
	                            	extreme = matcher1.replaceAll("#");
	                            	matcher1.reset();
	                            	j = extreme.length();
                            		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
                            			String extract = extreme.substring(i,j);
                            			Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m(([\\s]diam)?([\\s]wide)?)");
                                    	Matcher matcher3 = pattern18.matcher(extract);
                                    	String unit="";
                                    	if ( matcher3.find()){
                                    		unit = extract.substring(matcher3.start(), matcher3.end());
                                    	}
                                    	extract = matcher3.replaceAll("#");
                                    	matcher3.reset();
                            			numrange = numrange.concat(" min_size_"+sizect+"=\""+extract.substring(0, extract.indexOf('-'))+"\" min_size_unit_"+sizect+"=\""+unit+"\" max_size_"+sizect+"=\""+extract.substring(extract.indexOf('-')+1,extract.indexOf('#'))+"\" max_size_unit_"+sizect+"=\""+unit+"\"");
                            			sizect+=1;
                            		}
                            		else{
                            			if(flag3==1){
                            				StringBuffer sb = new StringBuffer();
                    						Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
                    						matcher1 = pattern9.matcher(innertagstate);
                    						while ( matcher1.find()){
                    							int p=matcher1.start();
                    							int q=matcher1.end();
                    							matcher1.appendReplacement(sb, innertagstate.subSequence(p,q-1)+","+extreme.subSequence(i,j).toString()+"\"");
                    						}
                    						matcher1.appendTail(sb);
                    						innertagstate=sb.toString();
                    						matcher1.reset();
                            			}
                            			else{
                            				if(flag==0)
                            					innertagstate=innertagstate.concat(" "+"size=\""+extreme.subSequence(i,j).toString());
                            				else
                            					innertagstate=innertagstate.concat(","+extreme.subSequence(i,j).toString());
                            				flag=1;
                            			}
                            		}
                            	}
                            	if(flag==1)
                            		innertagstate=innertagstate.concat("\"");
                            	plaincharset = matcher2.replaceAll("#");
                            	innertagstate = innertagstate.concat(numrange);
                            	matcher2.reset();
                            	//System.out.println("plaincharset2:"+plaincharset);
                            	Pattern pattern14 = Pattern.compile("[±\\d\\[\\]\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
                            	matcher2 = pattern14.matcher(plaincharset);
                            	int flag1=0;
                            	while ( matcher2.find()){
                            		flag7 = 1;
                            		if(plaincharset.charAt(matcher2.start())==' '){
                            			i=matcher2.start()+1;
                            		}
                            		else{
                            			i=matcher2.start();
                            		}
                            		j=matcher2.end();
                            		if(flag==1){
                            			StringBuffer sb = new StringBuffer();
                    					Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
                    					Matcher matcher1 = pattern9.matcher(innertagstate);
                    					while ( matcher1.find()){
                    						int p=matcher1.start();
                    						int q=matcher1.end();
                    						matcher1.appendReplacement(sb, innertagstate.subSequence(p,q-1)+","+plaincharset.subSequence(i,j).toString()+"\"");
                    					}
                    					matcher1.appendTail(sb);
                    					innertagstate=sb.toString();
                    					matcher1.reset();
                            		}
                            		else{
                            			if(flag1==0)
                            				innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
                            			else
                            				innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
                            			flag1=1;
                            		}
                            	}
                            	if(flag1==1)
                            		innertagstate=innertagstate.concat("\"");
                            	plaincharset = matcher2.replaceAll("#");
                            	matcher2.reset();
                            	int countct = 0;
                            	Pattern pattern15 = Pattern.compile("([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?|[\\[]?[±]?[\\d]+[+]?[\\]]?)[\\–\\–\\-]+[a-zA-Z]+");
                            	matcher2 = pattern15.matcher(plaincharset);
                            	plaincharset = matcher2.replaceAll("#");
                            	matcher2.reset();     	
                            	Pattern pattern16 = Pattern.compile("(?<!([/][\\s]?))([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?([\\[]?[\\–\\-]?[\\]]?[\\[]?[\\d]+[+]?[\\]]?)*|[±]?[\\d]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\–\\-]?% of [\\w]+ length|[\\s]?[\\–\\-]?height of [\\w]+|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[dcmµ]?m))");
                            	matcher2 = pattern16.matcher(plaincharset);
                            	int flag2=0;
                            	String countrange = "";
                            	while ( matcher2.find()){
                            		flag7 = 1;
                            		i=matcher2.start();
                            		j=matcher2.end();
                            		String extreme = plaincharset.substring(i,j);
                        			i = 0;
                        			j = extreme.length();
                            		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
                                	Matcher matcher1 = pattern20.matcher(extreme);
                                	if ( matcher1.find()){
                                		int p = matcher1.start();
                                		int q = matcher1.end();
                                		if(extreme.charAt(q-2)=='–' | extreme.charAt(q-2)=='-')
                                			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-2)+"\"");
                                		else
                                			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-1)+"\"");
                                	}
                                	extreme = matcher1.replaceAll("#");
                            		matcher1.reset();
                            		if(extreme.contains("#"))
                            			i = extreme.indexOf("#")+1;
                            		j = extreme.length();
                            		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
                                	matcher1 = pattern21.matcher(extreme);
                                	if ( matcher1.find()){
                                		int p = matcher1.start();
                                		int q = matcher1.end();
                                		j = p;
                                		if(extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-')
                                			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(p+2,q-1)+"\"");
                                		else
                                			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-1)+"\"");
                                	}
                            		matcher1.reset();
                            		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
                            			String extract = extreme.substring(i,j);
                            			Pattern pattern22 = Pattern.compile("[\\[\\]]+");
                            			matcher1 = pattern22.matcher(extract);
                            			extract = matcher1.replaceAll("");
                            			matcher1.reset();
                            			countrange = countrange.concat(" min_count_"+countct+"=\""+extract.substring(0, extract.indexOf('-'))+"\" max_count_"+countct+"=\""+extract.substring(extract.indexOf('-')+1,extract.length())+"\"");
                            			countct+=1;
                            		}
                            		else{
                            			if(flag2==0)
                            				innertagstate=innertagstate.concat(" "+"count=\""+extreme.subSequence(i,j).toString());
                            			else
                            				innertagstate=innertagstate.concat(","+extreme.subSequence(i,j).toString());
                            			flag2=1;
                            		}
                            	}
                            	if(flag2==1)
                            		innertagstate=innertagstate.concat("\"");
                            	innertagstate = innertagstate.concat(countrange);
                            	matcher2.reset();                				
                				Pattern pattern7 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
                            	matcher2 = pattern7.matcher(charset);
                            	String str3 = "";
                            	//int flag3=0;
                            	while (matcher2.find()){
                            		flag7=1;
                            		int flag5=0;
                            		String first = "";
                            		String chstate = "";
                            		i=matcher2.start()+1;
                            		j=matcher2.end()-1;
                            		str3=charset.subSequence(i,j).toString();
                            		if(str3.contains("-")|str3.contains("–")){
                            			first = str3.substring(0, str3.indexOf("-"));
                            			str3=str3.substring(str3.indexOf("-")+1|str3.indexOf("–")+1, str3.length());
                            			flag5=1;
                            		}
                            			ResultSet rs1 = stmt1.executeQuery("select category from fnaglossaryfixed where term='"+str3+"'");
                            			if(rs1.next()){
                            				int flag4=0;
                            				chstate=rs1.getString("category");
                            				if(chstate.contains("/")){
                            					String [] terms = chstate.split("/");
                            					chstate=terms[0];
                                    			for(int t=1;t<terms.length;t++)
                                    				chstate=chstate.concat("_or_"+terms[t]);  
                            				}
                            				StringBuffer sb = new StringBuffer();
                        					Pattern pattern8 = Pattern.compile(chstate+"=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
                        					Matcher matcher3 = pattern8.matcher(innertagstate);
                        					while ( matcher3.find()){
                        						int q=matcher3.start();
                        						int r=matcher3.end();
                        						if(flag5==1)
                        							matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+first+"-"+str3+"\"");
                                				else
                                					matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+str3+"\"");
                        						flag4=1;
                        					}
                        					if(flag4==1){
                        						matcher3.appendTail(sb);
                        						innertagstate=sb.toString();
                        					}
                        					else{
                        						if(flag5==1)
                        							innertagstate=innertagstate.concat(" "+chstate+"=\""+first+"-"+str3+"\"");
                        						else
                        							innertagstate=innertagstate.concat(" "+chstate+"=\""+str3+"\"");
                        					}
                        					matcher3.reset();
                            			}                			
                            	}
                        		matcher2.reset();                				
                				Pattern pattern5 = Pattern.compile("[a-zA-Z]+");
                        		Matcher matcher1 = pattern5.matcher(relation);
                        		flag=0;
                        		while ( matcher1.find()){
                            		i=matcher1.start();
                            		j=matcher1.end();
                            		ResultSet rs1=stmt1.executeQuery("Select * from negation where term='"+relation.subSequence(i,j).toString()+"'");
                    				while(rs1.next()){
                    					flag=1;
                    				}
                        		}
                        		matcher1.reset();
                        		if(flag==1)
                        			negation = "yes";
                        		else
                        			negation = "no";
                        		//plainrelation = ps.plaintextextractor(relation);
                        		Pattern pattern3 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×µ%“”\\_,]+");
                        		matcher1 = pattern3.matcher(relation);
                        		while ( matcher1.find()){
                            		i=matcher1.start();
                            		j=matcher1.end();
                            		plainrelation=plainrelation.concat(relation.subSequence(i,j).toString());
                            	}
                            	matcher1.reset();
                            	Pattern pattern10 = Pattern.compile("_");
                                // Replace all occurrences of pattern in input
                                matcher2 = pattern10.matcher(plainrelation);
                                plainrelation = matcher2.replaceAll(" ");
                                matcher2.reset();
                                Pattern pattern11 = Pattern.compile(":");
                                // Replace all occurrences of pattern in input
                                matcher2 = pattern11.matcher(plainrelation);
                                plainrelation = matcher2.replaceAll(",");
                                matcher2.reset();
                            	if(ct==1)
                            		outertag=outertag.concat("<"+org1+"_R"+ct+"_"+org2);
                            	else
                            		outertag=outertag.concat("_R"+ct+"_"+org2);
                            	if(flag7 == 1)
                            		innertags=innertags.concat("<"+org1+innertagstate+"/><"+org2+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
                            	else	
                            		innertags=innertags.concat("<"+org1+"/><"+org2+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
                            	markedrelations = markedrelations.concat("<relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
                			}
                			else{
                				Pattern pattern5 = Pattern.compile("[a-zA-Z]+");
                        		Matcher matcher1 = pattern5.matcher(relation);
                        		int flag=0;
                        		while ( matcher1.find()){
                            		i=matcher1.start();
                            		j=matcher1.end();
                            		ResultSet rs1=stmt1.executeQuery("Select * from negation where term='"+relation.subSequence(i,j).toString()+"'");
                    				while(rs1.next()){
                    					flag=1;
                    				}
                        		}
                        		matcher1.reset();
                        		if(flag==1)
                        			negation = "yes";
                        		else
                        			negation = "no";
                        		//plainrelation = ps.plaintextextractor(relation);
                        		Pattern pattern3 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×µ%“”\\_,]+");
                        		matcher1 = pattern3.matcher(relation);
                            	while ( matcher1.find()){
                            		i=matcher1.start();
                            		j=matcher1.end();
                            		plainrelation=plainrelation.concat(relation.subSequence(i,j).toString());
                            	}
                            	matcher1.reset();
                            	Pattern pattern10 = Pattern.compile("_");
                                // Replace all occurrences of pattern in input
                                Matcher matcher2 = pattern10.matcher(plainrelation);
                                plainrelation = matcher2.replaceAll(" ");
                                matcher2.reset();
                                Pattern pattern11 = Pattern.compile(":");
                                // Replace all occurrences of pattern in input
                                matcher2 = pattern11.matcher(plainrelation);
                                plainrelation = matcher2.replaceAll(",");
                                matcher2.reset();
                            	if(ct==1)
                            		outertag=outertag.concat("<"+org1+"_R"+ct+"_"+org2);
                            	else
                            		outertag=outertag.concat("_R"+ct+"_"+org2);
                            	innertags=innertags.concat("<"+org1+"/><"+org2+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
                            	markedrelations = markedrelations.concat("<relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
                			}
                			ct++;
                		}
                		outertag=outertag.concat(">");               		
                		
                //handle states for first and last organ in segment
                		innertagstate = "";
                		Pattern pattern6 = Pattern.compile("[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,µ%\\*\\{\\}\\[\\](<\\{)(\\}>)m]+<"+organ1+">");
                    	Matcher matcher1 = pattern6.matcher(str);
                    	String state = "";
                    	flag6 = 0;
                    	while ( matcher1.find()){
                    		i=matcher1.start();
                    		j=matcher1.end();
                    		state=state.concat(str.subSequence(i,j).toString());
                    		String plaincharset = "";
                    		plaincharset = ps.plaintextextractornum(state);
                    		/*Pattern pattern12 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]+");
                        	Matcher matcher2 = pattern12.matcher(state);
                        	while ( matcher2.find()){
                        		i=matcher2.start();
                        		j=matcher2.end();
                        		plaincharset=plaincharset.concat(state.subSequence(i,j).toString());
                        	}
                        	matcher2.reset();*/
            				innertagstate = charstatehandler(plaincharset, state);
                    	}
                    	matcher1.reset();
                    	if(flag6 == 1){
                    		StringBuffer sb = new StringBuffer();
                    		if(organ1.compareTo("n")==0)
                    			organ1 = "chromosome_count";
                    		Pattern pattern9 = Pattern.compile("<"+organ1+"[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]*/>");
                    		Matcher matcher2 = pattern9.matcher(innertags);
                    		if ( matcher2.find()){
                    			int q=matcher2.start();
                    			matcher2.appendReplacement(sb, innertags.subSequence(q,innertags.indexOf("/",q))+" "+innertagstate+"/>");
                    		}
                    		matcher2.appendTail(sb);
                    		innertags=sb.toString();
                    	}
                    	innertagstate = "";
                    	Pattern pattern10 = Pattern.compile("<"+organ2+">[\\w±\\+\\–\\-\\—°.²:½/¼\"“”\\_;x´\\×\\s,=µ%\\*\\{\\}\\[\\](<\\{)(\\}>)m]+");
                    	matcher1 = pattern10.matcher(str);
                    	state = "";
                    	flag6 = 0;
                    	while ( matcher1.find()){
                    		i=matcher1.start();
                    		j=matcher1.end();
                    		state=state.concat(str.subSequence(i,j).toString());
                    		String plaincharset = "";
                    		plaincharset = ps.plaintextextractornum(state);
            				/*Pattern pattern12 = Pattern.compile("[\\w±\\+\\–\\-\\—°²\\.:=/\\s½\"¼;x´\\×\\*µ%“”\\_,]+");
                        	Matcher matcher2 = pattern12.matcher(state);
                        	while ( matcher2.find()){
                        		i=matcher2.start();
                        		j=matcher2.end();
                        		plaincharset=plaincharset.concat(state.subSequence(i,j).toString());
                        	}
                        	matcher2.reset();*/
                    		innertagstate = charstatehandler(plaincharset, state);
            				/*Pattern pattern13 = Pattern.compile("[xX\\×±\\d\\–\\-\\.\\s\\+]+[\\s]?[dcmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
                        	Matcher matcher2 = pattern13.matcher(plaincharset);
                        	int flag=0;
                        	while ( matcher2.find()){
                        		flag6 = 1;
                        		if(plaincharset.charAt(matcher2.start())==' '){
                        			i=matcher2.start()+1;
                        		}
                        		else{
                        			i=matcher2.start();
                        		}
                        		j=matcher2.end();
                        		if(flag==0)
                        			innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
                        		else
                        			innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
                        		flag=1;
                        	}
                        	if(flag==1)
                        		innertagstate=innertagstate.concat("\"");
                        	plaincharset = matcher2.replaceAll("#");
                        	matcher2.reset();
                        	Pattern pattern14 = Pattern.compile("[±\\d\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
                        	matcher2 = pattern14.matcher(plaincharset);
                        	int flag1=0;
                        	while ( matcher2.find()){
                        		flag6 = 1;
                        		if(plaincharset.charAt(matcher2.start())==' '){
                        			i=matcher2.start()+1;
                        		}
                        		else{
                        			i=matcher2.start();
                        		}
                        		j=matcher2.end();
                        		if(flag==1){
                        			StringBuffer sb = new StringBuffer();
                					Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
                					Matcher matcher3 = pattern9.matcher(innertagstate);
                					while ( matcher3.find()){
                						int p=matcher3.start();
                						int q=matcher3.end();
                						matcher3.appendReplacement(sb, innertagstate.subSequence(p,q-1)+","+plaincharset.subSequence(i,j).toString()+"\"");
                					}
                					matcher3.appendTail(sb);
                					innertagstate=sb.toString();
                					matcher3.reset();
                        		}
                        		else{
                        			if(flag1==0)
                        				innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
                        			else
                        				innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
                        			flag1=1;
                        		}
                        	}
                        	if(flag1==1)
                        		innertagstate=innertagstate.concat("\"");
                        	plaincharset = matcher2.replaceAll("#");
                        	matcher2.reset();
                        	Pattern pattern15 = Pattern.compile("([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)[\\–\\–\\-]+[a-zA-Z]+");
                        	matcher2 = pattern15.matcher(plaincharset);
                        	plaincharset = matcher2.replaceAll("#");
                        	matcher2.reset();     	
                        	Pattern pattern16 = Pattern.compile("(?<!([/]))([±]?[\\d]+[\\–\\-][\\d]+[+]?|[±]?[\\d]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\–\\-]?% of [\\w]+ length|[\\s]?[\\–\\-]?height of [\\w]+|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[dcmµ]?m))");
                        	matcher2 = pattern16.matcher(plaincharset);
                        	int flag2=0;
                        	while ( matcher2.find()){
                        		flag6 = 1;
                        		i=matcher2.start();
                        		j=matcher2.end();
                        		if(flag2==0)
                        			innertagstate=innertagstate.concat(" "+"count=\""+plaincharset.subSequence(i,j).toString());
                        		else
                        			innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
                        		flag2=1;
                        	}
                        	if(flag2==1)
                        		innertagstate=innertagstate.concat("\"");
                        	matcher2.reset();   
                        	//System.out.println("state:"+state);
                    		Pattern pattern7 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
                        	matcher2 = pattern7.matcher(state);
                        	String str3 = "";
                        	//int flag3=0;
                        	while ( matcher2.find()){
                        		flag6=1;
                        		int flag5=0;
                        		String first = "";
                        		String chstate = "";
                        		i=matcher2.start()+1;
                        		j=matcher2.end()-1;
                        		//System.out.println("inside4");
                        		str3=state.subSequence(i,j).toString();
                        		if(str3.contains("-")|str3.contains("–")){
                        			first = str3.substring(0, str3.indexOf("-"));
                        			str3=str3.substring(str3.indexOf("-")+1|str3.indexOf("–")+1, str3.length());
                        			//System.out.println("inside5");
                        			flag5=1;
                        		}
                        			ResultSet rs1 = stmt1.executeQuery("select * from fnaglossaryfixed where term='"+str3+"'");
                        			if(rs1.next()){
                        				int flag4=0;
                        				chstate=rs1.getString(4);
                        				if(chstate.contains("/")){
                        					String [] terms = chstate.split("/");
                        					chstate=terms[0];
                                			for(int t=1;t<terms.length;t++)
                                				chstate=chstate.concat("_or_"+terms[t]);  
                        					System.out.println(chstate);
                        				}
                        				StringBuffer sb = new StringBuffer();
                    					Pattern pattern8 = Pattern.compile(chstate+"=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
                    					Matcher matcher3 = pattern8.matcher(innertagstate);
                    					while ( matcher3.find()){
                    						int q=matcher3.start();
                    						int r=matcher3.end();
                    						if(flag5==1)
                    							matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+first+"-"+str3+"\"");
                            				else{
                            					matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+str3+"\"");
                            				}
                            				flag4=1;
                    					}
                    					if(flag4==1){
                    						matcher3.appendTail(sb);
                    						innertagstate=sb.toString();
                    					}
                    					else{
                    						if(flag5==1)
                    							innertagstate=innertagstate.concat(" "+chstate+"=\""+first+"-"+str3+"\"");
                    						else
                    							innertagstate=innertagstate.concat(" "+chstate+"=\""+str3+"\"");
                    					}
                    					matcher3.reset();
                        			}                			
                        	}
                    		matcher2.reset();*/
                    	}
                    	matcher1.reset();
                    	if(flag6 == 1){
                    		//System.out.println("innertags:"+innertags);
                    		//System.out.println("<"+org2+"/><relation code=\"R"+(ct-1)+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
                    		ct-=1;
                    		StringBuffer sb = new StringBuffer();
                    		Pattern pattern9 = Pattern.compile("<"+org2+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
                    		Matcher matcher2 = pattern9.matcher(innertags);
                    		while ( matcher2.find()){
                    			int q=matcher2.start();
                    			matcher2.appendReplacement(sb, innertags.subSequence(q,innertags.indexOf("/",q))+innertagstate+"/><relation code=\"R"+ct+"\" agent=\""+org1+"\" target=\""+org2+"\" name=\""+plainrelation+"\" negation=\""+negation+"\"/>");
                    		}
                    		matcher2.appendTail(sb);
                    		innertags=sb.toString();
                    		matcher2.reset();
                    	}
                    }
            		System.out.println("str6:"+str6);
            		Pattern pattern10 = Pattern.compile("_");
                    // Replace all occurrences of pattern in input
                    Matcher matcher2 = pattern10.matcher(str6);
                    str6 = matcher2.replaceAll(" ");
                    matcher2.reset();
                    Pattern pattern11 = Pattern.compile(":");
                    // Replace all occurrences of pattern in input
                    matcher2 = pattern11.matcher(str6);
                    str6 = matcher2.replaceAll(",");
                    matcher2.reset();
            		innertags = innertags.concat("<Text>"+str6+"</Text>");
            		markedsent = markedsent.concat(outertag+innertags+"</"+outertag.substring(1));
            		stmt1.execute("insert into marked_complexseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+markedsent+"','"+markedrelations+"')");
            		//stmt1.execute("insert into marked_simpleseg values('"+rs.getString(1)+"','"+rs.getString(2)+"','"+markedsent+"')");
                }
			}
		}catch (Exception e)
        {
    		System.err.println(e);
        }
	}
	
	protected String charstatehandler(String plaincharset, String state){
		String innertagstate = "";
		try{
			Statement stmt1 = conn.createStatement();
			int i,j;
			//System.out.println("plain:"+plaincharset);
			//System.out.println("state:"+state);
			Pattern pattern19 = Pattern.compile("[±]?[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?[m]?[\\s]?[xX\\×]+[\\[]?[\\d\\s\\.]+[\\]]?[\\[]?[\\–\\-]+[\\]]?[\\[]?[\\d\\s\\.]+[\\]]?[dcmµ]?m");
        	Matcher matcher2 = pattern19.matcher(plaincharset);
        	int flag3=0;
        	while ( matcher2.find()){
        		if(plaincharset.charAt(matcher2.start())==' '){
        			i=matcher2.start()+1;
        		}
        		else{
        			i=matcher2.start();
        		}
        		j=matcher2.end();
        		if(flag3==0)
    				innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
    			else
    				innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
    			flag3=1;
        	}
        	if(flag3==1)
        		innertagstate=innertagstate.concat("\"");
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	
           	int sizect = 0;
           	Pattern pattern13 = Pattern.compile("[xX\\×±\\d\\[\\]\\–\\-\\.\\s\\+]+[\\s]?[dcmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
	    	matcher2 = pattern13.matcher(plaincharset);
	    	int flag=0;
	    	String numrange="";
	    	while ( matcher2.find()){
	    		flag6 = 1;
	    		if(plaincharset.charAt(matcher2.start())==' '){
	    			i=matcher2.start()+1;
	    		}
	    		else{
	    			i=matcher2.start();
	    		}
	    		j=matcher2.end();
	    		String extreme = plaincharset.substring(i,j);
    			i = 0;
    			j = extreme.length();
        		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
            	Matcher matcher1 = pattern20.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(q-2)=='–' | extreme.charAt(q-2)=='-')
            			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-2)+"\"");
            		else
            			numrange = numrange.concat(" min_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		if(extreme.contains("#"))
        			i = extreme.indexOf("#")+1;
        		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern21.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-')
            			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+2,q-1)+"\"");
            		else
            			numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		j = extreme.length();
        		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		numrange = numrange.concat(" max_extreme_size_"+sizect+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
            	extreme = matcher1.replaceAll("#");
            	matcher1.reset();
            	j = extreme.length();
        		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m(([\\s]diam)?([\\s]wide)?)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	String unit="";
                	if ( matcher3.find()){
                		unit = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
        			numrange = numrange.concat(" min_size_"+sizect+"=\""+extract.substring(0, extract.indexOf('-'))+"\" min_size_unit_"+sizect+"=\""+unit+"\" max_size_"+sizect+"=\""+extract.substring(extract.indexOf('-')+1,extract.indexOf('#'))+"\" max_size_unit_"+sizect+"=\""+unit+"\"");
        			sizect+=1;
        		}
        		else{
        			if(flag3==1){
        				StringBuffer sb = new StringBuffer();
						Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
						matcher1 = pattern9.matcher(innertagstate);
						while ( matcher1.find()){
							int p=matcher1.start();
							int q=matcher1.end();
							matcher1.appendReplacement(sb, innertagstate.subSequence(p,q-1)+","+extreme.subSequence(i,j).toString()+"\"");
						}
						matcher1.appendTail(sb);
						innertagstate=sb.toString();
						matcher1.reset();
        			}
        			else{
        				if(flag==0)
        					innertagstate=innertagstate.concat(" "+"size=\""+extreme.subSequence(i,j).toString());
        				else
        					innertagstate=innertagstate.concat(","+extreme.subSequence(i,j).toString());
        				flag=1;
        			}
        		}
	    	}
	    	if(flag==1)
	    		innertagstate=innertagstate.concat("\"");
	    	plaincharset = matcher2.replaceAll("#");
	    	innertagstate = innertagstate.concat(numrange);
	    	matcher2.reset();
	    	Pattern pattern14 = Pattern.compile("[±\\d\\[\\]\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
	    	matcher2 = pattern14.matcher(plaincharset);
	    	int flag1=0;
	    	while ( matcher2.find()){
	    		flag6 = 1;
	    		if(plaincharset.charAt(matcher2.start())==' '){
	    			i=matcher2.start()+1;
	    		}
	    		else{
	    			i=matcher2.start();
	    		}
	    		j=matcher2.end();
	    		if(flag==1){
	    			StringBuffer sb = new StringBuffer();
					Pattern pattern9 = Pattern.compile("size=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
					Matcher matcher3 = pattern9.matcher(innertagstate);
					while ( matcher3.find()){
						int p=matcher3.start();
						int q=matcher3.end();
						matcher3.appendReplacement(sb, innertagstate.subSequence(p,q-1)+","+plaincharset.subSequence(i,j).toString()+"\"");
					}
					matcher3.appendTail(sb);
					innertagstate=sb.toString();
					matcher3.reset();
	    		}
	    		else{
	    			if(flag1==0)
	    				innertagstate=innertagstate.concat(" "+"size=\""+plaincharset.subSequence(i,j).toString());
	    			else
	    				innertagstate=innertagstate.concat(","+plaincharset.subSequence(i,j).toString());
	    			flag1=1;
	    		}
	    	}
	    	if(flag1==1)
	    		innertagstate=innertagstate.concat("\"");
	    	plaincharset = matcher2.replaceAll("#");
	    	matcher2.reset();
	    	int countct = 0;
	    	Pattern pattern15 = Pattern.compile("([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?|[\\[]?[±]?[\\d]+[+]?[\\]]?)[\\–\\–\\-]+[a-zA-Z]+");
	    	matcher2 = pattern15.matcher(plaincharset);
	    	plaincharset = matcher2.replaceAll("#");
	    	matcher2.reset();     	
	    	Pattern pattern16 = Pattern.compile("(?<!([/][\\s]?))([\\[]?[±]?[\\d]+[\\]]?[\\[]?[\\–\\-][\\]]?[\\[]?[\\d]+[+]?[\\]]?([\\[]?[\\–\\-]?[\\]]?[\\[]?[\\d]+[+]?[\\]]?)*|[±]?[\\d]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\–\\-]?% of [\\w]+ length|[\\s]?[\\–\\-]?height of [\\w]+|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[dcmµ]?m))");
	    	matcher2 = pattern16.matcher(plaincharset);
	    	int flag2=0;
	    	String countrange = "";
	    	while ( matcher2.find()){
	    		flag6 = 1;
	    		i=matcher2.start();
	    		j=matcher2.end();
	    		String extreme = plaincharset.substring(i,j);
    			i = 0;
    			j = extreme.length();
        		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
            	Matcher matcher1 = pattern20.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(q-2)=='–' | extreme.charAt(q-2)=='-')
            			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-2)+"\"");
            		else
            			countrange = countrange.concat(" min_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		if(extreme.contains("#"))
        			i = extreme.indexOf("#")+1;
        		j = extreme.length();
        		Pattern pattern21 = Pattern.compile("\\[[±\\d\\.\\s\\+\\–\\-]*[\\–\\-]{1}[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern21.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		j = p;
            		if(extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-')
            			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(p+2,q-1)+"\"");
            		else
            			countrange = countrange.concat(" max_extreme_count_"+countct+"=\""+extreme.substring(p+1,q-1)+"\"");
            	}
        		matcher1.reset();
        		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern22 = Pattern.compile("[\\[\\]]+");
        			matcher1 = pattern22.matcher(extract);
        			extract = matcher1.replaceAll("");
        			matcher1.reset();
        			countrange = countrange.concat(" min_count_"+countct+"=\""+extract.substring(0, extract.indexOf('-'))+"\" max_count_"+countct+"=\""+extract.substring(extract.indexOf('-')+1,extract.length())+"\"");
        			countct+=1;
        		}
        		else{
        			if(flag2==0)
        				innertagstate=innertagstate.concat(" "+"count=\""+extreme.subSequence(i,j).toString());
        			else
        				innertagstate=innertagstate.concat(","+extreme.subSequence(i,j).toString());
        			flag2=1;
        		}
	    	}
	    	if(flag2==1)
	    		innertagstate=innertagstate.concat("\"");
	    	innertagstate = innertagstate.concat(countrange);
	    	matcher2.reset();                	
			Pattern pattern7 = Pattern.compile("[{][\\w±\\+\\–\\-\\.:=/\\_]+[}]");
	    	matcher2 = pattern7.matcher(state);
	    	String str3 = "";
	    	//int flag3=0;
	    	while ( matcher2.find()){
	    		flag6=1;
	    		int flag5=0;
	    		String first = "";
	    		String chstate = "";
	    		i=matcher2.start()+1;
	    		j=matcher2.end()-1;
	    		str3=state.subSequence(i,j).toString();
	    		if(str3.contains("-")|str3.contains("–")){
	    			first = str3.substring(0, str3.indexOf("-"));
	    			str3=str3.substring(str3.indexOf("-")+1|str3.indexOf("–")+1, str3.length());
	    			flag5=1;
	    		}
	    			ResultSet rs1 = stmt1.executeQuery("select category from fnaglossaryfixed where term='"+str3+"'");
	    			if(rs1.next()){
	    				int flag4=0;
	    				chstate=rs1.getString("category");
	    				if(chstate.contains("/")){
	    					String [] terms = chstate.split("/");
	    					chstate=terms[0];
	            			for(int t=1;t<terms.length;t++)
	            				chstate=chstate.concat("_or_"+terms[t]);  
	    				}
	    				StringBuffer sb = new StringBuffer();
						Pattern pattern8 = Pattern.compile(chstate+"=\"[\\w±\\+\\–\\-\\.:/\\_;x´\\s,xX\\×]+\"");
						Matcher matcher3 = pattern8.matcher(innertagstate);
						while ( matcher3.find()){
							int q=matcher3.start();
							int r=matcher3.end();
							if(flag5==1)
								matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+first+"-"+str3+"\"");
	        				else
	        					matcher3.appendReplacement(sb, innertagstate.subSequence(q,r-1)+","+str3+"\"");
							flag4=1;
						}
						if(flag4==1){
							matcher3.appendTail(sb);
							innertagstate=sb.toString();
						}
						else{
							if(flag5==1)
								innertagstate=innertagstate.concat(" "+chstate+"=\""+first+"-"+str3+"\"");
							else
								innertagstate=innertagstate.concat(" "+chstate+"=\""+str3+"\"");
						}
						matcher3.reset();
	    			}                			
	    	}
			matcher2.reset();
		}
		catch (Exception e)
        {
    		System.err.println(e);
        }
		return(innertagstate);
	}
          	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//new ParseComplexseg("fnav19_benchmark");
		new ParseComplexseg_notinuse("annotationevaluation_heuristics");
	}

}
