/**
 * 
 */
package fna.parsing.datacleaner;

import java.util.*;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * merge all kinds of distribution to outputelement/"general_distribution"
 * also parse out introduced, and cultivated from sourceelements(including elevation) 
 * outputelement, elevation, introduced, and cultivated should be child nodes of DISTRIBUTION, sibling nodes of ecological_info.
 * keep only capitalized words
 * @author hongcui
 *
 */
@SuppressWarnings({ "unchecked", "unused" })
public class CleanDistribution extends DataCleaner {
	private String directions="c|e|w|s|n|ec|wc|sc|nc|nw|ne|sw|se|central|east|west|south|north|eastcentral|westcentral|southcentral|northcentral|northwest|northeast|southwest|southeast";
	private Hashtable<String, String> statenames = new Hashtable<String, String>();
	private Hashtable<String, String> locations = new Hashtable<String, String>();
	/**
	 * sourceElements should also include xx_distribution, "elevation"
	 */
	public CleanDistribution(String sourcedir, ArrayList<String> sourceElements, String outputElement, String outputdir) {
		
		super(sourcedir, sourceElements, outputElement, outputdir);
		this.legalvalues="northern hemisphere|southern hemisphere|worldwide|tropical|temperate";
		//populate mapping tables
		statenames.put(	 "Alta",	"Alberta"	);
		statenames.put(	 "Ala",	"Alabama"	);
		statenames.put(	 "Mont",	"Montana"	);
		statenames.put(	 "B.C.",	"British Columbia"	);
		statenames.put(	 "Alaska",	"Alaska"	);
		statenames.put(	 "Nebr",	"Nebraska"	);
		statenames.put(	 "Man",	"Manitoba"	);
		statenames.put(	 "Ariz",	"Arizona"	);
		statenames.put(	 "Nev",	"Nevada"	);
		statenames.put(	 "N.B.",	"New Brunswick"	);
		statenames.put(	 "Ark",	"Arkansas"	);
		statenames.put(	 "N.H.",	"New Hampshire"	);
		statenames.put(	 "Nfld and Labr",	"Newfoundland and Labrador"	);
		statenames.put(	 "Nfld",	"Newfoundland"	);
		statenames.put(	 "Labr",	"Labrador"	);
		statenames.put(	 "Calif",	"California"	);
		statenames.put(	 "N.J.",	"New Jersey"	);
		statenames.put(	 "N.W.T",	"Northwest Territories"	);
		statenames.put(	 "Colo",	"Colorado"	);
		statenames.put(	 "N.Mex",	"New Mexico"	);
		statenames.put(	 "N.S.",	"Nova Scotia"	);
		statenames.put(	 "Conn",	"Connecticut"	);
		statenames.put(	 "N.Y.",	"New York"	);
		statenames.put(	 "Nunavut",	"Nunavut"	);
		statenames.put(	 "Del",	"Delaware"	);
		statenames.put(	 "N.C.",	"North Carolina"	);
		statenames.put(	 "Ont",	"Ontario"	);
		statenames.put(	 "D.C.",	"District of Columbia"	);
		statenames.put(	 "N.Dak",	"North Dakota"	);
		statenames.put(	 "P.E.I",	"Prince Edward Island"	);
		statenames.put(	 "Fla",	"Florida"	);
		statenames.put(	 "Ohio",	"Ohio"	);
		statenames.put(	 "Que",	"Quebec"	);
		statenames.put(	 "Ga",	"Georgia"	);
		statenames.put(	 "Okla",	"Oklahoma"	);
		statenames.put(	 "Sask",	"Saskatchewan"	);
		statenames.put(	 "Idaho",	"Idaho"	);
		statenames.put(	 "Oreg",	"Oregon"	);
		statenames.put(	 "Yukon",	"Yukon"	);
		statenames.put(	 "Ill",	"Illinois"	);
		statenames.put(	 "Pa",	"Pennsylvania"	);
		statenames.put(	 "Ind",	"Indiana"	);
		statenames.put(	 "R.I.",	"Rhode Island"	);
		statenames.put(	 "Iowa",	"Iowa"	);
		statenames.put(	 "S.C.",	"South Carolina"	);
		statenames.put(	 "Kans",	"Kansas"	);
		statenames.put(	 "S.Dak",	"South Dakota"	);
		statenames.put(	 "Ky",	"Kentucky"	);
		statenames.put(	 "Tenn",	"Tennessee"	);
		statenames.put(	 "La",	"Louisiana"	);
		statenames.put(	 "Tex",	"Texas"	);
		statenames.put(	 "Maine",	"Maine"	);
		statenames.put(	 "Utah",	"Utah"	);
		statenames.put(	 "Md",	"Maryland"	);
		statenames.put(	 "Vt",	"Vermont"	);
		statenames.put(	 "Mass",	"Massachusetts"	);
		statenames.put(	 "Va",	"Virginia"	);
		statenames.put(	 "Mich",	"Michigan"	);
		statenames.put(	 "Wash",	"Washington"	);
		statenames.put(	 "Minn",	"Minnesota"	);
		statenames.put(	 "W.Va",	"West Virginia"	);
		statenames.put(	 "Miss",	"Mississippi"	);
		statenames.put(	 "Wis",	"Wisconsin"	);
		statenames.put(	 "Mo",	"Missouri"	);
		statenames.put(	 "Wyo",	"Wyoming"	);
		//locations
		locations.put("s", "sourth");
		locations.put("c", "central");
		locations.put("e", "east");
		locations.put("w", "west");
		locations.put("n", "north");
		locations.put("ec", "eastcentral");
		locations.put("wc", "westcentral");
		locations.put("sc", "southcentral");
		locations.put("nc", "northcentral");
		locations.put("nw", "northwest");
		locations.put("ne", "northeast");
		locations.put("sw", "sourthwest");
		locations.put("se", "southeast");
		

		

	}

