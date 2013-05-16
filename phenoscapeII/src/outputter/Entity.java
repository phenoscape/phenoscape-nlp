package outputter;

import org.semanticweb.owlapi.model.OWLClass;

public class Entity {
	
	public String getPrimaryEntityString(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getString(); //return the label
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().getString(); //return label of the primary entity
		return null; //return null for other cases
	}
	
	public String getPrimaryEntityLabel(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getLabel(); //return the label
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().getLabel(); //return label of the primary entity
		return null; //return null for other cases
	}
	
	public String getPrimaryEntityID(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getId(); //return the label
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().getId(); //return label of the primary entity
		return null; //return null for other cases
	}
	
	public String getPrimaryEntityOWLClassIRI(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).getClassIRI(); //return the label
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().getClassIRI(); //return label of the primary entity
		return null; //return null for other cases
	}
	
	public boolean isOntologized(){
		if(this instanceof SimpleEntity) return ((SimpleEntity)this).isOntologized(); //return the label
		if(this instanceof CompositeEntity) return ((CompositeEntity)this).getPrimaryEntity().isOntologized(); //return label of the primary entity
		return false; //return null for other cases
	}

}
