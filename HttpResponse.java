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
    int error = 0;
    String version;
    int status;
    String statusLine = "";
    String headers = "";
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
                // if (line.startsWith("Content-Length") ||
                    // line.startsWith("Content-length")) {
                if (line.toLowerCase(Locale.ROOT).startsWith("content-length")) {
                    String[] tmp = line.split(" ");
                    length = Integer.parseInt(tmp[1]);
                }
                else if (line.toLowerCase(Locale.ROOT).startsWith("last-modified")) {
                    String[] tmp = line.split(" ", 2);
                    lastModified = toDate(tmp[1]);
                    // System.out.println("parsed date: " + lastModified.toString());
                } else if (line.toLowerCase(Locale.ROOT).startsWith("expires")) {
                    String[] tmp = line.split(" ", 2);
                    expires = toDate(tmp[1]);
                    // System.out.println("parsed date: " + expires.toString());
                }  else if (line.toLowerCase(Locale.ROOT).startsWith("etag")) {
                    String[] tmp = line.split(" ", 2);
                    etag = tmp[1];
                    System.out.println("etag: " + etag);
                }

                line = fromServer.readLine();
            }
        } catch (IOException e) {
            // error = 500;
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

    /**
     * Convert response into a string for easy re-sending. Only
     * converts the response headers, body is not converted to a
     * string.
     */
    public String toString() {
        String res = "";

        res = statusLine + CRLF;
        res += headers;
        res += CRLF;

        return res;
    }

    public byte[] getBody() {
        return body.toByteArray();
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
        if (lastModified != null || expires != null) {
            return now.after(lastModified) && now.before(expires); // no web request is needed
        }

        // System.out.println(now.toString() + ", " + lastModified.toString() + ", " + expires.toString());
        //etag
        //last modified
        return true; // for now
    }
}
