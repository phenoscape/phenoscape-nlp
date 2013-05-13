package outputter;

import java.util.Hashtable;
import java.util.Set;

public class PermittedRelations {	
	/**
	 * match the relation to the restricted relation list
	 * @param fromstructure name
	 * @param relation string
	 * @param tostructure name, not the chain of names
	 * @return if match, return Quality with id and label, if not, return null
	 */
	public static Quality matchInPermittedRelation(String relation, boolean negation) {
		//TODO: handle negated relations
		
		Quality relationalquality = new Quality();
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
		if(Dictionary.relationalqualities.containsKey(relation))
		{
			relationalquality.setString(relation);
			relationalquality.setId(Dictionary.relationalqualities.get(relation));
			relationalquality.setLabel(Dictionary.resrelationQ.get(Dictionary.relationalqualities.get(relation)));
			relationalquality.setConfidenceScore((float)1.0);
			return relationalquality;
		}
		//if failed in above steps then it uses wordnet to find the different synonyms of the relation string
		Hashtable<String, Integer> forms = getdifferentrelationalforms(relation);
		//of the identified relations, it finds the best equivalent relation else it returns null
		if(forms!=null)
		{
			
			relation_ID=getbestrelation(forms,relation);
			if(relation_ID!=null)
			{
				relationalquality.setString(relation);
				relationalquality.setId(relation_ID);
				relationalquality.setLabel(Dictionary.resrelationQ.get((relation_ID)));
				relationalquality.setConfidenceScore((float)1.0);
				return relationalquality;
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
				return Dictionary.relationalqualities.get(form);
		
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
	
	
	
}