package com.example.snaper

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * This file provides a wrapper for the Build versions. Everytime an android version number gets
 * fixed, this file should be updated. Generally, BuildCompat.isAtLeast*() works before android
 * release is finalized, Build.VERSION_CODES.* works after.
 */
object BuildVersionUtils {
    val isM: Boolean
        get() = Build.VERSION.SDK_INT == Build.VERSION_CODES.M

    val isHeadingWorks: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N


    val isAtLeastNMR1: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

    val isAtLeastO: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    val isBoundsScaledUpByMagnifier: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1


    val isAtLeastP: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    val isAtLeastQ: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    val isAtLeastR: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    val isAtLeastS: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S_V2)
    val isAtLeastS2: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2

    @get:ChecksSdkIntAtLeast(api = 33)
    val isAtLeastT: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    val isAtLeastU:Boolean
        get() =Build.VERSION.SDK_INT >=Build.VERSION_CODES.UPSIDE_DOWN_CAKE

}