package meldexun.losslessrecorder.util;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.CRC32;

class PNGChunkWriter extends FilterOutputStream {

	private final byte[] buffer = new byte[8];
	private int type = -1;
	private final ByteArrayOutputStream data = new ByteArrayOutputStream();
	private final CRC32 crc = new CRC32();

	public PNGChunkWriter(OutputStream out) {
		super(out);
	}

	public void writeByte(int v) throws IOException {
		IOUtil.writeByte(this, v);
	}

	public void writeShort(int v) throws IOException {
		IOUtil.writeShort(this, this.buffer, v);
	}

	public void writeInt(int v) throws IOException {
		IOUtil.writeInt(this, this.buffer, v);
	}

	public void writeLong(long v) throws IOException {
		IOUtil.writeLong(this, this.buffer, v);
	}

	@Override
	public void write(int b) throws IOException {
		if (this.isChunkOpen()) {
			this.data.write(b);
			this.crc.update(b);
		} else {
			super.write(b);
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		if (this.isChunkOpen()) {
			this.data.write(b);
			this.crc.update(b);
		} else {
			super.write(b);
		}
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (this.isChunkOpen()) {
			this.data.write(b, off, len);
			this.crc.update(b, off, len);
		} else {
			super.write(b, off, len);
		}
	}

	public void startChunk(int type) {
		if (this.isChunkOpen()) {
			throw new IllegalStateException();
		}
		this.type = type;
		this.crc.update(type >> 24 & 0xFF);
		this.crc.update(type >> 16 & 0xFF);
		this.crc.update(type >> 8 & 0xFF);
		this.crc.update(type >> 0 & 0xFF);
	}

	public void endChunk() throws IOException {
		if (!this.isChunkOpen()) {
			throw new IllegalStateException();
		}
		IOUtil.writeInt(this.out, this.buffer, this.data.size());
		IOUtil.writeInt(this.out, this.buffer, this.type);
		this.data.writeTo(this.out);
		IOUtil.writeInt(this.out, this.buffer, (int) this.crc.getValue());
		this.type = -1;
		this.data.reset();
		this.crc.reset();
	}

	private boolean isChunkOpen() {
		return this.type >= 0;
	}

}
