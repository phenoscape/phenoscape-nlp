/**
 * 
 */
package conceptmapping;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import oboaccessor.OBO2DB;
import org.semanticweb.owlapi.model.OWLClass;
import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Updates
 *
 */
public class Utilities {
	
	public static ArrayList<OBO2DB> OBOqualityOntoAPIs = new ArrayList<OBO2DB>();;
	public static ArrayList<OBO2DB> OBOentityOntoAPIs  = new ArrayList<OBO2DB>();
	public static ArrayList<OWLAccessorImpl> OWLqualityOntoAPIs = new ArrayList<OWLAccessorImpl>();;
	public static ArrayList<OWLAccessorImpl> OWLentityOntoAPIs  = new ArrayList<OWLAccessorImpl>();
	public static ArrayList<String> excluded = new ArrayList<String>();

	static{
		excluded.add("cellular quality");
		
		String [] entityontologies = new String[]{
 		"C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source\\tao.owl",
		"C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\archosaur\\vertebrate_anatomy.obo",
		"C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\archosaur\\amniote_draft.obo"};
		String [] qualityontologies = new String[]{
		"C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source\\pato.owl"};
		 
		/*
		entityOntoPaths.add("http://purl.obolibrary.org/obo/tao.owl");
		entityOntoPaths.add("https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/skeletal/obo/vertebrate_anatomy.obo");
		entityOntoPaths.add("https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/amniote_draft.obo");
		qualityOntoPaths.add("http://www.berkeleybop.org/ontologies/pato.owl");
		*/
		
		for(String onto: entityontologies){
			if(onto.endsWith(".owl")){
				OWLAccessorImpl api = new OWLAccessorImpl(new File(onto));
				OWLentityOntoAPIs.add(api);
			}else if(onto.endsWith(".obo")){
				int i = onto.lastIndexOf("/");
				int j = onto.lastIndexOf("\\");
				i = i>j? i:j;
				String ontoname = onto.substring(i+1).replaceFirst("\\.obo", "");
				OBO2DB o2d = new OBO2DB("obo", onto ,ontoname);
				OBOentityOntoAPIs.add(o2d);
			}
		}
		
		for(String onto: qualityontologies){
			if(onto.endsWith(".owl")){
				OWLAccessorImpl api = new OWLAccessorImpl(new File(onto));
				OWLqualityOntoAPIs.add(api);
			}else if(onto.endsWith(".obo")){
				int i = onto.lastIndexOf("/");
				int j = onto.lastIndexOf("\\");
				i = i>j? i:j;
				String ontoname = onto.substring(i+1).replaceFirst("\\.obo", "");
				OBO2DB o2d = new OBO2DB("obo", onto ,ontoname);
				OBOqualityOntoAPIs.add(o2d);
			}
		}
		
	}
	

	/**
	 * 
	 * @param term
	 * @param type: entity or quality
	 * @return ArrayList of results, one result from an ontology 
	 */
	public static ArrayList<String[]> searchOntologies(String term, String type) {
		//search quality ontologies
		ArrayList<String[]> results = new ArrayList<String[]>();
		//boolean added = false;
		if(type.compareTo("quality")==0){
			for(OWLAccessorImpl api: OWLqualityOntoAPIs){
				String[] result = searchOWLOntology(term, api, type);
				if(result!=null){
					//added = true;
					results.add(result);
				}
			}			
			for(OBO2DB o2d: OBOqualityOntoAPIs){
				String[] result = searchOBOOntology(term, o2d, type);
				if(result!=null){
					//added = true;
					results.add(result);
				}
			}
		}else if(type.compareTo("entity")==0){
			for(OWLAccessorImpl api: OWLentityOntoAPIs){
				String[] result = searchOWLOntology(term, api, type);
				if(result!=null){
					//added = true;
					results.add(result);
				}
			}			
			for(OBO2DB o2d: OBOentityOntoAPIs){
				String[] result = searchOBOOntology(term, o2d, type);
				if(result!=null){
					//added = true;
					results.add(result);
				}
			}
		}
		return results;
		//if(added){
		//	return results;
		//}else{
		//	return null;
		//}
	}

	private static String[] searchOBOOntology(String term, OBO2DB o2d, String type) {
		String [] result = new String[3]; //an array with three elements: type, id, and label
		String[] match = o2d.getID(term);
		if(match !=null){
			result[0] = type;
			result[1] = match[0]; //id
			result[2] = match[1]; //label
		}else{
			result = null;
		}
		return result;
	}

	/**
	 * 
	 * @param term
	 * @param owlapi
	 * @param type
	 * @return array of 3 elements: 0: type; 1:ID; 2:label
	 */
	private static String[] searchOWLOntology(String term, OWLAccessorImpl owlapi, String type) {
		String[] result = null;
		List<OWLClass> matches = owlapi.retrieveConcept(term, excluded);
		Iterator<OWLClass> it = matches.iterator();
		
		//exact match first
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			if(label.compareToIgnoreCase(term)==0){
				result= new String[3];
				result[0] = type;
				result[1] = c.toString().replaceFirst("http.*?(?=(PATO|TAO|AMAO|VAO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "");//id
				result[2] = label;
				return result;
			}
		}
		//otherwise, append all possible matches
		it = matches.iterator();
		result = new String[]{"", "", ""};
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			result[0] = type;
			result[1] += c.toString().replaceFirst(".*http.*?(?=(PATO|TAO|AMAO|VAO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "")+";";
			result[2] += label+";";
		}
		if(result[1].length()>0){
			result[1] = result[1].replaceFirst(";$", "");
			result[2] = result[2].replaceFirst(";$", "");
			return result;
		}else{
			return null;
		}
	}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
