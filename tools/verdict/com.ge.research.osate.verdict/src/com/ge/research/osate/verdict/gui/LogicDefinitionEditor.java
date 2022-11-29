package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.ApplicationWindow;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this class generates the Logical Definition editor dialog box for wizard
public class LogicDefinitionEditor extends ApplicationWindow {
    private Shell lastShell;
    private List<String> formulaBuffer = new ArrayList<String>();
    private List<String> tmpBuffer = new ArrayList<String>();
    private String startLine;
    private DrpDnLists drpdn;
    private boolean systemLevel;
    private boolean exportDefinition = false;
    private int leftParCount = 0;
    private int rightParCount = 0;

    // Jface variables
    private Composite composite;
    private Button leftParenthesis;
    private Button rightParenthesis;
    private Button lAnd;
    private Button lOr;
    private Button lNot;
    private Button delete;
    private Button reset;
    private Button done;
    private Combo comboLeft;
    private Combo comboRight;
    private Rectangle sourceRect;
    private Boolean valid = true;

    public LogicDefinitionEditor(
            Shell shell, DrpDnLists list, boolean sysLevel, Rectangle sourceRect) {
        super(shell);
        lastShell = shell;
        drpdn = list;
        systemLevel = sysLevel;
        this.sourceRect = sourceRect;

        if ((drpdn.inPorts.length == 0) && !systemLevel) {
            valid = false;
            MessageDialog.openError(
                    lastShell,
                    "VERDICT Wizard Launcher",
                    "No input defined for the selected component. Cannot define cyber-relation in terms of input.");
        }
    }

    public void run() {
        setShellStyle(SWT.TITLE | SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.MAX | SWT.RESIZE);
        setBlockOnOpen(true);
        open();
        lastShell.redraw();
        lastShell.setFocus();
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Wizard: Logical Definition Editor");
        shell.pack();
        double x = (sourceRect.width - 500) * 0.5;
        double y = (sourceRect.height - 500) * 0.5;
        shell.setLocation((int) x, (int) y);
    }

