/**
 * 
 */
package outputter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;


/**
 * @author hong cui
 *  accepts entity (entity, entity locator, quality modifier) and quality phrases and search them in ontologies.
 *  this class generates various forms of search phrases to try to hit a target in the ontologies 
 * 	apply stemming?   
 *  rank candidate matches
 *
 */
public class TermSearcher {
	private static final Logger LOGGER = Logger.getLogger(TermSearcher.class);   
	/** The strong entity id cache. */
	public static Hashtable<String, ArrayList<FormalConcept>> entityIDCache = new Hashtable<String, ArrayList<FormalConcept>>(); //term=> {id, label}

	/** The strong quality id cache. */
	public static Hashtable<String, ArrayList<FormalConcept>> qualityIDCache = new Hashtable<String, ArrayList<FormalConcept>>();
	
	/** The candidate entity id cache. */
	public static Hashtable<String, ArrayList<FormalConcept>> entityCandidateIDCache = new Hashtable<String, ArrayList<FormalConcept>>(); //term=> {id, label}

	/** The candidate quality id cache. */
	public static Hashtable<String, ArrayList<FormalConcept>> qualityCandidateIDCache = new Hashtable<String, ArrayList<FormalConcept>>();

	/** The entity id cache. */
	public static Hashtable<String, ArrayList<FormalConcept>> regexpEntityIDCache = new Hashtable<String, ArrayList<FormalConcept>>(); //term=> {id, label}

	/** The quality id cache. */
	public static Hashtable<String, ArrayList<FormalConcept>> regexpQualityIDCache = new Hashtable<String, ArrayList<FormalConcept>>();
	private static Pattern p = Pattern.compile("^("+Dictionary.spatialtermptn+")(\\b.*)");

	ArrayList<Hashtable<String, String>> candidatematches = new ArrayList<Hashtable<String, String>> (); 


