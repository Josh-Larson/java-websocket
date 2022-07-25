package me.joshlarson.websocket.common;

import me.joshlarson.websocket.common.parser.ParserByteStream;
import me.joshlarson.websocket.common.parser.http.*;
import me.joshlarson.websocket.common.parser.websocket.*;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public abstract class WebSocketProtocol {
	
	private static final ReentrantLock DIGEST_LOCK = new ReentrantLock();
	private static MessageDigest SHA1_DIGEST;
	
	static {
		try {
			SHA1_DIGEST = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	private final Consumer<byte []> writer;
	private final Runnable closer;
	private final HttpParser httpParser;
	private final WebsocketParser websocketParser;
	
	private final WebSocketCallback callback;
	private final WebSocketHandler handler;
	
	private boolean websocketMode;
	private boolean closing;
	
	public WebSocketProtocol(WebSocketCallback callback, Consumer<byte []> writer, Runnable closer) {
		this.httpParser = new HttpParser();
		this.websocketParser = new WebsocketParser();
		this.writer = writer;
		this.closer = closer;
		this.callback = callback;
		this.handler = new WebSocketHandler(this);
		
		this.websocketMode = false;
		this.closing = false;
	}
	
	public void onConnect() {
		callback.onConnect(handler);
	}
	
	public void onDisconnect() {
		// A possibly atypical close method - ideally this happens via websocket packets first
		if (!closing)
			callback.onDisconnect(handler, 1006, "");
		closing = true;
	}
	
	public void onRead(byte [] data, int start, int length) {
		if (this.websocketMode) {
			try {
				onWebsocketRead(data, start, length);
			} catch (WebsocketParserException e) {
				socketClose();
			}
		} else {
			try {
				onHttpRead(data, start, length);
			} catch (HttpParserException e) {
				socketClose();
			}
		}
	}
	
	public void send(WebsocketFrame frame) {
		if (!websocketMode)
			throw new IllegalStateException("cannot send websocket frame in HTTP mode");
		boolean nowClosing = !closing && (frame.type() == WebsocketFrameType.CLOSE);
		if (nowClosing)
			closing = true;
		
		this.writer.accept(frame.encode());
		
		if (nowClosing) {
			int closeCode = getWebSocketCloseCode(frame.data());
			String closeReason = getWebSocketCloseReason(frame.data());
			callback.onDisconnect(handler, closeCode, closeReason);
		}
	}
	
	public void send(HttpFrame frame) {
		if (websocketMode)
			throw new IllegalStateException("cannot send HTTP frame in websocket mode");
		this.writer.accept(frame.encode());
	}
	
	public void sendClose() {
		sendClose(WebSocketCloseReason.NORMAL.getStatusCode(), "");
	}
	
	public void sendClose(int closeCode, String closeReason) {
		byte [] closeReasonEncoded = closeReason.getBytes(StandardCharsets.UTF_8);
		byte [] closeData = new byte[2 + closeReasonEncoded.length];
		closeData[0] = (byte) ((closeCode >> 8) & 0xFF);
		closeData[1] = (byte) (closeCode & 0xFF);
		System.arraycopy(closeReasonEncoded, 0, closeData, 2, closeReasonEncoded.length);
		
		send(new WebsocketFrame(WebsocketFrameType.CLOSE, closeData));
	}
	
	protected abstract void onWebsocketFrame(WebsocketFrame frame);
	protected abstract void onHttpFrame(HttpFrame frame);
	
	protected void socketClose() {
		this.closer.run();
	}
	
	protected WebSocketHandler getHandler() {
		return handler;
	}
	
	protected void switchToWebsocket() {
		ParserByteStream buffer = httpParser.getBuffer();
		this.websocketMode = true;
		onRead(buffer.getByteArray(), 0, buffer.getSize());
	}
	
	private void onWebsocketRead(byte [] data, int start, int length) throws WebsocketParserException {
		while (true) {
			WebsocketFrame frame = websocketParser.parseChunk(data, start, length);
			data = null;
			start = 0;
			length = 0;
			if (frame == null)
				break;
			
			switch (frame.type()) {
				case TEXT -> callback.onTextMessage(handler, new String(frame.data(), StandardCharsets.UTF_8));
				case BINARY -> callback.onBinaryMessage(handler, frame.data());
				case PING -> {
					send(new WebsocketFrame(WebsocketFrameType.PONG, frame.data()));
					callback.onPing(handler, frame.data());
				}
				case PONG -> callback.onPong(handler, frame.data());
				case CLOSE -> {
					if (!this.closing) {
						send(new WebsocketFrame(WebsocketFrameType.CLOSE, frame.data()));
					} else {
						socketClose();
					}
				}
			}
			
			onWebsocketFrame(frame);
		}
	}
	
	private void onHttpRead(byte [] data, int start, int length) throws HttpParserException {
		while (true) {
			HttpFrame frame = httpParser.parseChunk(data, start, length);
			data = null;
			start = 0;
			length = 0;
			if (frame == null)
				break;
			
			onHttpFrame(frame);
		}
	}
	
	protected static String getWebSocketAcceptString(@NotNull String websocketKey) {
		byte [] acceptPreDigest = (websocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8);
		byte [] acceptDigest;
		DIGEST_LOCK.lock();
		try {
			SHA1_DIGEST.reset();
			acceptDigest = SHA1_DIGEST.digest(acceptPreDigest);
		} finally {
			DIGEST_LOCK.unlock();
		}
		return Base64.getEncoder().encodeToString(acceptDigest);
	}
	
	private static int getWebSocketCloseCode(byte [] closeData) {
		if (closeData.length < 2)
			return 1005;
		
		return ((closeData[0] & 0xFF) << 8) | (closeData[1] & 0xFF);
	}
	
	private static String getWebSocketCloseReason(byte [] closeData) {
		if (closeData.length <= 2)
			return "";
		
		return new String(closeData, 2, closeData.length-2, StandardCharsets.UTF_8);
	}
	
}
