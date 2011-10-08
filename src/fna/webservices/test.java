package fna.webservices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class test {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
/*		String x = "This, is a &&^%$$^$sentence. \"containing\"; <some/ ; [punctuation]> :marks!+" +
				"was trying to&* ,,..??++*figure%$# ##out if@ this%%$678^ !!really ?works??";
		Pattern p = Pattern.compile("[\\W\\d\\s\\e]+");
		String [] a = p.split(x);
		for (String s : a) {
				System.out.print(s+" ");
		}
		System.out.println(a.length);*/
		try {
		//URL url = new URL("http://plazi2.cs.umb.edu:8080/OmniFAT/find_names");
/*			
		    String discussion = "Caryophyllaceae includes 54 locally endemic genera " +
		    		"(many of them in the eastern Mediterranean region of Europe, " +
		    		"Asia, and Africa), cultivated taxa (especially Dianthus, " +
		    		"Gypsophila, and Silene), and weedy taxa (mostly from Eurasia). " +
		    		"Of the 37 genera in the flora area, 15 are entirely non-native: " +
		    		"Agrostemma, Corrigiola, Gypsophila, Holosteum, Lepyrodiclis, " +
		    		"Moenchia, Myosoton, Petrorhagia, Polycarpaea, Polycarpon, " +
		    		"Saponaria, Scleranthus, Spergula, Vaccaria, and Velezia.";
		
			
			try {
			    // Construct data
			    String data = URLEncoder.encode("document_text", "UTF-8") 
			    + "=" + URLEncoder.encode(discussion, "UTF-8");
			    data += "&" + URLEncoder.encode("omni_fat_instance", "UTF-8") 
			    + "=" + URLEncoder.encode("Botany.web", "UTF-8");

			    // Send data
			    URL url = new URL("http://plazi2.cs.umb.edu:8080/OmniFAT/find_names");
			    URLConnection conn = url.openConnection();
			    conn.setDoOutput(true);
			    OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			    wr.write(data);
			    wr.flush();

			    // Get the response
			    BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			    String line;
			    while ((line = rd.readLine()) != null) {
			        // Process line...
			    	System.out.println(line);
			    }
			    wr.close();
			    rd.close();
			    
			    
			} catch (Exception e) {
			} */
			
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse("D:\\FNA\\FNAV19\\target\\transformed\\1.xml");
	        doc.getDocumentElement().normalize();
	        
	        TransformerFactory tFactory = TransformerFactory.newInstance();
	        Transformer tFormer = tFactory.newTransformer();
	        
	        Element root = doc.getDocumentElement();
	        System.out.println(root.getNodeName() + root.getNodeValue());
	        NodeList nodes = root.getChildNodes();
	        String [] taglist = {"discussion"};
	        for (int i = 0 ; i <nodes.getLength(); i++) {
	        	//Element el = (Element);
	        	if (nodes.item(i).getNodeName().equals(taglist[0])) {
/*	        		nodes.item(i).getFirstChild().setNodeValue("partha" + 
	        				nodes.item(i).getFirstChild().getNodeValue());*/
	        		HashMap<String, String> found = new HashMap<String, String>();
	        		found.put("Illecebrum verticillatum", 
	        				"urn:lsid:globalnames.org:index:a4a43b7a-eead-55ae-bb0b-195eaadf9c6b");
	        		found.put("Europe", 
	        				"urn:lsid:globalnames.org:index:29e3b733-2106-5ca8-bfc9-12d1b77f3310");
	        		found.put("Herniaria", 
	        				"urn:lsid:globalnames.org:index:4c09c584-4bc5-574b-942f-49b2735f50a8");
	        		found.put("Paronychia", 
	        				"urn:lsid:globalnames.org:index:df9047f9-7acf-535f-8161-bc62237c1871");
	        		
	        		
	        		
	        		String originalNodeValue = nodes.item(i).getFirstChild().getNodeValue();
	        		String temp = originalNodeValue;
	        		
	        		nodes.item(i).getFirstChild().setNodeValue("");
	        		Set <String> keys  = found.keySet();
	        		
	        		for (String key : keys) {
	        			temp = temp.replace(key, "#"+key+"#");
	        		}
	        		
	        		String [] parts = temp.split("#");
	        		
	        		for (String part : parts){
	        			if (found.containsKey(part)) {
	    	        		Element elem = doc.createElement("name");
	    	        		elem.setAttribute("lsid", found.get(part));
	    	        		elem.setAttribute("src", "gni");
	    	        		elem.appendChild(doc.createTextNode(part));
	    	        		nodes.item(i).appendChild(elem);
	        			} else {
	        				nodes.item(i).appendChild(
	        						doc.createTextNode(part));
	        			}
	        		}
/*	        		nodes.item(i).getFirstChild().setNodeValue("");
	        		Element elem = doc.createElement("name");
	        		elem.setAttribute("lsid", "bla bla");
	        		elem.setAttribute("src", "gni");
	        		//elem.appendChild(doc.createTextNode(nodeValue));
	        		elem.appendChild(doc.createTextNode("This is new node"));
	        		nodes.item(i).appendChild(elem);
	        		nodes.item(i).appendChild(doc.createTextNode("This is the 2nd node"));*/
	        		
	        		break;
	        	}
/*	        	System.out.println(nodes.item(i).getNodeName());
	        	if (nodes.item(i).getFirstChild() != null)
	        	System.out.println(nodes.item(i).getFirstChild().getNodeValue());*/
	        }
	        FileOutputStream flt = new FileOutputStream
	        (new File("D:\\FNA\\FNAV19\\target\\name-tagged\\1.xml"));
	        OutputStreamWriter out = new OutputStreamWriter(flt);
	        Source source = new DOMSource(doc);
	        StreamResult result = new StreamResult(out);
	        tFormer.transform(source, result); 
		} catch (Exception exe){
			exe.printStackTrace();
		}

	}

}
