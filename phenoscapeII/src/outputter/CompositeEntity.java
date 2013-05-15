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
	SimpleEntity entity; //the first entity in the post-composed entity
    ArrayList<Entity> entities; //relation + entity
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
	 * 
	 * @param entity: simple, rentity, or composite entity
	 */
	public void addEntity(Entity entity){ 
		entities.add(entity);
	}
	
	/**
	 * removing elements from the middle may not make sense.
	 * @return
	 */
	public Entity removeLastEntity(){
		return entities.remove(entities.size()-1); 
	}
	
	public Entity getEntity(int index){
		return entities.get(index);
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer();
		for(Entity e: entities){
			sb.append(e.toString()+" and ");
		}
		return sb.toString().trim();
	}
	public boolean isOntologized() {
		return this.getPrimaryEntity().id != null;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
