 /* $Id: CharStateHandler.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@SuppressWarnings("unused")
public class CharStateHandler {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "root";
	static protected ArrayList<String> adverbs = new ArrayList<String>();
	static protected ArrayList<String> notadverbs = new ArrayList<String>();
	static protected String glosstable = null;
	
	public CharStateHandler(String database) {
		CharStateHandler.database = database;
		if(database.endsWith("fna")){
			CharStateHandler.glosstable = "fnaglossaryfixed";
		}else if(database.endsWith("treatise")){
			CharStateHandler.glosstable = "treatisehglossaryfixed";
		} 
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @param plaincharset : styles 2[10] mm diam.
	 * @param state: <styles> 2[10] mm {diam}.
	 * @return: characters marked up in XML format <character name="" value="">
	 */
	public String characterstate(String plaincharset, String state){
		
		String innertagstate = "";
		try{
			Statement stmt2 = conn.createStatement();
			int i,j;
			plaincharset = plaincharset.replaceAll("\\([\\s]?|\\[[\\s]?", "[");
			plaincharset = plaincharset.replaceAll("[\\s]?\\)|[\\s]?\\]", "]");
			//System.out.println("plain:"+plaincharset);
			//System.out.println("state:"+state);
			Pattern pattern19 = Pattern.compile("[±]?[\\[]?[\\d\\s\\.]+[\\]]?[\\s]?[\\[]?[\\–\\-]+[\\]]?[\\s]?[\\[]?[\\d\\s\\.]+[+]?[\\]]?[\\s]?[dcmµ]?[m]?[\\s]?[xX\\×]+[\\s]?[\\[]?[\\d\\s\\.]+[\\]]?[\\s]?[\\[]?[\\–\\-]+[\\]]?[\\s]?[\\[]?[\\d\\s\\.]+[+]?[\\]]?[\\s]?[dcmµ]?m");
        	Matcher matcher2 = pattern19.matcher(plaincharset);
        	while ( matcher2.find()){
        		if(plaincharset.charAt(matcher2.start())==' '){
        			i=matcher2.start()+1;
        		}
        		else{
        			i=matcher2.start();
        		}
        		j=matcher2.end();
        		String match = plaincharset.substring(i, j);
        		Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m");
            	Matcher matcher3 = pattern18.matcher(match);
            	String[] unit = new String[2];
        		int num = 0;
            	while ( matcher3.find()){
            		unit[num] = match.substring(matcher3.start(), matcher3.end());
            		num++;
            	}
            	match = matcher3.replaceAll("#");
            	matcher3.reset();
        		int en = match.indexOf('-');
        		int lasten = match.lastIndexOf('-');
        		if (match.substring(en+1, match.indexOf('×',en+1)).contains("+"))
        			innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"length\" from=\""+match.substring(0,en).trim()+"\" to=\""+match.substring(en+1, match.indexOf('+',en+1)).trim()+"\" upper_restricted=\"false\" unit=\""+unit[0].trim()+"\"/>");
        		else
        			innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"length\" from=\""+match.substring(0,en).trim()+"\" to=\""+match.substring(en+1, match.indexOf('×',en+1)).trim()+"\" unit=\""+unit[0].trim()+"\"/>");
        		if (num>1){
        			if (match.substring(lasten+1, match.indexOf('#',lasten+1)).contains("+"))
        				innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('×')+2,lasten).trim()+"\" to=\""+match.substring(lasten+1, match.indexOf('+',lasten+1)).trim()+"\" upper_restricted=\"false\" unit=\""+unit[1].trim()+"\"/>");
        			else
        				innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('×')+2,lasten).trim()+"\" to=\""+match.substring(lasten+1, match.indexOf('#',lasten+1)).trim()+"\" unit=\""+unit[1].trim()+"\"/>");
        		}
        		else{
        			if (match.substring(lasten+1, match.indexOf('#',lasten+1)).contains("+"))
        				innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('×')+2,lasten).trim()+"\" to=\""+match.substring(lasten+1, match.indexOf('+',lasten+1)).trim()+"\" upper_restricted=\"false\" unit=\""+unit[0].trim()+"\"/>");
        			else
        				innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"width\" from=\""+match.substring(match.indexOf('×')+2,lasten).trim()+"\" to=\""+match.substring(lasten+1, match.indexOf('#',lasten+1)).trim()+"\" unit=\""+unit[0].trim()+"\"/>");
        		}
        	}
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	//System.out.println("plaincharset1:"+plaincharset);
        	Pattern pattern24 = Pattern.compile("l/w[\\s]?=[\\d\\.\\s\\+\\–\\-]+");
        	matcher2 = pattern24.matcher(plaincharset);
        	while ( matcher2.find()){
        		if(plaincharset.charAt(matcher2.start())==' '){
        			i=matcher2.start()+1;
        		}
        		else{
        			i=matcher2.start();
        		}
        		j=matcher2.end();
        		String match = plaincharset.substring(i, j);
        		int en = match.indexOf('-');
        		if (match.contains("+"))
        			innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"l_w_ratio\" from=\""+match.substring(match.indexOf('=')+2,en).trim()+"\" to=\""+match.substring(en+1, match.indexOf('+',en+1)).trim()+"\" upper_restricted=\"false\"/>");
        		else
        			innertagstate=innertagstate.concat("<character char_type=\"range_value\" name=\"l_w_ratio\" from=\""+match.substring(match.indexOf('=')+2,en).trim()+"\" to=\""+match.substring(en+1, match.indexOf(' ',en+1)).trim()+"\"/>");
        	}
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	int sizect = 0;
			Pattern pattern13 = Pattern.compile("[xX\\×±\\d\\[\\]\\–\\-\\.\\s\\+]+[\\s]?[dcmµ]?m(?![\\w])(([\\s]diam)?([\\s]wide)?)");
        	matcher2 = pattern13.matcher(plaincharset);
        	String toval="";
        	String fromval="";
        	while ( matcher2.find()){
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
            			innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2).trim()+"\" to=\"\"/>");
            		else
            			innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
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
            		if (extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-'){
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			else
            				innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-1).trim()+"\"/>");
            		}
            		else{
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			else
            				innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
            		}
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		j = extreme.length();
        		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if (extreme.charAt(q-2)=='+')
            			innertagstate = innertagstate.concat("<character name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
        			else
        				innertagstate = innertagstate.concat("<character name=\"atypical_size\" value=\""+extreme.substring(p+1,q-1).trim()+"\"/>");
            	}
            	extreme = matcher1.replaceAll("#");
            	matcher1.reset();
            	j = extreme.length();
        		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m(([\\s]diam)?([\\s]wide)?)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	String unit="";
                	
                	/*if ( matcher3.find()){
                		unit = extract.substring(matcher3.start(), matcher3.end());
                	}
                	if(unit.length()>0) extract = matcher3.replaceAll("#");
                	if(extract.indexOf("#")<0) extract +="#";*/
                	if ( matcher3.find()){
                		unit = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");                	
                	matcher3.reset();
                	String from = extract.substring(0, extract.indexOf('-')).trim();
                	String to = extract.substring(extract.indexOf('-')+1,extract.indexOf('#')).trim();
                	boolean upperrestricted = ! to.endsWith("+");
                	to = to.replaceFirst("\\+$", "").trim();
                	innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"size\" from=\""+from+"\" from_unit=\""+unit.trim()+"\" to=\""+to+"\" to_unit=\""+unit.trim()+"\" upper_restricted=\""+upperrestricted+"\"/>");
        			toval = extract.substring(0, extract.indexOf('-'));
        			fromval = extract.substring(extract.indexOf('-')+1,extract.indexOf('#'));
                	sizect+=1;
        		}
        		else{
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[dcmµ]?m(([\\s]diam)?([\\s]wide)?)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	String unit="";
                	if ( matcher3.find()){
                		unit = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
        			innertagstate = innertagstate.concat("<character name=\"size\" value=\""+extract.substring(0,extract.indexOf('#')).trim()+"\" unit=\""+unit.trim()+"\"/>");
        			toval = extract.substring(0,extract.indexOf('#'));
        			fromval = extract.substring(0,extract.indexOf('#'));
        		}
        		
        		StringBuffer sb = new StringBuffer();
				Pattern pattern25 = Pattern.compile("to=\"\"");
				matcher1 = pattern25.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb, "to=\""+toval.trim()+"\"");
				}
				matcher1.appendTail(sb);
				innertagstate=sb.toString();
				matcher1.reset();
				StringBuffer sb1 = new StringBuffer();
				Pattern pattern26 = Pattern.compile("from=\"\"");
				matcher1 = pattern26.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb1, "from=\""+fromval.trim()+"\"");
				}
				matcher1.appendTail(sb1);
				innertagstate=sb1.toString();
				matcher1.reset();
        	}
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        	//System.out.println("plaincharset2:"+plaincharset);
        	
        	
        	
        	
        	
        	
        	Pattern pattern14 = Pattern.compile("[±\\d\\[\\]\\–\\-\\./\\s]+[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
        	matcher2 = pattern14.matcher(plaincharset);
        	toval="";
        	fromval="";
        	while ( matcher2.find()){
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
            			innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2).trim()+"\" to=\"\"/>");
            		else
            			innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
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
            		if (extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-'){
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			else
            				innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\"\" to=\""+extreme.substring(p+2,q-1).trim()+"\"/>");
            		}
            		else{
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			else
            				innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
            		}
            	}
            	extreme = matcher1.replaceAll("#");
        		matcher1.reset();
        		j = extreme.length();
        		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if (extreme.charAt(q-2)=='+')
            			innertagstate = innertagstate.concat("<character char_type=\"relative_value\" name=\"atypical_size\" from=\""+extreme.substring(p+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
        			else
        				innertagstate = innertagstate.concat("<character char_type=\"relative_value\" name=\"atypical_size\" value=\""+extreme.substring(p+1,q-1).trim()+"\"/>");
            	}
            	extreme = matcher1.replaceAll("#");
            	matcher1.reset();
            	j = extreme.length();      	
            	if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	String relative="";
                	if ( matcher3.find()){
                		relative = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
                	innertagstate = innertagstate.concat("<character char_type=\"relative_range_value\" name=\"size\" from=\""+extract.substring(0, extract.indexOf('-')).trim()+"\" to=\""+extract.substring(extract.indexOf('-')+1,extract.indexOf('#')).trim()+"\" relative_constraint=\""+relative.trim()+"\"/>");
        			toval = extract.substring(0, extract.indexOf('-'));
        			fromval = extract.substring(extract.indexOf('-')+1,extract.indexOf('#'));
                	sizect+=1;
        		}
        		else{
        			String extract = extreme.substring(i,j);
        			Pattern pattern18 = Pattern.compile("[\\s]?[\\–\\-]?(% of [\\w]+ length|height of [\\w]+|times as [\\w]+ as [\\w]+|total length|their length|(times)?[\\s]?length of [\\w]+)");
                	Matcher matcher3 = pattern18.matcher(extract);
                	String relative="";
                	if ( matcher3.find()){
                		relative = extract.substring(matcher3.start(), matcher3.end());
                	}
                	extract = matcher3.replaceAll("#");
                	matcher3.reset();
        			innertagstate = innertagstate.concat("<character char_type=\"relative_value\" name=\"size\" value=\""+extract.substring(0,extract.indexOf('#')).trim()+"\" relative_constraint=\""+relative.trim()+"\"/>");
        			toval = extract.substring(0,extract.indexOf('#'));
        			fromval = extract.substring(0,extract.indexOf('#'));
        		}
        		
        		StringBuffer sb = new StringBuffer();
				Pattern pattern25 = Pattern.compile("to=\"\"");
				matcher1 = pattern25.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb, "to=\""+toval.trim()+"\"");
				}
				matcher1.appendTail(sb);
				innertagstate=sb.toString();
				matcher1.reset();
				StringBuffer sb1 = new StringBuffer();
				Pattern pattern26 = Pattern.compile("from=\"\"");
				matcher1 = pattern26.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb1, "from=\""+fromval.trim()+"\"");
				}
				matcher1.appendTail(sb1);
				innertagstate=sb1.toString();
				matcher1.reset();
        	}
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();
        
        	int countct = 0;
        	//Pattern pattern15 = Pattern.compile("([\\[]?[±]?[\\d]+[\\]]?[\\s]?[\\[]?[\\–\\-][\\]]?[\\s]?[\\[]?[\\d]+[+]?[\\]]?|[\\[]?[±]?[\\d]+[+]?[\\]]?[\\s]?)[\\–\\–\\-]+[a-zA-Z]+");
        	Pattern pattern15 = Pattern.compile("([\\[]?[±]?[\\d]+[\\]]?[\\s]?[\\[]?[\\–\\-][\\]]?[\\s]?[\\[]?[\\d]+[+]?[\\]]?|[\\[]?[±]?[\\d]+[+]?[\\]]?[\\s]?)[\\–\\–\\-]+[a-zA-Z]+");
        	matcher2 = pattern15.matcher(plaincharset);
        	plaincharset = matcher2.replaceAll("#");
        	matcher2.reset();     	
        	//Pattern pattern16 = Pattern.compile("(?<!([/][\\s]?))([\\[]?[±]?[\\d]+[\\]]?[\\s]?[\\[]?[\\–\\-][\\]]?[\\s]?[\\[]?[\\d]+[+]?[\\]]?[\\s]?([\\[]?[\\–\\-]?[\\]]?[\\s]?[\\[]?[\\d]+[+]?[\\]]?)*|[±]?[\\d]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\–\\-]?% of [\\w]+ length|[\\s]?[\\–\\-]?height of [\\w]+|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[dcmµ]?m))");
        	//add \\. to allow 0.5-0.6+
        	//TODO: match also just a period . 
        	Pattern pattern16 = Pattern.compile("(?<!([/][\\s]?))([\\[]?[±]?[\\d\\./]+[\\]]?[\\s]?[\\[]?[\\–\\-][\\]]?[\\s]?[\\[]?[\\d\\./]+[+]?[\\]]?[\\s]?([\\[]?[\\–\\-]?[\\]]?[\\s]?[\\[]?[\\d\\./]+[+]?[\\]]?)*|[±]?[\\d\\./]+[+]?)(?!([\\s]?[n/]|[\\s]?[\\–\\-]?% of [\\w]+ length|[\\s]?[\\–\\-]?height of [\\w]+|[\\s]?[\\–\\-]?times|[\\s]?[\\–\\-]?total length|[\\s]?[\\–\\-]?their length|[\\s]?[\\–\\-]?(times)?[\\s]?length of|[\\s]?[dcmµ]?m))");
        	matcher2 = pattern16.matcher(plaincharset);
        	while ( matcher2.find()){
        		i=matcher2.start();
        		j=matcher2.end();
        		String extreme = plaincharset.substring(i,j);
        		if(!extreme.matches(".*\\d.*")) continue;
    			i = 0;
    			j = extreme.length();
        		Pattern pattern20 = Pattern.compile("\\[[±\\d\\.\\s\\+]+[\\–\\-]{1}[±\\d\\.\\s\\+\\–\\-]*\\]");
            	Matcher matcher1 = pattern20.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		if(extreme.charAt(q-2)=='–' | extreme.charAt(q-2)=='-')
            			innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,q-2).trim()+"\" to=\"\"/>");
            		else
            			innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
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
            		if (extreme.charAt(p+1)=='–' | extreme.charAt(p+1)=='-'){
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\"\" to=\""+extreme.substring(p+2,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			else
            				innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\"\" to=\""+extreme.substring(p+2,q-1).trim()+"\"/>");
            		}
            		else{
            			if (extreme.charAt(q-2)=='+')
            				innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
            			else
            				innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"atypical_count\" from=\""+extreme.substring(p+1,extreme.indexOf("-",p+1)).trim()+"\" to=\""+extreme.substring(extreme.indexOf("-",p+1)+1,q-1).trim()+"\"/>");
            		}
            	}
        		matcher1.reset();
        		Pattern pattern23 = Pattern.compile("\\[[±\\d\\.\\s\\+]+\\]");
            	matcher1 = pattern23.matcher(extreme);
            	if ( matcher1.find()){
            		int p = matcher1.start();
            		int q = matcher1.end();
            		j = p;
            		if (extreme.charAt(q-2)=='+')
            			innertagstate = innertagstate.concat("<character name=\"atypical_count\" from=\""+extreme.substring(p+1,q-2).trim()+"\" upper_restricted=\"false\"/>");
        			else
        				innertagstate = innertagstate.concat("<character name=\"atypical_count\" value=\""+extreme.substring(p+1,q-1).trim()+"\"/>");
            	}
            	matcher1.reset();
        		if(extreme.substring(i,j).contains("–")|extreme.substring(i,j).contains("-") && !extreme.substring(i,j).contains("×") && !extreme.substring(i,j).contains("x") && !extreme.substring(i,j).contains("X")){
        			String extract = extreme.substring(i,j);
        			Pattern pattern22 = Pattern.compile("[\\[\\]]+");
        			matcher1 = pattern22.matcher(extract);
        			extract = matcher1.replaceAll("");
        			matcher1.reset();
                	innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\"count\" from=\""+extract.substring(0, extract.indexOf('-')).trim()+"\" to=\""+extract.substring(extract.indexOf('-')+1,extract.length()).trim()+"\"/>");
        			toval = extract.substring(0, extract.indexOf('-'));
        			fromval = extract.substring(extract.indexOf('-')+1,extract.length());
        			countct+=1;
        		}
        		else{
        			String extract = extreme.substring(i,j);
        			innertagstate = innertagstate.concat("<character name=\"count\" value=\""+extract.trim()+"\"/>");
        			toval = extract;
        			fromval = extract;
        		}
        		
        		StringBuffer sb = new StringBuffer();
				Pattern pattern25 = Pattern.compile("to=\"\"");
				matcher1 = pattern25.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb, "to=\""+toval.trim()+"\"");
				}
				matcher1.appendTail(sb);
				innertagstate=sb.toString();
				matcher1.reset();
				StringBuffer sb1 = new StringBuffer();
				Pattern pattern26 = Pattern.compile("from=\"\"");
				matcher1 = pattern26.matcher(innertagstate);
				while ( matcher1.find()){
					matcher1.appendReplacement(sb1, "from=\""+fromval.trim()+"\"");
				}
				matcher1.appendTail(sb1);
				innertagstate=sb1.toString();
				matcher1.reset();
        	}
        	matcher2.reset();   
        	        	
        	
        	
        	
        	Pattern pattern27 = Pattern.compile("([\\[]?[{])([\\w±\\+\\–\\-\\.:=/\\_]+)([}][\\]]?)[\\s]to[\\s]([\\[]?[{])([\\w±\\+\\–\\-\\.:=/\\_]+)([}][\\]]?)");
        	matcher2 = pattern27.matcher(state);
        	String state1 = "";
        	String state2 = "";
        	String resstate1 = "";
        	String resstate2 = "";
        	while (matcher2.find()){
        		String chstate1 = "", chstate2 = "";
        		state1=matcher2.group(2);
        		resstate1 = state1;
        		state2=matcher2.group(5);
        		resstate2 = state2;
        		resstate1 = resstate1.replaceAll("\\_", " ");
        		resstate2 = resstate2.replaceAll("\\_", " ");
        		if(state1.contains("-")|state1.contains("–")){
        			state1=state1.substring(state1.indexOf("-")+1|state1.indexOf("–")+1, state1.length());
        		}
        		if(state1.contains("_")){
        			state1=state1.substring(state1.indexOf("_")+1);
        		}
        		if(state2.contains("-")|state2.contains("–")){
        			state2=state2.substring(state2.indexOf("-")+1|state2.indexOf("–")+1, state2.length());
        		}
        		if(state2.contains("_")){
        			state2=state2.substring(state2.indexOf("_")+1);
        		}
        		ResultSet rs1 = stmt2.executeQuery("select category from "+CharStateHandler.glosstable+" where term='"+state1+"'");
        		if(rs1.next()){
        			chstate1=rs1.getString("category");
        			if(chstate1.contains("/")){
        				String [] terms = chstate1.split("/");
        				chstate1=terms[0];
                		for(int t=1;t<terms.length;t++)
                			chstate1=chstate1.concat("_or_"+terms[t]);  
        			}
        		}
        		ResultSet rs2 = stmt2.executeQuery("select category from "+CharStateHandler.glosstable+" where term='"+state2+"'");
        		if(rs2.next()){
        			chstate2=rs2.getString("category");
        			if(chstate2.contains("/")){
        				String [] terms = chstate2.split("/");
        				chstate2=terms[0];
                		for(int t=1;t<terms.length;t++)
                			chstate2=chstate2.concat("_or_"+terms[t]);  
        			}
        		}
        		if((chstate1.contains(chstate2)|chstate2.contains(chstate1)) && chstate1.compareTo("")!=0 && chstate2.compareTo("")!=0){
        			innertagstate = innertagstate.concat("<character char_type=\"range_value\" name=\""+chstate1+"\" from=\""+resstate1.trim()+"\" to=\""+resstate2.trim()+"\"/>");
        		}
        	}    		
        	matcher2.reset();  
        	      	
        	
        	
        	        	
			Pattern pattern7 = Pattern.compile("([\\[]?[{])([\\w±\\+\\–\\-\\.:=/\\_]+)([}][\\]]?)");
        	matcher2 = pattern7.matcher(state);
        	String str3 = "";
        	while (matcher2.find()){
        		int flag5=0;
        		int flag6=0;
        		String first = "";
        		String chstate = "";
        		String resstate = "";
        		i=matcher2.start();
        		j=matcher2.end();
        		str3=matcher2.group(2);//state.subSequence(i,j).toString();
        		resstate=str3;
        		resstate=resstate.replaceAll("\\_", " ");
        		if(str3.contains("-")|str3.contains("–")){
        			first = str3.substring(0, str3.indexOf("-"));
        			str3=str3.substring(str3.indexOf("-")+1|str3.indexOf("–")+1, str3.length());
        			flag5=1;
        		}
        		if(str3.contains("_")){
        			str3=str3.substring(str3.indexOf("_")+1);
        			flag6=1;
        		}
        		ResultSet rs1 = stmt2.executeQuery("select category from "+CharStateHandler.glosstable+" where term='"+str3+"'");
        		if(rs1.next()){
        			chstate=rs1.getString("category");
        			if(chstate.contains("/")){
        				String [] terms = chstate.split("/");
        				chstate=terms[0];
                		for(int t=1;t<terms.length;t++)
                			chstate=chstate.concat("_or_"+terms[t]);  
        			}
        			if(state.indexOf(i)=='[' && state.indexOf(j-1)==']'){
        				if(flag5==1)
        					innertagstate = innertagstate.concat("<character name=\"atypical_"+chstate+"\" value=\""+first.trim()+"-"+str3.trim()+"\"/>");
        				else if(flag6==1)
        					innertagstate = innertagstate.concat("<character name=\"atypical_"+chstate+"\" value=\""+resstate.trim()+"\"/>");
        				else
        					innertagstate = innertagstate.concat("<character name=\"atypical_"+chstate+"\" value=\""+str3.trim()+"\"/>");
        			}
        			else{
        				if(flag5==1)
        					innertagstate = innertagstate.concat("<character name=\""+chstate+"\" value=\""+first.trim()+"-"+str3.trim()+"\"/>");
        				else if(flag6==1)
        					innertagstate = innertagstate.concat("<character name=\""+chstate+"\" value=\""+resstate.trim()+"\"/>");
        				else
        					innertagstate = innertagstate.concat("<character name=\""+chstate+"\" value=\""+str3.trim()+"\"/>");
        			}
        		}                			
        	}    		
        	matcher2.reset(); 
        	
        	
        	
        	
        	Pattern pattern28 = Pattern.compile("([\\[]?[{])([\\w±\\+\\–\\-\\.:=/\\_]+)([}][\\]]?)[\\s](with[\\s])?([\\[]?[{])([\\w±\\+\\–\\-\\.:=/\\_]+)([}][\\]]?)([\\s]to[\\s]([\\[]?[{])([\\w±\\+\\–\\-\\.:=/\\_]+)([}][\\]]?)|[\\s]or[\\s]([\\[]?[{])([\\w±\\+\\–\\-\\.:=/\\_]+)([}][\\]]?))?");
    		matcher2 = pattern28.matcher(state);
    		while (matcher2.find()){
    			String mod = matcher2.group(2);
    			//System.out.println(mod);
    			//System.out.println(matcher2.group(6));
    			//System.out.println(matcher2.group(10));
    			//System.out.println(matcher2.group(13));
    			if(Utilities.isAdv(mod, adverbs, notadverbs)){
    				String chstate = matcher2.group(6).replaceAll("\\_", " ");
    				StringBuffer sb = new StringBuffer();
    				Pattern pattern29 = Pattern.compile("value=\""+chstate+"\"");
    				Matcher matcher1 = pattern29.matcher(innertagstate);
    				String value="";
    				while ( matcher1.find()){
    					matcher1.appendReplacement(sb, "modifier=\""+mod+"\" "+matcher1.group());
    					value = matcher1.group();
    				}
    				matcher1.appendTail(sb);
    				innertagstate=sb.toString();
					if(value.length()> 0) innertagstate= combineModifiers(innertagstate, value);
    				matcher1.reset();
    				if(matcher2.group(10)!=null){
    					StringBuffer sb1 = new StringBuffer();
    					Pattern pattern30 = Pattern.compile("from=\""+chstate+"\"");
    					matcher1 = pattern30.matcher(innertagstate);
    					value="";
    					while ( matcher1.find()){
    						matcher1.appendReplacement(sb1, "modifier=\""+mod+"\" "+matcher1.group());
    						value = matcher1.group();
    					}
    					matcher1.appendTail(sb1);
    					innertagstate=sb1.toString();
    					if(value.length()> 0) innertagstate= combineModifiers(innertagstate, value);
    					matcher1.reset();
    					
    					chstate = matcher2.group(10).replaceAll("\\_", " ");
    					StringBuffer sb2 = new StringBuffer();
    					Pattern pattern31 = Pattern.compile("value=\""+chstate+"\"");
    					matcher1 = pattern31.matcher(innertagstate);
    					value="";
    					while ( matcher1.find()){
    						matcher1.appendReplacement(sb2, "modifier=\""+mod+"\" "+matcher1.group());
    						value = matcher1.group();
    					}
    					matcher1.appendTail(sb2);
    					innertagstate=sb2.toString();
    					if(value.length()> 0) innertagstate= combineModifiers(innertagstate, value);
    					matcher1.reset();
    				}
    				if(matcher2.group(13)!=null){
    					chstate = matcher2.group(13).replaceAll("\\_", " ");
    					StringBuffer sb3 = new StringBuffer();
    					Pattern pattern32 = Pattern.compile("value=\""+chstate+"\"");
    					matcher1 = pattern32.matcher(innertagstate);
    					value = "";
    					while ( matcher1.find()){
    						matcher1.appendReplacement(sb3, "modifier=\""+mod+"\" "+matcher1.group());
    						value = matcher1.group();
    					}
    					matcher1.appendTail(sb3);
    					innertagstate=sb3.toString();
    					if(value.length()> 0) innertagstate = combineModifiers(innertagstate, value);
    					matcher1.reset();
    				}
    			}
    		}
    		matcher2.reset();
        	
		}
		catch (Exception e)
        {
    		System.err.println(e);
    		e.printStackTrace();
        }
		return(innertagstate.replaceAll("\\s+\\.\\s+", ".")); //turn 4 . 5 to 4.5
	}

	
	/**
	 * modifier="a" modifier="b" value="c" 
	 * @param element: <character name="n" modifier="a" modifier="b" value="c"/> 
	 * @param value: value="c"
	 * @return: <character name="n" modifier="a;b" value="c"/> 
	 */
	private static String combineModifiers(String text, String value){
		
		String rtext = "";
		Pattern p = Pattern.compile("(.*?)(<character [^>]*? "+value+".*?/>)(.*)");
		Matcher m0 = p.matcher(text);
		while(m0.matches()){
			rtext +=m0.group(1);
			rtext +=combineModifiers4Element(m0.group(2));
			text = m0.group(3);
			m0 = p.matcher(text);
		}
		return rtext+text;
	}
	
	private static String combineModifiers4Element(String element) {
		Pattern ptn = Pattern.compile("(.*? )(modifier=\\S+)(['\"].*)");
		Matcher m = ptn.matcher(element);
		String result = "";
		String modifiers = "";
		while(m.matches()){
			result +=m.group(1).replaceFirst("^['\"]", "");
			modifiers += m.group(2).replaceAll("modifier=", "")+";";
			element = m.group(3);
			m = ptn.matcher(element);
		}
		result += element.replaceFirst("^['\"]", "");
		modifiers = "modifier=\""+modifiers.replaceAll("['\"]", "").replaceAll("\\W+$", "").trim()+"\"";
		result = result.replaceFirst(" value", modifiers+" value").replaceAll("\\s+", " ");
		return result;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		CharStateHandler ch = new CharStateHandler("annotationevaluation_heuristics_fna");
		// TODO Auto-generated method stub
		String str1 = "0.5-0.6+"; 		String str2 = "0.5-0.6+";	
		System.out.println(ch.characterstate(str1, str2));
		
		str1 = "0.5-0.6+ cm"; 	 str2 = "0.5-0.6+ cm";	
		System.out.println(ch.characterstate(str1, str2));
		
		str1 = "1/3-1/2"; 	 str2 = "1/3-1/2";	
		System.out.println(ch.characterstate(str1, str2));
		
		str1 = "[5-]8+"; 	 str2 = "[5-]8+";	
		System.out.println(ch.characterstate(str1, str2));
		
		str1 = "outer and mid phyllaries acute"; 
		str2 = "{outer} and <{mid}> <phyllaries> {acute}";
		System.out.println(ch.characterstate(str1, str2));
		
	}

}
