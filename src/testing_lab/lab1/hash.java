package testing_lab.lab1;

import java.util.ArrayList;
import java.util.HashMap;
import fna.parsing.ApplicationUtilities;

public class hash {

	/**
	 * @param args
	 */
	
	public int max ;
	public int min ;
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		HashMap hm = new HashMap();
		HashMap hm1 = new HashMap();
/*		
		hash h = new hash();
		h.max = 10;
		h.min = 11;
		
		hm.put("1", h);
		hm1.put("1", hm.get("1"));
		
		hash h1 = (hash)hm1.get("1");
		h1.max = 5;
		
		hash h2 = (hash)hm.get("1");
		System.out.println(h2.max);*/
/*		
		ArrayList<String> al = new ArrayList<String>();
		al.add("Partha");
		al.add("Pratim");
		al.add("Sanyal");
		
		String [] a = new String [al.size()];
		al.toArray(a);
		
		for (String s : a) {
			System.out.println(s);
		}*/
		
/*		String x = ApplicationUtilities.longestCommonSubstring("XXXX", "YYYYY");
		System.out.println(x.length());*/
		
/*		String [] h = {"AAABBBCCC", "AABBBC", "AABBD", "ABBD", "ABBDEFG"};
		String common = h[0];
		for(int i= 0; i<h.length-1 ; i++) {
			common = ApplicationUtilities.longestCommonSubstring(common, h[i+1]);
		}
		
		System.out.println(common);*/
		
		String s = "";
		String [] h = {"AAABBBCCC", "AABBBC", "AABBD", "ABBD", "ABBDEFG"};
		for (String a : h) {
			s += a + '|';
		}
		System.out.println(s.substring(0, s.lastIndexOf('|')));
		
	}

}
