/* $Id: Test.java 827 2011-06-05 03:36:57Z hong1.cui $ */
/**
 * 
 */
package fna.charactermarkup;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * @author hongcui
 *
 */
@SuppressWarnings({ "unused" })
public class Test {
	Connection conn = null;
	ArrayList<String> chunkedtokens = new ArrayList<String>();
	/**
	 * 
	 */
	public Test() {
		//as long as its bony portion 
		/*this.chunkedtokens.add("{as-long-as}");
		this.chunkedtokens.add("its");
		this.chunkedtokens.add("{bony}");
		this.chunkedtokens.add("(portion)");*/

		//as deep as broad
		/*this.chunkedtokens.add("{as-deep-as}");
		this.chunkedtokens.add("{broad}");*/


		//ad short as width of the tip 
		this.chunkedtokens.add("{as-short-as}");
		this.chunkedtokens.add("{width}");
		this.chunkedtokens.add("r[p[of] the (tip)]");

	}

	private void normalizeAsAsThan() {
		for(int i = 0; i< this.chunkedtokens.size(); i++){	
			String token = this.chunkedtokens.get(i);
			String chunk = token+" ";
			boolean success = false;
			if(token.matches("\\{?as-("+ChunkedSentence.asasthan+")-as\\}?")){//{as-long-as}: treat these as ChunkTHAN
				//looking for the 2nd part
				int j = 0; String t = "";
				for(j = i+1; j<this.chunkedtokens.size(); j++){
					t = this.chunkedtokens.get(j);
					if(t.length()!=0) break;
				}
				if(t.matches("\\{?("+ChunkedSentence.asasthan+")\\}?")){ //case 1
					chunk +=t+" ";
					success = true;
				}
				else if(t.matches("\\{?(height|width|length|depth|thickness)\\}?")){ //case 3
					chunk +=t+" ";
					for(int k = j+1; k < this.chunkedtokens.size(); k++){
						if(this.chunkedtokens.get(k).length()==0) continue;
						if(this.chunkedtokens.get(k).startsWith("r[p[of")){
							chunk += this.chunkedtokens.get(k)+" ";
							j = k;
							success = true;
							break;
						}
					}
				}
				if(!success){
					//case 2
					while(!t.startsWith("(") && !t.equals(",")){//found bony in {bony} (portion)
						chunk +=t+" ";
						if(j < this.chunkedtokens.size()-1) t = this.chunkedtokens.get(++j);
						else break;
						success = true;
					}
					while((t.length()==0 || t.startsWith("("))){ //found (portion)
						chunk +=t+" ";
						if(j < this.chunkedtokens.size()-1) t = this.chunkedtokens.get(++j);
						else break;
					}
				}

				//form n[chunk]
				if(success){
					this.chunkedtokens.set(i, "n["+chunk.trim()+"]");
					for(int k=i+1; k<=j; k++){
						this.chunkedtokens.set(k, "");
					}
				}
			}			
		}		
	}
	public void constraint(){
		String[] organ = new String[]{"long", "cauline", "leaf", "abaxial", "surface", "trichomode"};
		Hashtable<String, String> mapping = new Hashtable<String, String>();
		mapping.put("cauline", "type");
		mapping.put("leaf", "parent_organ");
		mapping.put("long", "null");
		mapping.put("surface", "parent_organ");
		mapping.put("abaxial", "type");
		mapping.put("trichomode", "type");
		int j = 5;
		boolean terminate =false;
		for(;j >=0; j--){
			if(terminate) break;
			String w = organ[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
			String type = "null";
			if(w.startsWith("(")) type="parent_organ";
			else type = mapping.get(w);
			if(!type.equals("null")){
				organ[j] = "";
				if(type.equals("type")){
					System.out.println("constraint_"+type+": "+w.replaceAll("(\\(|\\))", "").trim()); //may not have.						
				}else{//"parent_organ": collect all until a null constraint is found
					String constraint = w;
					j--;
					for(; j>=0; j--){
						w = organ[j].replaceAll("(\\w+\\[|\\]|\\{|\\})", "");
						if(w.startsWith("(")) type="parent_organ";
						else type = mapping.get(w);;
						if(!type.equals("null")){
							constraint = w+" "+constraint;
							organ[j] = "";
						}
						else{
							System.out.println("constraint_parent_organ: "+constraint.replaceAll("(\\(|\\))", "").trim()); //may not have.
							terminate = true;
							break;
						}
					}
				}
			}else{
				break;
			}
		}
		j++;
		System.out.println(j);
	}
	public void test1(){

		String tsent = "<a b> a b <a b c> {a b} <a> <b>";
		Pattern p = Pattern.compile("(.*?<[^>]*) ([^<]*>.*)");//<floral cup> => <floral-cup>
		Matcher m = p.matcher(tsent);
		while(m.matches()){
			tsent = m.group(1)+"-"+m.group(2);
			m = p.matcher(tsent);
		}
		System.out.println(tsent);
	}

	private ArrayList<String> breakText(String text) {
		ArrayList<String> tokens = new ArrayList<String>();
		String[] words = text.split("\\s+");
		String t = "";
		int left = 0;
		for(int i = 0; i<words.length; i++){
			String w = words[i];
			if(w.indexOf("[")<0 && w.indexOf("]")<0 && left==0){
				if(!w.matches("\\b(this|have|that|may|be)\\b")){tokens.add(w);};
			}else{
				left += w.replaceAll("[^\\[]", "").length();
				left -= w.replaceAll("[^\\]]", "").length();
				t += w+" ";
				if(left==0){
					tokens.add(t.trim());
					t = "";
				}
			}
		}
		return tokens;
	}

	public String addSentmod(String subject, String sentmod) {
		String[] tokens = subject.split("\\s+");
		String substring = "";
		for(int i = 0; i<tokens.length; i++){
			if(!sentmod.matches(".*?\\b"+tokens[i].replaceAll("[{()}]", "")+"\\b.*")){
				substring +=tokens[i]+" ";
			}
		}
		substring = substring.trim();
		substring ="{"+sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ")+"} "+substring;
		return substring;
	}

