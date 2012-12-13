package owlaccessor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.semanticweb.owlapi.apibinding.OWLManager;
//import org.semanticweb.owlapi.io.*;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

// TODO: Auto-generated Javadoc
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
@SuppressWarnings("unused")
public class OWLAccessorImpl implements OWLAccessor {

	/** The manager. */
	private OWLOntologyManager manager;

	/** The df. */
	private OWLDataFactory df;

	/** A set of ontologies. */
	private Set<OWLOntology> onts;

	/** The allclasses. */
	private Set<OWLClass> allclasses=new HashSet<OWLClass>();
	
	/** The relational slim. */
	private Set<OWLClass> relationalSlim = new HashSet<OWLClass>();
	
	/** The obsolete. */
	private Set<OWLClass> obsolete = new HashSet<OWLClass>();
	
	/** The excluded. */
	private boolean excluded = false;
	
	/** The search cache. */
	private Hashtable<String, OWLClass> searchCache;
	
	/** The source. */
	private String source;
	/**
	 * Instantiates a new oWL accessor impl.
	 *
	 * @param ontoURL the onto url
	 * @param eliminate the eliminate
	 * @throws Exception the exception
	 */
	public OWLAccessorImpl(String ontoURL, ArrayList<String> eliminate)throws Exception {
		manager = OWLManager.createOWLOntologyManager();
		df = manager.getOWLDataFactory();
		IRI iri = IRI.create(ontoURL);
		source = ontoURL;
		OWLOntology rootOnt = manager.loadOntologyFromOntologyDocument(iri);
		
		constructorHelper(rootOnt, eliminate);
	}

		
	/**
	 * Instantiates a new oWL accessor impl.
	 * 
	 * @param file the file
	 * @param eliminate the eliminate
	 * @throws Exception the exception
	 */

	public OWLAccessorImpl(File file, ArrayList<String> eliminate) throws Exception {
		manager = OWLManager.createOWLOntologyManager();
		df = manager.getOWLDataFactory();
		source = file.getAbsolutePath();
		OWLOntology rootOnt = manager.loadOntologyFromOntologyDocument(file);
		
		constructorHelper(rootOnt, eliminate);
		
	}
	
	
	/**
	 * Constructor helper: the common part of the two constructors.
	 *
	 * @param rootOnt the root ont
	 * @param eliminate the eliminate
	 */
	private void constructorHelper(OWLOntology rootOnt, ArrayList<String> eliminate){
		onts=rootOnt.getImportsClosure();
		for (OWLOntology ont:onts){
			allclasses.addAll(ont.getClassesInSignature(true));
		}
		
		//eliminate branches
		allclasses.removeAll(this.getWordsToEliminate(eliminate));
		
		//add all relational slim terms to a list
		//also add all obsolete terms to a list
		for (OWLClass c: allclasses){
			if(isRelationalSlim(c)){
				this.relationalSlim.add(c);
			}
			if(this.isObsolete(c)){
				this.obsolete.add(c);
			}
		}
		
		//remove all obosolete classes
		relationalSlim.removeAll(obsolete);
		allclasses.removeAll(obsolete);
		
	}
	
	/**
	 * Gets the source.
	 *
	 * @return the source
	 */
	public String getSource(){
		return source;
	}

