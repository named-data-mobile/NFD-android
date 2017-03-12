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

import net.named_data.jndn.Data;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import net.named_data.nfd.wifidirect.callback.ProbeOnData;
import net.named_data.nfd.wifidirect.model.Peer;
import net.named_data.nfd.wifidirect.utils.IPAddress;
import net.named_data.nfd.wifidirect.utils.NDNController;

import java.io.IOException;

import static net.named_data.nfd.wifidirect.utils.NDNController.myAddress;

/**
 * Probes network for data prefixes, as specified in protocol.
 */
public class ProbeRunnable implements Runnable {
  private static final String TAG = "ProbeRunnable";
  //Notice: (this number) * (running interval) should equal to 1 minute
  private static final int MAX_TIMEOUTS_ALLOWED = 60;
  private NDNController mController = NDNController.getInstance();

  private OnData onData = new OnData() {
    private ProbeOnData probeOnData = new ProbeOnData();

    @Override
    public void onData(Interest interest, Data data) {
      Name interestName = interest.getName();
      Log.d(TAG, "Got data for interest " + interestName);
      String peerIp = interestName.get(interestName.size() - 3).toEscapedString();
      probeOnData.doJob(interest, data);
      Peer peer = NDNController.getInstance().getPeerByIp(peerIp);
      if (peer != null)
        peer.setNumProbeTimeouts(0);    // peer responded, so reset timeout counter
    }
  };

  private OnTimeout onTimeout = new OnTimeout() {
    @Override
    public void onTimeout(Interest interest) {
      Name interestName = interest.getName();
      Log.d(TAG, "interest " + interestName + " times out");
      String peerIp = interestName.get(interestName.size() - 3).toEscapedString();
      Peer peer = NDNController.getInstance().getPeerByIp(peerIp);
      if (peer == null) {
        Log.d(TAG, "No peer information available to track timeout.");
        return;
      }

      Log.d(TAG, "Timeout for interest: " + interest.getName().toString() +
        " Attempts: " + (peer.getNumProbeTimeouts() + 1));

      if (peer.getNumProbeTimeouts() + 1 >= MAX_TIMEOUTS_ALLOWED) {
        // This case means, remove a peer which
        // (1) is indicatated connected by Wifi-Direct
        // (2) but doesn't response to probeInterest
        // so remove it (disconnect it and remove saved states)
        NDNController.getInstance().removePeer(peerIp);
      } else {
        peer.setNumProbeTimeouts(peer.getNumProbeTimeouts() + 1);
      }
    }
  };

  @Override
  public void run() {
    Log.d(TAG, "start to probe");
    try {
      if (IPAddress.getLocalIPAddress() == null) {

        // this means that a disconnect has recently occurred and this device
        // is no longer a part of a group (WDBroadcastReceiver.myAddress is this
        // device's previous WD IP)
        if (myAddress != null) {
          Log.d(TAG, "A disconnect has been detected, refreshing state...");

          // unregister the previous "/localhop/wifidirect/..." prefix
          mController.cleanUpConnections();
          // recreateFace for future connection
          mController.recreateFace();

          // most likely will have a new IP to register "/localhop/wifidirect/<IP>"
          // call this so that the next time a group is joined a new local prefix
          // registration will occur
          mController.setHasRegisteredOwnLocalhop(false);

          // ensure that peer diiscovery is running, if it had not been before
          mController.startDiscoveringPeers();
        } else {
          Log.d(TAG, "Skip this iteration due to null WD ip.");
        }

      } else {
        for(String ip : NDNController.getInstance().getIpsOfConnectedPeers()) {
          //send interest to this peer
          Interest interest = new Interest(new Name(NDNController.PROBE_PREFIX + "/" + ip + "/" + myAddress + "/probe"));
          interest.setMustBeFresh(true);
          interest.setInterestLifetimeMilliseconds(NDNController.PROBE_INTEREST_LIFETIME);
          Log.d(TAG, "Sending interest: " + interest.getName().toString());
          NDNController.getInstance().getLocalHostFace().expressInterest(interest, onData, onTimeout);
        }
      }
    } catch (IOException ioe) {
      Log.e(TAG, "Something went wrong with sending a probe interest.");
      ioe.printStackTrace();
    }
  }

}
