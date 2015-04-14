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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class LogcatSettingsFragment extends ListFragment {

  public static LogcatSettingsFragment newInstance() {
    return new LogcatSettingsFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setHasOptionsMenu(true);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState)
  {
    super.onViewCreated(view, savedInstanceState);

    View v = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_logcat_tags_list_header, null);
    getListView().addHeaderView(v, null, false);
    getListView().setDivider(getResources().getDrawable(R.drawable.list_item_divider));

    // @TODO implement log item removal
    //    ListView listView = (ListView) v.findViewById(android.R.id.list);
    //    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    //    listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
    //      @Override
    //      public void onItemCheckedStateChanged(ActionMode mode,
    //                                            int position,
    //                                            long id,
    //                                            boolean checked)
    //      {
    //        // Nothing to do here
    //      }
    //
    //      @Override
    //      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    //        MenuInflater inflater = mode.getMenuInflater();
    //        inflater.inflate(R.menu.menu_logcat_settings_multiple_modal_menu, menu);
    //        return true;
    //      }
    //
    //      @Override
    //      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    //        return false;
    //      }
    //
    //      @Override
    //      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    //        switch (item.getItemId()) {
    //          case R.id.menu_item_delete_setting_item:
    //            // TODO: Delete log item tag
    //            return true;
    //          default:
    //            return false;
    //        }
    //      }
    //
    //      @Override
    //      public void onDestroyActionMode(ActionMode mode) {
    //        // Nothing to do here
    //      }
    //    });
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);

    if (m_adapter == null) {
      m_logcatSettingsManager = LogcatSettingsManager.get(getActivity());
      m_logcatSettingItems = m_logcatSettingsManager.getLogcatSettingItems();

      m_adapter = new LogcatSettingsAdapter(getActivity(), m_logcatSettingItems);
    }
    // setListAdapter must be called after addHeaderView.  Otherwise, there is an exception on some platforms.
    // http://stackoverflow.com/a/8141537/2150331
    setListAdapter(m_adapter);
  }

  @Override
  public void onDestroyView()
  {
    super.onDestroyView();
    setListAdapter(null);
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
  {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_logcat_settings, menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item)
  {
    switch (item.getItemId()) {
      case R.id.action_log_settings:
        FragmentManager fm = getActivity().getSupportFragmentManager();
        ResetLogLevelDialog dialog
          = ResetLogLevelDialog.newInstance(getString(R.string.reset_log_level_dialog_title));
        dialog.setTargetFragment(LogcatSettingsFragment.this, REQUEST_CODE_DIALOG_RESET_ALL_LOG_LEVELS);
        dialog.show(fm, DIALOG_RESET_ALL_LOG_LEVELS_TAG);

        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    final String logTag = m_logcatSettingItems.get(position).getLogTag();

    final FragmentManager fm = getActivity().getSupportFragmentManager();
    final ResetLogLevelDialog dialog = ResetLogLevelDialog.newInstance(logTag, position);

    dialog.setTargetFragment(LogcatSettingsFragment.this, REQUEST_CODE_DIALOG_SET_LOG_LEVEL);
    dialog.show(fm, DIALOG_SET_LOG_LEVEL_TAG);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (resultCode != Activity.RESULT_OK) {
      return;
    }

    String newLogLevel;
    switch (requestCode) {
    case REQUEST_CODE_DIALOG_RESET_ALL_LOG_LEVELS:
      newLogLevel = data.getStringExtra(ResetLogLevelDialog.EXTRA_RESET_LOG_LEVEL_VALUE);

      // Update settings
      for (LogcatSettingItem item : m_logcatSettingItems) {
        item.setLogLevel(newLogLevel);
      }

      // Update UI
      updateListUI();

      // Save setting items
      m_logcatSettingsManager.saveSettingItems();
      break;
    case REQUEST_CODE_DIALOG_SET_LOG_LEVEL:
      newLogLevel = data.getStringExtra(ResetLogLevelDialog.EXTRA_RESET_LOG_LEVEL_VALUE);
      final int listPosition
          = data.getIntExtra(ResetLogLevelDialog.EXTRA_LOG_ITEM_LIST_POSITION, -1);

      if (listPosition != -1) {
        m_logcatSettingItems.get(listPosition).setLogLevel(newLogLevel);

        // Update UI
        updateListUI();

        // Save setting items
        m_logcatSettingsManager.saveSettingItems();
      }
      break;
    }
  }

  /**
   * Convenience method that updates the UI by notifying the backing list adapter
   * that changes has been made to the underlying data set.
   */
  private void updateListUI() {
    ((LogcatSettingsAdapter) getListAdapter()).notifyDataSetChanged();
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Adapter for use by thi ListFragment that display a list of LogcatSettingItem.
   */
  private static class LogcatSettingsAdapter extends ArrayAdapter<LogcatSettingItem> {

    public LogcatSettingsAdapter(Context context, ArrayList<LogcatSettingItem> objects) {
      super(context, 0, objects);
      m_layoutInflater = LayoutInflater.from(context);
    }

    @SuppressLint("InflateParams")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      SettingItemHolder holder;

      if (convertView == null) {
        holder = new SettingItemHolder();

        convertView = m_layoutInflater.inflate(R.layout.list_item_setting_item, null);
        convertView.setTag(holder);

        holder.m_logTag = (TextView) convertView.findViewById(R.id.list_item_log_tag);
        holder.m_logLevel = (TextView) convertView.findViewById(R.id.list_item_setting_log_level);
      } else {
        holder = (SettingItemHolder) convertView.getTag();
      }

      LogcatSettingItem item = getItem(position);
      holder.m_logTag.setText(item.getLogTag());
      holder.m_logLevel.setText(item.getLogLevel());

      return convertView;
    }

    private static class SettingItemHolder {
      private TextView m_logTag;
      private TextView m_logLevel;
    }

    private final LayoutInflater m_layoutInflater;
  }

  /**
   * Convenient dialog fragment that prompts for the log level value
   * to reset all tags to.
   */
  public static class ResetLogLevelDialog extends DialogFragment {

    public static ResetLogLevelDialog newInstance(String dialogTitle) {
      return newInstance(dialogTitle, -1);
    }

    public static ResetLogLevelDialog newInstance(String dialogTitle, int listPosition) {
      final Bundle args = new Bundle();
      args.putSerializable(BUNDLE_KEY_DIALOG_TITLE, dialogTitle);

      if (listPosition != -1) {
        args.putSerializable(BUNDLE_KEY_DIALOG_LIST_POSITION, listPosition);
      }

      final ResetLogLevelDialog fragment = new ResetLogLevelDialog();
      fragment.setArguments(args);

      return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      final String [] logLevelValues
          = getResources().getStringArray(R.array.reset_log_level_values);

      final String dialogTitle = getArguments().getString(BUNDLE_KEY_DIALOG_TITLE);

      return new AlertDialog.Builder(getActivity())
                            .setTitle(dialogTitle)
                            .setItems(
                                logLevelValues,
                                new DialogInterface.OnClickListener() {
                                  @Override
                                  public void onClick(DialogInterface dialog, int which) {
                                    sendResult(Activity.RESULT_OK, logLevelValues[which]);
                                  }
                                })
                            .create();
    }

    /**
     * Convenient method to send data back to the fragment that presents this
     * dialog.
     *
     * @param resultCode Result code to be passed back
     * @param logLevelValue Log level value to be passed back
     */
    private void sendResult(int resultCode, String logLevelValue) {
      if (getTargetFragment() == null) {
        return;
      }

      // Create intent
      Intent intent = new Intent();
      intent.putExtra(EXTRA_RESET_LOG_LEVEL_VALUE, logLevelValue);

      // Fill item position if present
      final int logItemPosition = getArguments().getInt(BUNDLE_KEY_DIALOG_LIST_POSITION, -1);
      if (logItemPosition != -1) {
        intent.putExtra(EXTRA_LOG_ITEM_LIST_POSITION, logItemPosition);
      }

      // Send results
      getTargetFragment().onActivityResult(getTargetRequestCode(), resultCode, intent);
    }

    /** Unique extra name to be used */
    public static final String EXTRA_RESET_LOG_LEVEL_VALUE
        = "net.named_data.nfd.reset_log_level_value";

    public static final String EXTRA_LOG_ITEM_LIST_POSITION
        = "net.named_data.nfd.log_item_list_position";

    private static final String BUNDLE_KEY_DIALOG_TITLE
        = "BUNDLE_KEY_DIALOG_TITLE";

    private static final String BUNDLE_KEY_DIALOG_LIST_POSITION
        = "BUNDLE_KEY_DIALOG_LIST_POSITION";
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Array list that contains all the tags that are logged */
  private ArrayList<LogcatSettingItem> m_logcatSettingItems;

  /** Reference to the currently used LogcatSettingsManager */
  private LogcatSettingsManager m_logcatSettingsManager;

  /** Request code for dialog that gets the new log level for all tags */
  private static final int REQUEST_CODE_DIALOG_RESET_ALL_LOG_LEVELS = 1;

  /** Request code for dialog that gets the new log level for a single tag */
  private static final int REQUEST_CODE_DIALOG_SET_LOG_LEVEL = 2;

  /** Unique tag that identifies dialog fragment in the fragment manager */
  private static final String DIALOG_RESET_ALL_LOG_LEVELS_TAG = "ResetAllLogLevelDialog";

  /** Unique tag that identifies dialog fragment in the fragment manager */
  private static final String DIALOG_SET_LOG_LEVEL_TAG = "SetLogLevelDialog";

  private ArrayAdapter<LogcatSettingItem> m_adapter;
}
