package fna.parsing.character;

import java.util.ArrayList;

public class GraphNode {

	private String nodeName;
	private String nodeNumber;
	private ArrayList<String> edges;
	
	public GraphNode (String nodeName) {
		this.nodeName = nodeName;
		edges = new ArrayList<String>();
	}

	@SuppressWarnings("unused")
	private GraphNode(){
		
	}
	/**
	 * @return the nodeName
	 */
	public String getNodeName() {
		return nodeName;
	}
	/**
	 * @param nodeName the nodeName to set
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}
	/**
	 * @return the edges
	 */
	public ArrayList<String> getEdges() {
		return edges;
	}
	/**
	 * @param edges the edges to set
	 */
	public void setEdges(ArrayList<String> edges) {
		this.edges = edges;
	}

	/**
	 * @return the nodeNumber
	 */
	public String getNodeNumber() {
		return nodeNumber;
	}

	/**
	 * @param nodeNumber the nodeNumber to set
	 */
	public void setNodeNumber(String nodeNumber) {
		this.nodeNumber = nodeNumber;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((edges == null) ? 0 : edges.hashCode());
		result = prime * result
				+ ((nodeName == null) ? 0 : nodeName.hashCode());
		result = prime * result
				+ ((nodeNumber == null) ? 0 : nodeNumber.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GraphNode))
			return false;
		final GraphNode other = (GraphNode) obj;
		if (edges == null) {
			if (other.edges != null)
				return false;
		} else if (!edges.equals(other.edges))
			return false;
		if (nodeName == null) {
			if (other.nodeName != null)
				return false;
		} else if (!nodeName.equals(other.nodeName))
			return false;
		if (nodeNumber == null) {
			if (other.nodeNumber != null)
				return false;
		} else if (!nodeNumber.equals(other.nodeNumber))
			return false;
		return true;
	}
}
