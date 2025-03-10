package com.aefyr.sai.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.AttrRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aefyr.sai.R;
import com.aefyr.sai.model.backup.PackageMeta;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.zip.ZipEntry;

public class Utils {
    private static final String TAG = "SAIUtils";

    @Nullable
    public static String getAppLabel(Context c, String packageName) {
        try {
            PackageManager pm = c.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String throwableToString(Throwable throwable) {
        StringWriter sw = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(sw);

        throwable.printStackTrace(pw);
        pw.close();

        return sw.toString();
    }

    @SuppressLint("PrivateApi")
    @Nullable
    public static String getSystemProperty(String key) {
        try {
            return (String) Class.forName("android.os.SystemProperties")
                    .getDeclaredMethod("get", String.class)
                    .invoke(null, key);
        } catch (Exception e) {
            Log.w("SAIUtils", "Unable to use SystemProperties.get", e);
            return null;
        }
    }

    public static void copyTextToClipboard(Context c, CharSequence text) {
        ClipboardManager clipboardManager = (ClipboardManager) c.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardManager.setPrimaryClip(ClipData.newPlainText("text", text));
    }

    public static boolean isMiui() {
        return !TextUtils.isEmpty(getSystemProperty("ro.miui.ui.version.name"));
    }

    public static String getFileNameFromZipEntry(ZipEntry zipEntry) {
        String path = zipEntry.getName();
        int lastIndexOfSeparator = path.lastIndexOf("/");
        if (lastIndexOfSeparator == -1)
            return path;
        return path.substring(lastIndexOfSeparator + 1);
    }

    public static boolean apiIsAtLeast(int sdkInt) {
        return Build.VERSION.SDK_INT >= sdkInt;
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
    }

    public static void hideKeyboard(Fragment fragment) {
        Activity activity = fragment.getActivity();
        if (activity != null) {
            hideKeyboard(activity);
            return;
        }

        InputMethodManager inputMethodManager = (InputMethodManager) fragment.requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(fragment.requireView().getWindowToken(), 0);
    }

    public static String escapeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    @SuppressLint("DefaultLocale")
    @Nullable
    public static File createBackupFile(Context c, PackageMeta packageMeta) {
        File backupsDir = new File(Environment.getExternalStorageDirectory(), "SAI");
        if (!backupsDir.exists() && !backupsDir.mkdir()) {
            Log.e(TAG, "Unable to mkdir:" + backupsDir.toString());
            return null;
        }

        String backupFileName = BackupNameFormat.format(PreferencesHelper.getInstance(c).getBackupFileNameFormat(), packageMeta);
        if (DbgPreferencesHelper.getInstance(c).shouldReplaceDots())
            backupFileName = backupFileName.replace('.', ',');

        if (backupFileName.length() > 160)
            backupFileName = backupFileName.substring(0, 160);

        File backupFile = new File(backupsDir, escapeFileName(String.format("%s.apks", backupFileName)));
        int suffix = 0;
        while (backupFile.exists()) {
            suffix++;
            backupFile = new File(backupsDir, escapeFileName(String.format("%s(%d).apks", backupFileName, suffix)));
        }

        try {
            if (!backupFile.createNewFile())
                return null;
        } catch (IOException e) {
            Log.e(TAG, "Unable to create backup file", e);
            return null;
        }

        return backupFile;
    }

    private static DecimalFormat sSizeDecimalFormat;

    public static String formatSize(Context c, long bytes) {
        if (sSizeDecimalFormat == null) {
            sSizeDecimalFormat = new DecimalFormat("#.##");
            sSizeDecimalFormat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.US));
        }

        String[] units = c.getResources().getStringArray(R.array.size_units);

        for (int i = 0; i < units.length; i++) {

            float size = (float) bytes / (float) Math.pow(1024, i);

            if (size < 1024)
                return String.format("%s %s", sSizeDecimalFormat.format(size), units[i]);

        }

        return bytes + " B";
    }

    public static int getThemeColor(Context c, @AttrRes int attribute) {
        TypedValue typedValue = new TypedValue();
        c.getTheme().resolveAttribute(attribute, typedValue, true);
        return typedValue.data;
    }

}
