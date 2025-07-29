/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.example.snapcollector

import android.view.accessibility.AccessibilityWindowInfo
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat

/** Provides a series of utilities for interacting with [AccessibilityWindowInfo] objects.  */
object AccessibilityWindowInfoUtils {
    fun isPictureInPicture(window: AccessibilityWindowInfo?): Boolean {
        return BuildVersionUtils.isAtLeastO && (window != null) && window.isInPictureInPictureMode
    }

    /** Returns the root node of the tree of `windowInfo`.  */
    fun getRoot(windowInfo: AccessibilityWindowInfoCompat):AccessibilityNodeInfoCompat?  {
        var nodeInfo: AccessibilityNodeInfoCompat? = null
        try {
            nodeInfo = windowInfo.root
        } catch (_: SecurityException) {

        }
        return nodeInfo
    }


}