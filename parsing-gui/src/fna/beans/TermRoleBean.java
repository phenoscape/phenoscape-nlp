/**
 * This bean is used to hold terms and roles in the 3rd tab named "Others" under markup tab
 */
package fna.beans;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;

/**
 * @author Partha Pratim Sanyal
 *
 */
public class TermRoleBean {
	private Label termLabel;
	private Button roleCombo;
	public Label getTermLabel() {
		return termLabel;
	}
	public void setTermLabel(Label termLabel) {
		this.termLabel = termLabel;
	}
	public Button getRoleCombo() {
		return roleCombo;
	}
	public void setRoleCombo(Button roleCombo) {
		this.roleCombo = roleCombo;
	}
	public TermRoleBean(Label termLabel, Button roleCombo) {
		super();
		this.termLabel = termLabel;
		this.roleCombo = roleCombo;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((roleCombo == null) ? 0 : roleCombo.hashCode());
		result = prime * result
				+ ((termLabel == null) ? 0 : termLabel.hashCode());
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
		if (!(obj instanceof TermRoleBean))
			return false;
		final TermRoleBean other = (TermRoleBean) obj;
		if (roleCombo == null) {
			if (other.roleCombo != null)
				return false;
		} else if (!roleCombo.equals(other.roleCombo))
			return false;
		if (termLabel == null) {
			if (other.termLabel != null)
				return false;
		} else if (!termLabel.equals(other.termLabel))
			return false;
		return true;
	}
}
