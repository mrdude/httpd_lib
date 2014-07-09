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
package httpd.sync;

import httpd.HttpServer;
import httpd.RequestHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.LinkedList;
import java.util.List;

/**
 * @deprecated use AsyncHttpServer instead
 */
public class SyncHttpServer implements HttpServer
{
	private RequestHandler reqHandler;
	private List<ServerSocket> ssList = new LinkedList<>();
	private volatile boolean quitFlag;

	/**
	 * Starts the server
	 * @return true if the server was successfully started, false if not
	 */
	public boolean start(RequestHandler reqHandler, List<InetSocketAddress> listenInterfaces)
	{
		if( reqHandler == null )
			return false;

		this.reqHandler = reqHandler;

		//start the ServerSockets
		System.out.println("Bound interfaces:");
		for( InetSocketAddress addr : listenInterfaces )
		{
			try
			{
				ServerSocket ss = new ServerSocket( addr.getPort(), 10, addr.getAddress() );
				ss.setSoTimeout(1);

				ssList.add(ss);
				System.out.println("\t" +ss.getLocalSocketAddress());
			}
			catch(IOException e)
			{
				System.out.println("Could not create server @ " +addr+ ":");
				e.printStackTrace( System.out );
			}
		}
		System.out.println("End of bound interfaces");

		//make sure that we started some servers
		if( ssList.size() == 0 )
			return false;

		//spin up the worker threads
		final int workerThreadCount = 2;
		for( int x=0; x<workerThreadCount; x++ )
			new SyncRequestProcessor(this, x);

		return true;
	}

	/** Signals the server to stop */
	public void stop()
	{
		quitFlag = true;
	}

	Socket accept()
	{
		try
		{
			for( ServerSocket ss : ssList )
			{
				try
				{
					return ss.accept();
				}
				catch(SocketTimeoutException ste) {}
			}
		}
		catch(IOException e)
		{
			return null;
		}

		return null;
	}

	boolean quitFlag() { return quitFlag; }
	RequestHandler requestHandler() { return reqHandler; }
}