package meldexun.losslessrecorder;

import net.minecraftforge.common.config.Config;

@Config(modid = LosslessRecorder.MODID)
public class LosslessRecorderConfig {

	public static int fps = 30;
	public static boolean createAPNG = true;
	public static int apngCompression = 0;
	public static boolean deleteFrames = false;

}
