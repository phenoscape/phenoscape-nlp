 /* $Id: ChunkedSentence.java 988 2011-09-23 16:44:53Z hong1.cui $ */
/**
 * 
 */
package fna.charactermarkup;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Document;
import org.jdom.Element;




/**
 * 
 * @author hongcui
 *	This class generates a chunked sentence from the parsing tree and provides a set of access methods to facilitate final annotation.
 *	A chunked sentence is a marked sentence (with organs enclosed by <> and states by {}) with "chunks" of text enclosed by [], for example 
 *	<Heads> 3 , {erect} , [in corymbiform or paniculiform arrays]. (sent. 302)
 *
 *	the annotation of a chunk may require access to the original parsing tree, but that is not handled by this class.
 */

@SuppressWarnings("unchecked")
public class ChunkedSentence {
	private String glosstable = null;
	private String markedsent = null;
	private String chunkedsent = null;
	private ArrayList<String> chunkedtokens = null;
	@SuppressWarnings("unused")
	private ArrayList<String> charactertokensReversed = new ArrayList<String>();
	private int pointer = 0; //pointing at the next chunk to be annotated
	private String subjecttext = null;
	private String text = null;
	private String sentsrc = null;
	private String tableprefix = null;
	public static final String binaryTvalues = "true|yes|usually";
	public static final String binaryFvalues = "false|no|rarely";
	public static final String pronouns = "them";
	public static final String locationpp="near|from";
	public static final String units= "cm|mm|dm|m|meter|meters|microns|micron|unes|µm|um";
	public static final String percentage="%|percent";
	public static final String degree="°|degree|degrees";
	public static final String times = "times|folds|lengths|widths";
	public static final String per = "per";
	public static final String more="greater|more|less|fewer";
	public static final String counts="few|several|many|none|numerous|single|couple";
	public static final String basecounts="each|every|per";
	public static final String pairs="pair|pairs|series|array|arrays|row|rows";
	public static final String clusters="cluster|clusters|involucre|involucres|rosette|rosettes|pair|pairs|series|ornament|ornamentation|array|arrays";
	public static final String prepositions = "above|across|after|along|among|amongst|around|as|at|before|behind|beneath|between|beyond|by|for|from|in|into|near|of|off|on|onto|out|outside|over|than|throughout|to|toward|towards|up|upward|with|without";
	public static final String stop = "a|about|above|across|after|along|also|although|amp|an|and|are|as|at|be|because|become|becomes|becoming|been|before|being|beneath|between|beyond|but|by|ca|can|could|did|do|does|doing|done|for|from|had|has|have|hence|here|how|if|in|into|inside|inward|is|it|its|may|might|more|most|near|no|not|of|off|on|onto|or|out|outside|outward|over|should|so|than|that|the|then|there|these|this|those|throughout|to|toward|towards|up|upward|was|were|what|when|where|which|why|with|within|without|would";
	public static final String skip = "and|becoming|if|or|that|these|this|those|to|what|when|where|which|why|not|throughout";
	public static final String positionprep = "of|part_of|in|on|between";
	public static Hashtable<String, String> eqcharacters = new Hashtable<String, String>();
	private boolean inSegment = false;
	private boolean rightAfterSubject = false;
	@SuppressWarnings("unused")
	private int sentid = -1;
	private ArrayList<String> pastpointers = new ArrayList<String>();

	public String unassignedmodifier = null;
	//caches
	public static Hashtable<String, String> characterhash = new Hashtable<String, String>();
	public static ArrayList<String> adverbs = new ArrayList<String>();
	public static ArrayList<String> verbs = new ArrayList<String>();
	public static ArrayList<String> nouns = new ArrayList<String>();
	public static ArrayList<String> notadverbs = new ArrayList<String>();
	public static ArrayList<String> notverbs = new ArrayList<String>();
	public static ArrayList<String> notnouns = new ArrayList<String>();
	
	protected Connection conn = null;
	/*static protected String username = "root";
	static protected String password = "root";
	static protected String database = "fnav19_benchmark";*/
	
