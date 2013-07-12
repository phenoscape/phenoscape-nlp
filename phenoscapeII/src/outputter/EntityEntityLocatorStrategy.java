/**
 * 
 */
package outputter;

import java.util.ArrayList;

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
		ArrayList<EntityProposals> entitylps = new ArrayList<EntityProposals>();
		//entitylp.setPhrase(elocatorphrase);
		if(entitylocators!=null) {
			//SimpleEntity result = (SimpleEntity) new TermSearcher().searchTerm(elocatorphrase, "entity");
			ArrayList<EntityProposals> result = new EntitySearcherOriginal().searchEntity(root, structid,  elocatorphrase, "", elocatorphrase, prep); //advanced search
			if(result!=null){
				entitylps = result;
			}else{ //entity locator not matched
				//TODO
			}
		}
		//SimpleEntity sentity = (SimpleEntity)new TermSearcher().searchTerm(entityphrase, "entity");
		ArrayList<EntityProposals> sentityps = new EntitySearcherOriginal().searchEntity(root, structid,  entityphrase, "", entityphrase, prep); //advanced search
		if(sentityps!=null){//if entity matches
			//entity
			entities = new ArrayList<EntityProposals>();
			for(EntityProposals entitylp: entitylps){
				if(entitylp.getPhrase().length()>0){
					for(Entity entityl: entitylp.getProposals()){
						//relation & entity locator
						FormalRelation rel = new FormalRelation();
						rel.setString("part of");
						rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
						rel.setId("BFO:000050");
						rel.setConfidenceScore((float)1.0);
						REntity rentity = new REntity(rel, entityl);
						for(EntityProposals sentityp: sentityps){
						for(Entity sentity: sentityp.getProposals()){
							//composite entity
							CompositeEntity centity = new CompositeEntity();
							centity.addEntity(sentity);
							centity.addEntity(rentity);
							//EntityProposals entities = new EntityProposals();
							//entities.setPhrase(sentityp.getPhrase());
							EntityProposals centityp = new EntityProposals();
							centityp.setPhrase(sentityp.getPhrase());
							centityp.add(centity);
							entities.add(centityp);
						}
						}
					}
					//return entities;
					return;
				}else{
					//EntityProposals entities = new EntityProposals();
					//entities = new EntityProposals();
					//entities.setPhrase(sentityp.getPhrase());
					//entities.add(sentityp.getProposals());
					entities = sentityps;
					//return entities;
					return;
				}
			}
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
