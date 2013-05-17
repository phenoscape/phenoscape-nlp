/**
 * 
 */
package fna.parsing;

import java.io.*;

/**
 * @author Hong Updates
 * this program put resultant document.xml files from WordDocSegmenter.java
 * into C:\DOCCreation\FNA19package\word\, package/zip the parts to generate n.docx files
 * the files containing keys are named n_key.docx. 
 */
public class WordDocGenerator {
	String sourcedir = "C:/DEMO/FNA-v19-excerpt/target/"+ApplicationUtilities.getProperty("EXTRACTED");
	String packagedir = "C:/DOCCreation/FNA19package";
	String worddir = "word";
	String targetdir = "C:/DEMO/FNA-v19-excerpt/target/"+ApplicationUtilities.getProperty("EXTRACTEDWORD");
	
	public WordDocGenerator(){
		File t = new File(this.targetdir);
		if(!t.exists()){
			t.mkdir();
		}
	}
	
	public void generateFromDir(){
		File[] sfiles = (new File(sourcedir)).listFiles();
		for(int i = 0; i<sfiles.length; i++){
			String filename = sfiles[i].getName().replace(".xml", "");
			String newpdir = targetdir+"/"+filename;
			File pkg = new File(newpdir);
			if(!pkg.exists()){
				pkg.mkdir();
			}
			copy("xcopy", packagedir, newpdir);//make a new package
			copy("copy", sfiles[i].getAbsolutePath(), newpdir+"/"+worddir+"/document.xml");//place xml file in the new package
			//zip newpdir
			File[] files = pkg.listFiles();
			OpenXMLZipFile.CreateZipFile(newpdir+".docx", files);	
			pkg.delete();
		}
	}
	
	public static void copy(String copy, String from, String to){
		try { 
			Runtime rt=Runtime.getRuntime(); 
			String cmd = "cmd /c "+copy+" \""+from+"\" \""+to+"\"";
			if(copy.startsWith("x")){
				cmd += " /e";
			}			
			Process p=rt.exec(cmd); 
			p.waitFor(); 
			System.out.println("process exit with value "+p.exitValue()); 
		} catch(Exception e) { 
			System.out.println(e.getMessage()); 
		} 
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		WordDocGenerator wdg = new WordDocGenerator();
		wdg.generateFromDir();

	}

}
