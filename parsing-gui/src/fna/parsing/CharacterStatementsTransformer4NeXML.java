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
public class CharacterStatementsTransformer4NeXML extends
		CharacterStatementsTransformer {
	protected XPath xpathstates;
	protected XPath xpathstate;
	protected XPath xpathchar;

	/**
	 * @param listener
	 * @param display
	 * @param perllog
	 * @param seeds
	 */
	public CharacterStatementsTransformer4NeXML(ProcessListener listener,
			Display display, Text perllog, ArrayList seeds) {
		super(listener, display, perllog, seeds);
		// TODO Auto-generated constructor stub
	}
	
	protected void setXPaths(){
		try{
			xpathstates = XPath.newInstance("//x:format/x:states");
			xpathstate = XPath.newInstance(".//x:state");
			xpathchar = XPath.newInstance("//x:format/x:char");
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
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
