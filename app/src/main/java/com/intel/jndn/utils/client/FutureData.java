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
package com.intel.jndn.utils.client;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;

/**
 * Reference to a Packet that has yet to be returned from the network. Usage:
 *
 * <pre><code>
 * FutureData futureData = new FutureData(face, interest.getName());
 * face.expressInterest(interest, new OnData(){
 *	... futureData.resolve(data); ...
 * }, new OnTimeout(){
 *  ... futureData.reject(new TimeoutException());
 * });
 * Data resolvedData = futureData.get(); // will block and call face.processEvents() until complete
 * </code></pre>
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class FutureData implements Future<Data> {

  protected final Face face;
  private final Name name;
  private Data data;
  private boolean cancelled = false;
  private Throwable error;

  /**
   * Constructor
   *
   * @param face
   * @param name
   */
  public FutureData(Face face, Name name) {
    this.face = face;
    this.name = new Name(name);
  }

  /**
   * Get the Interest name.
   *
   * @return
   */
  public Name getName() {
    return name;
  }

  /**
   * Cancel the current request.
   *
   * @param mayInterruptIfRunning
   * @return
   */
  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    cancelled = true;
    return cancelled;
  }

  /**
   * Determine if this request is cancelled.
   *
   * @return
   */
  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  /**
   * Determine if the request has completed (successfully or not).
   *
   * @return
   */
  @Override
  public boolean isDone() {
    return data != null || error != null || isCancelled();
  }

  /**
   * Set the packet when successfully retrieved; unblocks get().
   *
   * @param d
   */
  public void resolve(Data d) {
    data = d;
  }

  /**
   * Set the exception when request failed; unblocks get().
   *
   * @param e
   */
  public void reject(Throwable e) {
    error = e;
  }

  /**
   * Block until packet is retrieved.
   *
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   */
  @Override
  public Data get() throws InterruptedException, ExecutionException {
    while (!isDone()) {
      // process face events
      try {
        synchronized (face) {
          face.processEvents();
        }
      } catch (EncodingException | IOException e) {
        throw new ExecutionException("Failed to retrieve packet.", e);
      }
    }

    // case: cancelled
    if (cancelled) {
      throw new InterruptedException("Interrupted by user.");
    }

    // case: error
    if (error != null) {
      throw new ExecutionException("Future rejected with error.", error);
    }

    return data;
  }

  /**
   * Block until packet is retrieved or timeout is reached.
   *
   * @param timeout
   * @param unit
   * @return
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws TimeoutException
   */
  @Override
  public Data get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    long interval = TimeUnit.MILLISECONDS.convert(timeout, unit);
    long endTime = System.currentTimeMillis() + interval;
    long currentTime = System.currentTimeMillis();
    while (!isDone() && !isCancelled() && currentTime <= endTime) {
      // process face events
      try {
        synchronized (face) {
          face.processEvents();
        }
      } catch (EncodingException | IOException e) {
        throw new ExecutionException("Failed to retrieve packet.", e);
      }

      currentTime = System.currentTimeMillis();
    }

    // case: cancelled
    if (cancelled) {
      throw new InterruptedException("Interrupted by user.");
    }

    // case: error
    if (error != null) {
      throw new ExecutionException("Future rejected with error.", error);
    }

    // case: timed out
    if (currentTime > endTime) {
      throw new TimeoutException("Timed out.");
    }

    return data;
  }
}
