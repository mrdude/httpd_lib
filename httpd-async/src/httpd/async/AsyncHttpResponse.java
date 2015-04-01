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
package httpd.async;

import httpd.HttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class AsyncHttpResponse implements HttpResponse
{
	private String responseLine;
	private HashMap<String, String> headers = new HashMap<>();
	private InputStream in;

	AsyncHttpResponse() {}

	/**
	 * Sets the response line. Do *NOT* include the protocol.
	 * @return this
	 */
	public AsyncHttpResponse responseLine(String resLine)
	{
		responseLine = resLine;
		return this;
	}

	/**
	 * Adds/sets a header
	 * @return this
	 */
	public AsyncHttpResponse header(String key, Object value)
	{
		headers.put(key, value.toString());
		return this;
	}

	/**
	 * Sets the InputStream that will become the body of the response
	 * @return this
	 */
	public AsyncHttpResponse body(InputStream in)
	{
		this.in = in;
		return this;
	}

	String responseLine() { return responseLine; }
	Iterator<Map.Entry<String, String>> headerIterator() { return headers.entrySet().iterator(); }
	InputStream body() { return in; }
	void closeBodyStream()
	{
		try
		{
			in.close();
		}
		catch(IOException e) {}
	}
}