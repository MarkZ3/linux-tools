package org.eclipse.linuxtools.internal.tmf.core;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.linuxtools.tmf.core.timestamp.TmfTimePreferences;

/**
 * Preference initializer for tmf.core
 *
 */
public class TmfCorePreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IEclipsePreferences defaultPreferences = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
        TmfTimePreferences.init(defaultPreferences);
    }

}
