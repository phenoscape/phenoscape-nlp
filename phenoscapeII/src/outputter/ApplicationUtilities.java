package outputter;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;


public class ApplicationUtilities {

	private static Properties properties = null;
	private static FileInputStream fstream = null;
	
	static {
		try {
			fstream = new FileInputStream(System.getProperty("user.dir")
					+System.getProperty("file.separator")+"application.properties");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	private ApplicationUtilities(){}
	
	public static void setLogFilePath() throws Exception {
		
		FileInputStream fstream = null;
		FileWriter fwriter = null;
		BufferedWriter out = null;
		
		try {
			String logProperties = System.getProperty("user.dir")+ getProperty("LOG.FILE.LOCATION");
			System.out.println("");
			fstream = new FileInputStream(logProperties);
			Properties properties = new Properties();
			properties.load(fstream);
			String logFilePath = properties.getProperty("log4j.appender.ROOT.File");
			/* Check if log path is already set*/
			if (logFilePath == null) {
				fwriter = new FileWriter(logProperties ,true);
		        out = new BufferedWriter(fwriter);
		        out.newLine();
		        logFilePath = getProperty("LOG.APPENDER") + System.getProperty("user.dir") 
		        	+ getProperty("LOG");
		        logFilePath = logFilePath.trim();
		        logFilePath = logFilePath.replaceAll("\\\\", "\\\\\\\\");
		        out.write(logFilePath);
				out.flush();
				System.exit(0);
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
	
	public static String getProperty(String key) {
	
		if(properties == null) {
			properties = new Properties();
			try {
				properties.load(fstream);
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
		return properties.getProperty(key);
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String type = getProperty("database.name");
		String message = getProperty("database.url");
	}

	

	
	

}
