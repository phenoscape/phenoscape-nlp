 /* $Id: POSTagger4StanfordParser.java 988 2011-09-23 16:44:53Z hong1.cui $ */
/**
 * 
 */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fna.parsing.ApplicationUtilities;
import fna.parsing.state.SentenceOrganStateMarker;
import conceptmapping.*;


/**
 * @author hongcui
 *
 */
@SuppressWarnings({  "unused","static-access" })
public class POSTagger4StanfordParser {
	static protected Connection conn = null;
	static protected String username = "root";
	static protected String password = "root";
	private ArrayList<String> chunkedtokens = null;
	private ArrayList<String> charactertokensReversed = null;
	public static Hashtable<String, String> characterhash = new Hashtable<String, String>();
	private boolean printList = false;
	private String tableprefix = null;
	private String glosstable = null;
	public static String comprepstring = SentenceOrganStateMarker.compoundprep.replaceAll(" ", "-");
	private static Pattern compreppattern = Pattern.compile("\\{?("+comprepstring+")\\}?");
	//private Pattern viewptn = Pattern.compile( "(.*?\\b)(in\\s+[a-z_<>{} -]+\\s+[<{]*view[}>]*)(\\s.*)"); to match in dorsal view
	//private Pattern viewptn = Pattern.compile( "(.*?\\b)(in\\s+[a-z_<>{} -]*\\s*[<{]*(?:view|profile)[}>]*)(\\s.*)"); //to match in dorsal view and in profile
	private Pattern viewptn = Pattern.compile( "(.*?\\b)(in\\s+[a-z_<>{} -]*\\s*[<{]*(?:view|profile)[}>]*)(.*)"); //to match in dorsal view and in profile
	private String countp = "more|fewer|less|\\d+";
	private Pattern countptn = Pattern.compile("((?:^| |\\{)(?:"+countp+")\\}? (?:or|to) \\{?(?:"+countp+")(?:\\}| |$))");
	private String romandigits = "i|v|x"; 
	private Pattern positionptn = Pattern.compile("(<(\\S+?)> [<{]?(?:\\d|"+romandigits+")+\\b[}>]?(?![-\\d]*%)(?:\\s*(and|-)\\s*[<{]?(?:\\d|"+romandigits+")+\\b[}>]?(?!%))?)");
	private ArrayList<String> prepphrases = new ArrayList<String>();
	private String positions = ""; //initialized with two values that are not positions for convenience
	private Pattern positionptn2;
	private String characterptn;
	Pattern pof1 = Pattern.compile("(.*?)\\{?("+this.characterptn+")\\}? of (.*?<\\w+>.*)");
	Pattern p1 = Pattern.compile("(.*<\\w+> )\\{?("+this.characterptn+")\\}?");
	Pattern pof2 = Pattern.compile("\\{?("+this.characterptn+")\\}? of ((?:<?\\{?("+this.positions+")\\}?>? |<\\w+> |of )+)(.*)");
	Pattern p2 = Pattern.compile("((?:<?\\{?("+this.positions+")\\}?>? |<\\w+> |of )+)\\{?("+this.characterptn+")\\}?(.*)");
	Pattern pof3=Pattern.compile("((?:<?\\{?("+this.positions+")\\}?>? |<\\w+> |of )+)");
	Pattern p3=Pattern.compile("\\{?("+this.characterptn+")\\}?");
	String structs = "((?:<?\\{?("+this.positions+")\\}?>? |<\\w+> |of )+)";
	private boolean printRelative=true;
	private boolean printfromto = true;
	

