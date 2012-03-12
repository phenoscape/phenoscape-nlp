

/* $Id: StreamGobblerWordNet.java 790 2011-04-11 17:57:38Z hong1.cui $ */
/**
* 
*/

package outputter;
import java.io.InputStream;
import java.util.ArrayList;

/**
* @author Hong Updates
*
*/
public class StreamGobblerWordNet extends StreamGobbler {

	/**
	 * @param is
	 * @param type
	 * @param headings
	 * @param trees
	 */
	public StreamGobblerWordNet(InputStream is, String type,
			ArrayList<String> headings, ArrayList<String> trees) {
		super(is, type, headings, trees);
		// TODO Auto-generated constructor stub
	}

	protected StringBuffer gobbleLine(String line, StringBuffer sb) {
		if(debug) System.out.println(type+">"+line);
		sb.append(line+" ");
		return sb;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}

