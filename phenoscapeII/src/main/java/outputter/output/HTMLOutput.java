package outputter.output;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import outputter.ApplicationUtilities;
import outputter.XML2EQ;
import outputter.evaluation.EQHolder;
import outputter.knowledge.ELKReasoner;
import owlaccessor.OWLAccessorImpl;

public class HTMLOutput {

	public static Connection conn;
	private static final Logger LOGGER = Logger.getLogger(HTMLOutput.class); 
	static ELKReasoner elkentity;
	static ELKReasoner elkquality;
	static ELKReasoner elkspatial=null;

	private static Hashtable<String, String> id2number= new Hashtable<String, String>();

	static String relation ="inheres_in|adjacent_to|distal_to|OBO_REL_part_of|part of|inheres in|adjacent to|distal to|PHENOSCAPE_complement_of|complement of|and|some|bearer_of|anterior_to|anteriorly_connected_to|attaches_to|extends_from|connected_to|decreased_in_magnitude_relative_to|deep_to|develops_from|distal_to|distally_connected_to|dorsal_to|encloses|extends_to|has_cross_section|has_muscle_insertion|has_muscle_origin|has_part|in_anterior_side_of|in_distal_side_of|in_lateral_side_of|in_left_side_of|in_median_plane_of|in_posterior_side_of|in_proximal_side_of|in_right_side_of|increased_in_magnitude_relative_to|located_in|overlaps|part_of|passes_through|posterior_to|posteriorly_connected_to|proximal_to|proximally connected to|similar_in_magnitude_relative_to|surrounded by|surrounds|ventral_to|vicinity_of|serves_as_attachment_site_for|inheres_in|not";
	static String relationid = relation+ "|BFO_0000050|BFO_0000052|BFO_0000053|BFO:0000053|RO:0002220|BSPO:0000096|UBERON:anteriorly_connected_to|UBERON:attaches_to|PHENOSCAPE:extends_from|RO:0002150|PATO:decreased_in_magnitude_relative_to|BSPO:0000107|RO:0002202|BSPO:0000097|UBERON:distally_connected_to|BSPO:0000098|UBERON:encloses|PHENOSCAPE:extends_to|PATO:has_cross_section|UBERON:has_muscle_insertion|UBERON:has_muscle_origin|BFO:0000051|BSPO:0000123|BSPO:0000125|UBERON:in_lateral_side_of|BSPO:0000120|UBERON:in_median_plane_of|BSPO:0000122|BSPO:0000124|BSPO:0000121|PATO:increased_in_magnitude_relative_to|OBO_REL:located_in|RO:0002131|BFO:0000050|BSPO:passes_through|BSPO:0000099|UBERON:posteriorly_connected_to|BSPO:0000100|UBERON:proximally_connected_to|PATO:similar_in_magnitude_relative_to|RO:0002219|RO:0002221|BSPO:0000102|BSPO:0000103|PHENOSCAPE:serves_as_attachment_site_for|PHENOSCAPE:complement_of";

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
	public static void fetchFromDB(ArrayList<String> character_state, LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, ArrayList<EQHolder>>>> characters, String outputtable, String[] curatortablenames) throws SQLException
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

			
			//curators first

			for(int c=0; c<curatortablenames.length; c++)
			{
				
				sql = "select characterlabel,statelabel, entitylabel,entityid,qualitylabel,qualityid,relatedentitylabel,relatedentityid from "+ApplicationUtilities.getProperty("database.name")+"."+curatortablenames[c]+
						" where characterid = '"+characterid +"' and stateid='"+stateid+"'";

				rs = stmt.executeQuery(sql);
				ArrayList<EQHolder> curator1state = new ArrayList<EQHolder>();

				while(rs.next())
				{
					EQHolder eq = new EQHolder();
					if(c==0){
						characteridlabel=characterid+"|||"+rs.getString("characterlabel");
						stateidlabel=stateid+"|||"+rs.getString("statelabel");
					}
					eq.setEntityid(rs.getString("entityid"));
					eq.setEntitylabel(rs.getString("entitylabel"));
					eq.setQualityid(rs.getString("qualityid"));
					eq.setQualitylabel(rs.getString("qualitylabel"));
					eq.setRelatedentityid(rs.getString("relatedentityid"));
					eq.setRelatedentitylabel(rs.getString("relatedentitylabel"));
					/*eq.setGsunontologizedentity(retrieveunontologized(rs.getString("entityid"),rs.getString("entitylabel")));
					eq.setGsunontologizedquality(retrieveunontologized(rs.getString("qualityid"),rs.getString("qualitylabel")));
					eq.setGsunontologizedrelatedentity(retrieveunontologized(rs.getString("relatedentityid"),rs.getString("relatedentitylabel")));
					eq.setUnontologizedentity("");
					eq.setUnontologizedquality("");
					eq.setUnontologizedrelatedentity("");*/
					//will send each of the id,label to the checkontology function, that returns the labels of unontologized ids
					curator1state.add(eq);

				}

				individualstateeqs.put(curatortablenames[c], curator1state);

			}
			
