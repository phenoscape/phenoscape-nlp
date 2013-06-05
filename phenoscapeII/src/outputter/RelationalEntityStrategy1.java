package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
/*
 * 
 * This class identifies related entities. If a quality is identified to be a relational quality,
 * then other structures which contain the same quality can be related entities
 * else
 * If the structure that contains the relational Q is "whole organism" then all key entities except the first one can be related entities
 * else
 * the structure can be a bilateral structure then all the key entities will be bilateral
 * 
 * 
 */

public class RelationalEntityStrategy1 {

	private Element root;
	private Element structure;
	static XPath pathCharacterUnderStucture;
	ArrayList<EntityProposals> relatedentities;
	ArrayList<EntityProposals> primaryentities;
	ArrayList<EntityProposals> keyentities;
	private Hashtable<String,ArrayList<EntityProposals>> entities = new Hashtable<String,ArrayList<EntityProposals>>();

public Hashtable<String, ArrayList<EntityProposals>> getEntities() {
		return entities;
	}

static {
	try {
		pathCharacterUnderStucture = XPath.newInstance(".//character");
	} catch (JDOMException e) {
		e.printStackTrace();
	}
}
	public RelationalEntityStrategy1(Element root, Element Structure,ArrayList<EntityProposals> keyentities) {
		// TODO Auto-generated constructor stub
		this.root = root;
		this.structure = Structure;
		this.relatedentities= new ArrayList<EntityProposals>();
		this.keyentities=keyentities;
		this.primaryentities = new ArrayList<EntityProposals>();
	}
/*
 * Makes a call to Structureswithsamecharacters() to identify the entities of the relational quality
 */
	public void handle() {
		bilateralstructures(this.structure.getAttributeValue("name"));
	}
	
	

	
/* It checks, if the structure belongs to lateralsides classes and 
*	if true, it creates "in a right side of" classes on key entities
*/
	private void bilateralstructures(String structname) {

		//If it is a simple entity add part of multicellular organism
		//If it is a composite entity and part of relation, then replace the part of relation with "in right side of" and "in left side of"
		//else
		//primary entity of this composite entity "in right side of" or "in left side of" 
		if(XML2EQ.elk.lateralsidescache.get(structname)!=null)
		{
			if((this.keyentities!=null)&&(this.keyentities.size()>0))
			{
				for(EntityProposals ep:this.keyentities)
			{
				EntityProposals RelatedEP = new EntityProposals();
				EntityProposals PrimaryEP = new EntityProposals();
				for(Entity e: ep.getProposals())
				{
					if(e instanceof SimpleEntity)// define
					{
						//Related Entity
						REntity related1 = this.multicellularrelatedentity("in right side of");
						CompositeEntity centity = new CompositeEntity();
						centity.addEntity((SimpleEntity)e);
						centity.addEntity(related1);
						RelatedEP.add(centity);
						
						//Primary Entities
						related1 = this.multicellularrelatedentity("in left side of");
						CompositeEntity centity1 = new CompositeEntity();
						centity1.addEntity((SimpleEntity)e);
						centity1.addEntity(related1);
						PrimaryEP.add(centity1);
					}
					if(e instanceof CompositeEntity)
					{
						//if there is a part of relation already existing, then the related entity is the entity locator
						REntity relatedE = (REntity) ((CompositeEntity) e).getEntity(1);
						if(relatedE.getString().equals("part of"))
						{
							FormalRelation rel = new FormalRelation();
							rel.setString("in right side of");
							rel.setLabel(Dictionary.resrelationQ.get("BSPO:0000121"));
							rel.setId("BSPO:0000121");
							rel.setConfidenceScore((float)1.0);
							
							SimpleEntity entitylocator = (SimpleEntity) relatedE.getEntity();
							REntity relatedentity1 = new REntity(rel,entitylocator);
							
							CompositeEntity centity = new CompositeEntity();
							centity.addEntity(((CompositeEntity) e).getEntity(0));
							centity.addEntity(relatedentity1);
							RelatedEP.add(centity);

							FormalRelation rel1 = new FormalRelation();
							rel1.setString("in left side of");
							rel1.setLabel(Dictionary.resrelationQ.get("BSPO:0000120"));
							rel1.setId("BSPO:0000120");
							rel1.setConfidenceScore((float)1.0);
							
							SimpleEntity entitylocator1 = (SimpleEntity) relatedE.getEntity();
							REntity relatedentity2 = new REntity(rel1,entitylocator1);
							
							CompositeEntity centity1 = new CompositeEntity();
							centity.addEntity(((CompositeEntity) e).getEntity(0));
							centity.addEntity(relatedentity2);
							PrimaryEP.add(centity1);
							
						}
						else
						{
							//Related Entity
							//to existing composite entity add a multicellular related entity
							REntity entity1 = this.multicellularrelatedentity("in right side of");
							CompositeEntity centity = ((CompositeEntity) e);
							centity.addEntity(entity1);
							RelatedEP.add(centity);
							
							//Primary Entities
							REntity entity2 = this.multicellularrelatedentity("in left side of");
							CompositeEntity centity1 = ((CompositeEntity) e);
							centity1.addEntity(entity2);
							PrimaryEP.add(centity1);
						}
		
						
					}
				}	
				if(RelatedEP.getProposals().size()>0)
				{
					this.relatedentities.add(RelatedEP);
					this.primaryentities.add(PrimaryEP);
				}
			}
				this.entities.put("Related Entities",this.relatedentities);
				this.entities.put("Primary Entity", this.primaryentities);
			}
		}
	}
	
	public REntity multicellularrelatedentity(String relation)
	{
		String id;
		
	if(relation.equals("in right side of"))
		id="BSPO:0000121";
	else
		id="BSPO:0000120";
		
	FormalRelation rel = new FormalRelation();
	rel.setString(relation);
	rel.setLabel(Dictionary.resrelationQ.get(id));
	rel.setId(id);
	rel.setConfidenceScore((float)1.0);
	
	SimpleEntity entity = new SimpleEntity();
	entity.setLabel("multicellular organism");
	entity.setString("multicellular organism");
	entity.setId("UBERON:0000468");
	entity.setConfidenceScore((float)1.0);
	
	REntity relatedentity = new REntity(rel,entity);

		return relatedentity;
	}
	
	public ArrayList<EntityProposals> getrelatedEntities() {
		return relatedentities;
	}
	public ArrayList<EntityProposals> getPrimaryentities() {
		return primaryentities;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
