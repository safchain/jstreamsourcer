import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.supernovapps.audio.jstreamsourcer.Icecast;

public class IcecastEx {

  public static void main(String[] args) {
    Icecast icecast = new Icecast(320, 5000);
    icecast.setHost("localhost");
    icecast.setUsername("source");
    icecast.setPassword("hackme");
    icecast.setPath("/live");
    icecast.setPort(8000);
    icecast.start();

    String fileName = args[1];

    try {
      byte[] buffer = new byte[4096];

      FileInputStream inputStream = new FileInputStream(fileName);

      int total = 0;
      int nRead = 0;
      while ((nRead = inputStream.read(buffer)) != -1) {
        icecast.write(buffer, nRead);

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
