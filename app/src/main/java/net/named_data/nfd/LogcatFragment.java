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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import net.named_data.nfd.utils.G;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Logcat fragment that houses the output of LogCat in a ListView for viewing.
 *
 * The ListView is backed by a {@link net.named_data.nfd.LogcatFragment.LogListAdapter}
 * that that limits the number of logs that are being displayed to the user. This
 * prevents the ListView from growing indeterminately. This maximum number of
 * log lines that is allowed to be displayed is set by
 * {@link net.named_data.nfd.LogcatFragment#s_logMaxLines}.
 *
 */
public class LogcatFragment extends Fragment {

  public static Fragment newInstance() {
    return new LogcatFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    @SuppressLint("InflateParams")
    View v = inflater.inflate(R.layout.fragment_logcat_output, null);

    // Get UI Elements
    m_logListView = (ListView) v.findViewById(R.id.log_output);
    m_logListAdapter = new LogListAdapter(inflater, s_logMaxLines);
    m_logListView.setAdapter(m_logListAdapter);

    return v;
  }

  @Override
  public void onResume() {
    super.onResume();
    startLogging();
    G.Log("LogcatFragment(): onResume()");
  }

  @Override
  public void onPause() {
    super.onPause();
    stopLogging();
    G.Log("LogcatFragment(): onPause()");
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_log, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_log_settings:
        m_callbacks.onDisplayLogcatSettings();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      m_callbacks = (Callbacks) activity;
    } catch (ClassCastException e) {
      G.Log("Hosting activity must implement callback.");
      throw e;
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    m_callbacks = null;
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Starts logging by spawning a new thread to capture logs.
   */
  private void startLogging() {
    // Clear output, update UI and get tag arguments
    clearLogOutput();
    appendLogText(getString(R.string.loading_logger));
    m_tagArguments = LogcatSettingsManager.get(getActivity()).getTags();

    new Thread(){
      @Override
      public void run() {
        captureLog();
        G.Log("Thread done capturing logs.");
      }
    }.start();
  }

  /**
   * Stops the logging by killing the process running logcat.
   */
  private void stopLogging() {
    // Kill process
    m_logProcess.destroy();
  }

  /**
   * Clear log adapter and update UI.
   */
  private void clearLogOutput() {
    m_logListAdapter.clearMessages();
  }

  /**
   * Convenience method to append a message to the log output
   * and scroll to the bottom of the log.
   *
   * @param message String message to be posted to the text view.
   */
  private void appendLogText(String message) {
    m_logListAdapter.addMessage(message);
    m_logListView.setSelection(m_logListAdapter.getCount() - 1);
  }

  /**
   * Convenience method to capture the output from logcat.
   */
  private void captureLog() {
    try {
      /**
       * NOTE: The use of the 'time' log output format is hard
       * coded for now. This option is similar to that of the
       * log output that is seen in Android Studio and Eclipse.
       *
       * In the future this value can be user configurable or
       * placed in the log preference settings.
       */
      // Build command for execution
      String cmd = String.format("%s -v time %s *:S",
          "logcat",
          m_tagArguments);

      G.Log("LogCat Command: " + cmd);

      m_logProcess =  Runtime.getRuntime().exec(cmd);
      BufferedReader in = new BufferedReader(
          new InputStreamReader(m_logProcess.getInputStream()));

      String line;
      while ((line = in.readLine()) != null) {
        final String message = line;
        getActivity().runOnUiThread(new Runnable() {
          @Override
          public void run() {
            appendLogText(message);
          }
        });
      }

      // Wait for process to join this thread
      m_logProcess.waitFor();
    } catch (IOException | InterruptedException e) {
      G.Log("captureLog(): " + e);
    }
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Custom LogListAdapter to limit the number of log lines that
   * is being stored and displayed.
   */
  private static class LogListAdapter extends BaseAdapter {

    /**
     * Create a ListView compatible adapter with an
     * upper bound on the maximum number of entries that will
     * be displayed in the ListView.
     *
     * @param maxLines Maximum number of entries allowed in
     *                 the ListView for this adapter.
     */
    public LogListAdapter(LayoutInflater layoutInflater, int maxLines) {
      m_data = new ArrayList<>();
      m_layoutInflater = layoutInflater;
      m_maxLines = maxLines;
    }

    /**
     * Add a message to be displayed in the log's list view.
     *
     * @param message Message to be added to the underlying data store
     *                and displayed on thi UI.
     */
    public void addMessage(String message) {
      if (m_data.size() == m_maxLines) {
        m_data.get(0);
      }

      m_data.add(message);
      notifyDataSetChanged();
    }

    /**
     * Convenience method to clear all messages from the underlying
     * data store and update the UI.
     */
    public void clearMessages() {
      m_data.clear();
      this.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      return m_data.size();
    }

    @Override
    public Object getItem(int position) {
      return m_data.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      LogEntryViewHolder holder;

      if (convertView == null) {
        holder = new LogEntryViewHolder();

        convertView = m_layoutInflater.inflate(R.layout.list_item_log, null);
        convertView.setTag(holder);

        holder.logLineTextView = (TextView) convertView.findViewById(R.id.log_line);
      } else {
        holder = (LogEntryViewHolder) convertView.getTag();
      }

      holder.logLineTextView.setText(m_data.get(position));
      return convertView;
    }

    /** Underlying message data store for log messages*/
    private final ArrayList<String> m_data;

    /** Layout inflater for inflating views */
    private final LayoutInflater m_layoutInflater;

    /** Maximum number of log lines to display */
    private final int m_maxLines;
  }

  /**
   * Log entry view holder object for holding the output.
   */
  private static class LogEntryViewHolder {
    public TextView logLineTextView;
  }

  //////////////////////////////////////////////////////////////////////////////

  public interface Callbacks {
    void onDisplayLogcatSettings();
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Maximum number of log lines to be displayed by the backing adapter of the ListView */
  private static final int s_logMaxLines = 380;

  /** Process in which logcat is running in */
  private Process m_logProcess;

  /** ListView for displaying log output in */
  private ListView m_logListView;

  /** Customized ListAdapter for controlling log output */
  private LogListAdapter m_logListAdapter;

  /** Tag argument to logcat */
  private String m_tagArguments;

  /** Callback reference to hosting activity */
  private Callbacks m_callbacks;
}
