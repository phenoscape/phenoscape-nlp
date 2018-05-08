/**
 * 
 */
package conceptmapping;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import oboaccessor.OBO2DB;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.model.OWLClass;

import owlaccessor.OWLAccessorImpl;

// TODO: Auto-generated Javadoc
/**
 * The Class TermOutputer.
 *
 * @author Hong Updates
 * This class output extracted terms and their mapping PATO concepts to a table, including source info.
 * Run this class after completing CharaParser(4Phenoscape).step2 (2.1, 2.2, 2.3, and 2.4)
 * 11/23: rewrite to accommodate OBO format
 * 
 * 
 * TAO: http://purl.obolibrary.org/obo/tao.owl
 * VAO(Vertebrate Anatomy Ontology): https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/skeletal/obo/vertebrate_anatomy.obo
 * AAO(Amniote Anatomy Ontology): https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/amniote_draft.obo
 */
public class TermOutputer {
	
	/** The conn. */
	private Connection conn;
	
	/** The database. */
	private String database;
	
	/** The username. */
	private String username = "root";
	
	/** The password. */
	private String password = "root";
	
	/** The entitytable. */
	private String entitytable = "entity";
	
	/** The qualitytable. */
	private String qualitytable = "quality";
	
	/** The entity ontos path. */
	private ArrayList<String> entityOntosPath =null;
	
	/** The quality ontos path. */
	private ArrayList<String> qualityOntosPath =null;
	
	/** The glosstable. */
	private String glosstable = null;
	
	/** The sourceprefix. */
	private String sourceprefix = null;

	private static final Logger LOGGER = Logger.getLogger(TermOutputer.class);  
	
