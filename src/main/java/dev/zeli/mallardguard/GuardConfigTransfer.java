package dev.zeli.mallardguard;

import dev.zeli.mallardguard.GuardPackets.*;
import static dev.zeli.mallardguard.GuardPackets.writeOptional;
import static dev.zeli.mallardguard.GuardPackets.readOptional;
import static dev.zeli.mallardguard.GuardPackets.clientPolicy;
import dev.zeli.mallardguard.client.GuardClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Bounded config uploads/snapshots with timeout and disconnect cleanup. */
public final class GuardConfigTransfer {
    private GuardConfigTransfer() {}
    static final int SAVE_CHUNK_BYTES=24_000, MAX_SAVE_BYTES=2_097_152;
    private static final int MAX_DECODED_BYTES=33_554_432;
    private static final long UPLOAD_TIMEOUT=15_000_000_000L;

    /** Limit both wire size and inflated size; no unbounded compressed payload allocation. */
    private static byte[] compress(RegistryFriendlyByteBuf buf){
        int size=buf.readableBytes();if(size>MAX_DECODED_BYTES)throw new IllegalArgumentException("Config exceeds the saved-data size limit.");
        byte[] data=new byte[size];buf.readBytes(data);var codec=new java.util.zip.Deflater();
        try{codec.setInput(data);codec.finish();var output=new java.io.ByteArrayOutputStream(Math.min(size,SAVE_CHUNK_BYTES));byte[] chunk=new byte[SAVE_CHUNK_BYTES];while(!codec.finished()){int count=codec.deflate(chunk);if(output.size()+count>MAX_SAVE_BYTES)throw new IllegalArgumentException("Config exceeds the transfer size limit.");output.write(chunk,0,count);}return output.toByteArray();}finally{codec.end();}
    }
    private static byte[] decompress(byte[] data){
        var codec=new java.util.zip.Inflater();
        try{codec.setInput(data);var output=new java.io.ByteArrayOutputStream(SAVE_CHUNK_BYTES);byte[] chunk=new byte[SAVE_CHUNK_BYTES];while(!codec.finished()){int count=codec.inflate(chunk);if(output.size()+count>MAX_DECODED_BYTES)throw new IllegalArgumentException("Inflated config is too large.");if(count==0&&!codec.finished())throw new IllegalArgumentException("Incomplete compressed config.");output.write(chunk,0,count);}if(codec.getRemaining()!=0)throw new IllegalArgumentException("Trailing compressed config data.");return output.toByteArray();}catch(java.util.zip.DataFormatException error){throw new IllegalArgumentException("Invalid compressed config.",error);}finally{codec.end();}
    }

