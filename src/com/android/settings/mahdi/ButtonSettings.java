/*
 * Copyright (C) 2014 Mahdi-Rom Project
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
import android.media.AudioSystem;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.VolumePanel;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import java.net.URISyntaxException;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;

import com.android.internal.util.mahdi.DeviceUtils;

public class ButtonSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "ButtonSettings";

    private static final String CATEGORY_POWER = "power_key";
    private static final String CATEGORY_VOLUME = "volume_keys";

    private static final String KEY_NAVIGATION_BAR = "navbar_settings";
    private static final String KEY_HARDWARE_KEYS = "hardware_keys";
    private static final String KEY_PIE_CONTROL = "pie_control";
    private static final String KEY_PA_PIE_CONTROL = "pa_pie_control";
    private static final String KEY_POWER_END_CALL = "power_end_call";
    private static final String BUTTON_VOLUME_DEFAULT = "button_volume_default_screen";
    private static final String KEY_VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String KEY_SWAP_VOLUME_BUTTONS = "swap_volume_buttons";

    private Preference mNavigationBar;
    private Preference mHardwareKeys;
    private Preference mPieControl;
    private Preference mPaPieControl;
    private CheckBoxPreference mPowerEndCall;
    private ListPreference mVolumeDefault;
    private ListPreference mVolumeKeyCursorControl;
    private CheckBoxPreference mSwapVolumeButtons;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.button_settings);

        final PreferenceScreen prefScreen = getPreferenceScreen();
        final ContentResolver resolver = getActivity().getContentResolver();
        Resources res = getResources();

        final PreferenceCategory powerCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_POWER);
        final PreferenceCategory volumeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_VOLUME);

        final boolean hasPowerKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_POWER);

        mNavigationBar = findPreference(KEY_NAVIGATION_BAR);
        mPieControl = findPreference(KEY_PIE_CONTROL);
        mPaPieControl = findPreference(KEY_PA_PIE_CONTROL);

        // Only show the hardware keys config on a device that does not have a navbar
        mHardwareKeys = (PreferenceScreen) findPreference(KEY_HARDWARE_KEYS);
        if (mHardwareKeys != null) {
            if (!res.getBoolean(R.bool.config_has_hardware_buttons)) {
                prefScreen.removePreference(mHardwareKeys);
            }        
        }

        // Power button ends calls.
        mPowerEndCall = (CheckBoxPreference) findPreference(KEY_POWER_END_CALL);
        if (hasPowerKey) {
            if (!Utils.isVoiceCapable(getActivity())) {
                powerCategory.removePreference(mPowerEndCall);
                mPowerEndCall = null;
            }
        } else {
            prefScreen.removePreference(powerCategory);
        }

        if (Utils.hasVolumeRocker(getActivity())) {
            mVolumeDefault = (ListPreference) findPreference(BUTTON_VOLUME_DEFAULT);
            String currentDefault = Settings.System.getString(resolver, Settings.System.VOLUME_KEYS_DEFAULT);
            if (!Utils.isVoiceCapable(getActivity())) {
                removeListEntry(mVolumeDefault, String.valueOf(AudioSystem.STREAM_RING));
            }
            if (currentDefault == null) {
                currentDefault = mVolumeDefault.getEntryValues()[mVolumeDefault.getEntryValues().length - 1].toString();
                mVolumeDefault.setSummary(getString(R.string.button_volume_default_summary));
            }
            mVolumeDefault.setValue(currentDefault);
            mVolumeDefault.setSummary(mVolumeDefault.getEntry());
            mVolumeDefault.setOnPreferenceChangeListener(this);

            int swapVolumeKeys = Settings.System.getInt(getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, 0);
            mSwapVolumeButtons = (CheckBoxPreference)
                    prefScreen.findPreference(KEY_SWAP_VOLUME_BUTTONS);
            mSwapVolumeButtons.setChecked(swapVolumeKeys > 0);

            int cursorControlAction = Settings.System.getInt(resolver,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0);
            mVolumeKeyCursorControl = initActionList(KEY_VOLUME_KEY_CURSOR_CONTROL,
                    cursorControlAction);

            if (!res.getBoolean(R.bool.config_show_volumeRockerWake)) {
                volumeCategory.removePreference(findPreference(Settings.System.VOLUME_WAKE_SCREEN));
            }
        } else {
            prefScreen.removePreference(volumeCategory);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean navBarEnabled = Settings.System.getInt(
                getContentResolver(), Settings.System.NAVIGATION_BAR_SHOW, 0) == 1;
        mNavigationBar.setSummary(navBarEnabled
                ? R.string.enabled : R.string.disabled);

        boolean hwKeysEnabled = Settings.System.getInt(
                getContentResolver(), Settings.System.DISABLE_HARDWARE_KEYS, 1) == 0;
        mHardwareKeys.setSummary(hwKeysEnabled
                ? R.string.enabled : R.string.disabled);

        boolean slimPieEnabled = Settings.System.getInt(
                getContentResolver(), Settings.System.PIE_CONTROLS, 0) == 1;
        mPieControl.setSummary(slimPieEnabled
                ? R.string.enabled : R.string.disabled);

        boolean paPieEnabled = Settings.System.getInt(
                getContentResolver(), Settings.System.PA_PIE_CONTROLS, 0) == 1;
        mPaPieControl.setSummary(paPieEnabled
                ? R.string.enabled : R.string.disabled);

        // Power button ends calls.
        if (mPowerEndCall != null) {
            final int incallPowerBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR,
                    Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_DEFAULT);
            final boolean powerButtonEndsCall =
                      (incallPowerBehavior == Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP);
            mPowerEndCall.setChecked(powerButtonEndsCall);

            UpdateSettings();
            updateVisiblePreferences();
        }
    }

    public void UpdateSettings() {}

    private ListPreference initActionList(String key, int value) {
        ListPreference list = (ListPreference) getPreferenceScreen().findPreference(key);
        list.setValue(Integer.toString(value));
        list.setSummary(list.getEntry());
        list.setOnPreferenceChangeListener(this);
        return list;
    }

    private void handleActionListChange(ListPreference pref, Object newValue, String setting) {
        String value = (String) newValue;
        int index = pref.findIndexOfValue(value);

        pref.setSummary(pref.getEntries()[index]);
        Settings.System.putInt(getContentResolver(), setting, Integer.valueOf(value));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mVolumeDefault) {
            String value = (String) newValue;
            Settings.System.putString(getActivity().getContentResolver(), Settings.System.VOLUME_KEYS_DEFAULT, value);
            updateVolumeDefault(newValue);
            return true;
        } else if (preference == mVolumeKeyCursorControl) {
            handleActionListChange(mVolumeKeyCursorControl, newValue,
                    Settings.System.VOLUME_KEY_CURSOR_CONTROL);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mSwapVolumeButtons) {
            int value = mSwapVolumeButtons.isChecked()
                    ? (Utils.isTablet(getActivity()) ? 2 : 1) : 0;
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.SWAP_VOLUME_KEYS_ON_ROTATION, value);
        } else if (preference == mPowerEndCall) {
            handleTogglePowerButtonEndsCallPreferenceClick();
            return true;
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private void handleTogglePowerButtonEndsCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR, (mPowerEndCall.isChecked()
                        ? Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_HANGUP
                        : Settings.Secure.INCALL_POWER_BUTTON_BEHAVIOR_SCREEN_OFF));
    }

    private void updateVolumeDefault(Object newValue) {
        int index = mVolumeDefault.findIndexOfValue((String) newValue);
        int value = Integer.valueOf((String) newValue);
        Settings.Secure.putInt(getActivity().getContentResolver(),
                Settings.System.VOLUME_KEYS_DEFAULT, value);
        mVolumeDefault.setSummary(mVolumeDefault.getEntries()[index]);
    }

    public void removeListEntry(ListPreference list, String valuetoRemove) {
        ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> values = new ArrayList<CharSequence>();

        for (int i = 0; i < list.getEntryValues().length; i++) {
            if (list.getEntryValues()[i].toString().equals(valuetoRemove)) {
                continue;
            } else {
                entries.add(list.getEntries()[i]);
                values.add(list.getEntryValues()[i]);
            }
        }

        list.setEntries(entries.toArray(new CharSequence[entries.size()]));
        list.setEntryValues(values.toArray(new CharSequence[values.size()]));
    }

    private void updateVisiblePreferences() {
        int slimPie = Settings.System.getInt(getContentResolver(),
                Settings.System.PIE_CONTROLS, 0);
        int paPie = Settings.System.getInt(getContentResolver(),
                Settings.System.PA_PIE_CONTROLS, 0);

        if (slimPie == 1) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.PA_PIE_CONTROLS, 0);
            mPaPieControl.setEnabled(false);
        } else if (paPie == 1) {
            Settings.System.putInt(getContentResolver(),
                Settings.System.PIE_CONTROLS, 0);
            mPieControl.setEnabled(false);
        } else {
            mPieControl.setEnabled(true);
            mPaPieControl.setEnabled(true);
        }
    }
}
