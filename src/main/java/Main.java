import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class Main {
  public static void main(String[] args) {
    
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    
    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept(); // Wait for connection from client.
      String httpOKResponse = "HTTP/1.1 200 OK\r\n\r\n";
      String http404Response = "HTTP/1.1 404 Not Found\r\n\r\n";
      OutputStream clientOutput = clientSocket.getOutputStream();

      BufferedReader bufferReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      if(bufferReader.readLine().contains("/")){
        clientOutput.write(httpOKResponse.getBytes(StandardCharsets.UTF_8));
      }
      else{
        clientOutput.write(http404Response.getBytes(StandardCharsets.UTF_8));
      }

      clientOutput.flush();
      clientOutput.close();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}
