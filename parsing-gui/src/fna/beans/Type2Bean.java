package fna.beans;

import java.util.HashMap;

import org.eclipse.swt.widgets.Text;

public class Type2Bean {
	private TextBean textBean;
	private HashMap <Integer, NomenclatureBean> nomenclatures; 
	private HashMap <Integer, ExpressionBean> expressions;
	private DescriptionBean descriptionBean; 
	private SpecialBean special;
	private HashMap <String, Text> abbreviations;
	
	/**
	 * @return the textBean
	 */
	public TextBean getTextBean() {
		return textBean;
	}

	/**
	 * @param textBean the textBean to set
	 */
	public void setTextBean(TextBean textBean) {
		this.textBean = textBean;
	}

	/**
	 * @return the nomenclatures
	 */
	public HashMap<Integer, NomenclatureBean> getNomenclatures() {
		return nomenclatures;
	}

	/**
	 * @param nomenclatures the nomenclatures to set
	 */
	public void setNomenclatures(HashMap<Integer, NomenclatureBean> nomenclatures) {
		this.nomenclatures = nomenclatures;
	}

	/**
	 * @return the expressions
	 */
	public HashMap<Integer, ExpressionBean> getExpressions() {
		return expressions;
	}

	/**
	 * @param expressions the expressions to set
	 */
	public void setExpressions(HashMap<Integer, ExpressionBean> expressions) {
		this.expressions = expressions;
	}

	/**
	 * @return the descriptionBean
	 */
	public DescriptionBean getDescriptionBean() {
		return descriptionBean;
	}

	/**
	 * @param descriptionBean the descriptionBean to set
	 */
	public void setDescriptionBean(DescriptionBean descriptionBean) {
		this.descriptionBean = descriptionBean;
	}

	/**
	 * @return the special
	 */
	public SpecialBean getSpecial() {
		return special;
	}

	/**
	 * @param special the special to set
	 */
	public void setSpecial(SpecialBean special) {
		this.special = special;
	}

	/**
	 * @return the abbreviations
	 */
	public HashMap<String, Text> getAbbreviations() {
		return abbreviations;
	}

	/**
	 * @param abbreviations the abbreviations to set
	 */
	public void setAbbreviations(HashMap<String, Text> abbreviations) {
		this.abbreviations = abbreviations;
	}

	public Type2Bean(TextBean textBean,
			HashMap<Integer, NomenclatureBean> nomenclatures,
			HashMap<Integer, ExpressionBean> expressions,
			DescriptionBean descriptionBean, SpecialBean special,
			HashMap<String, Text> abbreviations) {
		this.textBean = textBean;
		this.nomenclatures = nomenclatures;
		this.expressions = expressions;
		this.descriptionBean = descriptionBean;
		this.special = special;
		this.abbreviations = abbreviations;
	}

}
