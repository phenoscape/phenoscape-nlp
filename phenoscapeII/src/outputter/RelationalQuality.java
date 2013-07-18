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
	
	public String toString(){
		return "quality="+this.relationalquality.toString()+" related entity="+this.relatedentity.toString();
	}
	
	public String getString()
	{
		String quality="";
		for(Quality q:this.relationalquality.getProposals())
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
		for(Quality q:this.relationalquality.getProposals())
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
		for(Quality q:this.relationalquality.getProposals())
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
		
		for(Quality q:this.relationalquality.getProposals())
		{
			score*=q.getConfidienceScore();
		}
		for(Entity e:this.relatedentity.getProposals())
		{
			score*=e.getConfidienceScore();
		}
		return score;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
