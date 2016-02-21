/*
* Copyright (C) 2014 The CyanogenMod Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.Log;
import com.android.settings.util.Helpers;

import java.util.List;
import java.util.ArrayList;

import com.android.internal.logging.MetricsLogger;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SeekBarPreferenceCham;

import cyanogenmod.providers.CMSettings;

public class NotificationDrawerSettings extends SettingsPreferenceFragment  implements Preference.OnPreferenceChangeListener{

    private static final String ENABLE_TASK_MANAGER = "enable_task_manager";

    private static final String PREF_SMART_PULLDOWN = "smart_pulldown";
    private static final String STATUS_BAR_QUICK_QS_PULLDOWN = "qs_quick_pulldown";
    private static final String PREF_STATUS_BAR_HEADER_FONT_STYLE = "status_bar_header_font_style";
    private static final String CUSTOM_HEADER_IMAGE = "status_bar_custom_header";
    private static final String DAYLIGHT_HEADER_PACK = "daylight_header_pack";
    private static final String DEFAULT_HEADER_PACKAGE = "com.android.systemui";
    private static final String CUSTOM_HEADER_IMAGE_SHADOW = "status_bar_custom_header_shadow";

    private SwitchPreference mEnableTaskManager;
    private ListPreference mSmartPulldown;
    private ListPreference mQuickPulldown;
    private ListPreference mStatusBarHeaderFontStyle;
    private ListPreference mNumColumns;
    private ListPreference mNumRows;
    private ListPreference mDaylightHeaderPack;
    private SwitchPreference mCustomHeaderImage;
    private SeekBarPreferenceCham mHeaderShadow;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.notification_drawer_settings);
        
        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();
	Resources res = getResources();

	mQuickPulldown = (ListPreference) findPreference(STATUS_BAR_QUICK_QS_PULLDOWN);

	mEnableTaskManager = (SwitchPreference) findPreference(ENABLE_TASK_MANAGER);
        mEnableTaskManager.setChecked((Settings.System.getInt(resolver,
                Settings.System.ENABLE_TASK_MANAGER, 0) == 1));

	// Number of QS Columns 3,4,5
        mNumColumns = (ListPreference) findPreference("sysui_qs_num_columns");
        int numColumns = Settings.System.getIntForUser(resolver,
                Settings.System.QS_NUM_TILE_COLUMNS, getDefaultNumColumns(),
                UserHandle.USER_CURRENT);
        mNumColumns.setValue(String.valueOf(numColumns));
        updateNumColumnsSummary(numColumns);
        mNumColumns.setOnPreferenceChangeListener(this);

	// Number of QS Rows 3,4
        mNumRows = (ListPreference) findPreference("sysui_qs_num_rows");
        int numRows = Settings.System.getIntForUser(resolver,
                Settings.System.QS_NUM_TILE_ROWS, getDefaultNumRows(),
                UserHandle.USER_CURRENT);
        mNumRows.setValue(String.valueOf(numRows));
        updateNumRowsSummary(numRows);
        mNumRows.setOnPreferenceChangeListener(this);

	// header image packs
        final boolean customHeaderImage = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER, 0) == 1;
        mCustomHeaderImage = (SwitchPreference) findPreference(CUSTOM_HEADER_IMAGE);
        mCustomHeaderImage.setChecked(customHeaderImage);

         String imageHeaderPackage = Settings.System.getString(getContentResolver(),
                 Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK);
         if (imageHeaderPackage == null) {
             imageHeaderPackage = DEFAULT_HEADER_PACKAGE;
         }
         mDaylightHeaderPack = (ListPreference) findPreference(DAYLIGHT_HEADER_PACK);
         List<String> entries = new ArrayList<String>();
         List<String> values = new ArrayList<String>();
         getAvailableHeaderPacks(entries, values);
         mDaylightHeaderPack.setEntries(entries.toArray(new String[entries.size()]));
         mDaylightHeaderPack.setEntryValues(values.toArray(new String[values.size()]));
 
         int valueIndexHeader = mDaylightHeaderPack.findIndexOfValue(imageHeaderPackage);
         if (valueIndexHeader == -1) {
             // no longer found
             imageHeaderPackage = DEFAULT_HEADER_PACKAGE;
             Settings.System.putString(getContentResolver(),
                     Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, imageHeaderPackage);
             valueIndexHeader = mDaylightHeaderPack.findIndexOfValue(imageHeaderPackage);
         }
         mDaylightHeaderPack.setValueIndex(valueIndexHeader >= 0 ? valueIndexHeader : 0);
         mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntry());
         mDaylightHeaderPack.setOnPreferenceChangeListener(this);
         
	mHeaderShadow = (SeekBarPreferenceCham) findPreference(CUSTOM_HEADER_IMAGE_SHADOW);
        final int headerShadow = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, 0);
        mHeaderShadow.setValue((int)((headerShadow / 255) * 100));
        mHeaderShadow.setOnPreferenceChangeListener(this);

	// Status bar header font style
        mStatusBarHeaderFontStyle = (ListPreference) findPreference(PREF_STATUS_BAR_HEADER_FONT_STYLE);
        mStatusBarHeaderFontStyle.setOnPreferenceChangeListener(this);
        mStatusBarHeaderFontStyle.setValue(Integer.toString(Settings.System.getIntForUser(resolver,
                Settings.System.STATUS_BAR_HEADER_FONT_STYLE, 0, UserHandle.USER_CURRENT)));
        mStatusBarHeaderFontStyle.setSummary(mStatusBarHeaderFontStyle.getEntry());

        // Smart Pulldown
        mSmartPulldown = (ListPreference) findPreference(PREF_SMART_PULLDOWN);
        mSmartPulldown.setOnPreferenceChangeListener(this);
        int smartPulldown = Settings.System.getInt(getContentResolver(),
                Settings.System.QS_SMART_PULLDOWN, 0);
        mSmartPulldown.setValue(String.valueOf(smartPulldown));
        updateSmartPulldownSummary(smartPulldown);

 	int quickPulldown = CMSettings.System.getInt(resolver,
                CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN, 1);
        mQuickPulldown.setValue(String.valueOf(quickPulldown));
        if (quickPulldown == 0) {
            // quick pulldown deactivated
            mQuickPulldown.setSummary(res.getString(R.string.status_bar_quick_qs_pulldown_off));
        } else {
            String direction = res.getString(quickPulldown == 2
                    ? R.string.status_bar_quick_qs_pulldown_left
                    : R.string.status_bar_quick_qs_pulldown_right);
            mQuickPulldown.setSummary(
                    res.getString(R.string.status_bar_quick_qs_pulldown_summary, direction));
        }
        mQuickPulldown.setOnPreferenceChangeListener(this);

    }

    @Override
    protected int getMetricsCategory() {
        return MetricsLogger.MAIN_SETTINGS;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ContentResolver resolver = getActivity().getContentResolver();
	Resources res = getResources();
	if (preference == mEnableTaskManager) {
            boolean value = ((SwitchPreference)preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ENABLE_TASK_MANAGER, value ? 1:0);
		    Helpers.restartSystemUI();
            return true;
	} else if (preference == mDaylightHeaderPack) {
             String value = (String) newValue;
             Settings.System.putString(getContentResolver(),
                     Settings.System.STATUS_BAR_DAYLIGHT_HEADER_PACK, value);
             int valueIndex = mDaylightHeaderPack.findIndexOfValue(value);
             mDaylightHeaderPack.setSummary(mDaylightHeaderPack.getEntries()[valueIndex]);
             return true;
	} else if (preference == mHeaderShadow) {
            Integer headerShadow = (Integer) newValue;
            int realHeaderValue = (int) (((double) headerShadow / 100) * 255);
            Settings.System.putInt(getContentResolver(),
              Settings.System.STATUS_BAR_CUSTOM_HEADER_SHADOW, realHeaderValue);
	    return true;
	} else if (preference == mNumColumns) {
            int numColumns = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.QS_NUM_TILE_COLUMNS,
                    numColumns, UserHandle.USER_CURRENT);
            updateNumColumnsSummary(numColumns);
            return true;
	} else if (preference == mNumRows) {
            int numRows = Integer.valueOf((String) newValue);
            Settings.System.putIntForUser(resolver, Settings.System.QS_NUM_TILE_ROWS,
                    numRows, UserHandle.USER_CURRENT);
            updateNumRowsSummary(numRows);
            return true;
        } else if (preference == mSmartPulldown) {
            int smartPulldown = Integer.valueOf((String) newValue);
            Settings.System.putInt(getContentResolver(), Settings.System.QS_SMART_PULLDOWN,
                    smartPulldown);
            updateSmartPulldownSummary(smartPulldown);
            return true;
	} else if (preference == mStatusBarHeaderFontStyle) {
            int val = Integer.parseInt((String) newValue);
            int index = mStatusBarHeaderFontStyle.findIndexOfValue((String) newValue);
            Settings.System.putIntForUser(resolver,
                    Settings.System.STATUS_BAR_HEADER_FONT_STYLE, val, UserHandle.USER_CURRENT);
            mStatusBarHeaderFontStyle.setSummary(mStatusBarHeaderFontStyle.getEntries()[index]);
            return true;
        } else if (preference == mQuickPulldown) {
            int quickPulldown = Integer.valueOf((String) newValue);
            CMSettings.System.putInt(resolver, CMSettings.System.STATUS_BAR_QUICK_QS_PULLDOWN,
                    quickPulldown);
            if (quickPulldown == 0) {
                // quick pulldown deactivated
                mQuickPulldown.setSummary(res.getString(R.string.status_bar_quick_qs_pulldown_off));
            } else {
                String direction = res.getString(quickPulldown == 2
                        ? R.string.status_bar_quick_qs_pulldown_left
                        : R.string.status_bar_quick_qs_pulldown_right);
                mQuickPulldown.setSummary(
                        res.getString(R.string.status_bar_quick_qs_pulldown_summary, direction));
            }
            return true;
	}

        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	if (preference == mCustomHeaderImage) {
                final boolean value = ((SwitchPreference)preference).isChecked();
                Settings.System.putInt(getContentResolver(),
                        Settings.System.STATUS_BAR_CUSTOM_HEADER, value ? 1 : 0);
                return true;
            }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateSmartPulldownSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            // Smart pulldown deactivated
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_off));
        } else {
            String type = null;
            switch (value) {
                case 1:
                    type = res.getString(R.string.smart_pulldown_dismissable);
                    break;
                case 2:
                    type = res.getString(R.string.smart_pulldown_persistent);
                    break;
                default:
                    type = res.getString(R.string.smart_pulldown_all);
                    break;
            }
            // Remove title capitalized formatting
            type = type.toLowerCase();
            mSmartPulldown.setSummary(res.getString(R.string.smart_pulldown_summary, type));
        }
    }

    private void updateNumColumnsSummary(int numColumns) {
        String prefix = (String) mNumColumns.getEntries()[mNumColumns.findIndexOfValue(String
                .valueOf(numColumns))];
        mNumColumns.setSummary(getResources().getString(R.string.qs_num_columns_showing, prefix));
    }

    private void updateNumRowsSummary(int numRows) {
        String prefix = (String) mNumRows.getEntries()[mNumRows.findIndexOfValue(String
                .valueOf(numRows))];
        mNumRows.setSummary(getResources().getString(R.string.qs_num_rows_showing, prefix));
    }

    private int getDefaultNumColumns() {
        try {
            Resources res = getActivity().getPackageManager()
                    .getResourcesForApplication("com.android.systemui");
            int val = res.getInteger(res.getIdentifier("quick_settings_num_columns", "integer",
                    "com.android.systemui")); // better not be larger than 5, that's as high as the
                                              // list goes atm
            return Math.max(1, val);
        } catch (Exception e) {
            return 3;
        }
    }

    private int getDefaultNumRows() {
        try {
            Resources res = getActivity().getPackageManager()
                    .getResourcesForApplication("com.android.systemui");
            int val = res.getInteger(res.getIdentifier("quick_settings_num_rows", "integer",
                    "com.android.systemui")); // better not be larger than 4, that's as high as the
                                              // list goes atm
            return Math.max(1, val);
        } catch (Exception e) {
            return 3;
        }
    }

    private void getAvailableHeaderPacks(List<String> entries, List<String> values) {
        Intent i = new Intent();
        PackageManager packageManager = getPackageManager();
        i.setAction("org.omnirom.DaylightHeaderPack");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            if (packageName.equals(DEFAULT_HEADER_PACKAGE)) {
                values.add(0, packageName);
            } else {
                values.add(packageName);
            }
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = r.activityInfo.packageName;
            }
            if (packageName.equals(DEFAULT_HEADER_PACKAGE)) {
                entries.add(0, label);
            } else {
                entries.add(label);
            }
        }
        i.setAction("org.omnirom.DaylightHeaderPack1");
        for (ResolveInfo r : packageManager.queryIntentActivities(i, 0)) {
            String packageName = r.activityInfo.packageName;
            values.add(packageName  + "/" + r.activityInfo.name);
            String label = r.activityInfo.loadLabel(getPackageManager()).toString();
            if (label == null) {
                label = packageName;
            }
            entries.add(label);
        }
    }
}
