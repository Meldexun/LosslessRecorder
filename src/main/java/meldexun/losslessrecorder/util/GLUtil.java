package meldexun.losslessrecorder.util;

import java.nio.ByteBuffer;

import javax.annotation.Nullable;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL44;
import org.lwjgl.opengl.GL45;
import org.lwjgl.opengl.GLContext;

public class GLUtil {

	public static ContextCapabilities CAPS;

	public static void init() {
		CAPS = GLContext.getCapabilities();
	}

	public static int createBuffer(long size, int flags, int usage) {
		if (CAPS.OpenGL45) {
			int buffer = GL45.glCreateBuffers();
			GL45.glNamedBufferStorage(buffer, size, flags);
			return buffer;
		} else if (CAPS.OpenGL44) {
			int buffer = GL15.glGenBuffers();
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
			GL44.glBufferStorage(GL15.GL_ARRAY_BUFFER, size, flags);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			return buffer;
		} else {
			int buffer = GL15.glGenBuffers();
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
			GL15.glBufferData(GL15.GL_ARRAY_BUFFER, size, usage);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			return buffer;
		}
	}

	public static ByteBuffer map(int buffer, long length, int accessRange, int access, @Nullable ByteBuffer oldBuffer) {
		if (CAPS.OpenGL45) {
			return GL45.glMapNamedBufferRange(buffer, 0L, length, accessRange, oldBuffer);
		} else if (CAPS.OpenGL30) {
			if (!CAPS.OpenGL44) {
				accessRange &= ~(GL44.GL_MAP_PERSISTENT_BIT | GL44.GL_MAP_COHERENT_BIT);
			}
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
			ByteBuffer byteBuffer = GL30.glMapBufferRange(GL15.GL_ARRAY_BUFFER, 0L, length, accessRange, oldBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			return byteBuffer;
		} else {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
			ByteBuffer byteBuffer = GL15.glMapBuffer(GL15.GL_ARRAY_BUFFER, access, length, oldBuffer);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
			return byteBuffer;
		}
	}

	public static void unmap(int buffer) {
		if (CAPS.OpenGL45) {
			GL45.glUnmapNamedBuffer(buffer);
		} else {
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, buffer);
			GL15.glUnmapBuffer(GL15.GL_ARRAY_BUFFER);
			GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		}
	}

}
