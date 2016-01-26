/*
The MIT License (MIT)

Copyright (c) <year> <copyright holders>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package httpd.async2;

import com.google.common.collect.ImmutableMap;
import httpd.HttpRequest;
import httpd.common.Parsers;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

class AsyncHttpRequest2 implements HttpRequest
{
	private volatile boolean locked; //is this object locked?
	private String initialRequestLine;
	private String method;
	private String request;
	private String protocol;
	private ImmutableMap<String, String> getParameters;
	private HashMap<String, String> headers = new HashMap<>();
	private ImmutableMap<String, String> immutableHeaders;

	AsyncHttpRequest2() {}

	/**
	 * Processes the initial request line.
	 * Calling this method on a locked HttpRequest will result in a RuntimeException
	 * @return true if successful, false if the request line was malformed
	 */
	boolean processInitialRequest(String line)
	{
		if( locked )
			throw new RuntimeException("Called HttpRequest.processInitialRequest() on a locked HttpRequest");

		Parsers.InitialRequestLine req = Parsers.processInitialRequestLine( line );

		if( req == null )
			return false;

		initialRequestLine = req.requestURL;
		method = req.method;
		request = req.request;
		protocol = req.protocol;
		getParameters = req.getParameters;

		return true;
	}

	/**
	 * Processes a single line of the header
	 * Calling this method on a locked HttpRequest will result in a RuntimeException
	 */
	void processHeader(String headerLine)
	{
		if( locked )
			throw new RuntimeException("Called HttpRequest.processHeader() on a locked HttpRequest");

		String[] split = headerLine.split(":");

		if( split.length != 2 ) //ignore any malformed headers
			return;

		String key = split[0].trim();
		String value = split[1].trim();
		headers.put( key, value );
	}

	/**
	 * "Locks" the HttpRequest, effectively making it immutable.
	 * A HttpRequest must be locked before it can be used by a RequestHandler.
	 */
	void lock()
	{
		if( locked ) //if the object is already locked, return
			return;

		locked = true;
		immutableHeaders = ImmutableMap.copyOf(headers);
		headers = null; //get rid of the headers object -- we don't need it anymore, so it can be GC-ed
	}

	public String initialRequestLine() { return initialRequestLine; }
	public String method() { return method; }
	public String request() { return request; }
	public String protocol() { return protocol; }
	public ImmutableMap<String, String> getParameters() { return getParameters; }
	public ImmutableMap<String, String> headers() { return immutableHeaders; }

	public String toString() { return method+ " " +request+ " " +protocol; }

	boolean supportsKeepAlive()
	{
		if( !headers().containsKey("Connection") )
			return false;

		return headers().get("Connection").equals("keep-alive");
	}
}