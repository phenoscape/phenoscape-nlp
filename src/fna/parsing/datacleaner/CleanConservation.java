/**
 * 
 */
package fna.parsing.datacleaner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author hongcui
 *
 */
@SuppressWarnings({ "unchecked" })
public class CleanConservation extends DataCleaner {

	/**
	 * @param sourcedir
	 * @param sourceElements
	 * @param outputElement
	 * @param outputdir
	 */
	public CleanConservation(String sourcedir,
			ArrayList<String> sourceElements, String outputElement,
			String outputdir) {
		super(sourcedir, sourceElements, outputElement, outputdir);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see fna.parsing.datacleaner.DataCleaner#clean(org.jdom.Element)
	 */
	@Override
	protected Element clean(Element root) {
		try{
			Element conservation = new Element("conservation");
			root.addContent(conservation);
			Iterator<String> it = this.sourceelements.iterator();
			while(it.hasNext()){
				String ename = it.next();
				List<Element> elements = XPath.selectNodes(root, "//"+ename);
				if(elements.size()==0){
					conservation.detach();
				}
				Iterator<Element> eit = elements.iterator();
				while(eit.hasNext()){
					Element e = eit.next();
					Element p = e.getParentElement();					
					p.removeContent(e);
					if(p.getChildren().size()==0){
						p.detach();
					}
					conservation.setText("true");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return root;
	}

	/* (non-Javadoc)
	 * @see fna.parsing.datacleaner.DataCleaner#collectLegalValues()
	 */
	@Override
	protected void collectLegalValues() { //no need for this
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcedir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta-clean-1";
		ArrayList<String> sourceElements = new ArrayList<String>();
		sourceElements.add("ecological_info/conservation");
		String outputElement = null;
		String outputdir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta-clean-2";
		CleanConservation ct = new CleanConservation(sourcedir, sourceElements, outputElement, outputdir);
		ct.collectSourceContent();
		ct.collectLegalValues();
		ct.cleanFiles();
	}

}
