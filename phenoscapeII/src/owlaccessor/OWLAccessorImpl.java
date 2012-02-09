package owlaccessor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

//import org.semanticweb.owlapi.util.SimpleIRIMapper;

//this message is for testing purpose

/**
 * This class includes implemented methods being used to retrieve meaning of and
 * relationships among terms in PATO using OWL API. Keywords, synonyms, and
 * parents of a term could be retrieved by giving the term.
 * 
 * TAO: http://berkeleybop.org/ontologies/tao.owl
 * PATO: http://purl.obolibrary.org/obo/pato.owl
 * 
 * @author Zilong Chang, Hong Cui
 * 
 */
public class OWLAccessorImpl implements OWLAccessor {

	private OWLOntologyManager manager;
	private OWLDataFactory df;
	private OWLOntology ont;

	public OWLAccessorImpl(String ontoURL) {
		manager = OWLManager.createOWLOntologyManager();
		df = manager.getOWLDataFactory();
		IRI iri = IRI.create(ontoURL);
		try {
			ont = manager.loadOntologyFromOntologyDocument(iri);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public OWLAccessorImpl(File file) {
		manager = OWLManager.createOWLOntologyManager();
		df = manager.getOWLDataFactory();

		try {
			ont = manager.loadOntologyFromOntologyDocument(file);
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Set<String> getKeywords(OWLClass c) {
		WordFilter wf = new WordFilter();
		Set<OWLAnnotation> ds = c.getAnnotations(ont, df
				.getOWLAnnotationProperty(IRI
						.create("http://purl.obolibrary.org/obo/IAO_0000115")));
		if (ds.isEmpty()) {
			return new HashSet<String>();
		} else {
			Set<String> tresult = new HashSet<String>();
			OWLAnnotation orgdef = (OWLAnnotation) ds.toArray()[0];
			String def = this.getRefinedOutput(orgdef.toString()).replaceFirst(
					"quality inhering in a bearer by virtue of the bearer's",
					"");
			StringTokenizer st = new StringTokenizer(def);
			while (st.hasMoreTokens()) {
				String temp = st.nextToken().replaceAll("\\\\", "")
						.replaceAll(",", "").replaceAll("\\.", "")
						.replaceAll("\\(", "").replaceAll("\\)", "")
						.replaceAll(";", "").replaceAll(":", "")
						.replaceAll(".*[0-9].*", "")
						.replaceAll(".*[+\\-*/].*", "").trim().toLowerCase();
				if (temp.length() > 1) {
					if (!wf.isInList(temp)) {
						tresult.add(temp);
					}
				}
			}
			return tresult;
		}
	}

	@Override
	public List<OWLClass> retrieveConcept(String con) {
		con = con.trim();
		List<OWLClass> result = new ArrayList<OWLClass>();
		try {
			for (OWLClass c : ont.getClassesInSignature()) {
				// match class concepts and also the synonyms
				List<String> syns = this.getSynonymLabels(c);
				String label = this.getLabel(c);
				boolean syn = matchSyn(con, syns, "p");
				if (label.contains(con) || label.equals(con) || syn) {
					result.add(c);
					if (syn && !label.contains(con)) {
						//System.out.println("syn+:" + con);
					}
					//break;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * 
	 * @param con
	 * @param syns
	 * @param mode
	 *            : exact="e" or partial="p"
	 * @return
	 */
	private boolean matchSyn(String label, List<String> synlabels, String mode) {
		Iterator<String> it = synlabels.iterator();
		while (it.hasNext()) {
			if (mode.compareTo("p") == 0 && it.next().contains(label)) {
				return true;
			}
			if (mode.compareTo("e") == 0 && it.next().compareTo(label) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Remove the non-readable or non-meaningful characters in the retrieval
	 * from OWL API, and return the refined output.
	 */
	public String getRefinedOutput(String origin) {
		// Annotation(<http://www.geneontology.org/formats/oboInOwl#hasExactSynonym>
		// W)
		if (origin.startsWith("Annotation")) {
			origin = origin.replaceFirst("^Annotation.*>\\s+", "")
					.replaceFirst("\\)\\s*$", "").trim();
		}

		/*
		 * Remove the ^^xsd:string tail from the returned annotation value
		 */
		return origin.replaceAll("\\^\\^xsd:string", "").replaceAll("\"", "")
				.replaceAll("\\.", "");
	}

	/**
	 * Recursively return all the ancestors of a term.
	 */
	// public Set<String> getAncestors(OWLClass c) {
	// Set<String> result = new HashSet<String>();
	// this.getAncestorsHelper(c, result);
	// return result;
	// }

	// private void getAncestorsHelper(OWLClass c, Set<String> s) {
	// if(c.gets)
	// for (OWLClassExpression p : c.getSuperClasses(ont)) {
	// if (!p.isAnonymous()) {
	// l.add(p.asOWLClass());
	// this.getAncestorsHelper(p.asOWLClass(), l);
	// }
	// }
	// }

	/**
	 * Return the parents of a term.
	 */
	public List<OWLClass> getParents(OWLClass c) {
		List<OWLClass> parent = new ArrayList<OWLClass>();
		for (OWLClassExpression ce : c.getSuperClasses(ont)) {
			if (!ce.isAnonymous())
				parent.add(ce.asOWLClass());
		}
		return parent;
	}

	/**
	 * Return the exact synonyms of a term represented by an OWLClass object.
	 */
	public Set<OWLAnnotation> getExactSynonyms(OWLClass c) {
		return c.getAnnotations(
				ont,
				df.getOWLAnnotationProperty(IRI
						.create("http://www.geneontology.org/formats/oboInOwl#hasExactSynonym")));
	}

	/**
	 * Return the related synonyms of a term represented by an OWLClass object.
	 */
	public Set<OWLAnnotation> getRelatedSynonyms(OWLClass c) {
		return c.getAnnotations(
				ont,
				df.getOWLAnnotationProperty(IRI
						.create("http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym")));
	}

	/**
	 * Return the labels of a term represented by an OWLClass object.
	 */
	public Set<OWLAnnotation> getLabels(OWLClass c) {
		return c.getAnnotations(ont, df
				.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()));
	}

	@Override
	public List<String> getParentsLabels(OWLClass c) {
		List<String> result = new ArrayList<String>();
		for (OWLClass p : this.getParents(c)) {
			result.add(this.getLabel(p));
		}
		return result;
	}

	@Override
	public String getLabel(OWLClass c) {
		if (this.getLabels(c).isEmpty()) {
			return "";
		} else {
			OWLAnnotation label = (OWLAnnotation) this.getLabels(c).toArray()[0];
			return this.getRefinedOutput(label.getValue().toString());
		}
	}

	@Override
	public List<String> getSynonymLabels(OWLClass c) {
		ArrayList<String> labels = new ArrayList<String>();
		Set<OWLAnnotation> anns = getExactSynonyms(c);
		anns.addAll(this.getRelatedSynonyms(c));
		Iterator<OWLAnnotation> it = anns.iterator();
		while (it.hasNext()) {
			// Annotation(<http://www.geneontology.org/formats/oboInOwl#hasExactSynonym>
			// W)
			labels.add(this.getRefinedOutput(it.next().toString()));
		}
		return labels;
	}

	@Override
	public Set<OWLClass> getAllClasses() {
		// TODO Auto-generated method stub
		return ont.getClassesInSignature();
	}

	@Override
	public Set<String> getAllOffSprings(OWLClass c) {
		Set<String> r = new HashSet<String>();
		for (OWLClassExpression ch : c.getSubClasses(ont)) {
			r.add(this.getLabel(ch.asOWLClass()));
			r.addAll(this.getAllOffSprings(ch.asOWLClass()));
		}
		return r;
	}

	@Override
	public OWLClass getClassByLabel(String l) {
		for (OWLClass c : this.getAllClasses()) {
			if (this.getLabel(c).trim().toLowerCase()
					.equals(l.trim().toLowerCase())) {
				return c;
			}
		}

		return null;

	}

	@Override
	public String getID(OWLClass c) {

		Set<OWLAnnotation> ids = c
				.getAnnotations(
						ont,
						df.getOWLAnnotationProperty(IRI
								.create("http://www.geneontology.org/formats/oboInOwl#id")));
		
		if(ids.isEmpty()){
			return "";//no id, return empty string
		}else{
			return this.getRefinedOutput(((OWLAnnotation)ids.toArray()[0]).toString());
		}
	}
}
