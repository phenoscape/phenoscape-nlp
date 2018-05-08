package outputter.data;


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
	String  restrictedrelation; //TODO: check with Hari, should this be a formal concept too?
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
			return this.mainquality.getString()+" and "+this.restrictedrelation+"("+this.comparedquality.getString()+" and "+this.relatedentity.getRelation().getString()+"("+((CompositeEntity)((REntity)this.relatedentity).getEntity()).getFullString()+"))";
		}
		else
		{
			return this.mainquality.getString()+" and "+this.restrictedrelation+"("+this.comparedquality.getString()+" and "+this.relatedentity.getRelation().getString()+"("+((REntity)this.relatedentity).getEntity().getString()+"))";
		}
	}
	
	public String getFullLabel()
	{
		return this.mainquality.getLabel()+" and "+this.restrictedrelation+"("+this.comparedquality.getLabel()+" and "+this.relatedentity.getRelation().getLabel()+"("+((REntity)this.relatedentity).getEntity().getLabel()+"))";
	}
	
	public String getLabel(){
		return this.mainquality.getLabel();
	}
	
	public String getFullId()
	{
		if(((REntity)this.relatedentity).getEntity() instanceof CompositeEntity)
		{
		return this.mainquality.getId()+" and "+this.restrictedrelation+"("+this.comparedquality.getId()+" and "+this.relatedentity.getRelation().getId()+"("+((CompositeEntity)((REntity)this.relatedentity).getEntity()).getFullID()+"))";
		}
		else
		{
		return this.mainquality.getId()+" and "+this.restrictedrelation+"("+this.comparedquality.getId()+" and "+this.relatedentity.getRelation().getId()+"("+((REntity)this.relatedentity).getEntity().getId()+"))";
		}
	}
	
	public String getFullIRI() {
		if(((REntity)this.relatedentity).getEntity() instanceof CompositeEntity)
		{
		return this.mainquality.getClassIRI()+" and "+this.restrictedrelation+"("+this.comparedquality.getClassIRI()+" and "+this.relatedentity.getRelation().getClassIRI()+"("+((CompositeEntity)((REntity)this.relatedentity).getEntity()).getFullIRI()+"))";
		}
		else
		{
		return this.mainquality.getClassIRI()+" and "+this.restrictedrelation+"("+this.comparedquality.getClassIRI()+" and "+this.relatedentity.getRelation().getClassIRI()+"("+((REntity)this.relatedentity).getEntity().getClassIRI()+"))";
		}
	}
	
	@Override
	public float getConfidenceScore() {
		
		return this.mainquality.getConfidenceScore()*this.comparedquality.getConfidenceScore()*this.relatedentity.getConfidenceScore();

	}
	
	public String getId()
	{
		return null;
	}
	public Quality getMainQuality() {
		return mainquality;
	}

	public Quality getComparedQuality() {
		return comparedquality;
	}

	public String getRestrictedRelation() {
		return restrictedrelation;
	}

	public REntity getRelatedEntity() {
		return relatedentity;
	}
	
	public String toString(){
		
		return this.mainquality.getString()+" and "+this.restrictedrelation+"("+this.comparedquality.getString()+this.relatedentity.toString()+")";
	}

	public String getUnOntologized()
	{
		String unontologized ="";
		if(this.mainquality.getId()==null)
		{
			unontologized = this.mainquality.getUnOntologized()+"#";
		}
		if(this.comparedquality.getId()==null)
		{
			unontologized+=this.comparedquality.getUnOntologized()+"#";
		}
		
		return unontologized;
	}
	public String content(){
		
		return this.mainquality.content()+" and "+this.restrictedrelation+"("+this.comparedquality.content()+this.relatedentity.content()+")";
	}


}
