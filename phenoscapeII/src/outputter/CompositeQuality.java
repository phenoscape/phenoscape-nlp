package outputter;

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

	public CompositeQuality(String string, String label, String id) {
		super(string, label, id);
		// TODO Auto-generated constructor stub
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

}
