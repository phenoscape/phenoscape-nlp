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
public class Remove extends DataCleaner {

	/**
	 * @param sourcedir
	 * @param sourceElements
	 * @param outputElement
	 * @param outputdir
	 */
	public Remove(String sourcedir, ArrayList<String> sourceElements,
			String outputElement, String outputdir) {
		super(sourcedir, sourceElements, outputElement, outputdir);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see fna.parsing.datacleaner.DataCleaner#clean(org.jdom.Element)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected Element clean(Element root) {
		try{
			Iterator<String> it = this.sourceelements.iterator();
			while(it.hasNext()){
				String ename = it.next();
				List<Element> elements = XPath.selectNodes(root, "//"+ename);
				Iterator<Element> eit = elements.iterator();
				while(eit.hasNext()){
					Element e = eit.next();
					Element p = e.getParentElement();
					p.removeContent(e);
					if(p.getChildren().size()==0){
						p.detach();
					}
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
	protected void collectLegalValues() {//no need for this
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcedir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta";
		ArrayList<String> sourceElements = new ArrayList<String>();
		sourceElements.add("ecological_info/couplet");
		sourceElements.add("ecological_info/number_of_infrataxa");
		String outputElement = null;
		String outputdir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta-clean-0";
		Remove rm = new Remove(sourcedir, sourceElements, outputElement, outputdir);
		rm.collectSourceContent();
		rm.collectLegalValues();
		rm.cleanFiles();

	}

}
