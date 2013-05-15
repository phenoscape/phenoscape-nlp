package outputter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

public class EntityParser {
	private ArrayList<Entity> entities = new ArrayList<Entity>();

	/**
	 * TODO parse also entity locators for entity
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
							Entity entity = new EntitySearcherOriginal().searchEntity(root, sid, sname, "", sname, "");
							//keyentities.add(entity);//TODO try harder to find a match for the key entity
							if(entity!=null){
								entities.add(entity);
							}
						}else{
							boolean negation = false;
							if(relation.getAttribute("negation")!=null) negation = Boolean.getBoolean(relation.getAttributeValue("negation"));
							RelationHandler rh = 	new RelationHandler(root, 
									relation.getAttributeValue("name"),  
									Utilities.getStructureName(root,  relation.getAttributeValue("to")), 
									relation.getAttributeValue("to"),
									Utilities.getStructureName(root,  relation.getAttributeValue("from")),
									relation.getAttributeValue("from"), negation, keyelement);
							rh.handle();
							Entity entity = rh.getEntity();
							if(entity!=null){
								entities.add(entity);
							}

						}
					}				
				}catch(Exception ex){
					ex.printStackTrace();
				}	
	}

	public ArrayList<Entity> getEntities() {
		return entities;
	}

	public void setEntities(ArrayList<Entity> entities) {
		this.entities = entities;
	}
}