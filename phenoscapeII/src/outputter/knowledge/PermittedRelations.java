package outputter.knowledge;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import outputter.Utilities;
import outputter.XML2EQ;
import outputter.data.Quality;
import outputter.data.QualityProposals;

public class PermittedRelations {	
	private static final Logger LOGGER = Logger.getLogger(PermittedRelations.class);   
	static Hashtable<String, QualityProposals> cache = new Hashtable<String, QualityProposals>();
	/**
	 * match the relation in PATO relation slim
	 * @param relation "name of the quality string"
	 * @param negation "whether it is a negated relation or not"
	 * @param flag  flag==1, search for relation in relationalqualities else in restricted relational list
	 * @
	 * @return if match, return Quality with id and label, if not, return null
	 *
	 */
	public static QualityProposals matchInPermittedRelation(String relation, boolean negation,int flag) {
		QualityProposals qp = cache.get(relation+negation+flag);
		if(qp!=null) return qp;
		//TODO: handle negated relations
		QualityProposals qproposals = new QualityProposals();
		qproposals.setPhrase(relation);
		Quality relationalquality = new Quality();
		relation = Utilities.removeprepositions(relation);
		//Adding the below adjective forms to address issue => separate => separated, so it matches ontology
		LinkedHashSet<String> relationForms = Wordforms.toAdjective(relation);

		Hashtable<String,Hashtable<String, String>> qualityholder;
		Hashtable<String,Hashtable<String, String>> qualityholderslim;
		Hashtable<String,Hashtable<String, String>> qualityholderrestrict;
		qualityholderslim = Dictionary.relationalqualities;
		qualityholderrestrict = Dictionary.restrictedrelations;
		

		String[] relation_ID=null;
		//checks if the given relation is present in the identified relationalqualities - Hariharan
		for(String rel:relationForms)
		{
			
			if(flag==0){
				qualityholder = qualityholderrestrict;
			}else{
				qualityholder = qualityholderrestrict.containsKey(rel)? qualityholderrestrict : qualityholderslim;
			}
			
			if(qualityholder.containsKey(rel))
			{
				relationalquality.setString(rel);
				relationalquality.setId(retrieve_id(rel,qualityholder));
				relationalquality.setLabel(retrieve_label(rel,qualityholder));
				relationalquality.setConfidenceScore((float)1.0);
				qproposals.add(relationalquality);
				cache.put(relation+negation+flag, qproposals);
				return qproposals;
			}
		}
		//Check whether something is present in PATO before converting to various forms = Hariharan
		//Example:broad has an exact match in PATO, but finding synonyms we get tolerant to be a synonym which is in relational quality 

		ArrayList<Hashtable<String, String>> results = new ArrayList<Hashtable<String, String>>();
		XML2EQ.ontoutil.searchOntologies(relation, "quality", results);
		if(results.size()==0)
		{
			//if failed in above steps then it uses wordnet to find the different synonyms of the relation string
			Hashtable<String, Integer> forms = getdifferentrelationalforms(relation);
			//of the identified relations, it finds the best equivalent relation else it returns null
			if((forms.size()!=0))
			{

				relation_ID=getbestrelation(forms,relation,flag);
				if(relation_ID!=null)
				{
					relationalquality.setString(relation);
					relationalquality.setId(relation_ID[0]);
					relationalquality.setLabel(relation_ID[1]);
					relationalquality.setConfidenceScore((float)1.0);
					qproposals.add(relationalquality);
					cache.put(relation+negation+flag, qproposals);
					return qproposals;
				}
			}
		}
		return null;
	}



	// decides the best equivalent relation from different identified relations using some semantic measures
	private static String[] getbestrelation(Hashtable<String, Integer> forms,String  relation, int flag) {
		// TODO 
		//update this later to find the closest similar relation
		//int probability =0,maxprobability=0;

		Hashtable<String,Hashtable<String, String>> qualityholder;
		Hashtable<String,Hashtable<String, String>> qualityholderslim;
		Hashtable<String,Hashtable<String, String>> qualityholderrestrict;
		qualityholderslim = Dictionary.relationalqualities;
		qualityholderrestrict = Dictionary.restrictedrelations;
		Set<String> keys;
		//System.out.println("forms size" +forms.size());
		keys = forms.keySet();
		for(String form:keys){
			if(flag==0){
				qualityholder = qualityholderrestrict;
			}else{
				qualityholder = qualityholderrestrict.containsKey(form)? qualityholderrestrict : qualityholderslim;
			}
			if(qualityholder.containsKey(form)){
				String[] result = new String[2];
				result[0] = retrieve_id(form,qualityholder);
				result[1] = form;
  				return result;
			}
		}
		
		return null;
	}

	// returns the different relational forms of the current passed string
	private static Hashtable<String,Integer> getdifferentrelationalforms(String relation) {
		// TODO Auto-generated method stub
		//Top Synonyms
		Hashtable<String, Integer> synonyms;
		synonyms= WordNet.getallsynonyms(relation);
		
		//System.out.println("size == "+synonyms.size());
		return synonyms;
	}
	
	public static String retrieve_id(String relation, Hashtable<String, Hashtable<String, String>> qualityholder)
	{
		Hashtable<String,String> all_possible_relations = qualityholder.get(relation);
		Set<String> Keyset= all_possible_relations.keySet();
		for(String key:Keyset)
		{
			//TODO: add code to choose the best from the list of relational qualities
			return all_possible_relations.get(key);
		}
		return null;
	}
	
	public static String retrieve_label(String relation, Hashtable<String, Hashtable<String, String>> qualityholder)
	{
		Hashtable<String,String> all_possible_relations = qualityholder.get(relation);
		Set<String> Keyset= all_possible_relations.keySet();
		for(String key:Keyset)
		{
			//TODO: add code to choose the best from the list of relational qualities
			return key;
		}
		return null;
	}
	
	/*
	 * Changed by Zilong: deal with relationship such as connect, contact, interconnect etc.
	 * Transform the result from CharaParser which is of the form:
	 * connection[E] between A[EL] and B[EL] <some text>[Q] -the quality could be misidentified
	 * to the form:
	 * A[E] is in connection with[Q] B[QM]
	 * 
	 * */
	//if(entity.toLowerCase().trim().matches("("+Dictionary.contact+")")){
	//	EQ.put("entity", entitylocator.split(",")[0]);//the first EL as E
	//	EQ.put("quality", "in contact with"); //"in contact with" can be found in ontos
	//	EQ.put("qualitymodifier", entitylocator.replaceFirst("[^,]*,?", "").trim());//the rest of EL is QM
	//	EQ.put("entitylocator", "");//empty the EL
	//}
	/*End handling the "contact" type relation*/
	
}
