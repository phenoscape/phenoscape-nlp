package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

/**
 * Hong 08/04/09 revised for FoC volumes a) add start, names,
 * tribegenusnamestyle private properties. b) if(treatment.indexOf(new
 * Element("text"))>=0){ =>added this condition to filter out empty files. Hong
 * 10/7/08: a) record "smallcaps" for genus/tribe names this is necessary when a
 * taxonlist is not provided with "smallcaps" info in the extracted records,
 * VolumeVerifier can build a taxon index for VolumeTransformer. b) also keep
 * the original delimiters in names: may be useful for VolumeVerifier.
 * 
 * Chunshui summer 08: To extract the data from the docx file.
 * 
 * The functions include: 1, (TODO)extract the document.xml from the docx file.
 * 2, parse the document.xml 3, output individual treatment in an intermediate
 * xml file.
 * 
 * Only the paragraphs enclosed in the style listed in style-mapping.properties
 * file will be kept.
 * 
 * And save the data to an XML format listing style and text pair for each
 * paragraph.
 * 
 * The output will be processed further by VolumeVerifier.java
 * 
 * @author chunshui
 */

@SuppressWarnings({ "unchecked" })
public class VolumeExtractor extends Thread {

	protected String source;
	// private MainForm mainForm;
	protected static final Logger LOGGER = Logger
			.getLogger(VolumeExtractor.class);

	protected String target;

	protected ProcessListener listener;

	protected int count;

	protected Element treatment;

	protected XMLOutputter outputter;

	// private String start = "Name"; //TODO: include the following in the
	// configuration file: style names indicating the start of a new treatment
	// private String syn = "Syn";
	// private String tribegennamestyle = "smallCaps";
	protected static String start = ".*?(Heading|Name).*"; // starts a treatment
	// public static String start = ""; //starts a treatment
	protected String names = ".*?(Syn|Name).*"; // other interesting names worth parsing
	protected String key = ".*?-Key.*";											
	public String tribegennamestyle = "caps";
	protected static String ignorednames = "incertae sedis";
	private boolean debug = false;
	private boolean keydebug = true;

	public VolumeExtractor(String source, String target,
			ProcessListener listener) {
		this.source = source;
		this.target = target;
		this.listener = listener;
		Registry.TribeGenusNameCase = tribegennamestyle;
		Registry.NomenclatureStylePtn = start;
		Registry.SynonymStylePtn = names;
	}

	/**
	 * Extract the data from the source file
	 * 
	 * TODO: unzip the document.xml from the docx file
	 */

	public void run() {
		listener.setProgressBarVisible(true);
		extract();
		listener.setProgressBarVisible(false);
	}

	public void extract() throws ParsingException {
		try {
			listener.progress(1);
			// init the outputter
			outputter = new XMLOutputter(Format.getPrettyFormat());

			// build the root element from the xml file
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(source + "document.xml");
			if(debug) System.out.println(source + "document.xml");
			Element root = doc.getRootElement();

			// find all <w:p> tags
			List wpList = XPath.selectNodes(root, "/w:document/w:body/w:p");

			// iterate over the <w:p> tags
			count = 1;
			int total = wpList.size();
			for (Iterator iter = wpList.iterator(); iter.hasNext();) {
				// Element test = (Element)iter.next();
				// System.out.println(test.getName());//new added
				processParagraph((Element) iter.next());
				listener.progress((count * 100) / total);
				// output();
			}

			// output the last file
			output();
		} catch (Exception e) {
			LOGGER.error(
					"Unable to parse/ extract the file in VolumeExtractor:extract",
					e);
			e.printStackTrace();
			throw new ParsingException(e);
		}
	}

	/**
	 * To process a w:p tag
	 * 
	 * output style:text pairs for each paragraph
	 * 
	 * @param wp
	 * @throws JDOMException
	 */
	private void processParagraph(Element wp) throws Exception {
		// read the paragraph style
		Attribute att = (Attribute) XPath.selectSingleNode(wp,
				"./w:pPr/w:pStyle/@w:val");// XXX change from @w:val to w:val
		if (att == null) {// TODO: issue a warning
			if(debug) System.out.println("============================================>null");
			return;
		}
		String style = att.getValue();
		if(debug) System.out.println(style);

		// check if a name paragraph reached, assuming a treatment starts with a
		// Name paragraph
		// if (style.indexOf("Name") >= 0) {
		if (style.matches(start)) {// start = ".*?(Heading|Name).*"
			// The code reaches to a name paragraph
			// output the current treatment file
			// if (treatment != null) {
			if (treatment != null) {
				if (treatment.getChild("paragraph") != null) {
					if (treatment.getChild("paragraph").getChild("text") != null
							&& !treatment.getChild("paragraph")
									.getChild("text").getTextTrim()
									.matches(".*?" + ignorednames + ".*")
							&& treatment.getChildren("paragraph").size() >= 2) { 
						// must contain style and text, must contain >=2 paragraphs
						/*
						 * It is not possible for a treatment to just have a
						 * name Heading4 /Taxa incertae sedis from FoC v22, taxa
						 * whose placement is uncertain
						 */

						output(); // ready to write this treatment out
						count++;
					}
				} else {
					output(); // ready to write this treatment out
					count++;
				}
			}

			// logger.info("processing: " + count);
			// create a new output file
			treatment = new Element("treatment");
		}
		populateTreatment(wp, style);
	}

