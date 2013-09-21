package outputter.data;

public class FormalRelation implements FormalConcept {
	String searchString; //phrase
	String label; //lable of the class representing the phrase in an ontology
	String id; //id of the class representing the phrase in an ontology
	String classIRI; //class iri of the class representing the phrase in an ontology
	float confidenceScore; //the confidence the system has in the id and classIRI represent the semantics of the string.
	String string;
	
	public FormalRelation() {
	}

	/**
	 * 
	 */
	public FormalRelation(String string, String label, String id, String iri, String searchstring) {
		this.searchString = searchstring;
		this.label = label;
		this.id = id;
		this.classIRI = iri;	
		this.string = string;
	}

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#setString(java.lang.String)
	 */
	@Override
	public void setSearchString(String searchstring) {
		this.searchString = searchstring;

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

	/* (non-Javadoc)
	 * @see outputter.FormalConcept#getString()
	 */
	@Override
	public String getSearchString() {
		
		return this.searchString;
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
	public float getConfidenceScore() {
	
		return this.confidenceScore;
	}
	
	public String toString(){
		return this.label;
	}

	@Override
	public String content() {
		return this.classIRI;
	}
	@Override
	public boolean isOntologized() {
		return this.id != null;
	}

	public FormalRelation clone(){
		FormalRelation fr = new FormalRelation();
		fr.setClassIRI(this.classIRI);
		fr.setConfidenceScore(this.confidenceScore);
		fr.setId(this.id);
		fr.setLabel(this.label);
		fr.setSearchString(this.searchString);
		fr.setString(this.string);
		return fr;
	}

	@Override
	public void setString(String string) {
		this.string = string;
		
	}

	@Override
	public String getString() {
		return this.string;
	}




}