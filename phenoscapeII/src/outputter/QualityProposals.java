package outputter;

import java.util.ArrayList;

/**
 * @author Hong Cui
 * This class holds 0 - n proposals for one quality phrase
 */
public class QualityProposals {
	ArrayList<Quality> proposals = new ArrayList<Quality>();
	/**
	 * 
	 */
	public QualityProposals() {		
	}
	
	public void add(Quality q){
		proposals.add(q);
	}
	
	public ArrayList<Quality> getProposals(){
		return proposals;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
