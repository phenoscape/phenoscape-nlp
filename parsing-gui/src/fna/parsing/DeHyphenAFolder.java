/**
 * 
 */
package fna.parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import fna.parsing.character.Glossary;

/**
 * @author hongcui
 * Move the dyhypen() function from VolumeDehypenizer, to make DeHyphenAFolder a utility class that can be called by other projects.
 */
@SuppressWarnings("unchecked")
public class DeHyphenAFolder {
	private ProcessListener listener;
	private String database;
	@SuppressWarnings("unused")
	private VolumeDehyphenizer vd;
	@SuppressWarnings("unused")
	private String dataPrefix;
	private String tablename;
	private Glossary glossary;
	private File folder;
	private File outfolder;
	private static final Logger LOGGER = Logger.getLogger(DeHyphenAFolder.class);
	private Connection conn;
    //static public String num = "\\d[^a-z]+";
    private Hashtable<String,String> mapping = new Hashtable<String, String>();
    private String glossarytable;
	/**
	 * 
	 */
	public DeHyphenAFolder(ProcessListener listener, String workdir, 
    		String todofoldername, String database, VolumeDehyphenizer vd, String dataPrefix, String glossarytable, Glossary glossary) {
		this.listener = listener;
		this.glossarytable = glossarytable;
        this.database = database;
        this.vd = vd;
        this.dataPrefix = dataPrefix;
        this.tablename = dataPrefix+"_"+ApplicationUtilities.getProperty("ALLWORDS");
        
        this.glossary = glossary;
        workdir = workdir.endsWith("/")? workdir : workdir+"/";
        this.folder = new File(workdir+todofoldername);
        this.outfolder = new File(workdir+ApplicationUtilities.getProperty("DEHYPHENED"));
        if(!outfolder.exists()){
            outfolder.mkdir();
        }
        
        try{
            if(conn == null){
                Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
                String URL = ApplicationUtilities.getProperty("database.url");
                conn = DriverManager.getConnection(URL);
                //createNumTextMixTable();
                createAllWordsTable();
            }
        }catch(Exception e){
        	LOGGER.error("Database is down! (VolumeDehyphenizer)", e);
            e.printStackTrace();
        }
	}
	
