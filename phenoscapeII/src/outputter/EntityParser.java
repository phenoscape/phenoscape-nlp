package outputter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.jdom.Element;
import org.jdom.xpath.XPath;


/**
 * This class parses entity or quality from one part_of chain of <structure> elements.
 * It takes as input a <structure> element, identify the part_of chain starting with the structure as an entity and/or a quality,
 * Returns an EntityProposals and/or a Structure2Quality strategy
 * @author Hong Cui
 *
 */
public class EntityParser {
	//caches: key = structid
	private static Hashtable<String, ArrayList<EntityProposals>> entitycache = new Hashtable<String, ArrayList<EntityProposals>>() ;
	private static Hashtable<String, Structure2Quality> s2qcache = new Hashtable<String, Structure2Quality> ();
	//private Hashtable<String, EntityProposals> spaitialmodifiercache;
	//private Hashtable<String, HashSet<String>> identifiedqualitiescache;
	
	private ArrayList<EntityProposals> entity;
	private Structure2Quality s2q;
	private EntityProposals spaitialmodifier;
	private HashSet<String> identifiedqualities;
	private ArrayList<EntityProposals> keyentities;

	public EntityParser(Element statement, Element root, String structureid, String structurename, ArrayList<EntityProposals> keyentities, boolean keyelement) {
		this.keyentities = keyentities; 
		if(entitycache.get(structureid)!=null){
			entity = entitycache.get(structureid);
			s2q =  s2qcache.get(structureid);
		}else{
			String parents = Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + structureid + "']" +
					 "|//relation[@name='in'][@from='" + structureid + "']" +
					 "|//relation[@name='on'][@from='" + structureid + "']", 0);
			this.entity = new EntitySearcherOriginal().searchEntity(root, structureid, structurename, parents, structurename, "part_of");	
			entitycache.put(structureid, entity);
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
			
			Structure2Quality rq = new Structure2Quality(root, structurename, structureid, keyentities);
			rq.handle();
			String t = "";
			//If any structure is a quality detach all the structures containing the structure id
			
			// if this.entity is not ontologized, then take the qualities else retain the entities
			if(rq.qualities.size()>0){
				s2q = rq;
				s2qcache.put(structureid, rq);
			}
			//resolveStructure(); //EntityParser doesn't have info to resolve this.
		}
	}

	/**
	 * 
	 * @return null if no entity has been found/ontologized.
	 */
	public ArrayList<EntityProposals> getEntity() {
		if(entity!=null && entity.size()>0 && entity.get(0)!=null) return entity;
		else return null;
	}

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
