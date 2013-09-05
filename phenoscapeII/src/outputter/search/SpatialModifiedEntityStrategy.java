/**
 * 
 */
package outputter.search;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Element;

import outputter.Utilities;
import outputter.data.CompositeEntity;
import outputter.data.Entity;
import outputter.data.EntityProposals;
import outputter.data.FormalConcept;
import outputter.data.FormalRelation;
import outputter.data.REntity;
import outputter.data.SimpleEntity;
import outputter.knowledge.Dictionary;

/**
 * @author Hong Cui
 * = EntitySearcher4
 * anterior maxilla => anterior region part of maxilla
 * anterior process of maxilla => process part of anterior region part of maxilla
 * 
 *
 */
public class SpatialModifiedEntityStrategy implements SearchStrategy {
	private static final Logger LOGGER = Logger.getLogger(SpatialModifiedEntityStrategy.class);   
	ArrayList<EntityProposals> entities;
	private String entityphrase;
	private String elocatorphrase;
	private Element root;
	private String structid;
	private String prep;
	private String originalentityphrase;
	
	//search results:
	private ArrayList<EntityProposals> entityls;
	private ArrayList<EntityProposals> sentityps;
	
	private static Hashtable<String, ArrayList<EntityProposals>> cache = new Hashtable<String, ArrayList<EntityProposals>>();
	private static ArrayList<String> nomatchcache = new ArrayList<String>();
	public static Pattern spatialptn = Pattern.compile("^("+Dictionary.spatialtermptn+")\\b\\s*\\b("+Dictionary.allSpatialHeadNouns()+")?\\b");

	/**
	 * [the expression is a query expanded with syn rings, 
	 * for example, '(?:anterior|front) (?:maxilla|maxillary)'] --not yet
	 */
	public SpatialModifiedEntityStrategy(Element root, String structid,
			String entityphrase, String elocatorphrase,
			String originalentityphrase, String prep) {
		this.entityphrase = entityphrase;
		this.elocatorphrase = elocatorphrase;
		this.root = root;
		this.structid = structid;
		this.prep = prep;
		this.originalentityphrase = originalentityphrase;
		LOGGER.debug("SpatialModifiedEntityStrategy: search '"+entityphrase+"[orig="+originalentityphrase+"]'");
	}

