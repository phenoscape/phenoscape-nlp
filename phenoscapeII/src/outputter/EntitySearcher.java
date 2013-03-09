/**
 * 
 */
package outputter;

import java.util.List;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.jdom.xpath.XPath;

import owlaccessor.OWLAccessorImpl;

/**
 * @author hong cui
 * this class tries to adjust the splitting point between entity and entitylocator, depending on the ontology lookup results
 * try different strategies to form entity and entity locator (optional) and search the ontologies by calling searchTerm.
 * 
 * 		//unhandled cases: 
		//upper pharyngeal tooth plates 4 and 5 => upper pharyngeal tooth plate
		//humeral deltopectoral crest apex => process
		//joints: Junction between metapterygoid and hyomandibular, 300+ examples at SELECT * FROM fish_original_1st WHERE entitylabel LIKE "%joint%";
		// = something connecting two bones =>to be handled by KeyEnttityFinder
 */
public class EntitySearcher {
	private Dictionary dict;
	private static XPath textpath;
	private TermSearcher ts;
	static{
		try{
			textpath = XPath.newInstance(".//text");
		}catch(Exception e){
			e.printStackTrace();
		}
	}


	/**
	 * 
	 * @param entity
	 * @param entitylocator: may be an empty string
	 */
	public EntitySearcher(Dictionary dict) {
		this.dict = dict;
		TermSearcher ts = new TermSearcher(dict);
	}

