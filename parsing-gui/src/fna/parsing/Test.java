package fna.parsing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
		int i = 0;
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
		String dbcmd = "perl ..\\phenoscape-Unsupervised\\test.pl";
		Test t = new Test(dbcmd);
	}
}