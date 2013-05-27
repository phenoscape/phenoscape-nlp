/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.jdom.Element;

/**
 * @author updates
 * This strategy aims to match a pre-composed term in an ontology.
 * For examples: 
 * 
 * input: e:posterior dorsal fin
 * direct match in 
 * 
 * 
 * input: e:postaxial process, el:modifier fibula
 * generate variations:
 * 1. (postaxial|syn_ring) (process|crest|syn_ring) of modifier (fibula|fibular|adj)
 * 2. modifier (fibula|fibular|adj) (postaxial|syn_ring) (process|crest|syn_ring) 
 * 3. (postaxial|syn_ring) modifier (fibula|fibular|adj) (process|crest|syn_ring)
 * 4. modifier (postaxial|syn_ring) (fibula|fibular|adj) (process|crest|syn_ring)
 * 
 */
public class EntitySearcher1 extends EntitySearcher {

	/**
	 * 
	 */
	public EntitySearcher1() {
		
	}

	/* (non-Javadoc)
	 * @see outputter.EntitySearcher#searchEntity(org.jdom.Element, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public EntityProposals searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		
		//no entity locator: direct match, for examples: "bone of humerus", "posterior dorsal fin"
		if(elocatorphrase==null || elocatorphrase.length()==0){
			SimpleEntity entity = (SimpleEntity)new TermSearcher().searchTerm(entityphrase, "entity");
			if(entity!=null){	
				EntityProposals entities = new EntityProposals();
				entities.setPhrase(entityphrase);
				entities.add(entity);
				return entities;
			}
		}
		if(entityphrase.split("\\s").length>=2)
		{
		//try out the variations
		SynRingVariation entityvariation = new SynRingVariation(entityphrase);
		SynRingVariation elocatorvariation = null;
		if(elocatorphrase==null || elocatorphrase.length()==0){
			//elocatorvariation =  new SynRingVariation(elocatorphrase);
		}
		
		if(elocatorvariation == null){ //try entityvariation alone
			String spatial = entityvariation.getLeadSpaticalTermVariation();
			String head = entityvariation.getHeadNounVariation();
			spatial="||"+spatial;
			head+="||"+head;
			
			// the below code passes all the spatial and entity variations to termsearcher and get all the matching entities.
			ArrayList<FormalConcept> matches = TermSearcher.entityvariationtermsearch(spatial,head);
			if(matches.size()>0)
			{
				EntityProposals entities = new EntityProposals();
				for(int i =0; i <matches.size(); i++){
					entities.add((Entity)matches.get(i));					
				}
				return entities;
			}
		}else{
		System.out.println("");
			if(prep.contains("part_of")){
				System.out.println();
			}
		}
		}
		return new EntitySearcher2().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
	}

	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
