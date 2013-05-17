/**
 * 
 */
package fna.parsing;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;

/**
 * @author Hong Updates
 *
 */

public class FloweringTimeParser4FNA extends EnumerativeElementParser{
	static Hashtable<String, String> m2smapping = new Hashtable<String, String>();
	static Hashtable<String, String> s2mmapping = new Hashtable<String, String>();
	static String monthring="jan-feb-mar-apr-may-jun-jul-aug-sep-oct-nov-dec-jan-feb-mar-apr-may-jun-jul-aug-sep-oct-nov-dec";
	static String value="(.*?)((jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|spring|summer|fall|winter|year|round|late|early|mid|middle| |-)+)(.*)";		
	static String seasonring = "spring-summer-fall-winter-spring-summer-fall-winter";
	static String seasons = "(spring|summer|fall|winter)";
	static String months ="(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)";
	public FloweringTimeParser4FNA(Element parent, String text, String enutag){
		super(parent, text, enutag);
		m2smapping.put("jan", "winter");
		m2smapping.put("feb", "winter");
		m2smapping.put("mar", "spring");
		m2smapping.put("apr", "spring");
		m2smapping.put("may", "spring");
		m2smapping.put("jun", "summer");
		m2smapping.put("jul", "summer");
		m2smapping.put("aug", "summer");
		m2smapping.put("sep", "fall");
		m2smapping.put("oct", "fall");
		m2smapping.put("nov", "fall");
		m2smapping.put("dec", "winter");
		
		s2mmapping.put("spring", "mar@apr@may");
		s2mmapping.put("summer", "jun@jul@aug");
		s2mmapping.put("fall", "sep@oct@nov");
		s2mmapping.put("winter", "dec@jan@feb");
	}
	
	public Element parse(){
		text = text.toLowerCase().replaceFirst("flowering\\s+", "").replaceAll("–", "-");
		//System.out.println("original: "+text);
		//clean up the text
		Pattern p = Pattern.compile(value);
		Matcher m = p.matcher(text);
		String clean = "";
		while(m.matches()){
			clean += m.group(2)+"@";
			text = m.group(4);
			m=p.matcher(text);			
		}
		//System.out.println("cleaned: "+clean);
		//fetch the values
		String[] ranges = clean.split("\\s*@\\s*");
		ArrayList<String> values = new ArrayList<String>();
		for(int i = 0; i<ranges.length; i++){
			String range = ranges[i].trim();
			if(range.indexOf("-")>=0){
				range = range.replaceFirst("^-+", "").replaceFirst("-+$", "");
				String times[] = range.split("-");
				if(times.length>1){
					values.addAll(allValuesInRange(times));
				}else{
					values.add(times[0]);
				}
			}else{
				values.add(range);
			}
		}
		
		formElements(values);
		return parent;	
	}

	private void formElements(ArrayList<String> values) {
		String includedseasons = getSeasons(values);
		String includedmonths = getMonths(values);	
			
		Iterator<String> it = values.iterator();
		
		while(it.hasNext()){
			String month = (String) it.next();
			if(month.compareTo("")!=0){
				Element enuelement = new Element(enutag);
				enuelement.setText(month);
				parent.addContent(enuelement);
				//System.out.println("add: "+month);
				if(month.indexOf("year")>=0){
					addAllMonthsSeasons();
					includedseasons = FloweringTimeParser4FNA.seasons.replaceAll("\\W", "@");
					includedmonths = FloweringTimeParser4FNA.months.replaceAll("\\W", "@");
				}
				
				//add corresponding seasons for the month (if this is a month values)
				String season = FloweringTimeParser4FNA.m2smapping.get(month.toLowerCase());
				if(season !=null && includedseasons.indexOf(season)<0){
					addElement(season);
					includedseasons +=season+"@";
				}					
				
				//add corresponding months for the season (if this is a season value)
				String monthlist = FloweringTimeParser4FNA.s2mmapping.get(month.toLowerCase());
				if( monthlist!=null){ 
					String[] months = monthlist.split("\\s*@\\s*");
					for(int i=0; i<months.length; i++){
						if(months[i].compareTo("")!=0 && includedmonths.indexOf(months[i])<0){
							addElement(months[i]);
							includedmonths +=months[i]+"@";					
						}			
					}
				}
			}
		}
	}

	private void addAllMonthsSeasons() {
		Set<String> seasons = FloweringTimeParser4FNA.s2mmapping.keySet();
		Set<String> months = FloweringTimeParser4FNA.m2smapping.keySet();
		Iterator<String> s = seasons.iterator();
		while(s.hasNext()){
			String season = (String)s.next();
			addElement(season);
		}
		
		Iterator<String> m = months.iterator();
		while(m.hasNext()){
			String season = (String)m.next();
			addElement(season);
		}
	}

	private void addElement(String season) {
		Element enuelement;
		enuelement = new Element(enutag);
		enuelement.setText(season);
		parent.addContent(enuelement);
		//System.out.println("add: "+season);
	}
	
	
	

	/**
	 * 
	 * @param times
	 * @return
	 */
	
	private ArrayList<String> allValuesInRange(String[] times) {
		ArrayList<String> results = new ArrayList<String>();
		String s = times[0];
		String e = times[times.length-1];
		String[] ss = s.split("\\s+");
		String[] es = e.split("\\s+");
		if((ss[ss.length-1].matches(FloweringTimeParser4FNA.seasons) && es[es.length-1].matches(FloweringTimeParser4FNA.months))||
		   (ss[ss.length-1].matches(FloweringTimeParser4FNA.months) && es[es.length-1].matches(FloweringTimeParser4FNA.seasons))	){
			//return original values
			dump2ArrayList(times, results);
		}else{
			Pattern p = Pattern.compile(".*?\\b("+ss[ss.length-1]+"\\b.*?\\b"+es[es.length-1]+")\\b.*");
			Matcher mm = p.matcher(FloweringTimeParser4FNA.monthring);
			Matcher sm = p.matcher(FloweringTimeParser4FNA.seasonring);
			if(mm.matches()){
				//collect all months
				dump2ArrayList(mm.group(1).split("-"), results);
			}else if(sm.matches()){
				dump2ArrayList(sm.group(1).split("-"), results);
			}			
		}
		return results;
	}

	private void dump2ArrayList(String[] array, ArrayList<String> arrayList) {
		for(int i = 0; i <array.length; i++){
			arrayList.add(array[i]);
		}
	}

	/*
	 * return @-connected values
	 */
	private String getSeasons(ArrayList<String> values) {
		String seasons = "";
		Iterator<String> it = values.iterator();
		while(it.hasNext()){
			String v = ((String)it.next()).trim();
			String[] t = v.split("\\s+");
			if(t[t.length-1].matches(FloweringTimeParser4FNA.seasons)){
				seasons +=t[t.length-1]+"@";
			}
		}
		return seasons;
	}

	/*
	 * return @-connected values
	 */
	private String getMonths(ArrayList<String> values) {
		String months = "";
		Iterator<String> it = values.iterator();
		while(it.hasNext()){
			String v = ((String)it.next()).trim();
			String[] t = v.split("\\s+");
			if(t[t.length-1].matches(FloweringTimeParser4FNA.months)){
				months +=t[t.length-1]+"@";
			}
		}
		return months;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
