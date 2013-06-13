package outputter;

import java.util.ArrayList;
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
	private EntityProposals entity;
	private Structure2Quality s2q;

	public EntityParser(Element statement, Element root, Element structure, boolean keyelement) {
		String structureid = structure.getAttributeValue("id");
		String structurename = Utilities.getStructureName(root, structureid);
		String parents = Utilities.getStructureChain(root, "//relation[@from='" + structureid + "']", 0);
		this.entity = new EntitySearcherOriginal().searchEntity(root, structureid, structurename, parents, structurename, "part_of");	
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
		Structure2Quality rq = new Structure2Quality(root, structurename, structureid, null);
		rq.handle();
		//If any structure is a quality detach all the structures containing the structure id
		
		// if this.entity is not ontologized, then take the qualities else retain the entities
		if(rq.qualities.size()>0){
			s2q = rq;
		}
		resolvestructure();
	}


	
	public void resolvestructure()
	{
		
		for(Entity proposal :this.entity.getProposals())
		{
			if((proposal.isOntologized()==false)&&(s2q!=null))
			{
				this.entity =null;
				return;
			}
		}
		this.s2q=null;
	}
	
	
	
	public EntityProposals getEntity() {
		return entity;
	}

	public Structure2Quality getQualityStrategy() {
		return s2q;
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
		//add all structures which are not "whole organism" to key structure.
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
}
