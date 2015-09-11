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
package com.intel.jndn.utils.server;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnInterest;

/**
 * Base interface for defining a server; see descendant interfaces for different
 * modes of serving packets. This class extends {@link Runnable} expecting
 * implementing classes to do any necessary event processing in the
 * {@link Runnable#run()} block, thus allowing different ways to manage servers
 * (e.g. single-thread vs {@link ScheduledThreadPoolExecutor}.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface Server extends Runnable, OnInterest {

  /**
   * @return the {@link Name} prefix this server is serving on.
   */
  public Name getPrefix();

  /**
   * Add a stage to the server pipeline. Each stage should be processed once the
   * server has the {@link Data} packet available to send (e.g. after a callback
   * has produced a packet); also, stages should be processed in the order they
   * are added.
   *
   * @param pipelineStage a Data-to-Data processing stage
   */
  public void addPipelineStage(PipelineStage<Data, Data> pipelineStage);
}
