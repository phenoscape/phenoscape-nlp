package outputter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;


/**
 * This Strategy checks whether one <structure> is a quality (relational or not).
 * If true will create qualities accordingly.
 * This class also adjust underlying xml file by detaching the structure. 
 *  
 */

public class Structure2Quality implements AnnotationStrategy{

	private static final Logger LOGGER = Logger.getLogger(Structure2Quality.class);   
	Element root;
	String  relation;
	String structname;
	String structid;
	boolean negation; // if true, negate the relation string
	boolean fromcharacterstatement;
	ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>(); //typically has 1 element, declared to be an arraylist for some rare cases (like 3 entities contact one another)
	ArrayList<EntityProposals> primaryentities = new ArrayList<EntityProposals>(); //if find a relational quality, the primaryentities are used at the left side of the relation.
	ArrayList<EntityProposals> spatialmodifier = null;
	private TermOutputerUtilities ontoutil;
	//static XPath pathCharacterUnderStucture;
	XPath pathrelationfromStructure;
	ArrayList<Entity> bilateral = new ArrayList<Entity>();
	ArrayList<EntityProposals> keyentities;
	HashSet<String> identifiedqualities;//list of unique xml id of the structures found to be quality
	List<Element> detach_characters = new ArrayList<Element>();


	/*static{
		try{
			pathCharacterUnderStucture = XPath.newInstance(".//character");
		}catch (Exception e){
			LOGGER.error("", e);
		}
	}*/

	public Structure2Quality(Element root,String structurename, String structureid, ArrayList<EntityProposals> keyentities) {
		this.root = root;
		this.structname = structurename;
		this.structid = structureid;
		this.keyentities = keyentities;
		identifiedqualities = new HashSet<String>(); //list of unique xml id of the structures found to be quality
	}

	public void handle() {
		try {
			if(!isPossibleQuality()) return;
			//whether to include spatial constraint in the search for quality
			//first, try include
			//if failed, try exclude

			QualityProposals relationalquality = PermittedRelations.matchInPermittedRelation(structname, false,1);
			if(relationalquality!=null) parseforQuality(this.structname, this.structid);
			else{
				ArrayList<Quality> quality= searchForSimpleQuality(this.structname);
				if(quality!=null) parseforQuality(this.structname, this.structid);
				else{
					if(removeSpatialConstraint(this.structid)){
						parseforQuality(this.structname, this.structid); //to see if the structure is a quality (relational or other quality)
					}
				}
			}
			//if(removeSpatialConstraint(this.structid)){
			//	parseforQuality(this.structname, this.structid); //to see if the structure is a quality (relational or other quality)
			//}
		} catch (JDOMException e) {
			LOGGER.error("", e);
		}
	}

	/**
	 * remove the constraint part of the structure name that is a spatial term
	 * @param structid
	 * @return boolean success or not
	 * @throws JDOMException
	 */
	private boolean removeSpatialConstraint(String structid) throws JDOMException {

		Element structure = (Element) XPath.selectSingleNode(root, ".//structure[@id='"+structid+"']");
		if((structure.getAttributeValue("constraint")!=null)&&(structure.getAttributeValue("constraint").matches(Dictionary.spatialtermptn)))
		{
			this.structname = structure.getAttributeValue("name");
			LOGGER.debug("Structure2Quality calls EntitySearcherOriginal to search structure constraint '"+structure.getAttributeValue("constraint")+"'");
			this.spatialmodifier =  new EntitySearcherOriginal().searchEntity(root, "", structure.getAttributeValue("constraint"), "", "","");
			return true;
		}
		return false;
	}

