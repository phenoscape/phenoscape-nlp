package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

public class PermittedRelations {	
	/**
	 * match the relation in PATO relation slim
	 * @param fromstructure name
	 * @param relation string
	 * @param tostructure name, not the chain of names
	 * @return if match, return Quality with id and label, if not, return null
	 */
	public static QualityProposals matchInPermittedRelation(String relation, boolean negation) {
		//TODO: handle negated relations
		QualityProposals qproposals = new QualityProposals();
		Quality relationalquality = new Quality();
		String relationcopy = relation;
		relation = Utilities.removeprepositions(relation);
		//Adding the below adjective forms to address issue => separate => separated, so it matches ontology
		LinkedHashSet<String> relationForms = Wordforms.toAdjective(relation);
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
		String relation_ID=null;
		//checks if the given relation is present in the identified relationalqualities - Hariharan
		for(String rel:relationForms)
		{
			if(Dictionary.relationalqualities.containsKey(rel))
		{
			relationalquality.setString(rel);
			relationalquality.setId(retrieve_id(rel));
			relationalquality.setLabel(retrieve_label(rel));
			relationalquality.setConfidenceScore((float)1.0);
			qproposals.add(relationalquality);
			return qproposals;
		}
		}
		//TODO: Check whether something is present in PATO before converting to various forms = Hariharan
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
			
			relation_ID=getbestrelation(forms,relation);
			if(relation_ID!=null)
			{
				relationalquality.setString(relation);
				relationalquality.setId(relation_ID.split("\\|\\|")[0]);
				relationalquality.setLabel(retrieve_label(relation_ID.split("\\|\\|")[1]));
				relationalquality.setConfidenceScore((float)1.0);
				qproposals.add(relationalquality);
				return qproposals;
			}
		}
	}
		return null;
	}



	// decides the best equivalent relation from different identified relations using some semantic measures
	private static String getbestrelation(Hashtable<String, Integer> forms,String  relation) {
		// TODO 
		//update this later to find the closest similar relation
		//int probability =0,maxprobability=0;
		Set<String> keys;
		//System.out.println("forms size" +forms.size());
		keys = forms.keySet();
		for(String form:keys)
			if(Dictionary.relationalqualities.containsKey(form))
				return (retrieve_id(form)+"||"+form);
		
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
	
	public static String retrieve_id(String relation)
	{
		Hashtable<String,String> all_possible_relations = Dictionary.relationalqualities.get(relation);
		Set<String> Keyset= all_possible_relations.keySet();
		for(String key:Keyset)
		{
			//code to choose the best from the list of relational qualities
			return all_possible_relations.get(key);
		}
		return null;
	}
	
	public static String retrieve_label(String relation)
	{
		Hashtable<String,String> all_possible_relations = Dictionary.relationalqualities.get(relation);
		Set<String> Keyset= all_possible_relations.keySet();
		for(String key:Keyset)
		{
			//code to choose the best from the list of relational qualities
			return key;
		}
		return null;
	}
	
}
