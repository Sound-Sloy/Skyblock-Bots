package me.horeatise.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.horeatise.SkyblockBots;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Shadow public int attackCooldown;


    @Inject(at = @At("HEAD"), method = "tick")
    private void init(CallbackInfo info) {
        //if (this.attackCooldown == 10000)
        //    this.attackCooldown = 0;
    }
}