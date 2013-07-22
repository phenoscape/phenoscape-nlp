/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * @author Hong Cui
 * search originals as precomposed entity
 */
public class EntitySearcher0 extends EntitySearcher {
	private static final Logger LOGGER = Logger.getLogger(EntitySearcher0.class);   

	/**
	 * 
	 */
	public EntitySearcher0() {
	}
	
	public ArrayList<EntityProposals> searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		LOGGER.debug("EntitySearcher0: search '"+entityphrase+"[orig="+originalentityphrase+"]'");
		//search the originals:
		//no entity locator: try direct match, for examples: "bone of humerus", "posterior dorsal fin"
		if(elocatorphrase==null || elocatorphrase.length()==0){
			LOGGER.debug("search entity '"+entityphrase+"'");
			ArrayList<FormalConcept> entity = new TermSearcher().searchTerm(entityphrase, "entity");
			if(entity!=null){	
				EntityProposals ep = new EntityProposals();
				//ep.setPhrase(entityphrase);
				ep.setPhrase(originalentityphrase);
				for(FormalConcept fc: entity){
					ep.add((SimpleEntity)fc);
				}
				//ep.add(entity);
				ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
				//entities.add(ep);
				Utilities.addEntityProposals(entities, ep);
				LOGGER.debug("EntitySearcher0 completed search for '"+entityphrase+"[orig="+originalentityphrase+"]' and returns");
				for(EntityProposals aep: entities){
					LOGGER.debug("..EntityProposals:"+aep.toString());
				}
				return entities;
			}
		}
		//with entity locator:
		if(prep.contains("part_of")){//glenoid head of scapula, Entity phrase = glenoid head, Elocator = scapula
			if(elocatorphrase.length()>0)
			{
				LOGGER.debug("search entity '"+entityphrase+" of "+elocatorphrase+"'");
				ArrayList<FormalConcept> entity = new TermSearcher().searchTerm(entityphrase+" of "+elocatorphrase, "entity");
				if(entity!=null){	
					EntityProposals ep = new EntityProposals();
					//ep.setPhrase(entityphrase);
					ep.setPhrase(originalentityphrase);
					for(FormalConcept fc: entity){
						ep.add((SimpleEntity)fc);
					}
					//ep.add(entity);
					ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
					//entities.add(ep);
					Utilities.addEntityProposals(entities, ep);
					LOGGER.debug("EntitySearcher0 returns");
					for(EntityProposals aep: entities){
						LOGGER.debug("..EntityProposals:"+aep.toString());
					}
					return entities;
				}
			}
		}
		
		LOGGER.debug("EntitySearcher0 calls EntitySearch1");
		return new EntitySearcher1().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
