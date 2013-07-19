/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author Hong Cui
 *
 */
public class EntitySearcher6 extends EntitySearcher {
	private static final Logger LOGGER = Logger.getLogger(EntitySearcher6.class);   
	boolean debug = true;
	/**
	 * 
	 */
	public EntitySearcher6() {
	}

	/* (non-Javadoc)
	 * @see outputter.EntitySearcher#searchEntity(org.jdom.Element, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)
	 */
	/**
	 * test cases:patterns.xml_s413c5e3e-7941-44e3-8be6-b17e6193752e.xml (manual)
	 */
	@Override
	public ArrayList<EntityProposals> searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		LOGGER.debug("EntitySearcher6: search '"+entityphrase+"[orig="+originalentityphrase+"]'");
		//still not find a match, remove the last term in the entityphrase, when what is then left is not just a spatial term 
		//"humeral deltopectoral crest apex" => "humeral deltopectoral crest"	
		//TODO "some part" of humerus; "some quality"
		//the last token could be a number (index)
		//Changed by Zilong:
		//enhanced entity format condition to exclude the spatial terms: in order to solve the problem that 
		//"rostral tubule" will match "anterior side" because rostral is synonymous with anterior

		String[] entitylocators = null;
		if(elocatorphrase.length()>0) entitylocators = elocatorphrase.split("\\s*,\\s*");
		
		SimpleEntity entityl = new SimpleEntity();
		entityl.setString(elocatorphrase);
		if(entitylocators!=null) {
			SimpleEntity result = (SimpleEntity) new TermSearcher().searchTerm(elocatorphrase, "entity");
			if(result!=null){
				entityl = result;
				LOGGER.debug("search for locator '"+elocatorphrase+"' found a match: "+entityl.toString());
			}else{ //entity locator not matched
				LOGGER.debug("search for locator '"+elocatorphrase+"' found no match");
			}
		}
		
