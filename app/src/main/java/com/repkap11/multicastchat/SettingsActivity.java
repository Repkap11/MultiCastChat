package com.repkap11.multicastchat;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            setupSimplePreferencesScreen();
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            // Inflate the menu; this adds items to the action bar if it is present.
            inflater.inflate(R.menu.activity_main, menu);
            super.onCreateOptionsMenu(menu, inflater);
        }

        private void setupSimplePreferencesScreen() {

            // In the simplified UI, fragments are not used at all and we instead
            // use the older PreferenceActivity APIs.

            // Add 'general' preferences.
            addPreferencesFromResource(R.xml.pref_none);

            PreferenceCategory fakeHeader = new PreferenceCategory(getActivity());
            fakeHeader.setTitle(R.string.pref_header_general);
            getPreferenceScreen().addPreference(fakeHeader);
            addPreferencesFromResource(R.xml.pref_general);
            bindPreferenceSummaryToValue(findPreference("pref_display_name"));

            fakeHeader = new PreferenceCategory(getActivity());
            fakeHeader.setTitle(R.string.pref_header_notifications);
            getPreferenceScreen().addPreference(fakeHeader);
            addPreferencesFromResource(R.xml.pref_notification);

            fakeHeader = new PreferenceCategory(getActivity());
            fakeHeader.setTitle(R.string.pref_header_network);
            getPreferenceScreen().addPreference(fakeHeader);
            addPreferencesFromResource(R.xml.pref_network);
            bindPreferenceSummaryToValue(findPreference("pref_ip_address"));
            bindPreferenceSummaryToValue(findPreference("pref_port"));
        }


        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();

                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

        private static void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), ""));
        }
    }
}
