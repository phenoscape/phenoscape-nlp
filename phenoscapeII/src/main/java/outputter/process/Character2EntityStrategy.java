/**
 * 
 */
package outputter.process;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import outputter.XML2EQ;
import outputter.data.CompositeEntity;
import outputter.data.Entity;
import outputter.data.EntityProposals;
import outputter.data.FormalConcept;
import outputter.data.FormalRelation;
import outputter.data.Quality;
import outputter.data.QualityProposals;
import outputter.data.REntity;
import outputter.data.SimpleEntity;
import outputter.knowledge.Dictionary;
import outputter.search.SpatialModifiedEntityStrategy;
import outputter.search.TermSearcher;
import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Cui
 * This class tests and see if the <character> is really a subclass of the subject entity, 
 * if so it will turn the <character> into an entity and set its quality to "present" 
 */
public class Character2EntityStrategy implements AnnotationStrategy{
	private static final Logger LOGGER = Logger.getLogger(Character2EntityStrategy.class);   
	EntityProposals entity; //result of the computation
	QualityProposals quality; //result of the computation, if set, it's "present"
	Entity subjectentity; //subject of the character, keyentity.
	String character; //the <character>
	/**
	 * 
	 */
	public Character2EntityStrategy(Entity subjectentity, String subclass) {
		this.subjectentity = subjectentity;
		this.character = subclass;
	}
	public EntityProposals getEntity(){
		return entity;
	}
	public QualityProposals getQuality(){
		return quality;
	}
	public void handle(){
		//try to match it in entity ontologies	  
		//text::Caudal fin
		//text::heterocercal  (heterocercal tail is a subclass of caudal fin)
		//text::diphycercal
		//=> heterocercal tail: present
		boolean subclassconfirmed = false;
		if(subjectentity.isOntologized()){
			//String quality = Utilities.formQualityValueFromCharacter(chara);
			for(OWLAccessorImpl owlimpl: XML2EQ.ontoutil.OWLentityOntoAPIs){
				//Hashtable<String, ArrayList<OWLClass>> result = owlimpl.retrieveConcept(subjectentity.getLabel());
				//if(result==null) continue;
				//List<OWLClass> classlist =result.get("original");
				//if(classlist==null || classlist.size()==0) continue;
				//OWLClass c = classlist.get(0);
				OWLClass c = owlimpl.getOWLClassByIRI(subjectentity.getClassIRI());
				Set<OWLClassExpression> subclasses = c.getSubClasses(owlimpl.getOntologies());
				for(OWLClassExpression subclass: subclasses){
					OWLClass asubclass = (OWLClass) subclass;
					if(owlimpl.getLabel(asubclass).startsWith(this.character)){
						subclassconfirmed = true;
						//use the subclass as the entity, quality=present
						//TODO: is there a need to consider character constraint which may contain an entity locator?
						SimpleEntity e = new SimpleEntity();
						e.setSearchString(this.character);
						e.setString(this.character);
						e.setId(owlimpl.getID(asubclass));//heterocercal tail
						e.setLabel(owlimpl.getLabel(asubclass));
						e.setClassIRI(asubclass.getIRI().toString());
						if(this.entity==null) this.entity = new EntityProposals(); 
						this.entity.add(e);
						
						Quality q = Dictionary.present;
						q.setSearchString("");
						q.setString("present");
						q.setConfidenceScore(1f);
						if(this.quality==null) this.quality = new QualityProposals(); 
						this.quality.add(q);
						break;
					}
				}
				if(subclassconfirmed) break;
			}		
		}
		
		/*if(!subclassconfirmed){
			//is the quality spatial? should not occur after xmlnormalization
			Matcher m = SpatialModifiedEntityStrategy.spatialptn.matcher(character);
			if(m.matches()){
				//form spatial entity
				if(character.indexOf(" ")<0) character += " region";
				ArrayList<FormalConcept> spatialentities = new TermSearcher().searchTerm(character, "entity");//anterior + region: simple search
				if(spatialentities!=null) LOGGER.debug("...found match");
				else{
					//create phrase-based spatialentities
					SimpleEntity spatial = new SimpleEntity();
					spatial.setSearchString(character);
					spatial.setString(character);
					spatial.setConfidenceScore(1f);
					spatialentities = new ArrayList<FormalConcept>();
					spatialentities.add(spatial);
				}
				//postcompose with subject entity
				for(FormalConcept spatial: spatialentities){
					FormalRelation rel = Dictionary.partof;
					rel.setConfidenceScore((float)1.0);
					REntity rentity = new REntity(rel, this.subjectentity);
					//composite entity = entity locator for sentity
					CompositeEntity centity = new CompositeEntity(); 
					centity.addEntity((SimpleEntity)spatial); 
					centity.addEntity(rentity);
					if(this.entity==null) this.entity = new EntityProposals(); 
					this.entity.add(centity);
				}				
			}
		}*/
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
