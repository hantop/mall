package com.hengxunda.springcloud.nio.handlers.live.utils;

import com.baidubce.services.lss.model.GetStreamResponse;
import com.baidubce.services.lss.model.ListStreamResponse;
import com.baidubce.services.lss.model.LivePlay;
import com.baidubce.services.lss.model.LiveStream;
import com.google.common.collect.Maps;
import com.hengxunda.springcloud.common.utils.Collections3Utils;
import com.hengxunda.springcloud.common.utils.SpringContextUtils;
import com.hengxunda.springcloud.common.utils.StringUtils;
import com.hengxunda.springcloud.nio.baidu.BaiduCloudService;
import com.hengxunda.springcloud.nio.handlers.live.LiveAddressHandler;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class LiveAddressUtils {

    private static BaiduCloudService baiduCloudService = SpringContextUtils.getBean(BaiduCloudService.class);

    private static final Map<String, LVBChannel> roomIdAddress = Maps.newHashMap();

    private static boolean isInited = false;

    public static void put(String roomId, LVBChannel address) {
        roomIdAddress.put(roomId, address);
    }

    public static LVBChannel get(String roomId) {
        if (Objects.isNull(roomIdAddress.get(roomId))) {
            initSession();
        }
        return roomIdAddress.get(roomId);
    }

    private static void initSession() {

        if (!isInited) {
            ListStreamResponse listResponseReady = baiduCloudService.listStream("play.ofweek.com", "READY");
            ListStreamResponse listResponseOngoing = baiduCloudService.listStream("play.ofweek.com", "ONGOING");
            List<LiveStream> streams = Collections3Utils.union(listResponseReady.getStreams(), listResponseOngoing.getStreams());

            streams.forEach(liveStream -> {
                String pushStream = liveStream.getPublish().getPushStream();
                if (Objects.nonNull(pushStream) && pushStream.contains(LiveAddressHandler.LIVE_NAME_PREFIX)) {
                    String roomId = StringUtils.substringAfter(pushStream, "_");
                    GetStreamResponse streamResponse = baiduCloudService.getStream("play.ofweek.com", "liveofweek", pushStream);
                    LiveAddressUtils.put(roomId, newLVBChannel(streamResponse));
                }

            });
            isInited = true;
        }

    }

    private static LVBChannel newLVBChannel(GetStreamResponse streamResponse) {
        LVBChannel channel = LVBChannel.builder()
                .channelId(streamResponse.getSessionId())
                .upstreamAddress(streamResponse.getPublish().getPushUrl())
                .build();

        LivePlay playInfo = streamResponse.getPlay();
        DownstreamAddress address = new DownstreamAddress();
        address.setFlvAddress(playInfo.getFlvUrls().get("L0"));
        address.setHlsAddress(playInfo.getHlsUrls().get("L0"));
        address.setRtmpAddress(playInfo.getRtmpUrls().get("L0"));

        channel.setDownstreamAddress(address);
        channel.setHlsVoiceAddress(playInfo.getHlsUrls().get("L1"));
        return channel;
    }
}
