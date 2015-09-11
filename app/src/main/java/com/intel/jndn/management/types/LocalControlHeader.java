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
package com.intel.jndn.management.types;

/**
 * Define constants for local control header options. See
 * <a href="http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt#Enable-a-LocalControlHeader-feature">http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt#Enable-a-LocalControlHeader-feature</a>
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public enum LocalControlHeader {

  INCOMING_FACE_ID(1),
  NEXT_HOP_FACE_ID(2),
  CACHING_POLICY(3);

  LocalControlHeader(int value) {
    value_ = value;
  }

  public final int getNumericValue() {
    return value_;
  }
  private final int value_;
}
