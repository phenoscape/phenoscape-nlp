package outputter.search;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.xpath.XPath;

import outputter.Utilities;
import outputter.data.EntityProposals;
import outputter.data.FormalConcept;
import outputter.data.SimpleEntity;

/**
 * 
 * @author Hong Cui
 * strategy for phrases that omitted the head noun, for example epibranchial, which
 * may match Epibranchial bone, Epibranchial element, or Epibranchial cartilage
 *
 */
public class EntitySearcher5 extends EntitySearcher {
	private static final Logger LOGGER = Logger.getLogger(EntitySearcher5.class);   
	private static Hashtable<String, ArrayList<EntityProposals>> cache = new Hashtable<String, ArrayList<EntityProposals>>();
	private static ArrayList<String> nomatchcache = new ArrayList<String>();
	
	public EntitySearcher5() {

	}

	@Override
	public ArrayList<EntityProposals> searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		LOGGER.debug("EntitySearcher5: search '"+entityphrase+"[orig="+originalentityphrase+"]'");
		
		//search cache
		if(EntitySearcher5.nomatchcache.contains(entityphrase+"+"+elocatorphrase)) return null;
		if(EntitySearcher5.cache.get(entityphrase+"+"+elocatorphrase)!=null) return EntitySearcher5.cache.get(entityphrase+"+"+elocatorphrase);
		//TODO take care of elocatorphrase
		
		//bone, cartilage,  element
		//Epibranchial 1: (0) present and ossified E: Epibranchial 1 bone, Q: present
		//Epibranchial 1: (1) present and cartilaginous E: Epibranchial 1 cartilage, Q: present
		//Epibranchial 1: (2) absent E: Epibranchial 1 cartilage, Q: absent E: Epibranchial 1 bone, Q: absent
		//The curator should use both the cartilage and bone terms to annotate state 2 because the author clearly differentiates between the two.
		 

