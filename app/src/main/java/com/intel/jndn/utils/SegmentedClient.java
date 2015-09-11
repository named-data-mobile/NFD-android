/*
 * jndn-utils
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
package com.intel.jndn.utils;

import com.intel.jndn.utils.client.SegmentedFutureData;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;

import net.named_data.nfd.utils.G;

/**
 * Provide a client to simplify retrieving segmented Data packets over the NDN
 * network. This class expects the Data producer to follow the NDN naming
 * conventions (see http://named-data.net/doc/tech-memos/naming-conventions.pdf)
 * and produce Data packets with a valid segment as the last component of their
 * name; additionally, at least the first packet should set the FinalBlockId of
 * the packet's MetaInfo (see
 * http://named-data.net/doc/ndn-tlv/data.html#finalblockid).
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedClient implements Client {

  private static SegmentedClient defaultInstance;
  private static final Logger logger = Logger.getLogger(SegmentedClient.class.getName());

  /**
   * Singleton access for simpler client use.
   *
   * @return
   */
  public static SegmentedClient getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new SegmentedClient();
    }
    return defaultInstance;
  }

  /**
   * Asynchronously send Interest packets for a segmented result; will block
   * until the first packet is received and then send remaining interests until
   * the specified FinalBlockId.
   *
   * @param face
   * @param interest should include either a ChildSelector or an initial segment
   * number; the initial segment number will be cut off in the de-segmented
   * packet.
   * @return a list of FutureData packets; if the first segment fails, the list
   * will contain one FutureData with the failure exception
   */
  @Override
  public Future<Data> getAsync(Face face, Interest interest) {
    List<Future<Data>> segments = getAsyncList(face, interest);
    Name name = hasSegment(interest.getName()) ? interest.getName().getPrefix(-1) : interest.getName();
    return new SegmentedFutureData(name, segments);
  }

  /**
   * Asynchronously send Interest packets for a segmented result; will block
   * until the first packet is received and then send remaining interests until
   * the specified FinalBlockId.
   *
   * @param face
   * @param name the {@link Name} of the packet to retrieve using a default
   * interest
   * @return an aggregated data packet from all received segments
   */
  public Future<Data> getAsync(Face face, Name name) {
    return getAsync(face, SimpleClient.getDefaultInterest(name));
  }

  /**
   * Asynchronously send Interest packets for a segmented result; will block
   * until the first packet is received and then send remaining interests until
   * the specified FinalBlockId.
   *
   * @param face
   * @param interest should include either a ChildSelector or an initial segment
   * number
   * @return a list of FutureData packets; if the first segment fails, the list
   * will contain one FutureData with the failure exception
   */
  public List<Future<Data>> getAsyncList(Face face, Interest interest) {
    // get first segment; default 0 or use a specified start segment
    long firstSegment = 0;
    boolean specifiedSegment = false;
    try {
      firstSegment = interest.getName().get(-1).toSegment();
      specifiedSegment = true;
    } catch (EncodingException e) {
      // check for interest selector if no initial segment found
      if (interest.getChildSelector() == -1) {
        logger.log(Level.WARNING, "No child selector set for a segmented Interest; this may result in incorrect retrieval.");
        // allow this interest to pass without a segment marker since it may still succeed
      }
    }

    // setup segments
    final List<Future<Data>> segments = new ArrayList<>();
    segments.add(SimpleClient.getDefault().getAsync(face, interest));

    // retrieve first packet to find last segment value
    long lastSegment;
    try {
      G.Log("+++++++ " + segments.get(0).get().getMetaInfo().getFinalBlockId().toEscapedString());
      lastSegment = segments.get(0).get().getMetaInfo().getFinalBlockId().toSegment();
    } catch (ExecutionException | InterruptedException | EncodingException e) {
      logger.log(Level.SEVERE, "Failed to retrieve first segment: ", e);
      return segments;
    }

    // cut interest segment off
    if (specifiedSegment) {
      interest.setName(interest.getName().getPrefix(-1));
    }

    // send interests in remaining segments
    for (long i = firstSegment + 1; i <= lastSegment; i++) {
      Interest segmentedInterest = new Interest(interest);
      segmentedInterest.getName().appendSegment(i);
      Future<Data> futureData = SimpleClient.getDefault().getAsync(face, segmentedInterest);
      segments.add((int) i, futureData);
    }

    return segments;
  }

  /**
   * Asynchronously send Interests for a segmented Data packet using a default
   * interest (e.g. 2 second timeout); this will block until complete (i.e.
   * either data is received or the interest times out). See getAsync(Face face)
   * for more information.
   *
   * @param face
   * @param name
   * @return
   */
  public List<Future<Data>> getAsyncList(Face face, Name name) {
    return getAsyncList(face, SimpleClient.getDefaultInterest(name));
  }

  /**
   * Retrieve a segmented Data packet; will block until all segments are
   * received and will re-assemble these.
   *
   * @param face
   * @param interest should include either a ChildSelector or an initial segment
   * number
   * @return a Data packet; the name will inherit from the sent Interest, not
   * the returned packets and the content will be a concatenation of all of the
   * packet contents.
   * @throws java.io.IOException
   */
  @Override
  public Data getSync(Face face, Interest interest) throws IOException {
    try {
      return getAsync(face, interest).get();
    } catch (ExecutionException | InterruptedException e) {
      logger.log(Level.WARNING, "Failed to retrieve data.", e);
      throw new IOException("Failed to retrieve data.", e);
    }
  }

  /**
   * Synchronously retrieve the Data for a Name using a default interest (e.g. 2
   * second timeout); this will block until complete (i.e. either data is
   * received or the interest times out). See getSync(Face face) for more
   * information.
   *
   * @param face
   * @param name
   * @return
   * @throws java.io.IOException
   */
  public Data getSync(Face face, Name name) throws IOException {
    return getSync(face, SimpleClient.getDefaultInterest(name));
  }

  /**
   * Check if a name ends in a segment component; uses marker value found in the
   * NDN naming conventions (see
   * http://named-data.net/doc/tech-memos/naming-conventions.pdf).
   *
   * @param name
   * @return
   */
  public static boolean hasSegment(Name name) {
    return name.get(-1).getValue().buf().get(0) == 0x00;
  }
}
