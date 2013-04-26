/**
 * 
 */
package outputter;

import org.jdom.Element;

/**
 * @author updates
 *
 */
public class EntitySearcher4 extends EntitySearcher {

	/**
	 * 
	 */
	public EntitySearcher4() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see outputter.EntitySearcher#searchEntity(org.jdom.Element, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public Entity searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep, int ingroup) {
		//anterior process of the maxilla => process^part_of(anterior region^part_of(maxilla)): entity = process, locator = anterior region, maxilla

		String[] entityphrasetokens = entityphrase.split("\\s+");
		String[] entitylocators = null;
		if(elocatorphrase.length()>0) entitylocators = elocatorphrase.split("\\s*,\\s*");
		
		SimpleEntity entityl = new SimpleEntity();
		entityl.setString(elocatorphrase);
		if(entitylocators!=null) {
			SimpleEntity result = (SimpleEntity)TermSearcher.searchTerm(elocatorphrase, "entity", ingroup);
			if(result!=null){
				entityl = result;
			}	
		}
		
		if(entityphrasetokens[0].matches("("+Dictionary.spatialtermptn+")")){
			String newentity = Utilities.join(entityphrasetokens, 1, entityphrasetokens.length-1, " "); //process
			SimpleEntity sentity = (SimpleEntity)TermSearcher.searchTerm(newentity, "entity", ingroup);
			if(sentity!=null){
				SimpleEntity sentity1 = (SimpleEntity)TermSearcher.searchTerm(entityphrasetokens[0]+" region", "entity", ingroup);//anterior + region
				if(sentity1!=null){
					//nested part_of relation
					if(entityl.getString().length()>0){ //maxilla
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
						return centity;
					}else{//sentity1 be the entity locator
						//relation & entity locator: 
						FormalRelation rel = new FormalRelation();
						rel.setString("part of");
						rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
						rel.setId("BFO:000050");
						rel.setConfidenceScore((float)1.0);
						REntity rentity = new REntity(rel, sentity1);
						//composite entity = entity locator for sentity
						CompositeEntity centity = new CompositeEntity(); 
						centity.addEntity(sentity); 
						centity.addEntity(rentity);	
						return centity;
					}	
				}				
			}else{
				//TODO
			}
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
