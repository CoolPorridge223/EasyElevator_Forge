package org.DJB.easyelevator.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.*;

/** Forge 有方向的网络通道；延续原包字段、长度限制与主线程校验。 */
public final class ForgeNetwork {
    private static final String VERSION="1";
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation("easyelevator","main"),()->VERSION,VERSION::equals,VERSION::equals);
    private static int discriminator;
    private static final Map<ResourceLocation,BiConsumer<CustomPacketPayload,Context>> SERVER=new HashMap<>();
    private static final Map<ResourceLocation,Consumer<CustomPacketPayload>> CLIENT=new HashMap<>();
    public interface CustomPacketPayload {
        record Id<T extends CustomPacketPayload>(ResourceLocation id,Class<T> type) {}
        Id<? extends CustomPacketPayload> getId();
    }
    public interface StreamCodec<B,T> { T decode(B buf); void encode(B buf,T value); }
    public record Context(ServerPlayer player) { public net.minecraft.server.MinecraftServer server(){return player.getServer();} }
    /** 注册时固定方向，禁止客户端伪造服务器状态包；大音频由 AudioUpload 分片传输。 */
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void register(CustomPacketPayload.Id<T> id,StreamCodec<FriendlyByteBuf,T> codec,boolean clientbound) {
        CHANNEL.messageBuilder(id.type(),discriminator++,clientbound?NetworkDirection.PLAY_TO_CLIENT:NetworkDirection.PLAY_TO_SERVER)
            .encoder((p,b)->codec.encode(b,p)).decoder(codec::decode)
            .consumerMainThread((p,c)->{
                if(clientbound){var h=CLIENT.get(id.id());if(h!=null)h.accept(p);}
                else if(c.get().getSender()!=null){var h=SERVER.get(id.id());if(h!=null)h.accept(p,new Context(c.get().getSender()));}
                c.get().setPacketHandled(true);
            }).add();
    }
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void registerGlobalReceiver(CustomPacketPayload.Id<T> id,BiConsumer<T,Context> receiver){SERVER.put(id.id(),(p,c)->receiver.accept((T)p,c));}
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void clientReceiver(CustomPacketPayload.Id<T> id,Consumer<T> receiver){CLIENT.put(id.id(),p->receiver.accept((T)p));}
    public static void send(ServerPlayer player,CustomPacketPayload payload){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),payload);}
    public static void sendToServer(CustomPacketPayload payload){
        if(payload instanceof ElevatorNetworking.DoorSoundUpload upload){AudioUpload.send(upload);return;}
        CHANNEL.sendToServer(payload);
    }
    static void deliverUpload(ServerPlayer player,ElevatorNetworking.DoorSoundUpload upload){
        var h=SERVER.get(upload.getId().id());if(h!=null)h.accept(upload,new Context(player));
    }
    public static Collection<ServerPlayer> around(ServerLevel world,BlockPos pos,double radius){return world.getPlayers(p->p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=radius*radius);}
    public static Collection<ServerPlayer> tracking(Entity entity){return ((ServerLevel)entity.level()).getChunkSource().chunkMap.getPlayers(entity.chunkPosition(),false);}
}
