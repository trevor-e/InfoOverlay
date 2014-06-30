package com.trevore.infooverlay;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;

import javax.inject.Inject;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public static class MainFragment extends PreferenceFragment {
        public static final String KEY_LOCATION = OverlayService.KEY_LOCATION;
        public static final String KEY_ENABLED = OverlayService.KEY_ENABLED;
        public static final String KEY_COLOR = OverlayService.KEY_COLOR;

        /**
         * User's shared preferences
         */
        @Inject
        SharedPreferences sharedPreferences;

        /**
         * The preference used to pick the location of the text display
         */
        private ListPreference locationPreference;

        /**
         * The preference used to toggle the status of the service
         */
        private SwitchPreference statusPreference;

        /**
         * The preference used to set the text color
         */
        private EditTextPreference colorPreference;

        /**
         * Listens for changes to the location preference, updates the location
         */
        private Preference.OnPreferenceChangeListener locationChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Intent intent = new Intent(getActivity(), OverlayService.class);
                intent.putExtra(KEY_LOCATION, (String) newValue);
                MainFragment.this.getActivity().startService(intent);
                return true;
            }
        };

        /**
         * Listens for changes to the status preference, toggles the background thread
         */
        private Preference.OnPreferenceChangeListener serviceStatusListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Intent intent = new Intent(getActivity(), OverlayService.class);
                intent.putExtra(KEY_ENABLED, String.valueOf((Boolean) newValue));
                MainFragment.this.getActivity().startService(intent);
                return true;
            }
        };

        private Preference.OnPreferenceChangeListener colorChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Intent intent = new Intent(getActivity(), OverlayService.class);
                intent.putExtra(KEY_COLOR, (String) newValue);
                MainFragment.this.getActivity().startService(intent);
                return true;
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            MainApplication.from(getActivity()).getObjectGraph().inject(this);

            addPreferencesFromResource(R.xml.preferences);
            PreferenceManager.setDefaultValues(getActivity(), R.xml.preferences, true);
            locationPreference = (ListPreference) findPreference(getString(R.string.pref_location));
            locationPreference.setOnPreferenceChangeListener(locationChangeListener);
            statusPreference = (SwitchPreference) findPreference(getString(R.string.pref_service_enabled));
            statusPreference.setOnPreferenceChangeListener(serviceStatusListener);
            colorPreference = (EditTextPreference) findPreference(getString(R.string.pref_color));
            colorPreference.setOnPreferenceChangeListener(colorChangeListener);

            Intent intent = new Intent(getActivity(), OverlayService.class);
            intent.putExtra(KEY_LOCATION, locationPreference.getValue());
            intent.putExtra(KEY_ENABLED, String.valueOf(statusPreference.isChecked()));
            intent.putExtra(KEY_COLOR, colorPreference.getText());
            getActivity().startService(intent);
        }
    }
}