	   public boolean dehyphen(){

		   if(listener!= null) listener.progress(1);
		   vd.showPerlMessage("Checking files...\n");
		   if(hasProblems()){
			   vd.showPerlMessage("");
			   vd.showPerlMessage("Files with problems are listed above. \n");
			   vd.showPerlMessage("Run this step again after the above identified problems are corrected.\n");
			   listener.progress(0);
			   return false;
		   }else{
			   vd.showPerlMessage("File checking completed. \n");
			   listener.progress(5);
			   vd.showPerlMessage("Pre-processing files... \n");
		       fillInWords();
		       if(listener!= null) listener.progress(50);
	
		       DeHyphenizer dh = new DeHyphenizerCorrected(this.database, this.tablename, "word", "count", "-", this.glossarytable, glossary);
	
		       try{
		            Statement stmt = conn.createStatement();
		            ResultSet rs = stmt.executeQuery("select word from "+tablename+" where word like '%-%'");
		            while(rs.next()){
		                String word = rs.getString("word");
		                String dhword = dh.normalFormat(word).replaceAll("-", "_"); //so dhwords in _allwords table are comparable to words in _wordpos and other tables.
		                Statement stmt1 = conn.createStatement();
		                stmt1.execute("update "+tablename+" set dhword ='"+dhword+"' where word='"+word+"'");
		                mapping.put(word, dhword);
		            }
		            stmt.execute("update "+tablename+" set dhword=word where dhword is null");
		       }catch(Exception e){
		        	LOGGER.error("Problem in VolumeDehyphenizer:dehyphen", e);
		            e.printStackTrace();
		       }
		       normalizeDocument();
		       if(listener!= null) listener.progress(100);
		       return true;
		   }
	    }
	    
	   
	   /**
	    * 
	    * @return pass the check or not
	    */
	    private boolean hasProblems() {
        	boolean has = false;
        	int problemcount = 0;
	        try {
	            File[] flist = folder.listFiles();//description folder
	            for(int i= 0; i < flist.length; i++){
	                BufferedReader reader = new BufferedReader(new FileReader(flist[i]));
	                String line = null; 
	                StringBuffer sb = new StringBuffer();
	                while ((line = reader.readLine()) != null) {
	                    line = line.replaceAll(System.getProperty("line.separator"), " ");
	                    sb.append(line);
	                }
	                reader.close();
	                String text = sb.toString();
	                //check for unmatched brackets
	                if(hasUnmatchedBrackets(text)){
	                	has = true;
	                	vd.showPerlMessage((++problemcount)+": "+flist[i].getAbsolutePath()+" contains unmatched brackets in \""+text+"\"\n");
	                }
	                //check for missing spaces between text and numbers: 
	                if(text.matches(".*[a-zA-Z]\\d.*") || text.matches(".*\\d[a-zA-Z].*")){
	                	//has =true; //ant descriptions contain "Mf4"
	                 	//vd.showPerlMessage((++problemcount)+": "+flist[i].getAbsolutePath()+" misses a space between a word and a number in \""+text+"\"\n");      	       
	                }
	                //check for (?)
	                if(text.matches(".*?\\(\\s*\\?\\s*\\).*")){
	                	has =true;
	                 	vd.showPerlMessage((++problemcount)+": "+flist[i].getAbsolutePath()+" contains expression (?) in \""+text+"\"\n");  
	                 	vd.showPerlMessage("Change (?) to an text expression such as (not certain)");
	                }
	            }
	            File cfolder = new File(folder.getParentFile(), "characters");
	            flist = cfolder.listFiles();//description folder
	            for(int i= 0; i < flist.length; i++){
	                BufferedReader reader = new BufferedReader(new FileReader(flist[i]));
	                String line = null; 
	                StringBuffer sb = new StringBuffer();
	                while ((line = reader.readLine()) != null) {
	                    line = line.replaceAll(System.getProperty("line.separator"), " ");
	                    sb.append(line);
	                }
	                reader.close();
	                String text = sb.toString();
	                //check for unmatched brackets
	                if(hasUnmatchedBrackets(text)){
	                	has = true;
	                	vd.showPerlMessage((++problemcount)+": "+flist[i].getAbsolutePath()+" contains unmatched brackets in \""+text+"\"\n");
	                }
	                //check for missing spaces between text and numbers: 
	                if(text.matches(".*[a-zA-Z]\\d.*") || text.matches(".*\\d[a-zA-Z].*")){
	                	//has =true; //ant descriptions contain "Mf4"
	                 	//vd.showPerlMessage((++problemcount)+": "+flist[i].getAbsolutePath()+" misses a space between a word and a number in \""+text+"\"\n");      	       
	                }
	                //check for (?)
	                if(text.matches(".*?\\(\\s*\\?\\s*\\).*")){
	                	has =true;
	                 	vd.showPerlMessage((++problemcount)+": "+flist[i].getAbsolutePath()+" contains expression (?) in \""+text+"\"\n");  
	                 	vd.showPerlMessage("Change (?) to an text expression such as (not certain)");
	                }
	            }
	        }catch(Exception e){
	            	LOGGER.error("Problem in VolumeDehyphenizer:check4UnmatchedBrackets", e);
		            e.printStackTrace();
	        }
	        return has;
	    }

		private void createAllWordsTable(){
	        try{
	            Statement stmt = conn.createStatement();
	            stmt.execute("drop table if exists "+tablename);
	            String query = "create table if not exists "+tablename+" (word varchar(150) unique not null primary key, count int, dhword varchar(150), inbrackets int default 0)";
	            stmt.execute(query);	           
	        }catch(Exception e){
	        	LOGGER.error("Problem in VolumeDehyphenizer:createWordTable", e);
	            e.printStackTrace();
	        }
	    }
	    
