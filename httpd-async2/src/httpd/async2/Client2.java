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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;

class Client2
{
	private static final Logger logger = LoggerFactory.getLogger(Client2.class);
	private ByteBuffer buf = ByteBuffer.allocate(1024 * 4);
	private SocketAddress addr; //the address of the client
	private AsyncHttpServer2 sv;
	private AsyncHttpRequest2 req;
	private boolean useKeepAliveConnection; //should we use a keep-alive connection?

	private DeferredWrite dw;

	Client2(AsyncHttpServer2 sv, SocketChannel sc)
	{
		this.sv = sv;

		try
		{
			addr = sc.getRemoteAddress();
		}
		catch(IOException e) {}

		logger.info("New client: {}", addr == null ? "Unknown address" : addr);
	}

	void read(SelectionKey key)
	{
		SocketChannel sc = (SocketChannel)key.channel();

		try
		{
			int bytesRead = sc.read(buf);
			if( bytesRead == -1 ) //the connection was closed nicely by the client
			{
				close(key);
				return;
			}
		}
		catch(IOException e) //the connection was forcibly closed by the client
		{
			close(key);
			return;
		}

		if( checkForNewLine() ) //there is a \n in the buffer
		{
			int count = 0;
			do
			{
				String line = getNewLine();
				processLine(key, line);
				count++;
			}
			while( checkForNewLine() );
		}
		else //there is no \n in the buffer
		{
			if( buf.capacity() == buf.position() ) //if the buffer is full, close the Socket
				close(key);
		}
	}

	void write(SelectionKey key)
	{
		if( dw.update(key) )
			completeRequest(key);
	}

	/** Checks the ByteBuffer for a \n */
	private boolean checkForNewLine()
	{
		for( int x=0; x<buf.position(); x++ )
		{
			byte b = buf.get(x);
			if( b == '\n' )
				return true;
		}

		return false;
	}

	private String getNewLine()
	{
		//read the line from the ByteBuffer
		buf.flip();
		StringBuilder str = new StringBuilder();
		while(true)
		{
			char ch = (char)buf.get();
			if( ch == '\n' )
				break;

			str.append(ch);
		}

		//remove the line from the ByteBuffer
		int bytesToWrite = buf.limit() - buf.position();
		for( int x=0; x<bytesToWrite; x++ )
		{
			byte b = buf.get( buf.position() + x );
			buf.put(x, b);
		}

		buf.position(bytesToWrite);

		//return the line
		return str.toString();
	}

	private void processLine(SelectionKey key, String line)
	{
		if( req == null )
		{
			req = new AsyncHttpRequest2();
			req.processInitialRequest(line); //TODO handle invalid initial requests
		}
		else
		{
			if( line.trim().equals("") ) //handle the request
			{
				handleRequest(key);
				buf.clear(); //reset the read buffer
			}
			else
			{
				req.processHeader(line); //process a header
			}
		}
	}

	private void handleRequest(SelectionKey key)
	{
		//generate the response
		req.lock();
		AsyncHttpResponse2 res;

		try
		{
			res = new AsyncHttpResponse2();
			sv.requestHandler().handleRequest(req, res);
		}
		catch(Exception e)
		{
			logger.error("RequestHandler threw an exception, using the 500 generator: {}", e);
			res = new AsyncHttpResponse2();
			sv.requestHandler().generate500Page(req, res);
		}

		//should we use a keep-alive connection?
		/* We can only use keep-alive connection if:
		 * 1) the client supports it (the client sent a Connection: keep-alive header)
		 * 2) we aren't using Transfer-Encoding: chunked (which this server doesn't support)
		 * 3) we supplied a Content-Length
		 */
		if( req.supportsKeepAlive() && res.hasContentLengthHeader() )
		{
			res.header("Connection", "keep-alive");
			useKeepAliveConnection = true;
		}

		//write the headers
		{
			ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			BufferedWriter out = new BufferedWriter( new OutputStreamWriter(byteOut) );

			try
			{
				//response line
				out.write( req.protocol() );
				out.write(" ");
				out.write( res.responseLine() );
				out.newLine();

				//headers
				Iterator< Map.Entry<String, String> > it = res.headerIterator();
				while( it.hasNext() )
				{
					Map.Entry<String, String> e = it.next();

					out.write( e.getKey() );
					out.write(": ");
					out.write( e.getValue() );
					out.newLine();
				}

				out.newLine();
				out.flush();
			}
			catch(IOException e)
			{
					/* Since we are writing to a ByteArrayOutputStream, we don't
					 * need to deal with this error.
					 */
			}

			ByteBuffer bb = ByteBuffer.wrap( byteOut.toByteArray() );

			try
			{
				writeByteBufferToSocket(bb, (SocketChannel)key.channel());
			}
			catch(IOException e) //the client closed the connection
			{
				close(key);
				return;
			}
		}

		//change the interestOps to OP_WRITE, so that the selection loop in AsyncHttpRequest2 will call Client2.write()
		key.interestOps( SelectionKey.OP_WRITE );

		//prepare for deferred writes
		if( res.body() instanceof FileInputStream )
			dw = new FileChannelDeferredWrite( (FileInputStream)res.body() );
		else
			dw = new CopyingDeferredWrite( res.body() );
	}

