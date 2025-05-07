package io.antmedia.webrtcandroidframework.core;

import android.media.MediaCodecInfo;

import org.webrtc.EglBase;
import org.webrtc.HardwareVideoEncoderFactory;
import org.webrtc.VideoCodecInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AMSHardwareVideoEncoderFactory extends HardwareVideoEncoderFactory {
    public AMSHardwareVideoEncoderFactory(EglBase.Context sharedContext, boolean enableIntelVp8Encoder, boolean enableH264HighProfile) {
        super(sharedContext, enableIntelVp8Encoder, enableH264HighProfile);
    }

    enum VideoCodecMimeType {
        VP8("video/x-vnd.on2.vp8"),
        VP9("video/x-vnd.on2.vp9"),
        H264("video/avc"),
        AV1("video/av01");

        private final String mimeType;

        private VideoCodecMimeType(String mimeType) {
            this.mimeType = mimeType;
        }

        String mimeType() {
            return this.mimeType;
        }
    }

    public boolean isHardwareSupportedInCurrentSdkH264(MediaCodecInfo info) {
        // First, H264 hardware might perform poorly on this model.
        String name = info.getName();
        // QCOM and Exynos H264 encoders are always supported.
        //return name.startsWith(QCOM_PREFIX) || name.startsWith(EXYNOS_PREFIX);
        //ignore chipsets and return true for all of them
        return true;
    }

    @Override
    public VideoCodecInfo[] getSupportedCodecs() {
        List<VideoCodecInfo> supportedCodecInfos = new ArrayList<VideoCodecInfo>();
        // Generate a list of supported codecs in order of preference:
        // VP8, VP9, H264 (high profile), H264 (baseline profile) and AV1.
        for (VideoCodecMimeType type : new VideoCodecMimeType[] {VideoCodecMimeType.H264}) {

            supportedCodecInfos.add(new VideoCodecInfo(
                    type.name(), getDefaultH264Params( /* highProfile= */ true)));
        }


        return supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
    }

    public static Map<String, String> getDefaultH264Params(boolean isHighProfile) {
        Map<String, String> params = new HashMap();
        params.put("level-asymmetry-allowed", "1");
        params.put("packetization-mode", "1");
        params.put("profile-level-id", isHighProfile ? "640c1f" : "42e01f");
        return params;
    }
}
