package com.android.settings.beanstalk;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class StatusBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "StatusBarSettings";
    private static final String KEY_STATUS_BAR_CLOCK = "clock_style_pref";

    private PreferenceScreen mClockStyle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.beanstalk_statusbar_settings);

	mClockStyle = (PreferenceScreen) prefSet.findPreference(KEY_STATUS_BAR_CLOCK);
	updateClockStyleDescription();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        return false;
    }

    @Override
    public void onResume() {
	super.onResume();
	updateClockStyleDescription();
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
 		return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateClockStyleDescription() {
	if (mClockStyle == null) {
	    return;
	}
	if (Settings.System.getInt(getContentResolver(),
		Settings.System.STATUS_BAR_CLOCK, 1) == 1) {
	    mClockStyle.setSummary(getString(R.string.enabled));
	} else {
	    mClockStyle.setSummary(getString(R.string.disabled));
	}
    }
}
