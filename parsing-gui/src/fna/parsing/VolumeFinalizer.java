/**
 * $Id: VolumeFinalizer.java 996 2011-10-07 01:13:47Z hong1.cui $
 */
package fna.parsing;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.jdom.Comment;
import org.jdom.Element;
import org.semanticweb.owlapi.model.OWLOntology;

import outputter.ApplicationUtilities;
//import outputter.TermEQ2IDEQ;
import outputter.XML2EQ;
import owlaccessor.OWLAccessorImpl;
import fna.charactermarkup.StanfordParser;
import fna.parsing.state.SentenceOrganStateMarker;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Set;


/**
 * @author chunshui
 */
@SuppressWarnings("unused")
public class VolumeFinalizer extends Thread {
    /*glossary established in VolumeDehyphenizer
    private String glossary;*/

    private static ProcessListener listener;
    private String dataPrefix;
    private static final Logger LOGGER = Logger.getLogger(VolumeFinalizer.class);
    private Connection conn = null;
    private String glossaryPrefix;
    private static String version="SemanticCharaParser v-alpha-0.1";
    private static boolean standalone = Boolean.valueOf(ApplicationUtilities.getProperty("finalizer.standalone"));//set to true when running only StanfordParser; false when running with GUI. 
    private static String standalonefolder = "C:/Users/updates/CharaParserTest/EQ-patterns_FixedGloss";
    private Text finalLog;
    private Display display;
    
    public VolumeFinalizer(ProcessListener listener, Text finalLog, String dataPrefix, Connection conn, String glossaryPrefix, Display display) {
        if(!standalone) VolumeFinalizer.listener = listener;
    	if(!standalone) this.finalLog = finalLog;
    	if(!standalone) this.display = display;
    	this.dataPrefix = dataPrefix;
        this.conn = conn;
        this.glossaryPrefix = glossaryPrefix;
    }
    

	
	public void run () {
		try{
			outputFinal();
		}catch(Exception e){
			this.showOutputMessage(e.toString());
			e.printStackTrace();
		}
        //check for final result errors
		//no final folder for EQ application
        /*File finalFileList= null;
        File transformedFileList = null;
        if(!standalone){
        	finalFileList = new File(Registry.TargetDirectory+"\\final\\");
        	transformedFileList = new File(Registry.TargetDirectory+"\\transformed\\");
        }
        if(standalone){
        	finalFileList = new File(this.standalonefolder+"\\final\\");
        	transformedFileList = new File(this.standalonefolder+"\\transformed\\");
        }
        if(finalFileList.list().length != transformedFileList.list().length)
        {
    		if(!standalone) this.showOutputMessage("System terminates with errors. Annotated files are not completed.");
        }*/
    }
	
