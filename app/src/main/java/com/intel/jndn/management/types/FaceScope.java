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
 * Indicate whether the face is local for scope control purposes; used by
 * FaceStatus See
 * <a href="http://redmine.named-data.net/projects/nfd/widi/FaceMgmt">http://redmine.named-data.net/projects/nfd/widi/FaceMgmt</a>
 *
 * @author andrew
 */
public enum FaceScope {

  LOCAL(0),
  NON_LOCAL(1);

  FaceScope(int value) {
    value_ = value;
  }

  public final int getNumericValue() {
    return value_;
  }
  private final int value_;
}
