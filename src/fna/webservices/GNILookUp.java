package fna.webservices;

/**
 * This class name-tags the transformation folder and creates a new folder "name-tagged"
 * @author Partha Pratim Sanyal (ppsanyal@email.arizona.edu)
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import fna.parsing.ApplicationUtilities;

public class GNILookUp {

	private static Properties properties = null;
	private static FileInputStream fstream = null;
	private static Set<Object> tagKeys = null;
	private static final Logger LOGGER = Logger.getLogger(GNILookUp.class);
	private String gniURL = ApplicationUtilities.getProperty("GNI");
	private static String plantURL = ApplicationUtilities.getProperty("IPNI");	
	private String plaziWebService = ApplicationUtilities.getProperty("PLAZI");
	
	private ArrayList <String> tags ;
	private HashMap<String, String> lsidMap;
	private ArrayList<String> dictionary;
	
	static {
		try {
			fstream = new FileInputStream(System.getProperty("user.dir")+
					"\\markuptags.properties");
			properties = new Properties();
			properties.load(fstream);
			tagKeys = properties.keySet();
		} catch (FileNotFoundException e) {
			LOGGER.error("couldn't open file in GNILookUp static block", e);
		} catch (IOException e) {
			LOGGER.error("couldn't open file in GNILookUp static block", e);
			e.printStackTrace();
		} 
	}

	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String source1 = "D:\\FNA\\FNAV19\\target\\transformed";
		String destination1 = "D:\\FNA\\FNAV19\\target\\name-tagged\\";
		new GNILookUp().getTagNames(source1, destination1);
		
		//getLSIDFromPlantServer("Illecebrum verticillatum", plantURL);

	}
	

	public void getTagNames(String source, String destination) {

		File transformedDirectory  = new File(source);
		File [] files = transformedDirectory.listFiles();
		
		/* The grand for loop */
		try {
			//createDictionary();
			lsidMap = new HashMap<String, String>();
			for (File file : files) {
				tags = new ArrayList<String>();
								
/*				readTags(file);
				writeMarkUp(file, destination1);*/
				
				markUpScientificNames(file, destination);
				
				//remove this break later - now test for one file
				//break;
			}			
			saveNames();
		} catch (Exception exe){
			exe.printStackTrace();
			LOGGER.error("Error in write getTagNames ", exe);
		}

		
	}
