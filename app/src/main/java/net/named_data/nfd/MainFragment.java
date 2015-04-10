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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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

import net.named_data.nfd.service.NfdService;
import net.named_data.nfd.utils.G;

public class MainFragment extends Fragment {

  public static MainFragment newInstance() {
    // Create fragment arguments here (if necessary)
    return new MainFragment();
  }

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    m_handler = new Handler();
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState)
  {
    @SuppressLint("InflateParams")
    View v =  inflater.inflate(R.layout.fragment_main, null);

    m_nfdStartStopSwitch = (Switch)v.findViewById(R.id.nfd_start_stop_switch);
    m_nfdStartStopSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean isOn)
      {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.edit()
          .putBoolean(PREF_NFD_SERVICE_STATUS, isOn)
          .apply();

        if (isOn) {
          startNfdService();
        }
        else {
          stopNfdService();
        }
      }
    });

    return v;
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
    assert m_isNfdServiceConnected;

    m_nfdStartStopSwitch.setText(R.string.starting_nfd);
    sendNfdServiceMessage(NfdService.START_NFD_SERVICE);
  }

  private void
  stopNfdService() {
    assert m_isNfdServiceConnected;

    m_nfdStartStopSwitch.setText(R.string.stopping_nfd);
    sendNfdServiceMessage(NfdService.STOP_NFD_SERVICE);
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
   *
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
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        boolean shouldServiceBeOn = sp.getBoolean(PREF_NFD_SERVICE_STATUS, true);

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
   *
   * This method attempts to reconnect the application to the NfdService
   * when the NfdService has been killed (either by the user or by the OS).
   */
  private Runnable m_retryConnectionToNfdService = new Runnable() {
    @Override
    public void
    run()
    {
      G.Log("Retrying connection to NFD Service ...");
      bindNfdService();
    }
  };

  //////////////////////////////////////////////////////////////////////////////

  /** Button that starts and stops the NFD */
  private Switch m_nfdStartStopSwitch;

  /** Flag that marks that application is connected to the NfdService */
  private boolean m_isNfdServiceConnected = false;

  /** Client Message Handler */
  private final Messenger m_clientMessenger = new Messenger(new ClientHandler());

  /** Messenger connection to NfdService */
  private Messenger m_nfdServiceMessenger = null;

  private Handler m_handler;

  private static final String PREF_NFD_SERVICE_STATUS = "NFD_SERVICE_STATUS";
}
