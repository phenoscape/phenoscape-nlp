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
 * This strategy aims to match a precomposed term in an ontology.
 * For example: 
 * input: postaxial process, fibula
 * generate variations:
 * 1. (postaxial|syn_ring) (process|crest|syn_ring) of (fibula|fibular|adj)
 * 2. (fibula|fibular|adj) (postaxial|syn_ring) (process|crest|syn_ring) 
 * 3. (postaxial|syn_ring) (fibula|fibular|adj) (process|crest|syn_ring)
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
	public Entity searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		//case of bone of humerus: join entity and entity locator
		String[] entitylocators = null;
		if(elocatorphrase.length()>0) entitylocators = elocatorphrase.split("\\s*,\\s*");
		String[] entityphrasetokens = entityphrase.split("\\s+");
		
		//case of bone of humerus: join entity and entity locator
		if(prep.contains("part_of")){
			String phrase = entityphrase+" of "+elocatorphrase;
			boolean goodphrase = false;
			List<Element> texts = new ArrayList<Element>();
			try{
				texts = textpath.selectNodes(root);
			}catch(Exception e){
				e.printStackTrace();
			}
			for(Element text : texts){
				if(text.getTextNormalize().toLowerCase().contains(phrase)){
					goodphrase = true;
					break;
				}
			}
			if(goodphrase){//perfect match for a pre-composed term
				SimpleEntity entity = (SimpleEntity)new TermSearcher().searchTerm(phrase, "entity");
				if(entity!=null){	
					return entity;
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
