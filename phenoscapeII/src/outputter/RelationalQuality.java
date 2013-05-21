/**
 * 
 */
package outputter;

/**
 * @author updates
 *
 */
public class RelationalQuality extends Quality {
	QualityProposals relationalquality;
	EntityProposals relatedentity; //also called qualitymodifier
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
	/*public RelationalQuality(String string, String label, String id) {
		Quality q = new Quality(string, label, id);
		relationalquality = new QualityProposals();
		relationalquality.add(q);
	}*/

	public RelationalQuality(QualityProposals relationalquality, EntityProposals relatedentity) {
		this.relationalquality = relationalquality;
		this.relatedentity = relatedentity;
		
	}
	public QualityProposals getQuality(){
		return relationalquality;
	}
	
	public void setQuality(QualityProposals relationalquality){
		this.relationalquality = relationalquality;
	}
	
	public EntityProposals getQualityModifier(){
		return relatedentity;
	}
	
	public void setQualityModifier(EntityProposals relatedentity){
		this.relatedentity = relatedentity;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
