import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.zip.GZIPOutputStream;

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
      String contentEncoding = "Content-Encoding: ";
      String encodingVal = null;
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
        }
        if(line.contains("Accept-Encoding:")){
          for(String str : line.split("Accept-Encoding: ")[1].split(",") ){
            if(str.trim().toLowerCase().equals("gzip"))
              encodingVal = "gzip";            
          } 

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
        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzip = new GZIPOutputStream(arrayOutputStream);
        if( encodingVal==null || encodingVal.contains("invalid-encoding")){
          encodingVal=null;
        }
        else{
          encodingVal=contentEncoding+encodingVal;
        }
        setCompResponse(clientOutput,httpOKResponse,contentType,contentLength,response,encodingVal,gzip,arrayOutputStream);

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
  private static void setCompResponse(OutputStream clientOutput, String httpStatus, String contentType, String contentLength, String response, String encoding, GZIPOutputStream gzip, ByteArrayOutputStream arrayOutputStream) throws IOException{
    if(encoding!=null){  
      if(encoding.contains("gzip")){
        gzip.write(response.getBytes());
        gzip.close();
        byte[] compressedResponse = arrayOutputStream.toByteArray();     
        contentLength = "Content-Length: "+String.valueOf(compressedResponse.length);
        String output = httpStatus+crlf+(encoding==null?"":encoding+crlf)+(contentType==null?"":(contentType+crlf))+(contentLength==null?"":(contentLength+crlf))+crlf;
        clientOutput.write(output.getBytes()); 
        clientOutput.write(compressedResponse);
      }
    } 
      else{
        String output = httpStatus+crlf+(encoding==null?"":encoding+crlf)+(contentType==null?"":(contentType+crlf))+(contentLength==null?"":(contentLength+crlf))+crlf+(response==null?"":response);
        clientOutput.write(output.getBytes());
      }
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
    
