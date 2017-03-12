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

import com.intel.jndn.management.types.FibEntry;
import com.intel.jndn.management.types.NextHopRecord;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.nfd.wifidirect.utils.NDNController;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Handle OnData events for outgoing probe interests.
 */
public class ProbeOnData implements NDNCallbackOnData {

  private static final String TAG = "ProbeOnData";
  private NDNController mController = NDNController.getInstance();
  private Face mFace = mController.getLocalHostFace();

  @Override
  public void doJob(Interest interest, Data data) {
    // interest name = /localhop/wifidirect/<toIp>/<fromIp>/probe%timestamp
    Log.d(TAG, "Got data for interest: " + interest.getName().toString());

    String[] nameArr = interest.getName().toString().split("/");
    String peerIp = nameArr[nameArr.length - 3];
    int peerFaceId = mController.getFaceIdForPeer(peerIp);

    // parse the data, update controller prefix map
    /**
     * Data is in form:
     * {numPrefixes}\n
     * prefix1\n
     * prefix2\n
     * ...
     */
    String[] responseArr = data.getContent().toString().split("\n");

    // validation
    if (peerFaceId == -1) {
      Log.e(TAG, "Undocumented peer.");
      return;
    }

    int numPrefixes = Integer.parseInt(responseArr[0]);
    HashSet<String> prefixesInResp = new HashSet<>(numPrefixes);
    for (int i = 1; i <= numPrefixes; i++) {
      prefixesInResp.add(responseArr[i]);
    }

    // enumerate FIB entries, and collect the set of data prefixes towards this peer
    HashSet<String> prefixesRegisteredForPeer = new HashSet<>();
    try {
      List<FibEntry> fibEntries = mController.getNfdcHelper().fibList();
      for (FibEntry fibEntry : fibEntries) {
        //if (fibEntry.getPrefix().toString().startsWith(NDNController.DATA_PREFIX)) {
        String fibEntryPrefix = fibEntry.getPrefix().toString();
        if (!fibEntryPrefix.startsWith("/localhop") && !fibEntryPrefix.startsWith("/localhost")) {
          List<NextHopRecord> nextHopRecords = fibEntry.getNextHopRecords();
          for (NextHopRecord nextHopRecord : nextHopRecords) {
            if (nextHopRecord.getFaceId() == peerFaceId) {
              prefixesRegisteredForPeer.add(fibEntryPrefix);
            }
          }
        }
      }

      // iterate through prefixes found in response,
      // removing any already registered prefixes for this peer
      // any prefix remaining in prefixesRegisteredForPeer after this
      // is no longer advertised by peer
      Iterator<String> it = prefixesInResp.iterator();
      while (it.hasNext()) {
        String prefix = it.next();
        if (prefixesRegisteredForPeer.contains(prefix)) {
          it.remove();
          prefixesRegisteredForPeer.remove(prefix);
        }
      }

      // register new prefixes in response
      if (prefixesInResp.size() > 0) {
        Log.d(TAG, prefixesInResp.size() + " new prefixes to add.");
        mController.ribRegisterPrefix(peerFaceId, prefixesInResp.toArray(new String[0]));
      } else {
        Log.d(TAG, "No new prefixes to register.");
      }

      // unregister all prefixes that no longer are supported via this face
      for (String toRemovePrefix : prefixesRegisteredForPeer) {
        Log.d(TAG, "Removing from FIB: " + toRemovePrefix + " " + peerFaceId);
        mController.getNfdcHelper().ribUnregisterPrefix(new Name(toRemovePrefix), peerFaceId);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
