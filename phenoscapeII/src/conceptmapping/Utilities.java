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
	
	public static ArrayList<OBO2DB> OBOqualityOntoAPIs = new ArrayList<OBO2DB>();
	public static ArrayList<OBO2DB> OBOentityOntoAPIs  = new ArrayList<OBO2DB>();
	public static ArrayList<OWLAccessorImpl> OWLqualityOntoAPIs = new ArrayList<OWLAccessorImpl>();
	public static ArrayList<OWLAccessorImpl> OWLentityOntoAPIs  = new ArrayList<OWLAccessorImpl>();
	public static ArrayList<String> excluded = new ArrayList<String>();
	@SuppressWarnings("unused")
	private static String ontologyfolder;
	private String database;

	public Utilities(String ontologyfolder, String database)throws Exception{
		Utilities.ontologyfolder = ontologyfolder;
		excluded.add("cellular quality");//exclude "cellular quality"
		this.database = database;
		
		//create a list of relative path of the ontologies
		String [] entityontologies = new String[]{
 		ontologyfolder+System.getProperty("file.separator")+"tao.owl",
		ontologyfolder+System.getProperty("file.separator")+"vertebrate_anatomy.obo",
		ontologyfolder+System.getProperty("file.separator")+"amniote_draft.obo",
		ontologyfolder+System.getProperty("file.separator")+"bspo.owl"};
		String [] qualityontologies = new String[]{
		ontologyfolder+System.getProperty("file.separator")+"pato.owl"};
		 
		/*
		entityOntoPaths.add("http://purl.obolibrary.org/obo/tao.owl");
		entityOntoPaths.add("https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/skeletal/obo/vertebrate_anatomy.obo");
		entityOntoPaths.add("https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/amniote_draft.obo");
		entityOntoPaths.add("http://www.berkeleybop.org/ontologies/bspo.owl");
		qualityOntoPaths.add("http://www.berkeleybop.org/ontologies/pato.owl");
		*/
		
		//for each entity ontology
		for(String onto: entityontologies){
			if(onto.endsWith(".owl")){
				OWLAccessorImpl api = new OWLAccessorImpl(new File(onto), new ArrayList<String>());
				OWLentityOntoAPIs.add(api);
			}else if(onto.endsWith(".obo")){
				int i = onto.lastIndexOf("/");
				int j = onto.lastIndexOf("\\");
				i = i>j? i:j;
				String ontoname = onto.substring(i+1).replaceFirst("\\.obo", "");
				OBO2DB o2d = new OBO2DB(database, onto ,ontoname);
				OBOentityOntoAPIs.add(o2d);
			}
		}
		
		for(String onto: qualityontologies){
			if(onto.endsWith(".owl")){
				OWLAccessorImpl api = new OWLAccessorImpl(new File(onto), excluded);
				OWLqualityOntoAPIs.add(api);
			}else if(onto.endsWith(".obo")){
				int i = onto.lastIndexOf("/");
				int j = onto.lastIndexOf("\\");
				i = i>j? i:j;
				String ontoname = onto.substring(i+1).replaceFirst("\\.obo", "");
				OBO2DB o2d = new OBO2DB(database, onto ,ontoname);
				OBOqualityOntoAPIs.add(o2d);
			}
		}
	}

	
	public String[] retreiveParentInfoFromPATO (String classlabel){
		//find OWL PATO
		OWLAccessorImpl pato = null;
		for(OWLAccessorImpl api: OWLqualityOntoAPIs){
			if(api.getSource().indexOf("pato")>=0){
				pato = api;
				break;
			}
		}
		//find parent
		String [] result = null; 
		if(pato!=null){
			OWLClass c = pato.getClassByLabel(classlabel);
			List<OWLClass> pcs = pato.getParents(c);
			result = new String[2]; //0: ID; 1:label
			for(OWLClass pc: pcs){
				result[0] += pato.getID(pc)+",";
				result[1] += pato.getLabel(pc)+",";
			}
			result[0] = result[0].replaceFirst(",$", "");
			result[1] = result[1].replaceFirst(",$", "");
		}		
		return result;
	}
	

	/**
	 * 
	 * @param term
	 * @param type: entity or quality
	 * @return ArrayList of results, one result from an ontology 
	 */
	public ArrayList<String[]> searchOntologies(String term, String type) throws Exception {
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

	private String[] searchOBOOntology(String term, OBO2DB o2d, String type) throws Exception{
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
	private String[] searchOWLOntology(String term, OWLAccessorImpl owlapi, String type) throws Exception {
		String[] result = null;
		List<OWLClass> matches = owlapi.retrieveConcept(term);
		Iterator<OWLClass> it = matches.iterator();
		
		//exact match first
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			if(label.compareToIgnoreCase(term)==0){
				result= new String[3];
				result[0] = type;
				result[1] = c.toString().replaceFirst("http.*?(?=(PATO|TAO|AMAO|VAO|BSPO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "");//id
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
			result[1] += c.toString().replaceFirst(".*http.*?(?=(PATO|TAO|AMAO|VAO|BSPO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "")+";";
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
