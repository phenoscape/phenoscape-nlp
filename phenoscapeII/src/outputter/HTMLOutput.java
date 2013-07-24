package outputter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import owlaccessor.OWLAccessorImpl;

public class HTMLOutput {

	public static Connection conn;
	/**
	 * @param args
	 */
	
	public static void GetDBConnection()
	{
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost/biocreative2012?user=root&password=forda444");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void fetchFromDB(ArrayList<String> character_state, LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, ArrayList<EQHolder>>>> characters, String tablename, int numberofcurators) throws SQLException
	{
		ResultSet rs;
		String sql;
		Statement stmt = conn.createStatement();
		

		for(int i=0;i<character_state.size();i++)
		{
			LinkedHashMap<String,LinkedHashMap<String,ArrayList<EQHolder>>> stateeqs = new LinkedHashMap<String,LinkedHashMap<String,ArrayList<EQHolder>>>();
			LinkedHashMap<String,ArrayList<EQHolder>> individualstateeqs = new LinkedHashMap<String,ArrayList<EQHolder>>();

			String characterid = character_state.get(i).split("\\|\\|\\|")[0];
			String stateid = character_state.get(i).split("\\|\\|\\|")[1];
			String characteridlabel="";
			String stateidlabel="";
			
			sql = "select characterlabel,statelabel,entitylabel,entityid,qualitylabel,qualityid,relatedentitylabel,relatedentityid from  biocreative2012.swartz_after_xml2eq"+
				  " where characterid = '"+characterid +"' and stateid='"+stateid+"'";
			
			rs = stmt.executeQuery(sql);
			ArrayList<EQHolder> charparserstate = new ArrayList<EQHolder>();
			

			
			while(rs.next())
			{
				EQHolder eq = new EQHolder();
				
				characteridlabel=characterid+"|||"+rs.getString("characterlabel");
				stateidlabel=stateid+"|||"+rs.getString("statelabel");
				eq.setEntityid(rs.getString("entityid"));
				eq.setEntitylabel(rs.getString("entitylabel"));
				eq.setQualityid(rs.getString("qualityid"));
				eq.setQualitylabel(rs.getString("qualitylabel"));
				eq.setRelatedentityid(rs.getString("relatedentityid"));
				eq.setRelatedentitylabel(rs.getString("relatedentitylabel"));
				
				charparserstate.add(eq);
				
			}
			
			individualstateeqs.put("Charaparser", charparserstate);
			//curator1
			
			for(int count=0;count<numberofcurators;count++)
			{
				sql = "select entitylabel,entityid,qualitylabel,qualityid,relatedentitylabel,relatedentityid from biocreative2012."+tablename+(count+1)+
					  " where characterid = '"+characterid +"' and stateid='"+stateid+"'";
				
				rs = stmt.executeQuery(sql);
				ArrayList<EQHolder> curator1state = new ArrayList<EQHolder>();
				
				while(rs.next())
				{
					EQHolder eq = new EQHolder();
					
					eq.setEntityid(rs.getString("entityid"));
					eq.setEntitylabel(rs.getString("entitylabel"));
					eq.setQualityid(rs.getString("qualityid"));
					eq.setQualitylabel(rs.getString("qualitylabel"));
					eq.setRelatedentityid(rs.getString("relatedentityid"));
					eq.setRelatedentitylabel(rs.getString("relatedentitylabel"));
					
					curator1state.add(eq);
					
				}
				
				individualstateeqs.put(tablename+(count+1), curator1state);
				
			}
						stateeqs.put(stateidlabel, individualstateeqs);
						
						if(characters.get(characteridlabel)==null)
						{
							characters.put(characteridlabel, stateeqs);
						}
						else
						{
							LinkedHashMap<String,LinkedHashMap<String,ArrayList<EQHolder>>> character = characters.get(characteridlabel);
							character.putAll(stateeqs);
						}
						
		}
	}
	
	/*
	 * Print the ontology version in the output HTML
	 * 
	 * 
	 */
	public static void version(PrintWriter output,String version) throws IOException, OWLOntologyCreationException
	{
		OWLOntology temp=null;
		output.println("<CENTER> <TABLE border = 2> <TR> <TH COLSPAN =\"2\">");
		output.println("<B>Ontology Versions</B>");
		output.println("</TH></TR>");
		System.out.println();
		for(OWLAccessorImpl api: XML2EQ.ontoutil.OWLqualityOntoAPIs)
		{
			String S[]=api.getSource().split("[\\\\.]");
			output.println("<TR> <TD>");
			output.println((S[S.length-2].toUpperCase()));
			output.println("</TD>  <TD>");
			temp = api.getManager().getOntologies().iterator().next();
			output.println((temp.getOntologyID().getVersionIRI()));
			output.println("</TD> </TR>");
		}
		
		for(OWLAccessorImpl api1: XML2EQ.ontoutil.OWLentityOntoAPIs)
		{
			String S[]=api1.getSource().split("[\\\\.]");
			output.println("<TR> <TD>");
			output.println((S[S.length-2].toUpperCase()));
			output.println("</TD>  <TD>");
			Set<OWLOntology> ontologies = api1.getManager().getOntologies();
			for(OWLOntology ontology:ontologies)
			{
				if(ontology.toString().contains(S[S.length-2]))
				{
					temp = ontology;
					break;
				}
			}
			output.println(temp!=null?(temp.getOntologyID().getVersionIRI()):"");
			output.println("</TD> </TR>");
		}
		
		output.println("<TR><TD>charparser version</TD><TD>"+ version+"</TD></TR>");
		output.println("</TABLE>");
		output.println("</CENTER>");
		//output.close();
	}
	/*
	 * output's the EQ statement in HTML format
	 * 
	 * @param filename prefix will be the output filename
	 * @param tablename contains the name of the table which contains goldstandard or curators results
	 * @param numberofcurators holds the number of curators result compared with the charparser output
	 * 
	 * @output: Prints an HTML output with charparser and curators result for a characterid and stateid combination
	 */
	
	public static void outputHTML(String filename,String tablename, int numberofcurators)
	{

		String columns[] = {"CharacterID","CharacterLabel","StateID","StateLabel","Source of EQ","EntityID",
							"EntityLabel","QualityID","QualityLabel","RelatedEntityID","RelatedEntityLabel"};
		Statement stmt;
		ArrayList<String> character_state = new ArrayList<String>();
		try {
			PrintWriter output = new PrintWriter(new FileWriter(ApplicationUtilities.getProperty("output.dir")+filename+".html"));
			System.out.println(ApplicationUtilities.getProperty("output.dir")+filename+".html");
			output.println("<HTML>");
			output.println("<HEAD>");
			output.println("<TITLE>");
			output.println("Charparser Output");
			output.println(" </TITLE></HEAD>");
			output.println("<BODY>");
			output.println("<CENTER>");
			output.println("<H1>");
			output.println("Charparser Output");
			output.println("</H1>");
			if(XML2EQ.ontoutil!=null)
			{
				try {
					version(output,"1.0");
				} catch (OWLOntologyCreationException e) {
					e.printStackTrace();
				}
			}
			output.println("</CENTER><BR><BR>");

			output.println("<CENTER>");
			output.println("<TABLE BORDER=\"1\" width=\"200%\">");			
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"8%\"/>");
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"8%\"/>");
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"8%\"/>");
			output.println("<col width=\"15%\"/>");
			output.println("<col width=\"8%\"/>");
			output.println("<col width=\"15%\"/>");
			output.println("<col width=\"8%\"/>");
			output.println("<col width=\"15%\"/>");
			
