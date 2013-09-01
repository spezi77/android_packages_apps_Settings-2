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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

/**
 * Performance Settings
 */
public class PerformanceSettings extends SettingsPreferenceFragment {
    private static final String TAG = "PerformanceSettings";

    private static final String USE_16BPP_ALPHA_PREF = "pref_use_16bpp_alpha";

    private static final String USE_16BPP_ALPHA_PROP = "persist.sys.use_16bpp_alpha";

    public static final String VIBE_STR = "pref_vibe_strength";
    public static final String VIBE_STR_FILE = "/sys/class/timed_output/vibrator/vibe_strength"; 

    private CheckBoxPreference mUse16bppAlphaPref;

    private AlertDialog alertDialog;

    private EditTextPreference mVibeStrength;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.performance_settings);

            PreferenceScreen prefSet = getPreferenceScreen();

            mUse16bppAlphaPref = (CheckBoxPreference) prefSet.findPreference(USE_16BPP_ALPHA_PREF);
            String use16bppAlpha = SystemProperties.get(USE_16BPP_ALPHA_PROP, "0");
            mUse16bppAlphaPref.setChecked("1".equals(use16bppAlpha));

	    mVibeStrength = (EditTextPreference) prefSet.findPreference(VIBE_STR);
            if (!Utils.fileExists(VIBE_STR_FILE)) {
                prefSet.removePreference(mVibeStrength);
            } else {
                mVibeStrength.setOnPreferenceChangeListener(this);
                String mCurVibeStrength = Utils.fileReadOneLine(VIBE_STR_FILE);
                mVibeStrength.setSummary(getString(R.string.pref_vibe_strength_summary, mCurVibeStrength));
                mVibeStrength.setText(mCurVibeStrength);
            } 
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mUse16bppAlphaPref) {
            SystemProperties.set(USE_16BPP_ALPHA_PROP,
                    mUse16bppAlphaPref.isChecked() ? "1" : "0");
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVibeStrength) {
            int strength = Integer.parseInt((String) newValue);
            if (strength > 120 || strength < 0) {
                return false;
            }
            if (Utils.fileWriteOneLine(VIBE_STR_FILE, (String) newValue)) {
                mVibeStrength.setSummary(getString(R.string.pref_vibe_strength_summary, (String) newValue));
                return true;
            } else {
                return false;
            } 
        }
        return true;
    }

}
