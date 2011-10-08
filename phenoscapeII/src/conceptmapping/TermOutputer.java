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

import org.semanticweb.owlapi.model.OWLClass;

import owlaccessor.OWLAccessorImpl;

/**
 * @author Hong Updates
 * This class output extracted terms and their mapping PATO concepts to a table, including source info
 *TAO: http://purl.obolibrary.org/obo/tao.owl
 */
public class TermOutputer {
	private Connection conn;
	private String username = "root";
	private String password = "root";
	private String entitytable = "entity";
	private String qualitytable = "quality";
	private OWLAccessorImpl patoapi = null;
	private OWLAccessorImpl taoapi = null;
	
	/**
	 * 
	 */
	public TermOutputer(String database, String tableprefix) {
		this.entitytable = tableprefix+"_"+entitytable;
		this.qualitytable = tableprefix+"_"+qualitytable;
		String PATOURL="C:/Documents and Settings/Hong Updates/Desktop/Australia/phenoscape-fish-source/pato.owl";
		String TAOURL="C:/Documents and Settings/Hong Updates/Desktop/Australia/phenoscape-fish-source/tao.owl";
		this.patoapi = new OWLAccessorImpl(new File(PATOURL));
		this.taoapi = new OWLAccessorImpl(new File(TAOURL));
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
	
	
	private void outputTerms(ArrayList<String> entities, String outtable) {
		Iterator<String> it = entities.iterator();
		String outtableo = outtable;
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = null;
			while(it.hasNext()){
				String term = it.next();
				outtable = outtableo;
				String[] ontoidinfo = findID(term);
				String ontoid = "";
				String ontolabel = "";
				if(ontoidinfo !=null){
					outtable = ontoidinfo[0];
					ontoid = ontoidinfo[1];
					ontolabel = ontoidinfo[2];
				}
				rs = stmt.executeQuery("select distinct pdf, charnumber, characterr, sentence from fish_original where sentence rlike '(^|[^a-z])"+term+"([^a-z]|$)'" );
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
						addrecord(term, ontoid, ontolabel, source.toString(), character, sentence, outtable);
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
			//System.out.println(q);
			stmt.execute(q);
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private String[] findID(String term) {
		String qualityid = "";
		String entityid = "";
		String qualitylabel = "";
		String entitylabel="";
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select qualityontoid from fish_original where qualitylabel='"+term+"'");
			if(rs.next()){
				qualityid = rs.getString("qualityontoid");			
				qualitylabel = term;
			}
			rs = stmt.executeQuery("select entityontoid from fish_original where entitylabel='"+term+"'");
			if(rs.next()){
				entityid = rs.getString("entityontoid");
				entitylabel = term;
			}
			if((entityid+qualityid).trim().length()==0){
				return searchOntologies(term);
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
	private String[] searchOntologies(String term) {

		String[] patoresult = searchOntology(term, patoapi, this.qualitytable);
		String[] taoresult = searchOntology(term, taoapi, this.entitytable);

		if(patoresult==null && taoresult!=null) return taoresult;
		if(patoresult!=null && taoresult==null) return patoresult;
		if(patoresult!=null && taoresult!=null) return taoresult;
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
		return null;
	}

	private String[] searchOntology(String term, OWLAccessorImpl owlapi, String type) {
		List<OWLClass> matches = owlapi.retrieveConcept(term);
		Iterator<OWLClass> it = matches.iterator();
		String[] result = null;
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			if(label.compareToIgnoreCase(term)==0){
				result= new String[3];
				result[0] = type;
				result[1] = c.toString().replaceFirst("http.*?(?=(PATO|TAO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "");
				result[2] = label;
				return result;
			}
		}
		it = matches.iterator();
		result = new String[]{"", "", ""};
		while(it.hasNext()){
			OWLClass c = it.next();
			String label = owlapi.getLabel(c);
			result[0] = type;
			result[1] += c.toString().replaceFirst(".*http.*?(?=(PATO|TAO)_)", "").replaceFirst("_", ":").replaceAll("[<>]", "")+";";
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
			String q = "SELECT distinct word FROM markedupdatasets.pheno_fish_allwords where "+
			"word in (select term from markedupdatasets.antglossaryfixed where category !='structure') or "+
			"word in (select word from markedupdatasets.pheno_fish_wordroles p where semanticrole ='c')";
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
			String q = "SELECT distinct word FROM markedupdatasets.pheno_fish_allwords where "+
			"word in (select term from markedupdatasets.antglossaryfixed where category ='structure') or "+
			"word in (select word from markedupdatasets.pheno_fish_wordroles p where semanticrole in ('os', 'op'))";
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
		TermOutputer to = new TermOutputer("phenoscape", "pheno_fish_rel_syn");
		to.output();
		
	}

}
