/**
 * 
 */
package owlaccessor;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

// TODO: Auto-generated Javadoc
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
	 * Retrieve the classes representing exact matched or related terms (synonyms) of the given concept from an ontology
	 * When there is no such a term, return and empty list.
	 * @param con - the given concept
	 * @return Hashtable<syn_type(original|exact|narrow|related),  arraylist of matched_classes>
	 * @throws Exception 
	 */
	public Hashtable<String, ArrayList<OWLClass>> retrieveConcept(String con) throws Exception;
	

	
	/**
	 * Retrieve the classes representing exact matched or related terms (synonyms) of the given concept from PATO 
	 * from a sub group, such as relational slim 
	 * When there is no such a term, return and empty list.
	 * @param con - the given concept
	 * @return a list of matched or related terms (synonyms)
	 * @throws Exception 
	 */
	//public List<OWLClass> retrieveConcept(String con, int subgroup) throws Exception;
	
	/**
	 * Retrieve a set of keywords in a term's definition.
	 *
	 * @param c the c
	 * @return a set of keywords
	 */
	public Set<String> getKeywords(OWLClass c);
	
	/**
	 * Return all classes in the PATO ontology.
	 *
	 * @return the all classes
	 */
	public Set<OWLClass> getAllClasses();
	
	/**
	 * Retrieve the label (the readable term) of a OWLClass.
	 *
	 * @param c the c
	 * @return the label
	 */
	public String getLabel(OWLClass c);
	
	/**
	 * Retrieve related and exact synonyms of a OWLClass(term).
	 *
	 * @param c the c
	 * @return the synonym labels
	 */
	public List<String> getSynonymLabels(OWLClass c);
	
	/**
	 * Retrieve parents labels of a given OWLClass.
	 *
	 * @param c the c
	 * @return the parents labels
	 */
	public List<String> getParentsLabels(OWLClass c);
	
	/**
	 * Retrieve all offspring terms of a term.
	 *
	 * @param c the c
	 * @return the all off springs
	 */
	public Set<String> getAllOffspringLables(OWLClass c);
	
	/**
	 * This method is flawed as one label may be used by multiple classes
	 * Retrieve OWLClass by label
	 * @param l the l
	 * @return the class by label
	 */
	//public OWLClass getClassByLabel(String l);
	
	/**
	 * Retrive ID of an OWLClass.
	 *
	 * @param c the c
	 * @return the iD
	 */
	public String getID(OWLClass c);
	
	/**
	 * Gets the all off springs.
	 *
	 * @param c the c
	 * @return the all off springs
	 */
	public Set<OWLClass> getAllOffsprings(OWLClass c);



	public OWLClass getClassByIRI(String iri);

}
