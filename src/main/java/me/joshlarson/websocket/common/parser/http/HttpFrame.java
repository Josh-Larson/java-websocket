package me.joshlarson.websocket.common.parser.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HttpFrame {
	
	@Nullable
	String getHeaderValue(String key);
	@NotNull
	byte [] encode();
	
}
