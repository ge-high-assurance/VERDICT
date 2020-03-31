package com.ge.research.osate.verdict.gui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.function.Predicate;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

public class BundlePreferences extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	// preference keys
	private static final String BUNDLE_JAR = "verdict_bundle_jar";
	private static final String DOCKER_IMAGE = "verdict_bundle_image";
	private static final String STEM_DIR = "STEM";
	private static final String AADL2IML_BIN = "aadl2iml_bin";
	private static final String KIND2_BIN = "kind2_bin";
	private static final String SOTERIA_PP_BIN = "soteria_pp_bin";
	private static final String GRAPH_VIZ_PATH = "graph_viz_path";

	public BundlePreferences() {
		super();
	}

	private static ScopedPreferenceStore preferenceStore = null;

	public static ScopedPreferenceStore getVerdictPreferenceStore() {
		if (preferenceStore == null) {
			preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, "com.ge.research.osate.verdict");
		}
		return preferenceStore;
	}

	public static String getBundleJar() {
		return getVerdictPreferenceStore().getString(BUNDLE_JAR);
	}

	public static String getDockerImage() {
		return getVerdictPreferenceStore().getString(DOCKER_IMAGE);
	}

	public static String getStemDir() {
		return getVerdictPreferenceStore().getString(STEM_DIR);
	}

	public static String getAadl2imlBin() {
		return getVerdictPreferenceStore().getString(AADL2IML_BIN);
	}

	public static String getKind2Bin() {
		return getVerdictPreferenceStore().getString(KIND2_BIN);
	}

	public static String getSoteriaPpBin() {
		return getVerdictPreferenceStore().getString(SOTERIA_PP_BIN);
	}

	public static String getGraphVizPath() {
		return getVerdictPreferenceStore().getString(GRAPH_VIZ_PATH);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(getPreferenceStore());
		setDescription("Preferences for Verdict (MBAS/CRV)");
		this.noDefaultAndApplyButton();
	}

	/**
	 * Make it so that a file/directory text field gets saved when modified.
	 *
	 * @param editor the text field
	 * @param pref_key the preferences key for storing the path
	 * @param validator only save if the file passes this validation predicate
	 */
	private void addSaveHandler(StringFieldEditor editor, String pref_key, Predicate<File> validator) {
		// The preferences won't save any other way
		// This is really ugly, and it makes it save even if the user doesn't press "Apply and Close"
		// But at least it saves the preferences instead of not saving them at all

		Text textField = null;

		try {
			Field field = StringFieldEditor.class.getDeclaredField("textField");
			field.setAccessible(true);

			textField = (Text) field.get(editor);
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			e.printStackTrace();
		}

		if (textField != null) {
			textField.addModifyListener(event -> {
				String path = editor.getStringValue();
				if (validator.test(new File(path))) {
					ScopedPreferenceStore prefs = getVerdictPreferenceStore();
					prefs.putValue(pref_key, path);
					try {
						prefs.save();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	/**
	 * Make it so that a string text field gets saved when modified.
	 *
	 * @param editor the text field
	 * @param pref_key the preferences key for storing the path
	 */
	private void addSaveHandler(StringFieldEditor editor, String pref_key) {
		// The preferences won't save any other way
		// This is really ugly, and it makes it save even if the user doesn't press "Apply and Close"
		// But at least it saves the preferences instead of not saving them at all

		Text textField = null;

		try {
			Field field = StringFieldEditor.class.getDeclaredField("textField");
			field.setAccessible(true);

			textField = (Text) field.get(editor);
		} catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
			e.printStackTrace();
		}

		if (textField != null) {
			textField.addModifyListener(event -> {
				String path = editor.getStringValue();
				ScopedPreferenceStore prefs = getVerdictPreferenceStore();
				prefs.putValue(pref_key, path);
				try {
					prefs.save();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		}
	}

	@Override
	protected void createFieldEditors() {
		FileFieldEditor bundleJar = new FileFieldEditor(BUNDLE_JAR, "Bundle JAR:", true,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		bundleJar.setFileExtensions(new String[] { "*.jar" });
		bundleJar.setStringValue(getBundleJar());
		addField(bundleJar);
		addSaveHandler(bundleJar, BUNDLE_JAR,
				file -> file.exists() && file.isFile() && file.getAbsolutePath().endsWith(".jar"));

		StringFieldEditor dockerImage = new StringFieldEditor(DOCKER_IMAGE, "Bundle docker IMAGE:",
				30, 1, StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		dockerImage.setStringValue(getDockerImage());
		addField(dockerImage);
		addSaveHandler(dockerImage, DOCKER_IMAGE);

		DirectoryFieldEditor stemDir = new DirectoryFieldEditor(STEM_DIR, "STEM Project Path:", getFieldEditorParent());
		stemDir.setStringValue(getStemDir());
		addField(stemDir);
		addSaveHandler(stemDir, STEM_DIR, file -> file.exists() && file.isDirectory());

		FileFieldEditor aadl2imlBin = new FileFieldEditor(AADL2IML_BIN, "Aadl2iml Binary:", true,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		aadl2imlBin.setStringValue(getAadl2imlBin());
		addField(aadl2imlBin);
		addSaveHandler(aadl2imlBin, AADL2IML_BIN, file -> file.exists() && file.isFile());

		FileFieldEditor kind2Bin = new FileFieldEditor(KIND2_BIN, "Kind2 Binary:", true,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		kind2Bin.setStringValue(getKind2Bin());
		addField(kind2Bin);
		addSaveHandler(kind2Bin, KIND2_BIN, file -> file.exists() && file.isFile());

		FileFieldEditor soteriaPpBin = new FileFieldEditor(SOTERIA_PP_BIN, "Soteria++ Binary:", true,
				StringFieldEditor.VALIDATE_ON_KEY_STROKE, getFieldEditorParent());
		soteriaPpBin.setStringValue(getSoteriaPpBin());
		addField(soteriaPpBin);
		addSaveHandler(soteriaPpBin, SOTERIA_PP_BIN, file -> file.exists() && file.isFile());

		DirectoryFieldEditor graphVizPath = new DirectoryFieldEditor(GRAPH_VIZ_PATH, "GraphViz Path:",
				getFieldEditorParent());
		graphVizPath.setStringValue(getGraphVizPath());
		addField(graphVizPath);
		addSaveHandler(graphVizPath, GRAPH_VIZ_PATH, file -> file.exists() && file.isDirectory());
	}
}
