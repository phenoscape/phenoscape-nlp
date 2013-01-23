/**
 * 
 */
package conceptmapping;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import oboaccessor.OBO2DB;
import org.semanticweb.owlapi.model.OWLClass;

import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Updates
 *
 */
public class TermOutputerUtilities {
	
	public static ArrayList<OBO2DB> OBOqualityOntoAPIs = new ArrayList<OBO2DB>();
	public static ArrayList<OBO2DB> OBOentityOntoAPIs  = new ArrayList<OBO2DB>();
	public static ArrayList<OWLAccessorImpl> OWLqualityOntoAPIs = new ArrayList<OWLAccessorImpl>();
	public static ArrayList<OWLAccessorImpl> OWLentityOntoAPIs  = new ArrayList<OWLAccessorImpl>();
	public static ArrayList<String> excluded = new ArrayList<String>();
	@SuppressWarnings("unused")
	private static String ontologyfolder;
	private String database;

	public static boolean debug = false;
	public static Hashtable<String, String> singulars = new Hashtable<String, String>();
	public static Hashtable<String, String> plurals = new Hashtable<String, String>();
	static{
		//check cache
		singulars.put("axis", "axis");
		singulars.put("axes", "axis");
		singulars.put("bases", "base");
		singulars.put("boss", "boss");
		singulars.put("buttress", "buttress");
		singulars.put("callus", "callus");
		singulars.put("frons", "frons");
		singulars.put("grooves", "groove");
		singulars.put("interstices", "interstice");
		singulars.put("lens", "len");
		singulars.put("media", "media");
		singulars.put("midnerves", "midnerve");
		singulars.put("process", "process");
		singulars.put("series", "series");
		singulars.put("species", "species");
		singulars.put("teeth", "tooth");
		singulars.put("valves", "valve");
		singulars.put("i", "i"); //could add more roman digits
		singulars.put("ii", "ii");
		singulars.put("iii", "iii");
		
		plurals.put("axis", "axes");
		plurals.put("base", "bases");		
		plurals.put("groove", "grooves");
		plurals.put("interstice", "interstices");
		plurals.put("len", "lens");
		plurals.put("media", "media");
		plurals.put("midnerve", "midnerves");
		plurals.put("tooth", "teeth");
		plurals.put("valve", "valves");
		plurals.put("boss", "bosses");
		plurals.put("buttress", "buttresses");
		plurals.put("callus", "calluses");
		plurals.put("frons", "fronses");
		plurals.put("process", "processes");
		plurals.put("series", "series");
		plurals.put("species", "species");
		plurals.put("i", "i"); //could add more roman digits
		plurals.put("ii", "ii");
		plurals.put("iii", "iii");
	}
	
