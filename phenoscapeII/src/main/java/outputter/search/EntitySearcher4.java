/**
 * 
 */
package outputter.search;

import java.util.ArrayList;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.jdom.Element;

import outputter.Utilities;
import outputter.data.CompositeEntity;
import outputter.data.EntityProposals;
import outputter.data.FormalConcept;
import outputter.data.REntity;
import outputter.data.SimpleEntity;
import outputter.knowledge.Dictionary;

/**
 * @author Hong Cui
 * the strategy for handling cases such as 'otic canal' which matches 'otic sensoary canal'.
 * turn 'otic canal' to 'otic .* canal'
 *
 */
public class EntitySearcher4 extends EntitySearcher {
	private static final Logger LOGGER = Logger.getLogger(EntitySearcher4.class);  
	private static Hashtable<String, ArrayList<EntityProposals>> cache = new Hashtable<String, ArrayList<EntityProposals>>();
	private static ArrayList<String> nomatchcache = new ArrayList<String>();
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
		
		//search cache
		if(EntitySearcher4.nomatchcache.contains(entityphrase+"+"+elocatorphrase)) return null;
		if(EntitySearcher4.cache.get(entityphrase+"+"+elocatorphrase)!=null) return EntitySearcher4.cache.get(entityphrase+"+"+elocatorphrase);
		
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
		String aentityphrase = entityphrase;
		if(entityphrase.contains(" ")) aentityphrase = entityphrase.replaceAll("\\s+", " .*? ");
		//ArrayList<FormalConcept> sentities = TermSearcher.regexpSearchTerm(entityphrase, "entity"); //candidate matches for the same entity
		ArrayList<FormalConcept> sentities = new TermSearcher().searchTerm(aentityphrase, "entity"); //candidate matches for the same entity

		if(sentities!=null){
			LOGGER.debug("search for entity '"+aentityphrase+"' found match, forming proposals...");
			boolean found = false;
			EntityProposals ep = new EntityProposals();
			ep.setPhrase(originalentityphrase);
			for(FormalConcept sentityfc: sentities){				
				SimpleEntity sentity = (SimpleEntity)sentityfc;
				sentity.setConfidenceScore(1f/sentities.size());
				if(sentity!=null){//if entity matches
					if(elocatorphrase.length()>0){
						for(FormalConcept fc: entityls){
							SimpleEntity entityl = (SimpleEntity)fc;
							entityl.setConfidenceScore(1f/entityls.size());
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
						ep.add(sentity); //no locator
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
				//caching
				if(entities==null) EntitySearcher4.nomatchcache.add(entityphrase+"+"+elocatorphrase);
				else EntitySearcher4.cache.put(entityphrase+"+"+elocatorphrase, entities);
				
				return entities;
			}
		}else{
			LOGGER.debug("...search for entity '"+entityphrase+"' found no match");
			EntitySearcher4.nomatchcache.add(entityphrase+"+"+elocatorphrase);
		}
		LOGGER.debug("EntitySearcher4 calls EntitySearcher5");
		return  new EntitySearcher5().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
