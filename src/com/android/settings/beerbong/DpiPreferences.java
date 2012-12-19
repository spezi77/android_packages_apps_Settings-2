package com.android.settings.beerbong;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.View;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.io.*;

/**
 * @author beerbong
 * @version 1.0
 */

public class DpiPreferences extends SettingsPreferenceFragment implements OnPreferenceChangeListener, OnPreferenceClickListener {

    private static final String PROPERTY = "qemu.sf.lcd_density";
    private static final String TAG = "beerbong/Dpi";

    private static final String DPI_PREF = "system_dpi_window";
    private static final String CUSTOM_DPI_PREF = "custom_dpi_text";

    private ListPreference mDpiWindow;
    private EditTextPreference mCustomDpi;
    private Context mContext;
   
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();

        Utils.setContext(mContext);

        addPreferencesFromResource(R.xml.beerbong_dpi_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver cr = getActivity().getApplicationContext().getContentResolver();
      
        String prop = Utils.getProperty(PROPERTY);

        mDpiWindow = (ListPreference) prefSet.findPreference(DPI_PREF);
        mDpiWindow.setValue(prop);
        mDpiWindow.setOnPreferenceChangeListener(this);
      
        mCustomDpi = (EditTextPreference) findPreference(CUSTOM_DPI_PREF);
        mCustomDpi.setOnPreferenceClickListener(this);
      
    }
    @Override
    public boolean onPreferenceClick(Preference preference) {

        if (preference == mCustomDpi) {
            final String prop = Utils.getProperty(PROPERTY);
            mCustomDpi.getEditText().setText(prop);
            mCustomDpi.getEditText().setSelection(prop.length());
            mCustomDpi.getDialog().findViewById(android.R.id.button1).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int value = 213;
                    try {
                        value = Integer.parseInt(mCustomDpi.getEditText().getText().toString());
                    } catch (Throwable t) {}
                    if (value < 120) value = 120;
                    else if (value > 480) value = 480;
                    Utils.setProperty(PROPERTY, String.valueOf(value), true);
                    mCustomDpi.getDialog().dismiss();
                    if (!prop.equals(String.valueOf(value))) {
                        Utils.reboot();
                    }
                }
            });
            return true;
        }
        return false;
    }
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDpiWindow) {
            String prop = Utils.getProperty(PROPERTY);
            Utils.setProperty(PROPERTY, newValue.toString(), true);
            if (!prop.equals(newValue.toString())) {
                Utils.reboot();
            }
            return true;
        }
        return false;
    }
}
