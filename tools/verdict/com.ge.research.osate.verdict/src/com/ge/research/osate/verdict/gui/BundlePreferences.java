package com.ge.research.osate.verdict.gui;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class BundlePreferences extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {
    // preference keys
    private static final String STEM_DIR = "STEM";
    private static final String DOCKER_IMAGE = "verdict_bundle_image";
    private static final int FIRST_NON_DOCKER_INDEX =
            3; // index of first non-Docker field editor (including separator)
    private static final String BUNDLE_JAR = "verdict_bundle_jar";
    private static final String KIND2_BIN = "kind2_bin";
    private static final String SOTERIA_PP_BIN = "soteria_pp_bin";
    private static final String GRAPH_VIZ_PATH = "graph_viz_path";
    // singleton preference store
    private static ScopedPreferenceStore preferenceStore =
            new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.ge.research.osate.verdict");
    // child field editors
    private List<FieldEditor> fields = new ArrayList<>();

    public BundlePreferences() {
        super(GRID);
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(preferenceStore);
        setDescription("Preferences for Verdict (MBAS/CRV)");
    }

    public static String getStemDir() {
        return preferenceStore.getString(STEM_DIR);
    }

    public static String getDockerImage() {
        return preferenceStore.getString(DOCKER_IMAGE);
    }

    public static String getBundleJar() {
        return preferenceStore.getString(BUNDLE_JAR);
    }

    public static String getKind2Bin() {
        return preferenceStore.getString(KIND2_BIN);
    }

    public static String getSoteriaPpBin() {
        return preferenceStore.getString(SOTERIA_PP_BIN);
    }

    public static String getGraphVizPath() {
        return preferenceStore.getString(GRAPH_VIZ_PATH);
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        // Disable the non-docker field editors if the user types a docker image name
        String preferenceName = ((FieldEditor) event.getSource()).getPreferenceName();
        if (DOCKER_IMAGE.equals(preferenceName)) {
            String dockerImage = ((StringFieldEditor) event.getSource()).getStringValue();
            boolean isEnabled = dockerImage.isEmpty();
            for (int i = FIRST_NON_DOCKER_INDEX; i < fields.size(); i++) {
                fields.get(i).setEnabled(isEnabled, getFieldEditorParent());
            }
        }
        super.propertyChange(event);
    }

    @Override
    protected void createFieldEditors() {
        DirectoryFieldEditor stemDir =
                new DirectoryFieldEditor(STEM_DIR, "STEM Project Path:", getFieldEditorParent());
        addField(stemDir);

        StringFieldEditor dockerImage =
                new StringFieldEditor(DOCKER_IMAGE, "Bundle Docker Image:", getFieldEditorParent());
        addField(dockerImage);

        LabelFieldEditor separator =
                new LabelFieldEditor(
                        "   --- Remaining fields not needed if Docker is used ---   ",
                        getFieldEditorParent());
        addField(separator);

        FileFieldEditor bundleJar =
                new FileFieldEditor(BUNDLE_JAR, "Bundle Jar:", true, getFieldEditorParent());
        bundleJar.setFileExtensions(new String[] {"*.jar"});
        addField(bundleJar);

        FileFieldEditor kind2Bin =
                new FileFieldEditor(KIND2_BIN, "Kind2 Binary:", true, getFieldEditorParent());
        addField(kind2Bin);

        FileFieldEditor soteriaPpBin =
                new FileFieldEditor(
                        SOTERIA_PP_BIN, "Soteria++ Binary:", true, getFieldEditorParent());
        addField(soteriaPpBin);

        DirectoryFieldEditor graphVizPath =
                new DirectoryFieldEditor(GRAPH_VIZ_PATH, "GraphViz Path:", getFieldEditorParent());
        addField(graphVizPath);
    }

    @Override
    protected void addField(FieldEditor editor) {
        fields.add(editor);
        super.addField(editor);
    }
}