	/**
	 * 
	 */
	public POSTagger4StanfordParser(Connection conn, String tableprefix, String glosstable, String characterptn) {
		this.conn = conn;
		this.tableprefix = tableprefix;
		this.glosstable = glosstable;
		this.characterptn = characterptn;
		try{
			Statement stmt = this.conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct phrase from "+tableprefix+"_prepphrases");
			while(rs.next()){
				prepphrases.add(rs.getString("phrase"));
			}
			
			rs= stmt.executeQuery("select distinct term from "+tableprefix+"_term_category where category='position' union select distinct term from "+this.glosstable+" where category='position'");
			while(rs.next()){
				positions += rs.getString(1).replaceAll("\\(.*?\\)", "")+"|";
			}
			positions = positions.replaceFirst("\\|$", "");
			positionptn2 = Pattern.compile("(.*?)([<{]?\\b(?:"+this.positions+")\\b[}>]?\\s+to)(\\b.*)");		
			

		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 		//insert our POS tags to segments (simple or complex: new segmentation)
			 //output POSed segments to a database table and to the posed file	
			  * str is markedsent

	 */
		protected String POSTag(String str, String src, String type) throws Exception{
			boolean containsArea = false;
			String strcp = str;
			str = StanfordParser.normalizeSpacesRoundNumbers(str);
			
			/*str = str.replaceAll("\\b(?<=\\d+) \\. (?=\\d+)\\b", "."); //2 . 5 =>2.5
			str = str.replaceAll("(?<=\\d)\\s+/\\s+(?=\\d)", "/"); // 1 / 2 => 1/2
			str = str.replaceAll("(?<=\\d)\\s+[–-—]\\s+(?=\\d)", "-"); // 1 - 2 => 1-2*/
			/*if(str.indexOf(" -{")>=0){//1–2-{pinnately} or -{palmately} {lobed} => {1–2-pinnately-or-palmately} {lobed}
				str = str.replaceAll("\\s+or\\s+-\\{", "-or-").replaceAll("\\s+to\\s+-\\{", "-to-").replaceAll("\\s+-\\{", "-{");
			}*/

			//if(str.matches(".*?-(or|to)\\b.*") || str.matches(".*?\\b(or|to)-.*") ){//1–2-{pinnately} or-{palmately} {lobed} => {1–2-pinnately-or-palmately} {lobed}
            //to avoid turning 'relative-to its {length}'  to 'relative-to-its {length}'
			if(str.matches(".*?-(or|to)\\s+[^a-z<].*") || str.matches(".*?\\b(or|to)-.*") ){//1–2-{pinnately} or-{palmately} {lobed} => {1–2-pinnately-or-palmately} {lobed}
				str = str.replaceAll("\\}?-or\\s+\\{?", "-or-").replaceAll("\\}?\\s+or-\\{?", "-or-").replaceAll("\\}?-to\\s+\\{?", "-to-").replaceAll("\\}?\\s+to-\\{?", "-to-").replaceAll("-or\\} \\{", "-or-").replaceAll("-to\\} \\{", "-to-");
			}
			//{often} 2-, 3-, or 5-{ribbed} ; =>{often} {2-,3-,or5-ribbed} ;  635.txt-16
			Pattern pp = Pattern.compile("(.*?)((\\d-,\\s*)+ (to|or) \\d-\\{)(.*)");
			Matcher m = pp.matcher(str);
			while(m.matches()){
				str = m.group(1)+"{"+m.group(2).replaceAll("[, ]","").replaceAll("\\{$", "")+m.group(5);
				m = pp.matcher(str);
			}
			String scp = str;
			str = str.replaceAll("(?<![\\d(\\[–—-]\\s?)[–—-]+\\s*(?="+NumericalHandler.numberpattern+"\\s+\\W?("+ChunkedSentence.units+")\\W?)", " to "); //fna: tips>-2.5 {mm}
			if(!scp.equals(str)){
				System.out.println();
			}
			
			//make a position to [anterior to] a single token
			m = this.positionptn2.matcher(str);
			while(m.matches()){
				str = m.group(1)+m.group(2).replaceAll("[<{}>]", "").replaceAll("\\s+", "-")+"-PPP"+m.group(3);
				m = this.positionptn2.matcher(str);
			}
			
			str = str.replaceAll("(?<=> [\\d"+this.romandigits+"]{1,3})-(?=<)", " - "); //<metacarpal> 2-<metacarpal> 1 {length} {ratio}
			this.chunkedtokens = new ArrayList<String>(Arrays.asList(str.split("\\s+")));
			str = normalizePositionList(str);
			str = normalizeCountList(str);
			lookupCharacters(str);//populate charactertokens
	        if(this.charactertokensReversed.contains("color") || this.charactertokensReversed.contains("coloration")){
	        	str = normalizeColorPatterns();
	        	lookupCharacters(str);
	        }
	        
	        if(str.indexOf(" to ")>=0 ||str.indexOf(" or ")>=0){
	        	if(this.printList){
					System.out.println(str);
				}
	        	str = normalizeCharacterLists(); //a set of states of the same character connected by ,/to/or => {color-blue-to-red}
	        }

	        if(str.matches(".*? as\\s+[\\w{}<>]+\\s+as .*")){
	           str = normalizeAsAs(str);
	        }
	        
	        if(str.matches(".*\\bsame\\b.*?\\bas\\b.*")){
	        	str = normalizeSameAs(str);
	        }
	        
	        //if(str.matches(".*?\\bin\\b.*?\\bassociation\\W+(with|to)\\b.*")){
	        //	str = normalizeAssociationWith(str);
	        //}
	        
	        
	        if(str.matches(".*?(?<=[a-z])/(?=[a-z]).*")){
	        	str = str.replaceAll("(?<=[a-z])/(?=[a-z])", "-");
	        }
	        
	        //10-20(-38) {cm}×6-10 {mm} 
	        
			try{
				Statement stmt = conn.createStatement();
				Statement stmt1 = conn.createStatement();
				String strcp2 = str;
				
				String strnum = null;
				/*
				//if(str.indexOf("}×")>0){//{cm}×
				if(str.indexOf("×")>0){
					containsArea = true;
					String[] area = normalizeArea(str);
					str = area[0]; //with complete info
					strnum = area[1]; //contain only numbers
				}
				*/
		           
		        //deal with (3) as bullet
				Pattern pattern1  = Pattern.compile("^(and )?([(\\[]\\s*\\d+\\s*[)\\]]|\\d+.)\\s+(.*)"); //( 1 ), [ 2 ], 12.
				m = pattern1.matcher(str.trim());
				if(m.matches()){
					str = m.group(3);
				}
				if(str.indexOf("±")>=0){
					str = str.replaceAll("±(?!~[a-z])","{moreorless}").replaceAll("±(?!\\s+\\d)","moreorless");
				}
				/*to match {more} or {less}*/
				if(str.matches(".*?\\b[{<]*more[}>]*\\s+or\\s+[{<]*less[}>]*\\b?.*")){
					str = str.replaceAll("[{<]*more[}>]*\\s+or\\s+[{<]*less[}>]*", "{moreorless}");
				}
				//if(str.matches(".*?\\bin\\s+[a-z_<>{} -]+\\s+[<{]?view[}>]?\\b.*")){//ants: "in full-face view"
				if(str.matches(".*?\\bin\\s+[a-z_<>{} -]*\\s*[<{]?(view|profile)[}>]?\\b.*")){
					m = viewptn.matcher(str);
					while(m.matches()){
						str = m.group(1)+" {"+m.group(2).replaceAll("[<>{}]", "").replaceAll("\\s+", "-")+"} "+m.group(3); 
						m = viewptn.matcher(str);
					}
				}
				
				//make a prepphrase (e.g. in relation to) a single token
				Iterator<String> it = prepphrases.iterator();
				while(it.hasNext()){
					String phrase = "\\{?\\<?"+it.next().trim().replaceAll(" ", "\\\\>?\\\\}? \\\\{?\\\\<?")+"\\>?\\}?";
					Pattern p = Pattern.compile("(.*?)(\\b"+phrase+"\\b)(.*)");
					m = p.matcher(str);
					while(m.matches()){
						str = m.group(1)+m.group(2).replaceAll("[<{}>]", "").replaceAll("\\s+", "-")+"-PPP"+m.group(3);
						m = p.matcher(str);
					}					
				}		
				
				//make a position to [anterior to] a single token
				//m = this.positionptn2.matcher(str);
				//while(m.matches()){
				//	str = m.group(1)+m.group(2).replaceAll("[<{}>]", "").replaceAll("\\s+", "-")+"-PPP"+m.group(3);
				//	m = this.positionptn2.matcher(str);
				//}
										
				if(str.indexOf("×")>0){
					containsArea = true;
					String[] area = normalizeArea(str);
					str = area[0]; //with complete info
					strnum = area[1]; //like str but with numerical expression normalized
				}

				str = handleBrackets(str);
				str = stringCharacterComparison(str);
				str = normalizefromto(str);
				if(type.compareTo("character")==0){//{postorbital} , {form} of {dorsal} <surface>
					String temp = str;
					str = str.replaceFirst("(?<=^|,\\s)\\{?\\w+\\}? of ", "").trim(); //shape of 
					String ch = temp.replace(str, "").replace("\\s+of\\s+", "").replaceAll("[{}]", "").trim();
					StanfordParser.characterRstates.put(ch, "1"); //to keep only the unique characters
				}
				
				stmt.execute("update "+this.tableprefix+"_markedsentence set rmarkedsent ='"+str.replaceAll("-PPP", "")+"' where source='"+src+"'");	
				
				if(containsArea){
					str = strnum;
					str = handleBrackets(str);
				}
				str = Utilities.threeingSentence(str);
   	            if(strcp.compareTo(str)!=0){
	        	   System.out.println("orig sent==>"+ strcp);
	        	   System.out.println("rmarked==>"+ strcp2);
	        	   System.out.println("threed-sent==>"+ str);
	           }
	           //str = str.replaceAll("}>", "/NN").replaceAll(">}", "/NN").replaceAll(">", "/NN").replaceAll("}", "/JJ").replaceAll("[<{]", "");
	           
	           
	           StringBuffer sb = new StringBuffer();
	           /*Pattern pattern7 = Pattern.compile("(.*?)([<{]*)([0-9a-zA-Z-]+)[}>]*(.*)");
	           Matcher m = pattern7.matcher(str);
	           while ( m.matches()){
	        	   sb.append(m.group(1));
	        	   String pos = m.group(2);
	        	   String word = m.group(3);
	        	   str = m.group(4);*/
    		   	   //m = pattern7.matcher(str);
    		   	   //continue;
	           String[] tokens = str.split("\\s+");
	           for(int i = 0; i<tokens.length; i++){
	        	   String word = tokens[i];
	        	   String pos = "";
	        	   if(word.endsWith("}")){
	        		   pos = "{";
	        	   }else if(word.endsWith(">")){
	        		   pos = "<";
	        	   }
	        	   word = word.replaceAll("[<>{}]", "").trim();
	        	   String p = "";
	        	   if(word.length()>0 && !word.matches("\\W") && !word.matches("("+ChunkedSentence.prepositions+")") &&!word.matches("("+ChunkedSentence.stop+")")){
		        	   ResultSet rs1 = stmt1.executeQuery("select semanticrole from "+this.tableprefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+" where word='"+word+"'");
		       		   if(rs1.next()){
		       			   p = rs1.getString("semanticrole");
		       		   }
	        	   }
	        	   
	        		Matcher mc = compreppattern.matcher(word);
	        		if(mc.matches()){
		        		   sb.append(word+"/IN ");
	        		}else if(word.contains("relative~")){
	        			 sb.append(word+"/JJ ");
	        		}else if(word.matches("in-.*?(-view|profile)")){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.matches("from~.*?~to~.*")){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.endsWith("-PPP")){//prepphrase in_association_with
	        		   sb.append(word.replaceFirst("-PPP", "")+"/IN ");
	        	   }else if(word.endsWith("ly") && word.indexOf("~") <0){ //character list is not RB
	        		   sb.append(word+"/RB ");
	        	   }else if(word.compareTo("becoming")==0 || word.compareTo("about")==0){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.compareTo("throughout")==0 && i+1 < tokens.length && tokens[i+1].matches("(\\.|;|,|or)")){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.compareTo("throughout")==0 && i+1 >= tokens.length){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.compareTo("at-least")==0){
	        		   sb.append(word+"/RB ");
	        	   }else if(word.compareTo("one_another")==0){
	        		   sb.append(word+"/NN ");
	        	   }else if(word.compareTo("plus")==0){
	        		   sb.append(word+"/CC ");
	        	   }else if(word.matches("\\d+[cmd]?m\\d+[cmd]?m")){ //area turned into 32cm35mm
	        		   //sb.append(word+"/CC ");
	        		   sb.append(word+"/CD ");
	        	   }else if(word.matches("("+ChunkedSentence.units+")")){
	       			   sb.append(word+"/NN ");
	       		   }else if(word.matches("as-\\S+")){ //as-wide-as
	       		   	   sb.append(word+"/IN "); //changed from RB to IN 2/22/02 by Hong
	       		   }else if(word.matches("same-\\S+")){ //same-as
	       		   	   sb.append(word+"/IN "); //added 2/22/02 by Hong
	       		   }else if(word.matches("in-\\S+")){ //in-association-with/to
	       		   	   sb.append(word+"/IN "); //added 2/22/02 by Hong
	       		   }else if(p.contains("op")){ //<inner> larger.
	       				//System.out.println(rs1.getString(2));
	       			   sb.append(word+"/NN ");
	       		   }else if(p.contains("os") || pos.indexOf('<') >=0){
	       			   sb.append(word+"/NN ");
	       		   }else if(p.contains("c")|| pos.indexOf('{') >=0){
	       			   	//ResultSet rs3 = stmt1.executeQuery("select word from wordpos4parser where word='"+word+"' and certaintyl>5");
	       				ResultSet rs2 = stmt1.executeQuery("select word from brown_wordfreq where word='"+word+"' and freq>79");//1/largest freq in wordpos = 79/largest in brown
	       				if(rs2.next()){
	       					sb.append(word+" ");
	       				//}else if(word.indexOf("3-")>=0){
	       				//	sb.append(word+"/CD");
	       				}else{
	       					sb.append(word+"/JJ ");
	       				}
	       		   }else{
	       				sb.append(word+" ");
	       		   }
	       		   //m = pattern7.matcher(str);
	       		}
	           	//sb.append(str);
	       		str = sb.toString().trim();
	       		str = str.replaceAll("(?<=[a-z])\\s+[_–-]\\s+(?=[a-z])", "-").replaceAll("/[A-Z]+\\s*[-–]\\s*", "-").replaceAll("\\d-\\s+(?=[a-z])", "3-"); //non -septate/JJ or linear/JJ _ovoid/JJ
	       		str = str.replaceAll("[\\[\\(]", " -LRB-/-LRB- ").replaceAll("[\\)\\]]", " -RRB-/-RRB- ").replaceAll("\\s+", " ").trim(); 
	       		str = str.replaceAll("moreorless/JJ","moreorless/RB");
	       		return str;
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}
		//return "";
	}
	
	private String normalizefromto(String str) {
		String cp = str;
		boolean changed = false;
		if(str.matches(".*\\bfrom .*? to\\b.*")){
			Pattern struct = Pattern.compile("(.*?)(\\bfrom (?:<?\\{?(?:"+this.positions+")\\}?>? |<\\w+> |of |the )+to (?:<?\\{?(?:"+this.positions+")\\}?>? |<\\w+> |of |the )+)(.*)");
			Matcher m = struct.matcher(str+" "); //need the trailing space
			while(m.matches()){
				str = m.group(1)+m.group(2).trim().replaceAll(" ", "~")+" "+m.group(3);
				m = struct.matcher(str);
				changed = true;
			}
					
			if(this.printfromto && changed){
				System.out.println("normalized from-to from:"+cp);
				System.out.println("normalized from-to to: "+str);
			}
		}
		return str;
	}

	/**
	 * {width} of <ethmoid> relative-to its {length} from <snout> <tip> to the {posterior} <{margin}> of the <parietals>
	 * @param str [char of A|A char] [relative-to|<=|>=|=|x times] [char of B|B char]
	 * @return {relative~{A~char}~{relation}~{B~char}}, assign JJ as its post
	 */
	private String stringCharacterComparison(String str) {
		//width of A relative-to length of B
		//width of A relative to B
		//A width relative to length of B
		//A width relative to B
		//width of A relative to length
		String cp = str;
		Pattern relations = Pattern.compile("(.*?)\\b(relative-to|[\\w-]+equal-to|[\\w]+er\\}? than|times)\\b(.*)");
		Matcher m = relations.matcher(str);
		if(m.matches() && str.indexOf("<")>=0 && str.matches(".*?\\b("+this.characterptn+")\\b.*")){ //mostly like a comparison of characters
			if(m.group(1).trim().length()>0 && m.group(3).trim().length()>0){
				String[] part1 = pullCharacterInfo(m.group(1).trim(), "part1");
				String[] part2 = pullCharacterInfo(m.group(3).trim(), "part2");
				String relation = m.group(2);
				if(part1[0]!=null && part2[0]!=null && part1[0].length()>0 && part2[0].length()>0){
					str = part1[1]+" {relative~{"+part1[0]+"}~{"+relation+"}~{"+part2[0]+"}} " +part2[1];
					if(this.printRelative){
						System.out.println(cp);
						System.out.println("after relative reformation:" + str);
					}
					return str;
				}
			}
		}
		return str;
	}
	
	
	/**
	 * part1:
	 * input string: {width} of <ethmoid>
	 * output string[0]: ethmoid~width string[1]: ""
	 * part2: 
	 * input string: its {length} from <snout> <tip> to the {posterior} <{margin}> of the <parietals>
	 * output string[0]: length; [1] from <snout> <tip> to the {posterior} <{margin}> of the <parietals>
	 * @param str a string containing an organ or a character or both
	 * @return two text segments: the first is the organ~character pair (if not found, the first element = ""), the text that to the left or right of the organ~character pair makes the second segment
	 */
	private String[] pullCharacterInfo(String str, String part){
		String[] result = new String[2];

		if(part.compareTo("part1")==0){
			//find the last structure and character
			Matcher m = pof1.matcher(str);
			if(m.matches()){
				result[1] = m.group(1).trim();
				result[0] = m.group(3).replaceAll("[{<>}]", "").trim().replaceAll(" ",  "-")+"~"+m.group(2);
				return result;
			}
			m = p1.matcher(str);
			if(m.matches()){
				String temp = m.group(1);//ends with a space
				String ch = m.group(2);
				result[1] = temp.replaceFirst("(<?\\{?(this.positions)\\}?>? |<\\w+> |of )+$", "").trim();
				result[0] = temp.replace(result[1], "").trim().replaceAll("[{<>}]", "").replaceAll(" ",  "-")+"~"+ch;
				return result;
			}			
		}else{
			//find the first structure/character (may just have one of the two)
			//contain both elements
			Matcher m = pof2.matcher(str);
			if(m.matches()){
				result[1] = m.group(4).trim();
				result[0] = m.group(2).replaceAll("[{<>}]", "").trim().replaceAll(" ",  "-")+"~"+m.group(1);
				return result;
			}
			m = p2.matcher(str);
			if(m.matches()){
				result[1] = m.group(4).trim();
				result[0] = m.group(1).replaceAll("[{<>}]", "").trim().replaceAll(" ",  "-")+"~"+m.group(3);
				return result;
			}
			str = str+" "; //need the trailing space
			//contain one of the two
			m = pof3.matcher(str);
			int starto=1000, endo=1000, startc=1000, endc=1000, start=0, end=0;
			if(m.find()){
				starto = m.start();
				endo = m.end();
			}
			m = p3.matcher(str);
			if(m.find()){
				startc = m.start();
				endc = m.end();
			}
			start = starto<startc? starto:startc;
			if(start == starto) end = endo;
			else end = endc;
			result[0] = str.substring(start, end).replaceAll("[<{}>]", "").trim();
			result[1] = str.substring(end).trim();
			return result;
		}					
		return result;
	}

	/** 	
	 * @param str: {upper} {pharyngeal} <tooth> <plates> 4 and 5
	 * @return: {upper} {pharyngeal} <tooth> <plates_4_and_5>
	 */
	private String normalizePositionList(String str) {
		Matcher m = positionptn.matcher(str);
		while(m.find()){
			int start = m.start(1);
			int end = m.end(1);
			String position = m.group(1);
			String organ = m.group(2);
			if(!isPosition(organ, position)) continue;
			String rposition = "<"+position.replaceAll("[<>]", "").replaceAll("\\s+", "_")+">";
			//synchronize this.chunkedtokens
			//split by single space to get an accurate count to elements that would be in chunkedtokens
			int index = (str.substring(0, start).trim()+" a").trim().split("\\s").length-1; //number of tokens before the count pattern
			this.chunkedtokens.set(index, rposition);
			int num = position.split("\\s+").length;
			for(int i = index+1; i < index+num; i++){
				this.chunkedtokens.set(i, "");
			}
			//resemble the str from chunkedtokens, counting all empty elements, so the str and chunkedtokens are in synch.
			str = "";
			for(String t: this.chunkedtokens){
				str +=t+" ";
			}
			m = positionptn.matcher(str);
		}
		return str.replaceAll("\\s+", " ").trim();
	}


	/**
	 * tooth 5 means the fifth tooth, 5 is position (true)
	 * teeth 5 means 5 teeth, 5 is count(false)
	 * teeth 2 and 3 means the second and third teeth, 2 and 3 are position(true)
	 * tooth 1 ??? treated as position (true) for the time being
	 * @param organ: teeth
	 * @param position: <teeth> 4 and 5
	 * @return
	 */
	private boolean isPosition(String organ, String position) {
		boolean multiplepositions = false;
		boolean pluralorgan = false;
		position = position.replace("<"+organ+">", "").trim();
		if(position.contains(" ") || position.contains("-")){
			multiplepositions = true;
		}		
		if(TermOutputerUtilities.isPlural(organ)){
			pluralorgan = true;
		}
		if(pluralorgan && !multiplepositions) return false;
		return true;
	}

	/**
	 * replace "one or two" with {count~list~one~or~two} in the string
	 * update this.chunkedTokens	
	 * @param str
	 */
		private String normalizeCountList(String str) {
			Matcher m = this.countptn.matcher(str);
			while(m.find()){
				int start = m.start(1);
				int end = m.end(1);
				String count = m.group(1).trim();
				String rcount = "{count~list~"+count.replaceAll(" ","~").replaceAll("[{}]", "")+"}";
				//synchronise this.chunkedtokens
				//split by single space to get an accurate count to elements that would be in chunkedtokens
				int index = (str.substring(0, start).trim()+" a").trim().split("\\s").length-1; //number of tokens before the count pattern
				this.chunkedtokens.set(index, rcount);
				int num = count.split("\\s+").length;
				for(int i = index+1; i < index+num; i++){
					this.chunkedtokens.set(i, "");
				}
				//resemble the str from chunkedtokens, counting all empty elements, so the str and chunkedtokens are in synch.
				str = "";
				for(String t: this.chunkedtokens){
					str +=t+" ";
				}
				m = this.countptn.matcher(str);
			}
			return str.replaceAll("\\s+", " ").trim();
		}

/**remove all bracketed text such as "leaves large (or small as in abc)"
 * do not remove brackets that are part of numerical expression : 2-6 (-10)
 * @param str: "leaves large (or small as in abc)"
 * @return: "leaves large"
 */
	private String handleBrackets(String str) {
		//remove nested brackets left by pl such as (petioles (2-)4-8 cm)
		//String p1 ="\\([^()]*?[a-zA-Z][^()]*?\\)";
		//String p2 = "\\[[^\\]\\[]*?[a-zA-Z][^\\]\\[]*?\\]";
		//String p3 = "\\{[^{}]*?[a-zA-Z][^{}]*?\\}";				
		if(str.matches(".*?\\(.*?[a-zA-Z].*?\\).*") || str.matches(".*?\\[.*?[a-zA-Z].*?\\].*")){ 
			String[] pretokens = str.split("\\s+");
			str = Utilities.threeingSentence(str);
			String[] tokens = str.split("\\s+");
			StringBuffer bracketfree = new StringBuffer();
			boolean inbracket = false;
			for(int i=0; i<tokens.length; i++){
				if(tokens[i].matches("[(\\[].*")){
					inbracket = true;
				}
				if(!inbracket){
					if(tokens[i].compareTo("3")==0){
						bracketfree.append(pretokens[i]+" ");
					}else{
						bracketfree.append(tokens[i]+" ");
					}
				}												
				if(tokens[i].matches(".*[)\\]]")){
					inbracket = false;
				}
			}
			str = bracketfree.toString().trim();
			if(str.matches(".*?\\(\\s+?\\s+\\).*")){//2n=20( ? ), 30 => 2n=20?, 30
				str = str.replaceAll("\\(\\s+?\\s+\\)", "?");
			}
			//str = str.replaceAll(p1, "").replaceAll(p2, "").replaceAll("\\s+", " ").trim();					
		}
		return str;
	}

	
	
		/**
		 * make  "suffused with dark blue and purple or green" one token
		 * ch-ptn"color % color color % color @ color"
		 * @return {color~list~color1~color2}
		 */
	private String normalizeColorPatterns() {
		String list = "";
		String result = "";
		String header = "ttt";
		for(int i = this.charactertokensReversed.size() -1; i>=0; i--){
			list+=this.charactertokensReversed.get(i)+" ";
		}
		list = list.trim()+" "; //need to have a trailing space
		Pattern p = Pattern.compile("(.*?)((color|coloration)\\s+%\\s+(?:(?:color|coloration|@|%) )+)(.*)");
		Matcher m = p.matcher(list);
		int base = 0;
		while(m.matches()){
			int start = (m.group(1).trim()+" a").trim().split("\\s+").length+base-1;
			int end = start+(m.group(2).trim()+" b").trim().split("\\s+").length-1;
			String ch = m.group(3)+header;
			list = m.group(4);
			m = p.matcher(list);
			//form result string, adjust chunkedtokens
			for(int i = base; i<start; i++){
				result += this.chunkedtokens.get(i)+" ";
			}
			if(end>start){ //if it is a list
				String t= "{"+ch+"~list~";
				for(int i = start; i<end; i++){
					t += this.chunkedtokens.get(i).trim().replaceAll("[{}]", "").replaceAll("[,;\\.]", "punct")+"~";
					this.chunkedtokens.set(i, "");
				}
				t = t.replaceFirst("~$", "}");
				t = distributePrep(t)+" ";
				this.chunkedtokens.set(end-1, t.trim());//"suffused with ..." will not form a list with other previously mentioned colors, but may with following colors, so put this list close to the next token.
				result +=t;
			}
			//prepare for the next step
			base = end;
			
			
		}
		//dealing with the last segment of the list or the entire list if no match
		for(int i = base; i<(list.trim()+" b").trim().split("\\s+").length+base-1; i++){
			result += this.chunkedtokens.get(i)+" ";
		}
		return result;
	}

	/**
	 * 
	 * @param t: {color~list~suffused~with~red~or~purple}
	 * @return {color~list~suffused~with~red~or~purple}
	 */
	private String distributePrep(String t) {
			Pattern p = Pattern.compile("(^.*~list~)(.*?~with~)(.*?~or~)(.*)");
			Matcher m = p.matcher(t);
			if(m.matches()){
				t = m.group(1)+m.group(2)+m.group(3)+m.group(2)+m.group(4);
			}
			return t;
		}

	/**
	 * 
	 * @param text
	 * @return two strings: one contains all text from text with rearranged spaces, the other contains numbers as the place holder of the area expressions
	 */	
	private String[] normalizeArea(String text){
			String[] result = new String[2];
			String text2= text;
			Pattern p = Pattern.compile("(.*?)([\\d\\.()+-]+ \\{[cmd]?m\\}×\\S*\\s*[\\d\\.()+-]+ \\{[cmd]?m\\}×?(\\S*\\s*[\\d\\.()+-]+ \\{[cmd]?m\\})?)(.*)");
			Matcher m = p.matcher(text);
			while(m.matches()){
				text = m.group(1)+m.group(2).replaceAll("[ \\{\\}]", "")+ m.group(4);
				m = p.matcher(text2);
				m.matches();
				text2 = m.group(1)+m.group(2).replaceAll("[cmd]?m", "").replaceAll("[ \\{\\}]", "")+ m.group(4);
				m = p.matcher(text);
			}
			result[0] = text;
			result[1] = text2;
			return result;
	}
	
	private void lookupCharacters(String str) {
		if(str.trim().length() ==0){
			return;
		}
		this.charactertokensReversed = new ArrayList<String>();
		boolean save = false;
		boolean ambiguous = false;
		ArrayList<String> saved = new ArrayList<String>();
		
		ArrayList<String> amb = new ArrayList<String>();
		for(int i = this.chunkedtokens.size()-1; i>=0; i--){
			String word = this.chunkedtokens.get(i);
			if(word.indexOf("~list~")>0){
				String ch = word.substring(0, word.indexOf("~list~")).replaceAll("\\W", "").replaceFirst("ttt$", "");
				this.charactertokensReversed.add(ch);
			}else if(word.indexOf('{')>=0 && word.indexOf('<')<0){
				String ch = Utilities.lookupCharacter(word, conn, this.characterhash, glosstable, tableprefix); //remember the char for this word (this word is a word before (to|or|\\W)
				if(ch==null){
					this.charactertokensReversed.add(word.replaceAll("[{}]", "")); //
				}else{
					this.charactertokensReversed.add(ch); //color
					if(save){
						save(saved, this.chunkedtokens.size()-1-i, ch); 
						if(ch.indexOf(Utilities.or)>0){
							ambiguous = true;
							amb.add(this.chunkedtokens.size()-1-i+"");
						}
					}
					save = false;
				}
			}else if (word.indexOf('<')>=0){
				this.charactertokensReversed.add("#");
				save = true;
			}else if(word.matches("(to|or)")){
				this.charactertokensReversed.add("@"); //to|or
				save = true;
			}else if(word.matches("\\W")){
				this.charactertokensReversed.add(word); //,;.
				save = true;
			}else if(word.compareTo("±")==0){
				this.charactertokensReversed.add("moreorless"); //,;.
				save = true;
			}else{
				this.charactertokensReversed.add("%");
				save = true;
			}
		}
		
		//deal with a/b characters
		if(ambiguous){
			Iterator<String> it = amb.iterator();
			while(it.hasNext()){
				int i = Integer.parseInt(it.next());
				Pattern p = Pattern.compile("("+this.charactertokensReversed.get(i)+"|"+this.charactertokensReversed.get(i).replaceAll(Utilities.or, "|")+")");
				String tl = lastSaved(saved, i);
				Matcher m = p.matcher(tl);
				//if(m.matches()){
				if(m.find()){
					this.charactertokensReversed.set(i, m.group(1));
				}else{
					String tn = nextSaved(saved, i);
					m = p.matcher(tn);
					//if(m.matches()){
					if(m.find()){
						this.charactertokensReversed.set(i, m.group(1));
					}
				}
			}
		}
	}
	
	private String lastSaved(ArrayList<String> saved, int index){
		for(int i = index-1; i >=0 && i<saved.size(); i--){
			if(saved.get(i).trim().length()>0){
				return saved.get(i);
			}
		}
		return "";
	}
	
	private String nextSaved(ArrayList<String> saved, int index){
		for(int i = index+1; i <saved.size(); i++){
			if(saved.get(i).trim().length()>0){
				return saved.get(i);
			}
		}
		return "";
	}
	
	
	
	private void save(ArrayList<String> saved, int index, String ch){
		while(saved.size()<=index){
			saved.add("");
		}
		saved.set(index, ch);
	}
	/**
	 * put a list of states of the same character connected by to/or in a chunk
	 * color, color, or color
	 * color or color to color
	 * 
	 * {color~list~blue~to~red}
	 * @return updated string
	 */
	private String normalizeCharacterLists(){
		//charactertokens.toString
		String list = "";
		String result = "";
		for(int i = this.charactertokensReversed.size() -1; i>=0; i--){
			list+=this.charactertokensReversed.get(i)+" ";
		}
		list = list.trim()+" "; //need to have a trailing space
		
		//pattern match: collect state one by one
		int base = 0;
		//Pattern pt = Pattern.compile("(.*?(?:^| ))(([0-9a-z–\\[\\]\\+-]+ly )*([a-z-]+ )+([@,;\\.] )+\\s*)(([a-z-]+ )*(\\4)+[@,;\\.%\\[\\]\\(\\)#].*)");//
		Pattern pt = Pattern.compile("(.*?(?:^| ))(([0-9a-z–\\[\\]\\+-]+ly )*([_a-z-]+ )+([@,;\\.] )+\\s*)(([_a-z-]+ )*(\\4)+([0-9a-z–\\[\\]\\+-]+ly )*[@,;\\.%\\[\\]\\(\\)#].*)");//
		Matcher mt = pt.matcher(list);
		while(mt.matches()){
			int start = (mt.group(1).trim()+" a").trim().split("\\s+").length+base-1; //"".split(" ") == 1
			String l = mt.group(2);
			String ch = mt.group(4).trim();
			list = mt.group(6);
			//Pattern p = Pattern.compile("(([a-z-]+ )*([a-z-]+ )+([@,;\\.] )+\\s*)(([a-z-]+ )*(\\3)+[@,;\\.%\\[\\]\\(\\)#].*)");//merely shape, @ shape
			Pattern p = Pattern.compile("(([a-z-]+ )*([a-z-]+ )+([0-9a-z–\\[\\]\\+-]+ly )*([@,;\\.] )+\\s*)(([a-z-]+ )*(\\3)+([0-9a-z–\\[\\]\\+-]+ly )*[@,;\\.%\\[\\]\\(\\)#].*)");//merely shape, @ shape
			Matcher m = p.matcher(list);
			while(m.matches()){
				l += m.group(1);
				//list = m.group(5);
				list = m.group(6);
				m = p.matcher(list);
			}
			l += list.replaceFirst("[@,;\\.%\\[\\]\\(\\)#].*$", "");//take the last seg from the list
			int end = start+(l.trim()+" b").trim().split("\\s+").length-1;
			if(! l.matches(".*?@[^,;\\.]*") && l.matches(".*?,.*")){ //the last state is not connected by or/to, then it is not a list
				start = end;
			}
				
			
			list = list.replaceFirst("^.*?(?=[@,;\\.%\\[\\]\\(\\)#])", "");
			mt = pt.matcher(list);
			
			for(int i = base; i<start; i++){
				result += this.chunkedtokens.get(i)+" ";
			}
			if(end>start){ //if it is a list
				String t= "{"+ch+"~list~";
				for(int i = start; i<end; i++){
					if(this.chunkedtokens.get(i).length()>0){
						t += this.chunkedtokens.get(i).trim().replaceAll("[{}]", "").replaceAll("[,;\\.]", "punct")+"~";
					}else if(i == end-1){
						while(this.chunkedtokens.get(i).length()==0){
							i++;
						}
						t+=this.chunkedtokens.get(i).trim().replaceAll("[{}]", "").replaceAll("[,;\\.]", "punct")+"~";
					}
					this.chunkedtokens.set(i, "");
				}
				t = t.replaceFirst("~$", "}")+" ";
				if(t.indexOf("ttt~list")>=0) t = t.replaceAll("~color.*?ttt~list", "");
				this.chunkedtokens.set(start, t);
				result +=t;
				if(this.printList){
					System.out.println(">>>"+t);
				}
			}
			base = end;
		}
		
		for(int i = base; i<(list.trim()+" b").trim().split("\\s+").length+base-1; i++){
			result += this.chunkedtokens.get(i)+" ";
		}
		
		return result.trim();
	}
		
	/**
	 * the same as => same-as/IN
	 * as wide as or/to wider than inner
	 * as wide as inner
	 * as wide as long
	 * @return
	 */	
	/*private String normalizeAssociationWith(String str) {
		String result = "";
		Pattern p = Pattern.compile("(.*?\\b)(in\\b.*?\\bassociation\\W+(?:with|to))(\\b.*)");
		Matcher m = p.matcher(str);
		while(m.matches()){
			result+=m.group(1);
			result+="{"+m.group(2).replaceAll("\\s+", "-").replaceAll("[{}<>]", "")+"}";
			str = m.group(3);
			m = p.matcher(str);
		}
		result+=str;
		return result.replaceAll("\\{+", "{").replaceAll("\\}+", "}").trim();
	}*/


	/**
	 * the same as => same-as/IN
	 * as wide as or/to wider than inner
	 * as wide as inner
	 * as wide as long
	 * @return
	 */	
	private String normalizeSameAs(String str) {
		String result = "";
		Pattern p = Pattern.compile("(.*?\\b)(same\\b[ \\w{}<>]+\\s+as)(\\b.*)");
		Matcher m = p.matcher(str);
		while(m.matches()){
			result+=m.group(1);
			result+="{"+m.group(2).replaceAll("\\s+", "-").replaceAll("[{}<>]", "")+"}";
			str = m.group(3);
			m = p.matcher(str);
		}
		result+=str;
		return result.replaceAll("\\{+", "{").replaceAll("\\}+", "}").trim();
	}

	/**
	 * as wide as => as-wide-as/IN
	 * as wide as or/to wider than inner
	 * as wide as inner
	 * as wide as long
	 * @return
	 */	
	private String normalizeAsAs(String str) {
		String result = "";
		Pattern p = Pattern.compile("(.*?\\b)(as\\s+[\\w{}<>]+\\s+as)(\\b.*)");
		Matcher m = p.matcher(str);
		while(m.matches()){
			result+=m.group(1);
			result+="{"+m.group(2).replaceAll("\\s+", "-").replaceAll("[{}<>]", "")+"}";
			str = m.group(3);
			m = p.matcher(str);
		}
		result+=str;
		return result.trim();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		//File posedfile = new File(posedfile); 
		//File parsedfile = new File("");
		String database = "fnav19_benchmark";
		String tableprefix = "fnav19";
		String POSTaggedSentence="POSedSentence";
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				//Statement stmt = conn.createStatement();
				//stmt.execute("create table if not exists "+tableprefix+"_"+POSTaggedSentence+"(source varchar(100) NOT NULL, posedsent TEXT, PRIMARY KEY(source))");
				//stmt.execute("delete from "+tableprefix+"_"+POSTaggedSentence);
				//stmt.close();
			}
		}catch(Exception e){
			e.printStackTrace();			
		}
		POSTagger4StanfordParser tagger = new POSTagger4StanfordParser(conn, tableprefix, "fnaglossaryfixed");
		
		//String str="<Cypselae> {tan} , {subcylindric} , {subterete} to 5-{angled} , 8–10 {mm} , {indistinctly} 8–10-{ribbed}";
		//String src="364.txt-15";
		//String str="{often} 2- , 3- , or 5-{ribbed}";
		//String src="625.txt-16";
		//String str = "<heads> in {paniculiform} arrays .";
		//String src = "10.txt-4";
		//String str = "<{middle}> <phyllaries> {acuminate} at <apex> with <point> 22 – 38 {mm} and <{spine}> <tip> 6 – 9 {mm} , or in some {cultivated} {forms} {broadly} {obtuse} to {truncate} and {mucronate} with or without <{spine}> <tip> 1 – 2 {mm} , {distal} <margins> with or without {indistinct} {yellowish} <margins> .";
		//String src = "41.txt-1";
		//String str = " <outer> 5 – 6 {lance-ovate} to {lanceolate} , 4 – 7 {mm} , {basally} {cartilaginous} , {distally} {herbaceous} , <inner> 8 + {lance-linear} to {linear} , 6 – 12 {mm} , {herbaceous} , all {usually} with some <{gland}>-{tipped} <hairs> 0 . 5 – 0 . 8 {mm} on <margins> near <bases> or on {abaxial} <faces> toward <tips> .";
		//String src = "273.txt-6";
		//String str = "<stems> {usually} 1 , {branched} {distally} or {openly} so throughout , {leafy} , {glabrous} or {thinly} {arachnoid-tomentose} .";
		String src = "157.txt-1";
		String str = "laminae 6 17 cm . long , 2 - 7 cm . broad , lanceolate to narrowly oblong or elliptic_oblong , abruptly and narrowly acuminate , obtuse to acute at the base , margin entire , the lamina drying stiffly chartaceous to subcoriaceous , smooth on both surfaces , essentially glabrous and the midvein prominent above , glabrous to sparsely puberulent beneath , the 8 to 18 pairs of major secondary veins prominent beneath and usually loop_connected near the margin , microscopic globose_capitate or oblongoid_capitate hairs usually present on the lower surface , clear or orange distally .";
		try{
		System.out.println(tagger.POSTag(str, src, "description")); //type is one of "character" and "description"
		}catch(Exception e){
			e.printStackTrace();
		}*/
	}

}
