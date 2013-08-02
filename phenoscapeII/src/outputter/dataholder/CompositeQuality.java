package outputter.dataholder;


/**
 * For post-composition of qualities.
 * For example, an author may describe the frontal bone as having a "greater length relative to width". This requires post-composing the quality with the two PATO terms for length and width:
     E: frontal bone, Q: length^increased_in_magnitude_relative_to(width^inheres_in(frontal bone))
     
 * @author Hari, Hong Cui
 *
 */
public class CompositeQuality extends Quality {

	Quality mainquality;
	Quality comparedquality;
	String  restrictedrelation;
	REntity relatedentity;

	public CompositeQuality(Quality primary_quality, Quality secondary_quality, String relation, Entity relatedentity) {
		// TODO Auto-generated constructor stub
		this.mainquality = primary_quality;
		this.comparedquality = secondary_quality;
		this.restrictedrelation = relation;
		this.relatedentity = (REntity) relatedentity;
	}

/*	public CompositeQuality(String string, String label, String id) {
		super(string, label, id);
		// TODO Auto-generated constructor stub
	}
	*/
	
	public String getFullString()
	{
		if(((REntity)this.relatedentity).getEntity() instanceof CompositeEntity)
		{
			return this.mainquality.getString()+"^"+this.restrictedrelation+"("+this.comparedquality.getString()+"^"+this.relatedentity.getRelation().getString()+"("+((CompositeEntity)((REntity)this.relatedentity).getEntity()).getFullString()+"))";
		}
		else
		{
			return this.mainquality.getString()+"^"+this.restrictedrelation+"("+this.comparedquality.getString()+"^"+this.relatedentity.getRelation().getString()+"("+((REntity)this.relatedentity).getEntity().getString()+"))";
		}
	}
	
	public String getFullLabel()
	{
		return this.mainquality.getLabel()+"^"+this.restrictedrelation+"("+this.comparedquality.getLabel()+"^"+this.relatedentity.getRelation().getLabel()+"("+((REntity)this.relatedentity).getEntity().getLabel()+"))";
	}
	
	public String getFullId()
	{
		if(((REntity)this.relatedentity).getEntity() instanceof CompositeEntity)
		{
		return this.mainquality.getId()+"^"+this.restrictedrelation+"("+this.comparedquality.getId()+"^"+this.relatedentity.getRelation().getId()+"("+((CompositeEntity)((REntity)this.relatedentity).getEntity()).getFullID()+"))";
		}
		else
		{
		return this.mainquality.getId()+"^"+this.restrictedrelation+"("+this.comparedquality.getId()+"^"+this.relatedentity.getRelation().getId()+"("+((REntity)this.relatedentity).getEntity().getId()+"))";
		}
	}
	
	@Override
	public float getConfidienceScore() {
		
		return this.mainquality.getConfidienceScore()*this.comparedquality.getConfidienceScore()*this.relatedentity.getConfidienceScore();

	}
	
	public String getId()
	{
		return null;
	}
	public Quality getMainquality() {
		return mainquality;
	}

	public Quality getComparedquality() {
		return comparedquality;
	}

	public String getRestrictedrelation() {
		return restrictedrelation;
	}

	public REntity getRelatedentity() {
		return relatedentity;
	}
	
	public String toString(){
		
		return this.mainquality.getString()+"^"+this.restrictedrelation+"("+this.comparedquality.getString()+this.relatedentity.toString()+")";
	}

	public String content(){
		
		return this.mainquality.content()+"^"+this.restrictedrelation+"("+this.comparedquality.content()+this.relatedentity.content()+")";
	}
}
