package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.osate.aadl2.DataPort;
import org.osate.aadl2.impl.DefaultAnnexSubclauseImpl;
import org.osate.aadl2.impl.SystemImplementationImpl;
import org.osate.aadl2.impl.SystemTypeImpl;

import com.ge.research.osate.verdict.dsl.verdict.Statement;
import com.ge.research.osate.verdict.dsl.verdict.Verdict;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractSubclause;

/**
*
* Author: Soumya Talukder
* Date: Jul 18, 2019
*
*/

//this class creates the Wizard dashboard
public class WzrdDashboard extends ApplicationWindow {
	private List<SystemTypeImpl> systems = new ArrayList<SystemTypeImpl>(); // stores all the SytemTypeImpl instances in the model
	private Shell lastShell; // the shell from where dashboard is invoked
	private Shell shell; // the dashboard shell
	private Resource resource; // contains the dynamically updated resource contents of the invoking .aadl file
	private IPath fileModel; // the invoking file path
	private Rectangle sourceRect; // the shell-size info of the invoking window
	private Boolean valid = true; // boolean denoting whether the launched dashboard session is valid
	private int colorTag = -1; // an integer denoting the type of a system (e.g. a component, a system etc)
	private Image[] images = new Image[3]; // stores the colour boxes used as colour legends in the dashboard

	public WzrdDashboard(Resource res, Shell osateShell, IPath fileModel) {
		super(osateShell);
		lastShell = osateShell;
		resource = res;
		this.fileModel = fileModel;
		images[0] = createImage(Display.getCurrent(), 242, 174, 39); // amber box
		images[1] = createImage(Display.getCurrent(), 158, 196, 155); // green box
		images[2] = createImage(Display.getCurrent(), 145, 169, 216); // blue box

		// extract the SystemTypeImpl instances in the current resource
		try {
			loadSystems(resource);
		} catch (Exception e) {
			e.printStackTrace();
		}
		// check if invoking model doesn't have any SystemTypeImpl instance
		if (systems.size() == 0) {
			valid = false;
			MessageDialog.openError(lastShell, "VERDICT Wizard Launcher",
					"No system/component (SystemTypeImpl.class) present in the selected .aadl file. Aborting Wizard.");
		}
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Wizard: Dashboard ");
		// set location of the dashboard wrt the parent window-shell------------
		sourceRect = lastShell.getBounds();
		double x = (sourceRect.width - 1000) * 0.5;
		double y = (sourceRect.height - 300) * 0.5;
		shell.setLocation((int) x, (int) y);
		// ----------------------------------------------------------------------
		this.shell = shell;
	}

	public void run() {
		setShellStyle(SWT.TITLE | SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.MAX | SWT.RESIZE); // setting properties of the dashboard shell
		setBlockOnOpen(true);
		open();
	}

	@Override
	protected Control createContents(Composite parent) {
		try {
			loadSystems(resource); // re-extract SystemTypeImpl instances from the current (possibly modified) resource
		} catch (Exception e) {
			e.printStackTrace();
		}

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(4, false));

