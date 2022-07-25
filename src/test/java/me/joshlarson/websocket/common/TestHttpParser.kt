package me.joshlarson.websocket.common

import me.joshlarson.websocket.common.parser.http.HttpFrame
import me.joshlarson.websocket.common.parser.http.HttpParser
import me.joshlarson.websocket.common.parser.http.HttpRequest
import me.joshlarson.websocket.common.parser.http.HttpResponse
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestHttpParser {
	
	@Test
	fun testHeaderRequestParsing() {
		val request: HttpRequest = parseHttp("""
			GET /chat HTTP/1.1
			Host: server.example.com
			Upgrade: websocket
			Connection: Upgrade
			Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
			Origin: http://example.com
			Sec-WebSocket-Protocol: chat, superchat
			Sec-WebSocket-Version: 13
			
			
			""") as HttpRequest
		
		assertEquals("GET", request.method)
		assertEquals("/chat", request.path)
		assertEquals("HTTP/1.1", request.version)
		
		assertEquals(mapOf(
			"Host" to "server.example.com",
			"Upgrade" to "websocket",
			"Connection" to "Upgrade",
			"Sec-WebSocket-Key" to "dGhlIHNhbXBsZSBub25jZQ==",
			"Origin" to "http://example.com",
			"Sec-WebSocket-Protocol" to "chat, superchat",
			"Sec-WebSocket-Version" to "13"
		), request.headers)
		assertEquals("dGhlIHNhbXBsZSBub25jZQ==", request.getHeaderValue("sec-websocket-key"))
		
		assertEquals(0, request.body.size)
	}
	
	@Test
	fun testHeaderRequestWithDataParsing() {
		val request: HttpRequest = parseHttp("""
			POST /test HTTP/1.1
			Host: foo.example
			Content-Type: application/x-www-form-urlencoded
			Content-Length: 27
			
			field1=value1&field2=value2""") as HttpRequest
		
		assertEquals("POST", request.method)
		assertEquals("/test", request.path)
		assertEquals("HTTP/1.1", request.version)
		
		assertEquals(mapOf(
			"Host" to "foo.example",
			"Content-Type" to "application/x-www-form-urlencoded",
			"Content-Length" to "27",
		), request.headers)
		assertEquals("27", request.getHeaderValue("content-length"))
		
		assertArrayEquals("field1=value1&field2=value2".encodeToByteArray(), request.body)
	}
	
	@Test
	fun testHeaderResponseParsing() {
		val response: HttpResponse = parseHttp("""
			HTTP/1.1 101 Switching Protocols
			Upgrade: websocket
			Connection: Upgrade
			Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
			
			
			""") as HttpResponse
		
		assertEquals("HTTP/1.1", response.version)
		assertEquals(101, response.statusCode)
		assertEquals("Switching Protocols", response.statusMessage)
		
		assertEquals(mapOf(
			"Upgrade" to "websocket",
			"Connection" to "Upgrade",
			"Sec-WebSocket-Accept" to "s3pPLMBiTxaQ9kYGzzhZRbK+xOo=",
		), response.headers)
		assertEquals("s3pPLMBiTxaQ9kYGzzhZRbK+xOo=", response.getHeaderValue("sec-websocket-accept"))
		
		assertEquals(0, response.body.size)
	}
	
	private fun parseHttp(message: String): HttpFrame? {
		val messageEncoded = message
			.trimIndent()
			.replace("\r", "")
			.replace("\n", "\r\n")
			.encodeToByteArray()
		
		val parser = HttpParser()
		for (i in 1..3) {
			val parsed = parser.parseChunk(messageEncoded, 0, messageEncoded.size)
			
			if (parsed != null)
				assertArrayEquals(messageEncoded, parsed.encode())
		}
		val parsed = parser.parseChunk(messageEncoded, 0, messageEncoded.size)
		
		if (parsed != null)
			assertArrayEquals(messageEncoded, parsed.encode())
		
		return parsed
	}
}
