package prefuse.demos.applets;

import prefuse.util.ui.JPrefuseApplet;


public class DataMountain extends JPrefuseApplet {

    /**
	 * 
	 */
	private static final long serialVersionUID = -8974380070557609590L;

	public void init() {
        this.setContentPane(prefuse.demos.DataMountain.demo());
    }
    
} // end of class DataMountain
