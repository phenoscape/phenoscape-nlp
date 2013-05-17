package fna.beans;

import java.util.Arrays;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/* This bean represents a row in the Character tab terms area */

public class CoOccurrenceBean {
	private TermBean term1;
	private TermBean term2;
	private Button contextButton;
	private Label frequency;
	private String [] sourceFiles;
	private int groupNo;
	private String keep;
	private Text text4unpaired;
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

	public void setText(Text textbox){
		this.text4unpaired = textbox;
	}
	
	public Text getText(){
		return this.text4unpaired;
	}
	public CoOccurrenceBean(TermBean term1, TermBean term2,
			Button contextButton, Label frequency, String[] sourceFiles,
			int groupNo, String keep) {
		this.term1 = term1;
		this.term2 = term2;
		this.contextButton = contextButton;
		this.frequency = frequency;
		this.sourceFiles = sourceFiles;
		this.groupNo = groupNo;
		this.keep = keep;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((contextButton == null) ? 0 : contextButton.hashCode());
		result = prime * result
				+ ((frequency == null) ? 0 : frequency.hashCode());
		result = prime * result + groupNo;
		result = prime * result + ((keep == null) ? 0 : keep.hashCode());
		result = prime * result + Arrays.hashCode(sourceFiles);
		result = prime * result + ((term1 == null) ? 0 : term1.hashCode());
		result = prime * result + ((term2 == null) ? 0 : term2.hashCode());
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
		if (!(obj instanceof CoOccurrenceBean))
			return false;
		final CoOccurrenceBean other = (CoOccurrenceBean) obj;
		if (contextButton == null) {
			if (other.contextButton != null)
				return false;
		} else if (!contextButton.equals(other.contextButton))
			return false;
		if (frequency == null) {
			if (other.frequency != null)
				return false;
		} else if (!frequency.equals(other.frequency))
			return false;
		if (groupNo != other.groupNo)
			return false;
		if (keep == null) {
			if (other.keep != null)
				return false;
		} else if (!keep.equals(other.keep))
			return false;
		if (!Arrays.equals(sourceFiles, other.sourceFiles))
			return false;
		if (term1 == null) {
			if (other.term1 != null)
				return false;
		} else if (!term1.equals(other.term1))
			return false;
		if (term2 == null) {
			if (other.term2 != null)
				return false;
		} else if (!term2.equals(other.term2))
			return false;
		return true;
	}
	/**
	 * @return the sourceFiles
	 */
	public String [] getSourceFiles() {
		return sourceFiles;
	}
	/**
	 * @param sourceFiles the sourceFiles to set
	 */
	public void setSourceFiles(String [] sourceFiles) {
		this.sourceFiles = sourceFiles;
	}
	/**
	 * @return the groupNo
	 */
	public int getGroupNo() {
		return groupNo;
	}
	/**
	 * @param groupNo the groupNo to set
	 */
	public void setGroupNo(int groupNo) {
		this.groupNo = groupNo;
	}
	public CoOccurrenceBean() {}
	/**
	 * @return the term1
	 */
	public TermBean getTerm1() {
		return term1;
	}
	/**
	 * @param term1 the term1 to set
	 */
	public void setTerm1(TermBean term1) {
		this.term1 = term1;
	}
	/**
	 * @return the term2
	 */
	public TermBean getTerm2() {
		return term2;
	}
	/**
	 * @param term2 the term2 to set
	 */
	public void setTerm2(TermBean term2) {
		this.term2 = term2;
	}
	/**
	 * @return the contextButton
	 */
	public Button getContextButton() {
		return contextButton;
	}
	/**
	 * @param contextButton the contextButton to set
	 */
	public void setContextButton(Button contextButton) {
		this.contextButton = contextButton;
	}
	/**
	 * @return the frequency
	 */
	public Label getFrequency() {
		return frequency;
	}
	/**
	 * @param frequency the frequency to set
	 */
	public void setFrequency(Label frequency) {
		this.frequency = frequency;
	}


	

}
