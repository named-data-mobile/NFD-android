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

import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvDecoder;

/**
 * Interface used by StatusDataset to decode generic message types; if they are
 * Decodable, then StatusDataset will instantiate and decode them.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public interface Decodable {

  public void wireDecode(TlvDecoder decoder) throws EncodingException;
}
