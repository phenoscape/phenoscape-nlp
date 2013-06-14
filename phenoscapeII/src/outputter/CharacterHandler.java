/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

/**
 * @author hong cui
 * Handles a character of a structure
 * grab character and constrain info from <character> tag
 * 
 *  
 * Could character be a relationalquality?
 * yes, for example, "fused"
 */
public class CharacterHandler {
	//input
	private TermOutputerUtilities ontoutil;
	Element root;
	Element chara;
	ArrayList<String> qualityclues; //may have multiple qualityclues: "color and shape of abc"
	private ArrayList<EntityProposals> keyentities;
	boolean fromcharacterstatement = false;
	
	//results
	EntityProposals entity; //the entity result will be saved here, which may be null, indicating the key entities parsed from the character statement should be used for this character
	ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>(); //the quality result will be saved here. Because n structures may be involved in constraints (hence multiple relational qualities), this needs to be an arraylist. May be relationalquality, simple quality, or negated quality
	ArrayList<EntityProposals> entityparts = new ArrayList<EntityProposals>(); //come from constraints, may have multiple.
	ArrayList<EntityProposals> primaryentities = new ArrayList<EntityProposals>(); //entities no need to be resolved
	boolean donotresolve=false;// This is to resolve between key entities and relational quality entities

	//used in process
	boolean resolve = false;
	private ToBeSolved tobesolvedentity;
	ArrayList<EntityProposals> relatedentities = new ArrayList<EntityProposals>();
	ArrayList<Entity> bilateral = new ArrayList<Entity>();
	static XPath pathCharacterUnderStucture;