	/** Writes the entire ByteBuffer to the given SocketChannel */
	private void writeByteBufferToSocket(ByteBuffer bb, SocketChannel sc) throws IOException
	{
		int bytesWritten = 0;
		while(true)
		{
			bytesWritten += sc.write(bb);
			if( bytesWritten == bb.limit() )
				break;
		}
	}

	/** This is called after a response is successfully sent. */
	private void completeRequest(SelectionKey key)
	{
		if( useKeepAliveConnection ) //the client supports keep-alive connections, and we supplied a Content-Length
		{
			//reset the Client2 object
			buf.clear();
			req = null;
			dw = null;
			useKeepAliveConnection = false;

			//make sure that the key's interest ops are OP_READ
			key.interestOps( SelectionKey.OP_READ );

			logger.info("Keeping a connection alive... (Client Address = {})", addr == null ? "Unknown address" : addr);
		}
		else //the client doesn't support keep-alive connections
		{
			//close the connection
			close(key);

			logger.info("Closing a connection... (Client Address = {})", addr == null ? "Unknown address" : addr);
		}
	}

	void close(SelectionKey key)
	{
		SocketChannel sc = (SocketChannel)key.channel();
		key.cancel();

		try
		{
			sc.close();
		}
		catch(IOException e) {}
	}

	private abstract class DeferredWrite
	{
		/** Returns true when the write is finished */
		public abstract boolean update(SelectionKey key);
	}

	private class CopyingDeferredWrite extends DeferredWrite
	{
		private ReadableByteChannel inChannel;
		private ByteBuffer buf = ByteBuffer.allocate( 4096 );

		public CopyingDeferredWrite( InputStream in )
		{
			buf.position( 0 ).flip();
			inChannel = Channels.newChannel(in);
		}

		public boolean update(SelectionKey key)
		{
			try
			{
				if( buf.remaining() == 0 )
					refillByteBuffer();

				SocketChannel sc = (SocketChannel)key.channel();
				sc.write( buf );

				return false;
			}
			catch(IOException e)
			{
				return true;
			}
		}

		private void refillByteBuffer() throws IOException
		{
			buf.clear();
			int bytesRead = inChannel.read( buf );
			if( bytesRead == -1 ) //end of stream
				throw new EOFException();
			buf.flip();
		}
	}

	private class FileChannelDeferredWrite extends DeferredWrite
	{
		private FileChannel inChannel;
		private long pos;

		public FileChannelDeferredWrite(FileInputStream fis)
		{
			inChannel = fis.getChannel();
		}

		public boolean update(SelectionKey key)
		{
			try
			{
				SocketChannel sc = (SocketChannel) key.channel();

				long bytesWritten = inChannel.transferTo( pos, 1024 * 1024, sc ); //transfer bytes 1MB at a time
				if( pos >= inChannel.size() || bytesWritten == -1 ) //end of file was reached -- return true
					return true;

				pos += bytesWritten;
			}
			catch(IOException e)
			{
				//TODO I'm not entirely sure what to do with this exception....
			}

			return false;
		}
	}
}