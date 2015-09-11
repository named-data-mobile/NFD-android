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
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.util.Blob;

/**
 * Represent a FaceStatus object from /localhost/nfd/faces/list; see
 * <a href="http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt">http://redmine.named-data.net/projects/nfd/wiki/FaceMgmt</a>
 * for details
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class FaceStatus implements Decodable {

  /**
   * Spec from
   * <a href="http://redmine.named-data.net/projects/nfd/wiki/ControlCommand">http://redmine.named-data.net/projects/nfd/wiki/ControlCommand</a>
   */
  public static final int TLV_FACE_ID = 105;
  public static final int TLV_URI = 114;
  public static final int TLV_EXPIRATION_PERIOD = 109;

  /**
   * Spec from
   * <a href="http://redmine.named-data.net/projects/nfd/widi/FaceMgmt">http://redmine.named-data.net/projects/nfd/widi/FaceMgmt</a>
   */
  public static final int TLV_FACE_STATUS = 128;
  public static final int TLV_LOCAL_URI = 129;
  public static final int TLV_CHANNEL_STATUS = 130;
  public static final int TLV_FACE_SCOPE = 132;
  public static final int TLV_FACE_PERSISTENCY = 133;
  public static final int TLV_LINK_TYPE = 134;
  public static final int TLV_N_IN_INTERESTS = 144;
  public static final int TLV_N_IN_DATAS = 145;
  public static final int TLV_N_OUT_INTERESTS = 146;
  public static final int TLV_N_OUT_DATAS = 147;
  public static final int TLV_N_IN_BYTES = 148;
  public static final int TLV_N_OUT_BYTES = 149;
  public static final int TLV_NUM_IN_NACKS = 151;
  public static final int TLV_NUM_OUT_NACKS = 152;

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
    encoder.writeNonNegativeIntegerTlv(TLV_N_OUT_BYTES, outBytes);
    encoder.writeNonNegativeIntegerTlv(TLV_N_IN_BYTES, inBytes);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_OUT_NACKS, numOutNacks);
    encoder.writeNonNegativeIntegerTlv(TLV_N_OUT_DATAS, outDatas);
    encoder.writeNonNegativeIntegerTlv(TLV_N_OUT_INTERESTS, outInterests);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_IN_NACKS, numInNacks);
    encoder.writeNonNegativeIntegerTlv(TLV_N_IN_DATAS, inDatas);
    encoder.writeNonNegativeIntegerTlv(TLV_N_IN_INTERESTS, inInterests);
    encoder.writeNonNegativeIntegerTlv(TLV_LINK_TYPE, linkType.getNumericValue());
    encoder.writeNonNegativeIntegerTlv(TLV_FACE_PERSISTENCY, facePersistency.getNumericValue());
    encoder.writeNonNegativeIntegerTlv(TLV_FACE_SCOPE, faceScope.getNumericValue());
    encoder.writeOptionalNonNegativeIntegerTlv(TLV_EXPIRATION_PERIOD, expirationPeriod);
    encoder.writeBlobTlv(TLV_LOCAL_URI, new Blob(localUri).buf());
    encoder.writeBlobTlv(TLV_URI, new Blob(uri).buf());
    encoder.writeNonNegativeIntegerTlv(TLV_FACE_ID, faceId);
    encoder.writeTypeAndLength(TLV_FACE_STATUS, encoder.getLength() - saveLength);
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
  @Override
  public void wireDecode(TlvDecoder decoder) throws EncodingException {
    int endOffset = decoder.readNestedTlvsStart(TLV_FACE_STATUS);
    // parse
    this.faceId = (int) decoder.readNonNegativeIntegerTlv(TLV_FACE_ID);
    Blob uri_ = new Blob(decoder.readBlobTlv(TLV_URI), true); // copy because buffer is immutable
    this.uri = uri_.toString();
    Blob localUri_ = new Blob(decoder.readBlobTlv(TLV_LOCAL_URI), true); // copy because buffer is immutable
    this.localUri = localUri_.toString();
    this.expirationPeriod = (int) decoder.readOptionalNonNegativeIntegerTlv(TLV_EXPIRATION_PERIOD, endOffset);
    this.faceScope = FaceScope.values()[(int) decoder.readNonNegativeIntegerTlv(TLV_FACE_SCOPE)];
    this.facePersistency = FacePersistency.values()[(int) decoder.readNonNegativeIntegerTlv(TLV_FACE_PERSISTENCY)];
    this.linkType = LinkType.values()[(int) decoder.readNonNegativeIntegerTlv(TLV_LINK_TYPE)];
    this.inInterests = (int) decoder.readNonNegativeIntegerTlv(TLV_N_IN_INTERESTS);
    this.inDatas = (int) decoder.readNonNegativeIntegerTlv(TLV_N_IN_DATAS);
    this.numInNacks = decoder.readNonNegativeIntegerTlv(TLV_NUM_IN_NACKS);
    this.outInterests = (int) decoder.readNonNegativeIntegerTlv(TLV_N_OUT_INTERESTS);
    this.outDatas = (int) decoder.readNonNegativeIntegerTlv(TLV_N_OUT_DATAS);
    this.numOutNacks = decoder.readNonNegativeIntegerTlv(TLV_NUM_OUT_NACKS);
    this.inBytes = (int) decoder.readNonNegativeIntegerTlv(TLV_N_IN_BYTES);
    this.outBytes = (int) decoder.readNonNegativeIntegerTlv(TLV_N_OUT_BYTES);
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
   * Get face ID
   *
   * @return
   */
  public String getUri() {
    return uri;
  }

  /**
   * Set URI
   *
   * @param uri
   */
  public void setUri(String uri) {
    this.uri = uri;
  }

  /**
   * Get face ID
   *
   * @return
   */
  public String getLocalUri() {
    return localUri;
  }

  /**
   * Set local URI
   *
   * @param localUri
   */
  public void setLocalUri(String localUri) {
    this.localUri = localUri;
  }

  /**
   * Get expiration period
   *
   * @return
   */
  public int getExpirationPeriod() {
    return expirationPeriod;
  }

  /**
   * Set expiration period
   *
   * @param expirationPeriod
   */
  public void setExpirationPeriod(int expirationPeriod) {
    this.expirationPeriod = expirationPeriod;
  }

  /**
   * Get face scope value
   *
   * @return
   */
  public FaceScope getFaceScope() {
    return faceScope;
  }

  /**
   * Set face scope value
   *
   * @param faceScope
   */
  public void setFaceScope(FaceScope faceScope) {
    this.faceScope = faceScope;
  }

  /**
   * Get face persistency value
   *
   * @return
   */
  public FacePersistency getFacePersistency() {
    return facePersistency;
  }

  /**
   * Set face persistency value
   *
   * @param facePersistency
   */
  public void setFacePersistency(FacePersistency facePersistency) {
    this.facePersistency = facePersistency;
  }

  /**
   * Get link type
   *
   * @return
   */
  public LinkType getLinkType() {
    return linkType;
  }

  /**
   * Set link type
   *
   * @param linkType
   */
  public void setLinkType(LinkType linkType) {
    this.linkType = linkType;
  }

  /**
   * Get number of received Interest packets
   *
   * @return
   */
  public int getInInterests() {
    return inInterests;
  }

  /**
   * Set number of received Interest packets
   *
   * @param inInterests
   */
  public void setInInterests(int inInterests) {
    this.inInterests = inInterests;
  }

  /**
   * Get number of sent Interest packets
   *
   * @return
   */
  public int getOutInterests() {
    return outInterests;
  }

  /**
   * Set number of sent Interest packets
   *
   * @param outInterests
   */
  public void setOutInterests(int outInterests) {
    this.outInterests = outInterests;
  }

  /**
   * Get number of received Data packets
   *
   * @return
   */
  public int getInDatas() {
    return inDatas;
  }

  /**
   * Set number of received Data packets
   *
   * @param inDatas
   */
  public void setInDatas(int inDatas) {
    this.inDatas = inDatas;
  }

  /**
   * Get number of sent Data packets
   *
   * @return
   */
  public int getOutDatas() {
    return outDatas;
  }

  /**
   * Set number of sent Data packets
   *
   * @param outDatas
   */
  public void setOutDatas(int outDatas) {
    this.outDatas = outDatas;
  }

  /**
   * Get number of input bytes
   *
   * @return
   */
  public int getInBytes() {
    return inBytes;
  }

  /**
   * Set number of input bytes
   *
   * @param inBytes
   */
  public void setInBytes(int inBytes) {
    this.inBytes = inBytes;
  }

  /**
   * Get number of output bytes
   *
   * @return
   */
  public int getOutBytes() {
    return outBytes;
  }

  /**
   * Set number of output bytes
   *
   * @param outBytes
   */
  public void setOutBytes(int outBytes) {
    this.outBytes = outBytes;
  }

  public long getNumInNacks() {
    return numInNacks;
  }

  public void setNumInNacks(long numInNacks) {
    this.numInNacks = numInNacks;
  }

  public long getNumOutNacks() {
    return numOutNacks;
  }

  public void setNumOutNacks(long numOutNacks) {
    this.numOutNacks = numOutNacks;
  }

  private int faceId = -1;
  private String uri = ""; // can't use URI because some are invalid syntax
  private String localUri = ""; // can't use URI because some are invalid syntax
  private int expirationPeriod = 0;
  private FaceScope faceScope = FaceScope.LOCAL;
  private FacePersistency facePersistency = FacePersistency.ON_DEMAND;
  private LinkType linkType = LinkType.POINT_TO_POINT;
  private int inInterests = 0;
  private int outInterests = 0;
  private int inDatas = 0;
  private int outDatas = 0;
  private int inBytes = 0;
  private int outBytes = 0;
  private long numInNacks = 0;
  private long numOutNacks = 0;
}
