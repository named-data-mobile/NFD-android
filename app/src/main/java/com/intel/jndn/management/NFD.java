/*
 * jndn-management
 * Copyright (c) 2015, Intel Corporation.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms and conditions of the GNU Lesser General Public License,
 * version 3, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details.
 */
package com.intel.jndn.management;

import com.intel.jndn.management.types.StatusDataset;
import com.intel.jndn.management.types.ControlResponse;
import com.intel.jndn.management.types.FaceStatus;
import com.intel.jndn.management.types.FibEntry;
import com.intel.jndn.management.types.ForwarderStatus;
import com.intel.jndn.management.types.LocalControlHeader;
import com.intel.jndn.management.types.RibEntry;
import com.intel.jndn.utils.SimpleClient;
import com.intel.jndn.utils.SegmentedClient;
import java.io.IOException;
import java.util.List;
import net.named_data.jndn.ControlParameters;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.security.SecurityException;
import java.util.logging.Logger;

import net.named_data.nfd.utils.G;

/**
 * Helper class for interacting with an NDN forwarder daemon; see
 * <a href="http://redmine.named-data.net/projects/nfd/wiki/Management">http://redmine.named-data.net/projects/nfd/wiki/Management</a>
 * for explanations of the various protocols used.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class NFD {

  public final static long DEFAULT_TIMEOUT = 2000;
  public final static int OK_STATUS = 200;
  static private final Logger logger = Logger.getLogger(NFD.class.getName());

  /**
   * Ping a forwarder on an existing face to verify that the forwarder is
   * working and responding to requests; this version sends a discovery packet
   * to /localhost/nfd which should always respond if the requestor is on the
   * same machine as the NDN forwarding daemon.
   *
   * @param face only a localhost Face
   * @return true if successful, false otherwise
   */
  public static boolean pingLocal(Face face) {
    return ping(face, new Name("/localhost/nfd"));
  }

  /**
   * Request a name on an existing face to verify the forwarder is working and
   * responding to requests. Note that the name must be served or cached on the
   * forwarder for this to return true.
   *
   * @param face
   * @param name
   * @return true if successful, false otherwise
   */
  public static boolean ping(Face face, Name name) {
    // build interest
    Interest interest = new Interest(name);
    interest.setInterestLifetimeMilliseconds(DEFAULT_TIMEOUT);
    interest.setMustBeFresh(true);

    // send packet
    try {
      Data data = SimpleClient.getDefault().getSync(face, interest);
      return data != null;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Retrieve the status of the given forwarder; calls /localhost/nfd/status
   * which requires a local Face (all non-local packets are dropped)
   *
   * @param forwarder only a localhost Face
   * @return the forwarder status object, see
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/ForwarderStatus">
   * http://redmine.named-data.net/projects/nfd/wiki/ForwarderStatus</a>.
   * @throws java.lang.Exception
   */
  public static ForwarderStatus getForwarderStatus(Face forwarder) throws Exception {
    Interest interest = new Interest(new Name("/localhost/nfd/status"));
    interest.setMustBeFresh(true);
    interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
    interest.setInterestLifetimeMilliseconds(DEFAULT_TIMEOUT);

    Data data = SimpleClient.getDefault().getSync(forwarder, interest);
    G.Log("-----------" + data.getName().toUri());
    ForwarderStatus status = new ForwarderStatus();
    status.wireDecode(data.getContent().buf());
    G.Log("----------- done");
    return status;
  }

  /**
   * Retrieve a list of faces and their status from the given forwarder; calls
   * /localhost/nfd/faces/list which requires a local Face (all non-local
   * packets are dropped)
   *
   * @param forwarder only a localhost Face
   * @return a list of face status objects, see
   * http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt.
   * @throws java.lang.Exception
   */
  public static List<FaceStatus> getFaceList(Face forwarder) throws Exception {
    Data data = retrieveDataSet(forwarder, new Name("/localhost/nfd/faces/list"));
    return StatusDataset.wireDecode(data.getContent(), FaceStatus.class);
  }

  /**
   * Retrieve a list of FIB entries and their NextHopRecords from the given
   * forwarder; calls /localhost/nfd/fib/list which requires a local Face (all
   * non-local packets are dropped).
   *
   * @param forwarder only a localhost Face
   * @return a list of FIB entries, see
   * http://redmine.named-data.net/projects/nfd/wiki/FibMgmt#FIB-Dataset.
   * @throws java.lang.Exception
   */
  public static List<FibEntry> getFibList(Face forwarder) throws Exception {
    Data data = retrieveDataSet(forwarder, new Name("/localhost/nfd/fib/list"));
    return StatusDataset.wireDecode(data.getContent(), FibEntry.class);
  }

  /**
   * Retrieve a list of routing entries from the RIB; calls
   * /localhost/nfd/rib/list which requires a local Face (all non-local packets
   * are dropped).
   *
   * @param forwarder only a localhost Face
   * @return a list of RIB entries, i.e. routes, see
   * http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#RIB-Dataset.
   * @throws java.lang.Exception
   */
  public static List<RibEntry> getRouteList(Face forwarder) throws Exception {
    Data data = retrieveDataSet(forwarder, new Name("/localhost/nfd/rib/list"));
    return StatusDataset.wireDecode(data.getContent(), RibEntry.class);
  }

  /**
   * Helper method to register a new face on the forwarder; as mentioned at
   * <a href="http://named-data.net/doc/NFD/current/manpages/nfdc.html">http://named-data.net/doc/NFD/current/manpages/nfdc.html</a>,
   * this is more for debugging; use 'register' instead
   *
   * @param forwarder only a localhost Face
   * @param faceId
   * @param prefix
   * @throws java.lang.Exception
   */
  public static void addNextHop(Face forwarder, int faceId, Name prefix) throws Exception {
    // build command name
    Name command = new Name("/localhost/nfd/fib/add-nexthop");
    ControlParameters parameters = new ControlParameters();
    parameters.setName(prefix);
    parameters.setFaceId(faceId);
    command.append(parameters.wireEncode());

    // send the interest
    sendCommand(forwarder, new Interest(command));
  }

  /**
   * Create a new face on the given forwarder. Ensure the forwarding face is on
   * the local machine (management requests are to /localhost/...) and that
   * command signing has been set up (e.g. forwarder.setCommandSigningInfo()).
   *
   * @param forwarder only a localhost Face
   * @param uri
   * @return
   * @throws java.lang.Exception
   */
  public static int createFace(Face forwarder, String uri) throws Exception {
    Name command = new Name("/localhost/nfd/faces/create");
    ControlParameters parameters = new ControlParameters();
    parameters.setUri(uri);
    command.append(parameters.wireEncode());

    // send the interest
    ControlResponse response = sendCommand(forwarder, new Interest(command));

    // return
    return response.getBody().get(0).getFaceId();
  }

  /**
   * Destroy a face on given forwarder. Ensure the forwarding face is on the
   * local machine (management requests are to /localhost/...) and that command
   * signing has been set up (e.g. forwarder.setCommandSigningInfo()).
   *
   * @param forwarder only a localhost Face
   * @param faceId
   * @throws java.lang.Exception
   */
  public static void destroyFace(Face forwarder, int faceId) throws Exception {
    Name command = new Name("/localhost/nfd/faces/destroy");
    ControlParameters parameters = new ControlParameters();
    parameters.setFaceId(faceId);
    command.append(parameters.wireEncode());

    // send the interest
    sendCommand(forwarder, new Interest(command));
  }

  /**
   * Enable a local control feature on the given forwarder. See
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt#Enable-a-LocalControlHeader-feature">http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt#Enable-a-LocalControlHeader-feature</a>
   *
   * @param forwarder
   * @param header
   * @throws Exception
   */
  public static void enableLocalControlHeader(Face forwarder, LocalControlHeader header) throws Exception {
    // build command name
    Name command = new Name("/localhost/nfd/faces/enable-local-control");
    ControlParameters parameters = new ControlParameters();
    parameters.setLocalControlFeature(header.getNumericValue());
    command.append(parameters.wireEncode());

    // send command and return
    sendCommand(forwarder, new Interest(command));
  }

  /**
   * Disable a local control feature on the given forwarder. See
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt#Disable-a-LocalControlHeader-feature">http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt#Disable-a-LocalControlHeader-feature</a>
   *
   * @param forwarder
   * @param header
   * @throws Exception
   */
  public static void disableLocalControlHeader(Face forwarder, LocalControlHeader header) throws Exception {
    // build command name
    Name command = new Name("/localhost/nfd/faces/disable-local-control");
    ControlParameters parameters = new ControlParameters();
    parameters.setLocalControlFeature(header.getNumericValue());
    command.append(parameters.wireEncode());

    // send command and return
    sendCommand(forwarder, new Interest(command));
  }

  /**
   * Register a route on the forwarder; see
   * <a href="http://named-data.net/doc/NFD/current/manpages/nfdc.html">http://named-data.net/doc/NFD/current/manpages/nfdc.html</a>
   * for command-line usage and
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/RibMgmt">http://redmine.named-data.net/projects/nfd/wiki/RibMgmt</a>
   * for protocol documentation. Ensure the forwarding face is on the local
   * machine (management requests are to /localhost/...) and that command
   * signing has been set up (e.g. forwarder.setCommandSigningInfo()).
   *
   * @param forwarder only a localhost Face
   * @param controlParameters
   * @throws Exception
   */
  public static void register(Face forwarder, ControlParameters controlParameters) throws Exception {
    // build command name
    Name command = new Name("/localhost/nfd/rib/register");
    command.append(controlParameters.wireEncode());

    // send the interest
    sendCommand(forwarder, new Interest(command));
  }

  /**
   * Register a route on a forwarder; this will create a new face on the
   * forwarder to the given URI/route pair. See register(Face,
   * ControlParameters) for more details documentation.
   *
   * @param forwarder only a localhost Face
   * @param uri
   * @param cost
   * @param route
   * @throws java.lang.Exception
   */
  public static void register(Face forwarder, String uri, Name route, int cost) throws Exception {
    // create the new face
    int faceId = createFace(forwarder, uri);

    // run base method
    register(forwarder, faceId, route, cost);
  }

  /**
   * Register a route on a forwarder; this will not create a new face since it
   * is provided a faceId. See register(Face, ControlParameters) for full
   * documentation
   *
   * @param forwarder only a localhost Face
   * @param faceId
   * @param route
   * @param cost
   * @throws java.lang.Exception
   */
  public static void register(Face forwarder, int faceId, Name route, int cost) throws Exception {
    // build command name
    ControlParameters parameters = new ControlParameters();
    parameters.setName(route);
    parameters.setFaceId(faceId);
    parameters.setCost(cost);
    ForwardingFlags flags = new ForwardingFlags();
    flags.setCapture(true);
    flags.setChildInherit(true);
    parameters.setForwardingFlags(flags);

    // run base method
    register(forwarder, parameters);
  }

  /**
   * Unregister a route on a forwarder; see
   * <a href="http://named-data.net/doc/NFD/current/manpages/nfdc.html">http://named-data.net/doc/NFD/current/manpages/nfdc.html</a>
   * for command-line usage and
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/RibMgmt">http://redmine.named-data.net/projects/nfd/wiki/RibMgmt</a>
   * for protocol documentation. Ensure the forwarding face is on the local
   * machine (management requests are to /localhost/...) and that command
   * signing has been set up (e.g. forwarder.setCommandSigningInfo()
   *
   * @param forwarder
   * @param controlParameters
   * @throws java.lang.Exception
   */
  public static void unregister(Face forwarder, ControlParameters controlParameters) throws Exception {
    // build command name
    Name command = new Name("/localhost/nfd/rib/unregister");
    command.append(controlParameters.wireEncode());

    // send the interest
    sendCommand(forwarder, new Interest(command));
  }

  /**
   * Unregister a route on a forwarder; see
   * <a href="http://named-data.net/doc/NFD/current/manpages/nfdc.html">http://named-data.net/doc/NFD/current/manpages/nfdc.html</a>
   * for command-line usage and
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/RibMgmt">http://redmine.named-data.net/projects/nfd/wiki/RibMgmt</a>
   * for protocol documentation. Ensure the forwarding face is on the local
   * machine (management requests are to /localhost/...) and that command
   * signing has been set up (e.g. forwarder.setCommandSigningInfo()
   *
   * @param forwarder
   * @param route
   * @throws java.lang.Exception
   */
  public static void unregister(Face forwarder, Name route) throws Exception {
    // build command name
    ControlParameters controlParameters = new ControlParameters();
    controlParameters.setName(route);

    // send the interest
    unregister(forwarder, controlParameters);
  }

  /**
   * Unregister a route on a forwarder; see
   * <a href="http://named-data.net/doc/NFD/current/manpages/nfdc.html">http://named-data.net/doc/NFD/current/manpages/nfdc.html</a>
   * for command-line usage and
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/RibMgmt">http://redmine.named-data.net/projects/nfd/wiki/RibMgmt</a>
   * for protocol documentation. Ensure the forwarding face is on the local
   * machine (management requests are to /localhost/...) and that command
   * signing has been set up (e.g. forwarder.setCommandSigningInfo()
   *
   * @param forwarder
   * @param route
   * @param faceId
   * @throws java.lang.Exception
   */
  public static void unregister(Face forwarder, Name route, int faceId) throws Exception {
    // build command name
    ControlParameters controlParameters = new ControlParameters();
    controlParameters.setName(route);
    controlParameters.setFaceId(faceId);

    // send the interest
    unregister(forwarder, controlParameters);
  }

  /**
   * Unregister a route on a forwarder; see
   * <a href="http://named-data.net/doc/NFD/current/manpages/nfdc.html">http://named-data.net/doc/NFD/current/manpages/nfdc.html</a>
   * for command-line usage and
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/RibMgmt">http://redmine.named-data.net/projects/nfd/wiki/RibMgmt</a>
   * for protocol documentation. Ensure the forwarding face is on the local
   * machine (management requests are to /localhost/...) and that command
   * signing has been set up (e.g. forwarder.setCommandSigningInfo()
   *
   * @param forwarder
   * @param route
   * @param uri
   * @throws java.lang.Exception
   */
  public static void unregister(Face forwarder, Name route, String uri) throws Exception {
    int faceId = -1;
    for (FaceStatus face : getFaceList(forwarder)) {
      if (face.getUri().matches(uri)) {
        faceId = face.getFaceId();
        break;
      }
    }

    if (faceId == -1) {
      throw new ManagementException("Face not found: " + uri);
    }

    // send the interest
    unregister(forwarder, route, faceId);
  }

  /**
   * Set a strategy on the forwarder; see
   * <a href="http://named-data.net/doc/NFD/current/manpages/nfdc.html">http://named-data.net/doc/NFD/current/manpages/nfdc.html</a>
   * for command-line usage and
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/StrategyChoice">http://redmine.named-data.net/projects/nfd/wiki/StrategyChoice</a>
   * for protocol documentation. Ensure the forwarding face is on the local
   * machine (management requests are to /localhost/...) and that command
   * signing has been set up (e.g. forwarder.setCommandSigningInfo()).
   *
   * @param forwarder only a localhost Face
   * @param prefix
   * @param strategy
   * @throws Exception
   */
  public static void setStrategy(Face forwarder, Name prefix, Name strategy) throws Exception {
    // build command name
    Name command = new Name("/localhost/nfd/strategy-choice/set");
    ControlParameters parameters = new ControlParameters();
    parameters.setName(prefix);
    parameters.setStrategy(strategy);
    command.append(parameters.wireEncode());

    // send the interest
    sendCommand(forwarder, new Interest(command));
  }

  /**
   * Build an interest to retrieve a segmented data set from the NFD; for
   * details on the DataSet, see
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/StatusDataset">http://redmine.named-data.net/projects/nfd/wiki/StatusDataset</a>
   *
   * @param forwarder
   * @param datasetName
   * @return
   * @throws IOException
   * @throws ManagementException
   */
  public static Data retrieveDataSet(Face forwarder, Name datasetName) throws IOException, ManagementException {
    // build management Interest packet; see <a href="http://redmine.named-data.net/projects/nfd/wiki/StatusDataset">http://redmine.named-data.net/projects/nfd/wiki/StatusDataset</a>
    Interest interest = new Interest(datasetName);
    interest.setMustBeFresh(true);
    interest.setChildSelector(Interest.CHILD_SELECTOR_RIGHT);
    interest.setInterestLifetimeMilliseconds(DEFAULT_TIMEOUT);

    // send packet
    Data data = SegmentedClient.getDefault().getSync(forwarder, interest);

    // check for failed request
    if (data.getContent().buf().get(0) == ControlResponse.TLV_CONTROL_RESPONSE) {
      throw ManagementException.fromResponse(data.getContent());
    }

    return data;
  }

  /**
   * Send an interest as a command to the forwarder; this method will convert
   * the interest to a command interest and block until a response is received
   * from the forwarder. Ensure the forwarding face is on the local machine
   * (management requests are to /localhost/...) and that command signing has
   * been set up (e.g. forwarder.setCommandSigningInfo()).
   *
   * @param forwarder only a localhost Face, command signing info must be set
   * @param interest As described at
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/ControlCommand,">http://redmine.named-data.net/projects/nfd/wiki/ControlCommand,</a>
   * the requested interest must have encoded ControlParameters appended to the
   * interest name
   * @return
   * @throws java.io.IOException
   * @throws net.named_data.jndn.encoding.EncodingException
   * @throws com.intel.jndn.management.ManagementException
   */
  public static ControlResponse sendCommand(Face forwarder, Interest interest) throws IOException, EncodingException, ManagementException {
    // forwarder must have command signing info set
    try {
      forwarder.makeCommandInterest(interest);
    } catch (SecurityException e) {
      throw new IllegalArgumentException("Failed to make command interest; ensure command signing info is set on the face.", e);
    }

    // send command packet
    Data data = SimpleClient.getDefault().getSync(forwarder, interest);

    // decode response
    ControlResponse response = new ControlResponse();
    response.wireDecode(data.getContent().buf());

    // check response for success
    if (response.getStatusCode() != OK_STATUS) {
      throw ManagementException.fromResponse(response);
    }

    return response;
  }
}