	/**
	 * Search term in the whole ontology (of a particular type)
	 * preopercular latero-sensory canal =>	preopercular sensory canal
	 * pectoral-fin spine => pectoral fin spine.
	 *
	 * @param phrase the term
	 * @param phrasetype the type
	 * @return null or FormalConcept [a 4-key hashtable: term, querytype, id, label.]
	 * @throws Exception the exception
	 */
	public ArrayList<FormalConcept> searchTerm(String phrase, String phrasetype){
		String phrasecopy;
		if(phrase.trim().length()==0) return null;
		//the first strong match based on the original phrase is returned right away. Other matches are saved in candidate matches
		//strong match = a match to a class lable or an exact synonym
		phrase = format(phrase);
		phrasecopy=phrase;
		//FormalConcept result = searchCache(phrase, phrasetype);
		//if(result!=null) return result;

		//search ontologies
		//one result = 4-element array: querytype[qualty|entity], id, label, matchtype[original|exact|narrow|related]
		//one result from each ontology that has at least some type of hit
		//result from each ontology is either a match to original class, or via exact, narrow, or related synonyms, the first match is returned.
		//0. special cases
		if(phrasetype.compareTo("entity")==0){
			//'process' => 'anatomical projection' UBERON:0004529
			if(phrase.compareTo("process")==0){
				SimpleEntity se = new SimpleEntity();
				se.setClassIRI("http://purl.obolibrary.org/obo/UBERON_0004529");
				se.setConfidenceScore(1f);
				se.setId("UBERON:0004529");
				se.setLabel("anatomical projection");
				se.setString(phrase);
				ArrayList<FormalConcept> result = new ArrayList<FormalConcept>();
				result.add(se);
				return result;
			}
		}

		//1. search the original phrase
		ArrayList<Hashtable<String, String>> results = new ArrayList<Hashtable<String, String>>();

		ArrayList<FormalConcept> strongmatch = getStrongMatch(phrase, phrasetype, results, 1f);
		if(strongmatch != null) return strongmatch;
		///if landed here, all matches based on the original phrase are weak matches.
		candidatematches.addAll(results);
		results = new ArrayList<Hashtable<String, String>>();

		/* TODO
		 * Changed by Zilong: deal with terms like "unossified" 
		 * Transform the result from an adjective word (binary form) to "noun+present/absent" 
		 * */
		/*quality = quality.toLowerCase().trim();
		if(dictionary.verbalizednouns.containsKey(quality)){
			EQ.put("entity", entity+" "+dictionary.verbalizednouns.get(quality).split(",")[0]);
			EQ.put("quality", dictionary.verbalizednouns.get(quality).split(",")[1]);
		}*/
		/*end handling the "unossified" like term*/

		//2. dorsal portion => dorsal region
		/*if(phrasetype.compareTo("entity")==0){
			Matcher m = p.matcher(phrase);//term = dorsal portion
			String spatials = "";
			boolean trimed = false;

			while(m.matches()){
				spatials += m.group(1)+" ";
				phrase = m.group(2).trim();
				trimed = true;
				m = p.matcher(phrase);
			}*/
		//spatials = dorsal ; phrase = portion,, spatials distal; phrase = end
		//TODO: check: spatialMaps function is replaced by SpatialModifiedEntityStrategy.synVariation
		/*String repl = Dictionary.spatialMaps.get(phrase);
			if(trimed && repl!=null){
				phrase=spatials+repl; //repl = region, newTerm = dorsal region

				strongmatch = getStrongMatch(phrase, phrasetype, results, 0.8f);
				if(strongmatch != null) return strongmatch;

				//if landed here, all matches based on this spatial reform are weak matches.
				candidatematches.addAll(results);
				results = new ArrayList<Hashtable<String, String>>();
			}*/
		/*phrase = phrasecopy;
		}*/

		//TODO let ontoutil.searchOntologies handle variations in hyphens as this case can be mixed with any other cases
		//3. phrase with hyphens, replace hyphens with spaces
		if(phrase.indexOf("-")>0){ //caudal-fin
			phrase = phrase.replaceAll("-", " ");
			strongmatch = getStrongMatch(phrase, phrasetype, results, 1f);
			if(strongmatch != null) return strongmatch;

			//TODO: latero-sensory => sensory
			//if landed here, all matches based on this spatial reform are weak matches.
			candidatematches.addAll(results);
			results = new ArrayList<Hashtable<String, String>>();
			phrase = phrasecopy;
		}		

		//4. phrase with /, assuming one / in the phrase.
		if(phrase.indexOf("/")>0){ //xyz bone/tendon
			String replacement = phrase.substring(phrase.indexOf("/")).replaceFirst("^/", ""); //tendon
			String firstpart = phrase.substring(0, phrase.indexOf("/")); //xyz bone

			strongmatch = getStrongMatch(firstpart, phrasetype, results, 1f);
			if(strongmatch != null) return strongmatch;

			//if landed here, all matches based on this reform are weak matches.
			candidatematches.addAll(results);
			results = new ArrayList<Hashtable<String, String>>();

			while(firstpart.contains(" ")){
				phrase = firstpart.replaceFirst("\\s\\S+$", replacement);//replace the last word in firstpart with "replacement": now term = xyz tendon
				strongmatch = getStrongMatch(phrase, phrasetype, results, 0.8f);
				if(strongmatch != null) return strongmatch;

				//if landed here, all matches based on this spatial reform are weak matches.
				candidatematches.addAll(results);
				results = new ArrayList<Hashtable<String, String>>();
				firstpart = firstpart.substring(0, firstpart.lastIndexOf(" ")).trim();
			}
		}

		//convert to relational adjectives by appending ed|-shaped|-like|less etc.
		if(phrasetype.compareTo("quality")==0)
		{
			//TODO: Handle cases like divergent from => ending with a preposition

			LinkedHashSet<String> phraseforms = Wordforms.toAdjective(phrase);
			//Uses wordforms class to get all the adjectives of this quality
			for(String form:phraseforms)
			{
				//to match predefined quality mapping
				if(Dictionary.qualitymapping.get(form)!=null)
					form=Dictionary.qualitymapping.get(form);
				strongmatch = getStrongMatch(form, phrasetype, results, 0.8f);
				if(strongmatch != null) return strongmatch;
				candidatematches.addAll(results);
				results = new ArrayList<Hashtable<String, String>>();
			}
		}
		//5.shrinking: should all put in candidatematches, stop shrinking when one match is found, stop shrinking when a spatial term becomes the last word in the phrase
		//shrinking from the end of the phrase forward. if a phrase start with spatial terms, shrinking from both ends. 
		//"humeral deltopectoral crest apex" => "humeral deltopectoral crest"
		//"crest" => "process"
		if(phrasetype.compareTo("entity")==0){
			String[] tokens = phrase.split("\\s+");
			int left = 0;
			int right = tokens.length-1;
			//TODO: need to save the spatial terms that are removed to create compound entity
			//s1 s2 e1 e2 e3 => s1 s
			// Utilities.join(tokens, tokens.length-2, tokens.length-1, " ")
		}

		//lastly, rank candidate matches and select the most likely one
		//TODO
		/*if(this.candidatematches.size()>0)
		{

			return candidateMataches(phrase, this.candidatematches, phrasetype,.5f);
		}
		else*/
		cacheCandidateMataches(phrase, phrasetype,.5f);
		return null;
	}