	/**
	 * * Search a phrase A B C
	 * search A B C
	 * if succeeds, search the parent entity locator + A B C [tooth => ceratobranchial 5 tooth]
	 * if succeeds, entityphrase = p.e.l + A B C, entitylocator = entitylocator - p.e.l
	 * if fails, entityphrase = A B C, entitylocator = entitylocator
	 * if fails, search B C
	 * if succeeds, entityphrase = B C, entitylocator = (entityphrase - B C), entitylocator
	 * if fails, search C
	 * if succeeds, entityphrase = C, entitylocator = (entityphrase - C), entitylocator
	 * if fails, search the parent entity locator
	 * if succeeds, entitylable = p.e.l*, entitylocator = entitylocator - p.e.l
	 * if fails, search the next parent entity locator
	 * ....
	 *
	 * @param entityphrase the entityphrase
	 * @param elocatorphrase the elocatorphras
	 * @param originalentityphrase the originalentityphrase
	 * @param preposition used between entityphrase and elocatorphrase
	 * @return 4-key Hashtable<String, String>: keys: entity, entitylocator values: ids of entity and entitylocator
	 * @throws Exception the exception
	 */
	public Hashtable<String, String> searchEntity(Element root, String structid,  String entityphrase, String elocatorphrase, String originalentityphrase, String prep, int ingroup){
		//System.out.println("search entity: "+entityphrase);
		Hashtable<String, String> result = new Hashtable<String, String>();
		result.put("entity", "");//TODo, change to entity, taking ID for a value
		result.put("entitylocator", "");
		//entityresults.put("entitylocatorphrase", elocatorphrase);
		//TODO create and maintain a cache for entity search?
	
		//special case: dealing with process
		entityphrase = entityphrase.replaceAll("("+Dictionary.process+")", "process");
		elocatorphrase = elocatorphrase.replaceAll("("+Dictionary.process+")", "process");
		entityphrase = entityphrase.replaceAll("latero-sensory", "sensory");
		elocatorphrase = elocatorphrase.replaceAll("laterosensory", "sensory");
		
		String finalentitylocator = "";
		String finalentityphrase = "";
		String finalentityid = "";
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
				Hashtable<String, String> r = ts.searchTerm(phrase, "entity", ingroup);
				if(r!=null){
					result.put("entity", r.get("id")); // entitylocator = "";
					return result;
				}
			}			
		}
		
		//search entity and entity locator separately
		
		Hashtable<String, String> er = ts.searchTerm(entityphrase, "entity", ingroup);
		result.put("entitylocator", searchEntityLocator(elocatorphrase, entitylocators, ingroup));
		//anterior margin of maxilla => anterior margin^part_of(maxilla)): entity = anterior margin, locator = maxilla


		if(er!=null){//if entity matches
			result.put("entity",  er.get("id"));			
			return result;
		}
		
		//re-arranging word in entity, first search for entity locator
				
		//"maxillary process" => process^part_of(maxilla) : entity = process, locator = maxilla
		String adjID = ts.adjectiveOrganSearch(entityphrasetokens[0]);
		if(adjID!=null){
			result.put("entitylocator", adjID);
			String newentity = Utilities.join(entityphrasetokens, 1, entityphrasetokens.length-1, " ");
			er = ts.searchTerm(newentity, "entity", ingroup);
			if(er!=null){
				result.put("entity", er.get("id"));
				return result;
			}else{
				//TODO
			}
					
		}
		
		//anterior process of the maxilla => process^part_of(anterior region^part_of(maxilla)): entity = process, locator = anterior region, maxilla
		if(entityphrasetokens[0].matches("("+dict.spatialtermptn+")")){
			String newentity = Utilities.join(entityphrasetokens, 1, entityphrasetokens.length-1, " "); //process
			er = ts.searchTerm(newentity, "entity", ingroup);
			if(er!=null){
				result.put("entity", er.get("id"));
				er = ts.searchTerm(entityphrasetokens[0]+" region", "entity", ingroup);//anterior + region
				if(er!=null){
					String locators = (er.get("id")+","+result.get("entitylocator")).replaceFirst(",$", ""); //anterior region, maxilla
					result.put("entitylocator", locators);
					return result;
				}				
			}else{
				//TODO
			}
		}
		
		//bone, cartilage,  element
		//Epibranchial 1: (0) present and ossified E: Epibranchial 1 bone, Q: present
		//Epibranchial 1: (1) present and cartilaginous E: Epibranchial 1 cartilage, Q: present
		//Epibranchial 1: (2) absent E: Epibranchial 1 cartilage, Q: absent E: Epibranchial 1 bone, Q: absent
		//The curator should use both the cartilage and bone terms to annotate state 2 because the author clearly differentiates between the two.
		 
		//search with regular expression  "epibranchial .*" to find possible missing headnouns 
		if(entityphrase.indexOf(" ")<0 && entityphrase.compareTo(originalentityphrase)==0){
			Hashtable<String, String> headnouns = new Hashtable<String, String>();
			ArrayList<Hashtable<String, String>> regexpresults = ts.regexpSearchTerm(entityphrase+" .*", "entity", ingroup);
			if(regexpresults!=null){
				for(Hashtable<String, String> regexpresult: regexpresults){
					headnouns.put(regexpresult.get("label").replace(entityphrase, ""), regexpresult.get("id"));
				}			
			}
			//search headnouns in the context 
			String noun = searchContext(root, structid, headnouns); //bone, cartilaginous
			if(noun != null){
				result.put("entity", headnouns.get(noun));
				return result;
			}
		}	
		
		//still not find a match, remove the last term in the entityphrase, when what is left is not just a spatial term 
		//"humeral deltopectoral crest apex" => "humeral deltopectoral crest"		
		//the last token could be a number (index)
		//Changed by Zilong:
		//enhanced entity format condition to exclude the spatial terms: in order to solve the problem that 
		//"rostral tubule" will match "anterior side" because rostral is synonymous with anterior
		
		
		String[] tokens = entityphrase.split("\\s+");
		if(tokens.length>=2){ //to prevent "rostral tubule" from entering the subsequent process 
			String shortened = entityphrase.substring(0, entityphrase.lastIndexOf(" ")).trim();
			if(!shortened.matches(".*?\\b("+dict.spatialtermptn+")$")){
				er = ts.searchTerm(shortened, "entity", ingroup);
				if(er!=null){
					result.put("entity", er.get("id"));
					return result;
				}else{
					//TODO
				}
			}			
		}
	
		//shrinking 
		/*int size = entityphrasetokens.length - 1;
		for(int i = 0; i <= size; i++){
			String entityterm = Utilities.join(entityphrasetokens, i, size, " "); 
			Hashtable<String, String> result = new TermSearcher(dict).searchTerm(entityterm, "entity", ingroup);
			if(result!=null){
				if(entitylocators != null && i==0 && elocatorphrase.length()>0){//has entitylocator
					//TODO deal with entity/entity locator expressed in other forms: upper pharyngeal tooth vs. tooth of upper pharyngeal etc.
					Hashtable<String, String> newresult = new TermSearcher(dict).searchTerm(entitylocators[0]+" "+entityterm, "entity", ingroup);
					if(newresult!=null){
						finalentityid = newresult.get("id");
						finalentityphrase = newresult.get("label");
						finalentitylocator = elocatorphrase.replaceFirst(entitylocators[0], "").replaceAll("^\\s*,\\s*", "");
						break;
					}
				}
				finalentityid = result.get("id");
				finalentityphrase = result.get("label");
				String left = entityphrase.replaceFirst(entityterm, "").trim();//e.g. ventral [process]
				if(!dict.spatialterms.contains(left)){
					finalentitylocator = left+","+elocatorphrase.trim();
				}else{
					finalentitylocator = elocatorphrase;
				}
				finalentitylocator = finalentitylocator.replaceFirst(",$", "").replaceFirst("^,", "").trim();
				break;			
			}else{
				if(i == size && entitylocators!= null){//entityphrase returned no result, try entitylocators
					int j = 0;
					while(result==null && j<entitylocators.length){
						result = new TermSearcher(dict).searchTerm(entitylocators[j], "entity", ingroup);
						j++;
					}
					if(result!=null){
						finalentityid = result.get("id");
						finalentityphrase = result.get("label");
						finalentitylocator = Utilities.join(entitylocators, j, entitylocators.length-1, ",");
						break;
					}
				}
			}				
		}*/		
		
		
		return result;
	}
	
	/**
	 * look into text context for statements containing structid 
	 * to determin the target the context is most close to. for example
	 *  //bone, cartilage,  element
		//Epibranchial 1: (0) present and ossified E: Epibranchial 1 bone, Q: present
		//Epibranchial 1: (1) present and cartilaginous E: Epibranchial 1 cartilage, Q: present
		//Epibranchial 1: (2) absent E: Epibranchial 1 cartilage, Q: absent E: Epibranchial 1 bone, Q: absent
		//The curator should use both the cartilage and bone terms to annotate state 2 because the author clearly differentiates between the two.
	 * @param root
	 * @param structid
	 * @param target
	 * @return
	 */
	private String searchContext(Element root, String structid, Hashtable<String, String> targets){
		try{
			Element statement = (Element) XPath.selectSingleNode(root, ".//statement/structure[@id='"+structid+"']");
			//could perform a content similarity measure between the defintions associated with the targets in ontology and the text of the statement
			String text = statement.getChildText("text");
			if(targets.get("bone") != null && targets.get("cartilage")!=null){
				int bonecount = text.replaceAll("(ossifi|bone)", "#").replaceAll("[^#]", "").length();
				int cartcount = text.replaceAll("cartil", "#").replaceAll("[^#]", "").length();
				if(bonecount > cartcount) return "bone";
				if(bonecount < cartcount) return "cartilage";
				if(bonecount == cartcount) return "element";
			}			
		}catch(Exception e){
			e.printStackTrace();
		}		
		return null;
	}
	
	private String searchEntityLocator(String elocatorphrase, String[] entitylocators, int ingroup){
		if(entitylocators!=null) {
			Hashtable<String, String> elr =  ts.searchTerm(elocatorphrase, "entity", ingroup);
			if(elr!=null){
				return elr.get("id");
			}else{ //entity locator not matched
				//TODO
			}
		}
		return "";
	}

	/**
	 * Valid.
	 *
	 * @param organphrase the organphrase
	 * @return true, if successful
	 * @throws Exception the exception
	 */
//	private boolean valid(String organphrase) throws Exception{
//
//			String text = organphrase.replaceAll("[<>]", "");
//			boolean flag1= false;
//			boolean flag2 = false;
//			Statement stmt = conn.createStatement();
//			ResultSet rs = stmt.executeQuery("select count(*) from "+dataprefix+"_markedsentence where markedsent like '%"+organphrase+"%'");
//			if(rs.next() && rs.getInt(1)>0) flag1=true;
//			rs = stmt.executeQuery("select count(*) from "+dataprefix+"_sentence where sentence like '%"+text+"%'");
//			if(rs.next() && rs.getInt(1)>0) flag2=true;
//			return flag1&&flag2;
//
//	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
