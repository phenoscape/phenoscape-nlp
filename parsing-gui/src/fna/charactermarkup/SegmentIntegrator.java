 /* $Id: SegmentIntegrator.java 827 2011-06-05 03:36:57Z hong1.cui $ */
package fna.charactermarkup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.parsing.ParsingUtil;

@SuppressWarnings("unchecked")
public class SegmentIntegrator {
	static protected Connection conn = null;
	static protected String database = null;
	static protected String username = "root";
	static protected String password = "root";
	private static String glosstable;
	private String lifestyle = null;
	private String projectfolder = null;
	
	
	public SegmentIntegrator(String database, String projectfolder) {
		// TODO Auto-generated constructor stub
		SegmentIntegrator.database = database;
		if(database.endsWith("fna")){
			SegmentIntegrator.glosstable = "fnaglossaryfixed";
		}else if(database.endsWith("treatise")){
			SegmentIntegrator.glosstable = "treatisehglossaryfixed";
		} 
		this.projectfolder = projectfolder;
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
		try{
			//collect life_style terms
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select distinct term from "+SegmentIntegrator.glosstable+" where category='life_style'");
			while(rs.next()){
				this.lifestyle += rs.getString(1)+"|";
			}
			this.lifestyle = lifestyle.replaceFirst("\\|$", "");
		}catch(Exception e){
			e.printStackTrace();
		}
		collect(database);
		
	}
	
	protected void collect(String database){
		integrator();
		//randomdesc();
		filecopier();
		/*try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				integrator();
				//randomdesc();
				filecopier();
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}*/
	}
	
