/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author Hong Cui
 *
 */
public class EntitySearcher6 extends EntitySearcher {

	/**
	 * 
	 */
	public EntitySearcher6() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see outputter.EntitySearcher#searchEntity(org.jdom.Element, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public EntityProposals searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		//still not find a match, remove the last term in the entityphrase, when what is left is not just a spatial term 
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
			}else{ //entity locator not matched
				//TODO
			}
		}
		
		String[] tokens = entityphrase.split("\\s+");
		if(tokens.length>=2){ //to prevent "rostral tubule" from entering the subsequent process 
			String shortened = entityphrase.substring(0, entityphrase.lastIndexOf(" ")).trim();
			if(!shortened.matches(".*?\\b("+Dictionary.spatialtermptn+")$")){
				//SimpleEntity sentity = (SimpleEntity) new TermSearcher().searchTerm(shortened, "entity");
				ArrayList<FormalConcept> sentities = TermSearcher.regexpSearchTerm(shortened+"\\b.*", "entity");
				if(sentities!=null){
					EntityProposals entities = new EntityProposals();
					for(FormalConcept sentityfc: sentities){
						//if sentity part_of entityl holds, then sentity's conf score = 1 and return the result
						SimpleEntity sentity = (SimpleEntity)sentityfc;
						if(sentity.isOntologized() && entityl!=null && entityl.isOntologized()){
							if(XML2EQ.elk.isSubclassOfWithPart(entityl.getClassIRI(), sentity.getClassIRI())){
								sentity.setConfidenceScore(1f);
								FormalRelation rel = new FormalRelation();
								rel.setString("part of");
								rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
								rel.setId("BFO:000050");
								rel.setConfidenceScore((float)1.0);
								REntity rentity = new REntity(rel, entityl);
								//composite entity
								CompositeEntity centity = new CompositeEntity();
								centity.addEntity(sentity);
								centity.addEntity(rentity);
								entities.setPhrase(sentity.getString());
								entities.add(centity);
								return entities;
							}
						}
					}
					//else, record all results
					for(FormalConcept sentityfc: sentities){
						SimpleEntity sentity = (SimpleEntity)sentityfc;
						if(sentity.getId().compareTo(Dictionary.mcorganism)==0){
							//too general "body scale", try to search for "scale"
							//TODO: multi-cellular organism is too general a syn for body. "body" could mean something more restricted depending on the context.
							//TODO: change labels to ids
						}
						if(sentity!=null){//if entity matches
							//entity
							if(entityl.getString().length()>0){
								//relation & entity locator
								FormalRelation rel = new FormalRelation();
								rel.setString("part of");
								rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
								rel.setId("BFO:000050");
								rel.setConfidenceScore((float)1.0);
								REntity rentity = new REntity(rel, entityl);
								//composite entity
								CompositeEntity centity = new CompositeEntity();
								centity.addEntity(sentity);
								centity.addEntity(rentity);
								//EntityProposals entities = new EntityProposals();
								entities.setPhrase(sentity.getString());
								entities.add(centity);
							}else{
								//EntityProposals entities = new EntityProposals();
								entities.setPhrase(sentity.getString());
								entities.add(sentity);
							}
						}
					}
					return entities;
				}
			}			
		}
		//If not found in Ontology, then return the phrase as simpleentity string
		//TODO return "some anatomical entity" or other high level concepts. 
		SimpleEntity sentity = new SimpleEntity();
		sentity.setString(entityphrase);
		sentity.confidenceScore=(float) 1.0;
		EntityProposals entities = new EntityProposals();
		entities.setPhrase(sentity.getString());//set once for all proposals
		entities.add(sentity);
		return entities;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
