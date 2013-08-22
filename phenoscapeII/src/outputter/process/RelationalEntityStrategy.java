package outputter.process;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
/*
 * 
 * This class identifies related entities. If a quality is identified to be a relational quality,
 * then other structures which contain the same quality can be related entities
 * else
 * If the structure that contains the relational Q is "whole_organism" then all key entities except the first one can be related entities
 * else
 * the structure can be a bilateral structure then all the key entities will be bilateral
 * 
 * 
 */











import outputter.XML2EQ;
import outputter.data.CompositeEntity;
import outputter.data.Entity;
import outputter.data.EntityProposals;
import outputter.data.FormalRelation;
import outputter.data.REntity;
import outputter.data.SimpleEntity;
import outputter.knowledge.Dictionary;

public class RelationalEntityStrategy implements AnnotationStrategy{

	private static final Logger LOGGER = Logger.getLogger(RelationalEntityStrategy.class);   
	ArrayList<EntityProposals> relatedentities;
	ArrayList<EntityProposals> primaryentities;
	ArrayList<EntityProposals> keyentities;
	private Hashtable<String,ArrayList<EntityProposals>> entities = new Hashtable<String,ArrayList<EntityProposals>>();
	private Entity bilateralentity;

public Hashtable<String, ArrayList<EntityProposals>> getEntities() {
		return entities;
	}


	public RelationalEntityStrategy(Entity bilateral) {
		// TODO Auto-generated constructor stub
		this.bilateralentity	= bilateral;
		this.relatedentities= new ArrayList<EntityProposals>();
		this.primaryentities = new ArrayList<EntityProposals>();
	}
/*
 * Makes a call to Structureswithsamecharacters() to identify the entities of the relational quality
 */
	public void handle() {
		bilateralstructures();
	}
	
	

	
/* It checks, if the structure belongs to lateralsides classes and 
*	if true, it creates "in a right side of" classes on key entities
*/
	private void bilateralstructures() {

				EntityProposals RelatedEP = new EntityProposals();
				EntityProposals PrimaryEP = new EntityProposals();

				//Cloning to modify this object and create new related and primary entities
			
				//If it is a simple entity add part of multicellular organism
					if(this.bilateralentity instanceof SimpleEntity)// define
					{
						//Related Entity
						SimpleEntity bilateralclone1 = ((SimpleEntity)this.bilateralentity).clone();
						REntity related1 = this.multicellularrelatedentity("in right side of");
						CompositeEntity centity = new CompositeEntity();
						centity.addEntity((SimpleEntity)bilateralclone1);
						centity.addEntity(related1);
						RelatedEP.add(centity);
						
						//Primary Entities
						related1 = this.multicellularrelatedentity("in left side of");
						CompositeEntity centity1 = new CompositeEntity();
						centity1.addEntity((SimpleEntity)bilateralclone1);
						centity1.addEntity(related1);
						PrimaryEP.add(centity1);
					}
					if(this.bilateralentity instanceof CompositeEntity)//In the composite structure, it identifies the bilateral entity and makes it to be located in a multicellular organism 
					{
						//Cloning to prevent the original entity from being modified
						CompositeEntity bilateralclone1 = ((CompositeEntity)this.bilateralentity).clone();
						CompositeEntity bilateralclone2 = ((CompositeEntity)this.bilateralentity).clone();
						
						//Creating related entities by appending 'in right side of' relation to the bilateral structure
						//for(Entity e:((CompositeEntity) bilateralclone1).getEntities())
						ArrayList<Entity> entities = ((CompositeEntity) bilateralclone1).getEntities();
						for(int i = 0; i < entities.size(); i++ )
						{
							Entity e = entities.get(i);
							if(XML2EQ.elk.lateralsidescache.get(e.getPrimaryEntityLabel())!=null)
							{
								if(e instanceof REntity)
								{
									Entity RelatedE = ((REntity)e).getEntity();//bilateralentity 
									
									REntity entity1 = this.multicellularrelatedentity("in right side of");
									CompositeEntity centity1 = new CompositeEntity();
									centity1.addEntity(RelatedE);
									centity1.addEntity(entity1);
									((REntity) e).setEntity(centity1);
									RelatedEP.add(bilateralclone1);
								}else if(e instanceof SimpleEntity && i ==0)
								{
									//A (part of B) => A (part of (C part of B)): replace the RE following the SE
									//C = 'dorsal region'
									REntity re = (REntity) entities.get(i+1);
									CompositeEntity centity1 = new CompositeEntity(); //C part of B									
									centity1.addEntity(Dictionary.dorsalregion);
									centity1.addEntity(re.clone());
									re.setEntity(centity1);
									entities.set(i+1, re);
									RelatedEP.add(bilateralclone1);
								}
							}
						}	
						//Creating primary entities by appending in right side of relation to the bilateral structure
						//for(Entity e:((CompositeEntity) bilateralclone2).getEntities())
						entities = ((CompositeEntity) bilateralclone2).getEntities();
						for(int i = 0; i < entities.size(); i++ )
						{
							Entity e = entities.get(i);
							if(XML2EQ.elk.lateralsidescache.get(e.getPrimaryEntityLabel())!=null)
							{
								if(e instanceof REntity)
								{
									Entity RelatedE = ((REntity)e).getEntity();//bilateralentity 
									
									//Primary Entities
									REntity entity2 = this.multicellularrelatedentity("in left side of");
									CompositeEntity centity2 = new CompositeEntity();
									centity2.addEntity(RelatedE);
									centity2.addEntity(entity2);
									((REntity) e).setEntity(centity2);
									PrimaryEP.add(bilateralclone2);
									
								}else if(e instanceof SimpleEntity && i ==0)
								{
									//A (part of B) => A (part of (C part of B)): replace the RE following the SE
									//C = 'ventral region'
									REntity re = (REntity) entities.get(i+1);
									CompositeEntity centity1 = new CompositeEntity(); //C part of B									
									centity1.addEntity(Dictionary.ventralregion);
									centity1.addEntity(re.clone());
									re.setEntity(centity1);
									entities.set(i+1, re);
									PrimaryEP.add(bilateralclone2);
								}
							}
						}	
					}
	
				if(RelatedEP.getProposals().size()>0)
				{
					this.relatedentities.add(RelatedEP);
					this.primaryentities.add(PrimaryEP);
				}
			//This will contain all the identified related and primary entities
				this.entities.put("Related Entities",this.relatedentities);
				this.entities.put("Primary Entity", this.primaryentities);

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
