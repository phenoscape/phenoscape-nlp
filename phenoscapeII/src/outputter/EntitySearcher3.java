/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * @author updates
 *
 */
public class EntitySearcher3 extends EntitySearcher {
	private static final Logger LOGGER = Logger.getLogger(EntitySearcher3.class);   
	/**
	 * 
	 */
	public EntitySearcher3() {
		// TODO Auto-generated constructor stub
		
	}

	/* (non-Javadoc)
	 * @see outputter.EntitySearcher#searchEntity(org.jdom.Element, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public ArrayList<EntityProposals> searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		LOGGER.debug("EntitySearcher3: search '"+entityphrase+"[orig="+originalentityphrase+"]'");
		//re-arranging word in entity, first search for entity locator
		
		//"maxillary process" => process^part_of(maxilla) : entity = process, locator = maxilla
		//TODO: process of maxilla case
		//String[] entitylocators = null;
		//if(elocatorphrase.length()>0) entitylocators = elocatorphrase.split("\\s*,\\s*");
		String[] entityphrasetokens = entityphrase.split("\\s+");
		
		String adjIDlabel = TermSearcher.adjectiveOrganSearch(entityphrasetokens[0]);
		if(adjIDlabel!=null){
			SimpleEntity entityl = new SimpleEntity();
			entityl.setString(entityphrasetokens[0]);
			entityl.setLabel(adjIDlabel.substring(adjIDlabel.indexOf("#")+1));
			entityl.setId(adjIDlabel.substring(0, adjIDlabel.indexOf("#")));
			entityl.setConfidenceScore((float)1);
			String newentity = Utilities.join(entityphrasetokens, 1, entityphrasetokens.length-1, " ");
			SimpleEntity sentity = (SimpleEntity)new TermSearcher().searchTerm(newentity, "entity");
			if(sentity!=null){
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
				EntityProposals ep = new EntityProposals();
				ep.setPhrase(sentity.getString());
				ep.add(centity);
				ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
				//entities.add(ep);
				Utilities.addEntityProposals(entities, ep);
				return entities;	
			}		
		}
		LOGGER.debug("EntitySearcher3 calls EntitySearcher4");
		return new EntitySearcher4().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
