
package com.android.settings.beanstalk;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.app.Dialog;
import android.view.Gravity;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.graphics.Color;
import com.android.settings.Utils;
import net.margaritov.preference.colorpicker.ColorPickerPreference;
import com.android.settings.util.Helpers;

public class RamSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "RamSettings";

    private static final String RECENT_MENU_CLEAR_ALL = "recent_menu_clear_all";
    private static final String RECENT_MENU_CLEAR_ALL_LOCATION = "recent_menu_clear_all_location";
    private static final String SHOW_RECENTS_MEMORY_INDICATOR = "show_recents_memory_indicator";
    private static final String RECENTS_MEMORY_INDICATOR_LOCATION =
            "recents_memory_indicator_location";
    private static final String SHOW_RAMBAR_GB =
            "show_rambar_gb";
    private static final String LARGE_RECENT_THUMBS = "large_recent_thumbs";
    private static final String CUSTOM_RECENT_MODE = "custom_recent_mode";
    private static final String RECENT_PANEL_SHOW_TOPMOST =
            "recent_panel_show_topmost";
    private static final String RECENT_PANEL_LEFTY_MODE =
            "recent_panel_lefty_mode";
    private static final String RECENT_PANEL_SCALE =
            "recent_panel_scale";
    private static final String RECENT_PANEL_EXPANDED_MODE =
            "recent_panel_expanded_mode";
    private static final String RECENT_PANEL_BG_COLOR =
            "recent_panel_bg_color";

    private CheckBoxPreference mLargeRecentThumbs;
    private ColorPickerPreference mRecentsColor;
    private CheckBoxPreference mRecentClearAll;
    private CheckBoxPreference mRambarGB;
    private ListPreference mRecentClearAllPosition;
    private CheckBoxPreference mShowRecentsMemoryIndicator;
    private ListPreference mRecentsMemoryIndicatorPosition;
    private CheckBoxPreference mRecentsCustom;

    private ContentResolver mContentResolver;
    private Context mContext;

    private CheckBoxPreference mRecentsShowTopmost;
    private CheckBoxPreference mRecentPanelLeftyMode;
    private ListPreference mRecentPanelScale;
    private ListPreference mRecentPanelExpandedMode;
    private ColorPickerPreference mRecentPanelBgColor;

    private static final int MENU_RESET = Menu.FIRST;
    private static final int DEFAULT_BACKGROUND_COLOR = 0x00ffffff;

    @Override
    public void onResume() {
        super.onResume();
        updateSystemPreferences();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ram_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

	mContentResolver = getContentResolver();

        mRecentClearAll = (CheckBoxPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL);
        mRecentClearAll.setChecked(Settings.System.getInt(resolver,
                Settings.System.SHOW_CLEAR_RECENTS_BUTTON, 0) == 1);
        mRecentClearAll.setOnPreferenceChangeListener(this);
        mRecentClearAllPosition = (ListPreference) prefSet.findPreference(RECENT_MENU_CLEAR_ALL_LOCATION);
        String recentClearAllPosition = Settings.System.getString(resolver, Settings.System.CLEAR_RECENTS_BUTTON_LOCATION);
        if (recentClearAllPosition != null) {
            mRecentClearAllPosition.setValue(recentClearAllPosition);
        }
        mRecentClearAllPosition.setOnPreferenceChangeListener(this);

        mRambarGB = (CheckBoxPreference) prefSet.findPreference(SHOW_RAMBAR_GB);
        mRambarGB.setChecked(Settings.System.getInt(resolver,
                Settings.System.SHOW_GB_RAMBAR, 0) == 1);
        mRambarGB.setOnPreferenceChangeListener(this);

        mLargeRecentThumbs = (CheckBoxPreference) prefSet.findPreference(LARGE_RECENT_THUMBS);

        mLargeRecentThumbs.setChecked((Settings.System.getInt(mContentResolver,
                Settings.System.LARGE_RECENT_THUMBS, 0) == 1));

	mRecentsColor = (ColorPickerPreference) findPreference("recents_panel_color");
        mRecentsColor.setOnPreferenceChangeListener(this);

	boolean enableRecentsCustom = Settings.System.getBoolean(getContentResolver(),
                                      Settings.System.CUSTOM_RECENT, false);
        mRecentsCustom = (CheckBoxPreference) findPreference(CUSTOM_RECENT_MODE);
        mRecentsCustom.setChecked(enableRecentsCustom);
        mRecentsCustom.setOnPreferenceChangeListener(this);

        mShowRecentsMemoryIndicator = (CheckBoxPreference)
                prefSet.findPreference(SHOW_RECENTS_MEMORY_INDICATOR);
        mShowRecentsMemoryIndicator.setChecked(Settings.System.getInt(resolver,
                Settings.System.SHOW_RECENTS_MEMORY_INDICATOR, 0) == 1);
        mShowRecentsMemoryIndicator.setOnPreferenceChangeListener(this);
        mRecentsMemoryIndicatorPosition = (ListPreference) prefSet
                .findPreference(RECENTS_MEMORY_INDICATOR_LOCATION);
        String recentsMemoryIndicatorPosition = Settings.System.getString(
                resolver, Settings.System.RECENTS_MEMORY_INDICATOR_LOCATION);
        if (recentsMemoryIndicatorPosition != null) {
            mRecentsMemoryIndicatorPosition.setValue(recentsMemoryIndicatorPosition);
        }
        mRecentsMemoryIndicatorPosition.setOnPreferenceChangeListener(this);

        // Recent panel background color
        mRecentPanelBgColor =
                (ColorPickerPreference) findPreference(RECENT_PANEL_BG_COLOR);
        mRecentPanelBgColor.setOnPreferenceChangeListener(this);
        final int intColor = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_PANEL_BG_COLOR, 0x00ffffff);
        String hexColor = String.format("#%08x", (0x00ffffff & intColor));
        if (hexColor.equals("#00ffffff")) {
            mRecentPanelBgColor.setSummary("TRDS default");
        } else {
            mRecentPanelBgColor.setSummary(hexColor);
        }
        mRecentPanelBgColor.setNewPreviewColor(intColor);
        setHasOptionsMenu(true);

        boolean enableRecentsShowTopmost = Settings.System.getInt(getContentResolver(),
                                      Settings.System.RECENT_PANEL_SHOW_TOPMOST, 0) == 1;
        mRecentsShowTopmost = (CheckBoxPreference) findPreference(RECENT_PANEL_SHOW_TOPMOST);
        mRecentsShowTopmost.setChecked(enableRecentsShowTopmost);
        mRecentsShowTopmost.setOnPreferenceChangeListener(this);

        mRecentPanelLeftyMode =
                (CheckBoxPreference) findPreference(RECENT_PANEL_LEFTY_MODE);
        mRecentPanelLeftyMode.setOnPreferenceChangeListener(this);

        mRecentPanelScale =
                (ListPreference) findPreference(RECENT_PANEL_SCALE);
        mRecentPanelScale.setOnPreferenceChangeListener(this);

        mRecentPanelExpandedMode =
                (ListPreference) findPreference(RECENT_PANEL_EXPANDED_MODE);
        mRecentPanelExpandedMode.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mRecentClearAll) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.SHOW_CLEAR_RECENTS_BUTTON, value ? 1 : 0);
	} else if (preference == mRambarGB) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(resolver, Settings.System.SHOW_GB_RAMBAR, value ? 1 : 0);
	} else if (preference == mRecentsCustom) { // Enable||disbale Slim Recent
            Settings.System.putBoolean(getActivity().getContentResolver(),
                    Settings.System.CUSTOM_RECENT,
                    ((Boolean) objValue) ? true : false);
            Helpers.restartSystemUI();
        } else if (preference == mRecentClearAllPosition) {
            String value = (String) objValue;
            Settings.System.putString(resolver, Settings.System.CLEAR_RECENTS_BUTTON_LOCATION, value);
	} else if (preference == mRecentsColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(mContentResolver,
                    Settings.System.RECENTS_PANEL_COLOR, intHex);
	    Helpers.restartSystemUI();
        } else if (preference == mShowRecentsMemoryIndicator) {
            boolean value = (Boolean) objValue;
            Settings.System.putInt(
                    resolver, Settings.System.SHOW_RECENTS_MEMORY_INDICATOR, value ? 1 : 0);
        } else if (preference == mRecentsMemoryIndicatorPosition) {
            String value = (String) objValue;
            Settings.System.putString(
                    resolver, Settings.System.RECENTS_MEMORY_INDICATOR_LOCATION, value);
        } else if (preference == mRecentPanelScale) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_SCALE_FACTOR, value);
            return true;
        } else if (preference == mRecentPanelExpandedMode) {
            int value = Integer.parseInt((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_EXPANDED_MODE, value);
            return true;
        } else if (preference == mRecentPanelBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(objValue)));
            if (hex.equals("#00ffffff")) {
                preference.setSummary("TRDS default");
            } else {
                preference.setSummary(hex);
            }
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_BG_COLOR,
                    intHex);
            return true;
        } else if (preference == mRecentPanelLeftyMode) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_GRAVITY,
                    ((Boolean) objValue) ? Gravity.LEFT : Gravity.RIGHT);
            return true;
        } else if (preference == mRecentsShowTopmost) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.RECENT_PANEL_SHOW_TOPMOST,
                    ((Boolean) objValue) ? 1 : 0);
            return true;
        } else {
            return false;
        }

        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
        if (preference == mLargeRecentThumbs) {
            value = mLargeRecentThumbs.isChecked();
            Settings.System.putInt(mContentResolver,
                    Settings.System.LARGE_RECENT_THUMBS, value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void updateSystemPreferences() {
        final boolean recentLeftyMode = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_PANEL_GRAVITY, Gravity.RIGHT) == Gravity.LEFT;
        mRecentPanelLeftyMode.setChecked(recentLeftyMode);

        final int recentScale = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_PANEL_SCALE_FACTOR, 100);
        mRecentPanelScale.setValue(recentScale + "");

        final int recentExpandedMode = Settings.System.getInt(getContentResolver(),
                Settings.System.RECENT_PANEL_EXPANDED_MODE, 0);
        mRecentPanelExpandedMode.setValue(recentExpandedMode + "");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset_default_message)
                .setIcon(R.drawable.ic_settings_backup)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                resetToDefault();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetToDefault() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setTitle(R.string.shortcut_action_reset);
        alertDialog.setMessage(R.string.qs_style_reset_message);
        alertDialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                resetValues();
            }
        });
        alertDialog.setNegativeButton(R.string.cancel, null);
        alertDialog.create().show();
    }

    private void resetValues() {
        Settings.System.putInt(getContentResolver(),
                Settings.System.RECENT_PANEL_BG_COLOR, DEFAULT_BACKGROUND_COLOR);
        mRecentPanelBgColor.setNewPreviewColor(DEFAULT_BACKGROUND_COLOR);
        mRecentPanelBgColor.setSummary("TRDS default");
    }
}
