package meldexun.losslessrecorder;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BooleanSupplier;

import javax.imageio.ImageIO;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL21;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GLSync;

import meldexun.losslessrecorder.util.APNGEncoder;
import meldexun.losslessrecorder.util.GLBuffer;
import meldexun.losslessrecorder.util.GLUtil;
import meldexun.memoryutil.MemoryAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLConstructionEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent.KeyInputEvent;

@Mod(modid = LosslessRecorder.MODID, acceptableRemoteVersions = "*")
public class LosslessRecorder {

	public static final String MODID = "losslessrecorder";
	public static final Logger LOGGER = LogManager.getLogger(MODID);
	public static final KeyBinding KEY_START = new KeyBinding("Start Recording", Keyboard.KEY_F10, "Lossless Recorder");
	public static final KeyBinding KEY_STOP = new KeyBinding("Stop Recording", Keyboard.KEY_F12, "Lossless Recorder");

	@EventHandler
	public void onFMLConstructionEvent(FMLConstructionEvent event) {
		GLUtil.init();
		MinecraftForge.EVENT_BUS.register(this);
	}

	@EventHandler
	public void onFMLInitializationEvent(FMLInitializationEvent event) {
		ClientRegistry.registerKeyBinding(KEY_START);
		ClientRegistry.registerKeyBinding(KEY_STOP);
	}

	@SubscribeEvent
	public void onConfigChangedEvent(OnConfigChangedEvent event) {
		if (event.getModID().equals(MODID)) {
			ConfigManager.sync(MODID, Config.Type.INSTANCE);
		}
	}

	@SubscribeEvent
	public void onKeyInputEvent(KeyInputEvent event) {
		if (Keyboard.isKeyDown(KEY_START.getKeyCode())) {
			start();
		}
		if (Keyboard.isKeyDown(KEY_STOP.getKeyCode())) {
			stop();
		}
	}

	private static final Queue<GLBuffer> BUFFER_CACHE = new ConcurrentLinkedQueue<>();
	private static final Queue<BooleanSupplier> TASKS = new ConcurrentLinkedQueue<>();
	private static final DecimalFormat FORMAT = new DecimalFormat("0000");

	private static boolean recording;
	private static Path outputDirectory;
	private static Path framesDirectory;
	private static int width;
	private static int height;
	private static int frameCount;
	private static long nextFrame;

	public static void start() {
		if (!recording) {
			recording = true;
			outputDirectory = Paths.get("recordings", LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuu-MM-dd-HH-mm-ss")));
			framesDirectory = outputDirectory.resolve("frames");
			try {
				Files.createDirectories(outputDirectory);
				Files.createDirectories(framesDirectory);
			} catch (IOException e) {
				LosslessRecorder.LOGGER.error("Failed creating output directories", e);
				throw new UncheckedIOException(e);
			}
			Framebuffer fb = Minecraft.getMinecraft().getFramebuffer();
			width = fb.framebufferWidth;
			height = fb.framebufferHeight;
			frameCount = 0;
			nextFrame = System.nanoTime();
		}
	}

	public static void stop() {
		recording = false;
		if (LosslessRecorderConfig.createAPNG) {
			while (!TASKS.isEmpty()) {
				TASKS.removeIf(BooleanSupplier::getAsBoolean);
			}
			try {
				Path[] frames = Files.list(framesDirectory).filter(p -> p.getFileName().toString().endsWith(".png")).toArray(Path[]::new);
				APNGEncoder.createAPNG(frames, width, height, LosslessRecorderConfig.fps, LosslessRecorderConfig.apngCompression, outputDirectory.resolve(outputDirectory.getFileName() + ".png"));
				if (LosslessRecorderConfig.deleteFrames) {
					for (Path p : frames) {
						Files.delete(p);
					}
					if (!Files.list(framesDirectory).findFirst().isPresent()) {
						Files.delete(framesDirectory);
					}
				}
			} catch (IOException e) {
				LosslessRecorder.LOGGER.error("Failed creating APNG", e);
				throw new UncheckedIOException(e);
			}
		}
	}

	public static void update() {
		if (recording && System.nanoTime() >= nextFrame) {
			nextFrame += 1_000_000_000L / LosslessRecorderConfig.fps;
			if (System.nanoTime() > nextFrame) {
				nextFrame = System.nanoTime();
				LOGGER.warn("Dropping frames!");
			}

			Minecraft mc = Minecraft.getMinecraft();
			Framebuffer fb = mc.getFramebuffer();
			int width = fb.framebufferWidth;
			int height = fb.framebufferHeight;
			long bytes = (long) width * height * 4;
			int index = frameCount++;

			GLBuffer pixelBuffer;
			if (BUFFER_CACHE.isEmpty()) {
				pixelBuffer = new GLBuffer(bytes, GL30.GL_MAP_READ_BIT, 0, true, GL30.GL_MAP_READ_BIT);
			} else {
				GLBuffer pixelBuffer1 = BUFFER_CACHE.remove();
				if (pixelBuffer1.getSize() < bytes) {
					pixelBuffer1.dispose();
					pixelBuffer1 = new GLBuffer(bytes, GL30.GL_MAP_READ_BIT, 0, true, GL30.GL_MAP_READ_BIT);
				}
				pixelBuffer = pixelBuffer1;
			}

			GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, pixelBuffer.getBuffer());
			GL11.glReadPixels(0, 0, width, height, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, 0L);
			GLSync sync = GL32.glFenceSync(GL32.GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
			GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER, 0);

			TASKS.add(() -> {
				if (GL32.glGetSynci(sync, GL32.GL_SYNC_STATUS) == GL32.GL_UNSIGNALED) {
					return false;
				}
				GL32.glDeleteSync(sync);

				CompletableFuture.runAsync(() -> {
					BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
					int[] imageData = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
					MemoryAccess.copyMemory(pixelBuffer, 0L, MemoryAccess.of(imageData), 0L, bytes);
					BUFFER_CACHE.add(pixelBuffer);
					TextureUtil.processPixelValues(imageData, width, height);
					try {
						ImageIO.write(image, "png", framesDirectory.resolve(FORMAT.format(index) + ".png").toFile());
					} catch (IOException e) {
						e.printStackTrace();
					}
				});

				return true;
			});
		}

		TASKS.removeIf(BooleanSupplier::getAsBoolean);
	}

}
