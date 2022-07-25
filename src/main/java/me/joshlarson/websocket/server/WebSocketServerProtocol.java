package me.joshlarson.websocket.server;

import me.joshlarson.websocket.common.WebSocketProtocol;
import me.joshlarson.websocket.common.parser.http.HttpFrame;
import me.joshlarson.websocket.common.parser.http.HttpRequest;
import me.joshlarson.websocket.common.parser.http.HttpResponse;
import me.joshlarson.websocket.common.parser.websocket.WebsocketFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class WebSocketServerProtocol extends WebSocketProtocol {
	
	private final WebSocketServerCallback callback;
	
	public WebSocketServerProtocol(WebSocketServerCallback callback, Consumer<byte []> writer, Runnable closer) {
		super(callback, writer, closer);
		this.callback = callback;
	}
	
	@Override
	protected void onWebsocketFrame(WebsocketFrame frame) {
	
	}
	
	@Override
	protected void onHttpFrame(HttpFrame frame) {
		if (!(frame instanceof HttpRequest request)) {
			// ?!?!?!
			send(new HttpResponse("HTTP/1.1", 400, "Invalid Request", new HashMap<>(), new byte[0]));
			socketClose();
			return;
		}
		
		String connection = request.getHeaderValue("Connection");
		String upgrade = request.getHeaderValue("Upgrade");
		if (connection == null || "Upgrade".compareToIgnoreCase(connection) != 0 || upgrade == null || "websocket".compareToIgnoreCase(upgrade) != 0) {
			// If we're not upgrading to websockets, treat this as some other kind of HTTP request
			callback.onHttpRequest(getHandler(), request);
			return;
		}
		
		String websocketKey = request.getHeaderValue("Sec-WebSocket-Key");
		if (websocketKey == null) {
			send(new HttpResponse("HTTP/1.1", 400, "Invalid Request", new HashMap<>(), new byte[0]));
			socketClose();
			return;
		}
		
		Map<String, String> responseHeaders = new HashMap<>();
		responseHeaders.put("Upgrade", "websocket");
		responseHeaders.put("Connection", "Upgrade");
		responseHeaders.put("Sec-WebSocket-Accept", getWebSocketAcceptString(websocketKey));
		responseHeaders.put("Sec-WebSocket-Protocol", "");
		send(new HttpResponse("HTTP/1.1", 101, "Switching Protocols", responseHeaders, new byte[0]));
		
		switchToWebsocket();
		callback.onUpgrade(getHandler(), request);
	}
	
}
