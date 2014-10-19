/*
 * Copyright (C) 2012 Slimroms Project
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceScreen;
import android.preference.SlimSeekBarPreference;
import android.provider.Settings;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.widget.Toast;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.util.mahdi.DeviceUtils;
import com.android.internal.util.mahdi.QSUtils;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.mahdi.SystemSettingCheckBoxPreference;
import com.android.settings.mahdi.chameleonos.SeekBarPreference;

import java.io.File;

import net.margaritov.preference.colorpicker.ColorPickerPreference;
import net.margaritov.preference.colorpicker.ColorPickerView;


public class NotificationDrawerStyle extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener {

    private static final String TAG = "NotificationDrawerStyle";

    private static final String TINTED_STATUSBAR = "tinted_statusbar";
    private static final String TINTED_STATUSBAR_OPTION = "tinted_statusbar_option";
    private static final String TINTED_STATUSBAR_FILTER = "status_bar_tinted_filter";
    private static final String TINTED_STATUSBAR_TRANSPARENT = "tinted_statusbar_transparent";
    private static final String TINTED_NAVBAR_TRANSPARENT = "tinted_navbar_transparent";
    private static final String QUICK_TILES_BG_COLOR = "quick_tiles_bg_color";
    private static final String QUICK_TILES_BG_PRESSED_COLOR = "quick_tiles_bg_pressed_color";
    private static final String QUICK_TILES_ALPHA = "quick_tiles_alpha";
    private static final String QUICK_TILES_TEXT_COLOR = "quick_tiles_text_color";
    private static final String ADDITIONAL_OPTIONS = "quicksettings_tiles_style_additional_options";
    private static final String NOTIFICATION_WALLPAPER = "notification_wallpaper";
    private static final String NOTIFICATION_WALLPAPER_LANDSCAPE = "notification_wallpaper_landscape";
    private static final String NOTIFICATION_WALLPAPER_ALPHA = "notification_wallpaper_alpha";
    private static final String NOTIFICATION_ALPHA = "notification_alpha";

    private static final int DEFAULT_QUICK_TILES_TEXT_COLOR = 0xffcccccc;

    private static final int MENU_RESET = Menu.FIRST;

    private static final int DLG_RESET = 0;
    private static final int DLG_PICK_COLOR = 1;

    private ListPreference mTintedStatusbar;
    private ListPreference mTintedStatusbarOption;
    private SystemSettingCheckBoxPreference mTintedStatusbarFilter;
    private SeekBarPreference mTintedStatusbarTransparency;
    private SeekBarPreference mTintedNavbarTransparency;
    private ColorPickerPreference mQuickTilesBgColor;
    private ColorPickerPreference mQuickTilesBgPressedColor;
    private ColorPickerPreference mQuickTilesTextColor;
    private SlimSeekBarPreference mQsTileAlpha;
    private ListPreference mNotificationWallpaper;
    private ListPreference mNotificationWallpaperLandscape;
    SlimSeekBarPreference mWallpaperAlpha;
    SlimSeekBarPreference mNotificationAlpha;

    private File mImageTmp;

    private static final int REQUEST_PICK_WALLPAPER = 201;
    private static final int REQUEST_PICK_WALLPAPER_LANDSCAPE = 202;

    private Activity mActivity;

    private boolean mCheckPreferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        refreshSettings();
    }

    private PreferenceScreen refreshSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        PackageManager pm = getPackageManager();
        Resources systemUiResources;
        try {
            systemUiResources = pm.getResourcesForApplication("com.android.systemui");
        } catch (Exception e) {
            Log.e(TAG, "can't access systemui resources",e);
            return null;
        }

        mActivity = getActivity();

        addPreferencesFromResource(R.xml.notification_bg_pref);

        PreferenceScreen prefSet = getPreferenceScreen();

        mImageTmp = new File(getActivity().getFilesDir() + "/notifi_bg.tmp");

        int intColor;
        String hexColor;
        float transparency;

        mTintedStatusbar = (ListPreference) findPreference(TINTED_STATUSBAR);
        int tintedStatusbar = Settings.System.getInt(getContentResolver(),
                    Settings.System.STATUS_BAR_TINTED_COLOR, 0);
        mTintedStatusbar.setValue(String.valueOf(tintedStatusbar));
        mTintedStatusbar.setSummary(mTintedStatusbar.getEntry());
        mTintedStatusbar.setOnPreferenceChangeListener(this);

        mTintedStatusbarOption = (ListPreference) findPreference(TINTED_STATUSBAR_OPTION);
        int tintedStatusbarOption = Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_TINTED_OPTION, 0);
        mTintedStatusbarOption.setValue(String.valueOf(tintedStatusbarOption));
        mTintedStatusbarOption.setSummary(mTintedStatusbarOption.getEntry());
        mTintedStatusbarOption.setEnabled(tintedStatusbar != 0);
        mTintedStatusbarOption.setOnPreferenceChangeListener(this);

        mTintedStatusbarFilter = (SystemSettingCheckBoxPreference) findPreference(TINTED_STATUSBAR_FILTER);
        mTintedStatusbarFilter.setEnabled(tintedStatusbar != 0);

        mTintedStatusbarTransparency = (SeekBarPreference) findPreference(TINTED_STATUSBAR_TRANSPARENT);
        mTintedStatusbarTransparency.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_TINTED_STATBAR_TRANSPARENT, 100));
        mTintedStatusbarTransparency.setEnabled(tintedStatusbar != 0);
        mTintedStatusbarTransparency.setOnPreferenceChangeListener(this);

        mTintedNavbarTransparency = (SeekBarPreference) findPreference(TINTED_NAVBAR_TRANSPARENT);
        mTintedNavbarTransparency.setValue(Settings.System.getInt(getContentResolver(),
                Settings.System.STATUS_BAR_TINTED_NAVBAR_TRANSPARENT, 100));
        mTintedNavbarTransparency.setEnabled(tintedStatusbar != 0);
        mTintedNavbarTransparency.setOnPreferenceChangeListener(this);

        mQuickTilesBgColor = (ColorPickerPreference) findPreference(QUICK_TILES_BG_COLOR);
        mQuickTilesBgColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_BG_COLOR, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/qs_background_color", null, null));
            mQuickTilesBgColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mQuickTilesBgColor.setSummary(hexColor);
        }
        mQuickTilesBgColor.setNewPreviewColor(intColor);

        mQuickTilesBgPressedColor =
                (ColorPickerPreference) findPreference(QUICK_TILES_BG_PRESSED_COLOR);
        mQuickTilesBgPressedColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_BG_PRESSED_COLOR, -2);
        if (intColor == -2) {
            intColor = systemUiResources.getColor(systemUiResources.getIdentifier(
                    "com.android.systemui:color/qs_background_pressed_color", null, null));
            mQuickTilesBgPressedColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mQuickTilesBgPressedColor.setSummary(hexColor);
        }
        mQuickTilesBgPressedColor.setNewPreviewColor(intColor);

        mQuickTilesTextColor = (ColorPickerPreference) findPreference(QUICK_TILES_TEXT_COLOR);
        mQuickTilesTextColor.setOnPreferenceChangeListener(this);
        intColor = Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.QUICK_TILES_TEXT_COLOR, -2);
        if (intColor == -2) {
            intColor = DEFAULT_QUICK_TILES_TEXT_COLOR;
            mQuickTilesTextColor.setSummary(getResources().getString(R.string.default_string));
        } else {
            hexColor = String.format("#%08x", (0xffffffff & intColor));
            mQuickTilesTextColor.setSummary(hexColor);
        }
        mQuickTilesTextColor.setNewPreviewColor(intColor);

        try{
            transparency = Settings.System.getFloat(getContentResolver(),
                    Settings.System.QUICK_TILES_BG_ALPHA);
        } catch (Exception e) {
            transparency = 0;
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.QUICK_TILES_BG_ALPHA, 0.0f);
        }
        mQsTileAlpha = (SlimSeekBarPreference) findPreference(QUICK_TILES_ALPHA);
        mQsTileAlpha.setInitValue((int) (transparency * 100));
        mQsTileAlpha.setOnPreferenceChangeListener(this);

        mNotificationWallpaper =
                (ListPreference) findPreference(NOTIFICATION_WALLPAPER);
        mNotificationWallpaper.setOnPreferenceChangeListener(this);

        mNotificationWallpaperLandscape =
                (ListPreference) findPreference(NOTIFICATION_WALLPAPER_LANDSCAPE);
        mNotificationWallpaperLandscape.setOnPreferenceChangeListener(this);

        if (!DeviceUtils.isPhone(mActivity)) {
            prefSet.removePreference(mNotificationWallpaperLandscape);
        }

        try{
            transparency = Settings.System.getFloat(getContentResolver(),
                    Settings.System.NOTIFICATION_BACKGROUND_ALPHA);
        } catch (Exception e) {
            transparency = 0;
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.NOTIFICATION_BACKGROUND_ALPHA, 0.1f);
        }
        mWallpaperAlpha = (SlimSeekBarPreference) findPreference(NOTIFICATION_WALLPAPER_ALPHA);
        mWallpaperAlpha.setInitValue((int) (transparency * 100));
        mWallpaperAlpha.setOnPreferenceChangeListener(this);

        try{
            transparency = Settings.System.getFloat(getContentResolver(),
                    Settings.System.NOTIFICATION_ALPHA);
        } catch (Exception e) {
            transparency = 0;
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.NOTIFICATION_ALPHA, 0.0f);
        }
        mNotificationAlpha = (SlimSeekBarPreference) findPreference(NOTIFICATION_ALPHA);
        mNotificationAlpha.setInitValue((int) (transparency * 100));
        mNotificationAlpha.setOnPreferenceChangeListener(this);

        setHasOptionsMenu(true);
        mCheckPreferences = true;
        updateCustomBackgroundSummary();
        return prefs;
    }


    @Override
    public void onResume() {
        super.onResume();
        updateCustomBackgroundSummary();
    }


    private void updateCustomBackgroundSummary() {
        int resId;
        String value = Settings.System.getString(getContentResolver(),
                Settings.System.NOTIFICATION_BACKGROUND);
        if (value == null) {
            resId = R.string.notification_background_default_wallpaper;
            mNotificationWallpaper.setValueIndex(2);
            mNotificationWallpaperLandscape.setEnabled(false);
        } else if (value.startsWith("color=")) {
            resId = R.string.notification_background_color_fill;
            mNotificationWallpaper.setValueIndex(0);
            mNotificationWallpaperLandscape.setEnabled(false);
        } else {
            resId = R.string.notification_background_custom_image;
            mNotificationWallpaper.setValueIndex(1);
            mNotificationWallpaperLandscape.setEnabled(true);
        }
        mNotificationWallpaper.setSummary(getResources().getString(resId));

        value = Settings.System.getString(getContentResolver(),
                Settings.System.NOTIFICATION_BACKGROUND_LANDSCAPE);
        if (value == null) {
            resId = R.string.notification_background_default_wallpaper;
            mNotificationWallpaperLandscape.setValueIndex(1);
        } else {
            resId = R.string.notification_background_custom_image;
            mNotificationWallpaperLandscape.setValueIndex(0);
        }
        mNotificationWallpaperLandscape.setSummary(getResources().getString(resId));
    }

    public void deleteWallpaper(boolean orientation) {
        String path = Settings.System.getString(getContentResolver(),
                Settings.System.NOTIFICATION_BACKGROUND);
        if (path != null && !path.startsWith("color=")) {
            File wallpaperToDelete = new File(Uri.parse(path).getPath());

            if (wallpaperToDelete != null
                    && wallpaperToDelete.exists() && !orientation) {
                wallpaperToDelete.delete();
            }
        }

        path = Settings.System.getString(getContentResolver(),
                Settings.System.NOTIFICATION_BACKGROUND_LANDSCAPE);
        if (path != null) {
            File wallpaperToDelete = new File(Uri.parse(path).getPath());

            if (wallpaperToDelete != null
                    && wallpaperToDelete.exists() && orientation) {
                wallpaperToDelete.delete();
            }
            if (orientation) {
                Settings.System.putString(getContentResolver(),
                    Settings.System.NOTIFICATION_BACKGROUND_LANDSCAPE, null);
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_WALLPAPER
                    || requestCode == REQUEST_PICK_WALLPAPER_LANDSCAPE) {

                if (mImageTmp.length() == 0 || !mImageTmp.exists()) {
                    Toast.makeText(mActivity,
                            getResources().getString(R.string.shortcut_image_not_valid),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                File image = new File(mActivity.getFilesDir() + File.separator
                        + "notification_background_" + System.currentTimeMillis() + ".png");
                String path = image.getAbsolutePath();
                mImageTmp.renameTo(image);
                image.setReadable(true, false);

                if (requestCode == REQUEST_PICK_WALLPAPER) {
                    Settings.System.putString(getContentResolver(),
                        Settings.System.NOTIFICATION_BACKGROUND, path);
                } else {
                    Settings.System.putString(getContentResolver(),
                        Settings.System.NOTIFICATION_BACKGROUND_LANDSCAPE, path);
                }
            }
        } else {
            if (mImageTmp.exists()) {
                mImageTmp.delete();
            }
        }
        updateCustomBackgroundSummary();
    }

    private void startPictureCrop(int request, boolean landscape) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        Intent intent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        intent.putExtra("crop", "true");
        boolean isPortrait = getResources()
            .getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        intent.putExtra("aspectX", (landscape ? !isPortrait : isPortrait)
                ? width : height);
        intent.putExtra("aspectY", (landscape ? !isPortrait : isPortrait)
                ? height : width);
        intent.putExtra("outputX", (landscape ? !isPortrait : isPortrait)
                ? width : height);
        intent.putExtra("outputY", (landscape ? !isPortrait : isPortrait)
                ? height : width);
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.PNG.toString());
        try {
            mImageTmp.createNewFile();
            mImageTmp.setWritable(true, false);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(mImageTmp));
            startActivityForResult(intent, request);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTintedStatusbar) {
            int val = Integer.parseInt((String) newValue);
            int index = mTintedStatusbar.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
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
            int val = Integer.parseInt((String) newValue);
            int index = mTintedStatusbarOption.findIndexOfValue((String) newValue);
            Settings.System.putInt(getContentResolver(),
                Settings.System.STATUS_BAR_TINTED_OPTION, val);
            mTintedStatusbarOption.setSummary(mTintedStatusbarOption.getEntries()[index]);
        } else if (preference == mTintedStatusbarTransparency) {
            int val = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_TINTED_STATBAR_TRANSPARENT, val);
            return true;
        } else if (preference == mTintedNavbarTransparency) {
            int val = ((Integer)newValue).intValue();
            Settings.System.putInt(getContentResolver(),
                    Settings.System.STATUS_BAR_TINTED_NAVBAR_TRANSPARENT, val);
            return true;
        } else if (preference == mWallpaperAlpha) {
            float valNav = Float.parseFloat((String) newValue);
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.NOTIFICATION_BACKGROUND_ALPHA, valNav / 100);
            return true;
        } else if (preference == mNotificationAlpha) {
            float valNav = Float.parseFloat((String) newValue);
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.NOTIFICATION_ALPHA, valNav / 100);
            return true;
        } else if (preference == mNotificationWallpaper) {
            int indexOf = mNotificationWallpaper.findIndexOfValue(newValue.toString());
            switch (indexOf) {
                //Displays color dialog when user has chosen color fill
                case 0:
                    showDialogInner(DLG_PICK_COLOR);
                    break;
                //Launches intent for user to select an image/crop it to set as background
                case 1:
                    startPictureCrop(REQUEST_PICK_WALLPAPER, false);
                    break;
                //Sets background to default
                case 2:
                    deleteWallpaper(false);
                    deleteWallpaper(true);
                    Settings.System.putString(getContentResolver(),
                            Settings.System.NOTIFICATION_BACKGROUND, null);
                    updateCustomBackgroundSummary();
                    break;
            }
            return true;
        } else if (preference == mNotificationWallpaperLandscape) {
            int indexOf = mNotificationWallpaperLandscape.findIndexOfValue(newValue.toString());
            switch (indexOf) {
                //Launches intent for user to select an image/crop it to set as background
                case 0:
                    startPictureCrop(REQUEST_PICK_WALLPAPER_LANDSCAPE, true);
                    break;
                //Sets background to default
                case 1:
                    deleteWallpaper(true);
                    updateCustomBackgroundSummary();
                    break;
            }
            return true;
        } else if (preference == mQuickTilesBgColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_TILES_BG_COLOR,
                    intHex);
            return true;

        } else if (preference == mQuickTilesBgPressedColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_TILES_BG_PRESSED_COLOR,
                    intHex);
            return true;
        } else if (preference == mQuickTilesTextColor) {
            String hex = ColorPickerPreference.convertToARGB(
                    Integer.valueOf(String.valueOf(newValue)));
            preference.setSummary(hex);
            int intHex = ColorPickerPreference.convertToColorInt(hex);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.QUICK_TILES_TEXT_COLOR,
                    intHex);
            return true;
        } else if (preference == mQsTileAlpha) {
            float valNav = Float.parseFloat((String) newValue);
            Settings.System.putFloat(getContentResolver(),
                    Settings.System.QUICK_TILES_BG_ALPHA, valNav / 100);
            return true;
        }
        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                showDialogInner(DLG_RESET);
                return true;
             default:
                return super.onContextItemSelected(item);
        }
    }

    private void showDialogInner(int id) {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(id);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int id) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            frag.setArguments(args);
            return frag;
        }

        NotificationDrawerStyle getOwner() {
            return (NotificationDrawerStyle) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            switch (id) {
                case DLG_RESET:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.reset)
                    .setMessage(R.string.qs_style_reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.QUICK_TILES_BG_COLOR, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.QUICK_TILES_BG_PRESSED_COLOR, -2);
                            Settings.System.putInt(getActivity().getContentResolver(),
                                    Settings.System.QUICK_TILES_TEXT_COLOR, -2);
                            getOwner().refreshSettings();
                        }
                    })
                    .create();
                case DLG_PICK_COLOR:
                    final ColorPickerView colorView = new ColorPickerView(getOwner().mActivity);
                    String currentColor = Settings.System.getString(
                            getOwner().getContentResolver(),
                            Settings.System.NOTIFICATION_BACKGROUND);
                    if (currentColor != null && currentColor.startsWith("color=")) {
                        int color = Color.parseColor(currentColor.substring("color=".length()));
                        colorView.setColor(color);
                    }
                    colorView.setAlphaSliderVisible(false);

                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.notification_drawer_custom_background_dialog_title)
                    .setView(colorView)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().deleteWallpaper(false);
                            getOwner().deleteWallpaper(true);
                            Settings.System.putString(
                                getOwner().getContentResolver(),
                                Settings.System.NOTIFICATION_BACKGROUND,
                                "color=" + String.format("#%06X",
                                (0xFFFFFF & colorView.getColor())));
                            getOwner().updateCustomBackgroundSummary();
            
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

}
