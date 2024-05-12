import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Main {
  public static void main(String[] args) {
    
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    
    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept(); // Wait for connection from client.
      String httpResponse = "HTTP/1.1 200 OK\r\n\r\n";
      OutputStream clientOutput = clientSocket.getOutputStream();
      clientOutput.write(httpResponse.getBytes(StandardCharsets.UTF_8));
      clientOutput.flush();
      clientOutput.close();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
