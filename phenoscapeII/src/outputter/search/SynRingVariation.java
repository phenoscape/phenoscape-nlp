/**
 * 
 */
package outputter.search;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;

import outputter.ApplicationUtilities;
import outputter.knowledge.Dictionary;
import outputter.knowledge.TermOutputerUtilities;
import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Cui
 * This is a utility class with static methods that 
 * construct regex patterns of alternatives for an input word (spatial or structure)
 * 
 * Examples:
 *
 * input: postaxial
 * output:
 * synring: postaxial|syn_ring_from_dictionary|syn_from_onto
 * 
 * 
 * input: nose
 * output:
 * synring: nose|nasal|syn_ring_from_dictionary|syn_from_onto|relational_adj_from_onto
 */
public class SynRingVariation {
	//private String leadspatialtermvariation="";
	//private String headnounvariation="";
	private static final Logger LOGGER = Logger.getLogger(SynRingVariation.class);   
	private static Hashtable<String, String> cache = new Hashtable<String, String>();
	/**
	 * 
	 * @param phrase
	 */
	/*public SynRingVariation(String phrase) {
		//TODO create variation
		getSynRing4Spatial(phrase);
		extractheadnounvariation(phrase);
		this.leadspatialtermvariation=this.leadspatialtermvariation!=""?this.leadspatialtermvariation.substring(1):"";
		this.headnounvariation = this.headnounvariation!=""?this.headnounvariation.substring(1):"";
		
	}*/
	
	
	/**
	 * 
	 * @param structure: one word representing a structure
	 * @return
	 */
	//TODO check duplicates: (?:(?:opening|foramina|foramen|foramens|perforation|orifice|opening|foramina|bone foramen|foramen|foramens|bone foramen|perforation|orifice|orifice))
	public static String getSynRing4Structure(String structure) {
		if(structure.length()==0) return "";
		String synring = cache.get(structure);
		if(synring!=null) return synring;
		
		synring = structure;
		OWLAccessorImpl owlapi=null;
		ArrayList<String> ontosynonyms;
		//grab a synring from Dictionary
		if(structure.matches("\\b("+Dictionary.process+")\\b"))
			synring = Dictionary.process;
		if(structure.matches("\\b("+Dictionary.opening+")\\b"))
			synring = Dictionary.opening;
		
		//find owlapi for UBERON
		for(OWLAccessorImpl temp:TermOutputerUtilities.OWLentityOntoAPIs){
			if(temp.getSource().indexOf(ApplicationUtilities.getProperty("ontology.uberon"))>1){
				owlapi=temp;
				break;
			}
		}
		
		//expanding the synring with synonyms
		for(String form:synring.split("\\|"))
		{
			if(!form.matches("\\b("+synring+")\\b")) synring+="|"+form; //don't add duplicates
			ontosynonyms = owlapi.getSynonymLabelsbyPhrase(form,"entity");
			for(String syn:ontosynonyms)
				if(!form.matches("\\b("+synring+")\\b")) synring+="|"+syn;
		}
		
		//fetch adjective organs
		ArrayList<String> adjectives = new ArrayList<String> ();
		if(OWLAccessorImpl.organadjectives.get(structure)!=null) adjectives.addAll(OWLAccessorImpl.organadjectives.get(structure));
		if(OWLAccessorImpl.adjectiveorgans.get(structure)!=null) adjectives.addAll(OWLAccessorImpl.adjectiveorgans.get(structure).values());
		if(Dictionary.organadjectives.get(structure)!=null) adjectives.addAll(Dictionary.organadjectives.get(structure)); //those not covered in ontologies
		if(Dictionary.adjectiveorgans.get(structure)!=null) adjectives.addAll(Dictionary.adjectiveorgans.get(structure));
		
		for(String adjectiveform: adjectives){
			if(!adjectiveform.matches("\\b("+synring+")\\b")) synring+="|"+adjectiveform;
		}
		
		cache.put(structure, synring);
		return synring;
	}

	/**
	 * Gets all the synonyms of the spatial term and forms a string like "anterior|syn1|syn2"
	 * @param spatial: one word spatial term such as 'anterior'
	 * @return: a string of synonym ring like "anterior|syn1|syn2"
	 */
	public static String getSynRing4Spatial(String spatial) {
		if(spatial.length()==0) return "";
		String synring = cache.get(spatial);
		if(synring!=null) return synring;
		//String forms = prefixSpatial(spatial);
		OWLAccessorImpl owlapi=null;

		for(OWLAccessorImpl temp:TermOutputerUtilities.OWLentityOntoAPIs){
			if(temp.getSource().indexOf(ApplicationUtilities.getProperty("ontology.bspo"))>=1){
				owlapi=temp;
				break;
			}
			
		}
		
	
		synring = spatial;
		ArrayList<String> ontosynonyms = owlapi.getSynonymLabelsbyPhrase(spatial,"spatial");
		for(String syn:ontosynonyms){
			if(!synring.matches("\\b("+syn+")\\b"))
				synring +="|"+syn;
		}
		
		cache.put(spatial, synring);
		return synring;
		
	}

	/*
	 * This creates various forms of the spatial term eg: postaxial => postaxial|post-axial|pre-axial|preaxial|axial. 
	 * This will get us at least a match
	 * this is currently not used.
	 */

	private String prefixSpatial(String spatial) {
		
	String forms=spatial+"|";
	String leadtermcopy = spatial;
	//To remove existing prefixes from the term
	for(String temp:Dictionary.prefixes.split("\\|"))
		if(spatial.matches(temp+".*"))
			{
			spatial = spatial.substring(temp.length());
			break;
			}
	//To add all kind of prefixes to the term
	for(String temp:Dictionary.prefixes.split("\\|"))
		if(forms.contains(leadtermcopy)==false)
		forms+=temp+spatial+"|";
		return forms;
	}

	

	/*public String getLeadSpaticalTermVariation(){
		return this.leadspatialtermvariation;
	}
	
	public String getHeadNounVariation(){
		return this.headnounvariation;
	}*/
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	//	getsynonym("anatomical section");
	/*	SynRingVariation sv= new SynRingVariation("postaxial process");
		System.out.println("sv.leadspatialtermvariation = "+sv.leadspatialtermvariation);
		System.out.println("sv.headnounvariation ="+sv.headnounvariation);
	*/
	System.out.println(SynRingVariation.getSynRing4Spatial("basal"));
	System.out.println(SynRingVariation.getSynRing4Structure("radial"));
		
	}

}
