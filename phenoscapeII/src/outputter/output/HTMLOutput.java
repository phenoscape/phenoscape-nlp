package outputter.output;

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

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import outputter.ApplicationUtilities;
import outputter.XML2EQ;
import outputter.evaluation.EQHolder;
import owlaccessor.OWLAccessorImpl;

public class HTMLOutput {

	public static Connection conn;
	private static final Logger LOGGER = Logger.getLogger(HTMLOutput.class);  
	/**
	 * @param args
	 */
	/*
	 * Creates new DB connection
	 * 
	 */
	public static void GetDBConnection()
	{
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
			conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Fetches all the character,state eq statements from the database.
	 * 
	 * @param character_state contains all the character,state information
	 * @param characters holds all the EQ statements belonging a state,character combination
	 * @param outputtable contains the output from the previous step
	 * @param curatortablename contains the curatortable names which should be outputted along with original input
	 * @param number of curators contains the number of curator result against which charparser output is being compared to
	 * 
	 */
	public static void fetchFromDB(ArrayList<String> character_state, LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, ArrayList<EQHolder>>>> characters, String outputtable, String curatortablename, int numberofcurators) throws SQLException
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
			
			sql = "select characterlabel,statelabel,entitylabel,entityid,qualitylabel,qualityid,relatedentitylabel,relatedentityid from  "+ApplicationUtilities.getProperty("database.name")+"."+outputtable+
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
				sql = "select entitylabel,entityid,qualitylabel,qualityid,relatedentitylabel,relatedentityid from "+ApplicationUtilities.getProperty("database.name")+"."+curatortablename+(count+1)+
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
				
				individualstateeqs.put(curatortablename+(count+1), curator1state);
				
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
	 * Prints the ontology and charparser versions in the output HTML
	 * 
	 * 
	 */
	public static void version(PrintWriter output,String version) throws IOException, OWLOntologyCreationException
	{
		OWLOntology temp=null;
		output.println("<CENTER> <TABLE border = 2> <TR> <TH COLSPAN =\"2\">");
		output.println("<B>Ontology Versions</B>");
		output.println("</TH></TR>");
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
	
	public static void outputHTML(String outputtable,String curatortablename, int numberofcurators)
	{

		String columns[] = {"CharacterID","CharacterLabel","StateID","StateLabel","Source of EQ","EntityID",
							"EntityLabel","QualityID","QualityLabel","RelatedEntityID","RelatedEntityLabel"};
		Statement stmt;
		ArrayList<String> character_state = new ArrayList<String>();
		try {
			PrintWriter output = new PrintWriter(new FileWriter(ApplicationUtilities.getProperty("output.dir")+outputtable+".html"));
			LOGGER.debug("output saved in "+ApplicationUtilities.getProperty("output.dir")+outputtable+".html");
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
					version(output,ApplicationUtilities.getProperty("charparser.version"));
				} catch (OWLOntologyCreationException e) {
					e.printStackTrace();
				}
			}
			output.println("</CENTER><BR><BR>");

			output.println("<CENTER>");
			output.println("<TABLE BORDER=\"1\" width=\"250%\">");			
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"20%\"/>");
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"20%\"/>");
			output.println("<col width=\"5%\"/>");
			output.println("<col width=\"20%\"/>");
			
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
			
			String sql = "select distinct characterid, stateid from  "+ApplicationUtilities.getProperty("database.name")+"."+outputtable;
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
			fetchFromDB(character_state,characters,outputtable,curatortablename,numberofcurators);
			
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
							entityid += eqcount+". *"+eq.getEntityid().replaceAll("(Score:)","").replaceAll("@,", "<BR>*")+"<BR>";
							entitylabel += eqcount+". *"+eq.getEntitylabel().replaceAll("(Score:)","").replaceAll("@,", "<BR>*")+"<BR>";
							if((eq.getRelatedentityid()!=null)&&(eq.getRelatedentityid().equals("")==false))
							{
							relatedentityid += eqcount+". *"+eq.getRelatedentityid().replaceAll("(Score:)","").replaceAll("@,", "<BR>*")+"<BR>";
							relatedentitylabel += eqcount+". *"+eq.getRelatedentitylabel().replaceAll("(Score:)","").replaceAll("@,", "<BR>*")+"<BR>";
							}
							qualityid += eqcount+". *"+eq.getQualityid().replaceAll("(Score:)","").replaceAll("@,", "<BR>*")+"<BR>";
							qualitylabel += eqcount+". *"+eq.getQualitylabel().replaceAll("(Score:)","").replaceAll("@,", "<BR>*")+"<BR>";
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


