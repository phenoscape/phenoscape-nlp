/**
 * 
 */
package outputter.data;

import java.util.ArrayList;

/**
 * @author Hong Cui
 *
 */
public interface Proposals {
    //change object to a superclass of QualityP and EntityP.
	public boolean add(Object c);
	public Object getProposals();
	public String getPhrase();
	//public void setPhrase(String phrase);
	public float higestScore();

}
