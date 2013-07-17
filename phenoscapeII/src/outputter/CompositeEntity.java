/**
 * 
 */
package outputter;

import java.util.ArrayList;

/**
 * @author updates
 *
 *post-composed entity = entity [and relation SOME entity]+
 *
 *e.g.:
 *1. proximal region AND (part_of SOME (clavicle blade AND in_right_side_of SOME multi-cellular organism))) 
 *2. lamina AND (part_of SOME (anterior region and part_of SOME scapula)))
 */
public class CompositeEntity extends Entity {
	//SimpleEntity entity; //the first entity in the post-composed entity
    ArrayList<Entity> entities; //relation + entity
    String string;

	/**
	 * 
	 */
	public CompositeEntity() {
		// TODO Auto-generated constructor stub
		entities = new ArrayList<Entity>();
	}

	public SimpleEntity getPrimaryEntity(){
		return (SimpleEntity) entities.get(0);
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
	
	
	public Entity getEntity(int index){
		return entities.get(index);
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for(Entity e: entities){
			sb.append(e.toString()+" and ");
		}
		return sb.toString().replaceFirst("\\s+and $", "");
	}
	public boolean isOntologized() {
		return this.getPrimaryEntity().id != null;
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
		// TODO Auto-generated method stub
		return null;
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
	public float getConfidienceScore() {
		// TODO Auto-generated method stub
		return 0f;
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
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
