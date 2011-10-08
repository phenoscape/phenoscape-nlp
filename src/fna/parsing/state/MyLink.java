package fna.parsing.state;

public class MyLink {

	int weight; // should be private for good practice
	int id;
	public MyLink(int weight, int id) {
		this.id = id; // This is defined in the outer class.
		this.weight = weight;
	}
	public int getWeight(){
		return weight;
	}
	public String toString() { // Always good for debugging
		return "E"+id;
	}
	
}