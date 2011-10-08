package testing_lab.lab1;

//Send questions, comments, bug reports, etc. to the authors:

//Rob Warner (rwarner@interspatial.com)
//Robert Harris (rbrt_harris@yahoo.com)

import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
* This class demonstrates the MessageBox class
*/
public class ShowMessageBox {
// Strings to show in the Icon dropdown
private static final String[] ICONS = { "SWT.ICON_ERROR",
    "SWT.ICON_INFORMATION", "SWT.ICON_QUESTION", "SWT.ICON_WARNING",
    "SWT.ICON_WORKING"};

// Strings to show in the Buttons dropdown
private static final String[] BUTTONS = { "SWT.OK", "SWT.OK | SWT.CANCEL",
    "SWT.YES | SWT.NO", "SWT.YES | SWT.NO | SWT.CANCEL",
    "SWT.RETRY | SWT.CANCEL", "SWT.ABORT | SWT.RETRY | SWT.IGNORE"};

/**
 * Runs the application
 */
public void run() {
  Display display = new Display();
  Shell shell = new Shell(display);
  shell.setText("Show Message Box");
  createContents(shell);
  shell.pack();
  shell.open();
  while (!shell.isDisposed()) {
    if (!display.readAndDispatch()) {
      display.sleep();
    }
  }
  display.dispose();
}

/**
 * Creates the main window's contents
 * 
 * @param shell the parent shell
 */
private void createContents(final Shell shell) {
  shell.setLayout(new GridLayout(2, false));

  // Create the dropdown to allow icon selection
  new Label(shell, SWT.NONE).setText("Icon:");
  final Combo icons = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);
  for (int i = 0, n = ICONS.length; i < n; i++)
    icons.add(ICONS[i]);
  icons.select(0);

  // Create the dropdown to allow button selection
  new Label(shell, SWT.NONE).setText("Buttons:");
  final Combo buttons = new Combo(shell, SWT.DROP_DOWN | SWT.READ_ONLY);
  for (int i = 0, n = BUTTONS.length; i < n; i++)
    buttons.add(BUTTONS[i]);
  buttons.select(0);

  // Create the entry field for the message
  new Label(shell, SWT.NONE).setText("Message:");
  final Text message = new Text(shell, SWT.BORDER);
  message.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

  // Create the label to show the return from the open call
  new Label(shell, SWT.NONE).setText("Return:");
  final Label returnVal = new Label(shell, SWT.NONE);
  returnVal.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

  // Create the button and event handler
  // to display the message box
  Button button = new Button(shell, SWT.PUSH);
  button.setText("Show Message");
  button.addSelectionListener(new SelectionAdapter() {
    public void widgetSelected(SelectionEvent event) {
      // Clear any previously returned value
      returnVal.setText("");

      // This will hold the style to pass to the MessageBox constructor
      int style = 0;

      // Determine which icon was selected and
      // add it to the style
      switch (icons.getSelectionIndex()) {
      case 0:
        style |= SWT.ICON_ERROR;
        break;
      case 1:
        style |= SWT.ICON_INFORMATION;
        break;
      case 2:
        style |= SWT.ICON_QUESTION;
        break;
      case 3:
        style |= SWT.ICON_WARNING;
        break;
      case 4:
        style |= SWT.ICON_WORKING;
        break;
      }

      // Determine which set of buttons was selected
      // and add it to the style
      switch (buttons.getSelectionIndex()) {
      case 0:
        style |= SWT.OK;
        break;
      case 1:
        style |= SWT.OK | SWT.CANCEL;
        break;
      case 2:
        style |= SWT.YES | SWT.NO;
        break;
      case 3:
        style |= SWT.YES | SWT.NO | SWT.CANCEL;
        break;
      case 4:
        style |= SWT.RETRY | SWT.CANCEL;
        break;
      case 5:
        style |= SWT.ABORT | SWT.RETRY | SWT.IGNORE;
        break;
      }

      // Display the message box
      MessageBox mb = new MessageBox(shell, style);
      mb.setText("Message from SWT");
      mb.setMessage(message.getText());
      int val = mb.open();
      String valString = "";
      switch (val) // val contains the constant of the selected button
      {
      case SWT.OK:
        valString = "SWT.OK";
        break;
      case SWT.CANCEL:
        valString = "SWT.CANCEL";
        break;
      case SWT.YES:
        valString = "SWT.YES";
        break;
      case SWT.NO:
        valString = "SWT.NO";
        break;
      case SWT.RETRY:
        valString = "SWT.RETRY";
        break;
      case SWT.ABORT:
        valString = "SWT.ABORT";
        break;
      case SWT.IGNORE:
        valString = "SWT.IGNORE";
        break;
      }
      returnVal.setText(valString);
    }
  });
}

/**
 * Application entry point
 * 
 * @param args the command line arguments
 */
public static void main(String[] args) {
  //new ShowMessageBox().run();
	//import org.eclipse.swt.SWT;
	//import org.eclipse.swt.widgets.*;
	//System.out.println(Runtime.getRuntime().);
	System.out.println(System.getProperty("user.dir"));
			Display display = new Display();
			Shell shell = new Shell(display);
			Link link = new Link(shell, SWT.NONE);
			String text = "The SWT component is designed to provide <a>efficient</a>, <a>portable</a> <a href=\"native\">access to the user-interface facilities of the operating systems</a> on which it is implemented.";
			link.setText(text);
			link.setSize(400, 400);
			link.addListener (SWT.Selection, new Listener () {
				public void handleEvent(Event event)  {
					System.out.println("Selection: " + event.text);
					Runtime runtime = Runtime.getRuntime();
					try {
						Process proc = runtime.exec("C:\\Windows\\notepad.exe \"C:\\Users\\Partha Pratim Sanyal\\Documents\\finicky.txt \"");
					} catch (Exception e){
						
					}
					
				}
			});
			shell.pack ();
			shell.open();
			while (!shell.isDisposed()) {
				if (!display.readAndDispatch())
					display.sleep();
			}
			display.dispose();
		}

}