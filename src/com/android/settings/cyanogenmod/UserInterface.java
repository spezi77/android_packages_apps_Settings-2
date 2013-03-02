
package com.android.settings.cyanogenmod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.lang.StringBuilder;
import java.util.Random;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import java.io.*;

import android.app.Activity;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemProperties;
import android.os.RemoteException;
import android.os.Handler;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.text.Spannable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import com.android.internal.view.RotationPolicy;
import com.android.settings.DreamSettings;
import com.android.settings.cyanogenmod.DisplayRotation;
import com.android.settings.util.AbstractAsyncSuCMDProcessor;

public class UserInterface extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";

    private static final String PREF_USE_ALT_RESOLVER = "use_alt_resolver";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final int REQUEST_PICK_BOOT_ANIMATION = 203;
    private static final String DUAL_PANE_PREFS = "dual_pane_prefs";

    Preference mCustomBootAnimation;
    CheckBoxPreference mUseAltResolver;
    private Preference mCustomLabel;
    CheckBoxPreference mDisableBootAnimation;
    private ListPreference mDualPanePrefs;
    Preference mScroller;

    private Random randomGenerator = new Random();
    // previous random; so we don't repeat
    private static int mLastRandomInsultIndex = -1;
    private String[] mInsults;

    int newDensityValue;

    private String mCustomLabelText = null;
    DensityChanger densityFragment;
    Scroller scrollerFragment;

    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
	mContext = getActivity();
