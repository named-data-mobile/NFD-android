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

import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Name;
import net.named_data.nfd.wifidirect.utils.NDNController;

/**
 * Convenience class used for registering a prefix towards some Face, denoted by
 * its Face ID. Note that this class differs from RegisterPrefixRunnable, as the latter
 * deals with registering prefixes to a localhost face, while this class does not make
 * that assumption.
 */
public class RibRegisterPrefixRunnable implements Runnable {

  private final String TAG = "RibRegister";

  private String prefixToRegister;
  private int faceId;
  private int cost;
  private boolean childInherit;
  private boolean capture;

  public RibRegisterPrefixRunnable(String prefixToRegister, int faceId, int cost,
                                   boolean childInherit, boolean capture) {
    this.prefixToRegister = prefixToRegister;
    this.capture = capture;
    this.childInherit = childInherit;
    this.cost = cost;
    this.faceId = faceId;
  }

  @Override
  public void run() {
    try {
      ForwardingFlags flags = new ForwardingFlags();
      flags.setChildInherit(childInherit);
      flags.setCapture(capture);
      NDNController.getInstance().getNfdcHelper().ribRegisterPrefix(new Name(prefixToRegister),
        faceId, cost, childInherit, capture);

      Log.d(TAG, "registered rib prefix: " + prefixToRegister);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
