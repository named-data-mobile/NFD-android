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

import net.named_data.jndn.encoding.tlv.Tlv;
import java.nio.ByteBuffer;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.util.Blob;

/**
 * Represent a Route object from /localhost/nfd/rib/list; see
 * <a href="http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#RIB-Dataset">http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#RIB-Dataset</a>
 * for details.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class Route {

  /**
   * TLV type, see
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#TLV-TYPE-assignments">http://redmine.named-data.net/projects/nfd/wiki/RibMgmt#TLV-TYPE-assignments</a>
   */
  public final static int TLV_ROUTE = 129;

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
    encoder.writeOptionalNonNegativeIntegerTlv(Tlv.ControlParameters_ExpirationPeriod, faceId);
    encoder.writeNonNegativeIntegerTlv(Tlv.ControlParameters_Flags, flags.getForwardingEntryFlags());
    encoder.writeNonNegativeIntegerTlv(Tlv.ControlParameters_Cost, cost);
    encoder.writeNonNegativeIntegerTlv(Tlv.ControlParameters_Origin, origin);
    encoder.writeNonNegativeIntegerTlv(Tlv.ControlParameters_FaceId, faceId);
    encoder.writeTypeAndLength(TLV_ROUTE, encoder.getLength() - saveLength);
  }

  /**
   * Decode the input from its TLV format.
   *
   * @param input The input buffer to decode. This reads from position() to
   * limit(), but does not change the position.
   * @throws net.named_data.jndn.encoding.EncodingException
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
    int endOffset = decoder.readNestedTlvsStart(TLV_ROUTE);
    this.faceId = (int) decoder.readNonNegativeIntegerTlv(Tlv.ControlParameters_FaceId);
    this.origin = (int) decoder.readNonNegativeIntegerTlv(Tlv.ControlParameters_Origin);
    this.cost = (int) decoder.readNonNegativeIntegerTlv(Tlv.ControlParameters_Cost);
    this.flags.setForwardingEntryFlags((int) decoder.readNonNegativeIntegerTlv(Tlv.ControlParameters_Flags));
    this.expirationPeriod = (int) decoder.readOptionalNonNegativeIntegerTlv(Tlv.ControlParameters_ExpirationPeriod, endOffset);
    decoder.finishNestedTlvs(endOffset);
  }

  /**
   * Get Face ID
   *
   * @return
   */
  public int getFaceId() {
    return faceId;
  }

  /**
   * Set Face ID
   *
   * @param faceId
   */
  public void setFaceId(int faceId) {
    this.faceId = faceId;
  }

  /**
   * Get origin
   *
   * @return
   */
  public int getOrigin() {
    return origin;
  }

  /**
   * Set origin
   *
   * @param origin
   */
  public void setOrigin(int origin) {
    this.origin = origin;
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

  /**
   * Get flags
   *
   * @return
   */
  public ForwardingFlags getFlags() {
    return flags;
  }

  /**
   * Set flags
   *
   * @param flags
   */
  public void setFlags(ForwardingFlags flags) {
    this.flags = flags;
  }

  /**
   * Get expiration period
   *
   * @return
   */
  public double getExpirationPeriod() {
    return expirationPeriod;
  }

  /**
   * Set expiration period
   *
   * @param expirationPeriod
   */
  public void setExpirationPeriod(double expirationPeriod) {
    this.expirationPeriod = expirationPeriod;
  }

  private int faceId = -1;
  private int origin = -1;
  private int cost = -1;
  private ForwardingFlags flags = new ForwardingFlags();
  private double expirationPeriod = -1.0;
}
