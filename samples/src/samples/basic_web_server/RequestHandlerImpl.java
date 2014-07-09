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
package samples.basic_web_server;

import httpd.HttpRequest;
import httpd.HttpResponse;
import httpd.RequestHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;

class RequestHandlerImpl implements RequestHandler
{
	private final File docroot;

	RequestHandlerImpl(File docroot)
	{
		this.docroot = docroot;
	}

	public void handleRequest(HttpRequest req, HttpResponse res) throws Exception
	{
		File responseFile;
		if( req.request().equals("/") )
			responseFile = new File(docroot, "index.html");
		else
			responseFile = new File(docroot, req.request());

		FileInputStream responseFileInputStream = new FileInputStream( responseFile );

		res.responseLine( "200 OK" )
		   .header( "Server", "BasicWebServer-httpd_lib" )
		   .header( "Content-Length", Long.toString( responseFile.length() ) )
		   .header( "Content-Type", "text/html" )
		   .body( responseFileInputStream )
		   .deferredWrite( true ); //this is so that writing the client's response to the socket won't block other clients
	}

	public void generate500Page(HttpRequest req, HttpResponse res)
	{
		byte[] data;

		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		PrintStream out = new PrintStream( byteOut );

		out.println("<html>");
		out.println("<head><title>500 Internal Server Error</title></head>");
		out.println("<body>");
		out.println("<h1>The server screwed up something while serving your request</h1>");
		out.println("</body>");
		out.println("</html>");

		out.flush();
		data = byteOut.toByteArray();

		res.responseLine( "500 Internal Server Error" )
		   .header( "Server", "BasicWebServer-httpd_lib" )
		   .header( "Content-Length", Integer.toString( data.length ) )
		   .header( "Content-Type", "text/html" )
		   .body( data );
	}
}
