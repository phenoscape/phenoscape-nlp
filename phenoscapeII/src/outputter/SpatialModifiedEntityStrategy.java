/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * @author Hong Cui
 * = EntitySearcher4
 * anterior maxilla => anterior region part of maxilla
 * anterior process of maxilla => process part of anterior region part of maxilla
 * 
 *
 */
public class SpatialModifiedEntityStrategy implements AnnotationStrategy {
	private static final Logger LOGGER = Logger.getLogger(SpatialModifiedEntityStrategy.class);   
	ArrayList<EntityProposals> entities;
	private String entityphrase;
	private String elocatorphrase;
	private Element root;
	private String structid;
	private String prep;
	private String originalentityphrase;

	/**
	 * [the expression is a query expanded with syn rings, 
	 * for example, '(?:anterior|front) (?:maxilla|maxillary)'] --not yet
	 */
	public SpatialModifiedEntityStrategy(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		this.entityphrase = entityphrase;
		this.elocatorphrase = elocatorphrase;
		this.root = root;
		this.structid = structid;
		this.prep = prep;
		this.originalentityphrase = originalentityphrase;
		LOGGER.debug("SpatialModifiedEntityStrategy: search "+entityphrase+"[orig="+originalentityphrase+"]");
	}

	/* (non-Javadoc)
	 * @see outputter.AnnotationStrategy#handle()
	 */
	@Override
	public void handle() {
		String[] entityphrasetokens = entityphrase.split("\\s+");
		String[] entitylocators = null;
		if(elocatorphrase.length()>0) entitylocators = elocatorphrase.split("\\s*,\\s*");

		SimpleEntity entityl = new SimpleEntity();
		entityl.setString(elocatorphrase);
		if(entitylocators!=null) {
			SimpleEntity result = (SimpleEntity)new TermSearcher().searchTerm(elocatorphrase, "entity");
			if(result!=null){
				entityl = result;
				LOGGER.debug("search for locator ["+elocatorphrase+"] found a match: "+result.toString());
			}else{
				LOGGER.debug("search for locator ["+elocatorphrase+"] found no match");
			}
		}

		if(entityphrasetokens[0].matches("("+Dictionary.spatialtermptn+")")){
			String newentity = Utilities.join(entityphrasetokens, 1, entityphrasetokens.length-1, " "); //process
			//SimpleEntity sentity = (SimpleEntity)new TermSearcher().searchTerm(newentity, "entity");
			if(newentity.length()<=0) return;
			ArrayList<EntityProposals> sentityps = new EntitySearcherOriginal().searchEntity(root, structid,  newentity, elocatorphrase, originalentityphrase, prep); //advanced search
			if(sentityps!=null){
				LOGGER.debug("search for entity ["+newentity+"] found match");
				entities = new ArrayList<EntityProposals>();
				SimpleEntity sentity1 = (SimpleEntity)new TermSearcher().searchTerm(entityphrasetokens[0]+" region", "entity");//anterior + region: simple search
				for(EntityProposals sentityp: sentityps){
					for(Entity sentity: sentityp.getProposals()){
						if(sentity1!=null){
							LOGGER.debug("search for spatial term  ["+entityphrasetokens[0]+" region"+"] found a match :"+sentity1.toString());
							//nested part_of relation
							if(entityl.getString().length()>0){ //anterior process of maxilla 
								//relation & entity locator: inner
								FormalRelation rel = new FormalRelation();
								rel.setString("part of");
								rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
								rel.setId("BFO:000050");
								rel.setConfidenceScore((float)1.0);
								REntity rentity = new REntity(rel, entityl);
								//composite entity = entity locator for sentity
								CompositeEntity centity = new CompositeEntity(); //anterior region^part_of(maxilla)
								centity.addEntity(sentity1); //anterior region
								centity.addEntity(rentity);	//^part_of(maxilla)	
								//relation & entity locator:outer 
								rel = new FormalRelation();
								rel.setString("part of");
								rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
								rel.setId("BFO:000050");
								rel.setConfidenceScore((float)1.0);
								rentity = new REntity(rel, centity);
								centity = new CompositeEntity(); //process^part_of(anterior region^part_of(maxilla))
								centity.addEntity(sentity); //process
								centity.addEntity(rentity);	//^part_of(anterior region^part_of(maxilla))
								centity.setString(this.originalentityphrase);
								EntityProposals centityp = new EntityProposals();
								//centityp.setPhrase(sentity.getString());//use the primary entity's phrase
								centityp.setPhrase(this.originalentityphrase);
								centityp.add(centity);
								//entities.add(centityp);
								Utilities.addEntityProposals(entities, centityp);
								LOGGER.debug("with entity locator, form a composite entity proposals: "+centityp.toString());
								//return entities;
								return;
							}else{//anterior maxilla 
								//corrected 6/1/13 [basal scutes]: sentity1 be the entity; sentity is the entity locator
								//relation & entity locator: 
								FormalRelation rel = new FormalRelation();
								rel.setString("part of");
								rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
								rel.setId("BFO:000050");
								rel.setConfidenceScore((float)1.0);
								REntity rentity = new REntity(rel, sentity);
								//composite entity = entity locator for sentity
								CompositeEntity centity = new CompositeEntity(); 
								centity.addEntity(sentity1); 
								centity.addEntity(rentity);	
								centity.setString(this.originalentityphrase);
								EntityProposals centityp = new EntityProposals();
								//centityp.setPhrase(sentity1.getString());
								centityp.setPhrase(this.originalentityphrase);
								centityp.add(centity);
								//entities.add(centityp);
								Utilities.addEntityProposals(entities, centityp);
								LOGGER.debug("without entity locator, form a composite entity proposals: "+centityp.toString());
								//return entities;
								return;
							}	
						}else{
							LOGGER.debug("search for spatial term  ["+entityphrasetokens[0]+" region"+"] found no match");
						}
					}
				}
			}
		}


	}

	public ArrayList<EntityProposals> getEntities() {
		return this.entities;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}




}