	/* (non-Javadoc)
	 * @see outputter.AnnotationStrategy#handle()
	 */
	@Override
	public void handle() {
		//search cache
		if(SpatialModifiedEntityStrategy.nomatchcache.contains(entityphrase+"+"+elocatorphrase)){
			entities = null;
			return;
		}
		if(SpatialModifiedEntityStrategy.cache.get(entityphrase+"+"+elocatorphrase)!=null){
			entities = SpatialModifiedEntityStrategy.cache.get(entityphrase+"+"+elocatorphrase);
			return;
		}
		
		//ArrayList<EntityProposals> entityls = new ArrayList<EntityProposals>();
		//entityl.setString(elocatorphrase);
		if(elocatorphrase.length()>0) {
			//ArrayList<FormalConcept> results = new TermSearcher().searchTerm(elocatorphrase, "entity"); //change this to EntitySearcherOriginal?
			ArrayList<EntityProposals> results = new EntitySearcherOriginal().searchEntity(root, structid, elocatorphrase, "",originalentityphrase, prep);
			if(results!=null){
				LOGGER.debug("SME...searched locator '"+elocatorphrase+"' found match: ");
				for(EntityProposals result: results){
					if(entityls==null) entityls = new ArrayList<EntityProposals>();
					entityls.add(result);
					LOGGER.debug(".." +result.toString());
				}
			}else{
				LOGGER.debug("SME...not match for locator '"+elocatorphrase+"'");
			}
		}

		
		Matcher m = spatialptn.matcher(entityphrase);
		if(m.find()){
			String spatialterm = entityphrase.substring(m.start(), m.end()).trim();
			String newentity = entityphrase.substring(m.end()).trim();
			if(spatialterm.indexOf(" ")<0) spatialterm += " region";
			//take synonyms into account
			spatialterm = synVariation(spatialterm);
			LOGGER.debug("SME...formed spatial term '"+spatialterm+"'");
			//entityphrase='ventral surface'
			//if(entityphrasetokens[0].matches("("+Dictionary.spatialtermptn+")")){
			//String newentity = Utilities.join(entityphrasetokens, 1, entityphrasetokens.length-1, " "); //anything after the spatial term
			//SimpleEntity sentity = (SimpleEntity)new TermSearcher().searchTerm(newentity, "entity");


			//ArrayList<EntityProposals> sentityps = null;
			if(newentity.length()<=0){//e.g. search for 'ventral surface'
				//entityls => sentityps
				if(entityls!=null && entityls.size()>0){
					sentityps = entityls;
					//create a empty entityls
					//entityls = new ArrayList<EntityProposals>();
					entityls = null;
				}
			}else{
				LOGGER.debug("SME...calls EntitySearcherOriginal for newentity '"+newentity/*+","+elocatorphrase*/+"'");
				//sentityps = new EntitySearcherOriginal().searchEntity(root, structid,  newentity, elocatorphrase, originalentityphrase, prep); //advanced search
				sentityps = new EntitySearcherOriginal().searchEntity(root, structid,  newentity, "", originalentityphrase, prep); //advanced search
			}

			LOGGER.debug("SME...now search for spatial term  '"+spatialterm+"'");
			//SimpleEntity sentity1 = (SimpleEntity)new TermSearcher().searchTerm(spatialterm, "entity");
			//ArrayList<FormalConcept> spatialentities = TermSearcher.regexpSearchTerm(spatialterm, "entity");//anterior + region: simple search
			ArrayList<FormalConcept> spatialentities = new TermSearcher().searchTerm(spatialterm, "entity");//anterior + region: simple search
			if(spatialentities!=null) LOGGER.debug("...found match");
			else{
				//create phrase-based spatialentities
				SimpleEntity spatial = new SimpleEntity();
				spatial.setSearchString(spatialterm);
				spatial.setString(spatialterm);
				spatial.setConfidenceScore(1f);
				spatialentities = new ArrayList<FormalConcept>();
				spatialentities.add(spatial);
			}
			boolean found = false;
			EntityProposals centityp = new EntityProposals();
			
			if(sentityps==null){ //standalone spatial term such as 'ventral region'
				//if(spatialentities!=null){
				//TODO consider partial results?
					found = true;
					for(FormalConcept spatialentity: spatialentities){
						SimpleEntity sentity1 = (SimpleEntity) spatialentity;
						if(sentity1!=null){
							centityp.add(sentity1);
						}
					}
				//}
			}else{
				centityp.setPhrase(this.originalentityphrase);
				for(EntityProposals sentityp: sentityps){
					for(Entity sentity: sentityp.getProposals()){
						for(FormalConcept spatialentity: spatialentities){
							SimpleEntity sentity1 = (SimpleEntity) spatialentity;
							if(sentity1!=null){//ventral region
								//nested part_of relation
								if(entityls!=null && entityls.size()>0 || sentity instanceof CompositeEntity){ //anterior process of maxilla 
									//relation & entity locator: inner
									FormalRelation rel = Dictionary.partof;
									rel.setConfidenceScore((float)1.0);
									ArrayList<REntity> rentities = new ArrayList<REntity>();
									REntity re = null;
									//TODO: what if both conditions are true?
									if(entityls!=null && entityls.size()>0){
										for(EntityProposals ep: entityls){
											for(Entity entityl: ep.getProposals()){
												re = new REntity(rel, entityl);
												rentities.add(re);
											}
										}
									}else if(sentity instanceof CompositeEntity){//entitylocator is in sentity
										//sentity should not have any post-composed quality
										re = ((CompositeEntity) sentity).getEntityLocator();
										rentities.add(re);
										sentity = ((CompositeEntity) sentity).getTheSimpleEntity();
									}

									for(REntity rentity: rentities){
										//composite entity = entity locator for sentity
										CompositeEntity centity = new CompositeEntity(); //anterior region^part_of(maxilla)
										centity.addEntity(sentity1); //anterior region
										centity.addEntity(rentity);	//^part_of(maxilla)	
										//relation & entity locator:outer 
										rel = Dictionary.partof;
										rel.setConfidenceScore((float)1.0);
										REntity rentity2 = new REntity(rel, centity);
										centity = new CompositeEntity(); //process^part_of(anterior region^part_of(maxilla))
										centity.addEntity(sentity); //process
										centity.addEntity(rentity2);	//^part_of(anterior region^part_of(maxilla))
										centity.setString(this.originalentityphrase);
										centityp.add(centity);
										found = true;
										//LOGGER.debug("with entity locator, SME form a composite entity proposals: "+centity.toString());
										//entities.add(centityp);
									}
								}else{//anterior maxilla 
									//corrected 6/1/13 [basal scutes]: sentity1 be the entity; sentity is the entity locator
									//relation & entity locator: 
									FormalRelation rel = Dictionary.partof;
									rel.setConfidenceScore((float)1.0);
									REntity rentity = new REntity(rel, sentity);
									//composite entity = entity locator for sentity
									CompositeEntity centity = new CompositeEntity(); 
									centity.addEntity(sentity1); 
									centity.addEntity(rentity);	
									centity.setString(this.originalentityphrase);
									centityp.add(centity);
									found = true;
									//LOGGER.debug("without entity locator, SME form a composite entity proposals: "+centityp.toString());
								}	
							}else{
								LOGGER.debug("SME search for spatial term  '"+spatialterm+"' found no match");
							}
						}
					}
				}

			}
			
			
			
			if(found){
				if(entities==null) 	entities = new ArrayList<EntityProposals>();
				Utilities.addEntityProposals(entities, centityp);
				LOGGER.debug("SpatialModifiedEntityStrategy completed, returns");
				for(EntityProposals ep: entities){
					LOGGER.debug(".."+ep.toString());
				}
			}
			
			//caching
			if(entities==null) SpatialModifiedEntityStrategy.nomatchcache.add(entityphrase+"+"+elocatorphrase);
			else SpatialModifiedEntityStrategy.cache.put(entityphrase+"+"+elocatorphrase, entities);
		}


	}

