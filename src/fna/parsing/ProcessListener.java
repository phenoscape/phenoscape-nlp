/**
 * $Id: ProcessListener.java 840 2011-06-05 03:57:51Z hong1.cui $
 */
package fna.parsing;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;



/**
 * Listen to the parse process. And append the contents to the table.
 * 
 * TODO: to seperate the background code from SWT, an interface should be
 * added.
 * 
 * @author chunshui
 */
public class ProcessListener {
	
	private Table table;
	private Display display;
	private ProgressBar progressBar;
	
	public ProcessListener(Table table, Display display) {
		this.table = table;
		this.display = display;
	}
	
	public ProcessListener(Table table, ProgressBar progressBar, Display display) {
		this.table = table;
		this.progressBar = progressBar;
		this.display = display;
	}
	
	public ProcessListener(ProgressBar popupBar, Display display2) {
		// TODO Auto-generated constructor stub
		this.progressBar = popupBar;
		this.display = display2;
	}

	public void info(final String... contents) {		
		display.syncExec(new Runnable() {
			public void run() {
				TableItem item = new TableItem(table, SWT.NONE);
				if (contents.length > 1) {
					contents[1] = contents[1].substring(contents[1].lastIndexOf("\\")+1);
				}
			    item.setText(contents);	
			}
		});
		

	}


	
	public void progress(final int selection) {
		display.syncExec(new Runnable() {
			public void run() {	
				progressBar.setSelection(selection);
			}
		});
	}
	
	public void clear() {
		display.syncExec(new Runnable() {
			public void run() {
				table.removeAll();
			}
		});
		
	}
	
	public void setProgressBarVisible(final boolean visible) {
		display.syncExec(new Runnable() {
			public void run() {
				progressBar.setVisible(visible);
			}
		});		
	}
	
	public Table getTable() {
		return this.table;
	}
}
