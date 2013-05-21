/**
 * 
 */
package outputter;

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
	private String leadspatialtermvariation;
	private String headnounvariation;

	/**
	 * 
	 * @param phrase
	 */
	public SynRingVariation(String phrase) {
		//TODO create variation
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

	}

}
