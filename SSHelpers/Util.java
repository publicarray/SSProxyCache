package SSHelpers;

import java.util.Date;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;
/**
 * A utility class containing small helper methods.
 * These methods can be used my importing the class: <code>import static SSHelpers.Util.*;</code>
 *
 * @author      Sebastian Schmidt
 * @version     1.0 2016-10-18
 * @since       1.0
 */
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

    /**
     * Converts a string to a Java date object given the correct format.
     * @param  dateStr The string that is parsed as a date.
     * @param  format  The format as defined in the SimpleDateFormat <a href="https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html">https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html</a>.
     * @return         Returns a new date object. On failure it will return null.
     */
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
     /**
     * Converts a string to a Java date object using "E, dd MMM yyyy HH:mm:ss z" (Mon, 13 Jun 2016 21:06:31 GMT) as the format.
     * @param  dateStr The string that is parsed as a date.
     * @return         Returns a new date object. On failure it will return null.
     */
    public static Date toDate(String dateStr) {
        String format = "E, dd MMM yyyy HH:mm:ss z";
        return Util.toDate(dateStr, format);
    }

    /**
     * Converts a Java string object to a string with the given format.
     * @param  date   The date to format.
     * @param  format The format as defined in the SimpleDateFormat <a href="https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html">https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html</a>.
     * @return        The date as a string.
     */
    public static String dateToString(Date date, String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat (format);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(date);
    }

    /**
     * Converts a Java string object to a string using "E, dd MMM yyyy HH:mm:ss z" (Mon, 13 Jun 2016 21:06:31 GMT) as the format.
     * @param  date   The date to format.
     * @return        The date as a string.
     */
    public static String dateToString(Date date) {
        return Util.dateToString(date, "E, dd MMM yyyy HH:mm:ss z");
    }
}
