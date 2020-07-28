package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
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
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
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
import org.eclipse.swt.widgets.Text;
import org.osate.aadl2.ComponentImplementation;
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

		for (EObject obj : aadlObjs) {
			if (obj instanceof ComponentImplementation) {
				ComponentImplementation impl = (ComponentImplementation) obj;
				impls.add(impl);
				for (Subcomponent comp : impl.getAllSubcomponents()) {
					comps.add(comp);
				}
			}
		}

		Set<String> systemNames = new HashSet<>();
		systemNames.addAll(impls.stream().map(ComponentImplementation::getName).collect(Collectors.toList()));
		systemNames.addAll(comps.stream().map(Subcomponent::getName).collect(Collectors.toList()));

		suggComponents.add(SynthesisCostModel.COMPONENT_ALL);
		for (String system : systemNames) {
			suggComponents.add(system);
		}

		suggDefenseProps.add(SynthesisCostModel.DEFENSE_PROP_ALL);

		suggDals.add(SynthesisCostModel.DAL_LINEAR);
		for (int dal = 0; dal < 10; dal++) {
			suggDals.add(Integer.toString(dal));
		}

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
		ScrolledComposite scroller = new ScrolledComposite(parent, SWT.H_SCROLL);

		composite = new Composite(scroller, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		// TODO this scroll area doesn't seem to be working
		scroller.setContent(composite);
		scroller.setExpandHorizontal(true);
		scroller.setExpandVertical(true);
		scroller.setMinSize(500, 400);

		Label componentLabel = new Label(composite, SWT.NONE);
		componentLabel.setText("Component: ");
		componentLabel.setFont(boldFont);

		// List all components

		Label propLabel = new Label(composite, SWT.NONE);
		propLabel.setText("Defense Property: ");
		propLabel.setFont(boldFont);

		// List all defense properties

		table = new Table(composite, SWT.BORDER);
		table.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
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

			@Override
			public void update(ViewerCell cell) {
				TableItem item = (TableItem) cell.getItem();
				Button button = buttons.get(cell.getElement());
				if (button == null) {
					button = new Button((Composite) cell.getViewerRow().getControl(), SWT.NONE);
					button.setImage(deleteImage);
					buttons.put(cell.getElement(), button);

					// TODO detect press and delete...
					// need to associate with the correct row, which is tricky
				}
				TableEditor editor = new TableEditor(item.getParent());
				editor.grabHorizontal = true;
				editor.grabVertical = true;
				editor.setEditor(button, item, cell.getColumnIndex());
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

	private void loadTable() {
		table.removeAll();

		for (int i = 0; i < costModel.rules.size(); i++) {
			addTableRow(costModel.rules.get(i), i);
		}

		table.pack();
		composite.pack();
	}

	private void addTableRow(SynthesisCostModel.Rule rule, int index) {
//		TableItem row = new TableItem(table, SWT.NONE);
//		Color background = index % 2 == 0 ? new Color(Display.getCurrent(), 255, 255, 255)
//				: new Color(Display.getCurrent(), 204, 204, 204);
//
//		String component = rule.component.orElse(COMPONENT_ALL);
//		String defenseProp = rule.defenseProperty.orElse(DEFENSE_PROP_ALL);
//		String dal = rule.dal.map(i -> i.toString()).orElse(DAL_LINEAR);
//		String cost = Double.toString(rule.value);
//
//		row.setBackground(background);
//
//		addTextEditor(row, suggComponents, component, 0, background, str -> {
//			if (suggComponentsIndexMap.containsKey(str)) {
//				rule.component = str.equals(COMPONENT_ALL) ? Optional.empty() : Optional.of(str);
//				return true;
//			} else {
//				return false;
//			}
//		});
//
//		addTextEditor(row, suggDefenseProps, defenseProp, 1, background, str -> {
//			if (suggDefensePropsIndexMap.containsKey(str)) {
//				rule.defenseProperty = str.equals(DEFENSE_PROP_ALL) ? Optional.empty() : Optional.of(str);
//				return true;
//			} else {
//				return false;
//			}
//		});
//		addTextEditor(row, suggDals, dal, 2, background, str -> {
//			if (suggDalsIndexMap.containsKey(str)) {
//				try {
//					rule.dal = str.equals(DAL_LINEAR) ? Optional.empty() : Optional.of(Integer.parseInt(str));
//					return true;
//				} catch (NumberFormatException e) {
//					return false;
//				}
//			} else {
//				return false;
//			}
//		});
//		addTextEditor(row, null, cost, 3, background, str -> {
//			try {
//				rule.value = Double.parseDouble(str);
//				return true;
//			} catch (NumberFormatException e) {
//				return false;
//			}
//		});
//		addCloseButton(row, 4, background, () -> {
//			int pos = costModel.rules.indexOf(rule);
//			costModel.rules.remove(pos);
//			table.remove(pos);
//			row.dispose();
//		});
	}

	private void addTextEditor(TableItem row,
			List<String> options,
			String currentSelection,
			int column,
			Color background,
			Function<String, Boolean> listener) {
		TableEditor editor = new TableEditor(table);
		editor.grabHorizontal = true;
		editor.grabVertical = true;

		Color invalidColor = new Color(Display.getCurrent(), 255, 127, 127);

		if (options != null) {
			CCombo combo = new CCombo(table, SWT.NONE);
			combo.setEditable(true);
			for (String option : options) {
				combo.add(option);
			}
			combo.setText(currentSelection);
			combo.setBackground(background);

			combo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					combo.setBackground(listener.apply(combo.getText()) ? background : invalidColor);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					combo.setBackground(listener.apply(combo.getText()) ? background : invalidColor);
				}
			});

			combo.addModifyListener(
					e -> combo.setBackground(listener.apply(combo.getText()) ? background : invalidColor));

			editor.setEditor(combo, row, column);
		} else {
			Text text = new Text(table, SWT.NONE);
			text.setEditable(true);
			text.setText(currentSelection);
			text.setBackground(background);

			text.addModifyListener(e -> text.setBackground(listener.apply(text.getText()) ? background : invalidColor));

			editor.setEditor(text, row, column);
		}
	}

	private void addCloseButton(TableItem row, int column, Color background, Runnable listener) {
		TableEditor editor = new TableEditor(table);
		editor.grabHorizontal = true;
		editor.grabVertical = true;

		Button button = new Button(table, SWT.PUSH);
		button.setImage(deleteImage);
		button.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				listener.run();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		editor.setEditor(button, row, column);
	}
}
