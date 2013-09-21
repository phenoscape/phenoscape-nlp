package fna.parsing;

import java.util.Hashtable;
import fna.parsing.character.Glossary;
//

public class DeHyphenizerCorrected extends DeHyphenizer {
	public DeHyphenizerCorrected(String database, String table, String column, String countcolumn, String hyphen, String glossarytable, Glossary glossary) {
		super(database, table, column, countcolumn, hyphen, glossarytable, glossary);
	}

	
	protected void collectTerms(String[] segs, String[] terms, int[][] matrix) {
		//rank rows by the distance between a 1 in upper matrix to the diagonal line
		int max = 0;
		Hashtable<String, String> rank = new Hashtable<String, String>();
		for(int i = 0; i < segs.length; i++){
			int distance = getDistance(matrix[i], i);
			if(distance > max){
				max = distance;
			}
			String list = (String)rank.get(distance+"");
			if(list == null){
				rank.put(distance+"", i+"");
			}else{
				rank.put(distance+"", list+" "+i+"");
			}
		}
		//collect terms
		String checked="-";
		for(int i = max; i >= 0; i--){
			String rows = (String)rank.get(i+"");
			if(rows!= null && i == 0){//term not see in learned or glossary, and not connectable to other terms
				String[] rowss = rows.split(" ");
				for(int j = 0; j < rowss.length; j++){
					int arow = Integer.parseInt(rowss[j]);
					if(checked.indexOf("-"+arow+"-")<0){
						terms[arow] = segs[arow];
					}
				}
			}else if(rows!=null){
				String[] rowss = rows.split(" ");
				for(int j = 0; j < rowss.length; j++){
					int arow = Integer.parseInt(rowss[j]);
					if(checked.indexOf("-"+arow+"-")<0){
						terms[arow] = formTerm(segs, arow, arow+i);
						checked += formString(arow, arow+i, "-");
					}
				}
			}
		}
	}

/**
 * the distance between a 1 in upper matrix to the diagonal line
 * @param is
 * @return
 */
	private int getDistance(int[] arow, int rownumber) {
		for(int i = arow.length-1; i>=0; i--){
			if(arow[i] == 1){
				return i - rownumber; 
			}
		}
		return 0;
	}
}
