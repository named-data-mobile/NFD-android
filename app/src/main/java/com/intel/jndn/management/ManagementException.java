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

import com.intel.jndn.management.types.ControlResponse;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

/**
 * Represent a failure to correctly manage the NDN Forwarding Daemon (NFD).
 * Inspect this object with getCause() to see why the management operation
 * failed.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ManagementException extends Exception {

  /**
   *
   * @param message
   */
  public ManagementException(String message) {
    super(message);
  }

  /**
   *
   * @param message
   * @param cause
   */
  public ManagementException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Parse an NFD response to create a ManagementException.
   *
   * @param forwarderResponse
   * @return
   */
  public static ManagementException fromResponse(Blob forwarderResponse) {
    ControlResponse response = new ControlResponse();
    try {
      response.wireDecode(forwarderResponse.buf());
      String message = "Action failed, forwarder returned: " + response.getStatusCode() + " " + response.getStatusText();
      return new ManagementException(message);
    } catch (EncodingException e) {
      return new ManagementException("Action failed and forwarder response was unparseable.", e);
    }
  }

  /**
   * Parse an NFD response to create a ManagementException.
   *
   * @param forwarderResponse
   * @return
   */
  public static ManagementException fromResponse(ControlResponse response) {
    String message = "Action failed, forwarder returned: " + response.getStatusCode() + " " + response.getStatusText();
    return new ManagementException(message);
  }
}
