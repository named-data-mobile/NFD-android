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

import net.named_data.jndn.Name;
import net.named_data.nfd.wifidirect.utils.NDNController;

/**
 * Convenience class used for registering a prefix towards some Face, denoted by
 * its Face ID. Note that this class differs from RegisterPrefixRunnable, as the latter
 * deals with registering prefixes to a localhost face, while this class does not make
 * that assumption.
 */
public class RibUnregisterPrefixRunnable implements Runnable {

  private static final String TAG = "RibUnregisterTask";

  private String prefixToUnregister;

  public RibUnregisterPrefixRunnable(String prefixToUnregister) {
    this.prefixToUnregister = prefixToUnregister;
  }

  @Override
  public void run() {
    try {
      NDNController.getInstance().getNfdcHelper().ribUnregisterPrefix(new Name(prefixToUnregister));

      Log.d(TAG, "Unregistered rib prefix: " + prefixToUnregister);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
