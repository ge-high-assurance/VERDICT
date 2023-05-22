package com.ge.research.osate.verdict.gui;

import com.ge.research.osate.verdict.dsl.parser.antlr.VerdictParser;
import com.ge.research.osate.verdict.dsl.ui.VerdictUiModule;
import com.ge.research.osate.verdict.dsl.ui.internal.VerdictActivator;
import com.google.inject.Injector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.DefaultLocationInFileProvider;
import org.eclipse.xtext.util.ITextRegion;
import org.osate.aadl2.impl.DefaultAnnexSubclauseImpl;
import org.osate.aadl2.impl.SystemTypeImpl;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Author: Soumya Talukder Date: Jul 18, 2019 */
// this class performs multiple activities when user click "Save & Close" button in Wizard
public class WzrdClosure {
    private boolean validationFailed = false;
    private boolean invalidReq =
            false; // boolean containing information whether an entered cyber-property is
    // invalid/incomplete
    private boolean invalidMission =
            false; // boolean containing information whether an entered cyber-mission is
    // invalid/incomplete
    private boolean invalidMissionFound =
            false; // boolean containing information whether any of the entered cyber-mission is
    // invalid/incomplete
    private SystemTypeImpl sys; // the current SystemTypeImpl instance
    private boolean systemLevel;
    private List<WzrdTableRow> tableContents;
    public List<WzrdTableRow> backUp;
    private DrpDnLists drpdn;
    private List<Integer> failedToParseRows = new ArrayList<Integer>();
    private int offset = -1;
    private String appendix = null;
    private boolean needToAppend = false;
    private OffsetTabPair offTab;
    private boolean annexPresent;
    private List<MissionInfo> missions;

