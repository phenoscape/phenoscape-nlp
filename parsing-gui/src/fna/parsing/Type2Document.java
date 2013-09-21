 package fna.parsing;
/** @author Partha Pratim Sanyal ppsanyal@email.arizona.edu*/
import java.sql.SQLException;
import java.util.HashMap;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Combo;
import com.swtdesigner.SWTResourceManager;

import fna.beans.DescriptionBean;
import fna.beans.ExpressionBean;
import fna.beans.NomenclatureBean;
import fna.beans.SectionBean;
import fna.beans.SpecialBean;
import fna.beans.TextBean;
import fna.beans.Type2Bean;
import fna.db.ConfigurationDbAccessor;

public class Type2Document {
	private Text text;
	private Text text_1;
	private Text text_2;
	private Text text_3;
	private Text text_4;
	private Text text_5;
	private Text text_6;
	private Text text_7;
	private Text text_9;
	private Text text_10;
	private Text text_11;
	private Text text_12;
	private Text text_13;
	private Text text_14;
	private Text text_15;
	private Text text_16;
	private Text text_33;
	private Text text_40;
	private Text text_41;
	private Text text_42;
	private Text text_8;
	private Text text_29;

	/* String for user input while adding a row*/
	
	private String identifier = null;
	private Group nomenclatureGroup = null;
	private Group expressionGroup = null;
	private Group descriptionGroup = null;
	private ScrolledComposite nomenScrolledComposite = null;
	private ScrolledComposite expScrolledComposite = null;
	private ScrolledComposite descScrolledComposite = null;
	
	/*The following set of variables will act as beans to hold data for saving in database*/
	/* This HashMap stores the data available from the nomenclature tab */
	private HashMap<Integer, NomenclatureBean> nomenclatures = new HashMap<Integer, NomenclatureBean>();
	/* This HashMap stores the data available from the expression tab */
	private HashMap<Integer, ExpressionBean> expressions = new HashMap<Integer, ExpressionBean>();
	/* This HashMap stores the label data available from the description tab */
	private HashMap<Integer, SectionBean> sections = new HashMap<Integer, SectionBean>();
	/* One comprehensive bean to store all description related data */
	private DescriptionBean descriptionBean = null;
	/* This HashMap store the data from the last tab - Abbreviations */
	private HashMap<String, Text> abbreviations = new HashMap<String, Text>();
	/* This bean will be used to store data from the special tab */
	private SpecialBean special = null;
	/* This bean will store data from Text tab */
	private TextBean textBean = null;
	
