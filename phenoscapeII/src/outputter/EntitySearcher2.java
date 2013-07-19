/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * @author Hong Cui
 * This class post-composes spatial parts and their parent organs
 * e.g .distal end of fibula
 *
 */
public class EntitySearcher2 extends EntitySearcher {
	private static final Logger LOGGER = Logger.getLogger(EntitySearcher2.class);   
	/**
	 * 
	 */
	public EntitySearcher2() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<EntityProposals> searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		LOGGER.debug("EntitySearcher2: search '"+entityphrase+"[orig="+originalentityphrase+"]'");
		//anterior margin of maxilla => anterior margin^part_of(maxilla)): entity = anterior margin, locator = maxilla
		
		//search entity and entity locator separately
		
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
		SimpleEntity sentity = (SimpleEntity)new TermSearcher().searchTerm(entityphrase, "entity");
		if(sentity!=null){//if entity matches
			//entity
			if(entityl.getString().length()>0){
				//relation & entity locator
				FormalRelation rel = Dictionary.partof;
				rel.setConfidenceScore((float)1.0);
				REntity rentity = new REntity(rel, entityl);
				//composite entity
				CompositeEntity centity = new CompositeEntity();
				centity.addEntity(sentity);
				centity.addEntity(rentity);
				EntityProposals ep = new EntityProposals();
				ep.setPhrase(sentity.getString());
				ep.add(centity);
				ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
				//entities.add(ep);
				Utilities.addEntityProposals(entities, ep);
				return entities;
			}else{
				EntityProposals ep = new EntityProposals();
				ep.setPhrase(sentity.getString());
				ep.add(sentity);
				ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
				//entities.add(ep);
				Utilities.addEntityProposals(entities, ep);
				return entities;
			}
		}
		//entity not match, keep searching
		LOGGER.debug("EntitySearcher2 calls EntitySearcher3");
		return new EntitySearcher3().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
	}

}
