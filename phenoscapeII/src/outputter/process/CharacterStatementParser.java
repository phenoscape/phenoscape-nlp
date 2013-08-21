/**
 * 
 */
package outputter.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import outputter.Utilities;
import outputter.XML2EQ;
import outputter.data.CompositeEntity;
import outputter.data.EQProposals;
import outputter.data.Entity;
import outputter.data.EntityProposals;
import outputter.data.FormalRelation;
import outputter.data.QualityProposals;
import outputter.data.REntity;
import outputter.data.SimpleEntity;
import outputter.knowledge.Dictionary;
import outputter.knowledge.TermOutputerUtilities;
import outputter.prep.XMLNormalizer;

/**
 * @author updates
 * identify entities, and spatial regions and entity locators associated with the entities, form simple or composite entities
   also identify quality clues from the character statement, such as "size of" "number of", and "fusion of". 
 */
public class CharacterStatementParser extends Parser {
	private static final Logger LOGGER = Logger.getLogger(CharacterStatementParser.class);   
	//ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
	ArrayList<EntityProposals> keyentities = new ArrayList<EntityProposals>();
	//use of qualityclue: test cases:patterns.xml_s08e7ab42-dd8f-409c-adbe-5126d8579e82.xml
	ArrayList<String> qualityClue = new ArrayList<String> ();
	ArrayList<String> underscoredStructureIDs = new ArrayList<String> ();
	ArrayList<String> structureIDs = new ArrayList<String> ();
	String checked = "";
	boolean debug = false;
	private Hashtable<String, ArrayList<EntityProposals>> entityHash = new Hashtable<String, ArrayList<EntityProposals>>();
	private Hashtable<String, ArrayList<Structure2Quality>> qualityHash = new Hashtable<String, ArrayList<Structure2Quality>>();
	static XPath pathstructure;
	static XPath pathrelation;
	static{
		try{
			pathstructure = XPath.newInstance(".//structure");
			pathrelation = XPath.newInstance(".//relation");
		}catch(Exception e){
			LOGGER.error("", e);
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
	public void parse(Element statement, Element root, EQProposals notused) {
		try {
			parseForQualityClue(statement); 
			parseForEntities(statement, root, true);
		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	/**
	 * This method checks, if any of the structures in character statement are actually quality. 
	 * If so, it detaches the structure along with the relations that contain the structure, so the structure will not be processed as an entity
	 * 
	 * @param statement
	 * @param root
	 * @throws JDOMException
	 */
	/*@SuppressWarnings("unchecked")
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
	}*/


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
			LOGGER.error("", e);
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
		
		if(debug){
			System.out.print("quality clues:");
			for(String qc: this.qualityClue){
				System.out.print(qc);
			}
			System.out.println();
		}
	}

	/**
   //TODO patterns s0fd16381: maxillae, anterior end of 
    * two independent structures in character statement: sereno style.
    * //case of 'pubis_ischium', one structure id for two structures.
	*  //joint: joint^overlaps(metapterygoid)^overlaps(hyomandibula)
	* //skeletal joint: UBERON:0000982
	*					 
	 * @param statement
	 * @param root
	 */
	public void parseForEntities(Element statement, Element root, boolean fromcharacterdescription){
		try{
			//ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
			//ArrayList<Structure2Quality> s2qs = new ArrayList<Structure2Quality>();
			List<Element> structures = XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(statement);
			ArrayList<String> RelatedStructures = new ArrayList<String>(); //keep a record on related entities, which should not be processed again
			for(Element structure: structures){
				String structureid = structure.getAttributeValue("id");	
				if(!checked.contains(structureid+",")){	
					this.structureIDs.add(structureid);
					String name = structure.getAttributeValue("name");
					if((name.indexOf("_")>0)||(name.indexOf("/")>0)){//case of 'pubis_ischium', one structure id for two structures. also humerus/radius ratio case
						name=name.replaceAll("(\\(|\\))", "");//used to address case 'fang relative-to (dentary_tooth) size' check with prof.
						String[] names = name.split("[_|/]");
						underscoredStructureIDs.add(structureid);
						LOGGER.debug("CSP: '"+name+"' is decomposed into "+names.length);
						int index = 0;
						for(String aname: names){
							LOGGER.debug("CSP: searching "+aname);
							String parents = parseStructure(statement, root,
									fromcharacterdescription, /*entities, s2qs,*/
									structureid+"#"+index, aname); //use the same structureid for all structures
							checked += structureid+","+parents+",";
							index++;
						}
						//entityHash.put(structureid, entities);
						//qualityHash.put(structureid, s2qs);
					}else{
						String structurename = Utilities.getStructureName(root, structureid);
						String parents = parseStructure(statement, root,
								fromcharacterdescription, /*entities, s2qs,*/
								structureid, structurename);
						checked += structureid+","+parents+",";
						//entityHash.put(structureid, entities);
						//qualityHash.put(structureid, s2qs);
					}
				}
			}
			resolve(/*entities, s2qs,*/ statement, root);
	
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}


	private String parseStructure(Element statement, Element root,
			boolean fromcharacterdescription, String structureid, String structurename) {
		String structureidcopy = structureid;
		if(structureid.indexOf("#")>0) structureid = structureid.substring(0, structureid.indexOf("#"));
		String parents = Utilities.getStructureChainIds(root, "//relation[@name='part_of'][@from='" + structureid + "']" +
				 "|//relation[@name='in'][@from='" + structureid + "']" +
				 "|//relation[@name='on'][@from='" + structureid + "']", 0); //list of structures separated with ","
		if(debug){
			System.out.println("parse structure:"+structurename);
		}
		EntityParser ep = new EntityParser(statement, root, structureidcopy, structurename, null, fromcharacterdescription);
		if(ep.getEntity()!=null){
			ArrayList<EntityProposals> entities;
			if(this.entityHash.get(structureid)==null){
				entities = new ArrayList<EntityProposals>();
			}else{
				entities = this.entityHash.get(structureid);			
			}
			entities.addAll(ep.getEntity());
			this.entityHash.put(structureid, entities);
			LOGGER.debug("CharacterStatementParser received matched entities: ");
			for(EntityProposals aep: entities){
				LOGGER.debug(".."+aep.toString());
			}
		}
		if(ep.getQualityStrategy()!=null){
			ArrayList<Structure2Quality> qualities;
			if(this.qualityHash.get(structureid)==null){
				qualities = new ArrayList<Structure2Quality>();
			}else{
				qualities = this.qualityHash.get(structureid);			
			}
			qualities.add(ep.getQualityStrategy());
			this.qualityHash.put(structureid, qualities);	
			if(debug){
				System.out.println("matched qualities:");
				for(Structure2Quality as2q: qualities){
					for(QualityProposals qp: as2q.qualities){
						System.out.println(qp.toString());
					}
				}
				System.out.println();
			}	
		}
		return parents;
	}

	/**
	 * TODO
	 * 1. determine keyentities (note Sereno style)
	 * 2. post-compose entities with quality 
	 * 3. approve/disapprove structure2quality results
	 * 4. constructure EQ for non-key entities 
	 * @param entities
	 * @param s2qs
	 * @param statement
	 * @param root
	 */
	private void resolve(/*ArrayList<EntityProposals> entities,
			ArrayList<Structure2Quality> s2qs,*/ Element statement, Element root) {
		boolean foundaentity = false;
		//1. 'pubis_ischium': these are the key entities
		//TODO: what about "A_B joint": should have a entity search strategy to handle this
		int count = 0;
		for (String structid: this.underscoredStructureIDs){
			LOGGER.debug("CSP: resolving underscored structures...  ");
			ArrayList<EntityProposals> entities = this.entityHash.get(structid);

//get quality from relations and characters from each of the structures and post compose entities with the quality
			if((entities!=null)&&(this.qualityClue.contains("ratio")==false)){
				LOGGER.debug("CSP: entities from underscored structures (qualityclue is not 'ratio'):");
					for(EntityProposals aep: entities){
						LOGGER.debug(".."+aep.toString());
					}
				LOGGER.debug("CSP: post-composed with quality...");
				postcomposeWithQuality(entities, structid, statement, root);//for resolved entities only; update entities in this entityHash
				foundaentity = true;
				this.qualityHash.remove(structid);
				//remove from s2q with a structid < the first struct id 
				if(count==0) removeS2Qbefore(structid);
				this.structureIDs.remove(structid);
			}
			else if((entities!=null)&&(this.qualityClue.contains("ratio")==true))//if the quality clue has ratio then, just return all th entities
			{
				Set<String> keys = this.entityHash.keySet();
				for(String key:keys)
				{
					this.keyentities.addAll(this.entityHash.get(key));
					foundaentity = true;
					this.qualityHash.remove(structid);
					//remove from s2q with a structid < the first struct id 
					if(count==0) removeS2Qbefore(structid);
					this.structureIDs.remove(structid);
				}
			}
			count++;
		}
		
		for(int i = 0; i < this.structureIDs.size(); i++){
			String structid = this.structureIDs.get(i);
			ArrayList<EntityProposals> entities = this.entityHash.get(structid);
			if(entities!=null && entities.size()>1){ //entities resulted from 1 structureid
				LOGGER.debug("CSP: entities from one structures:");
				for(EntityProposals aep: entities){
					LOGGER.debug(".."+aep.toString());
				}
				LOGGER.debug("CSP:post-composed with quality...");
				postcomposeWithQuality(entities, structid, statement, root);
				this.keyentities = entities;	
				this.entityHash.remove(structid);
				this.qualityHash = null;
			}		
			/*if(entities==null && i == this.structureIDs.size()-1){//no entity 
				if(debug) {
					System.out.println("no entity, keyentities is set to null");
				}
				this.keyentities = null;
			} */
			
			if(qualityHash !=null && this.qualityHash.get(structid)!=null && entityHash!=null && entities!=null){		
				ArrayList<Structure2Quality> s2qs = this.qualityHash.get(structid);
				boolean entitywin = entityWin(entities, s2qs);
				
					LOGGER.debug("CSP: selecting btw entity and quality for structure: "+structid);
				
					LOGGER.debug("CSP: matched entities:");
					for(EntityProposals aep: entities){
						LOGGER.debug(aep.toString());
					}
					
					LOGGER.debug("CSP: matched qualities:");
					for(Structure2Quality as2q: s2qs){
						for(QualityProposals qp: as2q.qualities){
							LOGGER.debug(".."+qp.toString());
						}
					}
					
					LOGGER.debug("CSP: entity win? " +entitywin);
				
				if(entitywin) this.qualityHash.remove(structid);
				if(!entitywin) this.entityHash.remove(structid);
			}
			
		}		
		
		//remaining s2qs
		if(this.qualityHash!=null){
			Enumeration<String> keys = this.qualityHash.keys();
			while(keys.hasMoreElements()){
				String sid = keys.nextElement();
				LOGGER.debug("CSP: remaining qualities from s2q:");
				
				ArrayList<Structure2Quality> s2qs = this.qualityHash.get(sid);
				//post compose quality to the closest proceeding entity, or
				//create EQ with the closest following entity
				//if no entity exsits, give up
			
					for(Structure2Quality as2q: s2qs){
						for(QualityProposals qp: as2q.qualities){
							LOGGER.debug(".."+ qp.toString());
						}
					}
					LOGGER.debug("CSP: how to consume this? ");
				
				consumeQuality(sid, s2qs); //update this.entityHash
			}
		}

		if(this.entityHash!=null && this.entityHash.size()>0){
			//Check ELK if there is actually a part of relationship, if yes proceed
//			else if they are involved in any relation as from then, they can key entity
			
			//LOGGER.debug("CSP: remaining entities: post-compose with 'part_of':");
			
			FormalRelation fr = Dictionary.partof;
			fr.setConfidenceScore(0.5f);
			//remaining entities
			//compose entities using 'part_of' relation
			//test cases: 
			//patterns.xml_s413c5e3e-7941-44e3-8be6-b17e6193752e.xml (-)
			//patterns.xml_s08e7ab42-dd8f-409c-adbe-5126d8579e82.xml (+)
			ArrayList<String> orderedIDs = partofwholeorder(this.structureIDs, root);//reverse the order in original text
			for(String sid: orderedIDs){
				ArrayList<EntityProposals> entities = this.entityHash.get(sid);
				if(this.keyentities==null){
					this.keyentities = entities;
				}else if(this.keyentities.size()==0){
					this.keyentities.addAll(entities);
				}else{
					if(checkForPartOfRelation(entities)==true){//Checks ELKReasoner whether the keyentities are part of entities
						addEntityLocators(this.keyentities, entities);
						LOGGER.debug("CSP: add to part_of chain:");
						for(EntityProposals aep: entities){
							LOGGER.debug(".."+aep.toString());
						}
					}
					else
					{
						this.keyentities.addAll(entities);
					}
				}
			}
			//reverse the order back to original-like
			for(int i = 0; i<this.keyentities.size()/2; i++){
				Collections.swap(this.keyentities, i, this.keyentities.size()-i-1);
			}
		}
		/*if(entities.size()>1){//multiple unrelated entities  
		
			//1. Sereno style or the like
			//2. qualities mistreated as entities			
			
		}else if (entities.size()==1){ //one entity, must be the key entity
			keyentities=entities;
		}*/
		
		/*if(s2qs.size()>0) //if s2qs is approved, use the quality and call s2q.cleanHandledStructures
		{
			for(Structure2Quality s2q: s2qs)
			{
				s2q.cleanHandledStructures();
			}
		}*/	
	}

	/**
	 * are keyentities part of any of the entities?
	 * @param entities
	 * @return
	 */
	private boolean checkForPartOfRelation(ArrayList<EntityProposals> entities) {
		
		for(EntityProposals ep1 : this.keyentities)
		{
			for(Entity e1: ep1.getProposals())
			{
				for(EntityProposals ep2:entities)
				{
					for(Entity e2:ep2.getProposals())
					{
						if((e1.getClassIRI()!=null)&&(e2.getClassIRI()!=null))
						{
						if(XML2EQ.elk.isPartOf(e1.getClassIRI(),e2.getClassIRI()))
						{
							return true;
						}
						}
					}
				}
			}
		}
		
		return false;
	}


	/**
	 * Order the structures shared btw structureIDs and entityHash,
	 * The ordering is based on part_of relation: 0 part of 1 part of 2 etc.
	 * Note: in Sereno style, entities are listed from whole to part, which is consistent with other styles 
	 * Since 'part_of' chains are already handled by EntityParser, these structure should just be a list of structure in whole/part order,
	 * this function should just revert the order of structureIDs that are in entityHash
	 * @param structureIDs
	 * @return structure ids sorted in order of 'part of' relationship
	 */
	private ArrayList<String> partofwholeorder(ArrayList<String> structureIDs, Element root) {
		ArrayList<String> ordered = new ArrayList<String> ();
		for(String id: structureIDs){
			if(this.entityHash.containsKey(id)){
				ordered.add(0,  id);
			}
		}
		return ordered;
	}


	private void consumeQuality(String sid, ArrayList<Structure2Quality> s2qs) {
		// TODO implement comparison
		
	}


	private boolean entityWin(ArrayList<EntityProposals> entities,
			ArrayList<Structure2Quality> s2qs) {
		// TODO implement comparison
		return true;
	}

	/**
	 * type of each quality (simple or relational) determines the relation to be used to postcompose an entity
	 * @param entities
	 * @param entitylocators
	 * 
	 */
	private void addEntityLocators(ArrayList<EntityProposals> entities, ArrayList<EntityProposals> entitylocators){
		for(EntityProposals entity: entities){
			ArrayList<Entity> eps = entity.getProposals();
			ArrayList<Entity> epsresult = new ArrayList<Entity>(); //for saving postcomposed entity proposals 
			for (Entity e: eps){
				for(EntityProposals entitylocator: entitylocators){
					ArrayList<Entity> elps = entitylocator.getProposals();
					for(Entity elp: elps){
						Entity ecopy = (Entity) e.clone();
						SimpleEntity qentity = new SimpleEntity();
						qentity.setClassIRI(elp.getClassIRI());
						qentity.setConfidenceScore(elp.getConfidenceScore());
						qentity.setId(elp.getId());
						qentity.setLabel(elp.getLabel());
						qentity.setString(elp.getString());
						FormalRelation fr = Dictionary.partof;
						fr.setConfidenceScore(0.5f);
						REntity re = new REntity(fr, qentity); 
						CompositeEntity ce = new CompositeEntity(); 
						ce.addEntity(ecopy);
						ce.addParentEntity(re); //ce.addParentEntity(re)											
						epsresult.add(ce); //save a proposal
					}
				}
			}
			entity.setProposals(epsresult);
			//eps = epsresult; //update entities
		}
	}
	


	/**
	 * if an entity has a character *modifier* in the original text, use the character (simple or relational quality) 
	 * to post-compose the entity. for example "white hair: absent" => E: hair bearer of white: Q: absent
	 * 
	 * note, one structureid may be associated with multiple structures, for example 'pubis_ischium'
	 * characters associated with the structureid 
	 * @param entities
	 */
	@SuppressWarnings("unchecked")
	private void postcomposeWithQuality(ArrayList<EntityProposals> entities, String structureid, Element statement, Element root) {
		try{
			Element structure = (Element) XPath.selectSingleNode(statement, ".//structure[@id='"+structureid+"'");
			StateStatementParser ssp = new StateStatementParser(ontoutil, null, new ArrayList<String>(),statement.getChildText("text"));
			List<Element> relations = XPath.selectNodes(statement, ".//relation[@from='"+structureid+"']"); 
			ArrayList<String> StructuredQualities = new ArrayList<String>();//scope of this variable?
			for(Element relation: relations){
				if(relation.getAttribute("to")!=null && checked.contains(relation.getAttributeValue("to")+",")){
					continue; //part_of relations have already been dealt with in EntityParser
				}
				ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>();
				ArrayList<EntityProposals> entities1 = new ArrayList<EntityProposals>();
			    ssp.parseRelation(relation, root, StructuredQualities, entities1, qualities, null);
			    //entities1 is redundant and not used
				if(qualities!=null && qualities.size()!=0){
					Utilities.postcompose(entities, qualities);
				}
			}
			
			List<Element> characters = structure.getChildren("character");
			for(Element character: characters){
				ArrayList<EntityProposals> entities1 = new ArrayList<EntityProposals> ();
				ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals> ();
				ssp.parserCharacter(character, statement, root, entities1, qualities);
				//entities1 is redundant and not used
				if(qualities!=null && qualities.size()!=0){
					Utilities.postcompose(entities, qualities);
				}	
			}			
		}catch(Exception e){
			LOGGER.error("", e);
		}		
	}

	


	



	/**
	 * structid has been identified as an entity, 
	 * any structure with an id less than structid is less likely to be a quality
	 * @param structid
	 */
	private void removeS2Qbefore(String structid) {
		int structint = Integer.parseInt(structid.replaceAll("[^0-9]", ""));
		//use enumeration to avoid concurrentmodification exception
		Enumeration<String> ids = this.qualityHash.keys();
		while(ids.hasMoreElements()){
			String id = ids.nextElement();
			int idint = Integer.parseInt(id.replaceAll("[^0-9]", ""));
			if(idint <= structint) this.qualityHash.remove(id);
		}
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
