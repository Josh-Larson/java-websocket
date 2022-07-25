package me.joshlarson.websocket.common;

import me.joshlarson.websocket.common.parser.http.HttpFrame;
import me.joshlarson.websocket.common.parser.websocket.WebsocketFrame;
import me.joshlarson.websocket.common.parser.websocket.WebsocketFrameType;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public record WebSocketHandler(@NotNull WebSocketProtocol protocol) {
	
	public void close() {
		protocol.sendClose();
	}
	
	public void close(int closeCode, @NotNull String closeReason) {
		protocol.sendClose(closeCode, closeReason);
	}
	
	public void sendText(String text) {
		protocol.send(new WebsocketFrame(WebsocketFrameType.TEXT, text.getBytes(StandardCharsets.UTF_8)));
	}
	
	public void sendBinary(byte[] data) {
		protocol.send(new WebsocketFrame(WebsocketFrameType.BINARY, data));
	}
	
	public void sendPing(byte[] data) {
		protocol.send(new WebsocketFrame(WebsocketFrameType.PING, data));
	}
	
	public void sendPong(byte[] data) {
		protocol.send(new WebsocketFrame(WebsocketFrameType.PONG, data));
	}
	
	public void sendHttpFrame(HttpFrame frame) {
		protocol.send(frame);
	}
	
}
