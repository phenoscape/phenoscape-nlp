/**
 * 
 */
package outputter;

import java.util.ArrayList;

/**
 * @author Hong Cui
 * This class holds 0 - n proposals for one EQ phrase
 */
public class EQStatementProposals {
	ArrayList<EQStatement> proposals = new ArrayList<EQStatement>();
	/**
	 * 
	 */
	public EQStatementProposals() {		
	}
	
	public void add(EQStatement eq){
		proposals.add(eq);
	}
	
	public ArrayList<EQStatement> getProposals(){
		return proposals;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for(EQStatement eq: proposals){
			sb.append(eq.toString());
			sb.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}

	public String getType(){
		if(proposals.size()==0) return null;
		return proposals.get(0).getType();
	}
	
	public String getStateId(){
		if(proposals.size()==0) return null;
		return proposals.get(0).getStateId();
	}
	
	public String getDescription() {
		if(proposals.size()==0) return null;
		return proposals.get(0).getStateId();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}



}
