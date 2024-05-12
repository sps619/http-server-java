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
      String httpOKResponse = "HTTP/1.1 200 OK";
      String http404Response = "HTTP/1.1 404 Not Found";
      String crlf = "\r\n";
      String contentType = "Content-Type: text/plain";
      String contentLength = "Content-Length: ";
      OutputStream clientOutput = clientSocket.getOutputStream();

      BufferedReader bufferReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String input = bufferReader.readLine();
      if(input.split(" ")[1].equals("/")){
        clientOutput.write(httpOKResponse.getBytes());
        clientOutput.write(crlf.getBytes());
        clientOutput.write(crlf.getBytes());
      }

      else if(input.contains("/echo/")){
        clientOutput.write(httpOKResponse.getBytes());
        clientOutput.write(crlf.getBytes());
        clientOutput.write(contentType.getBytes());
        clientOutput.write(crlf.getBytes());
        clientOutput.write(contentLength.getBytes());
        clientOutput.write(String.valueOf(input.split(" ")[1].split("/echo/")[1].length()).getBytes());
        clientOutput.write(crlf.getBytes());
        clientOutput.write(crlf.getBytes());
        clientOutput.write(input.split(" ")[1].split("/echo/")[1].getBytes());
      }
      else{
        clientOutput.write(http404Response.getBytes());
        clientOutput.write(crlf.getBytes());
        clientOutput.write(crlf.getBytes());
      }
      clientOutput.flush();
      clientOutput.close();
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }
}