	private boolean printNorm = false;
	private boolean printNormThan = false;
	private boolean printNormTo = false;
	private boolean printExp = false;
	private boolean printRecover = false;
	private String clauseModifierConstraint;
	private String clauseModifierContraintId;
	

	
	public ChunkedSentence(ArrayList<String> chunkedtokens, String chunkedsent, Connection conn, String glosstable, String tableprefix){
		this.chunkedtokens = chunkedtokens;
		this.chunkedsent = chunkedsent;
		this.conn = conn;
		this.glosstable = glosstable;
		this.tableprefix = tableprefix;
		this.recoverOrgans();
		
	}
	/**
	 * @param tobechunkedmarkedsent 
	 * @param tree 
	 * 
	 */
	public ChunkedSentence(int id, Document collapsedtree, Document tree, String tobechunkedmarkedsent,  String sentsrc, String tableprefix,Connection conn, String glosstable) {
		eqcharacters.put("wide", "width"); //2 cm. wide
		eqcharacters.put("long", "length");
		eqcharacters.put("broad", "width");
		eqcharacters.put("diam", "diameter");
		//eqcharacters.put("width", "width");
		//eqcharacters.put("length", "length");
		//eqcharacters.put("depth", "depth");
		//eqcharacters.put("breadth", "width");
				
		this.tableprefix = tableprefix;
		this.glosstable = glosstable;
		this.conn = conn;
		
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select term from "+glosstable+" where category='character'");
			while(rs.next()){
				nouns.add(rs.getString("term"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
		this.sentsrc = sentsrc;
		this.sentid = id;
		this.markedsent = tobechunkedmarkedsent;
		//tobechunkedmarkedsent = tobechunkedmarkedsent.replaceAll("[\\[\\(]", " -LRB-/-LRB- ").replaceAll("[\\]\\)]", " -RRB-/-RRB- ").replaceAll("\\s+", " ").trim(); 
		tobechunkedmarkedsent = tobechunkedmarkedsent.replaceAll("[\\[\\(]", "-LRB-/-LRB-").replaceAll("[\\]\\)]", "-RRB-/-RRB-").trim(); 
		if(tobechunkedmarkedsent.matches(".*?\\d.*")){
			tobechunkedmarkedsent = NumericalHandler.normalizeNumberExp(tobechunkedmarkedsent);
		}
		String[] temp = tobechunkedmarkedsent.split("\\s+");
		chunkedtokens = new ArrayList<String>(Arrays.asList(temp)); //based on markedsent, which provides <>{} tags.
				
		Element root = collapsedtree.getRootElement();
		String treetext = SentenceChunker4StanfordParser.allText(root).trim();
		String[] treetoken = treetext.split("\\s+"); //based on the parsing tree, which holds some chunks.
		String realchunk = "";
		ArrayList<String> brackets = new ArrayList<String>();
		int i = 0;
		//go through treetoken to chunk state lists, and brackets
		for(; i<treetoken.length; i++){
			if(treetoken[i].matches("^\\S+~list~\\S+")){//r[p[of] o[{architecture~list~smooth~or~barbellulate~to~plumose} (bristles)]]
				//String[] parts = treetoken[i].split("~list~");
				//treetoken[i] = parts[0]+"["+parts[1]+"]"; 
				//treetoken[i] = treetoken[i].replace("~list~", "[{").replaceAll("\\{(?=\\w{2,}\\[)", "").replaceAll("(?<=~[a-z0-9-]{2,40})(\\}| |$)","}]");
				treetoken[i] = treetoken[i].replace("~list~", "[{").replaceAll("\\{(?=\\w{2,}\\[)", "").replaceAll("(?<=~[a-z0-9-]{1,40})(\\}| |$)","}]");
			}
		}		
		for(i= 0; i<treetoken.length; i++){
			if(treetoken[i].indexOf('[') >=0){
				int bcount  = treetoken[i].replaceAll("[^\\[]", "").trim().length();
				for(int j = 0; j < bcount; j++){
					brackets.add("[");
				}
			}

			if(brackets.size()>0){//in
				//restore original number expressions
				String w = treetoken[i].replaceAll("(\\w+\\[|\\])", "");
				realchunk += treetoken[i].replace(w, chunkedtokens.get(i))+" ";
				chunkedtokens.set(i, "");
			}
			
			if(treetoken[i].indexOf(']')>=0){
				int bcount  = treetoken[i].replaceAll("[^\\]]", "").trim().length();
				for(int j = 0; j < bcount; j++){
					brackets.remove(0);
				}
			}
			
			if(brackets.size()==0 && realchunk.length()>0){
				chunkedtokens.set(i, realchunk.replaceAll("<", "(").replaceAll(">", ")").trim()); //inside a chunk, an organ is marked by #. e.g. #leaves# 
				realchunk="";
			}
			
		}
		if(realchunk.length()>0){
			chunkedtokens.set(i-1+0, realchunk.trim());
		}		
		this.chunkedsent = "";
		
		
		int discoveredchunks = 0;

		discoveredchunks += normalizeOtherINs(); //find objects for those VB/IN that without
		discoveredchunks += normalizeBetween();
		discoveredchunks +=normalizeThan();
		discoveredchunks +=normalizeTo();
		normalizeUnits();
		int allchunks = chunks();
		StanfordParser.countChunks(allchunks, discoveredchunks);
		
		recoverVPChunks();//recover unidentified verb phrases
		recoverConjunctedOrgans(); //
		//findSubject(); no longer needed //set the pointer to a place right after the subject, assuming the subject part is stable in chunkedtokens at this time
		recoverOrgans();
		segmentSent();//insert segment marks in chunkedtokens while producing this.chunkedsent
		
		//TODO move this to an earlier place
		//if the last words in l[] are marked with {}, take them out of the chunk
		if(this.chunkedsent.matches(".*?l\\[[^\\[].*?}\\].*")){
			removeStateFromList();
		}
	}
	
	/**
	 * count the chunks in chunkedtokens
	 * @return
	 */
	private int chunks() {
		int count = 0;
		Iterator<String> it = this.chunkedtokens.iterator();
		while(it.hasNext()){
			if(it.next().matches("[^l]\\[.*")){
				count++;
			}
		}
		return count;
	}
	/**
	 * scan through a chunkedtokens to find Verbs not parsed as such by the parser
	 * find verbs by
	 * 1. look into this.verbs
	 * 2. find pattern o ting/ted by o, then t must be a verb and save this verb in verbs
	 */
	private void recoverVPChunks() {
		for(int i = 0; i < this.chunkedtokens.size(); i++){
			String t = this.chunkedtokens.get(i);
			if(t.contains("-")) continue; //check 751
			if(!t.contains("[") && this.verbs.contains(t)){
				recoverVPChunk(i);
			}else if(!t.contains("[") && (t.endsWith("ing")|| t.endsWith("ing}"))){
				 if(connects2organs(i)){
					 ChunkedSentence.verbs.add(t.replaceAll("\\W", ""));
					 recoverVPChunk(i);
				 }
			}/*else if(!t.contains("[")&& t.endsWith("ed") && this.chunkedtokens.size()>i+1 && this.chunkedtokens.get(i+1).matches(".*?\\bby\\b.*")){				
			}*/
		}
	}

	/**
	 * 
	 * @param i :index of the verb
	 * @return
	 */
	private boolean connects2organs(int i) {
		boolean organ1 = false;
		boolean organ2 = false;
		if(i>=1 && this.chunkedtokens.size()>i+1){
			String t = this.chunkedtokens.get(i-1);
			if(t.endsWith(">") || t.matches(".*\\bo\\[[^\\]\\[]*\\]+") || t.endsWith(")") ){
				organ1 = true;
			}
			
			do{
				i++;
				t = this.chunkedtokens.get(i).trim();
			}while(t.length()==0);
			if(t.endsWith(">") || t.matches("[uz]?\\[?\\bo\\[[^\\]\\[]*\\]+") || t.endsWith(")") ){
				organ2 = true;
			}
			
			/*for(int j = i+1; j < this.chunkedtokens.size(); j++){
				t = this.chunkedtokens.get(j);
				if(t.endsWith(">") || t.matches("[uz]?\\[?\\bo\\[[^\\]\\[]*\\]+") || t.endsWith(")") ){
					organ2 = true;
					break;
				}
				if((j == i+1 && t.equals(","))|| t.matches("\\w+")){
					organ2 = false;
					break;
				}
			}*/
		}
		return organ1 && organ2;
	}
	/**
	 * 
	 * @param i: the index of a possible verb
	 */
	private void recoverVPChunk(int i) {
	 
		String chunk = "";
		boolean foundo = false;
		int j = i+1;
		for(; j < chunkedtokens.size(); j++){
			//scan for the end of the chunk TODO: may refactor with normalizeOtherINs on this search
			String t = this.chunkedtokens.get(j);
			if(j==i+1 && t.matches(",")){ //verb not have object
				return;
			}
			if(t.matches("(;|\\.)")) break;
			if(foundo && (t.contains("{") || t.contains("~list~")||t.matches("(\\w+|,|;|\\.)")||t.contains("["))){
				break;
			}
			if(t.contains("<")){
				chunk += t+" ";
				foundo = true;
			}else if(t.matches(".*?\\bo\\[[^\\]*]+") || t.matches(".*?l\\[[^\\]]*\\]+")){//found noun)
				chunk += t+" ";
				foundo = true;
				j++;
				break;
			}else{
				chunk += t+" ";
			}						
		}
		if(!foundo) return;
		//format the chunk
		chunk = chunk.trim();
		if(chunk.endsWith(">")){
			chunk = "b[v["+this.chunkedtokens.get(i)+"]"+" o["+chunk.replaceAll("<", "(").replaceAll(">", ")")+"]]";
		}else if(chunk.matches(".*?\\bo\\[.*\\]+")){
			if(chunk.contains(" v[")){
				chunk = chunk.replaceFirst(" v[", " v["+this.chunkedtokens.get(i)+" ");
			}else if(chunk.matches("^r\\[.*")){//t[c[{extending}] r[p[to] o[(midvalve)]]]
				//chunk = chunk.replaceFirst("^r[p[", "b[v["+this.chunkedtokens.get(i)+ " "); //need to make the v is taken as a relation in processChunkVP
				chunk = "t[c["+this.chunkedtokens.get(i)+"] "+chunk+"]";
			}else if(chunk.startsWith("l[")){
				chunk = "b[v["+this.chunkedtokens.get(i)+"] "+chunk.replaceFirst("^l\\[", "o[")+"]"; 
			}else if(chunk.startsWith("u[")){
				chunk = chunk.replaceFirst("^u[", "b[v["+this.chunkedtokens.get(i)+ "] "); 
			}
		}
		//this.chunkedtokens.set(i, chunk);
		if(this.printRecover){
			System.out.println("verb chunk formed: "+chunk +" for \n"+this.sentid+"["+this.sentsrc+"]"+this.markedsent);
		}
		for(int k = i; k<j; k++){
			this.chunkedtokens.set(k, "");
		}
		this.chunkedtokens.set(j-1, chunk);
			/*
			t = t.replaceFirst("^u\\[", "").replaceFirst("\\]$", "");
			String o = t.substring(t.indexOf("o[")).trim();
			t = t.substring(0, t.indexOf("o[")).trim();
			if(t.length()>0){
				String[] states = t.split("\\s+");
				for(int k = 0; k < states.length; k++){
					String ch = Utilities.lookupCharacter(states[k], conn, characterhash, glosstable);
					if(ch!=null){
						scs = (scs.trim().length()>0? scs.trim()+"] ": "")+ch+"["+states[k].replaceAll("[{}]", "")+" ";
					}else{
						scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"m["+states[k].replaceAll("[{}]", "")+" ";
					}
				}		
			}
			scs = (scs.trim().length()>0? scs.trim()+"] ": "")+o;
		}*/
		
	}
	/**
	 * attempts to mark modified non-subject organs as a chunk to avoid characters of these organs be attached to previous organs
	 * run this after recoverConjunctedOrgans to exclude organs that are objects of VP/PP-phrases)
	 * does not attempt to recognize conjunctions as the decisions may be context-dependent
	 */
	private void recoverOrgans() {
		//for(int i = this.chunkedtokens.size()-1; i >=this.pointer; i--){
		for(int i = this.chunkedtokens.size()-1; i >=0; i--){
			String t = this.chunkedtokens.get(i);
			if(t.endsWith(">") || t.endsWith(")")){//TODO: not dealing with nplist at this time, may be later
				recoverOrgan(i);//chunk and update chunkedtokens
			}
		}		
	}
	
	/**
	 * 
	 * @param last: the index of the last part of an organ name
	 */
	private void recoverOrgan(int last) {
		String chunk = this.chunkedtokens.get(last);
		boolean foundm = false; //modifiers
		boolean subjecto = false;
		int i = last-1;
		//for(;i >=this.pointer; i--){
		for(;i >=0; i--){
			String t = this.chunkedtokens.get(i);
			/*preventing "the" from blocking the organ following ",the" to being matched as a subject organ- mohan 10/19/2011*/
			if(t.matches("the|a|an")){
				if(i!=0){
					i=i-1;
					t = this.chunkedtokens.get(i);
				}
			}			
			/*end mohan*/
			if(t.matches("\\{[\\w-]+\\}")|| t.matches("\\d+") || t.contains("~list~")){
				chunk = t+" "+chunk;
				foundm = true;
			}else if(!foundm && (t.endsWith(">") ||t.endsWith(")") )){ //if m o m o, collect two chunks
				chunk = t+" "+chunk;
			}else{
				if(t.equals(","))subjecto = true;
				else if((i==0 && t.matches("(a|an|the)"))){
					subjecto = true;
					this.chunkedtokens.set(0, ""); //remove the article
				}
				break;
			}
		}
		//if(i==0) subjecto = true;
		//reformat this.chunkedtokens
		if(subjecto || i==-1){ 
			chunk = "z["+chunk.trim().replaceAll("<", "(").replaceAll(">", ")")+"]";
		}else{
			chunk = "u["+chunk.trim().replaceFirst("[<(]", "o[(").replaceFirst("[)>]$", ")]").replaceAll("<", "(").replaceAll(">", ")").replaceAll("[{}]", "")+"]";//<leaf><blade> => u[o[(leaf)(blade)]]
		}
		
		//reset from i+2 to last
		for(int j = i+1; j <last; j++){
			this.chunkedtokens.set(j, "");
		}
		while(i>=0 && this.chunkedtokens.get(i).length()==0){
			i--;
		}
		//if the previous nonempty chunk ends with a (), then merge this new u[] with the ()
		if(i>=0 && this.chunkedtokens.get(i).matches(".*\\)\\W*\\]$")){
			chunk = "("+chunk.replaceAll("(\\w+\\[|\\])", "").replaceAll(" ", ") (")+")";
			chunk = chunk.replaceAll("\\(+", "(").replaceAll("\\)+", ")");
			String previous = this.chunkedtokens.get(i);
			String p1 = previous.substring(0, previous.lastIndexOf(")")+1);
			previous = previous.replace(p1, p1+" "+chunk);
		    this.chunkedtokens.set(i, previous);
		    this.chunkedtokens.set(last, "");
		}else{
			//otherwise
			this.chunkedtokens.set(last, chunk);
		}
		if(this.printRecover){
			System.out.println("nsorgan chunk formed: "+chunk +" for \n"+this.sentid+"["+this.sentsrc+"]"+this.markedsent);
		}
	}
	/**
	 * attempts to include broken-away conjuncted organs to pp and vb phrase
	 */
	private void recoverConjunctedOrgans() {
		for(int i = 0; i < this.chunkedtokens.size(); i++){
			String t = this.chunkedtokens.get(i);
			if(this.chunkedtokens.size()>i+2){
				if((t.startsWith("r[p") || t.startsWith("b[v")) && 
						(this.chunkedtokens.get(i+1).matches("(and|or|plus)")||
								(this.chunkedtokens.get(i+1).matches(",") && this.chunkedtokens.get(i+2).matches("(and|or|plus)")))) {//check 211
					recoverConjunctedOrgans4PP(i);
				}else if((t.startsWith("r[p") || t.startsWith("b[v")) && this.chunkedtokens.get(i+1).startsWith("<")){//found a broken away noun
					int j = i;
					String newo = "";
					String o = this.chunkedtokens.get(++j);					
					do{
						newo += o;
						this.chunkedtokens.set(j, "");
						o = this.chunkedtokens.get(++j);					
					}while (o.startsWith("<"));
					String p1 = t.replaceFirst("\\]+$", "");
					String p2 = t.replace(p1, "");
					newo = newo.replaceAll("<", "(").replaceAll(">", ")").trim();
					t = p1+" "+newo+p2;
					this.chunkedtokens.set(i, "");
					this.chunkedtokens.set(--j, t);
				}
				
				
				/*else if (t.startsWith("b[v") && this.chunkedtokens.get(i+1).matches("(and|or|plus)")){
					recoverConjunctedOrgans4VB(i);
				}*/
			}
		}
	}

	/**
	 * recover if what follows the PP is "and|or|plus" and a (modified) organ followed by a , or a series of chunks
	 * @param i: the index where a PP-chunk followed by and|or|plus is found
	 */
	private void recoverConjunctedOrgans4PP(int i) {
		String recovered = this.chunkedtokens.get(i+1)+" ";//and|or|plus
		boolean foundo = false;
		boolean recover = true;
		int endindex = 0;
		for(int j = i+2; j < this.chunkedtokens.size(); j++){
			String t = this.chunkedtokens.get(j);
			if(!foundo && (t.matches("\\{\\w+\\}") || t.equals(",") || t.contains("~list~"))){//states before an organ 
				recovered += t+" ";
			}else if(t.matches("<\\w+>") || t.contains("l[")){//organ
				recovered += t+" ";
				endindex = j;
				foundo = true;
			}else if(foundo && t.matches("(,|;|\\.)")){//states before an organ 
				break; //organ followed by ",",  should recover
			}else if(foundo && t.contains("[") && !t.contains("~list~")){//found or not found organ
				//do nothing
			}else{
				recover = false;
				break;
			}
		}
		
		if(recover){
			//reformat: insert recovered before the last set of ] 
			String chunk = this.chunkedtokens.get(i);
			String p1 = chunk.replaceFirst("\\]+$", "");
			String p2 = chunk.replace(p1, "");
			recovered = recovered.replaceAll("<", "(").replaceAll(">", ")").trim();
			chunk = p1+" "+recovered+p2;
			this.chunkedtokens.set(i, "");
			//reset from i+1 to endindex
			for(int j = i+1; j <endindex; j++){
				this.chunkedtokens.set(j, "");
			}
			this.chunkedtokens.set(endindex, chunk);
			if(this.printRecover){
				System.out.println("pp/vp object chunk formed: "+chunk +" for \n"+this.sentid+"["+this.sentsrc+"]"+this.markedsent);
			}
			
		}
	}
	/**
	 * insert segment marks in chunkedtokens while producing this.chunkedsent
	 * after first round of segmentation, proceed to the 2nd round to disambiguate ", those of" 
	 */
	private void segmentSent() {
		int i;
		for(i = this.chunkedtokens.size()-1; i>=0; i--){
			String t = this.chunkedtokens.get(i);
			if(t.compareTo("") !=0){
				this.chunkedsent = t+" "+this.chunkedsent;;
			}
			if(t.indexOf('<')>=0 || t.indexOf("z[")>=0){//z[ is chunkOrgan
				for(i = i-1; i>=0; i--){
					String m = this.chunkedtokens.get(i);
					if(m.matches(".*?\\b("+ChunkedSentence.prepositions+")\\b.*")){
						this.chunkedsent = m+" "+this.chunkedsent;
						break; //has prepositions before <
					}
					//if(m.matches("(,|;|:)") && !suspend){
					if(m.matches("(,|;|:)")){
						this.chunkedtokens.set(i, "SG"+m+"SG"); //insert a segment mark
						this.chunkedsent = "SG"+m+"SG"+" "+this.chunkedsent;
						break;
					}else{
						if(m.compareTo("") !=0){
							this.chunkedsent = m+" "+this.chunkedsent;
						}
					}
					
				}
			}
		}
		if(this.chunkedtokens.get(this.chunkedtokens.size()-1).matches("\\W")){
			this.chunkedtokens.set(this.chunkedtokens.size()-1, "SG"+this.chunkedtokens.get(this.chunkedtokens.size()-1)+"SG");
		}
		this.chunkedsent.trim();
		disambiguateThose();
	}
	
	/**
	 * <corollas> {purple} , those of {sterile} <florets> ± {expanded} , {exceeding} <corollas> of {fertile} <florets> , those of {fertile} <florets> 15-18 {mm} .
	 * <phyllaries> {many} in 6-8 <series>... , <apices> {shape~list~acute~to~acuminate} , those of {innermost} {bristly-ciliate-or-plumose} .
	 * find "those" instances in chunkedsent, fix chunkedsent, then fix chunkedtokens
	 * fix = replacing those with the subject of the last segment
	 */
	private void disambiguateThose() {
		Pattern p = null;
		if(this.chunkedsent.indexOf(" those r[p[of")>0){
			//p = Pattern.compile("((?:.*?SG\\WSG.*|^)<(.*?)>.*?)those(\\s+r?\\[?p?\\[?of.*)");
			p = Pattern.compile("((?:.*?SG\\WSG.*|^)(?:z\\[\\(|<)(.*?)(?:>|\\)\\]).*?)those(\\s+r?\\[?p?\\[?of.*)");
			Matcher m = p.matcher(this.chunkedsent);
			while(m.matches()){
				String noun = m.group(2);
				int indexOfthose = m.group(1).split("\\s+").length;
				//in case there are to~12~cm, need to adjust indexOfthose
				String textbeforethose = m.group(1);
				Pattern pt = Pattern.compile("(.*?)\\b(to~\\d+~(?:"+this.units+").*?)\\b(.*)");
				Matcher mt = pt.matcher(textbeforethose);
				while(mt.matches()){
					textbeforethose = mt.group(3);
					indexOfthose += mt.group(2).replaceAll("[^~]", "").length();
					mt = pt.matcher(textbeforethose);
				}
				//update chunkedsent and chunkedtokens
				//"those" may be included in a chunk
				String token = this.chunkedtokens.get(indexOfthose);
				if(token.compareTo("those")==0){
					String temp = m.group(1).trim();
					temp = temp.replaceFirst(",$", "SG,SG");
					this.chunkedsent = temp+" <"+noun+">"+m.group(3);
					this.chunkedtokens.set(indexOfthose, "<"+noun+">");
					if(this.chunkedtokens.get(indexOfthose-1).compareTo(",")==0){
						this.chunkedtokens.set(indexOfthose-1, "SG,SG");
					}
				}else{//in a chunk: break the chunk into two
					int indexOfchunk = findChunk(indexOfthose, "those");
					String chunk = this.chunkedtokens.get(indexOfchunk);
					String[] two = chunk.split("\\s*those\\s*");
					two[0] += " ("+noun+")";
					//find how many closing brackets are needed in two[0] and form the two new chunks
					int lb = two[0].replaceAll("[^\\[]", "").length();
					int rb = two[0].replaceAll("[^\\]]", "").length();
					for(int i = 0; i<lb-rb; i++){
						two[0]+="]";
						two[1] = two[1].replaceFirst("\\]$", "");						
					}
					String newchunk = two[0]+" "+two[1];
					this.chunkedsent = this.chunkedsent.replace(chunk, newchunk);
					//replace the old chunk with two chunks in this.chunkedtokens 
					if(this.chunkedtokens.get(indexOfchunk+1).length()==0){
						this.chunkedtokens.set(indexOfchunk, two[0]);
						this.chunkedtokens.set(indexOfchunk+1, two[1]);
					}else if(this.chunkedtokens.get(indexOfchunk-1).length()==0){
						this.chunkedtokens.set(indexOfchunk-1, two[0]);
						this.chunkedtokens.set(indexOfchunk, two[1]);
					}
				}
				m = p.matcher(this.chunkedsent);
			}
		}
	}
	
	/**
	 * find the index in this.chunkedtokens that is near indexofkeyword and hold a chunk containing "keyword"
	 * @param indexOfkeyword
	 * @param keyword
	 * @return
	 */
	private int findChunk(int indexOfkeyword, String keyword) {
		//search downwards
		String chunk = "";
		int i = indexOfkeyword;
		do{
			i++;
			chunk = this.chunkedtokens.get(i);
		}while(chunk.length()==0);
		if(chunk.indexOf(keyword)>=0){
			return i;
		}
		//search upwards
		chunk = "";
		i = indexOfkeyword;
		do{
			i--;
			chunk = this.chunkedtokens.get(i);
		}while(chunk.length()==0);
		if(chunk.indexOf(keyword)>=0){
			return i;
		}		
		System.out.println("Wrong chunks in ChunkedSentence, System exiting.");
		System.exit(1); //should never reach here
		return 0;
	}
	/**
	 * l[(mid) and (distal) (cauline) {smaller}]
	 * ==>
	 * l[(mid) and (distal) (cauline)] {smaller}
	 */
	private void removeStateFromList() {
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			String t = this.chunkedtokens.get(i);
			if(t.matches("l\\[[^\\[]*?}\\]")){
				String list = t.substring(0, t.lastIndexOf(")")+1).trim();
				String state = t.replace(list, "").replaceFirst("\\]$", "").trim();
				list= list+"]";
				if(this.chunkedtokens.get(i+1).length()==0){
					this.chunkedtokens.set(i, list);
					this.chunkedtokens.set(i+1, state);
				}else if(this.chunkedtokens.get(i-1).length()==0){
					this.chunkedtokens.set(i-1, list);
					this.chunkedtokens.set(i, state);
				}else{
					System.err.println("removeStateFromList messed up");
				}
				this.chunkedsent = this.chunkedsent.replace(t, list+" "+state);
			}
			
		}
		
	}

	
	/**
	 * 3] {mm}
	 * 
	 */
	private void normalizeUnits(){
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			String word = this.chunkedtokens.get(i);
			if(word.matches("[<{]("+ChunkedSentence.units+")[}>]")){
				if(i-1>=0){
					String latest = this.chunkedtokens.get(i-1);
					if(latest.matches(".*?\\d\\]+$")){
						String rest = latest.replaceAll("\\]+$", "").trim();
						String brackets = latest.replace(rest, "").trim();
						String norm = rest+ " "+word.replaceAll("[{}<>]", "")+brackets; //mm, not {mm}
						this.chunkedtokens.set(i-1, norm);
						this.chunkedtokens.set(i, "");
					}					
				}
			}
		}
	}
	
	/**
	 * shorter and wider than ...
	 * more/less smooth than ...
	 * pretty good now
	 */
	private int normalizeThan(){
		int count = 0;
		String np = "";
		int thani = 0;
		int firstmorei = this.chunkedtokens.size();
		String more = "";
		String preps = ChunkedSentence.prepositions.replaceFirst("\\bthan\\|", "").replaceFirst("\\bto\\|", "");
		if(this.markedsent.indexOf("than") >=0 ){
			if(this.printNormThan){
				System.out.println("Need to normalize Than! "+np);
			}
			for(int i = 0; i<this.chunkedtokens.size(); i++){
				//scan for JJRs
				String token = this.chunkedtokens.get(i);
				if(more.length()==0 && (token.matches(".*?\\b(\\w+er|more|less)\\b.*") && (token.indexOf("<")<0)|| this.markedsent.indexOf(token+" than")>=0)){ //<inner> is not, but <longer> than is 
					firstmorei = i;
					if(token.matches(".*?\\bmore\\b.*")){
						more = "more";
					}else if(token.matches(".*?\\b\\w+er\\b.*")){
						more = "er";
					}
				}else if(more.compareTo("er") == 0 && !token.matches(".*?\\b(\\w+er|more|less|and|or|than)\\b.*") ){
					more = "";
					firstmorei = this.chunkedtokens.size();;
				}
				if(token.matches(".*?\\bthan\\b.*")){
					//needs normalization
					thani = i;
					if(firstmorei < thani){
						//join all tokens between firstmorei and thani--this is the subject of "than"
						for(int j = firstmorei; j<=thani; j++){
							if(this.chunkedtokens.get(j).length()>0){
								np += this.chunkedtokens.get(j)+" ";
							}
							this.chunkedtokens.set(j, "");
						}
						
						//scan for the object of "than"
					
						for(i=i+1; i<this.chunkedtokens.size(); i++){
							String w = this.chunkedtokens.get(i).replaceAll("(\\<|\\>|\\{|\\}|\\w+\\[|\\])", "");
							//if(w.matches("\\b("+preps+"|and|or|that|which|but)\\b") || w.matches("\\W")){
							if(w.matches("\\b("+preps+"|and|that|which|but)\\b") || w.matches("\\p{Punct}")){ //should allow ±, n[{shorter} than] ± {campanulate} <throats>
								np = np.replaceAll("<", "(").replaceAll(">", ")").trim();
								this.chunkedtokens.set(thani, "n["+np+"]");
								count++;
								break;
							}else{
								if(this.chunkedtokens.get(i).length()>0){
									np += this.chunkedtokens.get(i)+" ";
								}
								this.chunkedtokens.set(i, "");
							}
						}
						if(this.printNormThan){
							System.out.println("Normalize Than! "+np);
						}
						thani = 0;
						firstmorei = this.chunkedtokens.size();
						np = "";
					}
				}
			
			}
		}
		return count;
	}
	
	/**
	 * expanded to <throats>
	 * to 6 m.
	 * 
	 */
	private int normalizeTo(){
		int count = 0;
		String np = "";
		boolean startn = false;
		//ArrayList<String> copy = (ArrayList<String>)this.chunkedtokens.clone();
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			ArrayList<String> copy = (ArrayList<String>)this.chunkedtokens.clone();
			String token = this.chunkedtokens.get(i);
			if(token.compareTo("to") == 0 || token.matches(".*?\\bto]+$")){
				
				//scan for the next organ
				for(int j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(j==i+1 && t.matches("\\d[^a-z]*")){//match "to 6[-9]" ; not match "to 5-lobed"
						copy = formRangeMeasure(i);
						break;
					}
					if(startn && t.indexOf('<')<0){
						break;
					}
					//to b[v[expose] o[(stigma)]]
					if(t.matches("[,:;\\d]") || t.matches(".*?\\b[pv]\\[.*") ||t.matches(".*?\\b("+ChunkedSentence.prepositions+"|and|or|that|which|but)\\b.*")){
						break;
					}
					np +=t+" ";
					this.chunkedtokens.set(j, "");
					if(t.lastIndexOf(' ') >=0){
						t = t.substring(t.lastIndexOf(' ')); //last word there
					}
					if(t.indexOf('<')>=0 || t.indexOf('(')>=0){ //t may have []<>{}
						startn = true; //not break yet, may be the next token is a noun
					}
				}
				if(!startn){
					this.chunkedtokens = copy; //not finding the organ, reset
				}else{
					if(this.printNormTo){
						System.out.println("To needs normalization!");
					}
					np = "to "+np;
					//scan forward for the start of the chunk
					boolean startc = false; //find the start of the chunk
					for(int j = i-1; j>=0; j--){
						String t = this.chunkedtokens.get(j);
						if(t.matches(".*?\\b("+ChunkedSentence.prepositions+"|and|or|that|which|but)\\b.*") || t.matches(".*?[>;,:].*") ||(t.matches("^\\w+\\[.*") && j!=i-1) ){ //the last condition is to avoid nested chunks. cannot immediately before w[].e.g: b[v[{placed}] o[{close}]] w[to {posterior} (shell) (margin)] ; 
							np = np.replaceAll("<", "(").replaceAll(">", ")").replaceAll("\\s+", " ").trim();
							//np = np.replaceAll("\\s+", " ").trim();
							this.chunkedtokens.set(i, "w["+np+"]"); //replace "to" with np
							count++;
							startn = false;
							startc = true;
							if(this.printNormTo){
								System.out.println("!normalizedTo! "+np);
							}
							break;
						}else{	
							np = t+" "+np;
							this.chunkedtokens.set(j, "");
						}
					}
					if(!startc){
						this.chunkedtokens = copy; //not finding the start of the chunk, reset
					}
			}
		}
	}
		return count;
}
	/**
	 * form a chunk if a pattern "to # unit" is found starting from i
	 * @param i: index of "to", which is followed by a number
	 * @return this.chunkedtokens
	 */
	private ArrayList<String> formRangeMeasure(int i) {
		String chunk = "to~"+this.chunkedtokens.get(i+1)+"~"; //"to"
		if(this.chunkedtokens.size()>i+2){
			String unit = this.chunkedtokens.get(i+2).replaceAll("\\W", " ").trim();
			if(unit.matches("("+this.units+")")){
				chunk += unit;
				this.chunkedtokens.set(i+2, chunk);
				this.chunkedtokens.set(i+1, "");
				if(this.chunkedtokens.get(i).equals("to")){
					this.chunkedtokens.set(i, "");
				}else{
					this.chunkedtokens.set(i, this.chunkedtokens.get(i).replaceFirst("\\s+to(?=\\W+$)", ""));
				}
			}
		}		
		return this.chunkedtokens;
	}
	
	
	/**between 5 and 10
	 * between the frontal and the sphenotic spine 
	 * between anterior supraneural bone and neural spine of vertebra 4 
	 * between neural arches of vertebrae 3 and 4
	 * @return
	 * 5-10
	 * r[p[between] o[the frontal and the sphenotic spine]] 
	 * r[p[between] o[anterior supraneural bone and neural spine]] of vertebra 4 
	 * r[p[between] o[neural arches]] of vertebrae 3 and 4
	 * 
	 * 
	 * what about:
	 * 948[Armbruster_2004.xml_ffbaa153-5288-4671-866c-33d14c78c44e.txt-0]: 
	 * <space> r[p[between] o[{posterior} (process)]] r[p[of] o[(coracoid) (strut) and {posterior} (process)]] r[p[of] o[(coracoid)]]
	 */
	private int normalizeBetween(){
		int count = 0;
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			String token = this.chunkedtokens.get(i);			
			if(token.matches(".*?\\bbetween\\b.*")){//between
				if(this.printNorm){
					System.out.println(token+" needs normalization!");	
				}
				if(token.matches("r\\[.*? and .*?\\]")){//already a chunk, fix the format: r[p[between] the {frontal} and o[the (sphenotic) ({spine})]]
					token = token.replaceFirst("o\\[", "").replaceFirst("\\]\\s*", "] o[");
					this.chunkedtokens.set(i, token);
					return ++count;
				}
				String chara = Utilities.lookupCharacter(this.chunkedtokens.get(i+1).replaceAll("[<>(){}\\]\\[]", ""), conn, characterhash, this.glosstable, this.tableprefix);
				if(this.chunkedtokens.get(i+1).matches("\\d+.*") || (chara!=null && chara.compareToIgnoreCase("structure")!=0)){
					//deal with "between 5 and 10" => "5-10"
					//between red and purple => red to purple
					count += normalizeBetweenCharacters(i);
				}else{
					//find the nearest "and" that is not separated from "between" by any stopwords or puncts
					//if such "and" can not be found, find the nearest pl structure terms
					count += normalizeBetweenStructures(i);
				}
				
			}
		}
		return count;
	}
	
	private int normalizeBetweenCharacters(int prepindex) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	/**normalize one instance of "between"
	 * find the nearest "and" that is not separated from "between" by any stopwords or puncts
	 * if such "and" can not be found, find the nearest pl structure terms
	 * @param the index for the prep (between) and it is the starting point for the search in chunkedtoken
	 * @return
	 * r[p[between] o[the frontal and the sphenotic spine]] 
	 * r[p[between] o[anterior supraneural bone and neural spine]] of vertebra 4 
	 * r[p[between] o[neural arches]] of vertebrae 3 and 4
	 */
	private int normalizeBetweenStructures(int prepindex) {
		int nearestN1 = 0;
		int nearestN2 = 0;
		int nearestAND = 0;
		for(int i = prepindex+1; i < this.chunkedtokens.size(); i++){
			String token = this.chunkedtokens.get(i);
			if(nearestAND ==0 && (token.matches(".*?\\b("+ChunkedSentence.prepositions+"|,)\\b.*"))){
				//failed to find "and", make the chunk stop by nearestN1
				//check this before checking for < or (
				return makeChunk4Between(prepindex, nearestN1);
				
			}
			if(nearestAND == 0 && ((token.contains("<") || token.contains("(")))){
				nearestN1 = i;
			}
			if(nearestAND == 0 && (token.compareToIgnoreCase("and")==0)){
				nearestAND = i;
			}

			if(nearestAND > 0 && ((token.contains("<") || token.contains("(")))){
				nearestN2 = i;
			}
			if(nearestAND > 0 && nearestN2>0 && !token.contains("<") && !token.contains("(")){
				//find the 2nd organ, make the chunk
				return makeChunk4Between(prepindex, nearestN2);
			}
			if(nearestAND > 0 && (token.matches(".*?\\b("+ChunkedSentence.prepositions+"|,)\\b.*"))){
				//failed to find nearestN2, make the chunk stop now
				return makeChunk4Between(prepindex, i-1);				
			}
		}
		return 0;
	}
	/**
	 * form a chunk using all tokens from prepindex to endindex
	 * reset all these tokens in chunkedtokens
	 * put the chunk at the prepindex
	 * @param prepindex
	 * @param endindex
	 */
	private int makeChunk4Between(int prepindex, int endindex) {
		//String chunk = "r[p["+this.chunkedtokens.get(prepindex)+"] o[";
		if(endindex <= prepindex) return 0;
		String chunk = "";
		for(int i = prepindex+1; i<=endindex; i++){
			String t = this.chunkedtokens.get(i);
			t = t.contains("<")? "("+t.replaceAll("[<>(){}]", "")+")": t;
			chunk +=t+" ";
			this.chunkedtokens.set(i, "");
		}
		if(this.chunkedtokens.get(prepindex).contains("[between]")){
			chunk = this.chunkedtokens.get(prepindex).replaceAll("\\]+$", " ")+chunk.trim()+"]]";
		}else{ //bare word between
			chunk = "r[p["+this.chunkedtokens.get(prepindex)+"] o["+chunk.trim()+"]]";
		}
		this.chunkedtokens.set(prepindex, chunk);
		return 1;
	}
	/**
	 * most [of] lengths
	 * [in] zyz arrays
	 */
	private int normalizeOtherINs(){
		
		//boolean startn = false;
		int count = 0;
		String preps = ChunkedSentence.prepositions.replaceAll("\\b(than|to|between)\\|", "");
		for(int i = 0; i<this.chunkedtokens.size(); i++){
			String token = this.chunkedtokens.get(i);
			
			if(token.matches(".*?p\\[\\{?[a-z]+\\}?\\]+") || token.matches(".*?\\b("+preps+")\\b\\]*$") ||
					token.matches(".*?\\b(as-.*?-as|same-.*?-as|\\w+-to|in-.*?-(with|to))\\b.*?")){//[of] ...onto]]
				token = token.replaceAll("[{}]", "");
				if(this.printNorm){
					System.out.println(token+" needs normalization!");
				}
				// a prep is identified, needs normalization
				ArrayList<String> copy = (ArrayList<String>)this.chunkedtokens.clone();
				//String nscopy = null;
				String npcopy = null;
				ArrayList<String> ctcopy = null;
				boolean startn = false;
				String np = "";
				//String ns = "";
				boolean foundorgan = false;
				//boolean ofnumber = false;
				//lookforward in chunkedtokens to find the object noun
				int j = 0;
				for(j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(j==i+1 && t.matches("^[,;\\.]")){//"smooth throughout, ", but what about "smooth throughout OR hairy basally"?
						if(this.printNorm){
							System.out.println("encounter ',' immediately, no object is expected");
						}
						break; 						
					}
					/*if(t.startsWith("r[p[") && !np.matches(".*?\\b(or|and)\\b\\s+$")){
						npcopy = np;//TODO: 4/14/2011 check out 501.txt-4, 502.txt-5 "after flowering, 10 cm in fruit" 512.txt-11 "differing from inner, highly variable in <color>"
						break;
					}*/
					if(!foundorgan && startn && t.indexOf('<')<0 && t.indexOf('(')<0 && !Utilities.isNoun(t, nouns, notnouns)){ //test whole t, not the last word once a noun has been found
						//save ns for now, but keep looking for organs
						//nscopy = nscopy == null ? ns : nscopy; //keep only the first copy
						npcopy = npcopy == null? np : npcopy;
						ctcopy = ctcopy == null? (ArrayList<String>)this.chunkedtokens.clone():ctcopy;
					}
					//if(startn && !foundorgan && ishardstop(j)){
					if(!foundorgan && ishardstop(j)){
						//hard stop encountered, break
						//ns = nscopy;
						if(npcopy!=null && ctcopy!=null){
							np = npcopy;
							this.chunkedtokens = ctcopy;
						}
						break;
					}
					
					if(foundorgan && t.indexOf('<')<0 && t.indexOf('(')<0){ //test whole t, not the last word once a noun has been found
						break; //break, the end of the search is reached, found organ as object
					}
					
					np +=t+" "; //any word in betweens
					this.chunkedtokens.set(j, "");
					
					if(t.indexOf('<')>=0 ||t.indexOf('(')>=0){ //t may have []<>{}
						startn = true; //not break yet, may be the next token is also a noun
						foundorgan = true;
					}
					
					if(!foundorgan && Utilities.isNoun(t, nouns, notnouns)){ //t may have []<>{}
						startn = true; //won't affect the value of foundorgan, after foundorgan is true, "plus" problem
						if(Utilities.isPlural(t)){
							foundorgan = true;
							np = np.trim();
							if(np.lastIndexOf(" ")>0){
								np = np.substring(0, np.lastIndexOf(" "))+" "+ "("+t.replaceAll("\\W", "")+") ";
							}else{
								np = "("+np.replaceAll("\\W", "")+") ";
							}
						}
					}
				}
				
				/*
				 for(int j = i+1; j<this.chunkedtokens.size(); j++){
					String t = this.chunkedtokens.get(j).trim();
					if(startn && t.indexOf('<')<0 && !Utilities.isNoun(t, nouns)){ //test whole t, not the last word once a noun has been found
						break; //break, the end of the search is reached
					}
					np +=t+" ";
					this.chunkedtokens.set(j, "");
					
					if(t.indexOf('<')>=0 ||t.indexOf('(')>=0 || Utilities.isNoun(t, nouns)){ //t may have []<>{}
						startn = true; //not break yet, may be the next token is a noun
						ns += t+" ";
					}
				} 
				 */
				//form the normalized chunk
				if(foundorgan || npcopy!= null /*|| ofnumber*/){
					//ns = ns.trim();
					//if(!ns.endsWith("]")){ //not already a chunk
						//np = np.replace(ns, "").trim();
						//ns  = "("+ns.replaceAll("[{(<>)}]", "").replaceAll("\\s+", ") (")+")"; //mark the object as organ word by word
						//np = (np.replaceAll("<", "(").replaceAll(">", ")")+" "+ns).trim();
						np = np.replaceAll("<", "(").replaceAll(">", ")").replaceAll("\\s+", " ").trim();
					//}
					String symbol = "o";	
					/*if(ofnumber){
						symbol = "c";
					}*/
					if(token.indexOf('[')>=0){
						String rest = token.replaceFirst("\\]+$", "").trim();
						String brackets = token.replace(rest, "").replaceFirst("\\]$", "").trim();
						token = rest + "] "+symbol+"["+np.trim()+"]"+brackets;
						this.chunkedtokens.set(i, token);
						if(this.printNorm){
							System.out.println("!normalized!: "+token);
						}
					}else{//without [], one word per token
						token = "r[p["+token+"] "+symbol+"["+np.trim()+"]]";
						this.chunkedtokens.set(i, token);
						if(this.printNorm){
							System.out.println("!normalized!: "+token);
						}
					}
					count++;
				}else{ 
					if(j-i==1){
						//cancel the normalization attempt on this prep, return to the original chunkedtokens
						this.chunkedtokens = copy;
					}else if(np.matches(".*? [\\d+%]$")){//reached the end of the sentence.This is the case for "plumose on distal 80 % ."?
						//also the same width dorsally as proximally
						this.chunkedtokens = copy;
						//np = np.replaceAll("\\s+", " ").trim();
						String head = token.replaceFirst("\\]+$", "").trim();
						String brackets = token.replace(head, "").replaceFirst("\\]$", "").trim();
						String rest = np.replaceFirst(".*?(?=(\\.|;|,|\\band\\b|\\bor\\b|\\w\\[))", "").trim();
						np = np.replace(rest, ""); //perserve spaces for later
						String object = np.replaceAll("\\s+", " ").trim();
						if(object.length()>0){
							token = head + "] o["+np.replaceAll("\\s+", " ").trim()+"]"+brackets;
							this.chunkedtokens.set(i, token);
							int npsize = np.split("\\s").length; //split on single space to perserve correct count of tokens
							for(int k = i+1; k<=i+npsize; k++){
								this.chunkedtokens.set(k, "");
							}
							if(this.printNorm){
								System.out.println("!default normalized to (.|;|,|and|or|r[)!: "+token);
							}
							count++;
						}
					}else{
						//cancel the normalization attempt on this prep, return to the original chunkedtokens
						this.chunkedtokens = copy;
					}
				}
			}
			//i=i+1;

		}
		/*if(!startn){
			this.chunkedtokens = copy;
		}*/
		return count;
	}
	

	

	private boolean ishardstop(int j) {
		String t1 = this.chunkedtokens.get(j).trim();
		
		if(t1.matches("^\\w\\[.*")){
			return true;
		}
		if(t1.startsWith(".")){
			return true;
		}
		
		if(this.chunkedtokens.size()==j+1){
			return true;
		}

		String t2 = this.chunkedtokens.get(j+1).trim();
		if(t1.startsWith(",") && t2.matches("^\\W*[<(].*")){
			return true;
		}
		return false;
	}
	public String toString(){
		return this.chunkedsent;
	}
	public int getPointer(){
		return this.pointer;
	}
	//mohan code to reset the pointer
	public void resetPointer(){
		this.pointer=0;
	}
	//end mohan code
	public void setInSegment(boolean yes){
		this.inSegment = yes;
	}
	
	public void setRightAfterSubject(boolean yes){
		this.rightAfterSubject = yes;
	}
	/**
	 * move pointer after lead in chunkedtokens
	 * @param lead
	 */
	public int skipLead(String[] tobeskipped){
		int wcount = 0;
		if(tobeskipped[tobeskipped.length-1].compareTo("chromosome")==0){
			for(int i = 0; i<this.chunkedtokens.size(); i++){
				if(this.chunkedtokens.get(i).endsWith("=")){
					this.pointer = i;
					break;
				}
			}
			this.pointer++;
		}else if(tobeskipped[tobeskipped.length-1].compareTo("whole_organism")==0){ //Treatises: "general" = "whole_organism"
			//do not skip
		}else{
			int sl = tobeskipped.length;
			boolean find = false;
			for(int i = 0; i<this.chunkedtokens.size(); i++){
				//chunkedtokens may be a list: shape[{shape~list~planoconvex~to~ventribiconvex~subquadrate~to~subcircular}]
				/*wcount += (this.chunkedtokens.get(i)+" a").replaceAll(",", "or").replaceAll("\\b(or )+", "or ")
				.replaceFirst("^.*?~list~", "").replaceAll("~", " ")
				.trim().split("\\s+").length-1;*/
				if(this.chunkedtokens.get(i).trim().length()>0) wcount++;
				//if(this.chunkedtokens.get(i).replace("SG", "").replaceAll("(\\w+\\[|\\]|\\)|\\(|\\{|\\})", "").replaceAll("-", "_").toLowerCase().matches(".*?\\b"+(tobeskipped[sl-1].length()-2>0 ? tobeskipped[sl-1].substring(0, tobeskipped[sl-1].length()-2) : tobeskipped[sl-1])+".*") && wcount>=sl){//try to match <phyllaries> to phyllary, "segement I", i is 1-character long
				if(this.chunkedtokens.get(i).replace("SG", "").replaceAll("(\\w+\\[|\\]|\\)|\\(|\\{|\\})", "").replaceAll("-", "_").toLowerCase().matches(".*?\\b"+(tobeskipped[sl-1].length()-2>0 ? tobeskipped[sl-1].substring(0, tobeskipped[sl-1].length()-2) : tobeskipped[sl-1])+"\\S*") && wcount>=sl){//use \\S* so "leaves" not match b[v[leaves] o[(preopercle)]]
					if(wcount==sl){
						this.pointer = i;
						find = true;
					}else{
						//if wcount > sl, then there must be some extra words that have been skipped
						//put those words in chunkedtokens for process
						//example:{thin} {dorsal} {median} <septum> {centrally} only ; 
						int save = i;
						if(!this.chunkedtokens.get(i).matches(".*?\\bof\\b.*")){//, , l[(taproots) and clusters], , , , r[p[of] o[{coarse} {fibrous} (roots)]],, tobeskiped is "taproot and root"
							i++;
							for(int j = 0; j < wcount-sl; j++){
								this.chunkedtokens.add(i++, this.chunkedtokens.get(j));
							}
							this.chunkedtokens.add(i++, ",");
						}
						this.pointer = save;
						find = true;
					}
					break;
				}
			}
			//this.pointer++;
			if(find) this.pointer++;
		}
		if(this.pointer==0){return -1;}
		return 1;
	}

	public boolean hasNext(){
		if(pointer <this.chunkedtokens.size()){
			return true;
		}
		return false;
	}
	
	public int getSize(){
		return this.chunkedtokens.size();
	}
	
	public Chunk nextChunk(){
		
		Chunk ck = getNextChunk();
		while(ck==null && this.hasNext()){
			ck=this.getNextChunk();
		}
		if(ck instanceof ChunkOrgan){
			this.rightAfterSubject = true;
		}else{
			this.rightAfterSubject = false;
		}
		return ck==null? new ChunkEOS(".") : ck;
		
	}
	/**
	 * returns the next Chunk: may be a
	 * Organ, Value, Comparative Value, SimpleCharacterState, Subclause,
	 * PrepChunk, IVerbChunk (Intransitive verb chunk, followed by a preposition), VerbChunk, ADJChunk
	 * @return
	 */
	public Chunk getNextChunk(){
		Chunk chunk = null;
		String token = this.chunkedtokens.get(pointer);////a token may be a word or a chunk of text
		while(token.trim().length()==0){
			pointer++;
			token = this.chunkedtokens.get(pointer);
		}
		token = token.compareTo("±")==0? "moreorless" : token;
		token = token.matches(".*?\\d.*")? NumericalHandler.originalNumForm(token) : token;
		
		//all tokens: 
		//number:
		//if(token.matches(".*?\\d+$")){ //ends with a number
		if(NumericalHandler.isNumerical(token) ||token.matches("^to~\\d.*")|| token.matches("l\\s*\\W\\s*w")){//l-w or l/w
				chunk = getNextNumerics();//pointer++;
				if(this.unassignedmodifier != null){
					chunk.setText(this.unassignedmodifier+ " "+chunk.toString());
				}
				return chunk;
		}
		
		if(token.indexOf("×")>0 && token.length()>0 && token.indexOf(" ")<0){
			//token: 4-9cm×usually15-25mm			
			String[] dim = token.split("×");
			boolean isArea = true;
			int c = 0;
			for(int i = 0; i<dim.length; i++){
				isArea = dim[i].matches(".*?\\d.*") && isArea;
				c++;
			}
			if(isArea && c>=2){
				token = token.replaceAll("×[^0-9]*", " × ").replaceAll("(?<=[^a-z])(?=[a-z])", " ").replaceAll("(?<=[a-z])(?=[^a-z])", " ").replaceAll("\\s+", " ").trim();
				chunk = new ChunkArea(token);
				pointer++;
				return chunk;
			}
		}

		if(token.indexOf("=")>0){//chromosome count 2n=, FNA specific
			String l = "";
			String t= this.chunkedtokens.get(pointer++);
			while(t.indexOf("SG")<0){
				l +=t+" ";
				t= this.chunkedtokens.get(pointer++);				
			}
			l = l.replaceFirst("\\d[xn]=", "").trim();
			chunk = new ChunkChrom(l);
			return chunk;
		}
		//create a new ChunkedSentence object for bracketed text 
		if(token.startsWith("-LRB-/-LRB-")){
			ArrayList<String> tokens = new ArrayList<String>();
			String text = "";
			if(token.indexOf("-RRB-/-RRB-")<0+0){
				String t = this.chunkedtokens.get(++this.pointer);
				while(!t.endsWith("-RRB-/-RRB-")){
					tokens.add(t);
					text += t+ " ";
					t = this.chunkedtokens.get(++this.pointer);
				}
			}
			text=text.trim();
			if(text.length()>0){ //when -LRB- and -RRB- are on the same line, text="" for example, as in -LRB-/-LRB-3--RRB-/-RRB-5-{merous} (3-)5-{merous}
				this.pointer++;
				if(!text.matches(".*?[,;\\.:]$")){
					text +=" .";
					tokens.add(".");
				}
				Chunk c = new ChunkBracketed(text);
				c.setChunkedTokens(tokens);
				return c;
			} //else, continue on
		}
		//create a new ChunkedSentence object
		if(token.startsWith("s[")){
			ArrayList<String> tokens = new ArrayList<String>();
			String text = token.replaceFirst("s\\[", "").replaceFirst("\\]$", "");
			//break text into correct tokens: s[that is {often} {concealed} r[p[by] o[(trichomes)]]] ;
			tokens = Utilities.breakText(text);
			this.pointer++;
			text=text.trim();
			if(!text.matches(".*?[,;\\.:]$")){
				text +=" .";
				tokens.add(".");
			}
			Chunk c = new ChunkSBAR(text);
			c.setChunkedTokens(tokens);
			return c;
		}
		if(token.matches("\\W") ){//treat L/RRBs as either , or null
			pointer++;
			this.unassignedmodifier = null;
			return new ChunkComma("");
		}
		
		if(token.matches("\\b(and|either)\\b")){
			pointer++;
			this.unassignedmodifier = null;
			return null;
		}
		//end of a segment
		if(token.matches("SG[;:\\.]SG")){
			this.inSegment = false;
			pointer++;
			//this.unassignedmodifier = null;
			return new ChunkEOL(""); //end of line/statement
		}
		
		if(token.matches("SG,SG")){
			this.inSegment = false;
			pointer++;
			this.unassignedmodifier = null;
			return new ChunkEOS("");//end of segment/substence
		}
		
		//start of a segment
		if(!this.inSegment){
			this.inSegment = true;
			chunk = getNextOrgan();//pointer++
			if(chunk != null){
				this.unassignedmodifier = null;
				return chunk;
			}
		}
		
		//all chunks
		if(token.matches("^\\w+\\[.*")){
			String type = chunkType(pointer);
			token = this.chunkedtokens.get(pointer); //as checkType may have reformatted token.
			try{
				if(type != null){
					Class c = Class.forName("fna.charactermarkup."+type);
					Constructor cons = c.getConstructor(String.class);
					pointer++;
					//deal with any unassignedmodifier when EOS is approached.
					//if(this.unassignedmodifier != null && this.chunkedtokens.get(pointer).matches("(SG)?\\W(SG)?")){
					if(this.unassignedmodifier != null){ //did not see why the 2nd condition is needed. Here, assuming any unassigned modifier should be applied to the next valid chunk
						token = token.replaceFirst("\\[", "["+this.unassignedmodifier+" ");
						this.unassignedmodifier = null;
					}
					return (Chunk)cons.newInstance(token.trim());
				}else{//if the chunk is not correctly formatted. Forward pointer to the next comma.
					//forward pointer to after the next [;:,.]
					if(this.printExp){
						System.out.println("PP without a Noun: "+token);
					}
					pointer++;
					/*String t = "";
					do{
						if(this.pointer < this.chunkedtokens.size()){
							t = this.chunkedtokens.get(this.pointer++);
						}else{
							break;
						}
					}while (!t.matches("[,;:\\.]"));*/
					return null;
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		//OR:
		if(token.compareTo("or") == 0){
			this.pointer++;
			return new ChunkOR("or");
		}
		
		//text:
		chunk = composeChunk();
		
		return chunk;
	}
	
	
	private Chunk composeChunk() {
		Chunk chunk;
		String token;
		String scs = "";
		String role = "";
		boolean foundo = false;//found organ
		boolean founds = false;//found state
		if(this.unassignedmodifier != null){
			scs =(scs.trim().length()>0? scs.trim()+"] ": "")+"m["+this.unassignedmodifier.replaceAll("[{}]", "")+" ";
			this.unassignedmodifier = null;
		}
		int i = 0;
		for(i = this.pointer; i<this.chunkedtokens.size(); i++){
			token = this.chunkedtokens.get(i);
			/* if one of the tokens match those in the stop list but not in skip list, skip it and get the next token- mohan 10/19/2011*/
			if(token.matches("("+stop+")") && !token.matches("("+skip+")")){
				i=i+1;
				token = this.chunkedtokens.get(i);
			}
			/*end mohan 10/19/2011*/
			token = token.matches(".*?\\d.*")? NumericalHandler.originalNumForm(token):token;
			if(token.length()==0){
				continue;
			}
			//token = NumericalHandler.originalNumForm(token); //turn -LRB-/-LRB-2
			if(token.matches("^\\w+\\[.*")){ //modifier +  a chunk: m[usually] n[size[{shorter}] constraint[than or {equaling} (phyllaries)]]
				//if(scs.matches("\\w{2,}\\[.*") && token.matches("\\w{2,}\\[.*")){ // scs: position[{adaxial}] token: pubescence[{pubescence~list~glabrous~or~villous}]
				if(scs.matches(".*?\\bo\\[\\w+\\s.*")){
					pointer = i;
					scs = scs.replaceAll("o\\[", "o[(").trim()+")]";
					return new ChunkNonSubjectOrgan("u["+scs+"]");
				}else if(scs.matches(".*?\\w{2,}\\[.*")){
					pointer = i;
					return new ChunkSimpleCharacterState("a["+scs.trim()+"]]"); 
				}else {
					String type = chunkType(i); //changed from pointer to i
					token = this.chunkedtokens.get(i);
					token = token.matches(".*?\\d.*")? NumericalHandler.originalNumForm(token):token;
					scs = scs.trim().length()>0? scs.trim()+"] " : ""; //modifier 
					String start = token.substring(0, token.indexOf("[")+1); //becomes n[m[usually] size[{shorter}] constraint[than or {equaling} (phyllaries)]]
					String end = token.replace(start, "");
					token = start+scs+end;
					try{
						if(type !=null){//r[p[as]] without o[]
							Class c = Class.forName("fna.charactermarkup."+type);
							Constructor cons = c.getConstructor(String.class);
							pointer = i+1;
							return (Chunk)cons.newInstance(token.trim());
						}else{ //parsing failure, continue with the next chunk
							pointer = i+1;
							return null;
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}
			
			role = token.charAt(0)+"";
			token = token.replaceAll("[<>{}]", "");
			//<roots> {usually} <taproots> , {sometimes} {fibrous}.
			String symbol= this.rightAfterSubject? "type" : "o";
			if(!foundo && role.compareTo("<")==0){
				scs = (scs.trim().length()>0? scs.trim()+"] ": "")+symbol+"["+token+" ";
				foundo = true;
			}else if(foundo && role.compareTo("<")==0){
				scs += token+" ";
			}else if(foundo && role.compareTo("<") !=0){
				this.pointer = i;
				scs = scs.replaceFirst("^\\]\\s+", "").replaceFirst(symbol+"\\[", "###[").replaceAll("\\w+\\[", "m[").replaceAll("###\\[", symbol+"[").trim()+"]"; //change all non-type character to modifier: <Inflorescences> {indeterminate} <heads>
				if(!this.rightAfterSubject){
					//reformat m[] o[] o[] to m[] o[()] o[()]
					String m = scs.substring(0, scs.indexOf("o["));
					String o = scs.substring(scs.indexOf("o[")).replaceAll("\\[", "[(").replaceAll("\\]", ")]");
					scs = m+o;
				}
				return this.rightAfterSubject? new ChunkSimpleCharacterState("a["+scs+"]") : new ChunkNonSubjectOrgan("u["+scs+"]"); //must have type[ or o[
			}
			
			if(token.matches(".*?"+NumericalHandler.numberpattern+"$") || token.matches("\\d+\\+?") || token.matches("^to~\\d.*")){ //0. sentence ends with a number, the . is not separated by a space
				if(scs.matches(".*?\\w{2,}\\[.*")){//must have character[
					pointer=i;
					scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
					return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
				}else{
					pointer=i;
					chunk = getNextNumerics();
					if(chunk!=null){
						if(scs.length()>0){
							scs = scs.replaceFirst("^\\]", "").trim()+"] "+chunk.toString();
						}else{
							scs = chunk.toString();
						}
						chunk.setText(scs);
						return chunk;
					}else{
						pointer++;
						return chunk; //return null, skip this token: parsing failure
					}
				}
			}

			
			//add to a state chunk until a) a preposition b) a punct mark or c)another state is encountered
			if(role.compareTo("<") !=0 && true){
				String chara = Utilities.lookupCharacter(token, conn, characterhash, glosstable, tableprefix);
				if(chara==null && Utilities.isAdv(token, adverbs, notadverbs)){
					scs = scs.trim().length()>0? scs.trim()+ "] m["+token+" " : "m["+token;
				}else if(token.matches(".*[,;:\\.\\[].*") || token.matches("\\b("+ChunkedSentence.prepositions+"|or|and)\\b") || token.compareTo("-LRB-/-LRB-")==0){
					this.pointer = i;
					if(scs.matches(".*?\\w{2,}\\[.*")){//must have character[
						scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
						return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
					}else{
						if(scs.indexOf("m[")>=0){
							this.unassignedmodifier = "{"+scs.trim().replaceAll("(m\\[|\\])", "").replaceAll("\\s+", "} {")+"}";
						}
						if(this.pastpointers.contains(i+"")){
							this.pointer = i+1;
						}else{
							this.pastpointers.add(i+"");
						}
						//if(token.matches("SG.SG")) return new ChunkEOS("");
						return null;
					}
				}else{
					//String chara = Utilities.lookupCharacter(token, conn, characterhash, glosstable, tableprefix);
					if(!founds && chara!=null){
						scs = (scs.trim().length()>0? scs.trim()+"] ": "")+chara+"["+token+" ";
						founds = true;
						if(i+1==this.chunkedtokens.size()){ //reach the end of chunkedtokens
							scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
							this.pointer = i+1;
							return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
						}
					}else if(founds && chara!=null && scs.matches(".*?"+chara+"\\[.*")){ //coloration coloration: dark blue
						scs += token+" ";
					}else if(founds){
						this.pointer = i;
						scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";
						return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
					}else if(chara==null){
						if(Utilities.isVerb(token, verbs, notverbs) && !founds){//construct ChunkVP or ChunkCHPP
							scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"v["+token+" ";
							//continue searching for either a <> or a r[]
							boolean findc = false; //find a chunk
							boolean findo = false; //find an organ
							boolean findm = false; //find a modifier
							boolean findt = false; //find a text token
							for(int j = i+1; j < this.chunkedtokens.size(); j++){
								String t = this.chunkedtokens.get(j).trim();
								if(t.length() == 0){continue;}
								if(t.startsWith("u[")){//form a vb chunk
									t = t.replaceFirst("^u\\[", "").replaceFirst("\\]$", "");
									String o = t.substring(t.indexOf("o[")).trim();
									t = t.substring(0, t.indexOf("o[")).trim();
									if(t.length()>0){
										String[] states = t.split("\\s+");
										for(int k = 0; k < states.length; k++){
											String ch = Utilities.lookupCharacter(states[k], conn, characterhash, glosstable, tableprefix);
											if(ch!=null){
												scs = (scs.trim().length()>0? scs.trim()+"] ": "")+ch+"["+states[k].replaceAll("[{}]", "")+" ";
											}else{
												scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"m["+states[k].replaceAll("[{}]", "")+" ";
											}
										}		
									}
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+o;
									this.pointer = j+1;
									return new ChunkVP("b["+scs+"]"); 
								}
								String ch = Utilities.lookupCharacter(t, conn, characterhash, glosstable, tableprefix);
								if((!findc &&!findo) && t.matches("^[rwl]\\[.*")){
									scs = scs.replaceFirst("^\\]\\s+", "").trim()+"] ";
									scs += t;
									findc = true;
								}else if(!findo && t.indexOf("<")>=0){
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"o["+t.replace("<", "(").replace(">", ")").replaceAll("[{}]", "")+" ";
									findo = true;
								}else if(!findo && !findc && ch!=null){
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+ch+"["+t.replaceAll("[{}]", "")+" ";
								}else if(!findo && !findc && !findm && Utilities.isAdv(t, adverbs, notadverbs)){
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"m["+t.replaceAll("[{}]", "")+" ";
									findm = true;
								}else if(!findo && !findc && findm && Utilities.isAdv(t, adverbs, notadverbs)){
									scs += t.replaceAll("[{}]", "")+" ";
								}else if(findo && t.indexOf("<")>=0){
									scs += t.replace("<", "(").replace(">", ")").replaceAll("[{}]", "")+" ";
								}else if((findo || findc) && t.indexOf("<")<0){ //must have foundo or foundc
									this.pointer = j;
									if(findo){scs = scs.replaceFirst("^\\]\\s+", "").trim()+"]";}
									if(scs.indexOf("p[")>=0){
										return new ChunkCHPP("t["+scs.replace("v[", "c[")+"]");
									}else{
										scs = scs.replace("l[", "o[");
										if(scs.matches(".*?\\bv\\[[^\\[]* m\\[.*")){//v[comprising] m[a] architecture[surrounding] o[(involucre)]
											scs = format(scs);
											//scs = scs.replaceFirst("\\] o\\[", " ").replaceFirst("\\] m\\[", "] o[");
										}else if(scs.matches(".*?\\bv\\[[^\\[]* \\w{2,}\\[.*")){//v[comprising]  architecture[surrounding]
											scs = format(scs);
											//scs = scs.replaceFirst("\\] o\\[", " ").replaceFirst("\\] \\w{2,}\\[", "] o[");
										}
										return new ChunkVP("b["+scs+"]"); 
									}
								}else if(t.matches(".*?\\W.*") || t.matches("\\b("+ChunkedSentence.prepositions+"|or|and)\\b") || t.compareTo("-LRB-/-LRB-")==0){
									if(scs.matches(".*?\\w{2,}\\[.*")){ //borne {singly
										this.pointer = j;
										scs = (scs.replaceFirst("^\\]", "").trim()+"]").replaceFirst("\\bv\\[[^\\[]*?\\]\\s*", "");
										return new ChunkSimpleCharacterState("a["+scs.trim()+"]");
									}else{
										//search failed
										if(this.pastpointers.contains(i+"")){
											this.pointer = i+1;
										}else{
											this.pointer = i;
											this.pastpointers.add(i+"");
										}
										return null;
									}
								}else if(!findt){ //usually v[comprising] m[a {surrounding}] o[involucre]
									scs = (scs.trim().length()>0? scs.trim()+"] ": "")+"m["+t+" "; //taking modifiers
									findt = true;
								}else if(findt){
									scs += t+" ";
								}
							}
						}else{
							scs = "";
						}
					}
				}
			}
		}
		if(i==this.chunkedtokens.size()){
			this.pointer = this.chunkedtokens.size();
		}
		return null;
	}
	

	/**
	 * 
	 * @return e.g. 3 cm, what about "3 cm to 10 dm"?
	 * also 3 times (... longer than, as wide as ...)
	 */
	/*private Chunk getNextBroken() {
		String result = "";
		String type = "";
		boolean found = false;
		for(int i = pointer; i<this.chunkedtokens.size(); i++){
			if(this.chunkedtokens.get(i).matches(".*?-")){ //ends with a hyphen
				result += this.chunkedtokens.get(i)+ " ";
				found = true;
				type = checkType(i);
			}
			if(found){
				result += this.chunkedtokens.get(i)+ " ";
				pointer = i+1;
				try{
					if(type != null){
						Class c = Class.forName(type);
						Constructor cons = c.getConstructor(String.class);
						return (Chunk)cons.newInstance(result.replaceAll("[<>]", "").trim());
					}else{
						return new SimpleCharacterState(result.replaceAll("[<>]", "").trim());
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		return null;
	}*/
	/**
	 * m[usually] v[comprising] m[a] architecture[surrounding] o[(involucre)]
	 * 
	 * m[usually] v[comprising] o[1 architecture[surrounding] (involucre)]
	 */
	private String format(String scs) {
		String first = scs.substring(0, scs.indexOf("v["));
		String rest = scs.replace(first, "");
		String v = rest.substring(0, rest.indexOf(']')+1+0);
		String o = rest.replace(v, "").trim(); //m[a] architecture[surrounding] o[(involucre)]
		String newo = "o[";
		do{
			String t = o.indexOf(' ')>=0? o.substring(0, o.indexOf(' ')) : o;
			o = o.replaceFirst(t.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]").replaceAll("\\(", "\\\\(").replaceAll("\\)", "\\\\)"),"").trim();
			if(t.startsWith("m[")){
				t = t.replaceAll("(m\\[|\\])", "").trim();
				if(t.compareTo("a") == 0 && !o.matches("(couple|few)")){
					t = "1";
				}
			}
			if(t.startsWith("o[")){
				t=t.replaceAll("(o\\[|\\])", "").trim();
			}
			newo+=t+" ";
			
		}while(o.length()>0);
		return first+v+" "+newo.trim()+"]";
	}
	/**
	 * TODO: deal with LRB-/-LRB
	 * @return e.g. 3 cm, what about "3 cm to 10 dm"?
	 * also 3 times (... longer than, as wide as ...)
	 */
	private Chunk getNextNumerics() {
		String numerics = "";
		String t = this.chunkedtokens.get(this.pointer);
		t = NumericalHandler.originalNumForm(t).replaceAll("\\?", "");		
		if(t.matches("^to~\\d.*")){
			this.pointer++;
			return new ChunkValue(t.replaceAll("~", " ").trim());
		}
		if(t.matches(".*?("+ChunkedSentence.percentage+")")){
			numerics += t+ " ";
			pointer++;
			return new ChunkValuePercentage(numerics.trim());
		}
		if(t.matches(".*?("+ChunkedSentence.degree+")")){
			numerics += t+ " ";
			pointer++;
			return new ChunkValueDegree(numerics.trim());
		}
		if(t.matches(".*?[()\\[\\]\\-\\–\\d\\.×\\+°²½/¼\\*/%]*?[½/¼\\d][()\\[\\]\\-\\–\\d\\.×\\+°²½/¼\\*/%]*(-\\s*("+ChunkedSentence.counts+")\\b|$)")){ //ends with a number
			numerics += t+ " ";
			pointer++;
			if(pointer==this.chunkedtokens.size()){
				return new ChunkCount(numerics.replaceAll("[{()}]", "").trim());
			}
			t = this.chunkedtokens.get(this.pointer);//read next token
			/*if(t.matches("^[{<(]*("+ChunkedSentence.percentage+").*")){
				numerics += t+ " ";
				pointer++;
				return new ChunkValuePercentage(numerics.replaceAll("[{(<>)}]", "").trim());
			}
			if(t.matches("^[{<(]*("+ChunkedSentence.degree+")\\b.*")){
				numerics += t+ " ";
				pointer++;
				return new ChunkValueDegree(numerics.replaceAll("[{(<>)}]", "").trim());
			}*/
			if(t.matches("^[{<(]*("+ChunkedSentence.units+")\\b.*?")){
				numerics += t+ " ";
				pointer++;
				adjustPointer4Dot(pointer);//in bhl, 10 cm . long, should skip the ". long" after the unit
				numerics = numerics.replaceAll("[{(<>)}]", "").trim();
				if(numerics.contains("×")){
					return new ChunkArea(numerics);
				}
				return new ChunkValue(numerics);
			}
			if(t.matches("^[{<(]*("+ChunkedSentence.times+")\\b.*?")){
				numerics += t+ " ";
				pointer++;
				numerics = numerics.replaceAll("[{(<>)}]", "");
				Chunk c = nextChunk();
				while(c.toString().contains("character")){
					numerics +=c.toString().replaceAll("(\\w+\\[|\\])", "")+" ";
					c = nextChunk();
				}
				numerics +=c.toString();
				if(c instanceof ChunkTHANC){
					return new ChunkValue(numerics);//1.5-2 times n[size[{longer} than {wide}]]
				}else{
					return new ChunkComparativeValue(numerics);//1-2 times a[shape[divided]]???; 1-2 times shape[{shape~list~pinnately~lobed~or~dissected}];many 2-4[-6+] times a[size[widths]];[0.5-]1.5-4.5 times u[o[(leaves)]];0.4-0.5 times u[o[(diams)]]
				}
			}
			/*if(found && this.chunkedtokens.get(i).matches("^("+this.per+")\\b.*?")){
				numerics += this.chunkedtokens.get(i)+ " ";
				pointer = i+1;
				return new ChunkBasedCount(numerics.replaceAll("[<>]", "").trim());
			}*/
			return new ChunkCount(numerics.replaceAll("[{()}]", "").trim());
		}
		
		if(t.matches("l\\s*\\W\\s*w")){
			while(!t.matches(".*?\\d.*")){
				t = this.chunkedtokens.get(++this.pointer);
			}
			this.pointer++;
			return new ChunkRatio(NumericalHandler.originalNumForm(t).trim());
		}
		return null;
	}
	
	
	/**
	 * needed for cases like "10 cm . long/broad/wide/thick", skip ". "
	 * @param pointer2
	 */
	private void adjustPointer4Dot(int pointer) {
		//boolean iscase = false;
		while(this.chunkedtokens.size()>pointer && this.chunkedtokens.get(pointer).trim().length()==0){
			pointer++;
		}
		if(this.chunkedtokens.size()>pointer && this.chunkedtokens.get(pointer).trim().matches("\\.")){//optional
			this.pointer++;
		}
		
		/*while(this.chunkedtokens.size()>pointer && this.chunkedtokens.get(pointer).trim().length()==0){
			pointer++;
		}
		while(this.chunkedtokens.size()>pointer && this.chunkedtokens.get(pointer).trim().matches("[{(<]?(long|broad|wide|thick)[})>]?")){//required
			pointer++;
			iscase = true;
		}
		if(iscase){
			this.pointer = pointer;
		}*/
	}
	/**
	 * 
	 * @return e.g. z[m[leaf] e[blade]], apex, 
	 * margins and apexes
	 * {} <> <>
	 * {} ()
	 */
	public Chunk getNextOrgan() {
		String organ = "";
		boolean found = false;
		int i = 0;
		for(i = pointer; i<this.chunkedtokens.size(); i++){
			String token = this.chunkedtokens.get(i);
			if(token.startsWith("z[")){
				pointer++;
				return new ChunkOrgan(token);
			}
			if(token.matches(".*?\\b("+ChunkedSentence.prepositions+")\\b.*") || token.matches(".*?[,;:\\.].*")){
				break;
			}
			if(found && token.matches("\\b(and|or)\\b")){
				found = false;
			}
			if(found && !token.matches(".*?[>)]\\]*$")){
				pointer = i;
				if(organ.matches("^\\w+\\[")){
					organ = organ.replaceAll("(\\w+\\[|\\])", "");
				}
				organ = organ.replaceAll("[<(]", "(").replaceAll("[>)]", ")").trim();
				return new ChunkOrgan("z["+organ+"]");
			}
			organ += token+" ";
			if(token.matches(".*?[>)]\\]*$")){
				found = true;
			}
			
		}
		if(found){
			pointer = i;
			if(organ.matches("^\\w+\\[")){
				organ = organ.replaceAll("(\\w+\\[|\\])", "");
			}
			organ = organ.replaceAll("[<(]", "(").replaceAll("[>)]", ")").trim();
			return new ChunkOrgan("z["+organ+"]");
		}
		
		return null;
	}
	
	/**
	 * use the un-collapsedTree (this.tree) to check the type of a chunk with the id, 
	 * @param i
	 * @return: 

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
non-subject organ/structure u[m[] relief[] o[]]
character modifier: a[m[largely] relief[smooth] m[abaxially]]

	 */
	private String chunkType(int id) {
		String token = this.chunkedtokens.get(id);
		if(token.matches("^\\w{2,}\\[.*")){
			return "ChunkSL"; //state list
		}
		/*if(token.startsWith("q[")){
			return "ChunkQP";
		}*/
		/*if(token.startsWith("s[")){
			return "ChunkSBAR";
		}*/
		if(token.startsWith("b[")){
			return "ChunkVP";
		}
		//if(token.startsWith("r[") && token.indexOf("[of]") >= 0){
		//	return "ChunkOf";
		//}
		if(token.startsWith("r[")){
			//r[p[around] o[10 mm]] should be ChunkValue
			if(token.matches(".* o\\[\\(?[0-9+×x°²½/¼*/%-]+\\)?.*("+ChunkedSentence.units+")\\]+")){
				token = token.replaceFirst("\\[p\\[", "[m[").replaceAll("[or]\\[", "").replaceFirst("\\]+$", "");
				this.chunkedtokens.set(id, token);
				return "ChunkValue";
			}else if(token.matches(".* o\\[\\(?[0-9+×x°²½/¼*/%-]+\\)?\\]+") && !token.matches(".*[×x]\\].*")){//r[p[at] o[30×]] is not a value
				token = token.replaceFirst("\\[p\\[", "[m[").replaceAll("[or]\\[", "").replaceFirst("\\]+$", "");
				this.chunkedtokens.set(id, token);
				return "ChunkValue";
			}else if(token.indexOf("o[")>=0 /*|| token.indexOf("c[")>=0*/){
				//r[p[without] o[or r[p[with] o[{poorly} {developed} {glutinous} ({ridge})]]]] ; 
				token = token.replaceAll("r\\[p\\[of\\]\\]", "of");
				this.chunkedtokens.set(id, token);
				//r[p[for] o[{dorsal} 12 , {form}]] SG.SG
				if(token.matches(".*? o\\[.*?, \\{\\w+\\}\\]+") && id >= this.chunkedtokens.size()-2){
					token = token.replaceFirst(", \\{\\w+\\}(?=\\]{1,3})","");
					this.chunkedtokens.set(id, token);
				}
				//nested preps
				if(token.matches(".*?\\[p\\[\\w+\\] o\\[\\w+ r\\[p\\[.*")){
					Pattern p = Pattern.compile("(.*?\\[p\\[\\w+)(\\] o\\[)(\\w+ )(r\\[p\\[)(.*)");
					Matcher m = p.matcher(token);
					if(m.matches()){
						token = m.group(1)+" "+m.group(3)+m.group(5).replaceFirst("\\]\\]\\s*$", "");
						this.chunkedtokens.set(id, token);
					}					
				}
				return "ChunkPrep";
			}else if(token.indexOf("-as")>0){//as-wide-as, same-width-as:r[p[{same-width-distally-as}]]
				//a[intensity_level_or_thickness[thin]]
				//repack as ChunkSimpleCharacterState
				token = token.substring(token.lastIndexOf("[")+1, token.indexOf("]")).replaceAll("[{}]", ""); //same-width-distally-as
				String charword = token.replaceFirst(".*?-", "").replaceFirst("-.*", "");
				String chara = Utilities.lookupCharacter(charword, this.conn, ChunkedSentence.characterhash, glosstable, tableprefix);
				if(chara==null) return null;
				else{
					token = token.replace("-", " ");
					String nexttoken = this.chunkedtokens.get(id+1);
					if(nexttoken.indexOf("[")<0){
						token = "a["+chara+"["+token+" "+nexttoken+"]]";
						this.chunkedtokens.set(id+1, "");
					}else token = "a["+chara+"["+token+"]]";
					this.chunkedtokens.set(id, token);
					return "ChunkSimpleCharacterState";
				}
				
			}else{
				return null;
			}
		}
		if(token.startsWith("t[")){
			//this was for FNAv19, but it seemed all t[ chunks were only generated by composeChunk, bypassing this step. t[ chunks generated by chunking does not seem to need this reformatting.
			//reformat c[] in t[]: c: {loosely} {arachnoid} : should be m[loosely] architecture[arachnoid]
			/*Pattern p = Pattern.compile("(.*?\\b)c\\[([^]].*?)\\](.*)");
			Matcher m = p.matcher(token);
			String reformed = "";
			if(m.matches()){
				reformed += m.group(1);
				String c = reformCharacterState(m.group(2));
				reformed += c+ m.group(3);
			}
			this.chunkedtokens.set(id, reformed);*/
			return "ChunkCHPP"; //character/state-pp
		}
		if(token.startsWith("n[")){//returns three different types of ChunkTHAN
			Pattern p = Pattern.compile("\\bthan\\b");
			Matcher m = p.matcher(token);
			m.find();
			//String beforethan = token.substring(0, token.indexOf(" than "));
			String beforethan = token.substring(0, m.start()).trim();
			String charword = beforethan.lastIndexOf(' ')>0 ? beforethan.substring(beforethan.lastIndexOf(' ')+1) : beforethan.replaceFirst("n\\[", "");
			String beforechar = beforethan.replace(charword, "").trim().replaceFirst("n\\[", "");
			
			String chara = null;
			if(!charword.matches("("+ChunkedSentence.more+")")){
				chara = Utilities.lookupCharacter(charword, this.conn, ChunkedSentence.characterhash, glosstable, tableprefix);
			}
			String afterthan = token.substring(token.indexOf(" than ")+6);
			//Case B
			if(afterthan.matches(".*?\\d.*?\\b("+ChunkedSentence.units+"|long|length|wide|width)\\b.*") || afterthan.matches(".*?\\d\\.\\d.*")){// "n[{longer} than 3 (cm)]" => n[size[{longer} than 3 (cm)]]
				if(chara==null){chara="size";}
				token = "n["+token.replaceFirst("n\\[", chara+"[")+"]";
				this.chunkedtokens.set(id, token);
				return "ChunkTHAN"; //character
			}else if(afterthan.matches(".*?\\b\\d\\b.*")){// "n[{longer} than 3 (cm)]" => n[size[{longer} than 3 (cm)]]
				if(chara==null){chara="count";}
				token = "n["+token.replaceFirst("n\\[", chara+"[")+"]";
				this.chunkedtokens.set(id, token);
				return "ChunkTHAN";
			}//Case C
			else if(afterthan.indexOf("(")>=0){ //contains organ
				if(chara==null){//is a constraint, lobed n[more than...]
					token = "n["+token.replaceFirst("n\\[", "constraint[")+"]";
					this.chunkedtokens.set(id, token);
					return "ChunkTHAN";
				}else{//n[more deeply lobed than...
					token = "n["+(beforechar.length()>0? "m["+beforechar+"] ": "")+chara+"["+charword+"] constraint[than "+afterthan+"]";
					this.chunkedtokens.set(id, token);
					return "ChunkTHAN";
				}
			}//Case A n[wider than long]
			else{
				token = "n["+token.replaceFirst("n\\[", chara+"[")+"]";
				this.chunkedtokens.set(id, token);
				return "ChunkTHANC"; //character
			}
		}
		if(token.startsWith("w[")){//w[{proximal} to the (florets)] ; or w[to (midvine)]
			//reformat it to CHPP
			if(token.indexOf("w[to ")>=0){
				token = token.replaceFirst("w\\[to ", "r[p[to] o[")+"]";
				this.chunkedtokens.set(id, token);
				return "ChunkPrep";
			}else{
				token = token.replaceFirst("w\\[","t[c[").replaceFirst("(\\s+|\\b)to\\s+", "] r[p[to] o[")+"]]";
				this.chunkedtokens.set(id, token);
				return "ChunkCHPP";
			}
			
		}
		if(token.startsWith("l[")){
			return "ChunkNPList";
		}
		if(token.startsWith("i[")){
			return "ChunkPPList";
		}
		if(token.startsWith("z[")){
			return "ChunkOrgan";
		}
		if(token.startsWith("u[")){
			return "ChunkNonSubjectOrgan";
		}
		return null;
	}

	/**
	 * 
	 * @param group: {loosely} {arachnoid}
	 * @return:m[loosely] architecture[arachnoid]
	 */
	private String reformCharacterState(String charstring) {
		String result = "";
		String first = "";
		String last = "";
		if(charstring.lastIndexOf(' ')>=0){
			last = charstring.substring(charstring.lastIndexOf(' ')).trim();
			first = charstring.replace(last, "").trim();
			result = "m["+first+"] ";
		}else{
			last = charstring.trim();
		}
			
		String c = Utilities.lookupCharacter(last, conn, characterhash, glosstable, tableprefix);
		if(c!=null){
			result += c+"["+last+"]";
		}else if(Utilities.isVerb(last, verbs, notverbs)){
			result += "v["+last+"]";
		}
	
		return result.trim();
	}
	
	/**
	 * when parsing fails at certain point, forward the pointer to the next comma
	 */
	public void setPointer2NextComma() {
		for(; this.pointer<this.chunkedtokens.size(); pointer++){
			if(this.chunkedtokens.get(pointer).matches("(,|\\.|;|:)")){
				break;
			}
		}
		
	}
	public String getText(){
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select originalsent from "+this.tableprefix+"_sentence where source ='"+sentsrc+"'");
			if(rs.next()){
				this.text = rs.getString(1); //has to use originalsent, because it is "ditto"-fixed (in SentenceOrganStateMarker.java) and perserve capitalization for measurements markup
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return this.text;
	}
	
	public String getSubjectText(){
		return this.subjecttext;
	}

	
	/*
	private void findSubject(){
		String senttag = null;
		String sentmod = null;
		String text = null;
		String taggedtext = null;
		//boolean islifestyle = false;//make this a post-process
		try{
			Statement stmt = conn.createStatement();
			//ResultSet rs = stmt.executeQuery("select modifier, tag, originalsent from "+this.tableprefix+"_sentence where source ='"+sentsrc+"'");
			ResultSet rs = stmt.executeQuery("select modifier, tag, originalsent from "+this.tableprefix+"_sentence where source ='"+sentsrc+"'");
			if(rs.next()){
				senttag = rs.getString(2).trim();
				senttag = senttag.compareTo("general")==0? "whole_organism" : senttag;
				sentmod = rs.getString(1).trim();
				this.text = rs.getString(3); //has to use originalsent, because it is "ditto"-fixed (in SentenceOrganStateMarker.java) and perserve capitalization for measurements markup
			}
			rs = stmt.executeQuery("select rmarkedsent from "+this.tableprefix+"_markedsentence where source ='"+sentsrc+"'");
			if(rs.next()){
				taggedtext = rs.getString(1).trim();
				text = taggedtext.replaceAll("[{}<>]", "").trim();
			}

		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		if(senttag.compareTo("ignore")!=0){
			//sentence subject
			if(senttag.compareTo("whole_organism")==0){
				this.subjecttext = "(whole_organism)";
			}else if(senttag.compareTo("chromosome")==0){
				this.subjecttext = "(chromosome)";
				skipLead("chromosome".split("\\s"));
			}else if(senttag.compareTo("ditto")!=0 && senttag.length()>0){
				//find the subject segment
				String subject = "";
				String [] tokens = text.split("\\s+");
				if(senttag.indexOf("[")<0){ 
					if(senttag.matches(".*\\b(or|and|plus)\\b.*")){// a , c, and/or b
						int or = senttag.lastIndexOf(" or ");
						int and = senttag.lastIndexOf(" and ");
						int ind = or < and ? and : or;
						int plus = senttag.lastIndexOf(" plus ");
						ind = plus < ind ? ind : plus;
						String seg = senttag.substring(ind).replaceAll("oo", "(oo|ee)").trim();// and/or b
						if(seg.indexOf("(oo|ee)")>=0){
							seg =seg.replaceFirst(".$", "\\\\w+\\\\b");
						}else if(seg.length() < 5){
							seg =seg.replaceFirst("..$", "\\\\w+\\\\b");
						}else{
							seg = seg.replaceFirst("...$", "\\\\w+\\\\b");
						}
						//seg = seg.replaceFirst("(and|or) ", "(and|or|plus|,) .*?");
						seg = seg.replaceFirst("(and|or) ", "(\\\\band\\\\b|\\\\bor\\\\b|\\\\bplus\\\\b|,).*?\\\\b");
						//tag derived from complex text expression: "biennial or short_lived perennial" from "iennials or short-lived , usually monocarpic perennials ,"
						seg = seg.replaceAll("(?<=\\W)\\s+(?=\\W)", ".*?")
						.replaceAll("(?<=\\W)\\s+(?=\\w)", ".*?\\\\b")
						.replaceAll("(?<=\\w)\\s+(?=\\W)", "\\\\b.*?")
						.replaceAll("(?<=\\w)\\s+(?=\\w)", "\\\\b.*?\\\\b");
						Pattern p = Pattern.compile("(^.*?"+seg+")");
						Matcher m = p.matcher(text.replaceAll("\\s*-\\s*", "_"));
						if(m.find()){
							subject = m.group(1);
							subject = subject.replaceAll("\\s+-\\s+", "-");
							if(skipLead(subject.split("\\s+"))<0){
								this.subjecttext = null;
							}else{
								String organs = senttag.replaceAll("\\w+\\s+(?!(and |or |plus |$))", "|").replaceAll("\\s*\\|\\s*", "|").replaceAll("(^\\||\\|$)", "").replaceAll("\\|+", "|");//o1|o2
								//turn organ names in subject to singular
								String[] stokens = subject.split("\\s+");
								subject = "";
								for(int i = 0; i < stokens.length; i++){
									String singular = Utilities.toSingular(stokens[i]);
									if(singular.matches("("+organs+")")){
										stokens[i] = singular;
									}
									subject += stokens[i]+" ";
								}
								subject = formatSubject(subject, taggedtext);
								//subject = subject.trim().replaceAll("(?<=\\b("+organs+")\\b) ", ") ").replaceAll(" (?=\\b("+organs+")\\b)", " (").replaceFirst("(?<=\\b("+organs+")\\b)$", ")").replaceFirst("^(?=\\b("+organs+")\\b)", "(").trim();
								//subject = subject.replaceAll("(?<=\\w) ", "} ").replaceAll(" (?=\\w)", " {").replaceAll("(?<=\\w)$", "}").replaceAll("^(?=\\w)", "{").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
								this.subjecttext = subject;
							}
						}	
						
					}else{
						for(int i = 0; i<tokens.length; i++){
							if(Utilities.toSingular(tokens[i]).compareTo(senttag.replaceAll("_", ""))==0){
								subject = subject.replaceAll("\\s+-\\s+", "-");
								subject += tokens[i]+ " ";
								//subject = "{"+subject.trim().replaceAll("[\\[\\]{}()]", "").replaceAll(" ", "} {")+"}";
								//subject = (subject + " ("+tokens[i].replaceAll("[\\[\\]]", "").replaceAll(" ", ") (")+")").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
								//this.subjecttext = addSentmod(subject, sentmod); not used in phenoscape annotation
								this.subjecttext=formatSubject(subject.trim(), taggedtext);
								if(subject.length()>0){
									//skipLead(subject.replaceAll("[\\[\\]{}()]", "").split("\\s+"));
									int skip = skipLead(subject.split("\\s+"));
									if(skip==-1) this.subjecttext=null; //subject search failed.
									break;
								}
							}else{
								subject += tokens[i]+" ";
							}
						}
					}
				}else if(senttag.indexOf("[")>=0){// must not be of-case
					subject = ("{"+sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {")+"} ("+senttag.replaceAll("[\\[\\]]", "").replaceAll(" ", ") (")+")").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
					this.subjecttext=subject;
					String mt = (sentmod+" "+senttag).replaceAll("\\[+.+?\\]+", "").replaceAll("\\s+", " ").trim();
					if(mt.length()>0)
						skipLead(mt.split("\\s+"));
				}

			}else if(senttag.compareTo("ditto")==0){
				if(sentsrc.endsWith("0")){
					this.subjecttext ="(whole_organism)";//it is a starting sentence in a treatment, without an explicit subject.
				}else{
					this.subjecttext ="ditto";
					//mohan code :10/28/2011. If the subject is ditto and the first chunk is a preposition chunk make the subject empty so that it can search within the same sentence for the subject.
					int j=0;
					String text1 = "";
					for(j=0;j<this.chunkedtokens.size();j++)
					{
					text1 = "";
					text1 += this.chunkedtokens.get(j);//gets the first token to check if its a preposition
					if(text1.compareTo("")!=0)
					{
						break;
					}
					}if(text1.matches("r\\[p\\[.*\\]")){
						int i=0;
						for(i=0;i<this.chunkedtokens.size();i++)
						{
							String text2="";
							text2+=this.chunkedtokens.get(i);
							if(text2.matches("(\\<.*\\>)"))
							{
								this.subjecttext =null;
								break;
							}
						}
						
					}
					//End of mohan//
				}
			}
		}else{
			if(this.text.matches(".*?[A-Z]{2,}.*")){ //this.text must be originalsent where captalization is perserved.
				this.subjecttext = "measurements";
			}else{
				this.subjecttext = "ignore";
			}
		}
		if(this.subjecttext!=null && this.subjecttext.endsWith("}")){
			this.subjecttext = null;
			this.pointer = 0;
		}
	}*/
	
	/**
	 * manual digit 
	 * => (manual) (digit) or {manual} (digit) based on the tags used in taggedtext 
	 * @param subject
	 * @param taggedtext
	 * @return
	 */
	private String formatSubject(String subject, String taggedtext) {
		String[] tokens = subject.split("\\s+");
		String formatted = "";
		for(String t: tokens){
			String tag = getTag(t, taggedtext);
			if(tag.contains("<")){
				formatted += "("+t+") ";
			}else if(tag.contains("{")){
				formatted += "{"+t+"} ";
			}else{
				formatted += t+" ";
			}
		}
		formatted = formatted.trim();
		//make sure the last word is in (), in case the word was not tagged with<> in taggedtext
		if(!formatted.endsWith(")")){
			int lasti = formatted.lastIndexOf(" ")<0 ? 0 : formatted.lastIndexOf(" ");
			String lastw = formatted.substring(lasti).replaceAll("\\W", "").trim();
			formatted = formatted.replaceAll(lastw, "("+lastw+")");
		}
		return formatted.replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ").trim();
	}
	
	/**
	 * 
	 * @param t: digit
	 * @param taggedtext: <manual> <digit>
	 * @return: <
	 */
	private String getTag(String t, String taggedtext) {
		if(taggedtext.contains("<"+t+">")) return "<";
		if(taggedtext.contains("<{"+t+"}>")) return "<";
		if(taggedtext.contains("{"+t+"}")) return "{";
		return "";
	}
	/**
	 * sent
	 * @param subject: {basal} (blade)
	 * @param sentmod basal [leaf]
	 * @return
	 */
	private String addSentmod(String subject, String sentmod) {
		if(sentmod.indexOf("[")>=0){
			String[] tokens = subject.split("\\s+");
			String substring = "";
			for(int i = 0; i<tokens.length; i++){
				if(!sentmod.matches(".*?\\b"+tokens[i].replaceAll("[{()}]", "")+"\\b.*")){
					substring +=tokens[i]+" ";
				}
			}
			substring = substring.trim();
			substring ="{"+sentmod.replaceAll("[\\[\\]]", "").replaceAll(" ", "} {").replaceAll("[{(]and[)}]", "and").replaceAll("[{(]or[)}]", "or").replaceAll("\\{\\}", "").replaceAll("\\s+", " ")+"} "+substring;
			return substring;
		}
		return subject;
	}
	/**
	 * 
	 * @param begainindex (inclusive)
	 * @param endindex (not include)
	 * @return element in the range
	 */
	public String getText(int begainindex, int endindex) {
		String text = "";
		for(int i = begainindex; i < endindex; i++){
			text += this.chunkedtokens.get(i)+" ";
		}
		return text.replaceAll("\\s+", " ").trim();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	public String getTokenAt(int i) {
		return this.chunkedtokens.get(i);
	}
	public void setClauseModifierConstraint(String modifier, String constraintId) {
		this.clauseModifierConstraint = modifier;		
		this.clauseModifierContraintId = constraintId;
	}
	public ArrayList<String> getClauseModifierConstraint() {//apply to all characters in this chunkedsentence
		if(this.clauseModifierConstraint!=null){
			ArrayList<String> mc = new ArrayList<String>();
			mc.add(this.clauseModifierConstraint);	
			if(this.clauseModifierContraintId!=null) mc.add(this.clauseModifierContraintId);
			return mc;
		}else{
			return null;
		}
	}
	

	
	
}
