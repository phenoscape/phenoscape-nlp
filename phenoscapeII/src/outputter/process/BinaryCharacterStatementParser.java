/**
 * 
 */
package outputter.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import outputter.Utilities;
import outputter.XML2EQ;
import outputter.data.EQProposals;
import outputter.data.Entity;
import outputter.data.EntityProposals;
import outputter.data.NegatedQuality;
import outputter.data.Quality;
import outputter.data.QualityProposals;
import outputter.data.RelationalQuality;
import outputter.knowledge.Dictionary;
import outputter.knowledge.TermOutputerUtilities;
import outputter.prep.XMLNormalizer;

/**
 * @author updates
 * parse out Entities and Qualities, form EQ directly without access state statements as they are just binary values, i.e., T/F. (not two different vaules, but binary values)
 */
public class BinaryCharacterStatementParser extends StateStatementParser {
	private static final Logger LOGGER = Logger.getLogger(BinaryCharacterStatementParser.class);  
	//public String characterlabel;
	private int binaryType = 0;
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
			LOGGER.error("", e);
		}
	}

	public BinaryCharacterStatementParser(TermOutputerUtilities ontoutil, String characterlabel, int btype) {
		super(ontoutil, null, null,characterlabel);
		this.binaryType = btype;
		//this.characterlabel=characterlabel;

	}

	/**
	 * @param statement: the character statement in a binary statement 
	 */
	/*public void parse(Element statement, Element root){
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
			//	LOGGER.error("", e);
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
	}*/

	/**
	 * A ventral_to B =>from super: E: A  Q: ventral_to B
	 * expected final results:
	 * Yes: A, ventral_to B
	 * No: A, not ventral_to B
	 * 
	 * A ventral_to B =>E: A  Q: ventral_to B
	 * present: A ventral_to B, present
	 * absent: A ventral_to B, absent
	 * 
	 * red flowers =>E: red flowers Q: null
	 * Yes: red flowers, present
	 * No: red flowers, absent
	 * 
	 * red flowers =>E: red flowers Q: null
	 * present: red flowers, present
	 * absent: red flowers, absent
	 * 
	 * flowers red =>E: flowers Q: red
	 * yes: flowers, red
	 * no: flowers, not red
	 * 
	 * flowers red =>E: flowers Q: red
	 * present: flowers, red
	 * absent: flowers, not red
	 * 
	 * 
	 * @param statement a character statement that is a binary statement 
	 * @param root the root element of the parent xml document 
	 * @param posempty the postive EQProposals holding metadata and original text
	 * @param negempty the negative EQProposals holding metadata and original text
	 */
	public void parse(Element statement, Element root, EQProposals posempty, EQProposals negempty){
		ArrayList<EQProposals> negativestatements = new ArrayList<EQProposals>();
		//super.parseMetadata(statement, root, posempty);
		super.parseRelationsFormEQ(statement, root, posempty);
		super.parseCharactersFormEQ(statement, root, posempty);
		if(this.EQStatements.size()==0){
			parseStandaloneStructures(statement, root, posempty);
		}
		for(EQProposals eqp: this.EQStatements){
			QualityProposals qp = eqp.getQuality();
			if(qp==null){//no qp, create qp
				presentAbsentQuality(negempty, negativestatements, eqp);
			}else{//has q, generate and add negated quality
				for(Quality q: qp.getProposals()){
					if(q instanceof RelationalQuality) //relational, generate and add negated quality
					{	
						boolean postcomposed = false;
						// 1: present/absent; 2: yes/no
						if(this.binaryType==1) postcomposed = postcomposeRelationalQuality(negempty, negativestatements, eqp, q);
						if(this.binaryType==2 || !postcomposed) negateRelationalQuality(negempty, negativestatements,
								eqp, q); // a ventral to b: yes =>
					}else{ //not relational, generate and add the negated quality
						negateQuality(negempty, negativestatements, eqp, q);
					}

				}
			}
		}
		//}
		this.EQStatements.addAll(negativestatements);
		//if(XML2EQ.isRecordperformance()) populateStateLabel(this.EQStatements,root);

	}

	/**
	 * A ventral_to B =>E: A  Q: ventral_to B
	 * return: 
	 * present: A ventral_to B, present
	 * absent: A ventral_to B, absent
	 * 
	 * post-compose Q to Entities, update eqp, and add negated to the negative EQProposals
	 * @param negativestatements
	 * @param q
	 * @return postcomposition success or not
	 */
	private boolean postcomposeRelationalQuality(EQProposals negempty,
			ArrayList<EQProposals> negativestatements, EQProposals eqp, Quality q) {
		ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
		if(eqp.getEntity()==null) return false;
		entities.add(eqp.getEntity());
		ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>();
		QualityProposals qp = new QualityProposals();
		qp.add(q);
		qualities.add(qp);
		boolean success = Utilities.postcompose(entities, qualities); //postcompose
		if(!success) return success;
		//update eqp
		eqp.setEntity(entities.get(0));
		qp = new QualityProposals();
		qp.setPhrase("present");
		Quality present = Dictionary.present;
		present.setSearchString("");
		present.setString("present");
		present.setConfidenceScore((float)1.0);
		qp.add(present);
		eqp.setQuality(qp);//update qp
		
		//create negaged
		qp = new QualityProposals();
		qp.setPhrase("absent");
		Quality absent = Dictionary.absent;
		present.setSearchString("");
		present.setString("absent");
		present.setConfidenceScore((float)1.0);
		qp.add(absent);
		
		EQProposals falseeq = eqp.clone();
		falseeq.setStateId(negempty.getStateId());
		falseeq.setStateText(negempty.getStateText());
		falseeq.setQuality(qp);
		negativestatements.add(falseeq);
		return success;
	}

	private void presentAbsentQuality(EQProposals negempty,
			ArrayList<EQProposals> negativestatements, EQProposals eqp) {
		QualityProposals qp;
		//q = "present"
		qp = new QualityProposals();
		Quality q = Dictionary.present;
		q.setSearchString("");
		q.setString("present");
		q.setConfidenceScore((float)1.0);
		qp.add(q);
		eqp.setQuality(qp);//update qp
		//check for any positive state statement present(yes,true etc.), if so assign that state to it
		//create absent quality
		Quality absent = Dictionary.absent;
		absent.setSearchString("");
		absent.setString("absent");
		absent.setConfidenceScore((float)1.0);
		qp = new QualityProposals();
		qp.add(absent);
		EQProposals falseeq = eqp.clone();
		falseeq.setStateId(negempty.getStateId());
		falseeq.setStateText(negempty.getStateText());
		falseeq.setQuality(qp);
		negativestatements.add(falseeq);
		//handle negative statements at the last
	}

	private void negateQuality(EQProposals negempty,
			ArrayList<EQProposals> negativestatements, EQProposals eqp, Quality q) {
		if(q.getId()!=null)
		{
			QualityProposals negativeqp = new QualityProposals();
			String [] parentinfo = TermOutputerUtilities.retreiveParentInfoFromPATO(q.getId()); 
			if(parentinfo!=null)
			{
				Quality parentquality = new Quality();
				parentquality.setSearchString("");
				parentquality.setString(parentinfo[1]);
				parentquality.setLabel(parentinfo[1]);
				parentquality.setId(parentinfo[0]);
				NegatedQuality nq = new NegatedQuality(q, parentquality);
				negativeqp.add(nq);
				EQProposals falseeq = eqp.clone();
				falseeq.setQuality(negativeqp);
				falseeq.setStateId(negempty.getStateId());
				falseeq.setStateText(negempty.getStateText());
				negativestatements.add(falseeq);	
			}
		}
	}

	private void negateRelationalQuality(EQProposals negempty,
			ArrayList<EQProposals> negativestatements, EQProposals eqp, Quality q) {
		QualityProposals negativeqp = new QualityProposals();
		QualityProposals thisqp = ((RelationalQuality)q).getQuality();
		EntityProposals relatedentity = ((RelationalQuality)q).getRelatedEntity();
		QualityProposals nqp = new QualityProposals();
		for(Quality q1:thisqp.getProposals())
		{
			if(q1.getId()!=null)
			{
					String [] parentinfo = TermOutputerUtilities.retreiveParentInfoFromPATO(q1.getId()); 
					if(parentinfo!=null)
					{
						Quality parentquality = new Quality();
						parentquality.setSearchString("");
						parentquality.setString(parentinfo[1]);
						parentquality.setLabel(parentinfo[1]);
						parentquality.setId(parentinfo[0]);
						NegatedQuality nq = new NegatedQuality(q1, parentquality);
						nqp.getProposals().add(nq);
					}
			}
		}
		RelationalQuality rq = new RelationalQuality(nqp,relatedentity);
		negativeqp.add(rq);
		EQProposals falseeq = eqp.clone();
		falseeq.setQuality(negativeqp);
		falseeq.setStateId(negempty.getStateId());
		falseeq.setStateText(negempty.getStateText());
		negativestatements.add(falseeq);
	}
	
	/*
	 * 
	 * This method populates the state statement for all binary statements
	 * The first binary state statement id and label will be populated
	 */

