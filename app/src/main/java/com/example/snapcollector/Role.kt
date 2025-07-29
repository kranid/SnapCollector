package com.example.snapcollector

import android.app.ActionBar.Tab
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.webkit.WebView
import android.widget.*
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Role {
    @SerialName("none")
    NONE,

    @SerialName("button")
    BUTTON,

    @SerialName("check_box")
    CHECK_BOX,

    @SerialName("drop_down_list")
    DROP_DOWN_LIST,

    @SerialName("edit_text")
    EDIT_TEXT,

    @SerialName("grid")
    GRID,

    @SerialName("image")
    IMAGE,

    @SerialName("image_button")
    IMAGE_BUTTON,

    @SerialName("list")
    LIST,

    @SerialName("radio_button")
    RADIO_BUTTON,

    @SerialName("seek_control")
    SEEK_CONTROL,

    @SerialName("switch")
    SWITCH,

    @SerialName("tab_bar")
    TAB_BAR,
    @SerialName("tab")
    TAB,

    @SerialName("toggle_button")
    TOGGLE_BUTTON,

    @SerialName("view_group")
    VIEW_GROUP,

    @SerialName("web_view")
    WEB_VIEW,

    @SerialName("pager")
    PAGER,

    @SerialName("checked_text_view")
    CHECKED_TEXT_VIEW,

    @SerialName("progress_bar")
    PROGRESS_BAR,

    @SerialName("action_bar_tab")
    ACTION_BAR_TAB,

    @SerialName("drawer_layout")
    DRAWER_LAYOUT,

    @SerialName("sliding_drawer")
    SLIDING_DRAWER,

    @SerialName("icon_menu")
    ICON_MENU,

    @SerialName("toast")
    TOAST,

    @SerialName("alert_dialog")
    ALERT_DIALOG,

    @SerialName("date_picker_dialog")
    DATE_PICKER_DIALOG,

    @SerialName("time_picker_dialog")
    TIME_PICKER_DIALOG,

    @SerialName("date_picker")
    DATE_PICKER,

    @SerialName("time_picker")
    TIME_PICKER,

    @SerialName("number_picker")
    NUMBER_PICKER,

    @SerialName("scroll_view")
    SCROLL_VIEW,

    @SerialName("horizontal_scroll_view")
    HORIZONTAL_SCROLL_VIEW,

    @SerialName("keyboard_key")
    KEYBOARD_KEY,

    @SerialName("talkback_edit_text_overlay")
    TALKBACK_EDIT_TEXT_OVERLAY,

    @SerialName("text_entry_key")
    TEXT_ENTRY_KEY
}


object RoleUtils {
    private const val TALKBACK_EDIT_TEXT_OVERLAY_CLASSNAME = "TalkbackEditTextOverlay"

    fun getSourceRole(event: AccessibilityEvent?): Role {
        if (event == null) return Role.NONE
        val role = sourceClassNameToRole(event)
        return role
    }

    private fun sourceClassNameToRole(event: AccessibilityEvent?): Role {
        val eventClassName = event?.className ?: return Role.NONE

        return when {
            ClassLoadingCache.checkInstanceOf(eventClassName, "android.widget.Toast\$TN")
                    || ClassLoadingCache.checkInstanceOf(
                eventClassName,
                "android.widget.Toast"
            ) -> Role.TOAST

            ClassLoadingCache.checkInstanceOf(
                eventClassName,
                Tab::class.java
            ) -> Role.ACTION_BAR_TAB

            ClassLoadingCache.checkInstanceOf(
                eventClassName,
                androidx.drawerlayout.widget.DrawerLayout::class.java
            )
                    || ClassLoadingCache.checkInstanceOf(
                eventClassName,
                "androidx.core.widget.DrawerLayout"
            ) -> Role.DRAWER_LAYOUT

            ClassLoadingCache.checkInstanceOf(
                eventClassName,
                SlidingDrawer::class.java
            ) -> Role.SLIDING_DRAWER

            ClassLoadingCache.checkInstanceOf(
                eventClassName,
                "com.android.internal.view.menu.IconMenuView"
            ) -> Role.ICON_MENU

            ClassLoadingCache.checkInstanceOf(
                eventClassName,
                DatePicker::class.java
            ) -> Role.DATE_PICKER

            ClassLoadingCache.checkInstanceOf(
                eventClassName,
                TimePicker::class.java
            ) -> Role.TIME_PICKER

            ClassLoadingCache.checkInstanceOf(
                eventClassName,
                NumberPicker::class.java
            ) -> Role.NUMBER_PICKER

            ClassLoadingCache.checkInstanceOf(
                eventClassName,
                DatePickerDialog::class.java
            ) -> Role.DATE_PICKER_DIALOG

            ClassLoadingCache.checkInstanceOf(
                eventClassName,
                TimePickerDialog::class.java
            ) -> Role.TIME_PICKER_DIALOG

            ClassLoadingCache.checkInstanceOf(eventClassName, AlertDialog::class.java)
                    || ClassLoadingCache.checkInstanceOf(
                eventClassName,
                "androidx.appcompat.app.AlertDialog"
            ) -> Role.ALERT_DIALOG

            else -> Role.NONE
        }
    }

