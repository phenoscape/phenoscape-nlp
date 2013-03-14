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
	 * @return key: "qualitymodifier|entitylocator|relationalquality|qualitymodifierid|entitylocatorid|relationalqualityid|qualitymodifierlabel|entitylocatorlabel|relationalqualitylabel|extraEQs(list of hashes)" "
	 */
	public Hashtable<String, Object> handle(Element root, String[] relationstrings, String structurename, String structid, boolean keyelement){
		Hashtable<String, Object> results = new Hashtable<String, Object> ();
		results.put("extraEQs", new ArrayList<Hashtable<String, String>>());
		//TODO call EntitySearcher to help with entitylocator identification?
		//TODO use character info to help with entity identification?
		//TODO negation
		String temp="";
		boolean hascharacter = hasCharacters(structid, root);
		if (relationstrings != null) {
			for (String r : relationstrings) {
				String toid = r.replaceFirst(".*?\\)", "").trim();
				String toname = Utilities.getStructureName(root, toid);
				String relation = r.replace(toid, "").replaceAll("[()]", "").trim();
				Hashtable<String, String> relationalqualityID = matchInRestrictedRelation(structurename, relation, toname, hascharacter);
				toname = toname + "," + Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + toid + "']");
				toname = toname.replaceFirst(",$", "");
				if(relationalqualityID !=null){ //yes, the relation is a relational quality
					Hashtable<String, String> result = es.searchEntity(root, toid, toname, "", toname, relation, 0);
					if(results.get("relationalqualityid") == null){
						results.put("relationalqualityid", relationalqualityID.get("id"));
						results.put("relationalquality", relation);
						results.put("relationalqualitylabel", relationalqualityID.get("label"));
						//toname is then a qualitymodifier, containing an organ and its optional parent organs
						results.put("qualitymodifier", toname);
						if(result!=null){
							String qm = result.get("entityid")==null? "": result.get("entityid")+","+
									result.get("entitylocatorid")==null? "":result.get("entitylocatorid");
							if(qm.replaceFirst("(^,+|,+$)", "").trim().length()>0){
								results.put("qualitymodifierid", qm); //use , not ;. ; used to separate qualitymodifiers of different quality
							}
							qm = result.get("entitylabel")==null? "": result.get("entitylabel")+","+
									result.get("entitylocatorlabel")==null? "":result.get("entitylocatorlabel");
							if(qm.replaceFirst("(^,+|,+$)", "").trim().length()>0){
								results.put("qualitymodifierlabel", qm); //use , not ;. ; used to separate qualitymodifiers of different quality
							}
						}
					}else{
						results.put("relationalquality", results.get("relationalquality")+";"+relation);
						results.put("relationalqualityid", results.get("relationalqualityid")+";"+relationalqualityID.get("id"));
						results.put("relationalqualitylabel", results.get("relationalqualitylabel")+";"+relationalqualityID.get("label"));
						//toname is then a qualitymodifier
						results.put("qualitymodifier", results.get("qualitymodifier")+";"+toname);
						String qm = result.get("entityid")==null? "": result.get("entityid")+","+
								result.get("entitylocatorid")==null? "":result.get("entitylocatorid");
						if(qm.replaceFirst("(^,+|,+$)", "").trim().length()>0){
							results.put("qualitymodifierid", results.get("qualitymodifierid")+";"+qm); //use , not ;. ; used to separate qualitymodifiers of different quality
						}
						qm = result.get("entitylabel")==null? "": result.get("entitylabel")+","+
								result.get("entitylocatorlabel")==null? "":result.get("entitylocatorlabel");
						if(qm.replaceFirst("(^,+|,+$)", "").trim().length()>0){
							results.put("qualitymodifierlabel", results.get("qualitymodifierlabel")+";"+qm); //use , not ;. ; used to separate qualitymodifiers of different quality
						}
					}
				}else{//no, the relation should not be considered relational quality
					//entity locator?
					if (r.matches("\\((" + Dictionary.positionprep + ")\\).*")) { // entitylocator
						//if (r.contains("between"))
						//	entitylocator += "between " + toname + ",";
						//else
						//	entitylocator += toname + ",";
						if(results.get("entitylocator")==null){
							results.put("entitylocator", toname);
						}else{
							results.put("entitylocator", results.get("entitylocator")+","+toname); // connected by ',' because all entitylocators are related to the same entity: the from structure 
						}
						Hashtable<String, String> result = es.searchEntity(root, toid, toname, "", toname, relation,  0);
						if(result!=null){
							String entityid = result.get("entityid");
							if(entityid !=null){
								if(results.get("entitylocatorid")==null){
									results.put("entitylocatorid", entityid);
									results.put("entitylocatorlabel", result.get("entitylabel"));
								}else{
									results.put("entitylocatorid", results.get("entitylocatorid")+","+entityid); // connected by ',' because all entitylocators are related to the same entity: the from structure 
									results.put("entitylocatorlabel", results.get("entitylocatorlabel")+","+result.get("entitylabel")); 
								}
							}
						}
					} else if (r.matches("\\(with\\).*")) {
						//check to-structure, if to-structure has no character, then generate EQ to_entity:present
						if(!hasCharacters(toid, root) && !keyelement){
							Hashtable<String, String> EQ = new Hashtable<String, String>();
							Utilities.initEQHash(EQ);
							Hashtable<String, String> result = es.searchEntity(root, toid, toname, "", toname, relation, 0);
							EQ.put("entity", toname);
							EQ.put("quality", "present");
							EQ.put("type", keyelement ? "character" : "state");
							if(result!=null){
								if(result.get("entityid") !=null)
									EQ.put("entityid", result.get("entityid"));
								EQ.put("qualityid", "PATO:0000467");
								if(result.get("entitylabel")!=null)
									EQ.put("entitylabel", result.get("entitylabel"));
								EQ.put("qualitylabel", "present");
							}
							ArrayList<Hashtable<String, String>> extraEQs = (ArrayList<Hashtable<String, String>>) results.get("extraEQs");
							extraEQs.add(EQ);
						}
					} else if (r.matches("\\(without\\).*")) {
						// output absent as Q for toid
						if (!keyelement) {
							Hashtable<String, String> EQ = new Hashtable<String, String>();
							Utilities.initEQHash(EQ);
							Hashtable<String, String> result = es.searchEntity(root, toid, toname, "", toname, relation, 0);
							EQ.put("entity", toname);
							EQ.put("quality", "absent");
							EQ.put("type", keyelement ? "character" : "state");
							if(result!=null){
								if(result.get("entityid") !=null)
									EQ.put("entityid", result.get("entityid"));
								EQ.put("qualityid", "PATO:0000462");
								if(result.get("entitylabel")!=null)
									EQ.put("entitylabel", result.get("entitylabel"));
								EQ.put("qualitylabel", "absent");
							}
							ArrayList<Hashtable<String, String>> extraEQs = (ArrayList<Hashtable<String, String>>) results.get("extraEQs");
							extraEQs.add(EQ);
						}
					} else {//qualitymodifier to which quality??? could indicate an error, but output anyway
						Hashtable<String, String> result = es.searchEntity(root, toid, toname, "", toname, relation, 0);
						results.put("qualitymodifier", toname);
						if(result!=null){
							String entityid =result.get("entityid");
							if(entityid!=null){
								if(results.get("qualitymodifierid")==null){
									results.put("qualitymodifierid", entityid);
									results.put("qualitymodifierlabel", result.get("entitylabel"));
								}else{
									results.put("qualitymodifierid", results.get("qualitymodifierid")+","+entityid); // connected by ',' because all entitylocators are related to the same entity: the from structure 
									results.put("qualitymodifierlabel", results.get("qualitymodifierlabel")+","+result.get("entitylabel")); // connected by ',' because all entitylocators are related to the same entity: the from structure 
								}
							}
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
	 * @return if match, return id and label, if not, return null
	 */
	private Hashtable<String, String> matchInRestrictedRelation(String fromstructure, String relation, String tostructure, boolean hascharacter) {
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
