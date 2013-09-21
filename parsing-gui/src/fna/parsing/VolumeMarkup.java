/**
 * $Id: VolumeMarkup.java 909 2011-08-12 22:08:51Z hong1.cui $
 */
package fna.parsing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import fna.db.VolumeMarkupDbAccessor;

/**
 * To run unsupervised.pl
 * 
 * @author chunshui
 */
@SuppressWarnings({  "unused" })
public class VolumeMarkup {
	
	protected ProcessListener listener;
	
	protected Display display = null;
	protected Text perlLog = null;
	protected String dataPrefix = null;
	
	protected String markupMode = "plain"; //TODO: make this configurable

	private String glossarytable;
	protected static final Logger LOGGER = Logger.getLogger(VolumeMarkup.class);
	
	public VolumeMarkup(ProcessListener listener, Display display, Text perlLog, String dataPrefix, String glossarytable) {
		this.listener = listener;
        this.display = display;
        this.perlLog = perlLog;
        this.dataPrefix = dataPrefix;
        this.glossarytable = glossarytable;
	}
	
	public void showPerlMessage(final String message) {
		display.syncExec(new Runnable() {
			public void run() {
				perlLog.append(message);
			}
		});
	}
	
	public void incrementProgressBar(int progress) {
		listener.progress(progress);
	}
	/**
	 * call perl 
	 * @throws ParsingException
	 */
	public void markup() throws ParsingException {
		String workdir = Registry.TargetDirectory;
		//String todofoldername = ApplicationUtilities.getProperty("DEHYPHENED");
		String todofoldername = ApplicationUtilities.getProperty("DESCRIPTIONS");
		String savefoldername = ApplicationUtilities.getProperty("MARKEDUP");
		String databasenameprefix = ApplicationUtilities.getProperty("database.name");
		String com = "perl " + ApplicationUtilities.getProperty("UNSUPERVISED") +"\""+workdir
		+ System.getProperty("file.separator")+todofoldername + "\" "+ databasenameprefix+" "+this.markupMode +" "+dataPrefix.trim();
		
		//this command will not output marked-up descriptions to the file system. it only holds the results in mySQL database
		
		//hasproblem checks the input descriptions and if any issues opens a new window and request the user to fix this
		if(hasProblems(workdir+ System.getProperty("file.separator")+todofoldername)==false)
		{
			System.out.println("Run command: " + com);
			showPerlMessage("Run command: " + com + "\n");
			try {
			 runCommand(com);
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeMarkup : markup Failed to run the unsupervised.pl", e);
			showPerlMessage("VolumeMarkup : markup Failed to run the unsupervised.pl" + e.getMessage() + "\n");
			throw new ParsingException("Failed to run the unsupervised.pl.", e);
		}
		}
		else
		{
			showPerlMessage("\n\nFix the files and rerun Step2"+"\n");
		}
	}

    private boolean hasProblems(String Descriptions) {
    	boolean has = false;
    	int problemcount = 0;
    	File folder = new File(Descriptions);
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
                	showPerlMessage((++problemcount)+": "+flist[i].getAbsolutePath()+" contains unmatched brackets in \""+text+"\"\n");
                	java.awt.Desktop.getDesktop().edit(new File(flist[i].getAbsolutePath()));

                }
                //check for missing spaces between text and numbers: 
               /* if(text.matches(".*[a-zA-Z]\\d.*") || text.matches(".*\\d[a-zA-Z].*")){
                	//has =true; //ant descriptions contain "Mf4"
                 	//vd.showPerlMessage((++problemcount)+": "+flist[i].getAbsolutePath()+" misses a space between a word and a number in \""+text+"\"\n");      	       
                }
                //check for (?)
                if(text.matches(".*?\\(\\s*\\?\\s*\\).*")){
                	has =true;
                 	vd.showPerlMessage((++problemcount)+": "+flist[i].getAbsolutePath()+" contains expression (?) in \""+text+"\"\n");  
                 	vd.showPerlMessage("Change (?) to an text expression such as (not certain)");
                }*/
            }

        }catch(Exception e){
            	LOGGER.error("Problem in VolumeDehyphenizer:check4UnmatchedBrackets", e);
	            e.printStackTrace();
        }
        return has;
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
  
	//Perl would hang on any MySQL warnings or errors
	protected void runCommand(String com) throws IOException,
			InterruptedException {
		long time = System.currentTimeMillis();

		Process p = Runtime.getRuntime().exec(com);
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p
				.getInputStream()));
		
		BufferedReader errInput = new BufferedReader(new InputStreamReader(p
				.getErrorStream()));
		
		// read the output from the command
		String s = "";
		int i = 0;
		while ((s = stdInput.readLine()) != null) {
			// listener.info(String.valueOf(i), s);
			incrementProgressBar(i++ % 100);
			System.out.println(s + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
			showPerlMessage(s + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds\n");
		}
		
		// read the errors from the command
		String e = "";
		while ((e = errInput.readLine()) != null) {
			// listener.info(String.valueOf(i), s);
			incrementProgressBar(i++ % 100);
			System.out.println(e + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
			showPerlMessage(e + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds\n");
		}
	}
}
