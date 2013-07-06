/* $Id: CharacterAnnotatorChunked.java 997 2011-10-07 01:14:22Z hong1.cui $ */
/**
 * 
 */
package fna.charactermarkup;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.*;
import conceptmapping.*;

/**
 * @author hongcui fnaglossaryfixed: move verbs such as comprising from the
 *         glossary
 * 
 */

@SuppressWarnings({ "unchecked", "unused", "static-access" })
public class CharacterAnnotatorChunked {

	private Element statement = null;
	private ChunkedSentence cs = null;
	private static ArrayList<Element> subjects = new ArrayList<Element>();
	// static so a ditto sent can see the last subject
	private ArrayList<Element> latestelements = new ArrayList<Element>();
	// save the last set of elements added. independent from adding elements to <Statement>
	private ArrayList<Element> elementlog = new ArrayList<Element>();
	// log the sequence in which elements were created
	private String delims = "comma|or";
	private static int structid = 1;
	private static int relationid = 1;
	private String unassignedcharacter = null;
	// private String unassignedmodifiers = null; //holds modifiers that may be
	// applied to the next chunk
	protected Connection conn = null;
	private String tableprefix = null;
	private String glosstable = null;
	private boolean inbrackets = false;
	private String text = null;
	private String notInModifier = "a|an|the";
	private String negationpt = "not|never";
	private String nonrelation = "through|by|to|into";
	private String size ="size|length|width";
	private String lifestyle = "";
	private String characters;
	private boolean partofinference = false;
	private ArrayList<Element> pstructures = new ArrayList<Element>();
	private ArrayList<Element> cstructures = new ArrayList<Element>();
	private boolean attachToLast = false; // this switch controls where a character will be attached to.
											// "true": attach to last organ seen. "false":attach to the
											// subject of a clause
	private boolean printAnnotation = true;
	private boolean debugNum = false;
	private boolean printComma = false;
	private boolean printAttach = false;
	private boolean evaluation = false;
	private boolean printOR = true;
	private String sentsrc;
	private boolean nosubject;
	private boolean debugextraattributes=false;

