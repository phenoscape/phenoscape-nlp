package outputter;

/**
 * @author Hong Cui
 * this class performs reasoning tasks
 * 
 * the conventions used: 
 * 1. flush the reasoner *before* sending a query
 * 2. remove added axioms *after* receiving the result from the reasoner.
 **/
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntaxEditorParser;
import org.semanticweb.elk.owlapi.ElkReasonerConfiguration;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.elk.reasoner.config.ReasonerConfiguration;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.expression.OWLEntityChecker;
import org.semanticweb.owlapi.expression.ParserException;
import org.semanticweb.owlapi.expression.ShortFormEntityChecker;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationObjectVisitorEx;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.BidirectionalShortFormProvider;
import org.semanticweb.owlapi.util.BidirectionalShortFormProviderAdapter;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;
import org.semanticweb.owlapi.util.OWLObjectVisitorExAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import uk.ac.manchester.cs.owl.owlapi.OWLObjectPropertyImpl;


public class ELKReasoner{
	private static final Logger LOGGER = Logger.getLogger("org.semanticweb.elk");   
	OWLReasoner reasoner;
	OWLDataFactory dataFactory;
	OWLOntology ont;
	OWLOntologyManager man;
	private ElkReasonerFactory reasonerFactory;

	public static Hashtable<String,IRI> lateralsidescache = new Hashtable<String,IRI>();//holds classes with lateral sides
	boolean printmessage = Boolean.valueOf(ApplicationUtilities.getProperty("elk.printmessage"));

