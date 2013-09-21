package owlaccessor;

import java.util.ArrayList;

import org.junit.Test;

import outputter.knowledge.Dictionary;


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
//	@Test
//	public void testRetriveConcept(){
//		OWLAccessor a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/tao.owl");
//		
//		List<OWLClass> l =  a.retrieveConcept("tooth");
//		
//		System.out.println(l.size());
//		
//		System.out.println(a.getID(a.getClassByLabel("tooth")));
//		
//	}
//	
//	@Test
//	public void testGetID(){
//		OWLAccessor a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/pato.owl");
//		List<OWLClass> l =  a.retrieveConcept("shape");
//		
//		System.out.println("("+a.getLabel(l.get(0))+", "+a.getID(l.get(0))+")");//output the (term, id) pair. 
//	}

//	@Test
//	public void testGetLastWord(){
//		DBMigrater dbm = new DBMigrater();
//		
//		assertTrue(dbm.getLastWord("est;").equals("est;"));
//		assertTrue(dbm.getLastWord("thi is a t est;").equals("est;"));
//		assertTrue(dbm.getLastWord("").equals(""));
//		assertTrue(dbm.getLastWord("	").equals(""));
//		assertTrue(dbm.getLastWord(" ").equals(""));
//	}
	
	@Test
	public void testGetAllOffsprings() throws Exception{
		OWLAccessor a = new OWLAccessorImpl("http://www.berkeleybop.org/ontologies/pato.owl", new ArrayList<String>());
		for(String s:a.getAllOffspringLables(a.getOWLClassByIRI(Dictionary.cellquality))){
			System.out.println(s);
		}
	}
}
