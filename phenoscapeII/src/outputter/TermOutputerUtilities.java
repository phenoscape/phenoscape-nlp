/**
 * 
 */
package outputter;

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
	private static String ontologyfolder = ApplicationUtilities.getProperty("ontology.dir");
	private static String[] entityontologies;
	private static String[] qualityontologies;
	//private String database;

	public static boolean debug = false;
	public static String attributes = "";
	
		
		//note, the order of the ontolgies listed in the string imply the importance of the ontologies:
		//(they will also be searched, but if a term match in multiple ontology, the first match is taken as the result)
	static{
		//TODO:add GO:bioprocess
		entityontologies = new String[]{
				ontologyfolder+System.getProperty("file.separator")+"ext.owl",
				ontologyfolder+System.getProperty("file.separator")+"bspo.owl"
				};
		qualityontologies = new String[]{
				ontologyfolder+System.getProperty("file.separator")+"pato.owl"
		};
		
		//for each entity ontology
		for(String onto: entityontologies){
			if(onto.endsWith(".owl")){
				OWLAccessorImpl api = new OWLAccessorImpl(new File(onto), new ArrayList<String>());
				OWLentityOntoAPIs.add(api);
				//this.alladjectiveorgans.add(api.adjectiveorgans);
			}/*else if(onto.endsWith(".obo")){ //no longer take OBO format
				int i = onto.lastIndexOf("/");
				int j = onto.lastIndexOf("\\");
				i = i>j? i:j;
				String ontoname = onto.substring(i+1).replaceFirst("\\.obo", "");
				OBO2DB o2d = new OBO2DB(database, onto ,ontoname);
				OBOentityOntoAPIs.add(o2d);
			}*/
		}
		
		//for each entity ontology
		for(String onto: qualityontologies){
			if(onto.endsWith(".owl")){
				OWLAccessorImpl api = new OWLAccessorImpl(new File(onto), excluded);
				attributes += "|"+api.getLowerCaseAttributeSlimStringPattern();
				attributes = attributes.replaceAll("(^\\||\\|$)", "");
				OWLqualityOntoAPIs.add(api);
			}/*else if(onto.endsWith(".obo")){
				int i = onto.lastIndexOf("/");
				int j = onto.lastIndexOf("\\");
				i = i>j? i:j;
				String ontoname = onto.substring(i+1).replaceFirst("\\.obo", "");
				OBO2DB o2d = new OBO2DB(database, onto ,ontoname);
				OBOqualityOntoAPIs.add(o2d);
			}*/
		}
		excluded.add(Dictionary.cellquality);//exclude "cellular quality"
	}
	
	
	/**
	 * constructor may be needed if we need to exclude different parts of ontology.
	 * @param ontologyfolder
	 */
	public TermOutputerUtilities(/*String ontologyfolder, String database*/){

	}

	/**
	 * search up the is_a path until one of the parent class is identified. 
	 * @param classlabel
	 * @return an array with two element: ids and labels of the parents
	 */
	/*public String[] retreiveParentInfoFromPATO (String classid){
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
			OWLClass c = pato.getClassByIRI(Dictionary.patoiri+classid.replaceAll(":", "_"));
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
	}*/
	
	public static String[] retreiveParentInfoFromPATO (String classid){
		//find OWL PATO
		OWLAccessorImpl pato = null;
		for(OWLAccessorImpl api: OWLqualityOntoAPIs){
			if(api.getSource().indexOf("pato")>=0){
				pato = api;
				break;
			}
		}
		//find parent
		
		if(pato!=null){
			String [] result = {"",""}; 
			return findTargetParent(pato, classid, result);
		}		
		return null;
	}
	
	private static String[] findTargetParent(OWLAccessorImpl pato, String classid, String[] result){
		OWLClass c = pato.getClassByIRI(Dictionary.patoiri+classid.replaceAll(":", "_"));
		if(c!=null){
			 if(pato.getLabel(c).matches("\\b"+Dictionary.patoupperclasses+"\\b")){
				 result[0] = pato.getID(c);
				 result[1] = pato.getLabel(c);
				 return result;
			}else{
				List<OWLClass> pcs = pato.getParents(c);
				for(OWLClass pc: pcs){
					return findTargetParent(pato, pato.getID(pc), result);
				}
			}
		}
		//if landed here, need to update Dictionary.patoupperclasses
		return null; 
	}

	/**
	 * merged to  searchOntologies(String term, String type, String ingroup)
	 * @param term
	 * @param type: entity or quality
	 * @return ArrayList of results, one result from an ontology 
	 */
