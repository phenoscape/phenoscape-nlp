/**
 * 
 */
package outputter;

import org.jdom.Element;

/**
 * @author Hong Cui
 * = EntitySearcher2
 */
public class EntityEntityLocatorStrategy implements AnnotationStrategy {
	EntityProposals entities;
	private String elocatorphrase;
	private String entityphrase;
	/**
	 * 
	 */
	public EntityEntityLocatorStrategy(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		this.elocatorphrase = elocatorphrase;
		this.entityphrase = entityphrase;
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
				entities = new EntityProposals();
				entities.setPhrase(sentity.getString());
				entities.add(centity);
				//return entities;
				return;
			}else{
				//EntityProposals entities = new EntityProposals();
				entities = new EntityProposals();
				entities.setPhrase(sentity.getString());
				entities.add(sentity);
				//return entities;
				return;
			}
		}
	}

	public EntityProposals getEntities() {
		return entities;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}


}
