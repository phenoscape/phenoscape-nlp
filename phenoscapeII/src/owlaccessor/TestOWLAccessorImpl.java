package owlaccessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;


public class TestOWLAccessorImpl {
	
//	public String[] searchOntology(String term, String ontofilepath, String type) {
//		OWLAccessorImpl owlapi = new OWLAccessorImpl(new File(ontofilepath));
//		List<OWLClass> matches = owlapi.retrieveConcept(term);
//		Iterator<OWLClass> it = matches.iterator();
//		String[] result = null;
//		while(it.hasNext()){
//			OWLClass c = it.next();
//			String label = owlapi.getLabel(c);
//			if(label.compareToIgnoreCase(term)==0){
//				result= new String[3];
//				result[0] = type;
//				result[1] = c.toString().replaceFirst("http.*?(?=(PATO|TAO)_)", "").replaceFirst("_", ":").replaceFirst(">$", "");
//				result[2] = label;
//				return result;
//			}
//		}
//		it = matches.iterator();
//		result = new String[]{"", "", ""};
//		while(it.hasNext()){
//			OWLClass c = it.next();
//			String label = owlapi.getLabel(c);
//			result[0] = type;
//			result[1] += c.toString().replaceFirst(".*http.*?(?=(PATO|TAO)_)", "").replaceFirst("_", ":").replaceFirst(">$", "")+";";
//			result[2] += label+";";
//		}
//		if(result!=null){
//			result[1] = result[1].replaceFirst(";$", "");
//			result[2] = result[2].replaceFirst(";$", "");
//		}
//		return result;
//	}
//	
//
//	@Test
//	public void test() {
//		
//		
		
		
		
		/*String ontoURL ="http://www.co-ode.org/ontologies/pizza/pizza.owl" ;
		OWLAccessor acc = new OWLAccessorImpl(ontoURL);
		acc.retrieveConcept("quality");
	
		OWLOntologyManager manager=OWLManager.createOWLOntologyManager();
		//df=manager.getOWLDataFactory();
		
		//access TAO Ontology on web
		//IRI iri = IRI.create("http://www.berkeleybop.org/ontologies/tao.owl");
		//access PATO Ontology on web
		//IRI iri = IRI.create("http://www.berkeleybop.org/ontologies/pato.owl");
		//IRI iri=IRI.create("http://www.co-ode.org/ontologies/pizza/pizza.owl");
		IRI iri = IRI.create(ontoURL);
		try {
			OWLOntology ont = manager.loadOntologyFromOntologyDocument(iri);
			assertTrue(ont.containsClassInSignature(IRI.create("http://purl.obolibrary.org/obo/PATO_0000001")));
			Set<OWLEntity> en=ont.getEntitiesInSignature(IRI.create("http://purl.obolibrary.org/obo/PATO_0000001"));
			assertTrue(en.size()==1);
			for(OWLEntity ent:en){
				for(OWLAnnotation a:ent.getAnnotations(ont)){
					if(a.getProperty().isLabel()){
						String label=((OWLAccessorImpl)acc).getRefinedOutput(a.getValue().toString());
						assertTrue(label.contains("quality")||label.equals("quality"));
					}
				}
			}
			
		} catch (OWLOntologyCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
//	}
	
	@Test
	public void testGetAnnotationProperties() throws Exception{
		File phenoscape = new File("C:\\Users\\Zilong Chang\\Downloads\\phenoscape-ext.owl");
		//String url = "http://obo.svn.sourceforge.net/viewvc/obo/uberon/trunk/merged.owl";
		OWLAccessorImpl a = new OWLAccessorImpl(phenoscape, new ArrayList<String>());
		
		//OWLAccessorImpl a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/pato.owl");
		//OWLAccessorImpl a = new OWLAccessorImpl("http://purl.obolibrary.org/obo/tao.owl", new ArrayList<String>());
		//System.out.println(a.getLabel(a.getClassByLabel("cellular quality")));
		//for(OWLAnnotation oa : a.getExactSynonyms(a.getClassByLabel("color"))){
		//	System.out.println(oa.toString());
		//}
		
		//List<String> s = new ArrayList<String>();
		//System.out.println(a.retrieveConcept("pterotic-supracleithrum").size());
		List<OWLClass> results = a.retrieveConcept("anal fin");
		for(OWLClass c : results){
			System.out.println(c.getIRI());
		}
		
		//s.add("cellular quality");
		//System.out.println(a.retrieveConcept("ploidy").size());
		
	}
	
	//public static void main(String[] args) {
	//	TestOWLAccessorImpl to = new TestOWLAccessorImpl();
	//	String path="C:/Documents and Settings/Hong Updates/Desktop/Australia/phenoscape-fish-source/tao.owl";
	//	to.searchOntology("aorta", path, "TAO");
		
	//}
	

}
