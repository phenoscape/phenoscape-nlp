package fna.beans;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class ExpressionBean {

	private Label label;
	private Text text;
	
	public Label getLabel() {
		return label;
	}
	public Text getText() {
		return text;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((text == null) ? 0 : text.hashCode());
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
		if (!(obj instanceof ExpressionBean))
			return false;
		final ExpressionBean other = (ExpressionBean) obj;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		return true;
	}
	public ExpressionBean(Label label, Text text) {
		super();
		this.label = label;
		this.text = text;
	}
	/**
	 * @param label the label to set
	 */
	public void setLabel(Label label) {
		this.label = label;
	}
	/**
	 * @param text the text to set
	 */
	public void setText(Text text) {
		this.text = text;
	}

}
