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

import httpd.HttpServer;
import httpd.RequestHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

//TODO drop connections that take too long -- to fight BEAST
/** @deprecated Use AsyncHttpServer2 instead */
public class AsyncHttpServer implements HttpServer
{
	private volatile boolean quitFlag;
	private Thread t;

	private RequestHandler reqHandler;
	private Selector selector;

	public boolean start(RequestHandler reqHandler, List<InetSocketAddress> listenInterfaces)
	{
		if( reqHandler == null )
			return false;

		this.reqHandler = reqHandler;

		try
		{
			selector = Selector.open();

			System.out.println("Bound interfaces:");
			for( InetSocketAddress addr : listenInterfaces )
			{
				try
				{
					ServerSocketChannel ssc = ServerSocketChannel.open();
					ssc.configureBlocking(false);
					ssc.bind( addr );
					ssc.register(selector, SelectionKey.OP_ACCEPT);

					System.out.println("\t" +ssc.getLocalAddress());
				}
				catch(IOException e) {}
			}
			System.out.println("End of bound interfaces");
		}
		catch(IOException e)
		{
			return false;
		}

		t = new Thread( new Runnable() {
			public void run() { AsyncHttpServer.this.run(); }
		});
		t.setName("AsyncHttpServerThread");
		t.start();

		return true;
	}

	private void run()
	{
		while( !quitFlag )
		{
			try
			{
				selector.select();
				Iterator<SelectionKey> it = selector.selectedKeys().iterator();
				while( it.hasNext() )
				{
					SelectionKey key = it.next();
					it.remove(); //TODO is this needed?

					if( !key.isValid() ) //TODO is this needed?
						continue;

					if( key.isAcceptable() )
						accept(key);
					else if( key.isReadable() )
						read(key);
				}
			}
			catch(IOException e)
			{
				System.err.println("Caught an exception from a selector:");
				e.printStackTrace(System.err);
			}
		}
	}

	private void accept(SelectionKey key)
	{
		ServerSocketChannel ssc = (ServerSocketChannel)key.channel();

		try
		{
			SocketChannel sc = ssc.accept();
			sc.configureBlocking(false);
			SelectionKey clientKey = sc.register(selector, SelectionKey.OP_READ);
			clientKey.attach( new Client(this, sc) );
		}
		catch(IOException e) {}
	}

	private void read(SelectionKey key)
	{
		Client cl = (Client)key.attachment();
		cl.read(key);
	}

	public void stop()
	{
		quitFlag = true;
		selector.wakeup();
	}

	RequestHandler requestHandler() { return reqHandler; }
}