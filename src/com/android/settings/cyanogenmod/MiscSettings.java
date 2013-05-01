/*
* Copyright (C) 2012 The CyanogenMod Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.settings.cyanogenmod;

import android.app.ActivityManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;
import com.android.settings.util.CMDProcessor;

    public class MiscSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
    private static final String KEY_HIGH_END_GFX = "high_end_gfx";
    private static final String USE_HIGH_END_GFX_PROP = "persist.sys.use_high_end_gfx";
    private static final String KEY_RECENTS_RAM_BAR = "recents_ram_bar";
    private static final String PREF_VIBRATE_NOTIF_EXPAND = "vibrate_notif_expand";
    private static final String KEY_LOW_BATTERY_WARNING_POLICY = "pref_low_battery_warning_policy";
    private static final String HIDDEN_STATUSBAR_PULLDOWN = "hidden_statusbar_pulldown";
    private static final String HIDDEN_STATUSBAR_PULLDOWN_TIMEOUT = "hidden_statusbar_pulldown_timeout";

    private CheckBoxPreference mHighEndGfx;
    private Preference mRamBar;
    CheckBoxPreference mVibrateOnExpand;
    private ListPreference mLowBatteryWarning;
    CheckBoxPreference mHiddenStatusbarPulldown;
    ListPreference mHiddenStatusbarPulldownTimeout;
    

    private ContentResolver mContentResolver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.misc_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        mContentResolver = getActivity().getApplicationContext().getContentResolver();

	   boolean isHighEndGfx = ActivityManager.isHighEndGfx();
            mHighEndGfx = (CheckBoxPreference) findPreference(KEY_HIGH_END_GFX);
            if(isHighEndGfx) {
                getPreferenceScreen().removePreference(mHighEndGfx);
            } else {
                mHighEndGfx.setChecked(SystemProperties.getBoolean(USE_HIGH_END_GFX_PROP,
false));
            }

	mLowBatteryWarning = (ListPreference) findPreference(KEY_LOW_BATTERY_WARNING_POLICY);
        int lowBatteryWarning = Settings.System.getInt(getActivity().getContentResolver(),
                                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, 3);
        mLowBatteryWarning.setValue(String.valueOf(lowBatteryWarning));
        mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntry());
        mLowBatteryWarning.setOnPreferenceChangeListener(this);

	mHiddenStatusbarPulldown = (CheckBoxPreference) prefSet.findPreference(HIDDEN_STATUSBAR_PULLDOWN);
        mHiddenStatusbarPulldown.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.HIDDEN_STATUSBAR_PULLDOWN, 0) == 1));

	mHiddenStatusbarPulldownTimeout = (ListPreference) findPreference(HIDDEN_STATUSBAR_PULLDOWN_TIMEOUT);
        mHiddenStatusbarPulldownTimeout.setOnPreferenceChangeListener(this);
        mHiddenStatusbarPulldownTimeout.setValue(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.HIDDEN_STATUSBAR_PULLDOWN_TIMEOUT, 10000) + "");

	mVibrateOnExpand = (CheckBoxPreference) findPreference(PREF_VIBRATE_NOTIF_EXPAND);
        mVibrateOnExpand.setChecked(Settings.System.getBoolean(mContext.getContentResolver(),
                Settings.System.VIBRATE_NOTIF_EXPAND, true));

	mRamBar = findPreference(KEY_RECENTS_RAM_BAR);
        updateRamBar();
    }

    private void updateRamBar() {
        int ramBarMode = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.RECENTS_RAM_BAR_MODE, 0);
        if (ramBarMode != 0)
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_enabled));
        else
            mRamBar.setSummary(getResources().getString(R.string.ram_bar_color_disabled));
    }

    @Override
    public void onResume() {
        super.onResume();
	updateRamBar();
    }

     @Override
     public void onPause() {
         super.onResume();
        updateRamBar();
     }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
	if (preference == mLowBatteryWarning) {
            int lowBatteryWarning = Integer.valueOf((String) objValue);
            int index = mLowBatteryWarning.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.POWER_UI_LOW_BATTERY_WARNING_POLICY, lowBatteryWarning);
            mLowBatteryWarning.setSummary(mLowBatteryWarning.getEntries()[index]);
            return true;
	} else if (preference == mHiddenStatusbarPulldownTimeout) {
            int val = Integer.parseInt((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.HIDDEN_STATUSBAR_PULLDOWN_TIMEOUT, val);
            return true;
        }

        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
if (preference == mHighEndGfx) {
SystemProperties.set(USE_HIGH_END_GFX_PROP, mHighEndGfx.isChecked() ? "1" : "0");
	} else if (preference == mVibrateOnExpand) {
            Settings.System.putBoolean(mContext.getContentResolver(),
                    Settings.System.VIBRATE_NOTIF_EXPAND,
                    ((CheckBoxPreference) preference).isChecked());
            Helpers.restartSystemUI();
            return true;
	} else if (preference == mHiddenStatusbarPulldown) {
            value = mHiddenStatusbarPulldown.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.HIDDEN_STATUSBAR_PULLDOWN, value ? 1 : 0);
            return true;
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }
}
