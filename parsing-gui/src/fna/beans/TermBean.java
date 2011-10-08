package fna.beans;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.swtdesigner.SWTResourceManager;

import fna.parsing.ApplicationUtilities;
import fna.parsing.MainForm;
import fna.parsing.Registry;
import fna.parsing.character.GraphNode;
import fna.parsing.character.ManipulateGraphML;

/** This bean will hold a term and the delete button 
 * It has the additional capability of shuffling between deleted terms 
 * and parent terms group
 * @author Partha Pratim Sanyal
 *
 */
public class TermBean {
	private Text termText;
	private Label delete;
	private boolean togglePosition;
	private Group termGroup;
	private Group parentGroup;	
	private Group deletedGroup;
	/* Coordinates for the Text inside any Terms group */
	private static Rectangle textCood = new Rectangle(10, 10, 100, 20);
	/* Coordinates of the Cross Label inside the Term group*/
	private static Rectangle delCood = new Rectangle(110, 10, 20, 20);
	private static Color color = new Color(Display.getCurrent(), 184,244,166);



	public TermBean(Group termGroup, Group deletedGroup, boolean toggleGroup, String text) {
		termText = new Text(termGroup, SWT.BORDER);
		termText.setBackground(color);
		termText.setBounds(textCood);
		termText.setEditable(false);
		termText.setText(text);
		termText.setToolTipText(text);
		
		delete = new Label(termGroup, SWT.NONE);
		delete.setImage(SWTResourceManager.getImage(TermBean.class, "/fna/parsing/remove.jpg"));
		delete.setBounds(delCood);
		delete.setToolTipText("Click to delete this term");
		delete.addMouseListener(new MouseListener() {
			public void mouseDown(MouseEvent me){
				changeParentGroup();
			}
			public void mouseUp(MouseEvent me){	}
			public void mouseDoubleClick(MouseEvent me){	}
		});
		
		this.togglePosition = toggleGroup;
		this.termGroup = termGroup;
		this.parentGroup = (Group)termGroup.getParent();			
		this.deletedGroup = deletedGroup;
		
	}

