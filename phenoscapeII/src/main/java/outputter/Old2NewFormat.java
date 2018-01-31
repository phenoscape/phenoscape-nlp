/**
 * 
 */
package outputter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;



/**
 * @author Hong Cui
 * convert EQ output format from 2012 to 2013 version
 *
 */
public class Old2NewFormat {

	/**
	 * 
	 */
	public static void main(String[] args) {
		//old format:
		//each row reps one EQ, one state could have N rows/EQs.
		//entityid : 0-1 value
		//entitylocatorid: comma-separated a list of values, ordered in the part,whole order.
		//qualityid: 0-1 value
		//qnparentid: 0-1 value
		//qualitymodifier: comma-separated a list of values, ordered in the part. whole order.
		
		//one value is a semi-colon separated list of possible matches.
		String table = "bc2012rerun_xml2eq_result_best_sent"; 

		String out = "bc2012rerun_xml2eq_result_best_sent_newformat";
		Statement stmt = null;
		ResultSet rs = null;
		Connection conn = null;
		int count = 0;
		try{
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost/biocreative2012?user=biocreative&password=biocreative");
			stmt = conn.createStatement();
			rs = stmt.executeQuery("select * from "+table);
			while(rs.next()){
				String source = rs.getString("source").trim();
				String characterID= rs.getString("characterID").trim();
				String stateID = rs.getString("stateID").trim();
				String description = rs.getString("description").trim();
				String entity = rs.getString("entity").trim();
				String entitylabel= rs.getString("entitylabel").trim();
				String entityid= rs.getString("entityid").trim(); 
				String quality = rs.getString("quality").trim();
				String qualitylabel = rs.getString("qualitylabel").trim();
				String qualityid = rs.getString("qualityid").trim();
				String qualitynegated= rs.getString("qualitynegated").trim();
				String qualitynegatedlabel = rs.getString("qualitynegatedlabel").trim();
				String qnparentlabel= rs.getString("qnparentlabel").trim();
				String qnparentid= rs.getString("qnparentid").trim();
				String qualitymodifier = rs.getString("qualitymodifier").trim();
				String qualitymodifierlabel = rs.getString("qualitymodifierlabel").trim();
				String qualitymodifierid= rs.getString("qualitymodifierid").trim();
				String entitylocator = rs.getString("entitylocator").trim();
				String entitylocatorlabel = rs.getString("entitylocatorlabel").trim();
				String entitylocatorid = rs.getString("entitylocatorid").trim(); //part,whole
							
				//combine entity and entity locator => entityid
				//A and (BFO:0000050 some B)
				
				ArrayList<String> entityproposals = new ArrayList<String>();
				if(!entityid.isEmpty()){
					entityproposals.addAll(Arrays.asList(entityid.replaceAll("(^;|;$)+", "").replaceAll(";+", ";").split("\\s*;\\s*")));
				}
				
				ArrayList<ArrayList<String>> elproposals = generateProposals(entitylocatorid);
				
				//combine entityProposals and elproposals
				
				int size = elproposals.size();
				if(entityproposals.size()>0 && elproposals.size()>0){
					ArrayList<ArrayList<String>> finaleproposals = new ArrayList<ArrayList<String>>();
					for(String ep: entityproposals){
						for(int i = 0; i < size; i++){
							ArrayList<String> temp = (ArrayList<String>) elproposals.get(i).clone();
							temp.add(0, ep);
							finaleproposals.add(temp);
						}
					}
					elproposals = finaleproposals;
				}else if(elproposals.size()==0 && entityproposals.size()>0){
					for(String ep: entityproposals){
						ArrayList<String> anEp = new ArrayList<String>();
						anEp.add(ep);
						elproposals.add(anEp);
					}
				}
					
				//postcompose final entityids
				entityid = "";
				for(ArrayList<String> elp: elproposals){
					String temp = "";
					if(elp.size()>1){
						temp = elp.get(elp.size()-1);
						for(int i = elp.size()-2; i>=0; i--){
							temp = elp.get(i)+ " and (BFO:0000050 some "+temp+")";
						}
					}else{
						temp = elp.get(0);
					}
					if(!temp.isEmpty()) entityid += temp+" Score:[1.0]@,";
				}
				
				entityid = entityid.replaceFirst("@,$", "");
				
				//form negated quality
				if(!qnparentid.isEmpty() && !qualityid.isEmpty()){
					String[] qnpproposals = qnparentid.replaceAll("(^;|;$)+", "").replaceAll(";+", ";").split("\\s*;\\s*");
					String[] qproposals = qualityid.replaceAll("(^;|;$)+", "").replaceAll(";+", ";").split("\\s*;\\s*");
					qualityid = "";
					for(String qnpproposal: qnpproposals){
						for(String qproposal: qproposals){
							String temp = qnpproposal +" and (PHENOSCAPE:complement_of some "+qproposal+")";
							qualityid += temp+" Score:[1.0]@,";;
						}
					}
				}else if(!qualityid.isEmpty()){
					String[] qproposals = qualityid.replaceAll("(^;|;$)+", "").replaceAll(";+", ";").split("\\s*;\\s*");
					qualityid = "";
					for(String qproposal: qproposals){
						String temp = qproposal;
						qualityid += temp+" Score:[1.0]@,";;
					}
				}
				qualityid = qualityid.replaceFirst("@,$", "");
				
				//form related entity
				String relatedEntityId = "";
				if(!qualitymodifierid.isEmpty()){
					ArrayList<ArrayList<String>> reproposals = generateProposals(qualitymodifierid);
					for(ArrayList<String> resl: reproposals){
						String temp = "";
						if(resl.size()>1){
							temp = resl.get(resl.size()-1);
							for(int i = resl.size()-2; i>=0; i--){
								temp = resl.get(i)+ " and (BFO:0000050 some "+temp+")";
							}
						}else{
							temp = resl.get(0);
						}
						if(!temp.isEmpty()) relatedEntityId += temp+" Score:[1.0]@,";
					}
				}
				
				relatedEntityId = relatedEntityId.replaceFirst("@,$", "");
				
				//add the record to out
				String sql = "insert into "+out+" (characterid, stateid, statelabel, entitylabel, entityid, qualitylabel, qualityid, relatedentitylabel, relatedentityid) "
						+ "values(?,?,?,?,?,?,?,?,?)";
				PreparedStatement insert = conn.prepareStatement(sql);
				insert.setString(1, characterID);
				insert.setString(2, stateID);
				insert.setString(3, description);
				insert.setString(4, entitylabel);
				insert.setString(5, entityid);
				insert.setString(6, qualitylabel);
				insert.setString(7, qualityid);
				insert.setString(8, qualitymodifierlabel);
				insert.setString(9, relatedEntityId);
				insert.execute();
				insert.close();
				count++;
				System.out.println("inserted "+count+" records");
			}
			
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			if(rs!=null){
				try {
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(stmt!=null){
				try {
					stmt.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(conn!=null){
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private static ArrayList<ArrayList<String>> generateProposals(
			String entitylocatorid) {
		ArrayList<ArrayList<String>> elproposals = new ArrayList<ArrayList<String>>();
		if(!entitylocatorid.isEmpty()){
			String [] els = entitylocatorid.replaceAll("(^,|,$)+", "").replaceAll(",+",",").split("\\s*,\\s*");
			for(String el: els){
				el = el.replaceAll("(^;|;$)", "");
				if(el.contains(";")){
					String[] elps = el.split("\\s*;\\s*");
					//clone elps.length sets of proposals
					//add individual proposal to appropriate sets:
					if(elproposals.size()==0) elproposals.add(new ArrayList<String>());
					ArrayList<ArrayList<String>> allproposals = new ArrayList<ArrayList<String>>();
					for(ArrayList<String> elproposal: elproposals){
						for(int i = 0; i < elps.length; i++){
							@SuppressWarnings("unchecked")
							ArrayList<String> clone = (ArrayList<String>) elproposal.clone();
							clone.add(elps[i]);
							allproposals.add(clone);
						}
					}																					
					elproposals = allproposals;
				}else{
					if(elproposals.size()==0) elproposals.add(new ArrayList<String>());
					for(ArrayList<String> elproposal: elproposals){
						elproposal.add(el);
					}
				}
			}	
		}
		return elproposals;
	}




}
