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

import com.intel.jndn.utils.client.FutureData;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.OnTimeout;
import java.util.logging.Logger;

/**
 * Provide a client to simplify information retrieval over the NDN network.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SimpleClient implements Client {

  public static final long DEFAULT_SLEEP_TIME = 20;
  public static final long DEFAULT_TIMEOUT = 2000;
  private static final Logger logger = Logger.getLogger(SimpleClient.class.getName());
  private static SimpleClient defaultInstance;

  /**
   * Singleton access for simpler client use
   *
   * @return
   */
  public static SimpleClient getDefault() {
    if (defaultInstance == null) {
      defaultInstance = new SimpleClient();
    }
    return defaultInstance;
  }

  /**
   * Asynchronously request the Data for an Interest. This will send the
   * Interest and return immediately; use futureData.get() to block until the
   * Data returns (see FutureData) or manage the event processing independently.
   *
   * @param face
   * @param interest
   * @return
   */
  @Override
  public Future<Data> getAsync(Face face, Interest interest) {
    final FutureData futureData = new FutureData(face, interest.getName());

    // send interest
    try {
      face.expressInterest(interest, new OnData() {
        @Override
        public void onData(Interest interest, Data data) {
          futureData.resolve(data);
        }
      }, new OnTimeout() {
        @Override
        public void onTimeout(Interest interest) {
          futureData.reject(new TimeoutException());
        }
      });
    } catch (IOException e) {
      logger.log(Level.WARNING, "IO failure while sending interest: ", e);
      futureData.reject(e);
    }

    return futureData;
  }

  /**
   * Synchronously retrieve the Data for a Name using a default interest (e.g. 2
   * second timeout); this will block until complete (i.e. either data is
   * received or the interest times out).
   *
   * @param face
   * @param name
   * @return
   */
  public Future<Data> getAsync(Face face, Name name) {
    return getAsync(face, getDefaultInterest(name));
  }

  /**
   * Synchronously retrieve the Data for an Interest; this will block until
   * complete (i.e. either data is received or the interest times out).
   *
   * @param face
   * @param interest
   * @return Data packet or null
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
   * received or the interest times out).
   *
   * @param face
   * @param name
   * @return
   * @throws java.io.IOException
   */
  public Data getSync(Face face, Name name) throws IOException {
    return getSync(face, getDefaultInterest(name));
  }

  /**
   * Create a default interest for a given Name using some common settings: -
   * lifetime: 2 seconds
   *
   * @param name
   * @return
   */
  public static Interest getDefaultInterest(Name name) {
    Interest interest = new Interest(name, DEFAULT_TIMEOUT);
    return interest;
  }
}
