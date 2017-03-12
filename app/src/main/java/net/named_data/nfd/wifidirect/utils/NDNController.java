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

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.intel.jndn.management.ManagementException;

import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.InterestFilter;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterestCallback;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.SecurityException;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.nfd.utils.NfdcHelper;
import net.named_data.nfd.wifidirect.callback.GenericCallback;
import net.named_data.nfd.wifidirect.callback.ProbeOnInterest;
import net.named_data.nfd.wifidirect.model.Peer;
import net.named_data.nfd.wifidirect.runnable.DiscoverPeersRunnable;
import net.named_data.nfd.wifidirect.runnable.FaceAndRouteConsistencyRunnable;
import net.named_data.nfd.wifidirect.runnable.GroupStatusConsistencyRunnable;
import net.named_data.nfd.wifidirect.runnable.ProbeRunnable;
import net.named_data.nfd.wifidirect.service.WDBroadcastReceiverService;
import net.named_data.nfd.wifidirect.runnable.FaceCreateRunnable;
import net.named_data.nfd.wifidirect.runnable.FaceDestroyRunnable;
import net.named_data.nfd.wifidirect.runnable.FaceEventProcessRunnable;
import net.named_data.nfd.wifidirect.runnable.RegisterPrefixRunnable;
import net.named_data.nfd.wifidirect.runnable.RibRegisterPrefixRunnable;
import net.named_data.nfd.wifidirect.runnable.RibUnregisterPrefixRunnable;
import net.named_data.nfd.wifidirect.runnable.UnregisterPrefixRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * New streamlined NDNOverWifiDirect controller. This class acts as the
 * manager (hence controller) of the protocol, and applications using
 * this project need only interface with the returned instance via getInstance().
 */

