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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 * Represents a list of Packets that have been requested asynchronously and have
 * yet to be returned from the network. Usage:
 *
 * <pre><code>
 * SegmentedFutureData segmentedFutureData = new SegmentedFutureData(face, name, futureDataList);
 * Data data = segmentedFutureData.get(); // will block until complete
 * </code></pre>
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedFutureData implements Future<Data> {

  private final Name name;
  List<Future<Data>> segments;
  private boolean cancelled = false;

  /**
   * Constructor
   *
   * @param name this will be the name of the returned Data packet, regardless
   * of suffixes (e.g. segment components) on each segment packet
   * @param segments
   */
  public SegmentedFutureData(Name name, List<Future<Data>> segments) {
    this.name = name;
    this.segments = segments;
  }

  /**
   * Get the Interest name; this will also be the name of the Data packet
   * returned from get().
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
    // check for errors, cancellation
    if (isCancelled()) {
      return true;
    }

    // check each segment for completion
    for (Future<Data> futureData : segments) {
      if (!futureData.isDone()) {
        return false;
      }
    }

    return true;
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
    // aggregate bytes
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    for (Future<Data> futureData : segments) {
      try {
        content.write(futureData.get().getContent().getImmutableArray());
      } catch (ExecutionException | IOException | InterruptedException e) {
        throw new ExecutionException("Failed while aggregating retrieved packets.", e);
      }
    }

    // build aggregated packet (copy first packet)
    Data data = new Data(segments.get(0).get());
    data.setName(getName());
    data.setContent(new Blob(content.toByteArray()));
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

    // aggregate bytes
    ByteArrayOutputStream content = new ByteArrayOutputStream();
    for (Future<Data> futureData : segments) {
      try {
        content.write(futureData.get().getContent().getImmutableArray());
      } catch (ExecutionException | IOException | InterruptedException e) {
        throw new ExecutionException("Failed while aggregating retrieved packets.", e);
      }

      // check for timeout
      if (System.currentTimeMillis() > endTime) {
        throw new TimeoutException("Timed out.");
      }
    }

    // build aggregated packet (copy first packet)
    Data data = new Data(segments.get(0).get());
    data.setName(getName());
    data.setContent(new Blob(content.toByteArray()));
    return data;
  }
}