    public WzrdClosure(
            SystemTypeImpl sys,
            List<WzrdTableRow> tableContents,
            List<WzrdTableRow> deleteContents,
            IPath filePath,
            boolean systemLevel,
            DrpDnLists drpdn,
            Composite composite,
            List<MissionInfo> missions,
            Shell shell) {
        this.sys = reloadSystem(sys);
        this.tableContents = tableContents;
        this.systemLevel = systemLevel;
        this.drpdn = drpdn;
        this.backUp = new ArrayList<WzrdTableRow>(tableContents);
        this.missions = missions;
        annexPresent = hasAnnex();

        // save the invoking .aadl editor if it has unsaved content
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IEditorPart openEditor = page.getActiveEditor();

        if (openEditor != null) {
            boolean response = page.saveEditor(openEditor, true);
            if (response && openEditor.isDirty()) {
                MessageDialog.openError(
                        shell,
                        "VERDICT Wizard Launcher",
                        "Wizard cannot save content with existing unsaved content on the editor. Save the .aadl editor before clicking 'Save + Close' in Wizard.");
                return;
            }
        }

        if (!validate(composite)) {
            validationFailed = true;
            return;
        }

        deleteOldContents();
        if ((tableContents.size() > 0) || (missions.size() > 0)) {
            getAppendOffset(annexPresent, filePath);
        }
        generateAppendCode(annexPresent);
        parseForCorrectness();

        String errStr;
        if (failedToParseRows.size() > 0) {
            errStr = "Logical definition is not entered or is invalid, for one or more rows. ";
            errStr =
                    errStr
                            + "While saving, those rows will be discarded by Wizard. Are you sure to continue?";
            invalidReq =
                    !(MessageDialog.openConfirm(
                            composite.getShell(), "Wizard: Confirmation", errStr));
        }
        if (invalidMissionFound) {
            errStr = "At least one mission does not have any cyber-requirement assigned. ";
            errStr =
                    errStr
                            + "While saving, those missions will be discarded by Wizard. Are you sure to continue?";
            invalidMission =
                    !(MessageDialog.openConfirm(
                            composite.getShell(), "Wizard: Confirmation", errStr));
        }

        if (!(invalidReq || invalidMission)) {
            deleteIncorrectContents();
            try {
                ChangeInFile changedFile = new ChangeInFile(sys, deleteContents, filePath);
                appendMakeReady(annexPresent, filePath);
                changedFile.refreshFile(offset, needToAppend, appendix, systemLevel);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isAborted() {
        return (validationFailed || invalidMission || invalidReq);
    }

    private boolean validate(Composite composite) {
        Map<String, Integer> counts = new HashMap<>();

        for (WzrdTableRow row : tableContents) {
            String key = row.getFormulaID();
            if (counts.containsKey(key)) {
                counts.put(key, counts.get(key) + 1);
            } else {
                counts.put(key, 1);
            }
        }

        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                MessageDialog.openError(
                        composite.getShell(),
                        "Duplicate ID",
                        "Cannot save because multiple rows found with ID: " + entry.getKey());
                return false;
            }
        }

        // TODO: check for IDs already existing elsewhere in AADL file

        return true;
    }

    // delete existing contents initially read from the .aadl that are deleted in the script
    // during the current Wizard session
    private void deleteOldContents() {
        int repI = 0;
        int initTableSize = tableContents.size();
        for (int i = 0; i < initTableSize; i++) {
            if (!tableContents.get(repI).isNew()) {
                String existingID = tableContents.get(repI).getFormulaID();
                tableContents.remove(repI);
                for (int ii = 0; ii < missions.size(); ii++) {
                    Set<Integer> rows = missions.get(ii).getRow();
                    Set<Integer> newRows = new HashSet<Integer>();
                    Iterator<Integer> elements = rows.iterator();
                    while (elements.hasNext()) {
                        int k = elements.next();
                        if (k > repI) {
                            newRows.add(k - 1);
                        } else if (k == repI) {
                            missions.get(ii).addToCyberReqs(existingID);
                            // do nothing
                        } else {
                            newRows.add(k);
                        }
                    }
                    missions.get(ii).setRow(newRows);
                }
                repI--;
            }
            repI++;
        }
    }

    // delete rows with incorrect logical definition in the table
    private void deleteIncorrectContents() {
        int repI = 0;
        int initTableSize = tableContents.size();
        for (int i = 0; i < initTableSize; i++) {
            for (int j = 0; j < failedToParseRows.size(); j++) {
                if (i == failedToParseRows.get(j)) {
                    tableContents.remove(repI);
                    for (int ii = 0; ii < missions.size(); ii++) {
                        Set<Integer> rows = missions.get(ii).getRow();
                        Set<Integer> newRows = new HashSet<Integer>();
                        Iterator<Integer> elements = rows.iterator();
                        while (elements.hasNext()) {
                            int k = elements.next();
                            if (k > repI) {
                                newRows.add(k - 1);
                            } else if (k == repI) {
                                // do nothing
                            } else {
                                newRows.add(k);
                            }
                        }
                        missions.get(ii).setRow(newRows);
                    }
                    repI--;
                }
            }
            repI++;
        }
    }

    // parse the entered logical formula for syntactic correctness
    private void parseForCorrectness() {
        for (int i = 0; i < tableContents.size(); i++) {
            if (tableContents.get(i).isNew()) {
                Injector injector =
                        VerdictActivator.getInstance().getInjector(VerdictUiModule.INJECTOR_NAME);
                VerdictParser parser = injector.getInstance(VerdictParser.class);
                IParseResult parseResult = null;
                try {
                    parseResult =
                            parser.parse(
                                    parser.getGrammarAccess().getAnnexSubclauseRule(),
                                    new StringReader(tableContents.get(i).getVerdictStatement()));
                } catch (Exception e) {
                    System.out.println("Error in parsing!!");
                    e.printStackTrace();
                }
                if ((parseResult == null) || parseResult.hasSyntaxErrors()) {
                    failedToParseRows.add(i);
                    if (systemLevel && missions.size() > 0) {
                        for (int j = 0; j < missions.size(); j++) {
                            Set<Integer> rows = missions.get(j).getRow();
                            Set<Integer> newRows = new HashSet<Integer>();
                            if (rows.contains(i)) {
                                rows.remove(i);
                            }
                            Iterator<Integer> elements = rows.iterator();
                            while (elements.hasNext()) {
                                int k = elements.next();
                                if (k > i) {
                                    newRows.add(k - 1);
                                } else if (k == i) {
                                    // do nothing
                                } else {
                                    newRows.add(k);
                                }
                            }
                            if (newRows.size() > 0) {
                                missions.get(j).setRow(newRows);
                            } else {
                                missions.remove(j);
                                invalidMissionFound = true;
                            }
                        }
                    }
                }
            }
        }
    }

    // generate the code that needs to be injected into the .aadl script by Wizard
    private void generateAppendCode(Boolean hasAnnex) {
        if (tableContents.size() == 0 && missions.size() == 0) {
            return;
        }
        int tabCount = offTab.tabCount;

        for (int i = 0; i < tableContents.size(); i++) {
            String str = "";
            WzrdTableRow tableRow = tableContents.get(i);
            if (systemLevel) {
                str = str + "CyberReq {\n";
                str = addTab(tabCount + 2, str) + "id = \"" + tableRow.getFormulaID() + "\"\n";
                str =
                        addTab(tabCount + 2, str)
                                + "condition = "
                                + tableRow.getThirdElement()
                                + "\n";
                str =
                        addTab(tabCount + 2, str)
                                + "cia = "
                                + drpdn.CIA_ABBREV[tableRow.getFirstElement()]
                                + "\n";
                str =
                        addTab(tabCount + 2, str)
                                + "severity = "
                                + drpdn.SEVERITY[tableRow.getSecondElement()]
                                + "\n";

                if ((tableRow.getComment() != null) && tableRow.getComment() != "") {
                    str =
                            addTab(tabCount + 2, str)
                                    + "comment = \""
                                    + tableRow.getComment()
                                    + "\"\n";
                }
                if ((tableRow.getDescription() != null) && tableRow.getComment() != "") {
                    str =
                            addTab(tabCount + 2, str)
                                    + "description = \""
                                    + tableRow.getDescription()
                                    + "\"\n";
                }
                if ((tableRow.getPhase() != null) && tableRow.getComment() != "") {
                    str = addTab(tabCount + 2, str) + "phases = \"" + tableRow.getPhase() + "\"\n";
                }
                if ((tableRow.getExternal() != null) && tableRow.getComment() != "") {
                    str =
                            addTab(tabCount + 2, str)
                                    + "external = \""
                                    + tableRow.getPhase()
                                    + "\"\n";
                }

                if (hasAnnex) {
                    str = addTab(tabCount + 1, str) + "}";
                } else {
                    str = addTab(tabCount + 2, str) + "}";
                }
                tableRow.setVerdictStatement(str);
            } else {
                str = str + "CyberRel {\n";
                str = addTab(tabCount + 2, str) + "id = \"" + tableRow.getFormulaID() + "\"\n";
                if (!tableRow.getThirdElement().equals("TRUE")) {
                    str =
                            addTab(tabCount + 2, str)
                                    + "inputs = "
                                    + tableRow.getThirdElement()
                                    + "\n";
                }
                str =
                        addTab(tabCount + 2, str)
                                + "output = "
                                + drpdn.outPorts[tableRow.getFirstElement()]
                                + ":"
                                + drpdn.CIA_ABBREV[tableRow.getSecondElement()]
                                + "\n";

                if ((tableRow.getComment() != null) && tableRow.getComment() != "") {
                    str =
                            addTab(tabCount + 2, str)
                                    + "comment = \""
                                    + tableRow.getComment()
                                    + "\"\n";
                }
                if ((tableRow.getDescription() != null) && tableRow.getComment() != "") {
                    str =
                            addTab(tabCount + 2, str)
                                    + "description = \""
                                    + tableRow.getDescription()
                                    + "\"\n";
                }
                if ((tableRow.getPhase() != null) && tableRow.getComment() != "") {
                    str = addTab(tabCount + 2, str) + "phases = \"" + tableRow.getPhase() + "\"\n";
                }
                if ((tableRow.getExternal() != null) && tableRow.getComment() != "") {
                    str =
                            addTab(tabCount + 2, str)
                                    + "external = \""
                                    + tableRow.getPhase()
                                    + "\"\n";
                }

                if (hasAnnex) {
                    str = addTab(tabCount + 1, str) + "}";
                } else {
                    str = addTab(tabCount + 2, str) + "}";
                }
                tableRow.setVerdictStatement(str);
            }
        }

        if (systemLevel && missions.size() > 0) {
            for (int i = 0; i < missions.size(); i++) {
                String str = "";
                MissionInfo mission = missions.get(i);
                Set<Integer> cyberReqs = mission.getRow();
                if (cyberReqs.size() == 0 && missions.get(i).getCyberReqs().size() == 0) {
                    missions.get(i).setVerdictStatement(str);
                    invalidMissionFound = true;
                    continue;
                }
                str = str + "MissionReq {\n";
                str = addTab(tabCount + 2, str) + "id = \"" + mission.getMissionID() + "\"\n";
                str = addTab(tabCount + 2, str) + "cyberReqs = ";
                Iterator<Integer> elements = cyberReqs.iterator();
                while (elements.hasNext()) {
                    int currentID = elements.next();
                    mission.addToCyberReqs(tableContents.get(currentID).getFormulaID());
                }
                for (int j = 0; j < mission.getCyberReqs().size(); j++) {
                    if (j != mission.getCyberReqs().size() - 1) {
                        str = str + "\"" + mission.getCyberReqs().get(j) + "\", ";
                    } else {
                        str = str + "\"" + mission.getCyberReqs().get(j) + "\"\n";
                    }
                }

                if ((mission.getComment() != null) && mission.getComment() != "") {
                    str =
                            addTab(tabCount + 2, str)
                                    + "comment = \""
                                    + mission.getComment()
                                    + "\"\n";
                }
                if ((mission.getDescription() != null) && mission.getComment() != "") {
                    str =
                            addTab(tabCount + 2, str)
                                    + "description = \""
                                    + mission.getDescription()
                                    + "\"\n";
                }

                if (hasAnnex) {
                    str = addTab(tabCount + 1, str) + "}";
                } else {
                    str = addTab(tabCount + 2, str) + "}";
                }
                missions.get(i).setVerdictStatement(str);
            }
        }
    }

    // add cosmetic tab/newline etc to the generated code block, as appropriate for the .aadl script
    private void appendMakeReady(Boolean hasAnnex, IPath filePath) {

        if (tableContents.size() == 0 && missions.size() == 0) {
            return;
        }
        int tabCount = offTab.tabCount;

        appendix = "";

        if (!hasAnnex) {
            appendix = addTab(tabCount + 1, appendix) + "annex verdict{**\n";
            appendix = addTab(tabCount + 2, appendix);
        } else {
            appendix = addTab(tabCount, appendix);
        }
        for (int i = 0; i < tableContents.size(); i++) {
            needToAppend = true;
            if (i == 0) {
                appendix = appendix + tableContents.get(i).getVerdictStatement();
            } else {
                if (!hasAnnex) {
                    appendix = appendix + "\n";
                    appendix =
                            addTab(tabCount + 2, appendix)
                                    + tableContents.get(i).getVerdictStatement();
                } else {
                    appendix = appendix + "\n";
                    appendix =
                            addTab(tabCount + 1, appendix)
                                    + tableContents.get(i).getVerdictStatement();
                }
            }
        }

        if (systemLevel && (missions.size() > 0)) {
            for (int i = 0; i < missions.size(); i++) {
                if (missions.get(i).getVerdictStatement() == "") {
                    continue;
                }
                needToAppend = true;
                if (!hasAnnex) {
                    appendix = appendix + "\n";
                    appendix =
                            addTab(tabCount + 2, appendix) + missions.get(i).getVerdictStatement();
                } else {
                    appendix = appendix + "\n";
                    appendix =
                            addTab(tabCount + 1, appendix) + missions.get(i).getVerdictStatement();
                }
            }
        }

        if (!hasAnnex) {
            appendix = appendix + "\n";
            appendix = addTab(tabCount + 1, appendix) + "**};\n";
        } else {
            appendix = appendix + "\n\t";
        }
    }

    // get the desired starting offset (as in the .aadl) of the generated code-block
    private void getAppendOffset(boolean hasAnnex, IPath filePath) {
        DefaultLocationInFileProvider defLoc = new DefaultLocationInFileProvider();

        if (!hasAnnex) {
            ITextRegion region = defLoc.getFullTextRegion(sys);
            try {
                offset = region.getOffset() + region.getLength();
                offTab = getOffsetTab(filePath);

            } catch (Exception e) {
                e.printStackTrace();
            }
            offset = offset - offTab.addOffset;

        } else {
            List<EObject> objs = sys.eContents();
            for (int i = 0; i < objs.size(); i++) {
                if (objs.get(i) instanceof DefaultAnnexSubclauseImpl) {
                    if (!((DefaultAnnexSubclauseImpl) objs.get(i)).getName().equals("verdict")) {
                        continue;
                    }
                    ITextRegion region = defLoc.getFullTextRegion(objs.get(i));
                    try {
                        offset = region.getOffset() + region.getLength();
                        offTab = getOffsetTab(filePath);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    offset =
                            offset
                                    - offTab.addOffset
                                    + 1; // 1 appear here since no semicolon if annex is present
                    break;
                }
            }
        }
    }

    protected List<WzrdTableRow> getBackUp() {
        return backUp;
    }

    // check whether current component has existing Annex block
    protected boolean hasAnnex() {
        boolean noAnnex = true;
        List<EObject> objs = sys.eContents();
        for (int i = 0; i < objs.size(); i++) {
            if (objs.get(i) instanceof DefaultAnnexSubclauseImpl) {
                if (!((DefaultAnnexSubclauseImpl) objs.get(i)).getName().equals("verdict")) {
                    continue;
                }
                noAnnex = false;
                break;
            }
        }
        return !noAnnex;
    }

    // Determine the offset/no of tabs for each line of the generated code-block
    private OffsetTabPair getOffsetTab(IPath filePath) throws IOException {

        FileReader file =
                new FileReader(
                        ResourcesPlugin.getWorkspace()
                                .getRoot()
                                .getFile(filePath)
                                .getRawLocationURI()
                                .toURL()
                                .getPath());
        int c;
        int count = 0;
        int addOffset = 0;
        int tabCount = 0;

        while ((c = file.read()) != -1) {
            count++;
            if (count == offset) {
                break;
            }

            char repC = (char) c;

            if (String.valueOf(repC).equals("\n")) {
                addOffset = 0;
                tabCount = 0;
            } else {
                addOffset++;
                if (String.valueOf(repC).equals("\t")) {
                    tabCount++;
                }
            }
        }

        file.close();
        return new OffsetTabPair(addOffset, tabCount);
    }

    // append tab to the string
    private String addTab(int i, String str) {
        for (int j = 0; j < i; j++) {
            str = str + "\t";
        }
        return str;
    }

    // a class that stores a pair of integers
    private class OffsetTabPair {
        public int addOffset;
        public int tabCount;

        private OffsetTabPair(int i, int j) {
            addOffset = i;
            tabCount = j;
        }
    }

    private SystemTypeImpl reloadSystem(SystemTypeImpl sys) {
        Resource oldResource = sys.eResource();
        ResourceSetImpl resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry();
        Resource resource = resourceSet.createResource(oldResource.getURI());
        try {
            resource.load(null);
        } catch (Exception e) {
            System.out.println("Error in reloading resource while saving content by Wizard.");
            e.printStackTrace();
        }
        TreeIterator<EObject> tree = resource.getAllContents();
        while (tree.hasNext()) {
            EObject anObject = tree.next();
            if (anObject instanceof SystemTypeImpl) {
                if (((SystemTypeImpl) anObject).getFullName().equals(sys.getFullName())) {
                    sys = (SystemTypeImpl) anObject;
                    break;
                }
            }
        }
        return sys;
    }
}
