/**
 * 
 */
package outputter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import conceptmapping.TermOutputerUtilities;

/**
 * @author hong cui
 *  accepts entity (entity, entity locator, quality modifier) and quality phrases and search them in ontologies.
 *  this class generates various forms of search phrases to try to hit a target in the ontologies 
 * 	apply stemming?   
 *  rank candidate matches
 *
 */
public class TermSearcher {

	/** The entity id cache. */
	public static Hashtable<String, Hashtable<String, String>> entityIDCache = new Hashtable<String, Hashtable<String, String>>(); //term=> {id, label}
	
	/** The quality id cache. */
	public static Hashtable<String, Hashtable<String, String>> qualityIDCache = new Hashtable<String, Hashtable<String, String>>();
	
	/** The entity id cache. */
	public static Hashtable<String, ArrayList<Hashtable<String, String>>> regexpEntityIDCache = new Hashtable<String, ArrayList<Hashtable<String, String>>>(); //term=> {id, label}
	
	/** The quality id cache. */
	public static Hashtable<String, ArrayList<Hashtable<String, String>>> regexpQualityIDCache = new Hashtable<String, ArrayList<Hashtable<String, String>>>();
	private static Pattern p;
	
	private Dictionary dict;
	
	
	/**
	 * 
	 */
	public TermSearcher(Dictionary dict) {
		this.dict = dict;	
		p = Pattern.compile("^("+dict.spatialtermptn+")(\\b.*)");
	}

