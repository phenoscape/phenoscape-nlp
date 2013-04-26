/**
 * 
 */
package outputter;

import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 * @author Hong Cui
 * 
 * Uses Chain of Responsibility pattern
 *
 */
public abstract class EntitySearcher {
	protected static XPath textpath;
	static{
		try{
			textpath = XPath.newInstance(".//text");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	
	/*whether the request can be handled by this searcher */
	//public abstract boolean canHandle (Element root, String structid,  String entityphrase, String elocatorphrase, String originalentityphrase, String prep, int ingroup);
	/*handle the request*/
	public abstract Entity searchEntity(Element root, String structid,  String entityphrase, String elocatorphrase, String originalentityphrase, String prep, int ingroup);
	/*otherwise, set another handler to handle the request*/
   //public abstract void  setHandler(EntitySearcher handler, Element root, String structid,  String entityphrase, String elocatorphrase, String originalentityphrase, String prep, int ingroup);
	
}