	private boolean isPossibleQuality() throws JDOMException {
		Element structure = (Element) XPath.selectSingleNode(root, ".//structure[@id='"+this.structid+"']");
		//if structure has constraint but the constraint is not a spatial modifier, then this can not be a quality. for example: parasymphysial plate (should not match plate-like)
		if((structure.getAttributeValue("constraint")!=null && !structure.getAttributeValue("constraint").matches(Dictionary.spatialtermptn))){ 
			return false;
		}
		//if the full structurename (constraint+name) matches spatial term pattern (e.g.'dorsal surface'), then this can not be a quality
		if(this.structname.matches("^("+Dictionary.spatialtermptn+")+\\s+("+Dictionary.allSpatialHeadNouns()+")+")){
			return false;
		}
		return true;
	}

	/**
	 * call this (to detach all identifiedqualities) only when the quality/structure conflict is resolved: 
	 */
	public void cleanHandledStructures(){

		try{
			for(String structid: identifiedqualities){
				Element structure = (Element) XPath.selectSingleNode(root, ".//structure[@id='"+structid+"']");
				if(structure!=null)
					structure.detach(); //identifiedqualities are used to check the relations this structure is involved in, 
				//and the relations are needed for other purpose, 
				//so don't detach relation here. 
			} 
		}catch (JDOMException e) {
			LOGGER.error("", e);
		}
	}

