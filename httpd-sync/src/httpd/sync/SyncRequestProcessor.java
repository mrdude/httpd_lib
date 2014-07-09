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

import java.io.*;
import java.net.Socket;
import java.util.Iterator;
import java.util.Map;

class SyncRequestProcessor extends Thread
{
	private final byte[] buf = new byte[ 1024 * 4 ]; //the buffer used for copying streams
	private final SyncHttpServer serv;

	SyncRequestProcessor(SyncHttpServer serv, int id)
	{
		this.serv = serv;
		setName("RequestProcessor " +id);
		setDaemon(true);
		start();
	}

	public void run()
	{
		while( !serv.quitFlag() )
		{
			try( Socket s = serv.accept() )
			{
				if( s != null )
				{
					InputStream in = s.getInputStream();
					OutputStream out = s.getOutputStream();
					handleSocket(s, in, out);
				}
			}
			catch(IOException e) {}
		}
	}

	private void handleSocket(Socket s, InputStream in, OutputStream out) throws IOException
	{
		SyncHttpRequest req = new SyncHttpRequest();

		//read the initial request line and headers
		BufferedReader br = new BufferedReader( new InputStreamReader(in) );
		req.processInitialRequest( br.readLine() ); //TODO handle invalid request headers

		while(true)
		{
			String line = br.readLine();
			if( line.equals("") )
				break;

			req.processHeader( line );
		}

		//lock the request and pass it to the RequestHandler
		req.lock();
		System.out.println( "New Request: " +getName()+ ": " +req.toString()+ ":" +s.getRemoteSocketAddress() );
		SyncHttpResponse res = new SyncHttpResponse();

		try
		{
			serv.requestHandler().handleRequest(req, res);
		}
		catch(Exception e)
		{
			System.out.println("Caught an exception:" +getName()+ ":" +e);
			res = new SyncHttpResponse();
			serv.requestHandler().generate500Page(req, res);
		}

		//send the response
		byte[] b = responseToByteArray(res, req.protocol());
		res.closeBodyStream();
		out.write(b);
		out.flush();
	}

	/** Converts the HTTPResponse to a byte array */
	byte[] responseToByteArray(SyncHttpResponse res, String httpProtocol) throws IOException
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

		return byteOut.toByteArray();
	}

	private void copyStream(InputStream in, OutputStream out) throws IOException
	{
		for( int bytesRead = in.read(buf); bytesRead != -1; bytesRead = in.read(buf) )
			out.write(buf, 0, bytesRead);
	}
}