//      Utils.setContext(mContext);
        ContentResolver resolver = getActivity().getContentResolver();
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.user_interface_settings);

        PreferenceScreen prefs = getPreferenceScreen();
	mInsults = mContext.getResources().getStringArray(
                R.array.disable_bootanimation_insults);

	mUseAltResolver = (CheckBoxPreference) findPreference(PREF_USE_ALT_RESOLVER);
        mUseAltResolver.setChecked(Settings.System.getInt(
                getActivity().getContentResolver(),
                Settings.System.ACTIVITY_RESOLVER_USE_ALT, 0) == 1);

	mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        updateCustomLabelTextSummary();

	mScroller = findPreference("scroll_setup");
        mScroller.setSummary(R.string.scroll_summary);

	mDisableBootAnimation = (CheckBoxPreference)findPreference("disable_bootanimation");
        mDisableBootAnimation.setChecked(!new File("/system/media/bootanimation.zip").exists());
        if (mDisableBootAnimation.isChecked()) {
            Resources res = mContext.getResources();
            String[] insults = res.getStringArray(R.array.disable_bootanimation_insults);
            int randomInt = randomGenerator.nextInt(insults.length);
            mDisableBootAnimation.setSummary(insults[randomInt]);
        }

	mCustomBootAnimation = findPreference("custom_bootanimation");

	mDualPanePrefs = (ListPreference) prefs.findPreference(DUAL_PANE_PREFS);
        mDualPanePrefs.setOnPreferenceChangeListener(this);
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mDualPanePrefs) {
            int value = Integer.valueOf((String) newValue);
            Settings.System.putInt(getActivity().getApplicationContext().getContentResolver(),
                    Settings.System.DUAL_PANE_PREFS, value);
            getActivity().recreate();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            final Preference preference) {
        if (preference == mScroller) {
            ((PreferenceActivity) getActivity())
                    .startPreferenceFragment(new Scroller(), true);
            return true;
        } else if (preference == mUseAltResolver) {
		Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.ACTIVITY_RESOLVER_USE_ALT,
                    ((CheckBoxPreference) preference).isChecked() ? 1 : 0);
            return true;
	} else if (preference == mDisableBootAnimation) {
            DisableBootAnimation();
            return true;
	} else if (preference == mCustomBootAnimation) {
            PackageManager packageManager = getActivity().getPackageManager();
            Intent test = new Intent(Intent.ACTION_GET_CONTENT);
            test.setType("file/*");
            List<ResolveInfo> list = packageManager.queryIntentActivities(test, PackageManager.GET_ACTIVITIES);
            if(list.size() > 0) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                intent.setType("file/*");
                startActivityForResult(intent, REQUEST_PICK_BOOT_ANIMATION);
            } else {
                //No app installed to handle the intent - file explorer required
                Toast.makeText(mContext, R.string.install_file_manager_error, Toast.LENGTH_SHORT).show();
            }
            return true;
	} else if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);

            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);

            alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("Cyanogenmod.CARRIER_LABEL_CHANGED");
                    getActivity().getApplicationContext().sendBroadcast(i);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });

            alert.show();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(), Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }

    private void DisableBootAnimation() {
        CMDProcessor term = new CMDProcessor();
        if (!term.su.runWaitFor(
                "grep -q \"debug.sf.nobootanimation\" /system/build.prop")
                .success()) {
            // if not add value
            Helpers.getMount("rw");
            term.su.runWaitFor("echo debug.sf.nobootanimation="
                + String.valueOf(mDisableBootAnimation.isChecked() ? 1 : 0)
                + " >> /system/build.prop");
            Helpers.getMount("ro");
        }
        // preform bootanimation operations off UI thread
        AbstractAsyncSuCMDProcessor processor = new AbstractAsyncSuCMDProcessor(true) {
            @Override
            protected void onPostExecute(String result) {
                if (mDisableBootAnimation.isChecked()) {
                    // do not show same insult as last time
                    int newInsult = randomGenerator.nextInt(mInsults.length);
                    while (newInsult == mLastRandomInsultIndex)
                        newInsult = randomGenerator.nextInt(mInsults.length);

                    // update our static index reference
                    mLastRandomInsultIndex = newInsult;
                    mDisableBootAnimation.setSummary(mInsults[newInsult]);
                } else {
                    mDisableBootAnimation.setSummary("");
                }
            }
        };
        processor.execute(getBootAnimationCommand(mDisableBootAnimation.isChecked()));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_BOOT_ANIMATION) {
                if (data==null) {
                    //Nothing returned by user, probably pressed back button in file manager
                    return;
                }

                String path = data.getData().getEncodedPath();

                Helpers.getMount("rw");
                //backup old boot animation
                new CMDProcessor().su.runWaitFor("mv /system/media/bootanimation.zip /system/media/bootanimation.backup");

                //Copy new bootanimation, give proper permissions
                new CMDProcessor().su.runWaitFor("cp "+ path +" /system/media/bootanimation.zip");
                new CMDProcessor().su.runWaitFor("chmod 644 /system/media/bootanimation.zip");

                //Update setting to reflect that boot animation is now enabled
                mDisableBootAnimation.setChecked(false);

                Helpers.getMount("ro");
            }
        }
    }

    /**
     * creates a couple commands to perform all root
     * operations needed to disable/enable bootanimations
     *
     * @param checked state of CheckBox
     * @return script to turn bootanimations on/off
     */
    private String[] getBootAnimationCommand(boolean checked) {
        String[] cmds = new String[2];
        String storedLocation = "/system/media/bootanimation.backup";
        String activeLocation = "/system/media/bootanimation.zip";
        if (checked) {
            /* make backup */
            cmds[0] = "mv " + activeLocation + " " + storedLocation + "; ";
        } else {
            /* apply backup */
            cmds[0] = "mv " + storedLocation + " " + activeLocation + "; ";
        }
        /*
         * use sed to replace build.prop property
         * debug.sf.nobootanimation=[1|0]
         *
         * without we get the Android shine animation when
         * /system/media/bootanimation.zip is not found
         */
        cmds[1] = "busybox sed -i \"/debug.sf.nobootanimation/ c "
                + "debug.sf.nobootanimation=" + String.valueOf(checked ? 1 : 0)
                + "\" " + "/system/build.prop";
        return cmds;
    }
}
