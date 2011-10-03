package org.strategoxt.imp.testing.preferences;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.spi.RegistryContributor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.strategoxt.imp.testing.listener.ITestListener;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By subclassing
 * <samp>FieldEditorPreferencePage</samp>, we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that belongs to the main
 * plug-in class. That way, preferences can be accessed directly via the preference store.
 */

public class SpoofaxTestingPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public SpoofaxTestingPreferencePage() {
		super(GRID);
		setPreferenceStore(PlatformUI.getPreferenceStore());
		setDescription("Spoofax-Testing preferences");
	}

	/**
	 * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to manipulate various
	 * types of preferences. Each field editor knows how to save and restore itself.
	 */
	public void createFieldEditors() {

		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(
				ITestListener.EXTENSION_ID);
		String[][] providers = new String[config.length][2];

		int i = 0;
		for (IConfigurationElement e : config) {
			RegistryContributor contributor = (RegistryContributor) e.getContributor();
			providers[i][0] = contributor.getName();
			providers[i][1] = contributor.getActualName();
			i++;
		}

		addField(new ComboFieldEditor(PreferenceConstants.P_LISTENER_ID, "&Plug-in to use as a view", providers,
				getFieldEditorParent()));

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}

}