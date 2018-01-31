package outputter.knowledge;

/**
 * @author Hong Cui
 * this class performs reasoning tasks
 * 
 * the conventions used: 
 * 1. flush the reasoner *before* sending a query
 * 2. remove added axioms *after* receiving the result from the reasoner.
 **/
import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectProperty;
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
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;

import outputter.ApplicationUtilities;
import owlaccessor.OWLAccessorImpl;



public class ELKReasoner{
	private static final Logger LOGGER = Logger.getLogger("org.semanticweb.elk");   
	private OWLReasoner reasoner;
	private OWLOntologyManager man = OWLManager.createOWLOntologyManager();
	private OWLDataFactory dataFactory = man.getOWLDataFactory();
	private OWLOntology ont;
	private ElkReasonerFactory reasonerFactory;
	public final static String temp = "TEMP";
	
	private final OWLObjectProperty rel = dataFactory.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BFO_0000050")); //part_of
	private final OWLClass thing = dataFactory.getOWLClass(IRI.create("http://www.w3.org/2002/07/owl#Thing"));//Thing 
	private final OWLObjectProperty lateralside = dataFactory.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BSPO_0000126"));//in_lateral_side_of
	private final OWLObjectProperty partof = dataFactory.getOWLObjectProperty(IRI.create("http://purl.obolibrary.org/obo/BFO_0000050")); //part_of

	public static Hashtable<String,IRI> lateralsidescache = new Hashtable<String,IRI>();//holds classes with lateral sides
	public static Hashtable<String, String> isSubclassOfWithPartCache = new Hashtable<String, String> () ;
	public TreeMap<String,Boolean> subclasscache = new TreeMap<String,Boolean>();//results of isSubClassOf
	public TreeMap<String,Boolean> partofcache = new TreeMap<String,Boolean>();//results of isPartOf
	boolean printmessage = Boolean.valueOf(ApplicationUtilities.getProperty("elk.printmessage"));
	//boolean printmessage = true;
	public static Hashtable<String, String> equivalent = new Hashtable<String, String>();
	static{	
		equivalent.put("http://purl.obolibrary.org/obo/ro_0002220","http://purl.obolibrary.org/obo/pato_0002259"); //adjacent to
		equivalent.put("http://purl.obolibrary.org/obo/bspo_0000096","http://purl.obolibrary.org/obo/pato_0001632");//anterior_to
		equivalent.put("http://purl.obolibrary.org/obo/bspo_0000097","http://purl.obolibrary.org/obo/pato_0001234"); //distal to
		equivalent.put("http://purl.obolibrary.org/obo/bspo_0000098","http://purl.obolibrary.org/obo/pato_0001233");//dorsal_to
		equivalent.put("http://purl.obolibrary.org/obo/obo_rel_located_in","http://purl.obolibrary.org/obo/pato_0002261"); //located in
		equivalent.put("http://purl.obolibrary.org/obo/ro_0002131","http://purl.obolibrary.org/obo/pato_0001590"); //overlap with
		equivalent.put("http://purl.obolibrary.org/obo/bspo_0000099","http://purl.obolibrary.org/obo/pato_0001633"); //posterior to
		equivalent.put("http://purl.obolibrary.org/obo/bspo_0000100","http://purl.obolibrary.org/obo/pato_0001195"); //proximal to
		equivalent.put("http://purl.obolibrary.org/obo/ro_0002221","http://purl.obolibrary.org/obo/pato_0001772"); //surrounding
		equivalent.put("http://purl.obolibrary.org/obo/bspo_0000102","http://purl.obolibrary.org/obo/pato_0001196"); //ventral to
		equivalent.put("http://purl.obolibrary.org/obo/bfo_0000052","http://purl.obolibrary.org/obo/pato_inheres_in");
		
		equivalent.put("http://purl.obolibrary.org/obo/pato_0002259","http://purl.obolibrary.org/obo/pato_0002259"); //adjacent to
		equivalent.put("http://purl.obolibrary.org/obo/pato_0001632","http://purl.obolibrary.org/obo/pato_0001632");//anterior_to
		equivalent.put("http://purl.obolibrary.org/obo/pato_0001234","http://purl.obolibrary.org/obo/pato_0001234"); //distal to
		equivalent.put("http://purl.obolibrary.org/obo/pato_0001233","http://purl.obolibrary.org/obo/pato_0001233");//dorsal_to
		equivalent.put("http://purl.obolibrary.org/obo/pato_0002261","http://purl.obolibrary.org/obo/pato_0002261"); //located in
		equivalent.put("http://purl.obolibrary.org/obo/pato_0001590","http://purl.obolibrary.org/obo/pato_0001590"); //overlap with
		equivalent.put("http://purl.obolibrary.org/obo/pato_0001633","http://purl.obolibrary.org/obo/pato_0001633"); //posterior to
		equivalent.put("http://purl.obolibrary.org/obo/pato_0001195","http://purl.obolibrary.org/obo/pato_0001195"); //proximal to
		equivalent.put("http://purl.obolibrary.org/obo/pato_0001772","http://purl.obolibrary.org/obo/pato_0001772"); //surrounding
		equivalent.put("http://purl.obolibrary.org/obo/pato_0001196","http://purl.obolibrary.org/obo/pato_0001196"); //ventral to
		equivalent.put("http://purl.obolibrary.org/obo/pato_inheres_in","http://purl.obolibrary.org/obo/pato_inheres_in");
	}

