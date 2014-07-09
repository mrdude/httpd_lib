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
package samples.quick_and_dirty;

import httpd.HttpRequest;
import httpd.HttpResponse;
import httpd.HttpServer;
import httpd.RequestHandler;
import httpd.async2.AsyncHttpServer2;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class QuickAndDirtySample
{
	public static void main(String[] args)
	{
		HttpServer sv = new AsyncHttpServer2();
		if( !sv.start( getRequestHandlerImpl(), getListenInterfaces() ) )
		{
			System.out.println("Couldn't start the server.");
			System.exit(1);
		}

		System.out.println("HTTP server is now running in a seperate thread");

		doOtherApplicationStuff();

		System.out.println("Stopping server");
		sv.stop();
		System.out.println("Server is stopped");

		System.exit(0);
	}

	private static RequestHandler getRequestHandlerImpl()
	{
		return new RequestHandler()
		{
			public void handleRequest(HttpRequest req, HttpResponse res) throws Exception
			{
				byte[] data;

				ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
				PrintStream out = new PrintStream( byteOut );

				out.println("<html>");
				out.println("<head><title>200 OK</title></head>");
				out.println("<body>");
				out.println("<h1>Hello Internet World!</h1>");
				out.println("</body>");
				out.println("</html>");

				out.flush();
				data = byteOut.toByteArray();

				res.responseLine( "200 OK" )
				   .header( "Server", "SuperCoolCustomHttpLibThingy" )
				   .header( "Content-Length", Integer.toString( data.length ) )
				   .header( "Content-Type", "text/html" )
				   .body(data);
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
				   .header( "Server", "SuperCoolCustomHttpLibThingy" )
				   .header( "Content-Length", Integer.toString( data.length ) )
				   .header( "Content-Type", "text/html" )
				   .body( data );
			}
		};
	}

	private static List<InetSocketAddress> getListenInterfaces()
	{
		return Arrays.asList(
				new InetSocketAddress( 80 ),
				new InetSocketAddress( "localhost", 8080 )
		);
	}

	private static void doOtherApplicationStuff()
	{
		System.out.println("Type 'ENTER' to quit");
		new Scanner( System.in ).nextLine();
	}
}