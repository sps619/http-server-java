import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Files;

public class Main {

  static String crlf = "\r\n";

  public static void main(String[] args) {
    ExecutorService threadPool = Executors.newFixedThreadPool(6);
    try{
      ServerSocket serverSocket = new ServerSocket(4221);
      serverSocket.setReuseAddress(true);
      while(true) {
        Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
        System.out.println("Connection Accepted!!");
        threadPool.submit(()->sendResponse(clientSocket,args));
      }
    }
    catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  private static void sendResponse(Socket clientSocket, String[] args){
    try {
      String httpOKResponse = "HTTP/1.1 200 OK";
      String http404Response = "HTTP/1.1 404 Not Found";
      String http201Response = "HTTP/1.1 201 Created";
      String contentType = "Content-Type: text/plain";
      String content = "Content-Length: ";
      OutputStream clientOutput = clientSocket.getOutputStream();

      BufferedReader bufferReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
      String UserAgent = null;
      String data=null;
      int contentLengthPOST=0;
      String input = bufferReader.readLine();
      while(true){
        String line = bufferReader.readLine();
        if(line.isEmpty() || line == null){
            break;
          }
        if(line.contains("User-Agent:"))
          UserAgent = line;  
        if(line.contains(content)){
          contentLengthPOST = Integer.parseInt(line.split(" ")[1]);
          System.out.println("Content Length: "+contentLengthPOST);
        }
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
        setResponse(clientOutput,httpOKResponse,contentType,contentLength,response);
      }
      else if(input.contains("/files/")){
        String fileName = input.split(" ")[1].split("/")[2];
        String directory = null;
        if(args.length>1 && args[0].equals("--directory") && args[1]!=null){
          directory = args[1].endsWith("/")?args[1]:args[1]+"/";
        }
        String filePath = directory+fileName;
        if(input.contains("GET")){
          File file = new File(filePath);
          if(file.exists()){         
            int fileSize = (int) file.length();
            byte[] fileContent = new byte[fileSize];
            FileInputStream fileInput = new FileInputStream(file);
            fileInput.read(fileContent);
            contentType = "Content-Type: application/octet-stream";
            String contentLength = content+fileSize;
            setFileResponse(clientOutput,httpOKResponse,contentType,contentLength,null,fileContent);
            fileInput.close();
          }
          else{
            set404Response(clientOutput,http404Response);
          }
        }
        else if(input.contains("POST")){
            char[] charBuff = new char[contentLengthPOST];
            bufferReader.read(charBuff);
            String body = new String(charBuff);
            Path file = Path.of(filePath);
            Files.writeString(file, body);
            setResponse(clientOutput,http201Response, null, null, null);
        }
        
      }
      else{
        set404Response(clientOutput,http404Response);
      }
      clientOutput.flush();
      clientOutput.close();
      System.out.println("Connection Closed!!");
    }
    catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
    finally{
      try{
        clientSocket.close();
      }
      catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
      }
    }
  }

  private static void setResponse(OutputStream clientOutput, String httpStatus, String contentType, String contentLength, String response) throws IOException{
    String output = httpStatus+crlf+(contentType==null?"":(contentType+crlf))+(contentLength==null?"":(contentLength+crlf))+crlf+(response==null?"":response);
    clientOutput.write(output.getBytes());
  }

  private static void setFileResponse(OutputStream clientOutput, String httpStatus, String contentType, String contentLength, String response, byte[] fileContent) throws IOException{
    String output = httpStatus+crlf+(contentType==null?"":(contentType+crlf))+(contentLength==null?"":(contentLength+crlf))+crlf+(response==null?"":response);
    clientOutput.write(output.getBytes());
    clientOutput.write(fileContent);
  }
  
  private static void set404Response(OutputStream clientOutput, String httpStatus){
    String output = httpStatus+crlf+crlf;
    try{
      clientOutput.write(output.getBytes());
    }
    catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

}
    
