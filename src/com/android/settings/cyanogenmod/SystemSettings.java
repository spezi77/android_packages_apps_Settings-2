/*
 * Copyright (C) 2012 The CyanogenMod project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cyanogenmod;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

public class SystemSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SystemSettings";

    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";

    private PreferenceScreen mNotificationPulse;
    private PreferenceScreen mBatteryPulse;
    private static final String KEY_KILL_APP_LONGPRESS_TIMEOUT = "kill_app_longpress_timeout";

    private ListPreference mKillAppLongpressTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.system_settings);
        mNotificationPulse = (PreferenceScreen) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                getPreferenceScreen().removePreference(mNotificationPulse);
            } else {
                updateLightPulseDescription();

	mKillAppLongpressTimeout = (ListPreference) findPreference(KEY_KILL_APP_LONGPRESS_TIMEOUT);
        mKillAppLongpressTimeout.setOnPreferenceChangeListener(this);

        int statusKillAppLongpressTimeout = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                 Settings.System.KILL_APP_LONGPRESS_TIMEOUT, 1500);
        mKillAppLongpressTimeout.setValue(String.valueOf(statusKillAppLongpressTimeout));
        mKillAppLongpressTimeout.setSummary(mKillAppLongpressTimeout.getEntry());

            }
        }

        mBatteryPulse = (PreferenceScreen) findPreference(KEY_BATTERY_LIGHT);
        if (mBatteryPulse != null) {
            if (getResources().getBoolean(
                    com.android.internal.R.bool.config_intrusiveBatteryLed) == false) {
                getPreferenceScreen().removePreference(mBatteryPulse);
            } else {
                updateBatteryPulseDescription();
            }
        }

        // Only show the hardware keys config on a device that does not have a navbar
        // Only show the navigation bar config on phones that has a navigation bar
  //      boolean removeKeys = false;
  //      boolean removeNavbar = false;
  //      IWindowManager windowManager = IWindowManager.Stub.asInterface(
  //              ServiceManager.getService(Context.WINDOW_SERVICE));
  //      try {
  //          if (windowManager.hasNavigationBar()) {
  //              removeKeys = true;
  //              if (Utils.isTablet(getActivity())) {
  //                  removeNavbar = true;
  //             }
  //          } else {
  //              removeNavbar = true;
  //          }
  //      } catch (RemoteException e) {
            // Do nothing
   //     }

        // Act on the above
      //  if (removeKeys) {
       //     getPreferenceScreen().removePreference(findPreference(KEY_HARDWARE_KEYS));
      //  }
     //   if (removeNavbar) {
       //     getPreferenceScreen().removePreference(findPreference(KEY_NAVIGATION_BAR));
     //   }

    }

    private void updateLightPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mNotificationPulse.setSummary(getString(R.string.notification_light_disabled));
        }
    }

    private void updateBatteryPulseDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
            mBatteryPulse.setSummary(getString(R.string.notification_light_enabled));
        } else {
            mBatteryPulse.setSummary(getString(R.string.notification_light_disabled));
        }
     }

    @Override
    public void onResume() {
        super.onResume();
        updateLightPulseDescription();
        updateBatteryPulseDescription();
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        if (preference == mKillAppLongpressTimeout) {
            int statusKillAppLongpressTimeout = Integer.valueOf((String) objValue);
            int index = mKillAppLongpressTimeout.findIndexOfValue((String) objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.KILL_APP_LONGPRESS_TIMEOUT, statusKillAppLongpressTimeout);
            mKillAppLongpressTimeout.setSummary(mKillAppLongpressTimeout.getEntries()[index]);
            return true;

        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

}
