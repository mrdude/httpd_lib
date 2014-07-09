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

import java.io.*;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

class Client
{
	private ByteBuffer buf = ByteBuffer.allocate(4192);
	private AsyncHttpServer sv;
	private AsyncHttpRequest req;

	Client(AsyncHttpServer sv, SocketChannel sc)
	{
		this.sv = sv;

		SocketAddress addr = null;
		try
		{
			addr = sc.getRemoteAddress();
		}
		catch(IOException e) {}

		System.out.println("HTTP: New client:" +(addr == null ? "Unknown address" : addr));
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

		buf.position( bytesToWrite );

		//return the line
		return str.toString();
	}

	private void processLine(SelectionKey key, String line)
	{
		if( req == null )
		{
			req = new AsyncHttpRequest();
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
		AsyncHttpResponse res;

		try
		{
			res = new AsyncHttpResponse();
			sv.requestHandler().handleRequest(req, res);
		}
		catch(Exception e)
		{
			res = new AsyncHttpResponse();
			sv.requestHandler().generate500Page(req, res);
		}

		try
		{
			//convert the response to a bytebuffer
			ByteBuffer responseBytes = responseToByteArray(res, req.protocol());

			//write the bytes
			SocketChannel sc = (SocketChannel)key.channel();

			int bytesWritten = 0;
			while(true)
			{
				bytesWritten += sc.write(responseBytes); //TODO fix this loop so that it doesn't block the thread
				if( bytesWritten == responseBytes.limit() )
					break;
			}

			//close the connection
			close(key);
		}
		catch(IOException e)
		{
			close(key);
		}
	}

	private void close(SelectionKey key)
	{
		SocketChannel sc = (SocketChannel)key.channel();
		key.cancel();

		try
		{
			sc.close();
		}
		catch(IOException e) {}
	}

	/** Converts the HTTPResponse to a bytebuffer */
	ByteBuffer responseToByteArray(AsyncHttpResponse res, String httpProtocol) throws IOException
	{
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();

		//write the response line & headers
		{
			BufferedWriter out = new BufferedWriter( new OutputStreamWriter(byteOut) );

			//response line
			out.write( httpProtocol );
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

		//copy the response body into the buffer
		copyStream( res.body(), byteOut );

		return ByteBuffer.wrap( byteOut.toByteArray() );
	}

	private byte[] streamCopyBuf = new byte[4192];
	private void copyStream(InputStream in, OutputStream out) throws IOException
	{
		for( int bytesRead = in.read(streamCopyBuf); bytesRead != -1; bytesRead = in.read(streamCopyBuf) )
			out.write(streamCopyBuf, 0, bytesRead);
	}
}