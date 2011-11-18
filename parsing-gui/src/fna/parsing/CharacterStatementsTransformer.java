/**
 * 
 */
package fna.parsing;
import java.io.BufferedWriter;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

import fna.db.*;

/**
 * @author hongcui
 * 	
 * transform NeXML chars/states to a suitable format for CharaParser
 * expect 1 NeXML file per PDF paper (original pub) in source folder 
 */
@SuppressWarnings("unchecked")
public class CharacterStatementsTransformer extends Thread {
	private ArrayList<String> seeds = new ArrayList<String>();
	//private File source =new File(Registry.SourceDirectory); //a folder of text documents to be annotated
	//private File source = new File("Z:\\WorkFeb2008\\WordNov2009\\Description_Extraction\\extractionSource\\Plain_text");
	private File source = new File(Registry.SourceDirectory);
	//File target = new File(Registry.TargetDirectory);
	//File target = new File("Z:\\WorkFeb2008\\WordNov2009\\Description_Extraction\\extractionSource\\Plain_text_transformed");
	private File target = new File(Registry.TargetDirectory);
	private XMLOutputter outputter = null;
	private String markupMode = "plain";
	private String dataprefix = null;
	protected static final Logger LOGGER = Logger.getLogger(CharacterStatementsTransformer.class);
	private String seedfilename = "seeds";
	private ProcessListener listener;
	private Text perlLog;
	private String glossarytable;
	
	CharacterStatementsTransformer(ProcessListener listener, Display display, 
			Text perllog, String dataprefix,String glossarytable, ArrayList seeds){
		//super(listener, display, perllog, dataprefix);
		this.seeds = seeds;
		this.listener = listener;
		this.perlLog = perllog;
		//this.markupMode = "plain";
		this.dataprefix = dataprefix;
		this.glossarytable=glossarytable;
		outputter = new XMLOutputter(Format.getPrettyFormat());
		File target = new File(Registry.TargetDirectory);
		Utilities.resetFolder(target, "descriptions");
		Utilities.resetFolder(target, "transformed");
		Utilities.resetFolder(target, "descriptions-dehyphened");
		Utilities.resetFolder(target, "markedup");
		Utilities.resetFolder(target, "final");
		Utilities.resetFolder(target, "co-occurrence");
	}	
	
	/**
	 * create folders:
	 * descriptions for to-be-annotated morph description segments
	 * transformed for entire documents (tags: <document><nonMorph></nonMorph><treatment></treatment></document>)
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
		//dehypen descriptions folder: may be a redundant step
		//DeHyphenAFolder dhf = new DeHyphenAFolder(listener,target.getAbsolutePath(),"descriptions", 
		//		ApplicationUtilities.getProperty("database.name"), perlLog,  dataprefix,this.glossarytable, null);
		//dhf.dehyphen();

	}
	/**
	 * Copy file to trafolder
	 * take <description> elements from file and output them to desfolder. 
	 * take <character> elements from file and output them to charfolder. 
	 * @param folder
	 * @param fname
	 */
	private void outputTo(File desfolder, File chafolder, File trafolder, File file) {
		try{
			String fname = file.getName();
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(file);
			Element root = doc.getRootElement();
			root.detach();
			//copy to trafolder
			XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
			out.output(new Document(root), new BufferedOutputStream(new FileOutputStream(new File(trafolder, file.getName()))));
			//get <state> to put in desfolder
			List<Element> allstates = XPath.selectNodes(root, "//format/states");
			Iterator<Element> its = allstates.iterator();
			while(its.hasNext()){
				Element states = its.next();
				String statesid = states.getAttributeValue("id");
				List<Element> stateels = XPath.selectNodes(states, ".//state");
				Iterator<Element> it = stateels.iterator();
				while(it.hasNext()){
					Element state = it.next();
					String stateid = state.getAttributeValue("id");
					String content = state.getAttributeValue("label").trim();
					content = content.replaceFirst("[,;\\.]+$", ";");
					write2file(desfolder, fname+"_"+statesid+"_"+stateid+".txt", content);
				}
			}
			//get <character> to put in chafolder
			List<Element> chars = XPath.selectNodes(root, "//format/char");
			Iterator<Element> it = chars.iterator();
			while(it.hasNext()){
				Element cha = it.next();
				String statesid = cha.getAttributeValue("states");
				String content = cha.getAttributeValue("label").trim();
				content = content.replaceFirst("[,;\\.]+$", ";");
				write2file(chafolder, fname+"_"+statesid+".txt", content);
			}
		}catch(Exception e){
			e.printStackTrace();
		}

	}

	private void write2file(File desfolder, String fname, String text) {
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
			nfile.renameTo(new File(nfile.getName()+""+System.currentTimeMillis()));
			if(nfile.mkdir()){
				return nfile;
			}
		}
		return nfile;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//save to-be-annotated files to source folder 
		CharacterStatementsTransformer tpm = new CharacterStatementsTransformer(null, null, null, "bhl_2vs","", null);
		tpm.output2Target();
	}
	
	public void run () {
		listener.setProgressBarVisible(true);
		System.out.println("compiled");
		output2Target();
		listener.setProgressBarVisible(false);
	}

}
