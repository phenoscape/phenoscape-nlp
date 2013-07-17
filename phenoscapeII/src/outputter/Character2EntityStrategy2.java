/**
 * 
 */
package outputter;

import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;

import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Cui
 * This class tests and see if the <character> is really a subclass of the subject entity, 
 * if so it will turn the <character> to an entity and set its quality to "present" 
 */
public class Character2EntityStrategy2 {
	private static final Logger LOGGER = Logger.getLogger(Character2EntityStrategy2.class);   
	EntityProposals entity; //result of the computation
	QualityProposals quality; //result of the computation, if set, it's "present"
	Entity subjectentity; //subject of the character, keyentity.
	String subclass;
	/**
	 * 
	 */
	public Character2EntityStrategy2(Entity subjectentity, String subclass) {
		this.subjectentity = subjectentity;
		this.subclass = subclass;
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
					if(owlimpl.getLabel(asubclass).startsWith(this.subclass)){
						subclassconfirmed = true;
						//use the subclass as the entity, quality=present
						//TODO: is there a need to consider character constraint which may contain an entity locator?
						SimpleEntity e = new SimpleEntity();
						e.setString(this.subclass);
						e.setId(owlimpl.getID(asubclass));//heterocercal tail
						e.setLabel(owlimpl.getLabel(asubclass));
						e.setClassIRI(asubclass.getIRI().toString());
						this.entity.add(e);
						
						Quality q = new Quality();
						q.setString("present");
						q.setId("PATO:0000467");
						q.setLabel("present");
						this.quality.add(q);
						break;
					}
				}
				if(subclassconfirmed) break;
			}		
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
