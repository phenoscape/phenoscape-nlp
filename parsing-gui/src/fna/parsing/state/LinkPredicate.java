package fna.parsing.state;

import org.apache.commons.collections15.Predicate;

public class LinkPredicate implements Predicate<MyLink>{
	
		public boolean evaluate(MyLink l) {
			return l.getWeight() >=2;
			} 
		

}
