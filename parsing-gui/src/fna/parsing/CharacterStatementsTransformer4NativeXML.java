/**
 * 
 */
package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 *
 */
public class CharacterStatementsTransformer4NativeXML extends
		CharacterStatementsTransformer {
	protected XPath xpathstate;
	protected XPath xpathchar;

	/**
	 * @param listener
	 * @param display
	 * @param perllog
	 * @param seeds
	 */
	public CharacterStatementsTransformer4NativeXML(ProcessListener listener,
			Display display, Text perllog, ArrayList seeds) {
		super(listener, display, perllog, seeds);
		// TODO Auto-generated constructor stub
	}
	
	protected void setXPaths(){
		try{
			xpathstate = XPath.newInstance("description");
			xpathchar = XPath.newInstance("character");
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * Copy file to trafolder
	 * take <description> elements from file and output them to desfolder. 
	 * take <character> elements from file and output them to charfolder. 
	 * @param folder
	 * @param fname
	 */
	protected void outputTo(File desfolder, File chafolder, File trafolder, File file) {
		try{
			String fname = file.getName();
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(file);
			Element root = doc.getRootElement();
			//get <state> to put in desfolder
			List<Element> allstates = xpathstate.selectNodes(root);
			Iterator<Element> its = allstates.iterator();
			while(its.hasNext()){
				Element state = its.next();
				String stateid = state.getAttributeValue("pid");
				String content = state.getTextNormalize().trim();
				content = content.replaceFirst("[,;\\.]+$", ";");
				write2file(desfolder, stateid+".txt", content);				
			}
			//get <character> to put in chafolder
			List<Element> chars = xpathchar.selectNodes(root);
			Iterator<Element> it = chars.iterator();
			while(it.hasNext()){
				Element cha = it.next();
				String chaid = cha.getAttributeValue("pid");
				String content = cha.getTextNormalize().trim();
				content = content.replaceFirst("[,;\\.]+$", ";");
				write2file(chafolder, chaid+".txt", content);
			}
			
			root.detach();
			//copy to trafolder
			outputter.output(new Document(root), new BufferedOutputStream(new FileOutputStream(new File(trafolder, file.getName()))));

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
