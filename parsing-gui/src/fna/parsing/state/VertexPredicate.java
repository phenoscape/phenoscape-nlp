package fna.parsing.state;

import org.apache.commons.collections15.Predicate;

import edu.uci.ics.jung.graph.Graph;

public class VertexPredicate  implements Predicate<State>{
			Graph<State, MyLink> g;
		public VertexPredicate(Graph<State, MyLink> g){
			this.g = g;
		}
		public boolean evaluate(State s) {
			return g.degree(s) > 1;
		} 
		
}
