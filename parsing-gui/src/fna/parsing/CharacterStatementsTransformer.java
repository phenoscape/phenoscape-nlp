/**
 * 
 */
package fna.parsing;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;



/**
 * @author hongcui
 * 	
 * transform NeXML chars/states to a suitable format for CharaParser
 * expect 1 NeXML file per PDF paper (original pub) in source folder 
 */

public abstract class CharacterStatementsTransformer extends Thread {
	protected ArrayList<String> seeds = new ArrayList<String>();
	//private File source =new File(Registry.SourceDirectory); //a folder of text documents to be annotated
	//private File source = new File("Z:\\WorkFeb2008\\WordNov2009\\Description_Extraction\\extractionSource\\Plain_text");
	protected File source = new File(Registry.SourceDirectory);
	//File target = new File(Registry.TargetDirectory);
	//File target = new File("Z:\\WorkFeb2008\\WordNov2009\\Description_Extraction\\extractionSource\\Plain_text_transformed");
	protected File target = new File(Registry.TargetDirectory);

	protected static final Logger LOGGER = Logger.getLogger(CharacterStatementsTransformer.class);
	protected String seedfilename = "seeds";
	protected ProcessListener listener;
	protected Text perlLog;
	protected XMLOutputter outputter;
	//protected String prefix;
	//protected String glossarytable;

	
	CharacterStatementsTransformer(ProcessListener listener, Display display, 
			Text perllog, ArrayList<String> seeds/*, String prefix, String glossarytable*/){
		this.seeds = seeds;
		this.listener = listener;
		this.perlLog = perllog;
		//this.prefix = prefix;
		//this.glossarytable = glossarytable;
		this.outputter = new XMLOutputter(Format.getPrettyFormat());
		setXPaths();
		File target = new File(Registry.TargetDirectory);
		Utilities.resetFolder(target, "descriptions");
		Utilities.resetFolder(target, "transformed");
		//TermOutputerUtilities.resetFolder(target, "descriptions-dehyphened");
		Utilities.resetFolder(target, "markedup");
		Utilities.resetFolder(target, "final");
		Utilities.resetFolder(target, "co-occurrence");
	}	
	
	protected abstract void setXPaths();
	/**
	 * create folders:
	 * descriptions for state label attributes
	 * characters for char elements
	 * transformed for entire NeXML documents 
	 */
	private void output2Target() {
		File des = createFolderIn(target, "descriptions");
		File tra = createFolderIn(target, "transformed");
		File cha = createFolderIn(target, "characters");
		File[] files = this.source.listFiles();
		listener.progress(30);
		for(int i = 0; i<files.length; i++){
			String fname = files[i].getName();
			outputTo(des,cha, tra,files[i]);
			/* Show on the table - show from transformed folder --
			 * put a listener progress here
			 * .*/
			listener.info((i+1) + "", fname.replaceAll("\\..*$", "")+".xml");
			listener.progress((90* i)/files.length);
		}
		listener.progress(60);
	}

	protected abstract void outputTo(File desfolder, File chafolder, File trafolder, File file); 

	protected void write2file(File desfolder, String fname, String text) {
		try{
			BufferedWriter out = new BufferedWriter(
					new FileWriter(new File(desfolder, fname)));
			out.write(text);
			out.flush();
			out.close();
		}catch(IOException e){
			LOGGER.error("Exception in Type3PreMarkup.write2file", e);
		}
	}

	private File createFolderIn(File target, String foldername) {
		File nfile = new File(target, foldername);
		if(nfile.mkdir()){
			return nfile;
		}else{
			nfile.renameTo(new File(target, nfile.getName()+""+System.currentTimeMillis()));
			if(nfile.mkdir()){
				return nfile;
			}
		}
		return nfile;
	}

	

	public void run () {
		listener.setProgressBarVisible(true);
		System.out.println("compiled");
		output2Target();
		listener.setProgressBarVisible(false);
	}

}
