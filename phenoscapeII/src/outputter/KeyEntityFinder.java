/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;

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
 */
public class KeyEntityFinder {
	private List<Element> chars;
	private List<Element> states;
	private List<String> keyentities;
	private Element root;
	
	
	/**
	 */
	public KeyEntityFinder(List<Element> chars, List<Element> states, Element root, List<String> keyentities) {
		this.chars = chars;
		this.states = states;
		this.root = root;
		this.keyentities = keyentities;
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
	 */
	public List<Element> getKeyEntities() throws Exception{
		List<Element> keys = new ArrayList<Element>();
		
		//add all structures which are not "whole organism" to key structures
		//TODO should to-structure involved in a relation be considered key?
		for (Element e : chars) {
			keys.addAll(XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(e));
		}
		
		//no key structures found
		if (keys.size() == 0) {
			Element key = new Element("structure");
			key.setAttribute("name", "unknown");
			key.setAttribute("id", "unknown" + XML2EQ.unknownid);
			XML2EQ.unknownid++;
			keys.add(key);
		}

		//populate keyentities
		for(Element key:keys){
			//TODO ontology lookup for key entities
			keyentities.add(Utilities.getStructureName(root, key.getAttributeValue("id")));
		}
		return keys;
	
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
