/*
 * Copyright (C) 2015 The OneUI Open Source Project
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

package com.android.systemui;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.content.IntentFilter;
import android.provider.Settings;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.os.BatteryManager;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.net.Uri;
import android.text.TextUtils;

import android.util.Log;
import java.util.Calendar;

import com.android.systemui.SystemUI;
import com.android.internal.util.one.OneUtils;

public class OneService extends SystemUI {

    static final String TAG = "OneService";

    private final Handler mHandler = new Handler();
    private final Receiver m = new Receiver();

    private int SmallHours;
    private int MorningHours;
    private int NoonHours;
    private int NightHours;

    private int level;
    private int status;
    private int mState;

    private PowerManager pm;
    private IPowerManager mPower;

    public void start() {
        pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));

        ContentObserver obs = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                UpdateAll();
            }
        };
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.SMARTER_BRIGHTNESS),
                false, obs, UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.SMALL_BRIGHTNESS),
                false, obs, UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.MORNING_BRIGHTNESS),
                false, obs, UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.NOON_BRIGHTNESS),
                false, obs, UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.NIGHT_BRIGHTNESS),
                false, obs, UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.POWER_SAVE_SETTINGS),
                false, obs, UserHandle.USER_ALL);

        UpdateAll();
        m.init();
    }

    private final class Receiver extends BroadcastReceiver {

        public void init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            filter.addAction(Intent.ACTION_USER_SWITCHED);
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.addAction(Intent.ACTION_SCREEN_ON);
            mContext.registerReceiver(this, filter, null, mHandler);
        }

        public void UpdateAMPM() {
            Resources r = Resources.getSystem();
            Calendar cd = Calendar.getInstance();
            int hours = cd.get(Calendar.HOUR);
            int ampm = cd.get(Calendar.AM_PM);
            if (ampm == Calendar.AM) {
                  if (hours < 6) {
                      UpdateBrightness(SmallHours == 0 ? 2 : SmallHours);
                  } else if (hours >= 6 && hours < 12) {
                      UpdateBrightness(MorningHours == 0 ? 120 : MorningHours);
                  } else {
                      UpdateBrightness(2);
                  }
            } else {
                if (hours < 6) {
                    UpdateBrightness(NoonHours == 0 ? 120 : NoonHours);
                } else if (hours >= 6 && hours < 12) {
                    UpdateBrightness(NightHours == 0 ? 50 : NightHours);
                } else {
                    UpdateBrightness(2);
                }
           }
       }

       private void UpdateBrightness(int value) {
           try {
              int mMinimumBacklight = pm.getMinimumScreenBrightnessSetting();
              final int val = value + mMinimumBacklight;
              mPower.setTemporaryScreenBrightnessSettingOverride(val);
              AsyncTask.execute(new Runnable() {
                        public void run() {
                            Settings.System.putIntForUser(mContext.getContentResolver(),
                                    Settings.System.SCREEN_BRIGHTNESS, val,
                                    UserHandle.USER_CURRENT);
                        }
                    });
           } catch (RemoteException e) {
              Log.w(TAG, "Setting Brightness failed: " + e);
           }      
        }

        private void updateSaverMode() {
            if (status !=
                BatteryManager.BATTERY_STATUS_CHARGING) {
                if (level <= 15 && mState == 3
                    || mState == 1) {
                    setSaverMode(true);
                } else {
                    setSaverMode(false);
                }
            } else {
                setSaverMode(false);
            }
            Log.w(TAG, "Setting PowerSaver mode is: " + getSaverMode());
        }

        private boolean getSaverMode() {
             return pm.isPowerSaveMode();
        }

        private void setSaverMode(boolean mode) {
             pm.setPowerSaveMode(mode);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_TICK)) {
                UpdateAMPM();
            } else if (action.equals(Intent.ACTION_BATTERY_CHANGED) && mState != 2) {
                level = intent.getIntExtra("level", 0);
                status = intent.getIntExtra("status", 0);
                updateSaverMode();
            } else if (action.equals(Intent.ACTION_SCREEN_OFF)
             && mState == 2) {
                setSaverMode(true);
            } else if (action.equals(Intent.ACTION_SCREEN_ON)
             && mState == 2) {
                setSaverMode(false);
            }
        }
    };

    private void UpdateAll() {
        SmallHours = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.SMALL_BRIGHTNESS, 0);
        MorningHours = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.MORNING_BRIGHTNESS, 0);
        NoonHours = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.NOON_BRIGHTNESS, 0);
        NightHours = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.NIGHT_BRIGHTNESS, 0);

        mState = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.POWER_SAVE_SETTINGS, 0);

        m.UpdateUI(mNightmode);
        m.updateSaverMode();
        m.UpdateAMPM();
    }

}

