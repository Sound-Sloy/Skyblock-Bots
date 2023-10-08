package me.horeatise.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class HandleInputEventsMixin extends ReentrantThreadExecutor<Runnable> implements WindowEventHandler {

    @Shadow
    private void handleBlockBreaking(boolean breaking) {};

    @Inject(method = "handleInputEvents()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;handleBlockBreaking(Z)V"), cancellable = true)
    private void injected(CallbackInfo ci){
//        boolean modifiedBl3 = false;
//        if(((MinecraftClient)(Object)this).player.isUsingItem())
//        this.handleBlockBreaking((!bl3 && ((MinecraftClient)(Object)this).options.attackKey.isPressed() && ((MinecraftClient)(Object)this).mouse.isCursorLocked());
//        ci.cancel();
    }

    public HandleInputEventsMixin(String string) {
        super(string);
    }

    @Override
    public void onWindowFocusChanged(boolean focused) {

    }

    @Override
    public void onResolutionChanged() {

    }

    @Override
    public void onCursorEnterChanged() {

    }

    @Override
    protected Runnable createTask(Runnable runnable) {
        return null;
    }

    @Override
    protected boolean canExecute(Runnable task) {
        return false;
    }

    @Override
    protected Thread getThread() {
        return null;
    }
}
