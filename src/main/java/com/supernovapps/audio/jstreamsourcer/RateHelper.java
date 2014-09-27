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

public class RateHelper {

  private long burst = 0;
  private float kBps = 0;
  private long written = 0;
  private long startedAt = 0;

  public RateHelper(int kbps, int burst) {
    kBps = kbps / 8 * 1024;
    this.burst = burst;
  }

  public void wait(int w) {
    long s = getWaitTime(w);
    if (s > 0) {
      try {
        Thread.sleep(s);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public long getWaitTime(int w) {
    written += w;
    if (startedAt == 0) {
      startedAt = System.currentTimeMillis();
    }

    long t = (long) (written / kBps) * 1000;
    long r = System.currentTimeMillis() - startedAt;

    return t - r - burst;
  }
}
