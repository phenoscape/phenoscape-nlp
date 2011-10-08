package fna.parsing.character;

import java.util.*;

@SuppressWarnings("unchecked")
public class State {
	private String term = null;
	@SuppressWarnings("unused")
	private ArrayList characters = null;
	private Glossary glossary = null;
	//may consider adding constraints.
	
	public State(String term, Glossary glossary) {
	//public State(String term) {
		this.term = term;
		this.characters = new ArrayList();
		this.glossary = glossary;
	}
	
	/*public State(String term, ArrayList characters){
		this.term = term;
		this.characters = characters;		
	}*/
	
	/*public void addCharacter(ArrayList characters){
			this.characters.addAll(characters);
	}	*/
	
	@SuppressWarnings("static-access")
	public ArrayList getCharacters(){
		return glossary.getCharacter(term);
	}
	
	public boolean associatedWithCharacter(){
		return getCharacters().size() != 0;
	}
	
	public boolean associatedWithCharacter(String category){
		return getCharacters().contains(category);
	}
	
	public String toString(){
		return term;
	}
}
