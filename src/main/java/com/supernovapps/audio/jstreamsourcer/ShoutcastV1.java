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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class ShoutcastV1 extends Sourcer {
  private String host = null;
  private int port = 0;

  private String password = null;

  private Socket sock = null;

  private String metadataTemplate = "_album_ _song_ _artist_";

  public ShoutcastV1(int kbps, int burst) {
    super(kbps, burst);
  }

  public boolean start(Socket sock) {
    try {
      this.sock = sock;
      sock.connect(new InetSocketAddress(host, port), 5000);
      sock.setSendBufferSize(64 * 1024);
      out = sock.getOutputStream();

      PrintWriter output = writeAuthentication();

      InputStreamReader isr = new InputStreamReader(sock.getInputStream());
      BufferedReader in = new BufferedReader(isr);
      String line = in.readLine();
      if (line == null || !line.contains("OK")) {
        if (listener != null) {
          listener.onError("Connection / Authentification error");
        }

        return false;
      }

      while (line != null && line.length() > 0) {
        line = in.readLine();
      }

      writeHeaders(output);
    } catch (Exception e) {
      e.printStackTrace();

      try {
        if (sock != null)
          sock.close();
      } catch (IOException e1) {
        e1.printStackTrace();
      }

      if (listener != null) {
        listener.onError("Connection / Authentification error");
      }

      return false;
    }
    started = true;

    if (listener != null) {
      listener.onConnected();
    }

    return true;
  }

  private void writeHeaders(PrintWriter output) {
    for (Entry<String, String> entry : streamInfos.entrySet()) {
      output.println(entry.getKey() + ": " + entry.getValue());
    }

    output.println("");
    output.flush();
  }

  private PrintWriter writeAuthentication() {
    PrintWriter output = new PrintWriter(out);
    output.println(password);
    output.flush();
    return output;
  }

  @Override
  public boolean start() {
    return start(new Socket());
  }

  @Override
  public boolean isStarted() {
    return sock != null && sock.isConnected() && started;
  }

  @Override
  public boolean stop() {

    try {
      out.flush();
      out.close();
      sock.close();

      started = false;

    } catch (IOException e) {
      e.printStackTrace();
    }

    if (listener != null) {
      listener.onDisconnected(!started);
    }

    return !started;
  }

  public HttpUriRequest getUpdateMetadataRequest(String song, String artist, String album) {
    if (!started) {
      return null;
    }

    String metadata =
        metadataTemplate.replace("_song_", song).replace("_artist_", artist)
            .replace("_album_", album);

    final MetaDataHttpRequestParams params = new MetaDataHttpRequestParams();
    params.put("mode", "updinfo");
    params.put("charset", "UTF-8");
    params.put("song", metadata);
    params.put("pass", password);

    final HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("User-Agent", USER_AGENT);

    HttpUriRequest httpRequest =
        new HttpGet(MetaDataHttpRequestParams.getUrlWithQueryString(
            "http://" + host + ":" + Integer.toString(port) + "/admin.cgi", params));
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        httpRequest.addHeader(entry.getKey(), entry.getValue());
      }
    }

    return httpRequest;
  }

  @Override
  public void updateMetadata(String song, String artist, String album) {
    if (!started) {
      return;
    }

    HttpClient httpClient = new DefaultHttpClient();
    HttpParams httpParams = httpClient.getParams();
    HttpConnectionParams.setConnectionTimeout(httpParams, timeout);
    HttpConnectionParams.setSoTimeout(httpParams, timeout);

    HttpUriRequest request = getUpdateMetadataRequest(song, artist, album);

    try {
      httpClient.execute(request);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public int getTimeout() {
    return timeout;
  }

  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }
}