			//machine
			sql = "select entitylabel,entityid,qualitylabel,qualityid,relatedentitylabel,relatedentityid,unontologizedentity, unontologizedquality, unontologizedrelatedentity from  "+ApplicationUtilities.getProperty("database.name")+"."+outputtable+
					" where characterid = '"+characterid +"' and stateid='"+stateid+"'";

			rs = stmt.executeQuery(sql);
			ArrayList<EQHolder> charparserstate = new ArrayList<EQHolder>();



			while(rs.next())
			{
				EQHolder eq = new EQHolder();

				eq.setEntityid(rs.getString("entityid"));
				eq.setEntitylabel(rs.getString("entitylabel"));

				eq.setQualityid(rs.getString("qualityid"));
				eq.setQualitylabel(rs.getString("qualitylabel"));

				eq.setRelatedentityid(rs.getString("relatedentityid"));
				eq.setRelatedentitylabel(rs.getString("relatedentitylabel"));

				/*eq.setUnontologizedentity(rs.getString("unontologizedentity")!=null?rs.getString("unontologizedentity"):"");
				eq.setUnontologizedquality(rs.getString("unontologizedquality")!=null?rs.getString("unontologizedquality"):"");
				eq.setUnontologizedrelatedentity(rs.getString("unontologizedrelatedentity")!=null?rs.getString("unontologizedrelatedentity"):"");

				eq.setGsunontologizedentity("");
				eq.setGsunontologizedquality("");
				eq.setGsunontologizedrelatedentity("");*/

				charparserstate.add(eq);

			}

			individualstateeqs.put("Charaparser", charparserstate);

			

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

	private static String retrieveunontologized(String id, String label) {

		int count=0;
		System.out.println();

		String ids[] = extractids(id);
		ELKReasoner tempelk=null;
		String unontologized ="";
		id = id.toUpperCase().trim();

		for(String tempid:ids)
		{
			if(tempid.contains("BSPO"))
			{
				tempelk = elkspatial;
			} else if(tempid.contains("PATO"))
			{
				tempelk = elkquality;
			} else
			{
				tempelk = elkentity;
			}
			if(tempelk.CheckClassExistence(tempid)==false)
			{
				unontologized+=extractLabel(label,count)+"#";
			}
			count++;
		}
		unontologized = unontologized.replaceAll("(#)$", "");

		return unontologized;
	}



	private static String extractLabel(String label,int index)
	{
		String labels[]=null,final_labels[] = null;
		int count=0;
		label= label.replaceAll("(\\(|\\))", "");
		labels = label.split(relation);
		final_labels = new String[labels.length];	

		for(String temp:labels)
		{
			if(temp.equals(" ")==false)
			{
				final_labels[count++] = temp;
			}
		}
		System.out.println(final_labels[index].trim());
		return final_labels[index].trim();

	}


	//Extracts ids from the string

	private static String[] extractids(String value) {
		value=value.replaceAll("(\\(|\\))", "");
		String[] temp = value.split("\\s");
		String id ="";
		for(String t:temp)
		{
			if(t.matches("[A-Z]+[_:][0-9]+")==true)
			{
				id+=t+" ";
			}
		}
		id=id.trim();
		return id.split(" ");
	}

