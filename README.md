[![Build Status](https://travis-ci.org/safchain/jstreamsourcer.png)](https://travis-ci.org/safchain/jstreamsourcer)
[![Coverage Status](https://img.shields.io/coveralls/safchain/jstreamsourcer.svg)](https://coveralls.io/r/safchain/jstreamsourcer?branch=master)

jstreamsourcer
==============

Java sourcer library for Icecast, Shoutcast v1/v2 streaming servers.

Examples
--------

Icecast

```java
import com.supernovapps.audio.jstreamsourcer.Icecast;

icecast icecast = new Icecast(320, 5000);
icecast.setHost("localhost");
icecast.setUsername("source");
icecast.setPassword("password");
icecast.setPath("/live");
icecast.setPort(8000);
icecast.start();

byte[] buffer = new byte[4096];

FileInputStream inputStream = new FileInputStream("test.mp3");
icecast.updateMetadata("song", "artist", "album");

while (inputStream.read(buffer) != -1) {
  icecast.write(buffer, nRead);
}
inputStream.close();
```

Shoutcast v1

```java
import com.supernovapps.audio.jstreamsourcer.ShoutcastV1;

shoutcastV1 shoutcast = new ShoutcastV1(128, 5000);
shoutcast.setHost("localhost");
shoutcast.setPassword("password");
shoutcast.setPort(8001);
shoutcast.setTimeout(30);
shoutcast.start();

byte[] buffer = new byte[4096];

FileInputStream inputStream = new FileInputStream("test.mp3");
shoutcast.updateMetadata("song", "artist", "album");

while (inputStream.read(buffer) != -1) {
  shoutcast.write(buffer, nRead);
}
inputStream.close();
```

Shoutcast v2

```java
import com.supernovapps.audio.jstreamsourcer.ShoutcastV2;

shoutcastV2 shoutcast = new ShoutcastV2(320, 1000);
shoutcast.setHost("localhost");
shoutcast.setSid("1");
shoutcast.setUid("1");
shoutcast.setPassword("password");
shoutcast.setPort(8000);
shoutcast.setTimeout(30);
shoutcast.start();

byte[] buffer = new byte[4096];

FileInputStream inputStream = new FileInputStream("test.mp3");
shoutcast.updateMetadata("song", "artist", "album");

while (inputStream.read(buffer) != -1) {
  shoutcast.write(buffer, nRead);
}
inputStream.close();
```

License
-------

This program is free software; you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation; either version 2 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with this program; if
not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
02110-1301, USA.