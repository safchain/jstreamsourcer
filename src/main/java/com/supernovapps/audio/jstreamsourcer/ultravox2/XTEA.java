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

public class XTEA {

  @SuppressWarnings("rawtypes")
  private static Object resizeArray(Object oldArray, int newSize) {
    int oldSize = java.lang.reflect.Array.getLength(oldArray);
    Class elementType = oldArray.getClass().getComponentType();
    Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
    int preserveLength = Math.min(oldSize, newSize);
    if (preserveLength > 0) {
      System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
    }
    return newArray;
  }

  public void encipher(int[] v, int[] k, int num_rounds) {
    if (num_rounds == 0) {
      num_rounds = 32;
    }

    int v0 = v[0], v1 = v[1], i;
    int sum = 0, delta = 0x9E3779B9;

    for (i = 0; i < num_rounds; i++) {
      v0 += (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + k[sum & 3]);
      sum += delta;
      v1 += (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + k[(sum >>> 11) & 3]);
    }
    v[0] = v0;
    v[1] = v1;
  }

  public void decipher(int[] v, int[] k, int num_rounds) {
    if (num_rounds == 0) {
      num_rounds = 32;
    }

    int v0 = v[0], v1 = v[1], i;
    int delta = 0x9E3779B9, sum = delta * num_rounds;

    for (i = 0; i < num_rounds; i++) {
      v1 -= (((v0 << 4) ^ (v0 >>> 5)) + v0) ^ (sum + k[(sum >>> 11) & 3]);
      sum -= delta;
      v0 -= (((v1 << 4) ^ (v1 >>> 5)) + v1) ^ (sum + k[sum & 3]);
    }
    v[0] = v0;
    v[1] = v1;
  }

  public int fourCharsToLong(byte[] s, int i) {
    int l = 0;
    l |= s[0 + i];
    l <<= 8;
    l |= s[1 + i];
    l <<= 8;
    l |= s[2 + i];
    l <<= 8;
    l |= s[3 + i];
    return l;
  }

  public void longToFourChars(int l, byte[] r) {
    r[3] = (byte) (l & 0xff);
    l >>>= 8;
    r[2] = (byte) (l & 0xff);
    l >>>= 8;
    r[1] = (byte) (l & 0xff);
    l >>>= 8;
    r[0] = (byte) (l & 0xff);
    l >>>= 8;
  }

  public String XTEA_encipher(byte[] c_data, byte[] c_key) {
    /*
     * key is always 128 bits
     */
    if (c_key.length < 16) {
      c_key = (byte[]) resizeArray(c_key, 16);
    }

    int[] k = new int[4];
    k[0] = fourCharsToLong(c_key, 0);
    k[1] = fourCharsToLong(c_key, 4);
    k[2] = fourCharsToLong(c_key, 8);
    k[3] = fourCharsToLong(c_key, 12);

    /*
     * data is multiple of 64 bits
     */
    int size = c_data.length;
    if (size % 8 != 0) {
      size += 8 - size % 8;
      c_data = (byte[]) resizeArray(c_data, size);
    }

    String result = "";
    for (int x = 0; x < size; x += 8) {
      int[] v = new int[2];
      v[0] = fourCharsToLong(c_data, x);
      v[1] = fourCharsToLong(c_data, x + 4);
      encipher(v, k, 32);

      result = result.concat(String.format("%08x", v[0]));
      result = result.concat(String.format("%08x", v[1]));
    }

    return result;
  }
}
