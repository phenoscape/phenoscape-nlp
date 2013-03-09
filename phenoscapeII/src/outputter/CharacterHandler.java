/**
 * 
 */
package outputter;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

/**
 * @author hong cui
 * grab character and constrain info from <character> tag
 * return a hashtable:
 * key: "quality" value: qualityID
 * key: "qualitymodifier" value: qualitymodifierID
 * 
 * TODO could character be a relationalquality?
 * 
 */
public class CharacterHandler {
	private TermSearcher ts = null;
	private EntitySearcher es = null;
	/**
	 * 
	 */
	public CharacterHandler(TermSearcher ts, EntitySearcher es) {
		this.ts = ts;
		this.es = es;
	}

	/**
	 * 
	 * @param root
	 * @param chars
	 * @return two-key hashtable: quality|qualitymodifier, elements are IDs separated by ';'
	 */
	public Hashtable<String, String> handle(Element root, List<Element> chars){
		Hashtable<String, String> results = new Hashtable<String, String> ();
		
		Iterator<Element> it = chars.iterator();
		while (it.hasNext()) { //loop through characters one by one
			Element chara = it.next();
			// characters = quality
			String quality = Utilities.formQualityValueFromCharacter(chara);
			Hashtable<String, String> result = ts.searchTerm(quality, "quality", 0);
			results.put("quality", result.get("id"));
			// constraints = qualitymodifier
			String qmIDs = ""; 
		
			if (chara.getAttribute("constraintid") != null) {
				String conid = chara.getAttributeValue("constraintid");
				try{
					String qualitymodifier = Utilities.getStructureName(root, conid);
					qmIDs += es.searchEntity(root, conid, qualitymodifier, "", qualitymodifier,"", 0).get("entity")+",";
					qualitymodifier = Utilities.getStructureChain(root, "//relation[@from='" + chara.getAttributeValue("constraintid") + "']");
					qmIDs += es.searchEntity(root, conid, qualitymodifier, "", qualitymodifier, "", 0).get("entity");
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			results.put("qualitymodifier", qmIDs);
		}	
		return results;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
