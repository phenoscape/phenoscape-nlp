/**
 * 
 */
package fna.parsing;




/**
 * @authors prasad, hong cui,
 *
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import com.swtdesigner.SWTResourceManager;

import fna.beans.CharacterGroupBean;
import fna.beans.CoOccurrenceBean;
import fna.beans.ContextBean;
import fna.beans.TermBean;
import fna.beans.TermRoleBean;
import fna.beans.TermsDataBean;
import fna.charactermarkup.Utilities;
import fna.db.CharacterStateDBAccess;
import fna.db.MainFormDbAccessor;
import fna.db.VolumeMarkupDbAccessor;
import fna.parsing.character.GraphNode;
import fna.parsing.character.LearnedTermsReport;
import fna.parsing.character.ManipulateGraphML;
import fna.parsing.state.GraphMLOutputter;
import fna.parsing.state.StateCollectorTest;

@SuppressWarnings("unused")
public class MainForm {

	private Hashtable<String, String> categorizedtermsS = new Hashtable<String, String>();
	private Hashtable<String, String> categorizedtermsC = new Hashtable<String, String>();
	private Hashtable<String, String> categorizedtermsO = new Hashtable<String, String>();
	private ArrayList<String> inistructureterms = new ArrayList<String>();
	private ArrayList<String> inicharacterterms = new ArrayList<String>();	
	private String type4xml;
	private Combo combo_1_1_1;
	private ProgressBar markupProgressBar;
	private Table findStructureTable;
	private Table findDescriptorTable;
	static {
		//Set the Log File path
		try {
			ApplicationUtilities.setLogFilePath();
		} catch (Exception exe) {
			exe.printStackTrace();
		}

	}
	private Combo combo;
	
	private Connection conn;
	private Combo modifierListCombo;
	private Table finalizerTable;
	private Table transformationTable;
	
	private Table verificationTable;
	private Table extractionTable;
	private Table tagTable;
	//private Text targetText;
	//private Text sourceText;
	//private Text configurationText;
	private TabItem generalTabItem;
	private StyledText contextStyledText;
	private ProgressBar extractionProgressBar;
	private ProgressBar verificationProgressBar;
	private ProgressBar transformationProgressBar;
	//private ProgressBar finalizerProgressBar;
	private ProgressBar popupBar;
	
	private Combo tagListCombo;
	public static Combo dataPrefixCombo;
	public static Combo glossaryPrefixCombo;

	/* This Group belongs to Markup Tab -> Others tab*/
	//private Group termRoleGroup;
	//private Composite termRoleGroup;
	/* This ScrolledComposite to MarkUpTab -> Others tab*/
	//private ScrolledComposite scrolledComposite;
	/* This rectangle will hold the latest coordinates of the Markup -Others checkbox*/
	private static Rectangle otherChkbx = new Rectangle(10, 20, 93, 16);
	/* This rectangle will hold the latest coordinates of the Markup -Others tab term*/
	private static Rectangle otherTerm = new Rectangle(109, 20, 144, 21);
	/* This rectangle will hold the latest coordinates of the Markup - Others tab combo*/
	private static Rectangle otherCombo = new Rectangle (265, 23, 90, 16);
	/* This String array holds all the roles for the Markup/Others tab */
	private static String [] otherRoles = {"Other","Structure", "Descriptor", "Verb"};
	/* This ArrayList will hold all the group info of removed terms*/
	private static ArrayList <TermRoleBean> markUpTermRoles = new ArrayList<TermRoleBean>();
	
	private StyledText glossaryStyledText;
	public Shell shell;
	/*In Unknown removal this variable is used to remember the last tab selected*/
	private static int hashCodeOfItem = 0;
	//private boolean [] statusOfMarkUp = {false, false, false, false, false, false, false, false};
	private boolean [] statusOfMarkUp = {false, false, false, false};
	private static boolean saveFlag = false;
	private static final Logger LOGGER = Logger.getLogger(MainForm.class);
	/* document type: This will determine how many tabs to show */
	private String type = "";
	
	private MainFormDbAccessor mainDb = new MainFormDbAccessor();
	private CharacterStateDBAccess charDb = null;
	public static Text markUpPerlLog;
	/*Character Tab variables-----------------------------------------------------------------------------------*/
	/* This combo is the decision combo in Character tab */
	private static Combo comboDecision;
	/* This combo is the groups list on the Character Tab*/
	public static Combo groupsCombo;
	/* This Scrolled composite holds the termsGroup */
	private static Group termsGroup;
	/* This Scrolled Composite will hold the terms group */	
	private static ScrolledComposite termsScrolledComposite;
	/* This Group will hold all the removed terms */
	private static Group removedTermsGroup;
	/* This Scrolled Composite will hold the removed terms group */	
	private static ScrolledComposite removedScrolledComposite ;
	/* This is the standard increment for every terms row*/
	private static int standardIncrement = 30;
	/* These are the initial coordinates of term 1 group - this will keep on changing and hold the latest group
	 * Once a new group is loaded, this will be reset to initial values
	 * Initial y =
	 * */
	private static Rectangle term1 = new Rectangle(40, 10, 130, 35);
	
	/* These are the initial coordinates of term 2 group - this will keep on changing and hold the latest group
	 * Once a new group is loaded, this will be reset to initial values
	 * Initial y =
	 * */
	private static Rectangle term2 = new Rectangle(210, 10, 130, 35);
	
	/* These are the initial coordinates of deleted term 2 group - this will keep on changing and hold the latest group
	 * Once a new group is loaded, this will be reset to initial values
	 * Initial y =
	 * */
	private static Rectangle contextRadio = new Rectangle(10, 20, 20, 15);
	
	/* These are the initial coordinates of frequency label - this will keep on changing and hold the latest group
	 * Once a new group is loaded, this will be reset to initial values
	 * Initial y =
	 * */
	private static Rectangle frequencyLabel = new Rectangle(370, 20, 35, 15);
	/* This HashMap will hold all the group info temporarily*/
	private static HashMap <String, CharacterGroupBean> groupInfo = new HashMap <String, CharacterGroupBean> ();
	/* This HashMap will hold all processed groups information */
	private static TreeMap <String, String> processedGroups = new TreeMap<String, String> ();	
	/* This table is for showing contextual sentences */
	private Table contextTable;
	/* This table holed currently processed groups */
	private Table processedGroupsTable;
	/* This will hold sorted order of each group of terms */
	private boolean [] sortedBy ;
	/* This is the sort label picture */
	private Label sortLabel;
	/*this is the co-occur frequency label*/
	private Label label; 
	/* This will all the groups removed edges from the graph */
	private static HashMap<String, ArrayList<String>> removedEdges 
		= new HashMap<String, ArrayList<String>>();
	/* This variable stores the number of groups produced initially. This is needed to check whether
	 * remaining terms group can be generated.
	 */
	private static int noOfTermGroups = 0;
	/* Show information to the user after the first time pop up message while grouping the remaining terms */
	private static boolean charStatePopUp = true;
	

	String [] glossprefixes = null;
	private Text projectDirectory;
	
	private CLabel StepsToBeCompletedLbl;
	
	private CLabel label_prjDir;
	private CLabel label_dsprefix;
	private CLabel label_glossary;
	private CLabel lblForProject;
	private CLabel lblWithDatasetPrefix;
	private CLabel lblAndGlossary;
	private CLabel lblStepsToBe;
	private Text tab5desc;
	private Text text_1;
	/*Display colordisplay = new Display();

	Color red = colordisplay.getSystemColor(SWT.COLOR_RED);
	Color green = colordisplay.getSystemColor(SWT.COLOR_GREEN);*/
	Color red;
	Color white;
	Color green;
	public static Color grey;
	private Text step3_desc;
	private Text tab2desc; 	   
	private Text tab3desc;
	/*context for structure terms and descriptor terms for step 4/tab 5*/
	//private StyledText structureContextText;
	//private StyledText moreStructureContextText;
	//private StyledText descriptorContextText;
	//private StyledText moreDescriptorContextText;
	//private StyledText othersContextText;
	
	ArrayList<String> removedTags = new ArrayList<String>();// used to remove descriptors marked red
	//ArrayList<String> descriptorsToSaveList = new ArrayList<String>();// used to save the descriptors that are marked green
	//ArrayList<Integer> descriptorsToSaveIndexList = new ArrayList<Integer>();// used to save the descriptors index that are marked green
	
	private Text tab6desc;
	private Text step6_desc;
	private Text txtThisLastStep;
	private Composite termRoleMatrix4structures;
	private ScrolledComposite scrolledComposite4structures;
	private StyledText contextText4structures;
	private Composite termRoleMatrix4characters;
	private ScrolledComposite scrolledComposite4characters;
	private StyledText contextText4characters;
	private Composite termRoleMatrix4others;
	private ScrolledComposite scrolledComposite4others;
	private StyledText contextText4others;
	protected String[] categories; //used to populate decision list combo in step 6.
	private boolean unpaired;
	private Composite composite4structures;
	private Group group4structures;
	private Composite composite4characters;
	private Group group4characters;
	private Composite composite4others;
	private Group group4others;
	protected UUID lastSavedIdS = UUID.randomUUID();
	protected UUID lastSavedIdC = UUID.randomUUID();
	protected UUID lastSavedIdO = UUID.randomUUID();
	protected Composite currentTermRoleMatrix;
	
	

	
	
	//////////////////methods///////////////////////
	
	/////////////////display application window/////
	
	public static void main(String[] args) {
		launchMarker("type3");
	}
	
	public static void launchMarker(String type) {
		try {
			MainForm window = new MainForm();
			String[] info = type.split(":");
			window.type = info[0] ;
			if(window.type.compareTo("type4")==0 && info.length>1){
				window.setType4XML(info[1]);
			}
			window.open();
		} catch (Exception e) {
			LOGGER.error("Error Launching application", e);
			e.printStackTrace();
		}
	}
	
	/**
	 * Open the window
	 */
	public void open() throws Exception {
		final Display display = Display.getDefault();
	    /*DeviceData data = new DeviceData();
	    data.tracking = true;
	    final Display display = new Display(data);
	    Sleak sleak = new Sleak();
	    sleak.open();*/
	    
		 red = display.getSystemColor(SWT.COLOR_RED);
		 green = display.getSystemColor(SWT.COLOR_GREEN);
		 grey = display.getSystemColor(SWT.COLOR_GRAY);
		
		
		createContents(display);
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		if(shell.isDisposed()) {
			System.exit(0);
		}
	}

	/**
	 * Create contents of the application window
	 */
	protected void createContents(Display display) throws Exception{
		shell = new Shell(display);
		shell.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/garland_logo.gif"));
		shell.setSize(843, 614);
		shell.setLocation(200, 100);
		shell.setText(ApplicationUtilities.getProperty("application.name"));
		
		
		final TabFolder tabFolder = new TabFolder(shell, SWT.NONE);
		tabFolder.setBounds(10, 10, 803, 543);
		tabFolder.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
				System.out.println();
				
			}
			//Logic for tab access goes here
			public void widgetSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				// chk if values were loaded
				StringBuffer messageText = new StringBuffer();
				String tabName = arg0.item.toString();
				tabName = tabName.substring(tabName.indexOf("{")+1, tabName.indexOf("}"));
	
				/****** Logic for tab access goes here*******/
				//if status is true  - u can go to the next tab, else don't even think! 
				// For general tab
				//if (configurationText == null ) return;
				if(dataPrefixCombo == null) return;
				if(tabName.indexOf(ApplicationUtilities.getProperty("tab.one.name")) == -1 &&
						!statusOfMarkUp[0] && !saveFlag)  {
					// inform the user that he needs to load the information for starting mark up
					// focus back to the general tab
					checkFields(messageText, tabFolder);
					return;
				}
				if (combo.getText().equals("")) {//data prefix combo
					checkFields(messageText, tabFolder);
					return;
				}
				//show pop up to inform the user: configuration info is saved
				/*if(statusOfMarkUp[0]) {
					if(!saveFlag) {
						ApplicationUtilities.showPopUpWindow(
								ApplicationUtilities.getProperty("popup.info.prefix.save"), 
								ApplicationUtilities.getProperty("popup.header.info"), SWT.ICON_INFORMATION);
						saveFlag = true;
					}

					try {
						int option_chosen = getType(type);
						mainDb.savePrefixData(dataPrefixCombo.getText().replaceAll("-", "_").trim(),glossaryPrefixCombo.getText().trim(),option_chosen);
						
					} catch (Exception exe) {
						LOGGER.error("Error saving dataprefix", exe);
						exe.printStackTrace();
					}
				 }*/
				/* Check if the Type of document selected is Type 3 or Type 4*/
				//if (type.equals("")){
					//segmentation
					if (!statusOfMarkUp[1]) {
						if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
								&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))) {
							ApplicationUtilities.showPopUpWindow( 
									ApplicationUtilities.getProperty("popup.error.tab")+ " " +
									ApplicationUtilities.getProperty("tab.two.name"), 
									ApplicationUtilities.getProperty("popup.header.error"),
									SWT.ICON_ERROR);
							tabFolder.setSelection(1);
							tabFolder.setFocus();
							return;
						}
					}
					//verification
					if (!statusOfMarkUp[2]) {
						if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
								&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
								&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))) {
							ApplicationUtilities.showPopUpWindow(								
									ApplicationUtilities.getProperty("popup.error.tab")+ " " +
									ApplicationUtilities.getProperty("tab.three.name"), 
									ApplicationUtilities.getProperty("popup.header.error"),
									SWT.ICON_ERROR);
							tabFolder.setSelection(2);
							tabFolder.setFocus();
							return;
							
						}

					}
				//}

				//Transformation
				if (!statusOfMarkUp[3]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.four.name"))) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.four.name"), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						//if (type.equals("")){
							tabFolder.setSelection(3);
						//} else {
						//	tabFolder.setSelection(1);
						//}
						
						tabFolder.setFocus();
						return;							
					}

				}
				/*
				// Mark Up
				if (!statusOfMarkUp[4]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.four.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.five.name"))) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.five.name")
								.substring(0, ApplicationUtilities.getProperty("tab.five.name").indexOf(" ")), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						if (type.equals("")){
							tabFolder.setSelection(4);
						} else {
							tabFolder.setSelection(2);
						}
						tabFolder.setFocus();
						return;
					}
					

				}
				//Unknown Removal
				if (!statusOfMarkUp[5]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.four.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.five.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.six.name"))) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.six.name"), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						if (type.equals("")){
							tabFolder.setSelection(5);
						} else {
							tabFolder.setSelection(3);
						}
						tabFolder.setFocus();
						 
						
						return;
					}

				}
				//Finalizer
				if (!statusOfMarkUp[6]) {
					if(!tabName.equals(ApplicationUtilities.getProperty("tab.one.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.two.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.three.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.four.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.five.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.six.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.seven.name"))
							&& !tabName.equals(ApplicationUtilities.getProperty("tab.character"))
					) {
						ApplicationUtilities.showPopUpWindow(								
								ApplicationUtilities.getProperty("popup.error.tab")+ " " +
								ApplicationUtilities.getProperty("tab.seven.name"), 
								ApplicationUtilities.getProperty("popup.header.error"),
								SWT.ICON_ERROR);
						if (type.equals("")){
							tabFolder.setSelection(6);
						} else {
							tabFolder.setSelection(4);
						}
						tabFolder.setFocus();
						return;
					}

				}	
				//changed from 6 to 5
				if (statusOfMarkUp[5]) {//passed tab 6 (step5),landed on tab 7 (step6)
					if(tabName.equals(ApplicationUtilities.getProperty("tab.character"))){
						charDb = new CharacterStateDBAccess(dataPrefixCombo.getText().replaceAll("-", "_").trim(), glossaryPrefixCombo.getText().trim());
						// set the decisions combo
						categories = setCharacterTabDecisions();
						comboDecision.setItems(categories);
						comboDecision.setText(categories[0]);
						processedGroups.clear();
						// set the groups list
						setCharactertabGroups();
						// show the terms that co-occured in the first group
						loadTerms();
						//Clear context table;
						contextTable.removeAll();
						//load processed groups table;
						loadProcessedGroups();
						
						//createRemainingTermsGroup();
					}
				}
				*/

			}
			
		});
		
		/////////////////////////////////////////////////
		/////////////general tab (tab1) /////////////////
		/////////////////////////////////////////////////
		
		generalTabItem = new TabItem(tabFolder, SWT.NONE);
		generalTabItem.setText(ApplicationUtilities.getProperty("tab.one.name"));

		final Composite composite = new Composite(tabFolder, SWT.NONE);
		generalTabItem.setControl(composite);

		//initialized variables to hold three filepaths when save project				
		//targetText = new Text(composite, SWT.BORDER);
		//configurationText = new Text(composite, SWT.BORDER);
		//sourceText = new Text(composite, SWT.BORDER);
		
		final Group configurationDirectoryGroup_1_1_1 = new Group(composite, SWT.NONE);
		//configurationDirectoryGroup_1_1_1.setEnabled(false);
		//configurationDirectoryGroup_1_1_1.setBounds(20, 117, 748, 70);
		configurationDirectoryGroup_1_1_1.setText(
				ApplicationUtilities.getProperty("datasetprefix"));
		configurationDirectoryGroup_1_1_1.setVisible(false);
		// get value from the project conf and set it here
		List <String> datasetPrefixes = new ArrayList <String> (); 
		mainDb.datasetPrefixRetriever(datasetPrefixes);
		String [] prefixes = new String [datasetPrefixes.size()];
		int loopCount = 0;
		for (String s : datasetPrefixes) {
			prefixes [loopCount] = s;
			loopCount++;
		}
		List <String> glossaryPrefixes = new ArrayList <String> (); 
		
		mainDb.glossaryPrefixRetriever(glossaryPrefixes);
		glossprefixes = new String [glossaryPrefixes.size()];
		int glossCount = 0;
		for (String s : glossaryPrefixes) {
			glossprefixes [glossCount] = s;
			glossCount++;
		}
		
		CLabel lblSelectA = new CLabel(composite, SWT.NONE);
		lblSelectA.setBounds(20, 37, 208, 21);
		lblSelectA.setText(ApplicationUtilities.getProperty("labelSelectProject"));
		
		projectDirectory = new Text(composite, SWT.BORDER);
		projectDirectory.setToolTipText(ApplicationUtilities.getProperty("chooseDirectoryTooltip"));
		projectDirectory.setBounds(238, 37, 386, 21);
		
		final Button browseConfigurationButton = new Button(composite, SWT.NONE);
		browseConfigurationButton.setBounds(654, 35, 100, 23);
		browseConfigurationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				browseConfigurationDirectory(); // browse the configuration directory
			}
		});
		browseConfigurationButton.setText(ApplicationUtilities.getProperty("browseProjectBtn"));
		browseConfigurationButton.setToolTipText(ApplicationUtilities.getProperty("browseProjectTTT"));
				
		Group grpCreateANew = new Group(composite, SWT.NONE);
		grpCreateANew.setText("Create a new project :");
		grpCreateANew.setBounds(10, 10, 773, 215);
				
		final Button saveProjectButton = new Button(grpCreateANew, SWT.NONE);
		saveProjectButton.setBounds(635, 182, 100, 23);
		saveProjectButton.setText(ApplicationUtilities.getProperty("saveProjectBtn"));
		saveProjectButton.setToolTipText(ApplicationUtilities.getProperty("saveProjectTTT"));
		saveProjectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if (checkFields(new StringBuffer(), tabFolder)) {
					return;
				}
				saveProject(); 
				try {
					int option_chosen =getType(type); 
					mainDb.savePrefixData(dataPrefixCombo.getText().replaceAll("-", "_").trim(),glossaryPrefixCombo.getText().trim(),option_chosen);
					//mainDb.loadStatusOfMarkUp(statusOfMarkUp, combo.getText());
				} catch (Exception exe) {
					exe.printStackTrace();
					LOGGER.error("Error saving dataprefix", exe);
				}
				String messageHeader = ApplicationUtilities.getProperty("popup.header.info");
				String message = ApplicationUtilities.getProperty("popup.info.saved");				
				ApplicationUtilities.showPopUpWindow(message, messageHeader, SWT.ICON_INFORMATION); 	
				//saveFlag = false;
				saveFlag = true;
				statusOfMarkUp[0] = true;
			}
		});

												
		combo = new Combo(grpCreateANew, SWT.NONE);
		combo.setBounds(23, 134, 138, 23);
		combo.setToolTipText(ApplicationUtilities.getProperty("application.dataset.instruction"));
		dataPrefixCombo = combo;
		combo.setItems(prefixes);

		Combo glossaryCombo = new Combo(grpCreateANew, SWT.NONE);
		glossaryCombo.setBounds(199, 134, 138, 23);
		glossaryCombo.setItems(glossprefixes);
		glossaryPrefixCombo = glossaryCombo;
														
		CLabel label = new CLabel(grpCreateANew, SWT.NONE);
		label.setBounds(23, 82, 344, 21);
		label.setText(ApplicationUtilities.getProperty("datasetprefix"));
		combo.addModifyListener(new ModifyListener() {
			public void modifyText(final ModifyEvent me) {
				 if (combo.getText().trim().equals("")) {
					saveFlag = false;
				}
			}
		});
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				try {
					mainDb.loadStatusOfMarkUp(statusOfMarkUp, combo.getText());
				} catch (Exception exe) {
					LOGGER.error("Error in loading Status of mark Up", exe);
					exe.printStackTrace();
				}
				/* remove the deleted edges graph if a new prefix is selected*/
				removedEdges.clear();
			}
		});
		
		//hide load last project part
		/*Group grpContinueWithThe = new Group(composite, SWT.NONE);
		grpContinueWithThe.setToolTipText("Continue with the last project");
		grpContinueWithThe.setText("Continue with the last project");
		grpContinueWithThe.setBounds(20, 264, 763, 144);
						
		final Button loadProjectButton = new Button(grpContinueWithThe, SWT.NONE);
		loadProjectButton.setBounds(634, 99, 120, 23);
		loadProjectButton.setToolTipText(ApplicationUtilities.getProperty("loadLastProjectTTT"));
		loadProjectButton.setText(ApplicationUtilities.getProperty("loadLastProjectBtn"));
		loadProjectButton.addSelectionListener(new SelectionAdapter() {
			private TabFolder markupNReviewTabFolder;

			public void widgetSelected(final SelectionEvent e){
				//make all labels in this group "grpContinueWithThe" visible
				lblForProject.setVisible(true);
				lblWithDatasetPrefix.setVisible(true);
				lblAndGlossary.setVisible(true);
				label_prjDir.setVisible(true);
				label_dsprefix.setVisible(true);
				label_glossary.setVisible(true);
				lblStepsToBe.setVisible(true);
		
				loadProject();
				//step1 description
				if(tab2desc!=null){
					tab2desc.setText(ApplicationUtilities.getProperty("step1DescpPart1")+Registry.TargetDirectory.concat(ApplicationUtilities.getProperty("EXTRACTED"))+ ApplicationUtilities.getProperty("step1DescpPart2") +Registry.SourceDirectory+ApplicationUtilities.getProperty("step1DescpPart3"));
				}
				if(tab3desc!=null){
					tab3desc.setText(ApplicationUtilities.getProperty("step2DescpPart1")+Registry.TargetDirectory.concat(ApplicationUtilities.getProperty("EXTRACTED"))+ApplicationUtilities.getProperty("step2DescpPart2"));
				}
				// code for setting the text of the combo to the last accessed goes here 
				try {
					int option_chosen = getType(type);
					String t = mainDb.getLastAccessedDataSet(option_chosen);
					String prefix = t==null? "" : t;
					int index= t.indexOf("|");
					String glossaryName="";
					if(index != -1)
					{
						prefix=t.substring(0,index);
						glossaryName=t.substring(index+1);
						
					}
					MainForm.dataPrefixCombo.setText(prefix);
					label_dsprefix.setText(prefix);
					
					mainDb.loadStatusOfMarkUp(statusOfMarkUp, combo.getText());
					//mainDb.saveStatus("general", combo.getText(), true);
					statusOfMarkUp[0] = true;
					//display all remaining steps:
					StringBuffer remainingSteps= new StringBuffer();
					int i=3;
					if(type.trim().equalsIgnoreCase(""))
						i=1;
					boolean notComplete=false;
					for(;i<statusOfMarkUp.length;i++)
					{
						if(statusOfMarkUp[i])
						{
							notComplete=true;
							remainingSteps.append("Step "+i+",");
							// try loading all those steps..
							
								if(!StepsToBeCompletedLbl.getVisible())
									StepsToBeCompletedLbl.setVisible(true);
						}
						else{
						if(i==1)
							loadFileInfo(extractionTable, Registry.TargetDirectory + 
									ApplicationUtilities.getProperty("EXTRACTED"));
							if(i==2)
							loadFileInfo(verificationTable, Registry.TargetDirectory + 
									ApplicationUtilities.getProperty("EXTRACTED"));
							if(i==3)
							loadFileInfo(transformationTable, Registry.TargetDirectory + 
									ApplicationUtilities.getProperty("TRANSFORMED"));
						}		
					}
					if(!notComplete)
					{
						remainingSteps.append(ApplicationUtilities.getProperty("stepsComplete"));
						if(!StepsToBeCompletedLbl.getVisible())
							StepsToBeCompletedLbl.setVisible(true);
					}
					if(remainingSteps!=null && remainingSteps.length()>0){
					
						String remainingStepsLb =	remainingSteps.substring(0,remainingSteps.length()-1);
						StepsToBeCompletedLbl.setText(remainingStepsLb);
						
					}
					
					if(glossprefixes!=null && glossprefixes.length>0)
					MainForm.glossaryPrefixCombo.setText(glossaryName);
					label_glossary.setText(glossaryName);
					
				} catch (Exception ex) {
					LOGGER.error("Error Setting focus", ex);
					ex.printStackTrace();
				} 
				
				//load step4 here
				if(!statusOfMarkUp[4]){
					createSubtab(markupNReviewTabFolder, "structures",composite4structures,group4structures, scrolledComposite4structures, termRoleMatrix4structures, contextText4structures);
					createSubtab(markupNReviewTabFolder, "characters", composite4characters,group4characters, scrolledComposite4characters, termRoleMatrix4characters, contextText4characters);
					createSubtab(markupNReviewTabFolder, "others", composite4others,group4others, scrolledComposite4others, termRoleMatrix4others, contextText4others);
					//loadOthersTable();
					
				}
				
				if(!statusOfMarkUp[5])//unknown removal
				{
					loadTags(tabFolder);
					//should not rerun character grouping, should load results from terms table. Hong TODO 5/23/11
					//set the decisions combo
					//setCharacterTabDecisions();
					processedGroups.clear();
					//set the groups list
					setCharactertabGroups();
					// show the terms that co-occured in the first group
					loadTerms();
					//Clear context table;
					contextTable.removeAll();
					//load processed groups table;
					loadProcessedGroups();
				}
				if(!statusOfMarkUp[6])
				{
					loadFileInfo(finalizerTable, Registry.TargetDirectory + 
							ApplicationUtilities.getProperty("FINAL"));
				}
				
			}
			
			});
		
		this.lblStepsToBe = new CLabel(grpContinueWithThe, SWT.NONE);
		lblStepsToBe.setVisible(false);
		lblStepsToBe.setBounds(10, 84, 162, 21);
		lblStepsToBe.setText(ApplicationUtilities.getProperty("stepsTobeCompletedLbl"));
						
		this.StepsToBeCompletedLbl = new CLabel(grpContinueWithThe, SWT.NONE);
		this.StepsToBeCompletedLbl.setBounds(198, 84, 416, 21);
		this.StepsToBeCompletedLbl.setText("");
		this.StepsToBeCompletedLbl.setVisible(false);
		
		this.lblForProject= new CLabel(grpContinueWithThe, SWT.NONE);
		lblForProject.setBounds(10, 25, 224, 21);
		lblForProject.setText(ApplicationUtilities.getProperty("loadPrjLbl"));
		lblForProject.setVisible(false);
		
		this.label_prjDir= new CLabel(grpContinueWithThe, SWT.NONE);
		label_prjDir.setBounds(240, 25, 247, 21);
		label_prjDir.setText("");
		
		this.lblWithDatasetPrefix= new CLabel(grpContinueWithThe, SWT.NONE);
		lblWithDatasetPrefix.setBounds(10, 52, 113, 21);
		lblWithDatasetPrefix.setText(ApplicationUtilities.getProperty("datasetLbl"));
		lblWithDatasetPrefix.setVisible(false);
		
		this.label_dsprefix = new CLabel(grpContinueWithThe, SWT.NONE);
		label_dsprefix.setBounds(129, 52, 128, 21);
		label_dsprefix.setText("");
		
		this.lblAndGlossary = new CLabel(grpContinueWithThe, SWT.NONE);
		lblAndGlossary.setBounds(263, 52, 79, 21);
		lblAndGlossary.setText(ApplicationUtilities.getProperty("glossaryLbl"));
		lblAndGlossary.setVisible(false);
		
		this.label_glossary= new CLabel(grpContinueWithThe, SWT.NONE);
		label_glossary.setBounds(348, 52, 137, 21);
		label_glossary.setText("");
		*/
		
				
		
		/*****end of type1-specific tabs****/
		/*****start all type shared tabs****/

		/* Transformation Tab: step 3*/
		final TabItem transformationTabItem = new TabItem(tabFolder, SWT.NONE);
		transformationTabItem.setText(ApplicationUtilities.getProperty("tab.two.name"));
		final Composite composite_3 = new Composite(tabFolder, SWT.NONE);
		transformationTabItem.setControl(composite_3);
		
		step3_desc = new Text(composite_3, SWT.READ_ONLY | SWT.WRAP);
		step3_desc.setText(ApplicationUtilities.getProperty("step3DescpText"));
		step3_desc.setBounds(20, 10, 744, 62);
		
		transformationTable = new Table(composite_3, SWT.FULL_SELECTION | SWT.BORDER);
		transformationTable.setBounds(20, 89, 744, 369);
		transformationTable.setLinesVisible(true);
		transformationTable.setHeaderVisible(true);
		transformationTable.addMouseListener(new MouseListener () {
			public void mouseDoubleClick(MouseEvent event) {
				String filePath = Registry.TargetDirectory + 
				ApplicationUtilities.getProperty("TRANSFORMED")+ System.getProperty("file.separator") +
				transformationTable.getSelection()[0].getText(1).trim();
				if (filePath.indexOf("xml") != -1) {
					try {
						Runtime.getRuntime().exec(ApplicationUtilities.getProperty("notepad") 
								+ " \"" + filePath + "\"");
					} catch (Exception e){
						ApplicationUtilities.showPopUpWindow(ApplicationUtilities.getProperty("popup.error.msg") +
								ApplicationUtilities.getProperty("popup.editor.msg"),
								ApplicationUtilities.getProperty("popup.header.error"), 
								SWT.ERROR);
					}
				} 
			}			
			public void mouseDown(MouseEvent event) {}
			public void mouseUp(MouseEvent event) {}
		});


		final TableColumn transformationNumberColumnTableColumn_1 = new TableColumn(transformationTable, SWT.NONE);
		transformationNumberColumnTableColumn_1.setWidth(168);
		transformationNumberColumnTableColumn_1.setText("File Count");

		final TableColumn transformationNameColumnTableColumn_1 = new TableColumn(transformationTable, SWT.NONE);
		transformationNameColumnTableColumn_1.setWidth(172);
		transformationNameColumnTableColumn_1.setText("File Links");

		final Button startTransformationButton = new Button(composite_3, SWT.NONE);
		startTransformationButton.setToolTipText("Run step 3");
		startTransformationButton.setBounds(547, 464, 90, 23);
		startTransformationButton.setText(ApplicationUtilities.getProperty("step3RunBtn"));
		startTransformationButton.setToolTipText(ApplicationUtilities.getProperty("step3RunTTT"));
		startTransformationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				transformationTable.removeAll();
				ProcessListener listener = 
						new ProcessListener(transformationTable, transformationProgressBar, 
								shell.getDisplay());

					CharacterStatementsTransformer preMarkUp = 
							new CharacterStatementsTransformer4NeXML(listener, shell.getDisplay(), 
									null, new ArrayList<String>());
					preMarkUp.start();
				/*if(type.equals("")) {
					startTransformation(); // start the transformation process
				} 
				else if (type.equals("type2")) {
					startType2Transformation(); // When the doc selected is type 2
				}				
				else if (type.equals("type3")) {
					startType3Transformation(); // start pre-mark up process
				} else if (type.equals("type4")) {
					startType4Transformation(); // When the doc selected is type 4
				}*/
				
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.two.name"), combo.getText(), true);
					statusOfMarkUp[1] = true;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - transform" , exe);
					exe.printStackTrace();
				}
			}
		});
		/* Type 4 Transformation doesn't do anything other than listing source files : Doubtful*/
		//if (type.equals("type4")){
		//	startTransformationButton.setVisible(false);
		//}

		final Button clearTransformationButton = new Button(composite_3, SWT.NONE);
		clearTransformationButton.setToolTipText(ApplicationUtilities.getProperty("ClearRerunTTT"));
		clearTransformationButton.setBounds(647, 464, 110, 23);
		clearTransformationButton.setText(ApplicationUtilities.getProperty("ClearRerunBtn"));
		clearTransformationButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {/*
				clearTransformation();				
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.two.name"), combo.getText(), false);
					statusOfMarkUp[3] = false;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - transform" , exe);
					exe.printStackTrace();
				}
			*/
				//commented above code to make it re-run

				transformationTable.removeAll();
				ProcessListener listener = 
						new ProcessListener(transformationTable, transformationProgressBar, 
								shell.getDisplay());

					CharacterStatementsTransformer preMarkUp = 
							new CharacterStatementsTransformer4NeXML(listener, shell.getDisplay(), 
									null, new ArrayList<String>());
					preMarkUp.start();
				/*if(type.equals("")) {
					startTransformation(); // start the transformation process
				} 
				else if (type.equals("type2")) {
					startType2Transformation(); // When the doc selected is type 4
				}				
				else if (type.equals("type3")) {
					startType3Transformation(); // start pre-mark up process
				} else if (type.equals("type4")) {
					startType4Transformation(); // When the doc selected is type 4
				}*/
				try {					
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.two.name"), combo.getText(), true);
					statusOfMarkUp[1] = true;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - transform" , exe);
					exe.printStackTrace();
				}
			}
		});

		transformationProgressBar = new ProgressBar(composite_3, SWT.NONE);
		transformationProgressBar.setVisible(false);
		transformationProgressBar.setBounds(10, 464, 524, 17);
		
		/*Button button = new Button(composite_3, SWT.NONE);
		button.setBounds(577, 464, 60, 23);
		button.setText("Load");*/

		
		/*CLabel label_2 = new CLabel(composite_3, SWT.NONE);
		label_2.setBounds(105, 38, 61, 21);
		label_2.setText("New Label");
		
		CLabel label_3 = new CLabel(composite_3, SWT.NONE);
		label_3.setBounds(105, 38, 238, 23);
		label_3.setText("New Label");*/
		/*button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				loadFileInfo(transformationTable, Registry.TargetDirectory + 
						ApplicationUtilities.getProperty("TRANSFORMED"));
			}
		});
		 */
		
		/* Mark Up Tab: this is fifth tab but is step 4 in annotation*/
		/*contains 5 subtabs: 1. Run unsupervised learning perl code */
		/*2-5: term curation subtabs, filtered by the glossarytable*/
		/*2: "Find Structures" from sentence tags, wordpos (p,s) table; save to WordRole */
		/*3: "Find Descriptors" from wordpos (b), save to WordRole*/
		/*4: "Find More Structures" from allwords (dhword), save to WordRole*/
		/*5: "Find More Descriptors" from unknownwords(dhword), save to WordRole*/
		final TabItem markupTabItem = new TabItem(tabFolder, SWT.NONE);
		markupTabItem.setText(ApplicationUtilities.getProperty("tab.three.name"));

		final Composite composite_4 = new Composite(tabFolder, SWT.NONE);
		markupTabItem.setControl(composite_4);

		//final TabFolder markupNReviewTabFolder = new TabFolder(composite_4, SWT.NONE);
		final TabFolder markupNReviewTabFolder = new TabFolder(composite_4, SWT.NONE);
		markupNReviewTabFolder.setBounds(0, 0, 795, 515);
		
		markupNReviewTabFolder.addSelectionListener(new SelectionListener() {
			public void widgetDefaultSelected(SelectionEvent arg0) {
				System.out.println();
				
			}
			//Logic for subtab access goes here
			public void widgetSelected(SelectionEvent arg0) {
				String tabName = arg0.item.toString();
				tabName = tabName.substring(tabName.indexOf("{")+1, tabName.indexOf("}")).trim();
				
				//manage termRoleMatrix
				if(currentTermRoleMatrix!=null){
					//dispose current
					Control[] children = currentTermRoleMatrix.getChildren();
					for(Control child: children) child.dispose();
					//load termRoleMatrix for the new selection
					if(tabName.compareTo(ApplicationUtilities.getProperty("tab.three.one.name"))==0){
						reLoadTermArea(termRoleMatrix4structures, scrolledComposite4structures, contextText4structures, "structures");
					}else if(tabName.compareTo(ApplicationUtilities.getProperty("tab.three.two.name"))==0){
						reLoadTermArea(termRoleMatrix4characters, scrolledComposite4characters, contextText4characters, "characters");
					}else if(tabName.compareTo(ApplicationUtilities.getProperty("tab.three.three.name"))==0){
						reLoadTermArea(termRoleMatrix4others, scrolledComposite4others, contextText4characters, "others");
					}
					
				}
				if(tabName.compareTo(ApplicationUtilities.getProperty("tab.three.one.name"))==0) currentTermRoleMatrix = termRoleMatrix4structures;
				if(tabName.compareTo(ApplicationUtilities.getProperty("tab.three.two.name"))==0) currentTermRoleMatrix = termRoleMatrix4characters;
				if(tabName.compareTo(ApplicationUtilities.getProperty("tab.three.three.name"))==0) currentTermRoleMatrix = termRoleMatrix4others;

			}
		});
		
		TabItem tbtmPerlProgram = new TabItem(markupNReviewTabFolder, SWT.NONE);
		tbtmPerlProgram.setText("Run Perl Program");
		
		Composite composite_9 = new Composite(markupNReviewTabFolder, SWT.NONE);
		tbtmPerlProgram.setControl(composite_9);
		
		tab5desc = new Text(composite_9, SWT.READ_ONLY | SWT.WRAP);
		tab5desc.setToolTipText(ApplicationUtilities.getProperty("step4Descp"));
		tab5desc.setText(ApplicationUtilities.getProperty("step4Descp"));
		tab5desc.setEditable(false);
		tab5desc.setBounds(10, 10, 744, 39);
		
		/*"run perl" subtab*/
		markUpPerlLog = new Text(composite_9, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
		markUpPerlLog.setBounds(10, 103, 744, 250);
		markUpPerlLog.setEnabled(true);

		markupProgressBar = new ProgressBar(composite_9, SWT.NONE);
		markupProgressBar.setBounds(7, 434, 432, 17);
		markupProgressBar.setVisible(false);

		final Button startMarkupButton_1 = new Button(composite_9, SWT.NONE);
		startMarkupButton_1.setToolTipText(ApplicationUtilities.getProperty("step4RunTTT"));
		startMarkupButton_1.setBounds(545, 434, 91, 23);
		startMarkupButton_1.setText(ApplicationUtilities.getProperty("step4RunBtn"));
		startMarkupButton_1.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {
				if(!termRoleMatrix4structures.isDisposed()){
					termRoleMatrix4structures.setVisible(false);
					termRoleMatrix4characters.setVisible(false);
					termRoleMatrix4others.setVisible(false);
				}
				startMarkup();
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.three.name"), combo.getText(), true);
					statusOfMarkUp[2] = true;
				} catch (Exception exe) {
					LOGGER.error("Couldn't save status - markup" , exe);
					exe.printStackTrace();
				}
				
			}
		});
		
		/*3 subtabs*/
		composite4structures = new Composite(markupNReviewTabFolder, SWT.NONE);
		group4structures = new Group(composite4structures, SWT.NONE);
		scrolledComposite4structures = new ScrolledComposite(group4structures, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		termRoleMatrix4structures = new Composite(scrolledComposite4structures, SWT.NONE);
		contextText4structures = new StyledText(composite4structures, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL|SWT.H_SCROLL);
		
		composite4characters = new Composite(markupNReviewTabFolder, SWT.NONE);
		group4characters = new Group(composite4characters, SWT.NONE);
		scrolledComposite4characters = new ScrolledComposite(group4characters, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		termRoleMatrix4characters = new Composite(scrolledComposite4characters, SWT.NONE);
		contextText4characters = new StyledText(composite4characters, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL|SWT.H_SCROLL);

		composite4others = new Composite(markupNReviewTabFolder, SWT.NONE);
		group4others = new Group(composite4others, SWT.NONE);
		scrolledComposite4others = new ScrolledComposite(group4others, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		termRoleMatrix4others = new Composite(scrolledComposite4others, SWT.NONE);
		contextText4others = new StyledText(composite4others, SWT.WRAP | SWT.BORDER | SWT.V_SCROLL|SWT.H_SCROLL);
		
		createSubtab(markupNReviewTabFolder, "structures",composite4structures,group4structures, scrolledComposite4structures, termRoleMatrix4structures, contextText4structures);
		createSubtab(markupNReviewTabFolder, "characters", composite4characters,group4characters, scrolledComposite4characters, termRoleMatrix4characters, contextText4characters);
		createSubtab(markupNReviewTabFolder, "others", composite4others,group4others, scrolledComposite4others, termRoleMatrix4others, contextText4others);

		/*structure subtab*/
		/*final TabItem tbtmFindStructureNames = new TabItem(markupNReviewTabFolder, SWT.NONE);
		tbtmFindStructureNames.setText("Remove Non-Structure Terms");

		final Composite composite_2 = new Composite(markupNReviewTabFolder, SWT.NONE);
		tbtmFindStructureNames.setControl(composite_2);

		tab5desc = new Text(composite_2, SWT.READ_ONLY | SWT.WRAP);
		tab5desc.setToolTipText(ApplicationUtilities.getProperty("step4Descp1"));
		tab5desc.setText(ApplicationUtilities.getProperty("step4Descp1"));
		tab5desc.setEditable(false);
		tab5desc.setBounds(10, 10, 744, 39);
		
		findStructureTable = new Table(composite_2, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION );
		final TableColumn findStructureTableColumn_Count = new TableColumn(findStructureTable, SWT.NONE);
		findStructureTableColumn_Count.setWidth(81);
		findStructureTableColumn_Count.setText("Count");
		final TableColumn findStructureTableColumn_Words = new TableColumn(findStructureTable, SWT.NONE);
		findStructureTableColumn_Words.setWidth(658);
		findStructureTableColumn_Words.setText("Structure Name");
		
		findStructureTable.setBounds(10, 74, 744, 209);
		findStructureTable.setLinesVisible(true);
		findStructureTable.setHeaderVisible(true);
		findStructureTable.addListener(SWT.Selection, new Listener() {//display context for selected structure term
		      public void handleEvent(Event e) {		    	 
		    	 TableItem[] selItem= findStructureTable.getSelection();
		    	 for (TableItem item : selItem) {
			  				String str = item.getText(1);
			  				try {
			  					structureContextText.setText("");
								mainDb.getContextData(str, structureContextText);
							} catch (ParsingException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							} catch (SQLException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
			  			
		    	 }
		      }
		    });

		structureContextText = new StyledText(composite_2, SWT.BORDER | SWT.V_SCROLL|SWT.H_SCROLL);
		structureContextText.setEditable(false);
		structureContextText.setDoubleClickEnabled(false);
		structureContextText.setBounds(10, 299, 744, 114);
		*/

	    /*Load button*/
		/*Button tab5_findstructure_loadFromLastTimeButton = new Button(composite_2, SWT.NONE);
		tab5_findstructure_loadFromLastTimeButton.setToolTipText(ApplicationUtilities.getProperty("termCurationLoadTTT"));
		tab5_findstructure_loadFromLastTimeButton.setBounds(171, 433, 155, 25);
		tab5_findstructure_loadFromLastTimeButton.setText(ApplicationUtilities.getProperty("termCurationLoad"));
		tab5_findstructure_loadFromLastTimeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//call functions to load values for all tables in order			
				int c = loadFindStructureTable(); //use tag in sentence table
				if(c==0){
					String messageHeader = ApplicationUtilities.getProperty("popup.header.info");
					String message = ApplicationUtilities.getProperty("popup.load.nodata");				
					ApplicationUtilities.showPopUpWindow(message, messageHeader, SWT.ICON_INFORMATION);
				}
			}			
		});*/
	    
		/*"mark as bad" button*/
		/*Button tab5_findstructure_MarkAsGoodButton = new Button(composite_2, SWT.NONE);
		tab5_findstructure_MarkAsGoodButton.setToolTipText(ApplicationUtilities.getProperty("termCurationMarkBadTTT"));
		tab5_findstructure_MarkAsGoodButton.setText(ApplicationUtilities.getProperty("termCurationMarkBad"));
		tab5_findstructure_MarkAsGoodButton.setBounds(342, 433, 132, 25);
		tab5_findstructure_MarkAsGoodButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int i=0;
				for (TableItem item : findStructureTable.getItems()) {
					if (item.getChecked()) {	
						if(item.getBackground(1).equals(red)){
							item.setBackground(0,white);
							item.setBackground(1,white);
						}else{
							item.setBackground(0,red);
							item.setBackground(1,red);
						}
						item.setChecked(false);
					}
					i+=1;
				}
				
			}
		});*/
		
		/*"mark others as good" button*/
		/*final Button tab5_findstructure_MarkAsBadButton = new Button(composite_2, SWT.NONE);
		tab5_findstructure_MarkAsBadButton.setToolTipText(ApplicationUtilities.getProperty("termCurationMarkOtherGoodTTT"));
		tab5_findstructure_MarkAsBadButton.setBounds(479, 433, 140, 25);
		tab5_findstructure_MarkAsBadButton.setText(ApplicationUtilities.getProperty("termCurationMarkOthersGood"));
	    tab5_findstructure_MarkAsBadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int i=0;
				for (TableItem item : findStructureTable.getItems()) {
					if (!item.getBackground(1).equals(red)) {				
						findStructureTable.getItem(i).setBackground(0,green);
						findStructureTable.getItem(i).setBackground(1,green);
						item.setChecked(false);
					}
					i+=1;
				}
				//removeBadStructuresFromTable(findStructureTable);
				//try { You don't need to run markup again ater removal!
				//	mainDb.saveStatus(ApplicationUtilities.getProperty("tab.three.name"), combo.getText(), false);
				//	statusOfMarkUp[2] = false;
				//} catch (Exception exe) {
				//	LOGGER.error("Couldnt save status - markup" , exe);
				//} 				
			}
		});*/
		
		
	    /*"Save" button*/
	    /*final Button tab5_findstructure_SaveButton = new Button(composite_2, SWT.NONE);
		tab5_findstructure_SaveButton.setText(ApplicationUtilities.getProperty("termCurationSave"));// save good structure names here.
		tab5_findstructure_SaveButton.setBounds(622, 433, 132, 25);
		tab5_findstructure_SaveButton.setToolTipText(ApplicationUtilities.getProperty("termCurationSaveTTT"));
		tab5_findstructure_SaveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//saveStructureTerms(markupTable_Descriptor, Registry.MARKUP_ROLE_O);
				saveStructureTerms(findStructureTable, Registry.MARKUP_ROLE_O);
				structureContextText.setText("");
			}
		});*/

		/*"find descriptors" subtab*/
		/*final TabItem tbtmFindDescriptors = new TabItem(markupNReviewTabFolder, SWT.NONE);
		tbtmFindDescriptors.setText("Remove Non-Descriptor terms");

		final Composite composite_7 = new Composite(markupNReviewTabFolder, SWT.NONE);
		tbtmFindDescriptors.setControl(composite_7);

		tab5desc = new Text(composite_7, SWT.READ_ONLY | SWT.WRAP);
		tab5desc.setToolTipText(ApplicationUtilities.getProperty("step4Descp2"));
		tab5desc.setText(ApplicationUtilities.getProperty("step4Descp2"));
		tab5desc.setEditable(false);
		tab5desc.setBounds(10, 10, 744, 39);
		
		findDescriptorTable = new Table(composite_7, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
		findDescriptorTable.setBounds(10, 74, 744, 209);
		findDescriptorTable.setLinesVisible(true);
		findDescriptorTable.setHeaderVisible(true);
		final TableColumn findDescriptorTableColumn_Count = new TableColumn(findDescriptorTable, SWT.NONE);
		findDescriptorTableColumn_Count.setWidth(81);
		findDescriptorTableColumn_Count.setText("Count");
		final TableColumn findDescriptorTableColumn_Words = new TableColumn(findDescriptorTable, SWT.LEFT);
		findDescriptorTableColumn_Words.setWidth(659);
		findDescriptorTableColumn_Words.setText("Descriptors");
		findDescriptorTable.addListener(SWT.Selection, new Listener() {
		      public void handleEvent(Event e) {
		    	 TableItem[] selItem= findDescriptorTable.getSelection();
		    	 for (TableItem item : selItem) {
			  		String str = item.getText(1);
	  				try {
	  					descriptorContextText.setText("");
						mainDb.getContextData(str, descriptorContextText);
					} catch (ParsingException e1) {
						e1.printStackTrace();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
			  			
		    	 }
		      }
		    });
		
		descriptorContextText = new StyledText(composite_7, SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
		descriptorContextText.setEditable(false);
		descriptorContextText.setDoubleClickEnabled(false);
		descriptorContextText.setBounds(10, 299, 744, 114);
		*/
		/*"load results from last time" button*/
		/*Button tab5_findDescriptor_loadFromLastTimeButton = new Button(composite_7, SWT.NONE);
		tab5_findDescriptor_loadFromLastTimeButton.setBounds(171, 433, 155, 25);
		tab5_findDescriptor_loadFromLastTimeButton.setText(ApplicationUtilities.getProperty("termCurationLoad"));
		tab5_findDescriptor_loadFromLastTimeButton.setToolTipText(ApplicationUtilities.getProperty("termCurationLoadTTT"));
		tab5_findDescriptor_loadFromLastTimeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//call functions to load values for all tables in order			
				int c = loadFindDescriptorTable(); //use wordpos table
				if(c==0){
					String messageHeader = ApplicationUtilities.getProperty("popup.header.info");
					String message = ApplicationUtilities.getProperty("popup.load.nodata");				
					ApplicationUtilities.showPopUpWindow(message, messageHeader, SWT.ICON_INFORMATION);
				}

			}			
		});*/
		/*"mark as bad" button */
		/*final Button tab5_findDescriptor_MarkAsGoodButton = new Button(composite_7, SWT.NONE);//this button is on the markup-descriptor tab
		tab5_findDescriptor_MarkAsGoodButton.setText(ApplicationUtilities.getProperty("termCurationMarkBad"));
		tab5_findDescriptor_MarkAsGoodButton.setBounds(342, 433, 132, 25);
		tab5_findDescriptor_MarkAsGoodButton.setToolTipText(ApplicationUtilities.getProperty("termCurationMarkBadTTT"));
		tab5_findDescriptor_MarkAsGoodButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				TableItem [] items = findDescriptorTable.getItems();
				for (TableItem item : items) {
					if (item.getChecked()) {
						if(item.getBackground().equals(red)){
							item.setBackground(0,white);
							item.setBackground(1,white);
						}else{
							item.setBackground(0, red);
							item.setBackground(1, red);	
						}
						item.setChecked(false);
						
					}
				}
			}
		});*/

	
		/*"mark others as good" button*/
		/*final Button tab5_findDescriptor_MarkAsBadButton = new Button(composite_7, SWT.NONE);
		tab5_findDescriptor_MarkAsBadButton.setText(ApplicationUtilities.getProperty("termCurationMarkOthersGood"));
		tab5_findDescriptor_MarkAsBadButton.setBounds(479, 433, 140, 25);
		tab5_findDescriptor_MarkAsBadButton.setToolTipText(ApplicationUtilities.getProperty("termCurationMarkOthersGoodTTT"));
		tab5_findDescriptor_MarkAsBadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				TableItem [] items = findDescriptorTable.getItems();
				for (TableItem item : items) {
					if (!item.getBackground(1).equals(red)) {
						item.setBackground(0, green);
						item.setBackground(1, green);	
						item.setChecked(false);
					}
				}
				//removeDescriptorFromTable(findDescriptorTable);
			}
		});*/
		
		/*save button*/
		/*Button tab5_findDescriptor_SaveButton = new Button(composite_7, SWT.NONE);
		tab5_findDescriptor_SaveButton.setBounds(622, 433, 132, 25);
		tab5_findDescriptor_SaveButton.setText(ApplicationUtilities.getProperty("termCurationSave"));
		tab5_findDescriptor_SaveButton.setToolTipText(ApplicationUtilities.getProperty("termCurationSaveTTT"));
		tab5_findDescriptor_SaveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				saveDescriptorTerms(findDescriptorTable, Registry.MARKUP_ROLE_B);
				descriptorContextText.setText("");
				//saveTermRole(descriptorsToSaveList, Registry.MARKUP_ROLE_B);
				
				//for display only show the descriptors that are red and undecided
				
				//markupTable_1.removeAll();//removed temporarily, should be removed from database
				
				//findDescriptorTable.removeAll();
				//reLoadTable();				
			}
		});*/

		/*** "Find more Structure" subtab ***/
		/*
		TabItem findMoreStructure = new TabItem(markupNReviewTabFolder, SWT.NONE);
		findMoreStructure.setText("Find More Structures");
		
		Composite composite_10 = new Composite(markupNReviewTabFolder, SWT.NONE);
		findMoreStructure.setControl(composite_10);
		
		tab5desc = new Text(composite_10, SWT.READ_ONLY | SWT.WRAP);
		tab5desc.setToolTipText(ApplicationUtilities.getProperty("step4Descp3"));
		tab5desc.setText(ApplicationUtilities.getProperty("step4Descp3"));
		tab5desc.setEditable(false);
		tab5desc.setBounds(10, 10, 744, 39);
		
		findMoreStructureTable = new Table(composite_10, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
		//findMoreStructureTable.setBounds(35, 94, 722, 216);
		findMoreStructureTable.setBounds(10, 74, 744, 209);
		findMoreStructureTable.setHeaderVisible(true);
		findMoreStructureTable.setLinesVisible(true);
		TableColumn findMoreStructTalbeColumn_Count = new TableColumn(findMoreStructureTable, SWT.NONE);
		findMoreStructTalbeColumn_Count.setWidth(81);
		findMoreStructTalbeColumn_Count.setText("Count");		
		TableColumn findMoreStructTableColumn_Words = new TableColumn(findMoreStructureTable, SWT.NONE);
		findMoreStructTableColumn_Words.setWidth(658);
		findMoreStructTableColumn_Words.setText("StructureName");
		findMoreStructureTable.addListener(SWT.Selection, new Listener() {//display context for selected structure term
		      public void handleEvent(Event e) {		    	 
		    	 TableItem[] selItem= findMoreStructureTable.getSelection();
		    	 for (TableItem item : selItem) {
	  				String str = item.getText(1);
	  				try {
	  					moreStructureContextText.setText("");
						mainDb.getContextData(str, moreStructureContextText);
					} catch (ParsingException e1) {
						e1.printStackTrace();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}			  			
		    	 }
		     }
		});
		
		moreStructureContextText = new StyledText(composite_10, SWT.BORDER | SWT.V_SCROLL|SWT.H_SCROLL);
		//moreStructureContextText.setBounds(35, 334, 722, 69);
		moreStructureContextText.setEditable(false);
		moreStructureContextText.setDoubleClickEnabled(false);
		moreStructureContextText.setBounds(10, 299, 744, 114);
		*/
		/*"load from last time" button*/
		/*Button tab5_findMoreStructure_loadFromLastTimeButton = new Button(composite_10, SWT.NONE);
		tab5_findMoreStructure_loadFromLastTimeButton.setBounds(171, 433, 155, 25);
		tab5_findMoreStructure_loadFromLastTimeButton.setText(ApplicationUtilities.getProperty("termCurationLoad"));
		tab5_findMoreStructure_loadFromLastTimeButton.setToolTipText(ApplicationUtilities.getProperty("termCurationLoadTTT"));
		tab5_findMoreStructure_loadFromLastTimeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//call functions to load values for all tables in order			
				int c = loadFindMoreStructureTable(); //use unknownwords table
				if(c==0){
					String messageHeader = ApplicationUtilities.getProperty("popup.header.info");
					String message = ApplicationUtilities.getProperty("popup.load.nodata");				
					ApplicationUtilities.showPopUpWindow(message, messageHeader, SWT.ICON_INFORMATION);
				}
			}			
		});
		*/
		/*"mark as good*/
		/*Button tab5_findMoreStructure_MarkAsGoodButton = new Button(composite_10, SWT.NONE);
		tab5_findMoreStructure_MarkAsGoodButton.setBounds(342, 433, 132, 25);
		tab5_findMoreStructure_MarkAsGoodButton.setText(ApplicationUtilities.getProperty("termCurationMarkGood"));
		tab5_findMoreStructure_MarkAsGoodButton.setToolTipText(ApplicationUtilities.getProperty("termCurationMarkGoodTTT"));
		tab5_findMoreStructure_MarkAsGoodButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int i=0;
				for (TableItem item : findMoreStructureTable.getItems()) {
					if (item.getChecked()) {
						if(item.getBackground().equals(green)){
							item.setBackground(0,white);
							item.setBackground(1,white);
						}else{
							item.setBackground(0,green);
							item.setBackground(1,green);
						}
						item.setChecked(false);
					}
					i+=1;
				}				
			}
		});
		*/
		/*mark others as bad*/
		/*Button tab5_findMoreStructure_MarkAsBadButton = new Button(composite_10, SWT.NONE);
		tab5_findMoreStructure_MarkAsBadButton.setBounds(479, 433, 132, 25);
		tab5_findMoreStructure_MarkAsGoodButton.setToolTipText(ApplicationUtilities.getProperty("termCurationMarkOthersBadTTT"));
		tab5_findMoreStructure_MarkAsBadButton.setText(ApplicationUtilities.getProperty("termCurationMarkOthersBad"));
	    tab5_findMoreStructure_MarkAsBadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				int i=0;
				for (TableItem item : findMoreStructureTable.getItems()) {
					if (!item.getBackground(1).equals(green)) {				
						findMoreStructureTable.getItem(i).setBackground(0,red);
						findMoreStructureTable.getItem(i).setBackground(1,red);
						item.setChecked(false);
					}
					i+=1;
				}			
			}
		});

		Button tab5_findMoreStructure_SaveButton = new Button(composite_10, SWT.NONE);
		tab5_findMoreStructure_SaveButton.setBounds(622, 433, 132, 25);
		tab5_findMoreStructure_SaveButton.setText(ApplicationUtilities.getProperty("termCurationSave"));
		tab5_findMoreStructure_SaveButton.setToolTipText(ApplicationUtilities.getProperty("termCurationSaveTTT"));
		tab5_findMoreStructure_SaveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				saveStructureTerms(findMoreStructureTable, Registry.MARKUP_ROLE_O);
				moreStructureContextText.setText("");
			}
		});*/
		
		/* "Find More Descriptors" subtab */
		/*TabItem descriptor2Tab = new TabItem(markupNReviewTabFolder, SWT.NONE);
		descriptor2Tab.setText("Find More Descriptors");
		Composite composite_11 = new Composite(markupNReviewTabFolder, SWT.NONE);
		descriptor2Tab.setControl(composite_11);

		tab5desc = new Text(composite_11, SWT.READ_ONLY | SWT.WRAP);
		tab5desc.setToolTipText(ApplicationUtilities.getProperty("step4Descp4"));
		tab5desc.setText(ApplicationUtilities.getProperty("step4Descp4"));
		tab5desc.setEditable(false);
		tab5desc.setBounds(10, 10, 744, 39);
		
		findMoreDescriptorTable = new Table(composite_11, SWT.BORDER | SWT.CHECK | SWT.FULL_SELECTION);
		findMoreDescriptorTable.setBounds(10, 74, 744, 209);
		findMoreDescriptorTable.setHeaderVisible(true);
		findMoreDescriptorTable.setLinesVisible(true);

		moreDescriptorContextText = new StyledText(composite_11, SWT.BORDER|SWT.H_SCROLL|SWT.V_SCROLL);
		moreDescriptorContextText.setEditable(false);
		moreDescriptorContextText.setDoubleClickEnabled(false);	
		moreDescriptorContextText.setBounds(10, 299, 744, 114);

		TableColumn findMoreDescriptorTableColumn_Count= new TableColumn(findMoreDescriptorTable, SWT.NONE);
		findMoreDescriptorTableColumn_Count.setWidth(81);
		findMoreDescriptorTableColumn_Count.setText("Count");
		TableColumn findMoreDescriptorTableColumn_Words= new TableColumn(findMoreDescriptorTable, SWT.NONE);
		findMoreDescriptorTableColumn_Words.setWidth(658);
		findMoreDescriptorTableColumn_Words.setText("Descriptor Name");
		findMoreDescriptorTable.addListener(SWT.Selection, new Listener() {//display context for selected structure term
		      public void handleEvent(Event e) {		    	 
		    	 TableItem[] selItem= findMoreDescriptorTable.getSelection();
		    	 for (TableItem item : selItem) {
	  				String str = item.getText(1);
	  				try {
	  					moreDescriptorContextText.setText("");
						mainDb.getContextData(str, moreDescriptorContextText);
					} catch (ParsingException e1) {
						e1.printStackTrace();
					} catch (SQLException e1) {
						e1.printStackTrace();
					}			  			
		    	 }
		     }
		});*/
		/*load button*/
		/*Button tab5_findMoreDescriptor_loadFromLastTimeButton = new Button(composite_11, SWT.NONE);
		tab5_findMoreDescriptor_loadFromLastTimeButton.setBounds(171, 433, 155, 25);
		tab5_findMoreDescriptor_loadFromLastTimeButton.setText(ApplicationUtilities.getProperty("termCurationLoad"));
		tab5_findMoreDescriptor_loadFromLastTimeButton.setToolTipText(ApplicationUtilities.getProperty("termCurationLoadTTT"));
		tab5_findMoreDescriptor_loadFromLastTimeButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				//call functions to load values for all tables in order			
				int c = loadFindMoreDescriptorTable();
				if(c==0){
					String messageHeader = ApplicationUtilities.getProperty("popup.header.info");
					String message = ApplicationUtilities.getProperty("popup.load.nodata");				
					ApplicationUtilities.showPopUpWindow(message, messageHeader, SWT.ICON_INFORMATION);
				}
			}			
		});*/

		/*mark as good*/
		/*Button tab5_findMoreDescriptor_MarkAsGoodButton = new Button(composite_11, SWT.NONE);
		tab5_findMoreDescriptor_MarkAsGoodButton.setBounds(342, 433, 132, 25);
		tab5_findMoreDescriptor_MarkAsGoodButton.setText(ApplicationUtilities.getProperty("termCurationMarkGood"));
		tab5_findMoreDescriptor_MarkAsGoodButton.setToolTipText(ApplicationUtilities.getProperty("termCurationMarkGoodTTT"));
		tab5_findMoreDescriptor_MarkAsGoodButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				TableItem [] items = findMoreDescriptorTable.getItems();
				for (TableItem item : items) {
					if (item.getChecked()) {
						if(item.getBackground().equals(green)){
							item.setBackground(0,white);
							item.setBackground(1,white);
						}else{
							item.setBackground(0, green);
							item.setBackground(1, green);
						}
						item.setChecked(false);
					}
				}
			}
		});*/
		/*mark others as bad*/
		/*Button tab5_findMoreDescriptor_MarkAsBadButton = new Button(composite_11, SWT.NONE);
		tab5_findMoreDescriptor_MarkAsBadButton.setBounds(479, 433, 140, 25);
		tab5_findMoreDescriptor_MarkAsBadButton.setText(ApplicationUtilities.getProperty("termCurationMarkOthersBad"));
		tab5_findMoreDescriptor_MarkAsBadButton.setToolTipText(ApplicationUtilities.getProperty("termCurationMarkOthersBadTTT"));
		tab5_findMoreDescriptor_MarkAsBadButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				TableItem [] items = findMoreDescriptorTable.getItems();
				for (TableItem item : items) {
					if (!item.getBackground(1).equals(green)) {
						item.setBackground(0, red);
						item.setBackground(1, red);
						item.setChecked(false);
					}
				}
				//removeDescriptorFromTable(findMoreDescriptorTable);
			}
		});

		Button tab5_findMoreDescriptor_SaveButton = new Button(composite_11, SWT.NONE);
		tab5_findMoreDescriptor_SaveButton.setBounds(622, 433, 132, 25);
		tab5_findMoreDescriptor_SaveButton.setText(ApplicationUtilities.getProperty("termCurationSave"));
		tab5_findMoreDescriptor_SaveButton.setToolTipText(ApplicationUtilities.getProperty("termCurationSaveTTT"));
		tab5_findMoreDescriptor_SaveButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				saveDescriptorTerms(findMoreDescriptorTable, Registry.MARKUP_ROLE_B);
				moreDescriptorContextText.setText("");
				//saveTermRole(descriptorsToSaveList, Registry.MARKUP_ROLE_B);
				
				//for display only show the descriptors that are red and undecided
				
				//markupTable_1.removeAll();//removed temporarily, should be removed from database
				
				//findMoreDescriptorTable.removeAll();
				//reLoadTable();				
			}
		});*/
		/********************************/
		/*"unknown removal" tab: step 5 */
		/********************************/
		/*
		final TabItem tagTabItem = new TabItem(tabFolder, SWT.NONE);
		tagTabItem.setText(ApplicationUtilities.getProperty("tab.six.name"));

		final Composite composite_6 = new Composite(tabFolder, SWT.NONE);
		tagTabItem.setControl(composite_6);

		tab6desc = new Text(composite_6, SWT.READ_ONLY|SWT.WRAP);
		tab6desc.setText(ApplicationUtilities.getProperty("step5Descp"));
		tab6desc.setEditable(false);
		tab6desc.setBounds(10, 10, 741, 41);
		
		tagTable = new Table(composite_6, SWT.CHECK | SWT.BORDER | SWT.FULL_SELECTION);
		//tagTable = new Table(composite_6,  SWT.BORDER | SWT.FULL_SELECTION);
		tagTable.setLinesVisible(true);
		tagTable.setHeaderVisible(true);
		tagTable.setBounds(10, 57, 744, 203);
		
	    final TableColumn newColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
	    newColumnTableColumn.setWidth(81);
	    newColumnTableColumn.setText("Check one");

		final TableColumn numberColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
		numberColumnTableColumn.setWidth(78);
		numberColumnTableColumn.setText("Sentence Id");

	    final TableColumn modifierColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
	    modifierColumnTableColumn.setWidth(65);
	    modifierColumnTableColumn.setText("Modifier");
	    
		final TableColumn tagColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
		tagColumnTableColumn.setWidth(78);
		tagColumnTableColumn.setText("Tag");

		final TableColumn sentenceColumnTableColumn = new TableColumn(tagTable, SWT.NONE);
		sentenceColumnTableColumn.setWidth(515);
		sentenceColumnTableColumn.setText("Sentence");

		
	    tagTable.addListener(SWT.Selection, new Listener() {
	        public void handleEvent(Event event) {
	        	TableItem item = (TableItem) event.item;
	        	item.setChecked(true);
	        	//tagTable.getItem(hashCodeOfItem).setChecked(false);
	        	for (TableItem tempItem : tagTable.getItems()) {
	        		if (tempItem.hashCode() == hashCodeOfItem) {
	        			tempItem.setChecked(false);
	        		}
	        	} 
	        	int sentid = Integer.parseInt(item.getText(1));
	        	updateContext(sentid);
	        	if (hashCodeOfItem != item
	        			.hashCode()) {
	        		hashCodeOfItem = item.hashCode();
	        	} else {
	        		hashCodeOfItem = 0;
	        	}
	        	
	        }
	      });


		//controls for marking up a sentence
		final Label modifierLabel = new Label(composite_6, SWT.NONE);
		modifierLabel.setText("Modifier:");
		modifierLabel.setBounds(10, 275, 64, 15);

		modifierListCombo = new Combo(composite_6, SWT.NONE);
		modifierListCombo.setBounds(80, 270, 210, 21);				
		
		final Label tagLabel = new Label(composite_6, SWT.NONE);
		tagLabel.setText("Tag:");
		tagLabel.setBounds(300, 275, 64, 24);
		
		tagListCombo = new Combo(composite_6, SWT.NONE);
		tagListCombo.setBounds(370, 270, 210, 21);
		
		final Button applyToAllButton = new Button(composite_6, SWT.NONE);
		applyToAllButton.setText(ApplicationUtilities.getProperty("Apply2Checked"));
		applyToAllButton.setToolTipText(ApplicationUtilities.getProperty("Apply2CheckedTTT"));
		applyToAllButton.setBounds(626, 270, 130, 23);
		applyToAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				applyTagToAll();//also check the next box automatically								
			}
		});

	    //context
	    final Label contextLabel = new Label(composite_6, SWT.NONE);
		contextLabel.setText("Context:");
		contextLabel.setBounds(10, 310, 88, 15);
		contextStyledText = new StyledText(composite_6, SWT.READ_ONLY| SWT.WRAP | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
		//contextStyledText = new StyledText(composite_6, SWT.V_SCROLL | SWT.READ_ONLY | SWT.H_SCROLL | SWT.BORDER);
		contextStyledText.setBounds(10, 330, 744, 114);		

		//load button
		final Button loadTagButton = new Button(composite_6, SWT.NONE);
		loadTagButton.setBounds(392, 450, 168, 23);
		loadTagButton.setText(ApplicationUtilities.getProperty("sentCurationLoad"));
		loadTagButton.setToolTipText(ApplicationUtilities.getProperty("sentCurationLoadTTT"));
		//loadTagButton.setToolTipText("Load sentences to be tagged");
		loadTagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				ApplicationUtilities.showProgressPopup(popupBar);
				loadTags(tabFolder);
				groupInfo.clear();
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.six.name"), combo.getText(), true);
					statusOfMarkUp[5] = true;
					
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - unknown" , exe);
					exe.printStackTrace();
				}
				
			}
		});
		

		//save button
		final Button saveTagButton = new Button(composite_6, SWT.NONE);
		saveTagButton.setBounds(580, 450, 174, 23);
		saveTagButton.setText("Save Tagged Sentences");
		saveTagButton.setToolTipText("Save tagged sentences");
		saveTagButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				saveTag(tabFolder);
			}
		});

		*/
		/********************************/
		/*"Character State" tab: step 6 */
		/*make changes in prefix_grouped_terms, save decisions to prefix_group_decisions */
		/********************************/
		/*
		TabItem tbtmCharacterStates = new TabItem(tabFolder, SWT.NONE);
		tbtmCharacterStates.setText(ApplicationUtilities.getProperty("tab.character"));
		
		Composite composite_8 = new Composite(tabFolder, SWT.NONE);
		tbtmCharacterStates.setControl(composite_8);
		
		Group grpContextTable = new Group(composite_8, SWT.NONE);
		grpContextTable.setText("Context Table");
		grpContextTable.setBounds(0, 356, 635, 149);
		// Add the context table here
		contextTable = new Table(grpContextTable, SWT.FULL_SELECTION | SWT.BORDER);
		contextTable.setBounds(10, 20, 615, 119);
		contextTable.setHeaderVisible(true);
		contextTable.setLinesVisible(true);
		contextTable.addMouseListener(new MouseListener () {
			public void mouseDoubleClick(MouseEvent event) {
				try {
					String filePath = Registry.TargetDirectory + 
					ApplicationUtilities.getProperty("DESCRIPTIONS")+ "\\";
					String fileName = contextTable.getSelection()[0].getText(0).trim();
					fileName = fileName.substring(0, fileName.indexOf("-"));
					filePath += fileName;
					if (filePath.indexOf("txt") != -1) {
						try {
							Runtime.getRuntime().exec(ApplicationUtilities.getProperty("notepad") 
									+ " \"" + filePath + "\"");
						} catch (Exception e){
							ApplicationUtilities.showPopUpWindow(ApplicationUtilities.getProperty("popup.error.msg") +
									ApplicationUtilities.getProperty("popup.editor.msg"),
									ApplicationUtilities.getProperty("popup.header.error"), 
									SWT.ERROR);
						}
					}
				} catch (Exception exe) {
					LOGGER.error("Error in displaying the file in context table", exe);
					exe.printStackTrace();
				}
 
			}			
			public void mouseDown(MouseEvent event) {}
			public void mouseUp(MouseEvent event) {}
		});
		
		final TableColumn contextTablecolumn_1 = new TableColumn(contextTable, SWT.NONE);
		contextTablecolumn_1.setWidth(100);
		contextTablecolumn_1.setText("Source");
		
		final TableColumn contextTablecolumn_2 = new TableColumn(contextTable, SWT.NONE);
		contextTablecolumn_2.setWidth(512);
		contextTablecolumn_2.setText("Sentence");
		
		Group group_2 = new Group(composite_8, SWT.NONE);
		group_2.setBounds(641, 314, 144, 191);
		
		processedGroupsTable = new Table(group_2, SWT.BORDER | SWT.FULL_SELECTION);
		processedGroupsTable.setBounds(10, 10, 124, 171);
		processedGroupsTable.setLinesVisible(true);
		processedGroupsTable.setHeaderVisible(true);
		
		TableColumn tableColumn = new TableColumn(processedGroupsTable, SWT.NONE);
		tableColumn.setWidth(120);
		tableColumn.setText("Processed Groups");
		
		Group group_3 = new Group(composite_8, SWT.NONE);
		group_3.setBounds(0, 295, 635, 36);
		
		Label lblGroup = new Label(group_3, SWT.NONE);
		lblGroup.setBounds(7, 13, 45, 15);
		lblGroup.setText("Groups");
		
		groupsCombo = new Combo(group_3, SWT.NONE);
		groupsCombo.setBounds(56, 10, 161, 23);
		groupsCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				//Clear context table as the sentences from the previous group needn't be shown for another group;
				contextTable.removeAll();
				loadTerms();
			}
		});
		
		Label lblDecision = new Label(group_3, SWT.NONE);
		lblDecision.setBounds(270, 13, 120, 17);
		lblDecision.setText("Label for this group");
		
		comboDecision = new Combo(group_3, SWT.NONE);
		comboDecision.setBounds(392, 10, 145, 23);
		
		Button btnSave = new Button(group_3, SWT.NONE);
		btnSave.setBounds(550, 8, 75, 25);
		btnSave.setText(ApplicationUtilities.getProperty("SaveCategoryBtn"));
		btnSave.setToolTipText(ApplicationUtilities.getProperty("SaveCategoryTTT"));

		btnSave.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				
				try {
					String decision4group = comboDecision.getText();
					if(unpaired){
						Control[] children = termsGroup.getChildren();
						//loop through children for Group (hold term1) and Text (holds category)
						//save to the final term-category table
						boolean ignore = false;
						for(int i = 1; i < children.length; i++){
							if(children[i] instanceof Text && children[i-1] instanceof Group){
								String term = children[i-1].getToolTipText();
								String decision = ((Text)(children[i])).getText();
								int choice = -1;
								if(!ignore && decision.trim().length()<1){
									choice = ApplicationUtilities.showPopUpWindow(
											ApplicationUtilities.getProperty("popup.char.missing"),
											ApplicationUtilities.getProperty("popup.header.warning"), 
											SWT.YES | SWT.NO);
								}
								if(choice == SWT.NO) return;
								else if(choice == SWT.YES)  ignore = true;
								charDb.saveTermCategory(groupsCombo.getText().replace("Group_", ""),term, decision);
							}
						}	
						decision4group = "done";
					}
					ArrayList<TermsDataBean> terms = new ArrayList<TermsDataBean>();				
					ArrayList <CoOccurrenceBean> cooccurrences = groupInfo.get(groupsCombo.getText()).getCooccurrences();								

					((CharacterGroupBean)groupInfo.get(groupsCombo.getText())).setSaved(true);
					((CharacterGroupBean)groupInfo.get(groupsCombo.getText())).setDecision(decision4group);

					//Save the decision first 
					charDb.saveDecision(cooccurrences.get(0).getGroupNo(), decision4group);
					
					//Save the terms remain in the group
					for (CoOccurrenceBean cbean : cooccurrences) {
						TermsDataBean tbean = new TermsDataBean();
						tbean.setFrequency(Integer.parseInt(cbean.getFrequency().getText()));
						tbean.setGroupId(cbean.getGroupNo());
						tbean.setKeep(cbean.getKeep());
						tbean.setSourceFiles(cbean.getSourceFiles());
						
						// The fun starts here! try and save the terms that are there in parentTermGroup
						if(cbean.getTerm1() != null && cbean.getTerm1().isTogglePosition()) {
							tbean.setTerm1(cbean.getTerm1().getTermText().getText());
						} else {
							tbean.setTerm1("");
						}
						
						if(cbean.getTerm2() != null && cbean.getTerm2().isTogglePosition()){
							tbean.setTerm2(cbean.getTerm2().getTermText().getText());
						} else {
							tbean.setTerm2("");
						}
						// Add the termdatabean to the arraylist
						terms.add(tbean);
					}
					
					// update terms table, keeping on the terms remains in the group
					charDb.saveTerms(terms);
					
				} catch (Exception exe) {
					LOGGER.error("Couldnt save the character tab details in MainForm" , exe);
					exe.printStackTrace();
				}
				
				String savedGroupName = groupsCombo.getText();
				processedGroups.put(savedGroupName, savedGroupName);				
				Set <String> processed = processedGroups.keySet();
				processedGroupsTable.removeAll();
				for (String groupName : processed) {
					TableItem item = new TableItem(processedGroupsTable, SWT.NONE);
					item.setText(groupName);
				}		
	

				// Logic for the terms removed from the groups goes here 
				if (noOfTermGroups <= getNumberOfGroupsSaved() && isTermsNotGrouped()) {
					int choice = 0;
					if(charStatePopUp){
						choice = ApplicationUtilities.showPopUpWindow(
								ApplicationUtilities.getProperty("popup.charstates.info"),
								ApplicationUtilities.getProperty("popup.header.info") + " : " +
								ApplicationUtilities.getProperty("popup.header.character"), 
								SWT.YES | SWT.NO | SWT.CANCEL);
						charStatePopUp = false;
					} else {
						choice = ApplicationUtilities.showPopUpWindow(
								ApplicationUtilities.getProperty("popup.char.cont"),
								ApplicationUtilities.getProperty("popup.header.info") + " : " +
								ApplicationUtilities.getProperty("popup.header.character"), 
								SWT.YES | SWT.NO | SWT.CANCEL);
					}

					
				    switch (choice) {
				    case SWT.OK:
				    case SWT.YES:
				    	showRemainingTerms();
				    	break;
				    case SWT.CANCEL:
				    case SWT.NO:
				    case SWT.RETRY:
				    case SWT.ABORT:
				    case SWT.IGNORE:
				    default : //Do Nothing! :-)
				    }

				}
			}

		});
	
		
		Label label_1 = new Label(composite_8, SWT.SEPARATOR | SWT.VERTICAL);
		label_1.setBounds(510, 240, -6, 191);
		
		Group grpRemovedTerms = new Group(composite_8, SWT.NONE);
		grpRemovedTerms.setText("Removed Terms");
		grpRemovedTerms.setBounds(457, 81, 328, 208);
		
		removedScrolledComposite = new ScrolledComposite(grpRemovedTerms, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		removedScrolledComposite.setBounds(10, 24, 308, 174);
		removedScrolledComposite.setExpandHorizontal(true);
		removedScrolledComposite.setExpandVertical(true);
		
		removedTermsGroup = new Group(removedScrolledComposite, SWT.NONE);
		removedTermsGroup.setLayoutData(new RowData());
		
		removedScrolledComposite.setContent(removedTermsGroup);
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Group grpDeleteAnyTerm = new Group(composite_8, SWT.NONE);
		grpDeleteAnyTerm.setText("Categorize the terms by their character categories.");
		grpDeleteAnyTerm.setBounds(0, 55, 451, 234);
		
		termsScrolledComposite = new ScrolledComposite(grpDeleteAnyTerm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		termsScrolledComposite.setBounds(10, 41, 429, 183);
		termsScrolledComposite.setExpandHorizontal(true);
		termsScrolledComposite.setExpandVertical(true);
		
		termsGroup = new Group(termsScrolledComposite, SWT.NONE);
		termsScrolledComposite.setContent(termsGroup);
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Label lblContext = new Label(grpDeleteAnyTerm, SWT.NONE);
		lblContext.setBounds(10, 20, 55, 15);
		lblContext.setText("Context");
		
		Label lblTerm = new Label(grpDeleteAnyTerm, SWT.NONE);
		lblTerm.setBounds(72, 20, 55, 15);
		lblTerm.setText("Term");
		
		Label lblTerm_1 = new Label(grpDeleteAnyTerm, SWT.NONE);
		lblTerm_1.setBounds(170, 20, 120, 15);
		lblTerm_1.setText("Co-occurred Term");
		
		Label lblFrequency = new Label(grpDeleteAnyTerm, SWT.NONE);
		lblFrequency.setBounds(353, 20, 55, 15);
		lblFrequency.setText("Frequency");
		
		sortLabel = new Label(grpDeleteAnyTerm, SWT.NONE);
		sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/down.jpg"));		
		sortLabel.setBounds(408, 10, 31, 25);
		sortLabel.setToolTipText("Sort by frequency");
		sortLabel.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent me){
				ArrayList <CoOccurrenceBean> cooccurences = null;
				Rectangle tempCoordinates = null;
					cooccurences = 
						((CharacterGroupBean)groupInfo.get(groupsCombo.getText())).getCooccurrences();
					int selectionIndex = groupsCombo.getSelectionIndex();
					int size = cooccurences.size();
					CoOccurrenceBean [] cbeans = new CoOccurrenceBean[size];
					int i = 0, j = 0, k = 0;
					for (CoOccurrenceBean cbean: cooccurences) {
						cbeans[i++] = cbean;
					}
					
					boolean toSort = true;
					int firstFrequency = 0; 
						if(cbeans!=null && cbeans.length>0)
							firstFrequency = cbeans[0].getFrequency().getBounds().y;
					int lastFrequency = 0;
					if(cbeans!=null && cbeans.length>0)
						lastFrequency = cbeans[size-1].getFrequency().getBounds().y;
					if(firstFrequency<lastFrequency) {
						toSort = true;
					} else if (firstFrequency>lastFrequency){
						toSort = false;
					}
					
					for (i = 0, j = size-1, k = size-1; i < size/2; i++, j--, k-=2){
						CoOccurrenceBean beanFirst = cbeans[i];
						CoOccurrenceBean beanLast = cbeans[j];
						if(beanFirst.getTerm1() != null 
								&& beanFirst.getTerm2() != null
								&& beanLast.getTerm1() != null
								&& beanLast.getTerm2() != null) {
							// Swap coordinates of radio button 
							tempCoordinates = beanFirst.getContextButton().getBounds();
							beanFirst.getContextButton().setBounds(beanLast.getContextButton().getBounds());
							beanLast.getContextButton().setBounds(tempCoordinates);
							
							// Swap Frequencies 					
							tempCoordinates = null;
							tempCoordinates = beanFirst.getFrequency().getBounds();
							beanFirst.getFrequency().setBounds(beanLast.getFrequency().getBounds());
							beanLast.getFrequency().setBounds(tempCoordinates);
							
							
							if (toSort) {
								//Sort Ascending
									// Swap Group 1 
									tempCoordinates = beanFirst.getTerm1().getTermGroup().getBounds();
									tempCoordinates.y += k * standardIncrement;
									beanFirst.getTerm1().getTermGroup().setBounds(tempCoordinates);
									
									tempCoordinates = beanLast.getTerm1().getTermGroup().getBounds();
									tempCoordinates.y -= k * standardIncrement;						
									beanLast.getTerm1().getTermGroup().setBounds(tempCoordinates);
									
									// Swap Group 2
									tempCoordinates = beanFirst.getTerm2().getTermGroup().getBounds();
									tempCoordinates.y += k * standardIncrement;
									beanFirst.getTerm2().getTermGroup().setBounds(tempCoordinates);
									
									tempCoordinates = beanLast.getTerm2().getTermGroup().getBounds();
									tempCoordinates.y -= k * standardIncrement;						
									beanLast.getTerm2().getTermGroup().setBounds(tempCoordinates);

								
							} else {
								// Sort Descending
								// Swap Group 1
									tempCoordinates = beanFirst.getTerm1().getTermGroup().getBounds();
									tempCoordinates.y -= k * standardIncrement;
									beanFirst.getTerm1().getTermGroup().setBounds(tempCoordinates);
									
									tempCoordinates = beanLast.getTerm1().getTermGroup().getBounds();
									tempCoordinates.y += k * standardIncrement;						
									beanLast.getTerm1().getTermGroup().setBounds(tempCoordinates);
									
									// Swap Group 2 
									tempCoordinates = beanFirst.getTerm2().getTermGroup().getBounds();
									tempCoordinates.y -= k * standardIncrement;
									beanFirst.getTerm2().getTermGroup().setBounds(tempCoordinates);
									
									tempCoordinates = beanLast.getTerm2().getTermGroup().getBounds();
									tempCoordinates.y += k * standardIncrement;						
									beanLast.getTerm2().getTermGroup().setBounds(tempCoordinates);


							}
						}
						
					}
						
					if(sortedBy!=null && sortedBy.length>0 && sortedBy[selectionIndex]) {
						//Sort Ascending
						sortedBy[selectionIndex] = false;
						sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/up.jpg"));
						
					} else if(sortedBy!=null && sortedBy.length>0 && !sortedBy[selectionIndex]){
						// Sort Descending
						sortedBy[selectionIndex] = true;
						sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/down.jpg"));	
						
					}
					
					termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					
			}
			public void mouseUp(MouseEvent me){	}
			public void mouseDoubleClick(MouseEvent me){	}
		});
		Button btnViewGraphVisualization = new Button(composite_8, SWT.NONE);
		btnViewGraphVisualization.setBounds(457, 55, 159, 25);
		btnViewGraphVisualization.setText("View Graph Visualization");
		btnViewGraphVisualization.setToolTipText("Click to view the graph visualization of the terms that have co-occurred");
		
		step6_desc = new Text(composite_8, SWT.READ_ONLY | SWT.WRAP);
		step6_desc.setText(ApplicationUtilities.getProperty("step6DescpText"));
		step6_desc.setEditable(false);
		step6_desc.setBounds(10, 10, 762, 39);
	

		btnViewGraphVisualization.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				if(groupsCombo.getItemCount()>0 && groupsCombo.getText().trim()!="")
				{
				CoOccurrenceGraph.viewGraph(Registry.TargetDirectory+
				ApplicationUtilities.getProperty("CHARACTER-STATES") + "\\" + groupsCombo.getText()+".xml", groupsCombo.getText());
				}
				else
				{
					String messageHeader = ApplicationUtilities.getProperty("popup.header.info");
					ApplicationUtilities.showPopUpWindow("No graph to display", messageHeader, SWT.ICON_WARNING);
					//when no graph to display.. land him on next tab
				}
			}
		});
		*/
		/*************** Finalizer tab ***********************/

		final TabItem finalizerTabItem = new TabItem(tabFolder, SWT.NONE);
		finalizerTabItem.setText(ApplicationUtilities.getProperty("tab.four.name"));

		final Composite composite_5 = new Composite(tabFolder, SWT.NONE);
		finalizerTabItem.setControl(composite_5);

		txtThisLastStep = new Text(composite_5, SWT.READ_ONLY | SWT.WRAP);
		txtThisLastStep.setText(ApplicationUtilities.getProperty("step7DescpText"));
		txtThisLastStep.setBounds(10, 10, 744, 38);
	
		finalizerTable = new Table(composite_5, SWT.FULL_SELECTION | SWT.BORDER);
		finalizerTable.setBounds(10, 54, 744, 250);
		finalizerTable.setLinesVisible(true);
		finalizerTable.setHeaderVisible(true);
		finalizerTable.addMouseListener(new MouseListener () {
			public void mouseDoubleClick(MouseEvent event) {
				String filePath = Registry.TargetDirectory + 
				ApplicationUtilities.getProperty("FINAL")+ System.getProperty("file.separator") +
				finalizerTable.getSelection()[0].getText(1).trim();				
				
				if (filePath.indexOf("xml") != -1) {
					try {
						Runtime.getRuntime().exec(ApplicationUtilities.getProperty("notepad") 
								+ " \"" + filePath + "\"");
					} catch (Exception e){
						ApplicationUtilities.showPopUpWindow(ApplicationUtilities.getProperty("popup.error.msg") +
								ApplicationUtilities.getProperty("popup.editor.msg"),
								ApplicationUtilities.getProperty("popup.header.error"), 
								SWT.ERROR);
					}
				} 
			}			
			public void mouseDown(MouseEvent event) {}
			public void mouseUp(MouseEvent event) {}
		});

		final TableColumn transformationNumberColumnTableColumn_1_2 = new TableColumn(finalizerTable, SWT.NONE);
		transformationNumberColumnTableColumn_1_2.setWidth(168);
		transformationNumberColumnTableColumn_1_2.setText("File Count");

		final TableColumn transformationNameColumnTableColumn_1_2 = new TableColumn(finalizerTable, SWT.NONE);
		transformationNameColumnTableColumn_1_2.setWidth(359);
		transformationNameColumnTableColumn_1_2.setText(ApplicationUtilities.getProperty("file"));
		
		final Text finalLog = new Text(composite_5, SWT.WRAP | SWT.V_SCROLL | SWT.MULTI | SWT.BORDER);
		finalLog.setBounds(10, 310, 744, 150);
		finalLog.setEnabled(true);

		/*finalizerProgressBar = new ProgressBar(composite_5, SWT.NONE);
		finalizerProgressBar.setVisible(false);
		finalizerProgressBar.setBounds(10, 436, 322, 17);*/

		final Button startFinalizerButton = new Button(composite_5, SWT.NONE);
		startFinalizerButton.setToolTipText("Run step 7");
		startFinalizerButton.setBounds(530, 470, 96, 23);
		//startFinalizerButton.setBounds(364, 470, 85, 23);
		startFinalizerButton.setText(ApplicationUtilities.getProperty("step7RunBtn"));
		startFinalizerButton.setToolTipText(ApplicationUtilities.getProperty("step7RunTTT"));
		startFinalizerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e){
				finalizerTable.removeAll();
				boolean completed = startFinalize(finalLog);
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.four.name"), combo.getText(), true);
					statusOfMarkUp[3] = true;
					if(completed){
					/*File fileList= new File(Registry.TargetDirectory+"\\final\\");
					if(fileList.list().length==0)
					{
						//show error popup
						statusOfMarkUp[3] = false;
						ApplicationUtilities.showPopUpWindow("Error executing step 7", "Error",SWT.ERROR);
					}*/
					}
					
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - markup" , exe);
					exe.printStackTrace();
				}
			}
		});
		
		/*Button btnLoad_2 = new Button(composite_5, SWT.NONE);
		btnLoad_2.setToolTipText(ApplicationUtilities.getProperty("step7LoadTTT"));
		btnLoad_2.setBounds(455, 470, 192, 23);
		btnLoad_2.setText(ApplicationUtilities.getProperty("step7LoadBtn"));
		btnLoad_2.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				loadFileInfo(finalizerTable, Registry.TargetDirectory + 
						ApplicationUtilities.getProperty("FINAL"));
			}
		});*/
		
		final Button clearFinalizerButton = new Button(composite_5, SWT.NONE);
		clearFinalizerButton.setToolTipText(ApplicationUtilities.getProperty("ClearRerunTTT"));
		clearFinalizerButton.setBounds(653, 470, 96, 23);
		clearFinalizerButton.setText(ApplicationUtilities.getProperty("ClearRerunBtn"));
		clearFinalizerButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				finalizerTable.removeAll();
				startFinalize(finalLog);
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.four.name"), combo.getText(), true);
					statusOfMarkUp[3] = true;
					//check if finalized final contains files--this should be done after finalize step is completed.
					/*File fileList= new File(Registry.TargetDirectory+"\\final\\");
					if(fileList.list().length==0)
					{
						//show error popup
						statusOfMarkUp[3] = false;
						ApplicationUtilities.showPopUpWindow("Error executing step 7", "Error",SWT.ERROR);
					}*/
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - markup" , exe);
					exe.printStackTrace();
				}							
			}
		});