/*	private void populateStateLabel(ArrayList<EQProposals> eqstatements,Element root) {

		try {
		
			Element state = (Element) XMLNormalizer.pathStateStatement.selectSingleNode(root);
			Element Text = (Element) XMLNormalizer.pathText.selectSingleNode(state);
			String stateid = state.getAttributeValue("state_id");
			String text = Text.getTextTrim();
					
			System.out.println(text);
			
			for(EQProposals eqp:eqstatements)
			{
				eqp.setDescription(text);
				eqp.setStateId(stateid);
			}
			
			
		} catch (JDOMException e) {
			e.printStackTrace();
		}	

		
	}*/

	/*
	 * 
	 * This method populates the state statement for all binary statements
	 * 
	 */

	@SuppressWarnings("unchecked")
	/*private void populateStateLabel(ArrayList<EQProposals> eqstatements,Element root) {

		try {
		
			List<Element> states =  XMLNormalizer.pathStateStatement.selectNodes(root);
			Hashtable<String,String> binary = new Hashtable<String,String>();
			
			//Stores the information about positive and negative states
			for (Element state : states) {
				Element text = (Element) pathText2.selectSingleNode(state);
				String value = text.getTextTrim();
				if(value.matches("(" + Dictionary.binaryTvalues + ")"))
				{
					binary.put("positive", state.getAttributeValue("state_id")+"@@"+value);
				}
				
				if(value.matches("(" + Dictionary.binaryFvalues + ")"))
				{
					binary.put("negative", state.getAttributeValue("state_id")+"@@"+value);
				}
			}
			//Checks if the quality proposal is a negated quality or false values
			for(EQProposals eqp:eqstatements)
			{
				QualityProposals qp = eqp.getQuality();
				for(Quality q:qp.getProposals())
				{
					if((q!=null)&&(q.getLabel()!=null))
					{
					if((q instanceof NegatedQuality)||(q.getLabel().matches(".*(" + Dictionary.binaryFvalues + ")")))
					{
						eqp.setStateId(binary.get("negative")!=null?binary.get("negative").split("@@")[0]:(binary.get("positive")!=null?binary.get("positive").split("@@")[0]:""));
						eqp.setStateText(binary.get("negative")!=null?binary.get("negative").split("@@")[1]:(binary.get("positive")!=null?binary.get("positive").split("@@")[1]:""));
						break;
					}else
					{
						eqp.setStateId(binary.get("positive")!=null?binary.get("positive").split("@@")[0]:(binary.get("negative")!=null?binary.get("negative").split("@@")[0]:""));
						eqp.setStateText(binary.get("positive")!=null?binary.get("positive").split("@@")[1]:(binary.get("negative")!=null?binary.get("negative").split("@@")[1]:""));
						break;
					}
					}
				}
			}
			
		} catch (JDOMException e) {
			e.printStackTrace();
		}	

		
	}*/
	
