/*
 * Copyright (C) 2014 The OmniROM Project
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
import android.content.res.Resources;
import android.database.ContentObserver;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.mahdi.SystemSettingCheckBoxPreference;
import com.android.settings.mahdi.chameleonos.SeekBarPreference;

public class TintedSystemBarSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener {

    private static final String TAG = "TintedSystemBarSettings";
    private static final String KEY_TINTED_SYSTEMBAR_SETTINGS = "tinted_systembar_settings";

    private static final String TINTED_STATUSBAR = "tinted_statusbar";
    private static final String TINTED_STATUSBAR_OPTION = "tinted_statusbar_option";
    private static final String TINTED_STATUSBAR_FILTER = "status_bar_tinted_filter";
    private static final String TINTED_STATUSBAR_TRANSPARENT = "tinted_statusbar_transparent";
    private static final String TINTED_NAVBAR_TRANSPARENT = "tinted_navbar_transparent";

    private ListPreference mTintedStatusbar;
    private ListPreference mTintedStatusbarOption;
    private SystemSettingCheckBoxPreference mTintedStatusbarFilter;
    private SeekBarPreference mTintedStatusbarTransparency;
    private SeekBarPreference mTintedNavbarTransparency;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.tinted_systembar_settings);

        PreferenceScreen prefSet = getPreferenceScreen();
        ContentResolver resolver = getActivity().getContentResolver();

        mTintedStatusbar = (ListPreference) findPreference(TINTED_STATUSBAR);
        int tintedStatusbar = Settings.System.getInt(resolver,
                    Settings.System.STATUS_BAR_TINTED_COLOR, 0);
        mTintedStatusbar.setValue(String.valueOf(tintedStatusbar));
        mTintedStatusbar.setSummary(mTintedStatusbar.getEntry());
        mTintedStatusbar.setOnPreferenceChangeListener(this);

        mTintedStatusbarOption = (ListPreference) findPreference(TINTED_STATUSBAR_OPTION);
        int tintedStatusbarOption = Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_TINTED_OPTION, 0);
        mTintedStatusbarOption.setValue(String.valueOf(tintedStatusbarOption));
        mTintedStatusbarOption.setSummary(mTintedStatusbarOption.getEntry());
        mTintedStatusbarOption.setEnabled(tintedStatusbar != 0);
        mTintedStatusbarOption.setOnPreferenceChangeListener(this);

        mTintedStatusbarFilter = (SystemSettingCheckBoxPreference) findPreference(TINTED_STATUSBAR_FILTER);
        mTintedStatusbarFilter.setEnabled(tintedStatusbar != 0);

        mTintedStatusbarTransparency = (SeekBarPreference) findPreference(TINTED_STATUSBAR_TRANSPARENT);
        mTintedStatusbarTransparency.setValue(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_TINTED_STATBAR_TRANSPARENT, 100));
        mTintedStatusbarTransparency.setEnabled(tintedStatusbar != 0);
        mTintedStatusbarTransparency.setOnPreferenceChangeListener(this);

        mTintedNavbarTransparency = (SeekBarPreference) findPreference(TINTED_NAVBAR_TRANSPARENT);
        mTintedNavbarTransparency.setValue(Settings.System.getInt(resolver,
                Settings.System.STATUS_BAR_TINTED_NAVBAR_TRANSPARENT, 100));
        mTintedNavbarTransparency.setEnabled(tintedStatusbar != 0);
        mTintedNavbarTransparency.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
        ContentResolver resolver = getActivity().getContentResolver();
        if (preference == mTintedStatusbar) {
            int val = Integer.parseInt((String) objValue);
            int index = mTintedStatusbar.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                Settings.System.STATUS_BAR_TINTED_COLOR, val);
            mTintedStatusbar.setSummary(mTintedStatusbar.getEntries()[index]);
            if (mTintedStatusbarOption != null) {
                mTintedStatusbarOption.setEnabled(val != 0);
            }
            mTintedStatusbarFilter.setEnabled(val != 0);
            mTintedStatusbarTransparency.setEnabled(val != 0);
            if (mTintedNavbarTransparency != null) {
                mTintedNavbarTransparency.setEnabled(val != 0);
            }
        } else if (preference == mTintedStatusbarOption) {
            int val = Integer.parseInt((String) objValue);
            int index = mTintedStatusbarOption.findIndexOfValue((String) objValue);
            Settings.System.putInt(resolver,
                Settings.System.STATUS_BAR_TINTED_OPTION, val);
            mTintedStatusbarOption.setSummary(mTintedStatusbarOption.getEntries()[index]);
        } else if (preference == mTintedStatusbarTransparency) {
            int val = ((Integer)objValue).intValue();
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_TINTED_STATBAR_TRANSPARENT, val);
            return true;
        } else if (preference == mTintedNavbarTransparency) {
            int val = ((Integer)objValue).intValue();
            Settings.System.putInt(resolver,
                    Settings.System.STATUS_BAR_TINTED_NAVBAR_TRANSPARENT, val);
            return true;
        } else {
            return false;
        }
        return true;
    }

}