	    /*private void createNumTextMixTable(){
	        try{
	            Statement stmt = conn.createStatement();
	            String query = "create table if not exists "+tablename1+" (id int not null auto_increment primary key, mix varchar(30), file varchar(400))";
	            stmt.execute(query);
	            stmt.execute("delete from "+tablename1);
	        }catch(Exception e){
	            e.printStackTrace();
	        }
	    }*/
	    /**
	     * check for unmatched brackets too.
	     */
	    private void fillInWords(){
	        try {
	            Statement stmt = conn.createStatement();
	            ResultSet rs = null;
	            File[] flist = folder.listFiles();
	            int total = flist.length;
	            for(int i= 0; i < flist.length; i++){
	                BufferedReader reader = new BufferedReader(new FileReader(flist[i]));
	                String line = null; 
	                StringBuffer sb = new StringBuffer();
	                while ((line = reader.readLine()) != null) {
	                    line = line.replaceAll(System.getProperty("line.separator"), " ");
	                    sb.append(line);
	                }
	                reader.close();
	                String text = sb.toString();
	                text = text.toLowerCase();
	                text = text.replaceAll("<[^<]+?>", " ");
	                text = text.replaceAll("\\d", " ");
	                text = text.replaceAll("\\(", " ( ");
	                text = text.replaceAll("\\)", " ) ");
	                text = text.replaceAll("\\[", " [ ");
	                text = text.replaceAll("\\]", " ] ");
	                text = text.replaceAll("\\{", " { ");
	                text = text.replaceAll("\\}", " } ");
	                text = text.replaceAll("\\s+", " ").trim();
                    String[] words = text.split("\\s+");
                    int lround = 0;
                    int lsquare = 0;
                    int lcurly = 0;
                    int inbracket = 0;
                    for(int j = 0; j < words.length; j++){
                        String w = words[j].trim();
                        if(w.compareTo("(")==0) lround++;
                        else if(w.compareTo(")")==0) lround--;
                        else if(w.compareTo("[")==0) lsquare++;
                        else if(w.compareTo("]")==0) lsquare--;
                        else if(w.compareTo("{")==0) lcurly++;
                        else if(w.compareTo("}")==0) lcurly--;
                        else{
                        	w = w.replaceAll("[^-a-z]", " ").trim();
                            if(w.matches(".*?\\w.*")){
                            	if(lround+lsquare+lcurly > 0){
                            		inbracket = 1;
                            	}else{
                            		inbracket = 0;
                            	}
	                            int count = 1;
	                            rs = stmt.executeQuery("select word, count, inbrackets from "+tablename+"  where word='"+w+"'");
	                            if(rs.next()){ //normal word exist
	                                count += rs.getInt("count");
	                                inbracket *= rs.getInt("inbrackets");
	                            }
	                            stmt.execute("delete from "+tablename+" where word ='"+w+"'");
	                            stmt.execute("insert into "+tablename+" (word, count, inbrackets) values('"+w+"', "+count+","+inbracket+")");
	                        }
                        }
                    }
                    listener.progress(5+i*45/total);

	                /*while ((line = reader.readLine()) != null) {
	                    line = line.toLowerCase();
	                    line = line.replaceAll("<[^<]+?>", " "); //for xml or html docs
	                    line = line.replaceAll(num, " ");
	                    line = line.replaceAll("[^-a-z]", " ");
	                    line = normalize(line);

	                    Statement stmt = conn.createStatement();
                        ResultSet rs = null;
	                    String[] words = line.split("\\s+");
	                    for(int j = 0; j < words.length; j++){
	                        String w = words[j].trim();
	                        if(w.matches(".*?\\w.*")){
	                            int count = 1;
	                            rs = stmt.executeQuery("select word, count from "+tablename+"  where word='"+w+"'");
	                            if(rs.next()){
	                                count = rs.getInt("count")+1;
	                            }
	                            stmt.execute("delete from "+tablename+" where word ='"+w+"'");
	                            stmt.execute("insert into "+tablename+" (word, count) values('"+w+"', "+count+")");
	                        }
	                    }
	                    rs.close();
	                    stmt.close();
	                }*/
	            }
                rs.close();
                stmt.close();
	        } catch (Exception e) {
	        	LOGGER.error("Problem in VolumeDehyphenizer:fillInWords", e);
	            e.printStackTrace();
	        }
	    }
	    private boolean hasUnmatchedBrackets(String text) {
	    	String[] lbrackets = new String[]{"\\[", "(", "{"};
	    	String[] rbrackets = new String[]{"\\]", ")", "}"};
	    	for(int i = 0; i<lbrackets.length; i++){
	    		int left1 = text.replaceAll("[^"+lbrackets[i]+"]", "").length();
	    		int right1 = text.replaceAll("[^"+rbrackets[i]+"]", "").length();
	    		if(left1!=right1) return true;
	    	}
			return false;
		}

