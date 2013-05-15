/**
 * 
 */
package outputter;

import java.util.Hashtable;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author updates
 *
 */
public class EntitySearcher6 extends EntitySearcher {

	/**
	 * 
	 */
	public EntitySearcher6() {
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see outputter.EntitySearcher#searchEntity(org.jdom.Element, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public Entity searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep, int ingroup) {
		//still not find a match, remove the last term in the entityphrase, when what is left is not just a spatial term 
		//"humeral deltopectoral crest apex" => "humeral deltopectoral crest"	
		//TODO "some part" of humerus; "some quality"
		//the last token could be a number (index)
		//Changed by Zilong:
		//enhanced entity format condition to exclude the spatial terms: in order to solve the problem that 
		//"rostral tubule" will match "anterior side" because rostral is synonymous with anterior
		
		
		String[] tokens = entityphrase.split("\\s+");
		if(tokens.length>=2){ //to prevent "rostral tubule" from entering the subsequent process 
			String shortened = entityphrase.substring(0, entityphrase.lastIndexOf(" ")).trim();
			if(!shortened.matches(".*?\\b("+Dictionary.spatialtermptn+")$")){
				SimpleEntity sentity = (SimpleEntity) TermSearcher.searchTerm(shortened, "entity", ingroup);
				if(sentity!=null){
					if(sentity.getId().compareTo(Dictionary.mcorganism)==0){
						//too general "body scale", try to search for "scale"
						//TODO: multi-cellular organism is too general a syn for body. "body" could mean something more restricted depending on the context.
						//TODO: change labels to ids
					}
					return sentity;
				}
			}			
		}
		//If not found in Ontology, then return the phrase as simpleentity string
		SimpleEntity sentity = new SimpleEntity();
		sentity.setString(entityphrase);
		sentity.confidenceScore=(float) 1.0;
		return sentity;
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
