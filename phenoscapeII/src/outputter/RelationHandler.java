/**
 * 
 */
package outputter;


import java.util.ArrayList;
import java.util.Hashtable;
import org.jdom.Element;


/**
 * @author Hong Cui
 * Restricted relation list:
 * http://phenoscape.org/wiki/Guide_to_Character_Annotation#Relations_used_for_post-compositions
 */
public class RelationHandler {
	EntityProposals entity; //entity holds the result on entity, may be simple or composite (with a entity locator)
	QualityProposals quality; //quality (simple quality or relational quality) or negated quality. If relational quality, must have qualitymodifier (i.e. related entity)
	ArrayList<EQStatementProposals> otherEQs;
	
	Element root;
	String relation;
	String tostructname;
	String tostructid;
	String fromstructname;
	String fromstructid;
	Element relelement;
	boolean negation; //if true, negate the relation string
	boolean fromcharacterstatement;
	
	public RelationHandler(Element root, String relation, Element relelement, String tostructname, String tostructid, String structname, String structid, boolean negation, boolean keyelement){
		this.root = root;
		this.relation = relation;
		this.tostructname = tostructname;
		this.tostructid = tostructid;
		this.fromstructname = structname;
		this.fromstructid = structid;
		this.fromcharacterstatement = keyelement;
		this.otherEQs = new ArrayList<EQStatementProposals>();
		this.relelement = relelement;
		
	}
	
	/**
	 * identify entitylocator, qualitymodifier and/or relationalquality (the last is based on restricted relation list) from this relation
	 * the process may also generate EQ such as xyz:absent from "without" phrases
	 * 
	 * @param root of the description
	 * @param relationstrings, each with a format of "fromid relation_string tostructid"
	 * @param structurename the from_structure
	 * @return key: "qualitymodifier|entitylocator|relationalquality|qualitymodifierid|entitylocatorid|relationalqualityid|qualitymodifierlabel|entitylocatorlabel|relationalqualitylabel|extraEQs(list of hashes)" "
	 */
	public void handle(){
		parseEntity();
		parseRelation(); //may added to entity an entity locator
	}
		
		
		
