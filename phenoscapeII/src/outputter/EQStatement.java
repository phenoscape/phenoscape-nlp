/**
 * 
 */
package outputter;

/**
 * @author updates
 * store and access information about an EQ statement
 */
public class EQStatement {
	String characterSource;
	String stateSource;
	Entity entity;
	Quality quality;
	Entity relatedEntity;
	

	/**
	 * 
	 */
	public EQStatement() {
	}

	public void setCharacterSource(String characterSource){
		this.characterSource = characterSource;
	}
	
	public void setStateSource(String stateSource){
		this.stateSource = stateSource;
	}
	
	public void setEntity(Entity entity){
		this.entity = entity;
	}
	
	public void setQuality(Quality quality){
		this.quality = quality;
	}
	
	public void setRelatedEntity(Entity entity){
		this.relatedEntity = entity;
	}
	
	public String getCharacterSource(){
		return this.characterSource;
	}
	
	public String getStateSource( ){
		return this.stateSource  ;
	}
	
	public Entity getEntity( ){
		return this.entity  ;
	}
	
	public Quality getQuality( ){
		return this.quality  ;
	}
	
	public Entity getRelatedEntity( ){
		return this.relatedEntity  ;
	}
	
	public float calculateConfidenceScore(){
		//TODO
		return (float) 0.0;
	}
	
	public String toString(){
		//TODO
		return "";
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
