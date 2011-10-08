package fna.beans;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

public class NomenclatureBean {

	/**
	 * @param args
	 */
	
	private Group parent;
	private Button yesRadioButton;
	private Button noRadioButton;
	private Text description;
	private Label label;
	

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result
				+ ((noRadioButton == null) ? 0 : noRadioButton.hashCode());
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result
				+ ((yesRadioButton == null) ? 0 : yesRadioButton.hashCode());
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
		if (!(obj instanceof NomenclatureBean))
			return false;
		final NomenclatureBean other = (NomenclatureBean) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (label == null) {
			if (other.label != null)
				return false;
		} else if (!label.equals(other.label))
			return false;
		if (noRadioButton == null) {
			if (other.noRadioButton != null)
				return false;
		} else if (!noRadioButton.equals(other.noRadioButton))
			return false;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (yesRadioButton == null) {
			if (other.yesRadioButton != null)
				return false;
		} else if (!yesRadioButton.equals(other.yesRadioButton))
			return false;
		return true;
	}

	public NomenclatureBean(Group parent, Button yesRadioButton,
			Button noRadioButton, Text description, Label label) {
		super();
		this.parent = parent;
		this.yesRadioButton = yesRadioButton;
		this.noRadioButton = noRadioButton;
		this.description = description;
		this.label = label;
	}

	public Label getLabel() {
		return label;
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public Group getParent() {
		return parent;
	}


	public Button getYesRadioButton() {
		return yesRadioButton;
	}


	public Button getNoRadioButton() {
		return noRadioButton;
	}

	public Text getDescription() {
		return description;
	}



}
