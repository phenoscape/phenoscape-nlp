package testing_lab.lab1;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;


public class TableItemSelection {

	/**
	 * @param args
	 */
	public static void main (String [] args) {
		Display display = new Display ();
		Shell shell = new Shell (display);
		final Table table = new Table (shell, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		for (int i=0; i<12; i++) {
			TableItem item = new TableItem (table, SWT.NONE);
			item.setText ("Item " + i);
		}
		table.setSize (100, 100);
		/*table.addListener (SWT.MouseDoubleClick, new Listener () {
			public void handleEvent (Event event) {
				String string = event.detail == SWT.CHECK ? "Checked" : "Selected";
				System.out.println (event.index + " " + string);
			}
		}); */
		
		table.addMouseListener(new MouseListener () {
			public void mouseDoubleClick(MouseEvent event) {
				//System.out.println(event.);
				System.out.println(table.getSelectionIndex());
				return;
				//System.out.println("Selection: " + event.text);
				/*Runtime runtime = Runtime.getRuntime();
				try {
					Process proc = runtime.exec("C:\\Windows\\notepad.exe \""+linkPath+"\"");
				} catch (Exception e){
					
				} */
			}
			
			public void mouseDown(MouseEvent event) {}
			public void mouseUp(MouseEvent event) {}
		});
		shell.setSize (200, 200);
		shell.open ();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
	}


}
