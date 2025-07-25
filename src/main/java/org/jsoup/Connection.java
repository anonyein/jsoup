package org.jsoup;

import org.jsoup.helper.RequestAuthenticator;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.parser.StreamParser;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.Authenticator;
import java.net.CookieStore;
import java.net.Proxy;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 The Connection interface is a convenient HTTP client and session object to fetch content from the web, and parse them
 into Documents.
 <p>To start a new session, use either {@link org.jsoup.Jsoup#newSession()} or {@link org.jsoup.Jsoup#connect(String)}.
 Connections contain {@link Connection.Request} and {@link Connection.Response} objects (once executed). Configuration
 settings (URL, timeout, useragent, etc) set on a session will be applied by default to each subsequent request.</p>
 <p>To start a new request from the session, use {@link #newRequest()}.</p>
 <p>Cookies are stored in memory for the duration of the session. For that reason, do not use one single session for all
 requests in a long-lived application, or you are likely to run out of memory, unless care is taken to clean up the
 cookie store. The cookie store for the session is available via {@link #cookieStore()}. You may provide your own
 implementation via {@link #cookieStore(java.net.CookieStore)} before making requests.</p>
 <p>Request configuration can be made using either the shortcut methods in Connection (e.g. {@link #userAgent(String)}),
 or by methods in the {@link Connection.Request} object directly. All request configuration must be made before the request is
 executed. When used as an ongoing session, initialize all defaults prior to making multi-threaded {@link
#newRequest()}s.</p>
 <p>Note that the term "Connection" used here does not mean that a long-lived connection is held against a server for
 the lifetime of the Connection object. A socket connection is only made at the point of request execution ({@link
#execute()}, {@link #get()}, or {@link #post()}), and the server's response consumed.</p>
 <p>For multi-threaded implementations, it is important to use a {@link #newRequest()} for each request. The session may
 be shared across concurrent threads, but a not a specific request.</p>
 <p><b>HTTP/2</b> support: On JVM 11 and above, requests use {@link java.net.http.HttpClient}, which supports
 HTTP/2. To use the legacy {@link java.net.HttpURLConnection} instead, set
 <code>System.setProperty("jsoup.useHttpClient", "false")</code>.</p>
 */
@SuppressWarnings("unused")
public interface Connection {

    /**
     * GET and POST http methods.
     */
    enum Method {
        GET(false),
        POST(true),
        PUT(true),
        DELETE(true),
        /**
         Note that unfortunately, PATCH is not supported in many JDKs.
         */
        PATCH(true),
        HEAD(false),
        OPTIONS(false),
        TRACE(false);

        private final boolean hasBody;

        Method(boolean hasBody) {
            this.hasBody = hasBody;
        }

        /**
         * Check if this HTTP method has/needs a request body
         * @return if body needed
         */
        public final boolean hasBody() {
            return hasBody;
        }
    }

    /**
     Creates a new request, using this Connection as the session-state and to initialize the connection settings (which
     may then be independently changed on the returned {@link Connection.Request} object).
     @return a new Connection object, with a shared Cookie Store and initialized settings from this Connection and Request
     @since 1.14.1
     */
    Connection newRequest();

    /**
     Creates a new request, using this Connection as the session-state and to initialize the connection settings (which
     may then be independently changed on the returned {@link Connection.Request} object).
     @return a new Connection object, with a shared Cookie Store and initialized settings from this Connection and Request
     @param url URL for the new request
     @since 1.17.1
     */
    default Connection newRequest(String url) {
        return newRequest().url(url);
    }

    /**
     Creates a new request, using this Connection as the session-state and to initialize the connection settings (which
     may then be independently changed on the returned {@link Connection.Request} object).
     @return a new Connection object, with a shared Cookie Store and initialized settings from this Connection and Request
     @param url URL for the new request
     @since 1.17.1
     */
    default Connection newRequest(URL url) {
        return newRequest().url(url);
    }

    /**
     * Set the request URL to fetch. The protocol must be HTTP or HTTPS.
     * @param url URL to connect to
     * @return this Connection, for chaining
     */
    Connection url(URL url);

    /**
     * Set the request URL to fetch. The protocol must be HTTP or HTTPS.
     * @param url URL to connect to
     * @return this Connection, for chaining
     */
    Connection url(String url);

    /**
     * Set the proxy to use for this request. Set to <code>null</code> to disable a previously set proxy.
     * @param proxy proxy to use
     * @return this Connection, for chaining
     */
    Connection proxy(@Nullable Proxy proxy);

    /**
     * Set the HTTP proxy to use for this request.
     * @param host the proxy hostname
     * @param port the proxy port
     * @return this Connection, for chaining
     */
    Connection proxy(String host, int port);

    /**
     * Set the request user-agent header.
     * @param userAgent user-agent to use
     * @return this Connection, for chaining
     * @see org.jsoup.helper.HttpConnection#DEFAULT_UA
     */
    Connection userAgent(String userAgent);

    /**
     Set the total maximum request duration. If a timeout occurs, an {@link java.net.SocketTimeoutException} will be
     thrown.
     <p>The default timeout is <b>30 seconds</b> (30,000 millis). A timeout of zero is treated as an infinite timeout.</p>
     <p>This timeout specifies the combined maximum duration of the connection time and the time to read
     the full response.</p>
     <p>Implementation note: when this <code>Connection</code> is backed by <code>HttpURLConnection</code> (rather than <code>HttpClient</code>, as used in JVM 11+), this timeout is implemented by setting both the socket connect and read timeouts to half of the specified value.</p>

     @param millis number of milliseconds (thousandths of a second) before timing out connects or reads.
     @return this Connection, for chaining
     @see #maxBodySize(int)
     */
    Connection timeout(int millis);

    /**
     * Set the maximum bytes to read from the (uncompressed) connection into the body, before the connection is closed,
     * and the input truncated (i.e. the body content will be trimmed). <b>The default maximum is 2MB</b>. A max size of
     * <code>0</code> is treated as an infinite amount (bounded only by your patience and the memory available on your
     * machine).
     *
     * @param bytes number of bytes to read from the input before truncating
     * @return this Connection, for chaining
     */
    Connection maxBodySize(int bytes);

    /**
     * Set the request referrer (aka "referer") header.
     * @param referrer referrer to use
     * @return this Connection, for chaining
     */
    Connection referrer(String referrer);

    /**
     * Configures the connection to (not) follow server redirects. By default, this is <b>true</b>.
     * @param followRedirects true if server redirects should be followed.
     * @return this Connection, for chaining
     */
    Connection followRedirects(boolean followRedirects);

    /**
     * Set the request method to use, GET or POST. Default is GET.
     * @param method HTTP request method
     * @return this Connection, for chaining
     */
    Connection method(Method method);

    /**
     * Configures the connection to not throw exceptions when an HTTP error occurs. (4xx - 5xx, e.g. 404 or 500). By
     * default, this is <b>false</b>; an IOException is thrown if an error is encountered. If set to <b>true</b>, the
     * response is populated with the error body, and the status message will reflect the error.
     * @param ignoreHttpErrors - false (default) if HTTP errors should be ignored.
     * @return this Connection, for chaining
     */
    Connection ignoreHttpErrors(boolean ignoreHttpErrors);

    /**
     * Ignore the document's Content-Type when parsing the response. By default, this is <b>false</b>, an unrecognised
     * content-type will cause an IOException to be thrown. (This is to prevent producing garbage by attempting to parse
     * a JPEG binary image, for example.) Set to true to force a parse attempt regardless of content type.
     * @param ignoreContentType set to true if you would like the content type ignored on parsing the response into a
     * Document.
     * @return this Connection, for chaining
     */
    Connection ignoreContentType(boolean ignoreContentType);

    /**
     Set a custom SSL socket factory for HTTPS connections.
     <p>Note: if set, the legacy <code>HttpURLConnection</code> will be used instead of the JVM's
     <code>HttpClient</code>.</p>

     @param sslSocketFactory SSL socket factory
     @return this Connection, for chaining
     @see #sslContext(SSLContext)
     @deprecated use {@link #sslContext(SSLContext)} instead.
     */
    @Deprecated
    Connection sslSocketFactory(SSLSocketFactory sslSocketFactory);

    /**
     Set a custom SSL context for HTTPS connections.
     <p>Note: when using the legacy <code>HttpURLConnection</code>, only the <code>SSLSocketFactory</code> from the
     context will be used.</p>

     @param sslContext SSL context
     @return this Connection, for chaining
     @since 1.21.2
     */
    default Connection sslContext(SSLContext sslContext) {
        throw new UnsupportedOperationException();
    }

    /**
     * Add a request data parameter. Request parameters are sent in the request query string for GETs, and in the
     * request body for POSTs. A request may have multiple values of the same name.
     * @param key data key
     * @param value data value
     * @return this Connection, for chaining
     */
    Connection data(String key, String value);

    /**
     * Add an input stream as a request data parameter. For GETs, has no effect, but for POSTS this will upload the
     * input stream.
     * <p>Use the {@link #data(String, String, InputStream, String)} method to set the uploaded file's mimetype.</p>
     * @param key data key (form item name)
     * @param filename the name of the file to present to the remove server. Typically just the name, not path,
     * component.
     * @param inputStream the input stream to upload, that you probably obtained from a {@link java.io.FileInputStream}.
     * You must close the InputStream in a {@code finally} block.
     * @return this Connection, for chaining
     * @see #data(String, String, InputStream, String)
     */
    Connection data(String key, String filename, InputStream inputStream);

    /**
     * Add an input stream as a request data parameter. For GETs, has no effect, but for POSTS this will upload the
     * input stream.
     * @param key data key (form item name)
     * @param filename the name of the file to present to the remove server. Typically just the name, not path,
     * component.
     * @param inputStream the input stream to upload, that you probably obtained from a {@link java.io.FileInputStream}.
     * @param contentType the Content Type (aka mimetype) to specify for this file.
     * You must close the InputStream in a {@code finally} block.
     * @return this Connection, for chaining
     */
    Connection data(String key, String filename, InputStream inputStream, String contentType);

    /**
     * Adds all of the supplied data to the request data parameters
     * @param data collection of data parameters
     * @return this Connection, for chaining
     */
    Connection data(Collection<KeyVal> data);

    /**
     * Adds all of the supplied data to the request data parameters
     * @param data map of data parameters
     * @return this Connection, for chaining
     */
    Connection data(Map<String, String> data);

    /**
     Add one or more request {@code key, val} data parameter pairs.
     <p>Multiple parameters may be set at once, e.g.:
     <code>.data("name", "jsoup", "language", "Java", "language", "English");</code> creates a query string like:
     <code>{@literal ?name=jsoup&language=Java&language=English}</code></p>
     <p>For GET requests, data parameters will be sent on the request query string. For POST (and other methods that
     contain a body), they will be sent as body form parameters, unless the body is explicitly set by
     {@link #requestBody(String)}, in which case they will be query string parameters.</p>

     @param keyvals a set of key value pairs.
     @return this Connection, for chaining
     */
    Connection data(String... keyvals);

    /**
     * Get the data KeyVal for this key, if any
     * @param key the data key
     * @return null if not set
     */
    @Nullable KeyVal data(String key);

    /**
     * Set a POST (or PUT) request body. Useful when a server expects a plain request body (such as JSON), and not a set
     * of URL encoded form key/value pairs. E.g.:
     * <code><pre>Jsoup.connect(url)
     * .requestBody(json)
     * .header("Content-Type", "application/json")
     * .post();</pre></code>
     * If any data key/vals are supplied, they will be sent as URL query params.
     * @see #requestBodyStream(InputStream)
     * @return this Request, for chaining
     */
    Connection requestBody(String body);

    /**
     Set the request body. Useful for posting data such as byte arrays or files, and the server expects a single request
     body (and not a multipart upload). E.g.:
     <code><pre> Jsoup.connect(url)
     .requestBody(new ByteArrayInputStream(bytes))
     .header("Content-Type", "application/octet-stream")
     .post();
     </pre></code>
     <p>Or, use a FileInputStream to data from disk.</p>
     <p>You should close the stream in a finally block.</p>

     @param stream the input stream to send.
     @return this Request, for chaining
     @see #requestBody(String)
     @since 1.20.1
     */
    default Connection requestBodyStream(InputStream stream) {
        throw new UnsupportedOperationException();
    }

    /**
     * Set a request header. Replaces any existing header with the same case-insensitive name.
     * @param name header name
     * @param value header value
     * @return this Connection, for chaining
     * @see org.jsoup.Connection.Request#header(String, String)
     * @see org.jsoup.Connection.Request#headers()
     */
    Connection header(String name, String value);

    /**
     * Sets each of the supplied headers on the request. Existing headers with the same case-insensitive name will be
     * replaced with the new value.
     * @param headers map of headers name {@literal ->} value pairs
     * @return this Connection, for chaining
     * @see org.jsoup.Connection.Request#headers()
     */
    Connection headers(Map<String,String> headers);

    /**
     * Set a cookie to be sent in the request.
     * @param name name of cookie
     * @param value value of cookie
     * @return this Connection, for chaining
     */
    Connection cookie(String name, String value);

    /**
     * Adds each of the supplied cookies to the request.
     * @param cookies map of cookie name {@literal ->} value pairs
     * @return this Connection, for chaining
     */
    Connection cookies(Map<String, String> cookies);

    /**
     Provide a custom or pre-filled CookieStore to be used on requests made by this Connection.
     @param cookieStore a cookie store to use for subsequent requests
     @return this Connection, for chaining
     @since 1.14.1
     */
    Connection cookieStore(CookieStore cookieStore);

    /**
     Get the cookie store used by this Connection.
     @return the cookie store
     @since 1.14.1
     */
    CookieStore cookieStore();

    /**
     * Provide a specific parser to use when parsing the response to a Document. If not set, jsoup defaults to the
     * {@link Parser#htmlParser() HTML parser}, unless the response content-type is XML, in which case the
     * {@link Parser#xmlParser() XML parser} is used.
     * @param parser alternate parser
     * @return this Connection, for chaining
     */
    Connection parser(Parser parser);

    /**
     * Set the character-set used to encode the request body. Defaults to {@code UTF-8}.
     * @param charset character set to encode the request body
     * @return this Connection, for chaining
     */
    Connection postDataCharset(String charset);

    /**
     Set the authenticator to use for this connection, enabling requests to URLs, and via proxies, that require
     authentication credentials.
     <p>The authentication scheme used is automatically detected during the request execution.
     Supported schemes (subject to the platform) are {@code basic}, {@code digest}, {@code NTLM},
     and {@code Kerberos}.</p>

     <p>To use, supply a {@link RequestAuthenticator} function that:
     <ol>
     <li>validates the URL that is requesting authentication, and</li>
     <li>returns the appropriate credentials (username and password)</li>
     </ol>
     </p>

     <p>For example, to authenticate both to a proxy and a downstream web server:
     <code><pre>
     Connection session = Jsoup.newSession()
         .proxy("proxy.example.com", 8080)
         .auth(auth -&gt; {
             if (auth.isServer()) { // provide credentials for the request url
                 Validate.isTrue(auth.url().getHost().equals("example.com"));
                 // check that we're sending credentials were we expect, and not redirected out
                 return auth.credentials("username", "password");
             } else { // auth.isProxy()
                 return auth.credentials("proxy-user", "proxy-password");
             }
         });

     Connection.Response response = session.newRequest("https://example.com/adminzone/").execute();
     </pre></code>
     </p>

     <p>The system may cache the authentication and use it for subsequent requests to the same resource.</p>

     <p><b>Implementation notes</b></p>
     <p>For compatibility, on a Java 8 platform, authentication is set up via the system-wide default
     {@link java.net.Authenticator#setDefault(Authenticator)} method via a ThreadLocal delegator. Whilst the
     authenticator used is request specific and thread-safe, if you have other calls to {@code setDefault}, they will be
     incompatible with this implementation.</p>
     <p>On Java 9 and above, the preceding note does not apply; authenticators are directly set on the request. </p>
     <p>If you are attempting to authenticate to a proxy that uses the {@code basic} scheme and will be fetching HTTPS
     URLs, you need to configure your Java platform to enable that, by setting the
     {@code jdk.http.auth.tunneling.disabledSchemes} system property to {@code ""}.
     This must be executed prior to any authorization attempts. E.g.:
     <code><pre>
     static {
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
        // removes Basic, which is otherwise excluded from auth for CONNECT tunnels
     }</pre></code>
     </p>
     * @param authenticator the authenticator to use in this connection
     * @return this Connection, for chaining
     * @since 1.17.1
     */
    default Connection auth(@Nullable RequestAuthenticator authenticator) {
        throw new UnsupportedOperationException();
    }

    /**
     * Execute the request as a GET, and parse the result.
     * @return parsed Document
     * @throws java.net.MalformedURLException if the request URL is not an HTTP or HTTPS URL, or is otherwise malformed
     * @throws HttpStatusException if the response is not OK and HTTP response errors are not ignored
     * @throws UnsupportedMimeTypeException if the response mime type is not supported and those errors are not ignored
     * @throws java.net.SocketTimeoutException if the connection times out
     * @throws IOException on error
     */
    Document get() throws IOException;

    /**
     * Execute the request as a POST, and parse the result.
     * @return parsed Document
     * @throws java.net.MalformedURLException if the request URL is not a HTTP or HTTPS URL, or is otherwise malformed
     * @throws HttpStatusException if the response is not OK and HTTP response errors are not ignored
     * @throws UnsupportedMimeTypeException if the response mime type is not supported and those errors are not ignored
     * @throws java.net.SocketTimeoutException if the connection times out
     * @throws IOException on error
     */
    Document post() throws IOException;

    /**
     * Execute the request.
     * @return the executed {@link Response}
     * @throws java.net.MalformedURLException if the request URL is not a HTTP or HTTPS URL, or is otherwise malformed
     * @throws HttpStatusException if the response is not OK and HTTP response errors are not ignored
     * @throws UnsupportedMimeTypeException if the response mime type is not supported and those errors are not ignored
     * @throws java.net.SocketTimeoutException if the connection times out
     * @throws IOException on error
     */
    Response execute() throws IOException;

    /**
     * Get the request object associated with this connection
     * @return request
     */
    Request request();

    /**
     * Set the connection's request
     * @param request new request object
     * @return this Connection, for chaining
     */
    Connection request(Request request);

    /**
     * Get the response, once the request has been executed.
     * @return response
     * @throws IllegalArgumentException if called before the response has been executed.
     */
    Response response();

    /**
     * Set the connection's response
     * @param response new response
     * @return this Connection, for chaining
     */
    Connection response(Response response);

    /**
     Set the response progress handler, which will be called periodically as the response body is downloaded. Since
     documents are parsed as they are downloaded, this is also a good proxy for the parse progress.
     <p>The Response object is supplied as the progress context, and may be read from to obtain headers etc.</p>
     @param handler the progress handler
     @return this Connection, for chaining
     @since 1.18.1
     */
    default Connection onResponseProgress(Progress<Response> handler) {
        throw new UnsupportedOperationException();
    }

    /**
     * Common methods for Requests and Responses
     * @param <T> Type of Base, either Request or Response
     */
    @SuppressWarnings("UnusedReturnValue")
    interface Base<T extends Base<T>> {
        /**
         * Get the URL of this Request or Response. For redirected responses, this will be the final destination URL.
         * @return URL
         * @throws IllegalArgumentException if called on a Request that was created without a URL.
         */
        URL url();

        /**
         * Set the URL
         * @param url new URL
         * @return this, for chaining
         */
        T url(URL url);

        /**
         * Get the request method, which defaults to <code>GET</code>
         * @return method
         */
        Method method();

        /**
         * Set the request method
         * @param method new method
         * @return this, for chaining
         */
        T method(Method method);

        /**
         * Get the value of a header. If there is more than one header value with the same name, the headers are returned
         * comma separated, per <a href="https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2">rfc2616-sec4</a>.
         * <p>
         * Header names are case-insensitive.
         * </p>
         * @param name name of header (case-insensitive)
         * @return value of header, or null if not set.
         * @see #hasHeader(String)
         * @see #cookie(String)
         */
        @Nullable String header(String name);

        /**
         * Get the values of a header.
         * @param name header name, case-insensitive.
         * @return a list of values for this header, or an empty list if not set.
         */
        List<String> headers(String name);

        /**
         * Set a header. This method will overwrite any existing header with the same case-insensitive name. If there
         * is more than one value for this header, this method will update the first matching header.
         * <p>For compatibility, if the content of the header includes text that cannot be represented by ISO-8859-1,
         * then it should be encoded first per <a href="https://www.ietf.org/rfc/rfc2047.txt">RFC 2047</a>.</p>
         * @param name Name of header
         * @param value Value of header
         * @return this, for chaining
         * @see #addHeader(String, String)
         */
        T header(String name, String value);

        /**
         * Add a header. The header will be added regardless of whether a header with the same name already exists.
         * <p>For compatibility, if the content of the header includes text that cannot be represented by ISO-8859-1,
         * then it should be encoded first per <a href="https://www.ietf.org/rfc/rfc2047.txt">RFC 2047</a>.</p>
         * @param name Name of new header
         * @param value Value of new header
         * @return this, for chaining
         */
        T addHeader(String name, String value);

        /**
         * Check if a header is present
         * @param name name of header (case-insensitive)
         * @return if the header is present in this request/response
         */
        boolean hasHeader(String name);

        /**
         * Check if a header is present, with the given value
         * @param name header name (case-insensitive)
         * @param value value (case-insensitive)
         * @return if the header and value pair are set in this req/res
         */
        boolean hasHeaderWithValue(String name, String value);

        /**
         * Remove headers by name. If there is more than one header with this name, they will all be removed.
         * @param name name of header to remove (case-insensitive)
         * @return this, for chaining
         */
        T removeHeader(String name);

        /**
         * Retrieve all of the request/response header names and corresponding values as a map. For headers with multiple
         * values, only the first header is returned.
         * <p>Note that this is a view of the headers only, and changes made to this map will not be reflected in the
         * request/response object.</p>
         * @return headers
         * @see #multiHeaders()

         */
        Map<String, String> headers();

        /**
         * Retreive all of the headers, keyed by the header name, and with a list of values per header.
         * @return a list of multiple values per header.
         */
        Map<String, List<String>> multiHeaders();

        /**
         * Get a cookie value by name from this request/response.
         * @param name name of cookie to retrieve.
         * @return value of cookie, or null if not set
         */
        @Nullable String cookie(String name);

        /**
         * Set a cookie in this request/response.
         * @param name name of cookie
         * @param value value of cookie
         * @return this, for chaining
         */
        T cookie(String name, String value);

        /**
         * Check if a cookie is present
         * @param name name of cookie
         * @return if the cookie is present in this request/response
         */
        boolean hasCookie(String name);

        /**
         * Remove a cookie by name
         * @param name name of cookie to remove
         * @return this, for chaining
         */
        T removeCookie(String name);

        /**
         Retrieve the request/response cookies as a map. For response cookies, if duplicate cookie names were sent, the
         last one set will be the one included. For session management, rather than using these response cookies, prefer
         to use {@link Jsoup#newSession()} and related methods.

         @return simple cookie map
         @see #cookieStore()
         */
        Map<String, String> cookies();
    }

    /**
     * Represents a HTTP request.
     */
    @SuppressWarnings("UnusedReturnValue")
    interface Request extends Base<Request> {
        /**
         * Get the proxy used for this request.
         * @return the proxy; <code>null</code> if not enabled.
         */
        @Nullable Proxy proxy();

        /**
         * Update the proxy for this request.
         * @param proxy the proxy ot use; <code>null</code> to disable.
         * @return this Request, for chaining
         */
        Request proxy(@Nullable Proxy proxy);

        /**
         * Set the HTTP proxy to use for this request.
         * @param host the proxy hostname
         * @param port the proxy port
         * @return this Connection, for chaining
         */
        Request proxy(String host, int port);

        /**
         * Get the request timeout, in milliseconds.
         * @return the timeout in milliseconds.
         */
        int timeout();

        /**
         * Update the request timeout.
         * @param millis timeout, in milliseconds
         * @return this Request, for chaining
         */
        Request timeout(int millis);

        /**
         * Get the maximum body size, in bytes.
         * @return the maximum body size, in bytes.
         */
        int maxBodySize();

        /**
         * Update the maximum body size, in bytes.
         * @param bytes maximum body size, in bytes.
         * @return this Request, for chaining
         */
        Request maxBodySize(int bytes);

        /**
         * Get the current followRedirects configuration.
         * @return true if followRedirects is enabled.
         */
        boolean followRedirects();

        /**
         * Configures the request to (not) follow server redirects. By default this is <b>true</b>.
         * @param followRedirects true if server redirects should be followed.
         * @return this Request, for chaining
         */
        Request followRedirects(boolean followRedirects);

        /**
         * Get the current ignoreHttpErrors configuration.
         * @return true if errors will be ignored; false (default) if HTTP errors will cause an IOException to be
         * thrown.
         */
        boolean ignoreHttpErrors();

        /**
         * Configures the request to ignore HTTP errors in the response.
         * @param ignoreHttpErrors set to true to ignore HTTP errors.
         * @return this Request, for chaining
         */
        Request ignoreHttpErrors(boolean ignoreHttpErrors);

        /**
         * Get the current ignoreContentType configuration.
         * @return true if invalid content-types will be ignored; false (default) if they will cause an IOException to
         * be thrown.
         */
        boolean ignoreContentType();

        /**
         * Configures the request to ignore the Content-Type of the response.
         * @param ignoreContentType set to true to ignore the content type.
         * @return this Request, for chaining
         */
        Request ignoreContentType(boolean ignoreContentType);

        /**
         * Get the current custom SSL socket factory, if any.
         * @return custom SSL socket factory if set, null otherwise
         */
        @Nullable SSLSocketFactory sslSocketFactory();

        /**
         Set a custom SSL socket factory for HTTPS connections.
         <p>Note: if set, the legacy <code>HttpURLConnection</code> will be used instead of the JVM's
         <code>HttpClient</code>.</p>

         @param sslSocketFactory SSL socket factory
         @see #sslContext(SSLContext)
         @deprecated use {@link #sslContext(SSLContext)} instead.
         */
        @Deprecated
        void sslSocketFactory(SSLSocketFactory sslSocketFactory);

        /**
         Get the current custom SSL context, if any.

         @return custom SSL context if set, null otherwise
         @since 1.21.2
         */
        @Nullable
        default SSLContext sslContext() {
            throw new UnsupportedOperationException();
        }

        /**
         Set a custom SSL context for HTTPS connections.
         <p>Note: when using the legacy <code>HttpURLConnection</code>, only the <code>SSLSocketFactory</code> from the
         context will be used.</p>

         @param sslContext SSL context
         @return this Request, for chaining
         @since 1.21.2
         */
        default Request sslContext(SSLContext sslContext) {
            throw new UnsupportedOperationException();
        }

        /**
         * Add a data parameter to the request
         * @param keyval data to add.
         * @return this Request, for chaining
         */
        Request data(KeyVal keyval);

        /**
         * Get all of the request's data parameters
         * @return collection of keyvals
         */
        Collection<KeyVal> data();

        /**
         * Set a POST (or PUT) request body. Useful when a server expects a plain request body, not a set of URL
         * encoded form key/value pairs. E.g.:
         * <code><pre>Jsoup.connect(url)
         * .requestBody(json)
         * .header("Content-Type", "application/json")
         * .post();</pre></code>
         * <p>If any data key/vals are supplied, they will be sent as URL query params.</p>
         * @param body to use as the request body. Set to null to clear a previously set body.
         * @return this Request, for chaining
         * @see #requestBodyStream(InputStream)
         */
        Request requestBody(@Nullable String body);

        /**
         * Get the current request body.
         * @return null if not set.
         */
        @Nullable String requestBody();

        /**
         Set the request body. Useful for posting data such as byte arrays or files, and the server expects a single
         request body (and not a multipart upload). E.g.:
         <code><pre> Jsoup.connect(url)
         .requestBody(new ByteArrayInputStream(bytes))
         .header("Content-Type", "application/octet-stream")
         .post();
         </pre></code>
         <p>Or, use a FileInputStream to data from disk.</p>
         <p>You should close the stream in a finally block.</p>

         @param stream the input stream to send.
         @return this Request, for chaining
         @see #requestBody(String)
         @since 1.20.1
         */
        default Request requestBodyStream(InputStream stream) {
            throw new UnsupportedOperationException();
        }

        /**
         * Specify the parser to use when parsing the document.
         * @param parser parser to use.
         * @return this Request, for chaining
         */
        Request parser(Parser parser);

        /**
         * Get the current parser to use when parsing the document.
         * @return current Parser
         */
        Parser parser();

        /**
         * Sets the post data character set for x-www-form-urlencoded post data
         * @param charset character set to encode post data
         * @return this Request, for chaining
         */
        Request postDataCharset(String charset);

        /**
         * Gets the post data character set for x-www-form-urlencoded post data
         * @return character set to encode post data
         */
        String postDataCharset();

        /**
         Set the authenticator to use for this request.
         See {@link Connection#auth(RequestAuthenticator) Connection.auth(authenticator)} for examples and
         implementation notes.
         * @param authenticator the authenticator
         * @return this Request, for chaining.
         * @since 1.17.1
         */
        default Request auth(@Nullable RequestAuthenticator authenticator)  {
            throw new UnsupportedOperationException();
        }

        /**
         Get the RequestAuthenticator, if any, that will be used on this request.
         * @return the RequestAuthenticator, or {@code null} if not set
         * @since 1.17.1
         */
        @Nullable
        default RequestAuthenticator auth() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Represents a HTTP response.
     */
    interface Response extends Base<Response> {

        /**
         * Get the status code of the response.
         * @return status code
         */
        int statusCode();

        /**
         * Get the status message of the response.
         * @return status message
         */
        String statusMessage();

        /**
         * Get the character set name of the response, derived from the content-type header.
         * @return character set name if set, <b>null</b> if not
         */
        @Nullable String charset();

        /**
         * Set / override the response character set. When the document body is parsed it will be with this charset.
         * @param charset to decode body as
         * @return this Response, for chaining
         */
        Response charset(String charset);

        /**
         * Get the response content type (e.g. "text/html");
         * @return the response content type, or <b>null</b> if one was not set
         */
        @Nullable String contentType();

        /**
         Read and parse the body of the response as a Document. If you intend to parse the same response multiple times,
         you should {@link #readFully()} first, which will buffer the body into memory.

         @return a parsed Document
         @throws IOException if an IO exception occurs whilst reading the body.
         @see #readFully()
         */
        Document parse() throws IOException;

        /**
         Read the response body, and returns it as a plain String.

         @return body
         @throws IOException if an IO exception occurs whilst reading the body.
         @since 1.21.1
         */
        default String readBody() throws IOException {
            throw new UnsupportedOperationException();
        }

        /**
         Get the body of the response as a plain String.

         <p>Will throw an UncheckedIOException if the body has not been buffered and an error occurs whilst reading the
         body; use {@link #readFully()} first to buffer the body and catch any exceptions explicitly. Or more simply,
         {@link #readBody()}.</p>

         @return body
         @throws UncheckedIOException if an IO exception occurs whilst reading the body.
         @see #readBody()
         @see #readFully()
         */
        String body();

        /**
         Get the body of the response as an array of bytes.

         <p>Will throw an UncheckedIOException if the body has not been buffered and an error occurs whilst reading the
         body; use {@link #readFully()} first to buffer the body and catch any exceptions explicitly.</p>

         @return body bytes
         @throws UncheckedIOException if an IO exception occurs whilst reading the body.
         @see #readFully()
         */
        byte[] bodyAsBytes();

        /**
         Read the body of the response into a local buffer, so that {@link #parse()} may be called repeatedly on the same
         connection response. Otherwise, once the response is read, its InputStream will have been drained and may not be
         re-read.

         <p>Subsequent calls methods than consume the body, such as {@link #parse()}, {@link #body()},
         {@link #bodyAsBytes()}, will not need to read the body again, and will not throw exceptions.</p>
         <p>Calling {@link #readBody()}} has the same effect.</p>

         @return this response, for chaining
         @throws IOException if an IO exception occurs during buffering.
         @since 1.21.1
         */
        default Response readFully() throws IOException {
            throw new UnsupportedOperationException();
        }

        /**
         * Read the body of the response into a local buffer, so that {@link #parse()} may be called repeatedly on the
         * same connection response. Otherwise, once the response is read, its InputStream will have been drained and
         * may not be re-read.
         * <p>Calling {@link #body() } or {@link #bodyAsBytes()} has the same effect.</p>
         * @return this response, for chaining
         * @throws UncheckedIOException if an IO exception occurs during buffering.
         * @deprecated use {@link #readFully()} instead (for the checked exception). Will be removed in a future version.
         */
        @Deprecated
        Response bufferUp();

        /**
         Get the body of the response as a (buffered) InputStream. You should close the input stream when you're done
         with it.
         <p>Other body methods (like readFully, body, parse, etc) will generally not work in conjunction with this method,
         as it consumes the InputStream.</p>
         <p>Any configured max size or maximum read timeout applied to the connection will not be applied to this stream,
         unless {@link #readFully()} is called prior.</p>
         <p>This method is useful for writing large responses to disk, without buffering them completely into memory
         first.</p>
         @return the response body input stream
         */
        BufferedInputStream bodyStream();

        /**
         Returns a {@link StreamParser} that will parse the Response progressively.
         * @return a StreamParser, prepared to parse this response.
         * @throws IOException if an IO exception occurs preparing the parser.
         */
        default StreamParser streamParser() throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A Key:Value tuple(+), used for form data.
     */
    interface KeyVal {

        /**
         * Update the key of a keyval
         * @param key new key
         * @return this KeyVal, for chaining
         */
        KeyVal key(String key);

        /**
         * Get the key of a keyval
         * @return the key
         */
        String key();

        /**
         * Update the value of a keyval
         * @param value the new value
         * @return this KeyVal, for chaining
         */
        KeyVal value(String value);

        /**
         * Get the value of a keyval
         * @return the value
         */
        String value();

        /**
         * Add or update an input stream to this keyVal
         * @param inputStream new input stream
         * @return this KeyVal, for chaining
         */
        KeyVal inputStream(InputStream inputStream);

        /**
         * Get the input stream associated with this keyval, if any
         * @return input stream if set, or null
         */
        @Nullable InputStream inputStream();

        /**
         * Does this keyval have an input stream?
         * @return true if this keyval does indeed have an input stream
         */
        boolean hasInputStream();

        /**
         * Set the Content Type header used in the MIME body (aka mimetype) when uploading files.
         * Only useful if {@link #inputStream(InputStream)} is set.
         * <p>Will default to {@code application/octet-stream}.</p>
         * @param contentType the new content type
         * @return this KeyVal
         */
        KeyVal contentType(String contentType);

        /**
         * Get the current Content Type, or {@code null} if not set.
         * @return the current Content Type.
         */
        @Nullable String contentType();
    }
}
