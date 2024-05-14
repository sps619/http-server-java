import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class Main {

  static String crlf = "\r\n";

  public static void main(String[] args) {
    
    ServerSocket serverSocket = null;
    Socket clientSocket = null;

    try {
      serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      clientSocket = serverSocket.accept(); // Wait for connection from client.
      String httpOKResponse = "HTTP/1.1 200 OK";
      String http404Response = "HTTP/1.1 404 Not Found";
      String contentType = "Content-Type: text/plain";
      String content = "Content-Length: ";
      OutputStream clientOutput = clientSocket.getOutputStream();

      BufferedReader bufferReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String UserAgent = null;
      String input = bufferReader.readLine();
      while(true){
        String line = bufferReader.readLine();
        if(line.isEmpty() || line == null)
          break;
        if(line.contains("User-Agent:"))
          UserAgent = line;
      }
      if(input.split(" ")[1].equals("/")){
        String rootOkResponse = httpOKResponse+crlf+crlf;
        clientOutput.write(rootOkResponse.getBytes());
        setResponse(clientOutput,httpOKResponse,null,null,null);
      }

      else if(input.contains("/echo/")){
        String length = String.valueOf(input.split(" ")[1].split("/echo/")[1].length());
        String contentLength = content+length;
        String response = input.split(" ")[1].split("/echo/")[1];
        setResponse(clientOutput,httpOKResponse,contentType,contentLength,response);
      }
      else if(input.contains("/user-agent")){
        String response = UserAgent.split(" ")[1];
        String contentLength = content+response.length();
        System.out.println("UserAgent: "+UserAgent+" response: "+response+" input: "+input);
        //String contentLength = null;//content+String.valueOf(agent.length());
        setResponse(clientOutput,httpOKResponse,contentType,contentLength,response);
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

  private static void setResponse(OutputStream clientOutput, String httpStatus, String contentType, String contentLength, String response) throws IOException{
    String output = httpStatus+crlf+(contentType==null?"":(contentType+crlf))+(contentLength==null?"":(contentLength+crlf))+crlf+(response==null?"":response);
    clientOutput.write(output.getBytes());
  }
}