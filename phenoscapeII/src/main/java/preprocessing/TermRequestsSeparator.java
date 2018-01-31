/**
 * 
 */
package preprocessing;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasoner;

import owlaccessor.OWLAccessorImpl;
import uk.ac.manchester.cs.owlapi.modularity.ModuleType;
import uk.ac.manchester.cs.owlapi.modularity.SyntacticLocalityModuleExtractor;

/**
 * @author Hong Cui
 * 
 * for CharaParser 2013 evaluation with Phenoscape project. 
 * Jim sent term requests to UBERON, BSPO, and PATO in one owl file.
 * this class separate requests for different ontologies.
 *
 */
public class TermRequestsSeparator {
	private static final Logger LOGGER = Logger.getLogger(OWLAccessorImpl.class);  
	OWLOntologyManager manager;
	OWLDataFactory df;
	OWLOntology rootOnt;
	Set<OWLClass> allclasses=new HashSet<OWLClass>();
	Hashtable<String, String> outputnames = new Hashtable<String, String> (); 
	int totalEntities = 0;
	/**
	 * 
	 */
	public TermRequestsSeparator(String outputname, String inputfile) {
		this.outputnames.put("uberon", outputname+"_uberon");
		this.outputnames.put("bspo", outputname+"_bspo");
		this.outputnames.put("pato", outputname+"_pato");
		//this.outputnames.put("others", outputname+"_others");

		manager = OWLManager.createOWLOntologyManager();
		df = manager.getOWLDataFactory();
		try{
			rootOnt = manager.loadOntologyFromOntologyDocument(new File(inputfile));		
			allclasses.addAll(rootOnt.getClassesInSignature(true));
			totalEntities = rootOnt.getSignature(true).size();
			System.out.println("Some statistics of the module:"+rootOnt);
			System.out.println(" " + totalEntities + " entities");
			System.out.println(" " + rootOnt.getLogicalAxiomCount() + " logical axioms");
			System.out.println(" " + (rootOnt.getAxiomCount() - rootOnt.getLogicalAxiomCount())
					+ " other axioms");
		}catch(Exception e){
			System.out.println("can't load ontology:"+inputfile);
			LOGGER.error("", e);
			System.exit(1);
		}
	}

	/**
	 * PATO, BSPO, and UBERON
	 */
	public void separate() throws Exception{
		Set<OWLEntity> uberon = new HashSet<OWLEntity>();
		Set<OWLEntity> bspo = new HashSet<OWLEntity>();
		Set<OWLEntity> pato = new HashSet<OWLEntity>();
		//Set<OWLEntity> others = new HashSet<OWLEntity>();
		//collect classes 
		for(OWLClass clas: allclasses){
			String uri = clas.getIRI().toString();

			/*if(uri.contains("UBERON")){
				uberon.add(clas);
				//uberon.addAll((Collection<? extends OWLEntity>) clas.getSuperClasses(rootOnt));
			} else 	if(uri.contains("BSPO")){
				bspo.add(clas);
				//bspo.addAll((Collection<? extends OWLEntity>) clas.getSuperClasses(rootOnt));
			} else if(uri.contains("PATO")){
				pato.add(clas);
				//pato.addAll((Collection<? extends OWLEntity>) clas.getSuperClasses(rootOnt));
			} else{ 				
				System.err.println("Look: "+clas.getIRI().toString());
			}*/
			//this omits the subclassof properties and the superclasses.
			if(uri.contains("UBERONTEMP")){
				uberon.add(clas);
				uberon.addAll(subSuperClasses(clas));
			} else 	if(uri.contains("BSPOTEMP")){
				bspo.add(clas);
				bspo.addAll(subSuperClasses(clas));
			} else if(uri.contains("PATOTEMP")){
				pato.add(clas);
				pato.addAll(subSuperClasses(clas));
			} else{
				//others.add(clas);
				//others.addAll(subSuperClasses(clas));
				if(!uri.contains("UBERON") && !uri.contains("BSPO") && !uri.contains("PATO"))
					System.err.println("Look: "+clas.getIRI().toString());
				
			}
		}
		//deduplicate others
		/*Set<OWLEntity> rest = new HashSet<OWLEntity>();
		for(OWLEntity c: others){
			if(!uberon.contains(c) && !bspo.contains(c) && !pato.contains(c)){
				rest.add(c);
			}
		}
		
		if(totalEntities == bspo.size()+uberon.size()+pato.size()+rest.size()){
			System.out.println("all is covered");
		}else{
			System.out.println("diff is "+ (totalEntities - (bspo.size()+uberon.size()+pato.size()+rest.size())));
		}*/	

		//extrac onto
		outputOntology(uberon, "uberon");
		outputOntology(bspo, "bspo");
		outputOntology(pato, "pato");
		//outputOntology(rest, "others");

	}

	/**
	 * 
	 * @param clas
	 * @return sub/super classes of clas including the classes mentioned in restrictions
	 */
	private Set<OWLEntity> subSuperClasses(OWLClass clas) {
		Set<OWLEntity> results = new HashSet<OWLEntity>();
		Set<OWLClassExpression> all = clas.getSuperClasses(rootOnt);
		all.addAll(clas.getSubClasses(rootOnt));
		for(OWLClassExpression oce: all){
			if(oce instanceof OWLClass){
				results.add((OWLEntity) oce);
			}else{
				//Set<OWLClassExpression> cons = oce.asConjunctSet();
				//Set<OWLClassExpression> dis = oce.asDisjunctSet();
				Set<OWLClass> cs = oce.getClassesInSignature();
				//oce.getNestedClassExpressions()
				results.addAll(cs);
			}
		}

		return results;
	}

	private void outputOntology(Set<OWLEntity> uberon, String onto)
			throws OWLOntologyCreationException, OWLOntologyStorageException {
		SyntacticLocalityModuleExtractor sme = new SyntacticLocalityModuleExtractor(manager,
				rootOnt, ModuleType.STAR);

		IRI moduleIRI = IRI.create("file:///"+outputnames.get(onto)+".owl");
		//OWLOntology mod = sme.extractAsOntology(uberon, moduleIRI);
		OWLOntology mod = sme.extractAsOntology(uberon, moduleIRI, -1, -1, new StructuralReasoner(rootOnt, new SimpleConfiguration(),
				BufferingMode.NON_BUFFERING));

		System.out.println("Some statistics of the module:"+onto);
		System.out.println(" " + mod.getSignature(true).size() + " entities");
		System.out.println(" " + mod.getLogicalAxiomCount() + " logical axioms");
		System.out.println(" " + (mod.getAxiomCount() - mod.getLogicalAxiomCount())
				+ " other axioms");
		System.out.println("Saving the module as " + mod.getOntologyID().getOntologyIRI());
		manager.saveOntology(mod);
		System.out.println();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String outputname="C:/Users/updates/CharaParserTest/Ontologies/charaparser_eval/term_requests_ALL";
		String inputfile = "C:/Users/updates/CharaParserTest/Ontologies/charaparser_eval/term_requests_ALL.owl";
		try{
			TermRequestsSeparator trs = new TermRequestsSeparator(outputname, inputfile);
			trs.separate();
		}catch(Exception e){
			e.printStackTrace();
		}

	}

}
