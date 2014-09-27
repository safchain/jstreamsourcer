/*
 * Copyright (C) 2014 Sylvain Afchain
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package com.supernovapps.audio.jstreamsourcer.ultravox2;

import java.io.IOException;
import java.io.InputStream;

public class Message {
  private static final byte SYNC = 0x5A;

  /*
   * Message Classes
   */
  public static final byte CLASS_OPERATIONS = 0x0;
  public static final byte CLASS_BROADCASTER = 0x1;
  public static final byte CLASS_LISTENER = 0x2;
  public static final byte CLASS_CACHEABLE_METADATA1 = 0x3;
  public static final byte CLASS_CACHEABLE_METADATA2 = 0x4;
  public static final byte CLASS_PASS_TROUGH_METADATA1 = 0x5;
  public static final byte CLASS_PASS_TROUGH_METADATA2 = 0x6;
  public static final byte CLASS_DATA1 = 0x7;
  public static final byte CLASS_DATA2 = 0x8;
  public static final byte CLASS_FRAMED_DATA = 0x9;
  public static final byte CLASS_CACHEABLE_BINARY_METADATA = 0xA;

  /*
   * Broadcast Messages
   */
  public static final short BROADCAST_ND = 0x000;
  public static final short BROADCAST_AUTHENTIFICATE = 0x001;
  public static final short BROADCAST_SETUP = 0x002;
  public static final short BROADCAST_NEGOTIATE_BUFFER_SIZE = 0x003;
  public static final short BROADCAST_STANDBY = 0x004;
  public static final short BROADCAST_TERMINATE = 0x005;
  public static final short BROADCAST_FLUSH_CACHEABLE_DATA = 0x006;
  public static final short BROADCAST_REQUIRE_LISTENER_AUTH = 0x007;
  public static final short BROADCAST_NEGOTIATE_MAX_PAYLOAD = 0x008;
  public static final short BROADCAST_REQUEST_CIPHER = 0x009;
  public static final short BROADCAST_STREAM_MINE_TYPE = 0x040;
  public static final short BROADCAST_FILE_TRANSFERT_BEGIN1 = 0x050;
  public static final short BROADCAST_FILE_TRANSFERT_BEGIN2 = 0x051;
  public static final short BROADCAST_CONFIGURE_ICY_NAME = 0x100;
  public static final short BROADCAST_CONFIGURE_ICY_GENRE = 0x101;
  public static final short BROADCAST_CONFIGURE_ICY_URL = 0x102;
  public static final short BROADCAST_CONFIGURE_ICY_PUB = 0x103;

  public static final short LISTENER_TEMPORARY_BROADCAST_INTERRUPTION = 0x001;
  public static final short LISTENER_BROADCAST_TERMINATION = 0x002;

  public static final short CACHEABLE_METADATA1_XML_AOL_METADATA = 0x901;
  public static final short CACHEABLE_METADATA1_XML_SHOUCAST_METADATA = 0x902;

  /*
   * data type
   */
  public static final short DATA1_MP3 = 0x000;
  public static final short DATA2_VLB = 0x000;
  public static final short DATA2_AAC_LC = 0x001;
  public static final short DATA2_AACP = 0x003;

  /*
   * Default Version
   */
  public static final String ULTVX_VERSION = "2.1";

  private static final int HEADER_SIZE = 6;

  /*
   * Message fields
   */
  private byte sync = SYNC;
  private byte resQos = 0;
  public byte msgClass;
  public short msgType;
  public byte[] payload;
  public int payloadLen = 0;
  public byte trailing = 0x00;

  public byte[] encode() {
    int len = payloadLen;
    if (len == 0 && payload != null) {
      len = payload.length;
    }

    byte[] buffer = new byte[len + 7];

    buffer[0] = sync;
    buffer[1] = resQos;

    buffer[2] = (byte) (((msgClass << 4) | (msgType >> 8)) & 0xFF);
    buffer[3] = (byte) (msgType & 0xFF);

    buffer[4] = (byte) ((len >> 8) & 0xFF);
    buffer[5] = (byte) (len & 0xFF);

    if (payload != null) {
      System.arraycopy(payload, 0, buffer, HEADER_SIZE, len);
    }

    buffer[len + 6] = trailing;

    return buffer;
  }

  static public Message decodeHeader(byte[] buffer) throws IOException {
    Message msg = new Message();

    msg.sync = buffer[0];
    if (msg.sync != SYNC) {
      throw new IOException("Malformed message header");
    }

    msg.resQos = buffer[1];

    msg.msgClass = (byte) ((buffer[2] >> 4) & 0xFF);
    msg.msgType = (short) (buffer[2] & 0x0F);
    msg.msgType = (short) (msg.msgType << 8 | buffer[3]);

    int length = buffer[4] ;
    msg.payloadLen = ((length << 8 | buffer[5]) & 0xFF);

    msg.payload = new byte[msg.payloadLen];

    return msg;
  }

  static public Message decode(byte[] buffer) throws IOException {
    Message msg = decodeHeader(buffer);

    System.arraycopy(buffer, HEADER_SIZE, msg.payload, 0, msg.payloadLen);

    return msg;
  }

  static public Message read(InputStream in) throws IOException {
    byte[] buffer = new byte[6];

    int ret = in.read(buffer);
    if (ret != buffer.length) {
      throw new IOException("Malformed message" + in.available());
    }

    Message msg = decodeHeader(buffer);

    ret = in.read(msg.payload, 0, msg.payloadLen);
    if (ret != msg.payload.length) {
      throw new IOException("Malformed message");
    }

    // trailing
    int t = in.read();
    if (t != 0x00) {
      throw new IOException("Malformed message");
    }

    return msg;
  }
}
