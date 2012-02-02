package owlaccessor;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.model.OWLClass;

public class TestOWLAccessor2 {

//	@Test
//	public void testGetKeyWords() {
//		OWLAccessor a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/pato.owl");
//		for (String k :a.getKeywords(a.retrieveConcept("acute angle").get(0))){
//			//the first match term 
//			//(if PATO only contains unique terms, then only one term per list)
//			System.out.println(k);
//			
//		}
//		
//		System.out.println(a.getAllClasses().size());
//	}
//	@Test
//	public void testGetParentLabels(){
//		OWLAccessor a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/pato.owl");
//		
//		for (String p : a.getParentsLabels(a.retrieveConcept("increased fragility").get(0))){
//			System.out.println(p);
//		}
//		
//	}
//	
//	@Test
//	public void testDBMigrater(){
//		DBMigrater dbm = new DBMigrater();
//		//dbm.migrateRelations();
//	}
//
//	@Test
//	public void testGetAllOffSprings(){
//		OWLAccessor a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/pato.owl");
//		
//		
//		for(String c :a.getAllOffSprings(a.getClassByLabel("shape"))){
//			System.out.println(c);
//		}
//		System.out.println(a.getAllOffSprings(a.getClassByLabel("shape")).size());
//	}
//	
	@Test
	public void testRetriveConcept(){
		OWLAccessor a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/pato.owl");
		
		List<OWLClass> l =  a.retrieveConcept("dslfhsfhaskhskaf");
		
		System.out.println(l.size());
		
		
		
	}

}
