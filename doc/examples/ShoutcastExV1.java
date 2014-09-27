import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.supernovapps.audio.jstreamsourcer.ShoutcastV1;

public class ShoutcastExV1 {

  public static void main(String[] args) {
    ShoutcastV1 shoutcast = new ShoutcastV1(128, 5000);
    shoutcast.setHost("localhost");
    shoutcast.setPassword("source01");
    shoutcast.setPort(8001);
    shoutcast.setTimeout(30);
    shoutcast.start();

    String fileName = args[1];

    try {
      byte[] buffer = new byte[4096];

      FileInputStream inputStream = new FileInputStream(fileName);

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
