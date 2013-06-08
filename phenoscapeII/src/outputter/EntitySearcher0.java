/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.jdom.Element;

/**
 * @author Hong Cui
 * search originals as precomposed entity
 */
public class EntitySearcher0 extends EntitySearcher {

	/**
	 * 
	 */
	public EntitySearcher0() {
	}
	
	public EntityProposals searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		//search the originals:
		//no entity locator: try direct match, for examples: "bone of humerus", "posterior dorsal fin"
		if(elocatorphrase==null || elocatorphrase.length()==0){
			SimpleEntity entity = (SimpleEntity)new TermSearcher().searchTerm(entityphrase, "entity");
			if(entity!=null){	
				EntityProposals entities = new EntityProposals();
				entities.setPhrase(entityphrase);
				entities.add(entity);
				return entities;
			}
		}
		//with entity locator:
		if(prep.contains("part_of")){//glenoid head of scapula, Entity phrase = glenoid head, Elocator = scapula
			if(elocatorphrase!="")
			{
				SimpleEntity entity = (SimpleEntity)new TermSearcher().searchTerm(entityphrase+" of "+elocatorphrase, "entity");
				if(entity!=null){	
					EntityProposals entities = new EntityProposals();
					entities.setPhrase(entityphrase);
					entities.add(entity);
					return entities;
				}
			}
		}
		
		
		return new EntitySearcher1().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
