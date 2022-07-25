package me.joshlarson.websocket.common.parser.websocket;

import me.joshlarson.websocket.common.parser.ParserByteStream;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WebsocketParser {
	
	private final ParserByteStream buffer;
	private final List<byte[]> fragmentBuffer;
	
	private WebsocketFrameHeader currentFrameHeader;
	private WebsocketFrameType fragmentedOpcode;
	private long fragmentedSize;
	
	public WebsocketParser() {
		this.buffer = new ParserByteStream(1024);
		this.fragmentBuffer = new ArrayList<>();
		this.currentFrameHeader = null;
		this.fragmentedOpcode = null;
		this.fragmentedSize = 0;
	}
	
	@Nullable
	public WebsocketFrame parseChunk(@Nullable byte[] chunk, int start, int length) throws WebsocketParserException {
		// Enables pipelining of parsers
		if (chunk != null)
			this.buffer.write(chunk, start, length);
		
		while (true) {
			// Need a new packet
			if (currentFrameHeader == null)
				parseHeaderInProgress();
			
			// Didn't generate a new packet to process
			if (currentFrameHeader == null)
				break;
			
			// May reset the internal currentFrameHeader to null
			WebsocketFrame frame = parseWebsocketFrame();
			if (frame != null)
				return frame;
			
			// Don't have a full packet yet
			if (currentFrameHeader != null)
				break;
		}
		
		return null;
	}
	
	private void parseHeaderInProgress() throws WebsocketParserException {
		if (buffer.getSize() < 2)
			return;
		
		byte [] bufferData = buffer.getByteArray();
		byte firstByte = bufferData[0];
		byte secondByte = bufferData[1];
		boolean fin = (firstByte & 0b10000000) != 0;
		boolean mask = (secondByte & 0b10000000) != 0;
		byte opcode = (byte) (firstByte & 0x0F);
		byte payloadLengthInitial = (byte) (secondByte & 0x7F);
		int headerLength = 2 + (mask ? 4 : 0) + (payloadLengthInitial == 0x7E ? 2 : (payloadLengthInitial == 0x7F ? 8 : 0));
		
		if (buffer.getSize() < headerLength)
			return;
		
		if ((bufferData[0] & 0b01110000) != 0)
			throw new WebsocketParserException("undefined use of reserved bits");
		
		long payloadLength = switch(payloadLengthInitial) {
			case 0x7F -> (((long) bufferData[2] & 0xFF) << (8*7)) | (((long) bufferData[3] & 0xFF) << (8*6)) |
						 (((long) bufferData[4] & 0xFF) << (8*5)) | (((long) bufferData[5] & 0xFF) << (8*4)) |
						 (((long) bufferData[6] & 0xFF) << (8*3)) | (((long) bufferData[7] & 0xFF) << (8*2)) |
						 (((long) bufferData[8] & 0xFF) << 8)     | (((long) bufferData[9] & 0xFF));
			case 0x7E -> (((long) bufferData[2] & 0xFF) << 8) | ((long) bufferData[3] & 0xFF);
			default -> payloadLengthInitial;
		};
		
		int maskKey = 0;
		if (mask) {
			for (int m = headerLength-4; m < headerLength; m++) {
				maskKey = (maskKey << 8) | (bufferData[m] & 0xFF);
			}
		}
		
		buffer.removeFromStart(headerLength);
		currentFrameHeader = new WebsocketFrameHeader(fin, mask, opcode, payloadLength, maskKey);
	}
	
	private WebsocketFrame parseWebsocketFrame() throws WebsocketParserException {
		if (buffer.getSize() < currentFrameHeader.payloadLength())
			return null;
		
		// TODO: Handle long size
		byte [] bufferData = buffer.read((int) currentFrameHeader.payloadLength());
		
		// Rules for control frames
		if (currentFrameHeader.opcode() >= 8) {
			if (!currentFrameHeader.fin())
				throw new WebsocketParserException("invalid fragmented frame type");
			if (bufferData.length > 125)
				throw new WebsocketParserException("invalid control frame length");
		}
		
		// Unmask (booo websocket spec)
		if (currentFrameHeader.mask()) {
			int maskByte1 = (currentFrameHeader.maskingKey() >> 24) & 0xFF;
			int maskByte2 = (currentFrameHeader.maskingKey() >> 16) & 0xFF;
			int maskByte3 = (currentFrameHeader.maskingKey() >> 8) & 0xFF;
			int maskByte4 = currentFrameHeader.maskingKey() & 0xFF;
			
			int maskXorIndex = 0;
			while (maskXorIndex < bufferData.length - (bufferData.length % 4)) {
				bufferData[maskXorIndex++] ^= maskByte1;
				bufferData[maskXorIndex++] ^= maskByte2;
				bufferData[maskXorIndex++] ^= maskByte3;
				bufferData[maskXorIndex++] ^= maskByte4;
			}
			
			if (maskXorIndex < bufferData.length)
				bufferData[maskXorIndex++] ^= maskByte1;
			if (maskXorIndex < bufferData.length)
				bufferData[maskXorIndex++] ^= maskByte2;
			if (maskXorIndex < bufferData.length)
				bufferData[maskXorIndex] ^= maskByte3;
		}
		
		WebsocketFrameType opcode = WebsocketFrameType.getTypeForOpcode(currentFrameHeader.opcode());
		boolean finalFrame = currentFrameHeader.fin();
		currentFrameHeader = null;
		
		if (opcode != WebsocketFrameType.CONTINUATION && finalFrame)
			return new WebsocketFrame(opcode, bufferData);
		
		// Fragmented segment
		this.fragmentBuffer.add(bufferData);
		this.fragmentedSize += bufferData.length;
		
		// First fragmented segment
		if (opcode != WebsocketFrameType.CONTINUATION) {
			fragmentedOpcode = opcode;
			return null;
		}
		
		// Last fragmented segment
		if (finalFrame) {
			if (fragmentedSize > Integer.MAX_VALUE)
				throw new WebsocketParserException("fragmented size is too large");
			
			byte [] accumulatedData = new byte[(int) fragmentedSize];
			int currentIndex = 0;
			for (byte [] fragment : fragmentBuffer) {
				System.arraycopy(fragment, 0, accumulatedData, currentIndex, fragment.length);
				currentIndex += fragment.length;
			}
			WebsocketFrameType fragmentedOpcode = this.fragmentedOpcode;
			this.fragmentedOpcode = null;
			fragmentBuffer.clear();
			
			return new WebsocketFrame(fragmentedOpcode, accumulatedData);
		}
		
		return null;
	}
	
}
