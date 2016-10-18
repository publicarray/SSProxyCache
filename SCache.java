import java.io.*;
import java.net.*;
import java.util.*;
import java.util.HashMap;
import java.text.SimpleDateFormat;
import static SSHelpers.Util.*;

public class SCache {
    // private Helpers helpers = new Helpers();

    // A key value store in memory
    private Map<String, HttpResponse> cacheStore;
    // A HttpRequest used to send If-None-Match and If-Modified-Since requests to the server
    HttpRequest request;

    /**
     * This SCache class contains the cache for the proxy.
     * It provides methods to add, validate and get items from the cache.
     * The cache uses the URL as a key to lookup items in the cache
     */
    public SCache() {
        // synchronised HasMap prevents data corruption when reading while writing the Map
        cacheStore = Collections.synchronizedMap(new HashMap<String, HttpResponse>());
        request = new HttpRequest("GET", "", "HTTP/1.1", "", "", 0);
    }

    /**
     * Adds the HttpResponse into the cache with the url as the key.
     * @param  key   The Url
     * @param  value The HttpResponse
     * @return       The given value HttpResponse
     */
    public HttpResponse put(String key, HttpResponse value) {
        System.out.println("SCACHE: Saved " + key + " to cache.");
        return cacheStore.put(key, value);
    }

    /**
     * Retrieves the HttpResponse from cache by looking up the url.
     * The HttpResponse is checked to make sure it is fresh.
     * @param  key The Url
     * @return     The Cached HttpResponse
     */
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

    /**
     * Determines if the cached HttpResonse is still fresh and updates the cache when it is not.
     * It checks that the response has not expired by checking the Last-Modified and the Expired header
     * in the response. If no expired header is present the server is asked with If-None-Match and the
     * If-Modified-Since headers. If the server response status code is not a 304 Not Modified response
     * than the new response updates the value in the cache.
     * @param  requestURL The key in the cache
     * @param  response   The value in the cache
     * @return            isValid? as in is the item in the cache fresh?
     */
    public boolean isValid(String requestURL, HttpResponse response) {
        if (!response.isExpired()) { // TODO: add command line flag
            System.out.println("SCACHE: response has not expired");
            return true;
        }

        request.setHost(getHost(requestURL));
        request.setPort(getPort(requestURL));
        request.URL = getLocation(requestURL);

        HttpResponse newResponse = request.askServerIfvalid(response.lastModified, response.etag); // ask severer
        if (newResponse == null) { // if error
            return true; // use the cached response if new response has an unrecoverable error.
        }
        if (newResponse.status == 304) { // Not modified, so still valid
            System.out.println("SCACHE: 304 Not modified");
            return true;
        } else { // 200 OK and other responses
            System.out.println("SCACHE: response was modified. Updating Cache.");
            put(requestURL, newResponse); // save the new response
            return false;
        }
    }
}
