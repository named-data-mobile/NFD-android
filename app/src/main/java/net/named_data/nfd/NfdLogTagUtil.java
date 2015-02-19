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
import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @brief Utility class to generate and retrieve log tags
 */
public class NfdLogTagUtil {

  /**
   * @brief Get tags that are stored for the given application's context.
   *
   * The method returns a string representation of tags that should be displayed
   * to the UI. These tags and their output levels have been saved by the settings
   * UI in the given context.
   *
   * An example of the return string is as such:
   *
   * <pre>
   *    NFDService:S Strategy:S TcpChannel:S TcpFactory:S TcpLocalFace:S UdpFactory:S *:S
   * </pre>
   *
   * @param context Current application context to retrieve log tags for
   * @return String representation of log tags ready for use as arguments
   *         to logcat.
   */
  public static String getTags(Context context) {
    SharedPreferences preferences = context.getSharedPreferences(
        PREFERENCE_NFD_TAGS_FILENAME, Context.MODE_PRIVATE);
    String tagsString = preferences.getString(PREFERENCE_NFD_TAGS_KEY, "");

    G.Log("loadCommand(): " + tagsString);

    return tagsString;
  }

  /**
   * @brief Save tags as a string to the current context's preference.
   *
   * An example of a tag string that should be saved is shown as follows:
   *
   * <pre>
   *    NFDService:S Strategy:S TcpChannel:S TcpFactory:S TcpLocalFace:S UdpFactory:S *:S
   * </pre>
   *
   * NdfLogTagUtil.TagBuilder provides convenient methods to generate tag strings.
   *
   * @param context Current application context to save tag string to
   * @param tagsString String representation of the tags to be saved
   */
  public static void saveTags(Context context, String tagsString) {
    // Save preferred log level
    SharedPreferences.Editor editor
        = context.getSharedPreferences(
        PREFERENCE_NFD_TAGS_FILENAME, Context.MODE_PRIVATE).edit();
    editor.putString(PREFERENCE_NFD_TAGS_KEY, tagsString);
    editor.commit();

    G.Log("saveTags(): " + tagsString);
  }

  /**
   * @brief Convenience class to create and generate tags for use as arguments
   * to logcat.
   */
  public static class TagBuilder {

    private TagBuilder() {
      m_tagMap = new HashMap<CharSequence, CharSequence>();
    }

    /**
     * @brief Get a new instance of a TagBuilder object.
     *
     * @return New TagBuilder ojbect for use.
     */
    public static TagBuilder getTagBuilder() {
      return new TagBuilder();
    }

    /**
     * @bried Add a tag with an associated log level value.
     *
     * Tag can be any tag for the logger to filter.
     *
     * The log level should be one of the following form:
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
     * @param tag Tag for the logger to filter for.
     * @param logLevel Log level for specified tag.
     */
    public void addTag(CharSequence tag, CharSequence logLevel) {
      m_tagMap.put(tag, logLevel);
    }

    /**
     * @brief Silence all tags that are not added to the current TagBuilder
     * object.
     */
    public synchronized  void addSilenceNonRelatedTags() {
      m_silenceNonRelatedTags = true;
    }

    /**
     * @brief Generate a string representing all the tags to be filtered and the
     * relevant log levels.
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
    public String generateTagString() {
      ArrayList<String> tags = new ArrayList<String>();
      for (Map.Entry<CharSequence, CharSequence> item : m_tagMap.entrySet()) {
        tags.add(String.format("%s:%s", item.getKey(), item.getValue()));
      }

      Collections.sort(tags);

      if (m_silenceNonRelatedTags) {
        tags.add("*:S");
      }

      return TextUtils.join(" ", tags);
    }

    /** Mapping of tag and log levels */
    private Map<CharSequence, CharSequence> m_tagMap;

    /** Flag that determines if all non related tags should be silenced */
    private boolean m_silenceNonRelatedTags;
  }

  /** @brief Preference filename */
  private static final String PREFERENCE_NFD_TAGS_FILENAME = "NFD_TAGS_PREFERENCE_FILE";

  /** @brief Key in SharedPreference that stores the string of tags */
  private static final String PREFERENCE_NFD_TAGS_KEY = "NFD_TAGS";
}
