package me.joshlarson.websocket.server;

import me.joshlarson.websocket.common.WebSocketCallback;
import me.joshlarson.websocket.common.WebSocketHandler;
import me.joshlarson.websocket.common.parser.http.HttpRequest;
import org.jetbrains.annotations.NotNull;

public interface WebSocketServerCallback extends WebSocketCallback {
	
	default void onHttpRequest(@NotNull WebSocketHandler obj, @NotNull HttpRequest request) {}
	default void onUpgrade(@NotNull WebSocketHandler obj, @NotNull HttpRequest request) {}
	
}