	/*protected void createTreatment() {
		treatment = new Element("treatment");
	}*/

	protected void populateTreatment(Element wp, String style)
			throws JDOMException {
		Element se = new Element("style");
		se.setText(style);

		Element pe = new Element("paragraph");
		pe.addContent(se);

		if (style.matches(start) || style.matches(names)) {
			extractNameParagraph(wp, pe);
		}else if(style.matches(key)){
			extractKeyParagraph(wp, pe); //try to separate a key "statement" from "determination"
		}else {		
			extractTextParagraph(wp, pe);
		}

		// add the element to the treatment (root) element
		treatment.addContent(pe);
	}

	/**
	 * wp containing the text, to be formated as "statement # determination", then add to pe
	 * @param wp
	 * @param pe
	 */
	private void extractKeyParagraph(Element wp, Element pe) throws JDOMException{
		StringBuffer formatted = new StringBuffer();
		List<Element> text = XPath.selectNodes(wp, "./w:r/w:tab");
		Iterator<Element> it = text.iterator();
		while(it.hasNext()){
			Element t = it.next();			
			t.setText("###");
			t.setName("t");
		}
		
		text = XPath.selectNodes(wp, "./w:r/w:t");
		it = text.iterator();
		while(it.hasNext()){
			Element t = it.next();			
			formatted.append(t.getTextTrim()+" ");
		}
		/*
		List<Element> text = XPath.selectNodes(wp, "./w:r/w:t");
		Iterator<Element> it = text.iterator();
		while(it.hasNext()){
			Element t = it.next();			
			if(t.getAttribute("space", Namespace.XML_NAMESPACE) != null && t.getAttributeValue("space", Namespace.XML_NAMESPACE).compareTo("preserve")==0){
				String temp = t.getTextTrim();
				if(temp.length()>0) formatted.append(" ### ").append(temp+" ");
			}else{
				formatted.append(t.getTextTrim()+" ");
			}
		}
		*/
		
		Element te = new Element("text");
		String t = formatted.toString().trim();
		te.setText(t);
		pe.addContent(te);
		if(keydebug) System.out.println(t);
		
	}

	private void extractNameParagraph(Element wp, Element pe)
			throws JDOMException {
		String acase = "";
		List rList = XPath.selectNodes(wp, "./w:r");

		for (Iterator ti = rList.iterator(); ti.hasNext();) {
			Element re = (Element) ti.next();
			// find smallCaps
			Element rpr = (Element) XPath.selectSingleNode(re, "./w:rPr"); // Genus,
																			// Tribe
																			// names
																			// are
																			// in
																			// smallCaps
			if (rpr != null
					&& XPath.selectSingleNode(rpr, "./w:"
							+ tribegennamestyle) != null) {
				acase = tribegennamestyle;
			} else {
				acase = "";
			}
			// collect text
			StringBuffer buffer = new StringBuffer();
			List textList = XPath.selectNodes(re, "./w:t");
			for (Iterator it = textList.iterator(); it.hasNext();) {
				Element wt = (Element) it.next();
				String tmp = wt.getText();
				buffer.append(tmp).append(" ");
			}
			// }
			String text = buffer.toString().replaceAll("\\s+", " ").trim();
			;
			// build the elements
			Element te = null;
			if (text.matches(".*?\\S.*")) { // not an empty string or a
											// number of spaces
				te = new Element("text");
				te.setText(text);
			}
			if(debug) System.out.println("Name: " + acase + " : " + text);
			Attribute ca = null;
			if (!acase.equals("") && te != null) {
				ca = new Attribute("case", tribegennamestyle);
				te.setAttribute(ca);
			}
			if (te != null)
				pe.addContent(te);
		}
		
	}

	private void extractTextParagraph(Element wp, Element pe)
			throws JDOMException {
		StringBuffer buffer = new StringBuffer();

		List textList = XPath.selectNodes(wp, "./w:r/w:t");
		for (Iterator ti = textList.iterator(); ti.hasNext();) {
			Element wt = (Element) ti.next();
			buffer.append(wt.getText()).append("#");
		}
		String text = buffer.toString().replaceAll("-#", "-")
				.replaceAll("#", "").replaceAll("\\s+", " ").trim();

		/*
		 * buffer.append(wt.getText()).append("-"); } String text =
		 * buffer.toString().replaceAll("\\s+", " ").trim();
		 */
		Element te = new Element("text");
		te.setText(text);
		pe.addContent(te);

	}

	/**
	 * To output the <treatment> element
	 * 
	 * @throws IOException
	 */
	private void output() throws ParsingException {
		try {

			String file = target + "extracted\\" + count + ".xml";
			Document doc = new Document(treatment);
			BufferedOutputStream out = new BufferedOutputStream(
					new FileOutputStream(file));
			/* Producer */
			outputter.output(doc, out);

			/* Consumer */
			listener.info(count + "", file);

		} catch (IOException e) {
			LOGGER.error("Exception in VolumeExtractor : output", e);
			throw new ParsingException(e);
		}
	}

	public static String getStart() {
		return start;
	}

	public static void setStart(String start) {
		VolumeExtractor.start = start;
	}
}
