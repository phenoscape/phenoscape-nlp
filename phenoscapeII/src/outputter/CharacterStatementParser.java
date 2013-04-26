/**
 * 
 */
package outputter;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author updates
 * identify entities, and spatial regions and entity locators associated with the entities, form simple or composite entities
   also identify quality clues from the character statement, such as "size of" "number of", and "fusion of". 
 */
public class CharacterStatementParser extends Parser {
	ArrayList<Entity> entities = new ArrayList<Entity>();
	ArrayList<Entity> keyentities = new ArrayList<Entity>();
	String qualityClue;

	/**
	 * 
	 */
	public CharacterStatementParser() {
		super();
	}


	/** old doc from keyEntityFinder
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
	/** 
	 * parse out entities, may be simple or post-composed. 
	 * figure out is_a and part_of relations among entities.
	 * post-compose entities using spatial terms and relational quality terms.
	 * parse out character terms such as "number", "size", "ratio" and "fusion".
	 * 
	 * 
	 * TODO under construction
	 */
	@Override
	public void parse(Element statement, Element root) {
		parseForEntities(statement, root);
		parseForQualityClue(statement);
	}
	
	private void parseForQualityClue(Element statement) {
		// TODO 
		this.qualityClue = "";		
	}


	public void parseForEntities(Element statement, Element root){
		//add all structures which are not "whole organism" to key structures
		try{
			List<Element> structures = XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(statement);
			for(Element structure: structures){
				//Hashtable<String, String> keyentity = new Hashtable<String, String>();
				String sid = structure.getAttributeValue("id");
				if(!isToStructureInRelation(sid, root)){//to-structure involved in a relation are not considered a key
					String sname = Utilities.getStructureName(root, sid);
					Entity entity = new EntitySearcherOriginal().searchEntity(root, sid, sname, "", sname, "", 0);
					keyentities.add(entity);//TODO try harder to find a match for the key entity
					if(entity!=null){
						//structure.setAttribute("ontoid", entity.getId());
						entities.add(entity);
					}
				}					
			}				
		}catch(Exception ex){
			ex.printStackTrace();
		}	
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

	public String getQualityClue(){
		return this.qualityClue;
	}

	public ArrayList<Entity> getEntities(){
		return this.entities;
	}
	
	public ArrayList<Entity> getKeyEntities(){
		return this.keyentities;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
