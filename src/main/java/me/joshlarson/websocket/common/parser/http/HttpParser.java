package me.joshlarson.websocket.common.parser.http;

import me.joshlarson.websocket.common.parser.ParserByteStream;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class HttpParser {
	
	private final Map<String, String> headers;
	private final ParserByteStream buffer;
	
	// Transient State
	private String [] leadingLine;
	private HttpParserState state;
	
	public HttpParser() {
		this.headers = new LinkedHashMap<>();
		this.buffer = new ParserByteStream(1024);
		
		this.leadingLine = null;
		this.state = HttpParserState.LEADING_LINE;
	}
	
	public ParserByteStream getBuffer() {
		return buffer;
	}
	
	@Nullable
	public HttpFrame parseChunk(@Nullable byte [] chunk, int start, int length) throws HttpParserException {
		// Enables pipelining of parsers
		if (chunk != null)
			this.buffer.write(chunk, start, length);
		
		while (true) {
			switch (this.state) {
				case LEADING_LINE: {
					String line = readLine();
					if (line == null)
						return null;
					
					handleLeadingLine(line);
					break;
				}
				case HEADERS: {
					String line = readLine();
					if (line == null)
						return null;
					
					handleHeaderLine(line);
					break;
				}
				case DATA: return handleData();
			}
		}
	}
	
	private void resetInternalState() {
		this.headers.clear();
		this.buffer.reset();
		
		this.leadingLine = null;
		this.state = HttpParserState.LEADING_LINE;
	}
	
	private void handleLeadingLine(String leadingLine) throws HttpParserException {
		if (leadingLine == null)
			throw new HttpParserException("expected leading line");
		
		String [] parsed = leadingLine.split(" ", 3);
		if (parsed.length != 3)
			throw new HttpParserException("expected 3 parts to the leading line");
		
		if (!parsed[0].startsWith("HTTP/") && !parsed[2].startsWith("HTTP/"))
			throw new HttpParserException("expected protocol in the leading line");
		
		this.leadingLine = parsed;
		this.state = HttpParserState.HEADERS;
	}
	
	private void handleHeaderLine(String headerLine) throws HttpParserException {
		if (headerLine.isEmpty()) {
			this.state = HttpParserState.DATA;
			return;
		}
		
		String [] keyValue = headerLine.split(":", 2);
		if (keyValue.length < 2)
			throw new HttpParserException("expected colon in header line");
		
		keyValue[1] = keyValue[1].strip();
		
		Map.Entry<String, String> existingHeader = getHeader(keyValue[0]);
		if (existingHeader != null) {
			existingHeader.setValue(existingHeader.getValue() + ", " + keyValue[1]);
			return;
		}
		
		headers.put(keyValue[0], keyValue[1]);
	}
	
	@Nullable
	private HttpFrame handleData() {
		Map.Entry<String, String> contentLengthHeader = getHeader("Content-Length");
		if (leadingLine[0].startsWith("HTTP/")) {
			// Response
			if (contentLengthHeader != null) {
				int contentLength = Integer.parseInt(contentLengthHeader.getValue());
				if (buffer.getSize() < contentLength)
					return null;
				byte [] body = buffer.read(contentLength);
				int statusCode = Integer.parseInt(leadingLine[1]);
				HttpResponse ret = new HttpResponse(leadingLine[0], statusCode, leadingLine[2], new LinkedHashMap<>(headers), body);
				
				resetInternalState();
				return ret;
			}
			
			int statusCode = Integer.parseInt(leadingLine[1]);
			if ((statusCode >= 100 && statusCode < 200) || statusCode == 204 || statusCode == 304) {
				// None of these messages are allowed to include a message body
				HttpResponse ret = new HttpResponse(leadingLine[0], statusCode, leadingLine[2], new LinkedHashMap<>(headers), new byte[0]);
				
				resetInternalState();
				return ret;
			}
			
			// Can't know when this will end due to backwards compatibility with HTTP/1.0
			// TODO: Add function for manually ending the read
			return null;
		} else {
			// Request
			if (contentLengthHeader != null) {
				int contentLength = Integer.parseInt(contentLengthHeader.getValue());
				if (buffer.getSize() < contentLength)
					return null;
				
				byte [] body = buffer.read(contentLength);
				HttpRequest ret = new HttpRequest(leadingLine[0], leadingLine[1], leadingLine[2], new LinkedHashMap<>(headers), body);
				resetInternalState();
				return ret;
			}
			
			HttpRequest ret = new HttpRequest(leadingLine[0], leadingLine[1], leadingLine[2], new LinkedHashMap<>(headers), new byte[0]);
			resetInternalState();
			return ret;
		}
	}
	
	@Nullable
	private String readLine() {
		byte [] data = buffer.getByteArray();
		int size = buffer.getSize();
		
		for (int i = 1; i < size; i++) {
			if (data[i-1] == '\r' && data[i] == '\n') {
				String line = new String(data, 0, i-1, StandardCharsets.UTF_8);
				buffer.removeFromStart(i+1);
				return line;
			}
		}
		
		return null;
	}
	
	private Map.Entry<String, String> getHeader(String key) {
		for (Map.Entry<String, String> e : headers.entrySet()) {
			if (e.getKey().equalsIgnoreCase(key)) {
				return e;
			}
		}
		return null;
	}
	
	private enum HttpParserState {
		LEADING_LINE,
		HEADERS,
		DATA
	}
	
}
