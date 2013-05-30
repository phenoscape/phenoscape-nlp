package outputter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;


/**
 * this class parses entities from a statement
 * returns an ArrayList<EntityProposals> entities
 * @author Hong Cui
 *
 */
public class EntityParser {
	private ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();

	/**
	 * TODO parse also entity locators for entity
	 * TODO parser restricted relations for post-composition of entities
	 * @param statement
	 * @param root
	 */
	public EntityParser(Element statement, Element root, boolean keyelement) {
		//add all structures which are not "whole organism" to key structure
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
	}

	/*public void setEntities(ArrayList<Entity> entities) {
		this.entities = entities;
	}*/
}
