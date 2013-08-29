package outputter.process;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.xpath.XPath;

import outputter.Utilities;
import outputter.data.EntityProposals;
import outputter.search.EntitySearcherOriginal;


/**
 * This class parses entity or quality from one part_of chain of <structure> elements.
 * It takes as input a <structure> element, identify the part_of chain starting with the structure as an entity and/or a quality,
 * Returns an EntityProposals and/or a Structure2Quality strategy
 * @author Hong Cui
 *
 */
public class EntityParser {
	private static final Logger LOGGER = Logger.getLogger(EntityParser.class);   
	//caches: key = structid
	private static Hashtable<String, ArrayList<EntityProposals>> entitycache = new Hashtable<String, ArrayList<EntityProposals>>() ; //structureid => results
	private static Hashtable<String, Structure2Quality> s2qcache = new Hashtable<String, Structure2Quality> ();
	private static ArrayList<String> nomatchentitycache = new ArrayList<String>(); //structureid
	private static ArrayList<String> nomatchs2qcache = new ArrayList<String>();
	//private Hashtable<String, EntityProposals> spaitialmodifiercache;
	//private Hashtable<String, HashSet<String>> identifiedqualitiescache;
	
	private ArrayList<EntityProposals> entity;
	private Structure2Quality s2q;
	private EntityProposals spaitialmodifier;
	private HashSet<String> identifiedqualities;
	private ArrayList<EntityProposals> keyentities;

	/*
	 * Structureid: to handle pubis-ischium case, multiple structureids may be constructed from one structureid using '#' and an index, for example "o1#0", "o1#1"
	 * the copy should be used for saving to and looking up in caches
	 * the real structureid is used in process.
	 */
	public EntityParser(Element statement, Element root, String structureid, String structurename, ArrayList<EntityProposals> keyentities, boolean keyelement) {
		String structureidcopy = structureid;
		if(structureid.indexOf("#")>0) structureid = structureid.substring(0, structureid.indexOf("#"));
		this.keyentities = keyentities; 
		if(entitycache.get(structureidcopy)!=null){
			entity = entitycache.get(structureidcopy);
		}else if(s2qcache.get(structureidcopy)!=null){
			s2q =  s2qcache.get(structureidcopy);
		}else if(nomatchentitycache.contains(structureidcopy)){
			entity = null;
		}else if(nomatchs2qcache.contains(structureidcopy)){
			s2q = null;
		}else{
			String parents = Utilities.getNamesOnPartOfChain(root, structureid);
			LOGGER.debug("EntityParser calls EntitySearcherOriginal to search '"+structurename+","+parents+"'");
			this.entity = new EntitySearcherOriginal().searchEntity(root, structureid, structurename, parents, structurename+"+"+parents, "part_of");	
			if(this.entity!=null){
				LOGGER.debug("EntityParser recorded matched proposals: ");
				for(EntityProposals ep: entity){ 
					LOGGER.debug(".."+ep.toString());
				}
				entitycache.put(structureidcopy, entity);
			}else{
				EntityParser.nomatchentitycache.add(structureidcopy);
				LOGGER.debug("EntityParser found no matching entities for '"+structurename+","+parents+"'");
			}

			// could the 'structure' be a quality?
			//is the structure a simple quality?
			/*Quality result = (Quality) new TermSearcher().searchTerm(structurename, "quality");
			if(result!=null){ 
				quality = new QualityProposals();
				quality.add(result);
			}else{
				// is the structure a relational quality?
				QualityProposals relationalquality = PermittedRelations.matchInPermittedRelation(structurename, false);
				if (relationalquality != null) quality = relationalquality;
			}*/
			
			Structure2Quality rq = null;
			//structures involved in constraints should not be checked for quality (the resulting quality would be mistakenly applied to keyentities) 
			if(!Utilities.isConstraint(root, structureid)){
				rq = new Structure2Quality(root, structurename, structureid, keyentities);
				rq.handle();
			}
			//If any structure is a quality detach all the structures containing the structure id
			
			// if this.entity is not ontologized, then take the qualities else retain the entities
			if(rq!=null && rq.qualities.size()>0){
				s2q = rq;
				s2qcache.put(structureidcopy, rq);
				LOGGER.debug("EntityParser recorded candidate s2q");
			}else{
				EntityParser.nomatchs2qcache.add(structureidcopy);
				LOGGER.debug("EntityParser found no matching qualities for '"+structurename+","+parents+"'");
			}
			//resolveStructure(); //EntityParser doesn't have info to resolve this.
		}
	}

	

