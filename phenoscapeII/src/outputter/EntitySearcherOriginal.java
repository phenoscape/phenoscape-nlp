/**
 * 
 */
package outputter;

import java.util.List;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import owlaccessor.OWLAccessorImpl;

/**
 * @author hong cui
 * this class tries to adjust the splitting point between entity and entitylocator, depending on the ontology lookup results
 * try different strategies to form entity and entity locator (optional) and search the ontologies by calling searchTerm.
 * TODO
 * 		//unhandled cases: 
		//upper pharyngeal tooth plates 4 and 5 => upper pharyngeal tooth plate
		//humeral deltopectoral crest apex => process
		//joints: Junction between metapterygoid and hyomandibular, 300+ examples at SELECT * FROM fish_original_1st WHERE entitylabel LIKE "%joint%";
		// = something connecting two bones =>to be handled by KeyEnttityFinder
 */
public class EntitySearcherOriginal extends EntitySearcher {

	private static final Logger LOGGER = Logger.getLogger(EntitySearcherOriginal.class);  
	private static Hashtable<String, ArrayList<EntityProposals>> cache = new Hashtable<String, ArrayList<EntityProposals>>();
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
	 * @return null or an entity (simple or composite) [6-key Hashtable<String, String>: keys: entity, entityid, entitylabel, entitylocator, entitylocatorid, entitylocatorlabel]
	 * @throws Exception the exception
	 */
	@SuppressWarnings("unchecked")
	public ArrayList<EntityProposals> searchEntity(Element root, String structid,  String entityphrase, String elocatorphrase, String originalentityphrase, String prep){
		//System.out.println("search entity: "+entityphrase);
		//create and maintain a cache for entity search?: yes, created in EntityParser
		LOGGER.debug("EntitySearcherOriginal: search '"+entityphrase+"[orig="+originalentityphrase+"]'");


		//'sexes' =>multi-cellular organism organism 'bearer of' female/male
		String origname = Utilities.getOriginalStructureName(root, structid);
		if(origname!=null && origname.compareTo("sexes")==0){
			ArrayList<EntityProposals> eps = new ArrayList<EntityProposals>();
			//female:
			ArrayList<FormalConcept> femalefcs = new TermSearcher().searchTerm("female", "quality");
			EntityProposals ep = new EntityProposals();
			FormalRelation bearer = Dictionary.bearerof;
			SimpleEntity organism = (SimpleEntity) new TermSearcher().searchTerm("multi-cellular organism", "entity").get(0);
			LOGGER.debug("EntitySearcherOriginal: formed EntityProposals with CompositeEntity for 'female'");
			for(FormalConcept fc: femalefcs){
				Quality female = (Quality)fc;
				REntity re1 = new REntity(bearer, Utilities.wrapQualityAs(female)); //may alternatively relax REntity to allow Quality 

				CompositeEntity ce1 = new CompositeEntity();
				ce1.setString("female");
				ce1.addEntity(organism);
				ce1.addEntity(re1);
				ep.setPhrase("female"); //the phrase set this proposal apart from the other one
				ep.add(ce1);
				LOGGER.debug(".."	+ce1.toString());
			}
		
			eps.add(ep); //add one entity
			
			//male:
			ArrayList<FormalConcept>  malefcs = new TermSearcher().searchTerm("male", "quality");
			ep = new EntityProposals();
			ep.setPhrase("male");//the phrase set this proposal apart from the other one
			LOGGER.debug("EntitySearcherOriginal: formed EntityProposals with CompositeEntity for 'male'");
			for(FormalConcept fc: malefcs){
				Quality male = (Quality)fc;
				REntity re2 = new REntity(bearer, Utilities.wrapQualityAs(male)); 	
				CompositeEntity ce2 = new CompositeEntity();
				ce2.addEntity(organism);
				ce2.addEntity(re2);
				ce2.setClassIRI("male");
				ep.add(ce2);
				LOGGER.debug(".."+ce2.toString());
			}
			eps.add(ep); //add the other entity
			LOGGER.debug("EntitySearcherOriginal completed search for '"+entityphrase+"[orig="+originalentityphrase+"]' and returned two EntityProposals");
			return eps;
		}


		//each of entityphrase and elocatorphrase may be multiple names separated by ","
		if(entityphrase.indexOf(",")>0){
			String temp = entityphrase.indexOf(",")>0 ? entityphrase.substring(0, entityphrase.indexOf(",")).trim() : entityphrase; // the first seg
			String ltemp = entityphrase.indexOf(",")>0 ? entityphrase.substring(entityphrase.indexOf(",")+1).trim() : "";
			if(elocatorphrase.length()==0) elocatorphrase = ltemp;
			else elocatorphrase = ltemp+","+elocatorphrase; //all remaining segs, separate by ","
			entityphrase = temp;
		}

		entityphrase = Utilities.transform(entityphrase);
		elocatorphrase = Utilities.transform(elocatorphrase);

		//special case: dealing with process
		entityphrase = entityphrase.replaceAll("("+Dictionary.process+")", "process");
		elocatorphrase = elocatorphrase.replaceAll("("+Dictionary.process+")", "process");
		entityphrase = entityphrase.replaceAll("latero-sensory", "sensory");
		elocatorphrase = elocatorphrase.replaceAll("laterosensory", "sensory");
		//entityphrase = entityphrase.replaceAll("body scale", "dermal scale");
		//elocatorphrase = elocatorphrase.replaceAll("body scale", "dermal scale");


		LOGGER.debug("EntitySearcherOriginal calls EntitySearcher0");
		ArrayList<EntityProposals> entities =  new EntitySearcher0().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
		
		if(entities==null){
			//If not found in Ontology, then return the phrase as simpleentity string
			//TODO return "some anatomical entity" or other high level concepts. 
			//don't forget the entityl
			LOGGER.debug("EntitySearcherOriginal: no match in ontology is found for '"+entityphrase+"','"+elocatorphrase+"', form string-based proposals...");
			EntityProposals ep = new EntityProposals();
			entities = new ArrayList<EntityProposals>();
			SimpleEntity sentity = new SimpleEntity();
			sentity.setString(entityphrase);
			sentity.confidenceScore=0f;
			if(elocatorphrase.length()>0){
				//relation & entity locator
				FormalRelation rel =  Dictionary.partof;
				rel.setConfidenceScore((float)1.0);
				SimpleEntity entityl = new SimpleEntity();
				entityl.setString(elocatorphrase);
				REntity rentity = new REntity(rel, entityl);
				//composite entity
				CompositeEntity centity = new CompositeEntity();
				centity.addEntity(sentity);
				centity.addEntity(rentity);
				//EntityProposals entities = new EntityProposals();
				//ep.setPhrase(sentity.getString());
				centity.setString(originalentityphrase);
				ep.setPhrase(originalentityphrase);
				ep.add(centity);
				LOGGER.debug("add a proposal:"+centity.toString());
				//entities.add(ep);
				Utilities.addEntityProposals(entities, ep);
			}else{
				//EntityProposals entities = new EntityProposals();
				//ep.setPhrase(sentity.getString());
				ep.setPhrase(originalentityphrase);
				ep.add(sentity);
				LOGGER.debug("add a proposal:"+sentity.toString());
				//entities.add(ep);
				Utilities.addEntityProposals(entities, ep);
			}

		}
		LOGGER.debug("EntitySearcherOriginal completed search for '"+entityphrase+"[orig="+originalentityphrase+"]' and returns:");
		for(EntityProposals aep: entities){
			LOGGER.debug("..EntityProposals: "+aep.toString());
		}
		return entities;
		

		/*(String[] entitylocators = null;
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
				LOGGER.error("", e);
			}
			for(Element text : texts){
				if(text.getTextNormalize().toLowerCase().contains(phrase)){
					goodphrase = true;
					break;
				}
			}
			if(goodphrase){//perfect match for a pre-composed term
				SimpleEntity entity = (SimpleEntity)TermSearcher.searchTerm(phrase, "entity", ingroup);
				if(entity!=null){	
					return entity;
				}
			}			
		}

		//anterior margin of maxilla => anterior margin^part_of(maxilla)): entity = anterior margin, locator = maxilla

		//search entity and entity locator separately
		SimpleEntity entityl = new SimpleEntity();
		entityl.setString(elocatorphrase);
		if(entitylocators!=null) {
			SimpleEntity result = (SimpleEntity)TermSearcher.searchTerm(elocatorphrase, "entity", ingroup);
			if(result!=null){
				entityl = result;
			}else{ //entity locator not matched
				//TODO
			}
		}
		SimpleEntity sentity = (SimpleEntity)TermSearcher.searchTerm(entityphrase, "entity", ingroup);
		if(sentity!=null){//if entity matches
			//entity
			if(entityl.getString().length()>0){
				//relation & entity locator
				FormalRelation rel = new FormalRelation();
				rel.setString("part of");
				rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
				rel.setId("BFO:000050");
				rel.setConfidenceScore((float)1.0);
				REntity rentity = new REntity(rel, entityl);
				//composite entity
				CompositeEntity centity = new CompositeEntity();
				centity.addEntity(sentity);
				centity.addEntity(rentity);
				return centity;
			}else{
				return sentity;
			}
		}

		//re-arranging word in entity, first search for entity locator

		//"maxillary process" => process^part_of(maxilla) : entity = process, locator = maxilla
		//TODO: process of maxilla case
		String adjIDlabel = TermSearcher.adjectiveOrganSearch(entityphrasetokens[0]);
		if(adjIDlabel!=null){
			entityl = new SimpleEntity();
			entityl.setString(entityphrasetokens[0]);
			entityl.setLabel(adjIDlabel.substring(adjIDlabel.indexOf("#")+1));
			entityl.setId(adjIDlabel.substring(0, adjIDlabel.indexOf("#")));
			entityl.setConfidenceScore((float)1);
			String newentity = Utilities.join(entityphrasetokens, 1, entityphrasetokens.length-1, " ");
			sentity = (SimpleEntity)TermSearcher.searchTerm(newentity, "entity", ingroup);
			if(sentity!=null){
				//relation & entity locator
				FormalRelation rel = new FormalRelation();
				rel.setString("part of");
				rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
				rel.setId("BFO:000050");
				rel.setConfidenceScore((float)1.0);
				REntity rentity = new REntity(rel, entityl);
				//composite entity
				CompositeEntity centity = new CompositeEntity();
				centity.addEntity(sentity);
				centity.addEntity(rentity);
				return centity;

			}else{
				//TODO
			}			
		}

		//anterior process of the maxilla => process^part_of(anterior region^part_of(maxilla)): entity = process, locator = anterior region, maxilla
		if(entityphrasetokens[0].matches("("+Dictionary.spatialtermptn+")")){
			String newentity = Utilities.join(entityphrasetokens, 1, entityphrasetokens.length-1, " "); //process
			sentity = (SimpleEntity)TermSearcher.searchTerm(newentity, "entity", ingroup);
			if(sentity!=null){
				SimpleEntity sentity1 = (SimpleEntity)TermSearcher.searchTerm(entityphrasetokens[0]+" region", "entity", ingroup);//anterior + region
				if(sentity1!=null){
					//nested part_of relation
					if(entityl.getString().length()>0){ //maxilla
						//relation & entity locator: inner
						FormalRelation rel = new FormalRelation();
						rel.setString("part of");
						rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
						rel.setId("BFO:000050");
						rel.setConfidenceScore((float)1.0);
						REntity rentity = new REntity(rel, entityl);
						//composite entity = entity locator for sentity
						CompositeEntity centity = new CompositeEntity(); //anterior region^part_of(maxilla)
						centity.addEntity(sentity1); //anterior region
						centity.addEntity(rentity);	//^part_of(maxilla)	
						//relation & entity locator:outer 
						rel = new FormalRelation();
						rel.setString("part of");
						rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
						rel.setId("BFO:000050");
						rel.setConfidenceScore((float)1.0);
						rentity = new REntity(rel, centity);
						centity = new CompositeEntity(); //process^part_of(anterior region^part_of(maxilla))
						centity.addEntity(sentity); //process
						centity.addEntity(rentity);	//^part_of(anterior region^part_of(maxilla))
						return centity;
					}else{//sentity1 be the entity locator
						//relation & entity locator: 
						FormalRelation rel = new FormalRelation();
						rel.setString("part of");
						rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
						rel.setId("BFO:000050");
						rel.setConfidenceScore((float)1.0);
						REntity rentity = new REntity(rel, sentity1);
						//composite entity = entity locator for sentity
						CompositeEntity centity = new CompositeEntity(); 
						centity.addEntity(sentity); 
						centity.addEntity(rentity);	
						return centity;
					}	
				}				
			}else{
				//TODO
			}
		}

		//TODO
		/*
		 * Changed by Zilong: deal with spatial terms. 
		 */
		//String[] entityTerms=entity.toLowerCase().trim().split("\\s+");
		//if contains spatial terms
		//if(this.dictionary.spatialterms.contains(entityTerms[0])){
		//if the entity contains the spatial head noun 
		//	if(dictionary.spatialHeadNoun.contains(entityTerms[entityTerms.length-1])){
		//		String ne=entityTerms[0]+" region";//spatial term + region
		//		String nel=entityTerms[entityTerms.length-1]+","+entitylocator;
		//		EQ.put("entity", ne);
		//		EQ.put("entitylocator", nel);
		//	}
		//}

		/*case: Mesethmoid flares anteriorly-> E: anterior region(part_of(mesethmoid bone)), Q: decreased width*/
		/*need more supporting cases. For now, comment it out to avoid interference with cases like ventrally directed*/
		//String[] qualityTerms=quality.toLowerCase().trim().replaceAll("[\\[\\]]", "").split("\\s+");
		//		if(this.spatialterms.contains(qualityTerms[qualityTerms.length-1].replaceFirst("ly$", ""))){
		//			//if the quality contains the spatial term's adverb form.
		//			String ne = qualityTerms[qualityTerms.length-1].replaceFirst("ly$", "")+" region";
		//			String nel = entity+((entitylocator==null||entitylocator.equals(""))?"":(","+entitylocator));
		//			String nq = quality.toLowerCase().trim().replaceAll("\\[.*\\]", "");
		//			
		//			EQ.put("entity", ne);
		//			EQ.put("entitylocator", nel);
		//			EQ.put("quality", nq);
		//			
		//		}
		/*end handling spatial terms*/

		/* TODO
		 * Changed by Zilong: change any self-reference word to the keyentity(es)
		 * To add any new self-reference word, please modify the instance variable
		 * "selfReference." 
		 * 
		 * */

		//if(entity.toLowerCase().trim().matches("("+Dictionary.selfReference+")")){
		//	EQ.put("entity", this.keyentities.get(0));
		//}
		//if(entitylocator.toLowerCase().trim().matches("("+Dictionary.selfReference+")")){
		//	EQ.put("entitylocator", this.keyentities.get(0));
		//}
		//if(qualitymodifier.toLowerCase().trim().matches("("+Dictionary.selfReference+")")){
		//	EQ.put("qualitymodifier", this.keyentities.get(0));
		//}
		/*End dealing with self reference terms*/

		//bone, cartilage,  element
		//Epibranchial 1: (0) present and ossified E: Epibranchial 1 bone, Q: present
		//Epibranchial 1: (1) present and cartilaginous E: Epibranchial 1 cartilage, Q: present
		//Epibranchial 1: (2) absent E: Epibranchial 1 cartilage, Q: absent E: Epibranchial 1 bone, Q: absent
		//The curator should use both the cartilage and bone terms to annotate state 2 because the author clearly differentiates between the two.

		//search with regular expression  "epibranchial .*" to find possible missing headnouns 
		/*
		if(entityphrase.indexOf(" ")<0 && entityphrase.compareTo(originalentityphrase)==0){
			Hashtable<String, String> headnouns = new Hashtable<String, String>();
			ArrayList<FormalConcept> regexpresults = TermSearcher.regexpSearchTerm(entityphrase+" .*", "entity", ingroup);
			if(regexpresults!=null){
				for(FormalConcept regexpresult: regexpresults){
					headnouns.put(regexpresult.getLabel().replace(entityphrase, ""), regexpresult.getId());
				}			
			}
			//search headnouns in the context 
			String noun = searchContext(root, structid, headnouns); //bone, cartilaginous
			if(noun != null){
				sentity = new SimpleEntity();
				sentity.setString(entityphrase);
				sentity.setLabel(entityphrase);
				sentity.setId(headnouns.get(noun));
				sentity.setConfidenceScore((float)1.0);
				return sentity;
			}
		}	

		//finding the splitting point: body scale => scale of body
		String[] tokens = entityphrase.split("\\s+");
		for(int split = 0; split <= tokens.length-2; split++){
			String part1 = Utilities.join(tokens, 0, split, " ");
			String part2 = Utilities.join(tokens, split+1, tokens.length-1, " ");
			entityl = (SimpleEntity)TermSearcher.searchTerm(part1, "entity", 0); //locator
			SimpleEntity entity = (SimpleEntity)TermSearcher.searchTerm(part2, "entity", 0); //entity
			if(entityl!=null && entity!=null){
				//relation & entity locator: 
				FormalRelation rel = new FormalRelation();
				rel.setString("part of");
				rel.setLabel(Dictionary.resrelationQ.get("BFO:0000050"));
				rel.setId("BFO:000050");
				rel.setConfidenceScore((float)1.0);
				REntity rentity = new REntity(rel, entityl);
				//composite entity = entity locator for sentity
				CompositeEntity centity = new CompositeEntity(); 
				centity.addEntity(entity); 
				centity.addEntity(rentity);	
				return centity;
			}
		}

		//still not find a match, remove the last term in the entityphrase, when what is left is not just a spatial term 
		//"humeral deltopectoral crest apex" => "humeral deltopectoral crest"	
		//TODO "some part" of humerus; "some quality"
		//the last token could be a number (index)
		//Changed by Zilong:
		//enhanced entity format condition to exclude the spatial terms: in order to solve the problem that 
		//"rostral tubule" will match "anterior side" because rostral is synonymous with anterior


		tokens = entityphrase.split("\\s+");
		if(tokens.length>=2){ //to prevent "rostral tubule" from entering the subsequent process 
			String shortened = entityphrase.substring(0, entityphrase.lastIndexOf(" ")).trim();
			if(!shortened.matches(".*?\\b("+Dictionary.spatialtermptn+")$")){
				sentity = (SimpleEntity) TermSearcher.searchTerm(shortened, "entity", ingroup);
				if(sentity!=null){
					if(sentity.getId().compareTo(Dictionary.mcorganism)==0){
						//too general "body scale", try to search for "scale"
						//TODO: multi-cellular organism is too general a syn for body. "body" could mean something more restricted depending on the context.
						//TODO: change labels to ids
					}
					return sentity;
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


		//return null;
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
			StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);
			LOGGER.error(sw.toString());
		}		
		return null;
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

	/*
	@Override
	public boolean canHandle(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep, int ingroup) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setHandler(EntitySearcher handler, Element root,
			String structid, String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep, int ingroup) {
		// TODO Auto-generated method stub

	}
	 */


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//posterior supraorbital postfrontal
		//anteriormost teeth/posteriormost teeth
		EntitySearcherOriginal eso = new EntitySearcherOriginal();
		String src = "C:/Users/updates/CharaParserTest/EQ-swartz_FixedGloss/target/final/Swartz 2012.xml_states595.xml";
		SAXBuilder builder = new SAXBuilder();
		Document xml = null;
		try {
			xml = builder.build(new File(src));
		} catch (JDOMException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		if(xml!=null){
			Element root = xml.getRootElement();
			String structid ="o560";
			String entityphrase = "proximal tarsal element";
			//String entityphrase = "posterior postfrontal";
			//String entityphrase = "posterior supraorbital postfrontal";
			String elocatorphrase = "";
			String prep = "";
			ArrayList<EntityProposals> eps = eso.searchEntity(root, structid,  entityphrase, elocatorphrase, entityphrase, prep);
			System.out.println("result:");
			for(EntityProposals ep: eps )
				System.out.println(ep.toString());
		}
	}


}
