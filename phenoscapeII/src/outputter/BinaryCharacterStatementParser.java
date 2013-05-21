/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author updates
 * parse out Entities and Qualities, form EQ directly without access state statements as they are just binary values, i.e., T/F. (not two different vaules, but binary values)
 */
public class BinaryCharacterStatementParser extends StateStatementParser {
	
	/**
	 * @param ontologyIRIs
	 */
	public BinaryCharacterStatementParser(TermOutputerUtilities ontoutil) {
		super(ontoutil, null, null);
	}

	/**
	 * @param statement: the character statement in a binary statement 
	 */
	public void parse(Element statement, Element root){
		ArrayList<EQStatementProposals> negativestatements = new ArrayList<EQStatementProposals>();
		super.parse(statement, root);
		if(this.EQStatements.size()==0){
			parseStandaloneStructures(statement, root);
		}
		for(EQStatementProposals eqp: this.EQStatements){
			for(EQStatement eq: eqp.getProposals()){
				//update q for all eq candidates
				Quality q = eq.getQuality();
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
					if(q.getId()!=null)
					{
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
