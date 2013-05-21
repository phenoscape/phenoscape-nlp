package outputter;

import java.util.ArrayList;
import java.util.Hashtable;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * 
 * @author Hong Cui
 *
 */
public class EntitySearcher5 extends EntitySearcher {

	public EntitySearcher5() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public EntityProposals searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		//bone, cartilage,  element
		//Epibranchial 1: (0) present and ossified E: Epibranchial 1 bone, Q: present
		//Epibranchial 1: (1) present and cartilaginous E: Epibranchial 1 cartilage, Q: present
		//Epibranchial 1: (2) absent E: Epibranchial 1 cartilage, Q: absent E: Epibranchial 1 bone, Q: absent
		//The curator should use both the cartilage and bone terms to annotate state 2 because the author clearly differentiates between the two.
		 
		//search with regular expression  "epibranchial .*" to find possible missing headnouns 
		if(entityphrase.indexOf(" ")<0 && entityphrase.compareTo(originalentityphrase)==0){
			Hashtable<String, String> headnouns = new Hashtable<String, String>();
			ArrayList<FormalConcept> regexpresults = TermSearcher.regexpSearchTerm(entityphrase+" .*", "entity");
			if(regexpresults!=null){
				for(FormalConcept regexpresult: regexpresults){
					headnouns.put(regexpresult.getLabel().replace(entityphrase, ""), regexpresult.getId()+"#"+regexpresult.getClassIRI());
				}			
			}
			//search headnouns in the context 
			String noun = searchContext(root, structid, headnouns); //bone, cartilaginous
			if(noun != null){
				String[] idiri = headnouns.get(noun).split("#");
				SimpleEntity sentity = new SimpleEntity();
				sentity.setString(entityphrase);
				sentity.setLabel(entityphrase);
				sentity.setId(idiri[0]);
				sentity.setConfidenceScore((float)1.0);
				sentity.setClassIRI(idiri[1]);
				EntityProposals entities = new EntityProposals();
				entities.setPhrase(sentity.getString());
				entities.add(sentity);
				return entities;
			}else{
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
				
			}
		}
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
	 * @param root
	 * @param structid
	 * @param target
	 * @return
	 */
	private static String searchContext(Element root, String structid, Hashtable<String, String> targets){
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
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
