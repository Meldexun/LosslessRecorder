package meldexun.losslessrecorder;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import meldexun.imageutil.Color;
import meldexun.imageutil.png.CompressedAPNG;
import meldexun.imageutil.png.CompressedAPNG.Frame;
import meldexun.imageutil.png.PNGDecoder;
import meldexun.losslessrecorder.util.APNGEncoder;
import meldexun.memoryutil.MemoryAccess;
import meldexun.memoryutil.UnsafeBuffer;
import meldexun.memoryutil.UnsafeBufferUtil;

public class APNGUtil {

	private static final Pattern IMAGE_PATTERN = Pattern.compile(".*\\.(?:png|jpg|jpeg)");
	private static final DecimalFormat FORMAT = new DecimalFormat("0000");

	public static void main(String[] args) throws IOException {
		switch (args.length > 0 ? args[0] : "") {
		case "create": {
			if (args.length < 7) {
				throw new IllegalArgumentException();
			}
			Path[] src = Files.list(Paths.get(args[1])).filter(p -> IMAGE_PATTERN.matcher(p.getFileName().toString()).matches()).toArray(Path[]::new);
			int width = Integer.parseInt(args[2]);
			int height = Integer.parseInt(args[3]);
			int fps = Integer.parseInt(args[4]);
			int compression = Integer.parseInt(args[5]);
			Path dst = Paths.get(args[6]);
			APNGEncoder.createAPNG(src, width, height, fps, compression, dst);
			break;
		}
		case "split": {
			if (args.length < 3) {
				throw new IllegalArgumentException();
			}
			Path src = Paths.get(args[1]);
			Path dst = Paths.get(args[2]);
			Files.createDirectories(dst);
			CompressedAPNG apng;
			try (InputStream in = new BufferedInputStream(Files.newInputStream(src))) {
				apng = PNGDecoder.readCompressed(in);
			}
			try (UnsafeBuffer buffer = UnsafeBufferUtil.allocate(apng.width * apng.height * Color.BGRA.bytesPerPixel())) {
				BufferedImage image = new BufferedImage(apng.width, apng.height, BufferedImage.TYPE_INT_ARGB);
				MemoryAccess imageAccess = MemoryAccess.of(((DataBufferInt) image.getRaster().getDataBuffer()).getData());
				for (int i = 0; i < apng.frames.size(); i++) {
					Frame frame = apng.frames.get(i);
					PNGDecoder.decodeFrame(apng, frame, buffer, Color.BGRA);
					for (int y = 0; y < frame.height; y++) {
						for (int x = 0; x < frame.width; x++) {
							int c = buffer.getInt((y * frame.width + x) * Color.BGRA.bytesPerPixel());
							if (c == 0)
								continue;
							imageAccess.putInt(((y + frame.y_offset) * apng.width + (x + frame.x_offset)) * Color.BGRA.bytesPerPixel(), c);
						}
					}
					ImageIO.write(image, "png", dst.resolve(FORMAT.format(i) + ".png").toFile());
				}
			}
			break;
		}
		default:
			System.out.println("Not a valid command. Available commands:");
			System.out.println(" create [src] [width] [height] [fps] [compression] [dst]");
			System.out.println(" split [src] [dst]");
			break;
		}
	}

}
