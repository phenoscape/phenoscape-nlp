 /* $Id: SentenceChunker4StanfordParser.java 971 2011-09-13 18:32:55Z hong1.cui $ */
/**
 * 
 */
package fna.charactermarkup;

import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.filter.ElementFilter;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;

import fna.parsing.state.WordNetWrapper;

/**
 * @author hong
 * This class extract possible relationships from a parsing tree, which is represented in XML format
 * It also takes the marked sentence on which the parsing tree was generated, e.g. <leaves> {big}.
 * Since either source could contain errors, the class tries to make the best guess possible.
 * 
 * 
 * Penn Tags that we care:
 * VB, verb; VBD, verb, past tensel VBG, verb present participle or gerund; VBN, verb, past participle; VBP, verb, present, not 3rd person sinugular; and VBZ, verb, present tense, 3rd person singular
 * PP (IN), preposition;
 * JJR/JJS, adjective, comparative/superlative
 * RBR/RBS, adverb, comparative/superlative; RB, adverb
 * WHADVP, wh-adverb phrase
 * (QP (IN at) (JJS least) (CD 3/4)))
 * 
 * 
 * 
 * 
 * 
 * chunk symbols:
 
QP: q
SBAR: s
VP: b[v/o]
PP: r[p/o]
VP-PP: t[c/r[p/o]]
ADJ-PP:t[c/r[p/o]]
Than: n
To: w
NPList: l
PPList: i
main subject: z[m/e]
 * 
 */
