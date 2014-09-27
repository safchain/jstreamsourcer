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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class IcecastTest {

  private Icecast icecast;
  private int bitrate = 128;
  private boolean onConnected;

  @Before
  public void setUp() throws Exception {
    icecast = new Icecast(bitrate, 5000);
    icecast.setHost("localhost");
    icecast.setPort(8000);
    icecast.setPath("/stream1");
    icecast.setUsername("username1");
    icecast.setPassword("password1");

    onConnected = false;
  }

  @After
  public void tearDown() throws Exception {}

  @Test
  public void testConnect() throws IOException {
    Socket sockMock = EasyMock.createNiceMock(Socket.class);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream in = new ByteArrayInputStream(new String("HTTP OK").getBytes());

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    icecast.start(sockMock);

    String expected = "SOURCE /stream1 HTTP/1.0\n";
    expected += "Authorization: Basic dXNlcm5hbWUxOnBhc3N3b3JkMQ==\n";
    expected += "User-Agent: " + Sourcer.USER_AGENT + "\n";
    expected += "icy-notice1: " + Sourcer.USER_AGENT + "\n";
    expected += "content-type: " + icecast.getStreamInfo(Sourcer.CONTENT_TYPE) + "\n";
    expected += "icy-genre: " + icecast.getStreamInfo(Sourcer.ICY_GENRE) + "\n";
    expected += "icy-pub: " + icecast.getStreamInfo(Sourcer.ICY_PUB) + "\n";
    expected += "icy-name: " + icecast.getStreamInfo(Sourcer.ICY_NAME) + "\n";
    expected += "icy-url: " + icecast.getStreamInfo(Sourcer.ICY_URL) + "\n";
    expected += "icy-br: " + String.valueOf(bitrate) + "\n\n";

    String http = new String(out.toByteArray());
    Assert.assertEquals(expected, http);
  }

  @Test
  public void testConnectSuccess() throws IOException {
    Socket sockMock = EasyMock.createNiceMock(Socket.class);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream in = new ByteArrayInputStream(new String("HTTP OK").getBytes());

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.expect(sockMock.isConnected()).andReturn(true);
    EasyMock.replay(sockMock);

    boolean started = icecast.start(sockMock);

    Assert.assertTrue(started);
    Assert.assertTrue(icecast.isStarted());
  }

  @Test
  public void testConnectFails() throws IOException {
    Socket mock = EasyMock.createNiceMock(Socket.class);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream in = new ByteArrayInputStream(new String("KO").getBytes());

    EasyMock.expect(mock.getOutputStream()).andReturn(out);
    EasyMock.expect(mock.getInputStream()).andReturn(in);
    EasyMock.replay(mock);

    boolean started = icecast.start(mock);

    Assert.assertFalse(started);
    Assert.assertFalse(icecast.isStarted());
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

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream in = new ByteArrayInputStream(new String("HTTP OK").getBytes());

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    icecast.setOnSourcerListener(listener);
    icecast.start(sockMock);

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

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream in = new ByteArrayInputStream(new String("KO").getBytes());

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    icecast.setOnSourcerListener(listener);
    icecast.start(sockMock);

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

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream in = new ByteArrayInputStream(new String("HTTP OK").getBytes());

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.replay(sockMock);

    icecast.setOnSourcerListener(listener);

    boolean started = icecast.start(sockMock);
    Assert.assertTrue(started);

    boolean stopped = icecast.stop();
    Assert.assertTrue(stopped);

    Assert.assertTrue(onConnected);
  }

  @Test
  public void testUpdateMetadata() throws IOException, URISyntaxException {
    Socket sockMock = EasyMock.createNiceMock(Socket.class);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayInputStream in = new ByteArrayInputStream(new String("HTTP OK").getBytes());

    EasyMock.expect(sockMock.getOutputStream()).andReturn(out);
    EasyMock.expect(sockMock.getInputStream()).andReturn(in);
    EasyMock.expect(sockMock.isConnected()).andReturn(true);
    EasyMock.replay(sockMock);

    icecast.start(sockMock);

    HttpUriRequest request = icecast.getUpdateMetadataRequest("song", "artist", "album");
    Assert.assertEquals(icecast.getHost(), request.getURI().getHost());
    Assert.assertEquals(icecast.getPort(), request.getURI().getPort());

    HashMap<String, String> paramsMap = getParams(request);
    Assert.assertEquals(icecast.getPath(), paramsMap.get("mount"));
    Assert.assertEquals("updinfo", paramsMap.get("mode"));
    Assert.assertEquals("album song artist", paramsMap.get("song"));
  }

  private HashMap<String, String> getParams(HttpUriRequest request) throws URISyntaxException {
    List<NameValuePair> params =
        URLEncodedUtils.parse(new URI(request.getURI().toASCIIString()), "UTF-8");
    HashMap<String, String> paramsMap = new HashMap<String, String>();
    for (NameValuePair param : params) {
      paramsMap.put(param.getName(), param.getValue());
    }
    return paramsMap;
  }
}
