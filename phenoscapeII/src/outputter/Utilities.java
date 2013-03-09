/**
 * 
 */
package outputter;

import java.util.Hashtable;
import java.util.List;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author updates
 *
 */
public class Utilities {

	/**
	 * 
	 */
	public Utilities() {
		// TODO Auto-generated constructor stub
	}
	

	/**
	 * search in all relations in root and replace oldid with newid for all from and to attributes
	 * 
	 * @param oldid
	 * @param newid
	 * @param root
	 */
	@SuppressWarnings("unchecked")
	public static void changeIdsInRelations(String oldid, String newid, Element root) throws Exception {

		List<Element> rels = XPath.selectNodes(root, "//relation[@to='" + oldid + "'|@from='" + oldid + "']");
		for (Element rel : rels) {
			if (rel.getAttributeValue("to").compareTo(oldid) == 0)
				rel.setAttribute("to", newid);
			if (rel.getAttributeValue("from").compareTo(oldid) == 0)
				rel.setAttribute("from", newid);
		}
	}
	
	/**
	 * trace part_of relations of structid to get all its parent structures,
	 * separated by , in order
	 * 
	 * @param root
	 * @param xpath
	 *            : "//relation[@name='part_of'][@from='"+structid+"']"
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String getStructureChain(Element root, String xpath) {
		String path = "";
		try{
			List<Element> relations = XPath.selectNodes(root, xpath);			
			xpath = "";
			for (Element r : relations) {
				String pid = r.getAttributeValue("to");
				path += Utilities.getStructureName(root, pid) + ",";
				String[] pids = pid.split("\\s+");
				for (String id : pids) {
					if (id.length() > 0)
						xpath += "//relation[@name='part_of'][@from='" + id + "']|";
				}
			}
			if (xpath.length() > 0) {
				xpath = xpath.replaceFirst("\\|$", "");
				path += getStructureChain(root, xpath);
			} else {
				return path.replaceFirst(",$", "");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return path.replaceFirst(",$", "");
	}

	
	public static String formQualityValueFromCharacter(Element chara) {
		String charatype = chara.getAttribute("char_type") != null ? "range" : "discrete";
		String quality = "";
		if (charatype.compareTo("range") == 0) {
			quality = chara.getAttributeValue("from") + " " + (chara.getAttribute("from_unit") != null ? chara.getAttributeValue("from_unit") : "") + " to "
					+ chara.getAttributeValue("to") + " " + (chara.getAttribute("to_unit") != null ? chara.getAttributeValue("to_unit") : "");

		} else {
			quality = (chara.getAttribute("modifier") != null && chara.getAttributeValue("modifier").matches(".*?\\bnot\\b.*") ? "not" : "") + " "
					+ chara.getAttributeValue("value") + " " + (chara.getAttribute("unit") != null ? chara.getAttributeValue("unit") : "") + "["
					+ (chara.getAttribute("modifier") != null ? chara.getAttributeValue("modifier").replaceAll("\\bnot\\b;?", "") : "") + "]";

		}
		quality = quality.replaceAll("\\[\\]", "").replaceAll("\\s+", " ").trim();
		return quality;
	}

	/**
	 * Get structure name from the XML results of CharaParser.
	 * 
	 * @param root
	 * @param structids
	 *            : 1 or more structids
	 * @return
	 */
	public static String getStructureName(Element root, String structids) {
		String result = "";

		String[] ids = structids.split("\\s+");
		for (String structid : ids) {
			try{
				Element structure = (Element) XPath.selectSingleNode(root, "//structure[@id='" + structid + "']");
				String sname = "";
				if (structure == null) {
					sname = "REF"; // this should never happen
				} else {
					sname = ((structure.getAttribute("constraint") == null ? "" : structure.getAttributeValue("constraint")) + " " + structure.getAttributeValue("name"));
				}
				result += sname + ",";
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		result = result.replaceAll("\\s+", " ").replaceFirst(",$", "").trim();
		return result;
	}
	


	/**
	 * like array join function in Perl.
	 *
	 * @param tokens the tokens
	 * @param start the start index
	 * @param end the end index, inclusive
	 * @param delimiter the delimiter
	 * @return the string
	 */
	public static String join(String[] tokens, int start, int end,
			String delimiter) {
		String result = "";
		for(int i = start; i <=end; i++) result += tokens[i]+delimiter;
		return result.replaceFirst(delimiter+"$", "");
	}
	
	public static void initEQHash(Hashtable<String, String> EQ) {
		EQ.put("source", "");
		EQ.put("characterid", "");
		EQ.put("stateid", "");
		EQ.put("description", "");
		EQ.put("type", ""); // do not output type to table
		EQ.put("entity", "");
		EQ.put("entitylabel", "");
		EQ.put("entityid", "");
		EQ.put("quality", "");
		EQ.put("qualitylabel", "");
		EQ.put("qualityid", "");
		EQ.put("qualitynegated", "");
		EQ.put("qualitynegatedlabel", "");
		EQ.put("qnparentlabel", "");
		EQ.put("qnparentid", "");
		EQ.put("qualitymodifier", "");
		EQ.put("qualitymodifierlabel", "");
		EQ.put("qualitymodifierid", "");
		EQ.put("entitylocator", "");
		EQ.put("entitylocatorlabel", "");
		EQ.put("entitylocatorid", "");
		EQ.put("countt", "");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