		/**
	     * save original text mix in File source in a table,
	     * to be used in outputting final text
	     * @param mix
	     * @param source
	     * @return
	     */
	    /*private String fixNumTextMix(String mix, File source){
	        StringBuffer fixed = new StringBuffer();
	        Pattern p = Pattern.compile("(.*?)(\\d+-)([a-z].*)");
	        Matcher m = p.matcher(mix);
	        while(m.matches()){
	            fixed.append(m.group(1)).append("NUM-");
	            String save = m.group(2)+m.group(3);
	            save = save.substring(0, save.length() < mixlength ? save.length() : mixlength );
	            //save to table
	            mix = m.group(3);
	            try{
	                Statement stmt = conn.createStatement();
	                stmt.execute("insert into "+tablename1+" (mix, file) values ('"+save+"', '"+source.getName()+"')");
	            }catch (Exception e){
	                e.printStackTrace();
	            }
	        }
	        fixed.append(mix);
	        return fixed.toString();
	    }*/
	    
	    @SuppressWarnings("unused")
		private String fixBrokenHyphens(String broken){ //cup-[,]  disc-[,]  or dish-shaped
	        StringBuffer fixed = new StringBuffer();
	        Pattern p = Pattern.compile("(.*?\\b)([a-z]+)-\\W[^\\.]*?[a-z]+-([a-z]+)(.*)");
	        Matcher m = p.matcher(broken);
	        while(m.matches()){
	            String begin = m.group(1);
	            String part = broken.substring(m.start(2), m.start(3));
	            broken = m.group(4);
	            String fix = m.group(3);
	            part = part.replaceAll("-(?!\\w)", "-"+fix);
	            fixed.append(begin+part);
	            m = p.matcher(broken);
	        }
	        fixed.append(broken);
	        return fixed.toString();
	    }
	    private void normalizeDocument(){
	        try {
	            File[] flist = folder.listFiles();
	            for(int i= 0; i < flist.length; i++){
	                BufferedReader reader = new BufferedReader(new FileReader(flist[i]));
	                String line = null; //DO NOT normalize case
	                StringBuffer sb = new StringBuffer();
	                while ((line = reader.readLine()) != null) {
	                    line = line.replaceAll(System.getProperty("line.separator"), " ");
	                    sb.append(line);
	                }
	                reader.close();
	                String text = sb.toString();
	                text = performMapping(text);
	                //turn "." that are in brackets as [.DOT.] for unsupervised learning pl.
	                text = text.replaceAll("\\(", " ( ");
	                text = text.replaceAll("\\)", " ) ");
	                text = text.replaceAll("\\[", " [ ");
	                text = text.replaceAll("\\]", " ] ");
	                text = text.replaceAll("\\{", " { ");
	                text = text.replaceAll("\\}", " } ");
	                text = text.replaceAll("\\s+", " ").trim();
	                int lround = 0;
	                int lsquare = 0;
	                int lcurly = 0;
	                sb = new StringBuffer();
                    String[] words = text.split("\\s+");
	                for(int j = 0; j < words.length; j++){
                        String w = words[j].trim();
                        if(w.compareTo("(")==0){
                        	lround++;
                        	sb.append("(");
                        }else if(w.compareTo(")")==0){
                        	lround--;
                        	sb.append(")");
                        }else if(w.compareTo("[")==0){
                        	lsquare++;
                        	sb.append("[");
                        }else if(w.compareTo("]")==0){
                        	lsquare--;
                        	sb.append("]");
                        }else if(w.compareTo("{")==0){
                        	lcurly++;
                        	sb.append("{");
                        }else if(w.compareTo("}")==0){
                        	lcurly--;
                        	sb.append("}");
                        }else{
                        	if(w.matches(".*?[.?;:!].*?") && (lround+lsquare+lcurly)>0){
                        		w = w.replaceAll("\\.", "[DOT]");
                        		w = w.replaceAll("\\?", "[QST]");
                        		w = w.replaceAll(";", "[SQL]");
                        		w = w.replaceAll(":", "[QLN]");
                        		w = w.replaceAll("!", "[EXM]");
                        	}
                        	sb.append(w+" ");                        	
                        }                        
	                }
	                text = sb.toString().replaceAll("\\s*\\(\\s*", "(").replaceAll("\\s*\\)\\s*", ")")
	                .replaceAll("(?<=[^0-9+–-])\\(", " (").replaceAll("\\)(?=[a-z])", ") ").trim();
	                //write back
	                File outf = new File(outfolder, flist[i].getName());
	                //BufferedWriter out = new BufferedWriter(new FileWriter(flist[i]));
	                BufferedWriter out = new BufferedWriter(new FileWriter(outf));
	                out.write(text);
	                out.close();
	                //System.out.println(flist[i].getName()+" dehyphenized");
	                vd.showPerlMessage(flist[i].getName()+" dehyphenized\n");
	            }
	        } catch (Exception e) {
	        	LOGGER.error("Problem in VolumeDehyphenizer:normalizeDocument", e);
	            e.printStackTrace();
	        }
	    }
	    /*
	    private String normalize(String text){
	        text = text.replaceAll("-+", "-");
	        
	        Pattern p = Pattern.compile("(.*?\\W)-(.*)"); //remove proceeding -
	        Matcher m = p.matcher(text);
	        while(m.matches()){
	            text = m.group(1)+" "+m.group(2); 
	            m = p.matcher(text);
	        }
	        
	        p = Pattern.compile("(.*?)-(\\W.*)"); //remove trailing
	        m = p.matcher(text);
	        while(m.matches()){
	            text = m.group(1)+" "+m.group(2);
	            m = p.matcher(text);
	        }
	        //text = text.replaceAll("\\W-", " "); 
	        //text = text.replaceAll("-\\W", " ");

	        return text;
	    }*/
	    