	private void filecopier() {
		try{
			File ansdirectory = new File(this.projectfolder+"\\AnsKey_Benchmark_selected_sentence");
			String ansfilename[] = ansdirectory.list();
			for (int i = 0; i < ansfilename.length; i++) {
				String s = "";
				String str = "";
				File f = new File(this.projectfolder+"\\TestCase_Benchmark_sentence\\"+ansfilename[i]);
				if(!f.exists()){
					System.err.println(f.getName()+" does not exist");
				}else{
					FileInputStream istream = new FileInputStream(f);
					InputStreamReader inread = new InputStreamReader(istream);
					BufferedReader buff = new BufferedReader(inread);
					while((s = buff.readLine())!=null){
						str = str.concat(s);
					}
					FileOutputStream ostream = new FileOutputStream(this.projectfolder+"\\TestCase_Benchmark_selected_sentence\\"+ansfilename[i]); 
					PrintStream out = new PrintStream( ostream ); 
					out.print(str);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/*
	@SuppressWarnings("unused")
	private void randomdesc() {
		try{
			Statement stmt = conn.createStatement();
			ArrayList<String> desc = new ArrayList();
			ArrayList<String> randdesc = new ArrayList();
			Random r = new Random();
			//Runtime run = Runtime.getRuntime();
			int ct = 0;
			ResultSet rs = stmt.executeQuery("select * from markedsentence");
        	while(rs.next()){
        		String src = rs.getString("source");
        		src = src.substring(0, src.indexOf('-'));
        		if(!desc.contains(src)){
        			desc.add(src);
        			ct++;
        		}
        	}
        	System.out.println(ct);
        	ct = 0;
        	for(int i = 0; i < desc.size(); i++){
        		if (r.nextFloat()<.10){
        			randdesc.add(desc.get(i));
        			ct++;
        		}
        	}
        	System.out.println(ct);
        	for(int i = 0; i < randdesc.size(); i++){
        		rs = stmt.executeQuery("select * from markedsentence where source LIKE '"+randdesc.get(i)+"%'");
        		while(rs.next()){
        			String s = "";
        			String str = "";
        			FileInputStream istream = new FileInputStream("C:\\RAMU_BACKUP\\ACADEMICS-F\\UA\\RA\\TestCase_Treatise_sentence\\"+rs.getString("source")+".xml");
        			InputStreamReader inread = new InputStreamReader(istream);
        			BufferedReader buff = new BufferedReader(inread);
        			while((s = buff.readLine())!=null){
        				str = str.concat(s);
        			}
        			FileOutputStream ostream = new FileOutputStream("C:\\RAMU_BACKUP\\ACADEMICS-F\\UA\\RA\\TestCase_Treatise_selected_sentence\\"+rs.getString("source")+".xml"); 
        			PrintStream out = new PrintStream( ostream ); 
        			out.print(str);
        		}
        	}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	*/

	private void integrator() {
		try{
			Statement stmt = conn.createStatement();
			String[] sentid = new String[30];
			String newsrcstr = "", oldsrcstr = "";
			int srcflag = 0, ct = 0;
			ResultSet rs = stmt.executeQuery("select * from marked_simpleseg");
        	while(rs.next()){
        		newsrcstr=rs.getString("source");
        		if(oldsrcstr.compareTo(newsrcstr)==0)
        			srcflag = 1;
        		else
        			srcflag = 0;
        		if(srcflag == 1){
        			sentid[ct] = rs.getString("sentid");
        			ct++;
        		}
        		else{
        			if(rs.getString("sentid").compareTo("1")==0){
        				sentid[ct] = rs.getString("sentid");
        				ct++;
        			}
        			else{
        				XMLintegrator(sentid, ct, oldsrcstr);
        				for(int i = 0; i < sentid.length; i++)
        					sentid[i] = "";
        				ct = 0;
        				sentid[ct] = rs.getString("sentid");
        				ct++;
        			}
        			
        		}
        		oldsrcstr=rs.getString("source");
        	}
        	XMLintegrator(sentid, ct, oldsrcstr);
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	private void XMLintegrator(String[] sentid, int ct, String source) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document testcase = builder.build(this.projectfolder+"\\TestCase_Benchmark\\"+sentid[0]+".xml");
			Element testroot = testcase.getRootElement();
			for(int i = 1; i < ct; i++){
				System.out.println(sentid[i]);
				Document next = builder.build(this.projectfolder+"\\TestCase_Benchmark\\"+sentid[i]+".xml");
				Element nextroot = next.getRootElement();
				List<Element> testli = testroot.getChildren("structure");
				List<Element> nextli = nextroot.getChildren("structure");
				List<Element> testrel = testroot.getChildren("relation");
				int flag = 0;
				for(int j = 0; j < testli.size(); j++){ 
					Element nextele = (Element)nextli.get(0);
					Element testele = (Element)testli.get(j);
					if(nextele.getAttributeValue("name").compareTo(testele.getAttributeValue("name"))==0){
						if(nextele.getAttribute("constraint")!=null && testele.getAttribute("constraint")!=null && nextele.getAttributeValue("constraint").compareTo(testele.getAttributeValue("constraint"))==0){
							flag = 1;
							testele.addContent(nextele.cloneContent());
							String id = testele.getAttributeValue("id");
							List<Element> nextrel = nextroot.getChildren("relation");
							for(int k = 0; k < nextrel.size(); k++){
								Element nextrelele = (Element)nextrel.get(k);
								if(nextrelele.getAttributeValue("from").compareTo("o1")==0){
									nextrelele.setAttribute("from", id);
								}
							}
							List<Element> nextlistruct = nextroot.getChildren("structure");
							for(int l = 1; l < nextlistruct.size(); l++){
								Element nextstructele = (Element)nextlistruct.get(l);
								nextstructele.setAttribute("id", "o"+(l+testli.size()));
								List<Element> nextlirel = nextroot.getChildren("relation");
								for(int m = 0; m < nextlirel.size(); m++){
									Element nextlirelele = (Element)nextlirel.get(m);
									if(nextlirelele.getAttributeValue("from").compareTo("o"+(l+1))==0){
										nextlirelele.setAttribute("from", "o"+(l+testli.size()));
									}
									else if(nextlirelele.getAttributeValue("to").compareTo("o"+(l+1))==0){
										nextlirelele.setAttribute("to", "o"+(l+testli.size()));
									}	
								}
							}
							nextrel = nextroot.getChildren("relation");
							for(int n = 0; n < nextrel.size(); n++){
									Element nextrelele = (Element)nextrel.get(n);
								nextrelele.setAttribute("id", "R"+(n+1+testrel.size()));
							}
							nextroot.removeChild("structure");
							testroot.addContent(nextroot.cloneContent());
							break;
						}
						else if(nextele.getAttribute("constraint")==null && testele.getAttribute("constraint")==null){
							flag = 1;
							testele.addContent(nextele.cloneContent());
							String id = testele.getAttributeValue("id");
							List<Element> nextrel = nextroot.getChildren("relation");
							for(int k = 0; k < nextrel.size(); k++){
								Element nextrelele = (Element)nextrel.get(k);
								if(nextrelele.getAttributeValue("from").compareTo("o1")==0){
									nextrelele.setAttribute("from", id);
								}
							}
							List<Element> nextlistruct = nextroot.getChildren("structure");
							for(int l = 1; l < nextlistruct.size(); l++){
								Element nextstructele = (Element)nextlistruct.get(l);
								nextstructele.setAttribute("id", "o"+(l+testli.size()));
								List<Element> nextlirel = nextroot.getChildren("relation");
								for(int m = 0; m < nextlirel.size(); m++){
									Element nextlirelele = (Element)nextlirel.get(m);
									if(nextlirelele.getAttributeValue("from").compareTo("o"+(l+1))==0){
										nextlirelele.setAttribute("from", "o"+(l+testli.size()));
									}
									else if(nextlirelele.getAttributeValue("to").compareTo("o"+(l+1))==0){
										nextlirelele.setAttribute("to", "o"+(l+testli.size()));
									}	
								}
							}
							nextrel = nextroot.getChildren("relation");
							for(int n = 0; n < nextrel.size(); n++){
								Element nextrelele = (Element)nextrel.get(n);
								nextrelele.setAttribute("id", "R"+(n+1+testrel.size()));
							}
							nextroot.removeChild("structure");
							testroot.addContent(nextroot.cloneContent());
							break;
						}
					}
				}
				if(flag == 0){
					List<Element> nextlistruct = nextroot.getChildren("structure");
					for(int l = 0; l < nextlistruct.size(); l++){
						Element nextstructele = (Element)nextlistruct.get(l);
						nextstructele.setAttribute("id", "o"+(l+1+testli.size()));
						List<Element> nextlirel = nextroot.getChildren("relation");
						for(int m = 0; m < nextlirel.size(); m++){
							Element nextlirelele = (Element)nextlirel.get(m);
							if(nextlirelele.getAttributeValue("from").compareTo("o"+(l+1))==0){
								nextlirelele.setAttribute("from", "o"+(l+1+testli.size()));
							}
							else if(nextlirelele.getAttributeValue("to").compareTo("o"+(l+1))==0){
								nextlirelele.setAttribute("to", "o"+(l+1+testli.size()));
							}	
						}
					}
					List<Element> nextrel = nextroot.getChildren("relation");
					for(int n = 0; n < nextrel.size(); n++){
						Element nextrelele = (Element)nextrel.get(n);
						nextrelele.setAttribute("id", "R"+(n+1+testrel.size()));
					}
					testroot.addContent(nextroot.cloneContent());
				}
			}
			testroot.detach();
			testroot = lifeStyle(testroot);
			File f = new File(this.projectfolder+"\\TestCase_Benchmark_sentence\\"+source+".xml");
			ParsingUtil.outputXML(testroot, f, null);
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * re-annotate "trees" from structure to character lifestyle
	 */
	private Element lifeStyle(Element statement) {
		try{			
			//find life_style structures
			List<Element> structures = XPath.selectNodes(statement, ".//structure");
			Iterator<Element> it = structures.iterator();
			while(it.hasNext()){
				Element structure = it.next();
				String name = structure.getAttributeValue("name").trim();
				if(name.length()<=0) continue;
				if(lifestyle.matches(".*\\b"+name+"\\b.*")){
					if(structure.getAttribute("constraint") !=null)
						name = structure.getAttributeValue("constraint")+" "+name;
					//if(structure.getAttribute("constraint_parent_organ") !=null)
					//	name = structure.getAttributeValue("constraint_parent_organ")+" "+name;
					Element wo = (Element)XPath.selectSingleNode(statement, ".//structure[@name='whole organism']");
					if(wo!=null){
						List<Element> content = structure.getContent();
						structure.removeContent();
						/*for(int i = 0; i<content.size(); i++){
							Element e = content.get(i);
							e.detach();
							content.set(i, e);
						}*/
						wo.addContent(content);
						structure.detach();
						structure = wo;
					}
					structure.setAttribute("name", "whole organism");
					Element ch = new Element("character");
					ch.setAttribute("name", "life_style");
					ch.setAttribute("value", name);
					structure.addContent(ch);
				}
				//keep each life_style structure
				/*if(lifestyle.matches(".*\\b"+name+"\\b.*")){
					if(structure.getAttribute("constraint") !=null)
						name = structure.getAttributeValue("constraint")+" "+name;
					structure.setAttribute("name", "whole organism");
					Element ch = new Element("character");
					ch.setAttribute("name", "life_style");
					ch.setAttribute("value", name);
					structure.addContent(ch);
				}*/
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return statement;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//new SegmentIntegrator("benchmark_learningcurve_treatiseh_test_19");
		new SegmentIntegrator("annotationevaluation_heuristics", "");
	}

}
