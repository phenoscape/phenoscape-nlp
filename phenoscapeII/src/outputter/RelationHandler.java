/**
 * 
 */
package outputter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import org.jdom.Element;
import org.jdom.xpath.XPath;

import edu.mit.jwi.Dictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISenseEntry;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.morph.WordnetStemmer;

/**
 * @author updates
 * Restricted relation list:
 * http://phenoscape.org/wiki/Guide_to_Character_Annotation#Relations_used_for_post-compositions
 */
public class RelationHandler {
	private outputter.Dictionary dict;
	private EntitySearcher es;
	IDictionary wordnetdict;

	/**
	 * 
	 */
	public RelationHandler(outputter.Dictionary dict, EntitySearcher es) {
		this.dict = dict;
		this.es =es;
		
	
		
	//String path="C:/Users/Murali/Desktop/RA/External jars/wn3.1.dict/dict";
		//System.out.println(System.getProperty("user.dir")				+System.getProperty("file.separator")+ApplicationUtilities.getProperty("wordnet.dictionary"));
		//Initializing wordnet dictionary	
		wordnetdict = new edu.mit.jwi.Dictionary(new File(ApplicationUtilities.getProperty("wordnet.dictionary")));
		try {
			wordnetdict.open();			
		//	String realtion= matchInRestrictedRelation("a","go with","b",true);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
					if (r.matches("\\((" + outputter.Dictionary.positionprep + ")\\).*")) { // entitylocator
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
		String relation_ID=null;
		//checks if the given relation is present in the identified relationalqualities - Hariharan
		Hashtable<String, String> relationlist = null;
		if(dict.relationalqualities.contains(relation))
		{
			relationlist = new Hashtable<String, String>();
			relationlist.put("id", dict.relationalqualities.get(relation));
			relationlist.put("label", dict.resrelationQ.get(dict.relationalqualities.get(relation)));
			return relationlist;
		}
		//if failed in above steps then it uses wordnet to find the different synonyms of the relation string
		Hashtable<String, Integer> forms = getdifferentrelationalforms(relation);
		//of the identified relations, it finds the best equivalent relation else it returns null
		if(forms!=null)
			{
			
			relation_ID=getbestrelation(forms,relation);
			if(relation_ID!=null)
			{
				relationlist = new Hashtable<String, String>();
				relationlist.put("id", relation_ID);
				relationlist.put("label", dict.resrelationQ.get((relation_ID)));
			}
			return relationlist;
			}
		
		return relationlist;
	}

	// decides the best equivalent relation from different identified relations using some semantic measures
	private String getbestrelation(Hashtable<String, Integer> forms,String  relation) {
		// TODO 
		//update this later to find the closest similar relation
		//int probability =0,maxprobability=0;
		Set<String> keys;
		//System.out.println("forms size" +forms.size());
		keys = forms.keySet();
		for(String form:keys)
			if(dict.relationalqualities.containsKey(form))
				return dict.relationalqualities.get(form);
		
		return null;
	}

	// returns the different relational forms of the current passed string
	private Hashtable<String,Integer> getdifferentrelationalforms(String relation) {
		// TODO Auto-generated method stub
		//Top Synonyms
		Hashtable<String, Integer> synonyms;
		synonyms= getallsynonyms(relation);
		//System.out.println("size == "+synonyms.size());
		return synonyms;
	}
	// This methods get all the possible synonyms with POS of the word passed.

	private Hashtable<String,Integer> getallsynonyms(String word) {
		// TODO Auto-generated method stub
		Hashtable<String,Integer> synonyms = new Hashtable<String,Integer>();
		ISenseEntry senseEntry;
		WordnetStemmer stemmer = new WordnetStemmer(wordnetdict);
		
		for(edu.mit.jwi.item.POS pos : edu.mit.jwi.item.POS.values()) {
			
			List<String> stems = stemmer.findStems(word, pos);
			//System.out.println(stems.size());
			for(String stem : stems) {		
				//System.out.println(stem);
				IIndexWord indexWord = wordnetdict.getIndexWord(stem, pos);
				if(indexWord!=null) {
					int count = 0;
					for(IWordID wordId : indexWord.getWordIDs()) {
						//System.out.println(wordId);
						IWord aWord = wordnetdict.getWord(wordId);
						ISynset synset = aWord.getSynset();
						
						for( IWord w : synset.getWords ())
							{
							synonyms.put(w.getLemma().replaceAll("_", " ").trim(),wordnetdict.getSenseEntry(aWord.getSenseKey()).getTagCount());
						//	System.out.println(w.getLemma().replaceAll("_", " ").trim()+ wordnetdict.getSenseEntry(aWord.getSenseKey()).getTagCount());
							}
					
					}
									
				}
			}
		}	
		
		return synonyms;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//RelationHandler rh = new RelationHandler(new outputter.Dictionary(new ArrayList<String>()), new EntitySearcher(new outputter.Dictionary(new ArrayList<String>())));
		

	}

}
