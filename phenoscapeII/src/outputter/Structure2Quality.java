package outputter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

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

	Element root;
	String relation;
	String structname;
	String structid;
	boolean negation; // if true, negate the relation string
	boolean fromcharacterstatement;
	ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>(); //typically has 1 element, declared to be an arraylist for some rare cases (like 3 entities contact one another)
	ArrayList<EntityProposals> primaryentities = new ArrayList<EntityProposals>();
	private TermOutputerUtilities ontoutil;
	static XPath pathCharacterUnderStucture;
	XPath pathrelationfromStructure;

	ArrayList<EntityProposals> keyentities;
	HashSet<String> identifiedqualities;
	
	static{
		try{
			pathCharacterUnderStucture = XPath.newInstance(".//character");
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public Structure2Quality(Element root,String structurename, String structureid, ArrayList<EntityProposals> keyentities) {
		this.root = root;
		this.structname = structurename;
		this.structid = structureid;
		this.keyentities = keyentities;
		identifiedqualities = new HashSet<String>(); //list of unique xml id
	}

	public void handle() {
		try {

			parseforQuality(this.structname, this.structid); //to see if the structure is a quality (relational or other quality)
			//detach all identifiedqualities
			for(String structid: identifiedqualities){
				Element structure = (Element) XPath.selectSingleNode(root, ".//structure[@id='"+structid+"']");
				structure.detach(); //identifiedqualities are used to check the relations this structure is involved in, 
									//and the relations are needed for other purpose, 
									//so don't detach relation here. 
			}
		} catch (JDOMException e) {
			e.printStackTrace();
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
		characters = pathCharacterUnderStucture.selectNodes(Structures);
		//characters are checked to find out if the quality is negated
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
		QualityProposals relationalquality = PermittedRelations.matchInPermittedRelation(quality, false);
		// TODO:// deal// with// negated// quality// later
		if (relationalquality != null)
		{
			XPath pathrelationfromStructure = XPath.newInstance(".//relation[@from='" + qualityid + "']");
			List<Element> relations= pathrelationfromStructure.selectNodes(this.root);
			XPath structurewithstructid1;
			EntityProposals Relatedentity;

			//If two entities are there, then the first one is the primary entity and the second one is the related entity
			if((relations!=null)&&(relations.size()>0))
			{
				//Check whether this tostructure is a quality, if not create a related Entity
				for(Element relation:relations)
				{
					String tostructid = relation.getAttributeValue("to");
					structurewithstructid1 = XPath.newInstance(".//structure[@id='"+ tostructid.trim() + "']");
					Element tostruct = (Element) structurewithstructid1.selectSingleNode(this.root);
					String tostructname = tostruct.getAttributeValue("name");
					Relatedentity = new EntitySearcherOriginal().searchEntity(root, tostructname, tostructname, "", "","");	
					if(Relatedentity!=null)
					{
						RelationalQuality rq = new RelationalQuality(relationalquality, Relatedentity);
						QualityProposals qproposals = new QualityProposals();
						qproposals.add(rq);
						this.qualities.add(qproposals);
						this.identifiedqualities.add(qualityid);
						if(chara_detach!=null)
							chara_detach.detach();
					}
					//What are the primaryentities here?
				}
				return;
			}else if((this.keyentities!=null) && (this.keyentities.size()==2))
			{
				for(int i=1;i<this.keyentities.size();i++)
				{
					RelationalQuality rq = new RelationalQuality(relationalquality, this.keyentities.get(i));
					QualityProposals qproposals = new QualityProposals();
					qproposals.add(rq);
					this.qualities.add(qproposals);
					this.identifiedqualities.add(qualityid);
					if(chara_detach!=null)
						chara_detach.detach();
					return;
				}
				this.primaryentities.add(this.keyentities.get(0));
			}
			else if((this.keyentities!=null) && (this.keyentities.size()==1)) //bilateral structures?
			{ // TODO how to find related entities from a list of entities >2 or <2
				
				RelationalEntityStrategy1 re = new RelationalEntityStrategy1(this.root,Structures,this.keyentities);
				re.handle();
				Hashtable<String,ArrayList<EntityProposals>> entities = re.getEntities();
				//addREPE(entities,relationalquality);
				ArrayList<EntityProposals> relatedentities = entities.get("Related Entities");
				for(int i=0;i<relatedentities.size();i++)
				{
					RelationalQuality rq = new RelationalQuality(relationalquality, relatedentities.get(i));
					QualityProposals qproposals = new QualityProposals();
					qproposals.add(rq);
					this.qualities.add(qproposals);
					this.identifiedqualities.add(qualityid);
					if(chara_detach!=null)
						chara_detach.detach();
					return;
				}
				
				this.primaryentities.addAll(entities.get("Primary Entity"));
				return;
			}
			else
			{
				//TODO if key entities is also null. look into the text for clue? or preprocess later?
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
			Checkforsimplequality(chara,quality,qualityid,negated,chara_detach);
		}
		else
			Checkforsimplequality(null,quality,qualityid,negated,chara_detach);

		return;
	}
	
	//a separate function is created to handle structures(quality) with characters and without characters

	private void Checkforsimplequality(Element chara, String quality, String qualityid, boolean negated, Element chara_detach) {
		
		Quality result;
		if(chara!=null)
		quality=chara.getAttributeValue("value")+" "+quality;
		quality=quality.trim();
		TermSearcher ts = new TermSearcher();
		for(;;)
		{
		result = (Quality) ts.searchTerm(quality, "quality");
		if((result!=null)||quality.length()==0)
		break;
		quality =(quality.indexOf(" ")!=-1)?quality.substring(quality.indexOf(" ")).trim():"";
		}
		if (result != null) {
			if (negated) {
				String[] parentinfo = ontoutil.retreiveParentInfoFromPATO(result.getId());
				Quality parentquality = new Quality();
				parentquality.setString(parentinfo[1]);
				parentquality.setLabel(parentinfo[1]);
				parentquality.setId(parentinfo[0]);
				QualityProposals qproposals = new QualityProposals();
				qproposals.add(new NegatedQuality(result, parentquality));
				this.qualities.add(qproposals);
				this.identifiedqualities.add(qualityid);
				//to remove negated character and prevent from processed in the future
				if(chara_detach!=null)
					chara_detach.detach();
			} else {
				QualityProposals qproposals = new QualityProposals();
				qproposals.add(result);
				this.qualities.add(qproposals);
				this.identifiedqualities.add(qualityid);
			}
			if(chara!=null)
				chara.detach();			
		}
		
	}
}
