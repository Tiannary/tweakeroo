package fi.dy.masa.tweakeroo.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.tweaks.MiscTweaks;
import fi.dy.masa.tweakeroo.tweaks.PlacementTweaks;
import fi.dy.masa.tweakeroo.util.IMinecraftClientInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.options.GameOptions;
import net.minecraft.client.options.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements IMinecraftClientInvoker
{
    @Shadow
    public GameOptions options;

    @Shadow
    private int itemUseCooldown;

    @Shadow
    private void doAttack() {}

    @Shadow
    private void doItemUse() {}

    @Override
    public void setItemUseCooldown(int value)
    {
        this.itemUseCooldown = value;
    }

    @Override
    public void leftClickMouseAccessor()
    {
        this.doAttack();
    }

    @Override
    public void rightClickMouseAccessor()
    {
        this.doItemUse();
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void onGameLoop(boolean renderWorld, CallbackInfo ci)
    {
        MiscTweaks.onGameLoop();
    }

    @Inject(method = "doAttack", at = {
            @At(value = "INVOKE",
                target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackEntity(" +
                         "Lnet/minecraft/entity/player/PlayerEntity;" +
                         "Lnet/minecraft/entity/Entity;)V"),
            @At(value = "INVOKE",
                target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackBlock(" +
                         "Lnet/minecraft/util/math/BlockPos;" +
                         "Lnet/minecraft/util/math/Direction;)Z")
            })
    private void onLeftClickMousePre(CallbackInfo ci)
    {
        PlacementTweaks.onLeftClickMousePre();
    }

    @Inject(method = "doAttack", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/ClientPlayerEntity;swingHand(Lnet/minecraft/util/Hand;)V"))
    private void onLeftClickMousePost(CallbackInfo ci)
    {
        PlacementTweaks.onLeftClickMousePost();
    }

    @Redirect(method = "doItemUse()V", at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;interactBlock(" +
                         "Lnet/minecraft/client/network/ClientPlayerEntity;" +
                         "Lnet/minecraft/client/world/ClientWorld;" +
                         "Lnet/minecraft/util/Hand;" +
                         "Lnet/minecraft/util/hit/BlockHitResult;" +
                         ")Lnet/minecraft/util/ActionResult;"))
    private ActionResult onProcessRightClickBlock(
            ClientPlayerInteractionManager controller,
            ClientPlayerEntity player,
            ClientWorld world,
            Hand hand,
            BlockHitResult hitResult)
    {
        return PlacementTweaks.onProcessRightClickBlock(controller, player, world, hand, hitResult);
    }

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void onProcessKeybindsPre(CallbackInfo ci)
    {
        if (((MinecraftClient) (Object) this).currentScreen == null)
        {
            if (FeatureToggle.TWEAK_HOLD_ATTACK.getBooleanValue())
            {
                KeyBinding.setKeyPressed(InputUtil.fromName(this.options.keyAttack.getName()), true);
            }

            if (FeatureToggle.TWEAK_HOLD_USE.getBooleanValue())
            {
                KeyBinding.setKeyPressed(InputUtil.fromName(this.options.keyUse.getName()), true);
            }
        }
    }
}