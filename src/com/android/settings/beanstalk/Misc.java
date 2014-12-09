package com.android.settings.beanstalk;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.widget.Toast;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class Misc extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_NAVIGATION_BAR_HEIGHT = "navigation_bar_height";

    private ListPreference mNavigationBarHeight;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Activity activity = getActivity();
        final ContentResolver resolver = activity.getContentResolver();

        addPreferencesFromResource(R.xml.beanstalk_misc);

	PreferenceScreen prefSet = getPreferenceScreen();

	// Navbar height
	mNavigationBarHeight = (ListPreference) findPreference(KEY_NAVIGATION_BAR_HEIGHT);
	mNavigationBarHeight.setOnPreferenceChangeListener(this);
	int statusNavigationBarHeight = Settings.System.getInt(resolver,
		Settings.System.NAVIGATION_BAR_HEIGHT, 48);
	mNavigationBarHeight.setValue(String.valueOf(statusNavigationBarHeight));
	mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntry());
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
	ContentResolver resolver = getActivity().getContentResolver();
	final String key = preference.getKey();
	if (preference == mNavigationBarHeight) {
	    int statusNavigationBarHeight = Integer.valueOf((String) objValue);
	    int index = mNavigationBarHeight.findIndexOfValue((String) objValue);
	    Settings.System.putInt(resolver, Settings.System.NAVIGATION_BAR_HEIGHT,
		statusNavigationBarHeight);
	    mNavigationBarHeight.setSummary(mNavigationBarHeight.getEntries()[index]);
	}

	return true;
    }
}
