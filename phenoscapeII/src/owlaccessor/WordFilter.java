package owlaccessor;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import outputter.knowledge.Dictionary;

public class WordFilter {
	private Set<String> wl;
	
	public WordFilter(){
		wl = new HashSet<String>();
		wl.addAll(createSubset(Dictionary.STOP));
		wl.addAll(createSubset(Dictionary.NUMBERS));
		wl.addAll(createSubset(Dictionary.FORBIDDEN));
		wl.addAll(createSubset(Dictionary.PRONOUN));
		wl.addAll(createSubset(Dictionary.CHARACTER));
		wl.addAll(createSubset(Dictionary.PROPOSITION));
		wl.addAll(createSubset(Dictionary.CLUSTER));
		wl.addAll(createSubset(Dictionary.SUBSTRUCTURE));
		wl.addAll(createSubset(Dictionary.ADDITIONAL));
		wl.add("bearer");
		wl.add("bearer's");
		wl.add("inhering");
		wl.add("virtue");
	}
	
	/*
	 * s contains words to be eliminated separated by space
	 * 
	 * */
	private Set<String> createSubset(String s){
		Set<String> result = new HashSet<String>();
		StringTokenizer st = new StringTokenizer(s, "|");
		while (st.hasMoreTokens()){
			result.add(st.nextToken().trim());
		}
		return result;
	}
	
	public boolean isInList(String s){
		if(wl.contains(s.trim().toLowerCase()))
			return true;
		else
			return false;
	}
}
