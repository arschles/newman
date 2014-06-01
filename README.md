#Newman

[![Build Status](https://travis-ci.org/arschles/newman.png?branch=master)](https://travis-ci.org/stackmob/newman)

Newman is StackMob's HTTP client. We named it after the [Seinfeld Character](http://en.wikipedia.org/wiki/Newman_(Seinfeld)).

And wrote a post explaining our motivation for building this library [here](https://blog.stackmob.com/2013/03/newman/).

Newman supports the following basic features:

* Making HTTP requests and receiving responses
* Serializing and deserializing request and response bodies
* Serializing and deserializing requests and responses for replay or caching
* In memory response caching with TTL expiry
* ETag HTTP caching

To add it to your project, use this for Maven:

```xml
<dependency>
  <groupId>com.stackmob</groupId>
  <artifactId>newman_${scala.version}</artifactId>
  <version>1.3.5</version>
</dependency>
```

or the equivalent for sbt:

```scala
libraryDependencies += "com.stackmob" %% "newman" % "1.3.5"
```

# Basic Usage
	
```scala
import com.stackmob.newman._
import com.stackmob.newman.dsl._
import scala.concurrent._
import scala.concurrent.duration._
import java.net.URL

implicit val httpClient = new ApacheHttpClient
//execute a GET request
val url = new URL("http://google.com")
val response = Await.result(GET(url).apply, 1.second) //this will throw if the response doesn't return within 1 second
println(s"Response returned from ${url.toString} with code ${response.code}, body ${response.bodyString}")
```

#The DSL
Newman comes with a DSL which is inspired by [Dispatch](http://dispatch.databinder.net/Dispatch.html), 
but uses mostly english instead of symbols.
This DSL is the recommended way to build requests, and the above example in "Basic Usage" uses the DSL to 
construct a GET request.

To start using the DSL, simply `import com.stackmob.newman.dsl._`. 
The functions of interest in the DSL are uppercase representations of the HTTP verbs: 

* `def GET(url: URL)(implicit client: HttpClient)`
* `def POST(url: URL)(implicit client: HttpClient)`
* `def PUT(url: URL)(implicit client: HttpClient)`
* `def DELETE(url: URL)(implicit client: HttpClient)`
* `def HEAD(url: URL)(implicit client: HttpClient)`

Notice that each method takes an implicit `HttpClient`, so you must declare your own implicit before 
you use any of the above listed DSL methods, or pass one explicitly.

Each method listed above returns a Builder, which works in concert with the implicit methods defined 
in the `DSL` package to let you build up a request and then execute it.

# Executing Requests
The most important method on `com.stackmob.newman.HttpRequest` is `def apply: Future[HttpResponse]`. A few notes on this method:

* It returns immediately after the request is *started*
* It returns a [`scala.concurrent.Future`](http://www.scala-lang.org/api/current/index.html#scala.concurrent.Future) that will be complete immediately after the executing request is complete
* If you want to schedule some action to happen after the response is available, use `onComplete` or a similar callback
* The `Future` can also fail with an exception, which you can react to with `onFailure`
* If you need to block your code from proceeding until the `HttpResponse` is available, use `Await.result`.
* We recommend refactoring your blocking code to remove `Await.result` calls it possible, since latencies are unpredictable, subject to network conditions, etc...

# Serializing
Newman comes with built in support for serializing `HttpRequest`s and `HttpResponse`s to Json.

To serialize either, simply call the `toJson(prettyPrint: Boolean = false): String` method on the `HttpRequest` or `HttpResponse`. And to deserialize, call `HttpRequest.fromJson(json: String): Result[HttpRequest]` or `HttpResponse.fromJson(json: String): Result[HttpResponse]` to deserialize the `HttpRequest` or `HttpResponse`, respectively.

# ETag Support
Newman comes with an implementation of `HttpClient` called `ETagAwareHttpClient`. This implementation requires an underlying "raw" `HttpClient` to execute requests to a server, but it also requires an implementation of `HttpResponseCacher`.

It uses this `HttpResponseCacher` to check the cache for a response corresponding to a given request. If it finds one and that response has an `ETag` header in it, the `ETagAwareHttpClient` automatically sends an `If-None-Match` header to the server containing that `ETag`. In this case, if the server responds with a `304 NOT MODIFIED` response code, then `ETagAwareHttpClient` will return the cached version. In all other cases, `ETagAwareHttpClient` will cache and return the new response.

## Usage With the DSL
Using `ETagAwareHttpClient` is very similar to the basic usage above. Following demonstrates how to use the client with a (built-in) in-memory cache implementation.

```scala
import com.stackmob.newman.{ETagAwareHttpClient, ApacheHttpClient}
import com.stackmob.newman.caching.InMemoryHttpResponseCacher
import com.stackmob.newman.dsl._
import java.net.URL
	
//change this implementation to your own if you want to use Memcached, Redis, etc
val cache = new InMemoryHttpResponseCacher
val rawHttpClient = new ApacheHttpClient
//eTagClient will be used in the DSL to construct & execute requests below
implicit val eTagClient = new ETagAwareHttpClient(rawHttpClient, cache)
	
val url = new URL("http://stackmob.com")
//since the cacher is empty, this will issue a request to stackmob.com without an If-None-Match header
val res1 = GET(url).apply
//assuming res1 contained an ETag and stackmob.com fully supports ETag headers,
//stackmob.com will return a 304 response code in this request and res2 will come from the cache
val res2 = GET(url).apply
```
