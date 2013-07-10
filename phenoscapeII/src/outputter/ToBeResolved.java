/**
 * 
 */
package outputter;

import java.util.ArrayList;

/**
 * @author updates
 *
 */
public class ToBeResolved{
	
	//private String structurename;
	private String structureid;
	private ArrayList<EntityProposals> entity;
	Structure2Quality s2q;
	private QualityProposals quality;

	public ToBeResolved(String structureid){
		//this.structurename = structurename;
		this.structureid = structureid;
		//this.entity = entity;
		//this.s2q = s2q;			
	}
	
	public void setEntityCandidate(ArrayList<EntityProposals> entity){
		this.entity = entity;
	}
	
	public void setStructure2Quality(Structure2Quality s2q){
		this.s2q = s2q;
	}
	
	public void setQualityCandidate(QualityProposals quality){
		this.quality = quality;
	}

	public ArrayList<EntityProposals> getEntityCandidate() {
		return this.entity;
	}
	
	public Structure2Quality getStructure2Quality(){
		return this.s2q;
	}
	
	public QualityProposals getQualityCandidate(){
		return this.quality;
	}
	
}
