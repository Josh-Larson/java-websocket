module me.joshlarson.websocket {
	requires kotlin.stdlib;
	requires org.jetbrains.annotations;
	
	exports me.joshlarson.websocket.client;
	exports me.joshlarson.websocket.common;
	exports me.joshlarson.websocket.server;
	
	exports me.joshlarson.websocket.common.parser.http;
	exports me.joshlarson.websocket.common.parser.websocket;
}