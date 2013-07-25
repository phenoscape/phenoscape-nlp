/**
 * 
 */
package outputter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jdom.Element;

/**
 * @author Hong Cui
 * = EntitySearcher4
 * anterior maxilla => anterior region part of maxilla
 * anterior process of maxilla => process part of anterior region part of maxilla
 * 
 *
 */
public class SpatialModifiedEntityStrategy implements AnnotationStrategy {
	private static final Logger LOGGER = Logger.getLogger(SpatialModifiedEntityStrategy.class);   
	ArrayList<EntityProposals> entities;
	private String entityphrase;
	private String elocatorphrase;
	private Element root;
	private String structid;
	private String prep;
	private String originalentityphrase;

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

		ArrayList<SimpleEntity> entityls = new ArrayList<SimpleEntity>();
		//entityl.setString(elocatorphrase);
		if(elocatorphrase.length()>0) {
			ArrayList<FormalConcept> results = new TermSearcher().searchTerm(elocatorphrase, "entity"); //change this to EntitySearcherOriginal?
			if(results!=null){
				LOGGER.debug("SME search for locator '"+elocatorphrase+"' found match: ");
				for(FormalConcept result: results){
					entityls.add((SimpleEntity)result);
					LOGGER.debug(".." +result.toString());
				}
			}else{
				LOGGER.debug("SME search for locator '"+elocatorphrase+"' found no match");
			}
		}
		String t= "";
		Pattern p = Pattern.compile("^("+Dictionary.spatialtermptn+")\\s+("+Dictionary.allSpatialHeadNouns()+")?");
		Matcher m = p.matcher(entityphrase);
		if(m.find()){
			String spatialterm = entityphrase.substring(m.start(), m.end()).trim();
			String newentity = entityphrase.substring(m.end()).trim();
			if(spatialterm.indexOf(" ")<0) spatialterm += " region";
			//take synonyms into account
			spatialterm = synVariation(spatialterm);
			LOGGER.debug("SME formed spatial term '"+spatialterm+"'");
			//entityphrase='ventral surface'
			//if(entityphrasetokens[0].matches("("+Dictionary.spatialtermptn+")")){
			//String newentity = Utilities.join(entityphrasetokens, 1, entityphrasetokens.length-1, " "); //anything after the spatial term
			//SimpleEntity sentity = (SimpleEntity)new TermSearcher().searchTerm(newentity, "entity");
			
			
			ArrayList<EntityProposals> sentityps = null;
			if(newentity.length()<=0){
				//entityls => sentityps
				EntityProposals ep = new EntityProposals();
				boolean populated = false;
				for(SimpleEntity se: entityls){
					ep.add(se);
					populated = true;
				}
				if(populated){
					sentityps = new ArrayList<EntityProposals> ();
					sentityps.add(ep);
					//empty entityls
					entityls = new ArrayList<SimpleEntity>();
				}
			}else{
				LOGGER.debug("SME search for entity '"+newentity+"'");
				sentityps = new EntitySearcherOriginal().searchEntity(root, structid,  newentity, elocatorphrase, originalentityphrase, prep); //advanced search
			}
			
			if(sentityps!=null){
				entities = new ArrayList<EntityProposals>();
				LOGGER.debug("SME found match for entity, now search for spatial term  '"+spatialterm+"'");
				//SimpleEntity sentity1 = (SimpleEntity)new TermSearcher().searchTerm(spatialterm, "entity");
				//ArrayList<FormalConcept> spatialentities = TermSearcher.regexpSearchTerm(spatialterm, "entity");//anterior + region: simple search
				ArrayList<FormalConcept> spatialentities = new TermSearcher().searchTerm(spatialterm, "entity");//anterior + region: simple search
				if(spatialentities!=null) LOGGER.debug("SME search for spatial term  '"+spatialterm+"' found match");
				EntityProposals centityp = new EntityProposals();
				centityp.setPhrase(this.originalentityphrase);
				for(EntityProposals sentityp: sentityps){
					for(Entity sentity: sentityp.getProposals()){
						for(FormalConcept spatialentity: spatialentities){
							SimpleEntity sentity1 = (SimpleEntity) spatialentity;
							if(sentity1!=null){//ventral region
								//nested part_of relation
								if(entityls.size()>0 || sentity instanceof CompositeEntity){ //anterior process of maxilla 
									//relation & entity locator: inner
									FormalRelation rel = Dictionary.partof;
									rel.setConfidenceScore((float)1.0);
									ArrayList<REntity> rentities = new ArrayList<REntity>();
									REntity re = null;
									//TODO: what if both conditions are true?
									if(sentity instanceof CompositeEntity){
										//sentity should not have any post-composed quality
										re = ((CompositeEntity) sentity).getEntityLocator();
										rentities.add(re);
										sentity = ((CompositeEntity) sentity).getTheSimpleEntity();
									}else if(entityls.size()>0){
										for(SimpleEntity entityl: entityls){
											re = new REntity(rel, entityl);
											rentities.add(re);
										}
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
										LOGGER.debug("with entity locator, SME form a composite entity proposals: "+centity.toString());
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
									LOGGER.debug("without entity locator, SME form a composite entity proposals: "+centityp.toString());
								}	
							}else{
								LOGGER.debug("SME search for spatial term  '"+spatialterm+"' found no match");
							}
						}
					}
				}
				Utilities.addEntityProposals(entities, centityp);
			}
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