    // this defines all the widgets in the dialog-box
    @Override
    protected Control createContents(Composite parent) {
        startLine = "\nThe entered logic is:\n\n";
        composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));
        Label label = new Label(composite, SWT.WRAP);
        label.setText(startLine);
        Label labelBlank = new Label(composite, SWT.NONE);
        labelBlank.setText("");
        Label labelOperators = new Label(composite, SWT.NONE);
        labelOperators.setText("Operators");
        Group operators = new Group(composite, SWT.CENTER);
        operators.setLayout(new RowLayout(SWT.HORIZONTAL));
        leftParenthesis = new Button(operators, SWT.PUSH);
        leftParenthesis.setText(" ( ");
        rightParenthesis = new Button(operators, SWT.PUSH);
        rightParenthesis.setText(" ) ");
        rightParenthesis.setEnabled(false);
        lAnd = new Button(operators, SWT.PUSH);
        lAnd.setText("and");
        lAnd.setEnabled(false);
        lOr = new Button(operators, SWT.PUSH);
        lOr.setText("or");
        lOr.setEnabled(false);
        lNot = new Button(operators, SWT.PUSH);
        lNot.setText(" ! ");

        Group addPredicate = new Group(composite, SWT.NONE);
        addPredicate.setLayout(new RowLayout(SWT.HORIZONTAL));
        if (systemLevel) {
            addPredicate.setText("OUT port security");
        } else {
            addPredicate.setText("IN port security");
        }

        Group first = new Group(addPredicate, SWT.NONE);
        first.setLayout(new RowLayout(SWT.VERTICAL));

        Label comboLeftLabel = new Label(first, SWT.NONE);
        if (systemLevel) {
            comboLeftLabel.setText("OUT port");
        } else {
            comboLeftLabel.setText("IN port");
        }

        comboLeft = new Combo(first, SWT.DROP_DOWN | SWT.READ_ONLY);
        if (systemLevel) {
            comboLeft.setItems(drpdn.outPortsWithNull);
        } else {
            comboLeft.setItems(drpdn.inPortsWithNull);
        }

        Group second = new Group(addPredicate, SWT.NONE);
        second.setLayout(new RowLayout(SWT.VERTICAL));

        Label comboRightLabel = new Label(second, SWT.NONE);
        comboRightLabel.setText("Security concern");

        comboRight = new Combo(second, SWT.DROP_DOWN | SWT.READ_ONLY);
        comboRight.setItems(drpdn.CIA_WITH_NULL);
        comboRight.setEnabled(false);

        Group ending = new Group(composite, SWT.NONE);
        ending.setLayout(new RowLayout(SWT.HORIZONTAL));

        delete = new Button(ending, SWT.PUSH);
        delete.setText("  <  ");
        delete.setToolTipText("Click to delete the last entry");
        delete.setEnabled(false);

        reset = new Button(ending, SWT.PUSH);
        reset.setText("Reset");
        reset.setToolTipText("Click to reset the entered formula");

        done = new Button(ending, SWT.PUSH);
        done.setText("Done");
        done.setToolTipText("Click to save the entered formula in Wizard");
        done.setEnabled(false);

        composite.getShell().pack();

        // defines the activities to perform on clicking the button
        leftParenthesis.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        formulaBuffer.add("(");
                        label.setText(startLine + buffer2String(formulaBuffer));
                        label.pack();
                        comboLeft.deselectAll();
                        comboRight.deselectAll();
                        leftParCount++;
                        shellRefresh();
                    }
                });

        // defines the activities to perform on clicking the button
        rightParenthesis.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        formulaBuffer.add(")");
                        label.setText(startLine + buffer2String(formulaBuffer));
                        label.pack();
                        comboLeft.deselectAll();
                        comboRight.deselectAll();
                        leftParCount--;
                        shellRefresh();
                    }
                });

        // defines the activities to perform on clicking the button
        lAnd.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        formulaBuffer.add(" and ");
                        label.setText(startLine + buffer2String(formulaBuffer));
                        label.pack();
                        comboLeft.deselectAll();
                        comboRight.deselectAll();
                        shellRefresh();
                    }
                });

        // defines the activities to perform on clicking the button
        lOr.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        formulaBuffer.add(" or ");
                        label.setText(startLine + buffer2String(formulaBuffer));
                        label.pack();
                        comboLeft.deselectAll();
                        comboRight.deselectAll();
                        shellRefresh();
                    }
                });

        // defines the activities to perform on clicking the button
        lNot.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        formulaBuffer.add("!");
                        label.setText(startLine + buffer2String(formulaBuffer));
                        label.pack();
                        comboLeft.deselectAll();
                        comboRight.deselectAll();
                        shellRefresh();
                    }
                });

        // defines the activities to perform on clicking the button
        comboLeft.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        if (comboLeft.getSelectionIndex() > 0) {
                            if (systemLevel) {
                                tmpBuffer.add(
                                        drpdn.outPortsWithNull[
                                                max(comboLeft.getSelectionIndex(), 0)]);
                            } else {
                                tmpBuffer.add(
                                        drpdn.inPortsWithNull[
                                                max(comboLeft.getSelectionIndex(), 0)]);
                            }
                            formulaBuffer.add(buffer2String(tmpBuffer));
                        }
                        label.setText(startLine + buffer2String(formulaBuffer));
                        label.pack();
                        shellRefresh();
                    }
                });

        // defines the activities to perform on clicking the button
        comboRight.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        if (comboRight.getSelectionIndex() > 0) {
                            tmpBuffer.add(
                                    ":"
                                            + drpdn.CIA_ABBEV_WITH_NULL[
                                                    max(comboRight.getSelectionIndex(), 0)]);
                            formulaBuffer.remove(formulaBuffer.size() - 1);
                            formulaBuffer.add(buffer2String(tmpBuffer));
                            tmpBuffer.clear();
                        }
                        label.setText(startLine + buffer2String(formulaBuffer));
                        label.pack();
                        shellRefresh();
                    }
                });

        // defines the activities to perform on clicking the button
        reset.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        formulaBuffer.clear();
                        tmpBuffer.clear();
                        label.setText(startLine);
                        label.pack();
                        comboLeft.deselectAll();
                        comboRight.deselectAll();
                        leftParCount = 0;
                        rightParCount = 0;
                        shellRefresh();
                    }
                });

        // defines the activities to perform on clicking the button
        delete.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        String itemDeleted = formulaBuffer.get(formulaBuffer.size() - 1);
                        if (drpdn.findIndex(drpdn.OPERATORS_EXPR, itemDeleted) == 0) {
                            leftParCount--;
                        } else if (drpdn.findIndex(drpdn.OPERATORS_EXPR, itemDeleted) == 1) {
                            rightParCount--;
                        }
                        formulaBuffer.remove(formulaBuffer.size() - 1);
                        tmpBuffer.clear();
                        label.setText(startLine + buffer2String(formulaBuffer));
                        label.pack();
                        comboLeft.deselectAll();
                        comboRight.deselectAll();
                        shellRefresh();
                    }
                });

        // defines the activities to perform on clicking the button
        done.addSelectionListener(
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        exportDefinition = true;
                        composite.getShell().close();
                    }
                });

        composite
                .getShell()
                .addListener(
                        SWT.Close,
                        event -> {
                            if (exportDefinition) {
                                StatementEditor.logicDefined = buffer2String(formulaBuffer);
                            }
                        });

        return composite;
    }

    private int max(int a, int b) {
        if (a < b) {
            return b;
        } else {
            return a;
        }
    }

    // converts the buffer contents into a single string
    private String buffer2String(List<String> buffer) {
        String str = "";
        for (int i = 0; i < buffer.size(); i++) {
            str = str + buffer.get(i);
        }
        return str;
    }

    // refreshes all the widgets in the dialog box
    private void shellRefresh() {
        if (formulaBuffer.size() > 0) {
            String lastEntry = formulaBuffer.get(formulaBuffer.size() - 1);
            if (drpdn.findIndex(drpdn.OPERATORS_EXPR, lastEntry) == 1) {
                if (leftParCount > rightParCount) {
                    rightParenthesis.setEnabled(true);
                } else {
                    rightParenthesis.setEnabled(false);
                }
                lAnd.setEnabled(true);
                lOr.setEnabled(true);
                done.setEnabled(true);
                lNot.setEnabled(false);
                comboLeft.setEnabled(false);
                leftParenthesis.setEnabled(false);
            } else if ((drpdn.findIndex(drpdn.OPERATORS_EXPR, lastEntry) == 2)
                    || (drpdn.findIndex(drpdn.OPERATORS_EXPR, lastEntry) == 3)
                    || (drpdn.findIndex(drpdn.OPERATORS_EXPR, lastEntry) == 0)) {
                rightParenthesis.setEnabled(false);
                lAnd.setEnabled(false);
                lOr.setEnabled(false);
                done.setEnabled(false);
                lNot.setEnabled(true);
                comboLeft.setEnabled(true);
                leftParenthesis.setEnabled(true);
            } else if (drpdn.findIndex(drpdn.OPERATORS_EXPR, lastEntry) == 4) {
                rightParenthesis.setEnabled(false);
                lAnd.setEnabled(false);
                lOr.setEnabled(false);
                done.setEnabled(false);
                lNot.setEnabled(false);
                comboLeft.setEnabled(true);
                leftParenthesis.setEnabled(true);
            } else if ((drpdn.findIndex(drpdn.inPorts, lastEntry) >= 0)
                    || (drpdn.findIndex(drpdn.outPorts, lastEntry) >= 0)) {
                if (comboLeft.getSelectionIndex() > 0) {
                    comboLeft.setEnabled(false);
                    comboRight.setEnabled(true);
                    leftParenthesis.setEnabled(false);
                    rightParenthesis.setEnabled(false);
                    lAnd.setEnabled(false);
                    lOr.setEnabled(false);
                    lNot.setEnabled(false);
                    done.setEnabled(false);
                }
            } else {
                comboLeft.deselectAll();
                comboRight.deselectAll();
                comboRight.setEnabled(false);
                comboLeft.setEnabled(false);
                leftParenthesis.setEnabled(false);
                lNot.setEnabled(false);
                done.setEnabled(true);
                if (leftParCount > rightParCount) {
                    rightParenthesis.setEnabled(true);
                } else {
                    rightParenthesis.setEnabled(false);
                }
                lAnd.setEnabled(true);
                lOr.setEnabled(true);
            }
        } else {
            comboLeft.setEnabled(true);
            comboRight.setEnabled(false);
            leftParenthesis.setEnabled(true);
            rightParenthesis.setEnabled(false);
            lAnd.setEnabled(false);
            lOr.setEnabled(false);
            lNot.setEnabled(true);
            done.setEnabled(false);
        }
        if (leftParCount != rightParCount) {
            done.setEnabled(false);
        }
        if (formulaBuffer.size() == 0) {
            delete.setEnabled(false);
        } else {
            delete.setEnabled(true);
        }
        composite.update();
        composite.redraw();
        composite.getShell().pack();
    }

    public Boolean isValid() {
        return valid;
    }
}