public class NDNController implements WifiP2pManager.PeerListListener,
  WifiP2pManager.ConnectionInfoListener, WifiP2pManager.ChannelListener {
  // volatile variables accessed in multiple threads
  public static volatile String groupOwnerAddress;
  public static volatile String myAddress;
  public static volatile String myDeviceName;
  public static volatile int WIFI_STATE = WifiP2pManager.WIFI_P2P_STATE_ENABLED;

  public static final String URI_UDP_PREFIX = "udp://";
  public static final String URI_TCP_PREFIX = "tcp://";
  public static final String URI_TRANSPORT_PREFIX = URI_UDP_PREFIX;   // transport portion of uri that rest of project should use
  public static final String PROBE_PREFIX = "/localhop/wifidirect";   // prefix of prefix used in probing

  private static final String TAG = "NDNController";
  private static final int DISCOVER_PEERS_DELAY = 5000;  // in ms
  public static final int PROBE_DELAY = 1000;           // in ms
  public static final int PROBE_INTEREST_LIFETIME = 1000; // in ms (the network delay should not be large)
  private static final int FACE_AND_ROUTE_CONSISTENCY_CHECK_DELAY = 5000;
  private static final int GROUP_STATUS_CONSISTENCY_CHECK_DELAY = 10000;

  // Singleton
  private static NDNController mController = null;
  private static KeyChain mKeyChain = null;

  // WiFi Direct related resources
  private WifiP2pManager wifiP2pManager = null;
  private WifiP2pManager.Channel channel = null;
  private Context wifiDirectContext = null;       // context in which WiFi direct operations begin (an activity/fragment)
  private List<WifiP2pDevice> discoverdPeers = new ArrayList<>();

  // Relevant tasks, services, etc.
  private WDBroadcastReceiverService brService = null;
  private Future discoverPeersFuture = null;
  private Future probeFuture = null;
  private Future faceAndRouteConsistencyFuture = null;
  private Future groupStatusConsistencyFuture = null;
  // keep 1 thread to serialize all the tasks
  private ScheduledThreadPoolExecutor faceEventProcessExecutor = new ScheduledThreadPoolExecutor(1);
  private ScheduledThreadPoolExecutor localFaceCommandExecutor = new ScheduledThreadPoolExecutor(1);
  private ScheduledThreadPoolExecutor nfdcFaceCommandExecutor = new ScheduledThreadPoolExecutor(1);
  private ScheduledThreadPoolExecutor generalExecutor = new ScheduledThreadPoolExecutor(1);

  // Useful flags
  private boolean hasRegisteredOwnLocalhop = false;
  private boolean isGroupOwner;    // set in broadcast receiver, used primarily in ProbeOnInterest

  // we have some redundancy here in data, but difficult to avoid given WiFi Direct API
  private HashMap<String, Peer> ipPeerMapOfConnectedPeers = new HashMap<>();                  // { peerIp : PeerInstance }, contains at least Face id info

  // single shared Face instance at localhost
  private Face mFace = null;
  private final NfdcHelper nfdcHelper = new NfdcHelper();
  long registeredPrefixId = -1;

  private FaceEventProcessRunnable faceEventProcessRunnable = null;
  private Future faceEventProcessFuture = null;


  /**
   * Private constructor to prevent outside instantiation.
   */
  private NDNController() {

    if (mKeyChain == null) {
      try {
        // this is an expensive operation, so minimize it's invocation
        mKeyChain = buildTestKeyChain();
      } catch (SecurityException e) {
        Log.e(TAG, "Unable to build the test keychain.");
      }
    }
  }

  /**
   * Returns a shared instance of NDNController for use across the library.
   *
   * @return NDNController instance
   */
  public static NDNController getInstance() {
    if (mController == null) {
      mController = new NDNController();
    }

    return mController;
  }

  public void setRegisteredPrefixId(long registeredPrefixId) {
    this.registeredPrefixId = registeredPrefixId;
  }

  public long getRegisteredPrefixId() {
    return registeredPrefixId;
  }

  /**
   * @return
   */
  public List<WifiP2pDevice> getDiscoveredPeers() {
    return discoverdPeers;
  }


  /**
   * Logs the peer with the corresponding WD IP address. If a previous logging of
   * the peer exists, this will replace it.
   *
   * @param peerIp The peer's WD IP address
   * @param peer   A Peer instance with at least FaceId set.
   */
  public void logPeer(String peerIp, Peer peer) {
    ipPeerMapOfConnectedPeers.put(peerIp, peer);
  }

  /**
   * Returns the Face id associated with the given peer, denoted by IP address.
   *
   * @param peerIp The WiFi Direct IP address of the peer
   * @return the Face id of the peer or -1 if no mapping exists.
   */
  public int getFaceIdForPeer(String peerIp) {
    if (ipPeerMapOfConnectedPeers.containsKey(peerIp)) {
      return ipPeerMapOfConnectedPeers.get(peerIp).getFaceId();
    }

    return -1;
  }

  /**
   * Returns the logged peer instance (via logPeer()) by its
   * WiFi Direct IP address.
   *
   * @param ip WiFi Direct IP address of peer
   * @return the peer instance logged earlier by a call to logPeer(), or null
   * if none.
   */
  public Peer getPeerByIp(String ip) {
    return ipPeerMapOfConnectedPeers.get(ip);
  }

  /**
   * Similar to getConnectPeers, except this returns the IP addresses
   * of the currently logged peers.
   *
   * @return A set backed by the underlying map of logged peers. Thus,
   * be advised that changes to the returned set will be reflected in
   * the underlying map.
   */
  public Set<String> getIpsOfConnectedPeers() {
    return ipPeerMapOfConnectedPeers.keySet();
  }

  public Collection<Peer> getConnectedPeers() {
    return ipPeerMapOfConnectedPeers.values();
  }

  public boolean isNumOfConnectedPeersZero() {
    return ipPeerMapOfConnectedPeers.isEmpty();
  }

  public Map<String, Peer> getConnectedPeersMap() {
    return ipPeerMapOfConnectedPeers;
  }

  /**
   * Removes mapping to the logged peer, and destroys any state of that peer (e.g. any created
   * faces and registered prefixes).
   *
   * @param ip WiFi Direct IP address of peer
   */
  public void removePeer(String ip) {
    // for now, if the current device is not group owner, it only has one connection, so simply disconnect
    if (!isGroupOwner) {
      disconnect();
    }
    // if the current device is the group owner, we cannot disconnect the group, but simply remove the
    // group member.
    else {
      FaceDestroyRunnable runnable = new FaceDestroyRunnable(ipPeerMapOfConnectedPeers.get(ip).getFaceId());
      nfdcFaceCommandExecutor.execute(runnable);
      ipPeerMapOfConnectedPeers.remove(ip);
    }
  }

  /**
   * Returns whether this device is the group owner
   *
   * @return true if this device is GO, false otherwise
   */
  public boolean getIsGroupOwner() {
    return isGroupOwner;
  }

  /**
   * Sets whether this device is the group owner
   *
   * @param b whether the device is the GO.
   */
  public void setIsGroupOwner(boolean b) {
    isGroupOwner = b;
  }

  /**
   * Initializes the WifiP2p context, channel and manager, for use with discovering peers.
   * This must be done before ever calling discoverPeers().
   *
   * @param wifiP2pManager the WifiP2pManager
   * @param channel        the WifiP2p Channel
   */
  public void recordWifiP2pResources(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel) {
    this.wifiP2pManager = wifiP2pManager;
    this.channel = channel;
  }

  /**
   * Must be done before we can start the Broadcast Receiver Service -  in the future we can also
   * move the starting to somewhere within an activity or fragment in order to avoid this.
   *
   * @param context A valid Android context
   */
  public void setWifiDirectContext(Context context) {
    this.wifiDirectContext = context;
  }

  /**
   * Creates a face to the specified peer (IP), with the
   * uriPrefix (e.g. tcp://). Optional callback parameter
   * for adding a callback function to be called after successful
   * face creation. Passing in null for callback means no callback.
   *
   * @param peerIp    the peer's WiFi Direct IP
   * @param uriPrefix uri prefix
   * @param callback  An implementation of GenericCallback, or null. Is called AFTER face
   *                  creation succeeds.
   */
  public void createFace(String peerIp, String uriPrefix, GenericCallback callback) {

    if (peerIp.equals(IPAddress.getLocalIPAddress())) {
      return; //never add yourself as a face
    }

    // need to create a new face for this peer
    FaceCreateRunnable runnable = new FaceCreateRunnable(peerIp, uriPrefix + peerIp);

    if (callback != null) {
      runnable.setCallback(callback);
    }

    nfdcFaceCommandExecutor.execute(runnable);
  }

  /**
   * Registers the array of prefixes with the given Face, denoted by
   * its face id.
   *
   * @param faceId   The Face Id to register the prefixes to.
   * @param prefixes array of prefixes to register.
   */
  public void ribRegisterPrefix(int faceId, String[] prefixes) {
    Log.d(TAG, "ribRegisterPrefix called with: " + faceId + " and " + prefixes.length + " prefixes");

    HashSet<Integer> faceIds = new HashSet<>(ipPeerMapOfConnectedPeers.size());
    for (Peer p : ipPeerMapOfConnectedPeers.values()) {
      faceIds.add(p.getFaceId());
    }

    if (faceIds.contains(faceId)) {
      for (String prefix : prefixes) {
        Log.d(TAG, "ribRegisterPrefix() with prefix: " + prefix);
        RibRegisterPrefixRunnable runnable = new RibRegisterPrefixRunnable(prefix, faceId,
          0, true, false);
        nfdcFaceCommandExecutor.execute(runnable);
      }
    }
  }

  /**
   * Begins periodically looking for peers, and connecting
   * to them.
   */
  public void startDiscoveringPeers() {
    if (discoverPeersFuture == null) {
      Log.d(TAG, "Start discovering peers every " + DISCOVER_PEERS_DELAY + "ms");
      DiscoverPeersRunnable runnable = new DiscoverPeersRunnable();
      discoverPeersFuture = generalExecutor.scheduleWithFixedDelay(runnable, 100, DISCOVER_PEERS_DELAY, TimeUnit.MILLISECONDS);
    } else {
      Log.d(TAG, "Discovering peers already running!");
    }
  }

  /**
   * Stops periodically discovering peers and connecting to them.
   */
  public void stopDiscoveringPeers() {
    if (discoverPeersFuture != null) {
      discoverPeersFuture.cancel(true);
      discoverPeersFuture = null;
      Log.d(TAG, "Stopped discovering peers.");
    }
  }

  /**
   * Begins probing the network for data prefixes.
   */
  public void startProbing() {
    if (probeFuture == null) {
      Log.d(TAG, "Start probing for data prefixes every " + PROBE_DELAY + "ms");
      ProbeRunnable runnable = new ProbeRunnable();
      probeFuture = localFaceCommandExecutor.scheduleWithFixedDelay(runnable, 200, PROBE_DELAY, TimeUnit.MILLISECONDS);
    } else {
      Log.d(TAG, "Probing task already running!");
    }
  }

  /**
   * Stops probing the network for data prefixes.
   */
  public void stopProbing() {
    if (probeFuture != null) {
      probeFuture.cancel(true);
      probeFuture = null;
      Log.d(TAG, "Stopped probing.");
    } else {
      Log.d(TAG, "Pprobing already stopped");
    }
  }

  /**
   * Starts service that registers the broadcast receiver for handling peer discovery
   */
  public void startBroadcastReceiverService() {
    if (brService == null) {
      Log.d(TAG, "Starting WDBR service...");
      brService = new WDBroadcastReceiverService();
      Intent intent = new Intent(wifiDirectContext, WDBroadcastReceiverService.class);
      wifiDirectContext.startService(intent);
    } else {
      Log.d(TAG, "BroadcastReceiverService already started.");
    }
  }

  /**
   * Stops the service that registers the broadcast recevier for handling peer discovery.
   */
  public void stopBroadcastReceiverService() {
    if (brService == null) {
      Log.d(TAG, "BroadcastReceiverService not running, no need to stop.");
    } else {
      if (wifiDirectContext != null) {
        wifiDirectContext.stopService(new Intent(wifiDirectContext, WDBroadcastReceiverService.class));
      }

      brService = null;
      Log.d(TAG, "Stopped WDBR service.");
    }
  }

  /**
   * Starts periodically checking for consistency between NFD and NDNController's view of
   * active faces.
   */
  public void startFaceAndRouteConsistencyChecker() {
    if (faceAndRouteConsistencyFuture == null) {
      Log.d(TAG, "Start checking consistency of logged Faces every " +
        FACE_AND_ROUTE_CONSISTENCY_CHECK_DELAY + "ms");
      FaceAndRouteConsistencyRunnable runnable = new FaceAndRouteConsistencyRunnable();
      faceAndRouteConsistencyFuture = nfdcFaceCommandExecutor.scheduleWithFixedDelay(runnable,
        300, FACE_AND_ROUTE_CONSISTENCY_CHECK_DELAY, TimeUnit.MILLISECONDS);
    } else {
      Log.d(TAG, "Face consistency checker already running!");
    }
  }

  /**
   * Stops periodically checking for consistency between NFD and NDNController's view
   * of active faces.
   */
  public void stopFaceAndRouteConsistencyChecker() {
    if (faceAndRouteConsistencyFuture != null) {
      faceAndRouteConsistencyFuture.cancel(false);    // do not interrupt if running, but cancel further execution
      faceAndRouteConsistencyFuture = null;

      Log.d(TAG, "Stopped checking for Face consistency.");
    } else {
      Log.d(TAG, "Face consistency checker is already stopped");
    }
  }

  /**
   * Starts periodically checking for consistency between whether there are connected peers and this
   * device's myAddress.
   */
  public void startGroupConsistencyChecker() {
    if (groupStatusConsistencyFuture == null) {
      Log.d(TAG, "Start checking consistency of group status every " +
        GROUP_STATUS_CONSISTENCY_CHECK_DELAY + "ms");
      GroupStatusConsistencyRunnable.resetTimeoutTimes();
      GroupStatusConsistencyRunnable runnable = new GroupStatusConsistencyRunnable();
      groupStatusConsistencyFuture = generalExecutor.scheduleWithFixedDelay(runnable,
        300, GROUP_STATUS_CONSISTENCY_CHECK_DELAY, TimeUnit.MILLISECONDS);
    } else {
      Log.d(TAG, "Group status consistency checker already running!");
    }
  }

  /**
   * Stops periodically checking for consistency between NFD and NDNController's view
   * of active faces.
   */
  public void stopGroupConsistencyChecker() {
    if (groupStatusConsistencyFuture != null) {
      groupStatusConsistencyFuture.cancel(false);    // do not interrupt if running, but cancel further execution
      groupStatusConsistencyFuture = null;
      GroupStatusConsistencyRunnable.resetTimeoutTimes();
      Log.d(TAG, "Stopped checking for Face consistency.");
    } else {
      Log.d(TAG, "Group status consistency checker is already stopped");
    }
  }

  /**
   * Main convenience wrapper method to start all background
   * tasks/services for this protocol.
   */
  public void start() {
    recreateFace();
    startDiscoveringPeers();
    startProbing();
    startBroadcastReceiverService();
    startFaceAndRouteConsistencyChecker();
    startGroupConsistencyChecker();
  }

  /**
   * Main convenience wrapper method to stop all background
   * tasks/services for this protocol.
   */
  public void stop() {
    cleanUp();
    stopGroupConsistencyChecker();
    stopFaceAndRouteConsistencyChecker();
    stopBroadcastReceiverService();
    stopProbing();
    stopDiscoveringPeers();
  }

  /**
   * Whether or not /localhop/wifidirect/xxx.xxx.xxx.xxx has
   * been registered. Here, the ip is specifically that of this device.
   *
   * @return true if so, false otherwise
   */
  public boolean getHasRegisteredOwnLocalhop() {
    return this.hasRegisteredOwnLocalhop;
  }

  /**
   * Sets the flag for whether the /localhop/wifidiret/xxx.xxx.xxx.xxx prefix is registered.
   *
   * @param set true or false
   */
  public void setHasRegisteredOwnLocalhop(boolean set) {
    this.hasRegisteredOwnLocalhop = set;
  }

  /**
   * Convenience function to register the important /localhop prefix, necessary for
   * probe communication.
   */
  public void registerOwnLocalhop() {
    if (!hasRegisteredOwnLocalhop) {
      Log.d(TAG, "registerOwnLocalhop() starts to work");
      RegisterPrefixRunnable runnable = new RegisterPrefixRunnable(
        PROBE_PREFIX + "/" + myAddress, new OnInterestCallback() {
        @Override
        public void onInterest(Name prefix, Interest interest, Face face, long interestFilterId, InterestFilter filter) {
          (new ProbeOnInterest()).doJob(prefix, interest, face, interestFilterId, filter);
        }
      });
      localFaceCommandExecutor.execute(runnable);
    }

    setHasRegisteredOwnLocalhop(true);
  }

  /**
   * Convenience function to unregister the important /localhop prefix, if it was registered
   * previously.
   */
  public void unregisterOwnLocalhop() {
    if (myAddress != null) {
      RibUnregisterPrefixRunnable runnable = new RibUnregisterPrefixRunnable(PROBE_PREFIX + "/" + myAddress);
      nfdcFaceCommandExecutor.execute(runnable);
    }

    // unregister the prefix, no longer handled if logic gets to here
    UnregisterPrefixRunnable runnable1 = new UnregisterPrefixRunnable(registeredPrefixId);
    localFaceCommandExecutor.execute(runnable1);
    NDNController.getInstance().setHasRegisteredOwnLocalhop(false);
  }

  /**
   * Attempts to scan nearby network for WD peers. This can be a one-off
   * operation, or can be called periodically over time.
   *
   * @throws Exception
   */
  public void discoverPeers() throws Exception {

    if (wifiP2pManager == null || channel == null) {
      Log.e(TAG, "Unable to discover peers, did you recordWifiP2pResources() yet?");
      return;
    }

    wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        // WIFI_P2P_PEERS_CHANGED_ACTION intent sent!!
        Log.d(TAG, "Success on discovering peers");
      }

      @Override
      public void onFailure(int reasonCode) {
        String reasonString = WDBroadcastReceiver
          .getWifiP2pManagerMessageFromReasonCode(reasonCode);
        Log.d(TAG, "Fail discover peers, reason: " + reasonString);
      }
    });
  }

  /**
   * Returns a face to localhost, to avoid multiple creations of localhost
   * faces.
   *
   * @return the localhost Face instance.
   */
  public Face getLocalHostFace() {
    return mFace;
  }

  public NfdcHelper getNfdcHelper() {
    return nfdcHelper;
  }

  /**
   * Resets all state. (including disconnecting group, and reseting saved states)
   */
  public void cleanUp() {
    cancelConnect();
    disconnect();
    cleanUpConnections();
  }

  /**
   * cancel ongoing negotiation
   */
  public void cancelConnect() {
    // if you are negotiating with others, cancel the connection
    if (wifiP2pManager != null) {
      wifiP2pManager.cancelConnect(channel, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
          Log.d(TAG, "Successfully removed self from WifiP2p group.");
        }

        @Override
        public void onFailure(int reason) {
          String reasonString = WDBroadcastReceiver
            .getWifiP2pManagerMessageFromReasonCode(reason);
          Log.e(TAG, "Unable to remove self from WifiP2p group, reason: " + reasonString);
        }
      });
    }
  }

  /**
   * disconnect from a group
   */
  public void disconnect() {
    // if you are in a group, remove yourself from it
    // note that if you are the group owner, this will cause a disruption in connectivity
    // for the other peers
    if (wifiP2pManager != null) {
      wifiP2pManager.removeGroup(channel, new WifiP2pManager.ActionListener() {
        @Override
        public void onSuccess() {
          Log.d(TAG, "Successfully removed self from WifiP2p group.");
        }

        @Override
        public void onFailure(int reason) {
          String reasonString = WDBroadcastReceiver
            .getWifiP2pManagerMessageFromReasonCode(reason);
          Log.e(TAG, "Unable to remove self from WifiP2p group, reason: " + reasonString);
        }
      });
    }
  }

  /**
   * start all runnable, keep saved Wifi-Direct states unchanged
   */
  public void startRunnables() {
    startDiscoveringPeers();
    startProbing();
    startFaceAndRouteConsistencyChecker();
    startGroupConsistencyChecker();
  }

  /**
   * stop all runnable, keep saved Wifi-Direct states unchanged
   */
  public void stopRunnables() {
    startGroupConsistencyChecker();
    stopFaceAndRouteConsistencyChecker();
    stopProbing();
    stopDiscoveringPeers();
  }

  /**
   * Resets all saved states.
   */
  public void cleanUpConnections() {
    unregisterOwnLocalhop();

    // Remove all faces created to peers, and shut down the localhost face we used
    // for communication with NFD.
    // Make use of our handy executor
    Runnable cleanUpRunnable = new Runnable() {
      @Override
      public void run() {
        Log.d(TAG, "before cleaning up connected peers, the size of ipPeerMapOfConnectedPeers is " + ipPeerMapOfConnectedPeers.size());

        for (String peerIp : ipPeerMapOfConnectedPeers.keySet()) {
          try {
            Log.d(TAG, "Cleaning up face towards peer: " + peerIp);
            nfdcHelper.faceDestroy(ipPeerMapOfConnectedPeers.get(peerIp).getFaceId());
          } catch (ManagementException me) {
            Log.e(TAG, "Unable to destroy face to: " + peerIp);
          } catch (Exception e) {
            Log.e(TAG, "Unable to destroy face to: " + peerIp);
          }
        }

        myAddress = null;
        groupOwnerAddress = null;
        hasRegisteredOwnLocalhop = false;
        isGroupOwner = false;
        ipPeerMapOfConnectedPeers.clear();
        discoverdPeers.clear();
      }
    };

    nfdcFaceCommandExecutor.execute(cleanUpRunnable);

    if (faceEventProcessFuture != null) {
      faceEventProcessFuture.cancel(false);
      faceEventProcessFuture = null;
    }
    if (mFace != null) {
      mFace.shutdown();
      mFace = null;
    }
  }

  public void recreateFace() {
    if (faceEventProcessFuture != null) {
      faceEventProcessFuture.cancel(false);
      faceEventProcessFuture = null;
    }
    if (mFace != null) {
      mFace.shutdown();
      mFace = null;
    }
    mFace = new Face();
    faceEventProcessRunnable = new FaceEventProcessRunnable();
    try {
      mFace.setCommandSigningInfo(mKeyChain, mKeyChain.getDefaultCertificateName());
    } catch (SecurityException e) {
      Log.e(TAG, "Unable to set command signing info for localhost face.");
    }
    faceEventProcessFuture = faceEventProcessExecutor.scheduleWithFixedDelay(
      faceEventProcessRunnable, 0, 5, TimeUnit.MILLISECONDS);
    Log.d(TAG, "create face and start to process event");
  }

  /**
   * misc
   **/
  private KeyChain buildTestKeyChain() throws SecurityException {
    MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
    MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
    IdentityManager identityManager = new IdentityManager(identityStorage, privateKeyStorage);
    KeyChain keyChain = new KeyChain(identityManager);
    try {
      keyChain.getDefaultCertificateName();
    } catch (SecurityException e) {
      keyChain.createIdentity(new Name("/test/identity"));
      keyChain.getIdentityManager().setDefaultIdentity(new Name("/test/identity"));
    }
    return keyChain;

  }

  /* In the future, we should allow users to implement this method so they can provide their own keychain */
  public KeyChain getKeyChain() throws SecurityException {
    return buildTestKeyChain();
  }

  public void requestConnectionInfo() {
    wifiP2pManager.requestConnectionInfo(channel, this);
  }

  /**
   * update the connected peers info
   */
  private void updateIpPeerMapOfConnectedPeers() {
    Log.d(TAG, "update IpPeerMapOfConnectedPeers");
    for (String peerIp : ipPeerMapOfConnectedPeers.keySet()) {
      Peer peer = ipPeerMapOfConnectedPeers.get(peerIp);
      String macAddress = IPAddress.getMacFromArpCache(peerIp);
      if (macAddress == null) {
        continue;
      }
      for (WifiP2pDevice one : discoverdPeers) {
        // TODO: figure out why the 13th char in arp table is 8 smaller than that in device info
        if (one.deviceAddress.startsWith(macAddress.substring(0, 12))
          && one.deviceAddress.endsWith(macAddress.substring(13))) {
          peer.setDevice(one);
          break;
        }
      }
    }
  }

  /**
   * Remove the unconnected peers (those peers are not removed by the user, but disconnected for
   * some other reasons, e.g., shut down or out of range) from connected peers map.
   */
  private void removeDisconnectedPeersFromIpPeerMapOfConnectedPeers() {
    Log.d(TAG, "remove unconnected peers from IpPeerMapOfConnectedPeers");
    Log.d(TAG, "before removing, the size of IpPeerMapOfConnectedPeers is " + ipPeerMapOfConnectedPeers.size());
    for (String peerIp : ipPeerMapOfConnectedPeers.keySet()) {
      WifiP2pDevice peer = ipPeerMapOfConnectedPeers.get(peerIp).getDevice();
      if (peer != null && (!discoverdPeers.contains(peer) || peer.status != WifiP2pDevice.CONNECTED)) {
        FaceDestroyRunnable runnable = new FaceDestroyRunnable(ipPeerMapOfConnectedPeers.get(peerIp).getFaceId());
        nfdcFaceCommandExecutor.execute(runnable);
        ipPeerMapOfConnectedPeers.remove(peerIp);
      }
    }
    Log.d(TAG, "after removing, the size of IpPeerMapOfConnectedPeers is " + ipPeerMapOfConnectedPeers.size());
  }

  /**
   * Check Wifi-Direct state, change saved states when needed.
   * (1) When Wifi-Direct is connected, but myAddress is null, request connection info and save states
   * (2) When Wifi-Direct is disconnected, but myAddress is not null, clean all the states
   */
  private void checkConnectionConsistency() {
    Log.d(TAG, "start to check ConnectionConsistency");
    boolean hasConnection = false;
    for (WifiP2pDevice one : discoverdPeers) {
      if (one.status == WifiP2pDevice.CONNECTED) {
        hasConnection = true;
        break;
      }
    }
    if (hasConnection && myAddress == null) {
      requestConnectionInfo();
      return;
    }
    if (!hasConnection && myAddress != null) {
      cleanUpConnections();
      recreateFace();
      return;
    }
  }

  @Override
  public void onPeersAvailable(WifiP2pDeviceList peerList) {
    Log.d(TAG,
      String.format("Peers available: %d", peerList.getDeviceList().size()));

    discoverdPeers.clear();
    discoverdPeers.addAll(peerList.getDeviceList());

    updateIpPeerMapOfConnectedPeers();
    removeDisconnectedPeersFromIpPeerMapOfConnectedPeers();
    checkConnectionConsistency();

    // If an AdapterView is backed by this data, notify it
    // of the change.  For instance, if you have a ListView of available
    // peers, trigger an update.
    if (discoverdPeers.size() == 0) {
      Log.d(TAG, "No devices found");
      return;
    }
  }

  @Override
  public void onConnectionInfoAvailable(WifiP2pInfo info) {
    Log.d(TAG, "connection info is available!!");

    // check if group formation was successful
    if (info.groupFormed) {

      // group owner address
      groupOwnerAddress = info.groupOwnerAddress.getHostAddress();

      // this device's address, which is now available
      myAddress = IPAddress.getLocalIPAddress();
      Log.d(TAG, "My WiFi Direct IP address is: " + myAddress);

      if (!getHasRegisteredOwnLocalhop()) {
        // do so now
        registerOwnLocalhop();
        Log.d(TAG, "registerOwnLocalhop() called...");
      } else {
        Log.d(TAG, "already registered own /localhop prefix.");
      }

      if (info.isGroupOwner) {
        // Do whatever tasks are specific to the group owner.
        // One common case is creating a server thread and accepting
        // incoming connections.
        Log.d(TAG, "I am the group owner, wait for probe interests from peers...");
        setIsGroupOwner(true);
      } else {
        // non group owner
        // The other device acts as the client. In this case,
        // you'll want to create a client thread that connects to the group
        // owner.
        Log.d(TAG, "I am NOT the group owner.");
        setIsGroupOwner(false);

        // create a callback that will register the /localhop/wifidirect/<go-addr> prefix
        GenericCallback cb = new GenericCallback() {
          @Override
          public void doJob() {
            Log.d(TAG, "registering " + NDNController.PROBE_PREFIX + "/" + groupOwnerAddress);
            String[] prefixes = new String[1];
            prefixes[0] = NDNController.PROBE_PREFIX + "/" + groupOwnerAddress;
            ribRegisterPrefix(getFaceIdForPeer(groupOwnerAddress),
              prefixes);
          }
        };

        // create face towards GO, with callback to register /localhop/... prefix
        createFace(groupOwnerAddress, NDNController.URI_TRANSPORT_PREFIX, cb);
      }
    }

  }

  @Override
  public void onChannelDisconnected() {
    cleanUpConnections();
    recreateFace();
  }

  public void connect(WifiP2pDevice peerDevice) {
    WifiP2pConfig config = new WifiP2pConfig();
    config.deviceAddress = peerDevice.deviceAddress;
    config.wps.setup = WpsInfo.PBC;
    wifiP2pManager.connect(channel, config, new WifiP2pManager.ActionListener() {
      @Override
      public void onSuccess() {
        // onReceive() in WDBroadcastReceiver will receive an intent
      }

      @Override
      public void onFailure(int reason) {

        String reasonString = WDBroadcastReceiver
          .getWifiP2pManagerMessageFromReasonCode(reason);

        Log.e(TAG, "There was an issue with initiating connection reason: " + reasonString);
      }
    });
  }
}