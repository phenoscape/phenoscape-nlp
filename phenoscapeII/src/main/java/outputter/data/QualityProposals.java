package outputter.data;

import java.util.ArrayList;
import java.util.Comparator;

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
	@Override
	public boolean equals(Object qp){
		if(qp instanceof QualityProposals){
		return this.content().compareTo(((QualityProposals)qp).content())==0;
		}
		return false;
	}
	/**
	 * TODO: change Object to something more precise.
	 */
	public boolean add(Object q){
		if(q instanceof Quality){
			proposals.add((Quality)q);
			return true;
		}
		if(q instanceof QualityProposals){
			proposals.addAll(((QualityProposals) q).getProposals());
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
		int count=1;
		//sb.append(System.getProperty("line.separator"));
		for(Quality q:proposals)
		{
			sb.append("Q"+count+":"+q.toString()+System.getProperty("line.separator"));
			count++;
		}
		return sb.toString();
	}
	
	public String content(){
		StringBuffer sb = new StringBuffer();

		for(Quality q:proposals)
		{
			sb.append(q.content());
		}
		return sb.toString();
	}
	
	public float higestScore() {
		float max = 0f;
		for(Quality quality: proposals){
			if(quality.isOntologized()){
				float score = quality.getConfidenceScore();
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
				float score = quality.getConfidenceScore();
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
				float score = quality.getConfidenceScore();
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
