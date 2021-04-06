package minegame159.meteorclient.mixin;

import minegame159.meteorclient.systems.modules.Modules;
import minegame159.meteorclient.systems.modules.render.NoRender;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntityRenderer.class)
public class BeaconBlockEntityRendererMixin {

    @Inject(method = "renderLightBeam", at = @At("HEAD"), cancellable = true)
    private static void onRenderBeaconBeam(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, Identifier identifier, float f, float g, long l, int i, int j, float[] fs, float h, float k, CallbackInfo ci) {
        if (Modules.get().get(NoRender.class).noBeaconBeam()) {
            ci.cancel();
        }
    }
}
