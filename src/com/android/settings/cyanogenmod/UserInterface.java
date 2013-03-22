
package com.android.settings.cyanogenmod;

import java.io.BufferedReader; 
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.lang.StringBuilder;
import java.util.Enumeration;
import java.util.Random;
import java.util.zip.ZipEntry;
import android.util.Log;
import java.util.ArrayList;
import java.util.zip.ZipFile; 
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
import android.graphics.Point;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.BitmapDrawable; 
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message; 
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
import android.widget.ImageView;
import android.widget.LinearLayout; 
import android.widget.Toast;
import android.widget.TextView; 
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;
import com.android.internal.view.RotationPolicy;
import com.android.settings.DreamSettings;
import com.android.settings.cyanogenmod.DisplayRotation;
import com.android.settings.service.CodeReceiver; 
import com.android.settings.util.AbstractAsyncSuCMDProcessor;

public class UserInterface extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";

    private static final String PREF_USE_ALT_RESOLVER = "use_alt_resolver";
    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final int REQUEST_PICK_BOOT_ANIMATION = 203;
    private static final String DUAL_PANE_PREFS = "dual_pane_prefs";
    private static final String BOOTANIMATION_USER_PATH = "/data/local/bootanimation.zip";
    private static final String BOOTANIMATION_SYSTEM_PATH = "/system/media/bootanimation.zip"; 

    Preference mCustomBootAnimation;
    Preference mLcdDensity;
    CheckBoxPreference mUseAltResolver;
    private Preference mCustomLabel;
    CheckBoxPreference mDisableBootAnimation;
    private ListPreference mDualPanePrefs;
    Preference mScroller;
    ImageView view;
    TextView error;
    AlertDialog mCustomBootAnimationDialog; 

    private AnimationDrawable mAnimationPart1;
    private AnimationDrawable mAnimationPart2;
    private String mPartName1;
    private String mPartName2;
    private int delay;
    private int height;
    private int width;
    private String errormsg;
    private String mBootAnimationPath;

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

        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }

        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);

	mScroller = findPreference("scroll_setup");
        mScroller.setSummary(R.string.scroll_summary);

        mDisableBootAnimation = (CheckBoxPreference)findPreference("disable_bootanimation");

        mCustomBootAnimation = findPreference("custom_bootanimation");

	mDualPanePrefs = (ListPreference) prefs.findPreference(DUAL_PANE_PREFS);
        mDualPanePrefs.setOnPreferenceChangeListener(this);

	setHasOptionsMenu(true)
	resetBootAnimation(); 
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mDisableBootAnimation != null) {
            if (mDisableBootAnimation.isChecked()) {
                Resources res = mContext.getResources();
                String[] insults = res.getStringArray(R.array.disable_bootanimation_insults);
                int randomInt = randomGenerator.nextInt(insults.length);
                mDisableBootAnimation.setSummary(insults[randomInt]);
            } else {
                mDisableBootAnimation.setSummary(null);
            }
        }
    }

    /**
* Resets boot animation path. Essentially clears temporary-set boot animation
* set by the user from the dialog.
* @return returns true if a boot animation exists (user or system). false otherwise.
*/
    private boolean resetBootAnimation() {
        boolean bootAnimationExists = false;
        if(new File(BOOTANIMATION_USER_PATH).exists()) {
            mBootAnimationPath = BOOTANIMATION_USER_PATH;
            bootAnimationExists = true;
        } else if (new File(BOOTANIMATION_SYSTEM_PATH).exists()) {
            mBootAnimationPath = BOOTANIMATION_SYSTEM_PATH;
            bootAnimationExists = true;
        } else {
            mBootAnimationPath = "";
        }
        mCustomBootAnimation.setEnabled(!mDisableBootAnimation.isChecked());
        return bootAnimationExists;
    }

    private void resetSwaggedOutBootAnimation() {
        if(new File("/data/local/bootanimation.user").exists()) {
            // we're using the alt boot animation
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    new CMDProcessor().su.run("mv /data/local/bootanimation.user /data/local/bootanimation.zip");
                    return null;
                }
            }.execute();
        }
        CodeReceiver.setSwagInitiatedPref(mContext, false);
    }



    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            final Preference preference) {
        if (preference == mLcdDensity) {
            ((PreferenceActivity) getActivity())
            .startPreferenceFragment(new DensityChanger(), true);
            return true;
        } else if (preference == mScroller) {
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
            openBootAnimationDialog();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_BOOT_ANIMATION) {
                if (data==null) {
                    //Nothing returned by user, probably pressed back button in file manager
                    return;
                }
                mBootAnimationPath = data.getData().getPath();
                openBootAnimationDialog();
            }
        }
    } 

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(), Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }

    private void openBootAnimationDialog() {
        resetSwaggedOutBootAnimation();
        Log.e(TAG, "boot animation path: " + mBootAnimationPath);
        if(mCustomBootAnimationDialog != null) {
            mCustomBootAnimationDialog.cancel();
            mCustomBootAnimationDialog = null;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.bootanimation_preview);
        if (!mBootAnimationPath.isEmpty()
                && (!BOOTANIMATION_SYSTEM_PATH.equalsIgnoreCase(mBootAnimationPath) && !BOOTANIMATION_USER_PATH
                        .equalsIgnoreCase(mBootAnimationPath))) {
            builder.setPositiveButton(R.string.apply, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new InstallBootAnimTask(dialog).execute();
                    resetBootAnimation();
                }
            });
        } else if (new File(BOOTANIMATION_USER_PATH).exists()) {
            builder.setPositiveButton(R.string.clear_custom_bootanimation, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    new AbstractAsyncSuCMDProcessor() {
                        @Override
                        protected void onPostExecute(String result) {
                            resetBootAnimation();
                        }
                    }.execute("rm '" + BOOTANIMATION_USER_PATH + "'", "rm '/data/media/bootanimation.backup'");
                }
            });
        }
        builder.setNeutralButton(R.string.set_custom_bootanimation, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                PackageManager packageManager = getActivity().getPackageManager();
                Intent test = new Intent(Intent.ACTION_GET_CONTENT);
                test.setType("file/*");
                List<ResolveInfo> list = packageManager.queryIntentActivities(test,
                        PackageManager.GET_ACTIVITIES);
                if (list.size() > 0) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
                    intent.setType("file/*");
                    startActivityForResult(intent, REQUEST_PICK_BOOT_ANIMATION);
                } else {
                    //No app installed to handle the intent - file explorer required
                    Toast.makeText(mContext, R.string.install_file_manager_error,
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
        builder.setNegativeButton(com.android.internal.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        resetBootAnimation();
                        dialog.dismiss();
                    }
                });
        LayoutInflater inflater =
                (LayoutInflater) getActivity()
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_bootanimation_preview,
                (ViewGroup) getActivity()
                        .findViewById(R.id.bootanimation_layout_root));
        error = (TextView) layout.findViewById(R.id.textViewError);
        view = (ImageView) layout.findViewById(R.id.imageViewPreview);
        view.setVisibility(View.GONE);
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        view.setLayoutParams(new LinearLayout.LayoutParams(size.x / 2, size.y / 2));
        error.setText(R.string.creating_preview);
        builder.setView(layout);
        mCustomBootAnimationDialog = builder.create();
        mCustomBootAnimationDialog.setOwnerActivity(getActivity());
        mCustomBootAnimationDialog.show();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                createPreview(mBootAnimationPath);
            }
        });
        thread.start();
    }

    public void copy(File src, File dst) throws IOException {
        // use file channels for faster byte transfers
        FileChannel inChannel = new
                FileInputStream(src).getChannel();
        FileChannel outChannel = new
                FileOutputStream(dst).getChannel();
        try {
            // move the bytes from in to out
            inChannel.transferTo(0,
                    inChannel.size(),
                    outChannel);
        } finally {
            // ensure closure
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }

    private void createPreview(String path) {
        File zip = new File(path);
        ZipFile zipfile = null;
        String desc = "";
        InputStream inputStream = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        try {
            zipfile = new ZipFile(zip);
            ZipEntry ze = zipfile.getEntry("desc.txt");
            inputStream = zipfile.getInputStream(ze);
            inputStreamReader = new InputStreamReader(inputStream);
            StringBuilder sb = new StringBuilder();
            bufferedReader = new BufferedReader(inputStreamReader);
            String read = bufferedReader.readLine();
            while (read != null) {
                sb.append(read);
                sb.append("\n");
                read = bufferedReader.readLine();
            }
            desc = sb.toString();
        } catch (Exception e1) {
            errormsg = getActivity().getString(R.string.error_reading_zip_file);
            errorHandler.sendEmptyMessage(0);
            return;
        } finally {
            try {
                if (bufferedReader != null)
                    bufferedReader.close();
            } catch (IOException e) {
                // we tried
            }
            try {
                if (inputStreamReader != null)
                    inputStreamReader.close();
            } catch (IOException e) {
                // we tried
            }
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                // moving on...
            }
        }

        String[] info = desc.replace("\\r", "").split("\\n");
        width = Integer.parseInt(info[0].split(" ")[0]);
        height = Integer.parseInt(info[0].split(" ")[1]);
        delay = Integer.parseInt(info[0].split(" ")[2]);
        mPartName1 = info[1].split(" ")[3];
        try {
            if (info.length > 2) {
                mPartName2 = info[2].split(" ")[3];
            }
            else {
                mPartName2 = "";
            }
        } catch (Exception e) {
            mPartName2 = "";
        }

        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inSampleSize = 4;
        mAnimationPart1 = new AnimationDrawable();
        mAnimationPart2 = new AnimationDrawable();
        try {
            for (Enumeration<? extends ZipEntry> e = zipfile.entries(); e.hasMoreElements();) {
                ZipEntry entry = (ZipEntry) e.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String partname = entry.getName().split("/")[0];
                if (mPartName1.equalsIgnoreCase(partname)) {
                    InputStream partOneInStream = null;
                    try {
                        partOneInStream = zipfile.getInputStream(entry);
                        mAnimationPart1.addFrame(new BitmapDrawable(getResources(),
                                BitmapFactory.decodeStream(partOneInStream,
                                        null, opt)), delay);
                    } finally {
                        if (partOneInStream != null)
                            partOneInStream.close();
                    }
                } else if (mPartName2.equalsIgnoreCase(partname)) {
                    InputStream partTwoInStream = null;
                    try {
                        partTwoInStream = zipfile.getInputStream(entry);
                        mAnimationPart2.addFrame(new BitmapDrawable(getResources(),
                                BitmapFactory.decodeStream(partTwoInStream,
                                        null, opt)), delay);
                    } finally {
                        if (partTwoInStream != null)
                            partTwoInStream.close();
                    }
                }
            }
        } catch (IOException e1) {
            errormsg = getActivity().getString(R.string.error_creating_preview);
            errorHandler.sendEmptyMessage(0);
            return;
        }

        if (mPartName2.length() > 0) {
            Log.d(TAG, "Multipart Animation");
            mAnimationPart1.setOneShot(false);
            mAnimationPart2.setOneShot(false);
            mAnimationPart1.setOnAnimationFinishedListener(
                    new AnimationDrawable.OnAnimationFinishedListener() {
                @Override
                public void onAnimationFinished() {
                    Log.d(TAG, "First part finished");
                    view.setImageDrawable(mAnimationPart2);
                    mAnimationPart1.stop();
                    mAnimationPart2.start();
                }
            });
        } else {
            mAnimationPart1.setOneShot(false);
        }
        finishedHandler.sendEmptyMessage(0);
    }

    /**
     * creates a couple commands to perform all root
     * operations needed to disable/enable bootanimations
     *
     * @param checked state of CheckBox
     * @return script to turn bootanimations on/off
     */
    private String[] getBootAnimationCommand(boolean checked) {
        String[] cmds = new String[3];
        String storedLocation = "/system/media/bootanimation.backup";
        String storedUserLocation = "/data/local/bootanimation.backup";
        String activeLocation = "/system/media/bootanimation.zip";
        String activeUserLocation = "/data/local/bootanimation.zip";
        if (checked) {
            /* make backup */
            cmds[0] = "mv " + activeLocation + " " + storedLocation + "; ";
            cmds[1] = "mv " + activeUserLocation + " " + storedUserLocation + "; ";
        } else {
            /* apply backup */
            cmds[0] = "mv " + storedLocation + " " + activeLocation + "; ";
            cmds[1] = "mv " + activeUserLocation + " " + storedUserLocation + "; ";
        }
        /*
         * use sed to replace build.prop property
         * debug.sf.nobootanimation=[1|0]
         *
         * without we get the Android shine animation when
         * /system/media/bootanimation.zip is not found
         */
        cmds[2] = "busybox sed -i \"/debug.sf.nobootanimation/ c "
                + "debug.sf.nobootanimation=" + String.valueOf(checked ? 1 : 0)
                + "\" " + "/system/build.prop";
        return cmds;
    }

    private Handler errorHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            view.setVisibility(View.GONE);
            error.setText(errormsg);
        }
    };

    private Handler finishedHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            view.setImageDrawable(mAnimationPart1);
            view.setVisibility(View.VISIBLE);
            error.setVisibility(View.GONE);
            mAnimationPart1.start();
        }
    };

    class InstallBootAnimTask extends AsyncTask<Void, Void, Void> {
        private final DialogInterface dialog;
        private String taskAnimationPath;

        public InstallBootAnimTask(DialogInterface dialog) {
            this.dialog = dialog;
        }

        protected void onPreExecute() {
            //Update setting to reflect that boot animation is now enabled
            taskAnimationPath = mBootAnimationPath;
            mDisableBootAnimation.setChecked(false);
            DisableBootAnimation();
            dialog.dismiss();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            //Copy new bootanimation, give proper permissions
            new CMDProcessor().su.runWaitFor("cp "+ taskAnimationPath +" /data/local/bootanimation.zip");
            new CMDProcessor().su.runWaitFor("chmod 644 /data/local/bootanimation.zip");
            return null;
        }
    }

    private void DisableBootAnimation() {
        resetSwaggedOutBootAnimation();
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
                    mDisableBootAnimation.setSummary(null);
                }
                resetBootAnimation();
            }
        };
        processor.execute(getBootAnimationCommand(mDisableBootAnimation.isChecked()));
    }

    @Override
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
}
