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

import httpd.HttpServer;
import httpd.RequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;

//TODO drop connections that take too long -- to fight BEAST
public class AsyncHttpServer2 implements HttpServer
{
	private static final Logger logger = LoggerFactory.getLogger(AsyncHttpServer2.class);

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

			logger.info("Bound interfaces:");
			for( InetSocketAddress addr : listenInterfaces )
			{
				try
				{
					ServerSocketChannel ssc = ServerSocketChannel.open();
					ssc.configureBlocking(false);
					ssc.bind( addr );
					ssc.register(selector, SelectionKey.OP_ACCEPT);

					logger.info("\t{}", ssc.getLocalAddress());
				}
				catch(IOException e) {}
			}
			logger.info("End of bound interfaces");
		}
		catch(IOException e)
		{
			return false;
		}

		t = new Thread( new Runnable() {
			public void run() { AsyncHttpServer2.this.run(); }
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
					it.remove();

					if( !key.isValid() )
						continue;

					if( key.isAcceptable() )
						accept(key);
					else if( key.isReadable() )
						read(key);
					else if( key.isWritable() )
						write(key);
				}
			}
			catch(IOException e)
			{
				System.err.println("Caught an exception from a selector:");
				e.printStackTrace(System.err);
			}
		}

		//TODO shut down all Sockets, ServerSockets, and then close the selector
	}

	private void accept(SelectionKey key)
	{
		ServerSocketChannel ssc = (ServerSocketChannel)key.channel();

		try
		{
			SocketChannel sc = ssc.accept();
			sc.configureBlocking(false);
			SelectionKey clientKey = sc.register(selector, SelectionKey.OP_READ);
			clientKey.attach( new Client2(this, sc) );
		}
		catch(IOException e) {}
	}

	private void read(SelectionKey key)
	{
		Client2 cl = (Client2)key.attachment();
		cl.read(key);
	}

	private void write(SelectionKey key)
	{
		Client2 cl = (Client2)key.attachment();
		cl.write(key);
	}

	public void stop()
	{
		quitFlag = true;
		selector.wakeup();
	}

	RequestHandler requestHandler() { return reqHandler; }
}