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

import java.io.IOException;
import java.util.concurrent.Future;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;

/**
 * Base functionality provided by all NDN clients in this package.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface Client {

  /**
   * Asynchronously request the Data for an Interest. This will send the
   * Interest and return immediately; use futureData.get() to block until the
   * Data returns (see Future) or manage the event processing independently.
   *
   * @param face
   * @param interest
   * @return
   */
  public Future<Data> getAsync(Face face, Interest interest);

  /**
   * Synchronously retrieve the Data for an Interest; this will block until
   * complete (i.e. either data is received or the interest times out).
   *
   * @param face
   * @param interest
   * @return
   * @throws java.io.IOException
   */
  public Data getSync(Face face, Interest interest) throws IOException;
}
