/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.jdom.Element;

/**
 * @author updates
 *
 */
public class EntitySearcher2 extends EntitySearcher {

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
	public EntityProposals searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
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
				EntityProposals entities = new EntityProposals();
				entities.add(centity);
				return entities;
			}else{
				EntityProposals entities = new EntityProposals();
				entities.add(sentity);
				return entities;
			}
		}
		return new EntitySearcher3().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
	}

}
