/**
 * 
 */
package fna.parsing;

import java.util.regex.*;
import java.util.*;

import org.jdom.Element;

/**
 * @author Hong Updates
 *
 */
public class DistributionParser4FNA extends EnumerativeElementParser {

	/**
	 * @param parent
	 * @param text
	 */
	public DistributionParser4FNA(Element parent, String text, String enutag) {
		super(parent, text, enutag);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see fna.parsing.ElementParser#parse()
	 */
	@Override
	protected Element parse() {
		//format text, hide [,;] in parentheses
		ArrayList<String> values = new ArrayList<String>();
		text = format(text); 
		
		//collect values
		String[] areas = text.split("[;,]");
		for(int i = 0; i<areas.length; i++){
			String area = areas[i].trim();
			if(area.indexOf("@")>=0){
				values.addAll(allValues(area));				
			}else{
				values.add(area);
			}
			
		}
		//form elements
		Iterator<String> it = values.iterator();
		while(it.hasNext()){
			String area = (String)it.next();
			if(area.compareTo("") !=0){
					Element enuelement = new Element(enutag);
					enuelement.setText(area);
					//System.out.println("add "+enutag+": "+area);
					parent.addContent(enuelement);
			}		
		}
		
		return parent;
	}
	
	/**
	 * mexican (a@b) =>mexican(a), mexican(b)
	 * @param area
	 * @return
	 */

	private ArrayList<String> allValues(String area) {
		ArrayList<String> values = new ArrayList<String>();
		Pattern p = Pattern.compile("(.*?)\\(([^)]*?@[^)]*?)\\)(.*)");
		Matcher m = p.matcher(area);
		if(m.matches()){
			String com = m.group(1);
			String partstr = m.group(2);
			String rest = m.group(3);
			
			String[] parts = partstr.split("\\s*@\\s*");
			
			for(int i = 0; i<parts.length; i++){
				values.add(com+"("+parts[i]+")"+rest);
			}
		}
		return values;
	}

	private String format(String text) {
		String formated = "";
		Pattern p = Pattern.compile("(.*?)(\\([^)]*,[^)]*\\))(.*)"); 
		Matcher m = p.matcher(text);
		while(m.matches()){
			formated += m.group(1);
			String t = m.group(2);
			text = m.group(3);
			t = t.replaceAll(",", "@");
			formated +=t;
			m = p.matcher(text);
		}
		formated +=text;
		return formated;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DistributionParser4FNA p = new DistributionParser4FNA(null, "mexic(a, b, c, d), asian", "distribution");
		p.parse();

	}

}
