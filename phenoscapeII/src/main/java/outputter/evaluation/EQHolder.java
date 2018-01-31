package outputter.evaluation;

public class EQHolder {

	String entitylabel;
	String entityid;
	String qualitylabel;
	String qualityid;
	String relatedentitylabel;
	String relatedentityid;
	String unontologizedentity;
	String unontologizedquality;
	String unontologizedrelatedentity;
	String gsunontologizedentity;
	String gsunontologizedquality;
	String gsunontologizedrelatedentity;
	
	
	public String getUnontologizedentity() {
		return unontologizedentity;
	}
	public void setUnontologizedentity(String unontologizedentity) {
		this.unontologizedentity = unontologizedentity;
	}
	public String getUnontologizedquality() {
		return unontologizedquality;
	}
	public void setUnontologizedquality(String unontologizedquality) {
		this.unontologizedquality = unontologizedquality;
	}
	public String getUnontologizedrelatedentity() {
		return unontologizedrelatedentity;
	}
	public void setUnontologizedrelatedentity(String unontologizedrelatedentity) {
		this.unontologizedrelatedentity = unontologizedrelatedentity;
	}
	public String getGsunontologizedentity() {
		return gsunontologizedentity;
	}
	public void setGsunontologizedentity(String gsunontologizedentity) {
		this.gsunontologizedentity = gsunontologizedentity;
	}
	public String getGsunontologizedquality() {
		return gsunontologizedquality;
	}
	public void setGsunontologizedquality(String gsunontologizedquality) {
		this.gsunontologizedquality = gsunontologizedquality;
	}
	public String getGsunontologizedrelatedentity() {
		return gsunontologizedrelatedentity;
	}
	public void setGsunontologizedrelatedentity(String gsunontologizedrelatedentity) {
		this.gsunontologizedrelatedentity = gsunontologizedrelatedentity;
	}
	public String getEntitylabel() {
		return entitylabel;
	}
	public void setEntitylabel(String entitylabel) {
		this.entitylabel = entitylabel;
	}
	public String getEntityid() {
		return entityid;
	}
	public void setEntityid(String entityid) {
		this.entityid = entityid;
	}
	public String getQualitylabel() {
		return qualitylabel;
	}
	public void setQualitylabel(String qualitylabel) {
		this.qualitylabel = qualitylabel;
	}
	public String getQualityid() {
		return qualityid;
	}
	public void setQualityid(String qualityid) {
		this.qualityid = qualityid;
	}
	public String getRelatedentitylabel() {
		return relatedentitylabel;
	}
	public void setRelatedentitylabel(String relatedentitylabel) {
		this.relatedentitylabel = relatedentitylabel;
	}
	public String getRelatedentityid() {
		return relatedentityid;
	}
	public void setRelatedentityid(String relatedentityid) {
		this.relatedentityid = relatedentityid;
	}
	
	public String toString()
	{
		return ("EntityLabel = "+ this.entitylabel+ " EntityID = "+ this.entityid +" QualityID = "+ this.qualityid +" QualityLabel = "+this.qualitylabel+ "RelatedEntityLabel = "+this.relatedentitylabel+"RelatedEntityID = "+this.relatedentityid);
			   
	}
}
