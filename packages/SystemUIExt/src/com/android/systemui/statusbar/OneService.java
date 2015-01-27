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

import com.android.systemui.SystemUI;

public class OneService extends SystemUI {
    static final String TAG = "OneService";

    private final Handler mHandler = new Handler();
    private final Receiver mReceiver = new Receiver();

    private OneRefreshUI mOneUI;

    public void start() {
        mOneUI = new OneUI(mContext);
        mReceiver.init();
        UpdateScreenUI();
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
                      UpdateBrightness(2);
                  } else if (hours >= 6 && hours < 7) {
                      UpdateBrightness(50);
                  } else if (hours >= 7 && hours < 12) {
                      UpdateBrightness(100);
                  } else {
                      UpdateBrightness(2);
                  }
            } else {
                if (hours < 9) {
                    UpdateBrightness(100);
                } else if (hours >= 9 && hours < 11) {
                    UpdateBrightness(30);
                } else if (hours >= 11 && hours <= 12) {
                    UpdateBrightness(2);
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
                UpdateScreenUI();
            }
            UpdateScreenUI();
        }
    };

    private void UpdateScreenUI() {
        boolean smarterBrightness = Settings.System.getIntForUser(mContext.getContentResolver(),
             Settings.System.SMARTER_BRIGHTNESS, 0, UserHandle.USER_CURRENT) == 1;
        if (smarterBrightness) {
            mReceiver.UpdateAMPM();
        }
        // mOneUI.update(OneUtils.isOnline(mContext));
    }

    public interface OneRefreshUI {
	   void update(boolean cm);
    }
}

