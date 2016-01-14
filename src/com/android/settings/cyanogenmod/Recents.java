/*
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

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import com.android.internal.logging.MetricsLogger;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.util.Helpers;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

public class Recents extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String SHOW_CLEAR_ALL_RECENTS = "show_clear_all_recents";
    private static final String RECENTS_CLEAR_ALL_LOCATION = "recents_clear_all_location";
    private static final String RECENTS_CLEAR_ALL = "show_clear_all_recents";
    private static final String RECENTS_CLEAR_ALL_DISMISS_ALL = "recents_clear_all_dismiss_all";
    private static final String RECENTS_SHOW_SEARCH_BAR = "recents_show_search_bar";
    private static final String RECENTS_MEM_DISPLAY = "systemui_recents_mem_display";
    private static final String RECENTS_FULL_SCREEN = "recents_full_screen";
    private static final String RECENTS_FULL_SCREEN_CLOCK = "recents_full_screen_clock";
    private static final String RECENTS_FULL_SCREEN_DATE = "recents_full_screen_date";

    private static final String KEY_OMNISWITCH = "omniswitch";
    public static final String OMNISWITCH_PACKAGE_NAME = "org.omnirom.omniswitch";

    private Preference mOmniSwitch;
    private SwitchPreference mRecentsClearAll;
    private ListPreference mRecentsClearAllLocation;
    private SwitchPreference mRecentsClearAllDismissAll;
    private SwitchPreference mRecentsShowSearchBar;
    private SwitchPreference mRecentsMemDisplay;
    private SwitchPreference mRecentsFullScreen;
    private SwitchPreference mRecentsFullScreenClock;
    private SwitchPreference mRecentsFullScreenDate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.recents);
        ContentResolver resolver = getActivity().getContentResolver();
        PreferenceScreen prefSet = getPreferenceScreen();
	PackageManager pm = getPackageManager();

        mRecentsClearAll = (SwitchPreference) prefSet.findPreference(SHOW_CLEAR_ALL_RECENTS);

        mRecentsClearAllLocation = (ListPreference) prefSet.findPreference(RECENTS_CLEAR_ALL_LOCATION);
        int location = Settings.System.getIntForUser(resolver,
                Settings.System.RECENTS_CLEAR_ALL_LOCATION, 3, UserHandle.USER_CURRENT);
        mRecentsClearAllLocation.setValue(String.valueOf(location));
        mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntry());
        mRecentsClearAllLocation.setOnPreferenceChangeListener(this);

	mOmniSwitch = (Preference)
                prefSet.findPreference(KEY_OMNISWITCH);
        if (!Helpers.isPackageInstalled(OMNISWITCH_PACKAGE_NAME, pm)) {
            prefSet.removePreference(mOmniSwitch);
        }

	mRecentsClearAll = (SwitchPreference) prefSet.findPreference(RECENTS_CLEAR_ALL);
        mRecentsClearAllDismissAll = (SwitchPreference) prefSet.findPreference(RECENTS_CLEAR_ALL_DISMISS_ALL);
        mRecentsShowSearchBar = (SwitchPreference) prefSet.findPreference(RECENTS_SHOW_SEARCH_BAR);
        mRecentsMemDisplay = (SwitchPreference) prefSet.findPreference(RECENTS_MEM_DISPLAY);
        mRecentsFullScreen = (SwitchPreference) prefSet.findPreference(RECENTS_FULL_SCREEN);
        mRecentsFullScreenClock = (SwitchPreference) prefSet.findPreference(RECENTS_FULL_SCREEN_CLOCK);
        mRecentsFullScreenDate = (SwitchPreference) prefSet.findPreference(RECENTS_FULL_SCREEN_DATE);

        updateSettingsVisibility();

    }

    @Override
    protected int getMetricsCategory() {
        // todo add a constant in MetricsLogger.java
        return MetricsLogger.MAIN_SETTINGS;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mRecentsClearAllLocation) {
            int location = Integer.valueOf((String) newValue);
            int index = mRecentsClearAllLocation.findIndexOfValue((String) newValue);
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.RECENTS_CLEAR_ALL_LOCATION, location, UserHandle.USER_CURRENT);
            mRecentsClearAllLocation.setSummary(mRecentsClearAllLocation.getEntries()[index]);
            return true;
        }
        return false;
    }

    private void updateSettingsVisibility() {
        ContentResolver resolver = getActivity().getContentResolver();
        if ((Settings.System.getInt(resolver,
                Settings.System.RECENTS_USE_OMNISWITCH, 0) == 1) ||
                (Settings.System.getInt(resolver,
                Settings.System.USE_SLIM_RECENTS, 0) == 1)) {
            mRecentsClearAllLocation.setEnabled(false);
            mRecentsClearAll.setEnabled(false);
            mRecentsClearAllDismissAll.setEnabled(false);
            mRecentsShowSearchBar.setEnabled(false);
            mRecentsMemDisplay.setEnabled(false);
            mRecentsFullScreen.setEnabled(false);
            mRecentsFullScreenClock.setEnabled(false);
            mRecentsFullScreenDate.setEnabled(false);
            Toast.makeText(getActivity(), getString(R.string.stock_recents_disabled),
                Toast.LENGTH_LONG).show();
        }
    }
}