	/*
	 * Prints the ontology and charparser versions in the output HTML
	 * 
	 * 
	 */
	public static void version(PrintWriter output,String version) throws IOException, OWLOntologyCreationException
	{
		//OWLOntology temp=null;
		output.println("<CENTER> <TABLE border = 2> <TR> <TH COLSPAN =\"2\">");
		output.println("<B>Ontology Versions</B>");
		output.println("</TH></TR>");
		for(OWLAccessorImpl api: XML2EQ.ontoutil.OWLqualityOntoAPIs)
		{
			OWLOntology temp=null;
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
			OWLOntology temp=null;
			String S[]=api1.getSource().split("[\\\\.]");
			output.println("<TR> <TD>");
			output.println((S[S.length-2].toUpperCase()));
			output.println("</TD>  <TD>");
			Set<OWLOntology> ontologies = api1.getManager().getOntologies();
			for(OWLOntology ontology:ontologies)
			{
				if(ontology.getOntologyID().getVersionIRI()!=null){
					temp = ontology;
					break;
				}
				/*if(ontology.toString().contains(S[S.length-2]))
				{
					temp = ontology;
					break;
				}*/
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

	public static void outputHTML(String outputtable,String[] curatortablenames, String htmloutputfilename, String idmapping)
	{

		if(idmapping!=null){
		File f = new File(idmapping);
		if(f.exists()){
			populateId2Number(f);
		}
		}
		//String columns[] = {"CharacterID","CharacterLabel","StateID","StateLabel","Source of EQ","EntityID",
		//					"EntityLabel","QualityID","QualityLabel","RelatedEntityID","RelatedEntityLabel","UnontologizedEntity","UnontologizedQuality","UnontologizedRelatedEntity",
		//					"GSunontologizedEntity","GSunontologizedQuality","GSunontologizedRelatedEntity"};
		String columns[] = {"CharacterID","CharacterNumber","CharacterLabel","StateID","StateNumber","StateLabel","Source of EQ","EntityID",
				"EntityLabel","QualityID","QualityLabel","RelatedEntityID","RelatedEntityLabel"};
		Statement stmt;
		ArrayList<String> character_state = new ArrayList<String>();

		try {
			elkentity = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.uberon")+".owl"), false);
			elkquality = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.pato")+".owl"), false);
			elkspatial = new ELKReasoner(new File(ApplicationUtilities.getProperty("ontology.dir")+System.getProperty("file.separator")+ApplicationUtilities.getProperty("ontology.bspo")+".owl"), false);
			//elkentity = new ELKReasoner(new File(XML2EQ.uberon), false);
			//elkquality = new ELKReasoner(new File(XML2EQ.pato), false);
			//elkspatial = new ELKReasoner(new File(XML2EQ.bspo), false);
		} catch (OWLOntologyCreationException e1) {
			e1.printStackTrace();
		}

		try {
			PrintWriter output = new PrintWriter(new FileWriter(ApplicationUtilities.getProperty("output.dir")+htmloutputfilename+".html"));
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
			/*output.println("<col width=\"5%\"/>"); //1
			output.println("<col width=\"5%\"/>"); //2
			output.println("<col width=\"5%\"/>"); //3
			output.println("<col width=\"5%\"/>"); //4
			output.println("<col width=\"5%\"/>"); //5
			output.println("<col width=\"5%\"/>"); //6
			output.println("<col width=\"20%\"/>"); //7
			output.println("<col width=\"5%\"/>"); //8
		    output.println("<col width=\"20%\"/>"); //9
			output.println("<col width=\"5%\"/>"); //10
			output.println("<col width=\"20%\"/>"); //11
			 */

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

			//sql= "create table if not exists htmloutput(characterid varchar(300), characterlabel varchar(500), ";

			String sql = "select distinct characterid, stateid from  "+ApplicationUtilities.getProperty("database.name")+"."+curatortablenames[0];
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
			fetchFromDB(character_state,characters,outputtable,curatortablenames);

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
						/*String unontologizedentity="";
						String unontologizedquality="";
						String unontologizedrelatedentity="";
						String gsunontologizedentity="";
						String gsunontologizedquality="";
						String gsunontologizedrelatedentity="";*/

						
						String characterid = characteridlabel.substring(0, characteridlabel.indexOf("|||"));
						String stateid =stateidlabel.substring(0, stateidlabel.indexOf("|||"));
						String characternumber = HTMLOutput.id2number.get(characterid);
						String statenumber = HTMLOutput.id2number.get(stateid).replace(characternumber+"_", "");
						String characterlabel=characteridlabel.substring(characteridlabel.indexOf("|||")+3);
						String statelabel = stateidlabel.substring(stateidlabel.indexOf("|||")+3);

					
						output.println("<TR>");
						output.println("<TD>");
						output.println(characterid);
						output.println("</TD>");
						output.println("<TD>");
						output.println(characternumber);
						output.println("</TD>");
						output.println("<TD>");
						output.println(characterlabel);
						output.println("</TD>");
						output.println("<TD>");
						output.println(stateid);
						output.println("</TD>");
						output.println("<TD>");
						output.println(statenumber);
						output.println("</TD>");
						output.println("<TD>");
						output.println(statelabel);
						output.println("</TD>");
						output.println("<TD>");
						output.println(source);
						output.println("</TD>");	
						int eqcount=1;
						for(EQHolder eq:eqs)
						{

							entityid += eqcount+". *"+(eq.getEntityid()==null? "" : eq.getEntityid().replaceAll("(Score:)","").replaceAll("@,", "<BR>*"))+"<BR>";
							entitylabel += eqcount+". *"+(eq.getEntitylabel()==null? "":eq.getEntitylabel().replaceAll("(Score:)","").replaceAll("@,", "<BR>*"))+"<BR>";
							//if((eq.getRelatedentityid()!=null)&&(eq.getRelatedentityid().equals("")==false))
							//{
							relatedentityid += eqcount+". *"+(eq.getRelatedentityid()==null?"":eq.getRelatedentityid().replaceAll("(Score:)","").replaceAll("@,", "<BR>*"))+"<BR>";
							relatedentitylabel += eqcount+". *"+(eq.getRelatedentitylabel()==null?"":eq.getRelatedentitylabel().replaceAll("(Score:)","").replaceAll("@,", "<BR>*"))+"<BR>";
							//}
							qualityid += eqcount+". *"+(eq.getQualityid()==null?"":eq.getQualityid().replaceAll("(Score:)","").replaceAll("@,", "<BR>*"))+"<BR>";
							qualitylabel += eqcount+". *"+(eq.getQualitylabel()==null?"":eq.getQualitylabel().replaceAll("(Score:)","").replaceAll("@,", "<BR>*"))+"<BR>";

							/*unontologizedentity += eqcount+". *"+eq.getUnontologizedentity().replaceAll("@,", "<BR>*")+"<BR>";
							unontologizedquality += eqcount+". *"+eq.getUnontologizedquality().replaceAll("@,", "<BR>*")+"<BR>";
							unontologizedrelatedentity += eqcount+". *"+eq.getUnontologizedrelatedentity().replaceAll("@,", "<BR>*")+"<BR>";

							gsunontologizedentity += eqcount+". *"+eq.getGsunontologizedentity().replaceAll("#", "<BR>*")+"<BR>";
							gsunontologizedquality += eqcount+". *"+eq.getGsunontologizedquality().replaceAll("#", "<BR>*")+"<BR>";
							gsunontologizedrelatedentity += eqcount+". *"+eq.getGsunontologizedrelatedentity().replaceAll("#", "<BR>*")+"<BR>";*/

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

						/*output.println("<TD>");
						output.println(unontologizedentity.equals("")?"&nbsp;":unontologizedentity);
						output.println("</TD>");

						output.println("<TD>");
						output.println(unontologizedquality.equals("")?"&nbsp;":unontologizedquality);
						output.println("</TD>");

						output.println("<TD>");
						output.println(unontologizedrelatedentity.equals("")?"&nbsp;":unontologizedrelatedentity);
						output.println("</TD>");

						output.println("<TD>");
						output.println(gsunontologizedentity.equals("")?"&nbsp;":gsunontologizedentity);
						output.println("</TD>");

						output.println("<TD>");
						output.println(gsunontologizedquality.equals("")?"&nbsp;":gsunontologizedquality);
						output.println("</TD>");

						output.println("<TD>");
						output.println(gsunontologizedrelatedentity.equals("")?"&nbsp;":gsunontologizedrelatedentity);
						output.println("</TD>");
						 */
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

		LOGGER.debug("output saved in "+ApplicationUtilities.getProperty("output.dir")+htmloutputfilename+".html");
	}
	private static void populateId2Number(File f) {
		FileInputStream fis = null;
		BufferedReader reader = null;

		try {
			fis = new FileInputStream(f);
			reader = new BufferedReader(new InputStreamReader(fis));

			String line = reader.readLine();
			while(line != null){
				String[] ids = line.split("\\s+");
				HTMLOutput.id2number.put(ids[1], ids[0]);
				line = reader.readLine();
			}           

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				reader.close();
				fis.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	} 



	public static void main(String[] args) {
		String choutput = "xml2eq_best_full";
		//String choutput ="naive_40674";
		//String[] cuoutputs = new String[]{"naive_38484", "naive_40674", "naive_40676", "knowledge_40716", "knowledge_40717", "knowledge_40718"};
		String[] cuoutputs = new String[]{"knowledge_40716", "knowledge_40717", "knowledge_40718"};
		//String htmloutput = "naive_40674_to_3_knoweldge";
		String htmloutput = "knowledge_and_charaparser_best";
		String mappingfile="C:/Users/updates/CharaParserTest/charaParserEval2013/matchIds/id_mapping.txt";
		outputHTML(choutput,cuoutputs, htmloutput, mappingfile);
	}
}


