package testing_lab.lab1;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;

public class MainClass {
	private static Text text;

  public static void main(String[] a) {
    Display display = new Display();
    Shell shell = new Shell(display);
    shell.setSize(452, 150);
    shell.setLayout(new GridLayout());

    ProgressBar pb1 = new ProgressBar(shell, SWT.HORIZONTAL | SWT.SMOOTH);
    pb1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    pb1.setMinimum(0);
    pb1.setMaximum(100);

   
    
    text = new Text(shell, SWT.BORDER | SWT.MULTI| SWT.WRAP | SWT.V_SCROLL);
    GridData gd_text = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
    gd_text.heightHint = 65;
    text.setLayoutData(gd_text);
    System.out.println(Thread.currentThread());
    new LongRunningOperation(display, pb1, text).start();

    shell.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
    	  System.out.println("I'm sleeping");
        display.sleep();
      }
    }
  }
}

class LongRunningOperation extends Thread {
  private Display display;
  private Text text;
  private ProgressBar progressBar;

  public LongRunningOperation(Display display, ProgressBar progressBar, Text text) {
    this.display = display;
    this.progressBar = progressBar;
    this.text = text;
  }

  public void run() {
	  System.out.println(Thread.currentThread() + "Partha");
    for (int i = 0; i < 10; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
      }
      display.syncExec(new Runnable() {
        public void run() {
        	System.out.println(Thread.currentThread()+"Pratim");
          if (progressBar.isDisposed())
            return;
          progressBar.setSelection(progressBar.getSelection() + 1);
          text.append("Partha" + Math.random() +"\n");
       }
      });
    }
  }
}
