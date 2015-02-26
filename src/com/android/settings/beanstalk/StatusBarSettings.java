package com.android.settings.beanstalk;

import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class StatusBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "StatusBarSettings";
    private static final String KEY_STATUS_BAR_CLOCK = "clock_style_pref";
    private static final String KEY_CARRIERLABEL_PREFERENCE = "carrier_options";
    private static final String KEY_STATUS_BAR_NETWORK_ARROWS= "status_bar_show_network_activity";

    private PreferenceScreen mClockStyle;
    private PreferenceScreen mCarrierLabel;
    private SwitchPreference mNetworkArrows;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.beanstalk_statusbar_settings);

	PreferenceScreen prefSet = getPreferenceScreen();

	PackageManager pm = getPackageManager();
	Resources systemUiResources;
	try {
	    systemUiResources = pm.getResourcesForApplication("com.android.systemui");
	} catch (Exception e) {
	    Log.e(TAG, "can't access systemui resources",e);
	    return;
	}

	mCarrierLabel = (PreferenceScreen) prefSet.findPreference(KEY_CARRIERLABEL_PREFERENCE);
	if (Utils.isWifiOnly(getActivity())) {
		prefSet.removePreference(mCarrierLabel);
	}

        // Network arrows
        mNetworkArrows = (SwitchPreference) prefSet.findPreference(KEY_STATUS_BAR_NETWORK_ARROWS);
        mNetworkArrows.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
            Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1) == 1);
        mNetworkArrows.setOnPreferenceChangeListener(this);
        int networkArrows = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1);
        updateNetworkArrowsSummary(networkArrows);

	mClockStyle = (PreferenceScreen) prefSet.findPreference(KEY_STATUS_BAR_CLOCK);
	updateClockStyleDescription();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
	if (preference == mNetworkArrows) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY,
                    (Boolean) newValue ? 1 : 0);
            int networkArrows = Settings.System.getInt(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1);
            updateNetworkArrowsSummary(networkArrows);
            return true;
        }
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

    private void updateNetworkArrowsSummary(int value) {
        String summary = value != 0
                ? getResources().getString(R.string.enabled)
                : getResources().getString(R.string.disabled);
        mNetworkArrows.setSummary(summary);
    }
}
