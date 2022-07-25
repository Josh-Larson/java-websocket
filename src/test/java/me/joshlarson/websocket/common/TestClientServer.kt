package me.joshlarson.websocket.common

import me.joshlarson.websocket.client.WebSocketClientCallback
import me.joshlarson.websocket.client.WebSocketClientProtocol
import me.joshlarson.websocket.common.parser.http.HttpRequest
import me.joshlarson.websocket.common.parser.http.HttpResponse
import me.joshlarson.websocket.common.parser.websocket.WebSocketCloseReason
import me.joshlarson.websocket.server.WebSocketServerCallback
import me.joshlarson.websocket.server.WebSocketServerProtocol
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.Pipe
import java.util.concurrent.atomic.AtomicBoolean

class TestClientServer {
	
	@Test
	fun testConnection() {
		val success = AtomicBoolean(false)
		
		val clientCallback = object : WebSocketClientCallback {
			override fun onUpgrade(obj: WebSocketHandler, response: HttpResponse) {
				obj.sendText("Hello Server!")
			}
		}
		
		val serverCallback = object : WebSocketServerCallback {
			override fun onTextMessage(obj: WebSocketHandler, text: String) {
				assertEquals("Hello Server!", text)
				success.set(true)
				obj.close()
			}
		}
		
		handleClientServerCommunication(clientCallback, serverCallback)
		assertTrue(success.get())
	}
	
	@Test
	fun testBasicHttpResponse() {
		val success = AtomicBoolean(false)
		
		val clientCallback = object : WebSocketClientCallback {
			override fun onConnect(obj: WebSocketHandler) {
				obj.sendHttpFrame(HttpRequest("GET", "/", "HTTP/1.1", mapOf(), ByteArray(0)))
			}
		}
		
		val serverCallback = object : WebSocketServerCallback {
			override fun onHttpRequest(obj: WebSocketHandler, request: HttpRequest) {
				assertEquals("GET", request.method)
				assertEquals("/", request.path)
				assertEquals("HTTP/1.1", request.version)
				success.set(true)
			}
			
			override fun onUpgrade(obj: WebSocketHandler, request: HttpRequest) {
				obj.close()
			}
		}
		
		handleClientServerCommunication(clientCallback, serverCallback)
		assertTrue(success.get())
	}
	
	@Test
	fun testTextBinaryPingPongClose() {
		val success = AtomicBoolean(false)
		
		val clientCallback = object : WebSocketClientCallback {
			override fun onUpgrade(obj: WebSocketHandler, response: HttpResponse) {
				obj.sendText("Hello Server!")
			}
			
			override fun onBinaryMessage(obj: WebSocketHandler, data: ByteArray) {
				assertArrayEquals(ByteArray(1024), data)
				obj.sendPing(ByteArray(16)) // Should be returned as PONG automatically
			}
			
			override fun onPong(obj: WebSocketHandler, data: ByteArray) {
				assertArrayEquals(ByteArray(16), data)
				obj.close(WebSocketCloseReason.GOING_AWAY.statusCode.toInt(), "No Reason")
			}
		}
		
		val serverCallback = object : WebSocketServerCallback {
			override fun onTextMessage(obj: WebSocketHandler, text: String) {
				assertEquals("Hello Server!", text)
				obj.sendBinary(ByteArray(1024))
			}
			
			override fun onDisconnect(obj: WebSocketHandler, closeCode: Int, reason: String) {
				assertEquals(WebSocketCloseReason.GOING_AWAY.statusCode.toInt(), closeCode)
				assertEquals("No Reason", reason)
				success.set(true)
			}
		}
		
		handleClientServerCommunication(clientCallback, serverCallback)
		assertTrue(success.get())
	}
	
	private fun handleClientServerCommunication(clientCallback: WebSocketClientCallback, serverCallback: WebSocketServerCallback) {
		val clientToServerPipe = Pipe.open()
		val serverToClientPipe = Pipe.open()
		val stopRequest = AtomicBoolean(false)
		val client = WebSocketClientProtocol(clientCallback, "/", buildOutputStream(clientToServerPipe)) { stopRequest.set(true) }
		val server = WebSocketServerProtocol(serverCallback, buildOutputStream(serverToClientPipe)) { stopRequest.set(true) }
		
		val clientThread = Thread(createClientServerTestRunnable(client, serverToClientPipe, stopRequest))
		val serverThread = Thread(createClientServerTestRunnable(server, clientToServerPipe, stopRequest))
		
		serverThread.start()
		clientThread.start()
		
		while (!stopRequest.get()) {
			Thread.sleep(10)
		}
		
		clientThread.interrupt()
		serverThread.interrupt()
		
		clientThread.join()
		serverThread.join()
	}
	
	private fun createClientServerTestRunnable(protocol: WebSocketProtocol, pipe: Pipe, stopRequest: AtomicBoolean): () -> Unit {
		return {
			val data = ByteArray(1024)
			val inputStream = buildInputStream(pipe)
			try {
				protocol.onConnect()
				while (!stopRequest.get()) {
					val n: Int
					try {
						n = inputStream(data)
						protocol.onRead(data, 0, n)
					} catch (e: ClosedByInterruptException) {
						break
					}
				}
				protocol.onDisconnect()
			} finally {
				stopRequest.set(true)
			}
		}
	}
	
	private fun buildInputStream(pipe: Pipe): (ByteArray) -> Int {
		return { data: ByteArray -> pipe.source().read(ByteBuffer.wrap(data)) }
	}
	
	private fun buildOutputStream(pipe: Pipe): (ByteArray) -> Unit {
		return { data: ByteArray -> pipe.sink().write(ByteBuffer.wrap(data)); Unit }
	}
	
}