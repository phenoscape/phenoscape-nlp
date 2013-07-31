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
	
	/**
	 * relation is one of those from the restricted list
	 * @param relation
	 */
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
	
	public String content(){
		return "("+this.relation.content()+" some "+entity.content()+")";
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
		
		if(entity instanceof CompositeEntity)
		return "("+this.relation.getString()+" some ("+((CompositeEntity)entity).getFullString()+")"+")";
		else
		return "("+this.relation.getString()+" some "+entity.getString()+""+")";
		
	}

	@Override
	public String getLabel() {

		if(entity instanceof CompositeEntity)
		return "("+this.relation.getLabel()+" some ("+entity.getLabel()+")"+")";
		else
		return "("+this.relation.getLabel()+" some "+entity.getLabel()+""+")";
	}

	@Override
	public String getId() {
		
			if(entity instanceof CompositeEntity)
			return "("+this.relation.getId()+" some ("+((CompositeEntity)entity).getFullID()+")"+")";
			else
			return "("+this.relation.getId()+" some "+entity.getId()+""+")";

	}

	@Override
	public String getClassIRI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getConfidienceScore() {
		//multiple of all confidence scores
		return this.relation.getConfidienceScore()*this.entity.getConfidienceScore();
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
	
	public REntity clone(){
		FormalRelation fr = this.relation.clone();
		Entity e = this.entity.clone();
		return new REntity(fr, e);
	}

}
