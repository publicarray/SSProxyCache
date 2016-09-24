import java.io.*;
import java.net.*;
import java.util.*;

public class HttpRequest {
    /** Help variables */
    final static String CRLF = "\r\n";
    final static int HTTP_PORT = 80;
    /** Store the request parameters */
    String method;
    String URL;
    String version;
    String headers = "";
    /** Server and port */
    private String host;
    private int port;
    private int error;

    /** Create HttpRequest by reading it from the client socket */
    public HttpRequest(BufferedReader from) {
        error = 0;
        String firstLine = "";
        try {
            firstLine = from.readLine();
        } catch (IOException e) {
            error = 500;
            System.out.println("Error reading request line: " + e);
        }

        String[] tmp = firstLine.split(" ");
        method = tmp[0];
        URL = tmp[1];
        version = tmp[2];

        System.out.println("URL is: " + URL);

        if (!method.equals("GET")) {
            error = 501;
            System.out.println("Error: Method not GET");
        }
        try {
            String line = from.readLine();
            while (line.length() != 0) {
                headers += line + CRLF;
                /* We need to find host header to know which server to
                 * contact in case the request URL is not complete. */
                if (line.startsWith("Host:")) {
                    tmp = line.split(" ");
                    if (tmp[1].indexOf(':') > 0) {
                        String[] tmp2 = tmp[1].split(":");
                        host = tmp2[0];
                        port = Integer.parseInt(tmp2[1]);
                    } else {
                        host = tmp[1];
                        port = HTTP_PORT;
                    }
                }
                line = from.readLine();
            }
        } catch (IOException e) {
            error = 500;
            System.out.println("Error reading from socket: " + e);
            return;
        }
        System.out.println("Host to contact is: " + host + " at port " + port);
    }

    public int getError() {
        return error;
    }

    /** Return host for which this request is intended */
    public String getHost() {
        return host;
    }

    /** Return port for server */
    public int getPort() {
        return port;
    }

    /** Return URL to connect to */
    public String getURL() {
        return URL;
    }

    // Return the method (GET, PUT, POST, etc.)
    public String getMethod() {
        return method;
    }

    /**
     * Convert request into a string for easy re-sending.
     */
    public String toString() {
        String req = "";

        req = method + " " + URL + " " + version + CRLF;
        req += headers;
        /* This proxy does not support persistent connections */
        req += "Connection: close" + CRLF;
        req += CRLF;

        return req;
    }
}
