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
 * Handles the characters of a structure
 * grab character and constrain info from <character> tag
 * return a hashtable:
 * key: "quality" value: qualityID
 * key: "qualitymodifier" value: qualitymodifierID
 * 
 *  
 * TODO could character be a relationalquality?
 * yes, for example, "fused"
 */
public class CharacterHandler {
	private TermSearcher ts = null;
	private EntitySearcher es = null;
	private conceptmapping.TermOutputerUtilities ontoutil;
	/**
	 * 
	 */
	public CharacterHandler(TermSearcher ts, EntitySearcher es, conceptmapping.TermOutputerUtilities ontoutil2) {
		this.ts = ts;
		this.es = es;
		this.ontoutil = ontoutil2;
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
			boolean negated = false;
			if(quality.startsWith("not ")){
				negated = true;
				quality = quality.substring(quality.indexOf(" ")+1).trim(); //TODO: deal with negated quality here
			}
			Hashtable<String, String> result = ts.searchTerm(quality, "quality", 0);
			if(result!=null){
				if(negated){
					results.put("quality", "not "+quality);
					results.put("qualityid","not("+result.get("id")+")");
					results.put("qualitylabel", "not("+result.get("label")+")");
					results.put("qualitynegated", "not "+quality);
					String qualitylabel = result.get("label");
					if(qualitylabel!=null){
						results.put("qualitynegatedlabel", "not("+qualitylabel+")");
						String [] parentinfo = ontoutil.retreiveParentInfoFromPATO(qualitylabel);
						if(parentinfo != null){
							results.put("qnparentid", parentinfo[0]);
							results.put("qnparentlabel", parentinfo[1]);
						}else{
							System.err.println("should not landed here");
						}
					}
				}else{
					results.put("quality", quality);
					results.put("qualityid", result.get("id"));
					results.put("qualitylabel", result.get("label"));
				}
				// constraints = qualitymodifier
				String qms = ""; 
				String qmIDs = ""; 
				String qmlabels = ""; 
				if (chara.getAttribute("constraintid") != null) {
					String conid = chara.getAttributeValue("constraintid");
					try{
						String qualitymodifier = Utilities.getStructureName(root, conid);
						qms += qualitymodifier+",";
						Hashtable<String, String> r = es.searchEntity(root, conid, qualitymodifier, "", qualitymodifier,"", 0);
						if(r!=null){
							qmIDs += r.get("entityid")==null? "" : r.get("entityid")+",";
							qmlabels += r.get("entitylabel")==null? "" : r.get("entitylabel")+",";
						}

						qualitymodifier = Utilities.getStructureChain(root, "//relation[@from='" + chara.getAttributeValue("constraintid") + "']");
						qms += qualitymodifier+",";
						r = es.searchEntity(root, conid, qualitymodifier, "", qualitymodifier, "", 0);
						if(r!=null){
							qmIDs += r.get("entityid")==null? "" : r.get("entityid")+",";
							qmlabels += r.get("entitylabel")==null? "" : r.get("entitylabel")+",";
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				qms = qms.replaceAll("(^,+|,+$)", "").trim();
				if(qms.length()>0)
					results.put("qualitymodifier", qms);
				qmIDs = qms.replaceAll("(^,+|,+$)", "").trim();
				if(qmIDs.length()>0)
					results.put("qualitymodifierid", qmIDs);
				qmlabels = qmlabels.replaceAll("(^,+|,+$)", "").trim();
				if(qmlabels.length()>0)
					results.put("qualitymodifierlabel", qmlabels);
			}else{
				//TODO
			}
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
