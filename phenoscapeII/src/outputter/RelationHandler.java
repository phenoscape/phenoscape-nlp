/**
 * 
 */
package outputter;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.jdom.Element;
import org.jdom.xpath.XPath;


/**
 * @author Hong Cui
 * Restricted relation list:
 * http://phenoscape.org/wiki/Guide_to_Character_Annotation#Relations_used_for_post-compositions
 */
public class RelationHandler {
	EntityProposals entity; //entity holds the result on entity, may be simple or composite (with a entity locator)
	ArrayList<QualityProposals> quality; //quality (simple quality or relational quality) or negated quality. If relational quality, must have qualitymodifier (i.e. related entity)
	EntityProposals entitylocator;
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
	
	private ToBeResolved tobesolvedentity;
	private boolean resolve = false;
	private ArrayList<EntityProposals> keyentities;
	EntityProposals spatialmodifier;
	HashSet<String> identifiedqualities;
	
	public RelationHandler(Element root, String relation, Element relelement, String tostructname, String tostructid, String structname, String structid, boolean negation, ArrayList<EntityProposals> keyentities, boolean keyelement){
		this.root = root;
		this.relation = relation;
		this.tostructname = tostructname;
		this.tostructid = tostructid;
		this.fromstructname = structname;
		this.fromstructid = structid;
		this.fromcharacterstatement = keyelement;
		this.otherEQs = new ArrayList<EQStatementProposals>();
		this.relelement = relelement;
		this.keyentities = keyentities;
		
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
		if(resolve) resolve(); //for future development
	}
		
		
		
	private void resolve() {
		// TODO implement it
		//if fromstructure name overlaps with name of keyentities, resolve to entity
		if(this.keyentities!=null){
			for(EntityProposals ep: this.keyentities){
				for(Entity e: ep.getProposals()){
					if(contains(e.getString(), fromstructname)){//
						this.entity = this.tobesolvedentity.getEntityCandidate();
						this.tobesolvedentity.setQualityCandidate(null);
						this.tobesolvedentity.setStructure2Quality(null);
						return;
						
					}
				}
			}
		}
		
		//if a relational quality is identified, resolve to entity
		if(this.quality!=null){
			for(QualityProposals qp: this.quality){
				for(Quality q: qp.getProposals()){
					if(q instanceof RelationalQuality){
						this.entity = this.tobesolvedentity.getEntityCandidate();
						this.tobesolvedentity.setQualityCandidate(null);
						this.tobesolvedentity.setStructure2Quality(null);
						return;
					}
				}
			}
		}
		
		
		//if resolve to s2q, pass along the orphaned spatialmodifier
		//this.spatialmodifier = ep.spaitialmodifier;
		//this.identifiedqualities = ep.identifiedqualities
		
	}

	/**
	 * 
	 * @param string1
	 * @param string2
	 * @return if string1 contains any token of string2
	 */
	private boolean contains(String string1, String string2) {
		String[] tokens = string2.split("\\s+");
		for(String token: tokens){
			if(string1.contains(token)) return true;
		}
		return false;
	}

	private void parseEntity(){
		if(fromstructname.replace("_"," ").compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))!=0){ //otherwise, this.entity remains null
			//parents separated by comma (,).
			//String t="";
			//String parents = Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + fromstructid + "']", 0);
			//this.entity = new EntitySearcherOriginal().searchEntity(root, fromstructid, fromstructname, parents, fromstructname,"part_of");	//corrected by Hong
			//shouldn't this calls EntityParser? and then resolve the conflicts, like characterhandler do?
			//could fromstruct be a quality?
			EntityParser ep = new EntityParser(relelement, root, fromstructid, fromstructname, fromcharacterstatement);
			if(ep.getEntity()!=null && ep.getQualityStrategy()==null){
				this.entity = ep.getEntity();
			}
			else if(ep.getQualityStrategy()!=null && ep.getEntity()==null){
				if(this.quality==null) this.quality = new ArrayList<QualityProposals>();
				this.quality.addAll(ep.getQualityStrategy().qualities);
				this.spatialmodifier = ep.spaitialmodifier;
				this.identifiedqualities = ep.identifiedqualities;
			}
			else{
				this.tobesolvedentity = new ToBeResolved(fromstructid);
				this.tobesolvedentity.setEntityCandidate(ep.getEntity());
				this.tobesolvedentity.setStructure2Quality(ep.getQualityStrategy());
				this.resolve  = true;			
			}	
			
		}		
	}

	private void parseRelation(){
		
		//TODO call EntitySearcher to help with entitylocator identification?
		//TODO use character info to help with entity identification?
		//TODO negation

		QualityProposals relationalquality = PermittedRelations.matchInPermittedRelation(relation, negation,1);
		tostructname = tostructname + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + tostructid + "']" +
																			 "|//relation[@name='in'][@from='" + tostructid + "']" +
																			 "|//relation[@name='on'][@from='" + tostructid + "']", 0); //in/on = part_of?
		tostructname = tostructname.replaceFirst(",$", "").trim();
		if(relationalquality !=null){ //yes, the relation is a relational quality
			String t = "";
			EntityProposals relatedentity = new EntitySearcherOriginal().searchEntity(root, tostructid, tostructname, "", tostructname, relation);
			RelationalQuality rq = new RelationalQuality(relationalquality, relatedentity);
			if(this.quality==null) this.quality = new ArrayList<QualityProposals>();
			QualityProposals aquality = new QualityProposals();
			aquality.add(rq);
			quality.add(aquality);					
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
				//check to-structure, if to-structure has no character, then generate EQs 1) to_entity:present 2) from_entity: has_part to_entity
				if(!Utilities.hasCharacters(tostructid, root) && !fromcharacterstatement){
					EntityProposals entityproposals = new EntitySearcherOriginal().searchEntity(root, tostructid, tostructname, "", tostructname, "");					
					if(entityproposals!=null){
						//1) to_entity:present 
							EQStatementProposals eqproposals = new EQStatementProposals();
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
						//2) from_entity: has_part to_entity
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
			}else if(relation.matches("\\b(between|among|amongst)\\b.*")){
				//TODO between is a preposition too.
			}else if(relation.matches("\\b(found|located)\\b.*"))// This handles pattern 4.1
				{
				
				Quality present = new Quality();
				present.setString("present");
				present.setLabel("PATO:present");
				present.setId("PATO:0000467");
				present.setConfidenceScore((float)1.0);
				
				if(this.quality==null) this.quality = new ArrayList<QualityProposals>();
				QualityProposals aquality = new QualityProposals();
				aquality.add(present);	
				quality.add(aquality);
				
				if(fromstructname.replace("_"," ").compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))==0)
				{
				this.entitylocator = new EntitySearcherOriginal().searchEntity(root, tostructid, tostructname,"",tostructname,"");
				}
				}
				else {//qualitymodifier to which quality??? could indicate an error, but output anyway
			
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

	/**
	 * see if structid consist_of x exists in root, if yes , return the id for x, if not, return null
	 * @param structid
	 * @param root
	 * @return
	 */
	private ArrayList<String> isCluster(String structid, Element root) {
		ArrayList<String> partsids = new ArrayList<String>();
		try{
			List<Element> relations = XPath.selectNodes(root, "//relation[@name='consist_of'][@from='"+structid+"']"); //row of tooth or denticles
			for(Element relation: relations){
				partsids.add(relation.getAttributeValue("to"));
			}
			return partsids;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	public EntityProposals getEntity(){
		return this.entity;
	}
	
	public ArrayList<QualityProposals> getQuality(){
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
