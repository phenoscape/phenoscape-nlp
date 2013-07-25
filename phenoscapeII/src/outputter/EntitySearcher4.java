/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * @author Hong Cui
 * the strategy for handling cases such as 'otic canal' which matches 'otic sensoary canal'.
 * turn 'otic canal' to 'otic .* canal'
 *
 */
public class EntitySearcher4 extends EntitySearcher {
	private static final Logger LOGGER = Logger.getLogger(EntitySearcher4.class);  
	/**
	 * 
	 */
	public EntitySearcher4() {
	}



	@Override
	public ArrayList<EntityProposals> searchEntity(Element root,
			String structid, String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {

		LOGGER.debug("EntitySearcher4: search '"+entityphrase+"[orig="+originalentityphrase+"]'");
		String entityphrasecopy = entityphrase;
		//still not find a match, if entityphrase is at least two words long, add wildcard * in spaces

		//search for locator first
		String[] entitylocators = null;
		if(elocatorphrase.length()>0) entitylocators = elocatorphrase.split("\\s*,\\s*");
		ArrayList<SimpleEntity> entityls = new ArrayList<SimpleEntity>();

		if(entitylocators!=null) {
			//TODO: is elocator a reg exp?
			ArrayList<FormalConcept> result = new TermSearcher().searchTerm(elocatorphrase, "entity"); //TODO: should it call EntitySearcherOriginal? decided not to.
			if(result!=null){
				LOGGER.debug("search for locator '"+elocatorphrase+"' found match: ");
				for(FormalConcept fc: result){
					entityls.add((SimpleEntity)fc);
					LOGGER.debug(".."+fc.toString());
				}
			}else{ //entity locator not matched
				LOGGER.debug("search for locator '"+elocatorphrase+"' found no match");
			}
		}

		
		//search entityphrase using wildcard
		//String myentityphrase = entityphrase.replaceFirst("^\\(\\?:", "").replaceFirst("\\)$", "").trim();				
		if(entityphrase.contains(" ")) entityphrase = entityphrase.replaceAll("\\s+", " .*? ");
		//ArrayList<FormalConcept> sentities = TermSearcher.regexpSearchTerm(entityphrase, "entity"); //candidate matches for the same entity
		ArrayList<FormalConcept> sentities = new TermSearcher().searchTerm(entityphrase, "entity"); //candidate matches for the same entity

		if(sentities!=null){
			LOGGER.debug("search for entity '"+entityphrase+"' found match, forming proposals...");
			boolean found = false;
			EntityProposals ep = new EntityProposals();
			for(FormalConcept sentityfc: sentities){				
				SimpleEntity sentity = (SimpleEntity)sentityfc;
				if(sentity!=null){//if entity matches
					if(elocatorphrase.length()>0){
						for(FormalConcept fc: entityls){
							SimpleEntity entityl = (SimpleEntity)fc;
							//relation & entity locator
							CompositeEntity centity = new CompositeEntity();
							centity.addEntity(sentity);								
							centity.addParentEntity(new REntity(Dictionary.partof, entityl));
							centity.setString(originalentityphrase);
							ep.add(centity); //add the other	
							LOGGER.debug(".."+centity.toString());
							found = true;
						}
					}else{
						ep.add(sentity); //add the other
						LOGGER.debug(".."+sentity.toString());
						found = true;
					}
				}
			}
			if(found==true){
				ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
				Utilities.addEntityProposals(entities, ep);
				LOGGER.debug("EntitySearcher4 returns:");
				for(EntityProposals aep: entities){
					LOGGER.debug("..EntityProposals: "+aep.toString());
				}
				return entities;
			}
		}else{
			LOGGER.debug("search for entity '"+entityphrase+"' found no match");
		}

		return  new EntitySearcher5().searchEntity(root, structid, entityphrasecopy, elocatorphrase, originalentityphrase, prep);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
