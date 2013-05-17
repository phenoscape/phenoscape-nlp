 /* $Id: CompareXML.java 882 2011-07-18 04:46:48Z hong1.cui $ */
package fna.charactermarkup;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
@SuppressWarnings("unused")
public class CompareXML {

	protected static Connection conn = null;
	protected static String database = null;
	protected static String username = "termsuser";
	protected static String password = "termspassword";
	private static int structexactmatch = 0;
	private static int structpartmatch = 0;
	private static int structnomatch = 0;
	private static int structperfmatch = 0;
	private static int charexactmatch = 0;
	private static int charpartmatch = 0;
	private static int charnomatch = 0;
	private static int charperfmatch = 0;
	private static int relexactmatch = 0;
	private static int relpartmatch = 0;
	private static int relnomatch = 0;
	private static int relperfmatch = 0;
	private static int totmachinest = 0;
	private static int totmachinech = 0;
	private static int totmachinerel = 0;
	private static int tothumanst = 0;
	private static int tothumanch = 0;
	private static int tothumanrel = 0;
	private static int dscstructexactmatch = 0;
	private static int dscstructpartmatch = 0;
	private static int dscstructnomatch = 0;
	private static int dscstructperfmatch = 0;
	private static int dsccharexactmatch = 0;
	private static int dsccharpartmatch = 0;
	private static int dsccharnomatch = 0;
	private static int dsccharperfmatch = 0;
	private static int dscrelexactmatch = 0;
	private static int dscrelpartmatch = 0;
	private static int dscrelnomatch = 0;
	private static int dscrelperfmatch = 0;
	private static int dsctotmachinest = 0;
	private static int dsctotmachinech = 0;
	private static int dsctotmachinerel = 0;
	private static int dsctothumanst = 0;
	private static int dsctothumanch = 0;
	private static int dsctothumanrel = 0;
	private static int totalsize = 0;
	private static int totalstructures = 0;
	private static int totalcharacters = 0;
	private static int totalrelations = 0;
	private static int total = 0;
	private static int totalsegments = 0;
	
	
	private String projectfolder;
	
