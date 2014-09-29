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
import java.io.OutputStream;
import java.util.LinkedHashMap;

import com.supernovapps.audio.jstreamsourcer.OnSourcerListener;
import com.supernovapps.audio.jstreamsourcer.RateHelper;

public abstract class Sourcer {
  public final static String USER_AGENT = "JStreamSourcer/1.0";
  public static final String ICY_BR = "icy-br";
  public static final String ICY_URL = "icy-url";
  public static final String ICY_NAME = "icy-name";
  public static final String ICY_PUB = "icy-pub";
  public static final String ICY_GENRE = "icy-genre";
  public static final String CONTENT_TYPE = "content-type";

  protected OutputStream out;

  protected boolean started = false;

  protected RateHelper rateHelper = null;

  protected OnSourcerListener listener = null;

  protected int kbps = 320;

  protected int timeout = 5000;

  protected LinkedHashMap<String, String> streamInfos;

  /**
   * Instantiates a new sourcer.
   *
   * @param kbps, used for the stream, ex: 128, 256, 320, etc.
   * @param burst, number of seconds without any rate limiting. The packet will
   * be sent as faster as possible. 
   */
  public Sourcer(int kbps, int burst) {
    this.kbps = kbps;

    rateHelper = new RateHelper(kbps, burst);

    streamInfos = new LinkedHashMap<String, String>();
    streamInfos.put(CONTENT_TYPE, "audio/mpeg");
    streamInfos.put(ICY_GENRE, "N/A");
    streamInfos.put(ICY_PUB, "0");
    streamInfos.put(ICY_NAME, "N/A");
    streamInfos.put(ICY_URL, "N/A");
    streamInfos.put(ICY_BR, String.valueOf(kbps));
  }

  /*
   * abstract method
   */
  /**
   * Initiates a connection to the stream server
   *
   * @return true, if successfully connected
   */
  public abstract boolean start();
  
  /**
   * Checks if the streaming is started.
   *
   * @return true, if is started
   */
  public abstract boolean isStarted();
  
  /**
   * Stop the connection
   *
   * @return true, if successful
   */
  public abstract boolean stop();

  /**
   * Writes data to the openened streaming connection
   *
   * @param data, buffer of data
   * @param size, the size of the data buffer
   */
  public void write(byte[] data, int size) {
    if (!started || size <= 0) {
      return;
    }

    if (rateHelper != null) {
      rateHelper.wait(size);
    }

    try {
      out.write(data, 0, size);
    } catch (IOException e) {
      e.printStackTrace();

      stop();
    }
  }

  /**
   * Update metadata of the current streaming
   *
   * @param song, the song
   * @param artist, the artist
   * @param album, the album
   */
  public abstract void updateMetadata(final String song, final String artist, final String album);

  /**
   * Sets the OnSourcerListener. This is used to get notification when there is modification
   * of the current status of the streaming connection.
   *
   * @param l the new OnSourcerListener
   */
  public final void setOnSourcerListener(OnSourcerListener l) {
    listener = l;
  }

  /**
   * Gets the timeout that is used for the connection.
   *
   * @return the timeout
   */
  public int getTimeout() {
    return timeout;
  }

  /**
   * Sets the timeout that is used for the connection.
   *
   * @param timeout the new timeout
   */
  public void setTimeout(int timeout) {
    this.timeout = timeout;
  }

  /**
   * Sets the stream info. Permits to specify informations related to the
   * current streaming connection. Ex: url, genre, etc.
   *
   * @param key the key
   * @param value the value
   */
  public void setStreamInfo(String key, String value) {
    streamInfos.put(key, value);
  }

  /**
   * Gets the stream info.
   *
   * @param key the key
   * @return the stream info
   */
  public String getStreamInfo(String key) {
    return streamInfos.get(key);
  }
}
