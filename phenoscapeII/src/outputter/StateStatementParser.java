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
	String qualityclue;
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
	public StateStatementParser(TermOutputerUtilities ontoutil, ArrayList<Entity> keyentities, String qualityclue) {
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
		// relations should include those in this state statement and those in character statement
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

				RelationHandler rh = new RelationHandler(root, relname, toname, toid, fromname, fromid, neg, false);
				rh.handle();
				EQStatement eq = new EQStatement();
				ArrayList<Entity> entities = new ArrayList<Entity>();
				Entity e = rh.getEntity();
				if(e!=null && this.keyentities!=null){
					entities = resolve(e, this.keyentities); 
				}else if(this.keyentities!=null){
					entities = this.keyentities;
				}else{
					continue;
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
				}
				if(rh.otherEQs.size()>0)
				this.EQStatements.addAll(rh.otherEQs);
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		} 
				
		//then parse characters
		List<Element> characters;
		try {
			characters = pathCharacter.selectNodes(statement);
			for(Element character: characters){
				 CharacterHandler ch = new CharacterHandler(root, character, ontoutil); //may contain relational quality
				 ch.handle();
				 ArrayList<Quality> qualities = ch.getQualities();
				 ArrayList<Entity> entities = new ArrayList<Entity>();
				 Entity entity = ch.getEntity();
				 if(entity!=null && this.keyentities!=null){
					 //TODO resolve entity with keyentities
					 entities = resolve(entity, this.keyentities); 
				 }else if(this.keyentities!=null){
					 entities = this.keyentities;
				 }else{
					 continue;
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
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		
		//last, standing alone structures (without characters and are not the subject of a relation)
		//???
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
		return (ArrayList<Entity>) keyentities.clone();
	}

	/**
	 * the state statement may return a spatial element that need to be integrated into the keyentity
	 * @param entity
	 * @return
	 */
	private boolean isSpatial(Entity entityfromstate) {
		// TODO 
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
