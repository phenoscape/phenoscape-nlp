/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

/**
 * @author updates
 * parse out Entities and Qualities, form EQ directly without access state statements as they are just binary values, i.e., T/F. (not two different vaules, but binary values)
 */
public class BinaryCharacterStatementParser extends StateStatementParser {

	/**
	 * @param ontologyIRIs
	 */
	static XPath pathstructure;
	static XPath pathrelation;
	static{
		try{
			pathstructure = XPath.newInstance(".//structure");
			pathrelation = XPath.newInstance(".//relation");

		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public BinaryCharacterStatementParser(TermOutputerUtilities ontoutil) {
		super(ontoutil, null, null);
	}

	/**
	 * @param statement: the character statement in a binary statement 
	 */
	public void parse(Element statement, Element root){
		ArrayList<EQStatementProposals> negativestatements = new ArrayList<EQStatementProposals>();
		super.parseMetadata(statement, root);
		super.parseRelationsFormEQ(statement, root);
		super.parseCharactersFormEQ(statement, root);
		if(this.EQStatements.size()==0){
			//try {
			//checkandfilterstructuredquality(statement,root);//checks standalone structure for quality values and detach them.
			parseStandaloneStructures(statement, root);
			//} catch (JDOMException e) {
			// TODO Auto-generated catch block
			//	e.printStackTrace();
			//}
		}
		for(EQStatementProposals eqp: this.EQStatements){
			for(EQStatement eq: eqp.getProposals()){
				//update q for all eq candidates
				int flag=0;;
				Quality q = eq.getQuality();
				if(q instanceof RelationalQuality)
					flag=1;// This will help us to print relational quality properly
				if(q==null){
					//create q
					//q = "present"
					q = new Quality();
					q.setString("present");
					q.setLabel("PATO:present");
					q.setId("PATO:0000467");
					q.setConfidenceScore((float)1.0);
					eq.setQuality(q);//update q
					//create absent quality
					Quality absent = new Quality();
					absent.setString("absent");
					absent.setLabel("PATO:absent");
					absent.setId("PATO:0000462");
					absent.setConfidenceScore((float)1.0);
					EQStatement falseeq = eq.clone();
					falseeq.setQuality(absent);
					EQStatementProposals negativeproposals = new EQStatementProposals();
					negativeproposals.add(falseeq);
					negativestatements.add(negativeproposals);			
				}else{
					//generate negated quality
					if(flag==0)
					{
						if(q.getId()!=null)
						{
							@SuppressWarnings("static-access")
							String [] parentinfo = ontoutil.retreiveParentInfoFromPATO(q.getId()); 
							if(parentinfo!=null)
							{
								Quality parentquality = new Quality();
								parentquality.setString(parentinfo[1]);
								parentquality.setLabel(parentinfo[1]);
								parentquality.setId(parentinfo[0]);
								NegatedQuality nq = new NegatedQuality(q, parentquality);
								EQStatement falseeq = eq.clone();
								falseeq.setQuality(nq);
								EQStatementProposals negativeproposals = new EQStatementProposals();
								negativeproposals.add(falseeq);
								negativestatements.add(negativeproposals);	
							}
						}
					}
					else
					{
						QualityProposals qp = ((RelationalQuality)q).relationalquality;
						EntityProposals relatedentity = ((RelationalQuality)q).relatedentity;
						QualityProposals nqp = new QualityProposals();

						for(Quality q1:qp.proposals)
						{
							if(q1.getId()!=null)
							{
								@SuppressWarnings("static-access")
								 
								String complementQuality = Dictionary.complementRelations.get(q1.getLabel());
								if(complementQuality==null)
								{
								String [] parentinfo = ontoutil.retreiveParentInfoFromPATO(q1.getId()); 
								if(parentinfo!=null)
								{
									Quality parentquality = new Quality();
									parentquality.setString(parentinfo[1]);
									parentquality.setLabel(parentinfo[1]);
									parentquality.setId(parentinfo[0]);
									NegatedQuality nq = new NegatedQuality(q1, parentquality);
									nqp.proposals.add(nq);
								}
								}
								else
								{
									complementQuality = Utilities.removeprepositions(complementQuality);
									Hashtable<String,String> cq = Dictionary.relationalqualities.get(complementQuality);
									Set<String> keys = cq.keySet();
									for(String key:keys)
									{
										Quality negated =new Quality();
										negated.setLabel(key);
										negated.setId(cq.get(key));
										negated.setString(key);
										nqp.proposals.add(negated);

									}
								}
							}
						}
						RelationalQuality rq = new RelationalQuality(nqp,relatedentity);
						EQStatement falseeq = eq.clone();
						falseeq.setQuality(rq);
						EQStatementProposals negativeproposals = new EQStatementProposals();
						negativeproposals.add(falseeq);
						negativestatements.add(negativeproposals);	

					}
				}
			}
		}
		this.EQStatements.addAll(negativestatements);
	}

	/**
	 * construct EQstatement from standalone <structure>, as in "anal fin/absent|present" 
	 * @param statement
	 * @param root
	 */
	private void parseStandaloneStructures(Element statement, Element root) {
		try{
			ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
			ArrayList<Structure2Quality> s2qs = new ArrayList<Structure2Quality>();
			String checked = "";
			List<Element> structures = XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(statement);
			ArrayList<String> RelatedStructures = new ArrayList<String>(); //keep a record on related entities, which should not be processed again
			for(Element structure: structures){
				String structureid = structure.getAttributeValue("id");
				if(!checked.contains(structureid+",")){
					String parents = Utilities.getStructureChainIds(root, "//relation[@from='" + structureid + "']", 0);
					String structurename = Utilities.getStructureName(root, structureid);
					EntityParser ep = new EntityParser(statement, root, structureid, structurename, true);
					if(ep.getEntity()!=null) entities.add(ep.getEntity());
					if(ep.getQualityStrategy()!=null) s2qs.add(ep.getQualityStrategy());
					checked += structureid+","+parents+",";
				}
			}
			resolve(entities, s2qs, statement, root); //after resolution, entities holds good entities

			for(EntityProposals entityp: entities){
				EQStatementProposals eqp= new EQStatementProposals();
				for(Entity entity: entityp.getProposals()){
					EQStatement eq = new EQStatement();
					eq.setEntity(entity);
					eq.setSource(super.src);
					eq.setCharacterId(super.characterid);
					eq.setStateId(super.stateid);
					eq.setDescription(super.text);
					eq.setType("character");
					eqp.add(eq);
					//this.EQStatements.add(eq);
				}
				this.EQStatements.add(eqp);
			}
		}catch(Exception e){
			e.printStackTrace();
		}


		/*EntityParser ep = new EntityParser(statement, root, true);
		ArrayList<EntityProposals> entities = ep.getEntities();
		for(EntityProposals entityp: entities){
			EQStatementProposals eqp= new EQStatementProposals();
			for(Entity entity: entityp.getProposals()){
				EQStatement eq = new EQStatement();
				eq.setEntity(entity);
				eq.setSource(super.src);
				eq.setCharacterId(super.characterid);
				eq.setStateId(super.stateid);
				eq.setDescription(super.text);
				eq.setType("character");
				eqp.add(eq);
				//this.EQStatements.add(eq);
			}
			this.EQStatements.add(eqp);
		}	*/	
	}

	/**
	 * TODO
	 * 1. determine keyentities, post-compose entities with quality 2. approve/disapprove structure2quality results
	 * 3. constructure EQ for non-key entities
	 * @param entities
	 * @param s2qs
	 * @param statement
	 * @param root
	 */
	private void resolve(ArrayList<EntityProposals> entities,
			ArrayList<Structure2Quality> s2qs, Element statement, Element root) {
		// TODO Auto-generated method stub

		//if a s2q is approved, use the quality and call s2q.cleanHandledStructures
		//Decision should be made based on confidence score
	}

	/*private void checkandfilterstructuredquality(Element statement,Element root) throws JDOMException {

		String structname;
		String structid;

		List<Element> structures = pathstructure.selectNodes(statement);
		//Get all the structures and individually check, if each are qualities
		for(Element structure:structures)
		{
			structname = structure.getAttributeValue("name");
			structid = structure.getAttributeValue("id");
			Structure2Quality rq = new Structure2Quality(root,
					structname, structid, null);
			rq.handle();
			//If any structure is a quality detach all the structures containing the structure id
			if(rq.qualities.size()>0)
				structure.detach(); //no need to detach relations because the StateStatementParser was already called (the relations are handled there)	
		}
	}*/



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
