package fna.parsing;

import org.jdom.*;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import java.io.*;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

public class V8_Transformer {
	Element treatment = new Element("treatment");
	String keystorage = "";
	int keydetecter = 0;
	static int partdetecter, count;
	static Hashtable<String, String> hashtable = new Hashtable<String, String>();
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception{
		File extracted = new File("C:/Users/Li Chen/Desktop/Library Project/Library Project/Extracted");	
		File[] files = extracted.listFiles();
		for(int i = 1; i<=files.length; i++){
			count = i;
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build("C:/Users/Li Chen/Desktop/Library Project/Library Project/Extracted/" + i + ".xml");
			Element root = doc.getRootElement();
			List<Element> paralist = XPath.selectNodes(root, "/treatment/paragraph");
			//System.out.println(paralist.get(4).toString());
			V8_Transformer transformer = new V8_Transformer();
			transformer.createtreatment();
			if(partdetecter == 0){
				transformer.processparagraph(paralist);
			}else{
				transformer.processparagraph2(paralist);
			}
			transformer.output(i);
		} 
	}
	@SuppressWarnings("unchecked")
	private void processparagraph(List<Element> paralist) throws Exception{
		Iterator<Element> paraiter = paralist.iterator();
		int familydetecter = 0;
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
			if(text.contains("that are not pertinent in this volume")){
				partdetecter = 1;
			}
			if(text.contains("Family")&text.matches("[A-Z]+.+")){//Family Name
				familydetecter = 1;
				Element familyname = new Element("Family_Name");
				familyname.setText(text);
				treatment.addContent(familyname);
			}
			else if(familydetecter == 1 ){//Author
				Element author = new Element("Author");
				author.setText(text);
				treatment.addContent(author);
				
				familydetecter = 0;
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
			else if(text.matches("Flowering.+")&text.contains(";")){//Distribution
				Element floweringtime = new Element("Flowering_Time");
				Element habitat = new Element("Habitat");
				Element conservation = new Element("Conservation");
				Element elevation = new Element("Elevation");
				Element distribution = new Element("Distribution");
				String flowtime = null, habi = null, eleva=null, distri=null, conserv=null;
				String[] semi = new String[4];
				String[] dot = new String[3];
				semi = text.split(";");
				
					for(int i = 0; i<semi.length;i++){
						if(semi[i].contains("Flowering")){
							if(semi[i].contains(".")){
								dot = semi[i].split("\\.");
								flowtime = dot[0];
								habi = dot[1];
								File habitatout = new File("C:/Users/Li Chen/Desktop/Library Project/Library Project/habitat/" + count + ".txt");
								habitatout.delete();
								FileWriter fw = new FileWriter(habitatout, true);
								fw.append(habi + "\r\n");
								fw.flush();
								floweringtime.setText(flowtime);
								habitat.setText(habi);
							}
							
						}
						else if(semi[i].contains("conservation concern")){
							conserv = semi[i];
							conservation.setText(conserv);
							
						}
						else if(semi[i].contains(".,")){
							distri = semi[i];
							distribution.setText(distri);
							File distributionout = new File("C:/Users/Li Chen/Desktop/Library Project/Library Project/distribution/" + count + ".txt");
							distributionout.delete();
							FileWriter fw = new FileWriter(distributionout, true);
							fw.append(distri + "\r\n");
							fw.flush();
						}
						else if(semi[i].matches(".+ m")){
							eleva = semi[i];
							elevation.setText(eleva);
							
						}
					}				
				
					treatment.addContent(floweringtime);
					treatment.addContent(habitat);
					treatment.addContent(conservation);
					treatment.addContent(distribution);
					treatment.addContent(elevation);
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
	@SuppressWarnings("unchecked")
	private void processparagraph2(List<Element> paralist) throws Exception{
		Iterator<Element> paraiter = paralist.iterator();
		while(paraiter.hasNext()){
			Element pe = (Element)paraiter.next();
			List<Element> contentlist = pe.getChildren();
			Iterator<Element> contentiter = contentlist.iterator();
			String text = "";
			while(contentiter.hasNext()){
				Element te = (Element)contentiter.next();
				if(te.getName()=="text"){
					text = text + te.getText();
				}
			}
			//System.out.println(text);
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
	}
	private void output(int i) throws Exception {
		XMLOutputter outputter = new XMLOutputter();
		String file = "C:/Users/Li Chen/Desktop/Library Project/Library Project/Transformed/" + i + ".xml";
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(
				new FileOutputStream(file));
		outputter.output(doc, out);
	}
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
}

