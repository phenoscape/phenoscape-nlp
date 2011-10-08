package fna.parsing.state;

import java.util.*;
public class StateImporter {
	HashSet<State> states = null;
	
	StateImporter(ArrayList<String> states){
		this.states = new HashSet<State>();
		Iterator<String> it = states.iterator();
		while(it.hasNext()){
			this.states.add(new State((String)it.next()));
		}
	}
	
	@SuppressWarnings("unchecked")
	public Set getStates(){
		return states;
	}
}
