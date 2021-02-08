package minegame159.meteorclient.mixin;

import minegame159.meteorclient.modules.Modules;
import minegame159.meteorclient.modules.render.NoRender;
import net.minecraft.block.entity.EndPortalBlockEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.EndPortalBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlockEntityRenderer.class)
public class EndPortalBlockEntityRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public <T extends EndPortalBlockEntity> void onRenderEndGateway(T endPortalBlockEntity, float f, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, int j, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noEndGateway()) {
            ci.cancel();
        }
    }
}
