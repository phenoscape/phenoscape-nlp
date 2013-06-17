package outputter;

import org.semanticweb.owlapi.model.OWLClass;

public abstract class Entity implements FormalConcept{
	
	public String getPrimaryEntityString(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getString(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().getString(); 
		return null; //return null for other cases
	}
	
	public String getPrimaryEntityLabel(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getLabel(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().getLabel(); 
		if(this instanceof REntity) return ((REntity)this).getEntity().getPrimaryEntityLabel();
		return null; //return null for other cases
	}
	
	public String getPrimaryEntityID(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getId(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().getId(); 
		return null; //return null for other cases
	}
	
	public String getPrimaryEntityOWLClassIRI(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getClassIRI(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().getClassIRI(); 
		return null; //return null for other cases
	}
	
	public boolean isOntologized(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).isOntologized(); 
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().isOntologized(); 
		return false; //return false for other cases
	}

	public float getPrimaryEntityScore() {
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getConfidienceScore();
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().getConfidienceScore(); 
		return 0f;
	}
	
	public Entity clone(){
		return this.clone();
	}

	@Override
	public abstract void setString(String string);
	@Override
	public abstract void setLabel(String label);

	@Override
	public abstract void setId(String id);

	@Override
	public abstract void setClassIRI(String IRI);

	@Override
	public abstract void setConfidenceScore(float score);
	@Override
	public abstract String getString() ;

	@Override
	public abstract String getLabel();

	@Override
	public abstract String getId();

	@Override
	public abstract String getClassIRI();

	@Override
	public abstract float getConfidienceScore();

}
