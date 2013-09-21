package fna.parsing;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;


import fna.db.ConfigurationDbAccessor;

import java.sql.SQLException;

public class Type3Document {
	private Text text;

	/**
	 * @param args
	 */
	private ConfigurationDbAccessor configDb = new ConfigurationDbAccessor();
	private static final Logger LOGGER = Logger.getLogger(Type3Document.class);
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Type3Document().showType3Document();
	}
	
	/**
	 * @wbp.parser.entryPoint
	 */
	public void showType3Document() {
		final Display display = Display.getDefault();
		
		final Shell shell = new Shell();
		shell.setSize(613, 437);
		shell.setText("Type 3 Documents");
		shell.setLayout(new RowLayout(SWT.HORIZONTAL));
		
		final Group group = new Group(shell, SWT.NONE);
		group.setLayoutData(new RowData(585, 377));
		group.setBounds(10, 10, 500, 115);
		
		Button button = new Button(group, SWT.NONE);
		button.setBounds(408, 360, 75, 25);
		button.setText("Save");
		button.addMouseListener(new MouseListener(){
			public void mouseUp(MouseEvent mEvent){
				String [] paragraphs = text.getText().split("\r\n");
				try {
					configDb.saveParagraphTagDetails(null, paragraphs);
					ApplicationUtilities.showPopUpWindow(ApplicationUtilities.getProperty("popup.info.savetype3"),
							ApplicationUtilities.getProperty("popup.header.info"), SWT.ICON_INFORMATION);
					shell.dispose();
				} catch (SQLException sqle) {
					LOGGER.error("Unable to save paragraphs to db in Type3Document", sqle);
					sqle.printStackTrace();
				}
				
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		
		Button button_1 = new Button(group, SWT.NONE);
		button_1.setBounds(498, 360, 75, 25);
		button_1.setText("Skip");
		button_1.addMouseListener(new MouseListener(){
			public void mouseUp(MouseEvent mEvent){
					shell.dispose();				
			}
			
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		
		Label label = new Label(group, SWT.NONE);
		label.setBounds(10, 10, 482, 15);
		label.setText("If you have a sample paragraph for morphological description, please paste that below : ");
		
		text = new Text(group, SWT.BORDER | SWT.MULTI| SWT.WRAP | SWT.V_SCROLL);
		text.setBounds(10, 52, 563, 302);
		
		Label label_1 = new Label(group, SWT.NONE);
		label_1.setBounds(10, 31, 387, 15);
		label_1.setText("* Please separate the paragraphs by line breaks.");
		label_1.setForeground(new Color(display, 255, 0, 0));
		
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
	}
}
