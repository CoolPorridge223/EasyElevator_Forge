package org.DJB.easyelevator.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.DJB.easyelevator.Easyelevator;
import java.util.*;

/** 1.20.1 客户端包上限为 32767 字节；按 24 KiB 分片保留 512 KiB 音效上传能力。 */
public final class AudioUpload {
    private static final int CHUNK=24*1024;
    private static final int MAX=ElevatorNetworking.MAX_AUDIO_BYTES;
    private static final int MAX_PENDING=64;
    private static final long TIMEOUT_NANOS=20_000_000_000L;
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    /** 一个连接只保留一份上传，超过 20 秒或断线即丢弃；内存分配前校验长度。 */
    private static final class Pending {
        final UUID transfer;final BlockPos station;final byte[] data;final long started=System.nanoTime();int next;
        Pending(Chunk chunk){transfer=chunk.transfer;station=chunk.station;data=new byte[chunk.total];}
    }
    public record Chunk(BlockPos station,UUID transfer,int total,int offset,byte[] bytes) implements ForgeNetwork.CustomPacketPayload {
        static final Id<Chunk> ID=new Id<>(Easyelevator.id("audio_chunk"),Chunk.class);
        static final ForgeNetwork.StreamCodec<FriendlyByteBuf,Chunk> CODEC=new ForgeNetwork.StreamCodec<>(){
            public Chunk decode(FriendlyByteBuf b){return new Chunk(b.readBlockPos(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readByteArray(CHUNK));}
            public void encode(FriendlyByteBuf b,Chunk p){b.writeBlockPos(p.station);b.writeUUID(p.transfer);b.writeVarInt(p.total);b.writeVarInt(p.offset);b.writeByteArray(p.bytes);}
        };
        public Id<? extends ForgeNetwork.CustomPacketPayload> getId(){return ID;}
    }
    public static void register(){
        ForgeNetwork.register(Chunk.ID,Chunk.CODEC,false);
        ForgeNetwork.registerGlobalReceiver(Chunk.ID,(chunk,context)->receive(context.player(),chunk));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->PENDING.remove(event.getEntity().getUUID()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent event)->{if(event.phase==net.minecraftforge.event.TickEvent.Phase.END)expire();});
    }
    /** 客户端按原文件顺序发送；服务端不会信任客户端给出的偏移或总长。 */
    public static void send(ElevatorNetworking.DoorSoundUpload upload){
        byte[] bytes=upload.bytes();if(bytes.length==0||bytes.length>MAX)return;
        UUID transfer=UUID.randomUUID();
        for(int offset=0;offset<bytes.length;offset+=CHUNK)
            ForgeNetwork.sendToServer(new Chunk(upload.station(),transfer,bytes.length,offset,Arrays.copyOfRange(bytes,offset,Math.min(offset+CHUNK,bytes.length))));
    }
    private static void expire(){long now=System.nanoTime();PENDING.values().removeIf(p->now-p.started>TIMEOUT_NANOS);}
    private static void receive(ServerPlayer player,Chunk chunk){
        expire();UUID key=player.getUUID();
        if(!player.isAlive()||player.isSpectator()||chunk.total<=0||chunk.total>MAX||chunk.offset<0||chunk.bytes.length==0||chunk.bytes.length>CHUNK||chunk.offset>chunk.total-chunk.bytes.length){PENDING.remove(key);return;}
        if(chunk.offset==0){if(!PENDING.containsKey(key)&&PENDING.size()>=MAX_PENDING)return;PENDING.put(key,new Pending(chunk));}
        Pending p=PENDING.get(key);
        if(p==null||!p.transfer.equals(chunk.transfer)||!p.station.equals(chunk.station)||p.data.length!=chunk.total||p.next!=chunk.offset){PENDING.remove(key);return;}
        System.arraycopy(chunk.bytes,0,p.data,p.next,chunk.bytes.length);p.next+=chunk.bytes.length;
        if(p.next==p.data.length){PENDING.remove(key);ForgeNetwork.deliverUpload(player,new ElevatorNetworking.DoorSoundUpload(p.station,p.data));}
    }
}
