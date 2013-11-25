 /* $Id: Tree2XML.java 971 2011-09-13 18:32:55Z hong1.cui $ */
package fna.charactermarkup;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;


/**
 * @author hongcui
 *
 */

public class Tree2XML {
    private String test=null;
    //private String str = "";
    public static ArrayList<String> adverbs = new ArrayList<String>();
    //private static PrintWriter out; 
    /**
     * 
     */
    public Tree2XML(String test) {
        // TODO Auto-generated constructor stub
        this.test = test.replaceAll("\\(\\s*\\)", "");
    }
    
    public Document xml() throws Exception{
        if(test==null || test.trim().length()==0){
            return null;
        }
        /*
        //out.println();
        //out.println(test);

        //step 1: turn all ( to <
        test = test.replaceAll("\\(", "<");
        //System.out.println(test);
        //step 2: turn those ) that are after a <, but without another < in between, to />. Regexp: <[^<]*?\)
        Pattern p = Pattern.compile("(.*?<[^<]*?)(\\))(.*)");
        Matcher m = p.matcher(test);
        while(m.matches()){
            xml += m.group(1)+"/>";
            test = m.group(3);
            m = p.matcher(test);
        }
        xml+=test;
        //System.out.println(xml);
        //step 3: process remaining ) one by one
        p = Pattern.compile("(.*?\\))(.*)");
        m = p.matcher(xml);
        while(m.matches()){
            String part = m.group(1);
            part = process(part);
            xml = part+m.group(2);
           // System.out.println(xml);
            m = p.matcher(xml);
        }
        */
        String xml = "";
		try {
	         xml = format(test);
	        //System.out.println(xml);
			Document doc =null;
			SAXBuilder builder = new SAXBuilder();
			ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
			doc = builder.build(bais);
			return doc;
		} catch (Exception e) {
	      e.printStackTrace();
	      System.out.print("Problem parsing the xml: \n" + xml+"\n"+e.toString());
	      throw e;
		}
		//return doc;
    }
    
    /*
     * @param parsed: (NP (JJ subulate) (NNS enations))
     */
    private String format(String parsed) throws Exception{
    	try{
    	int count = 0;
    	String t;
    	StringBuffer xml = new StringBuffer();
    	parsed = parsed.replaceAll("\\)", ") ").replaceAll("\\s+", " ").trim();//(NP (JJ subulate) (NNS enations) )
        parsed = parsed.replaceAll("``", "JJ").replaceAll("\\(-LRB-", "(PUNCT").replaceAll("\\([^A-Z/ ]+", "(PUNCT");
       	parsed = parsed.replaceAll("(?<=\\([A-Z]{1,8}) (?!\\()", "_");//(NP (JJ_subulate) (NNS_enations))
    	
    	ArrayList<String> tokens = new ArrayList<String>(Arrays.asList(parsed.split("\\s+"))); 
    	for(int i = 0; i < tokens.size(); i++){
    		String token = tokens.get(i);
    		if(token.matches("\\)")){//now i-1 should have the start tag that matches this ")"
    			xml.append(tokens.get(i-1).replaceFirst("\\(", "</")+">");
    			tokens.remove(i-1);
    			tokens.remove(i-1);
    			i=i-2;
    		}else if(token.endsWith(")")){//(JJ_subulate)
    			token = token.replaceAll("[()]", "");
    			String tag = token.substring(0, token.indexOf("_"));
    			String text = token.substring(token.indexOf("_")+1);
    			xml.append("<"+tag+" id='"+count+"' text='"+text+"'/>");//<JJ text="subulate"/>
    			count++;
    			tokens.remove(i);
    			i--;
    		}else{//(NP
    			String tag = token.replaceFirst("\\(", "<").trim()+">";//<NP>
    			xml.append(tag); //keep this token
    		}
    	}    	
    	if(tokens.size()>0){
    		System.err.println("error reading xml");
    		System.exit(2);
    	}
       	return xml.toString();
    	}catch (Exception e){
    		e.printStackTrace();
    		throw e;
    	}
    }
    /**
     * <NN Heads/> will become
     * <NN text="Heads"/>
     * @param xml
     */
    /*private String format(String xml) {
        // TODO Auto-generated method stub
        String r = "";
        int count = 0;
        xml = xml.replaceAll("``", "JJ").replaceAll("<[^A-Z/ ]+", "<PUNCT");
        Pattern p = Pattern.compile("(.*?)<([^<]*?) ([^<]*?)/>(.*)");
        Matcher m = p.matcher(xml);
        while(m.matches()){
            r += m.group(1);
            r +="<"+m.group(2)+" id=\""+count+"\" text=\""+m.group(3)+"\"/>";
            xml = m.group(4);
            
            if(m.group(2).compareToIgnoreCase("RB")==0 && !this.adverbs.contains(m.group(3)) && !m.group(3).matches("\\b("+ChunkedSentence.prepositions+")\\b")){
            	this.adverbs.add(m.group(3));
            }
            m = p.matcher(xml);
            count++;
            
            
        }
        r +=xml;
        return r;
    }*/

