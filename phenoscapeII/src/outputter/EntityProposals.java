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
	String phrase = "";
	/**
	 * 
	 */
	public EntityProposals() {		
	}
	
	public boolean add(Object e){
		if(e instanceof Entity){
			proposals.add((Entity)e);
			return true;
		}
		if(e instanceof EntityProposals){
			proposals.addAll(((EntityProposals)e).getProposals());
			return true;
		}
		return false;
	}
	
	public ArrayList<Entity> getProposals(){
		return proposals;
	}

	public float higestScore() {
		float max = 0f;
		for(Entity entity: proposals){
			if(entity.isOntologized()){
				float score = entity.getPrimaryEntityScore();
				if(score > max){
					max = score; 
				}
			}
		}
		return max;
	}
	
	public Entity getEntityWithHigestScore() {
		Entity theone = null;
		float max = 0f;
		for(Entity entity: proposals){
			if(entity.isOntologized()){
				float score = entity.getPrimaryEntityScore();
				if(score > max){
					theone = entity; 
				}
			}
		}
		return theone;
	}
	
	public ArrayList<Entity> getEntitiesAbove(float threshold){
		ArrayList<Entity> results = new ArrayList<Entity>();
		for(Entity entity: proposals){
			if(entity.isOntologized()){
				float score = entity.getPrimaryEntityScore();
				if(score > threshold){
					results.add(entity); 
				}
			}
		}
		return results;
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
	
	public String toString(){
		StringBuffer sb = new StringBuffer();

		for(Entity e:proposals)
		{
			sb.append(e.toString());
		}
		return sb.toString();
	}
	
	public EntityProposals clone() {
		
		EntityProposals epclone = new EntityProposals();
		epclone.setPhrase(this.phrase);
		for(Entity e:this.getProposals())
		{
			epclone.proposals.add(e);
		}
		return epclone;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public void setProposals(ArrayList<Entity> proposals) {
		this.proposals = proposals;		
	}
}
