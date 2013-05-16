/**
 * 
 */
package outputter;

/**
 * @author updates
 *
 */
public class RelationalQuality extends Quality {
	Quality relationalquality;
	Entity relatedentity; //also called qualitymodifier
	/**
	 * 
	 */
	public RelationalQuality() {
	}

	/**
	 * @param string
	 * @param label
	 * @param id
	 * @param iri
	 */
	public RelationalQuality(String string, String label, String id) {
		relationalquality = new Quality(string, label, id);
	}

	public RelationalQuality(Quality relationalquality, Entity qualitymodifier) {
		this.relationalquality = relationalquality;
		this.relatedentity = qualitymodifier;
	}
	public Quality getQuality(){
		return relationalquality;
	}
	
	public void setQualityModifier(Quality relationalquality){
		this.relationalquality = relationalquality;
	}
	
	public Entity getQualityModifier(){
		return relatedentity;
	}
	
	public void setQualityModifier(Entity qualitymodifier){
		this.relatedentity = qualitymodifier;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
