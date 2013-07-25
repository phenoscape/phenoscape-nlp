/**
 * 
 */
package outputter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

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
	private static final Logger LOGGER = Logger.getLogger(EntitySearcher1.class);   
	private static boolean debug_permutation = false;
	//boolean debug = true;

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
	public ArrayList<EntityProposals> searchEntity(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		LOGGER.debug("EntitySearcher1: search '"+entityphrase+"[orig="+originalentityphrase+"]'");

		ArrayList<EntityProposals> entities = new ArrayList<EntityProposals>();
		EntityProposals ep = new EntityProposals(); //search results
		//entityphrase =  "posterior radials";
		//elocatorphrase = "anterior dorsal fin";
		//save phrases as components
		EntityComponents ecs = new EntityComponents(entityphrase, elocatorphrase);
		ArrayList<EntityComponent> components = ecs.getComponents(); //each component is an entity or an entity locator

		//construct pre-composed variations: selected permutations without repetition 
		ArrayList<String> variations  = new ArrayList<String>();
		//permutation on synrings that are results of subcomponents permutation
		permutation(components, variations); 
		LOGGER.debug(System.getProperty("line.separator")+"search variations...");
		LOGGER.debug("'"+entityphrase+" , "+elocatorphrase+"' generated "+variations.size()+" variations:");
		for(String variation : variations)
			LOGGER.debug(".."+variation);

		//search variations for pre-composed terms one by one, return all the results
		boolean found = false;
		LOGGER.debug("search variations one by one...");
		for(String variation: variations){
			//ArrayList<FormalConcept> entityfcs = new TermSearcher().regexpSearchTerm(variation, "entity"); //remove indexes from variation before search
			ArrayList<FormalConcept> entityfcs = new TermSearcher().searchTerm(variation, "entity"); //remove indexes from variation before search
			//check for the strength of the match: related synonyms: (?:(?:crista) (?:parotica)) entity=>tegmen tympani
			if(entityfcs!=null){
				for(FormalConcept entity:entityfcs){
					if(entity!=null){
						found = true;
						ep.setPhrase(entityphrase);
						ep.add((Entity)entity); //all variations are alternative entities (i.e. proposals) for the phrase
					}
				}
				LOGGER.debug("search variation '"+variation+"' found match: "+ep.toString());
			}
		}
		if(found){
			//entities.add(ep);
			Utilities.addEntityProposals(entities, ep);
			LOGGER.debug("EntitySearcher1 found matched variations, returns entity proposals:");
			for(EntityProposals aep: entities){
				LOGGER.debug("..EntityProposals:"+aep.toString());
			}
			return entities;
		}

		//failed to find pre-composed terms, try to post-compose using part_of
		//call on EntityEntityLocatorStrategy on expressions without spatial terms:
		//(the attachment of spatial terms to parent entity is different from attachement of a child entity to parent entity)
		//like EntitySearch2, but more flexible: may call the strategy on different entity/entity locator combinations
		//TODO: need more work: what's entityphrase and elocatorphrase?
		boolean hasspatial = ecs.containsSpatial();
		//if(elocatorphrase.trim().length()>0 && !hasspatial){//call EELS strategy when there is an entity locator to avoid infinite loop. 
		if(!hasspatial){//call EELS strategy when there is an entity locator to avoid infinite loop. 
			//ep.setPhrase(entityphrase);
			LOGGER.debug(System.getProperty("line.separator")+"EntitySearcher1 calls EntityEntityLocatorStrategy");

			if(components.size()==1){
				LOGGER.debug("find components size = 1");
				//has one component only, split the component into entity and entitylocator
				ArrayList<String> perms = components.get(0).getPermutations();
				for(String perm : perms){
					if(perm.indexOf(" of ")<0) continue; //there must be another variation with " of " that is equivalent to this variation
					if(this.debug_permutation) System.err.println("variation to split: "+perm);
					String[] parts = perm.split("\\s+of\\s+");
					for(int l = 0; l < parts.length-1; l++){ //length of entity
						entityphrase = Utilities.join(parts, 0, l, " of ");
						elocatorphrase =  Utilities.join(parts, l+1, parts.length-1, " of ");
						LOGGER.debug("..EEL search: entity '"+entityphrase+"' and locator '"+elocatorphrase+"'");
						EntityEntityLocatorStrategy eels = new EntityEntityLocatorStrategy(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
						eels.handle();
						ArrayList<EntityProposals> entity = eels.getEntities(); //a list of different entities: both sexes => female and male
						if(entity != null){
							found = true;
							//ep.add(entity);
							//entities.add(ep);
							//entities.addAll(entity);						
							for(EntityProposals aep: entity){
								Utilities.addEntityProposals(entities, aep);
								LOGGER.debug("..EEL adds proposals:"+aep);
							}
						}else{
							LOGGER.debug("..EEL found no match");
						}
					}
				}
			}else{
				LOGGER.debug("find components size > 1");
				//has multiple components
				//use the first n as entity
				for(int n = 1; n < components.size()-1; n++){
					for(int i = 0; i < n; i++){ //
						ArrayList<String> perms = components.get(i).getPermutations();
						String vars = "";
						for(String perm : perms){
							vars += perm+"|";
						}
						vars = vars.replaceFirst("\\|$", "");
						entityphrase +="(?:"+vars+") of ";
					}
					entityphrase = entityphrase.replaceFirst(" of $", "");
					//use the rest as entity locators
					for(int i = n; i < components.size(); i++){ //
						ArrayList<String> perms = components.get(i).getPermutations();
						String vars = "";
						for(String perm : perms){
							vars += perm+"|";
						}
						vars = vars.replaceFirst("\\|$", "");
						elocatorphrase +="(?:"+vars+") of ";
					}
					elocatorphrase = elocatorphrase.replaceFirst(" of $", "");

					LOGGER.debug("..EEL search: entity '"+entityphrase+"' and locator '"+elocatorphrase+"'");
					EntityEntityLocatorStrategy eels = new EntityEntityLocatorStrategy(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
					eels.handle();
					ArrayList<EntityProposals> entity = eels.getEntities(); //a list of different entities: both sexes => female and male
					if(entity != null){
						found = true;
						//ep.add(entity);
						//entities.add(ep);
						//entities.addAll(entity);						
						for(EntityProposals aep: entity){
							Utilities.addEntityProposals(entities, aep);
							LOGGER.debug("..EEL adds proposals:"+aep);
						}
					}else{
						LOGGER.debug("..EEL found no match");
					}
				}
			}
		}
		//if(found) return entities;

		//deal with spatial expressions
		if(hasspatial){
			LOGGER.debug(System.getProperty("line.separator")+"EntitySearcher1 calls SpatialModifiedEntityStrategy");
			//i.e. EntitySearch4, but more flexible: may call the strategy on different entity/entity locator combinations
			//TODO: need more work: what's entityphrase and elocatorphrase?
			SpatialModifiedEntityStrategy smes = new SpatialModifiedEntityStrategy(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
			smes.handle();
			ArrayList<EntityProposals> entity = smes.getEntities();
			if(entity != null){
				found = true;

				//ep.add(entity);
				//entities.add(ep);
				//entities.addAll(entity); //add a list of different entities: both sexes => female and male
				for(EntityProposals aep: entity){
					LOGGER.debug("..SME adds proposals: "+aep.toString());
					Utilities.addEntityProposals(entities, aep);
				}
			}else{
				LOGGER.debug("SME found no match");
			}
		}
		//if(found) return entities;
		LOGGER.debug(System.getProperty("line.separator")+"EntitySearcher1 calls EntitySearcher4");
		ArrayList<EntityProposals> entity = new EntitySearcher4().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
		//proximal tarsal element:
		//SpaticalModifiedEntity: phrase=proximal region entity=proximal region score=1.0 and (part_of some phrase=tarsal\b.* entity=tarsal bone score=0.5) 
		//EntitySearcher5: phrase=proximal tarsal\b.* entity=proximal tarsal bone score=0.5
		//TODO: save both or select one? 
		if(entity!=null){
			//entities.addAll(entity);
			for(EntityProposals aep: entity){
				LOGGER.debug("..ES4 adds proposals: "+aep.toString());
				Utilities.addEntityProposals(entities, aep);
			}
		}else{
			LOGGER.debug("..ES4 found no match");
		}


		LOGGER.debug(System.getProperty("line.separator")+"EntitySearcher1 completed search for '"+entityphrase+"[orig="+originalentityphrase+"]' and returns:");
		for(EntityProposals aep: entities){
			LOGGER.debug("..EntityProposals:"+aep.toString());
		}
		return entities;
		//return new EntitySearcher5().searchEntity(root, structid, entityphrase, elocatorphrase, originalentityphrase, prep);
	}



	/**
	 * 'posterior radials,anterior dorsal fin' generated 2 variations:
		..(?:(?:posterior|posterior side) (?:radials)) of (?:(?:fin) of (?:anterior|anterior side) (?:dorsal|dorsal side)|(?:anterior|anterior side) (?:dorsal|dorsal side) (?:fin))
		..(?:(?:fin) of (?:anterior|anterior side) (?:dorsal|dorsal side)|(?:anterior|anterior side) (?:dorsal|dorsal side) (?:fin)) (?:(?:posterior|posterior side) (?:radials))

	 * 'ventral radial crest,' generated 1 variations
	 *..(?:(?:process|crest|ridge|tentacule|shelf|flange|ramus) of (?:ventral|ventral side) (?:radial|radius)|(?:ventral|ventral side) (?:radial|radius) (?:process|crest|ridge|tentacule|shelf|flange|ramus))
	 * 
	 * 'posterior postfrontal,'  generated 1 variations:
	 * 1. (?:(?:posterior|posterior side) (?:postfrontal))
	 * @param components
	 * @param variations
	 */
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
				if(debug_permutation) System.err.println("variation: "+prefix+"("+lastindex+")");
			}
		}
		else {
			for (int i = 0; i < n; i++){
				ArrayList<EntityComponent> reducedcomps = new ArrayList<EntityComponent>();
				for(int j = 0; j < n; j++){
					if(j!=i) reducedcomps.add(components.get(j)); //reducedcomps = components - element_i
				}
				if(debug_permutation) System.err.println("prefix="+prefix+" new round: i="+i+ " components size="+reducedcomps.size());
				String newprefix = newPrefix(prefix, lastindex, clone, clone.indexOf(components.get(i)), i, components);
				if(debug_permutation) System.err.println("newprefix="+newprefix+" new round: i="+i+ " components size="+reducedcomps.size());
				permutation(/*(prefix+" "+components.get(i).getSynRing()).trim()*/newprefix, reducedcomps, variations, clone, clone.indexOf(components.get(i)));
			}
		}
	}

	/**
	 * decide whether to concatenate oldprefix and components.get(i).getSynRing() directly or to add " of " between them.
	 * add "of" after a structure when the lastindex < newindex (i.e., putting a child before a parent organ)
	 * @param oldprefix: the current prefix 
	 * @param lastindex: index of the last component in the original components(clone) that was added to prefix. 
	 * @param newindex:  index of the component i in the original clone
	 * @param i: index of the to-be-added component in current components
	 * @param components
	 * @return
	 */

	private static String newPrefix(String oldprefix, int lastindex, ArrayList<EntityComponent> clone, int newindex, int i,
			ArrayList<EntityComponent> components) {
		//if(lastindex>=0 && lastindex<components.size() && clone.get(lastindex).isStructure() && lastindex < newindex){
		if(lastindex>=0 && clone.get(lastindex).isStructure() && lastindex < newindex){
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
			//1. join entityphrase and elocatorphrase, then split them into entity components, sorted from child to parent organ
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
			//ArrayList<String> entityphrases = sort(entityphrase.split("\\s*(,| of )\\s*"));//sort: turn 'ventral radial process' to 'process, ventral radial'
			String spatialphraseptn = "(?:"+Dictionary.singlewordspatialtermptn +")?\\s*"
					+ "\\b(?:(?:"+Dictionary.allSpatialHeadNouns()+")\\b|\\b(?:"+TermOutputerUtilities.adjectiveorganptn+"))";
			String[] entityphrases = entityphrase.split("\\s*(,| of )\\s*");//sort: turn 'ventral radial process' to 'process, ventral radial'
			for(String phrase: entityphrases){
				phrase = phrase.trim();
				if(phrase.length()==0) continue;
				String phrasecp = phrase;
				//Pattern p = Pattern.compile("(.*?)\\b("+Dictionary.spatialtermptn+"|"+TermOutputerUtilities.adjectiveorganptn+")\\b(.*)"); //this splits on single-word spatial term also
				Pattern p = Pattern.compile("(.*?)\\b("+spatialphraseptn+")\\b(.*)");
				Matcher m = p.matcher(phrase);
				String temp = "";
				while(m.matches()){
					//temp += m.group(1)+"#"+m.group(2)+"#";
					temp += m.group(1)+m.group(2)+"#";
					phrase = m.group(3);
					m = p.matcher(phrase);
				}
				temp +=phrase.trim();//appending the original string to the tokens separated by #
				if(debug_permutation) System.err.println("split&join: '"+phrasecp+"' =>'"+temp+"'");
				String[] temps = temp.split("\\s*#\\s*");
				ArrayList<EntityComponent> thiscomponents = new ArrayList<EntityComponent>();
				//for(String part: temps){
				for(int i = temps.length-1; i>=0; i--){
					String part = temps[i];
					part = part.trim();
					if(part.length()>0){
						//parts.add(t);
						EntityComponent ec = new EntityComponent(part);
						ec.setSynRing(this.getSynRing4Phrase(part));
						if(part.indexOf(" ")<0 && part.matches(Dictionary.singlewordspatialtermptn)){
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
		 * sort organs so parent organs come later
		 * turn 'ventral radial process' to 'process, ventral radial'
		 * turn 'radial ventral region' to 'ventral region, radial'
		 * @param phrases: phrases without comma or 'of'
		 * @return sorted list of strings
		 */
		/*private ArrayList<String> sort(String[] phrases){
			ArrayList<String> sorted = new ArrayList<String>();
			for(String phrase: phrases){
				Pattern p = Pattern.compile("(.*?)\\b("+Dictionary.spatialtermptn+"|"+TermOutputerUtilities.adjectiveorganptn+")\\b(.*)");
				Matcher m = p.matcher(phrase);
				String temp = "";
				while(m.matches()){
					temp += m.group(1)+"#"+m.group(2)+"#";
					phrase = m.group(3);
					m = p.matcher(phrase);
				}
				temp +=phrase.trim();//appending the original string to the tokens separated by #
				String[] temps = temp.split("\\s*#\\s*");
				for(int)
			}
			return sorted;
		}*/

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
		 * @param phrase: (?:(?:shoulder) (?:girdle)) or dorsal fin
		 * @return (?:dorsal|dorsal side) (?:fin)
		 */
		private String getSynRing4Phrase(String phrase){
			String synring = "";
			phrase = phrase.replaceAll("(\\(\\?:|\\))", ""); //(?:(?:shoulder) (?:girdle)) =>shoulder girdle
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
		//Posterior radials in posterior-dorsal-fin
		EntitySearcher1 eso = new EntitySearcher1();
		String src = "C:/Users/updates/CharaParserTest/EQ-swartz_FixedGloss/target/final/Swartz 2012.xml_states1004.xml";
		SAXBuilder builder = new SAXBuilder();
		Document xml = null;
		try {
			xml = builder.build(new File(src));
		} catch (JDOMException e) {
			LOGGER.error("", e);
		} catch (IOException e) {
			LOGGER.error("", e);
		}
		if(xml!=null){
			Element root = xml.getRootElement();
			String structid ="o5";
			//String entityphrase = "posterior postfrontal";
			//String entityphrase ="heterocercal";
			//String elocatorphrase = "";
			//String entityphrase = "posterior supraorbital postfrontal";
			//String entityphrase ="posterior radials";
			//String elocatorphrase = "anterior dorsal fin";
			String entityphrase = "posterior radial";
			String elocatorphrase = "posterior dorsal fin";
			String prep = "";
			ArrayList<EntityProposals> eps = eso.searchEntity(root, structid,  entityphrase, elocatorphrase, entityphrase, prep);
			System.out.println("result:");
			for(EntityProposals ep: eps)
				System.out.println(ep.toString());
		}
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
