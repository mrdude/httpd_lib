What is this? (Five Second Overview)
------------------------------------

* An embeddable HTTP server
* Lightweight, reasonably performant
* MIT License -- use it where you want, how you want
* Uses a custom API (instead of the official Java HTTP API)
* Uses SLF4J for logging
* Supports keep-alive connections
* Easy to embed and start using
* Is *not* completely HTTP/1.1 compliant
* Only 85% complete, but 100% usable

How Do I Use It?
----------------

See the samples. The QuickAndDirty sample should give you a brief taste of the capabilities of the lib, and
BasicWebServer will let you serve files from disk.

Does it support PHP/Ruby on Rails/Perl/Python for server-side scripting?
------------------------------------------------------------------------

No, nor will it ever.


The point of this library is to provide a really simple, reasonably performant, embedded HTTP server. All of the
above falls outside the (admittedly small) scope of this lib. If you really need that stuff, you should be looking at
something else.

Does it support FastCGI?
------------------------

No, for the same reasons listed above.

Can I use it in a production environment?
-----------------------------------------

Ehh....I wouldn't recommend it. This isn't really meant to be an outward facing web server, and I have no idea how
well it scales. So while it may be OK for your small business website (emphasis on *small*), it shouldn't be running
your Alexa Top 500 site that gets 1.5 gazillion hits a second.


That being said, if you *do* end up using it for something cool like that, I'd love to hear about your experiences.

What are SyncHttpServer and AsyncHttpServer?
--------------------------------------------

They are old, deprecated implementations of the HTTP server part of the library. *Use AsyncHttpServer2 instead!*
SyncHttpServer spawns a separate thread for each client connection, and AsyncHttpServer uses a
single thread and asynchronous I/O. Neither implementation supports deferred writes.

TODO
----

* Finish up AsyncHttpServer2
	* Server doesn't clean up (close Selector, ServerSockets, etc.) after itself on shutdown
	* Server doesn't drop connections that take too long (so it is vulnerable to BEAST attacks)
* Some more comments in the code might be nice
* Add more samples
* Finish Javadoc documentation