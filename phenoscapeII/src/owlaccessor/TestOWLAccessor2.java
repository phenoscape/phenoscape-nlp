package owlaccessor;

import org.junit.Test;

public class TestOWLAccessor2 {

	@Test
	public void testGetKeyWords() {
		OWLAccessor a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/pato.owl");
		for (String k :a.getKeywords(a.retrieveConcept("acute angle").get(0))){
			//the first match term 
			//(if PATO only contains unique terms, then only one term per list)
			System.out.println(k);
			
		}
		
		System.out.println(a.getAllClasses().size());
	}
	@Test
	public void testGetParentLabels(){
		OWLAccessor a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/pato.owl");
		
		for (String p : a.getParentsLabels(a.retrieveConcept("increased fragility").get(0))){
			System.out.println(p);
		}
		
	}
	
	@Test
	public void testDBMigrater(){
		DBMigrater dbm = new DBMigrater();
		//dbm.migrateRelations();
	}

}
