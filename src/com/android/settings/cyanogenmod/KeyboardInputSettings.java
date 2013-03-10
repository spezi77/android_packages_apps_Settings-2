package com.android.settings.cyanogenmod;

import android.app.AlertDialog;
import android.os.Bundle;
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

public class KeyboardInputSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "KeyboardInputSettings";

    private static final String PREF_DISABLE_FULLSCREEN_KEYBOARD = "disable_fullscreen_keyboard";
    private static final String KEYBOARD_ROTATION_TOGGLE = "keyboard_rotation_toggle";
    private static final String KEYBOARD_ROTATION_TIMEOUT = "keyboard_rotation_timeout";

    private static final int KEYBOARD_ROTATION_TIMEOUT_DEFAULT = 5000; // 5s

    CheckBoxPreference mDisableFullscreenKeyboard;
    private CheckBoxPreference mKeyboardRotationToggle;
    private ListPreference mKeyboardRotationTimeout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.keyboard_input_settings);

        mDisableFullscreenKeyboard = (CheckBoxPreference) findPreference(PREF_DISABLE_FULLSCREEN_KEYBOARD);
        mDisableFullscreenKeyboard.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.DISABLE_FULLSCREEN_KEYBOARD, 0) == 1);

	mKeyboardRotationToggle = (CheckBoxPreference) findPreference(KEYBOARD_ROTATION_TOGGLE);
        mKeyboardRotationToggle.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.KEYBOARD_ROTATION_TIMEOUT, 0) > 0);

        mKeyboardRotationTimeout = (ListPreference) findPreference(KEYBOARD_ROTATION_TIMEOUT);
        mKeyboardRotationTimeout.setOnPreferenceChangeListener(this);
        updateRotationTimeout(Settings.System.getInt(getActivity()
                    .getContentResolver(), Settings.System.KEYBOARD_ROTATION_TIMEOUT, KEYBOARD_ROTATION_TIMEOUT_DEFAULT));
    }

    public void updateRotationTimeout(int timeout) {
        if (timeout == 0)
            timeout = KEYBOARD_ROTATION_TIMEOUT_DEFAULT;
        mKeyboardRotationTimeout.setValue(Integer.toString(timeout));
        mKeyboardRotationTimeout.setSummary(getString(R.string.keyboard_rotation_timeout_summary, mKeyboardRotationTimeout.getEntry()));

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void mKeyboardRotationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.keyboard_rotation_dialog);
        builder.setCancelable(false);
        builder.setPositiveButton(getResources().getString(com.android.internal.R.string.ok), null);
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDisableFullscreenKeyboard) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.DISABLE_FULLSCREEN_KEYBOARD, checked ? 1 : 0);
            return true;
	} else if (preference == mKeyboardRotationToggle) {
            boolean isAutoRotate = (Settings.System.getInt(getContentResolver(),
                        Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
            if (isAutoRotate && mKeyboardRotationToggle.isChecked())
                mKeyboardRotationDialog();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.KEYBOARD_ROTATION_TIMEOUT,
                    mKeyboardRotationToggle.isChecked() ? KEYBOARD_ROTATION_TIMEOUT_DEFAULT : 0);
            updateRotationTimeout(KEYBOARD_ROTATION_TIMEOUT_DEFAULT);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
	if (preference == mKeyboardRotationTimeout) {
            int timeout = Integer.parseInt((String) objValue);
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.KEYBOARD_ROTATION_TIMEOUT, timeout);
            updateRotationTimeout(timeout);
            return true;
	}
        return false;
    }
}