	/**
	 * Gets the keywords.
	 * 
	 * @param c
	 *            the c
	 * @return the keywords
	 */
	@Override
	public Set<String> getKeywords(OWLClass c) {
		WordFilter wf = new WordFilter();
		Set<OWLAnnotation> ds = this.getAnnotationByIRI(c,
				"http://purl.obolibrary.org/obo/IAO_0000115");

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

	/**
	 * Retrieve concept.
	 * 
	 * @param con
	 *            the concept
	 * @return the list
	 * @throws Exception
	 *             the exception
	 */
	@Override
	public List<OWLClass> retrieveConcept(String con) throws Exception {
		con = con.trim();
		List<OWLClass> result = new ArrayList<OWLClass>();		
		for (OWLClass c : allclasses) {
			List<String> syns = this.getSynonymLabels(c);
			String label = this.getLabel(c).toLowerCase();
			boolean syn = matchSyn(con, syns, "e");
			if (label.equals(con) || syn) {
				result.add(c);
			}
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see owlaccessor.OWLAccessor#retrieveConcept(java.lang.String, int)
	 */
	@Override
	public List<OWLClass> retrieveConcept(String con, int subgroup) throws Exception {
		con = con.trim();
		List<OWLClass> result = new ArrayList<OWLClass>();
		Set<OWLClass> classes = null;
		if(subgroup==outputter.TermEQ2IDEQ.RELATIONAL_SLIM){
			classes=this.relationalSlim;
		}else{
			classes=this.allclasses;
		}
		//maybe other slims
		
		for (OWLClass c : classes) {
			List<String> syns = this.getSynonymLabels(c);
			String label = this.getLabel(c).toLowerCase();
			boolean syn = matchSyn(con, syns, "e");
			if (label.equals(con) || syn) {
				result.add(c);
			}
		}
		return result;
	}
	
	/**
	 * Match syn.
	 * 
	 * @param label
	 *            the label
	 * @param synlabels
	 *            the synlabels
	 * @param mode
	 *            : exact="e" or partial="p"
	 * @return true, if successful
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
	 * Gets the words to eliminate.
	 *
	 * @param eliminate the eliminate
	 * @return the words to eliminate
	 */
	public Set<OWLClass> getWordsToEliminate(List<String> eliminate) {
		Set<OWLClass> er = new HashSet<OWLClass>();

		for (String s:eliminate){
			OWLClass c = this.getClassByLabel(s);
			er.add(c);//add itself to the set first
			er.addAll(this.getAllOffsprings(c));//add all its offsprings
		}
		return er;
	}

	/**
	 * Remove the non-readable or non-meaningful characters in the retrieval
	 * from OWL API, and return the refined output.
	 * <<<<<<< HEAD
	 *
	 * @param origin the origin??? what does it look like?
	 * >>>>>>> branch 'master' of ssh://git@github.com/zilongchang/phenoscape-nlp.git
	 * @return the refined output ??? what does it look like??
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
	 * 
	 * @param c
	 *            the c
	 * @return the parents
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
		for(OWLOntology ont:onts){
			for (OWLClassExpression ce : c.getSuperClasses(ont)) {
				if (!ce.isAnonymous())
					parent.add(ce.asOWLClass());
			}
		}
		return parent;
	}

	/**
	 * Gets the annotation property by an iri string.
	 * 
	 * @param c
	 *            the owl class
	 * @param iri
	 *            the iri of the annotation property
	 * @return the annotation property by iri
	 */
	public Set<OWLAnnotation> getAnnotationByIRI(OWLClass c, String iri) {
//		Set<OWLOntology> onts = ont.getImports(); 
//		onts.addAll(ont.getDirectImports());
//		onts.addAll(ont.getImportsClosure());
		
		Set<OWLAnnotation> allAnnotations = null;
		for(OWLOntology onto: onts){
			if(allAnnotations == null){
				allAnnotations = c.getAnnotations(onto,
					df.getOWLAnnotationProperty(IRI.create(iri)));
			}else{
				allAnnotations.addAll(c.getAnnotations(onto,
						df.getOWLAnnotationProperty(IRI.create(iri))));
			}
				
		}
		return allAnnotations;
		/*return c.getAnnotations(ont,
				df.getOWLAnnotationProperty(IRI.create(iri)));*/
	}

	/**
	 * Gets the annotation by iri.
	 * 
	 * @param c
	 *            the c
	 * @param iri
	 *            the iri
	 * @return the annotation by iri
	 */
	public Set<OWLAnnotation> getAnnotationByIRI(OWLClass c, IRI iri) {
		Set<OWLAnnotation> result = new HashSet<OWLAnnotation>();
		for(OWLOntology ont:onts){
				result.addAll(c.getAnnotations(ont, df.getOWLAnnotationProperty(iri)));
		}
		
		return result;
	}

	/**
	 * Checks if is obsolete.
	 *
	 * @param c the c
	 * @return true, if is obsolete
	 */
	public boolean isObsolete(OWLClass c){
		if(this.getLabel(c).startsWith("obsolete")){
			return true;
		}
		else{
			return false;
		}
	}
	
	/**
	 * Checks if is relational slim.
	 *
	 * @param c the c
	 * @return true, if is relational slim
	 */
	public boolean isRelationalSlim(OWLClass c){
		
		for(OWLAnnotation a: getAnnotationByIRI(c, "http://www.geneontology.org/formats/oboInOwl#inSubset")){
			if(a.getValue().toString().equals("http://purl.obolibrary.org/obo/pato#relational_slim"))
				return true;
		}
		
		return false;
	}
	/**
	 * Return the exact synonyms of a term represented by an OWLClass object.
	 * 
	 * @param c
	 *            the c
	 * @return the exact synonyms
	 */
	public Set<OWLAnnotation> getExactSynonyms(OWLClass c) {
		return this.getAnnotationByIRI(c,
				"http://www.geneontology.org/formats/oboInOwl#hasExactSynonym");
	}

	/**
	 * Return the related synonyms of a term represented by an OWLClass object.
	 * 
	 * @param c
	 *            the c
	 * @return the related synonyms
	 */
	public Set<OWLAnnotation> getRelatedSynonyms(OWLClass c) {
		return this
				.getAnnotationByIRI(c,
						"http://www.geneontology.org/formats/oboInOwl#hasRelatedSynonym");
	}
	
	/**
	 * Gets the narrow synonyms.
	 *
	 * @param c the c
	 * @return the narrow synonyms
	 */
	public Set<OWLAnnotation> getNarrowSynonyms(OWLClass c) {
		return this
				.getAnnotationByIRI(c,
						"http://www.geneontology.org/formats/oboInOwl#hasNarrowSynonym");
	}
	
	/**
	 * Gets the broad synonyms.
	 *
	 * @param c the c
	 * @return the broad synonyms
	 */
	public Set<OWLAnnotation> getBroadSynonyms(OWLClass c) {
		return this
				.getAnnotationByIRI(c,
						"http://www.geneontology.org/formats/oboInOwl#hasBroadSynonym");
	}

	/**
	 * Return the labels of a term represented by an OWLClass object.
	 * 
	 * @param c
	 *            the c
	 * @return the labels
	 */
	public Set<OWLAnnotation> getLabels(OWLClass c) {
		return this.getAnnotationByIRI(c, OWLRDFVocabulary.RDFS_LABEL.getIRI());
	}

	/**
	 * Gets the parents labels.
	 * 
	 * @param c
	 *            the c
	 * @return the parents labels
	 */
	@Override
	public List<String> getParentsLabels(OWLClass c) {
		List<String> result = new ArrayList<String>();
		for (OWLClass p : this.getParents(c)) {
			result.add(this.getLabel(p));
		}
		return result;
	}

	/**
	 * Gets the label.
	 * 
	 * @param c
	 *            the c
	 * @return the label
	 */
	@Override
	public String getLabel(OWLClass c) {
		if (this.getLabels(c).isEmpty()) {
			return "";
		} else {
			OWLAnnotation label = (OWLAnnotation) this.getLabels(c).toArray()[0];
			return this.getRefinedOutput(label.getValue().toString());
		}
	}

	/**
	 * Gets the synonym labels.
	 * 
	 * @param c
	 *            the c
	 * @return the synonym labels
	 */
	@Override
	public List<String> getSynonymLabels(OWLClass c) {
		ArrayList<String> labels = new ArrayList<String>();
		Set<OWLAnnotation> anns = getExactSynonyms(c);
		anns.addAll(this.getRelatedSynonyms(c));
		anns.addAll(this.getNarrowSynonyms(c));
		
		//Changed by ZILONG
		//anns.addAll(this.getBroadSynonyms(c));//DO NOT return Broad Synonyms now
		//END
		
		//if(c.getIRI().toString().contains("UBERON_0003221")){
		//	System.out.println("Syns for 0003221:"+anns.size());
		Iterator<OWLAnnotation> it = anns.iterator();
		while (it.hasNext()) {
			// Annotation(<http://www.geneontology.org/formats/oboInOwl#hasExactSynonym>
			// W)
			String label = this.getRefinedOutput(it.next().toString());
			//System.out.println("Syns:" + label);
			labels.add(label);
		}
		
		//}
		return labels;
	}

	/**
	 * Gets the all classes.
	 * 
	 * @return the all classes
	 */
	@Override
	public Set<OWLClass> getAllClasses() {
		// TODO Auto-generated method stub
		return allclasses;
	}

	/**
	 * Gets all offsprings of the given class.
	 * 
	 * @param c
	 *            the given class
	 * @return a set of all offsprings
	 */
	@Override
	public Set<OWLClass> getAllOffsprings(OWLClass c) {
		Set<OWLClass> r = new HashSet<OWLClass>();
		if(c!=null){
			for(OWLOntology ont: onts){
				for (OWLClassExpression ch : c.getSubClasses(ont)) {
					OWLClass o = ch.asOWLClass();
					r.add(o);
					r.addAll(this.getAllOffsprings(o));
				}
			}
		}
		return r;
	}

	/**
	 * Gets the all offspring lables.
	 * 
	 * @param c
	 *            the c
	 * @return the all offspring lables
	 */
	@Override
	public Set<String> getAllOffspringLables(OWLClass c) {
		Set<String> r = new HashSet<String>();
		for (OWLClassExpression ch : this.getAllOffsprings(c)) {
			r.add(this.getLabel(ch.asOWLClass()));
		}
		return r;
	}

	/**
	 * Gets the class by label.
	 * 
	 * @param l
	 *            the l
	 * @return the class by label
	 */
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

	/**
	 * Gets the iD.
	 * 
	 * @param c
	 *            the c
	 * @return the iD
	 */
	@Override
	public String getID(OWLClass c) {

		Set<OWLAnnotation> ids = new HashSet<OWLAnnotation>();
		for (OWLOntology ont:onts){		
			ids.addAll(c.getAnnotations(ont, df.getOWLAnnotationProperty(IRI.create("http://www.geneontology.org/formats/oboInOwl#id"))));
		}
		
		if (ids.isEmpty()) {
			return "";// no id, return empty string
		} else {
			//Should return only one id, assuming each class only has one id
			return this.getRefinedOutput(((OWLAnnotation) ids.toArray()[0])
					.toString());
		}
	}

}
