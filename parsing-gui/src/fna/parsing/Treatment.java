package fna.parsing;

public class Treatment {

	private String description; //character or state description text
	private String fileName; //file name where the description is found
	private String type; //character or state

	public Treatment(String fn, String des, String type){
		this.fileName = fn;
		this.description=des;
		this.type = type;
		
	}

	public String getDescription() {
		return this.description;
	}

	public String getFileName() {
		return this.fileName;
	}
	
	public String getType(){
		return this.type;
	}

}