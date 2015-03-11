/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015 Regents of the University of California
 *
 * This file is part of NFD (Named Data Networking Forwarding Daemon) Android.
 * See AUTHORS.md for complete list of NFD Android authors and contributors.
 *
 * NFD Android is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * NFD Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * NFD Android, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.named_data.nfd;

import android.app.Activity;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import net.named_data.nfd.utils.LogcatTags;

import java.util.ArrayList;
import java.util.List;

public class LogcatSettingsActivity extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    getFragmentManager().
        beginTransaction().
        replace(android.R.id.content, new NfdLogSettingsFragment()).
        commit();
  }

  public static class NfdLogSettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      // Load the preferences from an XML resource
      addPreferencesFromResource(R.xml.pref_nfd_log);

      PreferenceScreen preferenceScreen = getPreferenceScreen();

      //// NOTE: This section of code demonstrates how a preference can
      //// be added programmatically.
      ////
      // Creating and inserting a preference programmatically
      // Preference customPreference = preferenceScreen.
      //     findPreference(getString(R.string.pref_category_title_tags_key));
      // if (customPreference instanceof PreferenceCategory) {
      //   ListPreference listPreference = new ListPreference(getActivity());
      //   listPreference.setKey("NFDTest_Key");
      //   listPreference.setTitle("NFDTest");
      //   listPreference.setDefaultValue("I");
      //   listPreference.setNegativeButtonText(null);
      //   listPreference.setDialogTitle("NFDTest");

      //   String [] keys = getResources().getStringArray(R.array.pref_log_levels);
      //   String [] values = getResources().getStringArray(R.array.pref_log_level_values);

      //   listPreference.setEntries(keys);
      //   listPreference.setEntryValues(values);

      //   ((PreferenceCategory) customPreference).addPreference(listPreference);
      // }

      // Collect all Preference in the hierarchy
      m_tagListPreferences = new ArrayList<ListPreference>();
      extractPreferences(m_tagListPreferences,
          (PreferenceGroup) preferenceScreen.
              findPreference(getString(R.string.pref_category_title_tags_key)));

      // Set all preference setting
      m_resetPreference = preferenceScreen.
          findPreference(getString(R.string.pref_tags_log_level_key));
    }

    @Override
    public void onResume() {
      super.onResume();
      registerListeners();
    }

    @Override
    public void onPause() {
      super.onPause();
      unregisterPreferenceListeners();
      saveTagArguments();
    }

    /**
     * Convenience method to register preference listeners.
     */
    private void registerListeners() {
      for (Preference p : m_tagListPreferences) {
        if (p instanceof ListPreference) {
          registerPreferenceListener(p);
        }
      }

      m_resetPreference.setOnPreferenceChangeListener(m_setAllPreferenceChangeListener);
    }

    /**
     * Convenience method to unregister preference listeners.
     */
    private void unregisterPreferenceListeners() {
      for (Preference p : m_tagListPreferences) {
        if (p instanceof ListPreference) {
          unregisterPreferenceListener(p);
        }
      }

      m_resetPreference.setOnPreferenceChangeListener(null);
    }

    /**
     * Register preference listener and fire an update.
     *
     * @param preference Preference to register listener.
     */
    private void registerPreferenceListener(Preference preference) {
      // Attach listener
      preference.setOnPreferenceChangeListener(m_tagPreferenceChangeListener);

      // Trigger update
      m_tagPreferenceChangeListener.onPreferenceChange(preference,
          PreferenceManager.
              getDefaultSharedPreferences(preference.getContext()).
              getString(preference.getKey(), ""));
    }

    /**
     * Unregister preference listener for the given preference.
     *
     * @param preference Preference to unregister listener.
     */
    private void unregisterPreferenceListener(Preference preference) {
      // Remove listener
      preference.setOnPreferenceChangeListener(null);
    }

    /**
     * Convenience method that extracts all list preferences within a hierarchy
     * recursively.
     *
     * @param list List to add preference to
     * @param preferenceGroup Root preference group to begin search from
     */
    private void extractPreferences(List<ListPreference> list, PreferenceGroup preferenceGroup) {
      for (int i = 0; i < preferenceGroup.getPreferenceCount(); i++) {
        final Preference preference = preferenceGroup.getPreference(i);

        if (preference instanceof ListPreference) {
          list.add((ListPreference) preference);
        } else if (preference instanceof PreferenceGroup) {
          extractPreferences(list, (PreferenceGroup) preference);
        }
      }
    }

    /**
     * Save tag arguments for quick retrieval.
     */
    private void saveTagArguments() {
      LogcatTags.TagBuilder tagBuilder = LogcatTags.TagBuilder.getTagBuilder();

      for (Preference p : m_tagListPreferences) {
        if (p instanceof ListPreference) {
          ListPreference listPreference = (ListPreference) p;
          tagBuilder.addTag(listPreference.getTitle(), listPreference.getValue());
        }
      }

      LogcatTags.saveTags(getActivity(), tagBuilder.generateTagString());
    }

    /**
     * Convenience method to change all tags' log level to the
     * given logLevel.
     *
     * @param logLevel Target log level to set to.
     */
    private void setAllTagPreferences(String logLevel) {
      for (ListPreference preference : m_tagListPreferences) {
        preference.setValue(logLevel);
        m_tagPreferenceChangeListener.onPreferenceChange(preference, logLevel);
      }
    }

    /** List of preferences for registering handlers */
    private List<ListPreference> m_tagListPreferences;

    /** Reset log level preference */
    private Preference m_resetPreference;

    /**
     * Change listener that updates the summary text of the tag preferences.
     */
    private Preference.OnPreferenceChangeListener m_tagPreferenceChangeListener
        = new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object value) {
        // Get value of preference setting as a String
        String displayString = value.toString();

        // Deal with ListPreference
        if (preference instanceof ListPreference) {
          // Get display value
          ListPreference listPreference = (ListPreference) preference;
          int offset = listPreference.findIndexOfValue(displayString);
          displayString = (offset != -1) ?
              (String) listPreference.getEntries()[offset] :
              null;
        }

        // Update UI
        preference.setSummary(displayString);
        return true;
      }
    };

    /**
     * Change listener that resets all preference tags' log levels.
     */
    private Preference.OnPreferenceChangeListener m_setAllPreferenceChangeListener
        = new Preference.OnPreferenceChangeListener() {
      @Override
      public boolean onPreferenceChange(Preference preference, Object value) {
        setAllTagPreferences(value.toString());
        return true;
      }
    };
  }
}
