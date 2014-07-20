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

import httpd.HttpServer;
import httpd.async2.AsyncHttpServer2;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * Serves files from a single directory
 */
public class BasicWebServer
{
	/** We will be serving files from this directory */
	private static final File docroot = new File("docroot");

	public static void main(String[] args) throws Exception
	{
		HttpServer sv = new AsyncHttpServer2();
		sv.start( new RequestHandlerImpl( docroot ), getListenInterfaces() );

		System.out.println( "HTTP server is now running in a separate thread and serving files from " + docroot.getAbsolutePath() );

		doOtherApplicationStuff();

		System.out.println( "Stopping server" );
		sv.stop();
		System.out.println( "Server is stopped" );

		System.exit( 0 );
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
