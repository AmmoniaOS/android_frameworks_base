/*
 * Copyright (C) 2015 The Android One
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

package com.android.internal.util.one;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.text.DecimalFormat;
import java.util.Locale;
import java.io.File;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import java.util.Locale;

/**
* @hide
*/

public class OneUtils {

    public static String FILEPATH = "data/data/com.android.systemui/files";
    public static String DB_FILE_NAME = "one-location.db";
    private static String TABLE_NAME = "location_date";

    private static String location;
    private static String[] numm;

    private static String NUMBER_INDEX = "number=?";

    private static int LOCATION = 2;
    private static int CITY = 3;

    private static SQLiteDatabase db = null;

    public static boolean isSupportLanguage(boolean excludeTW) {
        Configuration configuration = Resources.getSystem().getConfiguration();
        if (excludeTW) {
            return configuration.locale.getLanguage().startsWith(Locale.CHINESE.getLanguage())
                    && !configuration.locale.getCountry().equals("TW");
        } else {
            return configuration.locale.getLanguage().startsWith(Locale.CHINESE.getLanguage());
        }
    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        }
        return false;
    }

    public static boolean isApkInstalledAndEnabled(String packagename, Context context) {
        int state;
        try {
            context.getPackageManager().getPackageInfo(packagename, 0);
            state = context.getPackageManager().getApplicationEnabledSetting(packagename);
        } catch (NameNotFoundException e) {
            return false;
        }
        return state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED && state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ? true : false;
    }

    public static boolean isApkInstalled(String packagename, Context context) {
        try {
            context.getPackageManager().getPackageInfo(packagename, 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        return true;
    }

    public static boolean isSystemApp(String packagename, Context context) {
        PackageInfo packageInfo;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(packagename, 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        return ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0);
    }

    public static String formatFileSize(long size) {
        DecimalFormat df = new DecimalFormat("#0.00");
        String fileSizeString = "";
        if (size < 1024) {
            fileSizeString = df.format((double) size) + "B";
        } else if (size < 1048576) {
            fileSizeString = df.format((double) size / 1024) + "KB";
        } else if (size < 1073741824) {
            fileSizeString = df.format((double) size / 1048576) + "MB";
        } else {
            fileSizeString = df.format((double) size / 1073741824) + "GB";
        }
        return fileSizeString;
    }

    public static String getCityFromPhone(CharSequence number) {
        if (TextUtils.isEmpty(number)) return "";
            File data = new File(FILEPATH, DB_FILE_NAME);
            db = SQLiteDatabase.openOrCreateDatabase(data, null);
            db.setLocale(Locale.CHINA);
            number = number.toString().replaceAll("(?:-| )", "");
            numm = new String[]{ ((String) number.subSequence(0,
            ((String) number).startsWith("0",0) ? 3 : (number.length() < 6 ?
              number.length() : ((boolean) number.subSequence(0,3).equals("106") ? 4 : 6)))) };
            Cursor cursor = db.query(TABLE_NAME, null, NUMBER_INDEX,
              numm, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                location = cursor.getString(LOCATION) + cursor.getString(CITY);
            } else {
                location = "未知号码";
            }
        return (TextUtils.isEmpty(location) ? "" : location);
    }

}