    /**
     * stanford parser
     * @throws ParsingException
     */
    public void outputFinal() throws Exception {
    	if(!standalone) this.showOutputMessage("System is starting the annotation step [could take hours on large collections]...");
		
		String posedfile = Registry.TargetDirectory+System.getProperty("file.separator")+this.dataPrefix + "_"+ApplicationUtilities.getProperty("POSED");
		String parsedfile =Registry.TargetDirectory+System.getProperty("file.separator")+this.dataPrefix + "_"+ApplicationUtilities.getProperty("PARSED");
		String database = ApplicationUtilities.getProperty("database.name");
		String glosstable = this.glossaryPrefix;
		
		SentenceOrganStateMarker sosm = new SentenceOrganStateMarker(conn, this.dataPrefix, glosstable, true, null, null);
		if(!standalone) this.showOutputMessage("System is pre-tagging sentences...");
		sosm.markSentences();
		StanfordParser sp = new StanfordParser(posedfile, parsedfile, database, this.dataPrefix,glosstable, false);
		if(!standalone) this.showOutputMessage("System is POS-tagging sentences...");
		sp.POSTagging();
		if(!standalone) this.showOutputMessage("System is syntactic-parsing sentences...");		
		sp.parsing();
		if(!standalone) this.showOutputMessage("System is annotating sentences...");
		sp.extracting();
		String xmldir = Registry.TargetDirectory+System.getProperty("file.separator")+"final"+System.getProperty("file.separator");
		String outputtable = this.dataPrefix+"_xml2eq";
		//String benchmarktable = "internalworkbench";
		if(!standalone) this.showOutputMessage("System is transforming EQ statements...");
		
		
		String ontodir = ApplicationUtilities.getProperty("ontology.dir");
		String uberon = ontodir+System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.uberon")+".owl";
		String bspo = ontodir+System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.bspo")+".owl";
		String pato = ontodir+System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.pato")+".owl";
		String spatialtermtable = ApplicationUtilities.getProperty("uniquespatialterms");
		XML2EQ x2e = new XML2EQ(xmldir, database, outputtable, uberon, bspo, pato, spatialtermtable, glosstable);
		x2e.outputEQs();

		//Appending new date to the csv and txt output - Hariharan task1
		Date d = new Date();
		String time = new Timestamp(d.getTime())+"";
		String csv = (Registry.TargetDirectory+System.getProperty("file.separator")+dataPrefix+"_"+time.replaceAll("[:-]","_")+"_EQ.csv").replaceAll("\\\\+", "/");
		String txt = (Registry.TargetDirectory+System.getProperty("file.separator")+dataPrefix+"_"+time.replaceAll("[:-]","_")+"_versions.txt").replaceAll("\\\\+", "/");
		writeCSVandVersionFiles(outputtable, csv, txt);

		if(!standalone){
			this.showOutputMessage("Operations completed.");
			this.showOutputMessage("Check result file in the database table: "+outputtable+" and in the files: "+csv+" and "+txt);
			LOGGER.debug("Check result file in the database table: "+outputtable+" and in the files: "+csv+" and "+txt);
		}
	}



	private void writeCSVandVersionFiles(String outputtable, String csv,
			String txt) throws SQLException {
		//output a csv file
		File csvfile = new File(csv);
		if(csvfile.exists()) csvfile.delete();
		String fieldlist ="source, characterID, characterlabel, stateID, statelabel, entity, entitylabel, entityid, " +
				"quality, qualitylabel, qualityid, relatedentity, relatedentitylabel, relatedentityid, " +
				"unontologizedentity, unontologizedquality, unontologizedrelatedentity";
		String header = "'"+fieldlist.replaceAll("\\s*,\\s*", "','")+"'";
		String export = "(SELECT "+header+ ") UNION (SELECT "+fieldlist+ " From "+ outputtable+
				" INTO OUTFILE '"+csv+"' FIELDS TERMINATED BY ',' "+
				" OPTIONALLY ENCLOSED BY '\\\"' "+
				" ESCAPED BY '\\\\' LINES TERMINATED BY '\\n')";
		/*String export = "SELECT * "+
				" INTO OUTFILE '"+csv+"' FIELDS TERMINATED BY ',' "+
				" OPTIONALLY ENCLOSED BY '\\\"' "+
				" ESCAPED BY '\\\\' LINES TERMINATED BY '\\n' FROM ("+
				" SELECT "+header+ " UNION SELECT "+fieldlist+ " From "+ outputtable+") a";	*/
		
		/*(SELECT 'source','characterID','characterlabel','stateID','statelabel','entity','entitylabel','entityid','quality','qualitylabel','qualityid','relatedentity','relatedentitylabel','relatedentityid','unontologizedentity','unontologizedquality','unontologizedrelatedentity')
		UNION 
		(SELECT source,characterID,characterlabel,stateID,statelabel,entity,entitylabel,entityid,quality,qualitylabel,qualityid,relatedentity,relatedentitylabel,relatedentityid,unontologizedentity,unontologizedquality,unontologizedrelatedentity 
		FROM test_xml2eq
		INTO OUTFILE 'C:/Users/updates/Desktop/SemanticCharaParser/testdataset/target/eq.csv' 
		FIELDS TERMINATED BY ',' 
		OPTIONALLY ENCLOSED BY '"' 
		ESCAPED BY '\\' 
		LINES TERMINATED BY '\r\n'); */
		Statement stmt = null;
		try{
			stmt = conn.createStatement();
			stmt.execute(export);
		}catch(Exception e){
			e.printStackTrace();
			LOGGER.error("Error: Export EQ to csv format failed "+ export);
			LOGGER.error("MySQL exception ", e);
		}finally{
			if(stmt!=null) stmt.close();
			if(conn!=null) conn.close();
		}
		
		//output the version file with charaparser and ontology versions
		StringBuffer versions = new StringBuffer();

		for(OWLAccessorImpl api: XML2EQ.ontoutil.OWLqualityOntoAPIs)
		{
			OWLOntology temp=null;
			String S[]=api.getSource().split("[\\\\.]");
			versions.append((S[S.length-2].toUpperCase())+" version:");
			temp = api.getManager().getOntologies().iterator().next();
			versions.append((temp.getOntologyID().getVersionIRI())+System.getProperty("line.separator"));
		}

		for(OWLAccessorImpl api1: XML2EQ.ontoutil.OWLentityOntoAPIs)
		{
			OWLOntology temp=null;
			String S[]=api1.getSource().split("[\\\\.]");
			versions.append((S[S.length-2].toUpperCase())+" version:");
			Set<OWLOntology> ontologies = api1.getManager().getOntologies();
			for(OWLOntology ontology:ontologies)
			{
				if(ontology.getOntologyID().getVersionIRI()!=null){
					temp = ontology;
					break;
				}
				/*if(ontology.toString().contains(S[S.length-2]))
						{
							temp = ontology;
							break;
						}*/
			}
			versions.append(temp!=null?(temp.getOntologyID().getVersionIRI()):"");
			versions.append(System.getProperty("line.separator"));
		}
		versions.append("Semantic CharaParser version:"+ version+System.getProperty("line.separator"));
		//write versions to the txt file
		try {
			File file = new File(txt);
			if (!file.exists()) {
				file.createNewFile();
			}
 
			FileWriter fw = new FileWriter(file.getAbsoluteFile());
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(versions.toString());
			bw.close(); 
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("IO exception ", e);
		}
	}

