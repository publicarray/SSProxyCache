# SSProxyCache

## Compile

### With make

Recommended for Linux and macOS

```bash
make
```

### With javac

Recommended for Windows

```bash
javac -Xlint:deprecation ProxyCache.java HttpRequest.java HttpResponse.java SCache.java SSHelpers/Util.java
```

## Running the program

```bash
java ProxyCache <port-number> [args]
# Example:
java ProxyCache 5001
```

Next change your browsers or system proxy settings and start browsing the web.

### Commandline flags

* -v --verbose
    * Prints verbose output
* -a, --secure
    * Proxy HTTPS/TLS connections. (experimental as it can course 100% CPU usage)
* -e --expires
    * Check expires header when checking the freshness of an object in the cache.

## Generating JavaDocs

### With make

Recommended for Linux and macOS

```bash
make docs
```

### With javadoc

Recommended for Windows

```bash
javadoc SCache.java HttpRequest.java HttpResponse.java ProxyCache.java -d docs -doctitle 'SS Proxy Cache API Specification' -windowtitle 'SS Proxy Cache API Specification' -subpackages SSHelpers -version -author
```
