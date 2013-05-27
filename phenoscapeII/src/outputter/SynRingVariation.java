/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;

import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Cui
 * construct regex patterns of alternatives for an input phrase
 * 
 * Examples:
 *
 * input: postaxial process
 * output:
 * leadspatialtermvariaiton: (postaxial|syn_ring_from_dictionary|syn_from_onto)
 * headnounvaration:(process|crest|syn_ring_from_dictionary|syn_from_onto|relational_adj_from_onto)
 * 
 * input:
 */
public class SynRingVariation {
	private String leadspatialtermvariation="";
	private String headnounvariation="";

	/**
	 * 
	 * @param phrase
	 */
	public SynRingVariation(String phrase) {
		//TODO create variation
		extractleadspatialterm(phrase);
		extractheadnounvariation(phrase);
		this.leadspatialtermvariation=this.leadspatialtermvariation!=""?this.leadspatialtermvariation.substring(1):"";
		this.headnounvariation = this.headnounvariation!=""?this.headnounvariation.substring(1):"";
	}


	private void extractheadnounvariation(String phrase) {
		//TODO: add some sophisticated ways to identify the headnoun term as of now considering all but the first token as headnoun
		OWLAccessorImpl owlapi=null;
		ArrayList<String> ontosynonyms;
		String headnoun = phrase.substring(phrase.indexOf(" ")+1);
		if(headnoun.matches(Dictionary.process))
			headnoun = Dictionary.process;
		
		for(OWLAccessorImpl temp:XML2EQ.ontoutil.OWLentityOntoAPIs)
			if(temp.getSource().indexOf("ext")>1)
				owlapi=temp;
			
		for(String form:headnoun.split("\\|"))
		{
			this.headnounvariation+="|"+form;
			ontosynonyms = owlapi.getSynonymLabelsbyPhrase(form,"entity");
			for(String syn:ontosynonyms)
				this.headnounvariation+="|"+syn;
		}
		//fetch adjective organs
		ArrayList<String> adjectives = (ArrayList<String>) XML2EQ.ontoutil.OWLentityOntoAPIs.get(0).organadjective.get(phrase.split("\\s")[1]);
		if(adjectives!=null)
		for(String adjectiveform: adjectives)
				this.headnounvariation+="|"+adjectiveform;

	}

//From the input phrase it extracts the leadspatialterm , formats it, get all the synonyms of each format and forms a string like "postaxial|preaxial|axial|syn_from onto"
	private void extractleadspatialterm(String phrase) {
		//TODO: add some sophisticated ways to identify the lead spatial term as of now taking the first token as lead phrase
		String leadterm = phrase.split("\\s")[0];
		String forms = format(leadterm);
		OWLAccessorImpl owlapi=null;

		for(OWLAccessorImpl temp:XML2EQ.ontoutil.OWLentityOntoAPIs)
			if(temp.getSource().indexOf("bspo")>1)
				owlapi=temp;
		
		for(String form:forms.split("\\|"))
		{
			if(Dictionary.spatialtermptn.contains(form))
				{
				this.leadspatialtermvariation+="|"+form;
				ArrayList<String> ontosynonyms = owlapi.getSynonymLabelsbyPhrase(form,"spatial");
				for(String syn:ontosynonyms)
					this.leadspatialtermvariation+="|"+syn;
				}
		}
	}

	//This creates various forms of the spatial term eg: postaxial => postaxial|post-axial|pre-axial|preaxial|axial. This will get us atleast a match

	private String format(String leadterm) {
		
	String forms=leadterm+"|";
	String leadtermcopy = leadterm;
	//To remove existing prefixes from the term
	for(String temp:Dictionary.prefixes.split("\\|"))
		if(leadterm.matches(temp+".*"))
			{
			leadterm = leadterm.substring(temp.length());
			break;
			}
	//To add all kind of prefixes to the term
	for(String temp:Dictionary.prefixes.split("\\|"))
		if(forms.contains(leadtermcopy)==false)
		forms+=temp+leadterm+"|";
	forms += leadterm;
		return forms;
	}

	

	public String getLeadSpaticalTermVariation(){
		return this.leadspatialtermvariation;
	}
	
	public String getHeadNounVariation(){
		return this.headnounvariation;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
	//	getsynonym("anatomical section");
		SynRingVariation sv= new SynRingVariation("postaxial process");
		System.out.println("sv.leadspatialtermvariation = "+sv.leadspatialtermvariation);
		System.out.println("sv.headnounvariation ="+sv.headnounvariation);
	}

}
