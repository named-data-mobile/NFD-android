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

import com.intel.jndn.management.EncodingHelper;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.util.Blob;

/**
 * Represent a entry in the RIB; see
 * <a href="http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#RIB-Dataset">http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#RIB-Dataset</a>
 * for details
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class RibEntry implements Decodable {

  /**
   * TLV type, see
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#TLV-TYPE-assignments">http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#TLV-TYPE-assignments</a>
   */
  public final static int TLV_RIB_ENTRY = 128;

  /**
   * Encode using a new TLV encoder.
   *
   * @return The encoded buffer.
   */
  public final Blob wireEncode() {
    TlvEncoder encoder = new TlvEncoder();
    wireEncode(encoder);
    return new Blob(encoder.getOutput(), false);
  }

  /**
   * Encode as part of an existing encode context.
   *
   * @param encoder
   */
  public final void wireEncode(TlvEncoder encoder) {
    int saveLength = encoder.getLength();
    for (Route route : routes) {
      route.wireEncode(encoder);
    }
    EncodingHelper.encodeName(name, encoder);
    encoder.writeTypeAndLength(TLV_RIB_ENTRY, encoder.getLength() - saveLength);
  }

  /**
   * Decode the input from its TLV format.
   *
   * @param input The input buffer to decode. This reads from position() to
   * limit(), but does not change the position.
   * @throws EncodingException For invalid encoding.
   */
  public final void wireDecode(ByteBuffer input) throws EncodingException {
    TlvDecoder decoder = new TlvDecoder(input);
    wireDecode(decoder);
  }

  /**
   * Decode as part of an existing decode context.
   *
   * @param decoder
   * @throws EncodingException
   */
  @Override
  public final void wireDecode(TlvDecoder decoder) throws EncodingException {
    int endOffset = decoder.readNestedTlvsStart(TLV_RIB_ENTRY);
    name = EncodingHelper.decodeName(decoder);
    while (decoder.getOffset() < endOffset) {
      Route route = new Route();
      route.wireDecode(decoder);
      routes.add(route);
    }
    decoder.finishNestedTlvs(endOffset);
  }

  /**
   * Get name
   *
   * @return
   */
  public Name getName() {
    return name;
  }

  /**
   * Set name
   *
   * @param name
   */
  public void setName(Name name) {
    this.name = name;
  }

  /**
   * Get routes
   *
   * @return
   */
  public List<Route> getRoutes() {
    return routes;
  }

  /**
   * Set routes
   *
   * @param routes
   */
  public void setRoutes(List<Route> routes) {
    this.routes = routes;
  }

  private Name name = new Name();
  private List<Route> routes = new ArrayList<>();
}