		//search with regular expression  "epibranchial .*" to find possible missing headnouns 
		String aentityphrase = entityphrase.replaceFirst("^\\(\\?:", "").replaceFirst("\\)$", "");	
		//if(entityphrase.indexOf(" ")<0 && entityphrase.compareTo(originalentityphrase)==0){
		if(aentityphrase.indexOf(" ")<0){
			Hashtable<String, String> headnouns = new Hashtable<String, String>();
			//ArrayList<FormalConcept> regexpresults = TermSearcher.regexpSearchTerm(entityphrase+" .*", "entity");
			ArrayList<FormalConcept> regexpresults = new TermSearcher().searchTerm(aentityphrase+" .*", "entity");
			String nouns = null;
			if(regexpresults!=null){
				LOGGER.debug("...search entity '"+aentityphrase+" .*' found match");
				for(FormalConcept regexpresult: regexpresults){
					//regexpresult.setSearchString(originalentityphrase+"["+regexpresult.getSearchString()+"]"); //record originalentityphrase for grouping entity proposals later
					headnouns.put(regexpresult.getLabel().replace(aentityphrase, ""), regexpresult.getId()+"#"+regexpresult.getClassIRI()); //don't trim headnoun
				}	
				
				if(regexpresults.size()<10){
					//search headnouns in the context: coronoid .* => coronoid process of ulna
					//headnouns may have leading or trailing spaces, perserve them: hindlimb intermedium; intermedium (fore)
						nouns= searchContext(root, structid, headnouns); //bone, cartilaginous
				}
			}else{
				LOGGER.debug("...search entity '"+aentityphrase+" .*' found no match");
			}
			

			
			
			if(nouns != null){
				LOGGER.debug("...found candidate headnouns '"+nouns+"', forming proposals...");
				EntityProposals ep = new EntityProposals();
				ArrayList<EntityProposals> entities = null;
				//ep.setPhrase(entityphrase+" .*");
				ep.setPhrase(originalentityphrase);
				String[] choices = nouns.split(",");
				float score = 1.0f/regexpresults.size();
				boolean found = false;
				for(String noun: choices){
					String[] idiri = headnouns.get(noun).split("#");
					SimpleEntity sentity = new SimpleEntity();
					sentity.setSearchString(aentityphrase+" .*");
					sentity.setString(aentityphrase);
					sentity.setLabel(noun.startsWith(" ")? aentityphrase+noun: noun+aentityphrase);
					sentity.setId(idiri[0]);
					sentity.setConfidenceScore(score);
					sentity.setClassIRI(idiri[1]);
					ep.add(sentity);
					LOGGER.debug(".....add a proposal:"+sentity);
					found = true;
				}
				//entities.add(ep);
				if(found){
					if(entities==null) entities = new ArrayList<EntityProposals>();
					Utilities.addEntityProposals(entities, ep);

					//logging
					LOGGER.debug("EntitySearcher5 completed search for '"+aentityphrase+"[orig="+originalentityphrase+"]' and returns:");
					for(EntityProposals aep: entities){
						LOGGER.debug("..: "+aep.toString());
					}	
					
					if(entities==null) EntitySearcher5.nomatchcache.add(entityphrase+"+"+elocatorphrase);
					else EntitySearcher5.cache.put(entityphrase+"+"+elocatorphrase, entities);
					
					return entities;
				}
			}else{
				LOGGER.debug("...candidate headnouns is null, search failed");
			}
				/*else{
			
				//text::Caudal fin
				//text::heterocercal  (heterocercal tail is a subclass of caudal fin, search "heterocercal *")
				//return all matches as candidates
				if(regexpresults!=null){
					EntityProposals entities = new EntityProposals();
					for(FormalConcept regexpresult: regexpresults){
						Entity e = (Entity) regexpresult;
						entities.add(e);
					}			
					return entities;
				}
				
			}*/
			//caching
		}
		EntitySearcher5.nomatchcache.add(entityphrase+"+"+elocatorphrase);
		LOGGER.debug("...search for entity '"+entityphrase+"' found no match");
		LOGGER.debug("EntitySearcher5 calls EntitySearcher6");
		return new EntitySearcher6().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
			
	}

	/**
	 * look into text context for statements containing structid 
	 * to determin the target the context is most close to. for example
	 *  //bone, cartilage,  element
		//Epibranchial 1: (0) present and ossified E: Epibranchial 1 bone, Q: present
		//Epibranchial 1: (1) present and cartilaginous E: Epibranchial 1 cartilage, Q: present
		//Epibranchial 1: (2) absent E: Epibranchial 1 cartilage, Q: absent E: Epibranchial 1 bone, Q: absent
		//The curator should use both the cartilage and bone terms to annotate state 2 because the author clearly differentiates between the two.
	 * 	//could perform a content similarity measure between the definitions associated with the targets in ontology and the text of the statement
	 * @param root
	 * @param structid
	 * @param target
	 * @return
	 */
	private static String searchContext(Element root, String structid, Hashtable<String, String> targets){
		try{
			Element structure = (Element) XPath.selectSingleNode(root, ".//statement/structure[@id='"+structid+"']");
			Element statement = structure.getParentElement();
			String text = statement.getChildText("text");
			//disambiguate between bone and cartilage
			if(targets.get(" bone") != null && targets.get(" cartilage")!=null){
				int bonecount = text.replaceAll("(ossifi|bone)", "#").replaceAll("[^#]", "").length();
				int cartcount = text.replaceAll("cartil", "#").replaceAll("[^#]", "").length();
				if(bonecount > cartcount) return "bone";
				if(bonecount < cartcount) return "cartilage";
				if(bonecount == cartcount) return "element";
			}			
			//filter other cases: prefer phrases one-word longer than the original phrase 
			String result = "";
			Enumeration<String> keys = targets.keys();
			while(keys.hasMoreElements()){
				String noun = keys.nextElement();
				if(noun.indexOf(" of ")>=0 || noun.indexOf(" and ")>=0) continue; //coronoids 'proccess of ulna': coronoids can't possibility be used to represent a complex concept that require the use of "of"
				if(noun.trim().indexOf(" ")<= 0){
					if(!related(noun.trim(), structid, root)){ //text: coronoids with tooth, then 'coronoid tooth' should be filtered
						result += noun+","; //don't trim noun
					}
				}
			}
			if(result.trim().length()>0){
				return result.replaceFirst(",$", "");
			}			
		}catch(Exception e){
			LOGGER.error("", e);
		}		
		return null;
	}
		
	/**
	 * 
	 * @param noun: the headnoun candidate
	 * @param structid: the structure that in need of the headnoun
	 * @param root: the root of the xml file
	 * @return whether the headnoun and the structure is connected via a <relation> chain in xml
	 */
	@SuppressWarnings("unchecked")
	private static boolean related(String noun, String structid, Element root) {
		try{
			XPath xpath = XPath.newInstance("//relation[@from='"+structid+"']");
			List<Element> relations = xpath.selectNodes(root);
			for(Element relation: relations){
				String toid = relation.getAttributeValue("to");
				Element related = (Element) XPath.selectSingleNode(root, "//structure[@id='"+toid+"']");
				if(related.getAttributeValue("name").compareTo(noun)==0){
					return true;
				}else{
					return related(noun, toid, root);
				}
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
		return false;
	}

	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
