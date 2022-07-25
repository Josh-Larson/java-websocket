package me.joshlarson.websocket.common.parser.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record HttpRequest(
		String method,
		String path,
		String version,
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
	
	@NotNull
	public Map<String, List<String>> getUrlParameters() {
		final Charset decodeCharset = StandardCharsets.UTF_8;
		Map<String, List<String>> urlParameters = new HashMap<>();
		
		int parameterStartIndex = path.indexOf('?');
		if (parameterStartIndex == -1)
			return urlParameters;
		
		for (String parameterPair : path.substring(parameterStartIndex+1).split("&")) {
			int parameterSplitIndex = parameterPair.indexOf('=');
			String key, value;
			if (parameterSplitIndex == -1) {
				key = parameterPair;
				value = null;
			} else {
				key = URLDecoder.decode(parameterPair.substring(0, parameterSplitIndex), decodeCharset);
				value = URLDecoder.decode(parameterPair.substring(parameterSplitIndex+1), decodeCharset);
			}
			if (!urlParameters.containsKey(key))
				urlParameters.put(key, new ArrayList<>());
			urlParameters.get(key).add(value);
		}
		
		return urlParameters;
	}
	
	@Override
	@NotNull
	public byte[] encode() {
		StringBuilder headerDataString = new StringBuilder(method);
		headerDataString.append(' ');
		headerDataString.append(path);
		headerDataString.append(' ');
		headerDataString.append(version);
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