	/**
	 * Instantiates a new term outputer.
	 *
	 * @param database the database
	 * @param outputtableprefix the outputtableprefix
	 * @param eOntoPaths the e onto paths
	 * @param qOntoPaths the q onto paths
	 * @param glosstable the glosstable
	 * @param sourceprefix the sourceprefix
	 */
	public TermOutputer(String database, String outputtableprefix, ArrayList<String> eOntoPaths, ArrayList<String> qOntoPaths, String glosstable, String sourceprefix) {
		this.entitytable = outputtableprefix+"_"+entitytable;
		this.qualitytable = outputtableprefix+"_"+qualitytable;
		this.entityOntosPath = eOntoPaths;
		this.qualityOntosPath = qOntoPaths;
		this.glosstable = glosstable;
		this.sourceprefix = sourceprefix;
		this.database = database;
		//String PATOURL="C:/Documents and Settings/Hong Updates/Desktop/Australia/phenoscape-fish-source/pato.owl";
		//String TAOURL="C:/Documents and Settings/Hong Updates/Desktop/Australia/phenoscape-fish-source/tao.owl";
		//this.patoapi = new OWLAccessorImpl(new File(PATOURL));
		//this.taoapi = new OWLAccessorImpl(new File(TAOURL));
		try{
			if(conn == null){
				Class.forName("com.mysql.jdbc.Driver");
				String URL = "jdbc:mysql://localhost/"+database+"?user="+username+"&password="+password;
				conn = DriverManager.getConnection(URL);
				Statement stmt = conn.createStatement();
				//stmt.execute("drop table if exists "+ entitytable); TODO uncomment
				//stmt.execute("create table if not exists "+entitytable+" (id int(11) not null unique auto_increment primary key, term varchar(100), ontoid text, ontolabel text, characterr text, characterstate text, source text)"); TODO uncomment
				stmt.execute("drop table if exists "+qualitytable);
				stmt.execute("create table if not exists "+qualitytable+" (id int(11) not null unique auto_increment primary key, term varchar(100), ontoid text, ontolabel text, characterr text, characterstate text, source text)");
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}

	/**
	 * Output.
	 */
	public void output(){
		ArrayList<String> entities =getEterms();
		ArrayList<String> qualities = getQterms();
		//outputTerms(entities, entitytable); TODO, uncomment
		outputTerms(qualities, qualitytable);
	}
	
	
	/**
	 * Process term before look it up in ontology and insert into database.
	 *
	 * @param term the original form of the term
	 * @param type 
	 * @return the processed term
	 */
	private String processTerm(String term, String type){
		String termProcessed = term;
		
		//Step 1: replace all underscores with hyphens. Universal for entities and qualities.
		termProcessed = termProcessed.replaceAll("_", "-");
		
		//Step 2: transform plurals to singular. Only for entities. 
		if(type.compareTo(this.entitytable)==0 && outputter.knowledge.TermOutputerUtilities.isPlural(termProcessed)){
			termProcessed=outputter.knowledge.TermOutputerUtilities.toSingular(termProcessed);
		}
		
		return termProcessed;
	}
	
	/**
	 * Output terms.
	 *
	 * @param entities the entities
	 * @param type the type
	 */
	private void outputTerms(ArrayList<String> entities, String type) {
		Iterator<String> it = entities.iterator();
		String outtableo = type;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = null;
			while(it.hasNext()){
				String term = it.next();
				type = outtableo;
				//use the pre-processed term for ontology looking up
				String termProcessed = this.processTerm(term, type);
				String[] ontoidinfo = findID(termProcessed, type);
				String ontoid = "";
				String ontolabel = "";
				if(ontoidinfo !=null){
					type = ontoidinfo[0];
					ontoid = ontoidinfo[1];
					ontolabel = ontoidinfo[2];
				}
				
				//we are using the original form of the term to look up sources, don't make changes to the original form before this point.
				rs = stmt.executeQuery("select distinct source, sentence, type from "+this.sourceprefix+"_sentence where sentence rlike '(^|[^a-z])"+term+"([^a-z]|$)'" );
				String sourcelist = "sourcelist|"; //this is so that the first source is not to match "()".
				String source = "";
				String sentence = "";
				String character = "";
				while(rs.next()){
					source=rs.getString("source");
					if(!source.matches("("+sourcelist.replaceFirst("\\|$", "")+")")){
						sourcelist +=rs.getString("source")+"|";						
						character = rs.getString("type");
						sentence = rs.getString("sentence");
						type = type.replaceFirst(";+$", "");
						//insert the prcessed term 
						addrecord(termProcessed, ontoid, ontolabel, source.toString(), character, sentence, type);
					}
				}
				/*rs = stmt.executeQuery("select distinct pdf, charnumber, characterr, sentence from "+this.sourceprefix+"_original where sentence rlike '(^|[^a-z])"+term+"([^a-z]|$)' or characterr rlike '(^|[^a-z])"+term+"([^a-z]|$)'" );
				String sourcelist = "sourcelist|"; //this is so that the first source is not to match "()".
				String source = "";
				String sentence = "";
				String character = "";
				while(rs.next()){
					source=rs.getString("pdf")+":"+rs.getString("charnumber");
					if(!source.matches("("+sourcelist.replaceFirst("\\|$", "")+")")){
						sourcelist +=rs.getString("pdf")+":"+rs.getString("charnumber")+"|";						
						character = rs.getString("characterr");
						sentence = rs.getString("sentence");
						type = type.replaceFirst(";+$", "");
						addrecord(term, ontoid, ontolabel, source.toString(), character, sentence, type);
					}
				}*/
				
				
			}
			rs.close();
			stmt.close();
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}

	/**
	 * Addrecord.
	 *
	 * @param term the term
	 * @param ontoid the ontoid
	 * @param ontolabel the ontolabel
	 * @param source the source
	 * @param character the character
	 * @param sentence the sentence
	 * @param outtable the outtable
	 */
	private void addrecord(String term, String ontoid, String ontolabel, String source, String character,
			String sentence, String outtable) {
		try{
			Statement stmt = conn.createStatement();
			sentence = sentence.replaceAll("\"", "\\\\\"");
			character = character.replaceAll("\"", "\\\\\"");
			String q = "insert into "+outtable+"(term, ontoid, ontolabel, characterr, characterstate, source) values (\""+term+"\",\""+ontoid+"\",\""+ontolabel+"\",\""+character+"\",\""+sentence+"\",\""+source+"\")";
			System.out.println(q);
			stmt.execute(q);
		}catch(Exception e){
			LOGGER.error("", e);
		}
	}

	/**
	 * Find id.
	 *
	 * @param term the term
	 * @param type the type
	 * @return the string[]
	 */
	private String[] findID(String term, String type) {
		String qualityid = "";
		String entityid = "";
		String qualitylabel = "";
		String entitylabel="";
		try{
			//check annotated record
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select qualityontoid from phenoscape.fish_original_1st_all where qualitylabel='"+term+"'");
			if(rs.next()){
				qualityid = rs.getString("qualityontoid");			
				qualitylabel = term;
			}
			rs = stmt.executeQuery("select entityontoid from phenoscape.fish_original_1st_all where entitylabel='"+term+"'");
			if(rs.next()){
				entityid = rs.getString("entityontoid");
				entitylabel = term;
			}
			if((entityid+qualityid).trim().length()==0){
				return searchOntologies(term, type);
			}else if(entityid.length()>0){
				return new String[]{this.entitytable, entityid, entitylabel};
			}else if(qualityid.length()>0){
				return new String[]{this.qualitytable, qualityid, qualitylabel};
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
		return null;
	}
		
	/**
	 * use OWL API.
	 *
	 * @param term the term
	 * @param type the type
	 * @return the string[]
	 * @throws Exception the exception
	 */
	private String[] searchOntologies(String term, String type) throws Exception {
		//search quality ontologies
		String[] results = new String[]{"", "", ""};
		boolean added = false;
		if(type.compareTo(this.qualitytable)==0){
			for(String qonto: this.qualityOntosPath){
				if(qonto.endsWith(".owl")){
					OWLAccessorImpl owlapi = new OWLAccessorImpl(new File(qonto), new ArrayList<String>());
					String[] result = searchOWLOntology(term, owlapi, type);
					if(result!=null){
						added = true;
						results = add(results, result);
					}
				}else if(qonto.endsWith(".obo")){
					String[] result = searchOBOOntology(term, qonto, type);
					if(result!=null){
						added = true;
						results = add(results, result);
					}
				}
			}
		}else if(type.compareTo(this.entitytable)==0){
			for(String eonto: this.entityOntosPath){
				if(eonto.endsWith(".owl")){
					OWLAccessorImpl owlapi = new OWLAccessorImpl(new File(eonto), new ArrayList<String>());
					String[] result = searchOWLOntology(term, owlapi, type);
					if(result!=null){
						added = true;
						results = add(results, result);
					}
				}else if(eonto.endsWith(".obo")){
					String[] result = searchOBOOntology(term, eonto, type);
					if(result!=null){
						added = true;
						results = add(results, result);
					}
				}
			}
		}
		if(added){
			return results;
		}else{
			return null;
		}
		/*String[] patoresult = searchOntology(term, this.qualitytable);
		String[] taoresult = searchOntology(term, this.entitytable);

		if(patoresult==null && taoresult!=null) return taoresult;
		if(patoresult!=null && taoresult==null) return patoresult;
		if(patoresult!=null && taoresult!=null) return taoresult;*/
			
		
		
		
		/*{//merge
			String[] results = new String[patoresult.length + taoresult.length];
			int i; int j;
			for(i=0, j=0; i<patoresult.length; i++, j++){
				results[i] = patoresult[j];
			}
			for(i=patoresult.length, j=0; i<patoresult.length + taoresult.length; i++, j++){
				results[i] = taoresult[j];
			}
			return results;
		}*/
		
	}

	/**
	 * Search obo ontology.
	 *
	 * @param term the term
	 * @param ontofile the ontofile
	 * @param type the type
	 * @return the string[]
	 * @throws Exception the exception
	 */
	private String[] searchOBOOntology(String term, String ontofile, String type) throws Exception{
		String [] result = new String[3];
		int i = ontofile.lastIndexOf("/");
		int j = ontofile.lastIndexOf("\\");
		i = i>j? i:j;
		String ontoname = ontofile.substring(i+1).replaceFirst("\\.obo", "");
		OBO2DB o2d = new OBO2DB("obo", ontofile ,ontoname);
		String[] match = o2d.getID(term);
		if(match !=null){
			result[0] = type;
			result[1] = match[0]; //id
			result[2] = match[1]; //label
		}else{
			result = null;
		}
		return result;
	}

	/**
	 * Adds the.
	 *
	 * @param results the results
	 * @param result the result
	 * @return the string[]
	 */
	private String[] add(String[] results, String[] result) {
		if(result == null) return results;
		int start = 1;
		if(results[0].length()==0 && results[1].length()==0 && results[2].length()==0 ){//initialization
			start =0;
		}
		for(int i = start; i < 3; i++){
			results[i] += result[i]+";";
		}
		return results;
	}

	/**
	 * Search owl ontology.
	 *
	 * @param term the term
	 * @param owlapi the owlapi
	 * @param type the type
	 * @return the string[]
	 * @throws Exception the exception
	 */
	private String[] searchOWLOntology(String term, OWLAccessorImpl owlapi, String type)throws Exception {
		String[] result = null;
		Hashtable<String, ArrayList<OWLClass>> typedmatches = owlapi.retrieveConcept(term);
		ArrayList<OWLClass> matches = typedmatches.get("original");
		matches.addAll(typedmatches.get("exact"));
		//matches.addAll(typedmatches.get("narrow"));
		//matches.addAll(typedmatches.get("related"));
		Iterator<OWLClass> it = matches.iterator();
		
		//exact match first
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			if(label.compareToIgnoreCase(term)==0){
				result= new String[3];
				result[0] = type;
				result[1] = c.toString().replaceFirst("http.*?(?=(PATO|TAO|VAO|AMAO|AAO|UBERON)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "");//id
				result[2] = label;
				return result;
			}
		}
		//otherwise, append all possible matches
		it = matches.iterator();
		result = new String[]{"", "", ""};
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			result[0] = type;
			result[1] += c.toString().replaceFirst(".*http.*?(?=(PATO|TAO|VAO|AMAO|AAO|UBERON)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "")+";";
			result[2] += label+";";
		}
		if(result[1].length()>0){
			result[1] = result[1].replaceFirst(";$", "");
			result[2] = result[2].replaceFirst(";$", "");
			return result;
		}else{
			return null;
		}
	}	

	/**
	 * Gets the qterms.
	 *
	 * @return the qterms
	 */
	private ArrayList<String> getQterms() {
		ArrayList<String> qterms = new ArrayList<String>();
		try{
			String q = "SELECT distinct word FROM "+this.sourceprefix+"_unknownwords where "+
//<<<<<<< HEAD
//			"word in (select term from phenoscape."+this.glosstable+" where category !='structure') or "+
//=======
			"word in (select term from "+this.glosstable+" where category !='structure') or "+
			"word in (select word from "+this.sourceprefix+"_wordroles p where semanticrole ='c') or "+
			"word in (select term from "+this.sourceprefix+"_term_category where category !='structure')";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(q);
			while(rs.next()){
				String word = rs.getString("word");
				if(!qterms.contains(word)){
					qterms.add(word);
				}
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
		return qterms;
	}
	
	/**
	 * Gets the eterms.
	 *
	 * @return the eterms
	 */
	private ArrayList<String> getEterms() {
		ArrayList<String> eterms = new ArrayList<String>();
		try{
			String q = "SELECT distinct word FROM "+this.sourceprefix+"_unknownwords where "+
//<<<<<<< HEAD
//			"word in (select term from phenoscape."+this.glosstable+" where category ='structure') or "+
//=======
			"word in (select term from "+this.glosstable+" where category ='structure') or "+
//>>>>>>> branch 'master' of ssh://git@github.com/zilongchang/phenoscape-nlp.git
			"word in (select word from "+this.sourceprefix+"_wordroles p where semanticrole in ('os', 'op')) or "+
			"word in (select term from "+this.sourceprefix+"_term_category where category ='structure')";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(q);
			while(rs.next()){
				String word = rs.getString("word");
				if(!eterms.contains(word)){
					eterms.add(word);
				}
			}
		}catch(Exception e){
			LOGGER.error("", e);
		}
		return eterms;
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		//need an database "obo" (may be empty) if search obo ontologies
		/*
		String database = "phenoscape";
		String outputtableprefix = "pheno_amphibia";
		String glosstable = "fishglossaryfixed";
		//changed to amphibia (was archosaur)
		String sourceprefix = "pheno_amphibia";
		ArrayList<String> eOntoPaths = new ArrayList<String>();
		//changed
		eOntoPaths.add("C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\vertebrate_anatomy.obo");
		//eOntoPaths.add("C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\AAO.obo");
		//eOntoPaths.add("C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\AA.obo");
		
		ArrayList<String> qOntoPaths = new ArrayList<String>();
		qOntoPaths.add("C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\pato.owl");

		*/
		//The three xml files sent by Alex on July 23, 2012
		String database = "biocreative2012";
		String outputtableprefix = "pheno_alex";
		String glosstable = "fishglossaryfixed";
		
		String sourceprefix = "phenoalex";

		ArrayList<String> eOntoPaths = new ArrayList<String>();
		//changed
		eOntoPaths.add("C:\\Users\\Zilong Chang\\Desktop\\onto\\phenoscape-ext.owl");
		
		ArrayList<String> qOntoPaths = new ArrayList<String>();
		qOntoPaths.add("C:\\Users\\Zilong Chang\\Desktop\\onto\\pato.owl");
		qOntoPaths.add("C:\\Users\\Zilong Chang\\Desktop\\onto\\bspo.owl");
		
		TermOutputer to = new TermOutputer(database, outputtableprefix, eOntoPaths, qOntoPaths, glosstable, sourceprefix);
		to.output();
		
	}

}
