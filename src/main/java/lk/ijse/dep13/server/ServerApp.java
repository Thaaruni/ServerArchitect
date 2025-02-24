package lk.ijse.dep13.server;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

public class ServerApp {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(80);
        System.out.println("Server started on port 80");

        while (true) {
            Socket localSocket = serverSocket.accept();
            System.out.println("Accepted connection from " + localSocket.getRemoteSocketAddress());
            new Thread(() -> {

                try {
                    InputStream is = localSocket.getInputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    BufferedReader br = new BufferedReader(isr);

                    /* Let's read the command line */
                    String commandLine = br.readLine();
                    if (commandLine == null) return;
                    String[] cmdArray = commandLine.split(" ");
                    String command = cmdArray[0];
                    String resourcePath = cmdArray[1];

                    /* Let's read the request headers */
                    String host = null;
                    String line;
                    while ((line = br.readLine()) != null && !line.isBlank()) {
                        String header = line.split(":")[0].strip();
                        String value = line.substring(line.indexOf(":") + 1).strip();
                        if (header.equalsIgnoreCase("Host")) host = value;
                    }

                    OutputStream os = localSocket.getOutputStream();
                    if (!command.equalsIgnoreCase("GET")) {
                        String httpResponseHead = """
                                HTTP/1.1 405 Method Not Allowed
                                Server: dep-server
                                Date: %s
                                Content-Type: text/html
                                
                                """.formatted(LocalDateTime.now());
                        os.write(httpResponseHead.getBytes());
                        os.flush();
                        String httpResponseBody = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                <title>DEP Server | 405 Method Not Allowed</title>
                                </head>
                                <body>
                                    <h1>405 Method Not Allowed </h1>
                                    <h2>DEP Server does not support %s method.</h2>
                                </body>
                                </html>
                                """.formatted(command);
                        os.write(httpResponseBody.getBytes());
                        os.flush();
                    }else if (host == null){
                        String httpResponseHead = """
                                HTTP/1.1 400 Bad Request
                                Server: dep-server
                                Date: %s
                                Content-Type: text/html
                                
                                """.formatted(LocalDateTime.now());
                        os.write(httpResponseHead.getBytes());
                        os.flush();
                        String httpResponseBody = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                <title>DEP Server | 400 Bad Request</title>
                                </head>
                                <body>
                                    <h1>400 Bad Request</h1>
                                    <h2>Invalid Request, DEP Server is not a dedicated web server.</h2>
                                </body>
                                </html>
                                """;
                        os.write(httpResponseBody.getBytes());
                        os.flush();
                    }else{
                        /* Identify the resource that client is looking for */
                        Path path;
                        if (resourcePath.equals("/")){
                            path = Path.of("http", host, "index.html");
                        }else{
                            path = Path.of("http", host, resourcePath);
                        }
                        if (!Files.exists(path)){
                            String httpResponseHead = """
                                HTTP/1.1 404 Not Found
                                Server: dep-server
                                Date: %s
                                Content-Type: text/html
                                
                                """.formatted(LocalDateTime.now());
                            os.write(httpResponseHead.getBytes());
                            os.flush();
                            String httpResponseBody = """
                                <!DOCTYPE html>
                                <html>
                                <head>
                                <title>DEP Server | 404 Not Found</title>
                                </head>
                                <body>
                                    <h1>404 Not Found</h1>
                                    <h2>Requested resource: %s not found</h2>
                                </body>
                                </html>
                                """.formatted(resourcePath);
                            os.write(httpResponseBody.getBytes());
                            os.flush();
                        }else{
                            String httpResponseHead = """
                                HTTP/1.1 200 OK
                                Server: dep-server
                                Date: %s
                                Content-Type: %s
                                
                                """.formatted(LocalDateTime.now(), Files.probeContentType(path));
                            os.write(httpResponseHead.getBytes());
                            os.flush();

                            /* Let's add the payload */
                            FileChannel fc = FileChannel.open(path);
                            ByteBuffer buffer = ByteBuffer.allocate(1024);
                            while (fc.read(buffer) != -1){
                                buffer.flip();
                                os.write(buffer.array(), 0, buffer.limit());
                            }
                            fc.close();
                            os.flush();
                        }
                    }
                    localSocket.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }).start();
        }

    }
}




