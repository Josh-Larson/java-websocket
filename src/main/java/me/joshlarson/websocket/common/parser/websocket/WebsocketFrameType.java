package me.joshlarson.websocket.common.parser.websocket;

public enum WebsocketFrameType {
	CONTINUATION((byte) 0x0),
	TEXT((byte) 0x1),
	BINARY((byte) 0x2),
	CLOSE((byte) 0x8),
	PING((byte) 0x9),
	PONG((byte) 0xA),
	UNKNOWN((byte) 0xFF);
	
	private final byte opcode;
	
	WebsocketFrameType(byte opcode) {
		this.opcode = opcode;
	}
	
	public byte getOpcode() {
		return opcode;
	}
	
	public static WebsocketFrameType getTypeForOpcode(byte opcode) {
		return switch (opcode) {
			case 0x0 -> CONTINUATION;
			case 0x1 -> TEXT;
			case 0x2 -> BINARY;
			case 0x8 -> CLOSE;
			case 0x9 -> PING;
			case 0xA -> PONG;
			default  -> UNKNOWN;
		};
	}
}
