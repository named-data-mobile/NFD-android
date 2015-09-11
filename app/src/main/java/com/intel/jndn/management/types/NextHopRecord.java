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

import java.nio.ByteBuffer;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.Tlv;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.util.Blob;

/**
 * Represent a NextHopRecord in a FibEntry; see
 * <a href="http://redmine.named-data.net/projects/nfd/wiki/FibMgmt#FIB-Dataset">http://redmine.named-data.net/projects/nfd/wiki/FibMgmt#FIB-Dataset</a>
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class NextHopRecord {

  public final static int TLV_NEXT_HOP_RECORD = 129;

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
    encoder.writeNonNegativeIntegerTlv(Tlv.ControlParameters_Cost, cost);
    encoder.writeNonNegativeIntegerTlv(Tlv.ControlParameters_FaceId, faceId);
    encoder.writeTypeAndLength(TLV_NEXT_HOP_RECORD, encoder.getLength() - saveLength);
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
  public final void wireDecode(TlvDecoder decoder) throws EncodingException {
    int endOffset = decoder.readNestedTlvsStart(TLV_NEXT_HOP_RECORD);
    this.faceId = (int) decoder.readNonNegativeIntegerTlv(Tlv.ControlParameters_FaceId);
    this.cost = (int) decoder.readNonNegativeIntegerTlv(Tlv.ControlParameters_Cost);
    decoder.finishNestedTlvs(endOffset);
  }

  /**
   * Get face ID
   *
   * @return
   */
  public int getFaceId() {
    return faceId;
  }

  /**
   * Set face ID
   *
   * @param faceId
   */
  public void setFaceId(int faceId) {
    this.faceId = faceId;
  }

  /**
   * Get cost
   *
   * @return
   */
  public int getCost() {
    return cost;
  }

  /**
   * Set cost
   *
   * @param cost
   */
  public void setCost(int cost) {
    this.cost = cost;
  }

  private int faceId;
  private int cost;
}
