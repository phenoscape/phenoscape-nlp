package fna.parsing;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import fna.db.ConfigurationDbAccessor;
import fna.parsing.ApplicationUtilities;
import fna.parsing.ParsingException;
import fna.parsing.VolumeExtractor;


public class Type1Document {

	/**
	 * @author Partha Pratim Sanyal
	 */
	
	private ConfigurationDbAccessor configDb = new ConfigurationDbAccessor();
	private static final Logger LOGGER = Logger.getLogger(Type1Document.class);
	private final HashMap <Integer, Text> textFields = new HashMap<Integer, Text> ();
	private final HashMap <Integer, Combo> comboFields = new HashMap<Integer, Combo> ();
	private final HashMap <Integer, Button> checkFields = new HashMap<Integer, Button> ();
	private int count = 1;
	
	private static Shell shell = null;
	private String [] tagnames = null;
	private ScrolledComposite scrolledComposite = null;
	private Group group = null;
	private String startUpString = null;

		
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
/*		Type1Document td1 = new Type1Document();
		
		do {
			td1.showType1Document();
			if(VolumeExtractor.getStart() == null) {

			}
		}
		while(VolumeExtractor.getStart() == null);
			
		
		//System.out.println("disposed :(");
*/		
	}
	
	/**
	 * @wbp.parser.entryPoint
	 */
	public void showType1Document() {
		//Display display = new Display ();
		final Display display = Display.getDefault();
		shell = new Shell (display);
		shell.setText("Type1 Document");
		shell.setSize(593, 276);
		scrolledComposite = new ScrolledComposite(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setBounds(10, 31, 557, 150);
		scrolledComposite.setLayout(new RowLayout(SWT.HORIZONTAL));
		group = new Group(scrolledComposite, SWT.NONE);
		group.setLayoutData(new RowData(487, 136));	
				
		Label label = new Label(shell, SWT.NONE);
		label.setBounds(28, 10, 131, 15);
		label.setText("Word Document Style");


		Button button = new Button(shell, SWT.NONE);
		button.setBounds(391, 205, 75, 25);
		button.setText("Save");
		button.addMouseListener(new MouseListener(){
			public void mouseUp(MouseEvent mEvent){
				String message = ApplicationUtilities.getProperty("popup.info.savetype1.tag") ;
				boolean flag = true;
				try {
					// call the function here to calculate common string
					flag =  commonStartUpString();
					if (flag) {
						message += createStyleMappingFile();					
						configDb.saveSemanticTagDetails(comboFields, checkFields);
					} else {
						message = ApplicationUtilities.getProperty("popup.error.style");
					}

				} catch (IOException exe) {
					LOGGER.error("Error saving to file in Type1doc", exe);
				} catch (SQLException sqlexp) {
					LOGGER.error("Error saving tags to database in Type1doc", sqlexp);
				}
				ApplicationUtilities.showPopUpWindow(message , "Information", SWT.ICON_INFORMATION);

				if(flag) {
					VolumeExtractor.setStart(".*?(" + startUpString + ").*");
					shell.dispose();
				} 

			}
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}

		});
		
		
		Button addRowButton = new Button(shell, SWT.NONE);
		addRowButton.setBounds(492, 205, 75, 25);
		addRowButton.setText("Add a Row");
		addRowButton.addMouseListener(new MouseListener(){
			public void mouseUp(MouseEvent mEvent){
				//add a row
				addRow();
			    scrolledComposite.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});

		
		Label label_1 = new Label(shell, SWT.NONE);
		label_1.setBounds(229, 10, 121, 15);
		label_1.setText("Semantic Tags");
		
		Label label_2 = new Label(shell, SWT.NONE);
		label_2.setBounds(458, 10, 55, 15);
		label_2.setText("Start Style");
		
		Text text = new Text(group, SWT.BORDER);
		text.setBounds(10, 10, 172, 21);
		textFields.put(new Integer(1), text);
		count += 1;
		
		Combo combo = new Combo(group, SWT.NONE);
		combo.setBounds(216, 10, 182, 23);
		
		//retrieve the data from db here.		
		List <String> tags = new ArrayList <String>();
		try {
			configDb.retrieveTagDetails(tags);
		} catch(Exception exe) {
			LOGGER.error("Error retrieving tags", exe);
		}
		tagnames = new String[tags.size()];
		int loopCount = 0;
		for (String s : tags) {
			tagnames [loopCount] = s;
			loopCount++;
		}
		//
		
		combo.setItems(tagnames);
		comboFields.put(new Integer(1), combo);
		
		final Button checkButton = new Button(group, SWT.CHECK);
		checkButton.setBounds(460, 10, 12, 16);
		checkFields.put(new Integer(1), checkButton);
		
		/*Load from style-mapping properties here */
		try {
			loadStyleMappinFile();
		} catch (Exception exe) {
			exe.printStackTrace();
			LOGGER.error("Error loading Tag styles", exe);
		}
		
		scrolledComposite.setRedraw(true);
		scrolledComposite.setContent(group);
		scrolledComposite.setMinSize(group.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setShowFocusedControl(true);		
		shell.open ();
		shell.layout();
		
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
	}
	
	private void loadStyleMappinFile() throws IOException {		
		
		String pathName = getStyleMappingFile();
		String style = "";
		String tag = "";
		
		File styleMapping = new File(pathName);
		if(styleMapping.isFile() && styleMapping.exists()) {
			BufferedReader in = new BufferedReader(new FileReader(styleMapping));
			String line = "";
			while ((line = in.readLine())!= null){
				if(line.indexOf("=")!= -1) {
					style = line.substring(0, line.indexOf("="));
					tag = line.substring(line.indexOf("=")+1);
				} else {
					style = line;
				}

				Integer key = new Integer(count-1);
				Text text = textFields.get(key);
				Combo combo = comboFields.get(key);
				
				combo.setText(tag);
				text.setText(style);
				
				/* Add another row here */
				addRow();
				
			}
		}
	}
	
	private String getStyleMappingFile() throws IOException {
		File project = new File(System.getProperty("user.dir")+"\\fnaproject.conf");
		BufferedReader in = new BufferedReader(new FileReader(project));
		String conf = in.readLine();
		conf = conf == null ? "C:\\" : conf;		
		String pathName = conf + ApplicationUtilities.getProperty("style.mapping"); 
		return pathName;
	}
	
	private String createStyleMappingFile() throws IOException, ParsingException {
		
		int countRows = textFields.size();
		
		String pathName = getStyleMappingFile(); 
		BufferedWriter out = new BufferedWriter(new FileWriter(pathName));
		for(int i=1; i<= countRows; i++) {
			Integer key = new Integer(i);
			Text text = textFields.get(key);
			String text1 = text.getText();
			
			Combo combo = comboFields.get(key);
			String comboText = null;
			comboText = (combo == null)? "" : combo.getText();
			
			if(!text1.equals("") && !comboText.equals("")) {
				String line = text1 + "=" + comboText;
				out.write(line);
				out.newLine();
				out.flush();
			}else if(!text1.equals("") || !comboText.equals("")){ //one has value, may be a comments, don't save
				//do nothing
			}else if (i==1){//both are empty
				//pathName = null;
				throw new ParsingException("No details to save");
			}

		}
		out.close();
		return pathName;
	}
	
	private void addRow() {
		
		// group height				
		RowData rowdata = (RowData)group.getLayoutData();
		rowdata.height += 30;
		group.setLayoutData(new RowData(rowdata.width, rowdata.height));
        Rectangle rect = group.getBounds();
        rect.height += 30;
        group.setBounds(rect);

        final Integer key = new Integer(count);
        final Integer previousKey = new Integer(key.intValue()-1);
        
		Combo tempCombo = new Combo(group, SWT.NONE);	
		tempCombo.setItems(tagnames);				
		rect = comboFields.get(previousKey).getBounds();
		rect.y += 30;
		tempCombo.setBounds(rect);
		comboFields.put(key, tempCombo); 
		
	    
	    Text tempText = new Text(group, SWT.BORDER);
	    rect = textFields.get(previousKey).getBounds();
	    rect.y += 30;
	    tempText.setBounds(rect);
	    textFields.put(key, tempText);
	    
	    Button tempCheck = new Button(group, SWT.CHECK);
	    rect = checkFields.get(previousKey).getBounds();
	    rect.y += 30;
	    tempCheck.setBounds(rect);
	    checkFields.put(key, tempCheck);	    

		count += 1;
	}

	
	private boolean commonStartUpString(){
		
		Set <Integer> keys = checkFields.keySet();
		List<String> styles = new ArrayList<String>();
		boolean flag = false;
		Button check = null;
		for (Integer i : keys) {
			check = checkFields.get(i);
			flag = check.getSelection();
			if(flag) {
				styles.add(textFields.get(i).getText());
			}
		}
		
		if(styles.size() == 0) {
			return false;
		}
		
		if (styles.size() == 1) {
			for (String style : styles) {
				startUpString = style ;
				return true;
			}
		}
		
		int size = styles.size();
		String [] startStyles = new String [size];
		styles.toArray(startStyles);
		String common = startStyles[0];
		for (int i=0; i < size-1; i++) {			
			common = ApplicationUtilities.longestCommonSubstring(common, startStyles[i+1]);
		}
		
		if (common.length() == 0) {
			
			for (String s : startStyles) {
				common += s + "|";
			}
			startUpString = common.substring(0, common.lastIndexOf('|'));
		} else {
			startUpString = common;
		}
		
		//ApplicationUtilities.showPopUpWindow(startUpString, "Common StartUp String", SWT.ICON_INFORMATION);
		return true;
	}
}
