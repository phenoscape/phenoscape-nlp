package fna.parsing.character;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;

//interface to the FNA glossary
//read the glossary into a database
public class Glossary {
	
	private static final Logger LOGGER = Logger.getLogger(Glossary.class);
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in CharacterLearner" + e);
			e.printStackTrace();
		}
	}
	private static String tablename = null;
	//private static String tablename1 = null;
	//private String database;
	static private Connection conn = null;
	
	
	public Glossary(String glosstable){
		Glossary.tablename = glosstable;
		
		/*this.tablename = tablePrefix+"_fnaglossary";
		this.tablename1 = tablePrefix+"_termforms";
		try{
			if(conn == null){
				this.database = databasename;
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("show tables");
				boolean g = false;
				while(rs.next()){
					if(rs.getString(1).compareToIgnoreCase(tablename)==0){
						g=true;
					}
				}
				stmt.execute("create table if not exists "+tablename1+" (id int not null auto_increment primary key, term1 varchar(100), type varchar(10), term2 varchar(100))");
				stmt.execute("delete from "+tablename1);				
				if(!g){
					stmt.execute("create table "+tablename+" (id int not null auto_increment primary key, term varchar(100), category varchar(100), limitation varchar(200), status varchar(50), definition varchar(2000))");
					populateTable(glosstable);//TODO: if tablename exist, do not populate it again
				}				
			}
		}catch(Exception e){
			LOGGER.error("Exception in CharacterLearner constructor" + e);
			e.printStackTrace();
		}*/
	}

	/*
	private void populateTable(File glossfile){
		try{
			FileReader fr = new FileReader(glossfile);
			LineNumberReader lnr = new LineNumberReader(fr);
			String line = null;
			line = lnr.readLine();
			ArrayList synterms = new ArrayList();
			while((line = lnr.readLine()) != null){
				String[] cols = line.split("\t");
				String term = cols[0]; //var., pl., sing., amphistom(at)ic, bract 2 (strict sense)etc
				String rel = cols[1]; // = bundled (not recommended), fascicled
				String cat = cols[2];
				String lim = cols[3];
				String def = cols[4];
				String status = "";
				ArrayList terms = new ArrayList();
				boolean flag = false;
				Pattern p = Pattern.compile("(.*?)(not recommended|broad sense|strict sense|misapplied)(.*)");
				do{
					flag = false;
					Matcher m = p.matcher(term);
					if(m.matches()){
						status += m.group(2)+" ";
						flag = true;
						term = m.group(1)+m.group(3);
					}
				}while(flag);
				
				p = Pattern.compile("(.*?)\\s+\\((\\w.*?)\\)");// sheath (leaf)
				Matcher m = p.matcher(term);
				if(m.matches()){
					term = m.group(1);
					lim += " "+m.group(2);
				}
								
				//String [] tms = term.split("(sing\\.|var\\.|pl\\.|,)\\s+");//embryotega  pl. embryotegae, embryotegas
				
				String [] tms = termForms(term);
				for(int i = 0; i < tms.length; i++){
					p= Pattern.compile("(.*?\\S)\\((\\w+)\\)(.*?)"); //epistorm(at)ic
					m = p.matcher(tms[i]);
					if(m.matches()){
						String term1 = m.group(1)+m.group(3);
						String term2 = m.group(1)+m.group(2)+m.group(3);
						terms.add(term1);
						terms.add(term2);
					}else{
						terms.add(tms[i]);
					}
				}
				//terms hold terms ...cleft, ribbed 1, poly..., bundle ()
				def = def.replaceAll("\"", "'");
				def = def.trim();
				lim = lim.trim();
				cat = cat.trim();
				status = status.trim();
				
				Iterator it = terms.iterator();
				while(it.hasNext()){
					String t = (String)it.next();
				    t = t.replace("...", "_");
				    t = t.replaceAll("-_", "_");
				    t = t.replaceAll("_-", "_");
				    t = t.replaceAll("[\\d()]", " ");
				    t = t.replaceFirst(",\\s*$", "");
				    t = t.trim();
					if(t.compareTo("") != 0){
						Statement stmt = conn.createStatement();
						String query = "insert into "+tablename+" (term, category, limitation, status, definition) values (\""+t+"\", \""+cat+"\", \""+lim+"\", \""+status+"\",\""+def+"\")";
						stmt.execute(query);
						if(rel.indexOf("[") < 0){
							rel = "["+cat+"]"+rel;
							synterms.add(rel); //to process later
						}
					}
				}
			}
			
			//process synterms, add the ones that are not in the table already
			Iterator it = synterms.iterator();
			Pattern p = Pattern.compile("(.*?)(not recommended|broad sense|strict sense|misapplied)(.*)");
			Pattern p1= Pattern.compile("(.*?\\S)\\((\\w+)\\)(.*?)"); //epistorm(at)ic
			while(it.hasNext()){
				String synstring = (String)it.next();
				String cat = synstring.substring(synstring.indexOf("[")+1,synstring.indexOf("]"));
				synstring = synstring.substring(synstring.indexOf("]")+1);
				String[] syns = synstring.split("[,;]");
				for(int i = 0; i < syns.length; i++){
					String status = "";
					boolean flag = false;
					do{
						flag = false;
						Matcher m = p.matcher(syns[i]);
						if(m.matches()){
							status += m.group(2)+" ";
							flag = true;
							syns[i] = m.group(1)+m.group(3);
						}
					}while(flag);
					syns[i] = syns[i].replaceAll("[=<>]", "");
					syns[i] = syns[i].replaceAll("Å", "");
					syns[i] = syns[i].replace("...", "_");
					syns[i] = syns[i].replace("_-", "_");
					syns[i] = syns[i].replace("-_", "_");
					syns[i] = syns[i].trim();
					
					Matcher m = p1.matcher(syns[i]);
					if(m.matches()){
						String term1 = m.group(1)+m.group(3);
						String term2 = m.group(1)+m.group(2)+m.group(3);
						addSyn(cat, term1, status);
						addSyn(cat, term2, status);
					}else{
						addSyn(cat, syns[i], status);
					}
				}
			}
		}catch(Exception e){
			LOGGER.error("Exception in CharacterLearner populateTable" + e);
			e.printStackTrace();
		}
	}*/
	/**
	 * parse term string to populate termforms table, return all terms
	 * termforms: single, form type, the other form
	 * @param term
	 * @return
	 */
	/*
	private String [] termForms(String term){
		ArrayList<String> terms = new ArrayList<String>();
		String types = "(?:sing\\.|var\\.|pl\\.)"; //embryotega  pl. embryotegae, embryotegas
		Pattern p = Pattern.compile("(.+?)("+types+".*)");
		Matcher m = p.matcher(term);
		if(m.matches()){
			String t1 = m.group(1).trim();
			String ts = m.group(2).trim();
			terms.add(t1);
			ts = ts.replaceAll("sing\\.", "[sing]").replaceAll("pl\\.", "[pl]").replaceAll("var\\.", "[var]");
			String[] parts = ts.split("\\[");
			for(int i = 1; i < parts.length; i++){
				String [] part = parts[i].split("\\]\\s*");
				String[] term2s = part[1].split("\\s*,\\s*");
				for(int j = 0; j < term2s.length; j++){
					add2TermForms(t1, part[0], term2s[j]);
					terms.add(term2s[j]);
				}
			}
		}else{
			terms.add(term);
		}
		return (String[])terms.toArray(new String[]{});
	}
	
	private void add2TermForms(String term1, String type, String term2){
		if(!term1.matches("\\s+[2-9]")){
			
			term1 = term1.replace("()", "").trim().replaceFirst("\\s+\\d+$", "");
			term2 = term2.replace("()", "").trim();
			if(type.equals("sing")){
				String t = term1;
				term1 = term2;
				term2 = t;
				type = "pl";
			}

			String[] term1s = new String[2];
			if(term1.matches(".*?\\(\\w+\\)")){
				term1s[0] = term1.replaceFirst("\\(\\w+\\)", "");
				term1s[1] = term1.replaceAll("[)(]", "");
			}else{
				term1s = new String[1];
				term1s[0] = term1;
			}
			for(int i = 0; i < term1s.length; i++){
				try{
					Statement stmt = conn.createStatement();
					stmt.execute("insert into "+tablename1+ "(term1, type, term2) values ('"+term1s[i]+"', '"+type+"', '"+term2.trim()+"')");
				}catch(Exception e){
					LOGGER.error("Exception in CharacterLearner add2TermForms" + e);
					e.printStackTrace();
				}
			}
			
		}
	}
	

	private void addSyn(String cat, String syn, String status)
			throws SQLException {
		Pattern p = Pattern.compile("(.*?)\\s+\\((\\w.*?)\\)");// sheath (leaf)
		Matcher m = p.matcher(syn);
		String lim = "";
		if(m.matches()){
			syn = m.group(1);
			lim += " "+m.group(2);
		}
		syn = syn.replaceAll("[()]", "").replaceFirst(",\\s*$", "");
		syn = syn.trim();
		if(syn.compareTo("") != 0){
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select id from "+tablename+" where term =\"" + syn + "\"");
			if(!rs.next()){
				String query = "insert into "+tablename+" (term, category, limitation, status) values (\""+syn+"\", \""+cat+"\", \""+lim+"\", \""+status+"\")";
				stmt.execute(query);
			}
		}
	}
	*/

	public static ArrayList<String> getCharacter(String state){
		ArrayList<String> chs = new ArrayList<String>();
		try{
			Statement stmt = conn.createStatement();
			String query = "select distinct category from "+tablename+" where term =\""+state+"\"";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				chs.add(rs.getString("category"));
			}
		}catch(Exception e){
			LOGGER.error("Exception in CharacterLearner getCharacter" + e);
			e.printStackTrace();
		}
		return chs;
	}
	
	public static String getAllCharacters(){
		StringBuffer chs = new StringBuffer();
		try{
			Statement stmt = conn.createStatement();
			String query = "select distinct term from "+tablename +" where category not in ('STRUCTURE / SUBSTANCE','STRUCTURE', 'CHARACTER', 'SUBSTANCE', 'PLANT')";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				chs.append(rs.getString("term").trim()+"|");
			}
		}catch(Exception e){
			LOGGER.error("Exception in CharacterLearner getAllCharacters" + e);
			e.printStackTrace();
		}
		return chs.toString().replaceFirst("\\|$", "");
	}

	public static void addInducedPair(String term, ArrayList<?> categories){
		Iterator<?> it = categories.iterator();
		while(it.hasNext()){
			String cat = (String)it.next();
			try{
				Statement stmt = conn.createStatement();
				String query = "insert into "+tablename+" (term, category, status) values (\""+term+"\", \""+cat+"\", \"learned\" )";
				stmt.execute(query);
			}catch(Exception e){
				LOGGER.error("Exception in CharacterLearner addInducedPair" + e);
				e.printStackTrace();
			}
		}
	}


	public static void main(String[] argv){
		//load glossary table directly
		//File glossfile = new File("C://Documents and Settings//hongcui//Desktop//WorkFeb2008//FNA//FNAGloss.txt");
		//Glossary g = new Glossary(glossfile, true, "fnav5_corpus", "fna");
	}
}
