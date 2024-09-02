package meldexun.losslessrecorder.util;

import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import meldexun.imageutil.IOUtil;

public class APNGEncoder {

	private static final long SIGNATURE = 0x89504e470d0a1a0aL;
	private static final int CHUNK_IHDR = 0x49484452;
	private static final int CHUNK_IDAT = 0x49444154;
	private static final int CHUNK_IEND = 0x49454e44;
	private static final int CHUNK_acTL = 0x6163544c;
	private static final int CHUNK_fcTL = 0x6663544c;
	private static final int CHUNK_fdAT = 0x66644154;
	private static final int BIT_DEPTH_8 = 8;
	private static final int COLOR_TYPE_TRUECOLOR_ALPHA = 6;
	private static final int COMPRESSION_DEFLATE = 0;
	private static final int FILTER_DEFAULT = 0;
	private static final int INTERLACE_NO_INTERLACE = 0;

	public static void createAPNG(Path[] files, int width, int height, int fps, int compression, Path dst) throws IOException {
		try (PNGChunkWriter out = new PNGChunkWriter(new BufferedOutputStream(Files.newOutputStream(dst)))) {
			out.writeLong(SIGNATURE);

			out.startChunk(CHUNK_IHDR);
			out.writeInt(width);
			out.writeInt(height);
			out.writeByte(BIT_DEPTH_8);
			out.writeByte(COLOR_TYPE_TRUECOLOR_ALPHA);
			out.writeByte(COMPRESSION_DEFLATE);
			out.writeByte(FILTER_DEFAULT);
			out.writeByte(INTERLACE_NO_INTERLACE);
			out.endChunk();

			out.startChunk(CHUNK_acTL);
			out.writeInt(files.length);
			out.writeInt(0);
			out.endChunk();

			int sequenceNumber = 0;
			BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			for (int i = 0; i < files.length; i++) {
				int x0 = Integer.MAX_VALUE;
				int y0 = Integer.MAX_VALUE;
				int x1 = Integer.MIN_VALUE;
				int y1 = Integer.MIN_VALUE;
				byte[] bytes;
				BufferedImage frame = ImageIO.read(files[i].toFile());
				BufferedImage compressedFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
				for (int y = 0; y < Math.min(frame.getHeight(), height); y++) {
					for (int x = 0; x < Math.min(frame.getWidth(), width); x++) {
						int prevColor = image.getRGB(x, y);
						int prevA = prevColor >> 24 & 0xFF;
						int prevR = prevColor >> 16 & 0xFF;
						int prevG = prevColor >> 8 & 0xFF;
						int prevB = prevColor >> 0 & 0xFF;
						int currColor = frame.getRGB(x, y);
						int currA = currColor >> 24 & 0xFF;
						int currR = currColor >> 16 & 0xFF;
						int currG = currColor >> 8 & 0xFF;
						int currB = currColor >> 0 & 0xFF;
						if (i == 0 || Math.abs(currR - prevR) > compression || Math.abs(currG - prevG) > compression || Math.abs(currB - prevB) > compression || Math.abs(currA - prevA) > compression) {
							x0 = Math.min(x, x0);
							y0 = Math.min(y, y0);
							x1 = Math.max(x, x1);
							y1 = Math.max(y, y1);
							compressedFrame.setRGB(x, y, currColor);
						} else {
							compressedFrame.setRGB(x, y, 0);
						}
						image.setRGB(x, y, currColor);
					}
				}
				if (x1 < x0 && y1 < y0) {
					x0 = y0 = x1 = y1 = 0;
				}
				ImageIO.write(compressedFrame.getSubimage(x0, y0, x1 - x0 + 1, y1 - y0 + 1), "png", buffer);
				bytes = buffer.toByteArray();
				buffer.reset();

				out.startChunk(CHUNK_fcTL);
				out.writeInt(sequenceNumber++);
				out.writeInt(x1 - x0 + 1);
				out.writeInt(y1 - y0 + 1);
				out.writeInt(x0);
				out.writeInt(y0);
				out.writeShort(1);
				out.writeShort(fps);
				out.writeByte(0);
				out.writeByte(0);
				out.endChunk();

				if (i == 0) {
					out.startChunk(CHUNK_IDAT);
				} else {
					out.startChunk(CHUNK_fdAT);
					out.writeInt(sequenceNumber++);
				}
				byte[] b = new byte[8192];
				InputStream in = new ByteArrayInputStream(bytes);
				IOUtil.readLong(in, b);
				while (true) {
					int length = IOUtil.readInt(in, b);
					int type = IOUtil.readInt(in, b);
					if (type == CHUNK_IDAT) {
						IOUtil.copy(in, out, b, length);
					} else {
						IOUtil.skip(in, length);
					}
					IOUtil.readInt(in, b);
					if (type == CHUNK_IEND) {
						break;
					}
				}
				out.endChunk();
			}

			out.startChunk(CHUNK_IEND);
			out.endChunk();
		}
	}

}
