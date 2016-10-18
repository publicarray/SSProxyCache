package SSHelpers;

import java.util.Date;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

public class Util {
    /**
     * From a url string get the host-name
     * @param  url The Url E.g. http://example.com/foo
     * @return     The Hostname E.g. example.com
     */
    public static String getHost(String url) {
        if (url == null || url.length() <= 0) {
            return "";
        }

        int start = url.indexOf("//");
        if (start == -1) {
            return "";
        }
        start += 2;

        int end = url.indexOf('/', start);
        if (end == -1) {
            return "";
        }

        return url.substring(start, end);
    }

    /**
     * From a url string get the port number
     * @param  url The Url E.g. http://example.com:50/foo
     * @return     The Port number E.g. 50
     */
    public static int getPort(String url) {
        if (url == null || url.length() <= 0) {
            return 80;
        }

        int start = url.indexOf("//");
        if (start == -1) {
            return 80;
        }

        start += 2;

        int end = url.indexOf('/', start);
        if (end == -1) {
            return 80;
        }

        int port = url.indexOf(':', start);
        if (port == -1) {
            return 80;
        }

        if (port < end) {
            end = port;
        }

        return Integer.parseInt(url.substring(port, end));
    }

    /**
     * From a url string get the location
     * @param  url The Url  E.g. http://example.com/foo
     * @return     The location E.g. /foo
     */
    public static String getLocation(String url) {
        if (url == null || url.length() <= 0) {
            return "";
        }

        int start = url.indexOf("//");
        if (start == -1) {
            return "";
        }
        start += 2;

        int end = url.indexOf('/', start);
        if (end == -1) {
            return "";
        }

        return url.substring(end);
    }

    public static Date toDate(String dateStr, String format) {
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat(format);
            inFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return inFormat.parse(dateStr);
        } catch (ParseException e) {
            System.out.println("Parse date error: " + e + " -- at position:" + e.getErrorOffset());
        }
        return null;
    }

    public static Date toDate(String dateStr) {
        String format = "E, dd MMM yyyy HH:mm:ss z"; // Mon, 13 Jun 2016 21:06:31 GMT
        return Util.toDate(dateStr, format);
    }

    public static String dateToString(Date date, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat (format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(date);
    }

    public static String dateToString(Date date) {
        return Util.dateToString(date, "E, dd MMM yyyy HH:mm:ss z");
    }
}
