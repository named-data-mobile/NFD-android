/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015-2017 Regents of the University of California
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

package net.named_data.nfd.wifidirect.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_DISABLED;
import static android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_STATE_ENABLED;

/**
 * WiFi Direct Broadcast receiver. Does not deviate too much
 * from the standard WiFi Direct broadcast receiver seen in the official
 * android docs.
 */

public class WDBroadcastReceiver extends BroadcastReceiver {

  private static final String TAG = "WDBroadcastReceiver";

  // WifiP2p
  private WifiP2pManager mManager;
  private WifiP2pManager.Channel mChannel;

  // the controller
  private NDNController mController;

  public WDBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel) {
    super();

    this.mManager = manager;
    this.mChannel = channel;
    this.mController = NDNController.getInstance();
  }

  @Override
  public void onReceive(Context context, Intent intent) {
    final String action = intent.getAction();

    if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
      // Check to see if Wi-Fi is enabled and notify appropriate activity

      Log.d(TAG, "wifi enabled check");
      int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
      if (state == WIFI_P2P_STATE_ENABLED) {
        // Wifi P2P is enabled
        Log.d(TAG, "WIFI IS ENABLED");
        mController.WIFI_STATE = WIFI_P2P_STATE_ENABLED;
        mController.startRunnables();
      } else {
        // Wi-Fi P2P is not enabled
        Log.d(TAG, "WIFI IS NOT ENABLED");
        mController.WIFI_STATE = WIFI_P2P_STATE_DISABLED;
        mController.stopRunnables();
        mController.cleanUpConnections();
        mController.recreateFace();
      }

    } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
      // Call WifiP2pManager.requestPeers() to get a list of current peers

      Log.d(TAG, "peers changed!");

      // request available peers from the wifi p2p manager. This is an
      // asynchronous call and the calling activity is notified with a
      // callback on PeerListListener.onPeersAvailable()
      if (mManager != null) {
        mManager.requestPeers(mChannel, mController);
      }

    } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
      // Respond to new connection or disconnections

      Log.d(TAG, "p2pconnection changed check");
      if (mManager == null) {
        Log.d(TAG, "mManager is null, skipping...");
        return;
      }

      NetworkInfo networkInfo = (NetworkInfo) intent
        .getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

      if (networkInfo.isConnected()) {
        // We are connected with the other device, request connection
        // info to find group owner IP
        mManager.requestConnectionInfo(mChannel, mController);
      } else {
        // Sometimes the framework sends a disconnect event when connecting to a new peer
        Log.d(TAG, "Received a disconnect notification!");
        mController.cleanUpConnections();
        mController.recreateFace();
      }

    } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
      // TODO: Respond to this device's state changing
      Log.d(TAG, "wifi state changed check");
      WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
      NDNController.myDeviceName = device.deviceName;
    } else if (WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION.equals(action)) {
      // if discovery (scanning) has either stopped or resumed
      switch (intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)) {
        case WifiP2pManager.WIFI_P2P_DISCOVERY_STARTED:
          Log.d(TAG, "Wifip2p discovery started.");
          break;
        case WifiP2pManager.WIFI_P2P_DISCOVERY_STOPPED:
          Log.d(TAG, "Wifipsp discovery stopped.");
          break;
        default:
          Log.d(TAG, "WIFI_P2P_DISCOVERY_CHANGED_ACTION returned other reason.");
      }
    }
  }

  /**
   * Given an integer reason code returned by a WifiP2pManager.ActionListener,
   * returns a human-readable message detailing the cause of the error.
   *
   * @param reasonCode integer reason code
   * @return a human-readable message
   */
  public static String getWifiP2pManagerMessageFromReasonCode(int reasonCode) {
    String reasonString;
    switch (reasonCode) {
      case WifiP2pManager.BUSY:
        reasonString = "Framework is busy.";
        break;
      case WifiP2pManager.ERROR:
        reasonString = "There was an error with the request.";
        break;
      case WifiP2pManager.P2P_UNSUPPORTED:
        reasonString = "P2P is unsupported on this device.";
        break;
      default:
        reasonString = "Unknown reason.";
    }

    return reasonString;
  }
}
