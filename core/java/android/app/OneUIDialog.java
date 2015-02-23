/*
 * Copyright (C) 2015 The OneUI Open OpenSource Project
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

package android.app;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.view.accessibility.AccessibilityEvent;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.widget.TextView;
import android.widget.LinearLayout;
import com.android.internal.util.one.OneUtils;

import com.android.internal.R;

/**
 * This is a custom dialog to be displayed when the OS is doing
 * work, such as during boot when apps are being optimized.
 *
 * {@hide}
 */
public class OneUIDialog extends Dialog {
    private TextView mMessage;
    private LinearLayout mColorLayout;

    public OneUIDialog(Context ctx) {
        this(ctx, android.R.style.Theme_Holo_Dialog);
    }

    public OneUIDialog(Context ctx, int i) {
        super(ctx, i);
        LayoutInflater inflater = (LayoutInflater)
        ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.one_dlg, null);
        mColorLayout = (LinearLayout) v.findViewById(R.id.one_color_layout);
        mMessage = (TextView) v.findViewById(R.id.one_dlg_title);
        setBackgroundColor(ctx);
        this.setContentView(v);

    }

    private void setBackgroundColor(Context ctx) {
        int mDefaultColor = ctx.getResources().getColor(R.color.default_background);
        int currHourColor = OneUtils.getCurrentHourColor();
        ObjectAnimator animator = ObjectAnimator.ofInt(mColorLayout,
                    "backgroundColor", mDefaultColor, currHourColor);
        animator.setDuration(3000);
        animator.setEvaluator(new ArgbEvaluator());
        animator.start();
    }

    public void setMessage(final CharSequence msg) {
            mMessage.setText(msg);
    }

    // This dialog will consume all events coming in to
    // it, to avoide it trying to do things too early in boot.
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        return true;
    }

    @Override
    public boolean dispatchKeyShortcutEvent(KeyEvent keyEvent) {
        return true;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        return true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent motionEvent) {
        return true;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent motionEvent) {
        return true;
    }
}
