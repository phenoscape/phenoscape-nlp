/**
 * 
 */
package outputter;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author updates
 * Restricted relation list:
 * http://phenoscape.org/wiki/Guide_to_Character_Annotation#Relations_used_for_post-compositions
 */
public class RelationHandler {
	private Dictionary dict;
	private EntitySearcher es;

	/**
	 * 
	 */
	public RelationHandler(Dictionary dict, EntitySearcher es) {
		this.dict = dict;
		this.es =es;
	}
	
	/**
	 * identify entitylocator, qualitymodifier and/or relationalquality (the last is based on restricted relation list) from this relation
	 * the process may also generate EQ such as xyz:absent from "without" phrases
	 * 
	 * @param root of the description
	 * @param relationstrings, each with a format of "fromid relation_string toid"
	 * @param structurename the from_structure
	 * @return key: "qualitymodifier|entitylocator|entity|relationalquality|extraEQs" element:  qualitymodifier|entitylocator_ID|entity_ID|relationalquality_ID|EQ_hashtable_list"
	 */
	public Hashtable<String, Object> handle(Element root, String[] relationstrings, String structurename, String structid, boolean keyelement){
		Hashtable<String, Object> results = new Hashtable<String, Object> ();
		results.put("extraEQs", new ArrayList<Hashtable<String, String>>());
		//TODO call EntitySearcher to help with entitylocator identification?
		//TODO use character info to help with entity identification?
		//TODO negation
		String qualitymodifier ="";
		String entitylocator = "";
		boolean hascharacter = hasCharacters(structid, root);
		if (relationstrings != null) {
			for (String r : relationstrings) {
				String toid = r.replaceFirst(".*?\\)", "").trim();
				String toname = Utilities.getStructureName(root, toid);
				String relation = r.replace(toid, "").replaceAll("[()]", "").trim();
				String relationalqualityID = matchInRestrictedRelation(structurename, relation, toname, hascharacter);
				toname = toname + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
				toname = toname.replaceFirst(",$", "");
				if(relationalqualityID !=null){ //yes, the relation is a relational quality
					Hashtable<String, String> result = es.searchEntity(root, toid, toname, "", toname, relation, 0);
					if(results.get("relationalquality") == null){
						results.put("relationalquality", relationalqualityID);
						//toname is then a qualitymodifier, containing an organ and its optional parent organs
						results.put("qualitymodifier", result.get("entityid")+","+result.get("entitylocatorid")); //use , not ;. ; used to separate qualitymodifiers of different quality
					}else{
						results.put("relationalquality", results.get("relationalquality")+";"+relationalqualityID);
						//toname is then a qualitymodifier
						results.put("qualitymodifier", results.get("qualitymodifier")+";"+result.get("entityid")+","+result.get("entitylocatorid")); //use , not ;. ; used to separate qualitymodifiers of different quality
					}
				}else{//no, the relation should not be considered relational quality
					//entity locator?
					if (r.matches("\\((" + Dictionary.positionprep + ")\\).*")) { // entitylocator
						//if (r.contains("between"))
						//	entitylocator += "between " + toname + ",";
						//else
						//	entitylocator += toname + ",";
						String entityid = es.searchEntity(root, toid, toname, "", toname, relation,  0).get("entityid");
						if(results.get("entitylocator")==null){
							results.put("entitylocator", entityid);
						}else{
							results.put("entitylocator", results.get("entitylocator")+","+entityid); // connected by ',' because all entitylocators are related to the same entity: the from structure 
						}						
					} else if (r.matches("\\(with\\).*")) {
						//check to-structure, if to-structure has no character, then generate EQ to_entity:present
						if(!hasCharacters(toid, root) && !keyelement){
							Hashtable<String, String> EQ = new Hashtable<String, String>();
							Utilities.initEQHash(EQ);
							EQ.put("entity", es.searchEntity(root, toid, toname, "", toname, relation, 0).get("entityid"));
							EQ.put("quality", "present");
							EQ.put("type", keyelement ? "character" : "state");
							
							ArrayList<Hashtable<String, String>> extraEQs = (ArrayList<Hashtable<String, String>>) results.get("extraEQs");
							extraEQs.add(EQ);
						}
					} else if (r.matches("\\(without\\).*")) {
						// output absent as Q for toid
						if (!keyelement) {
							Hashtable<String, String> EQ = new Hashtable<String, String>();
							Utilities.initEQHash(EQ);
							EQ.put("entity", es.searchEntity(root, toid, toname, "", toname, relation, 0).get("entityid"));
							EQ.put("quality", "absent");
							EQ.put("type", keyelement ? "character" : "state");
							ArrayList<Hashtable<String, String>> extraEQs = (ArrayList<Hashtable<String, String>>) results.get("extraEQs");
							extraEQs.add(EQ);
						}
					} else {//qualitymodifier to which quality??? could indicate an error, but output anyway
						String entityid = es.searchEntity(root, toid, toname, "", toname, relation, 0).get("entityid");
						if(results.get("qualitymodifier")==null){
							results.put("qualitymodifier", entityid);
						}else{
							results.put("qualitymodifier", results.get("qualitymodifier")+","+entityid); // connected by ',' because all entitylocators are related to the same entity: the from structure 
						}
					}					
				}
			}
		}
		return results;
	}
	
	/**
	 * 
	 * @param structureid
	 * @return true if the structure has character elements, false if not.
	 */
	private boolean hasCharacters(String structureid, Element root) {
		try{
			XPath characters = XPath.newInstance(".//Structure[@id='"+structureid+"']/Character");
			List<Element> chars = characters.selectNodes(root);
			if(chars.size()>0) return true;
		}catch(Exception e){
			e.printStackTrace();
		}	
		return false;
	}

	/**
	 * match the relation to the restricted relation list
	 * @param fromstructure name
	 * @param relation string
	 * @param tostructure name, not the chain of names
	 * @param hascharacter: if false ,then the relation has to be a relationalqaulity as the fromstructure has no characters
	 * @return if match, return ontologyID, if not, return null
	 */
	private String matchInRestrictedRelation(String fromstructure, String relation, String tostructure, boolean hascharacter) {
		// TODO Auto-generated method stub
		
		/*
		 * Changed by Zilong: deal with relationship such as connect, contact, interconnect etc.
		 * Transform the result from CharaParser which is of the form:
		 * connection[E] between A[EL] and B[EL] <some text>[Q] -the quality could be misidentified
		 * to the form:
		 * A[E] is in connection with[Q] B[QM]
		 * 
		 * */
		//if(entity.toLowerCase().trim().matches("("+Dictionary.contact+")")){
		//	EQ.put("entity", entitylocator.split(",")[0]);//the first EL as E
		//	EQ.put("quality", "in contact with"); //"in contact with" can be found in ontos
		//	EQ.put("qualitymodifier", entitylocator.replaceFirst("[^,]*,?", "").trim());//the rest of EL is QM
		//	EQ.put("entitylocator", "");//empty the EL
		//}
		/*End handling the "contact" type relation*/
		return null;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
