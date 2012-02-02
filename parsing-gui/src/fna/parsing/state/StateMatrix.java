package fna.parsing.state;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.uci.ics.jung.algorithms.cluster.VoltageClusterer;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseMultigraph;
import fna.charactermarkup.Utilities;
import fna.parsing.ApplicationUtilities;


@SuppressWarnings("unchecked")
public class StateMatrix {
	
	private ArrayList<State> states = null;
	private ArrayList<Cell> matrix = null;
	private int edgeCount = 0;
	private Connection conn = null;
	private String tableprefix = null;
	private String glossarytable = null;
	private ArrayList<String> freeterms = new ArrayList<String>();
	private String blockedterms = "absent|lacking|present"; //may be paired with any term, therefore should be blocked
	//private Hashtable<State, Hashtable<State, CoocurrenceScore>> matrix = null;
	
	StateMatrix(Connection conn, String tableprefix,String glosstable){
		states = new ArrayList<State>();
		matrix = new ArrayList<Cell>();
		this.tableprefix = tableprefix;
		this.conn = conn;
		this.glossarytable = glosstable;
		Statement stmt = null;
		try{
			stmt = conn.createStatement();
			stmt.execute("drop table if exists "+tableprefix+"_terms");
			stmt.execute("create table if not exists "+tableprefix+"_terms (term varchar(100), cooccurTerm varchar(100), frequency int(4), grouped varchar(2) default 'n', sourceFiles varchar(2000),  primary key(term, cooccurTerm))");
			stmt.execute("drop table if exists "+tableprefix+"_grouped_terms");
			stmt.execute("create table if not exists "+tableprefix+"_grouped_terms (groupId int, term varchar(100), cooccurTerm varchar(100), frequency int(4), keep varchar(20), sourceFiles varchar(2000))");
			stmt.execute("drop table if exists "+tableprefix+"_group_decisions");
			stmt.execute("create table if not exists "+tableprefix+"_group_decisions (groupId int, category varchar(200), primary key(groupId))");
			stmt.execute("drop table if exists "+tableprefix+"_term_category");
			stmt.execute("create table if not exists "+tableprefix+"_term_category (term varchar(100), category varchar(200))");
			//noneqterms must not be refreshed
			//stmt.execute("create table if not exists "+tableprefix+"_noneqterms (term varchar(100) not null, source varchar(200))");
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	/**
	 * empty matrix with the knownstates as the row and column
	 * @param knownstates
	 */
	StateMatrix(Connection conn, String tableprefix, Set<State> knownstates,String glosstable){
		Iterator<State> it = knownstates.iterator();
		while(it.hasNext()){
			State s = (State)it.next();
			states.add(s);
		}
		this.conn = conn;		
		this.tableprefix = tableprefix;
		this.glossarytable = glosstable;
		Statement stmt = null;
		try{
			stmt = conn.createStatement();
			stmt.execute("drop table if exists "+tableprefix+"_terms");
			stmt.execute("create table if not exists "+tableprefix+"_terms (term varchar(100), cooccurTerm varchar(100), frequency int(4), keep varchar(20), sourceFiles varchar(2000),  primary key(term, cooccurTerm))");
			stmt.execute("drop table if exists "+tableprefix+"_grouped_terms");
			stmt.execute("create table if not exists "+tableprefix+"_grouped_terms (groupId int, term varchar(100), cooccurTerm varchar(100), frequency int(4), keep varchar(20), sourceFiles varchar(2000), primary key(term, cooccurTerm))");
			stmt.execute("drop table if exists "+tableprefix+"_group_decisions");
			stmt.execute("create table if not exists "+tableprefix+"_group_decisions (groupId int, category varchar(200), primary key(groupId))");
			stmt.execute("drop table if exists "+tableprefix+"_term_category");
			stmt.execute("create table if not exists "+tableprefix+"_term_category (term varchar(100), category varchar(200))");
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("unused")
	private ArrayList<State> getStates(){
		//return matrix.keySet();
		return states;
	}
	
	/**
	 * 
	 * @param s1
	 * @param s2
	 * @param score: count = 1 for now
	 */
	public void addPair(State s1, State s2, int score, String source){
		//which one(s) is a new state?
		if(s1.isEmpty() || s2.isEmpty()){
			return;
		}
		
		int f1 = 0;
		int f2 = 0;
		if(states.contains(s1)){
			f1 = 1;
		}
		if(states.contains(s2)){
			f2 = 1;
		}
		
		if(f1+f2 == 0){//two new states
			states.add(s1);
			states.add(s2);
			int i1 = states.indexOf(s1);
			int i2 = states.indexOf(s2);
			Cell s1s2 = new Cell(i1, i2, new CooccurrenceScore(score, source));
			Cell s2s1 = new Cell(i2, i1, new CooccurrenceScore(score, source));
			matrix.add(s1s2);
			matrix.add(s2s1);
		}
		
		if(f1+f2 == 1){//one new state
			State newstate = f1 == 0? s1: s2;
			State existstate = newstate == s1? s2: s1;
			states.add(newstate);
			int i1 = states.indexOf(existstate);
			int i2 = states.indexOf(newstate);
			Cell en = new Cell(i1, i2, new CooccurrenceScore(score, source));
			Cell ne = new Cell(i2, i1, new CooccurrenceScore(score, source));
			matrix.add(en);
			matrix.add(ne);
		}
		
		if(f1+f2 == 2){// 0 new state, update the score
			int i1 = states.indexOf(s1);
			int i2 = states.indexOf(s2);
			Cell c = new Cell(i1, i2, null);
			int cellindex = matrix.indexOf(c);
			if(cellindex >=0){
				Cell e = matrix.get(cellindex);
				CooccurrenceScore cs = e.getScore();
				cs.updateBy(score, source);
			}else{
				matrix.add(new Cell(i1, i2, new CooccurrenceScore(score, source)));
			}
			
			c = new Cell(i2, i1, null);
			cellindex = matrix.indexOf(c);
			if(cellindex >=0){
				Cell e = matrix.get(cellindex);
				CooccurrenceScore cs = e.getScore();
				cs.updateBy(score, source);
			}else{
				matrix.add(new Cell(i2, i1, new CooccurrenceScore(score, source)));
			}
		}
		
	}
	
	public State getStateByName(String state){
		Iterator<State> en = states.iterator();
		while(en.hasNext()){
			State s = en.next();
			if(s.getName().compareTo(state)==0){
				return s;
			}
		}
		return null;
	}
	
	public CooccurrenceScore cooccurScore(String str1, String str2){
		State state1 = this.getStateByName(str1);
		State state2 = this.getStateByName(str2);
		int i = states.indexOf(state1);
		int j = states.indexOf(state2);
		Cell c = new Cell(i, j, null);
		if(matrix.contains(c)){
			c = matrix.get(matrix.indexOf(c));
			return c.getScore();//"[]" is an empty score
		}else{
			return null;
		}		
	}
	
	public void save2MySQL(Connection conn, String tableprefix, String username, String password){
		try{
			Collections.sort(matrix, new Cell());
			Cell c = null;
			int n = states.size();
			for(int i = 0; i < n; i++){
				for(int j = 0; j < n; j++){
					c = new Cell(i, j, null);
					if(matrix.contains(c)){
						State s1 = states.get(i);
						State s2 = states.get(j);
						//Statement stmt = conn.createStatement();
						//ResultSet rs = stmt.executeQuery("select term from terms where term = '"+s1.getName()+"' and cooccurTerm='"+s2.getName()+"'");
						Cell data = matrix.get(matrix.indexOf(c));
						CooccurrenceScore score = data.getScore();//"[]" is an empty score
						//String values = "'"+s1.getName()+"','";
						
						String src = score.getSourcesAsString();
						src = src.length() >=2000? src.substring(0, 1999) : src;
						//values +=s2.getName()+"',"+score.getSources().size()+",'"+src+"'";
						String othervalues = score.getSources().size()+",'"+src+"'";
						String[] pair = {s1.getName(), s2.getName()};
						Arrays.sort(pair);
						insertIntoTermsTable(pair, othervalues);
						}
				}				
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}	

	private void insertIntoTermsTable(String[] pair, String othervalues) {
		// TODO Auto-generated method stub
		try{
			Statement stmt = conn.createStatement();
			//check to see if the pair exist in terms table
			ResultSet rs = stmt.executeQuery("select * from "+tableprefix+"_terms where term='"+pair[0]+"' and cooccurTerm='"+pair[1]+"'");
			if(!rs.next()){
				stmt.execute("insert into "+tableprefix+"_terms (term, cooccurTerm, frequency, sourceFiles) values('"+pair[0]+"','"+pair[1]+"',"+othervalues+")");
			}
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	/**
	 * In preparing for character curation:
	 * 1. group co-occured nodes into groups, using JUNG library.
	 * 2. other non-cooccured terms as one cluster
	 */
	public Object Grouping(){
		//deal with co-occured terms
		StringBuffer cooccurTerms = new StringBuffer();
		//construct the graph
		Graph<State, MyLink> g = new UndirectedSparseMultigraph<State, MyLink>();		
		//state as node
		Iterator<Cell> it = this.matrix.iterator();
		while(it.hasNext()){
			Cell c = it.next();
			State node1  = this.states.get(c.getCindex());
			State node2 = this.states.get(c.getRindex());
			String term1 = node1.getName();
			String term2 = node2.getName();
			if(!term1.matches("("+this.blockedterms+")") && !term2.matches("("+this.blockedterms+")") &&
			   (!Utilities.inGlossary(term1, conn, this.glossarytable, this.tableprefix) || !Utilities.inGlossary(term2, conn, this.glossarytable, this.tableprefix))){
				cooccurTerms.append("'"+node1.getName()+"',");
				cooccurTerms.append("'"+node2.getName()+"',");
				int weight = c.getScore().valueSum();
				//if(weight>1){
				System.out.println(weight+" links "+node1.getName()+" and "+node2.getName());
				//}
				g.addVertex(node1);
				g.addVertex(node2);
				for(int i = 0; i<weight; i++){
					g.addEdge(new MyLink(1, edgeCount++), node1, node2);
				}
			}
		}
		/*
		//visualize the graph
		EdgePredicateFilter<State, MyLink> f1 = new EdgePredicateFilter<State, MyLink>(new LinkPredicate());
		VertexPredicateFilter<State, MyLink> f2 = new VertexPredicateFilter<State, MyLink>(new VertexPredicate(g));
		g = f1.transform(g);
		g = f2.transform(g);
		//Layout<State, MyLink> layout = new KKLayout<State, MyLink>(g); //a big round circle with vertex on top of each other
		//Layout<State, MyLink> layout = new FRLayout<State, MyLink>(g);//recognizable groups in the center
		Layout<State, MyLink> layout = new CircleLayout<State, MyLink>(g);


		final VisualizationModel<State,MyLink> visualizationModel = 
		            new DefaultVisualizationModel<State,MyLink>(layout, new Dimension(1400,800));
		VisualizationViewer vv =  new VisualizationViewer<State, MyLink>(visualizationModel, new Dimension(1400,800));

		VertexLabelAsShapeRenderer<State,MyLink> vlasr = new VertexLabelAsShapeRenderer<State,MyLink>(vv.getRenderContext());
		Transformer<MyLink, Stroke> edgeStrokeTransformer =
					new Transformer<MyLink, Stroke>() {
						public Stroke transform(MyLink l) {
							Stroke edgeStroke = new BasicStroke(1 + (l.getWeight()/10.0f), BasicStroke.CAP_BUTT,
									BasicStroke.JOIN_MITER);
							return edgeStroke;
						}
				};

		vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
		vv.getRenderContext().setVertexShapeTransformer(vlasr);
		vv.getRenderContext().setVertexLabelRenderer(new DefaultVertexLabelRenderer(Color.red));
		vv.getRenderContext().setEdgeDrawPaintTransformer(new ConstantTransformer(Color.yellow));
		vv.getRenderContext().setEdgeStrokeTransformer(edgeStrokeTransformer);
		vv.getRenderer().setVertexLabelRenderer(vlasr);
		vv.setBackground(Color.black);
		vv.getRenderer().getVertexLabelRenderer().setPosition(Position.CNTR);
		JFrame frame = new JFrame("Simple Graph View 2");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(vv);
		frame.pack();
		frame.setVisible(true);
		 */
		
		//use different algorithms to group the graph
		/*
		//1. Bicomponent Clustering: 336 groups.
		System.out.println("Bicomponent Clustering");
		BicomponentClusterer bc = new BicomponentClusterer();
		Set groups = (Set)bc.transform(g);
		printGroups(groups);
		
		//2. Edge Betweenness Clustering: 31 groups, 1 large group, some states may not have been included (total states 698)
		System.out.println("Edge Betweenness Clustering");
		EdgeBetweennessClusterer ebc = new EdgeBetweennessClusterer(1); //use any number for 1 here
		groups = ebc.transform(g);
		printGroups(groups);
		
		//3. Week Component Clustering, 31 groups similiar to the above
		System.out.println("Week Component Clustering");
		WeakComponentClusterer wcc = new WeakComponentClusterer();
		groups = wcc.transform(g);
		printGroups(groups);
		*/
		//4. Voltage Clustering: 21  groups of varied sizes
		System.out.println("Voltage Clustering");
		Collection clusters = null;
		if(g.getEdgeCount()>4){ //the voltage clustering algorithm works only when this condition is met
			VoltageClusterer vc = new VoltageClusterer(g, 50);
			if(g.getEdgeCount()>4)
				clusters = vc.cluster(50);
			else
				clusters = vc.cluster(g.getEdgeCount()-1);
			//saveClustersInDB(clusters);
		}else{
			Collection<State>  stateCol = g.getVertices();
			Set<State> stateSet = new HashSet<State>(stateCol);
			ArrayList<Set<State>> stateList = new ArrayList<Set<State>>();
			stateList.add(stateSet);
			//saveClustersInDB(stateList);
			clusters = stateList;
			//return stateList;
		}
		
		//deal with non-cooccured terms
		//add terms that are in WordRoles ("c") but not as cooccured.
		//save these terms in DB
		//save these terms in clusters for return
		ArrayList<State> freeStates = new ArrayList<State> ();
		try{
			Statement stmt = conn.createStatement();
			String q = "select word from "+this.tableprefix+"_"+ApplicationUtilities.getProperty("WORDROLESTABLE")+
					" where semanticrole ='c' and" +
					" mid(word, locate('_', word)+1) not in (select distinct term from " +this.glossarytable+")";
					
			String coocur = cooccurTerms.toString().replaceFirst(",$", "").replaceAll(",+", ",").trim();
			if(coocur.length()>0){
					q += " and word not in ("+coocur+")";
			}
			System.out.println(q);
			ResultSet rs = stmt.executeQuery(q);
			while(rs.next()){
				String freeterm = rs.getString("word");
				if(freeterm.indexOf("_")<0){//ignore terms such as lance_linear
					freeterms.add(freeterm);
					State free = new State(freeterm);
					freeStates.add(free);
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		Set<State> freeStateSet = new HashSet<State>(freeStates);
		clusters.add(freeStateSet);		
		saveClustersInDB(clusters);
		return clusters==null? new ArrayList<Set<State>>() : clusters;
	}
	


	/**
	 * the last set in the clusters may contain a set of freestates
	 * @param clusters
	 */
	private void saveClustersInDB(Collection<Set<State>> clusters){
		try{
			Statement stmt = conn.createStatement();
			int gcount = 1;
			Iterator<Set<State>> sets = clusters.iterator();
			while(sets.hasNext()){
				Set<State> states = (Set<State>)sets.next();
				String stategroup = formGroup(states);
				if(stategroup != null){
					//collect info from _terms table for a set of co-occured terms, insert this set as one group
					String[] terms = stategroup.replace("'", "").split("\\s*,\\s*");
					Arrays.sort(terms);
					for(int i = 0; i < terms.length; i++){
						for(int j = i+1; j <terms.length; j++){
							if(!Utilities.inGlossary(terms[i], conn, this.glossarytable, this.tableprefix) || !Utilities.inGlossary(terms[j], conn, this.glossarytable, this.tableprefix)){
								String[] info = freqsource(terms[i], terms[j]); //search for frequency and source files info for the term pair
								if(info!=null && info[1].split(",").length>=3){ //include only the terms apprears 3 or more times.
									String q = "insert into "+this.tableprefix+"_grouped_terms(term, cooccurTerm, frequency, sourceFiles) values ('"+terms[i]+"', '"+terms[j]+"',"+Integer.parseInt(info[0])+",'"+info[1]+"')";
									System.out.println("query::"+q);
									stmt.execute(q);
								}
							}
						}
					}
					ResultSet rs = stmt.executeQuery("select * from "+this.tableprefix+"_grouped_terms where isnull(groupId) or groupID=0");
					//set group id
					if(rs.next()){
						stmt.execute("update "+this.tableprefix+"_grouped_terms set groupId="+gcount+" where isnull(groupId) or groupID=0");
						gcount++;
					}
				}
			}
			//lastly, add this.freeterms as a set
			Iterator<String> it = this.freeterms.iterator();
			//Statement stmt = conn.createStatement();
			while(it.hasNext()){
				String w = it.next();
				ResultSet rs = stmt.executeQuery("select distinct source from "+this.tableprefix+"_"+ApplicationUtilities.getProperty("SENTENCETABLE")+
						" where sentence like '% "+w+" %' or sentence like '"+w+" %' or sentence like '% "+w+"'");
				String srcs = "";
				int count = 0;
				while(rs.next()){
					srcs+=rs.getString("source")+",";
					count++;
				}
				srcs = srcs.replaceFirst(",$", "").replaceAll(",+", ",").trim();
				if(srcs.length()>2000){
					srcs = srcs.substring(0, 1999);
				}
				stmt.execute("insert into "+this.tableprefix+"_grouped_terms(term, cooccurTerm, frequency, sourceFiles) values ('"+w+"', '', "+count+ ", '"+srcs+"')");
			}
			//set group id
			ResultSet rs = stmt.executeQuery("select * from "+this.tableprefix+"_grouped_terms where isnull(groupId) or groupID=0");
			if(rs.next()){
				stmt.execute("update "+this.tableprefix+"_grouped_terms set groupId="+gcount+" where isnull(groupId) or groupID=0");
			}
			stmt.close();
		}catch(Exception e){
			e.printStackTrace();
		}
					
	}
		
			
	
	/**
	 * not to include the same term pair in two different clusters/groups. This is done by using the "grouped" column in the _terms table
	 * @param term1
	 * @param term2
	 * @return the frequency [0] the two terms appear as a pair in a list of source files[1]
	 */
	private String[] freqsource(String term1, String term2) {
		String[] info = new String[2];
		try{
			Statement stmt = conn.createStatement();
			ResultSet rs = stmt.executeQuery("select frequency, sourceFiles from "+this.tableprefix+"_terms where grouped='n' and (term='"+term1+"' and cooccurterm='"+term2+"') or (term='"+term2+"' and cooccurterm='"+term1+"')");
			if(rs.next()){
				info[0] = rs.getString(1);
				info[1] = rs.getString(2);
				stmt.execute("update "+this.tableprefix+"_terms set grouped='y' where (term='"+term1+"' and cooccurterm='"+term2+"') or (term='"+term2+"' and cooccurterm='"+term1+"')");
				stmt.close();
				return info;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * If all states are in the glossary, ignore this set.
	 * Otherwise, form a group
	 * 
	 * @param states
	 * @return 't1', 't2', 't3',...,'tn'
	 */
	private String formGroup(Set<State> states) {
		boolean keep = false;
		Iterator<State> sit = states.iterator();
		StringBuffer statestring = new StringBuffer();
		if(states.size()==0)
			return null;
		HashMap stateCategoryCountMap = new HashMap<String,Integer>();
		while(sit.hasNext()){
			State s = sit.next();
			String term = s.getName();
			statestring.append("'"+term+"', ");		
			try{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct category from "+this.glossarytable+" where term ='"+term+"'");
				
				while(rs.next())
				{
					String category = rs.getString("category");
					if(stateCategoryCountMap.containsKey(category))
					{
						Integer count= (Integer) stateCategoryCountMap.get(category);
						stateCategoryCountMap.remove(category);
						stateCategoryCountMap.put(category, count+1);
					}
					else
					{
						stateCategoryCountMap.put(category, 1);
					}
				}
				
			}catch (Exception e) {
				e.printStackTrace();
			}
			
			//commented on 14th march to find the overlapping category.If such a category is present then decide whether to display the entire set or not
			
			/*
			State s = sit.next();
			String term = s.getName();
			statestring.append("'"+term+"', ");		
			ArrayList<String> chars = new ArrayList<String> ();
			try{
				Statement stmt = conn.createStatement();
				ResultSet rs = stmt.executeQuery("select distinct category from "+this.glossarytable+" where term ='"+term+"'");
				while(rs.next()){
					String chara = rs.getString("category");
					chars.add(chara);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
			if(chars.size()==0){
				keep = true;
				while(sit.hasNext())
				{
					State remainingStates = sit.next();
					String remainingTerm = remainingStates.getName();
					statestring.append("'"+remainingTerm+"', ");		
					
				}
				break;
			}					
		*/}
		//check the max count of mapvalues
		Collection values = stateCategoryCountMap.values();
		ArrayList valueList = new ArrayList(values);
		//Collections.sort(valueList);
		if(!valueList.contains(new Integer(states.size()))){
			//there is not one category that is common to all terms
			//sit = states.iterator();
			keep = true;
			while(sit.hasNext())
			{
				State remainingStates = sit.next();
				String remainingTerm = remainingStates.getName();
				statestring.append("'"+remainingTerm+"', ");		
				
			}
			
		}
		else{
			keep=false;
		}
	
		String stategroup = statestring.toString().replaceFirst(", $", "");		
		return keep? stategroup : null;
	}
	
	public String toString(){
		Collections.sort(matrix, new Cell());
		StringBuffer sb = new StringBuffer("");
		Cell c = null;
		int n = states.size();
		for(int i = 0; i < n; i++){
			for(int j = 0; j < n; j++){
				c = new Cell(i, j, null);
				if(matrix.contains(c)){
					State s1 = states.get(i);
					State s2 = states.get(j);
					Cell data = matrix.get(matrix.indexOf(c));
					String score = data.getScore().toString();//"[]" is an empty score
					//if(score.length() > 2){
						sb.append(s1.toString()+" coocurred with: ");
						sb.append("\t"+s2.toString()+" "+score+"\n");
					//}
				}
			}
		}
		return sb.toString();
	}
//changing on March1st by Prasad
	public int output2GraphML() {
		GraphMLOutputter gmo = new GraphMLOutputter();
		//from saved grouped_terms
		ArrayList<ArrayList> groups = new ArrayList<ArrayList>();
		int gnumber = 0;		
		try{
			Statement stmt = conn.createStatement();
			String q = "select groupId from "+this.tableprefix+"_grouped_terms order by groupId desc";
			ResultSet rs = stmt.executeQuery(q);
			if(rs.next()){
				gnumber = rs.getInt("groupId");
			}				
			for(int i = 1; i<=gnumber; i++){
				q = "select term, cooccurTerm, frequency from "+this.tableprefix+"_grouped_terms where groupId='"+i+"'";
				rs = stmt.executeQuery(q);
				ArrayList<ArrayList> group = new ArrayList<ArrayList>();
				while(rs.next()){
					ArrayList<String> row = new ArrayList<String>();
					row.add(rs.getString("term"));
					row.add(rs.getString("cooccurTerm"));
					row.add(rs.getString("frequency"));				
					group.add(row);
				}
				rs.close();
				groups.add(group);
			}
		}catch(Exception e){
			e.printStackTrace();
		}			
		if(groups.size()>0){
			gmo.output(groups, 1);
			return groups.size();
		}else
		{
			return 0;
		}
	}
					 
}