	public ELKReasoner(OWLOntology ont) throws OWLOntologyCreationException{
		if(!this.printmessage) LOGGER.setLevel(Level.ERROR);
		man = OWLManager.createOWLOntologyManager();
		dataFactory = man.getOWLDataFactory();
		this.ont = ont;
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);	
		final ElkReasonerConfiguration elkConfig = new ElkReasonerConfiguration(); 
		// Set the number of workers to 4 or any other number 
		elkConfig.getElkConfiguration().setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS,"4"); 
		reasoner = reasonerFactory.createReasoner(ont,elkConfig);	
	//	reasoner = reasonerFactory.createReasoner(ont);
		getClassesWithLateralSides();
	}

	public ELKReasoner(File ontologyfile) throws OWLOntologyCreationException{
		if(!this.printmessage) LOGGER.setLevel(Level.ERROR);
		man = OWLManager.createOWLOntologyManager();
		dataFactory = man.getOWLDataFactory();
		// Load your ontology.
		ont = man.loadOntologyFromOntologyDocument(ontologyfile);
		// Create an ELK reasoner.
		reasonerFactory = new ElkReasonerFactory();
				final ElkReasonerConfiguration elkConfig = new ElkReasonerConfiguration(); 
		// Set the number of workers to 4 or any other number 
		elkConfig.getElkConfiguration().setParameter(ReasonerConfiguration.NUM_OF_WORKING_THREADS,"4"); 
		reasoner = reasonerFactory.createReasoner(ont,elkConfig);	
		//reasoner = reasonerFactory.createReasoner(ont);
		getClassesWithLateralSides();//populates the lateral sides cache 
	}

	/**
	 * union of (in_lateral_side_of some Thing) and (part_of some in_lateral_side_of some Thing)
	 * this is used to find paired structures and their parts, for example clavicle blade is part of clavicle, which is a paired structure: there are left clavicle and right clavicle. 
	 **/
	void getClassesWithLateralSides(){
		if(!this.printmessage) LOGGER.setLevel(Level.ERROR);
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
		//reasoner.dispose();
		for (OWLClass owlClass : subClasses) {
			// Just iterates over it and print the name of the class
			for (OWLAnnotation labelannotation : owlClass
					.getAnnotations(ont, dataFactory.getRDFSLabel())) {
				if (labelannotation.getValue() instanceof OWLLiteral) {
					OWLLiteral val = (OWLLiteral) labelannotation.getValue();
					lateralsidescache.put(val.getLiteral(), owlClass.getIRI());
				}
			}
		}
		//return subClasses;
	}

	/**
	 * is subclassIRI is a subclass of superclassIRI?
	 * @param subclassIRI
	 * @param superclassIRI
	 * @return
	 */
	public boolean isSubClassOf(String subclassIRI, String superclassIRI){
		if(this.printmessage) LOGGER.setLevel(Level.ERROR);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		OWLClass superclass = dataFactory.getOWLClass(IRI.create(superclassIRI));
		NodeSet<OWLClass> subClasses = reasoner.getSubClasses(superclass, false); //grab all descendant classes
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

	public boolean isEquivalent(String classIRI1, String classIRI2){
		if(this.printmessage) LOGGER.setLevel(Level.ERROR);
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		OWLClass class1 = dataFactory.getOWLClass(IRI.create(classIRI1));
		OWLClass class2 = dataFactory.getOWLClass(IRI.create(classIRI2));
		return isEquivalentClass(class1, class2);
	}

	public boolean isEquivalentClass(OWLClass class1, OWLClassExpression class2) {
		/* this doesn't work: returns wrong value
		 * reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		Node<OWLClass> eqclasses = reasoner.getEquivalentClasses(class1);
		return eqclasses.getEntities().contains(class2);*/
		Set<OWLOntology> onts = this.ont.getImportsClosure();
		Set<OWLClassExpression> classes = class1.getEquivalentClasses(onts);
		return classes.contains(class2) || class1.equals(class2);
	}
	/**
	 * is class1 a part of class2
	 * @param class1IRI
	 * @param whole
	 * @return
	 */
	public boolean isPartOf(String part, String whole) {
		if(this.printmessage) LOGGER.setLevel(Level.ERROR);
		OWLObjectProperty rel = dataFactory.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BFO_0000050")); //part_of
		OWLClassExpression partofclass2 = dataFactory.getOWLObjectSomeValuesFrom(rel, dataFactory.getOWLClass(IRI.create(whole)));
		// Create a fresh name
		OWLClass newclass = dataFactory.getOWLClass(IRI.create("temp001"));
		//make newclass equivalent of partofclass2 
		OWLAxiom axiom = dataFactory.getOWLEquivalentClassesAxiom(newclass,partofclass2);
		man.addAxiom(ont, axiom);
		reasoner.flush();
		boolean result =  isSubClassOf(part, newclass.getIRI().toString());
		man.removeAxiom(ont, axiom);
		return result;
	}

	/**
	 * this method was largely taken from examples published on OWL API website.
	 * but it does not gather all the restrictions ('inherited anonymous classes').
	 * @param classIRI
	 * @param partIRI
	 * @return
	 */
	public boolean isSubclassOfWithPart(String subclassIRI, String partIRI){	
		if(this.printmessage) LOGGER.setLevel(Level.ERROR);
		OWLClass part= dataFactory.getOWLClass(IRI.create(partIRI)); //'epichordal lepidotrichium'
		//OWLClass subclaz= dataFactory.getOWLClass(IRI.create(subclassIRI)); //'caudal fin' 4000164

		HashSet<OWLClass> classeswithpart = new HashSet<OWLClass>();
		/*made no difference
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_ASSERTIONS);
		reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_HIERARCHY);*/

		//find all classes that have 'part' 

		RestrictionVisitor restrictionVisitor = new RestrictionVisitor(
				Collections.singleton(ont));
		for (OWLSubClassOfAxiom ax : ont
				.getSubClassAxiomsForSubClass(part)) {
			OWLClassExpression superCls = ax.getSuperClass();
			superCls.accept(restrictionVisitor);
		}
		System.out.println("Restricted properties for " + part + ": "
				+ restrictionVisitor.getRestrictedProperties().size());
		for (OWLObjectSomeValuesFrom prop : restrictionVisitor
				.getRestrictedProperties()) {
			if(prop.getProperty().toString().contains("http://purl.obolibrary.org/obo/BFO_0000050")){
				classeswithpart.add((OWLClass) prop.getFiller());
			}
		}

		//loop though classeswithpart
		for(OWLClass classwithpart: classeswithpart){
			if(isSubClassOf(subclassIRI, classwithpart.getIRI().toString())){
				return true;
			}
		}
		return false;
	}



	/**
	 * 		
		is class a subclass of the things that has part part
		for example, could 'caudal fin' has_part 'epichordal lepidotrichium'?
	 * @param classIRI
	 * @param partIRI
	 * @return
	 * this won't work for ELK 0.3, it does not support inverseObjectProperties. 
	 */
	/*public boolean isSubclassOfWithPart(String classIRI, String partIRI){	
		OWLClass part= dataFactory.getOWLClass(IRI.create(partIRI)); //'epichordal lepidotrichium'
		//OWLClass claz= dataFactory.getOWLClass(IRI.create(classIRI)); //'caudal fin' 4000164
		OWLObjectProperty haspartp = dataFactory.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BFO_0000051")); //has_part
		OWLClassExpression haspart = dataFactory.getOWLObjectSomeValuesFrom(haspartp, part);

		OWLClass haspartc = dataFactory.getOWLClass(IRI.create("temp001"));
		OWLAxiom ax = dataFactory.getOWLEquivalentClassesAxiom(haspartc, haspart);
		man.addAxiom(ont, ax);
		reasoner.flush();
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);

		NodeSet<OWLClass> subClasses = reasoner.getSubClasses(haspartc, false); //grab all descendant classes
		Iterator<OWLClass> it = subClasses.getFlattened().iterator();
		while(it.hasNext()){
			OWLClass aclass = it.next();
			System.out.println(aclass.getIRI().toString());
			if(aclass.getIRI().toString().compareTo(classIRI)==0){
				man.removeAxiom(ont, ax);
				return true;
			}
		}	
		man.removeAxiom(ont, ax);
		return false;
	}*/

	
	public void dispose() {
		reasoner.dispose();
	}
	
	public OWLOntology getOntology(){return ont;}

	/** Visits existential restrictions and collects the properties which are
	 * restricted */
	private static class RestrictionVisitor extends OWLClassExpressionVisitorAdapter {
		private Set<OWLClass> processedClasses;
		private Set<OWLObjectSomeValuesFrom> restrictedProperties;
		private Set<OWLOntology> onts;

		public RestrictionVisitor(Set<OWLOntology> onts) {
			restrictedProperties = new HashSet<OWLObjectSomeValuesFrom>();
			processedClasses = new HashSet<OWLClass>();
			this.onts = onts;
		}

		public Set<OWLObjectSomeValuesFrom> getRestrictedProperties() {
			return restrictedProperties;
		}

		@Override
		public void visit(OWLClass desc) {
			if (!processedClasses.contains(desc)) {
				// If we are processing inherited restrictions then we
				// recursively visit named supers. Note that we need to keep
				// track of the classes that we have processed so that we don't
				// get caught out by cycles in the taxonomy
				processedClasses.add(desc);
				for (OWLOntology ont : onts) {
					for (OWLSubClassOfAxiom ax : ont.getSubClassAxiomsForSubClass(desc)) {
						OWLClassExpression superCls = ax.getSuperClass();
						if(superCls instanceof OWLObjectSomeValuesFrom){
							restrictedProperties.add((OWLObjectSomeValuesFrom)superCls);
						}
						superCls.accept(this);
					}
				}
			}
		}
	}

	public static void main(String[] argv){
		try {
			ELKReasoner elk = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+"ext.owl"));
			//elk.getClassesWithLateralSides();
			/*String subclassIRI = "http://purl.obolibrary.org/obo/UBERON_0005621";//rhomboid is an organ
			String subclassIRI = "http://purl.obolibrary.org/obo/UBERON_0003098";//optic stalk is not an organ
			String superclassIRI = "http://purl.obolibrary.org/obo/UBERON_0000062"; //organ
			System.out.println(elk.isSubClassOf(subclassIRI, superclassIRI));		
			 */
			//String class1IRI = "http://purl.obolibrary.org/obo/UBERON_0003606"; //limb long bone
			//String class2IRI = "http://purl.obolibrary.org/obo/UBERON_0002495"; //long bone
			//String class2IRI = "http://purl.obolibrary.org/obo/UBERON_0002495"; //organ part, is neck part of organ part? false
			/*String subclass = "http://purl.obolibrary.org/obo/UBERON_4200054";
			String superclass = "http://purl.obolibrary.org/obo/UBERON_4000164";*/
			//String classIRI1 = "http://purl.obolibrary.obo/obo/UBERON_4100000";
			//String classIRI2 = "http://purl.obolibrary.obo/obo/UBERON_4100000";
			//String classIRI1 = "http://purl.obolibrary.org/obo/UBERON_2001794"; //orbitosphenoid-prootic joint
			//String classIRI2 = "http://purl.obolibrary.org/obo/UBERON_0000982"; //skeletal joint
			//String classIRI1 = "http://purl.obolibrary.org/obo/UBERON_0002217"; //synovial joint
			//String classIRI2 = "http://purl.obolibrary.org/obo/UBERON_0000982"; //skeletal joint
			//String classIRI1 = "http://purl.obolibrary.org/obo/UBERON_0011134"; //nosynovial joint
			//System.out.println(elk.isEquivalent(classIRI1, classIRI2)); //class A is not an "equivalent" class of itself
			
			OWLClass joint = elk.dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0000982")); //skeletal joint
			OWLClass joint1 = elk.dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0002217")); //synovial joint
			OWLClass joint2 = elk.dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0011134")); //nonsynovial joint
			OWLClassExpression joint1or2 = elk.dataFactory.getOWLObjectUnionOf(joint1, joint2);
			System.out.println(elk.isEquivalentClass(joint, joint1or2));
			
			//System.out.println(elk.isSubClassOf("http://purl.obolibrary.org/obo/uberon_0010545","http://purl.obolibrary.org/obo/uberon_0010546"));	

			//System.out.println(elk.isSubClassOf("http://purl.obolibrary.org/obo/uberon_0010546","http://purl.obolibrary.org/obo/uberon_0010545"));

			//System.out.println(elk.isPartOf("http://purl.obolibrary.org/obo/UBERON_0001028","http://purl.obolibrary.org/obo/UBERON_0011584"));
		    //elk.isSubPropertyOf("http://purl.obolibrary.org/obo/in_left_side_of","http://purl.obolibrary.org/obo/in_lateral_side_of");
			elk.dispose();

		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			LOGGER.error("", e);
		}
	}

}