		Composite childComposite = new Composite(composite, SWT.NONE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.horizontalSpan = 4;
		childComposite.setLayout(new GridLayout(1, false));
		childComposite.setLayoutData(gridData);

		// the following clabels define the color legends appearing at the top of dashboard
		CLabel labelFirstLegend = new CLabel(childComposite, SWT.NONE);
		labelFirstLegend.setText("Component with no Cyber-relation Defined");
		labelFirstLegend.setImage(images[0]);
		CLabel labelSecondLegend = new CLabel(childComposite, SWT.NONE);
		labelSecondLegend.setText("Component with One or More Cyber-relation(s) Defined");
		labelSecondLegend.setImage(images[1]);
		CLabel labelThirdLegend = new CLabel(childComposite, SWT.NONE);
		labelThirdLegend.setText("Top-Level System");
		labelThirdLegend.setImage(images[2]);

		// creates a button for each SystemTypeImpl instance in the invoking .aadl file
		for (int i = 0; i < systems.size(); i++) {
			Button aButton = new Button(composite, SWT.PUSH);
			aButton.setText(systems.get(i).getFullName());
			aButton.setToolTipText(composeToolTip(i));
			if (checkIfSystem(systems.get(i))) { // system with a corresponding .impl in invoking .aadl
				aButton.setBackground(new Color(Display.getCurrent(), 145, 169, 216));
			} else {
				switch (colorTag) {
				case 0: // component with cyber-relation defined
					aButton.setBackground(new Color(Display.getCurrent(), 158, 196, 155));
					break;
				case 1: // a spare legend, currently not used
					aButton.setBackground(new Color(Display.getCurrent(), 247, 135, 135));
					break;
				case 2: // component with no cyber-relation defined
					aButton.setBackground(new Color(Display.getCurrent(), 242, 174, 39));
					break;
				}
			}
			colorTag = -1;

			// this listener performs necessary activities when a button is selected
			aButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					for (int j = 0; j < systems.size(); j++) {
						if (systems.get(j).getFullName().equals(aButton.getText())) {
							List<EObject> objs = systems.get(j).eContents();
							for (int k = 0; k < objs.size(); k++) {
								if (objs.get(k) instanceof DefaultAnnexSubclauseImpl) {
									if (!((DefaultAnnexSubclauseImpl) objs.get(k)).getName().equals("verdict")) {
										continue;
									}
									break;
								}
							}
							// invoking cyber-property editor corresponding to the button

							StatementEditor editor = new StatementEditor(systems.get(j), fileModel, shell, sourceRect,
									"dashboard");

							// run the editor if the invoked editor instance is valid
							if (editor.isValid()) {
								composite.setEnabled(false);
								editor.run();
								composite.setEnabled(true);
							}
							try {
								loadSystems(resource); // re-extract SystemTypeImpl instances from the current (possibly modified) resource
							} catch (Exception e) {
								e.printStackTrace();
							}
							if (!aButton.isDisposed()) {
								aButton.setToolTipText(composeToolTip(j)); // update the toolTipText of the button
								// save the .aadl editor if any unsaved content exists-------------------
								IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
										.getActivePage();
								IEditorPart openEditor = page.getActiveEditor();
								if (openEditor != null) {
									page.saveEditor(openEditor, false);
								}
								// -----------------------------------------------------------------------

								// update button colour
								if (checkIfSystem(systems.get(j))) {
									aButton.setBackground(new Color(Display.getCurrent(), 145, 169, 216)); // system with a corresponding .impl in invoking
																											// .aadl
								} else {
									switch (colorTag) {
									case 0:
										aButton.setBackground(new Color(Display.getCurrent(), 158, 196, 155)); // component with cyber-relation defined
										break;
									case 1:
										aButton.setBackground(new Color(Display.getCurrent(), 247, 135, 135)); // a spare legend, currently not used
										break;
									case 2:
										aButton.setBackground(new Color(Display.getCurrent(), 242, 174, 39)); // component with no cyber-relation defined
										break;
									}
								}
							}
						}
					}
				}
			});
		}
		return composite;
	}

	// Dynamically updates tooltiptext of the buttons
	private String composeToolTip(int i) {
		String str = "";
		SystemTypeImpl sys = systems.get(i);
		List<DataPort> dpList = sys.getOwnedDataPorts();
		str = str + "IN ports:\n";
		String inLine = "";
		// wrap the string into multi-line if its lengthy-------------------------
		int inLength = 0;
		for (int j = 0; j < dpList.size(); j++) {
			if (dpList.get(j).isIn()) {
				inLength = inLength + (dpList.get(j).getFullName() + ",").length();
				inLine = inLine + dpList.get(j).getFullName() + ",";
				if (inLength > 30) {
					inLength = 0;
					inLine = inLine + "\n";
				}
			}
		}
		// -------------------------------------------------------------------------
		str = str + inLine + "\n\n";

		str = str + "OUT ports:\n";
		String outLine = "";
		int outLength = 0;
//		wrap the string into multi-line if its lengthy-------------------------
		int outCount = 0;
		for (int j = 0; j < dpList.size(); j++) {
			if (dpList.get(j).isOut()) {
				outCount++;
				outLength = outLength + (dpList.get(j).getFullName() + ",").length();
				outLine = outLine + dpList.get(j).getFullName() + ",";
				if (outLength > 30) {
					outLength = 0;
					outLine = outLine + "\n";
				}
			}
		}
		// ------------------------------------------------------------------------

		List<EObject> objs = sys.eContents();
		List<Statement> stmts = new ArrayList<Statement>();
		for (int k = 0; k < objs.size(); k++) {
			if (objs.get(k) instanceof DefaultAnnexSubclauseImpl) {
				if (!((DefaultAnnexSubclauseImpl) objs.get(k)).getName().equals("verdict")) {
					continue;
				}
				Verdict vd = ((VerdictContractSubclause) ((DefaultAnnexSubclauseImpl) objs.get(k))
						.getParsedAnnexSubclause()).getContract();
				stmts = vd.getElements();
				break;
			}
		}

		str = str + outLine + "\n\n";
		if (!checkIfSystem(sys)) {
			if (stmts.size() > 0) {
				str = str + "Cyber-relations:\n";
				if (stmts.size() == outCount) {
					colorTag = 0;
				} else {
					colorTag = 0;
				}
			} else {
				str = str + "No cyber-relation defined.";
				colorTag = 2;
			}
		} else {
			if (stmts.size() > 0) {
				str = str + "Cyber-requirements:\n";
			} else {
				str = str + "No cyber-requirement defined.";
			}
		}

		for (int m = 0; m < stmts.size(); m++) {
			str = str + stmts.get(m).getId() + "\n";
		}
		return str;
	}

	// reloads .aadl resource after modification of script content by Wizard
	private void loadSystems(Resource oldResource) throws Exception {
		ResourceSetImpl resourceSet = new ResourceSetImpl();
		resourceSet.getResourceFactoryRegistry();
		Resource resource = resourceSet
				.createResource(oldResource.getURI());
		resource.load(null);
		systems.clear();
		TreeIterator<EObject> tree = resource.getAllContents();
		while (tree.hasNext()) {
			EObject anObject = tree.next();
			if (anObject instanceof SystemTypeImpl) {
				systems.add((SystemTypeImpl) anObject);
			}
		}
	}

	// check if component corr. to selected button is "top-level" (i.e. invoking .aadl contains a corresponding .impl element)
	private boolean checkIfSystem(SystemTypeImpl sys) {

		boolean sysLevel = false;
		String systemName;

		TreeIterator<EObject> tree = sys.eResource().getAllContents();

		while (tree.hasNext()) {
			EObject tmp = tree.next();

			if (tmp instanceof SystemImplementationImpl) {
				systemName = ((SystemImplementationImpl) tmp).getTypeName();
				if (sys.getName().equals(systemName)) {
					sysLevel = true;
					break;
				}
			}
		}
		return sysLevel;
	}

	public Boolean isValid() {
		return valid;
	}

	// creates box for colour legends in dashboard
	private static Image createImage(Display display, int red, int green, int blue) {
		Color color = new Color(display, red, green, blue);
		Image image = new Image(display, 10, 10);
		GC gc = new GC(image);
		gc.setBackground(color);
		gc.fillRectangle(0, 0, 10, 10);
		gc.dispose();
		return image;
	}
}