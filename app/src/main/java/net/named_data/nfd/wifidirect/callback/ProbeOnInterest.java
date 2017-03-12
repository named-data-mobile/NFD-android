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

package net.named_data.nfd.wifidirect.callback;

import android.util.Log;

import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.FibEntry;
import com.intel.jndn.management.types.NextHopRecord;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.MetaInfo;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;
import net.named_data.nfd.wifidirect.utils.NDNController;

import java.util.HashSet;
import java.util.List;

/**
 * Handle OnInterest events for incoming probe interests.
 */
public class ProbeOnInterest implements NDNCallBackOnInterest {

  private static final String TAG = "ProbeOnInterest";
  private static final int DATA_LIFE_TIME = 500; // this should be smaller than NDNController.PROBE_INTEREST_LIFETIME

  private NDNController mController = NDNController.getInstance();


  @Override
  public void doJob(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
    Log.d(TAG, "Got an interest for: " + interest.getName().toString());

    // /localhop/wifidirect/192.168.49.x/192.168.49.y/probe?mustBeFresh=1
    String[] prefixArr = interest.getName().toString().split("/");

    // validate
    if (prefixArr.length != 6) {
      Log.e(TAG, "Error with this interest, skipping...");
    }

    final String peerIp = prefixArr[prefixArr.length - 2];

    // if not logged (a face created for this probing peer), should then create a face (mainly for GO)
    if (mController.getFaceIdForPeer(peerIp) == -1) {

      mController.createFace(peerIp, NDNController.URI_TRANSPORT_PREFIX, new GenericCallback() {
        @Override
        public void doJob() {
          Log.d(TAG, "Registering localhop for: " + peerIp);
          String[] prefixes = new String[1];
          prefixes[0] = NDNController.PROBE_PREFIX + "/" + peerIp;
          mController.ribRegisterPrefix(mController.getFaceIdForPeer(peerIp),
            prefixes);
        }
      });
    }

    // enumerate RIB, look for all /ndn/wifidirect/* data prefixes, return to user as described in slides
    try {
      // set of prefixes to return to interest sender
      HashSet<String> prefixesToReturn = new HashSet<>();
      String response = "";
      int num = 0;

      // consult NFD to get all entries in FIB
      List<FibEntry> fibEntries = mController.getNfdcHelper().fibList();

      // enumerate all faces
      List<FaceStatus> faceStatuses = mController.getNfdcHelper().faceList();
      HashSet<Integer> faceIds = new HashSet<>();
      for (FaceStatus faceStatus : faceStatuses) {
        faceIds.add(faceStatus.getFaceId());
      }

      // remove the interest incomming face id
      faceIds.remove(mController.getFaceIdForPeer(peerIp));

      // return only those prefixes that are handled by faces except for the interest incomming face
      for (FibEntry fibEntry : fibEntries) {
        if (!fibEntry.getPrefix().toString().startsWith("/localhop") &&
          !fibEntry.getPrefix().toString().startsWith("/localhost")) {
          // added constraint that the prefix must be served from devices except for the interest
          // incomming device (e.g. by an upper layer application)
          List<NextHopRecord> nextHopRecords = fibEntry.getNextHopRecords();
          for (NextHopRecord nextHopRecord : nextHopRecords) {
            if (faceIds.contains(nextHopRecord.getFaceId())) {
              prefixesToReturn.add(fibEntry.getPrefix().toString());
              num++;
              break;
            }
          }
        }
      }

      Data data = new Data();
      data.setName(new Name(interest.getName().toUri()));
      MetaInfo metaInfo = new MetaInfo();
      metaInfo.setFreshnessPeriod(DATA_LIFE_TIME);
      data.setMetaInfo(metaInfo);

      // format payload, for now ignore hopcount as it is not clear whether
      // it is useful
      for (String pre : prefixesToReturn) {
        response += ("\n" + pre);
      }

      Blob payload = new Blob(num + response); // num + ("\nprefix1\nprefix2...")
      data.setContent(payload);

      face.putData(data);
      Log.d(TAG, "Send data for: " + interest.getName().toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
