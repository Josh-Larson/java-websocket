package me.joshlarson.websocket.client;

import me.joshlarson.websocket.common.WebSocketProtocol;
import me.joshlarson.websocket.common.parser.http.HttpFrame;
import me.joshlarson.websocket.common.parser.http.HttpRequest;
import me.joshlarson.websocket.common.parser.http.HttpResponse;
import me.joshlarson.websocket.common.parser.websocket.WebsocketFrame;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class WebSocketClientProtocol extends WebSocketProtocol {
	
	private final WebSocketClientCallback callback;
	private final String websocketRequestKey;
	private final String url;
	private final String host;
	
	public WebSocketClientProtocol(@NotNull WebSocketClientCallback callback, @NotNull String url, @Nullable String host, @NotNull Consumer<byte []> writer, @NotNull Runnable closer) {
		super(callback, writer, closer);
		this.callback = callback;
		this.url = url;
		this.host = host;
		
		// Generate this session's random key
		byte [] websocketRequestKeyRaw = new byte[16];
		new SecureRandom().nextBytes(websocketRequestKeyRaw);
		this.websocketRequestKey = Base64.getEncoder().encodeToString(websocketRequestKeyRaw);
	}
	
	public WebSocketClientProtocol(@NotNull WebSocketClientCallback callback, @NotNull String url, @NotNull Consumer<byte []> writer, @NotNull Runnable closer) {
		this(callback, url, null, writer, closer);
	}
	
	@Override
	public void onConnect() {
		super.onConnect();
		
		Map<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put("Upgrade", "websocket");
		requestHeaders.put("Connection", "Upgrade");
		if (host != null)
			requestHeaders.put("Host", host);
		requestHeaders.put("Sec-WebSocket-Key", websocketRequestKey);
		requestHeaders.put("Sec-WebSocket-Protocol", "");
		requestHeaders.put("Sec-WebSocket-Version", "13");
		send(new HttpRequest("GET", url, "HTTP/1.1", requestHeaders, new byte[0]));
	}
	
	@Override
	protected void onWebsocketFrame(WebsocketFrame frame) {
	
	}
	
	@Override
	protected void onHttpFrame(HttpFrame frame) {
		if (!(frame instanceof HttpResponse response)) {
			// ?!?!?!
			socketClose();
			return;
		}
		
		if (response.statusCode() != 101) {
			socketClose();
			return;
		}
		
		String expectedAcceptKey = getWebSocketAcceptString(websocketRequestKey);
		if (!expectedAcceptKey.equals(response.getHeaderValue("Sec-WebSocket-Accept"))) {
			socketClose();
			return;
		}
		
		switchToWebsocket();
		callback.onUpgrade(getHandler(), response);
	}
	
}
