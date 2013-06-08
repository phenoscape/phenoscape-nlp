package outputter;

import java.util.ArrayList;

/**
 * @author Hong Cui
 * This class holds 0 - n proposals for one quality phrase
 */
public class QualityProposals implements Proposals {
	ArrayList<Quality> proposals = new ArrayList<Quality>();
	String phrase="";
	/**
	 * 
	 */
	public QualityProposals() {		
	}
	
	public boolean add(Object q){
		if(q instanceof Quality){
			proposals.add((Quality)q);
			return true;
		}
		return false;
	}

	public String getPhrase() {
		return this.phrase;
	}

	public void setPhrase(String phrase) {
		this.phrase = phrase;		
	}
	
	public ArrayList<Quality> getProposals(){
		return proposals;
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();

		for(Quality q:proposals)
		{
			sb.append(q.toString());
		}
		return sb.toString();
	}
	
	public float higestScore() {
		float max = 0f;
		for(Quality quality: proposals){
			if(quality.isOntologized()){
				float score = quality.getConfidienceScore();
				if(score > max){
					max = score; 
				}
			}
		}
		return max;
	}
	
	public Quality getEntityWithHigestScore() {
		Quality theone = null;
		float max = 0f;
		for(Quality quality: proposals){
			if(quality.isOntologized()){
				float score = quality.getConfidienceScore();
				if(score > max){
					theone = quality; 
				}
			}
		}
		return theone;
	}
	
	public ArrayList<Quality> getQualitiesAbove(float threshold){
		ArrayList<Quality> results = new ArrayList<Quality>();
		for(Quality quality: proposals){
			if(quality.isOntologized()){
				float score = quality.getConfidienceScore();
				if(score > threshold){
					results.add(quality); 
				}
			}
		}
		return results;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}


}
