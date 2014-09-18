
package com.android.settings.cyanogenmod;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.provider.Settings;

import com.android.settings.DisplaySettings;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.hardware.DisplayColor;
import com.android.settings.hardware.DisplayGamma;
import com.android.settings.hardware.VibratorIntensity;
import com.android.settings.location.LocationSettings;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {

	ContentResolver resolver = context.getContentResolver();

 	if (Settings.System.getInt(resolver,
 		Settings.System.QUIET_HOURS_REQUIRE_CHARGING, 0) != 0) {
 	    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
 	    Intent batteryStatus = context.registerReceiver(null, ifilter);
 	    final int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
 	    final boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
 	    	status == BatteryManager.BATTERY_STATUS_FULL;

 	    Settings.System.putInt(resolver,
 		Settings.System.QUIET_HOURS_REQUIRE_CHARGING, isCharging ? 2 : 1);
 }

	QuietHoursController.getInstance(ctx).scheduleService();
        /* Restore hardware tunable values */
        DisplayColor.restore(ctx);
        DisplayGamma.restore(ctx);
        VibratorIntensity.restore(ctx);
        DisplaySettings.restore(ctx);
        LocationSettings.restore(ctx);
    }
}
