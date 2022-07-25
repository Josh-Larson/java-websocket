package me.joshlarson.websocket.common

import me.joshlarson.websocket.common.parser.websocket.WebsocketFrame
import me.joshlarson.websocket.common.parser.websocket.WebsocketFrameType
import me.joshlarson.websocket.common.parser.websocket.WebsocketParser
import me.joshlarson.websocket.common.parser.websocket.WebsocketParserException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor
import kotlin.random.Random

class TestWebsocketParser {
	
	@Test
	fun testControlMessages() {
		for (opcode in 8..10) {
			val data = Random.nextBytes(8)
			val packet = buildPacket(fin=true, mask=true, opcode=opcode.toByte(), maskingKey=0x01020304, data=data)
			val parser = WebsocketParser()
			val finalFrame = feedChunks(parser, packet, 4096)
			assertNotNull(finalFrame)
			
			assertArrayEquals(data, finalFrame?.data)
			assertEquals(WebsocketFrameType.getTypeForOpcode(opcode.toByte()), finalFrame?.type)
		}
	}
	
	@Test
	fun testLongControlMessages() {
		for (opcode in 8..10) {
			val data = Random.nextBytes(126)
			val packet = buildPacket(fin=true, mask=true, opcode=opcode.toByte(), maskingKey=0x01020304, data=data)
			val parser = WebsocketParser()
			assertThrows(WebsocketParserException::class.java) { feedChunks(parser, packet, 4096) }
		}
	}
	
	@Test
	fun testFragmentedControlMessages() {
		for (opcode in 8..10) {
			val data = Random.nextBytes(8)
			val packet = buildPacket(fin=false, mask=true, opcode=opcode.toByte(), maskingKey=0x01020304, data=data)
			val parser = WebsocketParser()
			assertThrows(WebsocketParserException::class.java) { feedChunks(parser, packet, 4096) }
		}
	}
	
	@Test
	fun testSimpleData() {
		val data = Random.nextBytes(8)
		val packet = buildPacket(fin=true, mask=false, opcode=WebsocketFrameType.BINARY.opcode, maskingKey=0, data=data)
		val parser = WebsocketParser()
		val finalFrame = feedChunks(parser, packet, 4096)
		assertNotNull(finalFrame)
		
		assertArrayEquals(data, finalFrame?.data)
		assertEquals(WebsocketFrameType.getTypeForOpcode(WebsocketFrameType.BINARY.opcode), finalFrame?.type)
	}
	
	@Test
	fun testShortData() {
		val data = Random.nextBytes(60000)
		val packet = buildPacket(fin=true, mask=false, opcode=WebsocketFrameType.BINARY.opcode, maskingKey=0, data=data)
		val parser = WebsocketParser()
		val finalFrame = feedChunks(parser, packet, 4096)
		assertNotNull(finalFrame)
		
		assertArrayEquals(data, finalFrame?.data)
		assertEquals(WebsocketFrameType.getTypeForOpcode(WebsocketFrameType.BINARY.opcode), finalFrame?.type)
	}
	
	@Test
	fun testLongData() {
		val data = Random.nextBytes(1024*1024)
		val packet = buildPacket(fin=true, mask=false, opcode=WebsocketFrameType.BINARY.opcode, maskingKey=0, data=data)
		val parser = WebsocketParser()
		val finalFrame = feedChunks(parser, packet, 4096)
		assertNotNull(finalFrame)
		
		assertArrayEquals(data, finalFrame?.data)
		assertEquals(WebsocketFrameType.getTypeForOpcode(WebsocketFrameType.BINARY.opcode), finalFrame?.type)
	}
	
	@Test
	fun testLongDataMasked() {
		val data = Random.nextBytes(1024*1024)
		val packet = buildPacket(fin=true, mask=true, opcode=WebsocketFrameType.BINARY.opcode, maskingKey=0x7FFFFFFF, data=data)
		val parser = WebsocketParser()
		val finalFrame = feedChunks(parser, packet, 4096)
		assertNotNull(finalFrame)
		
		assertArrayEquals(data, finalFrame?.data)
		assertEquals(WebsocketFrameType.getTypeForOpcode(WebsocketFrameType.BINARY.opcode), finalFrame?.type)
	}
	
	@Test
	fun testUnevenDataMasked() {
		for (dataLength in 4..8) {
			val data = Random.nextBytes(dataLength)
			val packet = buildPacket(fin=true, mask=true, opcode=WebsocketFrameType.BINARY.opcode, maskingKey=0x7FFFFFFF, data=data)
			val parser = WebsocketParser()
			val finalFrame = feedChunks(parser, packet, 4096)
			assertNotNull(finalFrame)
			
			assertArrayEquals(data, finalFrame?.data)
			assertEquals(WebsocketFrameType.getTypeForOpcode(WebsocketFrameType.BINARY.opcode), finalFrame?.type)
		}
	}
	
//	@Test
//	fun testGigabyteUnfragmentedData() {
//		val data = Random.nextBytes(1024)
//		val packet = buildPacket(fin=true, mask=false, opcode=WebsocketFrameType.BINARY.opcode, maskingKey=0, data=data)
//
//		val parser = WebsocketParser()
//		val start = System.nanoTime()
//		for (i in 0 until 1024*1024) {
//			val finalFrame = feedChunks(parser, packet, 4096)
//			assertNotNull(finalFrame)
//		}
//		val end = System.nanoTime()
//		val finalFrame = feedChunks(parser, packet, 4096)
//		assertNotNull(finalFrame)
//		println((1 / ((end - start) / 1e9)).toString() + "GB/s")
//
//		assertArrayEquals(data, finalFrame?.data)
//		assertEquals(WebsocketFrameType.getTypeForOpcode(WebsocketFrameType.BINARY.opcode), finalFrame?.type)
//	}
	
