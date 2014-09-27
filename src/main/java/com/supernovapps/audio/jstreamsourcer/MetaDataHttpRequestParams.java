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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.concurrent.ConcurrentHashMap;

public class MetaDataHttpRequestParams {
  private static String ENCODING = "ISO-8859-1";

  private ConcurrentHashMap<String, String> params;

  public MetaDataHttpRequestParams() {
    params = new ConcurrentHashMap<String, String>();
  }

  public void put(String k, String v) {
    if (k != null && v != null) {
      params.put(k, v);
    }
  }

  public static String getUrlWithQueryString(String url, MetaDataHttpRequestParams params) {
    if (params != null) {
      String paramString = params.toString();
      if (url.indexOf("?") == -1) {
        url += "?" + paramString;
      } else {
        url += "&" + paramString;
      }
    }

    return url;
  }

  public String toString() {
    StringBuilder result = new StringBuilder();
    try {
      for (ConcurrentHashMap.Entry<String, String> entry : params.entrySet()) {
        if (result.length() > 0) {
          result.append("&");
        }

        String k = URLEncoder.encode(entry.getKey(), ENCODING);
        String v = URLEncoder.encode(entry.getValue(), ENCODING);

        result.append(k);
        result.append("=");
        result.append(v);
      }
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }

    return result.toString();
  }
}
