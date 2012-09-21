package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

import javax.swing.JFileChooser;

import org.jdom.*;
import org.jdom.output.XMLOutputter;
@SuppressWarnings("unused")
public class V3_Extractor {
	static Element treatment = new Element("treatment");

	public static void main(String[] args) throws Exception {
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Extractor");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = fc.showOpenDialog(fc);
		File resource = null;
		if (result == JFileChooser.APPROVE_OPTION) {
			resource = fc.getSelectedFile();
		}
		// new
		// File("D:/Library Project/work3/part2/vol03h Taxon HTML/vol03h Taxon HTML");
		File[] files = resource.listFiles();
		org.jsoup.select.Elements paras, bolds, As;
		org.jsoup.nodes.Element pe;
		org.jsoup.nodes.Document doc;
		String content = null, spicename = null;
		V3_Extractor ex = new V3_Extractor();
		for (int i = 0; i < files.length; i++) {
			if (files[i].getName().contains(".html")) {
				int selectdetecter = 0;
				doc = org.jsoup.Jsoup.parse(files[i], "UTF-8");
				paras = doc.select("p");
				bolds = doc.select("b");
				As = doc.select("A");
				Iterator<org.jsoup.nodes.Element> paraiter = paras.iterator();
				Iterator<org.jsoup.nodes.Element> bolditer = paras.iterator();
				Element para = null;
				if(!As.isEmpty()&bolds.size()>=2){
					spicename = bolds.get(0).ownText() + " "  + bolds.get(1).ownText() + "......name";
					para = new Element("paragraph");
					para.setText(spicename);
					treatment.addContent(para);
				}
				while (paraiter.hasNext()) {
					boolean bool = true;
					pe = (org.jsoup.nodes.Element) paraiter.next();
					bool = pe.getElementsByTag("b").isEmpty();
					if (!pe.text().isEmpty()) {
						content = pe.text() + "......" + bool;
						if(content.contains("SELECTED REFERENCES")){
							selectdetecter = 1;
							continue;
						}
						if(selectdetecter==1){
							content = "SELECTED REFERENCES " + content;
							selectdetecter = 0;
						}
						para = new Element("paragraph");
						para.setText(content);
						treatment.addContent(para);
					}
				}
				String name = files[i].getName().replaceAll(".html", "");
				ex.outputter(name);
				ex.createtreatment();
			}
		}
	}

	private void outputter(String filename) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		String file = "C:/Users/Li Chen/Desktop/FNA/V3/Extracted/" + filename
				+ ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}

	private void createtreatment() throws Exception {
		treatment = new Element("treatment");
	}
}



