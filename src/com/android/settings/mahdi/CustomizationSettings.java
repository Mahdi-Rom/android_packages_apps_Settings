/*
 * Copyright (C) 2012 The Mahdi Rom project
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

package com.android.settings.mahdi;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.IWindowManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class CustomizationSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "CustomizationSettings";

    private static final String KEY_STATUS_BAR_CLOCK = "clock_style_pref";
    private static final String STATUS_BAR_BATTERY = "status_bar_battery";
    private static final String STATUS_BAR_BATTERY_SHOW_PERCENT = "status_bar_battery_show_percent";
    private static final String STATUS_BAR_STYLE_HIDDEN = "4";
    private static final String STATUS_BAR_STYLE_TEXT = "6";
    private static final String KEY_IMMERSIVE_MODE = "immersive_mode";
    private static final String KEY_DISABLE_SYSTEM_GESTURES = "disable_system_gestures";
    private static final String KEY_EDGE_SERVICE_FOR_GESTURES = "edge_service_for_gestures";
    private static final String KEY_DISABLE_IN_LOCKSCREEN = "global_immersive_mode_system_bars_visibility";
    private static final String KEY_DISABLE_FORCED_NAVBAR = "disable_forced_navbar";

    private PreferenceScreen mClockStyle;
    private ListPreference mStatusBarBattery;
    private SystemSettingCheckBoxPreference mStatusBarBatteryShowPercent;
    private ListPreference mImmersiveModePref;
    private SystemSettingListPreference mDisableSystemGestures;
    private SystemSettingCheckBoxPreference mEdgeServiceForGestures;
    private SystemSettingCheckBoxPreference mDisableInLockscreen;
    private SystemSettingCheckBoxPreference mDisableForcedNavbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.customization_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mClockStyle = (PreferenceScreen) prefSet.findPreference(KEY_STATUS_BAR_CLOCK);
        if (mClockStyle != null) {
            updateClockStyleDescription();
        }

        mStatusBarBattery = (ListPreference) findPreference(STATUS_BAR_BATTERY);
        mStatusBarBatteryShowPercent =
                (SystemSettingCheckBoxPreference) findPreference(STATUS_BAR_BATTERY_SHOW_PERCENT);

        int batteryStyle = Settings.System.getInt(resolver, Settings.System.STATUS_BAR_BATTERY, 0);
        mStatusBarBattery.setValue(String.valueOf(batteryStyle));
        mStatusBarBattery.setSummary(mStatusBarBattery.getEntry());
        mStatusBarBattery.setOnPreferenceChangeListener(this);

        mImmersiveModePref = (ListPreference) prefSet.findPreference(KEY_IMMERSIVE_MODE);
        mImmersiveModePref.setOnPreferenceChangeListener(this);
        int immersiveModeValue = Settings.System.getInt(getContentResolver(), Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, 0);
        mImmersiveModePref.setValue(String.valueOf(immersiveModeValue));
        updateImmersiveModeSummary(immersiveModeValue);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mStatusBarBattery) {
            int batteryStyle = Integer.valueOf((String) objValue);
            int index = mStatusBarBattery.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(), Settings.System.STATUS_BAR_BATTERY, batteryStyle);
            mStatusBarBattery.setSummary(mStatusBarBattery.getEntries()[index]);
            enableStatusBarBatteryDependents((String) objValue);
        } else if (preference == mImmersiveModePref) {
            int immersiveModeValue = Integer.valueOf((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.GLOBAL_IMMERSIVE_MODE_STYLE, immersiveModeValue);
            updateImmersiveModeSummary(immersiveModeValue);   
        } else {
            return false;
        }
        return true;
    }        

    @Override
    public void onResume() {
        super.onResume();
        updateClockStyleDescription();
    }

    private void updateClockStyleDescription() {
        if (Settings.System.getInt(getContentResolver(),
               Settings.System.STATUS_BAR_CLOCK, 1) == 1) {
            mClockStyle.setSummary(getString(R.string.enabled));
        } else {
            mClockStyle.setSummary(getString(R.string.disabled));
         }
    }

    private void enableStatusBarBatteryDependents(String value) {
        boolean enabled = !(value.equals(STATUS_BAR_STYLE_TEXT)
                || value.equals(STATUS_BAR_STYLE_HIDDEN));
        mStatusBarBatteryShowPercent.setEnabled(enabled);
    }

    private void updateImmersiveModeSummary(int value) {
        Resources res = getResources();

        if (value == 0) {
            /* expanded desktop deactivated */
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_GLOBAL_IMMERSIVE_MODE_ENABLED, 0);
            mImmersiveModePref.setSummary(res.getString(R.string.immersive_mode_disabled));
        } else if (value == 1) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_GLOBAL_IMMERSIVE_MODE_ENABLED, 1);
            String statusBarPresent = res.getString(R.string.immersive_mode_summary_status_bar);
            mImmersiveModePref.setSummary(res.getString(R.string.summary_immersive_mode, statusBarPresent));
        } else if (value == 2) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.POWER_MENU_GLOBAL_IMMERSIVE_MODE_ENABLED, 1);
            String statusBarPresent = res.getString(R.string.immersive_mode_summary_no_status_bar);
            mImmersiveModePref.setSummary(res.getString(R.string.summary_immersive_mode, statusBarPresent));
        }

        mDisableSystemGestures =
                (SystemSettingListPreference) findPreference(KEY_DISABLE_SYSTEM_GESTURES);
        mEdgeServiceForGestures =
                (SystemSettingCheckBoxPreference) findPreference(KEY_EDGE_SERVICE_FOR_GESTURES);
        mDisableInLockscreen =
                (SystemSettingCheckBoxPreference) findPreference(KEY_DISABLE_IN_LOCKSCREEN);
        mDisableForcedNavbar =
                (SystemSettingCheckBoxPreference) findPreference(KEY_DISABLE_FORCED_NAVBAR);

        if (value == 0) {
            mDisableSystemGestures.setEnabled(false);
            mEdgeServiceForGestures.setEnabled(false);
            mDisableInLockscreen.setEnabled(false);
            mDisableForcedNavbar.setEnabled(false);
        } else {
            mDisableSystemGestures.setEnabled(true);
            mEdgeServiceForGestures.setEnabled(true);
            mDisableInLockscreen.setEnabled(true);
            mDisableForcedNavbar.setEnabled(true);
        }
    }

}
