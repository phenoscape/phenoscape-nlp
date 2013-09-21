/**
 * 
 */
package testcases;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * @author hong cui
 *
 * refresh test cases in 'test' from 'final'
 * refresh tested case in 'tested' from 'test'
 * 
 */
public class TestCaseCopier {

	/**
	 * 
	 */
	public TestCaseCopier(String test, String finall, String tested) {
		File[] ffinal = new File(finall).listFiles();
		String prefix = ffinal[0].getName().substring(0, ffinal[0].getName().indexOf("_"));
		File[] tests = new File(test).listFiles();
		if(tests[0].getName().compareTo("test.txt") == 0){//list of file names
			try{
				FileInputStream fstream = new FileInputStream(tests[0]);
				DataInputStream in = new DataInputStream(fstream);
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String fname;
				while ((fname = br.readLine()) != null)   {
					Files.copy(Paths.get(finall, fname), Paths.get(test, fname),  StandardCopyOption.REPLACE_EXISTING);
				}
				in.close();
			}catch (Exception e){//Catch exception if any
				e.printStackTrace();
			}
		}else{
			for(File t: tests){
				String tid = t.getName().substring(t.getName().lastIndexOf("_"));
				String fname = prefix+tid;
				try {
					//test->tested
					Files.copy(Paths.get(t.getAbsolutePath()), Paths.get(tested, t.getName()), StandardCopyOption.REPLACE_EXISTING);
					//final ->test
					Files.copy(Paths.get(finall, fname), Paths.get(test, t.getName()),  StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String test = "C:/Users/updates/CharaParserTest/EQ-patterns_FixedGloss/target/test";
		String finall = "C:/Users/updates/CharaParserTest/EQ-patterns_FixedGloss/target/final";
		String tested = "C:/Users/updates/CharaParserTest/EQ-patterns_FixedGloss/target/tested";
		TestCaseCopier tcc = new TestCaseCopier(test, finall, tested);



	}

}
