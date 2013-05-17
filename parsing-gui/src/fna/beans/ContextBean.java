package fna.beans;
/** This bean is used to get data from the sentence table in order to show
 * context table data in the Character tab 
 * @author Partha Pratim Sanyal
 *
 */
public class ContextBean {
	private String sourceText;
	private String sentence;
	/**
	 * @return the sourceText
	 */
	public String getSourceText() {
		return sourceText;
	}
	/**
	 * @param sourceText the sourceText to set
	 */
	public void setSourceText(String sourceText) {
		this.sourceText = sourceText;
	}
	/**
	 * @return the sentence
	 */
	public String getSentence() {
		return sentence;
	}
	/**
	 * @param sentence the sentence to set
	 */
	public void setSentence(String sentence) {
		this.sentence = sentence;
	}
	public ContextBean(String sourceText, String sentence) {
		this.sourceText = sourceText;
		this.sentence = sentence;
	}
	
}
