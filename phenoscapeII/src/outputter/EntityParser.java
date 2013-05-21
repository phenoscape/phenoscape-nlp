package outputter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

public class EntityParser {
	private ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();

	/**
	 * TODO parse also entity locators for entity
	 * TODO parser restricted relations for post-composition of entities
	 * @param statement
	 * @param root
	 */
	public EntityParser(Element statement, Element root, boolean keyelement) {
		//add all structures which are not "whole organism" to key structures
				try{
					List<Element> structures = XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(statement);
					for(Element structure: structures){
						//Hashtable<String, String> keyentity = new Hashtable<String, String>();
						String sid = structure.getAttributeValue("id");
						Element relation = Utilities.relationWithStructureAsSubject(sid, root);
						if(relation==null){//the structure is not related to others, form a simple entity
							String sname = Utilities.getStructureName(root, sid);
							EntityProposals entity = new EntitySearcherOriginal().searchEntity(root, sid, sname, "", sname, "");
							if(entity!=null){
								entities.add(entity);
							}
						}else{
							boolean negation = false;
							if(relation.getAttribute("negation")!=null) negation = Boolean.valueOf(relation.getAttributeValue("negation"));
							RelationHandler rh = new RelationHandler(root, 
									relation.getAttributeValue("name"),  
									Utilities.getStructureName(root,  relation.getAttributeValue("to")), 
									relation.getAttributeValue("to"),
									Utilities.getStructureName(root,  relation.getAttributeValue("from")),
									relation.getAttributeValue("from"), negation, keyelement);
							rh.handle();
							EntityProposals entity = rh.getEntity();
							if(entity!=null){
								entities.add(entity);
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