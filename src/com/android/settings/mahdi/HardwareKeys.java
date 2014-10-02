/*
 * Copyright (C) 2014 Slimroms
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.text.TextUtils;

import com.android.internal.util.mahdi.AppHelper;
import com.android.internal.util.mahdi.ButtonsConstants;
import com.android.internal.util.mahdi.DeviceUtils;
import com.android.internal.util.mahdi.DeviceUtils.FilteredDeviceFeaturesArray;
import com.android.internal.util.mahdi.HwKeyHelper;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.cyanogenmod.ButtonBacklightBrightness;
import com.android.settings.mahdi.util.ShortcutPickerHelper;
import com.android.internal.util.mahdi.QSUtils;

import org.cyanogenmod.hardware.KeyDisabler;

import java.util.*;

public class HardwareKeys extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnPreferenceClickListener,
        ShortcutPickerHelper.OnPickListener {

    private static final String TAG = "HardwareKeys";

    private static final String CATEGORY_KEYS = "button_keys";
    private static final String CATEGORY_BACK = "button_keys_back";
    private static final String CATEGORY_HOME = "button_keys_home";
    private static final String CATEGORY_MENU = "button_keys_menu";
    private static final String CATEGORY_ASSIST = "button_keys_assist";
    private static final String CATEGORY_APPSWITCH = "button_keys_appSwitch";

    private static final String KEYS_DISABLE_HW_KEYS = "disable_hardware_keys";
    private static final String KEYS_CATEGORY_BINDINGS = "keys_bindings";
    private static final String KEYS_ENABLE_CUSTOM = "enable_hardware_rebind";
    private static final String KEYS_BACK_PRESS = "keys_back_press";
    private static final String KEYS_BACK_LONG_PRESS = "keys_back_long_press";
    private static final String KEYS_BACK_DOUBLE_TAP = "keys_back_double_tap";
    private static final String KEYS_HOME_PRESS = "keys_home_press";
    private static final String KEYS_HOME_LONG_PRESS = "keys_home_long_press";
    private static final String KEYS_HOME_DOUBLE_TAP = "keys_home_double_tap";
    private static final String KEYS_MENU_PRESS = "keys_menu_press";
    private static final String KEYS_MENU_LONG_PRESS = "keys_menu_long_press";
    private static final String KEYS_MENU_DOUBLE_TAP = "keys_menu_double_tap";
    private static final String KEYS_ASSIST_PRESS = "keys_assist_press";
    private static final String KEYS_ASSIST_LONG_PRESS = "keys_assist_long_press";
    private static final String KEYS_ASSIST_DOUBLE_TAP = "keys_assist_double_tap";
    private static final String KEYS_APP_SWITCH_PRESS = "keys_app_switch_press";
    private static final String KEYS_APP_SWITCH_LONG_PRESS = "keys_app_switch_long_press";
    private static final String KEYS_APP_SWITCH_DOUBLE_TAP = "keys_app_switch_double_tap";
    private static final String KEY_BUTTON_BACKLIGHT = "button_backlight";
    private static final String LOCKSCREENLONG_PRESS_HOME = "lockscreen_long_press_home";
    private static final String KEY_HOME_ANSWER_CALL = "home_answer_call";

    private static final int DLG_SHOW_WARNING_DIALOG = 0;
    private static final int DLG_SHOW_ACTION_DIALOG  = 1;
    private static final int DLG_RESET_TO_DEFAULT    = 2;

    private static final int MENU_RESET = Menu.FIRST;

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME        = 0x01;
    private static final int KEY_MASK_BACK       = 0x02;
    private static final int KEY_MASK_MENU       = 0x04;
    private static final int KEY_MASK_ASSIST     = 0x08;
    private static final int KEY_MASK_APP_SWITCH = 0x10;

    private CheckBoxPreference mDisableHwKeys;
    private CheckBoxPreference mEnableCustomBindings;
    private Preference mBackPressAction;
    private Preference mBackLongPressAction;
    private Preference mBackDoubleTapAction;
    private Preference mHomePressAction;
    private Preference mHomeLongPressAction;
    private Preference mHomeDoubleTapAction;
    private Preference mMenuPressAction;
    private Preference mMenuLongPressAction;
    private Preference mMenuDoubleTapAction;
    private Preference mAssistPressAction;
    private Preference mAssistLongPressAction;
    private Preference mAssistDoubleTapAction;
    private Preference mAppSwitchPressAction;
    private Preference mAppSwitchLongPressAction;
    private Preference mAppSwitchDoubleTapAction;
    private ListPreference mLongHomeAction;
    private CheckBoxPreference mHomeAnswerCall;
    private ListPreference[] mActions;

    private Handler mHandler;

    private boolean mCheckPreferences;
    private Map<String, String> mKeySettings = new HashMap<String, String>();

    private ShortcutPickerHelper mPicker;
    private String mPendingSettingsKey;
    private static FilteredDeviceFeaturesArray sFinalActionDialogArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPicker = new ShortcutPickerHelper(getActivity(), this);

        // Before we start filter out unsupported options on the
        // ListPreference values and entries
        Resources res = getResources();
        sFinalActionDialogArray = new FilteredDeviceFeaturesArray();
        sFinalActionDialogArray = DeviceUtils.filterUnsupportedDeviceFeatures(getActivity(),
            res.getStringArray(res.getIdentifier(
                    "shortcut_action_hwkey_values", "array", "com.android.settings")),
            res.getStringArray(res.getIdentifier(
                    "shortcut_action_hwkey_entries", "array", "com.android.settings")));

        // Attach final settings screen.
        reloadSettings();

        setHasOptionsMenu(true);
    }

    private PreferenceScreen reloadSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        mHandler = new Handler();

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.hardware_keys);
        prefs = getPreferenceScreen();

        int deviceKeys = getResources().getInteger(
                com.android.internal.R.integer.config_deviceHardwareKeys);

        boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
        boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
        boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
        boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
        boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

        PreferenceCategory keysCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_KEYS);
        PreferenceCategory keysBackCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_BACK);
        PreferenceCategory keysHomeCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_HOME);
        PreferenceCategory keysMenuCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_MENU);
        PreferenceCategory keysAssistCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_ASSIST);
        PreferenceCategory keysAppSwitchCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_APPSWITCH);

        mDisableHwKeys = (CheckBoxPreference) prefs.findPreference(
                KEYS_DISABLE_HW_KEYS);
        mEnableCustomBindings = (CheckBoxPreference) prefs.findPreference(
                KEYS_ENABLE_CUSTOM);
        mBackPressAction = (Preference) prefs.findPreference(
                KEYS_BACK_PRESS);
        mBackLongPressAction = (Preference) prefs.findPreference(
                KEYS_BACK_LONG_PRESS);
        mBackDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_BACK_DOUBLE_TAP);
        mHomePressAction = (Preference) prefs.findPreference(
                KEYS_HOME_PRESS);
        mHomeLongPressAction = (Preference) prefs.findPreference(
                KEYS_HOME_LONG_PRESS);
        mHomeDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_HOME_DOUBLE_TAP);
        mMenuPressAction = (Preference) prefs.findPreference(
                KEYS_MENU_PRESS);
        mMenuLongPressAction = (Preference) prefs.findPreference(
                KEYS_MENU_LONG_PRESS);
        mMenuDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_MENU_DOUBLE_TAP);
        mAssistPressAction = (Preference) prefs.findPreference(
                KEYS_ASSIST_PRESS);
        mAssistLongPressAction = (Preference) prefs.findPreference(
                KEYS_ASSIST_LONG_PRESS);
        mAssistDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_ASSIST_DOUBLE_TAP);
        mAppSwitchPressAction = (Preference) prefs.findPreference(
                KEYS_APP_SWITCH_PRESS);
        mAppSwitchLongPressAction = (Preference) prefs.findPreference(
                KEYS_APP_SWITCH_LONG_PRESS);
        mAppSwitchDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_APP_SWITCH_DOUBLE_TAP);

        // Home button answers calls.
        mHomeAnswerCall = (CheckBoxPreference) findPreference(KEY_HOME_ANSWER_CALL);

        if (hasBackKey) {
            // Back key
            setupOrUpdatePreference(mBackPressAction,
                    HwKeyHelper.getPressOnBackBehavior(getActivity(), false),
                    Settings.System.KEY_BACK_ACTION);

            // Back key longpress
            setupOrUpdatePreference(mBackLongPressAction,
                    HwKeyHelper.getLongPressOnBackBehavior(getActivity(), false),
                    Settings.System.KEY_BACK_LONG_PRESS_ACTION);

            // Back key double tap
            setupOrUpdatePreference(mBackDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnBackBehavior(getActivity(), false),
                    Settings.System.KEY_BACK_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysBackCategory);
        }

        if (hasHomeKey) {
            // Home key
            setupOrUpdatePreference(mHomePressAction,
                    HwKeyHelper.getPressOnHomeBehavior(getActivity(), false),
                    Settings.System.KEY_HOME_ACTION);

            // Home key long press
            setupOrUpdatePreference(mHomeLongPressAction,
                    HwKeyHelper.getLongPressOnHomeBehavior(getActivity(), false),
                    Settings.System.KEY_HOME_LONG_PRESS_ACTION);

            // Home key double tap
            setupOrUpdatePreference(mHomeDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnHomeBehavior(getActivity(), false),
                    Settings.System.KEY_HOME_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysHomeCategory);
        }

        if (hasMenuKey) {
            // Menu key
            setupOrUpdatePreference(mMenuPressAction,
                    HwKeyHelper.getPressOnMenuBehavior(getActivity(), false),
                    Settings.System.KEY_MENU_ACTION);

            // Menu key longpress
            setupOrUpdatePreference(mMenuLongPressAction,
                    HwKeyHelper.getLongPressOnMenuBehavior(getActivity(), false, hasAssistKey),
                    Settings.System.KEY_MENU_LONG_PRESS_ACTION);

            // Menu key double tap
            setupOrUpdatePreference(mMenuDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnMenuBehavior(getActivity(), false),
                    Settings.System.KEY_MENU_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysMenuCategory);
        }

        if (hasAssistKey) {
            // Assistant key
            setupOrUpdatePreference(mAssistPressAction,
                    HwKeyHelper.getPressOnAssistBehavior(getActivity(), false),
                    Settings.System.KEY_ASSIST_ACTION);

            // Assistant key longpress
            setupOrUpdatePreference(mAssistLongPressAction,
                    HwKeyHelper.getLongPressOnAssistBehavior(getActivity(), false),
                    Settings.System.KEY_ASSIST_LONG_PRESS_ACTION);

            // Assistant key double tap
            setupOrUpdatePreference(mAssistDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnAssistBehavior(getActivity(), false),
                    Settings.System.KEY_ASSIST_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysAssistCategory);
        }

        if (hasAppSwitchKey) {
            // App switch key
            setupOrUpdatePreference(mAppSwitchPressAction,
                    HwKeyHelper.getPressOnAppSwitchBehavior(getActivity(), false),
                    Settings.System.KEY_APP_SWITCH_ACTION);

            // App switch key longpress
            setupOrUpdatePreference(mAppSwitchLongPressAction,
                    HwKeyHelper.getLongPressOnAppSwitchBehavior(getActivity(), false),
                    Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION);

            // App switch key double tap
            setupOrUpdatePreference(mAppSwitchDoubleTapAction,
                    HwKeyHelper.getDoubleTapOnAppSwitchBehavior(getActivity(), false),
                    Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION);
        } else {
            prefs.removePreference(keysAppSwitchCategory);
        }

        boolean enableHardwareRebind = Settings.System.getInt(getContentResolver(),
                Settings.System.HARDWARE_KEY_REBINDING, 0) == 1;
        mEnableCustomBindings = (CheckBoxPreference) findPreference(KEYS_ENABLE_CUSTOM);
        mEnableCustomBindings.setChecked(enableHardwareRebind);
        mEnableCustomBindings.setOnPreferenceChangeListener(this);

        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) findPreference(KEY_BUTTON_BACKLIGHT);
        if (!backlight.isButtonSupported() && !backlight.isKeyboardSupported()) {
            prefs.removePreference(backlight);
        }

        // Handle warning dialog.
        SharedPreferences preferences =
                getActivity().getSharedPreferences("hw_key_settings", Activity.MODE_PRIVATE);
        if (!hasHomeKey() && !preferences.getBoolean("no_home_action", false)) {
            preferences.edit()
                    .putBoolean("no_home_action", true).commit();
            showDialogInner(DLG_SHOW_WARNING_DIALOG, null, 0);
        } else if (hasHomeKey()) {
            preferences.edit()
                    .putBoolean("no_home_action", false).commit();
        }

        mLongHomeAction = (ListPreference) findPreference(LOCKSCREENLONG_PRESS_HOME);
        if (hasHomeKey) {
            mLongHomeAction.setKey(Settings.System.LOCKSCREEN_LONG_HOME_ACTION);
        } else {
            getPreferenceScreen().removePreference(mLongHomeAction);
        }

        mActions = new ListPreference[] {
            mLongHomeAction
        };
        for (ListPreference pref : mActions) {
            if (QSUtils.deviceSupportsTorch(getActivity())) {
                final CharSequence[] oldEntries = pref.getEntries();
                final CharSequence[] oldValues = pref.getEntryValues();
                ArrayList<CharSequence> newEntries = new ArrayList<CharSequence>();
                ArrayList<CharSequence> newValues = new ArrayList<CharSequence>();
                for (int i = 0; i < oldEntries.length; i++) {
                    newEntries.add(oldEntries[i].toString());
                    newValues.add(oldValues[i].toString());
                }
                newEntries.add(getString(R.string.lockscreen_buttons_flashlight));
                newValues.add("FLASHLIGHT");
                pref.setEntries(
                        newEntries.toArray(new CharSequence[newEntries.size()]));
                pref.setEntryValues(
                        newValues.toArray(new CharSequence[newValues.size()]));
            }
            pref.setOnPreferenceChangeListener(this);
        }

        mCheckPreferences = true;
        return prefs;
    }

    private void setupOrUpdatePreference(
            Preference preference, String action, String settingsKey) {
        if (preference == null || action == null) {
            return;
        }

        if (action.startsWith("**")) {
            preference.setSummary(getDescription(action));
        } else {
            preference.setSummary(AppHelper.getFriendlyNameForUri(
                    getActivity(), getActivity().getPackageManager(), action));
        }
        preference.setOnPreferenceClickListener(this);
        mKeySettings.put(settingsKey, action);
    }

    private String getDescription(String action) {
        if (sFinalActionDialogArray == null || action == null) {
            return null;
        }
        int i = 0;
        for (String actionValue : sFinalActionDialogArray.values) {
            if (action.equals(actionValue)) {
                return sFinalActionDialogArray.entries[i];
            }
            i++;
        }
        return null;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String settingsKey = null;
        int dialogTitle = 0;
        if (preference == mBackPressAction) {
            settingsKey = Settings.System.KEY_BACK_ACTION;
            dialogTitle = R.string.keys_back_press_title;
        } else if (preference == mBackLongPressAction) {
            settingsKey = Settings.System.KEY_BACK_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_back_long_press_title;
        } else if (preference == mBackDoubleTapAction) {
            settingsKey = Settings.System.KEY_BACK_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_back_double_tap_title;
        } else if (preference == mHomePressAction) {
            settingsKey = Settings.System.KEY_HOME_ACTION;
            dialogTitle = R.string.keys_home_press_title;
        } else if (preference == mHomeLongPressAction) {
            settingsKey = Settings.System.KEY_HOME_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_home_long_press_title;
        } else if (preference == mHomeDoubleTapAction) {
            settingsKey = Settings.System.KEY_HOME_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_home_double_tap_title;
        } else if (preference == mMenuPressAction) {
            settingsKey = Settings.System.KEY_MENU_ACTION;
            dialogTitle = R.string.keys_menu_press_title;
        } else if (preference == mMenuLongPressAction) {
            settingsKey = Settings.System.KEY_MENU_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_menu_long_press_title;
        } else if (preference == mMenuDoubleTapAction) {
            settingsKey = Settings.System.KEY_MENU_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_menu_double_tap_title;
        } else if (preference == mAssistPressAction) {
            settingsKey = Settings.System.KEY_ASSIST_ACTION;
            dialogTitle = R.string.keys_assist_press_title;
        } else if (preference == mAssistLongPressAction) {
            settingsKey = Settings.System.KEY_ASSIST_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_assist_long_press_title;
        } else if (preference == mAssistDoubleTapAction) {
            settingsKey = Settings.System.KEY_ASSIST_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_assist_double_tap_title;
        } else if (preference == mAppSwitchPressAction) {
            settingsKey = Settings.System.KEY_APP_SWITCH_ACTION;
            dialogTitle = R.string.keys_app_switch_press_title;
        } else if (preference == mAppSwitchLongPressAction) {
            settingsKey = Settings.System.KEY_APP_SWITCH_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_app_switch_long_press_title;
        } else if (preference == mAppSwitchDoubleTapAction) {
            settingsKey = Settings.System.KEY_APP_SWITCH_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_app_switch_double_tap_title;
        }

        if (settingsKey != null) {
            showDialogInner(DLG_SHOW_ACTION_DIALOG, settingsKey, dialogTitle);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mEnableCustomBindings) {
            boolean setCecked = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(), Settings.System.HARDWARE_KEY_REBINDING,
                    setCecked ? 1 : 0);
            return true;
        }

        ListPreference list = (ListPreference) preference;
        String value = (String) newValue;

        if (Settings.System.putString(getContentResolver(), list.getKey(), value)) {
            preference.setSummary(findEntryForValue(list, value));
        }
        return true;
    }

    private static void writeDisableHwkeysOption(Context context, boolean enabled) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int defaultBrightness = context.getResources().getInteger(
                com.android.internal.R.integer.config_buttonBrightnessSettingDefault);

        Settings.System.putInt(context.getContentResolver(),
                Settings.System.DISABLE_HARDWARE_KEYS, enabled ? 1 : 0);
        KeyDisabler.setActive(enabled);

        /* Save/restore button timeouts to disable them in softkey mode */
        Editor editor = prefs.edit();

        if (enabled) {
            int currentBrightness = Settings.System.getInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, defaultBrightness);
            if (!prefs.contains("pre_navbar_button_backlight")) {
                editor.putInt("pre_navbar_button_backlight", currentBrightness);
            }
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS, 0);
        } else {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.BUTTON_BRIGHTNESS,
                    prefs.getInt("pre_navbar_button_backlight", defaultBrightness));
            editor.remove("pre_navbar_button_backlight");
        }
        editor.commit();
    }

    private void updateDisableHwkeysOption() {
        boolean enabled = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.DISABLE_HARDWARE_KEYS, 0) != 0;

        mDisableHwKeys.setChecked(enabled);

        final PreferenceScreen prefScreen = getPreferenceScreen();

        /* Disable hw-key options if they're disabled */
        final PreferenceCategory backCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_BACK);
        final PreferenceCategory homeCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_HOME);
        final PreferenceCategory menuCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_MENU);
        final PreferenceCategory assistCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_ASSIST);
        final PreferenceCategory appSwitchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_APPSWITCH);
        final ButtonBacklightBrightness backlight =
                (ButtonBacklightBrightness) prefScreen.findPreference(KEY_BUTTON_BACKLIGHT);

        if (mEnableCustomBindings != null) {
            mEnableCustomBindings.setEnabled(!enabled);
        }

        /* Toggle backlight control depending on navbar state, force it to
           off if enabling */
        if (backlight != null) {
            backlight.setEnabled(!enabled);
            backlight.updateSummary();
        }

        /* Toggle hardkey control availability depending on navbar state */
        if (backCategory != null) {
            backCategory.setEnabled(!enabled);
        }
        if (homeCategory != null) {
            homeCategory.setEnabled(!enabled);
        }
        if (menuCategory != null) {
            menuCategory.setEnabled(!enabled);
        }
        if (assistCategory != null) {
            assistCategory.setEnabled(!enabled);
        }
        if (appSwitchCategory != null) {
            appSwitchCategory.setEnabled(!enabled);
        }
    }

    public static void restoreKeyDisabler(Context context) {
        if (!KeyDisabler.isSupported()) {
            return;
        }

        writeDisableHwkeysOption(context, Settings.System.getInt(context.getContentResolver(),
                Settings.System.DISABLE_HARDWARE_KEYS, 0) != 0);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDisableHwKeys) {
            mDisableHwKeys.setEnabled(false);
            writeDisableHwkeysOption(getActivity(), mDisableHwKeys.isChecked());
            updateDisableHwkeysOption();
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDisableHwKeys.setEnabled(true);
                }
            }, 1000);
        } else if (preference == mHomeAnswerCall) {
            handleToggleHomeButtonAnswersCallPreferenceClick();
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    private boolean hasHomeKey() {
        Iterator<String> nextAction = mKeySettings.values().iterator();
        while (nextAction.hasNext()){
            String action = nextAction.next();
            if (action != null && action.equals(ButtonsConstants.ACTION_HOME)) {
                return true;
            }
        }
        return false;
    }

    private void resetToDefault() {
        for (String settingsKey : mKeySettings.keySet()) {
            if (settingsKey != null) {
                Settings.System.putString(getActivity().getContentResolver(),
                settingsKey, null);
            }
        }
        Settings.System.putInt(getContentResolver(),
                Settings.System.HARDWARE_KEY_REBINDING, 1);
        reloadSettings();
    }

    @Override
    public void onResume() {
        updateDisableHwkeysOption();
        super.onResume();

        // Home button answers calls.
        if (mHomeAnswerCall != null) {
            final int incallHomeBehavior = Settings.Secure.getInt(getContentResolver(),
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR,
                    Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DEFAULT);
            final boolean homeButtonAnswersCall =
                (incallHomeBehavior == Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER);
            mHomeAnswerCall.setChecked(homeButtonAnswersCall);
        }

        for (ListPreference pref : mActions) {
            updateEntry(pref);
        }
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap bmp, boolean isApplication) {
        if (mPendingSettingsKey == null || action == null) {
            return;
        }
        Settings.System.putString(getContentResolver(), mPendingSettingsKey, action);
        reloadSettings();
        mPendingSettingsKey = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            }
        } else {
            mPendingSettingsKey = null;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                    showDialogInner(DLG_RESET_TO_DEFAULT, null, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.shortcut_action_reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void showDialogInner(int id, String settingsKey, int dialogTitle) {
        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id, settingsKey, dialogTitle);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(
                int id, String settingsKey, int dialogTitle) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("settingsKey", settingsKey);
            args.putInt("dialogTitle", dialogTitle);
            frag.setArguments(args);
            return frag;
        }

        HardwareKeys getOwner() {
            return (HardwareKeys) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final String settingsKey = getArguments().getString("settingsKey");
            int dialogTitle = getArguments().getInt("dialogTitle");
            switch (id) {
                case DLG_SHOW_WARNING_DIALOG:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.attention)
                    .setMessage(R.string.no_home_key)
                    .setPositiveButton(R.string.dlg_ok, null)
                    .create();
                case DLG_SHOW_ACTION_DIALOG:
                    if (sFinalActionDialogArray == null) {
                        return null;
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(dialogTitle)
                    .setNegativeButton(R.string.cancel, null)
                    .setItems(getOwner().sFinalActionDialogArray.entries,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if (getOwner().sFinalActionDialogArray.values[item]
                                    .equals(ButtonsConstants.ACTION_APP)) {
                                if (getOwner().mPicker != null) {
                                    getOwner().mPendingSettingsKey = settingsKey;
                                    getOwner().mPicker.pickShortcut(getOwner().getId());
                                }
                            } else {
                                Settings.System.putString(getActivity().getContentResolver(),
                                        settingsKey,
                                        getOwner().sFinalActionDialogArray.values[item]);
                                getOwner().reloadSettings();
                            }
                        }
                    })
                    .create();
                case DLG_RESET_TO_DEFAULT:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.shortcut_action_reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().resetToDefault();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
        }
    }

    private void updateEntry(ListPreference pref) {
        String value = Settings.System.getString(getContentResolver(), pref.getKey());
        if (value == null) {
            value = "";
        }

        CharSequence entry = findEntryForValue(pref, value);
        if (entry != null) {
            pref.setValue(value);
            pref.setSummary(entry);
            return;
        }
    }

    private CharSequence findEntryForValue(ListPreference pref, CharSequence value) {
        CharSequence[] entries = pref.getEntryValues();
        for (int i = 0; i < entries.length; i++) {
            if (TextUtils.equals(entries[i], value)) {
                return pref.getEntries()[i];
            }
        }
        return null;
    }

    private void handleToggleHomeButtonAnswersCallPreferenceClick() {
        Settings.Secure.putInt(getContentResolver(),
                Settings.Secure.RING_HOME_BUTTON_BEHAVIOR, (mHomeAnswerCall.isChecked()
                        ? Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_ANSWER
                        : Settings.Secure.RING_HOME_BUTTON_BEHAVIOR_DO_NOTHING));
    }
}
