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

import android.content.Context;
import android.text.TextUtils;

import net.named_data.nfd.utils.G;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Manager that controls the loading and saving of tags that are being logged.
 */
public class LogcatSettingsManager {

  private LogcatSettingsManager(Context context) {
    m_context = context;
    m_logcatSettingItems = new ArrayList<>();
    m_logLevelMap = new HashMap<>();
    m_logcatTagsJSONSerializer = new LogcatTagsJSONSerializer(context, NFD_LOGCAT_TAGS_FILENAME);

    // Populate log level map
    loadLogLevelMap();

    // Check if previous tags setting exists; otherwise load defaults
    if (m_logcatTagsJSONSerializer.logcatTagsFileExists()) {
      loadSettingItems();
    } else {
      loadDefaultSettingItems();
    }

    // Save setting items to file
    saveSettingItems();

    // Sort log tag name lexicographically
    Collections.sort(m_logcatSettingItems, new Comparator<LogcatSettingItem>() {
      @Override
      public int compare(LogcatSettingItem lhs, LogcatSettingItem rhs) {
        return lhs.getLogTag().compareTo(rhs.getLogTag());
      }
    });
  }

  /**
   * Gets the singleton logcat settings manager.
   *
   * @param context Current application context
   * @return The singleton settings manager
   */
  public static LogcatSettingsManager get(Context context) {
    if (s_logcatSettingsManager == null) {
      s_logcatSettingsManager = new LogcatSettingsManager(context.getApplicationContext());
    }
    return s_logcatSettingsManager;
  }

  /**
   * Return the current working copy of setting items that are managed by the
   * settings manager.
   *
   * @return Current setting items that are loaded
   */
  public ArrayList<LogcatSettingItem> getLogcatSettingItems() {
    return m_logcatSettingItems;
  }

  /**
   * Generate a string representing all the tags to be filtered by logcat
   * and the relevant log levels.
   *
   * An example of a string returned by this method is:
   *
   * <pre>
   *    NFDService:S Strategy:S TcpChannel:S TcpFactory:S TcpLocalFace:S UdpFactory:S *:S
   * </pre>
   *
   * @return String representation of the tags and their relevant log levels to be
   * filtered.
   */
  public String getTags() {
    ArrayList<String> arr = new ArrayList<>();
    for (LogcatSettingItem item : m_logcatSettingItems) {
      arr.add(String.format("%s:%s", item.getLogTag(), getPriorityName(item.getLogLevel())));
    }

    // Sort and silence everything else by default
    Collections.sort(arr);
    arr.add("*:S");

    return TextUtils.join(" ", arr);
  }

