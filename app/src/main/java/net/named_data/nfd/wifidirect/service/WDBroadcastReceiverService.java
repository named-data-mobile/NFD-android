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

package net.named_data.nfd.wifidirect.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import net.named_data.nfd.wifidirect.utils.NDNController;
import net.named_data.nfd.wifidirect.utils.WDBroadcastReceiver;

/**
 * Service that registers a WDBroadcastReceiver to listen to WiFi Direct
 * broadcasted intents.
 */

public class WDBroadcastReceiverService extends Service {

  private final static String TAG = "WDBRService";

  private WDBroadcastReceiver mReceiver = null;
  private WifiP2pManager mManager;
  private WifiP2pManager.Channel mChannel;
  private IntentFilter mIntentFilter;

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "initWifiP2p() service");
    initWifiP2p();
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Log.d(TAG, "registerReceiver()");
    registerReceiver(mReceiver, mIntentFilter);

    // If we get killed, after returning from here, restart
    return START_STICKY;
  }

  @Override
  public void onDestroy() {
    Log.d(TAG, "onDestroy()");

    if (mReceiver != null) {
      Log.d(TAG, "unregisterReceiver()");
      unregisterReceiver(mReceiver);
    }

    super.onDestroy();
  }

  /* initialize manager and receiver for activity */
  private void initWifiP2p() {
    mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
    mChannel = mManager.initialize(this, getMainLooper(), null);
    mReceiver = new WDBroadcastReceiver(mManager, mChannel);

    mIntentFilter = new IntentFilter();
    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
    mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

    NDNController.getInstance().recordWifiP2pResources(mManager, mChannel);
  }
}

