import java.io.*;
import java.net.*;
import java.util.*;

public class SHttpErrorResponse {
    final static String CRLF = "\r\n";

// HTTP/1.1 301 Moved Permanently
// Server: Apache
// Location: http://www.macrumors.com/404errortest/
// Content-Type: text/html; charset=iso-8859-1
// Content-Length: 246
// Accept-Ranges: bytes
// Date: Sat, 24 Sep 2016 21:12:24 GMT
// Via: SSProxy
// X-Cache: A-MISS

    /** Reply status and headers */
    String version;
    int status;
    String statusMessage;
    String server = "SSProxy";
    // String headers;

    /** Craft a response from the HTTP Status Code. */
    public SHttpErrorResponse(int statusCode) {
        this.version = "HTTP/1.1";
        this.status = statusCode;
        this.server = "SSProxy";

        switch (status) {
            case 400: // when headers are not properly formatted
                statusMessage = "Bad request";
                break;
            case 404:
                statusMessage = "Not found";
                break;
            case 405:
                statusMessage = "Method not allowed";
                break;
            case 500:
                statusMessage = "Internal server error";
                break;
            case 501: // for methods that are not implemented
                statusMessage = "Not implemented";
                break;
            case 505: // for http/2
                statusMessage = "HTTP version not supported";
                break;
            case 520:
                statusMessage = "Unknown Error";
                // used by Microsoft and CloudFlare as a "catch-all" response
                // for when the origin server returns something unexpected
                // or something that is not tolerated/interpreted.
                break;
            default:
                System.out.println("Invalid HTTP status code");
                status = 500;
                statusMessage = "Internal server error";
        }
    }

    /**
     * Convert response into a string for easy re-sending.
     */
    public String toString() {
        String res = "";
        res = version + " " + status + " " + statusMessage + CRLF;
        res += "Via: " + server + CRLF;
        // res += headers;
        res += "Content-Type: text/html;" + CRLF;
        res += "Content-Length: 10" + CRLF;
        res += "Connection: close" + CRLF;
        res += CRLF;
        res += "\n<html>\nError: " + status + "\n</html>";

        return res;
    }

    public void send(Socket host) {
        try {
            // Write response to host.
            DataOutputStream toClient = new DataOutputStream(host.getOutputStream());
            toClient.writeBytes(toString());
            host.close();
        } catch (IOException e) {
            System.out.println("Error writing response to client: " + e);
        }
    }
}
