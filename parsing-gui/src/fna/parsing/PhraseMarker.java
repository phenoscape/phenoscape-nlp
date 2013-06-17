/**
 * 
 */
package fna.parsing;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fna.parsing.ApplicationUtilities;
import fna.parsing.ParsingException;
import fna.parsing.TaxonIndexer;

/**
 * @author Hong Cui
 * convert a sentence to a format so the phrases matching terms in a knowledge source such as an ontology are marked.
 * A phrase is represented by words connected using "_"
 *
 */
public class PhraseMarker {
	private Pattern phrasepattern; //"dorsal_fin|leaf_blade"
	private String phrasestr;
	
	public ArrayList<String> phrases;
	//private static final String BIN_FILE = "PO_phrases.bin";
	/**
	 * @param termsource: filepath to the serialized arraylist, holding the phrases 
	 */
	@SuppressWarnings("unchecked")
	public PhraseMarker(String termsourcepath) {
		try {
			File file = new File(termsourcepath);
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					file));
			// Deserialize the object
		    phrases = (ArrayList<String>) in.readObject(); //phrases are words connected with "_"
			in.close();			
			Collections.sort(phrases, new PhraseComparable()); //longest phrases first
			phrasestr = "";
			for(String phrase: phrases){
				//hyomandibula-opercle_joint
				phrase = phrase.replaceAll("\\([^)]*\\)", "").trim();
				if(phrase.length()>0)
					phrase = phrase.replaceAll("-", "_");//hyomandibula_opercle_joint
					phrasestr += phrase+"|";
			}
			//phrasestr="";
			phrasestr = phrasestr.replaceFirst("\\|$", ""); //space separated words in phrases
			this.phrasepattern =Pattern.compile("(.*?\\b)("+phrasestr+")(\\b.*)");
		} catch (Exception e) {
			//LOGGER.error("Load the updated TaxonIndexer failed.", e);
			//StringWriter sw = new StringWriter();PrintWriter pw = new PrintWriter(sw);e.printStackTrace(pw);LOGGER.error(ApplicationUtilities.getProperty("CharaParser.version")+System.getProperty("line.separator")+sw.toString());
			e.printStackTrace();
		}
	}

	/**
	 * @param sentence : leaf blade rounded
	 * @return leaf_blade rounded
	 */
	public String markPhrases(String sentence){
		Matcher m = phrasepattern.matcher(sentence);
		//System.out.println(this.phrasestr);
		while(m.matches()){
			sentence = m.group(1)+m.group(2).replaceAll("\\s+", "_")+m.group(3);
			m = phrasepattern.matcher(sentence);
		}				
		return sentence;
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		long time1 = System.currentTimeMillis();
		String termpath = "C:/Users/updates/CharaParserTest/Ontologies/ext_terms.bin";
		PhraseMarker pm = new PhraseMarker(termpath);
		long time2 = System.currentTimeMillis();
		System.out.println("read term list took "+(time2-time1)+" ms");
		System.out.println(pm.markPhrases("hyomandibula-opercle joint and anal fin absent . female gonad present."));
		long time3 = System.currentTimeMillis();
		System.out.println("mark the sentence took "+(time3-time2)+" ms");
	}
}
