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

package com.supernovapps.audio.jstreamsourcer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.supernovapps.audio.jstreamsourcer.ultravox2.Message;

public class ShoutcastV2Test {

  private ShoutcastV2 shoutcast;
  private int bitrate = 128;
  private boolean onConnected;

  @Before
  public void setUp() throws Exception {
    shoutcast = new ShoutcastV2(bitrate, 5000);
    shoutcast.setHost("localhost");
    shoutcast.setPort(8000);
    shoutcast.setPath("/stream1");
    shoutcast.setUsername("username1");
    shoutcast.setPassword("password1");
    shoutcast.setUid("1");
    shoutcast.setSid("2");

    onConnected = false;
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testConnectSuccess() throws IOException {
    Socket sockMock = EasyMock.createNiceMock(Socket.class);

    ByteArrayInputStream in = new ByteArrayInputStream(getSetReplyByteSequence());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    boolean started = shoutcast.start(sockMock);

    Assert.assertTrue(started);
  }

  @Test
  public void testConnectFail() throws IOException {
    Socket sockMock = EasyMock.createNiceMock(Socket.class);

    ByteArrayInputStream in = new ByteArrayInputStream(getRequestCipherMessageReply().encode());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    boolean started = shoutcast.start(sockMock);

    Assert.assertFalse(started);
  }

  @Test
  public void testOnSourcerListenerSuccess() throws IOException {
    Socket sockMock = EasyMock.createNiceMock(Socket.class);

    OnSourcerListener listener = new OnSourcerListener() {

      @Override
      public void onError(String string) {}

      @Override
      public void onDisconnected(boolean connected) {}

      @Override
      public void onConnected() {
        onConnected = true;
      }
    };

    ByteArrayInputStream in = new ByteArrayInputStream(getSetReplyByteSequence());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    shoutcast.setOnSourcerListener(listener);
    shoutcast.start(sockMock);

    Assert.assertTrue(onConnected);
  }

  @Test
  public void testOnSourcerListenerFails() throws IOException {
    Socket sockMock = EasyMock.createNiceMock(Socket.class);

    OnSourcerListener listener = new OnSourcerListener() {

      @Override
      public void onError(String string) {
        onConnected = true;
      }

      @Override
      public void onDisconnected(boolean connected) {}

      @Override
      public void onConnected() {}
    };

    ByteArrayInputStream in = new ByteArrayInputStream(getRequestCipherMessageReply().encode());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    shoutcast.setOnSourcerListener(listener);
    shoutcast.start(sockMock);

    Assert.assertTrue(onConnected);
  }

  @Test
  public void testOnSourcerListenerDisconnect() throws IOException {
    Socket sockMock = EasyMock.createNiceMock(Socket.class);

    OnSourcerListener listener = new OnSourcerListener() {

      @Override
      public void onError(String string) {}

      @Override
      public void onDisconnected(boolean connected) {
        onConnected = true;
      }

      @Override
      public void onConnected() {}
    };

    ByteArrayInputStream in = new ByteArrayInputStream(getRequestCipherMessageReply().encode());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    shoutcast.setOnSourcerListener(listener);
    shoutcast.start(sockMock);
    shoutcast.stop();

    Assert.assertTrue(onConnected);
  }

  @Test
  public void testUpdateMetadata() throws IOException, ParserConfigurationException, SAXException {
    Socket sockMock = EasyMock.createNiceMock(Socket.class);

    ByteArrayInputStream in = new ByteArrayInputStream(getSetReplyByteSequence());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    shoutcast.start(sockMock);
    out.reset();

    shoutcast.updateMetadata("song", "artist", "album");
    byte[] metadataMessageBytes = out.toByteArray();

    Message message = Message.read(new ByteArrayInputStream(metadataMessageBytes));
    Assert.assertEquals(Message.CLASS_CACHEABLE_METADATA1, message.msgClass);
    Assert.assertEquals(Message.CACHEABLE_METADATA1_XML_SHOUCAST_METADATA, message.msgType);

    byte[] xmlBytes = new byte[message.payloadLen - 6];
    System.arraycopy(message.payload, 6, xmlBytes, 0, xmlBytes.length);

    ByteArrayInputStream xmlInputStream = new ByteArrayInputStream(xmlBytes);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    DocumentBuilder db = dbf.newDocumentBuilder();
    Document doc = db.parse(xmlInputStream);

    Node songNode = doc.getElementsByTagName("TIT2").item(0);
    Assert.assertEquals("song", songNode.getFirstChild().getNodeValue());

    Node albumNode = doc.getElementsByTagName("TALB").item(0);
    Assert.assertEquals("album", albumNode.getFirstChild().getNodeValue());

    Node artistNode = doc.getElementsByTagName("TPE1").item(0);
    Assert.assertEquals("artist", artistNode.getFirstChild().getNodeValue());
  }

  private byte[] getSetReplyByteSequence() throws IOException {
    ByteArrayOutputStream outMessageStream = new ByteArrayOutputStream();
    outMessageStream.write(getRequestCipherMessageReply().encode());
    outMessageStream.write(getBroadcastAuthRequestReply().encode());
    outMessageStream.write(getStreamMimeTypeReply().encode());
    outMessageStream.write(getSetupBroadcastReply().encode());
    outMessageStream.write(getNegociateBufferSizeReply().encode());
    outMessageStream.write(getNegociateMaxPayloadSizeReply().encode());
    outMessageStream.write(getIcyGenreReply().encode());
    outMessageStream.write(getIcyNameReply().encode());
    outMessageStream.write(getIcyPubReply().encode());
    outMessageStream.write(getIcyUrlReply().encode());
    outMessageStream.write(getStandbyReply().encode());
    return outMessageStream.toByteArray();
  }

  public Message getRequestCipherMessageReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_REQUEST_CIPHER;
    msg.payload = ("ACK:XXX").getBytes();

    return msg;
  }

  public Message getBroadcastAuthRequestReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_AUTHENTIFICATE;
    msg.payload = ("ACK:" + Message.ULTVX_VERSION + ":Allow").getBytes();

    return msg;
  }

  public Message getStreamMimeTypeReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_STREAM_MINE_TYPE;
    msg.payload = ("ACK:XXX").getBytes();

    return msg;
  }

  public Message getSetupBroadcastReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_SETUP;
    msg.payload = ("ACK:XXX").getBytes();

    return msg;
  }

  public Message getNegociateBufferSizeReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_NEGOTIATE_BUFFER_SIZE;
    msg.payload = ("ACK:XXX").getBytes();

    return msg;
  }

  public Message getNegociateMaxPayloadSizeReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_NEGOTIATE_MAX_PAYLOAD;
    msg.payload = ("ACK:XXX").getBytes();

    return msg;
  }

  public Message getIcyGenreReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_CONFIGURE_ICY_GENRE;
    msg.payload = ("ACK:XXX").getBytes();

    return msg;
  }

  public Message getIcyNameReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_CONFIGURE_ICY_NAME;
    msg.payload = ("ACK:XXX").getBytes();

    return msg;
  }

  public Message getIcyPubReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_CONFIGURE_ICY_PUB;
    msg.payload = ("ACK:XXX").getBytes();

    return msg;
  }

  public Message getIcyUrlReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_CONFIGURE_ICY_URL;
    msg.payload = ("ACK:XXX").getBytes();

    return msg;
  }

  public Message getStandbyReply() {
    Message msg = new Message();

    msg.msgClass = Message.CLASS_BROADCASTER;
    msg.msgType = Message.BROADCAST_STANDBY;
    msg.payload = ("ACK:Data transfer mode").getBytes();

    return msg;
  }
}
