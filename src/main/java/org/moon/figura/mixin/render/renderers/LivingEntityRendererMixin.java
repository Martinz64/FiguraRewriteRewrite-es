package org.moon.figura.mixin.render.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ElytraModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.ElytraLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.moon.figura.FiguraMod;
import org.moon.figura.avatar.Avatar;
import org.moon.figura.avatar.AvatarManager;
import org.moon.figura.model.rendering.PartFilterScheme;
import org.moon.figura.config.Config;
import org.moon.figura.ducks.LivingEntityRendererAccessor;
import org.moon.figura.gui.PopupMenu;
import org.moon.figura.mixin.render.layers.elytra.ElytraLayerAccessor;
import org.moon.figura.trust.TrustContainer;
import org.moon.figura.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> extends EntityRenderer<T> implements RenderLayerParent<T, M>, LivingEntityRendererAccessor<T> {

    protected LivingEntityRendererMixin(EntityRendererProvider.Context context) {
        super(context);
    }

    @Unique
    private Avatar currentAvatar;

    @Final
    @Shadow
    protected List<RenderLayer<T, M>> layers;

    private ElytraModel<T> elytraModel;
    private boolean triedFetchElytraModel = false;

    @Shadow protected abstract boolean isBodyVisible(T livingEntity);
    @Shadow
    public static int getOverlayCoords(LivingEntity entity, float whiteOverlayProgress) {
        return 0;
    }
    @Shadow protected abstract float getWhiteOverlayProgress(T entity, float tickDelta);

    private void fetchElytraModel() {
        triedFetchElytraModel = true;
        if (!((Object) this instanceof PlayerRenderer)) return;
        RenderLayer<T, M> layerCandidate = layers.get(6);
        if (!(layerCandidate instanceof ElytraLayer<T, M> elytraLayer)) { //a bit jank but it should get the elytra layer, look at PlayerRenderer.class
            FiguraMod.LOGGER.warn("Unable to find elytra layer... Seems some other mod is messing with the layers, or " + FiguraMod.MOD_NAME + " version is weird.");
            return;
        }
        elytraModel = ((ElytraLayerAccessor<T, M>) elytraLayer).getElytraModel();
    }

    @Override
    public ElytraModel<T> figura$getElytraModel() {
        if (!triedFetchElytraModel)
            fetchElytraModel();
        return elytraModel;
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", shift = At.Shift.AFTER), method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void preRender(T entity, float yaw, float delta, PoseStack matrices, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        currentAvatar = AvatarManager.getAvatar(entity);
        if (currentAvatar == null)
            return;

        if (currentAvatar.luaRuntime != null && getModel() instanceof PlayerModel<?> playerModel && entity instanceof Player) {
            currentAvatar.luaRuntime.vanilla_model.PLAYER.store(playerModel);
            if (currentAvatar.trust.get(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 1)
                currentAvatar.luaRuntime.vanilla_model.PLAYER.alter(playerModel);
        }

        boolean showBody = this.isBodyVisible(entity);
        boolean translucent = !showBody && Minecraft.getInstance().player != null && !entity.isInvisibleTo(Minecraft.getInstance().player);
        boolean glowing = !showBody && Minecraft.getInstance().shouldEntityAppearGlowing(entity);
        boolean invisible = !translucent && !showBody && !glowing;

        //When viewed 3rd person, render all non-world parts.
        PartFilterScheme filter = invisible ? PartFilterScheme.PIVOTS : entity.isSpectator() ? PartFilterScheme.HEAD : PartFilterScheme.MODEL;
        int overlay = getOverlayCoords(entity, getWhiteOverlayProgress(entity, delta));
        UIHelper.EntityRenderMode renderMode = UIHelper.paperdoll ? UIHelper.renderMode : UIHelper.EntityRenderMode.RENDER;
        currentAvatar.renderEvent(delta, renderMode.name());
        currentAvatar.render(entity, yaw, delta, translucent ? 0.15f : 1f, matrices, bufferSource, light, overlay, (LivingEntityRenderer<?, ?>) (Object) this, filter, translucent, glowing);
        currentAvatar.postRenderEvent(delta, renderMode.name());
    }

    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"), method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void endRender(T entity, float yaw, float delta, PoseStack matrices, MultiBufferSource bufferSource, int light, CallbackInfo ci) {
        if (currentAvatar == null)
            return;

        //Render avatar with params
        EntityModel<?> model = this.getModel();
        if (model instanceof PlayerModel<?> playerModel && entity instanceof Player && currentAvatar.luaRuntime != null && currentAvatar.trust.get(TrustContainer.Trust.VANILLA_MODEL_EDIT) == 1)
            currentAvatar.luaRuntime.vanilla_model.PLAYER.restore(playerModel);

        currentAvatar = null;
    }

    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    public void shouldShowName(T livingEntity, CallbackInfoReturnable<Boolean> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(Config.PREVIEW_NAMEPLATE.asBool());
        else if (!Minecraft.renderNames())
            cir.setReturnValue(false);
        else if (livingEntity.getUUID().equals(PopupMenu.getEntityId()))
            cir.setReturnValue(false);
        else if (Config.SELF_NAMEPLATE.asBool() && livingEntity == Minecraft.getInstance().player)
            cir.setReturnValue(true);
    }
}
