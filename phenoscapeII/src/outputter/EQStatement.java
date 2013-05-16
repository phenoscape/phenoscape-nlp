/**
 * 
 */
package outputter;

import java.io.File;

/**
 * @author updates
 * store and access information about an EQ statement
 */
public class EQStatement {
	String sourceFile;
	String characterId;
	String stateId;
	String description;
	Entity entity;
	Quality quality;
	private String type;
	//Entity relatedEntity;
	

	/**
	 * 
	 */
	public EQStatement() {
	}
	
	public void setSource(String source){
		this.sourceFile = source;
	}
	
	public void setCharacterId(String characterId){
		this.characterId = characterId;
	}
	
	public void setStateId(String stateId){
		this.stateId = stateId;
	}
	
	public void setEntity(Entity entity){
		this.entity = entity;
	}
	
	public void setQuality(Quality quality){
		this.quality = quality;
	}
	
	public void setDescription(String description){
		this.description = description;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	/*public void setRelatedEntity(Entity entity){
		this.relatedEntity = entity;
	}*/
	
	public String getSource(){
		return this.sourceFile;
	}
	public String getCharacterId(){
		return this.characterId;
	}
	
	public String getStateId( ){
		return this.stateId  ;
	}
	
	public Entity getEntity( ){
		return this.entity  ;
	}
	
	public Quality getQuality( ){
		return this.quality  ;
	}
	
	public String getDescription(){
		return this.description;
	}
	
	public String getType() {
		// TODO Auto-generated method stub
		return this.type;
	}

	/*public Entity getRelatedEntity( ){
		return this.relatedEntity  ;
	}*/
	
	public float calculateConfidenceScore(){
		//TODO
		return (float) 0.0;
	}
	
	public String toString(){
		
		StringBuffer sb = new StringBuffer();
		sb.append("text:"+this.description+System.getProperty("line.separator"));
		sb.append("Entity:"+ entity.toString()+System.getProperty("line.separator"));
		sb.append("Quality:"+(quality!=null?quality.toString():"")+System.getProperty("line.separator"));//Ternary operator to check quality null values => Hariharan
		return sb.toString();
		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}




}