	/* This variable will count the number of instances of nomenclature beans on the UI Nomenclature tab */
	private int nomenCount = 0;	
	/* This variable will count the number of instances of nomenclature beans on the UI Expressions tab */
	private int expCount = 0;
	/* This variable will count the number of instances of descriptions - 
	 * section labels on the UI Descriptions tab */
	private int secCount = 0;
	private ConfigurationDbAccessor configDb = new ConfigurationDbAccessor();	
	private Shell shlTypeDocument = null;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Type2Document().showType2Document();

	}
	
    /**
     * @wbp.parser.entryPoint
     */
    public void showType2Document() {
		final Display display = Display.getDefault();
		
		shlTypeDocument = new Shell(display);
		shlTypeDocument.setText("Type 2 Document");
		shlTypeDocument.setSize(780, 634);
		shlTypeDocument.setLocation(display.getBounds().x+200, display.getBounds().y+100);
		
		Composite composite = new Composite(shlTypeDocument, SWT.NONE);
		composite.setBounds(0, 0, 759, 557);
		
		TabFolder tabFolder = new TabFolder(composite, SWT.NONE);
		tabFolder.setBounds(10, 10, 742, 545);
		
		TabItem tbtmText = new TabItem(tabFolder, SWT.NONE);
		tbtmText.setText("Text");
		
		Group grpText = new Group(tabFolder, SWT.NONE);
		grpText.setText("Text");
		tbtmText.setControl(grpText);
		//The first tab!  --> Text
		Label lblLeadingIndentionOf = new Label(grpText, SWT.NONE);
		lblLeadingIndentionOf.setBounds(10, 82, 374, 15);
		lblLeadingIndentionOf.setText("Leading indention of other paragraph:");
		
		text = new Text(grpText, SWT.BORDER);
		text.setBounds(422, 79, 76, 21);
		
		Label lblCharacters = new Label(grpText, SWT.NONE);
		lblCharacters.setBounds(504, 82, 85, 15);
		lblCharacters.setText("characters");
		
		Label lblSpacingBetweenCharacters = new Label(grpText, SWT.NONE);
		lblSpacingBetweenCharacters.setBounds(10, 116, 374, 15);
		lblSpacingBetweenCharacters.setText("Spacing between paragraphs:");
		
		text_1 = new Text(grpText, SWT.BORDER);
		text_1.setBounds(422, 113, 76, 21);
		
		Label lblLines = new Label(grpText, SWT.NONE);
		lblLines.setBounds(504, 116, 85, 15);
		lblLines.setText("lines");
		
		Label lblstParagraph = new Label(grpText, SWT.NONE);
		lblstParagraph.setBounds(10, 47, 374, 15);
		lblstParagraph.setText("1st Paragraph:");
		
		text_2 = new Text(grpText, SWT.BORDER);
		text_2.setBounds(422, 47, 76, 21);
		
		Label label_2 = new Label(grpText, SWT.NONE);
		label_2.setBounds(504, 47, 85, 15);
		label_2.setText("characters");
		
		Label lblEstimatedAverageLengths = new Label(grpText, SWT.NONE);
		lblEstimatedAverageLengths.setBounds(10, 154, 374, 15);
		lblEstimatedAverageLengths.setText("Estimated average length(s) of a line:");
		
		text_3 = new Text(grpText, SWT.BORDER);
		text_3.setBounds(422, 148, 76, 21);
		
		Label label_3 = new Label(grpText, SWT.NONE);
		label_3.setBounds(504, 154, 85, 15);
		label_3.setText("characters");
		
		Label lblPageNumberForms = new Label(grpText, SWT.NONE);
		lblPageNumberForms.setBounds(10, 194, 141, 15);
		lblPageNumberForms.setText("Page number forms:");
		
		text_4 = new Text(grpText, SWT.BORDER);
		text_4.setBounds(161, 191, 477, 21);
		
		Label lblSectionHeadings = new Label(grpText, SWT.NONE);
		lblSectionHeadings.setBounds(10, 237, 141, 15);
		lblSectionHeadings.setText("Section headings:");
		
		Button btnCapitalized = new Button(grpText, SWT.RADIO);
		btnCapitalized.setBounds(169, 236, 90, 16);
		btnCapitalized.setText("Capitalized");
		
		Button btnAllCapital = new Button(grpText, SWT.RADIO);
		btnAllCapital.setBounds(263, 237, 90, 16);
		btnAllCapital.setText("ALL CAPITAL");
		
		text_5 = new Text(grpText, SWT.BORDER);
		text_5.setBounds(366, 231, 272, 21);
		
		Label lblFooterTokens = new Label(grpText, SWT.NONE);
		lblFooterTokens.setBounds(10, 283, 85, 15);
		lblFooterTokens.setText("Footer tokens:");
		
		Button btnHasFooters = new Button(grpText, SWT.CHECK);
		btnHasFooters.setBounds(123, 282, 93, 16);
		btnHasFooters.setText("Has footers");
		
		text_6 = new Text(grpText, SWT.BORDER);
		text_6.setBounds(222, 280, 98, 21);
		
		Label lblHeaderTokens = new Label(grpText, SWT.NONE);
		lblHeaderTokens.setBounds(337, 283, 85, 15);
		lblHeaderTokens.setText("Header tokens:");
		
		Button btnHasHeaders = new Button(grpText, SWT.CHECK);
		btnHasHeaders.setBounds(428, 282, 93, 16);
		btnHasHeaders.setText("Has headers");
		
		text_7 = new Text(grpText, SWT.BORDER);
		text_7.setBounds(523, 277, 98, 21);
		
		textBean = new TextBean(text_2, text, text_1, text_3,
				text_4, btnCapitalized,
				btnAllCapital, text_5, 
				new SpecialBean(btnHasFooters, btnHasHeaders, text_6, text_7));
		
		TabItem tbtmNomenclature = new TabItem(tabFolder, SWT.NONE);
		tbtmNomenclature.setText("Nomenclature");
		
		Group grpNomenclature = new Group(tabFolder, SWT.NONE);
		grpNomenclature.setText("Nomenclature");
		tbtmNomenclature.setControl(grpNomenclature);
		
		Label lblWhatIsIn = new Label(grpNomenclature, SWT.NONE);
		lblWhatIsIn.setBounds(10, 28, 111, 15);
		lblWhatIsIn.setText("What is in a name?");
		
		Label lblFamily = new Label(grpNomenclature, SWT.NONE);
		lblFamily.setBounds(233, 28, 55, 15);
		lblFamily.setText("Family");
		
		Label lblGenus = new Label(grpNomenclature, SWT.NONE);
		lblGenus.setBounds(399, 28, 55, 15);
		lblGenus.setText("Genus");
		
		Label lblSpecies = new Label(grpNomenclature, SWT.NONE);
		lblSpecies.setBounds(569, 28, 55, 15);
		lblSpecies.setText("Species");
		
		nomenScrolledComposite = new ScrolledComposite(grpNomenclature, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		nomenScrolledComposite.setBounds(10, 53, 714, 278);
		nomenScrolledComposite.setExpandHorizontal(true);
		nomenScrolledComposite.setExpandVertical(true);
		
		nomenclatureGroup = new Group(nomenScrolledComposite, SWT.NONE);
		nomenclatureGroup.setLayoutData(new RowData());
		
		Label lblName = new Label(nomenclatureGroup, SWT.NONE);
		lblName.setBounds(10, 20, 75, 15);
		lblName.setText("Name");

		Group group_2 = new Group(nomenclatureGroup, SWT.NONE);
		group_2.setBounds(100, 10, 182, 40);
		
		Button button = new Button(group_2, SWT.RADIO);
		button.setText("Yes");
		button.setBounds(10, 13, 39, 16);
		
		Button button_1 = new Button(group_2, SWT.RADIO);
		button_1.setText("No");
		button_1.setBounds(55, 13, 39, 16);
		
		text_14 = new Text(group_2, SWT.BORDER);
		text_14.setBounds(100, 11, 76, 21);
		nomenclatures.put(new Integer(nomenCount), new NomenclatureBean(group_2, button, button_1, text_14, lblName));
		nomenCount++;

		Group group_1 = new Group(nomenclatureGroup, SWT.NONE);
		group_1.setBounds(300, 10, 182, 40);
		
		Button button_2 = new Button(group_1, SWT.RADIO);
		button_2.setText("Yes");
		button_2.setBounds(10, 13, 39, 16);
		
		Button button_3 = new Button(group_1, SWT.RADIO);
		button_3.setText("No");
		button_3.setBounds(55, 13, 39, 16);
		
		text_15 = new Text(group_1, SWT.BORDER);
		text_15.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(nomenCount), new NomenclatureBean(group_1, button_2, button_3, text_15, lblName));
		nomenCount++;
		
		///////////////
		Group group_4 = new Group(nomenclatureGroup, SWT.NONE);
		group_4.setBounds(500, 10, 182, 40);
		
		Button button_4 = new Button(group_4, SWT.RADIO);
		button_4.setText("Yes");
		button_4.setBounds(10, 13, 39, 16);
		
		Button button_5 = new Button(group_4, SWT.RADIO);
		button_5.setText("No");
		button_5.setBounds(55, 13, 39, 16);
		
		text_16 = new Text(group_4, SWT.BORDER);
		text_16.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(nomenCount), new NomenclatureBean(group_4, button_4, button_5, text_16, lblName));
		nomenCount++;		
		
		String [] nomenclatureArray = {"Authors", "Date", "Publication", "Taxon Rank"};
		for (String name : nomenclatureArray){
			addNomenclatureRow(name);
		}
		
		nomenScrolledComposite.setContent(nomenclatureGroup);
		//When you add a row, reset the size of scrolledComposite
		nomenScrolledComposite.setMinSize(nomenclatureGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Button btnAddARow = new Button(grpNomenclature, SWT.NONE);
		btnAddARow.setBounds(10, 337, 75, 25);
		btnAddARow.setText("Add a Row");
		btnAddARow.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				identifier = null;
				showInputBox();
				if(identifier != null && !identifier.equals("")) {
					addNomenclatureRow(identifier);
				}
			}
		});
		
		TabItem tbtmExpressions = new TabItem(tabFolder, SWT.NONE);
		tbtmExpressions.setText("Expressions");
		
		Group grpExpressionsUsedIn = new Group(tabFolder, SWT.NONE);
		grpExpressionsUsedIn.setText("Expressions used in Nomenclature");
		tbtmExpressions.setControl(grpExpressionsUsedIn);
		
		Label lblUseCapLetters = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblUseCapLetters.setBounds(10, 22, 426, 15);
		lblUseCapLetters.setText("Use CAP words for fixed tokens; use small letters for variables");
		
		Label lblHononyms = new Label(grpExpressionsUsedIn, SWT.NONE);
		lblHononyms.setBounds(348, 22, 99, 15);
		lblHononyms.setText("Hononyms:");
		
		expScrolledComposite = new ScrolledComposite(grpExpressionsUsedIn, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		expScrolledComposite.setBounds(10, 43, 714, 440);
		expScrolledComposite.setExpandHorizontal(true);
		expScrolledComposite.setExpandVertical(true);
		
		expressionGroup = new Group(expScrolledComposite, SWT.NONE);
		expressionGroup.setLayoutData(new RowData());
		
		// count of number of rows
		expCount = 0;
		Label lblSpecialTokensUsed = new Label(expressionGroup, SWT.NONE);
		lblSpecialTokensUsed.setBounds(10, 20, 120, 15);
		lblSpecialTokensUsed.setText("Special tokens used:");
		
		text_29 = new Text(expressionGroup, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_29.setBounds(135, 20, 550, 70);
		
		expressions.put(new Integer(expCount), new ExpressionBean(lblSpecialTokensUsed, text_29));
		expCount++;
		
		String [] expressionArray = {"Minor Amendment:", "Past name:", "Name origin:", "Homonyms:"};
		for (String name : expressionArray){
			addExpressionRow(name);
		}

		expScrolledComposite.setContent(expressionGroup);
		expScrolledComposite.setMinSize(expressionGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Button btnAddARow_2 = new Button(grpExpressionsUsedIn, SWT.NONE);
		btnAddARow_2.setBounds(10, 489, 75, 25);
		btnAddARow_2.setText("Add a row");
		btnAddARow_2.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				identifier = null;
				showInputBox();
				if(identifier != null && !identifier.equals("")) {
					addExpressionRow(identifier);
				}
			}
		});
		
		TabItem tbtmDescription = new TabItem(tabFolder, SWT.NONE);
		tbtmDescription.setText("Description");
		
		Group grpMorphologicalDescriptions = new Group(tabFolder, SWT.NONE);
		tbtmDescription.setControl(grpMorphologicalDescriptions);
		
		Label lblAllInOne = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblAllInOne.setBounds(10, 53, 160, 15);
		lblAllInOne.setText("All in one paragraph");
		
		Button btnYes = new Button(grpMorphologicalDescriptions, SWT.RADIO);
		btnYes.setBounds(241, 52, 90, 16);
		btnYes.setText("Yes");
		
		Button btnNo = new Button(grpMorphologicalDescriptions, SWT.RADIO);
		btnNo.setBounds(378, 52, 90, 16);
		btnNo.setText("No");
		
		Label lblOtherInformationMay = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblOtherInformationMay.setBounds(10, 85, 438, 15);
		lblOtherInformationMay.setText("Other information may also be included in a description paragraph:");
		
		Combo combo = new Combo(grpMorphologicalDescriptions, SWT.NONE);
		combo.setItems(new String[] {"Nomenclature", "Habitat", "Distribution", "Discussion", "Other"});
		combo.setBounds(496, 82, 177, 23);
		
		descriptionBean = new DescriptionBean(btnYes, btnNo, combo, sections);
		
		Label lblMorphological = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblMorphological.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblMorphological.setBounds(10, 25, 242, 15);
		lblMorphological.setText("Morphological Descriptions: ");
		
		Label lblOrder = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblOrder.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblOrder.setBounds(22, 142, 55, 15);
		lblOrder.setText("Order");
		
		Label lblSection = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblSection.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblSection.setBounds(140, 142, 55, 15);
		lblSection.setText("Section");
		
		Label lblStartTokens = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblStartTokens.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblStartTokens.setBounds(285, 142, 90, 15);
		lblStartTokens.setText("Start tokens");
		
		Label lblEndTokens = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblEndTokens.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblEndTokens.setBounds(443, 142, 68, 15);
		lblEndTokens.setText("End tokens");
		
		Label lblEmbeddedTokens = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblEmbeddedTokens.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblEmbeddedTokens.setBounds(592, 142, 132, 15);
		lblEmbeddedTokens.setText("Embedded tokens");
		
		descScrolledComposite = new ScrolledComposite(grpMorphologicalDescriptions, 
				SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		descScrolledComposite.setBounds(10, 169, 714, 310);
		descScrolledComposite.setExpandHorizontal(true);
		descScrolledComposite.setExpandVertical(true);
		
		descriptionGroup = new Group(descScrolledComposite, SWT.NONE);
		descriptionGroup.setLayoutData(new RowData());
	
		text_33 = new Text(descriptionGroup, SWT.BORDER);
		text_33.setBounds(10, 20, 75, 20);
		
		Label lblNomenclature = new Label(descriptionGroup, SWT.NONE);
		lblNomenclature.setBounds(120, 20, 145, 20);
		lblNomenclature.setText("Nomenclature");
		
		text_40 = new Text(descriptionGroup, SWT.BORDER);
		text_40.setBounds(270, 20, 115, 20);
		
		text_41 = new Text(descriptionGroup, SWT.BORDER);
		text_41.setBounds(420, 20, 115, 20);
		
		text_42 = new Text(descriptionGroup, SWT.BORDER);
		text_42.setBounds(570, 20, 115, 20);

		sections.put(new Integer(secCount), new SectionBean(text_33, lblNomenclature, text_40, text_41, text_42));
		secCount ++;
		
		String [] sectionArray = {"Morph. description", "Habitat", 
				"Distribution", "Discussion", "Keys", "References"};
		for(String name : sectionArray) {
			addDescriptionRow(name);
		}

		descScrolledComposite.setContent(descriptionGroup);
		descScrolledComposite.setMinSize(descriptionGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		
		Button btnAddARow_1 = new Button(grpMorphologicalDescriptions, SWT.NONE);
		btnAddARow_1.setBounds(10, 482, 75, 25);
		btnAddARow_1.setText("Add a row");
		btnAddARow_1.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				identifier = null;
				showInputBox();
				if(identifier != null && !identifier.equals("")) {
					addDescriptionRow(identifier);
				}
			}
		});
		
		Label lblSectionIndicationsAnd = new Label(grpMorphologicalDescriptions, SWT.NONE);
		lblSectionIndicationsAnd.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD));
		lblSectionIndicationsAnd.setBounds(10, 116, 222, 15);
		lblSectionIndicationsAnd.setText("Section indications and order:");
		
		TabItem tbtmSpecial = new TabItem(tabFolder, SWT.NONE);
		tbtmSpecial.setText("Special");
		
		Group grpSpecialSections = new Group(tabFolder, SWT.NONE);
		grpSpecialSections.setText("Special Sections");
		tbtmSpecial.setControl(grpSpecialSections);
		
		Label lblGlossaries = new Label(grpSpecialSections, SWT.NONE);
		lblGlossaries.setBounds(10, 51, 55, 15);
		lblGlossaries.setText("Glossaries:");
		
		Button btnHasGlossaries = new Button(grpSpecialSections, SWT.CHECK);
		btnHasGlossaries.setBounds(96, 51, 93, 16);
		btnHasGlossaries.setText("has glossaries");
		
		Label lblGlossaryHeading = new Label(grpSpecialSections, SWT.NONE);
		lblGlossaryHeading.setBounds(257, 51, 93, 15);
		lblGlossaryHeading.setText("Glossary heading");
		
		text_9 = new Text(grpSpecialSections, SWT.BORDER);
		text_9.setBounds(377, 48, 76, 21);
		
		Label lblReferences = new Label(grpSpecialSections, SWT.NONE);
		lblReferences.setBounds(10, 102, 69, 15);
		lblReferences.setText("References :");
		
		Button btnHasReferences = new Button(grpSpecialSections, SWT.CHECK);
		btnHasReferences.setBounds(96, 102, 93, 16);
		btnHasReferences.setText("has references");
		
		Label lblReferencesHeading = new Label(grpSpecialSections, SWT.NONE);
		lblReferencesHeading.setBounds(257, 102, 114, 15);
		lblReferencesHeading.setText("References heading:");
		
		text_10 = new Text(grpSpecialSections, SWT.BORDER);
		text_10.setBounds(377, 99, 76, 21);
		special = 
			new SpecialBean(btnHasGlossaries,btnHasReferences, text_9, text_10);
		
		TabItem tbtmAbbreviations = new TabItem(tabFolder, SWT.NONE);
		tbtmAbbreviations.setText("Abbreviations");
		
		Group grpAbbreviationsUsedIn = new Group(tabFolder, SWT.NONE);
		tbtmAbbreviations.setControl(grpAbbreviationsUsedIn);
		
		text_11 = new Text(grpAbbreviationsUsedIn, SWT.BORDER | SWT.MULTI| SWT.WRAP | SWT.V_SCROLL );
		text_11.setBounds(10, 52, 691, 69);
		abbreviations.put("Text", text_11);
		
		Label lblAbbreviationsUsedIn = new Label(grpAbbreviationsUsedIn, SWT.NONE);
		lblAbbreviationsUsedIn.setBounds(10, 31, 272, 15);
		lblAbbreviationsUsedIn.setText("Abbreviations used in text:");
		
		Label lblAbbreviationsUsedIn_1 = new Label(grpAbbreviationsUsedIn, SWT.NONE);
		lblAbbreviationsUsedIn_1.setBounds(10, 150, 272, 15);
		lblAbbreviationsUsedIn_1.setText("Abbreviations used in bibliographical citations:");
		
		text_12 = new Text(grpAbbreviationsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_12.setBounds(10, 175, 691, 69);
		abbreviations.put("Bibliographical Citations", text_12);
		
		Label lblAbbreviationsUsedIn_2 = new Label(grpAbbreviationsUsedIn, SWT.NONE);
		lblAbbreviationsUsedIn_2.setBounds(10, 275, 272, 15);
		lblAbbreviationsUsedIn_2.setText("Abbreviations used in authorities:");
		
		text_13 = new Text(grpAbbreviationsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_13.setBounds(10, 296, 691, 69);
		abbreviations.put("Authorities", text_13);
		
		Label lblAbbreviationsUsedIn_3 = new Label(grpAbbreviationsUsedIn, SWT.NONE);
		lblAbbreviationsUsedIn_3.setBounds(10, 395, 204, 15);
		lblAbbreviationsUsedIn_3.setText("Abbreviations used in others:");
		
		text_8 = new Text(grpAbbreviationsUsedIn, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
		text_8.setBounds(10, 416, 691, 69);
		abbreviations.put("Others", text_8);
		final Type2Bean bean = new Type2Bean(textBean, nomenclatures, expressions, descriptionBean, special, abbreviations);
		Button btnSave = new Button(shlTypeDocument, SWT.NONE);
		btnSave.setBounds(670, 563, 75, 25);
		btnSave.setText("Save");
		btnSave.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				try {					
					if (configDb.saveType2Details(bean)) {
						ApplicationUtilities.showPopUpWindow(ApplicationUtilities.getProperty("popup.info.savetype3"),
								ApplicationUtilities.getProperty("popup.header.info"), SWT.ICON_INFORMATION);
						shlTypeDocument.dispose();
					}
				} catch (SQLException exe) {
					
				}
			}
		});	
		
		/* Load previously saved details here */
		try {
			configDb.retrieveType2Details(bean, this);
		} catch (SQLException exe) {
			exe.printStackTrace();
		}
		
		
		shlTypeDocument.open();
		shlTypeDocument.layout();
		while (!shlTypeDocument.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
    }
    
    public void addDescriptionRow(String sectionName) {
		RowData rowdata = (RowData)descriptionGroup.getLayoutData();
		rowdata.height += 60;
		descriptionGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
        Rectangle rect = descriptionGroup.getBounds();
        rect.height += 60;
        descriptionGroup.setBounds(rect);
        
        /*Add the row*/
        Text prevText = sections.get(new Integer(secCount-1)).getEndTokens();
        rect = prevText.getBounds();
        Text newText_1 = new Text(descriptionGroup, SWT.BORDER);
        newText_1.setBounds(10, rect.y + 40, 75, 20);	
        newText_1.setFocus();
		
		Label lblNew = new Label(descriptionGroup, SWT.NONE);
		lblNew.setBounds(120, rect.y + 40, 145, 20);
		lblNew.setText(sectionName);
		
		Text newText_2 = new Text(descriptionGroup, SWT.BORDER);
		newText_2.setBounds(270, rect.y + 40, 115, 20);
		
		Text newText_3 = new Text(descriptionGroup, SWT.BORDER);
		newText_3.setBounds(420, rect.y + 40, 115, 20);

		Text newText_4 = new Text(descriptionGroup, SWT.BORDER);
		newText_4.setBounds(570, rect.y + 40, 115, 20);
		sections.put(new Integer(secCount), new SectionBean(newText_1, lblNew, newText_2, newText_3, newText_4));
		secCount ++;
        
        descScrolledComposite.setMinSize(descriptionGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
    
    public void addExpressionRow(String expressionName) {
		RowData rowdata = (RowData)expressionGroup.getLayoutData();
		rowdata.height += 120;
		expressionGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
        Rectangle rect = expressionGroup.getBounds();
        rect.height += 120;
        expressionGroup.setBounds(rect);        
        /*Create a row*/
        ExpressionBean expBean = expressions.get(new Integer(expCount-1));
        Label previousLabel = expBean.getLabel();
        rect = previousLabel.getBounds();
        Label lblNew = new Label(expressionGroup, SWT.NONE);
        lblNew.setBounds(10,  rect.y + 90, 120, 15);
        lblNew.setText(expressionName);
        lblNew.setFocus();
        
        Text previousText = expBean.getText();
        rect = previousText.getBounds();
        
        Text newText = new Text(expressionGroup, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.MULTI);
        newText.setBounds(135, rect.y + 90, 550, 70);        
        expressions.put(new Integer(expCount), new ExpressionBean(lblNew, newText));
        expCount++;        
        expScrolledComposite.setMinSize(expressionGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }

    public void addNomenclatureRow(String name) {
    	
		RowData rowdata = (RowData)nomenclatureGroup.getLayoutData();
		rowdata.height += 30;
		nomenclatureGroup.setLayoutData(new RowData(rowdata.width, rowdata.height));
        Rectangle rect = nomenclatureGroup.getBounds();
        rect.height += 30;
        nomenclatureGroup.setBounds(rect);
        
        /*Create a row*/
        
        NomenclatureBean nbean = nomenclatures.get(new Integer(nomenCount-1));
        Label previousLabel = nbean.getLabel();
        rect = previousLabel.getBounds();
        rect.y += 45;
       
        Label lblNew = new Label(nomenclatureGroup, SWT.NONE);
		lblNew.setBounds(rect);
		lblNew.setText(name);
		lblNew.setFocus();
        
		/* Create the first group*/
		Group prevGroup = nbean.getParent();
		rect = prevGroup.getBounds();
		Group group_1 = new Group(nomenclatureGroup, SWT.NONE);
		group_1.setBounds(100, rect.y+45, 182, 40);
		
		Button buttonYes_1 = new Button(group_1, SWT.RADIO);
		buttonYes_1.setText("Yes");
		buttonYes_1.setBounds(10, 13, 39, 16);
		
		Button buttonNo_1 = new Button(group_1, SWT.RADIO);
		buttonNo_1.setText("No");
		buttonNo_1.setBounds(55, 13, 39, 16);
		
		Text text1 = new Text(group_1, SWT.BORDER);
		text1.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(nomenCount), new NomenclatureBean(group_1, buttonYes_1, buttonNo_1, text1, lblNew));
		nomenCount++;
		///////////////////////////////////
		
		 /*Create the second group */
		Group group_2 = new Group(nomenclatureGroup, SWT.NONE);
		group_2.setBounds(300, rect.y+45, 182, 40);
		
		Button buttonYes_2 = new Button(group_2, SWT.RADIO);
		buttonYes_2.setText("Yes");
		buttonYes_2.setBounds(10, 13, 39, 16);
		
		Button buttonNo_2 = new Button(group_2, SWT.RADIO);
		buttonNo_2.setText("No");
		buttonNo_2.setBounds(55, 13, 39, 16);
		
		Text text2 = new Text(group_2, SWT.BORDER);
		text2.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(nomenCount), new NomenclatureBean(group_2, buttonYes_2, buttonNo_2, text2, lblNew));
		nomenCount++; 
		
		/* Create the third group */
		Group group_3 = new Group(nomenclatureGroup, SWT.NONE);
		group_3.setBounds(500,rect.y+45, 182, 40);
		
		Button buttonYes_3 = new Button(group_3, SWT.RADIO);
		buttonYes_3.setText("Yes");
		buttonYes_3.setBounds(10, 13, 39, 16);
		
		Button buttonNo_3 = new Button(group_3, SWT.RADIO);
		buttonNo_3.setText("No");
		buttonNo_3.setBounds(55, 13, 39, 16);
		
		Text text3 = new Text(group_3, SWT.BORDER);
		text3.setBounds(100, 11, 76, 21);
		
		nomenclatures.put(new Integer(nomenCount), new NomenclatureBean(group_3, buttonYes_3, buttonNo_3, text3, lblNew));
		nomenCount++;
        nomenScrolledComposite.setMinSize(nomenclatureGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
    private void showInputBox() {
    	Display display = Display.getDefault();
    	final Shell dialog = new Shell (display, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
		dialog.setText("Add a row");
		dialog.setLocation(shlTypeDocument.getBounds().x/2 + shlTypeDocument.getBounds().width/2, 
				shlTypeDocument.getBounds().y/2+ shlTypeDocument.getBounds().height/2);
		FormLayout formLayout = new FormLayout ();
		formLayout.marginWidth = 10;
		formLayout.marginHeight = 10;
		formLayout.spacing = 10;
		dialog.setLayout (formLayout);

		Label label = new Label (dialog, SWT.NONE);
		label.setText ("Type an identifier:");
		FormData data = new FormData ();
		label.setLayoutData (data);

		Button cancel = new Button (dialog, SWT.PUSH);
		cancel.setText ("Cancel");
		data = new FormData ();
		data.width = 60;
		data.right = new FormAttachment (100, 0);
		data.bottom = new FormAttachment (100, 0);
		cancel.setLayoutData (data);
		cancel.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				dialog.close ();
			}
		});

		final Text text = new Text (dialog, SWT.BORDER);
		data = new FormData ();
		data.width = 200;
		data.left = new FormAttachment (label, 0, SWT.DEFAULT);
		data.right = new FormAttachment (100, 0);
		data.top = new FormAttachment (label, 0, SWT.CENTER);
		data.bottom = new FormAttachment (cancel, 0, SWT.DEFAULT);
		text.setLayoutData (data);

		Button ok = new Button (dialog, SWT.PUSH);
		ok.setText ("OK");
		data = new FormData ();
		data.width = 60;
		data.right = new FormAttachment (cancel, 0, SWT.DEFAULT);
		data.bottom = new FormAttachment (100, 0);
		ok.setLayoutData (data);
		ok.addSelectionListener (new SelectionAdapter () {
			public void widgetSelected (SelectionEvent e) {
				identifier = text.getText();
				dialog.close ();
			}
		});

		dialog.setDefaultButton (ok);
		dialog.pack ();
		dialog.open ();
		while (!dialog.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}

	}
}
