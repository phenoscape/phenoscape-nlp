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
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.*;


/**
 * @author hongcui
 * fnaglossaryfixed: move verbs such as comprising from the glossary
 *
 */

@SuppressWarnings({ "unchecked", "unused","static-access" })

public class CharacterAnnotatorChunked {
	private Element statement = null;
	private ChunkedSentence cs = null;
	private static ArrayList<Element> subjects = new ArrayList<Element>();//static so a ditto sent can see the last subject
	private ArrayList<Element> latestelements = new ArrayList<Element>();//save the last set of elements added. independent from adding elements to <Statement>
	private String delims = "comma|or";
	private static int structid = 1;
	private static int relationid = 1;
	private String unassignedcharacter = null;
	//private String unassignedmodifiers = null; //holds modifiers that may be applied to the next chunk
	protected Connection conn = null;
	private String tableprefix = null;
	private String glosstable = null;
	private boolean inbrackets = false;
	private String text  = null;
	private String notInModifier = "a|an|the";
	private String lifestyle = "";
	private String characters = "";
	private boolean partofinference = false;
	private ArrayList<Element> pstructures = new ArrayList<Element>();
	private ArrayList<Element> cstructures = new ArrayList<Element>();
	private boolean attachToLast = false; //this switch controls where a character will be attached to. "true": attach to last organ seen. "false":attach to the subject of a clause
	private boolean printAnnotation = true;
	private boolean debugNum = false;
	private boolean printComma = false;
	private boolean printAttach = false;
	private boolean evaluation = true;
	private String sentsrc;
	private boolean nosubject;

	
	/**
	 * 
	 */
	public CharacterAnnotatorChunked(Connection conn, String tableprefix, String glosstable, boolean evaluation) {
		this.conn = conn;
		this.tableprefix = tableprefix;
		this.glosstable = glosstable;
		this.evaluation = evaluation;
		this.nosubject = false;
		if(this.evaluation) this.partofinference = false; //partofinterference causes huge number of "relations"
		try{
			//collect life_style terms
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct term from "+this.glosstable+ " where category='life_style'");
			while(rs.next()){
				this.lifestyle += rs.getString(1)+"|";
			}
			this.lifestyle = lifestyle.replaceFirst("\\|$", "");
			
			rs = stmt.executeQuery("select distinct term from "+this.glosstable+ " where category='character'");
			while(rs.next()){
				this.characters += rs.getString(1)+"|";
			}
			this.characters = characters.replaceFirst("\\|$", "");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * reset annotator to process next description paragraph.
	 */
	public void reset(){
		this.subjects = new ArrayList<Element>();//static so a ditto sent can see the last subject
		this.latestelements = new ArrayList<Element>();//save the last set of elements added. independent from adding elements to <Statement>
		this.unassignedcharacter = null;
		this.inbrackets = false;
		this.pstructures = new ArrayList<Element>();
		this.cstructures = new ArrayList<Element>();
		this.nosubject = false;

	}
	public Element annotate(String sentindex, String sentsrc, ChunkedSentence cs) throws Exception{
		this.statement = new Element("statement");
		this.statement.setAttribute("id", sentindex);
		this.cs = cs;
		this.text = cs.getText();
		this.sentsrc = sentsrc;
		Element text = new Element("text");//make <text> the first element in statement
		text.addContent(this.text);
		if(!this.evaluation) this.statement.addContent(text);
		String subject= cs.getSubjectText();
		if(subject==null && cs.getPointer()==0){
			this.nosubject = true;
			annotateByChunk(cs, false);
		}else if(subject.equals("measurements")){
			this.annotatedMeasurements(this.text);
		}else if(!subject.equals("ignore")){
			if(subject.equals("ditto")){
				reestablishSubject();	
			}else{
				establishSubject(subject);
				if(this.partofinference){
					this.pstructures.addAll(CharacterAnnotatorChunked.subjects);
				}
			}
			cs.setInSegment(true);
			cs.setRightAfterSubject(true);
			annotateByChunk(cs, false);
		}
		
		lifeStyle();
		if(!this.evaluation) mayBeSameRelation();
		if(this.partofinference){
			puncBasedPartOfRelation();
		}
		
		XMLOutputter xo = new XMLOutputter(Format.getPrettyFormat());
		if(printAnnotation){
			System.out.println();
			System.out.println(xo.outputString(this.statement));
		}
		return this.statement;
	}

	/**
	 * assuming subject organs of subsentences in a sentence are parts of the subject organ of the sentence
	 * this assumption seemed hold for FNA data.
	 */
	private void puncBasedPartOfRelation() {
		for(int p = 0; p < this.pstructures.size(); p++){
			for(int c = 0; c < this.cstructures.size(); c++){
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
		try{			
			//find life_style structures
			List<Element> structures = XPath.selectNodes(this.statement, ".//structure");
			Iterator<Element> it = structures.iterator();
			//Element structure = null;
			while(it.hasNext()){
				Element structure = it.next();
				String name = structure.getAttributeValue("name").trim();
				if(name.length()<=0) continue;
				if(lifestyle.matches(".*\\b"+name+"\\b.*")){
					if(structure.getAttribute("constraint_type") !=null)
						name = structure.getAttributeValue("constraint_type")+" "+name;
					if(structure.getAttribute("constraint_parent_organ") !=null)
						name = structure.getAttributeValue("constraint_parent_organ")+" "+name;
					Element wo = (Element)XPath.selectSingleNode(this.statement, ".//structure[@name='whole_organism']");
					if(wo!=null){
						List<Element> content = structure.getContent();
						structure.removeContent();
						/*for(int i = 0; i<content.size(); i++){
							Element e = content.get(i);
							e.detach();
							content.set(i, e);
						}*/
						wo.addContent(content);
						structure.detach();
						structure = wo;
					}
					structure.setAttribute("name", "whole_organism");
					Element ch = new Element("character");
					ch.setAttribute("name", "life_style");
					ch.setAttribute("value", name);
					structure.addContent(ch);
				}
				//keep each life_style structure
				/*if(lifestyle.matches(".*\\b"+name+"\\b.*")){
					if(structure.getAttribute("constraint") !=null)
						name = structure.getAttributeValue("constraint")+" "+name;
					structure.setAttribute("name", "whole_organism");
					Element ch = new Element("character");
					ch.setAttribute("name", "life_style");
					ch.setAttribute("value", name);
					structure.addContent(ch);
				}*/
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * if there are structure with the same name and constraint but different ids
	 * add a relation 'may_be_the_same' among them, set symmetric="true"
	 */

	private void mayBeSameRelation() {
		try{
			List<Element> structures = XPath.selectNodes(this.statement, ".//structure");
			Hashtable<String, ArrayList<String>> names = new Hashtable<String, ArrayList<String>>();
			Iterator<Element> it = structures.iterator();
			//structure => ids hash
			while(it.hasNext()){
				Element structure = it.next();
				String name = structure.getAttributeValue("name");
				//one the two contraint types
				if(structure.getAttribute("constraint_type") !=null)
					name = structure.getAttributeValue("constraint_type")+" "+name;
				if(structure.getAttribute("constraint_parent_organ") !=null)
					name = structure.getAttributeValue("constraint_parent_organ")+" "+name;
				String id = structure.getAttributeValue("id");
				if(names.containsKey(name)){	
					names.get(name).add(id);//update the value for name 
					//names.put(name, names.get(name)); 
				}else{
					ArrayList<String> ids = new ArrayList<String>();
					ids.add(id);
					names.put(name, ids);
				}
			}
			//use the hash to create relations
			Enumeration<String> en = names.keys();
			while(en.hasMoreElements()){
				String name = en.nextElement();
				ArrayList<String> ids = names.get(name);
				if(ids.size()>1){
					for(int i = 0; i<ids.size(); i++){
						for(int j = i+1; j<ids.size(); j++){
							this.addRelation("may_be_the_same", "", true, ids.get(i), ids.get(j), false, "based_on_text");
						}
					}
				}				
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	private void annotateByChunk(ChunkedSentence cs, boolean inbrackets) {
		if(cs == null) return;
		this.inbrackets = inbrackets;
		
		while(cs.hasNext()){
			Chunk ck = cs.nextChunk();
						
			if(ck instanceof ChunkOR){
				int afterorindex = cs.getPointer();
				Element last = this.latestelements.get(this.latestelements.size()-1);
				ck = cs.nextChunk();
				if(ck!=null && last.getName().compareTo("character")==0){
					String cname = last.getAttributeValue("name");
					if(!(ck instanceof ChunkSimpleCharacterState) && !(ck instanceof ChunkNumericals)){//these cases can be handled by the normal annoation procedure
						Element e = new Element("character");
						if(this.inbrackets){e.setAttribute("note", "in_bracket");}
						e.setAttribute("name", cname);
						String v = ck.toString(); //may be a character list
						if(v.length()>=1){//chunk contains text						
							if(v.indexOf("~list~")>=0){
								v = v.replaceFirst("\\w{2,}\\[.*?~list~","").replaceAll("punct", ",").replaceAll("~", " ");
							}
							v = v.replaceAll("(\\w\\[|\\]|\\{|\\}|\\(|\\)|<|>)", "");
							e.setAttribute("value", v);
							addClauseModifierConstraint(cs, e);
							last.getParentElement().addContent(e);
						}else{//chunk not contain text: or nearly so, or not, or throughout
							e = traceBack4(e, last, afterorindex, cs.getPointer());
							last.getParentElement().addContent(e);
						}
					}
				}
				ArrayList<Element> e = new ArrayList<Element>();
				e.add(new Element("or"));
				updateLatestElements(e);
			}
				
			if(ck instanceof ChunkOrgan){//this is the subject of a segment. May contain multiple organs
				String content = ck.toString().replaceFirst("^z\\[", "").replaceFirst("\\]$", "");
				establishSubject(content/*, false*/);
				if(this.partofinference){
					this.cstructures.addAll(this.subjects);
				}
				cs.setInSegment(true);
				cs.setRightAfterSubject(true);
			}else if(ck instanceof ChunkNonSubjectOrgan){
				String content = ck.toString().replaceFirst("^u\\[", "").replaceFirst("\\]$", "");
				String structure = "";
				if(content.indexOf("o[")>=0){
					String m = content.substring(0, content.indexOf("o[")).replaceAll("m\\[", "{").replaceAll("\\]", "}");
					String o = content.substring(content.indexOf("o[")).replaceAll("o\\[", "").replaceAll("\\]", "");
					structure = m+o;
				}else{
					structure = content;
				}
				ArrayList<Element> structures = createStructureElements(structure/*, false*/);
				updateLatestElements(structures);
			}else if(ck instanceof ChunkPrep){
				/*String content = ck.toString();
				if(content.matches(".*?\\bwith\\b.*")){
					this.attachToLast = true;
				}
				if(content.indexOf("c[")>=0){
					content = content.replaceFirst(".*?\\bc\\[", "").replaceAll("\\]", "");
					this.annotateNumericals(content, "count", "", lastStructures());
				}else{*/
					processPrep((ChunkPrep)ck);
				//}
			}else if(ck instanceof ChunkCHPP){//t[c/r[p/o]] this chunk is converted internally and not shown in the parsing output
				String content = ck.toString().replaceFirst("^t\\[", "").replaceFirst("\\]$", "");
				processCHPP(content);
			}else if(ck instanceof ChunkNPList){
				establishSubject(ck.toString().replaceFirst("^l\\[", "").replaceFirst("\\]$", "")/*, false*/);				
			}else if(ck instanceof ChunkSimpleCharacterState){
				String content = ck.toString().replaceFirst("^a\\[", "").replaceFirst("\\]$", "");
				//ArrayList<Element> chars = processSimpleCharacterState(content, lastStructures());//with teeth closely spaced
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				ArrayList<Element> chars = processSimpleCharacterState(content, lastStructures());//apices of basal leaves spread 
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(content + " attached to "+parents.get(0).getAttributeValue("name"));
				}
				updateLatestElements(chars);
			}else if(ck instanceof ChunkSL){//coloration[coloration-list-red-to-black]
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(ck.toString() + " attached to "+parents.get(0).getAttributeValue("name"));
				}
				ArrayList<Element> chars = processCharacterList(ck.toString(), this.subjects);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkComma){
				this.latestelements.add(new Element("comma"));
			}else if(ck instanceof ChunkVP){
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				/*if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(ck.toString() + " attached to "+parents.get(0).getAttributeValue("name"));
				}*/
				ArrayList<Element> es = processTVerb(ck.toString().replaceFirst("^b\\[", "").replaceFirst("\\]$", ""), parents);
				//ArrayList<Element> es = processTVerb(ck.toString().replaceFirst("^b\\[", "").replaceFirst("\\]$", ""), CharacterAnnotatorChunked.subjects);
				updateLatestElements(es);
			}else if(ck instanceof ChunkComparativeValue){
				//ArrayList<Element> chars = processComparativeValue(ck.toString().replaceAll("–", "-"), lastStructures());
				String content = ck.toString();
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(content + " attached to "+parents.get(0).getAttributeValue("name"));
				}
				ArrayList<Element> chars = processComparativeValue(content.replaceAll("–", "-"), lastStructures());
				updateLatestElements(chars);
			}else if(ck instanceof ChunkRatio){
				//ArrayList<Element> chars = annotateNumericals(ck.toString(), "lwratio", "", lastStructures());
				String content = ck.toString();
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(content + " attached to "+parents.get(0).getAttributeValue("name"));
				}
				ArrayList<Element> chars = annotateNumericals(content, "lwratio", "", lastStructures(), false);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkArea){
				//ArrayList<Element> chars = annotateNumericals(ck.toString(), "area", "", lastStructures());
				String content = ck.toString();
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(content + " attached to "+ parents.get(0).getAttributeValue("name"));
				}
				ArrayList<Element> chars = annotateNumericals(content, "area", "", lastStructures(), false);
				updateLatestElements(chars);
			}else if(ck instanceof  ChunkNumericals){
				//** find parents, modifiers
				//TODO: check the use of [ and ( in extreme values
				//ArrayList<Element> parents = lastStructures();
				String text = ck.toString().replaceAll("–", "-");
				boolean resetfrom = false;
				if(text.matches(".*\\bto \\d.*")){ //m[mostly] to 6 m ==> m[mostly] 0-6 m
					text = text.replaceFirst("to\\s+", "0-");
					resetfrom = true;
				}
				ArrayList<Element> parents = this.attachToLast? lastStructures() : subjects;
				if(printAttach && subjects.get(0).getAttributeValue("name").compareTo(lastStructures().get(0).getAttributeValue("name")) != 0){
					System.out.println(text + " attached to "+parents.get(0).getAttributeValue("name"));
				}				
				if(debugNum){
					System.out.println();
					System.out.println(">>>>>>>>>>>>>"+text);
				}
				String modifier1 = "";//m[mostly] [4-]8–12[-19] mm m[distally]; m[usually] 1.5-2 times n[size[{longer} than {wide}]]:consider a constraint
				String modifier2 = "";
				modifier1 = text.replaceFirst("\\[?\\d.*$", "");
				String rest = text.replace(modifier1, "");
				modifier1 =modifier1.replaceAll("(\\w\\[|\\]|\\{|\\})", "").trim();
				modifier2 = rest.replaceFirst(".*?(\\d|\\[|\\+|\\-|\\]|%|\\s|"+ChunkedSentence.units+")+\\s?(?=[a-z]|$)", "");//4-5[+]
				String content = rest.replace(modifier2, "").replaceAll("(\\{|\\})", "").trim();
				modifier2 = modifier2.replaceAll("(\\w+\\[|\\]|\\{|\\})", "").trim();
				ArrayList<Element> chars = annotateNumericals(content, text.indexOf("size")>=0 || content.indexOf('/')>0 || content.indexOf('%')>0 || content.indexOf('.')>0? "size" : null, (modifier1+";"+modifier2).replaceAll("(^\\W|\\W$)", ""), lastStructures(), resetfrom);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkTHAN){
				ArrayList<Element> chars = processTHAN(ck.toString().replaceFirst("^n\\[", "").replaceFirst("\\]$", ""), this.subjects);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkTHANC){//n[(longer) than {wide}] .
				ArrayList<Element> chars = processTHAN(ck.toString().replaceFirst("^n\\[", "").replaceFirst("\\]$", ""), this.subjects);
				updateLatestElements(chars);
			}else if(ck instanceof ChunkBracketed){
				annotateByChunk(new ChunkedSentence(ck.getChunkedTokens(), ck.toString(), conn, glosstable, this.tableprefix), true); //no need to updateLatestElements
				this.inbrackets =false;
			}else if(ck instanceof ChunkSBAR){
				ArrayList<Element> subjectscopy = this.subjects;
				if(this.latestelements.get(this.latestelements.size()-1).getName().compareTo("structure")==0){
					this.subjects = latest("structure", this.latestelements);
				}else{
					int p = cs.getPointer()-2;
					String last = ""; //the chunk before ck??
					do{
						last = cs.getTokenAt(p--);
					}while(!last.matches(".*?\\S.*"));
					String constraintId = null;
					if(last.matches(".*?\\)\\]+")){
						constraintId = "o"+(this.structid-1);
						try{
							Element laststruct = (Element)XPath.selectSingleNode(this.statement, ".//structure[@id='"+constraintId+"']");
							ArrayList<Element> temp = new ArrayList<Element>();
							temp.add(laststruct);
							this.subjects = temp;
						}catch(Exception e){
							e.printStackTrace();
						}
					}else{
						//do nothing
						System.err.println("no structure element found for the SBARChunk, use subjects instead ");
						//this only works for situations where states before subjects got reintroduced after subjects in skiplead
						//this will not work for misidentified nouns before "that/which" statements, in "of/among which", and other cases
					}
				}
				String connector = ck.toString().substring(0,ck.toString().indexOf(" "));
				String content = ck.toString().substring(ck.toString().indexOf(" ")+1);
				ChunkedSentence newcs = new ChunkedSentence(ck.getChunkedTokens(), content, conn, glosstable, this.tableprefix);
				if(connector.compareTo("when")==0){//rewrite content and its chunkedTokens
					Pattern p = Pattern.compile("[\\.,:;]");
					Matcher m = p.matcher(ck.toString());
					int end = 0;
					if(m.find()){
						end = m.start();
					}
					//int end = ck.toString().indexOf(",") > 0? ck.toString().indexOf(",") : ck.toString().indexOf(".");
					String modifier = ck.toString().substring(0, end).trim();//when mature, 
					content = ck.toString().substring(end).replaceAll("^\\W+", "").trim();
					if(content.length()>0){
						ck.setChunkedTokens(Utilities.breakText(content));					
						newcs = new ChunkedSentence(ck.getChunkedTokens(), content, conn, glosstable, this.tableprefix);
					}else{
						newcs = null;
					}
					//attach modifier to the last characters
					if(this.latestelements.get(this.latestelements.size()-1).getName().compareTo("character")==0){
						Iterator<Element> it = this.latestelements.iterator();
						while(it.hasNext()){
							this.addAttribute(it.next(), "modifier", modifier);
						}
					}else{ 
						if(newcs!=null) newcs.unassignedmodifier = "m["+modifier+"]";//this when clause is a modifier for the subclause
						else{
							if(this.latestelements.get(this.latestelements.size()-1).getName().compareTo("comma")==0){
								this.latestelements.remove(this.latestelements.size()-1); //remove comma, so what follows when-clause may refer to the structure mentioned before as in <apex> r[p[of] o[(scape)]] , s[when laid {straight} {back} r[p[from] o[its (insertion)]] ,] just touches the {midpoint} r[p[of] o[the {posterior} (margin)]] r[p[in] o[(fullface)]] {view} ; 
							}
							cs.unassignedmodifier = "m["+modifier.replaceAll("(\\w+\\[|\\]|\\(|\\)|\\{|\\})", "")+"]";
						}
					}
				}

				if(connector.compareTo("where") == 0){
					//retrieve the last non-comma, non-empty chunk					
					int p = cs.getPointer()-2;
					String last = "";
					do{
						last = cs.getTokenAt(p--);
					}while(!last.matches(".*?\\w.*"));
					String constraintId = null;
					if(last.matches(".*?\\)\\]+")) constraintId = "o"+(this.structid-1);				
					cs.setClauseModifierConstraint(last.replaceAll("(\\w+\\[|\\]|\\{|\\}|\\)|\\()", ""), constraintId);
				}
				if(newcs!=null) newcs.setInSegment(true);
				annotateByChunk(newcs, false); //no need to updateLatestElements				
				this.subjects = subjectscopy;//return to original status
				cs.setClauseModifierConstraint(null, null); //return to original status
				//this.unassignedmodifiers = null;
				
			}else if(ck instanceof ChunkChrom){
				String content = ck.toString().replaceAll("[^\\d()\\[\\],+ -]", "").trim();
				//Element structure = new Element("chromosome");
				Element structure = new Element("structure");
				this.addAttribute(structure, "name", "chromosome");
				this.addAttribute(structure, "id", "o"+this.structid);
				this.structid++;
				ArrayList<Element> list = new ArrayList<Element>();
				list.add(structure);
				this.annotateNumericals(content, "count", "", list, false);
				/*for(int i = 0; i<counts.length; i++){
					Element character = new Element("character");
					this.addAttribute(character, "count", counts[i]);
					structure.addContent(character);
				}*/
				addClauseModifierConstraint(cs, structure);
				this.statement.addContent(structure);
			}else if(ck instanceof ChunkValuePercentage || ck instanceof ChunkValueDegree){
				String content = ck.toString();
				Element lastelement = this.latestelements.get(this.latestelements.size()-1);
				if(lastelement!=null && lastelement.getName().compareTo("character") == 0){
					this.addAttribute(lastelement, "modifier", content);
				}else{
					cs.unassignedmodifier = content;
				}
				
			}else if(ck instanceof ChunkEOS || ck instanceof ChunkEOL){
				if(cs.unassignedmodifier!=null && cs.unassignedmodifier.length()>0){
					Element lastelement = this.latestelements.get(this.latestelements.size()-1);
					if(lastelement.getName().compareTo("structure") == 0){
						Iterator<Element> it = this.latestelements.iterator();
						while(it.hasNext()){
							String sid = it.next().getAttributeValue("id");
							try{
								List<Element> relations = XPath.selectNodes(this.statement, ".//relation[@to='"+sid+"']");
								Iterator<Element> rit = relations.iterator();
								int greatestid = 0;
								Element relation = null;
								while(rit.hasNext()){
									Element r = rit.next();
									int rid = Integer.parseInt(r.getAttributeValue("id").replaceFirst("r", ""));
									if(rid>greatestid){
										greatestid = rid;
										relation = r;
									}
								}
								if(relation !=null)	 this.addAttribute(relation, "modifier", cs.unassignedmodifier);
								//TODO: otherwise, categorize modifier and create a character for the structure e.g.{thin} {dorsal} {median} <septum> {centrally} only ;
							}catch(Exception e){
								e.printStackTrace();
							}
						}
						
					}else if(lastelement.getName().compareTo("character") == 0){
						Iterator<Element> it = this.latestelements.iterator();
						while(it.hasNext()){
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

	private void addClauseModifierConstraint(ChunkedSentence cs, Element e) {
		ArrayList<String> cm = cs.getClauseModifierConstraint();
		if(cm!=null){
			if(cm.size()>1){//is a constraint
				this.addAttribute(e, "constraint", cm.get(0));
				this.addAttribute(e, "constraintid", cm.get(1));
			}else{
				this.addAttribute(e, "modifier", cm.get(0));
			}
		}
	}

	/**
	 * track back in this.chunkedTokens to populate the afteror element
	 * afteror shares the same character name and value with beforeor, but have different modifier--which is found from the missing text
	 * 	branched distally or throughout
	   	constricted distally or not
		subequal or weakly to strongly
		well distributed or not
		dioecious or nearly so
		spinulose or not
		openly branched distally or throughout
		branched proximally or distally
		usually 1 cm or less
	 * @param afteror
	 * @param beforor
	 */
	private Element traceBack4(Element afteror, Element beforeor, int afterorindex, int endindex) {
		String text =cs.getText(afterorindex, endindex); //from afterorindex (include) to endindex (not include)
		text = text.replaceAll("SG", "").replaceAll("\\W+", " ").replaceAll("\\s+", " ").trim();
		text = text.replaceFirst("\\s+so$", "");
		afteror = (Element)beforeor.clone();
		this.addAttribute(afteror, "modifier", text);
		return afteror;
	}

	private ArrayList<Element> annotateNumericals(String chunktext, String character, String modifier, ArrayList<Element> parents, boolean resetfrom) {
		ArrayList<Element> chars = NumericalHandler.parseNumericals(chunktext, character);
		if(chars.size()==0){//failed, simplify chunktext
			chunktext = chunktext.replaceAll("[()\\]\\[]", "");
			chars = NumericalHandler.parseNumericals(chunktext, character);
		}
		Iterator<Element> it = chars.iterator();
		ArrayList<Element> results = new ArrayList<Element>();
		while(it.hasNext()){
			Element e = it.next();
			if(resetfrom && e.getAttribute("from")!=null && e.getAttributeValue("from").equals("0") &&(e.getAttribute("from_inclusive")==null || e.getAttributeValue("from_inclusive").equals("true"))){// to 6[-9] m.
				e.removeAttribute("from");
				if(e.getAttribute("from_unit")!=null){
					e.removeAttribute("from_unit");
				}
			}
			if(modifier !=null && modifier.compareTo("")!=0){this.addAttribute(e, "modifier", modifier);}
			if(this.inbrackets){e.setAttribute("in_bracket", "true");}
			/*
			if(this.unassignedmodifiers != null && this.unassignedmodifiers.compareTo("") !=0){
				this.addAttribute(e, "modifier", this.unassignedmodifiers);
				this.unassignedmodifiers = "";
			}*/
			Iterator<Element> pit= parents.iterator();
			while(pit.hasNext()){
				Element ec = (Element)e.clone();
				ec.detach();
				Element p = pit.next();
				p.addContent(ec);
				results.add(ec);
			}
		}
		return results;
	}

	private ArrayList<Element> lastStructures() {
		ArrayList<Element> parents;
		if(this.latestelements.size()> 0 && this.latestelements.get(this.latestelements.size()-1).getName().compareTo("structure") ==0){
			parents = this.latestelements;
		}else{
			parents = this.subjects;
		}
		return parents;
	}
	/**
	 * 3 times n[...than...]
	   lengths 0.5–0.6+ times <bodies>
	   ca .3.5 times length of <throat>
       1–3 times {pinnately} {lobed}
       1–2 times shape[{shape~list~pinnately~lobed~or~divided}]
       4 times longer than wide
       
       
       
	 * @param content: 0.5–0.6+ times a[type[bodies]]
	 * @param subjects2
	 * @return
	 */
	private ArrayList<Element> processComparativeValue(String content,
			ArrayList<Element> parents) {
		if(content.startsWith("n[")){
			content = content.replaceFirst("^n\\[", "").replaceFirst("\\]", "").trim();
		}
		String v = content.replaceAll("("+ChunkedSentence.times+").*$", "").trim(); // v holds numbers
		String n = content.replace(v, "").trim();
		if(n.indexOf("constraint")>=0){
			n = n.replaceFirst("constraint\\[", "").replaceFirst("\\]$", ""); //n holds times....
		}
		if(n.indexOf("n[")>=0 ){//1.5–2.5 times n[{longer} than (throat)]
			//content = "n["+content.replace("n[", "");
			content = v.replaceFirst("(^| )(?=\\d)", " size[")+"] constraint["+n.replaceFirst("n\\[", "").trim(); //m[usually] 1.5-2
			return this.processTHAN(content, parents);
		}else if(n.indexOf("type[")==0 || n.indexOf(" type[")>0){//size[{longer}] constraint[than (object}]
			//this.processSimpleCharacterState("a[size["+v.replace(" times", "")+"]]", parents);
			//ArrayList<Element> structures = this.processObject(n);
			//this.createRelationElements("times", parents, structures, this.unassignedmodifiers);
			//this.unassignedmodifiers = null;
			//return structures;
			n = "constraint["+n.replaceFirst("type\\[", "(").replaceFirst("\\]", ")").replaceAll("a\\[", ""); //1-1.6 times u[o[bodies]] => constraint[times (bodies)]
			content = "size["+v+"] "+n;
			return this.processTHAN(content, parents);			
		}else if(n.indexOf("o[")>=0 ||n.indexOf("z[")>=0  ){//ca .3.5 times length r[p[of] o[(throat)]]
			n = "constraint["+n.replaceAll("[o|z]\\[", ""); //times o[(bodies)] => constraint[times (bodies)]
			content = "size["+v+"] "+n;
			return this.processTHAN(content, parents);	
		}else if(n.indexOf("a[")==0 || n.indexOf(" a[")>0){ //characters:1–3 times {pinnately} {lobed}
			String times = n.substring(0, n.indexOf(' '));
			n = n.substring(n.indexOf(' ')+1);
			n = n.replaceFirst("a\\[", "").replaceFirst("\\]$", "");
			n = "m["+v+" "+times+"] "+n;
			return this.processSimpleCharacterState(n, parents);
		}else if(content.indexOf("[")<0){ //{forked} {moreorless} unevenly ca . 3-4 times , 
			//content = 3-4 times; v = 3-4; n=times
			//marked as a constraint to the last character "forked". "ca." should be removed from sentences in SentenceOrganStateMarker.java
			Element lastelement = this.latestelements.get(this.latestelements.size()-1);
			if(lastelement.getName().compareTo("character")==0){
				Iterator<Element> it = this.latestelements.iterator();
				while(it.hasNext()){
					lastelement = it.next();
					if(cs.unassignedmodifier != null && cs.unassignedmodifier.trim().length()!=0){
						lastelement.setAttribute("modifier", cs.unassignedmodifier);
						cs.unassignedmodifier = null;
					}
					lastelement.setAttribute("constraint", content);
				}
			}else if(lastelement.getName().compareTo("structure")==0){
				return null; //parsing failure
			}
			return this.latestelements;
			
		}
		return null;
	}

	/**
	 * size[{longer}] constraint[than (object)]";
	 * shape[{lobed} constraint[than (proximal)]]
	 * @param replaceFirst
	 * @param subjects2
	 * @return
	 */
	private ArrayList<Element> processTHAN(String content,
			ArrayList<Element> parents) {
		
		ArrayList<Element> charas = new ArrayList<Element>();
		String[] parts = content.split("constraint\\[");
		if(content.startsWith("constraint")){
			charas = latest("character", this.latestelements);
		}else{
			if(parts[0].matches(".*?\\d.*") && parts[0].matches(".*size\\[.*")){//size[m[mostly] [0.5-]1.5-4.5] ;// often wider than 2 cm.
				parts[0] = parts[0].trim().replace("size[", "").replaceFirst("\\]$", "");
				Pattern p = Pattern.compile(NumericalHandler.numberpattern+" ?[{<(]?[cdm]?m?[)>}]?");
				Matcher m = p.matcher(parts[0]);
				String numeric = "";
				if(m.find()){ //a series of number
					numeric = parts[0].substring(m.start(), m.end()).trim().replaceAll("[{<(]$", "");
				}else{
					p = Pattern.compile("\\d+ ?[{<(]?[cdm]?m?[)>}]?"); //1 number
					m = p.matcher(parts[0]);
					m.find();
					numeric = parts[0].substring(m.start(), m.end()).trim().replaceAll("[{<(]$", "");
				}
				String modifier = parts[0].substring(0, parts[0].indexOf(numeric)).replaceAll("(\\w+\\[|\\[|\\])", "").trim();
				if(parts.length<2){//parse out a constraint for further process
					String constraint = parts[0].substring(parts[0].indexOf(numeric)+numeric.length()).trim();
					String t = parts[0];
					parts = new String[2];//parsed out a constraint for further process
					parts[0] = t;
					parts[1] = constraint;
				}
				/*String modifier = parts[0].replaceFirst("size\\[.*?\\]", ";").trim().replaceAll("(^;|;$|\\w\\[|\\])", "");
				String numeric = parts[0].substring(parts[0].indexOf("size["));
				numeric = numeric.substring(0, numeric.indexOf("]")+1).replaceAll("(\\w+\\[|\\])", "");*/
				charas = this.annotateNumericals(numeric.replaceAll("[{<()>}]", ""), "size", modifier.replaceAll("[{<()>}]", ""), parents, false);
			}else{//size[{shorter} than {plumose} {inner}]
				charas = this.processSimpleCharacterState(parts[0].replaceAll("(\\{|\\})", "").trim(), parents); //numeric part
			}
		}
		String object = null;
		ArrayList<Element> structures = new ArrayList<Element>();
		if(parts.length>1 && parts[1].length()>0){//parts[1]: than (other) {pistillate} (paleae)]
			if(parts[1].indexOf("(")>=0){
				String ostr = parts[1];
				object = ostr.replaceFirst("^.*?(?=[({])", "").replaceFirst("\\]+$", ""); //(other) {pistillate} (paleae)
				object = "o["+object+"]";
				if(object != null){
					structures.addAll(this.processObject(object));
				}
				/*while(ostr.indexOf('(')>=0){
					object = ostr.substring(ostr.indexOf('('), ostr.indexOf(')')+1);
					object = "o["+object+"]";
					ostr = ostr.substring(ostr.indexOf(')')+1);
					if(object != null){
						structures.addAll(this.processObject(object));
					}
				}*/
			}
			//have constraints even without an organ 12/15/10
				Iterator<Element> it = charas.iterator();
				while(it.hasNext()){
					Element e = it.next();
					//if(parts[1].indexOf("(")>=0){
					//	this.addAttribute(e, "constraint", this.listStructureNames(parts[1]));
					//}else{
						this.addAttribute(e, "constraint", parts[1].replaceAll("(\\(|\\)|\\{|\\}|\\w*\\[|\\])", ""));
				   //}
					if(object!=null){
						this.addAttribute(e, "constraintid", this.listStructureIds(structures));//TODO: check: some constraints are without constraintid
					}
				}
				
			
		}
		if(structures.size() > 0){
			return structures;
		}else{
			return charas;
		}
	}

	private ArrayList<Element> latest(String name,
			ArrayList<Element> list) {
		ArrayList<Element> selected = new ArrayList<Element>();
		int size = list.size();
		for(int i = size-1; i>=0; i--){
			if(list.get(i).getName().compareTo(name) == 0){
				selected.add(list.get(i));
			}else{
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
	 * @param content
	 * @param parents
	 * @return
	 */
	private ArrayList<Element> processTVerb(String content,
			ArrayList<Element> parents) {
		ArrayList<Element> results = new ArrayList<Element>();
		//String object = content.substring(content.indexOf("o["));
		String object = content.substring(content.lastIndexOf("o["));
		String rest = content.replace(object, "").trim();
		String relation = rest.substring(rest.indexOf("v["));
		String modifier = rest.replace(relation, "").trim().replaceAll("(m\\[|\\])", "");
		
		object = parenthesis(object);
		ArrayList<Element> tostructures = this.processObject(object); //TODO: fix content is wrong. i8: o[a] architecture[surrounding (involucre)]
		results.addAll(tostructures);
		
		this.createRelationElements(relation.replaceAll("(\\w\\[|\\])", ""), this.subjects, tostructures, modifier, false);
		return results;
	}

	/**
	 * @param content: m[usually] coloration[dark brown]: there is only one character states and several modifiers
	 * @param parents: of the character states
	 */
	private ArrayList<Element> processSimpleCharacterState(String content,
			ArrayList<Element> parents) {
		ArrayList<Element> results = new ArrayList<Element>();
		String modifier = "";
		String character = "";
		String state = "";
		String[] tokens = content.split("\\]\\s*");
		for(int i = 0; i<tokens.length; i++){
			if(tokens[i].matches("^m\\[.*")){
				modifier += tokens[i]+" ";
			}else if(tokens[i].matches("^\\w+\\[.*")){
				String[] parts = tokens[i].split("\\[");
				character = parts[0];
				if(this.unassignedcharacter!=null){
					character = this.unassignedcharacter;
					this.unassignedcharacter = null;
				}
				state = parts[1];
				modifier += "; ";
			}
		}
		String statecp = state;
		String charactercp = character;
		modifier = modifier.replaceAll("m\\[", "").trim().replaceAll("(^\\W|\\W$)", "").trim();
		String eqcharacter = ChunkedSentence.eqcharacters.get(state);
		if(eqcharacter != null){
			state = eqcharacter;
			character = Utilities.lookupCharacter(eqcharacter, conn, ChunkedSentence.characterhash, this.glosstable, this.tableprefix);
			if(character ==null){
				state = statecp;
				character = charactercp;
			}
		}
		if(character.compareToIgnoreCase("character")==0 && modifier.length() ==0){//high relief: character=relief, reset the character of "high" to "relief"
			Element lastelement = this.latestelements.get(this.latestelements.size()-1);
			if(lastelement.getName().compareTo("character")==0){
				Iterator<Element> it = this.latestelements.iterator();
				while(it.hasNext()){
					lastelement = it.next();
					lastelement.setAttribute("name", state);
				}
			}else if(lastelement.getName().compareTo("structure")==0){
				this.unassignedcharacter = state;
			}
			results = this.latestelements;
		}else if(state.length()>0){
			/*if(this.unassignedmodifiers!=null && this.unassignedmodifiers.length()>0){
				modifier = modifier+";"+this.unassignedmodifiers;
				this.unassignedmodifiers = "";
			}*/
			this.createCharacterElement(parents, results, modifier, state, character, "");
		}
		
		return results;
	}

	private void establishSubject(String content/*, boolean makeconstraint*/) {
		ArrayList<Element> structures = createStructureElements(content/*, makeconstraint*/);
		this.subjects = new ArrayList<Element>();
		this.latestelements = new ArrayList<Element>();
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element e = it.next();
			if(e.getName().compareTo("structure")==0){ //ignore character elements
				this.subjects.add(e);
				this.latestelements.add(e);
			}
		}
	}
	
	//fix: can not grab subject across treatments
	private void reestablishSubject() {
		Iterator<Element> it = this.subjects.iterator();
		this.latestelements = new ArrayList<Element>();
		while(it.hasNext()){
			Element e = it.next();
			e.detach();
			this.statement.addContent(e);
			this.latestelements.add(e);
		}
	}
	/**
	 * TODO: {shape-list-usually-flat-to-convex-punct-sometimes-conic-or-columnar}
	 *       {pubescence-list-sometimes-bristly-or-hairy}
	 * @param content: pubescence[m[not] {pubescence-list-sometimes-bristly-or-hairy}]
	 * @param parents
	 * @return
	 */
	private ArrayList<Element> processCharacterList(String content,
			ArrayList<Element> parents) {
		ArrayList<Element> results= new ArrayList<Element>();
		String modifier = "";
		if(content.indexOf("m[")>=0){
			modifier=content.substring(content.indexOf("m["), content.indexOf("{"));
			content = content.replace(modifier, "");
			modifier = modifier.trim().replaceAll("(m\\[|\\])", "");
		}
		content = content.replace(modifier, "");
		String[] parts = content.split("\\[");
		if(parts.length<2){
			return results; //@TODO: parsing failure
		}
		String cname = parts[0];
		if(this.unassignedcharacter!=null){
			cname = this.unassignedcharacter;
			this.unassignedcharacter = null;
		}
		String cvalue = parts[1].replaceFirst("\\{"+cname+"~list~", "").replaceFirst("\\W+$", "").replaceAll("~", " ").trim();
		if(cname.endsWith("ttt")){
			this.createCharacterElement(parents, results, modifier, cvalue, cname.replaceFirst("ttt$", ""), "");
			return results;
		}
		if(cvalue.indexOf(" to ")>=0){
			createRangeCharacterElement(parents, results, modifier, cvalue.replaceAll("punct", ",").replaceAll("(\\{|\\})", ""), cname); //add a general statement: coloration="red to brown"
		}
		String mall = "";
		boolean findm = false;
		//gather modifiers from the end of cvalues[i]. this modifier applies to all states
		do{
			findm = false;
			String last = cvalue.substring(cvalue.lastIndexOf(' ')+1);
			if(Utilities.lookupCharacter(last, conn, ChunkedSentence.characterhash, glosstable, tableprefix)==null && Utilities.isAdv(last, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)){
				mall +=last+ " ";
				cvalue = cvalue.replaceFirst(last+"$", "").trim();
				findm = true;
			}
		}while(findm);
		
		String[] cvalues = cvalue.split("\\b(to|or|punct)\\b");//add individual values
		for(int i = 0; i<cvalues.length; i++){
			String state = cvalues[i].trim();//usually papillate to hirsute distally
			//gather modifiers from the beginning of cvalues[i]. a modifier takes effect for all state until a new modifier is found
			String m = "";
			do{
				findm = false;
				if(state.length()==0){
					break;
				}
				int end = state.indexOf(' ')== -1? state.length():state.indexOf(' ');
				String w = state.substring(0, end);
				if(Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, glosstable, tableprefix)==null && Utilities.isAdv(w, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)){
					m +=w+ " ";
					w = w.replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}").replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)").replaceAll("\\+", "\\\\+");
					state = state.replaceFirst(w, "").trim();
					findm = true;
				}
			}while (findm);
			if(m.length()==0){
				state = (modifier+" "+mall+" "+state.replaceAll("\\s+", "#")).trim(); //prefix the previous modifier 
			}else{
				modifier = modifier.matches(".*?\\bnot\\b.*")? modifier +" "+m : m; //update modifier
				//cvalues[i] = (mall+" "+cvalues[i]).trim();
				state = (modifier+" "+mall+" "+state.replaceAll("\\s+", "#")).trim(); //prefix the previous modifier 
			}
			String[] tokens = state.split("\\s+");
			tokens[tokens.length-1] = tokens[tokens.length-1].replaceAll("#", " ");
			results.addAll(this.processCharacterText(tokens, parents, cname));
			//results.addAll(this.processCharacterText(new String[]{state}, parents, cname));
		}

		return results;
	}
	/**
	 * crowded to open
	 * for categorical range-value
	 * @param parents
	 * @param results
	 * @param modifier
	 * @param cvalue
	 * @param cname
	 */
	private String createRangeCharacterElement(ArrayList<Element> parents,
			ArrayList<Element> results, String modifiers, String cvalue,
			String cname) {
		Element character = new Element("character");
		if(this.inbrackets){character.setAttribute("note", "in_bracket");}
		character.setAttribute("char_type", "range_value");
		character.setAttribute("name", cname);
		
		String[] range = cvalue.split("\\s+to\\s+");//a or b, c, to d, c, e
		String[] tokens = range[0].replaceFirst("\\W$", "").replaceFirst("^.*?\\s+or\\s+", "").split("\\s*,\\s*"); //a or b, c, =>
		String from = getFirstCharacter(tokens[tokens.length-1]);
		tokens = range[1].split("\\s*,\\s*");
		String to = getFirstCharacter(tokens[0]);
		character.setAttribute("from", from.replaceAll("-c-", " ")); //a or b to c => b to c
		character.setAttribute("to", to.replaceAll("-c-", " "));
		
		boolean usedm = false;
		Iterator<Element> it = parents.iterator();
		while(it.hasNext()){
			Element e = it.next();
			character = (Element)character.clone();
			if(modifiers.trim().length() >0){
				addAttribute(character, "modifier", modifiers.trim()); //may not have
				usedm = true;
			}
			results.add(character); //add to results
			e.addContent(character);//add to e
		}
		if(usedm){
			modifiers = "";
		}
		addClauseModifierConstraint(cs, character);
		return modifiers;
		
	}

	/**
	 * 
	 * @param tokens: usually large
	 * @return: large
	 */
	private String getFirstCharacter(String character) {
		String[] tokens = character.trim().split("\\s+");
		String result = "";
		for(int i = 0; i<tokens.length; i++){
			if(Utilities.lookupCharacter(tokens[i], conn, ChunkedSentence.characterhash, glosstable, tableprefix)!=null){
				 result += tokens[i]+" ";
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
		if(elements != null){
			latestelements.addAll(elements);
		}
	}

	/**
	 * //t[c/r[p/o]]
	 * m[sometimes] v[subtended] r[p[by] o[(calyculi)]] 
	 * m[loosely loosely] architecture[arachnoid] r[p[at] o[m[distal] {end}]]
	 * 
	 * t[c[{sometimes} with (bases) {decurrent}] r[p[onto] o[(stems)]]]
	 * 
	 * nested:{often} {dispersed} r[p[with] o[aid  r[p[from] o[(pappi)]]]] 
	 * @param ck
	 */
	
	private void processCHPP(String content) {
		//having oval outline
		if(this.characterPrep(content)){
			return;
		}		
		String c = content.substring(0, content.indexOf("r["));
		String r = content.replace(c, "");
		if(r.lastIndexOf("o[")<0){ //#{usually} {arising} r[p[in]]# {distal} 1/2
			//failed parse
			cs.setPointer2NextComma();
			return;
		}
		String p = r.substring(0, r.lastIndexOf("o["));//{often} {dispersed} r[p[with] o[aid  r[p[from] o[(pappi)]]]] 
		String o = r.replace(p, "");
		String[] mc = c.split("(?<=\\])\\s*");
		String m = "";
		c = "";
		for(int i =0; i<mc.length; i++){
			if(mc[i].startsWith("m[")){
				m += mc[i]+" ";
			}else if(mc[i].startsWith("c[")/*mc[i].matches("^\\w+\\[.*")*/){
				c += mc[i]+" ";
			}
		}
		m = m.replaceAll("(m\\[|\\]|\\{|\\})", "").trim();
		c = c.replaceAll("(c\\[|\\]|\\{|\\})", "").trim(); //TODO: will this work for nested chuncks?
		p = p.replaceAll("(\\w\\[|\\])", "").trim();
		//c: {loosely} {arachnoid}
		String[] words = c.split("\\s+");
		if(Utilities.isVerb(words[words.length-1], ChunkedSentence.verbs, ChunkedSentence.notverbs) || p.compareTo("to")==0){//t[c[{connected}] r[p[by] o[{conspicuous} {arachnoid} <trichomes>]]] TODO: what if c was not included in this chunk?
			String relation = (c+" "+p).replaceAll("\\s+", " ");
			o = o.replaceAll("(o\\[|\\])", "");
			/*if(!o.endsWith(")") &&!o.endsWith("}")){ //1-5 series => 1-5 (series)
				String t = o.substring(o.lastIndexOf(' ')+1);
				o = o.replaceFirst(t+"$", "("+t)+")";
			}*/
			if(!o.endsWith(")")){ //force () on the last word. Hong 3/4/11
				String t = o.substring(o.lastIndexOf(' ')+1);
				t = t.replace("{", "").replace("}", "");
				o = o.substring(0, o.lastIndexOf(' ')+1)+"("+t+")";
				
				//System.out.println("forced organ in: "+o);
			}
			ArrayList<Element> structures = processObject("o["+o+"]");
			ArrayList<Element> entity1 = null;
			Element e = this.latestelements.get(this.latestelements.size()-1);
			if(e.getName().matches("("+this.delims+")") || e.getName().compareTo("character")==0 ){
				entity1 = this.subjects;
			}else{
				entity1 = (ArrayList<Element>)this.latestelements.clone();
				//entity1.remove(entity1.size()-1);
			}
			createRelationElements(relation, entity1, structures, m, false);
			updateLatestElements(structures);
		}else{//c: {loosely} {arachnoid} : should be m[loosly] architecture[arachnoid]
			//String[] tokens = c.replaceAll("[{}]", "").split("\\s+");
			//ArrayList<Element> charas = this.processCharacterText(tokens, this.subjects);
			ArrayList<Element> charas = this.processSimpleCharacterState(c, this.subjects);
			updateLatestElements(charas);
			processPrep(new ChunkPrep(r)); //not as a relation
		}
		
	}



	/**
	 * CK takes form of relation character/states [structures]?
	 * update this.latestElements with structures only.
	 * 
	 * nested1: r[p[of] o[5-40 , fusion[{fusion~list~distinct~or~basally~connate}] r[p[in] o[groups]] , coloration[{coloration~list~white~to~tan}] , {wholly} or {distally} {plumose} (bristles)]] []]
	 * nested2: r[p[with] o[{central} {cluster} r[p[of] o[(spines)]]]]
	 * @param ck
	 * @param asrelation: if this PP should be treated as a relation
	 */
	private void processPrep(ChunkPrep ck) {
		String ckstring = ck.toString(); //r[{} {} p[of] o[.....]]
		String modifier = ckstring.substring(0, ckstring.indexOf("p[")).replaceFirst("^r\\[", "").replaceAll("[{}]", "").trim();
		//sometime o[] is not here as in ckstring=r[p[at or above]] {middle}
		//String pp = ckstring.substring(ckstring.indexOf("p["), ckstring.lastIndexOf("] o[")).replaceAll("(\\w\\[|])", "");
		//String object  = ckstring.substring(ckstring.lastIndexOf("o[")).replaceFirst("\\]+$", "")+"]";	
		int objectindex = ckstring.indexOf("]", ckstring.indexOf("p[")+1);
		String pp = ckstring.substring(ckstring.indexOf("p["), objectindex).replaceAll("(\\w\\[|])", "");
		String object = "o["+ckstring.substring(objectindex).trim().replaceAll("(\\w\\[|])", "")+"]";
		
		//TODO: r[p[in] o[outline]] or r[p[with] o[irregular ventral profile]]
		if(characterPrep(ckstring)){
			return;		
		}
		/*String pp = null;
		String object = null;
		if(ckstring.matches(".*?\\]{4,}$")){//nested2 
			pp = ckstring.substring(ckstring.indexOf("p["), ckstring.lastIndexOf("] o[")).replaceAll("(\\w\\[|])", "");
			object  = ckstring.substring(ckstring.lastIndexOf("o[")).replaceFirst("\\]+$", "")+"]";	
		}else{//nested1 or not nested
			pp = ckstring.substring(ckstring.indexOf("p["), ckstring.indexOf("] o[")).replaceAll("(\\w\\[|])", "");
			object  = ckstring.substring(ckstring.indexOf("o[")).replaceFirst("\\]+$", "")+"]";//nested or not
		}*/
		
		object = NumericalHandler.originalNumForm(object);
		boolean lastIsStruct = false;
		boolean lastIsChara = false;
		boolean lastIsComma = false;
		Element lastelement = this.latestelements.get(this.latestelements.size()-1);
		if(lastelement.getName().compareTo("structure") == 0){//lastest element is a structure
			lastIsStruct = true;
		}else if(lastelement.getName().compareTo("character") == 0){
			lastIsChara = true;
		}else if(lastelement.getName().matches("("+this.delims+")")){
			lastIsComma = true;
			if(this.printComma){
				System.out.println("prep ahead of character: "+ckstring);
			}
		}
		//of o[3-7]
		if(lastIsStruct && object.matches("o\\[\\(?\\[?\\d.*?\\d\\+?\\]")){
			this.annotateNumericals(object.replaceAll("(o\\[|\\])", ""), "count", null, this.latestelements, false);
			return;
		}
		
		ArrayList<Element> structures = new ArrayList<Element>();
		//3/30/2011: try to separate "in {} {} arrays" cases from "at {flowering}", "in fruit", and "in size" cases
		//allow () be added around the last bare word if there is a {} before the bare word, or if the word is not a character (size, profile, lengths)
		object = parenthesis(object);
		/*if(! object.matches(".*?\\}\\]+$")){ //contains organ: > or untagged: arrays
			//add () around the last word if it is bare
			if(object.matches(".*?[a-z]\\]+$")){
				System.out.println("!!!!!!Object: "+object);
				int l = object.lastIndexOf(' ');
				if(l>0){
					String last = object.substring(l+1);
					object = object.replaceFirst(last+"$", "("+last.replaceFirst("\\]", ")]"));
				}else{//object= o[tendrils]
					object = object.replaceFirst("\\[", "[(").replaceFirst("\\]", ")]");
				}
			}*/
		if(object.matches(".*?\\)\\]+$")){
			structures = linkObjects(modifier, pp, object, lastIsStruct,
					lastIsChara, lastelement);
			updateLatestElements(structures);
		}else if(object.matches(".*?\\([-a-z]+\\).*") && !object.matches(".*?[-a-z]+\\]+$")){//contains organ in the middle of object:r[p[from] o[{thick} {notothyrial} (platform) {excavated} {laterally}]]
			String obj = object.substring(0, object.lastIndexOf(")")+1).trim();
			String modi = object.substring(object.lastIndexOf(")")+1).trim(); //TODO: left out right end modi for now.
			
			object = obj;
			structures = linkObjects(modifier, pp, object, lastIsStruct,
					lastIsChara, lastelement);
			updateLatestElements(structures);
		}else{// "at {flowering}]" or "in size]" 
			//contains no organ, e.g. "at flowering"
			//Element last = this.latestelements.get(this.latestelements.size()-1);
			if(lastIsStruct){
				addAttribute(lastelement, "constraint", ckstring.replaceAll("(\\w\\[|\\]|\\{|\\})", ""));//TODO 5/16/2011 <corollas> r[p[of] o[{sterile} {much} {expanded} and {exceeding} (corollas)]] This should not be happening.z[{equaling} (phyllaries)] r[p[at] o[{flowering}]]
			}else if(lastIsChara){ //character element
				addAttribute(lastelement, "modifier", ckstring.replaceAll("(\\w\\[|\\]|\\{|\\})", ""));
			}
			//addPPAsAttributes(ckstring);
		}
		
		//bookkeeping: update this.latestElements: only structures are visible
		//updateLatestElements(structures);
	}

	/**
	 * 
	 * @param ckstring:r[p[in] o[outline]]
	 * @return
	 */
	private boolean characterPrep(String ckstring) {
		boolean done =false;
		String lastword = ckstring.substring(ckstring.lastIndexOf(" ")).replaceAll("\\W", "");
		if(lastword.matches("("+this.characters+")")){
			Element lastelement = this.latestelements.get(this.latestelements.size()-1);
			if(lastelement.getName().compareTo("character")==0){//shell oval in outline
				Iterator<Element> it = this.latestelements.iterator();
				while(it.hasNext()){
					lastelement = it.next();
					lastelement.setAttribute("name", lastword);
				}
				done = true;
			}else if(lastelement.getName().compareTo("structure")==0){//shell in oval outline
				String cvalue = ckstring.replaceFirst(".*?\\]", "").replaceAll("\\w+\\[","").replaceAll(lastword, "").replaceAll("[{}\\]\\[]", "");
				Iterator<Element> it = this.latestelements.iterator();
				while(it.hasNext()){
					lastelement = it.next();
					lastelement.setAttribute("name", lastword);
					lastelement.setAttribute("value", cvalue);
				}
				done = true;
			}
		}
		return done;
	}

	private String parenthesis(String object) {
		if(!object.matches(".*?\\}\\]+$")){ //contains organ: > or untagged: arrays
			if(object.matches(".*?[a-z]\\]+$")){//there is a bare word
				int l = object.lastIndexOf(' ');
				l = l < 0 ? object.lastIndexOf('[') : l; 
				String last = object.substring(l+1).replaceAll("\\W+$", "");
				if(object.indexOf('{')>=0 || !isCharacter(last)){// if there are other modifiers/characters, then must make "last" a structure
					object = object.replaceFirst(last+"(?=\\]+$)", "("+last+")");
				}								
			}
		}
		return object;
	}

	private boolean isCharacter(String last) {
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from "+this.glosstable +" where term='"+last+"' and category='character'");
			if(rs.next()){
				return true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}

	private ArrayList<Element> linkObjects(String modifier, String pp,
			String object, boolean lastIsStruct, boolean lastIsChara,
			Element lastelement) {
		ArrayList<Element> structures;
		structures = processObject(object);
		String base = "";
		if(object.matches("o?\\[*\\{*("+ChunkedSentence.basecounts+")\\b.*")){
			base = "each";
		}
		if(lastIsChara){
			//if last character is size, change to location: <margins> r[p[with] o[3–6 (spines)]] 1–3 {mm} r[p[{near}] o[(bases)]]. 
			//1-3 mm is not a size, but a location of spines
			if(lastelement.getAttributeValue("name").compareTo("size") == 0 && 
					((lastelement.getAttributeValue("value")!=null && lastelement.getAttributeValue("value").matches(".*?\\d.*")) || (lastelement.getAttributeValue("from")!=null && lastelement.getAttributeValue("from").matches(".*?\\d.*")))
					&& pp.matches("("+ChunkedSentence.locationpp+")")){
				lastelement.setAttribute("name", "location");
			}
			//addAttribute(lastelement, "constraint", (pp+" "+base+" "+listStructureNames(structures)).replaceAll("\\s+", " ").replaceAll("(\\{|\\})", "")); //a, b, c
			addAttribute(lastelement, "constraint", (pp+" "+listStructureNames(object)).replaceAll("\\s+", " ").replaceAll("(\\w+\\[|\\(|\\)|\\{|\\}|\\])", ""));
			addAttribute(lastelement, "constraintid", listStructureIds(structures));//1, 2, 3
			if(modifier.length()>0){
				addAttribute(lastelement, "modifier", modifier);
			}
		}else{
			ArrayList<Element> entity1 = null;
			if(lastIsStruct){
				entity1 = this.latestelements;
			}else{
				entity1 = this.subjects;
			}
			String relation = relationLabel(pp, entity1, structures);//determine the relation
			if(relation != null){
				createRelationElements(relation, entity1, structures, modifier, false);//relation elements not visible to outside 
			}
			if(relation.compareTo("part_of")==0) structures = entity1; //part_of holds: make the organbeforeof/entity1 the return value, all subsequent characters should be refering to organbeforeOf/entity1
			
		}
		return structures;
	}
	/**
	 * o[.........{m} {m} (o1) and {m} (o2)]
	 * o[each {bisexual} , architecture[{architecture-list-functionally-staminate-punct-or-pistillate}] (floret)]] ; 
	 * @param object
	 * @return
	 */
	private ArrayList<Element> processObject(String object) {
		ArrayList<Element> structures;
		if(object.indexOf("l[")>=0){
			//a list of object
			object = object.replace("l[", "").replaceFirst("\\]", "");
		}
		String[] twoparts = separate(object);//find the organs in object o[.........{m} {m} (o1) and {m} (o2)]
		structures = createStructureElements(twoparts[1]/*, false*/);//to be added structures found in 2nd part, not rewrite this.latestelements yet
		if(twoparts[0].length()>0){
			/*if(twoparts[0].matches(".*?\\b\\w\\[.*")){//nested chunks: e.g. 5-40 , fusion[{fusion~list~distinct~or~basally~connate}] r[p[in] o[groups]] , coloration[{coloration~list~white~to~tan}] , {wholly} or {distally} {plumose}
				//get tokens for the new chunkedsentence
				ArrayList<String>tokens = Utilities.breakText(twoparts[0]);
				twoparts[0]=twoparts[0].trim();
				if(!twoparts[0].matches(".*?[,;\\.:]$")){
					twoparts[0] +=" .";
					tokens.add(".");
				}
				ChunkedSentence newcs = new ChunkedSentence(tokens, twoparts[0], conn, glosstable);
				//annotate this new chunk
				ArrayList<Element> subjectscopy = this.subjects;
				this.subjects = structures;
				newcs.setInSegment(true);
				annotateByChunk(newcs, false); //no need to updateLatestElements
				this.subjects = subjectscopy;
			}else{*/
				ArrayList<Element> structurescp = (ArrayList<Element>) structures.clone();
				String[] tokens = twoparts[0].replaceFirst("[_-]$", "").split("\\s+");//add character elements
				if(twoparts[1].indexOf(") plus")>0){//(teeth) plus 1-2 (bristles), the structure comes after "plus" should be excluded
					String firstorgans = twoparts[1].substring(0, twoparts[1].indexOf(") plus")); //(teeth
					String lastorganincluded = firstorgans.substring(firstorgans.lastIndexOf("(")+1);
					for(int i = structures.size()-1; i>=0;  i--){
						if(!structures.get(i).getAttributeValue("name").equals(Utilities.toSingular(lastorganincluded))){
							structures.remove(i);
						}
					}
				}
				processCharacterText(tokens, structures, null); //process part 1, which applies to all lateststructures, invisible
				structures = structurescp;
			//}
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
		while(it.hasNext()){
			Element e = it.next();
			list.append(e.getAttributeValue("id")+", ");
		}
		return list.toString().trim().replaceFirst(",$", "");
	}

	//find all () in object
	private String listStructureNames(String object){
		String os = "";
		object = object.replaceAll("\\)\\s*\\(", " "); //(leaf) (blade) =>(leaf blade)
		Pattern p = Pattern.compile(".*?\\(([^)]*?)\\)(.*)");
		Matcher m = p.matcher(object);
		while(m.matches()){
			os += m.group(1)+", ";
			object = m.group(2);
			m = p.matcher(object);
		}
		return os.trim().replaceFirst(",$", "");
	}
	/*private String listStructureNames(ArrayList<Element> structures) {
		StringBuffer list = new StringBuffer();
		Iterator<Element> it = structures.iterator();
		while(it.hasNext()){
			Element e = it.next();
			list.append(e.getAttributeValue("name")+", ");
		}
		return list.toString().trim().replaceFirst(",$", "");
	}*/

	private void createRelationElements(String relation, ArrayList<Element> fromstructs, ArrayList<Element> tostructs, String modifier, boolean symmetric) {
		//add relation elements
		relation = relation.replaceAll("(\\w+\\[|\\]|\\{|\\}|\\(|\\))", "");
		for(int i = 0; i<fromstructs.size(); i++){
			String o1id = fromstructs.get(i).getAttributeValue("id");
			for(int j = 0; j<tostructs.size(); j++){
				String o2id = tostructs.get(j).getAttributeValue("id");
				boolean negation=false;
				if(modifier.matches(".*?\\bnot\\b.*")){
					negation = true;
					modifier = modifier.replace("not", "").trim();
				}
				if(relation.matches(".*?\\bnot\\b.*")){
					negation = true;
					relation = relation.replace("not", "").trim();
				}
				addRelation(relation, modifier, symmetric, o1id, o2id, negation, "based_on_text");
			}
		}
		//add other relations as a constraint to the structure: apex of leaves {rounded}.
		//expect some character elements in the structure element.
		//if not, in post-processing, remove the constraint
		/*if(relation.compareTo("consists of")!=0){
			String constraint = relation+" ";
			for(int j = 0; j<this.lateststructures.size(); j++){
				constraint += this.lateststructures.get(j).getAttributeValue("name")+", "; //organ name list
			}
			constraint.trim().replaceFirst("\\s*,$", "");
			for(int i = 0; i<latests.size(); i++){
				addAttribute(latests.get(i), "constraint", constraint); //base, of leaves, petals; apex, of leaves, petals
			}
		}*/
	}

	private void addRelation(String relation, String modifier,
			boolean symmetric, String o1id, String o2id, boolean negation, String inferencemethod) {
		Element rela = new Element("relation");
		if(this.inbrackets){rela.setAttribute("in_bracket", "true");}
		rela.setAttribute("id", "r"+this.relationid);
		this.relationid++;
		rela.setAttribute("name", relation);
		rela.setAttribute("from", o1id);
		rela.setAttribute("to", o2id);
		rela.setAttribute("negation", negation+"");
		//rela.setAttribute("symmetric", symmetric+"");
		//rela.setAttribute("inference_method", inferencemethod);
		//if(modifier.length()>0 && modifier.indexOf("m[")>=0){
		if(modifier.length()>0){
			addAttribute(rela, "modifier", modifier.replaceAll("m\\[|\\]", ""));
		}
		addClauseModifierConstraint(cs, rela);
		this.statement.addContent(rela); //add to statement
	}
	
	
	
	/**
	 * 
	 * @param pp
	 * @param latests
	 * @param lateststructures2
	 * @return
	 */
	private String relationLabel(String pp, ArrayList<Element> organsbeforepp,
			ArrayList<Element> organsafterpp) {
		if(pp.compareTo("of") ==0){
			return differentiateOf(organsbeforepp, organsafterpp);
		}
		return pp;
	}

	private void addAttribute(Element e, String attribute, String value) {
		value = value.replaceAll("(\\w+\\[|\\]|\\{|\\}|\\(|\\))", "").replaceAll("\\s+;\\s+", ";").trim();
		if(value.indexOf("LRB-")>0) value = NumericalHandler.originalNumForm(value);
		value = value.replaceAll("\\b("+this.notInModifier+")\\b", "").trim();
		if(this.evaluation && attribute.startsWith("constraint_")) attribute="constraint"; 
		if(value.length()>0){
			if(value.indexOf("moreorless")>=0){
				value = value.replaceAll("moreorless", "more or less");
			}
			value = value.replaceAll(" , ", ", ").trim();
			String v = e.getAttributeValue(attribute);
			if(v==null || !v.matches(".*?(^|; )"+value+"(;|$).*")){
				if(v !=null && v.trim().length() > 0){
					v = v.trim()+ ";"+value;
				}else{
					v = value;
				}
				if(attribute.equals("constraintid")) v = v.replaceAll("\\W", " "); //IDREFS are space-separated
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
	 * involucre of => consists of
	 */
	private String differentiateOf(ArrayList<Element> organsbeforeOf,
			ArrayList<Element> organsafterOf) {
		String result = "part_of";
		try{
			Statement stmt = conn.createStatement();
			
			for (int i = 0; i<organsbeforeOf.size(); i++){
				String b = organsbeforeOf.get(i).getAttributeValue("name");
				if(b.matches("("+ChunkedSentence.clusters+")")){
					result = "consist_of";
					break;
				}
				for(int j = 0; j<organsafterOf.size(); j++){
					String a = organsafterOf.get(j).getAttributeValue("name");
					//String pattern = a+"[ ]+of[ ]+[0-9]+.*"+b+"[,\\.]"; //consists-of
					if(a.length()>0 && b.length()>0){
						String pb = Utilities.plural(b);
						String pa = Utilities.plural(a);
						String pattern = "("+b+"|"+pb+")"+"[ ]+of[ ]+[0-9]+.*"+"("+a+"|"+pa+")"+"[ ]?(,|;|\\.|and|or|plus)"; //consists-of
						String query = "select * from "+this.tableprefix+"_sentence where sentence rlike '"+pattern+"'";
						ResultSet rs  = stmt.executeQuery(query);
						if(rs.next()){
							result = "consist_of";
							break;
						}
						rs.close();
					}
				}				
			}	
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}

		return result;
	}

	//separate o[......... {m} {m} (o1) and {m} (o2)] to two parts: the last part include all organ names
	private String[] separate(String object) {
		String[] twoparts  = new String[2];
		object = object.replaceFirst("^o\\[", "").replaceFirst("\\]$", "").replaceAll("<", "(").replaceAll(">", ")");
		String part2 = "";
		//if(object.indexOf("(")>=0){
			part2 = object.substring(object.indexOf("(")).trim();
		//}else if(object.lastIndexOf(" ")>=0){
		//	part2 = object.substring(object.lastIndexOf(" ")).trim();
		//}else{
		//	part2 = object;
		//}
		String part1 = object.replace(part2, "").trim();
		if(part1.length()>0){
			//part 1 may still have modifiers of the first organ in part 2
			String[] ws1 = part1.split("\\s+");
			String[] ws2 = part2.split("\\s+");
			String o = "";
			for(int i =0; i<ws2.length; i++){
				if(ws2[i].indexOf("(")>=0){
					o +=ws2[i]+" ";
				}else{
					break;
				}
			}
			o = o.trim();
			for(int i = ws1.length-1; i>=0; i--){
				String escaped = ws1[i].replaceAll("\\{", "\\\\{").replaceAll("\\}", "\\\\}");
				if(constraintType(ws1[i].replaceAll("\\W", ""), o)!=null){
					part1 = part1.replaceFirst("\\s*"+escaped+"$", "");
					part2 = ws1[i]+" "+part2;
				}else{
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
	 * TODO: flower and leaf blades???
	 * @param ck: {} (), {} (), () and/or ()
	 * @return
	 */
	private ArrayList<Element> createStructureElements(String listofstructures/*, boolean makeconstraint*/){
		ArrayList<Element> results = new ArrayList<Element>();	
		//String[] organs = listofstructures.replaceAll(" (and|or|plus) ", " , ").split("\\)\\s*,\\s*"); //TODO: flower and leaf blades???
		String[] organs = listofstructures.replaceAll(",", " , ").split("\\)\\s+(and|or|plus|,)\\s+"); //TODO: flower and leaf blades???		
		String[] sharedcharacters = null;
		for(int i = 0; i<organs.length; i++){
			String[] organ = organs[i].trim().split("\\s+");
			//for each organ mentioned, find organ name
			String o = "";
			int j = 0;
			for(j = organ.length-1; j >=0; j--){
				//if(organ[j].startsWith("(")){ //(spine tip)
				/*if(organ[j].endsWith(")") || organ[j].startsWith("(")){ //(spine tip)	
					o = organ[j]+" "+o;
					organ[j] = "";
				}else{
					break;
				}*/
				if(organ[j].endsWith(")") || organ[j].startsWith("(")){ //(spine tip)	
					o = organ[j]+" "+o;
					organ[j] = "";
					break; //take the last organ name
				}
			}
			o = o.replaceAll("(\\w\\[|\\]|\\(|\\))", "").trim();
			//create element, 
			Element e = new Element("structure");
			if(this.inbrackets){e.setAttribute("note", "in_bracket");}
			String strid = "o"+this.structid;
			this.structid++;
			e.setAttribute("id", strid);
			//e.setAttribute("name", o.trim()); //must have.
			e.setAttribute("name", Utilities.toSingular(o.trim())); //must have. //corolla lobes
			this.statement.addContent(e);
			results.add(e); //results only adds e
			
			//determine constraints
			while(j>=0 && organ[j].trim().length()==0){
				j--;
			}
			//cauline leaf abaxial surface trichmode hair long
			boolean terminate =false;
			boolean distribute = false;
			String constraint = "";//plain
			for(;j >=0; j--){
				if(terminate) break;
				String w = organ[j].replaceAll("(\\w+\\[|\\]|\\{\\(|\\)\\}|\\(\\{|\\}\\))", "");
				if(w.equals(",")){
					distribute = true;
					continue;
				}
				String type = null;
				if(w.startsWith("(")) type="parent_organ";
				else type = constraintType(w, o);
				if(type!=null){
					organ[j] = "";
					constraint = w+" " +constraint; //plain
					//fancy:
					/*if(type.equals("type")){
						if(distribute){//outer , mid phyllaries => distribute "phyllaries" to "outer"
							//create element, 
							Element e1 = new Element("structure");
							if(this.inbrackets){e1.setAttribute("note", "in_bracket");}
							e1.setAttribute("id", "o"+this.structid);
							this.structid++;
							e1.setAttribute("name", Utilities.toSingular(o.trim())); //must have. //corolla lobes
							addAttribute(e1, "constraint_"+type, w.replaceAll("(\\(|\\))", "").trim()); //may not have.	
							this.statement.addContent(e1);
							results.add(e1); //results only adds e
							distribute = false;
						}else{
							addAttribute(e, "constraint_"+type, w.replaceAll("(\\(|\\))", "").trim()); //may not have.	
						}
					}else{//"parent_organ": collect all until a null constraint is found
						String constraint = w;
						j--;
						for(; j>=0; j--){
							w = organ[j].replaceAll("(\\w+\\[|\\]|\\{\\(|\\)\\})", "");
							if(w.startsWith("(")) type="parent_organ";
							else type = constraintType(w, o);
							if(type!=null){
								constraint = w+" "+constraint;
								organ[j] = "";
							}
							else{
								addAttribute(e, "constraint_parent_organ", constraint.replaceAll("(\\(|\\))", "").trim()); //may not have.
								terminate = true;
								if(this.partofinference){
									driveRelationFromStructrueContraint(strid, "part_of", constraint);
								}
								constraint = "";
								break;
							}
						}
						if(constraint.length()>0){
							addAttribute(e, "constraint_parent_organ", constraint.replaceAll("(\\(|\\)|\\}|\\{)", "").trim()); //may not have.
							terminate = true;
							if(this.partofinference){
								driveRelationFromStructrueContraint(strid, "part_of", constraint);
							}
							constraint = "";
							break;							
						}
					}*/
				}else{
					break;
				}				
			}
			j++;
			//plain
			if(constraint.trim().length() >0){
				addAttribute(e, "constraint", constraint.replaceAll("(\\(|\\))", "").trim()); //may not have.
			}
			//plain
			
			//determine character/modifier
			ArrayList<Element> list = new ArrayList<Element>();
			list.add(e);
			//process text reminding in organ
			if(organ[0].trim().length()>0){//has c/m remains, may be shared by later organs
				sharedcharacters = organ;
			}else if(sharedcharacters !=null){//share c/m from a previous organ
				organ = sharedcharacters;
			}
			processCharacterText(organ, list, null); //characters created here are final and all the structures will have, therefore they shall stay local and not visible from outside
			
			
		}
		return results;
	}


	/**
	 * cauline leaf abaxial surface thin trichomode hair
	 * constraint_type: trichomode
	   constraint_parent_organ: cauline leaf abaxial surface
	 * @param fromid: from_id
	 * @param relation: "part_of"
	 * @param toorganname: use this to find the to_id
	 */
	private void driveRelationFromStructrueContraint(String fromid,
			String relation, String toorganname) {
		try{
			//try to link toorganname to an previously mentioned organ
			List<Element> structures = XPath.selectNodes(this.statement, ".//structure");
			Iterator<Element> it = structures.iterator();
			boolean exist = false;
			while(it.hasNext()){
				Element structure = it.next();
				String name = structure.getAttributeValue("name");
				if(structure.getAttribute("constraint_type")!=null){
					String tokens = structure.getAttributeValue("constraint_type"); //need to reverse order
					tokens = reversed(tokens);
					name =tokens +" "+name;
				}
				if(structure.getAttribute("constraint_parent_organ")!=null){
					name = structure.getAttributeValue("constraint_parent_organ")+" "+name;
				}
				
				if(name.equals(toorganname)){
					exist = true;
					String toid = structure.getAttributeValue("id");
					addRelation(relation, "", false, fromid, toid, false, "based_on_parent_organ_constraint");
					break;
				}
			}
			if(!exist){ //create a new structure
				addRelation(relation, "", false, fromid, "o"+this.structid, false, "based_on_parent_organ_constraint");
				toorganname = toorganname.replaceFirst(" (?=\\w+$)", " (")+")"; //format organname
				if(toorganname.indexOf('(')<0) toorganname="("+toorganname;
				this.createStructureElements(toorganname);				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	/**
	 * turn "b;a" to "a b"
	 * @param tokens
	 * @return
	 */
	private String reversed(String tokens) {
		String[] ts = tokens.split("\\s*;\\s*");
		String result = "";
		for(int i = ts.length-1; i>=0; i--){
			result += i+" ";
		}
		return result.trim();
	}

	/**
	 * bases and tips mostly rounded 
	 * @param tokens
	 * @param parents
	 */
	private ArrayList<Element> processCharacterText(String[] tokens, ArrayList<Element> parents, String character) {
		ArrayList<Element> results = new ArrayList<Element>();
		//determine characters and modifiers
		String modifiers = "";
		for(int j = 0; j <tokens.length; j++){
			if(tokens[j].trim().length()>0){
				tokens[j] = NumericalHandler.originalNumForm(tokens[j]);
				if(tokens[j].indexOf("~list~")>=0){
					results = this.processCharacterList(tokens[j], parents);
				}else{
					String w = tokens[j];
					String chara= null;
					if(tokens[j].matches("\\w{2,}\\[.*")){
						chara=tokens[j].substring(0, tokens[j].indexOf('['));
						w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
					}else if(tokens[j].matches("\\w\\[.*")){
						w = tokens[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
					}
					w = w.replaceAll("(\\{|\\})", "");
					chara = Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, glosstable, tableprefix);
					
					if(chara==null && Utilities.isAdv(w, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)){//TODO: can be made more efficient, since sometimes character is already given
						modifiers +=w+" ";
					}else if(w.matches(".*?\\d.*") && !w.matches(".*?[a-z].*")){//TODO: 2 times =>2-times?

						results = this.annotateNumericals(w, "count", modifiers, parents, false);
						//annotateCount(parents, w, modifiers);
						modifiers = "";
					}else{
						//String chara = MyPOSTagger.characterhash.get(w);
						if(chara != null){
							if(character!=null){
								chara = character;
							}
							if(chara.compareToIgnoreCase("character")==0 && modifiers.length() ==0){//high relief: character=relief, reset the character of "high" to "relief"
								Element lastelement = null;
								if(results.size()>=1){
									lastelement = results.get(results.size()-1);
								}else if(this.latestelements.size()>=1){
									lastelement = this.latestelements.get(this.latestelements.size()-1);
								}
								if(lastelement != null && lastelement.getName().compareTo("character")==0){
									lastelement.setAttribute("name", w);
									/*Iterator<Element> it = this.latestelements.iterator();
									while(it.hasNext()){
										lastelement = it.next();
										lastelement.setAttribute("name", w);
									}*/
								}
							}else{
							createCharacterElement(parents, results,
									modifiers, w, chara, ""); //default type "" = individual vaues
							modifiers = "";
							}
						}
					}
				}
				
			}
		}
		return results;
	}

	private String createCharacterElement(ArrayList<Element> parents,
			ArrayList<Element> results, String modifiers, String cvalue, String cname, String char_type) {
		Element character = new Element("character");
		if(this.inbrackets){character.setAttribute("note", "in_bracket");}
		if(cname.compareTo("count")==0 && cvalue.indexOf("-")>=0 && cvalue.indexOf("-")==cvalue.lastIndexOf("-")){
			String[] values = cvalue.split("-");
			character.setAttribute("char_type", "range_value");
			character.setAttribute("name", cname);
			character.setAttribute("from", values[0]);
			character.setAttribute("to", values[1]);
		}else{
			if(cname.compareTo("size")==0){
				String value = cvalue.replaceFirst("\\b("+ChunkedSentence.units+")\\b", "").trim(); //5-10 mm
				String unit = cvalue.replace(value, "").trim();
				if(unit.length()>0){character.setAttribute("unit", unit);}
				cvalue = value;
			}else if(cvalue.indexOf("-c-")>=0 && (cname.compareTo("color") == 0 || cname.compareTo("coloration") ==0)){//-c- set in SentenceOrganStateMarkup
				String color = cvalue.substring(cvalue.lastIndexOf("-c-")+3); //pale-blue
				String m = cvalue.substring(0, cvalue.lastIndexOf("-c-")); //color = blue m=pale
				modifiers = modifiers.length()>0 ? modifiers + ";"+ m : m;
				cvalue = color;
			}
			if(char_type.length() >0){
				character.setAttribute("char_type", char_type);
			}
			character.setAttribute("name", cname);
			character.setAttribute("value", cvalue);
		}
		boolean usedm = false;
		Iterator<Element> it = parents.iterator();
		while(it.hasNext()){
			Element e = it.next();
			character = (Element)character.clone();
			if(modifiers.trim().length() >0){
				addAttribute(character, "modifier", modifiers.trim()); //may not have
				usedm = true;
			}
			results.add(character); //add to results
			e.addContent(character);//add to e
		}
		if(usedm){
			modifiers = "";
		}
		addClauseModifierConstraint(cs, character);
		return modifiers;
	}

	
	
	/**
	 * 
	 * @param parents
	 * @param w: m[usually] 0
	 * @param modifiers
	 * @return
	 */
	private ArrayList<Element> annotateCount(ArrayList<Element> parents, String w, String modifiers) {
		// TODO Auto-generated method stub
		String modifier = w.replaceFirst("\\d.*", "").trim();
		String number= w.replace(modifier, "").trim();
		ArrayList<Element> e = new ArrayList<Element>();
		Element count = new Element("character");
		if(this.inbrackets){count.setAttribute("in_bracket", "true");}
		count.setAttribute("name", "count");
		count.setAttribute("value", number);
		if(modifiers.length()>0){
			this.addAttribute(count, "modifier", modifiers);
		}
		if(modifier.length()>0){
			this.addAttribute(count, "modifier", modifier.replaceAll("(m\\[|\\])", ""));
		}
		Iterator<Element> it= parents.iterator();
		while(it.hasNext()){
			count = (Element)count.clone();
			e.add(count);
			it.next().addContent(count);
		}
		addClauseModifierConstraint(cs, count);
		return e;
	}

	



	//if w has been seen used as a modifier to organ o
	private String constraintType(String w, String o) {
		String result = null;
		w = w.replaceAll("\\W", "");
		String ch = Utilities.lookupCharacter(w, conn, ChunkedSentence.characterhash, this.glosstable, tableprefix);
		if(ch!=null && ch.matches(".*?_?(position|insertion|structure_type)_?.*") && w.compareTo("low")!=0) return "type";
		String sw = Utilities.toSingular(w);
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select * from "+this.tableprefix+"_sentence where tag = '"+w+"' or tag='"+sw+"'");
			if(rs.next()){
				return "parent_organ";
			}
			//rs = stmt.executeQuery("select * from "+this.tableprefix+"_sentence where modifier = '"+w+"' or modifier like '"+w+" %' or modifier like '% "+w+" %' or modifier like '% "+w+"'");
			rs = stmt.executeQuery("select * from "+this.tableprefix+"_sentence where modifier = '"+w+"'");
			if(rs.next()){
				return "type";
			}
			rs.close();
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * output annotated sentence in XML format
	 * Chunk types:
	 * PrepChunk, IVerbChunk (Intransitive verb chunk, followed by a preposition), VerbChunk, ADJChunk, SBARChunk,
	 * etc.
	 * @return
	 */
	/*public Element annotate() throws Exception{
		//query the sentence database for the tag/modifier for this sentence, using this.sentsrc
		//also use the substructure table to resolve of-clauses
		ArrayList<Chunk> chunks = new ArrayList();
		ArrayList<String> structureIDs = new ArrayList();

		this.currentsubject = "fetch from sentence table";
		String modifier = "fetch from sentence table";
		this.currentmainstructure = createStructureElement(this.currentsubject, modifier, this.structid++);
		while(cs.hasNext()){
			Chunk chunk = cs.nextChunk();
			chunks.add(chunk);
			if(chunk instanceof Organ){
				String organ = chunk.getText();				
				if(chunks.size() == 1){
					continue; //this is current subject read from the sentence table
				}else{
					this.statement.addContent(this.currentmainstructure); //add this completed structure
					//create a new structure element
				}
			}else if(chunk instanceof PrepChunk){
				String pphrase = chunk.getText();
				int chunkid = cs.getPointer() - 1;
				Element thiselement = (Element)XPath.selectSingleNode(root, ".\\*[id='"+chunkid+"']"); //IN
				String relationname = thiselement.getAttributeValue("text");
				//create structure(s) from the NPs. e.g "3 florets", character/modifier before organnames 
				//NP may be a list of NPs
				String np = pphrase.replaceFirst("^"+relationname, "").trim();
				ArrayList oids = annotateNP(np); //in which <structure> may be created and inserted into the <statement>
				if(chunks.get(chunks.size()-2) instanceof Organ){ //apex of leaves
					//create a relation
					Element relation = createRelationElement(this.relationid++);
				}else{
					//create a constraint for the last character
				}
			}else if(chunk instanceof SBARChunk){ //SBARChunk could follow any xyzChunk
					
				
			}else if(chunk instanceof SimpleCharacterState){
				//check for its character
				//associate it with current subject
				if(this.currentsubject ==null){
					//save this as a constraint for the to-be-discovered subject 
				}
			}
		}
		
		return statement;
		
	}*/

	public void setInBrackets(boolean b){
		this.inbrackets = b;
	}
	
	/**
	 * 
	 * @param measurements: CI 72 - 75 (74 ), SI 99 - 108 (102 ), PeNI 73 - 83 (73 ), LPeI 46 - 53 (46 ), DPeI 135 - 155 (145 ). 
	 */	
	private void annotatedMeasurements(String measurements) {
		measurements = measurements.replaceAll("–", "-");
		Element whole  = new Element("whole_organism");
		this.statement.addContent(whole);
		ArrayList<Element> parent = new ArrayList<Element>();
		parent.add(whole);
		//select delimitor
		int comma = measurements.replaceAll("[^,]", "").length();
		int semi = measurements.replaceAll("[^;]", "").length();
		String del = comma > semi ? "," : ";";
		String[] values = measurements.split(del);
		for(int i = 0; i < values.length; i++){
			String value = values[i].replaceFirst("[,;\\.]\\s*$", "");
			//separate char from values
			String chara = value.replaceFirst("\\s+\\d.*", "");
			String vstring = value.replaceFirst("^"+chara, "").trim();
			//seperate modifiers from vlu in case there is any
			String vlu = vstring.replaceFirst("\\s+[a-zA-Z].*", "").trim();
			String modifier = vstring.substring(vlu.length()).trim();
			modifier = modifier.length()>0? "m["+modifier+"]" : null;
			vlu = vlu.replaceAll("(?<=\\d)\\s*\\.\\s*(?=\\d)", ".");
			this.annotateNumericals(vlu.trim(), chara.trim(), modifier, parent, false);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
