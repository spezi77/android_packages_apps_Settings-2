/*
 * Copyright (C) 2012 The CyanogenMod Project
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.Context;
import android.content.res.Resources;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import com.android.settings.util.Helpers;
import com.android.settings.widget.SeekBarPreference;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.util.Helpers;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class StatusBar extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "StatusBar";
    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
 //   private static final String STATUS_BAR_SIGNAL = "status_bar_signal";
    private static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String STATUS_BAR_TRANSPARENCY = "status_bar_transparency";
    private static final String STATUS_BAR_CATEGORY_GENERAL = "status_bar_general";
    private static final String PREF_STATUSBAR_BACKGROUND_STYLE = "statusbar_background_style";
    private static final String PREF_STATUSBAR_BACKGROUND_COLOR = "statusbar_background_color";

    private ColorPickerPreference mColorPicker;
    private ListPreference mStatusBarBattery;
  //  private ListPreference mStatusBarCmSignal;
    private SeekBarPreference mStatusbarTransparency;
    private CheckBoxPreference mStatusBarBrightnessControl;
    private CheckBoxPreference mStatusBarNotifCount;
    private PreferenceScreen mClockStyle;
    private PreferenceCategory mPrefCategoryGeneral;

    ColorPickerPreference mStatusbarBgColor;
    ListPreference mStatusbarBgStyle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.status_bar);

        PreferenceScreen prefSet = getPreferenceScreen();

        mStatusBarBrightnessControl = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);
        mStatusBarBattery = (ListPreference) prefSet.findPreference(STATUS_BAR_BATTERY);

	float defaultAlpha;
        try{
            defaultAlpha = Settings.System.getFloat(getActivity()
                     .getContentResolver(), Settings.System.STATUS_BAR_TRANSPARENCY);
        } catch (Exception e) {
            defaultAlpha = 0.0f;
                    Settings.System.putFloat(getActivity().getContentResolver(), Settings.System.STATUS_BAR_TRANSPARENCY, 0.0f);
        }
        mStatusbarTransparency = (SeekBarPreference) prefSet.findPreference(STATUS_BAR_TRANSPARENCY);
        mStatusbarTransparency.setProperty(Settings.System.STATUS_BAR_TRANSPARENCY);
        mStatusbarTransparency.setInitValue((int) (defaultAlpha * 100));
        mStatusbarTransparency.setOnPreferenceChangeListener(this);

        mStatusBarBrightnessControl.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, 0) == 1));

        try {
            if (Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                mStatusBarBrightnessControl.setEnabled(false);
                mStatusBarBrightnessControl.setSummary(R.string.status_bar_toggle_info);
            }
        } catch (SettingNotFoundException e) {
        }

        int statusBarBattery = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_BATTERY, 0);
        mStatusBarBattery.setValue(String.valueOf(statusBarBattery));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        mStatusBarNotifCount = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NOTIF_COUNT);
        mStatusBarNotifCount.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF_COUNT, 0) == 1));

        mPrefCategoryGeneral = (PreferenceCategory) findPreference(STATUS_BAR_CATEGORY_GENERAL);

	mClockStyle = (PreferenceScreen) prefSet.findPreference("clock_style_pref");
        if (mClockStyle != null) {
            updateClockStyleDescription();
        }

        if (Utils.isTablet(getActivity())) {
            mPrefCategoryGeneral.removePreference(mStatusBarBrightnessControl);
        }

        mStatusbarBgColor = (ColorPickerPreference) prefSet.findPreference(PREF_STATUSBAR_BACKGROUND_COLOR);
        mStatusbarBgColor.setOnPreferenceChangeListener(this);

        mStatusbarBgStyle = (ListPreference) prefSet.findPreference(PREF_STATUSBAR_BACKGROUND_STYLE);
        mStatusbarBgStyle.setOnPreferenceChangeListener(this);

        updateVisibility();
    }

    private void updateVisibility() {
        int visible = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_BACKGROUND_STYLE, 2);
        if (visible == 2) {
            mStatusbarBgColor.setEnabled(false);
        } else {
            mStatusbarBgColor.setEnabled(true);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        if (preference == mStatusBarBattery) {
            int statusBarBattery = Integer.valueOf((String) newValue);
            int index = mStatusBarBattery.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BATTERY, statusBarBattery);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            return true;
        } else if (preference == mStatusbarTransparency) {
            float val = Float.parseFloat((String) newValue);
            Log.e("R", "value: " + val / 100);
            Settings.System.putFloat(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_TRANSPARENCY,
                    val / 100);
            return true;
        } else if (preference == mStatusbarBgStyle) {
            int value = Integer.valueOf((String) newValue);
            int index = mStatusbarBgStyle.findIndexOfValue((String) newValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUSBAR_BACKGROUND_STYLE, value);
            preference.setSummary(mStatusbarBgStyle.getEntries()[index]);
            updateVisibility();
            return true;
        } else if (preference == mStatusbarBgColor) {
            String hex = ColorPickerPreference.convertToARGB(Integer.valueOf(String
                    .valueOf(newValue)));
            preference.setSummary(hex);

            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_BACKGROUND_COLOR, intHex);
            Log.e("BAKED", intHex + "");
 //       } else if (preference == mStatusBarCmSignal) {
//            int signalStyle = Integer.valueOf((String) newValue);
//            int index = mStatusBarCmSignal.findIndexOfValue((String) newValue);
//            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
//                    Settings.System.STATUS_BAR_SIGNAL_TEXT, signalStyle);
//            mStatusBarCmSignal.setSummary(mStatusBarCmSignal.getEntries()[index]);
//            return true;
        }
        return false;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;

        if (preference == mStatusBarBrightnessControl) {
            value = mStatusBarBrightnessControl.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_BRIGHTNESS_CONTROL, value ? 1 : 0);
            return true;
        } else if (preference == mStatusBarNotifCount) {
            value = mStatusBarNotifCount.isChecked();
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NOTIF_COUNT, value ? 1 : 0);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateClockStyleDescription() {
        if (Settings.System.getInt(getActivity().getContentResolver(),
               Settings.System.STATUS_BAR_CLOCK, 0) == 1) {
            mClockStyle.setSummary(getString(R.string.clock_enabled));
        } else {
            mClockStyle.setSummary(getString(R.string.clock_disabled));
         }
     }

@Override
    public void onResume() {
        super.onResume();
        updateClockStyleDescription();
    }

}
