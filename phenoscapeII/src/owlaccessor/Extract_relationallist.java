package owlaccessor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Scanner;

public class Extract_relationallist {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	private static Connection con;
	private String url;
	private static String dburl = "jdbc:mysql://localhost:3306/";
	private static String uname = "root";
	private static String upw = "forda444";
	private static String dbName = "biocreative2012";
	
	public static void main(String[] args) throws FileNotFoundException, ClassNotFoundException, SQLException {
		// TODO Auto-generated method stub
		File f = new File("C:\\Users\\Murali\\Desktop\\RA\\ontologies\\phenex_relations.obo.txt");
			Class.forName("com.mysql.jdbc.Driver");

				con = DriverManager.getConnection(dburl + dbName, uname, upw);

				// Drop table if exists
				Statement stmt0 = con.createStatement();
				stmt0.executeUpdate("DROP TABLE IF EXISTS biocreative2012.restrictedrelations");
				stmt0.executeUpdate("create table biocreative2012.restrictedrelations(id varchar(100),label varchar(100))");

		Scanner sc = new Scanner(f);
		String id,name;
		
		while(sc.hasNext())
		{
			id = sc.nextLine();
			
			if(id.matches("((id:)|(name:)).*"))
					{
				id= id.replaceAll("(id:)", "");
				name = sc.nextLine().replaceAll("(name:)", "");
				stmt0.executeUpdate("Insert into restrictedrelations(id,label) values(\"" + id.trim()+"\",\""+name.trim()+"\")");
				//System.out.println("id== "+id.trim()+" name== "+name.trim());
					}
		}
con.close();
sc.close();

	}

}
