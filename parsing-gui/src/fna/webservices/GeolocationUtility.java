package fna.webservices;

import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import fna.parsing.ApplicationUtilities;

public class GeolocationUtility extends WebServicesUtilities{

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		String x = "Tucson";
		System.out.println(x + 
				(isPlaceName(x, "FallingRain")? " exists" : " doesn't exist!"));
		
	}
	/**
	 * This function validates a place name from the given servers
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public static boolean isPlaceName(String text) throws Exception {
		
		if(lookUpinGeolocation(text)) {
			return true;
		}
		
		if (lookUpInfallingRain(text)) {
			return true;
		}
		return false;
	}
	
	/**
	 * This function looks up the place name in source server
	 * @param text
	 * @param src
	 * @return
	 * @throws Exception
	 */
	public static boolean isPlaceName(String text, String src) throws Exception {
		
		if (src.equalsIgnoreCase("Geonames")) {
			return lookUpinGeolocation(text);
		}
		
		if (src.equalsIgnoreCase("FallingRain")) {
			return lookUpInfallingRain(text);
		}
		return false;
	}
	/**
	 * This function looks up the name in the Geolocation server
	 * @param text
	 * @return
	 * @throws Exception
	 */
	private static boolean lookUpinGeolocation(String text)throws Exception {
		String url = ApplicationUtilities.getProperty("Geonames");
		URL geoUrl = new URL(url+URLEncoder.encode(text, "UTF-8") );
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(geoUrl.openStream());
        NodeList nodes = doc.getElementsByTagName("name");
        
        for (int i=0; i< nodes.getLength(); i++) {
        	Element element = (Element)nodes.item(i);
        	String nodeValue = element.getFirstChild().getNodeValue();
        	if (nodeValue.equalsIgnoreCase(text)) {
        		return true;
        	}
        }
		return false;
	}
	
	/**
	 * This function looks up the place name in falling rain server
	 * @param text
	 * @return
	 * @throws Exception
	 */
	private static boolean lookUpInfallingRain(String text) throws Exception {
		if (text == null || text.equals("")){
			return false;
		}
	//	String url = ApplicationUtilities.getProperty("Geonames");
		String place = text.replaceAll(" ", "");
		
		if(text.length() <= 5) {
			place = formatText(place);
		} else if (text.length() > 5){
			place = formatText(place.substring(0,place.length()/2+1));			
		}
		
		 Parser parser = new Parser(ApplicationUtilities.getProperty("FallingRain") 
				 + place);
		 boolean found = false;
		 TagNameFilter filter = new TagNameFilter ("A");
		 org.htmlparser.util.NodeList list = parser.parse(filter);	 
		 
		 for (int i = 0; i< list.size(); i++ ) {
			 found = processMyNodes(text, list.elementAt(i), true);
			 if(found) {
				 break;
			 }
		 }
		 
		 
		return found;
	}
	
	/**
	 * This function inserts '/' into text in order to 
	 * format it the way Falling Rain server wants it
	 * @param text
	 * @return
	 */
	private static String formatText(String text){
		char [] letters = text.toCharArray();
		StringBuffer newtext = new StringBuffer();
		for (char c : letters) {
			newtext.append(c+ "/");
		}
		return newtext.toString();
	}
}
