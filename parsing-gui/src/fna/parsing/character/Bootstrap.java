package fna.parsing.character;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;

public class Bootstrap implements Comparator<StateGroup>{
	
	
	private ArrayList<StateGroup> source;
	private Glossary glossary;
	//private String tablename; //save the discoveries made from bootstrapping iterations
	static private Connection conn = null;
	
	@SuppressWarnings("unused")
	static private String username = ApplicationUtilities.getProperty("database.username");
	@SuppressWarnings("unused")
	static private String password = ApplicationUtilities.getProperty("database.password");
	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(Bootstrap.class);
	
	public Bootstrap(ArrayList<StateGroup> source, Glossary glossary, String database) {
	//public Bootstrap(ArrayList source, String database) {
		try{
			if(conn == null){
				Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		this.source = source;
		this.glossary = glossary;
		//this.tablename = learnttable;
	}
	/*public Bootstrap(ArrayList source, Glossary glossary, String database, String learnttable) {
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		this.source = source;
		this.glossary = glossary;
		this.tablename = learnttable;
	}*/
	
	public void go(){
		int total = 0;
		int count = 0;
		do{
			count = 0;
			Collections.sort(source, this);//resort the source
			Iterator<StateGroup> it = source.iterator();
			while(it.hasNext()){
				StateGroup sg = (StateGroup)it.next();
				count += inferCharacters(sg);//add discoveries (term, category) to glossary
			}
			total += count;
		}while(count > 0);
		System.out.println("learned categories for "+total+" states");
		//TODO list orphaned/free states 
	}

	/**
	 * all states in a group may or may not share one category
	 * @param sg
	 * @return the number of terms inserted into glossary
	 */
	@SuppressWarnings("static-access")
	public int inferCharacters(StateGroup sg){
		int count = 0;
		//single state case, skip
		if(sg.size() <=1){
			return count;
		}
		//known unknown case
		//A/B ? :return 0
		//A ?: ?=A
		if(sg.size() == 2 && sg.numberOfAssociated() == 1 && sg.getCount() > 3){
			ArrayList<String> cats = sg.seenCategories();
			if(cats.size() >1){
				return count;
			}
			ArrayList<?> unknowns = sg.nonCategoryStates((String)cats.get(0));
			glossary.addInducedPair(((State)unknowns.get(0)).toString(), cats);
			count++;
			System.out.println(((State)unknowns.get(0)).toString()+" is labeled as ["+(String)cats.get(0)+"], in group "+sg.toString()+"--a new discovery========");
			return count;
		}
		// known unknown known
		// unknown= shared if exist
		// unknown = majority of the states to the right of unknown 
		if(sg.size() > 2 && sg.numberOfAssociated() >=2 && sg.numberOfAssociated() <sg.size() && sg.getCount() >=2){
			//make sure the unknown is not the first state in the group
			String mostfreq = sg.mostFreqCategory();
			String[] mf = mostfreq.split("#");// "position#111";
			String cat = mf[0];
			int freq = mf[1].length();
			if(freq ==sg.numberOfAssociated()){ //found shared by all
				ArrayList<?> unknowns = sg.nonCategoryStates(cat);
				Iterator<?> it = unknowns.iterator();
				while(it.hasNext()){
					State s = (State)it.next();
					int pos = sg.getIndex(s);
					if(pos != 0){
						ArrayList<String> cats = new ArrayList<String>();
						cats.add(cat);
						glossary.addInducedPair(s.toString(), cats);
						count++;
						System.out.println(s.toString()+" is labeled as ["+cat+"], in group "+sg.toString()+"--a new discovery========");
					}
				}
				return count; //states have a shared character
			}
			StateGroup sgnew = new StateGroup();
			for(int i = 1; i < sg.size(); i++){
				sgnew.addState(sg.getState(i));
			}
			mostfreq = sgnew.mostFreqCategory();
			mf = mostfreq.split("#");// "position#111";
			cat = mf[0];
			freq = mf[1].length();
			ArrayList<?> unknowns = sgnew.nonCategoryStates(cat);
			Iterator<?> it = unknowns.iterator();
			while(it.hasNext()){
				State s = (State)it.next();
				ArrayList<String> cats = new ArrayList<String>();
				cats.add(cat);
				glossary.addInducedPair(s.toString(), cats);
				count++;
				System.out.println(s.toString()+" is labeled as ["+cat+"], in group "+sg.toString()+"--a new discovery========");
			}
			return count; //states have a shared character
			}
		if(sg.freeStates().size() > 0){
			System.out.println("not processed group: "+sg.toString()+"count: "+sg.getCount());
			System.out.println("\t free states: "+sg.freeStates().toString());
			System.out.println("\tfirst states: "+sg.getState(0).toString());
			System.out.println("\t most freq cat: "+sg.mostFreqCategory());
		}
		return count;
		}
	
		
		/*String mostfreq = sg.mostFreqCategory();
		if(mostfreq.compareTo("#")==0){
			System.out.println(sg.toString()+"group is unknown at this time");
			return count;
		}
		String[] mf = mostfreq.split("#");// "position#111";
		String cat = mf[0];
		int freq = mf[1].length();
		if(freq ==sg.size()){
			System.out.println(sg.toString()+" is labeled as ["+cat+"]");
			return count; //states have a shared character
		}
		//entire [margin], lobed [solid shape][plane shape]
		ArrayList states = sg.nonCategoryStates(cat);
		int number = sg.numberOfAssociated();
		//make the most frequent category the category for free states
		//If the categories are uniformed distributed, withhold the decision for this iteration
		float v = (float)freq/number;
		if(v >= 0.66){
			Iterator it = states.iterator();
			while(it.hasNext()){
				ArrayList list = new ArrayList();
				list.add(cat);
				String temp = ((State)it.next()).toString();
				glossary.addInducedPair(temp,list); 
				count++;
				System.out.println(temp+" is labeled as ["+cat+"], in group "+sg.toString()+"--a new discovery========");
			}
		}else{
			Iterator it = states.iterator();
			while(it.hasNext()){
				String temp = ((State)it.next()).toString();
				System.out.println(temp+" is unknown at this time, in group "+sg.toString());
			}
		}
		return count;
	}*/
	
	
	/**
	 * order by numberOfAssociated, size 
	 */
	public int compare(StateGroup g1, StateGroup g2){
		//if(g1.toString().compareTo(g2.toString()) == 0){
		//	return 0;
		//} each g is different, not possible to return 0
		
		//int known1 = g1.numberOfAssociated();
		//int known2 = g2.numberOfAssociated();
		int count1 = g1.getCount();
		int count2 = g2.getCount();
		//int v =known1 - known2;
		//if(v == 0){
			return count2 - count1; //any order is fine
		//}else{
			//return v;
		//}
	}
}
