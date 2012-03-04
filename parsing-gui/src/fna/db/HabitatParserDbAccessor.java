
package fna.db;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import fna.parsing.ApplicationUtilities;

public class HabitatParserDbAccessor {

	/**
	 * @param args
	 */
    private static final Logger LOGGER = Logger.getLogger(VolumeTransformerDbAccess.class);
    private static String url = ApplicationUtilities.getProperty("database.url");
    private static Connection conn = null;
    //private static String prefix = "fna";
    private String prefix = null;
    
	static {
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Couldn't find Class in HabitatParserDbAccessor" + e);
			e.printStackTrace();
		}
	}
	
	public HabitatParserDbAccessor(String prefix){
		this.prefix = prefix;
	}
	
	public void createTable(){
		try{
			conn = DriverManager.getConnection(url);
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists "+this.prefix+"_habitat");
			stmt.execute("create table if not exists "+this.prefix+"_habitat (source varchar(50) not null, habitat_string text, habitat_values varchar(500), primary key (source))");
			//stmt.execute("delete from "+this.prefix+"_habitat");
		}catch(Exception e){
			LOGGER.error("HabitatParserDbAccessor error:" + e);
			e.printStackTrace();
		}		
	}
	
	public void populateTable(File datasource) {
		File[] files = datasource.listFiles();
		for(int i = 0; i< files.length; i++){
			String text = read(files[i]);
			String fname = files[i].getName();
			if(text.trim().length()>0){
				insertRecord(fname, text);
			}
		}				
	}

	
	
	private void insertRecord(String fname, String text) {
		try{
			Statement stmt = conn.createStatement();
			stmt.execute("insert into "+this.prefix+"_habitat(source, habitat_string) values ('"+fname+"','"+text+"')");			
		}catch(Exception e){
			LOGGER.error("HabitatParserDbAccessor insert record error:" + e);
			e.printStackTrace();
		}
		
	}
	
	public void updateRecord(String src, String newtext, String colume, String where) {
		String query = "update "+this.prefix+"_habitat set "+colume+" ='"+newtext+"'";
		if(where.trim().length() > 0){
			query += " where "+where;
		}
		try{
			Statement stmt = conn.createStatement();
			stmt.execute(query);			
		}catch(Exception e){
			LOGGER.error("HabitatParserDbAccessor insert record error:" + e);
			e.printStackTrace();
		}
		
	}

	public ArrayList<String> selectRecords(String select, String where, String groupby, String orderby) {
		ArrayList<String> results= new ArrayList<String>();
		String query = "select  "+select+" from "+this.prefix+"_habitat ";
		if(where.length()>0){
			query += " where "+where;
		}
		if(groupby.length()>0){
			query += " group by "+groupby;
		}
		if(orderby.length()>0){
			query += " order by "+orderby;
		}
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(query);
			while(rs.next()){
				String[] temp = select.split(",");
				String r = "";
				for(int i = 1; i<=temp.length; i++){
					r += rs.getString(i)+"@";
				}
				r = r.replaceFirst("\\s*@$", "");
				results.add(r);
			}
		}catch(Exception e){
			LOGGER.error("HabitatParserDbAccessor insert record error:" + e);
			e.printStackTrace();
		}
		return results;
	}
	private String read(File file) {
		StringBuffer sb = new StringBuffer();
		try{
		    FileInputStream fstream = new FileInputStream(file);
		    DataInputStream in = new DataInputStream(fstream);
		    BufferedReader br = new BufferedReader(new InputStreamReader(in));
		    String line;
		    while ((line = br.readLine()) != null)   {
		    	line = line.replaceAll(System.getProperty("line.separator"), " ");
		    	sb.append(line);
		    }
		    in.close();
		}catch (Exception e){//Catch exception if any
			LOGGER.error("HabitatParserDbAccessor read file exception:" + e);
			e.printStackTrace();
		}
		return sb.toString().replaceAll("\\s+", " ");
	}






	public static void main(String[] args) throws Exception{
		// TODO Auto-generated method stub
		System.out.println(DriverManager.getConnection(url));

	}

}
