package fna.beans;
/**
 * @ author Partha 
 * */
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Text;

public class TextBean {
	
	private Text firstPara;
	private Text leadingIndentation;
	private Text spacing;
	private Text estimatedLength;
	private Text pageNumberFormsText;
	private Button sectionHeadingsCapButton;
	private Button sectionHeadingsAllCapButton;
	private Text sectionHeadingsText;
	private SpecialBean footerHeaderBean;

	public TextBean(Text firstPara, Text leadingIndentation, Text spacing,
			Text estimatedLength, Text pageNumberFormsText,
			Button sectionHeadingsCapButton,
			Button sectionHeadingsAllCapButton, Text sectionHeadingsText,
			SpecialBean footerHeaderBean) {
		super();
		this.firstPara = firstPara;
		this.leadingIndentation = leadingIndentation;
		this.spacing = spacing;
		this.estimatedLength = estimatedLength;
		this.pageNumberFormsText = pageNumberFormsText;
		this.sectionHeadingsCapButton = sectionHeadingsCapButton;
		this.sectionHeadingsAllCapButton = sectionHeadingsAllCapButton;
		this.sectionHeadingsText = sectionHeadingsText;
		this.footerHeaderBean = footerHeaderBean;
	}
	public Text getFirstPara() {
		return firstPara;
	}
	public Text getLeadingIndentation() {
		return leadingIndentation;
	}
	public Text getSpacing() {
		return spacing;
	}
	public Text getEstimatedLength() {
		return estimatedLength;
	}
	public Text getPageNumberFormsText() {
		return pageNumberFormsText;
	}
	public Button getSectionHeadingsCapButton() {
		return sectionHeadingsCapButton;
	}
	public Button getSectionHeadingsAllCapButton() {
		return sectionHeadingsAllCapButton;
	}
	public Text getSectionHeadingsText() {
		return sectionHeadingsText;
	}
	public SpecialBean getFooterHeaderBean() {
		return footerHeaderBean;
	}
	/**
	 * @param firstPara the firstPara to set
	 */
	public void setFirstPara(Text firstPara) {
		this.firstPara = firstPara;
	}
	/**
	 * @param leadingIndentation the leadingIndentation to set
	 */
	public void setLeadingIndentation(Text leadingIndentation) {
		this.leadingIndentation = leadingIndentation;
	}
	/**
	 * @param spacing the spacing to set
	 */
	public void setSpacing(Text spacing) {
		this.spacing = spacing;
	}
	/**
	 * @param estimatedLength the estimatedLength to set
	 */
	public void setEstimatedLength(Text estimatedLength) {
		this.estimatedLength = estimatedLength;
	}
	/**
	 * @param pageNumberFormsText the pageNumberFormsText to set
	 */
	public void setPageNumberFormsText(Text pageNumberFormsText) {
		this.pageNumberFormsText = pageNumberFormsText;
	}
	/**
	 * @param sectionHeadingsCapButton the sectionHeadingsCapButton to set
	 */
	public void setSectionHeadingsCapButton(Button sectionHeadingsCapButton) {
		this.sectionHeadingsCapButton = sectionHeadingsCapButton;
	}
	/**
	 * @param sectionHeadingsAllCapButton the sectionHeadingsAllCapButton to set
	 */
	public void setSectionHeadingsAllCapButton(Button sectionHeadingsAllCapButton) {
		this.sectionHeadingsAllCapButton = sectionHeadingsAllCapButton;
	}
	/**
	 * @param sectionHeadingsText the sectionHeadingsText to set
	 */
	public void setSectionHeadingsText(Text sectionHeadingsText) {
		this.sectionHeadingsText = sectionHeadingsText;
	}
	/**
	 * @param footerHeaderBean the footerHeaderBean to set
	 */
	public void setFooterHeaderBean(SpecialBean footerHeaderBean) {
		this.footerHeaderBean = footerHeaderBean;
	}
	
}
