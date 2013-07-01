/**
 * 
 */
package outputter;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * @author updates
 *
 */
public class Utilities {
	private static Hashtable<String, String> entityhash = new Hashtable<String, String>();

	private static Pattern p2 = Pattern.compile("(.*?)(\\d+) to (\\d+)");
	private static Pattern p1 = Pattern.compile("(first|second|third|forth|fouth|fourth|fifth|sixth|seventh|eighth|ninth|tenth)\\b(.*)");
	public static String preposition = "of|in|on|between|with|from|to|into|toward";

	/**
	 * 
	 */
	public Utilities() {
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * 
	 * @param sid
	 * @param charvalue
	 * @param root
	 * @return ture if the charvalue is a modifier in the original text
	 */
	/*public static boolean isModifier(String sid, String charvalue, Element statement){
		//Element root = statement.getDocument().getRootElement();
		String text = statement.getChildText("text");
		try {
			//construct the name
			Element struct = (Element) XPath.selectSingleNode(statement, ".//structure[@id='"+sid+"']");
			String singularname = struct.getAttributeValue("name");
			String pluralname = TermOutputerUtilities.toPlural(singularname);
			String name = "\\b("+pluralname+"|"+singularname+"|"+singularname.substring(0, singularname.length()-2)+".*?)\\b";
			
			String constraint = "";
			if(struct.getAttribute("constraint")!=null){
				String singularconstraint = struct.getAttributeValue("constraint");
				String plural = "";
				if(!singularconstraint.endsWith(" of")){ //row of
					String [] tokens = singularconstraint.split("\\s+");
					for(String token: tokens){
						plural = TermOutputerUtilities.toPlural(token)+" ";
						constraint += "\\b("+plural+"|"+token+"|"+token.substring(0, token.length()-2)+".*?)\\b\\s+";
					}
				}
			}
			name = constraint+name;
			//similar structures?
			List<Element> structs;
			if(struct.getAttribute("constraint")!=null){
				structs= XPath.selectNodes(statement, ".//structure[@name='"+singularname+"']");
			}else{
				structs= XPath.selectNodes(statement, ".//structure[@name='"+singularname+"']");
			}
			//search in text
			
			
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		
		
		return false;
	}*/
	
	public static List<Element>  relationWithStructureAsSubject(String sid, Element root) {
		try{
			XPath tostructure = XPath.newInstance(".//relation[@from='"+sid+"']");
			List<Element> rel = (List<Element>)tostructure.selectNodes(root);
			if(rel != null) return rel;
			
		}catch(Exception e){
			e.printStackTrace();
		}		
		return null;
	}
	
	/**
	 * 
	 * @param structureid
	 * @return true if the structure has character elements, false if not.
	 */
	public static boolean hasCharacters(String structureid, Element root) {
		try{
			XPath characters = XPath.newInstance(".//Structure[@id='"+structureid+"']/Character");
			List<Element> chars = characters.selectNodes(root);
			if(chars.size()>0) return true;
		}catch(Exception e){
			e.printStackTrace();
		}	
		return false;
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
	 * TODO limit to 3 commas
	 * TODO treat "in|on" as part_of? probably not
	 * @param root
	 * @param xpath
	 *            : "//relation[@name='part_of'][@from='"+structid+"']"
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String getStructureChain(Element root, String xpath, int count) {
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
						xpath += "//relation[@name='part_of'][@from='" + id + "']|//relation[@name='in'][@from='" + id + "']|//relation[@name='on'][@from='" + id + "']|";
				}
			}
			if (xpath.length() > 0 && count < 3) {
				xpath = xpath.replaceFirst("\\|$", "");
				path += getStructureChain(root, xpath, count++);
			} else {
				return path.replaceFirst(",$", "");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return path.replaceFirst(",$", "");
	}

	/**
	 * trace part_of relations of structid to get all its parent structure ids,
	 * separated by , in order
	 * 
	 * TODO limit to 3 commas
	 * TODO treat "in|on" as part_of? probably not
	 * @param root
	 * @param xpath
	 *            : "//relation[@name='part_of'][@from='"+structid+"']"
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static String getStructureChainIds(Element root, String xpath, int count) {
		String path = "";
		try{
			List<Element> relations = XPath.selectNodes(root, xpath);			
			xpath = "";
			for (Element r : relations) {
				String pid = r.getAttributeValue("to");
				path += pid + ",";
				String[] pids = pid.split("\\s+");
				for (String id : pids) {
					if (id.length() > 0)
						xpath += "//relation[@name='part_of'][@from='" + id + "']|//relation[@name='in'][@from='" + id + "']|//relation[@name='on'][@from='" + id + "']|";
				}
			}
			if (xpath.length() > 0 && count < 3) {
				xpath = xpath.replaceFirst("\\|$", "");
				path += getStructureChain(root, xpath, count++);
			} else {
				return path.replaceFirst(",$", "");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return path.replaceFirst(",$", "").trim();
	}

	
	public static String formQualityValueFromCharacter(Element chara) {
		String charatype = chara.getAttribute("char_type") != null ? "range" : "discrete";
		String quality = "";
		if (charatype.compareTo("range") == 0) {
			quality = chara.getAttributeValue("from") + " " + (chara.getAttribute("from_unit") != null ? chara.getAttributeValue("from_unit") : "") + " to "
					+ chara.getAttributeValue("to") + " " + (chara.getAttribute("to_unit") != null ? chara.getAttributeValue("to_unit") : "");

		} else {
			quality = (chara.getAttribute("modifier") != null && chara.getAttributeValue("modifier").matches(".*?\\bnot\\b.*") ? "not" : "") + " "
					+ chara.getAttributeValue("value") + " " + (chara.getAttribute("unit") != null ? chara.getAttributeValue("unit") : "") /*+ "["
					+ (chara.getAttribute("modifier") != null ? chara.getAttributeValue("modifier").replaceAll("\\bnot\\b;?", "") : "") + "]"*/;
			//not to include other modifiers in quality string

		}
		quality = quality.replaceAll("\\[\\]", "").replaceAll("\\s+", " ").trim();
		return quality;
	}
	
	/**
	 * spatial terms in adverb form: e.g. laterally
	 * @param chara
	 * @return 'laterally;ventrally'
	 */
	public static String getSpatialModifierFromCharacter(Element chara) {
		String spatials = "";
		String modifier = chara.getAttribute("modifier") != null ? chara.getAttributeValue("modifier").replaceAll("\\bnot\\b;?", "") : null;
		if(modifier!=null){
			String[] modifiers = modifier.split("\\s*;\\s*");			
			Pattern spatial = Pattern.compile(".*?((?:(?:"+Dictionary.spatialtermptn+")ly ?)+)(.*)");
			for(String mod: modifiers){
				Matcher m = spatial.matcher(mod);
				while(m.matches()){
					spatials += m.group(1).trim()+";";
					mod = m.group(2);
					m = spatial.matcher(mod);
				}
			}
		}
		return spatials.replaceFirst(";$", "").trim();
	}

	/**
	 * Get structure names for 1 or more structids from the XML results of CharaParser.
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
	 * fifth abc => abc 5
	 * abc_1 => abc 1
	 * 
	 * @param entitylist
	 *            : entity1, entity2
	 * @return
	 */
	public static String transform(String entitylist) {
		entitylist = entitylist.replaceAll("(?<=\\w)- (?=\\w)", "-");
		String transformed = entityhash.get(entitylist);
		if (transformed != null)
			return transformed;

		transformed = "";
		if (entitylist.matches(".*?(_[\\divx]+|first|second|third|forth|fouth|fourth|fifth|sixth|seventh|eighth|ninth|tenth).*")) {
			String[] entities = entitylist.split("(?<!_),(?!_)");
			for (String entity : entities) {
				// case one
				entity = entity.trim();
				if (entity.matches(".*?\\b(first|second|third|forth|fouth|fourth|fifth|sixth|seventh|eighth|ninth|tenth)\\b.*")) {
					Matcher m = p1.matcher(entity);
					if (m.matches()) {
						String position = turnPosition2Number(m.group(1));
						entity = m.group(2) + " " + position;
						transformed += entity + ",";
					} else {
						transformed += entity + ",";
					}
					// transformed = transformed.replaceFirst(",$", "").trim();
					// entityhash.put(entitylist, transformed);
					// return transformed;
				} // case two
				else if (entity.matches("(.*?_[\\divx]+)|(.*?_[\\divx]+-[\\divx]+)")) {// abc_1, abc_1_and_2, abc_1_to_3, abc_1-3
					String organ = entity.substring(0, entity.indexOf("_"));

					if (entity.matches(".*?_[\\divx]+-[\\divx]+")) {// abc_1-3
						entity = entity.replaceAll("-", "_to_");// before reformatRomans,replace "-" with "_to_"
					}

					entity = reformatRomans(entity);
					entity = entity.replaceAll("_(?=\\d+)", " ").replaceAll("(?<=\\d)_", " "); // abc_1_and_3 => abc 1 and 3
					if (entity.indexOf(" and ") < 0 && entity.indexOf(" to ") < 0) { // single entity
						transformed += entity + ",";
						// entityhash.put(entitylist, transformed);
						// return transformed;
					} else {// abc 1 and 2
						if (entity.indexOf(" and ") > 0) {
							transformed += entity.replaceFirst(" and ", "," + organ + " ") + ","; // abc 1,abc 2
							// entityhash.put(entitylist, transformed);
							// return transformed;
						}

						// abc 1 , 2 to 5 ; abc 2 to 5
						Matcher m = p2.matcher(entity);
						if (m.matches()) {
							String part1 = m.group(1);
							int from = Integer.parseInt(m.group(2));
							int to = Integer.parseInt(m.group(3));
							String temp1 = "";
							for (int i = from; i <= to; i++) {
								temp1 = temp1 + organ + " " + i + ",";
							}

							String temp = "";
							part1 = part1.replaceAll("\\D", "").trim();
							if (part1.length() > 0) {
								String[] nums = part1.split("\\s+");
								for (String n : nums) {
									temp = temp + organ + " " + n + ",";
								}
							}

							transformed = transformed + temp + temp1;
							// transformed.replaceFirst(",$", "").trim();
							// entityhash.put(entitylist, transformed);
							// return transformed;
						}
					}
				} else {// neither
					transformed += entity + ",";
				}
			}
		} else {
			transformed = entitylist;
			// entityhash.put(entitylist, entitylist);
		}
		transformed = transformed.replaceFirst(",$", "").trim();
		entityhash.put(entitylist, transformed);
		return transformed;
	}


	/**
	 * abc_iv_and_v
	 * 
	 * @param entity
	 * @return
	 */
	private static String reformatRomans(String entity) {
		String[] parts = entity.split("_");
		String reformatted = "";
		for (String part : parts) {
			if (part.matches("[ivx]+"))
				reformatted += turnRoman2Number(part) + "_";
			else
				reformatted += part + "_";
		}
		return reformatted.replaceFirst("_$", "");
	}

	/**
	 * 
	 * @param entity
	 * @return
	 */
	private static String turnRoman2Number(String word) {
		int total = 0;
		if (word.endsWith("iv")) {
			total += 4;
			word = word.replaceFirst("iv$", "");
		}
		if (word.endsWith("ix")) {
			total += 9;
			word = word.replaceFirst("ix$", "");
		}
		int length = word.length();
		for (int i = 0; i < length; i++) {
			if (word.charAt(i) == 'i')
				total += 1;
			if (word.charAt(i) == 'v')
				total += 5;
			if (word.charAt(i) == 'x')
				total += 10;
		}
		return total + "";
	}

	/**
	 * fifth => 5
	 * 
	 * @param word
	 * @return
	 */
	private static String turnPosition2Number(String word) {
		if (word.compareTo("first") == 0)
			return "1";
		if (word.compareTo("second") == 0)
			return "2";
		if (word.compareTo("third") == 0)
			return "3";
		if (word.compareTo("forth") == 0)
			return "4";
		if (word.compareTo("fouth") == 0)
			return "4";
		if (word.compareTo("fourth") == 0)
			return "4";
		if (word.compareTo("fifth") == 0)
			return "5";
		if (word.compareTo("sixth") == 0)
			return "6";
		if (word.compareTo("seventh") == 0)
			return "7";
		if (word.compareTo("eighth") == 0)
			return "8";
		if (word.compareTo("ninth") == 0)
			return "9";
		if (word.compareTo("tenth") == 0)
			return "10";
		return null;
	}
//code to remove prepositions from starting and ending of strings => Hariharan
	public static String removeprepositions(String trim) {
		for(;;)
		{
	   if(trim.matches("("+preposition+")\\s.*"))
		   trim = trim.substring(trim.indexOf(" ")+1);
	   else
		   break;
		}
		
		for(;;)
		{
			if(trim.matches(".*\\s("+preposition+")"))
				   trim = trim.substring(0,trim.lastIndexOf(" "));
			   else
				   break;
		}
		return trim;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File f = new File(ApplicationUtilities.getProperty("source.dir")+"test", "Swartz 2012.xml_states356.xml");
		SAXBuilder builder = new SAXBuilder();
		Document xml=null;
		try {
			xml = builder.build(f);
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Element root = xml.getRootElement();
		
		System.out.println(Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='o229']", 0));

	}

}
