import java.net.*;
import java.io.*;
import java.util.*;

public class ProxyCache {
    /** Port for the proxy */
    private static int port;
    /** Socket for client connections */
    private static ServerSocket socket;
    // Memory Cache
    private static SCache cache;

    /** Create the ProxyCache object and the socket */
    public static void init(int p) {
        port = p;
        cache = new SCache();
        try {
            socket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Error creating socket: " + e);
            System.exit(-1);
        }
    }

    public static int ifErrorSend(int errorCode, Socket client) {
        if (errorCode != 0) {
            HttpResponse errorResponse = new HttpResponse(errorCode);
            errorResponse.send(client);
            return 1;
        }
        return 0;
    }

    public static void handle(Socket client) {
        int errorCode = 0;
        Socket server = null;
        HttpRequest request = null;
        HttpResponse response = null;

        /* Process request. If there are any exceptions, then simply
         * return and end this request. This unfortunately means the
         * client will hang for a while, until it timeouts. */

        /* Read request */
        try {
            BufferedReader fromClient = new BufferedReader(new InputStreamReader(client.getInputStream()));
            request = new HttpRequest(fromClient);
            System.out.println("---> Request --->\n" + request.toString()); // Debug
        } catch (IOException e) {
            System.out.println("Error reading request from client: " + e);
            new HttpResponse(400).send(client);
            return;
        }

        // create key
        String key = request.getURL();
        // Check if key is in cache
        if ((response = cache.get(key)) != null && response.isValid()) { // get item from cache if the cache is still fresh
            // response is set and valid
            System.out.println("Retrieved " + key);
            System.out.println("Is valid? " + response.isValid());
        } else if (!request.method.equals("CONNECT")) { // handle requests except special case of CONNECT

            /* Send request to server */
            response = request.send();

            /* Read response and forward it to client */
            cache.put(key, response);// Save response to cache
            System.out.println("<--- Response <--- \n" + response.toString()); // Debug
        } // end else

        // http://stackoverflow.com/questions/16358589/implementing-a-simple-https-proxy-application-with-java
        // http://stackoverflow.com/questions/18273703/tunneling-two-socket-client-in-java#18274109
        if (request.method.equals("CONNECT")) {
            try {
                System.out.println("CONNECT found! Tunnelling connection.");
                server = new Socket(request.getHost(), request.getPort());
                InputStream fromClient = client.getInputStream();
                OutputStream toClient = client.getOutputStream();
                InputStream fromServer = server.getInputStream();
                OutputStream toServer = server.getOutputStream();
                new HttpResponse(200).setMessage("Connection established").setVersion("HTTP/1.0").send(client);

                // request
                while (true) {
                    for (int i = 0; (i = fromClient.read()) != -1; i++) {
                        toServer.write(i);
                        if (fromClient.available() == 0) {
                            break;
                        }
                    }
                    // response
                    for (int i = 0; (i = fromServer.read()) != -1; i++) {
                        toClient.write(i);
                        if (fromServer.available() == 0) {
                            break;
                        }
                    }
                }
                // return;
            } catch (UnknownHostException e) {
                System.out.println("Unknown host: " + request.getHost());
                System.out.println(e);
                new HttpResponse(404).send(client);
                return;
            } catch (IOException e) {
                System.out.println("Error writing request to server: " + e);
                new HttpResponse(500).send(client);
                return;
            }
        } else {
            try {
                /* Write response to client. First headers, then body */
                DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
                toClient.writeBytes(response.toString()); // headers
                response.body.writeTo(toClient); // body
                client.close();
            } catch (IOException e) {
                System.out.println("Error writing response to client: " + e);
                // new HttpResponse(500).send(client);
            }
        }
    }

/* -------------------------------------------------- */


    /** Read command line arguments and start proxy */
    public static void main(String args[]) {
        int myPort = 0;

        try {
            myPort = Integer.parseInt(args[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Need port number as argument");
            System.exit(-1);
        } catch (NumberFormatException e) {
            System.out.println("Please give port number as integer.");
            System.exit(-1);
        }
        init(myPort);

        /** Main loop. Listen for incoming connections and spawn a new
         * thread for handling them */
        Socket client = null;

        while (true) {
            try {
                client = socket.accept();
                handle(client);
            } catch (IOException e) {
                System.out.println("Error reading request from client: " + e);
                /* Definitely cannot continue processing this request,
                 * so skip to next iteration of while loop. */
            continue;
            }
        }
    }
}
