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
 * Represent a ForwarderStatus object from
 * http://redmine.named-data.net/projects/nfd/wiki/ForwarderStatus.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class ForwarderStatus implements Decodable {

  public static final int TLV_NFD_VERSION = 0x80;
  public static final int TLV_START_TIMESTAMP = 0x81;
  public static final int TLV_CURRENT_TIMESTAMP = 0x82;
  public static final int TLV_NUM_NAME_TREE_ENTRIES = 0x83;
  public static final int TLV_NUM_FIB_ENTRIES = 0x84;
  public static final int TLV_NUM_PIT_ENTRIES = 0x85;
  public static final int TLV_NUM_MEASUREMENT_ENTRIES = 0x86;
  public static final int TLV_NUM_CS_ENTRIES = 0x87;
  public static final int TLV_NUM_IN_INTERESTS = 0x90;
  public static final int TLV_NUM_IN_DATAS = 0x91;
  public static final int TLV_NUM_OUT_INTERESTS = 0x92;
  public static final int TLV_NUM_OUT_DATAS = 0x93;
  public static final int TLV_NUM_IN_NACKS = 0x97;
  public static final int TLV_NUM_OUT_NACKS = 0x98;

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
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_OUT_NACKS, numOutNacks);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_OUT_DATAS, numOutDatas);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_OUT_INTERESTS, numOutInterests);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_IN_NACKS, numInNacks);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_IN_DATAS, numInDatas);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_IN_INTERESTS, numInInterests);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_CS_ENTRIES, numCsEntries);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_MEASUREMENT_ENTRIES, numMeasurementEntries);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_PIT_ENTRIES, numPitEntries);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_FIB_ENTRIES, numFibEntries);
    encoder.writeNonNegativeIntegerTlv(TLV_NUM_NAME_TREE_ENTRIES, numNameTreeEntries);
    encoder.writeNonNegativeIntegerTlv(TLV_CURRENT_TIMESTAMP, currentTimestamp);
    encoder.writeNonNegativeIntegerTlv(TLV_START_TIMESTAMP, startTimestamp);
    encoder.writeBlobTlv(TLV_NFD_VERSION, new Blob(nfdVersion).buf());
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
  public void wireDecode(TlvDecoder decoder) throws EncodingException {
    this.nfdVersion = new Blob(decoder.readBlobTlv(TLV_NFD_VERSION), true).toString();
    this.startTimestamp = decoder.readNonNegativeIntegerTlv(TLV_START_TIMESTAMP);
    this.currentTimestamp = decoder.readNonNegativeIntegerTlv(TLV_CURRENT_TIMESTAMP);
    this.numNameTreeEntries = decoder.readNonNegativeIntegerTlv(TLV_NUM_NAME_TREE_ENTRIES);
    this.numFibEntries = decoder.readNonNegativeIntegerTlv(TLV_NUM_FIB_ENTRIES);
    this.numPitEntries = decoder.readNonNegativeIntegerTlv(TLV_NUM_PIT_ENTRIES);
    this.numMeasurementEntries = decoder.readNonNegativeIntegerTlv(TLV_NUM_MEASUREMENT_ENTRIES);
    this.numCsEntries = decoder.readNonNegativeIntegerTlv(TLV_NUM_CS_ENTRIES);
    this.numInInterests = decoder.readNonNegativeIntegerTlv(TLV_NUM_IN_INTERESTS);
    this.numInDatas = decoder.readNonNegativeIntegerTlv(TLV_NUM_IN_DATAS);
    this.numInNacks = decoder.readNonNegativeIntegerTlv(TLV_NUM_IN_NACKS);
    this.numOutInterests = decoder.readNonNegativeIntegerTlv(TLV_NUM_OUT_INTERESTS);
    this.numOutDatas = decoder.readNonNegativeIntegerTlv(TLV_NUM_OUT_DATAS);
    this.numOutNacks = decoder.readNonNegativeIntegerTlv(TLV_NUM_OUT_NACKS);
  }

  public String getNfdVersion() {
    return nfdVersion;
  }

  public void setNfdVersion(String nfdVersion) {
    this.nfdVersion = nfdVersion;
  }

  public long getStartTimestamp() {
    return startTimestamp;
  }

  public void setStartTimestamp(long startTimestamp) {
    this.startTimestamp = startTimestamp;
  }

  public long getCurrentTimestamp() {
    return currentTimestamp;
  }

  public void setCurrentTimestamp(long currentTimestamp) {
    this.currentTimestamp = currentTimestamp;
  }

  public long getNumNameTreeEntries() {
    return numNameTreeEntries;
  }

  public void setNumNameTreeEntries(long numNameTreeEntries) {
    this.numNameTreeEntries = numNameTreeEntries;
  }

  public long getNumFibEntries() {
    return numFibEntries;
  }

  public void setNumFibEntries(long numFibEntries) {
    this.numFibEntries = numFibEntries;
  }

  public long getNumPitEntries() {
    return numPitEntries;
  }

  public void setNumPitEntries(long numPitEntries) {
    this.numPitEntries = numPitEntries;
  }

  public long getNumMeasurementEntries() {
    return numMeasurementEntries;
  }

  public void setNumMeasurementEntries(long numMeasurementEntries) {
    this.numMeasurementEntries = numMeasurementEntries;
  }

  public long getNumCsEntries() {
    return numCsEntries;
  }

  public void setNumCsEntries(long numCsEntries) {
    this.numCsEntries = numCsEntries;
  }

  public long getNumInInterests() {
    return numInInterests;
  }

  public void setNumInInterests(long numInInterests) {
    this.numInInterests = numInInterests;
  }

  public long getNumInDatas() {
    return numInDatas;
  }

  public void setNumInDatas(long numInDatas) {
    this.numInDatas = numInDatas;
  }

  public long getNumInNacks() {
    return numInNacks;
  }

  public void setNumInNacks(long numInNacks) {
    this.numInNacks = numInNacks;
  }

  public long getNumOutInterests() {
    return numOutInterests;
  }

  public void setNumOutInterests(long numOutInterests) {
    this.numOutInterests = numOutInterests;
  }

  public long getNumOutDatas() {
    return numOutDatas;
  }

  public void setNumOutDatas(long numOutDatas) {
    this.numOutDatas = numOutDatas;
  }

  public long getNumOutNacks() {
    return numOutNacks;
  }

  public void setNumOutNacks(long numOutNacks) {
    this.numOutNacks = numOutNacks;
  }

  private String nfdVersion = "";
  private long startTimestamp;
  private long currentTimestamp;
  private long numNameTreeEntries;
  private long numFibEntries;
  private long numPitEntries;
  private long numMeasurementEntries;
  private long numCsEntries;
  private long numInInterests;
  private long numInDatas;
  private long numInNacks;
  private long numOutInterests;
  private long numOutDatas;
  private long numOutNacks;
}
