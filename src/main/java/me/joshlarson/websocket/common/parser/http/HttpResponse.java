package me.joshlarson.websocket.common.parser.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public record HttpResponse(
		String version,
		int statusCode,
		String statusMessage,
		Map<String, String> headers,
		byte [] body
) implements HttpFrame {
	
	@Override
	@Nullable
	public String getHeaderValue(String key) {
		for (Map.Entry<String, String> e : headers.entrySet()) {
			if (e.getKey().equalsIgnoreCase(key))
				return e.getValue();
		}
		return null;
	}
	
	@Override
	@NotNull
	public byte [] encode() {
		StringBuilder headerDataString = new StringBuilder(version);
		headerDataString.append(' ');
		headerDataString.append(statusCode);
		headerDataString.append(' ');
		headerDataString.append(statusMessage);
		headerDataString.append("\r\n");
		
		for (Map.Entry<String, String> e : headers.entrySet()) {
			headerDataString.append(e.getKey());
			headerDataString.append(": ");
			headerDataString.append(e.getValue());
			headerDataString.append("\r\n");
		}
		headerDataString.append("\r\n");
		
		byte [] headerData = headerDataString.toString().getBytes(StandardCharsets.UTF_8);
		if (body.length == 0)
			return headerData;
		
		byte [] encoded = new byte[headerData.length + body.length];
		System.arraycopy(headerData, 0, encoded, 0, headerData.length);
		System.arraycopy(body, 0, encoded, headerData.length, body.length);
		return encoded;
	}
	
}
