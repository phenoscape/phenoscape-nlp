package testing_lab.lab1;
//http://www.diegoparrilla.com/2005/07/swt-and-multithreading.html
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import com.swtdesigner.SWTResourceManager;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;

public class Threading {
	public static Text text;

	/**
	 * @param args
	 */
	public Shell shell = null;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new Threading().show();
	}
	

	
	/**
	 * @wbp.parser.entryPoint
	 */
	public void show () {
		final Display dis = new  Display();
		shell = new Shell(dis);
		shell.setSize(674, 364);
		shell.setImage(SWTResourceManager.getImage("C:\\Users\\Partha Pratim Sanyal\\Pictures\\ami2.jpg"));
		
		text = new Text(shell, SWT.BORDER | SWT.MULTI| SWT.WRAP | SWT.V_SCROLL);
		text.setBounds(10, 10, 638, 277);
		text.setEditable(false);
		
		Button button = new Button(shell, SWT.NONE);
		button.setBounds(573, 293, 75, 25);
		button.setText("Start");
		button.addMouseListener(new MouseListener(){
			public void mouseUp(MouseEvent mEvent){
				shell.getDisplay().syncExec(new Producer());
			}
			public void mouseDown(MouseEvent mEvent) { }
			public void mouseDoubleClick(MouseEvent mEvent) {}
		});
		
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!dis.readAndDispatch())
				dis.sleep();
		}
		
	}
}

class Producer implements Runnable {
	
	public void run() {
		for (int i=0; i <100000000;i++) {
			System.out.println(i);
			Threading.text.append(i+"\n");
		}
	}
}