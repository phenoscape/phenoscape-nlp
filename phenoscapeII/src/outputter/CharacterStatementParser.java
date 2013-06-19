/**
 * 
 */
package outputter;

import java.util.Enumeration;
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
	//ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
	ArrayList<EntityProposals> keyentities = new ArrayList<EntityProposals>();
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
			parseForEntities(statement, root, true);
		} catch (Exception e) {
			e.printStackTrace();
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
					if(name.indexOf("_")>0 || name.indexOf("/")>0){//case of 'pubis[_/]ischium', one structure id for two structures.
						String[] names = name.split("[_/]");
						underscoredStructureIDs.add(structureid);
						for(String aname: names){
							String parents = parseStructure(statement, root,
									fromcharacterdescription, /*entities, s2qs,*/
									structureid, aname); //use the same structureid for all structures
							checked += structureid+","+parents+",";
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
			e.printStackTrace();
		}
	}


	private String parseStructure(Element statement, Element root,
			boolean fromcharacterdescription, String structureid, String structurename) {
		String parents = Utilities.getStructureChainIds(root, "//relation[@name='part_of'][@from='" + structureid + "']" +
				 "|//relation[@name='in'][@from='" + structureid + "']" +
				 "|//relation[@name='on'][@from='" + structureid + "']", 0); //list of structures separated with ","
		if(debug){
			System.out.println("parse structure:"+structurename);
		}
		EntityParser ep = new EntityParser(statement, root, structureid, structurename, fromcharacterdescription);
		if(ep.getEntity()!=null){
			ArrayList<EntityProposals> entities;
			if(this.entityHash.get(structureid)==null){
				entities = new ArrayList<EntityProposals>();
			}else{
				entities = this.entityHash.get(structureid);			
			}
			entities.add(ep.getEntity());
			this.entityHash.put(structureid, entities);
			if(debug){
				System.out.println("matched entities:");
				for(EntityProposals aep: entities){
					System.out.println(aep.toString());
				}
				System.out.println();
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
			if(debug){
				System.out.println("resolving underscored structures...");
			}
			ArrayList<EntityProposals> entities = this.entityHash.get(structid);
			if(entities!=null){
				if(debug){
					System.out.println("entities from underscored structures:");
					for(EntityProposals aep: entities){
						System.out.println(aep.toString());
					}
					System.out.println("post-composed with quality...");
				}	
				postcomposeWithQuality(entities, structid, statement, root);//for resolved entities only; update entities in this entityHash
				foundaentity = true;
				this.qualityHash.remove(structid);
				//remove from s2q with a structid < the first struct id 
				if(count==0) removeS2Qbefore(structid);
				this.structureIDs.remove(structid);
			}
			count++;
		}
		
		for(int i = 0; i < this.structureIDs.size(); i++){
			String structid = this.structureIDs.get(i);
			ArrayList<EntityProposals> entities = this.entityHash.get(structid);
			if(entities!=null && entities.size()>1){ //entities resulted from 1 structureid
				if(debug){
					System.out.println("entities from structure id:"+structid);
					for(EntityProposals aep: entities){
						System.out.println(aep.toString());
					}
					System.out.println("post-composed with quality...");
				}	
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
				if(debug) {
					System.out.println("selecting btw entity and quality for structure: "+structid);
				
					System.out.println("matched entities:");
					for(EntityProposals aep: entities){
						System.out.println(aep.toString());
					}
					
					System.out.println("matched qualities:");
					for(Structure2Quality as2q: s2qs){
						for(QualityProposals qp: as2q.qualities){
							System.out.println(qp.toString());
						}
					}
					
					System.out.println("entity win? " +entitywin);
				}
				if(entitywin) this.qualityHash.remove(structid);
				if(!entitywin) this.entityHash.remove(structid);
			}
			
		}		
		
		//remaining s2qs
		if(this.qualityHash!=null){
			Enumeration<String> keys = this.qualityHash.keys();
			while(keys.hasMoreElements()){
				String sid = keys.nextElement();
				if(debug) {
					System.out.println("remaining qualities from s2q:");
				}
				ArrayList<Structure2Quality> s2qs = this.qualityHash.get(sid);
				//post compose quality to the closest proceeding entity, or
				//create EQ with the closest following entity
				//if no entity exsits, give up
				if(debug) {
					for(Structure2Quality as2q: s2qs){
						for(QualityProposals qp: as2q.qualities){
							System.out.println(qp.toString());
						}
					}
					System.out.println("how to consume this? ");
				}
				consumeQuality(sid, s2qs); //update this.entityHash
			}
		}
		
		if(this.entityHash!=null && this.entityHash.size()>0){
			if(debug){
				System.out.println("remaining entities: post-compose with 'part_of':");
			}
			FormalRelation fr = new FormalRelation();
			fr.setClassIRI("http://purl.obolibrary.org/obo/BFO_0000050");
			fr.setConfidenceScore(0.5f);
			fr.setId("BFO:0000050");
			fr.setLabel("part of");
			fr.setString("");
			//remaining entities
			//compose entities using 'part_of' relation
			ArrayList<String> orderedIDs = order(this.structureIDs, root);
			for(String sid: orderedIDs){
				ArrayList<EntityProposals> entities = this.entityHash.get(sid);
				if(debug){
					System.out.println("add entities from structure "+sid+" to part_of chain:");
					for(EntityProposals aep: entities){
						System.out.println(aep.toString());
					}
				}
				if(this.keyentities==null){
					this.keyentities = entities;
				}else if(this.keyentities.size()==0){
					this.keyentities.addAll(entities);
				}else{
					addEntityLocators(this.keyentities, entities);
				}
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
	 * Order the structures shared btw structureIDs and entityHash,
	 * The ordering is based on part_of relation: 0 part of 1 part of 2 etc.
	 * Note: in Sereno style, entities are listed from whole to part, which is consistent with other styles 
	 * Since 'part_of' chains are already handled by EntityParser, these structure should just be a list of structure in whole/part order,
	 * this function should just revert the order of structureIDs that are in entityHash
	 * @param structureIDs
	 * @return structure ids sorted in order of 'part of' relationship
	 */
	private ArrayList<String> order(ArrayList<String> structureIDs, Element root) {
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
						qentity.setConfidenceScore(elp.getConfidienceScore());
						qentity.setId(elp.getId());
						qentity.setLabel(elp.getLabel());
						qentity.setString(elp.getString());
						FormalRelation fr = new FormalRelation();
						fr.setClassIRI("http://purl.obolibrary.org/obo/BFO_0000050");
						fr.setConfidenceScore(0.5f);
						fr.setId("BFO:0000050");
						fr.setLabel("part of");
						fr.setString("");
						REntity re = new REntity(fr, qentity); //bear of some Ossified
						CompositeEntity ce = new CompositeEntity(); 
						ce.addEntity(ecopy);
						ce.addEntity(re);											
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
			StateStatementParser ssp = new StateStatementParser(ontoutil, null, new ArrayList<String>());
			List<Element> relations = XPath.selectNodes(statement, ".//relation[@from='"+structureid+"']"); 
			ArrayList<String> StructuredQualities = new ArrayList<String>();//scope of this variable?
			for(Element relation: relations){
				if(relation.getAttribute("to")!=null && checked.contains(relation.getAttributeValue("to")+",")){
					continue; //part_of relations have already been dealt with in EntityParser
				}
				ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>();
				ArrayList<EntityProposals> entities1 = new ArrayList<EntityProposals>();
			    ssp.parseRelation(relation, root, StructuredQualities, entities1, qualities);
			    //entities1 is redundant and not used
				if(qualities!=null && qualities.size()!=0){
					postcompose(entities, qualities);
				}
			}
			
			List<Element> characters = structure.getChildren("character");
			for(Element character: characters){
				ArrayList<EntityProposals> entities1 = new ArrayList<EntityProposals> ();
				ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals> ();
				ssp.parserCharacters(character, statement, root, entities1, qualities);
				//entities1 is redundant and not used
				if(qualities!=null && qualities.size()!=0){
					postcompose(entities, qualities);
				}	
			}			
		}catch(Exception e){
			e.printStackTrace();
		}		
	}

	/**
	 * type of each quality (simple or relational) determines the relation to be used to postcompose an entity
	 * @param entities
	 * @param qualities
	 * 
	 */
	private void postcompose(ArrayList<EntityProposals> entities, ArrayList<QualityProposals> qualities){
		for(EntityProposals entity: entities){
			ArrayList<Entity> eps = entity.getProposals();
			ArrayList<Entity> epsresult = new ArrayList<Entity>(); //for saving postcomposed entity proposals 
			for (Entity e: eps){
				for(QualityProposals quality: qualities){
					ArrayList<Quality> qps = quality.getProposals();
					for(Quality q: qps){
						if(q instanceof RelationalQuality){
							//check if the relation is in the restricted list for post composition
							QualityProposals relation = ((RelationalQuality) q).getQuality();
							EntityProposals rentity= ((RelationalQuality) q).getRelatedEntity();
							ArrayList<Quality> relations = relation.getProposals();
							for(Quality r : relations){
								if(r.isOntologized() && isRestrictedRelation(r.getId())){
									Entity ecopy = (Entity) e.clone(); //create fresh copy
									//increase confidence
									//create RE and create compositeEntity
									FormalRelation fr = new FormalRelation();
									fr.setClassIRI(r.getClassIRI());
									fr.setConfidenceScore(r.getConfidienceScore());
									fr.setId(r.getId());
									fr.setLabel(r.getLabel());
									fr.setString(r.getString());
									for(Entity e1: rentity.getProposals()){
										REntity re = new REntity(fr, e1);
										if(ecopy instanceof CompositeEntity){
											((CompositeEntity) ecopy).addEntity(re); 
											epsresult.add(ecopy); //save a proposal
										}else{
											CompositeEntity ce = new CompositeEntity(); 
											ce.addEntity(ecopy);
											ce.addEntity(re);											
											epsresult.add(ce); //save a proposal
										}										
									}							
								}
							}
						}else{
							//bear_of some Ossified: quality Ossified must be treated as a simple entity to form a composite entity
							Entity ecopy = (Entity) e.clone();
							SimpleEntity qentity = new SimpleEntity();
							qentity.setClassIRI(q.getClassIRI());
							qentity.setConfidenceScore(q.getConfidienceScore());
							qentity.setId(q.getId());
							qentity.setLabel(q.getLabel());
							qentity.setString(q.getString());
							FormalRelation fr = new FormalRelation();
							fr.setClassIRI("http://purl.obolibrary.org/obo/BFO_0000053");
							fr.setConfidenceScore(1f);
							fr.setId("BFO:0000053");
							fr.setLabel("bear of");
							fr.setString("");
							REntity re = new REntity(fr, qentity); //bear of some Ossified
							CompositeEntity ce = new CompositeEntity(); 
							ce.addEntity(ecopy);
							ce.addEntity(re);											
							epsresult.add(ce); //save a proposal
						}
					}
				}
			}
			eps = epsresult; //update entities
		}
	}
	
	private boolean isRestrictedRelation(String id) {
		if(Dictionary.resrelationQ.get(id) == null) return false;
		return true;
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
