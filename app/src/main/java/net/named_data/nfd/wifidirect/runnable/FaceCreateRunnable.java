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

import com.intel.jndn.management.ManagementException;

import net.named_data.nfd.wifidirect.callback.GenericCallback;
import net.named_data.nfd.wifidirect.model.Peer;
import net.named_data.nfd.wifidirect.utils.NDNController;

/**
 * Convenience class that creates a Face with the forwarder. A callback
 * is accpeted via the public setCallback(...) method, and will be called
 * if and only if face creation succeeds.
 */
// task to create a network face without using main thread
public class FaceCreateRunnable implements Runnable {

  private static final String TAG = "FaceCreateRunnable";
  private String peerIp;
  private String faceUri;
  private NDNController mController = NDNController.getInstance();
  private GenericCallback callback = null;

  public FaceCreateRunnable(String peerIp, String faceUri) {
    this.peerIp = peerIp;
    this.faceUri = faceUri;
  }

  public void setCallback(GenericCallback callback) {
    this.callback = callback;
  }

  @Override
  public void run() {
    int faceId = -1;

    try {
      Log.d(TAG, "-------- Inside face create runnable --------");

      faceId = mController.getNfdcHelper().faceCreate(faceUri);

      Log.d(TAG, "Created Face with Face id: " + faceId);
      if (faceId != -1) {

        // if face creation successful, log the new peer
        Peer peer = new Peer();
        peer.setFaceId(faceId);
        peer.setIpAddress(peerIp);
        mController.logPeer(peerIp, peer);

        // invoke callback, if any
        if (callback != null) {
          callback.doJob();
        }
      }

    } catch (ManagementException me) {
      Log.e(TAG, me.getMessage());
    } catch (Exception e) {
      Log.e(TAG, "" + e.getMessage());
      e.printStackTrace();
    }

    Log.d(TAG, "---------- END face create runnable -----------");
  }
}
