/**
 * 
 */
package outputter;

import java.util.ArrayList;

/**
 * @author Hong Cui
 *
 */
public interface Proposals {

	public boolean add(Object c);
	public Object getProposals();
	public String getPhrase();
	//public void setPhrase(String phrase);
	public float higestScore();

}