			output.println("<TR>");

			for(int i=0;i<columns.length;i++)
			{
				output.println("<TH>");
				output.println(columns[i]);
				output.println("</TH>");
			}
			
			output.println("</TR>");
			
			GetDBConnection();
		
				stmt = conn.createStatement();
			
			String sql = "select distinct characterid, stateid from  biocreative2012.swartz_after_xml2eq";
			ResultSet rs = stmt.executeQuery(sql);
			
			while(rs.next())
			{
				String characterid = rs.getString("characterid");
				String stateid = rs.getString("stateid");
				if(characterid.length() > 0 && stateid.length() > 0){
					character_state.add(characterid+"|||"+stateid);
				}
			}
			LinkedHashMap<String,LinkedHashMap<String,LinkedHashMap<String,ArrayList<EQHolder>>>> characters = new LinkedHashMap<String,LinkedHashMap<String,LinkedHashMap<String,ArrayList<EQHolder>>>>();
			fetchFromDB(character_state,characters,tablename,numberofcurators);
			
			for(String characteridlabel:characters.keySet())//Iterates through all the characters
			{
				Set<String> states = characters.get(characteridlabel).keySet();//gets all the states
				for(String stateidlabel:states)//Iterate through all the states
				{
					LinkedHashMap<String,ArrayList<EQHolder>> statedescription =  characters.get(characteridlabel).get(stateidlabel);
					Set<String> sources = statedescription.keySet();
					
					for(String source:sources)//Iterate through each sources
					{
						ArrayList<EQHolder> eqs = statedescription.get(source);
						
						String entityid="";
						String entitylabel="";
						String qualityid="";
						String qualitylabel="";
						String relatedentityid="";
						String relatedentitylabel="";
						
						output.println("<TR>");
						output.println("<TD>");
						output.println(characteridlabel.split("\\|\\|\\|")[0]);
						output.println("</TD>");
						output.println("<TD>");
						output.println(characteridlabel.split("\\|\\|\\|")[1]);
						output.println("</TD>");
						output.println("<TD>");
						output.println(stateidlabel.split("\\|\\|\\|")[0]);
						output.println("</TD>");
						output.println("<TD>");
						output.println(stateidlabel.split("\\|\\|\\|")[1]);
						output.println("</TD>");
						output.println("<TD>");
						output.println(source);
						output.println("</TD>");	
						int eqcount=1;
						for(EQHolder eq:eqs)
						{
							entityid += eqcount+". *"+eq.getEntityid().replaceAll("(Score:)","").replaceAll(",", "<BR>")+"<BR>";
							entitylabel += eqcount+". *"+eq.getEntitylabel().replaceAll("(Score:)","").replaceAll(",", "<BR>")+"<BR>";
							if((eq.getRelatedentityid()!=null)&&(eq.getRelatedentityid().equals("")==false))
							{
							relatedentityid += eqcount+". *"+eq.getRelatedentityid().replaceAll("(Score:)","").replaceAll(",", "<BR>")+"<BR>";
							relatedentitylabel += eqcount+". *"+eq.getRelatedentitylabel().replaceAll("(Score:)","").replaceAll(",", "<BR>")+"<BR>";
							}
							qualityid += eqcount+". *"+eq.getQualityid().replaceAll("(Score:)","").replaceAll(",", "<BR>")+"<BR>";
							qualitylabel += eqcount+". *"+eq.getQualitylabel().replaceAll("(Score:)","").replaceAll(",", "<BR>")+"<BR>";
						eqcount++;
						}
						
						output.println("<TD>");
						output.println(entityid.equals("")?"&nbsp;":entityid);
						output.println("</TD>");
						
						output.println("<TD>");
						output.println(entitylabel.equals("")?"&nbsp;":entitylabel);
						output.println("</TD>");
						
						output.println("<TD>");
						output.println(qualityid.equals("")?"&nbsp;":qualityid);
						output.println("</TD>");
						
						output.println("<TD>");
						output.println(qualitylabel.equals("")?"&nbsp;":qualitylabel);
						output.println("</TD>");
						
						output.println("<TD>");
						output.println(relatedentityid.equals("")?"&nbsp;":relatedentityid);
						output.println("</TD>");
						
						output.println("<TD>");
						output.println(relatedentitylabel.equals("")?"&nbsp;":relatedentitylabel);
						output.println("</TD>");
						
						output.println("</TR>");
					}
				}
				
			}
			
			
			output.println("</TABLE>");
			output.println("</CENTER>");
			output.println("</BODY>");
			output.println("</HTML>");
		output.close();
		conn.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		
	
	}
	public static void main(String[] args) {

		outputHTML("Charparser_output","curator",3);
	}
	}


