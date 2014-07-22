package httpd.common;

import com.google.common.collect.ImmutableMap;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;

public class Parsers
{
	public static class InitialRequestLine
	{
		public String method;
		public String request;
		public String protocol;
		public ImmutableMap<String, String> getParameters;
	}

	/**
	 * Processes the initial request line.
	 * @return an InitialRequestLine object if successful, or null if the request line was malformed
	 */
	public static InitialRequestLine processInitialRequestLine(String line)
	{
		int firstSpace = line.indexOf(" ");
		int lastSpace = line.lastIndexOf( " " );

		if( firstSpace == -1 || lastSpace == -1 )
			return null;

		String[] split = {
			line.substring( 0, firstSpace),
			line.substring( firstSpace+1, lastSpace ),
			line.substring( lastSpace+1 )
		};

		InitialRequestLine req = new InitialRequestLine();

		req.method = split[0];
		req.protocol = split[2];

		try
		{
			int queryIndex = split[1].indexOf( "?" );
			if( queryIndex == -1 )
			{
				req.request = URLDecoder.decode( split[1], "UTF-8" );
				req.getParameters = ImmutableMap.of();
			}
			else
			{
				req.request = URLDecoder.decode( split[1].substring(0, queryIndex), "UTF-8" );
				req.getParameters = parseGetParameters( split[1].substring( queryIndex+1 ) );
			}
		}
		catch( UnsupportedEncodingException e )
		{
			//This should never really be thrown -- UTF-8 is guaranteed to be supported
			return null;
		}

		return req;
	}

	private static ImmutableMap<String,String> parseGetParameters(String getParameterString)
		throws UnsupportedEncodingException
	{
		HashMap<String, String> map = new HashMap<>();

		for( String param : getParameterString.split("&") )
		{
			String split[] = param.split("=", 2);

			String key = split[0];
			String value = split.length >= 2 ? split[1] : "";

			key = URLDecoder.decode( key, "UTF-8" );
			value = URLDecoder.decode( value, "UTF-8" );

			map.put(key, value);
		}

		return ImmutableMap.copyOf(map);
	}
}