	    private String performMapping(String original){
	        Enumeration en = mapping.keys();
	        while(en.hasMoreElements()){
	            String hword = (String)en.nextElement();
	            String dhword = (String)mapping.get(hword);
	            //System.out.println("hword: "+hword +" dhword: "+dhword);
	            if(!hword.equals(dhword) && !hword.startsWith("-") && !hword.endsWith("-")){
	                //replace those in lower cases
	                original = original.replaceAll(hword, dhword);
	                //hyphen those phrases that are hyphened once 
	                String dhw = dhword.replaceAll("-", " "); //cup-shaped => cup shaped
	                original = original.replaceAll(dhw, dhword); //cup shaped =>cup-shaped
	                //upper cases
	                hword = hword.toUpperCase().substring(0,1)+hword.substring(1);
	                dhword = dhword.toUpperCase().substring(0,1)+dhword.substring(1);
	                original = original.replaceAll(hword, dhword);
	                dhw = dhword.replaceAll("-", " "); //Cup-shaped => Cup shaped
	                original = original.replaceAll(dhw, dhword); //Cup shaped =>Cup-shaped
	            }
	        }
	        return original;
	    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DeHyphenAFolder dhaf = new DeHyphenAFolder(null, "C:\\RA\\PARSER-DEMO\\Treatise\\target\\", 
		"descriptions", "markedupdatasets", null, "treatise", "treatisehglossaryfixed", null);
		dhaf.dehyphen();

	}

}
