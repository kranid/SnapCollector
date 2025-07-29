/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.example.snaper

import android.text.TextUtils

/** This class manages efficient loading of classes.  */
object ClassLoadingCache {
    private const val TAG = "ClassLoadingCache"

    // TODO: Use a LRU map instead?
    private val mCachedClasses = HashMap<String, Class<*>?>()

    /**
     * Returns a class by given `className`. It tries to load from the current class loader
     * and caches them.
     *
     * @param className The name of the class to load.
     * @return The class if loaded successfully, null otherwise.
     */
    fun loadOrGetCachedClass(className: String): Class<*>? {
        if (TextUtils.isEmpty(className)) {
            return null
        }
        if (mCachedClasses.containsKey(className)) {
            return mCachedClasses[className]
        }
        var insideClazz: Class<*>? = null
        try {
            val classLoader = ClassLoadingCache::class.java.classLoader
            if (classLoader != null) {
                insideClazz = classLoader.loadClass(className)
            }

        } catch (e: ClassNotFoundException) {
// any log
        }
        mCachedClasses[className] = insideClazz
        return insideClazz
    }

    /** Returns whether a target class is an instance of a reference class.  */
    fun checkInstanceOf(
        targetClassName: CharSequence?, referenceClassName: CharSequence?
    ): Boolean {
        if (targetClassName == null || referenceClassName == null) return false
        if (TextUtils.equals(targetClassName, referenceClassName)) return true
        val referenceClass = loadOrGetCachedClass(referenceClassName.toString())
        val targetClass = loadOrGetCachedClass(targetClassName.toString())
        return referenceClass != null && targetClass != null && referenceClass.isAssignableFrom(
            targetClass
        )
    }

    /** Returns whether a target class is an instance of a reference class.  */
    fun checkInstanceOf(targetClassName: CharSequence?, referenceClass: Class<*>?): Boolean {
        if (targetClassName == null || referenceClass == null) return false
        if (TextUtils.equals(targetClassName, referenceClass.name)) return true
        val targetClass = loadOrGetCachedClass(targetClassName.toString())
        return targetClass != null && referenceClass.isAssignableFrom(targetClass)
    }
}