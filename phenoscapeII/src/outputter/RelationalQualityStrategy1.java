package outputter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;


/**
 * RelationalQualityStrategy is used to check whether an entity is a quality (relaitonal or not) and
 * if true will create qualities accordingly.
 */

public class RelationalQualityStrategy1 {

	Element root;
	String relation;
	String tostructname;
	String tostructid;
	String fromstructname;
	String fromstructid;
	boolean negation; // if true, negate the relation string
	boolean fromcharacterstatement;
	ArrayList<QualityProposals> qualities = new ArrayList<QualityProposals>();
	private TermOutputerUtilities ontoutil;
	XPath pathCharacterUnderStucture;
	ArrayList<EntityProposals> keyentities;
	ArrayList<String> identifiedqualities;

	public RelationalQualityStrategy1(Element root,String toname, String toid, String fromname, String fromid, ArrayList<EntityProposals> keyentities) throws JDOMException {
		this.root = root;
		this.tostructname = toname;
		this.tostructid = toid;
		this.fromstructname = fromname;
		this.fromstructid = fromid;
		pathCharacterUnderStucture = XPath.newInstance(".//character");
		this.keyentities = keyentities;
		identifiedqualities = new ArrayList<String>();
		
	}

	public void handle() {
		try {
			parseforQuality(this.tostructname, this.tostructid); //to see if the structure is a quality (relational or other quality)
			parseforQuality(this.fromstructname, this.fromstructid);

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
			if ((modifier.startsWith("not") && !value.equals("absent")) || (!modifier.startsWith("not") && value.equals("absent"))) {
				negated = true;
			//	quality = quality.substring(quality.indexOf(" ") + 1).trim();
				chara_detach =chara;
				break;
				
			}

		}
		// is the candidate a relational quality?
		QualityProposals relationalquality = PermittedRelations.matchInPermittedRelation(quality, false);
		// TODO:// deal// with// negated// quality// later
		if (relationalquality != null)
		{
			//If two entities are there, then the first one is the primary entity and the second one is the related entity
			if((this.keyentities!=null) && (this.keyentities.size()==2))
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
			}
			else
			{ // TODO how to find related entities from a list of entities >2
				return;
			}
			return;
		}
		
        
		// may need to consider constraints, which may provide a related entity


		// not a relational quality, is this a simple quality or a negated
		//simple quality == quality character value + quality
		// quality?
		for(Element chara:characters)
		{
			quality=chara.getAttributeValue("value")+" "+quality;
			quality=quality.trim();
			TermSearcher ts = new TermSearcher();
			Quality result = (Quality) ts.searchTerm(quality, "quality");
			if (result != null) {
				if (negated) {
					/* TODO use parent classes Jim use for parent classes */
					String[] parentinfo = ontoutil.retreiveParentInfoFromPATO(result.getId());
					Quality parentquality = new Quality();
					parentquality.setString(parentinfo[1]);
					parentquality.setLabel(parentinfo[1]);
					parentquality.setId(parentinfo[0]);
					QualityProposals qproposals = new QualityProposals();
					qproposals.add(new NegatedQuality(result, parentquality));
					this.qualities.add(qproposals);
					this.identifiedqualities.add(qualityid);
					chara.detach();
					//return;
				} else {
					QualityProposals qproposals = new QualityProposals();
					qproposals.add(result);
					this.qualities.add(qproposals);
					this.identifiedqualities.add(qualityid);
					chara.detach();
					//return;
				}
				if(chara_detach!=null)
					chara_detach.detach();
			}
		quality=qualitycopy;
	}
		return;
	}
}
