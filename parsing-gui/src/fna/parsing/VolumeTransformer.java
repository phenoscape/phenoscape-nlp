/**
 * $Id: VolumeTransformer.java 996 2011-10-07 01:13:47Z hong1.cui $
 */
package fna.parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.db.VolumeTransformerDbAccess;

/**
 * To transform the extracted data to the xml format.
 * 
 * Note: before the transformation, the data should pass the check without
 * error.
 * 
 * @author chunshui
 */
@SuppressWarnings({ "unchecked", "unused","static-access" })
public class VolumeTransformer extends Thread {
	
	private static String organnames ="2n|achene|anther|apex|awn|ax|bark|beak|blade|bract|bracteole|branch|branchlet|broad|calyx|capsule|cap_sule|caropohore|carpophore|caudex|cluster|corolla|corona|crown|cup_|cusp|cyme|cymule|embryo|endosperm|fascicle|filament|flower|fruit|head|herb|homophyllous|hypanthium|hypanth_ium|indument|inflore|inflorescence|inflores_cence|inflo_rescence|internode|involucre|invo_lucre|in_florescence|in_ternode|leaf|limb|lobe|margin|midvein|nectary|node|ocrea|ocreola|ovary|ovule|pair|papilla|pedicel|pedicle|peduncle|perennial|perianth|petal|petiole|plant|prickle|rhizome|rhi_zome|root|rootstock|rosette|scape|seed|sepal|shoot|spikelet|spur|stamen|stem|stigma|stipule|sti_pule|structure|style|subshrub|taproot|taprooted|tap_root|tendril|tepal|testa|tooth|tree|tube|tubercle|tubercule|tuft|twig|utricle|vein|vine|wing|x";
	private static String organnamep ="achenes|anthers|awns|axes|blades|bracteoles|bracts|branches|buds|bumps|calyces|capsules|clusters|crescents|crowns|cusps|cymes|cymules|ends|escences|fascicles|filaments|flowers|fruits|heads|herbs|hoods|inflores|inflorescences|internodes|involucres|leaves|lengths|limbs|lobes|margins|midribs|midveins|nectaries|nodes|ocreae|ocreolae|ovules|pairs|papillae|pedicels|pedicles|peduncles|perennials|perianths|petals|petioles|pistils|plants|prickles|pules|rescences|rhizomes|rhi_zomes|roots|rows|scapes|seeds|sepals|shoots|spikelets|stamens|staminodes|stems|stigmas|stipules|sti_pules|structures|styles|subshrubs|taproots|tap_roots|teeth|tendrils|tepals|trees|tubercles|tubercules|tubes|tufts|twigs|utricles|veins|vines|wings";
	private static String usstates ="Ala\\.|Alaska|Ariz\\.|Ark\\.|Calif\\.|Colo\\.|Conn\\.|Del\\.|D\\.C\\.|Fla\\.|Ga\\.|Idaho|Ill\\.|Ind\\.|Iowa|Kans\\.|Ky\\.|La\\.|Maine|Md\\.|Mass\\.|Mich\\.|Minn\\.|Miss\\.|Mo\\.|Mont\\.|Nebr\\.|Nev\\.|N\\.H\\.|N\\.J\\.|N\\.Mex\\.|N\\.Y\\.|N\\.C\\.|N\\.Dak\\.|Ohio|Okla\\.|Oreg\\.|Pa\\.|R\\.I\\.|S\\.C\\.|S\\.Dak\\.|Tenn\\.|Tex\\.|Utah|Vt\\.|Va\\.|Wash\\.|W\\.Va\\.|Wis\\.|Wyo\\.";	
	private static String caprovinces="Alta\\.|B\\.C\\.|Man\\.|N\\.B\\.|Nfld\\. and Labr|N\\.W\\.T\\.|N\\.S\\.|Nunavut|Ont\\.|P\\.E\\.I\\.|Que\\.|Sask\\.|Yukon";
	private Properties styleMappings;
	private TaxonIndexer ti;
	private ProcessListener listener;
	//private Hashtable errors;
	//TODO: put the following in a conf file. same for those in volumeExtractor.java
	//private String start = "^Heading.*"; //starts a treatment
	private String start = VolumeExtractor.getStart(); //starts a treatment
	private String names = ".*?(Syn|Name).*"; //other interesting names worth parsing
	private String conservednamestatement ="(name conserved|nom. cons.)";
	private static final Logger LOGGER = Logger.getLogger(VolumeTransformer.class);
	private VolumeTransformerDbAccess vtDbA = null;	
	private Hashtable ranks;

	private String taxontable = null;
	private String authortable = null;
	private String publicationtable = null;
	private Connection conn = null;
	private String dataPrefix;
	
	private boolean debug = false;
	private boolean debugref = false;
	private boolean debugkey = true;
	
