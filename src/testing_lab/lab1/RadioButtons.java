package testing_lab.lab1;

/******************************************************************************
 * Copyright (c) 1998, 2004 Jackwind Li Guojie
 * All right reserved. 
 * 
 * Created on Jan 26, 2004 4:50:27 PM by JACK
 * $Id: RadioButtons.java 280 2010-03-26 00:22:06Z semantic.partha@gmail.com $
 * 
 * visit: http://www.asprise.com/swt
 *****************************************************************************/

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class RadioButtons {

  public RadioButtons() {
    Display display = new Display();
    Shell shell = new Shell(display);
    
    shell.setLayout(new RowLayout());
    
    Label label = new Label(shell, SWT.NULL);
    label.setText("Gender: ");
    label.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
    
    Button femaleButton = new Button(shell, SWT.RADIO);
    femaleButton.setText("F");
    
    Button maleButton = new Button(shell, SWT.RADIO);
    maleButton.setText("M");
    
    label = new Label(shell, SWT.NULL);
    label.setText("  Title: ");
    label.setBackground(display.getSystemColor(SWT.COLOR_YELLOW));
    
    Composite composite = new Composite(shell, SWT.NULL);
    composite.setLayout(new RowLayout());
    
    Button mrButton = new Button(composite, SWT.RADIO);
    mrButton.setText("Mr.");
    Button mrsButton = new Button(composite, SWT.RADIO);
    mrsButton.setText("Mrs.");
    Button msButton = new Button(composite, SWT.RADIO);
    msButton.setText("Ms.");
    Button drButton = new Button(composite, SWT.RADIO);
    drButton.setText("Dr.");  

    shell.pack();
    shell.open();
    //textUser.forceFocus();

    // Set up the event loop.
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) {
        // If no more entries in event queue
        display.sleep();
      }
    }

    display.dispose();
  }

  private void init() {

  }

  public static void main(String[] args) {
    new RadioButtons();
  }
}

