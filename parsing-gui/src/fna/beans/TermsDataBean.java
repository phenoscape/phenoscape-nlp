package fna.beans;
/**
 * This bean holds data directly from the database and also saved data into it
 * @author Partha Pratim Sanyal
 * */
public class TermsDataBean {
	private String term1;
	private String term2;
	private int groupId;
	private int frequency;
	private String keep;
	private String [] sourceFiles ;
	/**
	 * @return the term1
	 */
	public String getTerm1() {
		return term1;
	}
	/**
	 * @param term1 the term1 to set
	 */
	public void setTerm1(String term1) {
		this.term1 = term1;
	}
	/**
	 * @return the term2
	 */
	public String getTerm2() {
		return term2;
	}
	/**
	 * @param term2 the term2 to set
	 */
	public void setTerm2(String term2) {
		this.term2 = term2;
	}
	/**
	 * @return the groupId
	 */
	public int getGroupId() {
		return groupId;
	}
	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}
	/**
	 * @return the frequency
	 */
	public int getFrequency() {
		return frequency;
	}
	/**
	 * @param frequency the frequency to set
	 */
	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}
	/**
	 * @return the sourceFiles
	 */
	public String[] getSourceFiles() {
		return sourceFiles;
	}
	/**
	 * @param sourceFiles the sourceFiles to set
	 */
	public void setSourceFiles(String[] sourceFiles) {
		this.sourceFiles = sourceFiles;
	}
	/**
	 * @return the keep
	 */
	public String getKeep() {
		return keep;
	}
	/**
	 * @param keep the keep to set
	 */
	public void setKeep(String keep) {
		this.keep = keep;
	}
	
}
