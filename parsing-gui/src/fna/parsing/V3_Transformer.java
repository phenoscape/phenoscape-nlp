package fna.parsing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFileChooser;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;

public class V3_Transformer {
	Element treatment = new Element("treatment");
	@SuppressWarnings("unchecked")
	public static void main(String[] args)throws Exception{
		final JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Transformer");
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int result = fc.showOpenDialog(fc);
		File resource = null;
		String filename = null;
		if(result == JFileChooser.APPROVE_OPTION){
			resource = fc.getSelectedFile();
		}
		File[] files = resource.listFiles();
		Document doc = null;
		for(int i = 0; i<files.length; i++){
			filename = files[i].getName();
			SAXBuilder builder = new SAXBuilder();
			doc = builder.build(files[i]);
			Element root = doc.getRootElement();
			List<Element> paralist = XPath.selectNodes(root, "/treatment/paragraph");
			V3_Transformer transformer = new V3_Transformer();
			transformer.createtreatment();
			transformer.processparagraph(paralist,filename);
			transformer.output(filename);
		} 
	}

	private void processparagraph(List<Element> paralist, String filename)throws Exception{
		Iterator<Element> paraiter = paralist.iterator();
		int familydetecter = 0;
		while(paraiter.hasNext()){
			int bolddetecter = 0;
			int fnamedetecter = 0;
			Element pe = (Element)paraiter.next();
			String text = pe.getText();
			if(text.contains("......false")){
				bolddetecter = 1;
				text = text.replaceAll("......false", "");
			}
			if(text.contains("......name")){
				fnamedetecter = 1;
				text = text.replaceAll("......name", "");
			}
			text = text.replaceAll("......true", "");
			//System.out.println(text);
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
								File habitatout = new File("C:/Users/Li Chen/Desktop/FNA/V3/habitat/" + filename + ".txt");
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
							File distributionout = new File("C:/Users/Li Chen/Desktop/FNA/V3/distribution/" + filename + ".txt");
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
			else if(fnamedetecter == 1){//Species name
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
				else{//Discussion
					Element discussion = new Element("Discussion");
					discussion.setText(text);
					treatment.addContent(discussion);
				}
			}
		}
	}

	private void output(String filename)throws Exception{
		XMLOutputter outputter = new XMLOutputter();
		String file = "C:/Users/Li Chen/Desktop/FNA/V3/Transformed/" + filename;
		Document doc = new Document(treatment);
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
		outputter.output(doc, out);
	}
	
	private void createtreatment() throws Exception{
		treatment = new Element("treatment");
	}
}