/*	public ArrayList<String[]> searchOntologies(String term, String type) throws Exception {
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

	}*/


	/**
	 * 
	 * @param term
	 * @param type
	 * @return null or a hashtable (id=>label) containing classes that have term as an relational adjective.
	 */
	public Hashtable<String, String> searchAdjectiveOrgan(String term, String type) {
		if(type.compareTo("entity")==0){
			return OWLAccessorImpl.adjectiveorgans.get(term);
		}
		return null;
	}
	/**
	 * Search a term in a subgroup of an ontology
	 * subgroup only applies to PATO relational slim //TODO complete this part.
	 * @param term
	 * @param type: entity or quality
	 * @param subgroup: inRelationalSlim
	 * @return ArrayList of results, one result from an ontology 
	 */
	public ArrayList<Hashtable<String, String>> searchOntologies(String term, String type, ArrayList<Hashtable<String, String>> results) {
		//search quality or entity ontologies, depending on the type
		
		//quality
		if(type.compareTo("quality")==0){
			for(OWLAccessorImpl api: OWLqualityOntoAPIs){
				Hashtable<String, String> result = searchOWLOntology(term, api, type);
				if(result!=null){
					results.add(result);
				}
			}			
			/*TODO need review : result format should be the same as OWL search
			 * for(OBO2DB o2d: OBOqualityOntoAPIs){
				Hashtable<String, String> result = searchOBOOntology(term, o2d, type);
				if(result!=null){
					//added = true;
					results.add(result);
				}
			}*/			
		}
		
		
		//entity
		if(type.compareTo("entity")==0){
			for(OWLAccessorImpl api: OWLentityOntoAPIs){
				Hashtable<String, String> result = searchOWLOntology(term, api, type);
				if(result!=null){
					results.add(result);
				}
			}			
			/*TODO need review
			 * for(OBO2DB o2d: OBOentityOntoAPIs){
				Hashtable<String, String> result = searchOBOOntology(term, o2d, type);
				if(result!=null){
					//added = true;
					results.add(result);
				}
			}*/
		}
		return results;

	}
	
	/**
	 * 
	 * @param term
	 * @param o2d
	 * @param type
	 * @return  4-key hashtable: term, querytype, id, label, matchtype
	 * @throws Exception
	 */
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
	 * @param slim ?? 
	 * @return 5-key hashtable: term, querytype, id, label, matchtype
	 */
	private Hashtable<String, String> searchOWLOntology(String term, OWLAccessorImpl owlapi, String type) {
		Hashtable<String, String> result = null;
		//List<OWLClass> matches = (ArrayList<OWLClass>)owlapi.retrieveConcept(term);
		//should be
		
		Hashtable<String, ArrayList<OWLClass>> matches = (Hashtable<String, ArrayList<OWLClass>>)owlapi.retrieveConcept(term);
		if(matches == null || matches.size() ==0){
			//TODO: besides phrase based search, consider also make use of the relations and definitions used in ontology
			//TODO: update other copies of the method
			//task 2 matches can be null, if the term is looked up into other ontologies - modified by Hariharan
		}else{
			List<OWLClass> matchclass = matches.get("original");
			if(matchclass!=null && matchclass.size()!=0){
				result = collectResult(term, matchclass, type, "original", owlapi);
				return result;
			}
			
			matchclass = matches.get("exact");
			if(matchclass!=null && matchclass.size()!=0){
				result = collectResult(term, matchclass, type, "exact", owlapi);
				return result;
			}
			
			matchclass = matches.get("narrow");
			if(matchclass!=null && matchclass.size()!=0){
				result = collectResult(term, matchclass, type, "narrow", owlapi);
				return result;
			}
			
			matchclass = matches.get("related");
			if(matchclass!=null && matchclass.size()!=0){
				result = collectResult(term, matchclass, type, "related", owlapi);
				return result;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param term
	 * @param matches
	 * @param querytype
	 * @param matchtype
	 * @param owlapi
	 * @return 5-key hashtable: term, querytype, id, label, matchtype
	 */
	private Hashtable<String, String> collectResult(String term, List<OWLClass> matches, String querytype, String matchtype, OWLAccessorImpl owlapi){
		if(matches == null || matches.size() ==0) return null;
		Hashtable<String, String> result = new Hashtable<String, String>();
		result.put("term",  term);
		result.put("querytype",  querytype);
		result.put("matchtype", matchtype);
		result.put("id", "");
		result.put("label", "");
		boolean haveresult = false;
		Iterator<OWLClass> it = matches.iterator();
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			String id = owlapi.getID(c);
			result.put("id", result.get("id")+ id+";");
			result.put("label", result.get("label")+ label+";");
			haveresult = true;
		}
		if(haveresult){
			result.put("id", result.get("id").replaceFirst(";$", ""));
			result.put("label", result.get("label").replaceFirst(";$", ""));
		}
		if(haveresult) return result;
		return null;
	}
	/**
	 * merged to  searchOWLOntology(String term, OWLAccessorImpl owlapi, String type, String ingroup)
	 * @param term
	 * @param owlapi
	 * @param type
	 * @return 4-key hashtable: term, querytype, id, label, matchtype
	 */
	/*private String[] searchOWLOntology(String term, OWLAccessorImpl owlapi, String type) throws Exception {
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
	}*/

	
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

		s = Dictionary.singulars.get(word);
		if(s!=null) return s;
		
		if(word.matches("\\w+_[ivx-]+")){
			Dictionary.singulars.put(word, word);
			Dictionary.plurals.put(word, word);
			return word;
		}
		
		//adverbs
		if(word.matches("[a-z]{3,}ly")){
			Dictionary.singulars.put(word, word);
			Dictionary.plurals.put(word, word);
			return word;
		}
		
		String wordcopy = word;
		wordcopy = checkWN4Singular(wordcopy);
		if(wordcopy != null && wordcopy.length()==0){
			return word;
		}else if(wordcopy!=null){
			Dictionary.singulars.put(word, wordcopy);
			if(!wordcopy.equals(word)) Dictionary.plurals.put(wordcopy, word);
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
			Dictionary.singulars.put(word, s);
			if(!s.equals(word)) Dictionary.plurals.put(s, word);
			return s;
		  }
		}
		if(debug) System.out.println("["+word+"]'s singular is "+word);
		return word;
	}
	
	public static String plural(String b) {
		return Dictionary.plurals.get(b);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String[] results = retreiveParentInfoFromPATO("PATO:0000402");
		System.out.println(results[1]);
	}




}
