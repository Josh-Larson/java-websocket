package me.joshlarson.websocket.common.parser.websocket;

public enum WebSocketCloseReason {
	NORMAL(1000),
	GOING_AWAY (1001),
	PROTOCOL_ERROR (1002),
	UNKNOWN_DATA (1003),
	
	BAD_TEXT (1007),
	POLICY_VIOLATION (1008),
	MESSAGE_TOO_BIG (1009),
	EXPECTED_EXTENSION(1010),
	SERVER_UNEXPETED_CONDITION (1011);
	
	private final short statusCode;
	
	WebSocketCloseReason(int statusCode) {
		this.statusCode = (short) statusCode;
	}
	
	public short getStatusCode() {
		return statusCode;
	}
	
}
