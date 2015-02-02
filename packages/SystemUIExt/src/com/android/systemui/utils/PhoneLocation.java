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

package com.android.systemui.utils;

import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.Context;
import java.io.File;
import com.android.internal.util.one.OneUtils;

public class PhoneLocation {

    public static boolean refreshDATA(Context ctx) {
        try {
             if (!(new File(OneUtils.FILEPATH)).exists()) {
                 InputStream is = ctx.getResources().getAssets().open(OneUtils.DB_FILE_NAME);
                 FileOutputStream fos = ctx.openFileOutput(OneUtils.DB_FILE_NAME,
                 Context.MODE_WORLD_READABLE);
                 byte[] buffer = new byte[1024 * 1024];
                 int count = 0;
                 while ((count = is.read(buffer)) > 0) {
                         fos.write(buffer, 0, count);
                 }
                 fos.close();
                 is.close();
             }
             return true;
        } catch (Exception e) {
                 e.printStackTrace();
                 return false;
        }
    }

}