	public static void outputFinalXML(Element root, String fileindex, String targetstring) {
		File target = null;
		if(!standalone) target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty(targetstring));
		if(standalone) target = new File(standalonefolder+System.getProperty("file.separator")+"target"+System.getProperty("file.separator")+"final");
		File result = new File(target, fileindex + ".xml");
		Comment comment = new Comment("produced by "+VolumeFinalizer.version+System.getProperty("line.separator"));
		//Comment comment = null;
		ParsingUtil.outputXML(root, result, comment);
		if(!standalone) listener.info("" + fileindex, result.getPath(), "");//TODO: test 3/19/10 
	}


    
    
	protected void showOutputMessage(final String message) {
		display.syncExec(new Runnable() {
			public void run() {
				finalLog.append(message+"\n");
			}
		});
	}
	 
	
	/**
	 * create an XML file named with thisCharaID
	 * @param thisCharaID:Buckup_1998.xml_088683b8-4718-48de-ad0e-eb1de9c58eb6
	 * @return
	 */
	public static Element createCharacterHolder(String thisCharaID) {
		String pdf = thisCharaID.replaceFirst("\\.xml.*", "");
		String cid = thisCharaID.replaceFirst(".*\\.xml_", ""); 
		Element root = new Element("character_unit");
		root.setAttribute("source_pdf", pdf);
		root.setAttribute("character_id", cid);
		return root;
	}
	
	
    public static void main (String [] args) {
        String database="annotationevaluation";
        String username="root";
        String password="root";
        Connection conn = null;
        try{
            if(conn == null){
                Class.forName("com.mysql.jdbc.Driver");
                String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
                conn = DriverManager.getConnection(URL);
            }
        }catch(Exception e){
            e.printStackTrace();
        }
        //VolumeFinalizer vf = new VolumeFinalizer(null, "fnav19", conn, "fnaglossaryfixed");
        //vf.start();
    }







}