	private void changeParentGroup() {

		if(togglePosition) {

			boolean canDelete = isCooccurredTermPresent();
			
			//if (canDelete) {
				togglePosition = false;
				Rectangle rect = termGroup.getBounds();
				if (rect.x == 40) {
					rect.x = 10;
				} else {
					rect.x = 160;
				}
				
				termGroup.setParent(deletedGroup);
				termGroup.setBounds(rect);
				((ScrolledComposite)deletedGroup.getParent()).setMinSize(deletedGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				
				/* Delete the edge between the term nodes from the graph*/
				CharacterGroupBean characterBean = MainForm.getGroupInfo().get(MainForm.groupsCombo.getText());
				ArrayList <CoOccurrenceBean> cooccurrences = characterBean.getCooccurrences();
				for (CoOccurrenceBean cbean : cooccurrences) {
					if (cbean.getTerm1() != null  && cbean.getTerm2() != null) {
						String term1 = cbean.getTerm1().getTermText().getText();
						String term2 = cbean.getTerm2().getTermText().getText();
						if (termText.equals(cbean.getTerm1().getTermText()) || termText.equals(cbean.getTerm2().getTermText())) {
							String groupPath = Registry.TargetDirectory+
							ApplicationUtilities.getProperty("CHARACTER-STATES")+ "\\"
							+ characterBean.getGroupName() + ".xml";
							ManipulateGraphML.removeEdge(new GraphNode(term1), new GraphNode(term2), groupPath, characterBean.getGroupName());
							if(!canDelete) {
								/* If the terms paired is not present in the group then
								 *  make the context and frequency label invisible*/
								cbean.getFrequency().setVisible(false);
								cbean.getContextButton().setVisible(false);
								if(cbean.getText()!=null) cbean.getText().setVisible(false);
							}
							
							break;
						}
					}
					/* Visibility problem for remaining terms - radio and frequency label shouldn't be visible*/
					if(cbean.getTerm1() == null 
							&& cbean.getTerm2().termGroup.getParent() != parentGroup
							&& cbean.getTerm2().getTermText().equals(termText)){
						cbean.getFrequency().setVisible(false);
						cbean.getContextButton().setVisible(false);
						if(cbean.getText()!=null) cbean.getText().setVisible(false);
						break;
					} else if (cbean.getTerm2() == null 
							&& cbean.getTerm1().termGroup.getParent() != parentGroup
							&& cbean.getTerm1().getTermText().equals(termText)){
						cbean.getFrequency().setVisible(false);
						cbean.getContextButton().setVisible(false);
						if(cbean.getText()!=null) cbean.getText().setVisible(false);
						break;
					}

				}
				
/*			} else {
				ApplicationUtilities.showPopUpWindow(								
						"One of the terms in the pair was already deleted. " +
						"Hence, it is not possible to delete the term you have clicked.", 
						"Deletion not possible!",SWT.ICON_ERROR);
			}*/


		} else {
			togglePosition = true;
			Rectangle rect = termGroup.getBounds();
			
			if (rect.x == 10) {
				rect.x = 40;
			} else {
				rect.x = 210;
			}
			termGroup.setParent(parentGroup);
			termGroup.setBounds(rect);
			((ScrolledComposite)parentGroup.getParent()).setMinSize(parentGroup.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			
			/* Restore the edge between the term nodes in the graph*/
			CharacterGroupBean characterBean = MainForm.getGroupInfo().get(MainForm.groupsCombo.getText());
			ArrayList <CoOccurrenceBean> cooccurrences = characterBean.getCooccurrences();
			for (CoOccurrenceBean cbean : cooccurrences) {
				
				if(cbean.getTerm1() != null && cbean.getTerm2() != null) {
					String term1 = cbean.getTerm1().getTermText().getText();
					String term2 = cbean.getTerm2().getTermText().getText();
					if (termText.equals(cbean.getTerm1().getTermText()) || termText.equals(cbean.getTerm2().getTermText())) {
						
						/* Need to check if the two terms are in the same parent group before restoring a link between them*/
						if (isCooccurredTermPresent()) {
							String groupName = Registry.TargetDirectory+
							ApplicationUtilities.getProperty("CHARACTER-STATES")+ "\\"
							+ characterBean.getGroupName() + ".xml";
							ManipulateGraphML.insertEdge(new GraphNode(term1), new GraphNode(term2), groupName);
						}
						
						/* Make the context button and the frequency label visible again*/
						cbean.getContextButton().setVisible(true);
						cbean.getFrequency().setVisible(true);
						if(cbean.getText()!=null) cbean.getText().setVisible(true);						
						break;
					}
				}				

				/* Visibility problem for remaining terms - radio and frequency label shouldn't be visible*/
				if(cbean.getTerm1() == null 
						&& cbean.getTerm2().termGroup.getParent() == parentGroup
						&& cbean.getTerm2().getTermText().equals(termText)){
					cbean.getFrequency().setVisible(true);
					cbean.getContextButton().setVisible(true);
					if(cbean.getText()!=null) cbean.getText().setVisible(true);
					break;
				} else if (cbean.getTerm2() == null 
						&& cbean.getTerm1().termGroup.getParent() == parentGroup
						&& cbean.getTerm1().getTermText().equals(termText)) {
					cbean.getFrequency().setVisible(true);
					cbean.getContextButton().setVisible(true);
					if(cbean.getText()!=null) cbean.getText().setVisible(true);
					break;
				}
			}
			
		}
	}

	private boolean isCooccurredTermPresent(){
		/* Checking whether the term and its co-occurred term are present in the same parent group */
		Control [] controls = parentGroup.getChildren();
		Point point = null;
		boolean canDelete = false;
		for (Control control : controls) {
			point = control.getLocation();
			if (!point.equals(termGroup.getLocation())) {
				if (point.y == termGroup.getLocation().y) {						
					canDelete = true ;						
					break;
				} 
			}
				
		}
		return canDelete;
	}
	/**
	 * @return the termGroup
	 */
	public Group getTermGroup() {
		return termGroup;
	}

	/**
	 * @param termGroup the termGroup to set
	 */
	public void setTermGroup(Group termGroup) {
		this.termGroup = termGroup;
	}

	/**
	 * @return the togglePosition
	 */
	public boolean isTogglePosition() {
		return togglePosition;
	}

	/**
	 * @param togglePosition the togglePosition to set
	 */
	public void setTogglePosition(boolean togglePosition) {
		this.togglePosition = togglePosition;
	}

	/**
	 * @return the parentGroup
	 */
	public Group getParentGroup() {
		return parentGroup;
	}

	/**
	 * @param parentGroup the parentGroup to set
	 */
	public void setParentGroup(Group parentGroup) {
		this.parentGroup = parentGroup;
	}

	/**
	 * @return the deletedGroup
	 */
	public Group getDeletedGroup() {
		return deletedGroup;
	}

	/**
	 * @param deletedGroup the deletedGroup to set
	 */
	public void setDeletedGroup(Group deletedGroup) {
		this.deletedGroup = deletedGroup;
	}

	/**
	 * @return the termText
	 */
	public Text getTermText() {
		return termText;
	}

	/**
	 * @param termText the termText to set
	 */
	public void setTermText(Text termText) {
		this.termText = termText;
	}

	
}