	/**
	 * @param keyentities 
	 * 
	 */
	public CharacterHandler(Element root, Element chara, TermOutputerUtilities ontoutil, ArrayList<String> qualityclues, ArrayList<EntityProposals> keyentities, boolean fromecharacterstatement) {
		this.root = root;
		this.chara = chara;
		this.ontoutil = ontoutil;
		this.qualityclues = qualityclues;
		this.keyentities = keyentities;
		this.fromcharacterstatement = fromcharacterstatement;
		try {
			this.pathCharacterUnderStucture = XPath.newInstance(".//character");
		} catch (JDOMException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param root
	 * @param chara
	 * @return
	 */
	public void handle(){
		parseEntity();
		parseQuality();
		if(resolve) resolve();
	}
	
	/**
	 * can't be called out of context, so can't be a public method
	 */
	private void parseEntity(){
		Element structure = chara.getParentElement();
		if(structure.getAttributeValue("name").compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))!=0){
			String structureid = structure.getAttributeValue("id");
			String structurename = Utilities.getStructureName(root, structureid);
			EntityParser ep = new EntityParser(chara, root, structureid, structurename, fromcharacterstatement);
			this.tobesolvedentity = new ToBeSolved(structure.getAttributeValue("id"));
			this.tobesolvedentity.setEntityCandidate(ep.getEntity());
			this.tobesolvedentity.setStructure2Quality(ep.getQualityStrategy());
			this.resolve = true;			
			this.entity=ep.getEntity();
		}
		
		/*String structurename = (structure.getAttribute("constraint")!=null? 
				structure.getAttributeValue("constraint"): ""+" "+structure.getAttributeValue("name")).trim();
		String structureid = structure.getAttributeValue("id");
		if(structurename.compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))!=0){
		 //otherwise, this.entity remains null
			//parents separated by comma (,).
			String parents = Utilities.getStructureChain(root, "//relation[@from='" + structureid + "']", 0);
			this.entity = new EntitySearcherOriginal().searchEntity(root, structureid, structurename, parents, structurename, "part_of");	

			
			//if entity match is not very strong, consider whether the structure is really a quality
			//if(this.entity.higestScore() < 0.8f){
			//	Structure2Quality rq = new Structure2Quality(root, structurename, structureid, null);
			//	rq.handle();
			//	ArrayList<QualityProposals> qualities = rq.qualities;
			//	if(qualities.size()>0){
			//		boolean settled = false;
			//		for(QualityProposals qp : qualities){
			//			if(qp.higestScore() > this.entity.higestScore() && (this.keyentities!=null && this.keyentities.size()>0)){
			//				this.entity = null;
			//				this.qualities = qualities;
			//			break;
			//			}
				//	}
			//		if(!settled){
			//			//park the case and resolve it later after the quality is parsed
			//			this.tobesolvedentity = new ToBeSolved(structurename, structureid, this.entity, qualities);
			//			this.resolve = true;
			//		}					
			//	}
				
			//}

			//this.primaryentities.add(this.entity);

		//}		*/
	}
	
	
	private void parseQuality(){
		// characters => quality
		//get quality candidate
		String quality = Utilities.formQualityValueFromCharacter(chara);
		boolean negated = false;
		if(quality.startsWith("not ")){
			negated = true;
			quality = quality.substring(quality.indexOf(" ")+1).trim(); //deal with negated quality here
		}
		//is the candidate a relational quality?
		QualityProposals relationalquality = PermittedRelations.matchInPermittedRelation(quality, false);
		if(relationalquality!=null){
			//attempts to find related entity in contraints
			// constraints = qualitymodifier if quality is a relational quality
			if (chara.getAttribute("constraintid") != null) {
				ArrayList<EntityProposals> relatedentities = findEntityInConstraints();
				for(EntityProposals relatedentity: relatedentities){
					QualityProposals qproposals = new QualityProposals();
					qproposals.add(new RelationalQuality(relationalquality, relatedentity));
					this.qualities.add(qproposals);
				}
				}
			else if(structuresWithSameCharacters(this.chara.getParentElement().getParentElement())==true){// A and B: fused
				//Processes structures with same characters(RQ's) to be related => Hariharan
				Hashtable<String,ArrayList<EntityProposals>> entities = this.processStructuresWithSameCharacters(this.chara.getParentElement().getParentElement());
				addREPE(entities,relationalquality);
			}//check whether the parent structure(this.entity) is a bilateral entity
			else if((this.entity!=null)&&(checkBilateral(this.entity)==true))//bilateral structures: fused
			{
				this.primaryentities.clear();//Since among the primary entities only some are bilateral and is relevant to this character/relational quality => Hariharan
				for(Entity e:this.bilateral)
				{
				RelationalEntityStrategy1 re = new RelationalEntityStrategy1(e);
				re.handle();
				Hashtable<String,ArrayList<EntityProposals>> entities = re.getEntities();
				addREPE(entities,relationalquality);
				}
			}
			else if(structuresWithSameCharacters(this.chara.getParentElement().getParentElement())==false)//if entity is null,then structure is whole organism, it should be handled here
				//single, non-bilateral structure: fused: for example whole_organism:fused
			{
				//Handling characters that belong to a single structure => Hariharan
				Hashtable<String,ArrayList<EntityProposals>> entities = SingleStructures();
				addREPE(entities,relationalquality);
			}
			else
			{
				//Handle using text
			}
			if((this.primaryentities.size()>0)&&(this.qualities.size()>0))
				donotresolve=true;
				return;
		}
		
		
		//constraints may yield entity parts such as entity locator, save those, resolve them later
		if (chara.getAttribute("constraintid") != null) {
			ArrayList<EntityProposals> entities = findEntityInConstraints();
			for(EntityProposals entity: entities){
				this.entityparts.add(entity);
			}
		}
		

		//not a relational quality, is this a simple quality or a negated quality?

		TermSearcher ts = new TermSearcher();
		Quality result = (Quality) ts.searchTerm(quality, "quality");
		if(result!=null){ //has a strong match
			if(negated){
				/*TODO use parent classes Jim use for parent classes*/
				String [] parentinfo = ontoutil.retreiveParentInfoFromPATO(result.getId()); 
				Quality parentquality = new Quality();
				parentquality.setString(parentinfo[1]);
				parentquality.setLabel(parentinfo[1]);
				parentquality.setId(parentinfo[0]);		
				QualityProposals qproposals = new QualityProposals();
				qproposals.add(new NegatedQuality(result, parentquality));
				this.qualities.add(qproposals);
				return;
			}else{
				QualityProposals qproposals = new QualityProposals();
				qproposals.add(result);
				this.qualities.add(qproposals);
				return;
			}
		}else{//no match for quality, could it be something else?
			//try to match it in entity ontologies	  
			//text::Caudal fin heterocercal  (heterocercal tail is a subclass of caudal fin)
			//xml: structure: caudal fin, character:heterocercal
			//=> heterocercal tail: present
			if((this.entity!=null)&&(this.entity.higestScore()>=0.8f)){
				for(Entity e: entity.getProposals()){
					Character2EntityStrategy2 ces = new Character2EntityStrategy2(e, quality);
					ces.handle();
					if(ces.getEntity()!=null && ces.getQuality()!=null){
						this.entity = ces.getEntity();
						this.qualities.add(ces.getQuality());
						return;
					}
				}
			}
		}
		
		//TODO: could this do any good?
		//will resolve() do any good?
		//Entity result = (Entity) ts.searchTerm(quality, "entity");
		

		//still not successful, check other matches
		for(FormalConcept aquality: ts.getCandidateMatches()){
			if((qualityclues!=null)&&(qualityclues.size()!=0))
				for(String clue: qualityclues){
					Quality qclue = (Quality)ts.searchTerm(clue, "quality");
					if(aquality.getLabel().compareToIgnoreCase(clue)==0 || ontoutil.isChildQuality(aquality.getClassIRI(), qclue.getClassIRI()) ){
						aquality.setConfidenceScore(1.0f); //increase confidence score
					}					
				}
			//no clue or clue was not helpful
			QualityProposals qproposals = new QualityProposals();
			qproposals.add((Quality)aquality);
			this.qualities.add(qproposals); //keep confidence score as is
		}
		if(this.qualities.size()==0){
			result=new Quality();
			result.string=quality;
			result.confidenceScore= 0.0f; //TODO: confidence score of no-ontologized term = goodness of the phrase for ontology
			QualityProposals qproposals = new QualityProposals();
			qproposals.add(result);
			this.qualities.add(qproposals);
		}
		return;
		
	}
	
//Checks whether the parent structure is bilateral or not
	//this.entity contains the parent entity
	
private boolean checkBilateral(EntityProposals ep) {
		
	EntityProposals epclone = ep.clone();//cloning to avoid original entity proposals to be changed
		
	boolean bilateralcheck = false;
	for(Entity e:epclone.getProposals())
	{
		if(e.getId()!=null)
		{
			if(e instanceof SimpleEntity)
			{
				if(XML2EQ.elk.lateralsidescache.get(e.getPrimaryEntityLabel())!=null)
				{				
					this.bilateral.add(e);
					bilateralcheck=true;
				}
			}
			else
			{
				for(Entity e1:((CompositeEntity) e).getEntities())
				{
					if(XML2EQ.elk.lateralsidescache.get(e1.getPrimaryEntityLabel())!=null)
				{
					this.bilateral.add(e);
					bilateralcheck=true;
					break;
				}
				}
			}
		}
	}
		return bilateralcheck;
	}



private boolean structuresWithSameCharacters(Element statement) {
	
	try {
		List<Element> characters;
		characters = pathCharacterUnderStucture.selectNodes(statement);
	int count=0;
//Checks for same characters present under multiple structures
	Iterator<Element> itr = characters.listIterator();
	while(itr.hasNext())
	{
		Element character = itr.next();
		if((character.getAttribute("name").getValue().equals(this.chara.getAttribute("name").getValue()))&&(character.getAttribute("value").getValue().equals(this.chara.getAttribute("value").getValue())))
			count++;
	}
	if(count>1)
		return true;
	else
		return false;
	
	} catch (JDOMException e) {
		e.printStackTrace();
	}
	return false;
	}


//Adds Related entities and Primary entities to existing identified entities. Also remove duplicates in the entities
private void addREPE(Hashtable<String, ArrayList<EntityProposals>> entities, QualityProposals relationalquality) {
		

	
	ArrayList<EntityProposals> primaryentities = entities.get("Primary Entity");
	ArrayList<EntityProposals> relatedentities = entities.get("Related Entities");
	
	if((relatedentities!=null)&&relatedentities.size()>0)
	{
		for(EntityProposals relatedentity: relatedentities){
			QualityProposals qproposals = new QualityProposals();
			qproposals.add(new RelationalQuality(relationalquality, relatedentity));
			this.qualities.add(qproposals);
		}
		if(primaryentities.size()>0)
		{
			ListIterator<EntityProposals> itr1 = primaryentities.listIterator();
			//to remove duplicate entities
			while(itr1.hasNext())
			{
				EntityProposals ep1 = (EntityProposals) itr1.next();
				for(Entity en1: ep1.getProposals())
				{
				ListIterator<EntityProposals> itr2 = this.primaryentities.listIterator();
				while(itr2.hasNext())
				{
					EntityProposals ep2 = (EntityProposals) itr2.next();
					for(Entity en2:ep2.getProposals())//add more conditions to analyze and handle bilateral entities
						if(en2.getPrimaryEntityLabel().equals(en1.getPrimaryEntityLabel()))
							itr1.remove();
				}
				}
			}
			this.primaryentities.addAll(primaryentities);
		}
	}
		
	}

/*
 * It identifies structures which have the same characters(Relational Quality) 
 * and it returns all the structures which has the same characters as related entities
 * if a structure is a whole organism, it calls bilateralstructures to handle the scenario.
 * 
 * 
 */			@SuppressWarnings("unchecked")
	private Hashtable<String, ArrayList<EntityProposals>> processStructuresWithSameCharacters(Element statement) {
		//TODO: Handle scenario where all the characters have whole organism as its parent structure and Keyentities is null
		try {
			List<Element> characters = pathCharacterUnderStucture.selectNodes(statement);
			Hashtable<String,ArrayList<EntityProposals>> entities = new Hashtable<String,ArrayList<EntityProposals>>();
			ArrayList<EntityProposals> primaryentities = new ArrayList<EntityProposals>();
			ArrayList<EntityProposals> relatedentities = new ArrayList<EntityProposals>();
			
			//Remove all other characters that are not same as the character under consideration 
			Iterator<Element> itr = characters.listIterator();
			while(itr.hasNext())
			{
				Element character = itr.next();
				if(!(character.getAttribute("name").getValue().equals(this.chara.getAttribute("name").getValue()))||!(character.getAttribute("value").getValue().equals(this.chara.getAttribute("value").getValue())))
					itr.remove();
			}
			//First entity will be the main primary entity
			
			if(characters.size()>1)
			{
				if(!chara.getParentElement().getAttributeValue("name").equals(ApplicationUtilities.getProperty("unknown.structure.name")))
				{
					primaryentities.add(this.entity);//Since this is the identified entity of this character
				}
					
			}
			//finding the related entities using structures with same character name and value
			for(int i=1;i<characters.size();i++)
			{
				Element character = characters.get(i);
				if((character.getAttribute("name").getValue().equals(this.chara.getAttribute("name").getValue()))&&(character.getAttribute("value").getValue().equals(this.chara.getAttribute("value").getValue())))
					{
					//read the structure(other than whole organism) of this character, find the entity,create entity proposals
					Element ParentStructure = character.getParentElement();
					if(!ParentStructure.getAttributeValue("name").equals(ApplicationUtilities.getProperty("unknown.structure.name")))
					{
						EntityProposals entity  = new EntitySearcherOriginal().searchEntity(root, ParentStructure.getAttributeValue("id"), ParentStructure.getAttributeValue("name"), "", "","");				
					if(entity!=null)
						relatedentities.add(entity);
					character.detach();
					}
						
					}
			}
		if(relatedentities.size()>0)
		{
			entities.put("Related Entities", relatedentities);
			entities.put("Primary Entity", primaryentities);
		}
				
		return entities;

		} catch (JDOMException e) {
			e.printStackTrace();
		}
		return null;
	}
	
