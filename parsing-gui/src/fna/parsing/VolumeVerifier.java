package fna.parsing;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

/**
 * Hong. 08/04/09
 *  a) add ./text[@case=""] and fixBrokenNames
 *  b) TODO: output a report of potential errors for the user to check manually.
 * To verify the volume document format.
 * 
 * Style verify:
 * 
 * Number verify:
 * 
 * Name verify:
 * 
 * @author chunshui
 */
@SuppressWarnings({ "unchecked", "unused" })
public class VolumeVerifier extends Thread {

	private String target;

	private String conf;

	private ProcessListener listener;

	private String path;

	private int total;

	private File[] extracted;

	private TaxonIndexer ti;
	
	private String namelist = "|";
	
	private static final Logger LOGGER = Logger.getLogger(VolumeVerifier.class);
	public VolumeVerifier(ProcessListener listener) {
		this.listener = listener;

		target = Registry.TargetDirectory+System.getProperty("file.separator");
		conf = Registry.ConfigurationDirectory;

		path = target + "extracted";
	}

	public void run () {
		listener.setProgressBarVisible(true);
		verify();
		listener.setProgressBarVisible(false);
	}
	public void verify() {
		// get the extracted files list
		listener.progress(1);
		File directory = new File(path);
		extracted = directory.listFiles();
		total = extracted.length;
		listener.info("To verify files: " + extracted.length);

		listener.progress(10);
		// init the taxon index
		ti = new TaxonIndexer(conf); // TODO: add the
		// taxon index to
		// conf
		ti.build();
		listener.info("Taxon index initialized.");

		listener.progress(30);
		// verify the files
		listener.info("To verify the files");
		if (!verifyFile()) {
			listener.info("File verify failure!");
			return;
		} else {
			listener.info("File verify success!");
		}
		listener.progress(50);
		// verify the style
		listener.info("To verify the style");
		if (!verifyStyle()) {
			listener.info("Style verify failure!");
			return;
		} else {
			listener.info("Style verify success!");
		}
		
		listener.progress(70);
		// verify the number
		listener.info("To verify the number");
		if (!verifyNumber()) {
			listener.info("Number verify failure!");
			return;
		} else {
			listener.info("Number verify success!");
		}

		listener.progress(90);
		// verify the name
		listener.info("To verify the name");
		if (!verifyName()) {
			listener.info("Name verify failure!");
			return;
		} else {
			listener.info("Name verify success!");
		}

		listener.info("Volume format verify success!");
		
		// write the updated TaxonIndexer
		TaxonIndexer.saveUpdated(conf, ti);
		listener.info("Update the TaxonIndexer success!");
		listener.progress(99);
	}

	/**
	 * To verify the extracted files validity.
	 * 
	 * @return passed
	 */
	private boolean verifyFile() {
		boolean passed = true;

		for (int i = 1; i <= total; i++) {
			File file = new File(path, i + ".xml");

			if (!file.exists()) {
				listener.info("", file.getName(), "File does not exist!");
				passed = false;
			} else if (!file.isFile()) {
				listener.info("",  file.getName(), "File is not a file!");
				passed = false;
			}
		}

		return passed;
	}

	/**
	 * To verify the style of the document.
	 * 
	 * @return passed
	 */
	private boolean verifyStyle() {
		boolean passed = true;

		try {
			// load style mapping
			Properties props = new Properties();

			// read in the translation properties;
			props.load(new FileInputStream(conf + System.getProperty("file.separator")+"style-mapping.properties"));

			for (int i = 1; i <= total; i++) {
				File file = new File(path, i + ".xml");

				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element root = doc.getRootElement();

				// find all <style> tags
				List<Element> styleList = XPath.selectNodes(root,
						"/treatment/paragraph/style");

				// iterate over the <style> tags
				for (Iterator<Element> iter = styleList.iterator(); iter.hasNext();) {
					Element se = (Element) iter.next();
					String style = se.getText();
					String mapping = props.getProperty(style);

					// verify if the style has a mapping
					if (mapping == null || mapping.length() == 0) {
						listener.info("", file.getName(), "Invalid style "
								+ style);
						passed = false;
					}
				}

			}
		} catch (Exception e) {
			LOGGER.error("VolumeVerifier : verifyStyle - Exception ", e);
			throw new ParsingException(e);
		}

		return passed;
	}

