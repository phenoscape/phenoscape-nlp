package fna.parsing.state;

import java.util.*;

public class State implements Comparator<Object>{
	private String state = null;
	
	State(String state){
		this.state = state;
	}
	
	State(){
		
	}

	public int compare(Object o1, Object o2){
		State s1 = (State)o1;
		State s2 = (State)o2;
		return s1.getName().compareTo(s2.getName());
	}
	
	public boolean equals(Object o){
		State s = (State)o;
		return this.state.equals(s.getName());
	}
	public boolean isEmpty(){
		if(state == null || state.trim().length() == 0){
			return true;
		}
		return false;
	}
	
	
	public void setState(String state){
		this.state = state;
	}
	
	public String getName(){
		return state;
	}
	public String toString(){
		return state;
	}
}
