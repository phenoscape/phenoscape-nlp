package outputter;

/**
* perform reasoning tasks
**/
import java.io.File;
import java.util.Iterator;
import java.util.Set;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;


public class ELKReasoner {
	OWLReasoner reasoner;
	OWLDataFactory dataFactory;
	OWLOntology ont;
	OWLOntologyManager man;
	
	public ELKReasoner(String ontologyIRI) throws OWLOntologyCreationException{
		man = OWLManager.createOWLOntologyManager();
		dataFactory = man.getOWLDataFactory();
		// Load your ontology.
		ont = man.loadOntology(IRI.create(ontologyIRI));
		// Create an ELK reasoner.
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ont);
	}
	
	public ELKReasoner(File ontologyfile) throws OWLOntologyCreationException{
		man = OWLManager.createOWLOntologyManager();
		dataFactory = man.getOWLDataFactory();
		// Load your ontology.
		ont = man.loadOntologyFromOntologyDocument(ontologyfile);
		// Create an ELK reasoner.
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ont);
	}
	
	/**
	* union of (in_lateral_side_of some Thing) and (part_of some in_lateral_side_of some Thing)
	* this is used to find paired structures and their parts, for example clavicle blade is part of clavicle, which is a paired structure: there are left clavicle and right clavicle. 
	**/
	Set<OWLClass> getClassesWithLateralSides(){
	    OWLClass thing = dataFactory.getOWLClass(IRI.create("http://www.w3.org/2002/07/owl#Thing"));//Thing 
		OWLObjectProperty lateralside = dataFactory.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BSPO_0000126"));//in_lateral_side_of
		OWLObjectProperty partof = dataFactory.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BFO_0000050")); //part_of
		OWLClassExpression query1 = dataFactory.getOWLObjectSomeValuesFrom(lateralside, thing);
		OWLClassExpression query2 = dataFactory.getOWLObjectSomeValuesFrom(partof, 
			dataFactory.getOWLObjectSomeValuesFrom(lateralside, thing));

		// Create a fresh name for the query1.
		OWLClass newName1 = dataFactory.getOWLClass(IRI.create("temp001"));
		// Make the query equivalent to the fresh class
		OWLAxiom definition1 = dataFactory.getOWLEquivalentClassesAxiom(newName1,
				query1);
		man.addAxiom(ont, definition1);
		
		// Create a fresh name for the query2.
		OWLClass newName2 = dataFactory.getOWLClass(IRI.create("temp002"));
		// Make the query equivalent to the fresh class
		OWLAxiom definition2 = dataFactory.getOWLEquivalentClassesAxiom(newName2,
				query2);
		man.addAxiom(ont, definition2);

		// Remember to either flush the reasoner after the ontology change
		// or create the reasoner in non-buffering mode. Note that querying
		// a reasoner after an ontology change triggers re-classification of
		// the whole ontology which might be costly. Therefore, if you plan
		// to query for multiple complex class expressions, it will be more
		// efficient to add the corresponding definitions to the ontology at
		// once before asking any queries to the reasoner.
		reasoner.flush(); //triggers re-classification of the whole ontology

		// You can now retrieve subclasses, superclasses, and instances of
		// the query class by using its new name instead.
		Set<OWLClass> subClasses = reasoner.getSubClasses(newName1, false).getFlattened();
		subClasses.addAll(reasoner.getSubClasses(newName2, false).getFlattened());
		
		//reasoner.getSuperClasses(newName, true);
		//reasoner.getInstances(newName, false);

		// After you are done with the queries, you should remove the definitions
		man.removeAxiom(ont, definition1);
		man.removeAxiom(ont, definition2);

		// You can now add new definitions for new queries in the same way

		// After you are done with all queries, do not forget to free the
		// resources occupied by the reasoner
		reasoner.dispose();
		for (OWLClass owlClass : subClasses) {
			// Just iterates over it and print the name of the class
			for (OWLAnnotation labelannotation : owlClass
					.getAnnotations(ont, dataFactory.getRDFSLabel())) {
					if (labelannotation.getValue() instanceof OWLLiteral) {
						OWLLiteral val = (OWLLiteral) labelannotation.getValue();
						System.out.println(val.getLiteral());
					}
			}
		}
		return subClasses;
	}
	
	public  boolean isSubClassOf(String subclassIRI, String superclassIRI){
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		OWLClass superclass = dataFactory.getOWLClass(IRI.create(superclassIRI));
		NodeSet<OWLClass> subClasses = reasoner.getSubClasses(superclass, false); //grab all descendant classes
		reasoner.dispose();
		Iterator<OWLClass> it = subClasses.getFlattened().iterator();
		while(it.hasNext()){
			OWLClass aclass = it.next();
			//System.out.println(aclass.getIRI().toString());
			if(aclass.getIRI().toString().compareTo(subclassIRI)==0){
				return true;
			}
		}
		return false;
	}	
	
	public static void main(String[] argv){
		try {
			ELKReasoner elk = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+"ext.owl"));
			//Set<OWLClass> classes = elk.getClassesWithLateralSides();
			//String subclassIRI = "http://purl.obolibrary.org/obo/UBERON_0005621";//rhomboid is an organ
			String subclassIRI = "http://purl.obolibrary.org/obo/UBERON_0003098";//optic stalk is not an organ
			String superclassIRI = "http://purl.obolibrary.org/obo/UBERON_0000062"; //organ
			System.out.println(elk.isSubClassOf(subclassIRI, superclassIRI));		
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}