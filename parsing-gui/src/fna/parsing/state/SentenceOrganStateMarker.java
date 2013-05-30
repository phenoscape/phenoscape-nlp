/**
 * 
 */
package fna.parsing.state;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;

import fna.charactermarkup.ChunkedSentence;
import fna.charactermarkup.Utilities;
import fna.parsing.ApplicationUtilities;

import conceptmapping.*;

/**
 * @author hongcui
 * last stable version: 653
 * this version: try to find additional nouns from unknown words, and mark them with <>. 
 */
@SuppressWarnings("unused")
public class SentenceOrganStateMarker {
	private Hashtable<String, String> sentences = new Hashtable<String, String>();
	private Connection conn = null;
	private boolean marked = false;
	private boolean fixadjnn = false;
	private int fixedcount  =0;

	private Hashtable<String, String> adjnounsent = null;
	private String adjnounslist = "";
	private String organnames = null;
	private String statenames = null;
	private String tableprefix = null;
	private String glosstable = null;
	private String colors = null;
	public static String compoundprep = "according to|ahead of|along with|apart from|as for|aside from|as per|as to as well as|away from|because of|but for|by means of|close to|contrary to|depending on|due to|except for|equal to|forward of|further to|greater or equal to|greater than or equal to|in addition to|in association to|in association with|in between|in case of|in combination with|in face of|in favour of|in front of|in lieu of|in spite of|instead of|in view of|less than or equal to|near to|next to|on account of|on behalf of|on board|on to|on top of|opposite to|other than|out of|outside of|owing to|preparatory to|prior to|regardless of|relative to|save for|subequal to|together with|unequal to|up against|up until|vis-a-vis|with reference to|with regard to";
	public static Pattern compreppattern = Pattern.compile("(.*?)\\b("+compoundprep+")\\b(.*)");
	private String ignoredstrings = "if at all|at all|as well (?!as)|i\\s*\\.\\s*e\\s*\\.|means of";
	//private ArrayList<String> order = new ArrayList<String>();
	private Display display;
	private StyledText charLog;
	private String termprefix = "basi|hypo";


	private Connection con;
	private String url;

	private boolean printCompoundPP=false;

	/**
	 * 
	 */

