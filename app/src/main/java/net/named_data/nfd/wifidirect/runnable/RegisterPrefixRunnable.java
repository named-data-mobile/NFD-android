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
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.OnRegisterFailed;
import net.named_data.jndn.OnRegisterSuccess;
import net.named_data.nfd.wifidirect.utils.NDNController;

/**
 * Task that provides the ability to register a prefix to the specified face.
 * TODO: Given that multiple successive prefix registration calls can fail (NFD timeout),
 * This task needs to attempt to register prefixes 5 times or until success. Each
 * attempt is separated (e.g. 500ms) to increase chance of registration.
 */
public class RegisterPrefixRunnable implements Runnable {
  private final String TAG = "RegisterPrefixRunnable";
  private OnInterestCallback onInterestCallback;

  private String prefixToRegister;

  public RegisterPrefixRunnable(String prefix, OnInterestCallback cb) {
    this.prefixToRegister = prefix;
    this.onInterestCallback = cb;
  }

  @Override
  public void run() {
    Log.d(TAG, "try to register local prefix" + prefixToRegister);
    try {
      // allow child inherit
      final ForwardingFlags flags = new ForwardingFlags();
      flags.setChildInherit(true);

      Name prefix = new Name(prefixToRegister);

      long registerPrefixId = NDNController.getInstance().getLocalHostFace().registerPrefix(prefix, onInterestCallback,
        new OnRegisterFailed() {
          @Override
          public void onRegisterFailed(Name prefix) {
            Log.d(TAG, "Failed to register prefix: " + prefix.toString());
          }
        }, new OnRegisterSuccess() {
          @Override
          public void onRegisterSuccess(Name prefix, long registeredPrefixId) {
            Log.d(TAG, "Prefix registered successfully: " + prefixToRegister);

          }
        },
        flags);
      Log.d(TAG, "registered prefix id is " + registerPrefixId);
      NDNController.getInstance().setRegisteredPrefixId(registerPrefixId);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