	private static String combineModifiers(String element){
		Pattern ptn = Pattern.compile("(.*? )(modifier=\\S+)(['\"].*)");
		Matcher m = ptn.matcher(element);
		String result = "";
		String modifiers = "";
		while(m.matches()){
			result +=m.group(1).replaceFirst("^['\"]", "");
			modifiers += m.group(2).replaceAll("modifier=", "")+";";
			element = m.group(3);
			m = ptn.matcher(element);
		}
		result += element.replaceFirst("^['\"]", "");
		modifiers = "modifier=\""+modifiers.replaceAll("['\"]", "").replaceAll("\\W+$", "").trim()+"\"";
		result = result.replaceFirst("value", modifiers+" value").replaceAll("\\s+", " ");
		return result;
	}

	private String normalizeCountList(String str) {
		ArrayList<String> chunkedtokens = new ArrayList<String>(Arrays.asList(str.split("\\s+"))); 	
		String countp = "one|two|three|four|five|six|seven|eight|nine|ten|more|fewer";
		Pattern p = Pattern.compile("(\\b(?:"+countp+") (?:or|to) (?:"+countp+")\\b)");
		Matcher m = p.matcher(str);
		while(m.find()){
			int start = m.start(1);
			int end = m.end(1);
			String count = m.group(1);
			String rcount = "{count~list~"+count.replaceAll(" ","~")+"}";
			//synchronise this.chunkedtokens
			//split by single space to get an accurate count to elements that would be in chunkedtokens
			int index = (str.substring(0, start).trim()+" a").trim().split("\\s").length-1; //number of tokens before the count pattern
			chunkedtokens.set(index, rcount);
			int num = count.split("\\s+").length;
			for(int i = index+1; i < index+num; i++){
				chunkedtokens.set(i, "");
			}
			//resemble the str from chunkedtokens, counting all empty elements, so the str and chunkedtokens are in synch.
			str = "";
			for(String t: chunkedtokens){
				str +=t+" ";
			}
			m = p.matcher(str);
		}
		return str.replaceAll("\\s+", " ").trim();

	}

	private String normalizeSharedOrganObject(String object) {
		// TODO Auto-generated method stub
		if(object.matches(".*?\\b(and|or)\\b.*")){
			String norm = "";
			String[] segs = object.split("\\s+");
			String lastN = segs[segs.length-1].replaceAll("\\]+$", "").trim();
			for(int i= segs.length-1; i>=0; i--){
				norm = segs[i]+" "+norm;
				if(segs[i].matches("(,|and|or)") && !segs[i-1].contains("(")){
					norm = lastN+" "+norm;
				}
				if(segs[i].matches("(,|and|or)") && segs[i-1].contains("(")){
					lastN = segs[i-1].trim();
				}
			}
			return norm;
		}

		return object;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Test t = new Test();
		t.normalizeAsAsThan();
		/*String object = "o[the {frontal} , the (sphenotic) ({spine}) and the (flower)]";
		object = t.normalizeSharedOrganObject(object);
		System.out.println(object);*/
		//String str = "epural bones two or more present";
		//str = t.normalizeCountList(str);
		//System.out.println(str);


		//System.out.println(
		//t.addSentmod("{distal} (face)", "distal [basal leaf]")
		//t.combineModifiers("<character name=\"n\" modifier=\"a\" value=\"c\"/>")
		//);
		//String text = "that often do not overtop the heads";
		//t.breakText(text);
	}

}
