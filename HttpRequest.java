import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import static SSHelpers.Util.*;

/**
 * This class contains the needed information (host, port number and all Http headers) for a typical Http Request.
 *
 * @author      Modified by: Sebastian Schmidt
 * @version     1.0 2016-10-18
 * @since       1.0
 */
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

    /**
     * Creates a HttpRequest by reading it from the client socket.
     * The constructor attempts to parse the content form the client.
     * If there is a error in parsing the client will receive a
     * 400 (Bad request) error indicating that the request was malformed.
     * @param  from The content stream from a socket connection.
     */
    public HttpRequest(BufferedReader from) {
        error = 0;
        String firstLine = "";
        try {
            firstLine = from.readLine();
        } catch (IOException e) {
            error = 400;
            System.out.println("Error reading request line: " + e);
        }

        String[] tmp = firstLine.split(" ");
        method = tmp[0];
        URL = tmp[1];
        version = tmp[2];

        System.out.println("URL is: " + URL);

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

            // TODO: get POST parameters

        } catch (Exception e) {
            error = 400;
            System.out.println("Error reading from client socket: " + e);
            return;
        }
        System.out.println("Host to contact is: " + host + " at port " + port);
    }

    /**
     * Constructs a HttpRequest given the required variables
     * @param  method  The http method E.g GET or POST
     * @param  url     The location of the resource E.g. /example.html
     * @param  version The Http protocol version, usually it is HTTP/1.1
     * @param  headers Any optional http headers. If you don't want to add any use an empty string ("").
     * @param  host    The hostname of the server where the request is send to E.g. example.com
     * @param  port    The port number used for the webs server E.g. 80
     */
    public HttpRequest(String method, String url, String version, String headers, String host, int port) {
        this.method = method;
        this.URL = url;
        this.version = version;
        this.headers = headers;
        this.host = host;
        this.port = port;
        this.error = 0;
    }

    /**
     * Copy constructor
     * <p>
     * Returns a deep copy of the 'other' HttpRequest.
     * @param  other An existing HttpRequest to create the copy from.
     */
    public HttpRequest(HttpRequest other) {
        this.method = other.method;
        this.URL = other.URL;
        this.version = other.version;
        this.headers = other.headers;
        this.host = other.host;
        this.port = other.port;
        this.error = other.error;
    }

    public HttpResponse askServerIfvalid(Date lastModified, String etag) {
        if (lastModified == null || etag == null || etag.isEmpty()) {
            return null;
        }

        // User-Agent: SSProxyCache/1 (https://github.com/publicarray/SSProxyCache)
        // headers = "User-Agent: SSProxyCache/1.0\r\n";
        headers = "";

        if (!etag.isEmpty()) {
            // If-None-Match: "07bb743de42c7918a1a47fd61c6aa9c7:1469533683"\r\n
            headers += "If-None-Match: " + etag + CRLF;
        } if (lastModified != null) {
            // If-Modified-Since: Thu, 13 Oct 2016 00:36:43 GMT\r\n
            headers += "If-Modified-Since: " + dateToString(lastModified) + CRLF;
        }

        headers += "Host: " + host + CRLF;
        return send();
    }

    public int getError() {
        return error;
    }

    /** Return host for which this request is intended */
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /** Return port for server */
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    HttpResponse send() {
        // send message/request to server
        Socket server;
        try {
            server = new Socket(getHost(), getPort());
            DataOutputStream toServer = new DataOutputStream(server.getOutputStream());
            toServer.writeBytes(toString());
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + getHost());
            return new HttpResponse(404);
        } catch (IOException e) {
            System.out.println("HttpRequest.java - Error writing request to server: " + e);
            return new HttpResponse(500);
        }

        // handle response from server
        try {
            DataInputStream fromServer = new DataInputStream(server.getInputStream());
            HttpResponse response = new HttpResponse(fromServer);
            server.close();
            return response;
        } catch (IOException e) {
            System.out.println("HttpRequest.java - Error reading response from server: " + e);
            return new HttpResponse(520);
        }
    }
}
