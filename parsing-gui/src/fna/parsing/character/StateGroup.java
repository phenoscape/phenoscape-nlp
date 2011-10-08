package fna.parsing.character;

import java.util.*;

@SuppressWarnings("unchecked")
public class StateGroup {
	private ArrayList list = null;
	private int count = 1;
	
	public StateGroup() {
		this.list = new ArrayList();
	}
	
	public void addState(State s){
		list.add(s);
	}
	
	public State getState(int i){
		return (State)list.get(i);
	}
	
	public int getIndex(State state){
		for(int i = 0; i<list.size(); i++){
			if(list.get(i).toString().compareTo(state.toString()) == 0){
				return i;
			}
		}
		return -1;
	}
	
	public int size(){
		return list == null? 0 : list.size();
	}
	
	public int numberOfAssociated(){
		int c = 0;
		Iterator it = list.iterator();
		while(it.hasNext()){
			if(((State)it.next()).associatedWithCharacter()){
				c++;
			}
		}
		return c;
	}
	
	public void increment(){
		count++;
	}
	
	public int getCount(){
		return count;
	}
	
	public ArrayList freeStates(){
		ArrayList freestates = new ArrayList();
		Iterator it = list.iterator();
		//int i = 0;
		while(it.hasNext()){
			State s = (State)it.next();
			if(!s.associatedWithCharacter()){
				freestates.add(s);
			}
		}
		return freestates;
	}
	
	public ArrayList nonCategoryStates(String category){
		ArrayList alist = new ArrayList();
		Iterator it = list.iterator();
		//int i = 0;
		while(it.hasNext()){
			State s = (State)it.next();
			if(!s.associatedWithCharacter(category)){
				alist.add(s);
			}
		}
		return alist;
	}
	
	public String mostFreqCategory(){
		ArrayList categories = seenCategories();
		Hashtable counter = new Hashtable();
		Iterator it = categories.iterator();
		while(it.hasNext()){
			String cat = (String)it.next();
			String count = (String) counter.get(cat);
			if(count == null){
				counter.put(cat, "1");
			}else{
				count += "1";
				counter.put(cat, count);
			}
		}
		int max = 0;
		String thecat = "";
		String thecount = "";
		Enumeration en = counter.keys();
		while(en.hasMoreElements()){
			String cat = (String)en.nextElement();
			String count = (String)counter.get(cat);
			if(count.length() > max){
				max = count.length();
				thecat = cat;
				thecount = count;
			}
		}
		return thecat+"#"+thecount;
	}
	
	public ArrayList seenCategories(){
		ArrayList seen = new ArrayList();
		Iterator it = list.iterator();
		//int i = 0;
		while(it.hasNext()){
			State s = (State)it.next();
			seen.addAll(s.getCharacters());
		}
		return seen;
	}
	
	public String toString(){
		String [] names = new String[list.size()];
		Iterator it = list.iterator();
		int i = 0;
		while(it.hasNext()){
			State s = (State)it.next();
			names[i++] = s.toString();
		}
		Arrays.sort(names);
		StringBuffer sb = new StringBuffer();
		for(i = 0; i <names.length; i++){
			sb.append(names[i]+"|");
		}
		return sb.toString();
	}
}
