/**
 * 
 */
package outputter.data;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * @author Hong Cui
 * This class holds 0 - n proposals for one entity phrase
 */
public class EntityProposals implements Proposals, Comparator {
	ArrayList<Entity> proposals = new ArrayList<Entity>();
	String phrase = "";
	/**
	 * 
	 */
	public EntityProposals() {		
	}
	

	public boolean add(Object e){
		if(e instanceof Entity){
			/*if(!proposals.contains((Entity)e))*/  
			proposals.add((Entity)e);
			return true;
		}
		if(e instanceof EntityProposals){
			proposals.addAll(((EntityProposals)e).getProposals());
			/*for(Entity e1: ((EntityProposals)e).getProposals()){
				if(!proposals.contains(e1)) proposals.add(e1);
			}*/
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
		int count = 1;
		//sb.append(System.getProperty("line.separator"));
		for(Entity e:proposals)
		{
			sb.append("P"+count+":"+e.toString()+System.getProperty("line.separator"));
			count++;
		}
		return sb.toString().replaceFirst(System.getProperty("line.separator")+"$", "");
	}
	
	public String content(){
		StringBuffer sb = new StringBuffer();
		for(Entity e:proposals)
		{
			sb.append(e.content());
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

	@Override
	public int compare(Object arg0, Object arg1) {
		if(arg0 instanceof EntityProposals && arg1 instanceof EntityProposals){
			return ((EntityProposals)arg0).content().compareTo(((EntityProposals)arg1).content());
		}
		return -1;
	}
	
	@Override
	public boolean equals(Object ep){
		if(ep instanceof EntityProposals)
		return this.content().compareTo(((EntityProposals)ep).content())==0;
		else return false;
	}
}
