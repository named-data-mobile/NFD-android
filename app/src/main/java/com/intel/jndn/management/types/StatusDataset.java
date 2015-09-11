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

import com.intel.jndn.management.ManagementException;
import java.util.ArrayList;
import java.util.List;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.util.Blob;

/**
 * Helper class to handle StatusDatasets, see
 * <a href="http://redmine.named-data.net/projects/nfd/wiki/StatusDataset">http://redmine.named-data.net/projects/nfd/wiki/StatusDataset</a>
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class StatusDataset {

  /**
   * Decode multiple status entries as part of a StatusDataset, see
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/StatusDataset">http://redmine.named-data.net/projects/nfd/wiki/StatusDataset</a>
   *
   * @param <T>
   * @param statusDataset
   * @param type
   * @return
   * @throws com.intel.jndn.management.ManagementException
   */
  public static final <T extends Decodable> List<T> wireDecode(Blob statusDataset, Class<T> type) throws ManagementException {
    List<T> entries = new ArrayList<>();
    int endOffset = statusDataset.size();
    TlvDecoder decoder = new TlvDecoder(statusDataset.buf());
    while (decoder.getOffset() < endOffset) {
      try {
        T entry = type.newInstance();
        entry.wireDecode(decoder);
        entries.add(entry);
      } catch (EncodingException | IllegalAccessException | InstantiationException e) {
        throw new ManagementException("Failed to read status dataset.", e);
      }
    }
    return entries;
  }
}