		entityphrase = entityphrase.replaceFirst("^\\(\\?:", "").replaceFirst("\\)$", "");				
		String[] tokens = entityphrase.split("\\s+"); //(?:(?:humerus|humeral) (?:shaft))
		if(tokens.length>=2){ //to prevent "rostral tubule" from entering the subsequent process 
			String shortened = entityphrase.substring(0, entityphrase.lastIndexOf(" ")).trim();		
			
			if(!shortened.matches(".*?\\b("+Dictionary.spatialtermptn+")$")){
				//SimpleEntity sentity = (SimpleEntity) new TermSearcher().searchTerm(shortened, "entity");
				//search shortened and other strings with the same starting words
				//ArrayList<FormalConcept> shortentities = TermSearcher.regexpSearchTerm(shortened, "entity");
				
				ArrayList<FormalConcept> sentities = TermSearcher.regexpSearchTerm(shortened+"\\b.*", "entity"); //candidate matches for the same entity
				if(sentities!=null){
					LOGGER.debug("search for entity '"+shortened+"\\b.*' found match, forming proposals...");
					//construct anatomicalentity
					SimpleEntity anatomicalentity = new SimpleEntity();
					anatomicalentity.setClassIRI("http://purl.obolibrary.org/obo/UBERON_0001062");
					anatomicalentity.setConfidenceScore(0.8f);
					anatomicalentity.setId("UBERON:0001062");
					anatomicalentity.setLabel("anatomical entity");
					anatomicalentity.setString(entityphrase.substring(entityphrase.lastIndexOf(" ")).trim());
					anatomicalentity.setXMLid(structid);
					//construct relation
					FormalRelation rel =  Dictionary.partof;
					rel.setConfidenceScore((float)1.0);
					
					EntityProposals ep = new EntityProposals(); 
					boolean found = false;
					for(FormalConcept sentityfc: sentities){
						//if sentity part_of entityl holds, then sentity's conf score = 1 and return the result
						SimpleEntity sentity = (SimpleEntity)sentityfc;
						if(sentity.isOntologized() && entityl!=null && entityl.isOntologized()){
							if(XML2EQ.elk.isSubclassOfWithPart(entityl.getClassIRI(), sentity.getClassIRI())){
								LOGGER.debug("'"+entityl.getLabel() +"' and '"+sentity.getLabel() + "' are related, increase confidence score");
								found = true;
								sentity.setConfidenceScore(1f);
								CompositeEntity centity = new CompositeEntity();
								centity.addEntity(anatomicalentity);//anatomical entity ...								
								centity.addParentEntity(new REntity(rel, sentity)); // part of sentity ...
								centity.addParentEntity(new REntity(rel, entityl)); //part of entityl
								//ep.setPhrase(sentity.getString());
								ep.setPhrase(originalentityphrase);
								ep.add(centity);//add one proposal with anatomical entity
								LOGGER.debug("add a proposal with anatomical entity:"+centity.toString());
								centity = new CompositeEntity();
								centity.addEntity(sentity);
								centity.addParentEntity(new REntity(rel, entityl)); //part of entityl
								centity.setString(originalentityphrase);
								ep.add(centity); //add the other proposal without anatomical entity	
								LOGGER.debug("add the other proposal without anatomical entity:"+centity.toString());		
							}
						}
					}
					if(found){
						ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
						//entities.add(ep);
						Utilities.addEntityProposals(entities, ep);
						LOGGER.debug("EntitySearcher6 returns:");
						for(EntityProposals aep: entities){
							LOGGER.debug("..EntityProposals: "+aep.toString());
						}
						return entities;
					}
					//else, record results that meet certain criteria
					LOGGER.debug("entity and entity locator are not related");
					ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
					for(FormalConcept sentityfc: sentities){				
						SimpleEntity sentity = (SimpleEntity)sentityfc;
						//consider only the matches that are one word longer and don't have of/and in the labels
						String label = sentity.getLabel();
						String added = label.replaceFirst(shortened, "").trim();
						if(label.indexOf(" of ")>=0 || label.indexOf(" and ")>=0 || added.indexOf(" ")>0) continue;
						
						if(sentity.getId().compareTo(Dictionary.mcorganism)==0){
							//too general "body scale", try to search for "scale"
							//TODO: multi-cellular organism is too general a syn for body. "body" could mean something more restricted depending on the context.
							//TODO: change labels to ids
						}
						if(sentity!=null){//if entity matches
							//entity
							if(entityl.getString().length()>0){
								//relation & entity locator
								CompositeEntity centity = new CompositeEntity();
								centity.addEntity(anatomicalentity);
								centity.addParentEntity(new REntity(rel, sentity));
								centity.addParentEntity(new REntity(rel, entityl));
								centity.setString(originalentityphrase);
								//ep.setPhrase(sentity.getString());
								ep.setPhrase(originalentityphrase);
								ep.add(centity); //add one
								LOGGER.debug("add a proposal with anatomical entity:"+centity.toString());
								centity = new CompositeEntity();
								centity.addEntity(sentity);								
								centity.addParentEntity(new REntity(rel, entityl));
								centity.setString(originalentityphrase);
								ep.add(centity); //add the other	
								LOGGER.debug("add a proposal without anatomical entity:"+centity.toString());
							}else{
								//EntityProposals entities = new EntityProposals();
								CompositeEntity centity = new CompositeEntity();
								centity.addEntity(anatomicalentity);
								centity.addParentEntity(new REntity(rel, sentity));
								centity.setString(originalentityphrase);
								//ep.setPhrase(sentity.getString());
								ep.setPhrase(originalentityphrase);
								ep.add(centity); //add one
								LOGGER.debug("add a proposal with anatomical entity:"+centity.toString());
								ep.add(sentity); //add the other
								LOGGER.debug("add a proposal without anatomical entity:"+sentity.toString());
							}
						}
					}
					//entities.add(ep);
					Utilities.addEntityProposals(entities, ep);
					LOGGER.debug("EntitySearcher6 returns:");
					for(EntityProposals aep: entities){
						LOGGER.debug("..EntityProposals: "+aep.toString());
					}
					return entities;
				}else{
					LOGGER.debug("search for entity '"+shortened+"\\b.*' found no match");
				}
			}			
		}
		//If not found in Ontology, then return the phrase as simpleentity string
		//TODO return "some anatomical entity" or other high level concepts. 
		//don't forget the entityl
		LOGGER.debug("no match in ontology is found, form string-based proposals...");
		EntityProposals ep = new EntityProposals();
		ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
		SimpleEntity sentity = new SimpleEntity();
		sentity.setString(entityphrase);
		sentity.confidenceScore=0f;
		if(entityl.getString().length()>0){
			//relation & entity locator
			FormalRelation rel =  Dictionary.partof;
			rel.setConfidenceScore((float)1.0);
			REntity rentity = new REntity(rel, entityl);
			//composite entity
			CompositeEntity centity = new CompositeEntity();
			centity.addEntity(sentity);
			centity.addEntity(rentity);
			//EntityProposals entities = new EntityProposals();
			//ep.setPhrase(sentity.getString());
			centity.setString(originalentityphrase);
			ep.setPhrase(originalentityphrase);
			ep.add(centity);
			LOGGER.debug("add a proposal:"+centity.toString());
			//entities.add(ep);
			Utilities.addEntityProposals(entities, ep);
		}else{
			//EntityProposals entities = new EntityProposals();
			//ep.setPhrase(sentity.getString());
			ep.setPhrase(originalentityphrase);
			ep.add(sentity);
			LOGGER.debug("add a proposal:"+sentity.toString());
			//entities.add(ep);
			Utilities.addEntityProposals(entities, ep);
		}
		
		LOGGER.debug("EntitySearcher6 completed search for '"+entityphrase+"[orig="+originalentityphrase+"]' and returns:");
		for(EntityProposals aep: entities){
			LOGGER.debug("..EntityProposals: "+aep.toString());
		}
		return entities;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