	public VolumeTransformer(ProcessListener listener, String dataPrefix) throws ParsingException {
		this.listener = listener;
		this.dataPrefix = dataPrefix;
		//this.errors = new Hashtable();
		this.taxontable = dataPrefix.trim()+"_"	+ ApplicationUtilities.getProperty("taxontable");
		this.authortable = dataPrefix.trim() + "_" + ApplicationUtilities.getProperty("authortable");
		this.publicationtable = dataPrefix.trim() + "_" + ApplicationUtilities.getProperty("publicationtable");
		vtDbA = new VolumeTransformerDbAccess(dataPrefix);
		
		ti = TaxonIndexer.loadUpdated(Registry.ConfigurationDirectory);
		if(ti.emptyNumbers() || ti.emptyNames()) ti = null;
		
		// load style mapping
		styleMappings = new Properties();
		try {
			styleMappings.load(new FileInputStream(
					Registry.ConfigurationDirectory
							+ "/style-mapping.properties"));
		} catch (IOException e) {
			throw new ParsingException(
					"Failed to load the style mapping file!", e);
		}
		
		try{
			if(conn == null){
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("drop table if exists "+taxontable);
				stmt.execute("create table if not exists "+taxontable+" (taxonnumber varchar(10), name varchar(500), rank varchar(20), filenumber int)");
				stmt.execute("drop table if exists "+authortable);
				stmt.execute("create table if not exists "+ authortable+" (authority varchar(500) NOT NULL)");
				stmt.execute("drop table if exists "+publicationtable);
				stmt.execute("create table if not exists "+ publicationtable+" (publication varchar(500) NOT NULL)");				
			}
		}catch(Exception e){
			LOGGER.error("VolumeTransformer : Database error in constructor", e);
			e.printStackTrace();
		}	

	}

	
	/**
	 * Transform the extracted data to the xml format.
	 */
	public void run() {
		listener.setProgressBarVisible(true);
		transform();
		listener.setProgressBarVisible(false);
	}
	public void transform() throws ParsingException {
		//add start
		List idlist = new ArrayList();
		int iteratorcount = 0;
		String state = "", preid = "", id = "", nextstep = "";
		String split[] = new String[3];
		String split1[] = new String[30];
		String latin[] = new String[300];
		latin[0] = "a";
		latin[1] = "b";
		latin[2] = "c";
		latin[3] = "d";
		latin[4] = "e";
		latin[5] = "f";
		latin[6] = "g";
		latin[7] = "h";
		latin[8] = "i";
		//add end
		// get the extracted files list
		File source = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty("EXTRACTED"));
		int total = source.listFiles().length;
		listener.progress(1);
		try {
			for (int count = 1; count <= total; count++) {

				File file = new File(source, count + ".xml");
				// logger.info("Start to process: " + file.getName());

				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element root = doc.getRootElement();

				Element treatment = new Element("treatment");
				Element e2 = new Element("key");
				List plist = XPath.selectNodes(root, "/treatment/paragraph");
				int textcount = 0, nextstepid = 0;
				String ptexttag ="";
				String idstorage = "1";
				
				for (Iterator iter = plist.iterator(); iter.hasNext();) {
					Element pe = (Element) iter.next();
					String style = pe.getChildText("style");
					String text = getChildText(pe, "text");
					
					if (style.matches(start) ) {
						// process the name tag
						String sm= styleMappings.getProperty(style);//hong 6/26/08
						parseNameTag(count - 1, sm, text, treatment);
					}else if  (style.matches(names)) {
						// process the  synonym name tag
						String sm= styleMappings.getProperty(style);//hong 6/26/08
						parseSynTag(sm, text, treatment);
					}else if  (style.indexOf("Text") >= 0) {//hong 6/26/08
						// process the description, distribution, discussion tag
						if(text.trim().compareTo("") !=0){
							textcount++;
							ptexttag = parseTextTag(textcount, text, treatment, count, ptexttag);
						}
					}else {
						String sm = styleMappings.getProperty(style);
						Element e = new Element(sm);
						e.setText(text);
						treatment.addContent(e);
						/*
						text=text.replaceFirst("SELECTED REFERENCES?", "").trim();
						//end text format change++++++++++++++++++++++++++++++++++++++++++++++
						Matcher refM=Pattern.compile("([A-Z]\\w*?,? [A-Z]\\.)+(.*?)\\.(?=\\s[A-Z]\\w*?,? [A-Z]\\.(,|\\s?\\d{4}|\\s?[A-Z]\\.)|$)").matcher(text);
						while(refM.find()){
							addElement("reference",refM.group(1),e);
						}
						//e.setText(text);
						//Start text format change++++++++++++++++++++++++++++++++++++++++++++++++++
						
						//keys
						
						Element initial = new Element("initial_state");
						Element states = new Element("state");
						Element nextsteps = new Element("next_step");
						
						
						if(sm.equalsIgnoreCase("run_in_sidehead")){
							e2 = new Element("key");
							e2.setAttribute(new Attribute("name", text));
							treatment.addContent(e2);
							idlist.clear();
						}
						else if(sm.equals("key")){
							Element e1 = new Element("couplet");
							if (text.contains(" v. ") && text.contains(" p. ")
									&& !text.contains("Group ")) {
								split = text.split("[0-9]+[a-z]?\\. ");
								split1 = split[0].split("\\.");
								preid = split1[0];
								state = split[0].replace(preid + ".", "");
								nextstep = text.replace(split[0], "");
								idstorage = preid;
								Iterator iditerator = idlist.iterator();
								iteratorcount = 0;
								while(iditerator.hasNext()){
									String itemid = (String)iditerator.next();
									if(itemid.equalsIgnoreCase(preid)){
										iteratorcount++;
									}
								}
								id = preid + latin[iteratorcount];
								idlist.add(preid);
								nextsteps.setText(nextstep);
								// System.out.println(nextstep);
							} else if (text.contains(" v. ")
									&& text.contains(" p. ")
									&& text.contains("Group ")) {
								split = text.split("Group [0-9]");
								split1 = split[0].split("\\.");
								preid = split1[0];
								state = split[0].replace(preid + ".", "");
								nextstep = text.replace(split[0], "");
								idstorage = preid;
								Iterator iditerator = idlist.iterator();
								iteratorcount = 0;
								while(iditerator.hasNext()){
									String itemid = (String)iditerator.next();
									if(itemid.equalsIgnoreCase(preid)){
										iteratorcount++;
									}
								}
								id = preid + latin[iteratorcount];
								idlist.add(preid);
								nextsteps.setText(nextstep);
								// System.out.println(nextstep);
							} else if (!text.contains("Shifted to left margin.")&&text.contains("")) {
								split1 = text.split("\\.");
								preid = split1[0];
								state = text.replace(preid + ".", "");
								try{
								nextstepid = Integer.parseInt(idstorage) + 1;
								}catch(Exception excep){
									continue;
								}
								nextstep = nextstepid + "a";
								idstorage = preid;
								Iterator iditerator = idlist.iterator();
								iteratorcount = 0;
								while(iditerator.hasNext()){
									String itemid = (String)iditerator.next();
									if(itemid.equalsIgnoreCase(preid)){
										iteratorcount++;
									}
								}
								id = preid + latin[iteratorcount];
								idlist.add(preid);
								nextsteps.setAttribute(new Attribute("id", nextstep));
								//nextstep = nextid + "a";
								 //System.out.println(preid + "   " + state + "   " + nextstep);
							}
							
							initial.setAttribute(new Attribute("id", id));
							states.setText(state);
							
							e1.addContent(initial);
							e1.addContent(states);
							e1.addContent(nextsteps);
							e2.addContent(e1);
						}
						else{
							e.setName(sm);
							e.setText(text);
							treatment.addContent(e);
						}
							

						*/
						
	
					}
				}
			
				//further mark up reference
				List<Element> elements = XPath.selectNodes(treatment, "./references");
				Iterator<Element> it = elements.iterator();
				while(it.hasNext()){
					Element ref = it.next();
					furtherMarkupReference(ref);
				}
				
				//further mark up keys <run_in_sidehead>
				elements = XPath.selectNodes(treatment, "./key|./couplet");
				if(elements.size()>0){//contains key
					furtherMarkupKeys(treatment);
				}
				
				
				// output the treatment to transformed
				File xml = new File(Registry.TargetDirectory,
						ApplicationUtilities.getProperty("TRANSFORMED") + "/" + count + ".xml");
				ParsingUtil.outputXML(treatment, xml ,null);
				//String error = (String)errors.get(count+"");
				//error = error ==null? "":error;
				
				// output the description part to Registry.descriptions 08/04/09
				List<Element> textList = XPath.selectNodes(treatment, "./description");
				StringBuffer buffer = new StringBuffer("");
				for (Iterator ti = textList.iterator(); ti.hasNext();) {
					Element wt = (Element) ti.next();
					buffer.append(wt.getText()).append(" ");
				}
				String text = buffer.toString().replaceAll("\\s+", " ").trim();
				outputElementText(count, text, "DESCRIPTIONS");
				
				// output the habitat part to Registry.habitat 08/04/09
				textList = XPath.selectNodes(treatment, "./habitat");
				buffer = new StringBuffer("");
				for (Iterator ti = textList.iterator(); ti.hasNext();) {
					Element wt = (Element) ti.next();
					buffer.append(wt.getText()).append(" ");
				}
				text = buffer.toString().replaceAll("\\s+", " ").trim();
				outputElementText(count, text, "HABITATS");
				
				
				//listener.info(String.valueOf(count), xml.getPath(), error);
				listener.progress((count*50) / total);
			}
			
			//HabitatParser4FNA hpf = new HabitatParser4FNA(dataPrefix);
			//hpf.parse();
			//VolumeFinalizer vf = new VolumeFinalizer(listener,null, null, this.conn,null, null);//display output files to listener here.
			//vf.replaceWithAnnotated(hpf, "/treatment/habitat", "TRANSFORMED", true);
		} catch (Exception e) {
			LOGGER.error("VolumeTransformer : transform - error in parsing", e);
			e.printStackTrace();
			throw new ParsingException(e);
		}
	}

	/**
	 * First assemble the key element(s) <key></key>
	 * Then turn individual statement :
	 *  <key>2. Carpels and stamens more than 5; plants perennial; leaves alternate; inflorescences ax-</key>
  	 *	<key>illary, terminal, or leaf-opposed racemes or spikes ### 3. Phytolac ca ### (in part), p. 6</key>
     * to:
     * <key_statement>
     * <statement_id>2</statement_id>
     * <statement>Carpels and stamens more than 5; 
     * plants perennial; leaves alternate; inflorescences ax-illary, terminal, 
     * or leaf-opposed racemes or spikes</statement>
     * <determination>3. Phytolacca (in part), p. 6</determination>
     * </key_statement>
     * 
     * <determination> is optional, and may be replaced by <next_statement_id>.
	 * @param treatment
	 */
	private void furtherMarkupKeys(Element treatment) {
		assembleKeys(treatment);
		try{
			List<Element> keys = XPath.selectNodes(treatment, "./TaxonKey");
			for(Element key: keys){
				furtherMarkupKeyStatements(key);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	/* Turn individual statement :
	 *  <key>2. Carpels and stamens more than 5; plants perennial; leaves alternate; inflorescences ax-</key>
  	 *	<key>illary, terminal, or leaf-opposed racemes or spikes ### 3. Phytolac ca ### (in part), p. 6</key>
     * To:
     * <key_statement>
     * <statement_id>2</statement_id>
     * <statement>Carpels and stamens more than 5; 
     * plants perennial; leaves alternate; inflorescences ax-illary, terminal, 
     * or leaf-opposed racemes or spikes</statement>
     * <determination>3. Phytolacca (in part), p. 6</determination>
     * </key_statement>
     * 
     * <determination> is optional, and may be replaced by <next_statement_id>.
	 * @param treatment
	 */
	private void furtherMarkupKeyStatements(Element taxonkey) {
		ArrayList<Element> allstatements = new ArrayList<Element>();
		Element marked = new Element("key");
		List<Element> states = taxonkey.getChildren();
		Pattern p1 = Pattern.compile("(.*?)(( ### [\\d ]+[a-z]?\\.| ?#* ?Group +\\d).*)");//determ
		Pattern p2 = Pattern.compile("^([\\d ]+[a-z]?\\..*?) (.? ?[A-Z].*)");//id   2. "Ray” corollas
		String determ = null;
		String id = "";
		String broken = "";
		String preid = null;
		//process statements backwards
		for(int i = states.size()-1; i>=0; i--){
			Element state = states.get(i);
			if(state.getName().compareTo("key") == 0 || state.getName().compareTo("couplet") == 0){
				String text = state.getTextTrim()+broken;
				Matcher m = p1.matcher(text);
				if(m.matches()){
					text = m.group(1).trim();
					determ = m.group(2).trim();
				}
				m = p2.matcher(text);
				if(m.matches()){//good, statement starts with an id
					id = m.group(1).trim();
					text = m.group(2).trim();
					broken = "";
					//form a statement
					Element statement = new Element("key_statement");
					Element stateid = new Element("statement_id");
					stateid.setText(id.replaceAll("\\s*###\\s*", ""));
					Element stmt = new Element("statement");
					stmt.setText(text.replaceAll("\\s*###\\s*", ""));
					Element dtm = null;
					Element nextid = null;
					if(determ!=null) {
						dtm = new Element("determination");
						dtm.setText(determ.replaceAll("\\s*###\\s*", ""));
						determ = null;
					}else if(preid!=null){
						nextid = new Element("next_statement_id");
						nextid.setText(preid.replaceAll("\\s*###\\s*", ""));
						//preid = null;
					}
					preid = id;
					statement.addContent(stateid);
					statement.addContent(stmt);
					if(dtm!=null) statement.addContent(dtm);
					if(nextid!=null) statement.addContent(nextid);
					allstatements.add(statement);
				}else if(text.matches("^[a-z]+.*")){//a broken statement, save it
					broken = text;
				}
			}else{
				Element stateclone = (Element)state.clone();
				if(stateclone.getName().compareTo("run_in_sidehead")==0){
					stateclone.setName("key_head");
				}
				allstatements.add(stateclone);//"discussion" remains
			}
		}
		
		for(int i = allstatements.size()-1; i >=0; i--){
			marked.addContent(allstatements.get(i));
		}		
		taxonkey.getParentElement().addContent(marked);
		taxonkey.detach();
	}


	/**
	 * <treatment>
	 * <...>
	 * <references>...</references>
	 * <key>...</key>
	 * </treatment>
	 * deals with two cases:
	 * 1. the treatment contains one key with a set of "key/couplet" statements (no run_in_sidehead tags)
	 * 2. the treatment contains multiple keys that are started with <run_in_sidehead>Key to xxx (which may be also used to tag other content)
	 * @param treatment
	 */
	private void assembleKeys(Element treatment) {
		Element key = null;
		//removing individual statements from treatment and putting them in key
		List<Element> children = treatment.getChildren();////changes to treatment children affect elements too.
		Element[] elements = children.toArray(new Element[0]); //take a snapshot
		ArrayList<Element> detacheds = new ArrayList<Element>();
		boolean foundkey = false;
		for(int i = 0; i < elements.length; i++){
			Element e = elements[i];
			if(e.getName().compareTo("run_in_sidehead")==0 && (e.getTextTrim().startsWith("Key to ") || e.getTextTrim().matches("Group \\d+.*"))){
				foundkey = true;
				if(key!=null){
					treatment.addContent((Element)key.clone());	
				}
				key = new Element("TaxonKey");
			}
			if(!foundkey && (e.getName().compareTo("key")==0 || e.getName().compareTo("couplet")==0)){
				foundkey = true;	
				if(key==null){
					key = new Element("TaxonKey");
				}
			}
			if(foundkey){
				detacheds.add(e);
				key.addContent((Element)e.clone());
			}			
		}
		if(key!=null){
			treatment.addContent(key);					
		}
		for(Element e: detacheds){
			e.detach();
		}
	}


	/**
	 * turn
	 * <references>SELECTED REFERENCES Behnke, H.-D., C. Chang, I. J. Eifert, and T. J. Mabry. 1974. Betalains and P-type sieve-tube plastids in Petiveria and Agdestis (Phytolaccaceae). Taxon 23: 541–542. Brown, G. K. and G. S. Varadarajan. 1985. Studies in Caryophyllales I: Re-evaluation of classification of Phytolaccaceae s.l. Syst. Bot. 10: 49–63. Heimerl, A. 1934. Phytolaccaceae. In: H. G. A. Engler et al., eds. 1924+. Die natürlichen Pflanzenfamilien…, ed. 2. 26+ vols. Leipzig and Berlin. Vol. 16c, pp. 135–164. Nowicke, J. W. 1968. Palynotaxonomic study of the Phytolaccaceae. Ann. Missouri Bot. Gard. 55: 294–364. Rogers, G. K. 1985. The genera of Phytolaccaceae in the southeastern United States. J. Arnold Arbor. 66: 1–37. Thieret, J. W. 1966b. Seeds of some United States Phytolaccaceae and Aizoaceae. Sida 2: 352–360. Walter, H. P. H. 1906. Die Diagramme der Phytolaccaceen. Leipzig. [Preprinted from Bot. Jahrb. Syst. 37(suppl.): 1–57.] Walter, H. P. H. 1909. Phytolaccaceae. In: H. G. A. Engler, ed. 1900–1953. Das Pflanzenreich…. 107 vols. Berlin. Vol. 39[IV,83], pp. 1–154. Wilson, P. 1932. Petiveriaceae. In: N. L. Britton et al., eds. 1905+. North American Flora…. 47+ vols. New York. Vol. 21, pp. 257–266.</references>
	 * to
	 * <references><reference>Behnke, H.-D., C. Chang, I. J. Eifert, and T. J. Mabry. 1974. Betalains and P-type sieve-tube plastids in Petiveria and Agdestis (Phytolaccaceae). Taxon 23: 541–542. </reference> <reference>...</reference>....</references>
	 * @param ref
	 * @return
	 */
	private void furtherMarkupReference(Element ref) {
		//Element marked = new Element("references");
		String text = ref.getText();
		ref.setText("");
		if(this.debugref) System.out.println("\nReferences text:"+text);
		Pattern p = Pattern.compile("(.*?\\d+–\\d+\\.\\]?)(\\s+[A-Z]\\w+,.*)");
		Matcher m = p.matcher(text);
		while(m.matches()){
			String refstring = m.group(1);
			Element refitem = new Element("reference");
			refitem.setText(refstring);
			ref.addContent(refitem);
			if(this.debugref) System.out.println("a ref:"+refstring);
			text = m.group(2);
			m = p.matcher(text);
		}
		Element refitem = new Element("reference");
		refitem.setText("item:"+text);
		ref.addContent(refitem);
		if(this.debugref) System.out.println("a ref:"+text);
		//ref.getParentElement().addContent(marked);
		//ref.detach();	
	}


	private String getChildText(Element pe, String string) throws Exception{
		// TODO Auto-generated method stub
		StringBuffer buffer=new StringBuffer();
		List<Element> textList = XPath.selectNodes(pe, "./"+string);
		for (Iterator ti = textList.iterator(); ti.hasNext();) {
			Element wt = (Element) ti.next();
			buffer.append(wt.getText()).append(" ");
		}
		return buffer.toString().replaceAll("\\s+", " ").trim();
	}

	private String parseTextTag(int textcount, String text, Element treatment, int filecount, String ptag){

		String tag = "";
		Pattern organpt = Pattern.compile("\\b("+this.organnamep+"|"+this.organnames+")\\b", Pattern.CASE_INSENSITIVE);
		Matcher m = organpt.matcher(text);
		int organcount = 0;
		while(m.find()){
			////System.out.println(m.group());
			organcount++;
		}
		if(textcount ==1 && organcount >=2){
			tag = "description";
			addElement("description", text, treatment);
			//outputDescriptionText(filecount, text); //hong: 08/04/09 take this function out. FOC descriptions are not part of TEXT.
		}else if((textcount ==1 && organcount < 2)){
			tag = "distribution";
			//TODO: further markup distribution to: # of infrataxa, introduced, generalized distribution, flowering time,habitat, elevation, state distribution, global distribution 
			//addElement("distribution", text, treatment);
			parseDistriTag(text, treatment);
		}//else if(ptag.compareTo("distribution")==0){
		else if(ptag.compareTo("description")==0){//hong: 3/11/10 for FNA v19
			tag = "distribution";
			//TODO: further markup distribution to: # of infrataxa, introduced, generalized distribution, flowering time,habitat, elevation, state distribution, global distribution 
			//addElement("distribution", text, treatment);
			parseDistriTag(text, treatment);
		}else if(ptag.compareTo("distribution")==0||ptag.compareTo("discussion")==0){
			tag = "discussion";
			addElement("discussion", text, treatment);
			//System.out.println("discussion:"+text);
		}
		return tag;
		
	}
	/**
	 * further markup distribution to: (species-with infrataxa and higher)
	 * # of infrataxa, introduced, generalized distribution, 
	 * or (species-without infrataxa and lower)
	 * flowering time,habitat, elevation, state distribution, global distribution 
	 * @param text
	 * @param treatment
	 */
	private void parseDistriTag(String text, Element treatment){
		//System.out.println("::::::::::::::::::::::::::::::::::\ndistribution: "+text);
		Pattern rankp = Pattern.compile("^((?:Genera|Genus|Species|Subspecies|Varieties|Subgenera).*?:)\\s*(introduced\\s*;)?(.*)");
		Matcher m = rankp.matcher(text);
		if(m.matches()){//species and higher
			if(m.group(1) != null){
				addElement("number_of_infrataxa",m.group(1), treatment);
				//System.out.println("number_of_infrataxa:"+m.group(1));
			}
			if(m.group(2)!=null){
					addElement("introduced", m.group(2), treatment);
					//System.out.println("introduced:"+m.group(2));
			}
			if(m.group(3) != null){
					//addElement("general_distribution", m.group(3), treatment);
					//further markkup distribution
					DistributionParser4FNA dp = new DistributionParser4FNA(treatment, m.group(3), "general_distribution");
					treatment = dp.parse(); 
					//System.out.println("general_distribution:"+m.group(3));
			}	
		}else{//species and lower
			Pattern h = Pattern.compile("(Flowering.*?\\.)?(.*?(?:;|\\.$))?(\\s*of conservation concern\\s*(?:;|\\.$))?(.*?\\b(?:\\d+|m)\\b.*?(?:;|\\.$))?\\s*(introduced(?:;|\\.$))?(.*)");
			Matcher mh = h.matcher(text);
			if(mh.matches()){//TODO:habitat, elevation, state distribution, global distribution
				if(mh.group(1) != null){
					//addElement("flowering_time",mh.group(1), treatment);
					//further markkup distribution
					FloweringTimeParser4FNA dp = new FloweringTimeParser4FNA(treatment, mh.group(1), "flowering_time");
					treatment = dp.parse(); 
					//System.out.println("flowering_time:"+mh.group(1));
				}
				if(mh.group(2)!= null){
					addElement("habitat",mh.group(2), treatment);
					//System.out.println("habitat:"+mh.group(2));
				}
				if(mh.group(3)!= null){
					addElement("conservation",mh.group(3), treatment);
					//System.out.println("conservation:"+mh.group(3));
				}
				if(mh.group(4)!= null){
					addElement("elevation",mh.group(4), treatment);
					//System.out.println("elevation:"+mh.group(4));
				}
				if(mh.group(5)!= null){
					addElement("introduced",mh.group(5), treatment);
					//System.out.println("introduced:"+mh.group(5));
				}
				if(mh.group(6)!= null){
					String[] distrs = mh.group(6).split(";");
					for(int i= 0; i<distrs.length; i++){
						if(distrs[i].matches(".*?\\b("+this.usstates+")(\\W|$).*")){
							//addElement("us_distribution",distrs[i], treatment);
							//further markkup distribution
							DistributionParser4FNA dp = new DistributionParser4FNA(treatment, distrs[i], "us_distribution");
							treatment = dp.parse(); 
							//System.out.println("us_distribution:"+distrs[i]);
						}else if(distrs[i].matches(".*?\\b("+this.caprovinces+")(\\W|$).*")){
							//addElement("ca_distribution",distrs[i], treatment);
							//further markkup distribution
							DistributionParser4FNA dp = new DistributionParser4FNA(treatment, distrs[i], "ca_distribution");
							treatment = dp.parse(); 
							//System.out.println("ca_distribution:"+distrs[i]);
						}else{
							//addElement("global_distribution",distrs[i], treatment);
							//further markkup distribution
							DistributionParser4FNA dp = new DistributionParser4FNA(treatment, distrs[i], "global_distribution");
							treatment = dp.parse(); 
							//System.out.println("global_distribution:"+distrs[i]);
						}
					}
				}
			}else{
				System.err.println("distribution not match: "+text);
			}
			
			
		}
	}
	
	private void parseSynTag(String tag, String text, Element treatment){
		Element e = treatment.getChild("variety_name");
		if(e != null){
			tag = "synonym_of_variety_name";
		}else if((e = treatment.getChild("subspecies_name"))!=null){
			tag = "synonym_of_subspecies_name";
		}else if((e = treatment.getChild("species_name"))!=null){
			tag = "synonym_of_species_name";
		}else if((e = treatment.getChild("tribe_name"))!=null){
			tag = "synonym_of_tribe_name";
		}else if((e = treatment.getChild("genus_name"))!=null){
			tag = "synonym_of_genus_name";
		}
		
		addElement(tag, text, treatment);
		//System.out.println(tag+":"+text);
	}
	
	private String parseNameTag(int index, String namerank, String line,
			Element treatment) {
		if(line == null || line.equals("")){
			return ""; //TODO: should not happen. but did happen with v. 19 295.xml==>VolumeExtractor JDOM problem.
		}
		
		String name = ti.getName(index);
		if(name==null ||name.compareTo("") == 0){
			File xml = new File(Registry.TargetDirectory,
					ApplicationUtilities.getProperty("TRANSFORMED") + "/" + (index+1) + ".xml");
			listener.info("no name found in: ", xml.getPath());
			//errors.put((index+1)+"","no name found in: "+line);
			return "";
		}
		// make a copy of the line and will work on the new copy
		String text = new String(line);
		text = text.replaceAll(" ", " ").replaceAll("\\s+", " ").trim(); //there are some whitespaces that are not really a space, don't know what they are. 
		if(debug) System.out.println("\n"+(index+1)+": text="+text);
		
		String number = null;
		if(ti != null)
			number = ti.getNumber(index);
		else{
			number = line.substring(0, line.indexOf('.'));
		}
		// number
		addElement("number", number, treatment); // TODO: add the number tag
		                                         // to the sytle mapping

		//text = text.substring(number.length() + 1); //Hong 08/04/09 change to
		text = VolumeVerifier.fixBrokenNames(text);
		text = text.replaceFirst("^.*?(?=[A-Z])", "").trim();;
		
		//namerank and name
		//(subfam|var|subgen|subg|subsp|ser|tribe|subsect)
		if(namerank.indexOf("species_subspecies_variety_name")>=0){
			if(text.indexOf("var.") >=0){
				namerank = "variety_name";
			}else if(text.indexOf("subsp.") >=0){
				namerank = "subspecies_name";
			}else if(text.indexOf("ser.") >=0){
				namerank = "series_name";
			}else if(text.indexOf("sect.") >=0){
				namerank = "section_name";
			}else if(text.indexOf("subsect.") >=0){
				namerank = "subsection_name";
			}else {
				namerank = "species_name";
			}
		}
		if(debug) System.out.println("namerank:"+namerank);
		String[] nameinfo = getNameAuthority(name);
		if(nameinfo[0]!=null && nameinfo[1]!=null){
		addElement(namerank, nameinfo[0], treatment);
		try {
			vtDbA.add2TaxonTable(number, name, namerank, index+1);
		} catch (ParsingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
		}
		if(debug) System.out.println("name:"+nameinfo[0]);
		if(nameinfo[1].length()>0){
			addElement("authority", nameinfo[1], treatment);
			try {
				vtDbA.add2AuthorTable(nameinfo[1]);
			} catch (ParsingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
			}
			if(debug) System.out.println("authority:"+nameinfo[1]);
		}
		text = text.replaceFirst("^\\s*.{"+name.length()+"}","").trim();
		}
		//authority
		/*Pattern p = Pattern.compile("(.*?)((?: in|,|Â·|\\?).*)");
		Matcher m = p.matcher(text);
		if(m.matches()){
			if(m.group(1).trim().compareTo("")!= 0){
				addElement("authority", m.group(1).trim(), treatment);
				try {
					vtDbA.add2AuthorTable(m.group(1).trim());
				} catch (ParsingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
				}
				//System.out.println("authority:"+m.group(1).trim());
			}
			text = m.group(2).trim();
		}*/
		//save the segment after ?or ?for later
		/*String ending = "";
		int pos = text.lastIndexOf('.');
		if(pos < 0){
			pos = text.lastIndexOf('?');
		}
		if (pos != -1) {
			ending = text.substring(pos + 1).trim();
			text = text.substring(0, pos+1);
		}*/
		
		//derivation: deal with this first to remove [] and avoid pub-year match in []
		Pattern p = Pattern.compile("(.*?)(\\[.*?\\]$)");
		Matcher m = p.matcher(text);
		if(m.matches()){
			if(m.group(2).trim().compareTo("")!= 0){
				addElement("etymology", m.group(2).trim(), treatment);
				if(debug) System.out.println("etymology:"+m.group(2).trim());
			}
			text = m.group(1).trim();
		}
		
		//place of publication 
		//Pattern p = Pattern.compile("(.* [12]\\d\\d\\d|.*(?=Â·)|.*(?=.))(.*)"); //TODO: a better fix is needed Brittonia 28: 427, fig. 1.  1977   ?  Yellow spinecape [For George Jones Goodman, 1904-1999
		p = Pattern.compile("(.* [12]\\d\\d\\d)($|,|\\.| +)(.*)"); //TODO: a better fix is needed Brittonia 28: 427, fig. 1.  1977   ?  Yellow spinecape [For George Jones Goodman, 1904-1999
		m = p.matcher(text);
		if(m.matches()){
			String pp = m.group(1).replaceFirst("^\\s*[,\\.]", "").trim();			
			extractPublicationPlace(treatment, pp); //pp may be "Sp. Pl. 1: 480.  1753; Gen. Pl. ed. 5, 215.  1754"
			text = m.group(3).trim();
		}

		// conserved
		String conserved="name conserved";
		int	pos = text.indexOf(conserved);
		if(pos < 0){
			conserved="name proposed for conservation";
			pos = text.indexOf(conserved);
		}
		if(pos < 0){
			conserved="nom. cons.";
			pos = text.indexOf(conserved);
		}
		if (pos != -1) {
			//String conserved = text.substring(pos).trim();
			text = text.replace(conserved, "").trim();
			//conserved = conserved.replaceFirst("^\\s*[,;\\.]", "");
			addElement("conserved", conserved, treatment);
			if(debug) System.out.println("conserved:"+conserved);
			
			// trim the text
			//int p1 = text.lastIndexOf(',', pos);
			//text = text.substring(0, p1);
		}

		//past_name
		p = Pattern.compile("\\((?:as )?(.*?)\\)(.*)");
		m = p.matcher(text);
		if(m.matches()){
			if(m.group(1).trim().compareTo("")!= 0){
				addElement("past_name", m.group(1).trim(), treatment);
				if(debug) System.out.println("past_name:"+m.group(1).trim());
			}
			text = m.group(2).trim();
		}

		//common name
		p = Pattern.compile("(.*?)[•·](.*?)(\\[.*|$)");
		m = p.matcher(text);
		if(m.matches()){
			if(m.group(2).trim().compareTo("")!= 0){
				String[] commonnames = m.group(2).trim().split("\\s*,\\s*");
				for(String cname: commonnames){
					addElement("common_name", cname, treatment);
					if(debug) System.out.println("common_name:"+cname);
				}
			}
			text = (m.group(1)+" "+m.group(3)).trim();
		}

		// format mark, common name, derivation
		/*{
			//int pos = text.lastIndexOf('?);
			//if(pos < 0){
			//	pos = text.lastIndexOf('?);
			//}
			if (ending.compareTo("") != 0) {
				//String ending = text.substring(pos + 1).trim();
				String[] results = ending.split("\\[");

				String commonName = results[0].trim();
				addElement("common_name", commonName, treatment);
				//System.out.println("common_name:"+commonName);

				if (results.length > 1) {
					String derivation = results[1].trim();
					derivation = derivation.substring(0,
							derivation.length() - 1); // remove the last ']'
					addElement("derivation", derivation, treatment);
					//System.out.println("derivation:"+derivation);
				}
				
				//text = text.substring(0, pos).trim();
			}
		}*/
		

		if(text.trim().matches(".*?\\w+.*")){
			if(debug) System.out.println((index+1)+"unparsed: "+text);
			addElement("unparsed", text, treatment);
			File xml = new File(Registry.TargetDirectory,
					ApplicationUtilities.getProperty("TRANSFORMED") + "/" + (index+1) + ".xml");
			listener.info("unparsed: "+text, xml.getPath());
			//errors.put((index+1)+"","still left: "+text);
		}
		return namerank.replace("_name", "");
	}


	/**
	 * family, genus, species has authority
	 * lower ranked taxon have authorities in names themselves
	 * 
	 * Cactaceae Jussieu subfam. O puntioideae Burnett
	 * @param name
	 * @return
	 */
	private String[] getNameAuthority(String name) {
		String[] nameinfo = new String[2];
		if(name.matches(".*?\\b(subfam|var|subgen|subg|subsp|ser|tribe|sect|subsect)\\b.*")){
			nameinfo[0] = name;
			nameinfo[1] = "";
			return nameinfo;
		}
		//family
		Pattern p = Pattern.compile("^([a-z]*?ceae)(\\b.*)", Pattern.CASE_INSENSITIVE);
		Matcher m = p.matcher(name);
		if(m.matches()){
			nameinfo[0] = m.group(1).replaceAll("\\s", "").trim(); //in case an extra space is there
			nameinfo[1] = m.group(2).trim();
			return nameinfo;
		}
		//genus
		p = Pattern.compile("^([A-Z][A-Z].*?)(\\b.*)"); 
		m = p.matcher(name);
		if(m.matches()){
			nameinfo[0] = m.group(1).replaceAll("\\s", "").trim();
			nameinfo[1] = m.group(2).trim();
			return nameinfo;
		}
		//species
		p = Pattern.compile("^([A-Z].*?)\\s+([(A-Z].*)");
		m = p.matcher(name);
		if(m.matches()){
			nameinfo[0] = m.group(1).trim();
			nameinfo[1] = m.group(2).trim();
			return nameinfo;
		}
		
		
		return nameinfo;
	}


	private void extractPublicationPlace(Element treatment, String pp) {
		pp = pp.replaceFirst("^\\s*,", "").trim();
		String pub="";
		String pip="";
		String[] pps = pp.split(";");
		for(String apub: pps){
			String place_in_publication="(.*?)(\\d.*?)";
			Matcher pubm=Pattern.compile(place_in_publication).matcher(apub);
			if(pubm.matches()){
				pub=pubm.group(1).trim();
				pip=pubm.group(2).trim();
			}
						
			Element placeOfPub=new Element("place_of_publication");
			addElement("publication_title",pub,placeOfPub);
			addElement("place_in_publication",pip,placeOfPub);
			treatment.addContent(placeOfPub);
			if(debug) System.out.println("publication_title:"+pub);
			if(debug) System.out.println("place_in_publication:"+pip);
			
			try {
				vtDbA.add2PublicationTable(pub);
			} catch (ParsingException e) {
				e.printStackTrace();
				LOGGER.error("Couldn't perform parsing in VolumeTransformer:parseNameTag", e);
			} catch (SQLException e) {
				e.printStackTrace();
				LOGGER.error("Database access error in VolumeTransformer:parseNameTag", e);
			}
		}
	}

	private static void addElement(String tag, String text, Element parent) {
		Element e = new Element(tag);
		e.setText(text);
		parent.addContent(e);
	}

	private void outputElementText(int count, String text, String elementname) throws ParsingException {
		//System.out.println("write file "+count+".txt");
		//elementname = "DESCRIPTIONS"
		try {
			File file = new File(Registry.TargetDirectory,
					ApplicationUtilities.getProperty(elementname) + "/" + count + ".txt");
			BufferedWriter out = new BufferedWriter(new FileWriter(file));
			out.write(text);
			out.close(); // don't forget to close the output stream!!!
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Failed to output text file in VolumeTransformer:outputDescriptionText", e);
			throw new ParsingException("Failed to output text file.", e);
		}
	}
	


}
