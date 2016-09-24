import java.io.*;
import java.net.*;
import java.util.*;
import java.util.HashMap;

public class Cache {
    private Map<String, HttpResponse> cacheStore;

    public Cache() {
        // synchronised HasMap prevents data corruption when reading while writing the Map
        cacheStore = Collections.synchronizedMap(new HashMap<String, HttpResponse>());
    }

    // add value to HashMap
    public HttpResponse put(String key, HttpResponse value) {
        System.out.println("Saved " + key + " to cache.");
        return cacheStore.put(key, value);
    }

    // retrieve value from HashMap
    public HttpResponse get(String key) {
        System.out.println("looking for " + key + " in cache.");
        return cacheStore.get(key);
    }
}
