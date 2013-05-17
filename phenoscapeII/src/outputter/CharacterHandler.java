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
 * Handles the characters of a structure
 * grab character and constrain info from <character> tag
 * return a hashtable:
 * key: "quality" value: qualityID
 * key: "qualitymodifier" value: qualitymodifierID
 * 
 *  
 * TODO could character be a relationalquality?
 * yes, for example, "fused"
 */
public class CharacterHandler {
	private TermOutputerUtilities ontoutil;
	Element root;
	Element chara;
	Entity entity; //the entity result will be saved here, which may be null, indicating the ke y entities parsed from the character statement should be used for this character
	ArrayList<Quality> qualities = new ArrayList<Quality>(); //the quality result will be saved here. May be relationalquality, simple quality, or negated quality
	ArrayList<Entity> entityparts = new ArrayList<Entity>();
	ArrayList<String> qualityclues;
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
		String structurename = structure.getAttributeValue("name");
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
			quality = quality.substring(quality.indexOf(" ")+1).trim(); //TODO: deal with negated quality here
		}
		//is the candidate a relational quality?
		Quality relationalquality = PermittedRelations.matchInPermittedRelation(quality, false);
		if(relationalquality!=null){
			//attempts to find related entity in contraints
			// constraints = qualitymodifier if quality is a relational quality
			if (chara.getAttribute("constraintid") != null) {
				ArrayList<Entity> relatedentities = findEntityInConstraints();
				for(Entity relatedentity: relatedentities){
					this.qualities.add(new RelationalQuality(relationalquality, relatedentity));
					
				}
				return;
			}else{
				//TODO check if the subject entity is a bilateral paired organ
				return;
			}
		}
		
		
		//constarints may yield entity parts such as entity locator, save those, resolve them later
		if (chara.getAttribute("constraintid") != null) {
			ArrayList<Entity> entities = findEntityInConstraints();
			for(Entity entity: entities){
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
				
				this.qualities.add(new NegatedQuality(result, parentquality));
				return;
			}else{
				this.qualities.add(result);
				return;
			}
		}else{
			//check other matches
			for(FormalConcept match: ts.getCandidateMatches()){
				for(String clue: qualityclues){
					//TODO
				}
			}
			result=new Quality();
			result.string=quality;
			result.confidenceScore=(float) 1.0;
			this.qualities.add(result);
			return;
		}
	}
	
	
	
	private ArrayList<Entity> findEntityInConstraints() {
		ArrayList<Entity> entities = new ArrayList<Entity>();
		if (chara.getAttribute("constraintid") != null) {
			String[] conids = chara.getAttributeValue("constraintid").split("\\s+");
			try{
				for(String conid: conids){
					String qualitymodifier = Utilities.getStructureName(root, conid);
					//parents separated by comma (,).
					String qualitymodifierparents = Utilities.getStructureChain(root, "//relation[@from='" + chara.getAttributeValue("constraintid") + "']");
					Entity result = new EntitySearcherOriginal().searchEntity(root, conid, qualitymodifier, "", qualitymodifierparents,"");	
					if(result!=null) entities.add(result);
				}
				return entities;
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public void resolve(){
		//TODO
	}
	
	public ArrayList<Quality> getQualities(){
		return this.qualities;
	}

	public Entity getEntity(){
		return this.entity;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
