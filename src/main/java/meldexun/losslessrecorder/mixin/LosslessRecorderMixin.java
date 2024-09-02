package meldexun.losslessrecorder.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import meldexun.losslessrecorder.LosslessRecorder;
import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class LosslessRecorderMixin {

	@Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/shader/Framebuffer;framebufferRender(II)V", shift = Shift.AFTER))
	public void postRenderGame(CallbackInfo info) {
		LosslessRecorder.update();
	}

}