	private void parseEntity(){
		if(fromstructname.compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))!=0){ //otherwise, this.entity remains null
			//parents separated by comma (,).
			String parents = Utilities.getStructureChain(root, "//relation[@from='" + fromstructid + "']", 0);
			this.entity = new EntitySearcherOriginal().searchEntity(root, fromstructid, fromstructname, parents, fromstructname,"part_of");	//corrected by Hong
			//shouldn't this calls EntityParser? and then resolve the conflicts, like characterhandler do?
			//could fromstruct be a quality?
		}		
	}

	private void parseRelation(){
		
		//TODO call EntitySearcher to help with entitylocator identification?
		//TODO use character info to help with entity identification?
		//TODO negation

		QualityProposals relationalquality = PermittedRelations.matchInPermittedRelation(relation, negation);
		tostructname = tostructname + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + tostructid + "']" +
																			 "|//relation[@name='in'][@from='" + tostructid + "']" +
																			 "|//relation[@name='on'][@from='" + tostructid + "']", 0); //in/on = part_of?
		tostructname = tostructname.replaceFirst(",$", "").trim();
		if(relationalquality !=null){ //yes, the relation is a relational quality
			EntityProposals relatedentity = new EntitySearcherOriginal().searchEntity(root, tostructid, tostructname, "", tostructname, relation);
			RelationalQuality rq = new RelationalQuality(relationalquality, relatedentity);
			if(quality==null)
				quality = new QualityProposals();
			quality.add(rq);			
		}else{//no, the relation should not be considered relational quality
			//entity locator? parseEntity should have already processed entity locators if any
			/*if (relation.matches("\\b(" + outputter.Dictionary.positionprep + ")\\b.*")) { // entitylocator
				this.entity = new EntitySearcherOriginal().searchEntity(root, fromstructid, fromstructname, tostructname, fromstructname, "part_of");
				//Entity entity = new EntitySearcherOriginal().searchEntity(root, tostructid, tostructname, "", tostructname, relation);
				//if(entity!=null){
				//	this.entitylocator = entity;
				//	FormalRelation rel = new FormalRelation();
				//	rel.setString("part of");
				//	rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
				//	rel.setId("BFO:000050");
				//	rel.setConfidenceScore((float)1.0);
				//	REntity rentity = new REntity(rel, entity);
				//	//update this.entity with the composite entity
				//	CompositeEntity centity = new CompositeEntity();
				//	centity.addEntity(this.entity);
				//	centity.addEntity(rentity);
				//	this.entity = centity;					
				//}
			} else*/ if (relation.matches("\\bwith\\b.*")) {
				//check to-structure, if to-structure has no character, then generate EQ to_entity:present
				if(!Utilities.hasCharacters(tostructid, root) && !fromcharacterstatement){
					Hashtable<String, String> EQ = new Hashtable<String, String>();
					Utilities.initEQHash(EQ);
					EntityProposals entityproposals = new EntitySearcherOriginal().searchEntity(root, tostructid, tostructname, "", tostructname, relation);
					EQStatementProposals eqproposals = new EQStatementProposals();
					if(entityproposals!=null){
						ArrayList<Entity> entities = entityproposals.getProposals();
						for(Entity entity: entities){
							//construct quality
							Quality present = new Quality();
							present.setString("present");
							present.setLabel("PATO:present");
							present.setId("PATO:0000467");
							present.setConfidenceScore((float)1.0);
							EQStatement eq = new EQStatement();
							Element statement = relelement.getParentElement();
							eq.setCharacterId(statement.getAttributeValue("character_id"));
							eq.setDescription(statement.getChildText("text"));
							eq.setSource(root.getAttributeValue(ApplicationUtilities
									.getProperty("source.attribute.name")));
							eq.setStateId(statement.getAttribute("state_id") == null ? ""
									: statement.getAttributeValue("state_id"));
							eq.setType(this.fromcharacterstatement? "character" : "state");
							eq.setEntity(entity);
							eq.setQuality(present);
							eqproposals.add(eq);
						}
						this.otherEQs.add(eqproposals);
					}
				}
			} else if (relation.matches("without.*")) {
				// output absent as Q for tostructid
				if (!fromcharacterstatement) {
					EntityProposals entityproposals = new EntitySearcherOriginal().searchEntity(root, tostructid, tostructname, "", tostructname, relation);
					EQStatementProposals eqproposals = new EQStatementProposals();
					if(entityproposals!=null){
						ArrayList<Entity> entities = entityproposals.getProposals();
						for(Entity entity: entities){	
							//construct quality
							Quality absent = new Quality();
							absent.setString("absent");
							absent.setLabel("PATO:absent");
							absent.setId("PATO:0000462");
							absent.setConfidenceScore((float)1.0);
							EQStatement eq = new EQStatement();
							eq.setEntity(entity);
							eq.setQuality(absent);
							Element statement = relelement.getParentElement();
							eq.setCharacterId(statement.getAttributeValue("character_id"));
							eq.setDescription(statement.getChildText("text"));
							eq.setSource(root.getAttributeValue(ApplicationUtilities
									.getProperty("source.attribute.name")));
							eq.setStateId(statement.getAttribute("state_id") == null ? ""
									: statement.getAttributeValue("state_id"));
							eq.setType(this.fromcharacterstatement? "character" : "state");
							eqproposals.add(eq);
						}
						this.otherEQs.add(eqproposals);
					}
				}
			} else if(relation.matches("\\b(between|among|amongst)\\b.*")){
				//TODO between is a preposition too.
			} else {//qualitymodifier to which quality??? could indicate an error, but output anyway
				/*Hashtable<String, String> result = EntitySearcher.searchEntity(root, tostructid, tostructname, "", tostructname, relation, 0);
				results.put("qualitymodifier", results.get("qualitymodifier")+","+tostructname);
				if(result!=null){
					String entityid =result.get("entityid");
					if(entityid!=null){
						if(results.get("qualitymodifierid")==null){
							results.put("qualitymodifierid", entityid);
							results.put("qualitymodifierlabel", result.get("entitylabel"));
						}else{
							results.put("qualitymodifierid", results.get("qualitymodifierid")+","+entityid); // connected by ',' because all entitylocators are related to the same entity: the from structure 
							results.put("qualitymodifierlabel", results.get("qualitymodifierlabel")+","+result.get("entitylabel")); // connected by ',' because all entitylocators are related to the same entity: the from structure 
						}
					}
				}*/
			}					
		}
	}

	public EntityProposals getEntity(){
		return this.entity;
	}
	
	public QualityProposals getQuality(){
		return this.quality;
	}

	public ArrayList<EQStatementProposals> otherEQs(){
		return this.otherEQs;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//RelationHandler rh = new RelationHandler(new outputter.Dictionary(new ArrayList<String>()), new EntitySearcher(new outputter.Dictionary(new ArrayList<String>())));
		

	}

}
