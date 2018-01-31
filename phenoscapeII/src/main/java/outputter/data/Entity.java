package outputter.data;

import java.util.ArrayList;
import java.util.Comparator;

import org.semanticweb.owlapi.model.OWLClass;

public abstract class Entity implements FormalConcept, Comparator<Entity>{
	
	public String getPrimaryEntityString(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getString(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getTheSimpleEntity().getString(); 
		return null; //return null for other cases
	}
	
	
	
	public String getPrimaryEntityLabel(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getLabel(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getTheSimpleEntity().getLabel(); 
		if(this instanceof REntity) return ((REntity)this).getEntity().getPrimaryEntityLabel();
		return null; //return null for other cases
	}
	
	public String getPrimaryEntityID(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getId(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getTheSimpleEntity().getId(); 
		return null; //return null for other cases
	}
	
	public String getPrimaryEntityOWLClassIRI(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getClassIRI(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getTheSimpleEntity().getClassIRI(); 
		return null; //return null for other cases
	}
	
	public boolean isOntologized(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).isOntologized(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getTheSimpleEntity().isOntologized(); 
		return false; //return false for other cases
	}

	public float getPrimaryEntityScore() {
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getConfidenceScore();
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getTheSimpleEntity().getConfidenceScore(); 
		return 0f;
	}
	
	
	public Entity clone(){
		return this.clone();
	}

	public int compare(Entity e1, Entity e2){
		return e1.content().compareTo(e2.content());
	}
	
	@Override
	public boolean equals(Object e){
		if(e instanceof Entity){
			if(this.content().compareTo(((Entity)e).content())==0) return true;
		}
		return false;
	}
	
	@Override
	public abstract void setSearchString(String string);
	@Override
	public abstract void setLabel(String label);

	@Override
	public abstract void setId(String id);

	@Override
	public abstract void setClassIRI(String IRI);

	@Override
	public abstract void setConfidenceScore(float score);
	@Override
	public abstract String getSearchString() ;

	@Override
	public abstract String content() ;
	@Override
	public abstract String getLabel();

	@Override
	public abstract String getId();

	@Override
	public abstract String getClassIRI();

	@Override
	public abstract float getConfidenceScore();

	public abstract ArrayList<Entity> getIndividualEntities(); 
	
	

}
