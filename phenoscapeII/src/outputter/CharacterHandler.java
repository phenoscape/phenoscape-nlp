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
	private TermOutputerUtilities ontoutil;
	Element root;
	Element chara;
	EntityProposals entity; //the entity result will be saved here, which may be null, indicating the ke y entities parsed from the character statement should be used for this character
	ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>(); //the quality result will be saved here. Because n structures may be involved in constraints (hence multiple relational qualities), this needs to be an arraylist. May be relationalquality, simple quality, or negated quality
	ArrayList<EntityProposals> entityparts = new ArrayList<EntityProposals>(); //come from constraints, may have multiple.
	ArrayList<String> qualityclues; //may have multiple qualityclues: "color and shape of abc"
	boolean resolve = false;
	private ToBeSolved tobesolvedentity;
	private ArrayList<EntityProposals> keyentities;
	ArrayList<EntityProposals> primaryentities = new ArrayList<EntityProposals>();
	ArrayList<EntityProposals> relatedentities = new ArrayList<EntityProposals>();

	static XPath pathCharacterUnderStucture;

	/**
	 * @param keyentities 
	 * 
	 */
	public CharacterHandler(Element root, Element chara, TermOutputerUtilities ontoutil, ArrayList<String> qualityclues, ArrayList<EntityProposals> keyentities) {
		this.root = root;
		this.chara = chara;
		this.ontoutil = ontoutil;
		this.qualityclues = qualityclues;
		this.keyentities = keyentities;
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
	
	
	public void parseEntity(){
		Element structure = chara.getParentElement();
		String structurename = (structure.getAttribute("constraint")!=null? 
				structure.getAttributeValue("constraint"): ""+" "+structure.getAttributeValue("name")).trim();
		String structureid = structure.getAttributeValue("id");
		if(structurename.compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))!=0){ //otherwise, this.entity remains null
			//parents separated by comma (,).
			String parents = Utilities.getStructureChain(root, "//relation[@from='" + structureid + "']");
			this.entity = new EntitySearcherOriginal().searchEntity(root, structureid, structurename, "", parents,"");	

			
			//if entity match is not very strong, consider whether the structure is really a quality
			/*if(this.entity.higestScore() < 0.8f){
				Structure2Quality rq = new Structure2Quality(root, structurename, structureid, null);
				rq.handle();
				ArrayList<QualityProposals> qualities = rq.qualities;
				if(qualities.size()>0){
					boolean settled = false;
					for(QualityProposals qp : qualities){
						if(qp.higestScore() > this.entity.higestScore() && (this.keyentities!=null && this.keyentities.size()>0)){
							this.entity = null;
							this.qualities = qualities;
							break;
						}
					}
					if(!settled){
						//park the case and resolve it later after the quality is parsed
						this.tobesolvedentity = new ToBeSolved(structurename, structureid, this.entity, qualities);
						this.resolve = true;
					}					
				}
				
			}*/

			this.primaryentities.add(this.entity);

		}		
	}
	
	
	public void parseQuality(){
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
			else if(Structureswithsamecharacters(this.chara.getParentElement().getParentElement())==true){
				
				Hashtable<String,ArrayList<EntityProposals>> entities = this.Processstructureswithsamecharacters(this.chara.getParentElement().getParentElement());
				addREPE(entities,relationalquality);
			}
			else if(XML2EQ.elk.lateralsidescache.get(this.chara.getParentElement())!=null)//bilateral structures
			{
				RelationalEntityStrategy1 re = new RelationalEntityStrategy1(this.root,this.chara.getParentElement(),this.keyentities);
				re.handle();
				Hashtable<String,ArrayList<EntityProposals>> entities = re.getEntities();
				addREPE(entities,relationalquality);

			}
			else if(Structureswithsamecharacters(this.chara.getParentElement().getParentElement())==false)
			{
				//Handling structures that belong to single character
				Hashtable<String,ArrayList<EntityProposals>> entities = SingleStructures();
				addREPE(entities,relationalquality);
			}
			else
			{
				//Handle using text
			}
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
	
	
	
private boolean Structureswithsamecharacters(Element statement) {
	
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


//Adds Related entities and Primary entities to existing identified entities
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
					for(Entity en2:ep2.getProposals())
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
	private Hashtable<String, ArrayList<EntityProposals>> Processstructureswithsamecharacters(Element statement) {
		
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
				if(chara.getParentElement().getAttributeValue("name")!=ApplicationUtilities.getProperty("unknown.structure.name"))
				{
					EntityProposals primaryEntity  = new EntitySearcherOriginal().searchEntity(root, chara.getParentElement().getAttributeValue("id"), chara.getParentElement().getAttributeValue("name"), "", "","");
					primaryentities.add(primaryEntity);
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
					if(ParentStructure.getAttributeValue("name")!=ApplicationUtilities.getProperty("unknown.structure.name"))
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
						if(this.keyentities.size()>1)
						{
						for(int i=1;i<this.keyentities.size();i++)
							relatedentities.add(this.keyentities.get(i));
						
						primaryentities.add(this.keyentities.get(0));
						
						entities.put("Related Entities", relatedentities);
						entities.put("Primary Entity", primaryentities);
						}
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
					String qualitymodifierparents = Utilities.getStructureChain(root, "//relation[@from='" + chara.getAttributeValue("constraintid") + "']");
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
	}
	

	public ArrayList<QualityProposals> getQualities(){
		return this.qualities;
	}

	public EntityProposals getEntity(){
		return this.entity;
	}
	

	private class ToBeSolved{
		
		private String structurename;
		private String structureid;
		private EntityProposals entity;
		private ArrayList<QualityProposals> qualities;

		public ToBeSolved(String structurename, String structureid, EntityProposals entity, ArrayList<QualityProposals> qualities){
			this.structurename = structurename;
			this.structureid = structureid;
			this.entity = entity;
			this.qualities = qualities;			
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
