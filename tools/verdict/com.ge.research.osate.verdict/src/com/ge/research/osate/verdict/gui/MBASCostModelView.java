package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.osate.aadl2.AadlInteger;
import org.osate.aadl2.ComponentImplementation;
import org.osate.aadl2.DataImplementation;
import org.osate.aadl2.Property;
import org.osate.aadl2.Subcomponent;

public class MBASCostModelView extends ApplicationWindow{
	private Font font;
	private Font boldFont;

	private File costModelFile;
	private SynthesisCostModel costModel;

	private Image deleteImage;

	private Composite composite;
	private Table table;
	private TableViewer tableViewer;

	private final List<String> suggComponents;
	private final Map<String, Integer> suggComponentsIndexMap;
	private final List<String> suggDefenseProps;
	private final Map<String, Integer> suggDefensePropsIndexMap;
	private final List<String> suggDals;
	private final Map<String, Integer> suggDalsIndexMap;

	public MBASCostModelView(List<EObject> aadlObjs, File costModelFile) {
		super(null);

		deleteImage = ResourceLocator.imageDescriptorFromBundle("com.ge.research.osate.verdict", "icons/false.png")
				.get().createImage();

		this.costModelFile = costModelFile;
		costModel = new SynthesisCostModel();
		if (costModelFile.exists()) {
			costModel.loadFileXml(costModelFile);
		}

		font = new Font(null, "Helvetica", 12, SWT.NORMAL);
		boldFont = new Font(null, "Helvetica", 12, SWT.BOLD);
		// This view is AADL project-based
		// How can we ensure this?
		// Read/write the data from/to the xml file
		// every time user opens/save the GUI.

		suggComponents = new ArrayList<>();
		suggDefenseProps = new ArrayList<>();
		suggDals = new ArrayList<>();

		List<ComponentImplementation> impls = new ArrayList<>();
		List<Subcomponent> comps = new ArrayList<>();

		Set<String> defensePropNames = new HashSet<>();

		for (EObject obj : aadlObjs) {
			// be careful not to pick up data implementations
			if (obj instanceof ComponentImplementation && !(obj instanceof DataImplementation)) {
				ComponentImplementation impl = (ComponentImplementation) obj;
				impls.add(impl);
				for (Subcomponent comp : impl.getAllSubcomponents()) {
					comps.add(comp);
				}
			} else if (obj instanceof Property) {
				Property prop = (Property) obj;
				boolean rightMetaclass = prop.getAppliesToMetaclasses().stream().anyMatch(metaclass -> {
					String name = metaclass.getMetaclass().getName().toLowerCase();
					return "system".equals(name) || "connection".contentEquals(name);
				});
				boolean isInteger = prop.getPropertyType() instanceof AadlInteger;
				// we could further check if the range is 0..9
				if (rightMetaclass && isInteger) {
					defensePropNames.add(prop.getName());
				}
			}
		}

		Set<String> systemNames = new HashSet<>();
		systemNames.addAll(impls.stream().map(ComponentImplementation::getName).collect(Collectors.toList()));
		systemNames.addAll(comps.stream().map(Subcomponent::getName).collect(Collectors.toList()));

		for (String system : systemNames) {
			suggComponents.add(system);
		}
		suggComponents.sort(null);
		suggComponents.add(0, SynthesisCostModel.COMPONENT_ALL);

		for (String defenseProp : defensePropNames) {
			suggDefenseProps.add(defenseProp);
		}
		suggDefenseProps.sort(null);
		suggDefenseProps.add(0, SynthesisCostModel.DEFENSE_PROP_ALL);

		for (int dal = 0; dal < 10; dal++) {
			suggDals.add(Integer.toString(dal));
		}
		suggDals.sort(null);
		suggDals.add(0, SynthesisCostModel.DAL_LINEAR);

		// We want to be able to check contains in constant time
		suggComponentsIndexMap = new HashMap<>();
		for (int i = 0; i < suggComponents.size(); i++) {
			suggComponentsIndexMap.put(suggComponents.get(i), i);
		}
		suggDefensePropsIndexMap = new HashMap<>();
		for (int i = 0; i < suggDefenseProps.size(); i++) {
			suggDefensePropsIndexMap.put(suggDefenseProps.get(i), i);
		}
		suggDalsIndexMap = new HashMap<>();
		for (int i = 0; i < suggDals.size(); i++) {
			suggDalsIndexMap.put(suggDals.get(i), i);
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
		composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		String allComps = suggComponents.stream().filter(name -> !SynthesisCostModel.COMPONENT_ALL.equals(name))
				.collect(Collectors.joining(", "));
		String allDefenseProps = suggDefenseProps.stream()
				.filter(name -> !SynthesisCostModel.DEFENSE_PROP_ALL.equals(name)).collect(Collectors.joining(", "));

		Label componentLabel = new Label(composite, SWT.NONE);
		componentLabel.setText("Component:");
		componentLabel.setFont(boldFont);

		Label compsLabel = new Label(composite, SWT.WRAP);
		GridData compsLabelGridData = new GridData(SWT.LEFT, SWT.TOP, true, false);
		compsLabelGridData.widthHint = 600; // this is hardcoded, don't know how to make it adapt to window size
		compsLabel.setLayoutData(compsLabelGridData);
		compsLabel.setText(allComps);

		// List all components

		Label propLabel = new Label(composite, SWT.NONE);
		propLabel.setText("Defense Property:");
		propLabel.setFont(boldFont);

		Label defPropsLabel = new Label(composite, SWT.WRAP);
		GridData defPropsLabelGridData = new GridData(SWT.LEFT, SWT.TOP, true, false);
		defPropsLabelGridData.widthHint = 600; // this is hardcoded, don't know how to make it adapt to window size
		defPropsLabel.setLayoutData(defPropsLabelGridData);
		defPropsLabel.setText(allDefenseProps);

		// List all defense properties

		Composite topButtons = new Composite(composite, SWT.NONE);
		topButtons.setLayout(new RowLayout(SWT.HORIZONTAL));
		topButtons.setLayoutData(new GridData(SWT.CENTER, SWT.BOTTOM, true, true, 1, 1));

		Button newRule = new Button(topButtons, SWT.PUSH);
		newRule.setText("Add row");
		newRule.setFont(font);

		table = new Table(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		gridData.heightHint = 500;
		table.setLayoutData(gridData);
		table.setHeaderBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
		table.setHeaderForeground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_FOREGROUND));

//		TableColumn componentCol = new TableColumn(table, SWT.CENTER | SWT.WRAP);
//		componentCol.setText("Component");
//		componentCol.setWidth(240);
//		TableColumn defenseCol = new TableColumn(table, SWT.CENTER | SWT.WRAP);
//		defenseCol.setText("Defense Property");
//		defenseCol.setWidth(240);
//		TableColumn dalCol = new TableColumn(table, SWT.CENTER | SWT.WRAP);
//		dalCol.setText("DAL");
//		dalCol.setWidth(100);
//		TableColumn costCol = new TableColumn(table, SWT.CENTER | SWT.WRAP);
//		costCol.setText("Cost");
//		costCol.setWidth(160);
//		TableColumn deleteCol = new TableColumn(table, SWT.CENTER | SWT.WRAP);
//		deleteCol.setWidth(table.getItemHeight());

		createTableViewer();

//		loadTable();

		table.setHeaderVisible(true);

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

		newRule.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				costModel.rules.add(costModel.createRule(Optional.empty(), Optional.empty(), Optional.empty(), 0));
			}
		});

		cancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				composite.getShell().close();
			}
		});

		save.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				costModel.toFileXml(costModelFile);
				composite.getShell().close();
			}
		});

		return composite;
	}

	private void createTableViewer() {
		String[] columnNames = { "component", "defenseProp", "dal", "cost", "remove" };

		tableViewer = new TableViewer(table);
		tableViewer.setUseHashlookup(true);
		tableViewer.setColumnProperties(columnNames);

		createTableViewerColumn("Component", 200, 0).setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object elem) {
				return ((SynthesisCostModel.Rule) elem).getComponentStr();
			}
		});
		createTableViewerColumn("Defense Property", 200, 1).setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object elem) {
				return ((SynthesisCostModel.Rule) elem).getDefensePropertyStr();
			}
		});
		createTableViewerColumn("DAL", 80, 2).setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object elem) {
				return ((SynthesisCostModel.Rule) elem).getDalStr();
			}
		});
		createTableViewerColumn("Cost", 120, 3).setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object elem) {
				return ((SynthesisCostModel.Rule) elem).getValueStr();
			}
		});
		createTableViewerColumn("", 50, 4).setLabelProvider(new ColumnLabelProvider() {
			private Map<Object, Button> buttons = new HashMap<>();
			private Map<Object, SelectionListener> listeners = new HashMap<>();

			@Override
			public void update(ViewerCell cell) {
				TableItem item = (TableItem) cell.getItem();
				final Button button;
				if (buttons.containsKey(cell.getElement())) {
					button = buttons.get(cell.getElement());
				} else {
					button = new Button((Composite) cell.getViewerRow().getControl(), SWT.NONE);
					button.setImage(deleteImage);
					buttons.put(cell.getElement(), button);
				}
				TableEditor editor = new TableEditor(item.getParent());
				editor.grabHorizontal = true;
				editor.grabVertical = true;
				editor.setEditor(button, item, cell.getColumnIndex());

				if (listeners.containsKey(cell.getElement())) {
					button.removeSelectionListener(listeners.get(cell.getElement()));
				}
				final SynthesisCostModel.Rule rule = (SynthesisCostModel.Rule) cell.getElement();
				SelectionListener listener = new SelectionListener() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						costModel.rules.remove(rule);
						button.dispose();
						tableViewer.refresh();
					}

					@Override
					public void widgetDefaultSelected(SelectionEvent e) {
					}
				};
				listeners.put(cell.getElement(), listener);
				button.addSelectionListener(listener);

				editor.layout();
			}
		});

		CellEditor[] editors = new CellEditor[columnNames.length];
		editors[0] = new ComboBoxCellEditor(table, suggComponents.toArray(new String[] {}), SWT.NONE);
		editors[1] = new ComboBoxCellEditor(table, suggDefenseProps.toArray(new String[] {}), SWT.NONE);
		editors[2] = new ComboBoxCellEditor(table, suggDals.toArray(new String[] {}), SWT.NONE);
		editors[3] = new TextCellEditor(table, SWT.NONE);
		editors[4] = null;

		tableViewer.setCellEditors(editors);
		// tableViewer.setComparator(null);

		tableViewer.setCellModifier(new ICellModifier() {
			@Override
			public boolean canModify(Object element, String property) {
				return !property.equals("remove");
			}

			@Override
			public Object getValue(Object element, String property) {
				if (element instanceof SynthesisCostModel.Rule) {
					SynthesisCostModel.Rule rule = (SynthesisCostModel.Rule) element;
					switch (property) {
					case "component":
						return suggComponentsIndexMap.get(rule.getComponentStr());
					case "defenseProp":
						return suggDefensePropsIndexMap.get(rule.getDefensePropertyStr());
					case "dal":
						return suggDalsIndexMap.get(rule.getDalStr());
					case "cost":
						return rule.getValueStr();
					}
				}
				return null;
			}

			@Override
			public void modify(Object element, String property, Object value) {
				if (element instanceof TableItem) {
					element = ((TableItem) element).getData();
				}
				if (element instanceof SynthesisCostModel.Rule) {
					SynthesisCostModel.Rule rule = (SynthesisCostModel.Rule) element;
					switch (property) {
					case "component":
						costModel.updateRule(rule, rule.updateComponent(suggComponents.get((Integer) value)));
						break;
					case "defenseProp":
						costModel.updateRule(rule, rule.updateDefenseProperty(suggDefenseProps.get((Integer) value)));
						break;
					case "dal":
						costModel.updateRule(rule, rule.updateDal(suggDals.get((Integer) value)));
						break;
					case "cost":
						try {
							costModel.updateRule(rule, rule.updateValue((String) value));
						} catch (NumberFormatException e) {
						}
						break;
					}
				}
			}
		});

		tableViewer.setContentProvider(new ObservableListContentProvider<>());
		tableViewer.setInput(costModel.rules);
	}

	private TableViewerColumn createTableViewerColumn(String title, int width, int index) {
		TableViewerColumn column = new TableViewerColumn(tableViewer, SWT.LEFT, index);
		column.getColumn().setText(title);
		column.getColumn().setWidth(width);
		column.getColumn().setResizable(true);
		column.getColumn().setMoveable(false);
		return column;
	}
}
