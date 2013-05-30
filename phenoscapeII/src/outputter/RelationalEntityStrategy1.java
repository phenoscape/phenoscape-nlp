package outputter;

import java.util.ArrayList;
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
	private Element chara;
	private Element Statement;
	private TermOutputerUtilities ontoutil;
	static XPath pathCharacterUnderStucture;
	ArrayList<EntityProposals> relatedentities;
	ArrayList<EntityProposals> primaryentities;
	ArrayList<EntityProposals> keyentities;

static {
	try {
		pathCharacterUnderStucture = XPath.newInstance(".//character");
	} catch (JDOMException e) {
		e.printStackTrace();
	}
}
	public RelationalEntityStrategy1(Element root, Element chara,
			TermOutputerUtilities ontoutil, ArrayList<EntityProposals> keyentities) {
		// TODO Auto-generated constructor stub
		this.root = root;
		this.chara = chara;
		this.ontoutil = ontoutil;
		this.Statement = this.chara.getParentElement().getParentElement();
		relatedentities= new ArrayList<EntityProposals>();
		this.keyentities=keyentities;
		this.primaryentities = new ArrayList<EntityProposals>();
	}
/*
 * Makes a call to Structureswithsamecharacters() to identify the entities of the relational quality
 */
	public void handle() {
		Structureswithsamecharacters();
		SingleStructures();
	}
	
	
private void SingleStructures() {
	
	try {
		List<Element> characters;
		
			characters = pathCharacterUnderStucture.selectNodes(this.Statement);
		
		Iterator<Element> itr = characters.listIterator();
		while(itr.hasNext())
		{
			Element character = itr.next();
			if((character.getAttribute("name")== this.chara.getAttribute("name"))&&(character.getAttribute("value")== this.chara.getAttribute("value")))
				{
				Element ParentStructure = character.getParentElement();
				if(ParentStructure.getAttributeValue("name").equals(ApplicationUtilities.getProperty("unknown.structure.name")))
					wholeorganismrelentities();
				else
					bilateralstructures(ParentStructure.getAttributeValue("name"));
				}
		}
		
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	
}
private void wholeorganismrelentities() {
	
	if(this.keyentities.size()>1)
		{
		for(int i=1;i<this.keyentities.size();i++)
			this.relatedentities.add(this.keyentities.get(i));
		
		this.primaryentities.add(this.keyentities.get(0));
		}
	else
	{
		//this can be bilateral structure too
	}
}
/*
 * It identifies structures which have the same characters(Relational Quality) 
 * and it returns all the structures which has the same characters as related entities
 * if a structure is a whole organism, it calls bilateralstructures to handle the scenario.
 * 
 * 
 */			@SuppressWarnings("unchecked")
	private void Structureswithsamecharacters() {
		
		try {
			List<Element> characters = pathCharacterUnderStucture.selectNodes(this.Statement);
			
			//remove the main entity as this will be identified before this function call
			Iterator<Element> itr = characters.listIterator();
			while(itr.hasNext())
			{
				Element character = itr.next();
				if(!(character.getAttribute("name").getValue().equals(this.chara.getAttribute("name").getValue()))||!(character.getAttribute("value").getValue().equals(this.chara.getAttribute("value").getValue())))
					itr.remove();
			}
			//First entity will be the main primary entity
			
			if(characters.size()>1)
			{
				if(chara.getParentElement().getAttributeValue("name")!=ApplicationUtilities.getProperty("unknown.structure.name"))
				{
					EntityProposals primaryEntity  = new EntitySearcherOriginal().searchEntity(root, chara.getParentElement().getAttributeValue("id"), chara.getParentElement().getAttributeValue("name"), "", "","");
					this.primaryentities.add(primaryEntity);
				}
					
			}
			//finding the related entities using structures with same character name and value
			for(int i=1;i<characters.size();i++)
			{
				Element character = characters.get(i);
				if((character.getAttribute("name").getValue().equals(this.chara.getAttribute("name").getValue()))&&(character.getAttribute("value").getValue().equals(this.chara.getAttribute("value").getValue())))
					{
					//read the structure(other than whole organism) of this character, find the entity,create entity proposals
					Element ParentStructure = character.getParentElement();
					if(ParentStructure.getAttributeValue("name")!=ApplicationUtilities.getProperty("unknown.structure.name"))
					{
						EntityProposals entity  = new EntitySearcherOriginal().searchEntity(root, ParentStructure.getAttributeValue("id"), ParentStructure.getAttributeValue("name"), "", "","");				
					if(entity!=null)
						this.relatedentities.add(entity);
					//ParentStructure.detach();//Need to check on this
					character.detach();
					}
						
					}
			}
		
					
		} catch (JDOMException e) {
			e.printStackTrace();
		}
		
	}
	
/* It checks, if the structure belongs to lateralsides classes and 
*	if true, it creates "in a right side of" classes on key entities
*/
	private void bilateralstructures(String structname) {

		//If it is a simpleentity add part of mulitcellular organism
		//If it is a composite entity and part of relation, then related entity is one come after part of
		if(XML2EQ.elk.lateralsidescache.get(structname)!=null)
		{
			for(EntityProposals ep:this.keyentities)
			{
				EntityProposals ep1 = new EntityProposals();
				for(Entity e: ep.getProposals())
				{
					if(e instanceof CompositeEntity)
					{
						REntity	old_re = (REntity) ((CompositeEntity) e).getEntity(1);
						FormalRelation rel = new FormalRelation();
						rel.setString("in right side of");
						rel.setLabel(Dictionary.resrelationQ.get("BSPO:0000121"));
						rel.setId("BSPO:0000121");
						rel.setConfidenceScore((float)1.0);
						Entity related = old_re.getEntity();
						REntity new_re = new REntity(rel,related);
						CompositeEntity centity = new CompositeEntity();
						centity.addEntity(((CompositeEntity) e).getEntity(0));
						centity.addEntity(new_re);
						ep1.add(centity);
						
					}
				}	
				if(ep1.getProposals().size()>0)
				this.relatedentities.add(ep1);
			}
		}
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
