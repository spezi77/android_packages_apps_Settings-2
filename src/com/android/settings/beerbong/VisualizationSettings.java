/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.beerbong;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.widget.EditText;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.text.Spannable;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.view.RotationPolicy;
import com.android.settings.DreamSettings;
import com.android.settings.cyanogenmod.DisplayRotation;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

public class VisualizationSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    
    private static final String TAG = "BeerbongVisualizationSettings";

    private static final String PREF_UI_MODE = "ui_mode";

    private PreferenceScreen mDpiScreen;
    private ListPreference mUimode;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	    mContext = getActivity();
	    Utils.setContext(mContext);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.beerbong_visualization_settings);

	    mDpiScreen = (PreferenceScreen) findPreference("system_dpi");
	    updateDensityTextSummary();

        mUimode = (ListPreference) findPreference(PREF_UI_MODE);

        int uiMode = Settings.System.getInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.UI_MODE, 0);
        mUimode.setValue(String.valueOf(uiMode));
        mUimode.setSummary(mUimode.getEntry());
        mUimode.setOnPreferenceChangeListener(this);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
	    updateDensityTextSummary();
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (PREF_UI_MODE.equals(key)) {
            int uiMode = Integer.valueOf((String) objValue);
            int index = mUimode.findIndexOfValue((String)objValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(), Settings.System.UI_MODE, uiMode);
            mUimode.setSummary(mUimode.getEntries()[index]);
            setTabletUI(uiMode == 3);
            Utils.reboot();
        }

        return true;
    }

    private void updateDensityTextSummary() {
        String prop = Utils.getProperty("qemu.sf.lcd_density");
        if (prop == null) prop = Utils.getProperty("ro.sf.lcd_density");
        mDpiScreen.setSummary(getResources().getString(R.string.system_dpi_summary) + " " + prop);
    }
}
