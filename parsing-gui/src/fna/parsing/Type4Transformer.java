/**
 * 
 */
package fna.parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.jdom.Content;
import org.jdom.Text;

import fna.db.*;

/**
 * @author hongcui
 * split taxonX/other XML documents to smaller units, each resulting xml document contains 1 treatment.
 * 
 */
@SuppressWarnings({ "unchecked", "unused" })
public abstract class Type4Transformer extends Thread {
	private File source =new File(Registry.SourceDirectory); //a folder of xml documents to be annotated
	File target = new File(Registry.TargetDirectory);

	//File target = new File("Z:\\DATA\\Plazi\\2ndFetchFromPlazi\\target-taxonX-ants-trash");
	//private String tableprefix = "plazi_ants";

	private XMLOutputter outputter = null;
	// this is the dataprfix from general tab
	private String dataprefix = null;
	protected ProcessListener listener;
	protected static final Logger LOGGER = Logger.getLogger(Type3Transformer.class);
	/**
	 * 
	 */
	
	public Type4Transformer(ProcessListener listener, String dataprefix) {
		this.listener = listener;
		this.dataprefix = dataprefix;
		/* Remove this hardcoding later*/
		//dataprefix = "plazi_ants";
		if(!target.exists()){
			target.mkdir();
		}
		
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
	
	public void transform(){
		File[] files =  source.listFiles();
		//create renaming mapping table
		Hashtable<String, String> filemapping = new Hashtable<String, String>();

		listener.progress(1);
		for(int f = 0; f < files.length; f++) {
			listener.progress((100*(f+1))/files.length);
			int fn = f+1;
			System.out.println (files[f].getName()+" to "+ (f+1)+".xml");
			filemapping.put(files[f].getName(), (f+1)+".xml");
		}
		Type4TransformerDbAccessor t4tdb = new Type4TransformerDbAccessor("filenamemapping", dataprefix);
		t4tdb.addRecords(filemapping);
		
		//transform XML
		transformXML(files);

	}

	protected abstract void transformXML(File[] files);
	
	protected Element formatDescription(Element treatment, String descriptionXPath, String paraXPath, int fn, int count) {
		try{
			Element description = (Element)XPath.selectSingleNode(treatment, descriptionXPath);
			if(description==null){
				return treatment;
			}else{
				if(paraXPath != null){
					List<Element> ps = XPath.selectNodes(description, paraXPath);
					Iterator<Element> it = ps.iterator();
					int i = 0;
					while(it.hasNext()){
						Element p = it.next();
						p.setName("description");
						p.setAttribute("pid", fn+"_"+count+".txtp"+i);
						p.setNamespace(null);
						i++;
					}
				}else{ //no paraXPath is given, make the description element the only one 
					description.setName("description");
					description.setAttribute("pid", fn+"_"+count+".txtp0");
					description.setNamespace(null);
				}
				return treatment;
			}
		}catch(Exception e){
			e.printStackTrace();
			LOGGER.error("Type4Transformer : error.", e);
		}
		return null;
	}

	protected void getDescriptionFrom(Element root, int fn,  int count) {

		try{
		List<Element> divs = XPath.selectNodes(root, "/tax:taxonx/tax:taxonxBody/tax:treatment/tax:div");
		Iterator<Element> it = divs.iterator();
		int i = 0;
		while(it.hasNext()){
			Element div = it.next();
			if(div.getAttributeValue("type").compareToIgnoreCase("description")==0){
				//List<Element> ps = div.getChildren("p", div.getNamespace());
				List<Element> ps = div.getChildren("description");
				Iterator<Element> t = ps.iterator();
				while(t.hasNext()){
					Element p = t.next();
					int size = p.getContentSize();
					StringBuffer sb = new StringBuffer();
					for(int c = 0; c < size; c++){
						Content cont = p.getContent(c);
						if(cont instanceof Element){
							sb.append(((Element)cont).getTextNormalize()+" ");
						}else if(cont instanceof Text){
							sb.append(((Text)cont).getTextNormalize()+" ");
						}
					}
					
					//writeDescription2Descriptions(sb.toString(), fn+"_"+count+"_"+i); //record the position for each paragraph.
					writeDescription2Descriptions(sb.toString(), fn+"_"+count+".txtp"+i); //record the position for each paragraph.
					i++;
				}
			}
		}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	protected void writeDescription2Descriptions(String textNormalize, String fn) {
		try {
			File file = new File(target+"/descriptions", fn+ ".txt");
			
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(textNormalize);
			out.close(); // don't forget to close the output stream!!!
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Failed to output text file in Type4Transformer:outputDescriptionText", e);
			throw new ParsingException("Failed to output text file.", e);
		}
		
	}

	protected void writeTreatment2Transformed(Element root, int fn, int count) {
		// TODO Auto-generated method stub
		ParsingUtil.outputXML(root, new File(target+"/transformed", fn+"_"+count+".xml"), null);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//Type4Transformer t4t = new Type4Transformer();
		//t4t.transform();
	}

}
