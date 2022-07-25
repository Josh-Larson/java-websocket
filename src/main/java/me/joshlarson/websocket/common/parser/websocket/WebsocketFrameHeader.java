package me.joshlarson.websocket.common.parser.websocket;

public record WebsocketFrameHeader(
		boolean fin,
		boolean mask,
		byte opcode,
		long payloadLength,
		int maskingKey
) { }
