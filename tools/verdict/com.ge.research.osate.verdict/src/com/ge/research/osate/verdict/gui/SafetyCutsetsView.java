package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

public class SafetyCutsetsView extends ViewPart {
    public static final String ID = "com.ge.research.osate.verdict.gui.safetyCutsetsView";
    private Composite composite;
    public static List<MBASSafetyResult.CutsetResult> cutsets = new ArrayList<>();

    public SafetyCutsetsView() {
        super();
    }

    @Override
    public void setFocus() {
        if (composite != null) {
            composite.setFocus();
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        if (composite == null) {
            composite = new Composite(parent, SWT.NONE);
        }
        Display display = Display.getCurrent();

        composite.setSize(1130, 600);
        composite.setLayout(new FillLayout());

        Table table = new Table(composite, SWT.MULTI | SWT.FULL_SELECTION);
        table.setHeaderVisible(true);
        table.setLinesVisible(true);
        table.setHeaderBackground(display.getSystemColor(SWT.COLOR_TITLE_BACKGROUND_GRADIENT));
        table.setHeaderForeground(display.getSystemColor(SWT.COLOR_TITLE_FOREGROUND));

        TableColumn minimalFailurePath = new TableColumn(table, SWT.CENTER | SWT.WRAP);
        minimalFailurePath.setText("Minimal Failure Path");
        TableColumn pathLikelihood = new TableColumn(table, SWT.CENTER | SWT.WRAP);
        pathLikelihood.setText("Path Likelihood");
        TableColumn events = new TableColumn(table, SWT.CENTER | SWT.WRAP);
        events.setText("Safety Failure Cutsets");

        for (int i = 0; i < cutsets.size(); i++) {
            TableItem item = new TableItem(table, SWT.NONE);
            MBASSafetyResult.CutsetResult cutset = cutsets.get(i);
            String eventsText =
                    String.join(
                            " and ",
                            cutset.getEvents().stream()
                                    .map(event -> event.getComponent() + ":" + event.getEventName())
                                    .collect(Collectors.toList()));
            item.setText(
                    new String[] {
                        "# " + Integer.toString(i + 1), cutset.getLikelihood(), eventsText
                    });
        }

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumn(i).pack();
        }

        table.pack();
        composite.pack();
    }
}
