package fna.parsing.character;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Hashtable;

import fna.parsing.ApplicationUtilities;


/**
 * run through a columne in a database, remove hyphens that separate one word
 * @author hongcui
 *
 */
@SuppressWarnings({ "unchecked", "unused" })
public class DeHyphenizer {
	private String tablename;
	private String columnname;
	private String countcolumn;
	private String hyphen; 
	static private Connection conn = null;
	//static private String username = "termsuser";
	//static private String password = "termspassword";
	
	public DeHyphenizer(String database, String table, String column, String countcolumn, String hyphen) {
		// TODO Auto-generated constructor stub
		try{
			if(conn == null){
				String URL = ApplicationUtilities.getProperty("database.url");
				conn = DriverManager.getConnection(URL);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		this.tablename = table;
		this.columnname = column;
		this.countcolumn = countcolumn;
		this.hyphen = hyphen;
	}

	public void deHyphen(){
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select "+columnname+" from "+tablename+" where "+columnname+" like '%\\"+hyphen+"%'");
			while(rs.next()){
				String hyphenedterm = rs.getString(1);
				String[] parts = hyphenedterm.split("\\s+");
				String term = "";
				for(int i = 0; i<parts.length; i++){
					term += normalFormat(parts[i])+" ";
				}
				term = term.trim();
				if(term.compareTo(hyphenedterm) != 0){
					//System.out.println("\nold term: "+hyphenedterm);
					//System.out.println("new term: "+term);
					updateTable(hyphenedterm, term);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private String normalFormat(String hyphened){
		hyphened = hyphened.replaceFirst("^_", "");
		String[] segs = hyphened.split(this.hyphen);
		String[] terms = new String[segs.length];
		int[][] matrix = new int[segs.length][segs.length];
		//fill matrix
		for(int i = 0; i < segs.length; i++){
			for(int j = i; j<segs.length; j++){
				matrix[i][j] = isTerm(segs, i, j);
				matrix[j][i] = matrix[i][j];
			}
		}
		//rank rows
		int max = 0;
		Hashtable rank = new Hashtable();
		for(int i = 0; i < segs.length; i++){
			int ones = countOnes(matrix[i]);
			if(ones > max){
				max = ones;
			}
			String list = (String)rank.get(ones+"");
			if(list == null){
				rank.put(ones+"", i+"");
			}else{
				rank.put(ones+"", list+" "+i+"");
			}
		}
		//collect terms
		String checked="-";
		for(int i = 0; i <= max; i++){
			String list = (String)rank.get(i+"");
			if(list!= null && i == 0){//term not see in learned or glossary, and not connectable to other terms
				String[] indexes = list.split(" ");
				for(int j = 0; j < indexes.length; j++){
					int ind = Integer.parseInt(indexes[j]);
					terms[ind] = segs[ind];
				}

			}else if(list!=null){
				String[] indexes = list.split(" ");
				for(int j = 0; j < indexes.length; j++){
					int ind = Integer.parseInt(indexes[j]);
					if(checked.indexOf("-"+ind+"-")<0){
						int lastk = otherEndIndex(matrix[ind], ind); //last index of 1 in the row
					    terms[ind] = ind > lastk? formTerm(segs, lastk, ind) : formTerm(segs, ind, lastk);
					    checked += ind > lastk? formString(lastk, ind, "-") : formString(ind, lastk, "-");
					}
				}
			}
		}
		//out put term
		String term = "";
		for(int i =0; i<terms.length; i++){
			if(terms[i] != null){
				term += terms[i]+" ";
			}
		}
		return term.trim();
	}
	protected String formString(int start, int end, String connector){
		String str = "";
		for(int i = start; i<=end; i++){
			str +=i+connector;
		}
		return str;
	}
	
	protected String formTerm(String[] segs, int start, int end){
		String str="";
		for(int i = start; i<=end; i++){
			str +=segs[i];
		}
		return str;
	}
	
	private int otherEndIndex(int [] array, int theotherindex){
		boolean self = false;
		int index = -1;
		for(int i = 0; i< array.length; i++){
			if(array[i]==1 && i == theotherindex){
				self = true;
			}else if(array[i]==1){//take the greatest index, may cause problem here.
				index = i;
			}
		}
		
		return index==-1? theotherindex : index;
	}
	
	private int countOnes(int[] array){
		int count = 0;
		for(int i = 0; i< array.length; i++){
			if(array[i]==1){
				count++;
			}
		}
		return count;
	}
	
	private int isTerm(String[] segs, int start, int end){
		String str=formTerm(segs, start, end);
		if(stringMatchInGloss(str) || stringMatch(str)){
			return 1;
		}
		return 0;
	}
		/*ArrayList partindexes = new ArrayList();
		
		for(int i =0; i < segs.length; i++){
			if(!stringMatchInGloss(segs[i]) && !stringMatch(segs[i])){
				partindexes.add(new Integer(i));
			}
		}
        String term = "";
		Iterator it = partindexes.iterator();
		String checked ="";
		while(it.hasNext()){
			Integer i = (Integer)it.next();
			int index = i.intValue();
			if(checked.indexOf("-"+index+"-")>=0){
				continue;
			}
			String templ = segs[index];
			String tempr = segs[index];
			String checkedl = "-";
			String checkedr = "-";
			int ir = segs.length-1-index;
			int il = index;
			//right-ward
			for(int j = index+1; j < segs.length; j++){
				tempr = tempr + segs[j];
				if(stringMatchInGloss(tempr) || stringMatch(tempr)){
					ir = j - index;
					checkedr +=j+"-";
					break;
				}
			}
			//left-ward
			for(int j = index-1; j >=0; j--){
				templ = templ+segs[j];
				if(stringMatchInGloss(templ) || stringMatch(templ)){
					il =  index - j;
					checkedl +=j+"-";
					break;
				}
			}
			//
			String temp = ir >= il? templ : tempr;
			checked = ir >= il? checkedl : checkedr;
		return term;*/
		/*String dehyphened = hyphened.replace(hyphen, "");
		String spaced = hyphened.replace(hyphen, " ");
		if(stringMatchInGloss(dehyphened) || stringMatch(dehyphened)){
			return dehyphened;
		}
		return spaced;*/
	//}
	
	private void updateTable(String oldt, String newt){
		try{
			Statement stmt = conn.createStatement();
			if(countcolumn == null ||countcolumn.trim().compareTo("") == 0){//without count
				ResultSet rs = stmt.executeQuery("select "+columnname+" from "+tablename+" where "+columnname+" = '"+newt+"'");
				if(!rs.next()){//newt not exist
					stmt.execute("update "+tablename+" set "+columnname+" = '"+newt+"' where "+columnname+" = '"+oldt+"'");
					//System.out.println("no count, update old with new ");
				}else{
					stmt.execute("delete from "+tablename+" where "+columnname+" = '"+oldt+"'");
					//System.out.println("no count, new exists, remove old");
				}
			}else{//with count
				int total  = 0;
				boolean newexist = false;
				ResultSet rs = stmt.executeQuery("select "+countcolumn+" from "+tablename+" where "+columnname+" = '"+oldt+"'");
				if(rs.next()){
					total += rs.getInt(1);
				}
			
				rs = stmt.executeQuery("select "+countcolumn+" from "+tablename+" where "+columnname+" = '"+newt+"'");
				if(rs.next()){
					total += rs.getInt(1);
					newexist = true;
				}
				if(newexist){
					stmt.execute("delete from "+tablename+" where "+columnname+" = '"+oldt+"'");
					stmt.execute("update "+tablename+" set "+countcolumn+" = "+ total+" where "+columnname+" = '"+newt+"'");
					//System.out.println("count, new exist, remove old, update count for new: "+total);
				}else{
					String query = "update "+tablename+" set "+countcolumn+" = "+ total+", "+columnname+" = '"+newt+"' where "+columnname+" = '"+oldt+"'";
					stmt.execute(query);
					//System.out.println("count, update old with new  with count: "+total);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public boolean stringMatchInGloss(String term){
		boolean find = false;
		try{
			Statement stmt = conn.createStatement();
			String query = "select term from fnaglossary where term like '% "+term+" %' or term like '"+term+" %' or term like '% "+term+"' or term like '"+term+"'";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				find = true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return find;
	}
	
	public boolean stringMatch(String term){
		boolean find = false;
		try{
			Statement stmt = conn.createStatement();
			String query = "select "+this.columnname+" from "+tablename+" where "+this.columnname+" like '% "+term+" %' or "+this.columnname+" like '"+term+" %' or "+this.columnname+" like '% "+term+"' or "+this.columnname+" like '"+term+"'";
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				find = true;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return find;
	}
	/*
	public void deHyphenWithoutCount(){
		if(countcolumn == null ||countcolumn.trim().compareTo("") == 0){
			try{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select "+columnname+" from "+tablename+" where "+columnname+" like '%\\"+hyphen+"%'");
				while(rs.next()){
					String hyphenedterm = rs.getString(1);
					String dehyphenedterm = hyphenedterm.replace(hyphen, "");
					Statement stmt1 = conn.createStatement();
					ResultSet rs1 = stmt1.executeQuery("select "+columnname+" from "+tablename+" where "+columnname+" = '"+dehyphenedterm+"'");
					if(rs1.next()){
						stmt1.execute("delete from "+tablename+" where "+columnname+" = '"+hyphenedterm+"'");
					}else{
						String term = hyphenedterm.replace(hyphen, " ");
						stmt1.execute("update "+tablename+" set "+columnname+" = "+term+" where "+columnname+" = '"+hyphenedterm+"'");
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}else{
			deHyphenWithCount();
		}
	}
	
	public void deHyphenWithCount(){
		if(countcolumn != null ||countcolumn.trim().compareTo("") != 0){
			try{
				Statement stmt = conn.createStatement();
				String query ="select "+columnname+","+countcolumn+" from "+tablename+" where "+columnname+" like '%\\"+hyphen+"%'";
				ResultSet rs = stmt.executeQuery(query);
				while(rs.next()){
					String hyphenedterm = rs.getString(1);
					int hyphenedcount = rs.getInt(2);
					String dehyphenedterm = hyphenedterm.replace(hyphen, "");
					Statement stmt1 = conn.createStatement();
					ResultSet rs1 = stmt1.executeQuery("select "+countcolumn+" from "+tablename+" where "+columnname+" = '"+dehyphenedterm+"'");
					if(rs1.next()){
						int dehyphenedcount = rs1.getInt(1);
						if(dehyphenedcount >= hyphenedcount){
							stmt1.execute("delete from "+tablename+" where "+columnname+" = '"+hyphenedterm+"'");
							stmt1.execute("update "+tablename+" set "+countcolumn+" = "+ (dehyphenedcount+hyphenedcount)+" where "+columnname+" = '"+dehyphenedterm+"'");
							System.out.println("removed "+hyphenedterm+" add the count for "+dehyphenedterm+" by "+hyphenedcount);
						}
					}else{
						String term = hyphenedterm.replace(hyphen, " ");
						rs1 = stmt1.executeQuery("select "+countcolumn+" from "+tablename+" where "+columnname+" = '"+term+"'");
						if(rs1.next()){
							hyphenedcount += rs1.getInt(1);
							stmt1.execute("delete from "+tablename+" where "+columnname+" = '"+hyphenedterm+"'");
							stmt1.execute("update "+tablename+" set "+countcolumn+" = "+ hyphenedcount+" where "+columnname+" = '"+term+"'");
							System.out.println("removed "+hyphenedterm+" update the count for "+term+" to "+hyphenedcount);

						}else{
							stmt1.execute("update "+tablename+" set "+columnname+" = '"+term+"' where "+columnname+" = '"+hyphenedterm+"'");
							System.out.println("changed "+hyphenedterm+" to "+term);
						}
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}else{
			deHyphenWithoutCount();
		}
	}*/
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		DeHyphenizer dh = new DeHyphenizer("fnav5_corpus", "learnedstates_copy", "state", "count", "_");
		dh.deHyphen();
	}

}
