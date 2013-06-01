package outputter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;


public class Wordforms {

static String adjective_suffixes = "e|ed|ent|-like|like|shaped|-shaped|y|ly|ic|ted|ate|led|oid|ion|ied|ous|ing|form|iform|ally|ation|ure|al|ical|ication";
static Hashtable<String,LinkedHashSet<String>> adjectivecache =new Hashtable<String,LinkedHashSet<String>>();
	
	public static LinkedHashSet<String> toAdjective(String word)
	{
		//if the word is found in cache it returns it else, adjective forms are generated and a copy is stored in cache
		if(adjectivecache.get(word)!=null)
			return adjectivecache.get(word);
			
		String suffix[] = adjective_suffixes.split("\\|");
		ArrayList<String> wordforms = stemforms(word);//Stems the word and return all the stemmed form
		LinkedHashSet<String> forms = new LinkedHashSet<String>();
		forms.add(wordforms.get(0));
		for(String word1:wordforms)
		{
			for(String suf:suffix)
				forms.add(word1.trim()+suf);//add all adjective suffixes to all different stems
		}
		adjectivecache.put(word, forms);
		return forms;
	}
	
	private static ArrayList<String> stemforms(String word) {
		
		String suffix[] = adjective_suffixes.split("\\|");
		ArrayList<String> wordforms = new ArrayList<String>();
		wordforms.add(word);//the original word should be one of the forms
		
		//Remove plural form of the word =>es and ies can also be considered when needed
		if(word.matches(".*s"))
			{
			word=word.substring(0,word.lastIndexOf("s"));
			wordforms.add(word);
			}
		
		
		for(String suf:suffix)
			{
			if((word.lastIndexOf(suf)!=-1)&&(word.matches(".*("+suf+")$")==true))
			wordforms.add(word.substring(0,word.lastIndexOf(suf)));
			}
		
		return wordforms;
	}
	public static void main(String[] args) {
		HashSet<String> forms =  toAdjective("rhomboid");
		
		for(String form:forms)
			{
			if(form.equals("rhombic"))
			System.out.println(form);
			}
	}

}
