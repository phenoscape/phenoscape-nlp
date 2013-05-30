/**
 * 
 */
package outputter;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

/**
 * @author updates
 * identify entities, and spatial regions and entity locators associated with the entities, form simple or composite entities
   also identify quality clues from the character statement, such as "size of" "number of", and "fusion of". 
 */
public class CharacterStatementParser extends Parser {
	ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
	ArrayList<EntityProposals> keyentities = new ArrayList<EntityProposals>();
	ArrayList<String> qualityClue = new ArrayList<String> ();
	static XPath pathstructure;
	static XPath pathrelation;
	static{
		try{
			pathstructure = XPath.newInstance(".//structure");
			pathrelation = XPath.newInstance(".//relation");

		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public CharacterStatementParser(TermOutputerUtilities ontoutil) {
		super(ontoutil);
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
		try {
			parseForQualityClue(statement); 
			checkandfilterqualitystructures(statement,root);//this removes quality structure elements from statement
			parseForEntities(statement, root, true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This method checks, if any of the structures in character statement are actually quality. If so, it detaches the structure along with the relations that contain the structure
	 * @param statement
	 * @param root
	 * @throws JDOMException
	 */
	@SuppressWarnings("unchecked")
	private void checkandfilterqualitystructures(Element statement,Element root) throws JDOMException {

		String structname;
		String structid;

		List<Element> structures = pathstructure.selectNodes(statement);
		//fetch all the structures and individually check, if each are qualities
		for(Element structure:structures)
		{
			structname = structure.getAttributeValue("name");
			structid = structure.getAttributeValue("id");
			Structure2Quality rq = new Structure2Quality(root,
					structname, structid, null);
			rq.handle();
			//If any structure is a quality detach all the structures and relations that contains the structure id
			if(rq.qualities.size()>0)
			{
				structure.detach();
				List<Element> relations = pathrelation.selectNodes(statement);
				for(Element relation:relations)
					if((relation.getAttributeValue("from").equals(structid)) //the structure involved in the relation is actually a quality
							|| (relation.getAttributeValue("to").equals(structid)))
						relation.detach();
			}

		}
	}


	/**
	 * turn a statement from 
	 * <statement statement_type="character" character_id="states694" seg_id="0">
      <text>Shape of pineal series</text>
      <structure id="o2558" name="shape" />
      <structure id="o2559" name="series" constraint="pineal" />
      <relation id="r454" name="part_of" from="o2558" to="o2559" negation="false" />
    </statement>

    to 

    <statement statement_type="character" character_id="states694" seg_id="0">
      <text>Shape of pineal series</text>
      <structure id="o2559" name="series" constraint="pineal" />
    </statement>

    and grab "shape" as the qualityclue
	 * @param statement
	 */
	@SuppressWarnings("unchecked")
	private void parseForQualityClue(Element statement) {
		//find <structure>s named with an attribute
		List<Element> structures;
		try {
			structures = pathstructure.selectNodes(statement);
			for(Element structure: structures){
				String name = structure.getAttributeValue("name");
				if(name.matches("("+ontoutil.attributes+")")){
					//remove the <structure> and related relations
					structure.detach();
					String id = structure.getAttributeValue("id");
					List<Element> relations = XPath.selectNodes(statement, ".//relation[@from='"+id+"']|.//relation[@to='"+id+"'] "); //shape of, in shape
					for(Element relation: relations){
						relation.detach();
					}
				}
			}
		} catch (JDOMException e) {
			e.printStackTrace();
		}

		//record qualityclue which may or may not be marked as a structure
		String text = statement.getChildText("text").toLowerCase();
		Pattern p = Pattern.compile(".*?\\b("+ontoutil.attributes+")\\b(.*)");
		Matcher m = p.matcher(text);
		//could there be multiple attributes?
		while(m.matches()){
			this.qualityClue.add(m.group(1));
			m = p.matcher(m.group(2));
		}
	}

	/**

	 * @param statement
	 * @param root
	 */
	public void parseForEntities(Element statement, Element root, boolean fromcharacterdescription){
		EntityParser ep = new EntityParser(statement, root, fromcharacterdescription);
		entities = ep.getEntities();
		keyentities = ep.getEntities();
	}

	public ArrayList<String> getQualityClue(){
		return this.qualityClue;
	}

	public ArrayList<EntityProposals> getEntities(){
		return this.entities;
	}

	public ArrayList<EntityProposals> getKeyEntities(){
		return this.keyentities;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