	/**
	 * To verify the taxon number according to the taxon index.
	 * 
	 * If taxon index is not built yet, build it here.
	 * 
	 * @return passed True if the verify passed
	 */
	private boolean verifyNumber() {
		boolean passed = true;
		if(ti.emptyNumbers()){
			listener.info("no taxon list number to check against, build taxon numbers. ");
			fillInNumbers();
			listener.info("check passed");
			return passed;
		}
		try {
			for (int i = 1; i <= total; i++) {
				File file = new File(path, i + ".xml");

				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element root = doc.getRootElement();

				Element te = (Element) XPath.selectSingleNode(root,
						"/treatment/paragraph/text");
				String text = te.getText();

				String number = ti.getNumber(i - 1);
				if (!text.startsWith(number)) {
					// hong 6/26/08: make 12c1 and 12c.1 match
					// extract number 12c.1 from text and save it in ti
					Pattern p = Pattern.compile("(.*?[a-z])(\\d+)");
					Matcher m = p.matcher(number);
					boolean check = false;
					if (m.matches()) {
						String pt = m.group(1) + "\\.?" + m.group(2);
						p = Pattern.compile(pt);
						m = p.matcher(text);
						if (m.find()) {
							ti.addNumber(text.substring(m.start(), m.end())); //add one by one in sequence
							check = true;
						}
					}
					if (!check) {
						listener.info("", file.getName(),
								"Invalid number! Expected: " + number);
						passed = false;
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("VolumeVerifier : verifyNumber - Exception ", e);
			e.printStackTrace();
			throw new ParsingException(e);
		}

		return passed;
	}

	private void fillInNumbers(){
		try {
			for (int i = 1; i <= total; i++) {
				File file = new File(path, i + ".xml");
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element root = doc.getRootElement();
				Element te = (Element) XPath.selectSingleNode(root,
						"/treatment/paragraph/text");
				String text = "";
				if(te != null){//TODO: te should not be null, but for 295.xml JDOM screw-up
					text = te.getText();
					text = text.replaceAll("\\s[a-zA-Z].*", ""); //1. Amstersdfds 12.c Ames
				}
				if(text.matches("^\\d.*")){
					ti.addNumber(text); //add one by one in sequence
					System.out.println("add numbers :"+text+" for file "+i+".xml");
				}else{
					ti.addNumber("0"); //add one by one in sequence
					System.out.println("add numbers :"+0+" for file "+i+".xml");
				}
				
			}
		} catch (Exception e) {
			LOGGER.error("VolumeVerifier : fillInNumbers - Exception ", e);
			e.printStackTrace();
				throw new ParsingException(e);
		}
	}
	/**
	 * To verify the taxon name according to the taxon index
	 * 
	 * If taxon index is not built yet, build it here.
	 * 
	 * TODO: verify the var name
	 * 
	 * @return pass True if the verify passed
	 */
	private boolean verifyName() {
		boolean passed = true;
		if(ti.emptyNames()){
			listener.info("no taxon list name to check against, build taxon names. ");
			fillInNames();
			listener.info("check passed");
			return passed;
		}
		try {
			for (int i = 1; i <= total; i++) {
				File file = new File(path, i + ".xml");

				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element root = doc.getRootElement();


				String number = ti.getNumber(i - 1);
				String name = ti.getName(i - 1);
				
				//Element te = (Element) XPath.selectSingleNode(root,	"/treatment/paragraph/text");
				Element pe = (Element) XPath.selectSingleNode(root,	"/treatment/paragraph");
				
				String extractedname = extractName(pe, i + ".xml");
	
				
				//boolean check = (extractedname == null) || (name.indexOf(extractedname) >= 0 || extractedname.indexOf(name) >= 0);
				if (! match(name, extractedname)) {//HongCui
					passed = false;
					listener.info("", file.getName(),
							"Invalid name. Expected: " + number + "." + name); // TODO: if the logic is right after merged with hong's code?
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("VolumeVerifier : verifyName - Exception ", e);
			throw new ParsingException(e);//HongCui
		}

		return passed;
	}
	
	//if all words in name are in answer, then return true
	private boolean match (String answer, String name){
		if(name == null){
			return false;
		}
		name = name.toLowerCase();
		name = name.replaceAll("\\W", " ");
		name = name.trim();
		
		
		answer = answer.toLowerCase();
		answer = answer.replaceAll("\\W", " ");
		answer = answer.trim();
		String [] answerparts = name.split("\\s+");
	
		for(int i = 0; i<answerparts.length; i++){
			if(name.indexOf(answerparts[i]) < 0){
				return false;
			}
		}
		return true;
	}

	/**
	 * extract a name from the name paragraph element
	 * a name includes its authority
	 * @param pe
	 * @return
	 */
	private String extractName(Element pe, String filename) {//TODO: the case of "×" in v. 19, 516.xml "Agoseris ×elata"
		try{
			//concat <text> elements into one string
			StringBuffer buffer=new StringBuffer();

			List<Element> textList = XPath.selectNodes(pe, "./text");
			List<Element> additionalList = XPath.selectNodes(pe, "./text[@case='"+Registry.TribeGenusNameCase+"']");
			textList.addAll(additionalList);
			for (Iterator<Element> ti = textList.iterator(); ti.hasNext();) {
				Element wt = (Element) ti.next();
				buffer.append(wt.getText()).append(" ");
			}
			String text = buffer.toString().replaceAll("\\s+", " ").trim();
			text = text.replaceFirst("^.*?(?=[A-Z])", "").trim();
			//fix broken names: T HYRSOSTACHYS;  va r. subhispida
			text = fixBrokenNames(text);
			text = chunkPlaceOfPub(text, filename); //after this, text should only hold name information
			if(text.length() == 0){return "";} //TODO: shouldn't happen, except for 295.xml
	
			// hong 6/26/08: make 1.Pterostegia drymarioides
			// and 1. Pterostegia drymarioides Fischer ... match.
			// Extract full name string and save it in ti.
			
			//tribe: didn't need smallCaps info.
			Pattern p = Pattern
					.compile("\\b(?:subfam|var|subgen|subg|subsp|ser|tribe|subsect)[\\.\\s]+([-a-z]+)", Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(text);
			
			Pattern p1 = Pattern
			.compile("\\bsect[\\.\\s]+([-a-zA-Z]+)"); //Journal names may contain e.g. Sect. IV Hong 08/04/09
			Matcher m1 = p1.matcher(text);
			boolean check = false;
			if (m.find() || m1.find()) {// sub- names
				//Pattern p4 = Pattern.compile("(.*?\\s+|^)(\\D.*)");
				//Matcher m4 = p4.matcher(text);
				//if(m4.matches()){
					//String aname = m4.group(2);
					String aname = text;
					String laname = aname.toLowerCase();
					if(namelist.indexOf("|"+laname+"|")>=0){
						listener.info("", filename, "Repeated taxon name:"+aname+" [sub rank]");
						return aname;
						//System.out.println("::::::::::duplicate "+aname+" [sub rank]");//should not occur
					}else{
						namelist += laname+"|";
						return aname;
					}
			//	}
			
				/*Pattern p2 = Pattern.compile("\\.\\s*(\\D*?\\bsubg[\\.\\s]+[-a-z]+[\\s\\w\\.]*\\)[\\s\\w]*\\.\\s?[A-Z]+\\s*\\w+)",
						Pattern.CASE_INSENSITIVE); 
				Matcher m2 = p2.matcher(text);
				
				if (m2.find()) {
					String aname = m2.group(1);
					
					
					String laname = aname.toLowerCase();
					if(namelist.indexOf("|"+laname+"|")>=0){
						listener.info("", filename, "Repeated taxon name:"+aname+" [sub rank]");
						return aname;
						//System.out.println("::::::::::duplicate "+aname+" [sub rank]");//should not occur
					}else{
						namelist += laname+"|";
						return aname;
					}
				}
				
				Pattern p3 = Pattern.compile("\\.\\s*(\\D*?\\bsect[\\.\\s]+[-a-z]+[\\s\\w\\.]*\\)[\\s\\w]*\\.\\s?[A-Z]+\\s*\\w+)",
						Pattern.CASE_INSENSITIVE); 
				Matcher m3 = p3.matcher(text);
				
				if (m3.find()) {
					String aname = m3.group(1);
					String laname = aname.toLowerCase();
					if(namelist.indexOf("|"+laname+"|")>=0){
						listener.info("", filename, "Repeated taxon name:"+aname+" [sub rank]");
						return aname;
						//System.out.println("::::::::::duplicate "+aname+" [sub rank]");//should not occur
					}else{
						namelist += laname+"|";
						return aname;
					}
				}
			
				p = Pattern.compile("\\.\\s*(\\D*?\\b(?:subfam|var|subgen|subsp|ser|tribe)[\\.\\s]+[-a-z]+)",
						Pattern.CASE_INSENSITIVE); //Hong 08/04/09
				m = p.matcher(text);				
				if (m.find()) {
					String aname = m.group(1);
					String laname = aname.toLowerCase();
					if(namelist.indexOf("|"+laname+"|")>=0){
						listener.info("", filename, "Repeated taxon name:"+aname+" [sub rank]");
						return aname;
						//System.out.println("::::::::::duplicate "+aname+" [sub rank]");//should not occur
					}else{
						namelist += laname+"|";
						return aname;
					}
				}*/
			} else {// family, genus, species names
				/*
				 * FNA v. 5 and 19
				 *List<Element> textList2 = XPath.selectNodes(pe, "./text");
				int n = nameLine(textList2);
				String namestring = textList2.get(n).getText(); //2nd <text> holds the name
				//exception: <text>1. Artemisia aleutica</text>
				namestring = namestring.replaceAll("^\\d.*?\\s", "");
				Pattern famname = Pattern.compile("\\b([a-z]*?ceae)\\b.*", Pattern.CASE_INSENSITIVE);
				Pattern genname = Pattern.compile("^([A-Z][A-Z].*?)\\b.*"); //NOTHOCALAIS with two dots on top of last I
				m = famname.matcher(namestring);*/
				
				String namestring = text;
				namestring = namestring.replaceAll("^\\d.*?\\s+", "");
				namestring = namestring.replaceAll("^\\.\\s+", ""); // if there is a . Hong 08/04/09 e.g "4 . XXXX"
				Pattern famname = Pattern.compile("^([a-z]*?ceae)\\b.*", Pattern.CASE_INSENSITIVE);
				Pattern genname = Pattern.compile("^([A-Z][A-Z].*?)\\b.*"); //NOTHOCALAIS with two dots on top of last I
				m = famname.matcher(namestring);
				if(m.find()){
					//String aname = m.group(1);
					String aname = text;
					String laname = aname.toLowerCase();
					if(namelist.indexOf("|"+laname+"|")>=0){
						listener.info("", filename, "Repeated taxon name:"+aname+" [family rank]");
						//System.out.println("::::::::::duplicate "+aname+" [family rank]"); //should not occur
					}else{
						namelist += laname+"|";
						return aname; //family
					}
				}else{
					m = genname.matcher(namestring);
					if(m.find()){
						//String aname = m.group(1);
						String aname = text;
						String laname = aname.toLowerCase();
						if(namelist.indexOf("|"+laname+"|")>=0){
							listener.info("", filename, "Repeated taxon name:"+aname+" [genus rank]");
							//System.out.println("::::::::::duplicate "+aname+" [genus rank]"); //should not occur
						}else{
							namelist += laname+"|";
							return aname; //genus
						}
					}
				    else{
				    	//String aname = namestring;//species name
				    	//Hong: 08/04/09 take the first two words as species name
				    	//String[] tokens = namestring.trim().split("\\s+");
				    	//if(tokens.length >= 2){
					    	//String aname = tokens[0]+" "+tokens[1];
							String aname = text;
				    		String laname = aname.toLowerCase();
							if(namelist.indexOf("|"+laname+"|")>=0){
								listener.info("", filename, "Repeated taxon name:"+aname+" [species rank]");
								//System.out.println("::::::::::duplicate "+aname+" [species rank]"); //could occur
								/*String nextseg = textList2.get(n+1).getText();
								String firstword = nextseg.indexOf(" ")>0? nextseg.substring(0,nextseg.indexOf(" ")) : nextseg;
								aname += " "+firstword;
								laname = aname.toLowerCase();
								if(namelist.indexOf("|"+laname+"|")>=0){
									System.out.println("::::::::::2nd try: duplicate "+aname+" [species rank]"); //should not occur
								}else{
									namelist += laname+"|";
									return aname;
								} Hong 08/04/09*/
							}else{
								namelist += laname+"|";
								return aname;
							}
				    	//}else{
				    	//	System.out.println(namestring+" is not at [species rank]");
				    	//}
				    	
				    }
				}

				/*String fgsname = name.replaceFirst("\\bfam\\b", "").trim();
				if (text.toLowerCase().replaceFirst(
						"^" + number + "\\s*\\.\\s*", "").startsWith(
						fgsname.toLowerCase())) {
					check = true;
					ti.setName(i - 1, fgsname);
				}*/
			}
		}catch(Exception e){
			LOGGER.error("VolumeVerifier : extractName - Exception ", e);
			e.printStackTrace();
		}
	
		return null;
	}
	
	/**
	 * 
	 * @param text: 14c. Mirabilis linearis (Pursh) Heimerl var. subhispida (Heimerl) Spellenberg, Novon 12: 270. 2002
	 * @return: 14c. Mirabilis linearis (Pursh) Heimerl var. subhispida (Heimerl) Spellenberg
	 */
	private String chunkPlaceOfPub(String text, String filename) {
		//search for the comma before a number
		//Pattern p = Pattern.compile("(.*?[A-Z].*?),([^,]+?)\\d.*");
		String textcp = text;
		String journal = null;
		Pattern p = Pattern.compile("(.*?[A-Z].*),\\s+([^\\d]+)\\s+\\d.*"); //(Rydberg) Munz, Man. S. Calif. Bot., 598. 1935
		Matcher m = null;
		//problems 489.xml 6a. Dysphania R. Brown sect. Adenois (Moquin-Tandon) Mosyakin & Clemants, Ukrayins’k. Bot. Zhurn., n. s. 59: 382. 2002
		while(text.matches(".*?[A-Z].*?\\d+.*")){//4a. Echinocereus pectinatus (Scheidweiler) Engelmann var. wenigeri L. D. Benson, Cact. Succ. J. (Los Angeles) 40: 124, <=================>fig. 3. 1968 · Weniger’shedgehog,ashy white ra inbow cactus, langtry rainbow cactus
			m = p.matcher(text);
			if(m.matches()){
				text = m.group(1).trim();
				journal = m.group(2).replaceFirst(",\\s*$", "").trim();				
			}else{
				break;
			}
		}
		//post process to deal with some special cases: 
		//Boerhavia line arifolia A. Gray, Amer. J. Sci. Arts
		int in = text.indexOf(","); //suspecious
		if(in > 0){
			p = Pattern.compile(",[^\\d]+&"); //may be authority list: a, b & c
			m = p.matcher(text);
			if(!m.find()){
				p = Pattern.compile("\\([^()]*?,");//may be in ()
				m = p.matcher(text);
				if(!m.find()){// deal with this case
					text = text.substring(0, in).trim();
				}
			}
		}
		
		//in Smith ed. 
		in = text.indexOf(" in ");
		if(in > 0){
			text = text.substring(0, in).trim();
			//String rest = textcp.substring(in).trim();
		}
		
		//PHYTOLACCACEAE R. Brown • Pokeweed Family
		in = text.indexOf("·");
		if(in < 0){
			in = text.indexOf("•");
		}
		if(in > 0){
			text = text.substring(0, in).trim();
			//String rest = textcp.substring(in).trim();
		}
		
		if(!isJournal(journal)){
			listener.info("", filename, "Check taxon name:"+text);
		}

		return text;//text now holds only the name, which should not contain a number
	}
	/**
	 * check journal list
	 * @param journal
	 * @return
	 */
	private boolean isJournal(String journal) {
		/*Connection conn = null;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
			    String URL = "jdbc:mysql://localhost/journals?user=root&password=root";
				//String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select * from journals where abbreviation like'"+journal+" %'");
				if(rs.next()){
					return true;
				}
				stmt.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}*/
		return false;
	}

	public static String fixBrokenNames(String text){
		Pattern p = Pattern.compile("(.*?(?:^| ))([A-Z] )(\\w.*)");
		Matcher m = p.matcher(text);
		if(m.matches()){
			text = m.group(1)+m.group(2).trim()+m.group(3);
		}
		
		//T HYRSOSTACHYS
		p = Pattern.compile("(.*?(?:^| ))([A-Z]+ )([A-Z][A-Z].*)");
		m = p.matcher(text);
		if(m.matches()){
			text = m.group(1)+m.group(2).trim()+m.group(3);
		}
		
		p = Pattern.compile("(.*?(?:^| ))(\\d+ )(\\d+.*)");//HOng 08/04/09 "3 9 . xxxx"
		m = p.matcher(text);
		if(m.matches()){
			text = m.group(1)+m.group(2).trim()+m.group(3);
		}

		//va r. 
		//make sure any of these are not broken: subfam|var|subgen|subg|subsp|ser|tribe
		p = Pattern.compile("(.*?)\\b(s ?u ?b ?f ?a ?m|v ?a ?r|s ?u ?b ?g ?e ?n|s ?u ?b ?g|s ?u ?b ?s ?p|s ?e ?r|t ?r ?i ?b ?e)\\b(.*)");
		m = p.matcher(text);
		if(m.matches()){
			text = m.group(1)+m.group(2).replaceAll("\\s+", "")+m.group(3);
		}
		return text;
	}
	private int nameLine(List<Element> textList2) {
		// TODO Auto-generated method stub
		int size = textList2.size();
		for(int i = 0; i < size; i++){
			String l = textList2.get(i).getText().trim();
			//exception: <text>1. Artemisia aleutica</text> 
			if(l.length() > 4){return i;}
			if(!l.matches("^\\d.*") && !l.matches("\\W+")){ //the name line does not start with a number, nor does it contain only non=word characters
				return i;
			}
		}
		return 0;
	}

	/**
	 * 42000077#F=43#Caryophyllaceae[fam] #
	   50260245#SF=43a#Polycarpoideae[subfam] #
	   40035931#G=1#Drymaria[genus]#
	   06301172#1#Drymaria cordata[species] #F3 I NM W
	   50266761#1a#Drymaria cordata var cordata[variety]#F3 I W
	 * 
	 * 
	 * populate taxon index with names
	 * then concate all text in Name paragraph in one <text> element.
	 */
	private void fillInNames(){
		try {
			for (int i = 1; i <= total; i++) {
				File file = new File(path, i + ".xml");
				SAXBuilder builder = new SAXBuilder();
				Document doc = builder.build(file);
				Element root = doc.getRootElement();
				Element pe = (Element) XPath.selectSingleNode(root,
						"/treatment/paragraph"); //get the first paragraph, which is the name paragraph
				String taxonname = extractName(pe, i + ".xml");
				ti.addName(taxonname);//add one by one in sequence
				System.out.println("add name :"+taxonname+ " for file "+i+".xml");
			}
		} catch (Exception e) {
			LOGGER.error("VolumeVerifier : fillInNames - Exception ", e);
			e.printStackTrace();
				throw new ParsingException(e);
		}
	}
}
