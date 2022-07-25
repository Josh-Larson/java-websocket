package me.joshlarson.websocket.common.parser.websocket;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record WebsocketFrame(
		@NotNull WebsocketFrameType type,
		@NotNull byte [] data
) {
	
	@NotNull
	public List<byte []> encodeFragmented(int fragmentSize) {
		return encodeFragmented(false, 0, fragmentSize);
	}
	
	@NotNull
	public List<byte []> encodeFragmented(boolean masked, int maskKey, int fragmentSize) {
		List<byte []> fragments = new ArrayList<>();
		for (int dataIndex = 0; dataIndex < data.length; dataIndex += fragmentSize) {
			boolean lastFragment = dataIndex + fragmentSize >= data.length;
			fragments.add(encode(lastFragment, masked, maskKey, dataIndex, Math.min(data.length - dataIndex, fragmentSize)));
		}
		return fragments;
	}
	
	@NotNull
	public byte [] encode() {
		return encode(false, 0);
	}
	
	@NotNull
	public byte [] encode(boolean masked, int maskKey) {
		return encode(true, masked, maskKey, 0, data.length);
	}
	
	@NotNull
	public byte [] encode(boolean fin, boolean masked, int maskKey) {
		return encode(fin, masked, maskKey, 0, data.length);
	}
	
	@NotNull
	private byte [] encode(boolean fin, boolean masked, int maskKey, int dataIndex, long dataLength) {
		int headerLength = 2 + (masked ? 4 : 0) + ((dataLength > 65535) ? 8 : ((dataLength >= 126) ? 2 : 0));
		byte [] returnData = new byte[headerLength + (int) dataLength];
		
		if (fin)
			returnData[0] |= 0b10000000;
		returnData[0] |= type.getOpcode() & 0xF;
		
		if (masked) {
			returnData[1] |= 0b10000000;
			for (int i = 0; i < 4; i++) {
				returnData[headerLength-i-1] = (byte) ((maskKey >> (8 * i)) & 0xFF);
			}
		}
		
		if (dataLength > 65535) {
			returnData[1] |= 0x7F;
			for (int i = 0; i < 8; i++) {
				returnData[9 - i] = (byte) ((dataLength >> (i * 8)) & 0xFF);
			}
		} else if (dataLength >= 126) {
			returnData[1] |= 0x7E;
			returnData[2] = (byte) ((dataLength >> 8) & 0xFF);
			returnData[3] = (byte) (dataLength & 0xFF);
		} else {
			returnData[1] |= (byte) dataLength;
		}
		
		System.arraycopy(data, dataIndex, returnData, headerLength, (int) dataLength);
		if (masked) {
			int maskByte1 = (maskKey >> 24) & 0xFF;
			int maskByte2 = (maskKey >> 16) & 0xFF;
			int maskByte3 = (maskKey >> 8) & 0xFF;
			int maskByte4 = maskKey & 0xFF;
			
			int maskXorIndex = headerLength;
			while (maskXorIndex+3 < returnData.length) {
				returnData[maskXorIndex++] ^= maskByte1;
				returnData[maskXorIndex++] ^= maskByte2;
				returnData[maskXorIndex++] ^= maskByte3;
				returnData[maskXorIndex++] ^= maskByte4;
			}
			
			if (maskXorIndex < returnData.length)
				returnData[maskXorIndex++] ^= maskByte1;
			if (maskXorIndex < returnData.length)
				returnData[maskXorIndex++] ^= maskByte2;
			if (maskXorIndex < returnData.length)
				returnData[maskXorIndex] ^= maskByte3;
		}
		
		return returnData;
	}
	
}
