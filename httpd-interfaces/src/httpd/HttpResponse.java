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
package httpd;

import java.io.*;

public interface HttpResponse
{
	/**
	 * Sets the response line. Do *NOT* include the protocol.
	 * Examples: "200 OK", "404 File Not Found"
	 * @return this
	 */
	HttpResponse responseLine(String resLine);

	/**
	 * Adds/sets a header
	 * @return this
	 */
	HttpResponse header(String key, String value);

	/**
	 * Sets the InputStream that will become the body of the response
	 * @return this
	 */
	HttpResponse body(InputStream in);

	/**
	 * Sets the byte array that will become the body of the response
	 * @return this
	 */
	HttpResponse body(byte[] b);

	/**
	 * By toggling this flag, you hint to the HttpServer handling your response that you want it to be handled with a deferred write.
	 * Only AsyncHttpServer2 supports deferred writes.
	 * @return this
	 */
	HttpResponse deferredWrite(boolean on);
}