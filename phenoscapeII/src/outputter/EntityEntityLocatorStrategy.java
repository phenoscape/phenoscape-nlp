/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * @author Hong Cui
 * = EntitySearcher2
 */
public class EntityEntityLocatorStrategy implements AnnotationStrategy {
	ArrayList<EntityProposals> entities;
	private String elocatorphrase;
	private String entityphrase;
	private Element root;
	private String structid;
	private String prep;
	private String originalentityphrase;
	private static final Logger LOGGER = Logger.getLogger(EntityEntityLocatorStrategy.class);   
	/**
	 * 
	 */
	public EntityEntityLocatorStrategy(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		this.elocatorphrase = elocatorphrase;
		this.entityphrase = entityphrase;
		this.root = root;
		this.structid = structid;
		this.prep = prep;
		this.originalentityphrase = originalentityphrase;
		LOGGER.debug("EntityEntityLocatorStrategy: search '"+entityphrase+"[orig="+originalentityphrase+"]'");

	}

	/* (non-Javadoc)
	 * @see outputter.AnnotationStrategy#handle()
	 */
	@Override
	public void handle() {
		//anterior margin of maxilla => anterior margin^part_of(maxilla)): entity = anterior margin, locator = maxilla

		//search entity and entity locator separately

		String[] entitylocators = null;
		if(elocatorphrase.length()>0) entitylocators = elocatorphrase.split("\\s*,\\s*");

		//SimpleEntity entityl = new SimpleEntity();
		//entityl.setString(elocatorphrase);
		ArrayList<EntityProposals> entitylps = new ArrayList<EntityProposals>();
		//entitylp.setPhrase(elocatorphrase);
		if(entitylocators!=null) {
			//SimpleEntity result = (SimpleEntity) new TermSearcher().searchTerm(elocatorphrase, "entity");
			ArrayList<EntityProposals> result = new EntitySearcherOriginal().searchEntity(root, structid,  elocatorphrase, "", originalentityphrase, prep); //advanced search
			if(result!=null){
				entitylps = result;
				LOGGER.debug("EntityEntityLocatorStrategy: results from searching '"+elocatorphrase+"[orig="+originalentityphrase+"]':");
				for(EntityProposals ep: result){
					LOGGER.debug(ep.toString());
				}
			}else{ //entity locator not matched
				LOGGER.debug("EntityEntityLocatorStrategy: no results from searching '"+elocatorphrase+"[orig="+originalentityphrase+"]':");
				//TODO
			}
		}
		//SimpleEntity sentity = (SimpleEntity)new TermSearcher().searchTerm(entityphrase, "entity");
		ArrayList<EntityProposals> sentityps = new EntitySearcherOriginal().searchEntity(root, structid,  entityphrase, "", originalentityphrase, prep); //advanced search
		if(sentityps!=null){//if entity matches
			//entity
			entities = new ArrayList<EntityProposals>();
			for(EntityProposals entitylp: entitylps){
				if(entitylp.getPhrase().length()>0){
					LOGGER.debug("entity locator phrase is not empty, constructing composite entity...");
					for(Entity entityl: entitylp.getProposals()){
						//relation & entity locator
						FormalRelation rel = Dictionary.partof;
						rel.setConfidenceScore((float)1.0);
						REntity rentity = new REntity(rel, entityl);
						for(EntityProposals sentityp: sentityps){
							for(Entity sentity: sentityp.getProposals()){
								//composite entity
								CompositeEntity centity = new CompositeEntity();
								centity.addEntity(sentity);
								centity.addEntity(rentity);
								centity.setString(this.originalentityphrase);
								//EntityProposals entities = new EntityProposals();
								//entities.setPhrase(sentityp.getPhrase());
								EntityProposals centityp = new EntityProposals();
								//centityp.setPhrase(sentityp.getPhrase());
								centityp.setPhrase(this.originalentityphrase);
								centityp.add(centity);
								Utilities.addEntityProposals(entities, centityp);
								LOGGER.debug("..composite entity ="+centity.toString());
								LOGGER.debug("..composite entityproposals ="+centityp.toString());
							}
						}
					}
					//return entities;
					return;
				}else{
					LOGGER.debug("entity locator phrase is empty, saving simple entity as EntityProposals...");
					//EntityProposals entities = new EntityProposals();
					//entities = new EntityProposals();
					//entities.setPhrase(sentityp.getPhrase());
					//entities.add(sentityp.getProposals());
					for(EntityProposals ep: sentityps){
						ep.setPhrase(this.originalentityphrase+"["+ep.getPhrase()+"]");
						LOGGER.debug("..entityproposals ="+ep.toString());
					}
					entities = sentityps;
					
					//return entities;
					return;
				}
			}
		}else{
			LOGGER.debug("EntityEntityLocatorStrategy: no results from searching '"+entityphrase+"[orig="+originalentityphrase+"]':");
		}
	}

	public ArrayList<EntityProposals> getEntities() {
		return entities;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}


}