	private void cacheCandidateMataches(String term, String type, float confscore)
	{
		ArrayList<FormalConcept> concepts = null;
		for(Hashtable<String, String> aresult : candidatematches){
			if(aresult.get("matchtype").contains("related")){
				ArrayList<Hashtable<String, String>> resultlist = split(aresult);
				for(Hashtable<String, String> result: resultlist){
					if(type.compareTo("entity")==0){
						SimpleEntity entity = new SimpleEntity();
						entity.setString(result.get("term"));
						entity.setLabel(result.get("label"));
						entity.setId(result.get("id"));
						entity.setClassIRI(result.get("iri"));
						entity.setConfidenceScore(confscore);
						//cacheIt(aresult.get("term"), entity, type);
						//return entity;
						if(concepts == null) concepts = new ArrayList<FormalConcept>();
						concepts.add(entity);
					}else{
						Quality quality = new Quality();
						quality.setString(result.get("term"));
						quality.setLabel(result.get("label").split(";")[0]);
						quality.setId(result.get("id").split(";")[0]);
						quality.setClassIRI(result.get("iri").split(";")[0]);
						quality.setConfidenceScore(confscore);
						//cacheIt(aresult.get("term"), quality, type);
						//return quality;
						if(concepts == null) concepts = new ArrayList<FormalConcept>();
						concepts.add(quality);
					}		
				} 
			}	
		}
		if(concepts!=null) cacheCandidates(term, concepts, type);
	}

	private static String format(String word) {
		word = word.replaceAll("_", " "); // abc_1
		word = word.replaceAll("(?<=\\w)- (?=\\w)", "-"); // dorsal- fin
		//word = word.replaceAll("\\[.*?\\]", "");//remove [usually]
		//word = word.replaceAll("[()]", ""); // turn dorsal-(fin) to dorsal-fin: fix it early, not here because search word may be regular expressions
		word = word.replaceAll("-to\\b", " to"); // turn dorsal-to to dorsal to
		word = word.replaceAll("(?<=\\w)shaped", "-shaped");
		if(word.compareTo("elongate")==0) word = "elongated";
		if(word.compareTo("directed")==0) word = "direction";
		return word;
	}

	public static String adjectiveOrganSearch(String term){
		Hashtable<String, String> result = XML2EQ.ontoutil.searchAdjectiveOrgan(term, "entity");
		if(result!=null){
			//return the first match
			//TODO 
			Enumeration<String> en = result.keys();
			while(en.hasMoreElements()){
				String id = en.nextElement();
				return id+"#"+result.get(id);
			}
		}
		return null;
	}




	/**
	 * fill in results, return strong match if there is any
	 * @param term
	 * @param type
	 * @param results
	 * @return 4-key hashtable: term, querytype, id, label, including only original and exac synonym matches;  match via related synonyms is not considered strong
	 * @throws Exception
	 */

	private ArrayList<FormalConcept> getStrongMatch(String term, String type, ArrayList<Hashtable<String, String>> results, float confscore) {
		ArrayList<FormalConcept> concepts = null;
		XML2EQ.ontoutil.searchOntologies(term, type, results);
		if(results !=null && results.size() > 0){
			//loop through results to find the closest match
			//return original or exact match
			for(Hashtable<String, String> aresult : results){
				if(aresult.get("matchtype").contains("original") || aresult.get("matchtype").contains("exact")){
					ArrayList<Hashtable<String, String>> resultlist = split(aresult);
					for(Hashtable<String, String> result: resultlist){
						if(type.compareTo("entity")==0){
							SimpleEntity entity = new SimpleEntity();
							entity.setString(result.get("term"));
							entity.setLabel(result.get("label"));
							entity.setId(result.get("id"));
							entity.setClassIRI(result.get("iri"));
							entity.setConfidenceScore(confscore);
							//cacheIt(term, entity, type);
							if(concepts == null) concepts = new ArrayList<FormalConcept>();
							concepts.add(entity);
							//return entity;
						}else{
							Quality quality = new Quality();
							quality.setString(result.get("term"));
							quality.setLabel(result.get("label"));
							quality.setId(result.get("id"));
							quality.setClassIRI(result.get("iri"));
							quality.setConfidenceScore(confscore);
							//cacheIt(term, quality, type);
							if(concepts == null) concepts = new ArrayList<FormalConcept>();
							concepts.add(quality);
							//return quality;
						}		
					}
				} 
			}
			if(concepts!=null) cacheIt(term, concepts, type);
		}
		return concepts;
	}

