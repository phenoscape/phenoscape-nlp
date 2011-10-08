package fna.parsing;

import java.io.File;

public class Utilities {

	public static void resetFolder(File folder, String subfolder) {
		File d = new File(folder, subfolder);
		if(!d.exists()){
			d.mkdir();
		}else{ //empty folder
			Utilities.emptyFolder(d);
		}
	}
	
	public static void emptyFolder(File f){
			File[] fs = f.listFiles();
			for(int i =0; i<fs.length; i++){
				fs[i].delete();
			}
	}
}
