package betamax.server

import org.apache.http.client.HttpClient
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager
import org.apache.http.*
import static org.apache.http.HttpHeaders.*
import org.apache.http.client.methods.*
import org.apache.http.protocol.*

class HttpProxyHandler implements HttpRequestHandler {

	private static final NO_PASS_HEADERS = [
			CONTENT_LENGTH,
			HOST,
			"Proxy-Connection",
			CONNECTION,
			"Keep-Alive",
			PROXY_AUTHENTICATE,
			PROXY_AUTHORIZATION,
			TE,
			TRAILER,
			TRANSFER_ENCODING,
			UPGRADE
	].toSet().asImmutable()

	private final HttpClient httpClient = new DefaultHttpClient(new ThreadSafeClientConnManager())

	void handle(HttpRequest request, HttpResponse response, HttpContext context) {
		println "${Thread.currentThread().name}:: $request.requestLine.method request for $request.requestLine.uri"

		def proxyRequest = createProxyRequest(request)
		copyRequestData(request, proxyRequest)
		proxyRequest.addHeader(VIA, "Betamax")

		def proxyResponse = httpClient.execute(proxyRequest)

		println "${Thread.currentThread().name}:: serving response with status $proxyResponse.statusLine.statusCode"

		copyResponseData(proxyResponse, response)
		response.addHeader("X-Betamax", "REC")
	}

	private void copyRequestData(HttpRequest from, HttpRequest to) {
		for (header in from.allHeaders) {
			if (!(header.name in NO_PASS_HEADERS)) {
				println "copying request header $header"
				to.addHeader(header)
			}
		}

		if (from instanceof HttpEntityEnclosingRequest) {
			to.entity = from.entity
		}
	}

	private void copyResponseData(HttpResponse from, HttpResponse to) {
		to.statusCode = from.statusLine.statusCode
		for (header in from.allHeaders) {
			if (!(header.name in NO_PASS_HEADERS)) {
				println "copying response header: $header"
				to.addHeader(header)
			}
		}
		to.entity = from.entity
	}

	private HttpRequest createProxyRequest(HttpRequest request) {
		def method = request.requestLine.method.toUpperCase(Locale.ENGLISH)
		switch (method) {
			case "DELETE":
				return new HttpDelete(request.requestLine.uri)
			case "GET":
				return new HttpGet(request.requestLine.uri)
			case "HEAD":
				return new HttpHead(request.requestLine.uri)
			case "OPTIONS":
				return new HttpOptions(request.requestLine.uri)
			case "POST":
				return new HttpPost(request.requestLine.uri)
			case "PUT":
				return new HttpPut(request.requestLine.uri)
			default:
				throw new MethodNotSupportedException("$method method not supported")
		}
	}

}