	/**
	 * 
	 * @param multiplevalues
	 * @return
	 */

	private ArrayList<Hashtable<String, String>> split(
			Hashtable<String, String> multiplevalues) {
		//multiplevalues: keys: term, label, id, iri
		ArrayList<Hashtable<String, String>> splited = new ArrayList<Hashtable<String, String>>();
		String[] terms = multiplevalues.get("term").split(";");
		String[] labels = multiplevalues.get("label").split(";");
		String[] ids = multiplevalues.get("id").split(";");
		String[] iris = multiplevalues.get("iri").split(";");

		if(terms.length == 1 ){
			splited.add(multiplevalues);
		}else{
			for(int i = 0; i < terms.length; i++){
				Hashtable<String, String> one = new Hashtable<String, String>();
				one.put("term", terms[i]);
				one.put("label", labels[i]);
				one.put("id", ids[i]);
				one.put("iri", iris[i]);
				splited.add(one);
			}
		}
		return splited;
	}

	/**
	 * Searches for all combination of spatial and headnoun in owl entity and returns matching entities
	 * @param spatial
	 * @param headnoun
	 * @return
	 */
	/*public static ArrayList<FormalConcept> entityVariationTermSearch(String spatial, String headnoun)
	{
		ArrayList<FormalConcept> matches = new ArrayList<FormalConcept>();

		for(String spatialterm:spatial.split("\\|"))
			for(String nounterm:headnoun.split("\\|"))
			{
				FormalConcept term = new TermSearcher().searchTerm(spatialterm+" "+nounterm, "entity");
				if(term!=null)
					matches.add(term);
			}
		return matches;

	}*/
	/**
	 * search pattern with wildcard "*".
	 * search for "pevlic *", it will return any class with label or syn of "pelvic somthing".
	 * @param term
	 * @param type
	 * @param results
	 * @return all matches 
	 * @throws Exception
	 */

	public static ArrayList<FormalConcept> regexpSearchTerm(String phrase, String phrasetype){
		ArrayList<FormalConcept> result = null;
		if(phrasetype.compareTo("entity")==0){
			result = TermSearcher.regexpEntityIDCache.get(phrase);
		}
		if(phrasetype.compareTo("quality")==0){
			result = TermSearcher.regexpQualityIDCache.get(phrase);
		}		
		if(result !=null ) return result;

		if(phrasetype.compareTo("entity")==0){
			//'process' => 'anatomical projection' UBERON:0004529
			if(phrase.matches("\\W*process(\\|process)+\\W*")){
				SimpleEntity se = new SimpleEntity();
				se.setClassIRI("http://purl.obolibrary.org/obo/UBERON_0004529");
				se.setConfidenceScore(1f);
				se.setId("UBERON:0004529");
				se.setLabel("anatomical projection");
				se.setString(phrase);
				result= new ArrayList<FormalConcept>();
				result.add(se);
				TermSearcher.regexpEntityIDCache.put(phrase, result);
				return result;
			}
		}


		ArrayList<Hashtable<String, String>> searchresult = new ArrayList<Hashtable<String, String>> ();
		XML2EQ.ontoutil.searchOntologies(phrase, phrasetype, searchresult);
		if(searchresult !=null && searchresult.size() > 0){
			result = new ArrayList<FormalConcept>();
			for(Hashtable<String, String> item: searchresult){
				if(phrasetype.compareTo("entity")==0){
					String str = item.get("term");
					String[] labels = item.get("label").split(";");
					String[] ids = item.get("id").split(";");
					String[] iris = item.get("iri").split(";");
					for(int i = 0; i < labels.length; i++){
						SimpleEntity entity = new SimpleEntity();
						entity.setString(str);
						entity.setLabel(labels[i]);
						entity.setId(ids[i]);
						entity.setClassIRI(iris[i]);
						entity.setConfidenceScore((float)0.5);
						result.add(entity);
					}
				}else{
					String str = item.get("term");
					String[] labels = item.get("label").split(";");
					String[] ids = item.get("id").split(";");
					String[] iris = item.get("iri").split(";");
					for(int i = 0; i < labels.length; i++){
						Quality quality = new Quality();
						quality.setString(str);
						quality.setLabel(labels[i]);
						quality.setId(ids[i]);
						quality.setClassIRI(iris[i]);
						quality.setConfidenceScore((float)0.5);
						result.add(quality);
					}
				}		
			}			
			if(phrasetype.compareTo("entity")==0) TermSearcher.regexpEntityIDCache.put(phrase, result);
			if(phrasetype.compareTo("quality")==0) TermSearcher.regexpQualityIDCache.put(phrase, result);
			return result;
		}
		return result;
	}



