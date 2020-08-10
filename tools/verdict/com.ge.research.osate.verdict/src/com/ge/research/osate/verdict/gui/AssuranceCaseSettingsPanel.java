package com.ge.research.osate.verdict.gui;

import java.util.List;

import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;



/**
* @author Saswata Paul
*/
public class AssuranceCaseSettingsPanel extends ApplicationWindow {
	// variable to control granularity of GSN
	public static String rootGoalId;
	//variable to decide if svgs should be shown in a new Tab
	public static boolean showInTab = true;
	//variable to decide if xml should be generated
	public static boolean generateXml = true ;
	//variable to decide if security cases should be generated
	public static boolean securityCases = false ;
	
	private Font font;
	private Font boldFont;

	public AssuranceCaseSettingsPanel() {
		super(null);
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
		shell.setText("Assurance Case Settings");
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

		Label acsLabel = new Label(composite, SWT.NONE);
		acsLabel.setText("Assurance Case Settings");
		acsLabel.setFont(boldFont);
        GridData gd1 = new GridData ();
        gd1.horizontalAlignment = SWT.CENTER;		
        acsLabel.setLayoutData(gd1);
		
		Group acsGroup = new Group(composite, SWT.NONE);
		acsGroup.setLayout(new GridLayout(1, false));
		

		//Field to accept requirement ID
	    Label idLabel = new Label(acsGroup, SWT.NULL);
	    idLabel.setText("Enter Requirement Ids Below:");
		idLabel.setFont(font);
	    Label idLabel2 = new Label(acsGroup, SWT.NULL);
	    idLabel2.setText("Usage: Id1;Id2;Id3;...IdN;");
        Text idField = new Text(acsGroup, SWT.BORDER | SWT.LEFT);
        GridData gd2 = new GridData ();
        gd2.widthHint = 300;
        idField.setLayoutData(gd2);
        if(rootGoalId!=null) {
            idField.setText(rootGoalId);        	
        }
	    
        //Button to save settings for xml
        Button securityCasesButton = new Button(acsGroup, SWT.CHECK);	
        securityCasesButton.setText("Generate Security Cases");	
        securityCasesButton.setFont(font);	
        securityCasesButton.setSelection(securityCases);
        
        //Button to save settings for xml
        Button xmlButton = new Button(acsGroup, SWT.CHECK);	
        xmlButton.setText("Generate XML Artifacts");	
        xmlButton.setFont(font);	
        xmlButton.setSelection(generateXml);

        //Button to save settings for showing in tab
        Button showFragButton = new Button(acsGroup, SWT.CHECK);	
		showFragButton.setText("Auto-Open GSN Graphs");	
		showFragButton.setFont(font);	
		showFragButton.setSelection(showInTab);
        
		//Close buttons
		Composite closeButtons = new Composite(composite, SWT.NONE);
		closeButtons.setLayout(new RowLayout(SWT.HORIZONTAL));
		closeButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true, true, 1, 1));	
		
		Button cancel = new Button(closeButtons, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.setFont(font);

		Button save = new Button(closeButtons, SWT.PUSH);
		save.setText("Save Settings");
		save.setFont(font);
		
		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				composite.getShell().close();
			}
		});
		
		
		save.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {

				if (idField.getText()!=null) {
					rootGoalId=idField.getText();
				} 
				securityCases = securityCasesButton.getSelection();
				showInTab = showFragButton.getSelection();
				generateXml = xmlButton.getSelection();
				
				composite.getShell().close();
			}
		});
		
		return composite;
	}
	
 

	/**
	 * Can be used for dynamically populating the settings panel
	 */
//	protected Control createContents(Composite parent) {
//		Composite composite = new Composite(parent, SWT.NONE);
//		composite.setLayout(new GridLayout(1, false));
//
//		Label analysisLabel = new Label(composite, SWT.NONE);
//		analysisLabel.setText("Select a requirement to generate GSN fragment:");
//		analysisLabel.setFont(boldFont);
//
//		Group selectionButtonGroup = new Group(composite, SWT.NONE);
//		selectionButtonGroup.setLayout(new RowLayout(SWT.HORIZONTAL));
//
//		//Creating a button and listener for each Goal Id		
//		for (String aGoalId: allGoalIds) {
//			Button missionButton = new Button(selectionButtonGroup, SWT.CHECK);
//			missionButton.setText(aGoalId);
//			missionButton.setFont(font);
//			missionButton.setSelection(false);
//			missionButton.addSelectionListener(new SelectionAdapter() {
//				@Override
//				public void widgetSelected(SelectionEvent event) {
//					if( missionButton.getSelection()) {
//						rootGoalId = aGoalId;
//						System.out.println("Selected "+rootGoalId+ " for generating GSN fragment.");
//						composite.getShell().close();
//					}
//				}
//			});			
//		}
//				
//		return composite;
//	}
}
