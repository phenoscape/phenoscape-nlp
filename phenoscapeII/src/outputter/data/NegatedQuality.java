/**
 * 
 */
package outputter.data;

import outputter.knowledge.Dictionary;

/**
 * @author updates
 *
 */
public class NegatedQuality extends Quality {
	Quality quality;
	Quality parentQuality;
	FormalRelation negation = Dictionary.complementof;
	
	

	/**
	 * 
	 */
	public NegatedQuality() {
		negation.setConfidenceScore(1);
	}

	/**
	 * @param string
	 * @param label
	 * @param id
	 * @param iri
	 */
	public NegatedQuality(String string, String label, String id, String classIRI, String searchstring, Quality parentQuality) {
		super("not "+string, label, id, classIRI, searchstring);
		this.parentQuality = parentQuality;
		negation.setConfidenceScore(1);
		this.quality = new Quality(string, label, id, classIRI, searchstring) ;
	}

	public NegatedQuality(Quality quality, Quality parentQuality) {
		super("not "+quality.getString(), quality.getLabel(), quality.getId(), quality.getClassIRI(), "not "+quality.getSearchString());
		this.parentQuality = parentQuality;
		negation.setConfidenceScore(1);
		this.quality = quality;
	}
	
	
	public String content(){
		return parentQuality.content()+" and "+negation.content()+" some "+super.content();
	}
	
	public void setParentQuality(Quality parentQuality){
		this.parentQuality = parentQuality;
	}
	
	public Quality getParentQuality(){
		return this.parentQuality;
	}
	
	public Quality getQuality(){
		return this.quality;
	}
	
	public String getFullString()
	{		
		//return this.parentQuality.getString()+" and ("+negation.getLabel()+" some "+this.string+")";
		return this.string;
	}
	
	public String getFullLabel()
	{
		String parentlabel=this.parentQuality.getString(),neglabel=this.getFullString();
		
		if(this.parentQuality.getLabel()!=null)
			parentlabel=this.parentQuality.getLabel();
		if(this.label!=null)
			neglabel=this.label;
		
		return parentlabel+" and ("+negation.getLabel()+" some "+neglabel+")";
	}
	
	public String getFullId()
	{
		return this.parentQuality.getId()+" and ("+negation.getId()+" some "+this.id+")";
	}
	
	public String getFullIRI() {
		return this.parentQuality.getClassIRI()+" and ("+negation.getClassIRI()+" some "+this.classIRI+")";
	}
	
	public String getLabel()
	{
		return this.parentQuality.getLabel()+" and ("+negation.getLabel()+" some "+this.label+")";
	}
	
	/**
	 * not waisted => shape and complement_of some waisted
	 */
	public String toString(){
		return "phrase="+this.string+" quality="+parentQuality.getLabel()+" and "+negation.toString()+ " some "+ this.label+" score="+this.confidenceScore;
	}
	
	// Returns unontologized string
	public String getUnOntologized()
	{
		if(parentQuality.getId()==null)
			return parentQuality.getUnOntologized();
		return "";
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}



}
