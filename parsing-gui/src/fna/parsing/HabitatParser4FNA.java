/**
 * 
 */
package fna.parsing;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fna.db.HabitatParserDbAccessor;

/**
 * @author Hong Updates
 *
 */

public class HabitatParser4FNA implements Learn2Parse{
	private HabitatParserDbAccessor hpDbA = null;
	private String seedNList = "|";
	private String seedMList = "|";
	//private static Hashtable content = new Hashtable(); //tagged with <> and {}
	public HabitatParser4FNA(String dataprefix){
		//text is the database table name: source ->habitate string
		File source = new File(Registry.TargetDirectory,
				ApplicationUtilities.getProperty("HABITATS"));
		//File source = new File("C:/DATA/FNA-v19/target/habitats");
		//construct a database table for habitat data
		this.hpDbA = new HabitatParserDbAccessor(dataprefix);
		hpDbA.createTable();
		hpDbA.populateTable(source);		
	}
	
	/**
	 * perform bootstrapping
	 */
	public void parse(){
		//collect seeds
		collectSeeds();
		//bootstrapping
		bootstrap();
	}
	
	private void bootstrap() {
		// TODO Auto-generated method stub
		int discovery = 0;
		ArrayList<String> todos =null;
		do{
			discovery = 0;
			todos = hpDbA.selectRecords("source, habitat_string", "isnull(habitat_values)", "", "");
			discovery += tagHabitatStrings(todos);
			discovery += findNew(todos);			
		}while (discovery > 0);

		System.out.println("remining: ");
		Iterator<String> it = todos.iterator();
		while(it.hasNext()){
			String r = (String)it.next().trim().toLowerCase();
			
			String src = r.split("@")[0];
			String hs = r.split("@")[1];
			if(hs.trim().length() > 1){
				System.out.println(hs);
				hs = hs.replaceAll(">,", "@").replaceAll("(?<=s),", "@").replaceAll("[<>{}]", "");
				hpDbA.updateRecord(src, "??"+hs, "habitat_values", "source='"+src.trim()+"'");	
			}
		}
		
		
	}
	
	/**
	 * rules: can only learn {}s
	 * ^ Oak <scrub> => {oak}
	 * , saltbush <scrub> => {saltbush}
	 * 
	 * goal: when each , follows a [}>], then the hab. string is done!
	 * <>, x y z <0> => <>, <x y z 0> 
	 * 
	 * find new {}s
	 */

	private int findNew(ArrayList<String> todos) {
		int discovery = 0;
		Pattern p = Pattern.compile("(.*?)(\\w+)(\\s+<.*)");
		Iterator<String> it  = todos.iterator();
		while(it.hasNext()){
			String r = (String)it.next().trim().toLowerCase();
			String hs = r.split("@")[1];
			discovery += modifierBeforeH(p, hs);
		}
		return discovery;
	}

	private int modifierBeforeH(Pattern p, String h) {
		int discovery = 0;
		Matcher m = p.matcher(h);
		while(m.matches()){
			h = m.group(3);
			String temp = m.group(2);
			if(temp.length()>1 && seedMList.indexOf("|"+temp+"|") < 0){
				this.seedMList+=temp+"|";
				discovery++;
			}			
			m = p.matcher(h);
		}
		return discovery;
	}

	/**
	 * 
	 * @return a regexp consisting a list of seeds
	 */
	private void collectSeeds() {
		ArrayList<String> text = hpDbA.selectRecords("source, habitat_string", "", "", "");
		Iterator<String> it = text.iterator();
		while(it.hasNext()){
			String r = (String)it.next().trim().toLowerCase();
		//	String src = r.split("@")[0];
			String hs = r.split("@")[1];
			hs = hs.replaceAll("\\s*\\([^)]*\\)\\s*", " ");
			hs = hs.replaceAll("[\\W|\\s]+$", "");
			//the last word in a statement must be a N
			String seed = hs.substring(hs.lastIndexOf(" ")+1, hs.length()).trim();
			if(seed.length() > 1 && seedNList.indexOf("|"+seed+"|") < 0){
				seedNList +=seed+"|";
			}
			
			//"distributed sites"
			if(hs.indexOf(",")<0 && hs.indexOf(" ")>=0 && hs.indexOf(" ") == hs.lastIndexOf(" ")&& hs.indexOf(" and ")<0){
				String[] t = hs.split("\\s+");
				if(t[t.length-2].length()> 1 && seedMList.indexOf("|"+t[t.length-2]+"|") < 0){
					seedMList +=t[t.length-2]+"|";
				}
			}
			
			//"meadows and tundra"
			if(hs.matches(".*?\\w+ and \\w+$")){
				String[] t = hs.split("\\s+");
				if(t[t.length-3].length()> 1 && seedNList.indexOf("|"+t[t.length-3]+"|") < 0){
					seedNList +=t[t.length-3]+"|";
				}
			}
		}										
	}
	
	private int tagHabitatStrings(ArrayList<String> text) {
		int discovery = 0;
		Pattern p = Pattern.compile("(.*?)(\\w+)(\\s+<.*)");
		Iterator<String> it = text.iterator();
		int index = 0;
		while(it.hasNext()){
			String r = (String)it.next().trim().toLowerCase();
			String src = r.split("@")[0];
			String hs = r.split("@")[1];
			hs = mark(hs, seedNList, "<", ">");
			hs = mark(hs, seedMList, "{", "}"); //could have <{ }>
			if(isDone(hs)){
				discovery += modifierBeforeH(p, hs);
				hs = hs.replaceAll(">,", "@").replaceAll("[<>{}]", "");
				hpDbA.updateRecord(src, hs, "habitat_values", "source='"+src.trim()+"'");
				//text.set(index++, src+"@");
				it.remove();
			}else{
				//this.content.put(r, hs);
				text.set(index++, src+"@"+hs);
			}
			
		}
		return discovery;
	}
	
	private boolean isDone(String string){
	//	String sc = string;
		string = string.replaceAll(">,", "").replaceAll("},", "");
		if(string.indexOf(",") < 0){
			return true;
		}
		return false;
	}
	
	private String mark(String s, String list, String l, String r){
		s = s.replaceAll("["+l+r+"]", "");
		String[] ns = list.split("\\|");
		for(int i = 0; i<ns.length; i++){
			if(ns[i].compareTo("") !=0){
				//escape 
				ns[i] = ns[i].replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
				s = s.replaceAll("\\b"+ns[i]+"\\b", l+ns[i]+r);
			}
		}
		return s;
	}
	
	public ArrayList<String> getMarkedDescription(String src){
		ArrayList<String> list = new ArrayList<String>();
		
		ArrayList<String> h = hpDbA.selectRecords("habitat_values", "source='"+src+"'", "", "");
		if(h.size()>=1){
			String hs = h.get(0);
			hs = hs.replaceFirst("^\\?+", "");
			String[] hsegs = hs.split("@");
			for(int i = 0; i<hsegs.length; i++){
				if(hsegs[i].trim().length() > 0){
					String temp = hsegs[i].trim().replaceFirst("\\W+$", "");
					list.add("<habitat>"+temp+"</habitat>");
				}
			}			
		}
		return list;
		
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//HabitatParser4FNA hpf = new HabitatParser4FNA();
		//hpf.parse();

	}

}