/**
 * This function extracts, validates and marks up scientific names
 * @param file
 * @param destination
 */
	
	private void markUpScientificNames(File file, String destination) {
		try {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(file);
	        doc.getDocumentElement().normalize();	        
	        TransformerFactory tFactory = TransformerFactory.newInstance();
	        Transformer tFormer = tFactory.newTransformer();
	        
			for (Object obj : tagKeys) {
				String tagName = properties.getProperty((String)obj);
				if (!tagName.equals(properties.getProperty("discussion"))) {
					NodeList nodes = doc.getElementsByTagName(tagName);
					Element element = (Element)nodes.item(0);
					if (element != null) {
						String tagValue = element.getFirstChild().getNodeValue();
						if (tagValue != null && !tagValue.equals("")){
							/*						
							 * LinkedHashSet <String> name = lookUpScientificName(clean(tagValue), plaziWebService);
							String tempTag = null;
							for (String tempTagValue : name) {
							 tempTag = tempTagValue;	
							}*/
							String LSID = extractLSIDFromGNI(clean(tagValue), gniURL); 
							if (LSID != null && !LSID.equals("")) {
								lsidMap.put(tagValue, LSID);
								System.out.println("Name found : " + tagValue);
								LOGGER.info("Name found : " + tagValue);
								
								/* Markup the node immediately*/
				        		element.getFirstChild().setNodeValue("");
				        		Element elem = doc.createElement("name");
				        		elem.setAttribute("lsid", LSID);
				        		elem.setAttribute("src", "gni");
				        		elem.appendChild(doc.createTextNode(tagValue));
				        		element.appendChild(elem);
				        		System.out.println("Written lsid string tag for " + tagValue);
				        		LOGGER.info("Written lsid string tag for " + tagValue);
								
							}
						}

					}
				} else {
					NodeList nodes = doc.getElementsByTagName(tagName);
					int nodeLength = nodes.getLength();
					for (int i = 0 ; i < nodeLength; i++) {
						Element element = (Element)nodes.item(i);
						if (element != null) {
	
							String tagValue = element.getFirstChild().getNodeValue();
							String discussion = tagValue;
							System.out.println("Marking up discussion : " + discussion);
							
							/* Extract the Scientific names using Plazi Web Service*/
							LinkedHashSet <String> scientificNames = 
								lookUpScientificName(discussion, plaziWebService, 
										"Botany.Partha.web");
							System.out.println("Plazi returned the following valid names : " +
									scientificNames);
							HashMap <String, String> tempLsidMap = new HashMap<String, String>();
							HashMap <String, String> tempPlantLsidMap = new HashMap<String, String>();
							
							/* Extract the LSID from GNI and IPNI */
							for (String name : scientificNames) {
								String LSID;
								LSID = extractLSIDFromGNI(name, gniURL);
								if(LSID != null && !LSID.equals("")) {
									tempLsidMap.put(name, LSID);
								}
								LSID = null;
								LSID = getLSIDFromIPNI(name, plantURL);
								if(LSID != null && !LSID.equals("")) {
									tempPlantLsidMap.put(name, LSID);
								}
								
							}
							System.out.println("GNI correctly recognized the following names " +
									tempLsidMap);
							System.out.println("IPNI correctly recognized the following names " +
									tempPlantLsidMap);
							
							/* Insert lsid nodes inside the discussion */
							
							String originalNodeValue = nodes.item(i).getFirstChild().getNodeValue();
			        		String temp = originalNodeValue;
			        		
			        		nodes.item(i).getFirstChild().setNodeValue("");
			        		Set <String> keys  = tempLsidMap.keySet();
			        		
			        		for (String key : keys) {
			        			temp = temp.replace(key, "#"+key+"#");
			        		}
			        		
			        		String [] parts = temp.split("#");
			        		
			        		for (String part : parts){
			        			
			        			if (tempLsidMap.containsKey(part)) {
			        				System.out.println("Marking up " + part);
			    	        		Element elem = doc.createElement("name");
			    	        		if (tempPlantLsidMap.containsKey(part)) {
				    	        		elem.setAttribute("lsid", tempLsidMap.get(part)+","+tempPlantLsidMap.get(part));
				    	        		elem.setAttribute("src", "gni, plant");
			    	        		} else {
				    	        		elem.setAttribute("lsid", tempLsidMap.get(part));
				    	        		elem.setAttribute("src", "gni");
			    	        		}

			    	        		elem.appendChild(doc.createTextNode(part));
			    	        		nodes.item(i).appendChild(elem);
			        			} else {
			        				nodes.item(i).appendChild(
			        						doc.createTextNode(part));
			        			}
			        		}								
						
			        		lsidMap.putAll(tempLsidMap);
						}

					}
				}

				
			} 
			
	        FileOutputStream flt = new FileOutputStream
	        (new File(destination+file.getName()));
	        OutputStreamWriter out = new OutputStreamWriter(flt);
	        Source source = new DOMSource(doc);
	        StreamResult result = new StreamResult(out);
	        tFormer.transform(source, result); 
	        
	} catch (Exception exe) {
		exe.printStackTrace();
		LOGGER.error("Problem in markUpScientificNames" , exe);
	}
  }
	/**
	 * This method extracts the valid Scientific Names using Plazi Web Service
	 * @param discussion
	 * @param url
	 * @return
	 */
	@SuppressWarnings("null")
	public static LinkedHashSet <String> lookUpScientificName(String discussion, 
			String url, String select) 
		throws IOException{
		
		LinkedHashSet <String> validNames = new LinkedHashSet <String>();
		OutputStreamWriter outStream = null;
		try {
		    // Construct data
		    String data = URLEncoder.encode("document_text", "UTF-8") 
		    + "=" + URLEncoder.encode(clean(discussion), "UTF-8");
		    data += "&" + URLEncoder.encode("omni_fat_instance", "UTF-8") 
		    + "=" + URLEncoder.encode(select, "UTF-8");

		    // Send data
		    URLConnection conn = new URL(url).openConnection();
		    conn.setDoOutput(true);
		    outStream = new OutputStreamWriter(conn.getOutputStream());
		    outStream.write(data);
		    outStream.flush();

	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(conn.getInputStream());
	        NodeList nodes = doc.getElementsByTagName("dwc:ScientificName");
	        
	        int nodeLength = nodes.getLength();
	        for (int i = 0; i< nodeLength;  i++) {
	        	Element element = (Element)nodes.item(i);
	        	String nodeValue = element.getFirstChild().getNodeValue();
	        	if (nodeValue != null || !nodeValue.equals("")) {
	        		validNames.add(nodeValue);
	        	}
	        		        			        	
	        }
		   	    
		    
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("Plazi Web Service had a problem", e);
		} finally {
			if (outStream != null) {
				outStream.close();	
			}

		}
		
		return validNames;
	}
	

	/**
	 * This Method will extract the LSID
	 * @param tagValue
	 * @param url
	 * @return
	 * @throws Exception
	 */
	private String extractLSIDFromGNI(String tagValue, String url) throws Exception {
		URL gniUrl = new URL(url+URLEncoder.encode(tagValue, "UTF-8") );
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(gniUrl.openStream());
        NodeList nodes = doc.getElementsByTagName("name");
        String LSID = "";
        int nodeLength = nodes.getLength();
        for (int i = 0; i< nodeLength;  i++) {
        	Element element = (Element)nodes.item(i);
        	String nodeValue = element.getFirstChild().getNodeValue();
        	if (nodeValue.equalsIgnoreCase(tagValue)) {
        		LSID = element.getNextSibling().getNextSibling().getNextSibling()
				.getNextSibling().getFirstChild().getNodeValue();
        		break;
        		
        	}
        }
        
        return LSID;
	}
	
	/**
	 * This Method looks up names on the IPNI plant server
	 * @param name
	 * @param sourceUrl
	 * @return
	 * @throws Exception
	 */
	
	private String getLSIDFromIPNI(String name, String sourceUrl) throws Exception {
		URL plantUrl = new URL(sourceUrl+name.replace(" ", "%2520") );
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(plantUrl.openStream());
        NodeList nodes = doc.getElementsByTagName("dc:title");
        String LSID = "";
        int nodeLength = nodes.getLength();
        for (int i = 0; i< nodeLength;  i++) {
        	Element element = (Element)nodes.item(i);
        	String nodeValue = element.getFirstChild().getNodeValue();
        	System.out.println(nodeValue);
        	if (nodeValue.equalsIgnoreCase(name)){
        		NamedNodeMap nm = element.getParentNode().getAttributes();
        		String LSIDUri = nm.getNamedItem("rdf:about").getNodeValue();
        		LSID = LSIDUri.substring(LSIDUri.lastIndexOf("/")+1);
        	}
        }
        return LSID;
	}
	/**
	 * This method cleans the String from clutter
	 * @param description
	 * @return
	 */
	private static String clean(String description) {
		Pattern p = Pattern.compile("[\\W\\d\\s\\e]+");
		String [] a = p.split(description);
		StringBuffer sb = new StringBuffer();
		for (String s : a) {
			sb.append(s+" ");
		}
		return sb.toString().trim();		
	}
	
	/**
	 * This method saves the discovered scientific names to the database.
	 */
	private void saveNames()throws SQLException {
		
		PreparedStatement stmt = null;
		Connection conn = null;
		
		try {
			Class.forName(ApplicationUtilities.getProperty("database.driverPath"));
			conn = DriverManager.getConnection(ApplicationUtilities.getProperty("database.url"));
			stmt = conn.prepareStatement("insert into gnilsids(name, lsid, source) values(?,?,?)");
			
			Set <String> keys = lsidMap.keySet();
    		for (String name : keys) {
    			stmt.setString(1, name);
    			stmt.setString(2, lsidMap.get(name));
    			stmt.setString(3, "gni");
    			try {
    				stmt.execute();
    			} catch (Exception exe) {
    				if (!exe.getMessage().contains("Duplicate")) {
    					exe.printStackTrace();
    					LOGGER.error("Excepion in saveNames ", exe);
    				}
    			}
    			
    		}
    		
		} catch(Exception exe) {
			exe.printStackTrace();
			LOGGER.error("Excepion in saveNames ", exe);
		} finally {
			if (stmt != null) {
				stmt.close();
			}
			
			if (conn != null){
				conn.close();
			}
		}
	}
	
	
	/**
	 * This method will create a dictionary of common English 
	 * words in memory to save the Web services invocation for every word.
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	private void createDictionary() throws Exception {
		BufferedReader in = null;
		try {
		    in = new BufferedReader(new FileReader("dictionary.txt"));
		    String word;
		    dictionary = new ArrayList<String>();
		    while ((word = in.readLine()) != null) {
		        dictionary.add(word.toLowerCase());
		    }
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Error in write createDictionary ", e);
		} finally {
			in.close();
		}
	}
	
	/**
	 * This method will mark the file with lsids
	 * @param file
	 * @param destination
	 */
	@SuppressWarnings("unused")
	private void writeMarkUp(File file, String destination) {
		try {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(file);
	        doc.getDocumentElement().normalize();
	        
	        TransformerFactory tFactory = TransformerFactory.newInstance();
	        Transformer tFormer = tFactory.newTransformer();
	        
	        Element root = doc.getDocumentElement();
	        NodeList nodes = root.getChildNodes();
	        for (int i = 0 ; i <nodes.getLength(); i++) {
	        	
	        	if (!nodes.item(i).getNodeName().equalsIgnoreCase
	        			(properties.getProperty("discussion"))) {
	        		String nodeName = nodes.item(i).getNodeName();
		        	boolean hasTag = tags.contains(nodeName);
		        	
		        	if (hasTag) { 
		        		String nodeValue = nodes.item(i).getFirstChild().getNodeValue();
		        		if (lsidMap.get(nodeValue) != null) {
			        		nodes.item(i).getFirstChild().setNodeValue("");
			        		Element elem = doc.createElement("name");
			        		elem.setAttribute("lsid", lsidMap.get(nodeValue));
			        		elem.setAttribute("src", "gni");
			        		elem.appendChild(doc.createTextNode(nodeValue));
			        		nodes.item(i).appendChild(elem);
			        		tags.remove(nodes.item(i).getNodeName());
			        		System.out.println("Written lsid string tag for " + nodeValue);
			        		LOGGER.info("Written lsid string tag for " + nodeValue);	
		        		}

		        	}
	        	} else {
	        		String original = nodes.item(i).getFirstChild().getNodeValue();
	        		String discussion = 
	        			clean(nodes.item(i).getFirstChild().getNodeValue());
	        		
	        		System.out.println("Processing the following discussion : "+ discussion);
	        		LOGGER.info("Processing the following discussion : "+ discussion);
	        		
	        		Scanner sc = new Scanner(discussion);
	        		while (sc.hasNext()) {
	        			String word = sc.next();
	        			if (lsidMap.get(word) != null) {
			        		Element elem = doc.createElement("name");
			        		elem.setAttribute("lsid", lsidMap.get(word));
			        		elem.setAttribute("src", "gni");
			        		elem.appendChild(doc.createTextNode(word));
			        		nodes.item(i).appendChild(elem);
	        			}
	        		}
	        		tags.remove(properties.getProperty("discussion"));
	        	}
	        }
	        FileOutputStream flt = new FileOutputStream
	        (new File(destination+file.getName()));
	        OutputStreamWriter out = new OutputStreamWriter(flt);
	        Source source = new DOMSource(doc);
	        StreamResult result = new StreamResult(out);
	        tFormer.transform(source, result); 
		}
		catch (Exception e) {
			System.out.println("Error: " + e);
			e.printStackTrace();
			LOGGER.error("Error in write MarkUp ", e);
		}
	}
	
	/**
	 * This method reads the file information into memory and also checks for lsid and 
	 * valid Scientific names.
	 * @param file
	 */
	@SuppressWarnings("unused")
	private void readTags(File file) {
		
		try {
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(file);
			for (Object obj : tagKeys) {
				String tagName = properties.getProperty((String)obj);
				if (!tagName.equals(properties.getProperty("discussion"))) {
					NodeList nodes = doc.getElementsByTagName(tagName);
					if (nodes.getLength() != 0){
						tags.add(tagName);
					}
					Element element = (Element)nodes.item(0);
					if (element != null) {
						String tagValue = element.getFirstChild().getNodeValue();
						String LSID = extractLSIDFromGNI(clean(tagValue), gniURL); 
						if (!LSID.equals("")) {
							lsidMap.put(tagValue, LSID);
							System.out.println("Name found : " + tagValue);
							LOGGER.info("Name found : " + tagValue);
						}
					}
				} else {
					NodeList nodes = doc.getElementsByTagName(tagName);
					if (nodes.getLength() != 0){
						tags.add(tagName);
					}
					int nodeLength = nodes.getLength();
					for (int i = 0 ; i < nodeLength; i++) {
						Element element = (Element)nodes.item(i);
						if (element != null) {
							String tagValue = element.getFirstChild().getNodeValue();
							String discussion = clean(tagValue);
							String [] words = discussion.split(" ");
							for (String word : words) {
								if (lsidMap.get(word) == null && 
										!dictionary.contains(word.toLowerCase())){
									String LSID = extractLSIDFromGNI(clean(word), gniURL); 
									if (!LSID.equals("")) {
										lsidMap.put(word, LSID);
										System.out.println("Name found : " + word);
										LOGGER.info("Name found : " + word);
									}
								}
							}
						}

					}
				}

				
			}
			System.out.println(lsidMap);
			LOGGER.info("LSIDMAP " + lsidMap);
			System.out.println(tags);
			LOGGER.info("Tags " + tags);
			
		} catch (Exception exe) {
			exe.printStackTrace();
			LOGGER.error("Error in write readTags ", exe);
		}


	}
}
