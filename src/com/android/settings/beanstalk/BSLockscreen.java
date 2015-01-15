package com.android.settings.beanstalk;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.SwitchPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.widget.Toast;

import com.android.settings.beanstalk.util.Helpers;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class BSLockscreen extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {

    private static final String KEY_LOCKSCREEN_CAMERA_WIDGET_HIDE = "camera_widget_hide";
    private static final String KEY_LOCKSCREEN_DIALER_WIDGET_HIDE = "dialer_widget_hide";

    private SwitchPreference mCameraWidgetHide;
    private SwitchPreference mDialerWidgetHide;
    private PreferenceScreen mBeanstalkLockscreen;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.beanstalk_lockscreen);

	ContentResolver resolver = getActivity().getContentResolver();
	PreferenceScreen prefSet = getPreferenceScreen();
	PackageManager pm = getPackageManager();
        Resources res = getResources();

	mBeanstalkLockscreen = (PreferenceScreen) findPreference("beanstalk_lockscreen_screen");

        // Camera widget hide
        mCameraWidgetHide = (SwitchPreference) findPreference("camera_widget_hide");
        boolean mCameraDisabled = false;
        DevicePolicyManager dpm =
            (DevicePolicyManager) getActivity().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm != null) {
            mCameraDisabled = dpm.getCameraDisabled(null);
        }
        if (mCameraDisabled){
            mBeanstalkLockscreen.removePreference(mCameraWidgetHide);
        }

        // Dialer widget hide
        mDialerWidgetHide = (SwitchPreference) prefSet.findPreference(KEY_LOCKSCREEN_DIALER_WIDGET_HIDE);
        mDialerWidgetHide.setChecked(Settings.System.getIntForUser(resolver,
            Settings.System.DIALER_WIDGET_HIDE, 0, UserHandle.USER_CURRENT) == 1);
        mDialerWidgetHide.setOnPreferenceChangeListener(this);
        if (!Utils.isVoiceCapable(getActivity())){
            mBeanstalkLockscreen.removePreference(mDialerWidgetHide);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
	ContentResolver resolver = getActivity().getContentResolver();
	final String key = preference.getKey();
	if (preference == mDialerWidgetHide) {
            boolean value = (Boolean) objValue;
            Settings.System.putIntForUser(getActivity().getContentResolver(),
                    Settings.System.DIALER_WIDGET_HIDE, value ? 1 : 0, UserHandle.USER_CURRENT);
            Helpers.restartSystemUI();
        }
        return false;
    }
}
