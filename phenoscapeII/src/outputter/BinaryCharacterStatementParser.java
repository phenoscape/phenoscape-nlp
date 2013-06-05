/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.List;

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
		super.parseRelations(statement, root);
		super.parseCharacters(statement, root);
		if(this.EQStatements.size()==0){
			try {
				checkandfilterstructuredquality(statement,root);//checks standalone structure for quality values and detach them.
				parseStandaloneStructures(statement, root);
			} catch (JDOMException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		for(EQStatementProposals eqp: this.EQStatements){
			for(EQStatement eq: eqp.getProposals()){
				//update q for all eq candidates
				int flag=0;;
				Quality q = eq.getQuality();
				if(q instanceof RelationalQuality)
					flag=1;
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
					EQStatement falseeq = clone(eq);
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
						EQStatement falseeq = clone(eq);
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
						}
						RelationalQuality rq = new RelationalQuality(nqp,relatedentity);
						EQStatement falseeq = clone(eq);
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
		EntityParser ep = new EntityParser(statement, root, true);
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
		}		
	}

private void checkandfilterstructuredquality(Element statement,Element root) throws JDOMException {
		
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
	}

	private EQStatement clone(EQStatement eq){
		EQStatement eq1 = new EQStatement();
		eq1.setCharacterId(eq.getCharacterId());
		eq1.setEntity(eq.getEntity());
		eq1.setQuality(eq.getQuality());
		eq1.setSource(eq.getSource());
		eq1.setStateId(eq.getStateId()); //TODO: change it for states
		eq1.setDescription(eq.getDescription());
		eq1.setType(eq.getType());
		return eq1;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
