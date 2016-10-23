import java.net.*;
import java.io.*;
import java.util.*;

public class ProxyCache {
    private static boolean secure = false;
    public static boolean expires = false;
    public static boolean verbose = false;

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
            socket = new ServerSocket(p);
        } catch (IOException e) {
            System.out.println("Error creating socket: " + e);
            System.exit(-1);
        }
    }

    public static void verbose(String message) {
        if (verbose) {
            System.out.println(message);
        }
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
            verbose("\n**** New Request ****");
            request = new HttpRequest(fromClient);
            verbose("---> Request --->\n" + request.toString());
            if (request.getError() != 0) {
                new HttpResponse(request.getError()).send(client);
                return;
            }
        } catch (IOException e) {
            System.out.println("Error reading request from client: " + e);
            new HttpResponse(400).send(client);
        }

        if (request.method.equals("GET")) { // Handle GET requests
            // Check if key is in cache
            if ((response = cache.get(request.getURL())) == null) { // get item from cache
                // if the item is not in the cache.

                /* Send request to server */
                response = request.send();

                /* Read response and forward it to client */
                cache.put(request.getURL(), response);// Save response to cache
            }

            verbose("<--- Response <--- \n" + response.toString());
            try {
                /* Write response to client. First headers, then body */
                DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
                toClient.writeBytes(response.toString()); // headers
                response.body.writeTo(toClient); // body
                client.close();
            } catch (IOException e) {
                System.out.println("Error writing response to client: " + e);
            }
        }

        // http://stackoverflow.com/questions/16358589/implementing-a-simple-https-proxy-application-with-java
        // http://stackoverflow.com/questions/18273703/tunneling-two-socket-client-in-java#18274109
        else if (secure && request.method.equals("CONNECT")) {
            System.out.println("CONNECT found! attempt tunnel HTTPS connection.");
            DataInputStream fromClient;
            DataOutputStream toServer;
            try {
                server = new Socket(request.getHost(), request.getPort());
                fromClient = new DataInputStream(client.getInputStream());
                toServer = new DataOutputStream(server.getOutputStream());
                new HttpResponse(200).setMessage("Connection established").setVersion("HTTP/1.0").send(client);
            } catch (UnknownHostException e) {
                System.out.println("Unknown host: " + request.getHost());
                new HttpResponse(404).send(client);
                return;
            } catch (IOException e) {
                System.out.println("Error establishing connection: " + e);
                new HttpResponse(500).send(client);
                return;
            }

            try {
                client.setSoTimeout(3000);
                server.setSoTimeout(3000);
                byte[] buffer = new byte[2048];
                int rc = 0;

                // http://stackoverflow.com/questions/31365522/java-proxy-tunnelling-https
                Thread servertToClient = new TunnelThread(server, client);
                new Thread(servertToClient).start(); // from server to client
                while (!client.isClosed() && !server.isClosed() && !client.isInputShutdown() && !server.isOutputShutdown()) {
                    while ((rc = fromClient.read(buffer)) != -1) {
                        toServer.write(buffer, 0, rc);
                        toServer.flush();
                    }
                }
                server.close();
                client.close();
            } catch (SocketTimeoutException e) {
                System.out.println("Connection Timed out " + e);
                // EXIT HERE
                return;
            } catch (IOException e) {
                System.out.println("Error tunnelling request: " + e);
            }
        } else { // e.g POST, HEAD or DELETE requests
            System.out.println("Ignoring " + request.method + " request.");
            new HttpResponse(501).send(client);
            return;
        }
    }

/* -------------------------------------------------- */


    /** Read command line arguments and start proxy */
    public static void main(String args[]) {
        int myPort = 0;

        try {
            myPort = Integer.parseInt(args[0]);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("Usage: ProxyCache <Port Number> [args]\nArguments:\n  -v, --verbose    Verbose output (more logging) \n  -s, --secure    Proxy HTTPS/TLS (experimental, can use 100% CPU)\n  -e, --expires   Check expires header");
            System.exit(-1);
        } catch (NumberFormatException e) {
            System.out.println("Please give port number as integer.");
            System.exit(-1);
        }
        init(myPort);

        for (String argument: args) { // http://java.about.com/od/javasyntax/a/Using-Command-Line-Arguments.htm
            if(argument.equals("-v") || argument.equals("--verbose")) {
                // Verbose output (more logging)
                verbose = true;
                System.out.println("Verbose logging=true");

            }
            if(argument.equals("-s") || argument.equals("--secure")) {
                // proxy secure HTTPS/TLS connections (experimental as it can course 100% CPU usage)
                secure = true;
                System.out.println("Proxy HTTPS/TLS connections. (experimental as it can course 100% CPU usage)");

            }
            if(argument.equals("-e") || argument.equals("--expires")) {
                // when validating the cache also check the expires header, this can reduce the number of requests out to the web.
                expires = true;
                System.out.println("Check expires header when checking the freshness of an object in the cache.");
            }
        }

        /** Main loop. Listen for incoming connections and spawn a new
         * thread for handling them */
        Socket client = null;

        while (true) {
            try {
                client = socket.accept();
                new Thread(new ProxyThread(client)).start();
            } catch (IOException e) {
                System.out.println("Error reading request from client: " + e);
                /* Definitely cannot continue processing this request,
                 * so skip to next iteration of while loop. */
                continue;
            }
        }
    }
}



class ProxyThread extends Thread {
    Socket client;

    ProxyThread(Socket client) {
        this.client = client;
    }

    public void run() {
        ProxyCache.handle(client);
    }
}

class TunnelThread extends Thread {
    Socket server;
    Socket client;

    TunnelThread(Socket server, Socket client) {
        this.server = server;
        this.client = client;
    }

    public void run() {
        int rs = 0;
        byte[] buffer = new byte[2048];
        try {
            DataOutputStream toClient = new DataOutputStream(client.getOutputStream());
            DataInputStream fromServer = new DataInputStream(server.getInputStream());

            while (!server.isClosed() && !client.isClosed() && !server.isInputShutdown() && !client.isOutputShutdown()) {
                while ((rs = fromServer.read(buffer)) != -1) {
                    toClient.write(buffer, 0, rs);
                    toClient.flush();
                }
            }
            System.out.println("thread ended normally");
        } catch (IOException e) {
            System.out.println("Thread: IOError: " + e);
            interrupt();
            return;
        }
    }
}
