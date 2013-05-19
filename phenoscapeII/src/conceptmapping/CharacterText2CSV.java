/**
 * 
 */
package conceptmapping;

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author hong cui
 * the cvs file output by CharaParser-Biocreative2012 version does have character text in it, 
 * making it difficult for biocruators to review the results
 * This class adds text to the cvs file.
 * 
 * the input cvs file has cols in this order: 
 * source	characterid	stateid	description	entity	entitylabel	entityid	quality	qualitylabel	qualityid	qualitynegated	qualitynegatedlabel	qnparentlabel	qnparentid	qualitymodifier	qualitymodifierlabel	qualitymodifierid	entitylocator	entitylocatorlabel	entitylocatorid	countt
 */
public class CharacterText2CSV {
	Element root;
	Path csvpath;
	Path newcsv;
	final static Charset ENCODING = StandardCharsets.UTF_8;
	
	/**
	 * 
	 */
	public CharacterText2CSV(String nexmlfilepath, String csvfilepath) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document xml = builder.build(new File(nexmlfilepath));
			root = xml.getRootElement();
			csvpath = Paths.get(csvfilepath);
			newcsv = Paths.get(csvfilepath.replaceFirst("\\.csv$", ".new.csv"));
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public void addCharacaterText(){
		ArrayList<String> rows = new ArrayList<String>();
		try{
			Scanner scanner =  new Scanner(csvpath, ENCODING.name());
			while (scanner.hasNextLine()){
				String line = scanner.nextLine();
			    if(line.startsWith("\"source\"")){
			    	rows.add(line);
			    	continue;
			    }
			    String[] cols = line.split("\",\"");
			    String characterid = cols[1].trim();
			    XPath xpath = XPath.newInstance("//nex:char[@states='"+characterid+"']");
			    Element e = (Element) xpath.selectSingleNode(root);
			    String text = "";
			    if(e.getAttribute("label")!=null) text = e.getAttributeValue("label");
			    text = newLine(cols, text);
			    System.out.println("fetch character text:"+text);
			    rows.add(text); //insert the new line with character text
			    rows.add(line); //add the original line
			}   
			System.out.println("creating new csv file");
			write(rows);
			scanner.close();
			System.out.println("new csv file is at "+newcsv.getFileName());
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private String newLine(String[] cols, String charactertext){
		StringBuffer sb = new StringBuffer();
		sb.append("\""+cols[0].replaceFirst("^\"", "")+"\","); //source
		sb.append("\""+cols[1]+"\","); //characterid
		sb.append("\"\","); //stateid
		sb.append("\""+charactertext+"\","); //description
		for(int i = 4; i < cols.length; i++){
			sb.append("\"\","); //all other cols
		}
		return sb.toString().replaceFirst(",$", "").trim();
	}

	 void write(ArrayList<String> aLines) throws Exception {
		    try (BufferedWriter writer = Files.newBufferedWriter(newcsv, ENCODING)){
		      for(String line : aLines){
		        writer.write(line);
		        writer.newLine();
		      }
		    }
	 }
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String nexml = "C:/Users/updates/CharaParserTest/EQ-OLeary2013/Trail_12_April/source/OLeary_et_al_2013.xml";
		String csv = "C:/Users/updates/CharaParserTest/EQ-OLeary2013/mammal-output.csv"; 
		CharacterText2CSV ctc = new CharacterText2CSV(nexml, csv);
		ctc.addCharacaterText();

	}

}
