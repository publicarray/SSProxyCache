import java.io.*;
import java.net.*;
import java.util.*;
import java.util.HashMap;
import java.text.SimpleDateFormat;

public class SCache {
    private Map<String, HttpResponse> cacheStore;
    HttpRequest request;

    public SCache() {
        // synchronised HasMap prevents data corruption when reading while writing the Map
        cacheStore = Collections.synchronizedMap(new HashMap<String, HttpResponse>());
        request = new HttpRequest("GET", "", "HTTP/1.1", "", "", 0);
    }

    // add value to HashMap
    public HttpResponse put(String key, HttpResponse value) {
        System.out.println("SCACHE: Saved " + key + " to cache.");
        return cacheStore.put(key, value);
    }

    // retrieve value from HashMap
    public HttpResponse get(String key) {
        System.out.println("SCACHE: looking for " + key + " in cache.");
        HttpResponse value = cacheStore.get(key);
        if (value == null) {
            System.out.println("SCACHE: " + key + " not found in cache.");
            return null;
        }
        System.out.println("SCACHE: retrieved " + key + " from cache.");

        boolean valid = isValid(key, value);
        System.out.println("SCACHE: Is fresh and valid? " + valid);
        if (!valid) { // get the new value if there is a new one
            value = cacheStore.get(key);
        }
        return value;
    }

    // check if response is still valid
    public boolean isValid(String requestURL, HttpResponse response) {
        if (!response.isExpired()) { // TODO: add command line flag
            System.out.println("SCACHE: response has not expired");
            return true;
        }

        // HttpRequest check = new HttpRequest(request); // deep copy
        request.setHost(getHost(requestURL));
        request.setPort(getPort(requestURL));
        request.URL = getLocation(requestURL);

        HttpResponse newResponse = request.askServerIfvalid(response.lastModified, response.etag); // ask severer
        if (newResponse == null) { // if error
            return true; // use the cached response if new response has an unrecoverable error.
        }
        // HttpResponse r = new HttpRequest(request, response.lastModified, response.etag).send(); // ask server
        if (newResponse.status == 304) { // Not modified, so still valid
            System.out.println("SCACHE: 304 Not modified");
            return true;
        } else { // 200 OK and other responses
            System.out.println("SCACHE: response was modified. Updating Cache.");
            put(requestURL, newResponse); // save the new response
            return false;
        }

        // return false;
    }

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
}
