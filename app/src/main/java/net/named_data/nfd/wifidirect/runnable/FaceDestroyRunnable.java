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

import net.named_data.nfd.wifidirect.utils.NDNController;

/**
 * Attempts to destroy a given Face, denoted by its Face id.
 */
public class FaceDestroyRunnable implements Runnable {
  private int faceId;

  public FaceDestroyRunnable(int faceId) {
    this.faceId = faceId;
  }

  private static final String TAG = "FaceDestroyRunnable";

  @Override
  public void run() {
    try {
      Log.d(TAG, "-------- Inside face destroy task --------");
      // attempt to destroy Face Id, specified as the first and only parameter
      NDNController.getInstance().getNfdcHelper().faceDestroy(faceId);
      Log.d(TAG, "Successfully destroyed Face with Face id: " + faceId);
    } catch (ManagementException me) {
      Log.e(TAG, me.getMessage());
    } catch (Exception e) {
      Log.e(TAG, e.getMessage());
    }
    Log.d(TAG, "---------- END face destroy task -----------");
  }
}
