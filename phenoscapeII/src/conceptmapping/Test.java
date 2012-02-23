/**
 * 
 */
package conceptmapping;

import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hong Updates
 *
 */


public class Test {
	Hashtable<String, String> entityhash = new Hashtable<String, String>();
	
	public Test(){
		
	}
	
	private String turnRoman2Number(String word) {
		int total = 0;
		if(word.endsWith("iv")){
			total += 4;
			word = word.replaceFirst("iv$", "");
		}
		if(word.endsWith("ix")){
			total += 9;
			word = word.replaceFirst("ix$", "");
		}
		int length = word.length();
		for(int i = 0; i < length; i++){
			if(word.charAt(i)=='i') total += 1;
			if(word.charAt(i)=='v') total += 5;
			if(word.charAt(i)=='x') total += 10;
		}		
		return total+"";
	}
	
	/**
	 * fifth => 5
	 * @param word
	 * @return
	 */
	private String turnPosition2Number(String word) {
		if(word.compareTo("first")==0) return "1";
		if(word.compareTo("second")==0) return "2";
		if(word.compareTo("third")==0) return "3";
		if(word.compareTo("forth")==0) return "4";
		if(word.compareTo("fifth")==0) return "5";
		if(word.compareTo("sixth")==0) return "6";
		if(word.compareTo("seventh")==0) return "7";
		if(word.compareTo("eighth")==0) return "8";
		if(word.compareTo("ninth")==0) return "9";
		if(word.compareTo("tenth")==0) return "10";
		return null;
	}
	
	/**
	 * fifth abc => abc 5
	 * abc_1 => abc 1
	 * @param entitylist: entity1, entity2
	 * @return
	 */
	private String transform(String entitylist) {
		String transformed = entityhash.get(entitylist);
		if(transformed != null) return transformed;
		
		transformed = "";
		if(entitylist.matches(".*?(_[\\divx]+|first|second|third|forth|fifth|sixth|seventh|eighth|ninth|tenth).*")){
			String[] entities = entitylist.split("(?<!_),(?!_)");
			for(String entity : entities){
				//case one
				entity = entity.trim();
				if(entity.matches(".*?\\b(first|second|third|forth|fifth|sixth|seventh|eighth|ninth|tenth)\\b.*")){
					Pattern p = Pattern.compile("(first|second|third|forth|fifth|sixth|seventh|eighth|ninth|tenth)\\b(.*)");
					Matcher m = p.matcher(entity);
					if(m.matches()){
						String position = turnPosition2Number(m.group(1));
						entity = m.group(2)+" "+position;
						transformed += entity+",";
					}
					//transformed = transformed.replaceFirst(",$", "").trim();
					//entityhash.put(entitylist, transformed);
					//return transformed;
				}
				//case two
				if(entity.matches(".*?_[\\divx]+")){//abc_1, abc_1_and_2, abc_1_to_3
					String organ = entity.substring(0, entity.indexOf("_"));
					entity = reformatRomans(entity);
					entity = entity.replaceAll("_(?=\\d+)", " ").replaceAll("(?<=\\d)_", " "); //abc_1_and_3 => abc 1 and 3
					if(entity.indexOf(" and ")<0 && entity.indexOf(" to ")<0){ //single entity
						transformed += entity +",";
						//entityhash.put(entitylist, transformed);
						//return transformed;
					}else{// abc 1 and 2 
						if(entity.indexOf(" and ")>0){
							transformed += entity.replaceFirst(" and ", ","+organ+" ")+","; //abc 1,abc 2
							//entityhash.put(entitylist, transformed);
							//return transformed;
						}
						//abc 1 , 2 to 5 ; abc 2 to 5
						Pattern p = Pattern.compile("(.*?)(\\d+) to (\\d+)");
						Matcher m = p.matcher(entity);
						if(m.matches()){
							String part1 = m.group(1);
							int from = Integer.parseInt(m.group(2));
							int to = Integer.parseInt(m.group(3));
							String temp1 = "";
							for(int i = from; i <= to; i++){
								temp1 = temp1 + organ +" "+ i +",";
							}
							
							String temp = "";
							part1 = part1.replaceAll("\\D", "").trim();
							if(part1.length()>0){
								String[] nums = part1.split("\\s+");
								for(String n : nums){
									temp = temp + organ +" "+ n +","; 
								}
							}
							
							transformed = transformed+ temp + temp1;
							//transformed.replaceFirst(",$", "").trim();
							//entityhash.put(entitylist, transformed);
							//return transformed;
						}												
					}
				}
			}
		}else{
			transformed = entitylist;
			entityhash.put(entitylist, entitylist);
		}
		transformed = transformed.replaceFirst(",$", "").trim();
		entityhash.put(entitylist, transformed);
		return transformed;
	}
	
	/**
	 * abc_iv_and_v
	 * @param entity
	 * @return
	 */
	private String reformatRomans(String entity) {
		String[] parts = entity.split("_");
		String reformatted = "";
		for(String part : parts){
			if(part.matches("[ivx]+")) reformatted += this.turnRoman2Number(part)+"_";
			else reformatted += part+"_";
		}
		return reformatted.replaceFirst("_$", "");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Test t = new Test();
		System.out.println(t.transform("adb abc"));
	}

}
