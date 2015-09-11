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

import com.intel.jndn.utils.server.RespondWithData;
import com.intel.jndn.utils.server.Server;
import java.io.IOException;

/**
 * Defines the API for a {@link Server} producing {@link Data} packets
 * dynamically; in other words, when an {@link Interest} arrives, this server
 * will run a callback to determine what packet to send back. As good practice,
 * keep callback methods as short as possible.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface DynamicServer extends Server {

  /**
   * Set the callback method to run when an {@link Interest} packet is passed to
   * this server. This method should either return a {@link Data} packet that
   * satisfies the Interest or throw an Exception to avoid sending. Calling this
   * method a second time should replace the callback.
   *
   * @param callback the callback instance
   * @throws java.io.IOException if the server fails to register a prefix
   */
  public void respondUsing(RespondWithData callback) throws IOException;
}