  /**
   * Convenience method that saves all tags present in m_logcatSettingItems
   * to the persistent JSON storage file.
   */
  public void saveSettingItems() {
    // Create tags map
    Map<CharSequence, CharSequence> map = new HashMap<>();
    for (LogcatSettingItem item : m_logcatSettingItems) {
      map.put(item.getLogTag(), getPriorityName(item.getLogLevel()));
    }

    // Save tags
    try {
      m_logcatTagsJSONSerializer.saveTags(map);
    } catch (IOException e) {
      G.Log("saveSettingItems(): Error: " + e);
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Convenience method that loads all possible log level priority values as
   * specified by the string array: R.array.logcat_log_level_map.
   */
  private void loadLogLevelMap() {
    for (String item : m_context.getResources().getStringArray(R.array.logcat_log_level_map)) {
      String [] arr = item.split(":");
      m_logLevelMap.put(arr[0], arr[1]);
    }
  }

  /**
   * Convenience method that loads default values for tags and log levels that
   * should be used. This method should be invoked when the application does not
   * have previous JSON state information of the tags and log levels.
   */
  private void loadDefaultSettingItems() {
    String [] defaults
        = m_context.getResources().getStringArray(R.array.default_log_tags_and_levels);
    for (String item : defaults) {
      String [] arr = item.split(":");
      m_logcatSettingItems.add(new LogcatSettingItem(arr[0], getVerbosePriorityName(arr[1])));
    }
  }

  /**
   * Convenience method that loads all tags that were previously stored and recorded. This
   * method populates m_logcatSettingItems for use in a ListView to present all loaded tags and
   * their relevant log levels.
   */
  private void loadSettingItems() {
    Map<CharSequence, CharSequence> map;

    try {
      map = m_logcatTagsJSONSerializer.loadTags();

      for (Map.Entry<CharSequence, CharSequence> entry : map.entrySet()) {
        m_logcatSettingItems
            .add(new LogcatSettingItem(entry.getKey().toString(),
                getVerbosePriorityName(entry.getValue().toString())));
      }
    } catch (IOException | NullPointerException e) {
      G.Log("loadSettingItems(): Error in loading tags from file: " + e);
    }
  }

  /**
   * Convenience method to get the verbose priority name. For instance, if
   * "V" were passed in, the returned string would be "Verbose". This is
   * dependent on the data that is loaded into m_logLevelMap.
   *
   * @param priority Short form priority name, e.g. "V"
   * @return Verbose priority name, e.g. "Verbose" for a priority argument of "V"
   */
  private String getVerbosePriorityName(String priority) {
    for (Map.Entry<CharSequence, CharSequence> item : m_logLevelMap.entrySet()) {
      if (item.getValue().equals(priority)) {
        return item.getKey().toString();
      }
    }
    return null;
  }

  /**
   * Convenience method that gets the priority name from the verbose name. For instance,
   * if "Verbose" were passed in, the returned string would be "V". This is
   * dependent on the data that is loaded into m_logLevelMap.
   *
   * @param priorityVerboseName Verbose priority name, e.g. "Verbose"
   * @return Short form priority name, e.g. "V"
   */
  private String getPriorityName(String priorityVerboseName) {
    return m_logLevelMap.get(priorityVerboseName).toString();
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * JSON Serializer used to store JSON Tags and relevant settings to
   * a local file. The JSON file format that this serializer stores
   * in the private file is as such:
   *
   * <pre>
   *   [
   *     {
   *       "TcpFactory": "V",
   *       "CommandValidator": "V",
   *       "RibManager": "V",
   *       "Strategy": "V",
   *       "FaceTable": "V",
   *       "FibManager": "V",
   *       "FaceManager": "V",
   *       "PrivilegeHelper": "V",
   *       "ManagerBase": "V",
   *       "TcpChannel": "V",
   *       "InternalFace": "V",
   *       "TcpLocalFace": "V",
   *       "RemoteRegistrator": "V",
   *       "GeneralConfigSection": "V",
   *       "UdpFactory": "V",
   *       "StrategyChoice": "V",
   *       "TablesConfigSection": "V"
   *     }
   *   ]
   * </pre>
   *
   * Each line represents a log tag that is to be monitored as well as the
   * log level to be monitored (aka priority level of log cat). Tags can be any
   * tag for the logger to filter.
   *
   * The log level should be one of the following form:
   *
   * <pre>
   *     Log Level | Meaning
   *     ===================
   *       V       : Verbose
   *       D       : Debug
   *       I       : Info
   *       W       : Warning
   *       E       : Error
   *       F       : Fatal
   *       S       : Silent
   * </pre>
   *
   */
  private static class LogcatTagsJSONSerializer {

    public LogcatTagsJSONSerializer(Context context, String filename) {
      m_context = context;
      m_filename = filename;
    }

    /**
     * Convenience method to save all tags and their respective log levels from the
     * given map. The map should contain key-value pairs of the following format:
     *
     * <pre>
     *   "TagName": "V"
     * </pre>
     *
     * @param map Map to be converted saved as a JSON file
     * @throws IOException
     */
    public void saveTags(Map<CharSequence, CharSequence> map) throws IOException {
      // Create JSON Array
      JSONArray array = new JSONArray().put(new JSONObject(map));

      BufferedWriter writer = null;
      try {
        OutputStream out = m_context.openFileOutput(m_filename, Context.MODE_PRIVATE);
        writer = new BufferedWriter(new OutputStreamWriter(out));
        writer.write(array.toString());
      } catch (IOException e) {
        G.Log(String.format("saveTags(): Error while writing to file: %s - %s",
            m_filename, e));
      } finally {
        if (writer != null) {
          writer.close();
        }
      }
    }

    /**
     * Convenience method to load all tags that were previously saved to a JSON
     * file. The format depends on what was previously stored in the JSON file,
     * but it is recommended to following the format set out in
     * {@link
     * net.named_data.nfd.LogcatSettingsManager.LogcatTagsJSONSerializer#saveTags(java.util.Map)}
     *
     * @return Map that was previously stored in the JSON file
     * @throws IOException
     */
    public Map<CharSequence, CharSequence> loadTags() throws IOException {
      Map<CharSequence, CharSequence> map = new HashMap<>();

      BufferedReader reader = null;
      try {
        InputStream inputStream = m_context.openFileInput(m_filename);
        reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder jsonString = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          jsonString.append(line);
        }

        // Parse JSON array
        JSONArray array = (JSONArray) new JSONTokener(jsonString.toString()).nextValue();

        // Populate map
        for (int i=0; i<array.length(); i++) {
          JSONObject jsonObject = array.getJSONObject(i);
          Iterator<String> iterator = jsonObject.keys();
          while (iterator.hasNext()) {
            String key = iterator.next();
            map.put(key, (CharSequence) jsonObject.get(key));
          }
        }
      } catch (JSONException | IOException e) {
        G.Log(String.format("saveTags(): Error while reading from file: %s - %s",
            m_filename, e));
      } finally {
        if (reader != null) {
          reader.close();
        }
      }

      return map;
    }

    /**
     * @return true if a previously saved file exists; false otherwise
     */
    public boolean logcatTagsFileExists() {
      return m_context.getFileStreamPath(m_filename).exists();
    }

    /** Context for storing and retrieving files */
    private final Context m_context;

    /** File name to store JSON */
    private final String m_filename;
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Context for storage and retrieval of logcat settings (e.g. from Preferences or Files) */
  private final Context m_context;

  /** Array list that contains all the tags that are logged */
  private final ArrayList<LogcatSettingItem> m_logcatSettingItems;

  /** Mapping of log tag description to logcat priority setting */
  private final Map<CharSequence, CharSequence> m_logLevelMap;

  /** Reference to JSON Serializer for use */
  private final LogcatTagsJSONSerializer m_logcatTagsJSONSerializer;

  /** Singleton reference to a LogcatSettingsManager */
  private static LogcatSettingsManager s_logcatSettingsManager;

  /** NFD Logcat Tags JSON filename */
  private static final String NFD_LOGCAT_TAGS_FILENAME = "NFD_LOGCAT_TAGS_FILE";
}