	protected Element clean(Element root){
		try{
			Element gdistribution = new Element("distribution");
			root.addContent(gdistribution);
			Iterator<String> it = this.sourceelements.iterator();
			while(it.hasNext()){
				String ename = it.next();
				List<Element> elements = XPath.selectNodes(root, "//"+ename);
				Iterator<Element> eit = elements.iterator();
				while(eit.hasNext()){
					Element e = eit.next();
					String text = e.getText();
					int type = 0;
					if(text.trim().toLowerCase().startsWith("introduced")){
						type = 1;
					}else if(text.trim().toLowerCase().startsWith("cultivat")){
						type = 2;
					}
					if(ename.endsWith("elevation")){//process elevation here
						Element ce = new Element("elevation");
						text = text.replaceFirst("^[^-\\(\\d]+", "").replaceAll("\\s+", "_").replaceAll("\\W+", "-").replaceAll("[)(]", "").replaceAll("-\\d+-", "-").replaceAll("_", " ").replaceFirst("\\W+$", "").trim();
						//System.out.println();System.out.println(text);
						ce.setText(text);
						gdistribution.addContent(ce);
					}
					ArrayList<String> values = cleanText(text);
					Element p = e.getParentElement();					
					p.removeContent(e);
					if(p.getChildren().size()==0){
						p.detach();
					}
					Iterator<String> vit = values.iterator();
					while(vit.hasNext()){//if values is empty, no replacement is done, but the original element is removed
						Element ce = null;
						String t = vit.next();
						t = standardize(t);
						switch (type){
						case 1: ce = new Element("introduced");
								//System.out.println(t);
								ce.setText(t);
								gdistribution.addContent(ce);
								break;
						case 2: ce = new Element("cultivated");
								//System.out.println(t);
								ce.setText(t);
								gdistribution.addContent(ce);
								break;
						}
						ce = new Element(this.outputelement); //add intro and cult distributions are also part of general distribution
						//System.out.println(t);
						ce.setText(t);
						gdistribution.addContent(ce);
					}
				}
			}
			//introduced;
			Element ie = (Element)XPath.selectSingleNode(root, "//ecological_info/introduced");
			if(ie!=null){
				Element iep = ie.getParentElement();
				ie.detach(); //remove introduced
				if(iep.getChildren().size()==0){//remove parent of introduced if the parent has no children
					iep.detach();
				}
				//copy content from outputelement to "introduced"
				List<Element> gdes = XPath.selectNodes(root, "//distribution/"+this.outputelement);
				Iterator<Element> dgit = gdes.iterator();
				while(dgit.hasNext()){
					Element dge = dgit.next();
					Element nie = new Element("introduced");
					String text = standardize(dge.getText());
					//System.out.println(text);
					nie.setText(text);
					gdistribution.addContent(nie);
				}
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return root;
	}

	/**
	 * s africa => south africa
	 * Calif =>California
	 * 
	 * @param t
	 * @return
	 */
	protected String standardize(String t) {
		String[] tokens = t.split("\\s+");
		String result = "";
		for(int i = 0; i<tokens.length; i++){
			String st = this.locations.get(tokens[i]);
			if(st == null){
				st = this.statenames.get(tokens[i]);
			}
			if(st == null){
				result += tokens[i]+" ";
			}else{
				result += st+" ";
			}
		}
		return result.trim();
	}

	protected void collectLegalValues(){
		Iterator<String> it = this.sourcecontent.iterator();
		while(it.hasNext()){
			String text = it.next();
			text = text.replaceAll("[()]", "BRACKET").replaceAll("\\s+", "_").replaceAll("\\W+", "-").replaceAll("\\p{Punct}", " ").replaceAll("BRACKET", "*").replaceAll("\\b[0-9a-z]+\\b", "*").replaceAll("\\s+", " ").trim(); //remove all words that are not capitalized
			if(text.length()>1){
				String[] values = text.split("\\*+");
				for(int i = 0; i<values.length; i++){
					String v = values[i].trim();
					v = v.replaceAll("(?<=\\b[A-Z])( |$)","."); //B C => B.C.
					if(!this.legalvalues.matches(".*?(^|\\|)"+v+"(\\||$).*")) this.legalvalues +="|"+v;
				}
			}
		}
		this.legalvalues = this.legalvalues.replaceAll("\\|+", "|");
	} 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcedir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta-clean-2";
		ArrayList<String> sourceElements = new ArrayList<String>();
		sourceElements.add("ecological_info/general_distribution");
		sourceElements.add("ecological_info/ca_distribution");
		sourceElements.add("ecological_info/us_distribution");
		sourceElements.add("ecological_info/global_distribution");
		sourceElements.add("ecological_info/elevation");
		String outputElement = "general_distribution";
		String outputdir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta-clean-3";
		CleanDistribution ct = new CleanDistribution(sourcedir, sourceElements, outputElement, outputdir);
		ct.collectSourceContent();
		ct.collectLegalValues();
		ct.cleanFiles();
	}

}
