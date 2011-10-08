package fna.beans;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;

public class SpecialBean {
	private Button firstButton;
	private Button secondButton;
	private Text firstText;
	private Text secondText;
	public SpecialBean(Button firstButton, Button secondButton, Text firstText,
			Text secondText) {
		super();
		this.firstButton = firstButton;
		this.secondButton = secondButton;
		this.firstText = firstText;
		this.secondText = secondText;
	}
	/**
	 * @return the firstButton
	 */
	public Button getFirstButton() {
		return firstButton;
	}
	/**
	 * @return the secondButton
	 */
	public Button getSecondButton() {
		return secondButton;
	}
	/**
	 * @return the firstText
	 */
	public Text getFirstText() {
		return firstText;
	}
	/**
	 * @return the secondText
	 */
	public Text getSecondText() {
		return secondText;
	}
	/**
	 * @param firstButton the firstButton to set
	 */
	public void setFirstButton(Button firstButton) {
		this.firstButton = firstButton;
	}
	/**
	 * @param secondButton the secondButton to set
	 */
	public void setSecondButton(Button secondButton) {
		this.secondButton = secondButton;
	}
	/**
	 * @param firstText the firstText to set
	 */
	public void setFirstText(Text firstText) {
		this.firstText = firstText;
	}
	/**
	 * @param secondText the secondText to set
	 */
	public void setSecondText(Text secondText) {
		this.secondText = secondText;
	}


	
}
