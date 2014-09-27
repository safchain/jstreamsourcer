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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;

public class Icecast extends Sourcer {
  private String host = null;
  private int port = 0;
  private String path = null;

  private String username = null;
  private String password = null;

  private Socket sock = null;

  private String metadataTemplate = "_album_ _song_ _artist_";

  public Icecast(int kbps, int burst) {
    super(kbps, burst);
  }

  public boolean start(Socket sock) {
    try {
      this.sock = sock;
      sock.connect(new InetSocketAddress(host, port), timeout);
      sock.setSendBufferSize(64 * 1024);
      out = sock.getOutputStream();

      PrintWriter output = new PrintWriter(out);
      output.println("SOURCE " + path + " HTTP/1.0");

      writeAuthentication(output);
      writeHeaders(output);

      output.println("");
      output.flush();

      InputStreamReader isr = new InputStreamReader(sock.getInputStream());
      BufferedReader in = new BufferedReader(isr);
      String line = in.readLine();

      if (line == null || !line.contains("HTTP") || !line.contains("OK")) {
        if (listener != null) {
          listener.onError("Connection / Authentification error");
        }
        return false;
      }
    } catch (Exception e) {
      e.printStackTrace();

      try {
        if (sock != null) {
          sock.close();
        }
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

  @Override
  public boolean start() {
    return start(new Socket());
  }

  private void writeAuthentication(PrintWriter output) {
    String authString = username + ":" + password;
    String token = Base64.encodeBase64String(authString.getBytes());
    output.println("Authorization: Basic " + token);
  }

  private void writeHeaders(PrintWriter output) {
    LinkedHashMap<String, String> headers = new LinkedHashMap<String, String>();
    headers.put("User-Agent", USER_AGENT);
    headers.put("icy-notice1", USER_AGENT);

    for (Entry<String, String> entry : headers.entrySet()) {
      output.println(entry.getKey() + ": " + entry.getValue());
    }

    for (Entry<String, String> entry : streamInfos.entrySet()) {
      output.println(entry.getKey() + ": " + entry.getValue());
    }
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

    MetaDataHttpRequestParams params = new MetaDataHttpRequestParams();
    params.put("mode", "updinfo");
    params.put("mount", path);
    params.put("charset", "UTF-8");
    params.put("song", metadata);

    String authString = username + ":" + password;
    String token = Base64.encodeBase64String(authString.getBytes());

    HashMap<String, String> headers = new HashMap<String, String>();
    headers.put("Authorization:", "Basic " + token);
    headers.put("User-Agent", USER_AGENT);

    HttpUriRequest request =
        new HttpGet(MetaDataHttpRequestParams.getUrlWithQueryString("http://" + host + ":"
            + Integer.toString(port) + "/admin/metadata", params));
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        request.addHeader(entry.getKey(), entry.getValue());
      }
    }

    return request;
  }

  @Override
  public void updateMetadata(String song, String artist, String album) {
    if (!started) {
      return;
    }

    RequestConfig requestConfig =
        RequestConfig.custom().setSocketTimeout(timeout * 1000).setConnectTimeout(timeout * 1000)
            .build();
    HttpClient httpClient =
        HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();

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

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }
}
