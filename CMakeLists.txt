cmake_minimum_required(VERSION 2.8)
find_package(Java REQUIRED)
include(UseJava)
project(SSProxyCache)

set(CMAKE_JAVA_COMPILE_FLAGS "-Xlint:deprecation")
set(EXECUTABLE_OUTPUT_PATH ..)

add_jar(SSProxyCache HttpRequest.java HttpResponse.java ProxyCache.java SCache.java SSHelpers/Util.java)

get_target_property(_jarFile SSProxyCache JAR_FILE)
get_target_property(_classDir SSProxyCache CLASSDIR)