    /**
     * 
     * @param part looks like: 
     * a) <S    <NP      <NP <NN Heads/> <JJ many/>)     or 
     * b) <S    <NP      <NP> <NN Heads/> <JJ many/> </NP> )
     * @return: 
     * a) <S    <NP      <NP> <NN Heads/> <JJ many/> </NP> or
     * b) <S    <NP>      <NP> <NN Heads/> <JJ many/> </NP> </NP> 
     */
    /*
    private String process(String part) {
        String result = "";
        part = part.trim().replaceFirst("\\)$", "").replaceAll("\\s+", " ").trim();
        String cp = part;
        
        part = part.replaceAll("<[^<]*?/>", "");
        Pattern p = Pattern.compile("(.*?)<([A-Z]+)>\\s*</\\2>(.*)");
        Matcher m = p.matcher(part);
        while(m.matches()){
            part = m.group(1)+m.group(3);
            m = p.matcher(part);
        }
        part = part.trim();
        if(part.lastIndexOf("<") < 0){
        	return cp;
        }
        String tag = part.substring(part.lastIndexOf("<"));
        cp = cp.replaceAll(tag+"( |$)", tag+"* ");
        int index = cp.lastIndexOf('*');
        result = cp.substring(0, index)+">"+cp.substring(index+1)+"</"+tag.replaceFirst("<","")+">";
        result = result.replaceAll("\\*", "");        
        return result;
    }
	*/
 
    
 /*   private void processxml(Document root) {
    	str = "";
    	NodeList noun = root.getElementsByTagName("NP");
    	if( noun.getLength() != 0){
    		Node node = noun.item(0);
			if (node.hasChildNodes()){
				processchildnn(node);
			}
			else{
				if (node.getAttributes()!= null){
					str = str + node.getAttributes().getNamedItem("text").getNodeValue()+" ";
				}
			}
    	}
    	NodeList verbid = root.getElementsByTagName("VP");
		if( verbid.getLength() != 0){
			System.out.println(verbid.getLength());
			out.print(str+" / ");
			//System.out.println(verbid.item(0).getFirstChild().getTextContent());
			for(int i = 0; i < verbid.getLength(); i++){
				Node node = verbid.item(i);
				if (node.hasChildNodes()){
					processchilds(node);
					out.print(" / ");
				}
				else{
					if (node.getAttributes()!= null){
						out.println(node.getAttributes().getNamedItem("text").getNodeValue());
					}
				}
			}
		}
    }
    
    private void processchildnn(Node node) {
    	
		NodeList childid = node.getChildNodes();
		for(int j = 0; j < childid.getLength(); j++){
			Node nodes = childid.item(j);
			if (nodes.hasChildNodes())
				processchildnn(nodes);
			else{
				if (nodes.getAttributes()!= null && nodes.getNodeName() == "NN"){
					str = str + nodes.getAttributes().getNamedItem("text").getNodeValue()+" ";
				}
			}
		}
		
    }
    
    private void processchilds(Node node) {
    	
			NodeList childid = node.getChildNodes();
			for(int j = 0; j < childid.getLength(); j++){
				Node nodes = childid.item(j);
				if (nodes.hasChildNodes())
					processchilds(nodes);
				else{
					if (nodes.getAttributes()!= null){
						out.print(nodes.getAttributes().getNamedItem("text").getNodeValue()+" ");
					}
				}
			}
			
    }*/
		    
    /**
     * @param args
     */
    public static void main(String[] args) {
    }

}