    fun getRole(node: AccessibilityNodeInfoCompat?): Role {
        if (node == null) {
            return Role.NONE
        }

        // Check for text entry key role
        if (node.isTextEntryKey) {
            return Role.TEXT_ENTRY_KEY
        }

        val className = node.className ?: return Role.NONE

        // Identifies a11y overlay added by Talkback on edit texts
        if (ClassLoadingCache.checkInstanceOf(className, TALKBACK_EDIT_TEXT_OVERLAY_CLASSNAME)) {
            return Role.TALKBACK_EDIT_TEXT_OVERLAY
        }

        // Inheritance: View->ImageView
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.ImageView::class.java)) {
            return if (node.isClickable) Role.IMAGE_BUTTON else Role.IMAGE
        }

        // Subclasses of TextView

        // Inheritance: View->TextView->Button->CompoundButton->Switch
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.Switch::class.java)) {
            return Role.SWITCH
        }

        // Inheritance: View->TextView->Button->CompoundButton->ToggleButton
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.ToggleButton::class.java)) {
            return Role.TOGGLE_BUTTON
        }

        // Inheritance: View->TextView->Button->CompoundButton->RadioButton
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.RadioButton::class.java)) {
            return Role.RADIO_BUTTON
        }

        // Inheritance: View->TextView->Button->CompoundButton
        if (ClassLoadingCache.checkInstanceOf(
                className,
                android.widget.CompoundButton::class.java
            )
        ) {
            return Role.CHECK_BOX
        }

        // Inheritance: View->TextView->Button
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.Button::class.java)) {
            return Role.BUTTON
        }

        // Inheritance: View->TextView->CheckedTextView
        if (ClassLoadingCache.checkInstanceOf(
                className,
                android.widget.CheckedTextView::class.java
            )
        ) {
            return Role.CHECKED_TEXT_VIEW
        }

        // Inheritance: View->TextView->EditText
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.EditText::class.java)) {
            return if (node.isEnabled && !node.isEditable) Role.NONE else Role.EDIT_TEXT
        }

        // Subclasses of ProgressBar

        // Inheritance: View->ProgressBar->AbsSeekBar->SeekBar
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.SeekBar::class.java)
            || (AccessibilityNodeInfoUtils.hasValidRangeInfo(node)
                    && AccessibilityNodeInfoUtils.supportsAction(
                node,
                android.R.id.accessibilityActionSetProgress
            ))
        ) {
            return Role.SEEK_CONTROL
        }

        // Inheritance: View->ProgressBar
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.ProgressBar::class.java)
            || (AccessibilityNodeInfoUtils.hasValidRangeInfo(node)
                    && !AccessibilityNodeInfoUtils.supportsAction(
                node,
                android.R.id.accessibilityActionSetProgress
            ))
        ) {
            return Role.PROGRESS_BAR
        }

        if (ClassLoadingCache.checkInstanceOf(
                className, android.inputmethodservice.Keyboard.Key::class.java
            )
        ) {
            return Role.KEYBOARD_KEY
        }

        // Subclasses of ViewGroup

        // Inheritance: View->ViewGroup->AbsoluteLayout->WebView
        if (ClassLoadingCache.checkInstanceOf(className, android.webkit.WebView::class.java)) {
            return Role.WEB_VIEW
        }

        // Inheritance: View->ViewGroup->LinearLayout->TabWidget
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.TabWidget::class.java)) {
            return Role.TAB_BAR
        }

        // Inheritance: View->ViewGroup->FrameLayout->HorizontalScrollView
        if (ClassLoadingCache.checkInstanceOf(
                className,
                android.widget.HorizontalScrollView::class.java
            )
            && node.collectionInfo == null
        ) {
            return Role.HORIZONTAL_SCROLL_VIEW
        }

        // Inheritance: View->ViewGroup->FrameLayout->ScrollView
        if (ClassLoadingCache.checkInstanceOf(className, android.widget.ScrollView::class.java)) {
            return Role.SCROLL_VIEW
        }

        // Inheritance: View->ViewGroup->ViewPager
        if (ClassLoadingCache.checkInstanceOf(
                className,
                androidx.viewpager.widget.ViewPager::class.java
            )
        ) {
            return Role.PAGER
        }

        val collection = node.collectionInfo
        if (collection != null) {
            if (ClassLoadingCache.checkInstanceOf(className, android.widget.ListView::class.java)) {
                return Role.LIST
            }
            if (ClassLoadingCache.checkInstanceOf(className, android.widget.GridView::class.java)) {
                return Role.GRID
            }
            if (collection.rowCount > 1 && collection.columnCount > 1) {
                return Role.GRID
            }
            return Role.LIST
        }

        // Inheritance: View->ViewGroup
        if (ClassLoadingCache.checkInstanceOf(className, android.view.ViewGroup::class.java)) {
            return Role.VIEW_GROUP
        }

        return Role.NONE
    }

    fun getRole(node: AccessibilityNodeInfo?): Role {
        return if (node == null) Role.NONE else getRole(AccessibilityNodeInfoUtils.toCompat(node))
    }

    fun roleToString(role: Role): String = role.name
}