	@SuppressWarnings("unchecked")
	public void parseforQuality(String quality, String qualityid)
			throws JDOMException {
		// characters => quality
		// get quality candidate
		// handle this later => use below code to look and use all the
		// characters under a structure
		List<Element> characters = new ArrayList<Element>();
		Element Structures,chara_detach=null;
		boolean negated = false;
		String qualitycopy=quality;
		XPath structurewithstructid = XPath.newInstance(".//structure[@id='"+ qualityid + "']");
		Structures = (Element) structurewithstructid.selectSingleNode(this.root);
		//characters = pathCharacterUnderStucture.selectNodes(Structures);
		characters = Structures.getChildren("character");
		//characters are checked to find out if the quality should be negated
		for (Element chara : characters) {
			String modifier = chara.getAttribute("modifier")!=null? chara.getAttributeValue("modifier"): "";
			String value = chara.getAttribute("value")!=null? chara.getAttributeValue("value"):"";
			if ((modifier.startsWith("not") && !value.matches(Dictionary.negation)) || (!modifier.startsWith("not") && value.matches(Dictionary.negation))) {
				negated = true;
				chara_detach =chara;
				break;

			}

		}
		// is the candidate a relational quality?
		QualityProposals relationalquality = PermittedRelations.matchInPermittedRelation(quality, false,1);
		// TODO:// deal// with// negated// quality// later
		if (relationalquality != null)
		{
			XPath pathrelationfromStructure = XPath.newInstance(".//relation[@from='" + qualityid + "']");
			List<Element> relations= pathrelationfromStructure.selectNodes(this.root);
			XPath structurewithstructid1;
			ArrayList<EntityProposals> Relatedentity;

			//If two entities are there, then the first one is the primary entity and the second one is the related entity
			if((relations!=null)&&(relations.size()>0))
			{
				//Check whether this tostructure is a quality, if not create a related Entity
				for(Element relation:relations)
				{
					String tostructid = relation.getAttributeValue("to").trim();
					if(tostructid.indexOf(" ")>0){ //>1 id: o510 o511, in case of relation="between"
						QualityProposals qproposals = new QualityProposals();
						String[] toids = tostructid.split("\\s+");		
						int count = 0;
						for(String toid: toids){
							structurewithstructid1 = XPath.newInstance(".//structure[@id='"+ toid.trim() + "']");
							Element tostruct = (Element) structurewithstructid1.selectSingleNode(this.root);
							String tostructname = Utilities.getStructureName(root, toid);
							Relatedentity = new EntitySearcherOriginal().searchEntity(root, tostructname, tostructname, "", "","");	
							if(Relatedentity!=null){
								if(count==0){
									this.primaryentities.addAll(Relatedentity); //set the first be the primary
									if(this.keyentities==null){
										this.keyentities = new ArrayList<EntityProposals>(); 
										this.keyentities.addAll(Relatedentity);
									}
								}else{
									//set others as related entity	
									for(EntityProposals relatedentity: Relatedentity){
										RelationalQuality rq = new RelationalQuality(relationalquality, relatedentity);
										qproposals.add(rq);  //need fix: these rqs don't belong to the same QualityProposal!, should be A in_contact_with B and C and D
										qproposals.setPhrase(quality);
										//this.qualities.add(qproposals);
										Utilities.addQualityProposals(qualities, qproposals);
										this.identifiedqualities.add(qualityid);	
									}
								}
							}
							count++;
						}
						if(chara_detach!=null)
							chara_detach.detach();
					}else{//one id
						structurewithstructid1 = XPath.newInstance(".//structure[@id='"+ tostructid.trim() + "']");
						Element tostruct = (Element) structurewithstructid1.selectSingleNode(this.root);
						String tostructname = Utilities.getStructureName(root, tostructid);
						Relatedentity = new EntitySearcherOriginal().searchEntity(root, tostructname, tostructname, "", "","");	
						if(Relatedentity!=null)
						{
							for(EntityProposals relatedentity: Relatedentity){
								RelationalQuality rq = new RelationalQuality(relationalquality, relatedentity);
								QualityProposals qproposals = new QualityProposals();
								qproposals.add(rq);
								qproposals.setPhrase(quality);
								//this.qualities.add(qproposals);
								Utilities.addQualityProposals(qualities, qproposals);
								this.identifiedqualities.add(qualityid);
							}
							if(chara_detach!=null)
								chara_detach.detach();
						}
						//What are the primaryentities here?
					}
				}
				return;
			}else if((this.keyentities!=null) && (this.keyentities.size()>=2))
			{
				for(int i=1;i<this.keyentities.size();i++)
				{
					RelationalQuality rq = new RelationalQuality(relationalquality, this.keyentities.get(i));
					QualityProposals qproposals = new QualityProposals();
					qproposals.add(rq); 
					qproposals.setPhrase(quality);
					//this.qualities.add(qproposals);
					Utilities.addQualityProposals(qualities, qproposals);
					this.identifiedqualities.add(qualityid);
				}
				this.primaryentities.add(this.keyentities.get(0)); //if keyentities = [A, B, C, D] then primaryentity=A;
																   //qualities=[relationalquality B; relationalquality C;relationalquality D]
				if(chara_detach!=null)
					chara_detach.detach();
				return;
			}
			else if((this.keyentities!=null) && (this.keyentities.size()==1) && (checkbilateral()==true)) //bilateral structures?
			{ 
				//Here check bilateral will return us a list of entities which are bilateral.We pass it to relationalentitystrategy1
				//to find related entities and primary entities
				for(Entity e:this.bilateral)
				{
					RelationalEntityStrategy1 re = new RelationalEntityStrategy1(e);
					re.handle();
					Hashtable<String,ArrayList<EntityProposals>> entities = re.getEntities();
					ArrayList<EntityProposals> relatedentities = entities.get("Related Entities");

					if((relatedentities!=null)&&(relatedentities.size()>0))//Single key entity might be a bilateral
					{
						for(int i=0;i<relatedentities.size();i++)
						{
							RelationalQuality rq = new RelationalQuality(relationalquality, relatedentities.get(i));
							QualityProposals qproposals = new QualityProposals();
							qproposals.add(rq);
							qproposals.setPhrase(quality);
							//this.qualities.add(qproposals);
							Utilities.addQualityProposals(qualities, qproposals);
							this.identifiedqualities.add(qualityid);
						}
						this.primaryentities.addAll(entities.get("Primary Entity"));
						if(chara_detach!=null)
							chara_detach.detach();
					}
				}
				return;
			}
			else //pack and return an incomplete relational quality (EntityProposals = null)
			{
				RelationalQuality rq = new RelationalQuality(relationalquality, new EntityProposals());
				QualityProposals qproposals = new QualityProposals();
				qproposals.add(rq);
				qproposals.setPhrase(quality);
				//this.qualities.add(qproposals);
				Utilities.addQualityProposals(qualities, qproposals);
				this.identifiedqualities.add(qualityid);			
			}
			return;
		}


		// may need to consider constraints, which may provide a related entity
		// not a relational quality, is this a simple quality or a negated
		
		//simple quality == quality character value + quality
		// quality?
		if((characters!=null)&&(characters.size()>0))
		{
			for(Element chara:characters)
				checkForSimpleQuality(chara,quality,qualityid,negated,chara_detach);
		}
		else
			checkForSimpleQuality(null,quality,qualityid,negated,chara_detach);
		//detach_character(); need to be invoked only when S2Q is being accpeted by the calling function
		return;
	}

