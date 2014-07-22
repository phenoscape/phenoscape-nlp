/**
 * 
 */
package fna.parsing;

import java.util.Comparator;

/**
 * @author Hong Cui
 *
 */
public class PhraseComparable implements Comparator<String> {

	
	public int compare(String string1, String string2){
		string1 = string1.trim();
		string2 = string2.trim();
		int i = string1.split("\\s+").length;
		int j = string2.split("\\s+").length;
		if(i == j ) return 0;
		else if(i < j) return 1;
		else return -1;		
	}



}
