/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.R;
import com.android.systemui.statusbar.phone.SystemUIDialog;


public class OneUI implements OneService.OneRefreshUI {

    private final Context mContext;

    private static final String TAG_NOTIFICATION = "one_api";
    private static final int ID_NOTIFICATION = 168;

    private final NotificationManager mNoMan;

    private final Handler mHandler = new Handler();
    private final Receiver mReceiver = new Receiver();

    private SystemUIDialog mShowialog;

    public OneUI(Context context) {
        mContext = context;
        mNoMan = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mReceiver.init();
    }

    @Override
    public void update(boolean cm) {return;}

    private void showPushNotification(String message) {
        final Notification.Builder nb = new Notification.Builder(mContext)
                .setSmallIcon(R.drawable.ic_power_saver)
                .setContentTitle(mContext.getString(R.string.one_message_title))
                .setContentText(message)
                .setOngoing(false)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SYSTEM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(mContext.getResources().getColor(
                        com.android.internal.R.color.battery_saver_mode_color));
        mNoMan.notifyAsUser(TAG_NOTIFICATION, ID_NOTIFICATION, nb.build(), UserHandle.CURRENT);
    }

    private void showWaitNotification() {
        final Notification.Builder nb = new Notification.Builder(mContext)
                .setSmallIcon(R.drawable.ic_power_saver)
                .setContentTitle(mContext.getString(R.string.one_new_ota_title))
                .setContentText(mContext.getString(R.string.one_wait_nt_ct))
                .setOngoing(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SYSTEM)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setColor(mContext.getResources().getColor(
                        com.android.internal.R.color.battery_saver_mode_color));
        mNoMan.notifyAsUser(TAG_NOTIFICATION, ID_NOTIFICATION, nb.build(), UserHandle.CURRENT);
    }

    private void showOtaDialog(String message) {
        if (mShowialog != null) return;
        final SystemUIDialog d = new SystemUIDialog(mContext);
        d.setTitle(R.string.one_new_ota_title);
        d.setMessage(message);
        d.setNegativeButton(R.string.one_wait_title, mWaitDownload);
        d.setPositiveButton(R.string.one_download_title, mStartDownload);
        d.setShowForAllUsers(true);
        d.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mShowialog = null;
                showWaitNotification();
            }
        });
        d.show();
        mShowialog = d;
    }

    private final class Receiver extends BroadcastReceiver {

        public void init() {
            IntentFilter filter = new IntentFilter();
            mContext.registerReceiver(this, filter, null, mHandler);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
        }
    }

    private final OnClickListener mWaitDownload = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    mShowialog = null;
                    showWaitNotification();
                }
            });
        }
    };

    private final OnClickListener mStartDownload = new OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    mShowialog = null;
                }
            });
        }
    };
}
