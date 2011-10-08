package testing_lab.lab1;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

public class acxad {

  public static void main(String[] args) {
/*    Display display = new Display();
    Shell shell = new Shell(display);
    ProgressBar bar = new ProgressBar(shell, SWT.SMOOTH);
    bar.setBounds(10, 10, 200, 32);
    shell.open();
    for (int i = 0; i <= bar.getMaximum(); i++) {
      try {
        Thread.sleep(100);
      } catch (Throwable th) {
      }
      bar.setSelection(i);
    }
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch())
        display.sleep();
    }
    display.dispose();*/
/*	  String x = "Group1.xml";
	  System.out.println(x.substring(x.indexOf(".")-1,x.indexOf(".")));*/
	  
/*		String [] files = new String[]{"a", "b", "c", "d","e"};
		String sourceFile = "";
		for (String file : files) {
			sourceFile += file + ",";
		}
		sourceFile = sourceFile.substring(0, sourceFile.lastIndexOf(","));*/
	  	String x = "Group_12.xml";
		System.out.println(x.substring(0, x.indexOf(".xml")));
  }
}