	@Test
	fun testFragmentedData() {
		val data1 = Random.nextBytes(512)
		val data2 = Random.nextBytes(512)
		val data3 = Random.nextBytes(512)
		val allData = data1 + data2 + data3
		
		val packet1 = buildPacket(fin=false, mask=false, opcode=WebsocketFrameType.BINARY.opcode, maskingKey=0, data=data1)
		val packet2 = buildPacket(fin=false, mask=false, opcode=WebsocketFrameType.CONTINUATION.opcode, maskingKey=0, data=data2)
		val packet3 = buildPacket(fin=true, mask=false, opcode=WebsocketFrameType.CONTINUATION.opcode, maskingKey=0, data=data3)
		val allPackets = packet1 + packet2 + packet3
		
		val parser = WebsocketParser()
		val finalFrame = feedChunks(parser, allPackets, 4096)
		assertNotNull(finalFrame)
		
		assertArrayEquals(allData, finalFrame?.data)
		assertEquals(WebsocketFrameType.getTypeForOpcode(WebsocketFrameType.BINARY.opcode), finalFrame?.type)
	}
	
	@Test
	fun testInterleavedControlFrames() {
		val data1 = Random.nextBytes(256)
		val data2 = Random.nextBytes(256)
		val dataPing = Random.nextBytes(8)
		val allData = data1 + data2
		
		val packet1 = buildPacket(fin=false, mask=false, opcode=WebsocketFrameType.BINARY.opcode, maskingKey=0, data=data1)
		val packet2 = buildPacket(fin=true, mask=false, opcode=WebsocketFrameType.CONTINUATION.opcode, maskingKey=0, data=data2)
		val packetPing = buildPacket(fin=true, mask=false, opcode=WebsocketFrameType.PING.opcode, maskingKey=0, data=dataPing)
		
		val parser = WebsocketParser()
		assertNull(feedChunks(parser, packet1, 4096))
		val pingFrame = feedChunks(parser, packetPing, 128)
		val finalFrame = feedChunks(parser, packet2, 4096)
		assertNotNull(pingFrame)
		assertNotNull(finalFrame)
		
		assertArrayEquals(dataPing, pingFrame?.data)
		assertEquals(WebsocketFrameType.getTypeForOpcode(WebsocketFrameType.PING.opcode), pingFrame?.type)
		
		assertArrayEquals(allData, finalFrame?.data)
		assertEquals(WebsocketFrameType.getTypeForOpcode(WebsocketFrameType.BINARY.opcode), finalFrame?.type)
	}
	
	private fun feedChunks(parser: WebsocketParser, data: ByteArray, chunkSize: Int): WebsocketFrame? {
		if (chunkSize > data.size)
			return parser.parseChunk(data, 0, data.size)
		
		var index = 0
		while (index < data.size) {
			if (index + chunkSize > data.size) {
				return parser.parseChunk(data, index, data.size - index)
			}
			parser.parseChunk(data, index, chunkSize)
			index += chunkSize
		}
		return null
	}
	
	private fun buildPacket(fin: Boolean, mask: Boolean, opcode: Byte, maskingKey: Int, data: ByteArray): ByteArray {
		val headerLength = 2 + (if (mask) 4 else 0) + (if (data.size > 65535) 8 else (if (data.size >= 126) 2 else 0))
		val returnData = ByteArray(headerLength + data.size)
		if (fin)
			returnData[0] = returnData[0] or 0b10000000.toByte()
		returnData[0] = returnData[0] or (opcode and 0xF)
		
		if (mask) {
			returnData[1] = returnData[1] or 0b10000000.toByte()
			for (i in 0..3) {
				returnData[headerLength-i-1] = (maskingKey shr (8 * i)).toByte()
			}
		}
		
		if (data.size > 65535) {
			returnData[1] = returnData[1] or 0x7F
			val dataLength = data.size.toLong()
			for (i in 0..7) {
				returnData[9 - i] = ((dataLength shr (i * 8)) and 0xFF).toByte()
			}
		} else if (data.size >= 126) {
			returnData[1] = returnData[1] or 0x7E
			val dataLength = data.size
			for (i in 0..1) {
				returnData[3 - i] = ((dataLength shr (i * 8)) and 0xFF).toByte()
			}
		} else {
			returnData[1] = returnData[1] or data.size.toByte()
		}
		
		System.arraycopy(data, 0, returnData, headerLength, data.size)
		if (mask) {
			val xorBytes = ByteArray(4)
			for (i in 0..3) {
				xorBytes[i] = ((maskingKey ushr ((3 - i) * 8)) and 0xFF).toByte()
			}
			
			for (i in data.indices) {
				returnData[headerLength + i] = returnData[headerLength + i] xor xorBytes[i % 4]
			}
		}
		
		val frameEncodedPacket = WebsocketFrame(WebsocketFrameType.getTypeForOpcode(opcode), data)
		assertArrayEquals(returnData, frameEncodedPacket.encode(fin, mask, maskingKey))
		return returnData
	}
}
