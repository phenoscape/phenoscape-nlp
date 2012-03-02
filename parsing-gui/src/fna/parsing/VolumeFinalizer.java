/**
 * $Id: VolumeFinalizer.java 996 2011-10-07 01:13:47Z hong1.cui $
 */
package fna.parsing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import org.jdom.Comment;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import outputter.TermEQ2IDEQ;
import outputter.XML2EQ;

import fna.charactermarkup.StanfordParser;
import fna.parsing.state.SentenceOrganStateMarker;

/**
 * @author chunshui
 */
@SuppressWarnings({ "unchecked", "static-access" })
public class VolumeFinalizer extends Thread {
    /*glossary established in VolumeDehyphenizer
    private String glossary;*/

    private static ProcessListener listener;
    private String dataPrefix;
    private static final Logger LOGGER = Logger.getLogger(VolumeFinalizer.class);
    private Connection conn = null;
    private String glossaryPrefix;
    private static String version="$Id: VolumeFinalizer.java 996 2011-10-07 01:13:47Z hong1.cui $";
    private static boolean standalone = true;
    private static String standalonefolder = "C:\\Documents and Settings\\Hong Updates\\Desktop\\Australia\\phenoscape-fish-source";
    private Text finalLog;
    private Display display;
    
    public VolumeFinalizer(ProcessListener listener, Text finalLog, String dataPrefix, Connection conn, String glossaryPrefix, Display display) {
        if(!standalone) this.listener = listener;
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
		
		String posedfile = Registry.TargetDirectory+"/"+this.dataPrefix + "_"+ApplicationUtilities.getProperty("POSED");
		String parsedfile =Registry.TargetDirectory+"/"+this.dataPrefix + "_"+ApplicationUtilities.getProperty("PARSED");
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
		if(!standalone) this.showOutputMessage("System is generating term-based EQ statements...");
		String xmldir = Registry.TargetDirectory+"\\final\\";;
		String outputtable = this.dataPrefix+"_xml2eq";
		String benchmarktable = "internalworkbench";
		XML2EQ x2e = new XML2EQ(xmldir, database, outputtable, benchmarktable, dataPrefix, glosstable);
		x2e.outputEQs();
		if(!standalone) this.showOutputMessage("System is transforming EQ statements...");
		TermEQ2IDEQ t2id = new TermEQ2IDEQ(database, outputtable);
		if(!standalone) this.showOutputMessage("Operations completed. Check results in "+database+" database.");
	}

	public static void outputFinalXML(Element root, String fileindex, String targetstring) {
		File target = null;
		if(!standalone) target = new File(Registry.TargetDirectory, ApplicationUtilities.getProperty(targetstring));
		if(standalone) target = new File(standalonefolder+"\\target\\final");
		File result = new File(target, fileindex + ".xml");
		Comment comment = new Comment("produced by "+VolumeFinalizer.version+System.getProperty("line.separator"));
		//Comment comment = null;
		ParsingUtil.outputXML(root, result, comment);
		if(!standalone) listener.info("" + fileindex, result.getPath(), "");//TODO: test 3/19/10 
	}


    protected void resetOutputMessage() {
		display.syncExec(new Runnable() {
			public void run() {
				finalLog.setText("");
			}
		});
	}
    
	protected void showOutputMessage(final String message) {
		display.syncExec(new Runnable() {
			public void run() {
				finalLog.append(message+"\n");
			}
		});
	}
	 
	protected void popupMessage(final String message){
		display.syncExec(new Runnable() {
			public void run() {
				ApplicationUtilities.showPopUpWindow(message, "Error",SWT.ERROR);
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
