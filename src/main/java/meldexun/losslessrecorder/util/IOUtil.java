package meldexun.losslessrecorder.util;

import java.io.IOException;
import java.io.OutputStream;

class IOUtil {

	public static void writeByte(OutputStream out, int v) throws IOException {
		out.write(v);
	}

	public static void writeShort(OutputStream out, byte[] b, int v) throws IOException {
		b[0] = (byte) (v >>> 8 & 0xFF);
		b[1] = (byte) (v >>> 0 & 0xFF);
		out.write(b, 0, 2);
	}

	public static void writeInt(OutputStream out, byte[] b, int v) throws IOException {
		b[0] = (byte) (v >>> 24 & 0xFF);
		b[1] = (byte) (v >>> 16 & 0xFF);
		b[2] = (byte) (v >>> 8 & 0xFF);
		b[3] = (byte) (v >>> 0 & 0xFF);
		out.write(b, 0, 4);
	}

	public static void writeLong(OutputStream out, byte[] b, long v) throws IOException {
		b[0] = (byte) (v >>> 56 & 0xFF);
		b[1] = (byte) (v >>> 48 & 0xFF);
		b[2] = (byte) (v >>> 40 & 0xFF);
		b[3] = (byte) (v >>> 32 & 0xFF);
		b[4] = (byte) (v >>> 24 & 0xFF);
		b[5] = (byte) (v >>> 16 & 0xFF);
		b[6] = (byte) (v >>> 8 & 0xFF);
		b[7] = (byte) (v >>> 0 & 0xFF);
		out.write(b, 0, 8);
	}

}
