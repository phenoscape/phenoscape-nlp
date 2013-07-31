package outputter;

public class Quality implements FormalConcept {
	protected String string; //phrase
	protected String label; //lable of the class representing the phrase in an ontology
	protected String id; //id of the class representing the phrase in an ontology
	protected String classIRI; //class iri of the class representing the phrase in an ontology
	protected String type; //quality or negated quality
	protected float confidenceScore; //the confidence the system has in the id and classIRI represent the semantics of the string.
	
	public Quality() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	public Quality(String string, String label, String id) {
		this.string = string;
		this.label = label;
		this.id = id;
		
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setString(java.lang.String)
	 */
	@Override
	public void setString(String string) {
		this.string = string;

	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setLabel(java.lang.String)
	 */
	@Override
	public void setLabel(String label) {
		this.label = label;

	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setId(java.lang.String)
	 */
	@Override
	public void setId(String id) {
		this.id = id;

	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setClassIRI(java.lang.String)
	 */
	@Override
	public void setClassIRI(String IRI) {
		this.classIRI = IRI;

	}


	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setConfidenceScore(float)
	 */
	@Override
	public void setConfidenceScore(float score) {
		this.confidenceScore = score;

	}
	

	public void setType(String type) {
		this.type = type;

	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getString()
	 */
	@Override
	public String getString() {
		
		return this.string;
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getLabel()
	 */
	@Override
	public String getLabel() {
		
		return this.label;
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getId()
	 */
	@Override
	public String getId() {
		
		return this.id;
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getClassIRI()
	 */
	@Override
	public String getClassIRI() {
		return this.classIRI;
	}
	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getConfidienceScore()
	 */
	@Override
	public float getConfidienceScore() {
	
		return this.confidenceScore;
	}

	public String getType(){
		return this.type;
	}

	@Override
	public boolean isOntologized() {
		return this.id != null;
	}
	
	public String toString(){
		return "phrase="+this.string+" quality="+this.label+ " score="+this.confidenceScore;
	}
	
	public String content(){
		return this.classIRI;
	}
}



