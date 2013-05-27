/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import owlaccessor.OWLAccessorImpl;

/**
 * @author updates
 * 
 *         call this parser with entities parsed from CharacterStatement and
 *         optionally qualityClue. parse out EQ statements from a state
 *         statement, using entities and quality clues from the character
 *         statement
 */
public class StateStatementParser extends Parser {
	ArrayList<EQStatementProposals> EQStatements = new ArrayList<EQStatementProposals>();
	String src;
	String characterid;
	String stateid;
	String text;
	ArrayList<String> qualityclue;
	ArrayList<EntityProposals> keyentities;

	static XPath pathText2;
	static XPath pathRelation;
	static XPath pathRelationUnderCharacter;
	static XPath pathCharacter;
	static XPath pathStructure;

	static {
		try {
			pathText2 = XPath.newInstance(".//text");
			pathRelation = XPath.newInstance(".//relation");
			pathRelationUnderCharacter = XPath
					.newInstance(".//statement[@statement_type='character']/relation");
			pathCharacter = XPath.newInstance(".//character");
			pathStructure = XPath.newInstance(".//structure");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public StateStatementParser(TermOutputerUtilities ontoutil,
			ArrayList<EntityProposals> keyentities, ArrayList<String> qualityclue) {
		super(ontoutil);
		this.keyentities = keyentities;
		this.qualityclue = qualityclue;
	}

	/*
	 * Parse out entities and qualities. Reconcile with the entities parsed from
	 * characterStatementParser Form final EQs
	 * 
	 * Process <character> and <relation> one by one When its subject is
	 * unknown, use the entities extracted from CharacterStatementParser When it
	 * has a entity for the subject, reconcile the entities
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void parse(Element statement, Element root) {
		ArrayList<String> StructuredQualities = new ArrayList<String>();
		this.src = root.getAttributeValue(ApplicationUtilities
				.getProperty("source.attribute.name"));
		this.characterid = statement.getAttributeValue("character_id");
		this.stateid = statement.getAttribute("state_id") == null ? ""
				: statement.getAttributeValue("state_id");
		try {
			text = ((Element) pathText2.selectSingleNode(statement))
					.getTextNormalize();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		// parse relations first
		// Check whether tostruct and fromstruct is an entity or quality. 
		// If they are quality, generate qualities using relational quality strategy and map to keyentities
		// else use relationhandler and create entities and qualities.
		List<Element> relations;
		try {
			relations = pathRelation.selectNodes(statement);
			for (Element relation : relations) {
				int flag = 1; //should the relation be retained or ignored. 1 = retain
				for (String structid : StructuredQualities){ //StructuredQualities: initially empty, populated with the ids of 'bad' structures (that are really quliaties") 
					if ((relation.getAttributeValue("from").equals(structid)) //the structure involved in the relation is actually a quality
							|| (relation.getAttributeValue("to").equals(structid))) {
						// relation.detach();
						flag = 0; //bad relation
						break;
					} 
				}
				//process only the good relation
				if (flag == 1) {
					String fromid = relation.getAttributeValue("from");
					String toid = relation.getAttributeValue("to");
					String relname = relation.getAttributeValue("name").trim();
					boolean neg = Boolean.valueOf(relation
							.getAttributeValue("negation"));
					String toname = Utilities.getStructureName(root, toid);
					String fromname = Utilities.getStructureName(root, fromid);
					boolean maybesubject = false;
					List<QualityProposals> q = new ArrayList<QualityProposals>();
					ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
					EntityProposals e;

					// Changes starting => Hariharan
					// checking if entity is really an entity or it is a quality
					// by passing to and from struct names to relational quality
					// strategy.

					Structure2Quality rq1 = new Structure2Quality(root,
							toname, toid, keyentities);
					rq1.handle();

					Structure2Quality rq2 = new Structure2Quality(root,
							fromname, fromid, keyentities);
					rq2.handle();
					if (rq1.qualities.size() > 0) {
						StructuredQualities.addAll(rq1.identifiedqualities);
						e = null;//e is now showed to be a quality
						q.addAll(rq1.qualities);
					}else if (rq2.qualities.size() > 0) {
						StructuredQualities.addAll(rq2.identifiedqualities);
						e = null;//e is now showed to be a quality
						q.addAll(rq2.qualities);
					}else{
						try {
							maybesubject = maybeSubject(root, fromid);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						RelationHandler rh = new RelationHandler(root, relname,
								toname, toid, fromname, fromid, neg, false);
						rh.handle();

						e = rh.getEntity();
						q = new ArrayList<QualityProposals>();
						if(rh.getQuality()!=null) q.add(rh.getQuality());
						if (rh.otherEQs.size() > 0)
							this.EQStatements.addAll(rh.otherEQs);
					}

					// Changes Ending => Hariharan Include flag below to make
					// sure , to include EQ's only when qualities exist.

					if (q.size() > 0) {
						if (maybesubject && e != null
								&& this.keyentities != null) {
							entities = resolve(e, this.keyentities);
						} else if (maybesubject && e == null
								&& this.keyentities != null) {
							entities = this.keyentities;
						} else if (e != null) {
							entities.add(e);
						} else 
							entities = this.keyentities; // what if it is a subject, but not an entit at all? - Hariharan(So added this code)
						//construct EQStatementProposals
						constructureEQStatementProposals(q, entities);


					}

				}
			}
		} catch (JDOMException e) {
			e.printStackTrace();
			System.out.println("");
		}

		//then parse characters. Check, if the parent structure itself is a quality, if so use relationalquality strategy else use characterhandler.
		List<Element> characters;
		try {
			characters = pathCharacter.selectNodes(statement);
			EntityProposals entity = null;
			ArrayList<QualityProposals> qualities = null;
			for (Element character : characters) {
				// may contain relational quality
				String structid = character.getParentElement()
						.getAttributeValue("id" + "");
				String structname = character.getParentElement()
						.getAttributeValue("name" + "");
				boolean maybesubject = false;
				/*RelationalQualityStrategy1 rq2 = checkforquality(root,
						structname, structid, "", "", this.keyentities);*/
				Structure2Quality rq2 = new Structure2Quality(root,
						structname, structid, this.keyentities);
				rq2.handle();
				System.out.print("");
				//if (rq2 != null) {
				if(rq2.qualities.size()>0){
					entity = null;
					qualities = rq2.qualities;
				} else {
					try {
						maybesubject = maybeSubject(root, structid);
						entities.clear();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} // false if fromid appears in constraintid or toid
					CharacterHandler ch = new CharacterHandler(root, character,
							ontoutil, qualityclue); // may contain relational
					// quality
					ch.handle();
					qualities = ch.getQualities();
					ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
					entity = ch.getEntity();
				}
				if (maybesubject && entity != null && this.keyentities != null) {
					// TODO resolve entity with keyentities
					entities = resolve(entity, this.keyentities);
				} else if (maybesubject && entity == null
						&& this.keyentities != null) {
					entities = this.keyentities;
				} else if (entity != null) {
					entities.add(entity);
				} else 
					entities = this.keyentities; // what if it is a subject, but not an entit at all? - Hariharan(So added this code)
				constructureEQStatementProposals(qualities, entities);
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		}

		// last, standing alone structures (without characters 
		// and are not the subject of a relation)
		//TODO: could it really be a quality?

		//if a real entity, construct EQs of 'entity/present' 
		//The following case is handled by the wildcard entity search strategy 	  
		//text::Caudal fin
		//text::heterocercal  (heterocercal tail is a subclass of caudal fin, search "heterocercal *")
		//text::diphycercal
		//=> heterocercal tail: present
		List<Element> structures;
		try{
			structures = pathStructure.selectNodes(statement);
			ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
			for(Element structure: structures){
				String sid = structure.getAttributeValue("id");
				Element relation = (Element) XPath.selectSingleNode(statement, ".//relation[@from='"+sid+"']|.//relation[@to='"+sid+"']");
				if(structure.getChildren().isEmpty() && relation==null){
					//standing-alone structure
					String sname = Utilities.getStructureName(root, sid);
					EntityProposals ep = new EntitySearcherOriginal().searchEntity(root, sid, sname, "", sname, "");
					entities.add(ep);
				}
			}

			if(entities.size()>1){
				//more than one unrelated entity -- isn't it suspicious?
			}
			for(EntityProposals entity: entities){
				if (entity != null && this.keyentities != null) {
					// TODO resolve entity with keyentities
					entities = resolve(entity, this.keyentities);
				} else if (entity == null && this.keyentities != null) {
					entities = this.keyentities;
				} else if (entity != null) {
					entities.add(entity);
				}
				ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>();
				QualityProposals qp = new QualityProposals();
				Quality q = new Quality();
				q.setString("present");
				q.setId("PATO:0000467");
				q.setLabel("present");
				q.setConfidenceScore(1f);
				qp.add(q);
				qualities.add(qp);
				constructureEQStatementProposals(qualities, entities);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	private void constructureEQStatementProposals(
			List<QualityProposals> qualities, ArrayList<EntityProposals> entities) {

		for (QualityProposals qualityp : qualities){
			for (EntityProposals entityp : entities) {
				EQStatementProposals eqp = new EQStatementProposals();
				for(Quality quality: qualityp.getProposals()){
					for(Entity entity: entityp.getProposals()){
						EQStatement eq= new EQStatement();
						eq.setEntity(entity);
						if (quality instanceof RelationalQuality) {
							eq.setQuality(((RelationalQuality) quality));
							//							if (entity.getPrimaryEntityLabel() == ((RelationalQuality) quality).relatedentity
							//									.getPrimaryEntityLabel()){
							//								continue;
							//							} //don't recall what the above does --Hong May 20, 13.
						} else{
							eq.setQuality(quality);
						}
						eq.setSource(this.src);
						eq.setCharacterId(this.characterid);
						eq.setStateId(this.stateid);
						eq.setDescription(text);
						if (this instanceof StateStatementParser){
							eq.setType("state");
						}else{
							eq.setType("character");
						}
						//this.EQStatements.add(eq);
						//eq = new EQStatement();
						eqp.add(eq);
					}
				}
				this.EQStatements.add(eqp);
			}
		}
	}

	/**
	 * if structid appears as a constraintid or toid, then it can't be a subject
	 * 
	 * @param root
	 * @param structid
	 * @return
	 * @throws Exception
	 */
	private boolean maybeSubject(Element root, String structid)
			throws Exception {
		Element e = (Element) XPath.selectSingleNode(root,
				".//character[contains(@constraintid,'" + structid
				+ "')]|.//relation[@to='" + structid + "']");
		if (e == null)
			return true;
		return false;
	}

	private ArrayList<EntityProposals> integrateSpatial(EntityProposals entity,
			ArrayList<EntityProposals> keyentities2) {

		// TODO integrate entity with keyentities
		return (ArrayList<EntityProposals>) keyentities2.clone();
	}

	//TODO: resolve better between proposals
	private ArrayList<EntityProposals> resolve(EntityProposals e, ArrayList<EntityProposals> keyentities) {
		ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
		//no keyentities 
		if (keyentities == null || keyentities.size() == 0) {
			entities.add(e);
			return entities;
		}

		//find a place for the spatial terms
		if (containsSpatial(e)) {
			entities = integrateSpatial(e, this.keyentities);
			// TODO integrate entity with keyentities
		}

		if (e.getPhrase().replace("_", " ").compareTo(ApplicationUtilities.getProperty("unknown.structure.name")) == 0) { // if
			// e
			// is
			// whole_organism
			return (ArrayList<EntityProposals>) keyentities.clone();
		}
		// test subclass relations between all e proposals and each of the keyentities proposals
		ArrayList<EntityProposals> results = resolveBaseOnSubclassRelation(e, keyentities);
		if(results==null){
			results = resolveBaseOnPartOfRelation(e, keyentities);
		}

		if(results!=null)
			return results;

		return (ArrayList<EntityProposals>) keyentities.clone();
	}



	private ArrayList<EntityProposals> resolveBaseOnPartOfRelation(EntityProposals e, ArrayList<EntityProposals> keyentities){
		// test part_of relations between all e proposals and each of the keyentities proposals
		if (e.hasOntologizedWithHighConfidence()) {
			for (Entity entity: e.getProposals()){
				for (EntityProposals keye : keyentities) {
					for(Entity key: keye.getProposals()){
						if (XML2EQ.elk.isPartOf(entity.getPrimaryEntityOWLClassIRI(),
								key.getPrimaryEntityOWLClassIRI())) {
							// key is entity locator of e
							System.out.println("");
							CompositeEntity ce = new CompositeEntity();
							ce.addEntity(entity);
							FormalRelation rel = new FormalRelation();
							rel.setString("part of");
							rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
							rel.setId("BFO:000050");
							rel.setConfidenceScore((float) 1.0);
							REntity rentity = new REntity(rel, key);
							ce.addEntity(rentity);
							key = ce; // replace key with the composite entity in keyentities
						}
					}
				}
			}
			return (ArrayList<EntityProposals>) keyentities.clone();
		}		
		return null;
	}

	/**
	 * test subclass relations between all e proposals and each of the keyentities proposals
	 * if positive for any test, resolve to the subclass
	 * @param e
	 * @param keyentities
	 * @return
	 */
	private ArrayList<EntityProposals> resolveBaseOnSubclassRelation(EntityProposals e, ArrayList<EntityProposals> keyentities){
		if (e.hasOntologizedWithHighConfidence()) {
			for (Entity entity: e.getProposals()){
				for (EntityProposals keye : keyentities) {
					for(Entity key: keye.getProposals()){
						if (XML2EQ.elk.isSubClassOf(entity.getPrimaryEntityOWLClassIRI(),
								key.getPrimaryEntityOWLClassIRI())) {
							// reset key to the subclass
							//System.out.println("");
							keye.reset();
							keye.add(entity);
							keye.setPhrase(entity.getString());
							/*CompositeEntity ce = new CompositeEntity();
							ce.addEntity(entity);
							FormalRelation rel = new FormalRelation();
							rel.setString("part of");
							rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
							rel.setId("BFO:000050");
							rel.setConfidenceScore((float) 1.0);
							REntity rentity = new REntity(rel, key);
							ce.addEntity(rentity);
							key = ce; // replace key with the composite entity in keyentities*/
						}
					}
				}
			}
			return (ArrayList<EntityProposals>) keyentities.clone();
		}		
		return null;
	}

	/**
	 * the state statement may return a spatial element that need to be
	 * integrated into the keyentity
	 * 
	 * @param entity
	 * @return
	 */
	private boolean containsSpatial(EntityProposals entityfromstate) {
		// TODO
		return false;
	}

	public ArrayList<EQStatementProposals> getEQStatements() {
		return this.EQStatements;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