    private static final class Upload {
        final int request;final byte[][] parts;int next,size;long touched;
        Upload(SaveChunk first){request=first.request();parts=new byte[first.total()][];touched=System.nanoTime();}
        boolean append(SaveChunk chunk){
            if(chunk.request()!=request||chunk.total()!=parts.length||chunk.index()!=next||size+chunk.bytes().length>MAX_SAVE_BYTES)return false;
            parts[next++]=chunk.bytes();size+=chunk.bytes().length;touched=System.nanoTime();return true;
        }
        byte[] complete(){if(next!=parts.length)return null;byte[] data=new byte[size];int offset=0;for(byte[] part:parts){System.arraycopy(part,0,data,offset,part.length);offset+=part.length;}return data;}
    }
    private static final java.util.Map<java.util.UUID,Upload> UPLOADS=new java.util.HashMap<>();
    public static void expireUploads(){if(UPLOADS.isEmpty())return;long now=System.nanoTime();UPLOADS.values().removeIf(upload->now-upload.touched>UPLOAD_TIMEOUT);}
    public static void forgetUpload(java.util.UUID player){UPLOADS.remove(player);}
    public static void clearUploads(){UPLOADS.clear();cachedSnapshot=null;cachedSnapshotBytes=null;}
    public static void sendConfig(SaveConfig request,net.minecraft.core.RegistryAccess registries){
        RegistryFriendlyByteBuf buf=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),registries,ConnectionType.NEOFORGE);
        try{
            SaveConfig.CODEC.encode(buf,request);byte[] packed=compress(buf);int size=packed.length;
            if(size>MAX_SAVE_BYTES)throw new IllegalArgumentException("Config save is too large.");
            int total=(size+SAVE_CHUNK_BYTES-1)/SAVE_CHUNK_BYTES;
            for(int index=0;index<total;index++){byte[] bytes=java.util.Arrays.copyOfRange(packed,index*SAVE_CHUNK_BYTES,Math.min(size,(index+1)*SAVE_CHUNK_BYTES));PacketDistributor.sendToServer(new SaveChunk(request.request(),total,index,bytes));}
        }finally{if(buf!=null)buf.release();}
    }
    static void saveChunk(SaveChunk chunk,IPayloadContext context){
        if(!(context.player() instanceof ServerPlayer player))return;
        if(!player.hasPermissions(2)){PacketDistributor.sendToPlayer(player,new SaveResult(chunk.request(),0,"Only operators can change server settings."));return;}
        expireUploads();var id=player.getUUID();
        if(chunk.total()<1||chunk.total()>(MAX_SAVE_BYTES+SAVE_CHUNK_BYTES-1)/SAVE_CHUNK_BYTES||chunk.index()<0||chunk.index()>=chunk.total()||chunk.bytes().length==0||chunk.bytes().length>SAVE_CHUNK_BYTES){UPLOADS.remove(id);return;}
        if(chunk.index()==0){if(!UPLOADS.containsKey(id)&&UPLOADS.size()>=16){PacketDistributor.sendToPlayer(player,new SaveResult(chunk.request(),0,"Too many config uploads. Try again shortly."));return;}UPLOADS.put(id,new Upload(chunk));}
        Upload upload=UPLOADS.get(id);
        if(upload==null||!upload.append(chunk)){UPLOADS.remove(id);PacketDistributor.sendToPlayer(player,new SaveResult(chunk.request(),0,"Config upload was incomplete. Try again."));return;}
        byte[] data=upload.complete();if(data==null)return;UPLOADS.remove(id);
        RegistryFriendlyByteBuf buf=null;
        try{buf=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(decompress(data)),player.registryAccess(),ConnectionType.NEOFORGE);SaveConfig request=SaveConfig.CODEC.decode(buf);if(buf.readableBytes()!=0||request.request()!=chunk.request())throw new IllegalArgumentException("Invalid config upload.");GuardPackets.saveConfig(request,context);}
        catch(RuntimeException error){PacketDistributor.sendToPlayer(player,new SaveResult(chunk.request(),0,"Could not read config upload. Try again."));}
        finally{if(buf!=null)buf.release();}
    }
    /** Bounded snapshots use the same small transport size in both directions. */
    record ConfigSnapshot(Settings rules,MobSettings mobs,ShieldSettings shield,DamageState damage,ClientPolicy policy) {
        static final StreamCodec<RegistryFriendlyByteBuf,ConfigSnapshot> CODEC=StreamCodec.of(
            (buf,data)->{writeOptional(buf,Settings.CODEC,data.rules());writeOptional(buf,MobSettings.CODEC,data.mobs());writeOptional(buf,ShieldSettings.CODEC,data.shield());writeOptional(buf,DamageState.CODEC,data.damage());writeOptional(buf,ClientPolicy.CODEC,data.policy());},
            buf->new ConfigSnapshot(readOptional(buf,Settings.CODEC),readOptional(buf,MobSettings.CODEC),readOptional(buf,ShieldSettings.CODEC),readOptional(buf,DamageState.CODEC),readOptional(buf,ClientPolicy.CODEC)));
    }

    private static ConfigSnapshot cachedSnapshot;
    private static byte[] cachedSnapshotBytes;
    private static int nextSnapshot;
    private static Upload clientSnapshot;
    public static void clearClientSnapshot(){clientSnapshot=null;}
    public static void expireClientSnapshot(){if(clientSnapshot!=null&&System.nanoTime()-clientSnapshot.touched>UPLOAD_TIMEOUT)clearClientSnapshot();}
    static void sendSnapshot(ServerPlayer player,ConfigSnapshot snapshot) {
        RegistryFriendlyByteBuf buf=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),player.registryAccess(),ConnectionType.NEOFORGE);
        try{
            byte[] packed;
            if(cachedSnapshot!=null&&cachedSnapshot.rules()==snapshot.rules()&&cachedSnapshot.mobs()==snapshot.mobs()&&cachedSnapshot.shield()==snapshot.shield()&&cachedSnapshot.damage()==snapshot.damage()&&cachedSnapshot.policy()==snapshot.policy())packed=cachedSnapshotBytes;
            else{ConfigSnapshot.CODEC.encode(buf,snapshot);packed=compress(buf);cachedSnapshot=snapshot;cachedSnapshotBytes=packed;}int size=packed.length;
            if(size>MAX_SAVE_BYTES)throw new IllegalArgumentException("Config snapshot is too large.");
            int total=(size+SAVE_CHUNK_BYTES-1)/SAVE_CHUNK_BYTES,request=++nextSnapshot;
            for(int index=0;index<total;index++){
                byte[] bytes=java.util.Arrays.copyOfRange(packed,index*SAVE_CHUNK_BYTES,Math.min(size,(index+1)*SAVE_CHUNK_BYTES));
                PacketDistributor.sendToPlayer(player,new SnapshotChunk(request,total,index,bytes));
            }
        }finally{if(buf!=null)buf.release();}
    }
    public static void sendConfigData(ServerPlayer player,CustomPacketPayload payload) {
        if(payload instanceof Settings data)sendSnapshot(player,new ConfigSnapshot(data,null,null,null,null));
        else if(payload instanceof MobSettings data)sendSnapshot(player,new ConfigSnapshot(null,data,null,null,null));
        else if(payload instanceof ShieldSettings data)sendSnapshot(player,new ConfigSnapshot(null,null,data,null,null));
        else if(payload instanceof DamageState data)sendSnapshot(player,new ConfigSnapshot(null,null,null,data,null));
        else if(payload instanceof ClientPolicy data)sendSnapshot(player,new ConfigSnapshot(null,null,null,null,data));
        else throw new IllegalArgumentException("Not a config snapshot payload.");
    }
    public static void sendInitialConfig(ServerPlayer player){
        sendSnapshot(player,new ConfigSnapshot(GuardConfig.snapshot(player.hasPermissions(2)),GuardConfig.mobSnapshot(false),GuardConfig.shieldSnapshot(),null,clientPolicy()));
    }
    static void receiveSnapshot(SnapshotChunk chunk,IPayloadContext context) {
        expireClientSnapshot();
        if(chunk.total()<1||chunk.total()>(MAX_SAVE_BYTES+SAVE_CHUNK_BYTES-1)/SAVE_CHUNK_BYTES||chunk.index()<0||chunk.index()>=chunk.total()||chunk.bytes().length==0||chunk.bytes().length>SAVE_CHUNK_BYTES){clearClientSnapshot();return;}
        SaveChunk part=new SaveChunk(chunk.request(),chunk.total(),chunk.index(),chunk.bytes());
        if(chunk.index()==0)clientSnapshot=new Upload(part);
        if(clientSnapshot==null||!clientSnapshot.append(part)){clearClientSnapshot();return;}
        byte[] bytes=clientSnapshot.complete();if(bytes==null)return;clearClientSnapshot();
        RegistryFriendlyByteBuf buf=null;
        try{buf=new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(decompress(bytes)),context.player().registryAccess(),ConnectionType.NEOFORGE);
            ConfigSnapshot snapshot=ConfigSnapshot.CODEC.decode(buf);
            if(buf.readableBytes()!=0)throw new IllegalArgumentException("Trailing config snapshot data.");
            if(snapshot.policy()!=null)GuardClient.clientPolicy(snapshot.policy());
            if(snapshot.mobs()!=null)GuardClient.mobSettings(snapshot.mobs());
            if(snapshot.rules()!=null)GuardClient.settings(snapshot.rules());
            if(snapshot.shield()!=null)GuardClient.shieldSettings(snapshot.shield());
            if(snapshot.damage()!=null)GuardClient.damageState(snapshot.damage());
        }catch(RuntimeException error){System.err.println("Mallard Guard: could not read config snapshot: "+error.getMessage());}
        finally{if(buf!=null)buf.release();}
    }

}