@SuppressWarnings({ "unchecked", "unused" })
public class SentenceChunker4StanfordParser {
	private String sentsrc = null;
	private String tableprefix = null;
	private Connection conn = null;
	private String glosstable = null;
	private Document tree = null;
	private Document treecp = null;
	private String markedsent = null;
	private String [] tokensinsent = null;
	private String [] posoftokens = null;
	private static final String QPpathstr = ".//QP";
	private static final String PPINpathstr = ".//PP/IN";
	//private static final String PPTOpathstr = ".//PP/TO";
	private static final String Vpathstr = ".//VP/VBD|.//VP/VBG|.//VP/VBN|.//VP/VBP|.//VP/VBZ|.//VP/VB";
	private static final String NNpathstr = ".//NP/NN|.//NP/NNS";
	private static XPath QPpath = null;
	private static XPath PPINpath = null;
	private static XPath PPTOpath = null;
	private static XPath Vpath = null;
	private static XPath NNpath = null;
	private int sentindex = -1;
	private Pattern p = Pattern.compile("(.*?)((?:\\w+ )+)\\2(.*)");
	private String conjunctions = "and|or|plus";
	private boolean printVB = false;
	private boolean printPP = false;
	private boolean printNPlist = false;
	private boolean debug = false;
	//private boolean printPPTO = true;
	/**
	 * 
	 */
	public SentenceChunker4StanfordParser(int index, Document parsingTree, String markedsent, String sentsrc, String tableprefix,Connection conn, String glosstable) {
		this.sentsrc = sentsrc;
		this.tableprefix = tableprefix;
		this.conn = conn;
		this.glosstable = glosstable;
		this.sentindex = index;
		this.tree = parsingTree;
		this.treecp = (Document)tree.clone();
		this.markedsent = NumericalHandler.normalizeNumberExp(markedsent);
		//this.markedsent = markedsent.replaceAll(",", " , ").replaceAll(";", " ; ").replaceAll(":", " : ").replaceAll("\\.", " . ").replaceAll("\\[", " [ ").replaceAll("\\]", " ] ").replaceAll("\\s+", " ");
		//in markedsent, each non-<>{}-punctuation mark is surrounded with spaces
		this.posoftokens = this.markedsent.split("\\s+");
		this.tokensinsent = this.markedsent.replaceAll("[<{}>]", "").split("\\s+");
		for(int i =0; i<posoftokens.length; i++){
			if(this.posoftokens[i].indexOf("<")>=0 && this.posoftokens[i].indexOf("{")>=0){
				this.posoftokens[i]="NN";
			}else if(this.posoftokens[i].indexOf("<")>=0){
				this.posoftokens[i]="NN";
			}else if(this.posoftokens[i].indexOf("{")>=0){
				this.posoftokens[i]="ADJ";
			}else{
				this.posoftokens[i]="";
			}
		}		
		
		try{
			SentenceChunker4StanfordParser.QPpath = XPath.newInstance(QPpathstr);
			SentenceChunker4StanfordParser.PPINpath = XPath.newInstance(PPINpathstr);
			//SentenceChunker.PPTOpath = XPath.newInstance(PPTOpathstr);
			SentenceChunker4StanfordParser.Vpath = XPath.newInstance(Vpathstr);
			SentenceChunker4StanfordParser.NNpath = XPath.newInstance(NNpathstr);			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public ChunkedSentence chunkIt() throws Exception{
		//ArrayList<Relation> results = new ArrayList<Relation>();
		//check to see if the tree used the POS tag provided by markedsent, if not, return empty list.
		//if(POSMatch()){
			//hide complex number patterns should already be hidden before the tree was produced			
			try{
				Element root = tree.getRootElement();
				/*collapse QPs: QPs often break a semantic unit, don't chunk it.
				List<Element> QPs = QPpath.selectNodes(root);
				Iterator<Element> it = QPs.iterator();
				while(it.hasNext()){
					Element QP = it.next();
					collapseElement(QP, allText(QP), "q");									
				}*/

				collapseNPList();
				collapsePPList();
				
				//get All PP/IN
				List<Element> PPINs = PPINpath.selectNodes(root);
				do{
					ArrayList<Element> lPPINs = new ArrayList<Element>();
					//PPINs = XPath.selectNodes(root, "//PP/IN");
					PPINs = sortById(PPINpath.selectNodes(root));
					Iterator<Element> it = PPINs.iterator();
					while(it.hasNext()){
						Element PPIN = it.next();
						//List<Element> temp = XPath.selectNodes(PPIN, "//PP/IN");
						List<Element> temp = PPINpath.selectNodes(PPIN.getParentElement());
						if(temp.size() == 0){
							lPPINs.add(PPIN);
						}
					}
					extractFromlPPINs(lPPINs);
				}while (PPINs.size() > 0);	
				
				//get remaining VBs
				List<Element> VBs = Vpath.selectNodes(root);
				do{
					VBs = Vpath.selectNodes(root);
					Iterator<Element> it = VBs.iterator();
					ArrayList<Element> lVBs = new ArrayList<Element>();
					while(it.hasNext()){
						Element VB = it.next();
						if(Vpath.selectNodes(VB).size() == 0 && VB.getChild("PP") == null){ //VP/PP should have been processed in PPINs
							lVBs.add(VB);
						}
					}
					extractFromlVBs(lVBs);
					
				}while(VBs.size() > 0);
			
		//}
		collapseThatClause();//that, which
		collapseWhereClause();//where
		collapseWhenClause(); //when
		
		ChunkedSentence cs = new ChunkedSentence(this.sentindex , tree, treecp, this.markedsent, this.sentsrc, this.tableprefix,this.conn, this.glosstable);
		return cs;
			}catch(Exception e){
				e.printStackTrace();
				throw e;
			}
	}
	
	/**
	 * sort the list by the element id in ascending order
	 * @param selectNodes
	 * @return
	 * 
	 * procedure bubbleSort( A : list of sortable items ) defined as:
  n := length( A )
  do
    newn := 0
    for each i in 0 to n - 2  inclusive do:
      if A[ i ] > A[ i + 1 ] then
        swap( A[ i ], A[ i + 1 ] )
        newn := i + 1
      end if
    end for
    n := newn
  while n > 1
end procedure


	 */
	private List<Element> sortById(List<Element> selectNodes) {
		// TODO Auto-generated method stub
		int n = selectNodes.size();
		do{
			int newn = 0;
			for(int i = 0; i <= n-2; i++){
				int id1 = Integer.parseInt(selectNodes.get(i).getAttributeValue("id"));
				int id2 = Integer.parseInt(selectNodes.get(i+1).getAttributeValue("id"));
				if(id1 > id2){
					Element t = selectNodes.get(i);
					selectNodes.set(i, selectNodes.get(i+1));
					selectNodes.set(i+1, t);
				}
			}
			n = newn;
		}while (n > 1);
		
		return selectNodes;
	}

	/**
	 * (SBAR
              (WHNP (WDT that))
              (S
                (VP (VBP resemble)
                  (NP (NN tacks)))))
	 */
	private void collapseThatClause() {
		try{
			Element root  = tree.getRootElement();
			List<Element> thatclauses = XPath.selectNodes(root, ".//SBAR/WHNP/*[@text='that']"); //select WHNP elements
			thatclauses.addAll(XPath.selectNodes(root, ".//SBAR/WHNP/*[@text='which']"));
			Iterator<Element> it = thatclauses.iterator();
			while(it.hasNext()){
				Element WHNP = it.next();
				Element SBAR = WHNP.getParentElement();
				if(SBAR.getName().compareTo("SBAR") !=0){
					SBAR = SBAR.getParentElement();
				}
				collapseElement(SBAR, allText(SBAR), "s");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	
	/**
	 *     (SBAR
        (WHADVP (WRB where))
        (S
          (NP (NN spine) (NNS bases))
          (ADJP (JJ tend)
            (S
              (VP (TO to)
                (VP (VB be)
                  (ADJP (JJ elongate))))))))       
	 */
	private void collapseWhereClause() {
		try{
			Element root  = tree.getRootElement();
			List<Element> whereclauses = XPath.selectNodes(root, ".//SBAR/WHADVP/*[@text='where']"); //select WHNP elements
			Iterator<Element> it = whereclauses.iterator();
			while(it.hasNext()){
				Element WHNP = it.next();
				Element SBAR = WHNP.getParentElement();
				if(SBAR.getName().compareTo("SBAR") !=0){
					SBAR = SBAR.getParentElement();
				}
				collapseElement(SBAR, allText(SBAR), "s");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
    /*(SBAR
            (WHADVP (WRB when))
            (S
              (NP (NN corpus) (NN cavity))
              (VP (VBD became)
                (ADJP (JJ deep)))))
      (, ,)               
   
      (SBAR
      	(WHADVP (WRB when))
      	(S
        	(ADJP (JJ axillary))))
      (, ,)
       	
     (ADVP
              (ADJP
                (WHADVP (WRB when))
                (JJ terminal))
              (PP (IN with)
                (ADVP (RB moreorless))))
            (NP
              (NP (JJ well-defined) (JJ central) (NNS axis))
              (CC and)
              (NP (JJ shorter) (NN side) (NNS branches)))))   	
              
      (NP (WRB when) (JJ terminal))
              (, ,)
             
      (ADJP
                (WHADVP (WRB when))
                (JJ pubescent))
              (, ,))  
              
   (SBAR
        (WHADVP (WRB when)
          (ADJP (JJ hydrated) (, ,) (JJ obscured)))
        (FRAG
          (ADJP
            (WHADVP (WRB when))
            (JJ desiccated)))))
    (. .)))                
	 */
	/**
	 * a when-clause ends at a ","
	 * collect from when to nearest ,
	 * collapse the element of when (WRB, IN, or any wired tag when appears with)
	 * e.g., the last example above will be collapsed as: note WRB is changed to WHENCLS 
	    (SBAR
        (WHADVP (WHENCLS when hydrated)
          (ADJP (, ,) (JJ obscured)))
        (FRAG
          (ADJP
            (WHADVP (WHENCLS when desiccated))
            ))))
        (. .))) 
	 */
	private void collapseWhenClause() {
		try{
			Element root  = tree.getRootElement();
			List<Element> whenclauses = XPath.selectNodes(root, ".//*[@text='when']"); //select any element containing "when"
			for(int i = 0; i < whenclauses.size(); i++){
				Element WHEN = whenclauses.get(i);
				//collect words/leaf nodes after "when" until a [,.] is found
				//growing the text in WHEN while removing included leaf nodes
				String text = "s[when "+collectText4New(WHEN, root).trim()+"]";
				WHEN.setAttribute("text", text);
				WHEN.setName("WHENCLS");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * following text order, collect text for new element "when" until a , or . is reached
	 * @param e
	 * @return
	 */
	private String collectText4New(Element when, Element root){
		StringBuffer text = new StringBuffer();
		Iterator<Element> all = root.getDescendants(new ElementFilter());
		boolean findwhen = false;
		ArrayList<Element> toberemoved = new ArrayList<Element>();
		while(all.hasNext()){
			Element e = all.next();
			if(findwhen){
				if(e.getAttribute("text")!=null){
					String t =e.getAttributeValue("text");
					if(t.matches("[\\.:;,]")){
						text.append(t).append(" ");
						toberemoved.add(e);
						break;
					}else if(text.length()>0 && t.matches("\\w+\\[.*")){ 
						//match: when {terminal} ........ r[p[with]] {moreorless} l[{well-defined} {central} (axis) and {shorter} (side) (branches)] ,
						//not match: when r[p[in] flower] , .... 
						break;
					}else{
						text.append(t).append(" ");
						toberemoved.add(e);
					}
				}
			}else if(e.equals(when)){
				findwhen = true;
			}			
		}		
		
		for(int i = 0; i<toberemoved.size(); i++){
			toberemoved.get(i).detach();
		}
		return text.toString().trim();
	}
	
	private void extractFromlVBs(ArrayList<Element> lVBs) {
		Iterator<Element> it = lVBs.iterator();
		while(it.hasNext()){
			Element lVB = it.next();
			boolean sureverb = false;
			/*
			 * (ROOT
  (S
    (NP (JJ elongate) (NN spine) (NNS bases))
    (VP (MD may)
      (VP (VB form)
        (NP (JJ incipient) (NNS ribs))
        (ADVP (RB anteriorly))))
    (: ;)))
			 */
			if(lVB.getParentElement()!= null && lVB.getParentElement().getParentElement()!=null){
				List<Element> children= lVB.getParentElement().getParentElement().getChildren();
				if(children.get(0).getName().compareTo("MD")==0 && children.get(1).equals(lVB.getParentElement())){
					sureverb = true;
				}
			}
			extractFromlVB(lVB, sureverb);
		}		
	}

	private void extractFromlVB(Element lVB, boolean sureverb) {

		Element VP = lVB.getParentElement();
		if(VP == null){
			return; //lVB is root
		}
		//Element child = VP.getChild("NP");//NULL Pointer?
		//String chaso = getOrganFrom(child);
		String theverb = lVB.getAttribute("text").getValue();
		WordNetWrapper wnw = new WordNetWrapper(theverb);
		
		if(!sureverb && (theverb.length()<2 || theverb.matches("\\b(\\w+ly|ca)\\b") 
		   /*||wnw.mostlikelyPOS()== null || wnw.mostlikelyPOS().compareTo("verb") !=0*/)){ //text of V is not a word, e.g. "x"
			collapseElement(VP, "", "");
			return;
		}
		
		if(this.printVB){
			System.out.println();
			System.out.println(this.sentindex+": "+this.markedsent);
			System.out.println("parent is [VP]");
			//System.out.println("IN text: "+up2Text(parent, PP)+" "+lPPIN.getAttributeValue("text"));
			System.out.println("VP text: "+lVB.getAttributeValue("text"));
			System.out.println("child text:"+firstNP(VP));
		}
		//do extraction here
		//print(VP, child, "", chaso);
		String np = firstNP(VP).trim();
		if(np.length()>0){
			String chunk = "v["+lVB.getAttributeValue("text")+ "] o["+np+"]";
			collapseElement(VP, chunk, "b");
		}else{
			collapseElement(VP, "", "");
		}
	}

	private void extractFromlPPINs(ArrayList<Element> lPPINs) {

 		Iterator<Element> it = lPPINs.iterator();
		while(it.hasNext()){
			Element lPPIN = it.next();
			extractFromlPPIN(lPPIN);
		}
	}
	
	/*private void extractFromlPPTOs(ArrayList<Element> lPPTOs) {

		Iterator<Element> it = lPPTOs.iterator();
		while(it.hasNext()){
			Element lPPTO = it.next();
			extractFromlPPTO(lPPTO);
		}
	}*/
	
	
	/*
	 * (NP
                  (NP (NN extension))
                  (PP (IN of)
                    (NP (NN air))))
                    
   (ADJP (RB densely) (VBN crowded)
      (PP (IN at)
        (NP (NN stem) (NNS tips))))

	 */
	private void extractFromlPPIN(Element lPPIN) {
		
		Element PP = lPPIN.getParentElement();
		if(PP == null){ //TODO:(IN except) (IN among)
			return;
		}

		//String chaso = getOrganFrom(child);
		//both child and parent must contain an organ name to extract a relation
		//if child has no organ name, extract a constraint, location, "leaves on ...??
		//if parent has no organ name, collapse the NP. "distance of ..."
		Element parent = PP.getParentElement();//could be PP, NP, VP, ADJP, or UCP, etc .
		if(lPPIN.getAttribute("text").getValue().length()<2 ||
				lPPIN.getAttribute("text").getValue().matches("\\b(\\w+ly|ca|to|than)\\b")){ //text of IN is not a word, e.g. "x"
			collapseElement(PP, "", "");
			return;
		}
		
		//String phaso = getOrganFrom(parentcp); 
		//print(parent, child, phaso, chaso);
		String ptag = parent==null? "" : parent.getName(); //parent is null when it is collaped in a previous run.
		
		
		if(ptag.compareTo("NP") == 0 ||ptag.compareTo("NX") == 0||ptag.compareTo("X") == 0|| ptag.compareTo("S") == 0 || 
				ptag.compareTo("FRAG") == 0 || ptag.compareTo("UCP") == 0 ||
				ptag.compareTo("PRN") == 0 ||ptag.compareTo("WHNP") == 0 ||ptag.compareTo("SINV") == 0 ||
				ptag.compareTo("PP") == 0 ||ptag.compareTo("ROOT") == 0 || ptag.compareTo("") == 0){		
			/**
			 * (NP
        			(NP (JJ alternate))
        			(PP (IN with)
          				(NP
            				(NP (NN corolla) (NNS lobes))
			 */
			if(this.printPP){
				System.out.println();
				System.out.println(this.sentindex+": "+this.markedsent);
				System.out.println("parent is [NP/S/FRAG/UCP/PRN/WHNP/PP/ROOT]");
				//System.out.println("IN text: "+up2Text(parent, PP)+" "+lPPIN.getAttributeValue("text"));
				System.out.println("IN text: "+lPPIN.getAttributeValue("text"));
				System.out.println("child text:"+firstNP(PP));
			}
			String chunk = "p["+lPPIN.getAttributeValue("text") + "] o["+firstNP(PP)+"]";
			collapseElement(PP, chunk, "r");
		}else if(ptag.compareTo("VP") == 0){
			boolean trueVP = false;
			try{			
				if(XPath.selectSingleNode(parent, ".//VBD|.//VBG|.//VBN|.//VBP|.//VBZ|.//VB") != null){
					trueVP = true;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			if(trueVP){
				if(this.printPP){
					System.out.println();
					System.out.println(this.sentindex+": "+this.markedsent);
					System.out.println("parent is [VP]");
					System.out.println("VP-IN text: "+up2Text(parent, PP)+" "+lPPIN.getAttributeValue("text")); // decurrent /as/ wings
					System.out.println("child text NPJJ:"+firstNPJJ(PP)); //some VP has no VB in it, but has ADJs
					System.out.println("child text:"+firstNP(PP));
				}
				//String chunk = "c["+up2Text(parent, PP)+"] r[p["+lPPIN.getAttributeValue("text")+ "] o["+firstNP(PP)+"]]";
				//collapseElement(parent, chunk, "t");
				String chunk = "p["+lPPIN.getAttributeValue("text")+ "] o["+firstNP(PP)+"]";
				collapseElement(PP, chunk, "r");
			}else{
				if(this.printPP){
					System.out.println();
					System.out.println(this.sentindex+": "+this.markedsent);
					System.out.println("parent is [not true VP]");
					System.out.println("IN text: "+lPPIN.getAttributeValue("text")); // decurrent /as/ wings
					System.out.println("child text:"+firstNP(PP)); //some VP has no VB in it, but has ADJs
				}
				String chunk = "p["+lPPIN.getAttributeValue("text")+ "] o["+firstNP(PP)+"]";
				collapseElement(PP, chunk, "r");
			}
		}else if(ptag.compareTo("ADJP") == 0 || ptag.compareTo("ADVP") == 0 ||ptag.compareTo("NAC") == 0 ||ptag.compareTo("RRC") == 0){
			/*
			 * (ADJP (JJ decurrent)
            		(PP (IN as)
            			(NP
              				(NP (JJ spiny) (NNS wings))
			 */
			if(this.printPP){
				System.out.println();
				System.out.println(this.sentindex+": "+this.markedsent);
				System.out.println("parent is [ADJP/ADVP/RRC]");
				System.out.println("ADJ-IN text: "+up2Text(parent, PP)+" "+lPPIN.getAttributeValue("text")); // decurrent /as/ wings
				System.out.println("child textNPJJ:"+firstNPJJ(PP));
				System.out.println("child text:"+firstNP(PP));
			}
			//String chunk = "c["+up2Text(parent, PP)+"] r[p["+lPPIN.getAttributeValue("text")+"] o["+firstNP(PP)+"]]";
			//collapseElement(parent, chunk, "t");
			String chunk = "p["+lPPIN.getAttributeValue("text")+"] o["+firstNP(PP)+"]";
			collapseElement(PP, chunk, "r");
		}else{
			if(this.printPP){
				System.out.println();
				System.out.println(this.sentindex+": "+this.markedsent);
				System.out.println("parent is "+ptag);
			}
		}
		
	}
	
	/**
	 * 
	 * @param parent
	 * @param pp
	 * @return text of the closest sibling elements (set off by ,) before PP
	 */
	private String up2Text(Element parent, Element pp) {
		String text="";
		try{
			Iterator<Content> it = parent.getDescendants();
			while(it.hasNext()){
				Content c = it.next();
				if(c instanceof Element){
					Element n = (Element)c;
					if(n.equals(pp)){
						text = text.trim();
						int i = text.lastIndexOf(",");
						if(i>=0 && i<text.length()-1){
							text = text.substring(i+1).trim();
						}
						if(text.indexOf(",")>=0){
							text  = text.substring(text.lastIndexOf(",")+1).trim();
						}
						return text;
					}
					if(n.getAttribute("text") != null){
						text += n.getAttributeValue("text")+" ";
					}
					
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return "";
		
	}

	/* not good
	 * private String firstDirectNP(Element e) {
		Element NP = e.getChild("NP");
		if(NP != null){
		Iterator<Element> cs = NP.getChildren().iterator();
		while(cs.hasNext()){
			Element c = cs.next();
			if(c.getChildren().size() > 0){
				return "";
			}
			if(c.getName().startsWith("NN")){
				return allText(c.getParentElement());
			}
		}
		}
		return "";
	}*/
	
	
	private String firstNP(Element e) {
		try{	
			if(e.getName().startsWith("NP") && e.getAttributeValue("text") != null){
				return e.getAttributeValue("text");
			}
			Iterator<Content> it = e.getDescendants();
			//boolean takeit = false;
			//String text = ""; collecting all text before the first NP
			while(it.hasNext()){
				Content c = it.next();
				if(c instanceof Element){
					String name = ((Element)c).getName();
					if(name.startsWith("NN")){//TODO: consider also CD(3) and PRP (them): no, let ChunkedSentence fix those cases
						Element p = ((Element)c).getParentElement();
						return allText(p);
						//return checkAgainstMarkedSent(allText(p), lastIdIn(p));
					}
					if(name.startsWith("NP") && ((Element)c).getAttributeValue("text") != null){//already collapsed NPs
						return ((Element)c).getAttributeValue("text");
					}
					if(((Element)c).getAttribute("text") != null && ((Element)c).getAttributeValue("text").indexOf(" o[")>=0){
						return ((Element)c).getAttributeValue("text");
					}
					//text += ((Element) c).getAttribute("text")!=null? ((Element) c).getAttributeValue("text"): "" ;
					//text = text.trim()+" ";
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return "";
	}
	/**
	 * 
	 * @param e
	 * @return the last id included in the descendents of p
	 */
	private String lastIdIn(Element el){
		Iterator<Content> it = el.getDescendants();
		String last = "";
		while(it.hasNext()){
			Content c = it.next();
			if(c instanceof Element){
				Element e = (Element)c;
				if(e.getAttributeValue("id")!=null){
					last = e.getAttributeValue("id");
				}
			}
		}
		return last;
	}
	
	/**
	 * the effect of this method is to shrink the np to the last nn/nns, or to nil if np does not contain any nn/nns
	 * the nil case will then be handled by one of the normalization method in ChunkedSentence
	 * @param np
	 * @param idofthelastwordinnp
	 * @return
	 */
	private String checkAgainstMarkedSent(String np, String idofthelastwordinnp){
		/*in a case like the following
		some gland-tipped hairs 3 mm r[p[on] o[margins]] r[p[near] o[bases]]
		this strategy (taking the last np) is problematic. 
		Should avoid nested chunks and take up to "hairs" and not "bases".
		test this later 12/15/10*/
		String onp = np;
		int last = Integer.parseInt(idofthelastwordinnp);
		if(np.startsWith("l[")){//adjust the index of a noun list. the idofthelastwordinnp in this case should be the last word in the list, not the first.
			last = last+np.split("\\s+").length-1;
		}
		int lastcp = last;
		/*int cindex = np.indexOf("[")-1; //index of first r[
		if(cindex>=0){
			String chunked = np.substring(cindex).trim(); //index of first r[
			last = last - chunked.split("\\s+").length;
			np = np.substring(0,cindex).trim();
		}*/
		String[] words = np.split("\\s+");
		String[] words2 = np.split("\\s+");
		for(int i = words.length-1; i>=0; i--){//find the last NN as marked in markedsent (e.g. posoftokens)
			if(!this.posoftokens[last--].startsWith("NN")){
				words[i] = "";
			}else{
				break;
			}
		}
		np = "";
		for(int i = 0; i < words.length; i++){//find the last NN as marked in markedsent (e.g. posoftokens)
			np +=words[i]+" ";
		}
		String winner = np.trim();
		//test out: find the first nn
		int first = lastcp - words.length +1;
		boolean foundn = false;
		boolean nends = false;
		for(int i = 0; i<words.length; i++){
			if(nends){
				words[i] = "";
			}else if(foundn && !this.posoftokens[first].startsWith("NN")){
				words[i] = "";
				nends = true;
			}else if(this.posoftokens[first].startsWith("NN")){
				foundn = true;
			}
			first++;
		}
		
		String np2 = "";
		for(int i = 0; i < words.length; i++){//find the last NN as marked in markedsent (e.g. posoftokens)
			np2 +=words[i]+" ";
		}
		
		np = np.replaceAll("\\s+", " ").trim();
		np2 = np2.replaceAll("\\s+", " ").trim();
		
		if(np.length()>0) winner=np.trim();
		if(np.compareTo(np2)!=0){
			if(debug){
			System.out.println("first NP need help");
			System.out.println("np is: "+np);
			System.out.println("np2 is: "+np2);
			}
			if(np.length() > np2.length()){//make np2 hold the longer string
				String t = np;
				np = np2;
				np2 = t;
			}
			if(np2.indexOf("l[")>=0){
				winner = np2.trim();
			}else if(np2.replace(np, "").trim().matches("^("+this.conjunctions+")")){
				winner = np2.trim();
			}else {
				winner = np.trim();
			}
		}
		
		
		if(debug){
		System.out.println("winner is: "+winner);
		}
		return winner;		
	}
	/**
	 * 
	 * @param child
	 * @return the text of the first NP leaf element. This should also include CD (e.g. 3) if it appears before the NP
	 */
	/*private String firstNP(Element e) {
		String chunk = "";
		boolean hasnoun = false;
		try{	
			if(e.getName().startsWith("NP") && e.getAttributeValue("text") != null){
				return e.getAttributeValue("text");
			}
			Iterator<Content> it = e.getDescendants();
			boolean takeit = false;
			while(it.hasNext()){
				Content c = it.next();
				if(c instanceof Element){
					String name = ((Element)c).getName();
					if(name.startsWith("NN")){//TODO: consider also CD(3) and PRP (them)
						String text = allText(((Element)c).getParentElement());
						if(text.indexOf(chunk.trim())>=0){
							return text;
						}else{
							chunk +=text; //chunk may contain duplicate words
							Matcher m = p.matcher(chunk);
							if(m.matches()){
								chunk = m.group(1)+m.group(2)+m.group(3);
							}
							return chunk.trim();
						}
					}
					if(name.startsWith("NP") && ((Element)c).getAttributeValue("text") != null){//done: consider also CD(3) and PRP (them)
						String text =((Element)c).getAttributeValue("text");
						if(text.indexOf(chunk.trim())>=0){
							return text;
						}else{
							chunk +=text;
							return chunk.trim();
						}
					}
					if(name.matches("(CD|PRP|DT|JJ|TO)")){
						chunk += ((Element)c).getAttributeValue("text")+" ";
						takeit = true;
					}else if(takeit && ((Element)c).getAttributeValue("text") != null){
						chunk += ((Element)c).getAttributeValue("text")+" ";
					}
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return chunk;
	}*/
	
	
	private String firstNPJJ(Element e) {
		try{
			Iterator<Content> it = e.getDescendants();
			while(it.hasNext()){
				Content c = it.next();
				if(c instanceof Element){
					String name = ((Element)c).getName();
					if(name.startsWith("NN") || name.startsWith("JJ")){//TODO: consider also CD(3) and PRP (them)
						Element p = ((Element)c).getParentElement();
						return checkJJAgainstMarkedSent(allText(p), lastIdIn(p));
					}
					if(((Element)c).getAttribute("text") != null && ((Element)c).getAttributeValue("text").indexOf(" o[")>=0){
						return ((Element)c).getAttributeValue("text");
					}
					
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return "";
	}
	
	/**
	 * if np has a noun, cut-off at the last noun
	 * otherwise, do nothing
	 * @param npjj
	 * @param lastid
	 * @return
	 */
	private String checkJJAgainstMarkedSent(String npjj, String lastid){
		boolean hasN = false;
		int last = Integer.parseInt(lastid);
		String[] words = npjj.split("\\s+");
		for(int i = words.length-1; i>=0; i--){//find the last NN as marked in markedsent (e.g. posoftokens)
			if(!this.posoftokens[last--].startsWith("NN")){
				words[i] = "";
			}else{
				hasN = true;
				break;
			}
		}
		if(hasN){
			npjj = "";
			for(int i = 0; i < words.length; i++){//find the last NN as marked in markedsent (e.g. posoftokens)
				npjj +=words[i]+" ";
			}
		}
		return npjj.replaceAll("\\s+", " ").trim();
	}
	
	/*private String firstNPJJ(Element e) {
		String chunk = "";
		try{	
			if(e.getName().startsWith("NP") && e.getAttributeValue("text") != null){
				return e.getAttributeValue("text");
			}
			Iterator<Content> it = e.getDescendants();
			boolean takeit = false;
			while(it.hasNext()){
				Content c = it.next();
				if(c instanceof Element){
					String name = ((Element)c).getName();
					if(name.startsWith("NN") || name.startsWith("JJ") ){//done: consider also CD(3) and PRP (them)
						String text = allText(((Element)c).getParentElement());
						if(text.indexOf(chunk.trim())>=0){
							return text;
						}else{
							chunk +=text; //chunk may contain duplicate words
							Matcher m = p.matcher(chunk);
							if(m.matches()){
								chunk = m.group(1)+m.group(2)+m.group(3);
							}
							return chunk.trim();
						}
					}
					if(name.startsWith("NP") && ((Element)c).getAttributeValue("text") != null){//done: consider also CD(3) and PRP (them)
						String text =((Element)c).getAttributeValue("text");
						if(text.indexOf(chunk.trim())>=0){
							return text;
						}else{
							chunk +=text;
							return chunk.trim();
						}
					}
					if(name.startsWith("CD") ||name.startsWith("PRP") || name.startsWith("DT")){
						chunk += ((Element)c).getAttributeValue("text")+" ";
						takeit = true;
					}else if(takeit && ((Element)c).getAttributeValue("text") != null){
						chunk += ((Element)c).getAttributeValue("text")+" ";
					}
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return chunk;
	}*/
	

	private void print(Element p, Element c, String phaso, String chaso){
		System.out.println("parent text: ["+phaso+"] "+allText(p));
		System.out.println("child text: ["+chaso+"] "+allText(c));
		System.out.println();
	}

	/**
	 * also set the id of the resultant element be the id of its first word/text
	 * if chunk is "", then collapse all text/child nodes without inserting symbols 
	 * @param e
	 * @return if the element is collapsed successfully
	 */
	private void collapseElement(Element e, String chunk, String symbol) {
		if(e == null){
			return;
		}
		chunk = chunk.trim().replaceAll("\\s*\\w\\[\\]\\s*", "").replaceAll("\\s+", " ").trim(); //remove empty o[]
		//add .replaceFirst("\\] o\\[", "\\\\] .*? o\\\\[") so the pattern matches anything in btw of prep/verb and its object
		String pattern = chunk.trim().replaceFirst("\\] o\\[", "](.*?) o[").replaceAll("\\w\\[", "(?:\\\\w\\\\[)*\\\\b").replaceAll("\\]", "\\\\b(?:\\\\])*");
		
		// collapsed text is saved to e
		// keeping other children of e as e's children

		//determine the id for the new chunked element
		//detach any leaf element encountered along the path =>tobedeleted
		//collecting all text along the path into "text"		
		String id = "";
		String chunktext = chunk.length() ==0 ? allText(e) : chunk;//if chunk is "", then collapse all text/child nodes 
		chunktext = chunktext.trim().replaceAll("(\\w\\[|\\])", "");
		//Element thechild = null;
		String text = "";
		ArrayList<Element> tobedeleted = new ArrayList<Element>();
		try{
			Iterator<Content> cit = e.getDescendants();
			while(cit.hasNext()){
				Content c = cit.next();
				if(c instanceof Element){
					if(((Element)c).getAttribute("id") != null){
						if(id.compareTo("")==0)
							id = ((Element)c).getAttributeValue("id");	//take the starting index to be used for the clasped text
					}
					if(((Element)c).getAttribute("text") != null){
						String t = ((Element)c).getAttributeValue("text");
						text+=t+" ";
						t = t.replaceAll("(\\w\\[|\\])", "");
						chunktext = chunktext.replaceFirst("^"+t, "").trim();
						t = t.replaceFirst("^"+chunktext, "").trim();
						tobedeleted.add((Element)c);//mark to-be-deleted elements						
						if(chunktext.length()==0 || t.length()==0){
							//thechild = (Element)c;
							break;
						}
						
					}
				}
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}

		//delete
		try{
			Iterator<Element> it = tobedeleted.iterator();
			while(it.hasNext()){
				Element c = it.next();
				Element p = c.getParentElement();
				if(c.getChildren().size()==0){
					c.detach(); //remove an element
				}else if(c.getAttribute("text")!=null){
					c.removeAttribute("text");
					c.removeAttribute("id");
				}
				if(p.getChildren().size()==0 && !p.equals(e)){//remove its parent if no more children left
						p.detach();
				}
			}					
		}catch (Exception ex){
			ex.printStackTrace();
		}			
		//format text
		if(chunk.length()>0){//if chunk is "", then collapse all text/child nodes without inserting symbols 
			if(chunk.contains("] o[")){
				//chunk: v[covered] o[r[p[by] o[l[skin or plates]]]]
				//text: covered entirely r[p[by] o[l[skin or plates]]]  
				//after text: b[v[covered] entirely o[r[p[by] o[l[skin or plates]]]]  
				Pattern p = Pattern.compile(pattern);
				Matcher m = p.matcher(text.trim());
				if(m.matches()){
					String extra = m.group(1).trim();
					if(extra.length()>0) text = symbol+"["+chunk.replaceFirst("\\] o\\[", "] "+extra+" o[")+"]";
					else text = symbol+"["+chunk+"]";
				}
			}else{//text: even with, chunk: with
				String chunkstring = chunk.replaceAll("(\\w+\\[|\\])", "");
				text = text.replaceAll(chunkstring,  symbol+"["+chunk+"]");
			}
			//old code			
			//text = text.replaceFirst(pattern, symbol+
			//		"["+chunk+"]"); //this replacement may have no effect on text because the chunk does not consist of consecutive words.
		}
		//set id and text for e
		Attribute a = e.getAttribute("text");
		if(a != null){
			a.setValue(text);	
			e.getAttribute("id").setValue(id);
		}else{
			Attribute n = new Attribute("text", text);
			Attribute n2  = new Attribute("id", id);
			e.setAttribute(n2);
			e.setAttribute(n);
		}
		/*String text = allText(e);
		if(chunk.length()>0){
			text = text.replaceFirst(pattern, symbol+
					"["+chunk+"]"); //this replacement may have no effect on text because the chunk does not consist of consecutive words.
		}
		String id = "";
		try{
			//Attribute idatt = (Attribute)XPath.selectSingleNode(e, ".//@id");
			//id = idatt.getValue();
			Iterator<Content> cit = e.getDescendants();
			while(cit.hasNext()){
				Content c = cit.next();
				if(c instanceof Element){
					if(((Element)c).getAttribute("id") != null){
						id = ((Element)c).getAttributeValue("id");
						break;
					}
				}
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		e.removeContent();
		Attribute a = e.getAttribute("text");
		if(a != null){
			a.setValue(text);	
			e.getAttribute("id").setValue(id);
		}else{
			Attribute n = new Attribute("text", text);
			Attribute n2  = new Attribute("id", id);
			e.setAttribute(n2);
			e.setAttribute(n);
		}*/

		
	}

	/**
	 * NP
	 * 	NP a
	 * 	NP b     => NP
	 * 	CC and        NNS a, b, and c 
	 * 	NP c
	 * 	
	 * @param doc
	 * @return
	 */
	private void collapseNPList(){
		Element root = tree.getRootElement();
		try{
			List<Element> candidates = XPath.selectNodes(root, ".//NP/CC");
			Iterator<Element> it = candidates.iterator();
			while(it.hasNext()){
				Element CC = (Element)it.next();
				Element NP = CC.getParentElement();
				
				
				//all children must be either NP or NN/NNS, except for one CC, and
				//all NP child must have a child NN/NNS
				boolean isList = true;
				if(!CC.getAttributeValue("text").matches("(and|or|plus)")){
					isList = false;
				}
				List<Element> CCs = XPath.selectNodes(NP, "CC");
				if(CCs.size() > 1){
					isList = false;
				}
				List<Element> children = NP.getChildren();
				//if(children.get(children.size()-2).getName().compareTo("CC") != 0){ //second to the last element must be CC
				//	isList = false;
				//} //basal and cauline leaves=> two NN after CC
				Iterator<Element> itc = children.iterator();
				while(itc.hasNext()){
					Element e = itc.next();
					if(!e.getName().matches("\\b(NP|NN|NNS|CC|PUNCT)\\b")){
						isList=false;
					}
					List<Element> echildren = e.getChildren();
					if(echildren.size()>0){
						if(!echildren.get(echildren.size()-1).getName().matches("\\b(NN|NNS)\\b")){
							isList=false;
						}
					}
					if(XPath.selectSingleNode(e, ".//ADJP")!=null ||XPath.selectSingleNode(e, ".//PP")!=null){
						isList=false;
					}
				}
				
				String alltext = allText(NP);
				if(alltext.matches(".*?\\b("+ChunkedSentence.prepositions+"|"+ChunkedSentence.units+")\\b.*")){
					isList=false;
				}
				
				/*Iterator<Content> itc = NP.getDescendants();
				Element last = null;
				while(itc.hasNext()){
					Content c = itc.next();
					if(c instanceof Element){
						Element e = (Element)c;
						last = e;
						String ename = e.getName();
						if(!ename.matches("\\b(NP|NN|NNS|CC|JJ|PUNCT|DT|CD)\\b")){
							isList=false;
						}
						if(ename.compareTo("NP") == 0 && e.getChild("NN")==null && e.getChild("NNS") ==null){
							isList = false;
						}
					}
				}
				if(!last.getName().matches("\\b(NN|NNS)\\b")){//the last word must be a noun
					isList = false;
				}
				*/
				if(isList){//collapse the NP
					Iterator<Content> cit = NP.getDescendants();
					String id = "";
					if(this.printNPlist){
						System.out.println("NPList "+this.sentindex+":"+alltext);
					}
					while(cit.hasNext()){
						Content c = cit.next();
						if(c instanceof Element){
							if(((Element)c).getAttribute("id") != null){
								id = ((Element)c).getAttributeValue("id");
								break;
							}
						}
					}
					//String id = ((Attribute)XPath.selectSingleNode(NP, ".//@id")).getValue();
					NP.removeContent();
					Element NNS = new Element("NNS");
					NNS.setAttribute("text", "l["+alltext+"]");
					NNS.setAttribute("id", id);
					NP.addContent(NNS);
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	 /** PP
	 * 	IN on
	 *  CC or    => PP
	 * 	IN above      IN on or above
	 * 	
	 * @param doc
	 * @return
	 */
	private void collapsePPList(){
		Element root = tree.getRootElement();
		try{
			List<Element> candidates = XPath.selectNodes(root, ".//PP/CC");
			Iterator<Element> it = candidates.iterator();
			while(it.hasNext()){
				Element CC = (Element)it.next();
				Element PP = CC.getParentElement();
				List<Element> children = PP.getChildren();
				//all children must be either PP or IN, except for one CC, and
				//all PP child must have a child IN
				boolean isList = true;
				if(!CC.getAttributeValue("text").matches("(and|or)")){
					isList = false;
				}
				List<Element> CCs = XPath.selectNodes(PP, "CC");
				if(CCs.size() > 1){
					isList = false;
				}
				Iterator<Element> itc = children.iterator();
				int lastin = -1;
				int lastcc = -1;
				int count = 0;
				while(itc.hasNext()){
					Element e = itc.next();
					String ename = e.getName();
					if(!ename.matches("(PP|IN|CC|NP|PUNCT)")){
						isList=false;
					}
					if(ename.matches("(PP|IN)")){
						lastin = count;
					}
					if(ename.matches("CC")){
						lastcc = count;
					}
					if(ename.compareTo("PP") == 0 && e.getChild("IN")==null){
						isList = false;
					}
					if(ename.compareTo("PP") ==0 && e.getChildren().size()>1){
						isList = false;
					}
					count++;
				}
				if(lastin-lastcc != 1){
					isList = false;
				}
				if(isList){//collapse the PP list, but keep the NP of the PP
					//String id = ((Attribute)XPath.selectSingleNode(PP, ".//@id")).getValue();
					Iterator<Content> cit = PP.getDescendants();
					String id = "";
					while(cit.hasNext()){
						Content c = cit.next();
						if(c instanceof Element){
							if(((Element)c).getAttribute("id") != null){
								id = ((Element)c).getAttributeValue("id");
								break;
							}
						}
					}
					Element NP = PP.getChild("NP");
					PP.removeContent(NP);
					String alltext = allText(PP);
					PP.removeContent();
					Element IN = new Element("IN");
					IN.setAttribute("text", "i["+alltext+"]");
					IN.setAttribute("id", id);
					PP.addContent(IN);
					PP.addContent(NP);
				}
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * @param e
	 * @return true if allText of this element contains an organ name, i.e., marked with <> in markedsent
	 */
	private String getOrganFrom(Element e) {
		try{
			List<Element> nouns = XPath.selectNodes(e, ".//NN|.//NNS");
			Iterator<Element> it = nouns.iterator();
			while(it.hasNext()){
				Element noun = it.next();
				String word = noun.getAttribute("text").getValue();
				String index = noun.getAttribute("id").getValue();
				int i = Integer.parseInt(index);
				if(i >= this.posoftokens.length){
					System.out.println();
					return ""; //TODO
				}
				if(this.posoftokens[i].compareToIgnoreCase("NN")==0){
					return this.tokensinsent[i];
				}				
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		return "";
	}

	/**
	 *
	 * @param e
	 * @return a concatnation of value of all text attributes of this element and its descendants
	 */
	static String allText(Element e) {
		if(e == null){
			return "";
		}
		StringBuffer sb = new StringBuffer();
		Attribute t = e.getAttribute("text");
		if(t!=null){
			sb.append(t.getValue()+" ");
		}		
		Iterator<Content> it = e.getDescendants();
		while(it.hasNext()){
			Content cont = it.next();
			if(cont instanceof Element){
				t = ((Element)cont).getAttribute("text");
				if(t!=null){
					sb.append(t.getValue()+" ");
				}
			}
		}		
		return sb.toString().trim();
	}

	/**
	 * establish a mapping between the words of markedsent and the tree 
	 * @param markedsent
	 * @param tree
	 * @return
	 */
	boolean POSMatch() {		
		Iterator<Content> it = this.tree.getDescendants();
		int c = 0;
		while(it.hasNext()){
			Content cont = it.next();
			if(cont instanceof Element){
				Attribute t = ((Element)cont).getAttribute("text");
				if(t!=null){ //only leaf node has a text attribute
					String word=t.getValue();
					String pos = ((Element)cont).getName();
					if(pos.compareToIgnoreCase("PUNCT") != 0){
						if(this.tokensinsent[c].compareToIgnoreCase(word)!=0){
							System.err.println(c+"th token in sentence does not match that in the tree");
							System.exit(1);
						}
						if(this.posoftokens[c].compareTo("") !=0 && this.posoftokens[c].compareToIgnoreCase(pos)!=0){
							return false;
						}
					}
					c++;
				}
			}
		}
		return true;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Document doc = null;
		String xml="<root><e1 text='e1'><e11 text='e11'></e11></e1><e2 text='e2'></e2></root>";
		try {
		     SAXBuilder builder = new SAXBuilder();
		     ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
		     doc = builder.build(bais);
		    } catch (Exception e) {
		      System.out.print("Problem parsing the xml: \n" + e.toString());
		}
		Iterator<Content> it = doc.getDescendants();
			while(it.hasNext()){
				Content cont = it.next();
				if(cont instanceof Element){
					System.out.println(cont.toString());
					Attribute t = ((Element)cont).getAttribute("text");
					if(t!=null){
						String word=t.getValue();
						System.out.println(word);
					}
					
				}
			}   
		    

	}

}

/*examples
check markedsent and find the parts that need to be checked, 
for example: 
to-phrases like "reduced to", "longer than", "same as", "when ...", "in ... view"
one segment has 2 organs

****1 VP is ignored because no NN in it
(ROOT
  (S
    (NP (JJ Apical) (NN flagellomere))
    (VP (VBZ is)
      (NP (DT the) (JJS longest)))
    (. .)))

(ROOT
  (S
    (NP (NNS Leaves))
    (VP (VBD emersed) (, ,)
      (S
        (VP
          (ADVP (RB rarely))
          (VBG floating) (, ,)
          (ADVP (RB petiolate)))))
    (. .)))

(ROOT
  (NP
    (NP
      (NP (NNS Leaves))
      (VP (VBN emersed) (, ,)
        (ADVP (RB rarely))))
    (NP
      (NP (VBG floating))
      (, ,)
      (NP (JJ petiolate)))
    (. .)))

(ROOT
  (NP
    (NP (NNS Leaves))
    (ADJP (JJ emersed) (, ,) (RB rarely) (JJ floating) (, ,) (JJ petiolate))
    (. .)))
    
*****1 IN x: ignored
(ROOT
  (S
    (NP
      (NP (NN body) (NN ovoid))
      (, ,)
      (NP
        (NP (CD 2-4))
        (PP (IN x)
          (NP
            (NP (CD 1-1.5) (NN mm))
            (, ,)
            (ADJP (RB not) (JJ winged)))))
      (, ,))
    (VP (VBZ woolly))
    (. .)))

(ROOT
  (NP
    (NP
      (NP (NN body) (JJ ovoid))
      (, ,)
      (NP
        (NP (CD 2-4))
        (PP (IN x)
          (NP (CD 1-1.5) (NN mm))))
      (, ,))
    (ADJP (RB not) (JJ winged) (, ,) (JJ woolly))
    (. .)))

(ROOT
  (NP
    (NP
      (NP
        (NP (NNS teeth))
        (NP (CD 5))
        (, ,)
        (ADVP (CD erect) (TO to) (CD spreading)))
      (, ,)
      (NP (CD 1)))
    (: -)
    (NP (CD 3) (NN mm))
    (. .)))

(ROOT
  (FRAG
    (NP
      (NP (NNS teeth) (CD 5))
      (, ,)
      (ADJP (JJ erect) (TO to) (JJ spreading)))
    (, ,)
    (NP (CD 1/-3) (NN mm))
    (. .)))

******"when young" is not a relation but a constraint
(ROOT
  (S
    (NP (JJ basal))
    (VP (VBP rosette)
      (UCP
        (ADJP (JJ absent)
          (CC or)
          (RB poorly) (JJ developed))
        (CC and)
        (VP (VBG withering)
          (SBAR
            (WHADVP (WRB when))
            (S
              (ADJP (JJ young)))))))
    (. .)))

(ROOT
  (NP
    (NP
      (NP (JJ basal) (NN rosette))
      (UCP
        (ADJP (JJ absent)
          (CC or)
          (RB poorly) (JJ developed))
        (CC and)
        (VP (VBG withering)
          (SBAR
            (WHADVP (WRB when))
            (S
              (ADJP (JJ young)))))))
    (. .)))

(ROOT
  (NP
    (NP
      (NP (NNP Ray))
      (ADJP (JJ laminae)
        (NP (CD 6))))
    (: -)
    (NP
      (NP
        (NP (CD 7) (NNS x))
        (NP (CD 2/CD-32) (NN mm)))
      (, ,)
      (PP (IN with)
        (NP (CD 2))))
    (: -)
    (NP
      (NP (CD 5) (NNS hairs))
      (PP (IN inside)
        (NP
          (NP (NN opening))
          (PP (IN of)
            (NP (NN tube))))))
    (. .)))

****nested PPs (not in VB)
****1. start with the PP(IN with no other PP(IN in it
****2. extract the relation then replace the of-phrase with the full string NP e.g. (NP opening of tube)
****3. go to step 1
(ROOT
  (NP
    (NP (NN Ray) (NNS laminae))
    (NP
      (NP
        (QP (CD 6-7) (IN x) (CD 2-32))
        (NN mm))
      (, ,)
      (PP (IN with)
        (NP
          (NP (CD 2-5) (NNS hairs))
          (PP (IN inside)
            (NP
              (NP (NN opening))
              (PP (IN of)
                (NP (NN tube))))))))
    (. .)))



(ROOT
  (S
    (VP (VBZ veins)
      (NP
        (NP (CD 1))
        (, ,)
        (UCP
          (ADJP (RB mostly) (JJ prominent))
          (, ,)
          (ADJP
            (ADVP (RB longer)
              (PP (IN than)
                (NP
                  (NP (NN extension))
                  (PP (IN of)
                    (NP (NN air))))))
            (JJ spaces))
          (CC or)
          (VP (VBG running)
            (PP (IN through)
              (NP
                (NP
                  (QP (IN at) (JJS least) (CD 3/4)))
                (PP (IN of)
                  (NP
                    (NP (NN distance))
                    (PP (IN between)
                      (NP (NN node)
                        (CC and)
                        (NN apex)))))))))))
    (. .)))
    
    
****1 Collapse QP phrases
****2 collapse "extension of air spaces":extension is not an organ name
****3 collapse "distance between node and apex": distance is not an organ name
****4 do "longer than extension...": relation "longer than", collapse the ADJP node
****5 collapse "at least 3/4 of distance"
****6 do "running through": relation, collapse the VP node
****in 4 and 6, use the depth of the relation node to find entity1
(ROOT
  (NP
    (NP
      (NP (NNS veins) (CD 1))
      (, ,)
      (UCP
        (ADJP (RB mostly) (JJ prominent))
        (, ,)
        (ADJP (JJR longer)
          (PP (IN than)
            (NP
              (NP (NN extension))
              (PP (IN of)
                (NP (NN air) (NNS spaces))))))
        (CC or)
        (VP (VBG running)
          (PP (IN through)
            (NP
              (NP
                (QP (IN at) (JJS least) (CD 3/4)))
              (PP (IN of)
                (NP
                  (NP (NN distance))
                  (PP (IN between)
                    (NP (NN node)
                      (CC and)
                      (NN apex))))))))))
    (. .)))

****1 if decurrent is marked {decurrent} in markedsent BUT the tree has it as a VB, then report failure.
****2 otherwise, the PP (on distal phyllary margines) should be a constraint to the JJ decurrent.
(ROOT
  (S
    (NP (JJ erect) (NNS appendages))
    (VP (VB decurrent)
      (PP (IN on)
        (NP (JJ distal) (NN phyllary) (NNS margins)))
      (, ,)
      (NP (JJ dark) (JJ brown)
        (CC or)
        (JJ black) (, ,) (NNS scarious)))
    (. .)))

(ROOT
  (NP
    (NP (JJ erect) (NNS appendages))
    (ADJP
      (ADJP (JJ decurrent)
        (PP (IN on)
          (NP (JJ distal) (NN phyllary) (NNS margins))))
      (, ,)
      (ADJP (JJ dark) (JJ brown))
      (CC or)
      (ADJP (JJ black))
      (, ,)
      (ADJP (JJ scarious)))
    (. .)))

(ROOT
  (S
    (S
      (VP (VBZ stems)
        (S
          (VP (VBG arising)
            (PP
              (PP (IN at)
                (NP
                  (NP (NNS nodes))
                  (PP (IN of)
                    (NP (JJ caudex) (NNS branches)))))
              (CC and)
              (PP (IN at)
                (NP
                  (NP (JJ distal) (NNS nodes))
                  (PP (IN of)
                    (NP (JJ short))))))))))
    (, ,)
    (NP
      (NP (JJ nonflowering) (JJ aerial) (NNS branches))
      (, ,)
      (NP (CD 1-4) (NN dm))
      (, ,))
    (ADVP (RB essentially))
    (VP (VBZ glabrous))
    (. .)))

****1 do "nodes of caudex branches" => relation
****2 do "distal nodes of short, nonflowering aerial branches" => relation
****3 do "arising at nodes" => relation
****4 do "arising at distal nodes" =>relation How to get to arising in this case? the (CC and) is a clue?
*

(ROOT
  (NP
    (NP (NNS stems))
    (VP (VBG arising)
      (PP
        (PP (IN at)
          (NP
            (NP (NNS nodes))
            (PP (IN of)
              (NP (NN caudex) (NNS branches)))))
        (CC and)
        (PP (IN at)
          (NP
            (NP
              (NP (JJ distal) (NNS nodes))
              (PP (IN of)
                (NP (JJ short) (, ,) (JJ nonflowering) (JJ aerial) (NNS branches))))
            (, ,)
            (NP (CD 1-4) (NN dm))
            (, ,)
            (ADJP (RB essentially) (JJ glabrous))))))
    (. .)))

(ROOT
  (S
    (NP
      (NP (JJ Rhizomes) (NN horizontal))
      (, ,)
      (VP (VBG creeping)
        (PP (IN at)
          (CC or)
          (IN near)
          (NP (NN surface))))
      (, ,))
    (VP (VBN branched))
    (. .)))

*****1 do "at or near surface", treat it as a constraint to state "creeping"
(ROOT
  (S
    (NP
      (NP (NNS Rhizomes) (JJ horizontal))
      (, ,)
      (VP (VBG creeping)
        (PP (IN at)
          (CC or)
          (IN near)
          (NP (NN surface))))
      (, ,))
    (VP (JJ branched))
    (. .)))

(ROOT
  (S
    (NP (NNS achenes))
    (ADVP (RB obliquely))
    (VP
      (VP (VBD ovoid))
      (, ,)
      (NP
        (NP (CD 1) (NN mm))
        (, ,)
        (ADJP (RB abaxially) (JJ rounded))
        (, ,)
        (PP (IN with)
          (NP (CD 1) (JJ abaxial) (NN groove)))))
    (. .)))

*****1 use the only organ name in the sentence as entity1 of the relation with. Not the nearest NP, which is mm.
***** use the closest organ name? consider not only the distance but also the depths of the nodes.  
(ROOT
  (NP
    (NP
      (NP (NNS achenes))
      (ADVP (RB obliquely)))
    (ADJP (JJ ovoid))
    (, ,)
    (NP
      (NP (CD 1) (NN mm))
      (, ,)
      (ADJP (RB abaxially) (JJ rounded)))
    (, ,)
    (PP (IN with)
      (NP (CD 1) (JJ abaxial) (NN groove)))
    (. .)))

(ROOT
  (S
    (NP
      (NP (JJ Drupe) (NN red))
      (, ,)
      (NP (NN globose))
      (, ,))
    (VP (VBN seated)
      (PP (IN in)
        (NP (JJ small) (, ,) (JJ single-rimmed) (NN cupule))))
    (. .)))

(ROOT
  (S
    (NP
      (NP (NN Drupe) (JJ red))
      (, ,)
      (ADJP (JJ globose))
      (, ,))
    (VP (VBN seated)
      (PP (IN in)
        (NP (JJ small) (, ,) (JJ single-rimmed) (NN cupule))))
    (. .)))

(ROOT
  (S
    (VP (VBZ stamens)
      (NP
        (NP (RB mostly) (CD 6-10))
        (, ,)
        (NP
          (NP
            (QP (RB as) (JJ few) (IN as) (CD 3)))
          (PP (IN in)
            (NP (JJR more) (JJ distal) (NNS flowers))))))
    (. .)))
****1 unlike the previous example, here "in more distal flowers" should be a constraint for "as few as 3" 
****2. output of the extract function should be arrayList of things (relation and state/constraint)?
(ROOT
  (NP
    (NP (NNS stamens))
    (NP
      (NP (RB mostly) (CD 6-10))
      (, ,)
      (NP
        (NP
          (QP (RB as) (JJ few) (IN as) (CD 3)))
        (PP (IN in)
          (NP (JJR more) (JJ distal) (NNS flowers)))))
    (. .)))

(ROOT
  (S
    (NP (NNS Capsules))
    (VP (VBD exserted) (, ,)
      (S
        (NP (NNS valves))
        (VP (VBG separating)
          (PP (IN at)
            (NP (NN dehiscence))))))
    (. .)))
*****separating at a time??
*****1 dehiscence is not an organ.so this is not a relation.
(ROOT
  (NP
    (NP (NNS Capsules) (JJ exserted))
    (, ,)
    (NP
      (NP (NNS valves))
      (VP (VBG separating)
        (PP (IN at)
          (NP (NN dehiscence)))))
    (. .)))

(ROOT
  (S
    (NP
      (NP (NNP Capsules) (NNP brown))
      (, ,)
      (VP (VBN ellipsoid))
      (, ,))
    (VP (VBZ shorter)
      (PP (IN than)
        (NP (NNS tepals))))
    (. .)))

(ROOT
  (NP
    (NP (NNS Capsules))
    (ADJP
      (ADJP (JJ brown))
      (, ,)
      (ADJP (JJ ellipsoid))
      (, ,)
      (ADJP (JJR shorter)
        (PP (IN than)
          (NP (NNS tepals)))))
    (. .)))

(ROOT
  (S
    (NP
      (NP (NNS Heads))
      (ADJP (RB mostly) (VBN scattered)))
    (ADVP (RB along))
    (VP (VBZ stems))
    (. .)))

*****1 search for PP(IN
*****2 then VBs "scattered along stems"
(ROOT
  (S
    (NP (NNS Heads))
    (ADVP (RB mostly))
    (VP (VBN scattered)
      (PRT (RB along))
      (NP (NNS stems)))
    (. .)))


Bracket Labels
Clause Level

S - simple declarative clause, i.e. one that is not introduced by a (possible empty) subordinating conjunction or a wh-word and that does not exhibit subject-verb inversion.
SBAR - Clause introduced by a (possibly empty) subordinating conjunction.
SBARQ - Direct question introduced by a wh-word or a wh-phrase. Indirect questions and relative clauses should be bracketed as SBAR, not SBARQ.
SINV - Inverted declarative sentence, i.e. one in which the subject follows the tensed verb or modal.
SQ - Inverted yes/no question, or main clause of a wh-question, following the wh-phrase in SBARQ.
Phrase Level
ADJP - Adjective Phrase.
ADVP - Adverb Phrase.
CONJP - Conjunction Phrase.
FRAG - Fragment.
INTJ - Interjection. Corresponds approximately to the part-of-speech tag UH.
LST - List marker. Includes surrounding punctuation.
NAC - Not a Constituent; used to show the scope of certain prenominal modifiers within an NP.
NP - Noun Phrase.
NX - Used within certain complex NPs to mark the head of the NP. Corresponds very roughly to N-bar level but used quite differently.
PP - Prepositional Phrase.
PRN - Parenthetical.
PRT - Particle. Category for words that should be tagged RP.
QP - Quantifier Phrase (i.e. complex measure/amount phrase); used within NP.
RRC - Reduced Relative Clause.
UCP - Unlike Coordinated Phrase.
VP - Vereb Phrase.
WHADJP - Wh-adjective Phrase. Adjectival phrase containing a wh-adverb, as in how hot.
WHAVP - Wh-adverb Phrase. Introduces a clause with an NP gap. May be null (containing the 0 complementizer) or lexical, containing a wh-adverb such as how or why.
WHNP - Wh-noun Phrase. Introduces a clause with an NP gap. May be null (containing the 0 complementizer) or lexical, containing some wh-word, e.g. who, which book, whose daughter, none of which, or how many leopards.
WHPP - Wh-prepositional Phrase. Prepositional phrase containing a wh-noun phrase (such as of which or by whose authority) that either introduces a PP gap or is contained by a WHNP.
X - Unknown, uncertain, or unbracketable. X is often used for bracketing typos and in bracketing the...the-constructions.
Word level
CC - Coordinating conjunction
CD - Cardinal number
DT - Determiner
EX - Existential there
FW - Foreign word
IN - Preposition or subordinating conjunction
JJ - Adjective
JJR - Adjective, comparative
JJS - Adjective, superlative
LS - List item marker
MD - Modal
NN - Noun, singular or mass
NNS - Noun, plural
NNP - Proper noun, singular
NNPS - Proper noun, plural
PDT - Predeterminer
POS - Possessive ending
PRP - Personal pronoun
PRP$ - Possessive pronoun (prolog version PRP-S)
RB - Adverb
RBR - Adverb, comparative
RBS - Adverb, superlative
RP - Particle
SYM - Symbol
TO - to
UH - Interjection
VB - Verb, base form
VBD - Verb, past tense
VBG - Verb, gerund or present participle
VBN - Verb, past participle
VBP - Verb, non-3rd person singular present
VBZ - Verb, 3rd person singular present
WDT - Wh-determiner
WP - Wh-pronoun
WP$ - Possessive wh-pronoun (prolog version WP-S)
WRB - Wh-adverb
Function tags
Form/function discrepancies
-ADV (adverbial) - marks a constituent other than ADVP or PP when it is used adverbially (e.g. NPs or free ("headless" relatives). However, constituents that themselves are modifying an ADVP generally do not get -ADV. If a more specific tag is available (for example, -TMP) then it is used alone and -ADV is implied. See the Adverbials section.
-NOM (nominal) - marks free ("headless") relatives and gerunds when they act nominally.
Grammatical role
-DTV (dative) - marks the dative object in the unshifted form of the double object construction. If the preposition introducing the "dative" object is for, it is considered benefactive (-BNF). -DTV (and -BNF) is only used after verbs that can undergo dative shift.
-LGS (logical subject) - is used to mark the logical subject in passives. It attaches to the NP object of by and not to the PP node itself.
-PRD (predicate) - marks any predicate that is not VP. In the do so construction, the so is annotated as a predicate.
-PUT - marks the locative complement of put.
-SBJ (surface subject) - marks the structural surface subject of both matrix and embedded clauses, including those with null subjects.
-TPC ("topicalized") - marks elements that appear before the subject in a declarative sentence, but in two cases only:

   1. if the front element is associated with a *T* in the position of the gap.
   2. if the fronted element is left-dislocated (i.e. it is associated with a resumptive pronoun in the position of the gap). 

-VOC (vocative) - marks nouns of address, regardless of their position in the sentence. It is not coindexed to the subject and not get -TPC when it is sentence-initial.
Adverbials

Adverbials are generally VP adjuncts.

-BNF (benefactive) - marks the beneficiary of an action (attaches to NP or PP).
This tag is used only when (1) the verb can undergo dative shift and (2) the prepositional variant (with the same meaning) uses for. The prepositional objects of dative-shifting verbs with other prepositions than for (such as to or of) are annotated -DTV.
-DIR (direction) - marks adverbials that answer the questions "from where?" and "to where?" It implies motion, which can be metaphorical as in "...rose 5 pts. to 57-1/2" or "increased 70% to 5.8 billion yen" -DIR is most often used with verbs of motion/transit and financial verbs.
-EXT (extent) - marks adverbial phrases that describe the spatial extent of an activity. -EXT was incorporated primarily for cases of movement in financial space, but is also used in analogous situations elsewhere. Obligatory complements do not receive -EXT. Words such as fully and completely are absolutes and do not receive -EXT.
-LOC (locative) - marks adverbials that indicate place/setting of the event. -LOC may also indicate metaphorical location. There is likely to be some varation in the use of -LOC due to differing annotator interpretations. In cases where the annotator is faced with a choice between -LOC or -TMP, the default is -LOC. In cases involving SBAR, SBAR should not receive -LOC. -LOC has some uses that are not adverbial, such as with place names that are adjoined to other NPs and NAC-LOC premodifiers of NPs. The special tag -PUT is used for the locative argument of put.
-MNR (manner) - marks adverbials that indicate manner, including instrument phrases.
-PRP (purpose or reason) - marks purpose or reason clauses and PPs.
-TMP (temporal) - marks temporal or aspectual adverbials that answer the questions when, how often, or how long. It has some uses that are not strictly adverbial, auch as with dates that modify other NPs at S- or VP-level. In cases of apposition involving SBAR, the SBAR should not be labeled -TMP. Only in "financialspeak," and only when the dominating PP is a PP-DIR, may temporal modifiers be put at PP object level. Note that -TMP is not used in possessive phrases.
Miscellaneous
-CLR (closely related) - marks constituents that occupy some middle ground between arguments and adjunct of the verb phrase. These roughly correspond to "predication adjuncts", prepositional ditransitives, and some "phrasel verbs". Although constituents marked with -CLR are not strictly speaking complements, they are treated as complements whenever it makes a bracketing difference. The precise meaning of -CLR depends somewhat on the category of the phrase.

    * on S or SBAR - These categories are usually arguments, so the -CLR tag indicates that the clause is more adverbial than normal clausal arguments. The most common case is the infinitival semi-complement of use, but there are a variety of other cases.
    * on PP, ADVP, SBAR-PRP, etc - On categories that are ordinarily interpreted as (adjunct) adverbials, -CLR indicates a somewhat closer relationship to the verb. For example:
          o Prepositional Ditransitives
            In order to ensure consistency, the Treebank recognizes only a limited class of verbs that take more than one complement (-DTV and -PUT and Small Clauses) Verbs that fall outside these classes (including most of the prepositional ditransitive verbs in class [D2]) are often associated with -CLR.
          o Phrasal verbs
            Phrasal verbs are also annotated with -CLR or a combination of -PRT and PP-CLR. Words that are considered borderline between particle and adverb are often bracketed with ADVP-CLR.
          o Predication Adjuncts
            Many of Quirk's predication adjuncts are annotated with -CLR. 
    * on NP - To the extent that -CLR is used on NPs, it indicates that the NP is part of some kind of "fixed phrase" or expression, such as take care of. Variation is more likely for NPs than for other uses of -CLR. 

-CLF (cleft) - marks it-clefts ("true clefts") and may be added to the labels S, SINV, or SQ.
-HLN (headline) - marks headlines and datelines. Note that headlines and datelines always constitute a unit of text that is structurally independent from the following sentence.
-TTL (title) - is attached to the top node of a title when this title appears inside running text. -TTL implies -NOM. The internal structure of the title is bracketed as usual.

*/