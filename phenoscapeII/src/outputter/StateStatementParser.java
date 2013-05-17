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
 *call this parser with entities parsed from CharacterStatement and optionally qualityClue.
 *parse out EQ statements from a state statement, using entities and quality clues from the character statement
 */
public class StateStatementParser extends Parser {
	ArrayList<EQStatement> EQStatements = new ArrayList<EQStatement> (); 
	String src;
	String characterid;
	String stateid;
	String text;
	ArrayList<String> qualityclue;
	ArrayList<Entity> keyentities;
	
	static XPath pathText2;
	static XPath pathRelation;
	static XPath pathRelationUnderCharacter;
	static XPath pathCharacter;
	
	static {
		try{
			pathText2 = XPath.newInstance(".//text");
			pathRelation = XPath.newInstance(".//relation");
			pathRelationUnderCharacter = XPath.newInstance(".//statement[@statement_type='character']/relation");
			pathCharacter = XPath.newInstance(".//character");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 */
	public StateStatementParser(TermOutputerUtilities ontoutil, ArrayList<Entity> keyentities, ArrayList<String> qualityclue) {
		super(ontoutil);
		this.keyentities = keyentities;
		this.qualityclue = qualityclue;
	}

	/* Parse out entities and qualities. 
	 * Reconcile with the entities parsed from characterStatementParser
	 * Form final EQs
	 * 
	 * Process <character> and <relation> one by one
	 * When its subject is unknown, use the entities extracted from CharacterStatementParser
	 * When it has a entity for the subject, reconcile the entities
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void parse(Element statement, Element root) {
		this.src = root.getAttributeValue(ApplicationUtilities.getProperty("source.attribute.name"));
		this.characterid = statement.getAttributeValue("character_id");
		this.stateid = statement.getAttribute("state_id") == null ? "" : statement.getAttributeValue("state_id");
	
		try {
			text = ((Element) pathText2.selectSingleNode(statement)).getTextNormalize();
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		//parse relations first
		//relations should include those in this state statement and those in character statement
		List<Element> relations;
		try {
			relations = pathRelation.selectNodes(statement);
			for(Element relation : relations){
				String fromid = relation.getAttributeValue("from");
				String toid = relation.getAttributeValue("to");
				String relname = relation.getAttributeValue("name").trim();
				boolean neg = Boolean.getBoolean(relation.getAttributeValue("negation"));
				String toname = Utilities.getStructureName(root, toid);
				String fromname = Utilities.getStructureName(root, fromid);
				boolean maybesubject = maybeSubject(root, fromid); //false if fromid appears in constraintid or toid
				RelationHandler rh = new RelationHandler(root, relname, toname, toid, fromname, fromid, neg, false);
				rh.handle();
				EQStatement eq = new EQStatement();
				ArrayList<Entity> entities = new ArrayList<Entity>();
				Entity e = rh.getEntity();
				//e may or may not be the subject of this statement
				//need to reconcil with keyentities only when it is
				//if it is involved in a constraint or as a to-organ in a relation, it is not the subject, so it should not be reconciled with the keyentity
				if(maybesubject && e!=null && this.keyentities!=null){
					entities = resolve(e, this.keyentities); 
				}else if(maybesubject && e==null && this.keyentities!=null){
					entities = this.keyentities;
				}else if (e!=null) {
					entities.add(e);
				}					
				
				for(Entity entity: entities){
					eq.setEntity(entity);
					eq.setQuality(rh.getQuality());
					eq.setSource(this.src);
					eq.setCharacterId(this.characterid);
					eq.setStateId(this.stateid);
					eq.setDescription(text);
					if(this instanceof StateStatementParser) eq.setType("state");
					else eq.setType("character");
					this.EQStatements.add(eq);
					eq=new EQStatement();
				}
				if(rh.otherEQs.size()>0)
				this.EQStatements.addAll(rh.otherEQs);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
				
		///then parse characters
		List<Element> characters;
		try {
			characters = pathCharacter.selectNodes(statement);
			for(Element character: characters){
				String structid = character.getParentElement().getAttributeValue("id"+"");
				boolean maybesubject = maybeSubject(root, structid); //false if fromid appears in constraintid or toid
				 CharacterHandler ch = new CharacterHandler(root, character, ontoutil, qualityclue); //may contain relational quality
				 ch.handle();
				 ArrayList<Quality> qualities = ch.getQualities();
				 ArrayList<Entity> entities = new ArrayList<Entity>();
				 Entity entity = ch.getEntity();
				 if(maybesubject && entity!=null && this.keyentities!=null){
					 entities = resolve(entity, this.keyentities); 
				 }else if(maybesubject && entity==null && this.keyentities!=null){
					 entities = this.keyentities;
				 }else if(entity!= null){
					 entities.add(entity);
				 }

				 for(Entity e: entities){
					 for(Quality quality: qualities){
						 EQStatement eq = new EQStatement();
						 eq.setEntity(e);
						 eq.setQuality(quality);
						 eq.setSource(this.src);
						 eq.setCharacterId(this.characterid);
						 eq.setStateId(this.stateid);
						 eq.setDescription(text);
						 if(this instanceof StateStatementParser) eq.setType("state");
						 else eq.setType("character");
						 this.EQStatements.add(eq);						 
					 }
				 }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//last, standing alone structures (without characters and are not the subject of a relation)
		//???
	}
	
	/**
	 * if structid appears as a constraintid or toid, then it can't be a subject
	 * @param root
	 * @param structid
	 * @return
	 * @throws Exception
	 */
	private boolean maybeSubject(Element root, String structid) throws Exception {
		Element e = (Element) XPath.selectSingleNode(root, ".//character[contains(@constraintid,'"+structid+"')]|.//relation[@toid='"+structid+"']");
		if(e==null) return true;
		return false;
	}

	private ArrayList<Entity> integrateSpatial(Entity entity,
			ArrayList<Entity> keyentities2) {
		
		 //TODO integrate entity with keyentities
		return (ArrayList<Entity>) keyentities2.clone();
	}

	private ArrayList<Entity> resolve(Entity e, ArrayList<Entity> keyentities) {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		if(keyentities==null || keyentities.size()==0){
			entities.add(e);
			return entities;
		}
		if(isSpatial(e)){
			entities = integrateSpatial(e, this.keyentities); 
			 //TODO integrate entity with keyentities
		}

		if(e.getPrimaryEntityString().compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))==0){ //if e is whole_organism
			return (ArrayList<Entity>) keyentities.clone();
		}
		//test part_of relations between e and each of the keyentities
		if(e.isOntologized()){
			for(Entity keye : keyentities){
				if(keye.isOntologized()){
					if(XML2EQ.elk.isPartOf(e.getPrimaryEntityOWLClassIRI(), keye.getPrimaryEntityOWLClassIRI())){
						//keye is entity locator of e
						CompositeEntity ce = new CompositeEntity();
						ce.addEntity(e);
						FormalRelation rel = new FormalRelation();
						rel.setString("part of");
						rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
						rel.setId("BFO:000050");
						rel.setConfidenceScore((float)1.0);
						REntity rentity = new REntity(rel, keye);
						ce.addEntity(rentity);
						keye = ce; //replace keye with the composite entity
					}
				}
			}
			return (ArrayList<Entity>) keyentities.clone();
		}
		
		return (ArrayList<Entity>) keyentities.clone();
	}

	/**
	 * the state statement may return a spatial element that need to be integrated into the keyentity
	 * @param entity
	 * @return
	 */
	private boolean isSpatial(Entity entityfromstate) {
		//TODO
		return false;
	}

	public ArrayList<EQStatement> getEQStatements(){
		return this.EQStatements;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
