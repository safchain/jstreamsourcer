import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.supernovapps.audio.jstreamsourcer.ShoutcastV2;

public class ShoutcastExV2 {

  public static void main(String[] args) {
    ShoutcastV2 shoutcast = new ShoutcastV2(320, 1000);
    shoutcast.setHost("localhost");
    shoutcast.setSid("1");
    shoutcast.setUid("1");
    shoutcast.setPassword("toto1");
    shoutcast.setPort(8000);
    shoutcast.setTimeout(30);
    shoutcast.start();

    String fileName = "test.mp3";

    while (true) {
      try {
        byte[] buffer = new byte[4024];

        buffer = new byte[4024];

        FileInputStream inputStream = new FileInputStream(fileName);

        shoutcast.updateMetadata("ARF", "ERF", "URF");

        int total = 0;
        int nRead = 0;
        while ((nRead = inputStream.read(buffer)) != -1) {
          shoutcast.write(buffer, nRead);
          total += nRead;
        }

        inputStream.close();

        System.out.println("Read " + total + " bytes");
      } catch (FileNotFoundException ex) {
        System.out.println("Unable to open file '" + fileName + "'");
      } catch (IOException ex) {
        System.out.println("Error reading file '" + fileName + "'");
      }
    }
  }
}
