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

package net.named_data.nfd.wifidirect.runnable;

import android.util.Log;

import net.named_data.nfd.wifidirect.utils.NDNController;

/**
 * If connected peers list keeps empty, while myaddress keeps un-empty for 1 minute, remove the group
 */

public class GroupStatusConsistencyRunnable implements Runnable {

  private static final String TAG = "GroupStatusConsistency";


  //Notice: (this number) * (running interval) should equal to 1 minute
  public static final int MAX_TIMEOUTS_ALLOWED = 6;
  public static int TIME_OUT_TIMES = 0;

  public static void resetTimeoutTimes() {
    TIME_OUT_TIMES = 0;
  }

  @Override
  public void run() {
    Log.d(TAG, "Check GroupStatusConsistency");
    if(NDNController.getInstance().isNumOfConnectedPeersZero() && NDNController.myAddress != null) {
      TIME_OUT_TIMES ++;
    } else {
      TIME_OUT_TIMES = 0;
    }
    if(TIME_OUT_TIMES >= MAX_TIMEOUTS_ALLOWED) {
      NDNController.getInstance().disconnect();
      TIME_OUT_TIMES = 0;
    }
  }
}