/*	private void populateStateLabel(ArrayList<EQProposals> eqstatements,Element root) {

		try {
		
			ArrayList<Element> states = (ArrayList<Element>) XMLNormalizer.pathStateStatement.selectNodes(root);
			String assigned="";
			
			for(EQProposals eqp:eqstatements)
			{
				QualityProposals quality = eqp.getQuality();
				
				for(Quality q:quality.proposals)
				{
					int flag=0;
					for(Element state:states)
					{
						Element Text = (Element) XMLNormalizer.pathText.selectSingleNode(state);
						String text = Text.getTextTrim();
						
						if((assigned.equals("present")==false)||((text.matches("("+Dictionary.binaryTvalues +")")==true)&&(q.getLabel().matches("("+Dictionary.binaryTvalues +")")==true)))
						{
							eqp.setStateId(state.getAttributeValue("state_id"));
							eqp.setDescription(text);
							flag=1;
							assigned="present";
							break;
						}
						
						if((assigned.equals("absent")==false)||(text.matches("("+Dictionary.binaryFvalues +")")==true)&&(q.getLabel().matches("("+Dictionary.binaryFvalues +")")==true)))
						{
							eqp.setStateId(state.getAttributeValue("state_id"));
							eqp.setDescription(text);
							assigned="absent";
							flag=1;
							break;
						}

					}
					if(flag==1)
						break;
					
				}
			}
			
			
		} catch (JDOMException e) {
			e.printStackTrace();
		}	

		
	}*/

	/**
	 * construct EQstatement from standalone <structure>, as in "anal fin/absent|present" 
	 * @param statement
	 * @param root
	 */
	private void parseStandaloneStructures(Element statement, Element root, EQProposals empty) {
		try{
			ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
			ArrayList<Structure2Quality> s2qs = new ArrayList<Structure2Quality>();
			String checked = "";
			List<Element> structures = XMLNormalizer.pathNonWholeOrganismStructure.selectNodes(statement);
			ArrayList<String> RelatedStructures = new ArrayList<String>(); //keep a record on related entities, which should not be processed again
			for(Element structure: structures){
				String structureid = structure.getAttributeValue("id");
				if(!checked.contains(structureid+",")){
					String parents = Utilities.getIdsOnPartOfChain(root, structureid);
					String structurename = Utilities.getStructureName(root, structureid);
					EntityParser ep = new EntityParser(statement, root, structureid, structurename, null, true);
					if(ep.getEntity()!=null) entities.addAll(ep.getEntity());
					if(ep.getQualityStrategy()!=null) s2qs.add(ep.getQualityStrategy());
					checked += structureid+","+parents+",";
				}
			}
			resolve(entities, s2qs, statement, root); //after resolution, entities holds good entities

			/*for(EntityProposals entityp: entities){
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
				}
				this.EQStatements.add(eqp);
			}*/

			for(EntityProposals entityp: entities){
				//EQProposals eqp= new EQProposals();
				EQProposals eqp= empty.clone();
				eqp.setEntity(entityp);
				//eqp.setSource(super.src);
				//eqp.setCharacterId(super.characterid);
				//eqp.setStateId(super.stateid);
				eqp.setStateText(statement.getChildText("text"));
				eqp.setType("character");
				//eqp.setCharacterlabel(super.characterlabel);
				this.EQStatements.add(eqp);
			}			
		}catch(Exception e){
			LOGGER.error("", e);
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
