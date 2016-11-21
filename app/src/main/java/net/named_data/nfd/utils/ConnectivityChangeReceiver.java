/* -*- Mode:jde; c-file-style:"gnu"; indent-tabs-mode:nil; -*- */
/**
 * Copyright (c) 2015-2016 Regents of the University of California
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

package net.named_data.nfd.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import net.named_data.nfd.service.NfdService;

/**
 * ConnectivityChangeReceiver monitors network connectivity changes and update
 * face and route list accordingly.
 */
public class ConnectivityChangeReceiver extends BroadcastReceiver {
  private static final String TAG = ConnectivityChangeReceiver.class.getName();

  @Override
  public void
  onReceive(Context context, Intent intent) {
    NetworkInfo network = getNetworkInfo(context);
    if (null == network) {
      G.Log(TAG, "Connection lost");
      onConnectionLost(context);
    } else {
      G.Log(TAG, "Connection changed");
      onChange(context, network);
    }
  }

  private static NetworkInfo getNetworkInfo(Context applicationContext) {
    ConnectivityManager cm = (ConnectivityManager) applicationContext
        .getSystemService(Context.CONNECTIVITY_SERVICE);
    return cm.getActiveNetworkInfo();
  }

  private void
  onChange(Context context, NetworkInfo networkInfo) {
    if (networkInfo.isConnected()) {
      G.Log(TAG, "Network is connected");
      // (re-)start service, triggering (re-)creation of permanent faces and routes
      context.startService(new Intent(context, NfdService.class));
    }
  }

  private void
  onConnectionLost(Context context) {
    // nothing to do: face/routes should disappear by themselves
  }
}
