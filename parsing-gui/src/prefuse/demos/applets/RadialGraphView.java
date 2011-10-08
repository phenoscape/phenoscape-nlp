package prefuse.demos.applets;

import prefuse.util.ui.JPrefuseApplet;


public class RadialGraphView extends JPrefuseApplet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 4662402088440959222L;

	public void init() {
        this.setContentPane(
            prefuse.demos.RadialGraphView.demo("/socialnet.xml", "name"));
    }
    
} // end of class RadialGraphView
