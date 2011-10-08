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
public class CleanTime extends DataCleaner{

	/**
	 * 
	 */
	public CleanTime(String sourcedir, ArrayList<String> sourceElements, String outputElement, String outputdir) {
		super(sourcedir, sourceElements, outputElement, outputdir);
	}

	/*
	 * **************************************************************************************
	 * replace the content of each source element with its legal value
	 * replace the source element name with output element name
	 * 
	 * for flowering time, it is simple replacement
	 */
	protected Element clean(Element root){
		try{
			Iterator<String> it = this.sourceelements.iterator();
			while(it.hasNext()){
				String ename = it.next();
				List<Element> elements = XPath.selectNodes(root, "//"+ename);
				Iterator<Element> eit = elements.iterator();
				while(eit.hasNext()){
					Element e = eit.next();
					ArrayList<String> values = cleanText(e.getText());
					Element p = e.getParentElement();
					p.removeContent(e);
					Iterator<String> vit = values.iterator();
					while(vit.hasNext()){//if values is empty, no replacement is done, but the original element is removed
						Element ce = new Element(this.outputelement);
						String text = vit.next();
						ce.setText(text);
						p.addContent(ce);
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return root;
	}



	/**
	 * ***************************************************************************************
	 */
	protected void collectLegalValues(){
		this.legalvalues="jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|spring|summer|fall|winter|year round";
	} 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcedir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta-clean-0";
		ArrayList<String> sourceElements = new ArrayList<String>();
		sourceElements.add("ecological_info/flowering_time");
		String outputElement = "flowering_time";
		String outputdir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta-clean-1";
		CleanTime ct = new CleanTime(sourcedir, sourceElements, outputElement, outputdir);
		ct.collectSourceContent();
		ct.collectLegalValues();
		ct.cleanFiles();
	}

}
