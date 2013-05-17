package owlaccessor;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class WordFilter {
	static final public String STOP = "a|about|above|across|after|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|for|from|had|has|have|hence|here|how|if|in|into|inside|inward|is|it|its|may|might|more|most|near|no|not|of|off|on|onto|or|out|outside|outward|over|should|so|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|was|were|what|when|where|which|why|with|within|without|would|yet|etc";
	static final public String NUMBERS = "zero|one|ones|first|two|second|three|third|thirds|four|fourth|fourths|quarter|five|fifth|fifths|six|sixth|sixths|seven|seventh|sevenths|eight|eighths|eighth|nine|ninths|ninth|tenths|tenth";
	static final public String FORBIDDEN ="to|and|or|nor"; //words in this list can not be treated as boundaries "to|a|b" etc.
	static final public String PRONOUN ="all|each|every|some|few|individual|both|other|another|either|neither";
	static final public String CHARACTER ="lengths|length|lengthed|width|widths|widthed|heights|height|character|characters|distribution|distributions|outline|outlines|profile|profiles|feature|features|form|forms|mechanism|mechanisms|nature|natures|shape|shapes|shaped|size|sizes|sized";//remove growth, for growth line. check 207, 3971
	static final public String PROPOSITION ="above|across|after|along|around|as|at|before|beneath|between|beyond|by|for|from|in|into|near|of|off|on|onto|out|outside|over|than|throughout|toward|towards|up|upward|with|without";
	static final public String CLUSTER = "group|groups|clusters|cluster|arrays|array|series|fascicles|fascicle|pairs|pair|rows|number|numbers|\\d+";
	static final public String SUBSTRUCTURE = "part|parts|area|areas|portion|portions";
	static final public String ADDITIONAL = "bearer|entity|bearer's|bearers'|entities|inhering|inheres|inhere|virtue|quality|having|exhibiting";
	private Set<String> wl;
	
	public WordFilter(){
		wl = new HashSet<String>();
		wl.addAll(createSubset(STOP));
		wl.addAll(createSubset(NUMBERS));
		wl.addAll(createSubset(FORBIDDEN));
		wl.addAll(createSubset(PRONOUN));
		wl.addAll(createSubset(CHARACTER));
		wl.addAll(createSubset(PROPOSITION));
		wl.addAll(createSubset(CLUSTER));
		wl.addAll(createSubset(SUBSTRUCTURE));
		wl.addAll(createSubset(ADDITIONAL));
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
