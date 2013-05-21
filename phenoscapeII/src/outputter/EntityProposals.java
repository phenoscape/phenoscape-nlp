/**
 * 
 */
package outputter;

import java.util.ArrayList;

/**
 * @author Hong Cui
 * This class holds 0 - n proposals for one entity phrase
 */
public class EntityProposals implements Proposals {
	ArrayList<Entity> proposals = new ArrayList<Entity>();
	float confidencethreshold = 0.8f;
	String phrase = "";
	/**
	 * 
	 */
	public EntityProposals() {		
	}
	
	public void add(Object e){
		proposals.add((Entity)e);
	}
	
	public ArrayList<Entity> getProposals(){
		return proposals;
	}

	public boolean hasOntologizedWithHighConfidence() {
		for(Entity entity: proposals){
			if(entity.isOntologized() && entity.getPrimaryEntityScore()>=this.confidencethreshold)
				return true;
		}
		return false;
	}
	
	public void setPhrase(String phrase){
		this.phrase = phrase;
	}
	
	public String getPhrase(){
		return phrase;
	}

	public void reset() {
		this.proposals = new ArrayList<Entity>();
		this.phrase = "";		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}






}
