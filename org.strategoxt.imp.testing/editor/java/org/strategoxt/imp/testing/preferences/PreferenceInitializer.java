package org.strategoxt.imp.testing.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.PlatformUI;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	public final static String DEFAULT_LISTENER_ID = "org.strategoxt.imp.testing.ui";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer#initializeDefaultPreferences()
	 */
	@Override
	public void initializeDefaultPreferences() {
		System.out.println("CALLING DEFAULTS....");
		IPreferenceStore store = PlatformUI.getPreferenceStore();
		store.setDefault(PreferenceConstants.P_LISTENER_ID, DEFAULT_LISTENER_ID);
	}

}
