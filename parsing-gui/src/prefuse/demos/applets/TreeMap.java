package prefuse.demos.applets;

import prefuse.util.ui.JPrefuseApplet;


public class TreeMap extends JPrefuseApplet {

    /**
	 * 
	 */
	private static final long serialVersionUID = 7251868201218491714L;

	public void init() {
        this.setContentPane(
            prefuse.demos.TreeMap.demo("/chi-ontology.xml.gz", "name"));
    }
    
} // end of class TreeMap