/*		final TabItem glossaryTabItem = new TabItem(tabFolder, SWT.NONE);
		glossaryTabItem.setText(ApplicationUtilities.getProperty("tab.eight.name"));

		final Composite composite_7 = new Composite(tabFolder, SWT.NONE);
		glossaryTabItem.setControl(composite_7);

		glossaryStyledText = new StyledText(composite_7, SWT.V_SCROLL | SWT.READ_ONLY | SWT.H_SCROLL | SWT.BORDER);
		glossaryStyledText.setBounds(10, 10, 744, 369);

		final Button reportGlossaryButton = new Button(composite_7, SWT.NONE);
		reportGlossaryButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(final SelectionEvent e) {
				reportGlossary();
				try {
					mainDb.saveStatus(ApplicationUtilities.getProperty("tab.eight.name"), combo.getText(), true);
					statusOfMarkUp[7] = true;
				} catch (Exception exe) {
					LOGGER.error("Couldnt save status - glossary" , exe);
					exe.printStackTrace();
				}
				
			}
		});
		reportGlossaryButton.setBounds(654, 385, 100, 23);
		reportGlossaryButton.setText("Report");*/

		/* final Label logoLabel = new Label(shell, SWT.NONE);
		logoLabel.setText(ApplicationUtilities.getProperty("application.instructions"));
		logoLabel.setBounds(10, 485, 530, 83);

		final Label label = new Label(shell, SWT.NONE);
		label.setBackgroundImage(SWTResourceManager.getImage(MainForm.class, 
				ApplicationUtilities.getProperty("application.logo")));
		label.setBounds(569, 485, 253, 71);*/

	}

	private void createSubtab(final TabFolder markupNReviewTabFolder, final String type, Composite composite_1, Group group, final ScrolledComposite scrolledComposite, final Composite termRoleMatrix, final StyledText contextText) {
		String subtabTitle ="";
		String subtabInstruction = "";
		if(type.compareToIgnoreCase("others")==0){
			subtabTitle = ApplicationUtilities.getProperty("tab.three.three.name");
			subtabInstruction = "step4Descp3";
		}else if(type.compareToIgnoreCase("structures")==0){
			subtabTitle =  ApplicationUtilities.getProperty("tab.three.one.name");
			subtabInstruction = "step4Descp1";
		}else if(type.compareToIgnoreCase("characters")==0){
			subtabTitle =  ApplicationUtilities.getProperty("tab.three.two.name");
			subtabInstruction = "step4Descp2";
		}		
		
		TabItem tbtmCategorizeOthers = new TabItem(markupNReviewTabFolder, SWT.NONE);
		tbtmCategorizeOthers.setText(subtabTitle);
		//final Composite composite_1 = new Composite(markupNReviewTabFolder, SWT.NONE);
		
		tbtmCategorizeOthers.setControl(composite_1);

		//subtab instruction
		Text text_1 = new Text(composite_1, SWT.READ_ONLY | SWT.WRAP);
		text_1.setToolTipText(ApplicationUtilities.getProperty(subtabInstruction));
		text_1.setText(ApplicationUtilities.getProperty(subtabInstruction));
		text_1.setEditable(false);
		//text_1.setBounds(10, 17, 744, 39);
		text_1.setBounds(10, 2, 744, 68);
		
		//final Group group = new Group(composite_1, SWT.NONE);
		//group.setBounds(10, 62, 744, 250);
		group.setBounds(10, 72, 744, 250);

		//"table" header
		Label lblCount = new Label(group, SWT.NONE);
		lblCount.setText("Count");
		lblCount.setBounds(15, 10, 93, 15);
		
		Label lblTerm = new Label(group, SWT.NONE);
		lblTerm.setText("Term");
		lblTerm.setBounds(125, 10, 93, 15);

		Label lblRole1 = new Label(group, SWT.NONE);
		lblRole1.setText("Is Structure?");
		lblRole1.setBounds(325, 10, 93, 15);
		
		Label lblRole2 = new Label(group, SWT.NONE);
		lblRole2.setText("Is Descriptor?");
		lblRole2.setBounds(425, 10, 93, 15);

		Label lblRole3 = new Label(group, SWT.NONE);
		lblRole3.setText("Neither");
		lblRole3.setBounds(525, 10, 93, 15);
		
		//final ScrolledComposite scrolledComposite = new ScrolledComposite(group, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setBounds(0, 30,744, 200);
		scrolledComposite.setLayout(new RowLayout(SWT.VERTICAL));

		
		/*context area: event handler in loadOthersArea */
		contextText.setEditable(false);
		contextText.setDoubleClickEnabled(false);
		contextText.setBounds(10, 320, 744, 120);
		
		/*"load" button*/
		//final Composite termRoleMatrix = new Composite(scrolledComposite, SWT.NONE);
		Button tab5_others_loadFromLastTimeButton = new Button(composite_1, SWT.NONE);
		tab5_others_loadFromLastTimeButton.setBounds(459, 443, 155, 25);
		tab5_others_loadFromLastTimeButton.setText(ApplicationUtilities.getProperty("termCurationLoad"));
		tab5_others_loadFromLastTimeButton.setToolTipText(ApplicationUtilities.getProperty("termCurationLoadTTT"));
		tab5_others_loadFromLastTimeButton.addSelectionListener(new SelectionAdapter() {
			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(SelectionEvent e) {	
				ArrayList<String> words = null;
				if(type.compareTo("others")==0){
					words = fetchContentTerms(contextText);
				}else if(type.compareTo("structures")==0){
					if(inistructureterms==null || inistructureterms.size()==0){
						words = fetchStructureTerms(contextText);
						inistructureterms = (ArrayList<String>) words.clone();
					}else{
						words = (ArrayList<String>) inistructureterms.clone();
					}
				}else if(type.compareTo("characters")==0){
					if(inicharacterterms==null || inicharacterterms.size()==0){
						words = fetchCharacterTerms(contextText);
						inicharacterterms = (ArrayList<String>) words.clone();
					}else{
						words = (ArrayList<String>) inicharacterterms.clone();
					}
				}
				if(words.size()==0){
					String messageHeader = ApplicationUtilities.getProperty("popup.header.info");
					String message = ApplicationUtilities.getProperty("popup.load.nodata");				
					ApplicationUtilities.showPopUpWindow(message, messageHeader, SWT.ICON_INFORMATION);
				}else{
					loadTermArea(termRoleMatrix, scrolledComposite, words, contextText, type);
				}				
			}			
		});
		
		/* "Save" button */ 
		final Button saveButton = new Button(composite_1, SWT.NONE);
		saveButton.setText("Save");
		saveButton.setBounds(622, 443, 132, 25);//(650, 427, 98, 25);
		saveButton.addSelectionListener(new SelectionAdapter() {

			public void widgetSelected(final SelectionEvent e) {
				//int choice = ApplicationUtilities.showPopUpWindow(
				//	 "After the terms are saved, you will not be able to redo this step. Do you want to save now?", 
				//		ApplicationUtilities.getProperty("popup.header.info"), SWT.YES | SWT.NO);
				//if(choice == SWT.YES) {
					recordTermReviewResults(termRoleMatrix);
				//}else{
				//	return;
				//}
			}

			private void recordTermReviewResults(Composite termRoleMatrix) {
				try{
					//save to db
					ArrayList<String> noneqs = new ArrayList<String>();
					ArrayList<String> structures = new ArrayList<String>();
					ArrayList<String> characters = new ArrayList<String>();
					Hashtable<String, String> categorizedterms = null;
					UUID lastSavedId = null;
					if(type.compareToIgnoreCase("structures") ==0){
						categorizedterms = categorizedtermsS;
						lastSavedId = lastSavedIdS;
					}
					if(type.compareToIgnoreCase("characters") ==0){
						categorizedterms = categorizedtermsC;
						lastSavedId = lastSavedIdC;
					}
					if(type.compareToIgnoreCase("others") ==0){
						categorizedterms = categorizedtermsO;
						lastSavedId = lastSavedIdO;
					}
					Enumeration<String> en = categorizedterms.keys();
					while(en.hasMoreElements()){
						String t = en.nextElement();
						String type = categorizedterms.get(t);
						if(type.compareToIgnoreCase("others")==0) noneqs.add(t);
						if(type.compareToIgnoreCase("structures")==0) structures.add(t);
						if(type.compareToIgnoreCase("characters")==0) characters.add(t);
					}
					inistructureterms = null;
					inicharacterterms = null;		
					//categorizedterms = null;
					

					UUID currentSavedId = UUID.randomUUID();
					mainDb.recordNonEQTerms(noneqs, lastSavedId , currentSavedId);//noneq
					mainDb.saveTermRole(structures, Registry.MARKUP_ROLE_O, lastSavedId, currentSavedId); //structures
					mainDb.saveTermRole(characters, Registry.MARKUP_ROLE_B, lastSavedId, currentSavedId); //descriptors
					if(type.compareToIgnoreCase("structures") ==0){
						lastSavedIdS = currentSavedId;
					}
					if(type.compareToIgnoreCase("characters") ==0){
						lastSavedIdC = currentSavedId;
					}
					if(type.compareToIgnoreCase("others") ==0){
						lastSavedIdO = currentSavedId;
					}
					//set sentences to unknown
					ArrayList<String> nonStructureTerms = new ArrayList<String>();
					nonStructureTerms.addAll(noneqs);
					nonStructureTerms.addAll(characters);
					mainDb.setUnknownTags(nonStructureTerms);
				}catch(Exception e){
					e.printStackTrace();
				}
				//termRoleMatrix.setVisible(false);
				//termRoleMatrix4others.dispose();
				//termRoleMatrix4structures.dispose();
				//termRoleMatrix4characters.dispose();
				contextText.setText("Decisions on this tab are saved");
				/*Control[] controls = termRoleMatrix.getChildren();
				Hashtable<String, String> structures = new Hashtable<String, String>();
				Hashtable<String, String> characters = new Hashtable<String, String>();
				Hashtable<String, String> noneqs = new Hashtable<String, String>();
				for(Control control: controls){
					Composite termRoleGroup = (Composite) control;
					Control[] row = termRoleGroup.getChildren();
					String word = ((Label)row[1]).getText();
					if(((Button)row[2]).getSelection()){//structure
						structures.put(word, word);
					}
					if(((Button)row[3]).getSelection()){//structure
						characters.put(word, word);
					}
					if(((Button)row[4]).getSelection()){//structure
						noneqs.put(word, word);
					}					
				}
				
				try{
					//save to db
					ArrayList<String> terms = new ArrayList<String>();
					terms.addAll(noneqs.values());
					mainDb.recordNonEQTerms(terms);//noneq
					terms = new ArrayList<String>();
					terms.addAll(characters.values());					
					mainDb.saveTermRole(terms, Registry.MARKUP_ROLE_B); //descriptor
					terms = new ArrayList<String>();
					terms.addAll(structures.values());					
					mainDb.saveTermRole(terms, Registry.MARKUP_ROLE_O); //descriptor
					
					//set sentences to unknown
					ArrayList<String> nonStructureTerms = new ArrayList<String>();
					nonStructureTerms.addAll(noneqs.values());
					nonStructureTerms.addAll(characters.values());
					mainDb.setUnknownTags(nonStructureTerms);
				}catch(Exception e){
					e.printStackTrace();
				}
				//remove
				//termRoleMatrix.setVisible(false);
				termRoleMatrix.dispose();
				contextText.setText("");*/
				
			}
		});
	}
	
	

	/* This function saves the Other terms from the markup tab 
	 * to database after user assigns a role to each one of them*/
	/*private void saveOtherTerms() {
		
		HashMap<String, String> otherTerms = new HashMap<String, String> ();
		for (TermRoleBean  tbean : markUpTermRoles) {
			String word = tbean.getTermLabel().getText();
			String role = tbean.getRoleCombo().getText();
			if (!role.equalsIgnoreCase("Other")) {
				if(role.equalsIgnoreCase("Structure")) {
					otherTerms.put(word, Registry.MARKUP_ROLE_O);
				}
				
				if(role.equalsIgnoreCase("Descriptor")) {
					otherTerms.put(word, Registry.MARKUP_ROLE_B);
				}
				
				if(role.equalsIgnoreCase("Verb")) {
					otherTerms.put(word, Registry.MARKUP_ROLE_VERB);
				}
			}
		}
		
		try {
			mainDb.saveOtherTerms(otherTerms);
		} catch (Exception exe){
			LOGGER.error("Error in saving other terms from Markup-Others to database", exe);
			exe.printStackTrace();
		}
	}
	*/
	/* This function is called after the Markup is run to load the Others tab;*/ 
	/*
	public void showOtherTerms() {
		ArrayList<String> otherTerms = null;
		try {
			otherTerms = mainDb.getUnknownWords();
		} catch (Exception exe) {
			LOGGER.error("Exception in getting unknown words", exe);
			exe.printStackTrace();
		}
		
		if (otherTerms != null) {
			int counter =1;
	        for (String term : otherTerms) {
	        	addOtherTermsRow(term,counter);
	        	counter+=1;
	        	System.out.println("addother term called for "+term);
	        }
	        
			RowData rowdata = (RowData)termRoleGroup.getLayoutData();
			rowdata.height += 40;
			termRoleGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        Rectangle rect = termRoleGroup.getBounds();
	        rect.height += 40;
	        termRoleGroup.setBounds(rect); 
	       scrolledComposite.setMinSize(termRoleGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		}

	}*/
	/*
	public void showOtherTermsTable() {
		//Populate Others Table Hong TODO 5/23/11
		ArrayList<String> otherTerms = null;
		try {
			otherTerms = mainDb.getUnknownWords();
		} catch (Exception exe) {
			LOGGER.error("Exception in getting unknown words", exe);
			exe.printStackTrace();
		}
		
		if (otherTerms != null) {
			int counter =1;
	        for (String term : otherTerms) {
	        	//addOtherTermsRow(term,counter);
	        	
	        
	        //  Thanks Prasad Others tab removed this method used to load data in the Others table.
	         	
	        //	TableItem item = new TableItem(table_Others,SWT.HORIZONTAL);
	        //	item.setText(new String[] {counter+"",term});
	        	
	        	        	
	        	//table_Others.set
	        	counter+=1;
	        	System.out.println("addother term called for "+term);
	        }
	        
			
		}

	}*/
	
	
	
	/* This function adds a row to the Markup - Others tab*/
	/*private void addOtherTermsRow(String term,int counter){
		if (markUpTermRoles.size() > 7) {
			RowData rowdata = (RowData)termRoleGroup.getLayoutData();
			rowdata.height += 40;
			termRoleGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        Rectangle rect = termRoleGroup.getBounds();
	        rect.height += 40;
	        termRoleGroup.setBounds(rect);
		}
		
		Button button = new Button(termRoleGroup, SWT.CHECK);
		button.setBounds(otherChkbx.x, otherChkbx.y, otherChkbx.width, otherChkbx.height);
		button.setText(String.valueOf(counter));
		otherChkbx.y+=40;
		
		//instead of a combo box we need radio buttons.
		Button radiobutton_otherRoles =null;
		
		for(int i=0;i<otherRoles.length;i++){
			
			radiobutton_otherRoles= new Button(termRoleGroup, SWT.RADIO);
			radiobutton_otherRoles.setBounds(otherCombo.x +(i*100) , otherCombo.y, otherCombo.width, otherCombo.height);
		
		radiobutton_otherRoles.setText(otherRoles[i]);
		if(i==0){
			radiobutton_otherRoles.setSelection(true);
			
		}
		
		}
		otherCombo.y+=40;
		//radiobutton_otherRoles= new Button(termRoleGroup, SWT.RADIO);
		//radiobutton_otherRoles.setBounds(x,y,width,height);
		//radiobutton_otherRoles.setText("Other");
		
		//164,30   264,30    364,30  464,30
		//164,70   264,70         
		//Combo tempCombo = new Combo(termRoleGroup, SWT.NONE);	
		//tempCombo.setItems(otherRoles);
		//tempCombo.select(0);
		//tempCombo.setBounds(otherCombo.x , otherCombo.y, otherCombo.width, otherCombo.height);
		//otherCombo.y += 40;
	    
	    Label tempLabel = new Label(termRoleGroup, SWT.NONE);
	    tempLabel.setText(term);
	    tempLabel.setBounds(otherTerm.x, otherTerm.y, otherTerm.width, otherTerm.height);
	    otherTerm.y += 40;
	    
	    TermRoleBean tbean = new TermRoleBean(tempLabel, radiobutton_otherRoles);
	  //  markUpTermRoles.add(tbean);
	}*/
	
	/**
	 * This function saves the terms from the Structure tab 
	 * under markup tab - to the wordroles table
	 * method declaration changed to accept a list as the descriptors that are marked green
	 * are stored separately and passed here.Changed by Prasad in May 2011
	 */
	//private void saveTermRole(ArrayList <String>  terms, String role) {
		/*ArrayList <String> structureTerms = new ArrayList<String>();
		TableItem [] items = table.getItems();
		for (TableItem item : items) {
			structureTerms.add(item.getText(1));
		}*/
		/*try {
			mainDb.saveTermRole(terms, role);
			//now display only those that are yet to be decided.
			//so get descriptors from 
			
			
			/*markupTable_1.removeAll();//removed temporarily, should be removed from database
			int count = 1;
			for (String word : descriptorsToSaveList) {
				TableItem item = new TableItem(markupTable_1, SWT.NONE);
				item.setText(new String [] {count+"", word});
				count++;
			}*/
			
			
		/*} catch (Exception exe) {
			exe.printStackTrace();
		}
		
	}*/
	
	/**
	 * This function saves the terms from the Find(More)Structure subtab 
	 * under markup tab - to the wordroles table
	 * 
	 */
	/*private void saveStructureTerms(Table table, String role) {
		//save content of the table in order to assign correct color codes after green ones are saved
		Hashtable<String, Color> content = new Hashtable<String, Color>();
		ArrayList <String> structureTerms = new ArrayList<String>();
		ArrayList <String> nonStructureTerms = new ArrayList<String>();
		TableItem [] items = table.getItems();
		//collect decisions
		for (TableItem item : items) {
			Color color = item.getBackground(1); //bgcolor for text column
			content.put(item.getText(1), color);
			if(color!=null && color.equals(green)){
				structureTerms.add(item.getText(1));
			}else if(color!=null && color.equals(red)){
				nonStructureTerms.add(item.getText(1));
			}
		}
		//act on nonStructureTerms: 
		//set sentences tagged with nonStructureTerms to "unknown"
		//change pos from p/s to b
		try {
			mainDb.setUnknownTags(nonStructureTerms);
			mainDb.changePOStoB(nonStructureTerms);
		} catch (Exception exe) {
			LOGGER.error("Exception encountered in removing structures from database in MainForm:removeBadStructuresFromTable", exe);
			exe.printStackTrace();
		}
		//act on structureTerms: save them to wordrole table
		try {
			mainDb.saveTermRole(structureTerms, role);			
		} catch (Exception exe) {
			exe.printStackTrace();
		}
		//refresh table
		table.removeAll();
		List<String> terms = Arrays.asList(content.keySet().toArray(new String[]{}));
		Collections.sort(terms);
		int count = 1;
		Iterator<String> it = terms.iterator();
		while(it.hasNext()){
			String term = it.next();
			Color color = content.get(term);
			if(!color.equals(red) && !color.equals(green)){
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(new String[] {count+"", term});
				count++;
			}
		}		
	}*/
	
	/**
	 * This function saves the terms from the Find(More)Descriptor subtab 
	 * under markup tab - to the wordroles table
	 * 
	 */
	/*private void saveDescriptorTerms(Table table, String role) {
		//save content of the table in order to assign correct color codes after green ones are saved
		Hashtable<String, Color> content = new Hashtable<String, Color>();
		ArrayList <String> descriptorTerms = new ArrayList<String>();
		ArrayList <String> nonDescriptorTerms = new ArrayList<String>();
		TableItem [] items = table.getItems();
		//collect decisions
		for (TableItem item : items) {
			Color color = item.getBackground(1); //bgcolor for the word column
			content.put(item.getText(1), color);
			if(color!=null && color.equals(green)){
				descriptorTerms.add(item.getText(1));
			}else if(color!=null && color.equals(red)){
				nonDescriptorTerms.add(item.getText(1));
			}
		}
		//act on nonDescriptorTerms: set save_flag in wordroles to "red",
		//so next time these terms will not be curated again
		try {
			mainDb.recordNonEQTerms(nonDescriptorTerms);
		} catch (Exception exe) {
			LOGGER.error("Exception encountered in removing structures from database in MainForm:removeBadStructuresFromTable", exe);
			exe.printStackTrace();
		}
		//act on descriptorTerms: save them to wordrole table
		try {
			mainDb.saveTermRole(descriptorTerms, role);			
		} catch (Exception exe) {
			exe.printStackTrace();
		}
		//refresh table
		table.removeAll();
		List<String> terms = Arrays.asList(content.keySet().toArray(new String[]{}));
		Collections.sort(terms);
		int count = 1;
		Iterator<String> it = terms.iterator();
		while(it.hasNext()){
			String term = it.next();
			Color color = content.get(term);
			if(!color.equals(red) && !color.equals(green)){
				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(new String[] {count+"", term}); 
				count++;
			}
		}		
	}
	*/
	
