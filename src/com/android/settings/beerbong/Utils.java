package com.android.settings.beerbong;

import android.content.Context;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.os.PowerManager;

import com.android.settings.R;

import java.io.*;
import java.util.*;

public class Utils {

    public static final String MOUNT_SYSTEM_RW = "busybox mount -o rw,remount /system";
    public static final String MOUNT_SYSTEM_RO = "busybox mount -o ro,remount /system";

    private static Context mContext;

    public static void setContext(Context mc) {
        mContext = mc;
    }
    /*public static int mapChosenDpToPixels(int dp) {
        switch (dp) {
            case 48:
                return mContext.getResources().getDimensionPixelSize(R.dimen.navigation_bar_48);
            case 44:
                return mContext.getResources().getDimensionPixelSize(R.dimen.navigation_bar_44);
            case 42:
                return mContext.getResources().getDimensionPixelSize(R.dimen.navigation_bar_42);
            case 40:
                return mContext.getResources().getDimensionPixelSize(R.dimen.navigation_bar_40);
            case 36:
                return mContext.getResources().getDimensionPixelSize(R.dimen.navigation_bar_36);
            case 30:
                return mContext.getResources().getDimensionPixelSize(R.dimen.navigation_bar_30);
            case 24:
                return mContext.getResources().getDimensionPixelSize(R.dimen.navigation_bar_24);
        }
        return -1;
    }*/

    public static Iterator sortedIterator(Iterator it, Comparator comparator) {
        List list = new ArrayList();
        while (it.hasNext()) {
            list.add(it.next());
        }

        Collections.sort(list, comparator);
        return list.iterator();
    }
    public static void setProperty(String property, String value){
    setProperty(property, value, false);
    }
    public static void setProperty(String property, String value, boolean toData){
        if(readFile("/system/build.prop").contains(property + "="))
            execute(new String[]{
                MOUNT_SYSTEM_RW, 
                "cd /system", 
                "busybox sed -i 's|^"+property+"=.*|"+property+"=" + value + "|' build.prop", 
                "busybox chmod 644 build.prop", 
                MOUNT_SYSTEM_RO
            }, 0);
        else
            execute(new String[]{
                MOUNT_SYSTEM_RW, 
                "cd /system", 
                "chmod 777 build.prop", 
                "busybox printf \"\\n%b\" " + property + "=" + value + " >> build.prop", 
                "busybox chmod 644 build.prop", 
                MOUNT_SYSTEM_RO
            }, 0);
        if (toData) {
            String fileName = "/data/local.prop";
            File file = new File(fileName);
            if (!file.exists()) {
                writeFile(fileName, new String[] {property + "=" + value});
            } else if(readFile(fileName).contains(property + "=")) {
                execute(new String[]{
                    "cd /data", 
                    "busybox sed -i 's|^"+property+"=.*|"+property+"=" + value + "|' local.prop",
                }, 0);
            } else {
                execute(new String[]{
                    "cd /data",  
                    "busybox printf \"\\n%b\" " + property + "=" + value + " >> local.prop"
                }, 0);
            }
        }
    }
    public static String getProperty(String prop) {
        try {
            String output = null;
            Process p = Runtime.getRuntime().exec("getprop "+prop);
            p.waitFor();
            BufferedReader input = new BufferedReader (new InputStreamReader(p.getInputStream()));
            output = input.readLine();
            return output;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void restartUI() {
        execute(new String[] {"pkill -TERM -f com.android.systemui"}, 0);
    }
    public static void reboot() {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setTitle(R.string.alert_reboot);
        alert.setMessage(mContext.getString(R.string.alert_reboot_message));
        alert.setPositiveButton(R.string.alert_yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.dismiss();
                    PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                    pm.reboot("Settings Triggered Reboot");
                }
        });
        alert.setNegativeButton(R.string.alert_no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alert.show();
    }
    public static boolean execute(String command){
        return execute(new String[]{
            MOUNT_SYSTEM_RW, 
            command, 
            MOUNT_SYSTEM_RO
        },0);
    }
    public static boolean execute(String[] command, int wait) {
        if(wait!=0){
            try {
                Thread.sleep(wait);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Process proc;
        try {
            proc = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(proc.getOutputStream());
            for (String tmpCmd : command) {
                os.writeBytes(tmpCmd+"\n");
            }
            os.flush();
            os.close();
            proc.waitFor();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
          e.printStackTrace();
          return false;
        }
    }
    public static void writeFile(String filename, String[] lines) {
        try {
            boolean isSystem = filename.indexOf("system/") >= 0;
            if (isSystem) {
                execute(new String[]{MOUNT_SYSTEM_RW},0);
            }
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(filename);
                for (int i=0;i<lines.length;i++){
                    out.write((lines[i] + "\n").getBytes());
                }
            } finally {
                if (out != null) {
                    out.close();
                }
                if (isSystem) {
                    execute(new String[]{MOUNT_SYSTEM_RO},0);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    public static String readFile(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename), 256);
            StringBuffer sb = new StringBuffer();
            try {
                String linea = reader.readLine();
                while (linea != null) {
                    sb.append(linea + "\n");
                    linea = reader.readLine();
                }
            } finally {
                reader.close();
            }
            return sb.toString();
        } catch (Throwable t) {
            t.printStackTrace();
            return "";
        }
    }
}
