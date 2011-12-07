package fna.parsing;


import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import fna.db.VolumeMarkupDbAccessor;
import fna.parsing.character.Glossary;

//import fna.parsing.finalizer.Output;


/**
 * normalize hyphens in the document folder. may be plain text or html/xml docs. for the latter, tags are ignored in dehyphenization process.
 * run this before VolumeMarkup.
 * @author hongcui
 *
 */
@SuppressWarnings({ "unused","static-access" })
public class VolumeDehyphenizer extends Thread {
    //protected File folder = null;
    //protected File outfolder = null;
    //protected Connection conn = null;
    //protected final String username = ApplicationUtilities.getProperty("database.username");
    //protected final String password = ApplicationUtilities.getProperty("database.password");
    //protected String tablename = null;
    //private final String tablename1= "numtextmix";
    //private final int mixlength = 30;
    //static public String num = "\\d[^a-z]+";
    protected String database = "";
    //protected Hashtable<String,String> mapping = new Hashtable<String, String>();
    protected ProcessListener listener;
    private static final Logger LOGGER = Logger.getLogger(VolumeDehyphenizer.class);
    private Glossary glossary = null;  // TODO
    private Display display;
    private Text perlLog;
    private String dataPrefix;
    private DeHyphenAFolder dhf;
    private Table descriptorTable;
    private MainForm mainForm;
    private VolumeMarkupDbAccessor vmdb;
    private String glossaryTableName;
    
    public VolumeDehyphenizer(ProcessListener listener, String workdir, 
    		String todofoldername, String database, 
    		Display display, Text perlLog, String dataPrefix,  MainForm mainForm) {
        this.listener = listener;
        /** Synchronizing UI and background process **/
        this.display = display;
        this.perlLog = perlLog;
        this.dataPrefix = dataPrefix;
        this.mainForm = mainForm;
        this.glossaryTableName = mainForm.glossaryPrefixCombo.getText();
        this.vmdb = new VolumeMarkupDbAccessor(dataPrefix, this.glossaryTableName);
        
        this.glossary = new Glossary(this.glossaryTableName);
        //dehypen step is not needed for NeXML files
        //this.dhf = new DeHyphenAFolder(listener,workdir,todofoldername, database, this,  dataPrefix, this.glossaryTableName, glossary);
    }

    public void run () {
    	listener.setProgressBarVisible(true);
       	//boolean done = dhf.dehyphen();//dhf waits for all unmatched brackets are fixed.
    	//if(done){
    		VolumeMarkup vm = new VolumeMarkup(listener, display, perlLog, dataPrefix, this.glossaryTableName);
    		resetPerlMessage(); //clean up perlLog box
    		vm.markup();
    		listener.setProgressBarVisible(false);
    	//}

    }
    
    /*private void loadOthersTab() {
    	display.syncExec(new Runnable() {
    		public void run() {
    			mainForm.showOtherTerms();
    		}
    	});
    }*/
    /*
	private void loadDescriptorTab() {
		
		display.syncExec(new Runnable() {
			public void run() {
				ArrayList <String> words = null;
				try {
					words = vmdb.descriptorTerms4Curation();
				} catch (Exception exe){
					LOGGER.error("unable to load descriptor tab in Markup : MainForm", exe);
					exe.printStackTrace();
				}
				int count = 1;
				descriptorTable.removeAll(); //clean up before a load
				if (words != null) {
					for (String word : words){
						TableItem item = new TableItem(descriptorTable, SWT.NONE);
						item.setText(new String [] {count+"", word});
						count++;
					}
				}
			}});
	}*/
	
    public void resetPerlMessage() {
		display.syncExec(new Runnable() {
			public void run() {
				perlLog.setText("");
			}
		});
	}
    
