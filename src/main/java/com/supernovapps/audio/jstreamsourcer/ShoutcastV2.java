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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.supernovapps.audio.jstreamsourcer.ultravox2.Message;
import com.supernovapps.audio.jstreamsourcer.ultravox2.MessageFactory;

public class ShoutcastV2 extends Sourcer {
  private String host = null;
  private int port = 0;
  private String path = null;
  private String username = null;
  private String password = null;
  private String sid = "";
  private String uid = "";

  private Socket sock = null;
  private InputStream in = null;

  private int bufferSize = 320;
  private int maxPayload = 4096;

  public ShoutcastV2(int kbps, int burst) {
    super(kbps, burst);
  }

  public boolean start(Socket sock) {
    Message msg = null;

    try {
      this.sock = sock;
      sock.connect(new InetSocketAddress(host, port), 5000);
      sock.setSendBufferSize(64 * 1024);
      out = sock.getOutputStream();
      in = sock.getInputStream();

      msg = MessageFactory.getRequestCipherMessage();
      out.write(msg.encode());
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }

    while (!started) {
      try {
        msg = Message.read(in);
        if (msg.msgClass != Message.CLASS_BROADCASTER) {
          return false;
        }

        String payload = new String(msg.payload).trim();
        String[] data = payload.split(":");

        if (data[0].compareTo("ACK") != 0) {
          throw new Exception(data[0]);
        }

        switch (msg.msgType) {
          case Message.BROADCAST_REQUEST_CIPHER:
            String cipherkey = data[1];

            msg = MessageFactory.getBroadcastAuthRequest(sid, uid, password, cipherkey);
            out.write(msg.encode());

            break;
          case Message.BROADCAST_AUTHENTIFICATE:
            if (data[1].compareTo(Message.ULTVX_VERSION) != 0) {
              throw new Exception(data[1]);
            }

            if (data[2].compareTo("Allow") != 0) {
              throw new Exception(data[2]);
            }

            msg = MessageFactory.getStreamMimeType("audio/mpeg");
            out.write(msg.encode());

            break;
          case Message.BROADCAST_STREAM_MINE_TYPE:
            msg = MessageFactory.getSetupBroadcast(kbps * 1000, kbps * 1000);
            out.write(msg.encode());

            break;
          case Message.BROADCAST_SETUP:
            msg = MessageFactory.getNegociateBufferSize(bufferSize, bufferSize);
            out.write(msg.encode());

            break;
          case Message.BROADCAST_NEGOTIATE_BUFFER_SIZE:
            msg = MessageFactory.getNegociateMaxPayloadSize(maxPayload, maxPayload);
            out.write(msg.encode());

            break;
          case Message.BROADCAST_NEGOTIATE_MAX_PAYLOAD:
            msg = MessageFactory.getIcyGenre(streamInfos.get("icy-genre"));
            out.write(msg.encode());

            break;
          case Message.BROADCAST_CONFIGURE_ICY_GENRE:
            msg = MessageFactory.getIcyName(streamInfos.get("icy-name"));
            out.write(msg.encode());

            break;
          case Message.BROADCAST_CONFIGURE_ICY_NAME:
            String pub = streamInfos.get("icy-pub");
            if (pub.compareTo("1") == 0) {
              msg = MessageFactory.getIcyPub(true);
            } else {
              msg = MessageFactory.getIcyPub(false);
            }

            out.write(msg.encode());

            break;
          case Message.BROADCAST_CONFIGURE_ICY_PUB:
            msg = MessageFactory.getIcyUrl(streamInfos.get("icy-url"));
            out.write(msg.encode());

            break;
          case Message.BROADCAST_CONFIGURE_ICY_URL:
            msg = MessageFactory.getStandby();
            out.write(msg.encode());

            break;
          case Message.BROADCAST_STANDBY:
            if (data[1].compareTo("Data transfer mode") != 0)
              throw new Exception(data[1]);

            if (listener != null) {
              listener.onConnected();
            }

            started = true;

            break;
          default:
            break;
        }
      } catch (Exception ex) {
        if (listener != null) {
          listener.onError(ex.getMessage());
        }
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean start() {
    return start(new Socket());
  }

  @Override
  public void write(byte[] data, int size) {
    Message msg = MessageFactory.getData(Message.DATA1_MP3, data, size);

    byte[] buffer = msg.encode();
    super.write(buffer, buffer.length);
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

  @Override
  public void updateMetadata(String song, String artist, String album) {
    if (!started) {
      return;
    }

    Message[] msgs = MessageFactory.getMetadata(song, artist, album, maxPayload);
    for (Message msg : msgs) {
      byte[] buffer = msg.encode();
      super.write(buffer, buffer.length);
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

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public int getMaxPayload() {
    return maxPayload;
  }

  public void setMaxPayload(int maxPayload) {
    this.maxPayload = maxPayload;
  }

  public String getSid() {
    return sid;
  }

  public void setSid(String sid) {
    this.sid = sid;
  }

  public String getUid() {
    return uid;
  }

  public void setUid(String uid) {
    this.uid = uid;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }
}