	/**
	 * ventral portion => ventral region as portion is a syn of region
	 * @param spatialterm
	 * @return replace spatial term head noun synonyms with the head noun.
	 */
	private String synVariation(String spatialterm) {
		spatialterm = spatialterm.trim();
		if(spatialterm.indexOf(" ")>0){
			String result =  spatialterm.substring(0, spatialterm.lastIndexOf(" ")).trim();
			String syn = spatialterm.substring(spatialterm.lastIndexOf(" ")).trim();
			if(Dictionary.headnounsyns.get(syn)!=null) return spatialterm; //already with the headnoun
			Enumeration<String> terms = Dictionary.headnounsyns.keys();
			String matches = "";
			while(terms.hasMoreElements()){
				String term = terms.nextElement();
				if(term.trim().length()>0){
					String syns = Dictionary.headnounsyns.get(term);
					if(syns.length()>0 && syn.matches("\\b"+syns+"\\b")){
						matches = term+"|";
					}
				}
			}
			if(matches.length()>0){
				matches = matches.replaceAll("\\|$", "");
				result +=" "+matches; //ventral region
				return result;
			}else{
				return spatialterm;
			}

		}else{
			return spatialterm;
		}
	}
	
	/**
	 * search result for entity
	 * @return
	 */
	public ArrayList<EntityProposals> getEntityResult() {
		return this.sentityps;
	}

	/**
	 * search result for entity locator
	 * @return
	 */
	public ArrayList<EntityProposals> getEntityLocatorResult() {
		return this.entityls;
	}

	public ArrayList<EntityProposals> getEntities() {
		return this.entities;
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}




}
