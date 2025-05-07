package io.antmedia.webrtcandroidframework.core;

import androidx.annotation.Nullable;

import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.EglBase;
import org.webrtc.VideoCodecInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public class AMSDefaultVideoDecoderFactory extends DefaultVideoDecoderFactory {
    public AMSDefaultVideoDecoderFactory(@Nullable EglBase.Context eglContext) {
        super(eglContext);
    }

    public VideoCodecInfo[] getSupportedCodecs() {
        List<VideoCodecInfo> supportedCodecInfos = new ArrayList<VideoCodecInfo>();
        // Generate a list of supported codecs in order of preference:
        // VP8, VP9, H264 (high profile), H264 (baseline profile) and AV1.
        for (AMSHardwareVideoEncoderFactory.VideoCodecMimeType type : new AMSHardwareVideoEncoderFactory.VideoCodecMimeType[] {AMSHardwareVideoEncoderFactory.VideoCodecMimeType.H264}) {

            supportedCodecInfos.add(new VideoCodecInfo(
                    type.name(), AMSHardwareVideoEncoderFactory.getDefaultH264Params( /* highProfile= */ true)));
        }


        return supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);}
}
