package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.api.NetWorker;
import com.github.tartaricacid.netmusic.client.audio.NetMusicSound;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Supplier;

public class MusicToClientMessage {
    private static final String ERROR_404 = "http://music.163.com/404";
    private static final String MUSIC_163_URL = "https://music.163.com/";
    private final BlockPos pos;
    private final String url;
    private final int timeSecond;
    private final String songName;

    public MusicToClientMessage(BlockPos pos, String url, int timeSecond, String songName) {
        this.pos = pos;
        this.url = url;
        this.timeSecond = timeSecond;
        this.songName = songName;
    }

    public static MusicToClientMessage decode(PacketBuffer buf) {
        return new MusicToClientMessage(BlockPos.of(buf.readLong()), buf.readUtf(), buf.readInt(), buf.readUtf());
    }

    public static void encode(MusicToClientMessage message, PacketBuffer buf) {
        buf.writeLong(message.pos.asLong());
        buf.writeUtf(message.url);
        buf.writeInt(message.timeSecond);
        buf.writeUtf(message.songName);
    }

    public static void handle(MusicToClientMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient()) {
            context.enqueueWork(() -> onHandle(message));
        }
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void onHandle(MusicToClientMessage message) {
        String url = message.url;
        if (message.url.startsWith(MUSIC_163_URL)) {
            try {
                url = NetWorker.getRedirectUrl(message.url, NetMusic.NET_EASE_WEB_API.getRequestPropertyData());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (url != null && !url.equals(ERROR_404)) {
            playMusic(message, url);
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void playMusic(MusicToClientMessage message, String url) {
        final URL urlFinal;
        try {
            urlFinal = new URL(url);
            Minecraft.getInstance().submitAsync(() -> {
                NetMusicSound sound = new NetMusicSound(message.pos, urlFinal, message.timeSecond);
                Minecraft.getInstance().getSoundManager().play(sound);
                Minecraft.getInstance().gui.setNowPlaying(new StringTextComponent(message.songName));
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