	/**
	 * return a clone so subsequent changes to entities will not be proprogated to the entity cache.
	 * @return null if no entity has been found/ontologized.
	 */
	public ArrayList<EntityProposals> getEntity() {
		if(entity!=null && entity.size()>0 && entity.get(0)!=null){
			 ArrayList<EntityProposals> clone = new  ArrayList<EntityProposals>();
			 for(EntityProposals ep: entity){
				 clone.add(ep.clone());
			 }
			return clone;
		}
		else return null;
	}

	/**
	 * not return a clone because no subsequent change is performed on s2q
	 * @return
	 */
	public Structure2Quality getQualityStrategy() {
		return s2q;
	}

	public ArrayList<EntityProposals> getSpaitialmodifier(){
		if(s2q!=null) return this.s2q.spatialmodifier;
		return null;
	}
	
	public HashSet<String> getIdentifiedqualities(){
		if(s2q!=null) return this.s2q.identifiedqualities;
		return null;
	}
	
	//before
	//private ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();

	/**
	 * TODO parse also entity locators for entity
	 * TODO parser restricted relations for post-composition of entities
	 * @param statement
	 * @param root
	 */
	/*public EntityParser(Element statement, Element root, boolean keyelement) {
		//add all structures which are not "whole_organism" to key structure.
		try{
			List<Element> structures = XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(statement);
			ArrayList<String> RelatedStructures = new ArrayList<String>(); //keep a record on related entities, which should not be processed again
			for(Element structure: structures){
				//Hashtable<String, String> keyentity = new Hashtable<String, String>();
				int flag=1; //not being processed as a related entity
				String sid = structure.getAttributeValue("id");

				for(String Structid:RelatedStructures){
					if(Structid.equals(sid)){
						flag=0;
						break;
					}
				}

				if(flag==1)		
				{
					List<Element> relations = Utilities.relationWithStructureAsSubject(sid, root);
					if((relations==null)||(relations.size()==0)) {//the structure is not related to others, form a simple entity
						String sname = Utilities.getStructureName(root, sid);
						EntityProposals entity = new EntitySearcherOriginal().searchEntity(root, sid, sname, "", sname, "");
						if(entity!=null){
							entities.add(entity);
						}
					}else{
						for(Element relation:relations)
						{
							boolean negation = false;
							if(relation.getAttribute("negation")!=null) negation = Boolean.valueOf(relation.getAttributeValue("negation"));
							RelationHandler rh = new RelationHandler(root, 
									relation.getAttributeValue("name"),  
									relation,
									Utilities.getStructureName(root,  relation.getAttributeValue("to")), 
									relation.getAttributeValue("to"),
									Utilities.getStructureName(root,  relation.getAttributeValue("from")),
									relation.getAttributeValue("from"), negation, keyelement);
							rh.handle();
							EntityProposals entity = rh.getEntity();
							if(entity!=null){
								entities.add(entity);
								for(Entity related:entity.getProposals())
									if(related instanceof CompositeEntity)
										RelatedStructures.add(relation.getAttributeValue("to"));
							}
						}
					}
				}
			}				
		}catch(Exception ex){
			ex.printStackTrace();
		}	
	}

	public ArrayList<EntityProposals> getEntities() {
		return entities;
	}*/

	/*public void setEntities(ArrayList<Entity> entities) {
		this.entities = entities;
	}*/
	
	/**
	 * if an ontologized entity is found, resolve to it
	 * otherwise, resolve to s2q and cleanHandledStructures()
	 */
	/*public void resolveStructure()
	{
		
		for(Entity proposal :this.entity.getProposals())
		{
			if((proposal.isOntologized())) 
			{
				this.s2q=null;
				return;
			}
		}
		
		if(this.s2q != null && this.s2q.qualities.size()>0){
			this.entity = null;
			this.spaitialmodifier = this.s2q.spatialmodifier;
			this.identifiedqualities = this.s2q.identifiedqualities;
			s2q.cleanHandledStructures();
		}
	}*/
}
