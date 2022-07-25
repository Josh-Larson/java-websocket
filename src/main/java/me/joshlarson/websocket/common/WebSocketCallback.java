package me.joshlarson.websocket.common;


import org.jetbrains.annotations.NotNull;

public interface WebSocketCallback {
	
	default void onConnect(@NotNull WebSocketHandler obj) {}
	
	default void onDisconnect(@NotNull WebSocketHandler obj, int closeCode, @NotNull String reason) {}
	
	default void onTextMessage(@NotNull WebSocketHandler obj, @NotNull String text) {}
	
	default void onBinaryMessage(@NotNull WebSocketHandler obj, @NotNull byte [] data) {}
	
	default void onPing(@NotNull WebSocketHandler obj, @NotNull byte [] data) {}
	
	default void onPong(@NotNull WebSocketHandler obj, @NotNull byte [] data) {}
	
}
