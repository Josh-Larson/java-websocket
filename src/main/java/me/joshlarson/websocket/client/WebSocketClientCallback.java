package me.joshlarson.websocket.client;


import me.joshlarson.websocket.common.WebSocketCallback;
import me.joshlarson.websocket.common.WebSocketHandler;
import me.joshlarson.websocket.common.parser.http.HttpResponse;
import org.jetbrains.annotations.NotNull;

public interface WebSocketClientCallback extends WebSocketCallback {
	
	default void onUpgrade(@NotNull WebSocketHandler obj, @NotNull HttpResponse response) {}
	
}
