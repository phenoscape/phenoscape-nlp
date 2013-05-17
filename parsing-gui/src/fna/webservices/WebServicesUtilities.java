package fna.webservices;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Set;

import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;

import fna.parsing.ApplicationUtilities;
import fna.webservices.beans.ScientificName;

public class WebServicesUtilities {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String text = "Chromis abrupta"; //"Probolomyrmecini";
		/*		System.out.println("\n" + text + " is "+ 
		 (new WebServicesUtilities().isName(text) == true ? "present " : "absent ") 
		 +"in " + ApplicationUtilities.getProperty("HNS") );*/

		getNameInfo(text, ApplicationUtilities.getProperty("ZOOBANK"));
	}

	/**
	 * This function tells if "text" is a name recognized by any name server
	 * @param text
	 * @return
	 */
	public boolean isName(String text) {

		try {
			/* Checking in the HNS */
			if (text.contains(" ")) {
				if (checkHNSServer(text.substring(0, text.indexOf(" ")))) {
					return true;
				}
			} else {
				if (checkHNSServer(text)) {
					return true;
				}
			}

			/* Checking in Zoobank */

			if (checkZoobankServer(text)) {
				return true;
			}
		} catch (Exception exe) {
			exe.printStackTrace();
		}

		return false;
	}

	/**
	 * This function cjecks the name in Zoobank Server
	 * @param text
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	private boolean checkZoobankServer(String text) throws Exception {
		Parser parser = new Parser(ApplicationUtilities.getProperty("ZOOBANK")
				+ URLEncoder.encode(text));
		TagNameFilter filter = new TagNameFilter("SPAN");
		NodeList list = parser.parse(filter);
		boolean found = false;

		for (int i = 5; i < list.size(); i++) {
			found = processMyNodes(text, list.elementAt(i), false);
			if (found) {
				break;
			}
		}
		return found;
	}

	/**
	 * This function checks the name in HNS Server
	 * @param text
	 * @return
	 * @throws Exception
	 */
	private boolean checkHNSServer(String text) throws Exception {

		Parser parser = new Parser(ApplicationUtilities.getProperty("HNS")
				+ text);
		TagNameFilter filter = new TagNameFilter("TR");
		NodeList list = parser.parse(filter);
		boolean found = false;

		for (int i = 1; i < list.size(); i++) {
			found = processMyNodes(text, list.elementAt(i), false);
			if (found) {
				break;
			}
		}
		return found;
	}

	/**
	 * This is an utility ot parse the HTML
	 * @param text
	 * @param node
	 * @param exact
	 * @return
	 * @throws Exception
	 */
	protected static boolean processMyNodes(String text, Node node,
			boolean exact) throws Exception {
		boolean returnValue = false;
		if (node instanceof TextNode) {
			TextNode name = (TextNode) node;
			if (exact) {
				if (name.getText().equalsIgnoreCase(text)) {
					returnValue = true;
					return returnValue;
				}
			} else {
				if (name.getText().contains(text)) {
					returnValue = true;
					return returnValue;
				}
			}

		} else if (node instanceof TagNode) {
			TagNode tag = (TagNode) node;
			NodeList nl = tag.getChildren();
			if (null != nl)
				for (NodeIterator i = nl.elements(); i.hasMoreNodes();) {
					returnValue = processMyNodes(text, i.nextNode(), exact);
					if (returnValue) {
						break;
					}
				}

		}
		return returnValue;
	}

	/**
	 * This function checks whether the name exists in the given server
	 * @param text
	 * @param source
	 * @return
	 */
	public boolean isName(String text, String source) {

		try {
			if (source.equalsIgnoreCase("HNS")) {
				return checkHNSServer(text);
			}

			if (source.equalsIgnoreCase("ZOOBANK")) {
				return checkZoobankServer(text);
			}
		} catch (Exception exe) {
			exe.printStackTrace();
		}

		return false;
	}

	/**
	 * This function gets the Scientific Name from the designated source server
	 * @param name
	 * @param src
	 * @return
	 */
	@SuppressWarnings("deprecation")
	public static ScientificName getNameInfo(String name, String src)
			throws Exception {
		/* Implemented only for Zoobank server */

		ScientificName scientificName = new ScientificName();
		String url = src + URLEncoder.encode(name) + "%25";
		Parser parser = new Parser(url);
		TagNameFilter filter = new TagNameFilter("A");
		NodeList list = parser.parse(filter);
		String href = "";
		for (int i = 0; i < list.size(); i++) {
			href = getHref(name, list.elementAt(i));
			if (!href.equals("")) {
				break;
			}
		}

		if (!href.equals("")) {
			Parser nameParser = new Parser(href);
			TagNameFilter nameFilter = new TagNameFilter("span");
			NodeList nodelist = nameParser.parse(nameFilter);

			for (int i = 0; i < nodelist.size(); i++) {
				TagNode node = (TagNode) nodelist.elementAt(i);
				String id = node.getAttribute("id");
				if (id != null) {
					if (id.equals("ctl00_ContentPlaceHolder_LabelFullName")) {
						System.out.println(node.toPlainTextString());
						scientificName.setName(node.toPlainTextString());
					}

					if (id.equals("ctl00_ContentPlaceHolder_LabelAuthorship")) {
						System.out.println(node.toPlainTextString());
						scientificName.setAuthor(node.toPlainTextString());
					}

					if (id.equals("ctl00_ContentPlaceHolder_LabelPublishedIn")) {
						System.out.println(node.toPlainTextString());
						scientificName.setFirstPublished(node
								.toPlainTextString());
					}
				}

			}
		}
		return scientificName;
	}

	/**
	 * This function extracts the href from the matching name anchor tag
	 * @param text
	 * @param node
	 * @return
	 * @throws Exception
	 */
	private static String getHref(String text, Node node) throws Exception {
		if (node instanceof TagNode) {
			TagNode tag = (TagNode) node;
			if (tag.getParent().getParent().getText().contains(
					"ctl00_ContentPlaceHolder_ActResults")) {
				if (tag.getFirstChild().getText().equals("b")) {
					String[] terms = tag.toPlainTextString().split(" ");
					String name = terms[1] + " "
							+ terms[0].substring(0, terms[0].indexOf(","));
					if (name.equalsIgnoreCase(text)) {
						return "http://www.zoobank.org/?lsid="
								+ tag.getAttribute("href").substring(1);
					}
				}
			}

		}
		return "";
	}

	/**
	 * This function annotates a text segment using the src name server 
	 * @param segment
	 * @param source
	 * @return
	 */
	public String annotateNames(String segment, String source) {
		/* Not working on this now*/
		return null;
	}

	/**
	 * This function returns a list of names mentioned in the segment
	 * @param segment
	 * @param source
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public Set<String> names(String segment, String source, String type)
			throws Exception {
		/*Implemented using Plazi Web Service*/
		/** The following types can be selected for search :
		 * 	Ants&Spiders 
			Ants&Spiders-OCR
			Ants&Spiders-RdE
			Botany
			Botany.Partha.web
			Botany.web
			Default
			Default.web (Default)
		 */
		return GNILookUp.lookUpScientificName(segment, source, type);
	}

	/**
	 * This function returns all name servers the class knows how to access
	 * @return
	 */
	public HashMap<String, String> servers() {

		HashMap<String, String> servers = new HashMap<String, String>();

		servers.put("HNS", ApplicationUtilities.getProperty("HNS"));
		servers.put("ZOOBANK", ApplicationUtilities.getProperty("ZOOBANK"));
		servers.put("FISHBASE", ApplicationUtilities.getProperty("FISHBASE"));
		servers.put("Index-Fungorum", ApplicationUtilities
				.getProperty("Index-Fungorum"));
		servers.put("Geonames", ApplicationUtilities.getProperty("Geonames"));
		servers.put("FallingRain", ApplicationUtilities
				.getProperty("FallingRain"));

		return servers;
	}
}
