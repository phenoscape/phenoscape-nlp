/**
 * 
 */
package outputter.process;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import outputter.ApplicationUtilities;
import outputter.Utilities;
import outputter.XML2EQ;
import outputter.data.CompositeEntity;
import outputter.data.CompositeQuality;
import outputter.data.Entity;
import outputter.data.EntityProposals;
import outputter.data.FormalConcept;
import outputter.data.FormalRelation;
import outputter.data.NegatedQuality;
import outputter.data.Quality;
import outputter.data.QualityProposals;
import outputter.data.REntity;
import outputter.data.RelationalQuality;
import outputter.data.SimpleEntity;
import outputter.knowledge.Dictionary;
import outputter.knowledge.PermittedRelations;
import outputter.knowledge.TermOutputerUtilities;
import outputter.search.EntitySearcherOriginal;
import outputter.search.TermSearcher;


/**
 * @author hong cui
 * Handles a character of a structure
 * grab character and constrain info from <character> tag
 * 
 *  
 * Could character be a relationalquality?
 * yes, for example, "fused"
 */
public class CharacterHandler {
	//input
	private TermOutputerUtilities ontoutil;
	Element root;
	Element chara;
	ArrayList<String> qualityclues; //may have multiple qualityclues: "color and shape of abc"
	private ArrayList<EntityProposals> keyentities;
	boolean fromcharacterstatement = false;
	public static final String prepositions = "above|across|after|along|among|amongst|around|as|at|before|behind|beneath|between|beyond|by|for|from|in|into|near|of|off|on|onto|out|outside|over|than|throughout|to|toward|towards|up|upward|with|without";

	//results
	ArrayList<EntityProposals> entity; //the entity result will be saved here, which may be null, indicating the key entities parsed from the character statement should be used for this character
	ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>(); //the quality result will be saved here. Because n structures may be involved in constraints (hence multiple relational qualities), this needs to be an arraylist. May be relationalquality, simple quality, or negated quality
	ArrayList<EntityProposals> entityparts = new ArrayList<EntityProposals>(); //come from constraints, may have multiple.
	ArrayList<EntityProposals> primaryentities = new ArrayList<EntityProposals>(); //entities no need to be resolved
	boolean donotresolve=false;// This is to resolve between key entities and relational quality entities

	//used in process
	boolean resolve = false;
	private ToBeResolved tobesolvedentity;
	ArrayList<EntityProposals> relatedentities = new ArrayList<EntityProposals>();
	ArrayList<Entity> bilateral = new ArrayList<Entity>();
	static final private String specialprep = "by|through|via";
	static XPath pathCharacterUnderStucture;

	private static final Logger LOGGER = Logger.getLogger(CharacterHandler.class);   
	/**
	 * @param keyentities 
	 * 
	 */
	public CharacterHandler(Element root, Element chara, TermOutputerUtilities ontoutil, ArrayList<String> qualityclues, ArrayList<EntityProposals> keyentities, boolean fromecharacterstatement) {
		this.root = root;
		this.chara = chara;
		this.ontoutil = ontoutil;
		this.qualityclues = qualityclues;
		this.keyentities = keyentities;
		this.fromcharacterstatement = fromcharacterstatement;
		try {
			this.pathCharacterUnderStucture = XPath.newInstance(".//character");
		} catch (JDOMException e) {
			LOGGER.error("", e);
		}
	}

	/**
	 * 
	 * @param root
	 * @param chara
	 * @return
	 */
	public void handle(){
		try {
			parseEntity();
			parseQuality();
			if(resolve) resolve();

		} catch (Exception e) {
			LOGGER.error("", e);
		}
	}

