/**
 * 
 */
package fna.parsing.datacleaner;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.jdom.xpath.XPath;

import fna.charactermarkup.*;

/**
 * @author hongcui
 *
 */
public class CleanHabitate extends DataCleaner{

	/**
	 * 
	 */
	public CleanHabitate(String sourcedir, ArrayList<String> sourceElements, String outputElement, String outputdir) {
		super(sourcedir, sourceElements, outputElement, outputdir);
	}
	
	@SuppressWarnings("unchecked")
	protected Element clean(Element root){
		try{
			Iterator<String> it = this.sourceelements.iterator();
			while(it.hasNext()){
				String ename = it.next();
				List<Element> elements = XPath.selectNodes(root, "//"+ename);
				Iterator<Element> eit = elements.iterator();
				while(eit.hasNext()){
					Element e = eit.next();
					ArrayList<String> values = cleanText(e.getText());
					Element p = e.getParentElement();
					p.removeContent(e);
					if(p.getChildren().size()==0){
						p.detach();
					}
					Iterator<String> vit = values.iterator();
					while(vit.hasNext()){//if values is empty, no replacement is done, but the original element is removed
						Element ce = new Element(this.outputelement);
						String text = vit.next();
						ce.setText(text);
						p.addContent(ce);
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return root;
	}
	
	/**
	 * ***************************************************************************
	 * remove leading adv, and/or/etc, prepositions
	 * remove () and unclosed ones too
	 * ignore anything with a number in it
	 */
	protected ArrayList<String> cleanText(String text){
		ArrayList<String> cleaned = new ArrayList<String>();
		System.out.print(text+"==================>");
		if(text.matches("\\d")) return cleaned;
		text = text.replaceAll("\\(.*?(\\)|$)", "").replaceAll("(^|\\().*?[^(]?\\)", ""); //remove all words in parenthesis
		//text = text.replaceAll("[)(]", "");
		//word by word ellimination
		boolean changed = false;
		String word = "";
		do{
			changed = false;
			int i = text.indexOf(' ');
			if(i<0){
				word = text;
				text = "";
			}else{
				word = text.substring(0, i);
				text = text.replaceFirst("^"+word, "").trim();
			}
			if(Utilities.isAdv(word, ChunkedSentence.adverbs, ChunkedSentence.notadverbs)) changed = true;
			if(word.matches("\\b(and|or)\\b")) changed = true;
			if(word.matches("\\b("+ChunkedSentence.prepositions+")\\b")) changed = true;
			if(!changed) text = word+" "+text;			
		}while(changed);
		text = text.replaceFirst("^\\W+", "").replaceFirst("\\W+$", "");
		cleaned.add(text);
		System.out.println(text);
		return cleaned;	
	}

	protected void collectLegalValues(){ //no need to collect legal values
		
	} 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String sourcedir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta-clean-3";
		ArrayList<String> sourceElements = new ArrayList<String>();
		sourceElements.add("ecological_info/habitat");
		String outputElement = "habitat";
		String outputdir = "X:\\RESEARCH\\Projects\\FNA2010-characterSearch\\19-meta-clean-4";
		CleanHabitate ct = new CleanHabitate(sourcedir, sourceElements, outputElement, outputdir);
		ct.collectSourceContent();
		ct.collectLegalValues();
		ct.cleanFiles();

	}

}
