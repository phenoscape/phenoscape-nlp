package testing_lab.lab1;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import fna.parsing.ApplicationUtilities;

public class Test {

	/**
	 * @param args
	 */
	public String toString() {
		return "Hola!";
	}
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub

		//log4j.appender.ROOT.File=D:\\SMART RA\\Work Folders\\Source\\Logs\\markup.log
/*		String path = System.getProperty("user.dir") + "\\src\\log4j.properties";
		try{
		    // Create file 
		    FileWriter fstream = new FileWriter(path ,true);
		        BufferedWriter out = new BufferedWriter(fstream);
		        out.newLine();
		    out.write("Hello Java");
		    //Close the output stream
		    out.close();
		    }catch (Exception e){//Catch exception if any
		      System.err.println("Error: " + e.getMessage());
		    }*/
		String s = "D:\\SMART RA\\Workspace\\parsing-gui\\Logs\\markup.log";
		s = s.replace("\\", "\\\\");
		System.out.println(s);
		
		setLogFilePath();

}

	private static void setLogFilePath() throws Exception {
		
		FileInputStream fstream = null;
		FileWriter fwriter = null;
		BufferedWriter out = null;
		
		try {
			String logProperties = System.getProperty("user.dir")+"\\src\\log4j.properties";
			fstream = new FileInputStream(logProperties);
			Properties properties = new Properties();
			properties.load(fstream);
			String logFilePath = properties.getProperty("log4j.appender.ROOT.File");
			/* Check if log path is already set*/
			if (logFilePath == null) {
				fwriter = new FileWriter(logProperties ,true);
		        out = new BufferedWriter(fwriter);
		        out.newLine();
		        logFilePath = ApplicationUtilities.getProperty("LOG.APPENDER") + System.getProperty("user.dir") 
		        	+ ApplicationUtilities.getProperty("LOG");
		        System.out.println(logFilePath.trim());
		        logFilePath = logFilePath.trim();
		        logFilePath = logFilePath.replaceAll("\\\\", "\\\\\\\\");
		        out.write(logFilePath);
			}
			
			
		} catch(Exception exe) {
			exe.printStackTrace();
		} finally {
			if (out != null) {
				out.close();
			}
			
			if (fwriter != null){
				fwriter.close();
			}
			
			if (fstream != null){
				fstream.close();
			}
		}		
	}

	//}
	
/*	public static String longestCommonSubstring(String str1, String str2)
	{
		String sequence = "";
		if (str1 == null || str1.equals("") || str2 == null || str2.equals("")) {
			return "";
		}
			
	 
		int[][]num = new int[str1.length()][str2.length()];
		int maxlen = 0;
		int lastSubsBegin = 0;
		StringBuilder sequenceBuilder = new StringBuilder();
	 
		for (int i = 0; i < str1.length(); i++)
		{
			for (int j = 0; j < str2.length(); j++)
			{
				if (str1.charAt(i) != str2.charAt(j))
					num [i][j] = 0;
				else
				{
					if ((i == 0) || (j == 0))
						num[i][j] = 1;
					else
						num[i][j] = 1 + num[i - 1][j - 1];
	 
					if (num[i][j] > maxlen)
					{
						maxlen = num[i][j];
						int thisSubsBegin = i - num[i][j] + 1;
						if (lastSubsBegin == thisSubsBegin)
						{//if the current LCS is the same as the last time this block ran
							sequenceBuilder.append(str1.charAt(i));
						}
						else //this block resets the string builder if a different LCS is found
						{
							lastSubsBegin = thisSubsBegin;
							sequenceBuilder.delete(0, sequenceBuilder.length());//clear it
							sequenceBuilder.append(str1.substring(lastSubsBegin, (i + 1) - lastSubsBegin));
						}
					}
				}
			}
		}
		sequence = sequenceBuilder.toString();
		return sequence;
	}*/

/*	public int longestSubstr(String str_, String toCompare_) 
	{
		StringBuilder sequence = new StringBuilder();
	  if (str_.isEmpty() || toCompare_.isEmpty())
	    return 0;
	 
	  int[][] compareTable = new int[str_.length()][toCompare_.length()];
	  int maxLen = 0;
		int lastSubsBegin = 0;
	 
	  for (int m = 0; m < str_.length(); m++) 
	  {
	    for (int n = 0; n < toCompare_.length(); n++) 
	    {
	      compareTable[m][n] = (str_.charAt(m) != toCompare_.charAt(n)) ? 0
	          : (((m == 0) || (n == 0)) ? 1
	              : compareTable[m - 1][n - 1] + 1);
	      maxLen = (compareTable[m][n] > maxLen) ? compareTable[m][n]
	          : maxLen;
	    }
	  }
	  return maxLen;
	}*/
	
	public static String longestCommonSubstring(String first, String second) {
		 
		 String tmp = "";
		 String max = "";
						
		 for (int i=0; i < first.length(); i++){
			 for (int j=0; j < second.length(); j++){
				 for (int k=1; (k+i) <= first.length() && (k+j) <= second.length(); k++){
										 
					 if (first.substring(i, k+i).equals(second.substring(j, k+j))){
						 tmp = first.substring(i,k+i);
					 }
					 else{
						 if (tmp.length() > max.length())
							 max=tmp;
						 tmp="";
					 }
				 }
					 if (tmp.length() > max.length())
							 max=tmp;
					 tmp="";
			 }
		 }
				
		 return max;        
			    
	}



}