	/**
	 * Search term in the whole ontology (of a particular type)
	 * preopercular latero-sensory canal =>	preopercular sensory canal
	 * pectoral-fin spine => pectoral fin spine.
	 *
	 * @param phrase the term
	 * @param phrasetype the type
	 * @return null or a 4-key hashtable: term, querytype, id, label.
	 * @throws Exception the exception
	 */
	public Hashtable<String, String> searchTerm(String phrase, String phrasetype, int ingroup){
		if(phrase.trim().length()==0) return null;
		//the first strong match based on the original phrase is returned right away. Other matches are saved in candidate matches
		//strong match = a match to a class lable or an exact synonym
		ArrayList<Hashtable<String, String>> candidatematches = new ArrayList<Hashtable<String, String>> (); 
		
		Hashtable<String, String> result = searchCache(phrase, phrasetype);
		if(result!=null) return result;
		
		phrase = format(phrase);
		//search ontologies
		//one result = 4-element array: querytype[qualty|entity], id, label, matchtype[original|exact|narrow|related]
		//one result from each ontology that has at least some type of hit
		//result from each ontology is either a match to original class, or via exact, narrow, or related synonyms, the first match is returned.
		
		//1. search the original phrase
		ArrayList<Hashtable<String, String>> results = new ArrayList<Hashtable<String, String>>();
		Hashtable<String, String> strongmatch = getStrongMatch(phrase, phrasetype, results, ingroup);
		if(strongmatch != null) return strongmatch;
		
		//if landed here, all matches based on the original phrase are weak matches.
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
		if(phrasetype.compareTo("entity")==0){
			String phrasecopy = phrase;
			Matcher m = p.matcher(phrase);//term = dorsal portion
			String spatials = "";
			boolean trimed = false;
			
			while(m.matches()){
				spatials += m.group(1)+" ";
				phrase = m.group(2).trim();
				trimed = true;
				m = p.matcher(phrase);
			}
			//spatials = dorsal ; phrase = portion
			String repl = dict.spatialMaps.get(phrase);
			if(trimed && repl!=null){
				phrase=spatials+repl; //repl = region, newTerm = dorsal region
			
				strongmatch = getStrongMatch(phrase, phrasetype, results, ingroup);
				if(strongmatch != null) return strongmatch;
				
				//if landed here, all matches based on this spatial reform are weak matches.
				candidatematches.addAll(results);
				results = new ArrayList<Hashtable<String, String>>();
			}
			phrase = phrasecopy;
		}
		
		//TODO let ontoutil.searchOntologies handle variations in hyphens as this case can be mixed with any other cases
		//3. phrase with hyphens, replace hyphens with spaces
		if(phrase.indexOf("-")>0){ //caudal-fin
			phrase = phrase.replaceAll("-", " ");
			strongmatch = getStrongMatch(phrase, phrasetype, results, ingroup);
			if(strongmatch != null) return strongmatch;
			
			//TODO: latero-sensory => sensory
			//if landed here, all matches based on this spatial reform are weak matches.
			candidatematches.addAll(results);
			results = new ArrayList<Hashtable<String, String>>();
		}		
		
		//4. phrase with /, assuming one / in the phrase.
		if(phrase.indexOf("/")>0){ //xyz bone/tendon
			String replacement = phrase.substring(phrase.indexOf("/")).replaceFirst("^/", ""); //tendon
			String firstpart = phrase.substring(0, phrase.indexOf("/")); //xyz bone
			
			strongmatch = getStrongMatch(firstpart, phrasetype, results, ingroup);
			if(strongmatch != null) return strongmatch;
			
			//if landed here, all matches based on this reform are weak matches.
			candidatematches.addAll(results);
			results = new ArrayList<Hashtable<String, String>>();
			
			while(firstpart.contains(" ")){
				phrase = firstpart.replaceFirst("\\s\\S+$", replacement);//replace the last word in firstpart with "replacement": now term = xyz tendon
				strongmatch = getStrongMatch(phrase, phrasetype, results, ingroup);
				if(strongmatch != null) return strongmatch;
				
				//if landed here, all matches based on this spatial reform are weak matches.
				candidatematches.addAll(results);
				results = new ArrayList<Hashtable<String, String>>();
				firstpart = firstpart.substring(0, firstpart.lastIndexOf(" ")).trim();
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
		return null;
	}
	
	private String format(String word) {
		word = word.replaceAll("_", " "); // abc_1
		word = word.replaceAll("(?<=\\w)- (?=\\w)", "-"); // dorsal- fin
		// word = word.replaceAll("\\[.*?\\]", "");//remove [usually]
		word = word.replaceAll("[()]", ""); // turn dorsal-(fin) to dorsal-fin
		word = word.replaceAll("-to\\b", " to"); // turn dorsal-to to dorsal to
		return word;
	}
	
	public String adjectiveOrganSearch(String term){
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

	private Hashtable<String, String> getStrongMatch(String term, String type, ArrayList<Hashtable<String, String>> results, int ingroup) {
		results = XML2EQ.ontoutil.searchOntologies(term, type, ingroup);
		if(results !=null && results.size() > 0){
			//loop through results to find the closest match
			//return original or exact match
			for(Hashtable<String, String> aresult : results){
				if(aresult.get("matchtype").contains("original") || aresult.get("matchtype").contains("exact")){
					cacheIt(term, aresult, type);
					return aresult;				
				} 
			}			
		}
		return null;
	}
	
	/**
	 * 
	 * @param term
	 * @param type
	 * @param results
	 * @return all matches 
	 * @throws Exception
	 */

	public ArrayList<Hashtable<String, String>> regexpSearchTerm(String phrase, String phrasetype, int ingroup){
		ArrayList<Hashtable<String, String>> result = null;
		if(phrasetype.compareTo("entity")==0){
			 result = TermSearcher.regexpEntityIDCache.get(phrase);
		}
		if(phrasetype.compareTo("quality")==0){
			result = TermSearcher.regexpQualityIDCache.get(phrase);
		}		
		if(result !=null ) return result;
		result = XML2EQ.ontoutil.searchOntologies(phrase, phrasetype, ingroup);
		if(result !=null && result.size() > 0){	
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
	private Hashtable<String, String>  searchCache(String term, String type) {
		Hashtable<String, String> result = null;
		if(type.compareTo("entity")==0){
			 result = this.entityIDCache.get(term);
		}
		if(type.compareTo("quality")==0){
			result = this.qualityIDCache.get(term);
		}
		return result;
	}

	private void cacheIt(String term, Hashtable<String, String> aresult, String type) {
		if(type.compareTo("entity")==0) TermSearcher.entityIDCache.put(term, aresult);
		if(type.compareTo("quality")==0) TermSearcher.qualityIDCache.put(term, aresult);
	}


	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		TermSearcher ts = new TermSearcher(new Dictionary());
		Hashtable<String, String> result = ts.searchTerm("regular", "quality", 0);
		if(result !=null){
			Enumeration<String> en = result.keys();
			while(en.hasMoreElements()){
				String key = en.nextElement();
				System.out.println(key+"="+result.get(key));
			}
		}
	}

}
