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
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import net.named_data.nfd.service.NfdService;
import net.named_data.nfd.utils.G;

public class MainActivity
  extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

  @Override
  public void
  onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);

    getFragmentManager()
      .beginTransaction()
      .replace(android.R.id.content, new PreferenceFragment() {
        @Override
        public void onCreate(Bundle savedInstanceState)
        {
          super.onCreate(savedInstanceState);
          addPreferencesFromResource(R.xml.pref_general);

          m_startStopPref = findPreference("start_stop");
        }
      })
      .commit();
  }

  @Override
  protected void
  onPostCreate(Bundle savedInstanceState)
  {
    super.onPostCreate(savedInstanceState);

    m_startStopPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener()
    {
      @Override
      public boolean onPreferenceClick(Preference preference)
      {
        toggleNfdState();
        return true;
      }
    });
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Bind to NfdService
    bindNfdService();
  }

  @Override
  protected void onPause() {
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
      m_startStopPref.setTitle(R.string.stopping_nfd);
      sendNfdServiceMessage(NfdService.MESSAGE_STOP_NFD_SERVICE);
    } else {
      m_startStopPref.setTitle(R.string.starting_nfd);
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
   * Enable UI Button once critical operations are completed.
   */
  private void enableNfdButton() {
    m_startStopPref.setEnabled(true);
  }

  /**
   * Disable UI Button to ensure user is unable to hit the button mutiple times.
   */
  private void disableNfdButton() {
    m_startStopPref.setEnabled(false);
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
   * Toggle UI Button text to inform user of the next possible action.
   *
   * @param isNfdRunning true if NFD is currently running; false otherwise
   */
  private void setNfdButtonText(boolean isNfdRunning) {
    m_startStopPref.setTitle(isNfdRunning ? R.string.stop_nfd : R.string.start_nfd);
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
    if (m_isNfdServiceBound == false) {
      // Bind to Service
      m_isNfdServiceBound = this.bindService(
        new Intent(this, NfdService.class),
        m_ServiceConnection, Context.BIND_AUTO_CREATE);

      G.Log("MainActivity::bindNfdService()");
    }
  }

  /**
   * Method that unbinds the current activity from the NfdService.
   */
  private synchronized void unbindNfdService() {
    if (m_isNfdServiceBound == true) {
      // Unbind from Service
      this.unbindService(m_ServiceConnection);
      m_isNfdServiceBound = false;

      G.Log("MainActivity::unbindNfdService()");
    }
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object newValue)
  {
    return false;
  }

  /**
   * Client Message Handler.
   *
   * This handler is used to handle messages that are being sent back
   * from the NfdService to the current application.
   */
  class ClientHandler extends Handler
  {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
      case NfdService.MESSAGE_NFD_RUNNING:
        setNfdRunningState(true);
        setNfdButtonText(true);
        G.Log("ClientHandler: NFD is Running.");
        break;

      case NfdService.MESSAGE_NFD_STOPPED:
        setNfdRunningState(false);
        setNfdButtonText(false);
        G.Log("ClientHandler: NFD is Stopped.");
        break;

      default:
        super.handleMessage(msg);
        break;
      }

      enableNfdButton();
    }
  }

  /**
   * Client ServiceConnection to NfdService.
   */
  private ServiceConnection m_ServiceConnection = new ServiceConnection() {
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
      disableNfdButton();
      m_startStopPref.setTitle(R.string.reconnect_to_nfd);

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
        while (m_isNfdServiceConnected == false) {
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
  private Preference m_startStopPref;

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
