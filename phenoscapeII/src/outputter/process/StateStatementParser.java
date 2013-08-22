/**
 * 
 */
package outputter.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import outputter.ApplicationUtilities;
import outputter.Utilities;
import outputter.XML2EQ;
import outputter.data.CompositeEntity;
import outputter.data.EQProposals;
import outputter.data.Entity;
import outputter.data.EntityProposals;
import outputter.data.FormalRelation;
import outputter.data.Quality;
import outputter.data.QualityProposals;
import outputter.data.REntity;
import outputter.data.SimpleEntity;
import outputter.knowledge.Dictionary;
import outputter.knowledge.TermOutputerUtilities;
import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Cui
 * 
 *         call this parser with entities parsed from CharacterStatement and
 *         optionally qualityClue. parse out EQ statements from a state
 *         statement, using entities and quality clues from the character
 *         statement
 */
public class StateStatementParser extends Parser {
	//ArrayList<EQStatementProposals> EQStatements = new ArrayList<EQStatementProposals>();
	private static final Logger LOGGER = Logger.getLogger(StateStatementParser.class);   
	protected ArrayList<EQProposals> EQStatements = new ArrayList<EQProposals>();
	//String src;
	//String characterid;
	//String stateid;
	//String characterlabel;
	//String text;
	ArrayList<String> qualityclue;
	ArrayList<EntityProposals> keyentities; //use this when changes made here are used somewhere else.
	//ArrayList<EntityProposals> keyentitiesclone; //use this for local operation

	static XPath pathText2;
	static XPath pathRelation;
	static XPath pathRelationUnderCharacter;
	static XPath pathCharacter;
	static XPath pathPostCompCharacter;
	static XPath pathStructure;

