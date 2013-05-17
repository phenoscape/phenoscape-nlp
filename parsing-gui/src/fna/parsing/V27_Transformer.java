package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class V27_Transformer {
	Element treatment = new Element("treatment");
	String keystorage = "";
	int keydetecter = 0;
	static int partdetecter, count;
	static Hashtable<String, String> hashtable = new Hashtable<String, String>();
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception{
		File extracted = new File("C:/Users/Li Chen/Desktop/FNA/v27/Extracted");	
		File[] files = extracted.listFiles();
		for(int i = 1; i<=files.length; i++){
			count = i;
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build("C:/Users/Li Chen/Desktop/FNA/v27/Extracted/" + i + ".xml");
			Element root = doc.getRootElement();
			List<Element> paralist = XPath.selectNodes(root, "/treatment/paragraph");
			//System.out.println(paralist.get(4).toString());
			V27_Transformer transformer = new V27_Transformer();
			transformer.createtreatment();
			transformer.processparagraph(paralist);
			/*
			if(partdetecter == 0){
				transformer.processparagraph2(paralist);
			}else{
				transformer.processparagraph(paralist);
			}
			*/
			transformer.output(i);
		} 
	}
	@SuppressWarnings("unchecked")
	private void processparagraph(List<Element> paralist) throws Exception{
		File habitatout = new File("C:/Users/Li Chen/Desktop/FNA/v27/habitat/" + count + ".txt");
		File distriout = new File("C:/Users/Li Chen/Desktop/FNA/v27/distribution/" + count + ".txt");
		Iterator<Element> paraiter = paralist.iterator();
		//int familydetecter = 0;
		while(paraiter.hasNext()){
			int bolddetecter = 0;
			Element pe = (Element)paraiter.next();
			List<Element> contentlist = pe.getChildren();
			Iterator<Element> contentiter = contentlist.iterator();
			String text = "";
			while(contentiter.hasNext()){
				Element te = (Element)contentiter.next();
				if(te.getName()=="text"){
					text = text + te.getText();
				}
				if(te.getName()=="bold"){
					bolddetecter = 1;
				}
			}
			//System.out.println(text);
			if(text.contains("TAKAKIACEAE")&text.contains("Hattori")&text.contains("Inoue")){
				partdetecter = 1;
			}
			if(partdetecter == 0){
				if(text.contains("=")){
					Element discussion = new Element("Discussion");
					discussion.setText(text);
					treatment.addContent(discussion);
					String[] university = new String[3];
					String[] name = new String[3];
					String shortname, fullname;
					university = text.split("=");
					shortname = university[0];
					if(university[1].contains(";")){
						name = university[1].split(";");
						fullname = name[0];
					}else{
						fullname = university[1].replace(".", "");
					}
					hashtable.put(shortname, fullname);
				}else{
					Element reference = new Element("Reference");
					for(Iterator<String> itr = hashtable.keySet().iterator(); itr.hasNext();){
						String key = (String) itr.next();
						String value = (String) hashtable.get(key);
						if(text.contains(key)){
							text =  text.replace(key, value);
						}
					}
					reference.setText(text);
					treatment.addContent(reference);
				}
			}	
			else{
				if(!text.contains("[")&text.matches("\\d*\\.[A-Z]+\\s.+")&text.length()<=40){//Family Name
					Element familyname = new Element("Family_Name");
					familyname.setText(text);
					treatment.addContent(familyname);
					//System.out.println(text);
				}
				else if(text.matches("[A-Z]\\w+\\s[A-Z]\\.\\s[A-Z].*")){//Author
					Element author = new Element("Author");
					author.setText(text);
					treatment.addContent(author);
					//System.out.println(text);
					//familydetecter = 0;
				}
				else if(bolddetecter == 1&!text.matches("[0-9]+\\..+")){//Description
					Element description = new Element("Description");
					description.setText(text);
					treatment.addContent(description);
				}
				else if(text.matches("(Genus|Species) (ca)?.+")){//number of infrataxa
					Element infrataxa = new Element("Number_of_Infrataxa");
					infrataxa.setText(text);
					treatment.addContent(infrataxa);
				}
				else if(text.matches("[0-9]+\\. {0,5}[A-Z]{2}.+")){//Gene name
					Element genename = new Element("Gene_Name");
					genename.setText(text);
					treatment.addContent(genename);
				}
				else if(text.matches("SELECTED REFERENCE.+")){//Reference
					Element reference = new Element("Reference");
					reference.setText(text);
					treatment.addContent(reference);
				}
				else if(text.matches("Capsules (matur|fairly|rare|common|unknown|uncommon).+")){//Distribution
					Element Flowertime = new Element("Flower_Time");
					Element Habitat = new Element("Habitat");
					Element Elevation = new Element("Elevation");
					Element Distribution = new Element("Distribution");
					
					String dot[] = new String[5];
					dot = text.split("\\.");
					String ft = dot[0];
					
					String semi[] = new String[5];
					semi = dot[1].split(";");
					String habit = semi[0];
					String elevation, distri;
					try{
						elevation = semi[1];
					}catch(Exception e){continue;}

					distri = text.replaceAll(ft.replace("(", "\\(").replace(")", "\\)") + "\\.", "").replaceAll(habit + ";", "").replaceAll(elevation.replace("(", "\\(").replace(")", "\\)") + ";", "");
					BufferedWriter bwh, bwd;
					bwh = new BufferedWriter(new FileWriter(habitatout, true));
					bwd = new BufferedWriter(new FileWriter(distriout, true));
					bwh.write(habit + "\r\n");
					bwd.write(distri + "\r\n");
					bwh.flush();
					bwd.flush();
					bwh.close();
					bwd.close();
					Flowertime.setText(ft);
					Habitat.setText(habit);
					Elevation.setText(elevation);
					Distribution.setText(distri);
					treatment.addContent(Flowertime);
					treatment.addContent(Habitat);
					treatment.addContent(Elevation);
					treatment.addContent(Distribution);
				}
				else if(bolddetecter == 1&text.matches("[0-9]+\\..+")){//Species name
					Element speciesname = new Element("Species_Name");
					speciesname.setText(text);
					treatment.addContent(speciesname);
				}
				else{
					if(text.matches("[0-9]+\\..+\\.")){//Key without next step
						Element key = new Element("Key");
						key.setText(text);
						treatment.addContent(key);
					}
					else if(text.matches("[0-9]+\\.[A-Z]+.+")&text.contains("   ")){//Key with next step
						Element key = new Element("Key");
						key.setText(text);
						treatment.addContent(key);
					}
					else if(text.matches("[0-9]+\\..+")&!text.contains("   ")&!text.matches(".+\\.")){//Key need to combine
						keystorage = text;
						keydetecter = 1;
					}
					else if(keydetecter == 1){//Combine key
						text = keystorage + text;
	
						Element key = new Element("Key");
						key.setText(text);
						treatment.addContent(key);
	
						keydetecter = 0;
					}
					else{//Discussion
						Element discussion = new Element("Discussion");
						discussion.setText(text);
						treatment.addContent(discussion);
					}
				}
			}		
		}
	}
	
	private void output(int i) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		String file = "C:/Users/Li Chen/Desktop/FNA/v27/Transformed/" + i + ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
}