	public TermOutputerUtilities(String ontologyfolder, String database)throws Exception{
		TermOutputerUtilities.ontologyfolder = ontologyfolder;
		excluded.add("cellular quality");//exclude "cellular quality"
		this.database = database;
		
		//create a list of relative path of the ontologies
		String [] entityontologies = new String[]{

 		ontologyfolder+System.getProperty("file.separator")+"phenoscape-ext.owl",
//		ontologyfolder+System.getProperty("file.separator")+"vertebrate_anatomy.obo",
//		ontologyfolder+System.getProperty("file.separator")+"amniote_draft.obo",
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
		String [] result = {"",""}; 
		if(pato!=null){
			OWLClass c = pato.getClassByLabel(classlabel);
			if(c!=null){
				List<OWLClass> pcs = pato.getParents(c);
				for(OWLClass pc: pcs){
					result[0] += pato.getID(pc)+",";
					result[1] += pato.getLabel(pc)+",";
				}
				result[0] = result[0].replaceFirst(",$", "");
				result[1] = result[1].replaceFirst(",$", "");
			}
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

	/**
	 * Search a term in a subgroup of an ontology
	 * Currently, only applies to PATO relational slim
	 * @param term
	 * @param type: entity or quality
	 * @return ArrayList of results, one result from an ontology 
	 */
	public ArrayList<String[]> searchOntologies(String term, String type, int subgroup) throws Exception {
		//search quality ontologies
		ArrayList<String[]> results = new ArrayList<String[]>();
		//boolean added = false;
		if(type.compareTo("quality")==0){
			for(OWLAccessorImpl api: OWLqualityOntoAPIs){
				String[] result = searchOWLOntology(term, api, type, subgroup);
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

	
	private String[] searchOWLOntology(String term, OWLAccessorImpl owlapi, String type, int subgroup) throws Exception {
		String[] result = null;
		List<OWLClass> matches = (ArrayList<OWLClass>)owlapi.retrieveConcept(term);
		
		//task 2 matches can be null, if the term is looked up into other ontologies - modified by Hariharan

				if(matches!=null)
				{
		Iterator<OWLClass> it = matches.iterator();
		
		//exact match first
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			String id = owlapi.getID(c);
			if(label.compareToIgnoreCase(term)==0){
				result= new String[3];
				result[0] = type;
				result[1] = id;//id
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
			String id = owlapi.getID(c);
			result[0] = type;
			result[1] += id+";";
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
				return null;
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
		List<OWLClass> matches = (ArrayList<OWLClass>)owlapi.retrieveConcept(term);
		
		//Task 2 matches can be null, if the term is looked up into other ontologies - modified by Hariharan
				if(matches!=null)
				{
		Iterator<OWLClass> it = matches.iterator();
		
		//exact match first
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			String id = owlapi.getID(c);
			if(label.compareToIgnoreCase(term)==0){
				result= new String[3];
				result[0] = type;
				result[1] = id;//id
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
			String id = owlapi.getID(c);
			result[0] = type;
			result[1] += id+";";
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
				return null;
	}

	
	/*
	 * copied from fna.charactermarkup.Utilities
	 * */
	public static String checkWN(String cmdtext){
		try{
	 	  		Runtime r = Runtime.getRuntime();	
		  		Process proc = r.exec(cmdtext);
			    ArrayList<String> errors = new ArrayList<String>();
		  	    ArrayList<String> outputs = new ArrayList<String>();
		  
	            // any error message?
	            //StreamGobbler errorGobbler = new 
	                //StreamGobblerWordNet(proc.getErrorStream(), "ERROR", errors, outputs);            
	            
	            // any output?
	            StreamGobbler outputGobbler = new 
	                StreamGobblerWordNet(proc.getInputStream(), "OUTPUT", errors, outputs);
	                
	            // kick them off
	            //errorGobbler.start();
	            
	            outputGobbler.start();
	            //outputGobbler.gobble();
	                                    
	            // any error???
	            int exitVal = proc.waitFor();
	            //System.out.println("ExitValue: " + exitVal);

	            StringBuffer sb = new StringBuffer();
	            for(int i = 0; i<outputs.size(); i++){
	            	//sb.append(errors.get(i)+" ");
	            	sb.append(outputs.get(i)+" ");
	            }
	            return sb.toString();
				
		  	}catch(Exception e){
		  		e.printStackTrace();
		  	}
		  	return "";
	}
	////////////////////////////////////////////////////////////////////////
	
	/**
	 * return null : word not in WN
	 * return ""   : word is not a noun or is singular
	 * return aword: word is a pl and singular form is returned
	 */
	public static String checkWN4Singular(String word){
		
		String result = checkWN("wn "+word+" -over");
		if (result.length()==0){//word not in WN
			return null;
		}
		//found word in WN:
		String t = "";
		Pattern p = Pattern.compile("(.*?)Overview of noun (\\w+) (.*)");
		Matcher m = p.matcher(result);
		while(m.matches()){
			 t += m.group(2)+" ";
			 result = m.group(3);
			 m = p.matcher(result);
		}
		if (t.length() ==0){//word is not a noun
			return "";
		} 
		String[] ts = t.trim().split("\\s+"); //if multiple singulars (bases =>basis and base, pick the first one
		for(int i = 0; i<ts.length; i++){
			if(ts[i].compareTo(word)!=0){//find a singular form
				return ts[i];
			}
		}
		return "";//original is a singular
	}
	
	public static boolean isPlural(String t) {
		t = t.replaceAll("\\W", "");
		if(t.matches("\\b(series|species|fruit)\\b")){
			return true;
		}
		if(t.compareTo(toSingular(t))!=0){
			return true;
		}
		return false;
	}

	public static String toSingular(String word){
		String s = "";
		word = word.toLowerCase().replaceAll("[(){}]", "").trim(); //bone/tendon

		s = singulars.get(word);
		if(s!=null) return s;
		
		if(word.matches("\\w+_[ivx-]+")){
			singulars.put(word, word);
			plurals.put(word, word);
			return word;
		}
		
		//adverbs
		if(word.matches("[a-z]{3,}ly")){
			singulars.put(word, word);
			plurals.put(word, word);
			return word;
		}
		
		String wordcopy = word;
		wordcopy = checkWN4Singular(wordcopy);
		if(wordcopy != null && wordcopy.length()==0){
			return word;
		}else if(wordcopy!=null){
			singulars.put(word, wordcopy);
			if(!wordcopy.equals(word)) plurals.put(wordcopy, word);
			if(debug) System.out.println("["+word+"]'s singular is "+wordcopy);
			return wordcopy;
		}else{//word not in wn
		
			Pattern p1 = Pattern.compile("(.*?[^aeiou])ies$");
			Pattern p2 = Pattern.compile("(.*?)i$");
			Pattern p3 = Pattern.compile("(.*?)ia$");
			Pattern p4 = Pattern.compile("(.*?(x|ch|sh|ss))es$");
			Pattern p5 = Pattern.compile("(.*?)ves$");
			Pattern p6 = Pattern.compile("(.*?)ices$");
			Pattern p7 = Pattern.compile("(.*?a)e$");
			Pattern p75 = Pattern.compile("(.*?)us$");
			Pattern p8 = Pattern.compile("(.*?)s$");
			//Pattern p9 = Pattern.compile("(.*?[^aeiou])a$");
			
			Matcher m1 = p1.matcher(word);
			Matcher m2 = p2.matcher(word);
			Matcher m3 = p3.matcher(word);
			Matcher m4 = p4.matcher(word);
			Matcher m5 = p5.matcher(word);
			Matcher m6 = p6.matcher(word);
			Matcher m7 = p7.matcher(word);
			Matcher m75 = p75.matcher(word);
			Matcher m8 = p8.matcher(word);
			//Matcher m9 = p9.matcher(word);
			
			if(m1.matches()){
			  s = m1.group(1)+"y";
			}else if(m2.matches()){
			  s = m2.group(1)+"us";
			}else if(m3.matches()){
			  s = m3.group(1)+"ium";
			}else if(m4.matches()){
			  s = m4.group(1);
			}else if(m5.matches()){
			  s = m5.group(1)+"f";
			}else if(m6.matches()){
			  s = m6.group(1)+"ex";
			}else if(m7.matches()){
			  s = m7.group(1);
			}else if(m75.matches()){
			  s = word;
			}else if(m8.matches()){
			  s = m8.group(1);
			}//else if(m9.matches()){
			//  s = m9.group(1)+"um";
			//}
		  
		  if(s != null){
			if(debug) System.out.println("["+word+"]'s singular is "+s);
			singulars.put(word, s);
			if(!s.equals(word)) plurals.put(s, word);
			return s;
		  }
		}
		if(debug) System.out.println("["+word+"]'s singular is "+word);
		return word;
	}
	
	public static String plural(String b) {
		return TermOutputerUtilities.plurals.get(b);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
