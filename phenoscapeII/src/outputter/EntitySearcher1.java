/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;

/**
 * @author updates
 * it searches different variations of the E/EL compositions using all the elements.
 * 
 * For examples: 
 * input: e:postaxial process, el:modifier fibula
 * generate variations like:
 * 1. (postaxial|syn_ring) (process|crest|syn_ring) of modifier (fibula|fibular|adj)
 * 2. modifier (fibula|fibular|adj) (postaxial|syn_ring) (process|crest|syn_ring) 
 * 3. (postaxial|syn_ring) modifier (fibula|fibular|adj) (process|crest|syn_ring)
 * 4. modifier (postaxial|syn_ring) (fibula|fibular|adj) (process|crest|syn_ring)
 * 
 */
public class EntitySearcher1 extends EntitySearcher {

	/**
	 * 
	 */
	public EntitySearcher1() {

	}
    //TODO patterns s0fd16381: maxillae, anterior end of 
	/* (non-Javadoc)
	 * @see outputter.EntitySearcher#searchEntity(org.jdom.Element, java.lang.String, java.lang.String, java.lang.String, java.lang.String, java.lang.String, int)
	 */
	@Override
	public EntityProposals searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {

		EntityProposals entities = new EntityProposals(); //search results

		//save phrases as components
		EntityComponents ecs = new EntityComponents(entityphrase, elocatorphrase);
		ArrayList<EntityComponent> components = ecs.getComponents(); //each component is an entity/entity locator

		//construct variations: selected permutation without repetition 
		ArrayList<String> variations  = new ArrayList<String>();
		permutation(components, variations); 

		//search variations for pre-composed terms one by one, return all the results
		boolean found = false;
		for(String variation: variations){
			ArrayList<FormalConcept> entityfcs = new TermSearcher().regexpSearchTerm(variation, "entity"); //remove indexes from variation before search
			//check for the strength of the match: related synonyms: (?:(?:crista) (?:parotica)) entity=>tegmen tympani
			if(entityfcs!=null){
				for(FormalConcept entity:entityfcs){
					if(entity!=null){
						found = true;
						entities.setPhrase(entityphrase);
						entities.add((Entity)entity);
					}
				}
			}
		}
		if(found) return entities;

		//failed to find pre-composed terms, try to post-compose using part_of
		//call on EntityEntityLocatorStrategy on expressions with or without spatial terms as they may all exist as precomposed entities
		//i.e. EntitySearch2, but more flexible: may call the strategy on different entity/entity locator combinations
		//TODO: need more work: what's entityphrase and elocatorphrase?
		EntityEntityLocatorStrategy eels = new EntityEntityLocatorStrategy(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
		eels.handle();
		EntityProposals entity = eels.getEntities();
		if(entity != null){
			found = true;
			entities.setPhrase(entityphrase);
			entities.add(entity);
		}
		
		if(found) return entities;

		//deal with spatial expressions
		if(ecs.containsSpatial()){
			//i.e. EntitySearch4, but more flexible: may call the strategy on different entity/entity locator combinations
			//TODO: need more work: what's entityphrase and elocatorphrase?
			SpatialModifiedEntityStrategy smes = new SpatialModifiedEntityStrategy(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
			smes.handle();
			entity = smes.getEntities();
			if(entity != null){
				found = true;
				entities.add(entity);
			}
		}
		if(found) return entities;



		return new EntitySearcher5().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
	}



	public  static void permutation(ArrayList<EntityComponent> components, ArrayList<String> variations) { 
		//System.out.println("round 0: i=-1 "+ "components size="+components.size()+" prefix=''");
		permutation("", components, variations, clone(components), -1); 
		//remove indexes 
		for(int i = 0; i < variations.size(); i++){
			variations.set(i, variations.get(i).replaceAll("\\(-?\\d+\\)", "").trim());
		}		
	}



	private static void permutation(String prefix, ArrayList<EntityComponent> components, ArrayList<String> variations,  ArrayList<EntityComponent> clone, int lastindex) {
		int n = components.size();
		if (n == 0){
			if(!clone.get(lastindex).isSpatial()){ //the last component can not be a spatial term
				variations.add(prefix+"("+lastindex+")");
				//System.out.println();
				//System.out.println("variation: "+prefix+"("+lastindex+")");
			}
		}
		else {
			for (int i = 0; i < n; i++){
				ArrayList<EntityComponent> reducedcomps = new ArrayList<EntityComponent>();
				for(int j = 0; j < n; j++){
					if(j!=i) reducedcomps.add(components.get(j)); //reducedcomps = components - element_i
				}

				String newprefix = newPrefix(prefix, lastindex, clone.indexOf(components.get(i)), i, components);
				//System.out.println("new round: i="+i+ " components size="+reducedcomps.size()+" prefix="+(prefix+" "+components.get(i).getSynRing()).trim());
				//System.out.println("new round: i="+i+ " components size="+reducedcomps.size()+" prefix="+prefix);
				permutation(/*(prefix+" "+components.get(i).getSynRing()).trim()*/newprefix, reducedcomps, variations, clone, clone.indexOf(components.get(i)));
			}
		}
	}

	/**
	 * decide whether to concatenate oldprefix and components.get(i).getSynRing() directly or to add " of " between them.
	 * @param prefix
	 * @param lastindex: index of the last component in the original components(clone) that was added to prefix. 
	 * @param i: index of the to-be-added component in components
	 * @param components
	 * @return
	 */

	private static String newPrefix(String oldprefix, int lastindex, int newindex, int i,
			ArrayList<EntityComponent> components) {
		if(lastindex>=0 && components.get(i).isStructure() && lastindex < newindex){
			return (oldprefix+"("+lastindex+") of "+components.get(i).getSynRing()).trim();
		}
		return (oldprefix+"("+lastindex+") "+components.get(i).getSynRing()).trim();
	}

	private static ArrayList<EntityComponent> clone(
			ArrayList<EntityComponent> components) {
		ArrayList<EntityComponent> clone = new ArrayList<EntityComponent>();
		for(int i = 0 ; i < components.size(); i++){
			clone.add(components.get(i));
		}
		return clone;
	}


	/**
	 * private class
	 * @author Hong Cui
	 *
	 */
	private class EntityComponents{
		ArrayList<EntityComponent> components = new ArrayList<EntityComponent>(); //the order of the elements indicate the part of relation, 0 part of 1 part of 2 ...

		public EntityComponents(String entity, String locator){
			//1. join entityphrase and elocatorphrase, then split them into entity components
			components = joinAndSplit(entity, locator);

			//2. create syn_ring for each component
			setSynRings(components);
		}

		/**
		 * turn entityphrase + elocatorphrases to a list of EntityComponents, each EntityComponent represents one structure, e.g. 'dorsal', 'fin', 'dorsal region', 'long tooth'
		 * @param entityphrase: entities separated by ',', later entities are parent organs of the earlier ones
		 * @param elocatorphrase: entity locators separated by ',', later entities are parent organs of the earlier ones
		 * @return
		 */
		private ArrayList<EntityComponent> joinAndSplit(String entityphrase,
				String elocatorphrase) {
			ArrayList<EntityComponent> components = new ArrayList<EntityComponent>();
			entityphrase = entityphrase+","+elocatorphrase; //join

			//split: separate adjective organs ('nasal') and modified organ ('bone');
			//separate spatial term  ('dorsal') and modified organ ('fin'), but keep "dorsal margin" as one part;
			//split on " of ".
			String[] entityphrases = entityphrase.split("\\s*(,| of )\\s*");
			for(String phrase: entityphrases){
				phrase = phrase.trim();
				String phrasecp = phrase;
				Pattern p = Pattern.compile("(.*?)\\b("+Dictionary.spatialtermptn+"|"+TermOutputerUtilities.adjectiveorganptn+")\\b(.*)");
				Matcher m = p.matcher(phrase);
				String temp = "";
				while(m.matches()){
					temp += m.group(1)+"#"+m.group(2)+"#";
					phrase = m.group(3);
					m = p.matcher(phrase);
				}
				temp +=phrase;//appending the original string to the tokens separated by #
				String[] temps = temp.split("\\s*#\\s*");
				ArrayList<EntityComponent> thiscomponents = new ArrayList<EntityComponent>();
				for(String part: temps){
					part = part.trim();
					if(part.length()>0){
						//parts.add(t);
						EntityComponent ec = new EntityComponent(part);
						ec.setSynRing(this.getSynRing4Phrase(part));
						if(part.indexOf(" ")<0 && part.matches(Dictionary.spatialtermptn)){
							ec.isSpatial(true);
							ec.isStructure(false);
						}
						else{
							ec.isStructure(true);
							ec.isSpatial(false);
						}
						thiscomponents.add(ec);
					}
				}

				//permute parts in the phrase
				ArrayList<String> permutations = new ArrayList<String>();
				EntitySearcher1.permutation(thiscomponents, permutations); 

				//save EntityComponent
				EntityComponent ec = new EntityComponent(phrasecp);
				ec.isStructure(true); //each phrase representing a structure
				ec.setPermutations(permutations);
				components.add(ec);
			}
			return components;
		}

		/**
		 * Set syn ring for each compoent. Treat syn rings for different permutations the alternatives in the syn ring
		 * @param components
		 */
		private void setSynRings(ArrayList<EntityComponent> components) {
			for(EntityComponent component: components){
				String synring = "";
				ArrayList<String> permus = component.getPermutations();
				for(String permu : permus){ //A B; B A
					//synring += getSynRing4Phrase(permu)+"|"; //(A|A1|A2) (B|B1)|(B|B1) (A|A1|A2)
					synring += permu+"|"; //(A|A1|A2) (B|B1)|(B|B1) (A|A1|A2)
				}
				component.setSynRing("(?:"+synring.replaceFirst("\\|$", "").trim()+")");
			}
		}

		/**
		 * dorsal fin
		 * @param phrase
		 * @return (dorsal|dorsal side) (fin)
		 */
		private String getSynRing4Phrase(String phrase){
			String synring = "";
			String[] tokens = phrase.split("\\s+");
			//may use a more sophisticated approach to construct ngrams: A B C => A B C;A (B C); (A B) C;
			for(int i = 0; i < tokens.length; i++){
				if(tokens[i].matches(Dictionary.spatialtermptn)) synring += "(?:"+SynRingVariation.getSynRing4Spatial(tokens[i])+")"+" ";
				else synring += "(?:"+SynRingVariation.getSynRing4Structure(tokens[i])+")"+" ";
			}
			return synring;
		}
		public  ArrayList<EntityComponent> getComponents(){
			return this.components;
		}

		public EntityComponent getComponent(int index){
			return this.components.get(index);
		}

		public int indexOf(EntityComponent c){
			return this.components.indexOf(c);
		}

		/**
		 * whether this set of entitycomponents contain a spatial term
		 * @return
		 */
		public boolean containsSpatial(){
			//[dorsal radials, posterior dorsal fin] => true
			//[anterior process, maxilla] => true
			for(EntityComponent cp: components){
				if(cp.getPhrase().matches(".*?\\b("+Dictionary.spatialtermptn+")\\b.*"))
					return true;
			}
			return false;
		}
	}

	/**
	 * private class
	 * @author Hong Cui
	 *
	 */
	private class EntityComponent{
		String synring; //for the component and is the permutations concatenated as alternatives
		String phrase; //e.g. posterior dorsal fin, or fin
		ArrayList<String> permutations; // permutations of the parts (represented as synrings) in the phrase
		boolean spatial = false;
		boolean structure = false;

		public EntityComponent(String phrase){ this.phrase = phrase;}

		public String getPhrase(){return this.phrase;}
		public void setPermutations(ArrayList<String> permutations) {
			this.permutations = permutations;					
		}

		public ArrayList<String> getPermutations() {
			return this.permutations;				
		}

		/**
		 * 
		 * @param synring
		 */
		public void setSynRing(String synring) {this.synring = synring;}

		/**
		 * used only for one-word spatial terms
		 * @return
		 */
		public void isSpatial(boolean isspatial) {this.spatial = isspatial;}

		/**
		 * used for one-word or n-word phrases
		 * @return
		 */
		public void isStructure(boolean isstructure) {this.structure = isstructure;}

		public String getSynRing(){	return this.synring;}

		/**
		 * used only for one-word spatial terms
		 * @return
		 */
		public boolean isSpatial(){return spatial;} 

		/**
		 * used for one-word or n-word phrases
		 * @return
		 */
		public boolean isStructure(){return structure;}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}


/*if((entityphrase.split("\\s").length>=2)&&(elocatorphrase=="")){
	//try out the variations
	SynRingVariation entityvariation = new SynRingVariation(entityphrase);
	SynRingVariation elocatorvariation = null;
	if(elocatorphrase==null || elocatorphrase.length()==0){
		//elocatorvariation =  new SynRingVariation(elocatorphrase);
	}

	if(elocatorvariation == null){ //try entityvariation alone
		String spatial = entityvariation.getLeadSpaticalTermVariation(); //TODO
		String head = entityvariation.getHeadNounVariation(); //TODO remove duplicates

		// the below code passes all the spatial and entity variations to termsearcher and get all the matching entities.
		ArrayList<FormalConcept> matches = TermSearcher.entityvariationtermsearch(spatial,head);
		if(matches.size()>0)
		{
			EntityProposals entities = new EntityProposals();
			for(int i =0; i <matches.size(); i++){
				entities.add((Entity)matches.get(i));					
			}
			return entities;
		}
	}
}*/
