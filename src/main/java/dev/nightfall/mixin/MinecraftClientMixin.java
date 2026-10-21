package dev.nightfall.mixin;
import dev.nightfall.ui.NightfallMainMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Inject(method="setScreen",at=@At("HEAD"),cancellable=true)
    private void nightfall$replaceTitleScreen(Screen screen,CallbackInfo ci){
        if(screen instanceof TitleScreen && !(screen instanceof NightfallMainMenu)){
            ci.cancel();((MinecraftClient)(Object)this).setScreen(new NightfallMainMenu());
        }
    }
}
