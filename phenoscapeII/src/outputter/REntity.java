/**
 * 
 */
package outputter;

/**
 * @author updates
 *
 */
public class REntity extends Entity{
	FormalRelation relation;
	Entity entity;

	/**
	 * 
	 */
	public REntity(FormalRelation relation, Entity entity) {
		this.relation = relation;
		this.entity = entity;
		
	}

	public void setEntity(Entity entity){
		this.entity = entity;
	}
	
	public void setRelation(FormalRelation relation){
		this.relation = relation;
	}
	
	public Entity getEntity(){
		return this.entity;
	}
	
	public FormalRelation getRelation(){
		return this.relation;
	}
	
	/**
	 * (part_of some clavicle blade)
	 */
	public String toString(){
		return "("+this.relation.toString()+" some "+entity.toString()+")";
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
