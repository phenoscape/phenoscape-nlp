/**
 * 
 */
package testcases;

import java.io.File;
import java.io.IOException;
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
		for(File t: tests){
			String tid = t.getName().substring(t.getName().lastIndexOf("_"));
			String fname = prefix+tid;
			try {
				//test->tested
				Files.copy(Paths.get(t.getAbsolutePath()), Paths.get(tested, t.getName()), StandardCopyOption.REPLACE_EXISTING);
				//final ->test
				Files.copy(Paths.get(finall, fname), Paths.get(test, t.getName()),  StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
