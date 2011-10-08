/**
 * 
 */
package fna.parsing.datacleaner;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.parsing.ParsingUtil;
/**
 * @author hongcui
 *
 */
@SuppressWarnings({ "unchecked"})
public abstract class DataCleaner{
	protected String legalvalues = null;
	protected ArrayList<String> sourceelements = new ArrayList<String>();
	protected File outputdir = null;
	protected File sourcedir = null;
	protected String outputelement = null;
	protected ArrayList<String> sourcecontent = new ArrayList<String>();
	/**
	 * 
	 */
	public DataCleaner(String sourcedir, ArrayList<String> sourceElements, String outputElement, String outputdir) {
		// TODO Auto-generated constructor stub
		this.sourcedir = new File(sourcedir);
		this.sourceelements = sourceElements;
		this.outputdir = new File(outputdir);
		if(! this.outputdir.exists()){
			this.outputdir.mkdir();
		}
		this.outputelement = outputElement;
	}

	/**
	 * ***********************************************************************************
	 * collect content from sourceElements in the files from sourcedir
	 * save the content text in sourcecontent
	 */
	protected void collectSourceContent(){
		File[] flist = sourcedir.listFiles();
		for(int i = 0; i<flist.length; i++){
			saveContents(flist[i]);
		}
	}
	
	private void saveContents(File source){
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(source);
			Element root = doc.getRootElement();
			
			Iterator<String> it = sourceelements.iterator();
			while(it.hasNext()){
				String ename = it.next();
				List<Element> elements = XPath.selectNodes(root, "//"+ename);
				Iterator<Element> eit = elements.iterator();
				while(eit.hasNext()){
					Element e = eit.next();
					sourcecontent.add(e.getText());					
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/*
	 * **************************************************************************************
	 * replace the content of each source element with its legal value
	 * replace the source element name with output element name
	 */
	protected void cleanFiles(){
		File[] flist = sourcedir.listFiles();
		for(int i = 0; i<flist.length; i++){
			cleanElements(flist[i]);
		}
	}

	

	private void cleanElements(File file) {
		try{
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build(file);
			Element root = doc.getRootElement();
			root = clean(root);
			root.detach();
			ParsingUtil.outputXML(root, new File(outputdir, file.getName()), null);
		}catch (Exception e){
			e.printStackTrace();
		}
		
	}

	protected abstract Element clean(Element root);

	/**
	 * 1 text may contain multiple legal values
	 * @param text
	 * @return a set of legal values
	 */
	protected ArrayList<String> cleanText(String text) {
		
		ArrayList<String> values = new ArrayList<String>();
		//text = text.toLowerCase();
		System.out.println();
		System.out.print(text+"=========>");
		text = text.replaceAll("\\.", "PERIOD").replaceAll("\\p{Punct}", "").replaceAll("PERIOD", ".");

		Pattern p = Pattern.compile(".*?\\b("+this.legalvalues+")( |$|\\W)(.*)");
		Matcher m = p.matcher(text);
		while(m.matches()){
			values.add(m.group(1));
			System.out.print(":"+standardize(m.group(1)));
			m = p.matcher(m.group(3));
		}
		return values;
	}
	
	protected String standardize(String s){
		return s;
	}

	/*
	 * ***********************************************************************************
	 */
	protected abstract void collectLegalValues(); 


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
