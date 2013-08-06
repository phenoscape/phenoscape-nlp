/**
 * 
 */
package preprocessing;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;






import outputter.ApplicationUtilities;

/**
 * @author Hong Cui
 * 
 * Add character and state ids to Wasila's goldstandard
 * so it can be used to evaluate machine output
 */
public class AddIds2PatternsGoldstandard {

	String goldtablename;
	String machinetablename;
	Connection conn;
	
	/**
	 * 
	 */
	public AddIds2PatternsGoldstandard(String goldtablename, String machinetablename) {
		this.goldtablename = goldtablename;
		this.machinetablename = machinetablename;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void fillInIDs(){
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select characterlabel, statelabel from "+this.goldtablename);
			int count = 0;
			while(rs.next()){
				String[] ids = getIds(rs.getString("characterlabel"), rs.getString("statelabel"));//0: charaid, 1:stateid
				if(ids[0]!=null && ids[1]!=null && ids[0].length()>0 && ids[1].length()>0) System.out.println("found ids for record #"+ count);
				else 
					System.out.println("not found ids for record #"+ count + " "+ rs.getString("characterlabel") +"]["+ rs.getString("statelabel"));
				count++;
				//Statement update = conn.createStatement();
				//update.execute("update "+this.goldtablename+" set characterid='"+ids[0]+"' stateid='"+ids[1]+"'"
				//		+ " where characterlabel='"+rs.getString("characterlabel")+"' statelabel='"+rs.getString("statelabel")+"'");
			}	
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	private String[] getIds(String characterlabel, String statelabel) {
		characterlabel = characterlabel.replaceAll("&", "and");
		statelabel = statelabel.replaceAll("&", "and");
		String[] ids = new String[2];
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select characterid, stateid, characterlabel, statelabel from "+this.machinetablename);
			while(rs.next()){
				String clabel = rs.getString("characterlabel");
				String slabel = rs.getString("statelabel");
				//System.out.println("clabel:"+clabel);
				//System.out.println("slabel:"+slabel);
				if(clabel.replaceAll("\\W", "").trim().compareToIgnoreCase(characterlabel.replaceAll("\\W", "").trim())==0 && 
						slabel.replaceAll("\\W", "").trim().compareToIgnoreCase(statelabel.replaceAll("\\W", "").trim())==0){
					ids[0] = rs.getString("characterid");
					ids[1] = rs.getString("stateid");
					break;
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return ids;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String gold = "goldstandard";
		String machine = "pattern_xml2eq";
		AddIds2PatternsGoldstandard apg = new AddIds2PatternsGoldstandard(gold, machine);
		apg.fillInIDs();

	}

}
