/**
 * 
 */
package outputter;

/**
 * @author updates
 *
 */
public class RelationalQuality extends Quality {
	QualityProposals relationalquality; //any relation from PATO relational_slim, or size
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
	
	public EntityProposals getRelatedEntity(){
		return relatedentity;
	}
	
	public void setRelatedEntity(EntityProposals relatedentity){
		this.relatedentity = relatedentity;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
