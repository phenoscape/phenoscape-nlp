package fna.parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Test{
	
	public Test(String com){
		try{
			runCommand(com);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	protected void runCommand(String com) throws IOException,
			InterruptedException {
		long time = System.currentTimeMillis();

		Process p = Runtime.getRuntime().exec(com);
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(p
				.getInputStream()));
		
		BufferedReader errInput = new BufferedReader(new InputStreamReader(p
				.getErrorStream()));
		
		// read the output from the command
		String s = "";
		//int i = 0;
		while ((s = stdInput.readLine()) != null) {
			System.out.println(s + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
		}
		
		// read the errors from the command
		String e = "";
		while ((e = errInput.readLine()) != null) {
			// listener.info(String.valueOf(i), s);
			System.out.println(e + " at " + (System.currentTimeMillis() - time)
					/ 1000 + " seconds");
		}
	}
	
	public static void main(String[] args) {
		ArrayList<String> prepPhrases = new ArrayList<String>();
		prepPhrases.add("in relation to");
		Iterator<String> it = prepPhrases.iterator();
		String str = "{pectoral} <fins> in <relation> to {first} {pelvic}-<fin> <ray> when {depressed} {parallel} to <body> <axis>";
		while(it.hasNext()){
			String phrase = "\\{?\\<?"+it.next().trim().replaceAll(" ", "\\\\>?\\\\}? \\\\{?\\\\<?")+"\\>?\\}?";
			Pattern p = Pattern.compile("(.*?)(\\b"+phrase+"\\b)(.*)");
			Matcher m = p.matcher(str);
			while(m.matches()){
				str = m.group(1)+m.group(2).replaceAll("[<{}>]", "").replaceAll("\\s+", "_")+"_PPP"+m.group(3);
				m = p.matcher(str);
			}					
		}
		System.out.println(str);
		//String dbcmd = "perl ..\\phenoscape-Unsupervised\\test.pl";
		//Test t = new Test(dbcmd);
	}
}