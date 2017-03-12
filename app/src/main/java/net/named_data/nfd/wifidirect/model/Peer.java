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

package net.named_data.nfd.wifidirect.model;

import android.net.wifi.p2p.WifiP2pDevice;

/**
 * Represents a WifiDirect Peer.
 */
public class Peer {

  // members
  private WifiP2pDevice device;
  private String ipAddress;
  private int faceId;
  private int numProbeTimeouts = 0;   // number of timeouts while probing prefixes from this peer

  public Peer() {
  }

  public WifiP2pDevice getDevice() {
    return device;
  }

  public void setDevice(WifiP2pDevice device) {
    this.device = device;
  }

  public String getIpAddress() {
    return ipAddress;
  }

  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  public int getFaceId() {
    return faceId;
  }

  public void setFaceId(int faceId) {
    this.faceId = faceId;
  }

  public int getNumProbeTimeouts() {
    return numProbeTimeouts;
  }

  public void setNumProbeTimeouts(int numProbeTimeouts) {
    this.numProbeTimeouts = numProbeTimeouts;
  }

  @Override
  public String toString() {
    return "Peer{" +
      "name=\"" + device.deviceName + "\"" +
      ", macAddress=\"" + device.deviceAddress + "\"" +
      ", ipAddress=\"" + ipAddress + "\"" +
      ", faceId=" + faceId +
      ", numProbeTimeouts=" + numProbeTimeouts +
      '}';
  }
}
