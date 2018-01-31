/**
 * 
 */
package outputter.data;

import java.util.ArrayList;


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
	 * TODO: print () with entity is a composite entity
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
	public void setSearchString(String string) {
		
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
	public String getSearchString() {	
		if(entity instanceof CompositeEntity)
		return this.relation.getSearchString()+"#"+((CompositeEntity)entity).getSearchString();
		else
		return this.relation.getSearchString()+"#"+entity.getSearchString();
	}

	@Override
	public String getLabel() {
		String label="";

		if(entity instanceof SimpleEntity)
		{
			if(entity.getLabel()!=null)
			{
				label+=entity.getLabel();
			}else
			{
				label+=entity.getString();
			}
		}
		else if( entity instanceof CompositeEntity)
		{
			label+=((CompositeEntity) entity).getLabel();
		}
		
		if(entity instanceof CompositeEntity)
		return "("+this.relation.getLabel()+" some ("+label+")"+")";
		else
		return "("+this.relation.getLabel()+" some "+label+""+")";
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
	public float getConfidenceScore() {
		//multiple of all confidence scores
		return this.relation.getConfidenceScore()*this.entity.getConfidenceScore();
	}
	
	public REntity clone(){
		FormalRelation fr = this.relation.clone();
		Entity e = this.entity.clone();
		return new REntity(fr, e);
	}

	public int compare(Entity e1, Entity e2){
		return e1.content().compareTo(e2.content());
	}
	
	public boolean equals(Entity e){
		if(this.content().compareTo(e.content())==0) return true;
		return false;
	}

	@Override
	public ArrayList<Entity> getIndividualEntities() {
		ArrayList<Entity> individuals = new ArrayList<Entity>();
		individuals.addAll(entity.getIndividualEntities());
		return individuals;
	}

	@Override
	public void setString(String string) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getString() {
		// TODO Auto-generated method stub
		Entity e = this.entity;
		if(e instanceof CompositeEntity)
		{
		return ("("+this.relation.getLabel()+" some "+((CompositeEntity) e).getFullString()+")");
		} else if (e instanceof REntity)
		{
		return ("("+this.relation.getLabel()+" some "+((REntity) e).getString()+")");
		}
		return ("("+this.relation.getLabel()+" some "+e.getString()+")");
	}
	
    public String getunontologized(){
    	
    	Entity e = this.entity;
		if(e instanceof CompositeEntity)
		{
		return ((CompositeEntity) e).getunontologized();
		} else if (e instanceof REntity)
		{
		return ((REntity)e).getunontologized();
		} else if( e instanceof SimpleEntity)
		{
		return ((SimpleEntity)e).getunontologized();
		}
			
		return "";
    	
    }


}
