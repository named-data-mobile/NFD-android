/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/*
 * Copyright (c) 2015-2019 Regents of the University of California
 * <p/>
 * This file is part of NFD (Named Data Networking Forwarding Daemon) Android.
 * See AUTHORS.md for complete list of NFD Android authors and contributors.
 * <p/>
 * NFD Android is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p/>
 * NFD Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License along with
 * NFD Android, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.named_data.nfd;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import net.named_data.jndn.NetworkNack;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PingClientFragment extends Fragment implements PingClient.PingClientListener {

  private PingClient m_client;

  /**
   *  View holder object for holding the output.
   */
  private static class PingResultEntryViewHolder {
    public TextView pingResultTextView;
  }

  /** ListView for displaying ping output in */
  private ListView m_pingResultListView;

  private EditText m_pingNameEditText;

  private Button m_pingStartButton;
  private boolean m_isStartState = true;

  /** Customized ListAdapter for controlling ping output */
  private PingResultListAdapter m_pingResultListAdapter;

  private final String TAG_PING_STATUS = "PingStatus";
  private final String TAG_PING_NAME = "PingName";
  private final String TAG_PING_DATA = "PingData";
  private final String TAG_PING_STATE = "PingState";

  /////////////////

  public static PingClientFragment newInstance() {
    return new PingClientFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View v = inflater.inflate(R.layout.fragment_ping_client, container, false);

    // Get UI Elements
    m_pingResultListView = (ListView) v.findViewById(R.id.pingResult);

    m_pingNameEditText = (EditText) v.findViewById(R.id.pingName);
    m_pingStartButton = (Button) v.findViewById(R.id.pingStart);
    m_pingStartButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
          imm.hideSoftInputFromWindow(m_pingNameEditText.getWindowToken(), InputMethodManager.RESULT_UNCHANGED_SHOWN);
          if (m_isStartState) {
            setButtonState(false);
            String pingPrefix = m_pingNameEditText.getText().toString();

            m_pingResultListAdapter.clearMessages();
            m_pingResultListAdapter.addMessage("PING " + pingPrefix);
            m_pingResultListView.setSelection(m_pingResultListAdapter.getCount() - 1);

            m_client = new PingClient(pingPrefix);
            m_client.setListener(PingClientFragment.this);
            m_client.start();
          } else {
            setButtonState(true);
            if (m_client != null) {
              m_client.stop();
            }
          }
        }
      });

    return v;
  }

  private void setButtonState(boolean isStartState)
  {
    if (isStartState) {
      m_pingStartButton.setText(R.string.start);
    }
    else {
      m_pingStartButton.setText(R.string.stop);
    }
    m_isStartState = isStartState;
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState)
  {
    super.onActivityCreated(savedInstanceState);
    if (m_pingResultListAdapter == null) {
      m_pingResultListAdapter = new PingResultListAdapter(getActivity());
    }
    m_pingResultListView.setAdapter(m_pingResultListAdapter);

    if (savedInstanceState != null) {
      setButtonState(savedInstanceState.getBoolean(TAG_PING_STATUS));
      m_pingNameEditText.setText(savedInstanceState.getString(TAG_PING_NAME));
      m_pingResultListAdapter.setData(savedInstanceState.getStringArrayList(TAG_PING_DATA));

      Serializable state = savedInstanceState.getSerializable(TAG_PING_STATE);
      if (!m_isStartState && state != null) {
        m_client = new PingClient(m_pingNameEditText.getText().toString(), state);
      }
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (m_client != null) {
      m_client.setListener(null);
      m_client.stop();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState)
  {
    super.onSaveInstanceState(outState);
    outState.putBoolean(TAG_PING_STATUS, m_isStartState);
    outState.putString(TAG_PING_NAME, m_pingNameEditText.getText().toString());
    outState.putStringArrayList(TAG_PING_DATA, m_pingResultListAdapter.m_data);
    if (!m_isStartState && m_client != null) {
      outState.putSerializable(TAG_PING_STATE, m_client.getState());
    }
  }

  @Override
  public void onPingResponse(final String prefix, final long seq, final double elapsedTime) {
    this.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          m_pingResultListAdapter.addMessage("content from " + prefix + ": seq=" + seq
                                             + " time=" + String.format(Locale.getDefault(), "%.3f", elapsedTime) + " ms");
          m_pingResultListView.setSelection(m_pingResultListAdapter.getCount() - 1);
        }
      });
  }

  @Override
  public void onPingTimeout(final String prefix, final long seq) {
    this.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          m_pingResultListAdapter.addMessage("timeout from " + prefix + ": seq=" + seq);
          m_pingResultListView.setSelection(m_pingResultListAdapter.getCount() - 1);
        }
      });
  }

  @Override
  public void onPingNack(final String prefix, final long seq, final NetworkNack.Reason reason) {
    this.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          m_pingResultListAdapter.addMessage("NACK from " + prefix + ": seq=" + seq
                                             + " reason=" + reason);
          m_pingResultListView.setSelection(m_pingResultListAdapter.getCount() - 1);
        }
      });
  }

  @Override
  public void onCalcStatistics(final String msg) {
    this.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          m_pingResultListAdapter.addMessage(msg);
          m_pingResultListView.setSelection(m_pingResultListAdapter.getCount() - 1);
        }
      });
  }

  @Override
  public void onPingFinish() {
    this.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          m_pingStartButton.setTag(null);
          m_pingStartButton.setText(R.string.start);
        }
      });
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  private class PingResultListAdapter extends BaseAdapter {

    /**
     * Create a ListView compatible adapter with an
     * upper bound on the maximum number of entries that will
     * be displayed in the ListView.
     */
    PingResultListAdapter(Context context) {
      m_data = new ArrayList<>();
      m_layoutInflater = LayoutInflater.from(context);
    }

    /**
     * Add a message to be displayed in the ping result's list view.
     *
     * @param message Message to be added to the underlying data store
     *                and displayed on thi UI.
     */
    void addMessage(String message) {
      m_data.add(message);
      notifyDataSetChanged();
    }

    /**
     * Convenience method to clear all messages from the underlying
     * data store and update the UI.
     */
    void clearMessages() {
      m_data.clear();
      this.notifyDataSetChanged();
    }

    void setData(ArrayList<String> data) {
      m_data.clear();
      m_data.addAll(data);
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      PingResultEntryViewHolder holder;

      if (convertView == null) {
        holder = new PingResultEntryViewHolder();
        convertView = m_layoutInflater.inflate(R.layout.list_item_ping_result, null);
        convertView.setTag(holder);
        holder.pingResultTextView = (TextView) convertView.findViewById(R.id.ping_result);
      } else {
        holder = (PingResultEntryViewHolder) convertView.getTag();
      }

      holder.pingResultTextView.setText(m_data.get(position));
      return convertView;
    }

    private final ArrayList<String> m_data;
    private final LayoutInflater m_layoutInflater;
  }
}