	static {
		try {
			pathText2 = XPath.newInstance(".//text");
			pathRelation = XPath.newInstance(".//relation");
			pathRelationUnderCharacter = XPath
					.newInstance(".//statement[@statement_type='character']/relation");
			pathCharacter = XPath.newInstance(".//character");
			pathPostCompCharacter = XPath.newInstance(".//character[@is_modifier='true'][@name!='count']");
			pathStructure = XPath.newInstance(".//structure");
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	/**
	 * @param characterlabel 
	 * 
	 */
	public StateStatementParser(TermOutputerUtilities ontoutil,
			ArrayList<EntityProposals> keyentities, ArrayList<String> qualityclue, String characterlabel) {
		super(ontoutil);
		this.keyentities = keyentities;
		this.qualityclue = qualityclue;
		//this.characterlabel = characterlabel;
	}

	private ArrayList<EntityProposals> clone(ArrayList<EntityProposals> keyentities) {
		if(keyentities == null) return null;
		ArrayList<EntityProposals> keyentitiesclone = new ArrayList<EntityProposals>();
		for(int i = 0; i < keyentities.size(); i++){
			EntityProposals ep = keyentities.get(i);
			EntityProposals epclone = new EntityProposals();
			for(int j = 0; j < ep.getProposals().size(); j++){
				epclone.add(ep.getProposals().get(j));
			}
			keyentitiesclone.add(epclone);
		}
		return keyentitiesclone;
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
	public void parse(Element statement, Element root, EQProposals empty) {
		//refactored by extracting the following three methods
		//LOGGER.debug("StateStatementParser: parsing metadata...");
		//parseMetadata(statement, root);
		LOGGER.debug("StateStatementParser: parsing relations...");
		parseRelationsFormEQ(statement, root, empty);
		LOGGER.debug("StateStatementParser: parsing characters...");
		parseCharactersFormEQ(statement, root, empty);
		LOGGER.debug("StateStatementParser: parsing standalone structures...");
		parseStandaloneStructures(statement, root, empty); //not needed by BinaryCharacterStatementParser.
		//populate characterlabel, if it is null
		//populateCharacterLabel();
	}


	/*protected void parseMetadata(Element statement, Element root) {
		this.src = root.getAttributeValue(ApplicationUtilities
				.getProperty("source.attribute.name"));
		this.characterid = statement.getAttributeValue("character_id");
		this.stateid = statement.getAttribute("state_id") == null ? ""
				: statement.getAttributeValue("state_id");
		try {
			text = ((Element) pathText2.selectSingleNode(statement))
					.getTextNormalize();
		} catch (JDOMException e) {
			LOGGER.error("", e);
		}
	}*/

	/**
	 * last, standing alone structures (without characters 
		and are not the subject of a relation or a character constraint)
		//TODO: could it really be a quality?
	 * @param statement
	 * @param root
	 * @param empty
	 */
	@SuppressWarnings("unchecked")
	private void parseStandaloneStructures(Element statement, Element root, EQProposals empty) {


		//if a real entity, construct EQs of 'entity/present' 
		//The following case is handled by the wildcard entity search strategy 	  
		//text::Caudal fin
		//text::heterocercal  (heterocercal tail is a subclass of caudal fin, search "heterocercal *")
		//text::diphycercal
		//=> heterocercal tail: present
		List<Element> structures;
		try{
			//identify standalone local entities
			structures = pathStructure.selectNodes(statement);
			ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
			for(Element structure: structures){
				String sid = structure.getAttributeValue("id");
				Element relation = (Element) XPath.selectSingleNode(statement, ".//relation[@from='"+sid+"']|.//relation[@to='"+sid+"']|.//*[@constraintid='"+sid+"']");
				if(structure.getChildren().isEmpty() && relation==null && structure.getAttributeValue("processed")==null){
					//standing-alone structure: could be entity or quality
					String sname = Utilities.getStructureName(root, sid);		
					EntityParser ep = new EntityParser(statement, root, sid, sname, keyentities, this instanceof BinaryCharacterStatementParser);
					if(ep.getEntity()!=null){
						entities.addAll(ep.getEntity()); //if found entities, take them
					}else if(ep.getQualityStrategy()!=null){
						ArrayList<QualityProposals> qualities = ep.getQualityStrategy().qualities;
						ArrayList<EntityProposals> primentities = ep.getQualityStrategy().primaryentities;
						//this.constructEQStatementProposals(qualities, primentities);	
						this.constructEQProposals(qualities, primentities, empty);	
					}
					//ArrayList<EntityProposals> ep = new EntitySearcherOriginal().searchEntity(root, sid, sname, "", sname, "");
					//entities.addAll(ep);
				}
			}

			if(entities.size()>1){
				//more than one unrelated entity -- isn't it suspicious?
			}
			//resolve between local entities and keyentities extracted from character statement
			//construct EQs using resolved entities
			ArrayList<EntityProposals> entities1 = new ArrayList<EntityProposals>();
			for(EntityProposals entity: entities){
				ArrayList<EntityProposals> primaryEntities1 = new ArrayList<EntityProposals>();
				ArrayList<EntityProposals> keyentitiesclone =  clone(this.keyentities);
				primaryEntities1.add(entity);
				if (entity != null && this.keyentities != null) {
					// TODO resolve entity with keyentities
					entities1 = resolve(primaryEntities1, keyentitiesclone);
				} else if (entity == null && this.keyentities != null) {
					entities1 = keyentitiesclone; //so this.keyentities won't be changed accidentally
				} else if (entity != null) {
					entities1.add(entity);
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
				//constructEQStatementProposals(qualities, entities1);
				constructEQProposals(qualities, entities1, empty);
				entities1.clear();
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}

	protected void parseCharactersFormEQ(Element statement, Element root, EQProposals empty) {
		//then parse characters. 
		try {
			ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
			ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>();
			
			//if any one of the characters is a post-comp quality, the entity needs to be post-comped
			List<Element> postcompchars = pathPostCompCharacter.selectNodes(statement);
			ArrayList<QualityProposals> postcomps = new ArrayList<QualityProposals>();
			for(Element character: postcompchars){
				entities = new ArrayList<EntityProposals>();
				qualities = new ArrayList<QualityProposals>();
				if(character.getParentElement()==null) continue;
				if(character.getAttributes().size()==2 && character.getAttribute("value")!=null && character.getAttributeValue("value").matches(Dictionary.STOP)) continue;
				parserCharacter(character, statement, root, entities, qualities);
				postcomps.addAll(qualities);
			}
			if(postcomps.size()>0){
				LOGGER.debug("SSP: found postcomp qualities: ");
				for(QualityProposals qp: postcomps){
					LOGGER.debug(".."+qp.toString());
				}
			}
			Utilities.postcompose(entities, postcomps); //postcomp the entities 
														//assuming the same entities being returned in the loop
														
			//other qualities
			List<Element> characters = pathCharacter.selectNodes(statement);	
			characters.removeAll(postcompchars);
			
			if(characters.size()<=0){
				qualities = new ArrayList<QualityProposals>();//reset qualities, as they are post-compsed into entities
				constructEQProposals(qualities, entities, empty);
			}else{
				ArrayList<EntityProposals> lastentities = null;
				for (Element character : characters) {		
					LOGGER.debug("SSP: parsing character '"+character.getAttributeValue("value")+"'....");
					entities = new ArrayList<EntityProposals>();
					qualities = new ArrayList<QualityProposals>();
					if(character.getParentElement()==null) continue;
					if(character.getAttributes().size()==2 && character.getAttribute("value")!=null && character.getAttributeValue("value").matches(Dictionary.STOP)) continue;
					parserCharacter(character, statement, root, entities, qualities);
					if(entities!=null && entities.size()>0) lastentities = entities; //remember the last entity
					if(entities==null || entities.size()==0) entities = lastentities; //if no entity found, use the last entity
					LOGGER.debug("SSP: parsed entities:");
					if(entities!=null) for(EntityProposals ep: entities) LOGGER.debug(".."+ep.toString());
					LOGGER.debug("SSP: parsed qualities:");
					if(qualities!=null) for(QualityProposals qp: qualities) LOGGER.debug(".."+qp.toString());
					//constructEQStatementProposals(qualities, entities);
					Utilities.postcompose(entities, postcomps);
					constructEQProposals(qualities, entities, empty);
				}
			}
			
		} catch (JDOMException e) {
			LOGGER.error("", e);
		}
	}

	/**
	 * parse character and resolve for entity
	 * @param character
	 * @param statement
	 * @param root
	 * @param entities
	 * @param qualities
	 */
	public void parserCharacter(Element character, Element statement, Element root, ArrayList<EntityProposals> entities, ArrayList<QualityProposals> qualities){
		ArrayList<EntityProposals> entity = null;
		boolean donotresolve=false;
		// may contain relational quality
		String structid = character.getParentElement()
				.getAttributeValue("id" + "");
		//String structname = character.getParentElement()
		//		.getAttributeValue("name" + "");
		boolean maybesubject = false;

		//Structure2Quality rq2 = new Structure2Quality(root,
		//		structname, structid, this.keyentities);
		//rq2.handle();
		//if(rq2.qualities.size()>0){
		//	entity = rq2.primaryentities;
		//  qualities = rq2.qualities;
		//} else {
		try {
			maybesubject = maybeSubject(root, structid);// false if fromid appears in constraintid or toid
		} catch (Exception e1) {
			e1.printStackTrace();
		} 
		//TODO: redundant parsing of shared entity of the characters?
		

		CharacterHandler ch = new CharacterHandler(root, character,
				ontoutil, qualityclue,this.keyentities, false); // may contain relational quality
		ch.handle();
		qualities.addAll(ch.getQualities());
		entity = ch.getPrimaryentities();
		donotresolve=ch.donotresolve;
		//}
		if(donotresolve == false)
		{
			ArrayList<EntityProposals> keyentitiesclone =  clone(this.keyentities);
			if (maybesubject && entity != null && this.keyentities != null) {
				// TODO resolve entity with keyentities
				entities.addAll(resolve(entity,keyentitiesclone));
			} else if (maybesubject && entity == null
					&& this.keyentities != null) {
				entities.addAll(keyentitiesclone);
			} else if (entity != null) {
				entities.addAll(entity);
			} else{ 
				entities.addAll(keyentitiesclone); 
				// what if it is a subject, but not an entity at all? - Hariharan(So added this code)
				//hong: isn't this already handled above?
			}
		}
		else
		{
			entities.addAll(entity);
		}

		//This is for resolving final entities with entity in parts=> iliac blade: Flared at the proximal end 
		if(ch.entityparts.size()>0)
		{
			entities.addAll(resolveFinalEntities(entities,ch.entityparts));
		}
		
	}
	
	
	

	/**
	 * This is for resolving final entities with entity in parts, e.g.:
	 * iliac blade: Flared at the proximal end (here part is proximal end)
	 * dorsal fin: absent in both sexes (here part is dorsal fin)
	 * @param entities
	 * @param entityparts
	 * @return
	 */
	private ArrayList<EntityProposals> resolveFinalEntities(ArrayList<EntityProposals> entities,
			ArrayList<EntityProposals> entityparts) {

		ArrayList<EntityProposals> finalentities = new ArrayList<EntityProposals>();
		if((entities!=null)&&entities.size()>0)
		{
			for(EntityProposals ep1: entityparts)
			{
				for(Entity e1:ep1.getProposals())
				{
					//if e1 contains spatial term, it is part
					//else e1 is the parent
					boolean foundpart = false;
					if(e1.getString().matches(".*?\\b("+Dictionary.spatialtermptn+")\\b.*")) foundpart = true;
					for(EntityProposals ep2: entities)
					{
						EntityProposals ep3 = new EntityProposals();
						for(Entity e2: ep2.getProposals())
						{
							FormalRelation rel = Dictionary.partof;
							if(foundpart){				
								rel.setConfidenceScore((float)1.0);
								REntity rentity = new REntity(rel, e2);
								CompositeEntity centity = new CompositeEntity();
								//composite entity
								if(e1 instanceof SimpleEntity)
								{
									centity.addEntity(e1);
									centity.addEntity(rentity);
								}
								else
								{
									centity = ((CompositeEntity) e1).clone();
									((CompositeEntity)centity).addParentEntity(rentity);
								}
								ep3.add(centity);
							}else{							
								rel.setConfidenceScore((float)1.0);
								REntity rentity = new REntity(rel, e1);
								CompositeEntity centity = new CompositeEntity();
								//composite entity
								if(e2 instanceof SimpleEntity)
								{
									centity.addEntity(e2);
									centity.addEntity(rentity);
								}
								else
								{
									centity = ((CompositeEntity) e2).clone();
									((CompositeEntity)centity).addParentEntity(rentity);
								}
								ep3.add(centity);
							}
						}
						finalentities.add(ep3);
					}
				}
			}
		}
		entities.clear();
		return finalentities;
	}


	/*
	 * If the character label is null, populate it in the final eqproposal
	 * 
	 * 
	 */
	
	/*private void populateCharacterLabel() {

		//sets the characterlabel
		for(EQProposals eqp:this.EQStatements)
		{
			if((eqp.getCharacterlabel()==null)||(eqp.getCharacterlabel()==""))
			{
				eqp.setCharacterlabel(this.characterlabel);
			}
		}
	}*/
	
	
	/*
	 protected void parseCharacters(Element statement, Element root) {
		//then parse characters. Check, if the parent structure itself is a quality, if so use relationalquality strategy else use characterhandler.
		List<Element> characters;
		try {
			characters = pathCharacter.selectNodes(statement);
			ArrayList<EntityProposals> entity = null;
			ArrayList<QualityProposals> qualities = null;
			for (Element character : characters) {
				// may contain relational quality
				if(character.getParentElement()==null)
					continue;
				String structid = character.getParentElement()
						.getAttributeValue("id" + "");
				String structname = character.getParentElement()
						.getAttributeValue("name" + "");
				boolean maybesubject = false;
				ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
				Structure2Quality rq2 = new Structure2Quality(root,
						structname, structid, this.keyentities);
				rq2.handle();
				//if (rq2 != null) {
				if(rq2.qualities.size()>0){
					entity = rq2.primaryentities;
					qualities = rq2.qualities;
				} else {
					try {
						maybesubject = maybeSubject(root, structid);
						//entities.clear();
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} // false if fromid appears in constraintid or toid
					CharacterHandler ch = new CharacterHandler(root, character,
							ontoutil, qualityclue,this.keyentities); // may contain relational
					// quality
					ch.handle();
					qualities = ch.getQualities();
					entity = ch.getPrimaryentities();
					donotresolve=ch.donotresolve;
				}
				if(donotresolve==false)
				{
				ArrayList<EntityProposals> keyentitiesclone =  clone(this.keyentities);
				if (maybesubject && entity != null && this.keyentities != null) {
					// TODO resolve entity with keyentities
					entities = resolve(entity,keyentitiesclone);
				} else if (maybesubject && entity == null
						&& this.keyentities != null) {
					entities = keyentitiesclone;
				} else if (entity != null) {
					entities.addAll(entity);
				} else{ 
					entities = keyentitiesclone; 
					// what if it is a subject, but not an entity at all? - Hariharan(So added this code)
					//hong: isn't this already handled above?
				}
				}
				else
				{
					entities.addAll(entity);
				}
					constructureEQStatementProposals(qualities, entities);
			}
		} catch (JDOMException e) {
			LOGGER.error("", e);
		}
	}
	 */
	/**
	 * parse relations first
		Check whether tostruct and fromstruct is an entity or quality. 
		If they are quality, generate qualities using relational quality strategy and map to keyentities
		else use relationhandler and create entities and qualities.
	 * @param statement
	 * @param root
	 */

	protected void parseRelationsFormEQ(Element statement, Element root, EQProposals empty) {
		ArrayList<String> StructuredQualities = new ArrayList<String>();//scope of this variable?
		List<Element> relations;
		try {
			relations = pathRelation.selectNodes(statement);
			for (Element relation : relations) {
				ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
				List<QualityProposals> q = new ArrayList<QualityProposals>();
				int flag = parseRelation(relation, root, StructuredQualities, entities,  q, empty);
				if(flag==1){
					//constructEQStatementProposals(q, entities);
					constructEQProposals(q, entities, empty);
				}
			}
		} catch (JDOMException e) {
			LOGGER.error("", e);
		}
	}


	public int parseRelation(Element relation, Element root, ArrayList<String> StructuredQualities, ArrayList<EntityProposals> entities,  List<QualityProposals> q, EQProposals empty){
		int flag = 1; //should the relation be retained or ignored. 1 = retain
		for (String structid : StructuredQualities){ //StructuredQualities: initially empty, populated with the ids of 'bad' structures (that are really quliaties") 
			if ((relation.getAttributeValue("from").equals(structid)) //the structure involved in the relation is actually a quality
					|| (relation.getAttributeValue("to").equals(structid))) {
				// relation.detach();
				flag = 0; //bad relation
				LOGGER.debug("break on bad relation: "+relation.getAttributeValue("id"));
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
			ArrayList<EntityProposals> e = new ArrayList<EntityProposals>();
			ArrayList<EntityProposals> spatialmodifier = null;
			ArrayList<EntityProposals> entitylocator =null;
			// Changes starting => Hariharan
			// checking if entity is really an entity or it is a quality
			// by passing to and from struct names to relational quality
			// strategy.
			
			//moved this logic into EntityParser which will be called by RelationHandler later
			//Structure2Quality rq1 = new Structure2Quality(root,
			//		fromname, fromid, keyentities);
			//rq1.handle();

			/*Structure2Quality rq2 = null;
			if(toid.indexOf(" ")<0){ //temp solution, multiple ids in toid is handled in S2Q process in fromname
				rq2 = new Structure2Quality(root,
						toname, toid, keyentities);
				rq2.handle();
			}*/

			/*if (rq1.qualities.size() > 0) {
				StructuredQualities.addAll(rq1.identifiedqualities);
				if(rq1.primaryentities.size()>0)
					e = rq1.primaryentities;//e is now showed to be a quality
				q.addAll(rq1.qualities);
				//This ensures that characters are not detached until they have been used up to form meaningful EQStatements
				if((this.keyentities!=null)||(e.size()>0))
					rq1.detach_character();

				if(rq1.spatialmodifier!=null)
					spatialmodifier=rq1.spatialmodifier;

			}/*else if (rq2.qualities.size() > 0) {
				StructuredQualities.addAll(rq2.identifiedqualities);
				if(rq2.primaryentities.size()>0)
					e = rq2.primaryentities;//e is now showed to be a quality
				q.addAll(rq2.qualities);
				//This ensures that characters are not detached until they have been used up to form meaningful EQStatements
				if((this.keyentities!=null)||(e.size()>0))
					rq2.detach_character();
				if(rq2.spatialmodifier!=null)
					spatialmodifier=rq2.spatialmodifier;
			}*/
			//else{
				try {
					maybesubject = maybeSubject(root, fromid);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				RelationHandler rh = new RelationHandler(root, relname, relation,
						toname, toid, fromname, fromid, neg, this.keyentities, false);
				rh.handle();
				if(rh.getEntity()!=null)
				{
					e.addAll(rh.getEntity());
				}

				if(rh.getQuality()!=null) //including s2q identified qualities
				{
					q.addAll(rh.getQuality());
				}
				if (rh.otherEQs!=null && rh.otherEQs.size() > 0)
				{
					addMeta(rh.otherEQs, empty);
					this.EQStatements.addAll(rh.otherEQs);
				}
				if(rh.entitylocator!=null)
				{
					entitylocator = rh.entitylocator;
				}
				
				if(rh.spatialmodifier!=null)
				{
					spatialmodifier = rh.spatialmodifier;
				}
				
				if(rh.identifiedqualities!=null)
				{
					StructuredQualities.addAll(rh.identifiedqualities);
				}

			//}

			// Changes Ending => Hariharan Include flag below to make
			// sure , to include EQ's only when qualities exist.

			ArrayList<EntityProposals> keyentitiesclone =  clone(this.keyentities);
			if (maybesubject && e.size()>0
					&& this.keyentities != null) {
				entities.clear();
				entities.addAll(resolve(e, keyentitiesclone));
			} else if (maybesubject && e.size()==0
					&& keyentitiesclone != null) {
				entities.clear();
				entities.addAll(keyentitiesclone); //to avoid accidental changes to this.keyentities
			} else if (e.size()>0) {
				entities.clear();
				entities.addAll(e);
			} else if(keyentitiesclone != null) {
				entities.clear();
				entities.addAll(keyentitiesclone); 
				// what if it is a subject, but not an entit at all? - Hariharan(So added this code)
				//hong: hasn't this case been handled already above?
			}

			//resolve final entities with spatial modifiers
			if(spatialmodifier!=null)
			{
				ArrayList<EntityProposals> spatialmodifiers = new ArrayList<EntityProposals>();
				spatialmodifiers.addAll(spatialmodifier);
				entities.clear();
				entities.addAll(resolveFinalEntities(entities,spatialmodifiers));
				//entities = resolveFinalEntities(entities,spatialmodifiers);
			}

			if(entitylocator!=null)
			{
				ArrayList<EntityProposals> entitylocators = new ArrayList<EntityProposals>();
				entitylocators.addAll(entitylocator);		
				entities.clear();
				entities.addAll(resolveFinalEntities(entitylocators,entities));
				//entities = resolveFinalEntities(entitylocators,entities);
			}

			if(q.size()==0)
			{
				Quality present = new Quality();
				present.setString("present");
				present.setLabel("PATO:present");
				present.setId("PATO:0000467");
				present.setConfidenceScore((float)1.0);

				QualityProposals qp = new QualityProposals();
				qp.add(present);

				q.add(qp);
			}
		}
		return flag;
	}

	/**
	 * copy meta info about the EQ to otherEQs
	 * @param otherEQs
	 * @param empty
	 */
	private void addMeta(ArrayList<EQProposals> EQs, EQProposals empty) {
		if(empty==null) return;
		
		for(EQProposals EQ: EQs){
			EQ.setSource(empty.getSource());
			EQ.setCharacterId(empty.getCharacterId());
			EQ.setCharacterText(empty.getCharacterText());
			EQ.setStateId(empty.getStateId());
			EQ.setStateText(empty.getStateText());
			EQ.setType(empty.getType());
		}
	}

	private void constructEQProposals(List<QualityProposals> qualities, ArrayList<EntityProposals> entities, EQProposals empty){
		if(entities!=null && entities.size()>0 && qualities!=null && qualities.size()>0){
			for (QualityProposals qualityp : qualities){
				for (EntityProposals entityp : entities) {
					//EQProposals eqp = new EQProposals();
					EQProposals eqp = empty.clone();
					eqp.setEntity(entityp);
					eqp.setQuality(qualityp);
					//eqp.setSource(this.src);
					//eqp.setCharacterId(this.characterid);
					//eqp.setStateId(this.stateid);
					//eqp.setDescription(text);
					//eqp.setCharacterlabel(this.characterlabel);
					if (this instanceof StateStatementParser){
						eqp.setType("state");
					}else{
						eqp.setType("character");
					}
					this.EQStatements.add(eqp);
				}
			}			
		} else if(entities!=null && entities.size()>0 && this instanceof BinaryCharacterStatementParser){ //no qualities identified so far
				for (EntityProposals entityp : entities) {
					//EQProposals eqp = new EQProposals();
					EQProposals eqp = empty.clone();
					eqp.setEntity(entityp);
					eqp.setQuality(null); //this may be filled later for BinaryStateStatements
					//eqp.setSource(this.src);
					//eqp.setCharacterId(this.characterid);
					//eqp.setStateId(this.stateid);
					//eqp.setDescription(text);
					//eqp.setCharacterlabel(this.characterlabel);
					if (this instanceof StateStatementParser){
						eqp.setType("state");
					}else{
						eqp.setType("character");
					}
					this.EQStatements.add(eqp);
				}					
		}
		
	}
	
	
	/*private void constructEQStatementProposals(
			List<QualityProposals> qualities, ArrayList<EntityProposals> entities) {
		if((entities!=null)&&(entities.size()>0))
			for (QualityProposals qualityp : qualities){
				for (EntityProposals entityp : entities) {
					EQStatementProposals eqp = new EQStatementProposals();
					for(Quality quality: qualityp.getProposals()){
						for(Entity entity: entityp.getProposals()){
							EQStatement eq= new EQStatement();
							eq.setEntity(entity);
							eq.setQuality(quality);
							eq.setSource(this.src);
							eq.setCharacterId(this.characterid);
							eq.setStateId(this.stateid);
							eq.setDescription(text);
							if (this instanceof StateStatementParser){
								eq.setType("state");
							}else{
								eq.setType("character");
							}

							eqp.add(eq);
						}
					}
					this.EQStatements.add(eqp);
				}
			}
	}*/

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

	private ArrayList<EntityProposals> integrateSpatial(ArrayList<EntityProposals> e,
			ArrayList<EntityProposals> keyentities2) {

		// TODO integrate entity with keyentities
		return (ArrayList<EntityProposals>) clone(keyentities2);
	}

	//TODO: resolve better between proposals
	private ArrayList<EntityProposals> resolve(ArrayList<EntityProposals> e, ArrayList<EntityProposals> keyentities) {
		ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
		//no keyentities 
		if (keyentities == null || keyentities.size() == 0) {
			entities.addAll(e);
			return entities;
		}

		if(e==null || e.size()==0){
			return keyentities;
		}

		//find a place for the spatial terms
		if (containsSpatial(e)) {
			entities = integrateSpatial(e, keyentities);
			// TODO integrate entity with keyentities
		}

		Iterator entityitr =e.listIterator();
		while(entityitr.hasNext())
		{
			// if e is whole_organism
			EntityProposals ep = (EntityProposals) entityitr.next();
			if (ep.getPhrase().replace(" ", "_").compareTo(ApplicationUtilities.getProperty("unknown.structure.name")) == 0 || 
					ep.getPhrase().compareTo(ApplicationUtilities.getProperty("unknown.structure.name")) == 0) 
				entityitr.remove();
		}


		// test subclass relations between all e proposals and each of the keyentities proposals

		ArrayList<EntityProposals> results = resolveBaseOnSubclassRelation(e, keyentities);
		if(results==null){
			results = resolveBaseOnPartOfRelation(e, keyentities);
		}
		//TODO need to add code for resolving bilateral structure
		if(results!=null)
			return results;

		
		return (ArrayList<EntityProposals>) keyentities; //best of the three
		//return (ArrayList<EntityProposals>) e.clone(); //2nd
		//*TODO should return 'e part of keyentities' or simply 'e', not keyentities? //worst
		/*ArrayList<EntityProposals> resolved = new ArrayList<EntityProposals> ();
		for(EntityProposals ep: e){
			EntityProposals updated = new EntityProposals();
			for(Entity en: ep.getProposals()){
				for(EntityProposals epl: keyentities){
					if(ep.equals(epl)) break;
					for(Entity enl: epl.getProposals()){
						if(en.equals(enl)) break;
						CompositeEntity ce = new CompositeEntity();
						if(en instanceof CompositeEntity){
							ce.addEntity(en.clone());//caused infinite nesting without cloning
							ce.addParentEntity(new REntity(Dictionary.partof, enl.clone()));
							updated.add(ce);
						}else{
							ce.addEntity(en.clone());
							ce.addEntity(new REntity(Dictionary.partof, enl.clone()));
							updated.add(ce);
						}
					}
				}
			}
			resolved.add(updated);
		}
		return (ArrayList<EntityProposals>) resolved;*/
	}




	private ArrayList<EntityProposals> resolveBaseOnPartOfRelation(ArrayList<EntityProposals> eProposals, ArrayList<EntityProposals> keyentities){
		// test part_of relations between all e proposals and each of the keyentities proposals
		int flag=0;
		for(EntityProposals e: eProposals)
		{
			//if (e.higestScore()>=0.8f) {  //this condition is not need as part_of relation is a strong evidence.
			for (Entity entity: e.getProposals()){
				for (EntityProposals keye : keyentities) {
					for(Entity key: keye.getProposals()){
						if((key.isOntologized()==true)&&(entity.isOntologized()==true))
						{
							ArrayList<Entity> entities = entity.getIndividualEntities();
							ArrayList<Entity> keys = key.getIndividualEntities();
							for(Entity aentity: entities){
								for(Entity akey: keys){
									//if (XML2EQ.elk.isPartOf(entity.getPrimaryEntityOWLClassIRI(), key.getPrimaryEntityOWLClassIRI())) {
									if (XML2EQ.elk.isPartOf(aentity.getPrimaryEntityOWLClassIRI(), akey.getPrimaryEntityOWLClassIRI())) {

										// key is entity locator of e
										entity.setConfidenceScore(1f);
										CompositeEntity ce = new CompositeEntity();
										ce.addEntity(entity);								
										FormalRelation rel = Dictionary.partof;
										rel.setConfidenceScore((float) 1.0);
										REntity rentity = new REntity(rel, key);
										ce.addEntity(rentity);
										key = ce; // replace key with the composite entity in keyentities
										flag=1;
										break;
									}
								}
								if(flag==1) break;
							}
						}
					}
				}
			}
			//}
		}	
		if(flag==1){
			return keyentities;
		}else{
			return null;
		}

	}

	/**
	 * test subclass relations between all e proposals and each of the keyentities proposals
	 * if positive for any test, resolve to the subclass
	 * @param e
	 * @param keyentities
	 * @return
	 */
	private ArrayList<EntityProposals> resolveBaseOnSubclassRelation(ArrayList<EntityProposals> eProposals, ArrayList<EntityProposals> keyentities){
		int flag=0;
		for(EntityProposals e: eProposals)
		{
			//if (e.higestScore()>=0.8f) { //this condition is not need as subclass relation is a strong evidence.
			for (Entity entity: e.getProposals()){
				for (EntityProposals keye : keyentities) {
					for(Entity key: keye.getProposals()){
						if((key.isOntologized()==true)&&(entity.isOntologized()==true))
						{
							ArrayList<Entity> entities = entity.getIndividualEntities();
							ArrayList<Entity> keys = key.getIndividualEntities();
							for(Entity aentity: entities){
								for(Entity akey: keys){
									if (XML2EQ.elk.isSubClassOf(aentity.getPrimaryEntityOWLClassIRI(), akey.getPrimaryEntityOWLClassIRI())) {	
										//if (XML2EQ.elk.isSubClassOf(entity.getPrimaryEntityOWLClassIRI(), key.getPrimaryEntityOWLClassIRI())) {
										// reset this key proposals to the subclass
										keye.reset();
										entity.setConfidenceScore(1f);
										keye.add(entity);
										keye.setPhrase(entity.getString());
										flag=1;
										break;
									}
								}
								if(flag==1) break;
							}
						}
					}
				}
			}
			//}
		}	
		if(flag==1)
			return keyentities;
		else
			return null;
	}

	/**
	 * the state statement may return a spatial element that need to be
	 * integrated into the keyentity
	 * 
	 * @param entity
	 * @return
	 */
	private boolean containsSpatial(ArrayList<EntityProposals> e) {
		// TODO
		return false;
	}

	
	//public ArrayList<EQStatementProposals> getEQStatements() {
	public ArrayList<EQProposals> getEQStatements() {
		//TODO: for the same E, if Q elongated is there, remove Q:shape 
		return this.EQStatements;
	}

	public void clearEQStatements() {
		this.EQStatements.clear();		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}



}
