package fna.parsing.state;

import java.util.*;

/**
 * simply count the occurrence of different values
 * @author hongcui
 *
 */
public class CooccurrenceScore {
	private Hashtable<Integer, Integer> scores = null; //value->count
	private HashSet<String> sources = new HashSet<String>();

		CooccurrenceScore(){
			scores = new Hashtable<Integer, Integer>();
		}
		
		CooccurrenceScore(int value, String source){
			scores = new Hashtable<Integer, Integer>();
			scores.put(new Integer(value), new Integer(1));
			sources.add(source);
		}
		
		public Set<String> getSources(){
			return sources;
		}
		
		public String getSourcesAsString(){
			Iterator<String> it = sources.iterator();
			StringBuffer sb = new StringBuffer();
			while(it.hasNext()){
				sb.append(it.next()+",");
			}
			sb.replace(sb.lastIndexOf(","), sb.length(), "");
			return sb.toString();
		}
		
		public void updateBy(int value, String source){
			Integer key = new Integer(value);
			Integer count = scores.get(key);
			int newcount = count==null? 1 : count.intValue()+1;
			scores.put(key, new Integer(newcount));
			sources.add(source);
		}
		
		public boolean isEmpty(){
			return scores.size() == 0;
		}
		
		public int valueSum(){
			int sum = 0;
			Enumeration<Integer> en = scores.keys();
			while(en.hasMoreElements()){
				Integer v = en.nextElement();
				Integer c = scores.get(v);
				sum +=v.intValue()*c.intValue();
			}
			if(sum < 0) return 1;
			return sum;
		}
		
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append("[");
			Enumeration<Integer> en = scores.keys();
			while(en.hasMoreElements()){
				Integer v = en.nextElement();
				Integer c = scores.get(v);
				sb.append(v.intValue()+":"+c.intValue()+";");
			}
			if(sb.lastIndexOf(";") > 0){
				sb.replace(sb.lastIndexOf(";"), sb.length(), "]");
			}else{
				sb.append("]");
			}
			
			
			sb.append(" from sources: "+getSourcesAsString());

			return sb.toString();
		}
}