 private Hashtable<String, ArrayList<EntityProposals>> SingleStructures() {
		
					Element ParentStructure = this.chara.getParentElement();
					ArrayList<EntityProposals> primaryentities = new ArrayList<EntityProposals>();
					ArrayList<EntityProposals> relatedentities = new ArrayList<EntityProposals>();
					Hashtable<String,ArrayList<EntityProposals>> entities = new Hashtable<String,ArrayList<EntityProposals>>();
					
					if(ParentStructure.getAttributeValue("name").equals(ApplicationUtilities.getProperty("unknown.structure.name")))
					{
						if(this.keyentities.size()>1)//If keyentities.size> 1, first entity is a primary entity and the rest are related entities
						{
						for(int i=1;i<this.keyentities.size();i++)
							relatedentities.add(this.keyentities.get(i));
						
						primaryentities.add(this.keyentities.get(0));

						}
						else if((this.keyentities.size()==1)&&(this.checkBilateral(this.keyentities.get(0))==true))//If keyentities.size==1, the it can be a bilateral entity
						{
							//call bilateral strategy on the single key entity
							for(Entity e:this.bilateral)
							{
							RelationalEntityStrategy1 re = new RelationalEntityStrategy1(e);
							re.handle();
							Hashtable<String,ArrayList<EntityProposals>> entities1 = re.getEntities();
							relatedentities.addAll(entities1.get("Related Entities"));
							primaryentities.addAll(entities1.get("Primary Entity"));
							}
						}
						else
						{
								//TODO process text
						}
							
						entities.put("Related Entities", relatedentities);
						entities.put("Primary Entity", primaryentities);
						
					}
						
			return entities;
		
	}

	
	private ArrayList<EntityProposals> findEntityInConstraints() {
		ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
		if (chara.getAttribute("constraintid") != null) {
			String[] conids = chara.getAttributeValue("constraintid").split("\\s+");
			try{
				for(String conid: conids){
					String qualitymodifier = Utilities.getStructureName(root, conid);
					//parents separated by comma (,).
					String qualitymodifierparents = Utilities.getStructureChain(root, "//relation[@from='" + chara.getAttributeValue("constraintid") + "']", 0);
					EntityProposals result = new EntitySearcherOriginal().searchEntity(root, conid, qualitymodifier, "", qualitymodifierparents,"");	
					if(result!=null) entities.add(result);
				}
				return entities;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * resolve for entities from entity and entity parts obtained from constraints
	 * also when neither entity and qualities scores are strong, the keyentities scores are not strong or not ontologized
	 * should try to resove them here? or in the end?
	 */
	public void resolve(){
		//TODO
		//this was how s2q was used before in SSP.
		//Structure2Quality rq2 = new Structure2Quality(root,
		//		structname, structid, this.keyentities);
		//rq2.handle();
		//if(rq2.qualities.size()>0){
		//	entity = rq2.primaryentities;
	    //  qualities = rq2.qualities;
		//} else {
	}
	

	public ArrayList<QualityProposals> getQualities(){
		return this.qualities;
	}

	public EntityProposals getEntity(){
		return this.entity;
	}
	

	private class ToBeSolved{
		
		//private String structurename;
		private String structureid;
		private EntityProposals entity;
		private Structure2Quality s2q;
		private QualityProposals quality;

		public ToBeSolved(String structureid){
			//this.structurename = structurename;
			this.structureid = structureid;
			//this.entity = entity;
			//this.s2q = s2q;			
		}
		
		public void setEntityCandidate(EntityProposals entity){
			this.entity = entity;
		}
		
		public void setStructure2Quality(Structure2Quality s2q){
			this.s2q = s2q;
		}
		
		public void setQualityCandidate(QualityProposals quality){
			this.quality = quality;
		}
		
		
	}
		

	public ArrayList<EntityProposals> getPrimaryentities() {
		return primaryentities;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
