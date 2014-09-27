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

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.supernovapps.audio.jstreamsourcer.Sourcer;

public class MessageFactory {
  static public Message getRequestCipherMessage() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_REQUEST_CIPHER;
    msg.payload = Message.ULTVX_VERSION.getBytes();

    return msg;
  }

  static public Message getBroadcastAuthRequest(String sid, String uid, String pwd, String cipherkey) {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_AUTHENTIFICATE;

    byte[] bKey = cipherkey.getBytes();
    if (bKey.length < 16) {
      bKey = new byte[16];
      System.arraycopy(cipherkey.getBytes(), 0, bKey, 0, cipherkey.length());
    }

    XTEA xtea = new XTEA();

    String sUid = xtea.XTEA_encipher(uid.getBytes().clone(), cipherkey.getBytes().clone());
    String sPwd = xtea.XTEA_encipher(pwd.getBytes().clone(), cipherkey.getBytes().clone());

    String payload = Message.ULTVX_VERSION + ":" + sid + ":" + sUid + ":" + sPwd;

    msg.payload = payload.getBytes();

    return msg;
  }

  static public Message getStreamMimeType(String type) {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_STREAM_MINE_TYPE;

    msg.payload = type.getBytes();

    return msg;
  }

  static public Message getIcyGenre(String genre) {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_CONFIGURE_ICY_GENRE;

    msg.payload = genre.getBytes();

    return msg;
  }

  static public Message getIcyName(String name) {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_CONFIGURE_ICY_NAME;

    msg.payload = name.getBytes();

    return msg;
  }

  static public Message getIcyPub(boolean pub) {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_CONFIGURE_ICY_PUB;

    byte[] bytes = new byte[1];
    if (pub) {
      bytes[0] = 1;
      msg.payload = bytes;
    } else {
      bytes[0] = 0;
      msg.payload = bytes;
    }

    return msg;
  }

  static public Message getIcyUrl(String url) {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_CONFIGURE_ICY_URL;

    msg.payload = url.getBytes();

    return msg;
  }

  static public Message getSetupBroadcast(int average, int maximum) {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_SETUP;

    msg.payload = (Integer.toString(average) + ":" + Integer.toString(maximum)).getBytes();

    return msg;
  }

  static public Message getNegociateBufferSize(int desired, int minimum) {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_NEGOTIATE_BUFFER_SIZE;

    msg.payload = (Integer.toString(desired) + ":" + Integer.toString(minimum)).getBytes();

    return msg;
  }

  static public Message getNegociateMaxPayloadSize(int desired, int minimum) {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_NEGOTIATE_MAX_PAYLOAD;

    msg.payload = (Integer.toString(desired) + ":" + Integer.toString(minimum)).getBytes();

    return msg;
  }

  static public Message getStandby() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_STANDBY;

    /*
     * don't know why this payload, however the payload sent by the official version a shoutcast
     * sourcer send this one
     */
    msg.payload = new byte[2];
    msg.payload[0] = 0x01;
    msg.payload[1] = 0x30;

    return msg;
  }

  static public Message getData(short type, byte[] data, int len) {
    Message msg = new Message();

    if (type == Message.DATA1_MP3) {
      msg.msgClass = Message.CLASS_DATA1;
    } else {
      msg.msgClass = Message.CLASS_DATA2;
    }

    msg.payload = data;
    msg.payloadLen = len;

    return msg;
  }

  /*
   * <?xml version="1.0" encoding="UTF-8" ?> <metadata> <TIT2>title</TIT2> <TALB>album</TALB>
   * <TPE1>artist</TPE1> <TYER>year</TYER> <COMM></COMM> <TCON></TCON> <TENC>encoder</TENC>
   * <TRSN></TRSN> <WORS></WORS> </metadata>
   */
  static public Message[] getMetadata(String song, String artist, String album, int payloadLen) {
    List<Message> msgs = new ArrayList<Message>();

    String xml = getMetadataXmlString(song, artist, album);
    if (xml == null) {
      return msgs.toArray(new Message[msgs.size()]);
    }

    /*
     * [Metadata ID] (16bits) [Metadata Span] (16 bits) [Metadata Index] (16 bits)
     *
     * Total: 6 bytes
     */
    int size = payloadLen - 6;
    List<String> metadatas = new ArrayList<String>();
    for (int index = 0; index < xml.length(); index += size) {
      metadatas.add(xml.substring(index, Math.min(index + size, xml.length())));
      index += size;
    }

    short msgId = (short) (Math.floor(Math.random() * 32) + 1);
    short msgSpan = (short) metadatas.size();
    short msgIndex = 1;
    for (String metadata : metadatas) {
      Message msg = new Message();

      msg.msgClass = Message.CLASS_CACHEABLE_METADATA1;
      msg.msgType = Message.CACHEABLE_METADATA1_XML_SHOUCAST_METADATA;

      msg.payload = new byte[6 + metadata.length()];
      msg.payload[0] = 0;
      msg.payload[1] = (byte) msgId;
      msg.payload[2] = 0;
      msg.payload[3] = (byte) msgSpan;
      msg.payload[4] = 0;
      msg.payload[5] = (byte) msgIndex;

      try {
        byte[] metadataBytes = metadata.getBytes("UTF8");
        System.arraycopy(metadataBytes, 0, msg.payload, 6, metadataBytes.length);
      } catch (UnsupportedEncodingException e) {
        continue;
      }

      msgs.add(msg);
      msgIndex++;
    }

    return msgs.toArray(new Message[msgs.size()]);
  }

  private static String getMetadataXmlString(String song, String artist, String album)
      throws TransformerFactoryConfigurationError {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    try {
      DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
      Document doc = docBuilder.newDocument();

      Element rootElement = doc.createElement("metadata");
      doc.appendChild(rootElement);

      appendElement(rootElement, "TIT2", song);
      appendElement(rootElement, "TALB", album);
      appendElement(rootElement, "TPE1", artist);
      appendElement(rootElement, "TENC", Sourcer.USER_AGENT);

      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      DOMSource source = new DOMSource(doc);

      StringWriter outWriter = new StringWriter();
      StreamResult result = new StreamResult(outWriter);

      transformer.transform(source, result);
      StringBuffer sb = outWriter.getBuffer();
      String xml = sb.toString();

      return xml;
    } catch (ParserConfigurationException | TransformerException e) {
      e.printStackTrace();
    }

    return null;
  }

  private static void appendElement(Element rootElement, String el, String text) {
    Element e = rootElement.getOwnerDocument().createElement(el);
    e.setTextContent(text);
    rootElement.appendChild(e);
  }
}
