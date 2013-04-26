/**
 * 
 */
package outputter;

import java.util.ArrayList;

import org.jdom.Element;

/**
 * @author updates
 * parse out Entities and Qualities, form EQ directly without access state statements as they are just binary values, i.e., T/F. (not two different vaules, but binary values)
 */
public class BinaryCharacterStatementParser extends StateStatementParser {

	/**
	 * @param ontologyIRIs
	 */
	public BinaryCharacterStatementParser() {
		super(null);
	}

	/**
	 * @param statement: the character statement in a binary statement 
	 */
	public void parse(Element statement, Element root){
		ArrayList<EQStatement> falsestatements = new ArrayList<EQStatement>();
		super.parse(statement, root);
		for(EQStatement eq: this.EQStatements){
			Quality q = eq.getQuality();
			if(q==null){
				//q = "present"
				q = new Quality();
				q.setString("present");
				q.setLabel("PATO:present");
				q.setId("PATO:0000467");
				q.setConfidenceScore((float)1.0);
				//create absent quality
				Quality absent = new Quality();
				absent.setString("absent");
				absent.setLabel("PATO:absent");
				absent.setId("PATO:0000462");
				absent.setConfidenceScore((float)1.0);
				EQStatement falseeq = clone(eq);
				falseeq.setQuality(absent);
				falsestatements.add(falseeq);			
			}else{
				//generate negated quality
				String [] parentinfo = ontoutil.retreiveParentInfoFromPATO(q.getId()); 
				Quality parentquality = new Quality();
				parentquality.setString(parentinfo[1]);
				parentquality.setLabel(parentinfo[1]);
				parentquality.setId(parentinfo[0]);
				NegatedQuality nq = new NegatedQuality(q, parentquality);
				EQStatement falseeq = clone(eq);
				falseeq.setQuality(nq);
				falsestatements.add(falseeq);		
			}
		}
		this.EQStatements.addAll(falsestatements);
	}
	
	
	private EQStatement clone(EQStatement eq){
		EQStatement eq1 = new EQStatement();
		eq1.setCharacterSource(eq.getCharacterSource());
		eq1.setEntity(eq.getEntity());
		eq1.setQuality(eq.getQuality());
		eq1.setSource(eq.getSource());
		eq1.setStateSource(eq.getStateSource()); //TODO: change it for states
		return eq1;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
