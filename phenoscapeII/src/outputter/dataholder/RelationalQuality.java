/**
 * 
 */
package outputter.dataholder;


/**
 * @author updates
 *
 */
public class RelationalQuality extends Quality {
	private QualityProposals relationalquality; //any relation from PATO relational_slim, or size
	private EntityProposals relatedentity; //also called qualitymodifier
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
		this.setRelationalquality(relationalquality);
		this.setRelatedentity(relatedentity);
		
	}
	public QualityProposals getQuality(){
		return getRelationalquality();
	}
	
	public void setQuality(QualityProposals relationalquality){
		this.setRelationalquality(relationalquality);
	}
	
	public EntityProposals getRelatedEntity(){
		return getRelatedentity();
	}
	
	public void setRelatedEntity(EntityProposals relatedentity){
		this.setRelatedentity(relatedentity);
	}
	
	public String toString(){
		return "quality="+this.getRelationalquality().toString()+" related entity="+this.getRelatedentity().toString();
	}
	
	public String getString()
	{
		String quality="";
		for(Quality q:this.getRelationalquality().getProposals())
		{
			if((q instanceof CompositeQuality))
			{
				quality+=((CompositeQuality)q).getFullString()+",";
			}else if((q instanceof NegatedQuality))
			{
				quality+=((NegatedQuality)q).getFullString()+",";
			}else if(q instanceof RelationalQuality)
			{
				quality+=q.getString()+",";
			}
			else
			{
				quality+=q.getString();
			}
			
		}
		return quality.replaceAll(",$", "");
	}
	
	public String getId()
	{
		String id="";
		for(Quality q:this.getRelationalquality().getProposals())
		{
			if((q instanceof CompositeQuality))
			{
				id+=((CompositeQuality)q).getFullId()+",";
			}else if((q instanceof NegatedQuality))
			{
				id+=((NegatedQuality)q).getFullId()+",";
			}else if(q instanceof RelationalQuality)
			{
				id+=q.getId()+",";
			}
			else
			{
				id+=q.getId();
			}
			
		}
		return id.replaceAll(",$", "");
	}
	
	public String getLabel()
	{

		String label="";
		for(Quality q:this.getRelationalquality().getProposals())
		{
			if((q instanceof CompositeQuality))
			{
				label+=((CompositeQuality)q).getFullLabel()+",";
			}else if((q instanceof NegatedQuality))
			{
				label+=((NegatedQuality)q).getFullLabel()+",";
			}else if(q instanceof RelationalQuality)
			{
				label+=q.getLabel()+",";
			}
			else
			{
				label+=q.getLabel();
			}
			
		}
		return label.replaceAll(",$", "");
		
	}
	
	
	@Override
	public float getConfidienceScore() {
		
		float score=1.0f;
		
		for(Quality q:this.getRelationalquality().getProposals())
		{
			score*=q.getConfidienceScore();
		}
/*		for(Entity e:this.relatedentity.getProposals())
		{
			score*=e.getConfidienceScore();
		}*///Not needed as per discussion with Hong
		return score;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public EntityProposals getRelatedentity() {
		return relatedentity;
	}

	public void setRelatedentity(EntityProposals relatedentity) {
		this.relatedentity = relatedentity;
	}

	public QualityProposals getRelationalquality() {
		return relationalquality;
	}

	public void setRelationalquality(QualityProposals relationalquality) {
		this.relationalquality = relationalquality;
	}

}
