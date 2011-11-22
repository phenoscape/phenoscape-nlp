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

	protected static final Logger LOGGER = Logger.getLogger(CharacterStatementsTransformer.class);
	private String seedfilename = "seeds";
	private ProcessListener listener;
	private Text perlLog;
	private XMLOutputter outputter;
	private XPath xpathstates;
	private XPath xpathstate;
	private XPath xpathchar;
	
	CharacterStatementsTransformer(ProcessListener listener, Display display, 
			Text perllog, ArrayList seeds){
		//super(listener, display, perllog, dataprefix);
		try{
			xpathstates = XPath.newInstance("//x:format/x:states");
			xpathstate = XPath.newInstance(".//x:state");
			xpathchar = XPath.newInstance("//x:format/x:char");
		}catch (Exception e){
			e.printStackTrace();
		}
		this.seeds = seeds;
		this.listener = listener;
		this.perlLog = perllog;
		//this.markupMode = "plain";
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
	 * descriptions for state label atttributes
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
			//get <state> to put in desfolder
		    xpathstates.addNamespace("x", root.getNamespaceURI()); //this is how to handle default namespace
			List<Element> allstates = xpathstates.selectNodes(doc);
			Iterator<Element> its = allstates.iterator();
			while(its.hasNext()){
				Element states = its.next();
				String statesid = states.getAttributeValue("id");
				xpathstate.addNamespace("x", root.getNamespaceURI());
				List<Element> stateels = xpathstate.selectNodes(states);
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
			xpathchar.addNamespace("x", root.getNamespaceURI());
			List<Element> chars = xpathchar.selectNodes(root);
			Iterator<Element> it = chars.iterator();
			while(it.hasNext()){
				Element cha = it.next();
				String statesid = cha.getAttributeValue("states");
				String content = cha.getAttributeValue("label").trim();
				content = content.replaceFirst("[,;\\.]+$", ";");
				write2file(chafolder, fname+"_"+statesid+".txt", content);
			}
			
			root.detach();
			//copy to trafolder
			outputter.output(new Document(root), new BufferedOutputStream(new FileOutputStream(new File(trafolder, file.getName()))));

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
		CharacterStatementsTransformer tpm = new CharacterStatementsTransformer(null, null, null, null);
		tpm.output2Target();
	}
	
	public void run () {
		listener.setProgressBarVisible(true);
		System.out.println("compiled");
		output2Target();
		listener.setProgressBarVisible(false);
	}

}
