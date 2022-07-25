package me.joshlarson.websocket.common.parser;

import org.jetbrains.annotations.NotNull;

public class ParserByteStream {
	
	private byte[] data;
	private int size;
	
	public ParserByteStream(int capacity) {
		this.data = new byte[capacity];
		this.size = 0;
	}
	
	public void write(@NotNull byte[] chunk, int chunkStart, int chunkLength) {
		if (size + chunkLength > data.length) {
			byte[] newData = new byte[(int) Math.pow(2, Math.ceil(Math.log(size + chunkLength) / Math.log(2)))];
			System.arraycopy(data, 0, newData, 0, size);
			this.data = newData;
		}
		
		System.arraycopy(chunk, chunkStart, data, size, chunkLength);
		size += chunkLength;
	}
	
	@NotNull
	public byte[] read(int count) {
		if (count > size) throw new ArrayIndexOutOfBoundsException(count);
		
		byte[] ret = new byte[count];
		System.arraycopy(data, 0, ret, 0, count);
		removeFromStart(count);
		return ret;
	}
	
	public void removeFromStart(int count) {
		if (count >= size) {
			size = 0;
			return;
		}
		
		System.arraycopy(data, count, data, 0, size - count);
		size -= count;
	}
	
	@NotNull
	public byte[] getByteArray() {
		return data;
	}
	
	public int getSize() {
		return size;
	}
	
	public void reset() {
		this.size = 0;
	}
}
