package com.ge.research.osate.verdict.gui;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import verdict.vdm.vdm_model.Model;
import verdict.vdm.vdm_model.Mission;
import verdict.vdm.vdm_model.CyberReq;
import verdict.vdm.vdm_model.SafetyReq;


/**
* Let user enable cyber or safety relations inference when running MBAA.
*/
public class GSNSettingsPanel extends ApplicationWindow {
	/**
	 * variables to control granularity og GSN
	 * missionFragments -  mission requirements
	 * cyberFragments -  cyber requirements 
	 * cyberFragments -  safety requirements
	 * allFragments - all requirements   
	 */
	public static String rootGoalId;
	public static Model theVDMModel;

	
	private Font font;
	private Font boldFont;

	public GSNSettingsPanel(Model m) {
		super(null);
		
		//set the Model object as theVDMModel 
		theVDMModel = m;

		font = new Font(null, "Helvetica", 11, SWT.NORMAL);
		boldFont = new Font(null, "Helvetica", 11, SWT.BOLD);
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		Display display = shell.getDisplay();
		Monitor primary = display.getPrimaryMonitor();
		Rectangle bounds = primary.getBounds();
		Rectangle rect = shell.getBounds();

		int x = bounds.x + (bounds.width - rect.width) / 2;
		int y = bounds.y + (bounds.height - rect.height) / 2;

		shell.setLocation(x, y);
		shell.setText("GSN Settings");
		shell.setFont(font);
	}

	public void run() {
		setBlockOnOpen(true);
		open();
	}

	public void bringToFront(Shell shell) {
		shell.setActive();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		Label analysisLabel = new Label(composite, SWT.NONE);
		analysisLabel.setText("Select a requirement to generate GSN fragment:");
		analysisLabel.setFont(boldFont);

		Group selectionButtonGroup = new Group(composite, SWT.NONE);
		selectionButtonGroup.setLayout(new RowLayout(SWT.HORIZONTAL));

		
		
		for (Mission aMission: theVDMModel.getMission()) {
			Button missionButton = new Button(selectionButtonGroup, SWT.CHECK);
			missionButton.setText(aMission.getId());
			missionButton.setFont(font);
			missionButton.setSelection(false);
			missionButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if( missionButton.getSelection()) {
						rootGoalId = aMission.getId();
						System.out.println("Selected "+rootGoalId+ " to generate GSN fragment.");
						composite.getShell().close();
					}
				}
			});			
		}
		

		for (CyberReq aCyberReq: theVDMModel.getCyberReq()) {
			Button reqButton = new Button(selectionButtonGroup, SWT.CHECK);
			reqButton.setText(aCyberReq.getId());
			reqButton.setFont(font);
			reqButton.setSelection(false);
			reqButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if( reqButton.getSelection()) {
						rootGoalId = aCyberReq.getId();
						System.out.println("Selected "+rootGoalId+ " to generate GSN fragment.");
						composite.getShell().close();
					}
				}
			});			
		}
		

		for (SafetyReq aSafetyReq: theVDMModel.getSafetyReq()) {
			Button reqButton = new Button(selectionButtonGroup, SWT.CHECK);
			reqButton.setText(aSafetyReq.getId());
			reqButton.setFont(font);
			reqButton.setSelection(false);
			reqButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if( reqButton.getSelection()) {
						rootGoalId = aSafetyReq.getId();
						System.out.println("Selected "+rootGoalId+ " to generate GSN fragment.");
						composite.getShell().close();
					}
				}
			});			
		}
		
		return composite;
	}
}
