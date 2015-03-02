package com.android.settings.beanstalk;

import android.app.AlertDialog;
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
import android.content.DialogInterface;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.text.Spannable;
import android.text.TextUtils;
import android.widget.EditText;
import com.android.settings.widget.SeekBarPreferenceCham;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class StatusBarSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "StatusBarSettings";
    private static final String KEY_STATUS_BAR_CLOCK = "clock_style_pref";
    private static final String KEY_CARRIERLABEL_PREFERENCE = "carrier_options";
    private static final String KEY_STATUS_BAR_NETWORK_ARROWS= "status_bar_show_network_activity";
    private static final String KEY_STATUS_BAR_GREETING = "status_bar_greeting";
    private static final String KEY_STATUS_BAR_GREETING_TIMEOUT = "status_bar_greeting_timeout";

    private PreferenceScreen mClockStyle;
    private PreferenceScreen mCarrierLabel;
    private SwitchPreference mNetworkArrows;
    private SwitchPreference mStatusBarGreeting;
    private SeekBarPreferenceCham mStatusBarGreetingTimeout;

    private String mCustomGreetingText = "";

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

        // Greeting
        mStatusBarGreeting = (SwitchPreference) prefSet.findPreference(KEY_STATUS_BAR_GREETING);
        mCustomGreetingText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_GREETING);
        boolean greeting = mCustomGreetingText != null && !TextUtils.isEmpty(mCustomGreetingText);
        mStatusBarGreeting.setChecked(greeting);

	mStatusBarGreetingTimeout =
		(SeekBarPreferenceCham) prefSet.findPreference(KEY_STATUS_BAR_GREETING_TIMEOUT);
	int statusBarGreetingTimeout = Settings.System.getInt(getContentResolver(),
		Settings.System.STATUS_BAR_GREETING_TIMEOUT, 400);
	mStatusBarGreetingTimeout.setValue(statusBarGreetingTimeout / 1);
	mStatusBarGreetingTimeout.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
	if (preference == mNetworkArrows) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY,
                    (Boolean) objValue ? 1 : 0);
            int networkArrows = Settings.System.getInt(getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_NETWORK_ACTIVITY, 1);
            updateNetworkArrowsSummary(networkArrows);
            return true;
	} else if (preference == mStatusBarGreetingTimeout) {
	    int timeout = (Integer) objValue;
	    Settings.System.putInt(getActivity().getContentResolver(),
		Settings.System.STATUS_BAR_GREETING_TIMEOUT, timeout * 1);
	    return true;
        }
        return false;
    }

    @Override
    public void onResume() {
	super.onResume();
	updateClockStyleDescription();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
       if (preference == mStatusBarGreeting) {
           boolean enabled = mStatusBarGreeting.isChecked();
           if (enabled) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

                alert.setTitle(R.string.status_bar_greeting_title);
                alert.setMessage(R.string.status_bar_greeting_dialog);

                // Set an EditText view to get user input
                final EditText input = new EditText(getActivity());
                input.setText(mCustomGreetingText != null ? mCustomGreetingText :
			getResources().getString(R.string.status_bar_greeting_main));
                alert.setView(input);
                alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = ((Spannable) input.getText()).toString();
                        Settings.System.putString(getActivity().getContentResolver(),
                                Settings.System.STATUS_BAR_GREETING, value);
                        updateCheckState(value);
                    }
                });
                alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });

                alert.show();
            } else {
                Settings.System.putString(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_GREETING, "");
            }
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateCheckState(String value) {
        if (value == null || TextUtils.isEmpty(value)) mStatusBarGreeting.setChecked(false);
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
