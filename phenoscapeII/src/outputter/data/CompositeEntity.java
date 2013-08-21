/**
 * 
 */
package outputter.data;

import java.util.ArrayList;

import outputter.knowledge.Dictionary;

/**
 * @author Hong Cui
 *
 *post-composed entity = entity [and relation SOME entity]+
 *
 *e.g.:
 *1. proximal region AND (part_of SOME (clavicle blade AND in_right_side_of SOME multi-cellular organism))) 
 *2. lamina AND (part_of SOME (anterior region and part_of SOME scapula)))
 *
 *A AND (part_of (B AND part_of C)) is a typical case of nested post-composed entity
 *A AND (part_of B) AND (part_of C) is not a typical (and could be wrong) case of nested post-composed entity, but one could have
 *A AND (part_of B) AND (bearer_of 'increased size')
 */
public class CompositeEntity extends Entity {
	//SimpleEntity entity; //the first entity in the post-composed entity
    ArrayList<Entity> entities; //relation + entity
    String string;

	/**
	 * 
	 */
	public CompositeEntity() {
		entities = new ArrayList<Entity>();
	}

	public SimpleEntity getTheSimpleEntity(){
		return (SimpleEntity) entities.get(0);
	}
	
	/**
	 * 
	 * @return the first REntity with relation 'part_of'
	 */
	public REntity getEntityLocator(){
		for(Entity entity: entities){
			if(entity instanceof REntity && ((REntity)entity).getRelation().getClassIRI().compareTo(Dictionary.partofiri)==0){
				return (REntity) entity;
			}					
		}
		return null;
	}
	/**
	 * add new components to this CompositEentity: add another "and"
	 * increase the size of the arraylist
	 * @param entity: simple, rentity, or composite entity
	 */
	public void addEntity(Entity entity){ 
		if(entities.size()==0 && entity instanceof CompositeEntity){
			ArrayList<Entity> additions = ((CompositeEntity)entity).getEntities();
			entities.addAll(additions);			
		}else{
			entities.add(entity);
		}
	}
	
	/**
	 * add a parent entity 
	 * not increase the size of the arraylist for existing CompositeEntity
	 * increase the size of the arraylist for existing simpleEntity (in the process of constructing the compositEntity)
	 * @param entity
	 */
	public void addParentEntity(REntity entity){
	/*public void addParentEntity(Entity entity){
		if(entities.size()==0 && entity instanceof CompositeEntity){
			ArrayList<Entity> additions = ((CompositeEntity)entity).getEntities();
			entities.addAll(additions);			
		}else{*/
			Entity last = entities.get(entities.size()-1);
			if(last instanceof CompositeEntity){//is this possible?
				((CompositeEntity) last).addEntity(entity);
			}else if(last instanceof REntity){
				Entity e = ((REntity) last).getEntity();
				CompositeEntity ce = new CompositeEntity();
				ce.addEntity(e);
				ce.addEntity(entity);
				((REntity) last).setEntity(ce);
			}else{
				this.addEntity(entity);
			}
		//}
	}

	
	
	
	/**
	 * removing elements from the middle may not make sense.
	 * @return
	 */
	public Entity removeLastEntity(){
		return entities.remove(entities.size()-1); 
	}
	
	public ArrayList<Entity> getEntities() {
		return entities;
	}
	

	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for(Entity e: entities){
			sb.append(e.toString()+" and ");
		}
		return sb.toString().replaceFirst("\\s+and $", "");
	}
	
	public String content(){
		StringBuffer sb = new StringBuffer();
		for(Entity e: entities){
			sb.append(e.content()+" and ");
		}
		return sb.toString().replaceFirst("\\s+and $", "");
	}
	
	public boolean isOntologized() {
		return this.getTheSimpleEntity().id != null;
	}


	@Override
	public void setString(String string) {
		this.string = string;
		
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
	/**
	 * return the concatenation of the string of all entities
	 */
	public String getString() {
		/*String str = "";
		for(Entity e: this.entities){
			if(e instanceof SimpleEntity){
				str += e.getString()+" ";
			}else if(e instanceof REntity){
				str += ((REntity)e).getEntity().getString()+" ";
			}
		}
		return str;*/
		return this.string;
	}

	@Override	
	public String getLabel() {
		String label="";
		for(Entity e:this.getEntities())
		{
			if(e instanceof SimpleEntity)
			{
				label+=e.getLabel()+" and ";
			}
			else if( e instanceof CompositeEntity)
			{
				label+=((CompositeEntity) e).getLabel();
			}
			else
			{
				label+=((REntity) e).getLabel()+" and ";
			}
				
		}
		
		label = label.replaceAll("(and )$", "");
		return label.trim();
	}

	public String getFullID()
	{

		String id="";
		for(Entity e:this.getEntities())
		{
			if(e instanceof SimpleEntity)
			{
				id+=e.getId()+" and ";
			}
			else if( e instanceof CompositeEntity)
			{
				id+=((CompositeEntity) e).getFullID();
			}
			else
			{
				id+=((REntity) e).getId()+" and ";
			}
				
		}
		
		id = id.replaceAll("(and )$", "");
		return id.trim();
	
	}
	
	
	public String getFullString()
	{

		String string="";
		for(Entity e:this.getEntities())
		{
			if(e instanceof SimpleEntity)
			{
				string+=e.getString()+" and ";
			}
			else if(e instanceof CompositeEntity)
			{
				string+=((CompositeEntity) e).getFullString();
			}
			else
			{
				string+=((REntity) e).getString()+" and ";
			}
				
		}
		
		string = string.replaceAll("(and )$", "");
		return string.trim();	
	}
	
	
	@Override
	public String getId() {
		return this.getPrimaryEntityID();
	}

	@Override
	public String getClassIRI() {
		return this.getPrimaryEntityOWLClassIRI();
	}

	@Override
	public float getConfidenceScore() {
		
		float score=1.0f;
		for(Entity e:this.getEntities())
		{			
				score*=e.getConfidenceScore();	
		}
		
		return score;
	}
	//cloning - recursive implementation
	public CompositeEntity clone()
	{
		CompositeEntity clone=new CompositeEntity();
			for(Entity e:this.getEntities())
			{
				if(e instanceof SimpleEntity)
				{
					clone.addEntity(((SimpleEntity) e).clone());
				}
				else if(e instanceof CompositeEntity)
				{
					clone.addEntity(((CompositeEntity) e).clone());
				}
				else//e is related entity
				{
					FormalRelation related = ((REntity) e).getRelation();
					FormalRelation relation = new FormalRelation(related.getString(),related.getLabel(),related.getId(),related.getClassIRI());
					relation.setConfidenceScore(related.getConfidenceScore());
					if(((REntity)e).getEntity() instanceof SimpleEntity)
					{
						REntity re = new REntity(relation,((SimpleEntity)(((REntity)e).getEntity())).clone());
						clone.addEntity(re);
					}
					else
					{
						REntity re = new REntity(relation,((CompositeEntity)(((REntity)e).getEntity())).clone());
						clone.addEntity(re);
					}
				}	
			}
		return clone;
	}
	
	public int compare(Entity e1, Entity e2){
		return e1.content().compareTo(e2.content());
	}
	
	public boolean equals(Entity e){
		if(this.content().compareTo(e.content())==0) return true;
		return false;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<Entity> getIndividualEntities() {
		ArrayList<Entity> individuals = new ArrayList<Entity>();
		for(Entity e: this.entities){
			individuals.addAll(e.getIndividualEntities());
		}
		return individuals;
	}
}
