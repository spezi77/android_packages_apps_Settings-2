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
import android.content.ContentResolver;
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
    private static final String STATUS_BAR_BRIGHTNESS_CONTROL = "status_bar_brightness_control";
    private static final String STATUS_BAR_NOTIF_COUNT = "status_bar_notif_count";
    private static final String STATUS_BAR_CATEGORY_GENERAL = "status_bar_general";
    private static final String STATUS_BAR_DONOTDISTURB = "status_bar_donotdisturb";
    private static final String STATUS_BAR_NOTIF_ICON_OPACITY = "status_bar_icon_opacity"; 
    private static final String KEY_MMS_BREATH = "mms_breath";
    private static final String KEY_MISSED_CALL_BREATH = "missed_call_breath"; 

    private ColorPickerPreference mColorPicker;
    private CheckBoxPreference mStatusBarBrightnessControl;
    private CheckBoxPreference mStatusBarNotifCount;
    private PreferenceScreen mClockStyle;
    private PreferenceCategory mPrefCategoryGeneral;
    private CheckBoxPreference mStatusBarDoNotDisturb;
    private ListPreference mStatusBarIconOpacity; 
    private CheckBoxPreference mMMSBreath;
    private CheckBoxPreference mMissedCallBreath; 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	ContentResolver resolver = getContentResolver();

        addPreferencesFromResource(R.xml.status_bar);

        PreferenceScreen prefSet = getPreferenceScreen();

        mStatusBarBrightnessControl = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_BRIGHTNESS_CONTROL);

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

        mStatusBarNotifCount = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_NOTIF_COUNT);
        mStatusBarNotifCount.setChecked((Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF_COUNT, 0) == 1));

	mStatusBarIconOpacity = (ListPreference) prefSet.findPreference(STATUS_BAR_NOTIF_ICON_OPACITY);
        int iconOpacity = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.STATUS_BAR_NOTIF_ICON_OPACITY, 140);
        mStatusBarIconOpacity.setValue(String.valueOf(iconOpacity));
        mStatusBarIconOpacity.setOnPreferenceChangeListener(this); 

	mMMSBreath = (CheckBoxPreference) findPreference(KEY_MMS_BREATH);
        mMMSBreath.setChecked(Settings.System.getInt(resolver,
                Settings.System.MMS_BREATH, 0) == 1);

	mMissedCallBreath = (CheckBoxPreference) findPreference(KEY_MISSED_CALL_BREATH);
        mMissedCallBreath.setChecked(Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(),
                Settings.System.MISSED_CALL_BREATH, 0) == 1); 

        mPrefCategoryGeneral = (PreferenceCategory) findPreference(STATUS_BAR_CATEGORY_GENERAL);

	mClockStyle = (PreferenceScreen) prefSet.findPreference("clock_style_pref");
        if (mClockStyle != null) {
            updateClockStyleDescription();
        }

        if (Utils.isTablet(getActivity())) {
            mPrefCategoryGeneral.removePreference(mStatusBarBrightnessControl);
        }

	mStatusBarDoNotDisturb = (CheckBoxPreference) prefSet.findPreference(STATUS_BAR_DONOTDISTURB);
        mStatusBarDoNotDisturb.setChecked((Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.STATUS_BAR_DONOTDISTURB, 0) == 1));

    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean result = false;
        if (preference == mStatusBarIconOpacity) {
            int iconOpacity = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.STATUS_BAR_NOTIF_ICON_OPACITY, iconOpacity);
            return true;
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
	}else if (preference == mStatusBarDoNotDisturb) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_DONOTDISTURB,
                    mStatusBarDoNotDisturb.isChecked() ? 1 : 0);
            return true;
	} else if (preference == mMMSBreath) {
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.MMS_BREATH, 
                    mMMSBreath.isChecked() ? 1 : 0);
	    return true;
	} else if (preference == mMissedCallBreath) {
            Settings.System.putInt(mContext.getContentResolver(), Settings.System.MISSED_CALL_BREATH, 
                    mMissedCallBreath.isChecked() ? 1 : 0);
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
