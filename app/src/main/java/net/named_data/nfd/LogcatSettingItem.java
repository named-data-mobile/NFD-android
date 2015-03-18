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

/**
 * Logcat setting item that contains information about the tag name
 * in the m_logTag field and log level in the m_logLevel field.
 */
public class LogcatSettingItem {

  public LogcatSettingItem(String logTag, String logLevel) {
    m_logTag = logTag;
    m_logLevel = logLevel;
  }

  public String getLogTag() {
    return m_logTag;
  }

  public void setLogTag(String logTag) {
    m_logTag = logTag;
  }

  public String getLogLevel() {
    return m_logLevel;
  }

  public void setLogLevel(String logLevel) {
    m_logLevel = logLevel;
  }

  @Override
  public String toString() {
    return String.format("%s: %s", m_logTag, m_logLevel);
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Log tag that logcat should monitor */
  private String m_logTag;

  /** Log level (aka priority level) that logcat should use for this log tag */
  private String m_logLevel;
}
