/**
 * 
 */
package fna.parsing;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 *transform NeXML files
 */
public class Type4Transformer4Phenoscape extends Type4Transformer {

	/**
	 * @param listener
	 * @param dataprefix
	 */
	public Type4Transformer4Phenoscape(ProcessListener listener,
			String dataprefix) {
		super(listener, dataprefix);
	}

	/**
	 * 
	 */


	/* do one thing: 
	 * take out description element and save them in a separate folder.
	 * make sure the file names are mapped to numbers
	 * 
	 */
	/**
	 * @param files: NeXML files, one for each PDF source file
	 */
	@Override
	protected void transformXML(File[] files) {
		int number = 0;
		try{
			SAXBuilder builder = new SAXBuilder();
			for(int f = 0; f < files.length; f++) {
				int fn = f+1;
				Document doc = builder.build(files[f]);
				Element root = doc.getRootElement();
				formatDescription(root,"/treatment/description", null, fn, 0);
				root.detach();
				writeTreatment2Transformed(root, fn, 0);
				listener.info((number++)+"", fn+"_0.xml"); // list the file on GUI here
		        getDescriptionFrom(root,fn, 0);						
			}
		}catch(Exception e){
			e.printStackTrace();
			LOGGER.error("Type4Transformer : error.", e);
		}

	}
	@SuppressWarnings("unchecked")
	protected void getDescriptionFrom(Element root, int fn,  int count) {

		try{
			List<Element> descriptions = XPath.selectNodes(root, "/treatment/description");
			Iterator<Element> it = descriptions.iterator();
			int i = 0;
			while(it.hasNext()){
				Element description = it.next();
				writeDescription2Descriptions(description.getTextNormalize(), fn+"_"+count+".txtp"+i); //record the position for each paragraph.
				i++;							
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
