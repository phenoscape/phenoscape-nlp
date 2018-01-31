/**
 * 
 */
package outputter.data;


/**
 * @author Hong Cui
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
		this.relationalquality = relationalquality;
		this.relatedentity = relatedentity;
		
	}
	public QualityProposals getQuality(){
		return relationalquality;
	}
	
	public void setQuality(QualityProposals relationalquality){
		this.setRelationalquality(relationalquality);
	}
	
	public EntityProposals getRelatedEntity(){
		return relatedentity;
	}
	
	public void setRelatedEntity(EntityProposals relatedentity){
		this.relatedentity = relatedentity;
	}
	
	public void setRelationalquality(QualityProposals relationalquality) {
		this.relationalquality = relationalquality;
	}

	public String toString(){
		return "quality="+this.relationalquality.toString()+" related entity="+this.relatedentity.toString();
	}
	
	public String content(){
		return this.relationalquality.content()+" "+this.relatedentity.content();
	}
	
	/*public String getSearchString()
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
				quality+=q.getSearchString()+",";
			}
			else
			{
				quality+=q.getSearchString();
			}
			
		}
		return quality.replaceAll(",$", "");
	}*/
	
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
				id+=q.getId()+",";//added ',' by Hong 1/16/2014
			}
			
		}
		return id.replaceAll(",$", "");
	}
	
	//created to read the string of th relationalquality proposals
	
	public String getFullString()
	{
		String qstring="";
		for(Quality q:this.relationalquality.getProposals())
		{
			if((q instanceof CompositeQuality))
			{
				qstring+=((CompositeQuality)q).getFullString()+",";
			}else if((q instanceof NegatedQuality))
			{
				qstring+=((NegatedQuality)q).getFullString()+",";
			}else if(q instanceof RelationalQuality)
			{
				qstring+=((RelationalQuality)q).getFullString()+",";
			}
			else
			{
				qstring+=q.getString()+","; //added ',' by Hong 1/16/2014
			}
			
		}
		return qstring.replaceAll(",$", "");
	}
	
	public String getIRI()
	{
		String iri="";
		for(Quality q:this.relationalquality.getProposals())
		{
			if((q instanceof CompositeQuality))
			{
				iri+=((CompositeQuality)q).getFullIRI()+",";
			}else if((q instanceof NegatedQuality))
			{
				iri+=((NegatedQuality)q).getFullIRI()+",";
			}else if(q instanceof RelationalQuality)
			{
				iri+=q.getClassIRI()+",";
			}
			else
			{
				iri+=q.getClassIRI()+","; //added ',' by Hong 1/16/2014
			}
			
		}
		return iri.replaceAll(",$", "");
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
				label+=q.getLabel()+",";
			}
			
		}
		return label.replaceAll(",$", "");
		
	}
	
	
	@Override
	public float getConfidenceScore() {
		
		float score=1.0f;
		
		for(Quality q:this.relationalquality.getProposals())
		{
			score*=q.getConfidenceScore();
		}
/*		for(Entity e:this.relatedentity.getProposals())
		{
			score*=e.getConfidienceScore();
		}*///Not needed as per discussion with Hong
		return score;
	}
	
	public String getUnOntologized() {
		
		String unontologizedstring="";
		for(Quality q:this.relationalquality.getProposals())
		{
			if((q instanceof CompositeQuality))
			{
				unontologizedstring+=((CompositeQuality)q).getUnOntologized();
			}else if((q instanceof NegatedQuality))
			{
				unontologizedstring+=((NegatedQuality)q).getUnOntologized();
			}else if(q instanceof RelationalQuality)
			{
				unontologizedstring+=q.getUnOntologized();
			}
			else
			{
				unontologizedstring+=q.getUnOntologized();
			}
			
		}
		return unontologizedstring;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}



}
