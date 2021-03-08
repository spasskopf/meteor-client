package minegame159.meteorclient.mixin;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import minegame159.meteorclient.utils.network.ProxyScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.s2c.play.DisconnectS2CPacket;
import net.minecraft.text.LiteralText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net/minecraft/network/ClientConnection$1")
public class ClientConnectMixinForProxy {


    @Redirect(method = "initChannel(Lio/netty/channel/Channel;)V",
            at = @At(value = "INVOKE", ordinal = 0, target = "Lio/netty/channel/ChannelPipeline;addLast(Ljava/lang/String;Lio/netty/channel/ChannelHandler;)Lio/netty/channel/ChannelPipeline;"))
    private ChannelPipeline addProxyHandler(ChannelPipeline pipeline, String name, ChannelHandler handler) {
        if (ProxyScreen.isActive() && ProxyScreen.isSet()) {
            try {
                pipeline.addLast(ProxyScreen.ProxyType.create());
                ProxyScreen.setInfoText("Connected successfully!");
            } catch (Exception e) {
                ProxyScreen.setInfoText(String.format("Error: %s | %s, closed connection to prevent connecting without proxy", e.getClass().getSimpleName(), e.getLocalizedMessage()));
                //To prevent connecting without a proxy!
                assert MinecraftClient.getInstance().player != null;
                MinecraftClient.getInstance().player.networkHandler.onDisconnect(new DisconnectS2CPacket(
                        new LiteralText("[Meteor-Proxy] Invalid Settings! Disconnected to prevent connecting without proxy!")));
                //pipeline.close();
                e.printStackTrace();
            }
        }

        pipeline.addLast(name, handler);
        return pipeline;
    }


}
