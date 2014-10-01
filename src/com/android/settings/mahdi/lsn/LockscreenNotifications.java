/*
* Based on: http://www.lukehorvat.com/blog/android-numberpickerdialogpreference/
* Thanks to the original author!
*/

package com.android.settings.mahdi.lsn;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.Uri;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.Preference;
import android.preference.SeekBarPreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.view.WindowManager;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.Utils;

import com.android.settings.mahdi.lsn.AppMultiSelectListPreference;
import com.android.settings.mahdi.lsn.NumberPickerPreference;
import com.android.settings.mahdi.SystemSettingCheckBoxPreference;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

public class LockscreenNotifications extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    private static final String KEY_CATEGORY_WAKE_UP = "category_wake_up";
    private static final String KEY_INCLUDED_APPS = "lockscreen_notifications_included_apps";
    private static final String KEY_EXCLUDED_APPS = "lockscreen_notifications_excluded_apps";
    private static final String KEY_WAKE_ON_NOTIFICATION = "lockscreen_notifications_wake_on_notification";
    private static final String KEY_POCKET_MODE = "lockscreen_notifications_pocket_mode";
    private static final String KEY_SHOW_ALWAYS = "lockscreen_notifications_show_always";
    private static final String KEY_PRIVACY_MODE = "lockscreen_notifications_privacy_mode";
    private static final String KEY_EXPANDED_VIEW = "lockscreen_notifications_expanded_view";
    private static final String KEY_NOTIFICATIONS_HEIGHT = "lockscreen_notifications_notifications_height";
    private static final String KEY_OFFSET_TOP = "lockscreen_notifications_offset_top";
    private static final String KEY_NOTIFICATION_COLOR = "lockscreen_notifications_notification_color";

    private AppMultiSelectListPreference mIncludedAppsPref;
    private AppMultiSelectListPreference mExcludedAppsPref;
    private SystemSettingCheckBoxPreference mWakeOnNotification;
    private SystemSettingCheckBoxPreference mPocketMode;
    private SystemSettingCheckBoxPreference mShowAlways;
    private CheckBoxPreference mPrivacyMode;
    private CheckBoxPreference mExpandedView;
    private NumberPickerPreference mNotificationsHeight;
    private SeekBarPreference mOffsetTop;
    private ColorPickerPreference mNotificationColor;

    private Switch mActionBarSwitch;
    private LockscreenNotificationsEnabler mLsnEnabler;

    private ViewGroup mPrefsContainer;
    private View mDisabledText;

    private ContentObserver mSettingsObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateEnabledState();
        }
    };

    @Override
      public void onActivityCreated(Bundle icicle) {
        // We don't call super.onActivityCreated() here, since it assumes we already set up
        // Preference (probably in onCreate()), while ProfilesSettings exceptionally set it up in
        // this method.
        // On/off switch
        Activity activity = getActivity();
        //Switch
        mActionBarSwitch = new Switch(activity);

        if (activity instanceof PreferenceActivity) {
            final int padding = activity.getResources().getDimensionPixelSize(
                    R.dimen.action_bar_switch_padding);
            mActionBarSwitch.setPaddingRelative(0, 0, padding, 0);
            activity.getActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                    ActionBar.DISPLAY_SHOW_CUSTOM);
            activity.getActionBar().setCustomView(mActionBarSwitch, new ActionBar.LayoutParams(
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    ActionBar.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER_VERTICAL | Gravity.END));
        }

        mLsnEnabler = new LockscreenNotificationsEnabler(activity, mActionBarSwitch);
        // After confirming PreferenceScreen is available, we call super.
          super.onActivityCreated(icicle);
          setHasOptionsMenu(true);
      }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.lockscreen_notifications);
        PreferenceScreen prefs = getPreferenceScreen();
        final ContentResolver cr = getActivity().getContentResolver();

        mIncludedAppsPref = (AppMultiSelectListPreference) prefs.findPreference(KEY_INCLUDED_APPS);
        Set<String> includedApps = getIncludedApps();
        if (includedApps != null) {
            mIncludedAppsPref.setValues(includedApps);
        }
        mIncludedAppsPref.setOnPreferenceChangeListener(this);

        mExcludedAppsPref = (AppMultiSelectListPreference) prefs.findPreference(KEY_EXCLUDED_APPS);
        Set<String> excludedApps = getExcludedApps();
        if (excludedApps != null) {
            mExcludedAppsPref.setValues(excludedApps);
        }
        mExcludedAppsPref.setOnPreferenceChangeListener(this);

        mPocketMode = (SystemSettingCheckBoxPreference) prefs.findPreference(KEY_POCKET_MODE);
        mShowAlways = (SystemSettingCheckBoxPreference) prefs.findPreference(KEY_SHOW_ALWAYS);
        mWakeOnNotification = (SystemSettingCheckBoxPreference) prefs.findPreference(KEY_WAKE_ON_NOTIFICATION);
        PreferenceCategory category_wake_up = (PreferenceCategory) findPreference(KEY_CATEGORY_WAKE_UP);
        boolean hasProximitySensor = getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY);
        if (!hasProximitySensor) {
            prefs.removePreference(category_wake_up);
        }

        mPrivacyMode = (CheckBoxPreference) prefs.findPreference(KEY_PRIVACY_MODE);
        mPrivacyMode.setChecked(Settings.System.getInt(cr,
                Settings.System.LOCKSCREEN_NOTIFICATIONS_PRIVACY_MODE, 0) == 1);
        mPrivacyMode.setOnPreferenceChangeListener(this);

        mExpandedView = (SystemSettingCheckBoxPreference) prefs.findPreference(KEY_EXPANDED_VIEW);
        mExpandedView.setEnabled(!mPrivacyMode.isChecked());

        mOffsetTop = (SeekBarPreference) prefs.findPreference(KEY_OFFSET_TOP);
        mOffsetTop.setProgress((int)(Settings.System.getFloat(cr,
                Settings.System.LOCKSCREEN_NOTIFICATIONS_OFFSET_TOP, 0.3f) * 100));
        mOffsetTop.setTitle(getResources().getText(R.string.offset_top) + " " + mOffsetTop.getProgress() + "%");
        mOffsetTop.setOnPreferenceChangeListener(this);

        mNotificationsHeight = (NumberPickerPreference) prefs.findPreference(KEY_NOTIFICATIONS_HEIGHT);
        mNotificationsHeight.setValue(Settings.System.getInt(cr,
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HEIGHT, 4));
        Point displaySize = new Point();
        ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(displaySize);
        int max = Math.round((float)displaySize.y * (1f - (mOffsetTop.getProgress() / 100f)) /
                (float)mContext.getResources().getDimensionPixelSize(R.dimen.notification_row_min_height));
        mNotificationsHeight.setMinValue(1);
        mNotificationsHeight.setMaxValue(max);
        mNotificationsHeight.setOnPreferenceChangeListener(this);

        mNotificationColor = (ColorPickerPreference) prefs.findPreference(KEY_NOTIFICATION_COLOR);
        mNotificationColor.setAlphaSliderEnabled(true);
        int color = Settings.System.getInt(cr,
                Settings.System.LOCKSCREEN_NOTIFICATIONS_COLOR, 0x55555555);
        String hexColor = String.format("#%08x", (0xffffffff & color));
        mNotificationColor.setSummary(hexColor);
        mNotificationColor.setDefaultValue(color);
        mNotificationColor.setNewPreviewColor(color);
        mNotificationColor.setOnPreferenceChangeListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.lockscreen_notifications_fragment, container, false);
        mPrefsContainer = (ViewGroup) v.findViewById(R.id.prefs_container);
        mDisabledText = v.findViewById(R.id.disabled_text);

        View prefs = super.onCreateView(inflater, mPrefsContainer, savedInstanceState);
        mPrefsContainer.addView(prefs);

        return v;
    }

    @Override
    public void onDestroyView() {
        getActivity().getActionBar().setCustomView(null);
        super.onDestroyView();
    }

    @Override
      public void onResume() {
        super.onResume();
        if (mLsnEnabler != null) {
            mLsnEnabler.resume();
        }
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.LOCKSCREEN_NOTIFICATIONS),
                true, mSettingsObserver);
        updateEnabledState();

        // If running on a phone, remove padding around container
        // and the preference listview
        if (!Utils.isTablet(getActivity())) {
            mPrefsContainer.setPadding(0, 0, 0, 0);
            getListView().setPadding(0, 0, 0, 0);
        }
    }

    public void onPause() {
        super.onPause();
        if (mLsnEnabler != null) {
            mLsnEnabler.pause();
        }
        getContentResolver().unregisterContentObserver(mSettingsObserver);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object value) {
        if (pref == mIncludedAppsPref) {
            storeIncludedApps((HashSet<String>) value);
        } else if (pref == mExcludedAppsPref) {
            storeExcludedApps((HashSet<String>) value);
        } else if (pref == mPrivacyMode) {
            boolean privacyMode = (Boolean) value;
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_PRIVACY_MODE, privacyMode ? 1 : 0);
            mExpandedView.setEnabled(!privacyMode);
        } else if (pref == mOffsetTop) {
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_OFFSET_TOP, (Integer)value / 100f);
            mOffsetTop.setTitle(getResources().getText(R.string.offset_top) + " " + (Integer)value + "%");
            Point displaySize = new Point();
            ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(displaySize);
            int max = Math.round((float)displaySize.y * (1f - (mOffsetTop.getProgress() / 100f)) /
                    (float)mContext.getResources().getDimensionPixelSize(R.dimen.notification_row_min_height));
            mNotificationsHeight.setMaxValue(max);
        } else if (pref == mNotificationsHeight) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_HEIGHT, (Integer)value);
        } else if (pref == mNotificationColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(value)));
            pref.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.LOCKSCREEN_NOTIFICATIONS_COLOR, intHex);
        }
        return true;
    }

    private void updateEnabledState() {
        boolean enabled = Settings.System.getInt(getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS, 0) != 0;
        mPrefsContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
        mDisabledText.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }

    private HashSet<String> getIncludedApps() {
        String included = Settings.System.getString(getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS_INCLUDED_APPS);
        if (TextUtils.isEmpty(included)) {
            return null;
        }

        return new HashSet<String>(Arrays.asList(included.split("\\|")));
    }

    private void storeIncludedApps(HashSet<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS_INCLUDED_APPS, builder.toString());
    }

    private HashSet<String> getExcludedApps() {
        String excluded = Settings.System.getString(getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS_EXCLUDED_APPS);
        if (TextUtils.isEmpty(excluded)) {
            return null;
        }

        return new HashSet<String>(Arrays.asList(excluded.split("\\|")));
    }

    private void storeExcludedApps(HashSet<String> values) {
        StringBuilder builder = new StringBuilder();
        String delimiter = "";
        for (String value : values) {
            builder.append(delimiter);
            builder.append(value);
            delimiter = "|";
        }
        Settings.System.putString(getContentResolver(),
                Settings.System.LOCKSCREEN_NOTIFICATIONS_EXCLUDED_APPS, builder.toString());
    }
}
