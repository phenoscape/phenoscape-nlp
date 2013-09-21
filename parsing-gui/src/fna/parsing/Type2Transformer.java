/**
 * 
 */
package fna.parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 * For extracted XML formated descriptions from Treatise 
 */
@SuppressWarnings({ "unused" })
public class Type2Transformer extends Thread {

	//private File source =new File(Registry.SourceDirectory); //a folder of text documents to be annotated
	private File source = new File(Registry.SourceDirectory);
	//File target = new File(Registry.TargetDirectory);

	//File target = new File("Z:\\DATA\\Plazi\\2ndFetchFromPlazi\\target-taxonX-ants-trash");
	//private String tableprefix = "plazi_ants";

	//target folder
	File target = new File(Registry.TargetDirectory);
	//private String tableprefix = "plazi_ants";
	

	private XMLOutputter outputter = null;
	// this is the dataprfix from general tab
	private String dataprefix = null;
	private ProcessListener listener;
	protected static final Logger LOGGER = Logger.getLogger(CharacterStatementsTransformer.class);
	/**
	 * @param listener
	 * @param dataprefix
	 */
	public Type2Transformer(ProcessListener listener, String dataprefix) {
		this.listener = listener;
		this.dataprefix = dataprefix;
		File target = new File(Registry.TargetDirectory);
		Utilities.resetFolder(target, "descriptions");
		Utilities.resetFolder(target, "transformed");
		Utilities.resetFolder(target, "descriptions-dehyphened");
		Utilities.resetFolder(target, "markedup");
		Utilities.resetFolder(target, "final");
		Utilities.resetFolder(target, "co-occurrence");
	}

	public void run(){
		listener.setProgressBarVisible(true);
		transform();
		listener.setProgressBarVisible(false);
	}
	/**
	 * just take the content of <description>s out and save them in the target folder 
	 */
	public void transform(){
		try{
			/*Runtime r = Runtime.getRuntime();
			String src = "\""+source.getAbsolutePath()+"\"";
			String tgt = "\""+target.getAbsolutePath()+"\\transformed\"";
			String cmd = "copy "+src+" "+tgt;
			Process p = r.exec(cmd);
			int exitVal = p.waitFor();
            if(exitVal>0){
            	throw new Exception("transformed not created");
            }*/
			File[] files =  source.listFiles();
			SAXBuilder builder = new SAXBuilder();
			listener.progress(1);
			int total = files.length;
			
			for(int i = 0; i<total; i++){
				File f = files[i];		
				String tgt = target.getAbsolutePath()+System.getProperty("file.separator")+"transformed";
				File newFile = new File(tgt+System.getProperty("file.separator")+f.getName());
				if(!newFile.exists())
					newFile.createNewFile();
				FileChannel inputChannel = new FileInputStream(f).getChannel();
				FileChannel outputChannel = new FileOutputStream(newFile).getChannel();
				
				inputChannel.transferTo(0,inputChannel.size(),outputChannel);
				
				Document doc = builder.build(f);
				Element root = doc.getRootElement();
				
				Element descrp = (Element)XPath.selectSingleNode(root, "//treatment/description");
				String text = descrp.getTextNormalize();
				writeDescription2Descriptions(text,f.getName().replaceAll("xml$", "txt") ); //record the position for each paragraph.
				listener.progress((i+1)*100/total);
				listener.info((i)+"", f.getName()); 
			}	
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private void writeDescription2Descriptions(String textNormalize, String fn) {
		try {
			File file = new File(target+System.getProperty("file.separator")+"descriptions", fn);
			
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(textNormalize);
			out.close(); // don't forget to close the output stream!!!
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Failed to output text file in Type2Transformer:outputDescriptionText", e);
			throw new ParsingException("Failed to output text file.", e);
		}		
	}
	/**
	 * 
	 */
	public Type2Transformer() {
		// TODO Auto-generated constructor stub
	}

}