	/**
	 * search in cache
	 * @param term
	 * @param type
	 * @return
	 */
	private static ArrayList<FormalConcept>  searchCache(String term, String type) {
		ArrayList<FormalConcept> result = null;
		if(type.compareTo("entity")==0){
			result = entityIDCache.get(term);
		}
		if(type.compareTo("quality")==0){
			result = qualityIDCache.get(term);
		}
		return result;
	}

	private static void cacheIt(String term, ArrayList<FormalConcept> aresult, String type) {
		if(type.compareTo("entity")==0) TermSearcher.entityIDCache.put(term, aresult);
		if(type.compareTo("quality")==0) TermSearcher.qualityIDCache.put(term, aresult);
	}

	private static void cacheCandidates(String term, ArrayList<FormalConcept> aresult, String type) {
		if(type.compareTo("entity")==0) TermSearcher.entityCandidateIDCache.put(term, aresult);
		if(type.compareTo("quality")==0) TermSearcher.qualityCandidateIDCache.put(term, aresult);
	}

	public ArrayList<FormalConcept> getCandidateMatches(String term, String type) {
		if(type.compareTo("entity")==0) return TermSearcher.entityCandidateIDCache.get(term);
		else if(type.compareTo("quality")==0) return TermSearcher.qualityCandidateIDCache.get(term);
		return null;
	}
	
	/*public ArrayList<FormalConcept> getQualityCandidateMatches() {
		ArrayList<FormalConcept> fcs = new ArrayList<FormalConcept>();
		for(Hashtable<String, String> aresult: this.candidatematches){
			ArrayList<Hashtable<String, String>> resultlist = split(aresult);
			for(Hashtable<String, String> result: resultlist){
			if(result.get("querytype").compareTo("entity")==0){
				SimpleEntity se = new SimpleEntity();
				se.setConfidenceScore(0.5f); //candidate match is less reliable, will replace hard-coded score with a calculated score
				se.setString(result.get("term"));
				se.setLabel(result.get("label"));
				se.setId(result.get("id"));
				se.setClassIRI(result.get("iri"));
				fcs.add(se);				
			}
			if(result.get("querytype").compareTo("quality")==0){
				Quality q = new Quality();
				q.setConfidenceScore(0.5f); //candidate match is less reliable, will replace hard-coded score with a calculated score
				q.setString(result.get("term"));
				q.setLabel(result.get("label"));
				q.setId(result.get("id"));
				q.setClassIRI(result.get("iri"));
				fcs.add(q);				
			}
			}
		}
		return fcs;
	}*/

	
	/**
	 * @param args
	 */
	public static void main(String[] args) {	

		TermSearcher ts = new TermSearcher();
		//FormalConcept result = ts.searchTerm("ornament", "quality");
		//		ArrayList<FormalConcept> result =TermSearcher.regexpSearchTerm("epichordal\\b.*", "entity");
		//		if(result!=null){
		//			System.out.println(result.toString());
		//		}else{
		//			ArrayList<FormalConcept> fcs = ts.getCandidateMatches();
		//			for(FormalConcept fc: fcs){
		//				System.out.println(fc.toString());
		//			}

		ArrayList<FormalConcept> quality = ts.searchTerm("decreased broad", "quality");
		for(FormalConcept fc : quality)
			System.out.println(fc.getLabel());

	}		



}