	/**
	 * can't be called out of context, so can't be a public method
	 */
	private void parseEntity(){
		Element structure = chara.getParentElement();
		if(structure.getAttributeValue("name").compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))!=0){
			String structureid = structure.getAttributeValue("id");
			String structurename = Utilities.getStructureName(root, structureid);
			EntityParser ep = new EntityParser(chara, root, structureid, structurename, keyentities, fromcharacterstatement);
			this.tobesolvedentity = new ToBeResolved(structureid);
			this.tobesolvedentity.setEntityCandidate(ep.getEntity());
			this.tobesolvedentity.setStructure2Quality(ep.getQualityStrategy());
			this.resolve = true;			
			this.entity=ep.getEntity();
			if(this.entity!=null)
				this.primaryentities.addAll(this.entity);
		}

		/*String structurename = (structure.getAttribute("constraint")!=null? 
				structure.getAttributeValue("constraint"): ""+" "+structure.getAttributeValue("name")).trim();
		String structureid = structure.getAttributeValue("id");
		if(structurename.compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))!=0){
		 //otherwise, this.entity remains null
			//parents separated by comma (,).
			String parents = Utilities.getStructureChain(root, "//relation[@name='part_of'][@from='" + structureid + "']", 0);
			this.entity = new EntitySearcherOriginal().searchEntity(root, structureid, structurename, parents, structurename, "part_of");	


			//if entity match is not very strong, consider whether the structure is really a quality
			//if(this.entity.higestScore() < 0.8f){
			//	Structure2Quality rq = new Structure2Quality(root, structurename, structureid, null);
			//	rq.handle();
			//	ArrayList<QualityProposals> qualities = rq.qualities;
			//	if(qualities.size()>0){
			//		boolean settled = false;
			//		for(QualityProposals qp : qualities){
			//			if(qp.higestScore() > this.entity.higestScore() && (this.keyentities!=null && this.keyentities.size()>0)){
			//				this.entity = null;
			//				this.qualities = qualities;
			//			break;
			//			}
				//	}
			//		if(!settled){
			//			//park the case and resolve it later after the quality is parsed
			//			this.tobesolvedentity = new ToBeSolved(structurename, structureid, this.entity, qualities);
			//			this.resolve = true;
			//		}					
			//	}

			//}

			this.primaryentities.add(this.entity);

		//}		*/
	}


	private void parseQuality() throws Exception{
		// characters => quality
		//get quality candidate
		String quality = Utilities.formQualityValueFromCharacter(chara);
		String qualitycopy =quality;

		boolean special_case = false;
		// The below code handles all measure related cases like length,width,depth etc.
		if((quality.matches(".*(\\d)+.*")==true)||(quality.equals("")==true)||(quality.equals("not")==true||(quality.matches(".*(width|height|length|broad|depth).*")||(quality.matches(".*(half|full|quarter|much).*")))))
		{
			preProcess(this.chara.getAttributeValue("name"));//handles height/width cases

			if((this.chara.getAttributeValue("name")!=null)&&(this.chara.getAttributeValue("name").matches(".*(width|length|height|depth|broad).*")))
			{
				//To handle width,length statements of a same structure
				if((this.chara.getAttributeValue("constraint")!=null)&&(this.chara.getAttributeValue("constraint").matches(".*(width|length|height|depth|broad).*"))&&(this.chara.getAttributeValue("constraintid")==null))//constraint id should be null
				{
					special_case =specialSizeCaseSameStructure();// handles when two properties of same entity is compared
				}
				else if((this.chara.getAttributeValue("constraint")!=null)&&(this.chara.getAttributeValue("constraintid")!=null))
				{
					//if constraintid is not null then two different structures are being compared
					special_case = specialCaseDifferentStructures();

				}else//handles the case where a single property is being discussed
				{
					quality = this.chara.getAttributeValue("name");
				}
			}
			quality=format(quality);

		}
		if(special_case)
		{
			return;
		}

		boolean negated = false;
		if(quality.startsWith("not ")){
			negated = true;
			quality = quality.substring(quality.indexOf(" ")+1).trim(); //deal with negated quality here
		}
		//is the candidate a relational quality?
		QualityProposals relationalquality = PermittedRelations.matchInPermittedRelation(quality, false,1);
		if(relationalquality!=null){
			//attempts to find related entity in constraints
			// constraints = qualitymodifier if quality is a relational quality
			boolean usedconstraint = false;
			if (chara.getAttribute("constraintid") != null) {
				ArrayList<EntityProposals> relatedentities = findEntityInConstraints();
				String constraint = chara.getAttributeValue("constraint").trim();
				//check and see if prep of the relation need to be matched: 'separated from' <> 'separated by'
				String prep = constraint.contains(" ")? constraint.substring(0, constraint.indexOf(" ")): constraint;
				boolean check = false;
				if(prep.matches(this.specialprep )) check = true;
				for(Quality rq: relationalquality.getProposals()){
					String label = rq.getLabel();
					String labelprep = label.contains(" ")? label.substring(0, constraint.indexOf(" ")): label;
					if(!check || (check && labelprep.matches(CharacterHandler.prepositions) && labelprep.compareTo(prep)==0)){
						for(EntityProposals relatedentity: relatedentities){
							QualityProposals qproposals = new QualityProposals();
							qproposals.add(new RelationalQuality(relationalquality, relatedentity));
							qproposals.setPhrase(quality);
							Utilities.addQualityProposals(qualities, qproposals);
							usedconstraint = true;
							//this.qualities.add(qproposals);
						}
					}
				}
				/*ArrayList<EntityProposals> relatedentities = findEntityInConstraints();
				for(EntityProposals relatedentity: relatedentities){
					QualityProposals qproposals = new QualityProposals();
					qproposals.add(new RelationalQuality(relationalquality, relatedentity));
					qproposals.setPhrase(quality);
					Utilities.addQualityProposals(qualities, qproposals);
					//this.qualities.add(qproposals);
				}*/
			}
			if(!usedconstraint){
				if(structuresWithSameCharacters(this.chara.getParentElement().getParentElement())){// A and B: fused
					//Processes structures with same characters(RQ's) to be related => Hariharan
					Hashtable<String,ArrayList<EntityProposals>> entities = this.processStructuresWithSameCharacters(this.chara.getParentElement().getParentElement());
					addREPE(entities,relationalquality);
				}//check whether the parent structure(this.entity) is a bilateral entity
				else if((this.entity!=null)&&(checkBilateral(this.entity)==true))//bilateral structures: fused
				{
					this.primaryentities.clear();//Since among the primary entities only some are bilateral and is relevant to this character/relational quality => Hariharan
					for(Entity e:this.bilateral)
					{
						RelationalEntityStrategy re = new RelationalEntityStrategy(e);
						re.handle();
						Hashtable<String,ArrayList<EntityProposals>> entities = re.getEntities();
						addREPE(entities,relationalquality);
					}
				}
				else /* if(!structuresWithSameCharacters(this.chara.getParentElement().getParentElement()))*///if entity is null,then structure is whole_organism, it should be handled here
					//single, non-bilateral structure: fused: for example whole_organism:fused
				{
					//Handling characters that belong to a single structure => Hariharan

					Hashtable<String,ArrayList<EntityProposals>> entities = SingleStructures();
					addREPE(entities,relationalquality);
				}
				/*else
			{
				//Handle using text
			}*/
			}
			if((this.primaryentities.size()>0)&&(this.qualities.size()>0))
				donotresolve=true;
			return;
		}


		//constraints may yield entity parts such as entity locator, save those, resolve them later
		//Need to handle this differently, if quality is size.
		if (chara.getAttribute("constraintid") != null) {
			ArrayList<EntityProposals> entities = findEntityInConstraints();
			for(EntityProposals entity: entities){
				this.entityparts.add(entity);
			}
		}


		//not a relational quality, is this a simple quality or a negated quality?

		TermSearcher ts = new TermSearcher();
		ArrayList<FormalConcept> results = ts.searchTerm(quality, "quality");

		if(results!=null){ //has a strong match
			//QualityProposals qproposals = new QualityProposals();
			for(FormalConcept resultfc: results){
				QualityProposals qproposals = new QualityProposals();
				Quality result = (Quality)resultfc;
				//qualities involving length should be handled with related entity

				if((result.getLabel()!=null)&&result.getLabel().matches(".*(length|width|size|depth|broad)"))
				{
					this.resolve=true;
				}
				//the below if-loop is used to reset the string to original value of quality
				if((quality!=qualitycopy))
				{
					result.setSearchString(qualitycopy);
					result.setString(qualitycopy);
				}
				if(negated){
					/*TODO use parent classes Jim use for parent classes*/
					String [] parentinfo = ontoutil.retreiveParentInfoFromPATO(result.getId()); 
					Quality parentquality = new Quality();
					parentquality.setSearchString("");
					parentquality.setString(parentinfo[1]);
					parentquality.setLabel(parentinfo[1]);
					parentquality.setId(parentinfo[0]);		
					qproposals.add(new NegatedQuality(result, parentquality));
					qproposals.setPhrase("not "+quality);
					Utilities.addQualityProposals(qualities, qproposals);
					//this.qualities.add(qproposals);
					//return;
				}else{
					qproposals.add(result);
					qproposals.setPhrase(quality);
					Utilities.addQualityProposals(qualities, qproposals);
					//this.qualities.add(qproposals);
					//return;
				}
			}
			return;
		}else{//no match for quality, could it be something else?
			//try to match it in entity ontologies	  
			//text::Caudal fin heterocercal  (heterocercal tail is a subclass of caudal fin)
			//xml: structure: caudal fin, character:heterocercal
			//=> heterocercal tail: present

			if(this.entity!=null){
				for(int i = 0; i<entity.size(); i++){
					EntityProposals ep = entity.get(i);
					if(ep.higestScore()>=0.8f){
						for(Entity e: ep.getProposals()){
							Character2EntityStrategy ces = new Character2EntityStrategy(e, quality);
							ces.handle();
							if(ces.getEntity()!=null && ces.getQuality()!=null){
								ep = ces.getEntity();//update
								//this.qualities.add(ces.getQuality());
								Utilities.addQualityProposals(qualities, ces.getQuality());
								this.entity.set(i, ep);
								return;
							}
						}
					}
				}
			}

			/*if((this.entity!=null)&&(this.entity.higestScore()>=0.8f)){
				for(Entity e: entity.getProposals()){
					Character2EntityStrategy2 ces = new Character2EntityStrategy2(e, quality);
					ces.handle();
					if(ces.getEntity()!=null && ces.getQuality()!=null){
						this.entity = ces.getEntity();
						this.qualities.add(ces.getQuality());
						return;
					}
				}
			}*/
		}

		//TODO: could this do any good?
		//will resolve() do any good?
		//Entity result = (Entity) ts.searchTerm(quality, "entity");


		//still not successful, check other matches
		QualityProposals qproposals = null;
		ArrayList<FormalConcept> fcs = ts.getCandidateMatches(quality, "quality");
		if(fcs!=null){
			for(FormalConcept aquality: fcs){
				if((qualityclues!=null)&&(qualityclues.size()!=0)){
					for(String clue: qualityclues){
						ArrayList<FormalConcept> qclues = ts.searchTerm(clue, "quality");
						for(FormalConcept qcluefc: qclues){
							Quality qclue = (Quality)qcluefc;
							if(aquality.getLabel().compareToIgnoreCase(clue)==0 || ontoutil.isChildQuality(aquality.getClassIRI(), qclue.getClassIRI()) ){
								aquality.setConfidenceScore(1.0f); //increase confidence score
							}					
						}
					}
				}
				//no clue or clue was not helpful
				if(qproposals ==null) qproposals = new QualityProposals();
				qproposals.add((Quality)aquality);
				qproposals.setPhrase(quality);
				Utilities.addQualityProposals(qualities, qproposals);//correct grouping of proposals
				//this.qualities.add(qproposals); //incorrect, separated proposals from the same phrase 
			}
		}
		if((this.qualities.size()==0)&&(quality.equals("")==false)){
			Quality result=new Quality();
			result.setSearchString(quality);
			result.setString(quality);
			result.setConfidenceScore(0.0f); //TODO: confidence score of no-ontologized term = goodness of the phrase for ontology
			if(qproposals ==null) qproposals = new QualityProposals();
			qproposals.add(result);
			qproposals.setPhrase(quality);
			Utilities.addQualityProposals(qualities, qproposals);//correct grouping of proposals
			//this.qualities.add(qproposals); //incorrect, separated proposals from the same phrase 
		}
		return;

	}

	private String format(String quality) {
		boolean negation = false;
		if(((this.chara.getAttributeValue("name")!=null)&&(this.chara.getAttributeValue("name").matches(".*?_?(size|count|ratio)_?.*"))))
		{
			if(this.chara.getAttributeValue("name").matches(".*?_?size_?.*"))
			{
				quality="size";
			}
			else if(this.chara.getAttributeValue("name").matches(".*?_?count_?.*"))
			{
				quality="count";
			}
			else if(this.chara.getAttributeValue("name").equals(".*?_?ratio_?.*"))
			{
				quality = "ratio";
			}
		}

		if(quality.matches(".*_?(width|height|length|depth|size)_?.*"))
		{
			if(this.chara.getAttributeValue("modifier")!=null)
			{
				if(this.chara.getAttributeValue("modifier").matches(".*\\b(not|no)\\b.*"))
				{
					negation = true;
				}
				if(this.chara.getAttributeValue("modifier").matches(".*\\b(more|great|wide|broad|large)\\b.*"))
				{
					quality = (negation==false?"increased ":"decreased ")+quality;
				}
				else
				{
					quality = (negation==true?"increased ":"decreased ")+quality;
				}
			} else if(this.chara.getAttributeValue("value")!=null)
			{
				if(this.chara.getAttributeValue("value").matches(".*\\b(not|no)\\b.*"))
				{
					negation = true;
				}
				if(this.chara.getAttributeValue("value").matches(".*\\b(more|great|wide|broad|large)\\b.*"))
				{
					quality = (negation==false?"increased ":"decreased ")+quality;
				}
				else
				{
					quality = (negation==true?"increased ":"decreased ")+quality;
				}
			}
		}
		return quality;
	}
		
	/**
	 * handles case where same property of two different structures are being discussed
	 * Here, the first entity is being identified by this.entity and related entity is identified by constraintid
	 * @return always return true
	 * @throws Exception
	 */
	private boolean specialCaseDifferentStructures() throws Exception {

		String quality = this.chara.getAttributeValue("name");
		boolean negation=false;
		boolean flag=false;//used to check, if quality is modified
		if(this.chara.getAttributeValue("modifier")!=null)
		{
			if(this.chara.getAttributeValue("modifier").matches(".*(not|no).*"))
			{
				negation = true;
			}
			if(this.chara.getAttributeValue("modifier").matches(".*(more|great|wide|broad|large|long|atleast).*"))
			{
				quality = (negation==false?"increased ":"decreased ")+quality;
			}
			else
			{
				quality = (negation==true?"increased ":"decreased ")+quality;
			}
			flag=true;

		}

		TermSearcher ts = new TermSearcher();
		ArrayList<FormalConcept> primary_quality = ts.searchTerm(quality, "quality");
		if((primary_quality==null) &&(flag=true))//removes the increased or decreased and check for the plain quality
		{
			quality = quality.substring(quality.indexOf(" "));
			primary_quality = ts.searchTerm(quality, "quality");
		}
		QualityProposals qp = new QualityProposals();
		qp.setPhrase(quality);
		for(FormalConcept fc: primary_quality){
			qp.add(fc);
		}

		Element structure = (Element) XPath.selectSingleNode(root, ".//structure[@id='"+this.chara.getAttributeValue("constraintid")+"']");
		String structureid = structure.getAttributeValue("id");
		String structurename = Utilities.getStructureName(root, structureid);
		EntityParser rep = new EntityParser(chara, root, structureid, structurename, keyentities, fromcharacterstatement);
		structure.setAttribute("processed", "true");
		
		if(rep!=null && rep.getEntity()!=null){
			for(EntityProposals ep: rep.getEntity()){
				RelationalQuality rq = new RelationalQuality(qp,ep);
				qp = new QualityProposals();
				qp.add(rq);
				if(rq!=null)
				{
					Utilities.addQualityProposals(qualities, qp); //correct grouping
					//this.qualities.add(qp); //incorrect, separating proposals of the same phrase
				}
			}
		}
		return true;
	}


	private void preProcess(String quality) {
		//The below code is used to handle (height/width) kind of character name

		if((quality.contains("/")==true)&&(quality.split("/").length==2))
		{
			this.chara.setAttribute("constraint", quality.split("/")[1]);
			this.chara.setAttribute("name", quality.split("/")[0]);	
		}

		//The below code uses quality clue to process empty names and replace them with clues
		if((quality==null)||(quality.equals("")||quality.equals("size"))==true)
		{
			if(this.qualityclues!=null){
				for(String measure:this.qualityclues)
				{
					if(measure.matches("(height|length|width|depth|broad|breadth)")==true)
					{
						this.chara.setAttribute("name", measure);
						break;
					}
				}
			}
		}

		if(quality.contains("_")==true)
		{
			if(quality.contains("height"))
			{
				this.chara.setAttribute("name","height");
			}else if(quality.contains("width"))
			{
				this.chara.setAttribute("name","width");
			}else if(quality.contains("depth"))
			{
				this.chara.setAttribute("name","depth");
			}else if(quality.contains("length"))
			{
				this.chara.setAttribute("name","length");
			}
		}
	}


	private boolean specialSizeCaseSameStructure() {
		String primaryquality = cleanUp(this.chara.getAttributeValue("name"));
		String secondaryquality = cleanUp(this.chara.getAttributeValue("constraint"));
		String modifier = this.chara.getAttributeValue("modifier");	
		boolean negation=false;
		if(modifier==null)
		{
			modifier = this.chara.getAttributeValue("value");
		}
		String relation;
		Entity relatedentity=null;
		QualityProposals qp = new QualityProposals();
		qp.setPhrase(primaryquality+":"+secondaryquality);
		
		TermSearcher ts = new TermSearcher();
		ArrayList<FormalConcept> primary_quality =  ts.searchTerm(primaryquality, "quality");
		ArrayList<FormalConcept> secondary_quality = ts.searchTerm(secondaryquality, "quality");	

		if(modifier.matches(".*(not|no).*"))
		{
			negation = true;
		}
		if(modifier.matches(".*(more|great|wide|broad|large|much|long|tall).*"))
		{
			relation = negation==false?"increased_in_magnitude_relative_to":"decreased_in_magnitude_relative_to";
		}
		else
		{
			relation = negation==true?"increased_in_magnitude_relative_to":"decreased_in_magnitude_relative_to";
		}

		FormalRelation rel = Dictionary.iheresin;

		if(this.entity!=null)
		{
			for(EntityProposals ep: this.entity)
			{
				for(Entity e: ep.getProposals())
					//for(Entity e:this.entity.getProposals())
				{
					for(FormalConcept pfc: primary_quality){
						Quality primary_qualityq = (Quality)pfc;
						for(FormalConcept sfc: secondary_quality){
							Quality secondary_qualityq = (Quality)sfc;
							relatedentity = e; 
							REntity related = new REntity(rel,relatedentity);
							//CompositeQuality compquality = new CompositeQuality(primary_quality,secondary_quality,relation,related);
							CompositeQuality compquality = new CompositeQuality(primary_qualityq,secondary_qualityq,relation,related);
							qp.add(compquality);
						}
					}
				}
			}
		} else
		{
			if(this.keyentities!=null)
			{
				for(EntityProposals ep:this.keyentities)
				{
					for(Entity e: ep.getProposals())
					{
						for(FormalConcept pfc: primary_quality){
							Quality primary_qualityq = (Quality)pfc;
							for(FormalConcept sfc: secondary_quality){
								Quality secondary_qualityq = (Quality)sfc;
								relatedentity = e;
								REntity related = new REntity(rel,relatedentity);
								CompositeQuality compquality = new CompositeQuality(primary_qualityq,secondary_qualityq,relation,related);
								qp.add(compquality);
							}
						}
					}
				}
			}
		}
		Utilities.addQualityProposals(qualities, qp);
		//this.qualities.add(qp);

		return true;
	}

	private String cleanUp(String entityname) {

		if(entityname!=null)
		{
			if(entityname.matches(".*(length).*"))
			{
				entityname = "length";
			}
			else if(entityname.matches(".*(height).*"))
			{
				entityname = "height";
			}
			else if(entityname.matches(".*(depth).*"))
			{
				entityname = "depth";
			}
			else if(entityname.matches(".*(broad).*"))
			{
				entityname = "broad";
			}
			else{
				entityname = "width";
			}
		}
		return entityname;
	}

	/*
	 * Uses Elk to find out if which entities are bilateral
	 * //Checks whether the parent structure is bilateral or not
	//this.entity contains the parent entity

	 */

	private boolean checkBilateral(ArrayList<EntityProposals> eps) {
		boolean bilateralcheck = false;
		for(EntityProposals ep: eps){
			EntityProposals epclone = ep.clone();//cloning to avoid original entity proposals to be changed
			for(Entity e:epclone.getProposals())
			{
				if(e.getId()!=null)
				{
					if(e instanceof SimpleEntity)
					{
						if(XML2EQ.elk.lateralsidescache.get(e.getPrimaryEntityLabel())!=null)
						{				
							this.bilateral.add(e);
							bilateralcheck=true;
						}
					}
					else
					{
						for(Entity e1:((CompositeEntity) e).getEntities())
						{
							if(XML2EQ.elk.lateralsidescache.get(e1.getPrimaryEntityLabel())!=null)
							{
								this.bilateral.add(e);
								bilateralcheck=true;
								break;
							}
						}
					}
				}
			}
		}
		return bilateralcheck;
	}

	/*
	 * Processes multiple structures which has same characters
	 */

	private boolean structuresWithSameCharacters(Element statement) {

		try {
			List<Element> characters;
			characters = pathCharacterUnderStucture.selectNodes(statement);
			int count=0;
			//Checks for same characters present under multiple structures
			Iterator<Element> itr = characters.listIterator();
			while(itr.hasNext())
			{
				Element character = itr.next();
				if((character.getAttribute("name").getValue().equals(this.chara.getAttribute("name").getValue()))&&(character.getAttribute("value").getValue().equals(this.chara.getAttribute("value").getValue())))
					count++;
			}
			if(count>1)
				return true;
			else
				return false;

		} catch (JDOMException e) {
			LOGGER.error("", e);
		}
		return false;
	}


	/**
	 * Adds Related entities and Primary entities to existing identified entities. Also remove duplicates in the entities
	 * @param entities
	 * @param relationalquality
	 */
	private void addREPE(Hashtable<String, ArrayList<EntityProposals>> entities, QualityProposals relationalquality) {

		ArrayList<EntityProposals> primaryentities = entities.get("Primary Entity");
		ArrayList<EntityProposals> relatedentities = entities.get("Related Entities");

		if((relatedentities!=null)&&relatedentities.size()>0)
		{
			for(EntityProposals relatedentity: relatedentities){
				QualityProposals qproposals = new QualityProposals();
				qproposals.add(new RelationalQuality(relationalquality, relatedentity));
				//this.qualities.add(qproposals);
				Utilities.addQualityProposals(qualities, qproposals);
			}
			if(primaryentities.size()>0)
			{
				ListIterator<EntityProposals> itr1 = primaryentities.listIterator();
				//to remove duplicate entities
				while(itr1.hasNext())
				{
					boolean duplicate = false;
					EntityProposals ep1 = (EntityProposals) itr1.next();
					for(Entity en1: ep1.getProposals())
					{
						ListIterator<EntityProposals> itr2 = this.primaryentities.listIterator();
						while(itr2.hasNext())
						{
							EntityProposals ep2 = (EntityProposals) itr2.next();							
							for(Entity en2:ep2.getProposals()){//add more conditions to analyze and handle bilateral entities
								if(en2.content().compareTo(en1.content())==0)
									duplicate = true;			
							}
							
						}
					}
					if(!duplicate) Utilities.addEntityProposals(this.primaryentities, ep1);
				}
				/*while(itr1.hasNext())
				{
					EntityProposals ep1 = (EntityProposals) itr1.next();
					for(Entity en1: ep1.getProposals())
					{
						ListIterator<EntityProposals> itr2 = this.primaryentities.listIterator();
						while(itr2.hasNext())
						{
							EntityProposals ep2 = (EntityProposals) itr2.next();
							for(Entity en2:ep2.getProposals())//add more conditions to analyze and handle bilateral entities
								if(en2.getPrimaryEntityLabel().equals(en1.getPrimaryEntityLabel()))
									itr1.remove();	//caused IllegalState exception when there are duplicates							
						}
					}
				}
				this.primaryentities.addAll(primaryentities);
				*/
			}
		}

	}

	/*
	 * It identifies structures which have the same characters(Relational Quality) 
	 * and it returns all the structures which has the same characters as related entities
	 * if a structure is a whole_organism, it calls bilateralstructures to handle the scenario.
	 * 
	 * 
	 */			@SuppressWarnings("unchecked")
	 private Hashtable<String, ArrayList<EntityProposals>> processStructuresWithSameCharacters(Element statement) {
		 //TODO: Handle scenario where all the characters have whole_organism as its parent structure and Keyentities is null
		 try {
			 List<Element> characters = pathCharacterUnderStucture.selectNodes(statement);
			 Hashtable<String,ArrayList<EntityProposals>> entities = new Hashtable<String,ArrayList<EntityProposals>>();
			 ArrayList<EntityProposals> primaryentities = new ArrayList<EntityProposals>();
			 ArrayList<EntityProposals> relatedentities = new ArrayList<EntityProposals>();

			 //Remove all other characters that are not same as the character under consideration 
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
				 if(chara.getParentElement().getAttributeValue("name").compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))!=0)
				 {
					 primaryentities.addAll(this.entity);//Since this is the identified entity of this character
				 }

			 }
			 //finding the related entities using structures with same character name and value
			 for(int i=1;i<characters.size();i++)
			 {
				 Element character = characters.get(i);
				 if((character.getAttribute("name").getValue().equals(this.chara.getAttribute("name").getValue()))&&(character.getAttribute("value").getValue().equals(this.chara.getAttribute("value").getValue())))
				 {
					 //read the structure(other than whole_organism) of this character, find the entity,create entity proposals
					 Element ParentStructure = character.getParentElement();
					 if(!ParentStructure.getAttributeValue("name").equals(ApplicationUtilities.getProperty("unknown.structure.name")))
					 {
						 ArrayList<EntityProposals> entity  = new EntitySearcherOriginal().searchEntity(root, ParentStructure.getAttributeValue("id"), 
								 ParentStructure.getAttributeValue("name"), "", ParentStructure.getAttributeValue("name"),"");				
						 if(entity!=null)  relatedentities.addAll(entity); 
						 character.detach();
					 }

				 }
			 }
			 if(relatedentities.size()>0)
			 {
				 entities.put("Related Entities", relatedentities);
				 entities.put("Primary Entity", primaryentities);
			 }

			 return entities;

		 } catch (JDOMException e) {
			 LOGGER.error("", e);
		 }
		 return null;
	 }

	 private Hashtable<String, ArrayList<EntityProposals>> SingleStructures() {

		 Element ParentStructure = this.chara.getParentElement();
		 ArrayList<EntityProposals> primaryentities = new ArrayList<EntityProposals>();
		 ArrayList<EntityProposals> relatedentities = new ArrayList<EntityProposals>();
		 Hashtable<String,ArrayList<EntityProposals>> entities = new Hashtable<String,ArrayList<EntityProposals>>();

		 if(ParentStructure.getAttributeValue("name").compareTo(ApplicationUtilities.getProperty("unknown.structure.name"))==0)
		 {
			 if(this.keyentities.size()>1)//If keyentities.size> 1, first entity is a primary entity and the rest are related entities
			 {
				 for(int i=1;i<this.keyentities.size();i++)
					 relatedentities.add(this.keyentities.get(i));

				 primaryentities.add(this.keyentities.get(0));

			 }
			 //TODO: this.checkBilateral = true, but RelationalEntityStrategy return no results -- the formal check only the primary entity in a composite, the latter check the REntities as well
			 else if((this.keyentities.size()==1)&&(this.checkBilateral(this.keyentities)))//If keyentities.size==1, the it can be a bilateral entity
			 {
				 //call bilateral strategy on the single key entity
				 for(Entity e:this.bilateral)
				 {
					 RelationalEntityStrategy re = new RelationalEntityStrategy(e);
					 re.handle();
					 Hashtable<String,ArrayList<EntityProposals>> entities1 = re.getEntities();
					 relatedentities.addAll(entities1.get("Related Entities"));
					 primaryentities.addAll(entities1.get("Primary Entity"));
				 }
			 }
			 else if(this.keyentities.size()==1)
			 {
				 //use the the keyentities as both primary and related entities
				 relatedentities.addAll((Collection<? extends EntityProposals>) keyentities.clone());
				 primaryentities.addAll((Collection<? extends EntityProposals>) keyentities.clone());
			 }

			 entities.put("Related Entities", relatedentities);
			 entities.put("Primary Entity", primaryentities);

		 }

		 return entities;

	 }


	 private ArrayList<EntityProposals> findEntityInConstraints() {
		 ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
		 if (chara.getAttribute("constraintid") != null) {
			 String[] conids = chara.getAttributeValue("constraintid").split("\\s+");
			 try{
				 for(String conid: conids){
					 String relatedentity = Utilities.getStructureName(root, conid);
					 //parents separated by comma (,).
					 String relatedentityparents = Utilities.getNamesOnPartOfChain(root, chara.getAttributeValue("constraintid"));
					 ArrayList<EntityProposals> result = new EntitySearcherOriginal().searchEntity(root, conid, relatedentity, relatedentityparents, relatedentity+"+"+relatedentityparents,"part_of");	
					 if(result!=null) entities.addAll(result);
				 }
				 return entities;
			 }catch(Exception e){
				 LOGGER.error("", e);
			 }
		 }
		 return null;
	 }

	 /**
	  * resolve for entities from entity and entity parts obtained from constraints
	  * also when neither entity and qualities scores are strong, the keyentities scores are not strong or not ontologized
	  * should try to resolve them here? or in the end?
	  */
	 public void resolve(){

		 //TODO
		 //this was how s2q was used before in SSP.
		 //Structure2Quality rq2 = new Structure2Quality(root,
		 //		structname, structid, this.keyentities);
		 //rq2.handle();
		 //if(rq2.qualities.size()>0){
		 //	entity = rq2.primaryentities;
		 //  qualities = rq2.qualities;
		 //} else {

		 //TODO add some more conditions for resolving

		 //the below condition handles situation where a structure is identified to be a quality.
		 if(this.entity==null)
		 {

			 if(tobesolvedentity==null)
			 {					
				 if(this.qualities.size()>0)
				 {
					 resolveForWholeOrganism();// Identify Entities/qualities/REntities for length or width or size
					 this.donotresolve=true;
				 }
			 }else if(tobesolvedentity.s2q!=null)
			 {
				 if(tobesolvedentity.s2q.qualities!=null)
				 {
					 this.qualities.clear();
					 this.qualities.addAll(tobesolvedentity.s2q.qualities);
					 tobesolvedentity.s2q.detach_character();
					 tobesolvedentity.s2q.cleanHandledStructures();
					 if(tobesolvedentity.s2q.primaryentities.size()>0)//relational quality might contain primary entities
					 {
						 this.primaryentities.clear();
						 this.primaryentities.addAll(primaryentities);
					 }
					 else
					 {
						 if(keyentities!=null)
						 {
							 this.primaryentities.clear();
							 this.primaryentities.addAll(keyentities);
						 }
					 }
				 }
			 }

		 }
		 else
		 {
			 this.donotresolve=true;

		 }
		 //Resolve for quality when it is "length"
		 if(this.entityparts.size()>0)
		 {
			 resolveIntoRelationalQuality();
			 this.donotresolve=true;
		 }
		 //need to resolve on cases where both entity!=null and S2Q!=null
	 }

	 /*
	  * When the parent structure is whole organism then, the first key entity is considered primary and the rest as related entity
	  */

	 private void resolveForWholeOrganism() {

		 ArrayList<QualityProposals> rqp = new ArrayList<QualityProposals>();
		 if((this.keyentities!=null)&&(this.keyentities.size()>0))
		 {
			 if(this.entity==null)
			 {
				 //this.entity = this.keyentities.get(0);
				 this.entity = this.keyentities;
				 this.primaryentities.addAll(this.entity);
			 }

			 if(this.qualities.size()>0)
			 {
				 for(QualityProposals qp:this.qualities)
				 {

					 for(Quality q:qp.getProposals())//Reading each of the qualities
					 {
						 QualityProposals newqp = new QualityProposals();

						 if((q.getLabel()!=null)&&(q.getLabel().matches(".*(length|width|size|height|depth).*")))//If any of the quality label matches to "length", then the qp itself belongs to size
						 {		
							 for(int i=1;i<this.keyentities.size();i++)
							 {
								 RelationalQuality rq = new RelationalQuality(qp,this.keyentities.get(i));
								 newqp.add(rq);							
							 }
							 if(newqp.getProposals().size()>0)
							 {
								 rqp.add(newqp);
							 }
							 break;
						 }
					 }
				 }
			 }
			 if(rqp.size()>0)
			 {
				 this.qualities.clear();
				 this.qualities.addAll(rqp);
				 this.donotresolve = true;
			 }
		 }				
	 }

	 /**
	  * This function handles the special case when increased/decreased length is a quality
	  * The entity in parts will become the Related entity instead of Entity Locator
	  */
	 public void resolveIntoRelationalQuality() {

		 ArrayList<QualityProposals> rqp = new ArrayList<QualityProposals>();

		 if(this.qualities.size()>0)
		 {
			 for(QualityProposals qp:this.qualities)
			 {

				 for(Quality q:qp.getProposals())//Reading each of the qualities
				 {
					 QualityProposals newqp = new QualityProposals();

					 if((q.getLabel()!=null)&&(q.getLabel().matches(".*(length|width|size|height|depth|broad)")))//If any of the quality label matches to "length", then the qp itself belongs to size
					 {		
						 for(EntityProposals ep : this.entityparts)
						 {
							 RelationalQuality rq = new RelationalQuality(qp,ep);//this forms relationalquality for each of QP and RE and used for new QP
							 newqp.add(rq);
						 }
						 rqp.add(newqp); 
						 break;
					 }
				 }

			 }
			 if(rqp.size()>0)
			 {
				 this.entityparts.clear();
				 this.qualities.clear();
				 this.qualities.addAll(rqp);
			 }
		 }
	 }

	 public ArrayList<QualityProposals> getQualities(){
		 return this.qualities;
	 }

	 public ArrayList<EntityProposals> getEntity(){
		 return this.entity;
	 }





	 public ArrayList<EntityProposals> getPrimaryentities() {
		 return primaryentities;
	 }
	 /**
	  * @param args
	  */
	 public static void main(String[] args) {
		 // TODO Auto-generated method stub

	 }

}