	public SentenceOrganStateMarker(Connection conn, String tableprefix, String glosstable, boolean fixadjnn, Display display, StyledText charLog) {
		this.display = display;
		this.charLog = charLog;
		this.tableprefix = tableprefix;
		this.conn = conn;
		this.glosstable = glosstable;
		this.fixadjnn = fixadjnn;
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+this.tableprefix+"_markedsentence");
			stmt.execute("create table if not exists "+this.tableprefix+"_markedsentence (sentid int(11)NOT NULL Primary Key, source varchar(100) , markedsent text, rmarkedsent text, type varchar(20))");
			//stmt.execute("update "+this.tableprefix+"_sentence set charsegment =''");
			colors = this.colorsFromGloss();
		}catch(Exception e){
			e.printStackTrace();
		}

		//preparing...
		this.adjnounsent = new Hashtable<String, String>(); //source ->adjnoun (e.g. inner)
		ArrayList<String> adjnouns = new ArrayList<String>();//all adjnouns
		try{
			Statement stmt = conn.createStatement();
			//ResultSet rs = stmt.executeQuery("select source, tag, originalsent from "+this.tableprefix+"_sentence");
			ResultSet rs = stmt.executeQuery("select source, modifier, tag, sentence, originalsent from "+this.tableprefix+"_sentence order by sentid desc");
			//leave ditto as it is
			while(rs.next()){//read sent in in reversed order
				String tag = rs.getString("tag");
				String sent = rs.getString("sentence").trim();
				if(sent.length()!=0){
					String source = rs.getString("source");
					String osent = rs.getString("originalsent");
					String text = stringColors(sent.replaceAll("</?[BNOM]>", ""));
					text = text.replaceAll("[ _-]+\\s*shaped", "-shaped").replaceAll("(?<=\\s)�\\s+m\\b", "um");
					text = text.replaceAll("&#176;", "�");
					text = text.replaceAll("\\bca\\s*\\.", "ca");
					text = text.replaceAll("(?<=\\d)\\s*(?=("+ChunkedSentence.percentage+")\\b)", " ").replaceAll("\\s+", " "); //80percent =>80 percent
					text = text.replaceAll("(?<=\\d)\\s*(?=("+ChunkedSentence.degree+")\\b)", " ").replaceAll("\\s+", " "); //80degree =>80 degree
					text = text.replaceAll("height width ratio", "h/w");
					text = Utilities.reformAuxiliaryVerbs (text); 
					text = text.replaceAll("(?<=\\d\\s)x(?=\\s\\w)", "times"); // 2 x longer => 2 times longer, won't match 2x=24 
					text = stringCompoundPP(text);
					text = rs.getString("modifier")+"##"+tag+"##"+text;
					//text.matches(".*?("+termprefix+").*")
					sentences.put(source, text);
				}
			}
			//merge ditto sentences with previous sentences: this had the drawback of attaching nearest organ as the subject of the ditto sentence
			/*String dittos = "";
			while(rs.next()){//read sent in in reversed order
				String tag = rs.getString("tag");
				String sent = rs.getString("sentence");
				String source = rs.getString("source");
				String osent = rs.getString("originalsent");
				if(tag.compareTo("ditto")==0){ //attach ditto to the previous sentence
					dittos = sent.trim()+" "+dittos;
					//sentences.put(source, ""); //make ditto sent id's disappear
				}else{
					sent =sent.trim() +" "+ dittos.trim();
					if(osent.indexOf(dittos.trim())<0) osent =osent.trim() +" "+ dittos.trim(); //put a check here so dittos are not added multiple times when the user runs the Parser mutiple times on one document collection
					dittos = "";
					String text = stringColors(sent.replaceAll("</?[BNOM]>", ""));
					text = text.replaceAll("[ _-]+\\s*shaped", "-shaped").replaceAll("(?<=\\s)�\\s+m\\b", "um");
					text = text.replaceAll("&#176;", "�");
					text = text.replaceAll("\\bca\\s*\\.", "ca");
					text = rs.getString("modifier")+"##"+tag+"##"+text;
					sentences.put(source, text);
					//update originalsent
					Statement st = conn.createStatement();
					st.execute("update "+this.tableprefix+"_sentence set originalsent='"+osent+"' where source='"+source+"'");
				}
			}*/
			//collect adjnouns
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT distinct modifier FROM "+this.tableprefix+"_sentence s where modifier != \"\" and tag like \"[%\"");
			while(rs.next()){
				String modifier = rs.getString(1).replaceAll("\\[.*?\\]", "").trim();
				adjnouns.add(modifier);
			}
			//collect senteces that need adj-nn disambiguation
			stmt = conn.createStatement();
			rs = stmt.executeQuery("SELECT source, tag, modifier FROM "+this.tableprefix+"_sentence s where modifier != \"\" and tag like \"[%\"");
			while(rs.next()){
				String modifier = rs.getString(2).replaceAll("\\[.*?\\]", "").trim(); 
				String tag = rs.getString("tag");
				adjnounsent.put(tag, modifier);//tag: [phyllary]
				//adjnounsent.put(tag.replaceAll("\\W", ""), modifier);//TODO: need to investigate more on this
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		Collections.sort(adjnouns);
		for(int i = adjnouns.size()-1; i>=0; i--){
			this.adjnounslist +=adjnouns.get(i)+"|";
		}
		this.adjnounslist = this.adjnounslist.trim().length()==0? null : "[<{]*"+this.adjnounslist.replaceFirst("\\|$", "").replaceAll("\\|+", "|").replaceAll("\\|", "[}>]*|[<{]*").replaceAll(" ", "[}>]* [<{]*")+"[}>]*";
		this.organnames = collectOrganNames();
		this.statenames = collectStateNames();
	}



	/**
	 * The normalize prefix method is used to expand the prefix of a sentence.
	 * Example : {basi}- and <hypobranchial> <ossifications> will be expanded to 
	 * 			<basibranchial> and <hypobranchial> <ossifications>	 * 
	 * @param text
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private String normalizePrefix(String text) throws ClassNotFoundException, SQLException
	{
		String[] splittext = text.split("\\s");

		for(int i=0;i<splittext.length;i++)
		{
			if(splittext[i].matches("\\{?("+termprefix+")\\}?-.*"))
			{
				if(((i+2)<=splittext.length) && ((splittext[i+1].equals("and")||(splittext[i+1].equals("or")))))
				{
					String termprefix1[] = termprefix.split("\\|");

					for(int z=0;z<termprefix1.length;z++)
						if(splittext[i+2].contains(termprefix1[z]))
						{
							splittext[i] ="<"+splittext[i].substring(1, splittext[i].lastIndexOf("-")-1)+splittext[i+2].substring(termprefix1[z].length()+1, splittext[i+2].length());
							inserttotable(splittext[i]);
							break;
						}
					break;
				}
			}
		}
		// combine splittext to form a single text.
		text="";
		for(int i=0,j=0;i<splittext.length;i++,j++)	
		{
			text+=splittext[i];
			if(j<splittext.length-1)
				text+=' ';
		}	
		return text;
	}

	// The normalized prefix is inserterted into term category table as a structure term
	void inserttotable(String term) throws ClassNotFoundException, SQLException
	{

		Class.forName("com.mysql.jdbc.Driver");
		con = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));

		// Drop table if exists
		Statement stmt0 = con.createStatement();
		System.out.println("Insert into "+this.tableprefix+"_term_category(term,category)"+" values(\""+term.trim()+"\",\"structure\")");
		stmt0.executeUpdate("Insert into "+this.tableprefix+"_term_category(term,category)"+" values(\""+term.trim()+"\",\"structure\")");
	}
	/**
	 * turn reddish purple to reddish-purple
	 * @param replaceAll
	 * @return
	 */
	private String stringColors(String text) {
		boolean did = false;
		String pt = "\\b(?<="+this.colors+")\\s+(?="+this.colors+")\\b";
		Pattern p = Pattern.compile(pt);
		Matcher m = p.matcher(text);
		while(m.find()){
			text = text.replaceFirst(pt, "_c_");
			m = p.matcher(text);
			did = true;
		}
		//if(did) System.out.println("[color]:"+text);
		return text;
	}

	public Hashtable<String, String> markSentences() throws Exception{
		if(this.marked){
			loadMarked();
		}else{
			this.showOutputMessage("System is preparing the sentences...");
			//Iterator<String> it = order.iterator();
			//while(it.hasNext()){				
			Enumeration<String> en = sentences.keys();
			while(en.hasMoreElements()){
				String source = en.nextElement();
				//String source = it.next();
				String sent = (String)sentences.get(source);
				String taggedsent = "";
				//if(sent.trim().length()>0){
				String[] splits = sent.split("##");
				String modifier = splits[0];
				String tag = splits[1];
				sent = splits[2].trim().replaceAll("\\b("+this.ignoredstrings+") ", "");//must use space at the end for "i . e ." to match
				taggedsent = markASentence(source, modifier, tag.trim(), sent);
				//}

				//	System.out.println(taggedsent);
				sentences.put(source, taggedsent);
				try{
					Statement stmt1 = conn.createStatement();
					ResultSet rs = stmt1.executeQuery("select sentid, type from "+this.tableprefix+"_sentence where source='"+source+"'");
					if(rs.next()){
						int id = rs.getInt("sentid");
						String type = rs.getString("type");
						stmt1.execute("insert into "+this.tableprefix+"_markedsentence (sentid, source, markedsent, type) values("+id+",'"+source+"', '"+taggedsent+"', '"+type+"')");
					}
				}catch(Exception e){
					e.printStackTrace();
				}

			}
		}
		return sentences;
	}

	protected void loadMarked() {
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select source, markedsent from "+this.tableprefix+"_markedsentence");
			while(rs.next()){
				String source = (String)rs.getString("source");
				String taggedsent = (String)rs.getString("markedsent"); 
				sentences.put(source, taggedsent); //do this in addClause
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public String markASentence(String source, String modifier, String tag, String sent) throws ClassNotFoundException, SQLException {


		String taggedsent = markthis(source, sent, organnames, "<", ">");
		taggedsent = markthis(source, taggedsent, statenames, "{", "}");
		taggedsent = taggedsent.replaceAll("[<{]or[}>]", "or"); //make sure to/or are left untagged
		taggedsent = taggedsent.replaceAll("[<{]to[}>]", "to");
		//remove "<>" for <{spine}>-{tipped}  =>spine-{tipped} or {spine}-{tipped}
		if(taggedsent.indexOf(">-")>=0){
			taggedsent = taggedsent.replaceAll(">-", "#-").replaceAll("<(?=\\S+#)", "").replaceAll("#", "");
		}
		if(this.fixadjnn && this.adjnounslist!=null){
			//if((adjnounsent.containsKey(tag)&& taggedsent.matches(".*?[<{]*\\b(?:"+adjnounslist+")\\b[}>]*.*")) || taggedsent.matches(".*? of [<{]*\\b(?:"+adjnounslist+")\\b[}>]*.*")){
			if((adjnounsent.containsKey(tag)&& taggedsent.matches(".*?[<{]*\\b(?:"+adjnounslist+")[^ly ]*\\b[}>]*.*")) || taggedsent.matches(".*? of [<{]*\\b(?:"+adjnounslist+")[^ly ]*\\b[}>]*.*")){
				taggedsent = fixInner(source, taggedsent, tag.replaceAll("\\W",""));//need to put tag in after the modifier inner
			}
			//including modifiers results in nouns are added to state adjs.
			//if(adjnounsent.containsKey(modifier) && taggedsent.matches(".*?[<{]*\\b(?:"+adjnounslist+")\\b[}>]*.*") ){
			//	taggedsent = fixInner(source, taggedsent, modifier, true);//@TODO: debug: need to put tag in after the modifier inner
			//}
		}

		//fix cases such as {dorsal} and <{anal}> <fins> => <dorsal> <fins> and <anal> <fins>: "dorsal and anal fins"
		if(taggedsent.matches(".*?\\}>? and .*")){
			String sentcopy = taggedsent;
			boolean changed = false;
			Pattern p = Pattern.compile("(.*?)<?\\{(\\w+)\\}>? and ([^\\d]*) (<\\w+> *)+(.*)");
			Matcher m = p.matcher(taggedsent);
			while(m.matches()){
				String lead = m.group(1).trim()+ " ";
				String m1 = m.group(2).trim();
				String m2 = m.group(3).trim();
				String organ = taggedsent.substring(m.end(3), m.start(5)).trim();
				String rest = m.group(5);
				if(Utilities.isPosition(m1, conn, this.glosstable) && m2.matches("(<?\\{\\w+\\}>? *)+")){ //m2 can not have numbers, puncts, or stopword/prep
					taggedsent = lead +"{"+ m1 +"} " + organ + " and " +m2+" "+organ +" "+ rest;
					m = p.matcher(taggedsent);
					changed = true;
				}else{
					taggedsent = lead +"{"+ m1 +"}### and " +m2+" "+organ +" "+ rest; //avoid infinite loop
					m = p.matcher(taggedsent);
				}
			}
			taggedsent = taggedsent.replaceAll("\\}###", "}");
			if(changed){
				System.out.println("before inserting organ: "+sentcopy);
				System.out.println("after inserting organ: "+taggedsent);
			}
		}

		//fix cases such as basi_ and hypobranchial => basibranchial and hypobranchial

		if(taggedsent.matches("\\{?("+termprefix+")\\}?-.*")){
			taggedsent = normalizePrefix(taggedsent); //basi_ and hypobranchial => basibranchial and hypobranchial
		}

		return taggedsent;
	}

	/**
	 * mark Inner as organ for sent such as inner red.
	 * @param adjnouns
	 * @param taggedsent
	 * @return
	 */
	private String fixInner(String source, String taggedsent, String tag) {
		this.showOutputMessage("System is rewriting some sentences...");
		String fixed = "";
		String copysent = taggedsent;
		boolean needfix = false;
		boolean changed = true;
		//Pattern p =Pattern.compile("(.*?)(\\s*(?:[ <{]*\\b(?:"+adjnounslist+")\\b[}> ]*)+\\s*)(.*)");
		//Pattern p0 =Pattern.compile("(.*?)((?:^| )(?:(?:\\{|<\\{)*\\b(?:"+adjnounslist+")\\b(?:\\}>|\\})*) )(.*)");
		//Pattern p =Pattern.compile("(.*?)((?:^| )(?:(?:\\{|<\\{)*\\b(?:"+adjnounslist+")[^ly ]*\\b(?:\\}>|\\})*)\\s+)(.*)");
		Pattern p =Pattern.compile("(.*?)((?:^| )(?:(?:\\{|<\\{)*\\b(?:"+adjnounslist+")[^ly ]*\\b(?:\\}>|\\})*)\\s+)(((?!to\\s+\\D).*).*)");
		Matcher m = p.matcher(taggedsent);
		//Matcher m0 = p0.matcher(taggedsent);
		int matchcount = 0;
		while(m.matches() && changed){
			changed = false;
			matchcount++;
			String before = m.group(1);
			String inner = m.group(2);
			String after = m.group(3);
			//TODO: may be after should not start with "to" : proximal to heads tocheck: 3/30/11
			if(!before.trim().endsWith(">") &&!after.trim().startsWith("<")){//mark inner as organ
				if(before.trim().endsWith("of")&& before.lastIndexOf("<")>=0){ //"apices of inner" may appear at the main structure is mentioned, in these cases, matchcount>1					
					String organ = before.substring(before.lastIndexOf("<"));
					if(copysent.startsWith(organ)){
						tag = getParentTag(source);//tag may be null, remove before return
					}
					organ = organ.replaceFirst("\\s*of\\s*$", "").replaceAll("\\W", "");
					if(TermOutputerUtilities.toSingular(organ).compareTo(tag)==0 || 
							(organ.matches("(apex|apices)") && tag.compareTo("base")==0)){
						String b = source.substring(0, source.indexOf("-")+1);
						String nsource = b +(Integer.parseInt(source.substring(source.indexOf("-")+1))-1);
						tag = getParentTag(nsource);
					}
				}
				String copyinner = inner.trim();
				inner = copyinner.replaceAll("[<{}>]", "").replaceAll("\\s+", "} {").replaceAll("\\{and\\}", "and").replaceAll("\\{or\\}", "or");
				//inner = "<"+inner+">";
				//inner = "{"+inner+"} <"+tag+">";
				fixed +=before+" "+"{"+inner+"} ";
				//taggedsent = matchcount==1 && !before.trim().endsWith("of")? " "+after : "#<"+tag+">#"+" "+after;
				if(after.matches("^\\d\\s*/\\s*\\d.*")){//proximal 1 / 2
					taggedsent = " "+after;
				}else if(inner.endsWith("er") && after.startsWith("than")){
					taggedsent = " "+after;
				}else if(before.trim().endsWith("of")){
					taggedsent = "<"+tag+">"+" "+after;
				}else if(matchcount==1 && copysent.startsWith(copyinner)){
					taggedsent = " "+after;
				}else{
					int start = fixed.lastIndexOf(">")>=0? fixed.lastIndexOf(">") : 0;
					String segment = fixed.substring(start).trim();
					if(segment.indexOf(",")<0 && !segment.startsWith("and")){
						taggedsent = " "+after;
					}else{
						taggedsent = "<"+tag+">"+" "+after;
					}
				}
				needfix = true;
				changed = true;
			}
			//fixed +=before+" ";
			//taggedsent = inner+" "+after;
			m = p.matcher(taggedsent);
			//fixed = before+" "+inner+" "+after; //{outer} {pistillate}
			//m = p.matcher(fixed);
		}
		fixed +=taggedsent;
		if(needfix){
			//System.out.println("fixed "+fixedcount+":["+source+"] "+fixed);
			fixedcount++;
		}
		if(fixed.trim().length()<1){
			fixed = taggedsent;
		}
		return fixed.replaceAll("\\s+", " ").replaceAll("<null>", "");
	}

	private String getParentTag(String source) {
		String tag = null;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select sentid from "+this.tableprefix+"_sentence where source='"+source+"'");
			if(rs.next()){
				int sentid = rs.getInt("sentid");
				sentid = sentid+1;
				do{
					sentid--;
					rs = stmt.executeQuery("select tag from "+this.tableprefix+"_sentence where sentid <"+sentid+" order by sentid desc limit 1");
					if(rs.next()){
						tag = (String)rs.getString("tag").replaceAll("\\W", ""); 	
					}
				}while(tag.compareTo("ditto")==0);

			}
			rs.close();
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return tag;
	}

	/**
	 * retag {caline} 10 to <caline> 10 when an adjnoun does not follow an organ or proceeds  an organ.
	 * @param adjnouns
	 * @param taggedsent
	 * @return
	 */
	private String fixAdjNouns(/*ArrayList<?> adjnouns,*/ String adjnoun, String taggedsent) {
		adjnoun = adjnoun.replaceAll("\\s+", "\\\\W+");
		taggedsent = Pattern.compile("[<{]*\\b"+adjnoun+"\\b[}>]*", Pattern.CASE_INSENSITIVE).matcher(taggedsent).replaceFirst("<"+adjnoun+">").replaceAll("W\\+", "> <").replaceAll("<and>", "and").replaceAll("<or>", "or");
		return taggedsent;
	}

	public static String markthis(String source, String sent, String parts, String leftmark, String rightmark) {
		//no need if select sentence (vs. originalsent)
		//remove ()
		//sent = sent.replaceAll("\\(.*?\\)", "");
		//remove (text)
		//sent = sent.replaceAll("\\(\\s+(?![\\d\\�\\-\\�]).*?(?<![\\d\\�\\-\\�])\\s+\\)", "");

		sent = sent.replaceAll("(?<=\\w)\\s+(?=[,\\.;:])", "");

		//sent = sent.replaceAll("_", "-"); //keep _ so phrases are treated as one word

		Pattern tagsp = Pattern.compile("(.*?)\\b("+parts+")\\b(.*)", Pattern.CASE_INSENSITIVE);
		//System.out.println(parts);
		String taggedsent = "";
		Matcher m = tagsp.matcher(sent);
		while(m.matches()){
			taggedsent += m.group(1)+leftmark+m.group(2)+rightmark;
			sent = m.group(3);
			m = tagsp.matcher(sent);
		}
		taggedsent +=sent;

		String tsent = "";
		Pattern p = Pattern.compile("(.*\\}-)(\\w+)(.*)");
		m = p.matcher(taggedsent);
		while(m.matches()){
			tsent += m.group(1)+"{"+m.group(2)+"}";
			taggedsent = m.group(3);
			m = p.matcher(taggedsent);			
		}
		tsent +=taggedsent;
		tsent = tsent.replaceAll("\\}-\\{", "-"); // => {oblong}-{ovate} :  {oblong-ovate}
		/*p = Pattern.compile("(.*?<[^>]*) ([^<]*>.*)");//<floral cup> => <floral-cup>
		m = p.matcher(tsent);
		while(m.matches()){
			tsent = m.group(1)+"-"+m.group(2);
			m = p.matcher(tsent);
		}*/
		tsent = tsent.replaceAll("\\s*,\\s*", " , ");
		tsent = tsent.replaceAll("\\s*\\.\\s*", " . ");
		tsent = tsent.replaceAll("\\s*;\\s*", " ; ");
		tsent = tsent.replaceAll("\\s*:\\s*", " : ");
		tsent = tsent.replaceAll("\\s*\\]\\s*", " ] ");
		tsent = tsent.replaceAll("\\s*\\[\\s*", " [ ");
		//tsent = tsent.replaceAll("\\s*\\)\\s*", " ) ");
		//tsent = tsent.replaceAll("\\s*\\(\\s*", " ( ");
		tsent = tsent.replaceAll("\\s+", " ").trim();		
		return tsent;
	}

	protected String collectStateNames(){
		String statestring = "";
		try{
			Statement stmt = conn.createStatement();

			//ResultSet rs = stmt.executeQuery("select word from "+this.tableprefix+"_wordpos where pos ='b'");
			ResultSet rs = stmt.executeQuery("select word from "+this.tableprefix+"_wordroles where semanticrole ='c' ");

			while(rs.next()){
				String w = rs.getString("word");
				if(!w.matches("\\W+") && !w.matches("("+ChunkedSentence.stop+")") &&!w.matches("("+ChunkedSentence.prepositions+")")){
					statestring += "|"+ w; 
				}
			}

			/*wordroles only holds word not in glossary, so need to use glossary to mark a sentence as well.*/
			rs = stmt.executeQuery("select distinct term from "+this.glosstable+" where category not in ('STRUCTURE', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative', 'life_style')");
			while(rs.next()){
				String term = rs.getString("term").trim();
				if(term == null){continue;}
				term = term.indexOf(" ")> 0? term.substring(term.lastIndexOf(' ')+1) : term;
				if(!statestring.matches(".*\\b"+term+"\\b.*") &&  !term.matches("("+ChunkedSentence.stop+")") &&!term.matches("("+ChunkedSentence.prepositions+")"))
					statestring+=("|"+ term);
			}
		}catch (Exception e){
			e.printStackTrace();
		}
		return statestring.replaceAll("\\b(and|or|to)\\b", "").replaceAll("\\\\d\\+", "").trim().replaceFirst("^\\|", "").replaceFirst("\\|$", "").replaceAll("\\|+", "|");
	}

	protected String collectOrganNames(){
		StringBuffer tags = new StringBuffer();
		try{
			Statement stmt = conn.createStatement();
			organNameFromGloss(tags, stmt);
			organNameFromSentences(tags, stmt);
			organNameFromPlNouns(tags, stmt);

			tags = tags.replace(tags.lastIndexOf("|"), tags.lastIndexOf("|")+1, "");

		}catch(Exception e){
			e.printStackTrace();
		}
		return tags.toString().replaceAll("\\b\\d+\\b", "").replaceAll("\\|+", "|");
	}


	protected void organNameFromPlNouns(StringBuffer tags, Statement stmt)
			throws SQLException {
		ResultSet rs;
		String wordroletable = this.tableprefix + "_"+ApplicationUtilities.getProperty("WORDROLESTABLE");
		rs = stmt.executeQuery("select word from "+wordroletable+" where semanticrole in ('op', 'os')");
		while(rs.next()){
			String w = rs.getString("word").trim();
			if(!w.matches("("+ChunkedSentence.stop+")") &&!w.matches("("+ChunkedSentence.prepositions+")"))
				tags.append(w+"|");
		}
		/*
		String postable = this.tableprefix + "_"+ApplicationUtilities.getProperty("POSTABLE");
		rs = stmt.executeQuery("select word from "+postable+" where pos in ('p', 's', 'n') and word not in (select word from "+wordroletable+" where semanticrole in ('op', 'os'))");// and word not in (select term from "+this.glosstable+" where category ='life_style')");
		while(rs.next()){
			tags.append(rs.getString("word").trim()+"|");
		}*/
	}
	/**
	 * collect adj-noun structures such as "inner" as structure name
	 * @param tags
	 * @param stmt
	 * @throws SQLException
	 */
	protected void organNameFromSentences(StringBuffer tags, Statement stmt)
			throws SQLException {
		ResultSet rs;

		/*tag terms are already in WORDROLES
		 * rs = stmt.executeQuery("select distinct tag from sentence where tag not like '% %'");
		while(rs.next()){
			String tag = rs.getString("tag");
			if(tag == null || tag.indexOf("[")>=0|| tags.indexOf("|"+tag+"|") >= 0){continue;}
			tags.append(tag+"|");
		}*/

		rs = stmt.executeQuery("select modifier, tag from "+this.tableprefix+"_sentence where tag  like '[%]'"); //inner [tepal]
		while(rs.next()){
			String m = rs.getString("modifier");
			m = m.replaceAll("\\[^\\[*\\]", ""); 
			if(m.compareTo("")!= 0){
				String tag = null;
				if(m.lastIndexOf(" ")<0){
					tag = m;
				}else{
					tag = m.substring(m.lastIndexOf(" ")+1); //last word from modifier
				}
				if(tag == null ||tag.indexOf("[")>=0|| tags.indexOf("|"+tag+"|") >= 0 || tag.indexOf("[")>=0 || tag.matches(".*?(\\d|"+ChunkedSentence.stop+"|"+ChunkedSentence.prepositions+").*")){continue;}
				tags.append(tag+"|");
			}
		}
	}

	protected void organNameFromGloss(StringBuffer tags, Statement stmt)
			throws SQLException {
		ResultSet rs = stmt.executeQuery("select distinct term from "+this.glosstable+" where category in ('STRUCTURE', 'FEATURE', 'SUBSTANCE', 'PLANT', 'nominative', 'structure')");
		while(rs.next()){
			String term = rs.getString("term").trim();
			if(term == null){continue;}
			term = term.indexOf(" ")> 0? term.substring(term.lastIndexOf(' ')+1) : term;
			if(!term.matches("("+ChunkedSentence.stop+")") &&!term.matches("("+ChunkedSentence.prepositions+")"))
				tags.append(term+"|");
		}
	}

	protected String colorsFromGloss()
			throws SQLException {
		StringBuffer colors = new StringBuffer();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery("select distinct term from "+this.glosstable+" where category in ('coloration', 'color')");
		while(rs.next()){
			String term = rs.getString("term").trim();
			if(term == null){continue;}
			term = term.indexOf(" ")> 0? term.substring(term.lastIndexOf(' ')+1) : term;
			colors.append(term+"|");
		}
		return colors.toString().replaceFirst("\\|$", "");
	}

	private void resetOutputMessage() {
		if(display==null)return;
		display.syncExec(new Runnable() {
			public void run() {
				if(charLog!=null) charLog.setText("");
			}
		});
	}

	private void showOutputMessage(final String message) {
		if(display==null)return;
		display.syncExec(new Runnable() {
			public void run() {
				if(charLog!=null) charLog.append(message+"\n");
			}
		});
	}

	/*
	 * Handles the compound prepositions
	 */
	private String stringCompoundPP(String text) {
		boolean did = false;
		String result = "";
		Matcher m = compreppattern.matcher(text);
		while(m.matches()){
			String linked = m.group(2).replaceAll("\\s+", "-");
			result += m.group(1)+ linked;
			text = m.group(3);
			m = compreppattern.matcher(text);
			did = true;
		}
		result += text;
		if(did && printCompoundPP ) System.out.println("[result]:"+result);
		return result;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Connection conn = null;
		//String database="fnav19_benchmark";
		//String database="treatiseh_benchmark";
		//String database="plaziants_benchmark";//TODO
		//String database="annotationevaluation";
		//String database ="phenoscape";
		

		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			}

			//SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, "pltest", "antglossaryfixed", false);
			//SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, "fnav19", "fnaglossaryfixed", true);
			//SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, "treatiseh", "treatisehglossaryfixed", false);
			SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, ApplicationUtilities.getProperty("table.prefix"), "fishglossaryfixed", true, null, null);
			//SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, "plazi_ants_clause_rn", "antglossary");
			//SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, "bhl_clean", "fnabhlglossaryfixed");
			sosm.markSentences();
			
			}catch(Exception e){
				e.printStackTrace();
			}
		
		}

}
