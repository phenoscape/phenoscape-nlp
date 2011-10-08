/**
 * 
 */
package fna.parsing;

import java.io.StringReader;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author Hong Updates
 *
 */
public class WordDocSegmenter extends VolumeExtractor {
	private final String docString = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
			"<w:document xmlns:ve=\"http://schemas.openxmlformats.org/markup-compatibility/2006\" " +
			"xmlns:o=\"urn:schemas-microsoft-com:office:office\" " +
			"xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"" +
			" xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\" " +
			"xmlns:v=\"urn:schemas-microsoft-com:vml\" " +
			"xmlns:wp=\"http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing\" " +
			"xmlns:w10=\"urn:schemas-microsoft-com:office:word\" xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\" " +
			"xmlns:wne=\"http://schemas.microsoft.com/office/word/2006/wordml\">" +
			"<w:body></w:body></w:document>";
	private Element container = null;
	/**
	 * @param source
	 * @param target
	 * @param listener
	 */
	public WordDocSegmenter(String source, String target,
			ProcessListener listener) {
		super(source, target, listener);
		// TODO Auto-generated constructor stub
	}
	/**
	 *
	 *
	 **/
	public void createTreatment(){
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(new StringReader(this.docString));
			Element root = doc.getRootElement();
			container = (Element)XPath.selectSingleNode(root, "/w:document/w:body");
			treatment = root;
			treatment.detach();
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	public void populateTreatment(Element wp, String style){
		wp.detach();
		container.addContent(wp);
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
