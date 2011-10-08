package fna.parsing;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hong Updates
 *
 */
@SuppressWarnings("unchecked")
public class Test{
	static Hashtable<String, String> monthmapping = new Hashtable<String, String>();
	static String monthring="jan-feb-mar-apr-may-jun-jul-aug-sep-oct-nov-dec-jan-feb-mar-apr-may-jun-jul-aug-sep-oct-nov-dec";
	static String value="(.*?)((jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec|spring|summer|fall|winter|year|round|late|early|mid|middle| |-)+)(.*)";		
	static String seasonring = "spring-summer-fall-winter-spring-summer-fall-winter";
	static String seasons = "(spring|summer|fall|winter)";
	static String months ="(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)";
	private String text = null;
	@SuppressWarnings("unused")
	private String enutag = null;
	public Test(String text, String enutag){
		monthmapping.put("jan", "winter");
		monthmapping.put("feb", "winter");
		monthmapping.put("mar", "spring");
		monthmapping.put("apr", "spring");
		monthmapping.put("may", "spring");
		monthmapping.put("jun", "summer");
		monthmapping.put("jul", "summer");
		monthmapping.put("aug", "summer");
		monthmapping.put("sep", "fall");
		monthmapping.put("oct", "fall");
		monthmapping.put("nov", "fall");
		monthmapping.put("dec", "winter");
		this.text = text;
		this.enutag = enutag;
	}
	
	public void parse(){
		text = text.toLowerCase().replaceFirst("flowering\\s+", "").replaceAll("–", "-");
		System.out.println("original: "+text);
		//clean up the text
		Pattern p = Pattern.compile(value);
		Matcher m = p.matcher(text);
		String clean = "";
		while(m.matches()){
			clean += m.group(2)+"@";
			text = m.group(4);
			m=p.matcher(text);			
		}
		System.out.println("cleaned: "+clean);
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
		
		String includedseasons = getSeasons(values);
			
			
		Iterator<String> it = values.iterator();
		
		while(it.hasNext()){
			String month = (String) it.next();
			System.out.println("add: "+month);
			String season = Test.monthmapping.get(month.toLowerCase());
			if(season !=null && includedseasons.indexOf(season)<0){
				System.out.println("add: "+season);
				includedseasons +=season+"@";
			}					
		}	
	}
	
	
	

	/**
	 * 
	 * @param times
	 * @return
	 */
	private ArrayList<String> allValuesInRange(String[] times) {
		ArrayList results = new ArrayList();
		String s = times[0];
		String e = times[times.length-1];
		String[] ss = s.split("\\s+");
		String[] es = e.split("\\s+");
		if((ss[ss.length-1].matches(Test.seasons) && es[es.length-1].matches(Test.months))||
		   (ss[ss.length-1].matches(Test.months) && es[es.length-1].matches(Test.seasons))	){
			//return original values
			dump2ArrayList(times, results);
		}else{
			Pattern p = Pattern.compile(".*?\\b("+ss[ss.length-1]+"\\b.*?\\b"+es[es.length-1]+")\\b.*");
			Matcher mm = p.matcher(Test.monthring);
			Matcher sm = p.matcher(Test.seasonring);
			if(mm.matches()){
				//collect all months
				dump2ArrayList(mm.group(1).split("-"), results);
			}else if(sm.matches()){
				dump2ArrayList(sm.group(1).split("-"), results);
			}			
		}
		return results;
	}

	private void dump2ArrayList(String[] array, ArrayList arrayList) {
		for(int i = 0; i <array.length; i++){
			arrayList.add(array[i]);
		}
	}

	/*
	 * return @-connected values
	 */
	private String getSeasons(ArrayList<String> values) {
		String seasons = "";
		Iterator it = values.iterator();
		while(it.hasNext()){
			String v = ((String)it.next()).trim();
			String[] t = v.split("\\s+");
			if(t[t.length-1].matches(Test.seasons)){
				seasons +=t[t.length-1]+"@";
			}
		}
		return seasons;
	}
	
	public void deduplicateSort(List<String> tagList) {
		HashSet<String> set = new HashSet<String>(tagList);
		String[] sorted = set.toArray(new String[]{}); 
		Arrays.sort(sorted);
		tagList = Arrays.asList(sorted);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Test t = new Test("","");
		ArrayList<String> tagList = new ArrayList<String>();
		tagList.add("bark");
		tagList.add("stem");
		tagList.add("stems");
		tagList.add("barks");
		tagList.add("bark");
		
		t.deduplicateSort(tagList);
		
		
		
		
		
		
		/*String[] tests = new String[]{
				"Flowering year round (mostly Mar–Aug).", 
				"Flowering (Mar–)Apr(–Jun)", 
				"Flowering Mar–Nov (following rains).  ",
				"Flowering Dec–Mar. ", 
				"Flowering Dec–Apr(–May in North Carolina).  ", 
				"Flowering (chasmogamous) Mar–early Jun, (cleistogamous) Aug–Nov. ", 
				"Flowering summer (Jun–Aug).  ", 
				"Flowering spring–winter (Apr–Jul).  "};
		for(int i = 0; i<tests.length; i++){
			Test t = new Test(tests[i], "flowering_time");
			t.parse();
		}*/

	}

}

