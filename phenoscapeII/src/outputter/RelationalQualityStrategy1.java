package outputter;

import java.util.ArrayList;
import java.util.List;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;


//RelationalQualityStrategy is used to check whether an entity is a quality and if true will create qualities accordingly.
public class RelationalQualityStrategy1 {

	Element root, statement;
	String relation;
	String tostructname;
	String tostructid;
	String fromstructname;
	String fromstructid;
	boolean negation; // if true, negate the relation string
	boolean fromcharacterstatement;
	ArrayList<Quality> qualities = new ArrayList<Quality>();
	private TermOutputerUtilities ontoutil;
	XPath pathCharacterUnderStucture;
	ArrayList<Entity> keyentities;
	ArrayList<String> identifiedqualities;

	public RelationalQualityStrategy1(Element root, String relname,
			String toname, String toid, String fromname, String fromid,
			boolean neg, boolean b, Element statement, ArrayList<Entity> keyentities) throws JDOMException {
		this.root = root;
		this.relation = relname;
		this.tostructname = toname;
		this.tostructid = toid;
		this.fromstructname = fromname;
		this.fromstructid = fromid;
		this.fromcharacterstatement = b;
		pathCharacterUnderStucture = XPath.newInstance(".//character");
		this.keyentities = keyentities;
		identifiedqualities = new ArrayList<String>();
		
	}

	public void handle() {
		// TODO Auto-generated method stub
		try {
			parseforQuality(this.tostructname, this.tostructid);
			parseforQuality(this.fromstructname, this.fromstructid);

		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void parseforQuality(String quality, String qualityid)
			throws JDOMException {
		// characters => quality
		// get quality candidate
		// handle this later => use below code to look and use all the
		// characters under a structure
		List<Element> characters = new ArrayList<Element>();
		Element Structures;
		boolean negated = false;
		XPath structurewithstructid = XPath.newInstance(".//structure[@id='"+ qualityid + "']");
		Structures = (Element) structurewithstructid.selectSingleNode(this.root);
		characters = pathCharacterUnderStucture.selectNodes(Structures);

		for (Element chara : characters) {
			String value = Utilities.formQualityValueFromCharacter(chara);
			if (value.startsWith("not") || (value.equals("absent"))) {
				negated = true;
				quality = quality.substring(quality.indexOf(" ") + 1).trim();
				chara.detach();
				// TODO:// deal// with// negated// quality// here
			}

		}
		// is the candidate a relational quality?
		Quality relationalquality = PermittedRelations.matchInPermittedRelation(quality, false);
		if (relationalquality != null)
		{
			//If two entities are there, then the first one is the primary entity and the second one is the related entity
			if(this.keyentities.size()==2)
			{
				for(int i=1;i<this.keyentities.size();i++)
				{
					RelationalQuality rq = new RelationalQuality(relationalquality, this.keyentities.get(i));
					this.qualities.add(rq);
					this.identifiedqualities.add(qualityid);
					return;
				}
			}
			else
			{ // TODO how to find related entities from a list of entities >2
				}
			
		}
		
        
		// Take care of constraints in characters later
		// constraints may yield entity parts such as entity locator, save
		// those, resolve them later

		// not a relational quality, is this a simple quality or a negated
		// quality?
		Quality result = (Quality) TermSearcher.searchTerm(quality, "quality",0);
		if (result != null) {
			if (negated) {
				/* TODO use parent classes Jim use for parent classes */
				String[] parentinfo = ontoutil.retreiveParentInfoFromPATO(result.getId());
				Quality parentquality = new Quality();
				parentquality.setString(parentinfo[1]);
				parentquality.setLabel(parentinfo[1]);
				parentquality.setId(parentinfo[0]);

				this.qualities.add(new NegatedQuality(result, parentquality));
				this.identifiedqualities.add(qualityid);
				return;
			} else {
				this.qualities.add(result);
				this.identifiedqualities.add(qualityid);
				return;
			}
		} else {
			result = new Quality();
			result.string = quality;
			result.confidenceScore = (float) 1.0;
			this.qualities.add(result);
			this.identifiedqualities.add(qualityid);
			return;
		}
	}

	public static String formQualityValueFromCharacter(Element chara) {
		String charatype = chara.getAttribute("char_type") != null ? "range"
				: "discrete";
		String quality = "";
		if (charatype.compareTo("range") == 0) {
			quality = chara.getAttributeValue("from")
					+ " "
					+ (chara.getAttribute("from_unit") != null ? chara
							.getAttributeValue("from_unit") : "")
					+ " to "
					+ chara.getAttributeValue("to")
					+ " "
					+ (chara.getAttribute("to_unit") != null ? chara
							.getAttributeValue("to_unit") : "");

		} else {
			quality = (chara.getAttribute("modifier") != null
					&& chara.getAttributeValue("modifier").matches(
							".*?\\bnot\\b.*") ? "not" : "")
					+ " "
					+ chara.getAttributeValue("value")
					+ " "
					+ (chara.getAttribute("unit") != null ? chara
							.getAttributeValue("unit") : "")
					+ "["
					+ (chara.getAttribute("modifier") != null ? chara
							.getAttributeValue("modifier").replaceAll(
									"\\bnot\\b;?", "") : "") + "]";

		}
		quality = quality.replaceAll("\\[\\]", "").replaceAll("\\s+", " ")
				.trim();
		return quality;
	}

}
