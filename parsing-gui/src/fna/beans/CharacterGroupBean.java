package fna.beans;

import java.util.ArrayList;

/** This is a screenshot of the entire character tab
 * @author Partha Pratim Sanyal
 *  */


public class CharacterGroupBean {
	private ArrayList <CoOccurrenceBean> cooccurrences;
	private String groupName;
	private boolean isSaved;
	private String decision;
	/**
	 * @return the cooccurrences
	 */
	public ArrayList<CoOccurrenceBean> getCooccurrences() {
		return cooccurrences;
	}
	/**
	 * @param cooccurrences the cooccurrences to set
	 */
	public void setCooccurrences(ArrayList<CoOccurrenceBean> cooccurrences) {
		this.cooccurrences = cooccurrences;
	}
	/**
	 * @return the groupName
	 */
	public String getGroupName() {
		return groupName;
	}
	/**
	 * @param groupName the groupName to set
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	/**
	 * @return the isSaved
	 */
	public boolean isSaved() {
		return isSaved;
	}
	/**
	 * @param isSaved the isSaved to set
	 */
	public void setSaved(boolean isSaved) {
		this.isSaved = isSaved;
	}

	/**
	 * @return the decision
	 */
	public String getDecision() {
		return decision;
	}
	/**
	 * @param decision the decision to set
	 */
	public void setDecision(String decision) {
		this.decision = decision;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((cooccurrences == null) ? 0 : cooccurrences.hashCode());
		result = prime * result
				+ ((decision == null) ? 0 : decision.hashCode());
		result = prime * result
				+ ((groupName == null) ? 0 : groupName.hashCode());
		result = prime * result + (isSaved ? 1231 : 1237);
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
		if (!(obj instanceof CharacterGroupBean))
			return false;
		final CharacterGroupBean other = (CharacterGroupBean) obj;
		if (cooccurrences == null) {
			if (other.cooccurrences != null)
				return false;
		} else if (!cooccurrences.equals(other.cooccurrences))
			return false;
		if (decision == null) {
			if (other.decision != null)
				return false;
		} else if (!decision.equals(other.decision))
			return false;
		if (groupName == null) {
			if (other.groupName != null)
				return false;
		} else if (!groupName.equals(other.groupName))
			return false;
		if (isSaved != other.isSaved)
			return false;
		return true;
	}
	public CharacterGroupBean(ArrayList<CoOccurrenceBean> cooccurrences,
			String groupName, boolean isSaved) {
		this.cooccurrences = cooccurrences;
		this.groupName = groupName;
		this.isSaved = isSaved;
	}
	
	
}
