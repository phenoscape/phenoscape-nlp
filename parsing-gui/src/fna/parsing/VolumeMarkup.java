/**
 * $Id: VolumeMarkup.java 909 2011-08-12 22:08:51Z hong1.cui $
 */
package fna.parsing;

import java.io.BufferedReader;
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