	public void showPerlMessage(final String message) {
		display.syncExec(new Runnable() {
			public void run() {
				perlLog.append(message);
			}
		});
	}
	//moved the following to DeHyphenAFolder.java
	/*public void incrementProgressBar(int progress) {
		listener.progress(progress);
	}*/
	/*
    public void dehyphen(){
    	System.out.println("Preparing files...");
    	showPerlMessage("Preparing files...");
        incrementProgressBar(1);
        fillInWords();
        incrementProgressBar(50);

        DeHyphenizer dh = new DeHyphenizerCorrected(this.database, this.tablename, "word", "count", "-", dataPrefix);

        try{
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select word from "+tablename+" where word like '%-%'");
            while(rs.next()){
                String word = rs.getString("word");
                String dhword = dh.normalFormat(word);
                //System.out.println(word+"===>"+dhword);
                //MainForm.markUpPerlLog.append(word+"===>"+dhword+"\n");
                mapping.put(word, dhword);
            }
        }catch(Exception e){
        	LOGGER.error("Problem in VolumeDehyphenizer:dehyphen", e);
            e.printStackTrace();
        }
        normalizeDocument();
        if(listener!= null) incrementProgressBar(100);
    }
    
    private void createWordTable(){
        try{
            Statement stmt = conn.createStatement();
            String query = "create table if not exists "+tablename+" (word varchar(50) unique not null primary key, count int)";
            stmt.execute(query);
            stmt.execute("delete from "+tablename);
        }catch(Exception e){
        	LOGGER.error("Problem in VolumeDehyphenizer:createWordTable", e);
            e.printStackTrace();
        }
    }
    
    private void fillInWords(){
        try {
            Statement stmt = conn.createStatement();
            File[] flist = folder.listFiles();
            for(int i= 0; i < flist.length; i++){
                //System.out.println("read "+flist[i].getName());
                //MainForm.markUpPerlLog.append("read "+flist[i].getName()+"\n");
                BufferedReader reader = new BufferedReader(new FileReader(flist[i]));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    line = line.toLowerCase();
                    String linec = line;
                    //if(line.matches(".*?\\d+-(?=[a-z]).*")){
                    //   line = fixNumTextMix(line, flist[i]);
                    //}
                    line = line.replaceAll("<[^<]+?>", " "); //for xml or html docs
                    line = line.replaceAll(num, " ");
                    line = line.replaceAll("[^-a-z]", " ");

                    line = normalize(line);
                    
                    //System.err.println("line has changed from \n"+linec+" to \n"+line);
                
                    String[] words = line.split("\\s+");
                    for(int j = 0; j < words.length; j++){
                        String w = words[j].trim();
                        if(w.matches(".*?\\w.*")){
                            int count = 1;
                            ResultSet rs = stmt.executeQuery("select word, count from "+tablename+"  where word='"+w+"'");
                            if(rs.next()){
                                count = rs.getInt("count")+1;
                            }
                            stmt.execute("delete from "+tablename+" where word ='"+w+"'");
                            stmt.execute("insert into "+tablename+" values('"+w+"', "+count+")");
                        }
                    }
                }
                reader.close();
            }
        } catch (Exception e) {
        	LOGGER.error("Problem in VolumeDehyphenizer:fillInWords", e);
            e.printStackTrace();
        }
    }
    
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
                text = normalize(text);

                text = performMapping(text);
                //write back
                //System.out.println(text);
                File outf = new File(outfolder, flist[i].getName());
                //BufferedWriter out = new BufferedWriter(new FileWriter(flist[i]));
                BufferedWriter out = new BufferedWriter(new FileWriter(outf));
                out.write(text);
                out.close();
                //System.out.println(flist[i].getName()+" dehyphenized");
                //MainForm.markUpPerlLog.append(flist[i].getName()+" dehyphenized\n");
            }
        } catch (Exception e) {
        	LOGGER.error("Problem in VolumeDehyphenizer:normalizeDocument", e);
            e.printStackTrace();
        }
    }
    
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
        //HOng, 08/04/09 for FoC doc. "-" added in place of <dox-tags>.
        //if(line.matches(".*?[a-z]- .*")){//cup-  disc-  or dish-shaped
        //    line = fixBrokenHyphens(line); //Too loose. 
        //}
        //if(text.matches(".*?[a-z]-[^a-z0-9].*")){//cup-  disc-  or dish-shaped
        //text = fixBrokenHyphens(text);
        //}
        return text;
    }
    
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
    */
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        String workdir = "C:/FOC-v11/target";

        String todofoldername = "descriptions";

        //this.database = "focv11_corpus";

        //VolumeDehyphenizer vd = new VolumeDehyphenizer(null, workdir, todofoldername, database);
       // vd.dehyphen();
    }//

}

