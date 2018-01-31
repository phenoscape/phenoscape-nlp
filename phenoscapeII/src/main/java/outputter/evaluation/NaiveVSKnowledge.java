package outputter.evaluation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

import outputter.ApplicationUtilities;

/**
 * this class computes the complexity of a curator's EQs that are changed/unchanged 
 * from naive to knowledge round of curation.
 * 
 * @author Hong Cui
 *
 */
public class NaiveVSKnowledge {

	Connection conn;
	public NaiveVSKnowledge() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn  = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			Statement stmt = conn.createStatement();
			stmt.execute("drop table if exists naive_vs_knowledge");
			stmt.execute("create table if not exists naive_vs_knowledge (comparison varchar(50), isChanged int(1), stateid varchar(50), naiveEQlabel varchar(1000), knowledgeEQlabel varchar(1000),"
					+ " naiveComplexity int(5), knowledgeComplexity int(5), chNaivePrecision float(4,2), chNaiveRecall float(4,2), chKnowPrecision float(4,2), chKnowRecall float(4,2))");
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void collectResults(String naive, String knowledge, String changedcondition, String unchangedcondition){
		try {		
			Statement stmt = conn.createStatement();
			//evaluationrecords_states_naive_38484_knowledge_40717
			ResultSet rs = stmt.executeQuery("select stateid from evaluationrecords_states_"+naive+"_"+knowledge+"_sym" +" where "+unchangedcondition + " and "
					+ "stateid in (select stateid from evaluationrecords_states_"+knowledge+"_"+naive+"_sym" +" where "+unchangedcondition+")");
			while(rs.next()){
				String unchangedstate = rs.getString("stateid");
				Statement stmt1 = conn.createStatement();
				ResultSet naiveEQ = stmt1.executeQuery("select entityid, entitylabel, qualityid, qualitylabel, relatedentityid, relatedentitylabel from "+naive+" where stateid ='"+unchangedstate+"'");
				int naiveEQComplexity = 0;
				String naiveEQlabel = "";
				while(naiveEQ.next()){
				   naiveEQComplexity += (naiveEQ.getString("entityid")+naiveEQ.getString("qualityid")+naiveEQ.getString("relatedentityid")).replaceAll("[^:]", "").length();
				   naiveEQlabel += naiveEQ.getString("entitylabel")+" : "+naiveEQ.getString("qualitylabel")+" : "+naiveEQ.getString("relatedentitylabel") +" : ";
				}
				int knowEQComplexity = 0;
				String knowEQlabel = "";
				ResultSet knowEQ = stmt1.executeQuery("select entityid, entitylabel, qualityid, qualitylabel, relatedentityid, relatedentitylabel from "+knowledge+" where stateid ='"+unchangedstate+"'");
				while(knowEQ.next()){
					knowEQComplexity += (knowEQ.getString("entityid")+knowEQ.getString("qualityid")+knowEQ.getString("relatedentityid")).replaceAll("[^:]", "").length();
					knowEQlabel += knowEQ.getString("entitylabel")+" : "+knowEQ.getString("qualitylabel")+" : "+knowEQ.getString("relatedentitylabel") +":";		
				}
				ResultSet chNaiveScore = stmt1.executeQuery("select stateprecision, staterecall from evaluationrecords_states_xml2eq_"+naive.replace("naive_", "")+"_"+naive+"_sym"+" where stateid ='"+unchangedstate+"'");
				chNaiveScore.next();
				float chnaiveprecision = chNaiveScore.getFloat("stateprecision");
				float chnaiverecall = chNaiveScore.getFloat("staterecall");
				
				ResultSet chKnowScore = stmt1.executeQuery("select stateprecision, staterecall from evaluationrecords_states_xml2eq_"+knowledge.replace("knowledge_", "")+"_"+knowledge+"_sym"+" where stateid ='"+unchangedstate+"'");			
				chKnowScore.next();
				float chknowprecision = chKnowScore.getFloat("stateprecision");
				float chknowrecall = chKnowScore.getFloat("staterecall");
				
				stmt1.execute("insert into naive_vs_knowledge values('"+naive+"_"+knowledge+"', 0,'"+
				unchangedstate+"','"+
				naiveEQlabel+"','"+
				knowEQlabel+"',"+
				naiveEQComplexity+","+
				knowEQComplexity+","+
				chnaiveprecision+","+
				chnaiverecall+","+
				chknowprecision+","+
				chknowrecall+
				")");				
			}
			
			//evaluationrecords_states_naive_38484_knowledge_40717
			rs = stmt.executeQuery("select stateid from evaluationrecords_states_"+naive+"_"+knowledge+"_sym" +" where "+changedcondition + " union "
					+ "select stateid from evaluationrecords_states_"+knowledge+"_"+naive+"_sym" +" where "+changedcondition);
			while(rs.next()){
				String changedstate = rs.getString("stateid");
				Statement stmt1 = conn.createStatement();
				ResultSet naiveEQ = stmt1.executeQuery("select entityid, entitylabel, qualityid, qualitylabel, relatedentityid, relatedentitylabel from "+naive+" where stateid ='"+changedstate+"'");
				int naiveEQComplexity = 0;
				String naiveEQlabel = "";
				while(naiveEQ.next()){
				   naiveEQComplexity += (naiveEQ.getString("entityid")+naiveEQ.getString("qualityid")+naiveEQ.getString("relatedentityid")).replaceAll("[^:]", "").length();
				   naiveEQlabel += naiveEQ.getString("entitylabel")+" : "+naiveEQ.getString("qualitylabel")+" : "+naiveEQ.getString("relatedentitylabel") +" : ";
				}
				int knowEQComplexity = 0;
				String knowEQlabel = "";
				ResultSet knowEQ = stmt1.executeQuery("select entityid, entitylabel, qualityid, qualitylabel, relatedentityid, relatedentitylabel from "+knowledge+" where stateid ='"+changedstate+"'");
				while(knowEQ.next()){
					knowEQComplexity += (knowEQ.getString("entityid")+knowEQ.getString("qualityid")+knowEQ.getString("relatedentityid")).replaceAll("[^:]", "").length();
					knowEQlabel += knowEQ.getString("entitylabel")+" : "+knowEQ.getString("qualitylabel")+" : "+knowEQ.getString("relatedentitylabel") +":";		
				}
				ResultSet chNaiveScore = stmt1.executeQuery("select stateprecision, staterecall from evaluationrecords_states_xml2eq_"+naive.replace("naive_", "")+"_"+naive+"_sym"+" where stateid ='"+changedstate+"'");
				chNaiveScore.next();
				float chnaiveprecision = chNaiveScore.getFloat("stateprecision");
				float chnaiverecall = chNaiveScore.getFloat("staterecall");
				
				ResultSet chKnowScore = stmt1.executeQuery("select stateprecision, staterecall from evaluationrecords_states_xml2eq_"+knowledge.replace("knowledge_", "")+"_"+knowledge+"_sym"+" where stateid ='"+changedstate+"'");			
				float chknowprecision = 0f;
				float chknowrecall =0f;
				if(chKnowScore.next()){
					chknowprecision = chKnowScore.getFloat("stateprecision");
					chknowrecall = chKnowScore.getFloat("staterecall");
				}
				
				stmt1.execute("insert into naive_vs_knowledge values('"+naive+"_"+knowledge+"', 1,'"+
				changedstate+"','"+
				naiveEQlabel+"','"+
				knowEQlabel+"',"+
				naiveEQComplexity+","+
				knowEQComplexity+","+
				chnaiveprecision+","+
				chnaiverecall+","+
				chknowprecision+","+
				chknowrecall+
				")");				
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		String changed = "staterecall < 1 or stateprecision < 1";
		String unchanged = "staterecall = 1 and stateprecision = 1";
		NaiveVSKnowledge nvsk = new NaiveVSKnowledge();
		nvsk.collectResults("naive_38484", "knowledge_40717", changed, unchanged);
		nvsk.collectResults("naive_40674", "knowledge_40718", changed, unchanged);
		nvsk.collectResults("naive_40676", "knowledge_40716", changed, unchanged);		
	}	
	
}
