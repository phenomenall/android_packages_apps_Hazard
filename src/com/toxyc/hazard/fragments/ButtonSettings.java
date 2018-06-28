/*
 * Copyright (C) 2018 ToxycOS Project
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

package com.toxyc.hazard.fragments;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.ListPreference;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.os.PowerManager;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.Utils;

import com.toxyc.hazard.preference.ActionFragment;
import com.toxyc.hazard.preference.ActionPreference;
import com.toxyc.hazard.preference.CustomSeekBarPreference;

import com.android.internal.util.hwkeys.ActionConstants;
import com.android.internal.util.hwkeys.ActionUtils;

public class ButtonSettings extends ActionFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "Buttons";

    // Keys
    private static final String HWKEY_ENABLE = "hardware_keys_enable";

    // Brightness
    private static final String KEY_BUTTON_MANUAL_BRIGHTNESS_NEW = "button_manual_brightness_new";
    private static final String KEY_BUTTON_TIMEOUT = "button_timeout";
    private static final String KEY_BUTON_BACKLIGHT_OPTIONS = "button_backlight_options_category";

    // category keys
    private static final String CATEGORY_HWKEY = "hardware_keys";
    private static final String CATEGORY_HOME = "home_key";
    private static final String CATEGORY_MENU = "menu_key";
    private static final String CATEGORY_BACK = "back_key";
    private static final String CATEGORY_ASSIST = "assist_key";
    private static final String CATEGORY_APPSWITCH = "app_switch_key";
    private static final String CATEGORY_UTOUCH = "utouch_key";

    // Masks for checking presence of hardware keys.
    // Must match values in frameworks/base/core/res/res/values/config.xml
    public static final int KEY_MASK_HOME = 0x01;
    public static final int KEY_MASK_BACK = 0x02;
    public static final int KEY_MASK_MENU = 0x04;
    public static final int KEY_MASK_ASSIST = 0x08;
    public static final int KEY_MASK_APP_SWITCH = 0x10;
    public static final int KEY_MASK_CAMERA = 0x20;
    public static final int KEY_MASK_VOLUME = 0x40;

    private ContentResolver resolver;

    private ListPreference mTorchPowerButton;

    private SwitchPreference mHwKeyEnable;

    private CustomSeekBarPreference mButtonTimoutBar;
    private CustomSeekBarPreference mManualButtonBrightness;
    private PreferenceCategory mButtonBackLightCategory;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.buttons);

        resolver = getActivity().getContentResolver();
        final PreferenceScreen prefScreen = getPreferenceScreen();

        // HW Keys
        final PreferenceCategory hwkeyCat = (PreferenceCategory) prefScreen.findPreference(CATEGORY_HWKEY);
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
        final PreferenceCategory UTouchCategory =
                (PreferenceCategory) prefScreen.findPreference(CATEGORY_UTOUCH);

        final boolean hwKeysSupported = ActionUtils.isHWKeysSupported(getActivity());
        boolean hwKeysEnabled = hwKeysSupported && Settings.Secure.getIntForUser(getContentResolver(),
                    Settings.Secure.HARDWARE_KEYS_DISABLE, 0,
                    UserHandle.USER_CURRENT) == 0;

        if (!hwKeysSupported){
            prefScreen.removePreference(hwkeyCat);
            prefScreen.removePreference(backCategory);
            prefScreen.removePreference(homeCategory);
            prefScreen.removePreference(menuCategory);
            prefScreen.removePreference(assistCategory);
            prefScreen.removePreference(appSwitchCategory);
            prefScreen.removePreference(UTouchCategory);
        }else{
            mHwKeyEnable = (SwitchPreference) findPreference(HWKEY_ENABLE);
            mHwKeyEnable.setChecked(hwKeysEnabled);
            mHwKeyEnable.setOnPreferenceChangeListener(this);

            // bits for hardware keys present on device
            final int deviceKeys = getResources().getInteger(
                    com.android.internal.R.integer.config_deviceHardwareKeys);
            final int deviceWakeKeys = getResources().getInteger(
                    com.android.internal.R.integer.config_deviceHardwareWakeKeys);

            // read bits for present hardware keys
            final boolean hasHomeKey = (deviceKeys & KEY_MASK_HOME) != 0;
            final boolean hasBackKey = (deviceKeys & KEY_MASK_BACK) != 0;
            final boolean hasMenuKey = (deviceKeys & KEY_MASK_MENU) != 0;
            final boolean hasAssistKey = (deviceKeys & KEY_MASK_ASSIST) != 0;
            final boolean hasAppSwitchKey = (deviceKeys & KEY_MASK_APP_SWITCH) != 0;

            final boolean showHomeWake = (deviceWakeKeys & KEY_MASK_HOME) != 0;
            final boolean showBackWake = (deviceWakeKeys & KEY_MASK_BACK) != 0;
            final boolean showMenuWake = (deviceWakeKeys & KEY_MASK_MENU) != 0;
            final boolean showAssistWake = (deviceWakeKeys & KEY_MASK_ASSIST) != 0;
            final boolean showAppSwitchWake = (deviceWakeKeys & KEY_MASK_APP_SWITCH) != 0;

            // back key
            if (hasBackKey) {
                if (!showBackWake) {
                    backCategory.removePreference(findPreference(Settings.System.BACK_WAKE_SCREEN));
                }
            } else {
                prefScreen.removePreference(backCategory);
            }

            // home key
            if (hasHomeKey) {
                if (!showHomeWake) {
                    homeCategory.removePreference(findPreference(Settings.System.HOME_WAKE_SCREEN));
                }
            } else {
                prefScreen.removePreference(homeCategory);
            }

            // App switch key (recents)
            if (hasAppSwitchKey) {
                if (!showAppSwitchWake) {
                    appSwitchCategory.removePreference(findPreference(
                            Settings.System.APP_SWITCH_WAKE_SCREEN));
                }
            } else {
                prefScreen.removePreference(appSwitchCategory);
            }

            // menu key
            if (hasMenuKey) {
                if (!showMenuWake) {
                    menuCategory.removePreference(findPreference(Settings.System.MENU_WAKE_SCREEN));
                }
            } else {
                prefScreen.removePreference(menuCategory);
            }

            // search/assist key
            if (hasAssistKey) {
                if (!showAssistWake) {
                    assistCategory.removePreference(findPreference(Settings.System.ASSIST_WAKE_SCREEN));
                }
            } else {
                prefScreen.removePreference(assistCategory);
            }

            final boolean useUTouch = getResources().getBoolean(R.bool.config_use_utouch_hwkeys_binding);
            if (useUTouch){
                prefScreen.removePreference(backCategory);
                prefScreen.removePreference(assistCategory);
                prefScreen.removePreference(appSwitchCategory);
                prefScreen.removePreference(menuCategory);
            }else{
                prefScreen.removePreference(UTouchCategory);
            }
        }

        // Backlight
        mButtonBackLightCategory = (PreferenceCategory) findPreference(KEY_BUTON_BACKLIGHT_OPTIONS);
        final boolean enableBacklightOptions = hwKeysSupported && getResources().getBoolean(
                    com.android.internal.R.bool.config_button_brightness_support);
        if (!enableBacklightOptions) {
            prefScreen.removePreference(mButtonBackLightCategory);
        }else{
            mManualButtonBrightness = (CustomSeekBarPreference) findPreference(
                    KEY_BUTTON_MANUAL_BRIGHTNESS_NEW);
            final int customButtonBrightness = getResources().getInteger(
                    com.android.internal.R.integer.config_button_brightness_default);
            final int currentBrightness = Settings.System.getInt(resolver,
                    Settings.System.CUSTOM_BUTTON_BRIGHTNESS, customButtonBrightness);
            PowerManager pm = (PowerManager)getActivity().getSystemService(Context.POWER_SERVICE);
            mManualButtonBrightness.setMax(pm.getMaximumScreenBrightnessSetting());
            mManualButtonBrightness.setValue(currentBrightness);
            mManualButtonBrightness.setOnPreferenceChangeListener(this);

            mButtonTimoutBar = (CustomSeekBarPreference) findPreference(KEY_BUTTON_TIMEOUT);
            int currentTimeout = Settings.System.getInt(resolver,
                    Settings.System.BUTTON_BACKLIGHT_TIMEOUT, 0);
            mButtonTimoutBar.setValue(currentTimeout);
            mButtonTimoutBar.setOnPreferenceChangeListener(this);
        }

        // let super know we can load ActionPreferences
        onPreferenceScreenLoaded(ActionConstants.getDefaults(ActionConstants.HWKEYS));

        // load preferences first
        setActionPreferencesEnabled(hwKeysEnabled);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.HAZARD;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mHwKeyEnable) {
            boolean value = (Boolean) newValue;
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.HARDWARE_KEYS_DISABLE,
                    value ? 0 : 1);
            setActionPreferencesEnabled(value);
            return true;
        }
        return true;
    }

    @Override
    protected boolean usesExtendedActionsList() {
        return true;
    }
}
