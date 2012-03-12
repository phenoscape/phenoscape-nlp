/**
 * 
 */
package conceptmapping;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import oboaccessor.OBO2DB;

import org.semanticweb.owlapi.model.OWLClass;

import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Updates
 * This class output extracted terms and their mapping PATO concepts to a table, including source info.
 * 
 * 11/23: rewrite to accommodate OBO format
 * 
 * 
 * TAO: http://purl.obolibrary.org/obo/tao.owl
 * VAO(Vertebrate Anatomy Ontology): https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/skeletal/obo/vertebrate_anatomy.obo
 * AAO(Amniote Anatomy Ontology): https://phenoscape.svn.sourceforge.net/svnroot/phenoscape/trunk/vocab/amniote_draft.obo
 */
public class TermOutputer {
	private Connection conn;
	private String username = "root";
	private String password = "root";
	private String entitytable = "entity";
	private String qualitytable = "quality";
	private ArrayList<String> entityOntosPath =null;
	private ArrayList<String> qualityOntosPath =null;
	private String glosstable = null;
	private String sourceprefix = null;

	
	/**
	 * 
	 */
	public TermOutputer(String database, String outputtableprefix, ArrayList<String> eOntoPaths, ArrayList<String> qOntoPaths, String glosstable, String sourceprefix) {
		this.entitytable = outputtableprefix+"_"+entitytable;
		this.qualitytable = outputtableprefix+"_"+qualitytable;
		this.entityOntosPath = eOntoPaths;
		this.qualityOntosPath = qOntoPaths;
		this.glosstable = glosstable;
		this.sourceprefix = sourceprefix;
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
				stmt.execute("drop table if exists "+ entitytable);
				stmt.execute("create table if not exists "+entitytable+" (id int(11) not null unique auto_increment primary key, term varchar(100), ontoid text, ontolabel text, characterr text, characterstate text, source text)");
				stmt.execute("drop table if exists "+qualitytable);
				stmt.execute("create table if not exists "+qualitytable+" (id int(11) not null unique auto_increment primary key, term varchar(100), ontoid text, ontolabel text, characterr text, characterstate text, source text)");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	public void output(){
		ArrayList<String> entities =getEterms();
		ArrayList<String> qualities = getQterms();
		outputTerms(entities, entitytable);
		outputTerms(qualities, qualitytable);
	}
	
	
	private void outputTerms(ArrayList<String> entities, String type) {
		Iterator<String> it = entities.iterator();
		String outtableo = type;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = null;
			while(it.hasNext()){
				String term = it.next();
				type = outtableo;
				String[] ontoidinfo = findID(term, type);
				String ontoid = "";
				String ontolabel = "";
				if(ontoidinfo !=null){
					type = ontoidinfo[0];
					ontoid = ontoidinfo[1];
					ontolabel = ontoidinfo[2];
				}
				rs = stmt.executeQuery("select distinct pdf, charnumber, characterr, sentence from "+this.sourceprefix+"_original where sentence rlike '(^|[^a-z])"+term+"([^a-z]|$)' or characterr rlike '(^|[^a-z])"+term+"([^a-z]|$)'" );
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
				}
			}
			rs.close();
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

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
			e.printStackTrace();
		}
	}

	private String[] findID(String term, String type) {
		String qualityid = "";
		String entityid = "";
		String qualitylabel = "";
		String entitylabel="";
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select qualityontoid from "+this.sourceprefix+"_original where qualitylabel='"+term+"'");
			if(rs.next()){
				qualityid = rs.getString("qualityontoid");			
				qualitylabel = term;
			}
			rs = stmt.executeQuery("select entityontoid from "+this.sourceprefix+"_original where entitylabel='"+term+"'");
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
			e.printStackTrace();
		}
		return null;
	}
		
	/**
	 * use OWL API
	 * @param term
	 * @return
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

	private String[] searchOBOOntology(String term, String ontofile, String type) {
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

	private String[] searchOWLOntology(String term, OWLAccessorImpl owlapi, String type)throws Exception {
		String[] result = null;
		List<OWLClass> matches = owlapi.retrieveConcept(term);
		Iterator<OWLClass> it = matches.iterator();
		
		//exact match first
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			if(label.compareToIgnoreCase(term)==0){
				result= new String[3];
				result[0] = type;
				result[1] = c.toString().replaceFirst("http.*?(?=(PATO|TAO|VAO|AMAO|AAO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "");//id
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
			result[1] += c.toString().replaceFirst(".*http.*?(?=(PATO|TAO|VAO|AMAO|AAO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "")+";";
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
	
	

	private ArrayList<String> getQterms() {
		ArrayList<String> qterms = new ArrayList<String>();
		try{
			String q = "SELECT distinct word FROM markedupdatasets."+this.sourceprefix+"_unknownwords where "+
//<<<<<<< HEAD
//			"word in (select term from phenoscape."+this.glosstable+" where category !='structure') or "+
//=======
			"word in (select term from markedupdatasets."+this.glosstable+" where category !='structure') or "+
			"word in (select word from markedupdatasets."+this.sourceprefix+"_wordroles p where semanticrole ='c') or "+
			"word in (select term from markedupdatasets."+this.sourceprefix+"_term_category where category !='structure')";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(q);
			while(rs.next()){
				qterms.add(rs.getString("word"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return qterms;
	}

	private ArrayList<String> getEterms() {
		ArrayList<String> eterms = new ArrayList<String>();
		try{
			String q = "SELECT distinct word FROM markedupdatasets."+this.sourceprefix+"_unknownwords where "+
//<<<<<<< HEAD
//			"word in (select term from phenoscape."+this.glosstable+" where category ='structure') or "+
//=======
			"word in (select term from markedupdatasets."+this.glosstable+" where category ='structure') or "+
//>>>>>>> branch 'master' of ssh://git@github.com/zilongchang/phenoscape-nlp.git
			"word in (select word from markedupdatasets."+this.sourceprefix+"_wordroles p where semanticrole in ('os', 'op')) or "+
			"word in (select term from markedupdatasets."+this.sourceprefix+"_term_category where category ='structure')";
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery(q);
			while(rs.next()){
				eterms.add(rs.getString("word"));
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return eterms;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//need an database "obo" (may be empty) if search obo ontologies
		String database = "phenoscape";
		String outputtableprefix = "pheno_amphibia";
		String glosstable = "fishglossaryfixed";
		//changed to amphibia (was archosaur)
		String sourceprefix = "pheno_amphibia";
		ArrayList<String> eOntoPaths = new ArrayList<String>();
		//changed
		eOntoPaths.add("C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\vertebrate_anatomy.obo");
		eOntoPaths.add("C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\AAO.obo");
		eOntoPaths.add("C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\AA.obo");
		
		ArrayList<String> qOntoPaths = new ArrayList<String>();
		qOntoPaths.add("C:\\Users\\Zilong Chang\\Documents\\WORK\\Ontology\\pato.owl");
		
		TermOutputer to = new TermOutputer(database, outputtableprefix, eOntoPaths, qOntoPaths, glosstable, sourceprefix);
		to.output();
		
	}

}
