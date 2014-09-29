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

package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.ActivityManagerNative;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.internal.view.RotationPolicy;
import com.android.settings.DreamSettings;
import com.android.settings.hardware.DisplayColor;
import com.android.settings.hardware.DisplayGamma;
import com.android.settings.mahdi.DisplayRotation;
import com.android.settings.Utils;

import org.cyanogenmod.hardware.AdaptiveBacklight;
import org.cyanogenmod.hardware.ColorEnhancement;
import org.cyanogenmod.hardware.HighTouchSensitivity;
import org.cyanogenmod.hardware.SunlightEnhancement;
import org.cyanogenmod.hardware.TapToWake;

import java.util.ArrayList;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, OnPreferenceClickListener {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    /** Categories */
    private static final String CATEGORY_DISPLAY = "display_prefs";
    private static final String CATEGORY_GENERAL = "general_prefs";
    private static final String CATEGORY_ANIMATION = "animation_prefs";
    private static final String CATEGORY_SMART_COVER = "smart_cover_prefs";
    private static final String CATEGORY_LIGHT_OPTIONS = "led_prefs";
    private static final String CATEGORY_WAKEUP = "wakeup_prefs";

    private static final String KEY_ADAPTIVE_BACKLIGHT = "adaptive_backlight";
    private static final String KEY_SUNLIGHT_ENHANCEMENT = "sunlight_enhancement";
    private static final String KEY_COLOR_ENHANCEMENT = "color_enhancement";
    private static final String KEY_DISPLAY_COLOR = "color_calibration";
    private static final String KEY_DISPLAY_GAMMA = "gamma_tuning";
    private static final String KEY_SCREEN_COLOR_SETTINGS = "screencolor_settings";
    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";    
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_DISPLAY_ROTATION = "display_rotation";
    private static final String KEY_LOCKSCREEN_ROTATION = "lockscreen_rotation";
    private static final String KEY_AUTO_ADJUST_TOUCH = "auto_adjust_touch";
    private static final String KEY_POWER_CRT_MODE = "system_power_crt_mode";
    private static final String KEY_NOTIFICATION_LIGHT = "notification_light";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_SCREEN_ON_NOTIFICATION_LED = "screen_on_notification_led";
    private static final String KEY_BATTERY_LIGHT = "battery_light";
    private static final String KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED = "wake_when_plugged_or_unplugged";
    private static final String KEY_TAP_TO_WAKE = "double_tap_wake_gesture";
    private static final String KEY_PROXIMITY_WAKE = "proximity_on_wake";
    private static final String KEY_SCREEN_OFF_GESTURE_SETTINGS = "screen_off_gesture_settings";
    private static final String KEY_SCREEN_OFF_GESTURE_PACKAGE_NAME = "com.slim.device";
    private static final String KEY_TOUCH_CONTROL_SETTINGS = "touch_control_settings";
    private static final String KEY_TOUCH_CONTROL_PACKAGE_NAME = "com.mahdi.touchcontrol";

    /** Strings used for building the summary */
    private static final String ROTATION_ANGLE_0 = "0";
    private static final String ROTATION_ANGLE_90 = "90";
    private static final String ROTATION_ANGLE_180 = "180";
    private static final String ROTATION_ANGLE_270 = "270";
    private static final String ROTATION_ANGLE_DELIM = ", ";
    private static final String ROTATION_ANGLE_DELIM_FINAL = " & ";

    private static final int DLG_GLOBAL_CHANGE_WARNING = 1;

    private CheckBoxPreference mAdaptiveBacklight;
    private CheckBoxPreference mSunlightEnhancement;
    private CheckBoxPreference mColorEnhancement;
    private PreferenceScreen mScreenColorSettings;
    private PreferenceScreen mDisplayRotationPreference;    
    private FontDialogPreference mFontSizePref;
    private CheckBoxPreference mHighTouchSensitivity;
    private ListPreference mCrtMode;
    private PreferenceCategory mLightOptions;
    private CheckBoxPreference mNotificationPulse;
    private PreferenceScreen mNotificationLight;
    private CheckBoxPreference mScreenOnLed;
    private PreferenceScreen mBatteryPulse;
    private CheckBoxPreference mWakeWhenPluggedOrUnplugged;
    private CheckBoxPreference mTapToWake;
    private PreferenceScreen mScreenOffGesture;
    private PreferenceScreen mTouchControl;

    private final Configuration mCurConfig = new Configuration();
    
    private ListPreference mScreenTimeoutPreference;
    private Preference mScreenSaverPreference;

    private ContentObserver mAccelerometerRotationObserver =
            new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            updateDisplayRotationPreferenceDescription();
        }
    };

    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
        @Override
        public void onChange() {
            updateDisplayRotationPreferenceDescription();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();
        addPreferencesFromResource(R.xml.display_settings);
        PreferenceScreen prefSet = getPreferenceScreen();

        /** Display category */
        PreferenceCategory displayPrefs = (PreferenceCategory) findPreference(CATEGORY_DISPLAY);

        mAdaptiveBacklight = (CheckBoxPreference) findPreference(KEY_ADAPTIVE_BACKLIGHT);
        if (!isAdaptiveBacklightSupported()) {
            displayPrefs.removePreference(mAdaptiveBacklight);
            mAdaptiveBacklight = null;
        }

        mSunlightEnhancement = (CheckBoxPreference) findPreference(KEY_SUNLIGHT_ENHANCEMENT);
        if (!isSunlightEnhancementSupported()) {
            displayPrefs.removePreference(mSunlightEnhancement);
            mSunlightEnhancement = null;
        }

        mColorEnhancement = (CheckBoxPreference) findPreference(KEY_COLOR_ENHANCEMENT);
        if (!isColorEnhancementSupported()) {
            displayPrefs.removePreference(mColorEnhancement);
            mColorEnhancement = null;
        }

        if (!DisplayColor.isSupported()) {
            displayPrefs.removePreference(findPreference(KEY_DISPLAY_COLOR));
        }

        if (!DisplayGamma.isSupported()) {
            displayPrefs.removePreference(findPreference(KEY_DISPLAY_GAMMA));
        }

        mScreenColorSettings = (PreferenceScreen) findPreference(KEY_SCREEN_COLOR_SETTINGS);
        if (!isPostProcessingSupported()) {
            displayPrefs.removePreference(mScreenColorSettings);
        }

        /** General category */
        PreferenceCategory generalPrefs = (PreferenceCategory) findPreference(CATEGORY_GENERAL);

        mDisplayRotationPreference = (PreferenceScreen) findPreference(KEY_DISPLAY_ROTATION);
        
        final CheckBoxPreference lockScreenRotation =
                (CheckBoxPreference) findPreference(KEY_LOCKSCREEN_ROTATION);
        if (lockScreenRotation != null) {
            if (!getResources().getBoolean(com.android.internal.R.bool.config_enableLockScreenRotation)) {
                getPreferenceScreen().removePreference(lockScreenRotation);
            }
        }

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && res.getBoolean(
                        com.android.internal.R.bool.config_dreamsSupported) == false) {
            generalPrefs.removePreference(mScreenSaverPreference);
        }

        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);
        updateDisplayRotationPreferenceDescription();

        mFontSizePref = (FontDialogPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mFontSizePref.setOnPreferenceClickListener(this);

        mHighTouchSensitivity = (CheckBoxPreference) findPreference(KEY_AUTO_ADJUST_TOUCH);
        if (!isHighTouchSensitivitySupported()) {
            generalPrefs.removePreference(mHighTouchSensitivity);
            mHighTouchSensitivity = null;
        }

        /** Animation category */
        PreferenceCategory animationOptions = (PreferenceCategory) prefSet.findPreference(CATEGORY_ANIMATION);

        boolean electronBeamFadesConfig = getResources().getBoolean(
                com.android.internal.R.bool.config_animateScreenLights);
        mCrtMode = (ListPreference) prefSet.findPreference(KEY_POWER_CRT_MODE);
        if (!electronBeamFadesConfig && mCrtMode != null) {
            int crtMode = Settings.System.getInt(getContentResolver(),
                    Settings.System.SYSTEM_POWER_CRT_MODE, 1);
            mCrtMode.setValue(String.valueOf(crtMode));
            mCrtMode.setSummary(mCrtMode.getEntry());
            mCrtMode.setOnPreferenceChangeListener(this);
        } else if (animationOptions != null) {
            prefSet.removePreference(animationOptions);
        }		

        /** LED category */
        mLightOptions = (PreferenceCategory) prefSet.findPreference(CATEGORY_LIGHT_OPTIONS);

        mNotificationPulse = (CheckBoxPreference) findPreference(KEY_NOTIFICATION_PULSE);
        mNotificationLight = (PreferenceScreen) findPreference(KEY_NOTIFICATION_LIGHT);
        mScreenOnLed = (CheckBoxPreference) findPreference(KEY_SCREEN_ON_NOTIFICATION_LED);
        mBatteryPulse = (PreferenceScreen) findPreference(KEY_BATTERY_LIGHT);
        if (mNotificationPulse != null && mNotificationLight != null && mBatteryPulse != null) {
            if (getResources().getBoolean(
                    com.android.internal.R.bool.config_intrusiveNotificationLed)) {
                 if (getResources().getBoolean(
                         com.android.internal.R.bool.config_multiColorNotificationLed)) {
                     mLightOptions.removePreference(mNotificationPulse);
                     mLightOptions.removePreference(mScreenOnLed);
                     updateLightPulseDescription();
                 } else {
                     mLightOptions.removePreference(mNotificationLight);
                     try {
                         mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                                 Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                     } catch (SettingNotFoundException e) {
                         e.printStackTrace();
                     }
                 }
            }

            if (!getResources().getBoolean(
                    com.android.internal.R.bool.config_intrusiveBatteryLed)) {
                mLightOptions.removePreference(mBatteryPulse);
            } else {
                updateBatteryPulseDescription();
            }
        }

        /** Smart cover category */
        if (res.getIntArray(
                com.android.internal.R.array.config_smartCoverWindowCoords).length != 4) {
            getPreferenceScreen().removePreference(findPreference(CATEGORY_SMART_COVER));
        }

        /** Wakeup category */
        PreferenceCategory wakeupPrefs = (PreferenceCategory) findPreference(CATEGORY_WAKEUP);

        mWakeWhenPluggedOrUnplugged =
                (CheckBoxPreference) findPreference(KEY_WAKE_WHEN_PLUGGED_OR_UNPLUGGED);

        boolean wakeUpWhenPluggedOrUnpluggedConfig = getResources().getBoolean(
                com.android.internal.R.bool.config_unplugTurnsOnScreen);

        mWakeWhenPluggedOrUnplugged.setChecked(Settings.Global.getInt(resolver,
                Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                (wakeUpWhenPluggedOrUnpluggedConfig ? 1 : 0)) == 1);

        mTapToWake = (CheckBoxPreference) findPreference(KEY_TAP_TO_WAKE);
        if (!isTapToWakeSupported()) {
            wakeupPrefs.removePreference(mTapToWake);
            mTapToWake = null;
        }

        boolean proximityCheckOnWait = res.getBoolean(
                com.android.internal.R.bool.config_proximityCheckOnWake);
        if (!proximityCheckOnWait) {
            wakeupPrefs.removePreference(findPreference(KEY_PROXIMITY_WAKE));
            Settings.System.putInt(getContentResolver(), Settings.System.PROXIMITY_ON_WAKE, 0);
        }

        mScreenOffGesture = (PreferenceScreen) findPreference(KEY_SCREEN_OFF_GESTURE_SETTINGS);
        if (!Utils.isPackageInstalled(getActivity(), KEY_SCREEN_OFF_GESTURE_PACKAGE_NAME)) {
            wakeupPrefs.removePreference(mScreenOffGesture);
        }

        mTouchControl = (PreferenceScreen) findPreference(KEY_TOUCH_CONTROL_SETTINGS);
        if (!Utils.isPackageInstalled(getActivity(), KEY_TOUCH_CONTROL_PACKAGE_NAME)) {
            wakeupPrefs.removePreference(mTouchControl);
        }
    }

    private void updateDisplayRotationPreferenceDescription() {
        if (mDisplayRotationPreference == null) {
            // The preference was removed, do nothing
            return;
        }

        // We have a preference, lets update the summary
        StringBuilder summary = new StringBuilder();
        Boolean rotationEnabled = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION, 0) != 0;
        int mode = Settings.System.getInt(getContentResolver(),
                Settings.System.ACCELEROMETER_ROTATION_ANGLES,
                DisplayRotation.ROTATION_0_MODE|DisplayRotation.ROTATION_90_MODE|DisplayRotation.ROTATION_270_MODE);

        if (!rotationEnabled) {
            summary.append(getString(R.string.display_rotation_disabled));
        } else {
            ArrayList<String> rotationList = new ArrayList<String>();
            String delim = "";
            summary.append(getString(R.string.display_rotation_enabled) + " ");
            if ((mode & DisplayRotation.ROTATION_0_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_0);
            }
            if ((mode & DisplayRotation.ROTATION_90_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_90);
            }
            if ((mode & DisplayRotation.ROTATION_180_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_180);
            }
            if ((mode & DisplayRotation.ROTATION_270_MODE) != 0) {
                rotationList.add(ROTATION_ANGLE_270);
            }
            for(int i=0;i<rotationList.size();i++) {
                summary.append(delim).append(rotationList.get(i));
                if (rotationList.size() >= 2 && (rotationList.size() - 2) == i) {
                    delim = " " + ROTATION_ANGLE_DELIM_FINAL + " ";
                } else {
                    delim = ROTATION_ANGLE_DELIM + " ";
                }
            }
            summary.append(" " + getString(R.string.display_rotation_unit));
        }
        mDisplayRotationPreference.setSummary(summary);
    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            if (entries == null || entries.length == 0) {
                summary = "";
            } else {
                int best = 0;
                for (int i = 0; i < values.length; i++) {
                    long timeout = Long.parseLong(values[i].toString());
                    if (currentTimeout >= timeout) {
                        best = i;
                    }
                }
                summary = preference.getContext().getString(R.string.screen_timeout_summary,
                        entries[best]);
            }
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else if (revisedValues.size() > 0
                    && Long.parseLong(revisedValues.get(revisedValues.size() - 1).toString())
                    == maxTimeout) {
                // If the last one happens to be the same as the max timeout, select that
                screenTimeoutPreference.setValue(String.valueOf(maxTimeout));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        RotationPolicy.registerRotationPolicyListener(getActivity(),
                mRotationPolicyListener);

	final ContentResolver resolver = getContentResolver();

        // Display rotation observer
        resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true,
                mAccelerometerRotationObserver);

        updateDisplayRotationPreferenceDescription();
        updateState();
        updateLightPulseDescription();
        updateBatteryPulseDescription();

        if (mAdaptiveBacklight != null) {
            mAdaptiveBacklight.setChecked(AdaptiveBacklight.isEnabled());
        }

        if (mSunlightEnhancement != null) {
            if (SunlightEnhancement.isAdaptiveBacklightRequired() &&
                    !AdaptiveBacklight.isEnabled()) {
                mSunlightEnhancement.setEnabled(false);
            } else {
                mSunlightEnhancement.setChecked(SunlightEnhancement.isEnabled());
            }
        }

        if (mTapToWake != null) {
            mTapToWake.setChecked(TapToWake.isEnabled());
        }

        if (mColorEnhancement != null) {
            mColorEnhancement.setChecked(ColorEnhancement.isEnabled());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                mRotationPolicyListener);

	// Display rotation observer
        getContentResolver().unregisterContentObserver(mAccelerometerRotationObserver);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        if (dialogId == DLG_GLOBAL_CHANGE_WARNING) {
            return Utils.buildGlobalChangeWarningDialog(getActivity(),
                    R.string.global_font_change_title,
                    new Runnable() {
                        public void run() {
                            mFontSizePref.click();
                        }
                    });
        }
        return null;
    }

    private void updateState() {        
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();
    }

    private void updateScreenSaverSummary() {
        if (mScreenSaverPreference != null) {
            mScreenSaverPreference.setSummary(
                    DreamSettings.getSummaryTextWithDreamName(getActivity()));
        }
    }

    private void updateLightPulseDescription() {
        if (mNotificationLight == null) {
            return;
        }
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.NOTIFICATION_LIGHT_PULSE, 0) == 1) {
            mNotificationLight.setSummary(getString(R.string.enabled));
        } else {
            mNotificationLight.setSummary(getString(R.string.disabled));
        }
    }

    private void updateBatteryPulseDescription() {
        if (mBatteryPulse == null) {
            return;
        }
        if (Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.BATTERY_LIGHT_ENABLED, 1) == 1) {
            mBatteryPulse.setSummary(getString(R.string.enabled));
        } else {
            mBatteryPulse.setSummary(getString(R.string.disabled));
        }
    }

    /**
     * Reads the current font size and sets the value in the summary text
     */
    public void readFontSizePreference(Preference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // report the current size in the summary text
        final Resources res = getResources();
        String fontDesc = FontDialogPreference.getFontSizeDescription(res, mCurConfig.fontScale);
        pref.setSummary(getString(R.string.summary_font_size, fontDesc));
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mNotificationPulse) {
            boolean value = mNotificationPulse.isChecked();
            Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    value ? 1 : 0);
            return true;
        } else if (preference == mWakeWhenPluggedOrUnplugged) {
            Settings.Global.putInt(getContentResolver(),
                    Settings.Global.WAKE_WHEN_PLUGGED_OR_UNPLUGGED,
                    mWakeWhenPluggedOrUnplugged.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mTapToWake) {
            return TapToWake.setEnabled(mTapToWake.isChecked());
        } else if (preference == mAdaptiveBacklight) {
            if (mSunlightEnhancement != null &&
                    SunlightEnhancement.isAdaptiveBacklightRequired()) {
                mSunlightEnhancement.setEnabled(mAdaptiveBacklight.isChecked());
            }
            return AdaptiveBacklight.setEnabled(mAdaptiveBacklight.isChecked());
        } else if (preference == mSunlightEnhancement) {
            return SunlightEnhancement.setEnabled(mSunlightEnhancement.isChecked());
        } else if (preference == mHighTouchSensitivity) {
            return HighTouchSensitivity.setEnabled(mHighTouchSensitivity.isChecked());
        } else if (preference == mColorEnhancement) {
            return ColorEnhancement.setEnabled(mColorEnhancement.isChecked());
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }
        if (KEY_POWER_CRT_MODE.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            int index = mCrtMode.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SYSTEM_POWER_CRT_MODE,
                    value);
            mCrtMode.setSummary(mCrtMode.getEntries()[index]);
        }
        return true;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == mFontSizePref) {
            if (Utils.hasMultipleUsers(getActivity())) {
                showDialog(DLG_GLOBAL_CHANGE_WARNING);
                return true;
            } else {
                mFontSizePref.click();
            }
        }
        return false;
    }

    /**
     * Restore the properties associated with this preference on boot
     * @param ctx A valid context
     */
    public static void restore(Context ctx) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (isAdaptiveBacklightSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_ADAPTIVE_BACKLIGHT,
                    AdaptiveBacklight.isEnabled());
            if (!AdaptiveBacklight.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore adaptive backlight settings.");
            } else {
                Log.d(TAG, "Adaptive backlight settings restored.");
            }
        }

        if (isSunlightEnhancementSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_SUNLIGHT_ENHANCEMENT,
                    SunlightEnhancement.isEnabled());
            if (SunlightEnhancement.isAdaptiveBacklightRequired() &&
                    !AdaptiveBacklight.isEnabled()) {
                SunlightEnhancement.setEnabled(false);
                Log.d(TAG, "SRE requires CABC, disabled");
            } else {
                if (!SunlightEnhancement.setEnabled(enabled)) {
                    Log.e(TAG, "Failed to restore SRE settings.");
                } else {
                    Log.d(TAG, "SRE settings restored.");
                }
            }
        }

        if (isColorEnhancementSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_COLOR_ENHANCEMENT,
                    ColorEnhancement.isEnabled());
            if (!ColorEnhancement.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore color enhancement settings.");
            } else {
                Log.d(TAG, "Color enhancement settings restored.");
            }
        }

        if (isTapToWakeSupported()) {
            final boolean enabled = prefs.getBoolean(KEY_TAP_TO_WAKE,
                    TapToWake.isEnabled());
            if (!TapToWake.setEnabled(enabled)) {
                Log.e(TAG, "Failed to restore tap-to-wake settings.");
            } else {
                Log.d(TAG, "Tap-to-wake settings restored.");
            }
        }
    }

    private boolean isPostProcessingSupported() {
        boolean ret = true;
        final PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.qualcomm.display", PackageManager.GET_META_DATA);
        } catch (NameNotFoundException e) {
            ret = false;
        }
        return ret;
    }

    private static boolean isAdaptiveBacklightSupported() {
        try {
            return AdaptiveBacklight.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isSunlightEnhancementSupported() {
        try {
            return SunlightEnhancement.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isHighTouchSensitivitySupported() {
        try {
            return HighTouchSensitivity.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isColorEnhancementSupported() {
        try {
            return ColorEnhancement.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }

    private static boolean isTapToWakeSupported() {
        try {
            return TapToWake.isSupported();
        } catch (NoClassDefFoundError e) {
            // Hardware abstraction framework not installed
            return false;
        }
    }
}
