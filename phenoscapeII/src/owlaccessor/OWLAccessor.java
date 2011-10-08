/**
 * 
 */
package owlaccessor;

import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClass;

/**
 * @author Zilong Chang
 *
 */
public interface OWLAccessor {
	public List<OWLClass> retrieveConcept(String con);
//	public void showClass(OWLClass c, OWLOntology o);
//	public void showSuperClass(OWLClass c, OWLOntology o, int depth);
//	public List<OWLClass> getParent(OWLClass c, OWLOntology o);
//	public List<OWLClass> getAncestors(OWLClass c, OWLOntology o);
//	public Set<OWLAnnotation> getExactSynonyms(OWLClass c, OWLOntology o, OWLDataFactory df);
	public void showClass(OWLClass c);
	public void showSuperClass(OWLClass c, int depth);
	public Set<OWLAnnotation> getLabels(OWLClass c);
	public List<OWLClass> getParent(OWLClass c);
	public List<OWLClass> getAncestors(OWLClass c);
	public Set<OWLAnnotation> getExactSynonyms(OWLClass c);

}
