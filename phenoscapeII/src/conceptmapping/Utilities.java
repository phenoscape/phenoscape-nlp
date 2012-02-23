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
	
	public static ArrayList<String> qualityOntoPaths = new ArrayList<String>();;
	public static ArrayList<String> entityOntoPaths  = new ArrayList<String>();
	public static ArrayList<String> excluded = new ArrayList<String>();

	static{
		excluded.add("cellular quality");
		entityOntoPaths.add("C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source\\tao.owl");
		entityOntoPaths.add("C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\archosaur\\vertebrate_anatomy.obo");
		entityOntoPaths.add("C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\archosaur\\amniote_draft.obo");
		qualityOntoPaths.add("C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source\\pato.owl");
		 
		/*
		entityOntoPaths.add("http://purl.obolibrary.org/obo/tao.owl");
		entityOntoPaths.add("https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/skeletal/obo/vertebrate_anatomy.obo");
		entityOntoPaths.add("https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/amniote_draft.obo");
		qualityOntoPaths.add("http://www.berkeleybop.org/ontologies/pato.owl");
		*/
	}
	

	/**
	 * 
	 * @param term
	 * @param type: entity or quality
	 * @return 
	 */
	public static ArrayList<String[]> searchOntologies(String term, String type) {
		//search quality ontologies
		ArrayList<String[]> results = new ArrayList<String[]>();
		boolean added = false;
		if(type.compareTo("quality")==0){
			for(String qonto: qualityOntoPaths){
				if(qonto.endsWith(".owl")){
					OWLAccessorImpl owlapi = new OWLAccessorImpl(new File(qonto));
					//OWLAccessorImpl owlapi = new OWLAccessorImpl(qonto);
					String[] result = searchOWLOntology(term, owlapi, type);
					if(result!=null){
						added = true;
						results.add(result);
					}
				}else if(qonto.endsWith(".obo")){
					String[] result = searchOBOOntology(term, qonto, type);
					if(result!=null){
						added = true;
						results.add(result);
					}
				}
			}
		}else if(type.compareTo("entity")==0){
			for(String eonto: entityOntoPaths){
				if(eonto.endsWith(".owl")){
					OWLAccessorImpl owlapi = new OWLAccessorImpl(new File(eonto));
					//OWLAccessorImpl owlapi = new OWLAccessorImpl(eonto);
					String[] result = searchOWLOntology(term, owlapi, type);
					if(result!=null){
						added = true;
						results.add(result);
					}
				}else if(eonto.endsWith(".obo")){
					String[] result = searchOBOOntology(term, eonto, type);
					if(result!=null){
						added = true;
						results.add(result);
					}
				}
			}
		}
		if(added){
			return results;
		}else{
			return null;
		}
	}

	private static String[] searchOBOOntology(String term, String ontofile, String type) {
		String [] result = new String[3]; //an array with three elements: type, id, and label
		int i = ontofile.lastIndexOf("/");
		int j = ontofile.lastIndexOf("\\");
		i = i>j? i:j;
		String ontoname = ontofile.substring(i+1).replaceFirst("\\.obo", "");
		OBO2DB o2d = new OBO2DB("obo", ontofile ,ontoname);
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


	private static String[] searchOWLOntology(String term, OWLAccessorImpl owlapi, String type) {
		String[] result = null;
		List<OWLClass> matches = owlapi.retrieveConcept(term, this.excluded);
		Iterator<OWLClass> it = matches.iterator();
		
		//exact match first
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			if(label.compareToIgnoreCase(term)==0){
				result= new String[3];
				result[0] = type;
				result[1] = c.toString().replaceFirst("http.*?(?=(PATO|TAO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "");//id
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
			result[1] += c.toString().replaceFirst(".*http.*?(?=(PATO|TAO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "")+";";
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
