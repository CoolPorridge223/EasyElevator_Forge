package org.DJB.easyelevator.client;
import net.minecraft.client.Minecraft;
import java.util.function.BiConsumer;
import org.DJB.easyelevator.network.ForgeNetwork;
/** 客户端收发边界；共同网络类不引用任何客户端 Minecraft 类。 */
public final class ForgeClientNetwork {
    public record Context(Minecraft client) {}
    public static <T extends ForgeNetwork.CustomPacketPayload> void registerGlobalReceiver(ForgeNetwork.CustomPacketPayload.Id<T> id,BiConsumer<T,Context> handler){ForgeNetwork.clientReceiver(id,p->handler.accept(p,new Context(Minecraft.getInstance())));}
    public static boolean canSend(ForgeNetwork.CustomPacketPayload.Id<?> id){return Minecraft.getInstance().getConnection()!=null;}
    public static void send(ForgeNetwork.CustomPacketPayload payload){ForgeNetwork.sendToServer(payload);}
}
