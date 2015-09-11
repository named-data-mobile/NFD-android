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

import com.intel.jndn.utils.repository.ForLoopRepository;
import com.intel.jndn.utils.repository.Repository;
import com.intel.jndn.utils.server.SegmentedServerHelper;
import com.intel.jndn.utils.server.ServerBaseImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.transport.Transport;

/**
 * Implementation of a {@link RepositoryServer} that segments packets stored in
 * its repository.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedServer extends ServerBaseImpl implements RepositoryServer {

  private static final Logger logger = Logger.getLogger(SegmentedClient.class.getName());
  private final Repository repository = new ForLoopRepository();

  /**
   * {@inheritDoc}
   */
  public SegmentedServer(Face face, Name prefix) {
    super(face, prefix);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void serve(Data data) throws IOException {
    if (!isRegistered()) {
      register();
    }

    InputStream stream = new ByteArrayInputStream(data.getContent().getImmutableArray());
    List<Data> segments = SegmentedServerHelper.segment(data, stream);
    for (Data segment : segments) {
      logger.info("Added segment: " + segment.getName().toUri());
      repository.put(segment);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onInterest(Name prefix, Interest interest, Transport transport, long registeredPrefixId) {
    if (interest.getChildSelector() == -1) {
      try {
        interest.getName().get(-1).toSegment();
      } catch (EncodingException e) {
        interest.setChildSelector(Interest.CHILD_SELECTOR_LEFT);
      }
    }

    try {
      Data data = repository.get(interest);
      data = processPipeline(data);
      ByteBuffer buffer = data.wireEncode().buf();
      transport.send(buffer);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to send data for: " + interest.toUri(), e);
    }
  }
}
