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
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.RibEntry;

import net.named_data.nfd.wifidirect.callback.GenericCallback;
import net.named_data.nfd.wifidirect.model.Peer;
import net.named_data.nfd.wifidirect.utils.NDNController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Checks for the consistency between NDNController's view of
 * the logged peers and the NFD's. Specifically, this is carried
 * out by comparing views on active Faces.
 * <p>
 * This case occurs when the user delete the face manually. May be some other cases.
 */
public class FaceAndRouteConsistencyRunnable implements Runnable {
  private static final String TAG = "FaceAndRouteConsistency";

  @Override
  public void run() {

    Log.d(TAG, "Running periodic Face and route consistency check...");

    // first, let's retrieve a set of active FaceIds from NFD.
    // then, let's compare this set with what NDNController has
    try {
      List<FaceStatus> faceStatuses = NDNController.getInstance().getNfdcHelper().faceList();

      List<RibEntry> routeStatus = NDNController.getInstance().getNfdcHelper().ribList();

      // put face ids in an easy to access manner
      HashSet<Integer> nfdActiveFaceIds = new HashSet<>(faceStatuses.size());
      for (FaceStatus faceStatus : faceStatuses) {
        nfdActiveFaceIds.add(faceStatus.getFaceId());
      }

      List<String> peersWithoutFace = new ArrayList<>();

      Map<String, Peer> connectedPeers = NDNController.getInstance().getConnectedPeersMap();

      // create faces if needed
      for (final String ip : connectedPeers.keySet()) {
        int peerFaceId = connectedPeers.get(ip).getFaceId();
        if ((peerFaceId != -1) && (!nfdActiveFaceIds.contains(peerFaceId))) {
          // create the face but not destroy the logged peers
          peersWithoutFace.add(ip);
          Log.d(TAG, "create face for IP " + ip);
          NDNController.getInstance().createFace(ip, NDNController.URI_TRANSPORT_PREFIX, new GenericCallback() {
            @Override
            public void doJob() {
              Log.d(TAG, "Registering localhop for: " + ip);
              String[] prefixes = new String[1];
              prefixes[0] = NDNController.PROBE_PREFIX + "/" + ip;
              NDNController.getInstance().ribRegisterPrefix(NDNController.getInstance().getFaceIdForPeer(ip),
                prefixes);
            }
          });
        }
      }

      //create routes if needed
      for (final String ip : connectedPeers.keySet()) {
        if (peersWithoutFace.contains(ip)) {
          continue;
        }
        String prefix = NDNController.PROBE_PREFIX + "/" + ip;
        boolean exist = false;
        for (RibEntry oneRoute : routeStatus) {
          if (prefix.equals(oneRoute.getName().toUri())) {
            exist = true;
            break;
          }
        }
        if (!exist) {
          Log.d(TAG, "create route " + prefix);
          NDNController.getInstance().ribRegisterPrefix(NDNController.getInstance().getFaceIdForPeer(ip),
            new String[]{prefix});
        }
      }

      //register own prefix if needed
      if (NDNController.myAddress != null) {
        String myPrefix = NDNController.PROBE_PREFIX + "/" + NDNController.myAddress;
        boolean exist = false;
        for (RibEntry oneRoute : routeStatus) {
          if (myPrefix.equals(oneRoute.getName().toUri())) {
            exist = true;
            break;
          }
        }
        if (!exist) {
          NDNController.getInstance().registerOwnLocalhop();
        }
      }

    } catch (ManagementException me) {
      Log.e(TAG, "There was an issue retrieving FaceList from NFD");
    }
  }
}
