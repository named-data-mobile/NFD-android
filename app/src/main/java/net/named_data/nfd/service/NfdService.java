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

package net.named_data.nfd.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import net.named_data.nfd.G;

/**
 * @brief NfdService that runs the native NFD.
 *
 * NfdSevice runs as an independent process within the Android OS that provides
 * service level features to start and stop the NFD native code through the
 * NFD JNI wrapper.
 *
 */
public class NfdService extends Service {

  /**
   * @brief Loading of NFD Native libraries.
   */
  static {
    System.loadLibrary("nfd-wrapper");
  }

  /**
   * @brief Native API for starting the NFD.
   *
   * @param homePath Absolute path of the home directory for the service;
   *                 Usually achieved by calling ContextWrapper.getFilesDir().getAbsolutePath()
   */
  public native static void
  startNfd(String homePath);

  /**
   * @brief Native API for stopping the NFD.
   */
  public native static void
  stopNfd();

  /** Message to start NFD Service */
  public static final int MESSAGE_START_NFD_SERVICE = 1;

  /** Message to stop NFD Service */
  public static final int MESSAGE_STOP_NFD_SERVICE = 2;

  /** Message to query if NFD is running */
  public static final int MESSAGE_IS_NFD_RUNNING = 3;

  /** Message to indicate NFD is running */
  public static final int MESSAGE_NFD_RUNNING = 4;

  /** Message to indicate NFD is stopped */
  public static final int MESSAGE_NFD_STOPPED = 5;

  @Override
  public void onCreate() {
    G.Log("NFDService::onCreate()");
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    G.Log("NFDService::onStartCommand()");

    // If we need to handle per-client start invocations, they are to be
    // handled here.

    // Start NFD
    serviceStartNfd();

    // Service is restarted when killed.
    // Pending intents delivered; null intent redelivered otherwise.
    return START_STICKY;
  }

  /**
   * When clients bind to the NfdService, an IBinder interface to the
   * NFD Service Messenger is returned for clients to send messages
   * to the NFD Service.
   *
   * @param intent Intent as sent by the client.
   * @return IBinder interface to send messages to the NFD Service.
   */
  @Override
  public IBinder onBind(Intent intent) {
    return m_nfdServiceMessenger.getBinder();
  }

  @Override
  public void onDestroy() {
    G.Log("NFDService::onDestroy()");

    if (m_isNfdStarted) {
      G.Log("Stopping NFD ...");
      serviceStopNfd();
    }
  }

  /**
   * @brief Thread safe way of starting the NFD and updating the
   * started flag.
   */
  private synchronized void serviceStartNfd() {
    if (m_isNfdStarted == false) {
      m_isNfdStarted = true;
      startNfd(getFilesDir().getAbsolutePath());

      // TODO: Reload NFD and NRD in memory structures (if any)

      // Keep Service alive; In event when service is started
      // from a Handler's message through binding with the service.
      startService(new Intent(this, NfdService.class));

      G.Log("serviceStartNfd()");
    } else {
      G.Log("serviceStartNfd(): NFD Service already running!");
    }
  }

  /**
   * @brief Thread safe way of stopping the NFD and updating the
   * started flag.
   */
  private synchronized void serviceStopNfd() {
    if (m_isNfdStarted == true) {
      m_isNfdStarted = false;

      // TODO: Save NFD and NRD in memory data structures.
      stopNfd();

      stopSelf();
      G.Log("serviceStopNfd()");
    }
  }

  /**
   * @brief Thread safe way of checking if the the NFD is running.
   *
   * @return true if NFD is running; false otherwise.
   */
  private synchronized boolean isNfdRunning() {
    return m_isNfdStarted;
  }

  /**
   * @brief Message handler for the the NFD Service.
   */
  class NfdServiceMessageHandler extends Handler {

    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
      case NfdService.MESSAGE_START_NFD_SERVICE:
        serviceStartNfd();
        replyToClient(message, NfdService.MESSAGE_NFD_RUNNING);
        break;

      case NfdService.MESSAGE_STOP_NFD_SERVICE:
        serviceStopNfd();
        replyToClient(message, NfdService.MESSAGE_NFD_STOPPED);
        break;

      case NfdService.MESSAGE_IS_NFD_RUNNING:
        int replyMessage = isNfdRunning() ?
          NfdService.MESSAGE_NFD_RUNNING :
          NfdService.MESSAGE_NFD_STOPPED;

        replyToClient(message, replyMessage);
        break;

      default:
        super.handleMessage(message);
        break;
      }
    }

    private void replyToClient(Message message, int replyMessage) {
      try {
        message.replyTo.send(Message.obtain(null, replyMessage));
      } catch (RemoteException e) {
        // Nothing to do here; It means that client end has been terminated.
      }
    }
  }

  /** Messenger to handle messages that are passed to the NfdService */
  private final Messenger m_nfdServiceMessenger
    = new Messenger(new NfdServiceMessageHandler());

  /** Flag that denotes if the NFD has been started */
  private boolean m_isNfdStarted = false;

}
