/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015-2018 Regents of the University of California
 * <p>
 * This file is part of NFD (Named Data Networking Forwarding Daemon) Android.
 * See AUTHORS.md for complete list of NFD Android authors and contributors.
 * <p>
 * NFD Android is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p>
 * NFD Android is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * NFD Android, e.g., in COPYING.md file.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.named_data.nfd;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.intel.jndn.management.ManagementException;
import com.intel.jndn.management.types.ForwarderStatus;
import com.intel.jndn.management.types.RibEntry;

import net.named_data.jndn.Name;
import net.named_data.nfd.service.NfdService;
import net.named_data.nfd.utils.G;
import net.named_data.nfd.utils.NfdcHelper;
import net.named_data.nfd.utils.SharedPreferencesManager;
import net.named_data.nfd.wifidirect.utils.NDNController;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import java.util.List;

public class MainFragment extends Fragment {
  public static final String URI_UDP_PREFIX = "udp://";
  public static final String PREFIX_NDN = "/";
  public static final String PREFIX_LOCALHOP_NFD = "/localhop/nfd";

  public static MainFragment newInstance() {
    // Create fragment arguments here (if necessary)
    return new MainFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    m_handler = new Handler();
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    @SuppressLint("InflateParams")
    View v = inflater.inflate(R.layout.fragment_main, null);
    m_nfdStartStopSwitch = (Switch) v.findViewById(R.id.nfd_start_stop_switch);
    m_nfdStartStopSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
        m_sharedPreferences.edit()
          .putBoolean(PREF_NFD_SERVICE_STATUS, isOn)
          .apply();

        if (isOn) {
          startNfdService();
        } else {
          NDNController.getInstance().stop(); //stop wifi direct
          stopNfdService();
        }
      }
    });

    m_connectNearestHubSwitch = (Switch) v.findViewById(R.id.connect_nearest_hub_switch);
    if (SharedPreferencesManager.getConnectNearestHubAutomatically(getActivity().getApplicationContext())) {
      m_connectNearestHubSwitch.setChecked(true);
    }
    m_connectNearestHubSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
        SharedPreferencesManager.
          setConnectNearestHubAutomatically(getActivity().getApplicationContext(), isOn);
        if (isOn) {
          // when nfd service is running, connect NDN hub, otherwise, do nothing
          if(m_sharedPreferences.getBoolean(PREF_NFD_SERVICE_STATUS, true)) {
            connectNearestHub();
          }
        }
      }
    });

    m_nfdStatusView = (ViewGroup) v.findViewById(R.id.status_view);
    m_nfdStatusView.setVisibility(View.GONE);
    m_versionView = (TextView) v.findViewById(R.id.version);
    m_uptimeView = (TextView) v.findViewById(R.id.uptime);
    m_nameTreeEntriesView = (TextView) v.findViewById(R.id.name_tree_entries);
    m_fibEntriesView = (TextView) v.findViewById(R.id.fib_entries);
    m_pitEntriesView = (TextView) v.findViewById(R.id.pit_entries);
    m_measurementEntriesView = (TextView) v.findViewById(R.id.measurement_entries);
    m_csEntriesView = (TextView) v.findViewById(R.id.cs_entries);
    m_inInterestsView = (TextView) v.findViewById(R.id.in_interests);
    m_outInterestsView = (TextView) v.findViewById(R.id.out_interests);
    m_inDataView = (TextView) v.findViewById(R.id.in_data);
    m_outDataView = (TextView) v.findViewById(R.id.out_data);
    m_inNacksView = (TextView) v.findViewById(R.id.in_nacks);
    m_outNacksView = (TextView) v.findViewById(R.id.out_nacks);

    return v;
  }

  /**
   * when the user clicks "connect to the nearest hub automatically", create face and register prefix
   */
  private void connectNearestHub() {
    m_connectNearestHubAsyncTask = new ConnectNearestHubAsyncTask();
    m_connectNearestHubAsyncTask.execute();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    m_sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
  }

  @Override
  public void
  onResume() {
    super.onResume();

    bindNfdService();
  }

  @Override
  public void
  onPause() {
    super.onPause();

    unbindNfdService();
    m_handler.removeCallbacks(m_statusUpdateRunnable);
    m_handler.removeCallbacks(m_retryConnectionToNfdService);
  }

  /**
   * Method that binds the current activity to the NfdService.
   */
  private void
  bindNfdService() {
    if (!m_isNfdServiceConnected) {
      // Bind to Service
      getActivity().bindService(new Intent(getActivity(), NfdService.class),
        m_ServiceConnection, Context.BIND_AUTO_CREATE);
      G.Log("MainFragment::bindNfdService()");
    }
  }

  /**
   * Method that unbinds the current activity from the NfdService.
   */
  private void
  unbindNfdService() {
    if (m_isNfdServiceConnected) {
      // Unbind from Service
      getActivity().unbindService(m_ServiceConnection);
      m_isNfdServiceConnected = false;

      G.Log("MainFragment::unbindNfdService()");
    }
  }

  private void
  startNfdService() {
    if (BuildConfig.DEBUG && !m_isNfdServiceConnected)
      throw new RuntimeException("Service must be connected at this point");

    m_nfdStartStopSwitch.setText(R.string.starting_nfd);
    sendNfdServiceMessage(NfdService.START_NFD_SERVICE);
  }

  private void
  stopNfdService() {
    if (BuildConfig.DEBUG && !m_isNfdServiceConnected)
      throw new RuntimeException("Service must be connected at this point");

    m_nfdStartStopSwitch.setText(R.string.stopping_nfd);
    sendNfdServiceMessage(NfdService.STOP_NFD_SERVICE);

    // disable status block
    m_nfdStatusView.setVisibility(View.GONE);
    m_handler.removeCallbacks(m_statusUpdateRunnable);
  }

  /**
   * Convenience method to send a message to the NfdService
   * through a Messenger.
   *
   * @param message Message from a set of predefined NfdService messages.
   */
  private void
  sendNfdServiceMessage(int message) {
    if (m_nfdServiceMessenger == null) {
      G.Log("NfdService not yet connected");
      return;
    }
    try {
      Message msg = Message.obtain(null, message);
      msg.replyTo = m_clientMessenger;
      m_nfdServiceMessenger.send(msg);
    } catch (RemoteException e) {
      // If Service crashes, nothing to do here
      G.Log("Service Disconnected: " + e);
    }
  }

  private void
  setNfdServiceRunning() {
    m_nfdStartStopSwitch.setEnabled(true);
    m_nfdStartStopSwitch.setText(R.string.nfd_started);
    m_nfdStartStopSwitch.setChecked(true);
  }

  private void
  setNfdServiceStopped() {
    m_nfdStartStopSwitch.setEnabled(true);
    m_nfdStartStopSwitch.setText(R.string.nfd_stopped);
    m_nfdStartStopSwitch.setChecked(false);

  }

  private void
  setNfdServiceDisconnected() {
    m_nfdStartStopSwitch.setEnabled(false);
    m_nfdStartStopSwitch.setText(R.string.reconnect_to_nfd);
    m_nfdStartStopSwitch.setChecked(false);
  }

  /**
   * Client Message Handler.
   * <p>
   * This handler is used to handle messages that are being sent back
   * from the NfdService to the current application.
   */
  private class ClientHandler extends Handler {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case NfdService.NFD_SERVICE_RUNNING:
          setNfdServiceRunning();
          G.Log("ClientHandler: NFD is Running.");

          m_handler.postDelayed(m_statusUpdateRunnable, 500);
          break;

        case NfdService.NFD_SERVICE_STOPPED:
          setNfdServiceStopped();
          G.Log("ClientHandler: NFD is Stopped.");
          break;

        default:
          super.handleMessage(msg);
          break;
      }
    }
  }

  /**
   * Client ServiceConnection to NfdService.
   */
  private final ServiceConnection m_ServiceConnection = new ServiceConnection() {
    @Override
    public void
    onServiceConnected(ComponentName className, IBinder service) {
      // Establish Messenger to the Service
      m_nfdServiceMessenger = new Messenger(service);
      m_isNfdServiceConnected = true; // onServiceConnected runs on the main thread

      // Check if NFD Service is running
      try {
        boolean shouldServiceBeOn = m_sharedPreferences.getBoolean(PREF_NFD_SERVICE_STATUS, true);

        Message msg = Message.obtain(null, shouldServiceBeOn ? NfdService.START_NFD_SERVICE : NfdService.STOP_NFD_SERVICE);
        msg.replyTo = m_clientMessenger;
        m_nfdServiceMessenger.send(msg);
      } catch (RemoteException e) {
        // If Service crashes, nothing to do here
        G.Log("onServiceConnected(): " + e);
      }

      G.Log("m_ServiceConnection::onServiceConnected()");
    }

    @Override
    public void
    onServiceDisconnected(ComponentName componentName) {
      // In event of unexpected disconnection with the Service; Not expecting to get here.
      G.Log("m_ServiceConnection::onServiceDisconnected()");

      // Update UI
      setNfdServiceDisconnected();

      m_isNfdServiceConnected = false; // onServiceDisconnected runs on the main thread
      m_handler.postDelayed(m_retryConnectionToNfdService, 1000);
    }
  };

  /**
   * Attempt to reconnect to the NfdService.
   * <p>
   * This method attempts to reconnect the application to the NfdService
   * when the NfdService has been killed (either by the user or by the OS).
   */
  private Runnable m_retryConnectionToNfdService = new Runnable() {
    @Override
    public void
    run() {
      G.Log("Retrying connection to NFD Service ...");
      bindNfdService();
    }
  };

  private class StatusUpdateTask extends AsyncTask<Void, Void, ForwarderStatus> {
    /**
     * @param voids
     * @return ForwarderStatus if operation succeeded, null if operation failed
     */
    @Override
    protected ForwarderStatus
    doInBackground(Void... voids) {
      try {
        NfdcHelper nfdcHelper = new NfdcHelper();
        ForwarderStatus fs = nfdcHelper.generalStatus();
        nfdcHelper.shutdown();
        return fs;
      } catch (Exception e) {
        G.Log("Error communicating with NFD (" + e.getMessage() + ")");
        return null;
      }
    }

    @Override
    protected void
    onPostExecute(ForwarderStatus fs) {
      if (fs == null) {
        // when failed, try after 0.5 seconds
        m_handler.postDelayed(m_statusUpdateRunnable, 500);
      } else {
        m_versionView.setText(fs.getNfdVersion());
        m_uptimeView.setText(PeriodFormat.getDefault().print(new Period(
          fs.getCurrentTimestamp() - fs.getStartTimestamp())));
        m_nameTreeEntriesView.setText(String.valueOf(
          fs.getNNameTreeEntries()));
        m_fibEntriesView.setText(String.valueOf(fs.getNFibEntries()));
        m_pitEntriesView.setText(String.valueOf(fs.getNPitEntries()));
        m_measurementEntriesView.setText(String.valueOf(
          fs.getNMeasurementsEntries()));
        m_csEntriesView.setText(String.valueOf(fs.getNCsEntries()));

        m_inInterestsView.setText(String.valueOf(fs.getNInInterests()));
        m_outInterestsView.setText(String.valueOf(fs.getNOutInterests()));

        m_inDataView.setText(String.valueOf(fs.getNInData()));
        m_outDataView.setText(String.valueOf(fs.getNOutData()));

        m_inNacksView.setText(String.valueOf(fs.getNInNacks()));
        m_outNacksView.setText(String.valueOf(fs.getNOutNacks()));

        m_nfdStatusView.setVisibility(View.VISIBLE);

        // refresh after 5 seconds
        m_handler.postDelayed(m_statusUpdateRunnable, 5000);
      }
    }
  }


  private class ConnectNearestHubAsyncTask extends AsyncTask<Void, Void, String> {

    @Override
    protected String
    doInBackground(Void... params) {
      //check whether two prefixes exist or not
      boolean prefix_ndn_exist = false;
      boolean prefix_localhop_nfd_exist = false;
      NfdcHelper nfdcHelper = new NfdcHelper();
      try {
        List<RibEntry> ribEntries = nfdcHelper.ribList();
        for (RibEntry one : ribEntries) {
          if (one.getName().toUri().equals(PREFIX_NDN)) {
            prefix_ndn_exist = true;
          }

          if (one.getName().toUri().equals(PREFIX_LOCALHOP_NFD)) {
            prefix_localhop_nfd_exist = true;
          }
        }
      } catch (ManagementException e) {
        G.Log("Error fetching RIB list from NFD (" + e.getMessage() + ")");
      } finally {
        nfdcHelper.shutdown();
      }
      if (prefix_ndn_exist && prefix_localhop_nfd_exist)
        return "";
      final boolean prefix_ndn_exist_inner = prefix_ndn_exist;
      final boolean prefix_localhop_nfd_exist_inner = prefix_localhop_nfd_exist;
      //register prefixes if they don't exist
      RequestQueue queue = Volley.newRequestQueue(getContext());
      StringRequest stringRequest = new StringRequest(Request.Method.GET,
        getResources().getString(R.string.ndn_fch_website),
        new Response.Listener<String>() {
          @Override
          public void onResponse(String response) {
            if (!prefix_ndn_exist_inner)
              new RouteCreateToConnectNearestHubAsyncTask(
                new Name(PREFIX_NDN), URI_UDP_PREFIX + response).execute();
            if (!prefix_localhop_nfd_exist_inner)
              new RouteCreateToConnectNearestHubAsyncTask(
                new Name(PREFIX_LOCALHOP_NFD), URI_UDP_PREFIX + response).execute();
          }
        },
        new Response.ErrorListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            String toastString = getResources().getString(R.string.fragment_route_list_toast_cannot_connect_hub);
            Toast.makeText(getActivity(), toastString, Toast.LENGTH_LONG).show();
          }
        });
      // Add the request to the RequestQueue.
      queue.add(stringRequest);
      return "";
    }
  }

  private class RouteCreateToConnectNearestHubAsyncTask extends AsyncTask<Void, Void, String> {
    RouteCreateToConnectNearestHubAsyncTask(Name prefix, String faceUri) {
      m_prefix = prefix;
      m_faceUri = faceUri;
    }

    @Override
    protected String
    doInBackground(Void... params) {
      NfdcHelper nfdcHelper = new NfdcHelper();
      try {
        G.Log("Try to create route to connect the nearest hub");
        int faceId = nfdcHelper.faceCreate(m_faceUri);
        nfdcHelper.ribRegisterPrefix(m_prefix, faceId, 10, true, false);
        G.Log("Create permanent route" + m_prefix + " - " + m_faceUri);
      } catch (Exception e) {
        G.Log("Error in RouteCreateToConnectNearestHubAsyncTask: " + e.getMessage());
      } finally {
        nfdcHelper.shutdown();
      }
      return null;
    }

    private Name m_prefix;
    private String m_faceUri;
  }

  //////////////////////////////////////////////////////////////////////////////

  /**
   * Button that starts and stops the NFD
   */
  private Switch m_nfdStartStopSwitch;

  /**
   * Button that starts and stops the auto configuration
   */
  private Switch m_connectNearestHubSwitch;

  /**
   * Flag that marks that application is connected to the NfdService
   */
  private boolean m_isNfdServiceConnected = false;

  private ConnectNearestHubAsyncTask m_connectNearestHubAsyncTask;

  /**
   * Client Message Handler
   */
  private final Messenger m_clientMessenger = new Messenger(new ClientHandler());

  /**
   * Messenger connection to NfdService
   */
  private Messenger m_nfdServiceMessenger = null;

  /**
   * ListView holding NFD status information
   */
  private ViewGroup m_nfdStatusView;

  private TextView m_versionView;
  private TextView m_uptimeView;
  private TextView m_nameTreeEntriesView;
  private TextView m_fibEntriesView;
  private TextView m_pitEntriesView;
  private TextView m_measurementEntriesView;
  private TextView m_csEntriesView;
  private TextView m_inInterestsView;
  private TextView m_outInterestsView;
  private TextView m_inDataView;
  private TextView m_outDataView;
  private TextView m_inNacksView;
  private TextView m_outNacksView;

  private Handler m_handler;
  private Runnable m_statusUpdateRunnable = new Runnable() {
    @Override
    public void run() {
      new StatusUpdateTask().execute();
    }
  };

  private SharedPreferences m_sharedPreferences;

  private static final String PREF_NFD_SERVICE_STATUS = "NFD_SERVICE_STATUS";

  private static final String CONNECT_NEAREST_HUB_STATUS = "CONNECT_NEAREST_HUB_STATUS";
}