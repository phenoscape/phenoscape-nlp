/**
 * 
 */
package owlaccessor;

import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

/**
 * @author Zilong Chang, Hong Cui
 *
 */
public interface OWLAccessor {
	public List<OWLClass> retrieveConcept(String con);
	public List<OWLClass> getParents(OWLClass c);
	public List<OWLClass> getAncestors(OWLClass c);
	public Set<String> getKeywords(OWLClass c);
	public Set<OWLClass> getAllClasses();
	public String getLabel(OWLClass c);
	public List<String> getSynonymLabels(OWLClass c);
	public List<String> getParentsLabels(OWLClass c);

}