	public void collect(String database){
		
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				stmt.execute("drop table if exists precisionrecall");
				stmt.execute("create table if not exists precisionrecall (source varchar(100) NOT NULL, length Float(5,2),segcount Float(5,2),strcount Float(5,2),chcount Float(5,2),relcount Float(5,2), pperfst Float(5,2), pexactst Float(5,2), ppartialst Float(5,2), preasonst Float(5,2), " +
						"pperfch Float(5,2), pexactch Float(5,2), ppartialch Float(5,2), preasonch Float(5,2), pperfrel Float(5,2), pexactrel Float(5,2), ppartialrel Float(5,2), preasonrel Float(5,2), " +
						"rperfst Float(5,2), rexactst Float(5,2), rpartialst Float(5,2), rreasonst Float(5,2), rperfch Float(5,2), rexactch Float(5,2), rpartialch Float(5,2), rreasonch Float(5,2), " +
						"rperfrel Float(5,2), rexactrel Float(5,2), rpartialrel Float(5,2), rreasonrel Float(5,2), sentprecisionperf Float(5,2),sentrecallperf Float(5,2), sentprecisionreas Float(5,2),sentrecallreas Float(5,2), PRIMARY KEY(source))");
				//stmt.execute("delete from precisionrecall");
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public CompareXML(String database, String projectfolder) {
		CompareXML.database = database;
		this.projectfolder = projectfolder;
		try{
			
			//Pass the database whose sentence are to  be evaluated
			collect(database);
			
			//URL of folder containing the human annotated files 
			File ansdirectory = new File(this.projectfolder+"\\AnsKey_Benchmark_selected_sentence");
			String ansfilename[] = ansdirectory.list();
			
			//URL of folder containing the machine generated files
			//File testdirectory = new File(this.projectfolder+"\\TestCase_Benchmark_selected_sentence");
			File testdirectory = new File(this.projectfolder+"\\UnsupervisedStanford_Benchmark_selected_sentence");
			String testfilename[] = testdirectory.list();
			total = testfilename.length;
			for (int i = 0; i < ansfilename.length; i++) {
				for (int j = 0; j < testfilename.length; j++) {
					if(ansfilename[i].compareTo(testfilename[j])==0){
						SAXBuilder builder = new SAXBuilder();
						System.out.println(ansfilename[i]);
						Document anskey = builder.build(ansdirectory.getAbsolutePath()+"\\"+ansfilename[i]);
						Element ansroot = anskey.getRootElement();
						ansroot = ansroot.getChild("statement");
						String text = ansroot.getChildText("text");
						int size = text.split("\\s+").length;
						int strcount = XPath.selectNodes(ansroot, ".//structure").size();
						int chcount = XPath.selectNodes(ansroot, ".//character").size();
						int relcount = XPath.selectNodes(ansroot, ".//relation").size();
						totalsize += size;
						totalstructures += strcount;
						totalcharacters += chcount;
						totalrelations +=relcount;
						
						
						Document testcase = builder.build(testdirectory.getAbsolutePath()+"\\"+testfilename[j]);
						Element testroot = testcase.getRootElement();
						int segcount = XPath.selectNodes(testroot, ".//text").size();
						totalsegments += segcount;
						
						structexactmatch = 0;
						structpartmatch = 0;
						structnomatch = 0;
						structperfmatch = 0;
						charexactmatch = 0;
						charpartmatch = 0;
						charnomatch = 0;
						charperfmatch = 0;
						relexactmatch = 0;
						relpartmatch = 0;
						relnomatch = 0;
						relperfmatch = 0;
						totmachinest = 0;
						totmachinech = 0;
						totmachinerel = 0;
						tothumanst = 0;
						tothumanch = 0;
						tothumanrel = 0;
						validatestruct(ansroot, testroot);
						validatecharacter(ansroot, testroot);
						validaterelation(ansroot, testroot);
						calcprecisionrecall(testfilename[j].substring(0, testfilename[j].lastIndexOf('.')), size, segcount, strcount, chcount, relcount);
						break;
					}
				}
			}
			
			dsccalcprecisionrecall();
			
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Performs comparison between the <structure> elements present in machine & human annotated sentence.  
	 * @param ansroot
	 * @param testroot
	 */	
	@SuppressWarnings("unchecked")
	public void validatestruct(Element ansroot, Element testroot) {
		String exact = "";
		String ansexact = "";
		List<Element> ansli = ansroot.getChildren("structure");
		List<Element> testli = testroot.getChildren("structure");
		totmachinest = testli.size();
		tothumanst = ansli.size();
		for(int i = 0; i < testli.size(); i++){
			Element test = (Element)testli.get(i);
			for(int j = 0; j < ansli.size(); j++){
				Element ans = (Element)ansli.get(j);
				if(!ansexact.contains(ans.getAttributeValue("id"))){
					if (test.getAttributeValue("name").compareTo(ans.getAttributeValue("name"))==0){
						if (test.getAttribute("constraint")!=null && ans.getAttribute("constraint")!=null){
							if (test.getAttributeValue("constraint").compareTo(ans.getAttributeValue("constraint"))==0){
								structexactmatch++;
								structperfmatch++;
								exact += test.getAttributeValue("id");
								ansexact += ans.getAttributeValue("id");
								break;
							}
						}
						else{
							if(test.getAttribute("constraint")==null && ans.getAttribute("constraint")==null){
								structexactmatch++;
								structperfmatch++;
								exact += test.getAttributeValue("id");
								ansexact += ans.getAttributeValue("id");
								break;
							}
						}
					}
				}
			}
		}
		for(int i = 0; i < testli.size(); i++){
			Element test = (Element)testli.get(i);
			if(!exact.contains(test.getAttributeValue("id"))){
				for(int j = 0; j < ansli.size(); j++){
					Element ans = (Element)ansli.get(j);
					if(!ansexact.contains(ans.getAttributeValue("id"))){
						if (test.getAttributeValue("name").compareTo(ans.getAttributeValue("name"))==0){
							//if (test.getAttribute("constraint")!=null && ans.getAttribute("constraint")!=null){
								//if (test.getAttributeValue("constraint").contains(ans.getAttributeValue("constraint"))|ans.getAttributeValue("constraint").contains(test.getAttributeValue("constraint"))){
									structpartmatch++;
									exact += test.getAttributeValue("id");
									ansexact += ans.getAttributeValue("id");
									break;
								//}
							//}
						}
					}
				}
			}
		}
		for(int i = 0; i < testli.size(); i++){
			Element test = (Element)testli.get(i);
			if(!exact.contains(test.getAttributeValue("id"))){
				for(int j = 0; j < ansli.size(); j++){
					Element ans = (Element)ansli.get(j);
					if(!ansexact.contains(ans.getAttributeValue("id"))){
						//Reason: if(ans.getAttributeValue("name").compareTo(test.getAttributeValue("name"))==0){
						if (ans.getAttributeValue("name").contains(test.getAttributeValue("name"))||test.getAttributeValue("name").contains(ans.getAttributeValue("name"))){
							//if (test.getAttribute("constraint")!=null && ans.getAttribute("constraint")!=null){
								//if (test.getAttributeValue("constraint").contains(ans.getAttributeValue("constraint"))||ans.getAttributeValue("constraint").contains(test.getAttributeValue("constraint"))){
									structpartmatch++;
									exact += test.getAttributeValue("id");
									ansexact += ans.getAttributeValue("id");
									break;
								//}
							//}
							//else{
							//	if(test.getAttribute("constraint")==null && ans.getAttribute("constraint")==null){
							//		structpartmatch++;
							//		exact += test.getAttributeValue("id");
							//		ansexact += ans.getAttributeValue("id");
							//		break;
							//	}
							//}
						}
					}
				}
			}
		}
	/*	System.out.println("structperfmatch:"+structperfmatch);
		System.out.println("structexactmatch:"+structexactmatch);
		System.out.println("structpartmatch:"+structpartmatch);
		System.out.println("structnomatch:"+structnomatch);*/
	}
	
	/**
	 * Performs comparison between the <character> elements present in machine & human annotated sentence.
	 * @param ansroot
	 * @param testroot
	 */
	@SuppressWarnings("unchecked")
	public void validatecharacter(Element ansroot, Element testroot) {
		String exact = "";
		String ansliexact = "";
		ArrayList<List<Element>> myansli = new ArrayList<List<Element>> ();
		ArrayList<List<Element>> mytestli = new ArrayList<List<Element>> ();
		List<Element> ansli = null, testli = null;
		List<Element> structansli = ansroot.getChildren("structure");
		List<Element> structtestli = testroot.getChildren("structure");
		for( Iterator<Element> l = structansli.iterator(); l.hasNext();){
			Element structans = (Element)l.next();
			if(!structans.getChildren().isEmpty()){
					ansli = structans.getChildren();
					myansli.add(ansli);
					tothumanch += ansli.size();
			}
		}
		for( Iterator<Element> l = structtestli.iterator(); l.hasNext();){
			Element structtest = (Element)l.next();
			if(!structtest.getChildren().isEmpty()){
					testli = structtest.getChildren();
					mytestli.add(testli);
					totmachinech += testli.size();
			}
		}

		if(myansli!=null && mytestli!=null){
			for(Iterator<List<Element>> tli = mytestli.iterator(); tli.hasNext();){
				testli = (List<Element>)tli.next();
				for(int i = 0; i < testli.size(); i++){
					int flag = 0;
					Element test = (Element)testli.get(i);
					for(Iterator<List<Element>> ali = myansli.iterator(); ali.hasNext();){
						int aliflag = 0;
						ansli = (List<Element>)ali.next();
						for(int j = 0; j < ansli.size(); j++){
							flag = 0;
							Element ans = (Element)ansli.get(j);
							//System.out.println(test.getParentElement().getAttributeValue("id"));
							//System.out.println(ans.getParentElement().getAttributeValue("id"));
							//find matching structure
							if (test.getParentElement().getAttributeValue("name").compareTo(ans.getParentElement().getAttributeValue("name"))==0){
								List<Attribute> testattr = test.getAttributes();
								List<Attribute> ansattr = ans.getAttributes(); //ans: a character element; ansattr: att/value pairs
								for(Iterator<Attribute> k = testattr.iterator(); k.hasNext();){
									Attribute a = (Attribute)k.next();
									//System.out.println(ansattr.toString());
									//System.out.println(a.toString());
									if(a.getName().compareTo("name")==0||a.getName().compareTo("value")==0||a.getName().compareTo("char_type")==0||a.getName().compareTo("modifier")==0||a.getName().compareTo("from")==0||a.getName().compareTo("to")==0||a.getName().compareTo("from_unit")==0||a.getName().compareTo("to_unit")==0||a.getName().compareTo("unit")==0){
										if(!ansattr.toString().contains(a.toString())){
											flag = 1; //1: not perfect match
											break;//?
										}
									}
								}
								if(flag == 0){
									charexactmatch++;
									if(test.getAttributes().size() == ans.getAttributes().size())
										charperfmatch++;
									exact += test.getAttributes().toString();
									ansliexact += ans.getAttributes().toString();
									aliflag = 1;
									break;
								}
							}
						}
						if(aliflag == 1)
							break;
					}
				}
			}
			
			for(Iterator<List<Element>> tli = mytestli.iterator(); tli.hasNext();){
				testli = (List<Element>)tli.next();			
				for(int i = 0; i < testli.size(); i++){
					int flag = 0;
					Element test = (Element)testli.get(i);
					if(!exact.contains(test.getAttributes().toString())){
						for(Iterator<List<Element>> ali = myansli.iterator(); ali.hasNext();){
							int aliflag = 0;
							ansli = (List<Element>)ali.next();
							for(int j = 0; j < ansli.size(); j++){
								int ansliflag = 0;
								Element ans = (Element)ansli.get(j);
								if(!ansliexact.contains(ans.getAttributes().toString())){
									if (test.getParentElement().getAttributeValue("name").compareTo(ans.getParentElement().getAttributeValue("name"))==0){
										List<Attribute> testattr = test.getAttributes();
										List<Attribute> ansattr = ans.getAttributes();//one character element
										int missattr = 0;
										boolean goodv = false;
										boolean goodf = false;
										boolean goodt = false;
										for(int k = 0; k < testattr.size(); k++){
											Attribute atest = (Attribute)testattr.get(k);
											if(atest.getName().compareTo("name")==0|atest.getName().compareTo("value")==0|atest.getName().compareTo("char_type")==0|atest.getName().compareTo("modifier")==0|atest.getName().compareTo("from")==0|atest.getName().compareTo("to")==0|atest.getName().compareTo("from_unit")==0|atest.getName().compareTo("to_unit")==0|atest.getName().compareTo("unit")==0){	
												missattr = 0;
												for(int l = 0; l < ansattr.size(); l++){
													Attribute aans = (Attribute)ansattr.get(l);
													if(atest.getName().toString().compareTo(aans.getName().toString())==0){
														missattr = 1;
														//Reason: if(aans.getValue().compareTo(atest.getValue())==0){
														if(aans.getValue().contains(atest.getValue())|atest.getValue().contains(aans.getValue())){
															if(atest.getName().compareTo("value")==0) goodv = true;
															if(atest.getName().compareTo("from")==0) goodf = true;
															if(atest.getName().compareTo("to")==0) goodt = true;
															break;//??
														}
														else{
															ansliflag = 1;
															break;
														}
													}
												}
												if(missattr == 0)
													break;
											}
											if(ansliflag == 1)
												break;
										}
										if((ansliflag == 0 && missattr == 1) || goodv || (goodf && goodt)){
											charpartmatch++;
											ansliexact += ans.getAttributes().toString();
											flag = 1;
											aliflag = 1;
											break;
										}
									}
								}
							}
							if (aliflag == 1){
								break;
							}
						}
						if (flag == 0){
							charnomatch++;
						}
					}
				}
			}
		/*	System.out.println("charperfmatch:"+charperfmatch);
			System.out.println("charexactmatch:"+charexactmatch);
			System.out.println("charpartmatch:"+charpartmatch);
			System.out.println("charnomatch:"+charnomatch);*/
		}
	}
	
	/**
	 * Performs comparison between the <relation> elements present in machine & human annotated sentence.
	 * @param ansroot
	 * @param testroot
	 */
	@SuppressWarnings("unchecked")
	public void validaterelation(Element ansroot, Element testroot) {
		String exact = "";
		List<Element> ansli = ansroot.getChildren("relation");
		List<Element> testli = testroot.getChildren("relation");
		totmachinerel = testli.size();
		tothumanrel = ansli.size();
		for(int i = 0; i < testli.size(); i++){
			int flag = 0;
			Element test = (Element)testli.get(i);
			for(int j = 0; j < ansli.size(); j++){
				flag = 0;
				Element ans = (Element)ansli.get(j);
				List<Attribute> testattr = test.getAttributes();
				List<Attribute> ansattr = ans.getAttributes();
				for(int k = 0; k < testattr.size(); k++){
					Attribute a = (Attribute)testattr.get(k);
					//System.out.println(ansattr.toString());
					//System.out.println(a.toString());
					if(a.getName().compareTo("name")==0|a.getName().compareTo("from")==0|a.getName().compareTo("to")==0|a.getName().compareTo("negation")==0){
						if(a.getName().compareTo("from")==0|a.getName().compareTo("to")==0){
							List<Element> ansstruct = ansroot.getChildren("structure");
							List<Element> teststruct = testroot.getChildren("structure");
							String teststname = "";
							for(int m = 0; m < teststruct.size(); m++){
								Element testst = (Element)teststruct.get(m);
								if(testst.getAttributeValue("id").compareTo(a.getValue())==0){
									teststname = testst.getAttributeValue("name");
									break;
								}
							}
							String aansval = "";
							for(int n = 0; n < ansattr.size(); n++){
								Attribute aans = (Attribute)ansattr.get(n); 
								if(aans.getName().compareTo(a.getName())==0){
									aansval = aans.getValue();
									break;
								}
							}
							String ansstname = "";
							for(int m = 0; m < ansstruct.size(); m++){
								Element ansst = (Element)ansstruct.get(m);
								if(ansst.getAttributeValue("id").compareTo(aansval)==0){
									ansstname = ansst.getAttributeValue("name");
									break;
								}
							}
							//Reason: if(teststname.compareTo(ansstname)!=0){
							if(!teststname.contains(ansstname) && !ansstname.contains(teststname)){
								flag = 1;
								break;
							}
						}
						else if(!ansattr.toString().contains(a.toString())){
							flag = 1;
							break;
						}
					}
				}
				if(flag == 0){
					relexactmatch++;
					if(test.getAttributes().size() == ans.getAttributes().size())
						relperfmatch++;
					exact += test.getAttributeValue("id");
					ansli.remove(j);
					break;
				}
			}
		}
		
		for(int i = 0; i < testli.size(); i++){
			Element test = (Element)testli.get(i);
			int testflag = 0;
			if(!exact.contains(test.getAttributeValue("id"))){
				for(int j = 0; j < ansli.size(); j++){
					int flag = 0;
					Element ans = (Element)ansli.get(j);
					List<Attribute> testattr = test.getAttributes();
					List<Attribute> ansattr = ans.getAttributes();
					for(int k = 0; k < testattr.size(); k++){
						Attribute atest = (Attribute)testattr.get(k);
						if(atest.getName().compareTo("name")==0|atest.getName().compareTo("from")==0|atest.getName().compareTo("to")==0|atest.getName().compareTo("negation")==0){	
							for(int l = 0; l < ansattr.size(); l++){
								Attribute aans = (Attribute)ansattr.get(l);
								if(atest.getName().toString().compareTo(aans.getName().toString())==0){
									if(atest.getName().compareTo("from")==0|atest.getName().compareTo("to")==0){
										List<Element> ansstruct = ansroot.getChildren("structure");
										List<Element> teststruct = testroot.getChildren("structure");
										String teststname = "";
										for(int m = 0; m < teststruct.size(); m++){
											Element testst = (Element)teststruct.get(m);
											if(testst.getAttributeValue("id").compareTo(atest.getValue())==0){
												teststname = testst.getAttributeValue("name");
												break;
											}
										}
										String aansval = "";
										for(int n = 0; n < ansattr.size(); n++){
											Attribute aansk = (Attribute)ansattr.get(n); 
											if(aansk.getName().compareTo(atest.getName())==0){
												aansval = aansk.getValue();
												break;
											}
										}
										String ansstname = "";
										for(int m = 0; m < ansstruct.size(); m++){
											Element ansst = (Element)ansstruct.get(m);
											if(ansst.getAttributeValue("id").compareTo(aansval)==0){
												ansstname = ansst.getAttributeValue("name");
												break;
											}
										}
										//Reason: if(teststname.compareTo(ansstname)==0){
										if(teststname.contains(ansstname)|ansstname.contains(teststname)){
											break;
										}
										else{
											flag = 1;
											break;
										}
									}else{
										//Reason: if(aans.getValue().compareTo(atest.getValue())==0){
										if(aans.getValue().contains(atest.getValue())|atest.getValue().contains(aans.getValue())){
											break;
										}
										else{
											flag = 1;
											break;
										}
									}
								}
							}
						}
						if(flag == 1){
							break;
						}
					}
					if(flag == 0){
						relpartmatch++;
						exact += test.getAttributeValue("id");
						ansli.remove(j);
						testflag = 1;
						break;
					}
				}
				if(testflag == 0){
					relnomatch++;
				}
			}
		}
	/*	System.out.println("relperfmatch:"+relperfmatch);
		System.out.println("relexactmatch:"+relexactmatch);
		System.out.println("relpartmatch:"+relpartmatch);
		System.out.println("relnomatch:"+relnomatch);*/
	}
	
	/**
	 * Performs Precision & Recall calculations for Perfect match, exact match, partial match, reasonable match
	 * @param source
	 */
	public void calcprecisionrecall(String source, int size, int segcount, int strcount, int chcount, int relcount){
		String q ="";
		try{
			float pperfst, pexactst, ppartialst, preasonst;
			float pperfch, pexactch, ppartialch, preasonch;
			float pperfrel, pexactrel, ppartialrel, preasonrel;
			float rperfst, rexactst, rpartialst, rreasonst;
			float rperfch, rexactch, rpartialch, rreasonch;
			float rperfrel, rexactrel, rpartialrel, rreasonrel;
			float sentprecisionperf, sentrecallperf, sentprecisionreas, sentrecallreas;
						
			Statement stmt = conn.createStatement();
			if(tothumanst!=0 && totmachinest==0){
				pperfst = 0;
				pexactst = 0;
				ppartialst = 0;
				preasonst = 0;				
			}else if(tothumanst==0){
				pperfst = -1;
				pexactst = -1;
				ppartialst = -1;
				preasonst = -1;				
			}else{
				pperfst = (float)structperfmatch/totmachinest;
				pexactst = (float)structexactmatch/totmachinest;
				ppartialst = (float)structpartmatch/totmachinest;
				preasonst = (float)(structexactmatch+structpartmatch)/totmachinest;
			}			
			
			if(tothumanch!=0 && totmachinech == 0){
				pperfch = 0;
				pexactch = 0;
				ppartialch = 0;
				preasonch = 0;
			}else if(tothumanch==0){
				pperfch = -1;
				pexactch = -1;
				ppartialch = -1;
				preasonch = -1;
			}else{
				pperfch = (float)charperfmatch/totmachinech;
				pexactch = (float)charexactmatch/totmachinech;
				ppartialch = (float)charpartmatch/totmachinech;
				preasonch = (float)(charexactmatch+charpartmatch)/totmachinech;
			}
			
			if(tothumanrel != 0 && totmachinerel == 0){
				pperfrel = 0;
				pexactrel = 0;
				ppartialrel = 0;
				preasonrel = 0;
			}else if(tothumanrel==0){
				pperfrel = -1;
				pexactrel = -1;
				ppartialrel = -1;
				preasonrel = -1;
			}else{
				pperfrel = (float)relperfmatch/totmachinerel;
				pexactrel = (float)relexactmatch/totmachinerel;
				ppartialrel = (float)relpartmatch/totmachinerel;
				preasonrel = (float)(relexactmatch+relpartmatch)/totmachinerel;
			}
			
			
			
			 if(tothumanst==0){
				rperfst = -1;
				rexactst = -1;
				rpartialst = -1;
				rreasonst = -1;				
			}else{
				rperfst = (float)structperfmatch/tothumanst;
				rexactst = (float)structexactmatch/tothumanst;
				rpartialst = (float)structpartmatch/tothumanst;
				rreasonst = (float)(structexactmatch+structpartmatch)/tothumanst;
			}
			if(tothumanch == 0){
				rperfch = -1;
				rexactch = -1;
				rpartialch = -1;
				rreasonch = -1;
			}else{
				rperfch = (float)charperfmatch/tothumanch;
				rexactch = (float)charexactmatch/tothumanch;
				rpartialch = (float)charpartmatch/tothumanch;
				rreasonch = (float)(charexactmatch+charpartmatch)/tothumanch;
			}
			
			if(tothumanrel == 0){
				rperfrel = -1;
				rexactrel = -1;
				rpartialrel = -1;
				rreasonrel = -1;
			}else{
				rperfrel = (float)relperfmatch/tothumanrel;
				rexactrel = (float)relexactmatch/tothumanrel;
				rpartialrel = (float)relpartmatch/tothumanrel;
				rreasonrel = (float)(relexactmatch+relpartmatch)/tothumanrel;
			}
	
			//sentprecision = (float)(structexactmatch+structpartmatch+charexactmatch+charpartmatch+relexactmatch+relpartmatch)/(totmachinest+totmachinech+totmachinerel);
			//sentrecall = (float)(structexactmatch+structpartmatch+charexactmatch+charpartmatch+relexactmatch+relpartmatch)/(tothumanst+tothumanch+tothumanrel);
			if((tothumanst+tothumanch+tothumanrel)==0){
				sentprecisionreas = -1;
				sentprecisionperf = -1;
			}else if((totmachinest+totmachinech+totmachinerel)==0 && (tothumanst+tothumanch+tothumanrel)!=0){
				sentprecisionreas = 0;
				sentprecisionperf = 0;
			}else{
				sentprecisionreas = (float)(structexactmatch+structpartmatch+charexactmatch+charpartmatch+relexactmatch+relpartmatch)/(totmachinest+totmachinech+totmachinerel);
				sentprecisionperf = (float)(structperfmatch+charperfmatch+relperfmatch)/(totmachinest+totmachinech+totmachinerel);
			}
			if((tothumanst+tothumanch+tothumanrel)==0){
				sentrecallreas = -1;
				sentrecallperf = -1;
			}else{
				sentrecallreas = (float)(structexactmatch+structpartmatch+charexactmatch+charpartmatch+relexactmatch+relpartmatch)/(tothumanst+tothumanch+tothumanrel);
				sentrecallperf = (float)(structperfmatch+charperfmatch+relperfmatch)/(tothumanst+tothumanch+tothumanrel);
			}
			
			

			q = "insert into precisionrecall values('"+source+"',"+size+","+segcount+","+strcount+","+chcount+","+relcount+",'"+pperfst+"','"+pexactst+"','"+ppartialst+"','"+preasonst+"','"+pperfch+"','"+pexactch+"','"+ppartialch+"','"+preasonch+"','"+pperfrel+"','"+pexactrel+"','"+ppartialrel+"','"+preasonrel+"'," +
			"'"+rperfst+"','"+rexactst+"','"+rpartialst+"','"+rreasonst+"','"+rperfch+"','"+rexactch+"','"+rpartialch+"','"+rreasonch+"','"+rperfrel+"','"+rexactrel+"','"+rpartialrel+"','"+rreasonrel+"','"+sentprecisionperf+"','"+sentrecallperf+"','"+sentprecisionreas+"','"+sentrecallreas+"')";
			stmt.execute(q);
			
			dscstructexactmatch += structexactmatch;
			dscstructpartmatch += structpartmatch;
			dscstructnomatch += structnomatch;
			dscstructperfmatch += structperfmatch;
			dsccharexactmatch += charexactmatch;
			dsccharpartmatch += charpartmatch;
			dsccharnomatch += charnomatch;
			dsccharperfmatch += charperfmatch;
			dscrelexactmatch += relexactmatch;
			dscrelpartmatch += relpartmatch;
			dscrelnomatch += relnomatch;
			dscrelperfmatch += relperfmatch;
			dsctotmachinest += totmachinest;
			dsctotmachinech += totmachinech;
			dsctotmachinerel += totmachinerel;
			dsctothumanst += tothumanst;
			dsctothumanch += tothumanch;
			dsctothumanrel += tothumanrel;
			
		}catch(Exception e){
			System.out.println(q);
			e.printStackTrace();
		}
	}

	/**
	 * Performs Precision & Recall calculations for Perfect match, exact match, partial match, reasonable match
	 * of the entire description
	 * @param source
	 */
	public void dsccalcprecisionrecall(){
		try{
			Statement stmt = conn.createStatement();
			
			float dscpperfst, dscpexactst, dscppartialst, dscpreasonst;
			float dscpperfch, dscpexactch, dscppartialch, dscpreasonch;
			float dscpperfrel, dscpexactrel, dscppartialrel, dscpreasonrel;
			float dscrperfst, dscrexactst, dscrpartialst, dscrreasonst;
			float dscrperfch, dscrexactch, dscrpartialch, dscrreasonch;
			float dscrperfrel, dscrexactrel, dscrpartialrel, dscrreasonrel;
			
			dscpperfst = (float)dscstructperfmatch/dsctotmachinest;
			dscpexactst = (float)dscstructexactmatch/dsctotmachinest;
			dscppartialst = (float)dscstructpartmatch/dsctotmachinest;
			dscpreasonst = (float)(dscstructexactmatch+dscstructpartmatch)/dsctotmachinest;
						
			dscpperfch = (float)dsccharperfmatch/dsctotmachinech;
			dscpexactch = (float)dsccharexactmatch/dsctotmachinech;
			dscppartialch = (float)dsccharpartmatch/dsctotmachinech;
			dscpreasonch = (float)(dsccharexactmatch+dsccharpartmatch)/dsctotmachinech;
			
			dscpperfrel = (float)dscrelperfmatch/dsctotmachinerel;
			dscpexactrel = (float)dscrelexactmatch/dsctotmachinerel;
			dscppartialrel = (float)dscrelpartmatch/dsctotmachinerel;
			dscpreasonrel = (float)(dscrelexactmatch+dscrelpartmatch)/dsctotmachinerel;
			
			dscrperfst = (float)dscstructperfmatch/dsctothumanst;
			dscrexactst = (float)dscstructexactmatch/dsctothumanst;
			dscrpartialst = (float)dscstructpartmatch/dsctothumanst;
			dscrreasonst = (float)(dscstructexactmatch+dscstructpartmatch)/dsctothumanst;
			
			dscrperfch = (float)dsccharperfmatch/dsctothumanch;
			dscrexactch = (float)dsccharexactmatch/dsctothumanch;
			dscrpartialch = (float)dsccharpartmatch/dsctothumanch;
			dscrreasonch = (float)(dsccharexactmatch+dsccharpartmatch)/dsctothumanch;
			
			dscrperfrel = (float)dscrelperfmatch/dsctothumanrel;
			dscrexactrel = (float)dscrelexactmatch/dsctothumanrel;
			dscrpartialrel = (float)dscrelpartmatch/dsctothumanrel;
			dscrreasonrel = (float)(dscrelexactmatch+dscrelpartmatch)/dsctothumanrel;
			float dessize = (float)totalsize /total;
			float desstr = (float) totalstructures/total;
			float desch = (float) totalcharacters/total;
			float desrel = (float)totalrelations/total;
			float desseg = (float) totalsegments/total;
			
			System.out.println( "dscpperfst "+dscpperfst+"\n"+"dscpexactst "+dscpexactst+"\n"+"dscppatialst "+ dscppartialst+"\n"+"dscpreasonst "+dscpreasonst+"\n"+
					"dscpperfch "+dscpperfch+"\n"+"dscpexactch "+ dscpexactch+"\n"+"dscppartialch "+ dscppartialch+"\n"+"dscpreasonch "+ dscpreasonch+"\n"+
					"dscpperfrel "+dscpperfrel+"\n"+"dscpexactrel "+ dscpexactrel+"\n"+"dscppartialrel "+dscppartialrel+"\n"+"dscpreasonrel "+ dscpreasonrel+"\n"+
					"dscrperfst "+dscrperfst+"\n"+ "dscrexactst "+dscrexactst+"\n"+ "dscrpartialst "+dscrpartialst+"\n"+ "dscrreasonst "+dscrreasonst+"\n"+
					"dscrperfch "+dscrperfch+"\n"+ "dscrexactch "+dscrexactch+"\n"+ "dscrpartialch "+dscrpartialch+"\n"+ "dscrreasonch "+dscrreasonch+"\n"+
					"dscrperfrel "+dscrperfrel+"\n"+ "dscrexactrel "+dscrexactrel+"\n"+ "dscrpartialrel "+dscrpartialrel+"\n"+ "dscrreasonrel "+dscrreasonrel);
			
			stmt.execute("insert into precisionrecall values('avg',"+dessize+","+desseg+","+desstr+","+desch+","+desrel+",'"+dscpperfst+"','"+dscpexactst+"','"+dscppartialst+"','"+dscpreasonst+"','"+dscpperfch+"','"+dscpexactch+"','"+dscppartialch+"','"+dscpreasonch+"','"+dscpperfrel+"','"+dscpexactrel+"','"+dscppartialrel+"','"+dscpreasonrel+"'," +
					"'"+dscrperfst+"','"+dscrexactst+"','"+dscrpartialst+"','"+dscrreasonst+"','"+dscrperfch+"','"+dscrexactch+"','"+dscrpartialch+"','"+dscrreasonch+"','"+dscrperfrel+"','"+dscrexactrel+"','"+dscrpartialrel+"','"+dscrreasonrel+"','0','0','0','0')");
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
	
	
	/**
	 * 
	 * Each sentence to be compared needs to be in an individual file named by its 'source id'. The filename should 
	 * not contain any '[' or ']'. 
	 * The root element for the file should be <statement>.
	 * The machine generated files should be in a separate folder whose URL is stored in var:testdirectory.
	 * The human generated files should be in a separate folder whose URL is stored in var:ansdirectory.
	 *  
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//@SuppressWarnings("unused")
		String database="annotationevaluation";
		String projectfolder="C:\\DATA\\evaluation\\fnav19";
		
		//String database="annotationevaluation";
		//String projectfolder="C:\\DATA\\evaluation\\treatise";
		
		//String database="annotationevaluation_heuristics_fna";
		//String projectfolder="C:\\DATA\\evaluation\\fnav19";
		
		
		//String database="annotationevaluation_heuristics_treatise";
		//String projectfolder="C:\\DATA\\evaluation\\treatise";
		CompareXML cXML = new CompareXML(database, projectfolder);
	}

}
