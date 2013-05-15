/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author hong cui
 * the key entities will be used as the entities for all states without an explicit subject
 * 
 * the key entities may be simply collected by gathering all non-whole-organism structures from a character statement.
 * but more sophisticated methods are conceivable:
 * for example, the identification of the key entites may be affected by the types of qulities expressed in the character statements 
 * "is this possible for this E to have this Q?" if not, then the E should probably not be included as a key entity. 
 * 
 * 
 * TODO: complicated cases such as joints Junction between metapterygoid and hyomandibular, 300+ examples at SELECT * FROM fish_original_1st WHERE entitylabel LIKE "%joint%"
 *  "Quadratojugal squamosal and preopercular fused"
 *  "Contact between postorbital and lacrimal" => "joint" or "associated with"?
 *  "(organ name)"
 */
public class KeyEntityFinder {
	private EntitySearcherOriginal es;

	
	/**
	 */
	public KeyEntityFinder(EntitySearcherOriginal es) {
		this.es = es;		
	}

	/**
	 * [8]Armbruster_2004.xml_0638f15b-0de4-45fd-a3af-b1d209cea9d3.xml
	 * text::Walls of metapterygoid channel
	 * text::lateral wall slightly smaller to just slightly larger than mesial wall, or absent
	 * EQ::[E]lateral wall [Q]smaller [slightly]
	 * EQ::[E]lateral wall [Q]larger [just slightly] [QM]mesial wall
	 * EQ::[E]lateral wall [Q]absent
	 * text::mesial wall much taller
	 * EQ::[E]mesial wall [Q]taller [much]
	 * 
	 * @return an arraylist of hashtables with keys: name|structid|entityid, each hashtable is a keyentity
	 * 
	 * when contructing a new entity (post-composed entity such as a joint), 
	 * adjust 'root' to normalize it, for example, Junction between metapterygoid and hyomandibular
	 * 
	 * change structure 'junction' to 'metapterygoid-hyomandibular joint' (so characters/relations of junction now are associated with metapterygoid-hyomandibular joint"
	 * remove relation "between", if metapterygoid and hyomandibular have no characters, remove them too, (what if they have?). 
	 * 
	 * save onto-id of a structure in the new 'ontoid' attribute of <structure>
	 * 
	 * do not deal with entity locators for key entities, which will be dealt with when processing the character statement by both non-binary and binary statements..
	 * 
	 */
	public List<Element> getKeyEntities(List<Element> chars, List<Element> states, Element root, ArrayList<Hashtable<String, String>> keyentities){

		List<Element> keys = new ArrayList<Element>();
		//add all structures which are not "whole organism" to key structures
		//TODO should 
		for (Element e : chars) {
			try{
				List<Element> structures = XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(e);
				for(Element structure: structures){
					Hashtable<String, String> keyentity = new Hashtable<String, String>();
					String sid = structure.getAttributeValue("id");
					if(!isToStructureInRelation(sid, root)){//to-structure involved in a relation are not considered a key
						String sname = Utilities.getStructureName(root, sid);
						/*Hashtable<String, String> result = es.searchEntity(root, sid, sname, "", sname, "", 0);
						keyentity.put("entity", sname);//TODO try harder to find a match for the key entity
						if(result!=null){
							structure.setAttribute("ontoid", result.get("entityid"));
							keyentity.put("entityid", result.get("entityid"));
							keyentity.put("entitylabel", result.get("entitylabel"));
							keyentity.put("entitylocator", result.get("entitylocator")); //entity locator info is optional
							keyentity.put("entitylocatorid", result.get("entitylocatorid"));
							keyentity.put("entitylocatorlabel", result.get("entitylocatorlabel"));
						}
						keyentities.add(keyentity);
						keys.add(structure);
						
						Old copy of code
						*
						*/
						
						//Below changes by Hari
						Entity result = es.searchEntity(root, sid, sname, "", sname, "", 0);
						if(result!=null)
						{
						if(result instanceof SimpleEntity)
						{	
						keyentity.put("entity", sname);//TODO try harder to find a match for the key entity
							structure.setAttribute("ontoid", ((SimpleEntity) result).getId());
							keyentity.put("entityid", ((SimpleEntity) result).getId());
							keyentity.put("entitylabel", ((SimpleEntity) result).getLabel());
//							keyentity.put("entitylocator", result.get("entitylocator")); //entity locator info is optional
//							keyentity.put("entitylocatorid", result.get("entitylocatorid"));
//							keyentity.put("entitylocatorlabel", result.get("entitylocatorlabel"));
						
						}
						else if(result instanceof CompositeEntity)
						{
							keyentity.put("entity", sname);//TODO try harder to find a match for the key entity
							structure.setAttribute("ontoid", ((CompositeEntity) result).getPrimaryEntity().getId());
							keyentity.put("entityid", ((CompositeEntity) result).getPrimaryEntity().getId());
							keyentity.put("entitylabel", ((CompositeEntity) result).getPrimaryEntity().getLabel());
						}
						else {}
						keyentities.add(keyentity);
						keys.add(structure);
						}						
					}					
				}				
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
		
		//no key structures found
		if (keys.size() == 0) {
			Element key = new Element("structure");
			key.setAttribute("name", "unknown");
			key.setAttribute("id", "unknown" + XML2EQ.unknownid);
			XML2EQ.unknownid++;
			Hashtable<String, String> keyentity = new Hashtable<String, String>();
			keyentity.put("entity", "unknown");
			keyentities.add(keyentity);
			keys.add(key);
		}

		//populate keyentities
		/*for(Element key:keys){
			//TODO ontology lookup for key entities
			String structid = key.getAttributeValue("id");
			String structname = Utilities.getStructureName(root, structid);
			Hashtable<String, String> result = es.searchEntity(root, structid, structname, "", structname, "", 0);
			if(result!=null){
				key.setAttribute("ontoid", result.get("entityid"));
			}
			keyentities.add(structname);
		}*/	
		return keys;
	}
	
	private boolean isToStructureInRelation(String sid, Element root) {
		try{
			XPath tostructure = XPath.newInstance(".//relation[@to='"+sid+"']");
			Element rel = (Element)tostructure.selectSingleNode(root);
			if(rel == null) return false;
		}catch(Exception e){
			e.printStackTrace();
		}		
		return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
