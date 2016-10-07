import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;


public class HttpResponse {
    final static String CRLF = "\r\n";
    /** How big is the buffer used for reading the object */
    final static int BUF_SIZE = 8192;
    /** Reply status and headers */
    String version; // HTTP version, part of statusLine
    int status; // HTTP status code, part of statusLine
    String statusMessage; // part of statusLine
    String statusLine = "";
    String proxy = "SSProxy"; // self advertising + it helps to identify requests that have passed through the proxy
    String headers = ""; // all headers, excluding the statusLine
    Date lastModified;
    Date expires;
    String etag;

    /* Body of reply */
    ByteArrayOutputStream body = new ByteArrayOutputStream();

    /** Read response from server. */
    public HttpResponse(DataInputStream fromServer) {
        /* Length of the object */
        int length = -1;
        boolean gotStatusLine = false;

        /* First read status line and response headers */
        try {

            String line = fromServer.readLine();
            // Parse HTTP headers
            while (line != null && line.length() != 0) { // line != null to medigate java.lang.NullPointerException
                if (!gotStatusLine) {
                    statusLine = line;
                    String[] tmp = line.split(" ", 3);
                    if (tmp.length == 3) {
                        this.version = tmp[0];
                        this.status = Integer.parseInt(tmp[1]);
                        this.statusMessage = tmp[2];
                    }
                    gotStatusLine = true;
                } else {
                    headers += line + CRLF;
                }

                /* Get length of content as indicated by
                 * Content-Length header. Unfortunately this is not
                 * present in every response. Some servers return the
                 * header "Content-Length", others return
                 * "Content-length". You need to check for both
                 * here. */
                if (line.toLowerCase(Locale.ROOT).startsWith("content-length")) {
                    String[] tmp = line.split(" ");
                    length = Integer.parseInt(tmp[1]);
                }
                else if (line.toLowerCase(Locale.ROOT).startsWith("last-modified")) {
                    String[] tmp = line.split(" ", 2);
                    lastModified = toDate(tmp[1]);
                } else if (line.toLowerCase(Locale.ROOT).startsWith("expires")) {
                    String[] tmp = line.split(" ", 2);
                    expires = toDate(tmp[1]);
                }  else if (line.toLowerCase(Locale.ROOT).startsWith("etag")) {
                    String[] tmp = line.split(" ", 2);
                    etag = tmp[1];
                }

                line = fromServer.readLine();
            }
        } catch (IOException e) {
            System.out.println("Error reading headers from server: " + e);
            return;
        }

        try {
            int bytesRead = 0;
            byte buf[] = new byte[BUF_SIZE];
            boolean loop = false;

            /* If we didn't get Content-Length header, just loop until
             * the connection is closed. */
            if (length == -1) {
                loop = true;
            }

            /* Read the body in chunks of BUF_SIZE and copy the chunk
             * into body. Usually replies come back in smaller chunks
             * than BUF_SIZE. The while-loop ends when either we have
             * read Content-Length bytes or when the connection is
             * closed (when there is no Connection-Length in the
             * response. */
            while (bytesRead < length || loop) {
                /* Read it in as binary data */
                int res = fromServer.read(buf);

                if (res == -1) {
                    break;
                }
                /* Copy the bytes into body. Make sure we don't exceed
                 * the maximum object size. */
                body.write(buf, 0, res);
                bytesRead += res;
            }
        } catch (IOException e) {
            // error = 500;
            System.out.println("Error reading response body: " + e);
            return;
        }
    }

    // constructor for HTTP status codes
    public HttpResponse(int statusCode) {
        version = "HTTP/1.1";
        status = statusCode;

        switch (status) {
            case 200:
                statusMessage = "OK";
                break;
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
        statusLine = version + " " + status + " " + statusMessage;
        String bodyStr = status + " " + statusMessage;
        body.write(bodyStr.getBytes(), 0, bodyStr.length());
    }

    public HttpResponse setStaus(int status) {
        this.status = status;
        return this;
    }

    public HttpResponse setMessage(String message) {
        this.statusMessage = message;
        return this;
    }

    public HttpResponse setVersion(String version) {
        this.version = version;
        return this;
    }

    public byte[] getBody() {
        return body.toByteArray();
    }

    /**
     * Convert response into a string for easy re-sending. Only
     * converts the response headers, body is not converted to a
     * string.
     */
    public String toString() {
        String res = "";
        res = statusLine + CRLF;
        res += headers;
        res += "X-Proxy-Agent: " + proxy + CRLF;
        res += CRLF;

        return res;
    }

    // send response to a host (generally the client)
    public Socket send(Socket host) {
        try {
            // Write response to host.
            DataOutputStream toClient = new DataOutputStream(host.getOutputStream());
            toClient.writeBytes(toString());
        } catch (IOException e) {
            System.out.println("Error writing response to client: " + e);
        }
        return host;
    }

    private Date toDate(String dateStr, String format) {
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat(format);
            return inFormat.parse(dateStr);
        } catch (ParseException e) {
            System.out.println("Parse date error: " + e + " -- at position:" + e.getErrorOffset());
        }
        return null;
    }

    private Date toDate(String dateStr) {
        String format = "E, dd MMM yyyy HH:mm:ss z"; // Mon, 13 Jun 2016 21:06:31 GMT
        return toDate(dateStr, format);
    }

    // check if response is still valid
    public boolean isValid() {
        Date now = new Date(); // get current date time
        if (lastModified != null && expires != null) {
        }

        // System.out.println(now.toString() + ", " + lastModified.toString() + ", " + expires.toString());
        // TODO: etag
        // TODO: last modified
        return true; // assume as valid for now.
    }
}
