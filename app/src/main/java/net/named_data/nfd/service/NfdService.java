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
import android.os.IBinder;
import android.os.Messenger;

import net.named_data.nfd.G;

public class NfdService extends Service {

  private final Messenger mNfdServiceMessenger
      = new Messenger(new NfdServiceMessageHandler(this));

  @Override
  public void onCreate() {
    G.Log("onCreate()");
    // TODO: Reload NFD and NRD in memory structures (if any)
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    G.Log("onStartCommand()");
    // If we need to handle per-client start invocations, they are to be
    // handled here.

    // Nothing else to do here for now.

    // Service is restarted when killed.
    // Pending intents delivered; null intent redelivered otherwise
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
    return mNfdServiceMessenger.getBinder();
  }

  @Override
  public void onDestroy() {
    G.Log("onDestroy()");
    // TODO: Save NFD and NRD in memory data structures.
  }
}
