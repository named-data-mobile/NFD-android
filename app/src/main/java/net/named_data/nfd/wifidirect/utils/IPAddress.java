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

package net.named_data.nfd.wifidirect.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Helps retrieve the current WifiP2p interface IP address.
 * See: http://stackoverflow.com/questions/10053385/how-to-get-each-devices-ip-address-in-wi-fi-direct-scenario
 */
public class IPAddress {

  /**
   * Returns the WiFi Direct (WifiP2p interface) IP address, or null if not available.
   *
   * @return String representation of the WD Ip address if it exists, otherwise null.
   */
  public static String getLocalIPAddress() {
    try {
      byte[] ip = null;
      for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
        NetworkInterface intf = en.nextElement();
        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
          InetAddress inetAddress = enumIpAddr.nextElement();
          if (!inetAddress.isLoopbackAddress()) {
            if (inetAddress instanceof Inet4Address) { // fix for Galaxy Nexus. IPv4 is easy to use :-)
              ip = inetAddress.getAddress();
              String niceIp = getDottedDecimalIP(ip);
              if (niceIp.startsWith("192.168.49")) {  // wifid ip's are all in 192.168.49.x range
                return niceIp;
              }
            }
            //return inetAddress.getHostAddress().toString(); // Galaxy Nexus returns IPv6
          }
        }
      }

      return null;
    } catch (SocketException ex) {
      //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
    } catch (NullPointerException ex) {
      ex.printStackTrace();
      //Log.e("AndroidNetworkAddressFactory", "getLocalIPAddress()", ex);
    }
    return null;
  }

  private static String getDottedDecimalIP(byte[] ipAddr) {
    //convert to dotted decimal notation:
    String ipAddrStr = "";
    for (int i = 0; i < ipAddr.length; i++) {
      if (i > 0) {
        ipAddrStr += ".";
      }
      ipAddrStr += ipAddr[i] & 0xFF;
    }
    return ipAddrStr;
  }

  /**
   * Try to extract a hardware MAC address from a given IP address using the
   * ARP cache (/proc/net/arp).<br>
   * <br>
   * We assume that the file has this structure:<br>
   * <br>
   * IP address       HW type     Flags       HW address            Mask     Device
   * 192.168.18.11    0x1         0x2         00:04:20:06:55:1a     *        eth0
   * 192.168.18.36    0x1         0x2         00:22:43:ab:2a:5b     *        eth0
   *
   * @param ip
   * @return the MAC from the ARP cache
   */
  public static String getMacFromArpCache(String ip) {
    if (ip == null)
      return null;
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader("/proc/net/arp"));
      String line;
      while ((line = br.readLine()) != null) {
        String[] splitted = line.split(" +");
        if (splitted != null && splitted.length >= 4 && ip.equals(splitted[0])) {
          // Basic sanity check
          String mac = splitted[3];
          if (mac.matches("..:..:..:..:..:..")) {
            return mac;
          } else {
            return null;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        br.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  public static String getIPFromMac(String MAC) {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader("/proc/net/arp"));
      String line;
      while ((line = br.readLine()) != null) {

        String[] splitted = line.split(" +");
        if (splitted != null && splitted.length >= 4) {
          // Basic sanity check
          String device = splitted[5];
          if (device.matches(".*p2p-p2p0.*")) {
            String mac = splitted[3];
            if (mac.matches(MAC)) {
              return splitted[0];
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try {
        br.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
