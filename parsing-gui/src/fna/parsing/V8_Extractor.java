package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class V8_Extractor {
	Element treatment = new Element("treatment");
	//protected XMLOutputter outputter;

	public static void main(String[] args) throws Exception {
		int count = 1, i=0;
		V8_Extractor extractor = new V8_Extractor();
		SAXBuilder builder = new SAXBuilder();
		Document doc = builder.build("C:/Users/Li Chen/Desktop/Library Project/Library Project/document.xml");
		Element root = doc.getRootElement();
		List wpList = XPath.selectNodes(root, "/w:document/w:body/w:p");

		Iterator iter = wpList.iterator();
		while (iter.hasNext()) {
			extractor.processParagraph((Element) iter.next());
			count++;
			if (count > 100) {
				i++;
				count = 1;
				extractor.output(i);
				extractor.createtreatment();
				//break;
			}
		}
		//Document doc1 = new Document(treatment);
		//XMLOutputter outputter = new XMLOutputter();
		//String file = "d:/result.xml";
		//Document doc1 = new Document(extractor.treatment);
		//BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		//outputter.output(doc1, out);
	}

	private void processParagraph(Element wp) throws Exception {
		Element pe = new Element("paragraph");
		Attribute att = (Attribute) XPath.selectSingleNode(wp,"./w:pPr/w:pStyle/@w:val");
		String style = "";
		if (att != null) {
			style = att.getValue();
		}
		if (style != "") {
			Element se = new Element("style");
			se.setText(style);
			pe.addContent(se);
		}
		
		Element rowtag = (Element)XPath.selectSingleNode(wp,"./w:r");
		if (rowtag != null) {
			List wrlist = XPath.selectNodes(wp, "./w:r");
			Iterator riter = wrlist.iterator();
			while (riter.hasNext()) {
				Element r = (Element) riter.next();


				// start to process tab
				Element tabe = (Element) XPath.selectSingleNode(r,"./w:tab");
				if (tabe != null) {
					Element tab = new Element("tab");
					pe.addContent(tab);
				}
				// process tab end

				// start to process rPr
				Element rPre = (Element) XPath.selectSingleNode(r,"./w:rPr");
				if (rPre != null) {
					Element bold = (Element) XPath.selectSingleNode(rPre,"./w:b");
					if (bold != null) {
						bold = new Element("bold");
						pe.addContent(bold);
					}
					Element italy = (Element) XPath.selectSingleNode(rPre,"./w:i");
					if (italy != null) {
						italy = new Element("italy");
						pe.addContent(italy);
					}
				}
				// process rPr end
				
				// start to process text
				Element te = (Element)XPath.selectSingleNode(r,"./w:t");
				if (te != null) {
					Element text = new Element("text");
					text.setText(te.getText());
					pe.addContent(text);
				}
				// process text end
				
				//if(re.getContentSize()!=0){
				//	pe.addContent(re);
				//}
			}
		}
		treatment.addContent(pe);
		//System.out.println(treatment.getContentSize());
	}

	private void output(int i) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		String file = "C:/Users/Li Chen/Desktop/Library Project/Library Project/Extracted/" + i + ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
}