	/**
	 * 
	 */
	public CharacterAnnotatorChunked(Connection conn, String tableprefix, String glosstable, String characters, boolean evaluation) {
		this.conn = conn;
		this.tableprefix = tableprefix;
		this.glosstable = glosstable;
		this.evaluation = evaluation;
		this.nosubject = false;
		this.characters = characters;
		if (this.evaluation)
			this.partofinference = false; // partofinterference causes huge
											// number of "relations"
		try {
			// collect life_style terms
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct term from " + this.glosstable + " where category='life_style'");
			while (rs.next()) {
				this.lifestyle += rs.getString(1) + "|";
			}
			this.lifestyle = lifestyle.replaceFirst("\\|$", "");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * reset annotator to process next description paragraph.
	 */
	public void reset() {
		this.subjects = new ArrayList<Element>();// static so a ditto sent can
													// see the last subject
		this.latestelements = new ArrayList<Element>();// save the last set of
														// elements added.
														// independent from
														// adding elements to
														// <Statement>
		this.unassignedcharacter = null;
		this.inbrackets = false;
		this.pstructures = new ArrayList<Element>();
		this.cstructures = new ArrayList<Element>();
		this.nosubject = false;

	}

	public Element annotate(String sentindex, String sentsrc, ChunkedSentence cs) throws Exception {
		this.statement = new Element("statement");
		// sentindex: Buckup_1998.xml_088683b8-4718-48de-ad0e-eb1de9c58eb6.txt-0
		String segid = sentsrc.replaceFirst(".*-(?=[^-]+$)", "");
		sentsrc = sentsrc.replaceFirst(".*?\\.xml_", "").replaceFirst("\\.txt.*", "");
		String[] ids = sentsrc.split("_");
		String charaid = ids[0];
		String stateid = null;
		;
		if (ids.length > 1) {
			stateid = ids[1];
		}
		boolean isstate = false;
		this.statement.setAttribute("statement_type", "character");
		this.statement.setAttribute("character_id", charaid);
		if (stateid != null) {
			this.statement.setAttribute("statement_type", "character_state");
			this.statement.setAttribute("state_id", stateid);
			isstate = true;
		}
		this.statement.setAttribute("seg_id", segid);

		this.cs = cs;
		this.text = cs.getText();
		this.sentsrc = sentsrc;
		Element text = new Element("text");// make <text> the first element in
											// statement
		text.addContent(this.text);
		if (!this.evaluation)
			this.statement = addContent(this.statement, text);// add Element,
																// record in
																// elementlog

		int i = 0;
		String token = cs.getTokenAt(i++);
		while (token.length() == 0) {
			token = cs.getTokenAt(i++);
		}
		if (token.startsWith("z[") || token.startsWith("l[") || token.startsWith("u[")) {
			annotateByChunk(cs, false);
		} else {
			establishSubject("(whole_organism)", false);
			cs.setInSegment(true);
			cs.setRightAfterSubject(true);
			annotateByChunk(cs, false);
		}
		/*
		 * String subject= cs.getSubjectText(); if(subject==null &&
		 * cs.getPointer()==0){ Chunk ck = cs.nextChunk(); cs.resetPointer();
		 * establishSubject("(whole_organism)"); annotateByChunk(cs, false);
		 * }//end mohan code else if(subject.equals("measurements")){
		 * this.annotatedMeasurements(this.text); }else
		 * if(!subject.equals("ignore")){ if(subject.equals("ditto")){
		 * reestablishSubject(); }else{ establishSubject(subject);
		 * if(this.partofinference){
		 * this.pstructures.addAll(CharacterAnnotatorChunked.subjects); } }
		 * cs.setInSegment(true); cs.setRightAfterSubject(true);
		 * annotateByChunk(cs, false); }
		 */

		// postprocess functions
		// lifeStyle();
		// if(!this.evaluation) mayBeSameRelation();
		// if(this.partofinference){
		// puncBasedPartOfRelation();
		// }
		
		/*Normalization*/
		removeIsolatedCharacters();
		removeIsolatedWholeOrganismPlaceholders();
		annotateBareStatements();
		//manus digits i-iii => manus digit i, manus digit ii, manus digit iii
		decomposeMultipleStructures();//Changed by Zilong
		standardization();
		
		
		if (printAnnotation) {
			XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
			System.out.println();
			System.out.println(xo.outputString(this.statement));
		}
		return this.statement;
	}

	/**
	 * count = "none" =>count = 0
	 */
	private void standardization() {
		try {
			/* count = "none" =>count = 0 */
			List<Element> es = StanfordParser.path1.selectNodes(this.statement);
			for (Element e : es) {
				e.setAttribute("value", "0");
			}
			


			/*
			 * <structure id="o437" name="tooth"> <character name="presence"
			 * value="no" /> <character name="presence" value="present"
			 * constraint="on fourth upper pharyngeal tooth plate"
			 * constraintid="o438" /> </structure>
			 * 
			 * ==>
			 * 
			 * <structure id="o437" name="tooth"> <character name="presence"
			 * value="absent"
			 * constraint="on fourth upper pharyngeal tooth plate"
			 * constraintid="o438" /> </structure>
			 * 
			 * 
			 * <text>no circuli present on posterior surface of scales</text>
			 * <structure id="o357" name="circulus">
			 * <character name="presence"value="no" />
			 * </structure> <structure id="o358" name="surface"
			 * constraint="posterior" /> <relation id="r130" name="present on"
			 * from="o357" to="o358" negation="false" />
			 * 
			 * ==> <text>no circuli present on posterior surface of
			 * scales</text> <structure id="o357" name="circulus"> </structure>
			 * <structure id="o358" name="surface" constraint="posterior" />
			 * <relation id="r130" name="present on" from="o357" to="o358"
			 * negation="true" />
			 */
			// XPath nopresencech =
			// XPath.newInstance(".//character[@name='presence'][@value='no']");
			es = StanfordParser.path2.selectNodes(this.statement);
			ArrayList<Element> esstructures = new ArrayList<Element>();
			for (Element e : es) {
				esstructures.add(e.getParentElement());
			}
			List<Element> esc = StanfordParser.path3.selectNodes(this.statement);
			ArrayList<Element> escstructures = new ArrayList<Element>();
			for (Element e : esc) {
				escstructures.add(e.getParentElement());
			}
			List<Element> esr = StanfordParser.path4.selectNodes(this.statement);
			ArrayList<Element> esrstructures = new ArrayList<Element>();
			for (Element e : esr) {
				String strid = e.getAttributeValue("from");
				esrstructures.add((Element) XPath.selectSingleNode(this.statement, ".//structure[@id='" + strid + "']"));
			}
			esc = intersect(esstructures, escstructures);
			esr = intersect(esstructures, esrstructures);

			for (Element e : esc) {
				Element c = (Element) StanfordParser.path2.selectSingleNode(e);
				c.detach();
				List<Element> cl = StanfordParser.path3.selectNodes(e);
				for (Element c1 : cl)
					c1.setAttribute("value", "absent");
			}

			for (Element e : esr) {
				List<Element> rs = StanfordParser.path2.selectNodes(e);
				for (Element r : rs)
					r.detach();
				rs = XPath.selectNodes(this.statement, ".//relation[@from='" + e.getAttributeValue("id") + "'][starts-with(@name, 'present']");
				for (Element r : rs)
					r.setAttribute("negation", "true");
			}
			/* the remaining presence = no cases */
			es = StanfordParser.path2.selectNodes(this.statement);
			for (Element e : es) {
				e.setAttribute("value", "absent");
			}

			//whole_organism => whole organism
			es = StanfordParser.path6.selectNodes(this.statement);
			for (Element e : es) {
				e.setAttribute("name", "whole organism");
				e.setAttribute("name_original", "");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private List<Element> intersect(List<Element> es, List<Element> esc) {
		ArrayList<Element> common = new ArrayList<Element>();
		for (Element e : es) {
			if (esc.contains(e))
				common.add(e);
		}
		return common;
	}

	/**
	 * characters such as "orientation", "length" may be used in a character
	 * statement as <character name="character" value="orientation" /> these
	 * elements are removed by this function
	 * 
	 * @throws JDOMException
	 */
	private void removeIsolatedCharacters() throws JDOMException {
		//if (this.statement.getAttribute("state_id") == null) {
			List<Element> chars = StanfordParser.path5.selectNodes(this.statement);
			for (Element chara : chars) {
				if(chara.getAttributes().size()>2) continue; //isolated characters should only have name and value attributes.
				String v = chara.getAttributeValue("value");
				if (this.statement.getAttribute("state_id") == null){ //if a character statement, mark the [character]
					String text = this.statement.getChild("text").getTextTrim();
					text = text.replaceAll(v, "[" + v + "]");
					this.statement.getChild("text").setText(text);
				}
				List<Element> childreninorder = chara.getParentElement().getContent(new ElementFilter()); 
				Element nextchara = null;
				int i = childreninorder.indexOf(chara);
				if(i<childreninorder.size()-1)
					nextchara = childreninorder.get(++i);
				while(nextchara!=null && nextchara.getAttributeValue("name").compareTo("size")==0){ //next element is a size character, replace size with this character
					nextchara.setAttribute("name", v);
					nextchara = null;
					if(i<childreninorder.size()-1)
						nextchara = childreninorder.get(++i);;
				}
				chara.detach();
				chara = null;
			}
		//}
	}

	/**
	 * some state statements may have 1 adverb, or 20-40% as its entire
	 * statement, these will not get annotated in the annotation process
	 * annotate them here
	 */
	private void annotateBareStatements() {
		if (this.statement.getAttribute("state_id") != null) {
			if (this.statement.getChildren().size() == 1) {// holding a <text>
															// element alone
				String text = this.statement.getChildText("text");
				if (!text.matches("(" + ChunkedSentence.binaryTvalues + "|" + ChunkedSentence.binaryFvalues + ")")) {// non
																														// binary
																														// states
					Element str = new Element("structure"); // whole_organism as
															// a placeholder
					str.setAttribute("id", this.structid + "");
					this.structid++;
					str.setAttribute("name", "whole_organism");
					str.setAttribute("name_original", "");
					Element ch = new Element("character");
					ch.setAttribute("name", "unknown"); // TODO: unknown as a
														// placeholder
					ch.setAttribute("value", text.trim());
					str.addContent(ch);
					statement.addContent(str);
				}
			}
		}
	}

	// Copied from XML2EQ.java
	/**
	 * abc_iv_and_v
	 * 
	 * @param entity
	 * @return
	 */
	private String reformatRomans(String entity) {
		String[] parts = entity.split("_");
		String reformatted = "";
		for (String part : parts) {
			if (part.matches("[ivx]+"))
				reformatted += this.turnRoman2Number(part) + "_";
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
	private String turnRoman2Number(String word) {
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

	// conpied end

	private void decomposeAddToXML(Element e, String newname, String newid, String originalname) throws JDOMException {
		Element tempe = (Element) e.clone(); //all new elements would still have type="multi" attribute
		tempe.setAttribute("name", newname);
		tempe.setAttribute("name_original", originalname);
		tempe.setAttribute("id", newid);// id starts from 0
		addContent(e.getParentElement(), tempe);

		// handle relations
		String id = e.getAttributeValue("id");
		List<Element> rels = XPath.selectNodes(statement, ".//relation[@from='" + id + "'|@to='" + id + "']");
		for (Element r : rels) {
			Element tempr = (Element) r.clone();

			if (r.getAttribute("from").equals(id)) {
				tempr.setAttribute("from", newid);
			}

			if (r.getAttribute("to").equals(id)) {
				tempr.setAttribute("to", newid);
			}

			addContent(r.getParentElement(), tempr);
		}
	}

	/**
	 * 
	 * by Zilong
	 * Handle different relationships such as "connected to", "associate with"..
	 * 
	 *Original statement: 
	 * 	<statement statement_type="character_state" character_id="975551bf-c0d8-4d97-9cbc-f7c5b7b38b99" state_id="14e6ee40-450d-4d5f-9573-3c04d8dc0954" seg_id="0">
     * 		<text>loss of connection between pseudobranchial and suprabranchial arteries</text>
     *		<structure id="o234" name="whole_organism">
     *   		<character name="presence" value="loss" constraint="of connection" constraintid="o235" />
     * 		</structure>
     * 		<structure id="o235" name="connection" />
     * 		<structure id="o236" name="artery" constraint="pseudobranchial" />
     * 		<structure id="o237" name="artery" constraint="suprabranchial" />
     * 		<relation id="r72" name="between" from="o235" to="o236 o237" negation="false" />
     *	</statement>
	 * 
	 * @param reltype
	 * @throws JDOMException
	 */
	private void relationHandler(String reltype) throws JDOMException {
		List<Element> rs = XPath.newInstance(".//structure[@name='"+reltype+"']").selectNodes(statement);
		if(rs.isEmpty()){
			return;//no such relationship, return
		}
		
		String id = rs.get(0).getAttributeValue("id");
		//detach the structure "connection" 
		rs.get(0).detach();
		
		List<Element> preps = XPath.selectNodes(statement, ".//relation[@from='" + id + "']");
		for(Element prep:preps){
			String to=prep.getAttributeValue("to");
			String[] tos = to.trim().split("//s+");
			
			List<Element> characters = XPath.selectNodes(statement, ".//chracter[@name='presence']");
			//presence of the relationship
			if(characters.isEmpty()){
				return;
			}
			
			Element character=characters.get(0);
			
			String constraintStr = "";
			if(tos.length>1){
				//get all the constraint entities (from the second "to" entities of the relation element)
				for(int i=1;i<tos.length;i++){
					constraintStr+=tos[i]+" ";//concatenate the "to" entities from the second one 
				}
				
				//replace the current constraintid which refers to structure "connection" with
				//the new contraintStr.
				character.setAttribute("constraintid", constraintStr);
				
				//first "to" structure in the relation
				List<Element> first = XPath.selectNodes(statement, ".//structure[@id='"+tos[0]+"']");
				if(!first.isEmpty()){
					first.get(0).addContent(character);
				} 
			}
			
		}
				
	}
	
	/**
	 * This method is called to decompose an entity such as "manus digits i-iii"
	 * into several entities "manus digit i", "manus digit ii", and "manus digit iii"
	 * 
	 * @throws JDOMException
	 */
	private void decomposeMultipleStructures() throws JDOMException {
		List<Element> mss = XPath.newInstance(".//structure[@type='multi']").selectNodes(statement);

		Pattern p2 = Pattern.compile("(.*?)(\\d+) to (\\d+)");

		for (Element e : mss) {
			boolean isRoman=false;
			String originalname = e.getAttributeValue("name_original");
			String entity = e.getAttributeValue("name");
			//organ->singular
			String organ = TermOutputerUtilities.toSingular(entity.substring(0, entity.indexOf("_")));
			entity =entity.replaceFirst(entity.substring(0, entity.indexOf("_")), organ);

			if (entity.matches(".*?_[\\divx]+-[\\divx]+")) {// abc_1-3
				if(entity.matches(".*?_[ivx]+-[ivx]+")){
					isRoman=true;
				}
				entity = entity.replaceAll("-", "_to_");// before
														// reformatRomans,replace
														// "-" with "_to_"
			}

			entity = reformatRomans(entity);
			entity = entity.replaceAll("_(?=\\d+)", " ").replaceAll("(?<=\\d)_", " "); // abc_1_and_3
																						// =>
																						// abc
																						// 1
																						// and
																						// 3
			if (entity.indexOf(" and ") < 0 && entity.indexOf(" to ") < 0) { // single
																				// entity
				e.setAttribute("name", entity);
			} else {// abc 1 and 2
				if (entity.indexOf(" and ") > 0) {
					entity = entity.replaceFirst(" and ", "," + organ + " ");
					String[] entities = entity.split(",");
					for (int i = 0; i < entities.length; i++) {
						this.decomposeAddToXML(e, entities[i], e.getAttributeValue("id") + "-" + (i + 1), originalname);
						// id starts from 0
					}
				}

				// abc 1 , 2 to 5 ; abc 2 to 5
				Matcher m = p2.matcher(entity);
				if (m.matches()) {
					String part1 = m.group(1);
					int from = Integer.parseInt(m.group(2));
					int to = Integer.parseInt(m.group(3));
					for (int i = from; i <= to; i++) {
						String temp1 = organ + " " + (isRoman?RomanConversion.binaryToRoman(i):i);
						this.decomposeAddToXML(e, temp1, e.getAttributeValue("id") + "-" + i, originalname);// id=i
					}

					// abc 1, 2 to 5;
					part1 = part1.replaceAll("\\D", "").trim();
					if (part1.length() > 0) {
						String[] nums = part1.split("\\s+");
						for (String n : nums) {
							String temp1 = organ + " " + (isRoman? RomanConversion.binaryToRoman(Integer.parseInt(n)):n);
							this.decomposeAddToXML(e, temp1, e.getAttributeValue("id") + "-" + n, originalname);// id=i

						}
					}

					// transformed = transformed+ temp + temp1;
					// transformed.replaceFirst(",$", "").trim();
					// entityhash.put(entitylist, transformed);
					// return transformed;
				}

				// detach e and those relationships
				e.detach();
				// detach relations
				String id = e.getAttributeValue("id");
				List<Element> rels = XPath.selectNodes(statement, ".//relation[@from='" + id + "'|@to='" + id + "']");
				for (Element r : rels) {
					r.detach();
				}
			}
		}
	}

	private void removeIsolatedWholeOrganismPlaceholders() throws JDOMException {
		// remove whole_organism structures that without any character children
		// and are not involved in a relation.
		// These structures were put in as placeholders in processing
		// characters/character states descriptions
		List<Element> wholeOrgans = StanfordParser.path6.selectNodes(statement);
		Iterator<Element> it = wholeOrgans.iterator();
		while (it.hasNext()) {
			Element wo = it.next();
			String id = wo.getAttributeValue("id");
			List<Element> rels = XPath.selectNodes(statement, ".//relation[@from='" + id + "'|@to='" + id + "']");
			if (wo.getChildren().size() == 0 && rels.size() == 0) {
				wo.detach();
			}
		}
	}

	private Element addContent(Element element, Element ch) {
		element.addContent(ch);
		this.elementlog.add(ch);
		return element;
	}

	/**
	 * assuming subject organs of subsentences in a sentence are parts of the
	 * subject organ of the sentence this assumption seemed hold for FNA data.
	 */
	private void puncBasedPartOfRelation() {
		for (int p = 0; p < this.pstructures.size(); p++) {
			for (int c = 0; c < this.cstructures.size(); c++) {
				String pid = this.pstructures.get(p).getAttributeValue("id");
				String cid = this.cstructures.get(c).getAttributeValue("id");
				this.addRelation("part_of", "", false, cid, pid, false, "based_on_punctuation");
			}
		}
	}

	/**
	 * re-annotate "trees" from structure to character lifestyle
	 */
	private void lifeStyle() {
		try {
			// find life_style structures
			List<Element> structures = StanfordParser.path7.selectNodes(this.statement);
			Iterator<Element> it = structures.iterator();
			// Element structure = null;
			while (it.hasNext()) {
				Element structure = it.next();
				String name = structure.getAttributeValue("name").trim();
				if (name.length() <= 0)
					continue;
				if (lifestyle.matches(".*\\b" + name + "\\b.*")) {
					if (structure.getAttribute("constraint_type") != null)
						name = structure.getAttributeValue("constraint_type") + " " + name;
					if (structure.getAttribute("constraint_parent_organ") != null)
						name = structure.getAttributeValue("constraint_parent_organ") + " " + name;
					if (structure.getAttribute("constraint") != null)
						name = structure.getAttributeValue("constraint") + " " + name;
					Element wo = (Element) StanfordParser.path6.selectSingleNode(this.statement);
					if (wo != null) {
						List<Element> content = structure.getContent();
						structure.removeContent();
						/*
						 * for(int i = 0; i<content.size(); i++){ Element e =
						 * content.get(i); e.detach(); content.set(i, e); }
						 */
						wo.addContent(content);
						structure.detach();
						structure = wo;
					}
					structure.setAttribute("name", "whole_organism");
					Element ch = new Element("character");
					ch.setAttribute("name", "life_style");
					ch.setAttribute("value", name);
					structure = addContent(structure, ch);
					// structure.addContent(ch);
				}
				// keep each life_style structure
				/*
				 * if(lifestyle.matches(".*\\b"+name+"\\b.*")){
				 * if(structure.getAttribute("constraint") !=null) name =
				 * structure.getAttributeValue("constraint")+" "+name;
				 * structure.setAttribute("name", "whole_organism"); Element ch
				 * = new Element("character"); ch.setAttribute("name",
				 * "life_style"); ch.setAttribute("value", name);
				 * structure.addContent(ch); }
				 */
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * if there are structure with the same name and constraint but different
	 * ids add a relation 'may_be_the_same' among them, set symmetric="true"
	 */

	private void mayBeSameRelation() {
		try {
			List<Element> structures = StanfordParser.path7.selectNodes(this.statement);
			Hashtable<String, ArrayList<String>> names = new Hashtable<String, ArrayList<String>>();
			Iterator<Element> it = structures.iterator();
			// structure => ids hash
			while (it.hasNext()) {
				Element structure = it.next();
				String name = structure.getAttributeValue("name");
				// one the two contraint types
				if (structure.getAttribute("constraint_type") != null)
					name = structure.getAttributeValue("constraint_type") + " " + name;
				if (structure.getAttribute("constraint_parent_organ") != null)
					name = structure.getAttributeValue("constraint_parent_organ") + " " + name;
				if (structure.getAttribute("constraint") != null)
					name = structure.getAttributeValue("constraint") + " " + name;
				String id = structure.getAttributeValue("id");
				if (names.containsKey(name)) {
					names.get(name).add(id);// update the value for name
					// names.put(name, names.get(name));
				} else {
					ArrayList<String> ids = new ArrayList<String>();
					ids.add(id);
					names.put(name, ids);
				}
			}
			// use the hash to create relations
			Enumeration<String> en = names.keys();
			while (en.hasMoreElements()) {
				String name = en.nextElement();
				ArrayList<String> ids = names.get(name);
				if (ids.size() > 1) {
					for (int i = 0; i < ids.size(); i++) {
						for (int j = i + 1; j < ids.size(); j++) {
							this.addRelation("may_be_the_same", "", true, ids.get(i), ids.get(j), false, "based_on_text");
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void annotateByChunk(ChunkedSentence cs, boolean inbrackets) {
		if (cs == null)
			return;
		this.inbrackets = inbrackets;

		while (cs.hasNext()) {
			Chunk ck = cs.nextChunk();
			if (ck instanceof ChunkOR) {
				int afterorindex = cs.getPointer();
				Element last = this.latestelements.get(this.latestelements.size() - 1);
				ck = cs.nextChunk();
				if (ck != null && last.getName().compareTo("character") == 0) {
					String cname = last.getAttributeValue("name");
					//or greater, less, fewer, more, etc
					String content = ck.toString();
					if(content.indexOf(" ")<0 && content.matches(".*?\\b(\\w+er|more|less)\\b.*")){
						//add content to the value of last character
						String newvalue = last.getAttributeValue("value")+" or "+content.replaceAll("(\\w+\\[|\\])", "");
						last.setAttribute("value", newvalue);
						continue;
					}
					if (!(ck instanceof ChunkSimpleCharacterState) && !(ck instanceof ChunkNumericals)) {
						// these cases can be handled by the normal annotation procedure
						Element e = new Element("character");
						if (this.inbrackets) {
							e.setAttribute("in_bracket", "true");
						}
						e.setAttribute("name", cname);
						String v = ck.toString(); // may be a character list
						if (v.length() >= 1) {// chunk contains text
							if (v.indexOf("~list~") >= 0) {
								v = v.replaceFirst("\\w{2,}\\[.*?~list~", "").replaceAll("punct", ",").replaceAll("~", " ");
							}
							v = v.replaceAll("(\\w\\[|\\]|\\{|\\}|\\(|\\)|<|>)", "");
							e.setAttribute("value", v);
							addClauseModifierConstraint(cs, e);
							// last.getParentElement().addContent(e);
							addContent(last.getParentElement(), e);
						} else {// chunk not contain text: or nearly so, or not,
								// or throughout
							e = traceBack4(e, last, afterorindex, cs.getPointer());
							// last.getParentElement().addContent(e);
							addContent(last.getParentElement(), e);
						}
					}
					ArrayList<Element> e = new ArrayList<Element>();
					e.add(new Element("or"));
					updateLatestElements(e);
				}else if (ck != null && last.getName().compareTo("structure") == 0) { //scattered <patches> or absent
					if(this.printOR) System.out.println("created whole_organism for OR");
					ArrayList<Element> structure = this.createStructureElements("(whole_organism)", false);
					updateLatestElements(structure);
				}
				/*ArrayList<Element> e = new ArrayList<Element>();
				e.add(new Element("or"));
				updateLatestElements(e);*/
			}
			if (ck instanceof ChunkCharacterComparison){//{relative~{A~char}~{relation}~{B~char}}
				ArrayList<Element> structures = processChunkCharacterComparison(ck.toString());
				updateLatestElements(structures); //may still be modifiers etc following this chunk
			}
			if (ck instanceof ChunkOrgan) {// this is the subject of a segment.
											// May contain multiple organs
				//if this chunk appears right after a comma
				boolean aftercomma = false;
				if(this.latestelements.size()>0 && this.latestelements.get(this.latestelements.size()-1).getName().compareTo("comma")==0){
					aftercomma = true;
				}
				String content = ck.toString().replaceFirst("^z\\[", "").replaceFirst("\\]$", "");
				establishSubject(content, aftercomma/* , false */);
				if (this.partofinference) {
					this.cstructures.addAll(this.subjects);
				}
				cs.setInSegment(true);
				cs.setRightAfterSubject(true);
			} else if (ck instanceof ChunkNonSubjectOrgan) {
				String content = ck.toString().replaceFirst("^u\\[", "").replaceFirst("\\]$", "");
				String structure = "";
				if (content.indexOf("o[") >= 0) {
					String m = content.substring(0, content.indexOf("o[")).replaceAll("m\\[", "{").replaceAll("\\]", "}");
					String o = content.substring(content.indexOf("o[")).replaceAll("o\\[", "").replaceAll("\\]", "");
					structure = m + o;
				} else {
					structure = content;
				}
				//if this chunk appears right after a comma
				boolean aftercomma = false;
				if(this.latestelements.size()>0 && this.latestelements.get(this.latestelements.size()-1).getName().compareTo("comma")==0){
					aftercomma = true;
				}
				ArrayList<Element> structures = createStructureElements(structure, aftercomma/*
																				 * ,
																				 * false
																				 */);
				updateLatestElements(structures);
			} else if (ck instanceof ChunkPrep) {
				/*
				 * String content = ck.toString();
				 * if(content.matches(".*?\\bwith\\b.*")){ this.attachToLast =
				 * true; } if(content.indexOf("c[")>=0){ content =
				 * content.replaceFirst(".*?\\bc\\[", "").replaceAll("\\]", "");
				 * this.annotateNumericals(content, "count", "",
				 * lastStructures()); }else{
				 */
				processPrep((ChunkPrep) ck);
				// }
			} else if (ck instanceof ChunkCHPP) {// t[c/r[p/o]] this chunk is
													// converted internally and
													// not shown in the parsing
													// output
				String content = ck.toString().replaceFirst("^t\\[", "").replaceFirst("\\]$", "");
				processCHPP(content);
			} else if (ck instanceof ChunkNPList) {// NPList as a seperate chunk
				String content = ck.toString().replaceFirst("^l\\[", "").replaceFirst("\\]$", "");
				if (!content.endsWith(")")) {// format it
					content = content.replaceAll(" +(?=(,|and\\b|or\\b))", ") ") + ")";
					content = content.replaceAll(" +(?=\\w+\\))", " (");
					if(!content.startsWith("(")) content="("+content;//added by Hari
				}
				//if this chunk appears right after a comma
				boolean aftercomma = false;
				if(this.latestelements.size()>0 && this.latestelements.get(this.latestelements.size()-1).getName().compareTo("comma")==0){
					aftercomma = true;
				}
				establishSubject(content, aftercomma/* , false */);
			} else if (ck instanceof ChunkSimpleCharacterState) {
				String content = ck.toString().replaceFirst("^a\\[", "").replaceFirst("\\]$", "");
				// ArrayList<Element> chars =
				// processSimpleCharacterState(content, lastStructures());//with
				// teeth closely spaced
				// ArrayList<Element> parents = this.attachToLast?
				// lastStructures() : subjects;
				ArrayList<Element> chars = processSimpleCharacterState(content, lastStructures());// apices
																									// of
																									// basal
																									// leaves
																									// spread
				// if(printAttach &&
				// subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name"))
				// != 0){
				// System.out.println(content +
				// " attached to "+parents.get(0).getAttributeValue("name"));
				// }
				updateLatestElements(chars);
			} else if (ck instanceof ChunkSL) {// coloration[coloration-list-red-to-black]
				// ArrayList<Element> parents = this.attachToLast?
				// lastStructures() : subjects;
				// if(printAttach &&
				// subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name"))
				// != 0){
				// System.out.println(ck.toString() +
				// " attached to "+parents.get(0).getAttributeValue("name"));
				// }
				ArrayList<Element> chars = processCharacterList(ck.toString(), lastStructures(), false/*
																								 * this
																								 * .
																								 * subjects
																								 */);
				updateLatestElements(chars);
			} else if (ck instanceof ChunkComma) {
				this.latestelements.add(new Element("comma"));
			} else if (ck instanceof ChunkVP) {
				// ArrayList<Element> parents = this.attachToLast?
				// lastStructures() : subjects;
				/*
				 * if(printAttach &&
				 * subjects.get(0).getAttributeValue("name").compareTo
				 * (lastStructures().get(0).getAttributeValue("name")) != 0){
				 * System.out.println(ck.toString() +
				 * " attached to "+parents.get(0).getAttributeValue("name")); }
				 */
				ArrayList<Element> es = processTVerb(ck.toString().replaceFirst("^b\\[", "").replaceFirst("\\]$", ""), subjects);
				// ArrayList<Element> es =
				// processTVerb(ck.toString().replaceFirst("^b\\[",
				// "").replaceFirst("\\]$", ""),
				// CharacterAnnotatorChunked.subjects);
				updateLatestElements(es);
			} else if (ck instanceof ChunkComparativeValue) {//m[at-least] 2 times n[length[{longer}] constraint[than {wide}]]
				// ArrayList<Element> chars =
				// processComparativeValue(ck.toString().replaceAll("–", "-"),
				// lastStructures());
				String content = ck.toString();
				// ArrayList<Element> parents = this.attachToLast?
				// lastStructures() : subjects;
				// if(printAttach &&
				// subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name"))
				// != 0){
				// System.out.println(content +
				// " attached to "+parents.get(0).getAttributeValue("name"));
				// }
				ArrayList<Element> chars = processComparativeValue(content.replaceAll("–", "-"), lastStructures());
				updateLatestElements(chars);
			} else if (ck instanceof ChunkRatio) {
				// ArrayList<Element> chars = annotateNumericals(ck.toString(),
				// "lwratio", "", lastStructures());
				String content = ck.toString();
				// ArrayList<Element> parents = this.attachToLast?
				// lastStructures() : subjects;
				// if(printAttach &&
				// subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name"))
				// != 0){
				// System.out.println(content +
				// " attached to "+parents.get(0).getAttributeValue("name"));
				// }
				ArrayList<Element> chars = annotateNumericals(content, ((ChunkRatio) ck).getLabel(), "", lastStructures(), false, false);
				updateLatestElements(chars);
			} else if (ck instanceof ChunkArea) {
				// ArrayList<Element> chars = annotateNumericals(ck.toString(),
				// "area", "", lastStructures());
				String content = ck.toString();
				// ArrayList<Element> parents = this.attachToLast?
				// lastStructures() : subjects;
				// if(printAttach &&
				// subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name"))
				// != 0){
				// System.out.println(content + " attached to "+
				// parents.get(0).getAttributeValue("name"));
				// }
				ArrayList<Element> chars = annotateNumericals(content, "area", "", lastStructures(), false, false);
				updateLatestElements(chars);
			} else if (ck instanceof ChunkNumericals) {
				// ** find parents, modifiers
				// TODO: check the use of [ and ( in extreme values
				// ArrayList<Element> parents = lastStructures();
				String text = ck.toString().replaceAll("–", "-");
				boolean resetfrom = false;
				if (text.matches(".*\\bto \\d.*")) { // m[mostly] to 6 m ==>
														// m[mostly] 0-6 m
					text = text.replaceFirst("to\\s+", "0-");
					resetfrom = true;
				}
				// ArrayList<Element> parents = this.attachToLast?
				// lastStructures() : subjects;
				// if(printAttach &&
				// subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name"))
				// != 0){
				// System.out.println(text +
				// " attached to "+parents.get(0).getAttributeValue("name"));
				// }
				if (debugNum) {
					System.out.println();
					System.out.println(">>>>>>>>>>>>>" + text);
				}
				String modifier1 = "";// m[mostly] [4-]8–12[-19] mm m[distally];
										// m[usually] 1.5-2 times
										// n[size[{longer} than
										// {wide}]]:consider a constraint
				String modifier2 = "";
				modifier1 = text.replaceFirst("\\[?\\d.*$", "");
				String rest = text.replace(modifier1, "");
				modifier1 = modifier1.replaceAll("(\\w\\[|\\]|\\{|\\})", "").trim();
				modifier2 = rest.replaceFirst(".*?(\\d|\\[|\\+|\\-|\\]|%|\\s|" + ChunkedSentence.units + ")+\\s?(?=[a-z]|$)", "");// 4-5[+]
				String content = rest.replace(modifier2, "").replaceAll("(\\{|\\})", "").trim();
				modifier2 = modifier2.replaceAll("(\\w+\\[|\\]|\\{|\\})", "").trim();
				ArrayList<Element> chars = annotateNumericals(content, text.matches(".*?\\b("+this.size+")\\b.*") || content.indexOf('/') > 0 || content.indexOf('%') > 0
						|| content.indexOf('.') > 0 ? "size" : null, (modifier1 + ";" + modifier2).replaceAll("(^\\W|\\W$)", ""), lastStructures(), resetfrom, false);
				updateLatestElements(chars);
			} else if (ck instanceof ChunkTHAN) {
				ArrayList<Element> chars = processTHAN(ck.toString().replaceFirst("^n\\[", "").replaceFirst("\\]$", ""), this.subjects);
				updateLatestElements(chars);
			} else if (ck instanceof ChunkTHANC) {// n[(longer) than {wide}] .
				ArrayList<Element> chars = processTHAN(ck.toString().replaceFirst("^n\\[", "").replaceFirst("\\]$", ""), this.subjects);
				updateLatestElements(chars);
			} else if (ck instanceof ChunkBracketed) {
				annotateByChunk(new ChunkedSentence(ck.getChunkedTokens(), ck.toString(), conn, glosstable, this.tableprefix), true); // no
																																		// need																															// to
																																		// updateLatestElements
				this.inbrackets = false;
			} else if (ck instanceof ChunkSBAR) {
				ArrayList<Element> subjectscopy = this.subjects;
				if (this.latestelements.get(this.latestelements.size() - 1).getName().compareTo("structure") == 0) {
					this.subjects = latest("structure", this.latestelements);
				} else {
					int p = cs.getPointer() - 2;
					String last = ""; // the chunk before ck??
					do {
						last = cs.getTokenAt(p--);
					} while (last.matches(",") || last.matches(""));
					String constraintId = null;
					if (last.matches(".*?\\)\\]+")) {
						constraintId = "o" + (this.structid - 1);
						try {
							Element laststruct = (Element) XPath.selectSingleNode(this.statement, ".//structure[@id='" + constraintId + "']");
							ArrayList<Element> temp = new ArrayList<Element>();
							temp.add(laststruct);
							this.subjects = temp;
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						// do nothing
						System.err.println("no structure element found for the SBARChunk, use subjects instead ");
						// this only works for situations where states before
						// subjects got reintroduced after subjects in skiplead
						// this will not work for misidentified nouns before
						// "that/which" statements, in "of/among which", and
						// other cases
					}
				}
				ArrayList<String> chunkedTokens = ck.getChunkedTokens();
				String connector = ck.toString().substring(0, ck.toString().indexOf(" "));
				String text = ck.toString();
				while (!connector.matches("that|when|where|which")) {
					text = text.replace(connector, "").trim();
					for (int i = 0; i < chunkedTokens.size(); i++) {
						if (chunkedTokens.get(i).equals(connector)) {
							chunkedTokens.set(i, "");
							break;
						}
					}
					connector = text.substring(0, text.indexOf(" "));
				}
				String content = text.substring(text.indexOf(" ") + 1);
				ChunkedSentence newcs = new ChunkedSentence(chunkedTokens, content, conn, glosstable, this.tableprefix);
				Chunk firstck = newcs.getNextChunk();
				if (firstck instanceof ChunkNonSubjectOrgan || firstck instanceof ChunkOrgan) {
					//if this chunk appears right after a comma
					boolean aftercomma = false;
					if(this.latestelements.size()>0 && this.latestelements.get(this.latestelements.size()-1).getName().compareTo("comma")==0){
						aftercomma = true;
					}
					establishSubject(firstck.toString().replaceAll("\\w\\[", "").replaceAll("\\]", "").trim(), aftercomma);
				}

				if (connector.compareTo("when") == 0) {// rewrite content and
														// its chunkedTokens
					Pattern p = Pattern.compile("[\\.,:;]");
					Matcher m = p.matcher(ck.toString());
					int end = 0;
					if (m.find()) {
						end = m.start();
					}
					// int end = ck.toString().indexOf(",") > 0?
					// ck.toString().indexOf(",") : ck.toString().indexOf(".");
					String modifier = ck.toString().substring(0, end).trim();// when
																				// mature,
					content = ck.toString().substring(end).replaceAll("^\\W+", "").trim();
					if (content.length() > 0) {
						ck.setChunkedTokens(Utilities.breakText(content));
						newcs = new ChunkedSentence(ck.getChunkedTokens(), content, conn, glosstable, this.tableprefix);
					} else {
						newcs = null;
					}
					// attach modifier to the last characters
					if (this.latestelements.get(this.latestelements.size() - 1).getName().compareTo("character") == 0) {
						Iterator<Element> it = this.latestelements.iterator();
						while (it.hasNext()) {
							this.addAttribute(it.next(), "modifier", modifier);
						}
					} else {
						if (newcs != null)
							newcs.unassignedmodifier = "m[" + modifier + "]";// this
																				// when
																				// clause
																				// is
																				// a
																				// modifier
																				// for
																				// the
																				// subclause
						else {
							if (this.latestelements.get(this.latestelements.size() - 1).getName().compareTo("comma") == 0) {
								this.latestelements.remove(this.latestelements.size() - 1); // remove
																							// comma,
																							// so
																							// what
																							// follows
																							// when-clause
																							// may
																							// refer
																							// to
																							// the
																							// structure
																							// mentioned
																							// before
																							// as
																							// in
																							// <apex>
																							// r[p[of]
																							// o[(scape)]]
																							// ,
																							// s[when
																							// laid
																							// {straight}
																							// {back}
																							// r[p[from]
																							// o[its
																							// (insertion)]]
																							// ,]
																							// just
																							// touches
																							// the
																							// {midpoint}
																							// r[p[of]
																							// o[the
																							// {posterior}
																							// (margin)]]
																							// r[p[in]
																							// o[(fullface)]]
																							// {view}
																							// ;
							}
							cs.unassignedmodifier = "m[" + modifier.replaceAll("(\\w+\\[|\\]|\\(|\\)|\\{|\\})", "") + "]";
						}
					}
				}

				if (connector.compareTo("where") == 0) {
					// retrieve the last non-comma, non-empty chunk
					int p = cs.getPointer() - 2;
					String last = "";
					do {
						last = cs.getTokenAt(p--);
					} while (!last.matches(".*?\\w.*"));
					String constraintId = null;
					if (last.matches(".*?\\)\\]+"))
						constraintId = "o" + (this.structid - 1);
					cs.setClauseModifierConstraint(last.replaceAll("(\\w+\\[|\\]|\\{|\\}|\\)|\\()", ""), constraintId);
				}
				if (newcs != null)
					newcs.setInSegment(true);
				annotateByChunk(newcs, false); // no need to
												// updateLatestElements
				this.subjects = subjectscopy;// return to original status
				cs.setClauseModifierConstraint(null, null); // return to
															// original status
				// this.unassignedmodifiers = null;

			} else if (ck instanceof ChunkChrom) {
				String content = ck.toString().replaceAll("[^\\d()\\[\\],+ -]", "").trim();
				// Element structure = new Element("chromosome");
				Element structure = new Element("structure");
				this.addAttribute(structure, "name", "chromosome");
				this.addAttribute(structure, "name_original", ""); //what value should it be?
				this.addAttribute(structure, "id", "o" + this.structid);
				this.structid++;
				ArrayList<Element> list = new ArrayList<Element>();
				list.add(structure);
				this.annotateNumericals(content, "count", "", list, false, false);
				/*
				 * for(int i = 0; i<counts.length; i++){ Element character = new
				 * Element("character"); this.addAttribute(character, "count",
				 * counts[i]); structure.addContent(character); }
				 */
				addClauseModifierConstraint(cs, structure);
				// this.statement.addContent(structure);
				addContent(this.statement, structure);
			} else if (ck instanceof ChunkValuePercentage || ck instanceof ChunkValueDegree) {
				String content = ck.toString();
				Element lastelement = this.latestelements.get(this.latestelements.size() - 1);
				if (lastelement != null && lastelement.getName().compareTo("character") == 0) {
					this.addAttribute(lastelement, "modifier", content);
				} else {
					content = content.replaceAll("(m\\[|\\])", "").replaceAll("(?<=[^\\d])-(?=[^\\d])", " ");
					ArrayList<Element> chars = annotateNumericals(content, "size", "", lastStructures(), false, false);
					updateLatestElements(chars);
					//cs.unassignedmodifier = content;
				}

			} else if (ck instanceof ChunkEOS || ck instanceof ChunkEOL) {
				if (cs.unassignedmodifier != null && cs.unassignedmodifier.length() > 0 && this.latestelements.size()>=1) {
					Element latestelement = this.latestelements.get(this.latestelements.size() - 1);
					// if(latestelement == null){
					// latestelement =
					// this.createStructureElements("(placeholder)").get(0);
					// }
					if (latestelement.getName().compareTo("structure") == 0) {
						Iterator<Element> it = this.latestelements.iterator();
						while (it.hasNext()) {
							String sid = it.next().getAttributeValue("id");
							try {
								List<Element> relations = XPath.selectNodes(this.statement, ".//relation[@to='" + sid + "']");
								Iterator<Element> rit = relations.iterator();
								int greatestid = 0;
								Element relation = null;
								while (rit.hasNext()) {
									Element r = rit.next();
									int rid = Integer.parseInt(r.getAttributeValue("id").replaceFirst("r", ""));
									if (rid > greatestid) {
										greatestid = rid;
										relation = r;
									}
								}
								if (relation != null)
									this.addAttribute(relation, "modifier", cs.unassignedmodifier);
								// TODO: otherwise, categorize modifier and
								// create a character for the structure
								// e.g.{thin} {dorsal} {median} <septum>
								// {centrally} only ;
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					} else if (latestelement.getName().compareTo("character") == 0) {
						Iterator<Element> it = this.latestelements.iterator();
						while (it.hasNext()) {
							this.addAttribute(it.next(), "modifier", cs.unassignedmodifier);
						}
					}
				}
				this.attachToLast = false;
				cs.unassignedmodifier = null;
				this.unassignedcharacter = null;
			}
		}

	}

	/**
	 * {relative~{A~charA}~{relation}~{B~charB'}}
	 * 
	 * <structure id='1' name='A'/>
	 * <structure id='2' name='B'/>
	 * <relation name='charA relation charB'' from='1' to='2'>
	 * @param string
	 * @return
	 */
	private ArrayList<Element> processChunkCharacterComparison(String content) {
		ArrayList<Element>result = new ArrayList<Element>();
		String [] parts = content.replaceFirst("^\\{relative~", "").split("\\{"); //three parts
		String[] part1 = parts[1].replaceAll("[{}]", "").split("~"); //parts[0] = ""
		String organA = part1[0];
		String charA = part1[1];
		String[] part2 = parts[3].replaceAll("[{}]", "").split("~");
		String organB="", charB="";
		//part2 may have one or two elements
		if(part2.length==1){
			if(part2[0].matches("\\b("+this.characters+")\\b")){
				charB = part2[0];
				organB = organA;
			}else{
				organB = part2[0];
				charB = charA;
			}
		}else{
			organB = part2[0];
			charB = part2[1];
		}
		
		String relation = charA+" "+parts[2].replaceAll("[{}~]", "")+" "+charB;
		ArrayList<Element> structureA = this.createStructureElements("("+organA+")", false);
		ArrayList<Element> structureB = null;
		if(organB.compareTo(organA)!=0){
			structureB = this.createStructureElements(organB, false);
		}else{
			structureB = structureA;
		}
		this.createRelationElements("#quality comparison# "+relation, structureA, structureB, "", false);
		return result; //empty as don't want to expose the elements 
	}

	private void addClauseModifierConstraint(ChunkedSentence cs, Element e) {
		ArrayList<String> cm = cs.getClauseModifierConstraint();
		if (cm != null) {
			if (cm.size() > 1) {// is a constraint
				this.addAttribute(e, "constraint", cm.get(0));
				this.addAttribute(e, "constraintid", cm.get(1));
			} else {
				this.addAttribute(e, "modifier", cm.get(0));
			}
		}
	}

	/**
	 * track back in this.chunkedTokens to populate the afteror element afteror
	 * shares the same character name and value with beforeor, but have
	 * different modifier--which is found from the missing text branched
	 * distally or throughout constricted distally or not subequal or weakly to
	 * strongly well distributed or not dioecious or nearly so spinulose or not
	 * openly branched distally or throughout branched proximally or distally
	 * usually 1 cm or less
	 * 
	 * @param afteror
	 * @param beforor
	 */
	private Element traceBack4(Element afteror, Element beforeor, int afterorindex, int endindex) {
		String text = cs.getText(afterorindex, endindex); // from afterorindex
															// (include) to
															// endindex (not
															// include)
		text = text.replaceAll("SG", "").replaceAll("\\W+", " ").replaceAll("\\s+", " ").trim();
		text = text.replaceFirst("\\s+so$", "");
		afteror = (Element) beforeor.clone();
		this.addAttribute(afteror, "modifier", text);
		return afteror;
	}

	private ArrayList<Element> annotateNumericals(String chunktext, String character, String modifier, ArrayList<Element> parents, boolean resetfrom, boolean characterismodifier) {
		ArrayList<Element> chars = null;
		// if(character!=null && character.compareTo("size")==0 &&
		// chunktext.contains("times")){
		if (character == null){
			character = "count"; // convenient for phenoscape parsing as it
									// doesn't care numerical values
		}
		chunktext = chunktext.replaceAll("("+ChunkedSentence.percentage+")\\b", " %");
		chars = parseNumericals(chunktext, character); // annotate "2 times"
														// without changing
														// NumericalHandler.parseNumericals
		// }else{
		// chars = NumericalHandler.parseNumericals(chunktext, character);
		// //full numerical parsing for FNA-like data
		// }
		if (chars.size() == 0) {// failed, simplify chunktext
			chunktext = chunktext.replaceAll("[()\\]\\[]", "");
			if (character != null && character.compareTo("size") == 0 && chunktext.contains("times")) {
				chars = parseNumericals(chunktext, character);
			} else {
				chars = NumericalHandler.parseNumericals(chunktext, character);
			}
		}
		Iterator<Element> it = chars.iterator();
		ArrayList<Element> results = new ArrayList<Element>();
		while (it.hasNext()) {
			Element e = it.next();
			if (resetfrom && e.getAttribute("from") != null && e.getAttributeValue("from").equals("0")
					&& (e.getAttribute("from_inclusive") == null || e.getAttributeValue("from_inclusive").equals("true"))) {// to
																															// 6[-9]
																															// m.
				e.removeAttribute("from");
				if (e.getAttribute("from_unit") != null) {
					e.removeAttribute("from_unit");
				}
			}
			if (modifier != null && modifier.compareTo("") != 0) {
				this.addAttribute(e, "modifier", modifier);
			}
			if (this.inbrackets) {
				e.setAttribute("in_bracket", "true");
			}
			if(characterismodifier){
				e.setAttribute("is_modifier", "true");
				if(debugextraattributes) System.out.println("is modifier:"+e.getAttributeValue("value"));
			}
			/*
			 * if(this.unassignedmodifiers != null &&
			 * this.unassignedmodifiers.compareTo("") !=0){ this.addAttribute(e,
			 * "modifier", this.unassignedmodifiers); this.unassignedmodifiers =
			 * ""; }
			 */
			Iterator<Element> pit = parents.iterator();
			while (pit.hasNext()) {
				Element ec = (Element) e.clone();
				ec.detach();
				Element p = pit.next();
				// p.addContent(ec);
				addContent(p, ec);
				results.add(ec);
			}
		}
		return results;
	}

	/**
	 * this is a vastly simplified version of NumericalHandler.parseNumericals()
	 * which was developed for FNA numerical experssions
	 * 
	 * @param chunktext
	 * @param character
	 * @return
	 */
	private ArrayList<Element> parseNumericals(String chunktext, String character) {
		ArrayList<Element> chars = new ArrayList<Element>();
		Element chara = new Element("character");
		chara.setAttribute("name", character);// "size"
		chara.setAttribute("value", chunktext.trim());// "2 times" [the length
														// of organ a] as
														// constraint annotated
														// elsewhere.
		chars.add(chara);
		return chars;
	}

	private ArrayList<Element> lastStructures() {
		ArrayList<Element> parents;
		if (this.latestelements.size() > 0 && this.latestelements.get(this.latestelements.size() - 1).getName().compareTo("structure") == 0) {
			parents = this.latestelements;
		}else {
			parents = this.subjects;
		}
		return parents;
	}

	/**
	 * 3 times n[...than...] lengths 0.5–0.6+ times <bodies> ca .3.5 times
	 * length of <throat> 1–3 times {pinnately} {lobed} 1–2 times
	 * shape[{shape~list~pinnately~lobed~or~divided}] 4 times longer than wide
	 * 
	 * 
	 * 
	 * @param content
	 *            : 0.5–0.6+ times a[type[bodies]]
	 * @param subjects2
	 * @return
	 */
	private ArrayList<Element> processComparativeValue(String content, ArrayList<Element> parents) {
		if (content.startsWith("n[")) {
			content = content.replaceFirst("^n\\[", "").replaceFirst("\\]", "").trim();
		}
		String v = content.replaceAll("(" + ChunkedSentence.times + ").*$", "").trim(); // v
																						// holds
																						// numbers
		String n = content.replace(v, "").trim();
		if (n.indexOf("constraint") >= 0) {
			n = n.replaceFirst("constraint\\[", "").replaceFirst("\\]$", ""); // n
																				// holds
																				// "times....
		}
		if (n.indexOf("n[") >= 0) {// 1.5–2.5 times n[{longer} than (throat)]
			// content = "n["+content.replace("n[", "");
			// added, not tested
			if (n.matches("\\b(" + ChunkedSentence.times + ")\\b.*")) {
				n = n.replaceFirst("\\b(" + ChunkedSentence.times + ")\\b", "").trim();
				v = v + " times";
			}
			content = v.replaceFirst("(^| )(?=\\d)", " size[") + "] constraint[" + n.replaceFirst("n\\[", "").trim(); // m[usually]
																														// 1.5-2
			return this.processTHAN(content, parents);
		} else if (n.indexOf("type[") == 0 || n.indexOf(" type[") > 0) {// size[{longer}]
																		// constraint[than
																		// (object}]
			// this.processSimpleCharacterState("a[size["+v.replace(" times",
			// "")+"]]", parents);
			// ArrayList<Element> structures = this.processObject(n);
			// this.createRelationElements("times", parents, structures,
			// this.unassignedmodifiers);
			// this.unassignedmodifiers = null;
			// return structures;
			// added, not tested
			if (n.matches("\\b(" + ChunkedSentence.times + ")\\b.*")) {
				n = n.replaceFirst("\\b(" + ChunkedSentence.times + ")\\b", "").trim();
				v = v + " times";
			}
			n = "constraint[" + n.replaceFirst("type\\[", "(").replaceFirst("\\]", ")").replaceAll("a\\[", ""); // 1-1.6
																												// times
																												// u[o[bodies]]
																												// =>
																												// constraint[(bodies)]
			content = "size[" + v + "] " + n;
			return this.processTHAN(content, parents);
		} else if (n.indexOf("o[") >= 0 || n.indexOf("z[") >= 0) {// ca .3.5
																	// times
																	// length
																	// r[p[of]
																	// o[(throat)]]
			if (n.matches("\\b(" + ChunkedSentence.times + ")\\b.*")) {
				n = n.replaceFirst("\\b(" + ChunkedSentence.times + ")\\b", "").trim();
				v = v + " times";
			}
			n = "constraint[" + n.replaceAll("[o|z]\\[", ""); // times
																// o[(bodies)]
																// =>
																// constraint[times
																// (bodies)]
			content = "size[" + v + "] " + n;
			return this.processTHAN(content, parents);
		} else if (n.indexOf("a[") == 0 || n.indexOf(" a[") > 0) { // characters:1–3
																	// times
																	// {pinnately}
																	// {lobed}
			String times = n.substring(0, n.indexOf(' '));
			n = n.substring(n.indexOf(' ') + 1);
			n = n.replaceFirst("a\\[", "").replaceFirst("\\]$", "");
			n = "m[" + v + " " + times + "] " + n;
			return this.processSimpleCharacterState(n, parents);
		} else if (content.indexOf("[") < 0) { // {forked} {moreorless} unevenly
												// ca . 3-4 times ,
			// content = 3-4 times; v = 3-4; n=times
			// marked as a constraint to the last character "forked". "ca."
			// should be removed from sentences in SentenceOrganStateMarker.java
			Element lastelement = this.latestelements.get(this.latestelements.size() - 1);
			if (lastelement.getName().compareTo("character") == 0) {
				Iterator<Element> it = this.latestelements.iterator();
				while (it.hasNext()) {
					lastelement = it.next();
					if (cs.unassignedmodifier != null && cs.unassignedmodifier.trim().length() != 0) {
						lastelement.setAttribute("modifier", cs.unassignedmodifier);
						cs.unassignedmodifier = null;
					}
					lastelement.setAttribute("constraint", content);
				}
			} else if (lastelement.getName().compareTo("structure") == 0) {
				return null; // parsing failure
			}
			return this.latestelements;

		}
		return null;
	}

	/**
	 * size[{longer}] constraint[than (object)]"; shape[{lobed} constraint[than
	 * (proximal)]]
	 * 
	 * m[at-least] 2 times n[length[{longer}] constraint[than {wide}]
	 * size[less than 2 times {longer} constraint[than {wide}]]
	 * 
	 * @param replaceFirst
	 * @param subjects2
	 * @return
	 */
	private ArrayList<Element> processTHAN(String content, ArrayList<Element> parents) {

		ArrayList<Element> charas = new ArrayList<Element>();
		String modifier = "";
		while(content.startsWith("m[")){
			modifier += content.substring(0, content.indexOf("]")+1);
			content = content.substring(content.indexOf("]")+1).trim();
		}
		String[] parts = content.split("constraint\\[");
		if (content.startsWith("constraint")) {
			charas = latest("character", this.latestelements);
		} else {
			String ch = "";
			if(parts[0].contains(" n[") && parts.length>1 && parts[1].matches(".*?\\b("+ChunkedSentence.asasthan+")\\b.*")){//both parts contains a dimension
				String t = parts[0].substring(parts[0].indexOf(" n[")+3);
				ch = t.substring(0, t.indexOf("["));
			}
			if (parts[0].matches(".*?\\d.*") && parts[0].matches(".*(size|orientation)\\[.*")) {// size[m[mostly]
																					// [0.5-]1.5-4.5]
																					// ;//
																					// often
																					// wider
																					// than
																					// 2
																					// cm.

				if(ch.length()==0 && parts[0].indexOf("size[")>=0){
					ch = "size";
				}
				if(parts[0].indexOf("orientation[")>=0) ch = "orientation";
				parts[0] = parts[0].trim().replace("size[", "").replace("orientation[", "").replaceFirst("\\]$", "");
				Pattern p = Pattern.compile(NumericalHandler.numberpattern + " ?[{<(]?(?:"+ChunkedSentence.units+"|"+ChunkedSentence.percentage+"|"+ChunkedSentence.degree+")?[)>}]?\\b?(" + ChunkedSentence.times + ")?\\b");
				Matcher m = p.matcher(parts[0]);
				String numeric = "";
				if (m.find()) { // a series of number
					numeric = parts[0].substring(m.start(), m.end()).trim().replaceAll("[{<(]$", "");
				} else {
					p = Pattern.compile("\\d+ ?[{<(]?(?:"+ChunkedSentence.units+"|"+ChunkedSentence.percentage+"|"+ChunkedSentence.degree+")?[)>}]?\\b?(" + ChunkedSentence.times + ")?\\b"); // 1
																												// number
					m = p.matcher(parts[0]);
					m.find();
					numeric = parts[0].substring(m.start(), m.end()).trim().replaceAll("[{<(]$", "");
				}
				modifier = modifier.replaceAll("(m\\[|\\])", "");
				modifier = modifier+";"+ parts[0].substring(0, parts[0].indexOf(numeric)).replaceAll("(\\w+\\[|\\[|\\]|\\{|\\})", "").trim();
				modifier = modifier+";"+ parts[0].substring(parts[0].indexOf(numeric)+numeric.length()).replaceAll("(\\w+\\[|\\[|\\]|\\{|\\})", "").trim();
				modifier = modifier.replaceAll(";+", ";").replaceAll("(^;|;$)", "").replaceAll("-", " ");
				if (parts.length < 2) {// parse out a constraint for further
										// process
					//String constraint = parts[0].substring(parts[0].indexOf(numeric) + numeric.length()).trim(); //treated as a modifier above
					String t = parts[0];
					parts = new String[2];// parsed out a constraint for further
											// process
					parts[0] = t;
					//parts[1] = constraint;
					parts[1] = "";
				}
				/*
				 * String modifier = parts[0].replaceFirst("size\\[.*?\\]",
				 * ";").trim().replaceAll("(^;|;$|\\w\\[|\\])", ""); String
				 * numeric = parts[0].substring(parts[0].indexOf("size["));
				 * numeric = numeric.substring(0,
				 * numeric.indexOf("]")+1).replaceAll("(\\w+\\[|\\])", "");
				 */
				if(modifier.indexOf(" or ")>0){
					String[] mods = modifier.split(" or ");
					for(String mod: mods){
						charas.addAll(this.annotateNumericals(numeric.replaceAll("[{<()>}]", ""), ch, mod.replaceAll("[{<()>}]", ""), parents, false, false));
					}
				}else{
					charas = this.annotateNumericals(numeric.replaceAll("[{<()>}]", ""), ch, modifier.replaceAll("[{<()>}]", ""), parents, false, false);
				}
			} else {// size[{shorter} than {plumose} {inner}]; size[{equal-to} or {greater} than] 
				String value = "";
				String mod = "";
				if(modifier.length()>0){
					if(modifier.contains("m[not]")) mod = "not";
					if(modifier.contains("m[no]")) mod = "no";
					if(modifier.contains("m[much]")) value = "much";
				}
				if(parts[0].indexOf(" or ")>0){
					if(ch.length()==0) ch = parts[0].substring(0, parts[0].indexOf("["));
					parts[0] = parts[0].replaceAll("(\\w+\\[|\\])", "");
					String[] subparts = parts[0].split("( or | , )");
					for(String subpart: subparts){
						subpart = mod+";"+subpart.replaceAll("(\\{|\\})", "").trim();	
						subpart = subpart.replaceAll("-", " ");
						subpart = subpart.replaceFirst("\\s+than$", "");
						this.createCharacterElement(parents, charas, subpart.replaceAll(";+", ";").replaceAll("(^;|;$)", "").replaceAll("-", " "), value, ch, "", false);
						//charas.addAll(this.processSimpleCharacterState(ch+"["+subpart.replaceAll("(\\{|\\})", "").trim()+"]", parents));
					}
				}else{
					if(ch.length()==0) ch = parts[0].substring(0, parts[0].indexOf("["));
					this.createCharacterElement(parents, charas, (mod+";"+parts[0].replaceAll("(\\{|\\})", "").trim()).replaceAll(";+", ";").replaceAll("(^;|;$)","").replaceAll("-", " "), value, ch, "", false);
					//charas = this.processSimpleCharacterState(parts[0].replaceAll("(\\{|\\})", "").trim(), parents); // numeric part
				}
			}
		}
		String object = null;
		ArrayList<Element> structures = new ArrayList<Element>();
		if (parts.length > 1 && parts[1].length() > 0) {// parts[1]: than
														// (other) {pistillate}
														// (paleae)]
			if (parts[1].indexOf("(") >= 0) {
				String ostr = parts[1];
				object = ostr.replaceFirst("^.*?(?=[({])", "").replaceFirst("\\]+$", ""); // (other)
																							// {pistillate}
																							// (paleae)
				object = "o[" + object + "]";
				if (object != null) {
					structures.addAll(this.processObject(object));
				}
				/*
				 * while(ostr.indexOf('(')>=0){ object =
				 * ostr.substring(ostr.indexOf('('), ostr.indexOf(')')+1);
				 * object = "o["+object+"]"; ostr =
				 * ostr.substring(ostr.indexOf(')')+1); if(object != null){
				 * structures.addAll(this.processObject(object)); } }
				 */
			}
			// have constraints even without an organ 12/15/10
			Iterator<Element> it = charas.iterator();
			while (it.hasNext()) {
				Element e = it.next();
				// if(parts[1].indexOf("(")>=0){
				// this.addAttribute(e, "constraint",
				// this.listStructureNames(parts[1]));
				// }else{
				String constraint = parts[1].replaceAll("(\\(|\\)|\\{|\\}|\\w*\\[|\\])", "");
				constraint = map2character(constraint); //long => length
				this.addAttribute(e, "constraint", constraint);
				// }
				if (object != null) {
					this.addAttribute(e, "constraintid", this.listStructureIds(structures));// TODO:
																							// check:
																							// some
																							// constraints
																							// are
																							// without
																							// constraintid
				}
			}

		}
		if (structures.size() > 0) {
			return structures;
		} else {
			return charas;
		}
	}

	/**
	 * ChunkedSentence.asasthan: "long|wide|broad|tall|high|deep|short|narrow|thick"
	 * 
	 * @param constraint
	 * @return
	 */
	private String map2character(String constraint) {
		constraint = constraint.replaceAll("\\blong\\b", "length");
		constraint = constraint.replaceAll("\\blonger\\b", "length");
		//constraint = constraint.replaceAll("\\blongest\\b", "length");
		
		constraint = constraint.replaceAll("\\bwide\\b", "width");
		constraint = constraint.replaceAll("\\bwider\\b", "width");
		//constraint = constraint.replaceAll("\\bwidest\\b", "width");
		
		constraint = constraint.replaceAll("\\bbroad\\b", "width");
		constraint = constraint.replaceAll("\\bbroader\\b", "width");
		//constraint = constraint.replaceAll("\\bbroadest\\b", "width");
		
		constraint = constraint.replaceAll("\\btall\\b", "height");
		constraint = constraint.replaceAll("\\btaller\\b", "height");
		//constraint = constraint.replaceAll("\\btallest\\b", "height");
		
		constraint = constraint.replaceAll("\\bhigh\\b", "height");
		constraint = constraint.replaceAll("\\bhigher\\b", "height");
		//constraint = constraint.replaceAll("\\bhighest\\b", "height");
		
		constraint = constraint.replaceAll("\\bdeep\\b", "height");
		constraint = constraint.replaceAll("\\bdeeper\\b", "height");
		//constraint = constraint.replaceAll("\\bdeepest\\b", "height");

		constraint = constraint.replaceAll("\\bshort\\b", "length");
		constraint = constraint.replaceAll("\\bshorter\\b", "length");
		//constraint = constraint.replaceAll("\\bshortest\\b", "length");
		
		constraint = constraint.replaceAll("\\bnarrow\\b", "width");
		constraint = constraint.replaceAll("\\bnarrower\\b", "width");
		//constraint = constraint.replaceAll("\\bnarrowest\\b", "width");
		
		constraint = constraint.replaceAll("\\bthick\\b", "height");
		constraint = constraint.replaceAll("\\bthicker\\b", "height");
		//constraint = constraint.replaceAll("\\bthickest\\b", "height");
		
		return constraint;
	}

	private ArrayList<Element> latest(String name, ArrayList<Element> list) {
		ArrayList<Element> selected = new ArrayList<Element>();
		int size = list.size();
		for (int i = size - 1; i >= 0; i--) {
			if (list.get(i).getName().compareTo(name) == 0) {
				selected.add(list.get(i));
			} else {
				break;
			}
		}
		return selected;
	}

	/**
	 * 
	 * @param replaceFirst
	 */
	private void processChunkBracketed(String content) {
		// TODO Auto-generated method stub

	}

	/**
	 * 
	 * m[usually] v[comprising] o[a {surrounding} (involucre)]
	 * 
	 * @param content
	 * @param parents
	 * @return
	 */
	private ArrayList<Element> processTVerb(String content, ArrayList<Element> parents) {
		ArrayList<Element> results = new ArrayList<Element>();
		// String object = content.substring(content.indexOf("o["));
		String object = content.substring(content.lastIndexOf("o["));
		String rest = content.replace(object, "").trim();
		String relation = rest.substring(rest.indexOf("v["));
		String modifier = rest.replace(relation, "").trim().replaceAll("(m\\[|\\])", "");

		/*
		 * excluded from contact with frontal by sphenotic: "contact" is the
		 * subject of with... if(object.indexOf("(")<0){//content: v[{present}]
		 * regardless o[r[p[of] o[l[season or sex]]]] //take the v and make it a
		 * character //relation: v[{present}] regardless o[r[p[of] String v =
		 * relation.substring(0,
		 * relation.indexOf("]")).replaceAll("(v\\[|\\{|\\}|\\])", ""); String
		 * character = TermOutputerUtilities.lookupCharacter(v, conn,
		 * ChunkedSentence.characterhash, glosstable, tableprefix); if(character
		 * !=null){ this.createCharacterElement(this.subjects, results, "", v,
		 * character, ""); } return results; }
		 */
		object = parenthesis(object); //o[(fibula) {size}]]]	
		if (object.indexOf("(")>=0) {
			object = object.substring(0,  object.lastIndexOf(")")+1)+"]";
			ArrayList<Element> tostructures = this.processObject(object); // TODO:
																			// fix
																			// content
																			// is
																			// wrong.
																			// i8:
																			// o[a]
																			// architecture[surrounding
																			// (involucre)]
			results.addAll(tostructures);
			this.createRelationElements(relation.replaceAll("(\\w\\[|\\])", ""), this.subjects, tostructures, modifier, false);
			return results;
		} else {
			return latestelements;
		}
	}

	/**
	 * @param content
	 *            : m[usually] coloration[dark brown]: there is only one
	 *            character states and several modifiers
	 * @param parents
	 *            : of the character states
	 */
	private ArrayList<Element> processSimpleCharacterState(String content, ArrayList<Element> parents) {
		ArrayList<Element> results = new ArrayList<Element>();
		String modifier = "";
		String character = "";
		String state = "";
		String[] tokens = content.split("\\]\\s*");
		for (int i = 0; i < tokens.length; i++) {
			//Changed by Zilong: update: this change created numerous errors -- abandoned by Hong 7/1/13.
			//more ventrally->more should be modifier of ventrally
			//however, parsed as adj[more] adv[ventrally]
			//if(tokens[i].matches("^comparison\\[.*")){
			//	if(i<tokens.length-1){//only if not the last token
			//		if(tokens[i+1].matches("^m\\[.*")){//next token is a modifier.
			//			modifier +=tokens[i].replaceAll("^comparison\\[", "").trim()+" "+tokens[i+1]+" ";
			//			i++;
			//			continue;
			//		}
			//	}
		
			//}else 	//changed by Zilong
				
			if (tokens[i].matches("^m\\[.*")) {
				modifier += tokens[i] + " ";
			} else if (tokens[i].matches("^\\w+\\[.*")) {
				String[] parts = tokens[i].split("\\[");
				character = parts[0];
				if (this.unassignedcharacter != null) {
					character = this.unassignedcharacter;
					this.unassignedcharacter = null;
				}
				state = parts[1];
				modifier += "; ";
			}
		}
		modifier = modifier.replaceAll("m\\[", "").trim().replaceAll("(^\\W|\\W$)", "").trim();

		// make backups
		// String statecp = state;
		// String charactercp = character;

		Element lastelement = this.latestelements.get(this.latestelements.size() - 1);

		// Altered by Zilong
		// if the a simple character state is immediately preceded by a
		// conjunction,
		// the last element should not be changed to have the same attribute
		// "name" as
		// the current element.
		String conjunction = "(and|or)";
		String previousToken = this.cs.getTokenAt((this.cs.getPointer() - 2));
		if (previousToken != null) {
			if (previousToken.matches(conjunction)) {
				this.createCharacterElement(parents, results, modifier, state, character, "", false);
			}

			// deal with possible 3cm wide, or high relief cases: rewrite the
			// logic to make is simple and more robust
			else if (lastelement.getName().compareTo("character") == 0) {
				String eqcharacter = ChunkedSentence.eqcharacters.get(state); // find
																				// the
																				// equivalent
																				// character
																				// for
																				// the
																				// state,
																				// e.g.
																				// wide,
																				// relief
				if (eqcharacter != null) {// yes, it is the case
					Iterator<Element> it = this.latestelements.iterator();
					while (it.hasNext()) {
						lastelement = it.next();
						lastelement.setAttribute("name", eqcharacter);
					}
					results = this.latestelements;
				} else {// no, it is not the case
					this.createCharacterElement(parents, results, modifier, state, character, "", false);
				}
			} else {// no, it is not the case
				this.createCharacterElement(parents, results, modifier, state, character, "", false);
			}
		} else {
			this.createCharacterElement(parents, results, modifier, state, character, "", false);
		}
		/*
		 * seemed to be unnecessarily complexed and sensitive to the glossary
		 * (the lookupcharacter step determines the logic) String eqcharacter =
		 * ChunkedSentence.eqcharacters.get(state); if(eqcharacter != null){
		 * state = eqcharacter; character =
		 * TermOutputerUtilities.lookupCharacter(eqcharacter, conn,
		 * ChunkedSentence.characterhash, this.glosstable, this.tableprefix);
		 * if(character ==null){ state = statecp; character = charactercp; } }
		 * if(character.compareToIgnoreCase("character")==0 && modifier.length()
		 * ==0){//high relief: character=relief, reset the character of "high"
		 * to "relief" Iterator<Element> it = this.latestelements.iterator();
		 * while(it.hasNext()){ lastelement = it.next();
		 * lastelement.setAttribute("name", state); } }else
		 * if(lastelement.getName().compareTo("structure")==0){
		 * this.unassignedcharacter = state; } results = this.latestelements;
		 * }else if(state.length()>0){ //if(this.unassignedmodifiers!=null &&
		 * this.unassignedmodifiers.length()>0){ // modifier =
		 * modifier+";"+this.unassignedmodifiers; // this.unassignedmodifiers =
		 * ""; //} this.createCharacterElement(parents, results, modifier,
		 * state, character, ""); }
		 */

		return results;
	}

	private void establishSubject(String content, boolean aftercomma/* , boolean makeconstraint */) {
		ArrayList<Element> structures = createStructureElements(content, aftercomma/*
																		 * ,
																		 * makeconstraint
																		 */);
		this.subjects = new ArrayList<Element>();
		this.latestelements = new ArrayList<Element>();
		Iterator<Element> it = structures.iterator();
		while (it.hasNext()) {
			Element e = it.next();
			if (e.getName().compareTo("structure") == 0) { // ignore character
															// elements
				this.subjects.add(e);
				this.latestelements.add(e);
			}
		}
	}

	// fix: can not grab subject across treatments
	private void reestablishSubject() {
		Iterator<Element> it = this.subjects.iterator();
		this.latestelements = new ArrayList<Element>();
		while (it.hasNext()) {
			Element e = it.next();
			e.detach();
			// this.statement.addContent(e);
			addContent(this.statement, e);
			this.latestelements.add(e);
		}
	}

	/**
	 * TODO:
	 * {shape~list~usually-flat-to-convex-punct-sometimes-conic-or-columnar}
	 * {pubescence-list-sometimes-bristly-or-hairy}
	 * 
	 * @param content
	 *            : pubescence[m[not]
	 *            {pubescence-list-sometimes-bristly-or-hairy}]
	 * @param parents
	 * @param characterismodifier 
	 * @return
	 */
	private ArrayList<Element> processCharacterList(String content, ArrayList<Element> parents, boolean characterismodifier) {
		ArrayList<Element> results = new ArrayList<Element>();
		String modifier = "";
		if (content.indexOf("m[") >= 0) {
			modifier = content.substring(content.indexOf("m["), content.indexOf("{"));
			content = content.replace(modifier, "");
			modifier = modifier.trim().replaceAll("(m\\[|\\])", "");
		}
		content = content.replace(modifier, "");
		String[] parts = content.split("\\[");
		String cname = "";
		String list = "";
		if (parts.length < 2) {
			// {count~list~2~or~fewer}
			int i = parts[0].indexOf("~list~");
			if (i > 0) {
				cname = parts[0].substring(0, i).replace("{", "");
				list = parts[0];
			} else
				return results; // @TODO: parsing failure
		} else {
			cname = parts[0];
			list = parts[1];
		}
		if (this.unassignedcharacter != null) {
			cname = this.unassignedcharacter;
			this.unassignedcharacter = null;
		}
		String cvalue = list.replaceFirst("\\{" + cname + "~list~", "").replaceFirst("\\W+$", "").replaceAll("~", " ").trim();
		if (cname.endsWith("ttt")) {
			this.createCharacterElement(parents, results, modifier, cvalue, cname.replaceFirst("ttt$", ""), "", characterismodifier);
			return results;
		}
		if (cvalue.indexOf(" to ") >= 0) {
			createRangeCharacterElement(parents, results, modifier, cvalue.replaceAll("punct", ",").replaceAll("(\\{|\\})", ""), cname, characterismodifier); // add
																																			// a
																																			// general
																																			// statement:
																																			// coloration="red to brown"
		}
		String mall = "";
		boolean findm = false;
		// gather modifiers from the end of cvalues[i]. this modifier applies to
		// all states
		do {
			findm = false;
			String last = cvalue.substring(cvalue.lastIndexOf(' ') + 1);
			if (Utilities.lookupCharacter(last, conn, ChunkedSentence.characterhash, glosstable, tableprefix) == null
					&& Utilities.isAdv(last, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)) {
				mall += last + " ";
				cvalue = cvalue.replaceFirst(last + "$", "").trim();
				findm = true;
			}
		} while (findm);

		String[] cvalues = cvalue.split("\\b(to|or|punct)\\b");// add individual
																// values
		for (int i = 0; i < cvalues.length; i++) {
			String state = cvalues[i].trim();// usually papillate to hirsute
												// distally
			// gather modifiers from the beginning of cvalues[i]. a modifier
			// takes effect for all state until a new modifier is found
			String m = "";
			do {
				findm = false;
				if (state.length() == 0) {
					break;
				}
				int end = state.indexOf(' ') == -1 ? state.length() : state.indexOf(' ');
				String w = state.substring(0, end);
				if (Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, glosstable, tableprefix) == null
						&& Utilities.isAdv(w, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)) {
					m += w + " ";
					w = w.replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}").replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)").replaceAll("\\+", "\\\\+");
					state = state.replaceFirst(w, "").trim();
					findm = true;
				}
			} while (findm);
			if (m.length() == 0) {
				state = (modifier + " " + mall + " " + state.replaceAll("\\s+", "#")).trim(); // prefix
																								// the
																								// previous
																								// modifier
			} else {
				modifier = modifier.matches(".*?\\bnot\\b.*") ? modifier + " " + m : m; // update
																						// modifier
				// cvalues[i] = (mall+" "+cvalues[i]).trim();
				state = (modifier + " " + mall + " " + state.replaceAll("\\s+", "#")).trim(); // prefix
																								// the
																								// previous
																								// modifier
			}
			String[] tokens = state.split("\\s+");
			tokens[tokens.length - 1] = tokens[tokens.length - 1].replaceAll("#", " ");
			results.addAll(this.processCharacterText(tokens, parents, cname, characterismodifier));
			// results.addAll(this.processCharacterText(new String[]{state},
			// parents, cname));
		}

		return results;
	}

	/**
	 * crowded to open for categorical range-value
	 * 
	 * @param parents
	 * @param results
	 * @param modifier
	 * @param cvalue
	 * @param cname
	 * @param characterismodifier 
	 */
	private String createRangeCharacterElement(ArrayList<Element> parents, ArrayList<Element> results, String modifiers, String cvalue, String cname, boolean characterismodifier) {
		Element character = new Element("character");
		if (this.inbrackets) {
			character.setAttribute("in_bracket", "true");
		}
		if(characterismodifier){
			character.setAttribute("is_modifier", "true");
			if(debugextraattributes) System.out.println("is modifier:"+cvalue);
		}
		character.setAttribute("char_type", "range_value");
		character.setAttribute("name", cname);

		String[] range = cvalue.split("\\s+to\\s+");// a or b, c, to d, c, e
		String[] tokens = range[0].replaceFirst("\\W$", "").replaceFirst("^.*?\\s+or\\s+", "").split("\\s*,\\s*"); // a
																													// or
																													// b,
																													// c,
																													// =>
		String from = getFirstCharacter(tokens[tokens.length - 1]);
		tokens = range[1].split("\\s*,\\s*");
		String to = getFirstCharacter(tokens[0]);
		character.setAttribute("from", from.replaceAll("-c-", " ")); // a or b
																		// to c
																		// => b
																		// to c
		character.setAttribute("to", to.replaceAll("-c-", " "));

		boolean usedm = false;
		Iterator<Element> it = parents.iterator();
		while (it.hasNext()) {
			Element e = it.next();
			character = (Element) character.clone();
			if (modifiers.trim().length() > 0) {
				addAttribute(character, "modifier", modifiers.trim()); // may
																		// not
																		// have
				usedm = true;
			}
			results.add(character); // add to results
			// e.addContent(character);//add to e
			addContent(e, character);
		}
		if (usedm) {
			modifiers = "";
		}
		addClauseModifierConstraint(cs, character);
		return modifiers;

	}

	/**
	 * 
	 * @param tokens
	 *            : usually large
	 * @return: large
	 */
	private String getFirstCharacter(String character) {
		String[] tokens = character.trim().split("\\s+");
		String result = "";
		for (int i = 0; i < tokens.length; i++) {
			if (Utilities.lookupCharacter(tokens[i], conn, ChunkedSentence.characterhash, glosstable, tableprefix) != null) {
				result += tokens[i] + " ";
			}
		}
		return result.trim();
	}

	/**
	 * 
	 * @param elements
	 */
	private void updateLatestElements(ArrayList<Element> elements) {
		this.latestelements = new ArrayList<Element>();
		if (elements != null) {
			latestelements.addAll(elements);
		}
	}

	/**
	 * //t[c/r[p/o]] m[sometimes] v[subtended] r[p[by] o[(calyculi)]] m[loosely
	 * loosely] architecture[arachnoid] r[p[at] o[m[distal] {end}]]
	 * 
	 * t[c[{sometimes} with (bases) {decurrent}] r[p[onto] o[(stems)]]]
	 * 
	 * nested:{often} {dispersed} r[p[with] o[aid r[p[from] o[(pappi)]]]]
	 * 
	 * @param ck
	 */

	private void processCHPP(String content) {
		// having oval outline
		if (this.characterPrep(content)) {
			return;
		}
		String c = content.substring(0, content.indexOf("r["));
		String r = content.replace(c, "");
		if (r.lastIndexOf("o[") < 0) { // #{usually} {arising} r[p[in]]#
										// {distal} 1/2
			// failed parse
			cs.setPointer2NextComma();
			return;
		}
		String p = r.substring(0, r.lastIndexOf("o["));// {often} {dispersed}
														// r[p[with] o[aid
														// r[p[from]
														// o[(pappi)]]]]
		String o = r.replace(p, "");
		String[] mc = c.split("(?<=\\])\\s*");
		String m = "";
		c = "";
		for (int i = 0; i < mc.length; i++) {
			if (mc[i].startsWith("m[")) {
				m += mc[i] + " ";
			} else if (mc[i].startsWith("c[")/* mc[i].matches("^\\w+\\[.*") */) {
				c += mc[i] + " ";
			}
		}
		m = m.replaceAll("(m\\[|\\]|\\{|\\})", "").trim();
		c = c.replaceAll("(c\\[|\\]|\\{|\\})", "").trim(); // TODO: will this
															// work for nested
															// chuncks?
		p = p.replaceAll("(\\w\\[|\\])", "").trim();
		// c: {loosely} {arachnoid}
		String[] words = c.split("\\s+");
		if (Utilities.isVerb(words[words.length - 1], ChunkedSentence.verbs, ChunkedSentence.notverbs) || p.compareTo("to") == 0) {// t[c[{connected}]
																																	// r[p[by]
																																	// o[{conspicuous}
																																	// {arachnoid}
																																	// <trichomes>]]]
																																	// TODO:
																																	// what
																																	// if
																																	// c
																																	// was
																																	// not
																																	// included
																																	// in
																																	// this
																																	// chunk?
			String relation = (c + " " + p).replaceAll("\\s+", " ");
			o = o.replaceAll("(o\\[|\\])", "");
			/*
			 * if(!o.endsWith(")") &&!o.endsWith("}")){ //1-5 series => 1-5
			 * (series) String t = o.substring(o.lastIndexOf(' ')+1); o =
			 * o.replaceFirst(t+"$", "("+t)+")"; }
			 */
			if (!o.endsWith(")")) { // force () on the last word. Hong 3/4/11
				String t = o.substring(o.lastIndexOf(' ') + 1);
				t = t.replace("{", "").replace("}", "");
				o = o.substring(0, o.lastIndexOf(' ') + 1) + "(" + t + ")";

				// System.out.println("forced organ in: "+o);
			}
			ArrayList<Element> structures = processObject("o[" + o + "]");
			ArrayList<Element> entity1 = null;
			Element e = this.latestelements.get(this.latestelements.size() - 1);
			if (e.getName().matches("(" + this.delims + ")") || e.getName().compareTo("character") == 0) {
				entity1 = this.subjects;
			} else {
				entity1 = (ArrayList<Element>) this.latestelements.clone();
				// entity1.remove(entity1.size()-1);
			}
			createRelationElements(relation, entity1, structures, m, false);
			updateLatestElements(structures);
		} else {// c: {loosely} {arachnoid} : should be m[loosly]
				// architecture[arachnoid]
				// String[] tokens = c.replaceAll("[{}]", "").split("\\s+");
				// ArrayList<Element> charas = this.processCharacterText(tokens,
				// this.subjects);
			ArrayList<Element> charas = this.processSimpleCharacterState(c, this.subjects);
			updateLatestElements(charas);
			processPrep(new ChunkPrep(r)); // not as a relation
		}

	}

	/**
	 * CK takes form of relation character/states [structures]? update
	 * this.latestElements with structures only.
	 * 
	 * nested1: r[p[of] o[5-40 ,
	 * fusion[{fusion~list~distinct~or~basally~connate}] r[p[in] o[groups]] ,
	 * coloration[{coloration~list~white~to~tan}] , {wholly} or {distally}
	 * {plumose} (bristles)]] []] nested2: r[p[with] o[{central} {cluster}
	 * r[p[of] o[(spines)]]]]
	 * 
	 * @param ck
	 * @param asrelation
	 *            : if this PP should be treated as a relation
	 */
	private void processPrep(ChunkPrep ck) {
		String ckstring = ck.toString(); // r[{} {} p[of] o[.....]]
		String modifier = ckstring.substring(0, ckstring.indexOf("p[")).replaceFirst("^r\\[", "").replaceAll("[{}]", "").trim();
		// sometime o[] is not here as in ckstring=r[p[at or above]] {middle}
		// String pp = ckstring.substring(ckstring.indexOf("p["),
		// ckstring.lastIndexOf("] o[")).replaceAll("(\\w\\[|])", "");
		// String object =
		// ckstring.substring(ckstring.lastIndexOf("o[")).replaceFirst("\\]+$",
		// "")+"]";
		int objectindex = ckstring.indexOf("]", ckstring.indexOf("p[") + 1);
		String pp = ckstring.substring(ckstring.indexOf("p["), objectindex).replaceAll("(\\w\\[|])", "");
		pp = pp.replace("-", " ");
		String object = "o[" + ckstring.substring(objectindex).trim().replaceAll("(\\b\\w\\[)|]", "").trim() + "]";
		// String object =
		// "o["+ckstring.substring(objectindex).trim().replaceAll("(\\b\\w\\[|])",
		// "")+"]";
		// String object =
		// "o["+ckstring.substring(objectindex).trim().replaceAll("(\\[|])",
		// "")+"]";
		// TODO: r[p[in] o[outline]] or r[p[with] o[irregular ventral profile]]
		if (characterPrep(ckstring)) {
			return;
		}

		if (statePrep(ckstring)) {
			return;
		}
		/*
		 * String pp = null; String object = null;
		 * if(ckstring.matches(".*?\\]{4,}$")){//nested2 pp =
		 * ckstring.substring(ckstring.indexOf("p["),
		 * ckstring.lastIndexOf("] o[")).replaceAll("(\\w\\[|])", ""); object =
		 * ckstring.substring(ckstring.lastIndexOf("o[")).replaceFirst("\\]+$",
		 * "")+"]"; }else{//nested1 or not nested pp =
		 * ckstring.substring(ckstring.indexOf("p["),
		 * ckstring.indexOf("] o[")).replaceAll("(\\w\\[|])", ""); object =
		 * ckstring.substring(ckstring.indexOf("o[")).replaceFirst("\\]+$",
		 * "")+"]";//nested or not }
		 */

		object = NumericalHandler.originalNumForm(object);
		boolean lastIsStruct = false;
		boolean lastIsChara = false;
		boolean lastIsComma = false;
		// mohan code to get the original subject if the subject is empty Store
		// the chunk into the modifier
		// in dorsal view => in-dorsal-vew
		if (this.latestelements.size() == 0) {
			String content = ck.toString().replaceAll(" ", "-");
			// String structure = "m[" +content+"]";
			String structure = content.replaceAll("]-o\\[", "-").replaceAll("[{()}]", "");
			if (cs.unassignedmodifier == null) {
				cs.unassignedmodifier = structure;
			} else {
				cs.unassignedmodifier += structure;
			}

			return;
		}

		// end mohan code
		Element lastelement = this.latestelements.get(this.latestelements.size() - 1);
		if (lastelement.getName().compareTo("structure") == 0) {// latest
																// element is a
																// structure
			lastIsStruct = true;
		} else if (lastelement.getName().compareTo("character") == 0) {
			lastIsChara = true;
		} else if (lastelement.getName().matches("(" + this.delims + ")")) {
			lastIsComma = true;
			if (this.printComma) {
				System.out.println("prep ahead of character: " + ckstring);
			}
		}
		// of o[3-7]
		if (lastIsStruct && object.matches("o\\[\\(?\\[?\\d.*?\\d\\+?\\]")) {
			this.annotateNumericals(object.replaceAll("(o\\[|\\])", ""), "count", null, this.latestelements, false, false);
			return;
		}

		ArrayList<Element> structures = new ArrayList<Element>();
		// 3/30/2011: try to separate "in {} {} arrays" cases from
		// "at {flowering}", "in fruit", and "in size" cases
		// allow () be added around the last bare word if there is a {} before
		// the bare word, or if the word is not a character (size, profile,
		// lengths)
		object = parenthesis(object);
		// o[the {frontal} and the (sphenotic) ({spine})] ==> o[the {frontal}
		// ({spine}) and the (sphenotic) ({spine})]
		object = normalizeSharedOrganObject(object);
		/*
		 * if(! object.matches(".*?\\}\\]+$")){ //contains organ: > or untagged:
		 * arrays //add () around the last word if it is bare
		 * if(object.matches(".*?[a-z]\\]+$")){
		 * System.out.println("!!!!!!Object: "+object); int l =
		 * object.lastIndexOf(' '); if(l>0){ String last =
		 * object.substring(l+1); object = object.replaceFirst(last+"$",
		 * "("+last.replaceFirst("\\]", ")]")); }else{//object= o[tendrils]
		 * object = object.replaceFirst("\\[", "[(").replaceFirst("\\]", ")]");
		 * } }
		 */
		if (object.matches(".*?\\)\\]+$")) {
			// structures = linkObjects(modifier, pp, object, lastIsStruct,
			// lastIsChara, lastelement);
			structures = linkObjects(modifier, pp, object, lastIsStruct, lastIsChara); // apply
																						// to
																						// all
																						// latestelements
			updateLatestElements(structures);
		} else if (object.matches(".*?\\([-a-z]+\\).*") && !object.matches(".*?[-a-z]+\\]+$")) {// contains
																								// organ
																								// in
																								// the
																								// middle
																								// of
																								// object:r[p[from]
																								// o[{thick}
																								// {notothyrial}
																								// (platform)
																								// {excavated}
																								// {laterally}]]
			String obj = object.substring(0, object.lastIndexOf(")") + 1).trim();
			String modi = object.substring(object.lastIndexOf(")") + 1).trim(); // TODO:
																				// left
																				// out
																				// right
																				// end
																				// modi
																				// for
																				// now.

			object = obj;
			// structures = linkObjects(modifier, pp, object, lastIsStruct,
			// lastIsChara, lastelement);
			structures = linkObjects(modifier, pp, object, lastIsStruct, lastIsChara); // apply
																						// to
																						// all
																						// latestelements
			updateLatestElements(structures);
		} else {// "at {flowering}]" or "in size]"
				// contains no organ, e.g. "at flowering"
				// Element last =
				// this.latestelements.get(this.latestelements.size()-1);
			if (lastIsStruct) {
				for (Element lastE : this.latestelements) {
					lastE.setAttribute("name", lastE.getAttributeValue("name") + " " + ckstring.replaceAll("(\\w\\[|\\]|\\{|\\})", "").trim());
					// addAttribute(lastE, "constraint",
					// ckstring.replaceAll("(\\w\\[|\\]|\\{|\\})", ""));//TODO
					// 5/16/2011 <corollas> r[p[of] o[{sterile} {much}
					// {expanded} and {exceeding} (corollas)]] This should not
					// be happening.z[{equaling} (phyllaries)] r[p[at]
					// o[{flowering}]]
				}
			} else if (lastIsChara) { // character element
				for (Element lastE : this.latestelements) {
					addAttribute(lastE, "modifier", ckstring.replaceAll("(\\w\\[|\\]|\\{|\\})", ""));
				}
			}
			// addPPAsAttributes(ckstring);
		}

		// bookkeeping: update this.latestElements: only structures are visible
		// updateLatestElements(structures);
	}

	private boolean statePrep(String ckstring) {
		ckstring = ckstring.replaceAll("(\\w+\\[|\\]|\\)|\\(|\\{|\\})", "");
		if (ckstring.compareTo("in direct contact") == 0) {
			ArrayList<Element> structs = null;
			if (this.latestelements.size() != 0 && this.latestelements.get(this.latestelements.size() - 1).getName().compareTo("structure") == 0) {
				structs = this.latestelements;
			} else if (this.subjects.size() != 0) {
				structs = this.subjects;
			} else { // create placeholder structure "whole_organism"
				this.establishSubject("(whole_organism)", false);
				structs = this.subjects;
			}
			Element ch = new Element("character");
			ch.setAttribute("name", "position");
			ch.setAttribute("value", ckstring);
			ArrayList<Element> ech = new ArrayList<Element>();
			for (Element s : structs) {
				ech.add(ch);
				this.addContent(s, ch);
			}
			this.updateLatestElements(ech);
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param ckstring
	 *            :r[p[in] o[outline]]
	 * @return
	 */
	private boolean characterPrep(String ckstring) {
		boolean done = false;
		String lastword = ckstring.substring(ckstring.lastIndexOf(" ")).replaceAll("\\W", "");
		if (lastword.matches("(" + this.characters + ")")) {
			Element lastelement = this.latestelements.get(this.latestelements.size() - 1);
			if (lastelement.getName().compareTo("character") == 0) {// shell
																	// oval in
																	// outline
				Iterator<Element> it = this.latestelements.iterator();
				while (it.hasNext()) {
					lastelement = it.next();
					lastelement.setAttribute("name", lastword);
				}
				done = true;
			} else if (lastelement.getName().compareTo("structure") == 0) {// shell
																			// in
																			// oval
																			// outline
				String cvalue = ckstring.replaceFirst(".*?\\]", "").replaceAll("\\w+\\[", "").replaceAll(lastword, "").replaceAll("[{}\\]\\[]", "").trim();
				if(!cvalue.endsWith(")")){
					Iterator<Element> it = this.latestelements.iterator();
					while (it.hasNext()) {
						lastelement = it.next();
						Element chara = new Element("character");
						chara.setAttribute("name", lastword);
						chara.setAttribute("value", cvalue);
						this.addContent(lastelement, chara);
					}
					done = true;
				}
			}
		}
		return done;
	}

	private String parenthesis(String object) {
		if (!object.matches(".*?\\}\\]+$")) { // contains organ: > or untagged:
												// arrays
			if (object.matches(".*?\\bl\\[.*")) { // deal with list: o[l[season
													// or sex]]]]
				String beforelist = object.substring(0, object.indexOf("l["));
				String list = object.substring(object.indexOf("l[")); // l[season
																		// or
																		// sex]]]]
				list = list.replaceFirst("\\]", ")]").replaceFirst("\\[", "[(").replaceAll(" ", ") (");
				list = list.replaceAll("\\)+", ")").replaceAll("\\(+", "(").replaceAll("\\(or\\)", "or").replaceAll("\\(and\\)", "and").replaceAll("\\(,\\)", ",").trim();
				return beforelist + list;
			} else if (object.matches(".*?[a-z]\\]+$")) {// there is a bare word
				int l = object.lastIndexOf(' ');
				l = l < 0 ? object.lastIndexOf('[') : l;
				String last = object.substring(l + 1).replaceAll("\\W+$", "");
				if (object.indexOf('{') >= 0 || !isCharacter(last)) {// if there
																		// are
																		// other
																		// modifiers/characters,
																		// then
																		// must
																		// make
																		// "last"
																		// a
																		// structure
					object = object.replaceFirst(last + "(?=\\]+$)", "(" + last + ")");
				}
			}
		}
		return object;
	}

	private boolean isCharacter(String last) {
		try {
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from " + this.glosstable + " where term='" + last + "' and category='character'");
			if (rs.next()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private ArrayList<Element> linkObjects(String modifier, String pp, String object, boolean lastIsStruct, boolean lastIsChara/*
																																 * ,
																																 * Element
																																 * lastelement
																																 */) {
		ArrayList<Element> structures;
		structures = processObject(object);
		String base = "";
		if (object.matches("o?\\[*\\{*(" + ChunkedSentence.basecounts + ")\\b.*")) {
			base = "each";
		}
		if (lastIsChara) {
			for (Element lastelement : this.latestelements) {
				// if last character is size, change to location: <margins>
				// r[p[with] o[3–6 (spines)]] 1–3 {mm} r[p[{near}] o[(bases)]].
				// 1-3 mm is not a size, but a location of spines
				if (lastelement.getAttributeValue("name").compareTo("size") == 0
						&& ((lastelement.getAttributeValue("value") != null && lastelement.getAttributeValue("value").matches(".*?\\d.*")) || (lastelement
								.getAttributeValue("from") != null && lastelement.getAttributeValue("from").matches(".*?\\d.*")))
						&& pp.matches("(" + ChunkedSentence.locationpp + ")")) {
					lastelement.setAttribute("name", "location");
				}
				// addAttribute(lastelement, "constraint",
				// (pp+" "+base+" "+listStructureNames(structures)).replaceAll("\\s+",
				// " ").replaceAll("(\\{|\\})", "")); //a, b, c
				// addAttribute(lastelement, "constraint",
				// (pp+" "+listStructureNames(object)).replaceAll("\\s+",
				// " ").replaceAll("(\\w+\\[|\\(|\\)|\\{|\\}|\\])", ""));
				addAttribute(lastelement, "constraint", pp + " " + object);
				addAttribute(lastelement, "constraintid", listStructureIds(structures));// "1 2 3"
				if (modifier.length() > 0) {
					addAttribute(lastelement, "modifier", modifier);
				}
			}
		} else {// lastIsStructure

			// handle two cases: 1: the prep is a relation, e.g. of
			if (isRelation(pp)) {
				ArrayList<Element> entity1 = null;
				if (lastIsStruct) {
					entity1 = this.latestelements;
				} else {
					entity1 = this.subjects;
				}
				String relation = relationLabel(pp, entity1, structures);// determine
																			// the
																			// relation
				if (relation != null) {
					createRelationElements(relation, entity1, structures, modifier, false);// relation
																							// elements
																							// not
																							// visible
																							// to
																							// outside
				}
				// reset "subject" structure for prositional preps, so all
				// subsequent characters should refer to organbeforeOf/entity1
				boolean nextisposition = isNextChunkPosition();
				if (relation != null && relation.matches("(" + ChunkedSentence.positionprep + ")") && !nextisposition) {
					structures = entity1;
				}
			} else {
				// 2: the prep can not be a relation, e.g. through, by. Make
				// these modifiers/contraints to the last relation/character
				// A reaching B through C : matching element holds "reading"
				// A communicating with B through C: "communicating [with]"
				// A seperated from B by C: "seperated from"
				// A connected by B to C: "connected by"
				// A extended from (B1 of B2) into C: "extended from"

				// find the last non-of chunk
				int p = cs.getPointer() - 1;
				String lasttoken = "";
				do {
					p--;
					lasttoken = cs.getTokenAt(p);
					if(lasttoken == null){
						lasttoken="";
						break;
					}
				} while (lasttoken.compareTo("") == 0 || lasttoken.startsWith("r[p[of]"));
				
				if (lasttoken.matches("\\w+\\[.*")) {// is a chunk
					ArrayList<Element> targets = retrieveMatchingElement(lasttoken);
					for (Element target : targets) {
						String type = target.getName();
						if (type.compareToIgnoreCase("relation") == 0) {
							this.addAttribute(target, "modifier", pp + " " + object);
						} else if (type.compareToIgnoreCase("character") == 0) {
							this.addAttribute(target, "constraint", pp + " " + object);
							this.addAttribute(target, "constraintid", listStructureIds(structures));// "1 2 3"
						}
					}
				}
			}
		}
		return structures;
	}

	/**
	 * peek if the next chunk is a postion chunk
	 * 
	 * @return
	 */
	private boolean isNextChunkPosition() {
		int pointer = cs.getPointer() + 1;
		int size = cs.getSize();
		String token = "";
		while (token.length() == 0 && pointer < size) {
			token = cs.getTokenAt(pointer++);
		}
		if (token.matches(".*?\\b(" + ChunkedSentence.positionprep + ")\\b.*"))
			return true;
		return false;
	}

	/**
	 * In elementlog, find relation/character elements that matches the token
	 * 
	 * @param lasttoken
	 * @return
	 */
	// A reaching B through C : matching element holds "reading"
	// A communicating with B through C: "communicating [with]"
	// A seperated from B by C: "seperated from"
	// A connected by B to C: "connected by"
	// A extended from (B1 of B2) into C: "extended from"
	private ArrayList<Element> retrieveMatchingElement(String cstoken) {
		ArrayList<Element> result = new ArrayList<Element>();
		cstoken = cstoken.replaceAll("(\\w+\\[|\\]|\\}|\\{|\\)|\\()", "");
		cstoken = cstoken.indexOf(" ") > 0 ? cstoken.substring(0, cstoken.indexOf(" ")) : cstoken; // first
																									// word
		for (int i = this.elementlog.size() - 1; i >= 0; i--) {// search
																// backwards
			Element e = this.elementlog.get(i);
			String type = e.getName();
			if (type.contains("relation")) {
				String name = e.getAttributeValue("name");
				if (name.matches(".*_of"))
					continue;
				if (name.matches(".*?\\b" + cstoken + "\\b.*")) {
					result.add(e);
					while (this.elementlog.get(i - 1).getName().contains("relation") && this.elementlog.get(i - 1).getAttributeValue("name").compareToIgnoreCase(name) == 0) {
						result.add(e);
						i = i - 1;
					}
					return result;
				}
			}
			if (type.contains("character")) {
				String value = e.getAttribute("value") != null ? e.getAttributeValue("value") : null;
				if (value != null && value.matches(".*?\\b" + cstoken + "\\b.*?")) {
					result.add(e);
					while (this.elementlog.get(i - 1).getName().contains("character") && this.elementlog.get(i - 1).getAttribute("value") != null
							&& this.elementlog.get(i - 1).getAttributeValue("value").compareToIgnoreCase(value) == 0) {
						result.add(e);
						i = i - 1;
					}
					return result;
				}
				String constraint = e.getAttribute("constraint") != null ? e.getAttributeValue("constraint") : null;
				if (constraint != null && constraint.matches(".*?\\b" + cstoken + "\\b.*")) {
					result.add(e);
					while (this.elementlog.get(i - 1).getName().contains("character") && this.elementlog.get(i - 1).getAttribute("constraint") != null
							&& this.elementlog.get(i - 1).getAttributeValue("constraint").compareToIgnoreCase(constraint) == 0) {
						result.add(e);
						i = i - 1;
					}
					return result;
				}

			}
		}
		return result;
	}

	/**
	 * test and see whether pp plays the role of a relation here A reaching B
	 * through C A communicating with B through C A seperated from B by C A
	 * connected by B to C A extended from (B1 of B2) into C
	 * 
	 * @param pp
	 * @return
	 */
	private boolean isRelation(String pp) {
		int i = cs.getPointer();
		if ((i - 2) < 0)
			return true;
		String lasttoken = cs.getTokenAt(i - 2);
		// comes right after a verb/prep chuck and part of non-relational prep
		if ((lasttoken.compareTo("") == 0 || lasttoken.matches(".*\\b(b\\[v\\[|r\\[p\\[).*")) && pp.matches("(" + this.nonrelation + ")")) {
			return false;
		}
		return true;
	}

	/**
	 * o[.........{m} {m} (o1) and {m} (o2)] o[each {bisexual} ,
	 * architecture[{architecture
	 * -list-functionally-staminate-punct-or-pistillate}] (floret)]] ;
	 * 
	 * @param object
	 * @return
	 */
	private ArrayList<Element> processObject(String object) {
		ArrayList<Element> structures;
		if (object.indexOf("l[") >= 0) {
			// a list of object
			object = object.replace("l[", "").replaceFirst("\\]", "");
		}
		String[] twoparts = separate(object);// separate characters from the
												// organs in object
												// o[.........{m} {m} (o1) and
												// {m} (o2)]
		structures = createStructureElements(twoparts[1], false/* , false */);// to be
																		// added
																		// structures
																		// found
																		// in
																		// 2nd
																		// part,
																		// not
																		// rewrite
																		// this.latestelements
																		// yet
		if (twoparts[0].length() > 0) {
			/*
			 * if(twoparts[0].matches(".*?\\b\\w\\[.*")){//nested chunks: e.g.
			 * 5-40 , fusion[{fusion~list~distinct~or~basally~connate}] r[p[in]
			 * o[groups]] , coloration[{coloration~list~white~to~tan}] ,
			 * {wholly} or {distally} {plumose} //get tokens for the new
			 * chunkedsentence ArrayList<String>tokens =
			 * TermOutputerUtilities.breakText(twoparts[0]); twoparts[0]=twoparts[0].trim();
			 * if(!twoparts[0].matches(".*?[,;\\.:]$")){ twoparts[0] +=" .";
			 * tokens.add("."); } ChunkedSentence newcs = new
			 * ChunkedSentence(tokens, twoparts[0], conn, glosstable);
			 * //annotate this new chunk ArrayList<Element> subjectscopy =
			 * this.subjects; this.subjects = structures;
			 * newcs.setInSegment(true); annotateByChunk(newcs, false); //no
			 * need to updateLatestElements this.subjects = subjectscopy; }else{
			 */
			ArrayList<Element> structurescp = (ArrayList<Element>) structures.clone();
			String[] tokens = twoparts[0].replaceFirst("[_-]$", "").split("\\s+");// add
																					// character
																					// elements
			if (twoparts[1].indexOf(") plus") > 0) {// (teeth) plus 1-2
													// (bristles), the structure
													// comes after "plus" should
													// be excluded
				String firstorgans = twoparts[1].substring(0, twoparts[1].indexOf(") plus")); // (teeth
				String lastorganincluded = firstorgans.substring(firstorgans.lastIndexOf("(") + 1);
				for (int i = structures.size() - 1; i >= 0; i--) {
					if (!structures.get(i).getAttributeValue("name_original").equals(lastorganincluded)) {
					//if (!structures.get(i).getAttributeValue("name").equals(TermOutputerUtilities.toSingular(lastorganincluded))) {
						structures.remove(i);
					}
				}
			}
			processCharacterText(tokens, structures, null, false); // process part 1,
															// which applies to
															// all
															// lateststructures,
															// invisible
			structures = structurescp;
			// }
		}
		return structures;
	}

	/**
	 * 
	 * @param structures
	 * @return
	 */
	private String listStructureIds(ArrayList<Element> structures) {
		StringBuffer list = new StringBuffer();
		Iterator<Element> it = structures.iterator();
		while (it.hasNext()) {
			Element e = it.next();
			list.append(e.getAttributeValue("id") + ", ");
		}
		return list.toString().trim().replaceFirst(",$", "");
	}

	// find all () in object
	private String listStructureNames(String object) {
		String os = "";
		object = object.replaceAll("\\)\\s*\\(", " "); // (leaf) (blade) =>(leaf
														// blade)
		Pattern p = Pattern.compile(".*?\\(([^)]*?)\\)(.*)");
		Matcher m = p.matcher(object);
		while (m.matches()) {
			os += m.group(1) + ", ";
			object = m.group(2);
			m = p.matcher(object);
		}
		return os.trim().replaceFirst(",$", "");
	}

	/*
	 * private String listStructureNames(ArrayList<Element> structures) {
	 * StringBuffer list = new StringBuffer(); Iterator<Element> it =
	 * structures.iterator(); while(it.hasNext()){ Element e = it.next();
	 * list.append(e.getAttributeValue("name")+", "); } return
	 * list.toString().trim().replaceFirst(",$", ""); }
	 */

	private void createRelationElements(String relation, ArrayList<Element> fromstructs, ArrayList<Element> tostructs, String modifier, boolean symmetric) {
		// add relation elements
		relation = relation.replaceAll("(\\w+\\[|\\]|\\{|\\}|\\(|\\))", "");
		for (int i = 0; i < fromstructs.size(); i++) {
			String o1id = fromstructs.get(i).getAttributeValue("id");
			String o2id = "";
			boolean negation = false;
			for (int j = 0; j < tostructs.size(); j++) {
				if (relation.compareTo("between") == 0)
					o2id += tostructs.get(j).getAttributeValue("id") + " ";
				else
					o2id = tostructs.get(j).getAttributeValue("id");
				if (modifier.matches(".*?\\b(" + this.negationpt + ")\\b.*")) {
					negation = true;
					modifier = modifier.replaceFirst(".*?\\b(" + this.negationpt + ")\\b", "").trim();
				}
				if (relation.matches(".*?\\b(" + this.negationpt + ")\\b.*")) {
					negation = true;
					relation = relation.replaceFirst(".*?\\b(" + this.negationpt + ")\\b", "").trim();
				}
				if (relation.compareTo("between") != 0) {
					addRelation(relation, modifier, symmetric, o1id, o2id, negation, "based_on_text");
				}
			}
			if (relation.compareTo("between") == 0) {
				addRelation(relation, modifier, symmetric, o1id, o2id.trim(), negation, "based_on_text");
			}
		}
		// add other relations as a constraint to the structure: apex of leaves
		// {rounded}.
		// expect some character elements in the structure element.
		// if not, in post-processing, remove the constraint
		/*
		 * if(relation.compareTo("consists of")!=0){ String constraint =
		 * relation+" "; for(int j = 0; j<this.lateststructures.size(); j++){
		 * constraint +=
		 * this.lateststructures.get(j).getAttributeValue("name")+", "; //organ
		 * name list } constraint.trim().replaceFirst("\\s*,$", ""); for(int i =
		 * 0; i<latests.size(); i++){ addAttribute(latests.get(i), "constraint",
		 * constraint); //base, of leaves, petals; apex, of leaves, petals } }
		 */
	}

	private void addRelation(String relation, String modifier, boolean symmetric, String o1id, String o2id, boolean negation, String inferencemethod) {
		Element rela = new Element("relation");
		if (this.inbrackets) {
			rela.setAttribute("in_bracket", "true");
		}
		rela.setAttribute("id", "r" + this.relationid);
		this.relationid++;
		rela.setAttribute("name", relation);
		rela.setAttribute("from", o1id);
		rela.setAttribute("to", o2id);
		rela.setAttribute("negation", negation + "");
		// rela.setAttribute("symmetric", symmetric+"");
		// rela.setAttribute("inference_method", inferencemethod);
		// if(modifier.length()>0 && modifier.indexOf("m[")>=0){
		if (modifier.length() > 0) {
			addAttribute(rela, "modifier", modifier.replaceAll("m\\[|\\]", ""));
		}
		addClauseModifierConstraint(cs, rela);
		// this.statement.addContent(rela); //add to statement
		addContent(this.statement, rela);
	}

	/**
	 * 
	 * @param pp
	 * @param latests
	 * @param lateststructures2
	 * @return
	 */
	private String relationLabel(String pp, ArrayList<Element> organsbeforepp, ArrayList<Element> organsafterpp) {
		if (pp.compareTo("of") == 0) {
			return differentiateOf(organsbeforepp, organsafterpp);
		}
		return pp;
	}

	private void addAttribute(Element e, String attribute, String value) {
		if(attribute.compareTo("modifier")==0) value = value.replaceAll("-", " ");
		value = value.replaceAll("(\\w+\\[|\\]|\\{|\\}|\\(|\\))", "").replaceAll("\\s+;\\s+", ";").replaceAll("\\[", "").trim();
		if (value.indexOf("LRB-") > 0)
			value = NumericalHandler.originalNumForm(value);
		value = value.replaceAll("\\b(" + this.notInModifier + ")\\b", "").trim();
		if (this.evaluation && attribute.startsWith("constraint_"))
			attribute = "constraint";
		if (value.length() > 0) {
			if (value.indexOf("moreorless") >= 0) {
				value = value.replaceAll("moreorless", "more or less");
			}
			value = value.replaceAll(" , ", ", ").trim();
			String v = e.getAttributeValue(attribute);
			if (v == null || !v.matches(".*?(^|; )" + value + "(;|$).*")) {
				if (v != null && v.trim().length() > 0) {
					v = v.trim() + ";" + value;
				} else {
					v = value;
				}
				if (attribute.equals("constraintid"))
					v = v.replaceAll("\\W", " "); // IDREFS are space-separated
				v = v.replaceAll("\\s+", " ").trim();
				e.setAttribute(attribute, v);
			}
		}
	}

	/**
	 * 
	 * @param organs
	 * @param organs2
	 * @return part-of or consists-of
	 * 
	 *         involucre of => consists of
	 */
	private String differentiateOf(ArrayList<Element> organsbeforeOf, ArrayList<Element> organsafterOf) {
		String result = "part_of";
		try {
			Statement stmt = conn.createStatement();

			for (int i = 0; i < organsbeforeOf.size(); i++) {
				String b = organsbeforeOf.get(i).getAttributeValue("name");
				String pb = organsbeforeOf.get(i).getAttributeValue("name_original");
				if(pb.length()==0) pb = b;
				if (b.matches("(" + ChunkedSentence.pairs + "|" + ChunkedSentence.clusters + ")")) {
					// z[{2} (pairs)] r[p[of] o[(uroneural) (bones)]]
					// 2 was marked as the count from the organsbeforeOf
					List<Element> c = StanfordParser.path8.selectNodes(organsbeforeOf.get(i));
					if (c.size() > 0) {
						// append "pair(s)" to count value, then move counts to
						// organsafterOf
						countPairs(c, organsafterOf, organsbeforeOf.get(i));
						result = null;
						break;
					} else {
						result = "consist_of";
					}
					break;
				}
				for (int j = 0; j < organsafterOf.size(); j++) {
					String a = organsafterOf.get(j).getAttributeValue("name");
					String pa = organsafterOf.get(j).getAttributeValue("name_original");
					if(pa.length()==0) pa = a;
					// String pattern = a+"[ ]+of[ ]+[0-9]+.*"+b+"[,\\.]";
					// //consists-of
					if (a.length() > 0 && b.length() > 0) {
						String pattern = "(" + b + "|" + pb + ")" + "[ ]+of[ ]+[0-9]+.*" + "(" + a + "|" + pa + ")" + "[ ]?(,|;|\\.|and|or|plus)"; // consists-of
						String query = "select * from " + this.tableprefix + "_sentence where sentence rlike '" + pattern + "'";
						ResultSet rs = stmt.executeQuery(query);
						if (rs.next()) {
							result = "consist_of";
							break;
						}
						rs.close();
					}
				}
			}
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * append "pair(s)" to count value from organsbeforeof, then move counts to
	 * organsafterOf
	 * 
	 * @param countCharas
	 * @param organsafterOf
	 * @param elementbeforeOf
	 */

	private void countPairs(List<Element> countCharas, ArrayList<Element> organsafterOf, Element elementbeforeOf) {
		String pair = elementbeforeOf.getAttributeValue("name");
		String pairid = elementbeforeOf.getAttributeValue("id");
		String organsafterOfids = "";
		if (elementbeforeOf.getAttribute("constraint") != null)
			pair = elementbeforeOf.getAttributeValue("constraint") + " " + pair;
		Element parentOfCount = countCharas.get(0).getParentElement();
		int totalchildren = parentOfCount.getChildren().size();
		Iterator<Element> it = countCharas.iterator();
		int movedChildren = 0;
		while (it.hasNext()) {
			String thispair = pair; // use thispair to avoid multiple "s" be
									// added to counts in the loop
			Element count = it.next();
			if (count.getAttribute("value") != null) {
				String ct = count.getAttributeValue("value");
				thispair = ct.matches("a|an|one|1|single") ? thispair : thispair + "s";
				count.setAttribute("value", ct + " " + thispair);
			}
			count.detach();
			movedChildren++;
			Iterator<Element> et = organsafterOf.iterator();
			while (et.hasNext()) {
				Element e = et.next();
				addContent(e, count);
				organsafterOfids += e.getAttributeValue("id") + " ";
			}
		}
		if (totalchildren == movedChildren)
			parentOfCount.detach();

		// because "pair" is treated as a count and not an organ, if its ids are
		// used in any constraintid and in relations
		// these ids need to be changed to the ids of organsafterOf
		organsafterOfids = organsafterOfids.trim();
		try {
			List<Element> elements = XPath.selectNodes(this.statement, ".//character[@constraintid='" + pairid + "']");
			for (Element c : elements) {
				c.setAttribute("constraintid", organsafterOfids);
			}
			elements = XPath.selectNodes(this.statement, ".//relation[@to='" + pairid + "']");
			for (Element r : elements) {
				r.setAttribute("to", organsafterOfids);
			}
			// structure "pair" no longer there, so drop any relations from
			// pair.
			elements = XPath.selectNodes(this.statement, ".//relation[@from='" + pairid + "']");
			for (Element r : elements) {
				r.detach();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * separate o[......... {m} {m} (o1) and {m} (o2)] to two parts: the last
	 * part include all organ names also handles cases such as o[the {frontal}
	 * and the (sphenotic) ({spine})]
	 * 
	 * @param object
	 * @return
	 */
	private String[] separate(String object) {
		String[] twoparts = new String[2];
		object = object.replaceFirst("^o\\[", "").replaceFirst("\\]$", "").replaceAll("<", "(").replaceAll(">", ")");
		String part2 = "";
		if (object.indexOf("(") >= 0) {
			part2 = object.substring(object.indexOf("(")).trim();
		} else if (object.lastIndexOf(" ") >= 0) {// take the last word as an
													// organ and part2
			part2 = object.substring(object.lastIndexOf(" ")).trim();
		} else {
			part2 = object;
		}
		String part1 = object.replace(part2, "").trim();
		if (part1.length() > 0) {
			// part 1 may still have modifiers of the first organ in part 2, fix
			// this.
			String[] ws1 = part1.split("\\s+");
			String[] ws2 = part2.split("\\s+");
			String o = "";
			for (int i = 0; i < ws2.length; i++) {
				if (ws2[i].indexOf("(") >= 0) {
					o += ws2[i] + " ";
				} else {
					break;
				}
			}
			o = o.trim();
			for (int i = ws1.length - 1; i >= 0; i--) {
				String escaped = ws1[i].replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}");
				if (constraintType(ws1[i].replaceAll("\\W", ""), o) != null) {
					part1 = part1.replaceFirst("\\s*" + escaped + "$", "");
					part2 = ws1[i] + " " + part2;
				} else {
					break;
				}
			}
			part1 = part1.replaceAll("\\s+", " ").trim();
			part2 = part2.replaceAll("\\s+", " ").trim();
		}
		twoparts[0] = part1;
		twoparts[1] = part2;
		return twoparts;
	}

	/**
	 * @param object
	 *            : o[the {frontal} and the (sphenotic) ({spine})]
	 * @return o[the {frontal} ({spine}) and the (sphenotic) ({spine})]
	 */
	private String normalizeSharedOrganObject(String object) {
		// TODO Auto-generated method stub
		// if(object.matches(".*?\\b(and|or)\\b.*")){//a conic or subcylindric
		// lateral extremity is not a case of shared object
		if (object.matches(".*?\\band\\b.*")) {
			String norm = "";
			String[] segs = object.split("\\s+");
			String lastN = segs[segs.length - 1].replaceAll("\\]+$", "").trim();
			if (object.matches(".*?\\b(and|or)\\s+" + lastN.replaceFirst("\\(", "\\\\(").replaceFirst("\\)", "\\\\)").replaceFirst("\\{", "\\\\{").replaceFirst("\\}", "\\\\}")
					+ ".*")) {
				String lastTokenBeforeAnd = segs[segs.length - 3].replaceFirst("\\{", "(").replaceFirst("\\}", ")");
				segs[segs.length - 3] = lastTokenBeforeAnd; // o[the {quadrate}
															// and
															// (hyomandibula)]
			}
			for (int i = segs.length - 1; i >= 0; i--) {
				norm = segs[i] + " " + norm;
				// if(segs[i].matches("(,|and|or)") &&
				// !segs[i-1].contains("(")){
				if (segs[i].matches("(,|and)") && !segs[i - 1].contains("(")) {
					norm = lastN + " " + norm;
				}
				// if(segs[i].matches("(,|and|or)") && segs[i-1].contains("(")){
				if (segs[i].matches("(,|and)") && segs[i - 1].contains("(")) {
					lastN = segs[i - 1].trim();
				}
			}
			return norm.trim();
		}

		return object;
	}

	/**
	 * TODO: flower and leaf blades???
	 * 
	 * @param ck
	 *            : {} (), {} (), () and/or ()
	 * @return
	 */
	private ArrayList<Element> createStructureElements(String listofstructures, boolean aftercomma/*
																			 * ,
																			 * boolean
																			 * makeconstraint
																			 */) {
		ArrayList<Element> results = new ArrayList<Element>();
		if (listofstructures.startsWith("l[")) {
			listofstructures = listofstructures.replaceFirst("^l\\[", "").replaceFirst("\\]$", "");
		}
		// special case: pronouns like them: resolve to the last structure
		if (listofstructures.matches(".*?\\b(" + ChunkedSentence.pronouns + ")\\b.*") && listofstructures.indexOf(" ") < 0) {
			for (int e = this.elementlog.size() - 1; e >= 0; e--) {
				Element el = elementlog.get(e);
				if (el.getName().compareTo("structure") == 0) {
					results.add(el);
					return results;
				}
			}
		}

		// String[] organs = listofstructures.replaceAll(" (and|or|plus) ",
		// " , ").split("\\)\\s*,\\s*"); //TODO: flower and leaf blades???
		String[] organs = listofstructures.replaceAll(",", " , ").split("\\)\\s+(and|or|plus|,)\\s+"); 
		//Added by Hari
		for(int i=0;i<organs.length-1;i++)
			organs[i]=organs[i].trim()+")";
		// TODO: flower and leaf blades???

		// mohan 28/10/2011. If the first organ is a preposition then join the
		// preposition with the following organ
		for (int i = 0; i < organs.length; i++) {
			if (organs[i].matches("\\{r\\[p\\[.*\\]\\]\\}\\s+\\{.*\\}\\s+.*")) {
				organs[i] = organs[i].replaceAll("\\]\\]\\}\\s\\{", "]]}-{");
			}
		}
		
		String[] sharedcharacters = null;
		for (int i = 0; i < organs.length; i++) {
			String[] organ = organs[i].trim().split("\\s+");
			// for each organ mentioned, find organ name
			String o = "";
			int j = 0;
			for (j = organ.length - 1; j >= 0; j--) {
				// if(organ[j].startsWith("(")){ //(spine tip)
				/*
				 * if(organ[j].endsWith(")") || organ[j].startsWith("(")){
				 * //(spine tip) o = organ[j]+" "+o; organ[j] = ""; }else{
				 * break; }
				 */
				if (organ[j].endsWith(")") || organ[j].startsWith("(")) { // (spine
																			// tip)
					o = organ[j] + " " + o;
					organ[j] = "";
					break; // take the last organ name
				}
			}
			o = o.replaceAll("(\\w\\[|\\]|\\(|\\)|\\}|\\{)", "").trim();
			if (o.length() == 0)
				return results;
			// create element,
			Element e = new Element("structure");
			if (this.inbrackets) {
				e.setAttribute("in_bracket", "true");
			}
			String strid = "o" + this.structid;
			this.structid++;
			e.setAttribute("id", strid);
			// e.setAttribute("name", o.trim()); //must have.
			o = o.trim();
			if(o.indexOf("_")>0) {
				//make sure "_" is used only before the indexes, not btw words of a phrase
				if(o.matches("(.*?_[\\divx]+)|(.*?_[\\divx]+-[\\divx]+)")){
					//handle abc_i-iii, abc_2_to_5, abc_3_and_5, abc_3,4-5... 
					e.setAttribute("type","multi");
					e.setAttribute("name", adjustUnderscore(o));//make sure "_" is used only before the indexes, not btw words of a phrase
					e.setAttribute("name_original", o.replaceAll("_", " "));
				}else{
					if(isPrematched(o)){
						e.setAttribute("name", o.replaceAll("_", " ").trim()); //prematched phrases from uberon
						e.setAttribute("name_original", o.replaceAll("_", " ").trim());
					}
					else{
						e.setAttribute("name", o); //originally hyphenated phrases such as pubis_ischium
						e.setAttribute("name_original", o);
					}
				}
			}else{
				e.setAttribute("name_original", o); //add structure name as the original text
				e.setAttribute("name", TermOutputerUtilities.toSingular(o));
			}
			
			//if e appears right after a comma
			if(aftercomma){
				e.setAttribute("after_comma", "true");
				if(debugextraattributes) System.out.println("after_comma:"+e.getAttributeValue("name"));
			}
			
			//Changed by Zilong
			//if(o.trim().matches("(.*?_[\\divx]+)|(.*?_[\\divx]+-[\\divx]+)")){
			//handle abc_i-iii, abc_2_to_5, abc_3_and_5, abc_3,4-5... 
			//	e.setAttribute("type","multi");
			//}
			//Changed by Zilong End
			
			// must have.
			//corolla lobes
			addContent(this.statement, e);
			results.add(e); // results only adds e

			// determine constraints
			while (j >= 0 && organ[j].trim().length() == 0) {
				j--;
			}
			// cauline leaf abaxial surface trichmode hair long
			boolean terminate = false;
			boolean distribute = false;
			String constraint = "";// plain
			for (; j >= 0; j--) {
				if (terminate)
					break;

				String w = organ[j].replaceAll("(\\w+\\[|\\]|\\{\\(|\\)\\}|\\(\\{|\\}\\))", "");
				// mohan code to make w keep all the tags for a preposition
				// chunk
				if (organ[j].matches("\\{?r\\[p\\[.*")) {
					w = organ[j];
				}
				// end mohan code//
				if (w.equals(",")) {
					distribute = true;
					continue;
				}
				String type = null;
				if (organ[j].startsWith("(") || w.endsWith(")"))
					type = "parent_organ";
				else
					type = constraintType(w, o);
				if (type != null) {
					organ[j] = "";
					constraint = w + " " + constraint; // plain
				} else {
					break;
				}
			}
			j++;

			if (constraint.trim().length() > 0) {
				addAttribute(e, "constraint", constraint.replaceAll("(\\(|\\))", "").trim()); // may
																								// not
																								// have.
			}

			// determine character/modifier
			ArrayList<Element> list = new ArrayList<Element>();
			list.add(e);
			// process text reminding in organ
			if (organ[0].trim().length() > 0) {// has c/m remains, may be shared
												// by later organs
				sharedcharacters = organ;
			} else if (sharedcharacters != null) {// share c/m from a previous
													// organ
				organ = sharedcharacters;
			}
			processCharacterText(organ, list, null, true); // characters created here
														// are final and all the
														// structures will have,
														// therefore they shall
														// stay local and not
														// visible from outside

		}
		return results;
	}

	private boolean isPrematched(String o) {
		Statement stmt =null;
		ResultSet rs =null;
		try {
			// collect life_style terms
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select distinct term from " + this.glosstable + " where term ='"+o+"'");
			if (rs.next()) {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			try{
				if(rs!=null) rs.close();
				if(stmt!=null) stmt.close();
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}

	/**
	 * thoracic_vertebra_8
	 * @param o a phrase with underscores connecting each token, for example 'thoracic_vertebra_8 and_9', 'thoracic_vertebra_i-iii'
	 * @return a phrase with underscores connecting the word part and the index, for example 'thoracic vertebra_8', 'thoracic vertebra_i-iii'
	 */
	private String adjustUnderscore(String o) {		
		return o.replaceAll("_(?![\\divx]+)", " ").trim();
	}

	/**
	 * cauline leaf abaxial surface thin trichomode hair constraint_type:
	 * trichomode constraint_parent_organ: cauline leaf abaxial surface
	 * 
	 * @param fromid
	 *            : from_id
	 * @param relation
	 *            : "part_of"
	 * @param toorganname
	 *            : use this to find the to_id
	 */
	private void driveRelationFromStructrueContraint(String fromid, String relation, String toorganname) {
		try {
			// try to link toorganname to an previously mentioned organ
			List<Element> structures = StanfordParser.path7.selectNodes(this.statement);
			Iterator<Element> it = structures.iterator();
			boolean exist = false;
			while (it.hasNext()) {
				Element structure = it.next();
				String name = structure.getAttributeValue("name");
				if (structure.getAttribute("constraint_type") != null) {
					String tokens = structure.getAttributeValue("constraint_type"); // need
																					// to
																					// reverse
																					// order
					tokens = reversed(tokens);
					name = tokens + " " + name;
				}
				if (structure.getAttribute("constraint_parent_organ") != null) {
					name = structure.getAttributeValue("constraint_parent_organ") + " " + name;
				}
				if (structure.getAttribute("constraint") != null) {
					name = structure.getAttributeValue("constraint") + " " + name;
				}
				if (name.equals(toorganname)) {
					exist = true;
					String toid = structure.getAttributeValue("id");
					addRelation(relation, "", false, fromid, toid, false, "based_on_parent_organ_constraint");
					break;
				}
			}
			if (!exist) { // create a new structure
				addRelation(relation, "", false, fromid, "o" + this.structid, false, "based_on_parent_organ_constraint");
				toorganname = toorganname.replaceFirst(" (?=\\w+$)", " (") + ")"; // format
																					// organname
				if (toorganname.indexOf('(') < 0)
					toorganname = "(" + toorganname;
				this.createStructureElements(toorganname, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * turn "b;a" to "a b"
	 * 
	 * @param tokens
	 * @return
	 */
	private String reversed(String tokens) {
		String[] ts = tokens.split("\\s*;\\s*");
		String result = "";
		for (int i = ts.length - 1; i >= 0; i--) {
			result += i + " ";
		}
		return result.trim();
	}

	/**
	 * bases and tips mostly rounded
	 * 
	 * @param tokens
	 * @param parents
	 */
	private ArrayList<Element> processCharacterText(String[] tokens, ArrayList<Element> parents, String character, boolean characterismodifier) {
		ArrayList<Element> results = new ArrayList<Element>();
		// determine characters and modifiers
		String modifiers = "";
		for (int j = 0; j < tokens.length; j++) {
			if (tokens[j].trim().length() > 0) {
				tokens[j] = NumericalHandler.originalNumForm(tokens[j]);
				if (tokens[j].indexOf("~list~") >= 0) {
					results = this.processCharacterList(tokens[j], parents, characterismodifier);
				} else {
					String w = tokens[j];
					String chara = null;
					if (tokens[j].matches("\\w{2,}\\[.*")) {
						chara = tokens[j].substring(0, tokens[j].indexOf('['));
						w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
					} else if (tokens[j].matches("\\w\\[.*")) {
						w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
					}
					w = w.replaceAll("(\\{|\\})", "");
					chara = Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, glosstable, tableprefix);
					if (chara == null && w.matches("no")) {
						chara = "presence";
					}
					if (chara == null && Utilities.isAdv(w, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)) {// TODO:
																													// can
																													// be
																													// made
																													// more
																													// efficient,
																													// since
																													// sometimes
																													// character
																													// is
																													// already
																													// given
						modifiers += w + " ";
					} else if (w.matches(".*?\\d.*") && !w.matches(".*?[a-z].*")) {// TODO:
																					// 2
																					// times
																					// =>2-times?
						results = this.annotateNumericals(w, "count", modifiers, parents, false, characterismodifier);
						modifiers = "";
					} else {
						// String chara = MyPOSTagger.characterhash.get(w);
						if (chara != null) {
							if (character != null) {
								chara = character;
							}
							if (chara.compareToIgnoreCase("character") == 0 && modifiers.length() == 0) {// high
																											// relief:
																											// character=relief,
																											// reset
																											// the
																											// character
																											// of
																											// "high"
																											// to
																											// "relief"
								Element lastelement = null;
								if (results.size() >= 1) {
									lastelement = results.get(results.size() - 1);
								} else if (this.latestelements.size() >= 1) {
									lastelement = this.latestelements.get(this.latestelements.size() - 1);
								}
								if (lastelement != null && lastelement.getName().compareTo("character") == 0) {
									lastelement.setAttribute("name", w);
									/*
									 * Iterator<Element> it =
									 * this.latestelements.iterator();
									 * while(it.hasNext()){ lastelement =
									 * it.next();
									 * lastelement.setAttribute("name", w); }
									 */
								}
							} else {
								createCharacterElement(parents, results, modifiers, w, chara, "", characterismodifier); // default
																									// type
																									// ""
																									// =
																									// individual
																									// vaues
								modifiers = "";
							}
						}
					}
				}

			}
		}
		return results;
	}

	private String createCharacterElement(ArrayList<Element> parents, ArrayList<Element> results, String modifiers, String cvalue, String cname, String char_type, boolean characterismodifier) {
		Element character = new Element("character");
		if (this.inbrackets) {
			character.setAttribute("in_bracket", "true");
		}
		if(characterismodifier){
			character.setAttribute("is_modifier", "true");
			if(debugextraattributes) System.out.println("is modifier:"+cvalue);
		}
		if (cname.compareTo("count") == 0 && cvalue.indexOf("-") >= 0 && cvalue.indexOf("-") == cvalue.lastIndexOf("-")) {
			String[] values = cvalue.split("-");
			character.setAttribute("char_type", "range_value");
			character.setAttribute("name", cname);
			character.setAttribute("from", values[0]);
			character.setAttribute("to", values[1]);
		} else {
			if (cname.compareTo("size") == 0) {
				String value = cvalue.replaceFirst("\\b(" + ChunkedSentence.units + ")\\b", "").trim(); // 5-10
																										// mm
				String unit = cvalue.replace(value, "").trim();
				if (unit.length() > 0) {
					character.setAttribute("unit", unit);
				}
				cvalue = value;
			} else if (cvalue.indexOf("-c-") >= 0 && (cname.compareTo("color") == 0 || cname.compareTo("coloration") == 0)) {// -c-
																																// set
																																// in
																																// SentenceOrganStateMarkup
				String color = cvalue.substring(cvalue.lastIndexOf("-c-") + 3); // pale-blue
				String m = cvalue.substring(0, cvalue.lastIndexOf("-c-")); // color
																			// =
																			// blue
																			// m=pale
				modifiers = modifiers.length() > 0 ? modifiers + ";" + m : m;
				cvalue = color;
			}
			if (char_type.length() > 0) {
				character.setAttribute("char_type", char_type);
			}
			character.setAttribute("name", cname);
			character.setAttribute("value", cvalue);
		}
		boolean usedm = false;
		Iterator<Element> it = parents.iterator();
		while (it.hasNext()) {
			Element e = it.next();
			character = (Element) character.clone();
			if (modifiers.trim().length() > 0) {
				addAttribute(character, "modifier", modifiers.trim()); // may
																		// not
																		// have
				usedm = true;
			}
			results.add(character); // add to results
			// e.addContent(character);//add to e
			addContent(e, character);
		}
		if (usedm) {
			modifiers = "";
		}
		addClauseModifierConstraint(cs, character);
		return modifiers;
	}

	/**
	 * 
	 * @param parents
	 * @param w
	 *            : m[usually] 0
	 * @param modifiers
	 * @return
	 */
	private ArrayList<Element> annotateCount(ArrayList<Element> parents, String w, String modifiers) {
		// TODO Auto-generated method stub
		String modifier = w.replaceFirst("\\d.*", "").trim();
		String number = w.replace(modifier, "").trim();
		ArrayList<Element> e = new ArrayList<Element>();
		Element count = new Element("character");
		if (this.inbrackets) {
			count.setAttribute("in_bracket", "true");
		}
		count.setAttribute("name", "count");
		count.setAttribute("value", number);
		if (modifiers.length() > 0) {
			this.addAttribute(count, "modifier", modifiers);
		}
		if (modifier.length() > 0) {
			this.addAttribute(count, "modifier", modifier.replaceAll("(m\\[|\\])", ""));
		}
		Iterator<Element> it = parents.iterator();
		while (it.hasNext()) {
			count = (Element) count.clone();
			e.add(count);
			// it.next().addContent(count);
			addContent(it.next(), count);
		}
		addClauseModifierConstraint(cs, count);
		return e;
	}

	// if w has been seen used as a modifier to organ o
	private String constraintType(String w, String o) {
		String result = null;
		// mohan code to make w keep all the tags for a preposition chunk
		if (w.matches("\\{?r\\[p\\[.*"))// for cases such as
										// "with the head in full face view, the midpoint blah blah....",
										// "r[p[with head] {in-fullface-view}]"
										// is treated as a "condition"
										// constraint
		{
			return "condition";
		}
		// mohan code ends.

		// w = w.replaceAll("\\W", ""); //don't turn frontal-postorbital to
		// frontalpostorbital
		String ch = Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, this.glosstable, tableprefix);
		if (ch != null && ch.matches(".*?_?(position|insertion|structure_type|life_stage|functionality)_?.*") && w.compareTo("low") != 0)
			return "type";
		String sw = TermOutputerUtilities.toSingular(w);
		try {
			Statement stmt = conn.createStatement();
			// Nov 30th 2011. Considered to use glossary, term_category,
			// wordroles to replace sentence markup evidence. For some
			// collections (e.g. phenotype test) sentence markup is not reliable
			ResultSet rs = stmt.executeQuery("select * from " + this.tableprefix + "_sentence where tag = '" + w + "' or tag='" + sw + "'");
			if (rs.next()) {
				return "parent_organ";
			}
			// rs =
			// stmt.executeQuery("select * from "+this.tableprefix+"_sentence where modifier = '"+w+"' or modifier like '"+w+" %' or modifier like '% "+w+" %' or modifier like '% "+w+"'");
			rs = stmt.executeQuery("select * from " + this.tableprefix + "_sentence where modifier = '" + w + "'");
			if (rs.next()) {
				return "type";
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * output annotated sentence in XML format Chunk types: PrepChunk,
	 * IVerbChunk (Intransitive verb chunk, followed by a preposition),
	 * VerbChunk, ADJChunk, SBARChunk, etc.
	 * 
	 * @return
	 */
	/*
	 * public Element annotate() throws Exception{ //query the sentence database
	 * for the tag/modifier for this sentence, using this.sentsrc //also use the
	 * substructure table to resolve of-clauses ArrayList<Chunk> chunks = new
	 * ArrayList(); ArrayList<String> structureIDs = new ArrayList();
	 * 
	 * this.currentsubject = "fetch from sentence table"; String modifier =
	 * "fetch from sentence table"; this.currentmainstructure =
	 * createStructureElement(this.currentsubject, modifier, this.structid++);
	 * while(cs.hasNext()){ Chunk chunk = cs.nextChunk(); chunks.add(chunk);
	 * if(chunk instanceof Organ){ String organ = chunk.getText();
	 * if(chunks.size() == 1){ continue; //this is current subject read from the
	 * sentence table }else{
	 * this.statement.addContent(this.currentmainstructure); //add this
	 * completed structure //create a new structure element } }else if(chunk
	 * instanceof PrepChunk){ String pphrase = chunk.getText(); int chunkid =
	 * cs.getPointer() - 1; Element thiselement =
	 * (Element)XPath.selectSingleNode(root, ".\\*[id='"+chunkid+"']"); //IN
	 * String relationname = thiselement.getAttributeValue("text"); //create
	 * structure(s) from the NPs. e.g "3 florets", character/modifier before
	 * organnames //NP may be a list of NPs String np =
	 * pphrase.replaceFirst("^"+relationname, "").trim(); ArrayList oids =
	 * annotateNP(np); //in which <structure> may be created and inserted into
	 * the <statement> if(chunks.get(chunks.size()-2) instanceof Organ){ //apex
	 * of leaves //create a relation Element relation =
	 * createRelationElement(this.relationid++); }else{ //create a constraint
	 * for the last character } }else if(chunk instanceof SBARChunk){
	 * //SBARChunk could follow any xyzChunk
	 * 
	 * 
	 * }else if(chunk instanceof SimpleCharacterState){ //check for its
	 * character //associate it with current subject if(this.currentsubject
	 * ==null){ //save this as a constraint for the to-be-discovered subject } }
	 * }
	 * 
	 * return statement;
	 * 
	 * }
	 */

	public void setInBrackets(boolean b) {
		this.inbrackets = b;
	}

	/**
	 * 
	 * @param measurements
	 *            : CI 72 - 75 (74 ), SI 99 - 108 (102 ), PeNI 73 - 83 (73 ),
	 *            LPeI 46 - 53 (46 ), DPeI 135 - 155 (145 ).
	 */
	private void annotatedMeasurements(String measurements) {
		measurements = measurements.replaceAll("–", "-");
		Element whole = new Element("whole_organism");
		// this.statement.addContent(whole);
		addContent(this.statement, whole);
		ArrayList<Element> parent = new ArrayList<Element>();
		parent.add(whole);
		// select delimitor
		int comma = measurements.replaceAll("[^,]", "").length();
		int semi = measurements.replaceAll("[^;]", "").length();
		String del = comma > semi ? "," : ";";
		String[] values = measurements.split(del);
		for (int i = 0; i < values.length; i++) {
			String value = values[i].replaceFirst("[,;\\.]\\s*$", "");
			// separate char from values
			String chara = value.replaceFirst("\\s+\\d.*", "");
			String vstring = value.replaceFirst("^" + chara, "").trim();
			// seperate modifiers from vlu in case there is any
			String vlu = vstring.replaceFirst("\\s+[a-zA-Z].*", "").trim();
			String modifier = vstring.substring(vlu.length()).trim();
			modifier = modifier.length() > 0 ? "m[" + modifier + "]" : null;
			vlu = vlu.replaceAll("(?<=\\d)\\s*\\.\\s*(?=\\d)", ".");
			this.annotateNumericals(vlu.trim(), chara.trim(), modifier, parent, false, false);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
