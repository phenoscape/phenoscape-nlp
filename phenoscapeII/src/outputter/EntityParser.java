package outputter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

public class EntityParser {
	private ArrayList<Entity> entities = new ArrayList<Entity>();

	public EntityParser(Element statement, Element root) {
		//add all structures which are not "whole organism" to key structures
				try{
					List<Element> structures = XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(statement);
					for(Element structure: structures){
						//Hashtable<String, String> keyentity = new Hashtable<String, String>();
						String sid = structure.getAttributeValue("id");
						if(!Utilities.isToStructureInRelation(sid, root)){//to-structure involved in a relation are not considered a key
							String sname = Utilities.getStructureName(root, sid);
							Entity entity = new EntitySearcherOriginal().searchEntity(root, sid, sname, "", sname, "", 0);
							//keyentities.add(entity);//TODO try harder to find a match for the key entity
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