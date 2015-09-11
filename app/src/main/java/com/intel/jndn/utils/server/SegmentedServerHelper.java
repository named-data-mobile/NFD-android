/*
 * jndn-utils
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
package com.intel.jndn.utils.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.named_data.jndn.Data;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

/**
 * Helper for segmenting an input stream into a list of Data packets. Current
 * use of the default segment size of 4096 (only for
 * {@link #segment(net.named_data.jndn.Data, java.io.InputStream)} is based on
 * several assumptions: NDN packet size was limited to 8000 at the time this was
 * written and signature size is unknown.
 *
 * @author Andrew Brown <andrew.brown@intel.com>
 */
public class SegmentedServerHelper {

  public static final int DEFAULT_SEGMENT_SIZE = 4096;

  /**
   * Segment a stream of bytes into a list of Data packets; this must read all
   * the bytes first in order to determine the end segment for FinalBlockId.
   *
   * @param template the {@link Data} packet to use for the segment {@link Name},
   * {@link net.named_data.jndn.MetaInfo}, etc.
   * @param bytes an {@link InputStream} to the bytes to segment
   * @return a list of segmented {@link Data} packets
   * @throws IOException if the stream fails
   */
  public static List<Data> segment(Data template, InputStream bytes) throws IOException {
    return segment(template, bytes, DEFAULT_SEGMENT_SIZE);
  }

  /**
   * Segment a stream of bytes into a list of Data packets; this must read all
   * the bytes first in order to determine the end segment for FinalBlockId.
   *
   * @param template the {@link Data} packet to use for the segment {@link Name},
   * {@link net.named_data.jndn.MetaInfo}, etc.
   * @param bytes an {@link InputStream} to the bytes to segment
   * @return a list of segmented {@link Data} packets
   * @throws IOException if the stream fails
   */
  public static List<Data> segment(Data template, InputStream bytes, int segmentSize) throws IOException {
    List<Data> segments = new ArrayList<>();
    byte[] buffer_ = readAll(bytes);
    int numBytes = buffer_.length;
    int numPackets = (int) Math.ceil((double) numBytes / segmentSize);
    ByteBuffer buffer = ByteBuffer.wrap(buffer_, 0, numBytes);
    Name.Component lastSegment = Name.Component.fromNumberWithMarker(numPackets - 1, 0x00);

    for (int i = 0; i < numPackets; i++) {
      Data segment = new Data(template);
      segment.getName().appendSegment(i);
      segment.getMetaInfo().setFinalBlockId(lastSegment);
      byte[] content = new byte[Math.min(segmentSize, buffer.remaining())];
      buffer.get(content);
      segment.setContent(new Blob(content));
      segments.add(segment);
    }

    return segments;
  }

  /**
   * Read all of the bytes in an input stream.
   *
   * @param bytes the {@link InputStream} of bytes to read
   * @return an array of all bytes retrieved from the stream
   * @throws IOException if the stream fails
   */
  public static byte[] readAll(InputStream bytes) throws IOException {
    ByteArrayOutputStream builder = new ByteArrayOutputStream();
    int read = bytes.read();
    while (read != -1) {
      builder.write(read);
      read = bytes.read();
    }
    builder.flush();
    bytes.close();
    return builder.toByteArray();
  }
}