	public ELKReasoner(OWLOntology ont, boolean prereason) throws OWLOntologyCreationException{
		if(!this.printmessage) LOGGER.setLevel(Level.ERROR);
		this.ont = ont;
		OWLReasonerFactory reasonerFactory = new ElkReasonerFactory();
		Logger.getLogger("org.semanticweb.elk").setLevel(Level.ERROR);
		reasoner = reasonerFactory.createReasoner(ont);
		//reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		if(prereason) getClassesWithLateralSides();
		else reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
	}

	public ELKReasoner(File ontologyfile, boolean prereason) throws OWLOntologyCreationException{
		if(!this.printmessage) LOGGER.setLevel(Level.ERROR);
		// Load your ontology.
		ont = man.loadOntologyFromOntologyDocument(ontologyfile);
		// Create an ELK reasoner.
		reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ont);
		//reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		if(prereason) getClassesWithLateralSides();//populates the lateral sides cache 
		else reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
	}

	/**
	 * union of (in_lateral_side_of some Thing) and (part_of some in_lateral_side_of some Thing)
	 * this is used to find paired structures and their parts, for example clavicle blade is part of clavicle, which is a paired structure: there are left clavicle and right clavicle. 
	 **/
	void getClassesWithLateralSides(){
		if(!this.printmessage) LOGGER.setLevel(Level.ERROR);
		OWLClassExpression query1 = dataFactory.getOWLObjectSomeValuesFrom(lateralside, thing);
		OWLClassExpression query2 = dataFactory.getOWLObjectSomeValuesFrom(partof, 
				dataFactory.getOWLObjectSomeValuesFrom(lateralside, thing));

		// Create a fresh name for the query1.
		/*OWLClass newName1 = dataFactory.getOWLClass(IRI.create("temp_lateralside_thing"));
		OWLAxiom definition1 = dataFactory.getOWLEquivalentClassesAxiom(newName1,
				query1);
		man.addAxiom(ont, definition1);

		// Create a fresh name for the query2.
		OWLClass newName2 = dataFactory.getOWLClass(IRI.create("temp_partof_lateralside_thing"));
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
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
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
		 */
		Set<OWLClass> subClasses = reasoner.getSubClasses(query1, false).getFlattened();
		subClasses.addAll(reasoner.getSubClasses(query2, false).getFlattened());
		
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
		if(subclasscache.get(subclassIRI+" "+superclassIRI)!=null) return subclasscache.get(subclassIRI+" "+superclassIRI).booleanValue();

		if(this.printmessage) LOGGER.setLevel(Level.ERROR);
		//reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		OWLClass subclass = dataFactory.getOWLClass(IRI.create(subclassIRI));
		OWLClass superclass = dataFactory.getOWLClass(IRI.create(superclassIRI));
		Set<OWLClass> subClasses = reasoner.getSubClasses(superclass, false).getFlattened(); //grab all descendant classes
		boolean result = subClasses.contains(subclass);
		subclasscache.put(subclassIRI+" "+superclassIRI, result);
		return result;
		
		/*NodeSet<OWLClass> subClasses = reasoner.getSubClasses(superclass, false);
		Iterator<OWLClass> it = subClasses.getFlattened().iterator();
		while(it.hasNext()){
			OWLClass aclass = it.next();
			//System.out.println(aclass.getIRI().toString());
			if(aclass.getIRI().toString().compareTo(subclassIRI)==0){
				subclasscache.put(subclassIRI+" "+superclassIRI, new Boolean("true"));
				return true;
			}
		}
		subclasscache.put(subclassIRI+" "+superclassIRI, new Boolean("false"));
		return false;*/
	}	

	public boolean isEquivalent(String id1, String id2){
		
		String classIRI1 = getIRI(id1).toLowerCase();//composes the IRI
		String classIRI2 = getIRI(id2).toLowerCase();//composes the IRI
		if(this.printmessage) LOGGER.setLevel(Level.ERROR);
		
		if(equivalent.get(classIRI1)!=null && equivalent.get(classIRI2)!=null && 
				equivalent.get(classIRI1).compareTo(equivalent.get(classIRI2))==0)
			return true;
		
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		OWLClass class1 = dataFactory.getOWLClass(IRI.create(classIRI1));
		OWLClass class2 = dataFactory.getOWLClass(IRI.create(classIRI2));
		return isEquivalentClass(class1, class2);
	}

	public boolean isEquivalentClass(OWLClass class1, OWLClass class2) {
		// ELK version
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		Node<OWLClass> eqclasses = reasoner.getEquivalentClasses(class1);
		return eqclasses.getEntities().contains(class2);
		
		
		/*OWL API version
		Set<OWLOntology> onts = this.ont.getImportsClosure();
		Set<OWLClassExpression> classes = class1.getEquivalentClasses(onts);
		return classes.contains(class2) || class1.equals(class2);
		*/
	}
	/**
	 * is part a part_of whole?
	 * @param part
	 * @param whole
	 * @return
	 */
	public boolean isPartOf(String part, String whole) {
		if(part==null || whole==null) return false;
		if(partofcache.get(part+" "+whole)!=null) return partofcache.get(part+" "+whole).booleanValue();
		if(this.printmessage) LOGGER.setLevel(Level.ERROR);
		OWLClassExpression partofwhole = dataFactory.getOWLObjectSomeValuesFrom(rel, dataFactory.getOWLClass(IRI.create(whole)));
		// Create a fresh name
		/*OWLClass newclass = dataFactory.getOWLClass(IRI.create("temp_partof_"+whole));
		OWLAxiom axiom = dataFactory.getOWLEquivalentClassesAxiom(newclass,partofclass2);
		man.addAxiom(ont, axiom);
		reasoner.flush();
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		boolean result =  isSubClassOf(part, newclass.getIRI().toString());
		man.removeAxiom(ont, axiom);
		*/
		Set<OWLClass> subclasses = reasoner.getSubClasses(partofwhole, false).getFlattened();
		boolean result = subclasses.contains(dataFactory.getOWLClass(IRI.create(part)));
		
		partofcache.put(part+" "+whole, new Boolean(result));
		return result;
	}


	/**
	 * this method was largely taken from examples published on OWL API website.
	 * but it does not gather all the restrictions ('inherited anonymous classes').
	 * 
	 * is subclassIRI a subclass of something with part partIRI, in other words, 
	 * can this subclassIRI has part partIRI? 
	 * @param subclassIRI
	 * @param partIRI
	 * @return
	 */
	public boolean isSubclassOfWithPart(String subclassIRI, String partIRI){
		if(subclassIRI==null || partIRI==null) return false;
		//cache
		if(this.isSubclassOfWithPartCache.get(subclassIRI+":"+partIRI)!=null) return Boolean.valueOf(this.isSubclassOfWithPartCache.get(subclassIRI+":"+partIRI));
		OWLClass part= dataFactory.getOWLClass(IRI.create(partIRI)); 
		HashSet<OWLClass> classeswithpart = new HashSet<OWLClass>();
		Set<OWLOntology> onts = this.ont.getImportsClosure();
		RestrictionVisitor restrictionVisitor = new RestrictionVisitor(onts);
		for(OWLOntology ont: onts){
			for (OWLSubClassOfAxiom ax : ont
					.getSubClassAxiomsForSubClass(part)) {
				OWLClassExpression superCls = ax.getSuperClass();
				superCls.accept(restrictionVisitor);
			}
		}
		for(OWLClassExpression ce: restrictionVisitor.getClassInRestrictedProperties()){
			if(ce instanceof OWLClass) classeswithpart.add((OWLClass)ce);
		}
		//loop though classeswithpart
		boolean result = false;
		for(OWLClass classwithpart: classeswithpart){
			if(isEquivalentClass(dataFactory.getOWLClass(IRI.create(subclassIRI)), classwithpart)){
				result = true;
				break;
			}
			if(isSubClassOf(subclassIRI, classwithpart.getIRI().toString())){
				result = true;
				break;
			}
		}
		//caching
		this.isSubclassOfWithPartCache.put(subclassIRI+":"+partIRI, result+"");
		return result;
	}

	/**
     * Visits existential restrictions and collects the properties which are restricted
     */
    private static class RestrictionVisitor extends OWLClassExpressionVisitorAdapter {

        private boolean processInherited = true;

        private Set<OWLClass> processedClasses;

        private Set<OWLClassExpression> classInRestrictedProperties;

        private Set<OWLOntology> onts;

        public RestrictionVisitor(Set<OWLOntology> onts) {
            classInRestrictedProperties = new HashSet<OWLClassExpression>();
            processedClasses = new HashSet<OWLClass>();
            this.onts = onts;
        }


        public void setProcessInherited(boolean processInherited) {
            this.processInherited = processInherited;
        }


        public Set<OWLClassExpression> getClassInRestrictedProperties() {
            return classInRestrictedProperties;
        }


        public void visit(OWLClass desc) {
            if (processInherited && !processedClasses.contains(desc)) {
                // If we are processing inherited restrictions then
                // we recursively visit named supers.  Note that we
                // need to keep track of the classes that we have processed
                // so that we don't get caught out by cycles in the taxonomy
                processedClasses.add(desc);
                for (OWLOntology ont : onts) {
                    for (OWLSubClassOfAxiom ax : ont.getSubClassAxiomsForSubClass(desc)) {
                        ax.getSuperClass().accept(this);
                    }
                }
            }
        }


        public void reset() {
            processedClasses.clear();
            classInRestrictedProperties.clear();
        }


        public void visit(OWLObjectSomeValuesFrom desc) {
            // This method gets called when a class expression is an
            // existential (someValuesFrom) restriction and it asks us to visit it
            //classInRestrictedProperties.add(desc.getProperty());
        	if(desc.getProperty().toString().contains("http://purl.obolibrary.org/obo/BFO_0000050"))
        		classInRestrictedProperties.add(desc.getFiller());
        }
    }
		
	/**
	 * this method was largely taken from examples published on OWL API website.
	 * but it does not gather all the restrictions ('inherited anonymous classes').
	 * 
	 * is subclassIRI a subclass of something with part partIRI, in other words, 
	 * can this subclassIRI has part partIRI? 
	 * @param subclassIRI
	 * @param partIRI
	 * @return
	 */
	/*public boolean isSubclassOfWithPart(String subclassIRI, String partIRI){	
		if(this.printmessage) LOGGER.setLevel(Level.ERROR);
		OWLClass part= dataFactory.getOWLClass(IRI.create(partIRI)); //'epichordal lepidotrichium'
		//OWLClass subclaz= dataFactory.getOWLClass(IRI.create(subclassIRI)); //'caudal fin' 4000164

		HashSet<OWLClass> classeswithpart = new HashSet<OWLClass>();
		//made no difference
		reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
		reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_ASSERTIONS);
		reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_HIERARCHY);

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
			if(prop.getProperty().toString().contains(Dictionary.partofiri)){
			//if(prop.getProperty().toString().contains("http://purl.obolibrary.org/obo/BFO_0000050")){
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
	}*/



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
	/*private static class RestrictionVisitor extends OWLClassExpressionVisitorAdapter {
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
	}*/

	public Boolean CheckClassExistence(String id)
	{
		String IRI = getIRI(id);
		 
		org.semanticweb.owlapi.model.IRI url = org.semanticweb.owlapi.model.IRI.create(IRI);
		System.out.println(IRI);
		return ont.containsEntityInSignature(url,true);
	}
	
	public static String getIRI(String id) {
		if(id.startsWith(OWLAccessorImpl.temp)){
			return Dictionary.provisionaliri+id.substring(id.indexOf(":")+1);
		}else{
			return Dictionary.baseiri+id.replace(':', '_');
		}
	}
	

	
	/**
	 * 
	 * @param classIRI
	 * @param relation if Null, default to subclass relation is_a.
	 * @return the subsumers of ClassURI based on the relation.
	 */
	public void getSubsumer(IRI classIRI, String relation, HashSet<OWLClass> subsumers) {
		reasoner.flush();
		OWLClass cls= dataFactory.getOWLClass(classIRI); 
		if(relation==null){ //class subsumption
			Set<OWLClass> supers = reasoner.getSuperClasses(cls, false).getFlattened();
			for(OWLClass sup: supers){
				subsumers.add(sup);
				getSubsumer(sup.getIRI(), relation, subsumers);
			}
		}else if(relation.compareTo(Dictionary.partofiri)==0){ //part_of subsumption
			Set<OWLOntology> onts = this.ont.getImportsClosure();
			RestrictionVisitor restrictionVisitor = new RestrictionVisitor(onts);
			for(OWLOntology ont: onts){
				for (OWLSubClassOfAxiom ax : ont
						.getSubClassAxiomsForSubClass(cls)) {
					OWLClassExpression superCls = ax.getSuperClass();
					superCls.accept(restrictionVisitor);
				}
			}
			for(OWLClassExpression hasPartClass: restrictionVisitor.getClassInRestrictedProperties()){
				if(hasPartClass instanceof OWLClass){
					subsumers.add((OWLClass)hasPartClass);
					//System.out.println(((OWLClass)cls).getIRI() +" is part of "+ ((OWLClass)hasPartClass).getIRI());
					getSubsumer(((OWLClass) hasPartClass).getIRI(), relation, subsumers);
				}
			}
		}
		return;
	}

	public static void main(String[] argv){
		try {
			
			/*ELKReasoner elk = new ELKReasoner(new File("C:\\Users\\updates\\CharaParserTest\\Ontologies\\po.owl"), true);
			String subclass ="http://purl.obolibrary.org/obo/PO_0025332";//disk flower
			String part = "http://purl.obolibrary.org/obo/PO_0009032"; //petal
			System.out.println(elk.isSubclassOfWithPart(subclass, part)); //true: disk flower is subclass of flower which could have petals 
			*/
			//ELKReasoner elk = new ELKReasoner(new File("C:/Users/updates/CharaParserTest/Ontologies", "ext.owl"), true);
			ELKReasoner elk = new ELKReasoner(new File("C:/Users/updates/Desktop", "ext.owl"), true);
			
			IRI cls = IRI.create("http://purl.obolibrary.org/obo/UBERON_0003606");
			HashSet<OWLClass> subsumers = new HashSet<OWLClass>();
			System.out.println("subsumers for "+ cls+" :");
			elk.getSubsumer(cls, Dictionary.partofiri, subsumers);
			for(OWLClass sup: subsumers){
				System.out.println(sup.getIRI());
			}
			/*System.out.println("..........class Exists......."+elk.CheckClassExistence("UBERON:4200047"));//true
			System.out.println("..........class Exists......."+elk.CheckClassExistence("TEMP:4200047")); //false
			System.out.println("..........class Exists......."+elk.CheckClassExistence("UBERON:0011584"));//true

			

			elk.getClassesWithLateralSides();
			System.out.println("1:"+elk.isSubClassOf("http://purl.obolibrary.org/obo/UBERON_0005621",  "http://purl.obolibrary.org/obo/UBERON_0000062"));//true
			System.out.println("2:"+elk.isSubClassOf("http://purl.obolibrary.org/obo/UBERON_0003098",  "http://purl.obolibrary.org/obo/UBERON_0000062"));//false			
			System.out.println("3:"+elk.isPartOf("http://purl.obolibrary.org/obo/UBERON_0001028","http://purl.obolibrary.org/obo/UBERON_0011584")); //true
			System.out.println("4:"+elk.isPartOf("http://purl.obolibrary.org/obo/UBERON_4200050", "http://purl.obolibrary.org/obo/UBERON_0001424"));//cotyla part of ulna? false
			System.out.println("5:"+elk.isSubClassOf("http://purl.obolibrary.org/obo/UBERON_0002389", "http://purl.obolibrary.org/obo/UBERON_0002544"));//true
			System.out.println("6:"+elk.isPartOf("http://purl.obolibrary.org/obo/UBERON_0000976","http://purl.obolibrary.org/obo/BSPO_0000384"));//false

			//String class1IRI = "http://purl.obolibrary.org/obo/UBERON:0003606"; //limb long bone
			//String class2IRI = "http://purl.obolibrary.org/obo/UBERON_0002495"; //long bone
			//String class2IRI = "http://purl.obolibrary.org/obo/UBERON_0002495"; //organ part, is neck part of organ part? false
			//String subclass = "http://purl.obolibrary.org/obo/UBERON_4200054";
			//String superclass = "http://purl.obolibrary.org/obo/UBERON_4000164";
			System.out.println("7:"+elk.isSubClassOf("http://purl.obolibrary.org/obo/uberon_0010545","http://purl.obolibrary.org/obo/uberon_0010546"));//false	
			System.out.println("8:"+elk.isSubClassOf("http://purl.obolibrary.org/obo/uberon_0010546","http://purl.obolibrary.org/obo/uberon_0010545"));//false

			System.out.println("9:"+elk.isPartOf("http://purl.obolibrary.org/obo/uberon_4200047","http://purl.obolibrary.org/obo/uberon_0001274")); //attachment site, ischium: false
			System.out.println("10:"+elk.isSubclassOfWithPart("http://purl.obolibrary.org/obo/uberon_0001274", "http://purl.obolibrary.org/obo/uberon_4200047")); //ischium, attachement site:false
			System.out.println("11:"+elk.isSubclassOfWithPart( "http://purl.obolibrary.org/obo/uberon_3000767", "http://purl.obolibrary.org/obo/uberon_0001274")); //pelvic girdle opening, ischium : true
			System.out.println("12:"+elk.isSubclassOfWithPart("http://purl.obolibrary.org/obo/uberon_0001274", "http://purl.obolibrary.org/obo/uberon_3000767")); //ischium, pelvic girdle opening : true
			*/
			//System.out.println("13:"+elk.isSubclassOfWithPart("http://purl.obolibrary.org/obo/UBERON_0001424", "http://purl.obolibrary.org/obo/UBERON_4200050")); //is ulna a subclass of things with part cotyla? true 
			 
			/*OWLClass joint = elk.dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0000982")); //skeletal joint
			OWLClass joint1 = elk.dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0002217")); //synovial joint
			OWLClass joint2 = elk.dataFactory.getOWLClass(IRI.create("http://purl.obolibrary.org/obo/UBERON_0011134")); //nonsynovial joint
			OWLClassExpression joint1or2 = elk.dataFactory.getOWLObjectUnionOf(joint1, joint2);
			System.out.println(elk.isEquivalentClass(joint, joint1or2));*/
		    //elk.isSubPropertyOf("http://purl.obolibrary.org/obo/in_left_side_of","http://purl.obolibrary.org/obo/in_lateral_side_of");
			
			elk.dispose();
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			LOGGER.error("", e);
		}
	}
}


