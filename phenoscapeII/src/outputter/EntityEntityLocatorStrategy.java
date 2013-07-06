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
	private Element root;
	private String structid;
	private String prep;
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
		EntityProposals entitylp = new EntityProposals();
		entitylp.setPhrase(elocatorphrase);
		if(entitylocators!=null) {
			//SimpleEntity result = (SimpleEntity) new TermSearcher().searchTerm(elocatorphrase, "entity");
			EntityProposals result = new EntitySearcherOriginal().searchEntity(root, structid,  elocatorphrase, "", elocatorphrase, prep); //advanced search
			if(result!=null){
				entitylp = result;
			}else{ //entity locator not matched
				//TODO
			}
		}
		//SimpleEntity sentity = (SimpleEntity)new TermSearcher().searchTerm(entityphrase, "entity");
		EntityProposals sentityp = new EntitySearcherOriginal().searchEntity(root, structid,  entityphrase, "", entityphrase, prep); //advanced search
		if(sentityp!=null){//if entity matches
			//entity
			//if(entityl.getString().length()>0){
			if(entitylp.getPhrase().length()>0){
				entities = new EntityProposals();
				for(Entity entityl: entitylp.getProposals()){
					//relation & entity locator
					FormalRelation rel = new FormalRelation();
					rel.setString("part of");
					rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
					rel.setId("BFO:000050");
					rel.setConfidenceScore((float)1.0);
					REntity rentity = new REntity(rel, entityl);
					for(Entity sentity: sentityp.getProposals()){
						//composite entity
						CompositeEntity centity = new CompositeEntity();
						centity.addEntity(sentity);
						centity.addEntity(rentity);
						//EntityProposals entities = new EntityProposals();
						entities.setPhrase(sentityp.getPhrase());
						entities.add(centity);
					}
				}
				//return entities;
				return;
			}else{
				//EntityProposals entities = new EntityProposals();
				//entities = new EntityProposals();
				//entities.setPhrase(sentityp.getPhrase());
				//entities.add(sentityp.getProposals());
				entities = sentityp;
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
