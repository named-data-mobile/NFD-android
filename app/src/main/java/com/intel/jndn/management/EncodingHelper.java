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

import java.nio.ByteBuffer;
import net.named_data.jndn.ControlParameters;
import net.named_data.jndn.ForwardingFlags;
import net.named_data.jndn.Name;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.encoding.tlv.Tlv;
import net.named_data.jndn.encoding.tlv.TlvDecoder;
import net.named_data.jndn.encoding.tlv.TlvEncoder;
import net.named_data.jndn.util.Blob;

/**
 * Provide helper methods to cover areas too protected in Tlv0_1_1WireFormat;
 * this class can be deprecated if WireFormats allow passing in an existing
 * TlvEncoder/TlvDecoder (currently these methods are protected).
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class EncodingHelper {

  /**
   * Helper to decode names since Tlv0_1_1WireFormat.java uses its own internal,
   * protected implementation.
   *
   * @param input
   * @return
   * @throws EncodingException
   */
  public static Name decodeName(ByteBuffer input) throws EncodingException {
    TlvDecoder decoder = new TlvDecoder(input);
    return decodeName(decoder);
  }

  /**
   * Helper to decode names using an existing decoding context; could be merged
   * to Tlv0_1_1WireFormat.java.
   *
   * @param decoder
   * @return
   * @throws EncodingException
   */
  public static Name decodeName(TlvDecoder decoder) throws EncodingException {
    Name name = new Name();
    int endOffset = decoder.readNestedTlvsStart(Tlv.Name);
    while (decoder.getOffset() < endOffset) {
      name.append(new Blob(decoder.readBlobTlv(Tlv.NameComponent), true));
    }

    decoder.finishNestedTlvs(endOffset);
    return name;
  }

  /**
   * Helper to encode names since Tlv0_1_1WireFormat.java uses its own internal,
   * protected implementation.
   *
   * @param name
   * @return
   */
  public static Blob encodeName(Name name) {
    TlvEncoder encoder = new TlvEncoder();
    encodeName(name, encoder);
    return new Blob(encoder.getOutput(), false);
  }

  /**
   * Helper to encode names using an existing encoding context; could be merged
   * to Tlv0_1_1WireFormat.java.
   *
   * @param name
   * @param encoder
   */
  public static final void encodeName(Name name, TlvEncoder encoder) {
    int saveLength = encoder.getLength();
    for (int i = name.size() - 1; i >= 0; --i) {
      encoder.writeBlobTlv(Tlv.NameComponent, name.get(i).getValue().buf());
    }
    encoder.writeTypeAndLength(Tlv.Name, encoder.getLength() - saveLength);
  }

  /**
   * Helper to encode control parameters using an existing encoding context;
   * could be merged to Tlv0_1_1WireFormat.java.
   *
   * @param controlParameters
   * @param encoder
   */
  public static final void encodeControlParameters(ControlParameters controlParameters, TlvEncoder encoder) {
    int saveLength = encoder.getLength();

    // Encode backwards.
    encoder.writeOptionalNonNegativeIntegerTlvFromDouble(Tlv.ControlParameters_ExpirationPeriod,
            controlParameters.getExpirationPeriod());

    // Encode strategy
    if (controlParameters.getStrategy().size() != 0) {
      int strategySaveLength = encoder.getLength();
      encodeName(controlParameters.getStrategy(), encoder);
      encoder.writeTypeAndLength(Tlv.ControlParameters_Strategy,
              encoder.getLength() - strategySaveLength);
    }

    // Encode ForwardingFlags
    int flags = controlParameters.getForwardingFlags().getNfdForwardingFlags();
    if (flags != new ForwardingFlags().getNfdForwardingFlags()) // The flags are not the default value.
    {
      encoder.writeNonNegativeIntegerTlv(Tlv.ControlParameters_Flags, flags);
    }

    encoder.writeOptionalNonNegativeIntegerTlv(Tlv.ControlParameters_Cost, controlParameters.getCost());
    encoder.writeOptionalNonNegativeIntegerTlv(Tlv.ControlParameters_Origin, controlParameters.getOrigin());
    encoder.writeOptionalNonNegativeIntegerTlv(Tlv.ControlParameters_LocalControlFeature,
            controlParameters.getLocalControlFeature());

    // Encode URI
    if (!controlParameters.getUri().isEmpty()) {
      encoder.writeBlobTlv(Tlv.ControlParameters_Uri,
              new Blob(controlParameters.getUri()).buf());
    }

    encoder.writeOptionalNonNegativeIntegerTlv(Tlv.ControlParameters_FaceId, controlParameters.getFaceId());

    // Encode name
    if (controlParameters.getName().size() != 0) {
      encodeName(controlParameters.getName(), encoder);
    }

    encoder.writeTypeAndLength(Tlv.ControlParameters_ControlParameters, encoder.getLength() - saveLength);
  }
}
