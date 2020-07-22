package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
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
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.instance.ComponentInstance;


public class MBASCostsModelView extends ApplicationWindow{

	// By default, all component-defense pairs are using the scaling factor cost 1
	private static class ComponentDefenseCost {
		String componentName;
		String defensePropName;

		int DAL;
		double cost;
		double scalingFactor;

		public ComponentDefenseCost(String name, String defense, int DAL, double cost) {
			this.componentName = name;
			this.defensePropName = defense;
			this.DAL = DAL;
			this.cost= cost;
		}

		public ComponentDefenseCost(String name, String defense, double scalingFactor) {
			this.componentName = name;
			this.defensePropName = defense;
			this.scalingFactor= scalingFactor;
		}
	}

	public List<ComponentDefenseCost> linearCosts = new ArrayList<>();
	public List<ComponentDefenseCost> individualCosts = new ArrayList<>();

	private Font font;
	private Font boldFont;

	public MBASCostsModelView(List<EObject> aadlObjs) {
		super(null);
		font = new Font(null, "Helvetica", 12, SWT.NORMAL);
		boldFont = new Font(null, "Helvetica", 12, SWT.BOLD);
		// This view is AADL project-based
		// How can we ensure this?
		// Read/write the data from/to the xml file
		// every time user opens/save the GUI.

		if(!aadlObjs.isEmpty()) {
			for(EObject obj : aadlObjs) {
				if(obj instanceof ComponentImplementation) {
					System.out.println("Implementation: " + ((ComponentImplementation) obj).getName());
				}
				if (obj instanceof ComponentInstance) {
					System.out.println("Instance: " + ((ComponentInstance) obj).getName());
				}
			}
		}
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
		shell.setText("Costs Model");
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

		Label componentLabel = new Label(composite, SWT.NONE);
		componentLabel.setText("Component: ");
		componentLabel.setFont(boldFont);

		// List all components

		Label propLabel = new Label(composite, SWT.NONE);
		propLabel.setText("Defense Property: ");
		propLabel.setFont(boldFont);

		// List all defense properties

		Label analysisLabel = new Label(composite, SWT.NONE);
		analysisLabel.setText("Model Based Architecture Analysis");
		analysisLabel.setFont(boldFont);

		Group selDeAllButtons = new Group(composite, SWT.NONE);
		selDeAllButtons.setLayout(new RowLayout(SWT.HORIZONTAL));
		selDeAllButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true, true, 1, 1));

		Composite closeButtons = new Composite(composite, SWT.NONE);
		closeButtons.setLayout(new RowLayout(SWT.HORIZONTAL));
		closeButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true, true, 1, 1));

		Button cancel = new Button(closeButtons, SWT.PUSH);
		cancel.setText("Cancel");
		cancel.setFont(font);

		Button save = new Button(closeButtons, SWT.PUSH);
		save.setText("Save Settings");
		save.setFont(font);

		// Set the preferred size
		Point bestSize = getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		getShell().setSize(bestSize);

		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				composite.getShell().close();
			}
		});

		save.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// TODO save
				composite.getShell().close();
			}
		});
		return composite;
	}

}
