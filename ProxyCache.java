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
            SHttpErrorResponse errorResponse = new SHttpErrorResponse(errorCode);
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
            if ((errorCode = request.getError()) != 0) { // check for error
                new SHttpErrorResponse(errorCode).send(client); // craft and send a response
                return;
            }
            System.out.println("---> Request --->\n" + request.toString()); // Debug
        } catch (IOException e) {
            System.out.println("Error reading request from client: " + e);
            new SHttpErrorResponse(500).send(client);
            return;

        }
        // create key
        String key = request.getURL() + request.getPort();
        // Check if key is in cache
        if ((response = cache.get(key)) != null) {
            // response is set
            System.out.println("Retrieved" + key + " from cache.");
        } else {
            /* Send request to server */
            try {
                server = new Socket(request.getHost(), request.getPort());
                DataOutputStream toServer = new DataOutputStream(server.getOutputStream());
                 toServer.writeBytes(request.toString());
            } catch (UnknownHostException e) {
                System.out.println("Unknown host: " + request.getHost());
                System.out.println(e);
                new SHttpErrorResponse(404).send(client);
                return;
            } catch (IOException e) {
                System.out.println("Error writing request to server: " + e);
                new SHttpErrorResponse(500).send(client);
                return;
            }

            /* Read response and forward it to client */
            try {
                DataInputStream fromServer = new DataInputStream(server.getInputStream());
                response = new HttpResponse(fromServer);
                // if ((errorCode = response.getError()) != 0) { // check for error
                //     errorResponse = new SHttpErrorResponse(errorCode);
                //     errorResponse.send(client);
                //     return;
                // }

                // Save response to cache
                cache.put(key, response);
                System.out.println("<--- Response <--- \n" + response.toString()); // Debug
                server.close();
            } catch (IOException e) {
                System.out.println("Error reading response from server: " + e);
                new SHttpErrorResponse(520).send(client);
            }
        } // end else

        try {
            /* Write response to client. First headers, then body */
            DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
            toClient.writeBytes(response.toString()); // headers
            // toClient.write(response.getBody()); // body
            response.body.writeTo(toClient); // body
            client.close();
        } catch (IOException e) {
            System.out.println("Error writing response to client: " + e);
            new SHttpErrorResponse(500).send(client);
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