/**
 * In the Markup - Descriptor Tab, this function 
 * is used to remove any term selected by the user 
 */
	/*private void removeDescriptorFromTable(Table table){
		@SuppressWarnings("unused")
		boolean toRemove = false;
		TableItem [] items = table.getItems();
		for (TableItem item : items) {
			if (item.getChecked()) {
				removedTags.add(item.getText(1));
				//item.setBackground(0,red);
				//item.setBackground(1,red);
				//item.setBackground(2,red);
				item.setBackground(red);				
				toRemove = true;
				item.setChecked(false);
			} else {
				//descriptorsToSaveList.add(item.getText(1));
			}
		}
		
		// remove the tag from the database (No need to remove from database now!)
		if(toRemove) {
			try {
				mainDb.recordNonEQTerms(removedTags);
			} catch (Exception exe) {
				LOGGER.error("Exception encountered in removing tags from database in MainForm:removeMarkup", exe);
				exe.printStackTrace();
			}
		} else {
			ApplicationUtilities.showPopUpWindow("You have not selected anything for removal. " +
					"\nPlease select atleast one row.", 
					"Nothing Selected!", SWT.ICON_ERROR);
		}
	//	markupTable_1.removeAll();//removed temporarily, should be removed from database
		

	}*/

	private void browseConfigurationDirectory() {
        DirectoryDialog directoryDialog = new DirectoryDialog(shell);
        directoryDialog.setMessage("Please select a directory and click OK");
        
        String directory = directoryDialog.open();
        if(directory != null && !directory.equals("")) {
        	String dirsep = System.getProperty("file.separator");
        	if(!directory.endsWith(dirsep)){
        		directory =directory+dirsep;
        	}
        	
        String path = (new File(directory)).getAbsolutePath();;
        
        projectDirectory.setText(path);
        makeReqDirectories(path);               
        }
	}
	
	private void makeReqDirectories(String path) {
		File confFldr = new File(path, "conf");
		File srcFldr = new File(path, "source");
		File targetFldr = new File(path, "target");
	        
	      
        if(!confFldr.exists())
        	confFldr.mkdir();
        if(!srcFldr.exists())
        	srcFldr.mkdir();
        if(!targetFldr.exists())
        	targetFldr.mkdir();
        String targetPath= targetFldr.getAbsolutePath();
        File cooccur = new File(targetPath, "co-occurrence");
        File descriptions = new File(targetPath, "descriptions");
        File final_dir = new File(targetPath, "final");
        File habitats = new File(targetPath, "habitats");
        File transformed = new File(targetPath, "transformed");
	        
        cooccur.mkdir();
        descriptions.mkdir();
        final_dir.mkdir();
        habitats.mkdir();
        transformed.mkdir();

        Registry.ConfigurationDirectory = confFldr.getAbsolutePath();
        Registry.SourceDirectory=srcFldr.getAbsolutePath();
        Registry.TargetDirectory=targetFldr.getAbsolutePath();
	}

	private void startExtraction() throws Exception {
		
		ProcessListener listener = new ProcessListener(extractionTable, extractionProgressBar, shell.getDisplay());
		VolumeExtractor ve = new VolumeExtractor(Registry.SourceDirectory, Registry.TargetDirectory, listener);
		//VolumeExtractor ve = new WordDocSegmenter(Registry.SourceDirectory, Registry.TargetDirectory, listener);
		ve.start();
	}
	
	
	private void startVerification() {
		ProcessListener listener = new ProcessListener(verificationTable, verificationProgressBar, shell.getDisplay());
		VolumeVerifier vv = new VolumeVerifier(listener);
		vv.start();
	}
	
	private void clearVerification() {		
		verificationTable.removeAll();
	}
	
	/*private void startTransformation() {
		ProcessListener listener = new ProcessListener(transformationTable, transformationProgressBar, shell.getDisplay());
		VolumeTransformer vt = new VolumeTransformer(listener, dataPrefixCombo.getText().replaceAll("-", "_").trim());
		vt.start();
	}*/
	
	/*private void startType3Transformation() {
		ProcessListener listener = 
			new ProcessListener(transformationTable, transformationProgressBar, 
					shell.getDisplay());

		CharacterStatementsTransformer preMarkUp = 
				new CharacterStatementsTransformer4NeXML(listener, shell.getDisplay(), 
						null, new ArrayList<String>());
		preMarkUp.start();
	}*/
	
	/*private void startType2Transformation () {
		ProcessListener listener = 
			new ProcessListener(transformationTable, transformationProgressBar, 
					shell.getDisplay());
		Type2Transformer transformer = new Type2Transformer(listener, dataPrefixCombo.getText().replaceAll("-", "_").trim());
		transformer.start();
	}*/
	
	
	/*private void startType4Transformation () {
		ProcessListener listener = 
			new ProcessListener(transformationTable, transformationProgressBar, 
					shell.getDisplay());
		Type4Transformer transformer = null;
		if(this.type4xml.compareToIgnoreCase("taxonx") ==0){
			transformer = new Type4Transformer4TaxonX(listener, dataPrefixCombo.getText().replaceAll("-", "_").trim());
		}else if(this.type4xml.compareToIgnoreCase("phenoscape") ==0){
			transformer = new Type4Transformer4Phenoscape(listener, dataPrefixCombo.getText().replaceAll("-", "_").trim());
		}
		transformer.start();
	}*/

	private void clearTransformation() {
		transformationTable.removeAll();
	}
	
	private void loadProject() {
		File project = null;
		try{
			if(type.trim().equals(""))
				project= new File("fnaproject.conf");
			else
				if(type.trim().equals("type2"))
					project= new File("treatiseproject.conf");
				else
			if(type.trim().equals("type3"))
				project= new File("fishproject.conf");
				else 
			if(type.trim().equals("type4"))
				project= new File("taxonproject.conf");
			
			//	 project= new File("project.conf");			
		BufferedReader in = new BufferedReader(new FileReader(project));
		String conf = in.readLine();
		conf = conf == null ? "" : conf;
		//configurationText.setText(conf);
        Registry.ConfigurationDirectory = conf;

        String source = in.readLine();
        source = source == null ? "" : source;
        //sourceText.setText(source);
        Registry.SourceDirectory = source;
        
        String target = in.readLine();
        target = target == null ? "" : target;
        //targetText.setText(target);
        Registry.TargetDirectory = target;
        step3_desc.append(Registry.TargetDirectory+System.getProperty("file.separator")+ApplicationUtilities.getProperty("TRANSFORMED"));
        
        String projDir = in.readLine();
        projDir = projDir==null?"":projDir;
        projectDirectory.setText(projDir);
        label_prjDir.setText(projDir);
        
		
		}catch(Exception e){
			LOGGER.error("couldn't load the configuration file", e);
			e.printStackTrace();
		}
	}
	
	private void saveProject() {

		StringBuffer sb = new StringBuffer();
		
		//sb.append(configurationText.getText()).append("\\\n");
		//sb.append(sourceText.getText()).append("\\\n");
		//sb.append(targetText.getText());
		sb.append(Registry.ConfigurationDirectory);
		sb.append(Registry.SourceDirectory);
		sb.append(Registry.TargetDirectory);
		//save the main directory also
		sb.append("\\\n").append(projectDirectory.getText());
		
		File project =null;
		try{
			//System.out.println(type.equalsIgnoreCase(""));
			if(type.trim().equals(""))//that means fna is selected.. so save it to fnaproject.conf
				project = new File(System.getProperty("user.dir")+"\\fnaproject.conf");
				else
					if(type.trim().equals("type2"))
						project = new File(System.getProperty("user.dir")+"\\treatiseproject.conf");
						//project = new File(Registry.ConfigurationDirectory+"\\treatiseproject.conf");
						
				else
					if(type.trim().equals("type3"))
						project = new File(System.getProperty("user.dir")+"\\bhlproject.conf");
						//project = new File(Registry.ConfigurationDirectory+"\\bhlproject.conf");
				else 
					if(type.trim().equals("type4"))
						project = new File(System.getProperty("user.dir")+"\\taxonproject.conf");
						//project = new File(Registry.ConfigurationDirectory+"\\taxonproject.conf");
						
				
				
				//project = new File(System.getProperty("user.dir")+"\\project.conf");
			
			if(!project.exists()){
				project.createNewFile();
			}
			BufferedWriter out = new BufferedWriter(new FileWriter(project));
			out.write(sb.toString());
			out.close();
		}catch(Exception e){
			LOGGER.error("couldn't save project", e);
			e.printStackTrace();
		}
	
	}
	
	private void startMarkup() {
		mainDb.createPrepphraseTable();
		mainDb.createWordRoleTable();//roles are: op for plural organ names, os for singular, c for character, v for verb
		mainDb.createNonEQTable();
		mainDb.createTermCategoryTable();
		String workdir = Registry.TargetDirectory;
		//if there is a characters folder,add the files in characters folder to descriptions folder
		mergeCharDescFolders(new File(workdir));
		String todofoldername = ApplicationUtilities.getProperty("DESCRIPTIONS");
		String databasename = ApplicationUtilities.getProperty("database.name");
		ProcessListener listener = new ProcessListener(findStructureTable, markupProgressBar, shell.getDisplay());
		
		VolumeDehyphenizer vd = new VolumeDehyphenizer(listener, workdir, todofoldername,
				databasename, shell.getDisplay(), markUpPerlLog, 
				dataPrefixCombo.getText().replaceAll("-", "_").trim(), /*findDescriptorTable,*/ this);
		vd.start();		
	}
	
	private void mergeCharDescFolders(File parentfolder) {
		File charas = new File(parentfolder, ApplicationUtilities.getProperty("CHARACTERS"));
		if(charas.exists()){
			//add its files to Descriptions folder
			File descs = new File(parentfolder, ApplicationUtilities.getProperty("DESCRIPTIONS"));
			File[] cfiles = charas.listFiles();
			boolean nooverlap = true;
			for(File cfile: cfiles){
				//any risk of overwriting files?
				File target = new File(descs, cfile.getName());
				if(target.exists()){
					nooverlap = false;
					String messageHeader = ApplicationUtilities.getProperty("popup.header.warning");
					String message = ApplicationUtilities.getProperty("popup.warning.copyfiles");				
					ApplicationUtilities.showPopUpWindow(message, messageHeader, SWT.ICON_WARNING);
					break;
				}
			}
			if(nooverlap){
				for(File cfile: cfiles){
					//copy cfile to descs
					fna.parsing.Utilities.copyFile(cfile.getName(), charas, descs);
				}
			}
		}		
	}

	private boolean startFinalize(Text finalLog) {
		
		//ProcessListener listener = new ProcessListener(finalizerTable, finalizerProgressBar, shell.getDisplay());
		ProcessListener listener = new ProcessListener(finalizerTable, null, shell.getDisplay());

		//Connection conn = null;
		try{
			if(conn == null){
				Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
				conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			}
			this.mainDb.finalizeTermCategoryTable();
			//VolumeFinalizer vf = new VolumeFinalizer(listener, 
			//		dataPrefixCombo.getText().replaceAll("-", "_").trim(), conn,MainForm.glossaryPrefixCombo.getText().trim());
			finalLog.setText("");
			VolumeFinalizer vf = new VolumeFinalizer(listener, finalLog, 
					dataPrefixCombo.getText().replaceAll("-", "_").trim(), conn,MainForm.glossaryPrefixCombo.getText().trim(), shell.getDisplay());
			vf.start();
			//vf.join();
			System.out.println();
			return true;
		}catch(Exception e){
			e.printStackTrace();
		}
		return false;
	}
	
	/*private void removeBadStructuresFromTable(Table table) {
		// gather removed tag
		List<String> badStructures = new ArrayList<String>();
		int i=0;
		for (TableItem item : table.getItems()) {
			if (item.getChecked()) {
				badStructures.add(item.getText(1));
				table.getItem(i).setBackground(i,red);
			}
			i+=1;
		}
		// remove the tag from the database
		try {
			mainDb.setUnknownTags(badStructures);
		} catch (Exception exe) {
			LOGGER.error("Exception encountered in removing structures from database in MainForm:removeBadStructuresFromTable", exe);
			exe.printStackTrace();
		}

	}*/	
	
	private void loadTags(TabFolder tabFolder) {
		int XMLFileCount = loadTagTable(tabFolder);
		tagListCombo.add("PART OF LAST SENTENCE"); //part of the last sentence
		
		try {
			 mainDb.loadTagsData(tagListCombo, modifierListCombo);
			 if(XMLFileCount==0)
			 {
				String messageHeader = ApplicationUtilities.getProperty("popup.header.info");
				String message = ApplicationUtilities.getProperty("popup.load.nodata");				
					
			//	 ApplicationUtilities.showPopUpWindow(message, messageHeader,SWT.ICON_INFORMATION );
			 }
			} catch (Exception exe) {
				LOGGER.error("Exception encountered in loading tags from database in MainForm:loadTags", exe);
				exe.printStackTrace();
		    }

	}

	private int loadTagTable(TabFolder tabFolder) {
		tagTable.removeAll();
		int XMLFileCount =0;
		try {
			 if(mainDb.loadTagsTableData(tagTable)==0){
				ApplicationUtilities.showPopUpWindow(
							ApplicationUtilities.getProperty("popup.info.unknownremoval"), 
							ApplicationUtilities.getProperty("popup.header.info"), SWT.ICON_INFORMATION);
				this.tagListCombo.setText("");
				this.modifierListCombo.setText("");
				contextStyledText.setText("Preparing for the next step. ");
				contextStyledText.append("Please proceed to the next step when \"Done\" is displayed in this box.\n");
				try{
					if(conn == null){
						Class.forName("com.mysql.jdbc.Driver");
						String URL = ApplicationUtilities.getProperty("database.url");
						conn = DriverManager.getConnection(URL);
					}
				}catch(Exception e){
					e.printStackTrace();
				}
				String dataPrefix = MainForm.dataPrefixCombo.getText().replaceAll("-", "_").trim();
				String glosstable = MainForm.glossaryPrefixCombo.getText().trim();
				StateCollectorTest sct = new StateCollectorTest(conn, dataPrefix,true,glosstable, shell.getDisplay(), contextStyledText); /*using learned semanticroles only*/
				sct.collect();
				sct.saveStates();
				XMLFileCount = sct.grouping4GraphML();
				contextStyledText.append("Done! Ready to move to the next step.");
				//tabFolder.setSelection(4); //[general, step3, 4, 5, 6, 7] index starts at 0
				//tabFolder.setFocus();
			 }
		} catch (Exception exe) {
				LOGGER.error("Exception encountered in loading tags from database in MainForm:loadTags", exe);
				exe.printStackTrace();
		}
		return XMLFileCount;
	}
	
	private void updateContext(int sentid) throws ParsingException {
		contextStyledText.setText("");
		//tagListCombo.setText("");		
		try {
			mainDb.updateContextData(sentid, contextStyledText);
		} catch (Exception e) {
			LOGGER.error("Exception encountered in loading tags from database in MainForm:updateContext", e);
			e.printStackTrace();
			throw new ParsingException("Failed to execute the statement.", e);			
		}
	}
	
	private void applyTagToAll(){
		String tag = tagListCombo.getText();
		String modifier = this.modifierListCombo.getText();
		
		if (tag == null || tag.equals(""))
			return;
		
		TableItem[] items = tagTable.getItems();
		int i = 0;
		for (; i<items.length; i++) {
			//if (item.hashCode() == hashCodeOfItem) {			
			if (items[i].getChecked()) {
				items[i].setText(2, modifier);
				items[i].setText(3, tag);
				break;
			}
		}
		//auto forward to the next item
		
		if(i+1<items.length){
			i++;
			//now check the next item
			items[i].setChecked(true);
			items[i-1].setChecked(false);
			//show the context for the next item
        	updateContext(Integer.parseInt(items[i].getText(1)));
		}
	}
	
	/**
	 * This is used when Save is clicked on Step5.
	 * @param tagTable
	 * @throws ParsingException
	 * @throws SQLException
	 */
	private void saveTag(TabFolder tabFolder) {

		try {
			mainDb.saveTagData(tagTable);
			
		} catch (Exception exe) {
			LOGGER.error("Exception encountered in loading tags from database in MainForm:saveTag", exe);
			exe.printStackTrace();
		}
		loadTagTable(tabFolder);
		//reset context box
		//contextStyledText.setText("");
	}

	private void reportGlossary() {
		
		LearnedTermsReport ltr = new LearnedTermsReport(ApplicationUtilities.getProperty("database.name") + "_corpus");
		glossaryStyledText.append(ltr.report());
	}
	
	private boolean checkFields(StringBuffer messageText, TabFolder tabFolder) {
		
		boolean errorFlag = false;
		
		//if ( configurationText != null && configurationText.getText().equals("")) {
		if(Registry.ConfigurationDirectory==null){
			messageText.append(ApplicationUtilities.getProperty("popup.error.config"));
		}  			
		//if ( targetText != null && targetText.getText().equals("")) {
		//if(Registry.TargetDirectory == null){
		//	messageText.append(ApplicationUtilities.getProperty("popup.error.target"));
		//} 					
		//if ( sourceText != null && sourceText.getText().equals("")) {
		//if(Registry.SourceDirectory==null){
		//	messageText.append(ApplicationUtilities.getProperty("popup.error.source"));
		//} 
		
		if (dataPrefixCombo != null && dataPrefixCombo.getText().replaceAll("-", "_").trim().equals("")) {
			
			messageText.append(ApplicationUtilities.getProperty("popup.error.dataset"));
			
		}
		
		if (messageText.length() != 0) {
			messageText.append(ApplicationUtilities.getProperty("popup.error.info"));
			ApplicationUtilities.showPopUpWindow(messageText.toString(), 
					ApplicationUtilities.getProperty("popup.header.missing"), SWT.ICON_WARNING);						
			tabFolder.setSelection(0);
			tabFolder.setFocus();
			errorFlag = true;
		} else {
			//if(configurationText != null && !saveFlag) {
			if(dataPrefixCombo != null && !saveFlag) {
				errorFlag = false;
			}

		}		
		return errorFlag;
	}
	
	/**
	 * This function will set the decisions for the character tab, step 5.
	 */
	private String[] setCharacterTabDecisions() {
		ArrayList<String> decisions = new ArrayList<String> ();
		
		try {
			charDb.getDecisionCategory(decisions);
		} catch (Exception exe) {
			LOGGER.error("Couldnt retrieve decision names" , exe);
			exe.printStackTrace();
		}
		int count = 0;
		String [] strDecisions = new String[decisions.size()];
		for (String decision : decisions) {
			strDecisions[count++] = decision;
		}
		return strDecisions;
		//comboDecision.setItems(strDecisions);
		//comboDecision.setText(strDecisions[0]);
	}
	
	/**
	 * This function will prepare the character tab for display of co-occured terms
	 */
	private void setCharactertabGroups() {
		File directory = new File(Registry.TargetDirectory+System.getProperty("file.separator")+
				ApplicationUtilities.getProperty("CHARACTER-STATES"));
		File [] files = directory.listFiles();
		/**Update the global variable with number of groups**/
		noOfTermGroups = files.length;
		
		String [] fileNames = new String[files.length];
		int count = 0, removedEdgesSize = removedEdges.size();
		sortedBy = new boolean [fileNames.length];
		for (File group : files) {
			sortedBy[count] = true;
			fileNames[count] = group.getName().substring(0, group.getName().indexOf(".xml"));
			if (removedEdgesSize == 0){
				/* RemovedEdges HashMap is intialized to store removed edges when
				 *  the user interacts with the terms*/
				removedEdges.put(fileNames[count], new ArrayList<String>());
			}
			
			count++;
		}
		
		groupsCombo.setItems(fileNames);	
		//add check for filename
		if(fileNames!=null && fileNames.length>0)
			{
			groupsCombo.setText(fileNames[0]);
			}
		else
		{
			groupsCombo.setText("");
			//print alert
			ApplicationUtilities.showPopUpWindow(
					ApplicationUtilities.getProperty("popup.load.nodata"), 
					ApplicationUtilities.getProperty("popup.header.info"), SWT.ICON_INFORMATION);
		}			
		groupsCombo.select(0);
		
	}
	
	/***
	 * This function loads the character tab with the co-occurred terms
	 */
	private void loadTerms() {
		String groupName = groupsCombo.getText();
		CharacterGroupBean charGrpBean = groupInfo.get(groupName);
		int selectionIndex = groupsCombo.getSelectionIndex();
		if(charGrpBean == null || !charGrpBean.isSaved()){
			showTerms();
			//restore edges if they were removed but the group was not processed (saved)
			restoreUnsavedEdges();
			sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/down.jpg"));
		} else {
			/* Load it from memory! */
			termsGroup = null;
			termsGroup = new Group(termsScrolledComposite, SWT.NONE);
			termsGroup.setLayoutData(new RowData());
			termsScrolledComposite.setContent(termsGroup);
			termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			removedTermsGroup = null;
			removedTermsGroup = new Group(removedScrolledComposite, SWT.NONE);
			removedTermsGroup.setLayoutData(new RowData());
			removedScrolledComposite.setContent(removedTermsGroup);
			removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			ArrayList<CoOccurrenceBean> cooccurrences = (ArrayList<CoOccurrenceBean>)charGrpBean.getCooccurrences();
			
			if(cooccurrences.size() > 5) {
				
				/* If the number of rows is more than what is displayed, resize the group*/
				RowData rowdata = (RowData)termsGroup.getLayoutData();
				rowdata.height = cooccurrences.size() * 36;
				termsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
		        Rectangle rect = termsGroup.getBounds();
		        rect.height = cooccurrences.size() * 36;
		        termsGroup.setBounds(rect);
				
				
				rowdata = (RowData)removedTermsGroup.getLayoutData();
				rowdata.height = cooccurrences.size() * 36;
				removedTermsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
		        rect = removedTermsGroup.getBounds();
		        rect.height = cooccurrences.size() * 36;
		        removedTermsGroup.setBounds(rect);
			}
			
			/*Set the decision if it was saved*/
			comboDecision.setText(charGrpBean.getDecision());
			
			if (cooccurrences.size() != 0) {
				for (CoOccurrenceBean cbean : cooccurrences) {
					cbean.getContextButton().setParent(termsGroup);
					cbean.getContextButton().setSelection(false);
					cbean.getFrequency().setParent(termsGroup);
					if (cbean.getTerm1() != null){
						if (cbean.getTerm1().isTogglePosition()) {
							cbean.getTerm1().getTermGroup().setParent(termsGroup);
							cbean.getTerm1().setParentGroup(termsGroup);
							cbean.getTerm1().setDeletedGroup(removedTermsGroup);
						} else {
							cbean.getTerm1().getTermGroup().setParent(removedTermsGroup);
							cbean.getTerm1().setParentGroup(termsGroup);
							cbean.getTerm1().setDeletedGroup(removedTermsGroup);
						}
					}

					if (cbean.getTerm2() != null) {
						if (cbean.getTerm2().isTogglePosition()) {
							cbean.getTerm2().getTermGroup().setParent(termsGroup);
							cbean.getTerm2().setParentGroup(termsGroup);
							cbean.getTerm2().setDeletedGroup(removedTermsGroup);
						} else {
							cbean.getTerm2().getTermGroup().setParent(removedTermsGroup);
							cbean.getTerm2().setParentGroup(termsGroup);
							cbean.getTerm2().setDeletedGroup(removedTermsGroup);
						}
					}

				}
			}
			
			/*Resize the groups*/
			termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
				/** Show the correct sort order image */		
				if(sortedBy[selectionIndex]) {
					sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/down.jpg"));			
				} else {
					sortLabel.setImage(SWTResourceManager.getImage(MainForm.class, "/fna/parsing/up.jpg"));	
				
			}
			
		}
		
	}
	

	private void loadProcessedGroups() {
		try {
			ArrayList<String> processedGroupsList = charDb.getProcessedGroups();
			processedGroupsTable.removeAll();
			for (String groupName : processedGroupsList){
				processedGroups.put(groupName, groupName);
				TableItem item = new TableItem(processedGroupsTable, SWT.NONE);
				item.setText(groupName);
				
			}
		} catch (Exception exe) {
			LOGGER.error("Couldnt retrieve processedGroups terms" , exe);
			exe.printStackTrace();
		}
	}
	
	private void showTerms() {
		ArrayList<TermsDataBean> terms = null;
		boolean saved = false;
		term1.y = 10;
		term2.y = 10;
		contextRadio.y = 20;
		frequencyLabel.y = 20;
		ArrayList<CoOccurrenceBean> cooccurrences = new ArrayList<CoOccurrenceBean>();
		String decision = "";

		termsGroup = null;
		termsGroup = new Group(termsScrolledComposite, SWT.NONE);
		termsGroup.setLayoutData(new RowData());
		termsScrolledComposite.setContent(termsGroup);
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		removedTermsGroup = null;
		removedTermsGroup = new Group(removedScrolledComposite, SWT.NONE);
		removedTermsGroup.setLayoutData(new RowData());
		removedScrolledComposite.setContent(removedTermsGroup);
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				
		try {
			terms = charDb.getTerms(groupsCombo.getText());
			if(terms!=null && terms.size() != 0) {
					int groupId = ((TermsDataBean)terms.get(0)).getGroupId();
					decision = charDb.getDecision(groupId);
			}
			
		} catch (Exception exe) {
			LOGGER.error("Couldnt retrieve co-occurring terms" , exe);
			exe.printStackTrace();
		}

		if (terms!=null && terms.size() > 5) {
			
			RowData rowdata = (RowData)termsGroup.getLayoutData();
			rowdata.height = terms.size() * 36;
			termsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        Rectangle rect = termsGroup.getBounds();
	        rect.height = terms.size() * 36;
	        termsGroup.setBounds(rect);
			
			
			rowdata = (RowData)removedTermsGroup.getLayoutData();
			rowdata.height = terms.size() * 36;
			removedTermsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        rect = removedTermsGroup.getBounds();
	        rect.height = terms.size() * 36;
	        removedTermsGroup.setBounds(rect);
			
		}
		unpaired = false;
		if(unPairedTerms(terms)){
			unpaired = true;
		}
		
		if (terms!=null && terms.size() != 0) {
			int radio_button_count=0;
			for (final TermsDataBean tbean : terms) {
				radio_button_count+=1;
				CoOccurrenceBean cbean = new CoOccurrenceBean();
				if (!(tbean.getTerm1() == null) && !tbean.getTerm1().equals("")) {
					Group term1Group = new Group(termsGroup, SWT.NONE);
					term1Group.setToolTipText(tbean.getTerm1());
					term1Group.setBounds(term1.x, term1.y, term1.width, term1.height);
					cbean.setTerm1(new TermBean(term1Group, removedTermsGroup, true, tbean.getTerm1()));
				}
				
				if (!(tbean.getTerm2() == null) && !tbean.getTerm2().equals("")) {
					Group term2Group = new Group(termsGroup, SWT.NONE);	
					term2Group.setToolTipText(tbean.getTerm2());
					term2Group.setBounds(term2.x, term2.y, term2.width, term2.height);
					cbean.setTerm2(new TermBean(term2Group, removedTermsGroup, true, tbean.getTerm2()));
				}else if(Integer.parseInt(groupsCombo.getText().replaceFirst("Group_", "")) ==noOfTermGroups && unpaired){ //fill combo boxes in place of term2 for the last group of the terms
					Text term1decision = new Text(termsGroup, SWT.BORDER);
					term1decision.setBounds(term2.x, term2.y+5, term2.width, term2.height-10);
					term1decision.setToolTipText("select a category for the term");
					term1decision.setEditable(true);
					cbean.setText(term1decision);
				}
				
				cbean.setGroupNo(tbean.getGroupId());
				cbean.setSourceFiles(tbean.getSourceFiles());
				cbean.setKeep(tbean.getKeep());
				
				final Button button = new Button(termsGroup, SWT.RADIO);
				//button.setText("radio_"+radio_button_count);
				button.setBounds(contextRadio.x, contextRadio.y, contextRadio.width, contextRadio.height);
				button.setToolTipText("Select to see the context sentences");
				button.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						if (button.getSelection()) {
							contextTable.removeAll();
							try {
								//first show glossary defintions for the two terms in a pair
								String t1 = tbean.getTerm1();
								String t2 = tbean.getTerm2();
								if(conn == null){
									Class.forName("com.mysql.jdbc.Driver");
									String URL = ApplicationUtilities.getProperty("database.url");
									conn = DriverManager.getConnection(URL);
								}								
								String ch1 = fna.charactermarkup.Utilities.lookupCharacter(t1, conn, fna.charactermarkup.ChunkedSentence.characterhash, glossaryPrefixCombo.getText().trim(), dataPrefixCombo.getText().replaceAll("-", "_").trim());
								String ch2 = fna.charactermarkup.Utilities.lookupCharacter(t2, conn, fna.charactermarkup.ChunkedSentence.characterhash, glossaryPrefixCombo.getText().trim(), dataPrefixCombo.getText().replaceAll("-", "_").trim());
								ArrayList<ContextBean> contexts = charDb.getContext(tbean.getSourceFiles());
								TableItem item = new TableItem(contextTable, SWT.NONE);
								item.setText(new String[]{t1+":", ch1});
								item = new TableItem(contextTable, SWT.NONE);
								item.setText(new String[]{t2+":", ch2});
								//then show source sentences for the two terms
								
								for (ContextBean cbean : contexts){
									item = new TableItem(contextTable, SWT.NONE);
									//@TODO: style text here:	not possible using tableItem
									item.setText(new String[]{cbean.getSourceText(), cbean.getSentence()});
								}
								
								
							} catch (Exception exe) {
								LOGGER.error("Couldnt retrieve sentences terms" , exe);
								exe.printStackTrace();
							}
							
						}

					}
				});

				cbean.setContextButton(button);
				if (decision != null && !decision.equals("")){
					comboDecision.setText(decision);
					saved = true;
				}
				
				label = new Label(termsGroup, SWT.NONE);
				label.setBounds(frequencyLabel.x, frequencyLabel.y, frequencyLabel.width, frequencyLabel.height);
				label.setText(tbean.getFrequency()+ "");
				label.setToolTipText("Frequency of co-occurrence");
				cbean.setFrequency(label);
				cooccurrences.add(cbean);
				
				term1.y += standardIncrement;
				term2.y += standardIncrement;
				contextRadio.y += standardIncrement;
				frequencyLabel.y += standardIncrement;
			}
		}
		
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		CharacterGroupBean charGrpBean = new CharacterGroupBean(cooccurrences, groupsCombo.getText(), saved);
		groupInfo.put(groupsCombo.getText(), charGrpBean);
	}
	
	/**
	 * check and see if Terms contains only term1s
	 * @param terms
	 * @return
	 */
	private boolean unPairedTerms(ArrayList<TermsDataBean> terms) {
		if(terms==null) return false;
		Iterator<TermsDataBean> it = terms.iterator();
		while(it.hasNext()){
			TermsDataBean tdb = it.next();
			if(!(tdb.getTerm2()==null || tdb.getTerm2().length()==0)){
				return false;
			}
		}
		return true;
	}

	/**
	 * @return the groupInfo
	 */
	public static HashMap<String, CharacterGroupBean> getGroupInfo() {
		return groupInfo;
	}
	
	private void restoreUnsavedEdges(){
		String group = groupsCombo.getText();
		if (!groupInfo.get(group).isSaved()) {
			ArrayList <String> edges = removedEdges.get(group);
			if (edges != null) {
				for (String edgeNodes : edges){
					String [] nodes = edgeNodes.split(",");
					if(nodes[0] != null && !nodes[0].equals("") && nodes[1] != null && !nodes[1].equals("") ) {
						ManipulateGraphML.insertEdge(new GraphNode(nodes[0]), new GraphNode(nodes[1]), 
								Registry.TargetDirectory+System.getProperty("file.separator")+
									ApplicationUtilities.getProperty("CHARACTER-STATES")+ System.getProperty("file.separator")+ group + ".xml");
					}

				}
			}
	
		}
	}

	/**
	 * @return the removedEdges
	 */
	public static HashMap<String, ArrayList<String>> getRemovedEdges() {
		return removedEdges;
	}
	
	/* This function loads the files, if any, to the respective tabs*/
	private void loadFileInfo(Table table, String directoryPath){
		File directory = new File(directoryPath);
		File [] files = directory.listFiles();
		int count = 0;
		int [] fileNumbers = new int[files.length];
		/* Will need to change this logic if the filenames are no long numbers but strings */
		for (File file : files) {
			String fileName = file.getName();
			fileNumbers[count++] = Integer.parseInt(fileName.substring(0, fileName.indexOf(".xml")));
		}
		
		Arrays.sort(fileNumbers);
		
		for (int fileNumber : fileNumbers) {			
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(new String [] {fileNumber+"", fileNumber+".xml"});
		}
		
		
	}
	
	//removedTermsGroups
	//0. Copy and create a removed terms list
	//1. Check if there are terms remaining - a function that returns true/false
	//2. Create a new group with the number one more than the existing groups.
	//3. Create an xml for the group in 2
	//4. Prepare the GUI for displaying the terms : 
	//      a) Ignore repeated terms 
	//      b)Ignore if terms were reinserted in some existing groups
	//5. Save operation - call the same function as before but create a termsdatabean and pass it along.
	//6. Go to 1
	
	/**
	 * This function creates remaining terms group for the character tab,
	 * as a unpaired group
	 */
	private void showRemainingTerms() {
		
		ArrayList<TermsDataBean> terms = null;
		term1.y = 10;
		term2.y = 10;
		contextRadio.y = 20;
		frequencyLabel.y = 20;
		ArrayList<CoOccurrenceBean> cooccurrences = new ArrayList<CoOccurrenceBean>();
		String decision = "";

		termsGroup = null;
		termsGroup = new Group(termsScrolledComposite, SWT.NONE);
		termsGroup.setLayoutData(new RowData());
		termsScrolledComposite.setContent(termsGroup);
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		removedTermsGroup = null;
		removedTermsGroup = new Group(removedScrolledComposite, SWT.NONE);
		removedTermsGroup.setLayoutData(new RowData());
		removedScrolledComposite.setContent(removedTermsGroup);
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		
		int newGroupNumber = groupsCombo.getItemCount()+1;
		String newGroup = "Group_" + newGroupNumber;
		groupsCombo.add(newGroup);
		groupsCombo.setText(newGroup);
		groupsCombo.select(groupsCombo.getItemCount()-1);
		/*Generate the graph XML*/
		ArrayList <ArrayList<ArrayList<String>>> groups = null;
		/* Create the arraylist to create new terms list*/
		terms = getRemovedTerms(newGroupNumber);	
		/* Create the arraylist for Graph Visualization*/
		groups = createGraphML(terms);
		/* Create the GraphML */
		new GraphMLOutputter(false).output(groups, newGroupNumber);
		/* Create an entry in the removedEdges hashmap for the new group*/
		removedEdges.put(newGroup, new ArrayList<String>());
		/* Add an entry to the sort order */
		boolean [] newSortedBy = new boolean [sortedBy.length+1];
		for(int i = 0 ; i< sortedBy.length; i++) {
			newSortedBy[i] = sortedBy[i];
		}
		sortedBy = newSortedBy;

		if (terms.size() > 5) {
			RowData rowdata = (RowData)termsGroup.getLayoutData();
			rowdata.height = terms.size() * 36;
			termsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        Rectangle rect = termsGroup.getBounds();
	        rect.height = terms.size() * 36;
	        termsGroup.setBounds(rect);
			
			
			rowdata = (RowData)removedTermsGroup.getLayoutData();
			rowdata.height = terms.size() * 36;
			removedTermsGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
	        rect = removedTermsGroup.getBounds();
	        rect.height = terms.size() * 36;
	        removedTermsGroup.setBounds(rect);
			
		}
		
		unpaired = false;
		if(unPairedTerms(terms)){
			unpaired = true;
		}
		
		if (terms.size() != 0) {
			
			for (final TermsDataBean tbean : terms) {
				CoOccurrenceBean cbean = new CoOccurrenceBean();
				if (!(tbean.getTerm1() == null) && !tbean.getTerm1().equals("")) {
					
				Group term1Group = new Group(termsGroup, SWT.NONE);
				term1Group.setToolTipText(tbean.getTerm1());
				term1Group.setBounds(term1.x, term1.y, term1.width, term1.height);
				cbean.setTerm1(new TermBean(term1Group, removedTermsGroup, true, tbean.getTerm1()));

				}
				
				if (!(tbean.getTerm2() == null) && !tbean.getTerm2().equals("")) {
					Group term2Group = new Group(termsGroup, SWT.NONE);	
					term2Group.setToolTipText(tbean.getTerm2());
					term2Group.setBounds(term2.x, term2.y, term2.width, term2.height);
					cbean.setTerm2(new TermBean(term2Group, removedTermsGroup, true, tbean.getTerm2()));
				}else if(Integer.parseInt(groupsCombo.getText().replaceFirst("Group_", "")) >=noOfTermGroups && unpaired){ //fill combo boxes in place of term2 for the last group of the terms
					Text term1decision = new Text(termsGroup, SWT.BORDER);
					term1decision.setBounds(term2.x, term2.y+5, term2.width, term2.height-10);
					term1decision.setToolTipText("select a category for the term");
					term1decision.setEditable(true);
					cbean.setText(term1decision);
				}
				
				cbean.setGroupNo(tbean.getGroupId());
				cbean.setSourceFiles(tbean.getSourceFiles());
				cbean.setKeep(tbean.getKeep());
				
				final Button button = new Button(termsGroup, SWT.RADIO);
				button.setBounds(contextRadio.x, contextRadio.y, contextRadio.width, contextRadio.height);
				button.setToolTipText("Select to see the context sentences");
				button.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(final SelectionEvent e) {
						if (button.getSelection()) {
							contextTable.removeAll();
							try {
								//first show glossary defintions for the two terms in a pair
								String t1 = tbean.getTerm1();
								String t2 = tbean.getTerm2();
								if(conn == null){
									Class.forName("com.mysql.jdbc.Driver");
									String URL = ApplicationUtilities.getProperty("database.url");
									conn = DriverManager.getConnection(URL);
								}								
								String ch1 = fna.charactermarkup.Utilities.lookupCharacter(t1, conn, fna.charactermarkup.ChunkedSentence.characterhash, glossaryPrefixCombo.getText().trim(), dataPrefixCombo.getText().replaceAll("-", "_").trim());
								String ch2 = fna.charactermarkup.Utilities.lookupCharacter(t2, conn, fna.charactermarkup.ChunkedSentence.characterhash, glossaryPrefixCombo.getText().trim(), dataPrefixCombo.getText().replaceAll("-", "_").trim());
								ArrayList<ContextBean> contexts = charDb.getContext(tbean.getSourceFiles());
								TableItem item = new TableItem(contextTable, SWT.NONE);
								item.setText(new String[]{t1+":", ch1});
								item = new TableItem(contextTable, SWT.NONE);
								item.setText(new String[]{t2+":", ch2});
								//then show source sentences for the two terms
								for (ContextBean cbean : contexts){
									item = new TableItem(contextTable, SWT.NONE);
									item.setText(new String[]{cbean.getSourceText(), cbean.getSentence()});
								}								
								
							} catch (Exception exe) {
								LOGGER.error("Couldnt retrieve sentences terms" , exe);
								exe.printStackTrace();
							}
							
						}

					}
				});

				cbean.setContextButton(button);
				if (decision != null && !decision.equals("")){
					comboDecision.setText(decision);
				}
				
				Label label = new Label(termsGroup, SWT.NONE);
				label.setBounds(frequencyLabel.x, frequencyLabel.y, frequencyLabel.width, frequencyLabel.height);
				label.setText(tbean.getFrequency()+ "");
				label.setToolTipText("Frequency of co-occurrence");
				cbean.setFrequency(label);
				cooccurrences.add(cbean);
				
				term1.y += standardIncrement;
				term2.y += standardIncrement;
				contextRadio.y += standardIncrement;
				frequencyLabel.y += standardIncrement;
			}
		}
		
		termsScrolledComposite.setMinSize(termsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		removedScrolledComposite.setMinSize(removedTermsGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		CharacterGroupBean charGrpBean = new CharacterGroupBean(cooccurrences, groupsCombo.getText(), false);
		groupInfo.put(groupsCombo.getText(), charGrpBean);
		
		/* Save the newly formed group to db*/
		try {
			charDb.saveTerms(terms);
		} catch(Exception exe){
			exe.printStackTrace();
		}
	}
	
	/***
	 * This function creates the graph for the remaining character tab terms
	 * @param terms
	 * @return
	 */
	private ArrayList<ArrayList<ArrayList<String>>> createGraphML(ArrayList<TermsDataBean> terms) {
		ArrayList<ArrayList<ArrayList<String>>> group = new ArrayList<ArrayList<ArrayList<String>>>();
		ArrayList<ArrayList<String>> groups = new ArrayList<ArrayList<String>>();
		for (TermsDataBean tbean : terms){
			ArrayList<String> coTerms = new ArrayList<String>();
			if(tbean.getTerm1() != null) {
				coTerms.add(tbean.getTerm1());
			} else {
				coTerms.add("");
			}
			if(tbean.getTerm2() != null) {
				coTerms.add(tbean.getTerm2());
			} else {
				coTerms.add("");
			}
			if(coTerms.size() != 0){
				groups.add(coTerms);
			}
		}
		group.add(groups);
		return group;
	}
	
	/***
	 * This function will get all the removed terms and form the pool
	 * @param groupNo
	 * @return
	 */
	private ArrayList<TermsDataBean> getRemovedTerms(int groupNo) {
		ArrayList <TermsDataBean> terms = new ArrayList<TermsDataBean>();

			Set <String> keys = groupInfo.keySet();
			for (String key : keys){	
				CharacterGroupBean charBean = groupInfo.get(key);
				if (charBean.isSaved()){
					terms.addAll(getRemovedTermsInformation (charBean, groupNo));
				}
				
			}			

		
		return terms;
	}
	
	/**
	 * This is a helper internal function that gets the specific removed 
	 * terms information from the character tab's removed terms'
	 * Remove duplicate words, remove words already in glossary, and sort all words in one colume, i.e. term1
	 * @param charGroupBean
	 * @param groupNo
	 * @return
	 */
	private ArrayList <TermsDataBean> getRemovedTermsInformation (CharacterGroupBean charGroupBean, int groupNo){
		
		ArrayList <TermsDataBean> terms = new ArrayList<TermsDataBean>();	
		ArrayList <CoOccurrenceBean> cooccurrences = charGroupBean.getCooccurrences();
		String words = "";
		for (CoOccurrenceBean  bean : cooccurrences) {
			
			if(bean.getTerm1() != null) {
				if (!bean.getTerm1().isTogglePosition()) {
					String t1 = bean.getTerm1().getTermText().getText();
					words = words.replaceFirst("\\|$", "");
					if(!t1.matches("("+words+")") && !Utilities.inGlossary(t1, conn, MainForm.glossaryPrefixCombo.getText(), MainForm.dataPrefixCombo.getText())){
						words +=t1+"|";
						TermsDataBean tbean = new TermsDataBean();
						tbean.setFrequency(Integer.parseInt(bean.getFrequency().getText()));
						tbean.setSourceFiles(bean.getSourceFiles());
						tbean.setGroupId(groupNo);
						tbean.setTerm1(t1);
						tbean.setTerm2("");
						terms.add(tbean);
					}
					// Remove the term from the original group of removed terms
					bean.setTerm1(null);
				}
		    }

			if(bean.getTerm2() != null) {
				if (!bean.getTerm2().isTogglePosition()) {
					String t2 = bean.getTerm2().getTermText().getText();
					words = words.replaceFirst("\\|$", "");
					if(!t2.matches("("+words+")")&& !Utilities.inGlossary(t2, conn, MainForm.glossaryPrefixCombo.getText(), MainForm.dataPrefixCombo.getText())){
						words +=t2+"|";
						TermsDataBean tbean = new TermsDataBean();
						tbean.setFrequency(Integer.parseInt(bean.getFrequency().getText()));
						tbean.setSourceFiles(bean.getSourceFiles());
						tbean.setGroupId(groupNo);
						tbean.setTerm1(t2);
						tbean.setTerm2("");
						terms.add(tbean);
					}
					// Remove the term from the original group of removed terms
					bean.setTerm2(null);
				}
		    }
		}

		return terms;
	}
	
	/**
	 * This is a helper internal function that gets the specific removed 
	 * terms information from the character tab's removed terms'
	 * @param charGroupBean
	 * @param groupNo
	 * @return
	 */
	/*private ArrayList <TermsDataBean> getRemovedTermsInformation (CharacterGroupBean charGroupBean, int groupNo){
		
		ArrayList <TermsDataBean> terms = new ArrayList<TermsDataBean>();	
		ArrayList <CoOccurrenceBean> cooccurrences = charGroupBean.getCooccurrences();
		
		for (CoOccurrenceBean  bean : cooccurrences) {
			
			TermsDataBean tbean = new TermsDataBean();
			tbean.setFrequency(Integer.parseInt(bean.getFrequency().getText()));
			tbean.setSourceFiles(bean.getSourceFiles());
			tbean.setGroupId(groupNo);
			if(bean.getTerm1() != null) {
				if (!bean.getTerm1().isTogglePosition()) {
					tbean.setTerm1(bean.getTerm1().getTermText().getText());
					//Remove the term from the original group of removed terms
					bean.setTerm1(null);
					
			    } else {
			    	tbean.setTerm1("");
			    }
			} else {
		    	tbean.setTerm1("");
		    }

			if (bean.getTerm2() != null){
				if(!bean.getTerm2().isTogglePosition()){
					tbean.setTerm2(bean.getTerm2().getTermText().getText());
					// Remove the term from the original group of removed terms
					bean.setTerm2(null);
					
			    } else {
			    	tbean.setTerm2("");
			    }
			} else {
		    	tbean.setTerm2("");
		    }

			if (tbean.getTerm1() != null && tbean.getTerm2() != null) {
				if (!tbean.getTerm1().equals("")|| 
						!tbean.getTerm2().equals("")) {
					terms.add(tbean);
				}
			}
			
		}
		
		return terms;
	}*/
	
	/**
	 * This function checks how 
	 * many groups were saved
	 * @return
	 */  	
	private int getNumberOfGroupsSaved(){
		int returnValue = 0;
		Set <String>
		keys = groupInfo.keySet();
		for (String key : keys){
			CharacterGroupBean charGrpBean = groupInfo.get(key);
			if(charGrpBean.isSaved()) {
				returnValue ++;
			}
		}
		return returnValue;
	}
	
	/**
	 * This function checks if there are terms remaining that are not yet grouped.
	 * @return boolean
	 */
	private boolean isTermsNotGrouped(){
		boolean returnValue = false;
		Set <String>
		keys = groupInfo.keySet();
		for (String key : keys){
			CharacterGroupBean charGrpBean = groupInfo.get(key);
			ArrayList <CoOccurrenceBean> cooccurrences = charGrpBean.getCooccurrences();
			for (CoOccurrenceBean  bean : cooccurrences) {
				if((bean.getTerm1() != null && !bean.getTerm1().isTogglePosition()) ||
						(bean.getTerm2() != null && !bean.getTerm2().isTogglePosition())){
						returnValue = true;
						break;
				    } 
				}
			if(returnValue) {
				break;
			}
		}
		return returnValue;
	}
	
	private int getType(String type) {
		
		if(type.trim().equalsIgnoreCase(""))
			return 1;
		
		if(type.equals("type2"))
			return 2;
		
		if(type.equals("type3"))
			return 3;
		
		if(type.equals("type4"))
			return 4;
		else
			return 0;
	}
	

	
	/**
	 * this procedure seems to be slow and only a handful of terms are filtered.
	 * 1. search db for candidate structure terms
	 * 2. apply heuristic rules to filter the terms
	 * 		2.1 pos = v|adv
	 * 		2.2 does not ...
	 * 		2.3 by [means] of
	 * 3. filtered terms are not displayed and they will not be saved to wordroles table as "os" or "op".
	 * 4. terms filtered by 2.1.adv or 2.3 will be saved in NONEQTERMSTABLE
	 * 5. cache results to reduce cost
	 * @param contextText 
	 * @return filtered candidate structure words
	 */
	private ArrayList<String> fetchStructureTerms(StyledText contextText){
		ArrayList <String> words = new ArrayList<String>();
		ArrayList <String> filteredwords = new ArrayList<String>();
		ArrayList <String> noneqwords = new ArrayList<String>();
		try{
			if(conn == null){
				Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
				conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			}
			String prefix = dataPrefixCombo.getText().replaceAll("-", "_").trim();
			VolumeMarkupDbAccessor vmdb = new VolumeMarkupDbAccessor(prefix,glossaryPrefixCombo.getText().trim());
			words = vmdb.structureTags4Curation(words);
			for(String word: words){
				//before structure terms are set, partOfPrepPhrases can not be reliability determined
				if(Utilities.mustBeVerb(word, this.conn, prefix) || Utilities.mustBeAdv(word) /*|| TermOutputerUtilities.partOfPrepPhrase(word, this.conn, prefix)*/){
					//if(TermOutputerUtilities.mustBeAdv(word) /*|| TermOutputerUtilities.partOfPrepPhrase(word, this.conn, prefix)*/){
						noneqwords.add(word);
						contextText.append(word+" is excluded\n");						
						//sentences with those tags should be marked as unknown for later review
					//}					
					continue;
				}
				filteredwords.add(word);
			}
			mainDb.recordNonEQTerms(noneqwords, null, null);
			mainDb.setUnknownTags(noneqwords);
			words = null;
		}catch(Exception e){
			e.printStackTrace();
		}
		conn = null;
		return filteredwords;
	}

	/**
	 * 1. search db for candidate character terms
	 * 2. apply heuristic rules to filter the terms
	 * 		2.1 pos = adv
	 * 		2.2 by [means] of
	 * 3. filtered terms are not displayed and they will not be saved to wordroles table as "os" or "op".
	 * 4. terms filtered by 2.1.adv or 2.2 will be saved in NONEQTERMSTABLE
	 * 5. cache results to reduce cost
	 * @param contextText 
	 * @return filtered candidate character words
	 */
	private ArrayList<String> fetchCharacterTerms(StyledText contextText){
		ArrayList <String> words = new ArrayList<String>();;
		ArrayList <String> filteredwords = new ArrayList<String>();
		ArrayList <String> noneqwords = new ArrayList<String>();
		try{
			if(conn == null){
				Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
				conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			}
			String prefix = dataPrefixCombo.getText().replaceAll("-", "_").trim();
			VolumeMarkupDbAccessor vmdb = new VolumeMarkupDbAccessor(prefix, glossaryPrefixCombo.getText().trim());
			words = (ArrayList<String>)vmdb.descriptorTerms4Curation();
			for(String word: words){
				if(Utilities.mustBeVerb(word, conn, prefix) || Utilities.mustBeAdv(word) || Utilities.partOfPrepPhrase(word, this.conn, prefix)){
					noneqwords.add(word);
					contextText.append(word+" is excluded\n");
					continue;
				}
				filteredwords.add(word);
			}
			mainDb.recordNonEQTerms(noneqwords, null, null);
			words = null;
			
		}catch(Exception e){
			e.printStackTrace();
		}
		conn = null;
		return filteredwords;	
		}
	
	private ArrayList<String> fetchContentTerms(StyledText contextText) {
		ArrayList<String> words = new ArrayList<String>();
		ArrayList <String> filteredwords = new ArrayList<String>();
		ArrayList <String> noneqwords = new ArrayList<String>();
		try{
			
			VolumeMarkupDbAccessor vmdb = new VolumeMarkupDbAccessor(dataPrefixCombo.getText().replaceAll("-", "_").trim(),glossaryPrefixCombo.getText().trim());
			if(inistructureterms==null || inistructureterms.size()==0){
				inistructureterms = vmdb.structureTags4Curation(new ArrayList<String>());
			}
			if(inicharacterterms==null || inicharacterterms.size()==0){
				inicharacterterms = vmdb.descriptorTerms4Curation();
			}
			words=(ArrayList<String>)vmdb.contentTerms4Curation(words, inistructureterms, inicharacterterms);
			if(conn == null){
				Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
				conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			}
			String prefix = dataPrefixCombo.getText().replaceAll("-", "_").trim();
			for(String word: words){
				if(Utilities.mustBeVerb(word, conn, prefix) || Utilities.mustBeAdv(word) || Utilities.partOfPrepPhrase(word, this.conn, prefix)){
					noneqwords.add(word);
					contextText.append(word+" is excluded\n");
					continue;
				}
				filteredwords.add(word);
			}
			mainDb.recordNonEQTerms(noneqwords, null, null);
			words = null;
		}catch(Exception e){
			e.printStackTrace();
		}
		return filteredwords;
	}
	
	protected void reLoadTermArea(Composite termRoleMatrix, ScrolledComposite scrolledComposite, final StyledText contextText, final String type){
		int count = 0;
		try {
			if(termRoleMatrix.isDisposed()){
				ApplicationUtilities.showPopUpWindow(
						"Term categorization has been saved and the process can not be redone.", 
						ApplicationUtilities.getProperty("popup.header.info"), SWT.ICON_INFORMATION);
				return;
			}
			final int y = 10; //height of a row
			int m = 1; //vertical margin
			Hashtable <String, String> words = null;
			Hashtable<String, String> categorizedterms = null;
			if(type.compareToIgnoreCase("structures") ==0){
				words = categorizedtermsS;
				categorizedterms = categorizedtermsS; //the global variable categorizedtermsS is populated when the local variable thiscategorizedterms is populated below
			}
			if(type.compareToIgnoreCase("characters") ==0){
				words = categorizedtermsC;
				categorizedterms = categorizedtermsC;
			}
			if(type.compareToIgnoreCase("others") ==0){
				words = categorizedtermsO;
				categorizedterms = categorizedtermsO;
			}
			termRoleMatrix.setSize(744, words.size()*y);
			scrolledComposite.setContent(termRoleMatrix);
			termRoleMatrix.setVisible(true);
			final Hashtable<String, String> thiscategorizedterms = categorizedterms;
			if (words != null) {
				ArrayList<Control> tabList = new ArrayList<Control>();
				Enumeration<String> en = words.keys();
				while(en.hasMoreElements()){
					String word = en.nextElement();
					String cat = words.get(word);
					thiscategorizedterms.put(word, type); //populate term list 
					count++;					
					final Composite termRoleGroup = new Composite(termRoleMatrix, SWT.NONE);
					termRoleGroup.setLayoutData(new RowLayout(SWT.HORIZONTAL));	
					if(count % 2 == 0){
						termRoleGroup.setBackground(grey);
					}
					termRoleGroup.setBounds(0, (count-1)*y, 744, y);
					//show context info				
					termRoleGroup.addMouseListener(new MouseListener(){
						@Override
						public void mouseDoubleClick(MouseEvent e) {}
						@Override
						public void mouseDown(MouseEvent e) {
							Control[] controls = termRoleGroup.getChildren();
							if(controls[1] instanceof Label){
								String term = ((Label)controls[1]).getText().trim();
				  				try {
				  					contextText.setText("");
				  					contextText.setTopMargin(2);
									mainDb.getContextData(term, contextText);
								} catch (ParsingException e1) {
									e1.printStackTrace();
								} catch (SQLException e1) {
									e1.printStackTrace();
								}	
							}
						}
						@Override
						public void mouseUp(MouseEvent e) {}
					});
					
					Label clabel = new Label(termRoleGroup, SWT.NONE);
					clabel.setText(" "+count);
					if(count%2 == 0) clabel.setBackground(grey);
					clabel.setBounds(15, (count-1)*y+m, 93, y-2*m);
					
					
					
					Label tlabel = new Label(termRoleGroup, SWT.NONE);
					tlabel.setText(word);
					if(count%2 == 0) tlabel.setBackground(grey);
					tlabel.setBounds(125, (count-1)*y+m, 150, y-2*m);
					

						
					final Button button_1 = new Button(termRoleGroup, SWT.RADIO);
					button_1.setBounds(325, (count-1)*y+m, 90, y-2*m);			
					if(cat.compareToIgnoreCase("structures")==0) button_1.setSelection(true);
					if(count%2 == 0) button_1.setBackground(grey);
					tabList.add(button_1);
					button_1.addListener(SWT.Selection, new Listener() {
					      public void handleEvent(Event e) {
					    	  Control[] controls = button_1.getParent().getChildren();
					    	  if(controls[1] instanceof Label){
					    		 String term = ((Label)controls[1]).getText().trim();
					    		//String term = ((Text)controls[1]).getText().trim();
						    	 thiscategorizedterms.put(term, "structures");
					    	  }
					      }						
					});
					
					final Button button_2 = new Button(termRoleGroup, SWT.RADIO);
					button_2.setBounds(425, (count-1)*y+m, 90, y-2*m);
					//button_2.setSelection(true);//This can't be done. It will waste all the learning perl completed: For use cases where the person who runs charaparser needs another person to review the terms. Here mark all terms as "descriptor" by default so they will all be loaded to OTO for review
					if(cat.compareToIgnoreCase("characters")==0) button_2.setSelection(true);
					if(count%2 == 0) button_2.setBackground(grey);
					tabList.add(button_2);
					button_2.addListener(SWT.Selection, new Listener() {
					      public void handleEvent(Event e) {
					    	  Control[] controls = button_2.getParent().getChildren();
					    	  if(controls[1] instanceof Label){
					    		 String term = ((Label)controls[1]).getText().trim();
					    		 // String term = ((Text)controls[1]).getText().trim();
						    	 thiscategorizedterms.put(term, "characters");
					    	  }
					      }						
					});
					
					final Button button_3 = new Button(termRoleGroup, SWT.RADIO);
					button_3.setBounds(525, (count-1)*y+m, 90, y-2*m);
					if(cat.compareToIgnoreCase("others")==0) button_3.setSelection(true);
					if(count%2 == 0) button_3.setBackground(grey);
					tabList.add(button_3);
					button_3.addListener(SWT.Selection, new Listener() {
					      public void handleEvent(Event e) {
					    	  Control[] controls = button_3.getParent().getChildren();
					    	  if(controls[1] instanceof Label){
					    		 String term = ((Label)controls[1]).getText().trim();
					    		 //String term = ((Text)controls[1]).getText().trim();
						    	 thiscategorizedterms.put(term, "others");
					    	  }
					      }						
					});
					
					Label invisible = new Label(termRoleGroup, SWT.NONE);
					invisible.setBounds(720, (count-1)*y+m, 90, y-2*m);
					invisible.setText("invisible");
					invisible.setVisible(false);
					
					clabel.pack();
					tlabel.pack();
					button_1.pack();
					button_2.pack();
					button_3.pack();
					termRoleGroup.pack();
					termRoleGroup.redraw();
				}
				termRoleMatrix.pack();
				//termRoleMatrix.setTabList(tabList.toArray(new Control[]{}));
			}			
		} catch (Exception exe){
			LOGGER.error("unable to load subtab in Markup : MainForm", exe);
			exe.printStackTrace();
		}
	}
	
	protected void loadTermArea(Composite termRoleMatrix, ScrolledComposite scrolledComposite, ArrayList <String> words, final StyledText contextText, final String type) {
		int count = 0;
		try {
			if(termRoleMatrix.isDisposed()){
				ApplicationUtilities.showPopUpWindow(
						"Term categorization has been saved and the process can not be redone.", 
						ApplicationUtilities.getProperty("popup.header.info"), SWT.ICON_INFORMATION);
				return;
			}
			final int y = 10; //height of a row
			int m = 1; //vertical margin
			termRoleMatrix.setSize(744, words.size()*y);
			scrolledComposite.setContent(termRoleMatrix);
			termRoleMatrix.setVisible(true);
			Hashtable<String, String> categorizedterms = null;
			if(type.compareToIgnoreCase("structures") ==0){
				categorizedterms = categorizedtermsS;
			}
			if(type.compareToIgnoreCase("characters") ==0){
				categorizedterms = categorizedtermsC;
			}
			if(type.compareToIgnoreCase("others") ==0){
				categorizedterms = categorizedtermsO;
			}
			final Hashtable<String, String> thiscategorizedterms = categorizedterms;
			if (words != null) {
				ArrayList<Control> tabList = new ArrayList<Control>();
				for (String word : words){
					thiscategorizedterms.put(word, type);
					count++;					
					final Composite termRoleGroup = new Composite(termRoleMatrix, SWT.NONE);
					termRoleGroup.setLayoutData(new RowLayout(SWT.HORIZONTAL));	
					if(count % 2 == 0){
						termRoleGroup.setBackground(grey);
					}
					termRoleGroup.setBounds(0, (count-1)*y, 744, y);
					//show context info				
					termRoleGroup.addMouseListener(new MouseListener(){
						@Override
						public void mouseDoubleClick(MouseEvent e) {}
						@Override
						public void mouseDown(MouseEvent e) {
							Control[] controls = termRoleGroup.getChildren();
							if(controls[1] instanceof Label){
								String term = ((Label)controls[1]).getText().trim();
				  				try {
				  					contextText.setText("");
				  					contextText.setTopMargin(2);
									mainDb.getContextData(term, contextText);
								} catch (ParsingException e1) {
									e1.printStackTrace();
								} catch (SQLException e1) {
									e1.printStackTrace();
								}	
							}
						}
						@Override
						public void mouseUp(MouseEvent e) {}
					});
					
					Label clabel = new Label(termRoleGroup, SWT.NONE);
					clabel.setText(" "+count);
					if(count%2 == 0) clabel.setBackground(grey);
					clabel.setBounds(15, (count-1)*y+m, 93, y-2*m);
					
					Label tlabel = new Label(termRoleGroup, SWT.NONE);
					tlabel.setText(word);
					if(count%2 == 0) tlabel.setBackground(grey);
					tlabel.setBounds(125, (count-1)*y+m, 150, y-2*m);
					
					final Button button_1 = new Button(termRoleGroup, SWT.RADIO);
					button_1.setBounds(325, (count-1)*y+m, 90, y-2*m);
					if(type.compareToIgnoreCase("structures")==0) button_1.setSelection(true);
					if(count%2 == 0) button_1.setBackground(grey);
					tabList.add(button_1);
					button_1.addListener(SWT.Selection, new Listener() {
					      public void handleEvent(Event e) {
					    	  Control[] controls = button_1.getParent().getChildren();
					    	  if(controls[1] instanceof Label){
					    		 String term = ((Label)controls[1]).getText().trim();
						    	 thiscategorizedterms.put(term, "structures");
					    	  }
					      }						
					});
					
					final Button button_2 = new Button(termRoleGroup, SWT.RADIO);
					button_2.setBounds(425, (count-1)*y+m, 90, y-2*m);
					if(type.compareToIgnoreCase("characters")==0) button_2.setSelection(true);
					if(count%2 == 0) button_2.setBackground(grey);
					tabList.add(button_2);
					button_2.addListener(SWT.Selection, new Listener() {
					      public void handleEvent(Event e) {
					    	  Control[] controls = button_2.getParent().getChildren();
					    	  if(controls[1] instanceof Label){
					    		 String term = ((Label)controls[1]).getText().trim();
						    	 thiscategorizedterms.put(term, "characters");
					    	  }
					      }						
					});
					
					final Button button_3 = new Button(termRoleGroup, SWT.RADIO);
					button_3.setBounds(525, (count-1)*y+m, 90, y-2*m);
					if(type.compareToIgnoreCase("others")==0) button_3.setSelection(true);
					if(count%2 == 0) button_3.setBackground(grey);
					tabList.add(button_3);
					button_3.addListener(SWT.Selection, new Listener() {
					      public void handleEvent(Event e) {
					    	  Control[] controls = button_3.getParent().getChildren();
					    	  if(controls[1] instanceof Label){
					    		 String term = ((Label)controls[1]).getText().trim();
						    	 thiscategorizedterms.put(term, "others");
					    	  }
					      }						
					});
					
					Label invisible = new Label(termRoleGroup, SWT.NONE);
					invisible.setBounds(720, (count-1)*y+m, 90, y-2*m);
					invisible.setText("invisible");
					invisible.setVisible(false);
					
					clabel.pack();
					tlabel.pack();
					button_1.pack();
					button_2.pack();
					button_3.pack();
					termRoleGroup.pack();
					termRoleGroup.redraw();
				}
				termRoleMatrix.pack();
				//termRoleMatrix.setTabList(tabList.toArray(new Control[]{}));
			}			
		} catch (Exception exe){
			LOGGER.error("unable to load findMoreStructure subtab in Markup : MainForm", exe);
			exe.printStackTrace();
		}
	}


	
	/**loading structure/descriptor terms for curation**/

	/*protected void loadTermCurationTabs(){
		//loadFindStructureTable();
		//loadFindDescriptorTable();
		//loadFindMoreStructureTable();
		//loadFindMoreDescriptorTable();
		createSubtab(markupNReviewTabFolder, "structures");
		createSubtab(markupNReviewTabFolder, "characters");
		createSubtab(markupNReviewTabFolder, "others");
	}*/
	/*protected int loadFindStructureTable() {
		//ArrayList <String> words = new ArrayList<String>();
		findStructureTable.removeAll();
		int count = 0;
		try {
			ArrayList<String> words = fetchStructureTerms();			

			if (words != null) {
				for (String word : words){
					count++;
					TableItem item = new TableItem(findStructureTable, SWT.NONE);
					item.setText(new String [] {count+"", word});
				}
			}					
		} catch (Exception exe){
			LOGGER.error("unable to load findStructure subtab in Markup : MainForm", exe);
			exe.printStackTrace();
		}
		return count;
	}*/
	/*protected void loadOthersTable() {
		showOtherTerms();
	}*/

	/*protected int loadFindDescriptorTable() {
		// TODO Auto-generated method stub
		//ArrayList <String> words = null;
		findDescriptorTable.removeAll();
		int count = 0;
		try {
			ArrayList<String> words = fetchCharacterTerms();
			if (words != null) {
				for (String word : words){
					count++;
					TableItem item = new TableItem(findDescriptorTable, SWT.NONE);
					item.setText(new String [] {count+"", word});
				}
			}
			
		} catch (Exception exe){
			LOGGER.error("unable to load findDescriptor subtab in Markup : MainForm", exe);
			exe.printStackTrace();
		}
		return count;
	
	}*/


	
	/*protected int loadFindMoreStructureTable() {
		ArrayList <String> words = new ArrayList<String>();
		findMoreStructureTable.removeAll();
		int count = 0;
		try {
			words = fetchContentTerms(words);
			if (words != null) {
				for (String word : words){
					count++;
					TableItem item = new TableItem(findMoreStructureTable, SWT.NONE);
					item.setText(new String [] {count+"", word});
				}
			}					
		} catch (Exception exe){
			LOGGER.error("unable to load findMoreStructure subtab in Markup : MainForm", exe);
			exe.printStackTrace();
		}
		return count;
	}*/
	
	/*protected int loadFindMoreDescriptorTable() {
		ArrayList <String> words = new ArrayList<String>();
		findMoreDescriptorTable.removeAll();
		int count = 0;
		try {
			words = fetchContentTerms(words);
			if (words != null) {
				for (String word : words){
					count++;
					TableItem item = new TableItem(findMoreDescriptorTable, SWT.NONE);
					item.setText(new String [] {count+"", word});
				}
			}					
		} catch (Exception exe){
			LOGGER.error("unable to load findMoreDescriptor subtab in Markup : MainForm", exe);
			exe.printStackTrace();
		}
		return count;
	}*/
	
	private void setType4XML(String schema){
		this.type4xml = schema;
	}

	private String getType4XML(){
		return this.type4xml;
	}
}
