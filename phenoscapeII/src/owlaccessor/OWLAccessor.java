/**
 * 
 */
package owlaccessor;

import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

/**
 * This interface includes methods being used to retrieve meaning of and 
 * relationships among terms in PATO using OWL API. Keywords, synonyms, 
 * and parents of a term could be retrieved by giving the term.
 *  
 * @author Zilong Chang, Hong Cui
 *
 */
public interface OWLAccessor {
	
	/**
	 * Retrieve the classes representing exact matched or related terms (synonyms) of the given concept from PATO
	 * @param con - the given concept
	 * @return a list of matched or related terms (synonyms)
	 */
	public List<OWLClass> retrieveConcept(String con);
	
	/**
	 * Retrieve a set of keywords in a term's definition
	 * @param OWLClass c - the owlclass representing the term
	 * @return a set of keywords 
	 */
	public Set<String> getKeywords(OWLClass c);
	
	/**
	 * Return all classes in the PATO ontology
	 */
	public Set<OWLClass> getAllClasses();
	
	/**
	 * Retrieve the label (the readable term) of a OWLClass. 
	 */
	public String getLabel(OWLClass c);
	
	/**
	 * Retrieve related and exact synonyms of a OWLClass(term).
	 */
	public List<String> getSynonymLabels(OWLClass c);
	
	/**
	 * Retrieve parents labels of a given OWLClass. 
	 */
	public List<String> getParentsLabels(OWLClass c);
	
	/**
	 *Retrieve all offsprings of a term. 
	 */
	public Set<String> getAllOffSprings(OWLClass c);
	
	/**
	 *Retrieve class by label.
	 */
	public OWLClass getClassByLabel(String l);
}
