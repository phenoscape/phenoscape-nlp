/**
 * 
 */
package outputter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import outputter.data.CompositeEntity;
import outputter.data.EQProposals;
import outputter.data.Entity;
import outputter.data.EntityProposals;
import outputter.data.FormalConcept;
import outputter.data.FormalRelation;
import outputter.data.Quality;
import outputter.data.QualityProposals;
import outputter.data.REntity;
import outputter.data.RelationalQuality;
import outputter.data.SimpleEntity;
import outputter.knowledge.Dictionary;
import outputter.process.BinaryCharacterStatementParser;
import outputter.process.Parser;
import outputter.process.StateStatementParser;
import outputter.search.SynRingVariation;
import outputter.search.TermSearcher;
import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Cui
 *
 */
public class Utilities {
	private static final Logger LOGGER = Logger.getLogger(Utilities.class);   
	private static Hashtable<String, String> entityhash = new Hashtable<String, String>();

	private static Pattern p2 = Pattern.compile("(.*?)(\\d+) to (\\d+)");
	private static Pattern p1 = Pattern.compile("(first|second|third|forth|fouth|fourth|fifth|sixth|seventh|eighth|ninth|tenth)\\b(.*)");
	public static String preposition = "of|in|on|between|with|from|to|into|toward";
	private static int relationlength = 3;
	public static ArrayList<String> partofrelations = new ArrayList<String>();
	static{
		partofrelations.add("part_of");
		partofrelations.add("in");
		partofrelations.add("on");
	}

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
			LOGGER.error("", e);
		}


		return false;
	}*/

	public static List<Element>  relationWithStructureAsSubject(String sid, Element root) {
		try{
			XPath tostructure = XPath.newInstance(".//relation[@from='"+sid+"']");
			List<Element> rel = (List<Element>)tostructure.selectNodes(root);
			if(rel != null) return rel;

		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(sw.toString());
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
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(sw.toString());
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
	 * @count number of rounds in the iteration
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
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(sw.toString());
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
			if (xpath.length() > 0 && count < Utilities.relationlength ) {
				xpath = xpath.replaceFirst("\\|$", "");
				path += getStructureChainIds(root, xpath, count++);
			} else {
				return path.replaceFirst(",$", "");
			}
		}catch(Exception e){
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(sw.toString());
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
					System.out.println((new XMLOutputter(Format.getPrettyFormat())).outputString(root));
					sname = "ERROR"; // this should never happen
				} else {
					sname = ((structure.getAttribute("constraint") == null ? "" : structure.getAttributeValue("constraint")) + " " + structure.getAttributeValue("name").replaceAll("\\s+", "_"));
				}
				result += sname + ",";
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(sw.toString());
			}
		}
		result = result.replaceAll("\\s+", " ").replaceFirst(",$", "").trim();
		return result;
	}

	/**
	 * Get structure names for 1 or more structids from the XML results of CharaParser.
	 * 
	 * @param root
	 * @param structids
	 *            : 1 or more structids
	 * @return
	 */
	public static String getOriginalStructureName(Element root, String structids) {
		String result = "";

		String[] ids = structids.split("\\s+");
		for (String structid : ids) {
			try{
				Element structure = (Element) XPath.selectSingleNode(root, "//structure[@id='" + structid + "']");
				String sname = "";
				if (structure == null) {
					return null;
				} else {
					sname = ((structure.getAttribute("constraint") == null ? "" : structure.getAttributeValue("constraint")) + " " + structure.getAttributeValue("name_original"));
				}
				result += sname + ",";
			}catch(Exception e){
				StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
				LOGGER.error(sw.toString());
			}
		}
		result = result.replaceAll("\\s+", " ").replaceFirst(",$", "").trim();
		return result;
	}


	/**
	 * like the array join function in Perl.
	 *
	 * @param tokens the tokens
	 * @param start the start index, inclusive
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
	 * abc_1_to_3 => abc 1, abc 2, abc 3
	 * @param entitylist: a comma-separated list of (maybe indexed) structures: entity1, entity2
	 * @return a comma-separated list of the same structures with all covered indexes enumerated and turned to numbers.
	 *         If input string is not indexed structures, the original string will be returned. 
	 */
	public static String transformIndexedStructures(String entitylist) {
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
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(sw.toString());
		} catch (IOException e) {
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(sw.toString());
		}
		Element root = xml.getRootElement();

		System.out.println(Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='o229']", 0));

	}

	/**
	 * to allow using "bearer of [quality]" to construct a CompositeEntity
	 * this wrapper is not needed if REntity allowed relation+quality besides relation+entity.
	 * @param q
	 * @return
	 */
	public static SimpleEntity wrapQualityAs(Quality q) {
		SimpleEntity qentity = new SimpleEntity();
		qentity.setClassIRI(q.getClassIRI());
		qentity.setConfidenceScore(q.getConfidenceScore());
		qentity.setId(q.getId());
		qentity.setLabel(q.getLabel());
		qentity.setSearchString(q.getSearchString());
		qentity.setString(q.getString());
		return qentity;
	}

	/**
	 * add ep to entities, grouping proposals with the same phrase/string together
	 * @param entities
	 * @param ep
	 */
	public static void addEntityProposals(ArrayList<EntityProposals> entities,
			EntityProposals eps) {
		if(eps==null) return;
		/*for(EntityProposals aep: entities){
			for(Entity ex: aep.getProposals()){
				ArrayList<Entity> eproposals = ep.getProposals();
				for(int i = 0; i < eproposals.size(); i++){
					Entity in = eproposals.get(i);
					if(ex.content().compareTo(in.content())==0){
						eproposals.remove(in); //deduplicate
					}
				}
			}
		}*/
		//deduplicate in ep
		EntityProposals ep = eps.clone();
		Iterator<EntityProposals> it = entities.iterator();
		while(it.hasNext()){
			EntityProposals aep = it.next();
			Iterator<Entity> ite = aep.getProposals().iterator();
			while(ite.hasNext()){
				Entity ex = ite.next();
				ArrayList<Entity> eproposals = ep.getProposals();
				Iterator<Entity> itp = eproposals.iterator();
				while(itp.hasNext()){
					Entity in = itp.next();
					if(ex.content()!=null && ex.content().compareTo(in.content())==0){
						//eproposals.remove(in); //deduplicate
						itp.remove();
					}
				}
			}
		}
		eps = ep;
		//grouping
		for(EntityProposals aep: entities){
			if(ep.getPhrase().compareTo(aep.getPhrase())==0){
				aep.add(ep);
				return;
			}
		}
		//add new
		entities.add(ep);
	}
	
	/**
	 * add qp to qualities, grouping proposals with the same phrase/string together
	 * not adding duplicates
	 * @param qualities
	 * @param qp
	 */
	public static void addQualityProposals(ArrayList<QualityProposals> qualities,
			QualityProposals qp) {
		if(qp==null) return;
		for(QualityProposals aqp: qualities){
			for(Quality qx: aqp.getProposals()){
				ArrayList<Quality> qproposals = qp.getProposals();
				for(int i = 0; i<qproposals.size(); i++){
					Quality in = qproposals.get(i);
					if(qx.content().compareTo(in.content())==0){
						qproposals.remove(in);//deduplicate
					}
				}
			}
		}
		
		for(QualityProposals aqp: qualities){
			if(qp.getPhrase().compareTo(aqp.getPhrase())==0){
				aqp.add(qp);
				return;
			}
		}
		qualities.add(qp);
	}
	
	/**
	 * 
	 * @param root
	 * @param structureid
	 * @return
	 */
	public static boolean isConstraint(Element root, String structureid) {
		try{
			XPath constraintid = XPath.newInstance("//character[@constraintid='"+structureid+"']");
			if(constraintid.selectSingleNode(root)!=null) return true;
		}catch(Exception e){
			LOGGER.error("", e);
		}
		return false;
	}
	
	/**
	 * type of each quality (simple or relational) determines the relation to be used to postcompose an entity
	 * entity: carpal bone quality:ossified, twisted
	 * => carpal bone (bearer_of ossisied) and (bearer_of twisted)
	 * @param entities
	 * @param qualities
	 * @return postcomposition success or not
	 */
	public static boolean postcompose(ArrayList<EntityProposals> entities, ArrayList<QualityProposals> qualities){
		if(entities==null) return false;
		boolean success = false;
		for(EntityProposals entity: entities){
			ArrayList<Entity> eps = entity.getProposals();
			ArrayList<Entity> epsresult = new ArrayList<Entity>(); //for saving postcomposed entity proposals 
			boolean postcomped = false;
			for (Entity e: eps){
				CompositeEntity ce = new CompositeEntity(); //all qualities are composed into the ce or ecopy, if the latter is a compositie entity
				Entity ecopy = (Entity) e.clone(); //create fresh copy
				for(QualityProposals quality: qualities){
					ArrayList<Quality> qps = quality.getProposals();
					for(Quality q: qps){
						if(q instanceof RelationalQuality){
							//check if the relation is in the restricted list for post composition
							QualityProposals relation = ((RelationalQuality) q).getQuality();
							EntityProposals rentity= ((RelationalQuality) q).getRelatedEntity();
							ArrayList<Quality> relations = relation.getProposals();
							for(Quality r : relations){
								if(r.isOntologized() && isRestrictedRelation(r.getId())){
									//Entity ecopy = (Entity) e.clone(); //create fresh copy
									//increase confidence
									//create RE and create compositeEntity
									FormalRelation fr = new FormalRelation();
									fr.setClassIRI(r.getClassIRI());
									fr.setConfidenceScore(r.getConfidenceScore());
									fr.setId(r.getId());
									fr.setLabel(r.getLabel());
									fr.setSearchString(r.getSearchString());
									fr.setString(r.getString());
									for(Entity e1: rentity.getProposals()){
										REntity re = new REntity(fr, e1);
										if(ecopy instanceof CompositeEntity){
											((CompositeEntity) ecopy).addEntity(re); 
											postcomped = true;
											epsresult.add(ecopy); //save a proposal
										}else{
											//CompositeEntity ce = new CompositeEntity(); 
											ce.addEntity(ecopy);
											ce.addEntity(re);		
											postcomped = true;
											epsresult.add(ce); //save a proposal
										}										
									}							
								}
							}
						}else{
							//bear_of some Ossified: quality Ossified must be treated as a simple entity to form a composite entity
							//Entity ecopy = (Entity) e.clone();
							SimpleEntity qentity = Utilities.wrapQualityAs(q);
							FormalRelation fr = Dictionary.bearerof;
							fr.setConfidenceScore(1f);
							REntity re = new REntity(fr, qentity); //bearer of some Ossified
							//CompositeEntity ce = new CompositeEntity(); 
							ce.addEntity(ecopy);
							ce.addEntity(re);											
							epsresult.add(ce); //save a proposal
							postcomped = true;
						}
					}
				}
			}
			//eps = epsresult; //update entities
			if(postcomped){
				entity.setProposals(epsresult);
				success = true;
			}
		}
		return success;
	}

	private static boolean isRestrictedRelation(String id) {
		if(Dictionary.resrelationQ.get(id) == null) return false;
		return true;
	}

	public static String getSynRing4Phrase(String phrase){
		String synring = "";
		if(phrase.length()==0) return synring;
		phrase = phrase.replaceAll("(\\(\\?:|\\))", ""); //(?:(?:shoulder) (?:girdle)) =>shoulder girdle
		String[] tokens = phrase.split("\\s+");
		//may use a more sophisticated approach to construct ngrams: A B C => A B C;A (B C); (A B) C;
		for(int i = 0; i < tokens.length; i++){
			if(tokens[i].matches(Dictionary.spatialtermptn)) synring += "(?:"+SynRingVariation.getSynRing4Spatial(tokens[i])+")"+" ";
			else synring += "(?:"+SynRingVariation.getSynRing4Structure(tokens[i])+")"+" ";
		}
		return synring.trim();
	}

	public static String getIdsOnPartOfChain(Element root, String structureid) {
		return getStructureChainIds(root, partofXpath(structureid), 0);
		
	}
	
	public static String getNamesOnPartOfChain(Element root, String structureid) {
		return getStructureChain(root, partofXpath(structureid), 0);	
	}
	
	/*
	 * return: "//relation[@name='part_of'][@from='" + structureid + "']" +
				"|//relation[@name='in'][@from='" + structureid + "']" +
				"|//relation[@name='on'][@from='" + structureid + "']"
	 */
	public static String partofXpath(String structureid){
		String path = "";
		for(String partof: partofrelations){
			path += "//relation[@name='"+partof+"'][@from='" + structureid + "']|";
		}
		return path.replaceFirst("\\|+$","");
	}

	/**
	 * 
	 * @param entities
	 * @return true if entities hold a simple spatial entity
	 */
	public static boolean holdsSimpleSpatialEntity(ArrayList<EntityProposals> entities) {
		for(EntityProposals ep : entities){
			for(Entity e: ep.getProposals()){
				if(e instanceof SimpleEntity){
					if(e.getId()==null){
						if(e.getString().matches(".*\\b("+Dictionary.spatialheadnouns+")\\b.*")) return true;
						if(e.getString().matches(".*\\b("+Dictionary.spatialtermptn+")\\b.*")) return true;
					}
					if(e.getId().startsWith(Dictionary.spatialOntoPrefix)) return true;
				}
			}
		}
		return false;
	}


	public static void constructEQProposals(Parser parser, ArrayList<EQProposals> EQStatements, List<QualityProposals> qualities, 
			ArrayList<EntityProposals> entities, EQProposals empty, ArrayList<String> qualityclues){
		if(entities!=null && entities.size()>0 && qualities!=null && qualities.size()>0){//has both E and Q
			for (QualityProposals qualityp : qualities){
				for (EntityProposals entityp : entities) {
					//EQProposals eqp = new EQProposals();
					//if(!EQExsit(EQStatements, entityp, qualityp)){
						EQProposals eqp = empty.clone();
						eqp.setEntity(entityp);
						eqp.setQuality(qualityp);
						if (parser instanceof StateStatementParser){
							eqp.setType("state");
						}else{
							eqp.setType("character");
						}
						EQStatements.add(eqp);
					//}
				}
			}			
		} else if(entities!=null && entities.size()>0 && parser instanceof BinaryCharacterStatementParser){ //no qualities identified so far
			for (EntityProposals entityp : entities) {
				//EQProposals eqp = new EQProposals();
				//if(!EQExsit(EQStatements, entityp, null)){
					EQProposals eqp = empty.clone();
					eqp.setEntity(entityp);
					eqp.setQuality(null); //this may be filled later for BinaryStateStatements
					if (parser instanceof StateStatementParser){
						eqp.setType("state");
					}else{
						eqp.setType("character");
					}
					EQStatements.add(eqp);
				//}
			}					
		}else if(entities!=null && entities.size()>0){ //no qualities => check quality clue or set to "present"
			for (EntityProposals entityp : entities) {
				//EQProposals eqp = new EQProposals();
				EQProposals eqp = empty.clone();
				QualityProposals qualityp = null;
				//try to find qualityp
				//make use of quality clues if there is any
				int count = 0;
				if(qualityclues!=null && qualityclues.size()!=0){//include all ontologizaable quality clues in, TODO may handle them in a finer manner in the future
					QualityProposals qp = new QualityProposals();
					String phrase = "";
					for(String qualityclue : qualityclues){
						phrase += qualityclue +" ";
						ArrayList<FormalConcept> quality = new TermSearcher().searchTerm(qualityclue, "quality");
						if(quality!=null){
							for(FormalConcept q : quality){
								qp.add((Quality)q);
								count++;
							}
						}
					}
					qp.setPhrase(phrase.trim());
					for(Quality q: qp.getProposals()){
						q.setConfidenceScore(1f/count);
					}
					qualityp = qp;
					//eqp.setQuality(qp);
				}else{
					Quality q = Dictionary.present;
					q.setConfidenceScore(1f);
					QualityProposals qp = new QualityProposals();
					qp.add(q);
					qp.setPhrase("present");
					qualityp = qp;
					//eqp.setQuality(qp); 
				}
				
				//if(!EQExsit(EQStatements, entityp, qualityp)){
					eqp.setEntity(entityp);
					eqp.setQuality(qualityp);			
					if (parser instanceof StateStatementParser){
						eqp.setType("state");
					}else{
						eqp.setType("character");
					}
					EQStatements.add(eqp);
				//}
			}					
	}else if(qualities!=null && parser instanceof StateStatementParser){ //E = null, Q !=null from state statement. Ignore quality-only EQ from BinaryStatment. 
		for (QualityProposals qualityp : qualities){
			//if(!EQExsit(EQStatements, null, qualityp)){
				EQProposals eqp = empty.clone();
				eqp.setEntity(null);
				eqp.setQuality(qualityp);
				if (parser instanceof StateStatementParser){
					eqp.setType("state");
				}else{
					eqp.setType("character");
				}
				EQStatements.add(eqp);
			//}	
		}
	}
		
	}

	/**
	 * 
	 * @param EQStatements
	 * @param entityp
	 * @param qualityp
	 * @return
	 */
	/*private static boolean EQExsit(ArrayList<EQProposals> EQStatements,
			EntityProposals entityp, QualityProposals qualityp) {
		for(EQProposals eqp: EQStatements){
			if(entityp!=null && qualityp!=null){
				if(eqp.getEntity().equals(entityp) && eqp.getQuality().equals(qualityp)) return true;
			}else if(entityp ==null && qualityp!=null){
				if(eqp.getEntity()==null && eqp.getQuality().equals(qualityp)) return true;
			}else if(entityp!=null && qualityp==null){
				if(eqp.getEntity().equals(entityp) && eqp.getQuality()==null) return true;
			}else if(entityp==null && qualityp==null){
				return true;
			}
		}
		return false;
	}*/
}


