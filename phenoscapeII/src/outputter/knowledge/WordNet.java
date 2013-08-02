/**
 * 
 */
package outputter.knowledge;

import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISenseEntry;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.morph.WordnetStemmer;

/**
 * @author updates
 *
 */
public class WordNet {
	private static final Logger LOGGER = Logger.getLogger(WordNet.class);   
	// This methods get all the possible synonyms with POS of the word passed.

		public static Hashtable<String,Integer> getallsynonyms(String word) {
			// TODO Auto-generated method stub
			Hashtable<String,Integer> synonyms = new Hashtable<String,Integer>();
			ISenseEntry senseEntry;
			WordnetStemmer stemmer = new WordnetStemmer(Dictionary.wordnetdict);
			if(word.length()>0)
			{
			for(edu.mit.jwi.item.POS pos : edu.mit.jwi.item.POS.values()) {
				
				List<String> stems = stemmer.findStems(word, pos);
				//System.out.println(stems.size());
				for(String stem : stems) {		
					IIndexWord indexWord = Dictionary.wordnetdict.getIndexWord(stem, pos);
					if(indexWord!=null) {
						int count = 0;
						for(IWordID wordId : indexWord.getWordIDs()) {
							//System.out.println(wordId);
							IWord aWord = Dictionary.wordnetdict.getWord(wordId);
							ISynset synset = aWord.getSynset();
							
							for( IWord w : synset.getWords ())
								{
								synonyms.put(w.getLemma().replaceAll("_", " ").trim(),Dictionary.wordnetdict.getSenseEntry(aWord.getSenseKey()).getTagCount());
							//	System.out.println(w.getLemma().replaceAll("_", " ").trim()+ Dictionary.wordnetdict.getSenseEntry(aWord.getSenseKey()).getTagCount());
								}
						
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
		Hashtable<String,Integer> synonyms = getallsynonyms("centrally");
	Set<String> all_syn = synonyms.keySet();
	
	for(String temp:all_syn);
		//System.out.print("");
	
	}

}
