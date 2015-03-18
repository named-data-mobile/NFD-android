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
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import net.named_data.nfd.service.NfdService;
import net.named_data.nfd.utils.G;

public class MainFragment extends Fragment {

  public static MainFragment newInstance() {
    // Create fragment arguments here (if necessary)
    return new MainFragment();
  }

  @Override
  public View onCreateView(LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState)
  {
    @SuppressLint("InflateParams")
    View v =  inflater.inflate(R.layout.fragment_main, null);

    m_nfdStartStopSwitch = (Switch) v.findViewById(R.id.nfd_start_stop_switch);
    m_nfdStartStopSwitch.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        toggleNfdState();
      }
    });

    return v;
  }

  @Override
  public void onResume () {
    super.onResume();

    // Bind to NfdService
    bindNfdService();
  }

  @Override
  public void onPause () {
    super.onPause();

    // Unbind from NfdService
    unbindNfdService();
  }

  /**
   * Thread safe way to start and stop the NFD through
   * the UI Button.
   */
  private synchronized void toggleNfdState() {
    if (m_isNfdRunning) {
      m_nfdStartStopSwitch.setText(R.string.stopping_nfd);
      sendNfdServiceMessage(NfdService.MESSAGE_STOP_NFD_SERVICE);
    } else {
      m_nfdStartStopSwitch.setText(R.string.starting_nfd);
      sendNfdServiceMessage(NfdService.MESSAGE_START_NFD_SERVICE);
    }
  }

  /**
   * Convenience method to send a message to the NfdService
   * through a Messenger.
   *
   * @param message Message from a set of predefined NfdService messages.
   */
  private synchronized void sendNfdServiceMessage(int message) {
    try {
      Message msg = Message.obtain(null, message);
      msg.replyTo = m_clientMessenger;
      m_nfdServiceMessenger.send(msg);
    } catch (RemoteException e) {
      // If Service crashes, nothing to do here
      G.Log("Service Disconnected: " + e);
    }
  }

  /**
   * Enable UI Switch once critical operations are completed.
   */
  private void enableNfdSwitch() {
    m_nfdStartStopSwitch.setEnabled(true);
  }

  /**
   * Disable UI Switch to ensure user is unable to hit the switch multiple times.
   */
  private void disableNfdSwitch() {
    m_nfdStartStopSwitch.setEnabled(false);
  }

  /**
   * Thread safe way of flagging that the NFD is running.
   *
   * @param isNfdRunning true if NFD is running; false otherwise
   */
  private synchronized void setNfdRunningState(boolean isNfdRunning) {
    m_isNfdRunning = isNfdRunning;
  }

  /**
   * Toggle UI Switch to inform user of the next possible action.
   *
   * @param isNfdRunning true if NFD is currently running; false otherwise
   */
  private void setNfdSwitchState(boolean isNfdRunning) {
    m_nfdStartStopSwitch.setText(isNfdRunning ? R.string.nfd_started : R.string.nfd_stopped);
    m_nfdStartStopSwitch.setChecked(isNfdRunning);
  }

  /**
   * Update UI Switch to inform user that the NFD Service has been disconnected
   * and an attempt is made to reconnect with the NFD Service.
   */
  private void setNfdDisconnectedSwitchState() {
    disableNfdSwitch();
    m_nfdStartStopSwitch.setText(R.string.reconnect_to_nfd);
    m_nfdStartStopSwitch.setChecked(false);
  }

  /**
   * Thread safe way of flagging that application is successfully connected
   * to the NfdService.
   *
   * @param isNfdServiceConnected true if successfully connected to the NfdService;
   *                              false otherwise
   */
  private synchronized void setNfdServiceConnected(boolean isNfdServiceConnected) {
    m_isNfdServiceConnected = isNfdServiceConnected;
  }

  /**
   * Method that binds the current activity to the NfdService.
   */
  private synchronized void bindNfdService() {
    if (!m_isNfdServiceBound) {
      // Bind to Service
      m_isNfdServiceBound = getActivity().bindService(
          new Intent(getActivity(), NfdService.class),
          m_ServiceConnection, Context.BIND_AUTO_CREATE);

      G.Log("MainFragment::bindNfdService()");
    }
  }

  /**
   * Method that unbinds the current activity from the NfdService.
   */
  private synchronized void unbindNfdService() {
    if (m_isNfdServiceBound) {
      // Unbind from Service
      getActivity().unbindService(m_ServiceConnection);
      m_isNfdServiceBound = false;

      G.Log("MainFragment::unbindNfdService()");
    }
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
        case NfdService.MESSAGE_NFD_RUNNING:
          setNfdRunningState(true);
          setNfdSwitchState(true);
          G.Log("ClientHandler: NFD is Running.");
          break;

        case NfdService.MESSAGE_NFD_STOPPED:
          setNfdRunningState(false);
          setNfdSwitchState(false);
          G.Log("ClientHandler: NFD is Stopped.");
          break;

        default:
          super.handleMessage(msg);
          break;
      }

      enableNfdSwitch();
    }
  }

  /**
   * Client ServiceConnection to NfdService.
   */
  private final ServiceConnection m_ServiceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
      // Establish Messenger to the Service
      m_nfdServiceMessenger = new Messenger(service);

      // Set service connected flag
      setNfdServiceConnected(true);

      // Check if NFD Service is running
      try {
        Message msg = Message.obtain(null,
            NfdService.MESSAGE_IS_NFD_RUNNING);
        msg.replyTo = m_clientMessenger;
        m_nfdServiceMessenger.send(msg);
      } catch (RemoteException e) {
        // If Service crashes, nothing to do here
        G.Log("onServiceConnected(): " + e);
      }

      G.Log("m_ServiceConnection::onServiceConnected()");
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
      // In event of unexpected disconnection with the Service; Not expecting to get here.
      G.Log("m_ServiceConnection::onServiceDisconnected()");

      // Update UI
      setNfdDisconnectedSwitchState();

      // Reconnect to NfdService
      setNfdServiceConnected(false);
      retryConnectionToNfdService();
    }
  };

  /**
   * Attempt to reconnect to the NfdService.
   *
   * This method attempts to reconnect the application to the NfdService
   * when the NfdService has been killed (either by the user or by the OS).
   */
  private void retryConnectionToNfdService() {
    new Thread(){
      @Override
      public void run() {
        // TODO: Trying infinitely doesn't make sense.
        // Convert this to an AsyncTask that:
        //    - has a fixed number of retries
        //    - update UI to inform user of the progress
        //    - set switch to appropriate state when service fails to come online
        while (!m_isNfdServiceConnected) {
          G.Log("Retrying connection to NFD Service ...");
          bindNfdService();

          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            // Nothing to do here; Keep going.
          }
        }

        G.Log("Reconnection to NFD Service is successful.");
      }
    }.start();
  }

  //////////////////////////////////////////////////////////////////////////////

  /** Button that starts and stops the NFD */
  private Switch m_nfdStartStopSwitch;

  /** Flag that marks that application is bound to the NfdService */
  private boolean m_isNfdServiceBound = false;

  /** Flag that marks that application is connected to the NfdService */
  private boolean m_isNfdServiceConnected = false;

  /** Client Message Handler */
  private final Messenger m_clientMessenger = new Messenger(new ClientHandler());

  /** Messenger connection to NfdService */
  private Messenger m_nfdServiceMessenger = null;

  /** Flag that marks if the NFD is running */
  private boolean m_isNfdRunning = false;
}
