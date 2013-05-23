/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

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
	/**
	 * 
	 */
	public CharacterHandler(Element root, Element chara, TermOutputerUtilities ontoutil, ArrayList<String> qualityclues) {
		this.root = root;
		this.chara = chara;
		this.ontoutil = ontoutil;
		this.qualityclues = qualityclues;
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
		resolve();
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
				return;
			}else{
				//TODO check if the subject entity is a bilateral paired organ
				return;
			}
		}
		
		
		//constarints may yield entity parts such as entity locator, save those, resolve them later
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
			if(this.entity.hasOntologizedWithHighConfidence()){
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
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