	public void detach_character() {

		for(Element chara:this.detach_characters)
			chara.detach();

	}

	private boolean checkbilateral() {

		boolean bilateralcheck = false;
		for(Entity e:this.keyentities.get(0).getProposals())
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
						if(e1.getPrimaryEntityLabel()!=null)//Entities which don't have a perfect match in ontology need not be checked for bilateral
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


	//a separate function is created to handle structures(quality) with characters and without characters

	@SuppressWarnings("unchecked")
	private void checkForSimpleQuality(Element chara, String quality, String qualityid, boolean negated, Element chara_detach) {
		if(chara!=null){
			quality=chara.getAttributeValue("value")+" "+quality; //large + 'expansion'
		}
		quality=quality.trim();

		ArrayList<Quality> results = searchForSimpleQuality(quality);
		if(chara!=null){
			//results now hold different qualities, not proposals for the same quality
			ArrayList<FormalConcept> results2 =new TermSearcher().searchTerm(chara.getAttributeValue("value"), "quality");
			if(results2!=null) results.addAll((Collection<? extends Quality>) results2);
		}
		if (results != null) {
			if (negated) {
				for(Quality result: results){ 
					String[] parentinfo = TermOutputerUtilities.retreiveParentInfoFromPATO(result.getId());
					Quality parentquality = new Quality();
					parentquality.setString(parentinfo[1]);
					parentquality.setLabel(parentinfo[1]);
					parentquality.setId(parentinfo[0]);
					QualityProposals qproposals = new QualityProposals();//results hold different qualities, not proposals for the same quality
					NegatedQuality nquality = new NegatedQuality(result, parentquality);
					qproposals.add(nquality);	
					qproposals.setPhrase(nquality.getString());
					//this.qualities.add(qproposals);
					Utilities.addQualityProposals(qualities, qproposals);
				}
				this.identifiedqualities.add(qualityid);
				//to remove negated character and prevent from processed in the future
				this.detach_characters.add(chara_detach);
			} else {
				for(Quality result: results){
					QualityProposals qproposals = new QualityProposals();
					qproposals.setPhrase(result.getString());
					qproposals.add(result);
					Utilities.addQualityProposals(qualities, qproposals);
				}
				//this.qualities.add(qproposals);
				this.identifiedqualities.add(qualityid);
			}
			if(chara!=null)	
				this.detach_characters.add(chara);
		}
	}

	/**
	 * 
	 * @param quality: modifier/character + structure/quality, for example, elongate rod
	 * @return
	 */
	private ArrayList<Quality> searchForSimpleQuality(String quality) {
		ArrayList<FormalConcept> result;
		TermSearcher ts = new TermSearcher();
		for(;;)
		{
			result =  ts.searchTerm(quality, "quality");
			if((result!=null)||quality.length()==0)
				break;
			quality =(quality.indexOf(" ")!=-1)?quality.substring(quality.indexOf(" ")).trim():"";
		}
		
		if(result!=null){
			ArrayList<Quality> qualities = new ArrayList<Quality>();
			for(FormalConcept fc: result) qualities.add((Quality)fc);
			return qualities;
		}else{
			return null;
		}
	}
}
