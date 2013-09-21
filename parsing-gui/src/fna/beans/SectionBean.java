package fna.beans;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class SectionBean {

	private Text order;
	private Label section;
	private Text startTokens;
	private Text endTokens;
	private Text embeddedTokens;
	public SectionBean(Text order, Label section, Text startTokens,
			Text endTokens, Text embeddedTokens) {
		super();
		this.order = order;
		this.section = section;
		this.startTokens = startTokens;
		this.endTokens = endTokens;
		this.embeddedTokens = embeddedTokens;
	}
	/**
	 * @return the order
	 */
	public Text getOrder() {
		return order;
	}
	/**
	 * @param order the order to set
	 */
	public void setOrder(Text order) {
		this.order = order;
	}
	/**
	 * @return the section
	 */
	public Label getSection() {
		return section;
	}
	/**
	 * @param section the section to set
	 */
	public void setSection(Label section) {
		this.section = section;
	}
	/**
	 * @return the startTokens
	 */
	public Text getStartTokens() {
		return startTokens;
	}
	/**
	 * @param startTokens the startTokens to set
	 */
	public void setStartTokens(Text startTokens) {
		this.startTokens = startTokens;
	}
	/**
	 * @return the endTokens
	 */
	public Text getEndTokens() {
		return endTokens;
	}
	/**
	 * @param endTokens the endTokens to set
	 */
	public void setEndTokens(Text endTokens) {
		this.endTokens = endTokens;
	}
	/**
	 * @return the embeddedTokens
	 */
	public Text getEmbeddedTokens() {
		return embeddedTokens;
	}
	/**
	 * @param embeddedTokens the embeddedTokens to set
	 */
	public void setEmbeddedTokens(Text embeddedTokens) {
		this.embeddedTokens = embeddedTokens;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 53;
		int result = 1;
		result = prime * result
				+ ((embeddedTokens == null) ? 0 : embeddedTokens.hashCode());
		result = prime * result
				+ ((endTokens == null) ? 0 : endTokens.hashCode());
		result = prime * result + ((order == null) ? 0 : order.hashCode());
		result = prime * result + ((section == null) ? 0 : section.hashCode());
		result = prime * result
				+ ((startTokens == null) ? 0 : startTokens.hashCode());
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
		if (!(obj instanceof SectionBean))
			return false;
		final SectionBean other = (SectionBean) obj;
		if (embeddedTokens == null) {
			if (other.embeddedTokens != null)
				return false;
		} else if (!embeddedTokens.equals(other.embeddedTokens))
			return false;
		if (endTokens == null) {
			if (other.endTokens != null)
				return false;
		} else if (!endTokens.equals(other.endTokens))
			return false;
		if (order == null) {
			if (other.order != null)
				return false;
		} else if (!order.equals(other.order))
			return false;
		if (section == null) {
			if (other.section != null)
				return false;
		} else if (!section.equals(other.section))
			return false;
		if (startTokens == null) {
			if (other.startTokens != null)
				return false;
		} else if (!startTokens.equals(other.startTokens))
			return false;
		return true;
	}
	
	
}
