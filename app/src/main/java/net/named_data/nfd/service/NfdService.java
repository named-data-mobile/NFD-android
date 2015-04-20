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

package net.named_data.nfd.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import net.named_data.nfd.utils.G;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * NfdService that runs the native NFD.
 *
 * NfdService runs as an independent process within the Android OS that provides
 * service level features to start and stop the NFD native code through the
 * NFD JNI wrapper.
 *
 */
public class NfdService extends Service {
  /**
   * Loading of NFD Native libraries.
   */
  static {
    // At least on Galaxy S3 (4.1.1), all shared library dependencies that are located
    // in app's lib folder (not in /system/lib) need to be explicitly loaded.
    // The script https://gist.github.com/cawka/11fe9c23b7a13960330b can be used to
    // calculate proper dependency load list.
    // For example:
    //     cd app/src/main/libs/armeabi-v7a/
    //     bash android-shared-lib-dependencies.sh nfd-wrapper
    System.loadLibrary("crystax");
    System.loadLibrary("gnustl_shared");
    System.loadLibrary("cryptopp");
    System.loadLibrary("boost_system");
    System.loadLibrary("boost_filesystem");
    System.loadLibrary("boost_date_time");
    System.loadLibrary("boost_iostreams");
    System.loadLibrary("boost_program_options");
    System.loadLibrary("boost_chrono");
    System.loadLibrary("boost_random");
    System.loadLibrary("ndn-cxx");
    System.loadLibrary("boost_thread");
    System.loadLibrary("nfd-daemon");
    System.loadLibrary("nfd-wrapper");
  }

  /**
   * Native API for starting the NFD.
   *
   * @param params NFD parameters.  Must include 'homePath' with absolute path of the home directory
   *               for the service (ContextWrapper.getFilesDir().getAbsolutePath())
   */
  public native static void
  startNfd(Map<String, String> params);

  /**
   * Native API for stopping the NFD.
   */
  public native static void
  stopNfd();

  public native static List<String>
  getNfdLogModules();

  /** Message to start NFD Service */
  public static final int START_NFD_SERVICE = 1;

  /** Message to stop NFD Service */
  public static final int STOP_NFD_SERVICE = 2;

  /** Message to indicate that NFD Service is running */
  public static final int NFD_SERVICE_RUNNING = 3;

  /** Message to indicate that NFD Service is not running */
  public static final int NFD_SERVICE_STOPPED = 4;


  @Override
  public void onCreate() {
    G.Log("NFDService::onCreate()");
    m_nfdServiceMessenger = new Messenger(new NfdServiceMessageHandler());
  }

  @Override
  public int
  onStartCommand(Intent intent, int flags, int startId) {
    G.Log("NFDService::onStartCommand()");

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
  public IBinder
  onBind(Intent intent) {
    return m_nfdServiceMessenger.getBinder();
  }

  @Override
  public void
  onDestroy() {
    G.Log("NFDService::onDestroy()");

    serviceStopNfd();
    m_nfdServiceMessenger = null;
  }

  /////////////////////////////////////////////////////////////////////////////

  /**
   * Thread safe way of starting the NFD and updating the
   * started flag.
   */
  private synchronized void
  serviceStartNfd() {
    if (!m_isNfdStarted) {
      m_isNfdStarted = true;
      HashMap<String, String> params = new HashMap<>();
      params.put("homePath", getFilesDir().getAbsolutePath());
      Set<Map.Entry<String,String>> e = params.entrySet();

      startNfd(params);

      // Example how to retrieve all available NFD log modules
      List<String> modules = getNfdLogModules();
      for (String module : modules) {
        G.Log(module);
      }

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
   * Thread safe way of stopping the NFD and updating the
   * started flag.
   */
  private synchronized void
  serviceStopNfd() {
    if (m_isNfdStarted) {
      m_isNfdStarted = false;

      // TODO: Save NFD and NRD in memory data structures.
      stopNfd();

      stopSelf();
      G.Log("serviceStopNfd()");
    }
  }

  /**
   * Message handler for the the NFD Service.
   */
  private class NfdServiceMessageHandler extends Handler {

    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
      case NfdService.START_NFD_SERVICE:
        serviceStartNfd();
        replyToClient(message, NfdService.NFD_SERVICE_RUNNING);
        break;

      case NfdService.STOP_NFD_SERVICE:
        serviceStopNfd();
        replyToClient(message, NfdService.NFD_SERVICE_STOPPED);
        break;

      default:
        super.handleMessage(message);
        break;
      }
    }

    private void
    replyToClient(Message message, int replyMessage) {
      try {
        message.replyTo.send(Message.obtain(null, replyMessage));
      } catch (RemoteException e) {
        // Nothing to do here; It means that client end has been terminated.
      }
    }
  }

  /** Messenger to handle messages that are passed to the NfdService */
  private Messenger m_nfdServiceMessenger = null;

  /** Flag that denotes if the NFD has been started */
  private boolean m_isNfdStarted = false;
}
