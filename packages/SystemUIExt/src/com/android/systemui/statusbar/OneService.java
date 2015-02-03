/*
 * Copyright (C) 2015 The Android One Source Project
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
import java.util.Calendar;
import android.media.AudioManager;

import com.android.internal.util.one.OneUtils;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.AsyncTask;
import android.os.UserHandle;
import android.provider.Settings;
import android.os.IPowerManager;
import android.os.PowerManager;
import android.os.ServiceManager;
import android.os.RemoteException;
import android.util.Log;
import android.text.TextUtils;
import com.android.systemui.utils.Location;

import com.android.systemui.SystemUI;

public class OneService extends SystemUI {

    static final String TAG = "OneService";

    private final Handler mHandler = new Handler();
    private final Receiver mReceiver = new Receiver();

    private int SmallHours;
    private int MorningHours;
    private int NoonHours;
    private int NightHours;

    private int mSmarterSleep;

    private AudioManager audioMgr;

    public void start() {
        audioMgr = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        ContentObserver obs = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                UpdateSettings();
            }
        };
        final ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.SMARTER_BRIGHTNESS),
                false, obs, UserHandle.USER_ALL);
        resolver.registerContentObserver(Settings.Global.getUriFor(
                Settings.Global.SMARTER_SLEEP),
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
        UpdateSettings();
        mReceiver.init();
        if (Location.refreshDATA(mContext)) {return;}
    }

    private final class Receiver extends BroadcastReceiver {

        public void init() {
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
            filter.addAction(Intent.ACTION_USER_SWITCHED);
            mContext.registerReceiver(this, filter, null, mHandler);
        }

        public void UpdateAMPM() {
            Resources r = Resources.getSystem();
            Calendar cd = Calendar.getInstance();
            int hours = cd.get(Calendar.HOUR);
            int ampm = cd.get(Calendar.AM_PM);
            if (ampm == Calendar.AM) {
                  if (hours < 6) {
                      audioMgr.setRingerMode((mSmarterSleep == 1 ? 0 : 2));
                      UpdateBrightness((SmallHours == 0 ? 2 : SmallHours));
                  } else if (hours >= 6 && hours < 12) {
                      audioMgr.setRingerMode((mSmarterSleep == 2 ? 0 : 2));
                      UpdateBrightness((MorningHours == 0 ? 120 : MorningHours));
                  } else {
                      UpdateBrightness(2);
                  }
            } else {
                if (hours < 6) {
                    audioMgr.setRingerMode((mSmarterSleep == 3 ? 0 : 2));
                    UpdateBrightness((NoonHours == 0 ? 120 : NoonHours));
                } else if (hours >= 6 && hours < 12) {
                    audioMgr.setRingerMode((mSmarterSleep == 4 ? 0 : 2));
                    UpdateBrightness((NightHours == 0 ? 50 : NightHours));
                } else {
                    UpdateBrightness(2);
                }
           }
       }

       private void UpdateBrightness(int value) {
           try {
              PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
              IPowerManager mPower = IPowerManager.Stub.asInterface(ServiceManager.getService("power"));
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

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_TIME_TICK)) {
                // smarter aviate Init ...
                // smarter remind Init ...
                UpdateSettings();
            }
            UpdateSettings();
        }
    };

    private void UpdateSettings() {

        SmallHours = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.SMALL_BRIGHTNESS, 0);
        MorningHours = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.MORNING_BRIGHTNESS, 0);
        NoonHours = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.NOON_BRIGHTNESS, 0);
        NightHours = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.NIGHT_BRIGHTNESS, 0);

        mSmarterSleep = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.SMARTER_SLEEP, 0);

        boolean smarterBrightness = Settings.Global.getInt(mContext.getContentResolver(),
             Settings.Global.SMARTER_BRIGHTNESS, 0) == 1;
        if (smarterBrightness) {
            mReceiver.UpdateAMPM();
        }
    }
}

