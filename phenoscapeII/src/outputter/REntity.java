/**
 * 
 */
package outputter;

/**
 * @author Hong Cui
 * 
 * RelatedEntity with relation. For example, part_of fin.
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

	@Override
	public void setString(String string) {
		
	}

	@Override
	public void setLabel(String label) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setId(String id) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setClassIRI(String IRI) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setConfidenceScore(float score) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getString() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClassIRI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getConfidienceScore() {
		// TODO Auto-generated method stub
		return 0;
	}

	//@Override
	public void setXMLid(String xmlid) {
		// TODO Auto-generated method stub
		
	}

	//@Override
	public String getXMLid() {
		// TODO Auto-generated method stub
		return null;
	}

}
